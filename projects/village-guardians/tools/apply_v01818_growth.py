#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_once(path: Path, old: str, new: str) -> None:
    text = read(path)
    if old not in text:
        raise SystemExit(f"missing replacement anchor in {path}: {old[:120]!r}")
    write(path, text.replace(old, new, 1))


def sub_once(path: Path, pattern: str, replacement: str) -> None:
    text = read(path)
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f"expected one regex replacement in {path}, got {count}: {pattern[:120]!r}")
    write(path, updated)


# Version: v0.18.17 is already reserved by the UI rollback workflow, so this pass is v0.18.18.
replace_once(ROOT / "gradle.properties", "mod_version=0.18.16-alpha.1", "mod_version=0.18.18-alpha.1")

# New tactical consumables. Daily bread remains the one baseline food; paid food becomes tactical utility.
consumable = r'''package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Stackable tactical supplies sold by the storehouse. Buff state is intentionally transient. */
public final class VillageConsumableSystem {
    private static final Map<String, Long> READY_AT = new LinkedHashMap<>();
    private static final Map<UUID, Long> ARCANE_SURGE_UNTIL = new LinkedHashMap<>();

    private VillageConsumableSystem() {}

    public static void resetTransientState() {
        READY_AT.clear();
        ARCANE_SURGE_UNTIL.clear();
    }

    public static List<Consumable> catalog() {
        return List.of(Consumable.values());
    }

    public static boolean unlocked(Consumable consumable) {
        return consumable != null && VillageCouncilState.currentDay() >= consumable.requiredDay();
    }

    public static int effectiveCost(Consumable consumable) {
        if (consumable == null) return 0;
        return Math.max(12, consumable.baseCost() - VillageProgressionSystem.storehouseLevel() * 2);
    }

    public static int bundleCount(Consumable consumable) {
        if (consumable == null) return 0;
        int logisticsBonus = consumable == Consumable.BANDAGE
                ? VillageProgressionSystem.storehouseLevel() / 2 : 0;
        return consumable.baseBundle() + logisticsBonus;
    }

    public static String status(Consumable consumable) {
        if (consumable == null) return "알 수 없음";
        if (!unlocked(consumable)) return "제 " + consumable.requiredDay() + "일 해금";
        return "구매 가능";
    }

    public static String purchase(ServerPlayer player, String id) {
        Consumable consumable = Consumable.fromId(id);
        if (consumable == null) return "알 수 없는 전투 소모품입니다.";
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.STOREHOUSE)) {
            return "상점·보급소가 파괴되어 전투 소모품을 구매할 수 없습니다.";
        }
        if (!unlocked(consumable)) return "제 " + consumable.requiredDay() + "일부터 입고됩니다.";
        int cost = effectiveCost(consumable);
        if (!VillageProgressionSystem.spendCoins(player, cost)) {
            return "수호 주화가 부족합니다. 필요 " + cost + ", 현재 " + VillageProgressionSystem.coins(player);
        }
        ItemStack stack = consumable.item().getDefaultInstance();
        stack.setCount(bundleCount(consumable));
        stack.set(DataComponents.CUSTOM_NAME,
                Component.literal(consumable.displayName()).withStyle(consumable.color()));
        if (!player.addItem(stack)) player.drop(stack, false);
        return consumable.displayName() + " ×" + stack.getCount() + " 구매 완료 | 남은 주화 "
                + VillageProgressionSystem.coins(player);
    }

    public static boolean handleItemInteraction(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) return false;
        ItemStack stack = player.getItemInHand(event.getHand());
        Consumable consumable = match(stack);
        if (consumable == null) return false;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (VillageRespawnSystem.isDowned(player)) {
            player.sendSystemMessage(Component.literal("§c부활 대기 중에는 전투 소모품을 사용할 수 없습니다."));
            return true;
        }
        long now = player.level().getGameTime();
        String key = player.getUUID() + "|" + consumable.id();
        long readyAt = READY_AT.getOrDefault(key, 0L);
        if (readyAt > now) {
            player.sendSystemMessage(Component.literal("§e" + consumable.displayName() + " 재사용까지 "
                    + Math.max(1L, (readyAt - now + 19L) / 20L) + "초"));
            return true;
        }
        String result = apply(player, consumable, now);
        if (result == null || result.isBlank()) return true;
        stack.shrink(1);
        player.getInventory().setChanged();
        READY_AT.put(key, now + consumable.cooldownTicks());
        player.sendSystemMessage(Component.literal("§a[전투 소모품] §f" + result));
        return true;
    }

    public static float skillMultiplier(ServerPlayer player) {
        if (player == null) return 1.0f;
        long until = ARCANE_SURGE_UNTIL.getOrDefault(player.getUUID(), 0L);
        if (until <= player.level().getGameTime()) {
            ARCANE_SURGE_UNTIL.remove(player.getUUID());
            return 1.0f;
        }
        return 1.20f;
    }

    private static String apply(ServerPlayer player, Consumable consumable, long now) {
        return switch (consumable) {
            case BANDAGE -> {
                if (player.getHealth() >= player.getMaxHealth()) {
                    player.sendSystemMessage(Component.literal("§e체력이 가득 차 있어 응급 붕대를 사용하지 않았습니다."));
                    yield "";
                }
                float heal = 6.0f + VillageProgressionSystem.storehouseLevel() * 0.8f;
                player.heal(heal);
                yield "응급 붕대 사용 · 체력 " + String.format(Locale.ROOT, "%.1f", heal / 2.0f) + "칸 회복";
            }
            case CLEANSER -> {
                boolean affected = player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER)
                        || player.hasEffect(MobEffects.WEAKNESS) || player.hasEffect(MobEffects.SLOWNESS)
                        || player.hasEffect(MobEffects.DARKNESS);
                if (!affected) {
                    player.sendSystemMessage(Component.literal("§e정화할 약화 효과가 없어 정화 약제를 사용하지 않았습니다."));
                    yield "";
                }
                player.removeEffect(MobEffects.POISON);
                player.removeEffect(MobEffects.WITHER);
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.SLOWNESS);
                player.removeEffect(MobEffects.DARKNESS);
                player.heal(2.0f);
                yield "정화 약제 사용 · 독/위더/약화/둔화/암흑 제거";
            }
            case STIMULANT -> {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 15, 1));
                player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 15, 0));
                yield "전투 자극제 사용 · 15초간 신속 II + 힘 I";
            }
            case AEGIS_TONIC -> {
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 20, 1));
                yield "수호 비약 사용 · 20초간 피해 저항 + 흡수 보호막";
            }
            case ARCANE_CATALYST -> {
                ARCANE_SURGE_UNTIL.put(player.getUUID(), now + 20 * 20L);
                yield "비전 촉진제 사용 · 20초간 직업 기술 피해·치유 +20%";
            }
            case FIELD_REPAIR_KIT -> {
                String repaired = VillagePlacedTurretSystem.fieldRepairNearest(player,
                        70 + VillageProgressionSystem.storehouseLevel() * 15);
                if (repaired.isBlank()) {
                    player.sendSystemMessage(Component.literal("§e12블록 안에 수리가 필요한 가동 포탑이 없어 키트를 사용하지 않았습니다."));
                    yield "";
                }
                yield repaired;
            }
        };
    }

    private static Consumable match(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        if (custom == null) return null;
        String plain = ChatFormatting.stripFormatting(custom.getString());
        for (Consumable consumable : Consumable.values()) {
            if (stack.getItem() == consumable.item() && consumable.displayName().equals(plain)) return consumable;
        }
        return null;
    }

    public enum Consumable {
        BANDAGE("bandage", "응급 붕대", Items.PAPER, 1, 28, 2, 20 * 6,
                "즉시 체력 회복 · 보급소 레벨이 높을수록 회복량/묶음 증가", ChatFormatting.WHITE),
        CLEANSER("cleanser", "정화 약제", Items.HONEYCOMB, 2, 36, 1, 20 * 12,
                "독·위더·약화·둔화·암흑 제거 + 소량 회복", ChatFormatting.YELLOW),
        STIMULANT("stimulant", "전투 자극제", Items.SUGAR, 3, 48, 1, 20 * 40,
                "15초간 신속 II + 힘 I", ChatFormatting.RED),
        AEGIS_TONIC("aegis_tonic", "수호 비약", Items.PRISMARINE_CRYSTALS, 5, 62, 1, 20 * 45,
                "20초간 피해 저항 + 흡수 보호막", ChatFormatting.AQUA),
        ARCANE_CATALYST("arcane_catalyst", "비전 촉진제", Items.AMETHYST_SHARD, 6, 72, 1, 20 * 50,
                "20초간 직업 기술 피해·치유 +20%", ChatFormatting.LIGHT_PURPLE),
        FIELD_REPAIR_KIT("field_repair_kit", "응급 포탑 수리 키트", Items.IRON_INGOT, 7, 84, 1, 20 * 18,
                "12블록 안의 손상된 가동 포탑을 전투 중에도 즉시 수리", ChatFormatting.GOLD);

        private final String id, displayName, description;
        private final Item item;
        private final int requiredDay, baseCost, baseBundle, cooldownTicks;
        private final ChatFormatting color;

        Consumable(String id, String displayName, Item item, int requiredDay, int baseCost,
                   int baseBundle, int cooldownTicks, String description, ChatFormatting color) {
            this.id = id; this.displayName = displayName; this.item = item; this.requiredDay = requiredDay;
            this.baseCost = baseCost; this.baseBundle = baseBundle; this.cooldownTicks = cooldownTicks;
            this.description = description; this.color = color;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public Item item() { return item; }
        public int requiredDay() { return requiredDay; }
        public int baseCost() { return baseCost; }
        public int baseBundle() { return baseBundle; }
        public int cooldownTicks() { return cooldownTicks; }
        public String description() { return description; }
        public ChatFormatting color() { return color; }

        public static Consumable fromId(String id) {
            if (id == null) return null;
            String normalized = id.toLowerCase(Locale.ROOT);
            for (Consumable value : values()) if (value.id.equals(normalized)) return value;
            return null;
        }
    }
}
'''
write(JAVA / "VillageConsumableSystem.java", consumable)

# Event integration.
guardians = JAVA / "VillageGuardians.java"
replace_once(guardians,
             "        VillageRoleAbilitySystem.reset();\n        VillageRpgSystem.resetTransientState();",
             "        VillageRoleAbilitySystem.reset();\n        VillageConsumableSystem.resetTransientState();\n        VillageRpgSystem.resetTransientState();")
replace_once(guardians,
             "    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {\n        VillageStarterKit.handleItemInteraction(event);\n    }",
             "    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {\n        if (VillageConsumableSystem.handleItemInteraction(event)) return;\n        VillageStarterKit.handleItemInteraction(event);\n    }")

# Progression ownership: research hall owns skill efficiency; barracks owns XP/mercenaries.
progress = JAVA / "VillageProgressionSystem.java"
replace_once(progress,
'''    public static synchronized float learnedSkillDamageMultiplier(ServerPlayer player) {
        return 1.0f + skillRank(player) * 0.08f;
    }
''',
'''    public static synchronized float learnedSkillDamageMultiplier(ServerPlayer player) {
        return 1.0f + skillRank(player) * 0.08f;
    }

    public static synchronized float skillHallPowerMultiplier() {
        return isOperational(Building.SKILL_HALL) ? 1.0f + skillHallLevel * 0.05f : 1.0f;
    }

    public static synchronized float skillHallDurationMultiplier() {
        return isOperational(Building.SKILL_HALL) ? 1.0f + skillHallLevel * 0.05f : 1.0f;
    }
''')
replace_once(progress,
'''    public static synchronized int skillDurationBonusTicks(ServerPlayer player) {
        return barracksLevel * 40 + skillRank(player) * 60;
    }

    public static synchronized int skillCooldownReductionSeconds(ServerPlayer player) {
        return barracksLevel * 2 + skillRank(player);
    }
''',
'''    public static synchronized int skillDurationBonusTicks(ServerPlayer player) {
        return skillHallLevel * 30 + skillRank(player) * 60;
    }

    public static synchronized int skillCooldownReductionSeconds(ServerPlayer player) {
        int research = isOperational(Building.SKILL_HALL) ? skillHallLevel : 0;
        int barracksSupport = isOperational(Building.BARRACKS) ? barracksLevel / 2 : 0;
        return Math.min(7, research + barracksSupport + skillRank(player) / 2);
    }
''')
text = read(progress)
text = text.replace('"오늘의 빵 보급은 이미 받았습니다."', '"오늘의 배급 식량은 이미 받았습니다."')
text = text.replace('Component.literal("마을 배급빵")', 'Component.literal("마을 배급 식량")')
text = text.replace('return "오늘의 빵 " + count + "개를 받았습니다.";', 'return "오늘의 배급 식량 " + count + "개를 받았습니다.";')
write(progress, text)
sub_once(progress,
         r'    public static synchronized String buyFood\(ServerPlayer player\) \{.*?\n    \}\n\n    public static synchronized String improveForgeRank',
'''    public static synchronized String buyFood(ServerPlayer player) {
        return "유료 일반 식량은 일일 배급 식량으로 통합되었습니다. 상점의 전투 소모품을 이용하세요.";
    }

    public static synchronized String improveForgeRank''')

# Skill casts receive research-hall and temporary arcane-catalyst power; duration gets hall scaling.
role_skill = JAVA / "VillageRoleSkillSystem.java"
replace_once(role_skill,
'''        float power = powerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)
                * VillageEquipmentShop.roleSkillMultiplier(player)
                * VillageRelicSystem.skillMultiplier(player);
        float duration = durationMultiplier(player, role);
''',
'''        float power = powerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)
                * VillageProgressionSystem.skillHallPowerMultiplier()
                * VillageEquipmentShop.roleSkillMultiplier(player)
                * VillageRelicSystem.skillMultiplier(player)
                * VillageConsumableSystem.skillMultiplier(player);
        float duration = durationMultiplier(player, role) * VillageProgressionSystem.skillHallDurationMultiplier();
''')
replace_once(role_skill,
'''        cast(level, player, skill, powerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)
                * VillageRelicSystem.skillMultiplier(player),
                durationMultiplier(player, role), specialRank(player, role));
''',
'''        cast(level, player, skill, powerMultiplier(player, role)
                * VillageProgressionSystem.learnedSkillDamageMultiplier(player)
                * VillageProgressionSystem.skillHallPowerMultiplier()
                * VillageRelicSystem.skillMultiplier(player)
                * VillageConsumableSystem.skillMultiplier(player),
                durationMultiplier(player, role) * VillageProgressionSystem.skillHallDurationMultiplier(),
                specialRank(player, role));
''')

# Long-tail equipment enhancement with per-family hard caps and diminishing returns.
equipment = JAVA / "VillageEquipmentRaritySystem.java"
replace_once(equipment, "    private static final int MAX_ENHANCEMENT = 5;", "    private static final int MAX_ENHANCEMENT = 30;")
replace_once(equipment,
'''        List<EnhancementCandidate> result = new ArrayList<>();
        int maximum = maximumEnhancement();
        int limit = Math.min(MAIN_INVENTORY_SLOTS, player.getInventory().getContainerSize());
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            Rarity rarity = rarityOf(stack);
            if (rarity == null || !isUpgradeable(stack.getItem())) continue;
            int current = enhancementLevel(stack);
''',
'''        List<EnhancementCandidate> result = new ArrayList<>();
        int limit = Math.min(MAIN_INVENTORY_SLOTS, player.getInventory().getContainerSize());
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            Rarity rarity = rarityOf(stack);
            if (rarity == null || !isUpgradeable(stack.getItem())) continue;
            int current = enhancementLevel(stack);
            int maximum = maximumEnhancement(stack);
''')
replace_once(equipment, "        int maximum = maximumEnhancement();\n        if (current >= maximum) {",
             "        int maximum = maximumEnhancement(stack);\n        if (current >= maximum) {")
sub_once(equipment,
         r'    public static String enhancementEffectSummary\(ItemStack stack, int enhancement\) \{.*?\n    public static float incomingMultiplier',
'''    public static String enhancementEffectSummary(ItemStack stack, int enhancement) {
        Rarity rarity = rarityOf(stack);
        if (rarity == null) return "등급 효과 없음";
        int safe = Math.max(0, enhancement);
        Item item = stack.getItem();
        if (isMelee(item) || isProjectile(item)) {
            float value = 1.0f + rarity.powerStep() * 0.055f + enhancementAttackBonus(safe);
            String type = isMelee(item) ? "근접" : "원거리";
            return String.format(java.util.Locale.ROOT, "%s 피해 x%.3f (+%.1f%%)",
                    type, value, (value - 1.0f) * 100.0f);
        }
        if (isArmor(item) || item == Items.SHIELD) {
            float reduction = rarity.powerStep() * 0.012f + enhancementDefenseBonus(safe);
            return String.format(java.util.Locale.ROOT, "장비 단독 피해 감소 %.1f%%", reduction * 100.0f);
        }
        if (item == Items.BLAZE_ROD) {
            float value = 1.0f + rarity.powerStep() * 0.035f + enhancementSkillBonus(safe);
            return String.format(java.util.Locale.ROOT, "직업 기술 효과 x%.3f (+%.1f%%)",
                    value, (value - 1.0f) * 100.0f);
        }
        return "강화 단계 +" + safe;
    }

    public static int maximumEnhancement() {
        return MAX_ENHANCEMENT;
    }

    public static int maximumEnhancement(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !isUpgradeable(stack.getItem())) return 0;
        int smithy = VillageProgressionSystem.smithyLevel();
        int forgeCap = switch (smithy) {
            case 0 -> 1;
            case 1 -> 4;
            case 2 -> 7;
            case 3 -> 10;
            case 4 -> 14;
            default -> 18;
        };
        int endlessBonus = Math.max(0, (VillageCouncilState.currentDay() - 10) / 2);
        return Math.min(hardEnhancementCap(stack.getItem()), forgeCap + endlessBonus);
    }

    public static int enhancementCost(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        int current = enhancementLevel(stack);
        int masterwork = Math.max(0, current - 10);
        return 70 + current * 90 + masterwork * masterwork * 9
                + (rarity == null ? 0 : rarity.powerStep() * 30);
    }

    public static float meleeMultiplier(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        if (rarity == null || !isMelee(stack.getItem())) return 1.0f;
        return 1.0f + rarity.powerStep() * 0.055f + enhancementAttackBonus(enhancementLevel(stack));
    }

    public static float projectileMultiplier(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        if (rarity == null || !isProjectile(stack.getItem())) return 1.0f;
        return 1.0f + rarity.powerStep() * 0.055f + enhancementAttackBonus(enhancementLevel(stack));
    }

    public static float incomingMultiplier''')
replace_once(equipment, "        return Math.max(0.68f, 1.0f - reduction);", "        return Math.max(0.58f, 1.0f - reduction);")
replace_once(equipment,
'''        float mainBonus = rarityStep(main) * 0.035f + enhancementLevel(main) * 0.03f;
        float offBonus = rarityStep(off) * 0.035f + enhancementLevel(off) * 0.03f;
''',
'''        float mainBonus = rarityStep(main) * 0.035f + enhancementSkillBonus(enhancementLevel(main));
        float offBonus = rarityStep(off) * 0.035f + enhancementSkillBonus(enhancementLevel(off));
''')
replace_once(equipment,
'''    private static float rarityReduction(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        if (rarity == null || !(isArmor(stack.getItem()) || stack.getItem() == Items.SHIELD)) return 0.0f;
        return rarity.powerStep() * 0.012f + enhancementLevel(stack) * 0.008f;
    }
''',
'''    private static float rarityReduction(ItemStack stack) {
        Rarity rarity = rarityOf(stack);
        if (rarity == null || !(isArmor(stack.getItem()) || stack.getItem() == Items.SHIELD)) return 0.0f;
        return rarity.powerStep() * 0.012f + enhancementDefenseBonus(enhancementLevel(stack));
    }

    private static float enhancementAttackBonus(int level) {
        int safe = Math.max(0, level);
        int early = Math.min(10, safe);
        int master = Math.min(10, Math.max(0, safe - 10));
        int apex = Math.max(0, safe - 20);
        return early * 0.040f + master * 0.0225f + apex * 0.0125f;
    }

    private static float enhancementSkillBonus(int level) {
        int safe = Math.max(0, level);
        int early = Math.min(10, safe);
        int master = Math.min(10, Math.max(0, safe - 10));
        int apex = Math.max(0, safe - 20);
        return early * 0.030f + master * 0.0175f + apex * 0.010f;
    }

    private static float enhancementDefenseBonus(int level) {
        int safe = Math.max(0, level);
        int early = Math.min(10, safe);
        int master = Math.min(10, Math.max(0, safe - 10));
        return early * 0.0060f + master * 0.0035f;
    }

    private static int hardEnhancementCap(Item item) {
        if (item == Items.MACE || item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD) return 30;
        if (item == Items.IRON_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE) return 28;
        if (item == Items.TRIDENT || item == Items.BLAZE_ROD) return 26;
        if (item == Items.BOW) return 25;
        if (item == Items.CROSSBOW) return 24;
        if (item == Items.SHIELD) return 22;
        if (isArmor(item)) return 20;
        return 0;
    }
''')
replace_once(equipment,
'''        return item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD
                || item == Items.IRON_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE;
''',
'''        return item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD
                || item == Items.IRON_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE
                || item == Items.MACE;
''')
replace_once(equipment, '        if (item == Items.TRIDENT) return "성문 수호창";\n',
             '        if (item == Items.TRIDENT) return "성문 수호창";\n        if (item == Items.MACE) return "공성 전투망치";\n')

# Mercenaries: keep them relevant for a long campaign, but do not scale radius/damage linearly to 60.
merc = JAVA / "VillageMercenarySystem.java"
replace_once(merc, "public final class VillageMercenarySystem {\n", "public final class VillageMercenarySystem {\n    public static final int MAX_LEVEL = 60;\n")
text = read(merc).replace("Math.min(5, value)", "Math.min(MAX_LEVEL, value)")
text = text.replace("Math.min(5, Integer.parseInt(parts[1]))", "Math.min(MAX_LEVEL, Integer.parseInt(parts[1]))")
write(merc, text)
replace_once(merc,
'''        int currentRank = LEVELS.getOrDefault(uuid, 1);
        int nextRank = Math.min(5, 1 + kills / 8);
        KILLS.put(uuid, kills);
''',
'''        int currentRank = LEVELS.getOrDefault(uuid, 1);
        int nextRank = currentRank;
        while (nextRank < MAX_LEVEL && kills >= killsRequiredForLevel(nextRank + 1)) nextRank++;
        KILLS.put(uuid, kills);
''')
replace_once(merc, '                + " · 적 처치 경험으로 최대 Lv.5까지 성장";',
             '                + " · 적 처치 경험으로 최대 Lv." + MAX_LEVEL + "까지 장기 성장";')
sub_once(merc,
         r'    private static void bastionControl\(ServerLevel level, IronGolem mercenary, int rank\) \{.*?\n    private static void applyClassPassives',
'''    private static void bastionControl(ServerLevel level, IronGolem mercenary, int rank) {
        double radius = 4.5 + Math.min(6.5, rank * 0.11);
        int limit = 5 + Math.min(10, rank / 5);
        Vec3 eye = mercenary.position().add(0, 1.8, 0);
        boolean engaged = false;
        for (Mob enemy : VillageRaidSystem.activeEnemiesNear(level, mercenary.position(), radius, limit, null)) {
            if (!VillageDefenseLineOfSight.hasLine(level, eye, enemy)) continue;
            enemy.setTarget(mercenary);
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 28 + Math.min(90, rank * 2), 0));
            engaged = true;
        }
        if (engaged) VillageDefenseEffectSystem.mercenaryGuardPulse(level, mercenary.position(), radius);
    }

    private static void strikerPressure(ServerLevel level, IronGolem mercenary, int rank) {
        double range = 22.0 + Math.min(30.0, rank * 0.50);
        Mob target = VillageRaidSystem.nearestActiveEnemy(level, mercenary.blockPosition(), range);
        if (target == null || !VillageDefenseLineOfSight.hasLine(level, mercenary.position().add(0, 1.8, 0), target)) return;
        mercenary.setTarget(target);
        mercenary.getNavigation().moveTo(target, 1.18 + Math.min(0.35, rank * 0.006));
        VillageDefenseEffectSystem.mercenaryStrikerPressure(level, mercenary.position().add(0, 1.2, 0),
                target.position().add(0, target.getBbHeight() * 0.5, 0));
    }

    private static void rangedAttack(ServerLevel level, IronGolem mercenary, int rank) {
        Vec3 start = mercenary.position().add(0, 1.8, 0);
        double range = 42.0 + Math.min(48.0, rank * 0.80);
        Mob target = VillageRaidSystem.activeEnemiesNear(level, mercenary.position(), range,
                        18 + Math.min(18, rank / 3), null)
                .stream().filter(enemy -> VillageDefenseLineOfSight.hasLine(level, start, enemy))
                .min(java.util.Comparator.comparingDouble(mercenary::distanceToSqr)).orElse(null);
        mercenary.setTarget(null);
        if (target == null) return;
        float damage = 4.3f * mercenaryPower(rank) * VillageDefenseResearchSystem.mercenaryDamageMultiplier();
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.55, 0);
        VillageDefenseEffectSystem.mercenaryRangerShot(level, start, end);
        level.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 4, 0.14, 0.18, 0.14, 0.02);
        target.hurtServer(level, level.damageSources().mobAttack(mercenary), damage);
    }

    private static void healAllies(ServerLevel level, MinecraftServer server, IronGolem medic, int rank) {
        float amount = 2.3f * mercenaryPower(rank);
        double radius = 8.0 + Math.min(13.0, rank * 0.22);
        AABB area = medic.getBoundingBox().inflate(radius);
        for (IronGolem ally : level.getEntitiesOfClass(IronGolem.class, area,
                entity -> isMercenary(entity.getUUID()) && entity.isAlive())) ally.heal(amount);
        double radiusSquared = radius * radius;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == level && player.distanceToSqr(medic) <= radiusSquared
                    && !VillageRespawnSystem.isDowned(player)) player.heal(amount * 0.65f);
        }
        VillageDefenseEffectSystem.mercenaryHealPulse(level, medic.position(), radius);
        level.sendParticles(ParticleTypes.HEART, medic.getX(), medic.getY() + 1.4, medic.getZ(),
                3 + Math.min(10, rank / 5), 0.55, 0.4, 0.55, 0.02);
    }

    private static float mercenaryPower(int rank) {
        int safe = Math.max(1, Math.min(MAX_LEVEL, rank));
        int veteran = Math.min(19, safe - 1);
        int elite = Math.max(0, safe - 20);
        return 1.0f + veteran * 0.05f + elite * 0.025f;
    }

    private static int killsRequiredForLevel(int level) {
        int n = Math.max(0, Math.min(MAX_LEVEL - 1, level - 1));
        return n * 6 + (n * n) / 2;
    }

    private static void applyClassPassives''')
sub_once(merc,
         r'    private static void applyClassPassives\(IronGolem mercenary, MercenaryClass kind, int rank\) \{.*?\n    \}\n\n    public static synchronized MercenaryClass classOf',
'''    private static void applyClassPassives(IronGolem mercenary, MercenaryClass kind, int rank) {
        int duration = 20 * 60 * 60;
        int healthTier = Math.min(4, Math.max(0, (rank - 1) / 12));
        if (healthTier > 0) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, duration, healthTier - 1, false, false));
        }
        if (kind == MercenaryClass.BASTION) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, duration,
                    Math.min(2, rank / 20), false, false));
            mercenary.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration,
                    Math.min(3, Math.max(0, rank / 12)), false, false));
        } else if (kind == MercenaryClass.STRIKER) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.STRENGTH, duration,
                    Math.min(2, rank / 20), false, false));
            mercenary.addEffect(new MobEffectInstance(MobEffects.SPEED, duration,
                    Math.min(1, rank / 30), false, false));
        } else if (kind == MercenaryClass.RANGER) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.SPEED, duration,
                    rank >= 35 ? 1 : 0, false, false));
        } else if (kind == MercenaryClass.MEDIC) {
            mercenary.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration,
                    rank >= 30 ? 1 : 0, false, false));
        }
    }

    public static synchronized MercenaryClass classOf''')

# Tactical field repair method for the consumable repair kit.
turret = JAVA / "VillagePlacedTurretSystem.java"
anchor = "    public static synchronized String repair(ServerPlayer player, int id) {\n"
insert = '''    public static synchronized String fieldRepairNearest(ServerPlayer player, int amount) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return "";
        double rangeSquared = 12.0 * 12.0;
        TurretState target = TURRETS.values().stream()
                .filter(TurretState::active)
                .filter(state -> state.hp() > 0 && state.hp() < maxHp(state))
                .filter(state -> player.position().distanceToSqr(Vec3.atCenterOf(state.pos())) <= rangeSquared)
                .min(Comparator.comparingDouble(state ->
                        player.position().distanceToSqr(Vec3.atCenterOf(state.pos()))))
                .orElse(null);
        if (target == null) return "";
        int repairedHp = Math.min(maxHp(target), target.hp() + Math.max(1, amount));
        TurretState repaired = new TurretState(target.id(), target.type(), target.pos(), target.level(), repairedHp, true);
        TURRETS.put(target.id(), repaired);
        persist(repaired);
        buildVisual(level, repaired);
        VillageDefenseEffectSystem.turretRepairPulse(level,
                Vec3.atCenterOf(repaired.pos()).add(0.0, -0.35, 0.0));
        return "응급 포탑 수리 키트 사용 · " + repaired.type().displayName() + " #" + repaired.id()
                + " HP " + target.hp() + " → " + repairedHp;
    }

'''
replace_once(turret, anchor, insert + anchor)

# Wall combat: raised internal firing steps/one-block-high slits + top machicolation gallery.
fortress = JAVA / "VillageFortressTerrain.java"
replace_once(fortress, "        buildWalls(level, center, groundY);\n        buildTower(level,",
             "        buildWalls(level, center, groundY);\n        buildDefenderGalleries(level, center, groundY);\n        buildTower(level,")
replace_once(fortress, "        buildNorthGate(level, center, groundY);\n        clearMainAvenue(level, center, groundY);",
             "        buildNorthGate(level, center, groundY);\n        buildDefenderGalleries(level, center, groundY);\n        clearMainAvenue(level, center, groundY);")
replace_once(fortress,
'''            if (Math.floorMod(dx, 3) != 1) {
                int outerZ = center.getZ() + (north ? startZ : startZ + WALL_THICKNESS - 1);
                int innerZ = center.getZ() + (north ? startZ + WALL_THICKNESS - 1 : startZ);
                set(level, new BlockPos(center.getX() + dx, groundY + WALL_TOP_Y + 1, outerZ), Blocks.STONE_BRICKS);
                set(level, new BlockPos(center.getX() + dx, groundY + WALL_TOP_Y + 1, innerZ), Blocks.STONE_BRICKS);
            }
''',
'''            if (Math.floorMod(dx, 6) == 0) {
                for (int offset = 0; offset < WALL_THICKNESS; offset++) {
                    int z = center.getZ() + startZ + offset;
                    set(level, new BlockPos(center.getX() + dx, groundY + 3, z), Blocks.AIR);
                }
                int stepZ = center.getZ() + (north ? startZ + WALL_THICKNESS : startZ - 1);
                set(level, new BlockPos(center.getX() + dx, groundY + 1, stepZ), Blocks.STONE_BRICKS);
            }
            if (Math.floorMod(dx, 3) != 1) {
                int outerZ = center.getZ() + (north ? startZ : startZ + WALL_THICKNESS - 1);
                int innerZ = center.getZ() + (north ? startZ + WALL_THICKNESS - 1 : startZ);
                set(level, new BlockPos(center.getX() + dx, groundY + WALL_TOP_Y + 1, outerZ), Blocks.STONE_BRICKS);
                set(level, new BlockPos(center.getX() + dx, groundY + WALL_TOP_Y + 1, innerZ), Blocks.STONE_BRICKS);
            }
''')
replace_once(fortress,
'''            if (Math.floorMod(dz, 3) != 1) {
                int outerX = center.getX() + (startX < 0 ? startX : startX + WALL_THICKNESS - 1);
                int innerX = center.getX() + (startX < 0 ? startX + WALL_THICKNESS - 1 : startX);
                set(level, new BlockPos(outerX, groundY + WALL_TOP_Y + 1, center.getZ() + dz), Blocks.STONE_BRICKS);
                set(level, new BlockPos(innerX, groundY + WALL_TOP_Y + 1, center.getZ() + dz), Blocks.STONE_BRICKS);
            }
''',
'''            if (Math.floorMod(dz, 6) == 0) {
                for (int offset = 0; offset < WALL_THICKNESS; offset++) {
                    int x = center.getX() + startX + offset;
                    set(level, new BlockPos(x, groundY + 3, center.getZ() + dz), Blocks.AIR);
                }
                int stepX = center.getX() + (startX < 0 ? startX + WALL_THICKNESS : startX - 1);
                set(level, new BlockPos(stepX, groundY + 1, center.getZ() + dz), Blocks.STONE_BRICKS);
            }
            if (Math.floorMod(dz, 3) != 1) {
                int outerX = center.getX() + (startX < 0 ? startX : startX + WALL_THICKNESS - 1);
                int innerX = center.getX() + (startX < 0 ? startX + WALL_THICKNESS - 1 : startX);
                set(level, new BlockPos(outerX, groundY + WALL_TOP_Y + 1, center.getZ() + dz), Blocks.STONE_BRICKS);
                set(level, new BlockPos(innerX, groundY + WALL_TOP_Y + 1, center.getZ() + dz), Blocks.STONE_BRICKS);
            }
''')
marker = "    private static void buildNorthGate(ServerLevel level, BlockPos center, int groundY) {\n"
gallery = '''    private static void buildDefenderGalleries(ServerLevel level, BlockPos center, int groundY) {
        int floorY = groundY + WALL_TOP_Y;
        for (int offset = -WALL_RADIUS; offset <= WALL_RADIUS; offset++) {
            boolean murderHole = Math.floorMod(offset, 4) == 0;
            for (int outward = 0; outward <= 2; outward++) {
                if (outward == 2 && murderHole) continue;
                set(level, new BlockPos(center.getX() + offset, floorY,
                        center.getZ() - WALL_RADIUS - outward), Blocks.STONE_BRICKS);
                set(level, new BlockPos(center.getX() + offset, floorY,
                        center.getZ() + WALL_RADIUS + outward), Blocks.STONE_BRICKS);
                set(level, new BlockPos(center.getX() - WALL_RADIUS - outward, floorY,
                        center.getZ() + offset), Blocks.STONE_BRICKS);
                set(level, new BlockPos(center.getX() + WALL_RADIUS + outward, floorY,
                        center.getZ() + offset), Blocks.STONE_BRICKS);
            }
            if (!murderHole) {
                set(level, new BlockPos(center.getX() + offset, floorY + 1,
                        center.getZ() - WALL_RADIUS - 2), Blocks.STONE_BRICK_WALL);
                set(level, new BlockPos(center.getX() + offset, floorY + 1,
                        center.getZ() + WALL_RADIUS + 2), Blocks.STONE_BRICK_WALL);
                set(level, new BlockPos(center.getX() - WALL_RADIUS - 2, floorY + 1,
                        center.getZ() + offset), Blocks.STONE_BRICK_WALL);
                set(level, new BlockPos(center.getX() + WALL_RADIUS + 2, floorY + 1,
                        center.getZ() + offset), Blocks.STONE_BRICK_WALL);
            }
        }
    }

'''
replace_once(fortress, marker, gallery + marker)

# Primary UI: daily ration + tactical consumables, meaningful research-hall level readout.
ui = JAVA / "VillageUiController.java"
sub_once(ui,
         r'        int arrows = 16 \+ VillageProgressionSystem\.storehouseLevel\(\) \* 4;\n        int food = 5 \+ VillageProgressionSystem\.storehouseLevel\(\) \* 2;\n        addShop\(actions, labels, "buy_arrows".*?\n                "허기 회복용 익힌 소고기", "구매 가능", true\);',
'''        int arrows = 16 + VillageProgressionSystem.storehouseLevel() * 4;
        addShop(actions, labels, "claim_bread", "consumable", "오늘의 배급 식량", "무료",
                "하루 1회 기본 허기 회복 식량 · 접속 시 자동 지급, 놓쳤다면 여기서 수령", "하루 1회", true);
        addShop(actions, labels, "buy_arrows", "consumable", "화살 " + arrows + "개", "주화 14",
                "원거리 전투 보급", "구매 가능", true);
        for (VillageConsumableSystem.Consumable consumable : VillageConsumableSystem.catalog()) {
            boolean available = VillageConsumableSystem.unlocked(consumable);
            addShop(actions, labels, "consumable:" + consumable.id(), "consumable",
                    consumable.displayName() + " ×" + VillageConsumableSystem.bundleCount(consumable),
                    "주화 " + VillageConsumableSystem.effectiveCost(consumable), consumable.description(),
                    VillageConsumableSystem.status(consumable), available);
        }''')
needle = "        if (action.startsWith(\"fusion_combine:\")) {\n"
consumable_action = '''        if (action.startsWith("consumable:")) {
            if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                player.sendSystemMessage(Component.literal("§c전투 소모품 구매는 창고 단말기 근처에서만 가능합니다."));
            } else {
                openResult(player, "전투 소모품 구매 결과",
                        VillageConsumableSystem.purchase(player, action.substring("consumable:".length())),
                        "open_equipment_shop");
            }
            return true;
        }
'''
replace_once(ui, needle, consumable_action + needle)
sub_once(ui,
         r'            case "buy_food" -> \{.*?\n            \}\n            case "sell_loot"',
'''            case "claim_bread" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                    player.sendSystemMessage(Component.literal("§c배급 식량 수령은 창고 단말기 근처에서만 가능합니다."));
                } else {
                    openResult(player, "배급 식량", VillageProgressionSystem.claimDailyBread(player),
                            "open_equipment_shop");
                }
            }
            case "sell_loot"''')
replace_once(ui, '            case SKILL_HALL -> "직업 기술과 용병·포탑 방어 연구를 담당합니다.";',
             '            case SKILL_HALL -> "직업 기술과 용병·포탑 방어 연구를 담당합니다. 연구소 레벨마다 기술 위력·지속시간이 +5% 상승하고 재사용 효율도 개선됩니다.";')
replace_once(ui, '            case STOREHOUSE -> "장비·식량·화살 구매와 전리품 판매를 담당합니다.";',
             '            case STOREHOUSE -> "일일 배급 식량·화살·전투 소모품·장비 구매와 전리품 판매를 담당합니다.";')
replace_once(ui,
             '            case SKILL_HALL -> "최대 내구도 " + (520 + safe * 110) + " · 직업 기술·마을 방어 연구";',
             '            case SKILL_HALL -> "최대 내구도 " + (520 + safe * 110) + " · 기술 위력 +" + (safe * 5) + "% · 지속 +" + (safe * 5) + "% · 재사용 효율 +" + safe + "초 · 마을 방어 연구";')
replace_once(ui,
             '            case STOREHOUSE -> "최대 내구도 " + (560 + safe * 120) + " · 상품·보유품 판매·전리품 정산";',
             '            case STOREHOUSE -> "최대 내구도 " + (560 + safe * 120) + " · 일일 배급·전투 소모품·상품·전리품 정산";')
replace_once(ui,
             '                "강화할 장비를 직접 선택합니다. 대장간 레벨이 오르면 가능한 최대 강화 단계가 증가합니다.",',
             '                "강화할 장비를 직접 선택합니다. 대장간 레벨과 방어 일수가 장기 강화 한도를 열며 장비 계열별 최종 상한이 다릅니다.",')

# Legacy UI path remains save-compatible but can no longer buy duplicate generic food.
legacy_ui = JAVA / "VillageUiService.java"
text = read(legacy_ui)
text = text.replace('            case "buy_food" -> actAndReopen(player, () -> VillageProgressionSystem.buyFood(player), VillageProgressionSystem.Building.STOREHOUSE);\n', '')
text = text.replace('"buy_food", "전투 식량 " + food + "개 · 주화 18|허기 회복용 익힌 소고기",\n', '')
text = text.replace('case SKILL_HALL -> "최대 내구도 " + (520 + safe * 110) + " · 공용 전술·직업 성장·기술 장착 연구 기반";',
                    'case SKILL_HALL -> "최대 내구도 " + (520 + safe * 110) + " · 기술 위력/지속 +" + (safe * 5) + "% · 공용 전술·직업 연구";')
write(legacy_ui, text)

# Shop UX and action descriptions.
shop = JAVA / "VillageShopCatalogScreen.java"
text = read(shop)
text = text.replace("수호 화살·전투 건량·마을 배급빵은 제외됩니다.", "수호 화살·마을 배급 식량·전투 소모품은 제외됩니다.")
text = text.replace('if (card.action().equals("buy_arrows") || card.action().equals("buy_food"))',
                    'if (card.action().equals("buy_arrows") || card.action().equals("claim_bread") || card.action().startsWith("consumable:"))')
text = text.replace('if (action.equals("open_item_sell") || action.equals("buy_arrows") || action.equals("buy_food"))',
                    'if (action.equals("open_item_sell") || action.equals("buy_arrows") || action.equals("claim_bread") || action.startsWith("consumable:"))')
text = text.replace('if (card.action().equals("open_item_sell")) return "판매 목록 열기";',
                    'if (card.action().equals("open_item_sell")) return "판매 목록 열기";\n        if (card.action().equals("claim_bread")) return "수령";')
write(shop, text)

actions = JAVA / "VillageActionDescriptions.java"
text = read(actions)
text = text.replace('        if (action.startsWith("funding:")) {',
'''        if (action.startsWith("consumable:")) {
            return label + "\\n수호 주화로 전투 소모품을 구매합니다. 전투 중 우클릭해 사용하며 종류별 재사용 대기시간이 있습니다.";
        }
        if (action.startsWith("funding:")) {''')
text = text.replace('            case "buy_food" -> label + "\\n수호 주화로 전투 식량을 구매합니다.";\n', '')
text = text.replace('            case "claim_bread" -> "오늘의 무료 식량을 받습니다.";',
                    '            case "claim_bread" -> "오늘의 무료 배급 식량을 받습니다. 일반 식량 구매는 이 배급으로 통합되었습니다.";')
text = text.replace('            case "open_equipment_shop" -> "장비·방어구와 식량·화살 상점을 엽니다. 장비 재고는 매일 바뀝니다.";',
                    '            case "open_equipment_shop" -> "장비·방어구·화살·전투 소모품 상점을 엽니다. 일반 식량은 일일 배급으로 통합됐고 장비 재고는 매일 바뀝니다.";')
text = text.replace('                || action.equals("buy_food")\n', '')
text = text.replace('        if (action.startsWith("buy_")) return "구매";',
                    '        if (action.startsWith("buy_") || action.startsWith("consumable:")) return "구매";')
write(actions, text)

trading = JAVA / "VillageTradingSystem.java"
text = read(trading).replace('NAMED_PRICES.put("마을 배급빵", 2);', 'NAMED_PRICES.put("마을 배급 식량", 2);')
text = text.replace('        NAMED_PRICES.put("전투 건량", 3);\n', '')
write(trading, text)

# Permanent regression contract.
test = r'''#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"

def read(name):
    return (JAVA / name).read_text(encoding="utf-8")

def main():
    assert "mod_version=0.18.18-alpha.1" in (ROOT / "gradle.properties").read_text(encoding="utf-8")

    consumable = read("VillageConsumableSystem.java")
    for key in ("BANDAGE", "CLEANSER", "STIMULANT", "AEGIS_TONIC", "ARCANE_CATALYST", "FIELD_REPAIR_KIT"):
        assert key in consumable
    assert "VillagePlacedTurretSystem.fieldRepairNearest" in consumable
    assert "ARCANE_SURGE_UNTIL" in consumable and "1.20f" in consumable

    progression = read("VillageProgressionSystem.java")
    assert 'Component.literal("마을 배급 식량")' in progression
    assert "skillHallPowerMultiplier" in progression and "skillHallDurationMultiplier" in progression
    assert "barracksLevel * 2 + skillRank(player)" not in progression
    assert "Math.min(7, research + barracksSupport + skillRank(player) / 2)" in progression

    equipment = read("VillageEquipmentRaritySystem.java")
    assert "MAX_ENHANCEMENT = 30" in equipment
    assert "maximumEnhancement(ItemStack stack)" in equipment
    for cap in ("return 30", "return 28", "return 26", "return 25", "return 24", "return 22", "return 20"):
        assert cap in equipment
    assert "enhancementAttackBonus" in equipment and "master * 0.0225f" in equipment
    assert "|| item == Items.MACE" in equipment and 'Items.MACE) return "공성 전투망치"' in equipment

    merc = read("VillageMercenarySystem.java")
    assert "MAX_LEVEL = 60" in merc
    assert "killsRequiredForLevel" in merc and "mercenaryPower" in merc
    assert "42.0 + Math.min(48.0, rank * 0.80)" in merc
    assert "8.0 + Math.min(13.0, rank * 0.22)" in merc

    fortress = read("VillageFortressTerrain.java")
    assert "buildDefenderGalleries" in fortress
    assert "murderHole" in fortress
    assert "groundY + 3" in fortress and "Math.floorMod(dx, 6) == 0" in fortress

    turret = read("VillagePlacedTurretSystem.java")
    assert "fieldRepairNearest" in turret and "12.0 * 12.0" in turret

    role = read("VillageRoleSkillSystem.java")
    assert "VillageProgressionSystem.skillHallPowerMultiplier()" in role
    assert "VillageProgressionSystem.skillHallDurationMultiplier()" in role
    assert "VillageConsumableSystem.skillMultiplier(player)" in role

    controller = read("VillageUiController.java")
    assert '"consumable:" + consumable.id()' in controller
    assert 'case "buy_food"' not in controller
    assert 'case "claim_bread"' in controller
    assert "기술 위력 +" in controller and "재사용 효율 +" in controller

    shop = read("VillageShopCatalogScreen.java")
    assert 'action.startsWith("consumable:")' in shop
    assert 'action.equals("buy_food")' not in shop

    print("[PASS] v0.18.18 tactical consumables replace duplicate paid food")
    print("[PASS] endgame enhancement uses per-family caps and diminishing returns")
    print("[PASS] research hall, wall combat galleries, and Lv.60 mercenary growth are wired")

if __name__ == "__main__":
    main()
'''
write(ROOT / "tools/test_v01818_growth_consumables.py", test)

# Migrate one stale static contract from the removed paid-food action.
test_depth = ROOT / "tools/test_progression_depth.py"
text = read(test_depth)
text = text.replace("    assert 'action.equals(\"buy_arrows\")' in shop_ui and 'action.equals(\"buy_food\")' in shop_ui\n",
                    "    assert 'action.equals(\"buy_arrows\")' in shop_ui and 'action.startsWith(\"consumable:\")' in shop_ui\n")
write(test_depth, text)

# README canonical notes.
readme = ROOT / "README.md"
text = read(readme)
text = text.replace("현재 소스 버전 `0.18.16-alpha.1`", "현재 소스 버전 `0.18.18-alpha.1`")
text = text.replace("목표 JAR `villageguardians-0.18.16-alpha.1.jar`", "목표 JAR `villageguardians-0.18.18-alpha.1.jar`")
section = r'''

## 0.18.18 장기 성장·전투 보급·성벽 교전 패스

- 무료 `마을 배급 식량`을 하루 1회 기본 생존식량으로 통합하고, 역할이 겹치던 유료 `전투 건량` 판매는 제거했다.
- 보급소에 응급 붕대, 정화 약제, 전투 자극제, 수호 비약, 비전 촉진제, 응급 포탑 수리 키트를 추가했다. 날짜별 해금·종류별 재사용 대기시간·보급소 단계별 가격/묶음 보정이 적용된다.
- 대장간 장비 강화는 기존 +5 단일 상한을 폐지했다. 대장간 단계와 방어 일수가 장기 강화 한도를 열고 장검/망치 +30, 도끼 +28, 장창·비전봉 +26, 활 +25, 석궁 +24, 방패 +22, 방어구 +20의 계열별 최종 상한을 사용한다.
- +10 이후와 +20 이후 강화 효율은 완만해지며 강화 비용은 마스터워크 구간부터 가속 증가한다. Mace도 드랍/무기 스타일뿐 아니라 실제 등급 강화·피해 계산에 포함된다.
- 기술 연구소는 레벨마다 직업 기술 위력 +5%, 지속시간 +5%, 재사용 효율을 제공한다. 병영의 과도한 레벨당 -2초 쿨다운은 제거하고 병영은 경험치·용병 중심 시설로 되돌렸다.
- 일반 성벽에는 높은 1블록 사격구와 내부 발판을 배치해 적이 통과하지 못한 채 원거리 교전할 수 있고, 성벽 상단 바깥쪽에는 살짝 돌출된 전투 보행로와 투하구를 만들어 벽 바로 아래 적도 공격할 수 있게 했다.
- 용병 성장 상한을 Lv.5에서 Lv.60으로 확장했다. 처치 누적 요구량은 단계적으로 증가하고 피해·회복·사거리·제어 범위는 완만한 장기 곡선을 사용해 극후반에도 투자 가치가 남는다.
'''
if "## 0.18.18 장기 성장·전투 보급·성벽 교전 패스" not in text:
    text += section
write(readme, text)

print("[PASS] applied Village Guardians v0.18.18 growth patch")
