package io.github.q93503128.turnbound.combat;

public record StatusInstance(String id, String sourceId, int remainingOwnerTurns, double magnitude, int stacks) {
    public StatusInstance(String id, String sourceId, int remainingOwnerTurns, double magnitude) {
        this(id, sourceId, remainingOwnerTurns, magnitude, 1);
    }
    public StatusInstance tickOwnerTurn() { return new StatusInstance(id, sourceId, remainingOwnerTurns - 1, magnitude, stacks); }
    public StatusInstance withStacks(int value) { return new StatusInstance(id, sourceId, remainingOwnerTurns, magnitude, value); }
    public StatusInstance refresh(int turns, double newMagnitude) { return new StatusInstance(id, sourceId, turns, newMagnitude, stacks); }
}
