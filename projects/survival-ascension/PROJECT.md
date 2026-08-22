# Survival Ascension

- Mod version: `0.19.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Existing-world compatibility: 기존 스킬 SavedData, `infrastructure_v1`, `world_ascension_v1`, 강화 적/전술 분대 NBT, affix CustomData, 채굴 모드 persistent data를 유지한다. 0.19 종말 변이는 기존 엔티티에 소급 부여하지 않고 새 자연 생성 대상부터 persistent NBT로 저장한다.

## 핵심 방향
숙련 상승은 단순 수치 상승이 아니라 물리적 작업 규모와 선택지 증가다. 대량 생산 자원은 공동 인프라·재련·대형 작업으로 다시 소비되고, 보스 처치 이후에는 적의 조직·랭크·행동 자체도 함께 승천한다.

## 월드 승천
- 0단계 `각성`: 기본.
- 위더 최초 격파 → 1단계 `전설`.
- 엔더 드래곤 최초 격파 → 2단계 `종말`.
- `world_ascension_v1` SavedData에 서버 월드 공동 저장, 단계 후퇴 없음.
- 엘리트 출현/상위 랭크와 전술 분대 규모·형성 확률이 단계와 함께 증가.
- M → 인프라 → 진행도에서 현재 서버 단계 확인.

## 0.19 종말 변이
Hostiles Are Too Easy(CC0)의 Celestial Type 어휘 중 기존 시스템과 중복이 적은 Withered Skeleton / Phase Zombie / Plague Zombie를 NeoForge 26.2 이벤트 구조로 재구현한다.

공통 규칙:
- 월드 승천 2단계에서만 새로 부여.
- 자연 생성 적만 대상, 스포너 출신 제외.
- 아기 몹 제외.
- 좀비/스켈레톤 계열 중 18% 변이 판정.
- 변이 종류는 persistent NBT `survivalascension_endgame_mutation`에 저장.
- 엘리트 랭크·전술 분대 역할과 중첩 가능.
- 플레이어 처치 시 추가 경험치 10, 35% 확률 메아리 조각 1개.

행동:
- `위더`: 변이 스켈레톤이 플레이어에게 실제 체력 피해를 주면 위더 80틱.
- `위상`: 변이 좀비가 플레이어에게 피해를 받으면 쿨다운이 준비된 경우 55% 확률로 측후방 회피. 반응 쿨 45틱.
- `역병`: 변이 좀비가 플레이어에게 실제 체력 피해를 주면 독 120틱.

변이는 체력 배수만 추가하는 시스템이 아니며, 기존 Elite / Warband 행동 계층 위에 전투 규칙을 한 층 더 겹친다.

## 승천 중추
종말 단계 전용 공동 인프라.
- 네더의 별 4
- 드래곤의 숨결 64
- 흑요석 512
- 자수정 조각 512
- 메아리 조각 64

종말 단계 이전에는 서버가 자원 투입 자체를 거부한다. 완공 + 기동 Lv.90에서 착지 전 공중 돌진이 1회→2회가 되며 두 사용 모두 기존 R 대시 쿨다운을 공유한다.

## 현재 후반 루프
숙련 성장 → 광역/대형 행동 → 강화 적/전술 분대 → affix 장비 → 재련/분해 → 대량 자원 → 공동 인프라 → 터널/재파종/입체 건축/충격파 → 위더/드래곤 → 월드 승천 → 승천 중추 + 종말 변이.

## 안전 계약
- Shift 정밀 모드 유지.
- 벌목 자연 나무 잎 검증 + 틱 분산.
- 터널 정상 destroyBlock + 12/player·64/global tick budget.
- 관개 실제 씨앗 소비 + 보호 배치 훅.
- 건축 실제 재료 소비 + 보호 훅 + 최대 256 pending/player.
- 전투 충격파는 전투 Lv.90 + 훈련장 + 질주 직접 근접 + 쿨다운.
- 종말 변이는 Stage2 + 자연 생성 + 특정 몹으로 제한, 스포너 보상 농장 차단.

## 외부 소스 정책
Skill Proficiencies, Veinminer++, MineMenu, Building Gadgets 2, Mob Champions, Apotheosis, Mekanism, Warband 등 permissive 소스는 고지와 함께 필요한 구조를 적응한다. Hostiles Are Too Easy는 CC0이며 0.17의 동적 난도와 0.19의 종말 변이 어휘를 적응한다. 제한 라이선스 프로젝트는 기능/UX만 참고하고 소스·에셋은 복사하지 않는다.
