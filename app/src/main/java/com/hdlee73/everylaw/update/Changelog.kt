package com.hdlee73.everylaw.update

/** 앱 안에 함께 배포되는 업데이트 내역. 최신 버전이 위. (GitHub 릴리스 노트는 정보 화면에서 따로 보여준다) */
object Changelog {
    data class Entry(val version: String, val date: String, val notes: List<String>)

    val entries = listOf(
        Entry(
            "1.0.0", "2026-10-22",
            listOf(
                "법령명 + 조문 입력으로 법률→시행령→시행규칙→감독규정→시행세칙 위임 트리 조회",
                "폰은 아코디언, 폴더블·태블릿은 좌우 분할 비교 화면",
                "조문 관련 판례 목록과 판례 본문 보기",
                "법령·행정규칙·판례 키워드 통합 검색",
                "시행일(시점) 기준 조회와 시행 연혁 목록",
                "검색 결과 PDF 저장·공유",
                "GitHub 릴리스 기반 자동 업데이트(공식 서명 검증)",
                "공식 릴리스에 기본 인증키 내장: OC 입력 없이 바로 실제 법령 조회",
                "본인 OC 입력 시 우선 사용, 첫 실행·설정에 인증키 안내와 발급 방법 링크",
            ),
        ),
    )
}
