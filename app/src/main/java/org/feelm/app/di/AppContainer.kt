package org.feelm.app.di

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.feelm.app.BuildConfig
import org.feelm.app.data.FeelmRepository
import org.feelm.app.data.SeenTracker
import org.feelm.app.data.SettingsStore
import org.feelm.app.data.api.FeelmApi
import org.feelm.app.data.auth.AuthInterceptor
import org.feelm.app.data.auth.GoogleSignIn
import org.feelm.app.data.auth.TokenAuthenticator
import org.feelm.app.data.auth.TokenStore
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Hand-wired singletons.
 *
 * Two HTTP clients on purpose. The plain one exists so the authenticator can
 * spend the refresh token without going back through itself — an authenticator
 * whose refresh call can also 401 is an infinite loop, and the cheapest way to
 * make that impossible is a client that has no authenticator to loop through.
 */
class AppContainer(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        // Several numeric columns are DECIMALs that Doctrine hands back as
        // strings, so the same field arrives as 7.5 from one endpoint and
        // "7.5" from another. Lenient parsing accepts both rather than failing
        // the whole response over a pair of quotes.
        isLenient = true
    }

    private val converter = json.asConverterFactory("application/json".toMediaType())

    val tokenStore = TokenStore(context.applicationContext)

    val settings = SettingsStore(context.applicationContext)

    val googleSignIn = GoogleSignIn(context.applicationContext)

    private val baseClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /** No auth, no refresh: used only to redeem a refresh token. */
    private val plainApi: FeelmApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(baseClient)
        .addConverterFactory(converter)
        .build()
        .create(FeelmApi::class.java)

    private val authedClient = baseClient.newBuilder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .authenticator(
            TokenAuthenticator(
                store = tokenStore,
                refreshApi = { plainApi },
                onSignedOut = { /* the repository's isSignedIn flow reacts to the cleared store */ },
            )
        )
        .build()

    private val api: FeelmApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(authedClient)
        .addConverterFactory(converter)
        .build()
        .create(FeelmApi::class.java)

    val repository = FeelmRepository(api, tokenStore)

    /**
     * Lives as long as the process, because the NEW badges do: the answer is
     * the same on every screen and changes once a night when the crawl runs.
     */
    val seen = SeenTracker(repository, CoroutineScope(SupervisorJob() + Dispatchers.IO))
}
