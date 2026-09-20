package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.List;

/** Pure battle-status text/color projection; intentionally independent from NeoForge GUI classes for testability. */
final class BattleStatusPresentation {
    private static final int GOLD=TurnboundUiTokens.ACCENT,GREEN=TurnboundUiTokens.SUCCESS,RED=TurnboundUiTokens.DANGER,BLUE=TurnboundUiTokens.PRIMARY;

    private BattleStatusPresentation(){}

    static List<Badge> badges(ClientBattleState.Unit unit){
        List<Badge> out=new ArrayList<>();
        for(String token:unit.statuses()){
            if(!token.startsWith("@r:"))continue;
            String[] p=token.split(":");if(p.length<4)continue;
            String name=switch(p[1]){
                case"focus"->"집중";case"guard"->"Guard";case"shot"->"Shot";
                case"records"->"Records";case"bond"->"Bond";case"fury"->"Fury";default->p[1];
            };
            out.add(new Badge(name+" "+p[2]+"/"+p[3],resourceColor(p[1],parseInt(p[2],0),parseInt(p[3],1))));
        }
        for(String token:unit.statuses()){
            if(!token.startsWith("@m:"))continue;
            String marker=token.substring(3);
            if("duel".equals(marker))out.add(new Badge("결투 대상",GOLD));
            else if("sightline".equals(marker))out.add(new Badge("Sightline",BLUE));
        }
        for(String token:unit.statuses()){
            if(!token.startsWith("@s:"))continue;
            String[] p=token.split(":");if(p.length<5)continue;
            String id=p[1];int stacks=parseInt(p[2],1),turns=parseInt(p[3],1);double magnitude=parseDouble(p[4]);
            String label=statusLabel(id,magnitude);if(label.isBlank())continue;
            String suffix=stacks>1?" ×"+stacks:(turns>0&&turns<99?" "+turns+"T":"");
            out.add(new Badge(label+suffix,statusColor(id,magnitude)));
        }
        if("P08".equals(unit.defId())&&unit.hp()*100<=unit.maxHp()*50){
            out.add(new Badge(unit.hp()*100<=unit.maxHp()*30?"혈투 · 극한":"혈투",RED));
        }
        return List.copyOf(out);
    }

    private static String statusLabel(String id,double magnitude){return switch(id){
        case"exposed"->"노출";case"hunting_target"->"사냥 표적";case"sanctuary"->"안식 표식";case"partner_guard"->"계약수 보호";case"damage_reduction"->"피해 감소";case"guard_redirect"->"보호";case"time_echo"->"시간 메아리";
        case"attack_multiplier"->magnitude>=0?"공격↑":"공격↓";case"defense_multiplier"->magnitude>=0?"방어↑":"방어↓";case"speed_multiplier"->magnitude>=0?"속도↑":"속도↓";
        case"damage_taken_multiplier"->magnitude>0?"받는 피해↑":"받는 피해↓";case"healing_received_multiplier"->magnitude>=0?"회복↑":"회복↓";case"dot_max_hp"->"지속 피해";
        case"taunt"->"도발";case"silence"->"침묵";case"action_disable"->"행동 불가";case"serak_mark"->"균열 표식";case"b01_charge_warning","b04_eruption_warning","b05_collapse_warning"->"위험 예고";default->"";};}

    private static int statusColor(String id,double magnitude){
        if(id.equals("exposed")||id.equals("hunting_target")||id.equals("dot_max_hp")||id.equals("silence")||id.equals("action_disable")||id.contains("warning")||id.equals("serak_mark"))return RED;
        if((id.equals("attack_multiplier")||id.equals("defense_multiplier")||id.equals("speed_multiplier")||id.equals("healing_received_multiplier"))&&magnitude<0)return RED;
        return GREEN;
    }

    private static int resourceColor(String id,int value,int max){
        double ratio=Math.max(0.0,Math.min(1.0,value/(double)Math.max(1,max)));
        if("fury".equals(id)&&ratio>=0.80)return RED;
        if(("guard".equals(id)||"bond".equals(id))&&ratio>=0.50)return GREEN;
        if(("focus".equals(id)||"shot".equals(id))&&ratio>=0.99)return GREEN;
        return GOLD;
    }

    private static int parseInt(String v,int fallback){try{return Integer.parseInt(v);}catch(RuntimeException ignored){return fallback;}}
    private static double parseDouble(String v){try{return Double.parseDouble(v);}catch(RuntimeException ignored){return 0;}}

    record Badge(String text,int color){}
}
