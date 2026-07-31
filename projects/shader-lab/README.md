# Shader Lab Dreamscape 0.4.0-alpha.4

Minecraft Java 26.2 + NeoForge용 비공개 고품질 쉐이더 테스트다.

## 왜 0.3을 폐기했는가

0.3은 완성된 화면의 색상과 깊이만 보고 물·오로라를 추정했다. 그 결과 나뭇잎과 지형을
물이나 하늘로 오인해 화면 전체에 청록색 띠가 생겼다. 0.4에서는 `lush_grade` 후처리와
관련 GLSL·JSON을 전부 삭제했다.

## 0.4 구조

0.4는 Iris의 실제 월드 렌더링 파이프라인을 사용한다.

- 물 전용 프로그램: 물결, 굴절, 카우스틱, 거품, 화면 공간 반사
- 지형 전용 프로그램: 부드러운 그림자, SSAO, 흔들리는 식생, PBR 자동 노멀
- 실제 하늘 프로그램: 물리 하늘, 볼류메트릭 구름, 밤 오로라
- 카메라 단계: 자동 노출, FXAA, 블룸, 채도 조절

물과 지형이 서로 다른 프로그램에서 렌더링되므로 나무와 풀밭에 물 효과가 붙지 않는다.
오로라도 화면 위에 고정된 띠가 아니라 실제 하늘 프로그램에서 처리된다.

## 기반 쉐이더와 라이선스

- 기반: Sarp Shaders 1.0.0
- 제작자: xsoras / Sarp
- 공식 Modrinth 프로젝트: `sarp`
- 고정 버전 ID: `AwTfcPdR`
- 라이선스: MIT

빌드는 Modrinth API에서 프로젝트·버전·Minecraft 26.2·Iris 지원·SHA-512를 확인한다.
원본 ZIP에 MIT 라이선스 파일이 없거나 라이선스 원문이 다르면 즉시 실패하며 재배포하지
않는다. 초기 0.4 감사 빌드는 Sarp 원본을 수정하지 않고 포함하고, 파일 구조와 실제 설정
이름을 보고서로 남긴다.

## 설치 방식

Shader Lab JAR에는 `ShaderLab-Dreamscape-0.4.zip`이 포함된다. 실행 시:

1. 게임 폴더의 `shaderpacks`에 쉐이더팩을 설치한다.
2. 기존 `config/iris.properties`를 한 번 백업한다.
3. Dreamscape 쉐이더팩을 선택하고 쉐이더를 활성화한다.

실제 렌더링에는 Minecraft 26.2용 Iris 1.11.2 NeoForge와 Sodium 0.9.1 NeoForge가
필요하다. GitHub Actions는 Shader Lab·Iris·Sodium JAR 세 개가 들어 있는 테스트 키트를
생성한다.

## 현재 검증 범위

- Java 25 + NeoForge 26.2.0.40-beta clean build
- 공식 Sarp Modrinth 버전 및 SHA-512 확인
- 원본 MIT 라이선스 보존
- 실제 물·지형 프로그램 분리 확인
- 오로라·카우스틱·굴절·안개 기능 소스 확인
- 구형 화면 공간 후처리 부재 확인
- 중첩 쉐이더팩 ZIP과 최종 JAR 무결성 검사
- 공식 Iris·Sodium 26.2 JAR 확보

## 다음 단계

첫 감사 빌드의 소스 보고서에서 Sarp의 안개와 설정 구조를 확인한 뒤, 해수면 부근에만
깔리는 낮은 월드 공간 안개와 Dreamscape 색감 프리셋을 정확한 프로그램에 추가한다.
화면 전체 블러나 색상 기반 물 마스크는 다시 사용하지 않는다.

## 로컬 빌드

```bash
gradle wrapper --gradle-version 9.2.1 --distribution-type bin
./gradlew --no-daemon clean build --stacktrace
python3 scripts/verify_jar.py build/libs/shaderlab-0.4.0-alpha.4.jar
```

빌드에는 Modrinth에서 고정 Sarp 버전을 받기 위한 인터넷 연결이 필요하다.
