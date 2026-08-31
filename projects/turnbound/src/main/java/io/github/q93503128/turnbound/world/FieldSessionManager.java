package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
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

/** v0.4 Southgate Chapter 1 field session: five visible encounters, FT_MEADOW and canonical B01 gate. */
public final class FieldSessionManager {
    public static final String ENCOUNTER_A01_PATROL = "ENC_M01";
    private static final List<String> NORMAL_IDS = List.of("ENC_M01", "ENC_M02", "ENC_M03", "ENC_M04", "ENC_M05");
    private static final String B01_ID = "BATTLE_B01";
    private static final Map<UUID, FieldSession> SESSIONS = new LinkedHashMap<>();

    private FieldSessionManager() {}

    public static void ensureAutomatic(ServerPlayer player) {
        if (SESSIONS.containsKey(player.getUUID()) || BattleSessionManager.exists(player)) return;
        if (player.level().dimension() != Level.OVERWORLD || player.tickCount < 40) return;
        enter(player);
    }

    public static boolean enter(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return false;
        BattleSessionManager.end(player);
        remove(player);
        ServerLevel level = (ServerLevel) player.level();
        StarterSliceWorld.BuiltSlice slice = StarterSliceWorld.build(level);
        SouthgateChapterWorld.BuiltChapter chapter = SouthgateChapterWorld.build(level, slice.baseY());
        Set<String> persistedClears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        FieldSession session = new FieldSession(slice, chapter, persistedClears);
        SESSIONS.put(player.getUUID(), session);
        session.refreshWorld(level);
        player.setPos(slice.spawn().x, slice.spawn().y, slice.spawn().z);
        player.setYRot(180.0F);
        player.setXRot(3.0F);
        player.setDeltaMovement(Vec3.ZERO);
        session.spawnAll(level);
        player.sendSystemMessage(Component.literal("TURNBOUND · 남문 초원 Chapter 1").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("M01~M05와 B01 그라울까지 한 지역 진행으로 연결되었습니다.").withStyle(ChatFormatting.GRAY));
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
        boolean inStarter = StarterSliceWorld.contains(session.slice, player.position());
        boolean inChapter = SouthgateChapterWorld.contains(session.chapter, player.position());
        if (!inStarter && !inChapter) {
            Vec3 fallback = session.starterComplete() ? session.chapter.meadowRelay() : session.slice.spawn();
            player.setPos(fallback.x, fallback.y, fallback.z);
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (player.tickCount % 20 == 0) clearVanillaMobs(level, session.slice, session.chapter);
        session.tickPatrols(level, player);
    }

    public static void onBattleEnded(ServerPlayer player, String encounterId, BattleOutcome outcome) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || !(player.level() instanceof ServerLevel level)) return;
        String canonicalId = CampaignProgressStore.canonicalEncounterId(encounterId);
        Patrol patrol = session.encounters.get(canonicalId);
        if (patrol == null) return;
        patrol.despawn(level);
        if (outcome != BattleOutcome.ALLY_VICTORY) {
            patrol.reset(level);
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
            return;
        }

        boolean first = session.cleared.add(canonicalId);
        V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(canonicalId);
        int xp = first ? V04Catalogs.battleXp(spec) : 0;
        int gold = first ? V04Catalogs.battleGold(spec) : 0;
        session.earnedXp += xp;
        session.earnedGold += gold;
        patrol.defeated = true;
        boolean chapterCleared = session.chapterComplete();
        session.refreshWorld(level);
        session.spawnUnlockedMissing(level);

        FieldUiSnapshot.Reward reward = new FieldUiSnapshot.Reward(spec.label(), xp, gold, first, chapterCleared && B01_ID.equals(canonicalId));
        player.sendSystemMessage(Component.literal("승리 · " + spec.label()).withStyle(ChatFormatting.GREEN));
        if (session.starterComplete() && (canonicalId.equals("ENC_M01") || canonicalId.equals("ENC_M02"))) {
            player.sendSystemMessage(Component.literal("FT_MEADOW와 남문 초원 심부가 개방되었습니다.").withStyle(ChatFormatting.AQUA));
        }
        if (session.bossUnlocked() && canonicalId.equals("ENC_M04")) {
            player.sendSystemMessage(Component.literal("불안정 폭발체를 제압했습니다. B01 그라울의 봉쇄문이 열립니다.").withStyle(ChatFormatting.RED));
        }
        if (chapterCleared && B01_ID.equals(canonicalId)) {
            player.sendSystemMessage(Component.literal("Chapter 1 완료 · 들이받는 왕 그라울 격파").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
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
        if (id.equals(session.villageRelay) || id.equals(session.meadowRelay)) {
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.TRAVEL, null));
            return true;
        }
        return false;
    }

    public static void sendStatus(ServerPlayer player) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session != null) FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
    }

    public static void command(ServerPlayer player, String command) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || command == null || BattleSessionManager.exists(player)) return;
        String[] parts = command.split("\\|", -1);
        if (parts.length < 2 || !"TRAVEL".equals(parts[0])) return;
        Vec3 target;
        float yaw;
        if (AsterMarchRegionCatalog.FT_MEADOW.equals(parts[1]) && session.starterComplete()) {
            target = session.chapter.meadowRelay();
            yaw = 90.0F;
        } else if ("START_VILLAGE".equals(parts[1]) || AsterMarchRegionCatalog.FT_RADIA.equals(parts[1])) {
            target = session.slice.spawn();
            yaw = 180.0F;
        } else return;
        player.setPos(target.x, target.y, target.z);
        player.setYRot(yaw);
        player.setXRot(3.0F);
        player.setDeltaMovement(Vec3.ZERO);
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
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

    static Set<String> projectStarterClears(Set<String> persistedClears) { return StarterFieldProgress.project(persistedClears); }

    private static void clearVanillaMobs(ServerLevel level, StarterSliceWorld.BuiltSlice slice, SouthgateChapterWorld.BuiltChapter chapter) {
        AABB area = new AABB(AsterMarchRegionCatalog.SOUTHGATE.minX() - 4, Math.min(slice.baseY(), 56) - 8,
                AsterMarchRegionCatalog.SOUTHGATE.minZ() - 4, AsterMarchRegionCatalog.SOUTHGATE.maxX() + 4, 96,
                AsterMarchRegionCatalog.SOUTHGATE.maxZ() + 4);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) mob.discard();
    }

    private static final class FieldSession {
        private final StarterSliceWorld.BuiltSlice slice;
        private final SouthgateChapterWorld.BuiltChapter chapter;
        private final Map<String, Patrol> encounters = new LinkedHashMap<>();
        private final Set<String> cleared = new HashSet<>();
        private UUID npc;
        private UUID villageRelay;
        private UUID meadowRelay;
        private int earnedXp;
        private int earnedGold;

        private FieldSession(StarterSliceWorld.BuiltSlice slice, SouthgateChapterWorld.BuiltChapter chapter, Set<String> persistedClears) {
            this.slice = slice;
            this.chapter = chapter;
            encounters.put("ENC_M01", new Patrol("ENC_M01", slice.m01Home(), slice.m01End(), null, 0.0F));
            encounters.put("ENC_M02", new Patrol("ENC_M02", slice.m02Home(), slice.m02End(), null, 0.0F));
            encounters.put("ENC_M03", new Patrol("ENC_M03", chapter.m03Home(), chapter.m03End(), null, 0.0F));
            encounters.put("ENC_M04", new Patrol("ENC_M04", chapter.m04Home(), chapter.m04End(), chapter.m04BattleAnchor(), 180.0F));
            encounters.put("ENC_M05", new Patrol("ENC_M05", chapter.m05Home(), chapter.m05End(), chapter.m05BattleAnchor(), 180.0F));
            encounters.put(B01_ID, new Patrol(B01_ID, chapter.bossApproach(), chapter.bossApproach(), chapter.bossAnchor(), chapter.bossYaw()));
            cleared.addAll(StarterFieldProgress.project(persistedClears));
            for (String id : cleared) {
                Patrol patrol = encounters.get(id);
                if (patrol != null) patrol.defeated = true;
            }
        }

        private void refreshWorld(ServerLevel level) {
            SouthgateChapterWorld.setEntryGateOpen(level, slice.baseY(), starterComplete());
            SouthgateChapterWorld.setBossGateOpen(level, bossUnlocked());
        }

        private void spawnAll(ServerLevel level) {
            spawnNpc(level);
            spawnVillageRelay(level);
            spawnMeadowRelay(level);
            spawnUnlockedMissing(level);
        }

        private void spawnUnlockedMissing(ServerLevel level) {
            for (Patrol patrol : encounters.values()) {
                if (!isUnlocked(patrol.encounterId) || patrol.defeated || patrol.entitiesAlive(level)) continue;
                patrol.spawn(level);
            }
        }

        private void tickPatrols(ServerLevel level, ServerPlayer player) {
            for (Patrol patrol : encounters.values()) {
                if (!isUnlocked(patrol.encounterId) || patrol.defeated) continue;
                if (patrol.graceTicks > 0) patrol.graceTicks--;
                if (!patrol.entitiesAlive(level)) patrol.spawn(level);
                if (patrol.tick(level, player)) return;
            }
        }

        private boolean isUnlocked(String id) {
            if (id.equals("ENC_M01") || id.equals("ENC_M02")) return true;
            if (id.equals(B01_ID)) return bossUnlocked();
            return starterComplete();
        }

        private boolean starterComplete() { return StarterFieldProgress.starterPatrolComplete(cleared); }
        private boolean bossUnlocked() { return StarterFieldProgress.bossUnlocked(cleared); }
        private boolean chapterComplete() { return StarterFieldProgress.chapterComplete(cleared); }

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

        private void spawnVillageRelay(ServerLevel level) {
            Vec3 pos = slice.relay();
            ArmorStand stand = relayActor(level, pos, "남문 마을 계전석");
            level.addFreshEntity(stand); villageRelay = stand.getUUID();
        }

        private void spawnMeadowRelay(ServerLevel level) {
            Vec3 pos = chapter.meadowRelay();
            ArmorStand stand = relayActor(level, pos, "남문 초원 계전소 · FT_MEADOW");
            level.addFreshEntity(stand); meadowRelay = stand.getUUID();
        }

        private ArmorStand relayActor(ServerLevel level, Vec3 pos, String name) {
            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setInvulnerable(true); stand.setNoGravity(true); stand.setShowArms(true);
            stand.setCustomName(Component.literal(name).withStyle(ChatFormatting.LIGHT_PURPLE));
            stand.setCustomNameVisible(true);
            stand.setItemSlot(EquipmentSlot.HEAD, Items.AMETHYST_SHARD.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.MAINHAND, Items.COMPASS.getDefaultInstance());
            return stand;
        }

        private FieldUiSnapshot snapshot(ServerPlayer player, FieldUiSnapshot.Mode mode, FieldUiSnapshot.Reward reward) {
            List<FieldUiSnapshot.Encounter> views = new ArrayList<>();
            for (String id : NORMAL_IDS) views.add(encounterView(id));
            views.add(encounterView(B01_ID));
            boolean villageCurrent = player.position().distanceToSqr(slice.spawn()) <= 196.0;
            boolean meadowCurrent = player.position().distanceToSqr(chapter.meadowRelay()) <= 196.0;
            List<FieldUiSnapshot.Travel> travels = List.of(
                    new FieldUiSnapshot.Travel("START_VILLAGE", "남문 마을", true, villageCurrent),
                    new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_MEADOW, "남문 초원 계전소", starterComplete(), meadowCurrent));
            return new FieldUiSnapshot(true, mode, StarterFieldProgress.normalClearCount(cleared), 5,
                    bossUnlocked(), chapterComplete(), earnedXp, earnedGold, objective(), dialogue(),
                    reward == null ? FieldUiSnapshot.Reward.none() : reward, views, travels);
        }

        private FieldUiSnapshot.Encounter encounterView(String id) {
            V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(id);
            return new FieldUiSnapshot.Encounter(id, spec.label(), cleared.contains(id), isUnlocked(id), spec.boss());
        }

        private String objective() {
            if (chapterComplete()) return "Chapter 1 완료 · 그라울 격파 · 다음 목적지는 그늘숲(Gloamwood)";
            if (!starterComplete()) {
                int count = (cleared.contains("ENC_M01") ? 1 : 0) + (cleared.contains("ENC_M02") ? 1 : 0);
                return "MQ_C01_01 초원 순찰 · ENC_M01/M02 승리  " + count + "/2";
            }
            if (!cleared.contains("ENC_M04")) return "MQ_C01_02 불안정 폭발체 · ENC_M04의 E003 전투에서 승리";
            return "MQ_C01_03 그라울 · 열린 봉쇄문 너머 B01을 격파";
        }

        private String dialogue() {
            if (chapterComplete()) return "남문 봉쇄는 끝났다. 라디아로 돌아가 다음 작전, 그늘숲 진입을 준비해.";
            if (!starterComplete()) return "먼저 초입의 두 순찰을 정리해. 둘을 끝내면 초원 계전소와 심부 길이 열린다.";
            if (!cleared.contains("ENC_M04")) return "심부의 불안정 폭발체를 찾아. 그 놈을 꺾어야 그라울의 봉쇄선으로 갈 수 있어.";
            return "봉쇄문이 열렸다. 그라울은 동쪽 끝 전투장에 있다. 이번 전투에서는 도주할 수 없어.";
        }

        private void despawnAll(ServerLevel level) {
            for (Patrol patrol : encounters.values()) patrol.despawn(level);
            despawn(level, npc); despawn(level, villageRelay); despawn(level, meadowRelay);
            npc = null; villageRelay = null; meadowRelay = null;
        }

        private void despawn(ServerLevel level, UUID id) {
            if (id != null) { Entity e = level.getEntity(id); if (e != null) e.discard(); }
        }
    }

    private static final class Patrol {
        private final String encounterId;
        private final V04Catalogs.Encounter spec;
        private final Vec3 home;
        private final Vec3 patrolEnd;
        private final Vec3 fixedBattleAnchor;
        private final float fixedBattleYaw;
        private final List<UUID> actors = new ArrayList<>();
        private Vec3 pivot;
        private Vec3 facing = new Vec3(0, 0, -1);
        private boolean towardEnd = true;
        private boolean defeated;
        private int graceTicks = 40;
        private FieldEncounterRules.Phase phase = FieldEncounterRules.Phase.PATROL;

        private Patrol(String encounterId, Vec3 home, Vec3 patrolEnd, Vec3 fixedBattleAnchor, float fixedBattleYaw) {
            this.encounterId = encounterId;
            this.spec = CampaignEncounterCatalog.spec(encounterId);
            this.home = home;
            this.patrolEnd = patrolEnd;
            this.fixedBattleAnchor = fixedBattleAnchor;
            this.fixedBattleYaw = fixedBattleYaw;
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
                boolean started;
                if (fixedBattleAnchor != null) {
                    started = BattleSessionManager.startEncounterAt(player, encounterId, false, false, fixedBattleAnchor, fixedBattleYaw);
                } else {
                    BattleSessionManager.startEncounter(player, encounterId, false, false);
                    started = BattleSessionManager.exists(player);
                }
                if (!started) {
                    graceTicks = 40;
                    phase = FieldEncounterRules.Phase.RETURN;
                    spawn(level);
                }
                return started;
            }

            Vec3 target = switch (phase) {
                case ALERT -> playerFlat;
                case RETURN -> home;
                case PATROL -> towardEnd ? patrolEnd : home;
            };
            double speed = switch (phase) {
                case ALERT -> 0.105;
                case RETURN -> 0.075;
                case PATROL -> spec.boss() ? 0.0 : 0.035;
            };
            Vec3 delta = target.subtract(pivot);
            if (phase == FieldEncounterRules.Phase.PATROL && delta.lengthSqr() < 0.36) {
                towardEnd = !towardEnd;
            } else if (delta.lengthSqr() > 0.0001 && speed > 0.0) {
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
            for (int i = 0; i < spec.enemies().size(); i++) {
                Vec3 pos = formation(i, facing);
                ArmorStand stand = actor(level, pos, spec.enemies().get(i));
                level.addFreshEntity(stand);
                actors.add(stand.getUUID());
            }
            updateActors(level, facing);
        }

        private ArmorStand actor(ServerLevel level, Vec3 pos, String defId) {
            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setInvulnerable(true); stand.setNoGravity(true); stand.setShowArms(true);
            stand.setCustomName(Component.literal(CanonicalData.definition(defId, spec.level(), 0, false).name()));
            stand.setCustomNameVisible(false);
            stand.setItemSlot(EquipmentSlot.CHEST, spec.boss() ? Items.DIAMOND_CHESTPLATE.getDefaultInstance() : Items.IRON_CHESTPLATE.getDefaultInstance());
            stand.setItemSlot(EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS.getDefaultInstance());
            if ("E002".equals(defId)) stand.setItemSlot(EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
            else if ("E005".equals(defId)) stand.setItemSlot(EquipmentSlot.MAINHAND, Items.GOLDEN_HOE.getDefaultInstance());
            else if ("B01".equals(defId)) stand.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_AXE.getDefaultInstance());
            else if (!"E003".equals(defId)) stand.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_SWORD.getDefaultInstance());
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
                else stand.setCustomName(Component.literal(CanonicalData.definition(spec.enemies().get(i), spec.level(), 0, false).name()));
            }
        }

        private Vec3 formation(int index, Vec3 forward) {
            Vec3 right = new Vec3(-forward.z, 0, forward.x);
            return index == 0 ? pivot : pivot.subtract(forward.scale(1.2)).add(right.scale(index % 2 == 0 ? 1.25 : -1.25));
        }

        private boolean entitiesAlive(ServerLevel level) {
            if (actors.size() != spec.enemies().size()) return false;
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
            for (UUID id : actors) { Entity e = level.getEntity(id); if (e != null) e.discard(); }
            actors.clear();
        }

        private static Vec3 horizontalDirection(Vec3 candidate, Vec3 fallback) {
            Vec3 flat = new Vec3(candidate.x, 0, candidate.z);
            return flat.lengthSqr() > 0.000001 ? flat.normalize() : fallback;
        }
        private static float yawFor(Vec3 forward) { return (float)Math.toDegrees(Math.atan2(-forward.x, forward.z)); }
        private static float smoothYaw(float current, float target, float factor) { return current + wrapDegrees(target - current) * factor; }
        private static float wrapDegrees(float degrees) {
            float wrapped = degrees % 360.0F;
            if (wrapped >= 180.0F) wrapped -= 360.0F;
            if (wrapped < -180.0F) wrapped += 360.0F;
            return wrapped;
        }
    }
}
