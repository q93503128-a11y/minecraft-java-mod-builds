package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.TurnboundBattleActors;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Shared field representation for authored campaign encounters outside Southgate.
 *
 * The battle catalog already defines the exact enemy composition. Field groups mirror that composition with real
 * actors, but also arrange it with a region-specific silhouette so every area does not look like the same five
 * mannequins in a generic row. This remains presentation only: BattleSession owns all actual combat truth.
 */
public final class FieldEncounterPresentation {
    private static final Set<String> BACKLINE = Set.of("E002", "E005", "E007", "E010", "E011", "E013");

    private FieldEncounterPresentation() {}

    public static final class Group {
        private final List<UUID> actorIds;
        private final Vec3 center;

        private Group(List<UUID> actorIds, Vec3 center) {
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

        public void despawn(ServerLevel level) {
            for (UUID id : actorIds) {
                Entity entity = level.getEntity(id);
                if (entity != null) entity.discard();
            }
        }
    }

    public static Group spawn(ServerLevel level, String encounterId, Vec3 center, float battleYaw) {
        V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(encounterId);
        List<UUID> ids = new ArrayList<>();
        Vec3 forward = forward(battleYaw);
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);

        for (int i = 0; i < spec.enemies().size(); i++) {
            String defId = spec.enemies().get(i);
            Vec3 pos = center.add(offset(spec.region(), defId, i, spec.enemies().size(), forward, right));
            float yaw = facingYaw(battleYaw);
            Entity actor = spawnActor(level, defId, spec.level(), pos, yaw, spec.boss());
            ids.add(actor.getUUID());
        }
        return new Group(ids, center);
    }

    private static Entity spawnActor(ServerLevel level, String defId, int levelValue, Vec3 pos, float yaw, boolean boss) {
        BattleActorEntity animated = TurnboundBattleActors.spawn(level, defId, pos, yaw);
        String name = CanonicalData.definition(defId, levelValue, 0, false).name();
        if (animated != null) {
            animated.setCustomName(Component.literal(name));
            animated.setCustomNameVisible(false);
            animated.setFieldWalking(false);
            // Solo elites should read as deliberate gatekeepers rather than ordinary idle mobs even before the
            // proximity threat prelude takes over. Boss telegraphs are intentionally saved for player approach.
            if (defId.startsWith("EL")) animated.playReady();
            return animated;
        }

        // Defensive fallback only. Authored campaign enemies should normally resolve to GeckoLib actors.
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

    /**
     * Region silhouettes:
     * - Gloamwood spreads into a crescent/ambush shape.
     * - Aqueduct guards hold disciplined lateral lines with support behind them.
     * - Quarry packs form a forward wedge, making beasts/drillers feel like a push down the route.
     * - Relay rooms use a staggered chamber formation to sell mixed recovered combat data.
     */
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

    /** Encounter actors face toward the party approach side instead of sharing the battle camera yaw literally. */
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
