package com.chibi.dream.cute.core.ui

import android.view.View
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment

/**
 * Cộng chiều cao status bar thật vào paddingTop (giữ padding gốc). Nền vẫn vẽ dưới status bar,
 * nội dung đặt theo toạ độ "y Figma - 44dp" (xem dimens.xml).
 */
fun View.applyStatusBarPadding() {
    val initialTop = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        v.updatePadding(top = initialTop + insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

/**
 * Ẩn thanh điều hướng (mọi màn, mọi thiết bị). Vuốt từ đáy để hiện tạm rồi tự ẩn,
 * thanh hiện đè lên nội dung chứ không đẩy layout. Gọi lại mỗi khi window lấy focus.
 */
fun Window.applyNavigationBarMode() {
    WindowCompat.getInsetsController(this, decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.navigationBars())
    }
}

/**
 * Gọi trong OnApplyWindowInsetsListener: bàn phím mở làm thanh điều hướng hiện theo, đóng bàn phím
 * thì thanh vẫn ở lại (window không đổi focus nên onWindowFocusChanged không chạy) → ẩn lại.
 */
fun Window.rehideNavigationBarIfNeeded(insets: WindowInsetsCompat) {
    val navVisible = insets.isVisible(WindowInsetsCompat.Type.navigationBars())
    if (navVisible && !insets.isVisible(WindowInsetsCompat.Type.ime())) decorView.post { applyNavigationBarMode() }
}

/** `true` = icon status bar màu tối (nền sáng), `false` = icon sáng (nền tối). */
fun Fragment.setLightStatusBar(light: Boolean) {
    val window = requireActivity().window
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = light
}
