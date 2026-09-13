from pathlib import Path

ROOT = Path('projects/survival-ascension')
OLD = '0.61.24-alpha.1'
NEW = '0.61.25-alpha.1'


def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8')


def write(rel, text):
    (ROOT / rel).write_text(text, encoding='utf-8')


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected exactly one match, got {count}')
    return text.replace(old, new, 1)

# Server-authoritative auto-salvage preference and transaction.
path = 'src/main/java/kr/moonseungjun/survivalascension/equipment/EquipmentReforgeService.java'
text = read(path)
text = replace_once(text,
'''    public static final int ACTION_IMPRINT = 3;\n''',
'''    public static final int ACTION_IMPRINT = 3;\n    public static final int ACTION_AUTO_SALVAGE_CYCLE = 4;\n    public static final String AUTO_SALVAGE_THRESHOLD_KEY = "survivalascension_auto_salvage_threshold";\n''',
'auto salvage action/key')
text = replace_once(text,
'''        else if (action == ACTION_AWAKEN) awaken(player);\n        else if (action == ACTION_IMPRINT) imprint(player);\n''',
'''        else if (action == ACTION_AWAKEN) awaken(player);\n        else if (action == ACTION_IMPRINT) imprint(player);\n        else if (action == ACTION_AUTO_SALVAGE_CYCLE) cycleAutoSalvage(player);\n''',
'perform auto salvage action')
insert_after = '''    private static void salvage(ServerPlayer player) {\n        ItemStack held = player.getMainHandItem();\n        int rarity = AscensionAffixes.rarity(held);\n        if (rarity <= 0) {\n            player.sendSystemMessage(Component.literal("§c[장비] §f주 손에 정예/승천/신화 장비를 들어야 합니다."));\n            return;\n        }\n        if (player.isCreative()) {\n            player.sendSystemMessage(Component.literal("§e[분해] §f크리에이티브에서는 재료 복제를 막기 위해 분해 보상을 지급하지 않습니다."));\n            return;\n        }\n        String oldName = held.getHoverName().getString();\n        MaterialCost[] rewards = salvageRewards(held);\n        held.shrink(1);\n        for (MaterialCost reward : rewards) give(player, new ItemStack(reward.item(), reward.count()));\n        player.getInventory().setChanged();\n        player.containerMenu.broadcastChanges();\n        player.sendSystemMessage(Component.literal("§b[분해 완료] §f" + oldName + " §7→ §f" + join(rewards)\n                + " §8· 장비 부위/재질 체급/남은 내구도 반영"));\n    }\n'''
addition = insert_after + '''\n    public static int autoSalvageThreshold(ServerPlayer player) {\n        return Math.max(0, Math.min(3, player.getPersistentData().getIntOr(AUTO_SALVAGE_THRESHOLD_KEY, 0)));\n    }\n\n    private static void cycleAutoSalvage(ServerPlayer player) {\n        int next = (autoSalvageThreshold(player) + 1) % 4;\n        player.getPersistentData().putInt(AUTO_SALVAGE_THRESHOLD_KEY, next);\n        String mode = switch (next) {\n            case 1 -> "정예만";\n            case 2 -> "승천 이하";\n            case 3 -> "신화 포함";\n            default -> "꺼짐";\n        };\n        player.sendSystemMessage(Component.literal(next == 0\n                ? "§7[자동 분해] §f꺼짐 · 신규 승천 장비가 다시 바닥에 드롭됩니다."\n                : "§b[자동 분해] §f" + mode + " §7· 정예 처치 신규 장비를 드롭 전에 즉시 재료로 환급합니다. 각성 장비는 항상 보호됩니다."));\n    }\n\n    /**\n     * Converts only newly generated Survival Ascension elite loot. It never scans the player's\n     * inventory or touches manually imprinted/equipped gear, so enabling it cannot silently eat\n     * an existing endgame set. Returns true when the physical gear drop was consumed.\n     */\n    public static boolean tryAutoSalvage(ServerPlayer player, ItemStack stack) {\n        if (player.isCreative() || stack.isEmpty() || AscensionAffixes.isAwakened(stack)) return false;\n        int threshold = autoSalvageThreshold(player);\n        int rarity = AscensionAffixes.rarity(stack);\n        if (threshold <= 0 || rarity <= 0 || rarity > threshold) return false;\n\n        String oldName = stack.getHoverName().getString();\n        MaterialCost[] rewards = salvageRewards(stack);\n        for (MaterialCost reward : rewards) give(player, new ItemStack(reward.item(), reward.count()));\n        player.getInventory().setChanged();\n        player.containerMenu.broadcastChanges();\n        player.sendSystemMessage(Component.literal("§b[자동 분해] §f" + oldName + " §7→ §f" + join(rewards)), true);\n        return true;\n    }\n'''
text = replace_once(text, insert_after, addition, 'auto salvage service methods')
write(path, text)

# Convert generated elite loot before ItemEntity creation. This both avoids inventory labor and
# removes the discarded-gear entity from the world entirely.
path = 'src/main/java/kr/moonseungjun/survivalascension/equipment/AscensionAffixes.java'
text = read(path)
text = replace_once(text,
'''        ItemStack drop = createEliteDrop(level.getRandom(), rankId);\n        level.addFreshEntity(new ItemEntity(level, mob.getX(), mob.getY() + 0.5D, mob.getZ(), drop));\n''',
'''        ItemStack drop = createEliteDrop(level.getRandom(), rankId);\n        if (EquipmentReforgeService.tryAutoSalvage(killer, drop)) return;\n        level.addFreshEntity(new ItemEntity(level, mob.getX(), mob.getY() + 0.5D, mob.getZ(), drop));\n''',
'elite drop auto salvage')
write(path, text)

# Preserve player-local preference through ServerPlayer cloning.
path = 'src/main/java/kr/moonseungjun/survivalascension/progress/PlayerLifecycleState.java'
text = read(path)
text = replace_once(text,
'''            "survivalascension_bulk_tool_wear_bank",\n            "survivalascension_operation_interdiction_stage"\n''',
'''            "survivalascension_bulk_tool_wear_bank",\n            "survivalascension_operation_interdiction_stage",\n            "survivalascension_auto_salvage_threshold"\n''',
'clone auto salvage preference')
write(path, text)

# Add one compact radial entry rather than another screen/settings tree.
path = 'src/main/java/kr/moonseungjun/survivalascension/client/EquipmentRadialMenuScreen.java'
text = read(path)
text = replace_once(text,
'''            new Entry("분해", new ItemStack(Items.GRINDSTONE), Action.SALVAGE),\n            new Entry("장비 정보", new ItemStack(Items.SPYGLASS), Action.INFO),\n''',
'''            new Entry("분해", new ItemStack(Items.GRINDSTONE), Action.SALVAGE),\n            new Entry("자동 분해", new ItemStack(Items.HOPPER), Action.AUTO_SALVAGE),\n            new Entry("장비 정보", new ItemStack(Items.SPYGLASS), Action.INFO),\n''',
'radial auto salvage entry')
text = replace_once(text,
'''        int detailColor=(rarity>0||entry.action()==Action.IMPRINT&&imprintable||entry.action()==Action.BACK)?0xFFE0E0E0:0xFFFF7777;\n''',
'''        int detailColor=(rarity>0||entry.action()==Action.IMPRINT&&imprintable||entry.action()==Action.AUTO_SALVAGE||entry.action()==Action.BACK)?0xFFE0E0E0:0xFFFF7777;\n''',
'radial auto salvage valid color')
text = replace_once(text,
'''        if(action==Action.BACK)return "통합 메뉴로 돌아가기";\n        if(action==Action.IMPRINT){\n''',
'''        if(action==Action.BACK)return "통합 메뉴로 돌아가기";\n        if(action==Action.AUTO_SALVAGE)return "클릭할 때마다 정예 → 승천 이하 → 신화 포함 → 꺼짐";\n        if(action==Action.IMPRINT){\n''',
'radial auto salvage detail')
text = replace_once(text,
'''        return switch(action){case REFORGE->"비용 · "+EquipmentReforgeService.costText(held);case AWAKEN->rarity<3?"신화 III 장비 필요":(AscensionAffixes.isAwakened(held)?"이미 각성 완료":"4번째 승천 옵션 개방");case SALVAGE->"환급 · "+EquipmentReforgeService.salvageText(held);case INFO->AscensionAffixes.effectSummary(held);case IMPRINT->"";case BACK->"통합 메뉴로 돌아가기";};\n''',
'''        return switch(action){case REFORGE->"비용 · "+EquipmentReforgeService.costText(held);case AWAKEN->rarity<3?"신화 III 장비 필요":(AscensionAffixes.isAwakened(held)?"이미 각성 완료":"4번째 승천 옵션 개방");case SALVAGE->"환급 · "+EquipmentReforgeService.salvageText(held);case AUTO_SALVAGE->"자동 분해 설정";case INFO->AscensionAffixes.effectSummary(held);case IMPRINT->"";case BACK->"통합 메뉴로 돌아가기";};\n''',
'radial switch exhaustiveness')
text = replace_once(text,
'''        if(action==Action.BACK){this.minecraft.gui.setScreen(new AscensionRadialMenuScreen());return true;}\n        if(action==Action.INFO)return true;\n        if(this.minecraft.player==null)return true;\n''',
'''        if(action==Action.BACK){this.minecraft.gui.setScreen(new AscensionRadialMenuScreen());return true;}\n        if(action==Action.INFO)return true;\n        if(this.minecraft.player==null)return true;\n        if(action==Action.AUTO_SALVAGE){\n            ClientPacketDistributor.sendToServer(new EquipmentActionPayload(EquipmentReforgeService.ACTION_AUTO_SALVAGE_CYCLE));\n            this.minecraft.gui.setScreen(null);return true;\n        }\n''',
'radial auto salvage click')
text = replace_once(text,
'''        int id=switch(action){case REFORGE->EquipmentReforgeService.ACTION_REFORGE;case AWAKEN->EquipmentReforgeService.ACTION_AWAKEN;case SALVAGE->EquipmentReforgeService.ACTION_SALVAGE;default->-1;};\n''',
'''        int id=switch(action){case REFORGE->EquipmentReforgeService.ACTION_REFORGE;case AWAKEN->EquipmentReforgeService.ACTION_AWAKEN;case SALVAGE->EquipmentReforgeService.ACTION_SALVAGE;default->-1;};\n''',
'radial manual action switch retained')
text = replace_once(text,
'''    private enum Action{IMPRINT,REFORGE,AWAKEN,SALVAGE,INFO,BACK}\n''',
'''    private enum Action{IMPRINT,REFORGE,AWAKEN,SALVAGE,AUTO_SALVAGE,INFO,BACK}\n''',
'radial action enum')
write(path, text)

# Version identity.
path = 'gradle.properties'
text = read(path)
text = replace_once(text, f'mod_version={OLD}', f'mod_version={NEW}', 'gradle version')
text = text.replace('# 0.61.24 Mythic backlog enforcement (2026-09-13).', '# 0.61.25 Endgame auto-salvage QoL (2026-09-13).')
write(path, text)

path = 'src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java'
text = read(path)
text = replace_once(text, f'public static final String VERSION = "{OLD}";', f'public static final String VERSION = "{NEW}";', 'source version')
write(path, text)

# Regression contracts.
path = 'tools/test_current_source.py'
text = read(path)
text = text.replace(f'mod_version={OLD}', f'mod_version={NEW}')
text = text.replace(f'VERSION = "{OLD}"', f'VERSION = "{NEW}"')
needle = '''require("salvageText(held)" in equipment_ui, "salvage UI still previews rarity-only rewards")\n'''
replacement = needle + '''require("ACTION_AUTO_SALVAGE_CYCLE = 4" in equipment and "AUTO_SALVAGE_THRESHOLD_KEY" in equipment,\n        "auto-salvage action/preference authority missing")\nrequire("tryAutoSalvage(ServerPlayer player, ItemStack stack)" in equipment\n        and "rarity > threshold" in equipment and "AscensionAffixes.isAwakened(stack)" in equipment,\n        "auto-salvage threshold/awakened safety missing")\nrequire("EquipmentReforgeService.tryAutoSalvage(killer, drop)" in affixes,\n        "elite generated loot bypasses auto-salvage before ItemEntity creation")\nrequire("survivalascension_auto_salvage_threshold" in text(JAVA / "progress/PlayerLifecycleState.java"),\n        "auto-salvage preference is not preserved across player clone")\nrequire("Action.AUTO_SALVAGE" in equipment_ui and "Items.HOPPER" in equipment_ui\n        and "ACTION_AUTO_SALVAGE_CYCLE" in equipment_ui,\n        "auto-salvage radial control missing")\n'''
text = replace_once(text, needle, replacement, 'current source auto salvage contracts')
write(path, text)

path = 'tools/test_release_source.py'
text = read(path)
text = replace_once(text, f'CURRENT_VERSION = "{OLD}"', f'CURRENT_VERSION = "{NEW}"', 'release current version')
text = replace_once(text, f'PREVIOUS_DOC_VERSION = "{OLD}"', f'PREVIOUS_DOC_VERSION = "{NEW}"', 'release doc version')
write(path, text)

# Docs.
project_section = '''## 0.61.25 Endgame Auto-Salvage / 종결 이후 자동 분해\n- Equipment radial menu adds one compact `자동 분해` control rather than a separate settings screen. Each click cycles OFF → Elite only → Ascended-or-lower → Mythic included → OFF.\n- Auto-salvage applies only to newly generated Survival Ascension elite equipment credited to that killer. It does not scan inventory, equipped items, manually imprinted gear or existing world items.\n- Eligible gear is converted through the exact same body/material/condition-aware salvage reward authority before an ItemEntity is spawned, so it reduces both inventory cleanup and discarded gear entities.\n- Awakened equipment is always excluded. Creative mode never receives auto-salvage material rewards. Preference is server-authoritative and preserved across ServerPlayer clone.\n- No SavedData schema, reward formula, affix roll, packet schema or network protocol change. Protocol remains 15.\n\n'''
path = 'PROJECT.md'
text = read(path)
text = replace_once(text, f'- Mod version: `{OLD}`', f'- Mod version: `{NEW}`', 'PROJECT version')
old_compat = '- Existing-world compatibility: 0.61.24 adds no SavedData ID/codec field and does not bump the network protocol. It intentionally retires loaded Mythic III entities that exceed the 256-block / three-per-dimension cap so old persistent backlogs cannot survive indefinitely; all player progression and valid in-cap Mythics remain compatible. Network protocol remains 15.'
new_compat = '- Existing-world compatibility: 0.61.25 adds only a player-local auto-salvage preference key and no SavedData ID/codec field or packet-schema change. Existing progression, equipment, infrastructure, Mythics and worlds remain compatible. Network protocol remains 15.'
text = replace_once(text, old_compat, new_compat, 'PROJECT compatibility')
text = replace_once(text, '## 0.61.24 Mythic Backlog Enforcement / 신화 누적 개체 정리\n', project_section + '## 0.61.24 Mythic Backlog Enforcement / 신화 누적 개체 정리\n', 'PROJECT auto salvage section')
write(path, text)

readme_section = '''## 0.61.25-alpha.1 — Endgame Auto-Salvage / 종결 이후 자동 분해\n장비 메뉴에 `자동 분해`가 추가된다. 별도 설정창을 늘리지 않고 한 번 누를 때마다 **꺼짐 → 정예만 → 승천 이하 → 신화 포함 → 꺼짐**으로 순환한다.\n\n자동 분해는 인벤토리를 주기적으로 검사하지 않는다. 플레이어가 정예/승천/신화 적을 처치해 Survival Ascension 장비 드롭이 새로 만들어지는 순간에만 적용하며, 설정한 등급 이하라면 바닥에 장비 ItemEntity를 만들기 전에 기존 수동 분해와 동일한 환급 계산으로 바로 재료화한다. 따라서 종결 이후의 장비 정리 노동과 불필요한 드롭 엔티티를 함께 줄인다.\n\n기존 인벤토리/착용 장비/수동 각인 장비는 절대 자동으로 건드리지 않고, 각성 장비는 항상 보호한다. 설정은 서버 권한이며 사망/차원 이동으로 ServerPlayer가 교체되어도 유지된다. 저장 스키마와 네트워크 프로토콜 15는 그대로다.\n\n'''
path = 'README.md'
text = read(path)
text = replace_once(text, '## 0.61.24-alpha.1 — Mythic Backlog Enforcement / 신화 누적 개체 정리\n', readme_section + '## 0.61.24-alpha.1 — Mythic Backlog Enforcement / 신화 누적 개체 정리\n', 'README auto salvage section')
write(path, text)

change = '''## 0.61.25-alpha.1\n- Added a compact equipment-menu auto-salvage control: OFF → Elite → Ascended-or-lower → Mythic included → OFF.\n- Auto-salvage converts only newly generated Survival Ascension elite loot before ItemEntity creation, using the exact existing dynamic salvage reward calculation. Existing inventory/equipped/imprinted gear is never scanned.\n- Awakened gear is always protected, creative mode cannot receive salvage materials, and the server-authoritative preference survives Player clone.\n- Reduces endgame inventory cleanup and unnecessary discarded gear entities without changing affix rolls, salvage economy, packet schema or protocol 15.\n\n'''
path = 'CHANGELOG.md'
text = read(path)
text = replace_once(text, '# Changelog\n\n', '# Changelog\n\n' + change, 'CHANGELOG auto salvage')
write(path, text)

print('Survival Ascension 0.61.25 auto-salvage patch applied.')
