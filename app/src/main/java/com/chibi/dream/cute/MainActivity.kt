package com.chibi.dream.cute

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.chibi.dream.cute.core.common.AppLanguage
import com.chibi.dream.cute.core.ui.applyNavigationBarMode
import com.chibi.dream.cute.core.ui.rehideNavigationBarIfNeeded
import com.chibi.dream.cute.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Ngôn ngữ mà màn đang hiện đã inflate theo. */
    private lateinit var shownLocales: LocaleListCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AppLanguage.applyDefaultIfUnset()
        shownLocales = ConfigurationCompat.getLocales(resources.configuration)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Nội dung vẽ dưới status bar trong suốt: chỉ chừa inset trái/phải/dưới.
        // Màn nào cần tránh status bar thì tự xử lý top inset.
        // Thanh điều hướng luôn ẩn, hiện tạm thì đè lên nội dung: chỉ chừa đáy khi bàn phím mở (thanh hiện kèm,
        // DecorEditorFragment.liftPanelAboveKeyboard tính theo khoảng này). Kéo thanh thông báo / mở dialog làm
        // cửa sổ mất focus, hệ thống hiện thanh điều hướng thật: chừa đáy lúc đó làm cả màn bị đẩy lên.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboardOpen = insets.isVisible(WindowInsetsCompat.Type.ime())
            v.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = if (keyboardOpen) systemBars.bottom else 0,
            )
            window.rehideNavigationBarIfNeeded(insets)
            insets
        }
        window.applyNavigationBarMode()
    }

    /**
     * Manifest khai báo `configChanges="locale|layoutDirection"`: đổi ngôn ngữ không relaunch Activity
     * (relaunch làm cả màn hình nháy đen, nhất là trên MIUI). Resources đã mang ngôn ngữ mới, chỉ cần
     * inflate lại màn đang hiện; màn trong back stack đã huỷ view nên quay lại sẽ tự inflate theo ngôn ngữ mới.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val locales = ConfigurationCompat.getLocales(newConfig)
        if (locales == shownLocales) return
        shownLocales = locales
        reinflateCurrentScreen()
    }

    private fun reinflateCurrentScreen() {
        val fragments = supportFragmentManager.findFragmentById(R.id.nav_host)?.childFragmentManager ?: return
        // Không sửa được fragment sau onSaveInstanceState: recreate như cũ
        if (fragments.isStateSaved) {
            recreate()
            return
        }
        // Điều hướng vừa gọi (vd. Language → Intro) chạy xong trước, để inflate lại đúng màn đích
        fragments.executePendingTransactions()
        val current = fragments.primaryNavigationFragment ?: return
        fragments.beginTransaction().detach(current).commitNow()
        fragments.beginTransaction().attach(current).commitNow()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Đóng dialog / bàn phím / quay lại app có thể làm thanh điều hướng hiện lại
        if (hasFocus) window.applyNavigationBarMode()
    }
}
