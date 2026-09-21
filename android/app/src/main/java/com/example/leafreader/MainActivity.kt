package com.example.leafreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.leafreader.core.database.AppDatabase
import com.example.leafreader.core.datastore.ReaderPreferencesRepository
import com.example.leafreader.core.repository.BookRepositoryImpl
import com.example.leafreader.ui.bookshelf.BookshelfViewModel
import com.example.leafreader.ui.reader.ReaderViewModel
import com.example.leafreader.ui.settings.SettingsViewModel

/**
 * MainActivity acts solely as the entry-point lifecycle host.
 * Application logic, database handling, and reader session management
 * reside exclusively in ViewModels, Repositories, and Engines.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val bookRepository = BookRepositoryImpl(database.bookDao())
        val preferencesRepository = ReaderPreferencesRepository(applicationContext)

        val bookshelfViewModel = BookshelfViewModel(bookRepository)
        val readerViewModel = ReaderViewModel(bookRepository, preferencesRepository)
        val settingsViewModel = SettingsViewModel(preferencesRepository)

        setContent {
            LeafReaderApp(
                bookshelfViewModel = bookshelfViewModel,
                readerViewModel = readerViewModel,
                settingsViewModel = settingsViewModel
            )
        }
    }
}
