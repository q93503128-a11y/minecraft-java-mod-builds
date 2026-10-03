# TURNBOUND — New Drabyel → Av'Sal First Playtest Checklist

> 기준: 현재 `turnbound-alpha17-full-v04`의 최신 성공 Build TURNBOUND artifact.
> 목적: 첫 지역의 실제 게임성, 동선, 장비 성장, 행동경제, 다중 대상 전투, 역참/탈것, Av'Sal 첫 보스까지를 한 번에 검증한다.
> 실행 직전 GitHub Actions의 최신 성공 run과 artifact가 현재 branch HEAD의 코드 커밋을 검증한 것인지 확인한다.

## 1. 시작 원칙

첫 플레이는 가능한 한 정상 진행으로 한다.
텔레포트나 전투 강제 시작을 먼저 사용하면 이동 밀도, 보상 속도, 퀘스트 연결 문제가 가려진다.
일반 재접속은 진행 상태를 유지해야 한다. 완전 새 진행이 필요할 때만 OP 권한으로 `/turnbound reset`을 사용한다.

자동 회귀 기준:
- Build TURNBOUND #998 / code `db0f01818ec45cae2c1a0995a93bb010c717534f`에서 Chapter 1 same-level/no-equipment AUTO pacing contract 통과.
- 일반 2~4체전은 구성에 따라 최대 약 15 ally regular action 범위, 선택 Elite는 최대 20, Graul/Karnon은 24~42 범위로 회귀를 막는다.
- 이 수치는 실제 플레이 목표 시간이 아니다. 플레이 중 “여전히 길다/너무 짧다/위협 없이 HP만 많다”가 느껴지면 체감을 우선해 다시 조정한다.

확인할 핵심 감각:
- 길이 비어 있거나 지루하지 않은가
- 몬스터가 지나치게 띄엄띄엄 있지 않은가
- 일반 적 재생성 템포가 답답하지 않은가
- 메인/서브/반복 의뢰 사이에 성장 공백이 없는가
- 장비 T1 → T2와 강화 비용이 실제로 진행을 돕는가
- 역참과 탈것이 장거리 이동의 귀찮음을 줄이는가
- Av'Sal 첫 보스가 일반 정예보다 확실히 다른 전투인가

## 2. New Drabyel

- 입구 안내원이 아렌으로 표시되는지 확인.
- 첫 북쪽 순찰이 마을에서 과하게 멀지 않은지 확인.
- E 메뉴 확인 후 대장간 / 시장 / 역참 / 소환 시설이 실제 NPC 위치와 맞는지 확인.
- 지역 메인 조사 3곳 중 2곳으로 정상 완료되는지 확인.
- 목표 추적 중 52블록 안에 들어왔을 때 해당 플레이어에게만 가까운 목표 outline이 보이는지 확인.
- 지역 의뢰관 로웬에게 반복 의뢰를 받고 보고할 수 있는지 확인.

## 3. Capital Valley 성장

- 일반 몬스터와 비인간형 몬스터가 필드에서 자연스럽게 섞이는지 확인.
- 일반 적은 처치 뒤 짧은 시간 내 다시 생성되어 사냥 흐름이 끊기지 않는지 확인.
- T1 장비가 필드 전투에서 실제로 들어오는지 확인.
- Gold로 대장간 강화를 했을 때 다음 전투 체감이 생기는지 확인.
- T1/T2 SPD 장비를 선택했을 때 행동 순서와 행동 빈도가 실제로 달라지는지 확인. 높은 SPD 자체를 오류로 보지 말고 다른 장비 선택을 포기할 가치가 있는지 판단.
- 카이렌/리네트의 단일 집중과 브람/모르웬의 2인기, 라제의 전체 공격이 실제로 서로 다른 전투 선택처럼 느껴지는지 확인.
- 3~4체 일반전에서 2인기 사용 시 플레이어가 원하는 두 적을 직접 선택할 수 있고, 적 1명만 남으면 정상 사용되는지 확인.
- 루메아의 +180/+240 가속과 +360/+450 시간 도약이 Turn Order를 눈에 띄게 바꾸되 무조건적인 연속 행동 버튼처럼 느껴지지 않는지 확인.
- 메인 진행이 버겁다면 반복 의뢰/선택 정예로 성장할 수 있는지 확인.
- Warning Cave Elite / Graul / 북쪽 도로 등 여러 선택지 중 하나를 해결하면 Av'Sal 원정 브리핑이 열리는지 확인.

## 4. 역참과 길뿔 산양

현재 첫 구간 역참망:
- 프라이멀 길머리
- 캐피털 밸리 탑
- 뉴 드라비엘
- 끊긴 가도
- 아브살 외곽

확인:
- M 지도에서는 순간이동이 되지 않아야 함.
- 실제 역참지기 근처에서만 이동 UI가 열려야 함.
- 발견하지 않은 역참은 목적지로 쓸 수 없어야 함.
- 이동 시 출발 fade + 이동 소리 + 도착 fade/착지 소리가 자연스러운지 확인.
- 길뿔 산양을 역참에서 대여할 수 있는지 확인.
- 바닐라 말보다 이동이 확실히 빠르고 점프/단차 대응이 좋은지 확인.
- 하차 후 1분 안에는 다시 탈 수 있는지 확인.
- 전투 진입 / 빠른 이동 / 로그아웃 시 대여 탈것이 정리되는지 확인.
- 멀티 확인 가능 시: 다른 플레이어가 내가 빌린 산양을 탈 수 없어야 함.

## 5. Av'Sal 가는 길

메인 흐름:
1. 서쪽 길의 이상 신호
2. 운송인 데른 접촉
3. Av'Sal 외곽 도착
4. 외곽 조사 2/3
5. 중계선 해결 2/3
6. 카르논 보스
7. 사엘에게 보고

동시에 확인할 선택 콘텐츠:
- 잿빛 사냥개
- 뒤집힌 운송수레
- 녹슨 파수기
- 가도 밖 선택 정예

확인:
- 약 1.4k 블록 이동이 한 번의 빈 걷기가 아니라 목표/적/NPC/발견으로 나뉘는지 확인.
- 길에 몬스터가 너무 드물거나 한 위치에만 몰려 있지 않은지 확인.
- 선택 퀘스트를 건너뛰어도 메인 진행이 막히지 않는지 확인.
- T2 장비가 Av'Sal 전투와 정예에서 들어오기 시작하는지 확인.
- 중간 역참을 발견한 뒤에는 동일 장거리 구간을 매번 다시 걸을 필요가 없는지 확인.

## 6. Av'Sal 외곽과 중계선

- 폐품상 / 생존자 / 낡은 기록 중 2개만 확인해도 진행되는지 확인.
- M 지도에는 미해결 후보가 동시에 보이는지 확인.
- 중계선도 서부 배전실 / 파수조 / 동부 우회선 중 2개만 해결하면 되는지 확인.
- 비전투 경로와 전투 경로가 실제로 다른 선택처럼 느껴지는지 확인.
- 파수조 전투가 끝난 뒤 진행 카운트가 바로 갱신되는지 확인.
- 파수조는 자동 계측에서 HP-only drag가 발견되어 별도 pacing 보정이 들어갔다. 실제 플레이에서 3인전인데 선택 Elite처럼 오래 끌리지는 않는지 확인.

## 7. 첫 보스 — 수로 집행기 카르논

고정 시작 로스터:
- 카르논
- E009 고정 파수기
- E011 고정 지원 유닛

확인:
- 로스터가 매번 랜덤으로 바뀌지 않는지 확인.
- 카르논 전투에서 일반 전투가 아닌 보스 BGM이 재생되는지 확인.
- Barrier가 남은 상태에서 직접 공격하면 실제 반격이 오는지 확인.
- HP 55% 이하에서 Barrier 재전개 + 공격 템포 상승이 느껴지는지 확인.
- 수문 충돌 전에 예고 동작/바닥 범위가 읽히는지 확인.
- 예고 다음 광역 공격과 Gauge 지연이 실제로 적용되는지 확인.
- 보스/고정 잡몹의 모델 크기와 카메라 framing이 겹치거나 화면 밖으로 나가지 않는지 확인.
- 격파 뒤 보스가 즉시 다시 생성되지 않는지 확인.
- 카르논은 조정 전 자동 계측에서 61 ally actions / 22 enemy actions인데 아군 4명이 모두 생존하는 HP-sponge 형태였다. 현재는 24~42 ally-action 회귀 범위를 통과하지만, 실제 플레이에서 Barrier/반격/봉쇄/수문 충돌의 판단보다 체력 소모 시간이 더 크게 느껴지면 추가 단축한다.

## 8. 보스 이후

- 카르논 격파 후 목표가 사엘 보고로 바뀌는지 확인.
- 사엘에게 보고하면 MQ_AV07이 완료되고 첫 Av'Sal 슬라이스가 종료되는지 확인.
- 그 다음 다시 사엘에게 말하면 반복 지역 의뢰 루프로 돌아가는지 확인.
- 보스 첫 클리어 보상과 T2 장비 선택 보상이 정상 지급되는지 확인.
- 재접속 후에도 보스 클리어/보고 상태가 유지되는지 확인.

## 9. 소환 3D 연출

테스트용 재화가 필요하면:
- `/turnbound grant crystal 99999`

10연차 빠른 확인:
- `/turnbound archive ten`

확인:
- 실제 3D 캐릭터가 소환장 위에 등장하는지 확인.
- 카메라가 3D 배우를 제대로 framing하는지 확인.
- 중앙 2D bust가 3D 캐릭터를 덮지 않는지 확인.
- 10연차에서 `결과만 보기`를 누르면 연출만 종료되고 10개 결과 카드가 즉시 남는지 확인.
- 버튼이 `닫기`로 바뀌는지 확인.

## 10. 문제 보고 형식

문제가 보이면 다음 네 가지만 보내면 된다:
1. 어디서: 퀘스트/지역/시설/보스 단계
2. 무슨 현상: 느림, 약함, 길 찾기 어려움, UI 이상, 충돌, 보상 이상 등
3. 재현: 무엇을 한 직후 발생했는지
4. 가능하면 스크린샷 1장

코드상 정상이어도 플레이 감각이 나쁘면 문제로 취급한다.

## 검증 상태

- CODE REVIEWED: YES
- AUTOMATED TESTS: YES
- BUILD VERIFIED: YES
- DEDICATED SERVER SMOKE: YES
- JAR PRODUCED: YES
- ONE-CLICK PACK VERIFIED: YES
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO


## Build #1006 focused regression

Use the new one-click pack or otherwise ensure JourneyMap 26.2-6.0.8 is installed with the Build #1006 TURNBOUND JAR.

Check first:
- Bram/Lumea/Morwen selected-two skill: first and second enemies can both be clicked in the world; skill executes after two distinct targets; one remaining enemy still works.
- Character > Equipment: click Weapon/Armor/Accessory/Signature, compare candidate stats in the list, equip without cycling through the whole character roster.
- E > Equipment: no +1/+10 enhancement projection and no character target carousel.
- Blacksmith: selected gear shows current -> next and exact main-stat increase before enhancement.
- Summon duplicate below +10: only +Level increases, no Star Essence.
- Duplicate already at +10: no more +Level; Star Essence is granted.
- Closing/skipping summon presentation stops ritual-stage particle emission instead of continuing on the ground.
- N toggles JourneyMap minimap; M opens JourneyMap fullscreen.
- At the Capital Valley “choose one regional objective” step, north patrol / Warning Cave / Graul search choice all have map markers before the battle actors themselves are necessarily visible.
- TURNBOUND's former blocky custom full-map panel and clipped side-description panel should no longer be the production map UI.
