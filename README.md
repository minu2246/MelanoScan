# MelanoScan

MelanoScan은 피부 병변 이미지를 기반으로 AI 분석을 수행하고, 병변 크기 변화를 기록할 수 있는 Android 앱입니다.

주요 기능은 다음과 같습니다.

- 피부 병변 이미지 AI 분류
- benign_nevus / melanoma / seborrheic_keratosis 3클래스 예측
- AI 병변 영역 시각화
- ABCD 보조해석 기반 병변 형태 비교 정보 제공
- 100원 동전 기준 병변 크기 측정
- 병변별 측정 기록 저장 및 최장축 변화 그래프 확인

> 본 앱의 AI 분석 결과와 ABCD 보조해석 결과는 의료진의 확정 진단을 대신하지 않습니다.  
> 사진 조건, 조명, 초점, ROI 선택 범위, mask 품질에 따라 결과가 달라질 수 있습니다.

---

## 개발 환경

- Android Studio
- Kotlin
- Gradle Wrapper 사용
- minSdk: 26
- Gradle JDK: Android Studio Embedded JDK 권장
- 실행 대상: Android Emulator 또는 실제 Android 기기

---

## 프로젝트 실행 방법

### 1. 저장소 복제

ZIP 다운로드보다 `git clone`을 권장합니다.

```powershell
git clone https://github.com/minu2246/MelanoScan
cd MelanoScan
.\gradlew.bat clean
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
