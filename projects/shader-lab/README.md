# Shader Lab 0.1.0-alpha.1

Minecraft Java 26.2 + NeoForge용 클라이언트 전용 JAR 쉐이더 실험이다. 배포용 모드가
아니며, 26.2의 실제 post-effect 파이프라인이 사용자 환경에서 정상 동작하는지 확인하는
첫 번째 기능 테스트다.

## 현재 구현

- 월드 진입 후 `shaderlab:lush_grade` 자동 적용
- GLSL 330 기반 단일 색보정 패스
- 하이라이트 압축, 약한 대비·채도 향상, 따뜻한 밝은 영역과 차가운 그림자
- 기존에 다른 post effect가 활성화되어 있으면 덮어쓰지 않음
- 월드 저장 데이터, 아이템, 블록, 네트워크 패킷 없음
- 전용 서버에서는 모드 진입점 자체가 로드되지 않음

이 버전은 진짜 GPU 후처리 쉐이더지만, 아직 그림자 맵·전역 조명·반사·볼류메트릭
구름을 구현하는 완전한 쉐이더 로더는 아니다. 먼저 JAR 내부 리소스 로딩과 26.2
렌더링 호환성을 검증하기 위한 단계다.

## 테스트 환경

- Minecraft Java 26.2
- Java 25
- NeoForge 26.2.0.38-beta
- 첫 테스트에서는 Iris/OptiFine/Oculus 및 다른 쉐이더 모드를 빼는 것을 권장
- e4mc는 함께 있어도 렌더링 실험에는 관여하지 않음

## 게임에서 확인

1. GitHub Actions의 `shader-lab-0.1.0-alpha.1-deliverables` artifact에서 JAR을 받는다.
2. Modrinth 인스턴스의 `mods` 폴더에 JAR을 넣는다.
3. 게임을 실행하고 월드에 들어간다.
4. 바닐라보다 색이 약간 풍부하고 밝은 영역이 부드러워지는지 확인한다.
5. 검은 화면, 보라색 화면, 즉시 크래시가 나면 `latest.log`를 보존한다.

효과는 월드 진입 때 한 번만 적용된다. 관전자 시점 등 바닐라가 자체 post effect를
사용하면 그 효과를 우선하며, Shader Lab은 같은 월드에서 강제로 다시 덮어쓰지 않는다.

## 로컬 빌드

Gradle 9.2.1과 Java 25가 준비된 환경에서:

```bash
gradle wrapper --gradle-version 9.2.1 --distribution-type bin
./gradlew --no-daemon clean build --stacktrace
python3 scripts/verify_jar.py build/libs/shaderlab-0.1.0-alpha.1.jar
```

Windows에서는 두 번째 명령부터 `gradlew.bat`를 사용한다.

생성물:

```text
build/libs/shaderlab-0.1.0-alpha.1.jar
build/libs/shaderlab-0.1.0-alpha.1.jar.sha256
```
