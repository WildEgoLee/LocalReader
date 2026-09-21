package com.example.leafreader.ui.adaptive

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class LeafDestination(val title: String) {
    BOOKSHELF("书架"),
    SETTINGS("设置")
}

@Composable
fun AdaptiveNavigationSuite(
    adaptiveInfo: WindowAdaptiveInfo,
    currentDestination: LeafDestination,
    onNavigate: (LeafDestination) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    when (adaptiveInfo.widthSizeClass) {
        WindowWidthSizeClass.COMPACT -> {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentDestination == LeafDestination.BOOKSHELF,
                            onClick = { onNavigate(LeafDestination.BOOKSHELF) },
                            icon = { Icon(Icons.Default.Book, contentDescription = "书架") },
                            label = { Text("书架") }
                        )
                        NavigationBarItem(
                            selected = currentDestination == LeafDestination.SETTINGS,
                            onClick = { onNavigate(LeafDestination.SETTINGS) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                            label = { Text("设置") }
                        )
                    }
                }
            ) { innerPadding ->
                content()
            }
        }
        WindowWidthSizeClass.MEDIUM,
        WindowWidthSizeClass.EXPANDED -> {
            Row(modifier = modifier.fillMaxSize()) {
                NavigationRail {
                    NavigationRailItem(
                        selected = currentDestination == LeafDestination.BOOKSHELF,
                        onClick = { onNavigate(LeafDestination.BOOKSHELF) },
                        icon = { Icon(Icons.Default.Book, contentDescription = "书架") },
                        label = { Text("书架") }
                    )
                    NavigationRailItem(
                        selected = currentDestination == LeafDestination.SETTINGS,
                        onClick = { onNavigate(LeafDestination.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                        label = { Text("设置") }
                    )
                }
                content()
            }
        }
    }
}
