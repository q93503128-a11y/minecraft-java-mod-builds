#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/survivalascension"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, value: str) -> None:
    path.write_text(value, encoding="utf-8")


def replace_once(path: Path, old: str, new: str) -> None:
    value = read(path)
    count = value.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected exactly one replacement target, found {count}: {old[:100]!r}")
    write(path, value.replace(old, new, 1))


# ---------------------------------------------------------------------------
# Version 0.61.20
# ---------------------------------------------------------------------------
replace_once(ROOT / "gradle.properties", "mod_version=0.61.19-alpha.1", "mod_version=0.61.20-alpha.1")
replace_once(
    JAVA / "SurvivalAscension.java",
    'public static final String VERSION = "0.61.19-alpha.1";',
    'public static final String VERSION = "0.61.20-alpha.1";',
)

# ---------------------------------------------------------------------------
# Equipment rerolls: consuming materials must always produce a different set.
# Five affix keys mean every 1..4-affix state has at least one alternative.
# ---------------------------------------------------------------------------
affixes = JAVA / "equipment/AscensionAffixes.java"
replace_once(
    affixes,
    '''    public static boolean reroll(ItemStack stack, RandomSource random) {\n        int rarity = rarity(stack);\n        Category category = category(stack);\n        if (rarity <= 0 || category == Category.NONE) return false;\n        boolean awakened = isAwakened(stack);\n        rollAffixes(stack, random, awakened ? 4 : rarity, category, awakened);\n        return true;\n    }\n''',
    '''    public static boolean reroll(ItemStack stack, RandomSource random) {\n        int rarity = rarity(stack);\n        Category category = category(stack);\n        if (rarity <= 0 || category == Category.NONE) return false;\n        boolean awakened = isAwakened(stack);\n        List<String> previous = currentAffixes(stack);\n        rerollAffixes(stack, random, awakened ? 4 : rarity, category, awakened, previous);\n        return true;\n    }\n''',
)
replace_once(
    affixes,
    '''    private static void rollAffixes(ItemStack stack, RandomSource random, int count, Category category, boolean awakened) {\n        List<String> pool = new ArrayList<>(AFFIX_POOL);\n        Collections.shuffle(pool, new java.util.Random(random.nextLong()));\n        int affixCount = Math.max(1, Math.min(count, pool.size()));\n        List<String> chosen = new ArrayList<>(pool.subList(0, affixCount));\n        writeAffixes(stack, Math.max(1, Math.min(3, count)), category, chosen, awakened);\n    }\n''',
    '''    private static void rerollAffixes(ItemStack stack, RandomSource random, int count, Category category,\n                                      boolean awakened, List<String> previous) {\n        List<String> pool = new ArrayList<>(AFFIX_POOL);\n        Collections.shuffle(pool, new java.util.Random(random.nextLong()));\n        int affixCount = Math.max(1, Math.min(count, pool.size()));\n        List<String> chosen = new ArrayList<>(pool.subList(0, affixCount));\n\n        // A paid reroll must change gameplay. If the random subset is identical, swap exactly one\n        // affix for a currently missing key. AFFIX_POOL has five keys and rerolls use at most four.\n        if (chosen.size() == previous.size() && previous.containsAll(chosen) && chosen.containsAll(previous)) {\n            String missing = null;\n            for (String key : AFFIX_POOL) {\n                if (!previous.contains(key)) {\n                    missing = key;\n                    break;\n                }\n            }\n            if (missing != null && !chosen.isEmpty()) chosen.set(chosen.size() - 1, missing);\n        }\n        writeAffixes(stack, Math.max(1, Math.min(3, count)), category, chosen, awakened);\n    }\n\n    private static void rollAffixes(ItemStack stack, RandomSource random, int count, Category category, boolean awakened) {\n        List<String> pool = new ArrayList<>(AFFIX_POOL);\n        Collections.shuffle(pool, new java.util.Random(random.nextLong()));\n        int affixCount = Math.max(1, Math.min(count, pool.size()));\n        List<String> chosen = new ArrayList<>(pool.subList(0, affixCount));\n        writeAffixes(stack, Math.max(1, Math.min(3, count)), category, chosen, awakened);\n    }\n''',
)

# ---------------------------------------------------------------------------
# Equipment economy: keep body/material/condition salvage value, but base salvage
# can never return more of an imprint material than that rarity's imprint consumed.
# Awakened recovery is added only after this cap because it refunds a bounded share
# of the separate one-time awakening investment.
# ---------------------------------------------------------------------------
equipment = JAVA / "equipment/EquipmentReforgeService.java"
replace_once(
    equipment,
    '''        if (rarity == 3 && awakened) {\n            return new MaterialCost[] {\n                    new MaterialCost(Items.AMETHYST_SHARD, 24, "자수정 조각"),\n                    new MaterialCost(Items.DIAMOND, 3, "다이아몬드"),\n                    new MaterialCost(Items.ECHO_SHARD, 2, "메아리 조각")\n            };\n        }\n''',
    '''        if (rarity == 3 && awakened) {\n            // Only five four-affix combinations exist. Identical rerolls are blocked, so keep the\n            // late-game sink meaningful without making a targeted awakened reroll excessively punitive.\n            return new MaterialCost[] {\n                    new MaterialCost(Items.AMETHYST_SHARD, 16, "자수정 조각"),\n                    new MaterialCost(Items.DIAMOND, 2, "다이아몬드"),\n                    new MaterialCost(Items.ECHO_SHARD, 1, "메아리 조각")\n            };\n        }\n''',
)
replace_once(
    equipment,
    '''        } else {\n            addReward(amounts, labels, Items.AMETHYST_SHARD, body * 2, "자수정 조각");\n            addReward(amounts, labels, Items.DIAMOND, body / 2, "다이아몬드");\n            addReward(amounts, labels, Items.ECHO_SHARD, body / 4, "메아리 조각");\n        }\n\n        if (AscensionAffixes.isAwakened(stack)) {\n''',
    '''        } else {\n            addReward(amounts, labels, Items.AMETHYST_SHARD, body * 2, "자수정 조각");\n            addReward(amounts, labels, Items.DIAMOND, body / 2, "다이아몬드");\n            addReward(amounts, labels, Items.ECHO_SHARD, body / 4, "메아리 조각");\n        }\n\n        capBaseSalvageToImprintCost(amounts, rarity);\n\n        if (AscensionAffixes.isAwakened(stack)) {\n''',
)
replace_once(
    equipment,
    '''    /**\n     * One rarity is not one salvage value. A chestplate contains more recoverable work than boots,\n''',
    '''    private static void capBaseSalvageToImprintCost(Map<Item, Integer> amounts, int rarity) {\n        for (MaterialCost budget : imprintCosts(rarity - 1)) {\n            Integer current = amounts.get(budget.item());\n            if (current != null && current > budget.count()) amounts.put(budget.item(), budget.count());\n        }\n    }\n\n    /**\n     * One rarity is not one salvage value. A chestplate contains more recoverable work than boots,\n''',
)

# ---------------------------------------------------------------------------
# Harvesting: high-end hoe area bonuses must not be silently truncated at 384.
# Execution remains tick-drained at the existing local/global budgets.
# ---------------------------------------------------------------------------
harvesting = JAVA / "harvesting/HarvestingProgression.java"
replace_once(
    harvesting,
    "    private static final int MAX_PENDING_PER_PLAYER = 384;",
    "    private static final int MAX_PENDING_PER_PLAYER = 1152;",
)

# ---------------------------------------------------------------------------
# Current regression audit: lock the manual findings so they cannot silently return.
# ---------------------------------------------------------------------------
audit = ROOT / "tools/test_current_source.py"
replace_once(audit, 'require("mod_version=0.61.19-alpha.1" in props, "Survival Ascension version drift")',
             'require("mod_version=0.61.20-alpha.1" in props, "Survival Ascension version drift")')
replace_once(audit, 'require(\'VERSION = "0.61.19-alpha.1"\' in main, "source version drift")',
             'require(\'VERSION = "0.61.20-alpha.1"\' in main, "source version drift")')
replace_once(
    audit,
    '''require("AREA_GUARD" in harvesting and "MAX_PENDING_PER_PLAYER" in harvesting and "JOBS.clear()" in harvesting,\n        "harvesting queue bounds/cleanup missing")\n''',
    '''require("AREA_GUARD" in harvesting and "MAX_PENDING_PER_PLAYER" in harvesting and "JOBS.clear()" in harvesting,\n        "harvesting queue bounds/cleanup missing")\nrequire("MAX_PENDING_PER_PLAYER = 1152" in harvesting,\n        "high-end harvesting affixes can be silently truncated by the pending queue")\n''',
)
replace_once(
    audit,
    '''equipment = text(JAVA / "equipment/EquipmentReforgeService.java")\nequipment_ui = text(JAVA / "client/EquipmentRadialMenuScreen.java")\n''',
    '''equipment = text(JAVA / "equipment/EquipmentReforgeService.java")\naffixes = text(JAVA / "equipment/AscensionAffixes.java")\nequipment_ui = text(JAVA / "client/EquipmentRadialMenuScreen.java")\n''',
)
replace_once(
    audit,
    '''require("AscensionAffixes.isAwakened(stack)" in equipment and "Items.NETHERITE_SCRAP" in equipment\n        and "Items.DRAGON_BREATH" in equipment,\n        "awakened salvage does not recover a bounded share of awakening materials")\n''',
    '''require("AscensionAffixes.isAwakened(stack)" in equipment and "Items.NETHERITE_SCRAP" in equipment\n        and "Items.DRAGON_BREATH" in equipment,\n        "awakened salvage does not recover a bounded share of awakening materials")\nrequire("capBaseSalvageToImprintCost(amounts, rarity);" in equipment\n        and "imprintCosts(rarity - 1)" in equipment,\n        "base salvage can exceed the imprint material budget and print rare materials")\nrequire("Only five four-affix combinations exist" in equipment\n        and 'new MaterialCost(Items.ECHO_SHARD, 1, "메아리 조각")' in equipment,\n        "awakened Mythic reroll cost hardening missing")\nrequire("rerollAffixes" in affixes and "previous.containsAll(chosen)" in affixes\n        and "!previous.contains(key)" in affixes,\n        "paid equipment reroll can return the identical affix set")\n''',
)
replace_once(
    audit,
    'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.19 dynamic equipment salvage + canonical freight guide + full skill/runtime invariants")',
    'print("CURRENT SOURCE CHECK PASS: Survival Ascension 0.61.20 equipment economy + distinct rerolls + harvest queue + full skill/runtime invariants")',
)

# ---------------------------------------------------------------------------
# Player/developer documentation: repair only current headers and add a current
# release note; historical release sections remain historical.
# ---------------------------------------------------------------------------
changelog = ROOT / "CHANGELOG.md"
replace_once(
    changelog,
    "# Changelog\n\n",
    '''# Changelog\n\n## 0.61.20-alpha.1\n- Base equipment salvage still scales by equipment body, material quality and remaining durability, but each rarity is now capped so its pre-awakening salvage cannot return more of an imprint material than that rarity's imprint actually consumed. This closes imprint -> salvage diamond/material generation loops while retaining meaningful item-value differences.\n- Paid equipment rerolls can no longer return the identical affix set. If the random subset matches the current set, exactly one affix is replaced with a missing key.\n- Awakened Mythic reroll cost is reduced to amethyst16 + diamond2 + echo1 now that every paid reroll is guaranteed to change the four-affix set.\n- High-end Harvesting queued work cap rises from 384 to 1152 targets so large Mythic hoe areas/forward lanes are not silently truncated. Execution remains bounded at the existing 12 local / 64 global targets per tick.\n- No SavedData schema, packet, network protocol, force-load policy or strong solo-balance affix magnitude changed. Network protocol remains 15.\n\n''',
)

readme = ROOT / "README.md"
replace_once(readme,
             "Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25. Network protocol `9`.",
             "Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25. Network protocol `15`.")
replace_once(
    readme,
    "Survival Ascension makes progression increase the physical scale of player actions, then makes infrastructure, logistics, expeditions and combat consume that larger output again.\n\n",
    '''Survival Ascension makes progression increase the physical scale of player actions, then makes infrastructure, logistics, expeditions and combat consume that larger output again.\n\n## 0.61.20-alpha.1 — Equipment Economy & Harvest Queue Hardening / 장비 경제·수확 큐 안정화\n장비 분해의 부위/재질/내구도 차등 보상은 유지하되, 각 등급의 기본 분해가 해당 등급 각인에 실제로 들어간 희귀 재료보다 더 많이 반환하지 못하도록 상한을 둔다. 각성 재료의 부분 회수는 별도 각성 투자분에서만 추가된다. 따라서 장비 각인→분해로 다이아몬드 같은 희귀 재료를 생성할 수 없다.\n\n유료 재련은 반드시 최소 한 옵션이 달라진다. 각성 신화는 가능한 4옵션 조합이 5개뿐이므로 재련 비용을 자수정16·다이아2·메아리1로 낮췄다. 고등급 괭이의 대형 수확 영역은 1152개 큐까지 받아들이되 실제 작업은 기존 틱당 로컬12/전체64 제한으로 계속 분산한다. 네트워크 프로토콜은 15 그대로이며 저장 데이터 스키마 변경은 없다.\n\n''',
)

project = ROOT / "PROJECT.md"
replace_once(project, "- Mod version: `0.59.0-alpha.1`", "- Mod version: `0.61.20-alpha.1`")
replace_once(project, "- Network protocol: `9`", "- Network protocol: `15`")
replace_once(
    project,
    "- Existing-world compatibility: no new SavedData ID or codec field in 0.59. The new Apex escort bridge is runtime/tag-only and replaces at most one initial non-ocean escort slot; all 0.58 skill/expedition compatibility data, existing skill XP totals, `infrastructure_v1`, `field_depots_v1`, `outpost_v1`, `production_v1`, existing affix CustomData and older data remain unchanged. Network protocol stays 9. The content-preview lock advances only its Survival release identity to `0.59.0-alpha.1-content-preview.1`; the seven external project/version IDs are unchanged.",
    "- Existing-world compatibility: 0.61.20 adds no SavedData ID or codec field and does not bump the network protocol. Existing skill XP, infrastructure/logistics/outpost/production data and affix CustomData remain compatible. Distinct rerolls and salvage caps are server-side service rules; the 1152 Harvesting queue cap is runtime-only. Network protocol remains 15.",
)
replace_once(
    project,
    "## 0.59 Apex Content Escort Integration / 정점 사냥 콘텐츠 호위 연동\n",
    '''## 0.61.20 Equipment Economy & Harvest Queue Hardening / 장비 경제·수확 큐 안정화\n- Dynamic salvage retains equipment/body/material/condition scaling, but pre-awakening rewards are capped by the matching rarity's imprint-material budget.\n- Paid rerolls are guaranteed to change at least one affix; awakened Mythic reroll cost is amethyst16 + diamond2 + echo1.\n- Harvesting accepts up to 1152 queued targets while retaining the existing 12-per-player / 64-global tick drain.\n- No SavedData/protocol/force-load change; strong solo-balance affix magnitudes remain intentionally unchanged.\n\n## 0.59 Apex Content Escort Integration / 정점 사냥 콘텐츠 호위 연동\n''',
)

print("Applied Survival Ascension 0.61.20 manual-audit fixes")
