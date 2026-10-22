package com.hdlee73.everylaw.di

import android.content.Context
import com.hdlee73.everylaw.core.data.remote.LawApiService
import com.hdlee73.everylaw.core.data.repository.RemoteLawRepository
import com.hdlee73.everylaw.core.data.repository.SwitchingLawRepository
import com.hdlee73.everylaw.core.domain.GetAdminRuleUseCase
import com.hdlee73.everylaw.core.domain.GetDelegationTreeUseCase
import com.hdlee73.everylaw.core.domain.GetLawArticlesUseCase
import com.hdlee73.everylaw.core.domain.GetLawVersionsUseCase
import com.hdlee73.everylaw.core.domain.GetPrecedentUseCase
import com.hdlee73.everylaw.core.domain.LawRepository
import com.hdlee73.everylaw.core.domain.SearchKeywordUseCase
import com.hdlee73.everylaw.data.SettingsStore
import com.hdlee73.everylaw.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

/** 수동 DI 컨테이너. 앱 규모상 Hilt 없이 생성자 주입만 쓴다. */
class AppContainer(context: Context) {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings = SettingsStore(context.applicationContext, appScope)

    /** 사용자가 입력한 키 > 빌드 시 주입한 키 */
    private fun currentOc(): String = settings.settings.value.userOc.ifBlank { BuiltInKey.oc }

    // 요청 URL에 OC가 붙으므로 HTTP 로깅 인터셉터는 달지 않는다.
    private val api = LawApiService.create(ocProvider = ::currentOc)

    val repository: LawRepository = SwitchingLawRepository(
        hasKey = { currentOc().isNotBlank() },
        remote = RemoteLawRepository(api),
    )

    val getDelegationTree = GetDelegationTreeUseCase(repository)
    val searchKeyword = SearchKeywordUseCase(repository)
    val getLawVersions = GetLawVersionsUseCase(repository)
    val getLawArticles = GetLawArticlesUseCase(repository)
    val getPrecedent = GetPrecedentUseCase(repository)
    val getAdminRule = GetAdminRuleUseCase(repository)

    val updateManager = UpdateManager(context.applicationContext)
}
