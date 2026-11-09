package com.conght.chibiocmaker.core.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.conght.chibiocmaker.R
import com.conght.chibiocmaker.databinding.ViewPaperDialogBinding

/**
 * Khung dialog chung (Figma 11:429): tab tiêu đề cam đè lên mép trên thẻ kem, nội dung, vùng tuỳ biến
 * và 2 WoodButton (trái / phải). View con khai báo trong XML được đưa vào vùng tuỳ biến (giữa nội dung và hàng nút).
 * attr: `app:dialogTitle`, `app:dialogMessage`, `app:startText`, `app:endText`.
 */
class PaperDialogContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewPaperDialogBinding.inflate(LayoutInflater.from(context), this)

    /** Nút bên trái (Yes / Cancel). */
    val startButton: WoodButton get() = binding.btnStart

    /** Nút bên phải (No / Watch Ad / Done). */
    val endButton: WoodButton get() = binding.btnEnd

    init {
        binding.tvTitle.background = WoodButtonDrawable.orange(context, ringWidth = 0f)
        context.withStyledAttributes(attrs, R.styleable.PaperDialogContainer, defStyleAttr) {
            title = getString(R.styleable.PaperDialogContainer_dialogTitle)
            message = getString(R.styleable.PaperDialogContainer_dialogMessage)
            getString(R.styleable.PaperDialogContainer_startText)?.let { binding.btnStart.text = it }
            getString(R.styleable.PaperDialogContainer_endText)?.let { binding.btnEnd.text = it }
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        val extra = (0 until childCount).map(::getChildAt).filter { it !== binding.panel && it !== binding.tvTitle }
        extra.forEach {
            removeView(it)
            binding.content.addView(it)
        }
        binding.content.isVisible = extra.isNotEmpty()
    }

    var title: CharSequence?
        get() = binding.tvTitle.text
        set(value) {
            binding.tvTitle.text = value
            val hasTitle = !value.isNullOrEmpty()
            binding.tvTitle.isVisible = hasTitle
            // Không có tab tiêu đề thì thẻ không cần chừa chỗ cho phần tab đè lên
            binding.panel.updateLayoutParams<FrameLayout.LayoutParams> {
                topMargin = if (hasTitle) resources.getDimensionPixelSize(R.dimen.dialog_title_tab_overlap) else 0
            }
            binding.panel.updatePadding(
                top = resources.getDimensionPixelSize(if (hasTitle) R.dimen.dialog_card_padding_top else R.dimen.dialog_padding),
            )
        }

    var message: CharSequence?
        get() = binding.tvMessage.text
        set(value) {
            binding.tvMessage.text = value
            binding.tvMessage.isVisible = !value.isNullOrEmpty()
        }
}
