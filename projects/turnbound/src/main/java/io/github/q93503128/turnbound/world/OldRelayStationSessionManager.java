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

/** Final main-story field runtime: shared Serak records, R01-R05, Serak and Relay reconnection. */
public final class OldRelayStationSessionManager {
    private static final List<String> NORMAL_IDS = List.of("ENC_R01", "ENC_R02", "ENC_R03", "ENC_R04", "ENC_R05");
    private static final String BOSS_ID = "BATTLE_B05";
    private static final String RECORD_QUEST = "MQ_C05_02_serak_record";
    private static final String FINAL_QUEST = "MQ_C05_03_reconnect";
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();

    private OldRelayStationSessionManager() {}

    public static boolean enter(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD || !chapterUnlocked(player)) return false;
        RadiaHubSessionManager.remove(player);
        FieldSessionManager.remove(player);
        GloamwoodSessionManager.remove(player);
        BrokenAqueductSessionManager.remove(player);
        EmberQuarrySessionManager.remove(player);
        remove(player);
        ServerLevel level = (ServerLevel) player.level();
        OldRelayStationWorld.BuiltChapter chapter = OldRelayStationWorld.build(level);
        Session session = new Session(chapter);
        SESSIONS.put(player.getUUID(), session);
        session.refresh(level, player);
        session.spawnMissing(level, player);
        FieldSharedInteractionActors.ensureOldRelay(level, chapter,
                session.bossCleared(player) && !session.questComplete(player, FINAL_QUEST));
        Vec3 entry = chapter.entry();
        player.setPos(entry.x, entry.y, entry.z);
        player.setYRot(90.0F);
        player.setXRot(4.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.sendSystemMessage(Component.literal("TURNBOUND · 제5장 구 중계소")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("세라크 기록 4개를 복원하고 균열감시자를 격파한 뒤 Relay 제어 콘솔을 재가동하십시오.")
                .withStyle(ChatFormatting.GRAY));
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
        if (!OldRelayStationWorld.contains(player.position())) {
            Vec3 entry = session.chapter.entry();
            player.setPos(entry.x, entry.y, entry.z);
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (player.tickCount % 20 == 0) {
            clearVanillaMobs(level);
            FieldSharedInteractionActors.ensureOldRelay(level, session.chapter,
                    session.bossCleared(player) && !session.questComplete(player, FINAL_QUEST));
        }
        session.tickEncounters(level, player);
    }

    public static boolean interactEntity(ServerPlayer player, Entity target) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || target == null) return false;
        FieldSharedInteractionActors.Role role = FieldSharedInteractionActors.role(target);
        if (role == FieldSharedInteractionActors.Role.OLD_RELAY_FT) {
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.TRAVEL, null));
            return true;
        }
        int record = FieldSharedInteractionActors.oldRelayRecordIndex(role);
        if (record >= 0) {
            if (!session.questComplete(player, RECORD_QUEST)) {
                int progress = Math.min(4, session.recordCount(player));
                if (record == progress) {
                    CampaignProgressStore.questInteract(player.getUUID(), "SERAK_RECORD");
                    CampaignPersistence.saveIfDirty(player);
                    ServerLevel level = (ServerLevel) player.level();
                    session.refresh(level, player);
                    session.spawnMissing(level, player);
                    FieldSharedInteractionActors.ensureOldRelay(level, session.chapter,
                            session.bossCleared(player) && !session.questComplete(player, FINAL_QUEST));
                    FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
                }
            }
            return true;
        }
        if (role == FieldSharedInteractionActors.Role.OLD_RELAY_FINAL_CONSOLE) {
            if (session.bossCleared(player) && !session.questComplete(player, FINAL_QUEST)) {
                CampaignProgressStore.questInteract(player.getUUID(), "RELAY_CONSOLE");
                CampaignPersistence.saveIfDirty(player);
                ServerLevel level = (ServerLevel) player.level();
                session.refresh(level, player);
                FieldSharedInteractionActors.ensureOldRelay(level, session.chapter,
                        session.bossCleared(player) && !session.questComplete(player, FINAL_QUEST));
                FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.RESULT, null));
                if (session.questComplete(player, FINAL_QUEST)) {
                    player.sendSystemMessage(Component.literal("아스테르 변경 Relay 재연결 · 후반 콘텐츠 개방")
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
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
        } else if (AsterMarchRegionCatalog.FT_RELAY.equals(parts[1])) {
            Vec3 ft = session.chapter.fastTravel();
            player.setPos(ft.x, ft.y, ft.z);
            player.setYRot(90.0F);
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
        FieldSharedInteractionActors.ensureOldRelay(level, session.chapter,
                session.bossCleared(player) && !session.questComplete(player, FINAL_QUEST));
        V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(encounterId);
        boolean victory = outcome == BattleOutcome.ALLY_VICTORY;
        FieldUiSnapshot.Reward reward = new FieldUiSnapshot.Reward(
                spec.label(), victory ? V04Catalogs.battleXp(spec) : 0, victory ? V04Catalogs.battleGold(spec) : 0,
                victory, victory && BOSS_ID.equals(encounterId));
        if (victory && BOSS_ID.equals(encounterId)) {
            player.sendSystemMessage(Component.literal("세라크 격파 · 최종 Relay 제어 콘솔을 작동하십시오.")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
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
        var quests = CampaignProgressStore.snapshot(player.getUUID()).quests();
        return quests.completed().contains("MQ_C05_01_relay_key")
                || quests.unlockFlags().contains("OLD_RELAY_ENTRANCE");
    }

    private static void clearVanillaMobs(ServerLevel level) {
        AABB area = new AABB(240, 46, AsterMarchRegionCatalog.OLD_RELAY.minZ() - 12,
                AsterMarchRegionCatalog.OLD_RELAY.maxX() + 12, 106, -160);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (!(mob instanceof BattleActorEntity)) mob.discard();
        }
    }

    private static final class Session {
        private final OldRelayStationWorld.BuiltChapter chapter;
        private final Map<String, EncounterActor> encounters = new LinkedHashMap<>();

        private Session(OldRelayStationWorld.BuiltChapter chapter) {
            this.chapter = chapter;
            for (OldRelayStationWorld.EncounterPoint point : chapter.encounters()) {
                encounters.put(point.id(), new EncounterActor(point));
            }
        }

        private boolean questComplete(ServerPlayer player, String id) {
            return CampaignProgressStore.snapshot(player.getUUID()).quests().completed().contains(id);
        }

        private boolean flag(ServerPlayer player, String id) {
            return CampaignProgressStore.snapshot(player.getUUID()).quests().unlockFlags().contains(id);
        }

        private boolean cleared(ServerPlayer player, String id) {
            return CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters().contains(id);
        }

        private boolean bossOpen(ServerPlayer player) {
            return flag(player, "B05_GATE") || questComplete(player, RECORD_QUEST);
        }

        private boolean bossCleared(ServerPlayer player) {
            return cleared(player, BOSS_ID);
        }

        private int recordCount(ServerPlayer player) {
            return CampaignProgressStore.quests(player.getUUID()).counters().getOrDefault(RECORD_QUEST, 0);
        }

        private boolean unlocked(ServerPlayer player, String id) {
            return !id.equals(BOSS_ID) || bossOpen(player);
        }

        private void refresh(ServerLevel level, ServerPlayer player) {
            OldRelayStationWorld.setEntranceOpen(level, true);
            OldRelayStationWorld.setBossGateOpen(level, bossOpen(player));
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
            List<FieldUiSnapshot.Travel> travels = List.of(
                    new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RELAY, "구 중계소 계전소", true,
                            player.position().distanceToSqr(chapter.fastTravel()) <= 196.0),
                    new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RADIA, "라디아 귀환", true, false));
            int normalClears = 0;
            for (String id : NORMAL_IDS) if (clears.contains(id)) normalClears++;
            return new FieldUiSnapshot(true, mode, normalClears, 5, bossOpen(player), questComplete(player, FINAL_QUEST), 0, 0,
                    objective(player), dialogue(player), reward == null ? FieldUiSnapshot.Reward.none() : reward,
                    views, travels);
        }

        private FieldUiSnapshot.Encounter view(ServerPlayer player, String id, Set<String> clears) {
            V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(id);
            return new FieldUiSnapshot.Encounter(id, spec.label(), clears.contains(id), unlocked(player, id), spec.boss());
        }

        private String objective(ServerPlayer player) {
            if (!questComplete(player, RECORD_QUEST)) {
                return "세라크 기록 · 기록실 조사 " + Math.min(4, recordCount(player)) + "/4";
            }
            if (!bossCleared(player)) return "재연결 · 균열감시자 세라크 격파";
            if (!questComplete(player, FINAL_QUEST)) return "재연결 · Relay 제어 콘솔 작동";
            return "메인 스토리 완료 · 균열문 / 고난도 재도전 / 전용 장비 시험 개방";
        }

        private String dialogue(ServerPlayer player) {
            if (!bossOpen(player)) return "중계소 기록실 네 곳에 세라크가 남긴 봉쇄 기록이 흩어져 있다.";
            if (!bossCleared(player)) return "기록 복원이 끝났다. 세라크는 중계소를 지키기 위해 스스로 균열과 결합했다.";
            if (!questComplete(player, FINAL_QUEST)) return "세라크는 쓰러졌지만 Relay는 아직 멈춰 있다. 마지막 제어 콘솔을 직접 재가동해.";
            return "Relay 일부가 재가동되었고 동쪽 외부 지역의 미약한 신호가 잡힌다.";
        }

        private void despawnAll(ServerLevel level) {
            for (EncounterActor actor : encounters.values()) actor.despawn(level);
        }
    }

    private static final class EncounterActor {
        private final OldRelayStationWorld.EncounterPoint point;
        private FieldEncounterPresentation.Group group;
        private boolean engaged;
        private int graceTicks;

        private EncounterActor(OldRelayStationWorld.EncounterPoint point) {
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
