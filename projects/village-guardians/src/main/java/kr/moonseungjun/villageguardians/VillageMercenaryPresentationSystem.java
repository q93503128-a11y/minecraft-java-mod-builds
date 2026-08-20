package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Runtime-only owner-follow silhouettes that make mercenary classes readable without changing gameplay hitboxes. */
public final class VillageMercenaryPresentationSystem {
    private static final Map<UUID, Presence> ACTIVE = new HashMap<>();
    private static final int PRESENCE_DURATION = 20 * 60 * 60;

    private VillageMercenaryPresentationSystem() {}

    public static synchronized void reset() {
        ACTIVE.clear();
    }

    public static synchronized void ensure(
            ServerLevel level,
            Mob mercenary,
            VillageMercenarySystem.MercenaryClass kind,
            int rank) {
        if (level == null || mercenary == null || kind == null || !mercenary.isAlive()) return;
        int tier = visualTier(rank);
        String effectKind = "mercenary_presence_" + kind.id();
        Presence current = ACTIVE.get(mercenary.getUUID());
        if (current != null) {
            Entity actor = level.getEntity(current.actorUuid());
            if (actor instanceof VillageSkillEffectEntity effect && effect.isAlive()
                    && current.tier() == tier && effectKind.equals(effect.kind())) return;
            if (actor != null) actor.discard();
        }
        Vec3 look = horizontal(mercenary.getLookAngle());
        VillageSkillEffectEntity actor = VillageSkillEffectEntity.spawn(level, mercenary, effectKind,
                mercenary.position(), look, PRESENCE_DURATION, 0.0f, Integer.toString(tier));
        if (actor != null) ACTIVE.put(mercenary.getUUID(), new Presence(actor.getUUID(), tier));
    }

    public static synchronized void remove(ServerLevel level, UUID mercenaryUuid) {
        Presence presence = ACTIVE.remove(mercenaryUuid);
        if (presence == null || level == null) return;
        Entity actor = level.getEntity(presence.actorUuid());
        if (actor != null) actor.discard();
    }

    static int visualTier(int rank) {
        int safe = Math.max(1, Math.min(VillageMercenarySystem.MAX_LEVEL, rank));
        if (safe >= 60) return 3;
        if (safe >= 40) return 2;
        if (safe >= 20) return 1;
        return 0;
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 source = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return source.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : source.normalize();
    }

    private record Presence(UUID actorUuid, int tier) {}
}
