package com.chibi.dream.cute.feature.success

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.chibi.dream.cute.R
import com.chibi.dream.cute.core.ui.applyStatusBarPadding
import com.chibi.dream.cute.core.ui.base.BaseFragment
import com.chibi.dream.cute.core.ui.collectWhenStarted
import com.chibi.dream.cute.core.ui.loadImage
import com.chibi.dream.cute.core.ui.navigateSafe
import com.chibi.dream.cute.core.ui.registerGalleryDownload
import com.chibi.dream.cute.core.ui.setLightStatusBar
import com.chibi.dream.cute.core.ui.setOnSingleClickListener
import com.chibi.dream.cute.core.ui.showAppToast
import com.chibi.dream.cute.core.ui.showDownloadSuccessToast
import com.chibi.dream.cute.core.ui.shareImages
import com.chibi.dream.cute.databinding.FragmentSuccessBinding
import com.chibi.dream.cute.feature.maker.customize.CustomizeFragment
import com.chibi.dream.cute.feature.maker.customize.CustomizeMode
import com.chibi.dream.cute.feature.dialog.NoInternetDialogFragment
import com.chibi.dream.cute.feature.dialog.setLoadingVisible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Successful (Figma 3:1109 maker / 3:223 cosplay).
 * Download → lưu ra thư viện máy (Gallery).
 * Share → FileProvider. Re-edit → quay lại editor. Back = Home.
 */
@AndroidEntryPoint
class SuccessFragment : BaseFragment<FragmentSuccessBinding>(FragmentSuccessBinding::inflate) {

    enum class Source { MAKER, COSPLAY }

    private val viewModel: SuccessViewModel by viewModels()
    private val download = registerGalleryDownload { viewModel.download() }

    /** Tia sáng sau thẻ Cosplay: xoay chậm liên tục + phóng to / thu nhỏ nhẹ. */
    private var burstAnimator: AnimatorSet? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)
        setupSource()

        binding.btnReEdit.setOnSingleClickListener { reEdit() }
        childFragmentManager.setFragmentResultListener(KEY_NO_INTERNET, viewLifecycleOwner) { _, _ -> reEdit() }
        binding.btnShare.setOnSingleClickListener { share() }
        // My Creation mở ở tab Character (mặc định)
        binding.btnMyCreation.setOnSingleClickListener { navigateSafe(R.id.action_success_to_myCreation) }
        binding.btnHome.setOnSingleClickListener { goHome() }
        binding.btnDownload.setOnSingleClickListener { download() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        collectWhenStarted(viewModel.imagePath) { path ->
            binding.ivResult.loadImage(path, R.color.white)
            binding.btnShare.isEnabled = path != null
            binding.btnDownload.isEnabled = path != null
        }
        collectWhenStarted(viewModel.downloading) {
            binding.btnDownload.alpha = if (it) DISABLED_ALPHA else 1f
            setLoadingVisible(it)
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                SuccessEvent.Downloaded -> showDownloadSuccessToast()
                SuccessEvent.DownloadFailed -> showAppToast(R.string.save_failed)
            }
        }
    }

    /** COSPLAY: nền cam, thẻ polaroid (Figma 2:280); ảnh thu vào ô 208 trong thẻ, 3 sao ở đáy thẻ. */
    private fun setupSource() {
        val cosplay = viewModel.source == Source.COSPLAY
        binding.starBadge.isVisible = cosplay
        binding.cosplayDecor.isVisible = cosplay
        if (!cosplay) return
        binding.root.setBackgroundResource(R.drawable.bg_success_cosplay)
        startBurstAnimation()
        val size = resources.getDimensionPixelSize(R.dimen.success_card_image)
        binding.ivResult.updateLayoutParams<ConstraintLayout.LayoutParams> {
            width = size
            height = size
            dimensionRatio = null
            topToTop = R.id.cosplay_card
            startToStart = R.id.cosplay_card
            endToEnd = R.id.cosplay_card
            bottomToBottom = ConstraintLayout.LayoutParams.UNSET
            topMargin = resources.getDimensionPixelSize(R.dimen.success_card_padding)
        }
        listOf(binding.ivStar1, binding.ivStar2, binding.ivStar3).forEachIndexed { index, star ->
            star.setImageResource(if (index < viewModel.stars) R.drawable.img_star_on else R.drawable.img_star_off)
        }
        binding.starBadge.contentDescription =
            resources.getQuantityString(R.plurals.cd_rate_stars, viewModel.stars, viewModel.stars)
    }

    private fun startBurstAnimation() {
        val burst = binding.ivBurst
        val rotate = ObjectAnimator.ofFloat(burst, View.ROTATION, 0f, FULL_TURN).apply {
            duration = BURST_TURN_MS
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
        }
        val pulse = ObjectAnimator.ofPropertyValuesHolder(
            burst,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, BURST_PULSE_SCALE),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, BURST_PULSE_SCALE),
        ).apply {
            duration = BURST_PULSE_MS
            interpolator = AccelerateDecelerateInterpolator()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
        }
        burstAnimator = AnimatorSet().apply {
            playTogether(rotate, pulse)
            start()
        }
    }

    override fun onDestroyView() {
        burstAnimator?.cancel()
        burstAnimator = null
        super.onDestroyView()
    }

    private fun share() {
        val path = viewModel.imagePath.value ?: return
        if (!requireContext().shareImages(listOf(path), getString(R.string.cd_share))) {
            showAppToast(R.string.error_generic)
        }
    }

    /**
     * MAKER: quay lại Decor editor. COSPLAY: Success đã thay chỗ màn chơi trong back stack
     * nên mở lại màn chơi với đúng đề bài vừa chơi (bắt đầu lại từ đầu).
     * Editor tải dữ liệu từ API nên mất mạng → dialog No Internet, Try again gọi lại hàm này.
     */
    private fun reEdit() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.isOnline()) {
                openEditor()
            } else {
                NoInternetDialogFragment.showOnce(childFragmentManager, KEY_NO_INTERNET)
            }
        }
    }

    private fun openEditor() {
        val templateId = viewModel.cosplayTemplateId
        val challengeJson = viewModel.cosplayChallengeJson
        if (viewModel.source != Source.COSPLAY || templateId == null || challengeJson == null) {
            findNavController().popBackStack()
            return
        }
        navigateSafe(
            R.id.action_success_to_cosplayPlay,
            Bundle().apply {
                putString(CustomizeFragment.ARG_MODE, CustomizeMode.COSPLAY.name)
                putString(CustomizeFragment.ARG_TEMPLATE_ID, templateId)
                putString(CustomizeFragment.ARG_CHALLENGE_JSON, challengeJson)
            },
        )
    }

    private fun goHome() {
        findNavController().popBackStack(R.id.homeFragment, false)
    }

    companion object {
        private const val FULL_TURN = 360f
        private const val BURST_TURN_MS = 16_000L
        private const val BURST_PULSE_MS = 1_600L
        private const val BURST_PULSE_SCALE = 1.08f

        /** Id creation vừa lưu (Long), nguồn MAKER. */
        const val ARG_CREATION_ID = "creationId"

        /** Đường dẫn ảnh kết quả chưa lưu vào My creation (String), nguồn COSPLAY. */
        const val ARG_IMAGE_PATH = "imagePath"

        /** [Source] name. */
        const val ARG_SOURCE = "source"

        /** Số sao 0..3 (chỉ COSPLAY). */
        const val ARG_STARS = "stars"

        private const val DISABLED_ALPHA = 0.5f
        private const val KEY_NO_INTERNET = "success_no_internet"
    }
}
