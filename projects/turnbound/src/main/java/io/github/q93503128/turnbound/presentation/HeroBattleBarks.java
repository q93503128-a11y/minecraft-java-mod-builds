package io.github.q93503128.turnbound.presentation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/** Exact v0.4 character-wiki battle lines. Presentation only; never changes combat results. */
public final class HeroBattleBarks {
    public enum Event { START, TURN, LOW_50, LOW_30, ALLY_DEATH, REVIVE, VICTORY, SPECIAL }

    private record Lines(String name,String start,String turn,String low50,String low30,String allyDeath,String revive,String victory,Map<String,String> special) {}

    private static final Map<String, Lines> LINES = Map.ofEntries(
            Map.entry("P01",new Lines("카이렌","하나씩 끝내지.","정했어.","","아직 손은 움직여.","...기억해 두지.","이번엔 놓치지 않는다.","다음 길을 보자.",Map.of("p01_breaker_strike","이제 도망칠 틈은 없어."))),
            Map.entry("P02",new Lines("루메아","서두르진 마. 빨리 끝내면 되니까.","여기네.","","계산이 조금 틀어졌네.","그 장면은 싫은데.","이번엔 시간을 제대로 쓸게.","봐, 오래 안 걸렸지?",Map.of("p02_time_leap","네 차례, 앞당겨 줄게."))),
            Map.entry("P03",new Lines("브람","내 뒤에 서.","막을 곳은 정해졌다.","","이 정도면 멀쩡해.","내가 늦었다.","문은 아직 안 무너졌다.","전원 확인해.",Map.of("p03_guard_transfer","이쪽으로 와."))),
            Map.entry("P04",new Lines("엘리시아","무리하지 마, 고칠 수 있는 만큼만 다쳐.","상태 보여 줘.","","내 건 나중에.","잠깐만, 아직 데려갈 수 있어.","이번엔 내가 도움받았네.","좋아. 다들 숨 쉬고 있어.",Map.of("p04_returned_breath","아직 끝난 거 아니야."))),
            Map.entry("P05",new Lines("리네트","먼저 쏘는 건 양보할 수도 있어. 두 번째는 내 거고.","보인다.","","조준은 안 흔들려.","...농담할 때가 아니네.","이번 탄은 안 빗나가.","마지막 한 발은 아껴뒀는데.",Map.of("REACTION","거기."))),
            Map.entry("P06",new Lines("모르웬","오늘은 몇 줄이나 늘어날까.","다음 이름.","","내 이름도 칸은 있어.","기록했어. 돌아올 수 있으면 돌아와.","마침표를 잘못 찍었네.","살아 있는 사람부터 세자.",Map.of("MEMORY","잊지 않을게."))),
            Map.entry("P07",new Lines("마리온","우리 둘이면 한 자리 더 있는 셈이야.","토토, 준비됐지?","","나보다 토토부터 봐 줘.","","나까지 불려올 줄은 몰랐네.","잘했어, 토토. ...나도? 고마워.",Map.of("p07_summon_toto","계약대로, 와 줘!","TOTO_DEATH","괜찮아. 다시 부를게."))),
            Map.entry("P08",new Lines("라제","멀쩡할 때 끝내면 재미없잖아.","아직 덜 아파.","이제 좀 몸 풀리네.","그래, 이 정도지.","야. 거기 누워 있지 마.","죽은 줄 알았냐? 나도.","치료는... 조금만 해.",Map.of()))
    );

    private HeroBattleBarks() { }
    public static boolean contains(String id){return LINES.containsKey(id);}

    public static void say(ServerPlayer player,String heroId,Event event){say(player,heroId,event,"");}
    public static void say(ServerPlayer player,String heroId,Event event,String key){
        Lines lines=LINES.get(heroId);if(player==null||lines==null)return;
        String text=switch(event){
            case START->lines.start;case TURN->lines.turn;case LOW_50->lines.low50;case LOW_30->lines.low30;
            case ALLY_DEATH->lines.allyDeath;case REVIVE->lines.revive;case VICTORY->lines.victory;
            case SPECIAL->lines.special.getOrDefault(key,"");};
        if(text==null||text.isBlank())return;
        player.sendSystemMessage(Component.literal(lines.name+" · ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE)),true);
    }
}
