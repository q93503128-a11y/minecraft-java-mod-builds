# Countryside Days — 시골식당

폐기된 옛 프로젝트와 무관하게 2026-07-31부터 새로 시작한 Minecraft Java Edition용 NeoForge 힐링 생활 모드다.

## 정체성

이 게임은 식당 타이쿤이 아니다. 플레이어는 넓고 현실적인 시골 생활권에서 농사, 채집, 낚시, 동물 돌보기, 주민 관계, 배달, 집과 식당 꾸미기, 산책과 탐험을 자유롭게 즐긴다. 식당은 이 활동에서 얻은 재료와 주민 이야기가 모이는 중심 거점이다.

## 현재 단계

`0.1.0-alpha.2` 기반 구현 단계다.

현재 소스에 포함된 것:

- 공식 NeoForge 26.2 + Java 25 + ModDevGradle 프로젝트 골격
- `countrysidedays` 모드 진입점
- 시골 주방 작업대 블록
- 들나물, 민물고기, 시골 전골, 요리 수첩
- 전용 크리에이티브 탭
- 한국어·영어 번역
- 블록·아이템 모델 연결
- 조리 재료 태그
- JAR 내부 검증 도구

아직 완료되지 않은 것:

- 실제 Java 25 CI 빌드 결과 확인
- 월드 저장 데이터와 시골 생활권 생성
- 재료 수집·손질·조리·제공의 상호작용 루프
- 주민 일정과 관계
- 환경 렌더링과 전용 UI
- 서버·클라이언트 실행 검증

실제 Gradle 빌드와 JAR 검증이 성공하기 전에는 배포 완료 상태가 아니다.

## 문서

- [`PROJECT.md`](./PROJECT.md): 버전, 빌드, 호환성, 절대 규칙
- [`docs/GAME_VISION.md`](./docs/GAME_VISION.md): 월드와 플레이의 전체 방향

## 개발 빌드

Java 25와 Gradle 9.2.1이 필요하다.

```bash
gradle clean build
python3 tools/verify_jar.py build/libs/countrysidedays-0.1.0-alpha.2.jar
```

저장소 CI는 Gradle 9.2.1을 설치한 뒤 동일한 빌드와 JAR 검사를 수행한다. Gradle Wrapper는 최초 성공 빌드 이후 공식 생성물을 저장소에 고정한다.

## 바로 다음 구현 순서

1. CI에서 실제 `clean build` 성공
2. 공식 Gradle Wrapper 고정
3. 월드 저장 데이터와 시골 생활권 상태 모델
4. 시작 거점과 식당 기준점 생성
5. 첫 재료 수집·손질·조리·제공 사이클
6. 환경 연출과 첫 전용 UI
