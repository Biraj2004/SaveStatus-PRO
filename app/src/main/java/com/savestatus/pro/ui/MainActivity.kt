package com.savestatus.pro.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.savestatus.pro.R
import com.savestatus.pro.utils.AppToast
import com.savestatus.pro.utils.PermissionUtils

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var switchBusiness: SwitchMaterial
    private lateinit var tvWA: TextView
    private lateinit var tvBiz: TextView
    private lateinit var layoutPermission: LinearLayout
    private lateinit var tvPermissionMessage: TextView
    private lateinit var rootContainer: View
    private lateinit var appBarLayout: View
    private lateinit var bottomNavWrapper: View
    private lateinit var btnNavStatus: LinearLayout
    private lateinit var btnNavDownloaded: LinearLayout
    private lateinit var navStatusIcon: ImageView
    private lateinit var navStatusText: TextView
    private lateinit var navDownloadedIcon: ImageView
    private lateinit var navDownloadedText: TextView
    private lateinit var btnNavAbout: ImageButton

    private var isStatusNavSelected = true

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) onPermissionGranted() else showPermissionDenied()
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (PermissionUtils.hasStoragePermission(this)) onPermissionGranted() else showPermissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        setContentView(R.layout.activity_main)

        rootContainer        = findViewById(R.id.rootContainer)
        appBarLayout         = findViewById(R.id.appBarLayout)
        tabLayout           = findViewById(R.id.tabLayout)
        viewPager           = findViewById(R.id.viewPager)
        switchBusiness      = findViewById(R.id.switchBusiness)
        tvWA                = findViewById(R.id.tvWhatsApp)
        tvBiz               = findViewById(R.id.tvBusiness)
        layoutPermission    = findViewById(R.id.layoutPermission)
        tvPermissionMessage = findViewById(R.id.tvPermissionMessage)
        bottomNavWrapper    = findViewById(R.id.bottomNavWrapper)
        btnNavStatus        = findViewById(R.id.btnNavStatus)
        btnNavDownloaded    = findViewById(R.id.btnNavDownloaded)
        navStatusIcon       = findViewById(R.id.ivNavStatusIcon)
        navStatusText       = findViewById(R.id.tvNavStatus)
        navDownloadedIcon   = findViewById(R.id.ivNavDownloadedIcon)
        navDownloadedText   = findViewById(R.id.tvNavDownloaded)
        btnNavAbout         = findViewById(R.id.btnNavAbout)

        setupEdgeToEdge()
        setupViewPager()
        setupBottomNav()
        setupToggleSwitch()
        observeViewModel()
        checkAndRequestPermissions()

        findViewById<MaterialButton>(R.id.btnGrantPermission)
            .setOnClickListener { checkAndRequestPermissions() }
    }

    private fun setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { _, insets ->
            val systemInsets: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            appBarLayout.updatePadding(top = systemInsets.top)

            bottomNavWrapper.updateLayoutParams<androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams> {
                bottomMargin = systemInsets.bottom + 20.dp
            }

            insets
        }
    }

    private fun setupViewPager() {
        viewPager.adapter = StatusPagerAdapter(this, tabCount = 3)
        val titles = listOf(
            getString(R.string.tab_recent),
            getString(R.string.tab_images),
            getString(R.string.tab_videos)
        )
        TabLayoutMediator(tabLayout, viewPager) { tab, pos -> tab.text = titles[pos] }.attach()
    }

    private fun setupBottomNav() {
        updateBottomNav()

        btnNavAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        btnNavStatus.setOnClickListener {
            if (!isStatusNavSelected) {
                isStatusNavSelected = true
                updateBottomNav()
                viewPager.adapter = StatusPagerAdapter(this, tabCount = 3)
                val titles = listOf(
                    getString(R.string.tab_recent),
                    getString(R.string.tab_images),
                    getString(R.string.tab_videos)
                )
                TabLayoutMediator(tabLayout, viewPager) { tab, pos -> tab.text = titles[pos] }.attach()
                if (PermissionUtils.hasStoragePermission(this)) viewModel.loadStatuses()
            }
        }
        btnNavDownloaded.setOnClickListener {
            if (isStatusNavSelected) {
                isStatusNavSelected = false
                updateBottomNav()
                viewModel.loadDownloadedStatuses()
                viewPager.adapter = StatusPagerAdapter(
                    this, tabCount = 1,
                    startTab = StatusListFragment.TAB_DOWNLOADED
                )
                TabLayoutMediator(tabLayout, viewPager) { tab, _ ->
                    tab.text = getString(R.string.tab_downloaded)
                }.attach()
            }
        }
    }

    private fun updateBottomNav() {
        val activeColor = ContextCompat.getColor(this, R.color.nav_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive)

        // Status tab
        navStatusIcon.setColorFilter(
            if (isStatusNavSelected) activeColor else inactiveColor, PorterDuff.Mode.SRC_IN
        )
        navStatusText.setTextColor(if (isStatusNavSelected) activeColor else inactiveColor)
        navStatusText.setTypeface(
            null,
            if (isStatusNavSelected) Typeface.BOLD else Typeface.NORMAL
        )
        btnNavStatus.setBackgroundResource(
            if (isStatusNavSelected) R.drawable.bg_nav_item_selected
            else R.drawable.bg_nav_item_ripple
        )

        // Downloaded tab
        navDownloadedIcon.setColorFilter(
            if (!isStatusNavSelected) activeColor else inactiveColor, PorterDuff.Mode.SRC_IN
        )
        navDownloadedText.setTextColor(if (!isStatusNavSelected) activeColor else inactiveColor)
        navDownloadedText.setTypeface(
            null,
            if (!isStatusNavSelected) Typeface.BOLD else Typeface.NORMAL
        )
        btnNavDownloaded.setBackgroundResource(
            if (!isStatusNavSelected) R.drawable.bg_nav_item_selected
            else R.drawable.bg_nav_item_ripple
        )
    }

    private fun setupToggleSwitch() {
        switchBusiness.setOnCheckedChangeListener { _, isChecked ->
            viewModel.toggleMode(isChecked)
            tvWA.setTextColor(
                ContextCompat.getColor(this, if (!isChecked) R.color.text_primary else R.color.text_secondary)
            )
            tvBiz.setTextColor(
                ContextCompat.getColor(this, if (isChecked) R.color.text_primary else R.color.text_secondary)
            )
            val msg = if (isChecked) getString(R.string.toast_switched_biz)
                      else getString(R.string.toast_switched_wa)
            AppToast.plain(this, msg)
        }
    }

    private fun observeViewModel() {
        viewModel.error.observe(this) { msg ->
            msg ?: return@observe
            AppToast.error(this, msg)
        }
    }

    private fun checkAndRequestPermissions() {
        when {
            PermissionUtils.hasStoragePermission(this) -> onPermissionGranted()
            PermissionUtils.needsManageStoragePermission() -> showManageStorageDialog()
            else -> {
                val perms = PermissionUtils.getRequiredPermissions()
                if (perms.isEmpty()) onPermissionGranted()
                else permissionLauncher.launch(perms)
            }
        }
    }

    @android.annotation.SuppressLint("InlinedApi")
    private fun showManageStorageDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required_title))
            .setMessage(getString(R.string.manage_storage_required))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                try {
                    manageStorageLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            .apply { data = "package:$packageName".toUri() }
                    )
                } catch (_: Exception) {
                    manageStorageLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    )
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> showPermissionDenied() }
            .setCancelable(false).show()
    }

    private fun onPermissionGranted() {
        layoutPermission.visibility = View.GONE
        viewPager.visibility = View.VISIBLE
        viewModel.loadStatuses()
    }

    private fun showPermissionDenied() {
        layoutPermission.visibility = View.VISIBLE
        viewPager.visibility = View.GONE
        tvPermissionMessage.text = getString(R.string.permission_denied)
    }

    override fun onResume() {
        super.onResume()
        if (PermissionUtils.hasStoragePermission(this)) {
            onPermissionGranted()
        } else if (PermissionUtils.needsManageStoragePermission()) {
            showPermissionDenied()
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
