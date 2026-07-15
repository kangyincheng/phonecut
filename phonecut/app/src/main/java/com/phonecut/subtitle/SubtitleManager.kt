package com.phonecut.subtitle

class SubtitleManager {
    private val subtitles = mutableListOf<SubtitleItem>()

    fun getSubtitles(): List<SubtitleItem> = subtitles.toList()

    fun addSubtitle(subtitle: SubtitleItem) {
        subtitles.add(subtitle)
        sortSubtitles()
    }

    fun addSubtitles(subtitleList: List<SubtitleItem>) {
        subtitles.addAll(subtitleList)
        sortSubtitles()
    }

    fun removeSubtitle(subtitle: SubtitleItem) {
        subtitles.remove(subtitle)
    }

    fun removeSubtitleById(id: Long) {
        subtitles.removeAll { it.id == id }
    }

    fun updateSubtitle(subtitle: SubtitleItem) {
        val index = subtitles.indexOfFirst { it.id == subtitle.id }
        if (index >= 0) {
            subtitles[index] = subtitle
            sortSubtitles()
        }
    }

    fun getSubtitleAtTime(currentTimeMs: Long): List<SubtitleItem> {
        return subtitles.filter { it.isActive(currentTimeMs) }
    }

    fun clear() {
        subtitles.clear()
    }

    private fun sortSubtitles() {
        subtitles.sortBy { it.startTimeMs }
    }

    fun getSubtitleById(id: Long): SubtitleItem? {
        return subtitles.find { it.id == id }
    }

    fun getCount(): Int = subtitles.size

    fun getSubtitleByIndex(index: Int): SubtitleItem? {
        return if (index in subtitles.indices) subtitles[index] else null
    }
}
