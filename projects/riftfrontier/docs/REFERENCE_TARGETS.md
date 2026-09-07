# Riftfrontier — Reference Targets & Lessons

이 문서는 외부 대형 모드 조사에서 얻은 **설계 원리**를 기록한다.

목표는 복제가 아니라 학습이다. 코드·자산·UI를 그대로 가져오는 문서가 아니다.

---

## 1. Create

### 배울 것
- 반복 등록을 줄이는 content builder/DSL
- Datagen 중심 생산
- 기능 패밀리별 generator
- 복잡한 시스템을 인게임에서 직접 설명하는 Ponder식 사고
- 적은 기본 규칙이 조합되어 많은 플레이 상황을 만드는 구조

### 그대로 따라하지 않을 것
- Riftfrontier와 맞지 않는 기계 시스템 구조
- Create 내부 API에 프로젝트 핵심을 종속시키는 방식

---

## 2. TerraFirmaCraft

### 배울 것
- 게임 규칙 자체를 custom data type/schema로 만드는 사고
- worldgen/recipe/material 등 대량 정의의 data-driven 구조
- data validation을 개발 파이프라인에 넣는 방식
- total conversion 수준에서도 규칙을 분리해 유지하는 방식

### 적용
- creature, attack, region, encounter, faction, research 등 프로젝트 고유 schema
- 실행 전에 dangling reference와 잘못된 dependency를 잡는 validator

---

## 3. Cobblemon

### 배울 것
- 수백 개 개체를 species/data/assets로 분리 관리하는 구조
- base definition과 variant/form/aspect 분리
- addition layer를 통해 기본 정의 전체를 복사하지 않고 확장하는 방식
- model/texture/animation resolver 사고

### 적용
- 생물 base profile + region/event/elite/equipment variant
- asset resolver
- addon/compatibility를 고려한 제한된 patch/addition 설계

---

## 4. MineColonies

### 배울 것
- 독립 기능이 아니라 연결망이 규모를 만든다는 점
- job/building/research/request 등 domain registry 분리
- request chain을 통해 생산과 물류가 서로 필요를 전달하는 구조
- 건축 제작을 별도 생산 파이프라인으로 취급하는 방식

### 적용
- logistics request
- faction/world domain event
- 생산/저장/원정이 직접 클래스 호출로 엉키지 않게 domain boundary 유지

---

## 5. Ars Nouveau

### 배울 것
- 완성 스킬 수백 개보다 form/effect/augment 조합으로 플레이어가 콘텐츠를 생성하게 하는 방식
- SpellPart처럼 동작 부품을 데이터/등록 단위로 만드는 사고

### 적용
- 전투 스킬/모듈의 일부를 composable part로 설계
- 조합이 실제 플레이 차이를 만들 때만 확장

---

## 6. Mekanism

### 배울 것
- 대규모 registry를 기능별로 분리
- recipe/datagen provider를 생산 방식별 sub-provider로 계층화
- compatibility 계층을 core와 분리

### 적용
- 하나의 거대한 `AllContent.java`를 피한다.
- content family/datagen provider를 기능 단위로 나눈다.

---

## 7. Alex's Caves

### 배울 것
- 지역 수보다 지역 하나의 제작 밀도를 우선하는 방식
- biome, worldgen, mob, resource, structure, sound, boss가 하나의 테마 패키지로 느껴지는 구성
- 작은 개수의 지역도 각각 독립 DLC처럼 만들면 대형 모드 체감이 생긴다는 점

### 적용
- `Region Pack` 품질 게이트
- 첫 vertical slice 지역 완성 전 region count 확대 금지

### 주의
- 일부 대형 static registry 스타일은 Riftfrontier 규모에서는 피한다.

---

## 8. Advent of Ascension

### 배울 것
- 대형 RPG의 boss/state/attack behaviour/animation/audio 분리
- boss를 일반 몹의 고스탯 버전으로 만들지 않는 방식
- 장기간 대량 콘텐츠 유지가 얼마나 큰 migration 비용을 만드는지에 대한 경고

### 적용
- boss state machine
- telegraph/attack/recovery
- reusable boss behaviour
- 안정적인 ID와 migration 전략

### 라이선스 주의
- 공개된 소스를 설계 연구 자료로만 본다. 라이선스가 허용하지 않는 코드를 Riftfrontier에 복제하지 않는다.

---

## 9. 공통 결론

성공한 대형 모드에서 반복해서 나온 원칙:

```text
콘텐츠를 직접 많이 만든다
X

콘텐츠를 안정적으로 많이 만들 수 있는 언어/데이터/제작 파이프라인을 만든다
O
```

또한:

```text
큰 시스템 A + 큰 시스템 B + 큰 시스템 C
```

보다

```text
A의 결과가 B의 입력이 되고
B의 변화가 C를 바꾸고
C가 다시 A의 선택지를 바꾸는 구조
```

가 체감 규모를 훨씬 크게 만든다.

---

## 10. UI/아트/사운드 레퍼런스 규칙

위 모드들은 시스템 구조 참고 대상이지 Riftfrontier의 최종 디자인 시트가 아니다.

핵심 UI나 presentation 작업 전에는 별도로:

- Game UI Database
- Interface In Game
- 실제 상용 게임 영상/공식 스크린샷
- 관련 대형 Minecraft 모드의 실제 플레이 화면
- Blockbench/합법적 외부 asset source

를 조사한다.

각 핵심 화면/자산은 가능하면 다음처럼 분해한다.

```text
Reference A → information hierarchy
Reference B → navigation/input
Reference C → selection/feedback
Reference D → animation/transition
Riftfrontier → own tokens/assets/content identity
```

AI가 아무 참고 없이 장식 스타일을 먼저 결정하지 않는다.

---

## 11. 외부 자산 기록

사용하는 모든 외부 자산은 `THIRD_PARTY_ASSETS.md`에 최소한 다음을 기록한다.

```text
Asset
Author
Source
License/usage note
Immutable source/revision if practical
Modified
Used in
```

개인용 테스트와 공개 저장소 배포 가능 여부를 구분한다.
