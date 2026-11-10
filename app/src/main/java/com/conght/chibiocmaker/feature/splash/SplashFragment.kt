package com.conght.chibiocmaker.feature.splash

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.conght.chibiocmaker.R
import com.conght.chibiocmaker.core.ui.applyStatusBarPadding
import com.conght.chibiocmaker.core.ui.base.BaseFragment
import com.conght.chibiocmaker.core.ui.setLightStatusBar
import com.conght.chibiocmaker.databinding.FragmentSplashBinding
import com.conght.chibiocmaker.feature.intro.IntroFragment
import com.conght.chibiocmaker.feature.language.LanguageFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    private val viewModel: SplashViewModel by viewModels()
    private var spinnerAnimator: ObjectAnimator? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)

        startSpinner()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(SPLASH_MS)
            val nav = findNavController()
            // Intro hiện mỗi lần mở app; Language + Permission chỉ ở lần đầu
            if (viewModel.isOnboardingDone()) {
                nav.navigate(
                    R.id.action_splash_to_intro,
                    Bundle().apply { putBoolean(IntroFragment.ARG_ONBOARDING_DONE, true) },
                )
            } else {
                nav.navigate(
                    R.id.action_splash_to_language,
                    Bundle().apply { putBoolean(LanguageFragment.ARG_FROM_SETTING, false) },
                )
            }
        }
    }

    /** Vòng loading (Figma 282:19) xoay đều liên tục tới khi rời màn. */
    private fun startSpinner() {
        spinnerAnimator = ObjectAnimator.ofFloat(binding.ivSpinner, View.ROTATION, 0f, FULL_TURN).apply {
            duration = SPIN_MS
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    override fun onDestroyView() {
        spinnerAnimator?.cancel()
        spinnerAnimator = null
        super.onDestroyView()
    }

    private companion object {
        const val SPLASH_MS = 2500L
        const val SPIN_MS = 1000L
        const val FULL_TURN = 360f
    }
}
