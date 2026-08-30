#!/usr/bin/env python3
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
F = ROOT / "projects/frontier-settlement/src/main/resources/assets"
S = ROOT / "projects/survival-ascension/src/main/resources/assets"


def load(path: Path):
    if not path.exists():
        raise SystemExit(f"missing generated localization file: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def save(path: Path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def patch(namespace: str, values: dict[str, str], survival=False):
    base = S if survival else F
    path = base / namespace / "lang/ko_kr.json"
    data = load(path)
    for key, value in values.items():
        if key in data:
            data[key] = value
    save(path, data)
    return data

# Dungeons & Taverns: NLLB occasionally emits an empty string for this compact enchantment name.
patch("dnt", {
    "enchantment.dnt.ghasted": "가스트화",
})

# Better Combat: these are the entire currently missing client option set and are frequently visible.
patch("bettercombat", {
    "key.category.bettercombat.main": "개선된 전투",
    "text.autoconfig.bettercombat.option.client.isSwingThruGrassSmart": "풀을 통과해 공격",
    "text.autoconfig.bettercombat.option.client.isSwingThruGrassSmart.@Tooltip": "공격 범위 안에 적이 있으면 풀 같은 얇은 블록을 무시하고 공격합니다.",
    "text.autoconfig.bettercombat.option.client.isShowingWeaponTrails": "무기 궤적 표시",
    "text.autoconfig.bettercombat.option.client.isShowingWeaponTrails.@Tooltip": "공격할 때 무기 궤적 효과를 표시합니다.",
    "text.autoconfig.bettercombat.option.client.isTooltipAttackRangeReformat": "아이템 툴팁 공격 범위 재표시",
    "text.autoconfig.bettercombat.option.client.isTooltipAttackRangeReformat.@Tooltip": "엔티티 상호작용 범위를 공격 범위 형식으로 표시합니다.",
    "text.autoconfig.bettercombat.option.client.mineWithWeaponWhitelist": "무기 채굴 허용 목록",
    "text.autoconfig.bettercombat.option.client.mineWithWeaponWhitelist.@Tooltip": "이 정규식과 일치하는 ID를 가진 무기는 항상 블록을 채굴할 수 있습니다.",
    "text.autoconfig.bettercombat.option.client.firstPersonAnimations": "1인칭 공격 애니메이션",
    "text.autoconfig.bettercombat.option.client.firstPersonAnimations.@Tooltip": "1인칭에서 플레이어 공격 애니메이션을 표시합니다. 사용자 지정 카메라 모드 사용 시에만 변경할 수 있습니다.",
    "text.autoconfig.bettercombat.option.client.legAnimationThreshold": "다리 애니메이션 비활성화 속도",
    "text.autoconfig.bettercombat.option.client.legAnimationThreshold.@Tooltip": "실험적 기능: 이 속도보다 빠르게 이동할 때 다리 애니메이션을 비활성화합니다.",
})

# Sophisticated Backpacks: polish all new Mob Catcher-facing strings and recipe-adjacent controls.
sb = patch("sophisticatedbackpacks", {
    "item.sophisticatedbackpacks.mob_catcher_upgrade": "몹 포획 업그레이드",
    "item.sophisticatedbackpacks.mob_catcher_upgrade.tooltip": "웅크린 채 우클릭하면 수동적 몹을 배낭 보관 슬롯에 포획합니다.",
    "item.sophisticatedbackpacks.advanced_mob_catcher_upgrade": "고급 몹 포획 업그레이드",
    "item.sophisticatedbackpacks.advanced_mob_catcher_upgrade.tooltip": "웅크린 채 우클릭하면 수동적 몹과 적대적 몹을 배낭 보관 슬롯에 포획합니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_contains_mobs": "포획된 몹을 먼저 모두 풀어야 몹 포획 업그레이드를 제거할 수 있습니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_mobs_need_advanced": "포획된 몹이 있어 고급 몹 포획 업그레이드가 필요합니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_only_one_allowed": "배낭에는 몹 포획 업그레이드를 하나만 장착할 수 있습니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_captured": "%s 포획 완료",
    "gui.sophisticatedbackpacks.status.mob_catcher_released": "%s 해제 완료",
    "gui.sophisticatedbackpacks.status.mob_catcher_no_upgrade": "배낭에 몹 포획 업그레이드가 없습니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_invalid_entity": "이 몹은 포획할 수 없습니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_players_blocked": "플레이어는 포획할 수 없습니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_boss_blocked": "보스는 포획할 수 없습니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_passengers_blocked": "탑승객이 있거나 다른 엔티티를 태운 몹은 포획할 수 없습니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_blocklisted": "이 몹은 포획 차단 목록에 포함되어 있습니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_not_owner": "자신이 포획한 몹만 해제할 수 있습니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_inventory_blocked": "인벤토리를 가진 몹은 포획할 수 없습니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_hostile_needs_advanced": "적대적 몹을 포획하려면 고급 몹 포획 업그레이드가 필요합니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_too_large": "이 몹은 너무 커서 %s×%s 슬롯이 필요합니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_no_space": "포획하려면 연속된 %s×%s 빈 슬롯 공간이 필요합니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_release_failed": "이곳에는 몹을 풀 수 없습니다.",
    "gui.sophisticatedbackpacks.status.mob_catcher_no_release_space": "몹을 풀 수 있는 공간이 없습니다.",
    "gui.sophisticatedbackpacks.mob_catcher.click_to_release": "클릭하여 몹 풀기",
    "gui.sophisticatedbackpacks.mob_catcher.entity_preview_failed": "엔티티 미리보기를 표시할 수 없습니다.",
    "gui.sophisticatedbackpacks.upgrades.buttons.refill_crafting_grid": "제작 칸 다시 채우기",
    "gui.sophisticatedbackpacks.upgrades.buttons.do_not_refill_crafting_grid": "제작 칸 다시 채우지 않기",
    "gui.sophisticatedbackpacks.upgrades.buttons.refill_input": "입력 슬롯 다시 채우기",
    "gui.sophisticatedbackpacks.upgrades.buttons.do_not_refill_input": "입력 슬롯 다시 채우지 않기",
    "sophisticatedbackpacks.configuration.mobCatcherUpgrade": "몹 포획 업그레이드",
})
# Domain cleanup for remaining generated Sophisticated Backpacks text.
for key, value in list(sb.items()):
    if not isinstance(value, str):
        continue
    value = value.replace("군중 포착기", "몹 포획").replace("군중 포수", "몹 포획")
    value = value.replace("군중", "몹").replace("매프 캐치", "몹 포획").replace("Mafias", "몹")
    value = value.replace("상사", "보스")
    sb[key] = value
save(F / "sophisticatedbackpacks/lang/ko_kr.json", sb)

# Sophisticated Core: high-visibility upgrade terminology plus the three NLLB empty outputs.
patch("sophisticatedcore", {
    "itemGroup.sophisticatedcore": "Sophisticated Core",
    "item.sophisticatedcore.xp_bucket": "경험치 양동이",
    "upgrade_group.sophisticatedcore.stack_upgrades": "스택 업그레이드",
    "upgrade_group.sophisticatedcore.cooking_upgrades": "조리 업그레이드",
    "upgrade_group.sophisticatedcore.jukebox_upgrades": "주크박스 업그레이드",
    "gui.sophisticatedcore.settings.item_display": "아이템 표시",
    "gui.sophisticatedcore.upgrades.smoking": "훈연",
    "gui.sophisticatedcore.upgrades.auto_smoking": "자동 훈연",
    "gui.sophisticatedcore.upgrades.blasting": "용광",
    "gui.sophisticatedcore.upgrades.auto_blasting": "자동 용광",
    "gui.sophisticatedcore.upgrades.advanced_jukebox": "고급 주크박스",
    "gui.sophisticatedcore.upgrades.alchemy": "연금술",
    "gui.sophisticatedcore.upgrades.alchemy.tooltip": "연금술 설정",
    "gui.sophisticatedcore.upgrades.advanced_alchemy": "고급 연금술",
    "gui.sophisticatedcore.upgrades.advanced_alchemy.tooltip": "고급 연금술 설정",
    "gui.sophisticatedcore.upgrades.chipped_botanist_workbench": "식물학자 작업대",
    "gui.sophisticatedcore.upgrades.chipped_glassblower": "유리공 작업대",
    "gui.sophisticatedcore.upgrades.chipped_carpenters_table": "목수 작업대",
    "gui.sophisticatedcore.upgrades.chipped_mason_table": "석공 작업대",
    "gui.sophisticatedcore.upgrades.chipped_loom_table": "베틀 작업대",
    "gui.sophisticatedcore.upgrades.chipped_alchemy_bench": "연금술 작업대",
    "gui.sophisticatedcore.upgrades.chipped_tinkering_table": "수리 작업대",
    "gui.sophisticatedcore.upgrades.sawmill": "제재소",
    "gui.sophisticatedcore.buttons.upgrade_switch_enabled": "업그레이드 활성화",
    "gui.sophisticatedcore.buttons.upgrade_switch_disabled": "업그레이드 비활성화",
})

# Weapons Expanded: item names are deterministic from registry IDs and should never rely on MT.
wp_path = F / "weaponsexpanded/lang/ko_kr.json"
wp = load(wp_path)
materials = {
    "wooden": "나무", "golden": "금", "stone": "돌", "copper": "구리",
    "iron": "철", "diamond": "다이아몬드", "netherite": "네더라이트",
}
weapons = {
    "broadsword": "브로드소드", "sickle": "낫", "scythe": "대낫", "longsword": "장검",
    "katana": "카타나", "hatchet": "손도끼", "hammer": "망치", "battleaxe": "전투도끼",
    "greatsword": "대검", "warhammer": "워해머",
}
for material, material_ko in materials.items():
    for weapon, weapon_ko in weapons.items():
        key = f"item.weaponsexpanded.{material}_{weapon}"
        if key in wp:
            wp[key] = f"{material_ko} {weapon_ko}"
wp.update({k: v for k, v in {
    "item.weaponsexpanded.heavy_arrow": "중량 화살",
    "item.weaponsexpanded.explosive_arrow": "폭발 화살",
    "item.weaponsexpanded.longbow": "장궁",
    "item.weaponsexpanded.chain_crossbow": "연발 석궁",
    "item.minecraft.potion.effect.frostbite": "동상 물약",
    "item.minecraft.splash_potion.effect.frostbite": "투척용 동상 물약",
    "item.minecraft.lingering_potion.effect.frostbite": "잔류형 동상 물약",
    "item.minecraft.tipped_arrow.effect.frostbite": "동상의 화살",
    "enchantment.weaponsexpanded.withering": "위더링",
    "enchantment.weaponsexpanded.withering.desc": "몹이나 플레이어를 공격할 때 위더 효과를 부여합니다.",
    "enchantment.weaponsexpanded.polluting": "오염",
    "enchantment.weaponsexpanded.polluting.desc": "몹이나 플레이어를 공격할 때 독 효과를 부여합니다.",
    "enchantment.weaponsexpanded.frostbite": "동상",
    "enchantment.weaponsexpanded.frostbite.desc": "몹이나 플레이어를 공격할 때 동상 효과를 부여합니다.",
    "enchantment.weaponsexpanded.freeze": "빙결",
    "enchantment.weaponsexpanded.freeze.desc": "발사한 화살에 빙결 효과를 부여합니다.",
    "enchantment.weaponsexpanded.leech": "흡혈",
    "enchantment.weaponsexpanded.leech.desc": "피해를 줄 때 체력을 회복합니다.",
    "enchantment.weaponsexpanded.cleaving": "절단",
    "effect.weaponsexpanded.frostbite": "동상",
    "advancements.weaponsexpanded.diamond_weapon": "다이아몬드 무기",
    "advancements.weaponsexpanded.diamond_weapon.description": "다이아몬드 무기를 획득하세요.",
    "advancements.weaponsexpanded.netherite_weapon": "네더라이트 무기",
    "advancements.weaponsexpanded.netherite_weapon.description": "다이아몬드 무기를 네더라이트로 업그레이드하세요.",
    "advancements.weaponsexpanded.weapons_expanded": "확장된 무기",
    "advancements.weaponsexpanded.weapons_expanded.description": "모든 네더라이트 무기를 획득하세요.",
    "tooltip.weaponsexpanded.twohandedsword": "양손 무기",
    "tooltip.weaponsexpanded.chain_crossbow_shots": "장전: %s/%s",
    "tooltip.weaponsexpanded.warhammer.blunt_side": "둔기 면",
    "tooltip.weaponsexpanded.warhammer.sharp_side": "날 면",
    "item.modifiers.bothhands": "양손으로 들었을 때",
    "subtitles.weaponsexpanded.item.chain_crossbow.chamber": "연발 석궁 장전",
    "subtitles.weaponsexpanded.item.chain_crossbow.full": "연발 석궁 장전 완료",
    "weaponsexpanded.configuration.enable_custom_loot_tables": "사용자 지정 전리품 테이블 사용",
    "weaponsexpanded.configuration.enable_entity_melee_equipment": "엔티티 근접 무기 장비 사용",
    "weaponsexpanded.configuration.enable_trial_chamber_melee_equipment": "시련의 회당 근접 무기 장비 사용",
    "weaponsexpanded.configuration.alternate_two_handed_sword": "대체 양손검 방식 사용",
    "weaponsexpanded.configuration.disable_extra_axe_damage": "도끼 추가 내구도 소모 비활성화",
    "key.category.weaponsexpanded.general": "Weapons Expanded",
    "key.weaponsexpanded.toggle_bastard_sword_mode": "바스타드 소드 모드 전환",
}.items() if k in wp})
save(wp_path, wp)

# Xaero terminology: NLLB frequently mistranslates Auto as automobile and waypoint as log point.
for namespace in ("xaerobetterpvp", "xaerominimap"):
    path = F / namespace / "lang/ko_kr.json"
    d = load(path)
    for key, value in list(d.items()):
        if not isinstance(value, str):
            continue
        value = value.replace("자동차", "자동")
        value = value.replace("로그 포인트", "웨이포인트").replace("로그포인트", "웨이포인트")
        value = value.replace("선수", "플레이어")
        d[key] = value
    save(path, d)

# Final generated-overlay quality gate. This catches the failure mode that prompted this pass:
# keys technically present but empty or obviously corrupt.
target_paths = [
    F / "bettercombat/lang/ko_kr.json",
    F / "dnt/lang/ko_kr.json",
    F / "jade/lang/ko_kr.json",
    F / "sophisticatedbackpacks/lang/ko_kr.json",
    F / "sophisticatedcore/lang/ko_kr.json",
    S / "tbos/lang/ko_kr.json",
    F / "weaponsexpanded/lang/ko_kr.json",
    F / "xaerobetterpvp/lang/ko_kr.json",
    F / "xaerominimap/lang/ko_kr.json",
]
banned = (
    "doggystyle", "FileReport", "CompositeSON", "♡♡♡", "콘스탄티노플",
    "Scenic 님", "process는 다음을 수행", "매프 캐치", "Mafias",
)
empty = []
garbage = []
for path in target_paths:
    d = load(path)
    for key, value in d.items():
        if isinstance(value, str):
            if not value.strip():
                empty.append(f"{path.parent.parent.name}:{key}")
            if any(token in value for token in banned):
                garbage.append(f"{path.parent.parent.name}:{key}={value}")
if empty:
    raise SystemExit("empty Korean overlay values remain:\n" + "\n".join(empty[:80]))
if garbage:
    raise SystemExit("known garbage remains in Korean overlay:\n" + "\n".join(garbage[:80]))

# Critical screenshot/recipe-facing regression guards.
assert sb["item.sophisticatedbackpacks.mob_catcher_upgrade"] == "몹 포획 업그레이드"
assert sb["item.sophisticatedbackpacks.advanced_mob_catcher_upgrade"] == "고급 몹 포획 업그레이드"
for material, material_ko in materials.items():
    for weapon, weapon_ko in weapons.items():
        key = f"item.weaponsexpanded.{material}_{weapon}"
        if key in wp:
            assert wp[key] == f"{material_ko} {weapon_ko}", key

print("FULL PACK KOREAN POSTPROCESS PASS")
print("checked_files=", len(target_paths))
print("weapon_registry_names=", sum(1 for k in wp if k.startswith("item.weaponsexpanded.")))
