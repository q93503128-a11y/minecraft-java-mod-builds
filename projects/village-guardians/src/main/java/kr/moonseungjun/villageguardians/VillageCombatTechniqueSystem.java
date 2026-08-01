package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VillageCombatTechniqueSystem {
    private static final Map<UUID, Long> NEXT_SWEEP_AT = new HashMap<>();
    private static final ThreadLocal<Boolean> SECONDARY_DAMAGE = ThreadLocal.withInitial(() -> false);

    private VillageCombatTechniqueSystem() {}

    public static void reset() {
        NEXT_SWEEP_AT.clear();
        SECONDARY_DAMAGE.remove();
    }

    public static void handleIncomingDamage(LivingIncomingDamageEvent event) {
        if (SECONDARY_DAMAGE.get()
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || !(attacker.level() instanceof ServerLevel level)
                || !(event.getEntity() instanceof Mob primary)
                || !VillageRaidSystem.isActiveEnemy(primary.getUUID())) return;

        int playerLevel = VillageCouncilState.levelOf(attacker.getUUID());
        int research = VillageProgressionSystem.skillRank(attacker);
        if (event.getSource().getDirectEntity() instanceof AbstractArrow) {
            handleArrowTechnique(level, attacker, primary, event.getAmount(), playerLevel, research);
        } else if (attacker.getMainHandItem().is(ItemTags.SWORDS)) {
            handleSwordTechnique(level, attacker, primary, event.getAmount(), playerLevel, research);
        }
    }

    public static void castRoleTechnique(ServerPlayer player, VillageRole role, int learned) {
        if (!(player.level() instanceof ServerLevel level) || !VillageRaidSystem.isActive() || learned < 2) return;
        int playerLevel = VillageCouncilState.levelOf(player.getUUID());
        double radius = 6.0 + learned * 1.5;
        float damage = 3.0f + playerLevel * 0.28f + learned * 1.5f;
        int limit = switch (role) {
            case VANGUARD -> 9;
            case RANGER -> 7;
            case ARCANIST -> 11;
            case LUMINAR -> 3;
            case WARDEN -> 5;
        };
        List<Mob> targets = VillageRaidSystem.activeEnemiesNear(level, player.position(), radius, limit, null);
        secondaryDamage(level, targets, damage);
        if (!targets.isEmpty()) player.sendSystemMessage(Component.literal(
                "§d[연계 전술] §f주변 적 " + targets.size() + "명에게 전술 충격을 가했습니다."));
    }

    public static String unlockSummary(ServerPlayer player) {
        StringBuilder text = new StringBuilder();
        if (VillageSkillTreeSystem.projectileFireBonusTicks(player) > 0) text.append("발화 촉");
        if (VillageCouncilState.levelOf(player.getUUID()) >= 8 && VillageProgressionSystem.skillRank(player) >= 2) append(text, "검기 휩쓸기");
        if (VillageSkillTreeSystem.extraRicochetTargets(player) > 0) append(text, "도탄 사격");
        if (VillageSkillTreeSystem.executionMultiplier(player, 1.0f, 4.0f) > 1.0f) append(text, "처형 본능");
        return text.isEmpty() ? "아직 해금된 고급 전투 기술 없음" : text.toString();
    }

    private static void handleArrowTechnique(
            ServerLevel level,
            ServerPlayer attacker,
            Mob primary,
            float primaryDamage,
            int playerLevel,
            int research) {
        int fireTicks = VillageSkillTreeSystem.projectileFireBonusTicks(attacker);
        if (fireTicks > 0) primary.setRemainingFireTicks(Math.max(primary.getRemainingFireTicks(), fireTicks + research * 10));
        int extraTargets = VillageSkillTreeSystem.extraRicochetTargets(attacker);
        if (extraTargets <= 0 && (playerLevel < 10 || research < 2)) return;
        int limit = Math.max(2, extraTargets + (playerLevel >= 18 && research >= 4 ? 3 : 1));
        float ratio = playerLevel >= 18 && research >= 4 ? 0.52f : 0.36f;
        List<Mob> targets = VillageRaidSystem.activeEnemiesNear(
                level, primary.position(), 7.0 + research, limit, primary.getUUID());
        secondaryDamage(level, targets, Math.max(1.0f, primaryDamage * ratio));
    }

    private static void handleSwordTechnique(
            ServerLevel level,
            ServerPlayer attacker,
            Mob primary,
            float primaryDamage,
            int playerLevel,
            int research) {
        if (playerLevel < 8 || research < 2) return;
        long now = level.getGameTime();
        long readyAt = NEXT_SWEEP_AT.getOrDefault(attacker.getUUID(), 0L);
        if (readyAt > now) return;
        NEXT_SWEEP_AT.put(attacker.getUUID(), now + Math.max(8, 22 - research * 2));
        int limit = playerLevel >= 20 ? 7 : 4;
        List<Mob> targets = VillageRaidSystem.activeEnemiesNear(
                level, primary.position(), 4.5 + research * 0.35, limit, primary.getUUID());
        secondaryDamage(level, targets, Math.max(1.0f, primaryDamage * (0.38f + research * 0.035f)));
    }

    private static void secondaryDamage(ServerLevel level, List<Mob> targets, float damage) {
        if (targets.isEmpty()) return;
        SECONDARY_DAMAGE.set(true);
        try {
            for (Mob target : targets) if (target.isAlive()) target.hurtServer(level, level.damageSources().magic(), damage);
        } finally {
            SECONDARY_DAMAGE.set(false);
        }
    }

    private static void append(StringBuilder builder, String value) {
        if (!builder.isEmpty()) builder.append(" · ");
        builder.append(value);
    }
}
