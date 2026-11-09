package com.conght.chibiocmaker.data.repository

import com.conght.chibiocmaker.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Part đã mở khoá (theo từng template) và thời điểm được quay lượt kế tiếp (chung toàn app). */
interface LuckyGiftRepository {
    /** false = không khoá part nào. */
    val locksEnabled: Boolean get() = true

    fun unlockedParts(templateId: String): Flow<Set<String>>

    /** Epoch ms được quay lượt kế tiếp; 0 = chưa quay lần nào. Lưu mốc giờ nên vẫn chạy khi đã thoát app. */
    val nextSpinAtMs: Flow<Long>

    fun nowMs(): Long = System.currentTimeMillis()

    /** Lưu cùng lúc part vừa trúng và mốc đếm ngược: thoát app giữa chừng cũng không quay lại được. */
    suspend fun recordSpin(templateId: String, parts: Set<String>, nextSpinAtMs: Long)

    /** Không khoá gì: mặc định của ViewModel khi không được Hilt tiêm repository thật. */
    object Disabled : LuckyGiftRepository {
        override val locksEnabled = false
        override fun unlockedParts(templateId: String) = flowOf(emptySet<String>())
        override val nextSpinAtMs = flowOf(0L)
        override suspend fun recordSpin(templateId: String, parts: Set<String>, nextSpinAtMs: Long) = Unit
    }
}

class LuckyGiftRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
) : LuckyGiftRepository {
    override fun unlockedParts(templateId: String) = dataStore.luckyGiftUnlocked(templateId)
    override val nextSpinAtMs = dataStore.luckyGiftNextSpinAt
    override suspend fun recordSpin(templateId: String, parts: Set<String>, nextSpinAtMs: Long) =
        dataStore.recordLuckyGiftSpin(templateId, parts, nextSpinAtMs)
}
