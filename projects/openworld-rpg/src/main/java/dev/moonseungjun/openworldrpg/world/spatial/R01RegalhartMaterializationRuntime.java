package dev.moonseungjun.openworldrpg.world.spatial;

import dev.moonseungjun.openworldrpg.combat.encounter.r01.R01RegalhartCombatRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import dev.moonseungjun.openworldrpg.progression.r01.R01RegalhartRewardService;
import dev.moonseungjun.openworldrpg.progression.r01.R01RegalhartTerritoryController;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Physical materialization/reset gate for the accepted R01 Regalhart Rootshade start centers.
 *
 * <p>No arbitrary search radius is invented. The exact deterministic center either supports the
 * donor entity's real spawn AABB and all authored visibility/distance gates, or the operation fails
 * closed.</p>
 */
public final class R01RegalhartMaterializationRuntime {
    private R01RegalhartMaterializationRuntime() {
    }

    public static Optional<MaterializationPlan> planRepeatStart(
            MinecraftServer server,
            boolean activeBossInstance,
            CameraVisibility cameraVisibility
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(cameraVisibility, "cameraVisibility");

        return R01RegalhartTerritoryController
                .selectedRepeatStartAnchor(server, activeBossInstance)
                .flatMap(anchor -> planAnchor(
                        server.overworld(),
                        anchor,
                        null,
                        cameraVisibility
                ));
    }

    /**
     * Re-derives the deterministic center for the currently persisted encounter cycle.
     *
     * <p>This does not bypass repeat eligibility for spawning. It exists so an already-active boss
     * can return to the same cycle-owned controller start on disengage.</p>
     */
    public static Optional<MaterializationPlan> planCurrentCycleStart(
            MinecraftServer server,
            Entity excludedEntity,
            CameraVisibility cameraVisibility
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(cameraVisibility, "cameraVisibility");

        var state = R01RegalhartTerritoryController.state(server);
        var anchor = R01RegalhartSpatialAuthority.selectStartAnchor(
                server.overworld().getSeed(),
                state.cycleIndex()
        );
        return planAnchor(
                server.overworld(),
                anchor,
                excludedEntity,
                cameraVisibility
        );
    }

    /**
     * Future production spawn seam. Until Regalhart reward/presentation admission opens in the
     * external-actor catalog this method always fails closed without creating an entity.
     */
    public static Optional<LivingEntity> materializeRepeatIfAdmitted(
            MinecraftServer server,
            boolean activeBossInstance,
            CameraVisibility cameraVisibility
    ) {
        Objects.requireNonNull(server, "server");
        if (!R01ExternalActorCatalog.productionSpawnReady(
                R01ExternalActorCatalog.REGALHART
        )) {
            return Optional.empty();
        }

        var plan = planRepeatStart(
                server,
                activeBossInstance,
                cameraVisibility
        );
        if (plan.isEmpty()) {
            return Optional.empty();
        }

        MaterializationPlan accepted = plan.orElseThrow();
        Entity entity = ExternalActorBindingRuntime.spawnAuthored(
                server.overworld(),
                accepted.spawnBlockPos(),
                R01ExternalActorCatalog.REGALHART
        );
        return entity instanceof LivingEntity living
                ? Optional.of(living)
                : Optional.empty();
    }

    /**
     * Canonical disengage reset: return to this cycle's authored center, restore project HP/poise/
     * hostile statuses and clear Regalhart execution state. No reward is granted.
     */
    public static boolean resetExistingToControllerStart(
            MinecraftServer server,
            LivingEntity regalhart,
            CameraVisibility cameraVisibility
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(cameraVisibility, "cameraVisibility");
        if (!acceptedAuthoredRegalhart(server, regalhart)) {
            return false;
        }

        var plan = planCurrentCycleStart(
                server,
                regalhart,
                cameraVisibility
        );
        if (plan.isEmpty()) {
            return false;
        }

        MaterializationPlan accepted = plan.orElseThrow();
        if (regalhart instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }

        regalhart.teleportTo(
                accepted.spawnCenter().x,
                accepted.spawnCenter().y,
                accepted.spawnCenter().z
        );
        regalhart.setDeltaMovement(Vec3.ZERO);
        R01RegalhartCombatRuntime.resetEncounterState(regalhart);
        if (!ExternalActorBindingRuntime.resetProjectCombatState(
                regalhart,
                regalhart.level().getGameTime()
        )) {
            return false;
        }

        R01RegalhartRewardService.abortActiveEncounter(regalhart);
        R01RegalhartTerritoryController.acknowledgeDisengage(server);
        return true;
    }

    static Optional<MaterializationPlan> planAnchor(
            ServerLevel level,
            R01RegalhartSpatialBindingData.StartAnchor anchor,
            Entity excludedEntity,
            CameraVisibility cameraVisibility
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(cameraVisibility, "cameraVisibility");

        Vec3 spawnCenter = anchor.spawnCenter();
        if (!R01RegalhartSpatialAuthority.insideTerritory(
                spawnCenter.x,
                spawnCenter.z
        )) {
            return Optional.empty();
        }

        BlockPos supportPos = new BlockPos(
                anchor.x(),
                anchor.y(),
                anchor.z()
        );
        if (!level.hasChunkAt(supportPos)
                || !level.getBlockState(supportPos).isFaceSturdy(
                        level,
                        supportPos,
                        Direction.UP
                )) {
            return Optional.empty();
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
                .getOptional(
                        Identifier.parse(
                                R01ExternalActorCatalog.REGALHART
                        )
                )
                .orElse(null);
        if (type == null) {
            return Optional.empty();
        }

        AABB spawnBox = type.getSpawnAABB(
                spawnCenter.x,
                spawnCenter.y,
                spawnCenter.z
        );
        boolean collisionFree = excludedEntity == null
                ? level.noCollision(spawnBox)
                : level.noCollision(excludedEntity, spawnBox);
        if (!collisionFree || level.containsAnyLiquid(spawnBox)) {
            return Optional.empty();
        }

        List<ServerPlayer> activePlayers = level.players().stream()
                .filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .filter(ServerPlayer::isAlive)
                .filter(player -> !player.isSpectator())
                .toList();

        boolean directlyVisible = activePlayers.stream()
                .anyMatch(player ->
                        cameraVisibility.directlyVisible(
                                player,
                                spawnBox
                        )
                );
        List<Vec3> playerPositions = activePlayers.stream()
                .map(ServerPlayer::position)
                .toList();
        if (!R01RegalhartSpatialAuthority.materializationAllowed(
                anchor,
                playerPositions,
                directlyVisible
        )) {
            return Optional.empty();
        }

        return Optional.of(
                new MaterializationPlan(
                        anchor,
                        spawnCenter,
                        spawnBox,
                        supportPos
                )
        );
    }

    private static boolean acceptedAuthoredRegalhart(
            MinecraftServer server,
            LivingEntity regalhart
    ) {
        if (regalhart == null
                || regalhart.isRemoved()
                || regalhart.level() != server.overworld()
                || regalhart.level().isClientSide()
                || !ExternalActorBindingRuntime.isAuthoredSpawn(regalhart)) {
            return false;
        }
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(
                regalhart.getType()
        );
        return id != null
                && R01ExternalActorCatalog.REGALHART
                        .equals(id.toString());
    }

    @FunctionalInterface
    public interface CameraVisibility {
        boolean directlyVisible(
                ServerPlayer player,
                AABB spawnBox
        );
    }

    public record MaterializationPlan(
            R01RegalhartSpatialBindingData.StartAnchor anchor,
            Vec3 spawnCenter,
            AABB spawnBox,
            BlockPos supportPos
    ) {
        public MaterializationPlan {
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(spawnCenter, "spawnCenter");
            Objects.requireNonNull(spawnBox, "spawnBox");
            Objects.requireNonNull(supportPos, "supportPos");
        }

        public BlockPos spawnBlockPos() {
            return BlockPos.containing(spawnCenter);
        }
    }
}
