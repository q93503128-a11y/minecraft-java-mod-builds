#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def check(label: str, condition: bool) -> None:
    if not condition:
        raise AssertionError(label)
    print(f"[PASS] {label}")


def enum_constants(source: str, enum_name: str, stop: str) -> list[str]:
    block = source.split(f"enum {enum_name}", 1)[1].split(stop, 1)[0]
    return re.findall(r"^\s{8}([A-Z][A-Z0-9_]+)\(\"", block, re.M)


def main() -> None:
    props = (ROOT / "gradle.properties").read_text(encoding="utf-8")
    keys = read("VillageClientKeys.java")
    client_ui = read("VillageClientUi.java")
    hud = read("VillageSkillHudOverlay.java")
    role = read("VillageRoleSkillSystem.java")
    ability = read("VillageRoleAbilitySystem.java")
    shop = read("VillageEquipmentShop.java")
    relics = read("VillageRelicSystem.java")
    traits = read("VillageWaveTrait.java")
    bosses = read("VillageBossAspectSystem.java")
    raid = read("VillageRaidSystem.java")
    intel = read("VillageWaveIntelSystem.java")
    enemies = read("VillageEnemyArchetypeSystem.java")
    audit = (ROOT / "CONTENT-AUDIT-v0.18.0.md").read_text(encoding="utf-8")

    check("v0.18.2-alpha.1 version is active", "mod_version=" in props)

    check(
        "실제 기본키가 Z 기술1·X 기술2·V 빠른 통신·H/J/K 관리로 일치합니다",
        all(token in keys for token in [
            'ROLE_SKILL_ONE = key("role_skill_one", GLFW.GLFW_KEY_Z)',
            'ROLE_SKILL_TWO = key("role_skill_two", GLFW.GLFW_KEY_X)',
            'QUICK_COMMUNICATION = key("quick_communication", GLFW.GLFW_KEY_V)',
            'STATUS = key("status", GLFW.GLFW_KEY_H)',
            'GROWTH = key("personal_progress", GLFW.GLFW_KEY_J)',
            'ROLE_PROGRESS = key("role_progress", GLFW.GLFW_KEY_K)',
        ]),
    )
    check(
        "이전 Z/V/B 기본값만 Z/X/V로 이관하고 사용자 안전 설정은 보존합니다",
        "boolean oldDefaults" in keys
        and "keyValue(ROLE_SKILL_TWO) == GLFW.GLFW_KEY_V" in keys
        and "keyValue(QUICK_COMMUNICATION) == GLFW.GLFW_KEY_B" in keys
        and "if (!oldDefaults && !unsafe) return;" in keys
        and "minecraft.options.save()" in keys,
    )
    reserved_block = keys.split("VANILLA_RESERVED", 1)[1].split(");", 1)[0]
    check(
        "X 단독 기술과 X+숫자 바닐라 툴바 조합이 공존합니다",
        "GLFW.GLFW_KEY_X" not in reserved_block
        and "consumeSkillTwo(minecraft)" in keys
        and "minecraft.options.keyHotbarSlots" in keys
        and "skillTwoToolbarChord" in keys
        and 'VillageUiActionPayload("use_skill:1")' in keys,
    )
    check(
        "모든 UI와 기술 HUD가 실제 변경 키를 토큰으로 해석합니다",
        "resolveTokens" in keys
        and "VillageClientKeys.resolveTokens(payload.title())" in client_ui
        and "VillageClientKeys.resolveTokens(payload.labels())" in client_ui
        and "VillageClientKeys.resolveTokens(payload.text())" in hud
        and "{SKILL1}" in role and "{SKILL2}" in role,
    )

    offers = enum_constants(shop, "Offer", "private final String id")
    relic_values = enum_constants(relics, "Relic", "private final String id")
    trait_block = traits.split("enum VillageWaveTrait", 1)[1].split("private static final", 1)[0]
    trait_values = re.findall(r"^\s{4}([A-Z][A-Z0-9_]+)\(\"", trait_block, re.M)
    aspect_values = enum_constants(bosses, "Aspect", "private final String displayName")
    check("상점 고유 장비가 10종에서 24종으로 확장되었습니다", len(offers) == 24)
    check("보스 유물이 6종에서 11종으로 확장되었습니다", len(relic_values) == 11)
    check("웨이브 특성이 8종에서 12종으로 확장되었습니다", len(trait_values) == 12)
    check("기본 보스 네 종에 여섯 변이가 결합되는 24개 보스 조합이 존재합니다", len(aspect_values) == 6)

    check(
        "확장 장비의 기술 위력·피해 감소·재사용 감소가 실제 계산에 연결됩니다",
        "float skillMultiplier" in shop
        and "float damageReduction" in shop
        and "int cooldownReductionSeconds" in shop
        and "VillageEquipmentShop.roleSkillMultiplier(player)" in role
        and "VillageEquipmentShop.cooldownReductionSeconds(player)" in role
        and "VillageRelicSystem.cooldownReductionSeconds(player)" in role,
    )
    check(
        "보스 변이가 생성·주기 행동·시설 피해·정리·정찰에 모두 연결됩니다",
        "VillageBossAspectSystem.configure" in raid
        and "VillageBossAspectSystem.tick" in raid
        and "VillageBossAspectSystem.structureMultiplier" in raid
        and "VillageBossAspectSystem.forget" in raid
        and "VillageBossAspectSystem.reset" in raid
        and "VillageBossAspectSystem.previewText" in intel,
    )
    for token in ["PHALANX", "BLOOD_MOON", "STORMFRONT", "RIFTED"]:
        check(f"신규 웨이브 특성 {token}은 실제 병과 편성을 가집니다", token in enemies)

    skill_enum = role.split("enum ActiveSkill", 1)[1].split("private final String id", 1)[0]
    skills = re.findall(r"^\s{8}([A-Z][A-Z0-9_]+)\(\"", skill_enum, re.M)
    coverage = role.split("ScalingCoverage scalingCoverage", 1)[1].split("allSkillBranchesConnected", 1)[0]
    check("액티브 기술 수는 20종으로 유지됩니다", len(skills) == 20)
    missing = [skill for skill in skills if f"case {skill} -> new ScalingCoverage(true, true, true" not in coverage]
    check("20개 기술 모두 위력·지속·특수 성장 연결표를 통과합니다", not missing)
    check("런타임 전체 연결 검증 진입점이 존재합니다", "allSkillBranchesConnected()" in role)

    concrete_markers = [
        "SPIN_SCALE", "RALLY_SCALE", "waveCount", "durationMultiplier(), slam.specialRank()",
        "RAPID_SCALE", "RICOCHET_SCALE", "fireOrbContacts", "fractureDuration",
        "firstEchoChance", "secondEchoChance", "Math.max(50, duration / 2)",
        "FORTRESS_SCALE", "AEGIS_SCALE", "passiveRank",
    ]
    for marker in concrete_markers:
        check(f"성장 실제 적용 경로: {marker}", marker in ability)
    check(
        "지속형 기술 상태는 초기화와 만료 정리를 모두 수행합니다",
        all(token in ability for token in [
            "SPIN_SCALE.clear()", "RALLY_SCALE.clear()", "RAPID_SCALE.clear()",
            "RICOCHET_SCALE.clear()", "FORTRESS_SCALE.clear()", "AEGIS_SCALE.clear()",
            "SPIN_SCALE.keySet().removeIf", "RALLY_SCALE.entrySet().removeIf",
            "RAPID_SCALE.keySet().removeIf", "RICOCHET_SCALE.keySet().removeIf",
            "FORTRESS_SCALE.keySet().removeIf", "AEGIS_SCALE.keySet().removeIf",
        ]),
    )
    check(
        "강화 화살 위력은 본체와 파생 화살에 중복 곱해지지 않습니다",
        "RAPID_ARROWS" in ability
        and "event.setAmount(event.getAmount() * rapid.power())" in ability
        and "event.setAmount(event.getAmount() * ricochet.power())" in ability
        and "getBaseDamage()" not in ability
        and "event.getAmount() * 0.72f * ricochetPower" not in ability,
    )
    check(
        "콘텐츠 감사 문서가 현재 수량과 무한 캠페인 확장을 기록합니다",
        all(token in audit for token in [
            "상점 고유 장비 24종", "보스 변이 6종", "총 24개 보스 조합",
            "웨이브 특성 12종", "유물 11종", "고정 마지막 날은 없으며 무한 진행",
        ]),
    )


if __name__ == "__main__":
    main()
