package com.phonecut.subtitle

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

object SrtParser {
    private val timePattern = Pattern.compile(
        "(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})"
    )

    fun parse(inputStream: InputStream): List<SubtitleItem> {
        val subtitles = mutableListOf<SubtitleItem>()
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        
        var line: String?
        var state = 0
        var currentSubtitle: SubtitleItem? = null
        val textBuilder = StringBuilder()

        while (reader.readLine().also { line = it } != null) {
            val currentLine = line!!.trim()
            
            when (state) {
                0 -> {
                    if (currentLine.isNotEmpty() && currentLine.all { it.isDigit() }) {
                        currentSubtitle = SubtitleItem()
                        state = 1
                    }
                }
                1 -> {
                    val matcher = timePattern.matcher(currentLine)
                    if (matcher.find()) {
                        val startH = matcher.group(1)!!.toInt()
                        val startM = matcher.group(2)!!.toInt()
                        val startS = matcher.group(3)!!.toInt()
                        val startMs = matcher.group(4)!!.toInt()
                        val endH = matcher.group(5)!!.toInt()
                        val endM = matcher.group(6)!!.toInt()
                        val endS = matcher.group(7)!!.toInt()
                        val endMs = matcher.group(8)!!.toInt()
                        
                        currentSubtitle?.startTimeMs = timeToMs(startH, startM, startS, startMs)
                        currentSubtitle?.endTimeMs = timeToMs(endH, endM, endS, endMs)
                        state = 2
                        textBuilder.clear()
                    }
                }
                2 -> {
                    if (currentLine.isEmpty()) {
                        if (currentSubtitle != null && textBuilder.isNotEmpty()) {
                            currentSubtitle.text = textBuilder.toString().trim()
                            subtitles.add(currentSubtitle)
                        }
                        currentSubtitle = null
                        state = 0
                    } else {
                        if (textBuilder.isNotEmpty()) {
                            textBuilder.append("\n")
                        }
                        textBuilder.append(currentLine)
                    }
                }
            }
        }
        
        if (currentSubtitle != null && textBuilder.isNotEmpty()) {
            currentSubtitle.text = textBuilder.toString().trim()
            subtitles.add(currentSubtitle)
        }
        
        reader.close()
        return subtitles
    }

    private fun timeToMs(hours: Int, minutes: Int, seconds: Int, millis: Int): Long {
        return ((hours * 3600L + minutes * 60L + seconds) * 1000L) + millis
    }

    fun formatTime(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        val milliseconds = ms % 1000
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
    }

    fun toSrt(subtitles: List<SubtitleItem>): String {
        val sb = StringBuilder()
        for ((index, subtitle) in subtitles.withIndex()) {
            sb.append(index + 1).append("\n")
            sb.append(formatTime(subtitle.startTimeMs))
            sb.append(" --> ")
            sb.append(formatTime(subtitle.endTimeMs))
            sb.append("\n")
            sb.append(subtitle.text).append("\n\n")
        }
        return sb.toString()
    }
}
