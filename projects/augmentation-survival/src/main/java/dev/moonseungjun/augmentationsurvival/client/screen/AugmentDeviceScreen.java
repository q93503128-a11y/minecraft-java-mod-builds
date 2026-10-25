package dev.moonseungjun.augmentationsurvival.client.screen;

import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Compact three-card augmentation UI inspired by the project's chosen rarity-card reference. */
public final class AugmentDeviceScreen extends AbstractContainerScreen<AugmentDeviceMenu> {
    private static final int W=380,H=232,BG=0xFF101419,SURF=0xFF1B222A,HOVER=0xFF27323D,SEL=0xFF303D49;
    private static final int TEXT=0xFFF2F5F7,MUTED=0xFFAAB4BE,BORDER=0xFF46535F,GOOD=0xFF8ED9A0,BAD=0xFFE58C8C;
    private static final int CW=112,CH=111,GAP=9,X0=13;
    private boolean upgradeMode;

    public AugmentDeviceScreen(AugmentDeviceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float d){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,BG);
        g.outline(leftPos,topPos,imageWidth,imageHeight,0xFF667482);
        g.fill(leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+31,0xFF202832);
        g.fill(leftPos+3,topPos+31,leftPos+imageWidth-3,topPos+33,0xFF35404B);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
        int x=mx-leftPos,y=my-topPos;
        g.text(font,title.getString(),14,12,TEXT,false);
        if(menu.advanced()) tabs(g,x,y);
        if(upgradeMode&&menu.advanced()) upgrades(g,x,y); else choices(g,x,y);
    }

    private void tabs(GuiGraphicsExtractor g,int mx,int my){
        tab(g,14,37,104,"증강 선택",!upgradeMode,mx,my); tab(g,123,37,104,"코어 합성",upgradeMode,mx,my);
    }
    private void tab(GuiGraphicsExtractor g,int x,int y,int w,String s,boolean on,int mx,int my){
        boolean h=inside(mx,my,x,y,w,18); g.fill(x,y,x+w,y+18,on?SEL:(h?HOVER:SURF));
        g.outline(x,y,w,18,on?0xFFCAD4DD:BORDER); center(g,s,x+w/2,y+5,on?TEXT:MUTED);
    }

    private void choices(GuiGraphicsExtractor g,int mx,int my){
        int ty=menu.advanced()?60:39, cy=menu.advanced()?87:67;
        for(AugmentTier t:AugmentTier.values()){
            int x=16+t.ordinal()*58; boolean on=menu.tierOrdinal()==t.ordinal(),h=inside(mx,my,x,ty,54,19);
            g.fill(x,ty,x+54,ty+19,on?SEL:(h?HOVER:SURF)); g.outline(x,ty,54,19,on?color(t):BORDER);
            if(on)g.fill(x+2,ty+16,x+52,ty+18,color(t)); center(g,label(t),x+27,ty+6,on?TEXT:MUTED);
        }
        boolean cards=menu.status()==AugmentDeviceMenu.STATUS_READY||menu.status()==AugmentDeviceMenu.STATUS_ACQUIRED;
        for(int i=0;i<3;i++){ if(cards&&menu.candidate(i)!=null) card(g,i,cy,mx,my); else empty(g,i,cy); }
        if(!cards){ center(g,status(menu.status()),imageWidth/2,cy+48,statusColor(menu.status()));
            if(menu.status()==AugmentDeviceMenu.STATUS_IDLE) center(g,"보유 코어의 등급을 선택하세요.",imageWidth/2,cy+64,MUTED); }
        else if(menu.status()==AugmentDeviceMenu.STATUS_READY) center(g,menu.selectionCount()==2?"서로 다른 증강 2개를 선택하세요.":"원하는 증강을 선택하세요.",imageWidth/2,imageHeight-17,MUTED);
        else center(g,status(menu.status()),imageWidth/2,imageHeight-17,statusColor(menu.status()));
    }

    private void card(GuiGraphicsExtractor g,int slot,int y,int mx,int my){
        AugmentDefinition d=menu.candidate(slot); int x=X0+slot*(CW+GAP),c=color(d.tier());
        boolean on=(menu.selectedMask()&(1<<slot))!=0,h=inside(mx,my,x,y,CW,CH);
        g.fill(x,y,x+CW,y+CH,on?SEL:(h?HOVER:SURF)); g.outline(x,y,CW,CH,on?TEXT:c); g.fill(x+1,y+1,x+CW-1,y+5,c);
        g.item(new ItemStack(AugmentCoreItems.item(d.tier())),x+48,y+11);
        String raw=d.canonicalNameKo(), name=raw.replace(" (액티브 · 토글)","").replace(" (액티브)","");
        center(g,name,x+CW/2,y+34,TEXT); center(g,label(d.tier()),x+CW/2,y+50,c);
        center(g,raw.contains("(액티브")?"액티브":"패시브",x+CW/2,y+64,raw.contains("(액티브")?0xFFF0D58A:MUTED);
        int cur=menu.currentLevel(slot), next=Math.min(cur+1,d.maxLevel());
        center(g,cur==0?"획득 → Lv."+next:"Lv."+cur+" → "+next,x+CW/2,y+82,TEXT);
        center(g,"최대 Lv."+d.maxLevel(),x+CW/2,y+96,MUTED);
    }
    private void empty(GuiGraphicsExtractor g,int slot,int y){ int x=X0+slot*(CW+GAP); g.fill(x,y,x+CW,y+CH,SURF); g.outline(x,y,CW,CH,0xFF35404B); center(g,"?",x+CW/2,y+45,0xFF59636D); }

    private void upgrades(GuiGraphicsExtractor g,int mx,int my){
        center(g,"같은 등급 코어 3개를 상위 코어 1개로 정제합니다.",imageWidth/2,63,MUTED);
        for(int i=0;i<AugmentTier.values().length-1;i++){
            AugmentTier a=AugmentTier.values()[i],b=AugmentTier.values()[i+1]; int y=82+i*25;
            g.fill(22,y,imageWidth-22,y+21,SURF); g.outline(22,y,imageWidth-44,21,0xFF35404B); g.fill(23,y+1,27,y+20,color(a));
            g.item(new ItemStack(AugmentCoreItems.item(a)),33,y+3); g.text(font,label(a)+" ×3",55,y+7,color(a),false);
            g.text(font,"→",121,y+7,MUTED,false); g.item(new ItemStack(AugmentCoreItems.item(b)),139,y+3); g.text(font,label(b)+" ×1",161,y+7,color(b),false);
            action(g,267,y+2,38,"1회",mx,my); action(g,311,y+2,45,"전부",mx,my);
        }
        if(menu.status()==AugmentDeviceMenu.STATUS_UPGRADED) center(g,"코어 합성 완료 ×"+Math.max(1,menu.lastOutputCount()),imageWidth/2,imageHeight-17,GOOD);
        else if(menu.status()==AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES) center(g,"합성할 코어가 부족합니다.",imageWidth/2,imageHeight-17,BAD);
    }
    private void action(GuiGraphicsExtractor g,int x,int y,int w,String s,int mx,int my){boolean h=inside(mx,my,x,y,w,17);g.fill(x,y,x+w,y+17,h?HOVER:SURF);g.outline(x,y,w,17,h?0xFFB8C4CE:BORDER);center(g,s,x+w/2,y+5,h?TEXT:MUTED);}

    @Override public boolean mouseClicked(MouseButtonEvent e,boolean dc){
        if(e.button()!=0)return super.mouseClicked(e,dc); double x=e.x()-leftPos,y=e.y()-topPos;
        if(menu.advanced()){if(inside(x,y,14,37,104,18)){upgradeMode=false;return true;}if(inside(x,y,123,37,104,18)){upgradeMode=true;return true;}}
        if(upgradeMode&&menu.advanced()){for(int i=0;i<AugmentTier.values().length-1;i++){int yy=82+i*25;AugmentTier t=AugmentTier.values()[i];if(inside(x,y,267,yy+2,38,17))return send(AugmentDeviceMenu.upgradeOneButtonId(t));if(inside(x,y,311,yy+2,45,17))return send(AugmentDeviceMenu.upgradeAllButtonId(t));}return super.mouseClicked(e,dc);}
        int ty=menu.advanced()?60:39; for(AugmentTier t:AugmentTier.values()){int xx=16+t.ordinal()*58;if(inside(x,y,xx,ty,54,19))return send(AugmentDeviceMenu.beginButtonId(t));}
        if(menu.status()==AugmentDeviceMenu.STATUS_READY){int cy=menu.advanced()?87:67;for(int i=0;i<3;i++)if(menu.candidate(i)!=null&&inside(x,y,X0+i*(CW+GAP),cy,CW,CH))return send(AugmentDeviceMenu.candidateButtonId(i));}
        return super.mouseClicked(e,dc);
    }
    private boolean send(int id){if(minecraft==null||minecraft.player==null||minecraft.gameMode==null)return false;if(!menu.clickMenuButton(minecraft.player,id))return false;minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);return true;}
    private void center(GuiGraphicsExtractor g,String s,int x,int y,int c){g.text(font,s,x-font.width(s)/2,y,c,false);}
    private static boolean inside(double x,double y,int l,int t,int w,int h){return x>=l&&x<l+w&&y>=t&&y<t+h;}
    private static String label(AugmentTier t){return switch(t){case COMMON->"일반";case UNCOMMON->"고급";case RARE->"희귀";case EPIC->"영웅";case LEGENDARY->"전설";case MYTHIC->"신화";};}
    private static int color(AugmentTier t){return switch(t){case COMMON->0xFFC6CCD3;case UNCOMMON->0xFF72C986;case RARE->0xFF5BA5E3;case EPIC->0xFFB978E5;case LEGENDARY->0xFFE4B257;case MYTHIC->0xFFE16161;};}
    private static String status(int s){return switch(s){case AugmentDeviceMenu.STATUS_NO_CORE->"선택한 등급의 코어가 없습니다.";case AugmentDeviceMenu.STATUS_FULLY_MAXED->"이 등급의 모든 증강이 최대 레벨입니다.";case AugmentDeviceMenu.STATUS_ACQUIRED->"증강을 획득했습니다.";case AugmentDeviceMenu.STATUS_INVALID->"선택을 완료하지 못했습니다.";case AugmentDeviceMenu.STATUS_UPGRADED->"코어 합성이 완료되었습니다.";case AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES->"합성할 코어가 부족합니다.";default->"증강 코어를 선택하세요.";};}
    private static int statusColor(int s){return switch(s){case AugmentDeviceMenu.STATUS_ACQUIRED,AugmentDeviceMenu.STATUS_UPGRADED->GOOD;case AugmentDeviceMenu.STATUS_NO_CORE,AugmentDeviceMenu.STATUS_INVALID,AugmentDeviceMenu.STATUS_NOT_ENOUGH_CORES->BAD;default->MUTED;};}
}
