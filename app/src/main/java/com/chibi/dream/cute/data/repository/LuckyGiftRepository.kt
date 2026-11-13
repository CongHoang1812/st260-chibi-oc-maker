package com.chibi.dream.cute.data.repository

import com.chibi.dream.cute.data.local.datastore.SettingsDataStore
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

    /** Mốc [nextSpinAtMs] của lượt đếm ngược đã quay bằng quảng cáo (mỗi lượt đếm ngược chỉ 1 lần); 0 = chưa. */
    val adSpinUsedForMs: Flow<Long>

    /** Quay bằng quảng cáo lúc đang đếm ngược: lưu part trúng, đồng hồ giữ nguyên mốc [cooldownEndMs]. */
    suspend fun recordAdSpin(templateId: String, parts: Set<String>, cooldownEndMs: Long)

    /**
     * Mở khoá part bằng quảng cáo (bấm part khoá ở Customize): không đụng tới đồng hồ quay.
     * Chưa gọi ở đâu: chờ gắn quảng cáo (xem TODO(ADS) ở CustomizeViewModel.onUnlockPartResult).
     */
    suspend fun unlockParts(templateId: String, parts: Set<String>)

    /**
     * Lần vào Customize đầu tiên của phiên app (thoát hẳn rồi mở lại = phiên mới): trả true đúng một lần,
     * các lần sau trong phiên trả false. Mặc định false = không tự mở Lucky Gift.
     */
    fun consumeSessionAutoShow(): Boolean = false

    /** Không khoá gì: mặc định của ViewModel khi không được Hilt tiêm repository thật. */
    object Disabled : LuckyGiftRepository {
        override val locksEnabled = false
        override fun unlockedParts(templateId: String) = flowOf(emptySet<String>())
        override val nextSpinAtMs = flowOf(0L)
        override suspend fun recordSpin(templateId: String, parts: Set<String>, nextSpinAtMs: Long) = Unit
        override val adSpinUsedForMs = flowOf(0L)
        override suspend fun recordAdSpin(templateId: String, parts: Set<String>, cooldownEndMs: Long) = Unit
        override suspend fun unlockParts(templateId: String, parts: Set<String>) = Unit
    }
}

class LuckyGiftRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
) : LuckyGiftRepository {
    override fun unlockedParts(templateId: String) = dataStore.luckyGiftUnlocked(templateId)
    override val nextSpinAtMs = dataStore.luckyGiftNextSpinAt
    override suspend fun recordSpin(templateId: String, parts: Set<String>, nextSpinAtMs: Long) =
        dataStore.recordLuckyGiftSpin(templateId, parts, nextSpinAtMs)
    override val adSpinUsedForMs = dataStore.luckyGiftAdSpinUsedFor
    override suspend fun recordAdSpin(templateId: String, parts: Set<String>, cooldownEndMs: Long) =
        dataStore.recordLuckyGiftAdSpin(templateId, parts, cooldownEndMs)
    override suspend fun unlockParts(templateId: String, parts: Set<String>) =
        dataStore.unlockLuckyGiftParts(templateId, parts)

    /** Chỉ giữ trong bộ nhớ: repository là @Singleton nên sống đúng bằng tiến trình app. */
    private var sessionAutoShowPending = true

    override fun consumeSessionAutoShow(): Boolean = sessionAutoShowPending.also { sessionAutoShowPending = false }
}
