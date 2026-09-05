package io.github.q93503128.turnbound.world;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared physical ArmorStand registry for authored auxiliary world interactions.
 *
 * Player progression and menu state remain outside this class. A scope only expresses that one player currently
 * needs a physical actor to exist. Multiple players observing the same key reuse the same world entity, and one
 * player leaving cannot remove it while another observer still needs it.
 */
public final class SharedAuxiliaryActors {
    public record Spec(String key, Vec3 pos, Component name, Item item, boolean invisible, boolean showArms,
                       Collection<String> legacyNames) {
        public Spec {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("key");
            if (pos == null) throw new IllegalArgumentException("pos");
            if (name == null) throw new IllegalArgumentException("name");
            if (item == null) throw new IllegalArgumentException("item");
            legacyNames = legacyNames == null ? List.of() : List.copyOf(legacyNames);
        }
    }

    private record Observer(UUID playerId, String scope) {}

    private static final String COMMON_TAG = SharedAuxiliaryActorCatalog.COMMON_TAG;
    private static final AABB WORLD_AREA = new AABB(-520, 40, -520, 520, 116, 520);
    private static final Map<Observer, Set<String>> OBSERVATIONS = new ConcurrentHashMap<>();

    private SharedAuxiliaryActors() {}

    public static String key(Entity entity) {
        if (!(entity instanceof ArmorStand)) return null;
        for (String tag : entity.entityTags()) {
            String key = SharedAuxiliaryActorCatalog.fromTag(tag);
            if (key != null) return key;
        }
        return null;
    }

    public static void sync(ServerLevel level, UUID playerId, String scope, Collection<Spec> desired) {
        if (level == null || playerId == null || scope == null || scope.isBlank()) return;
        Observer observer = new Observer(playerId, scope);
        Map<String, Spec> byKey = new HashMap<>();
        if (desired != null) {
            for (Spec spec : desired) {
                if (spec == null) continue;
                byKey.put(spec.key(), spec);
                ensure(level, spec);
            }
        }

        Set<String> next = Set.copyOf(byKey.keySet());
        Set<String> previous = OBSERVATIONS.put(observer, next);
        if (previous == null) return;
        for (String removed : previous) {
            if (!next.contains(removed)) discardIfUnobserved(level, removed);
        }
    }

    public static void removeScope(ServerLevel level, UUID playerId, String scope) {
        if (level == null || playerId == null || scope == null) return;
        Set<String> removed = OBSERVATIONS.remove(new Observer(playerId, scope));
        if (removed == null) return;
        for (String key : removed) discardIfUnobserved(level, key);
    }

    public static void ensure(ServerLevel level, Spec spec) {
        if (level == null || spec == null) return;
        removeLegacyNear(level, spec);
        ArmorStand found = null;
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, WORLD_AREA)) {
            if (!spec.key().equals(key(stand))) continue;
            if (found == null) found = stand;
            else stand.discard();
        }
        if (found == null || found.isRemoved()) {
            found = new ArmorStand(level, spec.pos().x, spec.pos().y, spec.pos().z);
            configure(found, spec);
            level.addFreshEntity(found);
        } else {
            configure(found, spec);
        }
    }

    private static void configure(ArmorStand stand, Spec spec) {
        stand.setPos(spec.pos().x, spec.pos().y, spec.pos().z);
        stand.setDeltaMovement(Vec3.ZERO);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setInvisible(spec.invisible());
        stand.setShowArms(spec.showArms());
        stand.setCustomName(spec.name());
        stand.setCustomNameVisible(true);
        stand.setItemSlot(EquipmentSlot.MAINHAND, spec.item().getDefaultInstance());
        stand.addTag(COMMON_TAG);
        stand.addTag(SharedAuxiliaryActorCatalog.roleTag(spec.key()));
    }

    private static void removeLegacyNear(ServerLevel level, Spec spec) {
        if (spec.legacyNames().isEmpty()) return;
        Set<String> aliases = new HashSet<>(spec.legacyNames());
        Vec3 p = spec.pos();
        AABB nearby = new AABB(p.x - 1.25, p.y - 1.0, p.z - 1.25, p.x + 1.25, p.y + 2.5, p.z + 1.25);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, nearby)) {
            if (key(stand) != null) continue;
            Component name = stand.getCustomName();
            if (name != null && aliases.contains(name.getString())) stand.discard();
        }
    }

    private static void discardIfUnobserved(ServerLevel level, String actorKey) {
        if (actorKey == null || stillObserved(actorKey)) return;
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, WORLD_AREA)) {
            if (actorKey.equals(key(stand))) stand.discard();
        }
    }

    private static boolean stillObserved(String actorKey) {
        for (Set<String> keys : OBSERVATIONS.values()) if (keys.contains(actorKey)) return true;
        return false;
    }
}
