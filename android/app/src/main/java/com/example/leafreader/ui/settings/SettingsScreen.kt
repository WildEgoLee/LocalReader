package com.example.leafreader.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.leafreader.core.model.ReaderColumnMode
import com.example.leafreader.core.model.ReaderSettings
import com.example.leafreader.core.model.ReaderThemePalette
import com.example.leafreader.core.model.ReadingPageMode
import com.example.leafreader.ui.adaptive.WindowAdaptiveInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: ReaderSettings,
    adaptiveInfo: WindowAdaptiveInfo,
    onThemeChange: (ReaderThemePalette) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onMarginChange: (Int) -> Unit,
    onPageModeChange: (ReadingPageMode) -> Unit,
    onColumnModeChange: (ReaderColumnMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("阅读与全局设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "默认阅读排版",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Theme options
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("默认色彩主题", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        FilterChip(
                            selected = settings.themePalette == ReaderThemePalette.PAPER,
                            onClick = { onThemeChange(ReaderThemePalette.PAPER) },
                            label = { Text("羊皮纸") }
                        )
                        FilterChip(
                            selected = settings.themePalette == ReaderThemePalette.LIGHT,
                            onClick = { onThemeChange(ReaderThemePalette.LIGHT) },
                            label = { Text("浅白") }
                        )
                        FilterChip(
                            selected = settings.themePalette == ReaderThemePalette.DARK,
                            onClick = { onThemeChange(ReaderThemePalette.DARK) },
                            label = { Text("夜间") }
                        )
                        FilterChip(
                            selected = settings.themePalette == ReaderThemePalette.AMOLED,
                            onClick = { onThemeChange(ReaderThemePalette.AMOLED) },
                            label = { Text("AMOLED") }
                        )
                    }
                }
            }

            // Font & Size
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("默认字号", style = MaterialTheme.typography.labelLarge)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onFontSizeChange(settings.fontSizeSp - 1) }) {
                                Icon(Icons.Default.Remove, contentDescription = "缩小")
                            }
                            Text(
                                text = "${settings.fontSizeSp} sp",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = { onFontSizeChange(settings.fontSizeSp + 1) }) {
                                Icon(Icons.Default.Add, contentDescription = "放大")
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("行间距: ${(settings.lineSpacingMultiplier * 10).toInt() / 10f}x", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = settings.lineSpacingMultiplier,
                        onValueChange = onLineSpacingChange,
                        valueRange = 1.2f..2.2f
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Text("页面水平边距: ${settings.horizontalMarginDp} dp", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = settings.horizontalMarginDp.toFloat(),
                        onValueChange = { onMarginChange(it.toInt()) },
                        valueRange = 12f..48f
                    )
                }
            }

            // Modes
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("翻页与分栏行为", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = settings.pageMode == ReadingPageMode.PAGED,
                            onClick = { onPageModeChange(ReadingPageMode.PAGED) },
                            label = { Text("左右仿真翻页") }
                        )
                        FilterChip(
                            selected = settings.pageMode == ReadingPageMode.SCROLL,
                            onClick = { onPageModeChange(ReadingPageMode.SCROLL) },
                            label = { Text("平滑连续滚动") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("大屏分栏模式 (平板/折叠屏/桌面)", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                        FilterChip(
                            selected = settings.columnMode == ReaderColumnMode.AUTO,
                            onClick = { onColumnModeChange(ReaderColumnMode.AUTO) },
                            label = { Text("自适应双栏") }
                        )
                        FilterChip(
                            selected = settings.columnMode == ReaderColumnMode.SINGLE,
                            onClick = { onColumnModeChange(ReaderColumnMode.SINGLE) },
                            label = { Text("强制单栏") }
                        )
                    }
                }
            }

            // Architecture info
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("架构与隐私信息", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "LeafReader 坚守 Local-First 原则：\n• 无网络上报、无第三方统计、无云端账号\n• 书架数据保存在本地 Room SQLite 数据库\n• 阅读偏好保存在 Jetpack DataStore\n• 阅读位置采用逻辑 Locator (章节 + 字符偏移量)，不依赖物理分页\n• 采用 MVVM + Repository + ReaderEngine 架构，TXT 与 Readium EPUB 解耦",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
