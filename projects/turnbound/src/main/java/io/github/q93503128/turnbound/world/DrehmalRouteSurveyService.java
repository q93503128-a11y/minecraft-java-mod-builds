package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Read-only helper for the Minecraft 26.2 route survey.
 *
 * <p>This service deliberately does not write route JSON or flip production gates. A human client inspection must
 * still confirm sightlines, authored-world triggers, original NPC overlap and camera feel before coordinates are
 * committed to the production catalog.</p>
 */
public final class DrehmalRouteSurveyService {
    private static final int SITE_HALF_WIDTH = 1;
    private static final int CLIFF_SCAN_RADIUS = 4;
    private static final int CLIFF_DELTA = 3;

    public record Inspection(
            int x,
            int y,
            int z,
            float yaw,
            boolean stableThreeByThree,
            boolean clearHeadroom,
            boolean fluidFree,
            int localHeightSpread,
            boolean cliffRisk,
            boolean soloArenaOpen,
            boolean fourPlayerArenaOpen,
            String nearestAnchor,
            int nearestAnchorDistance
    ) {
        public boolean siteGeometryPass() {
            return stableThreeByThree && clearHeadroom && fluidFree && !cliffRisk;
        }

        public String positionJson() {
            return String.format(Locale.ROOT, "{\"x\":%d,\"y\":%d,\"z\":%d}", x, y, z);
        }

        public String arenaJson() {
            return String.format(Locale.ROOT,
                    "{\"center\":{\"x\":%d,\"y\":%d,\"z\":%d},\"yaw\":%.1f}", x, y, z, yaw);
        }

        public String summary() {
            return "site=" + pass(siteGeometryPass())
                    + " 3x3=" + pass(stableThreeByThree)
                    + " head=" + pass(clearHeadroom)
                    + " fluid=" + pass(fluidFree)
                    + " heightSpread=" + localHeightSpread
                    + " cliff=" + (cliffRisk ? "RISK" : "OK")
                    + " arenaSolo=" + pass(soloArenaOpen)
                    + " arenaCoop4=" + pass(fourPlayerArenaOpen)
                    + " nearest=" + nearestAnchor + "(" + nearestAnchorDistance + "m)";
        }

        private static String pass(boolean value) {
            return value ? "PASS" : "FAIL";
        }
    }

    private DrehmalRouteSurveyService() {}

    public static Inspection inspect(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            throw new IllegalArgumentException("Drehmal route survey requires a server player");
        }

        int x = (int)Math.floor(player.getX());
        int z = (int)Math.floor(player.getZ());
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        float yaw = Math.round(player.getYRot() * 10.0F) / 10.0F;

        boolean stable = true;
        boolean headroom = true;
        boolean dry = true;
        int minHeight = Integer.MAX_VALUE;
        int maxHeight = Integer.MIN_VALUE;
        for (int dx = -SITE_HALF_WIDTH; dx <= SITE_HALF_WIDTH; dx++) {
            for (int dz = -SITE_HALF_WIDTH; dz <= SITE_HALF_WIDTH; dz++) {
                int sx = x + dx;
                int sz = z + dz;
                int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, sx, sz);
                minHeight = Math.min(minHeight, sy);
                maxHeight = Math.max(maxHeight, sy);

                BlockPos feet = new BlockPos(sx, sy, sz);
                BlockPos below = feet.below();
                if (level.getBlockState(below).getCollisionShape(level, below).isEmpty()) stable = false;
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos space = feet.above(dy);
                    if (!level.getBlockState(space).getCollisionShape(level, space).isEmpty()) headroom = false;
                    if (!level.getFluidState(space).isEmpty()) dry = false;
                }
            }
        }
        int heightSpread = maxHeight - minHeight;
        if (heightSpread > 1) stable = false;

        boolean cliff = false;
        for (int dx = -CLIFF_SCAN_RADIUS; dx <= CLIFF_SCAN_RADIUS && !cliff; dx++) {
            for (int dz = -CLIFF_SCAN_RADIUS; dz <= CLIFF_SCAN_RADIUS; dz++) {
                int nearbyY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                if (Math.abs(nearbyY - y) > CLIFF_DELTA) {
                    cliff = true;
                    break;
                }
            }
        }

        Vec3 center = new Vec3(x + 0.5D, y, z + 0.5D);
        boolean soloArena = BattleSessionManager.surveyArenaOpen(player, center, yaw, 1);
        boolean coopArena = BattleSessionManager.surveyArenaOpen(player, center, yaw, 4);

        DrehmalWorldProfile.Anchor nearest = DrehmalWorldProfile.profile().anchors().stream()
                .filter(DrehmalWorldProfile.Anchor::enabled)
                .min(Comparator.comparingDouble(anchor -> horizontalDistanceSq(x, z, anchor)))
                .orElse(null);
        String nearestLocator = nearest == null ? "none" : nearest.locator();
        int nearestDistance = nearest == null ? -1 : (int)Math.round(Math.sqrt(horizontalDistanceSq(x, z, nearest)));

        return new Inspection(x, y, z, yaw, stable, headroom, dry, heightSpread, cliff,
                soloArena, coopArena, nearestLocator, nearestDistance);
    }

    public static List<String> routeSeedLines() {
        List<String> lines = new ArrayList<>();
        for (DrehmalFirstRouteCatalog.Site site : DrehmalFirstRouteCatalog.route().sites()) {
            DrehmalWorldProfile.Anchor anchor = DrehmalWorldProfile.enabled(site.surveySeedAnchor());
            String coordinate = anchor == null ? "missing" : anchor.x() + " " + anchor.y() + " " + anchor.z();
            String state = site.productionEnabled() ? "PRODUCTION" : site.verifiedIn26_2() ? "VERIFIED" : "PENDING";
            lines.add(site.playerLabel() + " · " + state + " · seed " + coordinate
                    + " · " + site.surveySeedAnchor());
        }
        return List.copyOf(lines);
    }

    private static double horizontalDistanceSq(int x, int z, DrehmalWorldProfile.Anchor anchor) {
        double dx = x + 0.5D - (anchor.x() + 0.5D);
        double dz = z + 0.5D - (anchor.z() + 0.5D);
        return dx * dx + dz * dz;
    }
}
