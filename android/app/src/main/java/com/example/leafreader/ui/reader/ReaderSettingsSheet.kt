package com.example.leafreader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.leafreader.core.model.ReaderColumnMode
import com.example.leafreader.core.model.ReaderSettings
import com.example.leafreader.core.model.ReaderThemePalette
import com.example.leafreader.core.model.ReadingPageMode
import com.example.leafreader.ui.theme.AmoledBackground
import com.example.leafreader.ui.theme.DarkBackground
import com.example.leafreader.ui.theme.LightBackground
import com.example.leafreader.ui.theme.PaperBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onThemeChange: (ReaderThemePalette) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onPageModeChange: (ReadingPageMode) -> Unit,
    onColumnModeChange: (ReaderColumnMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "阅读设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Theme selector (Paper, Light, Dark, AMOLED)
            Text(text = "背景主题", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                ThemeCircleOption(
                    palette = ReaderThemePalette.PAPER,
                    bgColor = PaperBackground,
                    label = "羊皮纸",
                    isSelected = settings.themePalette == ReaderThemePalette.PAPER,
                    onClick = { onThemeChange(ReaderThemePalette.PAPER) }
                )
                ThemeCircleOption(
                    palette = ReaderThemePalette.LIGHT,
                    bgColor = LightBackground,
                    label = "浅白",
                    isSelected = settings.themePalette == ReaderThemePalette.LIGHT,
                    onClick = { onThemeChange(ReaderThemePalette.LIGHT) }
                )
                ThemeCircleOption(
                    palette = ReaderThemePalette.DARK,
                    bgColor = DarkBackground,
                    label = "夜间",
                    isSelected = settings.themePalette == ReaderThemePalette.DARK,
                    onClick = { onThemeChange(ReaderThemePalette.DARK) }
                )
                ThemeCircleOption(
                    palette = ReaderThemePalette.AMOLED,
                    bgColor = AmoledBackground,
                    label = "纯黑",
                    isSelected = settings.themePalette == ReaderThemePalette.AMOLED,
                    onClick = { onThemeChange(ReaderThemePalette.AMOLED) }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            // Font size controller
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "字号", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onFontSizeChange(settings.fontSizeSp - 1) }) {
                        Icon(Icons.Default.Remove, contentDescription = "缩小字号")
                    }
                    Text(
                        text = "${settings.fontSizeSp} sp",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    IconButton(onClick = { onFontSizeChange(settings.fontSizeSp + 1) }) {
                        Icon(Icons.Default.Add, contentDescription = "放大字号")
                    }
                }
            }

            // Line spacing
            Text(
                text = "行距: ${(settings.lineSpacingMultiplier * 10).toInt() / 10f}x",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Slider(
                value = settings.lineSpacingMultiplier,
                onValueChange = onLineSpacingChange,
                valueRange = 1.2f..2.2f,
                steps = 4
            )

            // Page mode chips
            Text(text = "翻页模式", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                FilterChip(
                    selected = settings.pageMode == ReadingPageMode.PAGED,
                    onClick = { onPageModeChange(ReadingPageMode.PAGED) },
                    label = { Text("左右仿真翻页") }
                )
                FilterChip(
                    selected = settings.pageMode == ReadingPageMode.SCROLL,
                    onClick = { onPageModeChange(ReadingPageMode.SCROLL) },
                    label = { Text("平滑上下滚动") }
                )
            }

            // Column mode (Tablet / Adaptive)
            Text(text = "排版分栏 (平板与宽屏)", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                FilterChip(
                    selected = settings.columnMode == ReaderColumnMode.AUTO,
                    onClick = { onColumnModeChange(ReaderColumnMode.AUTO) },
                    label = { Text("自动 (自适应)") }
                )
                FilterChip(
                    selected = settings.columnMode == ReaderColumnMode.SINGLE,
                    onClick = { onColumnModeChange(ReaderColumnMode.SINGLE) },
                    label = { Text("单栏") }
                )
                FilterChip(
                    selected = settings.columnMode == ReaderColumnMode.DUAL,
                    onClick = { onColumnModeChange(ReaderColumnMode.DUAL) },
                    label = { Text("双栏") }
                )
            }
        }
    }
}

@Composable
private fun ThemeCircleOption(
    palette: ReaderThemePalette,
    bgColor: Color,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
