package com.chibi.dream.cute.data.repository

import com.chibi.dream.cute.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface SettingsRepository {
    val languageCode: Flow<String?>
    suspend fun setLanguage(code: String)
    val onboardingDone: Flow<Boolean>
    suspend fun setOnboardingDone(done: Boolean)

    /** Người dùng đã đánh giá app chưa. Đã đánh giá thì Setting ẩn dòng Rate us. */
    val appRated: Flow<Boolean>
    suspend fun setAppRated()

    /** Tăng số lần bấm Back ở Home (lưu qua các lần mở app), trả về giá trị mới. */
    suspend fun incrementHomeBackCount(): Int

    /** Số template lần tải gần nhất, để màn chọn template hiện đúng số thẻ chờ; null = chưa tải lần nào. */
    val templateCount: Flow<Int?>
    suspend fun setTemplateCount(count: Int)
}

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
) : SettingsRepository {
    override val languageCode = dataStore.languageCode
    override suspend fun setLanguage(code: String) = dataStore.setLanguage(code)
    override val onboardingDone = dataStore.onboardingDone
    override suspend fun setOnboardingDone(done: Boolean) = dataStore.setOnboardingDone(done)
    override val appRated = dataStore.appRated
    override suspend fun setAppRated() = dataStore.setAppRated()
    override suspend fun incrementHomeBackCount() = dataStore.incrementHomeBackCount()
    override val templateCount = dataStore.templateCount
    override suspend fun setTemplateCount(count: Int) = dataStore.setTemplateCount(count)
}
