# 기생술사 — 공통 숙주 카탈로그와 외부 모드 연결 계약 (2026-11-16)

> **최신 후속:** 외부 20종이 실제 런타임 레지스트리·클래스 fingerprint 검증을 통과했고, 총 카탈로그가 **54종**으로 늘었다. 아래 '외부 종 0개' 표현은 이전 중간 단계 기록이다. [외부 20종 실제 통합 보고서](./EXTERNAL_HOST_RUNTIME_BINDING_2026-11-16.md) 우선.


## 정본과 적용 경계

- 게임: Minecraft Java **1.21.1**, NeoForge **21.1.244**, Java 21. 기존 6직업, 저장 데이터와 서버 권위를 보존.
- 구현 커밋: `d52996e432ab39dbd95ba3a566ff3fd98bc41207`; 검증 [GitHub CI #43735461247](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43735461247), **BUILD VERIFIED/JAR PRODUCED**.
- 데이터: [`src/main/resources/data/anomalousclasses/parasite/hosts.json`](../src/main/resources/data/anomalousclasses/parasite/hosts.json). 게임 코드: `state/ParasiteRules`(catalog), `state/ParasiteService`(claim/movement/BD), `state/ParasiteHostCombatService`(authoritative melee), `state/ParasiteSpeciesService`(selected specials).
- 이 JSON은 **배포 JAR 내장 카탈로그**로 로딩된다. 아직 런타임 datapack 핫리로드나 서버 사용자 설정 파일을 지원한다는 뜻은 아니다.

## 기본 기능 범위

기존 10종: 돼지, 닭, 양, 소, 좀비, 거미, 슬라임, 크리퍼, 가디언, 엔더맨.

신규 지상형 24종:
- 평화/중립: 무시룸, 토끼, 여우, 염소, 말, 당나귀, 노새, 판다, 북극곰, 스니퍼, 아르마딜로, 개구리, 오셀롯, 거북
- 적대/강적: 호글린, 조글린, 허스크, 좀비 주민, 좀비화 피글린, 피글린, 피글린 야수, 변명자, 파괴수, 동굴거미

**신규 24종의 범위는 공통 실숙주 몸 이동, 점프, 서버 판정 기본 근접 공격, 사망/해제, BD·숙련 성장이다.** 각 종의 원래 광역 공격·원거리 투사체·보스 페이즈·아이템 장착/탑승 인벤토리 등은 추가 종별 어댑터 없이는 재현하지 않는다.

환경형 고유 동작이 연결된 일부 신규 종:
- 토끼·염소·말/당나귀/노새·개구리: 카탈로그 `innateJump`로 선천 점프 변동. 숙련도100에 타 숙주에도 자동 계승, 더 강한 기존 슬라임 점프를 덮어쓰지 않음
- 동굴거미: 선천 벽 타기, 해당 종 숙련도100이면 다른 숙주에서 거미 계열의 환경형 벽 타기 가능
- 기존 크리퍼·가디언·엔더맨의 직접 능력과 슬라임의 탄성 점프는 유지

카탈로그 한 행:

```json
{"id":"minecraft:rabbit","requiredBd":20,"intrusionTicks":40,"speedFactor":1.20,"adapter":"ground","innateJump":0.56}
```

`id`는 생물 원본 등록 키. `requiredBd`는 최소 영구 생체 데이터(소모 없음), `intrusionTicks`는 침투 기준 틱(프로필과 패시브로 최종 조정), `speedFactor`는 원본 이동 속도 배율, `adapter`는 현재 `ground` 또는 `aquatic`, `innateJump`는 지면 점프 속도(최소 .42). 임의 기술 리스트/종족 도감 장착제는 별도로 만들지 않는다. 신규 수치는 **밸런스 테스트 전 후보**.

## 외부 모드와 호환 준비

[확정 외부 모드팩](./pack-source.json)은 변경하지 않음: L_Ender's Cataclysm, Mowzie's Mobs, Born in Chaos, Mutant Monsters, YUNG's Better Dungeons, In Control!, Locator Bar, LambDynamicLights, JEI + 그 필수 라이브러리 6종.

현재 카탈로그에 **타 모드의 엔티티 ID는 0개**. 이 파일은 `minecraft:` 외의 리소스 네임스페이스도 형식상 읽을 수 있지만, *등록된다고 원본 전용 AI/보스 기술까지 구현되는 건 아님*. 검증 전 외부 몹 자동 전종 기생이나 임의 폭스/보스 대체 처리는 금지.

외부 종을 실제 활성화하기 전에:
1. 지정된 외부 모드팩 JAR/버전에서 진짜 `EntityType` ID와 `Mob` 상속 여부, 원본 AI/상태·Phase·공격 처리 조사.
2. 지상형 일반몹이라도 공격 애니메이션/AI 복구·주인/소환체·아군 권한·드롭과 보상 주체 확인. 기존 바닐라 구조가 맞는 경우에만 한 행을 추가한다.
3. 보스는 무조건 generic으로 넣지 말고 원본 Phase/피해 상한/시전 AI/드롭·소환체 복원 어댑터 후 명시적으로 추가한다. 지원할 수 없는 종은 UI에서 '미지원' 상태 유지.
4. 외부팩+우리 직업 모드를 **동일 서버와 클라이언트에 함께 설치**해 부팅·기생/지배/인형/보스 전투·저장·멀티 충돌 확인. 기존 외부팩 단독 서버 스모크는 이 검사와 다르다.
5. 성능 문제는 실제 측정으로 판단. 원본 월드의 모든 개체를 틱마다 순회하지 않는다.

## 검증 사실과 남은 QA

- **PASS:** 기생 카탈로그 34행 로딩, 24신규 조회, 종별 조건/계승 경계, 모드 보스 미등록, 기존 직업·저장/회복 verifier, Gradle/JAR.
- **NOT TESTED:** 실제 34종 기생·복구·탑승·히트박스·점프/수중 이동, 외부팩 동시 로딩, 멀티 플레이, 실제 캐릭터 조작감과 시각 품질.
- 플레이테스트는 무분별하게 34종 전체를 매번 수동 시키지 말고 **체형 대표 1~2개씩**(소형 토끼, 표준 좀비/피글린, 대형 말/파괴수, 수중 가디언) 점검→공통 결함 수정→기능별 나머지 표본 검증 순서가 효율적이다.
