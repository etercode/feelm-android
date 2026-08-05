package org.feelm.app.data.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.feelm.app.data.api.FeelmApi
import org.feelm.app.data.api.RefreshRequest

/**
 * Attaches the access token to every outbound call.
 *
 * Public endpoints are happy without one, so a signed-out browse works
 * unchanged; the header simply goes missing. Login and refresh are skipped
 * because presenting an expired token to them is what caused the 401 we are
 * trying to resolve.
 */
class AuthInterceptor(private val store: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.encodedPath.let { it.endsWith("/api/login") || it.endsWith("/api/token/refresh") }) {
            return chain.proceed(request)
        }

        val access = store.tokensBlocking()?.access ?: return chain.proceed(request)

        return chain.proceed(
            request.newBuilder().header("Authorization", "Bearer $access").build()
        )
    }
}

/**
 * Trades the refresh token for a new access token when the server rejects one.
 *
 * OkHttp calls this on a 401 and retries whatever it returns, so the refresh
 * is invisible above the network layer — no screen has to know a token aged
 * out mid-scroll.
 *
 * Two things it guards against. Parallel requests all 401ing at once would
 * otherwise each spend the refresh token, and only the first spend wins; the
 * lock plus the re-read inside it means the losers pick up the token the
 * winner just stored. And a refresh that itself fails clears the session
 * rather than retrying forever — that token is dead, and the only honest
 * answer is to sign out.
 */
class TokenAuthenticator(
    private val store: TokenStore,
    private val refreshApi: () -> FeelmApi,
    private val onSignedOut: () -> Unit,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once with a fresh token and still refused: give up.
        if (response.priorResponse != null) return null

        val attempted = store.tokensBlocking() ?: return null

        synchronized(this) {
            val current = store.tokensBlocking() ?: return null

            // Another call refreshed while this one waited on the lock.
            if (current.access != attempted.access) {
                return response.request.retryWith(current.access)
            }

            val refreshed = runCatching {
                runBlocking { refreshApi().refresh(RefreshRequest(current.refresh)) }
            }.getOrNull()

            if (refreshed == null) {
                runBlocking { store.clear() }
                onSignedOut()
                return null
            }

            runBlocking { store.save(refreshed) }
            return response.request.retryWith(refreshed.accessToken)
        }
    }

    private fun Request.retryWith(access: String): Request =
        newBuilder().header("Authorization", "Bearer $access").build()
}
