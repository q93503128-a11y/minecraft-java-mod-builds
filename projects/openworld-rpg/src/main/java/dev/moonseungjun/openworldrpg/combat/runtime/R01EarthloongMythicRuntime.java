package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
import dev.moonseungjun.openworldrpg.combat.state.ProjectEquipmentSlot;
import dev.moonseungjun.openworldrpg.combat.state.R01EarthloongMythicEffectState;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.progression.r01.R01EarthloongBossLootRules;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Runtime bridge for the two canon-locked R01 Earthloong Mythic unique powers.
 *
 * <p>This class intentionally emits no placeholder particles. Final Rootquake ground VFX and
 * Earthen Reprieve barrier presentation stay asset-gated, while the server damage/protection
 * authority is already deterministic.</p>
 */
public final class R01EarthloongMythicRuntime {
    public static final double ROOTQUAKE_RADIUS_BLOCKS = 4.0;
    private static final double ROOTQUAKE_RADIUS_SQR =
            ROOTQUAKE_RADIUS_BLOCKS * ROOTQUAKE_RADIUS_BLOCKS;

    private static final String ROOTQUAKE_ITEM_ID =
            R01EarthloongBossLootRules.MythicBase.ROOTQUAKE_MAUL.itemId();
    private static final String EARTHSCALE_WARD_ITEM_ID =
            R01EarthloongBossLootRules.MythicBase.EARTHSCALE_WARD.itemId();

    private static final ConcurrentHashMap<UUID, R01EarthloongMythicEffectState> STATES =
            new ConcurrentHashMap<>();

    private R01EarthloongMythicRuntime() {
    }

    public static RootquakeApplication onPersonalEliteBossPoiseBreak(
            ServerPlayer player,
            LivingEntity brokenTarget,
            PlayerCombatBuildState build,
            long gameTick
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(brokenTarget, "brokenTarget");
        Objects.requireNonNull(build, "build");

        if (player.level().isClientSide()
                || brokenTarget.level() != player.level()
                || !brokenTarget.isAlive()
                || !hasEquippedItem(
                        player,
                        ProjectEquipmentSlot.MAIN_WEAPON,
                        ROOTQUAKE_ITEM_ID
                )) {
            return RootquakeApplication.rejected();
        }

        double weaponPower = build.damageSource(
                ProjectImpactTransaction.DamageSchool.PHYSICAL
        ).weaponPower();
        R01EarthloongMythicEffectState.RootquakeTrigger trigger =
                state(player.getUUID()).tryRootquake(weaponPower, gameTick);
        if (!trigger.triggered()) {
            return new RootquakeApplication(
                    false,
                    trigger.damage(),
                    0
            );
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return RootquakeApplication.rejected();
        }

        int affected = 0;
        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                brokenTarget.getBoundingBox().inflate(ROOTQUAKE_RADIUS_BLOCKS),
                target -> target.isAlive()
                        && target != player
                        && target.distanceToSqr(brokenTarget) <= ROOTQUAKE_RADIUS_SQR
                        && (target instanceof Enemy
                                || ExternalActorBindingRuntime.ownsDamageAuthority(target))
        )) {
            if (ProjectMinecraftDamageApplicator.applyDirectPhysical(
                    player,
                    target,
                    trigger.damage()
            )) {
                affected++;
            }
        }

        CombatStateServices.markCombatActivity(player.getUUID(), gameTick);
        return new RootquakeApplication(true, trigger.damage(), affected);
    }

    public static R01EarthloongMythicEffectState.ReprieveTrigger onPerfectGuard(
            ServerPlayer player,
            long gameTick
    ) {
        Objects.requireNonNull(player, "player");
        if (!hasEquippedItem(
                player,
                ProjectEquipmentSlot.OFF_HAND,
                EARTHSCALE_WARD_ITEM_ID
        )) {
            return new R01EarthloongMythicEffectState.ReprieveTrigger(
                    false,
                    0.0,
                    Long.MIN_VALUE / 4,
                    Long.MIN_VALUE / 4
            );
        }

        return state(player.getUUID()).tryEarthenReprieve(
                player.getMaxHealth(),
                gameTick
        );
    }

    public static R01EarthloongMythicEffectState.BarrierApplication absorbBarrier(
            ServerPlayer player,
            double incomingDamage,
            long gameTick
    ) {
        Objects.requireNonNull(player, "player");
        return state(player.getUUID()).absorb(incomingDamage, gameTick);
    }

    public static void disconnect(UUID playerId) {
        STATES.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    static R01EarthloongMythicEffectState state(UUID playerId) {
        return STATES.computeIfAbsent(
                Objects.requireNonNull(playerId, "playerId"),
                ignored -> new R01EarthloongMythicEffectState()
        );
    }

    private static boolean hasEquippedItem(
            ServerPlayer player,
            ProjectEquipmentSlot slot,
            String itemId
    ) {
        return PlayerEquipmentService.state(player)
                .item(slot)
                .map(item -> itemId.equals(item.itemId()))
                .orElse(false);
    }

    public record RootquakeApplication(
            boolean triggered,
            double damagePerTarget,
            int affectedTargets
    ) {
        public static RootquakeApplication rejected() {
            return new RootquakeApplication(false, 0.0, 0);
        }
    }
}
