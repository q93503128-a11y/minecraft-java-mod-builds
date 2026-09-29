package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerBarrierAuthority;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerEquipmentService;
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
 * Server-authoritative Cleric root Ultimate.
 *
 * <p>Sanctuary is a fixed ground ward created at the accepted Ultimate transaction. It does not
 * consume Grace or Balanced Doctrine because those root mechanics explicitly apply to Cleric
 * actives, while this action is the root Ultimate. Its healing and barrier outcomes can still
 * generate Grace/Ultimate support events through the normal project-owned authorities.</p>
 */
public final class SanctuaryRuntime {
    public static final int DURATION_TICKS = 160;
    public static final int PULSE_INTERVAL_TICKS = 20;
    public static final int PULSE_COUNT = DURATION_TICKS / PULSE_INTERVAL_TICKS;
    public static final double INITIAL_BARRIER_COEFFICIENT = 0.25;
    public static final double TOTAL_HEAL_COEFFICIENT = 0.55;
    public static final double TOTAL_ENEMY_ACTION_COEFFICIENT = 2.00;
    public static final double NEGATIVE_STATUS_DURATION_MULTIPLIER = 0.80;

    private static final double HEAL_COEFFICIENT_PER_PULSE =
            TOTAL_HEAL_COEFFICIENT / PULSE_COUNT;
    private static final double DAMAGE_COEFFICIENT_PER_PULSE =
            TOTAL_ENEMY_ACTION_COEFFICIENT / PULSE_COUNT;
    private static final String BARRIER_SOURCE_PREFIX =
            "openworld_rpg:sanctuary/";

    private static final ConcurrentHashMap<UUID, ActiveZone> ACTIVE_ZONES =
            new ConcurrentHashMap<>();

    private SanctuaryRuntime() {
    }

    public static boolean canActivate(ServerPlayer caster) {
        Objects.requireNonNull(caster, "caster");
        if (caster.level().isClientSide()
                || PlayerProgressionService.state(caster)
                        .activeClass()
                        .filter(RootClass.CLERIC::equals)
                        .isEmpty()
                || CombatStateServices.combatBuilds()
                        .build(caster.getUUID())
                        .isEmpty()) {
            return false;
        }
        return ProjectUltimateChargeRuntime.canActivateUltimate(caster);
    }

    public static Activation activate(ServerPlayer caster) {
        Objects.requireNonNull(caster, "caster");
        if (!canActivate(caster)
                || !(caster.level() instanceof ServerLevel level)
                || !ProjectUltimateChargeRuntime.tryActivateUltimate(caster)) {
            return Activation.rejected();
        }

        long nowTick = level.getGameTime();
        Vec3 origin = caster.position();
        ActiveZone zone = new ActiveZone(
                caster.getUUID(),
                level,
                origin,
                nowTick,
                Math.addExact(nowTick, PULSE_INTERVAL_TICKS),
                Math.addExact(nowTick, DURATION_TICKS)
        );
        ACTIVE_ZONES.put(caster.getUUID(), zone);

        MinecraftServer server = level.getServer();
        List<ServerPlayer> allies = playersInside(server, zone);
        applyStatusProtection(allies, nowTick);

        double barrierOutputBonus = PlayerEquipmentService.state(caster)
                .aggregateHealingDoneBonus();
        int barrierRecipients = 0;
        for (ServerPlayer ally : allies) {
            var barrier = ProjectBarrierRuntime.applySkillBarrier(
                    caster,
                    ally,
                    BARRIER_SOURCE_PREFIX + caster.getUUID(),
                    INITIAL_BARRIER_COEFFICIENT,
                    barrierOutputBonus,
                    PlayerBarrierAuthority.DEFAULT_BARRIER_DURATION_TICKS,
                    true
            );
            if (barrier.accepted()) {
                barrierRecipients++;
            }
        }

        Pulse pulse = applyPulse(caster, zone, nowTick);
        return new Activation(
                true,
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

            List<ServerPlayer> allies = playersInside(server, zone);
            applyStatusProtection(allies, nowTick);

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
                    HEAL_COEFFICIENT_PER_PULSE
            );
            if (!healing.accepted()
                    || healing.effectiveHealing() <= 0.0) {
                continue;
            }
            healedRecipients++;

            var combat = CombatStateServices.states()
                    .getOrCreate(caster.getUUID(), nowTick);
            CombatStateServices.clericGraceStates()
                    .getOrCreate(caster.getUUID())
                    .recordEffectiveHeal(
                            ally.getUUID(),
                            healing.effectiveHealing(),
                            ally.getMaxHealth(),
                            nowTick,
                            combat.lastCombatActivityTick()
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
        for (LivingEntity target : zone.level().getEntitiesOfClass(
                LivingEntity.class,
                SanctuaryZoneShape.bounds(zone.origin()),
                target -> target != caster
                        && ExternalActorBindingRuntime
                                .projectTargetSnapshot(
                                        target,
                                        nowTick
                                ).isPresent()
                        && SanctuaryZoneShape.contains(
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
            }
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
                .filter(player -> SanctuaryZoneShape.contains(
                        zone.origin(),
                        player
                ))
                .toList();
    }

    private static void applyStatusProtection(
            List<ServerPlayer> allies,
            long nowTick
    ) {
        for (ServerPlayer ally : allies) {
            CombatStateServices.negativeStatusStates()
                    .getOrCreate(ally.getUUID())
                    .applyNegativeStatusDurationMultiplier(
                            NEGATIVE_STATUS_DURATION_MULTIPLIER,
                            2L,
                            nowTick
                    );
        }
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
        private final long activatedAtTick;
        private long nextPulseTick;
        private final long expiresAtTick;

        private ActiveZone(
                UUID casterId,
                ServerLevel level,
                Vec3 origin,
                long activatedAtTick,
                long nextPulseTick,
                long expiresAtTick
        ) {
            this.casterId = casterId;
            this.level = level;
            this.origin = origin;
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

        private void advancePulse() {
            nextPulseTick = Math.addExact(
                    nextPulseTick,
                    PULSE_INTERVAL_TICKS
            );
        }
    }

    public record Activation(
            boolean accepted,
            Vec3 origin,
            long expiresAtTick,
            int barrierRecipients,
            int healedRecipientsOnArrival,
            int damagedEnemiesOnArrival
    ) {
        public Activation {
            if (barrierRecipients < 0
                    || healedRecipientsOnArrival < 0
                    || damagedEnemiesOnArrival < 0
                    || expiresAtTick < 0L) {
                throw new IllegalArgumentException(
                        "Invalid Sanctuary activation result."
                );
            }
            if (!accepted
                    && (origin != null
                    || expiresAtTick != 0L
                    || barrierRecipients != 0
                    || healedRecipientsOnArrival != 0
                    || damagedEnemiesOnArrival != 0)) {
                throw new IllegalArgumentException(
                        "Rejected Sanctuary cannot carry applied state."
                );
            }
        }

        public static Activation rejected() {
            return new Activation(
                    false,
                    null,
                    0L,
                    0,
                    0,
                    0
            );
        }
    }
}
