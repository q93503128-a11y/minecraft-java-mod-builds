package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.presentation.PersonalPresentationIsolation;
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

/**
 * One-shot party chatter at authored field landmarks.
 *
 * The barks never advance quests or alter encounter rules. They only let the currently equipped core party react to
 * places the player is already walking through, keeping travel from feeling like silent space between combat triggers.
 */
public final class AsterMarchPartyFieldBarks {
    private enum Region { SOUTHGATE, GLOAMWOOD, AQUEDUCT, QUARRY, RELAY }
    private enum Beat {
        M_PATROL_CAMP, M_WAGON, M_MEMORIAL,
        G_LANTERN_FORK, G_ABANDONED_CAMP, G_CAUSEWAY,
        A_SERVICE, A_PIPE, A_LOOKOUT,
        Q_REST, Q_COOLING, Q_RAIL,
        R_SIGNAL, R_TRIAGE, R_FORK
    }
    private record Def(Beat beat, Region region, Vec3 pos, double radius, List<String> preferred, String fallback) {}

    private static final List<Def> DEFINITIONS = List.of(
            new Def(Beat.M_PATROL_CAMP, Region.SOUTHGATE, new Vec3(58,67,200), 15,
                    List.of("P01","P03","P05"), "순찰대가 여기서 숨을 돌렸던 것 같아. 다음 편성을 보고 움직이자."),
            new Def(Beat.M_WAGON, Region.SOUTHGATE, new Vec3(145,67,220), 14,
                    List.of("P05","P01","P08"), "수레가 길을 막은 게 아니라 급히 비켜 세워졌어. 전선이 더 안쪽으로 밀렸던 흔적이야."),
            new Def(Beat.M_MEMORIAL, Region.SOUTHGATE, new Vec3(252,67,258), 14,
                    List.of("P04","P06","P03"), "여기서부터 분위기가 달라져. 그라울의 흔적이 가까워."),

            new Def(Beat.G_LANTERN_FORK, Region.GLOAMWOOD, new Vec3(-12,68,-161), 14,
                    List.of("P02","P07","P05"), "등불 흔적이 갈라져 있어. 포자등불부터 차례로 확인하자."),
            new Def(Beat.G_ABANDONED_CAMP, Region.GLOAMWOOD, new Vec3(-68,70,-226), 13,
                    List.of("P07","P04","P06"), "야영지는 버려졌지만 싸운 흔적은 적어. 더 깊은 곳을 조사하던 사람들이었겠지."),
            new Def(Beat.G_CAUSEWAY, Region.GLOAMWOOD, new Vec3(-8,70,-286), 15,
                    List.of("P03","P02","P07"), "옛길이 아직 이어진다. 뿌리수호병 구역까지 진형을 유지하자."),

            new Def(Beat.A_SERVICE, Region.AQUEDUCT, new Vec3(-162,66,4), 13,
                    List.of("P02","P03","P05"), "정비 구역이 남아 있어. 밸브를 복구하면 하층으로 내려갈 수 있겠어."),
            new Def(Beat.A_PIPE, Region.AQUEDUCT, new Vec3(-219,66,27), 14,
                    List.of("P03","P05","P01"), "다리 폭이 좁아. 적 편성이 보이면 먼저 후열부터 확인하자."),
            new Def(Beat.A_LOOKOUT, Region.AQUEDUCT, new Vec3(-292,66,61), 14,
                    List.of("P02","P06","P03"), "시설 전체가 같은 명령을 기다리는 것처럼 조용해. ORO 구역이 멀지 않아."),

            new Def(Beat.Q_REST, Region.QUARRY, new Vec3(-95,69,345), 14,
                    List.of("P08","P04","P03"), "작업자들이 쉬던 자리야. 표층 전선을 끊고 나면 이쪽 숨통도 트이겠지."),
            new Def(Beat.Q_COOLING, Region.QUARRY, new Vec3(2,69,389), 15,
                    List.of("P08","P02","P04"), "냉각 설비가 멈췄어. 열기 속 용암굴착수의 핵을 직접 회수해야 해."),
            new Def(Beat.Q_RAIL, Region.QUARRY, new Vec3(35,66,417), 13,
                    List.of("P05","P08","P01"), "선로가 심부로 모인다. 핵 파편을 모으면 콜바크 쪽 봉쇄도 풀릴 거야."),

            new Def(Beat.R_SIGNAL, Region.RELAY, new Vec3(282,68,-196), 15,
                    List.of("P02","P06","P07"), "중계소 바깥 신호가 아직 완전히 죽진 않았어. 기록실부터 복원하자."),
            new Def(Beat.R_TRIAGE, Region.RELAY, new Vec3(336,68,-258), 14,
                    List.of("P04","P06","P03"), "여긴 사람을 살리던 흔적이 남아 있어. 세라크 기록도 같은 시기의 것일지 몰라."),
            new Def(Beat.R_FORK, Region.RELAY, new Vec3(386,67,-320), 15,
                    List.of("P06","P02","P07"), "신호가 여러 갈래로 찢어져 있어. 네 번째 기록을 찾으면 관측실 길이 분명해질 거야."));

    private static final Map<UUID, EnumSet<Beat>> SEEN = new ConcurrentHashMap<>();
    private AsterMarchPartyFieldBarks() {}

    public static void tick(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        Region region = region(player);
        if (region == null) return;
        EnumSet<Beat> seen = SEEN.computeIfAbsent(player.getUUID(), ignored -> EnumSet.noneOf(Beat.class));
        for (Def def : DEFINITIONS) {
            if (def.region() != region || seen.contains(def.beat())) continue;
            if (player.position().distanceToSqr(def.pos()) > def.radius() * def.radius()) continue;
            seen.add(def.beat());
            speak(level, player, def);
            break;
        }
    }

    public static void remove(ServerPlayer player) {
        if (player != null) SEEN.remove(player.getUUID());
    }

    private static void speak(ServerLevel level, ServerPlayer player, Def def) {
        String speaker = chooseSpeaker(player, def.preferred());
        String name = speaker == null ? "파티" : CanonicalData.definition(speaker).name();
        String line = speaker == null ? def.fallback() : line(speaker, def.beat(), def.fallback());
        ChatFormatting color = speakerColor(speaker);
        player.sendSystemMessage(Component.literal(name + " · " + line).withStyle(color));
        Vec3 p = def.pos().add(0, 1.0, 0);
        PersonalPresentationIsolation.particles(level, player, ParticleTypes.END_ROD,
                p.x, p.y, p.z, 3, 0.45, 0.35, 0.45, 0.005);
    }

    private static String chooseSpeaker(ServerPlayer player, List<String> preferred) {
        List<String> party = CampaignProgressStore.activeParty(player.getUUID());
        for (String id : preferred) if (party.contains(id)) return id;
        for (String id : party) if (id.startsWith("P")) return id;
        return null;
    }

    private static String line(String id, Beat beat, String fallback) {
        return switch (id) {
            case "P01" -> switch (beat) {
                case M_PATROL_CAMP -> "순찰선부터 정리하자. 계전소까지 길을 만든다.";
                case M_WAGON -> "후퇴가 급했던 흔적이네. 같은 방향으로 더 안쪽을 보자.";
                case A_PIPE -> "폭이 좁아. 한 명씩 끌어내서 처리하면 돼.";
                case Q_RAIL -> "심부로 이어진다. 핵 파편을 모으고 그대로 밀고 가자.";
                default -> fallback;
            };
            case "P02" -> switch (beat) {
                case G_LANTERN_FORK -> "포자등불 세 곳. 동선을 겹치지 않게 순서대로 돌자.";
                case A_SERVICE -> "압력 복구 순서를 맞추면 하층 접근도 같이 열릴 거야.";
                case A_LOOKOUT -> "명령 주기가 너무 일정해. ORO-7이 아직 시설을 잡고 있어.";
                case Q_COOLING -> "열이 계속 누적돼. 오래 머물지 말고 핵 회수 동선부터 잡자.";
                case R_SIGNAL -> "신호 간격이 남아 있어. 기록을 복원하면 패턴이 이어질 거야.";
                case R_FORK -> "분기 신호가 서로 어긋나. 마지막 기록이 기준점이겠네.";
                default -> fallback;
            };
            case "P03" -> switch (beat) {
                case M_PATROL_CAMP -> "초원은 엄폐가 없다. 흩어지지 말고 편성을 보고 들어가.";
                case G_CAUSEWAY -> "길이 좁아진다. 앞줄을 고정하고 지나가자.";
                case A_PIPE -> "난간 쪽으로 밀리지 마. 진형부터 잡는다.";
                case Q_REST -> "여기서 한 번 정리하고 가자. 심부부터는 열기가 더 세다.";
                case R_TRIAGE -> "대피 공간이었군. 통로를 막지 않게 움직이자.";
                default -> fallback;
            };
            case "P04" -> switch (beat) {
                case M_MEMORIAL -> "기록이 남아 있는 곳은 그냥 지나치지 말자. 누군가 돌아오지 못한 자리야.";
                case G_ABANDONED_CAMP -> "도망친 흔적보단 조사 흔적에 가까워. 더 깊이 들어갔던 것 같아.";
                case Q_REST -> "사람들이 쉬던 곳이네. 전선을 끊으면 이 길도 다시 쓸 수 있겠지.";
                case Q_COOLING -> "열기가 너무 강해. 핵을 찾더라도 무리해서 오래 머물진 마.";
                case R_TRIAGE -> "응급 처치 도구가 남아 있어. 여긴 마지막까지 사람을 받았던 곳이야.";
                default -> fallback;
            };
            case "P05" -> switch (beat) {
                case M_PATROL_CAMP -> "시야는 좋아. 적 수와 후열부터 확인하고 들어가자.";
                case M_WAGON -> "바퀴 자국이 한쪽으로 급하게 꺾였어. 전선은 저 방향이었겠네.";
                case A_PIPE -> "직선 시야가 길어. 원거리 개체가 있으면 먼저 보일 거야.";
                case Q_RAIL -> "선로가 한곳으로 모여. 심부 접근로를 찾기엔 좋은 표식이야.";
                default -> fallback;
            };
            case "P06" -> switch (beat) {
                case M_MEMORIAL -> "이름이 남은 기록은 읽어 두자. 사라진 사람도 여기 있었으니까.";
                case G_ABANDONED_CAMP -> "정리된 채 비어 있어. 떠난 순서가 기록처럼 남았네.";
                case A_LOOKOUT -> "시설은 멈췄는데 명령 흔적은 남아 있어. 오래된 기록과 비슷해.";
                case R_SIGNAL -> "끊긴 신호도 기록이야. 남은 조각을 이어 보면 누가 마지막에 있었는지 보여.";
                case R_TRIAGE -> "기계 기록보다 사람 흔적이 더 많네. 여기도 이름을 남겨야 할 곳이었어.";
                case R_FORK -> "찢어진 신호를 한 장씩 맞추면 돼. 마지막 기록이 빈칸을 채울 거야.";
                default -> fallback;
            };
            case "P07" -> switch (beat) {
                case G_LANTERN_FORK -> "토토가 깊은 길 쪽을 보고 있어. 등불부터 확인해 보자.";
                case G_ABANDONED_CAMP -> "토토가 싸움 냄새보다 오래된 흔적에 반응해. 조사대가 지나간 자리 같아.";
                case G_CAUSEWAY -> "뿌리가 길을 따라 자랐어. 토토도 같은 방향을 잡고 있어.";
                case R_SIGNAL -> "토토가 신호문 앞에서 멈췄어. 아직 뭔가 이어져 있는 것 같아.";
                case R_FORK -> "여러 신호가 섞여 있어도 토토는 한쪽을 계속 보고 있네. 기록을 더 찾자.";
                default -> fallback;
            };
            case "P08" -> switch (beat) {
                case M_WAGON -> "급하게 빠진 흔적이네. 그래도 길은 남았잖아. 가면 되지.";
                case Q_REST -> "이 냄새 익숙하네. 쉬던 자리에서 오래 버티진 말자.";
                case Q_COOLING -> "뜨거운 건 문제 없어. 핵 위치만 찾으면 내가 앞에서 길 열게.";
                case Q_RAIL -> "선로 끝이 심부면 오히려 편하네. 직선으로 가자.";
                default -> fallback;
            };
            default -> fallback;
        };
    }

    private static ChatFormatting speakerColor(String id) {
        if (id == null) return ChatFormatting.GRAY;
        return switch (id) {
            case "P02" -> ChatFormatting.AQUA;
            case "P04" -> ChatFormatting.GOLD;
            case "P06" -> ChatFormatting.LIGHT_PURPLE;
            case "P07" -> ChatFormatting.BLUE;
            case "P08" -> ChatFormatting.RED;
            default -> ChatFormatting.WHITE;
        };
    }

    private static Region region(ServerPlayer player) {
        if (FieldSessionManager.active(player)) return Region.SOUTHGATE;
        if (GloamwoodSessionManager.active(player)) return Region.GLOAMWOOD;
        if (BrokenAqueductSessionManager.active(player)) return Region.AQUEDUCT;
        if (EmberQuarrySessionManager.active(player)) return Region.QUARRY;
        if (OldRelayStationSessionManager.active(player)) return Region.RELAY;
        return null;
    }
}
