package com.conght.chibiocmaker.feature.cosplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conght.chibiocmaker.core.common.NetworkMonitor
import com.conght.chibiocmaker.core.common.retryWhenBackOnline
import com.conght.chibiocmaker.core.common.Resource
import com.conght.chibiocmaker.data.model.CosplayChallenge
import com.conght.chibiocmaker.data.repository.CosplayRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Màn bắt đầu Cosplay: đề bài ngẫu nhiên từ dữ liệu API (Loading → shimmer). */
@HiltViewModel
class CosplayViewModel @Inject constructor(
    private val cosplayRepository: CosplayRepository,
    private val gson: Gson,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {

    private val reloadKey = MutableStateFlow(0)

    /**
     * Lazily (không dừng khi màn ẩn): sang màn chơi rồi Back vẫn giữ đúng đề cũ.
     * WhileSubscribed sẽ huỷ luồng sau vài giây và tự random đề mới khi quay lại.
     * Chỉ [random] mới đổi đề.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val challenge: StateFlow<Resource<CosplayChallenge>> = reloadKey
        .flatMapLatest { cosplayRepository.randomChallenge() }
        .stateIn(viewModelScope, SharingStarted.Lazily, Resource.Loading)

    init {
        cosplayRepository.prepare()
        retryWhenBackOnline(networkMonitor, isError = { challenge.value is Resource.Error }, retry = ::random)
    }

    /** Random: xin đề mới (UI quay về shimmer tới khi ghép xong ảnh tham chiếu). */
    fun random() {
        reloadKey.value++
    }

    suspend fun isOnline(): Boolean = networkMonitor.isOnline.first()

    fun challengeJson(challenge: CosplayChallenge): String = gson.toJson(challenge)
}
