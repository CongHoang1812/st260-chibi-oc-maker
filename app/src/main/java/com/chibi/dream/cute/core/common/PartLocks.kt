package com.chibi.dream.cute.core.common

import com.chibi.dream.cute.data.model.PartCategory
import com.chibi.dream.cute.data.model.PartSelection
import com.chibi.dream.cute.data.model.TemplateDetail
import kotlin.random.Random

/**
 * Quy tắc khoá part của Lucky Gift: mỗi tab mở 1/3 đầu danh sách (làm tròn lên), khoá phần còn lại.
 * Part mặc định của template luôn mở để Reset và tab bắt buộc không bao giờ rơi vào part khoá.
 */
object PartLocks {
    /** Số part mở khoá mỗi lượt quay (khung Congratulations có 5 ô). */
    const val UNLOCK_PER_SPIN = 5

    /** Số ô trong lưới vòng quay. */
    const val GRID_SIZE = 15

    const val COOLDOWN_SECONDS = 1 * 60

    private const val FREE_DIVISOR = 3

    /** Khoá định danh một part: cùng id item có thể lặp lại giữa các tab. */
    fun key(categoryId: String, itemId: String): String = "$categoryId/$itemId"

    /** Key các part đang khoá của [detail], đã trừ [unlocked]. */
    fun lockedKeys(detail: TemplateDetail, unlocked: Set<String>): Set<String> =
        detail.categories.flatMap { category ->
            val defaultItemId = detail.defaultConfig.selections[category.id]?.itemId
            val freeCount = (category.items.size + FREE_DIVISOR - 1) / FREE_DIVISOR
            category.items.drop(freeCount)
                .filter { it.id != defaultItemId }
                .map { key(category.id, it.id) }
        }.toSet() - unlocked

    /** Giữ nguyên [selection] nếu part đã mở; đang khoá thì đổi sang một part đã mở bất kỳ trong tab. */
    fun openSelection(
        category: PartCategory,
        selection: PartSelection,
        locked: Set<String>,
        random: Random = Random.Default,
    ): PartSelection {
        val itemId = selection.itemId ?: return selection
        if (key(category.id, itemId) !in locked) return selection
        val item = category.items.filter { key(category.id, it.id) !in locked }.randomOrNull(random)
            ?: return PartSelection(null, null)
        return PartSelection(item.id, item.colors.randomOrNull(random)?.id)
    }
}
