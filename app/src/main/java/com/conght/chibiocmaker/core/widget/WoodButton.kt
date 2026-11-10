package com.conght.chibiocmaker.core.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withTranslation
import androidx.core.widget.TextViewCompat
import com.conght.chibiocmaker.R

/**
 * Nút chữ chung (nền [WoodButtonDrawable]), chữ trắng; viền chữ chỉ vẽ khi `wood_button_text_stroke` > 0
 * (Figma ST260 không viền chữ).
 * attr: `android:text`, `app:variant` (orange | gray), `app:buttonWidth`, `app:woodTextStroke`.
 * Nhấn: scale 0.95.
 */
class WoodButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    /** Thứ tự trùng enum `variant` trong attrs.xml. */
    enum class Variant { ORANGE, GRAY, BLUE, RED, TELEGRAM, WHATSAPP }

    var variant: Variant = Variant.ORANGE
        set(value) {
            field = value
            applyVariant()
        }

    private var buttonWidth = -1

    /** Variant đã dựng nền: bind lại cùng variant (vd. SubTabAdapter) không tạo lại drawable. */
    private var appliedVariant: Variant? = null

    @ColorInt
    private var textStrokeColor = 0
    private var textStrokeWidth = resources.getDimension(R.dimen.wood_button_text_stroke)

    init {
        // Text.Button mặc định, trừ khi layout đã đặt android:textAppearance (vd. Text.TabSmall)
        if (attrs == null || attrs.getAttributeResourceValue(ANDROID_NS, "textAppearance", 0) == 0) {
            TextViewCompat.setTextAppearance(this, R.style.Text_Button)
        }
        gravity = Gravity.CENTER
        includeFontPadding = false
        maxLines = 1
        isClickable = true
        isFocusable = true
        minHeight = resources.getDimensionPixelSize(R.dimen.wood_button_height)
        val horizontal = resources.getDimensionPixelSize(R.dimen.screen_padding)
        setPadding(
            horizontal,
            resources.getDimensionPixelSize(R.dimen.wood_button_inset_top),
            horizontal,
            resources.getDimensionPixelSize(R.dimen.wood_button_inset_bottom),
        )
        context.withStyledAttributes(attrs, R.styleable.WoodButton, defStyleAttr) {
            variant = Variant.entries[getInt(R.styleable.WoodButton_variant, 0)]
            buttonWidth = getDimensionPixelSize(R.styleable.WoodButton_buttonWidth, -1)
            textStrokeWidth = getDimension(R.styleable.WoodButton_woodTextStroke, textStrokeWidth)
        }
        applyVariant()
    }

    private fun applyVariant() {
        if (appliedVariant == variant) return
        appliedVariant = variant
        background = WoodButtonDrawable.of(context, variant)
        textStrokeColor = ContextCompat.getColor(
            context,
            when (variant) {
                Variant.ORANGE -> R.color.button_inner_stroke
                Variant.GRAY -> R.color.tab_off_shadow
                Variant.BLUE, Variant.RED, Variant.TELEGRAM -> R.color.button_blue_stroke
                Variant.WHATSAPP -> R.color.button_whatsapp_stroke
            },
        )
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpec = if (buttonWidth > 0) {
            MeasureSpec.makeMeasureSpec(buttonWidth, MeasureSpec.EXACTLY)
        } else {
            widthMeasureSpec
        }
        super.onMeasure(widthSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        val textLayout = layout
        if (textLayout != null && textStrokeWidth > 0f) {
            // Vẽ viền chữ trước, rồi để TextView vẽ phần fill lên trên
            val p = paint
            val oldColor = p.color
            p.style = Paint.Style.STROKE
            p.strokeJoin = Paint.Join.ROUND
            p.strokeWidth = textStrokeWidth
            p.color = textStrokeColor
            val available = height - extendedPaddingTop - extendedPaddingBottom
            val vOffset = if (gravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.CENTER_VERTICAL) {
                (available - textLayout.height) / 2f
            } else {
                0f
            }
            canvas.withTranslation(compoundPaddingLeft.toFloat() - scrollX, extendedPaddingTop + vOffset) {
                textLayout.draw(this)
            }
            p.style = Paint.Style.FILL
            p.color = oldColor
        }
        super.onDraw(canvas)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        val target = if (isPressed) PRESSED_SCALE else 1f
        if (scaleX != target) {
            animate().scaleX(target).scaleY(target).setDuration(PRESS_ANIM_MS).start()
        }
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val PRESSED_SCALE = 0.95f
        const val PRESS_ANIM_MS = 100L
    }
}
