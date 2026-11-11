package com.conght.chibiocmaker.feature.decor.editor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import com.conght.chibiocmaker.R
import com.conght.chibiocmaker.core.ui.loadImage
import java.io.File

/**
 * Nội dung một phần tử trên canvas: nhân vật, sticker (ảnh), speech (ảnh bong bóng + chữ) hoặc text.
 * Vị trí / scale / xoay / lật do [DecorCanvasView] áp qua translation / scale / rotation.
 */
@SuppressLint("ViewConstructor") // chỉ tạo bằng code từ DecorCanvasView
class DecorElementView(context: Context, val type: ElementType) : FrameLayout(context) {

    private val image: ImageView? = if (type == ElementType.TEXT) {
        null
    } else {
        ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
            .also { addView(it, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)) }
    }

    private val text: TextView? = if (type == ElementType.STICKER || type == ElementType.CHARACTER) {
        null
    } else {
        TextView(context).apply {
            gravity = Gravity.CENTER
            includeFontPadding = false
            val size = if (type == ElementType.SPEECH) R.dimen.decor_speech_text else R.dimen.decor_text_size
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(size))
            // Bong bóng: viền mờ nâu để chữ trắng vẫn đọc được trên nền trắng. Text: không bóng
            if (type == ElementType.SPEECH) {
                setShadowLayer(
                    resources.getDimension(R.dimen.decor_text_shadow), 0f, 0f,
                    ContextCompat.getColor(context, R.color.brown_dark),
                )
            }
        }.also {
            val params = if (type == ElementType.SPEECH) {
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                    val pad = resources.getDimensionPixelSize(R.dimen.decor_speech_text_padding)
                    setMargins(pad, pad, pad, pad)
                }
            } else {
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            }
            addView(it, params)
        }
    }

    private var bound: DecorElement? = null

    /** Vùng có điểm ảnh (alpha > 0) của ảnh nhân vật, theo pixel ảnh gốc; null = cả ảnh. */
    private var opaqueBounds: Rect? = null
    private val tmpRect = RectF()

    fun bind(element: DecorElement) {
        val old = bound
        bound = element
        if (image != null && old?.imageUrl != element.imageUrl) {
            if (type == ElementType.CHARACTER) {
                loadCharacter(image, element.imageUrl)
            } else {
                image.loadImage(element.imageUrl, R.color.transparent, forExport = true)
            }
        }
        if (text != null) {
            if (old?.text != element.text) text.text = element.text
            if (old?.fontId != element.fontId || old == null) {
                text.typeface = ResourcesCompat.getFont(context, DecorFonts.of(element.fontId).fontRes)
            }
            element.color?.let(text::setTextColor)
        }
    }

    /**
     * Ảnh nhân vật là PNG cỡ cả khung, phần lớn trong suốt: tìm vùng có hình để khung chọn
     * ôm sát nhân vật thay vì cả canvas.
     */
    private fun loadCharacter(target: ImageView, path: String?) {
        opaqueBounds = null
        val file = path?.let(::File)
        if (file == null) {
            Glide.with(target).clear(target)
            target.setImageDrawable(null)
            return
        }
        Glide.with(target).asBitmap().load(file)
            .signature(ObjectKey("${file.lastModified()}_${file.length()}"))
            .disallowHardwareConfig()
            .into(object : CustomViewTarget<ImageView, Bitmap>(target) {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    opaqueBounds = resource.opaqueBounds()
                    view.setImageBitmap(resource)
                    (parent?.parent as? View)?.invalidate()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) = Unit
                override fun onResourceCleared(placeholder: Drawable?) = view.setImageDrawable(null)
            })
    }

    /**
     * Vùng nội dung theo toạ độ local (gốc = tâm view, chưa scale / lật / xoay) ghi vào [out].
     * Nhân vật: phần có hình sau FIT_CENTER; loại khác: cả view.
     */
    fun contentBounds(out: RectF) {
        val hw = width / 2f
        val hh = height / 2f
        out.set(-hw, -hh, hw, hh)
        val bounds = opaqueBounds ?: return
        val iv = image ?: return
        if (iv.drawable == null) return
        tmpRect.set(bounds)
        iv.imageMatrix.mapRect(tmpRect)
        tmpRect.offset(iv.left + iv.paddingLeft - hw, iv.top + iv.paddingTop - hh)
        out.set(tmpRect)
    }

    companion object {
        /** Bước dò alpha (px): đủ chính xác cho khung chọn, nhanh với ảnh 1024px. */
        private const val ALPHA_SCAN_STEP = 4

        private fun Bitmap.opaqueBounds(): Rect? {
            var left = width
            var top = height
            var right = -1
            var bottom = -1
            val row = IntArray(width)
            for (y in 0 until height step ALPHA_SCAN_STEP) {
                getPixels(row, 0, width, 0, y, width, 1)
                for (x in 0 until width step ALPHA_SCAN_STEP) {
                    if (row[x] ushr 24 != 0) {
                        if (x < left) left = x
                        if (x > right) right = x
                        if (y < top) top = y
                        if (y > bottom) bottom = y
                    }
                }
            }
            if (right < 0) return null
            return Rect(
                (left - ALPHA_SCAN_STEP).coerceAtLeast(0),
                (top - ALPHA_SCAN_STEP).coerceAtLeast(0),
                (right + ALPHA_SCAN_STEP).coerceAtMost(width),
                (bottom + ALPHA_SCAN_STEP).coerceAtMost(height),
            )
        }

        /** Kích thước gốc (chưa scale) theo loại; TEXT = wrap_content, nhân vật = cả canvas. */
        fun layoutParamsFor(context: Context, type: ElementType): LayoutParams {
            val res = context.resources
            return when (type) {
                ElementType.CHARACTER -> LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

                ElementType.STICKER -> LayoutParams(
                    res.getDimensionPixelSize(R.dimen.decor_sticker_w),
                    res.getDimensionPixelSize(R.dimen.decor_sticker_h),
                    Gravity.CENTER,
                )

                ElementType.SPEECH -> LayoutParams(
                    res.getDimensionPixelSize(R.dimen.decor_speech_w),
                    res.getDimensionPixelSize(R.dimen.decor_speech_h),
                    Gravity.CENTER,
                )

                ElementType.TEXT -> LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            }
        }
    }
}
