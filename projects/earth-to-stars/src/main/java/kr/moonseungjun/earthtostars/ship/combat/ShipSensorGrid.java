package kr.moonseungjun.earthtostars.ship.combat;

import kr.moonseungjun.earthtostars.ship.runtime.ShipVec3;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ShipSensorGrid {
    private final Map<UUID, SensorContact> contacts = new LinkedHashMap<>();
    private long lastScanTick = Long.MIN_VALUE;

    public synchronized void update(Collection<SensorContact> freshContacts, long tick) {
        contacts.clear();
        for (SensorContact contact : freshContacts) {
            contacts.put(contact.targetId(), contact);
        }
        lastScanTick = tick;
    }

    public synchronized void expireOlderThan(long tick, long maxAgeTicks) {
        if (maxAgeTicks < 0L) {
            throw new IllegalArgumentException("maxAgeTicks must be >= 0");
        }
        if (lastScanTick == Long.MIN_VALUE) {
            return;
        }
        if (tick < lastScanTick) {
            throw new IllegalArgumentException("sensor tick must be monotonic");
        }
        if (tick - lastScanTick > maxAgeTicks) {
            contacts.clear();
        }
    }

    public synchronized Optional<SensorContact> bestHostile(ShipVec3 origin, ShipVec3 forward, TurretProfile profile) {
        ShipVec3 normalizedForward = forward.normalized();
        if (normalizedForward.lengthSquared() < 1.0E-12D) {
            return Optional.empty();
        }
        double maxDistanceSquared = profile.range() * profile.range();
        double minDot = Math.cos(Math.toRadians(profile.halfArcDegrees()));
        return contacts.values().stream()
                .filter(SensorContact::hostile)
                .filter(contact -> {
                    ShipVec3 delta = subtract(contact.position(), origin);
                    double distanceSquared = delta.lengthSquared();
                    if (distanceSquared < 1.0E-12D || distanceSquared > maxDistanceSquared) return false;
                    return normalizedForward.dot(delta.normalized()) >= minDot;
                })
                .min(Comparator
                        .comparingDouble((SensorContact contact) -> -contact.threat())
                        .thenComparingDouble(contact -> subtract(contact.position(), origin).lengthSquared()));
    }

    public synchronized int contactCount() {
        return contacts.size();
    }

    public synchronized long lastScanTick() {
        return lastScanTick;
    }

    private static ShipVec3 subtract(ShipVec3 left, ShipVec3 right) {
        return new ShipVec3(left.x() - right.x(), left.y() - right.y(), left.z() - right.z());
    }
}
