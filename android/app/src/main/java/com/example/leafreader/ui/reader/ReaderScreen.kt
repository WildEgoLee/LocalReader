package com.example.leafreader.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.leafreader.core.model.Chapter
import com.example.leafreader.core.model.ReaderColumnMode
import com.example.leafreader.core.model.ReaderThemePalette
import com.example.leafreader.core.model.ReadingPageMode
import com.example.leafreader.ui.adaptive.WindowAdaptiveInfo
import com.example.leafreader.ui.adaptive.WindowWidthSizeClass
import com.example.leafreader.ui.theme.AmoledBackground
import com.example.leafreader.ui.theme.AmoledText
import com.example.leafreader.ui.theme.DarkBackground
import com.example.leafreader.ui.theme.DarkText
import com.example.leafreader.ui.theme.LightBackground
import com.example.leafreader.ui.theme.LightText
import com.example.leafreader.ui.theme.PaperBackground
import com.example.leafreader.ui.theme.PaperText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    uiState: ReaderUiState,
    adaptiveInfo: WindowAdaptiveInfo,
    onBack: () -> Unit,
    onToggleControls: () -> Unit,
    onShowOverlay: (ReaderOverlay) -> Unit,
    onHideOverlay: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onSelectChapter: (Chapter) -> Unit,
    onProgressSliderChange: (Float) -> Unit,
    onThemeChange: (ReaderThemePalette) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onPageModeChange: (ReadingPageMode) -> Unit,
    onColumnModeChange: (ReaderColumnMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine colors from settings palette
    val (backgroundColor, textColor) = when (uiState.settings.themePalette) {
        ReaderThemePalette.PAPER -> Pair(PaperBackground, PaperText)
        ReaderThemePalette.LIGHT -> Pair(LightBackground, LightText)
        ReaderThemePalette.DARK -> Pair(DarkBackground, DarkText)
        ReaderThemePalette.AMOLED -> Pair(AmoledBackground, AmoledText)
    }

    // Determine whether 2 columns are used for reading
    val isTwoColumns = when (uiState.settings.columnMode) {
        ReaderColumnMode.AUTO -> adaptiveInfo.widthSizeClass == WindowWidthSizeClass.EXPANDED && adaptiveInfo.isLandscape
        ReaderColumnMode.DUAL -> true
        ReaderColumnMode.SINGLE -> false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Main reading container with optional supporting pane on tablet
        Row(modifier = Modifier.fillMaxSize()) {
            // Supporting TOC Pane on Tablet (Expanded)
            if (uiState.overlay == ReaderOverlay.Toc && adaptiveInfo.widthSizeClass != WindowWidthSizeClass.COMPACT) {
                AdaptiveTocContainer(
                    adaptiveInfo = adaptiveInfo,
                    chapters = uiState.tableOfContents,
                    currentChapter = uiState.currentChapter,
                    onSelectChapter = onSelectChapter,
                    onDismiss = onHideOverlay
                )
            }

            // The Reading Surface
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // 3-Zone Click Gesture Overlay (Left 25% Prev, Center 50% Controls, Right 25% Next)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val width = size.width
                                when {
                                    offset.x < width * 0.25f -> onPreviousPage()
                                    offset.x > width * 0.75f -> onNextPage()
                                    else -> onToggleControls()
                                }
                            }
                        }
                )

                // Reading Content (Single or Two-Column)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = uiState.settings.horizontalMarginDp.dp, vertical = 28.dp)
                ) {
                    // Header Chapter Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.currentChapter?.title ?: (uiState.book?.title ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${(uiState.readingProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.5f)
                        )
                    }

                    // Content text
                    if (isTwoColumns) {
                        TwoColumnReaderContent(
                            content = uiState.content,
                            textColor = textColor,
                            fontSizeSp = uiState.settings.fontSizeSp,
                            lineSpacingMultiplier = uiState.settings.lineSpacingMultiplier,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        SingleColumnReaderContent(
                            content = uiState.content,
                            textColor = textColor,
                            fontSizeSp = uiState.settings.fontSizeSp,
                            lineSpacingMultiplier = uiState.settings.lineSpacingMultiplier,
                            pageMode = uiState.settings.pageMode,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Footer Locator status
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LeafReader · 逻辑定位",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "轻触两侧翻页 · 中央呼出面板",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // Top Controls Overlay
        AnimatedVisibility(
            visible = uiState.overlay == ReaderOverlay.Controls,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.book?.title ?: "正在阅读",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回书架")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onShowOverlay(ReaderOverlay.Toc) }) {
                            Icon(Icons.Default.FormatListBulleted, contentDescription = "目录")
                        }
                        IconButton(onClick = { onShowOverlay(ReaderOverlay.Settings) }) {
                            Icon(Icons.Default.Settings, contentDescription = "阅读设置")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        }

        // Bottom Controls Overlay
        AnimatedVisibility(
            visible = uiState.overlay == ReaderOverlay.Controls,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    // Chapter & progress title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.currentChapter?.title ?: "第一章",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(uiState.readingProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Progress Slider with Prev/Next Chapter buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onPreviousPage) {
                            Icon(Icons.Default.NavigateBefore, contentDescription = "上一页/章")
                        }
                        Slider(
                            value = uiState.readingProgress,
                            onValueChange = onProgressSliderChange,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onNextPage) {
                            Icon(Icons.Default.NavigateNext, contentDescription = "下一页/章")
                        }
                    }
                }
            }
        }

        // TOC Bottom Sheet on Phone (Compact)
        if (uiState.overlay == ReaderOverlay.Toc && adaptiveInfo.widthSizeClass == WindowWidthSizeClass.COMPACT) {
            AdaptiveTocContainer(
                adaptiveInfo = adaptiveInfo,
                chapters = uiState.tableOfContents,
                currentChapter = uiState.currentChapter,
                onSelectChapter = onSelectChapter,
                onDismiss = onHideOverlay
            )
        }

        // Settings Bottom Sheet
        if (uiState.overlay == ReaderOverlay.Settings) {
            ReaderSettingsSheet(
                settings = uiState.settings,
                onThemeChange = onThemeChange,
                onFontSizeChange = onFontSizeChange,
                onLineSpacingChange = onLineSpacingChange,
                onPageModeChange = onPageModeChange,
                onColumnModeChange = onColumnModeChange,
                onDismiss = onHideOverlay
            )
        }
    }
}

@Composable
fun SingleColumnReaderContent(
    content: String,
    textColor: Color,
    fontSizeSp: Int,
    lineSpacingMultiplier: Float,
    pageMode: ReadingPageMode,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scrollModifier = if (pageMode == ReadingPageMode.SCROLL) {
        modifier.verticalScroll(scrollState)
    } else {
        modifier
    }

    Box(modifier = scrollModifier) {
        Text(
            text = content,
            color = textColor,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * lineSpacingMultiplier).sp,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Two-Column layout for Tablet landscape and Expanded screens.
 * Evokes an open physical hardcover book.
 */
@Composable
fun TwoColumnReaderContent(
    content: String,
    textColor: Color,
    fontSizeSp: Int,
    lineSpacingMultiplier: Float,
    modifier: Modifier = Modifier
) {
    // Split content visually into left and right columns
    val halfIndex = (content.length / 2).coerceIn(0, content.length)
    val leftContent = content.substring(0, halfIndex).trimEnd()
    val rightContent = content.substring(halfIndex).trimStart()

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = leftContent,
                color = textColor,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * lineSpacingMultiplier).sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxSize()
            )
        }
        Divider(modifier = Modifier.width(1.dp).fillMaxHeight(), color = textColor.copy(alpha = 0.15f))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rightContent,
                color = textColor,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * lineSpacingMultiplier).sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
