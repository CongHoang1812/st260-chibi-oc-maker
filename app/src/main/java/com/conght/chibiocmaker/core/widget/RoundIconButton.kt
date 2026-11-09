package com.conght.chibiocmaker.core.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.withStyledAttributes
import com.conght.chibiocmaker.R

/**
 * Nút icon (Figma "icon" 44×44, vd. 2:553): cùng nền với [WoodButton] ([WoodButtonDrawable.orange]),
 * glyph ở giữa. attr: `app:icon` (glyph). Kích thước nút đặt bằng layout;
 * glyph `icon_button_glyph_large` khi nút ≥ `icon_button_large`, nhỏ hơn thì `icon_button_glyph`.
 */
class RoundIconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageButton(context, attrs, defStyleAttr) {

    init {
        background = WoodButtonDrawable.orange(context)
        scaleType = ScaleType.FIT_CENTER
        context.withStyledAttributes(attrs, R.styleable.RoundIconButton, defStyleAttr) {
            getDrawable(R.styleable.RoundIconButton_icon)?.let(::setImageDrawable)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val large = w >= resources.getDimensionPixelSize(R.dimen.icon_button_large)
        val glyph = resources.getDimensionPixelSize(
            if (large) R.dimen.icon_button_glyph_large else R.dimen.icon_button_glyph,
        )
        val horizontal = (w - glyph) / 2
        val vertical = (h - glyph) / 2
        // Bóng trong ở đáy lõi làm phần sáng lệch lên → glyph cũng lệch lên
        val shift = resources.getDimensionPixelSize(R.dimen.stroke_thin)
        setPadding(horizontal, vertical - shift, horizontal, vertical + shift)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        val target = if (isPressed) PRESSED_SCALE else 1f
        if (scaleX != target) {
            animate().scaleX(target).scaleY(target).setDuration(PRESS_ANIM_MS).start()
        }
    }

    private companion object {
        const val PRESSED_SCALE = 0.95f
        const val PRESS_ANIM_MS = 100L
    }
}
