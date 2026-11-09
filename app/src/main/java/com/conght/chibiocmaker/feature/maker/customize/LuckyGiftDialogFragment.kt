package com.conght.chibiocmaker.feature.maker.customize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.conght.chibiocmaker.R
import com.conght.chibiocmaker.core.common.PartLocks
import com.conght.chibiocmaker.core.ui.base.BaseDialogFragment
import com.conght.chibiocmaker.core.ui.collectWhenStarted
import com.conght.chibiocmaker.core.ui.loadImage
import com.conght.chibiocmaker.core.ui.setOnSingleClickListener
import com.conght.chibiocmaker.databinding.DialogLuckyGiftBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Overlay Lucky Gift trong Customize (Figma 2:551 / 2:805 / 10:185). Dùng chung [CustomizeViewModel]
 * với màn cha, nên vòng quay, kết quả và đếm ngược sống qua xoay màn / process death.
 * START → ô sáng chạy quanh lưới rồi dừng ở các part trúng → Congratulations → Collect đóng overlay.
 */
class LuckyGiftDialogFragment : BaseDialogFragment<DialogLuckyGiftBinding>(DialogLuckyGiftBinding::inflate) {

    private val viewModel: CustomizeViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private val gridCells = mutableListOf<ImageView>()
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
        binding.btnSpin.setOnSingleClickListener { viewModel.spinLuckyGift() }
        binding.btnCollect.setOnSingleClickListener {
            viewModel.collectLuckyGift()
            dismiss()
        }
        viewModel.prepareLuckyGift()
        collectWhenStarted(viewModel.state) { render(it.luckyGift) }
    }

    private fun buildCells() {
        val inflater = LayoutInflater.from(requireContext())
        val gap = resources.getDimensionPixelSize(R.dimen.lucky_gift_cell_gap)
        val rowGap = resources.getDimensionPixelSize(R.dimen.lucky_gift_row_gap)
        gridCells.clear()
        binding.grid.removeAllViews()
        repeat(PartLocks.GRID_SIZE / COLUMNS) { row ->
            val line = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            binding.grid.addView(line)
            if (row > 0) line.updateLayoutParams<LinearLayout.LayoutParams> { topMargin = rowGap }
            repeat(COLUMNS) { column ->
                val cell = inflater.inflate(R.layout.item_lucky_gift_cell, line, false) as ImageView
                if (column > 0) (cell.layoutParams as LinearLayout.LayoutParams).marginStart = gap
                line.addView(cell)
                gridCells += cell
            }
        }

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
        binding.btnSpin.text = if (left > 0) {
            getString(R.string.timer_format, left / SECONDS_PER_MINUTE, left % SECONDS_PER_MINUTE)
        } else {
            getString(R.string.lucky_gift_start)
        }

        val result = gift.result
        if (result == null) {
            sawNoResult = true
            showResult(null)
        } else if (spinAnimation == null && binding.resultGroup.isGone()) {
            if (sawNoResult) playSpin(gift.grid, result) else showResult(result)
        }
        val busy = spinAnimation != null || result != null
        binding.btnSpin.isEnabled = gift.canSpin && !busy
        binding.btnSpin.alpha = if (binding.btnSpin.isEnabled || busy) 1f else DISABLED_ALPHA
        isCancelable = !busy
    }

    /** Ô sáng chạy ngẫu nhiên, chậm dần, rồi dừng ở các ô trúng. */
    private fun playSpin(grid: List<GiftCell>, result: List<GiftCell>) {
        val filled = gridCells.take(grid.size).ifEmpty { gridCells }
        val winners = result.map { won -> grid.indexOfFirst { it.key == won.key } }.filter { it >= 0 }
        spinAnimation = viewLifecycleOwner.lifecycleScope.launch {
            var previous: ImageView? = null
            repeat(SPIN_STEPS) { step ->
                previous?.isSelected = false
                previous = filled.random().also { it.isSelected = true }
                delay(SPIN_START_MS + step * SPIN_SLOWDOWN_MS)
            }
            previous?.isSelected = false
            winners.forEach { gridCells[it].isSelected = true }
            delay(WINNER_HOLD_MS)
            spinAnimation = null
            showResult(result)
            isCancelable = false
        }
        isCancelable = false
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

    override fun onDestroyView() {
        spinAnimation = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "LuckyGiftDialog"

        private const val COLUMNS = 5
        private const val SPIN_STEPS = 20
        private const val SPIN_START_MS = 60L
        private const val SPIN_SLOWDOWN_MS = 12L
        private const val WINNER_HOLD_MS = 700L
        private const val SECONDS_PER_MINUTE = 60
        private const val DISABLED_ALPHA = 0.6f

        fun showOnce(manager: FragmentManager) {
            if (manager.findFragmentByTag(TAG) != null) return
            LuckyGiftDialogFragment().show(manager, TAG)
        }
    }
}
