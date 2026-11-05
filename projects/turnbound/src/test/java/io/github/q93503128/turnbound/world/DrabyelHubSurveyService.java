package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Read-only New Drabyel service placement helper.
 *
 * <p>The command intentionally leaves verification/production flags false in its JSON fragment. Geometry can be
 * measured automatically, but town readability, doorway obstruction, original-NPC overlap and visual fit still need
 * human client inspection before a service is promoted.</p>
 */
public final class DrabyelHubSurveyService {
    private static final double HUB_SURVEY_RADIUS = 192.0D;

    public record Candidate(
            DrabyelHubServiceCatalog.Service service,
            DrehmalRouteSurveyService.Inspection inspection,
            int hubDistance
    ) {
        public Candidate {
            if (service == null) throw new IllegalArgumentException("Missing Drabyel service");
            if (inspection == null) throw new IllegalArgumentException("Missing survey inspection");
        }

        public boolean candidateGeometryPass() {
            return inspection.siteGeometryPass() && hubDistance >= 0 && hubDistance <= HUB_SURVEY_RADIUS;
        }

        public String summary() {
            return service.role() + " · " + service.playerLabel()
                    + " · zone=" + service.zone()
                    + " · geometry=" + (candidateGeometryPass() ? "PASS" : "FAIL")
                    + " · hubDistance=" + hubDistance + "m"
                    + " · visual=" + service.visualAsset();
        }

        public String catalogPatchJson() {
            return String.format(Locale.ROOT,
                    "\"position\":%s,\"yaw\":%.1f,\"verifiedIn26_2\":false,\"productionEnabled\":false",
                    inspection.positionJson(), inspection.yaw());
        }
    }

    private DrabyelHubSurveyService() {}

    public static Candidate inspect(ServerPlayer player, String role) {
        if (player == null) throw new IllegalArgumentException("New Drabyel service survey requires a player");
        var server = player.level().getServer();
        if (server == null || !DrehmalWorldBinding.isBound(server)) {
            throw new IllegalStateException("Bind the verified Drehmal world before surveying New Drabyel services");
        }

        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        DrabyelHubServiceCatalog.Service service = DrabyelHubServiceCatalog.hub().services().stream()
                .filter(candidate -> candidate.role().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown New Drabyel service role " + normalized));

        DrehmalRouteSurveyService.Inspection inspection = DrehmalRouteSurveyService.inspect(player);
        BlockPos hub = DrehmalWorldBinding.hubSeed();
        double dx = inspection.x() + 0.5D - (hub.getX() + 0.5D);
        double dz = inspection.z() + 0.5D - (hub.getZ() + 0.5D);
        int distance = (int)Math.round(Math.sqrt(dx * dx + dz * dz));
        return new Candidate(service, inspection, distance);
    }
}
