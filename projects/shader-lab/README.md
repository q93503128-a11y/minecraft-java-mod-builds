# Shader Lab Dreamscape 0.5.0-alpha.5

Minecraft Java 26.2 + NeoForge용 비공개 고품질 쉐이더 테스트다.

## 0.3 실패 원인과 폐기

0.3은 완성된 화면의 색상과 깊이만 보고 물·오로라를 추정했다. 그 결과 나뭇잎과 지형을
물이나 하늘로 오인해 청록색 띠가 생겼다. `lush_grade` 후처리와 관련 GLSL·JSON은 전부
삭제했으며 0.5에는 해당 구조가 없다.

## 0.5 렌더링 구조

Iris의 실제 월드 렌더링 프로그램을 사용한다.

- `gbuffers_water`: 물만 파도·굴절·거품·카우스틱·SSR·수심 흡수 처리
- `gbuffers_terrain`: 지형만 PBR·자동 노멀·SSAO·부드러운 그림자 처리
- 실제 하늘 프로그램: 별·물리 하늘·볼류메트릭 구름·밤 오로라 처리
- `atmosphere.glsl`: 월드 좌표와 고도·거리·하늘 접근도로 낮은 지표 안개 처리
- 카메라 단계: 자동 노출·FXAA·약한 블룸, 비네트 제거

물과 지형이 서로 다른 프로그램을 사용하므로 나무와 풀밭에 물 효과가 붙지 않는다.
오로라도 화면 중앙에 붙는 띠가 아니라 실제 밤하늘 방향에 생성된다.

## Dreamscape 기본 프리셋

### 물

- 굴절 1.25
- 카우스틱 1.5
- 파도 1.25
- 투명도 0.55
- 해안 거품 0.75
- SSR 32스텝
- 청록·맑은 시안 수색과 수심 흡수

### 지형과 블록

- 통합 PBR 모드 2
- 자동 노멀 강도 1.5
- SSAO 강도 1.25
- 그림자 부드러움 1.5
- 흔들리는 풀과 나뭇잎

### 하늘과 안개

- 청록·보라·분홍 다층 오로라 커튼
- 해수면 부근 Y=65를 중심으로 낮게 깔리는 월드 공간 안개
- 카메라 바로 앞은 가리고 10~42블록 이후부터 안개 형성
- 하늘 접근도가 낮은 동굴에서는 저층 안개 억제
- 볼류메트릭 라이트 24샘플

### 화면

- FXAA 활성화
- 자동 노출 유지
- 블룸 0.25로 억제
- 채도 1.05
- 비네트 제거

## 기반과 라이선스

- 기반: Sarp Shaders 1.0.0
- 원작자: xsoras / Sarp
- 공식 Modrinth 프로젝트: `sarp`
- 고정 버전 ID: `AwTfcPdR`
- 공식 프로젝트 라이선스: MIT

빌드는 공식 Modrinth 메타데이터와 원본 ZIP SHA-512를 확인한다. 원본 ZIP에는 라이선스
파일이 없으므로 그 사실을 감사 보고서에 기록하고, 공식 MIT 선언을 근거로 정식 MIT
고지문과 메타데이터 증빙 JSON을 파생 쉐이더팩에 추가한다. 원작자와 변경 내역도 함께
보존한다.

## 설치

테스트 키트의 `mods` 폴더에 있는 세 JAR를 인스턴스 `mods` 폴더에 넣는다.

- Shader Lab Dreamscape 0.5
- Iris 1.11.2 NeoForge
- Sodium 0.9.1 NeoForge

Shader Lab은 `ShaderLab-Dreamscape-0.5.zip`을 `shaderpacks` 폴더에 설치하고 Iris에서
자동 선택한다. 구형 0.4 쉐이더팩은 자동 삭제한다. 첫 실행에서 Iris가 설정을 먼저 읽은
경우 한 번 재시작한다.

## 검증

- Java 25 + NeoForge 26.2.0.40-beta clean build
- 공식 Sarp 버전·Minecraft 26.2·Iris 지원·SHA-512 확인
- 라이선스 증빙과 MIT 고지문 보존
- 실제 물·지형 프로그램 분리 확인
- 물 프리셋과 지형 PBR 프리셋 확인
- 실제 하늘 오로라와 월드 공간 저층 안개 패치 확인
- 구형 화면 공간 후처리 부재 확인
- 중첩 쉐이더팩 ZIP과 최종 JAR 무결성 검사
- 공식 Iris·Sodium 26.2 JAR 포함 테스트 키트 생성

## 로컬 빌드

```bash
gradle wrapper --gradle-version 9.2.1 --distribution-type bin
./gradlew --no-daemon clean build --stacktrace
python3 scripts/verify_jar.py build/libs/shaderlab-0.5.0-alpha.5.jar
```
