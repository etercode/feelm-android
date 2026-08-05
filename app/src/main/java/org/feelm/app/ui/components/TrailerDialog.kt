package org.feelm.app.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.feelm.app.R
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import org.feelm.app.ui.theme.OnImage

/**
 * The trailer, played in the app.
 *
 * A WebView around YouTube's iframe embed, which is the only sanctioned way to
 * play a YouTube video inside an app — the old YouTube Android Player API is
 * long dead, and scraping a stream URL violates the terms the videos are
 * served under.
 *
 * Two things decide whether the player actually loads, and both fail as the
 * same unhelpful "Video unavailable. Watch on YouTube":
 *
 *  - The base URL has to be a real domain. `about:blank` has no origin and
 *    `https://www.youtube.com` is worse than none, because claiming to *be*
 *    YouTube is exactly what a spoofed embed looks like. Feelm's own domain is
 *    honest and accepted.
 *  - Android tags its WebView user-agent with `; wv`, and YouTube refuses
 *    embedded playback to user-agents carrying it.
 *
 * Some videos are embed-disabled by whoever uploaded them, which no client-side
 * fix can change — hence the button out to the YouTube app.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrailerDialog(videoKey: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val html = remember(videoKey) {
        """
        <!doctype html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <style>
              html, body { margin:0; padding:0; background:#000; height:100%; }
              iframe { border:0; width:100%; height:100%; display:block; }
            </style>
          </head>
          <body>
            <iframe
              src="https://www.youtube.com/embed/$videoKey?autoplay=1&playsinline=1&rel=0"
              allow="autoplay; encrypted-media; picture-in-picture"
              allowfullscreen></iframe>
          </body>
        </html>
        """.trimIndent()
    }

    // Held so it can be destroyed on the way out. A WebView that keeps playing
    // after its dialog closes is audio with no window to pause it from.
    val webViewRef = remember { arrayOfNulls<WebView>(1) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef[0]?.apply {
                loadUrl("about:blank")
                destroy()
            }
            webViewRef[0] = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xF20A0C12)),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        // Without this the embed waits for a tap that the
                        // autoplay parameter has already promised will not come.
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.domStorageEnabled = true
                        // Drop the WebView marker; YouTube refuses embedded
                        // playback to user-agents carrying it.
                        settings.userAgentString =
                            settings.userAgentString.replace("; wv", "")
                        setBackgroundColor(android.graphics.Color.BLACK)
                        // Needed for fullscreen and for the player's own chrome.
                        webChromeClient = WebChromeClient()
                        loadDataWithBaseURL(
                            "https://feelm.org",
                            html,
                            "text/html",
                            "utf-8",
                            null,
                        )
                        webViewRef[0] = this
                    }
                },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = OnImage)
            }

            // The way out when the upload itself is embed-disabled: no client
            // can play those, and a dead black rectangle is a worse answer than
            // a link that works.
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://www.youtube.com/watch?v=$videoKey".toUri(),
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            ) {
                Text(stringResource(R.string.app_openYoutube), color = OnImage)
            }
        }
    }
}
