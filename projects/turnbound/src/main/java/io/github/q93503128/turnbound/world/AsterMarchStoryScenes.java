package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.TurnboundBattleActors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight 3D story beats for the five authored field chapters.
 *
 * Main-story quest state stays authoritative in CampaignProgressStore. This layer only stages the player's current
 * party as visual actors for short, non-blocking conversations on chapter entry, before a boss and after a boss.
 * Dialogue deliberately restates already-authored objectives/route information instead of inventing new mechanics.
 */
public final class AsterMarchStoryScenes {
    private enum Region { SOUTHGATE, GLOAMWOOD, AQUEDUCT, QUARRY, RELAY }
    private enum Moment { INTRO, BOSS_PRE, BOSS_POST }
    private record Key(Region region, Moment moment) {}
    private record SceneDef(Key key, Vec3 focus, List<String> lines) {}

    private static final int LINE_TICKS = 45;
    private static final int OUTRO_TICKS = 30;
    private static final double BOSS_TRIGGER_RADIUS_SQ = 30.0 * 30.0;
    private static final double POST_TRIGGER_RADIUS_SQ = 42.0 * 42.0;
    private static final double ABORT_MOVE_RADIUS_SQ = 13.0 * 13.0;

    private static final Map<Key, SceneDef> DEFINITIONS = definitions();
    private static final Map<UUID, EnumSet<MomentMarker>> SEEN = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveScene> ACTIVE = new ConcurrentHashMap<>();

    /** Stable marker enum keeps the presentation-seen set compact without persisting any gameplay state. */
    private enum MomentMarker {
        SOUTHGATE_INTRO, SOUTHGATE_PRE, SOUTHGATE_POST,
        GLOAMWOOD_INTRO, GLOAMWOOD_PRE, GLOAMWOOD_POST,
        AQUEDUCT_INTRO, AQUEDUCT_PRE, AQUEDUCT_POST,
        QUARRY_INTRO, QUARRY_PRE, QUARRY_POST,
        RELAY_INTRO, RELAY_PRE, RELAY_POST
    }

    private AsterMarchStoryScenes() {}

    public static void tick(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        ActiveScene active = ACTIVE.get(player.getUUID());
        if (active != null) {
            if (player.position().distanceToSqr(active.origin) > ABORT_MOVE_RADIUS_SQ) {
                finish(level, player.getUUID());
                return;
            }
            active.tick(level, player);
            if (active.finished(player.tickCount)) finish(level, player.getUUID());
            return;
        }

        Region region = currentRegion(player);
        if (region == null) return;
        Key key = nextScene(player, region);
        if (key == null) return;
        start(level, player, DEFINITIONS.get(key));
    }

    /** Battle entry must never leave duplicate party actors standing in the field while BattlePresentation owns them. */
    public static void cancelForBattle(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        finish(level, player.getUUID());
    }

    public static void remove(ServerPlayer player) {
        if (player == null) return;
        if (player.level() instanceof ServerLevel level) finish(level, player.getUUID());
        SEEN.remove(player.getUUID());
    }

    private static void start(ServerLevel level, ServerPlayer player, SceneDef def) {
        if (def == null) return;
        List<String> cast = storyCast(player);
        if (cast.isEmpty()) return;

        List<UUID> actors = new ArrayList<>();
        List<String> names = new ArrayList<>();
        Vec3 forward = playerForward(player);
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        Vec3 conversationCenter = player.position().add(forward.scale(1.45));
        Vec3 base = player.position().add(forward.scale(3.35));

        for (int i = 0; i < cast.size(); i++) {
            String id = cast.get(i);
            Vec3 pos = switch (i) {
                case 0 -> base.add(right.scale(-1.55));
                case 1 -> base.add(right.scale(1.55));
                default -> base.add(forward.scale(1.2));
            };
            float yaw = yawToward(pos, conversationCenter);
            BattleActorEntity actor = TurnboundBattleActors.spawn(level, id, pos, yaw);
            if (actor == null) continue;
            actor.setFieldWalking(false);
            actor.setCustomNameVisible(false);
            actors.add(actor.getUUID());
            names.add(CanonicalData.definition(id, 1, 0, false).name());
        }
        if (actors.isEmpty()) return;

        markSeen(player.getUUID(), def.key());
        ActiveScene scene = new ActiveScene(def, List.copyOf(actors), List.copyOf(names), player.position(), player.tickCount);
        ACTIVE.put(player.getUUID(), scene);
        scene.showLine(level, player, 0);
    }

    private static List<String> storyCast(ServerPlayer player) {
        List<String> party = CampaignProgressStore.activeParty(player.getUUID());
        List<String> out = new ArrayList<>();
        // Core characters read best in a story beat; filler units remain a fallback for unusual parties.
        for (String id : party) {
            if (id.startsWith("P") && TurnboundBattleActors.contains(id) && !out.contains(id)) out.add(id);
            if (out.size() >= 3) return List.copyOf(out);
        }
        for (String id : party) {
            if (TurnboundBattleActors.contains(id) && !out.contains(id)) out.add(id);
            if (out.size() >= 3) break;
        }
        return List.copyOf(out);
    }

    private static Key nextScene(ServerPlayer player, Region region) {
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        Set<String> quests = CampaignProgressStore.snapshot(player.getUUID()).quests().completed();
        Set<String> flags = CampaignProgressStore.snapshot(player.getUUID()).quests().unlockFlags();

        Key intro = new Key(region, Moment.INTRO);
        if (!seen(player.getUUID(), intro)) return intro;

        String boss = bossEncounter(region);
        Vec3 bossPos = DEFINITIONS.get(new Key(region, Moment.BOSS_PRE)).focus();
        boolean cleared = clears.contains(boss);
        if (!cleared && bossOpen(region, clears, quests, flags)
                && player.position().distanceToSqr(bossPos) <= BOSS_TRIGGER_RADIUS_SQ) {
            Key pre = new Key(region, Moment.BOSS_PRE);
            if (!seen(player.getUUID(), pre)) return pre;
        }
        if (cleared && player.position().distanceToSqr(bossPos) <= POST_TRIGGER_RADIUS_SQ) {
            Key post = new Key(region, Moment.BOSS_POST);
            if (!seen(player.getUUID(), post)) return post;
        }
        return null;
    }

    private static boolean bossOpen(Region region, Set<String> clears, Set<String> quests, Set<String> flags) {
        return switch (region) {
            case SOUTHGATE -> clears.contains("ENC_M04") || quests.contains("MQ_C01_02_unstable") || flags.contains("B01_PATH");
            case GLOAMWOOD -> quests.contains("MQ_C02_02_root_wall") || flags.contains("B02_GATE");
            case AQUEDUCT -> quests.contains("MQ_C03_02_old_orders") || flags.contains("ORO_ROOM");
            case QUARRY -> quests.contains("MQ_C04_02_core_fragment") || flags.contains("B04_GATE");
            case RELAY -> quests.contains("MQ_C05_02_serak_record") || flags.contains("B05_GATE");
        };
    }

    private static Region currentRegion(ServerPlayer player) {
        if (FieldSessionManager.active(player)) return Region.SOUTHGATE;
        if (GloamwoodSessionManager.active(player)) return Region.GLOAMWOOD;
        if (BrokenAqueductSessionManager.active(player)) return Region.AQUEDUCT;
        if (EmberQuarrySessionManager.active(player)) return Region.QUARRY;
        if (OldRelayStationSessionManager.active(player)) return Region.RELAY;
        return null;
    }

    private static String bossEncounter(Region region) {
        return switch (region) {
            case SOUTHGATE -> "BATTLE_B01";
            case GLOAMWOOD -> "BATTLE_B02";
            case AQUEDUCT -> "BATTLE_B03";
            case QUARRY -> "BATTLE_B04";
            case RELAY -> "BATTLE_B05";
        };
    }

    private static boolean seen(UUID playerId, Key key) {
        EnumSet<MomentMarker> markers = SEEN.get(playerId);
        return markers != null && markers.contains(marker(key));
    }

    private static void markSeen(UUID playerId, Key key) {
        SEEN.computeIfAbsent(playerId, ignored -> EnumSet.noneOf(MomentMarker.class)).add(marker(key));
    }

    private static MomentMarker marker(Key key) {
        return switch (key.region()) {
            case SOUTHGATE -> switch (key.moment()) {
                case INTRO -> MomentMarker.SOUTHGATE_INTRO; case BOSS_PRE -> MomentMarker.SOUTHGATE_PRE; case BOSS_POST -> MomentMarker.SOUTHGATE_POST;
            };
            case GLOAMWOOD -> switch (key.moment()) {
                case INTRO -> MomentMarker.GLOAMWOOD_INTRO; case BOSS_PRE -> MomentMarker.GLOAMWOOD_PRE; case BOSS_POST -> MomentMarker.GLOAMWOOD_POST;
            };
            case AQUEDUCT -> switch (key.moment()) {
                case INTRO -> MomentMarker.AQUEDUCT_INTRO; case BOSS_PRE -> MomentMarker.AQUEDUCT_PRE; case BOSS_POST -> MomentMarker.AQUEDUCT_POST;
            };
            case QUARRY -> switch (key.moment()) {
                case INTRO -> MomentMarker.QUARRY_INTRO; case BOSS_PRE -> MomentMarker.QUARRY_PRE; case BOSS_POST -> MomentMarker.QUARRY_POST;
            };
            case RELAY -> switch (key.moment()) {
                case INTRO -> MomentMarker.RELAY_INTRO; case BOSS_PRE -> MomentMarker.RELAY_PRE; case BOSS_POST -> MomentMarker.RELAY_POST;
            };
        };
    }

    private static void finish(ServerLevel level, UUID playerId) {
        ActiveScene scene = ACTIVE.remove(playerId);
        if (scene == null) return;
        for (UUID actorId : scene.actors) {
            Entity entity = level.getEntity(actorId);
            if (entity != null) entity.discard();
        }
    }

    private static Vec3 playerForward(ServerPlayer player) {
        double rad = Math.toRadians(player.getYRot());
        Vec3 forward = new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
        return forward.lengthSqr() > 0.00001 ? forward.normalize() : new Vec3(0, 0, 1);
    }

    private static float yawToward(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float)Math.toDegrees(Math.atan2(-dx, dz));
    }

    private static Map<Key, SceneDef> definitions() {
        Map<Key, SceneDef> out = new LinkedHashMap<>();
        put(out, Region.SOUTHGATE, Moment.INTRO, new Vec3(190, 67, 230),
                "남문 밖이야. 먼저 순찰선을 확보하자.",
                "적 편성을 보고 들어가. 초원은 숨을 곳이 적어.",
                "계전소까지 길을 만들면 심부로 갈 수 있어.");
        put(out, Region.SOUTHGATE, Moment.BOSS_PRE, AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B01).position(),
                "저 앞이 그라울의 봉쇄선이야.",
                "돌진을 허용하면 진형이 무너져. 준비하고 들어가.");
        put(out, Region.SOUTHGATE, Moment.BOSS_POST, AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B01).position(),
                "그라울이 쓰러졌어.",
                "라디아 북쪽 신호가 살아났다. 다음은 그늘숲이야.");

        put(out, Region.GLOAMWOOD, Moment.INTRO, new Vec3(-40, 70, -300),
                "포자등불부터 찾아. 깊은 길이 잠겨 있어.",
                "뿌리수호병이 보이면 진형을 흐트러뜨리지 마.",
                "베르나는 숲 안쪽에 있어. 서두르지 말자.");
        put(out, Region.GLOAMWOOD, Moment.BOSS_PRE, AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B02).position(),
                "뿌리 장벽이 열렸어.",
                "베르나의 중심부야. 행동 순서를 보고 들어가자.");
        put(out, Region.GLOAMWOOD, Moment.BOSS_POST, AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B02).position(),
                "숲의 맥동이 멎었어.",
                "서쪽 수로 신호가 들어온다. 라디아에서 경로를 바꾸자.");

        put(out, Region.AQUEDUCT, Moment.INTRO, new Vec3(-320, 67, 20),
                "압력 밸브 두 기가 먼저야.",
                "자동 방위 개체가 아직 움직인다.",
                "하층을 열면 ORO-7까지 닿을 수 있어.");
        put(out, Region.AQUEDUCT, Moment.BOSS_PRE, AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B03).position(),
                "ORO Room 접근이 풀렸어.",
                "보호 프로토콜이 남아 있을 거야. 외갑 변화를 봐.");
        put(out, Region.AQUEDUCT, Moment.BOSS_POST, AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B03).position(),
                "수문 명령이 정지했어.",
                "남쪽 채석장 통로가 열렸다. 계속 추적하자.");

        put(out, Region.QUARRY, Moment.INTRO, new Vec3(20, 70, 405),
                "표층 전선을 끊어 계전소를 확보하자.",
                "용암굴착수의 핵 안에 Relay 조각이 있어.",
                "두 개를 회수하면 콜바크의 길이 열린다.");
        put(out, Region.QUARRY, Moment.BOSS_PRE, AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B04).position(),
                "열핵 반응이 바로 앞이야.",
                "콜바크가 움직이기 시작했다. 한 번씩 보고 대응해.");
        put(out, Region.QUARRY, Moment.BOSS_POST, AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B04).position(),
                "콜바크의 열이 빠진다.",
                "Relay 조각을 모아 라디아 중앙 계전소로 돌아가자.");

        put(out, Region.RELAY, Moment.INTRO, new Vec3(365, 68, -305),
                "기록실 네 곳부터 복원하자.",
                "혼성 전투실이 이어져 있어. 조합을 보고 움직여.",
                "세라크는 마지막 관측실 너머야.");
        put(out, Region.RELAY, Moment.BOSS_PRE, AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B05).position(),
                "관측실의 균열이 한곳으로 모여.",
                "세라크전이다. 마지막 Relay를 여기서 끝내자.");
        put(out, Region.RELAY, Moment.BOSS_POST, AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B05).position(),
                "세라크는 끝났어.",
                "아직 마지막 콘솔이 남아 있어. Relay를 직접 재연결하자.");
        return Map.copyOf(out);
    }

    private static void put(Map<Key, SceneDef> out, Region region, Moment moment, Vec3 focus, String... lines) {
        Key key = new Key(region, moment);
        out.put(key, new SceneDef(key, focus, List.of(lines)));
    }

    private static final class ActiveScene {
        private final SceneDef def;
        private final List<UUID> actors;
        private final List<String> names;
        private final Vec3 origin;
        private final int startTick;
        private int shownLine = -1;

        private ActiveScene(SceneDef def, List<UUID> actors, List<String> names, Vec3 origin, int startTick) {
            this.def = def;
            this.actors = actors;
            this.names = names;
            this.origin = origin;
            this.startTick = startTick;
        }

        private void tick(ServerLevel level, ServerPlayer player) {
            int elapsed = Math.max(0, player.tickCount - startTick);
            int line = elapsed / LINE_TICKS;
            if (line < def.lines().size() && line != shownLine) showLine(level, player, line);
            if (line >= def.lines().size()) {
                for (UUID id : actors) {
                    Entity entity = level.getEntity(id);
                    if (entity == null) continue;
                    entity.setCustomNameVisible(false);
                    float yaw = yawToward(entity.position(), def.focus());
                    entity.setYRot(yaw);
                    entity.setYHeadRot(yaw);
                    if (entity instanceof BattleActorEntity actor) actor.setYBodyRot(yaw);
                }
            }
        }

        private void showLine(ServerLevel level, ServerPlayer player, int line) {
            if (line < 0 || line >= def.lines().size() || actors.isEmpty()) return;
            shownLine = line;
            int speaker = line % actors.size();
            Entity speakerEntity = level.getEntity(actors.get(speaker));
            if (speakerEntity == null) return;

            for (int i = 0; i < actors.size(); i++) {
                Entity entity = level.getEntity(actors.get(i));
                if (entity == null) continue;
                boolean speaking = i == speaker;
                entity.setCustomNameVisible(speaking);
                if (speaking) {
                    String name = i < names.size() ? names.get(i) : "Party";
                    entity.setCustomName(Component.literal(name + " · " + def.lines().get(line))
                            .withStyle(ChatFormatting.WHITE));
                    float yaw = yawToward(entity.position(), player.position());
                    entity.setYRot(yaw); entity.setYHeadRot(yaw);
                    if (entity instanceof BattleActorEntity actor) actor.setYBodyRot(yaw);
                } else {
                    float yaw = yawToward(entity.position(), speakerEntity.position());
                    entity.setYRot(yaw); entity.setYHeadRot(yaw);
                    if (entity instanceof BattleActorEntity actor) actor.setYBodyRot(yaw);
                }
            }
        }

        private boolean finished(int tick) {
            return tick - startTick >= def.lines().size() * LINE_TICKS + OUTRO_TICKS;
        }
    }
}
