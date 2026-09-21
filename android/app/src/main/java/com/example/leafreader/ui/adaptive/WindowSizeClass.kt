package com.example.leafreader.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    COMPACT,   // < 600dp (Phone portrait, folded foldables)
    MEDIUM,    // 600dp - 840dp (Foldable unfolded, small tablet, portrait tablet)
    EXPANDED   // >= 840dp (Large tablet landscape, desktop window mode)
}

data class WindowAdaptiveInfo(
    val widthSizeClass: WindowWidthSizeClass,
    val windowWidthDp: Dp,
    val isLandscape: Boolean
)

@Composable
fun rememberWindowAdaptiveInfo(): WindowAdaptiveInfo {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    val widthSizeClass = when {
        screenWidthDp < 600.dp -> WindowWidthSizeClass.COMPACT
        screenWidthDp < 840.dp -> WindowWidthSizeClass.MEDIUM
        else -> WindowWidthSizeClass.EXPANDED
    }

    return WindowAdaptiveInfo(
        widthSizeClass = widthSizeClass,
        windowWidthDp = screenWidthDp,
        isLandscape = isLandscape
    )
}
