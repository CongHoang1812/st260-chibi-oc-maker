package com.conght.chibiocmaker.feature.creation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conght.chibiocmaker.R
import com.conght.chibiocmaker.core.ui.GridSpacingItemDecoration
import com.conght.chibiocmaker.core.ui.applyStatusBarPadding
import com.conght.chibiocmaker.core.ui.base.BaseFragment
import com.conght.chibiocmaker.core.ui.collectWhenStarted
import com.conght.chibiocmaker.core.ui.navigateSafe
import com.conght.chibiocmaker.core.ui.registerGalleryDownload
import com.conght.chibiocmaker.core.ui.setLightStatusBar
import com.conght.chibiocmaker.core.ui.setOnSingleClickListener
import com.conght.chibiocmaker.core.ui.showAppToast
import com.conght.chibiocmaker.core.ui.showDownloadSuccessToast
import com.conght.chibiocmaker.core.ui.shareImages
import com.conght.chibiocmaker.core.ui.startTelegramStickerImport
import com.conght.chibiocmaker.core.ui.whatsAppAddStickerIntent
import com.conght.chibiocmaker.core.widget.WoodButton
import com.conght.chibiocmaker.data.model.Creation
import com.conght.chibiocmaker.data.model.CreationType
import com.conght.chibiocmaker.data.model.StickerExport
import com.conght.chibiocmaker.data.model.StickerTarget
import com.conght.chibiocmaker.data.model.WHATSAPP_PACK_MAX_STICKERS
import com.conght.chibiocmaker.data.model.WHATSAPP_PACK_MIN_STICKERS
import com.conght.chibiocmaker.databinding.FragmentMyCreationBinding
import com.conght.chibiocmaker.feature.dialog.ConfirmDialogFragment
import com.conght.chibiocmaker.feature.dialog.ConfirmDialogs
import com.conght.chibiocmaker.feature.dialog.NoInternetDialogFragment
import com.conght.chibiocmaker.feature.dialog.PackNameDialogFragment
import com.conght.chibiocmaker.feature.dialog.setLoadingVisible
import com.conght.chibiocmaker.feature.maker.customize.CustomizeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * My creation (Figma 3:1228): 2 tab Character / Design, dữ liệu thật từ Room.
 * Long-press → chế độ chọn nhiều (xoá / share / download; tab Character thêm xuất sticker).
 */
@AndroidEntryPoint
class MyCreationFragment : BaseFragment<FragmentMyCreationBinding>(FragmentMyCreationBinding::inflate) {

    private val viewModel: MyCreationViewModel by viewModels()
    private val downloadSelected = registerGalleryDownload { viewModel.download(viewModel.state.value.selectedIds) }

    /** Người dùng tự huỷ thì không có `validation_error`, không cần báo gì. */
    private val addToWhatsApp = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val error = result.data?.getStringExtra(EXTRA_VALIDATION_ERROR)
        when {
            result.resultCode == Activity.RESULT_OK -> showAppToast(R.string.sticker_added_to_whatsapp)
            error != null -> {
                Log.w(TAG, "WhatsApp rejected sticker pack: $error")
                showAppToast(R.string.sticker_export_failed)
            }
        }
    }

    private val adapter = CreationAdapter(object : CreationAdapter.Listener {
        override fun onOpen(creation: Creation) = openDetail(creation)

        override fun onLongPress(creation: Creation) = viewModel.startSelection(creation.id)
        override fun onToggle(creation: Creation) = viewModel.toggle(creation.id)
        override fun onDelete(creation: Creation) = confirmDelete(listOf(creation.id))
        override fun onEdit(creation: Creation) = openEdit(creation)
    })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)

        val columns = resources.getInteger(R.integer.creation_grid_columns)
        binding.rvCreations.layoutManager = GridLayoutManager(requireContext(), columns)
        binding.rvCreations.addItemDecoration(
            GridSpacingItemDecoration(
                columns,
                resources.getDimensionPixelSize(R.dimen.creation_grid_gap_h),
                resources.getDimensionPixelSize(R.dimen.creation_grid_gap_v),
            ),
        )
        binding.rvCreations.adapter = adapter
        setupTapOutsideToExitSelection()

        binding.btnBack.setOnSingleClickListener { onBack() }
        binding.tabCharacter.setOnSingleClickListener { viewModel.selectTab(CreationType.CHARACTER) }
        binding.tabDesign.setOnSingleClickListener { viewModel.selectTab(CreationType.DESIGN) }
        binding.btnDelete.setOnSingleClickListener { confirmDelete(viewModel.state.value.selectedIds) }
        binding.btnShare.setOnSingleClickListener { share(viewModel.state.value.selectedItems) }
        binding.btnDownload.setOnSingleClickListener { downloadSelected() }
        binding.btnTelegram.setOnSingleClickListener { viewModel.exportStickers(StickerTarget.TELEGRAM) }
        binding.btnWhatsapp.setOnSingleClickListener { viewModel.exportStickers(StickerTarget.WHATSAPP) }

        childFragmentManager.setFragmentResultListener(KEY_NO_INTERNET, viewLifecycleOwner) { _, _ ->
            pendingOnline?.invoke()
        }
        childFragmentManager.setFragmentResultListener(KEY_DELETE, viewLifecycleOwner) { _, result ->
            viewModel.onDeleteConfirmed(ConfirmDialogFragment.isConfirmed(result))
        }
        childFragmentManager.setFragmentResultListener(KEY_PACK_NAME, viewLifecycleOwner) { _, result ->
            viewModel.exportToWhatsApp(PackNameDialogFragment.packName(result))
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBack()
        })

        collectWhenStarted(viewModel.state, ::render)
        collectWhenStarted(viewModel.busy) { setLoadingVisible(it) }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is MyCreationEvent.Downloaded -> if (event.success) showDownloadSuccessToast() else showAppToast(R.string.save_failed)
                MyCreationEvent.InvalidWhatsAppStickerCount -> binding.root.showAppToast(
                    getString(R.string.whatsapp_sticker_count, WHATSAPP_PACK_MIN_STICKERS, WHATSAPP_PACK_MAX_STICKERS),
                )
                MyCreationEvent.StickerExportFailed -> showAppToast(R.string.sticker_export_failed)
                is MyCreationEvent.StickersReady -> onStickersReady(event.export)
                MyCreationEvent.RequestPackName -> showPackNameDialog()
            }
        }
    }

    private fun onStickersReady(export: StickerExport) {
        when (export) {
            StickerExport.WhatsAppNotInstalled -> showAppToast(R.string.whatsapp_not_installed)
            StickerExport.WhatsAppAlreadyAdded -> showAppToast(R.string.sticker_already_in_whatsapp)
            is StickerExport.WhatsApp -> try {
                val context = requireContext()
                addToWhatsApp.launch(context.whatsAppAddStickerIntent(export, getString(R.string.add_to_whatsapp)))
            } catch (_: ActivityNotFoundException) {
                showAppToast(R.string.whatsapp_not_installed)
            }
            is StickerExport.Telegram -> if (!requireContext().startTelegramStickerImport(export)) {
                showAppToast(R.string.telegram_not_installed)
            }
        }
    }

    override fun onDestroyView() {
        // Adapter sống theo Fragment -> gỡ khỏi RecyclerView để không giữ view cũ
        binding.rvCreations.adapter = null
        super.onDestroyView()
    }

    /**
     * Chế độ chọn: chạm ngoài thẻ thì thoát. RecyclerView luôn nhận touch nên vùng trống trong lưới
     * phải bắt riêng; phần còn lại của màn rơi xuống [FragmentMyCreationBinding.content].
     */
    private fun setupTapOutsideToExitSelection() {
        binding.content.setOnClickListener { viewModel.exitSelection() }
        val tapDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (binding.rvCreations.findChildViewUnder(e.x, e.y) == null) viewModel.exitSelection()
                return false
            }
        })
        binding.rvCreations.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                tapDetector.onTouchEvent(e)
                return false
            }
        })
    }

    private fun render(state: MyCreationUiState) {
        renderTabs(state.tab)
        // Ngoài chế độ chọn thì nền không nhận click, tránh tiếng click khi chạm vùng trống
        binding.content.isClickable = state.selectionMode
        val items = state.items
        val empty = items != null && items.isEmpty()
        binding.emptyState.isVisible = empty
        binding.tvEmpty.setText(
            if (state.tab == CreationType.CHARACTER) R.string.no_character_here else R.string.no_design_here,
        )
        adapter.submitList(items.orEmpty().map { CreationItem(it, state.selectionMode, it.id in state.selectedIds) })

        val hasSelection = state.selectionMode && state.selectedIds.isNotEmpty()
        listOf(binding.btnDelete, binding.btnShare, binding.btnDownload).forEach {
            it.isVisible = state.selectionMode
            it.isEnabled = hasSelection
            it.alpha = if (hasSelection) 1f else DISABLED_ALPHA
        }
        binding.stickerBar.isVisible = state.selectionMode && state.tab == CreationType.CHARACTER
        listOf(binding.btnTelegram, binding.btnWhatsapp).forEach {
            it.isEnabled = hasSelection
            it.alpha = if (hasSelection) 1f else DISABLED_ALPHA
        }
    }

    /** Tab đang chọn: nút cam, tab còn lại xám (Figma 2:1555). */
    private fun renderTabs(tab: CreationType) {
        listOf(binding.tabCharacter to CreationType.CHARACTER, binding.tabDesign to CreationType.DESIGN).forEach { (view, type) ->
            val selected = type == tab
            view.isSelected = selected
            view.variant = if (selected) WoodButton.Variant.ORANGE else WoodButton.Variant.GRAY
        }
    }

    private fun showPackNameDialog() {
        if (childFragmentManager.findFragmentByTag(PackNameDialogFragment.TAG) != null) return
        PackNameDialogFragment.newInstance(KEY_PACK_NAME).show(childFragmentManager, PackNameDialogFragment.TAG)
    }

    private fun confirmDelete(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        viewModel.requestDelete(ids)
        ConfirmDialogs.delete(KEY_DELETE).show(childFragmentManager, ConfirmDialogFragment.TAG)
    }

    private fun share(items: List<Creation>) {
        if (items.isEmpty()) return
        if (!requireContext().shareImages(items.map { it.imagePath }, getString(R.string.cd_share))) {
            showAppToast(R.string.error_generic)
        }
    }

    /** Việc bấm lúc mất mạng (Edit), làm lại khi Try again có mạng. */
    private var pendingOnline: (() -> Unit)? = null

    /** Mất mạng → dialog No Internet, giữ [action] để Try again làm lại. */
    private fun runWhenOnline(action: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (!viewModel.isOnline()) {
                pendingOnline = { runWhenOnline(action) }
                NoInternetDialogFragment.showOnce(childFragmentManager, KEY_NO_INTERNET)
                return@launch
            }
            pendingOnline = null
            action()
        }
    }

    /** Xem ảnh đã lưu ở internal storage: không cần mạng. */
    private fun openDetail(creation: Creation) {
        navigateSafe(
            R.id.action_myCreation_to_creationDetail,
            Bundle().apply { putLong(CreationDetailFragment.ARG_CREATION_ID, creation.id) },
        )
    }

    /** Customize tải part từ API nên cần mạng. */
    private fun openEdit(creation: Creation) = runWhenOnline {
        navigateSafe(
            R.id.action_myCreation_to_customize,
            Bundle().apply { putString(CustomizeFragment.ARG_CONFIG_JSON, creation.configJson) },
        )
    }

    private fun onBack() {
        if (viewModel.state.value.selectionMode) viewModel.exitSelection() else findNavController().popBackStack()
    }

    companion object {
        private const val KEY_DELETE = "creation_delete"
        private const val KEY_PACK_NAME = "creation_pack_name"
        private const val KEY_NO_INTERNET = "creation_no_internet"
        private const val DISABLED_ALPHA = 0.5f
        private const val TAG = "MyCreation"
        private const val EXTRA_VALIDATION_ERROR = "validation_error"
    }
}
