package com.conght.chibiocmaker.feature.creation

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.conght.chibiocmaker.R
import com.conght.chibiocmaker.core.ui.applyStatusBarPadding
import com.conght.chibiocmaker.core.ui.base.BaseFragment
import com.conght.chibiocmaker.core.ui.collectWhenStarted
import com.conght.chibiocmaker.core.ui.loadWithShimmer
import com.conght.chibiocmaker.core.ui.navigateSafe
import com.conght.chibiocmaker.core.ui.registerGalleryDownload
import com.conght.chibiocmaker.core.ui.setLightStatusBar
import com.conght.chibiocmaker.core.ui.setOnSingleClickListener
import com.conght.chibiocmaker.core.ui.showAppToast
import com.conght.chibiocmaker.core.ui.showDownloadSuccessToast
import com.conght.chibiocmaker.core.ui.shareImages
import com.conght.chibiocmaker.data.model.CreationType
import com.conght.chibiocmaker.databinding.FragmentCreationDetailBinding
import com.conght.chibiocmaker.feature.dialog.ConfirmDialogFragment
import com.conght.chibiocmaker.feature.dialog.ConfirmDialogs
import com.conght.chibiocmaker.feature.dialog.NoInternetDialogFragment
import com.conght.chibiocmaker.feature.maker.customize.CustomizeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Chi tiết creation (Figma 3:1583 / 3:1608). Character có nút Edit → Customize (cần mạng, mất mạng → dialog No Internet). */
@AndroidEntryPoint
class CreationDetailFragment : BaseFragment<FragmentCreationDetailBinding>(FragmentCreationDetailBinding::inflate) {

    private val viewModel: CreationDetailViewModel by viewModels()
    private val download = registerGalleryDownload { viewModel.download() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)

        binding.btnBack.setOnSingleClickListener { findNavController().popBackStack() }
        binding.btnDelete.setOnSingleClickListener {
            ConfirmDialogs.delete(KEY_DELETE).show(childFragmentManager, ConfirmDialogFragment.TAG)
        }
        binding.btnShare.setOnSingleClickListener {
            val path = viewModel.creation.value?.imagePath ?: return@setOnSingleClickListener
            if (!requireContext().shareImages(listOf(path), getString(R.string.cd_share))) {
                showAppToast(R.string.error_generic)
            }
        }
        binding.btnDownload.setOnSingleClickListener { download() }
        binding.btnEdit.setOnSingleClickListener { openEdit() }
        childFragmentManager.setFragmentResultListener(KEY_NO_INTERNET, viewLifecycleOwner) { _, _ -> openEdit() }
        childFragmentManager.setFragmentResultListener(KEY_DELETE, viewLifecycleOwner) { _, result ->
            if (ConfirmDialogFragment.isConfirmed(result)) viewModel.delete()
        }

        collectWhenStarted(viewModel.creation) { creation ->
            binding.ivImage.loadWithShimmer(creation?.imagePath, binding.shimmer)
            binding.btnEdit.isVisible = creation?.type == CreationType.CHARACTER
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                CreationDetailEvent.Deleted -> findNavController().popBackStack()
                is CreationDetailEvent.Downloaded -> if (event.success) showDownloadSuccessToast() else showAppToast(R.string.save_failed)
            }
        }
    }

    /** Customize tải part từ API nên cần mạng: mất mạng → dialog No Internet, Try again gọi lại hàm này. */
    private fun openEdit() {
        val creation = viewModel.creation.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            if (!viewModel.isOnline()) {
                NoInternetDialogFragment.showOnce(childFragmentManager, KEY_NO_INTERNET)
                return@launch
            }
            navigateSafe(
                R.id.action_creationDetail_to_customize,
                Bundle().apply {
                    putString(CustomizeFragment.ARG_CONFIG_JSON, creation.configJson)
                    putLong(CustomizeFragment.ARG_CREATION_ID, creation.id)
                },
            )
        }
    }

    companion object {
        /** Id creation (Long). */
        const val ARG_CREATION_ID = "creationId"

        private const val KEY_DELETE = "detail_delete"
        private const val KEY_NO_INTERNET = "detail_no_internet"
    }
}
