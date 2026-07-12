package com.phonecut.editor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.phonecut.app.databinding.ActivityVideoEditorBinding
import com.phonecut.subtitle.*

class VideoEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoEditorBinding
    private var player: ExoPlayer? = null
    private val subtitleManager = SubtitleManager()
    private var currentSubtitleAdapter: SubtitleListAdapter? = null
    private var videoUri: Uri? = null
    private var isPlaying = false
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 50)
        }
    }

    private val openSrtLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { importSrtFile(it) }
    }

    private val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startAutoSubtitle()
        } else {
            Toast.makeText(this, "需要录音权限才能识别语音", Toast.LENGTH_SHORT).show()
        }
    }

    private val subtitleEditLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val subtitleId = data?.getLongExtra(SubtitleEditActivity.EXTRA_SUBTITLE_ID, -1) ?: -1
            if (subtitleId != -1L) {
                val updatedSubtitle = data.getParcelableExtra<SubtitleItem>(SubtitleEditActivity.EXTRA_SUBTITLE)
                updatedSubtitle?.let {
                    subtitleManager.updateSubtitle(it)
                    currentSubtitleAdapter?.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoUri = intent.data
        if (videoUri == null) {
            Toast.makeText(this, "未获取到视频", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupPlayer()
        setupSubtitleList()
        setupControls()
    }

    private fun setupPlayer() {
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        
        videoUri?.let { uri ->
            val mediaItem = MediaItem.fromUri(uri)
            player?.setMediaItem(mediaItem)
            player?.prepare()
        }

        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                updatePlayPauseButton()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val duration = player?.duration ?: 0L
                    binding.seekBar.max = duration.toInt()
                    binding.tvTotalTime.text = formatTime(duration)
                }
            }
        })
    }

    private fun setupSubtitleList() {
        currentSubtitleAdapter = SubtitleListAdapter(subtitleManager.getSubtitles())
        currentSubtitleAdapter?.onItemClick = { subtitle ->
            editSubtitle(subtitle)
        }
        currentSubtitleAdapter?.onItemDelete = { subtitle ->
            subtitleManager.removeSubtitle(subtitle)
            currentSubtitleAdapter?.notifyDataSetChanged()
        }
        
        binding.rvSubtitles.layoutManager = LinearLayoutManager(this)
        binding.rvSubtitles.adapter = currentSubtitleAdapter
    }

    private fun setupControls() {
        binding.btnPlayPause.setOnClickListener {
            togglePlayPause()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    player?.seekTo(progress.toLong())
                    binding.tvCurrentTime.text = formatTime(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnAutoSubtitle.setOnClickListener {
            checkAudioPermission()
        }

        binding.btnImportSubtitle.setOnClickListener {
            importSrtFile()
        }

        binding.btnAddSubtitle.setOnClickListener {
            addNewSubtitle()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnExport.setOnClickListener {
            exportVideo()
        }
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            player?.pause()
        } else {
            player?.play()
        }
    }

    private fun updatePlayPauseButton() {
        if (isPlaying) {
            binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun updateProgress() {
        val currentPosition = player?.currentPosition ?: 0L
        binding.seekBar.progress = currentPosition.toInt()
        binding.tvCurrentTime.text = formatTime(currentPosition)
        
        val activeSubtitles = subtitleManager.getSubtitleAtTime(currentPosition)
        if (activeSubtitles.isNotEmpty()) {
            val current = activeSubtitles.first()
            binding.subtitleView.visibility = View.VISIBLE
            binding.subtitleView.setSubtitle(current, currentPosition)
        } else {
            binding.subtitleView.visibility = View.GONE
        }
    }

    private fun formatTime(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            startAutoSubtitle()
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startAutoSubtitle() {
        binding.tvStatus.visibility = View.VISIBLE
        binding.tvStatus.text = getString(R.string.recognizing)
        
        val manager = SpeechRecognizerManager(this)
        manager.setOnResultListener { subtitles ->
            subtitleManager.addSubtitles(subtitles)
            currentSubtitleAdapter?.notifyDataSetChanged()
            binding.tvStatus.visibility = View.GONE
            Toast.makeText(this, "识别完成，共${subtitles.size}条字幕", Toast.LENGTH_SHORT).show()
        }
        manager.setOnErrorListener { error ->
            binding.tvStatus.visibility = View.GONE
            Toast.makeText(this, "识别失败：$error", Toast.LENGTH_SHORT).show()
        }
        manager.startRecognition()
        
        handler.postDelayed({
            manager.stopRecognition()
            manager.destroy()
        }, 30000)
    }

    private fun importSrtFile() {
        openSrtLauncher.launch(arrayOf("application/x-subrip", "text/plain", "*/*"))
    }

    private fun importSrtFile(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val subtitles = SrtParser.parse(inputStream)
                subtitleManager.addSubtitles(subtitles)
                currentSubtitleAdapter?.notifyDataSetChanged()
                Toast.makeText(this, "导入成功，共${subtitles.size}条字幕", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "导入失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addNewSubtitle() {
        val currentTime = player?.currentPosition ?: 0L
        val newSubtitle = SubtitleItem(
            startTimeMs = currentTime,
            endTimeMs = currentTime + 3000,
            text = "新字幕"
        )
        subtitleManager.addSubtitle(newSubtitle)
        currentSubtitleAdapter?.notifyDataSetChanged()
        editSubtitle(newSubtitle)
    }

    private fun editSubtitle(subtitle: SubtitleItem) {
        val intent = Intent(this, SubtitleEditActivity::class.java).apply {
            putExtra(SubtitleEditActivity.EXTRA_SUBTITLE, subtitle)
            putExtra(SubtitleEditActivity.EXTRA_VIDEO_DURATION, player?.duration ?: 0L)
        }
        subtitleEditLauncher.launch(intent)
    }

    private fun exportVideo() {
        Toast.makeText(this, "导出功能需要FFmpeg支持，此处为演示", Toast.LENGTH_LONG).show()
    }

    override fun onStart() {
        super.onStart()
        handler.post(updateRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
