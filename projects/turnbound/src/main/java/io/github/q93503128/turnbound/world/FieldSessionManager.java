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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Owns Southgate Meadow A01 Chapter 1 vertical slice: five visible encounters, quest/reward loop, and B01. */
public final class FieldSessionManager {
    /** Compatibility alias used by older alpha tests and handoff notes. */
    public static final String ENCOUNTER_A01_PATROL = SouthgateEncounterCatalog.ENC_M01;
    private static final Map<UUID, FieldSession> SESSIONS = new LinkedHashMap<>();

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
        session.spawnAll(level);
        player.sendSystemMessage(Component.literal("남문 초원 · Chapter 1").withStyle(ChatFormatting.GOLD));
        session.sendStatus(player);
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
        if (BattleSessionManager.exists(player)) return;
        session.ensureBoss(level);
        session.tickEncounters(level, player);
    }

    public static void onBattleEnded(ServerPlayer player, String encounterId, BattleOutcome outcome) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || !(player.level() instanceof ServerLevel level)) return;
        Patrol patrol = session.encounters.get(encounterId);
        if (patrol == null) return;
        patrol.despawn(level);
        if (outcome == BattleOutcome.ALLY_VICTORY) {
            patrol.defeated = true;
            SouthgateChapterProgress.RewardReceipt reward = session.progress.recordVictory(encounterId);
            var spec = SouthgateEncounterCatalog.spec(encounterId);
            player.sendSystemMessage(Component.literal("승리 · " + spec.label()).withStyle(ChatFormatting.GREEN));
            player.sendSystemMessage(Component.literal("보상  XP +" + reward.xp() + " · Gold +" + reward.gold())
                    .withStyle(ChatFormatting.YELLOW));
            if (reward.chapterCleared()) {
                player.sendSystemMessage(Component.literal("Chapter 1 클리어 — 남문 초원 봉쇄 해제").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            } else if (reward.bossUnlocked() && !spec.boss()) {
                player.sendSystemMessage(Component.literal("남문 봉쇄선에서 B01 그라울의 기척이 드러났다.")
                        .withStyle(ChatFormatting.RED));
                session.ensureBoss(level);
            }
            session.sendStatus(player);
        } else {
            patrol.resetAfterNonVictory(level);
        }
    }

    public static boolean interactEntity(ServerPlayer player, Entity target) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || target == null || session.npc == null || !session.npc.equals(target.getUUID())) return false;
        session.sendStatus(player);
        if (session.progress.chapterCleared()) {
            player.sendSystemMessage(Component.literal("정찰관: 남문 길은 확보됐다. 다음 지역으로 진출할 준비를 해.")
                    .withStyle(ChatFormatting.AQUA));
        } else if (session.progress.bossUnlocked()) {
            player.sendSystemMessage(Component.literal("정찰관: 순찰대는 정리됐다. 남쪽 봉쇄선의 그라울을 처리해.")
                    .withStyle(ChatFormatting.AQUA));
        } else {
            player.sendSystemMessage(Component.literal("정찰관: 초원 곳곳의 적 무리를 정리하고 남문 봉쇄선까지 확보해.")
                    .withStyle(ChatFormatting.AQUA));
        }
        return true;
    }

    public static void sendStatus(ServerPlayer player) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            player.sendSystemMessage(Component.literal("활성 TURNBOUND 필드 세션이 없습니다."));
            return;
        }
        session.sendStatus(player);
    }

    public static void remove(ServerPlayer player) {
        FieldSession session = SESSIONS.remove(player.getUUID());
        if (session != null && player.level() instanceof ServerLevel level) session.despawnAll(level);
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) remove(player);
        SESSIONS.clear();
    }

    private static void clearVanillaMobs(ServerLevel level) {
        AABB cell = new AABB(FieldCellA01.ORIGIN_X, FieldCellA01.BASE_Y - 8, FieldCellA01.ORIGIN_Z,
                FieldCellA01.ORIGIN_X + FieldCellA01.SIZE, FieldCellA01.BASE_Y + 24, FieldCellA01.ORIGIN_Z + FieldCellA01.SIZE);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, cell)) mob.discard();
    }

    private static Vec3 local(double x, double y, double z) {
        return new Vec3(FieldCellA01.ORIGIN_X + x, FieldCellA01.BASE_Y + y, FieldCellA01.ORIGIN_Z + z);
    }

    private static List<PatrolLayout> normalLayouts() {
        return List.of(
                new PatrolLayout(SouthgateEncounterCatalog.ENC_M01, local(24.5, 1.0, 35.5), local(43.5, 1.0, 39.5)),
                new PatrolLayout(SouthgateEncounterCatalog.ENC_M02, local(17.0, 1.0, 22.0), local(10.5, 1.0, 34.0)),
                new PatrolLayout(SouthgateEncounterCatalog.ENC_M03, local(47.0, 1.0, 17.5), local(39.0, 1.0, 26.0)),
                new PatrolLayout(SouthgateEncounterCatalog.ENC_M04, local(26.0, 1.0, 13.5), local(38.0, 1.0, 18.5)),
                new PatrolLayout(SouthgateEncounterCatalog.ENC_M05, local(35.0, 1.0, 48.5), local(24.5, 1.0, 49.5))
        );
    }

    private static PatrolLayout bossLayout() {
        return new PatrolLayout(SouthgateEncounterCatalog.B01_GRAUL, local(32.5, 1.0, 53.0), local(39.0, 1.0, 49.0));
    }

    private record PatrolLayout(String encounterId, Vec3 home, Vec3 patrolEnd) {}

    private static final class FieldSession {
        private final FieldCellA01.BuiltCell cell;
        private final SouthgateChapterProgress progress = new SouthgateChapterProgress();
        private final Map<String, Patrol> encounters = new LinkedHashMap<>();
        private UUID npc;

        private FieldSession(FieldCellA01.BuiltCell cell) {
            this.cell = cell;
            for (PatrolLayout layout : normalLayouts()) encounters.put(layout.encounterId(), new Patrol(layout));
        }

        private void spawnAll(ServerLevel level) {
            spawnNpc(level);
            for (Patrol patrol : encounters.values()) patrol.spawn(level);
        }

        private void tickEncounters(ServerLevel level, ServerPlayer player) {
            for (Patrol patrol : encounters.values()) {
                if (patrol.defeated) continue;
                if (patrol.graceTicks > 0) patrol.graceTicks--;
                if (!patrol.entitiesAlive(level)) patrol.spawn(level);
                if (patrol.tick(level, player)) return;
            }
        }

        private void ensureBoss(ServerLevel level) {
            if (!progress.bossUnlocked() || progress.chapterCleared()) return;
            Patrol boss = encounters.get(SouthgateEncounterCatalog.B01_GRAUL);
            if (boss == null) {
                boss = new Patrol(bossLayout());
                encounters.put(SouthgateEncounterCatalog.B01_GRAUL, boss);
                boss.spawn(level);
            } else if (!boss.defeated && !boss.entitiesAlive(level)) {
                boss.spawn(level);
            }
        }

        private void spawnNpc(ServerLevel level) {
            if (npc != null && level.getEntity(npc) != null) return;
            Vec3 pos = cell.entry().add(-3.0, 0.0, 2.0);
            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setInvulnerable(true);
            stand.setNoGravity(true);
            stand.setShowArms(true);
            stand.setCustomName(Component.literal("남문 정찰관").withStyle(ChatFormatting.AQUA));
            stand.setCustomNameVisible(true);
            stand.setItemSlot(EquipmentSlot.HEAD, Items.LEATHER_HELMET.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.MAINHAND, Items.SPYGLASS.getDefaultInstance());
            level.addFreshEntity(stand);
            npc = stand.getUUID();
        }

        private void sendStatus(ServerPlayer player) {
            String objective = progress.chapterCleared()
                    ? "완료"
                    : progress.bossUnlocked() ? "B01 그라울 처치" : "적 무리 정리 " + progress.patrolsCleared() + "/" + progress.patrolGoal();
            player.sendSystemMessage(Component.literal("[Chapter 1] " + objective
                    + " · 누적 XP " + progress.earnedXp() + " · Gold " + progress.earnedGold())
                    .withStyle(ChatFormatting.GRAY));
        }

        private void despawnAll(ServerLevel level) {
            for (Patrol patrol : encounters.values()) patrol.despawn(level);
            if (npc != null) {
                Entity entity = level.getEntity(npc);
                if (entity != null) entity.discard();
                npc = null;
            }
        }
    }

    private static final class Patrol {
        private final PatrolLayout layout;
        private final SouthgateEncounterCatalog.EncounterSpec spec;
        private final List<UUID> actors = new ArrayList<>();
        private Vec3 pivot;
        private boolean patrolTowardEnd = true;
        private boolean defeated;
        private int graceTicks = 30;
        private FieldEncounterRules.Phase phase = FieldEncounterRules.Phase.PATROL;

        private Patrol(PatrolLayout layout) {
            this.layout = layout;
            this.spec = SouthgateEncounterCatalog.spec(layout.encounterId());
            this.pivot = layout.home();
        }

        /** @return true if this patrol opened a battle this tick. */
        private boolean tick(ServerLevel level, ServerPlayer player) {
            Vec3 playerFlat = new Vec3(player.getX(), pivot.y, player.getZ());
            double distance = playerFlat.distanceTo(pivot);
            phase = FieldEncounterRules.nextPhase(phase, distance, graceTicks);
            if (FieldEncounterRules.shouldEngage(distance, graceTicks)) {
                despawn(level);
                BattleSessionManager.startEncounter(player, spec.id());
                return true;
            }
            Vec3 target;
            double speed;
            if (phase == FieldEncounterRules.Phase.ALERT) {
                target = playerFlat;
                speed = spec.boss() ? 0.115 : 0.105;
            } else {
                target = patrolTowardEnd ? layout.patrolEnd() : layout.home();
                speed = spec.boss() ? 0.025 : 0.035;
                if (pivot.distanceTo(target) < 0.6) patrolTowardEnd = !patrolTowardEnd;
            }
            Vec3 delta = target.subtract(pivot);
            if (delta.lengthSqr() > 0.0001) {
                Vec3 step = delta.normalize().scale(Math.min(speed, Math.sqrt(delta.lengthSqr())));
                pivot = new Vec3(pivot.x + step.x, pivot.y, pivot.z + step.z);
            }
            updateActors(level, delta);
            return false;
        }

        private void spawn(ServerLevel level) {
            if (defeated) return;
            despawn(level);
            List<String> defs = spec.enemyDefinitionIds();
            for (int i = 0; i < defs.size(); i++) {
                Vec3 pos = formationPosition(i, new Vec3(0, 0, -1));
                ArmorStand stand = createActor(level, pos, defs.get(i));
                level.addFreshEntity(stand);
                actors.add(stand.getUUID());
            }
        }

        private ArmorStand createActor(ServerLevel level, Vec3 pos, String defId) {
            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setInvulnerable(true);
            stand.setNoGravity(true);
            stand.setShowArms(true);
            stand.setCustomName(Component.literal(SouthgateEncounterCatalog.enemyDefinition(defId).name()));
            stand.setCustomNameVisible(false);
            stand.setYRot(180.0F);
            switch (defId) {
                case "E001" -> {
                    stand.setItemSlot(EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.LEGS, Items.CHAINMAIL_LEGGINGS.getDefaultInstance());
                }
                case "E002" -> {
                    stand.setItemSlot(EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
                }
                case "E003" -> {
                    stand.setItemSlot(EquipmentSlot.HEAD, Items.LEATHER_HELMET.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_SWORD.getDefaultInstance());
                }
                case "E004" -> {
                    stand.setItemSlot(EquipmentSlot.HEAD, Items.IRON_HELMET.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.CHEST, Items.IRON_CHESTPLATE.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.OFFHAND, Items.SHIELD.getDefaultInstance());
                }
                case "E005" -> {
                    stand.setItemSlot(EquipmentSlot.HEAD, Items.LEATHER_HELMET.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.MAINHAND, Items.SHEARS.getDefaultInstance());
                }
                case "B01" -> {
                    stand.setItemSlot(EquipmentSlot.HEAD, Items.IRON_HELMET.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.CHEST, Items.IRON_CHESTPLATE.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.LEGS, Items.IRON_LEGGINGS.getDefaultInstance());
                    stand.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_AXE.getDefaultInstance());
                }
                default -> { }
            }
            return stand;
        }

        private void updateActors(ServerLevel level, Vec3 heading) {
            if (actors.size() != spec.enemyDefinitionIds().size()) return;
            double yaw = heading.lengthSqr() < 0.0001 ? 180.0 : Math.toDegrees(Math.atan2(-heading.x, heading.z));
            Vec3 forward = heading.lengthSqr() < 0.0001 ? new Vec3(0, 0, -1) : new Vec3(heading.x, 0, heading.z).normalize();
            for (int i = 0; i < actors.size(); i++) {
                Entity entity = level.getEntity(actors.get(i));
                if (entity instanceof ArmorStand stand) {
                    Vec3 pos = formationPosition(i, forward);
                    stand.setPos(pos.x, pos.y, pos.z);
                    stand.setYRot((float) yaw);
                    stand.setCustomNameVisible(i == 0 && phase == FieldEncounterRules.Phase.ALERT);
                    if (i == 0 && phase == FieldEncounterRules.Phase.ALERT) stand.setCustomName(Component.literal("!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    else stand.setCustomName(Component.literal(SouthgateEncounterCatalog.enemyDefinition(spec.enemyDefinitionIds().get(i)).name()));
                }
            }
        }

        private Vec3 formationPosition(int index, Vec3 forward) {
            Vec3 right = new Vec3(-forward.z, 0, forward.x);
            return switch (index) {
                case 0 -> pivot;
                case 1 -> pivot.subtract(forward.scale(1.25)).subtract(right.scale(1.35));
                case 2 -> pivot.subtract(forward.scale(1.25)).add(right.scale(1.35));
                case 3 -> pivot.subtract(forward.scale(2.45));
                default -> pivot.subtract(forward.scale(2.45)).add(right.scale((index - 3) * 1.35));
            };
        }

        private boolean entitiesAlive(ServerLevel level) {
            if (actors.size() != spec.enemyDefinitionIds().size()) return false;
            for (UUID id : actors) if (level.getEntity(id) == null) return false;
            return true;
        }

        private void resetAfterNonVictory(ServerLevel level) {
            defeated = false;
            graceTicks = 100;
            phase = FieldEncounterRules.Phase.PATROL;
            pivot = layout.home();
            patrolTowardEnd = true;
            spawn(level);
        }

        private void despawn(ServerLevel level) {
            for (UUID id : actors) {
                Entity entity = level.getEntity(id);
                if (entity != null) entity.discard();
            }
            actors.clear();
        }
    }
}
