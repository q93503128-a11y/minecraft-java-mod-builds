package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Locale;

/**
 * Explicit 2/3/4/5-piece equipment sets.
 *
 * Set membership belongs to the individual Village Guardians item, not its vanilla item type.
 * Legacy gear without a set stamp receives a deterministic fallback so existing saves remain useful.
 */
public final class VillageEquipmentSetSystem {
    private VillageEquipmentSetSystem() {}

    public static EquipmentSet setOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String stamped = VillageEquipmentIdentity.setId(stack);
        EquipmentSet explicit = EquipmentSet.parse(stamped);
        if (explicit != null) return explicit;

        String offer = VillageEquipmentIdentity.offer(stack);
        EquipmentSet offerSet = setForOfferId(offer);
        if (offerSet != null) return offerSet;

        if (VillageEquipmentRaritySystem.rarityOf(stack) == null) return null;
        return defaultSetForItem(stack.getItem());
    }

    public static EquipmentSet defaultSetForItem(Item item) {
        if (item == Items.BOW || item == Items.CROSSBOW) return EquipmentSet.NIGHT_HUNTER;
        if (item == Items.BLAZE_ROD) return EquipmentSet.ARCANE_RESONANCE;
        if (item == Items.SHIELD || isChest(item) || isLegs(item)) return EquipmentSet.WALL_GUARDIAN;
        if (isHelmet(item) || isBoots(item)) return EquipmentSet.NIGHT_HUNTER;
        if (item == Items.IRON_SWORD || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD
                || item == Items.IRON_AXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE
                || item == Items.MACE || item == Items.TRIDENT) {
            return EquipmentSet.FRONTLINE_EXECUTOR;
        }
        return EquipmentSet.FRONTLINE_EXECUTOR;
    }

    public static EquipmentSet setForOfferId(String offerId) {
        if (offerId == null || offerId.isBlank()) return null;
        return switch (offerId.toLowerCase(Locale.ROOT)) {
            case "watch_sword", "sentinel_axe", "veteran_blade", "wind_blade", "march_boots",
                    "executioner_axe", "blackwall_greatsword", "necro_breaker", "abyss_axe",
                    "last_guard_blade" -> EquipmentSet.FRONTLINE_EXECUTOR;
            case "hunter_bow", "twinstring_bow", "siege_crossbow", "eagle_crossbow", "star_bow",
                    "rift_longbow", "skyward_crossbow", "storm_longbow", "doomstar_bow" ->
                    EquipmentSet.NIGHT_HUNTER;
            case "arcane_focus", "frost_focus", "rune_leggings", "war_crown", "warcaster_focus" ->
                    EquipmentSet.ARCANE_RESONANCE;
            case "dawn_blade", "dawn_scepter", "phoenix_chest" -> EquipmentSet.DAWN_COVENANT;
            case "ward_shield", "bulwark_helm", "bastion_chest", "titan_shield", "aegis_chest",
                    "rift_aegis", "eclipse_plate", "bastion_crown", "century_aegis" ->
                    EquipmentSet.WALL_GUARDIAN;
            default -> null;
        };
    }

    public static EquipmentSet setForRaidDrop(
            VillageEnemyArchetypeSystem.Archetype archetype, boolean boss, RandomSource random) {
        if (boss || archetype == null) {
            EquipmentSet[] values = EquipmentSet.values();
            return values[random.nextInt(values.length)];
        }
        return switch (archetype) {
            case RUSHER, GRUNT, SHIELDBREAKER, SAPPER, NETHER_REAVER ->
                    EquipmentSet.FRONTLINE_EXECUTOR;
            case MARKSMAN, TOWER_HUNTER, BOGGED_ARCHER, BREEZE_DISRUPTOR, CAVE_STALKER ->
                    EquipmentSet.NIGHT_HUNTER;
            case HEXER, WAR_CHANTER -> EquipmentSet.ARCANE_RESONANCE;
            case NECROMANCER, PLAGUE_ARCHON -> EquipmentSet.DAWN_COVENANT;
            case BULWARK, ZOGLIN_BREACHER, MAGMA_BRUTE, SIEGE_BEAST, IRON_WARLORD, DREAD_KNIGHT ->
                    EquipmentSet.WALL_GUARDIAN;
        };
    }

    public static int countEquipped(Player player, EquipmentSet set) {
        if (player == null || set == null) return 0;
        int count = 0;
        if (setOf(player.getMainHandItem()) == set) count++;
        if (setOf(player.getOffhandItem()) == set) count++;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (setOf(player.getItemBySlot(slot)) == set) count++;
        }
        return Math.min(5, count);
    }

    public static float outgoingMultiplier(ServerPlayer player, boolean projectile) {
        float value = 1.0f;

        int frontline = countEquipped(player, EquipmentSet.FRONTLINE_EXECUTOR);
        if (!projectile && frontline >= 2) value *= 1.08f;
        if (!projectile && frontline >= 4) value *= 1.08f;
        if (!projectile && frontline >= 5) value *= 1.18f;

        int hunter = countEquipped(player, EquipmentSet.NIGHT_HUNTER);
        if (projectile && hunter >= 2) value *= 1.10f;
        if (projectile && hunter >= 4) value *= 1.10f;
        if (projectile && hunter >= 5) value *= 1.18f;

        return value;
    }

    public static float targetMultiplier(ServerPlayer player, Mob target, boolean projectile) {
        if (player == null || target == null) return 1.0f;
        float value = 1.0f;

        if (!projectile
                && countEquipped(player, EquipmentSet.FRONTLINE_EXECUTOR) >= 5
                && target.getHealth() <= target.getMaxHealth() * 0.40f) {
            value *= 1.25f;
        }
        if (projectile
                && countEquipped(player, EquipmentSet.NIGHT_HUNTER) >= 5
                && player.distanceToSqr(target) >= 144.0) {
            value *= 1.20f;
        }
        return value;
    }

    public static float roleSkillTargetMultiplier(
            ServerPlayer player, Mob target, VillageRole role) {
        if (player == null || target == null || role == null) return 1.0f;
        if (role == VillageRole.VANGUARD
                && countEquipped(player, EquipmentSet.FRONTLINE_EXECUTOR) >= 5
                && target.getHealth() <= target.getMaxHealth() * 0.40f) {
            return 1.25f;
        }
        if (role == VillageRole.RANGER
                && countEquipped(player, EquipmentSet.NIGHT_HUNTER) >= 5
                && player.distanceToSqr(target) >= 144.0) {
            return 1.20f;
        }
        return 1.0f;
    }

    public static float roleSkillMultiplier(
            ServerPlayer player, VillageRole role, int promotionTier) {
        if (player == null || role == null) return 1.0f;
        float value = 1.0f;
        switch (role) {
            case VANGUARD -> {
                int pieces = countEquipped(player, EquipmentSet.FRONTLINE_EXECUTOR);
                if (pieces >= 3) value *= 1.08f;
                if (pieces >= 5) value *= 1.15f;
            }
            case RANGER -> {
                // Base ranger skills often empower a real arrow, which already receives the set's
                // projectile multiplier at impact. Only promoted direct-damage skills scale here.
                if (promotionTier <= 0) break;
                int pieces = countEquipped(player, EquipmentSet.NIGHT_HUNTER);
                if (pieces >= 4) value *= 1.10f;
                if (pieces >= 5) value *= 1.15f;
            }
            case ARCANIST -> {
                int pieces = countEquipped(player, EquipmentSet.ARCANE_RESONANCE);
                if (pieces >= 2) value *= 1.12f;
                if (pieces >= 4) value *= 1.12f;
                if (pieces >= 5) value *= 1.20f;
            }
            case LUMINAR -> {
                int pieces = countEquipped(player, EquipmentSet.DAWN_COVENANT);
                if (pieces >= 2) value *= 1.10f;
                if (pieces >= 4) value *= 1.12f;
                if (pieces >= 5) value *= 1.18f;
            }
            case WARDEN -> {
                int pieces = countEquipped(player, EquipmentSet.WALL_GUARDIAN);
                if (pieces >= 3) value *= 1.10f;
                if (pieces >= 5) value *= 1.18f;
            }
        }
        return value;
    }

    public static int cooldownReductionSeconds(ServerPlayer player, VillageRole role) {
        if (player == null || role == null) return 0;
        return switch (role) {
            case ARCANIST -> {
                int pieces = countEquipped(player, EquipmentSet.ARCANE_RESONANCE);
                yield (pieces >= 3 ? 1 : 0) + (pieces >= 5 ? 1 : 0);
            }
            case LUMINAR -> {
                int pieces = countEquipped(player, EquipmentSet.DAWN_COVENANT);
                yield (pieces >= 3 ? 1 : 0) + (pieces >= 5 ? 1 : 0);
            }
            case VANGUARD -> countEquipped(player, EquipmentSet.FRONTLINE_EXECUTOR) >= 5 ? 1 : 0;
            case RANGER -> countEquipped(player, EquipmentSet.NIGHT_HUNTER) >= 5 ? 1 : 0;
            case WARDEN -> countEquipped(player, EquipmentSet.WALL_GUARDIAN) >= 5 ? 1 : 0;
        };
    }

    public static float incomingMultiplier(ServerPlayer player) {
        if (player == null) return 1.0f;
        float value = 1.0f;

        int frontline = countEquipped(player, EquipmentSet.FRONTLINE_EXECUTOR);
        if (frontline >= 4) value *= 0.95f;

        int hunter = countEquipped(player, EquipmentSet.NIGHT_HUNTER);
        if (hunter >= 3) value *= 0.96f;

        int dawn = countEquipped(player, EquipmentSet.DAWN_COVENANT);
        if (dawn >= 5) value *= 0.90f;

        int wall = countEquipped(player, EquipmentSet.WALL_GUARDIAN);
        if (wall >= 2) value *= 0.94f;
        if (wall >= 4) value *= 0.94f;
        if (wall >= 5) {
            value *= 0.90f;
            if (player.getHealth() <= player.getMaxHealth() * 0.40f) value *= 0.82f;
        }
        return Math.max(0.68f, value);
    }

    public static String tooltipSummary(Player player, ItemStack stack) {
        EquipmentSet set = setOf(stack);
        if (set == null) return "";
        int count = player == null ? 0 : countEquipped(player, set);
        return set.displayName() + " " + count + "/5 · " + set.effectText();
    }

    private static boolean isHelmet(Item item) {
        return item == Items.IRON_HELMET || item == Items.DIAMOND_HELMET || item == Items.NETHERITE_HELMET;
    }

    private static boolean isChest(Item item) {
        return item == Items.IRON_CHESTPLATE || item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE;
    }

    private static boolean isLegs(Item item) {
        return item == Items.IRON_LEGGINGS || item == Items.DIAMOND_LEGGINGS || item == Items.NETHERITE_LEGGINGS;
    }

    private static boolean isBoots(Item item) {
        return item == Items.IRON_BOOTS || item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_BOOTS;
    }

    public enum EquipmentSet {
        FRONTLINE_EXECUTOR(
                "frontline_executor", "전선 집행자",
                "근접 피해 +8%",
                "전사 기술 +8%",
                "근접 피해 +8% · 받는 피해 -5%",
                "근접 피해 +18% · 전사 기술 +15% · 재사용 -1초",
                "적 HP 40% 이하: 근접 공격·전사 공격 기술 +25%"),
        NIGHT_HUNTER(
                "night_hunter", "밤사냥꾼",
                "원거리 피해 +10%",
                "받는 피해 -4%",
                "원거리 피해 +10% · 전직 궁수 기술 +10%",
                "원거리 피해 +18% · 전직 궁수 기술 +15% · 재사용 -1초",
                "12블록 이상 표적: 원거리 공격·전직 궁수 공격 기술 +20%"),
        ARCANE_RESONANCE(
                "arcane_resonance", "비전 공명",
                "비전 기술 +12%",
                "기술 재사용 -1초",
                "비전 기술 +12%",
                "비전 기술 +20% · 재사용 추가 -1초",
                ""),
        DAWN_COVENANT(
                "dawn_covenant", "여명 성약",
                "성광 기술·치유 +10%",
                "기술 재사용 -1초",
                "성광 기술·치유 +12%",
                "성광 기술·치유 +18% · 재사용 추가 -1초 · 받는 피해 -10%",
                ""),
        WALL_GUARDIAN(
                "wall_guardian", "성벽 수호자",
                "받는 피해 -6%",
                "수호 기술 +10%",
                "받는 피해 추가 -6%",
                "수호 기술 +18% · 재사용 -1초 · 받는 피해 추가 -10%",
                "HP 40% 이하: 받는 피해 추가 -18%");

        private final String id;
        private final String displayName;
        private final String twoPiece;
        private final String threePiece;
        private final String fourPiece;
        private final String fivePiece;
        private final String capstoneText;

        EquipmentSet(
                String id, String displayName, String twoPiece, String threePiece,
                String fourPiece, String fivePiece, String capstoneText) {
            this.id = id;
            this.displayName = displayName;
            this.twoPiece = twoPiece;
            this.threePiece = threePiece;
            this.fourPiece = fourPiece;
            this.fivePiece = fivePiece;
            this.capstoneText = capstoneText;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }

        public String pieceEffect(int requiredPieces) {
            return switch (requiredPieces) {
                case 2 -> twoPiece;
                case 3 -> threePiece;
                case 4 -> fourPiece;
                case 5 -> fivePiece;
                default -> "";
            };
        }

        public String capstoneText() { return capstoneText; }

        public String effectText() {
            return "2셋 " + twoPiece + " · 3셋 " + threePiece
                    + " · 4셋 " + fourPiece + " · 5셋 " + fivePiece
                    + (capstoneText.isBlank() ? "" : " · " + capstoneText);
        }

        public static EquipmentSet parse(String value) {
            if (value == null || value.isBlank()) return null;
            String normalized = value.toLowerCase(Locale.ROOT);
            for (EquipmentSet set : values()) {
                if (set.id.equals(normalized) || set.name().toLowerCase(Locale.ROOT).equals(normalized)) return set;
            }
            return null;
        }
    }}
