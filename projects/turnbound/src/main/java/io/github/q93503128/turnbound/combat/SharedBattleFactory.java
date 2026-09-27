package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.world.CampaignProgressStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Builds a multi-owner PvE battle roster without assigning input/reward ownership to the wrong player. */
public final class SharedBattleFactory {
    public record Blueprint(BattleState state, List<UUID> participants, Map<String,UUID> actorOwners, int enemyHpMultiplier) {
        public Blueprint {
            participants = List.copyOf(participants);
            actorOwners = Map.copyOf(actorOwners);
        }
        public UUID ownerOf(String actorInstanceId) { return actorOwners.get(actorInstanceId); }
    }

    private SharedBattleFactory() {}

    public static Blueprint create(List<UUID> playerIds, String encounterId) {
        if (playerIds == null || playerIds.isEmpty()) throw new IllegalArgumentException("Missing shared-battle players");
        Set<UUID> unique = new LinkedHashSet<>(playerIds);
        if (unique.size() != playerIds.size()) throw new IllegalArgumentException("Duplicate shared-battle player");
        int playerCount = unique.size();
        SharedBattleRules.validatePlayerCount(playerCount);

        V04Catalogs.Encounter encounter = CampaignEncounterCatalog.spec(encounterId);
        ArrayList<CombatantState> units = new ArrayList<>();
        Map<String,UUID> actorOwners = new LinkedHashMap<>();
        int initiative = 0;
        int group = 0;

        for (UUID playerId : unique) {
            List<String> party = CampaignProgressStore.activeParty(playerId);
            if (party.isEmpty() || party.size() > 4) throw new IllegalStateException("Shared participant requires 1-4 active characters");
            for (int slot = 0; slot < party.size(); slot++) {
                String characterId = party.get(slot);
                String instanceId = "p" + (group + 1) + "_ally_" + characterId.toLowerCase();
                CombatantState ally = new CombatantState(
                        instanceId,
                        CampaignEncounterCatalog.campaignDefinition(playerId, characterId),
                        CombatantSide.ALLY,
                        initiative++);
                ally.setPresentationSlot(group, slot);
                ally.setRef("playerOwnerId", playerId.toString());
                units.add(ally);
                actorOwners.put(instanceId, playerId);
            }
            group++;
        }

        for (int index = 0; index < encounter.enemies().size(); index++) {
            String enemyId = encounter.enemies().get(index);
            CombatantDefinition canonical = CanonicalData.definition(enemyId, encounter.level(), 0, false);
            CombatantDefinition soloTuned = CampaignEncounterCatalog.tempoAdjustedEnemy(canonical, encounter);
            CombatantDefinition scaled = SharedBattleRules.scaleEnemyHp(soloTuned, playerCount);
            String instanceId = encounter.boss()
                    ? "boss_" + enemyId.toLowerCase()
                    : CampaignEncounterCatalog.canonicalId(encounterId).toLowerCase() + "_enemy_" + index;
            units.add(new CombatantState(instanceId, scaled, CombatantSide.ENEMY, initiative++));
        }

        return new Blueprint(new BattleState(units, SharedBattleRules.capacity(playerCount)),
                List.copyOf(unique), actorOwners, SharedBattleRules.enemyHpMultiplier(playerCount));
    }
}
