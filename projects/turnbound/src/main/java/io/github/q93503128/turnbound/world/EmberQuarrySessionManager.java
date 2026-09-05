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

/** Chapter 4 runtime: ash-route battles, physical E014 core recovery and B04 Kolvak. */
public final class EmberQuarrySessionManager {
    private static final List<String> NORMAL_IDS = List.of("ENC_Q01", "ENC_Q02", "ENC_Q03", "ENC_Q04", "ENC_Q05");
    private static final String BOSS_ID = "BATTLE_B04";
    private static final String CORE_QUEST = "MQ_C04_02_core_fragment";
    private static final Map<UUID, Session> SESSIONS = new LinkedHashMap<>();

    private EmberQuarrySessionManager() {}

    public static boolean enter(ServerPlayer player) {
        if (player.level().dimension() != Level.OVERWORLD || !chapterUnlocked(player)) return false;
        RadiaHubSessionManager.remove(player);
        FieldSessionManager.remove(player);
        GloamwoodSessionManager.remove(player);
        BrokenAqueductSessionManager.remove(player);
        remove(player);
        ServerLevel level = (ServerLevel) player.level();
        EmberQuarryChapterWorld.BuiltChapter chapter = EmberQuarryChapterWorld.build(level);
        Session session = new Session(chapter);
        SESSIONS.put(player.getUUID(), session);
        session.refresh(level, player);
        session.spawnAll(level, player);
        FieldSharedInteractionActors.ensureQuarry(level, chapter, Math.min(2, session.counter(player, CORE_QUEST + "|E014")));
        Vec3 entry = chapter.entry();
        player.setPos(entry.x, entry.y, entry.z);
        player.setYRot(0.0F);
        player.setXRot(5.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.sendSystemMessage(Component.literal("TURNBOUND · Chapter 4 재의 길").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("잿빛 사냥개/잉걸술사 전선을 돌파하고 용암굴착수의 Relay 핵을 직접 회수하십시오.").withStyle(ChatFormatting.GRAY));
        FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
        return true;
    }

    public static boolean active(ServerPlayer player) { return SESSIONS.containsKey(player.getUUID()) && player.level().dimension() == Level.OVERWORLD; }

    public static void tick(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || BattleSessionManager.exists(player)) return;
        ServerLevel level = (ServerLevel) player.level();
        if (!EmberQuarryChapterWorld.contains(player.position())) {
            Vec3 entry = session.chapter.entry();
            player.setPos(entry.x, entry.y, entry.z);
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (player.tickCount % 20 == 0) {
            clearVanillaMobs(level);
            FieldSharedInteractionActors.ensureQuarry(level, session.chapter,
                    Math.min(2, session.counter(player, CORE_QUEST + "|E014")));
        }
        session.tickEncounters(level, player);
    }

    public static boolean interactEntity(ServerPlayer player, Entity target) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || target == null) return false;
        FieldSharedInteractionActors.Role sharedRole = FieldSharedInteractionActors.role(target);
        if (sharedRole == FieldSharedInteractionActors.Role.QUARRY_RELAY) {
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.TRAVEL, null));
            return true;
        }
        int sharedCore = FieldSharedInteractionActors.quarryCoreIndex(sharedRole);
        if (sharedCore >= 0) {
            int kills = Math.min(2, session.counter(player, CORE_QUEST + "|E014"));
            int looted = Math.min(2, session.counter(player, CORE_QUEST + "|CORE_FRAGMENT"));
            if (!session.questComplete(player, CORE_QUEST) && sharedCore == looted && looted < kills) {
                CampaignProgressStore.recordLoot(player.getUUID(), "CORE_FRAGMENT", 1);
                CampaignPersistence.saveIfDirty(player);
                session.refresh((ServerLevel) player.level(), player);
                session.spawnMissing((ServerLevel) player.level(), player);
                FieldSharedInteractionActors.ensureQuarry((ServerLevel) player.level(), session.chapter,
                        Math.min(2, session.counter(player, CORE_QUEST + "|E014")));
                FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.QUEST, null));
            }
            return true;
        }

        UUID id = target.getUUID();
        if (id.equals(session.relay)) {
            FieldNetwork.sync(player, session.snapshot(player, FieldUiSnapshot.Mode.TRAVEL, null));
            return true;
        }
        Integer coreIndex = session.coreActors.remove(id);
        if (coreIndex != null) {
            int kills = session.counter(player, CORE_QUEST + "|E014");
            int looted = session.counter(player, CORE_QUEST + "|CORE_FRAGMENT");
            if (looted < kills && looted < 2 && !session.questComplete(player, CORE_QUEST)) {
                CampaignProgressStore.recordLoot(player.getUUID(), "CORE_FRAGMENT", 1);
                Entity actor = ((ServerLevel) player.level()).getEntity(id);
                if (actor != null) actor.discard();
                CampaignPersistence.saveIfDirty(player);
                session.refresh((ServerLevel) player.level(), player);
                session.spawnMissing((ServerLevel) player.level(), player);
                FieldSharedInteractionActors.ensureQuarry((ServerLevel) player.level(), session.chapter,
                        Math.min(2, session.counter(player, CORE_QUEST + "|E014")));
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
        } else if (AsterMarchRegionCatalog.FT_QUARRY.equals(parts[1]) && session.ashRouteComplete(player)) {
            Vec3 ft = session.chapter.fastTravel();
            player.setPos(ft.x, ft.y, ft.z);
            player.setYRot(0.0F);
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
        V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(encounterId);
        boolean victory = outcome == BattleOutcome.ALLY_VICTORY;
        if (victory) {
            int e014 = 0;
            for (String enemy : spec.enemies()) if ("E014".equals(enemy)) e014++;
            if (e014 > 0 && !session.questComplete(player, CORE_QUEST)) {
                CampaignProgressStore.recordKill(player.getUUID(), "E014", e014);
                CampaignPersistence.saveIfDirty(player);
            }
        }
        ServerLevel level = (ServerLevel) player.level();
        session.refresh(level, player);
        session.spawnMissing(level, player);
        FieldSharedInteractionActors.ensureQuarry(level, session.chapter,
                Math.min(2, session.counter(player, CORE_QUEST + "|E014")));
        FieldUiSnapshot.Reward reward = new FieldUiSnapshot.Reward(
                spec.label(), victory ? V04Catalogs.battleXp(spec) : 0, victory ? V04Catalogs.battleGold(spec) : 0,
                victory, victory && BOSS_ID.equals(encounterId));
        if (victory && BOSS_ID.equals(encounterId)) {
            player.sendSystemMessage(Component.literal("Chapter 4 완료 · 재의 거상 콜바크 격파").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        } else if (victory && e014Present(spec)) {
            player.sendSystemMessage(Component.literal("용암굴착수의 열핵이 전장에 남았습니다. 직접 회수하십시오.").withStyle(ChatFormatting.YELLOW));
        }
        FieldNetwork.sync(player, session.snapshot(player, victory ? FieldUiSnapshot.Mode.RESULT : FieldUiSnapshot.Mode.QUEST, reward));
    }

    public static void remove(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session != null && player.level() instanceof ServerLevel level) session.despawnAll(level);
        if (session != null) FieldNetwork.close(player);
    }

    public static void clearAll(Iterable<ServerPlayer> players) { for (ServerPlayer player : players) remove(player); SESSIONS.clear(); }

    public static boolean chapterUnlocked(ServerPlayer player) {
        var snapshot = CampaignProgressStore.snapshot(player.getUUID());
        return snapshot.clearedEncounters().contains("BATTLE_B03") || snapshot.quests().completed().contains("MQ_C03_03_oro7");
    }

    private static boolean e014Present(V04Catalogs.Encounter spec) { return spec.enemies().stream().anyMatch("E014"::equals); }

    private static void clearVanillaMobs(ServerLevel level) {
        AABB area = new AABB(AsterMarchRegionCatalog.QUARRY.minX() - 12, 46, 290,
                AsterMarchRegionCatalog.QUARRY.maxX() + 12, 102, AsterMarchRegionCatalog.QUARRY.maxZ() + 12);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (!(mob instanceof BattleActorEntity)) mob.discard();
        }
    }

    private static final class Session {
        private final EmberQuarryChapterWorld.BuiltChapter chapter;
        private final Map<String, EncounterActor> encounters = new LinkedHashMap<>();
        private final Map<UUID, Integer> coreActors = new LinkedHashMap<>();
        private UUID relay;

        private Session(EmberQuarryChapterWorld.BuiltChapter chapter) {
            this.chapter = chapter;
            for (EmberQuarryChapterWorld.EncounterPoint point : chapter.encounters()) encounters.put(point.id(), new EncounterActor(point));
        }

        private boolean questComplete(ServerPlayer player, String id) { return CampaignProgressStore.snapshot(player.getUUID()).quests().completed().contains(id); }
        private boolean flag(ServerPlayer player, String id) { return CampaignProgressStore.snapshot(player.getUUID()).quests().unlockFlags().contains(id); }
        private int counter(ServerPlayer player, String key) { return CampaignProgressStore.quests(player.getUUID()).counters().getOrDefault(key, 0); }
        private boolean ashRouteComplete(ServerPlayer player) { return flag(player, "FT_QUARRY") || questComplete(player, "MQ_C04_01_ash_route"); }
        private boolean bossOpen(ServerPlayer player) { return flag(player, "B04_GATE") || questComplete(player, CORE_QUEST); }
        private boolean cleared(ServerPlayer player, String id) { return CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters().contains(id); }

        private boolean unlocked(ServerPlayer player, String id) {
            if (id.equals("ENC_Q01") || id.equals("ENC_Q02")) return true;
            if (id.equals(BOSS_ID)) return bossOpen(player);
            return ashRouteComplete(player);
        }

        private void refresh(ServerLevel level, ServerPlayer player) {
            EmberQuarryChapterWorld.setAshGateOpen(level, ashRouteComplete(player));
            EmberQuarryChapterWorld.setBossGateOpen(level, bossOpen(player));
        }

        private void spawnAll(ServerLevel level, ServerPlayer player) { spawnRelay(level); spawnMissing(level, player); }

        private void spawnMissing(ServerLevel level, ServerPlayer player) {
            spawnCoreMissing(level, player);
            for (EncounterActor actor : encounters.values()) {
                if (!unlocked(player, actor.point.id()) || cleared(player, actor.point.id()) || actor.engaged) { actor.despawn(level); continue; }
                actor.spawn(level);
            }
        }

        private void spawnRelay(ServerLevel level) {
            if (relay != null && level.getEntity(relay) != null) return;
            ArmorStand actor = actor(level, chapter.fastTravel(), "잿불 채석장 계전소 · FT_QUARRY", Items.AMETHYST_SHARD, ChatFormatting.LIGHT_PURPLE);
            level.addFreshEntity(actor); relay = actor.getUUID();
        }

        private void spawnCoreMissing(ServerLevel level, ServerPlayer player) {
            if (questComplete(player, CORE_QUEST)) return;
            int killed = Math.min(2, counter(player, CORE_QUEST + "|E014"));
            int looted = Math.min(2, counter(player, CORE_QUEST + "|CORE_FRAGMENT"));
            for (int i = looted; i < killed; i++) {
                if (coreActors.containsValue(i)) continue;
                Vec3 pos = chapter.corePickupPositions().get(i);
                ArmorStand actor = actor(level, pos, "Relay 핵 파편 " + (i + 1) + "/2", Items.BLAZE_POWDER, ChatFormatting.GOLD);
                level.addFreshEntity(actor); coreActors.put(actor.getUUID(), i);
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
                    boolean started = BattleSessionManager.startEncounterAt(player, actor.point.id(), true, true, actor.point.battleAnchor(), actor.point.battleYaw());
                    if (started) { actor.despawn(level); actor.engaged = true; return; }
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
                    new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_QUARRY, "잿불 채석장 계전소", ashRouteComplete(player), nearFt),
                    new FieldUiSnapshot.Travel(AsterMarchRegionCatalog.FT_RADIA, "라디아 귀환", true, false));
            int normalClears = 0; for (String id : NORMAL_IDS) if (clears.contains(id)) normalClears++;
            return new FieldUiSnapshot(true, mode, normalClears, 5, bossOpen(player), clears.contains(BOSS_ID), 0, 0,
                    objective(player), dialogue(player), reward == null ? FieldUiSnapshot.Reward.none() : reward, views, travels);
        }

        private FieldUiSnapshot.Encounter view(ServerPlayer player, String id, Set<String> clears) {
            V04Catalogs.Encounter spec = CampaignEncounterCatalog.spec(id);
            return new FieldUiSnapshot.Encounter(id, spec.label(), clears.contains(id), unlocked(player, id), spec.boss());
        }

        private String objective(ServerPlayer player) {
            if (!questComplete(player, "MQ_C04_01_ash_route")) {
                int count = counter(player, "MQ_C04_01_ash_route");
                return "MQ_C04_01 재의 길 · E012/E013 포함 전투 승리 " + Math.min(2, count) + "/2";
            }
            if (!questComplete(player, CORE_QUEST)) {
                int kills = Math.min(2, counter(player, CORE_QUEST + "|E014"));
                int loot = Math.min(2, counter(player, CORE_QUEST + "|CORE_FRAGMENT"));
                return "MQ_C04_02 핵 파편 · E014 처치 " + kills + "/2 · 핵 회수 " + loot + "/2";
            }
            if (!questComplete(player, "MQ_C04_03_kolvak")) return "MQ_C04_03 콜바크 · B04 재의 거상 콜바크 격파";
            return "Chapter 4 완료 · 라디아로 귀환하거나 구 중계소 진입을 준비";
        }

        private String dialogue(ServerPlayer player) {
            if (!ashRouteComplete(player)) return "채석장 표층의 추격대와 잉걸술사 전선을 먼저 끊어 계전소를 확보해.";
            if (!bossOpen(player)) return "용암굴착수의 붉은 열핵 안에 Relay 조각이 있다. 두 개 모두 직접 회수해야 한다.";
            if (!questComplete(player, "MQ_C04_03_kolvak")) return "핵 파편이 봉쇄문을 열었다. 채석장 심부의 콜바크가 움직이기 시작했다.";
            return "콜바크 내부 Relay 흔적을 확보했다. 구 중계소로 이어지는 단서가 남았다.";
        }

        private void despawnAll(ServerLevel level) {
            if (relay != null) { Entity e=level.getEntity(relay); if(e!=null)e.discard(); relay=null; }
            for (UUID id : List.copyOf(coreActors.keySet())) { Entity e=level.getEntity(id); if(e!=null)e.discard(); }
            coreActors.clear();
            for (EncounterActor actor : encounters.values()) actor.despawn(level);
        }
    }

    private static final class EncounterActor {
        private final EmberQuarryChapterWorld.EncounterPoint point;
        private FieldEncounterPresentation.Group group;
        private boolean engaged;
        private int graceTicks;
        private EncounterActor(EmberQuarryChapterWorld.EncounterPoint point) { this.point = point; }
        private void spawn(ServerLevel level) {
            if (group != null && group.alive(level)) return;
            if (group != null) group.despawn(level);
            group = FieldEncounterPresentation.spawn(level, point.id(), point.fieldPosition(), point.battleYaw());
        }
        private void despawn(ServerLevel level) { if(group!=null){group.despawn(level);group=null;} }
    }

    private static ArmorStand actor(ServerLevel level, Vec3 pos, String name, Item item, ChatFormatting color) {
        ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
        stand.setInvulnerable(true); stand.setNoGravity(true); stand.setShowArms(true);
        stand.setCustomName(Component.literal(name).withStyle(color)); stand.setCustomNameVisible(true);
        stand.setItemSlot(EquipmentSlot.MAINHAND, item.getDefaultInstance()); stand.setItemSlot(EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET.getDefaultInstance());
        return stand;
    }
}
