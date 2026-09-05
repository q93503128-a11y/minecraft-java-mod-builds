package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.PersonalPresentationIsolation;
import io.github.q93503128.turnbound.presentation.TurnboundBattleActors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
 * party as visual actors for short, non-blocking conversations on chapter entry, at a major route turn, before a boss,
 * after a boss and at the final Relay reconnect. No quest counter, reward or encounter state is mutated here.
 */
public final class AsterMarchStoryScenes {
    private enum Region { SOUTHGATE, GLOAMWOOD, AQUEDUCT, QUARRY, RELAY }
    private enum Moment { INTRO, MID, BOSS_PRE, BOSS_POST, FINALE }
    private record Key(Region region, Moment moment) {}
    private record SceneDef(Key key, Vec3 focus, List<String> lines) {}

    private static final int LINE_TICKS = 45;
    private static final int OUTRO_TICKS = 30;
    private static final double MID_TRIGGER_RADIUS_SQ = 34.0 * 34.0;
    private static final double BOSS_TRIGGER_RADIUS_SQ = 30.0 * 30.0;
    private static final double POST_TRIGGER_RADIUS_SQ = 42.0 * 42.0;
    private static final double FINALE_TRIGGER_RADIUS_SQ = 30.0 * 30.0;
    private static final double ABORT_MOVE_RADIUS_SQ = 13.0 * 13.0;

    private static final Map<Key, SceneDef> DEFINITIONS = definitions();
    private static final Map<UUID, EnumSet<MomentMarker>> SEEN = new ConcurrentHashMap<>();
    private static final Map<UUID, ActiveScene> ACTIVE = new ConcurrentHashMap<>();

    /** Stable marker enum keeps the presentation-seen set compact without persisting any gameplay state. */
    private enum MomentMarker {
        SOUTHGATE_INTRO, SOUTHGATE_MID, SOUTHGATE_PRE, SOUTHGATE_POST, SOUTHGATE_FINALE,
        GLOAMWOOD_INTRO, GLOAMWOOD_MID, GLOAMWOOD_PRE, GLOAMWOOD_POST, GLOAMWOOD_FINALE,
        AQUEDUCT_INTRO, AQUEDUCT_MID, AQUEDUCT_PRE, AQUEDUCT_POST, AQUEDUCT_FINALE,
        QUARRY_INTRO, QUARRY_MID, QUARRY_PRE, QUARRY_POST, QUARRY_FINALE,
        RELAY_INTRO, RELAY_MID, RELAY_PRE, RELAY_POST, RELAY_FINALE
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
        player.sendSystemMessage(Component.literal("― " + sceneTitle(def.key()) + " ―")
                .withStyle(sceneColor(def.key().region()), ChatFormatting.BOLD));
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
        var snapshot = CampaignProgressStore.snapshot(player.getUUID());
        Set<String> clears = snapshot.clearedEncounters();
        Set<String> quests = snapshot.quests().completed();
        Set<String> flags = snapshot.quests().unlockFlags();

        Key intro = new Key(region, Moment.INTRO);
        if (!seen(player.getUUID(), intro)) return intro;

        Key mid = new Key(region, Moment.MID);
        SceneDef midDef = DEFINITIONS.get(mid);
        if (midDef != null && !seen(player.getUUID(), mid)
                && midOpen(player, region, clears, quests, flags)
                && player.position().distanceToSqr(midDef.focus()) <= MID_TRIGGER_RADIUS_SQ) {
            return mid;
        }

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

        if (region == Region.RELAY && finaleOpen(quests, flags)) {
            Key finale = new Key(region, Moment.FINALE);
            SceneDef finaleDef = DEFINITIONS.get(finale);
            if (finaleDef != null && !seen(player.getUUID(), finale)
                    && player.position().distanceToSqr(finaleDef.focus()) <= FINALE_TRIGGER_RADIUS_SQ) {
                return finale;
            }
        }
        return null;
    }

    private static boolean midOpen(ServerPlayer player, Region region, Set<String> clears, Set<String> quests, Set<String> flags) {
        return switch (region) {
            case SOUTHGATE -> (clears.contains("ENC_M01") && clears.contains("ENC_M02"))
                    || flags.contains("FT_MEADOW");
            case GLOAMWOOD -> quests.contains("MQ_C02_01_spores") || flags.contains("GLOAM_DEEP_PATH");
            case AQUEDUCT -> quests.contains("MQ_C03_01_dry_channel") || flags.contains("AQUEDUCT_LOWER");
            case QUARRY -> quests.contains("MQ_C04_01_ash_route") || flags.contains("FT_QUARRY");
            case RELAY -> CampaignProgressStore.quests(player.getUUID()).counters()
                    .getOrDefault("MQ_C05_02_serak_record", 0) >= 2;
        };
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

    private static boolean finaleOpen(Set<String> quests, Set<String> flags) {
        return quests.contains("MQ_C05_03_reconnect") || flags.contains("ENDGAME");
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
                case INTRO -> MomentMarker.SOUTHGATE_INTRO;
                case MID -> MomentMarker.SOUTHGATE_MID;
                case BOSS_PRE -> MomentMarker.SOUTHGATE_PRE;
                case BOSS_POST -> MomentMarker.SOUTHGATE_POST;
                case FINALE -> MomentMarker.SOUTHGATE_FINALE;
            };
            case GLOAMWOOD -> switch (key.moment()) {
                case INTRO -> MomentMarker.GLOAMWOOD_INTRO;
                case MID -> MomentMarker.GLOAMWOOD_MID;
                case BOSS_PRE -> MomentMarker.GLOAMWOOD_PRE;
                case BOSS_POST -> MomentMarker.GLOAMWOOD_POST;
                case FINALE -> MomentMarker.GLOAMWOOD_FINALE;
            };
            case AQUEDUCT -> switch (key.moment()) {
                case INTRO -> MomentMarker.AQUEDUCT_INTRO;
                case MID -> MomentMarker.AQUEDUCT_MID;
                case BOSS_PRE -> MomentMarker.AQUEDUCT_PRE;
                case BOSS_POST -> MomentMarker.AQUEDUCT_POST;
                case FINALE -> MomentMarker.AQUEDUCT_FINALE;
            };
            case QUARRY -> switch (key.moment()) {
                case INTRO -> MomentMarker.QUARRY_INTRO;
                case MID -> MomentMarker.QUARRY_MID;
                case BOSS_PRE -> MomentMarker.QUARRY_PRE;
                case BOSS_POST -> MomentMarker.QUARRY_POST;
                case FINALE -> MomentMarker.QUARRY_FINALE;
            };
            case RELAY -> switch (key.moment()) {
                case INTRO -> MomentMarker.RELAY_INTRO;
                case MID -> MomentMarker.RELAY_MID;
                case BOSS_PRE -> MomentMarker.RELAY_PRE;
                case BOSS_POST -> MomentMarker.RELAY_POST;
                case FINALE -> MomentMarker.RELAY_FINALE;
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

    private static String sceneTitle(Key key) {
        return switch (key.region()) {
            case SOUTHGATE -> switch (key.moment()) {
                case INTRO -> "남문 초원 · 출정";
                case MID -> "남문 초원 · 심부 개방";
                case BOSS_PRE -> "그라울 봉쇄선";
                case BOSS_POST -> "Chapter 1 · 전선 정리";
                case FINALE -> "남문 초원";
            };
            case GLOAMWOOD -> switch (key.moment()) {
                case INTRO -> "그늘숲 · 포자등불";
                case MID -> "그늘숲 · 깊은 길";
                case BOSS_PRE -> "베르나 중심부";
                case BOSS_POST -> "Chapter 2 · 맥동 정지";
                case FINALE -> "그늘숲";
            };
            case AQUEDUCT -> switch (key.moment()) {
                case INTRO -> "부서진 수로 · 압력선";
                case MID -> "부서진 수로 · 하층 개방";
                case BOSS_PRE -> "ORO-7 관리실";
                case BOSS_POST -> "Chapter 3 · 수문 정지";
                case FINALE -> "부서진 수로";
            };
            case QUARRY -> switch (key.moment()) {
                case INTRO -> "잿불 채석장 · 표층 전선";
                case MID -> "잿불 채석장 · 심부 진입";
                case BOSS_PRE -> "콜바크 열핵";
                case BOSS_POST -> "Chapter 4 · 열핵 정지";
                case FINALE -> "잿불 채석장";
            };
            case RELAY -> switch (key.moment()) {
                case INTRO -> "구 중계소 · 기록 복원";
                case MID -> "구 중계소 · 봉쇄 기록";
                case BOSS_PRE -> "세라크 관측실";
                case BOSS_POST -> "Chapter 5 · 마지막 콘솔";
                case FINALE -> "Aster March · Relay 재연결";
            };
        };
    }

    private static ChatFormatting sceneColor(Region region) {
        return switch (region) {
            case SOUTHGATE -> ChatFormatting.GOLD;
            case GLOAMWOOD -> ChatFormatting.DARK_GREEN;
            case AQUEDUCT -> ChatFormatting.AQUA;
            case QUARRY -> ChatFormatting.RED;
            case RELAY -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    private static ParticleOptions sceneParticle(Region region) {
        return switch (region) {
            case SOUTHGATE -> ParticleTypes.CLOUD;
            case GLOAMWOOD -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case AQUEDUCT -> ParticleTypes.ELECTRIC_SPARK;
            case QUARRY -> ParticleTypes.ASH;
            case RELAY -> ParticleTypes.PORTAL;
        };
    }

    private static Map<Key, SceneDef> definitions() {
        Map<Key, SceneDef> out = new LinkedHashMap<>();
        put(out, Region.SOUTHGATE, Moment.INTRO, new Vec3(190, 67, 230),
                "남문 밖이야. 먼저 순찰선을 확보하자.",
                "적 편성을 보고 들어가. 초원은 숨을 곳이 적어.",
                "계전소까지 길을 만들면 심부로 갈 수 있어.");
        put(out, Region.SOUTHGATE, Moment.MID, new Vec3(40, 67, 188),
                "앞쪽 봉쇄가 풀렸어. 이제 초원 심부로 들어갈 수 있어.",
                "계전소를 기준점으로 잡자. 불안정한 흔적이 안쪽에 몰려 있어.",
                "그라울의 길은 아직 닫혀 있어. 순찰선을 더 밀자.");
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
        put(out, Region.GLOAMWOOD, Moment.MID, new Vec3(-40, 70, -286),
                "포자등불의 신호가 이어졌어. 깊은 길이 열린다.",
                "안쪽은 뿌리수호병이 막고 있어. 같은 방식으로는 못 지나가.",
                "수호병이 있는 전투를 돌파하면 베르나의 관문까지 닿을 수 있어.");
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
        put(out, Region.AQUEDUCT, Moment.MID, new Vec3(-300, 66, 20),
                "두 밸브가 맞물렸어. 하층 수로의 잠금이 풀린다.",
                "오래된 방위 명령이 아직 길을 잡고 있어.",
                "명령을 끊으면 ORO-7 관리실까지 직선으로 이어질 거야.");
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
        put(out, Region.QUARRY, Moment.MID, new Vec3(20, 69, 395),
                "표층 전선 정리 완료. 계전소 신호가 안정됐어.",
                "Relay 핵 조각은 심부 굴착수 쪽에 남아 있어.",
                "두 조각을 회수하면 콜바크의 봉쇄 장치까지 밀어붙일 수 있어.");
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
        put(out, Region.RELAY, Moment.MID, new Vec3(365, 68, -305),
                "두 번째 기록까지 복원됐어. 봉쇄 순서가 보이기 시작한다.",
                "세라크는 중계소 전체를 한 번에 잠근 게 아니야. 기록실마다 잠금을 겹쳐 놨어.",
                "남은 기록을 복원하면 관측실의 마지막 봉쇄도 끊을 수 있어.");
        put(out, Region.RELAY, Moment.BOSS_PRE, AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B05).position(),
                "관측실의 균열이 한곳으로 모여.",
                "세라크전이다. 마지막 Relay를 여기서 끝내자.");
        put(out, Region.RELAY, Moment.BOSS_POST, AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B05).position(),
                "세라크는 끝났어.",
                "아직 마지막 콘솔이 남아 있어. Relay를 직접 재연결하자.");
        put(out, Region.RELAY, Moment.FINALE, new Vec3(458, 66, -350),
                "Relay가 다시 이어졌어.",
                "동쪽 외부 지역의 신호가 돌아온다. 이번엔 잡음이 아니야.",
                "라디아로 돌아가자. 새 전투 신호들이 기다리고 있어.");
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
            if (elapsed % 10 == 0) scenePulse(level, player, def, origin, elapsed);
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
                    entity.setCustomName(Component.literal(name).withStyle(sceneColor(def.key().region()), ChatFormatting.BOLD));
                    player.sendSystemMessage(Component.literal(name).withStyle(sceneColor(def.key().region()), ChatFormatting.BOLD)
                            .append(Component.literal(" · " + def.lines().get(line)).withStyle(ChatFormatting.WHITE)));
                    float yaw = yawToward(entity.position(), player.position());
                    entity.setYRot(yaw);
                    entity.setYHeadRot(yaw);
                    if (entity instanceof BattleActorEntity actor) actor.setYBodyRot(yaw);
                } else {
                    float yaw = yawToward(entity.position(), speakerEntity.position());
                    entity.setYRot(yaw);
                    entity.setYHeadRot(yaw);
                    if (entity instanceof BattleActorEntity actor) actor.setYBodyRot(yaw);
                }
            }
        }

        private boolean finished(int tick) {
            return tick - startTick >= def.lines().size() * LINE_TICKS + OUTRO_TICKS;
        }
    }

    private static void scenePulse(ServerLevel level, ServerPlayer player, SceneDef def, Vec3 origin, int elapsed) {
        ParticleOptions particle = sceneParticle(def.key().region());
        double phase = elapsed * 0.09;
        PersonalPresentationIsolation.particles(level, player, particle,
                origin.x + Math.cos(phase) * 1.8,
                origin.y + 1.0,
                origin.z + Math.sin(phase) * 1.8,
                2, 0.25, 0.25, 0.25, 0.008);

        Vec3 flat = new Vec3(def.focus().x - origin.x, 0, def.focus().z - origin.z);
        if (flat.lengthSqr() <= 0.0001) return;
        Vec3 dir = flat.normalize();
        double max = Math.min(8.0, Math.sqrt(flat.lengthSqr()));
        for (double d = 2.0; d <= max; d += 2.0) {
            Vec3 p = origin.add(dir.scale(d));
            PersonalPresentationIsolation.particles(level, player, particle,
                    p.x, p.y + 0.25, p.z, 1, 0.08, 0.08, 0.08, 0.002);
        }
    }
}
