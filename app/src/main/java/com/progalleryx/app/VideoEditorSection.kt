package com.progalleryx.app

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Advanced Video Studio 2.0 Entry Section.
 * Seamlessly integrates single/multi-clip editing, interactive timeline, timed overlays,
 * audio/music mixing, visual tuning, and non-destructive Media3 export.
 */
@Composable
fun VideoEditorSection(
    busy: Boolean,
    onBusyChange: (Boolean) -> Unit,
    onMessageChange: (String) -> Unit,
    initialVideoUri: Uri? = null,
    onClose: (() -> Unit)? = null,
    isArabic: Boolean = true,
    modifier: Modifier = Modifier
) {
    AdvancedVideoStudio(
        initialVideoUri = initialVideoUri,
        busy = busy,
        onBusyChange = onBusyChange,
        onMessageChange = onMessageChange,
        onClose = { onClose?.invoke() },
        isArabic = isArabic,
        modifier = modifier
    )
}
