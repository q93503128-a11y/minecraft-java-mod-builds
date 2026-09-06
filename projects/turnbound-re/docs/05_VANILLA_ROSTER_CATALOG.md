# 05 — VANILLA ROSTER CATALOG

## 목표
Minecraft 버전이 변해도 바닐라 Mob 누락을 사람이 수동 체크하지 않도록 **registry 기반 전수 검증**을 한다.

## Eligible 규칙
런타임/데이터 생성 단계에서 `minecraft` namespace의 `EntityType` 중 실제 `Mob`으로 생성 가능한 타입을 수집하고 다음 둘 중 정확히 하나에 매핑한다.
1. `CharacterDefinition`
2. 명시적 `VanillaMobExclusion` + 이유

Player, projectile, item entity, vehicle, marker/utility, armor stand처럼 캐릭터 전투원이 아닌 Entity는 exclusion 대상이다. 보스/특수 개체도 누락시키지 않는다.

## Build invariant
```text
eligibleVanillaMobIds == characterMappedIds UNION explicitExcludedIds
characterMappedIds INTERSECT explicitExcludedIds == empty
```
위 조건이 깨지면 `data:validate`/CI 실패.

## 수동적 동물
Pig, Cow, Sheep, Chicken 등도 eligible이며 캐릭터가 된다. 전투력이 낮더라도 Basic/Skill/Burst/Passive 또는 단순 kit를 가져 턴제 전투에 정상 참가할 수 있다. 낮은 originStar와 낮은 squadCost를 활용할 수 있다.

## 분류 작업 필드
각 엔티티에 최소:
- `entityId`
- `classification`: CHARACTER / EXCLUDED
- `characterId` 또는 `reason`
- `originStar`
- `role`
- `availability`
- `contentStatus`: STUB / PLAYABLE / BALANCED / PRESENTATION_READY

## 개발 순서
1. registry enumerator와 누락 실패 테스트를 M0에서 먼저 만든다.
2. Vertical Slice 대표 8종을 PLAYABLE까지 만든다.
3. 모든 바닐라 Mob을 STUB 이상으로 등록한다.
4. 계열별로 PLAYABLE 전환한다: passive animals → overworld hostile → aquatic → nether → end → villagers/illagers → bosses/special.
5. 밸런스/프레젠테이션 상태를 별도 추적한다.

## STUB의 의미
STUB는 누락 방지용 데이터 자리이며 release-ready가 아니다. 기본 공격만 자동 생성해 최종 콘텐츠로 간주하는 것을 금지한다.

## 업데이트 대응
Minecraft 버전 업데이트 후 CI가 새 EntityType을 발견하면 build를 실패시켜 개발자가 의식적으로 Character/Exclusion을 선택하게 한다.
