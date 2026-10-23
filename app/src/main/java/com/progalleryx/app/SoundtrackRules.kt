package com.progalleryx.app

/** Millisecond-accurate source audio window validation. */
object SoundtrackRules {
    fun validWindow(offsetMs: Long, videoLengthMs: Long, audioDurationMs: Long): Boolean =
        offsetMs >= 0L && videoLengthMs >= 500L && audioDurationMs >= videoLengthMs &&
            offsetMs <= audioDurationMs - videoLengthMs
    fun endMs(offsetMs: Long, videoLengthMs: Long, audioDurationMs: Long): Long {
        require(validWindow(offsetMs, videoLengthMs, audioDurationMs))
        return offsetMs + videoLengthMs
    }
}
