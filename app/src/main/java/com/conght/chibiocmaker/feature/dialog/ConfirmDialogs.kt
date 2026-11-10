package com.conght.chibiocmaker.feature.dialog

import com.conght.chibiocmaker.R

/**
 * Factory các dialog xác nhận theo Figma. Cách dùng (trong Fragment):
 * ```
 * childFragmentManager.setFragmentResultListener(KEY, viewLifecycleOwner) { _, b ->
 *     if (ConfirmDialogFragment.isConfirmed(b)) ...
 * }
 * ConfirmDialogs.exit(KEY).show(childFragmentManager, ConfirmDialogFragment.TAG)
 * ```
 */
object ConfirmDialogs {

    fun exit(requestKey: String) = ConfirmDialogFragment.newInstance(
        requestKey, R.string.dialog_exit_title, R.string.dialog_exit_message, R.string.yes, R.string.no,
    )

    fun reset(requestKey: String) = ConfirmDialogFragment.newInstance(
        requestKey, R.string.dialog_reset_title, R.string.dialog_reset_message, R.string.yes, R.string.no,
    )

    fun delete(requestKey: String) = ConfirmDialogFragment.newInstance(
        requestKey, R.string.dialog_delete_title, R.string.dialog_delete_message, R.string.yes, R.string.no,
    )

    /** Lucky Gift đang đếm ngược: Cancel (trái) | Watch Ad (phải) để quay thêm một lượt ngay. */
    fun luckyGiftAd(requestKey: String) = ConfirmDialogFragment.newInstance(
        requestKey,
        R.string.dialog_lucky_ad_title,
        R.string.dialog_lucky_ad_message,
        confirmText = R.string.watch_ad,
        dismissText = R.string.cancel,
        confirmOnStart = false,
        watchAd = true,
    )

    /** Download bị từ chối quyền bộ nhớ 2 lần (Android 9 trở xuống): Settings mở trang cài đặt app. */
    fun storageSettings(requestKey: String) = ConfirmDialogFragment.newInstance(
        requestKey,
        R.string.dialog_storage_title,
        R.string.dialog_storage_message,
        confirmText = R.string.go_to_settings,
        dismissText = R.string.cancel,
        confirmOnStart = false,
    )

    /** Cancel (trái) | Watch Ad (phải). Chưa có Ads SDK: Watch Ad trả `confirmed = true` ngay. */
    fun random(requestKey: String) = ConfirmDialogFragment.newInstance(
        requestKey,
        R.string.dialog_random_title,
        R.string.dialog_random_message,
        confirmText = R.string.watch_ad,
        dismissText = R.string.cancel,
        confirmOnStart = false,
        watchAd = true,
    )
}
