# 초성레시피 ☕

한글 초성으로 음료 레시피를 빠르게 찾아볼 수 있는 Android 앱입니다.

엑셀 파일로 레시피 데이터를 관리하고, 앱에서 초성 버튼을 눌러 원하는 메뉴를 바로 검색할 수 있습니다.

## 스크린샷

| 메인 화면 | 레시피 상세 |
|:-:|:-:|
| 초성 버튼으로 메뉴 탐색 | ICE/HOT 구분, 메모 기능 |

## 주요 기능

- **초성 검색** — ㄱ~ㅎ 버튼을 눌러 해당 초성으로 시작하는 레시피 목록을 확인
- **ICE / HOT 탭** — 음료 타입별로 레시피를 분류해서 볼 수 있음
- **엑셀 가져오기 / 내보내기** — `.xlsx` 파일로 레시피 데이터를 한 번에 관리 (새 파일 가져오면 기존 데이터는 자동 교체)
- **레시피 편집 & 삭제** — 상세 화면에서 바로 수정하거나 삭제 가능
- **메모 기능** — 레시피마다 개인 메모를 남길 수 있음

## 기술 스택

| 항목 | 사용 기술 |
|---|---|
| 언어 | Kotlin |
| UI | Material Design 3, ConstraintLayout |
| DB | Room (SQLite) |
| 엑셀 처리 | Apache POI |
| 최소 SDK | Android 8.0 (API 26) |

## 빌드 & 실행

```bash
# 프로젝트 클론
git clone https://github.com/kyop21/recipeProject.git

# Android Studio에서 열고 Gradle Sync 후 실행
```

## 라이선스

MIT License
