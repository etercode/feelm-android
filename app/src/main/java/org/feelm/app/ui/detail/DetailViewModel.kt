package org.feelm.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.feelm.app.FeelmApplication
import org.feelm.app.data.FeelmRepository
import org.feelm.app.data.api.CollectionItem
import org.feelm.app.data.api.CommunityRating
import org.feelm.app.data.api.Entry
import org.feelm.app.data.api.Progress
import org.feelm.app.data.api.Review
import org.feelm.app.data.api.ShelfStatus
import org.feelm.app.data.api.WorkDetail
import org.feelm.app.data.api.WorkSummary

data class DetailUiState(
    val loading: Boolean = true,
    val work: WorkDetail? = null,
    val communityRating: CommunityRating? = null,
    val collection: List<CollectionItem> = emptyList(),
    val related: List<WorkSummary> = emptyList(),
    val reviews: List<Review> = emptyList(),
    /** This user's own review, pulled out of the list so it can be edited. */
    val myReview: Review? = null,
    val shelfStatus: String? = null,
    /** The whole row, not just its shelf: rating and progress live here too. */
    val entry: Entry? = null,
    val signedIn: Boolean = false,
    val userId: Long? = null,
    val error: String? = null,
    val savingProgress: Boolean = false,
    /** Set when a write finished, so the screen can say so once. */
    val notice: String? = null,
)

/**
 * One title: the work, the shelf, the reviews and what to watch next.
 *
 * The work is fetched first because nothing else can be drawn without it;
 * related titles and reviews then load in parallel, since neither blocks the
 * other and both are below the fold. A failure in either is silent — a page
 * that renders without its "more like this" rail is still the page, and an
 * error banner over a working title would be noise.
 */
class DetailViewModel(
    private val repository: FeelmRepository,
    private val type: String,
    private val slug: String,
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            val signedIn = repository.isSignedIn.first()

            val response = runCatching { repository.work(type, slug) }.getOrElse { failure ->
                _state.value = _state.value.copy(
                    loading = false,
                    error = failure.message ?: "Could not load this title.",
                )
                return@launch
            }

            _state.value = DetailUiState(
                loading = false,
                work = response.item,
                communityRating = response.rating,
                collection = response.collection,
                signedIn = signedIn,
            )

            val related = async { runCatching { repository.related(type, slug) }.getOrDefault(emptyList()) }
            val reviews = async { runCatching { repository.reviews(type, slug) }.getOrNull() }
            // Only ask about shelves and authorship once there is somebody to
            // ask about.
            val me = async { if (signedIn) runCatching { repository.me() }.getOrNull() else null }
            val entry = async {
                if (signedIn) {
                    runCatching {
                        repository.entries().entries
                            .firstOrNull { it.itemId == response.item.id }
                    }.getOrNull()
                } else {
                    null
                }
            }

            val userId = me.await()?.id
            val allReviews = reviews.await()?.reviews.orEmpty()

            _state.value = _state.value.copy(
                related = related.await(),
                reviews = allReviews.filter { it.userId != userId },
                myReview = userId?.let { id -> allReviews.firstOrNull { it.userId == id } },
                communityRating = reviews.await()?.rating ?: _state.value.communityRating,
                entry = entry.await(),
                shelfStatus = entry.await()?.status,
                userId = userId,
            )
        }
    }

    /**
     * Tapping the shelf a title is already on takes it off again, which is what
     * a toggle should do and saves a separate remove control.
     */
    fun setShelf(status: String) {
        val current = _state.value
        val target = if (current.shelfStatus == status) null else status
        val previous = current.shelfStatus

        // Optimistic: the button moves now and reverts if the write is refused.
        _state.value = current.copy(shelfStatus = target, notice = null)

        viewModelScope.launch {
            runCatching { repository.setShelfStatus(type, slug, target) }
                .onFailure {
                    _state.value = _state.value.copy(
                        shelfStatus = previous,
                        notice = "Could not save that — check your connection.",
                    )
                }
        }
    }

    /**
     * Your own score for a title, which is not the same thing as a review.
     *
     * `Entry.rating` and `Review.rating` are separate columns and
     * `ReviewService` never touches the shelf — so rating something here does
     * not publish anything, and that distinction is the whole reason this
     * control exists alongside the review editor.
     *
     * Rating a title it makes no sense to have no shelf for, so an unshelved
     * one lands on "done" — you rated it, you have seen it.
     */
    fun setShelfRating(rating: Double?) {
        val previous = _state.value.entry
        val workId = _state.value.work?.id ?: return
        val hadShelf = _state.value.shelfStatus != null

        /*
         * `previous` is null for a title that was not already on a shelf, and
         * `previous?.copy()` on a null is null — so the optimistic update
         * silently did nothing and the stars never filled, however many times
         * you tapped them. Build a row when there is not one yet.
         */
        _state.value = _state.value.copy(
            entry = (previous ?: Entry(itemId = workId))
                .copy(rating = rating?.let { JsonPrimitive(it) }),
            shelfStatus = _state.value.shelfStatus ?: ShelfStatus.DONE,
        )

        viewModelScope.launch {
            runCatching {
                if (!hadShelf) repository.setShelfStatus(type, slug, ShelfStatus.DONE)
                repository.setShelfRating(type, slug, rating)
            }.onFailure {
                _state.value = _state.value.copy(
                    entry = previous,
                    shelfStatus = if (hadShelf) _state.value.shelfStatus else null,
                    notice = "Could not save your rating.",
                )
            }
        }
    }

    fun setShelfProgress(progress: Progress) {
        viewModelScope.launch {
            _state.value = _state.value.copy(savingProgress = true)
            runCatching { repository.setShelfProgress(type, slug, progress) }
                .onSuccess {
                    /*
                     * Write it back into the entry the screen is holding.
                     * Without this the fields keep what you typed but the
                     * collapsed summary still reads the old value, so saving
                     * looked like it had done nothing until the page was
                     * reopened.
                     */
                    val current = _state.value.entry
                    val workId = _state.value.work?.id
                    // Nothing on screen changes when progress saves — the
                    // fields already read what you typed — so without a word
                    // back there is no way to tell it worked.
                    _state.value = _state.value.copy(
                        entry = workId?.let {
                            (current ?: Entry(itemId = it)).copy(progress = progress.toJson())
                        } ?: current,
                        savingProgress = false,
                        notice = "Progress saved.",
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        savingProgress = false,
                        notice = "Could not save your progress.",
                    )
                }
        }
    }

    fun saveReview(rating: Double, body: String) {
        viewModelScope.launch {
            runCatching { repository.saveReview(type, slug, rating, body) }
                .onSuccess {
                    reloadReviews()
                    _state.value = _state.value.copy(notice = "Review saved.")
                }
                .onFailure {
                    _state.value = _state.value.copy(notice = "Could not save your review.")
                }
        }
    }

    fun deleteReview() {
        viewModelScope.launch {
            runCatching { repository.deleteReview(type, slug) }
                .onSuccess { reloadReviews() }
                .onFailure {
                    _state.value = _state.value.copy(notice = "Could not delete your review.")
                }
        }
    }

    private suspend fun reloadReviews() {
        val response = runCatching { repository.reviews(type, slug) }.getOrNull() ?: return
        val userId = _state.value.userId
        _state.value = _state.value.copy(
            reviews = response.reviews.filter { it.userId != userId },
            myReview = userId?.let { id -> response.reviews.firstOrNull { it.userId == id } },
            communityRating = response.rating,
        )
    }

    fun noticeShown() {
        _state.value = _state.value.copy(notice = null)
    }

    /** Mirrors the repository's shape so the local copy matches what was sent. */
    private fun Progress.toJson(): JsonObject = when (this) {
        is Progress.Series -> buildJsonObject {
            put("season", season)
            put("episode", episode)
        }
        is Progress.Game -> buildJsonObject { put("hours", hours) }
        is Progress.Book -> buildJsonObject { put("page", page) }
    }

    companion object {
        fun factory(type: String, slug: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                        as FeelmApplication
                DetailViewModel(app.container.repository, type, slug)
            }
        }
    }
}
