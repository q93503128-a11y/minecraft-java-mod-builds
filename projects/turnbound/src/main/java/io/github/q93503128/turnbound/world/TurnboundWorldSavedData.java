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
    public static final String REGION_MEADOW = "SOUTHGATE_MEADOW";
    public static final String REGION_GLOAMWOOD = "GLOAMWOOD";
    public static final String REGION_BROKEN_AQUEDUCT = "BROKEN_AQUEDUCT";
    public static final String REGION_EMBER_QUARRY = "EMBER_QUARRY";
    public static final String REGION_OLD_RELAY_APPROACH = "OLD_RELAY_APPROACH";
    public static final String REGION_ENDGAME = "ENDGAME";

    // Monotonic authored-world gates. These deliberately share the persisted unlock set with regions so old saves
    // remain codec-compatible; once any player earns one of these, another player can never physically re-close it.
    public static final String GATE_SOUTHGATE_DEEP = "GATE_SOUTHGATE_DEEP";
    public static final String GATE_SOUTHGATE_BOSS = "GATE_SOUTHGATE_BOSS";
    public static final String GATE_GLOAM_DEEP = "GATE_GLOAM_DEEP";
    public static final String GATE_GLOAM_BOSS = "GATE_GLOAM_BOSS";
    public static final String GATE_AQUEDUCT_LOWER = "GATE_AQUEDUCT_LOWER";
    public static final String GATE_AQUEDUCT_ORO = "GATE_AQUEDUCT_ORO";
    public static final String GATE_QUARRY_ASH = "GATE_QUARRY_ASH";
    public static final String GATE_QUARRY_BOSS = "GATE_QUARRY_BOSS";
    public static final String GATE_OLD_RELAY_BOSS = "GATE_OLD_RELAY_BOSS";

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
        var quests = snapshot.quests();
        Set<String> clears = snapshot.clearedEncounters();

        if (quests.completed().contains("MQ_P00_03_south_gate")
                || quests.unlockFlags().contains("REGION_MEADOW")) {
            unlockRegion(REGION_MEADOW);
        }

        if (clears.contains("ENC_M01") && clears.contains("ENC_M02")) unlockRegion(GATE_SOUTHGATE_DEEP);
        if (clears.contains("ENC_M04") || clears.contains("BATTLE_B01")) unlockRegion(GATE_SOUTHGATE_BOSS);

        if (quests.completed().contains("MQ_C02_01_spores") || quests.unlockFlags().contains("GLOAM_DEEP_PATH")
                || clears.contains("BATTLE_B02")) unlockRegion(GATE_GLOAM_DEEP);
        if (quests.completed().contains("MQ_C02_02_root_wall") || quests.unlockFlags().contains("B02_GATE")
                || clears.contains("BATTLE_B02")) unlockRegion(GATE_GLOAM_BOSS);

        if (quests.completed().contains("MQ_C03_01_dry_channel") || quests.unlockFlags().contains("AQUEDUCT_LOWER")
                || clears.contains("BATTLE_B03")) unlockRegion(GATE_AQUEDUCT_LOWER);
        if (quests.completed().contains("MQ_C03_02_old_orders") || quests.unlockFlags().contains("ORO_ROOM")
                || clears.contains("BATTLE_B03")) unlockRegion(GATE_AQUEDUCT_ORO);

        if (quests.completed().contains("MQ_C04_01_ash_route") || quests.unlockFlags().contains("FT_QUARRY")
                || clears.contains("BATTLE_B04")) unlockRegion(GATE_QUARRY_ASH);
        if (quests.completed().contains("MQ_C04_02_core_fragment") || quests.unlockFlags().contains("B04_GATE")
                || clears.contains("BATTLE_B04")) unlockRegion(GATE_QUARRY_BOSS);

        if (quests.completed().contains("MQ_C05_02_serak_record") || quests.unlockFlags().contains("B05_GATE")
                || clears.contains("BATTLE_B05")) unlockRegion(GATE_OLD_RELAY_BOSS);

        for (String encounterId : List.of("BATTLE_B01", "BATTLE_B02", "BATTLE_B03", "BATTLE_B04", "BATTLE_B05")) {
            if (clears.contains(encounterId)) recordEncounterClear(encounterId);
        }
        if (quests.completed().contains("MQ_C05_01_relay_key")
                || quests.unlockFlags().contains("OLD_RELAY_ENTRANCE")) {
            unlockRegion(REGION_OLD_RELAY_APPROACH);
        }
        if (quests.completed().contains("MQ_C05_03_reconnect")
                || quests.unlockFlags().contains("ENDGAME")) {
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
