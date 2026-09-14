# 27 — STARTER CHARACTER VISUAL PIPELINE

## 목표

Starter roster의 gameplay identity와 presentation entity를 분리한다.

- `sourceEntity`는 획득/드랍/데이터 정체성을 위해 vanilla entity를 유지한다.
- custom visual entity는 Character Overview와 virtual Battle Stage에서만 사용한다.
- server-authoritative combat/progression/save는 custom visual entity를 참조하지 않는다.
- 첫 production model은 `turnbound_re:zombie`다.

## Zombie 1차 production model

- gameplay source: `minecraft:zombie`
- presentation entity: `turnbound_re:starter_zombie_visual`
- texture: `assets/turnbound_re/textures/entity/starter_zombie.png`
- texture source: 프로젝트에 직접 제공된 128x128 원본, SHA-256 `52822eabfae98c0dbacc1299173b80b2aae4c372e964c568a4139613cf4b1b7b`
- model: head/body/pelvis + upper/lower arm + thigh/shin으로 분절한 humanoid
- visual intent: vanilla Zombie의 단순 skin swap이 아니라 굽은 팔, 비대칭 장갑/보호구, 분절 관절로 Vanguard/Breaker 실루엣을 강화한다.

## 외부 base/reference

Loy's Goodies `models/generic-model/characters/230507_alex.bbmodel`의 humanoid articulation을 editable base/reference로 검토했다.

- repo: `SL0ANE/Loy-s-Goodies`
- reviewed commit: `afbb7695b09de0ed8ee3aa97732ff7c3d367520c`
- license: CC0-1.0
- 직접 원본 texture를 반입하지 않는다.
- Java model geometry/UV는 TURNBOUND: RE texture와 전투 화면 스케일에 맞춰 재구성한다.

## 검증 기준

자동 검증:
- Zombie의 server catalog 원본 `minecraft:zombie`가 수정되지 않는가.
- client presentation mapping만 dedicated visual entity로 바뀌는가.
- Skeleton 등 다른 캐릭터는 vanilla source fallback을 유지하는가.
- model/renderer/entity registry가 26.2에서 build되는가.

최종 visual gate:
- Overview와 Battle Stage에서 같은 Zombie identity가 보이는가.
- 480x270에서도 머리/팔/몸통 실루엣이 뭉개지지 않는가.
- idle/action motion에서 팔과 몸통이 vanilla Zombie와 충분히 구분되는가.
- texture seam, inverted face, clipping이 없는가.

자동 build 성공은 이 visual gate를 대체하지 않는다.
