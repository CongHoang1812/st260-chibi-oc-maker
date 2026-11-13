package com.chibi.dream.cute.feature.luckyroll

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.chibi.dream.cute.R
import com.chibi.dream.cute.core.common.AppConfig
import com.chibi.dream.cute.core.common.Resource
import com.chibi.dream.cute.core.ui.applyStatusBarPadding
import com.chibi.dream.cute.core.ui.base.BaseFragment
import com.chibi.dream.cute.core.ui.collectWhenStarted
import com.chibi.dream.cute.core.ui.loadImage
import com.chibi.dream.cute.core.ui.navigateSafe
import com.chibi.dream.cute.core.ui.setLightStatusBar
import com.chibi.dream.cute.core.ui.setOnSingleClickListener
import com.chibi.dream.cute.databinding.FragmentLuckyRollBinding
import com.chibi.dream.cute.feature.dialog.ConfirmDialogFragment
import com.chibi.dream.cute.feature.dialog.ConfirmDialogs
import com.chibi.dream.cute.feature.dialog.NoInternetDialogFragment
import com.chibi.dream.cute.feature.maker.customize.CustomizeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Lucky roll (Figma 3:11 / 3:24): Idle → Random → Loading (shimmer) → kết quả + Edit.
 */
@AndroidEntryPoint
class LuckyRollFragment : BaseFragment<FragmentLuckyRollBinding>(FragmentLuckyRollBinding::inflate) {

    private val viewModel: LuckyRollViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)
        binding.btnBack.setOnSingleClickListener { findNavController().popBackStack() }
        binding.btnRandom.setOnSingleClickListener { requestRoll() }
        binding.errorState.setOnRetryClickListener { viewModel.roll() }
        binding.btnEdit.setOnSingleClickListener { openEdit() }
        childFragmentManager.setFragmentResultListener(KEY_NO_INTERNET, viewLifecycleOwner) { _, _ -> openEdit() }
//        childFragmentManager.setFragmentResultListener(KEY_RANDOM, viewLifecycleOwner) { _, bundle ->
//            if (ConfirmDialogFragment.isConfirmed(bundle)) viewModel.roll()
//        }

        collectWhenStarted(viewModel.state, ::render)
    }

    private fun requestRoll() {
        if (AppConfig.LUCKY_ROLL_REQUIRES_AD) {
            ConfirmDialogs.random(KEY_RANDOM).show(childFragmentManager, ConfirmDialogFragment.TAG)
        } else {
            viewModel.roll()
        }
    }

    /** Customize tải part từ API nên cần mạng: mất mạng → dialog No Internet, Try again gọi lại hàm này. */
    private fun openEdit() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = (viewModel.state.value as? LuckyRollUiState.Rolled)?.result as? Resource.Success
                ?: return@launch
            if (!viewModel.isOnline()) {
                NoInternetDialogFragment.showOnce(childFragmentManager, KEY_NO_INTERNET)
                return@launch
            }
            navigateSafe(
                R.id.action_luckyRoll_to_customize,
                Bundle().apply { putString(CustomizeFragment.ARG_CONFIG_JSON, viewModel.configJson(result.data)) },
            )
        }
    }

    private fun render(state: LuckyRollUiState) {
        val result = (state as? LuckyRollUiState.Rolled)?.result
        val loading = result is Resource.Loading
        if (loading) binding.shimmer.startShimmer() else binding.shimmer.stopShimmer()

        binding.idleBox.isVisible = state is LuckyRollUiState.Idle
        binding.resultSkeleton.root.isVisible = loading
        binding.errorState.isVisible = result is Resource.Error
        if (result is Resource.Error) binding.errorState.setError(result.throwable)
        binding.ivResult.isVisible = result is Resource.Success
        binding.btnEdit.isVisible = result is Resource.Success
        if (result is Resource.Success) binding.ivResult.loadImage(result.data.imageUrl, R.color.white)
    }

    private companion object {
        const val KEY_RANDOM = "lucky_roll_random"
        const val KEY_NO_INTERNET = "lucky_roll_no_internet"
    }
}
