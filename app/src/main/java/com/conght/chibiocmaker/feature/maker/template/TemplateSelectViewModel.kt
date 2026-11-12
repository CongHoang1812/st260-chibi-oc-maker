package com.conght.chibiocmaker.feature.maker.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conght.chibiocmaker.core.common.NetworkMonitor
import com.conght.chibiocmaker.core.common.retryWhenBackOnline
import com.conght.chibiocmaker.core.common.Resource
import com.conght.chibiocmaker.data.model.CharacterTemplate
import com.conght.chibiocmaker.data.repository.MakerRepository
import com.conght.chibiocmaker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TemplateSelectViewModel @Inject constructor(
    private val makerRepository: MakerRepository,
    private val networkMonitor: NetworkMonitor,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Template vừa bấm lúc mất mạng: Try again có mạng thì mở tiếp (sống qua xoay màn). */
    var pendingTemplate: CharacterTemplate? = null

    /** Mỗi lần tăng = tải lại danh sách (Retry). */
    private val reloadKey = MutableStateFlow(0)

    /**
     * Tải một lần, giữ tới khi rời màn (chỉ Retry mới tải lại). Không dùng WhileSubscribed: vào Customize
     * lâu rồi Back thì flow chạy lại, phát Loading trước → lưới hiện thẻ chờ rồi mới ra template (nháy).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<Resource<List<CharacterTemplate>>> = reloadKey
        .flatMapLatest { makerRepository.templates() }
        .onEach { if (it is Resource.Success) settingsRepository.setTemplateCount(it.data.size) }
        .stateIn(viewModelScope, SharingStarted.Lazily, Resource.Loading)

    /**
     * Số thẻ chờ khi đang tải = số template lần tải gần nhất (lần đầu chưa biết thì [DEFAULT_PLACEHOLDER_COUNT]).
     * null = chưa đọc xong DataStore: chưa hiện thẻ nào, tránh hiện số mặc định rồi co lại.
     */
    val placeholderCount: StateFlow<Int?> = settingsRepository.templateCount
        .map { it ?: DEFAULT_PLACEHOLDER_COUNT }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        retryWhenBackOnline(networkMonitor, isError = { state.value is Resource.Error }, retry = ::retry)
    }

    fun retry() {
        reloadKey.value++
    }

    suspend fun isOnline(): Boolean = networkMonitor.isOnline.first()

    private companion object {
        const val DEFAULT_PLACEHOLDER_COUNT = 10
    }
}
