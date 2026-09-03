package io.github.q93503128.turnbound.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Canonical v0.4 world-common progress. Player collection/growth stays in a Player Data Attachment;
 * physical world unlocks and one-time world claims live here so future multiplayer does not fork the authored map.
 */
public final class TurnboundWorldSavedData extends SavedData {
    private static final Codec<TurnboundWorldSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("clearedBosses", List.of())
                    .forGetter(data -> List.copyOf(data.clearedBosses)),
            Codec.STRING.listOf().optionalFieldOf("unlockedRegions", List.of("RADIA"))
                    .forGetter(data -> List.copyOf(data.unlockedRegions)),
            Codec.STRING.listOf().optionalFieldOf("claimedWorldRewards", List.of())
                    .forGetter(data -> List.copyOf(data.claimedWorldRewards))
    ).apply(instance, TurnboundWorldSavedData::new));

    public static final SavedDataType<TurnboundWorldSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Turnbound.MOD_ID, "aster_march/world_progress"),
            TurnboundWorldSavedData::new,
            CODEC,
            null);

    private final Set<String> clearedBosses = new LinkedHashSet<>();
    private final Set<String> unlockedRegions = new LinkedHashSet<>();
    private final Set<String> claimedWorldRewards = new LinkedHashSet<>();

    public TurnboundWorldSavedData() {
        unlockedRegions.add("RADIA");
    }

    private TurnboundWorldSavedData(List<String> bosses, List<String> regions, List<String> claims) {
        clearedBosses.addAll(bosses);
        unlockedRegions.addAll(regions);
        unlockedRegions.add("RADIA");
        claimedWorldRewards.addAll(claims);
    }

    public static TurnboundWorldSavedData get(MinecraftServer server) {
        if (server == null) throw new IllegalArgumentException("Missing TURNBOUND server for world SavedData");
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean bossCleared(String bossId) { return clearedBosses.contains(bossId); }
    public boolean regionUnlocked(String regionId) { return unlockedRegions.contains(regionId); }
    public boolean worldRewardClaimed(String claimId) { return claimedWorldRewards.contains(claimId); }
    public Set<String> clearedBosses() { return Set.copyOf(clearedBosses); }
    public Set<String> unlockedRegions() { return Set.copyOf(unlockedRegions); }

    public void recordEncounterClear(String encounterId) {
        if (encounterId == null || !CampaignEncounterCatalog.contains(encounterId)) return;
        var encounter = CampaignEncounterCatalog.spec(encounterId);
        if (!encounter.boss() || encounter.enemies().isEmpty()) return;
        String boss = encounter.enemies().getFirst();
        boolean changed = clearedBosses.add(boss);
        changed |= switch (boss) {
            case "B01" -> unlockedRegions.add("GLOAMWOOD");
            case "B02" -> unlockedRegions.add("BROKEN_AQUEDUCT");
            case "B03" -> unlockedRegions.add("EMBER_QUARRY");
            case "B04" -> unlockedRegions.add("OLD_RELAY_APPROACH");
            case "B05" -> unlockedRegions.add("ENDGAME");
            default -> false;
        };
        if (changed) setDirty();
    }

    /** Idempotent one-time world claim gate for authored chests/landmarks. */
    public boolean claimWorldReward(String claimId) {
        if (claimId == null || claimId.isBlank()) throw new IllegalArgumentException("Missing world reward claim id");
        boolean added = claimedWorldRewards.add(claimId);
        if (added) setDirty();
        return added;
    }

    public void unlockRegion(String regionId) {
        if (regionId == null || regionId.isBlank()) throw new IllegalArgumentException("Missing region id");
        if (unlockedRegions.add(regionId)) setDirty();
    }
}
