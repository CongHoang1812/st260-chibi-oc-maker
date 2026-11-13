package com.chibi.dream.cute.feature.decor.editor

import android.content.res.TypedArray
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.use
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chibi.dream.cute.R
import com.chibi.dream.cute.core.common.Resource
import com.chibi.dream.cute.core.ui.GridSpacingItemDecoration
import com.chibi.dream.cute.core.ui.LinearSpacingItemDecoration
import com.chibi.dream.cute.core.ui.applyStatusBarPadding
import com.chibi.dream.cute.core.ui.base.BaseFragment
import com.chibi.dream.cute.core.ui.collectWhenStarted
import com.chibi.dream.cute.core.ui.navigateSafe
import com.chibi.dream.cute.core.ui.setLightStatusBar
import com.chibi.dream.cute.core.ui.setOnSingleClickListener
import com.chibi.dream.cute.core.ui.showAppToast
import com.chibi.dream.cute.core.ui.skeleton.NoneAdapter
import com.chibi.dream.cute.core.ui.skeleton.SkeletonAdapter
import com.chibi.dream.cute.core.ui.skeleton.noneFirst
import com.chibi.dream.cute.data.model.DecorItem
import com.chibi.dream.cute.data.model.DecorPack
import com.chibi.dream.cute.data.model.DecorType
import com.chibi.dream.cute.databinding.FragmentDecorEditorBinding
import com.chibi.dream.cute.feature.dialog.ColorPickerDialogFragment
import com.chibi.dream.cute.feature.dialog.ConfirmDialogFragment
import com.chibi.dream.cute.feature.dialog.ConfirmDialogs
import com.chibi.dream.cute.feature.dialog.NoInternetDialogFragment
import com.chibi.dream.cute.feature.dialog.setLoadingVisible
import com.chibi.dream.cute.feature.success.SuccessFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Decor editor (Figma: nền ảnh 2:1182, nền màu 2:1240, sticker 2:1296, speech 2:1358, text 2:1452).
 * Background: tab Image = ô thêm ảnh từ máy + item của pack đầu tiên; Color; các pack còn lại
 * (mỗi pack cũng có ô thêm ảnh ở đầu).
 * Ảnh chọn từ máy chỉ đặt làm nền, không thêm ô vào lưới.
 * Ô None chỉ có ở lưới Background; Sticker / Speech không có, xoá phần tử bằng handle trên canvas.
 * Nhân vật là phần tử dưới cùng, chạm vào mới hiện khung (thu phóng / xoay / lật, không xoá).
 * Mất mạng: bấm ô trong lưới hoặc Download → dialog No Internet, Try again có mạng thì làm tiếp.
 */
@AndroidEntryPoint
class DecorEditorFragment : BaseFragment<FragmentDecorEditorBinding>(FragmentDecorEditorBinding::inflate) {

    private val viewModel: DecorEditorViewModel by viewModels()

    private val pickImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::importBackground)
    }

    private lateinit var noneAdapter: NoneAdapter
    private lateinit var cellAdapter: DecorCellAdapter
    private lateinit var gridLoading: ConcatAdapter
    private lateinit var gridReady: ConcatAdapter
    /** Tab Image / pack nền khi pack đang tải: None + ô thêm ảnh + skeleton. */
    private lateinit var gridImageLoading: ConcatAdapter
    private lateinit var gridSkeleton: SkeletonAdapter
    private lateinit var subTabAdapter: SubTabAdapter
    private lateinit var subTabsWithLoading: ConcatAdapter
    private lateinit var subTabSkeleton: SkeletonAdapter
    private lateinit var fontAdapter: FontChipAdapter
    private lateinit var colorChipAdapter: ColorChipAdapter
    private lateinit var backgroundColors: List<Int>

    /** Pack hiện tại theo loại, cập nhật từ ViewModel. */
    private var packs: Map<DecorType, Resource<List<DecorPack>>> = emptyMap()

    /** Đầu vào của hàng tab con + lưới ở lần render trước: kéo / xoay phần tử không đổi gì thì khỏi dựng lại. */
    private var renderedGridKey: GridKey? = null
    private var renderedPacks: Map<DecorType, Resource<List<DecorPack>>>? = null

    /** Bong bóng mà ô nhập ẩn et_speech đang sửa. */
    private var editingSpeechId: String? = null
    private var renderedInputFontId: String? = null

    /** Việc bấm lúc mất mạng, làm tiếp khi Try again và đã có mạng. */
    private var pendingOnlineAction: (() -> Unit)? = null

    private data class GridKey(
        val tab: EditorTab,
        val subTab: Int,
        val background: DecorBackground,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderedGridKey = null
        renderedPacks = null
        editingSpeechId = null
        renderedInputFontId = null
        pendingOnlineAction = null
        binding.content.applyStatusBarPadding()
        setLightStatusBar(true)

        backgroundColors = resources.obtainTypedArray(R.array.decor_background_colors).use { it.colors() }
        setupCanvas()
        setupTabs()
        setupLists()
        setupTextInput()
        setupDialogResults()

        binding.btnBack.setOnSingleClickListener { onBack() }
        binding.btnDownload.setOnSingleClickListener {
            runWhenOnline {
                // exportBitmap() không vẽ viền chọn / handle nên không cần bỏ chọn trước
                viewModel.save(binding.canvas.exportBitmap())
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBack()
        })

        collectWhenStarted(viewModel.data) { render() }
        val flows = viewModel.packs
        val allPacks = combine(
            flows.getValue(DecorType.BACKGROUND),
            flows.getValue(DecorType.STICKER),
            flows.getValue(DecorType.SPEECH),
        ) { bg, sticker, speech ->
            mapOf(DecorType.BACKGROUND to bg, DecorType.STICKER to sticker, DecorType.SPEECH to speech)
        }
        collectWhenStarted(allPacks) {
            packs = it
            render()
        }
        collectWhenStarted(viewModel.saving) {
            binding.btnDownload.isEnabled = !it
            setLoadingVisible(it)
        }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is DecorEditorEvent.Saved -> navigateSafe(
                    R.id.action_decorEditor_to_success,
                    Bundle().apply {
                        putLong(SuccessFragment.ARG_CREATION_ID, event.creationId)
                        putString(SuccessFragment.ARG_SOURCE, SuccessFragment.Source.MAKER.name)
                    },
                )

                DecorEditorEvent.SaveFailed -> showAppToast(R.string.save_failed)
                DecorEditorEvent.ImportFailed -> showAppToast(R.string.error_generic)
            }
        }
    }

    // ---------- Setup ----------

    private fun setupCanvas() {
        binding.canvas.listener = object : DecorCanvasView.Listener {
            override fun onSelect(id: String?) = viewModel.select(id)
            override fun onTransform(id: String, x: Float, y: Float, scale: Float, rotation: Float) =
                viewModel.transform(id, x, y, scale, rotation)

            override fun onFlip(id: String) = viewModel.flip(id)
            override fun onDelete(id: String) = viewModel.delete(id)
            override fun onEditText(id: String) {
                val element = viewModel.data.value.elements.firstOrNull { it.id == id } ?: return
                viewModel.select(id)
                if (element.type == ElementType.SPEECH) {
                    viewModel.selectMainTab(EditorTab.SPEECH)
                    showKeyboard(binding.etSpeech)
                } else {
                    viewModel.selectMainTab(EditorTab.TEXT)
                    binding.inputRow.isVisible = true
                    binding.etText.setTextAndMoveCursor(element.text.orEmpty())
                    showKeyboard(binding.etText)
                }
            }
        }
    }

    private fun setupTabs() {
        mapOf(
            binding.tabBackground to EditorTab.BACKGROUND,
            binding.tabSticker to EditorTab.STICKER,
            binding.tabSpeech to EditorTab.SPEECH,
            binding.tabText to EditorTab.TEXT,
        ).forEach { (view, tab) -> view.setOnSingleClickListener(intervalMs = TAB_DEBOUNCE_MS) { viewModel.selectMainTab(tab) } }
    }

    private fun setupLists() {
        val gap = resources.getDimensionPixelSize(R.dimen.grid_gap)
        val columns = resources.getInteger(R.integer.decor_grid_columns)

        noneAdapter = NoneAdapter(R.layout.item_none_decor) { runWhenOnline { viewModel.setBackgroundNone() } }
        cellAdapter = DecorCellAdapter(::onCellClick)
        gridSkeleton = SkeletonAdapter(R.layout.item_skeleton_decor, SKELETON_CELLS)
        gridLoading = noneFirst(noneAdapter, gridSkeleton)
        gridReady = noneFirst(noneAdapter, cellAdapter)
        gridImageLoading = noneFirst(noneAdapter, cellAdapter, gridSkeleton)
        binding.rvGrid.layoutManager = GridLayoutManager(requireContext(), columns)
        binding.rvGrid.addItemDecoration(GridSpacingItemDecoration(columns, gap, gap))

        subTabAdapter = SubTabAdapter(viewModel::selectSubTab)
        subTabSkeleton = SkeletonAdapter(R.layout.item_skeleton_sub_tab, SKELETON_PACKS)
        subTabsWithLoading = ConcatAdapter(subTabAdapter, subTabSkeleton)
        binding.rvSubTabs.addItemDecoration(LinearSpacingItemDecoration(gap))

        fontAdapter = FontChipAdapter { viewModel.setFont(it.id) }
        binding.rvFonts.adapter = fontAdapter
        binding.rvFonts.addItemDecoration(LinearSpacingItemDecoration(gap))

        val textColors = resources.obtainTypedArray(R.array.decor_text_colors).use { it.colors() }
        colorChipAdapter = ColorChipAdapter(listOf<Int?>(null) + textColors) { color ->
            if (color == null) {
                showColorPicker(KEY_TEXT_COLOR)
            } else {
                viewModel.setTextColor(color)
            }
        }
        binding.rvTextColors.adapter = colorChipAdapter
        binding.rvTextColors.addItemDecoration(LinearSpacingItemDecoration(gap))
    }

    /**
     * Edge-to-edge nên adjustResize không đẩy layout lên: tự dịch panel vừa đủ để hàng nhập chữ
     * nằm trên bàn phím. Canvas giữ nguyên kích thước.
     */
    private fun liftPanelAboveKeyboard() {
        val panel = binding.panel
        val row = binding.inputRow
        val insets = ViewCompat.getRootWindowInsets(panel)
        val ime = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
        val navBar = insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
        val overlap = ime - navBar - (panel.height - row.bottom) + resources.getDimensionPixelSize(R.dimen.grid_gap)
        panel.translationY = if (ime > 0 && row.isVisible) -overlap.coerceAtLeast(0).toFloat() else 0f
    }

    private fun setupTextInput() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.panel) { _, insets ->
            liftPanelAboveKeyboard()
            insets
        }
        binding.panel.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> liftPanelAboveKeyboard() }
        binding.btnTextDone.setOnSingleClickListener { submitText() }
        binding.etSpeech.doAfterTextChanged { text ->
            editingSpeechId?.let { viewModel.setSpeechText(it, text?.toString().orEmpty()) }
        }
        binding.etSpeech.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                true
            } else {
                false
            }
        }
        binding.etText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitText()
                true
            } else {
                false
            }
        }
    }

    private fun setupDialogResults() {
        childFragmentManager.setFragmentResultListener(KEY_EXIT, viewLifecycleOwner) { _, result ->
            if (ConfirmDialogFragment.isConfirmed(result)) findNavController().popBackStack()
        }
        childFragmentManager.setFragmentResultListener(KEY_NO_INTERNET, viewLifecycleOwner) { _, _ ->
            pendingOnlineAction?.let { action ->
                pendingOnlineAction = null
                runWhenOnline(action)
            }
        }
        childFragmentManager.setFragmentResultListener(KEY_BG_COLOR, viewLifecycleOwner) { _, result ->
            viewModel.setBackgroundColor(ColorPickerDialogFragment.color(result))
        }
        childFragmentManager.setFragmentResultListener(KEY_TEXT_COLOR, viewLifecycleOwner) { _, result ->
            viewModel.setTextColor(ColorPickerDialogFragment.color(result))
        }
    }

    // ---------- Render ----------

    private fun render() {
        val data = viewModel.data.value
        binding.canvas.render(data)

        val tab = data.mainTab
        binding.tabBackground.isSelected = tab == EditorTab.BACKGROUND
        binding.tabSticker.isSelected = tab == EditorTab.STICKER
        binding.tabSpeech.isSelected = tab == EditorTab.SPEECH
        binding.tabText.isSelected = tab == EditorTab.TEXT

        val showText = tab == EditorTab.SPEECH || tab == EditorTab.TEXT
        val target = data.textTarget
        val speech = target?.takeIf { it.type == ElementType.SPEECH }
        val leftTextInput = tab != EditorTab.TEXT && binding.etText.hasFocus()
        val leftSpeechInput = speech == null && binding.etSpeech.hasFocus()
        if (leftTextInput || leftSpeechInput) hideKeyboard()
        binding.inputRow.isVisible = tab == EditorTab.TEXT
        if (speech?.id != editingSpeechId) {
            editingSpeechId = speech?.id
            binding.etSpeech.setTextAndMoveCursor(speech?.text.orEmpty())
        }
        binding.rvFonts.isVisible = showText
        binding.rvTextColors.isVisible = showText
        // Text: font cách màu 16, màu là hàng cuối. Speech: font / màu / lưới cách nhau 8
        val speechTab = tab == EditorTab.SPEECH
        val speechGap = resources.getDimensionPixelSize(R.dimen.editor_speech_row_gap)
        binding.rvFonts.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = if (speechTab) speechGap else resources.getDimensionPixelSize(R.dimen.editor_fonts_bottom)
        }
        binding.rvTextColors.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = if (speechTab) speechGap else 0
        }
        binding.rvGrid.isVisible = tab != EditorTab.TEXT
        val fontId = target?.fontId ?: data.fontId
        fontAdapter.selectedId = fontId
        val textColor = target?.color ?: data.textColor
        colorChipAdapter.selectedColor = textColor
        renderInputColor(textColor)
        if (fontId != renderedInputFontId) {
            renderedInputFontId = fontId
            binding.etText.typeface = ResourcesCompat.getFont(requireContext(), DecorFonts.of(fontId).fontRes)
        }

        val gridKey = GridKey(tab, data.subTab, data.background)
        if (gridKey != renderedGridKey || packs !== renderedPacks) {
            renderedGridKey = gridKey
            renderedPacks = packs
            renderGrid(data, renderSubTabs(data))
        }

        val loading = packs.values.any { it is Resource.Loading }
        if (loading) binding.shimmer.startShimmer() else binding.shimmer.stopShimmer()
    }

    /** Vẽ hàng tab con, trả về Resource của pack đang chọn (null nếu tab Color / Image / Text). */
    private fun renderSubTabs(data: EditorData): Resource<DecorPack>? {
        val tab = data.mainTab
        val type = tab.packType ?: run {
            binding.rvSubTabs.isVisible = false
            return null
        }
        val resource = packs[type] ?: Resource.Loading
        val background = tab == EditorTab.BACKGROUND
        val fixed = if (background) {
            listOf(getString(R.string.tab_image), getString(R.string.tab_color))
        } else {
            emptyList()
        }
        val packList = (resource as? Resource.Success)?.data.orEmpty()
        val packTitles = packList.mapIndexed { i, p -> p.name.ifBlank { getString(R.string.tab_pack_number, i + 1) } }
        // Background: pack 1 nằm trong tab Image nên không có tab riêng
        val titles = fixed + if (background) packTitles.drop(1) else packTitles
        subTabAdapter.submitList(titles.mapIndexed { i, title -> SubTabAdapter.Tab(i, title, i == data.subTab) })
        binding.rvSubTabs.swapAdapter(if (resource is Resource.Success) subTabAdapter else subTabsWithLoading)
        // Figma Sticker / Speech không có hàng tab con: chỉ hiện khi có từ 2 pack để chọn
        binding.rvSubTabs.isVisible = fixed.isNotEmpty() || (resource is Resource.Success && titles.size > 1)

        val packIndex = packIndexOf(data)
        return when {
            packIndex == null -> null
            resource is Resource.Success -> resource.data.getOrNull(packIndex)?.let { Resource.Success(it) }
                ?: Resource.Success(DecorPack("", "", type, emptyList()))

            resource is Resource.Error -> resource
            else -> Resource.Loading
        }
    }

    private fun renderGrid(data: EditorData, pack: Resource<DecorPack>?) {
        val bg = data.background
        when (data.mainTab) {
            EditorTab.BACKGROUND -> when (data.subTab) {
                SUB_COLOR -> {
                    noneAdapter.isSelected = bg.isNone
                    val customColor = bg.color != null && bg.color !in backgroundColors
                    showCells(listOf(DecorCell.ColorWheel(customColor)) + backgroundColors.map { DecorCell.Color(it, it == bg.color) })
                }

                // Image và mọi pack nền: None + ô thêm ảnh từ máy + item của pack
                else -> {
                    noneAdapter.isSelected = bg.isNone
                    val items = (pack as? Resource.Success)?.data?.items
                    val cells = listOf(DecorCell.AddImage) +
                        items.orEmpty().map { DecorCell.Image(it.id, it.thumbnailUrl, it.imageUrl == bg.imagePath) }
                    cellAdapter.submitList(cells)
                    binding.rvGrid.swapAdapter(if (pack is Resource.Success) gridReady else gridImageLoading)
                }
            }

            EditorTab.STICKER, EditorTab.SPEECH -> showPack(pack, withNone = false) { false }

            EditorTab.TEXT -> Unit
        }
    }

    private fun showPack(pack: Resource<DecorPack>?, withNone: Boolean = true, selected: (DecorItem) -> Boolean) {
        if (pack is Resource.Success) {
            showCells(pack.data.items.map { DecorCell.Image(it.id, it.thumbnailUrl, selected(it)) }, withNone)
        } else {
            binding.rvGrid.swapAdapter(if (withNone) gridLoading else gridSkeleton)
        }
    }

    private fun showCells(cells: List<DecorCell>, withNone: Boolean = true) {
        cellAdapter.submitList(cells)
        binding.rvGrid.swapAdapter(if (withNone) gridReady else cellAdapter)
    }

    // ---------- Hành động ----------

    /** Mọi ô trong lưới (kể cả None) đều cần mạng: mất mạng thì hiện dialog No Internet. */
    private fun onCellClick(cell: DecorCell) = runWhenOnline { applyCell(cell) }

    private fun applyCell(cell: DecorCell) {
        val data = viewModel.data.value
        when (cell) {
            is DecorCell.ColorWheel -> showColorPicker(KEY_BG_COLOR)
            DecorCell.AddImage -> pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            is DecorCell.Color -> viewModel.setBackgroundColor(cell.color)
            is DecorCell.Image -> {
                val packItem = currentPackItems().firstOrNull { it.id == cell.id }
                when (data.mainTab) {
                    EditorTab.BACKGROUND -> viewModel.setBackgroundImage(packItem?.imageUrl ?: cell.url)
                    EditorTab.STICKER -> packItem?.let { viewModel.addSticker(it.imageUrl) }
                    EditorTab.SPEECH -> packItem?.let {
                        viewModel.addSpeech(it.imageUrl)
                        showKeyboard(binding.etSpeech)
                    }
                    EditorTab.TEXT -> Unit
                }
            }
        }
    }

    private fun currentPackItems(): List<DecorItem> {
        val data = viewModel.data.value
        val type = data.mainTab.packType ?: return emptyList()
        val index = packIndexOf(data) ?: return emptyList()
        val list = (packs[type] as? Resource.Success)?.data ?: return emptyList()
        return list.getOrNull(index)?.items.orEmpty()
    }

    /**
     * Pack ứng với tab con đang chọn; null = tab không phải pack (Color).
     * Background: Image = pack 0, Color, rồi pack 1, 2…
     */
    private fun packIndexOf(data: EditorData): Int? = when {
        data.mainTab != EditorTab.BACKGROUND -> data.subTab
        data.subTab == SUB_IMAGE -> 0
        data.subTab == SUB_COLOR -> null
        else -> data.subTab - 1
    }

    /** Có mạng → làm [action]; không → giữ lại và hiện dialog No Internet. */
    private fun runWhenOnline(action: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.isOnline()) {
                action()
            } else {
                pendingOnlineAction = action
                NoInternetDialogFragment.showOnce(childFragmentManager, KEY_NO_INTERNET)
            }
        }
    }

    private fun EditText.setTextAndMoveCursor(value: String) {
        setText(value)
        setSelection(value.length)
    }

    private fun submitText() {
        viewModel.submitText(binding.etText.text?.toString().orEmpty())
        binding.etText.text?.clear()
        hideKeyboard()
    }

    /** Mỗi lần mở bảng màu đều bắt đầu từ màu mặc định, không giữ màu đã chọn lần trước. */
    /**
     * Chữ đã gõ theo màu đang chọn. Placeholder "Type your content..." là màu đó làm nhạt;
     * màu quá sáng (vd. trắng, trùng nền ô nhập) thì dùng xám nhạt để vẫn đọc được.
     */
    private fun renderInputColor(@ColorInt textColor: Int) {
        binding.etText.setTextColor(textColor)
        val hint = if (ColorUtils.calculateLuminance(textColor) > LIGHT_LUMINANCE) {
            ContextCompat.getColor(requireContext(), R.color.hint_gray_light)
        } else {
            ColorUtils.setAlphaComponent(textColor, HINT_ALPHA)
        }
        binding.etText.setHintTextColor(hint)
    }

    private fun showColorPicker(requestKey: String) {
        if (childFragmentManager.findFragmentByTag(ColorPickerDialogFragment.TAG) == null) {
            ColorPickerDialogFragment.newInstance(requestKey, Color.RED).show(childFragmentManager, ColorPickerDialogFragment.TAG)
        }
    }

    private fun onBack() {
        if (viewModel.data.value.isDirty) {
            ConfirmDialogs.exit(KEY_EXIT).show(childFragmentManager, ConfirmDialogFragment.TAG)
        } else {
            findNavController().popBackStack()
        }
    }

    private fun showKeyboard(input: EditText) {
        input.post {
            input.requestFocus()
            WindowCompat.getInsetsController(requireActivity().window, input).show(WindowInsetsCompat.Type.ime())
        }
    }

    private fun hideKeyboard() {
        binding.etText.clearFocus()
        binding.etSpeech.clearFocus()
        WindowCompat.getInsetsController(requireActivity().window, binding.etText).hide(WindowInsetsCompat.Type.ime())
    }


    private fun RecyclerView.swapAdapter(target: RecyclerView.Adapter<*>) {
        if (adapter !== target) adapter = target
    }

    private fun TypedArray.colors(): List<Int> = (0 until length()).map { getColor(it, Color.WHITE) }

    companion object {
        /** Đường dẫn ảnh nhân vật tạm (cacheDir, internal storage). */
        const val ARG_CHARACTER_PATH = "characterPath"

        /** JSON CharacterConfig của nhân vật. */
        const val ARG_CONFIG_JSON = "configJson"

        private const val KEY_EXIT = "editor_exit"
        private const val KEY_BG_COLOR = "editor_bg_color"
        private const val KEY_TEXT_COLOR = "editor_text_color"
        private const val KEY_NO_INTERNET = "editor_no_internet"
        private const val SUB_IMAGE = 0
        private const val SUB_COLOR = 1
        private const val SKELETON_CELLS = 14
        private const val SKELETON_PACKS = 3
        private const val TAB_DEBOUNCE_MS = 150L
        private const val HINT_ALPHA = 0x80
        private const val LIGHT_LUMINANCE = 0.7
    }
}
