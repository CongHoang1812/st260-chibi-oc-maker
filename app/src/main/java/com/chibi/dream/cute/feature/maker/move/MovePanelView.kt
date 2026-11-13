package com.chibi.dream.cute.feature.maker.move

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.chibi.dream.cute.core.ui.setOnRepeatClickListener
import com.chibi.dream.cute.core.ui.setOnSingleClickListener
import com.chibi.dream.cute.databinding.ViewMovePanelBinding

/** Hướng nào còn đi được / zoom được; false = bộ phận đã chạm giới hạn, nút tương ứng bị disable. */
data class MoveLimits(
    val canMoveLeft: Boolean = true,
    val canMoveRight: Boolean = true,
    val canMoveUp: Boolean = true,
    val canMoveDown: Boolean = true,
    val canZoomIn: Boolean = true,
    val canZoomOut: Boolean = true,
)

/**
 * Panel chế độ Move (Figma 2:945): RESET bộ phận, Exit, xoay trái / phải, D-pad.
 * Figma không có nút zoom: zoom bằng hai ngón tay trên canvas (CustomizeFragment `onGesture`).
 * Các nút xoay / di chuyển giữ để lặp (80ms). Bước: dịch [MOVE_STEP_DP] dp, xoay [ROTATE_STEP_DEG]°.
 */
class MovePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onExit()
        fun onMove(dxDp: Float, dyDp: Float)
        fun onRotate(degrees: Float)

        /** RESET: đưa vị trí / góc xoay / cỡ của bộ phận đang chọn về mặc định. */
        fun onResetPart()
    }

    var listener: Listener? = null

    private val binding: ViewMovePanelBinding

    init {
        orientation = VERTICAL
        binding = ViewMovePanelBinding.inflate(LayoutInflater.from(context), this)
        binding.btnExit.setOnSingleClickListener { listener?.onExit() }
        binding.btnResetPart.setOnSingleClickListener { listener?.onResetPart() }
        binding.btnRotateLeft.setOnRepeatClickListener { listener?.onRotate(-ROTATE_STEP_DEG) }
        binding.btnRotateRight.setOnRepeatClickListener { listener?.onRotate(ROTATE_STEP_DEG) }
        binding.btnUp.setOnRepeatClickListener { listener?.onMove(0f, -MOVE_STEP_DP) }
        binding.btnDown.setOnRepeatClickListener { listener?.onMove(0f, MOVE_STEP_DP) }
        binding.btnLeft.setOnRepeatClickListener { listener?.onMove(-MOVE_STEP_DP, 0f) }
        binding.btnRight.setOnRepeatClickListener { listener?.onMove(MOVE_STEP_DP, 0f) }
    }

    fun setLimits(limits: MoveLimits) {
        binding.btnLeft.setActive(limits.canMoveLeft)
        binding.btnRight.setActive(limits.canMoveRight)
        binding.btnUp.setActive(limits.canMoveUp)
        binding.btnDown.setActive(limits.canMoveDown)
    }

    private fun View.setActive(active: Boolean) {
        isEnabled = active
        alpha = if (active) 1f else DISABLED_ALPHA
    }

    companion object {
        const val MOVE_STEP_DP = 10f
        const val ROTATE_STEP_DEG = 15f
        private const val DISABLED_ALPHA = 0.5f
    }
}
