package com.chibi.dream.cute.feature.setting

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.chibi.dream.cute.R
import com.chibi.dream.cute.core.common.AppConfig
import com.chibi.dream.cute.core.ui.applyStatusBarPadding
import com.chibi.dream.cute.core.ui.base.BaseFragment
import com.chibi.dream.cute.core.ui.collectWhenStarted
import com.chibi.dream.cute.core.ui.navigateSafe
import com.chibi.dream.cute.core.ui.setLightStatusBar
import com.chibi.dream.cute.core.ui.setOnSingleClickListener
import com.chibi.dream.cute.core.ui.showAppToast
import com.chibi.dream.cute.databinding.FragmentSettingBinding
import com.chibi.dream.cute.feature.dialog.RateUsDialogFragment
import com.chibi.dream.cute.feature.language.LanguageFragment
import dagger.hilt.android.AndroidEntryPoint

/** Setting (Figma 3:468): Language / Rate us / Share app / Privacy & Policy. */
@AndroidEntryPoint
class SettingFragment : BaseFragment<FragmentSettingBinding>(FragmentSettingBinding::inflate) {

    private val viewModel: SettingViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)

        binding.btnBack.setOnSingleClickListener { findNavController().popBackStack() }
        binding.rowLanguage.setOnSingleClickListener {
            navigateSafe(
                R.id.action_setting_to_language,
                Bundle().apply { putBoolean(LanguageFragment.ARG_FROM_SETTING, true) },
            )
        }
        binding.rowRate.setOnSingleClickListener { RateUsDialogFragment.showOnce(childFragmentManager) }
        binding.rowShare.setOnSingleClickListener { shareApp() }
        binding.rowPrivacy.setOnSingleClickListener { openPrivacyPolicy() }

        childFragmentManager.setFragmentResultListener(RateUsDialogFragment.KEY_RATED, viewLifecycleOwner) { _, _ ->
            viewModel.markAppRated()
            showAppToast(R.string.rate_thank_you)
        }
        collectWhenStarted(viewModel.appRated) { rated -> binding.rowRate.isVisible = !rated }
    }

    private fun shareApp() {
        val context = requireContext()
        val message = getString(
            R.string.share_app_message,
            getString(R.string.app_name),
            AppConfig.playStoreUrl(context.packageName),
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(send, getString(R.string.share_app)))
    }

    /** Mở Custom Tab; máy không có trình duyệt hỗ trợ thì thử ACTION_VIEW. */
    private fun openPrivacyPolicy() {
        val uri = AppConfig.PRIVACY_URL.toUri()
        try {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(requireContext(), uri)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: ActivityNotFoundException) {
                showAppToast(R.string.error_generic)
            }
        }
    }
}
