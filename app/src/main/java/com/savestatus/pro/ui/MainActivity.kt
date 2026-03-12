package com.savestatus.pro.ui

import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var btnNavStatus: LinearLayout
    private lateinit var btnNavDownloaded: LinearLayout
    private lateinit var navStatusIcon: ImageView
    private lateinit var navStatusText: TextView
    private lateinit var navDownloadedIcon: ImageView
    private lateinit var navDownloadedText: TextView

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

        @Suppress("DEPRECATION")
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        setContentView(R.layout.activity_main)

        tabLayout           = findViewById(R.id.tabLayout)
        viewPager           = findViewById(R.id.viewPager)
        switchBusiness      = findViewById(R.id.switchBusiness)
        tvWA                = findViewById(R.id.tvWhatsApp)
        tvBiz               = findViewById(R.id.tvBusiness)
        layoutPermission    = findViewById(R.id.layoutPermission)
        tvPermissionMessage = findViewById(R.id.tvPermissionMessage)
        btnNavStatus        = findViewById(R.id.btnNavStatus)
        btnNavDownloaded    = findViewById(R.id.btnNavDownloaded)
        navStatusIcon       = findViewById(R.id.ivNavStatusIcon)
        navStatusText       = findViewById(R.id.tvNavStatus)
        navDownloadedIcon   = findViewById(R.id.ivNavDownloadedIcon)
        navDownloadedText   = findViewById(R.id.tvNavDownloaded)

        setupViewPager()
        setupBottomNav()
        setupToggleSwitch()
        observeViewModel()
        checkAndRequestPermissions()

        findViewById<MaterialButton>(R.id.btnGrantPermission)
            .setOnClickListener { checkAndRequestPermissions() }
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
        val green = 0xFF22C55E.toInt()
        val grey  = 0xFF8A8A8E.toInt()

        // Status tab
        navStatusIcon.setColorFilter(
            if (isStatusNavSelected) green else grey, PorterDuff.Mode.SRC_IN
        )
        navStatusText.setTextColor(if (isStatusNavSelected) green else grey)
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
            if (!isStatusNavSelected) green else grey, PorterDuff.Mode.SRC_IN
        )
        navDownloadedText.setTextColor(if (!isStatusNavSelected) green else grey)
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
            tvWA.setTextColor(if (!isChecked) 0xFFFFFFFF.toInt() else 0xFF8A8A8E.toInt())
            tvBiz.setTextColor(if (isChecked) 0xFFFFFFFF.toInt() else 0xFF8A8A8E.toInt())
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

    private fun showManageStorageDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(getString(R.string.manage_storage_required))
            .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                try {
                    manageStorageLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            .apply { data = Uri.parse("package:$packageName") }
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
}
