package com.hdlee73.everylaw.core.model

/**
 * 위임 계층의 단계. 순서(ordinal)가 곧 상하위 깊이다.
 * 법률 → 시행령(대통령령) → 시행규칙(총리령·부령) → 감독규정(고시) → 시행세칙(감독원 세칙)
 */
enum class LawLevel(val label: String, val shortLabel: String) {
    LAW("법률", "법률"),
    DECREE("시행령", "시행령"),
    RULE("시행규칙", "시행규칙"),
    SUPERVISORY_REGULATION("감독규정", "감독규정"),
    ENFORCEMENT_DETAIL("시행세칙", "세칙");

    val depth: Int get() = ordinal
}

/**
 * "제12조", "제10조의2", "제2-3조"(행정규칙식 편장 번호) 같은 조문 번호.
 * [main]은 "12" 또는 "2-3"처럼 하이픈 포함 문자열, [branch]는 "의N" 가지번호.
 */
data class ArticleNo(val main: String, val branch: Int? = null) {
    val display: String get() = "제${main}조" + (branch?.let { "의$it" } ?: "")

    /** lawService.do 의 JO 파라미터 형식(조번호 4자리 + 가지번호 2자리). 하이픈 번호는 지원하지 않는다. */
    val joParam: String?
        get() = main.toIntOrNull()?.let { "%04d%02d".format(it, branch ?: 0) }

    override fun toString(): String = display

    companion object {
        private val PATTERN = Regex("""제?\s*(\d+(?:-\d+)*)\s*조(?:\s*의\s*(\d+))?""")

        /** API는 조번호를 "0420"처럼 0을 채워 주기도 한다. 본문 인용("제420조")과 맞도록 "420"으로 통일한다. */
        fun normalizeMain(raw: String): String =
            raw.trim().split('-').joinToString("-") { it.trimStart('0').ifEmpty { "0" } }

        fun of(main: String, branch: Int? = null) = ArticleNo(normalizeMain(main), branch)

        fun parse(text: String): ArticleNo? = PATTERN.find(text)?.let {
            of(it.groupValues[1], it.groupValues[2].toIntOrNull())
        }
    }
}

/** 하나의 조문(또는 행정규칙 조항). */
data class Article(
    val number: ArticleNo?,
    val title: String,
    val content: String,
) {
    val heading: String
        get() = listOfNotNull(number?.display, title.takeIf { it.isNotBlank() }).joinToString(" ")
}

/**
 * 위임 트리의 노드.
 * @param id 트리 안에서 유일한 키(UI 펼침 상태 저장용)
 * @param sourceName 법령/행정규칙 이름 (예: "자본시장과 금융투자업에 관한 법률 시행령")
 * @param issuer 소관·발령 기관 (행정규칙에서 주로 채워짐)
 */
data class LawNode(
    val id: String,
    val level: LawLevel,
    val sourceName: String,
    val article: Article,
    val issuer: String? = null,
    val children: List<LawNode> = emptyList(),
) {
    /** 자신을 포함한 모든 하위 노드를 깊이 우선으로 펼친 목록. */
    fun flatten(): List<LawNode> = listOf(this) + children.flatMap { it.flatten() }

    fun find(nodeId: String): LawNode? =
        if (id == nodeId) this else children.firstNotNullOfOrNull { it.find(nodeId) }

    /** 루트에서 [nodeId]까지의 경로. 없으면 빈 목록. */
    fun pathTo(nodeId: String): List<LawNode> {
        if (id == nodeId) return listOf(this)
        for (child in children) {
            val sub = child.pathTo(nodeId)
            if (sub.isNotEmpty()) return listOf(this) + sub
        }
        return emptyList()
    }

    val descendantCount: Int get() = children.sumOf { 1 + it.descendantCount }
}

/** 사용자가 입력한 검색어를 파싱한 결과. */
data class LawQuery(
    val raw: String,
    val lawName: String,
    val article: ArticleNo,
)

/** 법령 검색 결과 한 건 (lawSearch.do target=law). */
data class LawSummary(
    val id: String,
    val mst: String,
    val name: String,
    val abbreviation: String?,
    val kind: String?,
    val effectiveDate: String?,
)

/** 행정규칙 검색 결과 한 건 (lawSearch.do target=admrul). */
data class AdminRuleSummary(
    val serialNo: String,
    val name: String,
    val kind: String?,
    val agency: String?,
)

/** 행정규칙 본문 (lawService.do target=admrul). */
data class AdminRuleDocument(
    val serialNo: String,
    val name: String,
    val agency: String?,
    val articles: List<Article>,
)

/** 위임조문 3단비교의 한 줄: 법률 조문과 그 조문이 위임한 시행령·시행규칙 조문들. */
data class ThreeTierRow(
    val law: Article,
    val decrees: List<Article>,
    val rules: List<Article>,
)

data class ThreeTierComparison(
    val lawName: String,
    val decreeName: String?,
    val ruleName: String?,
    val rows: List<ThreeTierRow>,
)

/** 검색 한 번의 최종 결과. */
data class DelegationTree(
    val query: LawQuery,
    val law: LawSummary,
    val root: LawNode,
    /** 조회 기준 시행일(yyyyMMdd). null 이면 현행. */
    val asOf: String? = null,
    /** 실제로 조회에 쓰인 법률 버전 (시행일 기준 조회일 때) */
    val version: LawVersion? = null,
    /** 해당 조문을 참조조문으로 다룬 판례. */
    val precedents: List<PrecedentSummary> = emptyList(),
    /** 데모 데이터 여부 (API 키가 없을 때 true). */
    val isSample: Boolean = false,
    /** 일부 단계 조회 실패 등 사용자에게 알릴 참고 메시지. */
    val warnings: List<String> = emptyList(),
)

/** 판례 검색 결과 한 건 (lawSearch.do target=prec). */
data class PrecedentSummary(
    val serialNo: String,
    val caseName: String,
    val caseNumber: String?,
    val decisionDate: String?,
    val court: String?,
    val caseType: String?,
)

/** 판례 본문 (lawService.do target=prec). */
data class PrecedentDetail(
    val summary: PrecedentSummary,
    val holdings: String,
    val gist: String,
    val referencedArticles: String,
    val referencedCases: String,
    val fullText: String,
)

/** 키워드 통합 검색 결과. */
data class KeywordSearchResult(
    val keyword: String,
    val laws: List<LawSummary>,
    val adminRules: List<AdminRuleSummary>,
    val precedents: List<PrecedentSummary>,
    val isSample: Boolean = false,
    /** 일부 출처가 실패했을 때의 구체적인 원인 (예: "법령: DrfFormatException: …") */
    val warnings: List<String> = emptyList(),
)

/**
 * 법령의 시행일 기준 버전 (lawSearch.do target=eflaw).
 * @param status 현행 / 연혁 / 시행예정
 */
data class LawVersion(
    val mst: String,
    val name: String,
    val effectiveDate: String,
    val promulgationDate: String?,
    val revisionType: String?,
    val status: String?,
) {
    /** 20260101 → 2026.01.01 */
    val effectiveDateLabel: String get() = formatYmd(effectiveDate)
}

fun formatYmd(ymd: String?): String =
    if (ymd != null && ymd.length == 8 && ymd.all(Char::isDigit)) "${ymd.substring(0, 4)}.${ymd.substring(4, 6)}.${ymd.substring(6)}"
    else ymd.orEmpty()
