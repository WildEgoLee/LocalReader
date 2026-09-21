package com.example.leafreader.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.leafreader.core.model.Chapter
import com.example.leafreader.ui.adaptive.WindowAdaptiveInfo
import com.example.leafreader.ui.adaptive.WindowWidthSizeClass

/**
 * Adaptive Table of Contents.
 * Rendered as a ModalBottomSheet on Compact (Phone) displays,
 * and as a persistent or side Supporting Pane on Expanded (Tablet) displays.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveTocContainer(
    adaptiveInfo: WindowAdaptiveInfo,
    chapters: List<Chapter>,
    currentChapter: Chapter?,
    onSelectChapter: (Chapter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (adaptiveInfo.widthSizeClass == WindowWidthSizeClass.COMPACT) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            modifier = modifier
        ) {
            TocContent(
                chapters = chapters,
                currentChapter = currentChapter,
                onSelectChapter = {
                    onSelectChapter(it)
                    onDismiss()
                },
                onClose = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    } else {
        // Supporting side pane on tablet / expanded screen
        Surface(
            tonalElevation = 2.dp,
            modifier = modifier
                .width(320.dp)
                .fillMaxHeight()
        ) {
            TocContent(
                chapters = chapters,
                currentChapter = currentChapter,
                onSelectChapter = onSelectChapter,
                onClose = onDismiss,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun TocContent(
    chapters: List<Chapter>,
    currentChapter: Chapter?,
    onSelectChapter: (Chapter) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text(
                text = "目录 (${chapters.size} 章)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "关闭目录")
            }
        }
        Divider(modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(chapters, key = { it.id }) { chapter ->
                val isSelected = chapter.id == currentChapter?.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectChapter(chapter) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Text(
                            text = "当前",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
