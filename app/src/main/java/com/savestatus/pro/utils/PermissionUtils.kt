package com.savestatus.pro.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

object PermissionUtils {

    /**
     * Returns true if the app has the necessary storage permission to read WhatsApp .Statuses folder.
     *
     * Android 11+ (R): MANAGE_EXTERNAL_STORAGE is required because .Statuses is a hidden dot-folder
     *   that is NOT accessible via MediaStore / READ_MEDIA_IMAGES on any Android 11+ version.
     * Android < 11: READ_EXTERNAL_STORAGE is sufficient.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11+: need MANAGE_EXTERNAL_STORAGE to access hidden .Statuses folder
                Environment.isExternalStorageManager()
            }
            else -> {
                // Android < 11: READ_EXTERNAL_STORAGE is enough
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    @Suppress("unused")
    fun hasWritePermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> true
            else -> ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Returns the runtime permissions to request via ActivityResultContracts.RequestMultiplePermissions.
     * For Android 11+, MANAGE_EXTERNAL_STORAGE cannot be granted via this launcher
     * (it needs a Settings page), so we return empty and let needsManageStoragePermission() handle it.
     */
    fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Handled via Settings page (MANAGE_EXTERNAL_STORAGE)
                arrayOf()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10: only READ (WRITE is scoped)
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                // Android 8/9: READ + WRITE
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }

    /**
     * True if we need to send the user to the MANAGE_ALL_FILES_ACCESS settings page.
     * This applies to Android 11+ where MANAGE_EXTERNAL_STORAGE is required.
     */
    fun needsManageStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !Environment.isExternalStorageManager()
    }
}
