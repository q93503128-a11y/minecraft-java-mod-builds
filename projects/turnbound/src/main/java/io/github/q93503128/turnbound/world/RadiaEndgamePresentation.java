package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.CanonicalData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** World-first atmosphere and party reactions for the post-story Radia endgame atrium. */
public final class RadiaEndgamePresentation {
    private enum Beat { ATRIUM_ENTRY, HARD_GALLERY, RIFT_GRID, DEEP_RIFT }
    private static final Vec3 ATRIUM = new Vec3(-100,66,-88);
    private static final Vec3 HARD = new Vec3(-100,66,-105);
    private static final Vec3 RIFT = new Vec3(-100,66,-84);
    private static final Vec3 DEEP = new Vec3(-84,66,-72);
    private static final Map<UUID, EnumSet<Beat>> SEEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> CHALLENGE_COUNT = new ConcurrentHashMap<>();

    private RadiaEndgamePresentation() {}

    public static void tick(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null || !RadiaHubSessionManager.active(player)
                || !CampaignContentUnlocks.endgame(player.getUUID())) return;
        if (player.position().distanceToSqr(ATRIUM) > 78.0 * 78.0) return;

        UUID id = player.getUUID();
        EnumSet<Beat> seen = SEEN.computeIfAbsent(id, ignored -> EnumSet.noneOf(Beat.class));
        trigger(player, seen, Beat.ATRIUM_ENTRY, ATRIUM, 34, "Relay가 다시 열린 뒤 처음 보는 신호들이야. 원하는 전투부터 확인하자.");
        trigger(player, seen, Beat.HARD_GALLERY, HARD, 18, "같은 보스라도 이쪽 반응은 더 무거워. 일반 재도전과는 따로 준비하자.");
        trigger(player, seen, Beat.RIFT_GRID, RIFT, 24, "균열은 층마다 편성이 바뀌어. 다음 층 정보부터 보고 들어가자.");
        trigger(player, seen, Beat.DEEP_RIFT, DEEP, 13, "여기부터가 깊은 구간이야. 파티 완성도를 확인하고 계속 가자.");

        int done = ChallengeService.completed(id).size();
        Integer before = CHALLENGE_COUNT.put(id, done);
        if (before != null) {
            int mark = crossed(before, done);
            if (mark > 0) {
                player.sendSystemMessage(Component.literal("Challenge Board · " + mark + "/20 달성")
                        .withStyle(mark >= 20 ? ChatFormatting.GOLD : ChatFormatting.GREEN, ChatFormatting.BOLD));
                Vec3 p = new Vec3(-121,67,-86);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, mark >= 20 ? 30 : 14,
                        1.0, 1.1, 1.0, 0.02);
            }
        }

        if (player.tickCount % 20 == 0) ambience(level, player);
    }

    public static void remove(ServerPlayer player) {
        if (player == null) return;
        SEEN.remove(player.getUUID());
        CHALLENGE_COUNT.remove(player.getUUID());
    }

    private static void trigger(ServerPlayer player, EnumSet<Beat> seen, Beat beat, Vec3 center, double radius, String fallback) {
        if (seen.contains(beat) || player.position().distanceToSqr(center) > radius * radius) return;
        seen.add(beat);
        String speaker = speaker(player);
        String name = speaker == null ? "파티" : CanonicalData.definition(speaker).name();
        player.sendSystemMessage(Component.literal(name + " · " + line(speaker, beat, fallback)).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static void ambience(ServerLevel level, ServerPlayer player) {
        // Hard gallery: restrained red/ash language rather than constant fire spam.
        for (int i = 0; i < 5; i++) {
            Vec3 p = new Vec3(-116 + i * 8, 67.1, -105);
            if (player.position().distanceToSqr(p) <= 30.0 * 30.0)
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, 2, 0.5, 0.55, 0.5, 0.005);
        }
        // F10/F20/F30 milestone seals are visually stronger than ordinary Rift floors.
        for (Vec3 p : List.of(new Vec3(-84,67,-88), new Vec3(-84,67,-80), new Vec3(-84,67,-72))) {
            if (player.position().distanceToSqr(p) <= 34.0 * 34.0)
                level.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 3, 0.65, 0.65, 0.65, 0.01);
        }
        if (player.position().distanceToSqr(new Vec3(-100,66,-69)) <= 30.0 * 30.0) {
            level.sendParticles(ParticleTypes.END_ROD, -100, 67.2, -69, 2, 0.5, 0.8, 0.5, 0.006);
        }
    }

    private static int crossed(int before, int now) {
        for (int mark : new int[]{20,15,10,5}) if (before < mark && now >= mark) return mark;
        return 0;
    }

    private static String speaker(ServerPlayer player) {
        for (String id : CampaignProgressStore.activeParty(player.getUUID())) if (id.startsWith("P")) return id;
        return null;
    }

    private static String line(String id, Beat beat, String fallback) {
        if (id == null) return fallback;
        return switch (id) {
            case "P01" -> switch (beat) {
                case HARD_GALLERY -> "한 번 이긴 상대라도 조건이 바뀌면 다른 싸움이야. 다시 처음부터 본다.";
                case DEEP_RIFT -> "여기까지 왔으면 끝까지 가 보자. 한 층씩 정리하면 돼.";
                default -> fallback;
            };
            case "P02" -> switch (beat) {
                case RIFT_GRID -> "층마다 속도와 편성이 달라져. 다음 행동 순서를 먼저 계산하자.";
                case DEEP_RIFT -> "깊은 층은 한 번의 순서 손실도 커. 게이지를 낭비하지 말자.";
                default -> fallback;
            };
            case "P03" -> switch (beat) {
                case HARD_GALLERY -> "강해진 상대일수록 첫 타를 버티는 게 중요해. 진형부터 고정하자.";
                case RIFT_GRID -> "연속 전투라고 서두를 필요 없어. 매 층 편성을 다시 확인한다.";
                default -> fallback;
            };
            case "P04" -> switch (beat) {
                case ATRIUM_ENTRY -> "끝난 뒤에도 싸울 이유는 남아 있네. 다치지 않게 하나씩 해 보자.";
                case DEEP_RIFT -> "깊어질수록 회복 타이밍이 중요해져. 무리하면 바로 돌아오자.";
                default -> fallback;
            };
            case "P05" -> switch (beat) {
                case HARD_GALLERY -> "같은 표적이면 오히려 좋아. 달라진 부분만 찾으면 돼.";
                case RIFT_GRID -> "후열 위치가 층마다 달라질 수 있어. 시작 전에 편성부터 볼게.";
                default -> fallback;
            };
            case "P06" -> switch (beat) {
                case ATRIUM_ENTRY -> "끝난 전투도 다시 기록할 수 있어. 이번엔 다른 결말이 남겠네.";
                case DEEP_RIFT -> "깊은 층일수록 기록이 많아져. 쓰러지는 순서까지 기억해 둘게.";
                default -> fallback;
            };
            case "P07" -> switch (beat) {
                case RIFT_GRID -> "토토가 층마다 다른 쪽을 보고 있어. 안쪽 신호가 계속 바뀌는 모양이야.";
                case DEEP_RIFT -> "토토도 아직 갈 수 있대. 계약은 여기서 끝난 게 아니니까.";
                default -> fallback;
            };
            case "P08" -> switch (beat) {
                case HARD_GALLERY -> "더 세졌다고? 잘됐네. 같은 상대 두 번 잡는 건 지루할 뻔했어.";
                case DEEP_RIFT -> "이 정도 열기는 있어야 깊은 층이지. 계속 내려가자.";
                default -> fallback;
            };
            default -> fallback;
        };
    }
}
