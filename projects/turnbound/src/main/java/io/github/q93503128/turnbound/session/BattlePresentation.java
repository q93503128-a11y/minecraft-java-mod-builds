package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.CombatantSide;
import io.github.q93503128.turnbound.combat.CombatantState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class BattlePresentation {
    private final Map<String, UUID> actors = new LinkedHashMap<>();
    private final Map<String, Vec3> homes = new LinkedHashMap<>();
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
            double lane;
            double distance;
            if (combatant.side() == CombatantSide.ALLY) {
                lane = -4.6 + allyIndex * 1.25;
                distance = 4.2;
                allyIndex++;
            } else {
                lane = -3.0 + enemyIndex * 1.5;
                distance = 8.0;
                enemyIndex++;
            }
            Vec3 pos = anchor.add(forward.scale(distance)).add(right.scale(lane));
            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setCustomName(Component.literal(combatant.definition().name()));
            stand.setCustomNameVisible(true);
            stand.setInvulnerable(true);
            stand.setNoGravity(true);
            stand.setShowArms(true);
            stand.setItemSlot(
                    EquipmentSlot.HEAD,
                    combatant.side() == CombatantSide.ALLY
                            ? Items.DIAMOND_HELMET.getDefaultInstance()
                            : Items.NETHERITE_HELMET.getDefaultInstance());
            level.addFreshEntity(stand);
            actors.put(combatant.instanceId(), stand.getUUID());
            homes.put(combatant.instanceId(), pos);
        }
    }

    void lunge(ServerLevel level, String actorId, String targetId) {
        ArmorStand actor = entity(level, actorId);
        Vec3 target = homes.get(targetId);
        if (actor == null || target == null) return;
        Vec3 home = homes.get(actorId);
        Vec3 delta = target.subtract(home);
        if (delta.lengthSqr() > 0.001) {
            actor.setPos(target.subtract(delta.normalize().scale(1.4)));
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

    private static Vec3 horizontal(Vec3 vector) {
        Vec3 horizontal = new Vec3(vector.x, 0, vector.z);
        return horizontal.lengthSqr() < 0.001 ? new Vec3(0, 0, 1) : horizontal.normalize();
    }
}
