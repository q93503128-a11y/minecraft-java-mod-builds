package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Chapter 3 field runtime: shared pressure valves, five encounters and the ORO-7 route. */
public final class BrokenAqueductSessionManager {
    private static final List<String> NORMAL_IDS = List.of("ENC_A01", "ENC_A02", "ENC_A03", "ENC_A04", "ENC_A05");
    private static final String BOSS_ID = "BATTLE_B03";
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();

    private BrokenAqueductSessionManager() {}

    public static boolean enter(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD || !chapterUnlocked(player)) return false;
        RadiaHubSessionManager.remove(player);
        FieldSessionManager.remove(player);
        GloamwoodSessionManager.remove(player);
        remove(player);
        ServerLevel level = (ServerLevel) player.level();
        BrokenAqueductChapterWorld.BuiltChapter chapter = BrokenAqueductChapterWorld.build(level);
        Session session = new Session(chapter);
        SESSIONS.put(player.getUUID(), session);
        session.refresh(level, player);
        session.spawnMissing(level, player);
        FieldSharedInteractionActors.ensureAqueduct(level, chapter);
        Vec3 entry = chapter.entry();
        player.setPos(entry.x, entry.y, entry.z);
        player.setYRot(-90.0F);
        player.setXRot(4.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.sendSystemMessage(Component.literal("TURNBOUND · 제3장 무너진 흐름")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("수로 밸브 2기를 복구하고 ORO-7의 봉쇄 구역까지 진입하십시오.")
                .withStyle(ChatFormatting.DARK_AQUA));
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
        return true;
    }

    public static boolean active(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID()) && player.level().dimension() == Level.OVERWORLD;
    }

    public static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || BattleSessionManager.exists(player)) return;
        ServerLevel level = (ServerLevel) player.level();
        if (!BrokenAqueductChapterWorld.contains(player.position())) {
            Vec3 entry = session.chapter.entry();
            player.setPos(entry.x, entry.y, entry.z);
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (player.tickCount % 20 == 0) {
            clearVanillaMobs(level);
            FieldSharedInteractionActors.ensureAqueduct(level, session.chapter);
        }
        session.tickEncounters(level, player);
    }

    public static boolean interactEntity(ServerPlayer player, Entity target) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || target == null) return false;
        FieldSharedInteractionActors.Role role = FieldSharedInteractionActors.role(target);
        if (role == FieldSharedInteractionActors.Role.AQUEDUCT_RELAY) {
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.TRAVEL, null));
            return true;
        }
        int valve = FieldSharedInteractionActors.aqueductValveIndex(role);
        if (valve >= 0) {
            if (!session.questComplete(player, "MQ_C03_01_dry_channel")) {
                int progress = CampaignProgressStore.quests(player.getUUID()).counters()
                        .getOrDefault("MQ_C03_01_dry_channel", 0);
                if (valve == progress) {
                    CampaignProgressStore.questInteract(player.getUUID(), "AQUEDUCT_VALVE");
                    CampaignPersistence.saveIfDirty(player);
                    ServerLevel level = (ServerLevel) player.level();
                    session.refresh(level, player);
                    session.spawnMissing(level, player);
                    FieldSharedInteractionActors.ensureAqueduct(level, session.chapter);
                    FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
                }
            }
            return true;
        }
        return false;
    }

    public static void command(ServerPlayer player, String raw) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || raw == null || BattleSessionManager.exists(player)) return;
        String[] parts = raw.split("\\|", -1);
        if (parts.length < 2 || !"TRAVEL".equals(parts[0])) return;
        if (AsterMarchRegionCatalog.FT_RADIA.equals(parts[1])) {
            remove(player);
            RadiaHubSessionManager.enter(player);
        } else if (AsterMarchRegionCatalog.FT_AQUEDUCT.equals(parts[1]) && session.lowerOpen(player)) {
            Vec3 ft = session.chapter.fastTravel();
            player.setPos(ft.x, ft.y, ft.z);
            player.setYRot(-90.0F);
            player.setDeltaMovement(Vec3.ZERO);
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
        }
    }

    public static void onBattleEnded(ServerPlayer player, String encounterId, BattleOutcome outcome) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) return;
        EncounterActor actor = session.encounters.get(encounterId);
        if (actor == null) return;
        actor.engaged = false;
        actor.group = null;
        actor.graceTicks = outcome == BattleOutcome.ALLY_VICTORY ? 0 : 40;
        ServerLevel level = (ServerLevel) player.level();
        session.refresh(level, player);
        session.spawnMissing(level, player);
        FieldSharedInteractionActors.ensureAqueduct(level, session.chapter);
        V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(encounterId);
        boolean victory = outcome == BattleOutcome.ALLY_VICTORY;
        FieldUiSnapshot.Reward reward = new FieldUiSnapshot.Reward(
                spec.label(), victory ? V04Catalogs.battleXp(spec) : 0, victory ? V04Catalogs.battleGold(spec) : 0,
                victory, victory && BOSS_ID.equals(encounterId));
        if (victory && BOSS_ID.equals(encounterId)) {
            player.sendSystemMessage(Component.literal("제3장 완료 · 수문관리기 ORO-7 정지")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else if (victory && "ENC_A04".equals(encounterId) && session.oroRoomOpen(player)) {
            player.sendSystemMessage(Component.literal("녹슨 백부장을 격파했습니다. ORO-7 봉쇄 구역이 열립니다.")
                    .withStyle(ChatFormatting.RED));
        }
        FieldNetwork.sync(player, session.snapshot(player,
                victory ? FieldUiSnapshot.Mode.RESULT : FieldUiSnapshot.Mode.QUEST, reward));
    }

    public static void remove(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null && player.level() instanceof ServerLevel level) session.despawnAll(level);
        if (session != null) FieldNetwork.close(player);
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) remove(player);
        SESSIONS.clear();
    }

    public static boolean chapterUnlocked(ServerPlayer player) {
        var snapshot = CampaignProgressStore.snapshot(player.getUUID());
        return snapshot.clearedEncounters().contains("BATTLE_B02")
                || snapshot.quests().completed().contains("MQ_C02_03_verna");
    }

    private static void clearVanillaMobs(ServerLevel level) {
        AABB area = new AABB(AsterMarchRegionCatalog.AQUEDUCT.minX() - 12, 46,
                AsterMarchRegionCatalog.AQUEDUCT.minZ() - 12,
                -120, 100, AsterMarchRegionCatalog.AQUEDUCT.maxZ() + 12);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (!(mob instanceof BattleActorEntity)) mob.discard();
        }
    }

    private static final class Session {
        private final BrokenAqueductChapterWorld.BuiltChapter chapter;
        private final Map<String, EncounterActor> encounters = new LinkedHashMap<>();

        private Session(BrokenAqueductChapterWorld.BuiltChapter chapter) {
            this.chapter = chapter;
            for (BrokenAqueductChapterWorld.EncounterPoint point : chapter.encounters()) {
                encounters.put(point.id(), new EncounterActor(point));
            }
        }

        private boolean questComplete(ServerPlayer player, String id) {
            return CampaignProgressStore.snapshot(player.getUUID()).quests().completed().contains(id);
        }

        private boolean flag(ServerPlayer player, String id) {
            return CampaignProgressStore.snapshot(player.getUUID()).quests().unlockFlags().contains(id);
        }

        private boolean lowerOpen(ServerPlayer player) {
            return flag(player, "AQUEDUCT_LOWER") || questComplete(player, "MQ_C03_01_dry_channel");
        }

        private boolean oroRoomOpen(ServerPlayer player) {
            return flag(player, "ORO_ROOM") || questComplete(player, "MQ_C03_02_old_orders");
        }

        private boolean unlocked(ServerPlayer player, String id) {
            if (id.equals("ENC_A01") || id.equals("ENC_A02")) return true;
            if (id.equals(BOSS_ID)) return oroRoomOpen(player);
            return lowerOpen(player);
        }

        private boolean cleared(ServerPlayer player, String id) {
            return CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters().contains(id);
        }

        private void refresh(ServerLevel level, ServerPlayer player) {
            BrokenAqueductChapterWorld.setLowerGateOpen(level, lowerOpen(player));
            BrokenAqueductChapterWorld.setOroGateOpen(level, oroRoomOpen(player));
        }

        private void spawnMissing(ServerLevel level, ServerPlayer player) {
            for (EncounterActor actor : encounters.values()) {
                if (!unlocked(player, actor.point.id()) || cleared(player, actor.point.id()) || actor.engaged) {
                    actor.despawn(level);
                    continue;
                }
                actor.spawn(level);
            }
        }

        private void tickEncounters(ServerLevel level, ServerPlayer player) {
            for (EncounterActor actor : encounters.values()) {
                if (!unlocked(player, actor.point.id()) || cleared(player, actor.point.id())) continue;
                if (actor.graceTicks > 0) {
                    actor.graceTicks--;
                    continue;
                }
                actor.spawn(level);
                if (actor.group == null || actor.engaged) continue;
                Entity lead = actor.group.lead(level);
                if (lead == null) {
                    actor.group = null;
                    continue;
                }
                if (player.position().distanceToSqr(actor.group.center()) <= 12.25) {
                    boolean started = BattleSessionManager.startEncounterAt(player, actor.point.id(), true, true,
                            actor.point.battleAnchor(), actor.point.battleYaw());
                    if (started) {
                        actor.despawn(level);
                        actor.engaged = true;
                        return;
                    }
                    actor.graceTicks = 40;
                }
            }
        }

        private FieldUiSnapshot snapshot(ServerPlayer player, FieldUiSnapshot.Mode mode, FieldUiSnapshot.Reward reward) {
            Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
            List<FieldUiSnapshot.Encounter> views = new ArrayList<>();
            for (String id : NORMAL_IDS) views.add(view(player, id, clears));
            views.add(view(player, BOSS_ID, clears));
            boolean nearFt = player.position().distanceToSqr(chapter.fastTravel()) <= 196.0;
            List<FieldUiSnapshot.Travel> travels = List.of(
                    new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_AQUEDUCT, "붕괴 수로 계전소", lowerOpen(player), nearFt),
                    new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RADIA, "라디아 귀환", true, false));
            int normalClears = 0;
            for (String id : NORMAL_IDS) if (clears.contains(id)) normalClears++;
            return new FieldUiSnapshot(true, mode, normalClears, 5, oroRoomOpen(player), clears.contains(BOSS_ID), 0, 0,
                    objective(player), dialogue(player), reward == null ? FieldUiSnapshot.Reward.none() : reward,
                    views, travels);
        }

        private FieldUiSnapshot.Encounter view(ServerPlayer player, String id, Set<String> clears) {
            V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(id);
            return new FieldUiSnapshot.Encounter(id, spec.label(), clears.contains(id), unlocked(player, id), spec.boss());
        }

        private String objective(ServerPlayer player) {
            if (!questComplete(player, "MQ_C03_01_dry_channel")) {
                int count = CampaignProgressStore.quests(player.getUUID()).counters()
                        .getOrDefault("MQ_C03_01_dry_channel", 0);
                return "마른 수로 · 압력 밸브 복구 " + Math.min(2, count) + "/2";
            }
            if (!questComplete(player, "MQ_C03_02_old_orders")) return "오래된 명령 · 녹슨 백부장 격파";
            if (!questComplete(player, "MQ_C03_03_oro7")) return "ORO-7 · 수문관리기 ORO-7 정지";
            return "제3장 완료 · 라디아로 귀환하거나 다음 지역을 준비";
        }

        private String dialogue(ServerPlayer player) {
            if (!lowerOpen(player)) return "멈춘 밸브 두 기 때문에 하층 수로 접근로가 잠겨 있다.";
            if (!oroRoomOpen(player)) return "하층 수로가 열렸다. 남은 자동 방위 명령의 지휘 개체인 녹슨 백부장을 제거해.";
            if (!questComplete(player, "MQ_C03_03_oro7")) return "ORO-7 봉쇄 구역 접근이 허용되었다. 수문관리기 ORO-7을 정지시켜야 한다.";
            return "수로의 Relay 기록과 상세 도감이 복원되었다.";
        }

        private void despawnAll(ServerLevel level) {
            for (EncounterActor actor : encounters.values()) actor.despawn(level);
        }
    }

    private static final class EncounterActor {
        private final BrokenAqueductChapterWorld.EncounterPoint point;
        private FieldEncounterPresentation.Group group;
        private boolean engaged;
        private int graceTicks;

        private EncounterActor(BrokenAqueductChapterWorld.EncounterPoint point) {
            this.point = point;
        }

        private void spawn(ServerLevel level) {
            if (group != null && group.alive(level)) return;
            if (group != null) group.despawn(level);
            group = FieldEncounterPresentation.spawn(level, point.id(), point.fieldPosition(), point.battleYaw());
        }

        private void despawn(ServerLevel level) {
            if (group != null) group.despawn(level);
            group = null;
        }
    }
}
