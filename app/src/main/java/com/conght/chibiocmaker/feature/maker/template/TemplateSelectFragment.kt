package com.conght.chibiocmaker.feature.maker.template

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.conght.chibiocmaker.R
import com.conght.chibiocmaker.core.common.Resource
import com.conght.chibiocmaker.core.ui.GridSpacingItemDecoration
import com.conght.chibiocmaker.core.ui.applyStatusBarPadding
import com.conght.chibiocmaker.core.ui.base.BaseFragment
import com.conght.chibiocmaker.core.ui.collectWhenStarted
import com.conght.chibiocmaker.core.ui.navigateSafe
import com.conght.chibiocmaker.core.ui.setLightStatusBar
import com.conght.chibiocmaker.core.ui.setOnSingleClickListener
import com.conght.chibiocmaker.data.model.CharacterTemplate
import com.conght.chibiocmaker.databinding.FragmentTemplateSelectBinding
import com.conght.chibiocmaker.feature.dialog.NoInternetDialogFragment
import com.conght.chibiocmaker.feature.maker.customize.CustomizeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Maker – chọn nhân vật gốc (Figma 3:278). Không có ô None.
 * Chờ API: hiện số thẻ khung giấy bằng số template lần tải trước (ô ảnh shimmer), chưa bấm được; tải xong mới sang Customize.
 */
@AndroidEntryPoint
class TemplateSelectFragment : BaseFragment<FragmentTemplateSelectBinding>(FragmentTemplateSelectBinding::inflate) {

    private val viewModel: TemplateSelectViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)

        binding.btnBack.setOnSingleClickListener { findNavController().popBackStack() }

        childFragmentManager.setFragmentResultListener(KEY_NO_INTERNET, viewLifecycleOwner) { _, _ ->
            viewModel.pendingTemplate?.let(::onTemplateClick)
        }

        val adapter = TemplateAdapter(::onTemplateClick)
        val columns = resources.getInteger(R.integer.template_grid_columns)
        binding.rvTemplates.layoutManager = GridLayoutManager(requireContext(), columns)
        binding.rvTemplates.addItemDecoration(
            GridSpacingItemDecoration(
                columns,
                resources.getDimensionPixelSize(R.dimen.template_grid_gap_h),
                resources.getDimensionPixelSize(R.dimen.template_grid_gap_v),
            ),
        )
        binding.rvTemplates.adapter = adapter
        // Lưới chỉ đổi từ thẻ chờ sang thẻ thật: không cần hiệu ứng mờ / hiện dần (gây nháy khi tải xong)
        binding.rvTemplates.itemAnimator = null
        binding.errorState.setOnRetryClickListener { viewModel.retry() }

        collectWhenStarted(combine(viewModel.state, viewModel.placeholderCount, ::Pair)) { (resource, placeholders) ->
            binding.rvTemplates.isVisible = resource !is Resource.Error
            binding.errorState.isVisible = resource is Resource.Error
            when (resource) {
                Resource.Loading -> adapter.submitList(List(placeholders ?: 0) { TemplateCell(it, null) })
                is Resource.Success -> adapter.submitList(resource.data.mapIndexed { i, t -> TemplateCell(i, t) })
                is Resource.Error -> binding.errorState.setError(resource.throwable)
            }
        }
    }

    /** Có mạng → mở Customize; không → dialog No Internet (Try again gọi lại hàm này). */
    private fun onTemplateClick(template: CharacterTemplate) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.isOnline()) {
                openCustomize(template)
            } else {
                viewModel.pendingTemplate = template
                NoInternetDialogFragment.showOnce(childFragmentManager, KEY_NO_INTERNET)
            }
        }
    }

    private fun openCustomize(template: CharacterTemplate) {
        navigateSafe(
            R.id.action_templateSelect_to_customize,
            Bundle().apply { putString(CustomizeFragment.ARG_TEMPLATE_ID, template.id) },
        )
    }

    private companion object {
        const val KEY_NO_INTERNET = "template_no_internet"
    }
}
