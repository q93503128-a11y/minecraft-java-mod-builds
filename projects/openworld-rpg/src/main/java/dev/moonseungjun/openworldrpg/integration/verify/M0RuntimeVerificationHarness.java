package dev.moonseungjun.openworldrpg.integration.verify;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectMinecraftDamageApplicator;
import dev.moonseungjun.openworldrpg.combat.state.AttributeAllocation;
import dev.moonseungjun.openworldrpg.combat.state.EquipmentCombatState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerCombatBuildState;
import dev.moonseungjun.openworldrpg.combat.state.ProjectWeaponFamily;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import dev.moonseungjun.openworldrpg.integration.spellengine.SpellEngineAuthorityAdapter;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

/** CI-only exact dependency runtime verification. Never enabled in normal gameplay. */
public final class M0RuntimeVerificationHarness {
    private static final String ENABLE_PROPERTY = "openworld_rpg.m0RuntimeVerification";
    private static final int EARTHLOONG_SURVIVAL_TICKS = 40;
    private static final UUID VERIFICATION_ACTOR_ID =
            UUID.fromString("2d35c0ee-0ee0-4f00-8f0f-000000000001");
    private static VerificationSession activeSession;

    private M0RuntimeVerificationHarness() {
    }

    public static void initialize(RuntimeProfile profile, Logger logger) {
        if (profile != RuntimeProfile.GAMEPLAY
                || !Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "false"))) {
            return;
        }

        ServerLifecycleEvents.SERVER_STARTED.register(server -> beginVerification(server.overworld(), logger));
        ServerTickEvents.END_SERVER_TICK.register(server -> finishWhenReady(server.overworld(), logger));
        logger.info("Openworld RPG M0 runtime impact verification armed for gameplay server.");
    }

    private static synchronized void beginVerification(ServerLevel level, Logger logger) {
        if (activeSession != null) {
            throw new IllegalStateException("M0 Earthloong verification session already exists.");
        }

        BlockPos spawn = level.getSharedSpawnPos().above(4);
        Entity targetEntity = ExternalActorBindingRuntime.spawnAuthored(
                level,
                spawn,
                ExternalActorCombatProfile.r01Earthloong().entityId()
        );
        if (!(targetEntity instanceof LivingEntity target)) {
            targetEntity.discard();
            throw new IllegalStateException("M0 Earthloong verification target is not a living entity.");
        }

        long startTick = level.getGameTime();
        activeSession = new VerificationSession(target, startTick, startTick + EARTHLOONG_SURVIVAL_TICKS);
        logger.info(
                "OPENWORLD_RPG_M0_EARTHLOONG_SURVIVAL_ARMED entityId={} startTick={} verifyTick={}",
                target.getId(),
                startTick,
                startTick + EARTHLOONG_SURVIVAL_TICKS
        );
    }

    private static synchronized void finishWhenReady(ServerLevel level, Logger logger) {
        VerificationSession session = activeSession;
        if (session == null || level.getGameTime() < session.verifyTick()) {
            return;
        }
        activeSession = null;

        LivingEntity target = session.target();
        if (target.isRemoved() || !target.isAlive() || target.level() != level) {
            throw new IllegalStateException(
                    "Earthloong did not survive the authored spawn path for "
                            + EARTHLOONG_SURVIVAL_TICKS + " server ticks."
            );
        }

        verifyImpactAfterSurvival(level, target, session.startTick(), logger);
    }

    private static void verifyImpactAfterSurvival(
            ServerLevel level,
            LivingEntity target,
            long spawnTick,
            Logger logger
    ) {
        Entity attackerEntity = null;
        try {
            EntityType<?> attackerType = BuiltInRegistries.ENTITY_TYPE
                    .getOptional(Identifier.parse("minecraft:armor_stand"))
                    .orElseThrow(() -> new IllegalStateException("Vanilla armor_stand registry entry is missing."));
            attackerEntity = attackerType.spawn(
                    level,
                    target.blockPosition().offset(3, 0, 0),
                    EntitySpawnReason.COMMAND
            );
            if (!(attackerEntity instanceof LivingEntity attacker)) {
                throw new IllegalStateException("M0 verification attacker did not spawn as a living entity.");
            }

            long gameTick = level.getGameTime();
            float proxyHpBefore = target.getHealth();
            var canonicalHpBefore = ExternalActorBindingRuntime.canonicalHealthSnapshot(target)
                    .orElseThrow(() -> new IllegalStateException("Earthloong canonical HP state was not initialized."));
            var poiseBefore = ExternalActorBindingRuntime.poiseSnapshot(target, gameTick)
                    .orElseThrow(() -> new IllegalStateException("Earthloong poise state was not initialized."));

            var verificationBuild = new PlayerCombatBuildState(
                    8,
                    RootClass.MAGE,
                    new AttributeAllocation(0, 0, 0, 0, 7, 0),
                    EquipmentCombatState.weaponOnly(ProjectWeaponFamily.STAFF, 8)
            );
            var sourceSnapshot = verificationBuild.damageSource(ProjectImpactTransaction.DamageSchool.MAGIC);
            var targetSnapshot = ExternalActorBindingRuntime.projectTargetSnapshot(target, gameTick)
                    .orElseThrow(() -> new IllegalStateException("Earthloong project target snapshot was unavailable."));

            var decision = SpellEngineAuthorityAdapter.authority().onImpact(
                    VERIFICATION_ACTOR_ID,
                    ProjectSpellSpec.arcBolt().id(),
                    gameTick,
                    target.getId(),
                    9999.0,
                    17.0,
                    sourceSnapshot,
                    targetSnapshot
            );
            if (!decision.accepted()
                    || Math.abs(sourceSnapshot.weaponPower() - 30.0) > 0.0001
                    || Math.abs(sourceSnapshot.weightedOffensiveStat() - 10.95) > 0.0001
                    || Math.abs(sourceSnapshot.poiseOutputMultiplier() - 0.85) > 0.0001
                    || Math.abs(decision.finalDamage() - 29.0) > 0.0001
                    || Math.abs(decision.poiseDamage() - 4.25) > 0.0001) {
                throw new IllegalStateException("Canonical Arc Bolt verification result changed: " + decision);
            }

            if (!ProjectMinecraftDamageApplicator.applyDirectMagic(attacker, target, decision.finalDamage())) {
                throw new IllegalStateException("Minecraft rejected the project direct-magic verification hit.");
            }

            float proxyHpAfter = target.getHealth();
            var canonicalHpAfter = ExternalActorBindingRuntime.canonicalHealthSnapshot(target)
                    .orElseThrow(() -> new IllegalStateException("Earthloong canonical HP state disappeared."));
            double expectedCanonicalAfter = canonicalHpBefore.currentHealth() - decision.finalDamage();
            if (Math.abs(canonicalHpBefore.currentHealth() - 4900.0) > 0.0001
                    || Math.abs(canonicalHpAfter.currentHealth() - expectedCanonicalAfter) > 0.0001
                    || Math.abs(expectedCanonicalAfter - 4871.0) > 0.0001) {
                throw new IllegalStateException(
                        "Project canonical HP delta mismatch: before=" + canonicalHpBefore
                                + " expectedAfter=" + expectedCanonicalAfter
                                + " actualAfter=" + canonicalHpAfter
                );
            }

            double expectedProxyAfter = target.getMaxHealth() * canonicalHpAfter.fraction();
            if (Math.abs(proxyHpAfter - expectedProxyAfter) > 0.01F) {
                throw new IllegalStateException(
                        "Minecraft proxy HP is not synchronized to canonical HP: before=" + proxyHpBefore
                                + " expectedAfter=" + expectedProxyAfter
                                + " actualAfter=" + proxyHpAfter
                );
            }

            var arcBoltPoise = ExternalActorBindingRuntime.applyProjectPoiseDamage(
                    target,
                    decision.poiseDamage(),
                    gameTick
            ).orElseThrow(() -> new IllegalStateException("Earthloong poise application was unavailable."));
            if (Math.abs(arcBoltPoise.remainingPoise() - 185.75) > 0.0001) {
                throw new IllegalStateException("Arc Bolt poise delta mismatch: " + arcBoltPoise);
            }

            var breakResult = ExternalActorBindingRuntime.applyProjectPoiseDamage(
                    target,
                    arcBoltPoise.remainingPoise(),
                    gameTick
            ).orElseThrow(() -> new IllegalStateException("Earthloong poise break application was unavailable."));
            var brokenSnapshot = ExternalActorBindingRuntime.projectTargetSnapshot(target, gameTick)
                    .orElseThrow(() -> new IllegalStateException("Earthloong broken target snapshot was unavailable."));
            if (!breakResult.breakTriggered()
                    || Math.abs(brokenSnapshot.authoredDamageTakenMultiplier() - 1.15) > 0.0001) {
                throw new IllegalStateException(
                        "Earthloong runtime poise break did not expose canonical +15% damage window."
                );
            }

            logger.info(
                    "OPENWORLD_RPG_M0_RUNTIME_IMPACT_PASS target={} canonicalHpBefore={} canonicalHpAfter={} "
                            + "proxyHpBefore={} proxyHpAfter={} weaponPower={} weightedStat={} damage={} "
                            + "poiseBefore={} poiseAfterArcBolt={} breakDamageTakenMultiplier={} survivalTicks={}",
                    ExternalActorCombatProfile.r01Earthloong().entityId(),
                    canonicalHpBefore.currentHealth(),
                    canonicalHpAfter.currentHealth(),
                    proxyHpBefore,
                    proxyHpAfter,
                    sourceSnapshot.weaponPower(),
                    sourceSnapshot.weightedOffensiveStat(),
                    decision.finalDamage(),
                    poiseBefore.currentPoise(),
                    arcBoltPoise.remainingPoise(),
                    brokenSnapshot.authoredDamageTakenMultiplier(),
                    gameTick - spawnTick
            );
        } finally {
            if (attackerEntity != null) {
                attackerEntity.discard();
            }
            target.discard();
        }
    }

    private record VerificationSession(LivingEntity target, long startTick, long verifyTick) {
    }
}
