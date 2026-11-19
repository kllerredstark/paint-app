# 작품 보관함 (ArtVault) — Android

그림 작품 사진을 모아 보관하는 안드로이드 앱입니다. Kotlin + Jetpack Compose.

## 기능
- 사진 선택기로 작품 여러 장 한 번에 추가 (권한 요청 없음)
- 다른 앱(갤러리, 카메라 등)에서 **공유 → 작품 보관함**으로 바로 추가
- 제목 · 작가 · 제작일 · 앨범 · 태그 · 메모 기록
- 벽돌형(스태거드) 갤러리 — 폰 2열, 태블릿 3~5열 자동
- 검색, 즐겨찾기 / 앨범별 필터, 정렬(최근 추가 · 제작일 · 제목)
- 전체 화면 뷰어: 좌우로 넘기기, 핀치·두 번 탭 확대, 탭하면 정보 숨김
- 편집 · 삭제, 다크 모드

## 저장 방식
- 모든 작품은 **휴대폰 안(앱 전용 저장소)**에 저장됩니다. 인터넷 필요 없음.
- 원본은 긴 변 2400px JPEG로 줄여 저장하고, 목록용 썸네일(640px)을 따로 만듭니다.
- ⚠️ 앱을 삭제하면 보관한 작품도 함께 지워집니다.

## 빌드 방법

### A. Android Studio (권장)
1. Android Studio (Ladybug 이상) 설치
2. `File → Open` → 이 `ArtVault` 폴더 선택 → Gradle 동기화 기다리기
3. 휴대폰에서 **개발자 옵션 → USB 디버깅** 켜고 USB 연결
4. 상단 ▶ Run 누르면 폰에 설치·실행

APK 파일만 뽑으려면: `Build → Build App Bundle(s) / APK(s) → Build APK(s)`
→ `app/build/outputs/apk/debug/app-debug.apk`

### B. GitHub Actions (PC에 아무것도 안 깔고)
1. GitHub에 새 저장소를 만들고 이 폴더 전체를 올림
2. 저장소의 **Actions** 탭 → `Build APK` 실행 완료 기다리기 (약 5분)
3. 실행 결과 하단 **Artifacts → ArtVault-apk** 다운로드 → 압축 풀어 폰으로 옮겨 설치
   (설치 시 '출처를 알 수 없는 앱 허용' 필요)

## 구조
```
app/src/main/java/com/redstars/artvault/
├─ MainActivity.kt            공유 인텐트 처리, 화면 시작
├─ data/Artwork.kt            작품 데이터 모델
├─ data/ArtworkRepository.kt  이미지 축소·저장, JSON 목록 관리
└─ ui/
   ├─ GalleryViewModel.kt     검색·필터·정렬, 추가/편집/삭제
   ├─ GalleryScreen.kt        메인 갤러리 화면
   ├─ ViewerScreen.kt         전체 화면 뷰어(확대/넘기기)
   ├─ Sheets.kt               추가·편집 시트, 삭제 확인
   └─ Theme.kt                색상 테마
```

## 버전
AGP 8.7.3 · Kotlin 2.0.21 · Gradle 8.10.2 · compileSdk 35 · minSdk 28 (Android 9 이상)
