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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Items;
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

/** Radia hub runtime and v0.4 Prologue quest flow. */
public final class RadiaHubSessionManager {
    private static final List<String> TUTORIALS = List.of("TUTORIAL_1", "TUTORIAL_2", "TUTORIAL_3");
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();

    private RadiaHubSessionManager() {}

    public static boolean enter(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD) return false;
        FieldSessionManager.remove(player);
        GloamwoodSessionManager.remove(player);
        remove(player);
        ServerLevel level = (ServerLevel)player.level();
        RadiaHubWorld.BuiltHub hub = RadiaHubWorld.build(level);
        Session session = new Session(hub);
        SESSIONS.put(player.getUUID(), session);
        session.refresh(level, player);
        session.spawn(level);
        player.setPos(hub.spawn().x, hub.spawn().y, hub.spawn().z);
        player.setYRot(180.0F);
        player.setXRot(3.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.sendSystemMessage(Component.literal("TURNBOUND · 라디아").withStyle(ChatFormatting.GOLD));
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
        return true;
    }

    public static boolean active(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID()) && player.level().dimension() == Level.OVERWORLD;
    }

    public static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || BattleSessionManager.exists(player)) return;
        ServerLevel level = (ServerLevel)player.level();
        if (!RadiaHubWorld.contains(player.position())) {
            Vec3 spawn = session.hub.spawn();
            player.setPos(spawn.x, spawn.y, spawn.z);
            player.setDeltaMovement(Vec3.ZERO);
        }
        if (player.tickCount % 20 == 0) clearMobs(level);
    }

    public static boolean interactEntity(ServerPlayer player, Entity target) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || target == null) return false;
        UUID id = target.getUUID();
        if (id.equals(session.director)) {
            CampaignProgressStore.questInteract(player.getUUID(), "Director Iven");
            CampaignPersistence.saveIfDirty(player);
            session.refresh((ServerLevel)player.level(), player);
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
            return true;
        }
        if (id.equals(session.partyConsole)) {
            CampaignProgressStore.setActiveParty(player.getUUID(), CampaignProgressStore.activeParty(player.getUUID()));
            CampaignPersistence.saveIfDirty(player);
            session.refresh((ServerLevel)player.level(), player);
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
            return true;
        }
        if (id.equals(session.relay)) {
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.TRAVEL, null));
            return true;
        }
        if (id.equals(session.southGate)) {
            if (session.regionUnlocked(player)) transitionToMeadow(player);
            else FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
            return true;
        }
        for (int i = 0; i < session.tutorialActors.size(); i++) {
            if (id.equals(session.tutorialActors.get(i))) {
                session.startTutorial(player, i);
                return true;
            }
        }
        return false;
    }

    public static void command(ServerPlayer player, String raw) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || raw == null || BattleSessionManager.exists(player)) return;
        String[] parts = raw.split("\\|", -1);
        if (parts.length < 2 || !"TRAVEL".equals(parts[0])) return;
        if (("SOUTH_GATE".equals(parts[1]) || AsterMarchRegionCatalog.FT_MEADOW.equals(parts[1])) && session.regionUnlocked(player)) {
            transitionToMeadow(player);
        } else if (AsterMarchRegionCatalog.FT_GLOAM.equals(parts[1]) && session.chapterOneComplete(player)) {
            remove(player);
            GloamwoodSessionManager.enter(player);
        } else if (AsterMarchRegionCatalog.FT_RADIA.equals(parts[1])) {
            Vec3 spawn = session.hub.spawn();
            player.setPos(spawn.x, spawn.y, spawn.z);
            player.setDeltaMovement(Vec3.ZERO);
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
        }
    }

    public static void onBattleEnded(ServerPlayer player, String encounterId, BattleOutcome outcome) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !TUTORIALS.contains(encounterId)) return;
        session.refresh((ServerLevel)player.level(), player);
        V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(encounterId);
        FieldUiSnapshot.Reward reward = new FieldUiSnapshot.Reward(spec.label(), 0, 0,
                outcome == BattleOutcome.ALLY_VICTORY, false);
        FieldNetwork.sync(player, session.snapshot(player,
                outcome == BattleOutcome.ALLY_VICTORY ? FieldUiSnapshot.Mode.RESULT : FieldUiSnapshot.Mode.QUEST, reward));
    }

    public static void remove(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null && player.level() instanceof ServerLevel level) session.despawn(level);
        if (session != null) FieldNetwork.close(player);
    }

    public static void clearAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer player : players) remove(player);
        SESSIONS.clear();
    }

    private static void transitionToMeadow(ServerPlayer player) {
        remove(player);
        FieldSessionManager.enter(player);
        ServerLevel level = (ServerLevel)player.level();
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
        private UUID director;
        private UUID partyConsole;
        private UUID relay;
        private UUID southGate;
        private final List<UUID> tutorialActors = new ArrayList<>();

        private Session(RadiaHubWorld.BuiltHub hub) { this.hub = hub; }

        private void refresh(ServerLevel level, ServerPlayer player) {
            RadiaHubWorld.setSouthGateOpen(level, regionUnlocked(player));
        }

        private boolean flag(ServerPlayer player, String flag) {
            return CampaignProgressStore.snapshot(player.getUUID()).quests().unlockFlags().contains(flag);
        }

        private boolean complete(ServerPlayer player, String quest) {
            return CampaignProgressStore.snapshot(player.getUUID()).quests().completed().contains(quest);
        }

        private boolean regionUnlocked(ServerPlayer player) {
            return flag(player, "REGION_MEADOW") || complete(player, "MQ_P00_03_south_gate");
        }

        private boolean chapterOneComplete(ServerPlayer player) {
            var snapshot = CampaignProgressStore.snapshot(player.getUUID());
            return snapshot.clearedEncounters().contains("BATTLE_B01") || snapshot.quests().completed().contains("MQ_C01_03_graul");
        }

        private boolean tutorialUnlocked(ServerPlayer player, int index) {
            if (!flag(player, "BATTLE_TUTORIAL")) return false;
            Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
            return index == 0 || clears.contains(TUTORIALS.get(index - 1));
        }

        private void startTutorial(ServerPlayer player, int index) {
            if (index < 0 || index >= 3 || !tutorialUnlocked(player, index)) return;
            if (CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters().contains(TUTORIALS.get(index))) return;
            Vec3 anchor = hub.tutorialBattleAnchors().get(index);
            BattleSessionManager.startEncounterAt(player, TUTORIALS.get(index), false, false, anchor, 180.0F);
        }

        private void spawn(ServerLevel level) {
            director = spawnActor(level, hub.director(), "Director Iven", Items.SPYGLASS, ChatFormatting.GOLD);
            partyConsole = spawnActor(level, hub.partyConsole(), "파티 편성 콘솔", Items.COMPASS, ChatFormatting.AQUA);
            relay = spawnActor(level, hub.relay(), "라디아 계전소 · FT_RADIA", Items.AMETHYST_SHARD, ChatFormatting.LIGHT_PURPLE);
            southGate = spawnActor(level, hub.southGate(), "South Gate", Items.IRON_SWORD, ChatFormatting.GREEN);
            for (int i = 0; i < 3; i++) {
                tutorialActors.add(spawnActor(level, hub.tutorialPedestals().get(i), "전투 훈련 " + (i + 1),
                        i == 2 ? Items.TNT : Items.IRON_SWORD, ChatFormatting.YELLOW));
            }
        }

        private UUID spawnActor(ServerLevel level, Vec3 pos, String name, net.minecraft.world.item.Item item, ChatFormatting color) {
            ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
            stand.setInvulnerable(true);
            stand.setNoGravity(true);
            stand.setShowArms(true);
            stand.setCustomName(Component.literal(name).withStyle(color));
            stand.setCustomNameVisible(true);
            stand.setItemSlot(EquipmentSlot.MAINHAND, item.getDefaultInstance());
            level.addFreshEntity(stand);
            return stand.getUUID();
        }

        private void despawn(ServerLevel level) {
            desp(level, director); desp(level, partyConsole); desp(level, relay); desp(level, southGate);
            for (UUID id : tutorialActors) desp(level, id);
            tutorialActors.clear();
        }

        private void desp(ServerLevel level, UUID id) {
            if (id == null) return;
            Entity entity = level.getEntity(id);
            if (entity != null) entity.discard();
        }

        private FieldUiSnapshot snapshot(ServerPlayer player, FieldUiSnapshot.Mode mode, FieldUiSnapshot.Reward reward) {
            Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
            List<FieldUiSnapshot.Encounter> encounters = new ArrayList<>();
            int wins = 0;
            for (int i = 0; i < 3; i++) {
                String id = TUTORIALS.get(i);
                boolean done = clears.contains(id);
                if (done) wins++;
                encounters.add(new FieldUiSnapshot.Encounter(id, CampaignEncounterCatalog.spec(id).label(), done, tutorialUnlocked(player, i), false));
            }
            List<FieldUiSnapshot.Travel> travels = new ArrayList<>();
            travels.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RADIA, "라디아 계전소", true, true));
            travels.add(new FieldUiSnapshot.Travel("SOUTH_GATE", "남문 초원 진입", regionUnlocked(player), false));
            travels.add(new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_GLOAM, "그늘숲 Chapter 2", chapterOneComplete(player), false));
            return new FieldUiSnapshot(true, mode, wins, 3, false, false, 0, 0,
                    objective(player, wins), dialogue(player), reward == null ? FieldUiSnapshot.Reward.none() : reward,
                    encounters, List.copyOf(travels));
        }

        private String objective(ServerPlayer player, int wins) {
            if (!complete(player, "MQ_P00_01_arrival")) return "MQ_P00_01 라디아 도착 · Relay Hall의 Director Iven과 대화";
            if (!complete(player, "MQ_P00_02_first_party")) return "MQ_P00_02 첫 파티 · 파티 편성 콘솔에서 P01/P03/P04/F03 편성 확인";
            if (!complete(player, "MQ_P00_03_south_gate")) return "MQ_P00_03 남문 개방 · Training Yard 전투 훈련 " + wins + "/3";
            if (!chapterOneComplete(player)) return "Chapter 1 진행 · South Gate를 통해 남문 초원에서 B01 그라울까지 격파";
            if (!complete(player, "MQ_C02_03_verna")) return "Chapter 2 해금 · 라디아 계전소에서 그늘숲으로 이동";
            return "Chapter 2 완료 · 다음 지역 진행 준비";
        }

        private String dialogue(ServerPlayer player) {
            if (!complete(player, "MQ_P00_01_arrival")) return "이븐 국장이 Relay Hall에서 기다리고 있다.";
            if (!complete(player, "MQ_P00_02_first_party")) return "첫 출동 전에 현재 4인 파티를 확인해.";
            if (!complete(player, "MQ_P00_03_south_gate")) return "훈련장 세 전투를 순서대로 끝내면 남문이 열린다.";
            if (!chapterOneComplete(player)) return "남문 초원에서 첫 Relay 이상 신호가 잡혔다. 그라울을 쓰러뜨려 원인을 확인해.";
            if (!complete(player, "MQ_C02_03_verna")) return "그늘숲 식생이 Relay 신호를 흡수하고 있다. 북쪽 숲의 계전소를 조사해.";
            return "그늘숲 기록까지 복원되었다.";
        }
    }
}
