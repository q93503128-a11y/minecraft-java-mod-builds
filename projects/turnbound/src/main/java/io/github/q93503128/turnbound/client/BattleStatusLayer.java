package io.github.q93503128.turnbound.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** World-space battle resource/buff/debuff badges projected immediately above actors. */
public final class BattleStatusLayer implements GuiLayer {
    private static final int GOLD=0xFFFFC857,GREEN=0xFF80D49A,RED=0xFFFF7A6B;

    @Override public void render(@NotNull GuiGraphicsExtractor graphics, DeltaTracker tracker){
        ClientBattleState.Snapshot snapshot=ClientBattleState.snapshot();
        if(!snapshot.active()||snapshot.finished())return;
        Minecraft minecraft=Minecraft.getInstance();if(minecraft.player==null)return;
        for(ClientBattleState.Unit unit:snapshot.units()){
            if(unit.downed())continue;
            List<Badge> badges=badges(unit);if(badges.isEmpty())continue;
            BattleLiveProjection.ScreenPoint p=BattleLiveProjection.project(unit.x(),unit.y()+2.25,unit.z(),graphics.guiWidth(),graphics.guiHeight());
            if(p==null)continue;
            String text=badges.stream().limit(3).map(Badge::text).reduce((a,b)->a+" · "+b).orElse("");
            int w=Math.min(156,minecraft.font.width(text)+10),x=(int)Math.round(p.x()-w/2.0),y=(int)Math.round(p.y()-5);
            x=Math.max(3,Math.min(graphics.guiWidth()-w-3,x));y=Math.max(30,Math.min(graphics.guiHeight()-14,y));
            int color=badges.getFirst().color();graphics.fill(x,y,x+w,y+12,0xB712151A);graphics.fill(x,y,x+2,y+12,color);
            graphics.text(minecraft.font,Component.literal(UiTextLayout.fit(text,w-8)),x+5,y+2,color,true);
        }
    }

    static List<Badge> badges(ClientBattleState.Unit unit){
        List<Badge> out=new ArrayList<>();
        for(String token:unit.statuses()){
            if(token.startsWith("@r:")){String[] p=token.split(":");if(p.length>=4){String name=switch(p[1]){case"focus"->"집중";case"memory"->"기억";case"contract"->"계약 준비";case"hunt"->"사냥 지시";default->p[1];};out.add(new Badge(name+" "+p[2]+"/"+p[3],GOLD));}}
        }
        for(String token:unit.statuses()){
            if(!token.startsWith("@s:"))continue;String[] p=token.split(":");if(p.length<5)continue;String id=p[1];int stacks=parseInt(p[2],1),turns=parseInt(p[3],1);double magnitude=parseDouble(p[4]);
            String label=statusLabel(id,magnitude);if(label.isBlank())continue;String suffix=stacks>1?" ×"+stacks:(turns>0&&turns<99?" "+turns+"T":"");out.add(new Badge(label+suffix,statusColor(id,magnitude)));
        }
        if("P08".equals(unit.defId())&&unit.hp()*100<=unit.maxHp()*50)out.add(new Badge(unit.hp()*100<=unit.maxHp()*30?"혈투 · 극한":"혈투",RED));
        return List.copyOf(out);
    }

    private static String statusLabel(String id,double magnitude){return switch(id){
        case"exposed"->"노출";case"hunting_target"->"사냥 표적";case"damage_reduction"->"피해 감소";case"guard_redirect"->"보호";case"time_echo"->"시간 메아리";
        case"attack_multiplier"->magnitude>=0?"공격↑":"공격↓";case"defense_multiplier"->magnitude>=0?"방어↑":"방어↓";case"speed_multiplier"->magnitude>=0?"속도↑":"속도↓";
        case"damage_taken_multiplier"->magnitude>0?"받는 피해↑":"받는 피해↓";case"healing_received_multiplier"->magnitude>=0?"회복↑":"회복↓";case"dot_max_hp"->"지속 피해";
        case"taunt"->"도발";case"silence"->"침묵";case"action_disable"->"행동 불가";case"serak_mark"->"균열 표식";case"b01_charge_warning","b04_eruption_warning","b05_collapse_warning"->"위험 예고";default->"";};}
    private static int statusColor(String id,double magnitude){if(id.equals("exposed")||id.equals("hunting_target")||id.equals("dot_max_hp")||id.equals("silence")||id.equals("action_disable")||id.contains("warning")||id.equals("serak_mark"))return RED;if((id.equals("attack_multiplier")||id.equals("defense_multiplier")||id.equals("speed_multiplier")||id.equals("healing_received_multiplier"))&&magnitude<0)return RED;return GREEN;}
    private static int parseInt(String v,int fallback){try{return Integer.parseInt(v);}catch(RuntimeException ignored){return fallback;}}private static double parseDouble(String v){try{return Double.parseDouble(v);}catch(RuntimeException ignored){return 0;}}
    record Badge(String text,int color){}
}
