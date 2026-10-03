package dev.moonseungjun.openworldrpg.integration.actor;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.runtime.ProjectHostileStatusRuntime;
import dev.moonseungjun.openworldrpg.combat.state.ProjectHealthRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.ProjectPoiseRuntimeState;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01EarthloongEncounterDataLoader;
import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01NatureSpiritCombatRuntime;
import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlay;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlayLoader;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlayValidator;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
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
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public final class ExternalActorBindingRuntime {
    private static final String EARTHLOONG_OVERLAY_RESOURCE =
            "/data/openworld_rpg/integration/actors/r01_earthloong.json";
    private static final String AUTHORED_SPAWN_TAG = "openworld_rpg.authored_spawn";
    private static final String NO_CAPTURE_TAG = "openworld_rpg.no_capture";
    private static final String CAVE_CENTIPEDE_BODY_CLASS =
            "com.github.alexthe666.alexsmobs.entity.EntityCentipedeBody";

    private static final Map<String, ExternalActorCombatProfile> COMBAT_PROFILES = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> MULTIPART_PARENT_METHODS =
            new ConcurrentHashMap<>();
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

        for (ExternalActorCombatProfile profileData
                : R01ExternalActorCatalog.combatProfiles()) {
            ExternalActorCombatProfile previous = COMBAT_PROFILES.put(
                    profileData.entityId(),
                    profileData
            );
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate R01 external-actor combat profile: "
                                + profileData.entityId()
                );
            }
        }

        ExternalActorCombatProfile earthloongProfile =
                COMBAT_PROFILES.get(R01ExternalActorCatalog.EARTHLOONG);
        if (earthloongProfile == null
                || !earthloongProfile.entityId().equals(earthloong.target())) {
            throw new IllegalStateException(
                    "Earthloong combat profile target does not match actor overlay: "
                            + earthloong.target()
            );
        }

        var encounterData = R01EarthloongEncounterDataLoader.loadBundled();
        if (encounterData.contentLevel() != earthloongProfile.contentLevel()) {
            throw new IllegalStateException(
                    "Earthloong encounter-data level does not match actor profile: "
                            + encounterData.contentLevel() + " != "
                            + earthloongProfile.contentLevel()
            );
        }

        validateCaveCentipedeMultipartBridge(logger);

        /*
         * Fabric does not guarantee a dependency's ModInitializer runs before ours merely because the
         * dependency is present. Validate the concrete registry target at SERVER_STARTING, after all
         * common entrypoints have completed, while keeping profile/ownership hooks registered early.
         */
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            for (String entityId
                    : R01ExternalActorCatalog.requiredRegistryTargets()) {
                validateRequiredRegistryTarget(entityId, logger);
            }
        });

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
            ProjectHostileStatusRuntime.clear(entity.getUUID());
        });

        initialized = true;
        logger.info(
                "Openworld RPG R01 external actor registry gate armed for {} exact pinned targets; "
                        + "{} have project combat-stat authority. Cave Centipede multipart registry "
                        + "and Nature Spirit legacy registry are closed; non-Earthloong production "
                        + "spawns remain gated by attack/reward/presentation acceptance.",
                R01ExternalActorCatalog.requiredRegistryTargets().size(),
                COMBAT_PROFILES.size()
        );
    }

    public static Optional<ExternalActorCombatProfile> combatProfile(Entity entity) {
        String id = registryId(entity);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(COMBAT_PROFILES.get(id));
    }

    /**
     * Resolves the LivingEntity that owns canonical project HP/poise for an attacked entity.
     *
     * <p>Most actors own themselves. Cave Centipede body/tail parts walk the donor parent chain to
     * the exact bound head. Broken/missing chains return empty so damage fails closed.</p>
     */
    public static Optional<LivingEntity> damageAuthorityTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return Optional.empty();
        }
        if (combatProfile(living).isPresent()) {
            return Optional.of(living);
        }

        String id = registryId(entity);
        if (!R01ExternalActorCatalog.multipartCombatProxy(id)) {
            return Optional.empty();
        }
        String expectedOwnerId = R01ExternalActorCatalog.combatOwnerId(id);

        Entity current = entity;
        for (int depth = 0; depth < 16; depth++) {
            Entity parent = multipartParent(current);
            if (parent == null || parent == current) {
                return Optional.empty();
            }
            String parentId = registryId(parent);
            if (expectedOwnerId.equals(parentId)
                    && parent instanceof LivingEntity owner
                    && combatProfile(owner).isPresent()) {
                return Optional.of(owner);
            }
            if (!R01ExternalActorCatalog.multipartCombatProxy(parentId)) {
                return Optional.empty();
            }
            current = parent;
        }
        return Optional.empty();
    }

    public static boolean ownsProgression(Entity entity) {
        String id = registryId(entity);
        return combatProfile(entity).isPresent()
                || R01ExternalActorCatalog.multipartCombatProxy(id);
    }

    public static boolean ownsDamageAuthority(Entity entity) {
        String id = registryId(entity);
        return combatProfile(entity).isPresent()
                || R01ExternalActorCatalog.multipartCombatProxy(id);
    }

    public static boolean captureForbidden(Entity entity) {
        return ownsDamageAuthority(entity)
                || entity.entityTags().contains(NO_CAPTURE_TAG);
    }

    /**
     * True only for actors created through the project's accepted authored-spawn path.
     *
     * <p>Registry/stat binding alone is intentionally insufficient: donor natural spawns and
     * verification fixtures must not inherit production attack bridges merely because their entity
     * type is known.</p>
     */
    public static boolean isAuthoredSpawn(Entity entity) {
        return entity != null
                && entity.entityTags().contains(AUTHORED_SPAWN_TAG);
    }

    public static boolean isAuthoredWeakPointHit(
            LivingEntity target,
            Vec3 hitPosition
    ) {
        if (target == null || hitPosition == null) {
            return false;
        }
        return damageAuthorityTarget(target)
                .flatMap(ExternalActorBindingRuntime::combatProfile)
                .map(ExternalActorCombatProfile::weakPointProfile)
                .filter(profile -> !profile.isEmpty())
                .map(profile -> profile.contains(
                        damageAuthorityTarget(target).orElse(target),
                        hitPosition
                ))
                .orElse(false);
    }

    public static Optional<ProjectHealthRuntimeState.Snapshot> canonicalHealthSnapshot(
            LivingEntity living
    ) {
        return damageAuthorityTarget(living).flatMap(owner ->
                combatProfile(owner).map(profile ->
                        ensureHealthStateFromProxy(owner, profile).snapshot()
                )
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
        return damageAuthorityTarget(living).flatMap(owner ->
                combatProfile(owner).map(profile -> {
                    ProjectPoiseRuntimeState.Snapshot poise =
                            ensurePoiseState(owner, profile, gameTick).snapshot(gameTick);
                    double authoredDamageTakenMultiplier =
                            poise.damageTakenMultiplier()
                                    * R01NatureSpiritCombatRuntime
                                            .directDamageTakenMultiplier(
                                                    owner,
                                                    gameTick
                                            );
                    return profile.projectTargetSnapshot(
                            authoredDamageTakenMultiplier
                    );
                })
        );
    }

    public static Optional<ProjectPoiseRuntimeState.Snapshot> poiseSnapshot(
            LivingEntity living,
            long gameTick
    ) {
        return damageAuthorityTarget(living).flatMap(owner ->
                combatProfile(owner).map(profile ->
                        ensurePoiseState(owner, profile, gameTick).snapshot(gameTick)
                )
        );
    }

    public static Optional<ProjectPoiseRuntimeState.Application> applyProjectPoiseDamage(
            LivingEntity living,
            double rawPoiseDamage,
            long gameTick
    ) {
        return damageAuthorityTarget(living).flatMap(owner ->
                combatProfile(owner).map(profile -> {
                    double adjustedPoiseDamage =
                            rawPoiseDamage
                                    * R01NatureSpiritCombatRuntime
                                            .poiseDamageTakenMultiplier(
                                                    owner,
                                                    gameTick
                                            );
                    ProjectPoiseRuntimeState.Application application =
                            ensurePoiseState(owner, profile, gameTick).apply(
                                    adjustedPoiseDamage,
                                    gameTick
                            );
                    if (application.breakTriggered()) {
                        R01NatureSpiritCombatRuntime.onPoiseBroken(
                                owner,
                                gameTick
                        );
                    }
                    return application;
                })
        );
    }

    public static Entity spawnAuthored(ServerLevel level, BlockPos pos, String entityId) {
        ExternalActorCombatProfile profile = COMBAT_PROFILES.get(entityId);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "No project external-actor combat binding exists for: "
                            + entityId
            );
        }
        if (!R01ExternalActorCatalog.productionSpawnReady(entityId)) {
            throw new IllegalStateException(
                    "R01 external actor is registry/stat bound but its production spawn path "
                            + "is still gated by attack/reward/presentation acceptance: "
                            + entityId
            );
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

    /**
     * Restores project-owned combat state for an existing bound actor without touching donor-owned
     * presentation/passive effects. Intended for authored encounter reset, not ordinary healing.
     */
    public static boolean resetProjectCombatState(
            LivingEntity living,
            long gameTick
    ) {
        if (living == null || living.level().isClientSide() || gameTick < 0L) {
            return false;
        }
        ExternalActorCombatProfile profile = combatProfile(living).orElse(null);
        if (profile == null) {
            return false;
        }

        HEALTH_STATES.put(
                living.getUUID(),
                ProjectHealthRuntimeState.atFraction(
                        profile.maxHealth(),
                        1.0
                )
        );
        POISE_STATES.put(
                living.getUUID(),
                newPoiseState(profile, gameTick)
        );
        living.setHealth(living.getMaxHealth());
        ProjectHostileStatusRuntime.clear(living.getUUID());
        return true;
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
                ignored -> newPoiseState(profile, gameTick)
        );
    }

    private static ProjectPoiseRuntimeState newPoiseState(
            ExternalActorCombatProfile profile,
            long gameTick
    ) {
        return switch (profile.combatRank()) {
            case COMMON, STURDY_COMMON ->
                    ProjectPoiseRuntimeState.common(
                            profile.poiseMax(),
                            gameTick
                    );
            case NORMAL_ELITE ->
                    ProjectPoiseRuntimeState.elite(
                            profile.poiseMax(),
                            gameTick
                    );
            case MINIBOSS ->
                    ProjectPoiseRuntimeState.miniboss(
                            profile.poiseMax(),
                            gameTick
                    );
            case BOSS ->
                    ProjectPoiseRuntimeState.boss(
                            profile.poiseMax(),
                            gameTick
                    );
        };
    }

    private static String registryId(Entity entity) {
        if (entity == null) {
            return null;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id == null ? null : id.toString();
    }

    private static Entity multipartParent(Entity segment) {
        try {
            Object parent = multipartParentMethod(segment.getClass()).invoke(segment);
            return parent instanceof Entity entity ? entity : null;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Could not resolve Cave Centipede multipart parent for "
                            + registryId(segment),
                    exception
            );
        }
    }

    private static Method multipartParentMethod(Class<?> type) {
        return MULTIPART_PARENT_METHODS.computeIfAbsent(type, current -> {
            try {
                Method method = current.getMethod("getParent");
                if (!Entity.class.isAssignableFrom(method.getReturnType())) {
                    throw new IllegalStateException(
                            "Cave Centipede getParent() no longer returns Entity: "
                                    + current.getName()
                    );
                }
                return method;
            } catch (NoSuchMethodException exception) {
                throw new IllegalStateException(
                        "Pinned Cave Centipede multipart API no longer exposes public getParent(): "
                                + current.getName(),
                        exception
                );
            }
        });
    }

    private static void validateCaveCentipedeMultipartBridge(Logger logger) {
        try {
            Class<?> bodyClass = Class.forName(
                    CAVE_CENTIPEDE_BODY_CLASS,
                    false,
                    ExternalActorBindingRuntime.class.getClassLoader()
            );
            multipartParentMethod(bodyClass);
            logger.info(
                    "Openworld RPG Cave Centipede multipart damage bridge verified: "
                            + "body/tail hit proxies route through public getParent() to {}.",
                    R01ExternalActorCatalog.CAVE_CENTIPEDE_HEAD
            );
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException(
                    "Pinned Cave Centipede body class is missing: "
                            + CAVE_CENTIPEDE_BODY_CLASS,
                    exception
            );
        }
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
                                || lower.contains("nature_hamony")
                                || lower.contains("centipede_head")
                                || lower.contains("centipede_body")
                                || lower.contains("centipede_tail")
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
                    "Openworld RPG required R01 external-actor registry target is missing at server start: "
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
