package com.savestatus.pro.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.savestatus.pro.R
import com.savestatus.pro.data.StatusRepository
import com.savestatus.pro.model.StatusItem
import com.savestatus.pro.model.StatusType
import com.savestatus.pro.player.VideoPlayerManager
import com.savestatus.pro.utils.AppToast
import com.savestatus.pro.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.File

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var ivBack: ImageView
    private lateinit var ivDownload: ImageView
    private lateinit var ivShare: ImageView
    private lateinit var playerControls: View      // FrameLayout center
    private lateinit var ivPlayPause: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var tvCurrentTime: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var topBar: View
    private lateinit var bottomBar: View

    private var player: ExoPlayer? = null
    private var filePath: String = ""
    private var isActivityDestroyed = false
    private val repository by lazy { StatusRepository(applicationContext) }

    private val seekHandler   = Handler(Looper.getMainLooper())
    private val autoHideHandler = Handler(Looper.getMainLooper())
    private var controlsVisible = true
    private val AUTO_HIDE_MS = 3000L

    private val seekRunnable = object : Runnable {
        override fun run() {
            if (isActivityDestroyed) return
            player?.let { p ->
                val pos = p.currentPosition
                val dur = p.duration.takeIf { it > 0 } ?: 1L
                seekBar.progress = ((pos.toFloat() / dur) * 1000).toInt()
                tvCurrentTime.text = FileUtils.formatDuration(pos)
                tvTotalTime.text   = FileUtils.formatDuration(dur)
            }
            seekHandler.postDelayed(this, 500)
        }
    }

    private val autoHideRunnable = Runnable { if (!isActivityDestroyed) hideControls() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.activity_video_player)

        filePath = intent.getStringExtra("file_path") ?: run { finish(); return }

        bindViews()
        updateDownloadIcon()
        initPlayer()
        setupClickListeners()
        setupSeekBar()
        scheduleAutoHide()
    }

    override fun onResume() {
        super.onResume()
        if (!isActivityDestroyed) {
            player?.play()
            seekHandler.post(seekRunnable)
            scheduleAutoHide()
            updateDownloadIcon()          // re-check if user deleted the file
        }
    }

    override fun onPause() {
        super.onPause()
        player?.pause()
        seekHandler.removeCallbacks(seekRunnable)
        autoHideHandler.removeCallbacks(autoHideRunnable)
    }

    override fun onDestroy() {
        isActivityDestroyed = true
        seekHandler.removeCallbacks(seekRunnable)
        autoHideHandler.removeCallbacks(autoHideRunnable)
        player?.let { VideoPlayerManager.release(it) }
        player = null
        super.onDestroy()
    }

    // ── Bind views ───────────────────────────────────────────────

    private fun bindViews() {
        playerView     = findViewById(R.id.playerView)
        ivBack         = findViewById(R.id.ivBack)
        ivDownload     = findViewById(R.id.ivDownload)
        ivShare        = findViewById(R.id.ivShare)
        playerControls = findViewById(R.id.playerControls)
        ivPlayPause    = findViewById(R.id.ivPlayPause)
        seekBar        = findViewById(R.id.seekBar)
        tvCurrentTime  = findViewById(R.id.tvCurrentTime)
        tvTotalTime    = findViewById(R.id.tvTotalTime)
        topBar         = findViewById(R.id.topBar)
        bottomBar      = findViewById(R.id.bottomBar)
    }

    // ── Download icon state ──────────────────────────────────────

    private fun updateDownloadIcon() {
        val isDownloaded = FileUtils.isAlreadyDownloaded(File(filePath))
        ivDownload.setImageResource(if (isDownloaded) R.drawable.ic_check else R.drawable.ic_download)
    }

    // ── Player ───────────────────────────────────────────────────

    private fun initPlayer() {
        player = VideoPlayerManager.createPlayer(this)
        playerView.player = player

        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isActivityDestroyed)
                    ivPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (!isActivityDestroyed && state == Player.STATE_READY)
                    seekHandler.post(seekRunnable)
            }
        })

        VideoPlayerManager.play(player!!, Uri.fromFile(File(filePath)))
        seekHandler.post(seekRunnable)
    }

    // ── Click listeners ──────────────────────────────────────────

    private fun setupClickListeners() {
        playerView.setOnClickListener  { toggleControls() }
        ivBack.setOnClickListener      { finish() }
        ivPlayPause.setOnClickListener {
            player?.let { if (it.isPlaying) it.pause() else it.play() }
            rescheduleAutoHide()
        }
        ivDownload.setOnClickListener { handleDownload() }
        ivShare.setOnClickListener    { shareFile() }
    }

    private fun setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val dur = player?.duration?.takeIf { it > 0 } ?: return
                    player?.seekTo((progress.toFloat() / 1000 * dur).toLong())
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {
                autoHideHandler.removeCallbacks(autoHideRunnable)
            }
            override fun onStopTrackingTouch(sb: SeekBar) { rescheduleAutoHide() }
        })
    }

    // ── Controls visibility ──────────────────────────────────────

    private fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    private fun showControls() {
        controlsVisible = true
        fadeIn(topBar); fadeIn(playerControls); fadeIn(bottomBar)
        rescheduleAutoHide()
    }

    private fun hideControls() {
        controlsVisible = false
        fadeOut(topBar); fadeOut(playerControls); fadeOut(bottomBar)
    }

    private fun scheduleAutoHide() {
        autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_MS)
    }

    private fun rescheduleAutoHide() {
        autoHideHandler.removeCallbacks(autoHideRunnable)
        autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_MS)
    }

    private fun fadeIn(view: View) {
        if (view.isVisible) return
        view.visibility = View.VISIBLE
        view.startAnimation(AlphaAnimation(0f, 1f).apply { duration = 220 })
    }

    private fun fadeOut(view: View) {
        if (view.isGone) return
        val anim = AlphaAnimation(1f, 0f).apply {
            duration = 300
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(a: Animation?) {}
                override fun onAnimationRepeat(a: Animation?) {}
                override fun onAnimationEnd(a: Animation?) { view.visibility = View.GONE }
            })
        }
        view.startAnimation(anim)
    }

    // ── Download / Share ─────────────────────────────────────────

    private fun handleDownload() {
        if (!ivDownload.isEnabled) return          // rapid-click guard
        lifecycleScope.launch {
            val src = File(filePath)
            if (!src.exists()) {
                AppToast.plain(this@VideoPlayerActivity, "File not found")
                return@launch
            }
            if (FileUtils.isAlreadyDownloaded(src)) {
                AppToast.plain(this@VideoPlayerActivity, getString(R.string.already_downloaded))
                ivDownload.setImageResource(R.drawable.ic_check)
                return@launch
            }
            ivDownload.isEnabled = false           // disable while downloading
            val item = StatusItem(
                id = filePath, filePath = filePath,
                type = StatusType.VIDEO, lastModified = src.lastModified()
            )
            val success = repository.downloadStatus(item)
            ivDownload.isEnabled = true
            if (success) {
                AppToast.plain(this@VideoPlayerActivity, getString(R.string.download_success))
                ivDownload.setImageResource(R.drawable.ic_check)
            } else {
                AppToast.plain(this@VideoPlayerActivity, getString(R.string.download_failed))
            }
        }
    }

    private fun shareFile() {
        val file = File(filePath)
        if (!file.exists()) return
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, FileUtils.getFileUri(this@VideoPlayerActivity, file))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            getString(R.string.share_via)
        ))
    }
}
