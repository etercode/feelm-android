package org.feelm.app.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.feelm.app.data.api.TokenResponse

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "feelm_auth")

data class Tokens(val access: String, val refresh: String)

/**
 * Where the session lives between launches.
 *
 * Plain DataStore in the app's private directory — not an extra encryption
 * layer on top. On every device this app runs on (minSdk 26) that directory is
 * already encrypted at rest by the platform and unreadable by other apps, and
 * the tokens are short-lived and revocable server-side. Wrapping it in
 * EncryptedSharedPreferences would add a keystore round trip to every request
 * to protect against an attacker who, by definition, already has root.
 */
class TokenStore(private val context: Context) {

    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")

    val tokensFlow: Flow<Tokens?> = context.authDataStore.data.map { prefs ->
        val access = prefs[accessKey]
        val refresh = prefs[refreshKey]
        if (access != null && refresh != null) Tokens(access, refresh) else null
    }

    suspend fun tokens(): Tokens? = tokensFlow.first()

    /**
     * For OkHttp's interceptor and authenticator, which are synchronous by
     * contract and always run off the main thread. DataStore keeps the decoded
     * preferences in memory after the first read, so this is a lock, not IO.
     */
    fun tokensBlocking(): Tokens? = runBlocking { tokens() }

    suspend fun save(token: TokenResponse) {
        context.authDataStore.edit { prefs ->
            prefs[accessKey] = token.accessToken
            prefs[refreshKey] = token.refreshToken
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { prefs ->
            prefs.remove(accessKey)
            prefs.remove(refreshKey)
        }
    }
}
