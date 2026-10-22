package com.hdlee73.everylaw.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    /** 사용자가 직접 입력한 국가법령정보 OC. 비어 있으면 빌드에 주입된 값(BuildConfig.LAW_OC)을 쓴다. */
    val userOc: String = "",
    val autoUpdate: Boolean = true,
    val lastUpdateCheck: Long = 0L,
    val recent: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
)

/** 설정·최근 검색·즐겨찾기 저장소 (Jetpack DataStore). */
class SettingsStore(private val context: Context, scope: CoroutineScope) {

    private object Keys {
        val oc = stringPreferencesKey("law_oc")
        val autoUpdate = booleanPreferencesKey("auto_update")
        val lastCheck = longPreferencesKey("last_update_check")
        val recent = stringPreferencesKey("recent")
        val favorites = stringPreferencesKey("favorites")
    }

    val settings: StateFlow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            userOc = p[Keys.oc].orEmpty(),
            autoUpdate = p[Keys.autoUpdate] ?: true,
            lastUpdateCheck = p[Keys.lastCheck] ?: 0L,
            recent = p[Keys.recent].toList(),
            favorites = p[Keys.favorites].toList(),
        )
    }.stateIn(scope, SharingStarted.Eagerly, AppSettings())

    suspend fun setOc(value: String) = context.dataStore.edit { it[Keys.oc] = value.trim() }
    suspend fun setAutoUpdate(value: Boolean) = context.dataStore.edit { it[Keys.autoUpdate] = value }
    suspend fun markUpdateChecked(at: Long) = context.dataStore.edit { it[Keys.lastCheck] = at }

    suspend fun addRecent(query: String) = context.dataStore.edit { p ->
        p[Keys.recent] = (listOf(query) + p[Keys.recent].toList().filter { it != query }).take(20).join()
    }

    suspend fun clearRecent() = context.dataStore.edit { it.remove(Keys.recent) }

    suspend fun toggleFavorite(query: String) = context.dataStore.edit { p ->
        val list = p[Keys.favorites].toList()
        p[Keys.favorites] = (if (query in list) list - query else listOf(query) + list).join()
    }

    private fun String?.toList(): List<String> = this?.split(SEP)?.filter { it.isNotBlank() }.orEmpty()
    private fun List<String>.join(): String = joinToString(SEP)

    private companion object {
        const val SEP = "\u001F"
    }
}
