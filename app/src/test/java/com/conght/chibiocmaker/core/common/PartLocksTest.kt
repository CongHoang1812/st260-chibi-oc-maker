package com.chibi.dream.cute.core.common

import com.chibi.dream.cute.data.model.CharacterConfig
import com.chibi.dream.cute.data.model.PartCategory
import com.chibi.dream.cute.data.model.PartColor
import com.chibi.dream.cute.data.model.PartItem
import com.chibi.dream.cute.data.model.PartSelection
import com.chibi.dream.cute.data.model.TemplateDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PartLocksTest {

    private fun items(prefix: String, count: Int) =
        (1..count).map { PartItem("$prefix$it", "", listOf(PartColor("c", "#000000", ""))) }

    private val hair = PartCategory("hair", "Hair", "", 1, false, items("h", 6))
    private val body = PartCategory("body", "Body", "", 0, true, items("b", 4))
    private val single = PartCategory("hat", "Hat", "", 2, false, items("t", 1))
    private val detail = TemplateDetail(
        "t",
        listOf(hair, body, single),
        CharacterConfig("t", mapOf("hair" to PartSelection(null, null), "body" to PartSelection("b4", null))),
    )

    @Test
    fun locksTwoThirds_keepsFirstThirdAndDefaultOpen() {
        val locked = PartLocks.lockedKeys(detail, emptySet())
        // hair 6 → mở 2 (h1, h2); body 4 → mở ceil(4/3) = 2, b4 là mặc định nên cũng mở; hat 1 → mở hết
        assertEquals(setOf("hair/h3", "hair/h4", "hair/h5", "hair/h6", "body/b3"), locked)
    }

    @Test
    fun unlockedPartsAreRemoved() {
        val locked = PartLocks.lockedKeys(detail, setOf("hair/h3", "body/b3"))
        assertEquals(setOf("hair/h4", "hair/h5", "hair/h6"), locked)
    }

    @Test
    fun openSelection_keepsOpenAndNone_replacesLocked() {
        val locked = PartLocks.lockedKeys(detail, emptySet())
        assertEquals(PartSelection("h1", "c"), PartLocks.openSelection(hair, PartSelection("h1", "c"), locked))
        assertEquals(PartSelection(null, null), PartLocks.openSelection(hair, PartSelection(null, null), locked))
        repeat(20) {
            val replaced = PartLocks.openSelection(hair, PartSelection("h5", "c"), locked)
            assertTrue(replaced.itemId in setOf("h1", "h2"))
            assertEquals("c", replaced.colorId)
        }
    }
}
