package com.example.leafreader.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.leafreader.core.model.ReaderColumnMode
import com.example.leafreader.core.model.ReaderSettings
import com.example.leafreader.core.model.ReaderThemePalette
import com.example.leafreader.core.model.ReadingPageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_preferences")

/**
 * DataStore repository for global and reader UI preferences.
 */
class ReaderPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val FONT_SIZE_SP = intPreferencesKey("font_size_sp")
        val LINE_SPACING = floatPreferencesKey("line_spacing_multiplier")
        val PARAGRAPH_SPACING = intPreferencesKey("paragraph_spacing_dp")
        val HORIZONTAL_MARGIN = intPreferencesKey("horizontal_margin_dp")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val PAGE_MODE = stringPreferencesKey("page_mode")
        val COLUMN_MODE = stringPreferencesKey("column_mode")
        val VOLUME_KEY_NAV = booleanPreferencesKey("volume_key_nav")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }

    val readerSettingsFlow: Flow<ReaderSettings> = context.dataStore.data.map { preferences ->
        val themeName = preferences[PreferencesKeys.THEME_PALETTE] ?: ReaderThemePalette.PAPER.name
        val pageModeName = preferences[PreferencesKeys.PAGE_MODE] ?: ReadingPageMode.PAGED.name
        val columnModeName = preferences[PreferencesKeys.COLUMN_MODE] ?: ReaderColumnMode.AUTO.name

        ReaderSettings(
            themePalette = runCatching { ReaderThemePalette.valueOf(themeName) }.getOrDefault(ReaderThemePalette.PAPER),
            fontSizeSp = preferences[PreferencesKeys.FONT_SIZE_SP] ?: 18,
            lineSpacingMultiplier = preferences[PreferencesKeys.LINE_SPACING] ?: 1.6f,
            paragraphSpacingDp = preferences[PreferencesKeys.PARAGRAPH_SPACING] ?: 12,
            horizontalMarginDp = preferences[PreferencesKeys.HORIZONTAL_MARGIN] ?: 24,
            fontFamily = preferences[PreferencesKeys.FONT_FAMILY] ?: "Serif",
            pageMode = runCatching { ReadingPageMode.valueOf(pageModeName) }.getOrDefault(ReadingPageMode.PAGED),
            columnMode = runCatching { ReaderColumnMode.valueOf(columnModeName) }.getOrDefault(ReaderColumnMode.AUTO),
            volumeKeyNavigation = preferences[PreferencesKeys.VOLUME_KEY_NAV] ?: false,
            keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: false
        )
    }

    suspend fun updateTheme(palette: ReaderThemePalette) {
        context.dataStore.edit { it[PreferencesKeys.THEME_PALETTE] = palette.name }
    }

    suspend fun updateFontSize(sizeSp: Int) {
        context.dataStore.edit { it[PreferencesKeys.FONT_SIZE_SP] = sizeSp.coerceIn(12, 36) }
    }

    suspend fun updateLineSpacing(spacing: Float) {
        context.dataStore.edit { it[PreferencesKeys.LINE_SPACING] = spacing }
    }

    suspend fun updateHorizontalMargin(marginDp: Int) {
        context.dataStore.edit { it[PreferencesKeys.HORIZONTAL_MARGIN] = marginDp.coerceIn(8, 64) }
    }

    suspend fun updatePageMode(mode: ReadingPageMode) {
        context.dataStore.edit { it[PreferencesKeys.PAGE_MODE] = mode.name }
    }

    suspend fun updateColumnMode(mode: ReaderColumnMode) {
        context.dataStore.edit { it[PreferencesKeys.COLUMN_MODE] = mode.name }
    }
}
