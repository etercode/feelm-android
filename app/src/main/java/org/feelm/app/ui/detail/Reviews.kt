package org.feelm.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.feelm.app.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.feelm.app.data.api.CommunityRating
import org.feelm.app.data.api.Review
import org.feelm.app.ui.components.RATING_STEP
import org.feelm.app.ui.components.Stars
import org.feelm.app.ui.mediaUrl
import org.feelm.app.ui.theme.LocalFeelmColors

/**
 * What Feelm's own members thought, and a box to add to it.
 *
 * Your own review is lifted out of the list and shown as an editor, because
 * the alternative — finding your name among the others and tapping edit — is
 * how people end up writing a second review instead of fixing the first.
 */
@Composable
fun ReviewsSection(
    reviews: List<Review>,
    myReview: Review?,
    communityRating: CommunityRating?,
    signedIn: Boolean,
    onSave: (rating: Double, body: String) -> Unit,
    onDelete: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feelm = LocalFeelmColors.current

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.work_reviews),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            communityRating?.takeIf { it.count > 0 }?.let { rating ->
                Stars(rating = rating.average, size = 14.dp)
                Text(
                    text = if (rating.count == 1) "1 rating" else "${rating.count} ratings",
                    style = MaterialTheme.typography.labelMedium,
                    color = feelm.faint,
                )
            }
        }

        if (signedIn) {
            ReviewEditor(existing = myReview, onSave = onSave, onDelete = onDelete)
        } else {
            TextButton(onClick = onSignIn) { Text(stringResource(R.string.app_signInReview)) }
        }

        if (reviews.isEmpty() && myReview == null) {
            Text(
                text = stringResource(R.string.app_noReviews),
                style = MaterialTheme.typography.bodySmall,
                color = feelm.faint,
            )
        }

        reviews.forEach { review -> ReviewCard(review) }
    }
}

@Composable
private fun ReviewEditor(
    existing: Review?,
    onSave: (Double, String) -> Unit,
    onDelete: () -> Unit,
) {
    val feelm = LocalFeelmColors.current
    // Keyed on the saved review so a successful write refreshes the box rather
    // than leaving the old draft sitting in it.
    var rating by remember(existing) { mutableDoubleStateOf(existing?.rating ?: 0.0) }
    var body by remember(existing) { mutableStateOf(existing?.body.orEmpty()) }
    var open by remember(existing) { mutableStateOf(false) }

    /*
     * A saved review is shown, not left sitting in an open textarea.
     *
     * The editor used to stay open with the text still in it, which is exactly
     * what an unsaved draft looks like — so a successful save was
     * indistinguishable from one that had done nothing. Reading it back is the
     * receipt.
     */
    if (existing != null && !open) {
        SavedReview(
            review = existing,
            onEdit = { open = true },
            onDelete = onDelete,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(if (existing == null) R.string.app_yourRating else R.string.review_yours),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Stars(
                rating = rating,
                size = 26.dp,
                // Tapping the value you already gave clears it — otherwise a
                // misplaced tap on five stars is permanent.
                onRate = { value -> rating = if (rating == value) 0.0 else value },
            )
        }

        run {
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                placeholder = { Text(stringResource(R.string.app_reviewPlaceholder)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            /*
             * Both halves are required, because the server requires them:
             * ReviewService rejects an empty body outright and takes a
             * non-nullable float for the rating. Disabling the button says so
             * before the round trip rather than after it — the API's own
             * answer is `empty_body`, which is not a sentence anybody wants
             * to read.
             */
            val complete = rating >= RATING_STEP && body.isNotBlank()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onSave(rating, body) },
                    enabled = complete,
                ) { Text(stringResource(if (existing == null) R.string.app_postReview else R.string.app_saveChanges)) }

                if (existing != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.common_delete)) }
                }

                if (existing != null) {
                    TextButton(onClick = { open = false }) { Text(stringResource(R.string.common_cancel)) }
                }

                if (!complete) {
                    Text(
                        text = stringResource(if (rating < RATING_STEP) R.string.app_needRating else R.string.app_needBody),
                        style = MaterialTheme.typography.labelSmall,
                        color = feelm.faint,
                    )
                }
            }
        }
    }
}

/** Your review as it now stands, with the way back into editing it. */
@Composable
private fun SavedReview(review: Review, onEdit: () -> Unit, onDelete: () -> Unit) {
    val feelm = LocalFeelmColors.current
    var confirmingDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.review_yours),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Stars(rating = review.rating, size = 18.dp)
        }

        review.body?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

    }

    /*
     * Deleting takes a confirmation, unlike every other control here.
     * Everything else is recoverable by redoing it; a review is something
     * somebody wrote, and there is no undo behind this button.
     */
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(R.string.review_deleteTitle)) },
            text = { Text(stringResource(R.string.review_deleteBody)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = feelm.danger),
                ) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun ReviewCard(review: Review) {
    val feelm = LocalFeelmColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AsyncImage(
                model = mediaUrl(review.user?.avatar),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Text(
                text = review.user?.name?.takeIf { it.isNotBlank() }
                    ?: review.user?.username.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            review.rating?.let { Stars(rating = it, size = 13.dp) }
        }
        review.body?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
