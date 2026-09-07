package kr.moonseungjun.turnboundre.battle;

import kr.moonseungjun.turnboundre.data.StatusDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Battle-owned active-status ledger.
 * Data-defined statuses retain their definition and remaining duration so modifiers and expiry stay data-driven.
 * Core-managed states (GUARD/EXPOSED/POISE_GUARD) may still be stored without a definition because their lifetime
 * is part of the battle state-machine contract rather than ordinary status ticking.
 */
public final class StatusRuntime {
    public record ActiveStatus(StatusDefinition definition, int stacks, int remaining) {
        public ActiveStatus {
            if (stacks <= 0) throw new IllegalArgumentException("stacks must be > 0");
            if (definition != null && remaining <= 0) throw new IllegalArgumentException("data status remaining must be > 0");
        }

        public boolean dataDefined() { return definition != null; }
    }

    public record TriggeredHook(String statusId, int stacks, StatusDefinition.Hook hook) {}

    private final Map<String, ActiveStatus> activeById = new LinkedHashMap<>();

    public boolean has(String statusId) {
        return activeById.containsKey(requireId(statusId));
    }

    public int stacks(String statusId) {
        ActiveStatus status = activeById.get(requireId(statusId));
        return status == null ? 0 : status.stacks();
    }

    /** -1 means the lifetime is managed by a core battle rule instead of generic duration ticking. */
    public int remaining(String statusId) {
        ActiveStatus status = activeById.get(requireId(statusId));
        return status == null ? 0 : status.remaining();
    }

    /** Core-managed status insertion retained for GUARD/EXPOSED/POISE_GUARD. */
    public void apply(String statusId, int stacks, int maxStacks) {
        String id = requireId(statusId);
        validateStacks(stacks, maxStacks, id);
        activeById.put(id, new ActiveStatus(null, stacks, -1));
    }

    public void apply(StatusDefinition definition, int stacks, int durationOverride) {
        if (definition == null) throw new IllegalArgumentException("definition must not be null");
        String id = requireId(definition.id());
        validateStacks(stacks, definition.maxStacks(), id);
        int duration = durationOverride > 0 ? durationOverride : definition.baseDuration();
        if (duration <= 0) throw new IllegalArgumentException("duration must be > 0 for " + id);

        ActiveStatus current = activeById.get(id);
        if (current == null || current.definition() == null) {
            activeById.put(id, new ActiveStatus(definition, stacks, duration));
            return;
        }

        switch (definition.refreshRule()) {
            case "REFRESH_DURATION" -> activeById.put(id, new ActiveStatus(
                    definition, Math.min(definition.maxStacks(), Math.max(current.stacks(), stacks)), duration));
            case "ADD_STACK" -> activeById.put(id, new ActiveStatus(
                    definition, Math.min(definition.maxStacks(), current.stacks() + stacks), duration));
            case "REPLACE" -> activeById.put(id, new ActiveStatus(definition, stacks, duration));
            case "IGNORE" -> { /* existing status intentionally wins */ }
            default -> throw new IllegalArgumentException("unsupported refreshRule " + definition.refreshRule() + " for " + id);
        }
    }

    public boolean remove(String statusId) {
        return activeById.remove(requireId(statusId)) != null;
    }

    public void clear() {
        activeById.clear();
    }

    public Set<String> activeIds() {
        return Set.copyOf(activeById.keySet());
    }

    public List<TriggeredHook> hooks(String when) {
        if (when == null || when.isBlank()) throw new IllegalArgumentException("hook timing must not be blank");
        List<TriggeredHook> out = new ArrayList<>();
        activeById.forEach((id, active) -> {
            if (active.definition() == null) return;
            for (StatusDefinition.Hook hook : active.definition().hooks()) {
                if (when.equals(hook.when())) out.add(new TriggeredHook(id, active.stacks(), hook));
            }
        });
        return List.copyOf(out);
    }

    /** Multiplies every active hook of the given modifier type once per stack. */
    public double multiplier(String effectType) {
        if (effectType == null || effectType.isBlank()) throw new IllegalArgumentException("effectType must not be blank");
        double result = 1.0D;
        for (ActiveStatus active : activeById.values()) {
            if (active.definition() == null) continue;
            for (StatusDefinition.Hook hook : active.definition().hooks()) {
                if (!effectType.equals(hook.effect().type())) continue;
                double value = hook.effect().value();
                for (int i = 0; i < active.stacks(); i++) result *= value;
            }
        }
        return result;
    }

    /** Decrements one duration unit and returns ids that expired in deterministic insertion order. */
    public List<String> tick(String durationUnit) {
        if (durationUnit == null || durationUnit.isBlank()) throw new IllegalArgumentException("durationUnit must not be blank");
        List<String> expired = new ArrayList<>();
        List<Map.Entry<String, ActiveStatus>> snapshot = new ArrayList<>(activeById.entrySet());
        for (Map.Entry<String, ActiveStatus> entry : snapshot) {
            ActiveStatus active = entry.getValue();
            if (active.definition() == null || !durationUnit.equals(active.definition().durationUnit())) continue;
            int next = active.remaining() - 1;
            if (next <= 0) {
                activeById.remove(entry.getKey());
                expired.add(entry.getKey());
            } else {
                activeById.put(entry.getKey(), new ActiveStatus(active.definition(), active.stacks(), next));
            }
        }
        return List.copyOf(expired);
    }

    private static void validateStacks(int stacks, int maxStacks, String id) {
        if (stacks <= 0) throw new IllegalArgumentException("stacks must be > 0");
        if (maxStacks <= 0) throw new IllegalArgumentException("maxStacks must be > 0");
        if (stacks > maxStacks) throw new IllegalArgumentException("stacks exceed maxStacks for " + id);
    }

    private static String requireId(String statusId) {
        if (statusId == null || statusId.isBlank()) throw new IllegalArgumentException("statusId must not be blank");
        return statusId;
    }
}
