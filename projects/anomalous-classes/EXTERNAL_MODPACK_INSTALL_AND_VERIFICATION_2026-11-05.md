# 《이능 직업 서바이벌》 외부 모드팩 실제 적용·설치·검증 기록

> 2026-11-05 기준: **MC 1.21.1 / NeoForge 21.1.244 / Java 21**, 외부 기능 모드 9종·라이브러리 6종 고정. **게임 본체 클래스 구현은 아직 시작 전**.

## 1. 생성 및 검증된 산출물

- [최종 성공 CI #41989226541](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/41989226541) — [산출물 ZIP 받기](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/41989226541/artifacts/13397253687). Artifacts 보존기간 14일.
- 산출물 이름: **anomalous-classes-external-baseline-0.1.0.mrpack**. 외부 모드 JAR을 포함하지 않고 공식 Modrinth 배포 URL·SHA512·SHA1과 로더 정보를 지정한다.
- [설치 목록 및 정확한 Modrinth 버전 ID](./modpack/pack-source.json) / [재구성 및 JAR 검사 스크립트](./modpack/tools/build_external_pack.py) / [검증 워크플로](https://github.com/q93503128-a11y/minecraft-java-mod-builds/blob/main/.github/workflows/validate-anomalous-classes-external-pack.yml).
- **15/15 JAR SHA/ZIP/NeoForge 메타데이터/내부 Jar-in-Jar 검증 통과.** NeoForge 21.1.244 설치와 서버 필수 JAR 12/12 발견·새 월드 부팅·정상 종료 확인. In Control! 기본 areas.json 생성 확인, 초기 서버 ERROR 정정.

## 2. 모드팩 사용 방법

1. 위 GitHub Actions 산출물 ZIP을 내려받아 압축 해제한다(로그인 필요할 수 있음).
2. 그 안의 **anomalous-classes-external-baseline-0.1.0.mrpack**을 찾는다.
3. Modrinth App에서 모드팩/프로필 **가져오기(Import)**를 선택해 mrpack 파일을 지정한다.
4. Minecraft 1.21.1 / NeoForge 21.1.244가 적용되었고 15개 선정 파일이 설치되었는지 확인한다. 임의로 Fabric 버전을 섞지 않는다.
5. **JEI Ctrl+O**는 아이템 목록을 개인별 숨기는 토글이다. 이능 직업 자체는 아직 미구현이므로 클래스 스킬은 나타나지 않는다.

### 자신의 컴퓨터에서 재생성하는 방법

Python 3.12+와 인터넷이 필요하다. 작업 위치는 projects/anomalous-classes/modpack 이다.

~~~sh
python tools/build_external_pack.py --offline-check
python tools/build_external_pack.py --download-jars --output out
~~~

검증 후 out/ 안에 mrpack, RESOLVED_RELEASES.json, JAR_INSPECTION.json이 나온다. 외부 JAR은 일시 다운로드·검증한 뒤 삭제하며 git에 저장하지 않는다. **서버용** 필수 JAR을 별도 서버에 복사하려면 추가로 `--server-dir /your/test-server`를 지정한다. NeoForge 서버 설치는 별도로 수행한다.

## 3. 검증 수준

| 항목 | 결과 |
| --- | --- |
| 정확한 NeoForge/MC 버전의 외부 JAR 15종 | **VERIFIED** |
| 내장 Jar-in-Jar 모듈과 필수 modId | **VERIFIED** |
| .mrpack 패키지 생성 | **PRODUCED** |
| NeoForge 21.1.244 서버용 JAR 12종 탐색, 월드 시작/종료 | **SERVER SMOKE TESTED** |
| In Control! 기본 설정 폴더·areas.json 생성 | **VERIFIED** |
| 클라이언트 실제 메뉴/JEI/Locator/동적 조명·FPS | **NOT TESTED** |
| 외부 보스 실제 전투/던전 월드젠/전리품·멀티 접속 | **NOT TESTED** |
| 이능 직업 6종의 스킬·보스 지배/기생/군체·결계 호환 | **NOT IMPLEMENTED / NOT TESTED** |

### 실제 발견하여 수정한 항목

- LambDynamicLights NeoForge의 Modrinth 메타데이터에서 Fabric API가 의존성으로 잘못 나열된 문제: 특정 크로스로더 프로젝트 ID만 예외, **실제 내장 모듈은 별도 검사하여 빠짐없이 확인**.
- MezzConfig 단독 JAR과 LambDynamicLights의 다중 Jar-in-Jar: 루트 TOML만 읽는 검증 오류 수정.
- 파이프 `| tee`가 실패 상태를 숨기던 초기 CI 오류: `set -euo pipefail` 적용.
- 초기 서버 In Control! areas.json 쓰기 실패: [제작자 원본 코드](https://github.com/McJtyMods/InControl/blob/1.21_neo/src/main/java/mcjty/incontrol/tools/varia/JSonTools.java) 확인 후 CI 서버 config/incontrol 디렉터리를 사전 생성, 최종 CI에서 파일 생성 검증.

## 4. 이어서 할 일

- [여섯 직업 Lv300/301 범용 능력·성장 비교](./GENERAL_PURPOSE_LEVEL300_301_READINESS_MATRIX_V1.md): 본체+부하·숙주·결계·계약·군체 전체 전력을 포함. **특정 보스/상성별 평준화하지 않는다**.
- 다음 본체 개발 단위: NeoForge Java 프로젝트 정식 초기화 → 서버 권한 RPG XP·직업 데이터/레벨/자동 스탯 기초 → 한 직업의 실제 게임 안 수직 슬라이스. 기존 6직업 세부 기획과 7/8직업 보류 상태를 유지한다.
