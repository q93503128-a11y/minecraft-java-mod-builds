package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Runtime projection of the first Drehmal route.
 *
 * <p>Only entries explicitly promoted after a Minecraft 26.2 survey can drive proximity gameplay. Until then this
 * service provides authored exploration copy without teleporting, spawning or modifying the external world.</p>
 */
public final class DrehmalFirstRouteRuntime {
    private DrehmalFirstRouteRuntime() {}

    public static DrehmalFirstRouteCatalog.Site nearestProductionSite(ServerPlayer player) {
        if (player == null) return null;
        DrehmalFirstRouteCatalog.Site nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (DrehmalFirstRouteCatalog.Site site : DrehmalFirstRouteCatalog.productionSites()) {
            DrehmalFirstRouteCatalog.Position position = site.runtimePosition();
            if (position == null) continue;
            double distance = player.distanceToSqr(
                    position.x() + 0.5D,
                    position.y() + 0.5D,
                    position.z() + 0.5D);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = site;
            }
        }
        return nearest;
    }

    public static boolean insideSafetyZone(ServerPlayer player) {
        return player != null && insideSafetyZone(player.getX(), player.getZ());
    }

    static boolean insideSafetyZone(double x, double z) {
        return DrehmalRouteZoneRules.insideSafetyZone(DrehmalFirstRouteCatalog.productionSites(), x, z);
    }

    public static DrehmalFirstRouteCatalog.EncounterSlot encounterAt(ServerPlayer player) {
        if (player == null) return null;
        for (DrehmalFirstRouteCatalog.EncounterSlot encounter : DrehmalFirstRouteCatalog.productionEncounters()) {
            DrehmalFirstRouteCatalog.Site site = DrehmalFirstRouteCatalog.site(encounter.siteLocator());
            if (site == null || site.runtimePosition() == null || site.encounterRadius() <= 0) continue;
            double radius = site.encounterRadius();
            var position = site.runtimePosition();
            if (player.distanceToSqr(position.x() + 0.5D, position.y() + 0.5D, position.z() + 0.5D)
                    <= radius * radius) {
                return encounter;
            }
        }
        return null;
    }

    public static FieldUiSnapshot explorationSnapshot(ServerPlayer player) {
        DrehmalFirstRouteCatalog.Site location = locationSite(player);
        DrehmalContextualOnboarding.Guidance guidance = guidance(player, location);
        DrabyelInteractionPromptRules.Prompt interaction = DrabyelHubServiceRuntime.prompt(player);
        FieldUiSnapshot.Navigation navigation = navigation(player);
        return new FieldUiSnapshot(
                true,
                FieldUiSnapshot.Mode.NONE,
                0,
                0,
                false,
                false,
                0,
                0,
                guidance.objective(),
                guidance.hint(),
                FieldUiSnapshot.Reward.none(),
                List.of(),
                List.of(),
                "",
                0,
                location == null ? "" : location.locator(),
                location == null ? "" : location.playerLabel(),
                interaction.id(),
                interaction.label(),
                interaction.action(),
                navigation);
    }

    static String locationId(ServerPlayer player) {
        DrehmalFirstRouteCatalog.Site location = locationSite(player);
        return location == null ? "" : location.locator();
    }

    static String interactionId(ServerPlayer player) {
        return DrabyelHubServiceRuntime.prompt(player).id();
    }

    static String navigationId(ServerPlayer player) {
        return navigation(player).id();
    }

    static boolean insideHub(ServerPlayer player) {
        DrehmalFirstRouteCatalog.Site location = locationSite(player);
        return location != null && "HUB_SAFE".equals(location.kind());
    }

    private static FieldUiSnapshot.Navigation navigation(ServerPlayer player) {
        if (player == null) return FieldUiSnapshot.Navigation.none();
        return DrehmalRouteNavigationRules.target(
                DrehmalFirstRouteCatalog.productionSites(), player.getX(), player.getZ());
    }

    private static DrehmalFirstRouteCatalog.Site locationSite(ServerPlayer player) {
        if (player == null) return null;
        return DrehmalLocationBannerRules.current(
                DrehmalFirstRouteCatalog.productionSites(), player.getX(), player.getZ());
    }

    private static DrehmalContextualOnboarding.Guidance guidance(
            ServerPlayer player,
            DrehmalFirstRouteCatalog.Site location
    ) {
        if (player == null) {
            return DrehmalContextualOnboarding.resolve("", java.util.Set.of(), java.util.Set.of(), java.util.Set.of());
        }
        var server = player.level().getServer();
        var flags = server == null
                ? java.util.Set.<String>of()
                : ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        return DrehmalContextualOnboarding.resolve(
                location == null ? "" : location.kind(),
                CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters(),
                flags,
                DrabyelHubServiceRuntime.availableRoles());
    }
}
