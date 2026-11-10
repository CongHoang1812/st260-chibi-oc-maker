package com.conght.chibiocmaker.feature.language

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.conght.chibiocmaker.R
import com.conght.chibiocmaker.core.common.AppLanguage
import com.conght.chibiocmaker.core.ui.applyStatusBarPadding
import com.conght.chibiocmaker.core.ui.base.BaseFragment
import com.conght.chibiocmaker.core.ui.collectWhenStarted
import com.conght.chibiocmaker.core.ui.setLightStatusBar
import com.conght.chibiocmaker.core.ui.setOnSingleClickListener
import com.conght.chibiocmaker.databinding.FragmentLanguageBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Chọn ngôn ngữ. [ARG_FROM_SETTING] = false: lần đầu (→ intro), true: từ setting (→ quay lại).
 */
@AndroidEntryPoint
class LanguageFragment : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {

    private val viewModel: LanguageViewModel by viewModels()
    private val fromSetting by lazy { arguments?.getBoolean(ARG_FROM_SETTING, false) ?: false }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)

        binding.ivBackground.isVisible = fromSetting
        binding.vOverlay.isVisible = fromSetting
        binding.btnBack.isVisible = fromSetting
        binding.tvTitle.isVisible = !fromSetting

        val adapter = LanguageAdapter(viewModel.selected.value) { viewModel.select(it) }
        binding.rvLanguages.adapter = adapter
        binding.btnBack.setOnSingleClickListener { findNavController().popBackStack() }
        binding.btnCheck.setOnSingleClickListener { viewModel.confirm() }

        collectWhenStarted(viewModel.selected) {
            adapter.selected = it
            binding.btnCheck.isVisible = it != null
        }
        collectWhenStarted(viewModel.confirmed) { onConfirmed(it) }
    }

    private fun onConfirmed(language: AppLanguage) {
        val nav = findNavController()
        if (fromSetting) {
            nav.popBackStack()
        } else {
            nav.navigate(R.id.action_language_to_intro)
        }
        // Đổi locale sau khi điều hướng: Activity recreate sẽ khôi phục đúng màn đích
        AppLanguage.apply(language)
    }

    companion object {
        const val ARG_FROM_SETTING = "fromSetting"
    }
}
