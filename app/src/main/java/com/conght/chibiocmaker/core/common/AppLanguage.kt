package com.conght.chibiocmaker.core.common

import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import com.conght.chibiocmaker.R

/** Ngôn ngữ app hỗ trợ, theo thứ tự hiển thị trong Figma. */
enum class AppLanguage(val code: String, @param:StringRes val nameRes: Int, @param:DrawableRes val flagRes: Int) {
    HINDI("hi", R.string.language_hindi, R.drawable.ic_flag_hi),
    SPANISH("es", R.string.language_spanish, R.drawable.ic_flag_es),
    FRENCH("fr", R.string.language_french, R.drawable.ic_flag_fr),
    ENGLISH("en", R.string.language_english, R.drawable.ic_flag_en),
    PORTUGUESE("pt", R.string.language_portuguese, R.drawable.ic_flag_pt),
    INDONESIAN("in", R.string.language_indonesian, R.drawable.ic_flag_in);

    companion object {
        val DEFAULT = ENGLISH

        /** "id" (mã mới của tiếng Indonesia) -> "in" (mã resource values-in). */
        fun fromCode(code: String?): AppLanguage? {
            val normalized = code?.substringBefore('-')?.substringBefore('_')?.lowercase()
                ?.let { if (it == "id") "in" else it }
            return entries.firstOrNull { it.code == normalized }
        }

        /**
         * Ngôn ngữ app đang dùng (nếu đã chọn), ngược lại ngôn ngữ đầu tiên của máy nếu hỗ trợ, cuối cùng English.
         * Ngôn ngữ máy đọc từ `Resources.getSystem()`: `Locale.getDefault()` / `getAdjustedDefault()` đã bị Android
         * đổi sang ngôn ngữ app có (máy vi-VN, fr-FR → fr), không còn là ngôn ngữ đầu của máy.
         */
        fun current(): AppLanguage {
            val appLocales = AppCompatDelegate.getApplicationLocales()
            val locale = if (!appLocales.isEmpty) {
                appLocales[0]
            } else {
                ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]
            }
            return fromCode(locale?.language) ?: DEFAULT
        }

        /**
         * Chưa chọn ngôn ngữ: khoá app theo [current] (ngôn ngữ đầu của máy nếu hỗ trợ, không thì English).
         * Không khoá thì Android tự lấy ngôn ngữ kế tiếp app có trong danh sách của máy (vd. máy vi-VN, fr-FR
         * → chữ tiếng Pháp) trong khi [current] báo English. Gọi sau `super.onCreate` của Activity.
         */
        fun applyDefaultIfUnset() {
            if (AppCompatDelegate.getApplicationLocales().isEmpty) apply(current())
        }

        /** Áp ngôn ngữ cho app. MainActivity tự xử lý đổi locale (không recreate), xem onConfigurationChanged. */
        fun apply(language: AppLanguage) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.code))
        }
    }
}
