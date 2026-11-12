package com.conght.chibiocmaker.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.settingsDataStore

    val languageCode: Flow<String?> = dataStore.data.map { it[KEY_LANGUAGE] }
    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val appRated: Flow<Boolean> = dataStore.data.map { it[KEY_APP_RATED] ?: false }

    suspend fun setLanguage(code: String) {
        dataStore.edit { it[KEY_LANGUAGE] = code }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setAppRated() {
        dataStore.edit { it[KEY_APP_RATED] = true }
    }

    /** Số template lần tải gần nhất (số thẻ chờ ở màn chọn template); null = chưa tải lần nào. */
    val templateCount: Flow<Int?> = dataStore.data.map { it[KEY_TEMPLATE_COUNT] }

    suspend fun setTemplateCount(count: Int) {
        dataStore.edit { it[KEY_TEMPLATE_COUNT] = count }
    }

    /** Tăng số lần bấm Back ở Home, trả về giá trị mới. */
    suspend fun incrementHomeBackCount(): Int =
        dataStore.edit { it[KEY_HOME_BACK_COUNT] = (it[KEY_HOME_BACK_COUNT] ?: 0) + 1 }[KEY_HOME_BACK_COUNT] ?: 0

    fun luckyGiftUnlocked(templateId: String): Flow<Set<String>> =
        dataStore.data.map { it[keyLuckyGiftUnlocked(templateId)].orEmpty() }

    val luckyGiftNextSpinAt: Flow<Long> = dataStore.data.map { it[KEY_LUCKY_GIFT_NEXT_SPIN] ?: 0L }

    suspend fun recordLuckyGiftSpin(templateId: String, parts: Set<String>, nextSpinAtMs: Long) {
        dataStore.edit {
            val key = keyLuckyGiftUnlocked(templateId)
            it[key] = it[key].orEmpty() + parts
            it[KEY_LUCKY_GIFT_NEXT_SPIN] = nextSpinAtMs
        }
    }

    /** Mốc [luckyGiftNextSpinAt] của lượt đếm ngược đã dùng quay bằng quảng cáo; 0 = chưa dùng. */
    val luckyGiftAdSpinUsedFor: Flow<Long> = dataStore.data.map { it[KEY_LUCKY_GIFT_AD_USED_FOR] ?: 0L }

    /** Lượt quay bằng quảng cáo: lưu part trúng, giữ nguyên mốc đếm ngược, đánh dấu lượt đếm ngược này đã dùng. */
    suspend fun recordLuckyGiftAdSpin(templateId: String, parts: Set<String>, cooldownEndMs: Long) {
        dataStore.edit {
            val key = keyLuckyGiftUnlocked(templateId)
            it[key] = it[key].orEmpty() + parts
            it[KEY_LUCKY_GIFT_AD_USED_FOR] = cooldownEndMs
        }
    }

    /** Mở khoá part ngoài vòng quay (xem quảng cáo ở ô part khoá). */
    suspend fun unlockLuckyGiftParts(templateId: String, parts: Set<String>) {
        dataStore.edit {
            val key = keyLuckyGiftUnlocked(templateId)
            it[key] = it[key].orEmpty() + parts
        }
    }

    private fun keyLuckyGiftUnlocked(templateId: String) = stringSetPreferencesKey("lucky_gift_unlocked_$templateId")

    private companion object {
        val KEY_LUCKY_GIFT_NEXT_SPIN = longPreferencesKey("lucky_gift_next_spin_at")
        val KEY_LUCKY_GIFT_AD_USED_FOR = longPreferencesKey("lucky_gift_ad_used_for")
        val KEY_LANGUAGE = stringPreferencesKey("language_code")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_APP_RATED = booleanPreferencesKey("app_rated")
        val KEY_HOME_BACK_COUNT = intPreferencesKey("home_back_count")
        val KEY_TEMPLATE_COUNT = intPreferencesKey("template_count")
    }
}
