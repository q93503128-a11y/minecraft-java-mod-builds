# 13 — DATA SCHEMA CONTRACT

실제 Codec/JSON 이름은 구현하면서 Java naming에 맞출 수 있지만 의미와 필수성은 이 계약을 따른다.

## CharacterDefinition
```json
{
  "id": "turnbound_re:zombie",
  "sourceEntity": "minecraft:zombie",
  "originStar": 2,
  "squadCost": 2,
  "roles": ["VANGUARD", "BREAKER"],
  "baseStats": {"hp": 140, "atk": 32, "def": 28, "spd": 18, "poise": 100},
  "growth": {"hp": 8.0, "atk": 2.1, "def": 1.8, "spd": 0.35, "poise": 0.5},
  "ascensionFlat": {
    "2": {"hp": 0, "atk": 0, "def": 0, "spd": 0, "poise": 0},
    "3": {"hp": 20, "atk": 4, "def": 4, "spd": 1, "poise": 5}
  },
  "affinities": {
    "MELEE": "NORMAL", "PROJECTILE": "NORMAL", "FIRE": "WEAK",
    "BLAST": "NORMAL", "ARCANE": "NORMAL", "VOID": "RESIST"
  },
  "basicAction": "turnbound_re:zombie_strike",
  "skills": ["turnbound_re:zombie_grit", "turnbound_re:zombie_crush"],
  "burst": "turnbound_re:zombie_burst",
  "passives": ["turnbound_re:undead_endurance"],
  "availability": {"type": "ENCOUNTER_SHARD", "sourceTag": "turnbound_re:overworld_undead"},
  "presentationKey": "turnbound_re:zombie"
}
```

## BattleActionDefinition
```json
{
  "id": "turnbound_re:zombie_strike",
  "kind": "BASIC",
  "energyDelta": 10,
  "hpPower": 90,
  "poisePower": 20,
  "damageTag": "MELEE",
  "targeting": {"team": "ENEMY", "shape": "SINGLE", "count": 1},
  "priority": 0,
  "effects": [{"type": "DAMAGE"}]
}
```

Skill 소비는 `energyDelta` 음수로 표현할 수 있으나 decode 후 `kind`별 범위를 검증한다.

## StatusDefinition
```json
{
  "id": "turnbound_re:burn",
  "polarity": "NEGATIVE",
  "durationUnit": "TURN",
  "baseDuration": 2,
  "maxStacks": 3,
  "refreshRule": "REFRESH_DURATION",
  "dispelTags": ["DEBUFF", "FIRE"],
  "hooks": [{"when": "TURN_START", "effect": {"type": "DAMAGE_MAX_HP_PERCENT", "value": 0.03}}]
}
```

## EncounterDefinition
```json
{
  "id": "turnbound_re:test_plains_01",
  "difficulty": 1,
  "enemies": [
    {"character": "turnbound_re:zombie", "level": 5, "currentStar": 2},
    {"character": "turnbound_re:skeleton", "level": 5, "currentStar": 2}
  ],
  "rewardTable": "turnbound_re:test_plains_01",
  "sceneKey": "turnbound_re:debug_flat",
  "repeatable": true
}
```

## RewardTable
```json
{
  "id": "turnbound_re:test_plains_01",
  "rolls": [
    {"type": "COIN", "min": 20, "max": 30, "weight": 1},
    {"type": "ESSENCE", "min": 8, "max": 12, "weight": 1},
    {"type": "CHARACTER_SHARD", "character": "turnbound_re:zombie", "min": 1, "max": 2, "chance": 0.25}
  ]
}
```

## Save root
```json
{
  "schemaVersion": 1,
  "currencies": {},
  "characters": {},
  "party": {"slots": [], "capacity": 12},
  "discoveries": {},
  "worldFlags": {}
}
```

## Definition hash
전투 시작 시 사용 중 definition registry의 hash/version을 snapshot metadata에 기록해, 데이터 변경 후 오래된 battle log를 재생할 때 차이를 알 수 있게 한다.

## Validation
모든 ID는 namespaced ResourceLocation. 숫자 NaN/Infinity 금지. 확률 0..1. count 양수. origin/current star 범위와 level cap 검증. 모든 cross-reference resolve 필수.
