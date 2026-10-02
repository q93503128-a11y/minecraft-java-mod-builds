package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectPlayerIncomingDamageRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.sounds.SoundEvent;
import org.slf4j.Logger;

/**
 * Narrow donor-contact binder for Cave Centipede Scuttle Bite.
 *
 * <p>The pinned Alex's Mobs entity exposes a real melee contact, segmented movement and
 * {@code alexsmobs:centipede_attack}. The donor's direct damage and vanilla Poison effect remain
 * suppressed by {@code ExternalActorDamageAuthorityMixin}; this bridge converts only an accepted
 * project-authored spawn's donor contact into the canonical 0.35 s tell and 0.40 s recovery before
 * applying project-owned Scuttle Bite damage.</p>
 *
 * <p>Poison buildup is deliberately not applied here yet. The R01 document locks 30 buildup on
 * contact but does not currently specify Cave Centipede's enemy-applied Poison source-level damage
 * budget. Production spawn therefore remains fail-closed instead of inventing that missing value.</p>
 */
public final class R01CaveCentipedeDonorContactBridge {
    static final Identifier ATTACK_SOUND_ID =
            Identifier.fromNamespaceAndPath("alexsmobs", "centipede_attack");
    private static final R01SecondaryCreatureEncounterData DATA =
            R01SecondaryCreatureEncounterDataLoader.loadCaveCentipede();
    private static final R01SecondaryCreatureEncounterData.ActionRule SCUTTLE_BITE =
            DATA.rulesById().get(R01SecondaryCreatureEncounterData.ActionId.SCUTTLE_BITE);
    private static final Map<UUID, BiteState> STATES = new ConcurrentHashMap<>();
    private static volatile boolean initialized;

    private R01CaveCentipedeDonorContactBridge() {
    }

    public static synchronized void initialize(RuntimeProfile profile, Logger logger) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(logger, "logger");
        if (initialized) {
            return;
        }
        initialized = true;

        if (profile == RuntimeProfile.CORE) {
            logger.info("Openworld RPG Cave Centipede donor-contact bridge inactive for core profile.");
            return;
        }

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (BuiltInRegistries.SOUND_EVENT.getOptional(ATTACK_SOUND_ID).isEmpty()) {
                throw new IllegalStateException(
                        "Pinned Cave Centipede attack sound is missing: " + ATTACK_SOUND_ID
                );
            }
            logger.info(
                    "Openworld RPG Cave Centipede Scuttle Bite donor-contact bridge armed: "
                            + "{} tick tell, {} tick recovery, sound={}; Poison buildup remains "
                            + "fail-closed until its authored enemy proc budget is closed.",
                    SCUTTLE_BITE.tellTicks(),
                    SCUTTLE_BITE.recoveryTicks(),
                    ATTACK_SOUND_ID
            );
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> STATES.clear());
    }

    /**
     * Consumes a donor melee hit proposal for an accepted authored Cave Centipede.
     *
     * <p>Returning true means the donor proposal belonged to this bridge and must stay cancelled at
     * the vanilla damage boundary. Actual project damage is resolved later at the authored hit
     * frame. Returning false never authorizes donor damage.</p>
     */
    public static boolean interceptDonorMeleeContact(
            LivingEntity attacker,
            ServerPlayer target
    ) {
        Objects.requireNonNull(attacker, "attacker");
        Objects.requireNonNull(target, "target");

        var profile = ExternalActorBindingRuntime.combatProfile(attacker).orElse(null);
        if (profile == null
                || !R01ExternalActorCatalog.CAVE_CENTIPEDE_HEAD.equals(profile.entityId())
                || !ExternalActorBindingRuntime.isAuthoredSpawn(attacker)) {
            return false;
        }

        if (!(attacker.level() instanceof ServerLevel level)
                || target.level() != level
                || !attacker.isAlive()
                || !target.isAlive()) {
            return true;
        }

        long nowTick = level.getGameTime();
        BiteState current = STATES.get(attacker.getUUID());
        if (current != null) {
            if (current.pending() || nowTick < current.recoveryUntilTick()) {
                return true;
            }
            STATES.remove(attacker.getUUID(), current);
        }

        if (attacker.distanceTo(target) > SCUTTLE_BITE.maximumRange()) {
            return true;
        }

        if (attacker instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        STATES.put(
                attacker.getUUID(),
                BiteState.pending(
                        level,
                        attacker,
                        target.getUUID(),
                        Math.addExact(nowTick, SCUTTLE_BITE.tellTicks())
                )
        );
        return true;
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        STATES.forEach((actorId, state) -> {
            LivingEntity attacker = state.attacker();
            if (attacker.isRemoved()
                    || !attacker.isAlive()
                    || attacker.level() != state.level()) {
                STATES.remove(actorId, state);
                return;
            }

            long nowTick = state.level().getGameTime();
            if (!state.pending()) {
                if (nowTick >= state.recoveryUntilTick()) {
                    STATES.remove(actorId, state);
                }
                return;
            }

            if (attacker instanceof Mob mob) {
                mob.getNavigation().stop();
            }
            if (nowTick < state.resolveAtTick()) {
                return;
            }

            playAttackSound(attacker);

            ServerPlayer target = server.getPlayerList().getPlayer(state.targetId());
            if (target != null
                    && target.isAlive()
                    && target.level() == state.level()
                    && attacker.distanceTo(target) <= SCUTTLE_BITE.maximumRange()) {
                ProjectPlayerIncomingDamageRuntime.applyProjectOwnedActorHit(
                        attacker,
                        target,
                        SCUTTLE_BITE.toIncomingHit(DATA.contentLevel())
                );
            }

            STATES.put(
                    actorId,
                    BiteState.recovery(
                            state.level(),
                            attacker,
                            Math.addExact(nowTick, SCUTTLE_BITE.recoveryTicks())
                    )
            );
        });
    }

    static boolean donorContactBound(R01SecondaryCreatureEncounterData.ActionId action) {
        return action == R01SecondaryCreatureEncounterData.ActionId.SCUTTLE_BITE;
    }

    static R01SecondaryCreatureEncounterData.ActionRule scuttleBiteRule() {
        return SCUTTLE_BITE;
    }

    static Identifier attackSoundId() {
        return ATTACK_SOUND_ID;
    }

    private static void playAttackSound(LivingEntity attacker) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(ATTACK_SOUND_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Pinned Cave Centipede attack sound vanished: " + ATTACK_SOUND_ID
                ));
        attacker.playSound(sound, 1.0F, 1.0F);
    }

    private record BiteState(
            ServerLevel level,
            LivingEntity attacker,
            UUID targetId,
            boolean pending,
            long resolveAtTick,
            long recoveryUntilTick
    ) {
        private BiteState {
            Objects.requireNonNull(level, "level");
            Objects.requireNonNull(attacker, "attacker");
            if (pending && targetId == null) {
                throw new IllegalArgumentException("Pending Cave Centipede bite requires a target.");
            }
        }

        static BiteState pending(
                ServerLevel level,
                LivingEntity attacker,
                UUID targetId,
                long resolveAtTick
        ) {
            return new BiteState(
                    level,
                    attacker,
                    Objects.requireNonNull(targetId, "targetId"),
                    true,
                    resolveAtTick,
                    Long.MIN_VALUE
            );
        }

        static BiteState recovery(
                ServerLevel level,
                LivingEntity attacker,
                long recoveryUntilTick
        ) {
            return new BiteState(
                    level,
                    attacker,
                    null,
                    false,
                    Long.MIN_VALUE,
                    recoveryUntilTick
            );
        }
    }
}
