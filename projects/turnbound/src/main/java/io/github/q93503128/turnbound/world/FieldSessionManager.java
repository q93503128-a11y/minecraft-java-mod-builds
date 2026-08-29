package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Owns Southgate Meadow A01 and its first visible encounter party. */
public final class FieldSessionManager {
    public static final String ENCOUNTER_A01_PATROL = "southgate_a01_e001_e002_e005";
    private static final Map<UUID, FieldSession> SESSIONS = new HashMap<>();

    private FieldSessionManager() {}

    public static boolean enter(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) {
            player.sendSystemMessage(Component.literal("남문 초원은 오버월드에서 진입해야 합니다."));
            return false;
        }
        BattleSessionManager.end(player);
        remove(player);
        ServerLevel level = (ServerLevel) player.level();
        FieldCellA01.BuiltCell cell = FieldCellA01.build(level);
        FieldSession session = new FieldSession(cell);
        SESSIONS.put(player.getUUID(), session);
        player.setPos(cell.entry().x, cell.entry().y, cell.entry().z);
        player.setYRot(180.0F);
        player.setXRot(5.0F);
        player.setDeltaMovement(Vec3.ZERO);
        session.spawn(level);
        player.sendSystemMessage(Component.literal("남문 초원").withStyle(ChatFormatting.GOLD));
        return true;
    }

    public static boolean active(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID()) && player.level().dimension() == Level.OVERWORLD;
    }

    public static void tick(ServerPlayer player) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || player.level().dimension() != Level.OVERWORLD) return;
        ServerLevel level = (ServerLevel) player.level();
        if (!FieldCellA01.containsXZ(player.getX(), player.getZ())) {
            if (!BattleSessionManager.exists(player)) {
                Vec3 entry = session.cell.entry();
                player.setPos(entry.x, entry.y, entry.z);
                player.setDeltaMovement(Vec3.ZERO);
            }
            return;
        }
        if (player.tickCount % 20 == 0) clearVanillaMobs(level);
        if (BattleSessionManager.exists(player) || session.defeated) return;
        if (session.graceTicks > 0) session.graceTicks--;
        if (!session.entitiesAlive(level)) session.spawn(level);
        session.tickPatrol(level, player);
    }

    private static void clearVanillaMobs(ServerLevel level) {
        AABB cell = new AABB(FieldCellA01.ORIGIN_X, FieldCellA01.BASE_Y - 8, FieldCellA01.ORIGIN_Z,
                FieldCellA01.ORIGIN_X + FieldCellA01.SIZE, FieldCellA01.BASE_Y + 24, FieldCellA01.ORIGIN_Z + FieldCellA01.SIZE);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, cell)) mob.discard();
    }

    public static void onBattleEnded(ServerPlayer player, String encounterId, BattleOutcome outcome) {
        if (!ENCOUNTER_A01_PATROL.equals(encounterId)) return;
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || !(player.level() instanceof ServerLevel level)) return;
        session.despawn(level);
        if (outcome == BattleOutcome.ALLY_VICTORY) {
            session.defeated = true;
        } else {
            session.defeated = false;
            session.graceTicks = 100;
            session.phase = FieldEncounterRules.Phase.PATROL;
            session.pivot = session.cell.encounterHome();
            session.spawn(level);
        }
    }

    public static void remove(ServerPlayer player) {
        FieldSession session = SESSIONS.remove(player.getUUID());
        if (session != null && player.level() instanceof ServerLevel level) session.despawn(level);
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) remove(player);
        SESSIONS.clear();
    }

    private static final class FieldSession {
        private final FieldCellA01.BuiltCell cell;
        private final List<UUID> actors = new ArrayList<>();
        private Vec3 pivot;
        private boolean patrolTowardEnd = true;
        private boolean defeated;
        private int graceTicks = 30;
        private FieldEncounterRules.Phase phase = FieldEncounterRules.Phase.PATROL;

        private FieldSession(FieldCellA01.BuiltCell cell) {
            this.cell = cell;
            this.pivot = cell.encounterHome();
        }

        private void tickPatrol(ServerLevel level, ServerPlayer player) {
            Vec3 playerFlat = new Vec3(player.getX(), pivot.y, player.getZ());
            double distance = playerFlat.distanceTo(pivot);
            phase = FieldEncounterRules.nextPhase(phase, distance, graceTicks);
            if (FieldEncounterRules.shouldEngage(distance, graceTicks)) {
                despawn(level);
                BattleSessionManager.startEncounter(player, ENCOUNTER_A01_PATROL);
                return;
            }
            Vec3 target;
            double speed;
            if (phase == FieldEncounterRules.Phase.ALERT) {
                target = playerFlat;
                speed = 0.105;
            } else {
                target = patrolTowardEnd ? cell.encounterPatrolEnd() : cell.encounterHome();
                speed = 0.035;
                if (pivot.distanceTo(target) < 0.6) patrolTowardEnd = !patrolTowardEnd;
            }
            Vec3 delta = target.subtract(pivot);
            if (delta.lengthSqr() > 0.0001) {
                Vec3 step = delta.normalize().scale(Math.min(speed, Math.sqrt(delta.lengthSqr())));
                pivot = new Vec3(pivot.x + step.x, pivot.y, pivot.z + step.z);
            }
            updateActors(level, delta);
        }

        private void spawn(ServerLevel level) {
            despawn(level);
            ArmorStand walker = createActor(level, pivot, 0);
            ArmorStand archer = createActor(level, pivot.add(-1.35, 0, -1.25), 1);
            ArmorStand medic = createActor(level, pivot.add(1.35, 0, -1.25), 2);
            level.addFreshEntity(walker);
            level.addFreshEntity(archer);
            level.addFreshEntity(medic);
            actors.add(walker.getUUID());
            actors.add(archer.getUUID());
            actors.add(medic.getUUID());
        }

        private ArmorStand createActor(ServerLevel level, Vec3 pos, int role) {
            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setInvulnerable(true);
            stand.setNoGravity(true);
            stand.setShowArms(true);
            stand.setCustomName(Component.literal(switch (role) {
                case 0 -> "부패 보행자";
                case 1 -> "뼈 사수";
                default -> "야전 치유사";
            }));
            stand.setCustomNameVisible(false);
            stand.setYRot(180.0F);
            switch (role) {
                case 0 -> {
                    stand.setItemSlot(EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.LEGS, Items.CHAINMAIL_LEGGINGS.getDefaultInstance());
                }
                case 1 -> {
                    stand.setItemSlot(EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
                }
                default -> {
                    stand.setItemSlot(EquipmentSlot.HEAD, Items.LEATHER_HELMET.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.MAINHAND, Items.SHEARS.getDefaultInstance());
                }
            }
            return stand;
        }

        private void updateActors(ServerLevel level, Vec3 heading) {
            if (actors.size() != 3) return;
            double yaw = heading.lengthSqr() < 0.0001 ? 180.0 : Math.toDegrees(Math.atan2(-heading.x, heading.z));
            Vec3 forward = heading.lengthSqr() < 0.0001 ? new Vec3(0, 0, -1) : new Vec3(heading.x, 0, heading.z).normalize();
            Vec3 right = new Vec3(-forward.z, 0, forward.x);
            Vec3[] positions = {pivot, pivot.subtract(forward.scale(1.25)).subtract(right.scale(1.35)), pivot.subtract(forward.scale(1.25)).add(right.scale(1.35))};
            for (int i = 0; i < actors.size(); i++) {
                var entity = level.getEntity(actors.get(i));
                if (entity instanceof ArmorStand stand) {
                    stand.setPos(positions[i].x, positions[i].y, positions[i].z);
                    stand.setYRot((float) yaw);
                    stand.setCustomNameVisible(i == 0 && phase == FieldEncounterRules.Phase.ALERT);
                    if (i == 0 && phase == FieldEncounterRules.Phase.ALERT) stand.setCustomName(Component.literal("!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    else if (i == 0) stand.setCustomName(Component.literal("부패 보행자"));
                }
            }
        }

        private boolean entitiesAlive(ServerLevel level) {
            if (actors.size() != 3) return false;
            for (UUID id : actors) if (level.getEntity(id) == null) return false;
            return true;
        }

        private void despawn(ServerLevel level) {
            for (UUID id : actors) {
                var entity = level.getEntity(id);
                if (entity != null) entity.discard();
            }
            actors.clear();
        }
    }
}
