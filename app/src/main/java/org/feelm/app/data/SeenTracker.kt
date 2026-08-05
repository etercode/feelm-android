package org.feelm.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.feelm.app.data.api.WorkSummary
import java.time.Instant

/**
 * What has appeared in the catalog since you last caught up.
 *
 * Held once for the whole app rather than per screen: every poster card in
 * every rail and grid asks the same question, and each of them fetching the
 * answer would be one request per screen for a payload that changes when the
 * crawl runs — once a night.
 */
data class SeenState(
    val seenUpTo: Instant? = null,
    val seenIds: Set<Long> = emptySet(),
    val signedIn: Boolean = false,
) {
    /**
     * A title is new when the crawl added it after your last catch-up and you
     * have not opened it since.
     *
     * Signed out there is no "last visit" to compare against — a badge on
     * everything, which is a badge on nothing — so nothing is marked.
     */
    fun isNew(work: WorkSummary): Boolean {
        if (!signedIn || work.id in seenIds) return false
        val since = seenUpTo ?: return false
        val added = work.addedAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: return false
        return added.isAfter(since)
    }
}

class SeenTracker(
    private val repository: FeelmRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(SeenState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            if (!repository.isSignedIn.first()) {
                _state.value = SeenState()
                return@launch
            }

            runCatching { repository.seen() }
                .onSuccess { response ->
                    _state.value = SeenState(
                        seenUpTo = response.seenUpTo
                            ?.let { runCatching { Instant.parse(it) }.getOrNull() },
                        seenIds = response.itemIds.toSet(),
                        signedIn = true,
                    )
                }
        }
    }

    /**
     * Opening a title clears its badge.
     *
     * Applied locally first so the badge is gone by the time you press back,
     * rather than a round trip later.
     */
    fun markSeen(id: Long, type: String, slug: String) {
        val current = _state.value
        if (!current.signedIn || id in current.seenIds) return

        _state.value = current.copy(seenIds = current.seenIds + id)
        scope.launch { runCatching { repository.markSeen(type, slug) } }
    }

    fun catchUp() {
        scope.launch {
            runCatching { repository.catchUp() }
                .onSuccess { response ->
                    _state.value = _state.value.copy(
                        seenUpTo = response.seenUpTo
                            ?.let { runCatching { Instant.parse(it) }.getOrNull() },
                        // The server drops the individual marks when it moves
                        // the timestamp, because they are subsumed by it.
                        seenIds = emptySet(),
                    )
                }
        }
    }
}
