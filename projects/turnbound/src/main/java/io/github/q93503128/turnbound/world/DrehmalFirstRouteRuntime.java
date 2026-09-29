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
        for (DrehmalFirstRouteCatalog.Site site : DrehmalAdaptiveRoutePlacement.productionSites(player)) {
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
        return player != null && DrehmalRouteZoneRules.insideSafetyZone(
                DrehmalAdaptiveRoutePlacement.productionSites(player), player.getX(), player.getZ());
    }

    static boolean insideSafetyZone(double x, double z) {
        return DrehmalRouteZoneRules.insideSafetyZone(DrehmalFirstRouteCatalog.productionSites(), x, z);
    }

    static boolean insideSafetyZone(net.minecraft.server.level.ServerLevel level, double x, double z) {
        return DrehmalRouteZoneRules.insideSafetyZone(DrehmalAdaptiveRoutePlacement.productionSites(level), x, z);
    }

    public static DrehmalFirstRouteCatalog.EncounterSlot encounterAt(ServerPlayer player) {
        if (player == null) return null;
        for (DrehmalFirstRouteCatalog.EncounterSlot encounter : DrehmalAdaptiveRoutePlacement.productionEncounters(player)) {
            DrehmalFirstRouteCatalog.Site site = DrehmalAdaptiveRoutePlacement.site(player, encounter.siteLocator());
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
                DrehmalFastTravelService.travels(player),
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

    static void recordProgress(ServerPlayer player) {
        if (player == null) return;
        var server = player.level().getServer();
        if (server == null) return;
        ExternalWorldSavedData data = ExternalWorldSavedData.get(server);
        DrehmalFirstRouteProgress.record(data, player.getUUID(), locationSite(player));
        DrehmalFastTravelService.recordDiscovery(player);
        if (insideHubCoordinates(player.getX(), player.getZ())) {
            data.markOnboardingFlag(player.getUUID(), DrehmalFirstRouteProgress.HUB_REACHED);
        }
    }

    static boolean insideHub(ServerPlayer player) {
        if (player == null) return false;
        DrehmalFirstRouteCatalog.Site location = locationSite(player);
        return (location != null && "HUB_SAFE".equals(location.kind()))
                || insideHubCoordinates(player.getX(), player.getZ());
    }

    static boolean insideHubCoordinates(double x, double z) {
        var hub = DrehmalWorldProfile.enabled(DrehmalWorldProfile.HUB_LOCATOR);
        if (hub == null) return false;
        double dx = x - (hub.x() + 0.5D);
        double dz = z - (hub.z() + 0.5D);
        return dx * dx + dz * dz <= 64.0D * 64.0D;
    }

    private static FieldUiSnapshot.Navigation navigation(ServerPlayer player) {
        if (player == null) return FieldUiSnapshot.Navigation.none();
        var server = player.level().getServer();
        var flags = server == null
                ? java.util.Set.<String>of()
                : ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        var clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        boolean hubReached = DrehmalFirstRouteProgress.reached(flags, DrehmalFirstRouteProgress.HUB_REACHED);
        boolean inHub = insideHub(player);

        if (hubReached) {
            if (DrabyelOpeningTutorial.shouldSendOut(flags, clears)) {
                return openingPatrolNavigation(player);
            }
            if (!inHub && (!DrabyelOpeningTutorial.introReady(flags)
                    || DrabyelOpeningTutorial.shouldReturnToHub(false, clears))) {
                var hub = DrehmalWorldProfile.enabled(DrehmalWorldProfile.HUB_LOCATOR);
                if (hub != null) {
                    return new FieldUiSnapshot.Navigation(
                            hub.locator(), "뉴 드라비엘", hub.x() + 0.5D, hub.z() + 0.5D);
                }
            }
            return FieldUiSnapshot.Navigation.none();
        }

        var hub = DrehmalWorldProfile.enabled(DrehmalWorldProfile.HUB_LOCATOR);
        if (hub != null) {
            return new FieldUiSnapshot.Navigation(
                    hub.locator(), "뉴 드라비엘", hub.x() + 0.5D, hub.z() + 0.5D);
        }
        return DrehmalRouteNavigationRules.target(
                DrehmalAdaptiveRoutePlacement.productionSites(player), player.getX(), player.getZ(), flags, clears);
    }

    private static FieldUiSnapshot.Navigation openingPatrolNavigation(ServerPlayer player) {
        var patrol=DrehmalAdaptiveRoutePlacement.site(player,DrabyelOpeningTutorial.ENCOUNTER_SITE);
        if(patrol!=null&&patrol.runtimePosition()!=null){
            return navigationTo(patrol.locator(),"북쪽 길 순찰대",patrol.runtimePosition());
        }
        var placement=DrehmalMapPlacementCatalog.placement(DrabyelOpeningTutorial.ENCOUNTER_SITE);
        if(placement!=null&&!placement.siteSeeds().isEmpty()){
            var seed=placement.siteSeeds().getFirst();
            return new FieldUiSnapshot.Navigation(
                    DrabyelOpeningTutorial.ENCOUNTER_SITE,"북쪽 길 순찰대",seed.x()+0.5D,seed.z()+0.5D);
        }
        return FieldUiSnapshot.Navigation.none();
    }

    private static FieldUiSnapshot.Navigation navigationTo(
            String id,
            String label,
            DrehmalFirstRouteCatalog.Position position
    ) {
        return new FieldUiSnapshot.Navigation(id, label, position.x() + 0.5D, position.z() + 0.5D);
    }

    private static DrehmalFirstRouteCatalog.Site locationSite(ServerPlayer player) {
        if (player == null) return null;
        return DrehmalLocationBannerRules.current(
                DrehmalAdaptiveRoutePlacement.productionSites(player), player.getX(), player.getZ());
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
                DrabyelHubServiceRuntime.availableRoles(player));
    }
}
