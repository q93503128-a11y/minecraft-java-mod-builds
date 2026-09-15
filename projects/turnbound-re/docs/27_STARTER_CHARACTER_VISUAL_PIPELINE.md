# 27 — STARTER CHARACTER VISUAL PIPELINE

## 목표

Starter roster의 gameplay identity와 presentation entity를 분리한다.

- `sourceEntity`는 획득/드랍/데이터 정체성을 위해 vanilla entity를 유지한다.
- presentation entity는 Character Overview와 virtual Battle Stage에서만 사용한다.
- server-authoritative combat/progression/save는 presentation entity의 외형을 참조하지 않는다.
- 플레이어-facing 외형은 external production asset/base를 직접 사용하며, reference를 보고 TURNBOUND가 새 geometry를 재구성하지 않는다.

## Zombie production base

- gameplay source: `minecraft:zombie`
- presentation entity: `turnbound_re:starter_zombie_visual`
- model: Minecraft Java 26.2 runtime `ModelLayers.ZOMBIE`
- texture: `minecraft:textures/entity/zombie/zombie.png`
- classification: **DIRECT MOJANG RUNTIME BASE**
- TURNBOUND 추가 범위: authoritative enemy-target action에서 기존 head/body/arms/legs pose를 조정하는 presentation state만 추가한다.

즉 Zombie의 외형 차별화를 위해 자체 갑옷, 분절 팔/다리, 새 UV, 새 skin을 만들지 않는다. 더 강한 외형이 필요하면 실제 사용 가능한 외부 custom model을 확보해 그 asset 자체를 채택한다.

## 제거한 legacy 구현

external-only 규칙 이전에는:

- Loy's Goodies `230507_alex.bbmodel`을 reference로 보고
- TURNBOUND 전용 segmented humanoid geometry/UV를 다시 만들고
- `starter_zombie.png` 128×128 texture를 production resource로 사용했다.

이 방식은 외부 디자인 직접 사용이 아니므로 production에서 폐기한다.

- legacy reference repo: `SL0ANE/Loy-s-Goodies`
- reviewed commit: `afbb7695b09de0ed8ee3aa97732ff7c3d367520c`
- license: CC0-1.0
- legacy texture SHA-256 기록: `52822eabfae98c0dbacc1299173b80b2aae4c372e964c568a4139613cf4b1b7b`
- 현재 renderer는 이 custom texture를 참조하지 않는다.
- resource 파일 자체도 production tree에서 제거한다.

## 검증 기준

자동/코드 검증:
- Zombie의 server catalog 원본 `minecraft:zombie`가 수정되지 않는가.
- client presentation mapping만 dedicated visual entity를 유지하는가.
- renderer가 `ModelLayers.ZOMBIE`와 Mojang zombie texture를 직접 사용하는가.
- custom `LayerDefinition`/replacement geometry가 남아 있지 않은가.

최종 visual gate:
- Overview와 Battle Stage에서 같은 Mojang Zombie identity가 보이는가.
- action pose가 server-authored actor/target과 일치하는가.
- 480×270에서도 model이 UI에 잘리지 않는가.
- legacy segmented model/texture가 표시되지 않는가.

현재 전환은 build/CI를 실행하지 않았으며, 자동 build 성공은 visual gate를 대체하지 않는다.
