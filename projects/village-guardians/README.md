# Village Guardians — 마을지키기

마인크래프트 생존 월드의 한 마을을 실제 공동체로 운영하고 지키는 NeoForge JAR 모드다.

## 0.1 통치 코어

- 첫 접속자가 임시 촌장이 된다.
- 촌장은 한 명만 존재한다.
- 촌장 직책과 실무 역할은 별개다.
- 멀티플레이어는 다음 역할 중 하나를 선택한다.
  - `guard_captain` 경비대장
  - `builder` 건축가
  - `quartermaster` 보급관
  - `scout` 정찰병
  - `steward` 농업관
  - `medic` 의무관
- 마을 전체 시간 진행은 촌장이 안건을 발의하고 온라인 플레이어 과반수가 찬성해야 실행된다.
- 자연적인 바닐라 시간 흐름은 꺼진다.
- 다른 명령이나 모드가 일주기를 다시 켜도 서버가 주기적으로 마을 시간을 복구한다.

## 명령어

```text
/vg status
/vg role <role>
/vg propose advance_time
/vg vote yes
/vg vote no
/vg mayor transfer <player>
```

`/vg mayor transfer`는 현재 촌장만 사용할 수 있다.

## 시간 규칙

월드 시간은 자동으로 흐르지 않는다.

```text
아침 → 낮 → 저녁 → 밤 → 다음 날 아침
```

`advance_time` 안건이 통과될 때만 다음 단계로 이동한다.

## 빌드

Java 25가 필요하다. 프로젝트의 부트스트랩 스크립트가 Gradle 9.2.1을 자동으로 내려받아 빌드한다.

Windows:

```bat
build.bat
```

Linux/macOS:

```bash
chmod +x build.sh
./build.sh
```

완성 JAR은 `build/libs/` 아래에 생성된다. 현재 저장소에는 0.1 소스 기반이 들어가며, 실제 Gradle 빌드와 게임 로딩 검증이 끝나기 전에는 배포 완료로 간주하지 않는다.

## 현재 제한

통치 상태는 아직 메모리에만 존재하므로 서버를 완전히 재시작하면 촌장, 역할, 마을 날짜가 초기화된다. 다음 단계에서 NeoForge의 월드 저장 데이터로 영구 보존한다.
