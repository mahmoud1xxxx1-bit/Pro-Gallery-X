package com.progalleryx.app

/** Preset filter configurations calculated via pure adjustments. */
data class FilterPreset(
    val id: String,
    val displayName: String,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val temperature: Float,
    val tint: Float
) {
    init {
        require(brightness in -100f..100f)
        require(contrast in 0.5f..1.5f)
        require(saturation in 0f..2f)
        require(temperature in -100f..100f)
        require(tint in -100f..100f)
    }
}

object PhotoFilterPresets {
    val ORIGINAL = FilterPreset("original", "الأصل", 0f, 1f, 1f, 0f, 0f)
    val VIVID = FilterPreset("vivid", "ألوان نابضة", 12f, 1.15f, 1.35f, 4f, 0f)
    val WARM = FilterPreset("warm", "دافئ (غروب)", 6f, 1.05f, 1.12f, 30f, 10f)
    val COOL = FilterPreset("cool", "بارد (ثلجي)", 2f, 1.05f, 0.95f, -30f, -8f)
    val MONO = FilterPreset("mono", "أبيض وأسود", 0f, 1.18f, 0f, 0f, 0f)
    val SEPIA = FilterPreset("sepia", "عتيق كلاسيكي", 8f, 1.05f, 0.5f, 45f, 22f)
    val CINEMA = FilterPreset("cinema", "سينمائي درامي", -8f, 1.25f, 0.88f, -15f, 14f)
    val VINTAGE = FilterPreset("vintage", "ريترو قديم", 14f, 0.90f, 0.82f, 20f, 12f)

    val all: List<FilterPreset> = listOf(
        ORIGINAL, VIVID, WARM, COOL, MONO, SEPIA, CINEMA, VINTAGE
    )

    fun find(id: String): FilterPreset? = all.firstOrNull { it.id == id }

    fun applyTo(current: PhotoAdjustments, preset: FilterPreset): PhotoAdjustments =
        current.copy(
            brightness = preset.brightness,
            contrast = preset.contrast,
            saturation = preset.saturation,
            temperature = preset.temperature,
            tint = preset.tint
        )
}
