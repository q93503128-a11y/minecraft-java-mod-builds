package dev.moonseungjun.augmentationsurvival.core;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * Adds at most one augmentation-core roll when a natural structure container first generates loot.
 *
 * <p>The loot table alone is not trusted. The loot origin must be inside a currently generated
 * structure piece whose ID resolves through {@link CoreSourceResolver}. This prevents copied loot
 * tables or player-placed containers outside natural structures from becoming progression farms.</p>
 */
public final class StructureCoreRewardService {
    private StructureCoreRewardService() {
    }

    public static void initialize() {
        LootTableEvents.MODIFY_DROPS.register((holder, context, drops) -> holder.unwrapKey().ifPresent(tableKey -> {
            Identifier lootTableId = tableKey.identifier();
            if (!isStructureContainerTable(lootTableId)) {
                return;
            }

            Vec3 origin = context.getOptionalParameter(LootContextParams.ORIGIN);
            if (origin == null) {
                return;
            }
            ServerLevel level = context.getLevel();
            BlockPos blockPos = BlockPos.containing(origin.x, origin.y, origin.z);
            CoreSourceResolver.StructureResolution resolution = resolveNaturalStructure(level, blockPos);
            if (resolution == null || resolution.excludedSource() || !resolution.hasDistribution()) {
                return;
            }

            CoreDistributionCatalog.rollDistributionTemplate(
                    resolution.distributionProfile(),
                    resolution.coreRollMultiplier(),
                    context.getRandom()
            ).ifPresent(tier -> drops.add(new ItemStack(AugmentCoreItems.item(tier))));
        }));
    }

    private static boolean isStructureContainerTable(Identifier lootTableId) {
        String path = lootTableId.getPath();
        return path.startsWith("chests/") || path.startsWith("chest/");
    }

    private static CoreSourceResolver.StructureResolution resolveNaturalStructure(
            ServerLevel level,
            BlockPos blockPos
    ) {
        StructureManager manager = level.structureManager();
        Map<Structure, LongSet> candidates = manager.getAllStructuresAt(blockPos);
        if (candidates.isEmpty()) {
            return null;
        }

        Registry<Structure> structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        CoreSourceResolver.StructureResolution best = null;
        int bestRank = -1;
        double bestMultiplier = -1.0;

        for (Structure structure : candidates.keySet()) {
            StructureStart start = manager.getStructureWithPieceAt(blockPos, structure);
            if (!start.isValid()) {
                continue;
            }
            Identifier structureId = structureRegistry.getKey(structure);
            if (structureId == null) {
                continue;
            }
            CoreSourceResolver.StructureResolution resolution = CoreSourceResolver.resolveStructure(structureId);
            if (resolution.excludedSource()) {
                return resolution;
            }
            if (!resolution.hasDistribution()) {
                continue;
            }

            int rank = profileRank(resolution.distributionProfile());
            if (rank > bestRank || (rank == bestRank && resolution.coreRollMultiplier() > bestMultiplier)) {
                best = resolution;
                bestRank = rank;
                bestMultiplier = resolution.coreRollMultiplier();
            }
        }
        return best;
    }

    private static int profileRank(String profileId) {
        return switch (profileId) {
            case "pinnacle_cache" -> 4;
            case "dangerous_high" -> 3;
            case "dangerous_mid" -> 2;
            case "routine_exploration" -> 1;
            default -> 0;
        };
    }
}
