package kr.moonseungjun.villageguardians;

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

}
