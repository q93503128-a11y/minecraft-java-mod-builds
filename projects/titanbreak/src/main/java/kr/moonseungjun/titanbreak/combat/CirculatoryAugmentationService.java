package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Survival mechanics for heart/circulatory augmentations whose effects depend on the final
 * post-mitigation damage value. This deliberately runs at LivingDamageEvent.Pre so armor and
 * ordinary reduction are resolved before a lethal-hit safeguard is considered.
 */
public final class CirculatoryAugmentationService {
    private static final long ARTIFICIAL_FATAL_COOLDOWN = 1200L;
    private static final long HEMOSTATIC_REPEAT_WINDOW = 50L;
    private static final long HEMOSTATIC_SEAL_DELAY = 20L;
    private static final long HEMOSTATIC_SEAL_COOLDOWN = 120L;
    private static final long HEMOSTATIC_SEVERE_DELAY = 30L;
    private static final long HEMOSTATIC_SEVERE_COOLDOWN = 600L;

    private static final Set<String> PERSISTENT_DAMAGE_IDS = Set.of(
            "onFire", "inFire", "lava", "wither", "magic", "drown", "hotFloor", "cactus", "sweetBerryBush");

    private static final Map<UUID, RuntimeState> RUNTIME = new ConcurrentHashMap<>();

    private static final class RuntimeState {
        String lastDamageId = "";
        long lastDamageTick = Long.MIN_VALUE / 4L;
        long artificialHeartReadyTick = Long.MIN_VALUE / 4L;
        boolean dualHeartSpent;
        long sealAt = Long.MIN_VALUE / 4L;
        long sealReadyTick = Long.MIN_VALUE / 4L;
        long severeRecoveryAt = Long.MIN_VALUE / 4L;
        long severeReadyTick = Long.MIN_VALUE / 4L;
    }

    private CirculatoryAugmentationService() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        long now = player.level().getGameTime();
        TitanPlayerData.AugmentInstance pump = state.firstInstalledInstance("hemostatic_pump");
        if (pump == null) {
            runtime.sealAt = Long.MIN_VALUE / 4L;
            runtime.severeRecoveryAt = Long.MIN_VALUE / 4L;
            return;
        }

        if (runtime.sealAt <= now) {
            if (runtime.sealAt > Long.MIN_VALUE / 8L && player.getHealth() > 0.0F
                    && player.getHealth() < player.getMaxHealth()) {
                player.heal((float) CombatScale.toInternal(pump.enhancement() >= 10 ? 8.0D : 6.0D));
            }
            runtime.sealAt = Long.MIN_VALUE / 4L;
        }

        if (runtime.severeRecoveryAt <= now) {
            if (runtime.severeRecoveryAt > Long.MIN_VALUE / 8L && pump.enhancement() >= 10
                    && player.getHealth() > 0.0F && player.getHealth() <= player.getMaxHealth() * 0.60F) {
                player.heal((float) CombatScale.toInternal(25.0D));
            }
            runtime.severeRecoveryAt = Long.MIN_VALUE / 4L;
        }
    }

    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        float damage = event.getNewDamage();
        if (damage <= 0.0F) return;

        TitanPlayerData data = TitanPlayerData.get(level.getServer());
        TitanPlayerData.State state = data.state(player);
        RuntimeState runtime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new RuntimeState());
        long now = level.getGameTime();

        String damageId = event.getSource().getMsgId();
        boolean repeated = damageId.equals(runtime.lastDamageId) && now - runtime.lastDamageTick <= HEMOSTATIC_REPEAT_WINDOW;
        boolean persistent = repeated || PERSISTENT_DAMAGE_IDS.contains(damageId);
        runtime.lastDamageId = damageId;
        runtime.lastDamageTick = now;

        TitanPlayerData.AugmentInstance pump = state.firstInstalledInstance("hemostatic_pump");
        if (pump != null) {
            int enhancement = pump.enhancement();
            if (persistent) {
                damage *= enhancement >= 5 ? 0.55F : 0.72F;
                if (enhancement >= 7 && now >= runtime.sealReadyTick) {
                    runtime.sealAt = now + HEMOSTATIC_SEAL_DELAY;
                    runtime.sealReadyTick = now + HEMOSTATIC_SEAL_COOLDOWN;
                }
            }

            float projectedHealthLoss = Math.max(0.0F, damage - player.getAbsorptionAmount());
            boolean severe = player.getHealth() - projectedHealthLoss <= player.getMaxHealth() * 0.30F;
            if (enhancement >= 10 && severe && now >= runtime.severeReadyTick) {
                damage *= 0.80F;
                runtime.severeRecoveryAt = now + HEMOSTATIC_SEVERE_DELAY;
                runtime.severeReadyTick = now + HEMOSTATIC_SEVERE_COOLDOWN;
            }
        }

        float survivablePool = player.getHealth() + player.getAbsorptionAmount();
        if (damage + 1.0E-4F >= survivablePool) {
            TitanPlayerData.AugmentInstance dual = state.firstInstalledInstance("dual_heart");
            if (dual != null && dual.enhancement() >= 10 && !runtime.dualHeartSpent) {
                float targetHealth = Math.max(1.0F, player.getMaxHealth() * 0.20F);
                float allowed = player.getAbsorptionAmount() + Math.max(0.0F, player.getHealth() - targetHealth);
                damage = Math.min(damage, allowed);
                runtime.dualHeartSpent = true;
                data.addMasteryXp(player, "dual_heart", 8);
            } else {
                TitanPlayerData.AugmentInstance artificial = state.firstInstalledInstance("artificial_heart");
                if (artificial != null && artificial.enhancement() >= 10 && now >= runtime.artificialHeartReadyTick) {
                    float targetHealth = Math.max(1.0F, player.getMaxHealth() * 0.10F);
                    float allowed = player.getAbsorptionAmount() + Math.max(0.0F, player.getHealth() - targetHealth);
                    damage = Math.min(damage, allowed);
                    runtime.artificialHeartReadyTick = now + ARTIFICIAL_FATAL_COOLDOWN;
                    data.addMasteryXp(player, "artificial_heart", 5);
                }
            }
        }

        event.setNewDamage(Math.max(0.0F, damage));
    }

    public static void clear(UUID playerId) {
        RUNTIME.remove(playerId);
    }

    public static void clearAll() {
        RUNTIME.clear();
    }
}
