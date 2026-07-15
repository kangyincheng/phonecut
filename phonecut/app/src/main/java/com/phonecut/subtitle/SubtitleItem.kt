package com.phonecut.subtitle

import android.graphics.Color

enum class SubtitleType {
    STATIC,
    SCROLL_VERTICAL,
    SCROLL_HORIZONTAL
}

enum class SubtitlePosition {
    TOP,
    CENTER,
    BOTTOM
}

data class SubtitleItem(
    var id: Long = System.currentTimeMillis(),
    var text: String = "",
    var startTimeMs: Long = 0,
    var endTimeMs: Long = 0,
    var type: SubtitleType = SubtitleType.STATIC,
    var position: SubtitlePosition = SubtitlePosition.BOTTOM,
    var fontSize: Float = 24f,
    var textColor: Int = Color.WHITE,
    var backgroundColor: Int = Color.parseColor("#CC000000"),
    var hasBackground: Boolean = true,
    var scrollSpeed: Float = 1.0f,
    var bold: Boolean = false
) {
    val durationMs: Long
        get() = endTimeMs - startTimeMs

    fun isActive(currentTimeMs: Long): Boolean {
        return currentTimeMs in startTimeMs..endTimeMs
    }

    fun getProgress(currentTimeMs: Long): Float {
        if (durationMs <= 0) return 0f
        val progress = (currentTimeMs - startTimeMs).toFloat() / durationMs.toFloat()
        return progress.coerceIn(0f, 1f)
    }
}
