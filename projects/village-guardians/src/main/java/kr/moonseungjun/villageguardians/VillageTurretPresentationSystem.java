package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime-only visual owner for placed turrets.
 *
 * TurretState remains authoritative SavedData. These no-save actors are rebuilt from that state,
 * so losing a visual actor can never lose HP, level, placement or combat ownership.
 */
public final class VillageTurretPresentationSystem {
    private static final int ACTOR_DURATION = 20 * 60 * 60 * 24;
    private static final Map<Integer, UUID> ACTORS = new HashMap<>();
    private static int ticks;

    private VillageTurretPresentationSystem() {}

    public static synchronized void initialize(ServerLevel level, List<VillagePlacedTurretSystem.TurretState> states) {
        discardTracked(level);
        ticks = 0;
        if (states == null) return;
        for (VillagePlacedTurretSystem.TurretState state : states) ensure(level, state);
    }

    public static synchronized void tick(ServerLevel level, List<VillagePlacedTurretSystem.TurretState> states) {
        if (level == null || ++ticks < 20) return;
        ticks = 0;
        List<VillagePlacedTurretSystem.TurretState> safeStates = states == null ? List.of() : states;
        Set<Integer> liveIds = new HashSet<>();
        for (VillagePlacedTurretSystem.TurretState state : safeStates) {
            liveIds.add(state.id());
            ensure(level, state);
        }
        for (int id : new ArrayList<>(ACTORS.keySet())) {
            if (!liveIds.contains(id)) discard(level, id);
        }
    }

    public static synchronized void show(ServerLevel level, VillagePlacedTurretSystem.TurretState state) {
        ensure(level, state);
    }

    public static synchronized void remove(ServerLevel level, int id) {
        discard(level, id);
    }

    public static synchronized void aim(
            ServerLevel level,
            VillagePlacedTurretSystem.TurretState state,
            Vec3 target) {
        if (level == null || state == null || target == null || !state.active()) return;
        VillageSkillEffectEntity actor = ensure(level, state);
        if (actor == null) return;
        Vec3 origin = Vec3.atCenterOf(state.pos());
        Vec3 delta = target.subtract(origin);
        Vec3 horizontal = new Vec3(delta.x, 0.0, delta.z);
        if (horizontal.lengthSqr() > 1.0E-6) actor.setDirection(horizontal.normalize());
    }

    private static VillageSkillEffectEntity ensure(
            ServerLevel level,
            VillagePlacedTurretSystem.TurretState state) {
        if (level == null || state == null) return null;
        String expectedKind = state.active()
                ? "turret_body_" + state.type().id()
                : "turret_wreck_" + state.type().id();
        String expectedExtra = state.level() + "|" + (VillagePlacedTurretSystem.isDisabled(state.id()) ? 1 : 0);

        VillageSkillEffectEntity actor = actor(level, state.id());
        if (actor == null) {
            Vec3 position = new Vec3(state.pos().getX() + 0.5, state.pos().getY(), state.pos().getZ() + 0.5);
            actor = VillageSkillEffectEntity.spawn(level, null, expectedKind, position,
                    new Vec3(0.0, 0.0, 1.0), ACTOR_DURATION, 0.0f, expectedExtra);
            if (actor != null) ACTORS.put(state.id(), actor.getUUID());
            return actor;
        }

        if (!expectedKind.equals(actor.kind())) actor.setKind(expectedKind);
        if (!expectedExtra.equals(actor.extra())) actor.setExtra(expectedExtra);
        Vec3 expectedPosition = new Vec3(state.pos().getX() + 0.5, state.pos().getY(), state.pos().getZ() + 0.5);
        if (actor.position().distanceToSqr(expectedPosition) > 0.0001) actor.setPos(expectedPosition);
        return actor;
    }

    private static VillageSkillEffectEntity actor(ServerLevel level, int id) {
        UUID uuid = ACTORS.get(id);
        if (uuid == null) return null;
        Entity entity = level.getEntity(uuid);
        if (entity instanceof VillageSkillEffectEntity effect && effect.isAlive()) return effect;
        ACTORS.remove(id);
        return null;
    }

    private static void discard(ServerLevel level, int id) {
        UUID uuid = ACTORS.remove(id);
        if (uuid == null || level == null) return;
        Entity entity = level.getEntity(uuid);
        if (entity != null) entity.discard();
    }

    private static void discardTracked(ServerLevel level) {
        for (int id : new ArrayList<>(ACTORS.keySet())) discard(level, id);
        ACTORS.clear();
    }
}
