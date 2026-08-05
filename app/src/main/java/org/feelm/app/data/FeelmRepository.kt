package org.feelm.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.feelm.app.data.api.Progress
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.feelm.app.data.api.CatchUpResponse
import org.feelm.app.data.api.ChangePasswordRequest
import org.feelm.app.data.api.SeenResponse
import org.feelm.app.data.api.ChooseHandleRequest
import org.feelm.app.data.api.EntriesResponse
import org.feelm.app.data.api.FeedResponse
import org.feelm.app.data.api.ProfileOverview
import org.feelm.app.data.api.ProfileResponse
import org.feelm.app.data.api.UpdatePreferencesRequest
import org.feelm.app.data.api.UpdateProfileRequest
import org.feelm.app.data.api.FeelmApi
import org.feelm.app.data.api.FiltersResponse
import org.feelm.app.data.api.GoogleCredentialRequest
import org.feelm.app.data.api.HomeResponse
import org.feelm.app.data.api.LoginRequest
import org.feelm.app.data.api.PersonResponse
import org.feelm.app.data.api.RegisterRequest
import org.feelm.app.data.api.ReviewsResponse
import org.feelm.app.data.api.SaveReviewRequest
import org.feelm.app.data.api.SearchResponse
import org.feelm.app.data.api.SuggestResponse
import org.feelm.app.data.api.UpsertEntryRequest
import org.feelm.app.data.api.User
import org.feelm.app.data.api.WorkDetailResponse
import org.feelm.app.data.api.WorkSummary
import org.feelm.app.data.auth.TokenStore

/**
 * How the catalog is being sliced on the browse screen.
 *
 * One object rather than a dozen arguments threaded through the ViewModel:
 * paging re-issues the same query with the page advanced, and a filter sheet
 * that changes one field should not have to know the other eleven.
 */
data class BrowseQuery(
    val type: String? = null,
    val genres: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val scoreMin: Int? = null,
    val release: String? = null,
    val sort: String = "popularity",
) {
    /** Whether anything beyond the type and sort is narrowing the results. */
    val hasFilters: Boolean
        get() = genres.isNotEmpty() || certifications.isNotEmpty() || languages.isNotEmpty() ||
            yearFrom != null || yearTo != null || scoreMin != null || release != null

    val filterCount: Int
        get() = genres.size + certifications.size + languages.size +
            listOfNotNull(yearFrom ?: yearTo, scoreMin, release).size
}

/**
 * The one place the UI talks to.
 *
 * Screens get suspend functions and a sign-in flow; they never see Retrofit,
 * tokens or HTTP status codes. Errors surface as thrown exceptions and each
 * ViewModel decides what that means on its own screen — a failed listing is a
 * retry button, a failed shelf write is a toast and a reverted toggle.
 */
class FeelmRepository(
    private val api: FeelmApi,
    private val tokenStore: TokenStore,
) {
    val isSignedIn: Flow<Boolean> = tokenStore.tokensFlow.map { it != null }

    suspend fun home(): HomeResponse = api.home()

    suspend fun work(type: String, slug: String): WorkDetailResponse = api.work(type, slug)

    suspend fun related(type: String, slug: String): List<WorkSummary> =
        api.related(type, slug).items

    suspend fun reviews(type: String, slug: String): ReviewsResponse = api.reviews(type, slug)

    /**
     * Both arguments are required, not optional: the server takes a
     * non-nullable float and throws `empty_body` on blank text.
     */
    suspend fun saveReview(type: String, slug: String, rating: Double, body: String) {
        api.saveReview(type, slug, SaveReviewRequest(rating = rating, body = body))
    }

    suspend fun deleteReview(type: String, slug: String) {
        api.deleteReview(type, slug)
    }

    suspend fun browse(query: BrowseQuery, page: Int): SearchResponse = api.items(
        type = query.type,
        genres = query.genres.ifEmpty { null },
        certifications = query.certifications.ifEmpty { null },
        languages = query.languages.ifEmpty { null },
        yearFrom = query.yearFrom,
        yearTo = query.yearTo,
        scoreMin = query.scoreMin,
        release = query.release,
        sort = query.sort,
        page = page,
    )

    suspend fun search(query: String, filters: BrowseQuery, page: Int = 1): SearchResponse =
        api.search(
            query = query,
            type = filters.type,
            genres = filters.genres.ifEmpty { null },
            certifications = filters.certifications.ifEmpty { null },
            languages = filters.languages.ifEmpty { null },
            yearFrom = filters.yearFrom,
            yearTo = filters.yearTo,
            scoreMin = filters.scoreMin,
            release = filters.release,
            // A query with no explicit sort should rank by how well it matches,
            // which is what the server defaults to — sending "popularity" here
            // would quietly override that.
            sort = filters.sort.takeIf { it != "popularity" },
            page = page,
        )

    suspend fun overview(username: String): ProfileOverview = api.overview(username)

    suspend fun followers(username: String): List<User> = api.followers(username).users

    suspend fun following(username: String): List<User> = api.following(username).users

    suspend fun suggest(query: String): SuggestResponse = api.suggest(query)

    suspend fun filters(): FiltersResponse = api.filters()

    suspend fun person(slug: String): PersonResponse = api.person(slug)

    suspend fun profile(username: String): ProfileResponse = api.profile(username)

    suspend fun toggleFollow(username: String): Boolean = api.toggleFollow(username).following

    suspend fun feed(scope: String = "following", page: Int = 1): FeedResponse =
        api.feed(scope = scope, page = page)

    suspend fun updateProfile(
        name: String,
        tagline: String?,
        bio: String?,
        location: String?,
    ): User = api.updateProfile(
        UpdateProfileRequest(
            name = name.trim(),
            tagline = tagline?.trim()?.ifBlank { null },
            bio = bio?.trim()?.ifBlank { null },
            location = location?.trim()?.ifBlank { null },
        )
    )

    suspend fun updatePreferences(locale: String, timezone: String): User =
        api.updatePreferences(UpdatePreferencesRequest(locale, timezone))

    /**
     * An account created through Google has no password to confirm, so the
     * current one goes up blank and the server accepts the bearer token as
     * proof instead.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String) {
        api.changePassword(ChangePasswordRequest(currentPassword, newPassword))
    }

    suspend fun deleteAvatar() {
        api.deleteAvatar()
    }

    /**
     * Upload an avatar the app has already read into memory.
     *
     * Bytes rather than a File because the photo picker hands back a content
     * URI the app has no filesystem path for — copying it to a temp file first
     * would be a second write of something the server is about to resize
     * anyway.
     */
    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): User {
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        return api.uploadAvatar(
            MultipartBody.Part.createFormData("avatar", "avatar.jpg", body)
        )
    }

    suspend fun signIn(username: String, password: String) {
        tokenStore.save(api.login(LoginRequest(username.trim(), password)))
    }

    suspend fun signInWithGoogle(idToken: String) {
        tokenStore.save(api.google(GoogleCredentialRequest(idToken)))
    }

    /**
     * Registering does not mint a session, so this signs in straight after —
     * otherwise a new account lands on a login form having just typed the
     * password it would need to type again.
     */
    suspend fun register(username: String, email: String, password: String, name: String) {
        api.register(
            RegisterRequest(
                username = username.trim(),
                email = email.trim(),
                password = password,
                name = name.trim().ifEmpty { username.trim() },
            )
        )
        signIn(username, password)
    }

    suspend fun signOut() = tokenStore.clear()

    suspend fun me(): User = api.me()

    suspend fun chooseHandle(username: String): User =
        api.chooseHandle(ChooseHandleRequest(username.trim()))

    suspend fun entries(): EntriesResponse = api.entries()

    suspend fun seen(): SeenResponse = api.seen()

    suspend fun markSeen(type: String, slug: String) {
        api.markSeen(type, slug)
    }

    suspend fun catchUp(): CatchUpResponse = api.catchUp()

    /**
     * Put a title on a shelf, or take it off with a null status.
     *
     * The API distinguishes the two — a PUT with `clear` versus a DELETE — but
     * a caller is only ever answering "which shelf, if any", so that split
     * stays down here.
     */
    suspend fun setShelfStatus(type: String, slug: String, status: String?) {
        if (status == null) {
            api.deleteEntry(type, slug)
        } else {
            api.upsertEntry(type, slug, UpsertEntryRequest(status = status))
        }
    }

    suspend fun setShelfRating(type: String, slug: String, rating: Double?) {
        api.upsertEntry(type, slug, UpsertEntryRequest(rating = rating))
    }

    suspend fun setShelfProgress(type: String, slug: String, progress: Progress) {
        api.upsertEntry(type, slug, UpsertEntryRequest(progress = progress.toJson()))
    }

    private fun Progress.toJson(): JsonObject = when (this) {
        is Progress.Series -> buildJsonObject {
            put("season", season)
            put("episode", episode)
        }
        is Progress.Game -> buildJsonObject { put("hours", hours) }
        is Progress.Book -> buildJsonObject { put("page", page) }
    }
}
