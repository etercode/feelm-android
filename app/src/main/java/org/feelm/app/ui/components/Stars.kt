package org.feelm.app.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.feelm.app.ui.theme.LocalFeelmColors

/** Feelm rates out of five, in half steps. The server rejects anything else. */
const val MAX_RATING = 5.0
const val RATING_STEP = 0.5

/**
 * A rating out of five, in half stars.
 *
 * The scale is not a display choice — `ReviewService::validated` and
 * `ShelfService::isValidRating` both reject anything outside 0.5–5.0 or off a
 * half-step, so a control that can express other values can only produce
 * failed writes.
 *
 * When editable, which half of a star you tap decides the value: the left half
 * of the third star is 2.5, the right half is 3. Rounding a tap up to the whole
 * star would make half-star ratings unreachable on the one control meant to
 * enter them.
 */
@Composable
fun Stars(
    rating: Double?,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    onRate: ((Double) -> Unit)? = null,
) {
    val feelm = LocalFeelmColors.current
    val value = rating ?: 0.0

    Row(
        modifier = modifier.then(
            if (onRate == null) {
                Modifier
            } else {
                Modifier.pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val starWidth = this.size.width / 5f
                        val star = (offset.x / starWidth).toInt().coerceIn(0, 4)
                        val withinLeftHalf = (offset.x - star * starWidth) < starWidth / 2f
                        val tapped = star + if (withinLeftHalf) 0.5 else 1.0
                        onRate(tapped)
                    }
                }
            }
        )
    ) {
        (1..5).forEach { star ->
            val icon = when {
                value >= star -> Icons.Filled.Star
                value >= star - 0.5 -> Icons.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (value >= star - 0.5) feelm.star else feelm.faint,
                modifier = Modifier.size(size),
            )
        }
    }
}
