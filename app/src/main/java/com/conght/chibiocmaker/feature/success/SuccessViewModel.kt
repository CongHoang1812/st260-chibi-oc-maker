package com.conght.chibiocmaker.feature.success

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conght.chibiocmaker.core.common.NetworkMonitor
import com.conght.chibiocmaker.data.repository.CreationRepository
import com.conght.chibiocmaker.feature.maker.customize.CustomizeFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SuccessEvent {
    data object Downloaded : SuccessEvent
    data object DownloadFailed : SuccessEvent
}

@HiltViewModel
class SuccessViewModel @Inject constructor(
    private val creationRepository: CreationRepository,
    savedStateHandle: SavedStateHandle,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val creationId: Long = savedStateHandle[SuccessFragment.ARG_CREATION_ID] ?: -1L

    val source: SuccessFragment.Source = savedStateHandle.get<String>(SuccessFragment.ARG_SOURCE)
        ?.let { runCatching { SuccessFragment.Source.valueOf(it) }.getOrNull() } ?: SuccessFragment.Source.MAKER

    /** COSPLAY: đề bài vừa chơi, để Re-edit mở lại màn chơi. */
    val cosplayTemplateId: String? = savedStateHandle[CustomizeFragment.ARG_TEMPLATE_ID]
    val cosplayChallengeJson: String? = savedStateHandle[CustomizeFragment.ARG_CHALLENGE_JSON]

    val stars: Int = (savedStateHandle.get<Int>(SuccessFragment.ARG_STARS) ?: 0).coerceIn(0, MAX_STARS)

    /** COSPLAY: ảnh kết quả (cacheDir), không nằm trong My creation. */
    private val imageFile: String? = savedStateHandle[SuccessFragment.ARG_IMAGE_PATH]

    private val _imagePath = MutableStateFlow(imageFile)

    /** Ảnh hiện ở màn: [imageFile] hoặc ảnh của creation [creationId]. */
    val imagePath: StateFlow<String?> = _imagePath.asStateFlow()

    private val _downloading = MutableStateFlow(false)
    val downloading: StateFlow<Boolean> = _downloading.asStateFlow()

    private val _events = Channel<SuccessEvent>(Channel.BUFFERED)
    val events: Flow<SuccessEvent> = _events.receiveAsFlow()

    init {
        if (imageFile == null) viewModelScope.launch { _imagePath.value = creationRepository.get(creationId)?.imagePath }
    }

    /** Re-edit mở lại editor (tải part / nền từ API) nên cần mạng. */
    suspend fun isOnline(): Boolean = networkMonitor.isOnline.first()

    /** Lưu ảnh ra thư viện máy (Gallery). */
    fun download() {
        if (_downloading.value || (imageFile == null && creationId < 0)) return
        _downloading.value = true
        viewModelScope.launch {
            val result = if (imageFile != null) {
                creationRepository.downloadFile(imageFile)
            } else {
                creationRepository.download(listOf(creationId))
            }
            _downloading.value = false
            _events.send(if (result.isSuccess) SuccessEvent.Downloaded else SuccessEvent.DownloadFailed)
        }
    }

    private companion object {
        const val MAX_STARS = 3
    }
}
