package io.github.q93503128.turnbound.client;

import java.util.ArrayList;
import java.util.List;

/** Stable edge HUD: wide fixed turn strip, roomy skill dock, and non-overlapping party controls. */
final class BattleHudLayout {
    static final int SKILL_COUNT=5,ALLY_COUNT=4,ENEMY_COUNT=5;
    private BattleHudLayout(){}

    record Rect(int x,int y,int width,int height){Rect{if(width<=0||height<=0)throw new IllegalArgumentException("HUD rectangles must be positive");}int right(){return x+width;}int bottom(){return y+height;}boolean contains(double px,double py){return px>=x&&px<right()&&py>=y&&py<bottom();}boolean overlaps(Rect other){return x<other.right()&&right()>other.x&&y<other.bottom()&&bottom()>other.y;}}
    record Layout(int screenWidth,int screenHeight,List<Rect> allyBars,List<Rect> enemyBars,List<Rect> skillButtons,Rect actionHeader,Rect confirmButton,Rect tooltipArea,Rect timeline,Rect autoButton,Rect speedButton,Rect fleeButton,Rect settingsPanel,boolean compact){Layout{allyBars=List.copyOf(allyBars);enemyBars=List.copyOf(enemyBars);skillButtons=List.copyOf(skillButtons);}}

    static Layout calculate(int requestedWidth,int requestedHeight){
        int width=Math.max(1,requestedWidth),height=Math.max(1,requestedHeight);boolean compact=width<560||height<300;int margin=compact?4:6,gap=compact?3:4;
        int controlH=compact?16:19,controlW=compact?34:42,controlsTotal=controlW*3+gap*2,controlsX=Math.max(margin,width-margin-controlsTotal),controlsY=Math.max(margin,height-margin-controlH);Rect auto=inside(width,height,controlsX,controlsY,controlW,controlH),speed=inside(width,height,auto.right()+gap,controlsY,controlW,controlH),flee=inside(width,height,speed.right()+gap,controlsY,controlW,controlH);
        int allyGap=compact?3:5,allyH=compact?16:19,allyAvailable=Math.max(4,controlsX-margin-10),allyW=Math.max(1,Math.min(compact?92:130,(allyAvailable-allyGap*3)/4)),allyY=height-margin-allyH;List<Rect> allies=new ArrayList<>(ALLY_COUNT);for(int i=0;i<ALLY_COUNT;i++)allies.add(inside(width,height,margin+i*(allyW+allyGap),allyY,allyW,allyH));
        List<Rect> enemies=new ArrayList<>(ENEMY_COUNT);for(int i=0;i<ENEMY_COUNT;i++)enemies.add(inside(width,height,width-margin-1,margin+i,1,1));
        int timelineW=Math.min(compact?210:360,Math.max(1,width-2*margin)),timelineH=compact?16:20;Rect timeline=inside(width,height,(width-timelineW)/2,margin,timelineW,timelineH);
        int dockW=compact?Math.min(154,Math.max(116,width/3)):182,dockX=width-margin-dockW,skillRows=3,skillH=compact?25:30,skillW=(dockW-gap)/2,skillAreaH=skillRows*skillH+(skillRows-1)*gap,headerH=compact?20:24,dockBottom=controlsY-7,dockY=Math.max(timeline.bottom()+8,dockBottom-headerH-skillAreaH);Rect header=inside(width,height,dockX,dockY,dockW,headerH);List<Rect> skills=new ArrayList<>(SKILL_COUNT);for(int i=0;i<SKILL_COUNT;i++){int row=i/2,col=i%2;skills.add(inside(width,height,dockX+col*(skillW+gap),header.bottom()+row*(skillH+gap),skillW,skillH));}
        int tooltipW=compact?160:218,tooltipH=compact?70:88,tooltipX=Math.max(margin,dockX-tooltipW-7),tooltipY=Math.min(dockY,Math.max(timeline.bottom()+6,controlsY-tooltipH-8));Rect tooltip=inside(width,height,tooltipX,tooltipY,Math.min(tooltipW,Math.max(1,dockX-margin-7)),tooltipH);Rect confirm=inside(width,height,dockX,Math.max(margin,controlsY-1),1,1);int settingsW=Math.min(250,Math.max(1,width-margin*2)),settingsH=Math.min(110,Math.max(1,height-margin*2));Rect settings=inside(width,height,(width-settingsW)/2,(height-settingsH)/2,settingsW,settingsH);return new Layout(width,height,allies,enemies,skills,header,confirm,tooltip,timeline,auto,speed,flee,settings,compact);
    }
    private static Rect inside(int width,int height,int x,int y,int w,int h){int sx=clamp(x,0,width-1),sy=clamp(y,0,height-1);return new Rect(sx,sy,Math.max(1,Math.min(w,width-sx)),Math.max(1,Math.min(h,height-sy)));}private static int clamp(int value,int min,int max){return Math.max(min,Math.min(max,value));}
}
