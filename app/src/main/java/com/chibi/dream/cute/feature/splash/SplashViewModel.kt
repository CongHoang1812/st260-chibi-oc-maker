package com.chibi.dream.cute.feature.splash

import androidx.lifecycle.ViewModel
import com.chibi.dream.cute.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    suspend fun isOnboardingDone(): Boolean = settingsRepository.onboardingDone.first()
}
