package dev.moonseungjun.openworldrpg.integration.verify;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingRegistry;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Test-artifact-only navigation helper for real-client R01 surface acceptance.
 *
 * <p>These commands are registered only when the dedicated R01 integration verification marker is
 * present. They never mutate candidate status, quest state, rewards or production spatial
 * authority.</p>
 */
public final class R01SpatialReviewHarness {
    private static final String FIRST_COMMAND =
            "owr_r01_surface_first";
    private static final String NEXT_COMMAND =
            "owr_r01_surface_next";
    private static final String PREVIOUS_COMMAND =
            "owr_r01_surface_prev";
    private static final String CURRENT_COMMAND =
            "owr_r01_surface_current";
    private static final String JUMP_COMMAND =
            "owr_r01_surface_jump";

    private static final ConcurrentHashMap<UUID, Integer>
            REVIEW_INDEX = new ConcurrentHashMap<>();

    private static volatile boolean registered;

    private R01SpatialReviewHarness() {
    }

    public static synchronized void registerCommands() {
        if (registered) {
            return;
        }
        if (!R01PlayerVerificationBootstrap.enabled()) {
            return;
        }

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, buildContext, selection) -> {
                    dispatcher.register(
                            Commands.literal(FIRST_COMMAND)
                                    .executes(context -> moveToIndex(
                                            context.getSource().getEntity(),
                                            0
                                    ))
                    );
                    dispatcher.register(
                            Commands.literal(NEXT_COMMAND)
                                    .executes(context -> moveRelative(
                                            context.getSource().getEntity(),
                                            1
                                    ))
                    );
                    dispatcher.register(
                            Commands.literal(PREVIOUS_COMMAND)
                                    .executes(context -> moveRelative(
                                            context.getSource().getEntity(),
                                            -1
                                    ))
                    );
                    dispatcher.register(
                            Commands.literal(CURRENT_COMMAND)
                                    .executes(context -> showCurrent(
                                            context.getSource().getEntity()
                                    ))
                    );
                    dispatcher.register(
                            Commands.literal(JUMP_COMMAND)
                                    .then(Commands.argument(
                                                    "index",
                                                    IntegerArgumentType
                                                            .integer(1)
                                            )
                                            .executes(context -> moveToIndex(
                                                    context.getSource()
                                                            .getEntity(),
                                                    IntegerArgumentType
                                                            .getInteger(
                                                                    context,
                                                                    "index"
                                                            )
                                                            - 1
                                            )))
                    );
                }
        );
        registered = true;
    }

    public static void disconnect(UUID playerId) {
        if (playerId != null) {
            REVIEW_INDEX.remove(playerId);
        }
    }

    static int reviewPointCount() {
        return reviewPoints().size();
    }

    private static int moveRelative(
            Entity commandEntity,
            int delta
    ) {
        if (!(commandEntity instanceof ServerPlayer player)) {
            return 0;
        }
        List<R01SpatialReviewPlan.ReviewPoint> points =
                reviewPoints();
        if (points.isEmpty()) {
            return 0;
        }
        int current = REVIEW_INDEX.getOrDefault(
                player.getUUID(),
                delta > 0 ? -1 : 0
        );
        int next = Math.floorMod(
                current + delta,
                points.size()
        );
        return moveToIndex(player, next);
    }

    private static int moveToIndex(
            Entity commandEntity,
            int index
    ) {
        if (!(commandEntity instanceof ServerPlayer player)) {
            return 0;
        }
        List<R01SpatialReviewPlan.ReviewPoint> points =
                reviewPoints();
        if (index < 0 || index >= points.size()) {
            player.sendSystemMessage(Component.literal(
                    "R01 surface review index must be 1.."
                            + points.size()
                            + "."
            ));
            return 0;
        }
        if (player.level().getServer() == null
                || player.level()
                        != player.level().getServer().overworld()) {
            player.sendSystemMessage(Component.literal(
                    "R01 surface review requires the Azari Overworld."
            ));
            return 0;
        }

        R01SpatialReviewPlan.ReviewPoint point =
                points.get(index);
        ServerLevel level = (ServerLevel) player.level();
        double y = resolveSafeSurfaceY(level, point);

        player.teleportTo(
                point.x() + 0.5,
                y,
                point.z() + 0.5
        );
        player.setDeltaMovement(Vec3.ZERO);
        REVIEW_INDEX.put(player.getUUID(), index);
        sendPoint(player, point, index, points.size(), y);
        return 1;
    }

    private static int showCurrent(Entity commandEntity) {
        if (!(commandEntity instanceof ServerPlayer player)) {
            return 0;
        }
        List<R01SpatialReviewPlan.ReviewPoint> points =
                reviewPoints();
        if (points.isEmpty()) {
            return 0;
        }
        int index = Math.min(
                REVIEW_INDEX.getOrDefault(player.getUUID(), 0),
                points.size() - 1
        );
        R01SpatialReviewPlan.ReviewPoint point =
                points.get(index);
        double y = point.y() == null
                ? player.getY()
                : point.y() + 1.0;
        sendPoint(player, point, index, points.size(), y);
        return 1;
    }

    private static double resolveSafeSurfaceY(
            ServerLevel level,
            R01SpatialReviewPlan.ReviewPoint point
    ) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(point, "point");
        if (point.y() != null) {
            return point.y() + 1.0;
        }
        return level.getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                point.x(),
                point.z()
        ) + 1.0;
    }

    private static void sendPoint(
            ServerPlayer player,
            R01SpatialReviewPlan.ReviewPoint point,
            int index,
            int count,
            double resolvedY
    ) {
        player.sendSystemMessage(Component.literal(String.format(
                Locale.ROOT,
                "[R01 surface %d/%d] %s %s @ %d %.1f %d",
                index + 1,
                count,
                point.kind(),
                point.id(),
                point.x(),
                resolvedY,
                point.z()
        )));
        player.sendSystemMessage(Component.literal(
                point.role()
        ));
    }

    private static List<R01SpatialReviewPlan.ReviewPoint>
            reviewPoints() {
        return R01SpatialReviewPlan.surfacePoints(
                R01SpatialBindingRegistry.data()
        );
    }
}
