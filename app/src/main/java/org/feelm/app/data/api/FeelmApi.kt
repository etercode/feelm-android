package org.feelm.app.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Feelm's HTTP API.
 *
 * Everything under /api is stateless and bearer-authenticated, so there is no
 * cookie jar and no CSRF token — the client attaches an access token or it does
 * not, and the public endpoints answer either way.
 *
 * Repeatable filters (`genre`, `certification`, `language`) are declared as
 * lists because Symfony reads them as arrays off the query string; a
 * comma-joined string would arrive as one nonsensical value.
 */
interface FeelmApi {

    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): TokenResponse

    /** The Google ID token, verified server-side against Feelm's client id. */
    @POST("api/auth/google")
    suspend fun google(@Body body: GoogleCredentialRequest): TokenResponse

    @POST("api/register")
    suspend fun register(@Body body: RegisterRequest): RegisteredUser

    @POST("api/token/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse

    @GET("api/home")
    suspend fun home(
        @Query("rail") rail: Int = 20,
        @Query("upcoming") upcoming: Int = 12,
    ): HomeResponse

    @GET("api/items/{type}/{slug}")
    suspend fun work(
        @Path("type") type: String,
        @Path("slug") slug: String,
    ): WorkDetailResponse

    @GET("api/items/{type}/{slug}/related")
    suspend fun related(
        @Path("type") type: String,
        @Path("slug") slug: String,
        @Query("limit") limit: Int = 12,
    ): ItemsResponse

    @GET("api/items/{type}/{slug}/reviews")
    suspend fun reviews(
        @Path("type") type: String,
        @Path("slug") slug: String,
    ): ReviewsResponse

    @PUT("api/me/reviews/{type}/{slug}")
    suspend fun saveReview(
        @Path("type") type: String,
        @Path("slug") slug: String,
        @Body body: SaveReviewRequest,
    ): Response<Review>

    @DELETE("api/me/reviews/{type}/{slug}")
    suspend fun deleteReview(
        @Path("type") type: String,
        @Path("slug") slug: String,
    ): Response<Unit>

    /**
     * Browse. `total=0` because an endless grid never prints a count, and
     * counting how many of seven hundred thousand rows match is about half the
     * work of listing them.
     */
    @GET("api/items")
    suspend fun items(
        @Query("type") type: String? = null,
        @Query("genre") genres: List<String>? = null,
        @Query("genreMode") genreMode: String? = null,
        @Query("certification") certifications: List<String>? = null,
        @Query("language") languages: List<String>? = null,
        @Query("yearFrom") yearFrom: Int? = null,
        @Query("yearTo") yearTo: Int? = null,
        @Query("scoreMin") scoreMin: Int? = null,
        @Query("release") release: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24,
        @Query("total") total: Int = 0,
    ): SearchResponse

    /** The same filter vocabulary as /api/items, plus a query. */
    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String? = null,
        @Query("genre") genres: List<String>? = null,
        @Query("certification") certifications: List<String>? = null,
        @Query("language") languages: List<String>? = null,
        @Query("yearFrom") yearFrom: Int? = null,
        @Query("yearTo") yearTo: Int? = null,
        @Query("scoreMin") scoreMin: Int? = null,
        @Query("release") release: String? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 24,
        @Query("facets") facets: Int = 0,
    ): SearchResponse

    @GET("api/users/{username}/overview")
    suspend fun overview(@Path("username") username: String): ProfileOverview

    @GET("api/users/{username}/followers")
    suspend fun followers(@Path("username") username: String): FollowListResponse

    @GET("api/users/{username}/following")
    suspend fun following(@Path("username") username: String): FollowListResponse

    @GET("api/search/suggest")
    suspend fun suggest(
        @Query("q") query: String,
        @Query("limit") limit: Int = 8,
    ): SuggestResponse

    @GET("api/search/filters")
    suspend fun filters(): FiltersResponse

    @GET("api/people/{slug}")
    suspend fun person(@Path("slug") slug: String): PersonResponse

    @GET("api/me")
    suspend fun me(): User

    /** Only accepted while the account's handle is still pending. */
    @POST("api/me/username")
    suspend fun chooseHandle(@Body body: ChooseHandleRequest): User

    @GET("api/me/entries")
    suspend fun entries(): EntriesResponse

    @GET("api/me/seen")
    suspend fun seen(): SeenResponse

    @POST("api/me/seen/{type}/{slug}")
    suspend fun markSeen(
        @Path("type") type: String,
        @Path("slug") slug: String,
    ): Response<Unit>

    @POST("api/me/seen/catch-up")
    suspend fun catchUp(): CatchUpResponse

    @PATCH("api/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): User

    @PATCH("api/me/preferences")
    suspend fun updatePreferences(@Body body: UpdatePreferencesRequest): User

    @POST("api/me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<Unit>

    /** Field name is `avatar`, matching what MeController reaches for. */
    @Multipart
    @POST("api/me/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): User

    @DELETE("api/me/avatar")
    suspend fun deleteAvatar(): Response<Unit>

    @GET("api/users/{username}")
    suspend fun profile(@Path("username") username: String): ProfileResponse

    /** One call both follows and unfollows; the server answers with the new state. */
    @POST("api/me/follows/{username}")
    suspend fun toggleFollow(@Path("username") username: String): FollowResponse

    @GET("api/me/feed")
    suspend fun feed(
        @Query("scope") scope: String = "following",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 40,
    ): FeedResponse

    @PUT("api/me/entries/{type}/{slug}")
    suspend fun upsertEntry(
        @Path("type") type: String,
        @Path("slug") slug: String,
        @Body body: UpsertEntryRequest,
    ): Response<Entry>

    @DELETE("api/me/entries/{type}/{slug}")
    suspend fun deleteEntry(
        @Path("type") type: String,
        @Path("slug") slug: String,
    ): Response<Unit>
}
