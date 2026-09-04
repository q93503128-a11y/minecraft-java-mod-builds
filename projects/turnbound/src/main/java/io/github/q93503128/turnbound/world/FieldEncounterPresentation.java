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
import java.util.UUID;

/**
 * Shared field representation for authored campaign encounters outside Southgate.
 *
 * The battle catalog already defines the exact enemy composition. The old chapter runtimes reduced that whole
 * composition to one labelled ArmorStand, so a five-enemy room and a solo elite looked almost identical until
 * combat started. This presentation group mirrors the canonical composition with the real enemy/boss actors while
 * remaining completely non-authoritative: proximity still starts the existing BattleSession and combat rules stay
 * untouched.
 */
public final class FieldEncounterPresentation {
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
            Vec3 pos = center.add(offset(i, spec.enemies().size(), forward, right));
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

    /** Wide enough to read the encounter composition before engagement without blocking the authored route. */
    private static Vec3 offset(int index, int count, Vec3 forward, Vec3 right) {
        if (count <= 1) return Vec3.ZERO;
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
