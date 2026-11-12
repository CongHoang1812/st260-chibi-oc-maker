package com.conght.chibiocmaker.testutil

import com.conght.chibiocmaker.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** SettingsRepository trong bộ nhớ cho unit test. */
class FakeSettingsRepository : SettingsRepository {
    override val languageCode = MutableStateFlow<String?>(null)
    override suspend fun setLanguage(code: String) {
        languageCode.value = code
    }

    override val onboardingDone = MutableStateFlow(false)
    override suspend fun setOnboardingDone(done: Boolean) {
        onboardingDone.value = done
    }

    override val appRated = MutableStateFlow(false)
    override suspend fun setAppRated() {
        appRated.value = true
    }

    var homeBackCount = 0
    override suspend fun incrementHomeBackCount(): Int = ++homeBackCount

    override val templateCount = MutableStateFlow<Int?>(null)
    override suspend fun setTemplateCount(count: Int) {
        templateCount.value = count
    }
}
