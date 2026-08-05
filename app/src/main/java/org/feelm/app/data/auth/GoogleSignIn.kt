package org.feelm.app.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.feelm.app.BuildConfig

/** What the sheet came back with. */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult

    /** The sheet was dismissed. Not an error — say nothing and carry on. */
    data object Cancelled : GoogleSignInResult

    data class Failed(val message: String) : GoogleSignInResult
}

/**
 * Sign in with Google, via Credential Manager.
 *
 * The app never talks to Feelm's backend here — it only obtains an ID token
 * from Google and hands it over. The server verifies the signature and the
 * audience itself, which is the only place that check means anything: a token
 * the client validated is a token the client could have invented.
 *
 * `setFilterByAuthorizedAccounts(false)` so somebody signing in for the first
 * time sees their accounts at all. Filtering to previously-authorized ones
 * shows an empty sheet to every new user, which reads as "Google is broken".
 */
class GoogleSignIn(private val context: Context) {

    suspend fun requestIdToken(activityContext: Context): GoogleSignInResult {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = CredentialManager.create(context)
                .getCredential(activityContext, request)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleSignInResult.Success(
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                )
            } else {
                GoogleSignInResult.Failed("Google returned an unexpected credential.")
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleSignInResult.Failed("No Google account is available on this device.")
        } catch (e: GetCredentialException) {
            /*
             * The overwhelmingly likely cause on a fresh install is that no
             * Android OAuth client exists for this package and signing
             * certificate, which Google reports as a generic failure. Saying so
             * beats "an unknown error occurred" when the fix is one entry in
             * the Cloud console.
             */
            GoogleSignInResult.Failed(
                e.message ?: "Google sign-in is not available for this build."
            )
        }
    }
}
