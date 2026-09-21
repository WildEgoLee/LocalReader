package com.example.leafreader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leafreader.core.datastore.ReaderPreferencesRepository
import com.example.leafreader.core.model.ReaderColumnMode
import com.example.leafreader.core.model.ReaderSettings
import com.example.leafreader.core.model.ReaderThemePalette
import com.example.leafreader.core.model.ReadingPageMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: ReaderPreferencesRepository
) : ViewModel() {

    val settings: StateFlow<ReaderSettings> = preferencesRepository.readerSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReaderSettings()
        )

    fun updateTheme(theme: ReaderThemePalette) {
        viewModelScope.launch { preferencesRepository.updateTheme(theme) }
    }

    fun updateFontSize(sizeSp: Int) {
        viewModelScope.launch { preferencesRepository.updateFontSize(sizeSp) }
    }

    fun updateLineSpacing(spacing: Float) {
        viewModelScope.launch { preferencesRepository.updateLineSpacing(spacing) }
    }

    fun updateHorizontalMargin(marginDp: Int) {
        viewModelScope.launch { preferencesRepository.updateHorizontalMargin(marginDp) }
    }

    fun updatePageMode(mode: ReadingPageMode) {
        viewModelScope.launch { preferencesRepository.updatePageMode(mode) }
    }

    fun updateColumnMode(mode: ReaderColumnMode) {
        viewModelScope.launch { preferencesRepository.updateColumnMode(mode) }
    }
}
