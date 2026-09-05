package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.TurnboundBattleActors;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared field representation for authored campaign encounters outside Southgate.
 *
 * BattleSession owns combat truth and CampaignProgressStore owns player progression. This class owns only the
 * physical enemy silhouettes in the shared world. Multiple player sessions requesting the same encounter therefore
 * receive the same Group instead of spawning stacked copies. A group disappears only when no non-battling player in
 * that chapter still needs that encounter, and is reconstructed after a loss or when another player still needs it.
 */
public final class FieldEncounterPresentation {
    private static final Set<String> BACKLINE = Set.of("E002", "E005", "E007", "E010", "E011", "E013");
    private static final String COMMON_TAG = "turnbound_shared_field_enemy";
    private static final String ENCOUNTER_TAG_PREFIX = "turnbound_field_encounter:";
    private static final String SLOT_TAG_PREFIX = "turnbound_field_slot:";
    private static final Map<String, Group> SHARED = new LinkedHashMap<>();
    private static ServerLevel boundLevel;

    private FieldEncounterPresentation() {}

    public static final class Group {
        private final String encounterId;
        private final List<UUID> actorIds;
        private final Vec3 center;

        private Group(String encounterId, List<UUID> actorIds, Vec3 center) {
            this.encounterId = encounterId;
            this.actorIds = List.copyOf(actorIds);
            this.center = center;
        }

        public Entity lead(ServerLevel level) {
            if (actorIds.isEmpty()) return null;
            return level.getEntity(actorIds.getFirst());
        }

        public boolean alive(ServerLevel level) {
            if (actorIds.isEmpty()) return false;
            for (UUID id : actorIds) if (level.getEntity(id) == null) return false;
            return true;
        }

        public Vec3 center() { return center; }

        /**
         * Session-facing release. The shared actors remain when another active player still needs this encounter.
         * This lets one player enter battle without erasing another player's field target.
         */
        public void despawn(ServerLevel level) {
            bind(level);
            if (neededByAnyPlayer(level, encounterId)) return;
            discardNow(level);
            if (SHARED.get(encounterId) == this) SHARED.remove(encounterId);
        }

        private void discardNow(ServerLevel level) {
            for (UUID id : actorIds) {
                Entity entity = level.getEntity(id);
                if (entity != null) entity.discard();
            }
        }
    }

    public static Group spawn(ServerLevel level, String encounterId, Vec3 center, float battleYaw) {
        bind(level);
        V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(encounterId);

        Group existing = SHARED.get(encounterId);
        if (existing != null && existing.alive(level)) return existing;
        if (existing != null) SHARED.remove(encounterId);

        Group adopted = adoptTaggedGroup(level, encounterId, spec, center);
        if (adopted != null) {
            SHARED.put(encounterId, adopted);
            return adopted;
        }

        cleanupLegacyCopies(level, spec, center);
        Group created = spawnFresh(level, encounterId, spec, center, battleYaw);
        SHARED.put(encounterId, created);
        return created;
    }

    private static Group spawnFresh(ServerLevel level, String encounterId, V04Catalogs.Encounter spec, Vec3 center, float battleYaw) {
        List<UUID> ids = new ArrayList<>();
        Vec3 forward = forward(battleYaw);
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);

        for (int i = 0; i < spec.enemies().size(); i++) {
            String defId = spec.enemies().get(i);
            Vec3 pos = center.add(offset(spec.region(), defId, i, spec.enemies().size(), forward, right));
            float yaw = facingYaw(battleYaw);
            Entity actor = spawnActor(level, defId, spec.level(), pos, yaw, spec.boss());
            tag(actor, encounterId, i);
            ids.add(actor.getUUID());
        }
        return new Group(encounterId, ids, center);
    }

    private static Group adoptTaggedGroup(ServerLevel level, String encounterId, V04Catalogs.Encounter spec, Vec3 center) {
        AABB area = around(center, 7.0, 5.0);
        Map<Integer, Entity> bySlot = new HashMap<>();
        List<Entity> tagged = new ArrayList<>();
        collectTagged(level.getEntitiesOfClass(BattleActorEntity.class, area), encounterId, bySlot, tagged);
        collectTagged(level.getEntitiesOfClass(ArmorStand.class, area), encounterId, bySlot, tagged);

        if (tagged.isEmpty()) return null;
        if (bySlot.size() != spec.enemies().size()) {
            for (Entity entity : tagged) entity.discard();
            return null;
        }

        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < spec.enemies().size(); i++) {
            Entity entity = bySlot.get(i);
            if (entity == null) {
                for (Entity taggedEntity : tagged) taggedEntity.discard();
                return null;
            }
            ids.add(entity.getUUID());
        }
        return new Group(encounterId, ids, center);
    }

    private static <T extends Entity> void collectTagged(List<T> entities, String encounterId,
                                                          Map<Integer, Entity> bySlot, List<Entity> tagged) {
        String encounterTag = ENCOUNTER_TAG_PREFIX + encounterId;
        for (Entity entity : entities) {
            if (!entity.entityTags().contains(COMMON_TAG) || !entity.entityTags().contains(encounterTag)) continue;
            tagged.add(entity);
            int slot = slot(entity);
            Entity previous = bySlot.putIfAbsent(slot, entity);
            if (slot < 0 || previous != null) entity.discard();
        }
    }

    private static int slot(Entity entity) {
        for (String tag : entity.entityTags()) {
            if (!tag.startsWith(SLOT_TAG_PREFIX)) continue;
            try { return Integer.parseInt(tag.substring(SLOT_TAG_PREFIX.length())); }
            catch (NumberFormatException ignored) { return -1; }
        }
        return -1;
    }

    private static void tag(Entity entity, String encounterId, int slot) {
        entity.addTag(COMMON_TAG);
        entity.addTag(ENCOUNTER_TAG_PREFIX + encounterId);
        entity.addTag(SLOT_TAG_PREFIX + slot);
    }

    /**
     * Migrates alpha.17 per-player copies left in an existing world. Cleanup only runs with no active battle and
     * only near this encounter's authored position, with canonical enemy names as a second guard.
     */
    private static void cleanupLegacyCopies(ServerLevel level, V04Catalogs.Encounter spec, Vec3 center) {
        for (ServerPlayer player : level.players()) if (BattleSessionManager.exists(player)) return;
        Set<String> names = new java.util.HashSet<>();
        for (String defId : spec.enemies()) names.add(CanonicalData.definition(defId, spec.level(), 0, false).name());
        AABB area = around(center, 7.0, 5.0);
        for (BattleActorEntity entity : level.getEntitiesOfClass(BattleActorEntity.class, area)) {
            if (!entity.entityTags().contains(COMMON_TAG) && entity.getCustomName() != null
                    && names.contains(entity.getCustomName().getString())) entity.discard();
        }
        for (ArmorStand entity : level.getEntitiesOfClass(ArmorStand.class, area)) {
            if (!entity.entityTags().contains(COMMON_TAG) && entity.getCustomName() != null
                    && names.contains(entity.getCustomName().getString())) entity.discard();
        }
    }

    private static boolean neededByAnyPlayer(ServerLevel level, String encounterId) {
        FieldSharedEncounterRules.Region region = FieldSharedEncounterRules.regionOf(encounterId);
        for (ServerPlayer player : level.players()) {
            if (BattleSessionManager.exists(player) || !activeInRegion(player, region)) continue;
            var snapshot = CampaignProgressStore.snapshot(player.getUUID());
            if (snapshot.clearedEncounters().contains(encounterId)) continue;
            var quests = snapshot.quests();
            if (FieldSharedEncounterRules.unlocked(encounterId, quests.completed(), quests.unlockFlags())) return true;
        }
        return false;
    }

    private static boolean activeInRegion(ServerPlayer player, FieldSharedEncounterRules.Region region) {
        return switch (region) {
            case GLOAMWOOD -> GloamwoodSessionManager.active(player);
            case AQUEDUCT -> BrokenAqueductSessionManager.active(player);
            case QUARRY -> EmberQuarrySessionManager.active(player);
            case RELAY -> OldRelayStationSessionManager.active(player);
            case OTHER -> false;
        };
    }

    private static void bind(ServerLevel level) {
        if (boundLevel == level) return;
        SHARED.clear();
        boundLevel = level;
    }

    private static AABB around(Vec3 center, double horizontal, double vertical) {
        return new AABB(center.x - horizontal, center.y - vertical, center.z - horizontal,
                center.x + horizontal, center.y + vertical, center.z + horizontal);
    }

    private static Entity spawnActor(ServerLevel level, String defId, int levelValue, Vec3 pos, float yaw, boolean boss) {
        BattleActorEntity animated = TurnboundBattleActors.spawn(level, defId, pos, yaw);
        String name = CanonicalData.definition(defId, levelValue, 0, false).name();
        if (animated != null) {
            animated.setCustomName(Component.literal(name));
            animated.setCustomNameVisible(false);
            animated.setFieldWalking(false);
            if (defId.startsWith("EL")) animated.playReady();
            return animated;
        }

        ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setShowArms(true);
        stand.setYRot(yaw);
        stand.setYHeadRot(yaw);
        stand.setCustomName(Component.literal(name));
        stand.setCustomNameVisible(false);
        stand.setItemSlot(EquipmentSlot.CHEST, boss ? Items.DIAMOND_CHESTPLATE.getDefaultInstance() : Items.IRON_CHESTPLATE.getDefaultInstance());
        stand.setItemSlot(EquipmentSlot.MAINHAND, fallbackItem(defId));
        level.addFreshEntity(stand);
        return stand;
    }

    /** Region-specific silhouettes keep each authored field visually distinct. */
    private static Vec3 offset(String region, String defId, int index, int count, Vec3 forward, Vec3 right) {
        if (count <= 1) return Vec3.ZERO;
        Vec3 base = switch (region) {
            case "GLOAMWOOD" -> gloamwood(index, forward, right);
            case "AQUEDUCT" -> aqueduct(index, forward, right);
            case "QUARRY" -> quarry(index, forward, right);
            case "RELAY" -> relay(index, forward, right);
            default -> generic(index, forward, right);
        };
        if (BACKLINE.contains(defId)) base = base.add(forward.scale(-0.75));
        return base;
    }

    private static Vec3 gloamwood(int index, Vec3 forward, Vec3 right) {
        return switch (index) {
            case 0 -> right.scale(-1.35).add(forward.scale(0.15));
            case 1 -> right.scale(1.35).add(forward.scale(0.15));
            case 2 -> forward.scale(-1.35);
            case 3 -> right.scale(-2.15).add(forward.scale(-1.15));
            default -> right.scale(2.15).add(forward.scale(-1.15));
        };
    }

    private static Vec3 aqueduct(int index, Vec3 forward, Vec3 right) {
        return switch (index) {
            case 0 -> right.scale(-1.65);
            case 1 -> right.scale(0.0);
            case 2 -> right.scale(1.65);
            case 3 -> right.scale(-0.9).add(forward.scale(-1.35));
            default -> right.scale(0.9).add(forward.scale(-1.35));
        };
    }

    private static Vec3 quarry(int index, Vec3 forward, Vec3 right) {
        return switch (index) {
            case 0 -> forward.scale(0.75);
            case 1 -> right.scale(-1.35).add(forward.scale(-0.45));
            case 2 -> right.scale(1.35).add(forward.scale(-0.45));
            case 3 -> right.scale(-2.0).add(forward.scale(-1.55));
            default -> right.scale(2.0).add(forward.scale(-1.55));
        };
    }

    private static Vec3 relay(int index, Vec3 forward, Vec3 right) {
        return switch (index) {
            case 0 -> right.scale(-1.1).add(forward.scale(0.45));
            case 1 -> right.scale(1.1).add(forward.scale(-0.15));
            case 2 -> right.scale(-1.9).add(forward.scale(-1.2));
            case 3 -> right.scale(0.1).add(forward.scale(-1.55));
            default -> right.scale(2.0).add(forward.scale(-1.0));
        };
    }

    private static Vec3 generic(int index, Vec3 forward, Vec3 right) {
        return switch (index) {
            case 0 -> right.scale(-0.85);
            case 1 -> right.scale(0.85);
            case 2 -> forward.scale(-1.35);
            case 3 -> forward.scale(-1.35).add(right.scale(-1.35));
            default -> forward.scale(-1.35).add(right.scale(1.35));
        };
    }

    private static Vec3 forward(float yaw) {
        double rad = Math.toRadians(yaw);
        return new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
    }

    private static float facingYaw(float battleYaw) {
        float yaw = battleYaw + 180.0F;
        while (yaw >= 180.0F) yaw -= 360.0F;
        while (yaw < -180.0F) yaw += 360.0F;
        return yaw;
    }

    private static net.minecraft.world.item.ItemStack fallbackItem(String defId) {
        return switch (defId) {
            case "E002" -> Items.BOW.getDefaultInstance();
            case "E003" -> Items.TNT.getDefaultInstance();
            case "E005", "E007", "E011", "E013" -> Items.BLAZE_ROD.getDefaultInstance();
            case "E014", "EL04", "B04" -> Items.IRON_PICKAXE.getDefaultInstance();
            case "B01" -> Items.IRON_AXE.getDefaultInstance();
            case "B05" -> Items.DIAMOND_SWORD.getDefaultInstance();
            default -> Items.IRON_SWORD.getDefaultInstance();
        };
    }
}
