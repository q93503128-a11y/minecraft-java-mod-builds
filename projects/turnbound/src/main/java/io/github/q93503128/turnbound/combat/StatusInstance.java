package io.github.q93503128.turnbound.combat;
public record StatusInstance(String id, String sourceId, int remainingOwnerTurns, double magnitude) {
    public StatusInstance tickOwnerTurn() { return new StatusInstance(id, sourceId, remainingOwnerTurns - 1, magnitude); }
}
