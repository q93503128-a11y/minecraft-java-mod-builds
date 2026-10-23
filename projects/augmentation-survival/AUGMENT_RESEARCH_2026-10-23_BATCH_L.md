# Augmentation Survival — Batch L Research Notes

Date: 2026-10-23
Status: proposal research only; not yet confirmed roster
Base main HEAD checked before proposal: `ce42b5d2dddfd977d4d1ed8f7b0a85bd941ce4ad`

이번 배치는 Batch K에서 아직 결정되지 않은 `공중 작업`, `횃불 던지기`, `미끄러지기`, `급제동`, `도발`, `마법 추출`을 반복하지 않고, 기존 확정/보류/폐기와 의미가 겹치지 않는 새로운 Minecraft 고유 상호작용을 찾는 데 초점을 둔다.

## 1. 외부 사례 / Minecraft 메커니즘에서 확인한 방향

### 자동 도구 선택
Minecraft 26.2 Fabric에는 `Tool Switcher`, `AutoSwitch`, `Auto Tool Switch`처럼 목표 블록과 보유 도구를 비교해 가장 적합한 도구로 자동 전환하는 모드가 실제로 존재한다. 일부 구현은 Efficiency, Silk Touch/Fortune, 내구도까지 고려한다.

참고:
- https://modrinth.com/mod/tool-switcher
- https://modrinth.com/mod/autoswitch

### Mending 확장
바닐라 Mending은 손/장착 슬롯의 손상된 Mending 아이템 중 하나를 경험치 구슬마다 선택해 수리한다. `Inventory Mending`, `Nemo's Mending`, `OverpoweredMending` 같은 모드는 인벤토리 전체의 Mending 아이템으로 범위를 넓히거나 손상이 큰 장비를 우선 수리하는 방식이 반복적으로 사용되고 있다.

참고:
- https://minecraft.wiki/w/Mending
- https://modrinth.com/project/inventory-mending
- https://modrinth.com/mod/nemos-mending

### Minecart 이동
Minecraft 26.2에도 `Fast Player Minecarts`, `High-Speed Rail`, `Flash Carts`처럼 플레이어가 직접 타는 광차의 속도를 높여 철도를 장거리 이동수단으로 다시 살리는 모드가 다수 존재한다. 플레이어 탑승 광차만 강화하면 호퍼/상자 광차 기반 레드스톤 타이밍을 건드리지 않을 수 있다.

참고:
- https://modrinth.com/mod/fast-player-minecarts
- https://modrinth.com/project/highspeed-rail

### Fishing rod combat
Minecraft 전투 개조 모드와 Minecraft Dungeons 계열 구현에서는 낚싯대를 단순 낚시 도구가 아니라 적을 끌어오거나 짧게 제압하는 전투 도구로 활용하는 사례가 있다. 기존 보류 `갈고리`는 블록에 걸어 플레이어 자신을 당기는 이동기이므로, 적을 낚아채는 효과는 별도 축으로 설계할 수 있다.

참고:
- https://modrinth.com/mod/combat-control
- https://modrinth.com/datapack/dungeons-fishing-rod

### Trident / Riptide
`Transcending Trident`는 물 양동이를 보조손에 든 경우 비가 오지 않아도 Riptide를 쓸 수 있게 하는 사례가 있고, `Forked`/`Better Tridents`도 trident의 귀환·Channeling·습윤 판정을 재해석한다. 우리 쪽은 직접 기능 복제보다 `물 양동이를 실제 자원으로 소비해 건조 지형에서 1회 Riptide를 허용`하는 방식이 증강 시스템에 더 맞는다.

참고:
- https://modrinth.com/project/transcending-trident
- https://modrinth.com/project/forked
- https://modrinth.com/project/better-tridents

### Structure compass
`Explorer's Compass`는 Minecraft 26.2 Fabric에서 등록된 vanilla/modded structure를 GUI에서 선택해 찾는 구조를 제공한다. 현재 공식 모드팩은 Terralith, Repurposed Structures, Dungeons and Taverns, Chrono Dawn, World Bosses, MCEndgame 등 구조물 풀이 매우 크므로, 고등급 증강으로 구조물 탐색 자체를 확장할 여지가 있다.

참고:
- https://modrinth.com/mod/explorers-compass

## 2. Batch L 신규 후보

### 고급 — 최적 도구 Lv.1

- 블록 채굴을 시작할 때 핫바 안의 도구 중 해당 블록에 가장 빠른 유효 도구를 자동 선택한다.
- 채굴 입력을 놓거나 블록 파괴가 끝나면 원래 슬롯으로 돌아간다.
- 웅크리는 동안에는 자동 전환을 끈다.
- Silk Touch/Fortune처럼 결과 자체를 바꾸는 인챈트가 여러 후보에 섞여 있으면 임의로 의미를 바꾸지 않도록 현재 선택 도구를 우선하고, 단순 속도 비교만으로 서로 다른 채굴 결과를 바꾸지 않는다.
- 인벤토리 전체를 강제로 뒤섞지 않고 핫바만 사용한다.

### 고급 — 광차 질주 Lv.3

- 플레이어 자신이 탄 일반 광차의 최대 주행속도를 바닐라 대비 **x1.5 / x2.0 / x2.5**로 높인다.
- 빈 광차, 몹 탑승 광차, 상자/호퍼/화로 광차는 변경하지 않는다.
- 레드스톤 운송 시스템의 타이밍을 바꾸지 않고, 플레이어 철도 이동만 강화하는 목적이다.
- 실제 곡선/경사 안정성은 구현 시 Minecraft 26.2 광차 물리에서 별도 검증한다.

### 희귀 — 수선 배분 Lv.1

- 경험치 구슬을 획득할 때 손/장착 슬롯뿐 아니라 **플레이어 메인 인벤토리 전체의 Mending 아이템**을 수선 후보로 포함한다.
- 손상 비율이 가장 높은 아이템을 우선 수리한다.
- 수선 비율은 바닐라 Mending과 동일하게 **XP 1포인트당 내구도 2**를 유지한다.
- 수리에 쓰고 남은 XP만 플레이어 경험치로 들어가며 XP나 내구도를 추가 생성하지 않는다.

### 희귀 — 낚아채기 Lv.3

- 낚싯바늘이 적대적 개체에 걸린 상태에서 줄을 감으면 대상을 플레이어 쪽으로 강하게 당긴다.
- 일반 적은 약 **2 / 3 / 4블록** 당겨지고, 이후 1.5초 동안 이동속도가 **30% / 45% / 60% 감소**한다.
- 같은 대상에는 4초 내부 쿨다운.
- 보스는 이동량 25%, 둔화량 50%만 적용한다.
- 낚싯대 내구도는 정상 소비한다.
- 기존 `갈고리` 후보는 블록에 걸어 플레이어 자신을 이동시키는 능력이므로 역할이 다르다.

### 희귀 — 마법 미리보기 Lv.1

- 마법부여대의 세 후보에 대해 비용을 지불하기 전에 **실제로 붙을 모든 인챈트와 레벨을 미리 표시**한다.
- 후보 생성 규칙, 비용, 확률, 인챈트 결과 자체는 변경하지 않는다.
- `마법 새로고침`과 연계하면 새로고침 후 새 후보의 전체 결과도 즉시 확인할 수 있다.
- 정보 공개만 바꾸는 증강이며 더 좋은 결과를 강제로 생성하지 않는다.

### 영웅 — 물약 확산 Lv.3

- 자신이 **마시는 형태의 이로운 물약**을 사용했을 때 반경 8블록의 같은 팀/공유 그룹 플레이어와 길들인 전투 동료에게 그 물약의 이로운 상태효과를 공유한다.
- 공유 대상 지속시간은 원본 지속시간의 **25% / 40% / 55%**.
- 효과 단계(amplifier)는 원본과 동일.
- Splash/Lingering potion은 이미 범위 사용 수단이므로 이 증강의 발동 원인이 되지 않는다.
- 공유된 효과는 다시 `물약 확산`을 발동하지 않는다.

### 영웅 — 휴대 수류 Lv.1

- Riptide가 붙은 삼지창을 사용하려 할 때 비/물 조건을 만족하지 않아도, **보조손에 물 양동이**가 있으면 Riptide를 1회 사용할 수 있다.
- 성공한 건조 지형 Riptide 1회마다 물 양동이는 빈 양동이로 변한다.
- 이미 비를 맞거나 물속에 있어 바닐라 Riptide 조건을 만족하면 물을 소비하지 않는다.
- 별도 쿨다운은 두지 않고 삼지창 충전/사용 규칙은 정상 유지한다.
- Nether에서도 물 양동이를 실제 1회 자원으로 소비하는 방식이므로 사용 가능하도록 설계할 수 있으나, 최종 허용 여부는 밸런스 결정 시 고정한다.

### 전설 — 탐색 나침반 Lv.1

- 일반 나침반을 들고 증강 보조 입력을 사용하면 **구조물 검색 UI**를 연다.
- 서버 registry에서 검색 가능한 vanilla/modded structure를 선택하면 가장 가까운 유효 구조물을 찾고, 해당 나침반이 그 위치를 가리키게 한다.
- 검색 결과는 서버가 계산하고 캐시해 반복 탐색 비용을 줄인다.
- Stronghold/End progression처럼 별도 탐색 아이템이 게임 진행의 핵심인 구조물은 기본 blacklist 후보로 둔다.
- 모드팩의 던전/보스/탐험 구조물은 compat blacklist가 없는 한 정상 검색 가능하게 한다.
- 차원이 다르면 현재 차원에서 검색 가능한 구조물만 보여준다.
- 새 재화나 별도 성장축을 만들지 않고, 넓어진 공식 모드팩 월드를 탐험하는 전설급 편의/탐색 증강으로 사용한다.

## 3. 이번 배치에서 일부러 제외한 것

- Batch K 보류 후보 전부 재제안 금지.
- `반동 사격`: 사용자 폐기 결정 때문에 유사한 쇠뇌 반동 이동도 제외.
- 또 다른 단순 공격력/치명타/방어력 증가: 기존 로스터와 보류 후보에 충분히 많음.
- `삼지창 자동 귀환`: 사실상 Loyalty 대체에 가까워 증강 고유성이 약해 제외.
- `고고학 블록 외곽선`: 기존 광석/상자/적 표시형과 플레이 감각이 너무 비슷해 이번 배치에서는 제외.
- `펫 자동 텔레포트`: 바닐라/외부 companion 계열과 기능 경계가 복잡하고 `공간 연결`, `동료 구조`와도 일부 역할이 겹쳐 제외.

문서 제안만 기록했으므로 build/CI는 실행하지 않는다.
