package com.chibi.dream.cute.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Quyền xin ở màn Permission: Android 12 trở xuống xin Storage, Android 13 trở lên xin Notification.
 * Ảnh của app lưu ở internal storage; ghi ra Gallery (nút Download) xem `registerGalleryDownload`.
 */
object AppPermissions {

    /** Quyền bộ nhớ, `null` từ Android 13 (không còn quyền này). Android 9 trở xuống xin kèm quyền ghi. */
    val storage: Array<String>?
        get() = when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )

            Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            else -> null
        }

    /** Quyền thông báo, `null` khi API < 33 (không cần xin). */
    val notification: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    fun isStorageGranted(context: Context): Boolean = storage?.all { isGranted(context, it) } ?: true

    fun isNotificationGranted(context: Context): Boolean = notification?.let { isGranted(context, it) } ?: true

    fun isGranted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
