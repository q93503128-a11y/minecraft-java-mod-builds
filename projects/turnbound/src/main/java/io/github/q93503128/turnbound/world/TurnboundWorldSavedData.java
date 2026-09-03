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
import java.util.UUID;

/**
 * Canonical v0.4 world-common progress. Player collection/growth stays in a Player Data Attachment;
 * physical world unlocks and one-time world claims live here so future multiplayer does not fork the authored map.
 */
public final class TurnboundWorldSavedData extends SavedData {
    public static final String REGION_RADIA = "RADIA";
    public static final String REGION_GLOAMWOOD = "GLOAMWOOD";
    public static final String REGION_BROKEN_AQUEDUCT = "BROKEN_AQUEDUCT";
    public static final String REGION_EMBER_QUARRY = "EMBER_QUARRY";
    public static final String REGION_OLD_RELAY_APPROACH = "OLD_RELAY_APPROACH";
    public static final String REGION_ENDGAME = "ENDGAME";

    private static final Codec<TurnboundWorldSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("clearedBosses", List.of())
                    .forGetter(data -> List.copyOf(data.clearedBosses)),
            Codec.STRING.listOf().optionalFieldOf("unlockedRegions", List.of(REGION_RADIA))
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
        unlockedRegions.add(REGION_RADIA);
    }

    private TurnboundWorldSavedData(List<String> bosses, List<String> regions, List<String> claims) {
        clearedBosses.addAll(bosses);
        unlockedRegions.addAll(regions);
        unlockedRegions.add(REGION_RADIA);
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
            case "B01" -> unlockedRegions.add(REGION_GLOAMWOOD);
            case "B02" -> unlockedRegions.add(REGION_BROKEN_AQUEDUCT);
            case "B03" -> unlockedRegions.add(REGION_EMBER_QUARRY);
            // B04 only enables the Chapter 5 relay-fragment phase. The physical east road opens after MQ_C05_01.
            // B05 alone also does not open endgame; MQ_C05_03 / ENDGAME is the canonical boundary.
            default -> false;
        };
        if (changed) setDirty();
    }

    /**
     * Backfills shared world state from an already-existing player profile and promotes world-level quest gates.
     * This is migration/reconciliation, not a second progression authority.
     */
    public void reconcilePlayerProgress(UUID playerId) {
        if (playerId == null || !CampaignProgressStore.hasRuntime(playerId)) return;
        var snapshot = CampaignProgressStore.snapshot(playerId);
        for (String encounterId : List.of("BATTLE_B01", "BATTLE_B02", "BATTLE_B03", "BATTLE_B04", "BATTLE_B05")) {
            if (snapshot.clearedEncounters().contains(encounterId)) recordEncounterClear(encounterId);
        }
        if (snapshot.quests().completed().contains("MQ_C05_01_relay_key")
                || snapshot.quests().unlockFlags().contains("OLD_RELAY_ENTRANCE")) {
            unlockRegion(REGION_OLD_RELAY_APPROACH);
        }
        if (snapshot.quests().completed().contains("MQ_C05_03_reconnect")
                || snapshot.quests().unlockFlags().contains("ENDGAME")) {
            unlockRegion(REGION_ENDGAME);
        }
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
