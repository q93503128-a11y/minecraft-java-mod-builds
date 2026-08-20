package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.world.level.block.DoorBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Source-only accounting audit for Erden's retained urban interior variants.
 *
 * <p>This deliberately does not treat the aggregate planned-room counter as a quality target. The
 * counter includes connectivity regions detected in imported source floors, so adding a legitimate
 * facade crop can change it without adding or deleting any physical room. Instead this audit proves
 * that every one of the 233 placements maps to a real retained fragment, entrance, source-floor or
 * source-air-authored upper topology and full-interior plan, then exposes the weighted contribution
 * of every fragment variant. A multi-crop source also receives a counterfactual against its first
 * retained crop so a room-count delta can be explained rather than hidden or padded. Variants from
 * one source must retain at least half of the best variant's usable upper-interior capacity and may
 * lose at most one planned connectivity region, preventing visual diversity from hollowing out the
 * playable interior.</p>
 */
public final class ErdenUrbanInteriorTopologyAudit {
    public static final int AUDIT_REVISION = 3;
    private static final int EXPECTED_BUILDINGS = 233;

    private static boolean bootstrapped;

    private ErdenUrbanInteriorTopologyAudit() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;

        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        Map<String, ErdenUrbanFullInteriorPlanCatalog.InteriorPlan> plans =
                ErdenUrbanFullInteriorPlanCatalog.plans();
        Map<String, ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile> opportunities =
                ErdenUrbanUpperRoomOpportunityCatalog.profiles();
        Map<String, List<ErdenUrbanFullInteriorRouteCatalog.LevelRoutePlan>> routes =
                ErdenUrbanFullInteriorRouteCatalog.plans();

        Map<String, Integer> placementCounts = new LinkedHashMap<>();
        int buildings = 0;
        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            if (!snapshots.containsKey(placement.fragmentKey())) {
                throw new IllegalStateException("Erden placement references missing retained fragment "
                        + placement.fragmentKey());
            }
            placementCounts.merge(placement.fragmentKey(), 1, Integer::sum);
            buildings++;
        }
        if (buildings != EXPECTED_BUILDINGS || buildings != ExternalUrbanFabricBuilder.plotCount()) {
            throw new IllegalStateException("Erden interior topology placement count drifted: " + buildings);
        }
        if (snapshots.size() != ExternalUrbanFabricBuilder.facadeStyleCount()) {
            throw new IllegalStateException("Erden retained fragment/style accounting drifted fragments="
                    + snapshots.size() + " styles=" + ExternalUrbanFabricBuilder.facadeStyleCount());
        }

        List<String> fragmentKeys = snapshots.keySet().stream().sorted().toList();
        Map<String, List<FragmentMetrics>> byResource = new LinkedHashMap<>();
        int weightedPlannedRooms = 0;
        int weightedExistingRegions = 0;
        int weightedExistingCells = 0;
        int weightedAuthoredRegions = 0;
        int weightedAuthoredCells = 0;

        for (String fragmentKey : fragmentKeys) {
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot = snapshots.get(fragmentKey);
            ErdenUrbanFullInteriorPlanCatalog.InteriorPlan plan = plans.get(fragmentKey);
            ErdenUrbanUpperRoomOpportunityCatalog.OpportunityProfile opportunity =
                    opportunities.get(fragmentKey);
            List<ErdenUrbanFullInteriorRouteCatalog.LevelRoutePlan> fragmentRoutes = routes.get(fragmentKey);
            if (plan == null || opportunity == null || fragmentRoutes == null) {
                throw new IllegalStateException("Missing Erden interior topology input fragment=" + fragmentKey
                        + " plan=" + (plan != null) + " opportunity=" + (opportunity != null)
                        + " routes=" + (fragmentRoutes != null));
            }

            int placements = placementCounts.getOrDefault(fragmentKey, 0);
            if (placements <= 0) {
                throw new IllegalStateException("Retained Erden facade style is never placed: " + fragmentKey);
            }

            int entranceDoorY = retainedEntranceDoorY(snapshot);
            if (entranceDoorY == Integer.MIN_VALUE) {
                throw new IllegalStateException("Retained Erden fragment has no real entrance door at "
                        + snapshot.entranceX() + "," + snapshot.entranceZ() + " fragment=" + fragmentKey);
            }
            if (opportunity.groundFeetY() == Integer.MIN_VALUE
                    || plan.groundFeetY() != opportunity.groundFeetY()) {
                throw new IllegalStateException("Erden interior ground reference drifted fragment=" + fragmentKey
                        + " plan=" + plan.groundFeetY() + " opportunity=" + opportunity.groundFeetY());
            }

            int existingRegions = plan.existingLevels().stream()
                    .mapToInt(level -> level.regions().size()).sum();
            int existingCells = plan.existingLevels().stream()
                    .mapToInt(ErdenUrbanFullInteriorPlanCatalog.PlannedLevel::usableCells).sum();
            int authoredRegions = plan.selectedAuthoredLevels().stream()
                    .mapToInt(level -> level.regions().size()).sum();
            int authoredCells = plan.selectedAuthoredLevels().stream()
                    .mapToInt(ErdenUrbanFullInteriorPlanCatalog.PlannedLevel::usableCells).sum();
            int accountedRooms = existingRegions + authoredRegions;
            if (plan.plannedRooms() != accountedRooms) {
                throw new IllegalStateException("Erden planned-room accounting drifted fragment=" + fragmentKey
                        + " catalog=" + plan.plannedRooms() + " regions=" + accountedRooms);
            }
            if (accountedRooms <= 0 || existingCells + authoredCells <= 0) {
                throw new IllegalStateException("Erden retained facade has no usable upper interior fragment="
                        + fragmentKey + " existing_cells=" + existingCells
                        + " authored_cells=" + authoredCells + " regions=" + accountedRooms);
            }
            for (ErdenUrbanFullInteriorPlanCatalog.PlannedLevel level : plan.existingLevels()) {
                verifyUpperLevel(fragmentKey, plan.groundFeetY(), level);
            }
            for (ErdenUrbanFullInteriorPlanCatalog.PlannedLevel level : plan.selectedAuthoredLevels()) {
                verifyUpperLevel(fragmentKey, plan.groundFeetY(), level);
            }

            int routeRooms = fragmentRoutes.stream()
                    .mapToInt(level -> level.regionRoutes().size()).sum();
            int routeNodes = fragmentRoutes.stream()
                    .flatMap(level -> level.regionRoutes().stream())
                    .mapToInt(route -> route.path().size()).sum();
            List<String> existingLevelTopology = plan.existingLevels().stream()
                    .map(level -> level.feetY() + ":" + level.usableCells() + "/" + level.regions().size())
                    .toList();
            List<String> authoredLevelTopology = plan.selectedAuthoredLevels().stream()
                    .map(level -> level.feetY() + ":" + level.usableCells() + "/" + level.regions().size())
                    .toList();

            FragmentMetrics metrics = new FragmentMetrics(
                    fragmentKey, snapshot.resource(), placements, snapshot.blocks().size(),
                    snapshot.width(), snapshot.height(), snapshot.length(),
                    entranceDoorY, plan.groundFeetY(), existingCells, existingRegions,
                    authoredCells, authoredRegions, plan.plannedRooms(),
                    fragmentRoutes.size(), routeRooms, routeNodes,
                    existingLevelTopology, authoredLevelTopology);
            byResource.computeIfAbsent(snapshot.resource(), ignored -> new ArrayList<>()).add(metrics);

            weightedPlannedRooms += placements * plan.plannedRooms();
            weightedExistingRegions += placements * existingRegions;
            weightedExistingCells += placements * existingCells;
            weightedAuthoredRegions += placements * authoredRegions;
            weightedAuthoredCells += placements * authoredCells;

            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_INTERIOR_TOPOLOGY_FRAGMENT fragment={} resource={} placements={} retained={}x{}x{} source_blocks={} entrance_door_y={} ground_y={} ground_delta={} existing_levels={} existing_cells={} existing_regions={} authored_levels={} authored_cells={} authored_regions={} planned_rooms={} extension_route_levels={} extension_route_rooms={} extension_route_nodes={} source_only=true world_reads=false mutations=0 source_blocks_cut=0",
                    fragmentKey, snapshot.resource(), placements,
                    snapshot.width(), snapshot.height(), snapshot.length(), snapshot.blocks().size(),
                    entranceDoorY, plan.groundFeetY(), plan.groundFeetY() - entranceDoorY,
                    existingLevelTopology, existingCells, existingRegions,
                    authoredLevelTopology, authoredCells, authoredRegions, plan.plannedRooms(),
                    fragmentRoutes.size(), routeRooms, routeNodes);
        }

        for (Map.Entry<String, List<FragmentMetrics>> entry : byResource.entrySet()) {
            List<FragmentMetrics> variants = entry.getValue().stream()
                    .sorted(Comparator.comparing(FragmentMetrics::fragmentKey)).toList();
            if (variants.size() < 2) continue;
            FragmentMetrics baseline = variants.getFirst();
            int totalPlacements = variants.stream().mapToInt(FragmentMetrics::placements).sum();
            int actualWeightedRooms = variants.stream()
                    .mapToInt(metric -> metric.placements() * metric.plannedRooms()).sum();
            int firstVariantCounterfactual = totalPlacements * baseline.plannedRooms();
            int maxUpperCells = variants.stream().mapToInt(FragmentMetrics::upperCells).max().orElseThrow();
            int minUpperCells = variants.stream().mapToInt(FragmentMetrics::upperCells).min().orElseThrow();
            int maxPlannedRooms = variants.stream().mapToInt(FragmentMetrics::plannedRooms).max().orElseThrow();
            int minPlannedRooms = variants.stream().mapToInt(FragmentMetrics::plannedRooms).min().orElseThrow();
            for (FragmentMetrics variant : variants) {
                if ((long) variant.upperCells() * 2L < maxUpperCells) {
                    throw new IllegalStateException("Erden facade variant lost the majority of usable upper interior"
                            + " resource=" + entry.getKey() + " fragment=" + variant.fragmentKey()
                            + " cells=" + variant.upperCells() + " best=" + maxUpperCells);
                }
                if (variant.plannedRooms() < maxPlannedRooms - 1) {
                    throw new IllegalStateException("Erden facade variant connectivity collapsed resource="
                            + entry.getKey() + " fragment=" + variant.fragmentKey()
                            + " regions=" + variant.plannedRooms() + " best=" + maxPlannedRooms);
                }
            }
            int minUpperCapacityPercent = maxUpperCells <= 0
                    ? 100 : (int) ((long) minUpperCells * 100L / maxUpperCells);
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_INTERIOR_TOPOLOGY_VARIANT_QUALITY resource={} variants={} max_upper_cells={} min_upper_cells={} min_upper_capacity_percent={} max_planned_regions={} min_planned_regions={} majority_capacity_retained=true region_variance_bounded=true source_only=true world_reads=false mutations=0 source_blocks_cut=0",
                    entry.getKey(), variants.size(), maxUpperCells, minUpperCells,
                    minUpperCapacityPercent, maxPlannedRooms, minPlannedRooms);
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_INTERIOR_TOPOLOGY_VARIANTS resource={} variants={} total_placements={} placement_counts={} entrance_ground={} existing_levels={} existing_cells={} existing_regions={} planned_rooms_each={} first_variant_counterfactual_rooms={} actual_weighted_rooms={} room_delta={} source_only=true world_reads=false mutations=0 source_blocks_cut=0",
                    entry.getKey(), variants.size(), totalPlacements,
                    variants.stream().map(metric -> metric.fragmentKey() + ":" + metric.placements()).toList(),
                    variants.stream().map(metric -> metric.fragmentKey() + ":" + metric.entranceDoorY()
                            + "/" + metric.groundFeetY()).toList(),
                    variants.stream().map(metric -> metric.fragmentKey() + ":" + metric.existingLevels()).toList(),
                    variants.stream().map(metric -> metric.fragmentKey() + ":" + metric.existingCells()).toList(),
                    variants.stream().map(metric -> metric.fragmentKey() + ":" + metric.existingRegions()).toList(),
                    variants.stream().map(metric -> metric.fragmentKey() + ":" + metric.plannedRooms()).toList(),
                    firstVariantCounterfactual, actualWeightedRooms,
                    actualWeightedRooms - firstVariantCounterfactual);
        }

        if (weightedPlannedRooms != weightedExistingRegions + weightedAuthoredRegions) {
            throw new IllegalStateException("Erden weighted interior region accounting drifted planned="
                    + weightedPlannedRooms + " existing=" + weightedExistingRegions
                    + " authored=" + weightedAuthoredRegions);
        }

        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_INTERIOR_TOPOLOGY_PASS revision={} fragments={} buildings={} facade_styles={} weighted_planned_regions={} weighted_existing_regions={} weighted_existing_cells={} weighted_authored_regions={} weighted_authored_cells={} every_style_placed=true region_accounting=true real_entrances=true ground_references=true source_or_authored_upper=true variant_quality_guard=true source_only=true world_reads=false mutations=0 source_blocks_cut=0",
                AUDIT_REVISION, snapshots.size(), buildings, ExternalUrbanFabricBuilder.facadeStyleCount(),
                weightedPlannedRooms, weightedExistingRegions, weightedExistingCells,
                weightedAuthoredRegions, weightedAuthoredCells);
    }

    private static void verifyUpperLevel(
            String fragmentKey,
            int groundFeetY,
            ErdenUrbanFullInteriorPlanCatalog.PlannedLevel level) {
        if (level.feetY() <= groundFeetY || level.usableCells() <= 0 || level.regions().isEmpty()) {
            throw new IllegalStateException("Erden upper interior level is invalid fragment=" + fragmentKey
                    + " ground=" + groundFeetY + " floor=" + level.feetY()
                    + " cells=" + level.usableCells() + " regions=" + level.regions().size());
        }
    }

    private static int retainedEntranceDoorY(ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot) {
        int doorY = Integer.MAX_VALUE;
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            if (block.x() == snapshot.entranceX()
                    && block.z() == snapshot.entranceZ()
                    && block.state().getBlock() instanceof DoorBlock) {
                doorY = Math.min(doorY, block.y());
            }
        }
        return doorY == Integer.MAX_VALUE ? Integer.MIN_VALUE : doorY;
    }

    private record FragmentMetrics(
            String fragmentKey,
            String resource,
            int placements,
            int sourceBlocks,
            int width,
            int height,
            int length,
            int entranceDoorY,
            int groundFeetY,
            int existingCells,
            int existingRegions,
            int authoredCells,
            int authoredRegions,
            int plannedRooms,
            int routeLevels,
            int routeRooms,
            int routeNodes,
            List<String> existingLevels,
            List<String> authoredLevels) {
        int upperCells() {
            return existingCells + authoredCells;
        }
    }
}
