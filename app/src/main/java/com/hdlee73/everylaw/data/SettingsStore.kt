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

/** 분할 화면(좌우) 사용 방식 */
enum class SplitMode(val label: String, val desc: String) {
    Auto("자동", "화면이 넓을 때만 좌우로 나눠요"),
    Always("항상 분할", "화면 크기와 상관없이 좌우로 나눠요"),
    Never("분할 안 함", "항상 한 열(아코디언)로 보여요"),
}

data class AppSettings(
    /** 사용자가 직접 입력한 국가법령정보 OC. 비어 있으면 빌드에 주입된 값(BuiltInKey)을 쓴다. */
    val userOc: String = "",
    val autoUpdate: Boolean = true,
    val splitMode: SplitMode = SplitMode.Auto,
    /** 하위 법규를 위임한 항·호 바로 아래에 펼쳐 보여 줄지(true), 접이식 묶음으로 둘지(false) */
    val inlineDelegation: Boolean = true,
    val lastUpdateCheck: Long = 0L,
    val recent: List<String> = emptyList(),
    val favorites: List<String> = emptyList(),
    /** 첫 실행 OC 안내를 봤는지 */
    val ocGuideSeen: Boolean = false,
    /** 조문별로 사용자가 숨긴 하위 규정. 키는 "법령명|제N조", 값은 숨긴 노드의 안정 키(LawNode.stableKey) */
    val hidden: Map<String, Set<String>> = emptyMap(),
    /** DataStore에서 실제로 읽어 왔는지 (초기 기본값과 구분해 안내가 깜빡이지 않게) */
    val loaded: Boolean = false,
)

/** 설정·최근 검색·즐겨찾기 저장소 (Jetpack DataStore). */
class SettingsStore(private val context: Context, scope: CoroutineScope) {

    private object Keys {
        val oc = stringPreferencesKey("law_oc")
        val autoUpdate = booleanPreferencesKey("auto_update")
        val splitMode = stringPreferencesKey("split_mode")
        val inlineDelegation = booleanPreferencesKey("inline_delegation")
        val lastCheck = longPreferencesKey("last_update_check")
        val recent = stringPreferencesKey("recent")
        val favorites = stringPreferencesKey("favorites")
        val ocGuideSeen = booleanPreferencesKey("oc_guide_seen")
        val hidden = stringPreferencesKey("hidden_nodes")
    }

    val settings: StateFlow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            userOc = p[Keys.oc].orEmpty(),
            autoUpdate = p[Keys.autoUpdate] ?: true,
            splitMode = SplitMode.entries.firstOrNull { it.name == p[Keys.splitMode] } ?: SplitMode.Auto,
            inlineDelegation = p[Keys.inlineDelegation] ?: true,
            lastUpdateCheck = p[Keys.lastCheck] ?: 0L,
            recent = p[Keys.recent].toList(),
            favorites = p[Keys.favorites].toList(),
            ocGuideSeen = p[Keys.ocGuideSeen] ?: false,
            hidden = p[Keys.hidden].toList().mapNotNull { e ->
                e.split(PAIR, limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
            }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() },
            loaded = true,
        )
    }.stateIn(scope, SharingStarted.Eagerly, AppSettings())

    suspend fun setOc(value: String) = context.dataStore.edit { it[Keys.oc] = value.trim() }
    suspend fun setAutoUpdate(value: Boolean) = context.dataStore.edit { it[Keys.autoUpdate] = value }
    suspend fun setSplitMode(value: SplitMode) = context.dataStore.edit { it[Keys.splitMode] = value.name }
    suspend fun setInlineDelegation(value: Boolean) = context.dataStore.edit { it[Keys.inlineDelegation] = value }
    suspend fun markOcGuideSeen() = context.dataStore.edit { it[Keys.ocGuideSeen] = true }
    suspend fun markUpdateChecked(at: Long) = context.dataStore.edit { it[Keys.lastCheck] = at }

    suspend fun addRecent(query: String) = context.dataStore.edit { p ->
        p[Keys.recent] = (listOf(query) + p[Keys.recent].toList().filter { it != query }).take(20).join()
    }

    suspend fun clearRecent() = context.dataStore.edit { it.remove(Keys.recent) }

    suspend fun toggleFavorite(query: String) = context.dataStore.edit { p ->
        val list = p[Keys.favorites].toList()
        p[Keys.favorites] = (if (query in list) list - query else listOf(query) + list).join()
    }

    /** 조문 [articleKey]의 노드 [nodeKey]를 숨기거나(true) 되살린다(false). 전체 200건까지만 보관한다. */
    suspend fun setHidden(articleKey: String, nodeKey: String, hide: Boolean) = context.dataStore.edit { p ->
        val entry = articleKey + PAIR + nodeKey
        val list = p[Keys.hidden].toList().filter { it != entry }
        p[Keys.hidden] = (if (hide) list + entry else list).takeLast(200).join()
    }

    suspend fun clearHidden(articleKey: String) = context.dataStore.edit { p ->
        p[Keys.hidden] = p[Keys.hidden].toList().filter { !it.startsWith(articleKey + PAIR) }.join()
    }

    private fun String?.toList(): List<String> = this?.split(SEP)?.filter { it.isNotBlank() }.orEmpty()
    private fun List<String>.join(): String = joinToString(SEP)

    private companion object {
        const val SEP = "\u001F"
        const val PAIR = "\u001E"
    }
}
