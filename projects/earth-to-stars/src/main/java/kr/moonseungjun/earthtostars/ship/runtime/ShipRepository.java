package kr.moonseungjun.earthtostars.ship.runtime;

import kr.moonseungjun.earthtostars.ship.domain.ModuleSlot;
import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.domain.ShipState;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShipRepository {
    private final Map<ShipId, ShipState> ships = new ConcurrentHashMap<>();

    public ShipState create(UUID ownerId, Collection<ModuleSlot> slots) {
        ShipState state = ShipState.create(ShipId.random(), ownerId, slots);
        add(state);
        return state;
    }

    public void add(ShipState state) {
        if (ships.putIfAbsent(state.shipId(), state) != null) {
            throw new IllegalArgumentException("duplicate ship id: " + state.shipId());
        }
    }

    public Optional<ShipState> find(ShipId shipId) {
        return Optional.ofNullable(ships.get(shipId));
    }

    public int size() {
        return ships.size();
    }
}
