package io.github.q93503128.turnbound.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Interactive micro-incidents layered onto the authored Aster March route landmarks.
 *
 * <p>These incidents intentionally own no quest completion, rewards, encounter clears or campaign flags. They make
 * the scenery useful: players can inspect a place, read a before/after interpretation driven by existing canonical
 * progress, and receive a short visual cue toward the next relevant part of the route. This keeps exploration dense
 * without creating a second progression authority beside the chapter/quest systems.</p>
 */
public final class AsterMarchFieldIncidents {
    private enum Region { SOUTHGATE, GLOAMWOOD, AQUEDUCT, QUARRY, RELAY }

    private enum Resolution {
        MEADOW_DEEP, MEADOW_BOSS, CHAPTER_1_CLEAR,
        GLOAM_DEEP, GLOAM_BOSS, CHAPTER_2_CLEAR,
        AQUEDUCT_LOWER, AQUEDUCT_BOSS, CHAPTER_3_CLEAR,
        QUARRY_DEEP, QUARRY_BOSS, CHAPTER_4_CLEAR,
        RELAY_ENTRANCE, RELAY_BOSS, RELAY_CLEAR, ENDGAME
    }

    private record Def(
            String id,
            Region region,
            String title,
            Vec3 pos,
            Item icon,
            Resolution resolution,
            String before,
            String after,
            Vec3 next
    ) {}

    private static final double SPAWN_RADIUS_SQ = 58.0 * 58.0;
    private static final double DESPAWN_RADIUS_SQ = 72.0 * 72.0;
    private static final double LABEL_RADIUS_SQ = 14.0 * 14.0;
    private static final double DISCOVERY_RADIUS_SQ = 10.0 * 10.0;

    private static final List<Def> DEFINITIONS = List.of(
            // Southgate Meadow: four existing micro-landmarks become readable pieces of the first front line.
            def("M_PATROL", Region.SOUTHGATE, "남문 순찰 야영지", 58, 67, 200, Items.COMPASS, Resolution.MEADOW_DEEP,
                    "접어 둔 교대표와 빈 보급통이 남아 있다. 순찰선이 더 안쪽으로 밀리기 전 이곳이 마지막 안정 지점이었다.",
                    "순찰대 표식이 다시 같은 방향을 가리킨다. 초원 심부까지 이어지는 길이 이제 끊기지 않는다.",
                    145, 67, 220),
            def("M_WAGON", Region.SOUTHGATE, "부서진 수레", 145, 67, 220, Items.MINECART, Resolution.MEADOW_BOSS,
                    "수레는 전복된 게 아니라 길 밖으로 급히 밀려났다. 바퀴 자국과 긁힌 흙이 한 방향으로 깊게 이어진다.",
                    "뒤엉킨 바퀴 자국 사이로 아군 발자국이 새로 겹쳤다. 봉쇄선 너머의 큰 흔적만 남아 있다.",
                    252, 67, 258),
            def("M_MEMORIAL", Region.SOUTHGATE, "초원 추모비", 252, 67, 258, Items.WRITABLE_BOOK, Resolution.CHAPTER_1_CLEAR,
                    "낡은 이름 아래 최근 긁힌 자국이 더해져 있다. 누군가 거대한 것을 피해 이 돌기둥 뒤로 몸을 틀었던 흔적이다.",
                    "바람에 흙먼지만 남았다. 추모비 주변을 짓누르던 거친 발굽과 돌진 흔적은 더 이어지지 않는다.",
                    326, 68, 261),
            def("M_OMEN", Region.SOUTHGATE, "그라울 돌진 흔적", 326, 68, 261, Items.BONE, Resolution.CHAPTER_1_CLEAR,
                    "울타리가 같은 높이에서 연달아 뜯겨 있다. 힘으로 밀어붙인 일직선의 충돌 흔적이 보스 구역까지 이어진다.",
                    "새 충돌 자국은 없다. 부서진 울타리 너머로 남은 길은 이제 그늘숲 방향뿐이다.",
                    344, 68, 245),

            // Gloamwood: route reading changes from spore interference to Verna's inner growth.
            def("G_LANTERN", Region.GLOAMWOOD, "등불 갈림길", -12, 68, -161, Items.SOUL_LANTERN, Resolution.GLOAM_DEEP,
                    "서로 다른 방향의 포자가 등불 불빛을 덮고 있다. 가장 짙은 흐름이 숲 깊은 길을 가리고 있다.",
                    "포자 흐름이 얇아졌다. 갈림길의 오래된 석재가 다시 한 줄의 길처럼 읽힌다.",
                    -68, 70, -226),
            def("G_CAMP", Region.GLOAMWOOD, "버려진 조사 야영지", -68, 70, -226, Items.SPYGLASS, Resolution.GLOAM_DEEP,
                    "급히 떠난 야영지치고는 도구가 정돈돼 있다. 조사대는 도망친 게 아니라 더 깊은 곳으로 이동했던 것 같다.",
                    "남은 도구 위에 포자가 거의 쌓이지 않는다. 숲의 간섭이 약해져 조사대가 택한 방향을 따라갈 수 있다.",
                    -8, 70, -286),
            def("G_CAUSEWAY", Region.GLOAMWOOD, "숲에 삼켜진 옛길", -8, 70, -286, Items.MOSSY_COBBLESTONE, Resolution.GLOAM_BOSS,
                    "돌길 틈의 뿌리가 이동 방향과 반대로 휘어 있다. 안쪽에서 길 전체를 밀어내는 힘이 작용하고 있다.",
                    "뿌리가 더는 길을 밀어내지 않는다. 갈라진 석재 사이로 중심부까지 이어지는 옛길이 드러났다.",
                    -98, 71, -392),
            def("G_THORN", Region.GLOAMWOOD, "가시 아치", -98, 71, -392, Items.VINE, Resolution.GLOAM_BOSS,
                    "아치의 가시는 바깥이 아니라 안쪽을 향한다. 이곳부터는 숲 자체가 침입자를 거르는 경계처럼 움직인다.",
                    "가시 끝이 축 늘어졌다. 안쪽의 생장 명령이 끊기며 베르나의 중심부가 직접 보이기 시작한다.",
                    -57, 72, -424),
            def("G_GROVE", Region.GLOAMWOOD, "베르나 외곽 꽃밭", -57, 72, -424, Items.FLOWERING_AZALEA, Resolution.CHAPTER_2_CLEAR,
                    "꽃이 바람과 무관하게 같은 박자로 열렸다 닫힌다. 중심부의 맥동이 이 외곽까지 전달되고 있다.",
                    "꽃잎의 움직임이 제각각으로 돌아왔다. 숲 전체를 묶던 한 개의 박동은 사라졌다.",
                    -124, 66, 20),

            // Broken Aqueduct: infrastructure itself acts as the regional storyteller.
            def("A_SERVICE", Region.AQUEDUCT, "정비 벽감", -162, 66, 4, Items.REDSTONE_TORCH, Resolution.AQUEDUCT_LOWER,
                    "정비 기록의 압력 수치가 중간에서 끊겼다. 하층으로 내려가는 계통은 아직 잠긴 상태다.",
                    "압력 눈금이 정상 범위에 머문다. 하층 수로가 다시 하나의 계통으로 연결됐다.",
                    -219, 66, 27),
            def("A_PIPE", Region.AQUEDUCT, "배관 다리", -219, 66, 27, Items.IRON_INGOT, Resolution.AQUEDUCT_LOWER,
                    "다리 아래 배관마다 서로 다른 진동이 남아 있다. 두 압력원이 따로 움직여 시설 전체가 뒤틀리고 있다.",
                    "배관 진동이 같은 주기로 맞춰졌다. 물소리도 하층 방향으로 일정하게 흐른다.",
                    -292, 66, 61),
            def("A_LOOKOUT", Region.AQUEDUCT, "범람 감시대", -292, 66, 61, Items.SPYGLASS, Resolution.AQUEDUCT_BOSS,
                    "감시대의 오래된 바늘이 한 방향에 고정돼 있다. 시설 깊은 곳에서 반복되는 방위 명령을 가리킨다.",
                    "바늘이 처음으로 멈췄다. 남은 전기 신호는 ORO-7 관리실 방향에서만 튄다.",
                    -362, 65, -8),
            def("A_MAINT", Region.AQUEDUCT, "유지보수 구역", -362, 65, -8, Items.REPEATER, Resolution.AQUEDUCT_BOSS,
                    "벽면 표시가 외부 침입보다 내부 격리를 우선하도록 설계돼 있다. 방위 체계가 시설 안쪽을 적으로 간주한 흔적이다.",
                    "격리 표시가 꺼졌다. 잠금이 해제된 뒤에도 가장 두꺼운 방호선은 관리실 앞에 남아 있다.",
                    -413, 64, 53),
            def("A_SECURITY", Region.AQUEDUCT, "ORO 보안문", -413, 64, 53, Items.IRON_DOOR, Resolution.CHAPTER_3_CLEAR,
                    "외부를 향해야 할 방호판이 안쪽으로 겹쳐 있다. ORO-7이 보호한 것은 입구가 아니라 자신의 관리 구역이었다.",
                    "방호판의 전류가 완전히 끊겼다. 수로에는 기계음 대신 일정한 물소리만 남는다.",
                    -52, 68, 296),

            // Ember Quarry: heat, logistics and worker traces build toward Kolvak.
            def("Q_REST", Region.QUARRY, "작업자 휴게지", -95, 69, 345, Items.BREAD, Resolution.QUARRY_DEEP,
                    "작업표가 중간 교대에서 끝나 있다. 사람들은 장비를 버린 게 아니라 다시 돌아올 생각으로 두고 떠났다.",
                    "표층 전선이 조용해지며 휴게지까지 안전한 왕복 동선이 생겼다. 심부의 열기만 아직 거세다.",
                    2, 69, 389),
            def("Q_COOLING", Region.QUARRY, "냉각 가설대", 2, 69, 389, Items.WATER_BUCKET, Resolution.QUARRY_BOSS,
                    "임시 냉각수는 계속 증발하고 있다. 아래쪽에서 주기적으로 치솟는 열이 설비 용량을 넘는다.",
                    "냉각수가 더는 순간적으로 끓지 않는다. 회수한 Relay 핵과 함께 심부 봉쇄가 약해졌다.",
                    35, 66, 417),
            def("Q_RAIL", Region.QUARRY, "광차 선로 분기", 35, 66, 417, Items.RAIL, Resolution.QUARRY_BOSS,
                    "세 갈래 선로가 모두 같은 심부 갱도로 모인다. 무거운 화물이 반복해서 한 방향으로 운반된 흔적이다.",
                    "운반선의 끝이 드러났다. 남은 선로는 콜바크가 올라온 심부와 직접 이어진다.",
                    -22, 65, 459),
            def("Q_LOCKER", Region.QUARRY, "작업자 보관소", -22, 65, 459, Items.IRON_PICKAXE, Resolution.QUARRY_BOSS,
                    "보관함 안쪽이 열에 그을렸지만 잠금은 안에서 풀려 있다. 작업자들은 무언가 올라오기 전에 철수했다.",
                    "새 열흔은 생기지 않는다. 남은 장비의 금속음 너머로 심부 진동만 낮게 이어진다.",
                    54, 63, 438),
            def("Q_WARNING", Region.QUARRY, "콜바크 경고 가설문", 54, 63, 438, Items.BLAST_FURNACE, Resolution.CHAPTER_4_CLEAR,
                    "경고 장치의 그을음이 중앙에만 집중돼 있다. 열원은 넓게 퍼진 용암이 아니라 거대한 몸의 핵에서 반복해서 솟았다.",
                    "중앙 열흔이 식었다. 가설문을 울리던 무거운 진동도 더는 돌아오지 않는다.",
                    124, 66, -80),

            // Old Relay: weak signal fragments become progressively legible until reconnection.
            def("R_ARCH", Region.RELAY, "외곽 신호문", 282, 68, -196, Items.AMETHYST_SHARD, Resolution.RELAY_ENTRANCE,
                    "깨진 결정 사이에서 아주 짧은 응답이 반복된다. 완전히 죽은 설비가 아니라 인증을 기다리는 신호에 가깝다.",
                    "Relay 조각과 신호문이 같은 주기로 빛난다. 내부 접근로가 오래된 인증을 다시 받아들였다.",
                    336, 68, -258),
            def("R_TRIAGE", Region.RELAY, "임시 구호 구역", 336, 68, -258, Items.POTION, Resolution.RELAY_BOSS,
                    "기계 부품보다 응급 도구가 더 많이 남아 있다. 중계소가 무너지기 직전까지 이곳은 사람을 받아들였다.",
                    "복원한 기록의 시간대와 구호 표식이 맞아떨어진다. 마지막 인원은 관측실 봉쇄 직전까지 이곳에 있었다.",
                    386, 67, -320),
            def("R_FORK", Region.RELAY, "파손된 신호 분기", 386, 67, -320, Items.REDSTONE, Resolution.RELAY_BOSS,
                    "두 갈래 신호가 서로 다른 시간을 가리킨다. 기록이 빠진 구간마다 신호 순서가 뒤집혀 있다.",
                    "복원된 기록을 기준으로 신호 순서가 정렬됐다. 가장 늦은 응답은 관측 통로 안쪽에서 온다.",
                    411, 66, -347),
            def("R_MAINT", Region.RELAY, "중계소 정비실", 444, 66, -291, Items.COMPARATOR, Resolution.ENDGAME,
                    "도구는 남아 있지만 중앙 계통이 응답하지 않는다. 세라크의 균열과 별개로 Relay 자체도 마지막 재연결을 기다린다.",
                    "계측기가 안정된 간격으로 깜박인다. 오래 멈췄던 중계소가 다시 외부 신호를 주고받기 시작했다.",
                    458, 66, -350),
            def("R_OBSERVE", Region.RELAY, "세라크 관측 통로", 411, 66, -347, Items.ENDER_EYE, Resolution.RELAY_CLEAR,
                    "관측석 주변의 잔광이 한쪽으로 잡아당겨진다. 공간의 틈이 통로 끝에서 반복해서 열리고 닫힌다.",
                    "잔광이 더는 한 방향으로 휘지 않는다. 균열은 남았지만 누군가 붙잡아 늘이는 힘은 사라졌다.",
                    458, 66, -350)
    );

    private static final Map<String, Def> BY_ID = index();
    /** player -> (incident id -> spawned interaction actor) */
    private static final Map<UUID, Map<String, UUID>> ACTORS = new ConcurrentHashMap<>();
    /** Presentation-only one-shot reveal pulse; not persisted and never used as campaign progress. */
    private static final Map<UUID, Set<String>> DISCOVERED = new ConcurrentHashMap<>();

    private AsterMarchFieldIncidents() {}

    public static void sync(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        UUID playerId = player.getUUID();
        Map<String, UUID> actors = ACTORS.computeIfAbsent(playerId, ignored -> new LinkedHashMap<>());
        Set<String> discovered = DISCOVERED.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet());
        var snapshot = CampaignProgressStore.snapshot(playerId);

        // Remove stale or distant interaction actors first so only the local stretch of route is populated.
        for (var entry : List.copyOf(actors.entrySet())) {
            Def def = BY_ID.get(entry.getKey());
            Entity entity = level.getEntity(entry.getValue());
            if (def == null || entity == null || player.position().distanceToSqr(def.pos()) > DESPAWN_RADIUS_SQ) {
                if (entity != null) entity.discard();
                actors.remove(entry.getKey());
            }
        }

        for (Def def : DEFINITIONS) {
            double distanceSq = player.position().distanceToSqr(def.pos());
            if (distanceSq > SPAWN_RADIUS_SQ) continue;
            boolean resolved = resolved(snapshot, def.resolution());
            ArmorStand stand = actor(level, actors, def, resolved);
            if (stand == null) continue;
            stand.setCustomNameVisible(distanceSq <= LABEL_RADIUS_SQ);
            applyName(stand, def, resolved);
            if (distanceSq <= DISCOVERY_RADIUS_SQ && discovered.add(def.id())) reveal(level, def, resolved);
        }
    }

    public static boolean interact(ServerPlayer player, Entity target) {
        if (player == null || target == null || !(player.level() instanceof ServerLevel level)) return false;
        Map<String, UUID> actors = ACTORS.get(player.getUUID());
        if (actors == null || actors.isEmpty()) return false;
        String incidentId = null;
        for (var entry : actors.entrySet()) {
            if (entry.getValue().equals(target.getUUID())) {
                incidentId = entry.getKey();
                break;
            }
        }
        if (incidentId == null) return false;
        Def def = BY_ID.get(incidentId);
        if (def == null) return false;

        boolean resolved = resolved(CampaignProgressStore.snapshot(player.getUUID()), def.resolution());
        ChatFormatting color = color(def.region());
        player.sendSystemMessage(Component.literal("현장 조사 · " + def.title()).withStyle(color, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal(resolved ? def.after() : def.before()).withStyle(ChatFormatting.GRAY));
        inspectPulse(level, def, resolved);
        if (!resolved && def.next() != null) directionalTrail(level, player.position().add(0, 0.9, 0), def.next(), particle(def.region()));
        return true;
    }

    /** Battle owns the field while active; remove these floating inspection props until field control returns. */
    public static void cancelForBattle(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        despawn(level, ACTORS.remove(player.getUUID()));
    }

    public static void remove(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        despawn(level, ACTORS.remove(player.getUUID()));
        DISCOVERED.remove(player.getUUID());
    }

    private static ArmorStand actor(ServerLevel level, Map<String, UUID> actors, Def def, boolean resolved) {
        UUID existingId = actors.get(def.id());
        Entity existing = existingId == null ? null : level.getEntity(existingId);
        if (existing instanceof ArmorStand stand) return stand;
        if (existingId != null) actors.remove(def.id());

        ArmorStand stand = new ArmorStand(level, def.pos().x, def.pos().y, def.pos().z);
        stand.setInvisible(true);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setShowArms(true);
        stand.setYRot(0.0F);
        stand.setItemSlot(EquipmentSlot.MAINHAND, def.icon().getDefaultInstance());
        applyName(stand, def, resolved);
        level.addFreshEntity(stand);
        actors.put(def.id(), stand.getUUID());
        return stand;
    }

    private static void applyName(ArmorStand stand, Def def, boolean resolved) {
        ChatFormatting color = resolved ? ChatFormatting.DARK_GRAY : color(def.region());
        String prefix = resolved ? "기록 · " : "조사 · ";
        stand.setCustomName(Component.literal(prefix + def.title()).withStyle(color));
    }

    private static boolean resolved(CampaignProgressStore.Snapshot snapshot, Resolution state) {
        Set<String> clears = snapshot.clearedEncounters();
        Set<String> quests = snapshot.quests().completed();
        Set<String> flags = snapshot.quests().unlockFlags();
        return switch (state) {
            case MEADOW_DEEP -> clears.contains("ENC_M01") && clears.contains("ENC_M02");
            case MEADOW_BOSS -> clears.contains("ENC_M04");
            case CHAPTER_1_CLEAR -> clears.contains("BATTLE_B01") || quests.contains("MQ_C01_03_graul");
            case GLOAM_DEEP -> quests.contains("MQ_C02_01_spores") || flags.contains("GLOAM_DEEP_PATH");
            case GLOAM_BOSS -> quests.contains("MQ_C02_02_root_wall") || flags.contains("B02_GATE");
            case CHAPTER_2_CLEAR -> clears.contains("BATTLE_B02") || quests.contains("MQ_C02_03_verna");
            case AQUEDUCT_LOWER -> quests.contains("MQ_C03_01_dry_channel") || flags.contains("AQUEDUCT_LOWER");
            case AQUEDUCT_BOSS -> quests.contains("MQ_C03_02_old_orders") || flags.contains("ORO_ROOM");
            case CHAPTER_3_CLEAR -> clears.contains("BATTLE_B03") || quests.contains("MQ_C03_03_oro7");
            case QUARRY_DEEP -> quests.contains("MQ_C04_01_ash_route") || flags.contains("FT_QUARRY");
            case QUARRY_BOSS -> quests.contains("MQ_C04_02_core_fragment") || flags.contains("B04_GATE");
            case CHAPTER_4_CLEAR -> clears.contains("BATTLE_B04") || quests.contains("MQ_C04_03_kolvak");
            case RELAY_ENTRANCE -> quests.contains("MQ_C05_01_relay_key") || flags.contains("OLD_RELAY_ENTRANCE");
            case RELAY_BOSS -> quests.contains("MQ_C05_02_serak_record") || flags.contains("B05_GATE");
            case RELAY_CLEAR -> clears.contains("BATTLE_B05");
            case ENDGAME -> quests.contains("MQ_C05_03_reconnect") || flags.contains("ENDGAME");
        };
    }

    private static void reveal(ServerLevel level, Def def, boolean resolved) {
        ParticleOptions primary = particle(def.region());
        Vec3 p = def.pos().add(0, 0.75, 0);
        ring(level, primary, p, resolved ? 0.65 : 0.9, resolved ? 10 : 16);
        level.sendParticles(resolved ? ParticleTypes.END_ROD : primary,
                p.x, p.y + 0.35, p.z, resolved ? 3 : 7, 0.28, 0.3, 0.28, 0.015);
    }

    private static void inspectPulse(ServerLevel level, Def def, boolean resolved) {
        ParticleOptions primary = particle(def.region());
        Vec3 p = def.pos().add(0, 0.65, 0);
        ring(level, primary, p, resolved ? 0.8 : 1.15, resolved ? 14 : 22);
        ring(level, ParticleTypes.END_ROD, p.add(0, 0.55, 0), resolved ? 0.45 : 0.72, resolved ? 8 : 14);
        level.sendParticles(primary, p.x, p.y + 0.5, p.z, resolved ? 6 : 12, 0.45, 0.5, 0.45, 0.025);
    }

    private static void directionalTrail(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions primary) {
        Vec3 flat = new Vec3(to.x - from.x, 0, to.z - from.z);
        if (flat.lengthSqr() < 0.001) return;
        Vec3 direction = flat.normalize();
        double length = Math.min(13.0, Math.sqrt(flat.lengthSqr()));
        for (double distance = 1.25; distance <= length; distance += 1.35) {
            Vec3 p = from.add(direction.scale(distance));
            level.sendParticles(primary, p.x, p.y + Math.sin(distance * 0.8) * 0.08, p.z,
                    2, 0.09, 0.08, 0.09, 0.005);
        }
    }

    private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            level.sendParticles(particle, x, center.y, z, 1, 0.01, 0.01, 0.01, 0.0);
        }
    }

    private static ParticleOptions particle(Region region) {
        return switch (region) {
            case SOUTHGATE -> ParticleTypes.CLOUD;
            case GLOAMWOOD -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case AQUEDUCT -> ParticleTypes.ELECTRIC_SPARK;
            case QUARRY -> ParticleTypes.ASH;
            case RELAY -> ParticleTypes.PORTAL;
        };
    }

    private static ChatFormatting color(Region region) {
        return switch (region) {
            case SOUTHGATE -> ChatFormatting.GOLD;
            case GLOAMWOOD -> ChatFormatting.DARK_GREEN;
            case AQUEDUCT -> ChatFormatting.AQUA;
            case QUARRY -> ChatFormatting.YELLOW;
            case RELAY -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    private static Def def(String id, Region region, String title, double x, double y, double z, Item icon,
                           Resolution resolution, String before, String after, double nx, double ny, double nz) {
        return new Def(id, region, title, new Vec3(x, y, z), icon, resolution, before, after, new Vec3(nx, ny, nz));
    }

    private static Map<String, Def> index() {
        Map<String, Def> out = new LinkedHashMap<>();
        for (Def def : DEFINITIONS) {
            if (out.put(def.id(), def) != null) throw new IllegalStateException("Duplicate Aster March field incident " + def.id());
        }
        return Map.copyOf(out);
    }

    private static void despawn(ServerLevel level, Map<String, UUID> actors) {
        if (actors == null) return;
        for (UUID id : actors.values()) {
            Entity entity = level.getEntity(id);
            if (entity != null) entity.discard();
        }
        actors.clear();
    }
}
