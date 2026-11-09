package com.conght.chibiocmaker.core.widget

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withClip
import com.conght.chibiocmaker.R

/**
 * Nền nút chung (Figma "icon" 2:582, 35:116, 2:628...): khung trắng bo 16 viền nâu 1dp, bên trong
 * (cách 4dp) là lõi bo 12 viền đậm 1dp, bóng trong 3dp ở đáy lõi và đốm sáng nhỏ góc trên trái.
 * [ringWidth] = 0 bỏ khung trắng (tab tiêu đề dialog, Figma 2:453).
 * Bo góc không vượt quá nửa chiều cao nên nút thấp (tab 34dp) thành viên thuốc.
 */
class WoodButtonDrawable(
    res: Resources,
    @ColorInt private val borderColor: Int,
    @ColorInt private val ringColor: Int,
    @ColorInt private val coreColor: Int,
    @ColorInt private val coreStrokeColor: Int,
    @ColorInt shadeColor: Int,
    @ColorInt private val highlightColor: Int,
    private val ringWidth: Float = res.getDimension(R.dimen.wood_button_ring),
) : Drawable() {

    private val outerRadius = res.getDimension(R.dimen.wood_button_radius)
    private val coreRadius = res.getDimension(R.dimen.wood_button_core_radius)
    private val stroke = res.getDimension(R.dimen.stroke_thin)
    private val shade = res.getDimension(R.dimen.wood_button_shade)
    private val drop = res.getDimension(R.dimen.wood_button_drop)
    private val highlightX = res.getDimension(R.dimen.wood_button_highlight_x)
    private val highlightY = res.getDimension(R.dimen.wood_button_highlight_y)
    private val highlightW = res.getDimension(R.dimen.wood_button_highlight_w)
    private val highlightH = res.getDimension(R.dimen.wood_button_highlight_h)

    /** Lõi đã phủ bóng trong (đen 25% trên màu lõi): tô cả lõi màu này rồi phủ lõi gốc lệch lên [shade]. */
    @ColorInt
    private val shadedCore = ColorUtils.compositeColors(shadeColor, coreColor)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outer = RectF()
    private val core = RectF()
    private val tmp = RectF()
    private val outerPath = Path()
    private val corePath = Path()
    private var outerR = 0f
    private var coreR = 0f

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        outer.set(bounds)
        outerR = minOf(if (ringWidth > 0f) outerRadius else coreRadius, outer.height() / 2f)
        outerPath.reset()
        outerPath.addRoundRect(outer, outerR, outerR, Path.Direction.CW)
        core.set(outer)
        core.inset(ringWidth, ringWidth)
        coreR = if (ringWidth > 0f) (outerR - ringWidth).coerceAtLeast(0f) else outerR
        corePath.reset()
        corePath.addRoundRect(core, coreR, coreR, Path.Direction.CW)
    }

    override fun draw(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        if (ringWidth > 0f) {
            // Khung trắng + bóng đổ nhẹ của lõi xuống khung (Figma: drop shadow 0 4 4, bị khung cắt)
            paint.color = ringColor
            canvas.drawPath(outerPath, paint)
            canvas.withClip(outerPath) {
                paint.color = DROP_COLOR
                tmp.set(core)
                tmp.offset(0f, drop)
                drawRoundRect(tmp, coreR, coreR, paint)
            }
        }
        // Lõi + bóng trong ở đáy
        canvas.withClip(corePath) {
            paint.color = shadedCore
            drawPath(corePath, paint)
            paint.color = coreColor
            tmp.set(core)
            tmp.offset(0f, -shade)
            drawRoundRect(tmp, coreR, coreR, paint)
            // Đốm sáng góc trên trái
            paint.color = highlightColor
            tmp.set(core.left + highlightX, core.top + highlightY, core.left + highlightX + highlightW, core.top + highlightY + highlightH)
            drawOval(tmp, paint)
        }
        // Viền lõi và viền ngoài (nét nằm trong mép hình)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.color = coreStrokeColor
        tmp.set(core)
        tmp.inset(stroke / 2f, stroke / 2f)
        canvas.drawRoundRect(tmp, coreR, coreR, paint)
        if (ringWidth > 0f) {
            paint.color = borderColor
            tmp.set(outer)
            tmp.inset(stroke / 2f, stroke / 2f)
            canvas.drawRoundRect(tmp, outerR, outerR, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        private const val DROP_COLOR = 0x26000000

        /** Nút cam (nút chính, nút icon, tab đang chọn). */
        fun orange(context: Context, ringWidth: Float = context.resources.getDimension(R.dimen.wood_button_ring)) =
            create(context, R.color.orange_button, R.color.button_inner_stroke, ringWidth)

        /** Nút / tab không chọn (Figma 2:1558): lõi xám. */
        fun gray(context: Context) = create(context, R.color.tab_off_inner, R.color.tab_off_shadow)

        /** Nút xanh của panel Move (Figma 2:969, 2:950...). */
        fun blue(context: Context) = create(context, R.color.button_blue, R.color.button_blue_stroke)

        /** Nút đỏ của panel Move (EXIT, Figma 2:966). */
        fun red(context: Context) = create(context, R.color.button_red, R.color.button_blue_stroke)

        /** Nền theo [WoodButton.Variant] (dùng chung cho WoodButton và RoundIconButton). */
        fun of(context: Context, variant: WoodButton.Variant) = when (variant) {
            WoodButton.Variant.ORANGE -> orange(context)
            WoodButton.Variant.GRAY -> gray(context)
            WoodButton.Variant.BLUE -> blue(context)
            WoodButton.Variant.RED -> red(context)
        }

        private fun create(
            context: Context,
            coreRes: Int,
            coreStrokeRes: Int,
            ringWidth: Float = context.resources.getDimension(R.dimen.wood_button_ring),
        ): WoodButtonDrawable {
            fun color(id: Int) = ContextCompat.getColor(context, id)
            return WoodButtonDrawable(
                context.resources,
                borderColor = color(R.color.brown_dark),
                ringColor = color(R.color.white),
                coreColor = color(coreRes),
                coreStrokeColor = color(coreStrokeRes),
                shadeColor = color(R.color.button_shade),
                highlightColor = color(R.color.button_highlight),
                ringWidth = ringWidth,
            )
        }
    }
}
