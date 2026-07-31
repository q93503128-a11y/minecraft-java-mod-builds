# Shader Lab Reverie 0.6.0-alpha.6

Minecraft Java 26.2 + NeoForge용 고품질 클라이언트 쉐이더 모드다.

## 0.5에서 변화가 보이지 않았던 이유

0.5의 Shader Lab JAR는 쉐이더팩만 설치했고 실제 렌더러인 Iris와 Sodium은 별도 JAR였다.
Shader Lab JAR만 설치한 환경에서는 두 모드가 로드되지 않아 쉐이더가 비활성 상태였다.
0.6은 Shader Lab, Iris, Sodium, Reverie 쉐이더팩을 하나의 JAR에 넣는다.

## 기반

- Noble Shaders 1.9.6
- 공식 Modrinth 버전 ID `3cIADbit`
- GPL-3.0-only
- Iris 1.11.2 NeoForge
- Sodium 0.9.1 NeoForge

원본 Noble ZIP, Iris JAR, Sodium JAR는 공식 Modrinth 파일의 SHA-512와 대조한다.
Noble의 GPLv3 원문, 파생 소스, 원작자 표시와 변경 내역을 쉐이더팩 안에 보존한다.

## Reverie 프리셋

### 현실성

- POM 활성화 및 64 레이어
- 4096 그림자 맵, 256블록 그림자 거리
- 반사·굴절 품질 강화
- 현실적인 태양 크기와 대기 산란
- 물결, 수면 노멀, 카우스틱, 수중 안개와 물 패럴랙스 강화
- 따뜻한 2800K 블록 조명

### 몽롱함

- Y=66 부근에 낮게 형성되는 볼류메트릭 안개
- 안개 두께를 25블록으로 제한해 화면 전체를 뿌옇게 만들지 않음
- 약한 블룸과 절제된 글레어
- 전 화면 심도 효과와 비네트는 비활성화
- 구형 청록색 화면 오버레이는 포함하지 않음

## 설치

기존 `shaderlab` JAR를 삭제하고 `shaderlab-0.6.0-alpha.6.jar` 하나만 `mods` 폴더에 넣는다.
실행 시 `ShaderLab-Reverie-0.6.zip`을 설치하고 Iris에서 자동 선택한다. 구형 Dreamscape
0.4와 0.5 쉐이더팩은 자동 삭제한다.

## 검증

- Java 25 + NeoForge 26.2.0.40-beta clean build
- Noble 1.9.6 SHA-512 및 GPLv3 검증
- Reverie 프리셋 핵심 값 검증
- 최종 JAR 내부 Noble 쉐이더팩 검증
- 최종 JAR 내부 Iris·Sodium NeoForge JAR 검증
- 중첩 JAR와 중첩 쉐이더팩 ZIP 무결성 검사
- 구형 화면 공간 재질 추정 후처리 부재 확인

## 로컬 빌드

```bash
gradle wrapper --gradle-version 9.2.1 --distribution-type bin
./gradlew --no-daemon clean build --stacktrace
python3 scripts/verify_jar.py build/libs/shaderlab-0.6.0-alpha.6.jar
```
