package com.chibi.dream.cute.feature.maker.customize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.chibi.dream.cute.R
import com.chibi.dream.cute.core.common.PartLocks
import com.chibi.dream.cute.core.ui.base.BaseDialogFragment
import com.chibi.dream.cute.core.ui.collectWhenStarted
import com.chibi.dream.cute.core.ui.loadImage
import com.chibi.dream.cute.core.ui.setOnSingleClickListener
import com.chibi.dream.cute.databinding.DialogLuckyGiftBinding
import com.chibi.dream.cute.feature.dialog.ConfirmDialogFragment
import com.chibi.dream.cute.feature.dialog.ConfirmDialogs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Overlay Lucky Gift trong Customize (Figma 2:551 / 2:805 / 10:185). Dùng chung [CustomizeViewModel]
 * với màn cha, nên vòng quay, kết quả và đếm ngược sống qua xoay màn / process death.
 * START → 5 cột cuộn như máy slot, dừng lần lượt từ trái sang phải, sáng các part trúng → Congratulations
 * → Collect đóng overlay. Đang đếm ngược thì nút thành 🎬 FREE: xem quảng cáo để quay thêm một lượt.
 */
class LuckyGiftDialogFragment : BaseDialogFragment<DialogLuckyGiftBinding>(DialogLuckyGiftBinding::inflate) {

    private val viewModel: CustomizeViewModel by viewModels(ownerProducer = { requireParentFragment() })

    /** Ô của vòng quay theo thứ tự hàng (index = row * COLUMNS + column), khớp thứ tự của grid. */
    private val gridCells = mutableListOf<ImageView>()

    /** Mỗi cột một dải ô dọc nằm trong khung cắt; lúc quay thêm ô đệm vào dải rồi trượt xuống. */
    private val reelStrips = mutableListOf<LinearLayout>()
    private val resultCells = mutableListOf<ImageView>()
    private var shownGrid: List<GiftCell>? = null
    private var shownResult: List<GiftCell>? = null

    /** Đã thấy trạng thái chưa quay: kết quả đến sau đó thì chạy hiệu ứng, còn mở lại khi đã có kết quả thì hiện luôn. */
    private var sawNoResult = false
    private var spinAnimation: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        shownGrid = null
        shownResult = null
        sawNoResult = false
        buildCells()
        binding.btnSpin.setOnSingleClickListener { onSpinClick() }
        childFragmentManager.setFragmentResultListener(KEY_WATCH_AD, viewLifecycleOwner) { _, result ->
            if (ConfirmDialogFragment.isConfirmed(result)) viewModel.spinLuckyGift(withAd = true)
        }
        binding.btnCollect.setOnSingleClickListener {
            viewModel.collectLuckyGift()
            dismiss()
        }
        viewModel.prepareLuckyGift()
        collectWhenStarted(viewModel.state) { render(it.luckyGift) }
    }

    /** Hết đếm ngược: quay luôn. Đang đếm ngược: hỏi xem quảng cáo để quay thêm một lượt. */
    private fun onSpinClick() {
        val gift = viewModel.state.value.luckyGift ?: return
        when {
            gift.canSpin -> viewModel.spinLuckyGift()
            gift.canSpinWithAd && childFragmentManager.findFragmentByTag(ConfirmDialogFragment.TAG) == null ->
                ConfirmDialogs.luckyGiftAd(KEY_WATCH_AD).show(childFragmentManager, ConfirmDialogFragment.TAG)
        }
    }

    private fun buildCells() {
        val inflater = LayoutInflater.from(requireContext())
        val gap = resources.getDimensionPixelSize(R.dimen.lucky_gift_cell_gap)
        val cellSize = resources.getDimensionPixelSize(R.dimen.lucky_gift_cell)
        val reelHeight = reelHeight(ROWS)
        val cells = arrayOfNulls<ImageView>(ROWS * COLUMNS)
        reelStrips.clear()
        binding.grid.removeAllViews()
        repeat(COLUMNS) { column ->
            // FrameLayout cắt phần dải nằm ngoài khung (clipChildren mặc định)
            val reel = FrameLayout(requireContext())
            binding.grid.addView(reel, LinearLayout.LayoutParams(cellSize, reelHeight).apply {
                if (column > 0) marginStart = gap
            })
            val strip = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
            reel.addView(strip, FrameLayout.LayoutParams(cellSize, reelHeight))
            repeat(ROWS) { row ->
                cells[row * COLUMNS + column] = inflater.addReelCell(strip, row)
            }
            reelStrips += strip
        }
        gridCells.clear()
        gridCells += cells.filterNotNull()

        val resultSize = resources.getDimensionPixelSize(R.dimen.lucky_gift_result_cell)
        resultCells.clear()
        binding.resultRow.removeAllViews()
        repeat(PartLocks.UNLOCK_PER_SPIN) { index ->
            val cell = inflater.inflate(R.layout.item_lucky_gift_cell, binding.resultRow, false) as ImageView
            cell.layoutParams = LinearLayout.LayoutParams(resultSize, resultSize).apply {
                if (index > 0) marginStart = gap
            }
            binding.resultRow.addView(cell)
            resultCells += cell
        }
    }

    private fun render(gift: LuckyGiftState?) {
        // Màn cha chưa có template (đang tải lại) → không có gì để quay
        if (gift == null) {
            dismissAllowingStateLoss()
            return
        }
        if (gift.grid != shownGrid) {
            shownGrid = gift.grid
            gridCells.forEachIndexed { index, cell ->
                val item = gift.grid.getOrNull(index)
                cell.isSelected = false
                if (item == null) cell.setImageDrawable(null) else cell.loadImage(item.thumbnailUrl, R.color.white)
            }
        }

        val left = gift.remainingSeconds
        // Hết giờ: START. Đang đếm ngược, chưa xem quảng cáo: 🎬 FREE + ⏱ giờ còn lại bên dưới (Figma 2:760).
        // Đã dùng lượt quảng cáo: giờ còn lại nằm trên nút (Figma 2:888), chờ hết giờ mới quay tiếp
        val timer = getString(R.string.timer_format, left / SECONDS_PER_MINUTE, left % SECONDS_PER_MINUTE)
        val freeAd = left > 0 && !gift.adSpinUsed
        binding.ivSpinAd.isVisible = freeAd
        binding.tvSpin.text = when {
            left == 0 -> getString(R.string.lucky_gift_start)
            freeAd -> getString(R.string.lucky_gift_free)
            else -> timer
        }
        binding.spinTimer.isVisible = freeAd
        binding.tvSpinTimer.text = timer

        val result = gift.result
        if (result == null) {
            sawNoResult = true
            showResult(null)
        } else if (spinAnimation == null && binding.resultGroup.isGone()) {
            if (sawNoResult) playSpin(gift.grid, result) else showResult(result)
        }
        val busy = spinAnimation != null || result != null
        binding.btnSpin.isEnabled = (gift.canSpin || gift.canSpinWithAd) && !busy
        // Nút đang hiện giờ (Figma 2:888) vẫn đậm như thường
        val showsTimer = left > 0 && gift.adSpinUsed
        binding.btnSpin.alpha = if (binding.btnSpin.isEnabled || busy || showsTimer) 1f else DISABLED_ALPHA
        isCancelable = !busy
    }

    /**
     * Mỗi cột cuộn xuống qua một loạt ô ngẫu nhiên rồi chậm dần, dừng lần lượt từ trái sang phải
     * (cột sau cuộn dài hơn, cùng tốc độ). Các part trúng luôn dừng ở hàng giữa, dừng hết thì sáng hàng đó.
     */
    private fun playSpin(grid: List<GiftCell>, result: List<GiftCell>) {
        // Chụp ảnh đang hiện làm điểm bắt đầu của cuộn, trước khi đổi ô thật sang bố cục kết quả
        val before = gridCells.map { it.drawable?.constantState?.newDrawable(resources) ?: it.drawable }
        val winners = arrangeWithWinnersInMiddle(grid, result)
        val thumbnails = grid.map { it.thumbnailUrl }
        val inflater = LayoutInflater.from(requireContext())
        val pitch = resources.getDimensionPixelSize(R.dimen.lucky_gift_cell) +
            resources.getDimensionPixelSize(R.dimen.lucky_gift_row_gap)
        var longest = 0L
        reelStrips.forEachIndexed { column, strip ->
            val fillers = REEL_FILLERS + column * REEL_FILLERS_STEP
            // Dải: [ô thật – điểm dừng] [ô ngẫu nhiên] [bản sao ô đang hiện – điểm bắt đầu]
            repeat(fillers) { index ->
                val cell = inflater.addReelCell(strip, ROWS + index)
                thumbnails.randomOrNull()?.let { cell.loadImage(it, R.color.white) }
            }
            repeat(ROWS) { row ->
                val copy = inflater.addReelCell(strip, ROWS + fillers + row)
                copy.setImageDrawable(before.getOrNull(row * COLUMNS + column))
            }
            strip.updateLayoutParams { height = reelHeight(ROWS * 2 + fillers) }
            strip.translationY = -((ROWS + fillers) * pitch).toFloat()
            val duration = REEL_BASE_MS + column * REEL_STAGGER_MS
            strip.animate()
                .translationY(0f)
                .setDuration(duration)
                .setInterpolator(DecelerateInterpolator(REEL_DECELERATION))
                .start()
            longest = maxOf(longest, duration)
        }
        spinAnimation = viewLifecycleOwner.lifecycleScope.launch {
            delay(longest)
            resetReels()
            winners.forEach { gridCells[it].isSelected = true }
            delay(WINNER_HOLD_MS)
            spinAnimation = null
            showResult(result)
            isCancelable = false
        }
        isCancelable = false
    }

    /**
     * Đặt lại ảnh các ô thật (điểm dừng của cuộn): part trúng nằm giữa hàng giữa, part còn lại rải vào
     * các ô khác (hàng giữa chỉ chứa part trúng). Trả về index các ô trúng.
     */
    private fun arrangeWithWinnersInMiddle(grid: List<GiftCell>, result: List<GiftCell>): List<Int> {
        val count = result.size.coerceAtMost(COLUMNS)
        val firstWinner = (ROWS / 2) * COLUMNS + (COLUMNS - count) / 2
        val winners = (firstWinner until firstWinner + count).toList()
        val middleRow = (ROWS / 2) * COLUMNS until (ROWS / 2 + 1) * COLUMNS
        val arrangement = arrayOfNulls<GiftCell>(gridCells.size)
        winners.forEachIndexed { index, cell -> arrangement[cell] = result[index] }
        val wonKeys = result.map { it.key }.toSet()
        val others = grid.filter { it.key !in wonKeys }.shuffled()
        val freeSlots = gridCells.indices.filter { it !in middleRow }
        val slots = if (others.size < freeSlots.size) freeSlots.shuffled() else freeSlots
        slots.zip(others).forEach { (cell, item) -> arrangement[cell] = item }
        gridCells.forEachIndexed { index, cell ->
            cell.isSelected = false
            val item = arrangement[index]
            if (item == null) cell.setImageDrawable(null) else cell.loadImage(item.thumbnailUrl, R.color.white)
        }
        return winners
    }

    /** Bỏ ô đệm, dải về đúng 3 ô thật. */
    private fun resetReels() {
        reelStrips.forEach { strip ->
            strip.animate().cancel()
            strip.translationY = 0f
            if (strip.childCount > ROWS) strip.removeViews(ROWS, strip.childCount - ROWS)
            strip.updateLayoutParams { height = reelHeight(ROWS) }
        }
    }

    /** Thêm một ô vào cuối dải [strip]; ô thứ [position] > 0 cách ô trên một khoảng hàng. */
    private fun LayoutInflater.addReelCell(strip: LinearLayout, position: Int): ImageView {
        val cell = inflate(R.layout.item_lucky_gift_cell, strip, false) as ImageView
        if (position > 0) {
            (cell.layoutParams as LinearLayout.LayoutParams).topMargin =
                resources.getDimensionPixelSize(R.dimen.lucky_gift_row_gap)
        }
        strip.addView(cell)
        return cell
    }

    private fun reelHeight(cells: Int): Int {
        val cell = resources.getDimensionPixelSize(R.dimen.lucky_gift_cell)
        val rowGap = resources.getDimensionPixelSize(R.dimen.lucky_gift_row_gap)
        return cells * cell + (cells - 1) * rowGap
    }

    private fun showResult(result: List<GiftCell>?) {
        binding.spinGroup.isInvisible = result != null
        binding.resultGroup.isVisible = result != null
        if (result == null || result == shownResult) return
        shownResult = result
        resultCells.forEachIndexed { index, cell ->
            val item = result.getOrNull(index)
            cell.isVisible = item != null
            item?.let { cell.loadImage(it.thumbnailUrl, R.color.white) }
        }
    }

    private fun View.isGone() = visibility == View.GONE

    /** Lớp tối sau overlay đậm hơn dialog chung: đen 70% (Figma 2:759). */
    override fun onStart() {
        super.onStart()
        dialog?.window?.setDimAmount(resources.getFraction(R.fraction.lucky_gift_dim, 1, 1))
    }

    override fun onDestroyView() {
        reelStrips.forEach { it.animate().cancel() }
        reelStrips.clear()
        spinAnimation = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "LuckyGiftDialog"

        private const val KEY_WATCH_AD = "lucky_gift_watch_ad"
        private const val COLUMNS = 5
        private const val ROWS = PartLocks.GRID_SIZE / COLUMNS
        private const val REEL_FILLERS = 12
        private const val REEL_FILLERS_STEP = 4
        private const val REEL_BASE_MS = 1_400L
        private const val REEL_STAGGER_MS = 350L
        private const val REEL_DECELERATION = 1.6f
        private const val SECONDS_PER_MINUTE = 60
        private const val WINNER_HOLD_MS = 700L
        private const val DISABLED_ALPHA = 0.6f

        fun showOnce(manager: FragmentManager) {
            if (manager.findFragmentByTag(TAG) != null) return
            LuckyGiftDialogFragment().show(manager, TAG)
        }
    }
}
