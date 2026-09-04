from pathlib import Path

root = Path('projects/survival-ascension')
java = root / 'src/main/java/kr/moonseungjun/survivalascension'

# Fishing joins the generic SavedData/network-backed skill map without schema migration.
skill_type = java / 'progress/SkillType.java'
s = skill_type.read_text(encoding='utf-8')
old = '    HARVESTING("harvesting", "농사", 0xF4D35E),\n    COMBAT("combat", "전투", 0xFF6B6B),'
new = '    HARVESTING("harvesting", "농사", 0xF4D35E),\n    FISHING("fishing", "낚시", 0x4FC3F7),\n    COMBAT("combat", "전투", 0xFF6B6B),'
if s.count(old) != 1:
    raise SystemExit('SkillType insertion anchor drift')
skill_type.write_text(s.replace(old, new, 1), encoding='utf-8')

# Keep the opening pacing, flatten the post-Lv20 wall, then grow in readable stages.
tuning = java / 'progress/SkillTuning.java'
s = tuning.read_text(encoding='utf-8')
start = s.index('    public static long xpForNextLevel(int currentLevel) {')
end = s.index('    public static long xpAtLevel(int targetLevel) {', start)
replacement = '''    public static long xpForNextLevel(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) return 0L;
        int level = Math.max(0, currentLevel);
        if (level < 20) {
            long base = 40L + 8L * level + Math.round(1.5D * level * level);
            double factor = level < 10
                    ? 0.20D + 0.03D * level
                    : 0.50D + 0.015D * (level - 10);
            return Math.max(8L, Math.round(base * factor));
        }
        if (level < 30) return 430L + 15L * (level - 20);
        if (level < 60) {
            int x = level - 30;
            return Math.round(600.0D + 35.0D * x + 0.5D * x * x);
        }
        if (level < 90) {
            int x = level - 60;
            return Math.round(2100.0D + 70.0D * x + 1.0D * x * x);
        }
        return 5100L + 220L * (level - 90);
    }

'''
s = s[:start] + replacement + s[end:]
anchor = '            case HARVESTING -> { early = 1.50D; late = 1.20D; }\n            case COMBAT -> { early = 1.25D; late = 1.15D; }'
if s.count(anchor) != 1:
    raise SystemExit('SkillTuning skill multiplier anchor drift')
s = s.replace(anchor, '            case HARVESTING -> { early = 1.50D; late = 1.20D; }\n            case FISHING -> { early = 3.00D; late = 2.50D; }\n            case COMBAT -> { early = 1.25D; late = 1.15D; }', 1)
anchor = '    public static double combatDamageMultiplier(int level) {'
if s.count(anchor) != 1:
    raise SystemExit('SkillTuning fishing perk anchor drift')
fishing_perk = '''    public static double fishingRodPreservationChance(int level) {
        if (level >= 100) return 0.65D;
        if (level >= 90) return 0.50D;
        if (level >= 60) return 0.35D;
        if (level >= 30) return 0.20D;
        if (level >= 10) return 0.10D;
        return 0.0D;
    }

'''
s = s.replace(anchor, fishing_perk + anchor, 1)
tuning.write_text(s, encoding='utf-8')

# Automatic extra blocks still cost durability, but at one quarter of vanilla rate.
helper = java / 'progress/AutomatedToolBreak.java'
helper.write_text('''package kr.moonseungjun.survivalascension.progress;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Skill-expanded work keeps vanilla block-break hooks and still wears tools, but automatic
 * extra blocks only pay one normal vanilla durability roll per four successful extra blocks.
 * The player's original manual break always remains vanilla-authoritative.
 */
public final class AutomatedToolBreak {
    private static final String WEAR_BANK_KEY = "survivalascension_bulk_tool_wear_bank";
    private static final int AUTOMATIC_BLOCKS_PER_WEAR = 4;

    private AutomatedToolBreak() {}

    public static boolean destroyWithReducedWear(ServerPlayer player, BlockPos target) {
        ItemStack tool = player.getMainHandItem();
        if (player.isCreative() || tool.isEmpty() || !tool.isDamageableItem()) {
            return player.gameMode.destroyBlock(target);
        }

        int bank = Math.max(0, player.getPersistentData().getIntOr(WEAR_BANK_KEY, 0));
        if (bank >= AUTOMATIC_BLOCKS_PER_WEAR - 1) {
            boolean broken = player.gameMode.destroyBlock(target);
            if (broken) player.getPersistentData().putInt(WEAR_BANK_KEY, 0);
            return broken;
        }

        int damageBefore = tool.getDamageValue();
        tool.setDamageValue(0);
        boolean broken;
        try {
            broken = player.gameMode.destroyBlock(target);
        } finally {
            ItemStack held = player.getMainHandItem();
            if (!held.isEmpty() && held.getItem() == tool.getItem() && held.isDamageableItem()) {
                held.setDamageValue(damageBefore);
            }
        }
        if (broken) player.getPersistentData().putInt(WEAR_BANK_KEY, bank + 1);
        return broken;
    }
}
''', encoding='utf-8')

# AUTO already protects ores; make PLANE ore-safe too and route every automatic break through reduced wear.
mining = java / 'mining/MiningProgression.java'
s = mining.read_text(encoding='utf-8')
old_call = 'AutomatedToolBreak.destroyWithoutAdditionalWear'
if s.count(old_call) != 4:
    raise SystemExit(f'MiningProgression expected 4 old durability helper calls, got {s.count(old_call)}')
s = s.replace(old_call, 'AutomatedToolBreak.destroyWithReducedWear')
old_plane = '''                case PLANE -> {
                    if (areaSize > 1) breakArea(player, level, center, areaSize, Math.max(0.0F, centerState.getDestroySpeed(level, center)));
                }'''
new_plane = '''                case PLANE -> {
                    if (centerState.is(VALUABLE_ORES) && veinLimit > 1) breakConnectedOre(player, level, center, centerState, veinLimit);
                    else if (areaSize > 1) breakArea(player, level, center, areaSize, Math.max(0.0F, centerState.getDestroySpeed(level, center)));
                }'''
if s.count(old_plane) != 1:
    raise SystemExit('MiningProgression PLANE anchor drift')
mining.write_text(s.replace(old_plane, new_plane, 1), encoding='utf-8')

bore = java / 'mining/BoreMiningService.java'
s = bore.read_text(encoding='utf-8')
if s.count('AutomatedToolBreak.destroyWithoutAdditionalWear') != 1:
    raise SystemExit('Bore durability helper anchor drift')
bore.write_text(s.replace('AutomatedToolBreak.destroyWithoutAdditionalWear', 'AutomatedToolBreak.destroyWithReducedWear'), encoding='utf-8')

wood = java / 'woodcutting/WoodcuttingProgression.java'
s = wood.read_text(encoding='utf-8')
if s.count('AutomatedToolBreak.destroyWithoutAdditionalWear') != 1:
    raise SystemExit('Woodcutting durability helper anchor drift')
wood.write_text(s.replace('AutomatedToolBreak.destroyWithoutAdditionalWear', 'AutomatedToolBreak.destroyWithReducedWear'), encoding='utf-8')

mining_ui = java / 'client/MiningRadialMenuScreen.java'
s = mining_ui.read_text(encoding='utf-8')
s = s.replace('new Entry("자동", "광석=광맥 / 일반=굴착"', 'new Entry("자동", "광석=같은 종류 광맥 / 일반=굴착"')
s = s.replace('new Entry("굴착", "Lv.10 · 항상 시선 평면 광역"', 'new Entry("굴착", "Lv.10 · 일반=평면 / 광석=같은 종류 광맥 보호"')
mining_ui.write_text(s, encoding='utf-8')

# Fishing skill: successful catches grant skill XP; mastery gradually saves rod wear.
fishing = java / 'fishing/FishingProgression.java'
fishing.parent.mkdir(parents=True, exist_ok=True)
fishing.write_text('''package kr.moonseungjun.survivalascension.fishing;

import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.progress.SkillTuning;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

public final class FishingProgression {
    private FishingProgression() {}

    public static void onItemFished(ItemFishedEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        int oldLevel = SkillProgressData.get(player).level(player, SkillType.FISHING);
        int rawXp = xpForCatch(event);
        SkillProgressData.AddXpResult result = SkillProgressionService.award(player, SkillType.FISHING, rawXp);
        preserveRod(player, event, oldLevel);
        announceMilestones(player, result);
    }

    private static int xpForCatch(ItemFishedEvent event) {
        int xp = 8;
        for (ItemStack stack : event.getDrops()) {
            if (stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.PUFFERFISH)) {
                xp += 4;
            } else if (stack.is(Items.ENCHANTED_BOOK) || stack.is(Items.NAME_TAG) || stack.is(Items.NAUTILUS_SHELL)
                    || stack.is(Items.SADDLE) || stack.is(Items.BOW) || stack.is(Items.FISHING_ROD)) {
                xp += 12;
            } else {
                xp += 2;
            }
        }
        return Math.max(8, Math.min(32, xp));
    }

    private static void preserveRod(ServerPlayer player, ItemFishedEvent event, int level) {
        int damage = event.getRodDamage();
        if (damage <= 0) return;
        double chance = SkillTuning.fishingRodPreservationChance(level);
        if (chance > 0.0D && player.getRandom().nextDouble() < chance) {
            event.damageRodBy(Math.max(0, damage - 1));
        }
    }

    private static void announceMilestones(ServerPlayer player, SkillProgressData.AddXpResult result) {
        if (!result.leveledUp()) return;
        int oldLevel = result.oldLevel(), newLevel = result.newLevel();
        if (oldLevel < 10 && newLevel >= 10) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f낚싯대 마모 방지 10%"));
        if (oldLevel < 30 && newLevel >= 30) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f낚싯대 마모 방지 20%"));
        if (oldLevel < 60 && newLevel >= 60) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f낚싯대 마모 방지 35%"));
        if (oldLevel < 90 && newLevel >= 90) player.sendSystemMessage(Component.literal("§3[낚시 해금] §f낚싯대 마모 방지 50%"));
        if (oldLevel < 100 && newLevel >= 100) player.sendSystemMessage(Component.literal("§3[낚시 숙련 VI] §f낚싯대 마모 방지 65%"));
    }
}
''', encoding='utf-8')

main = java / 'SurvivalAscension.java'
s = main.read_text(encoding='utf-8')
import_anchor = 'import kr.moonseungjun.survivalascension.harvesting.IrrigationReplantService;'
if s.count(import_anchor) != 1:
    raise SystemExit('SurvivalAscension fishing import anchor drift')
s = s.replace(import_anchor, import_anchor + '\nimport kr.moonseungjun.survivalascension.fishing.FishingProgression;', 1)
listener_anchor = '        NeoForge.EVENT_BUS.addListener(IrrigationReplantService::onServerTick);'
if s.count(listener_anchor) != 1:
    raise SystemExit('SurvivalAscension fishing listener anchor drift')
s = s.replace(listener_anchor, listener_anchor + '\n        NeoForge.EVENT_BUS.addListener(FishingProgression::onItemFished);', 1)
main.write_text(s, encoding='utf-8')

skills_ui = java / 'client/SkillsScreen.java'
s = skills_ui.read_text(encoding='utf-8')
anchor = '            case HARVESTING -> "수확 " + SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level) + " · 속도 " + format(SkillTuning.harvestingSpeedMultiplier(level));\n            case COMBAT ->'
if s.count(anchor) != 1:
    raise SystemExit('SkillsScreen fishing row anchor drift')
fishing_case = '            case HARVESTING -> "수확 " + SkillTuning.harvestingAreaSize(level) + "×" + SkillTuning.harvestingAreaSize(level) + " · 속도 " + format(SkillTuning.harvestingSpeedMultiplier(level));\n            case FISHING -> "낚싯대 마모 방지 " + Math.round(SkillTuning.fishingRodPreservationChance(level) * 100.0D) + "% · 어획 성공으로 숙련";\n            case COMBAT ->'
skills_ui.write_text(s.replace(anchor, fishing_case, 1), encoding='utf-8')

# Focused persistent regression audit for this playtest-driven pass.
audit = root / 'tools/test_gameplay_qol_061.py'
audit.write_text('''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
errors = []

def read(rel):
    p = ROOT / rel
    if not p.exists():
        errors.append(f"missing {rel}")
        return ""
    return p.read_text(encoding="utf-8")

def need(text, needles, label):
    for n in needles:
        if n not in text:
            errors.append(f"{label} missing: {n}")

skill = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillType.java")
tuning = read("src/main/java/kr/moonseungjun/survivalascension/progress/SkillTuning.java")
helper = read("src/main/java/kr/moonseungjun/survivalascension/progress/AutomatedToolBreak.java")
mining = read("src/main/java/kr/moonseungjun/survivalascension/mining/MiningProgression.java")
bore = read("src/main/java/kr/moonseungjun/survivalascension/mining/BoreMiningService.java")
wood = read("src/main/java/kr/moonseungjun/survivalascension/woodcutting/WoodcuttingProgression.java")
fishing = read("src/main/java/kr/moonseungjun/survivalascension/fishing/FishingProgression.java")
main = read("src/main/java/kr/moonseungjun/survivalascension/SurvivalAscension.java")
ui = read("src/main/java/kr/moonseungjun/survivalascension/client/SkillsScreen.java")
mining_ui = read("src/main/java/kr/moonseungjun/survivalascension/client/MiningRadialMenuScreen.java")

need(skill, ['FISHING("fishing", "낚시"'], "fishing skill enum")
need(tuning, ["if (level < 30) return 430L + 15L * (level - 20);", "fishingRodPreservationChance", "case FISHING"], "post20 XP/fishing tuning")
need(helper, ["AUTOMATIC_BLOCKS_PER_WEAR = 4", "destroyWithReducedWear", "player.gameMode.destroyBlock(target)"], "reduced bulk wear")
if mining.count("AutomatedToolBreak.destroyWithReducedWear") != 4:
    errors.append("mining automatic break paths != 4")
if bore.count("AutomatedToolBreak.destroyWithReducedWear") != 1:
    errors.append("bore automatic break path != 1")
if wood.count("AutomatedToolBreak.destroyWithReducedWear") != 1:
    errors.append("wood automatic break path != 1")
need(mining, ["case PLANE -> {", "centerState.is(VALUABLE_ORES) && veinLimit > 1", "breakConnectedOre(player, level, center, centerState, veinLimit)"], "ore-safe plane mining")
need(mining_ui, ["광석=같은 종류 광맥", "광석=같은 종류 광맥 보호"], "ore-safe mining UI")
need(fishing, ["ItemFishedEvent", "SkillType.FISHING", "xpForCatch", "damageRodBy", "fishingRodPreservationChance"], "fishing runtime")
need(main, ["FishingProgression::onItemFished"], "fishing event wiring")
need(ui, ["case FISHING", "낚싯대 마모 방지"], "fishing skill UI")
if "destroyWithoutAdditionalWear" in helper + mining + bore + wood:
    errors.append("obsolete zero-wear bulk helper remains")

if errors:
    print("GAMEPLAY QOL AUDIT FAIL")
    for e in errors:
        print("-", e)
    raise SystemExit(1)
print("GAMEPLAY QOL AUDIT PASS")
print("post20_xp_curve=RETUNED")
print("ore_family_protection=AUTO_AND_PLANE")
print("bulk_tool_wear=ONE_VANILLA_ROLL_PER_4_AUTOMATIC_BLOCKS")
print("fishing_skill=WIRED")
''', encoding='utf-8')

print('SURVIVAL GAMEPLAY QOL PATCH APPLIED')
