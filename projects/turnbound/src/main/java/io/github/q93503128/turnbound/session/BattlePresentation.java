package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.TurnboundBattleActors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class BattlePresentation {
    private static final double[][] ALLY_FORMATION = {
            {-3.0, 4.0}, {-1.0, 4.0}, {1.0, 4.0}, {3.0, 4.0}
    };
    private static final double[][] ENEMY_FORMATION = {
            {-4.0, -4.0}, {-2.0, -4.0}, {0.0, -4.0}, {2.0, -4.0}, {4.0, -4.0}
    };

    private final Map<String, UUID> actors = new LinkedHashMap<>();
    private final Map<String, Vec3> homes = new LinkedHashMap<>();
    private final Map<String, CombatantSide> sides = new LinkedHashMap<>();
    private final Map<String, Boolean> summons = new LinkedHashMap<>();
    private UUID focusMarker;
    private UUID dangerMarker;
    private String dangerTarget = "";
    private String moving;
    private int returnTicks;

    void spawn(ServerLevel level, Vec3 center, float facingYaw, Iterable<CombatantState> combatants) {
        cleanupActors(level);
        spawnMissing(level, center, facingYaw, combatants);
    }

    void spawnMissing(ServerLevel level, Vec3 center, float facingYaw, Iterable<CombatantState> combatants) {
        List<CombatantState> units = new ArrayList<>();
        combatants.forEach(units::add);
        removeMissing(level, units);

        Vec3 forward = BattleArenaLocator.forward(facingYaw);
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        int allyIndex = 0;
        int enemyIndex = 0;
        for (CombatantState combatant : units) {
            if (combatant.definition().summon()) continue;
            boolean ally = combatant.side() == CombatantSide.ALLY;
            int index = ally ? allyIndex++ : enemyIndex++;
            if (actors.containsKey(combatant.instanceId())) continue;
            double[][] formation = ally ? ALLY_FORMATION : ENEMY_FORMATION;
            if (index >= formation.length) continue;
            Vec3 raw = localToWorld(center, right, forward, formation[index][0], formation[index][1]);
            spawnActor(level, combatant, BattleArenaLocator.groundPosition(level, raw), facingYaw);
        }

        for (CombatantState combatant : units) {
            if (!combatant.definition().summon() || actors.containsKey(combatant.instanceId())) continue;
            Vec3 ownerHome = homes.get(combatant.ref("ownerId"));
            if (ownerHome == null) ownerHome = center;
            Vec3 raw = ownerHome.add(right.scale(0.8)).subtract(forward.scale(1.0));
            spawnActor(level, combatant, BattleArenaLocator.groundPosition(level, raw), facingYaw);
        }
    }

    private void spawnActor(ServerLevel level, CombatantState combatant, Vec3 pos, float facingYaw) {
        boolean ally = combatant.side() == CombatantSide.ALLY;
        float yaw = facingYaw + (ally ? 0.0F : 180.0F);
        Entity actor = null;

        if (!combatant.definition().summon() && TurnboundBattleActors.contains(combatant.definition().id())) {
            BattleActorEntity animated = TurnboundBattleActors.spawn(level, combatant.definition().id(), pos, yaw);
            if (animated != null) {
                animated.setCustomName(Component.literal(combatant.definition().name()));
                animated.setCustomNameVisible(false);
                actor = animated;
            }
        }

        if (actor == null) {
            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setCustomName(Component.literal(combatant.definition().name()));
            stand.setCustomNameVisible(false);
            stand.setInvulnerable(true);
            stand.setNoGravity(true);
            stand.setShowArms(true);
            stand.setYRot(yaw);
            equipStandIn(stand, combatant);
            level.addFreshEntity(stand);
            actor = stand;
        }

        actors.put(combatant.instanceId(), actor.getUUID());
        homes.put(combatant.instanceId(), pos);
        sides.put(combatant.instanceId(), combatant.side());
        summons.put(combatant.instanceId(), combatant.definition().summon());
    }

    private void removeMissing(ServerLevel level, List<CombatantState> units) {
        Set<String> liveIds = new HashSet<>();
        for (CombatantState unit : units) liveIds.add(unit.instanceId());
        for (String id : List.copyOf(actors.keySet())) {
            if (liveIds.contains(id)) continue;
            UUID uuid = actors.remove(id);
            Entity entity = uuid == null ? null : level.getEntity(uuid);
            if (entity != null) entity.discard();
            homes.remove(id);
            sides.remove(id);
            summons.remove(id);
            if (id.equals(moving)) moving = null;
        }
    }

    private static Vec3 localToWorld(Vec3 center, Vec3 right, Vec3 forward, double x, double z) {
        return center.add(right.scale(x)).subtract(forward.scale(z));
    }

    Vec3 home(String combatantId) { return homes.get(combatantId); }

    Vec3 center() {
        Vec3 ally = centroid(CombatantSide.ALLY);
        Vec3 enemy = centroid(CombatantSide.ENEMY);
        if (ally != null && enemy != null) return ally.add(enemy).scale(0.5);
        if (ally != null) return ally;
        if (enemy != null) return enemy;
        return Vec3.ZERO;
    }

    private Vec3 centroid(CombatantSide side) {
        double x = 0.0, y = 0.0, z = 0.0;
        int count = 0;
        for (var entry : homes.entrySet()) {
            if (sides.get(entry.getKey()) != side || Boolean.TRUE.equals(summons.get(entry.getKey()))) continue;
            Vec3 home = entry.getValue();
            x += home.x; y += home.y; z += home.z; count++;
        }
        return count == 0 ? null : new Vec3(x / count, y / count, z / count);
    }

    void focus(ServerLevel level, String targetId) {
        clearFocus(level);
        Vec3 target = homes.get(targetId);
        if (target == null) return;
        ChatFormatting color = sides.get(targetId) == CombatantSide.ALLY ? ChatFormatting.AQUA : ChatFormatting.RED;
        ArmorStand marker = marker(level, target.add(0, 1.0, 0), "▼", color);
        focusMarker = marker.getUUID();
    }

    void syncDanger(ServerLevel level, Iterable<CombatantState> combatants) {
        String targetId = "";
        for (CombatantState unit : combatants) {
            if (!unit.downed() && (unit.hasStatus("e003_armed") || unit.hasStatus("b01_charge_warning")
                    || unit.hasStatus("b04_eruption_warning") || unit.hasStatus("b05_collapse_warning"))) {
                targetId = unit.instanceId();
                break;
            }
        }
        if (targetId.equals(dangerTarget)) return;
        clearDanger(level);
        dangerTarget = targetId;
        if (targetId.isBlank()) return;
        Vec3 target = homes.get(targetId);
        if (target == null) return;
        ArmorStand marker = marker(level, target.add(0, 1.35, 0), "!", ChatFormatting.GOLD);
        dangerMarker = marker.getUUID();
    }

    private ArmorStand marker(ServerLevel level, Vec3 pos, String text, ChatFormatting color) {
        ArmorStand marker = new ArmorStand(level, pos.x, pos.y, pos.z);
        marker.setInvisible(true);
        marker.setInvulnerable(true);
        marker.setNoGravity(true);
        marker.setCustomName(Component.literal(text).withStyle(color, ChatFormatting.BOLD));
        marker.setCustomNameVisible(true);
        level.addFreshEntity(marker);
        return marker;
    }

    void clearFocus(ServerLevel level) {
        if (focusMarker == null) return;
        Entity entity = level.getEntity(focusMarker);
        if (entity != null) entity.discard();
        focusMarker = null;
    }

    private void clearDanger(ServerLevel level) {
        if (dangerMarker != null) {
            Entity entity = level.getEntity(dangerMarker);
            if (entity != null) entity.discard();
        }
        dangerMarker = null;
        dangerTarget = "";
    }

    void performSkill(ServerLevel level, String actorId, String targetId, boolean damaging) {
        Entity actor = entity(level, actorId);
        if (actor instanceof BattleActorEntity animated) animated.playStrike();
        if (!damaging || targetId == null || targetId.isBlank()) return;
        Vec3 target = homes.get(targetId);
        Vec3 home = homes.get(actorId);
        if (actor == null || target == null || home == null) return;
        Vec3 delta = target.subtract(home);
        if (delta.lengthSqr() > 0.001) actor.setPos(target.subtract(delta.normalize().scale(1.45)));
        moving = actorId;
        returnTicks = 5;
    }

    void lunge(ServerLevel level, String actorId, String targetId) {
        performSkill(level, actorId, targetId, true);
    }

    void tick(ServerLevel level) {
        if (moving == null) return;
        if (--returnTicks <= 0) {
            Entity actor = entity(level, moving);
            Vec3 home = homes.get(moving);
            if (actor != null && home != null) actor.setPos(home);
            moving = null;
        }
    }

    void cleanup(ServerLevel level) {
        clearFocus(level);
        clearDanger(level);
        cleanupActors(level);
        moving = null;
    }

    private void cleanupActors(ServerLevel level) {
        for (UUID id : actors.values()) {
            Entity entity = level.getEntity(id);
            if (entity != null) entity.discard();
        }
        actors.clear();
        homes.clear();
        sides.clear();
        summons.clear();
    }

    private Entity entity(ServerLevel level, String id) {
        UUID uuid = actors.get(id);
        return uuid == null ? null : level.getEntity(uuid);
    }

    private static void equipStandIn(ArmorStand stand, CombatantState combatant) {
        boolean ally = combatant.side() == CombatantSide.ALLY;
        String id = combatant.definition().id();
        if (combatant.definition().summon()) {
            setSmall(stand);
            stand.setItemSlot(EquipmentSlot.HEAD, Items.WOLF_ARMOR.getDefaultInstance());
            return;
        }
        stand.setItemSlot(EquipmentSlot.CHEST, (ally ? Items.CHAINMAIL_CHESTPLATE : Items.IRON_CHESTPLATE).getDefaultInstance());
        stand.setItemSlot(EquipmentSlot.LEGS, (ally ? Items.LEATHER_LEGGINGS : Items.IRON_LEGGINGS).getDefaultInstance());
        stand.setItemSlot(EquipmentSlot.FEET, (ally ? Items.LEATHER_BOOTS : Items.IRON_BOOTS).getDefaultInstance());
        if (ally) {
            ItemStack mainHand = switch (id) {
                case "P01" -> Items.DIAMOND_SWORD.getDefaultInstance();
                case "P02" -> Items.CLOCK.getDefaultInstance();
                case "P03" -> Items.IRON_SWORD.getDefaultInstance();
                case "P04" -> Items.BLAZE_ROD.getDefaultInstance();
                case "P05", "F03" -> Items.CROSSBOW.getDefaultInstance();
                case "P06" -> Items.IRON_HOE.getDefaultInstance();
                case "P07" -> Items.PAPER.getDefaultInstance();
                case "P08" -> Items.IRON_AXE.getDefaultInstance();
                case "F04" -> Items.IRON_SWORD.getDefaultInstance();
                default -> Items.IRON_SWORD.getDefaultInstance();
            };
            stand.setItemSlot(EquipmentSlot.MAINHAND, mainHand);
            if (id.equals("P03") || id.equals("F04")) stand.setItemSlot(EquipmentSlot.OFFHAND, Items.SHIELD.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.HEAD, (id.equals("F03") || id.equals("F04"))
                    ? Items.LEATHER_HELMET.getDefaultInstance() : Items.DIAMOND_HELMET.getDefaultInstance());
            return;
        }
        stand.setItemSlot(EquipmentSlot.HEAD, Items.IRON_HELMET.getDefaultInstance());
        switch (id) {
            case "E002" -> stand.setItemSlot(EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
            case "E003" -> stand.setItemSlot(EquipmentSlot.MAINHAND, Items.TNT.getDefaultInstance());
            case "E005", "E007", "E011", "E013" -> stand.setItemSlot(EquipmentSlot.MAINHAND, Items.BLAZE_ROD.getDefaultInstance());
            case "B01", "B04" -> stand.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_AXE.getDefaultInstance());
            case "B05" -> stand.setItemSlot(EquipmentSlot.MAINHAND, Items.DIAMOND_SWORD.getDefaultInstance());
            default -> stand.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_SWORD.getDefaultInstance());
        }
    }

    /** Mojang 1.21.11/26.2 made ArmorStand#setSmall private; reproduce its synced flag update. */
    private static void setSmall(ArmorStand stand) {
        byte flags = stand.getEntityData().get(ArmorStand.DATA_CLIENT_FLAGS);
        stand.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, (byte)(flags | ArmorStand.CLIENT_FLAG_SMALL));
    }
}
