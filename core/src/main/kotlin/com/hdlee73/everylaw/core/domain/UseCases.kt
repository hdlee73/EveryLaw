package com.hdlee73.everylaw.core.domain

import com.hdlee73.everylaw.core.model.AdminRuleDocument
import com.hdlee73.everylaw.core.model.Article
import com.hdlee73.everylaw.core.model.ArticleNo
import com.hdlee73.everylaw.core.model.DelegationTree
import com.hdlee73.everylaw.core.model.KeywordSearchResult
import com.hdlee73.everylaw.core.model.LawQuery
import com.hdlee73.everylaw.core.model.LawSummary
import com.hdlee73.everylaw.core.model.LawVersion
import com.hdlee73.everylaw.core.model.formatYmd
import com.hdlee73.everylaw.core.model.PrecedentDetail
import com.hdlee73.everylaw.core.parser.LawAliases
import com.hdlee73.everylaw.core.parser.LawQueryParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 사용자에게 그대로 보여줄 수 있는 도메인 오류. */
sealed class LawDomainException(message: String) : Exception(message) {
    class InvalidQuery(message: String) : LawDomainException(message)
    class NotFound(message: String) : LawDomainException(message)
    /** 외부 API 호출·응답 처리 실패. 출처별 원인을 그대로 담는다. */
    class ApiFailure(message: String) : LawDomainException(message)
}

/** 트리 조회 진행 상황. UI가 단계별 로딩 문구를 보여줄 수 있게 Flow로 흘려보낸다. */
sealed interface DelegationProgress {
    data class Step(val message: String) : DelegationProgress
    data class Done(val tree: DelegationTree) : DelegationProgress
}

/**
 * "자본시장법 제12조" → 법률·시행령·시행규칙(3단비교) + 감독규정·시행세칙(행정규칙) + 관련 판례를 하나의 트리로.
 */
class GetDelegationTreeUseCase(
    private val repository: LawRepository,
    private val builder: DelegationTreeBuilder = DelegationTreeBuilder(),
) {
    /**
     * @param asOf 조회 기준 시행일(yyyyMMdd). null 이면 현행 법령 기준.
     */
    operator fun invoke(input: String, asOf: String? = null): Flow<DelegationProgress> = flow {
        val query = when (val parsed = LawQueryParser.parse(input)) {
            is LawQueryParser.Result.Invalid -> throw LawDomainException.InvalidQuery(parsed.reason)
            is LawQueryParser.Result.Ok -> parsed.query
        }

        emit(DelegationProgress.Step("'${query.lawName}' 찾는 중"))
        val law = pickLaw(query, repository.searchLaws(query.lawName))
            ?: throw LawDomainException.NotFound("'${query.lawName}' 법령을 찾지 못했어요.")

        // 병렬 조회(async)에서 함께 쓰므로 동기화 리스트
        val warnings: MutableList<String> = java.util.Collections.synchronizedList(mutableListOf())

        val version = asOf?.let { date ->
            emit(DelegationProgress.Step("${formatYmd(date)} 시행 버전 찾는 중"))
            repository.getLawVersions(law).filter { it.effectiveDate <= date }.maxByOrNull { it.effectiveDate }
                ?: throw LawDomainException.NotFound("${formatYmd(date)}에 시행 중이던 ${law.name}을(를) 찾지 못했어요.")
        }
        if (version != null) warnings += "감독규정·시행세칙과 판례는 현행 기준으로 함께 표시됩니다."

        emit(DelegationProgress.Step("3단 비교로 시행령·시행규칙 매핑 중"))
        val threeTier = runCatching { repository.getThreeTier(law, version?.mst ?: law.mst) }
            .onFailure { warnings += "3단 비교 조회 실패: ${it.message}" }
            .getOrNull()
        val row = threeTier?.rows?.firstOrNull { it.law.number.sameAs(query.article) }

        // 시점 조회는 시행일 법령 본문이 기준(3단비교 본문은 보조), 현행 조회는 3단비교 본문을 우선 사용
        val fetched = suspend {
            runCatching {
                if (version != null) repository.getLawArticlesAt(version, query.article)
                else repository.getLawArticles(law, query.article)
            }.getOrDefault(emptyList()).firstOrNull { it.number.sameAs(query.article) }
        }
        val fromRow = row?.law?.takeIf { it.content.isNotBlank() }
        val lawArticle: Article = (if (version != null) fetched() ?: fromRow else fromRow ?: fetched())
            ?: throw LawDomainException.NotFound("${law.name}에 ${query.article.display}가 없어요.")

        emit(DelegationProgress.Step("감독규정·시행세칙과 판례 찾는 중"))
        val (supervisory, details, precedents) = coroutineScope {
            val names = async { relatedAdminRuleNames(law) }
            val prec = async {
                runCatching { repository.searchPrecedents("${law.name} ${query.article.display}") }
                    .onFailure { warnings += "판례 조회 실패: ${it.message}" }
                    .getOrDefault(emptyList())
            }
            val (supNames, detNames) = names.await()
            val sup = async { loadAdminRules(supNames, warnings) }
            val det = async { loadAdminRules(detNames, warnings) }
            Triple(sup.await(), det.await(), prec.await())
        }

        val root = builder.build(
            DelegationTreeBuilder.Input(
                lawName = law.name,
                lawArticle = lawArticle,
                decreeName = threeTier?.decreeName,
                decrees = row?.decrees.orEmpty(),
                ruleName = threeTier?.ruleName,
                rules = row?.rules.orEmpty(),
                supervisory = supervisory,
                details = details,
            )
        )
        emit(
            DelegationProgress.Done(
                DelegationTree(
                    query = query,
                    law = law,
                    root = root,
                    asOf = asOf,
                    version = version,
                    precedents = precedents.take(30),
                    isSample = repository.isSample,
                    warnings = warnings.toList(),
                )
            )
        )
    }

    private fun pickLaw(query: LawQuery, candidates: List<LawSummary>): LawSummary? {
        val compact = query.lawName.replace(" ", "")
        return candidates.firstOrNull { it.name.replace(" ", "") == compact }
            ?: candidates.firstOrNull { it.abbreviation?.replace(" ", "") == compact }
            ?: candidates.firstOrNull { it.kind == "법률" }
            ?: candidates.firstOrNull()
    }

    /** (감독규정 이름들, 시행세칙 이름들) */
    private suspend fun relatedAdminRuleNames(law: LawSummary): Pair<List<String>, List<String>> {
        val fromHierarchy = runCatching { repository.getRelatedAdminRuleNames(law) }.getOrDefault(emptyList())
        val details = (LawAliases.enforcementDetails(law.name) + fromHierarchy.filter { it.contains("세칙") }).distinct()
        val supervisory = (LawAliases.supervisoryRegulations(law.name) +
            fromHierarchy.filter { it.contains("규정") && !it.contains("세칙") }).distinct()
        return supervisory.take(MAX_ADMIN_DOCS) to details.take(MAX_ADMIN_DOCS)
    }

    private suspend fun loadAdminRules(names: List<String>, warnings: MutableList<String>): List<AdminRuleDocument> =
        coroutineScope {
            names.map { name ->
                async {
                    runCatching {
                        val hit = repository.searchAdminRules(name).let { list ->
                            list.firstOrNull { it.name.replace(" ", "") == name.replace(" ", "") } ?: list.firstOrNull()
                        } ?: return@runCatching null
                        repository.getAdminRule(hit.serialNo).let { if (it.name.isBlank()) it.copy(name = hit.name, agency = hit.agency) else it }
                    }.onFailure { warnings += "'$name' 조회 실패: ${it.message}" }.getOrNull()
                }
            }.awaitAll().filterNotNull()
        }

    companion object {
        const val MAX_ADMIN_DOCS = 3
    }
}

private fun ArticleNo?.sameAs(other: ArticleNo): Boolean =
    this != null && main.trimStart('0') == other.main.trimStart('0') && (branch ?: 0) == (other.branch ?: 0)

/** 법령·행정규칙·판례를 한 번에 찾는 키워드 검색. 일부가 실패해도 나머지 결과는 보여준다. */
class SearchKeywordUseCase(private val repository: LawRepository) {
    suspend operator fun invoke(keyword: String): KeywordSearchResult {
        val k = keyword.trim()
        if (k.length < 2) throw LawDomainException.InvalidQuery("검색어를 두 글자 이상 입력해 주세요.")
        return coroutineScope {
            val laws = async { runCatching { repository.searchLaws(k) } }
            val rules = async { runCatching { repository.searchAdminRules(k) } }
            val precs = async { runCatching { repository.searchPrecedents(k) } }
            val lawsR = laws.await()
            val rulesR = rules.await()
            val precsR = precs.await()
            val failures = listOf("법령" to lawsR, "행정규칙" to rulesR, "판례" to precsR).mapNotNull { (label, r) ->
                r.exceptionOrNull()?.let { "$label 검색 실패 · ${describe(it)}" }
            }
            if (failures.size == 3) throw LawDomainException.ApiFailure(failures.joinToString("\n"))
            KeywordSearchResult(
                keyword = k,
                laws = lawsR.getOrDefault(emptyList()),
                adminRules = rulesR.getOrDefault(emptyList()),
                precedents = precsR.getOrDefault(emptyList()),
                isSample = repository.isSample,
                warnings = failures,
            )
        }
    }

    private fun describe(e: Throwable): String =
        "${e::class.simpleName}: ${e.message?.takeIf { it.isNotBlank() } ?: "(메시지 없음)"}"
}

class GetLawVersionsUseCase(private val repository: LawRepository) {
    suspend operator fun invoke(law: LawSummary): List<LawVersion> = repository.getLawVersions(law)
}

class GetLawArticlesUseCase(private val repository: LawRepository) {
    suspend operator fun invoke(law: LawSummary): List<Article> =
        repository.getLawArticles(law).filter { it.number != null }
}

class GetAdminRuleUseCase(private val repository: LawRepository) {
    suspend operator fun invoke(serialNo: String): AdminRuleDocument = repository.getAdminRule(serialNo)
}

class GetPrecedentUseCase(private val repository: LawRepository) {
    suspend operator fun invoke(serialNo: String): PrecedentDetail = repository.getPrecedent(serialNo)
}
