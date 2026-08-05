package org.feelm.app.ui

import androidx.compose.runtime.compositionLocalOf
import org.feelm.app.data.SeenState

/**
 * The NEW-badge answer, available to any poster card without threading it
 * through every rail, grid and screen that draws one.
 *
 * A composition local rather than a parameter because this is ambient truth:
 * no caller decides it, and a card three layers inside a lazy grid should not
 * make its parents carry it down.
 */
val LocalSeenState = compositionLocalOf { SeenState() }
