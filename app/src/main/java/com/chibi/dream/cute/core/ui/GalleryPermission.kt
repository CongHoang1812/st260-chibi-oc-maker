package com.chibi.dream.cute.core.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.chibi.dream.cute.R
import com.chibi.dream.cute.core.common.AppPermissions
import com.chibi.dream.cute.core.common.galleryAlbumName
import com.chibi.dream.cute.feature.dialog.ConfirmDialogFragment
import com.chibi.dream.cute.feature.dialog.ConfirmDialogs

/**
 * Bọc thao tác ghi ảnh ra Gallery (Download ở View / Success / My creation):
 * - Android 10+: chạy luôn (MediaStore không cần quyền).
 * - Android 9 trở xuống: xin WRITE_EXTERNAL_STORAGE; bị từ chối 2 lần thì lần bấm sau hiện dialog mở Settings.
 * Gọi lúc khởi tạo Fragment (khai báo property) vì có đăng ký ActivityResult.
 */
fun Fragment.registerGalleryDownload(download: () -> Unit): () -> Unit {
    val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
    val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            download()
        } else if (requireContext().addStorageDenial() >= MAX_STORAGE_DENIALS) {
            showStorageSettingsDialog()
        } else {
            showAppToast(R.string.save_permission_denied)
        }
    }
    lifecycle.addObserver(object : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            childFragmentManager.setFragmentResultListener(KEY_STORAGE_SETTINGS, owner) { _, result ->
                if (ConfirmDialogFragment.isConfirmed(result)) requireContext().openAppSettings()
            }
        }
    })
    return {
        val context = requireContext()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || AppPermissions.isGranted(context, permission) -> download()
            context.storageDenials() >= MAX_STORAGE_DENIALS -> showStorageSettingsDialog()
            else -> launcher.launch(permission)
        }
    }
}

/** Mở trang thông tin app trong Settings để người dùng tự cấp quyền. */
fun Context.openAppSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
}

private fun Fragment.showStorageSettingsDialog() {
    if (childFragmentManager.findFragmentByTag(ConfirmDialogFragment.TAG) != null) return
    ConfirmDialogs.storageSettings(KEY_STORAGE_SETTINGS).show(childFragmentManager, ConfirmDialogFragment.TAG)
}

private fun Context.storagePrefs() = getSharedPreferences(PREFS_GALLERY_PERMISSION, Context.MODE_PRIVATE)

private fun Context.storageDenials(): Int = storagePrefs().getInt(KEY_DENIALS, 0)

private fun Context.addStorageDenial(): Int {
    val count = storageDenials() + 1
    storagePrefs().edit { putInt(KEY_DENIALS, count) }
    return count
}

private const val MAX_STORAGE_DENIALS = 2
private const val PREFS_GALLERY_PERMISSION = "gallery_permission"
private const val KEY_DENIALS = "storage_denials"
private const val KEY_STORAGE_SETTINGS = "gallery_storage_settings"

/** Toast sau khi Download ra Gallery thành công, kèm tên album `Pictures/{tên app}`. */
fun Fragment.showDownloadSuccessToast() {
    val anchor = view ?: return
    anchor.showAppToast(getString(R.string.download_success, anchor.context.galleryAlbumName()))
}
