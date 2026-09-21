package dev.moonseungjun.openworldrpg.integration.actor;

import dev.moonseungjun.openworldrpg.integration.bootstrap.RuntimeProfile;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlay;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlayLoader;
import dev.moonseungjun.openworldrpg.integration.overlay.ActorIntegrationOverlayValidator;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.slf4j.Logger;

public final class ExternalActorBindingRuntime {
    private static final String EARTHLOONG_OVERLAY_RESOURCE =
            "/data/openworld_rpg/integration/actors/r01_earthloong.json";
    private static final String AUTHORED_SPAWN_TAG = "openworld_rpg.authored_spawn";
    private static final String NO_CAPTURE_TAG = "openworld_rpg.no_capture";

    private static final Map<String, ExternalActorCombatProfile> COMBAT_PROFILES = new ConcurrentHashMap<>();
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

        ResourceLocation earthloongId = ResourceLocation.parse(earthloong.target());
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(earthloongId)) {
            throw new IllegalStateException(
                    "Openworld RPG required R01 Earthloong registry target is missing: " + earthloongId
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

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            Optional<ExternalActorCombatProfile> actorProfile = combatProfile(entity);
            if (actorProfile.isEmpty() || !(entity instanceof LivingEntity living)) {
                return;
            }

            applyProjectCombatStats(living, actorProfile.get());
            living.addTag(NO_CAPTURE_TAG);
        });

        initialized = true;
        logger.info(
                "Openworld RPG external actor binding armed for {}: project HP/DEF/MR/Poise profile, "
                        + "authored spawn path and donor progression suppression hooks ready.",
                profileData.entityId()
        );
    }

    public static Optional<ExternalActorCombatProfile> combatProfile(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(COMBAT_PROFILES.get(id.toString()));
    }

    public static boolean ownsProgression(Entity entity) {
        return combatProfile(entity).isPresent();
    }

    public static boolean captureForbidden(Entity entity) {
        return combatProfile(entity).isPresent() || entity.getTags().contains(NO_CAPTURE_TAG);
    }

    public static Entity spawnAuthored(ServerLevel level, BlockPos pos, String entityId) {
        ExternalActorCombatProfile profile = COMBAT_PROFILES.get(entityId);
        if (profile == null) {
            throw new IllegalArgumentException("No project external-actor binding exists for: " + entityId);
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(entityId))
                .orElseThrow(() -> new IllegalStateException("Required external actor registry target vanished: " + entityId));
        Entity entity = type.spawn(level, pos, MobSpawnType.COMMAND);
        if (entity == null) {
            throw new IllegalStateException("Failed to spawn authored external actor: " + entityId);
        }
        entity.addTag(AUTHORED_SPAWN_TAG);
        entity.addTag(NO_CAPTURE_TAG);
        if (entity instanceof LivingEntity living) {
            applyProjectCombatStats(living, profile);
        }
        return entity;
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

    private static void applyProjectCombatStats(
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
        living.setHealth((float) Math.max(
                0.0,
                Math.min(profile.maxHealth(), profile.maxHealth() * healthRatio)
        ));
    }
}
