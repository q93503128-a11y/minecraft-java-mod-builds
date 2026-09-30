package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerBarrierAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Server-authoritative Cleric Active Skill 3.
 *
 * <p>The field is fixed at the server-accepted cast position. Five one-second pulses preserve the
 * authored whole-action healing and damage totals. Balanced Doctrine classifies this action as
 * healing/protection, so its accepted-cast output snapshot affects healing and the empowered
 * barrier only; the damage half never double-dips into the damaging Doctrine bonus.</p>
 */
public final class ConsecratedGroundRuntime {
    public static final int DURATION_TICKS = 100;
    public static final int PULSE_INTERVAL_TICKS = 20;
    public static final int PULSE_COUNT = DURATION_TICKS / PULSE_INTERVAL_TICKS;
    public static final double TOTAL_HEAL_COEFFICIENT = 0.30;
    public static final double TOTAL_ENEMY_ACTION_COEFFICIENT = 1.35;
    public static final double EMPOWERED_BARRIER_COEFFICIENT = 0.12;

    private static final double HEAL_COEFFICIENT_PER_PULSE =
            TOTAL_HEAL_COEFFICIENT / PULSE_COUNT;
    private static final double DAMAGE_COEFFICIENT_PER_PULSE =
            TOTAL_ENEMY_ACTION_COEFFICIENT / PULSE_COUNT;
    private static final String BARRIER_SOURCE_PREFIX =
            "openworld_rpg:consecrated_ground/";

    private static final ConcurrentHashMap<UUID, ActiveZone> ACTIVE_ZONES =
            new ConcurrentHashMap<>();

    private ConsecratedGroundRuntime() {
    }

    public static boolean canActivate(ServerPlayer caster) {
        Objects.requireNonNull(caster, "caster");
        return !caster.level().isClientSide()
                && PlayerProgressionService.state(caster)
                        .activeClass()
                        .filter(RootClass.CLERIC::equals)
                        .isPresent()
                && CombatStateServices.combatBuilds()
                        .build(caster.getUUID())
                        .isPresent();
    }

    public static Activation activate(ServerPlayer caster) {
        Objects.requireNonNull(caster, "caster");
        if (!canActivate(caster)
                || !(caster.level() instanceof ServerLevel level)) {
            return Activation.rejected();
        }

        var cast = ClericSkillRuntime.consumeAcceptedCast(
                caster,
                ProjectSpellSpec.CONSECRATED_GROUND_ID
        );
        if (cast == null) {
            return Activation.rejected();
        }

        long nowTick = level.getGameTime();
        Vec3 origin = caster.position();
        ActiveZone zone = new ActiveZone(
                caster.getUUID(),
                level,
                origin,
                cast.outputMultiplier(),
                nowTick,
                Math.addExact(nowTick, PULSE_INTERVAL_TICKS),
                Math.addExact(nowTick, DURATION_TICKS)
        );
        ACTIVE_ZONES.put(caster.getUUID(), zone);

        int barrierRecipients = 0;
        if (cast.graceEmpowered()) {
            double barrierOutputBonus = Math.max(
                    0.0,
                    cast.outputMultiplier() - 1.0
            );
            for (ServerPlayer ally : playersInside(
                    level.getServer(),
                    zone
            )) {
                var barrier = ProjectBarrierRuntime.applySkillBarrier(
                        caster,
                        ally,
                        BARRIER_SOURCE_PREFIX + caster.getUUID(),
                        EMPOWERED_BARRIER_COEFFICIENT,
                        barrierOutputBonus,
                        PlayerBarrierAuthority.DEFAULT_BARRIER_DURATION_TICKS,
                        true
                );
                if (barrier.accepted()) {
                    barrierRecipients++;
                }
            }
        }

        Pulse pulse = applyPulse(caster, zone, nowTick);
        return new Activation(
                true,
                cast.graceEmpowered(),
                cast.outputMultiplier(),
                origin,
                zone.expiresAtTick(),
                barrierRecipients,
                pulse.healedRecipients(),
                pulse.damagedEnemies()
        );
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        ACTIVE_ZONES.entrySet().removeIf(entry -> {
            ActiveZone zone = entry.getValue();
            ServerPlayer caster = server.getPlayerList()
                    .getPlayer(zone.casterId());
            if (caster == null
                    || caster.level() != zone.level()
                    || !caster.isAlive()) {
                return true;
            }

            long nowTick = zone.level().getGameTime();
            if (nowTick >= zone.expiresAtTick()) {
                return true;
            }
            if (nowTick >= zone.nextPulseTick()) {
                applyPulse(caster, zone, nowTick);
                zone.advancePulse();
            }
            return false;
        });
    }

    public static void disconnect(UUID playerId) {
        ACTIVE_ZONES.remove(Objects.requireNonNull(playerId, "playerId"));
    }

    public static int activeZoneCount() {
        return ACTIVE_ZONES.size();
    }

    private static Pulse applyPulse(
            ServerPlayer caster,
            ActiveZone zone,
            long nowTick
    ) {
        int healedRecipients = 0;
        for (ServerPlayer ally : playersInside(
                zone.level().getServer(),
                zone
        )) {
            var healing = ProjectHealingRuntime.applySkillHeal(
                    caster,
                    ally,
                    HEAL_COEFFICIENT_PER_PULSE,
                    zone.supportOutputMultiplier()
            );
            if (!healing.accepted()
                    || healing.effectiveHealing() <= 0.0) {
                continue;
            }

            healedRecipients++;
            var combat = CombatStateServices.states()
                    .getOrCreate(caster.getUUID(), nowTick);
            var healingGraceGain =
                    CombatStateServices.clericGraceStates()
                            .getOrCreate(caster.getUUID())
                            .recordEffectiveHeal(
                                    ally.getUUID(),
                                    healing.effectiveHealing(),
                                    ally.getMaxHealth(),
                                    nowTick,
                                    combat.lastCombatActivityTick()
                            );
            ClericRootPassiveRuntime.onGraceGain(
                    caster,
                    healingGraceGain,
                    nowTick
            );
            ProjectUltimateChargeRuntime.recordClericEffectiveHealing(
                    caster,
                    ally,
                    healing.effectiveHealing()
            );
        }

        var build = CombatStateServices.combatBuilds()
                .build(caster.getUUID())
                .orElse(null);
        if (build == null) {
            return new Pulse(healedRecipients, 0);
        }

        var source = build.damageSource(
                ProjectImpactTransaction.DamageSchool.MAGIC
        );
        int damagedEnemies = 0;
        LivingEntity firstDamaged = null;

        for (LivingEntity target : zone.level().getEntitiesOfClass(
                LivingEntity.class,
                ConsecratedGroundZoneShape.bounds(zone.origin()),
                target -> target != caster
                        && ExternalActorBindingRuntime
                                .projectTargetSnapshot(
                                        target,
                                        nowTick
                                ).isPresent()
                        && ConsecratedGroundZoneShape.contains(
                                zone.origin(),
                                target
                        )
        )) {
            var targetSnapshot = ExternalActorBindingRuntime
                    .projectTargetSnapshot(target, nowTick)
                    .orElse(null);
            if (targetSnapshot == null) {
                continue;
            }

            double damage = ProjectImpactTransaction.resolveDirectDamage(
                    new ProjectImpactTransaction.DirectDamageRequest(
                            source,
                            targetSnapshot,
                            ProjectImpactTransaction.DamageSchool.MAGIC,
                            DAMAGE_COEFFICIENT_PER_PULSE,
                            1.0,
                            1.0
                    )
            ).finalDamage();

            if (ProjectMinecraftDamageApplicator.applyDirectMagic(
                    caster,
                    target,
                    damage
            )) {
                damagedEnemies++;
                if (firstDamaged == null) {
                    firstDamaged = target;
                }
            }
        }

        if (firstDamaged != null
                && !zone.damageContributionPublished()) {
            CombatStateServices.markCombatActivity(
                    caster.getUUID(),
                    nowTick
            );
            var combat = CombatStateServices.states()
                    .getOrCreate(caster.getUUID(), nowTick);
            ClericRootPassiveRuntime.recordDamagingEligibleHit(
                    caster,
                    nowTick
            );
            var damagingGraceGain =
                    CombatStateServices.clericGraceStates()
                            .getOrCreate(caster.getUUID())
                            .recordDamagingActiveHit(
                                    nowTick,
                                    combat.lastCombatActivityTick()
                            );
            ClericRootPassiveRuntime.onGraceGain(
                    caster,
                    damagingGraceGain,
                    nowTick
            );
            ProjectUltimateChargeRuntime.recordClericDamagingActive(
                    caster,
                    firstDamaged
            );
            zone.markDamageContributionPublished();
        }

        return new Pulse(healedRecipients, damagedEnemies);
    }

    private static List<ServerPlayer> playersInside(
            MinecraftServer server,
            ActiveZone zone
    ) {
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == zone.level())
                .filter(ServerPlayer::isAlive)
                .filter(player -> !player.isSpectator())
                .filter(player -> ConsecratedGroundZoneShape.contains(
                        zone.origin(),
                        player
                ))
                .toList();
    }

    private record Pulse(
            int healedRecipients,
            int damagedEnemies
    ) {
    }

    private static final class ActiveZone {
        private final UUID casterId;
        private final ServerLevel level;
        private final Vec3 origin;
        private final double supportOutputMultiplier;
        private final long activatedAtTick;
        private long nextPulseTick;
        private final long expiresAtTick;
        private boolean damageContributionPublished;

        private ActiveZone(
                UUID casterId,
                ServerLevel level,
                Vec3 origin,
                double supportOutputMultiplier,
                long activatedAtTick,
                long nextPulseTick,
                long expiresAtTick
        ) {
            this.casterId = casterId;
            this.level = level;
            this.origin = origin;
            this.supportOutputMultiplier = supportOutputMultiplier;
            this.activatedAtTick = activatedAtTick;
            this.nextPulseTick = nextPulseTick;
            this.expiresAtTick = expiresAtTick;
        }

        private UUID casterId() {
            return casterId;
        }

        private ServerLevel level() {
            return level;
        }

        private Vec3 origin() {
            return origin;
        }

        private double supportOutputMultiplier() {
            return supportOutputMultiplier;
        }

        @SuppressWarnings("unused")
        private long activatedAtTick() {
            return activatedAtTick;
        }

        private long nextPulseTick() {
            return nextPulseTick;
        }

        private long expiresAtTick() {
            return expiresAtTick;
        }

        private boolean damageContributionPublished() {
            return damageContributionPublished;
        }

        private void markDamageContributionPublished() {
            damageContributionPublished = true;
        }

        private void advancePulse() {
            nextPulseTick = Math.addExact(
                    nextPulseTick,
                    PULSE_INTERVAL_TICKS
            );
        }
    }

    public record Activation(
            boolean accepted,
            boolean graceEmpowered,
            double doctrineSupportMultiplier,
            Vec3 origin,
            long expiresAtTick,
            int barrierRecipients,
            int healedRecipientsOnArrival,
            int damagedEnemiesOnArrival
    ) {
        public Activation {
            if (!Double.isFinite(doctrineSupportMultiplier)
                    || doctrineSupportMultiplier < 0.0
                    || barrierRecipients < 0
                    || healedRecipientsOnArrival < 0
                    || damagedEnemiesOnArrival < 0
                    || expiresAtTick < 0L) {
                throw new IllegalArgumentException(
                        "Invalid Consecrated Ground activation result."
                );
            }
            if (!accepted
                    && (graceEmpowered
                    || doctrineSupportMultiplier != 0.0
                    || origin != null
                    || expiresAtTick != 0L
                    || barrierRecipients != 0
                    || healedRecipientsOnArrival != 0
                    || damagedEnemiesOnArrival != 0)) {
                throw new IllegalArgumentException(
                        "Rejected Consecrated Ground cannot carry applied state."
                );
            }
        }

        public static Activation rejected() {
            return new Activation(
                    false,
                    false,
                    0.0,
                    null,
                    0L,
                    0,
                    0,
                    0
            );
        }
    }
}
