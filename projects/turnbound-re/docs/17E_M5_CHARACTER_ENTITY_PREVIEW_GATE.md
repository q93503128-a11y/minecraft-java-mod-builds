# 17E — M5 CHARACTER ENTITY PREVIEW GATE

최종 갱신: 2026-09-08

이 문서는 TURNBOUND: RE M5 Party Formation의 selected-character **실제 3D entity preview** 구현 계약과 자동 검증 범위를 기록한다.
시각적 품질 최종 판정 문서가 아니다. 실제 Minecraft screenshot audit 전에는 production visual PASS를 선언하지 않는다.

## 1. 목적

selected character가 텍스트 목록만으로 보이는 상태를 벗어나, 현재 CharacterDefinition이 가리키는 실제 Minecraft `LivingEntity`를 Overview 안에서 보여준다.

목표는 단순히 엔티티를 화면에 띄우는 것이 아니다.

- Zombie / Enderman / Iron Golem / Spider처럼 체형이 달라도 안정적으로 프레임 안에 들어와야 한다.
- 3D preview 때문에 역할/스탯/스킬 같은 핵심 정보가 눌리거나 잘리면 안 된다.
- 작은 GUI 영역에서는 억지로 축소한 프리뷰보다 텍스트 가독성을 우선한다.
- client가 character→entity 관계를 추측하거나 하드코딩하지 않는다.
- 매 frame entity를 새로 생성하지 않는다.
- preview entity는 실제 world에 spawn하지 않는 presentation-only 객체다.

## 2. 권한과 데이터 흐름

정본은 `CharacterDefinition.sourceEntity`다.

서버 흐름:

`DefinitionRegistry`
→ `CharacterDefinition.sourceEntity`
→ `CharacterPresentationNetworkPayloads.CatalogS2C`
→ client `ProgressionClientState`
→ selected character id에 대응하는 source entity 조회

원칙:

- client-side `switch(characterId)` 같은 별도 매핑 금지.
- 주변 entity 검색으로 외형 추측 금지.
- 현재 server definition registry에서 visual catalog를 작성한다.
- progression state를 답할 때 visual catalog와 progression snapshot이 같은 captured `DefinitionRegistry`를 사용한다.
- malformed/duplicate catalog entry는 client/server contract에서 fail closed한다.

이 추가 payload 때문에 공용 play-phase presentation protocol은 **v8**이다.

## 3. 렌더링 경로

NeoForge 26.2 / Minecraft 26.2의 현재 GUI extraction 경로를 사용한다.

- `BuiltInRegistries.ENTITY_TYPE`에서 server-published source id를 resolve.
- `EntityType.create(ClientLevel, EntitySpawnReason.COMMAND)`로 presentation entity를 생성.
- `LivingEntity`가 아닌 타입은 preview를 표시하지 않는다.
- `InventoryScreen.renderEntityInInventoryFollowsAngle(...)`를 사용해 vanilla inventory 계열의 실제 entity renderer를 재사용한다.
- 별도 custom renderer dependency를 추가하지 않는다.

preview 객체는:

- 현재 `ClientLevel`
- 현재 `sourceEntity`

조합이 유지되는 동안 재사용한다.
세계 또는 selected source entity가 바뀌면 새로 resolve한다.
Party Formation screen이 제거되면 cache를 clear한다.

## 4. 레이아웃 계약

3D preview는 **Overview tab에서만** 표시한다.
Skills/Growth는 정보밀도가 높으므로 selected detail 전체 폭을 유지한다.

`EntityPreviewLayout`은 entity type의 width/height와 selected-detail logical region을 입력으로 받아 `PreviewSpec`을 만든다.

현재 계약:

- preview 표시 최소 region: `210 × 120` logical px.
- 남겨야 하는 최소 text width: `118` logical px.
- preview box width: region의 약 1/3, `72..104` px clamp.
- preview box height: `80..116` px clamp.
- entity fit inner padding: horizontal 12 px / vertical 8 px.
- render scale 상한: `48`.
- entity width/height를 모두 만족하는 작은 쪽 scale을 선택.
- invalid/NaN/infinite/zero body size는 preview 숨김.

공간이 부족하면 preview를 작게 우겨 넣지 않고 **완전히 숨겨 Overview text width를 100% 보존**한다.

## 5. 체형 대응

자동 layout test에서 다음 대표 형태를 별도로 검사한다.

- Zombie-like humanoid: 약 `0.6 × 1.95`.
- Enderman-like tall body: 약 `0.6 × 2.9`.
- Iron Golem-like large body: 약 `1.4 × 2.7`.
- Spider-like wide/low body: 약 `1.4 × 0.9`.

각 경우:

- computed preview가 region 밖으로 나가지 않는지,
- scaled width가 horizontal inner box를 넘지 않는지,
- scaled height가 vertical inner box를 넘지 않는지,
- text readable width가 유지되는지

를 자동 검증한다.

현재 3D view는 과한 자동 회전 대신 약한 3/4 시점으로 고정한다.
실제 화면에서 체형별 중심·시선·animation pose가 어색한지는 screenshot audit에서 판정한다.

## 6. 실패 처리

다음 상황에서 player-facing debug text를 만들지 않는다.

- sourceEntity 없음.
- malformed id.
- registry에 없는 entity type.
- LivingEntity가 아닌 type.
- render region이 너무 작음.
- body dimension이 비정상.

이 경우 preview만 생략하고 나머지 Character Overview는 정상적으로 유지한다.

## 7. 자동 검증

추가된 핵심 자동 계약:

### `M5EntityPreviewLayoutTest`

- compact detail에서 preview를 숨기고 text width를 보존.
- normal detail에서 preview + 최소 text width 동시 확보.
- Zombie/Enderman/Iron Golem/Spider 계열 체형 fit.
- invalid body dimension fail closed.

### `M5CharacterPresentationPayloadTest`

- server-published character→sourceEntity catalog round-trip.
- duplicate character visual id reject.

기존 M0~M5 테스트도 함께 회귀한다.

## 8. 검증 기준

코드 검증 커밋:

`65d6fe652bd1d5eec563df69cb3e8f99f40816db`

GitHub Actions:

- workflow: `Build turnbound-re`
- run: `34177388163`
- 결과: **SUCCESS**
- Java: Temurin 25.0.4+1
- Gradle: 9.2.1
- NeoForge: 26.2.0.38-beta
- clean build / 전체 JUnit: **PASS**
- production JAR verify: **PASS**
- artifact upload: **PASS**
- JAR: `turnbound_re-0.1.0-alpha.1.jar`
- JAR SHA-256: `2e983c621678a803eee3340e471a3412464fc9149b1c4cd2f26cdf90bfcda906`

production JAR에 `CharacterEntityPreview`, `EntityPreviewLayout`, `CharacterPresentationNetworkPayloads.CatalogS2C`가 실제 포함된 것도 확인했다.

## 9. 아직 수동 검증이 필요한 부분

자동 PASS가 의미하는 것은 API/구조/layout math/회귀가 정상이라는 뜻이다.
다음은 아직 **NOT TESTED / VISUAL PENDING**이다.

- 실제 Minecraft에서 Zombie 중심 위치.
- Enderman 머리/발 clipping.
- Iron Golem의 시각적 크기와 프레임 여백.
- Spider의 낮은 체형 중심 배치.
- idle animation/frame 간 흔들림.
- GUI Scale별 실제 픽셀 clipping.
- texture/model/resource-pack 변화 시 체감.
- 실제 게임 화면에서 텍스트와 3D preview의 시선 경쟁.

따라서 이 gate는 **automated implementation gate PASS**이며 production visual PASS가 아니다.

## 10. 다음 작업

3D preview 자동 구현 gate는 닫혔다.
다음 M5 production 작업은 **Battle Result / Reward transition presentation**으로 이동한다.

다음 구현에서 반드시 지킬 것:

- 결과/보상 UI가 client 추측 보상을 표시하면 안 된다.
- 실제 server battle terminal state 및 reward settlement truth를 기반으로 해야 한다.
- reward roll을 client에서 다시 돌리지 않는다.
- 승리/패배/보상 수령 흐름이 전투 화면과 자연스럽게 이어져야 한다.
- player가 무엇을 얻었고 다음에 무엇을 할 수 있는지 빠르게 이해할 수 있어야 한다.
- 최종 시각 디자인은 기존 M5 visual language와 실제 screenshot audit를 거친다.
