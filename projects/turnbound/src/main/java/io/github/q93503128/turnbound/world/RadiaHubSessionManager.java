package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.CampaignEncounterCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Radia hub runtime, Prologue, canonical facilities and all five main-story chapter routes. */
public final class RadiaHubSessionManager {
    private static final List<String> TUTORIALS = List.of("TUTORIAL_1", "TUTORIAL_2", "TUTORIAL_3");
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();

    private RadiaHubSessionManager() {}

    public static boolean enter(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return false;
        FieldSessionManager.remove(player);
        GloamwoodSessionManager.remove(player);
        BrokenAqueductSessionManager.remove(player);
        EmberQuarrySessionManager.remove(player);
        OldRelayStationSessionManager.remove(player);
        remove(player);

        ServerLevel level = (ServerLevel) player.level();
        RadiaHubWorld.BuiltHub hub = RadiaHubWorld.build(level);
        Session session = new Session(hub);
        SESSIONS.put(player.getUUID(), session);
        session.refresh(level, player);
        RadiaHubSharedActors.ensure(level, hub);
        RadiaSafeSpawn.place(level, player);
        player.sendSystemMessage(Component.literal("TURNBOUND · 라디아").withStyle(ChatFormatting.GOLD));
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
        return true;
    }

    public static boolean active(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID()) && player.level().dimension() == Level.OVERWORLD;
    }

    public static void refreshProgress(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !(player.level() instanceof ServerLevel level) || BattleSessionManager.exists(player)) return;
        session.refresh(level, player);
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
    }

    public static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || BattleSessionManager.exists(player)) return;
        ServerLevel level = (ServerLevel) player.level();
        if (!RadiaHubWorld.contains(player.position())) RadiaSafeSpawn.place(level, player);
        if (player.tickCount % 100 == 0) RadiaHubSharedActors.ensure(level, session.hub);
        if (player.tickCount % 20 == 0) clearMobs(level);
    }

    public static boolean interactEntity(ServerPlayer player, Entity target) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || target == null) return false;
        RadiaHubActorCatalog.Role role = RadiaHubSharedActors.role(target);
        if (role == null) return false;

        switch (role) {
            case DIRECTOR -> {
                CampaignProgressStore.questInteract(player.getUUID(), "Director Iven");
                CampaignPersistence.saveIfDirty(player);
                session.refresh((ServerLevel) player.level(), player);
                FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
                return true;
            }
            case RELAY -> {
                if (session.chapterFourComplete(player) && !session.relayKeyComplete(player)) {
                    int submitted = RelayFragmentBridgeService.submitAvailable(player);
                    if (submitted > 0) {
                        CampaignPersistence.saveIfDirty(player);
                        player.sendSystemMessage(Component.literal("Relay 조각 제출 " + submitted + "개 · 구 중계소 좌표 복원")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
                FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.TRAVEL, null));
                return true;
            }
            case SOUTH_GATE -> {
                if (session.regionUnlocked(player)) transitionToMeadow(player);
                else FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
                return true;
            }
            default -> {
                if (role.tutorial()) {
                    session.startTutorial(player, role.tutorialIndex());
                    return true;
                }
                if (role.facility()) {
                    session.useFacility(player, role.facilityId());
                    return true;
                }
                return false;
            }
        }
    }

    public static void command(ServerPlayer player, String raw) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || raw == null || BattleSessionManager.exists(player)) return;
        String[] parts = raw.split("\\|", -1);
        if (parts.length < 2 || !"TRAVEL".equals(parts[0])) return;
        String destination = parts[1];
        if (("SOUTH_GATE".equals(destination) || AsterMarchRegionCatalog.FT_MEADOW.equals(destination))
                && session.regionUnlocked(player)) {
            transitionToMeadow(player);
        } else if (AsterMarchRegionCatalog.FT_GLOAM.equals(destination) && session.chapterOneComplete(player)) {
            remove(player);
            GloamwoodSessionManager.enter(player);
        } else if (AsterMarchRegionCatalog.FT_AQUEDUCT.equals(destination) && session.chapterTwoComplete(player)) {
            remove(player);
            BrokenAqueductSessionManager.enter(player);
        } else if (AsterMarchRegionCatalog.FT_QUARRY.equals(destination) && session.chapterThreeComplete(player)) {
            remove(player);
            EmberQuarrySessionManager.enter(player);
        } else if (AsterMarchRegionCatalog.FT_RELAY.equals(destination) && session.relayKeyComplete(player)) {
            remove(player);
            OldRelayStationSessionManager.enter(player);
        } else if (AsterMarchRegionCatalog.FT_RADIA.equals(destination)) {
            RadiaSafeSpawn.place((ServerLevel) player.level(), player);
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
        }
    }

    public static void onBattleEnded(ServerPlayer player, String encounterId, BattleOutcome outcome) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !TUTORIALS.contains(encounterId)) return;
        session.refresh((ServerLevel) player.level(), player);
        V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(encounterId);
        FieldUiSnapshot.Reward reward = new FieldUiSnapshot.Reward(
                spec.label(), 0, 0, outcome == BattleOutcome.ALLY_VICTORY, false);
        FieldNetwork.sync(player, session.snapshot(player,
                outcome == BattleOutcome.ALLY_VICTORY ? FieldUiSnapshot.Mode.RESULT : FieldUiSnapshot.Mode.QUEST,
                reward));
    }

    public static void remove(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null) FieldNetwork.close(player);
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) remove(player);
        SESSIONS.clear();
    }

    private static void transitionToMeadow(ServerPlayer player) {
        remove(player);
        FieldSessionManager.enter(player);
        ServerLevel level = (ServerLevel) player.level();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 123) + 1;
        player.setPos(0.5, y, 123.5);
        player.setYRot(180.0F);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static void clearMobs(ServerLevel level) {
        AABB area = new AABB(-132, 54, -116, 132, 100, 132);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) mob.discard();
    }

    private static final class Session {
        private final RadiaHubWorld.BuiltHub hub;

        private Session(RadiaHubWorld.BuiltHub hub) {
            this.hub = hub;
        }

        private void refresh(ServerLevel level, ServerPlayer player) {
            boolean sharedOpen = AsterMarchSharedWorldProgress.regionOpen(level, TurnboundWorldSavedData.REGION_MEADOW);
            RadiaHubWorld.setSouthGateOpen(level, sharedOpen || regionUnlocked(player));
        }

        private boolean flag(ServerPlayer player, String flag) {
            return CampaignProgressStore.snapshot(player.getUUID()).quests().unlockFlags().contains(flag);
        }

        private boolean complete(ServerPlayer player, String quest) {
            return CampaignProgressStore.snapshot(player.getUUID()).quests().completed().contains(quest);
        }

        private boolean cleared(ServerPlayer player, String encounter) {
            return CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters().contains(encounter);
        }

        private boolean regionUnlocked(ServerPlayer player) {
            return flag(player, "REGION_MEADOW") || complete(player, "MQ_P00_03_south_gate");
        }

        private boolean chapterOneComplete(ServerPlayer player) {
            return CampaignContentUnlocks.chapter1Complete(player.getUUID());
        }

        private boolean chapterTwoComplete(ServerPlayer player) {
            return CampaignContentUnlocks.chapter2Complete(player.getUUID());
        }

        private boolean chapterThreeComplete(ServerPlayer player) {
            return CampaignContentUnlocks.chapter3Complete(player.getUUID());
        }

        private boolean chapterFourComplete(ServerPlayer player) {
            return CampaignContentUnlocks.chapter4Complete(player.getUUID());
        }

        private boolean relayKeyComplete(ServerPlayer player) {
            return complete(player, "MQ_C05_01_relay_key") || flag(player, "OLD_RELAY_ENTRANCE");
        }

        private boolean storyComplete(ServerPlayer player) {
            return CampaignContentUnlocks.storyComplete(player.getUUID());
        }

        private boolean tutorialUnlocked(ServerPlayer player, int index) {
            if (!flag(player, "BATTLE_TUTORIAL")) return false;
            Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
            return index == 0 || clears.contains(TUTORIALS.get(index - 1));
        }

        private void startTutorial(ServerPlayer player, int index) {
            if (index < 0 || index >= TUTORIALS.size()
                    || !tutorialUnlocked(player, index)
                    || cleared(player, TUTORIALS.get(index))) return;
            Vec3 anchor = hub.tutorialBattleAnchors().get(index);
            BattleSessionManager.startEncounterAt(player, TUTORIALS.get(index), false, false, anchor, 180.0F);
        }

        private void useFacility(ServerPlayer player, String facilityId) {
            UUID playerId = player.getUUID();
            switch (facilityId) {
                case "ECHO_ARCHIVE" -> {
                    if (!CampaignContentUnlocks.archive(playerId)) {
                        locked(player, "메아리 기록관 · 그라울 격파 후 소환 기능 해금");
                        return;
                    }
                    MetaNetwork.open(player, "ARCHIVE");
                }
                case "FORGE_ANNEX" -> {
                    if (!CampaignContentUnlocks.forge(playerId)) {
                        locked(player, "대장간 별관 · 제1장 완료 후 장비 강화 해금");
                        return;
                    }
                    MetaNetwork.open(player, "FORGE");
                }
                case "MARKET_ROW" -> MetaNetwork.open(player, "MARKET");
                case "TRAINING_YARD" -> {
                    if (!CampaignContentUnlocks.prologueComplete(playerId)) {
                        locked(player, "훈련장 · 먼저 도입부 전투 훈련 3회를 완료해야 합니다.");
                        return;
                    }
                    player.sendSystemMessage(Component.literal("자유 훈련전 · 보상 없음").withStyle(ChatFormatting.YELLOW));
                    BattleSessionManager.start(player);
                }
                case "RIFT_GATE" -> {
                    if (!CampaignContentUnlocks.endgame(playerId)) {
                        locked(player, "균열문 · 세라크 격파와 Relay 재연결 후 개방");
                        return;
                    }
                    MetaNetwork.open(player, "RIFT");
                }
                case "MEMORIAL_STEPS" -> {
                    if (!CampaignContentUnlocks.characterQuestStageOne(playerId)) {
                        locked(player, "추모 계단 · 제2장 이후 인연 기록이 개방됩니다.");
                        return;
                    }
                    player.sendSystemMessage(Component.literal("추모 계단 · 모르웬 및 각성 관련 기록")
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                    MetaNetwork.open(player, "CHARACTERS");
                }
                case "CLOCK_TOWER" -> {
                    if (!CampaignContentUnlocks.chapter3Complete(playerId)) {
                        locked(player, "시계탑 · 개인 사건은 제3장 완료 이후 조사할 수 있습니다.");
                        return;
                    }
                    player.sendSystemMessage(Component.literal("시계탑 · 멈춘 시계탑").withStyle(ChatFormatting.AQUA));
                    MetaNetwork.open(player, "CHARACTERS");
                }
                case "BARRACKS" -> {
                    if (!CampaignContentUnlocks.characterQuestStageOne(playerId)) {
                        locked(player, "병영 · 제2장 이후 수비대 기록이 개방됩니다.");
                        return;
                    }
                    player.sendSystemMessage(Component.literal("병영 · 수비대 관련 기록").withStyle(ChatFormatting.GREEN));
                    MetaNetwork.open(player, "CHARACTERS");
                }
                default -> { }
            }
        }

        private FieldUiSnapshot snapshot(ServerPlayer player, FieldUiSnapshot.Mode mode, FieldUiSnapshot.Reward reward) {
            Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
            List<FieldUiSnapshot.Encounter> encounters = new ArrayList<>();
            int wins = 0;
            for (int i = 0; i < TUTORIALS.size(); i++) {
                String id = TUTORIALS.get(i);
                boolean done = clears.contains(id);
                if (done) wins++;
                encounters.add(new FieldUiSnapshot.Encounter(
                        id, CampaignEncounterCatalog.spec(id).label(), done, tutorialUnlocked(player, i), false));
            }

            List<FieldUiSnapshot.Travel> travels = new ArrayList<>();
            travels.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RADIA, "라디아 계전소", true, true));
            travels.add(new FieldUiSnapshot.Travel("SOUTH_GATE", "남문 초원 진입", regionUnlocked(player), false));
            travels.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_GLOAM, "그늘숲 · 제2장", chapterOneComplete(player), false));
            travels.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_AQUEDUCT, "붕괴 수로 · 제3장", chapterTwoComplete(player), false));
            travels.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_QUARRY, "잿불 채석장 · 제4장", chapterThreeComplete(player), false));
            travels.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RELAY, "구 중계소 · 제5장", relayKeyComplete(player), false));
            return new FieldUiSnapshot(
                    true, mode, wins, 3, false, storyComplete(player), 0, 0,
                    objective(player, wins), dialogue(player),
                    reward == null ? FieldUiSnapshot.Reward.none() : reward,
                    encounters, List.copyOf(travels));
        }

        private String objective(ServerPlayer player, int wins) {
            if (!complete(player, "MQ_P00_01_arrival")) return "라디아 도착 · 총괄관 아이븐과 대화";
            if (!complete(player, "MQ_P00_02_first_party")) return "첫 파티 · 카이렌 · 변경 사냥꾼 편성 확인";
            if (!complete(player, "MQ_P00_03_south_gate")) return "남문 개방 · 전투 훈련 " + wins + "/3";
            if (!chapterOneComplete(player)) return "제1장 · 들이받는 왕 그라울 격파";
            if (!chapterTwoComplete(player)) return "제2장 · 가시어미 베르나 격파";
            if (!chapterThreeComplete(player)) return "제3장 · 수문관리기 ORO-7 정지";
            if (!chapterFourComplete(player)) return "제4장 · 재의 거상 콜바크 격파";
            if (!relayKeyComplete(player)) return "중계소 열쇠 · 라디아 계전소에 세 지역 Relay 조각 제출";
            if (!storyComplete(player)) return "제5장 · 구 중계소에서 세라크 격파와 Relay 재연결 진행";
            return "메인 스토리 완료 · 균열문 / 고난도 재도전 / 전용 장비 시험 / 각성 콘텐츠 개방";
        }

        private String dialogue(ServerPlayer player) {
            if (!complete(player, "MQ_P00_03_south_gate")) return "훈련을 끝내고 남문을 개방해야 한다.";
            if (!chapterFourComplete(player)) return "아스테르 변경의 Relay 이상 신호를 각 지역에서 추적해.";
            if (!relayKeyComplete(player)) return "확보한 세 지역 Relay 조각을 중앙 계전소에 제출하면 구 중계소 좌표를 복원할 수 있다.";
            if (!storyComplete(player)) return "구 중계소 좌표가 복원됐다. 세라크와 마지막 Relay를 확인해.";
            return "Relay 일부가 재가동되었다. 라디아의 후반 시설과 동쪽 외부 신호가 활성화됐다.";
        }

        private static void locked(ServerPlayer player, String text) {
            player.sendSystemMessage(Component.literal("TURNBOUND · " + text).withStyle(ChatFormatting.GRAY));
        }
    }
}
