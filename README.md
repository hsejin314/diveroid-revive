# Diveroid Revive

**다이브로이드 방수 케이스용 비공식 카메라 앱.**

**An unofficial camera app for the Diveroid underwater phone housing.**

[한국어](#한국어) · [English](#english)

> Diveroid 는 해당 권리자의 상표입니다. 이 프로젝트는 Diveroid 와 무관한 개인 프로젝트이며, 케이스의 물리 버튼만 사용하고 Diveroid 센서나 앱과는 통신하지 않습니다.
>
> Diveroid is a trademark of its respective owner. This project is not affiliated with or endorsed by Diveroid. It only works with the housing's physical buttons; it does not talk to the Diveroid sensor or app.

---

## 한국어

### 만든 이유

다이브로이드 케이스에는 교체가 안 되는 배터리가 든 작은 다이브 컴퓨터 센서가 있습니다. 그 배터리가 다 되면 공식 앱은 아예 쓸 수 없게 막혀 있습니다. 케이스와 물리 버튼 세 개는 멀쩡한데, 그냥 두기는 그래서 만들어 봤습니다.

원래 앱의 3버튼 조작법은 그대로 두고, 기능을 조금 보정했습니다.

절전모드는 OLED 동작특성을 고려햐여 진짜 off 되도록 설정했습니다. 오래 실행한 경우의 에러도 줄여 awake의 성능도 향상되었습니다.

### 버튼

케이스 버튼은 플런저가 케이스 너머로 터치스크린을 누르는 방식입니다. 앱은 그 터치를 화면 오른쪽 가장자리의 세 구역으로 읽습니다. 블루투스도, 센서도, 캘리브레이션도 없습니다.

| 버튼 | 짧게 | 길게(0.7초) |
|---|---|---|
| 위 | 렌즈 셀렉터 (초광각 → 광각 → 줌 → 셀피) | **절전모드** |
| 중간 | 선택 확정 / **셔터** | 동영상 시작·정지 |
| 아래 | 중앙 초점 | **수중모드** (색보정) |

절전 중엔 아무 버튼으로 복귀합니다.

### 기능

- **진짜 절전**: 완전 검정 화면(OLED) + 카메라 해제. 아무 버튼으로 복귀. 조작 없이 1~2분 지나면 자동 진입(녹화 중 제외).
- **수중모드**: 카메라 ISP의 화이트밸런스 게인을 조정해 빨강을 되살립니다. 미리보기·사진·영상에 동일 적용. 강도 3단계. 켜고 끌 때 2초 와이프로 전/후를 보여줌.
- **다이빙 기록**: 시작/종료 시각, 총 시간, 시작·종료 GPS. 제목은 가까운 지명(영어)이 자동 입력. 편집·삭제·지도 열기.
- **화면비** 16:9 / 16:10 / 4:3 — 미리보기에 보이는 영역이 그대로 저장. 영상 FHD/4K, 사진은 항상 최대.
- **HUD**: 시각·배터리·다이브 시간·렌즈·화면비·수중 상태·REC. 버튼을 피해 왼쪽에만.
- **절대 안 잠김**: 다이빙 내내 화면 켜짐 유지, 잠금화면 위에 표시. 종료는 플런저가 닿지 않는 왼쪽 아래를 길게.
- **안 죽음**: 카메라 오류 시 자동 재연결. 진행 중 다이빙은 앱을 다시 켜도 이어짐.
- **언어**: English · 한국어 · 日本語 · 中文.

### 테스트 기기

Galaxy S23 FE (Android 14). 케이스에 들어가는 Android 11 이상 폰이면 동작할 겁니다(버튼 구역은 화면 비율 기준). 수동 화이트밸런스는 카메라가 서드파티 앱에 `MANUAL_POST_PROCESSING`을 열어줘야 하며 S23 FE는 됩니다. 안 되는 기기는 WB 프리셋으로 대체되고 화면에 "간이"라고 표시됩니다.

### 설치

[Releases](../../releases)에서 최신 APK를 받아 폰으로 복사 → 탭 → 출처를 알 수 없는 앱 허용.

첫 실행: **다이빙 시작** → 위치 권한(선택, 기록용) → 카메라 권한. 버튼이 오른쪽에 오도록 케이스에 넣습니다. 화면이 뒤집혀 보이면 시작 화면의 **화면 방향**을 바꾸세요.

### 빌드

Android Studio 없이 됩니다. JDK 17, Android SDK platform 35, Gradle 8.7. 아래 영어 섹션의 표와 명령 참고.

---

## English

### Why this exists

The Diveroid housing has a small dive-computer sensor with a non-replaceable battery. When that battery dies, the official app refuses to work — even though the housing itself, and its three physical buttons, are perfectly fine. You are left with a waterproof case you can't shoot with.

Diveroid Revive brings the housing back to life as a pure camera. It keeps the same three-button workflow as the original app and adds the things that were missing: real power saving, a dive timer with GPS, and underwater color correction that applies to photos and video, not just the preview.

### How the buttons work

The housing's buttons are plungers that press the phone's touchscreen through the case. The app reads those touches as three zones on the right edge of the screen — no Bluetooth, no sensor, no calibration.

| Button | Press | Hold (0.7 s) |
|---|---|---|
| Top | Lens selector (ultra-wide → wide → zoom → selfie) | **Power save** |
| Middle | Confirm selection / **Shutter** | Start / stop video |
| Bottom | Center focus | **Underwater mode** (color correction) |

Any button wakes the phone from power save.

### Features

- **Power save that actually saves power**: pure black screen (OLED), camera released, wakes on any button. Auto-enters after 1 or 2 minutes of no input (not while recording).
- **Underwater mode**: adjusts the camera ISP's white balance gains to recover reds. Applies identically to preview, photos and video. Three strengths. On/off reveals the change with a 2-second left-to-right wipe.
- **Dive log**: start/end time, duration, start and end GPS. The title defaults to the nearest place name in English. Edit, delete, open in a map.
- **Aspect ratio** 16:9 / 16:10 / 4:3 — what you see in the preview is exactly what is saved. Video in FHD or 4K. Photos always at full resolution.
- **HUD**: time, battery, dive timer, lens, aspect, underwater state, REC timer — on the left, away from the buttons.
- **Never locks**: the screen stays on and stays above the lock screen for the whole dive. Exit is a hold-only control at bottom-left, out of reach of the plungers.
- **Crash-tolerant**: camera errors trigger an automatic rebind; an in-progress dive survives an app restart.
- **Languages**: English, 한국어, 日本語, 中文.

### Tested on

Galaxy S23 FE (Android 14). Should work on any Android 11+ phone that fits the housing; the button zones are proportional to screen size. Manual white-balance gains require a camera that exposes `MANUAL_POST_PROCESSING` to third-party apps — the S23 FE does. If yours doesn't, the app falls back to a white-balance preset and says so on screen.

### Install

Download the latest APK from [Releases](../../releases), copy it to your phone, tap it, allow installation from unknown sources.

First run: tap **Start dive**, grant location (optional, for the dive log) and camera. Put the phone in the housing with the buttons on the right. If the screen appears upside down, change **Screen orientation** on the start screen.

### Build

No Android Studio required. JDK 17, Android SDK platform 35, Gradle 8.7.

```bash
export JAVA_HOME=/path/to/jdk-17
gradle assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

| Component | Version |
|---|---|
| AGP | 8.6.1 |
| Kotlin | 2.2.21 |
| CameraX | 1.5.3 |
| compileSdk / targetSdk | 35 |
| minSdk | 30 |

On Windows, `local.properties` must use forward slashes (`sdk.dir=C:/path/to/sdk`) and `gradle.properties` disables Gradle's JDK auto-detection, which otherwise fails on some setups.

### Layout

```
app/src/main/java/com/sejin/diveroidrevive/
  MainActivity.kt        Start screen: settings + current dive + last dive
  CameraActivity.kt      Camera, button mapping, HUD, power save, recording, exit
  ButtonDetector.kt      Touch → zone (top/mid/bottom) + press/hold; fires hold once even if stuck
  UnderwaterWb.kt        Underwater color: Camera2 interop, ISP WB gains
  LogListActivity.kt     Dive log list
  DiveDetailActivity.kt  Dive log detail: edit title/memo, delete, map
  DiveLog.kt             Dive record + dives.jsonl
  Gps.kt                 One-shot location + reverse geocoding
  Segmented.kt           Segmented control for the start screen
  Prefs.kt               Settings
  Fullscreen.kt          Immersive mode, keep-screen-on, orientation lock
res/font/                Chakra Petch, Rajdhani (SIL OFL — see FONTS-LICENSE.txt)
design/                  UI direction sketches (A chosen; B and C kept for reference)
```

### License

MIT. See [LICENSE](LICENSE).
