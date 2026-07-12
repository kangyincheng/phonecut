package com.phonecut.subtitle

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.phonecut.app.databinding.ActivitySubtitleEditBinding

class SubtitleEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubtitleEditBinding
    private var subtitle: SubtitleItem? = null
    private var videoDuration: Long = 0L
    private var previewTime: Long = 0L
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val previewRunnable = object : Runnable {
        override fun run() {
            updatePreview()
            handler.postDelayed(this, 50)
        }
    }

    companion object {
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_SUBTITLE_ID = "extra_subtitle_id"
        const val EXTRA_VIDEO_DURATION = "extra_video_duration"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubtitleEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        subtitle = intent.getParcelableExtra(EXTRA_SUBTITLE)
        videoDuration = intent.getLongExtra(EXTRA_VIDEO_DURATION, 0L)

        if (subtitle == null) {
            finish()
            return
        }

        initViews()
        loadSubtitleData()
        setupListeners()
    }

    private fun initViews() {
        val typeOptions = arrayOf(
            getString(R.string.static_subtitle),
            getString(R.string.scroll_up_down),
            getString(R.string.scroll_left_right)
        )
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, typeOptions)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSubtitleType.adapter = typeAdapter

        val positionOptions = arrayOf("顶部", "居中", "底部")
        val positionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, positionOptions)
        positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPosition.adapter = positionAdapter

        val colorOptions = arrayOf("白色", "黄色", "红色", "蓝色", "绿色", "黑色")
        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colorOptions)
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTextColor.adapter = colorAdapter
    }

    private fun loadSubtitleData() {
        val sub = subtitle ?: return

        binding.etSubtitleText.setText(sub.text)
        binding.etStartTime.setText(formatTimeMs(sub.startTimeMs))
        binding.etEndTime.setText(formatTimeMs(sub.endTimeMs))

        binding.spinnerSubtitleType.setSelection(
            when (sub.type) {
                SubtitleType.STATIC -> 0
                SubtitleType.SCROLL_VERTICAL -> 1
                SubtitleType.SCROLL_HORIZONTAL -> 2
            }
        )

        binding.spinnerPosition.setSelection(
            when (sub.position) {
                SubtitlePosition.TOP -> 0
                SubtitlePosition.CENTER -> 1
                SubtitlePosition.BOTTOM -> 2
            }
        )

        binding.seekFontSize.progress = sub.fontSize.toInt()
        binding.tvFontSizeValue.text = "${sub.fontSize.toInt()}sp"

        val colorIndex = getColorIndex(sub.textColor)
        binding.spinnerTextColor.setSelection(colorIndex)

        binding.switchBackground.isChecked = sub.hasBackground
        binding.seekScrollSpeed.progress = (sub.scrollSpeed * 50).toInt()
        binding.tvScrollSpeedValue.text = "x${sub.scrollSpeed}"
        binding.switchBold.isChecked = sub.bold

        updatePreviewSubtitle()
    }

    private fun getColorIndex(color: Int): Int {
        return when (color) {
            Color.WHITE -> 0
            Color.YELLOW -> 1
            Color.RED -> 2
            Color.BLUE -> 3
            Color.GREEN -> 4
            Color.BLACK -> 5
            else -> 0
        }
    }

    private fun getColorFromIndex(index: Int): Int {
        return when (index) {
            0 -> Color.WHITE
            1 -> Color.YELLOW
            2 -> Color.RED
            3 -> Color.BLUE
            4 -> Color.GREEN
            5 -> Color.BLACK
            else -> Color.WHITE
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSave.setOnClickListener {
            saveSubtitle()
        }

        binding.etSubtitleText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                subtitle?.text = s?.toString() ?: ""
                updatePreviewSubtitle()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.spinnerSubtitleType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                subtitle?.type = when (position) {
                    0 -> SubtitleType.STATIC
                    1 -> SubtitleType.SCROLL_VERTICAL
                    2 -> SubtitleType.SCROLL_HORIZONTAL
                    else -> SubtitleType.STATIC
                }
                binding.seekScrollSpeed.isEnabled = position != 0
                updatePreviewSubtitle()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerPosition.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                subtitle?.position = when (position) {
                    0 -> SubtitlePosition.TOP
                    1 -> SubtitlePosition.CENTER
                    2 -> SubtitlePosition.BOTTOM
                    else -> SubtitlePosition.BOTTOM
                }
                updatePreviewSubtitle()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.seekFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fontSize = progress.coerceAtLeast(12).toFloat()
                subtitle?.fontSize = fontSize
                binding.tvFontSizeValue.text = "${fontSize.toInt()}sp"
                updatePreviewSubtitle()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.spinnerTextColor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                subtitle?.textColor = getColorFromIndex(position)
                updatePreviewSubtitle()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.switchBackground.setOnCheckedChangeListener { _, isChecked ->
            subtitle?.hasBackground = isChecked
            updatePreviewSubtitle()
        }

        binding.seekScrollSpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val speed = (progress / 50f).coerceIn(0.2f, 3f)
                subtitle?.scrollSpeed = speed
                binding.tvScrollSpeedValue.text = "x${String.format("%.1f", speed)}"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.switchBold.setOnCheckedChangeListener { _, isChecked ->
            subtitle?.bold = isChecked
            updatePreviewSubtitle()
        }
    }

    private fun updatePreviewSubtitle() {
        val sub = subtitle ?: return
        previewTime = sub.startTimeMs
        binding.previewSubtitleView.setSubtitle(sub, previewTime)
    }

    private fun updatePreview() {
        val sub = subtitle ?: return
        previewTime += 50
        if (previewTime > sub.endTimeMs) {
            previewTime = sub.startTimeMs
        }
        binding.previewSubtitleView.updateTime(previewTime)
    }

    private fun saveSubtitle() {
        val sub = subtitle ?: return

        sub.text = binding.etSubtitleText.text.toString()
        
        val startStr = binding.etStartTime.text.toString()
        val endStr = binding.etEndTime.text.toString()
        
        sub.startTimeMs = parseTimeMs(startStr) ?: sub.startTimeMs
        sub.endTimeMs = parseTimeMs(endStr) ?: sub.endTimeMs

        if (sub.endTimeMs <= sub.startTimeMs) {
            sub.endTimeMs = sub.startTimeMs + 1000
        }

        val resultIntent = Intent().apply {
            putExtra(EXTRA_SUBTITLE_ID, sub.id)
            putExtra(EXTRA_SUBTITLE, sub)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun formatTimeMs(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)
        val millis = ms % 1000
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }

    private fun parseTimeMs(timeStr: String): Long? {
        return try {
            val parts = timeStr.split(":", ".")
            if (parts.size == 4) {
                val hours = parts[0].toLong()
                val minutes = parts[1].toLong()
                val seconds = parts[2].toLong()
                val millis = parts[3].toLong()
                hours * 3600000 + minutes * 60000 + seconds * 1000 + millis
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onStart() {
        super.onStart()
        handler.post(previewRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(previewRunnable)
    }
}
