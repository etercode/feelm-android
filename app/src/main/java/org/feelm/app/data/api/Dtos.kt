package org.feelm.app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/*
 * The wire format Feelm's API already speaks.
 *
 * These mirror App\Presenter\WorkPresenter on the Symfony side rather than the
 * database behind it — `details` is a per-type bag and `ratings` is keyed by
 * source, because that is the contract the web client was built on and the two
 * clients should not drift. Every field the presenter can omit is optional
 * here, and the Json parser ignores unknown keys, so a work carrying extra
 * `extra` columns does not fail the whole response.
 */

@Serializable
data class TokenResponse(
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("refresh_token_expires_at") val refreshTokenExpiresAt: String? = null,
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

/** The ID token Google hands the app, forwarded for server-side verification. */
@Serializable
data class GoogleCredentialRequest(val credential: String)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val name: String,
    val tagline: String? = null,
)

/**
 * What /api/register answers: the new account, not a session. Registering and
 * signing in are two calls, because the endpoint mints no tokens.
 */
@Serializable
data class RegisteredUser(
    val id: Long,
    val username: String? = null,
    val name: String? = null,
    val tagline: String? = null,
)

@Serializable
data class Rating(
    val rating: Double,
    val scale: Int = 10,
    val votes: Int? = null,
)

@Serializable
data class Source(val url: String? = null, val name: String? = null)

/** YouTube, in practice: `{ site, key }`. */
@Serializable
data class Trailer(val site: String? = null, val key: String? = null)

@Serializable
data class CastMember(
    val slug: String? = null,
    val name: String = "",
    val character: String? = null,
    val photo: String? = null,
)

/**
 * The type-specific block. A movie fills runtime and certification, a series
 * the two counts, a book pages and authors — so every field is optional and
 * the UI asks for the ones its type draws.
 */
@Serializable
data class Episode(
    val number: Int? = null,
    val title: String? = null,
    val runtime: Int? = null,
    val airDate: String? = null,
    val overview: String? = null,
)

@Serializable
data class Season(
    val number: Int? = null,
    val name: String = "",
    val title: String? = null,
    val year: Int? = null,
    val overview: String? = null,
    val poster: String? = null,
    val episodes: List<Episode> = emptyList(),
)

@Serializable
data class WorkDetails(
    val releaseDate: String? = null,
    val seasons: List<Season> = emptyList(),
    val runtime: Int? = null,
    val certification: String? = null,
    val originalLanguage: String? = null,
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val pages: Int? = null,
    val publisher: String? = null,
    val directors: List<String> = emptyList(),
    val writers: List<String> = emptyList(),
    val creators: List<String> = emptyList(),
    val authors: List<String> = emptyList(),
    val developers: List<String> = emptyList(),
    val cast: List<CastMember> = emptyList(),
)

/**
 * One row of a listing — a rail, a search result, the release queue.
 *
 * Covers both `listItem()` and `upcoming()`: the queue sends genres, a backdrop
 * and a trailer on top of the card fields, and nothing it omits is required.
 */
@Serializable
data class WorkSummary(
    val id: Long,
    val type: String,
    val slug: String,
    val title: String,
    val year: Int? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val externalScore: Double? = null,
    val ratings: Map<String, Rating> = emptyMap(),
    val genres: List<String> = emptyList(),
    val source: Source? = null,
    val details: WorkDetails = WorkDetails(),
    val addedAt: String? = null,
    val isUpcoming: Boolean = false,
    val trailer: Trailer? = null,
)

/** A title opened: everything above, plus the prose and the outbound links. */
@Serializable
data class WorkDetail(
    val id: Long,
    val type: String,
    val slug: String,
    val title: String,
    val originalTitle: String? = null,
    val year: Int? = null,
    val tagline: String? = null,
    val overview: String? = null,
    val genres: List<String> = emptyList(),
    val poster: String? = null,
    val backdrop: String? = null,
    val externalScore: Double? = null,
    val ratings: Map<String, Rating> = emptyMap(),
    val externalIds: Map<String, String> = emptyMap(),
    val source: Source? = null,
    val details: WorkDetails = WorkDetails(),
    val addedAt: String? = null,
    val isUpcoming: Boolean = false,
    val trailer: Trailer? = null,
)

/** What Feelm's own members scored it, as opposed to IMDb or TMDB. */
@Serializable
data class CommunityRating(val average: Double? = null, val count: Int = 0)

/**
 * Siblings in a collection. Deliberately not a [WorkSummary]: the endpoint
 * answers this one straight out of SQL, so `poster` is the raw stored path
 * rather than a resolved URL, and typing it the same would invite the UI to
 * load an image from a path that is not an address.
 */
@Serializable
data class CollectionItem(
    val id: Long,
    val type: String,
    val slug: String,
    val title: String,
    val part: Int? = null,
)

@Serializable
data class WorkDetailResponse(
    val item: WorkDetail,
    val rating: CommunityRating? = null,
    val collection: List<CollectionItem> = emptyList(),
)

@Serializable
data class Rail(val type: String, val items: List<WorkSummary> = emptyList())

@Serializable
data class HomeResponse(
    val rails: List<Rail> = emptyList(),
    val latest: List<WorkSummary> = emptyList(),
    val upcoming: List<WorkSummary> = emptyList(),
)

@Serializable
data class User(
    val id: Long,
    val username: String? = null,
    val name: String? = null,
    val tagline: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val avatar: String? = null,
    val email: String? = null,
    val emailVerified: Boolean = false,
    val joinedAt: String? = null,
    /** False for accounts created through Google, which never set one. */
    val hasPassword: Boolean = true,
    /**
     * A Google sign-up gets a placeholder handle it has not chosen yet. Until
     * this clears, the app owes the user the welcome screen.
     */
    val handlePending: Boolean = false,
    val locale: String? = null,
    val timezone: String? = null,
    val roles: List<String> = emptyList(),
)

@Serializable
data class ChooseHandleRequest(val username: String)

/**
 * How far through the catalog you have caught up.
 *
 * A NEW badge is "crawled after `seenUpTo` and not in `itemIds`" — which is
 * why catching up is one timestamp write rather than a row per work in a
 * catalog of seven hundred thousand.
 */
@Serializable
data class SeenResponse(
    val itemIds: List<Long> = emptyList(),
    val seenUpTo: String? = null,
)

@Serializable
data class CatchUpResponse(val ok: Boolean = false, val seenUpTo: String? = null)

/**
 * A title on your shelf. `status` is one of wishlist/active/done/dropped.
 *
 * Two fields cannot be typed the obvious way, because the API answers them
 * differently depending on which endpoint you ask:
 *
 *  - `rating` is a DECIMAL column held as `?string` on the entity.
 *    `EntryPresenter` casts it to a float, but `shelfStateForUser` selects it
 *    raw through `getArrayResult()`, so /api/me/entries sends `"7.5"` while a
 *    shelf write sends `7.5`. [JsonPrimitive] accepts both; [score] reads it.
 *  - `progress` is an object — how far into which season, or which page — not
 *    a number.
 *
 * Getting either wrong throws during decoding and takes the whole shelf with
 * it, which is exactly how this was found: a shelf of sixteen titles reported
 * itself as empty.
 */
@Serializable
data class Entry(
    val id: Long? = null,
    val userId: Long? = null,
    val itemId: Long,
    val status: String? = null,
    val rating: JsonPrimitive? = null,
    val progress: JsonElement? = null,
    val updatedAt: String? = null,
) {
    val score: Double? get() = rating?.contentOrNull?.toDoubleOrNull()
}

@Serializable
data class EntriesResponse(
    val entries: List<Entry> = emptyList(),
    val items: List<WorkSummary> = emptyList(),
    val total: Int = 0,
)

/**
 * A shelf write.
 *
 * `progress` is shaped by the work's type, and the server normalises it to
 * exactly the keys that type allows: `{season, episode}` for a series,
 * `{hours}` for a game, `{page}` for a book, and nothing at all for a film.
 * `rating` must be 0.5–5.0 on a half-step or the write is refused.
 */
@Serializable
data class UpsertEntryRequest(
    val status: String? = null,
    val rating: Double? = null,
    val progress: JsonObject? = null,
    val clear: Boolean? = null,
)

/** What `progress` means for each type, and what a control should collect. */
sealed interface Progress {
    data class Series(val season: Int, val episode: Int) : Progress
    data class Game(val hours: Double) : Progress
    data class Book(val page: Int) : Progress
}

/**
 * A page of works. Shared by /api/items and /api/search, which answer the same
 * shape — search adds a query and facets on top of a listing.
 */
@Serializable
data class SearchResponse(
    val items: List<WorkSummary> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pages: Int = 1,
    val hasMore: Boolean = false,
    val limit: Int = 24,
    val suggestion: String? = null,
)

@Serializable
data class PersonRef(val slug: String? = null, val name: String = "")

/** Type-ahead: a few works and any people whose name matches. */
@Serializable
data class SuggestResponse(
    val query: String? = null,
    val items: List<WorkSummary> = emptyList(),
    val total: Int = 0,
    val suggestion: String? = null,
    val people: List<PersonRef> = emptyList(),
)

@Serializable
data class GenreRef(val slug: String, val name: String)

@Serializable
data class LanguageRef(val code: String, val count: Int = 0)

@Serializable
data class YearBounds(val min: Int = 1888, val max: Int = 2031)

/**
 * What the filter panel offers. The server aggregates this over the whole
 * catalog and caches it for an hour, so it is fetched once per session and
 * held rather than re-asked when the sheet opens.
 */
@Serializable
data class FiltersResponse(
    val genres: List<GenreRef> = emptyList(),
    val certifications: List<String> = emptyList(),
    val languages: List<LanguageRef> = emptyList(),
    val years: YearBounds = YearBounds(),
    val decades: List<Int> = emptyList(),
    val sorts: List<String> = emptyList(),
)

@Serializable
data class UserCompact(
    val id: Long? = null,
    val username: String? = null,
    val name: String? = null,
    val avatar: String? = null,
)

@Serializable
data class Review(
    val id: Long? = null,
    val userId: Long? = null,
    val itemId: Long? = null,
    val user: UserCompact? = null,
    val rating: Double? = null,
    val body: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class ReviewsResponse(
    val reviews: List<Review> = emptyList(),
    val rating: CommunityRating? = null,
)

@Serializable
data class SaveReviewRequest(val rating: Double, val body: String)

@Serializable
data class ItemsResponse(val items: List<WorkSummary> = emptyList())

@Serializable
data class Person(
    val slug: String? = null,
    val name: String = "",
    /**
     * Raw stored path, unlike the cast photos on a work — that endpoint runs
     * them through the URL generator and this one does not. [mediaUrl] makes
     * it an address.
     */
    val photo: String? = null,
)

/** One role's worth of filmography: what they did, and on what. */
@Serializable
data class CreditGroup(
    val role: String = "",
    val items: List<WorkSummary> = emptyList(),
)

@Serializable
data class ShelfStats(
    val logged: Int = 0,
    val finished: Int = 0,
    val reviews: Int = 0,
    val byType: Map<String, Int> = emptyMap(),
)

/** A title someone is part way through, with the entry that says how far. */
@Serializable
data class CurrentEntry(val entry: Entry? = null, val item: WorkSummary)

@Serializable
data class ProfileResponse(
    val user: User,
    val stats: ShelfStats = ShelfStats(),
    val current: List<CurrentEntry> = emptyList(),
    val banner: WorkDetail? = null,
    val reviews: List<Review> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    /** Absent when looking at your own profile, or when signed out. */
    val isFollowing: Boolean? = null,
    val sharedCount: Int? = null,
)

/** One row of the timeline: who did what to which title, and what they said. */
@Serializable
data class ActivityRow(
    val entry: Entry? = null,
    val user: UserCompact? = null,
    val item: WorkSummary,
    val review: Review? = null,
)

@Serializable
data class FeedResponse(
    val scope: String = "following",
    val page: Int = 1,
    val hasMore: Boolean = false,
    val activity: List<ActivityRow> = emptyList(),
)

@Serializable
data class FollowResponse(val following: Boolean = false)

/** One shelf of someone's profile: the first few titles, and how many there are. */
@Serializable
data class ProfileRail(
    val items: List<CurrentEntry> = emptyList(),
    val total: Int = 0,
)

/**
 * A profile's shelves in one request.
 *
 * Five rails — watching, loved, finished, wishlist, dropped — each already
 * trimmed to poster-card fields. The server assembles them in one pass because
 * five separate calls is what the page used to cost.
 */
/** A page of somebody's shelf, filtered by status. */
@Serializable
data class UserShelfResponse(
    val items: List<CurrentEntry> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pages: Int = 1,
)

@Serializable
data class ProfileOverview(
    val rails: Map<String, ProfileRail> = emptyMap(),
)

/** Followers or following: the same shape either way. */
@Serializable
data class FollowListResponse(val users: List<User> = emptyList())

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val tagline: String? = null,
    val bio: String? = null,
    val location: String? = null,
)

@Serializable
data class UpdatePreferencesRequest(val locale: String, val timezone: String)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

@Serializable
data class PersonResponse(
    val person: Person,
    val credits: List<CreditGroup> = emptyList(),
    val total: Int = 0,
)

/** The four things a shelf button can say. Mirrors Entry::STATUSES. */
object ShelfStatus {
    const val WISHLIST = "wishlist"
    const val ACTIVE = "active"
    const val DONE = "done"
    const val DROPPED = "dropped"
}
