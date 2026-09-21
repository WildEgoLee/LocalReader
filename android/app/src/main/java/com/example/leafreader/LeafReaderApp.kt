package com.example.leafreader

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.leafreader.core.model.Book
import com.example.leafreader.ui.adaptive.AdaptiveNavigationSuite
import com.example.leafreader.ui.adaptive.LeafDestination
import com.example.leafreader.ui.adaptive.rememberWindowAdaptiveInfo
import com.example.leafreader.ui.bookshelf.BookshelfScreen
import com.example.leafreader.ui.bookshelf.BookshelfViewModel
import com.example.leafreader.ui.reader.ReaderScreen
import com.example.leafreader.ui.reader.ReaderViewModel
import com.example.leafreader.ui.settings.SettingsScreen
import com.example.leafreader.ui.settings.SettingsViewModel
import com.example.leafreader.ui.theme.LeafReaderTheme

object LeafDestinations {
    const val MAIN = "main"
    const val READER = "reader"
}

/**
 * Top-level application Composable managing Navigation Compose,
 * Window Adaptive Information, and decoupled ViewModels.
 */
@Composable
fun LeafReaderApp(
    bookshelfViewModel: BookshelfViewModel,
    readerViewModel: ReaderViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val adaptiveInfo = rememberWindowAdaptiveInfo()
    val navController = rememberNavController()
    var currentTab by remember { mutableStateOf(LeafDestination.BOOKSHELF) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            bookshelfViewModel.importBookFromUri(context, it)
        }
    }

    val bookshelfState by bookshelfViewModel.uiState.collectAsState()
    val readerState by readerViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.settings.collectAsState()

    LeafReaderTheme {
        NavHost(
            navController = navController,
            startDestination = LeafDestinations.MAIN
        ) {
            composable(LeafDestinations.MAIN) {
                AdaptiveNavigationSuite(
                    adaptiveInfo = adaptiveInfo,
                    currentDestination = currentTab,
                    onNavigate = { currentTab = it }
                ) {
                    when (currentTab) {
                        LeafDestination.BOOKSHELF -> {
                            BookshelfScreen(
                                uiState = bookshelfState,
                                adaptiveInfo = adaptiveInfo,
                                onBookClick = { book ->
                                    readerViewModel.openBook(book)
                                    navController.navigate(LeafDestinations.READER)
                                },
                                onAddBookClick = {
                                    filePickerLauncher.launch(
                                        arrayOf("text/plain", "application/epub+zip", "*/*")
                                    )
                                },
                                onToggleViewMode = { bookshelfViewModel.toggleViewMode() },
                                onSearchQueryChanged = { bookshelfViewModel.onSearchQueryChanged(it) }
                            )
                        }
                        LeafDestination.SETTINGS -> {
                            SettingsScreen(
                                settings = settingsState,
                                adaptiveInfo = adaptiveInfo,
                                onThemeChange = { settingsViewModel.updateTheme(it) },
                                onFontSizeChange = { settingsViewModel.updateFontSize(it) },
                                onLineSpacingChange = { settingsViewModel.updateLineSpacing(it) },
                                onMarginChange = { settingsViewModel.updateHorizontalMargin(it) },
                                onPageModeChange = { settingsViewModel.updatePageMode(it) },
                                onColumnModeChange = { settingsViewModel.updateColumnMode(it) }
                            )
                        }
                    }
                }
            }

            composable(LeafDestinations.READER) {
                ReaderScreen(
                    uiState = readerState,
                    adaptiveInfo = adaptiveInfo,
                    onBack = { navController.popBackStack() },
                    onToggleControls = { readerViewModel.toggleControlsOverlay() },
                    onShowOverlay = { readerViewModel.showOverlay(it) },
                    onHideOverlay = { readerViewModel.hideOverlay() },
                    onPreviousPage = { readerViewModel.onPreviousPage() },
                    onNextPage = { readerViewModel.onNextPage() },
                    onSelectChapter = { readerViewModel.onSelectChapter(it) },
                    onProgressSliderChange = { readerViewModel.onProgressSliderChange(it) },
                    onThemeChange = { readerViewModel.updateTheme(it) },
                    onFontSizeChange = { readerViewModel.updateFontSize(it) },
                    onLineSpacingChange = { readerViewModel.updateLineSpacing(it) },
                    onPageModeChange = { readerViewModel.updatePageMode(it) },
                    onColumnModeChange = { readerViewModel.updateColumnMode(it) }
                )
            }
        }
    }
}
