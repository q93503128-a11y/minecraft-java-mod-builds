package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * Linear class advancement layered over the stable five-role IDs.
 * Promotion is derived from player level, so old worlds need no save migration and
 * base/first-promotion skills remain valid after the second promotion.
 */
public final class VillageRolePromotionSystem {
    public static final int FIRST_PROMOTION_LEVEL = 30;
    public static final int SECOND_PROMOTION_LEVEL = 60;

    private VillageRolePromotionSystem() {}

    public static int tier(ServerPlayer player) {
        if (player == null) return 0;
        return tier(VillageCouncilState.levelOf(player.getUUID()));
    }

    public static int tier(int level) {
        return VillageCampaignProgression.promotionTierForLevel(level);
    }

    public static String displayName(VillageRole role, int level) {
        if (role == null) return "미선택";
        int tier = tier(level);
        return switch (role) {
            case VANGUARD -> tier >= 2 ? "파성검성" : tier == 1 ? "전선검장" : role.displayName();
            case RANGER -> tier >= 2 ? "천공추적자" : tier == 1 ? "성루명사수" : role.displayName();
            case ARCANIST -> tier >= 2 ? "대비전술사" : tier == 1 ? "전투원소술사" : role.displayName();
            case LUMINAR -> tier >= 2 ? "대성휘사제" : tier == 1 ? "전장성직자" : role.displayName();
            case WARDEN -> tier >= 2 ? "불락수호장" : tier == 1 ? "성문수호장" : role.displayName();
        };
    }

    public static String displayName(ServerPlayer player, VillageRole role) {
        int level = player == null ? 1 : VillageCouncilState.levelOf(player.getUUID());
        return displayName(role, level);
    }

    public static String tierLabel(int tier) {
        return tier >= 2 ? "2차 전직" : tier == 1 ? "1차 전직" : "기본 직업";
    }

    public static float outgoingMultiplier(ServerPlayer player, boolean projectile) {
        VillageRole role = player == null ? null : VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        int tier = tier(player);
        if (role == null || tier <= 0) return 1.0f;
        return switch (role) {
            case VANGUARD -> projectile ? 1.0f : tier >= 2 ? 1.15f : 1.07f;
            case RANGER -> projectile ? (tier >= 2 ? 1.15f : 1.07f) : 1.0f;
            case ARCANIST, LUMINAR -> 1.0f;
            case WARDEN -> tier >= 2 ? 1.04f : 1.02f;
        };
    }

    public static float targetMultiplier(ServerPlayer player, Mob target, boolean projectile) {
        if (player == null || target == null) return 1.0f;
        VillageRole role = VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        int tier = tier(player);
        if (role == VillageRole.RANGER && projectile && VillageRaidSystem.isAerialEnemy(target)) {
            return tier >= 2 ? 1.30f : tier == 1 ? 1.14f : 1.0f;
        }
        if (role == VillageRole.VANGUARD && !projectile && tier >= 2
                && target.getHealth() <= target.getMaxHealth() * 0.40f) {
            return 1.12f;
        }
        return 1.0f;
    }

    public static float incomingMultiplier(ServerPlayer player) {
        VillageRole role = player == null ? null : VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        int tier = tier(player);
        if (role == null || tier <= 0) return 1.0f;
        return switch (role) {
            case WARDEN -> tier >= 2 ? 0.88f : 0.94f;
            case LUMINAR -> tier >= 2 ? 0.95f : 0.98f;
            case VANGUARD -> tier >= 2 ? 0.96f : 0.99f;
            default -> 1.0f;
        };
    }

    public static float skillPowerMultiplier(ServerPlayer player, VillageRole role) {
        int tier = tier(player);
        if (tier <= 0 || role == null) return 1.0f;
        return switch (role) {
            case ARCANIST -> tier >= 2 ? 1.27f : 1.13f;
            case LUMINAR -> tier >= 2 ? 1.24f : 1.12f;
            case WARDEN -> tier >= 2 ? 1.16f : 1.08f;
            case VANGUARD, RANGER -> tier >= 2 ? 1.13f : 1.06f;
        };
    }

    public static int cooldownReductionSeconds(ServerPlayer player, VillageRole role) {
        int tier = tier(player);
        if (tier <= 0 || role == null) return 0;
        return switch (role) {
            case ARCANIST -> tier >= 2 ? 2 : 1;
            case RANGER -> tier >= 2 ? 1 : 0;
            default -> 0;
        };
    }

    public static float meleeLifeStealBonus(ServerPlayer player) {
        VillageRole role = player == null ? null : VillageCouncilState.roleOf(player.getUUID()).orElse(null);
        if (role != VillageRole.VANGUARD) return 0.0f;
        int tier = tier(player);
        return tier >= 2 ? 0.04f : tier == 1 ? 0.02f : 0.0f;
    }

    public static int bonusHealthPoints(ServerPlayer player, VillageRole role) {
        int tier = tier(player);
        if (tier <= 0 || role == null) return 0;
        return switch (role) {
            case VANGUARD -> tier >= 2 ? 10 : 4;
            case WARDEN -> tier >= 2 ? 14 : 6;
            case LUMINAR -> tier >= 2 ? 6 : 3;
            default -> tier >= 2 ? 4 : 2;
        };
    }

    public static String passiveSummary(ServerPlayer player, VillageRole role) {
        int tier = tier(player);
        if (tier <= 0 || role == null) return "전직 전";
        return switch (role) {
            case VANGUARD -> tier >= 2 ? "근접 화력·마무리·흡혈 강화" : "근접 화력·흡혈 강화";
            case RANGER -> tier >= 2 ? "원거리 화력·공중 추적 특화" : "원거리·대공 화력 강화";
            case ARCANIST -> tier >= 2 ? "기술 위력 대폭 강화·재사용 감소" : "기술 위력·재사용 강화";
            case LUMINAR -> tier >= 2 ? "치유·보호·생존 지원 대폭 강화" : "치유·보호 효율 강화";
            case WARDEN -> tier >= 2 ? "피해 경감·체력·수호 기술 대폭 강화" : "피해 경감·체력·수호 기술 강화";
        };
    }
}
