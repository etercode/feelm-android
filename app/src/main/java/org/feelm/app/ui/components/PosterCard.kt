package org.feelm.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.feelm.app.data.api.WorkSummary
import org.feelm.app.ui.LocalSeenState
import org.feelm.app.ui.metaLine
import org.feelm.app.ui.score
import org.feelm.app.ui.theme.LocalFeelmColors
import org.feelm.app.ui.theme.OnImage

/** The standard poster width in a rail. Grids override it to fill their cell. */
val PosterWidth = 124.dp

/**
 * One title in a rail or a grid.
 *
 * Artwork does the work: a 2:3 poster, the title under it, and a fact line
 * under that. The badges are the only things allowed on top of the image, and
 * only one of them shows at a time — a title is either newly crawled or not
 * yet out, never both worth saying at once.
 */
@Composable
fun PosterCard(
    work: WorkSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feelm = LocalFeelmColors.current

    Column(
        modifier = modifier
            .width(PosterWidth)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = work.poster,
                contentDescription = work.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            // One badge at a time. A title is either not out yet or newly
            // crawled; saying both at once is two labels competing over the
            // same corner of the same poster.
            if (work.isUpcoming) {
                Badge(
                    text = "SOON",
                    background = MaterialTheme.colorScheme.primary,
                    foreground = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                )
            } else if (LocalSeenState.current.isNew(work)) {
                Badge(
                    text = "NEW",
                    background = feelm.new,
                    foreground = OnImage,
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                )
            }

            work.score()?.takeIf { it > 0 }?.let { score ->
                ScorePill(
                    score = score,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                )
            }
        }

        Text(
            text = work.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
        )

        val meta = work.metaLine()
        if (meta.isNotEmpty()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = feelm.faint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun Badge(
    text: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        fontSize = 9.sp,
        color = foreground,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

/**
 * The score, over the poster.
 *
 * Sat on a translucent slab rather than straight on the artwork: a poster is
 * an arbitrary image, and white text on it is legible over a night sky and
 * invisible over a snowfield.
 */
@Composable
fun ScorePill(score: Int, modifier: Modifier = Modifier) {
    val feelm = LocalFeelmColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xCC0A0C12))
            .padding(horizontal = 5.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = feelm.star,
            modifier = Modifier.size(10.dp),
        )
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = OnImage,
        )
    }
}
