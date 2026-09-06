package kr.moonseungjun.turnboundre.battle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Battle-owned active-status ledger. Duration/refresh timing is driven by battle rules and StatusService;
 * this class only owns deterministic active ids and stack counts.
 */
public final class StatusRuntime {
    private final Map<String, Integer> stacksById = new LinkedHashMap<>();

    public boolean has(String statusId) {
        return stacksById.containsKey(requireId(statusId));
    }

    public int stacks(String statusId) {
        return stacksById.getOrDefault(requireId(statusId), 0);
    }

    public void apply(String statusId, int stacks, int maxStacks) {
        String id = requireId(statusId);
        if (stacks <= 0) throw new IllegalArgumentException("stacks must be > 0");
        if (maxStacks <= 0) throw new IllegalArgumentException("maxStacks must be > 0");
        if (stacks > maxStacks) throw new IllegalArgumentException("stacks exceed maxStacks for " + id);
        stacksById.put(id, stacks);
    }

    public boolean remove(String statusId) {
        return stacksById.remove(requireId(statusId)) != null;
    }

    public Set<String> activeIds() {
        return Set.copyOf(stacksById.keySet());
    }

    private static String requireId(String statusId) {
        if (statusId == null || statusId.isBlank()) throw new IllegalArgumentException("statusId must not be blank");
        return statusId;
    }
}
