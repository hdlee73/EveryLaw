package com.hdlee73.everylaw.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hdlee73.everylaw.di.BuiltInKey
import com.hdlee73.everylaw.data.AppSettings
import com.hdlee73.everylaw.data.SettingsStore
import com.hdlee73.everylaw.update.ReleaseInfo
import com.hdlee73.everylaw.update.UpdateManager
import com.hdlee73.everylaw.update.UpdateState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: SettingsStore,
    private val updates: UpdateManager,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = store.settings
    val updateState: StateFlow<UpdateState> = updates.state
    val currentVersion: String get() = updates.currentVersion

    /** 빌드에 키가 주입되었는지 (값 자체는 노출하지 않음) */
    val hasBuiltInKey: Boolean = BuiltInKey.oc.isNotBlank()

    fun saveOc(value: String) = viewModelScope.launch { store.setOc(value) }
    fun setAutoUpdate(value: Boolean) = viewModelScope.launch { store.setAutoUpdate(value) }

    fun checkUpdate() = viewModelScope.launch {
        updates.check()
        store.markUpdateChecked(System.currentTimeMillis())
    }

    /** 앱 시작 시 하루 한 번 자동 확인 */
    fun autoCheckIfDue() = viewModelScope.launch {
        val s = store.settings.value
        if (s.autoUpdate && System.currentTimeMillis() - s.lastUpdateCheck > DAY_MS) {
            updates.check()
            store.markUpdateChecked(System.currentTimeMillis())
        }
    }

    fun download(release: ReleaseInfo) = viewModelScope.launch { updates.download(release) }
    fun install(state: UpdateState.ReadyToInstall) = updates.install(state.file)
    fun dismissUpdate() = updates.reset()

    private companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
