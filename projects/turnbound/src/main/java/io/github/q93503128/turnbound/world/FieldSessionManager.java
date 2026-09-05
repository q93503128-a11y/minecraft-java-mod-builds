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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** v0.4 Southgate Chapter 1 field session: shared physical patrols with per-player progression and battle state. */
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

        FieldSharedInteractionActors.ensureSouthgate(level, slice, chapter);
        SouthgateSharedPatrols.ensure(level, slice, chapter);

        player.sendSystemMessage(Component.literal("TURNBOUND · 남문 초원 Chapter 1").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("M01~M05와 B01 그라울까지 한 지역 진행으로 연결되었습니다.").withStyle(ChatFormatting.GRAY));
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
        return true;
    }

    public static boolean active(ServerPlayer player) {
        return player != null && SESSIONS.containsKey(player.getUUID()) && player.level().dimension() == Level.OVERWORLD;
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

        if (player.tickCount % 20 == 0) {
            clearVanillaMobs(level, session.slice, session.chapter);
            FieldSharedInteractionActors.ensureSouthgate(level, session.slice, session.chapter);
            SouthgateSharedPatrols.ensure(level, session.slice, session.chapter);
        }
        SouthgateSharedPatrols.tick(level);
    }

    public static void onBattleEnded(ServerPlayer player, String encounterId, BattleOutcome outcome) {
        FieldSession session = SESSIONS.get(player.getUUID());
        if (session == null || !(player.level() instanceof ServerLevel level)) return;
        String canonicalId = CampaignProgressStore.canonicalEncounterId(encounterId);
        if (!NORMAL_IDS.contains(canonicalId) && !B01_ID.equals(canonicalId)) return;

        if (outcome != BattleOutcome.ALLY_VICTORY) {
            SouthgateSharedPatrols.refresh(level);
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
            return;
        }

        boolean first = session.cleared.add(canonicalId);
        V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(canonicalId);
        int xp = first ? V04Catalogs.battleXp(spec) : 0;
        int gold = first ? V04Catalogs.battleGold(spec) : 0;
        session.earnedXp += xp;
        session.earnedGold += gold;
        boolean chapterCleared = session.chapterComplete();
        session.refreshWorld(level);
        FieldSharedInteractionActors.ensureSouthgate(level, session.slice, session.chapter);
        SouthgateSharedPatrols.refresh(level);

        FieldUiSnapshot.Reward reward = new FieldUiSnapshot.Reward(spec.label(), xp, gold, first,
                chapterCleared && B01_ID.equals(canonicalId));
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
        FieldSharedInteractionActors.Role role = FieldSharedInteractionActors.role(target);
        if (role == FieldSharedInteractionActors.Role.SOUTHGATE_SCOUT) {
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
            return true;
        }
        if (role == FieldSharedInteractionActors.Role.SOUTHGATE_RELAY_VILLAGE
                || role == FieldSharedInteractionActors.Role.SOUTHGATE_RELAY_MEADOW) {
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
        } else {
            return;
        }
        player.setPos(target.x, target.y, target.z);
        player.setYRot(yaw);
        player.setXRot(3.0F);
        player.setDeltaMovement(Vec3.ZERO);
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
    }

    public static void remove(ServerPlayer player) {
        FieldSession removed = SESSIONS.remove(player.getUUID());
        if (removed != null) {
            FieldNetwork.close(player);
            if (player.level() instanceof ServerLevel level) SouthgateSharedPatrols.onPlayerSessionRemoved(level);
        } else {
            FieldNetwork.close(player);
        }
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) remove(player);
        SESSIONS.clear();
    }

    static Set<String> projectStarterClears(Set<String> persistedClears) {
        return StarterFieldProgress.project(persistedClears);
    }

    private static void clearVanillaMobs(ServerLevel level, StarterSliceWorld.BuiltSlice slice,
                                         SouthgateChapterWorld.BuiltChapter chapter) {
        AABB area = new AABB(AsterMarchRegionCatalog.SOUTHGATE.minX() - 4, Math.min(slice.baseY(), 56) - 8,
                AsterMarchRegionCatalog.SOUTHGATE.minZ() - 4, AsterMarchRegionCatalog.SOUTHGATE.maxX() + 4, 96,
                AsterMarchRegionCatalog.SOUTHGATE.maxZ() + 4);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (!(mob instanceof BattleActorEntity)) mob.discard();
        }
    }

    private static final class FieldSession {
        private final StarterSliceWorld.BuiltSlice slice;
        private final SouthgateChapterWorld.BuiltChapter chapter;
        private final Set<String> cleared = new HashSet<>();
        private int earnedXp;
        private int earnedGold;

        private FieldSession(StarterSliceWorld.BuiltSlice slice, SouthgateChapterWorld.BuiltChapter chapter,
                             Set<String> persistedClears) {
            this.slice = slice;
            this.chapter = chapter;
            cleared.addAll(StarterFieldProgress.project(persistedClears));
        }

        private void refreshWorld(ServerLevel level) {
            SouthgateChapterWorld.setEntryGateOpen(level, slice.baseY(), starterComplete());
            SouthgateChapterWorld.setBossGateOpen(level, bossUnlocked());
        }

        private boolean isUnlocked(String id) {
            return SouthgateEncounterVisibilityRules.unlocked(id, cleared);
        }

        private boolean starterComplete() {
            return StarterFieldProgress.starterPatrolComplete(cleared);
        }

        private boolean bossUnlocked() {
            return StarterFieldProgress.bossUnlocked(cleared);
        }

        private boolean chapterComplete() {
            return StarterFieldProgress.chapterComplete(cleared);
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
    }
}
