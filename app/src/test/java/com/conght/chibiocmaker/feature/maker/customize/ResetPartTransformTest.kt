package com.conght.chibiocmaker.feature.maker.customize

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.conght.chibiocmaker.core.common.NetworkMonitor
import com.conght.chibiocmaker.core.common.Resource
import com.conght.chibiocmaker.data.model.CharacterConfig
import com.conght.chibiocmaker.data.model.CharacterTemplate
import com.conght.chibiocmaker.data.model.CharacterTransform
import com.conght.chibiocmaker.data.model.Creation
import com.conght.chibiocmaker.data.model.CreationType
import com.conght.chibiocmaker.data.model.PartCategory
import com.conght.chibiocmaker.data.model.PartItem
import com.conght.chibiocmaker.data.model.PartSelection
import com.conght.chibiocmaker.data.model.TemplateDetail
import com.conght.chibiocmaker.data.repository.CreationRepository
import com.conght.chibiocmaker.data.repository.MakerRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** Nút RESET của panel Move (Figma 2:969): chỉ bộ phận đang chọn về vị trí / góc / cỡ mặc định. */
@OptIn(ExperimentalCoroutinesApi::class)
class ResetPartTransformTest {

    private val hair = PartCategory("hair", "Hair", "", 1, false, listOf(PartItem("h1", "t/h1", emptyList())))
    private val hat = PartCategory("hat", "Hat", "", 2, false, listOf(PartItem("t1", "t/t1", emptyList())))
    private val default = CharacterConfig(
        "t", mapOf("hair" to PartSelection("h1", null), "hat" to PartSelection("t1", null)),
    )
    private val detail = TemplateDetail("t", listOf(hair, hat), default)

    private val makerRepository = object : MakerRepository {
        override fun templates(): Flow<Resource<List<CharacterTemplate>>> = emptyFlow()
        override fun templateDetail(id: String) = flowOf<Resource<TemplateDetail>>(Resource.Success(detail))
        override suspend fun randomizeCategory(templateId: String, categoryId: String): PartSelection? = null
        override suspend fun randomizeCharacter(templateId: String): CharacterConfig? = null
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

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = CustomizeViewModel(
        makerRepository, creationRepository, Gson(), SavedStateHandle(mapOf("templateId" to "t")), online,
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} } }

    @Test
    fun reset_clearsOnlySelectedPart() = runTest {
        val vm = viewModel()
        vm.selectCategory("hat")
        vm.move(20f, 10f)
        vm.selectCategory("hair")
        vm.move(30f, -10f)
        vm.rotate(15f)
        vm.zoom(0.3f)
        assertNotEquals(CharacterTransform(), vm.state.value.config!!.partTransform("hair"))

        vm.resetPartTransform()

        val config = vm.state.value.config!!
        assertEquals(CharacterTransform(), config.partTransform("hair"))
        assertEquals(CharacterTransform(offsetX = 20f, offsetY = 10f), config.partTransform("hat"))
    }

    @Test
    fun reset_lastPart_dropsTransformMap_andKeepsFlip() = runTest {
        val vm = viewModel()
        vm.flip()
        vm.move(10f, 0f)
        vm.resetPartTransform()
        val config = vm.state.value.config!!
        assertNull(config.partTransforms)
        assertEquals(true, config.transform.flipped)
    }

    @Test
    fun reset_withoutPartTransforms_changesNothing() = runTest {
        val vm = viewModel()
        vm.resetPartTransform()
        assertEquals(default, vm.state.value.config)
        assertFalse(vm.state.value.isDirty)
    }
}
