package com.example.leafreader.core.model

enum class ReaderThemePalette {
    PAPER,    // Warm parchment: #F6F1E7 bg, #332F29 text
    LIGHT,    // Clean soft light: #FAFAFA bg, #202124 text
    DARK,     // Eye-care dark: #202124 bg, #E6E1E5 text
    AMOLED    // True pitch black: #000000 bg, #DCDCDC text
}

enum class ReadingPageMode {
    PAGED,    // Horizontal page flip
    SCROLL    // Continuous vertical scroll
}

enum class ReaderColumnMode {
    AUTO,     // Automatically 2-column on expanded landscape tablet, 1-column on phone
    SINGLE,   // Force 1-column
    DUAL      // Force 2-column
}

data class ReaderSettings(
    val themePalette: ReaderThemePalette = ReaderThemePalette.PAPER,
    val fontSizeSp: Int = 18,
    val lineSpacingMultiplier: Float = 1.6f,
    val paragraphSpacingDp: Int = 12,
    val horizontalMarginDp: Int = 24,
    val fontFamily: String = "Serif",
    val pageMode: ReadingPageMode = ReadingPageMode.PAGED,
    val columnMode: ReaderColumnMode = ReaderColumnMode.AUTO,
    val volumeKeyNavigation: Boolean = false,
    val keepScreenOn: Boolean = false
)
