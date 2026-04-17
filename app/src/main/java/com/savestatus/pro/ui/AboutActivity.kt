package com.savestatus.pro.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.savestatus.pro.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AboutActivity : AppCompatActivity() {

    private lateinit var tvCurrentVersion: TextView
    private lateinit var tvLatestVersion: TextView
    private lateinit var tvUpdateNow: TextView
    private lateinit var btnViewProject: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var ivProfile: ImageView
    private lateinit var aboutHeader: View
    private lateinit var cardApp: View
    private lateinit var cardDeveloper: View
    private lateinit var cardPermissions: View
    private lateinit var cardVersion: View

    private val uiScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        setContentView(R.layout.activity_about)

        bindViews()
        applyEdgeToEdge()
        setupActions()
        loadDeveloperProfile()
        bindCurrentVersion()
        runEnterAnimations()
        fetchLatestVersion()
    }

    override fun onDestroy() {
        super.onDestroy()
        uiScope.cancel()
    }

    private fun bindViews() {
        tvCurrentVersion = findViewById(R.id.tvCurrentVersion)
        tvLatestVersion = findViewById(R.id.tvLatestVersion)
        tvUpdateNow = findViewById(R.id.tvUpdateNow)
        btnViewProject = findViewById(R.id.btnViewProject)
        btnBack = findViewById(R.id.btnBack)
        ivProfile = findViewById(R.id.ivDeveloper)
        aboutHeader = findViewById(R.id.aboutHeader)
        cardApp = findViewById(R.id.cardApp)
        cardDeveloper = findViewById(R.id.cardDeveloper)
        cardPermissions = findViewById(R.id.cardPermissions)
        cardVersion = findViewById(R.id.cardVersion)
    }

    private fun applyEdgeToEdge() {
        val root = findViewById<android.view.View>(R.id.aboutRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    private fun setupActions() {
        btnBack.setOnClickListener { finish() }

        btnViewProject.setOnClickListener {
            openUrl(PROJECT_URL)
        }

        tvUpdateNow.setOnClickListener {
            openUrl(PROJECT_RELEASES_URL)
        }
    }

    private fun loadDeveloperProfile() {
        Glide.with(this)
            .load(DEVELOPER_AVATAR_URL)
            .circleCrop()
            .placeholder(R.drawable.ic_launcher_foreground)
            .into(ivProfile)
    }

    private fun bindCurrentVersion() {
        tvCurrentVersion.text = getString(R.string.about_current_version_value, getCurrentVersionName())
    }

    private fun fetchLatestVersion() {
        uiScope.launch {
            val latestVersion = withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(GITHUB_LATEST_RELEASE_API).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    connection.setRequestProperty("Accept", "application/vnd.github+json")
                    connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")

                    val responseCode = connection.responseCode
                    if (responseCode == 404) {
                        return@runCatching null
                    }
                    if (responseCode !in 200..299) {
                        throw IllegalStateException("GitHub API error: $responseCode")
                    }

                    val payload = connection.inputStream.bufferedReader().use { it.readText() }
                    val tag = JSONObject(payload).optString("tag_name", "")
                    tag.removePrefix("v").trim().ifBlank { null }
                }.getOrNull()
            }

            renderLatestVersion(latestVersion)
        }
    }

    private fun renderLatestVersion(latestVersion: String?) {
        if (latestVersion.isNullOrBlank()) {
            tvLatestVersion.text = getString(R.string.about_latest_version_unavailable)
            tvLatestVersion.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            tvUpdateNow.text = getString(R.string.about_update_pending_release)
            tvUpdateNow.setTextColor(ContextCompat.getColor(this, R.color.text_tertiary))
            tvUpdateNow.isEnabled = false
            return
        }

        tvLatestVersion.text = getString(R.string.about_latest_version_value, latestVersion)

        val currentVersion = getCurrentVersionName().removePrefix("v").trim()
        val needsUpdate = currentVersion != latestVersion

        if (needsUpdate) {
            tvLatestVersion.setTextColor(ContextCompat.getColor(this, R.color.version_latest_highlight))
            tvUpdateNow.text = getString(R.string.about_update_now)
            tvUpdateNow.setTextColor(ContextCompat.getColor(this, R.color.version_latest_highlight))
            tvUpdateNow.isEnabled = true
            animateUpdateLabel()
        } else {
            tvLatestVersion.setTextColor(ContextCompat.getColor(this, R.color.green_accent))
            tvUpdateNow.text = getString(R.string.about_up_to_date)
            tvUpdateNow.setTextColor(ContextCompat.getColor(this, R.color.green_accent))
            tvUpdateNow.isEnabled = false
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    private fun getCurrentVersionName(): String {
        return runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        }.getOrDefault("1.0.0")
    }

    private fun runEnterAnimations() {
        if (!shouldAnimate()) return

        val targets = listOf(aboutHeader, cardApp, cardDeveloper, cardPermissions, cardVersion)
        targets.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 24f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(70L * index)
                .setDuration(320L)
                .start()
        }
    }

    private fun animateUpdateLabel() {
        if (!shouldAnimate()) return

        tvUpdateNow.alpha = 0f
        tvUpdateNow.translationY = 8f
        tvUpdateNow.scaleX = 0.98f
        tvUpdateNow.scaleY = 0.98f

        tvUpdateNow.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(280L)
            .start()
    }

    private fun shouldAnimate(): Boolean {
        return runCatching {
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }

    companion object {
        private const val PROJECT_URL = "https://github.com/Biraj2004/SaveStatus-PRO"
        private const val PROJECT_RELEASES_URL = "https://github.com/Biraj2004/SaveStatus-PRO/releases"
        private const val GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/Biraj2004/SaveStatus-PRO/releases/latest"
        private const val DEVELOPER_AVATAR_URL = "https://github.com/Biraj2004.png"
    }
}
