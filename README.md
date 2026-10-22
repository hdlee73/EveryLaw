# Everylaw

법조문 하나(예: `자본시장법 제12조`)를 입력하면 그 조문이 위임한 하위 규정을
**법률 → 시행령 → 시행규칙 → 감독규정 → 시행세칙** 순서로 거슬러 찾아 한 화면에 계층으로 보여주는 안드로이드 앱입니다.

- 만든이: 이현덕 (hdlee73@gmail.com)
- 데이터: [국가법령정보 공동활용 Open API](https://open.law.go.kr/LSO/openApi/guideList.do)
- 디자인: 시안 C · 머티리얼 유(Tonal), Material 3

## 주요 기능

| 기능 | 설명 |
| --- | --- |
| 조문 파싱 | `자본시장법 제11조`, `은행법 34조의2`, `자본시장법 §12` 등을 법령명 + 조문번호로 분리, 약칭은 정식 명칭으로 변환 |
| 위임 트리 | 3단 비교(위임조문, `target=thdCmp&knd=2`)로 법률·시행령·시행규칙을 묶고, 행정규칙(`target=admrul`)에서 감독규정·시행세칙 조문을 찾아 병합 |
| 반응형 화면 | 폭 600dp 미만(일반 폰)은 아코디언, 그 이상(갤럭시 Z 폴드 펼침·태블릿)은 좌우 분할 비교 + 내비게이션 레일 |
| 관련 판례 | `target=prec` 본문검색으로 해당 조문을 다룬 판례 목록·본문(판시사항·판결요지·참조조문) |
| 키워드 검색 | 조문 번호 없이 입력하면 법령·행정규칙·판례 통합 검색 |
| 시행시점 조회 | 시행일을 고르거나 시행 연혁(`target=eflaw`)에서 버전을 골라 그 시점 조문 기준으로 조회 |
| PDF | 검색 결과(조문·하위 규정·판례)를 A4 PDF로 저장(SAF) 또는 공유. 기본 글꼴로 한글 출력 |
| 앱 정보·업데이트 | 앱 이름·버전·만든이·업데이트 내역, GitHub Releases 자동 업데이트 확인 |
| 뒤로가기 | 앱을 끄지 않고 직전 결과/화면으로 이동, 홈 첫 화면에서만 두 번 눌러 종료 |

## 구조 (MVVM + 클린 아키텍처)

```
core/  (순수 Kotlin, JVM에서 단위 테스트)
  model/        LawLevel, LawNode(트리), Article, LawVersion, Precedent…
  parser/       LawQueryParser(입력 파싱·약칭), XmlTree, DrfParsers(DRF XML → 모델)
  data/remote/  LawApiService (Retrofit2 + OkHttp, OC는 인터셉터로 주입)
  data/repository/ RemoteLawRepository, SampleLawRepository(데모), SwitchingLawRepository
  domain/       LawRepository, DelegationTreeBuilder, UseCases
app/   (Android, Jetpack Compose Material3)
  ui/search     SearchViewModel(StateFlow: Idle/Loading/TreeSuccess/KeywordSuccess/Error), SearchBar, SearchScreen
  ui/tree       LawTreeView(Split ↔ Accordion 분기), PdfExporter
  ui/library    판례·조문 목록·즐겨찾기·기록
  ui/settings   설정, 앱 정보, 업데이트 대화상자
  update/       UpdateManager(GitHub Releases + 서명 검증), Changelog
```

### 하위 규정을 연결하는 규칙 (`DelegationTreeBuilder`)

- 시행령: 3단 비교가 같은 줄로 묶어 준 조문
- 시행규칙: 본문에 `영 제N조`/`시행령 제N조`가 있으면 그 시행령 아래
- 감독규정: 본문이 참조하는 가장 하위 조문(시행규칙 > 시행령 > 법률) 아래
- 시행세칙: 본문이 참조하는 감독규정 조문(`규정 제2-3조`) 아래
- `제12조`가 `제120조`·`제12조의2`나 다른 법(`은행법 제12조`)과 섞이지 않도록 경계를 검사합니다.

감독규정·시행세칙 후보는 법령 체계도(`target=lsStmd`)와 주요 금융법의 알려진 감독규정 목록(`LawAliases`)에서 가져옵니다.

## 빌드

```bash
./gradlew :core:test :app:assembleDebug
```

### 국가법령정보 OC (인증값)

공식 릴리스 APK에는 **기본 OC가 내장**되어 있어 사용자는 아무것도 입력하지 않아도 됩니다.
OC는 저장소에 커밋하지 않고, 릴리스 워크플로가 GitHub Secret `LAW_OC`를 빌드 시 `BuildConfig`에 넣습니다(Secret이 없으면 릴리스가 실패하도록 막아 둠).

| 빌드 | 사용하는 키 |
| --- | --- |
| 공식 릴리스 (태그 `v*`) | Secret `LAW_OC` 내장 키 |
| 로컬 빌드 | `local.properties`의 `law.oc=발급받은OC` (git에서 제외) |
| PR CI 디버그 APK·포크 | 키 없음 → 데모 모드 |

설정 화면의 OC 입력은 **선택 사항**으로, 내 OC로 덮어쓸 때만 씁니다. 사용자 키도 내장 키도 없을 때만 데모 배지가 보입니다.

> 주의: 앱에 넣은 키는 숨길 수 없습니다. 문자열 검색으로 바로 보이지 않게 XOR로 감싸 두었지만, APK를 디컴파일하면 꺼낼 수 있습니다.
> 또 모든 사용자가 같은 OC로 호출하므로 일일 호출 한도와 이용 기록을 함께 쓰게 되고, 남용되면 그 OC가 제한될 수 있습니다.
> 국가법령정보 OC는 과금 키가 아니어서 위험은 크지 않지만, 사용자가 많아지면 별도 서버(프록시)를 두거나 키를 교체하세요.

## 릴리스 서명과 자동 업데이트

저장소가 공개여도 **다른 사람이 앱 업데이트를 배포할 수 없도록** 다음처럼 막습니다.

- 서명 키스토어와 비밀번호는 GitHub **Secrets**에만 저장합니다. 포크의 워크플로에는 Secrets가 전달되지 않고, 릴리스 잡은 `github.repository == 'hdlee73/Everylaw'`일 때만 돕니다.
- 앱의 업데이트 확인 주소는 빌드에 고정된 `hdlee73/Everylaw` 릴리스뿐입니다.
- 내려받은 APK의 서명 인증서(SHA-256)가 설치된 앱과 다르면 설치하지 않습니다. 안드로이드 자체도 서명이 다른 APK의 덮어쓰기 설치를 거부합니다.

필요한 Secrets (Settings › Secrets and variables › Actions):

| 이름 | 값 |
| --- | --- |
| `KEYSTORE_BASE64` | `base64 -w0 release.jks` 결과 |
| `KEYSTORE_PASSWORD` | 키스토어 비밀번호 |
| `KEY_ALIAS` | 키 별칭 |
| `KEY_PASSWORD` | 키 비밀번호 |
| `LAW_OC` | 국가법령정보 OC |

키 만들기 예:

```bash
keytool -genkeypair -v -keystore release.jks -alias everylaw -keyalg RSA -keysize 4096 -validity 10000
```

릴리스는 태그를 올리면 됩니다: `git tag v1.0.0 && git push origin v1.0.0` → 서명된 APK가 GitHub Release에 올라가고, 설치된 앱이 하루 한 번 확인해 업데이트를 안내합니다.

## 알려진 한계

- DRF 응답의 XML 태그명은 공개 가이드 기준으로 작성했고, 대상별 래퍼 구조 차이에 견디도록 "이름으로 찾기" 방식으로 파싱합니다. 실제 키로 응답을 확인하며 보정이 필요할 수 있습니다.
- 시점 조회 시 법률 본문·3단 비교는 해당 시행 버전을 쓰지만, 감독규정·시행세칙과 판례는 현행 기준입니다.
- 이 앱의 결과는 참고용이며, 법적 판단에는 국가법령정보센터 원문을 확인하세요.
