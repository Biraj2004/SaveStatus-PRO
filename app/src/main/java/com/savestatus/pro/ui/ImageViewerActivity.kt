package com.savestatus.pro.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.savestatus.pro.R
import com.savestatus.pro.data.StatusRepository
import com.savestatus.pro.model.StatusItem
import com.savestatus.pro.model.StatusType
import com.savestatus.pro.utils.AppToast
import com.savestatus.pro.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.File

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var photoView: PhotoView
    private lateinit var ivBack: ImageView
    private lateinit var ivDownload: ImageView
    private lateinit var ivShare: ImageView

    private var filePath: String = ""
    private val repository by lazy { StatusRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        setContentView(R.layout.activity_image_viewer)

        filePath = intent.getStringExtra("file_path") ?: run { finish(); return }

        photoView   = findViewById(R.id.photoView)
        ivBack      = findViewById(R.id.ivBack)
        ivDownload  = findViewById(R.id.ivDownload)
        ivShare     = findViewById(R.id.ivShare)

        Glide.with(this).load(File(filePath)).placeholder(R.color.card_dark).into(photoView)

        updateDownloadIcon()

        ivBack.setOnClickListener { finish() }
        ivDownload.setOnClickListener { handleDownload() }
        ivShare.setOnClickListener { shareFile() }
    }

    override fun onResume() {
        super.onResume()
        // Re-check file existence every time screen resumes (user may have deleted file)
        updateDownloadIcon()
    }

    /** Sets the download icon based on whether the file already exists in the save folder. */
    private fun updateDownloadIcon() {
        val isDownloaded = FileUtils.isAlreadyDownloaded(File(filePath))
        ivDownload.setImageResource(if (isDownloaded) R.drawable.ic_check else R.drawable.ic_download)
    }

    private fun handleDownload() {
        if (!ivDownload.isEnabled) return          // rapid-click guard
        lifecycleScope.launch {
            val src = File(filePath)
            if (!src.exists()) {
                AppToast.plain(this@ImageViewerActivity, "File not found")
                return@launch
            }
            if (FileUtils.isAlreadyDownloaded(src)) {
                AppToast.plain(this@ImageViewerActivity, getString(R.string.already_downloaded))
                ivDownload.setImageResource(R.drawable.ic_check)
                return@launch
            }
            ivDownload.isEnabled = false           // disable while downloading
            val item = StatusItem(filePath, filePath, StatusType.IMAGE, src.lastModified())
            val success = repository.downloadStatus(item)
            ivDownload.isEnabled = true
            if (success) {
                AppToast.plain(this@ImageViewerActivity, getString(R.string.download_success))
                ivDownload.setImageResource(R.drawable.ic_check)
            } else {
                AppToast.plain(this@ImageViewerActivity, getString(R.string.download_failed))
            }
        }
    }

    private fun shareFile() {
        val file = File(filePath)
        if (!file.exists()) return
        val uri: Uri = FileUtils.getFileUri(this, file)
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            getString(R.string.share_via)
        ))
    }
}
