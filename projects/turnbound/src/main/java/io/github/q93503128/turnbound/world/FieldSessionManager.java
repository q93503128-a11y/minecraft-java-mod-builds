package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Automatic alpha.16 starter field session: visible patrols with deterministic leash/return behavior. */
public final class FieldSessionManager {
    public static final String ENCOUNTER_A01_PATROL = SouthgateEncounterCatalog.ENC_M01;
    private static final Map<UUID, FieldSession> SESSIONS = new LinkedHashMap<>();

    private FieldSessionManager() {}

    /** Called from the player tick: no command is required on a normal Overworld/Superflat start. */
    public static void ensureAutomatic(ServerPlayer player) {
        if (SESSIONS.containsKey(player.getUUID()) || BattleSessionManager.exists(player)) return;
        if (player.level().dimension() != Level.OVERWORLD || player.tickCount < 40) return;
        enter(player);
    }

    /** Developer fallback; normal gameplay enters automatically. */
    public static boolean enter(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return false;
        BattleSessionManager.end(player);
        remove(player);
        ServerLevel level = (ServerLevel) player.level();
        StarterSliceWorld.BuiltSlice slice = StarterSliceWorld.build(level);
        FieldSession session = new FieldSession(slice);
        SESSIONS.put(player.getUUID(), session);
        player.setPos(slice.spawn().x, slice.spawn().y, slice.spawn().z);
        player.setYRot(180.0F);
        player.setXRot(3.0F);
        player.setDeltaMovement(Vec3.ZERO);
        session.spawnAll(level);
        player.sendSystemMessage(Component.literal("TURNBOUND · 남문 마을").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("마을 남문을 지나면 첫 필드 조우가 시작됩니다. 명령어는 필요하지 않습니다.").withStyle(ChatFormatting.GRAY));
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
        return true;
    }

    public static boolean active(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID()) && player.level().dimension() == Level.OVERWORLD;
    }

    public static void tick(ServerPlayer player) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || player.level().dimension() != Level.OVERWORLD || BattleSessionManager.exists(player)) return;
        ServerLevel level = (ServerLevel) player.level();
        if (!StarterSliceWorld.contains(session.slice, player.position())) {
            Vec3 spawn = session.slice.spawn();
            player.setPos(spawn.x, spawn.y, spawn.z);
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (player.tickCount % 20 == 0) clearVanillaMobs(level, session.slice);
        session.tickPatrols(level, player);
    }

    public static void onBattleEnded(ServerPlayer player, String encounterId, BattleOutcome outcome) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || !(player.level() instanceof ServerLevel level)) return;
        Patrol patrol = session.encounters.get(encounterId);
        if (patrol == null) return;
        patrol.despawn(level);
        if (outcome != BattleOutcome.ALLY_VICTORY) {
            patrol.reset(level);
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
            return;
        }
        boolean first = session.cleared.add(encounterId);
        var spec = SouthgateEncounterCatalog.spec(encounterId);
        int xp = first ? spec.rewardXp() : 0;
        int gold = first ? spec.rewardGold() : 0;
        session.earnedXp += xp;
        session.earnedGold += gold;
        patrol.defeated = true;
        boolean starterDone = session.starterComplete();
        FieldUiSnapshot.Reward reward = new FieldUiSnapshot.Reward(spec.label(), xp, gold, first, false);
        player.sendSystemMessage(Component.literal("승리 · " + spec.label()).withStyle(ChatFormatting.GREEN));
        if (starterDone) player.sendSystemMessage(Component.literal("남문 초원 체험 구간 확보 — 다음 필드는 이후 제작 단계에서 연결됩니다.").withStyle(ChatFormatting.AQUA));
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.RESULT, reward));
    }

    public static boolean interactEntity(ServerPlayer player, Entity target) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || target == null) return false;
        UUID id = target.getUUID();
        if (id.equals(session.npc)) {
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
            return true;
        }
        if (id.equals(session.relay)) {
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.TRAVEL, null));
            return true;
        }
        return false;
    }

    public static void sendStatus(ServerPlayer player) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
    }

    public static void command(ServerPlayer player, String command) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || command == null || BattleSessionManager.exists(player)) return;
        String[] parts = command.split("\\|", -1);
        if (parts.length >= 2 && "TRAVEL".equals(parts[0])) {
            Vec3 spawn = session.slice.spawn();
            player.setPos(spawn.x, spawn.y, spawn.z);
            player.setYRot(180.0F);
            player.setXRot(3.0F);
            player.setDeltaMovement(Vec3.ZERO);
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
        }
    }

    public static void remove(ServerPlayer player) {
        FieldSession session = SESSIONS.remove(player.getUUID());
        if (session != null && player.level() instanceof ServerLevel level) session.despawnAll(level);
        FieldNetwork.close(player);
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) remove(player);
        SESSIONS.clear();
    }

    private static void clearVanillaMobs(ServerLevel level, StarterSliceWorld.BuiltSlice slice) {
        AABB area = new AABB(StarterSliceWorld.ORIGIN_X - 4, slice.baseY() - 6, StarterSliceWorld.VILLAGE_Z - 4,
                StarterSliceWorld.ORIGIN_X + StarterSliceWorld.SIZE + 4, slice.baseY() + 20,
                StarterSliceWorld.FIELD_Z + StarterSliceWorld.SIZE + 4);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) mob.discard();
    }

    private static final class FieldSession {
        private final StarterSliceWorld.BuiltSlice slice;
        private final Map<String, Patrol> encounters = new LinkedHashMap<>();
        private final Set<String> cleared = new HashSet<>();
        private UUID npc;
        private UUID relay;
        private int earnedXp;
        private int earnedGold;

        private FieldSession(StarterSliceWorld.BuiltSlice slice) {
            this.slice = slice;
            encounters.put(SouthgateEncounterCatalog.ENC_M01,
                    new Patrol(SouthgateEncounterCatalog.ENC_M01, slice.m01Home(), slice.m01End()));
            encounters.put(SouthgateEncounterCatalog.ENC_M02,
                    new Patrol(SouthgateEncounterCatalog.ENC_M02, slice.m02Home(), slice.m02End()));
        }

        private void spawnAll(ServerLevel level) {
            spawnNpc(level);
            spawnRelay(level);
            for (Patrol patrol : encounters.values()) patrol.spawn(level);
        }

        private void tickPatrols(ServerLevel level, ServerPlayer player) {
            for (Patrol patrol : encounters.values()) {
                if (patrol.defeated) continue;
                if (patrol.graceTicks > 0) patrol.graceTicks--;
                if (!patrol.entitiesAlive(level)) patrol.spawn(level);
                if (patrol.tick(level, player)) return;
            }
        }

        private boolean starterComplete() {
            return cleared.contains(SouthgateEncounterCatalog.ENC_M01) && cleared.contains(SouthgateEncounterCatalog.ENC_M02);
        }

        private void spawnNpc(ServerLevel level) {
            Vec3 pos = slice.npc();
            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setInvulnerable(true); stand.setNoGravity(true); stand.setShowArms(true);
            stand.setCustomName(Component.literal("남문 정찰관").withStyle(ChatFormatting.AQUA));
            stand.setCustomNameVisible(true);
            stand.setItemSlot(EquipmentSlot.HEAD, Items.LEATHER_HELMET.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.MAINHAND, Items.SPYGLASS.getDefaultInstance());
            level.addFreshEntity(stand); npc = stand.getUUID();
        }

        private void spawnRelay(ServerLevel level) {
            Vec3 pos = slice.relay();
            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setInvulnerable(true); stand.setNoGravity(true); stand.setShowArms(true);
            stand.setCustomName(Component.literal("남문 마을 계전석").withStyle(ChatFormatting.LIGHT_PURPLE));
            stand.setCustomNameVisible(true);
            stand.setItemSlot(EquipmentSlot.HEAD, Items.AMETHYST_SHARD.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.MAINHAND, Items.COMPASS.getDefaultInstance());
            level.addFreshEntity(stand); relay = stand.getUUID();
        }

        private FieldUiSnapshot snapshot(ServerPlayer player, FieldUiSnapshot.Mode mode, FieldUiSnapshot.Reward reward) {
            List<FieldUiSnapshot.Encounter> views = List.of(
                    encounterView(SouthgateEncounterCatalog.ENC_M01),
                    encounterView(SouthgateEncounterCatalog.ENC_M02));
            boolean current = player.position().distanceToSqr(slice.spawn()) <= 144.0;
            List<FieldUiSnapshot.Travel> travels = List.of(new FieldUiSnapshot.Travel("START_VILLAGE", "남문 마을", true, current));
            return new FieldUiSnapshot(true, mode, cleared.size(), 2, false, false, earnedXp, earnedGold,
                    objective(), dialogue(), reward == null ? FieldUiSnapshot.Reward.none() : reward, views, travels);
        }

        private FieldUiSnapshot.Encounter encounterView(String id) {
            var spec = SouthgateEncounterCatalog.spec(id);
            return new FieldUiSnapshot.Encounter(id, spec.label(), cleared.contains(id), true, false);
        }

        private String objective() {
            if (starterComplete()) return "남문 초원 1구역 확보 완료 · 마을로 돌아가 정찰관에게 보고";
            return "남문을 지나 보이는 적 무리 2개를 정리하십시오.  " + cleared.size() + "/2";
        }

        private String dialogue() {
            if (starterComplete()) return "첫 길은 확보됐어. 마을은 안전하다. 다음 지역은 이 구간의 전투·UI가 안정된 뒤 이어서 열겠다.";
            return "마을 안에는 적이 없어. 남문 밖 초원에 보이는 두 무리만 정리해 줘. 피해서 지나가는 것도 가능해.";
        }

        private void despawnAll(ServerLevel level) {
            for (Patrol patrol : encounters.values()) patrol.despawn(level);
            despawn(level, npc); despawn(level, relay);
            npc = null; relay = null;
        }
        private void despawn(ServerLevel level, UUID id) { if (id != null) { Entity e = level.getEntity(id); if (e != null) e.discard(); } }
    }

    private static final class Patrol {
        private final String encounterId;
        private final SouthgateEncounterCatalog.EncounterSpec spec;
        private final Vec3 home;
        private final Vec3 patrolEnd;
        private final List<UUID> actors = new ArrayList<>();
        private Vec3 pivot;
        private Vec3 facing = new Vec3(0, 0, -1);
        private boolean towardEnd = true;
        private boolean defeated;
        private int graceTicks = 40;
        private FieldEncounterRules.Phase phase = FieldEncounterRules.Phase.PATROL;

        private Patrol(String encounterId, Vec3 home, Vec3 patrolEnd) {
            this.encounterId = encounterId;
            this.spec = SouthgateEncounterCatalog.spec(encounterId);
            this.home = home;
            this.patrolEnd = patrolEnd;
            this.pivot = home;
        }

        private boolean tick(ServerLevel level, ServerPlayer player) {
            Vec3 playerFlat = new Vec3(player.getX(), pivot.y, player.getZ());
            double playerDistance = playerFlat.distanceTo(pivot);
            double homeDistance = pivot.distanceTo(home);
            FieldEncounterRules.Phase previous = phase;
            phase = FieldEncounterRules.nextPhase(phase, playerDistance, homeDistance, graceTicks);
            if (previous == FieldEncounterRules.Phase.RETURN && phase == FieldEncounterRules.Phase.PATROL) {
                pivot = home;
                towardEnd = true;
                graceTicks = Math.max(graceTicks, FieldEncounterRules.RETURN_REAGGRO_GRACE_TICKS);
            }
            if (FieldEncounterRules.shouldEngage(phase, playerDistance, graceTicks)) {
                despawn(level);
                BattleSessionManager.startEncounter(player, encounterId, false, false);
                return true;
            }

            Vec3 target = switch (phase) {
                case ALERT -> playerFlat;
                case RETURN -> home;
                case PATROL -> towardEnd ? patrolEnd : home;
            };
            double speed = switch (phase) {
                case ALERT -> 0.105;
                case RETURN -> 0.075;
                case PATROL -> 0.035;
            };
            Vec3 delta = target.subtract(pivot);
            if (phase == FieldEncounterRules.Phase.PATROL && delta.lengthSqr() < 0.36) {
                towardEnd = !towardEnd;
            } else if (delta.lengthSqr() > 0.0001) {
                Vec3 movement = delta.normalize().scale(Math.min(speed, delta.length()));
                pivot = pivot.add(movement);
                facing = horizontalDirection(movement, facing);
            }
            updateActors(level, facing);
            return false;
        }

        private void spawn(ServerLevel level) {
            if (defeated) return;
            despawn(level);
            for (int i = 0; i < spec.enemyDefinitionIds().size(); i++) {
                Vec3 pos = formation(i, facing);
                ArmorStand stand = actor(level, pos, spec.enemyDefinitionIds().get(i));
                level.addFreshEntity(stand);
                actors.add(stand.getUUID());
            }
            updateActors(level, facing);
        }

        private ArmorStand actor(ServerLevel level, Vec3 pos, String defId) {
            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setInvulnerable(true); stand.setNoGravity(true); stand.setShowArms(true);
            stand.setCustomName(Component.literal(SouthgateEncounterCatalog.enemyDefinition(defId).name()));
            stand.setCustomNameVisible(false);
            stand.setItemSlot(EquipmentSlot.CHEST, Items.IRON_CHESTPLATE.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS.getDefaultInstance());
            if ("E002".equals(defId)) stand.setItemSlot(EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
            else stand.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_SWORD.getDefaultInstance());
            return stand;
        }

        private void updateActors(ServerLevel level, Vec3 heading) {
            facing = horizontalDirection(heading, facing);
            float targetYaw = yawFor(facing);
            for (int i = 0; i < actors.size(); i++) {
                Entity e = level.getEntity(actors.get(i));
                if (!(e instanceof ArmorStand stand)) continue;
                Vec3 p = formation(i, facing);
                stand.setPos(p.x, p.y, p.z);
                float yaw = smoothYaw(stand.getYRot(), targetYaw, 0.35F);
                stand.setYRot(yaw);
                stand.setYHeadRot(yaw);
                stand.setCustomNameVisible(i == 0 && phase == FieldEncounterRules.Phase.ALERT);
                if (i == 0 && phase == FieldEncounterRules.Phase.ALERT) stand.setCustomName(Component.literal("!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                else stand.setCustomName(Component.literal(SouthgateEncounterCatalog.enemyDefinition(spec.enemyDefinitionIds().get(i)).name()));
            }
        }

        private Vec3 formation(int index, Vec3 forward) {
            Vec3 right = new Vec3(-forward.z, 0, forward.x);
            return index == 0 ? pivot : pivot.subtract(forward.scale(1.2)).add(right.scale(index % 2 == 0 ? 1.25 : -1.25));
        }

        private boolean entitiesAlive(ServerLevel level) {
            if (actors.size() != spec.enemyDefinitionIds().size()) return false;
            for (UUID id : actors) if (level.getEntity(id) == null) return false;
            return true;
        }

        private void reset(ServerLevel level) {
            defeated = false;
            graceTicks = 100;
            phase = FieldEncounterRules.Phase.PATROL;
            pivot = home;
            facing = new Vec3(0, 0, -1);
            towardEnd = true;
            spawn(level);
        }

        private void despawn(ServerLevel level) {
            for (UUID id : actors) {
                Entity e = level.getEntity(id);
                if (e != null) e.discard();
            }
            actors.clear();
        }

        private static Vec3 horizontalDirection(Vec3 candidate, Vec3 fallback) {
            Vec3 flat = new Vec3(candidate.x, 0, candidate.z);
            return flat.lengthSqr() > 0.000001 ? flat.normalize() : fallback;
        }

        private static float yawFor(Vec3 forward) {
            return (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        }

        private static float smoothYaw(float current, float target, float factor) {
            float delta = wrapDegrees(target - current);
            return current + delta * factor;
        }

        private static float wrapDegrees(float degrees) {
            float wrapped = degrees % 360.0F;
            if (wrapped >= 180.0F) wrapped -= 360.0F;
            if (wrapped < -180.0F) wrapped += 360.0F;
            return wrapped;
        }
    }
}
