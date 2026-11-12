package com.conght.chibiocmaker.feature.cosplay

import android.os.Bundle
import android.view.View
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.conght.chibiocmaker.R
import com.conght.chibiocmaker.core.common.Resource
import com.conght.chibiocmaker.core.ui.applyStatusBarPadding
import com.conght.chibiocmaker.core.ui.base.BaseFragment
import com.conght.chibiocmaker.core.ui.collectWhenStarted
import com.conght.chibiocmaker.core.ui.loadImage
import com.conght.chibiocmaker.core.ui.navigateSafe
import com.conght.chibiocmaker.core.ui.setLightStatusBar
import com.conght.chibiocmaker.core.ui.setOnSingleClickListener
import com.conght.chibiocmaker.databinding.FragmentCosplayStartBinding
import com.conght.chibiocmaker.feature.dialog.NoInternetDialogFragment
import com.conght.chibiocmaker.feature.maker.customize.CustomizeFragment
import com.conght.chibiocmaker.feature.maker.customize.CustomizeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Cosplay start (Figma 3:41). Đang tải đề: ảnh tham chiếu là skeleton shimmer, nút Cosplay mờ.
 * Random gọi lại đề; (i) mở luật chơi (không tự hiện khi vào màn).
 * Mất mạng: Random / Cosplay hiện dialog No Internet, Try again làm lại đúng nút vừa bấm.
 */
@AndroidEntryPoint
class CosplayStartFragment : BaseFragment<FragmentCosplayStartBinding>(FragmentCosplayStartBinding::inflate) {

    private val viewModel: CosplayViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)

        binding.btnBack.setOnSingleClickListener { findNavController().popBackStack() }
        binding.btnInfo.setOnSingleClickListener { showInstruction() }
        binding.btnRandom.setOnSingleClickListener { onRandomClick() }
        binding.errorState.setOnRetryClickListener { viewModel.random() }
        binding.btnCosplay.setOnSingleClickListener { onCosplayClick() }
        childFragmentManager.setFragmentResultListener(KEY_NO_INTERNET_RANDOM, viewLifecycleOwner) { _, _ -> onRandomClick() }
        childFragmentManager.setFragmentResultListener(KEY_NO_INTERNET_COSPLAY, viewLifecycleOwner) { _, _ -> onCosplayClick() }

        collectWhenStarted(viewModel.challenge) { resource ->
            val ready = resource is Resource.Success
            if (resource is Resource.Loading) binding.shimmer.startShimmer() else binding.shimmer.stopShimmer()
            binding.referenceSkeleton.root.isInvisible = resource !is Resource.Loading
            binding.errorState.isVisible = resource is Resource.Error
            if (resource is Resource.Error) binding.errorState.setError(resource.throwable)
            binding.ivReference.isInvisible = !ready
            // Không disable: mất mạng làm đề lỗi, nút vẫn phải bấm được để hiện dialog No Internet
            binding.btnCosplay.alpha = if (ready) 1f else DISABLED_ALPHA
            if (resource is Resource.Success) binding.ivReference.loadImage(resource.data.referenceImageUrl, R.color.white)
        }
    }

    private fun onRandomClick() = runWhenOnline(KEY_NO_INTERNET_RANDOM) { viewModel.random() }

    private fun onCosplayClick() = runWhenOnline(KEY_NO_INTERNET_COSPLAY) {
        val challenge = (viewModel.challenge.value as? Resource.Success)?.data ?: return@runWhenOnline
        navigateSafe(
            R.id.action_cosplayStart_to_cosplayPlay,
            Bundle().apply {
                putString(CustomizeFragment.ARG_MODE, CustomizeMode.COSPLAY.name)
                putString(CustomizeFragment.ARG_TEMPLATE_ID, challenge.templateId)
                putString(CustomizeFragment.ARG_CHALLENGE_JSON, viewModel.challengeJson(challenge))
            },
        )
    }

    private fun runWhenOnline(noInternetKey: String, action: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.isOnline()) {
                action()
            } else {
                NoInternetDialogFragment.showOnce(childFragmentManager, noInternetKey)
            }
        }
    }

    private fun showInstruction() {
        if (childFragmentManager.findFragmentByTag(InstructionDialogFragment.TAG) == null) {
            InstructionDialogFragment().show(childFragmentManager, InstructionDialogFragment.TAG)
        }
    }

    private companion object {
        const val DISABLED_ALPHA = 0.5f
        const val KEY_NO_INTERNET_RANDOM = "cosplay_no_internet_random"
        const val KEY_NO_INTERNET_COSPLAY = "cosplay_no_internet_cosplay"
    }
}
