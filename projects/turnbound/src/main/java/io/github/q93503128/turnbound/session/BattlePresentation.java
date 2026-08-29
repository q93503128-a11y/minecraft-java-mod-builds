package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class BattlePresentation {
    private final Map<String, UUID> actors = new LinkedHashMap<>();
    private final Map<String, Vec3> homes = new LinkedHashMap<>();
    private UUID focusMarker;
    private String moving;
    private int returnTicks;

    void spawn(ServerPlayer player, Iterable<CombatantState> combatants) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 anchor = player.position();
        Vec3 forward = horizontal(player.getLookAngle());
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        int allyIndex = 0;
        int enemyIndex = 0;

        for (CombatantState combatant : combatants) {
            boolean ally = combatant.side() == CombatantSide.ALLY;
            int index = ally ? allyIndex++ : enemyIndex++;
            double lane = ally
                    ? -3.0 + index * 2.0
                    : -3.2 + index * 1.6;
            double distance = ally ? 3.8 : 7.3;
            Vec3 pos = anchor.add(forward.scale(distance)).add(right.scale(lane));

            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setCustomName(Component.literal(combatant.definition().name()));
            stand.setCustomNameVisible(false);
            stand.setInvulnerable(true);
            stand.setNoGravity(true);
            stand.setShowArms(true);
            stand.setYRot(player.getYRot() + (ally ? 0.0F : 180.0F));
            equipStandIn(stand, combatant, index);
            level.addFreshEntity(stand);

            actors.put(combatant.instanceId(), stand.getUUID());
            homes.put(combatant.instanceId(), pos);
        }
    }

    void focus(ServerLevel level, String targetId) {
        clearFocus(level);
        if (targetId == null || targetId.isBlank()) return;
        Vec3 target = homes.get(targetId);
        if (target == null) return;

        // NeoForge/Minecraft 26.2 exposes ArmorStand small/marker mutators privately.
        // A normal invisible, invulnerable no-gravity stand is sufficient because this entity is presentation-only.
        ArmorStand marker = new ArmorStand(level, target.x, target.y + 0.35, target.z);
        marker.setInvisible(true);
        marker.setInvulnerable(true);
        marker.setNoGravity(true);
        marker.setCustomName(Component.literal("▼").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        marker.setCustomNameVisible(true);
        level.addFreshEntity(marker);
        focusMarker = marker.getUUID();
    }

    void clearFocus(ServerLevel level) {
        if (focusMarker == null) return;
        var entity = level.getEntity(focusMarker);
        if (entity != null) entity.discard();
        focusMarker = null;
    }

    void lunge(ServerLevel level, String actorId, String targetId) {
        ArmorStand actor = entity(level, actorId);
        Vec3 target = homes.get(targetId);
        Vec3 home = homes.get(actorId);
        if (actor == null || target == null || home == null) return;
        Vec3 delta = target.subtract(home);
        if (delta.lengthSqr() > 0.001) {
            actor.setPos(target.subtract(delta.normalize().scale(1.45)));
        }
        moving = actorId;
        returnTicks = 5;
    }

    void tick(ServerLevel level) {
        if (moving == null) return;
        if (--returnTicks <= 0) {
            ArmorStand actor = entity(level, moving);
            Vec3 home = homes.get(moving);
            if (actor != null && home != null) actor.setPos(home);
            moving = null;
        }
    }

    void cleanup(ServerLevel level) {
        clearFocus(level);
        for (UUID id : actors.values()) {
            var entity = level.getEntity(id);
            if (entity != null) entity.discard();
        }
        actors.clear();
        homes.clear();
        moving = null;
    }

    private ArmorStand entity(ServerLevel level, String id) {
        UUID uuid = actors.get(id);
        if (uuid == null) return null;
        var entity = level.getEntity(uuid);
        return entity instanceof ArmorStand armorStand ? armorStand : null;
    }

    private static void equipStandIn(ArmorStand stand, CombatantState combatant, int index) {
        boolean ally = combatant.side() == CombatantSide.ALLY;
        stand.setItemSlot(EquipmentSlot.CHEST,
                (ally ? Items.CHAINMAIL_CHESTPLATE : Items.IRON_CHESTPLATE).getDefaultInstance());
        stand.setItemSlot(EquipmentSlot.LEGS,
                (ally ? Items.LEATHER_LEGGINGS : Items.IRON_LEGGINGS).getDefaultInstance());
        stand.setItemSlot(EquipmentSlot.FEET,
                (ally ? Items.LEATHER_BOOTS : Items.IRON_BOOTS).getDefaultInstance());

        if (ally) {
            String id = combatant.definition().id();
            ItemStack mainHand = switch (id) {
                case "P01" -> Items.DIAMOND_SWORD.getDefaultInstance();
                case "P02" -> Items.CLOCK.getDefaultInstance();
                case "P03" -> Items.IRON_SWORD.getDefaultInstance();
                case "P04" -> Items.BLAZE_ROD.getDefaultInstance();
                default -> Items.IRON_SWORD.getDefaultInstance();
            };
            stand.setItemSlot(EquipmentSlot.MAINHAND, mainHand);
            if (id.equals("P03")) stand.setItemSlot(EquipmentSlot.OFFHAND, Items.SHIELD.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.HEAD, Items.DIAMOND_HELMET.getDefaultInstance());
        } else {
            stand.setItemSlot(EquipmentSlot.HEAD, Items.IRON_HELMET.getDefaultInstance());
            switch (index) {
                case 2 -> stand.setItemSlot(EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
                case 3 -> {
                    stand.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_SWORD.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.OFFHAND, Items.SHIELD.getDefaultInstance());
                }
                case 4 -> stand.setItemSlot(EquipmentSlot.MAINHAND, Items.BLAZE_ROD.getDefaultInstance());
                default -> stand.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_SWORD.getDefaultInstance());
            }
        }
    }

    private static Vec3 horizontal(Vec3 vector) {
        Vec3 horizontal = new Vec3(vector.x, 0, vector.z);
        return horizontal.lengthSqr() < 0.001 ? new Vec3(0, 0, 1) : horizontal.normalize();
    }
}
