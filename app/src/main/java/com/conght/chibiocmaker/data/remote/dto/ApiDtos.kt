package com.conght.chibiocmaker.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Một bộ phận của template (endpoint `api/app/{APP_KEY}`, response = Map<templateKey, List<PartDto>>).
 * Field nullable vì Gson không đảm bảo non-null với Kotlin.
 */
data class PartDto(
    /** Tên thư mục template trên server. */
    @SerializedName("position") val position: String?,
    /** `X-Y`: X = zIndex (lớp vẽ), Y = thứ tự tab (tài liệu API ghi ngược, đã kiểm tra bằng ảnh thật). */
    @SerializedName("parts") val parts: String?,
    /** Mã màu cách nhau bởi dấu phẩy; rỗng = không có biến thể màu. */
    @SerializedName("colorArray") val colorArray: String?,
    @SerializedName("quantity") val quantity: Int?,
    /** Thứ tự sắp xếp template. */
    @SerializedName("level") val level: Int?,
)

/**
 * Cấu hình nền / sticker / speech bubble (`bg/bg.json`).
 * Server có 2 kiểu tên khoá, tên khoá cũng là tên thư mục ảnh: `stickers` / `speech_bubble` (app mẫu)
 * hoặc `sticker` / `speech bubble` (vd. ST279). Mỗi kiểu một field để mapper biết thư mục nào cần ghép.
 */
data class DecorConfigDto(
    @SerializedName("background") val background: List<DecorCategoryDto>?,
    @SerializedName("stickers") val sticker: List<DecorCategoryDto>?,
    @SerializedName("speech_bubble") val speechBubble: List<DecorCategoryDto>?,
    @SerializedName("sticker") val stickerSingular: List<DecorCategoryDto>? = null,
    @SerializedName("speech bubble") val speechBubbleSpaced: List<DecorCategoryDto>? = null,
)

/** [category] = tên thư mục con (có thể rỗng), [quantity] = số ảnh đánh số từ 1. */
data class DecorCategoryDto(
    @SerializedName("category") val category: String?,
    @SerializedName("quantity") val quantity: Int?,
)
