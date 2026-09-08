package kr.moonseungjun.turnboundre.client;

import kr.moonseungjun.turnboundre.network.ProgressionNetworkPayloads;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Client-only temporary party composition. It previews a server-published squad cost,
 * but the server revalidates ownership, uniqueness and capacity before persistence.
 */
public final class PartyFormationDraft {
    private PartyFormationDraft() {}

    public static List<String> assign(List<String> current, int slot, String characterId) {
        if (current == null) throw new IllegalArgumentException("current party required");
        if (slot < 0 || slot >= 4) throw new IllegalArgumentException("slot must be 0..3");
        if (characterId == null || characterId.isBlank()) throw new IllegalArgumentException("characterId required");

        ArrayList<String> next = new ArrayList<>(current);
        int existing = next.indexOf(characterId);
        if (existing == slot) return List.copyOf(next);

        if (slot < next.size()) {
            if (existing >= 0) {
                String displaced = next.get(slot);
                next.set(slot, characterId);
                next.set(existing, displaced);
            } else {
                next.set(slot, characterId);
            }
        } else {
            if (existing >= 0) next.remove(existing);
            if (next.size() < 4) next.add(characterId);
        }
        return List.copyOf(next);
    }

    public static List<String> remove(List<String> current, int slot) {
        if (current == null) throw new IllegalArgumentException("current party required");
        if (slot < 0 || slot >= 4) throw new IllegalArgumentException("slot must be 0..3");
        if (slot >= current.size()) return List.copyOf(current);
        ArrayList<String> next = new ArrayList<>(current);
        next.remove(slot);
        return List.copyOf(next);
    }

    public static int cost(List<String> party, ProgressionNetworkPayloads.Snapshot snapshot) {
        if (party == null || snapshot == null) throw new IllegalArgumentException("party/snapshot required");
        int total = 0;
        for (String id : party) {
            ProgressionNetworkPayloads.CharacterView view = snapshot.character(id).orElse(null);
            if (view == null) return Integer.MAX_VALUE;
            total = Math.addExact(total, view.squadCost());
        }
        return total;
    }

    public static boolean isSubmittable(List<String> party, ProgressionNetworkPayloads.Snapshot snapshot) {
        if (party == null || snapshot == null || party.size() > 4) return false;
        if (new HashSet<>(party).size() != party.size()) return false;
        for (String id : party) {
            ProgressionNetworkPayloads.CharacterView view = snapshot.character(id).orElse(null);
            if (view == null || !view.owned()) return false;
        }
        return cost(party, snapshot) <= snapshot.partyCapacity();
    }
}
