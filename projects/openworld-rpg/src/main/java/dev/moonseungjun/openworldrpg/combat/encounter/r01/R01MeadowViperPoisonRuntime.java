package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import dev.moonseungjun.openworldrpg.combat.authority.PlayerDefenseAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectDamageApplicationContext;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerNegativeStatusRuntimeState;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/** Server-authoritative Meadow Viper Poison buildup and timed-damage runtime. */
public final class R01MeadowViperPoisonRuntime {
    public static final String STATUS_ID = "openworld_rpg:viper_poison";
    public static final double BITE_BUILDUP = 40.0;
    public static final long DURATION_TICKS = 8L * 20L;
    public static final long DAMAGE_INTERVAL_TICKS = 20L;
    public static final double HEALING_RECEIVED_MULTIPLIER = 0.80;
    public static final double TOTAL_SOURCE_LEVEL_HP_SHARE = 0.09;
    public static final int SOURCE_LEVEL = 2;
    public static final long BUILDUP_DECAY_DELAY_TICKS = 3L * 20L;
    public static final double PLAYER_BUILDUP_DECAY_PER_SECOND = 20.0;

    private static final ResourceKey<DamageType> PROJECT_DIRECT_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(OpenworldRpgMod.MOD_ID, "project_direct_magic")
    );
    private static final Map<UUID, BuildupState> BUILDUP = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_DAMAGE_TICK = new ConcurrentHashMap<>();

    private R01MeadowViperPoisonRuntime() {
    }

    public static BuildupApplication applyBuildup(
            ServerPlayer target,
            double authoredBuildup,
            double activeDefenseMultiplier
    ) {
        if (target == null
                || !Double.isFinite(authoredBuildup)
                || authoredBuildup < 0.0
                || !Double.isFinite(activeDefenseMultiplier)
                || activeDefenseMultiplier < 0.0
                || activeDefenseMultiplier > 1.0) {
            throw new IllegalArgumentException("Invalid Meadow Viper Poison buildup input.");
        }
        if (target.level().isClientSide()) {
            return BuildupApplication.rejected();
        }

        var build = CombatStateServices.combatBuilds().build(target.getUUID());
        if (build.isEmpty()) {
            // Never fabricate WIL when the canonical player combat build is unavailable.
            return BuildupApplication.rejected();
        }

        long nowTick = target.level().getGameTime();
        var negativeStatuses = CombatStateServices.negativeStatusStates()
                .getOrCreate(target.getUUID());
        double received = authoredBuildup
                * activeDefenseMultiplier
                * negativeStatuses.negativeBuildupReceivedMultiplier(nowTick);
        if (received <= 0.0) {
            return new BuildupApplication(true, 0.0, currentBuildup(target), false);
        }

        double threshold = ProjectCombatRules.playerAilmentThreshold(
                build.orElseThrow().effectiveAttributes().wil()
        );
        double current = decayBuildup(target.getUUID(), nowTick);
        double next = current + received;
        boolean poisoned = next + 1.0e-9 >= threshold;
        if (poisoned) {
            BUILDUP.remove(target.getUUID());
            negativeStatuses.applyStatusForDuration(
                    STATUS_ID,
                    Set.of(PlayerNegativeStatusRuntimeState.MINOR_DISPELLABLE_TAG),
                    DURATION_TICKS,
                    nowTick
            );
            NEXT_DAMAGE_TICK.put(
                    target.getUUID(),
                    Math.addExact(nowTick, DAMAGE_INTERVAL_TICKS)
            );
            next = 0.0;
        } else {
            BUILDUP.put(
                    target.getUUID(),
                    new BuildupState(next, nowTick, nowTick)
            );
        }
        return new BuildupApplication(true, received, next, poisoned);
    }

    public static double currentBuildup(ServerPlayer target) {
        return decayBuildup(target.getUUID(), target.level().getGameTime());
    }

    public static boolean poisoned(ServerPlayer target, long nowTick) {
        return CombatStateServices.negativeStatusStates()
                .getOrCreate(target.getUUID())
                .hasStatus(STATUS_ID, nowTick);
    }

    public static double healingReceivedMultiplier(ServerPlayer target, long nowTick) {
        return poisoned(target, nowTick) ? HEALING_RECEIVED_MULTIPLIER : 1.0;
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            long nowTick = player.level().getGameTime();
            decayBuildup(player.getUUID(), nowTick);

            Long nextDamageTick = NEXT_DAMAGE_TICK.get(player.getUUID());
            if (nextDamageTick == null) {
                if (poisoned(player, nowTick)) {
                    NEXT_DAMAGE_TICK.put(
                            player.getUUID(),
                            Math.addExact(nowTick, DAMAGE_INTERVAL_TICKS)
                    );
                }
                continue;
            }
            if (nowTick < nextDamageTick) {
                continue;
            }

            // Sample the tick immediately before the scheduled slice. This gives a full 8 s Poison
            // exactly eight 1 s slices while still making cleanse remove the next pending slice.
            long statusSampleTick = Math.max(0L, nextDamageTick - 1L);
            if (!poisoned(player, statusSampleTick)) {
                NEXT_DAMAGE_TICK.remove(player.getUUID());
                continue;
            }

            applyPeriodicDamage(player);
            NEXT_DAMAGE_TICK.put(
                    player.getUUID(),
                    Math.addExact(nextDamageTick, DAMAGE_INTERVAL_TICKS)
            );
        }
    }

    private static double decayBuildup(UUID playerId, long nowTick) {
        BuildupState current = BUILDUP.get(playerId);
        if (current == null) {
            return 0.0;
        }
        if (nowTick <= current.lastUpdatedTick()) {
            return current.amount();
        }

        long decayStartTick = Math.addExact(
                current.lastBuildupTick(),
                BUILDUP_DECAY_DELAY_TICKS
        );
        long effectiveStart = Math.max(current.lastUpdatedTick(), decayStartTick);
        if (nowTick <= effectiveStart) {
            BUILDUP.replace(
                    playerId,
                    current,
                    new BuildupState(
                            current.amount(),
                            current.lastBuildupTick(),
                            nowTick
                    )
            );
            return current.amount();
        }

        double decayPerTick = PLAYER_BUILDUP_DECAY_PER_SECOND / 20.0;
        double next = Math.max(
                0.0,
                current.amount() - (nowTick - effectiveStart) * decayPerTick
        );
        if (next <= 1.0e-9) {
            BUILDUP.remove(playerId, current);
            return 0.0;
        }
        BUILDUP.replace(
                playerId,
                current,
                new BuildupState(next, current.lastBuildupTick(), nowTick)
        );
        return next;
    }

    private static void applyPeriodicDamage(ServerPlayer player) {
        var defense = CombatStateServices.defenseSnapshots()
                .snapshot(player.getUUID())
                .orElse(null);
        if (defense == null || !(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        // Poison canon is a fixed Lv2 source budget BEFORE target-specific mitigation.
        double rawTotal = ProjectCombatRules.benchmarkPlayerHealth(SOURCE_LEVEL)
                * TOTAL_SOURCE_LEVEL_HP_SHARE;
        double rawPerTick = rawTotal / (DURATION_TICKS / DAMAGE_INTERVAL_TICKS);
        PlayerDefenseAuthority.IncomingHit periodicHit =
                PlayerDefenseAuthority.IncomingHit.unguardable(
                        rawPerTick,
                        ProjectImpactTransaction.DamageSchool.MAGIC,
                        SOURCE_LEVEL,
                        false
                );
        double resolved = PlayerDefenseAuthority.mitigatedDamageBeforeActiveDefense(
                periodicHit,
                defense
        );
        if (resolved <= 0.0) {
            return;
        }

        var damageTypes = serverLevel.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        DamageSource source = new DamageSource(
                damageTypes.getOrThrow(PROJECT_DIRECT_MAGIC),
                null,
                null
        );
        float amount = (float) Math.min(resolved, Float.MAX_VALUE);
        boolean applied = ProjectDamageApplicationContext.authorizeNext(
                player,
                () -> player.hurtServer(serverLevel, source, amount)
        );
        if (applied) {
            long nowTick = serverLevel.getGameTime();
            CombatStateServices.markCombatActivity(player.getUUID(), nowTick);
            CombatStateServices.markHostileHpActivity(player.getUUID(), nowTick);
        }
    }

    public static void disconnect(UUID playerId) {
        BUILDUP.remove(playerId);
        NEXT_DAMAGE_TICK.remove(playerId);
    }

    private record BuildupState(
            double amount,
            long lastBuildupTick,
            long lastUpdatedTick
    ) {
        private BuildupState {
            if (!Double.isFinite(amount)
                    || amount <= 0.0
                    || lastBuildupTick < 0L
                    || lastUpdatedTick < lastBuildupTick) {
                throw new IllegalArgumentException(
                        "Invalid Meadow Viper Poison buildup state."
                );
            }
        }
    }

    public record BuildupApplication(
            boolean accepted,
            double appliedBuildup,
            double remainingBuildup,
            boolean poisoned
    ) {
        public BuildupApplication {
            if (!Double.isFinite(appliedBuildup)
                    || appliedBuildup < 0.0
                    || !Double.isFinite(remainingBuildup)
                    || remainingBuildup < 0.0) {
                throw new IllegalArgumentException("Invalid Meadow Viper Poison buildup result.");
            }
            if (!accepted && (appliedBuildup != 0.0 || remainingBuildup != 0.0 || poisoned)) {
                throw new IllegalArgumentException("Rejected buildup cannot carry applied state.");
            }
        }

        public static BuildupApplication rejected() {
            return new BuildupApplication(false, 0.0, 0.0, false);
        }
    }
}
