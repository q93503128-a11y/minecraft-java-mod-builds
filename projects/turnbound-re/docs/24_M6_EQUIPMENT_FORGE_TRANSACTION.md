# 24 — M6 EQUIPMENT FORGE TRANSACTION

이 문서는 `23_M6_MINIMAL_EQUIPMENT_CONTRACT.md`의 장비 backend를 실제 Minecraft inventory와 연결하는 production transaction 경계를 정본화한다.

## 1. 실제 흐름

```text
실제 채광
→ Minecraft 일반 인벤토리의 철/구리/금
→ Smithing Table 근처에서 제작/강화 요청
→ 서버가 현재 save + 현재 definition + 현재 inventory 재검증
→ Coin와 실제 material을 함께 지불
→ EquipmentProgress 저장
→ 캐릭터 장착
→ authored Encounter의 player participant에 장비 bonus 적용
```

장비 제작은 Minecraft material을 Coin으로 환전하는 경로가 아니다. 두 자원을 각각 실제 비용으로 사용한다.

## 2. 물리 workstation gate

첫 production bridge는 vanilla `Smithing Table`을 사용한다.

- 플레이어 기준 수평 4블록, 수직 2블록 범위 안에 Smithing Table이 있어야 `CRAFT`/`UPGRADE`가 허용된다.
- 검사는 매 tick 수행하지 않고 equipment snapshot/요청 시점에만 수행한다.
- 최종 HUB_01의 전용 forge 외형/배치는 World Asset Gate 이후 Smithing Table 기능을 감싸는 방식으로 교체할 수 있다.
- 장착/해제는 캐릭터 빌드 변경이므로 Smithing Table을 요구하지 않는다.

즉, 현재 단계에서도 화면 버튼만으로 아무 위치에서 장비를 생성하는 우회는 존재하지 않는다.

## 3. Inventory transaction

NeoForge 26.2 transfer API를 사용한다.

- `PlayerInventoryWrapper.of(player).getMainSlots()`만 대상으로 한다.
- armor/offhand는 장비 재료 소비에서 제외한다.
- 특히 offhand Battle Preparation과 장비 제작이 서로의 선택을 암묵적으로 소비하지 않는다.
- 요구 item type과 같은 실제 stack을 여러 슬롯에 걸쳐 정확한 수량까지 추출한다.
- component가 붙은 같은 item도 같은 Minecraft material type으로 계산한다.

재료 추출은 `Transaction.openRoot()` 안에서 수행한다.

- 정확한 요구량을 추출하지 못하면 commit하지 않는다.
- commit하지 않은 partial extraction은 rollback된다.
- 충분한 경우에만 progression compare-and-set과 transaction commit을 완료한다.

## 4. Progression compare-and-set

`PlayerProgressStore.replaceIfCurrent(expected, next)`는 transaction 계산에 사용한 immutable `PlayerProgress`가 여전히 현재 서버 상태인지 확인한다.

따라서 오래된 화면/패킷은 다음을 할 수 없다.

- Lv1 비용으로 이미 Lv2가 된 장비를 다시 강화.
- 이전 장착 상태를 기준으로 다른 캐릭터의 장비를 덮어쓰기.
- 오래된 Coin 상태와 현재 inventory를 섞어 결제.

클라이언트는 `expectedLevel`과 `expectedEquippedId`를 stale detection token으로 보낼 뿐, 비용/보너스/material 보유량을 결정하지 않는다.

## 5. Server-authored equipment snapshot

장비 UI가 사용할 별도 snapshot에는 다음만 포함한다.

- equipment id / ingredient item.
- 현재 level / max level.
- 현재 장착 캐릭터.
- 서버가 센 실제 material 보유량.
- 현재 bonus.
- 다음 tier Coin/material 비용과 bonus.
- `CRAFT` / `UPGRADE` / `MAX` 상태.
- 현재 차단 이유.
- Smithing Table 접근 가능 여부.

차단 이유 대표:

- `FORGE_UNAVAILABLE`
- `INSUFFICIENT_COIN`
- `INSUFFICIENT_MATERIAL`
- `MAX_LEVEL`

클라이언트가 material count를 계산해 서버에 보내는 구조는 사용하지 않는다.

## 6. Network version

Equipment action/snapshot wire가 추가되므로 TURNBOUND play protocol은 `12`에서 `13`으로 올린다.

구버전 client/server가 새 packet 계약을 조용히 섞어 사용하는 것을 허용하지 않는다.

## 7. 이번 단위 자동 검증

- NeoForge transaction에서 commit하지 않은 material extraction rollback.
- commit한 exact extraction만 실제 감소.
- 여러 stack에 분산된 동일 material 합산/추출.
- 다른 material 보존.
- equipment action packet stale token round-trip.
- server-authored material count/forge availability projection.
- Smithing Table gate가 없으면 충분한 Coin/material이어도 forge action 차단.
- 기존 전체 JUnit / clean build / production JAR verify.

## 8. 아직 남은 production presentation

이번 단위의 핵심은 **실제 자원 transaction과 서버 권한 경계**다.

다음 presentation 단위에서:

- 기존 selected-character 화면의 네 번째 `Equipment` context.
- 캐릭터당 1슬롯 현재 장착 상태.
- 세 대표 장비의 현재/다음 수치 비교.
- Smithing Table 접근 여부와 실제 보유 소재/비용.
- 제작/강화/장착/해제 입력.

을 기존 M5 visual language 안에 통합한다.

별도 장비 dashboard나 새 통화 화면은 만들지 않는다.

## 9. 통합 playtest에서 확인할 것

- 실제 채광량 대비 Lv1~3 강화 속도.
- Smithing Table 거리 gate가 자연스러운지.
- offhand 준비물과 장비 재료가 의도대로 분리되는지.
- 재접속/월드 save 후 equipment와 material이 정확히 남는지.
- multiplayer inventory race는 실제 멀티 테스트가 가능한 시점에 별도 검증한다.
