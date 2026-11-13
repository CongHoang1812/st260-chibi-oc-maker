package com.chibi.dream.cute.feature.success

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.chibi.dream.cute.core.common.NetworkMonitor
import com.chibi.dream.cute.data.model.CharacterConfig
import com.chibi.dream.cute.data.model.Creation
import com.chibi.dream.cute.data.model.CreationType
import com.chibi.dream.cute.data.repository.CreationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SuccessViewModelTest {

    private class FakeRepo(private val fail: Boolean = false) : CreationRepository {
        val downloaded = mutableListOf<List<Long>>()
        override fun observe(type: CreationType): Flow<List<Creation>> = emptyFlow()
        override suspend fun get(id: Long) = Creation(id, CreationType.DESIGN, "/files/creations/$id.png", null, 0)
        override suspend fun save(type: CreationType, bitmap: Bitmap, config: CharacterConfig?) = 1L
        override suspend fun delete(ids: List<Long>) = Unit
        override suspend fun download(ids: List<Long>): Result<Unit> {
            downloaded += ids
            return if (fail) Result.failure(IllegalStateException()) else Result.success(Unit)
        }
        val downloadedFiles = mutableListOf<String>()
        override suspend fun downloadFile(path: String): Result<Unit> {
            downloadedFiles += path
            return Result.success(Unit)
        }
        override suspend fun saveTemp(bitmap: Bitmap, fileName: String) = ""
        override suspend fun importImage(uri: Uri) = ""
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun handle(source: String = "MAKER", stars: Int = 0) = SavedStateHandle(
        mapOf(SuccessFragment.ARG_CREATION_ID to 7L, SuccessFragment.ARG_SOURCE to source, SuccessFragment.ARG_STARS to stars),
    )

    @Test
    fun readsArgsAndLoadsCreation() {
        val vm = SuccessViewModel(FakeRepo(), handle(source = "COSPLAY", stars = 9), onlineMonitor)
        assertEquals(SuccessFragment.Source.COSPLAY, vm.source)
        assertEquals(3, vm.stars)
        assertEquals("/files/creations/7.png", vm.imagePath.value)
    }

    @Test
    fun download_success_and_failure_emitEvents() = runTest {
        val ok = FakeRepo()
        val vm = SuccessViewModel(ok, handle(), onlineMonitor)
        vm.events.test {
            vm.download()
            assertEquals(SuccessEvent.Downloaded, awaitItem())
        }
        assertEquals(listOf(listOf(7L)), ok.downloaded)

        val failing = SuccessViewModel(FakeRepo(fail = true), handle(), onlineMonitor)
        failing.events.test {
            failing.download()
            assertEquals(SuccessEvent.DownloadFailed, awaitItem())
        }
    }

    @Test
    fun missingId_doesNotDownload() {
        val repo = FakeRepo()
        SuccessViewModel(repo, SavedStateHandle(), onlineMonitor).download()
        assertEquals(emptyList<List<Long>>(), repo.downloaded)
    }

    @Test
    fun cosplayImage_notFromMyCreation_downloadsFile() = runTest {
        val repo = FakeRepo()
        val vm = SuccessViewModel(
            repo,
            SavedStateHandle(mapOf(SuccessFragment.ARG_IMAGE_PATH to "/cache/cosplay_result.png", SuccessFragment.ARG_SOURCE to "COSPLAY")),
            onlineMonitor,
        )
        assertEquals("/cache/cosplay_result.png", vm.imagePath.value)
        vm.events.test {
            vm.download()
            assertEquals(SuccessEvent.Downloaded, awaitItem())
        }
        assertEquals(listOf("/cache/cosplay_result.png"), repo.downloadedFiles)
        assertEquals(emptyList<List<Long>>(), repo.downloaded)
    }
}

private val onlineMonitor = object : NetworkMonitor { override val isOnline = flowOf(true) }
