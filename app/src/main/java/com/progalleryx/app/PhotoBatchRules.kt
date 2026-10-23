package com.progalleryx.app

/** Rules and validations for multi-photo batch processing and export. */
object PhotoBatchRules {
    const val MIN_BATCH_SIZE = 1
    const val MAX_BATCH_SIZE = 25

    fun validBatchSize(size: Int): Boolean = size in MIN_BATCH_SIZE..MAX_BATCH_SIZE

    fun generateBatchFileName(index: Int, originalName: String?, prefix: String = "PGX_"): String {
        val cleanOriginal = originalName
            ?.substringBeforeLast('.')
            ?.trim()
            ?.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            ?.trim('_')
            ?.take(40)
            .orEmpty()
        val base = if (cleanOriginal.isNotBlank()) cleanOriginal else "photo"
        return "${prefix}${base}_${index + 1}.jpg"
    }

    fun adjustmentsFromPreset(
        preset: FilterPreset,
        jpegQuality: Int = 92,
        maxDimension: Int = 2400
    ): PhotoAdjustments {
        require(jpegQuality in 70..100)
        require(maxDimension in listOf(1200, 2400, 3600))
        return PhotoAdjustments(
            brightness = preset.brightness,
            contrast = preset.contrast,
            saturation = preset.saturation,
            temperature = preset.temperature,
            tint = preset.tint,
            jpegQuality = jpegQuality,
            maxDimension = maxDimension
        )
    }
}
