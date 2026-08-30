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

/** Owns Southgate Meadow Chapter 1 and its first clear-gated South Road continuation. */
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
        FieldCellA01.BuiltCell a01 = FieldCellA01.build(level);
        FieldCellA02.BuiltCell a02 = FieldCellA02.build(level);
        FieldSession session = new FieldSession(a01, a02);
        SESSIONS.put(player.getUUID(), session);
        player.setPos(a01.entry().x, a01.entry().y, a01.entry().z);
        player.setYRot(180.0F);
        player.setXRot(5.0F);
        player.setDeltaMovement(Vec3.ZERO);
        session.spawnAll(level);
        player.sendSystemMessage(Component.literal("남문 초원 · Chapter 1").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("정찰관을 조사하면 임무 현황을 확인할 수 있습니다.").withStyle(ChatFormatting.GRAY));
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
        return true;
    }

    public static boolean active(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID()) && player.level().dimension() == Level.OVERWORLD;
    }

    public static void tick(ServerPlayer player) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || player.level().dimension() != Level.OVERWORLD) return;
        if (BattleSessionManager.exists(player)) return;
        ServerLevel level = (ServerLevel) player.level();

        if (!session.allowedPosition(player.position())) {
            Vec3 fallback = player.getZ() >= FieldCellA02.ORIGIN_Z && session.progress.chapterCleared()
                    ? session.a02.entry()
                    : session.a01.entry();
            player.setPos(fallback.x, fallback.y, fallback.z);
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (player.tickCount % 20 == 0) clearVanillaMobs(level);
        if (session.progress.chapterCleared()) {
            FieldCellA02.unlockNorthGate(level);
            session.spawnA02Relay(level);
        }
        session.ensureBoss(level);
        if (FieldCellA01.containsXZ(player.getX(), player.getZ())) session.tickEncounters(level, player);
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
            if (reward.chapterCleared()) {
                FieldCellA02.unlockNorthGate(level);
                session.spawnA02Relay(level);
                player.sendSystemMessage(Component.literal("Chapter 1 클리어 — 남부 도로가 열렸습니다.")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            } else if (reward.bossUnlocked() && !spec.boss()) {
                player.sendSystemMessage(Component.literal("남문 봉쇄선에서 B01 그라울이 출현했습니다.")
                        .withStyle(ChatFormatting.RED));
                session.ensureBoss(level);
            }
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.RESULT, reward));
        } else {
            patrol.resetAfterNonVictory(level);
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
        }
    }

    public static boolean interactEntity(ServerPlayer player, Entity target) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || target == null) return false;
        UUID id = target.getUUID();
        if (id.equals(session.npc)) {
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
            return true;
        }
        if (id.equals(session.relayA01)) {
            boolean newlyActivated = session.progress.activateRelay(FieldTravelCatalog.RELAY_A01);
            if (newlyActivated) player.sendSystemMessage(Component.literal("남문 초원 계전석이 활성화되었습니다.").withStyle(ChatFormatting.AQUA));
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.TRAVEL, null));
            return true;
        }
        if (id.equals(session.relayA02)) {
            if (!session.progress.chapterCleared()) return true;
            boolean newlyActivated = session.progress.activateRelay(FieldTravelCatalog.RELAY_A02);
            if (newlyActivated) player.sendSystemMessage(Component.literal("남부 도로 거점 계전석이 활성화되었습니다.").withStyle(ChatFormatting.AQUA));
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.TRAVEL, null));
            return true;
        }
        return false;
    }

    public static void sendStatus(ServerPlayer player) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            player.sendSystemMessage(Component.literal("활성 TURNBOUND 필드 세션이 없습니다."));
            return;
        }
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
    }

    public static void command(ServerPlayer player, String command) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || command == null || command.isBlank() || BattleSessionManager.exists(player)) return;
        String[] parts = command.split("\\|", -1);
        if (parts.length >= 2 && "TRAVEL".equals(parts[0])) {
            session.travel(player, parts[1]);
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

    private static void clearVanillaMobs(ServerLevel level) {
        AABB cells = new AABB(FieldCellA01.ORIGIN_X, FieldCellA01.BASE_Y - 8, FieldCellA01.ORIGIN_Z,
                FieldCellA01.ORIGIN_X + FieldCellA01.SIZE, FieldCellA02.BASE_Y + 26, FieldCellA02.ORIGIN_Z + FieldCellA02.SIZE);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, cells)) mob.discard();
    }

    private static Vec3 a01(double x, double y, double z) {
        return new Vec3(FieldCellA01.ORIGIN_X + x, FieldCellA01.BASE_Y + y, FieldCellA01.ORIGIN_Z + z);
    }

    private static List<PatrolLayout> normalLayouts() {
        return List.of(
                new PatrolLayout(SouthgateEncounterCatalog.ENC_M01, a01(24.5, 1.0, 35.5), a01(43.5, 1.0, 39.5)),
                new PatrolLayout(SouthgateEncounterCatalog.ENC_M02, a01(17.0, 1.0, 22.0), a01(10.5, 1.0, 34.0)),
                new PatrolLayout(SouthgateEncounterCatalog.ENC_M03, a01(47.0, 1.0, 17.5), a01(39.0, 1.0, 26.0)),
                new PatrolLayout(SouthgateEncounterCatalog.ENC_M04, a01(26.0, 1.0, 13.5), a01(38.0, 1.0, 18.5)),
                new PatrolLayout(SouthgateEncounterCatalog.ENC_M05, a01(35.0, 1.0, 48.5), a01(24.5, 1.0, 49.5))
        );
    }

    private static PatrolLayout bossLayout() {
        return new PatrolLayout(SouthgateEncounterCatalog.B01_GRAUL, a01(32.5, 1.0, 53.0), a01(39.0, 1.0, 49.0));
    }

    private record PatrolLayout(String encounterId, Vec3 home, Vec3 patrolEnd) {}

    private static final class FieldSession {
        private final FieldCellA01.BuiltCell a01;
        private final FieldCellA02.BuiltCell a02;
        private final SouthgateChapterProgress progress = new SouthgateChapterProgress();
        private final Map<String, Patrol> encounters = new LinkedHashMap<>();
        private UUID npc;
        private UUID relayA01;
        private UUID relayA02;

        private FieldSession(FieldCellA01.BuiltCell a01, FieldCellA02.BuiltCell a02) {
            this.a01 = a01;
            this.a02 = a02;
            for (PatrolLayout layout : normalLayouts()) encounters.put(layout.encounterId(), new Patrol(layout));
        }

        private boolean allowedPosition(Vec3 position) {
            if (FieldCellA01.containsXZ(position.x, position.z)) return true;
            return progress.chapterCleared() && FieldCellA02.containsXZ(position.x, position.z);
        }

        private void spawnAll(ServerLevel level) {
            spawnNpc(level);
            spawnA01Relay(level);
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
            Vec3 pos = a01.entry().add(-3.0, 0.0, 2.0);
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

        private void spawnA01Relay(ServerLevel level) {
            if (relayA01 != null && level.getEntity(relayA01) != null) return;
            relayA01 = spawnRelay(level, a01(50.0, 3.0, 31.0), "남문 초원 계전석", ChatFormatting.AQUA);
        }

        private void spawnA02Relay(ServerLevel level) {
            if (!progress.chapterCleared()) return;
            if (relayA02 != null && level.getEntity(relayA02) != null) return;
            relayA02 = spawnRelay(level, a02.relay(), "남부 도로 거점 계전석", ChatFormatting.LIGHT_PURPLE);
        }

        private UUID spawnRelay(ServerLevel level, Vec3 pos, String name, ChatFormatting color) {
            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setInvulnerable(true);
            stand.setNoGravity(true);
            stand.setShowArms(true);
            stand.setCustomName(Component.literal(name).withStyle(color));
            stand.setCustomNameVisible(true);
            stand.setItemSlot(EquipmentSlot.HEAD, Items.AMETHYST_SHARD.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.CHEST, Items.CHAINMAIL_CHESTPLATE.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.MAINHAND, Items.COMPASS.getDefaultInstance());
            level.addFreshEntity(stand);
            return stand.getUUID();
        }

        private void travel(ServerPlayer player, String destinationId) {
            if (!progress.relayActivated(destinationId)) {
                player.sendSystemMessage(Component.literal("아직 활성화하지 않은 계전석입니다.").withStyle(ChatFormatting.GRAY));
                FieldNetwork.sync(player, snapshot(player, FieldUiSnapshot.Mode.TRAVEL, null));
                return;
            }
            Vec3 destination;
            float yaw;
            if (FieldTravelCatalog.RELAY_A01.equals(destinationId)) {
                destination = a01(50.0, 1.0, 28.5);
                yaw = 180.0F;
            } else if (FieldTravelCatalog.RELAY_A02.equals(destinationId) && progress.chapterCleared()) {
                destination = a02.relay().add(2.5, -1.0, 0.0);
                yaw = -90.0F;
            } else {
                return;
            }
            player.setPos(destination.x, destination.y, destination.z);
            player.setYRot(yaw);
            player.setXRot(4.0F);
            player.setDeltaMovement(Vec3.ZERO);
            FieldNetwork.sync(player, snapshot(player, FieldUiSnapshot.Mode.NONE, null));
        }

        private FieldUiSnapshot snapshot(ServerPlayer player, FieldUiSnapshot.Mode mode, SouthgateChapterProgress.RewardReceipt receipt) {
            String objective = objective();
            String dialogue = dialogue();
            List<FieldUiSnapshot.Encounter> encounterViews = new ArrayList<>();
            for (String id : SouthgateEncounterCatalog.normalEncounterIds()) {
                var spec = SouthgateEncounterCatalog.spec(id);
                encounterViews.add(new FieldUiSnapshot.Encounter(id, spec.label(), progress.cleared(id), true, false));
            }
            var boss = SouthgateEncounterCatalog.boss();
            encounterViews.add(new FieldUiSnapshot.Encounter(boss.id(), boss.label(), progress.cleared(boss.id()), progress.bossUnlocked(), true));

            List<FieldUiSnapshot.Travel> travelViews = new ArrayList<>();
            for (FieldTravelCatalog.Destination destination : FieldTravelCatalog.destinations()) {
                Vec3 anchor = destination.id().equals(FieldTravelCatalog.RELAY_A01) ? a01(50.0, 3.0, 31.0) : a02.relay();
                boolean current = player.position().distanceToSqr(anchor) <= 36.0;
                travelViews.add(new FieldUiSnapshot.Travel(destination.id(), destination.label(),
                        progress.relayActivated(destination.id()), current));
            }

            FieldUiSnapshot.Reward reward = receipt == null
                    ? FieldUiSnapshot.Reward.none()
                    : new FieldUiSnapshot.Reward(SouthgateEncounterCatalog.spec(receipt.encounterId()).label(),
                    receipt.xp(), receipt.gold(), receipt.firstClear(), receipt.chapterCleared());
            return new FieldUiSnapshot(true, mode, progress.patrolsCleared(), progress.patrolGoal(),
                    progress.bossUnlocked(), progress.chapterCleared(), progress.earnedXp(), progress.earnedGold(),
                    objective, dialogue, reward, encounterViews, travelViews);
        }

        private String objective() {
            if (progress.chapterCleared()) {
                return progress.relayActivated(FieldTravelCatalog.RELAY_A02)
                        ? "Chapter 1 완료 · 남부 도로 거점 확보"
                        : "남부 도로 거점으로 진출해 계전석을 활성화하십시오.";
            }
            if (progress.bossUnlocked()) return "남문 봉쇄선의 B01 그라울을 처치하십시오.";
            return "남문 초원의 적 무리를 정리하십시오.  " + progress.patrolsCleared() + "/" + progress.patrolGoal();
        }

        private String dialogue() {
            if (progress.chapterCleared()) {
                return "봉쇄선은 무너졌다. 남쪽 길이 열렸어. 다음 거점의 계전석까지 확보하면 이 일대 이동망도 복구된다.";
            }
            if (progress.bossUnlocked()) {
                return "순찰대는 전부 정리됐다. 이제 봉쇄선을 지키는 그라울만 남았어. 남쪽 길에서 끝내자.";
            }
            return "초원에 흩어진 적 무리를 먼저 정리해 줘. 보이는 적과 접촉하면 전투가 시작되고, 전부 정리하면 봉쇄선의 주인이 모습을 드러낼 거야.";
        }

        private void despawnAll(ServerLevel level) {
            for (Patrol patrol : encounters.values()) patrol.despawn(level);
            despawn(level, npc);
            despawn(level, relayA01);
            despawn(level, relayA02);
            npc = null;
            relayA01 = null;
            relayA02 = null;
        }

        private void despawn(ServerLevel level, UUID id) {
            if (id == null) return;
            Entity entity = level.getEntity(id);
            if (entity != null) entity.discard();
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
