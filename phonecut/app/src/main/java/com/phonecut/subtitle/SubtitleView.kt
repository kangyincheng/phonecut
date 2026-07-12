package com.phonecut.subtitle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

class SubtitleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        isFakeBoldText = false
    }

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#CC000000")
    }

    private var currentSubtitle: SubtitleItem? = null
    private var currentTimeMs: Long = 0
    private var scrollOffsetX: Float = 0f
    private var scrollOffsetY: Float = 0f
    private val textBounds = Rect()

    fun setSubtitle(subtitle: SubtitleItem?, timeMs: Long) {
        currentSubtitle = subtitle
        currentTimeMs = timeMs
        calculateScrollOffset()
        invalidate()
    }

    fun updateTime(timeMs: Long) {
        currentTimeMs = timeMs
        calculateScrollOffset()
        invalidate()
    }

    private fun calculateScrollOffset() {
        val subtitle = currentSubtitle ?: return
        val progress = subtitle.getProgress(currentTimeMs)
        
        when (subtitle.type) {
            SubtitleType.STATIC -> {
                scrollOffsetX = 0f
                scrollOffsetY = 0f
            }
            SubtitleType.SCROLL_HORIZONTAL -> {
                val textWidth = getTextWidth()
                scrollOffsetX = (width + textWidth) * progress - textWidth
                scrollOffsetY = 0f
            }
            SubtitleType.SCROLL_VERTICAL -> {
                val textHeight = getTextHeight()
                val lines = getLineCount()
                val totalHeight = textHeight * lines * 1.3f
                scrollOffsetX = 0f
                scrollOffsetY = (height + totalHeight) * progress - totalHeight
            }
        }
    }

    private fun getTextWidth(): Float {
        val subtitle = currentSubtitle ?: return 0f
        textPaint.textSize = subtitle.fontSize * resources.displayMetrics.density
        textPaint.isFakeBoldText = subtitle.bold
        var maxWidth = 0f
        for (line in subtitle.text.lines()) {
            val width = textPaint.measureText(line)
            if (width > maxWidth) maxWidth = width
        }
        return maxWidth
    }

    private fun getTextHeight(): Float {
        val subtitle = currentSubtitle ?: return 0f
        textPaint.textSize = subtitle.fontSize * resources.displayMetrics.density
        textPaint.getTextBounds("测试", 0, 2, textBounds)
        return textBounds.height().toFloat()
    }

    private fun getLineCount(): Int {
        return currentSubtitle?.text?.lines()?.size ?: 0
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val subtitle = currentSubtitle ?: return

        textPaint.textSize = subtitle.fontSize * resources.displayMetrics.density
        textPaint.color = subtitle.textColor
        textPaint.isFakeBoldText = subtitle.bold
        backgroundPaint.color = subtitle.backgroundColor

        val lines = subtitle.text.lines()
        val lineHeight = getTextHeight() * 1.3f

        val textWidth = getTextWidth()

        when (subtitle.type) {
            SubtitleType.STATIC -> {
                drawStaticSubtitle(canvas, lines, lineHeight, subtitle)
            }
            SubtitleType.SCROLL_HORIZONTAL -> {
                drawHorizontalScrollSubtitle(canvas, lines, lineHeight, subtitle)
            }
            SubtitleType.SCROLL_VERTICAL -> {
                drawVerticalScrollSubtitle(canvas, lines, lineHeight, subtitle)
            }
        }
    }

    private fun drawStaticSubtitle(canvas: Canvas, lines: List<String>, lineHeight: Float, subtitle: SubtitleItem) {
        val totalHeight = lineHeight * lines.size
        val baseY = when (subtitle.position) {
            SubtitlePosition.TOP -> totalHeight + paddingTop
            SubtitlePosition.CENTER -> height / 2f + totalHeight / 2f
            SubtitlePosition.BOTTOM -> height - paddingBottom - lineHeight * 0.3f
        }

        for ((i, line) in lines.withIndex()) {
            val y = baseY - (lines.size - 1 - i) * lineHeight
            val textWidth = textPaint.measureText(line)
            val x = (width - textWidth) / 2f

            if (subtitle.hasBackground) {
                val bgLeft = x - 20f
                val bgRight = x + textWidth + 20f
                val bgTop = y - lineHeight * 0.9f
                val bgBottom = y + lineHeight * 0.2f
                canvas.drawRect(bgLeft, bgTop, bgRight, bgBottom, backgroundPaint)
            }

            canvas.drawText(line, x, y, textPaint)
        }
    }

    private fun drawHorizontalScrollSubtitle(canvas: Canvas, lines: List<String>, lineHeight: Float, subtitle: SubtitleItem) {
        val baseY = when (subtitle.position) {
            SubtitlePosition.TOP -> lineHeight + paddingTop
            SubtitlePosition.CENTER -> height / 2f + lineHeight / 2f
            SubtitlePosition.BOTTOM -> height - paddingBottom - lineHeight * 0.3f
        }

        val allText = lines.joinToString("   ")
        val textWidth = textPaint.measureText(allText)

        val x = scrollOffsetX

        if (subtitle.hasBackground) {
            val bgTop = baseY - lineHeight * 0.9f
            val bgBottom = baseY + lineHeight * 0.2f
            canvas.drawRect(0f, bgTop, width.toFloat(), bgBottom, backgroundPaint)
        }

        canvas.drawText(allText, x, baseY, textPaint)
    }

    private fun drawVerticalScrollSubtitle(canvas: Canvas, lines: List<String>, lineHeight: Float, subtitle: SubtitleItem) {
        val totalHeight = lineHeight * lines.size
        val textWidth = getTextWidth()
        val x = (width - textWidth) / 2f

        val baseY = scrollOffsetY + lineHeight

        if (subtitle.hasBackground) {
            val bgLeft = x - 20f
            val bgRight = x + textWidth + 20f
            canvas.drawRect(bgLeft, 0f, bgRight, height.toFloat(), backgroundPaint)
        }

        for ((i, line) in lines.withIndex()) {
            val y = baseY + i * lineHeight
            if (y > -lineHeight && y < height + lineHeight) {
                val lineWidth = textPaint.measureText(line)
                val lineX = (width - lineWidth) / 2f
                canvas.drawText(line, lineX, y, textPaint)
            }
        }
    }
}
