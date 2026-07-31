# Shader Lab 0.2.0-alpha.2

Minecraft Java 26.2 + NeoForge용 클라이언트 전용 JAR 쉐이더 실험이다. 배포용 모드가
아니며, 모드 JAR 하나만으로 화면 전체에 강한 시네마틱 후처리를 적용하는 기능 테스트다.

## 0.2의 목표

0.1은 파이프라인 호환성을 확인하기 위한 약한 색보정에 가까워서, 전후 차이가 거의
느껴지지 않았다. 0.2는 월드에 들어가는 즉시 화면이 확실히 달라 보이도록 설계했다.

- 햇빛, 하늘 경계, 횃불과 밝은 블록 주변에 넓게 번지는 다단계 블룸
- 차가운 청록·보랏빛 그림자와 따뜻한 금빛 하이라이트
- 진주광택처럼 색이 섞이는 광휘 합성
- ACES 계열 시네마틱 톤매핑과 강한 명암·색감 보정
- 화면 가장자리의 매우 약한 색수차와 비네트
- 26.2 역방향 깊이 버퍼를 이용한 약한 원거리 대기감
- 흔들림, 모션 블러, 잔상은 사용하지 않음

## 렌더링 구조

`shaderlab:lush_grade`는 총 7개 패스로 동작한다.

1. 밝은 픽셀 추출
2. 좁은 가로 광휘 확산
3. 좁은 세로 광휘 확산
4. 넓은 가로 광휘 확산
5. 넓은 세로 광휘 확산
6. 원본·광휘·깊이 버퍼 합성 및 톤매핑
7. 최종 화면 출력

기존에 다른 post effect가 활성화되어 있으면 강제로 덮어쓰지 않는다. 월드 저장 데이터,
아이템, 블록, 네트워크 패킷은 없으며 전용 서버에서는 모드 진입점 자체가 로드되지 않는다.

## 테스트 환경

- Minecraft Java 26.2
- Java 25
- NeoForge 26.2.0.40-beta
- NVIDIA GTX 1660 SUPER 및 OpenGL 3.3 환경을 우선 기준으로 함
- Iris, OptiFine, Oculus 및 다른 쉐이더 모드는 첫 테스트에서 제외
- e4mc는 함께 있어도 렌더링 효과에 관여하지 않음

## 게임에서 확인

1. 기존 `shaderlab-0.1.0-alpha.1.jar`를 삭제한다.
2. `shaderlab-0.2.0-alpha.2.jar`를 Modrinth 인스턴스의 `mods` 폴더에 넣는다.
3. 게임을 완전히 종료했다가 다시 실행한다.
4. 낮의 하늘, 해 뜨는 방향, 물가, 횃불이 있는 밤, 네더의 밝은 블록을 확인한다.
5. 밝은 부분 주변 광휘와 전체 색감이 한눈에 달라져야 정상이다.

검은 화면, 보라색 화면, 셰이더 컴파일 오류, 지나친 과노출이 발생하면 `latest.log`와
문제가 보이는 스크린샷을 보존한다. 0.2는 시각적 차이를 크게 만드는 실험판이므로,
화면이 지나치게 밝거나 무거우면 다음 버전에서 강도와 성능 프리셋을 분리한다.

## 로컬 빌드

Gradle 9.2.1과 Java 25가 준비된 환경에서:

```bash
gradle wrapper --gradle-version 9.2.1 --distribution-type bin
./gradlew --no-daemon clean build --stacktrace
python3 scripts/verify_jar.py build/libs/shaderlab-0.2.0-alpha.2.jar
```

Windows에서는 두 번째 명령부터 `gradlew.bat`를 사용한다.

생성물:

```text
build/libs/shaderlab-0.2.0-alpha.2.jar
build/libs/shaderlab-0.2.0-alpha.2.jar.sha256
```
