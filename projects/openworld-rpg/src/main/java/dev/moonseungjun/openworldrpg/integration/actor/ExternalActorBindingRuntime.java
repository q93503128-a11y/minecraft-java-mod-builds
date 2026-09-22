package dev.moonseungjun.openworldrpg.integration.actor;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.state.ProjectHealthRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.ProjectPoiseRuntimeState;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongEncounterDataLoader;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlay;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlayLoader;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlayValidator;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoublePredicate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;

public final class ExternalActorBindingRuntime {
    private static final String EARTHLOONG_OVERLAY_RESOURCE =
            "/data/openworld_rpg/integration/actors/r01_earthloong.json";
    private static final String AUTHORED_SPAWN_TAG = "openworld_rpg.authored_spawn";
    private static final String NO_CAPTURE_TAG = "openworld_rpg.no_capture";

    private static final Map<String, ExternalActorCombatProfile> COMBAT_PROFILES = new ConcurrentHashMap<>();
    private static final Map<UUID, ProjectHealthRuntimeState> HEALTH_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, ProjectPoiseRuntimeState> POISE_STATES = new ConcurrentHashMap<>();
    private static volatile boolean initialized;

    private ExternalActorBindingRuntime() {
    }

    public static synchronized void initialize(RuntimeProfile profile, Logger logger) {
        if (initialized) {
            return;
        }
        if (profile == RuntimeProfile.CORE) {
            initialized = true;
            logger.info("Openworld RPG external actor binding runtime inactive for core isolation profile.");
            return;
        }

        ActorIntegrationOverlay earthloong = loadRequiredOverlay(EARTHLOONG_OVERLAY_RESOURCE);
        var validation = ActorIntegrationOverlayValidator.validate(earthloong);
        if (validation.hasErrors()) {
            throw new IllegalStateException(
                    "Openworld RPG required actor overlay is invalid: " + earthloong.target()
                            + " issues=" + validation.issues()
            );
        }

        ExternalActorCombatProfile profileData = ExternalActorCombatProfile.r01Earthloong();
        if (!profileData.entityId().equals(earthloong.target())) {
            throw new IllegalStateException(
                    "Earthloong combat profile target does not match actor overlay: "
                            + profileData.entityId() + " != " + earthloong.target()
            );
        }
        COMBAT_PROFILES.put(profileData.entityId(), profileData);

        var encounterData = R01EarthloongEncounterDataLoader.loadBundled();
        if (encounterData.contentLevel() != profileData.contentLevel()) {
            throw new IllegalStateException(
                    "Earthloong encounter-data level does not match actor profile: "
                            + encounterData.contentLevel() + " != " + profileData.contentLevel()
            );
        }

        /*
         * Fabric does not guarantee a dependency's ModInitializer runs before ours merely because the
         * dependency is present. Validate the concrete registry target at SERVER_STARTING, after all
         * common entrypoints have completed, while keeping profile/ownership hooks registered early.
         */
        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                validateRequiredRegistryTarget(profileData.entityId(), logger)
        );

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            Optional<ExternalActorCombatProfile> actorProfile = combatProfile(entity);
            if (actorProfile.isEmpty() || !(entity instanceof LivingEntity living)) {
                return;
            }

            double healthFraction = applyProjectCombatStats(living, actorProfile.get());
            ensureHealthState(living, actorProfile.get(), healthFraction);
            ensurePoiseState(living, actorProfile.get(), level.getGameTime());
            living.addTag(NO_CAPTURE_TAG);
            if (living instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            HEALTH_STATES.remove(entity.getUUID());
            POISE_STATES.remove(entity.getUUID());
        });

        initialized = true;
        logger.info(
                "Openworld RPG external actor binding armed for {}: project HP/DEF/MR/Poise profile, "
                        + "authored spawn path, canonical encounter/impact data and donor progression "
                        + "suppression hooks ready; exact registry validation scheduled for server start.",
                profileData.entityId()
        );
    }

    public static Optional<ExternalActorCombatProfile> combatProfile(Entity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(COMBAT_PROFILES.get(id.toString()));
    }

    public static boolean ownsProgression(Entity entity) {
        return combatProfile(entity).isPresent();
    }

    public static boolean ownsDamageAuthority(Entity entity) {
        return combatProfile(entity).isPresent();
    }

    public static boolean captureForbidden(Entity entity) {
        return combatProfile(entity).isPresent() || entity.entityTags().contains(NO_CAPTURE_TAG);
    }

    public static Optional<ProjectHealthRuntimeState.Snapshot> canonicalHealthSnapshot(
            LivingEntity living
    ) {
        return combatProfile(living).map(profile ->
                ensureHealthStateFromProxy(living, profile).snapshot()
        );
    }

    public static Optional<ProjectHealthRuntimeState.Application> applyProjectHealthDamage(
            LivingEntity living,
            double canonicalDamage,
            DoublePredicate proxyDamageApplier
    ) {
        return combatProfile(living).flatMap(profile -> {
            ProjectHealthRuntimeState state = ensureHealthStateFromProxy(living, profile);
            double canonicalApplied = state.previewAppliedDamage(canonicalDamage);
            if (canonicalApplied <= 0.0) {
                return Optional.empty();
            }

            double proxyMax = living.getMaxHealth();
            double proxyDamage = proxyMax * canonicalApplied / profile.maxHealth();
            if (!proxyDamageApplier.test(proxyDamage)) {
                return Optional.empty();
            }

            ProjectHealthRuntimeState.Application result = state.applyDamage(canonicalDamage);
            living.setHealth((float) Math.max(
                    0.0,
                    Math.min(proxyMax, proxyMax * result.fraction())
            ));
            return Optional.of(result);
        });
    }

    public static Optional<ProjectImpactTransaction.DamageTargetSnapshot> projectTargetSnapshot(
            LivingEntity living,
            long gameTick
    ) {
        return combatProfile(living).map(profile -> {
            ProjectPoiseRuntimeState.Snapshot poise =
                    ensurePoiseState(living, profile, gameTick).snapshot(gameTick);
            return profile.projectTargetSnapshot(poise.damageTakenMultiplier());
        });
    }

    public static Optional<ProjectPoiseRuntimeState.Snapshot> poiseSnapshot(
            LivingEntity living,
            long gameTick
    ) {
        return combatProfile(living).map(profile ->
                ensurePoiseState(living, profile, gameTick).snapshot(gameTick)
        );
    }

    public static Optional<ProjectPoiseRuntimeState.Application> applyProjectPoiseDamage(
            LivingEntity living,
            double rawPoiseDamage,
            long gameTick
    ) {
        return combatProfile(living).map(profile ->
                ensurePoiseState(living, profile, gameTick).apply(rawPoiseDamage, gameTick)
        );
    }

    public static Entity spawnAuthored(ServerLevel level, BlockPos pos, String entityId) {
        ExternalActorCombatProfile profile = COMBAT_PROFILES.get(entityId);
        if (profile == null) {
            throw new IllegalArgumentException("No project external-actor binding exists for: " + entityId);
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(entityId))
                .orElseThrow(() -> new IllegalStateException("Required external actor registry target vanished: " + entityId));
        Entity entity = type.spawn(level, pos, EntitySpawnReason.COMMAND);
        if (entity == null) {
            throw new IllegalStateException("Failed to spawn authored external actor: " + entityId);
        }
        entity.addTag(AUTHORED_SPAWN_TAG);
        entity.addTag(NO_CAPTURE_TAG);
        if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
        }
        if (entity instanceof LivingEntity living) {
            double healthFraction = applyProjectCombatStats(living, profile);
            ensureHealthState(living, profile, healthFraction);
            ensurePoiseState(living, profile, level.getGameTime());
        }
        return entity;
    }

    private static ProjectHealthRuntimeState ensureHealthStateFromProxy(
            LivingEntity living,
            ExternalActorCombatProfile profile
    ) {
        float proxyMax = living.getMaxHealth();
        float proxyHealth = living.getHealth();
        double fraction = proxyMax > 0.0F ? proxyHealth / proxyMax : 1.0;
        return ensureHealthState(living, profile, Math.max(0.0, Math.min(1.0, fraction)));
    }

    private static ProjectHealthRuntimeState ensureHealthState(
            LivingEntity living,
            ExternalActorCombatProfile profile,
            double healthFraction
    ) {
        return HEALTH_STATES.computeIfAbsent(
                living.getUUID(),
                ignored -> ProjectHealthRuntimeState.atFraction(profile.maxHealth(), healthFraction)
        );
    }

    private static ProjectPoiseRuntimeState ensurePoiseState(
            LivingEntity living,
            ExternalActorCombatProfile profile,
            long gameTick
    ) {
        return POISE_STATES.computeIfAbsent(
                living.getUUID(),
                ignored -> ProjectPoiseRuntimeState.boss(profile.poiseMax(), gameTick)
        );
    }

    private static void validateRequiredRegistryTarget(String entityId, Logger logger) {
        Identifier requiredId = Identifier.parse(entityId);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(requiredId)) {
            var externalActorCandidates = BuiltInRegistries.ENTITY_TYPE.keySet().stream()
                    .map(Object::toString)
                    .filter(id -> {
                        String lower = id.toLowerCase();
                        return lower.contains("earth")
                                || lower.contains("loux")
                                || lower.contains("regal")
                                || lower.contains("steelboar")
                                || lower.contains("nature_spirit")
                                || lower.contains("ferox")
                                || lower.contains("deathworm")
                                || lower.contains("hydra")
                                || lower.contains("riptooth")
                                || lower.contains("abyss")
                                || lower.contains("wyvern")
                                || lower.contains("inferno");
                    })
                    .sorted()
                    .toList();
            throw new IllegalStateException(
                    "Openworld RPG required R01 Earthloong registry target is missing at server start: "
                            + requiredId + "; installed actor-name candidates=" + externalActorCandidates
            );
        }

        logger.info(
                "Openworld RPG external actor registry target verified at server start: {}.",
                requiredId
        );
    }

    private static ActorIntegrationOverlay loadRequiredOverlay(String resourcePath) {
        try (InputStream stream = ExternalActorBindingRuntime.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing required actor overlay resource: " + resourcePath);
            }
            return ActorIntegrationOverlayLoader.parse(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load required actor overlay: " + resourcePath, exception);
        }
    }

    private static double applyProjectCombatStats(
            LivingEntity living,
            ExternalActorCombatProfile profile
    ) {
        var maxHealth = living.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            throw new IllegalStateException(
                    "Required external actor has no MAX_HEALTH attribute: " + profile.entityId()
            );
        }

        float oldMax = living.getMaxHealth();
        float oldHealth = living.getHealth();
        double healthRatio = oldMax > 0.0F ? oldHealth / oldMax : 1.0;

        maxHealth.setBaseValue(profile.maxHealth());
        float proxyMax = living.getMaxHealth();
        living.setHealth((float) Math.max(
                0.0,
                Math.min(proxyMax, proxyMax * healthRatio)
        ));
        return Math.max(0.0, Math.min(1.0, healthRatio));
    }
}
