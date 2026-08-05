package org.feelm.app.ui.settings

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.feelm.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import kotlin.math.max
import kotlin.math.min

/**
 * Pick the square.
 *
 * Drag to move, pinch to zoom, and the circle is what gets uploaded. The
 * previous version centre-cropped, which is right often enough to feel like a
 * bug when it is wrong — a face off to one side of a photo came out as an ear.
 *
 * The maths runs in image pixels, not screen pixels. `baseScale` is whatever
 * makes the shorter edge exactly fill the circle, so zoom starts at "as far out
 * as is still a full crop" and cannot go further — a crop with a transparent
 * corner is never what anybody meant.
 */
@Composable
fun AvatarCropper(
    bitmap: Bitmap,
    onCancel: () -> Unit,
    onConfirm: (Bitmap) -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF2090F1C)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val density = LocalDensity.current

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .aspectRatio(1f),
            ) {
                val viewport = with(density) { maxWidth.toPx() }
                val baseScale = viewport / min(bitmap.width, bitmap.height).toFloat()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Black)
                        .pointerInput(bitmap) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)

                                // Keep the circle covered: the image can never
                                // be dragged far enough to expose an edge.
                                val drawnWidth = bitmap.width * baseScale * scale
                                val drawnHeight = bitmap.height * baseScale * scale
                                val maxX = max(0f, (drawnWidth - viewport) / 2f)
                                val maxY = max(0f, (drawnHeight - viewport) / 2f)

                                offset = Offset(
                                    (offset.x + pan.x).coerceIn(-maxX, maxX),
                                    (offset.y + pan.y).coerceIn(-maxY, maxY),
                                )
                            }
                        },
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.None,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = baseScale * scale
                                scaleY = baseScale * scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                    )
                }

                // Deliberately outside the clipped Box so the confirm button
                // reads the same values the preview was drawn with.
                CropActions(
                    onCancel = onCancel,
                    onConfirm = {
                        onConfirm(
                            cropToCircle(
                                source = bitmap,
                                viewport = viewport,
                                baseScale = baseScale,
                                userScale = scale,
                                offset = offset,
                            )
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            Text(
                text = stringResource(R.string.app_cropHint),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA3ABBC),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun CropActions(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(bottom = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel), color = Color.White) }
        Button(onClick = onConfirm) { Text(stringResource(R.string.app_useThis)) }
    }
}

/**
 * Turn what the circle is showing into a square bitmap.
 *
 * The preview draws the image scaled about its centre and then translated, so
 * inverting that gives the source rectangle: the viewport is `viewport /
 * (baseScale * userScale)` image-pixels across, centred on the image's middle
 * shifted back by the pan.
 */
private fun cropToCircle(
    source: Bitmap,
    viewport: Float,
    baseScale: Float,
    userScale: Float,
    offset: Offset,
): Bitmap {
    val total = baseScale * userScale
    val side = (viewport / total).coerceAtMost(min(source.width, source.height).toFloat())

    val centerX = source.width / 2f - offset.x / total
    val centerY = source.height / 2f - offset.y / total

    val left = (centerX - side / 2f).coerceIn(0f, source.width - side)
    val top = (centerY - side / 2f).coerceIn(0f, source.height - side)

    val cropped = Bitmap.createBitmap(
        source,
        left.toInt(),
        top.toInt(),
        side.toInt().coerceAtLeast(1),
        side.toInt().coerceAtLeast(1),
    )

    // The server stores a small avatar; sending more than it keeps is a slower
    // upload for an identical result.
    return if (cropped.width <= AVATAR_UPLOAD_SIZE) {
        cropped
    } else {
        Bitmap.createScaledBitmap(cropped, AVATAR_UPLOAD_SIZE, AVATAR_UPLOAD_SIZE, true)
    }
}

const val AVATAR_UPLOAD_SIZE = 512
