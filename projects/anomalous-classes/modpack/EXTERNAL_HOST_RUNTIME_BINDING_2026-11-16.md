# 외부 모드 몹 20종 — 실등록 감사와 기생숙주 통합 (2026-11-16)

## 확인된 정본

- Minecraft 1.21.1 / NeoForge 21.1.244 / Java 21. [정확한 9기능+6라이브러리 버전 고정 manifest](./pack-source.json).
- 기존 바닐라 숙주 34종 보존. 외부 네 모드에서 **20개 기생 프로필** 신규 등록, 총 54종.
- 코드: [`ParasiteRules.java`](../src/main/java/kr/moonseungjun/anomalousclasses/state/ParasiteRules.java), [`hosts.json`](../src/main/resources/data/anomalousclasses/parasite/hosts.json), [`ParasiteService.java`](../src/main/java/kr/moonseungjun/anomalousclasses/state/ParasiteService.java), [`ParasiteSpeciesService.java`](../src/main/java/kr/moonseungjun/anomalousclasses/state/ParasiteSpeciesService.java).
- 소스: `b45e05dd2d006e0543211a6410bf329b622b8d10`, 컴파일 수정 `001b52750e110b10f25556624ed21643b655a83a`. CI [일반 빌드 #43741286060](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43741286060) / [외부 통합 서버 #43741441267](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43741441267) 모두 **SUCCESS**.

## 실제 서버가 검증한 외부 엔티티

2026-11-16 KST 기준 NeoForge 전용 서버 + 외부 필수 JAR 12개 + 우리 JAR 1개를 동시에 로딩하여 **월드 부팅·정상 종료**. 런타임 `EntityType` 레지스트리 직접 순회, 원본 객체 생성은 월드에 추가하지 않고 타입 확인에만 사용. [`EXTERNAL_ENTITY_REGISTRY_AUDIT.json` + 서버 로그 artifact](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/43741441267/artifacts/14172409041)로 재현 가능.

| 실제 네임스페이스 | 등록된 엔티티 유형 | `Mob` | 지원 프로필 |
|---|---:|---:|---:|
| `born_in_chaos_v1` | 98 | 84 | 10 |
| `cataclysm` | 103 | 39 | 7 |
| `mowziesmobs` | 41 | 18 | 2 |
| `mutantmonsters` | 14 | 8 | 1 |
| **합계** | **256** | **149** | **20** |

`Mob` 149개는 실제 타입/클래스 존재 확인이지, 149종 기생 구현 완료가 아니다. 생성 예외 **0개**, 지원 선언된 외부 몸체 `expectedClass` 20/20 일치, 서버 필수 파일 탐색 13/13 성공.

## 실제로 연결한 종

| 제공 모드 | 네임스페이스에서 확인한 원본 EntityType ID |
|---|---|
| Born in Chaos | `maggot`, `baby_spider`, `diamond_termite`, `decaying_zombie`, `barrel_zombie`, `zombie_fisherman`, `zombie_clown`, `zombie_lumberjack`, `dread_hound`, `zombie_bruiser` |
| Cataclysm | `koboleton`, `draugr`, `deepling`, `elite_draugr`, `deepling_brute`, `royal_draugr`, `coral_golem` |
| Mowzie's Mobs | `grottol`, `bluff` |
| Mutant Monsters | `spider_pig` |

> 위 축약 ID의 정확한 엔티티 등록 키는 해당 행 네임스페이스+콜론+ID다. `expectedClass`의 완전한 Java 패키지와 요구 BD·침투 시간·이동 배율은 [실제 JSON 프로필](../src/main/resources/data/anomalousclasses/parasite/hosts.json)이 정본이다.

범위는 원본 1기 탑승·점유 권한·기본 지상 이동/점프·서버 판정 근접 공격·해제/죽음/BD·종 숙련이다. 외부 원본 고유 공격·원거리 주문·울부짖음·다단계 보스 스크립트·드롭 보상/희귀도 재현까지 포함하지 않는다. 외부 프로필의 요구 BD·이동 속도·침투 시간은 **사용자 확정 밸런스가 아닌 임시 시안**.

환경형으로 `cataclysm:deepling`, `cataclysm:deepling_brute`는 수중 적응을, `born_in_chaos_v1:baby_spider`는 벽 타기를 선천 제공한다. 해당 종 완전 숙련100 시 타 숙주에서도 같은 상시 특성을 유지한다. 기존 거미/가디언/슬라임과 함께 장착 슬롯·마나·불필요한 쿨타임을 추가하지 않는다.

## 정확성·보존·보스 정책

- 등록 ID는 Modrinth 릴리스의 실제 `EntityType` 목록에서 가져왔고 추측·자연어 이름으로 변환하지 않았다.
- 외부 ID가 존재하더라도 실제 Java 클래스가 본 프로젝트의 `expectedClass`와 달라지면 그 원본을 기생 대상에서 제외한다. 실제 월드에 없는 외부 종은 자동 생성하지 않는다.
- 보스 구분: `cataclysm:ignis`, `cataclysm:the_harbinger`, `cataclysm:netherite_monstrosity`, `mowziesmobs:ferrous_wroughtnaut`, `mowziesmobs:frostmaw`, `mutantmonsters:mutant_zombie`, `mutantmonsters:mutant_creeper`, `born_in_chaos_v1:supreme_bonescaller`는 실제로 등록되어 있지만 우리 기생 카탈로그에서는 **명시적으로 미지원**이며 CI가 이를 확인한다.
- 다른 6직업과 공통 UI·저장 키는 이번 작업에서 수정하지 않았다. 기존 직업 검증과 영구 저장 검사를 일반 빌드에서 수행했다.
- 외부 모드 Jar를 GitHub 소스에 재배포하지 않는다. `.mrpack`은 공식 배포 URL·해시 참조이며, CI 원본 감사 artifact에는 제3자 JAR을 포함하지 않는다.

## 남은 실제 QA와 다음 개발 작업

**완료:** BUILD VERIFIED, JAR PRODUCED, external-mod dedicated server CO-LOAD, live EntityType 20/20 CLASS VERIFIED.

**미검증:** ACTUAL POSSESSION, CLIENT RUNTIME, PLAYTESTED, MULTIPLAYER TESTED, 방대한 외부 몹의 모델·애니메이션·타격 피드백·대상 AI 복원·실외부 보스 전투; 서버 부팅만으로 보장하지 않는다.

다음에는 20종을 전부 매번 수동 테스트시키지 말고, 체형/전투 유형별 대표 6종(예: 소형 Grottol, 인간형 Decaying Zombie, 사족 Dread Hound, 수중 Deepling, 대형 Coral Golem, 변종 Spider Pig)을 실제 클라이언트에서 먼저 검사해 **공통 어댑터 결함을 먼저 수정**한다. 이후 모드별 특수 공격/피해 모션, 보스 원형 페이즈 보존형 기생·지배 어댑터를 별도 작업으로 묶는다.

