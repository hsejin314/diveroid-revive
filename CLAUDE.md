# Diveroid Revive — 다이브로이드 케이스용 대체 카메라 앱 (구 Dive_App)

> 이 프로젝트는 `H:\Claude\Dive_App` 에서 독립적으로 진행한다. 상위 폴더에 CLAUDE.md 를 두지 말 것.
> GitHub 공개 저장소 `diveroid-revive` (MIT). 패키지 `com.sejin.diveroidrevive`, 앱 라벨 "Diveroid Revive".
> README 는 공개용(영/한) — 사용자 노출 문서. 내부 메모는 이 파일에만.

## 배경
- 사용자는 **다이브로이드(Diveroid) 방수 케이스**에 폰을 넣어 수중 촬영. 센서(다이브컴퓨터) 배터리는 죽어서 **케이스만** 사용.
- **만든 이유(공식 서사)**: 센서 배터리가 다 되면 공식 앱이 아예 못 쓰게 막아놓음 → 멀쩡한 케이스를 되살린다.
- 기본 앱 불만: 중간에 에러 나면 다시 못 켬, 절전모드가 실질적 절전이 안 됨.
- 목표: 기본 앱의 버튼 조작 UX를 그대로 살린 **가볍고 안 죽는** 카메라 앱.

## 하드웨어 사실 (사용자 확인, 2026-09-07)
- **버튼 = 물리 플런저**. 케이스 오른쪽(가로 기준) 푸시버튼 3개가 안쪽 **터치스크린을 직접 누른다.**
  → 앱 입장에선 **화면 우측 가장자리의 고정 좌표 터치 이벤트**. BLE·자석·센서 없음.
  → 위치: 우측 가장자리에서 약 1/5 지점. 위·아래는 같은 열, **중간만 조금 더 오른쪽**.
  → **캘리브레이션 없음** (케이스 닫으면 화면을 못 누르므로). 오른쪽 28% 세로 띠를 위/중간/아래 3밴드(38%/62% 경계)로 나눠 판정.
     기종별 차이는 비율 기반으로 흡수. 구역은 화면에 원으로 항상 표시되고 눌리면 강조 + 진동.
- 대상 폰: **Galaxy S23 FE, Galaxy S23 Ultra** (둘 다 OLED, 초광각/광각/망원, 4K). Android 14+.
- 알려진 quirk(원 앱 리뷰): 수심 압력으로 버튼이 **계속 눌린 상태**가 될 수 있음 → 롱프레스는 임계 도달 시 1회만 발화, 이후 릴리즈까지 무시.

## 기능 스펙 (확정)
### 1. 버튼 매핑 (원 앱과 동일)
| 버튼 | 짧게 | 길게 |
|---|---|---|
| 위 | 셀렉터 이동 (초광각 → 광각 → 줌 → 셀피 순환) | **절전모드** |
| 중간 | 선택 확정 / **셔터** | 동영상 촬영 시작·정지 |
| 아래 | 초점 (중앙 AF 트리거) | **수중모드** 토글 (청색 보정) |

### 2. 절전모드 (개선 포인트)
- 화면 **완전 검정만**(시계·버튼 아무것도 없음, 사용자 결정) + 밝기 최소 + 카메라 언바인드.
- **아무 터치**로 즉시 복귀 → 카메라 재바인드. 화면은 절대 잠기지 않게(`FLAG_KEEP_SCREEN_ON` + `setShowWhenLocked/TurnScreenOn`).
- 검정 오버레이는 `elevation=24dp` — Material Button 의 elevation 보다 위에 있어야 종료 버튼이 뚫고 나오지 않음.
- **진동 피드백 없음** (촬영 흔들림). 버튼 피드백은 구역 원 강조만.
- **자동 절전**: 녹화 중 아니고 마지막 버튼 조작 후 N분(설정: 1/2/끔, 기본 2) 지나면 진입.

### 3. 저장
- 사진/영상은 **MediaStore → 기본 갤러리(DCIM)** 에 바로 저장. 자체 앨범 UI 없음.

### 4. 촬영 화면 HUD (왼쪽 세로, 오른쪽 28% 는 버튼 구역이라 비움)
- 현재 시각, 배터리 %, DIVE 경과시간, 렌즈(광각/초광각/줌/셀피), 수중 ON/OFF, REC 타이머.
- **종료 버튼**: 화면 왼쪽 아래, 길게 눌러야 동작 (케이스 열고 손가락으로). 뒤로가기 제스처는 무시.

### 5. 시작 화면 = 설정 + 기록
- 화면 방향: **자동(시작 시 방향으로 고정) / 폰 상단 왼쪽 / 폰 상단 오른쪽**. 가로 고정이 기본.
- 영상 해상도: **FHD / 4K** (사진은 항상 최대).
- 화면비: **16:10 / 16:9 / 4:3**. 4:3 은 4:3 스트림, 나머지는 16:9 스트림 + ViewPort 크롭(16:10). 프리뷰 뷰 자체를 그 비율로 레터박스해서 "보이는 것 = 저장되는 것".
- 기록 화면: 목록(탭=상세, 길게=삭제) → 상세(제목·메모 편집, 삭제, 지도 열기).
- 언어: English / 한국어 / 日本語 / 中文.
- 자동 절전 대기 시간.
- 진행 중 다이빙 표시(앱이 죽어도 이어짐) + 종료 버튼, 최근 다이빙 기록 10개.

### 6. 다이빙 로그
- 시작 = 카메라 첫 진입, 종료 = 종료 버튼. 시작/종료 시각, 총 시간.
- **시작 시 GPS**: `LocationManager.getCurrentLocation`(FUSED→GPS→NETWORK 순, Play Services 없음). 위치 권한은 시작 화면에서 미리 요청.
- 종료 시 GPS 는 best-effort: 기록을 먼저 쓰고 applicationContext 로 위치를 받아 마지막 줄 갱신.
- 저장은 앱 내부 파일 `dives.jsonl` — DB 없음.

### 7. 수중모드 (v0.3 구현 방식 — GL 셰이더 아님. S23 FE 에서 수동 게인 동작 확인됨)
- ON/OFF 전환 시 직전 프레임을 캡처해 덮어두고 왼쪽→오른쪽으로 걷어내는 와이프(0.8초)로 전/후를 보여줌.
- **카메라 ISP 화이트밸런스 게인 직접 조정** (`UnderwaterWb.kt`, Camera2 interop). 프리뷰·사진·영상에 동일 적용, GPU/전력 비용 0.
- AWB 자동 상태에서 캡처 결과의 `COLOR_CORRECTION_GAINS/TRANSFORM` 을 계속 받아두다가, ON 시 AWB OFF + 그 값 고정 + R 게인 ×(1.4/1.8/2.4), B 게인 ÷(1.10/1.20/1.35). 강도 약/중/강은 시작 화면 설정.
- 기기가 `MANUAL_POST_PROCESSING` 미지원이거나 자동값을 아직 못 받았으면 **AWB 프리셋(SHADE)** 로 간이 보정 (상태 문구에 "간이" 표시됨).
- 실기기에서 삼성이 서드파티 앱에 수동 게인을 허용하는지가 관건. 안 되면 대안은 GL `CameraEffect`(SurfaceProcessor) — 약 300줄, 아직 안 만듦.

## 기술 결정
- **Kotlin + View 시스템 + CameraX** (Compose 불필요). `PreviewView` + 전체화면 터치 리스너.
- 렌즈 전환: 단일 후면 논리 카메라의 `setZoomRatio` (초광각≈0.6, 광각 1.0, 줌 3.0) / 셀피 = 전면 카메라 selector.
- 위치: `LocationManager` (Play Services 의존성 추가 안 함).
- 카메라 에러 → 자동 재바인드(기본 앱의 "죽으면 못 켬" 문제 해결 포인트).
- 우측 가장자리 제스처 충돌 방지: 몰입모드 + `setSystemGestureExclusionRects`.

## 빌드 환경 (v0.1 에서 검증됨)
- 로컬 툴체인 `H:\Claude\toolchain\` (Android Studio 없음): JDK 17.0.19, Gradle 8.7, SDK platform 34+**35**, build-tools 34+**35**.
- **AGP 8.6.1 / Kotlin 2.2.21 / CameraX 1.5.3 / compileSdk·targetSdk 35 / minSdk 30.**
  - CameraX 1.5.x 는 AGP 8.6+ 요구. AGP 8.7+ 는 Gradle 8.9 필요하므로 올리려면 Gradle 도 함께.
  - CameraX 1.6.x 는 compose 모듈이 compileSdk 37 요구 → 당분간 1.5.x 유지.
- Windows 함정 두 개(이미 해결, 건드리지 말 것):
  - `gradle.properties` 의 `org.gradle.java.installations.auto-detect=false` — 없으면 JDK 자동 탐색이 IOException.
  - `local.properties` 의 `sdk.dir` 은 `H:/Claude/toolchain/sdk` 슬래시 표기.
- 빌드: `JAVA_HOME='H:\Claude\toolchain\jdk\jdk-17.0.19+10'` + `gradle-8.7/bin/gradle.bat assembleDebug --no-daemon` (약 1분)
- 실기기 테스트는 사람이 수동(케이스 장착 + 물속). 에이전트 몫은 코드·빌드·APK 까지.

## 진행 상태
- v0.1 (2026-09-07): 프리뷰·3버튼 매핑·사진/영상 갤러리 저장·절전·설정.
- v0.2 (2026-09-07): 사용자 피드백 반영 — 캘리브레이션 제거(고정 구역), 방향 설정, HUD, 종료 버튼, 자동 절전, 버튼 피드백(강조+진동), 절전 시계 이동, 다이빙 기록. `DiveApp-v0.2.apk`
- v0.3 (2026-09-07): 절전 = 순수 검정(시계 제거), 진동 제거, 잠금 위 표시, GPS 시작/종료 기록, **수중 색보정(ISP WB 게인)** + 강도 설정. **S23 FE 실기기에서 1~5 전부 정상 확인됨** (수동 게인 동작).
- v0.4 (2026-09-07): 화면비 수정(ResolutionSelector + Recorder aspect + 프리뷰 레터박스 + HUD 표시), 기록 목록/상세 화면(제목·메모 편집, 삭제, 지도), 수중모드 전환 와이프 애니메이션, 버튼 이름표, **4개 언어**(en/ko/ja/zh, AppCompat 앱별 로케일).
- v0.5 (2026-09-07): 와이프 2초, 영어 강도 라벨 단축(줄바꿈), 기록 제목 기본값 = 시작 지점 **영어 지명**(Geocoder, subLocality→locality→…), 종료 지명은 제목 비었을 때만. `DiveApp-v0.5.apk`
- v0.6 (2026-09-07): **UI 리디자인 = A안 "다이브 컴퓨터"** 적용 (사용자 선택). 콘티 원본은 `design/` (B·C안 포함).
- v0.6.1 (2026-09-07): 방향 행 줄바꿈 수정.
- v0.7.0 (2026-10-10): **이름 변경 DiveApp → Diveroid Revive** (패키지·테마·prefs·DCIM 폴더 전부). git init, MIT LICENSE, 공개 README(영/한). `DiveroidRevive-v0.7.0.apk`
  - 패키지가 바뀌어 기존 설치본과 별개 앱. 이전 기록(dives.jsonl)은 이전 앱에 남음.

## 디자인 시스템 (A · Dive Computer) — 새 UI 는 반드시 이 토큰으로
- 색 (`res/values/colors.xml`): `bg #05080C` · `panel #0B1218` · `border #3B5560` · `divider #1C2A32` · `accent #4FE3FF` · `accent_dim #8FD3E0` · `muted #4A6A75` · `text #E8F6FA` · `rec #FF5252`. 다른 색 추가 금지.
- 서체 (`res/font/`, OFL): 숫자·헤더 = **Chakra Petch** (bold/semibold), 라벨·본문 = **Rajdhani** (semibold/medium). 테마 기본 fontFamily 가 Rajdhani. CJK 는 시스템 폰트로 자동 폴백.
- 형태: **모서리 각짐(radius 0)**, 1dp 테두리, 라벨은 대문자 + 자간 0.15~0.25, 강조는 왼쪽 3dp 시안 바(`panel_accent`).
- 재사용 스타일 (`res/values/styles.xml`): `HudNumber` `HudLabel` `Chip` `ZoneLabel` `Section` `Segment` `Row` `RowLabel/RowValue` `OutlineButton` `DetailLabel/DetailValue` `DiveEdit`.
- 재사용 드로어블: `chip_dim` `chip_border` `chip_fill` `seg_border` `block_start` `panel_accent` `zone_idle` `zone_active` `focus_box(_active)` `rec_dot`.
- 세그먼트 컨트롤은 `Segmented.kt` 로 코드 생성 (라디오버튼 안 씀). 선택 = 시안 채움 + 검정 글자.
- Material Button 대신 `OutlineButton` 스타일 TextView (Button 의 elevation 이 절전 오버레이를 뚫는 문제 회피).
- **행(Row) 규칙**: 라벨은 짧게(한 단어급), 값은 `RowValue`(한 줄·말줄임). 긴 설명은 대화상자 제목으로. 값이 길면 라벨 폭이 0에 가까워져 한 글자씩 세로로 접힘 (v0.6.1 에서 겪음).

## 다국어 규칙
- 모든 사용자 노출 문자열은 `res/values*/strings.xml`. 기본(`values/`)은 영어, `values-ko`, `values-ja`, `values-zh`.
- 새 문자열 추가 시 4개 파일 모두에 추가. 하드코딩 금지.
- 언어 전환은 `AppCompatDelegate.setApplicationLocales` — 시스템 설정과 무관하게 앱 안에서 바뀜.

## 작업 스타일
- 한국어, 간결, actionable. 큰 변경 전 계획 먼저.
- ponytail 플러그인 활성(`full`). **권한·CameraX 바인딩·서비스 등 "빼면 안 도는" 보일러플레이트는 줄이지 말 것** — 필요하면 `/ponytail:ponytail lite`.
