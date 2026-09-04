package io.github.q93503128.turnbound.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Presentation-only field rhythm layered around canonical campaign encounters.
 *
 * Encounter managers, quest progress and reward services remain authoritative. This class only reads the persisted
 * clear set and gives selected route beats a before/after visual cadence: nearby pressure before a fight, then a short
 * settling pulse and directional trail after the authoritative encounter clear appears. Existing saves seed their
 * current clear set silently so historical victories are never replayed as fresh events.
 */
public final class AsterMarchFieldSequences {
    private record Beat(
            String encounterId,
            Vec3 approach,
            Vec3 settle,
            Vec3 next,
            double approachRadius,
            double settleRadius,
            ParticleOptions pressureParticle,
            ParticleOptions clearParticle,
            ChatFormatting color,
            String approachLine,
            String clearLine) {}

    private static final List<Beat> BEATS = List.of(
            // Southgate Meadow: the open grassland starts readable, then pushes the eye toward Graul's impact lane.
            beat("ENC_M02", 13, 67, 173, 13, 67, 173, 58, 67, 200,
                    ParticleTypes.CLOUD, ParticleTypes.END_ROD, ChatFormatting.YELLOW,
                    "바람이 끊기는 지점마다 초원 순찰대의 발자국이 겹친다. 두 번째 경계선이 바로 앞이다.",
                    "초입 전선의 압박이 잦아들었다. 더 깊은 초원 쪽 발자국이 한 방향으로 모인다."),
            beat("ENC_M04", 218, 67, 228, 220, 67, 230, 326, 68, 261,
                    ParticleTypes.ASH, ParticleTypes.CLOUD, ChatFormatting.GOLD,
                    "마른 흙 위에서 불안정한 열기가 짧게 터졌다 사라진다. 앞의 전투 구역이 흔들리고 있다.",
                    "폭발 잔광이 꺼지자 거대한 충돌 흔적만 남았다. 흔적은 그라울의 접근로 쪽으로 이어진다."),
            beat("BATTLE_B01", 342, 68, 245, 355, 68, 245, 0, 0, 0,
                    ParticleTypes.CLOUD, ParticleTypes.END_ROD, ChatFormatting.GOLD,
                    "초원 전체의 먼지가 같은 박자로 들썩인다. 들이받는 왕의 영역에 들어섰다.",
                    "거대한 돌진 흔적이 멎었다. 남문 전선에 처음으로 고요가 돌아온다."),

            // Gloamwood: spore interference becomes a narrowing root corridor before Verna.
            beat("ENC_G02", -55, 70, -250, -55, 70, -250, -40, 70, -286,
                    ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.ENCHANT, ChatFormatting.DARK_GREEN,
                    "포자가 한쪽으로 빨려 들어가듯 흐른다. 숲길을 누르는 무언가가 가까이 있다.",
                    "엉킨 포자 흐름이 풀리며 깊은 숲 방향의 공기가 잠시 맑아진다."),
            beat("ENC_G04", -75, 71, -365, -75, 71, -365, -35, 72, -405,
                    ParticleTypes.ENCHANT, ParticleTypes.END_ROD, ChatFormatting.GREEN,
                    "뿌리가 땅 밑에서 서로 신호를 주고받듯 떨린다. 수호 개체의 밀도가 높아졌다.",
                    "뿌리의 진동이 끊기고 꽃잎 냄새가 더 짙어진다. 숲의 중심부가 가까워졌다."),
            beat("BATTLE_B02", -35, 72, -426, -35, 72, -440, 0, 0, 0,
                    ParticleTypes.ENCHANT, ParticleTypes.END_ROD, ChatFormatting.LIGHT_PURPLE,
                    "닫힌 꽃잎 사이로 빛이 새어 나온다. 베르나의 중심부가 바로 너머다.",
                    "숲을 조이던 맥동이 가라앉았다. 떠다니던 포자도 제각기 흩어지기 시작한다."),

            // Broken Aqueduct: pressure/electric rhythm points toward ORO-7's command room.
            beat("ENC_A02", -240, 66, -18, -240, 66, -18, -292, 66, 61,
                    ParticleTypes.DRIPPING_WATER, ParticleTypes.ELECTRIC_SPARK, ChatFormatting.AQUA,
                    "배관 안쪽에서 압력이 엇박자로 튄다. 전투 구역 뒤의 수로까지 같은 진동이 번진다.",
                    "불규칙한 수압이 한 번 크게 빠졌다. 하층 수로 쪽에서 남은 기계음이 더 선명해진다."),
            beat("ENC_A04", -380, 65, 15, -380, 65, 15, -413, 64, 53,
                    ParticleTypes.ELECTRIC_SPARK, ParticleTypes.CLOUD, ChatFormatting.BLUE,
                    "끊긴 명령선이 짧게 점멸한다. 관리실 쪽 방위 장치가 아직 전투 신호를 받고 있다.",
                    "전투 신호 하나가 완전히 꺼졌다. 남은 전류가 ORO 보안문 쪽으로만 모인다."),
            beat("BATTLE_B03", -418, 64, 35, -430, 64, 35, 0, 0, 0,
                    ParticleTypes.ELECTRIC_SPARK, ParticleTypes.END_ROD, ChatFormatting.AQUA,
                    "벽과 바닥의 금속음이 일정한 주기로 맞물린다. ORO-7의 관리 구역이다.",
                    "명령 주기가 끊기며 수로의 기계음이 자연스러운 잡음으로 돌아간다."),

            // Ember Quarry: ash and heat tighten into Kolvak's core-facing route.
            beat("ENC_Q02", -30, 69, 365, -30, 69, 365, 20, 70, 405,
                    ParticleTypes.ASH, ParticleTypes.FLAME, ChatFormatting.YELLOW,
                    "재가 지면을 스치지 못하고 위로 말려 오른다. 앞쪽 채석 설비에서 열압이 밀려온다.",
                    "재의 소용돌이가 풀렸다. 냉각 가설대와 심부 선로 쪽 시야가 잠시 깨끗해진다."),
            beat("ENC_Q05", 48, 64, 470, 48, 64, 470, 65, 63, 448,
                    ParticleTypes.ASH, ParticleTypes.FLAME, ChatFormatting.RED,
                    "바닥의 열이 한 지점에서 반복해서 솟는다. 콜바크 구역의 핵열과 같은 박자다.",
                    "심부의 열원이 하나 사라졌다. 남은 열기는 콜바크의 봉쇄선 쪽에 집중된다."),
            beat("BATTLE_B04", 65, 63, 442, 65, 63, 455, 0, 0, 0,
                    ParticleTypes.FLAME, ParticleTypes.END_ROD, ChatFormatting.GOLD,
                    "검은 암반 틈에서 붉은 열이 맥박친다. 거상의 핵이 바로 앞에서 움직인다.",
                    "거상의 열이 빠르게 식는다. 채석장 전체를 덮던 붉은 반사광도 옅어진다."),

            // Old Relay Station: broken signals converge into Serak, then release toward the final console.
            beat("ENC_R02", 320, 68, -245, 320, 68, -245, 365, 68, -305,
                    ParticleTypes.PORTAL, ParticleTypes.SOUL, ChatFormatting.LIGHT_PURPLE,
                    "끊긴 신호 조각들이 전투 구역을 피해 같은 방향으로 휘어진다. 기록 회랑이 가까워졌다.",
                    "찢어진 신호 한 갈래가 정렬됐다. 더 안쪽 중계 기록의 위치가 잠깐 또렷해진다."),
            beat("ENC_R04", 410, 66, -330, 410, 66, -330, 417, 66, -350,
                    ParticleTypes.SOUL, ParticleTypes.PORTAL, ChatFormatting.DARK_PURPLE,
                    "관측 통로의 빛이 앞으로 흐르지 못하고 되감긴다. 균열 중심이 바로 근처다.",
                    "되감기던 신호가 멎었다. 남은 균열 파형은 관측실 한 곳에만 겹쳐 있다."),
            beat("BATTLE_B05", 417, 66, -350, 430, 66, -350, 458, 66, -350,
                    ParticleTypes.PORTAL, ParticleTypes.ELECTRIC_SPARK, ChatFormatting.LIGHT_PURPLE,
                    "빛의 잔상이 같은 통로를 두 번 지나간다. 세라크의 균열이 현실을 접고 있다.",
                    "겹쳐 있던 신호가 한 줄로 펴졌다. 멈춘 Relay의 마지막 콘솔이 정상 좌표를 되찾는다."));

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    private AsterMarchFieldSequences() {}

    public static void tick(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        State state = STATES.computeIfAbsent(player.getUUID(), ignored -> new State());
        Set<String> clears = CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();

        if (!state.seeded) {
            state.knownClears.addAll(clears);
            state.seeded = true;
        } else {
            for (Beat beat : BEATS) {
                if (clears.contains(beat.encounterId()) && !state.knownClears.contains(beat.encounterId())) {
                    state.pendingClears.add(beat.encounterId());
                }
            }
            state.knownClears.addAll(clears);
        }

        for (Beat beat : BEATS) {
            if (state.pendingClears.contains(beat.encounterId()) && near(player, beat.settle(), beat.settleRadius())) {
                presentClear(level, player, beat);
                state.pendingClears.remove(beat.encounterId());
                continue;
            }

            if (clears.contains(beat.encounterId()) || !near(player, beat.approach(), beat.approachRadius())) continue;
            if (state.seenApproaches.add(beat.encounterId())) {
                player.sendSystemMessage(Component.literal(beat.approachLine()).withStyle(beat.color()));
            }
            if (player.tickCount % 20 == 0) pressure(level, player, beat);
        }
    }

    public static void remove(ServerPlayer player) {
        if (player != null) STATES.remove(player.getUUID());
    }

    private static Beat beat(String encounterId,
                             double ax, double ay, double az,
                             double sx, double sy, double sz,
                             double nx, double ny, double nz,
                             ParticleOptions pressure, ParticleOptions clear,
                             ChatFormatting color, String approachLine, String clearLine) {
        Vec3 next = nx == 0.0 && ny == 0.0 && nz == 0.0 ? null : new Vec3(nx, ny, nz);
        return new Beat(encounterId, new Vec3(ax, ay, az), new Vec3(sx, sy, sz), next,
                23.0, 72.0, pressure, clear, color, approachLine, clearLine);
    }

    private static boolean near(ServerPlayer player, Vec3 point, double radius) {
        return player.position().distanceToSqr(point) <= radius * radius;
    }

    private static void pressure(ServerLevel level, ServerPlayer player, Beat beat) {
        Vec3 p = beat.approach().add(0, 0.65, 0);
        level.sendParticles(beat.pressureParticle(), p.x, p.y, p.z, 7, 1.5, 0.6, 1.5, 0.012);
        Vec3 toPlayer = player.position().subtract(beat.approach());
        if (toPlayer.lengthSqr() > 0.01) {
            Vec3 edge = beat.approach().add(toPlayer.normalize().scale(2.3)).add(0, 0.25, 0);
            level.sendParticles(beat.pressureParticle(), edge.x, edge.y, edge.z, 3, 0.25, 0.18, 0.25, 0.006);
        }
    }

    private static void presentClear(ServerLevel level, ServerPlayer player, Beat beat) {
        player.sendSystemMessage(Component.literal(beat.clearLine()).withStyle(beat.color(), ChatFormatting.BOLD));
        Vec3 p = beat.settle().add(0, 0.8, 0);
        level.sendParticles(beat.clearParticle(), p.x, p.y, p.z, 18, 2.0, 0.9, 2.0, 0.018);
        if (beat.next() != null) trail(level, beat.settle().add(0, 0.7, 0), beat.next(), beat.clearParticle());
    }

    private static void trail(ServerLevel level, Vec3 origin, Vec3 target, ParticleOptions particle) {
        Vec3 flat = new Vec3(target.x - origin.x, 0, target.z - origin.z);
        if (flat.lengthSqr() < 0.01) return;
        Vec3 dir = flat.normalize();
        double length = Math.min(16.0, Math.sqrt(flat.lengthSqr()));
        for (double d = 1.6; d <= length; d += 1.6) {
            Vec3 p = origin.add(dir.scale(d));
            level.sendParticles(particle, p.x, p.y + 0.08 * Math.sin(d), p.z, 2, 0.16, 0.10, 0.16, 0.004);
        }
    }

    private static final class State {
        private final Set<String> knownClears = new HashSet<>();
        private final Set<String> seenApproaches = new HashSet<>();
        private final Set<String> pendingClears = new LinkedHashSet<>();
        private boolean seeded;
    }
}
