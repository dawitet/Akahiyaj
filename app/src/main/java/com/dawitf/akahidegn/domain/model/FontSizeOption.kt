package com.dawitf.akahidegn.domain.model

/**
 * Font size options for accessibility and user preference settings
 */
enum class FontSizeOption(
    val scaleFactor: Float,
    val displayName: String,
    val description: String
) {
    SMALL(0.85f, "Small", "Compact text for more content"),
    MEDIUM(1.0f, "Medium", "Standard text size"),
    LARGE(1.15f, "Large", "Larger text for better readability"),
    EXTRA_LARGE(1.3f, "Extra Large", "Maximum text size for accessibility");

    companion object {
        fun fromScaleFactor(scaleFactor: Float): FontSizeOption {
            return values().minByOrNull { kotlin.math.abs(it.scaleFactor - scaleFactor) } ?: MEDIUM
        }
        
        fun getDefaultFontSize(): FontSizeOption = MEDIUM
        
        fun getAllFontSizes(): List<FontSizeOption> = values().toList()
    }
}
