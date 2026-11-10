package com.conght.chibiocmaker.feature.maker.customize

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.conght.chibiocmaker.core.common.NetworkMonitor
import com.conght.chibiocmaker.core.common.PartLocks
import com.conght.chibiocmaker.core.common.Resource
import com.conght.chibiocmaker.data.model.CharacterConfig
import com.conght.chibiocmaker.data.model.CharacterTemplate
import com.conght.chibiocmaker.data.model.Creation
import com.conght.chibiocmaker.data.model.CreationType
import com.conght.chibiocmaker.data.model.PartCategory
import com.conght.chibiocmaker.data.model.PartColor
import com.conght.chibiocmaker.data.model.PartItem
import com.conght.chibiocmaker.data.model.PartSelection
import com.conght.chibiocmaker.data.model.TemplateDetail
import com.conght.chibiocmaker.data.repository.CreationRepository
import com.conght.chibiocmaker.data.repository.LuckyGiftRepository
import com.conght.chibiocmaker.data.repository.MakerRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LuckyGiftTest {

    private val red = PartColor("red", "#FF0000", "url/red")
    private val blue = PartColor("blue", "#0000FF", "url/blue")

    /** 6 item → mở h1, h2; khoá h3..h6. h1 chỉ có màu đỏ, h3 (khoá) có màu xanh. */
    private val hair = PartCategory(
        id = "hair", name = "Hair", iconUrl = "", zIndex = 1, isRequired = false,
        items = listOf(
            PartItem("h1", "t/h1", listOf(red)),
            PartItem("h2", "t/h2", listOf(red, blue)),
        ) + (3..6).map { PartItem("h$it", "t/h$it", listOf(blue)) },
    )

    /** 3 item → mở b1 (cũng là mặc định); khoá b2, b3. */
    private val body = PartCategory(
        id = "body", name = "Body", iconUrl = "", zIndex = 0, isRequired = true,
        items = (1..3).map { PartItem("b$it", "t/b$it", emptyList()) },
    )
    private val default = CharacterConfig(
        templateId = "t", selections = mapOf("hair" to PartSelection(null, null), "body" to PartSelection("b1", null)),
    )
    private val detail = TemplateDetail("t", listOf(hair, body), default)

    private var randomSelection: PartSelection? = null
    private var randomCharacter: CharacterConfig? = null

    private val makerRepository = object : MakerRepository {
        override fun templates(): Flow<Resource<List<CharacterTemplate>>> = emptyFlow()
        override fun templateDetail(id: String) = flowOf<Resource<TemplateDetail>>(Resource.Success(detail))
        override suspend fun randomizeCategory(templateId: String, categoryId: String) = randomSelection
        override suspend fun randomizeCharacter(templateId: String) = randomCharacter
    }

    private val creationRepository = object : CreationRepository {
        override fun observe(type: CreationType): Flow<List<Creation>> = emptyFlow()
        override suspend fun get(id: Long): Creation? = null
        override suspend fun save(type: CreationType, bitmap: Bitmap, config: CharacterConfig?) = 1L
        override suspend fun delete(ids: List<Long>) = Unit
        override suspend fun download(ids: List<Long>): Result<Unit> = Result.success(Unit)
        override suspend fun saveTemp(bitmap: Bitmap, fileName: String) = "/cache/$fileName"
        override suspend fun importImage(uri: Uri) = ""
    }

    private val online = object : NetworkMonitor { override val isOnline = flowOf(true) }

    /** Lưu như DataStore: part đã mở + mốc quay kế tiếp; đồng hồ là thời gian ảo của test. */
    private class FakeLuckyGiftRepository(private val clock: () -> Long) : LuckyGiftRepository {
        val unlocked = MutableStateFlow<Set<String>>(emptySet())
        override val nextSpinAtMs = MutableStateFlow(0L)
        override fun unlockedParts(templateId: String) = unlocked
        override fun nowMs() = clock()
        override suspend fun recordSpin(templateId: String, parts: Set<String>, nextSpinAtMs: Long) {
            unlocked.value = unlocked.value + parts
            this.nextSpinAtMs.value = nextSpinAtMs
        }
        override val adSpinUsedForMs = MutableStateFlow(0L)
        override suspend fun recordAdSpin(templateId: String, parts: Set<String>, cooldownEndMs: Long) {
            unlocked.value = unlocked.value + parts
            adSpinUsedForMs.value = cooldownEndMs
        }
    }

    private val gson = Gson()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.repository() = FakeLuckyGiftRepository { testScheduler.currentTime }

    private fun TestScope.viewModel(
        repository: LuckyGiftRepository,
        args: Map<String, Any?> = mapOf(CustomizeFragment.ARG_TEMPLATE_ID to "t"),
    ) = CustomizeViewModel(makerRepository, creationRepository, gson, SavedStateHandle(args), online, repository).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
    }

    @Test
    fun maker_locksTwoThirds_andLockedItemCannotBeSelected() = runTest {
        val vm = viewModel(repository())
        val state = vm.state.value
        assertEquals(setOf("hair/h3", "hair/h4", "hair/h5", "hair/h6", "body/b2", "body/b3"), state.lockedParts)
        assertTrue(state.luckyGift!!.canSpin)

        vm.selectItem("h4")
        assertNull(vm.state.value.selection?.itemId)
        vm.selectItem("h2")
        assertEquals("h2", vm.state.value.selection?.itemId)
    }

    @Test
    fun cosplay_hasNoLocks() = runTest {
        val vm = viewModel(
            repository(),
            mapOf(CustomizeFragment.ARG_TEMPLATE_ID to "t", CustomizeFragment.ARG_MODE to CustomizeMode.COSPLAY.name),
        )
        assertTrue(vm.state.value.isReady)
        assertTrue(vm.state.value.lockedParts.isEmpty())
        assertNull(vm.state.value.luckyGift)
        vm.selectItem("h4")
        assertEquals("h4", vm.state.value.selection?.itemId)
    }

    @Test
    fun colorOnNone_picksFirstOpenItemWithThatColor() = runTest {
        val vm = viewModel(repository())
        vm.selectColor("blue") // h2 (mở) có xanh; h3 có xanh nhưng đang khoá
        assertEquals(PartSelection("h2", "blue"), vm.state.value.selection)
    }

    @Test
    fun dice_neverLandsOnLockedPart() = runTest {
        val vm = viewModel(repository())
        randomSelection = PartSelection("h5", "blue")
        vm.randomCategory()
        val itemId = vm.state.value.selection?.itemId
        assertTrue(itemId in setOf("h1", "h2"))
    }

    @Test
    fun randomAll_replacesLockedParts() = runTest {
        val vm = viewModel(repository())
        randomCharacter = CharacterConfig(
            "t", mapOf("hair" to PartSelection("h6", "blue"), "body" to PartSelection("b3", null)),
        )
        vm.randomAll()
        val selections = vm.state.value.config!!.selections
        assertTrue(selections["hair"]?.itemId in setOf("h1", "h2"))
        assertEquals("b1", selections["body"]?.itemId)
    }

    @Test
    fun editingSavedCharacter_keepsWornLockedPart_butCannotReselectAfterChange() = runTest {
        val saved = CharacterConfig("t", mapOf("hair" to PartSelection("h5", "blue"), "body" to PartSelection("b1", null)))
        val vm = viewModel(
            repository(),
            mapOf(CustomizeFragment.ARG_CONFIG_JSON to gson.toJson(saved)),
        )
        assertEquals("h5", vm.state.value.selection?.itemId)
        vm.selectItem("h5") // bấm lại part đang mặc: vẫn giữ
        assertEquals("h5", vm.state.value.selection?.itemId)
        vm.selectItem("h1")
        vm.selectItem("h5") // đã đổi đi thì không chọn lại được
        assertEquals("h1", vm.state.value.selection?.itemId)
    }

    @Test
    fun spin_unlocksFiveParts_startsCooldown_thenCollect() = runTest {
        val repo = repository()
        val vm = viewModel(repo)
        vm.prepareLuckyGift()
        val grid = vm.state.value.luckyGift!!.grid
        assertEquals(6, grid.size) // tất cả 6 part khoá (lưới tối đa 15)

        vm.spinLuckyGift()
        runCurrent()
        val gift = vm.state.value.luckyGift!!
        val won = gift.result!!.map { it.key }.toSet()
        assertEquals(PartLocks.UNLOCK_PER_SPIN, won.size)
        assertEquals(won, repo.unlocked.value)
        assertEquals(1, vm.state.value.lockedParts.size)
        assertEquals(PartLocks.COOLDOWN_SECONDS, gift.remainingSeconds)
        assertFalse(gift.canSpin)

        vm.collectLuckyGift()
        assertNull(vm.state.value.luckyGift!!.result)
        assertTrue(vm.state.value.luckyGift!!.grid.isEmpty())

        advanceTimeBy(PartLocks.COOLDOWN_SECONDS * 1_000L - 1_500L)
        assertEquals(2, vm.state.value.luckyGift!!.remainingSeconds)
        advanceTimeBy(2_000L)
        assertEquals(0, vm.state.value.luckyGift!!.remainingSeconds)
        assertTrue(vm.state.value.luckyGift!!.canSpin)
    }

    @Test
    fun spinIsIgnoredDuringCooldown() = runTest {
        val repo = repository()
        repo.nextSpinAtMs.value = testScheduler.currentTime + 60_000L
        val vm = viewModel(repo)
        vm.prepareLuckyGift()
        vm.spinLuckyGift()
        runCurrent()
        assertNull(vm.state.value.luckyGift!!.result)
        assertTrue(repo.unlocked.value.isEmpty())
    }

    @Test
    fun spinWithAd_duringCooldown_unlocksParts_keepsTimer() = runTest {
        val repo = repository()
        repo.nextSpinAtMs.value = testScheduler.currentTime + 10 * 60_000L
        val vm = viewModel(repo)
        vm.prepareLuckyGift()
        assertTrue(vm.state.value.luckyGift!!.canSpinWithAd)

        vm.spinLuckyGift(withAd = true)
        runCurrent()
        val gift = vm.state.value.luckyGift!!
        assertEquals(PartLocks.UNLOCK_PER_SPIN, gift.result!!.size)
        assertEquals(PartLocks.UNLOCK_PER_SPIN, repo.unlocked.value.size)
        // Đồng hồ chạy tiếp từ mốc cũ, không đặt lại 30:00
        assertEquals(10 * 60, gift.remainingSeconds)
        assertTrue(gift.adSpinUsed)
        assertFalse(gift.canSpinWithAd) // đang hiện Congratulations

        // Mỗi lượt đếm ngược chỉ xem quảng cáo 1 lần
        vm.collectLuckyGift()
        vm.prepareLuckyGift()
        assertFalse(vm.state.value.luckyGift!!.canSpinWithAd)
        vm.spinLuckyGift(withAd = true)
        runCurrent()
        assertNull(vm.state.value.luckyGift!!.result)

        // Hết giờ: quay START được, lượt đếm ngược mới lại cho xem quảng cáo
        advanceTimeBy(10 * 60_000L + 1_000L)
        assertTrue(vm.state.value.luckyGift!!.canSpin)
        vm.prepareLuckyGift()
        vm.spinLuckyGift()
        runCurrent()
        vm.collectLuckyGift()
        vm.prepareLuckyGift()
        // Mốc đếm ngược mới khác mốc đã xem quảng cáo (dữ liệu test chỉ 6 part nên đã mở hết, không quay tiếp được)
        val next = vm.state.value.luckyGift!!
        assertTrue(next.remainingSeconds > 0)
        assertFalse(next.adSpinUsed)
    }

    @Test
    fun spinWithAd_isIgnoredWhenFreeSpinAvailable() = runTest {
        val repo = repository()
        val vm = viewModel(repo)
        vm.prepareLuckyGift()
        assertFalse(vm.state.value.luckyGift!!.canSpinWithAd)
        vm.spinLuckyGift(withAd = true)
        runCurrent()
        assertNull(vm.state.value.luckyGift!!.result)
    }

    @Test
    fun cooldownContinuesFromSavedTime_afterReopeningApp() = runTest {
        val repo = repository()
        // Lần trước quay xong rồi thoát app; mở lại khi còn 10 phút
        repo.nextSpinAtMs.value = testScheduler.currentTime + 10 * 60_000L
        val vm = viewModel(repo)
        assertEquals(10 * 60, vm.state.value.luckyGift!!.remainingSeconds)
        assertFalse(vm.state.value.luckyGift!!.canSpin)
    }

    @Test
    fun allPartsUnlocked_hidesGift() = runTest {
        val repo = repository()
        repo.unlocked.value = setOf("hair/h3", "hair/h4", "hair/h5", "hair/h6", "body/b2", "body/b3")
        val vm = viewModel(repo)
        val gift = vm.state.value.luckyGift
        assertNotNull(gift)
        assertFalse(gift!!.hasLocked)
        assertFalse(gift.canSpin)
    }
}
