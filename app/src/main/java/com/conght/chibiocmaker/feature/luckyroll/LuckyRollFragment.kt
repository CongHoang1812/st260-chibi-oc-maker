package com.conght.chibiocmaker.feature.luckyroll

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.conght.chibiocmaker.R
import com.conght.chibiocmaker.core.common.AppConfig
import com.conght.chibiocmaker.core.common.Resource
import com.conght.chibiocmaker.core.ui.applyStatusBarPadding
import com.conght.chibiocmaker.core.ui.base.BaseFragment
import com.conght.chibiocmaker.core.ui.collectWhenStarted
import com.conght.chibiocmaker.core.ui.loadImage
import com.conght.chibiocmaker.core.ui.navigateSafe
import com.conght.chibiocmaker.core.ui.setLightStatusBar
import com.conght.chibiocmaker.core.ui.setOnSingleClickListener
import com.conght.chibiocmaker.databinding.FragmentLuckyRollBinding
import com.conght.chibiocmaker.feature.dialog.ConfirmDialogFragment
import com.conght.chibiocmaker.feature.dialog.ConfirmDialogs
import com.conght.chibiocmaker.feature.maker.customize.CustomizeFragment
import dagger.hilt.android.AndroidEntryPoint

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
        binding.btnEdit.setOnSingleClickListener {
            val result = (viewModel.state.value as? LuckyRollUiState.Rolled)?.result as? Resource.Success
                ?: return@setOnSingleClickListener
            navigateSafe(
                R.id.action_luckyRoll_to_customize,
                Bundle().apply { putString(CustomizeFragment.ARG_CONFIG_JSON, viewModel.configJson(result.data)) },
            )
        }
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
    }
}
