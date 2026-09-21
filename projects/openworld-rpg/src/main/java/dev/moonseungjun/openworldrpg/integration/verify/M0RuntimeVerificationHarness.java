package dev.moonseungjun.openworldrpg.integration.verify;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectMinecraftDamageApplicator;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import dev.moonseungjun.openworldrpg.integration.spellengine.SpellEngineAuthorityAdapter;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

/**
 * CI-only runtime verification. Never enabled in normal gameplay.
 *
 * <p>This uses production actor binding, project impact resolution, Minecraft final-damage
 * application and runtime poise state against the exact Earthloong registry entity. The source
 * snapshot is an explicit verification fixture and is never installed into player runtime state.</p>
 */
public final class M0RuntimeVerificationHarness {
    private static final String ENABLE_PROPERTY = "openworld_rpg.m0RuntimeVerification";
    private static final UUID VERIFICATION_ACTOR_ID =
            UUID.fromString("2d35c0ee-0ee0-4f00-8f0f-000000000001");

    private M0RuntimeVerificationHarness() {
    }

    public static void initialize(RuntimeProfile profile, Logger logger) {
        if (profile != RuntimeProfile.GAMEPLAY
                || !Boolean.parseBoolean(System.getProperty(ENABLE_PROPERTY, "false"))) {
            return;
        }

        ServerLifecycleEvents.SERVER_STARTED.register(server -> verify(server.overworld(), logger));
        logger.info("Openworld RPG M0 runtime impact verification armed for gameplay server.");
    }

    private static void verify(ServerLevel level, Logger logger) {
        BlockPos spawn = level.getSharedSpawnPos().above(8);
        Entity targetEntity = null;
        Entity attackerEntity = null;

        try {
            targetEntity = ExternalActorBindingRuntime.spawnAuthored(
                    level,
                    spawn,
                    ExternalActorCombatProfile.r01Earthloong().entityId()
            );
            attackerEntity = EntityType.ARMOR_STAND.spawn(
                    level,
                    spawn.offset(3, 0, 0),
                    EntitySpawnReason.COMMAND
            );

            if (!(targetEntity instanceof LivingEntity target)
                    || !(attackerEntity instanceof LivingEntity attacker)) {
                throw new IllegalStateException("M0 verification actors did not spawn as living entities.");
            }

            long gameTick = level.getGameTime();
            float hpBefore = target.getHealth();
            var poiseBefore = ExternalActorBindingRuntime.poiseSnapshot(target, gameTick)
                    .orElseThrow(() -> new IllegalStateException("Earthloong poise state was not initialized."));

            var sourceSnapshot = new ProjectImpactTransaction.DamageSourceSnapshot(
                    8,
                    30.0,
                    20.0,
                    0.0,
                    1.0
            );
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
                    || Math.abs(decision.finalDamage() - 32.0) > 0.0001
                    || Math.abs(decision.poiseDamage() - 5.0) > 0.0001) {
                throw new IllegalStateException("Canonical Arc Bolt verification result changed: " + decision);
            }

            if (!ProjectMinecraftDamageApplicator.applyDirectMagic(
                    attacker,
                    target,
                    decision.finalDamage()
            )) {
                throw new IllegalStateException("Minecraft rejected the project direct-magic verification hit.");
            }

            float hpAfter = target.getHealth();
            float expectedHpAfter = hpBefore - (float) decision.finalDamage();
            if (Math.abs(hpAfter - expectedHpAfter) > 0.001F) {
                throw new IllegalStateException(
                        "Project direct-magic HP delta mismatch: before=" + hpBefore
                                + " expectedAfter=" + expectedHpAfter
                                + " actualAfter=" + hpAfter
                );
            }

            var arcBoltPoise = ExternalActorBindingRuntime.applyProjectPoiseDamage(
                    target,
                    decision.poiseDamage(),
                    gameTick
            ).orElseThrow(() -> new IllegalStateException("Earthloong poise application was unavailable."));
            if (Math.abs(arcBoltPoise.remainingPoise() - 185.0) > 0.0001) {
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
                    "OPENWORLD_RPG_M0_RUNTIME_IMPACT_PASS target={} hpBefore={} hpAfter={} "
                            + "damage={} poiseBefore={} poiseAfterArcBolt={} breakDamageTakenMultiplier={}",
                    ExternalActorCombatProfile.r01Earthloong().entityId(),
                    hpBefore,
                    hpAfter,
                    decision.finalDamage(),
                    poiseBefore.currentPoise(),
                    arcBoltPoise.remainingPoise(),
                    brokenSnapshot.authoredDamageTakenMultiplier()
            );
        } finally {
            if (attackerEntity != null) {
                attackerEntity.discard();
            }
            if (targetEntity != null) {
                targetEntity.discard();
            }
        }
    }
}
