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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Chapter 2 field runtime: three spore interactions, five encounters, FT_GLOAM and B02 Verna. */
public final class GloamwoodSessionManager {
    private static final List<String> NORMAL_IDS = List.of("ENC_G01", "ENC_G02", "ENC_G03", "ENC_G04", "ENC_G05");
    private static final String BOSS_ID = "BATTLE_B02";
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();

    private GloamwoodSessionManager() {}

    public static boolean enter(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD || !chapterUnlocked(player)) return false;
        RadiaHubSessionManager.remove(player);
        FieldSessionManager.remove(player);
        remove(player);
        ServerLevel level = (ServerLevel)player.level();
        GloamwoodChapterWorld.BuiltChapter chapter = GloamwoodChapterWorld.build(level);
        Session session = new Session(chapter);
        SESSIONS.put(player.getUUID(), session);
        session.refresh(level, player);
        session.spawnAll(level, player);
        Vec3 entry = chapter.entry();
        player.setPos(entry.x, entry.y, entry.z);
        player.setYRot(180.0F);
        player.setXRot(4.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.sendSystemMessage(Component.literal("TURNBOUND · Chapter 2 그늘 아래").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("포자등불 3곳을 조사해 깊은 길을 열고, 뿌리수호병이 포함된 전투 2개를 승리하십시오.").withStyle(ChatFormatting.GRAY));
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
        if (!GloamwoodChapterWorld.contains(player.position())) {
            Vec3 entry = session.chapter.entry();
            player.setPos(entry.x, entry.y, entry.z);
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (player.tickCount % 20 == 0) clearVanillaMobs(level);
        session.tickEncounters(level, player);
    }

    public static boolean interactEntity(ServerPlayer player, Entity target) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || target == null) return false;
        UUID id = target.getUUID();
        if (id.equals(session.relay)) {
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.TRAVEL, null));
            return true;
        }
        Integer sporeIndex = session.sporeActors.remove(id);
        if (sporeIndex != null) {
            if (!session.questComplete(player, "MQ_C02_01_spores")) {
                CampaignProgressStore.questInteract(player.getUUID(), "SPORE_LANTERN");
                Entity actor = ((ServerLevel)player.level()).getEntity(id);
                if (actor != null) actor.discard();
                CampaignPersistence.saveIfDirty(player);
                session.refresh((ServerLevel)player.level(), player);
                session.spawnMissing((ServerLevel)player.level(), player);
                FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
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
        } else if (AsterMarchRegionCatalog.FT_GLOAM.equals(parts[1])) {
            Vec3 ft = session.chapter.fastTravel();
            if (session.deepOpen(player)) {
                player.setPos(ft.x, ft.y, ft.z);
                player.setYRot(180.0F);
                player.setDeltaMovement(Vec3.ZERO);
                FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.NONE, null));
            }
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
        ServerLevel level = (ServerLevel)player.level();
        session.refresh(level, player);
        session.spawnMissing(level, player);
        V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(encounterId);
        boolean victory = outcome == BattleOutcome.ALLY_VICTORY;
        FieldUiSnapshot.Reward reward = new FieldUiSnapshot.Reward(
                spec.label(), victory ? V04Catalogs.battleXp(spec) : 0, victory ? V04Catalogs.battleGold(spec) : 0,
                victory, victory && BOSS_ID.equals(encounterId));
        if (victory && BOSS_ID.equals(encounterId)) {
            player.sendSystemMessage(Component.literal("Chapter 2 완료 · 가시어미 베르나 격파").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else if (victory && session.deepOpen(player)) {
            player.sendSystemMessage(Component.literal("그늘숲 진행 갱신 · 깊은 길/보스 관문 상태를 확인했습니다.").withStyle(ChatFormatting.DARK_GREEN));
        }
        FieldNetwork.sync(player, session.snapshot(player, victory ? FieldUiSnapshot.Mode.RESULT : FieldUiSnapshot.Mode.QUEST, reward));
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
        return snapshot.clearedEncounters().contains("BATTLE_B01") || snapshot.quests().completed().contains("MQ_C01_03_graul");
    }

    private static void clearVanillaMobs(ServerLevel level) {
        AABB area = new AABB(AsterMarchRegionCatalog.GLOAMWOOD.minX() - 12, 52, AsterMarchRegionCatalog.GLOAMWOOD.minZ() - 12,
                AsterMarchRegionCatalog.GLOAMWOOD.maxX() + 12, 104, -110);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) mob.discard();
    }

    private static final class Session {
        private final GloamwoodChapterWorld.BuiltChapter chapter;
        private final Map<String, EncounterActor> encounters = new LinkedHashMap<>();
        private final Map<UUID, Integer> sporeActors = new LinkedHashMap<>();
        private UUID relay;

        private Session(GloamwoodChapterWorld.BuiltChapter chapter) {
            this.chapter = chapter;
            for (GloamwoodChapterWorld.EncounterPoint point : chapter.encounters()) {
                encounters.put(point.id(), new EncounterActor(point));
            }
        }

        private boolean questComplete(ServerPlayer player, String id) {
            return CampaignProgressStore.snapshot(player.getUUID()).quests().completed().contains(id);
        }

        private boolean flag(ServerPlayer player, String id) {
            return CampaignProgressStore.snapshot(player.getUUID()).quests().unlockFlags().contains(id);
        }

        private boolean deepOpen(ServerPlayer player) {
            return flag(player, "GLOAM_DEEP_PATH") || questComplete(player, "MQ_C02_01_spores");
        }

        private boolean bossOpen(ServerPlayer player) {
            return flag(player, "B02_GATE") || questComplete(player, "MQ_C02_02_root_wall");
        }

        private boolean unlocked(ServerPlayer player, String id) {
            if (id.equals("ENC_G01") || id.equals("ENC_G02")) return true;
            if (id.equals(BOSS_ID)) return bossOpen(player);
            return deepOpen(player);
        }

        private boolean cleared(ServerPlayer player, String id) {
            return CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters().contains(id);
        }

        private void refresh(ServerLevel level, ServerPlayer player) {
            GloamwoodChapterWorld.setDeepGateOpen(level, deepOpen(player));
            GloamwoodChapterWorld.setBossGateOpen(level, bossOpen(player));
        }

        private void spawnAll(ServerLevel level, ServerPlayer player) {
            spawnRelay(level);
            spawnSporeMissing(level, player);
            spawnMissing(level, player);
        }

        private void spawnMissing(ServerLevel level, ServerPlayer player) {
            spawnSporeMissing(level, player);
            for (EncounterActor actor : encounters.values()) {
                if (!unlocked(player, actor.point.id()) || cleared(player, actor.point.id()) || actor.engaged) {
                    actor.despawn(level);
                    continue;
                }
                actor.spawn(level);
            }
        }

        private void spawnRelay(ServerLevel level) {
            if (relay != null && level.getEntity(relay) != null) return;
            ArmorStand actor = actor(level, chapter.fastTravel(), "그늘숲 계전소 · FT_GLOAM", Items.AMETHYST_SHARD, ChatFormatting.LIGHT_PURPLE);
            level.addFreshEntity(actor);
            relay = actor.getUUID();
        }

        private void spawnSporeMissing(ServerLevel level, ServerPlayer player) {
            if (questComplete(player, "MQ_C02_01_spores")) return;
            int progress = CampaignProgressStore.quests(player.getUUID()).counters().getOrDefault("MQ_C02_01_spores", 0);
            for (int i = Math.min(3, progress); i < chapter.sporeLanterns().size(); i++) {
                boolean already = sporeActors.containsValue(i);
                if (already) continue;
                ArmorStand actor = actor(level, chapter.sporeLanterns().get(i), "포자등불 조사 " + (i + 1) + "/3", Items.GLOW_BERRIES, ChatFormatting.GREEN);
                level.addFreshEntity(actor);
                sporeActors.put(actor.getUUID(), i);
            }
        }

        private void tickEncounters(ServerLevel level, ServerPlayer player) {
            for (EncounterActor actor : encounters.values()) {
                if (!unlocked(player, actor.point.id()) || cleared(player, actor.point.id())) continue;
                if (actor.graceTicks > 0) { actor.graceTicks--; continue; }
                actor.spawn(level);
                if (actor.group == null || actor.engaged) continue;
                Entity lead = actor.group.lead(level);
                if (lead == null) { actor.group = null; continue; }
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
                    new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_GLOAM, "그늘숲 계전소", deepOpen(player), nearFt),
                    new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RADIA, "라디아 귀환", true, false));
            int normalClears = 0;
            for (String id : NORMAL_IDS) if (clears.contains(id)) normalClears++;
            return new FieldUiSnapshot(true, mode, normalClears, 5, bossOpen(player), clears.contains(BOSS_ID), 0, 0,
                    objective(player), dialogue(player), reward == null ? FieldUiSnapshot.Reward.none() : reward, views, travels);
        }

        private FieldUiSnapshot.Encounter view(ServerPlayer player, String id, Set<String> clears) {
            V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(id);
            return new FieldUiSnapshot.Encounter(id, spec.label(), clears.contains(id), unlocked(player, id), spec.boss());
        }

        private String objective(ServerPlayer player) {
            if (!questComplete(player, "MQ_C02_01_spores")) {
                int count = CampaignProgressStore.quests(player.getUUID()).counters().getOrDefault("MQ_C02_01_spores", 0);
                return "MQ_C02_01 포자 흔적 · 포자등불 조사 " + Math.min(3, count) + "/3";
            }
            if (!questComplete(player, "MQ_C02_02_root_wall")) {
                int count = CampaignProgressStore.quests(player.getUUID()).counters().getOrDefault("MQ_C02_02_root_wall", 0);
                return "MQ_C02_02 뿌리 장벽 · E008 뿌리수호병 포함 전투 승리 " + Math.min(2, count) + "/2";
            }
            if (!questComplete(player, "MQ_C02_03_verna")) return "MQ_C02_03 베르나 · B02 가시어미 베르나 격파";
            return "Chapter 2 완료 · 라디아로 귀환하거나 다음 지역을 준비";
        }

        private String dialogue(ServerPlayer player) {
            if (!deepOpen(player)) return "포자등불 세 곳이 깊은 숲길을 잠그고 있는 Relay 신호를 교란한다.";
            if (!bossOpen(player)) return "깊은 길이 열렸다. 뿌리수호병이 있는 전투를 두 번 돌파해 베르나의 관문을 끊어라.";
            if (!questComplete(player, "MQ_C02_03_verna")) return "가시 장벽이 약해졌다. 숲 최심부의 베르나를 상대할 수 있다.";
            return "그늘숲의 Relay 기록이 복원되었다.";
        }

        private void despawnAll(ServerLevel level) {
            if (relay != null) { Entity e = level.getEntity(relay); if (e != null) e.discard(); relay = null; }
            for (UUID id : List.copyOf(sporeActors.keySet())) { Entity e = level.getEntity(id); if (e != null) e.discard(); }
            sporeActors.clear();
            for (EncounterActor actor : encounters.values()) actor.despawn(level);
        }
    }

    private static final class EncounterActor {
        private final GloamwoodChapterWorld.EncounterPoint point;
        private FieldEncounterPresentation.Group group;
        private boolean engaged;
        private int graceTicks;

        private EncounterActor(GloamwoodChapterWorld.EncounterPoint point) { this.point = point; }

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

    private static ArmorStand actor(ServerLevel level, Vec3 pos, String name, Item item, ChatFormatting color) {
        ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setShowArms(true);
        stand.setCustomName(Component.literal(name).withStyle(color));
        stand.setCustomNameVisible(true);
        stand.setItemSlot(EquipmentSlot.MAINHAND, item.getDefaultInstance());
        stand.setItemSlot(EquipmentSlot.HEAD, Items.LEATHER_HELMET.getDefaultInstance());
        return stand;
    }
}
