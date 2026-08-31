package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Bridges the Chapter 5 three-fragment submission objective to already-authored story clears.
 * MEADOW=B01 and QUARRY=B04 are directly supported by the story text. The exact AQUEDUCT acquisition
 * presentation is a Canon Gap, so B03/ORO-7 recovery is isolated here as an implementation fallback.
 */
public final class RelayFragmentBridgeService {
    public static final String MEADOW = "RELAY_FRAGMENT_MEADOW";
    public static final String AQUEDUCT = "RELAY_FRAGMENT_AQUEDUCT";
    public static final String QUARRY = "RELAY_FRAGMENT_QUARRY";

    private RelayFragmentBridgeService() {}

    public static Set<String> available(ServerPlayer player) { return available(player.getUUID()); }

    static Set<String> available(java.util.UUID playerId) {
        CampaignProgressStore.Snapshot snapshot = CampaignProgressStore.snapshot(playerId);
        Set<String> flags = new LinkedHashSet<>();
        if (snapshot.clearedEncounters().contains("BATTLE_B01") || snapshot.quests().completed().contains("MQ_C01_03_graul")) flags.add(MEADOW);
        // IMPLEMENTATION_FALLBACK: exact Aqueduct fragment acquisition presentation is not specified by v0.4 canon.
        if (snapshot.clearedEncounters().contains("BATTLE_B03") || snapshot.quests().completed().contains("MQ_C03_03_oro7")) flags.add(AQUEDUCT);
        if (snapshot.clearedEncounters().contains("BATTLE_B04") || snapshot.quests().completed().contains("MQ_C04_03_kolvak")) flags.add(QUARRY);
        return Set.copyOf(flags);
    }

    public static int submitAvailable(ServerPlayer player) {
        return submitAvailable(player.getUUID());
    }

    static int submitAvailable(java.util.UUID playerId) {
        if (!CampaignProgressStore.quests(playerId).completed().contains("MQ_C04_03_kolvak")) return 0;
        int before = CampaignProgressStore.quests(playerId).marks().getOrDefault("MQ_C05_01_relay_key", Set.of()).size();
        for (String flag : available(playerId)) CampaignProgressStore.inventoryFlag(playerId, flag);
        int after = CampaignProgressStore.quests(playerId).completed().contains("MQ_C05_01_relay_key")
                ? 3 : CampaignProgressStore.quests(playerId).marks().getOrDefault("MQ_C05_01_relay_key", Set.of()).size();
        return Math.max(0, after - before);
    }
}
