package com.hdlee73.everylaw.core

import com.hdlee73.everylaw.core.data.repository.SampleLawRepository
import com.hdlee73.everylaw.core.domain.DelegationProgress
import com.hdlee73.everylaw.core.domain.DelegationTreeBuilder
import com.hdlee73.everylaw.core.domain.GetDelegationTreeUseCase
import com.hdlee73.everylaw.core.domain.SearchKeywordUseCase
import com.hdlee73.everylaw.core.model.ArticleNo
import com.hdlee73.everylaw.core.model.LawLevel
import com.hdlee73.everylaw.core.parser.DrfParsers
import com.hdlee73.everylaw.core.parser.LawQueryParser
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LawQueryParserTest {
    private fun ok(s: String) = (LawQueryParser.parse(s) as LawQueryParser.Result.Ok).query

    @Test fun parsesAbbreviationAndArticle() {
        val q = ok("자본시장법 제11조")
        assertEquals("자본시장과 금융투자업에 관한 법률", q.lawName)
        assertEquals(ArticleNo("11"), q.article)
    }

    @Test fun parsesBranchAndLooseForms() {
        assertEquals(ArticleNo("34", 2), ok("은행법 34조의2").article)
        assertEquals(ArticleNo("12"), ok("자본시장법 §12").article)
        assertEquals("은행법", ok("은행법제34조").lawName)
    }

    @Test fun rejectsMissingParts() {
        assertTrue(LawQueryParser.parse("자본시장법") is LawQueryParser.Result.Invalid)
        assertTrue(LawQueryParser.parse("제11조") is LawQueryParser.Result.Invalid)
    }

    @Test fun joParam() {
        assertEquals("001200", ArticleNo("12").joParam)
        assertEquals("003402", ArticleNo("34", 2).joParam)
    }
}

class ReferenceMatchTest {
    @Test fun matchesExactArticleOnly() {
        val no = ArticleNo("12")
        assertTrue(DelegationTreeBuilder.refersTo("법 제12조제2항에 따라", DelegationTreeBuilder.LAW_PREFIX, no))
        assertFalse(DelegationTreeBuilder.refersTo("법 제120조에 따라", DelegationTreeBuilder.LAW_PREFIX, no))
        assertFalse(DelegationTreeBuilder.refersTo("법 제12조의2에 따라", DelegationTreeBuilder.LAW_PREFIX, no))
        assertFalse(DelegationTreeBuilder.refersTo("은행법 제12조", DelegationTreeBuilder.LAW_PREFIX, no))
        assertTrue(DelegationTreeBuilder.refersTo("영 제16조에 따른", DelegationTreeBuilder.DECREE_PREFIX, ArticleNo("16")))
        assertTrue(DelegationTreeBuilder.refersTo("규정 제2-3조제2항", DelegationTreeBuilder.SUPERVISORY_PREFIX, ArticleNo("2-3")))
        assertFalse(DelegationTreeBuilder.refersTo("규정 제2-30조", DelegationTreeBuilder.SUPERVISORY_PREFIX, ArticleNo("2-3")))
    }
}

class DrfParsersTest {
    @Test fun parsesLawSearch() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
            <LawSearch><target>law</target><totalCnt>1</totalCnt>
            <law id="1"><법령일련번호>123456</법령일련번호><법령명한글><![CDATA[자본시장과 금융투자업에 관한 법률]]></법령명한글>
            <법령약칭명><![CDATA[자본시장법]]></법령약칭명><법령ID>009999</법령ID><법령구분명>법률</법령구분명><시행일자>20260101</시행일자></law>
            </LawSearch>"""
        val r = DrfParsers.parseLawSearch(xml).single()
        assertEquals("123456", r.mst)
        assertEquals("자본시장법", r.abbreviation)
    }

    @Test fun parsesThreeTierRows() {
        val xml = """<ThdCmpLawXService><기본정보><법령명>테스트법</법령명><시행령명>테스트법 시행령</시행령명></기본정보>
            <위임조문삼단비교>
              <법률조문><조번호>12</조번호><조제목>인가</조제목><조내용>제12조(인가) ② 대통령령으로 정하는 요건</조내용></법률조문>
              <시행령조문><조번호>16</조번호><조제목>인가요건</조제목><조내용>제16조(인가요건) 법 제12조</조내용></시행령조문>
              <시행규칙조문><조번호>2</조번호><조내용>제2조(신청서) 영 제16조</조내용></시행규칙조문>
            </위임조문삼단비교></ThdCmpLawXService>"""
        val t = DrfParsers.parseThreeTier(xml)
        assertEquals("테스트법 시행령", t.decreeName)
        val row = t.rows.single()
        assertEquals(ArticleNo("12"), row.law.number)
        assertEquals("인가요건", row.decrees.single().title)
        assertEquals("신청서", row.rules.single().title)
    }

    @Test fun parsesLawBodyWithParagraphs() {
        val xml = """<법령><기본정보><법령명_한글>테스트법</법령명_한글></기본정보><조문>
            <조문단위><조문번호>1</조문번호><조문여부>전문</조문여부><조문내용>제1장 총칙</조문내용></조문단위>
            <조문단위><조문번호>12</조문번호><조문여부>조문</조문여부><조문제목>인가</조문제목><조문내용>제12조(인가)</조문내용>
              <항><항내용>① 첫째 항</항내용></항><항><항내용>② 둘째 항</항내용><호><호내용>1. 호</호내용></호></항></조문단위>
            </조문></법령>"""
        val body = DrfParsers.parseLawBody(xml)
        val art = body.articles.single()
        assertEquals("인가", art.title)
        assertTrue(art.content.contains("② 둘째 항\n1. 호"))
    }

    @Test fun splitsAdminRuleArticles() {
        val arts = DrfParsers.splitAdminArticles("제2장 인가\n제2-3조(대주주의 요건) ① 영 제16조\n제2-4조(서식) 규칙 제2조")
        assertEquals(listOf("2-3", "2-4"), arts.map { it.number?.main })
        assertEquals("대주주의 요건", arts[0].title)
    }

    @Test fun parsesPrecedent() {
        val xml = """<PrecService><판례정보일련번호>1</판례정보일련번호><사건명>사건</사건명><판결요지>요지<br/>둘째줄</판결요지><참조조문>법 제12조</참조조문></PrecService>"""
        val p = DrfParsers.parsePrecedent(xml)
        assertEquals("사건", p.summary.caseName)
        assertEquals("법 제12조", p.referencedArticles)
    }

    @Test(expected = com.hdlee73.everylaw.core.parser.DrfFormatException::class)
    fun rejectsHtmlError() {
        DrfParsers.parseLawSearch("<!DOCTYPE html><html><body>사용자 정보 검증에 실패하였습니다.</body></html>")
    }
}

class UseCaseTest {
    @Test fun buildsFiveLevelTreeFromSample() = runTest {
        val progress = GetDelegationTreeUseCase(SampleLawRepository(latencyMs = 0))("자본시장법 제12조").toList()
        val tree = (progress.last() as DelegationProgress.Done).tree
        val levels = tree.root.flatten().map { it.level }.toSet()
        assertEquals(LawLevel.entries.toSet(), levels)
        val det = tree.root.flatten().first { it.level == LawLevel.ENFORCEMENT_DETAIL }
        assertEquals(
            listOf(LawLevel.LAW, LawLevel.DECREE, LawLevel.SUPERVISORY_REGULATION, LawLevel.ENFORCEMENT_DETAIL),
            tree.root.pathTo(det.id).map { it.level },
        )
        assertTrue(tree.precedents.isNotEmpty())
        assertTrue(tree.isSample)
    }

    @Test fun keywordSearchReturnsAllSections() = runTest {
        val r = SearchKeywordUseCase(SampleLawRepository(latencyMs = 0))("금융투자")
        assertTrue(r.laws.isNotEmpty())
        assertTrue(r.precedents.isNotEmpty())
    }
}

class AsOfTest {
    @Test fun picksVersionInForceOnDate() = runTest {
        val progress = GetDelegationTreeUseCase(SampleLawRepository(latencyMs = 0))("자본시장법 제12조", asOf = "20250315").toList()
        val tree = (progress.last() as DelegationProgress.Done).tree
        assertEquals("20240101", tree.version?.effectiveDate)
        assertTrue(tree.root.article.content.contains("2024.01.01 시행 당시"))
    }
}
