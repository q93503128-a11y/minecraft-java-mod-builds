package kr.moonseungjun.earthtostars.ship.combat;

import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;

import java.util.Optional;
import java.util.UUID;

public record TurretFireSolution(
        ShipVec3 origin,
        ShipVec3 direction,
        Optional<UUID> targetId,
        double projectileSpeed,
        int projectileLifetimeTicks,
        float damage
) {
}
