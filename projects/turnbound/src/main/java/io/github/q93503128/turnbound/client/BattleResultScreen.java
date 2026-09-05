package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.network.BattleCommandPayload;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.world.CampaignProgressStore;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

/** Post-battle result page sized from actual content; all four active party members are always shown. */
public final class BattleResultScreen extends Screen {
    private static final int TEXT = 0xFFF4F0E6;
    private static final int SECONDARY = 0xFFAEB7C6;
    private static final int MUTED = 0xFF707987;
    private static final int GREEN = 0xFF62D39A;
    private static final int GOLD = 0xFFF6C85F;
    private static final int DANGER = 0xFFFF7A59;
    private static final int GAUGE = 0xFF6DC6FF;
    private static final int PANEL_DARK = 0xD90A0D13;
    private static final int CLEANUP_DARK = 0xF20A0D13;
    private static final int VICTORY_REVEAL_TICKS = 42;
    private static final int DEFEAT_REVEAL_TICKS = 12;

    private BattleHudButton continueButton;
    private int elapsedTicks;
    private boolean cleanupOpen;
    private boolean lastOverflow;
    private long seenMetaRevision = -1;

    public BattleResultScreen() { super(Component.literal("TURNBOUND 결과")); }

    @Override
    protected void init() {
        super.init();
        lastOverflow = overflowPredicted();
        seenMetaRevision = ClientMetaState.revision();
        Rect panel = panel();
        if (cleanupOpen) { buildCleanupWidgets(panel); return; }
        continueButton = addRenderableWidget(new BattleHudButton(panel.right() - 124, panel.bottom() - 32, 110, 22,
                Component.literal(lastOverflow ? "장비 정리" : "현장 복귀  R"), lastOverflow ? GOLD : GREEN,
                ignored -> continueField()));
        updateContinueVisibility();
    }

    @Override
    public void tick() {
        super.tick();
        var snapshot = ClientBattleState.snapshot();
        if (!snapshot.active()) { if (minecraft != null) minecraft.gui.setScreen(null); return; }
        if (!snapshot.finished() && minecraft != null) { minecraft.gui.setScreen(new BattleScreen()); return; }
        elapsedTicks++;
        boolean overflowNow = overflowPredicted();
        if (overflowNow != lastOverflow) { lastOverflow = overflowNow; if (!overflowNow) cleanupOpen = false; rebuild(); return; }
        if (cleanupOpen && ClientMetaState.revision() != seenMetaRevision) { seenMetaRevision = ClientMetaState.revision(); rebuild(); return; }
        updateContinueVisibility();
    }

    private void rebuild() { clearWidgets(); init(); }
    private void updateContinueVisibility() { if (continueButton != null) { boolean v=resultVisible(); continueButton.visible=v; continueButton.active=v; } }
    private boolean resultVisible() { boolean victory="ALLY_VICTORY".equals(ClientBattleState.snapshot().outcome()); return elapsedTicks >= (victory?VICTORY_REVEAL_TICKS:DEFEAT_REVEAL_TICKS); }
    private boolean overflowPredicted() { return ClientBattleState.hasResultNotice("INVENTORY_FULL"); }

    private void continueField() {
        if (!resultVisible()) return;
        if (overflowPredicted()) { cleanupOpen=true; ClientPacketDistributor.sendToServer(new MetaCommandPayload("SYNC")); rebuild(); return; }
        ClientPacketDistributor.sendToServer(new BattleCommandPayload("FLEE"));
    }

    private void buildCleanupWidgets(Rect panel) {
        int dialogW=Math.min(430,panel.width()-28), dialogH=Math.min(244,panel.height()-36);
        int dx=panel.x()+(panel.width()-dialogW)/2, dy=panel.y()+(panel.height()-dialogH)/2;
        var meta=ClientMetaState.snapshot();
        int buttonY=dy+86;
        if(!meta.pendingEquipment().isEmpty()){
            var pending=meta.pendingEquipment().getFirst();
            if(pending.claimable()) addRenderableWidget(new BattleHudButton(dx+16,buttonY,152,22,Component.literal("대기 보상 수령"),GREEN,ignored->meta("REWARD_CLAIM|"+pending.instanceId())));
            if(pending.immediateSellable()) addRenderableWidget(new BattleHudButton(dx+176,buttonY,Math.min(190,dialogW-192),22,Component.literal("대기 보상 판매 · "+pending.salePrice()+"G"),GOLD,ignored->meta("REWARD_SELL|"+pending.instanceId())));
            buttonY+=34;
        }
        List<ClientMetaState.EquipmentRow> sellable=meta.equipment().stream().filter(ClientMetaState.EquipmentRow::sellable)
                .sorted(Comparator.comparingInt((ClientMetaState.EquipmentRow row)->tierRank(row.tier())).thenComparingInt(ClientMetaState.EquipmentRow::enhancement).thenComparing(ClientMetaState.EquipmentRow::instanceId)).limit(4).toList();
        for(var row:sellable){
            if(buttonY+22>dy+dialogH-37) break;
            String text="판매 · "+row.tier()+" "+row.name()+" +"+row.enhancement()+" · "+row.salePrice()+"G";
            addRenderableWidget(new BattleHudButton(dx+16,buttonY,dialogW-32,22,Component.literal(text),GOLD,ignored->meta("SELL|"+row.instanceId())));
            buttonY+=27;
        }
        addRenderableWidget(new BattleHudButton(dx+dialogW-92,dy+dialogH-30,76,20,Component.literal("결과로"),MUTED,ignored->{cleanupOpen=false;rebuild();}));
    }

    private void meta(String command){ClientPacketDistributor.sendToServer(new MetaCommandPayload(command));}
    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){}

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){
        if(!resultVisible()){super.extractRenderState(graphics,mouseX,mouseY,partialTick);return;}
        var snapshot=ClientBattleState.snapshot(); Rect panel=panel(); boolean victory="ALLY_VICTORY".equals(snapshot.outcome()); int accent=victory?GREEN:DANGER;
        graphics.fill(panel.x(),panel.y(),panel.right(),panel.bottom(),PANEL_DARK); TurnboundFrameStyle.frame(graphics,panel.x(),panel.y(),panel.width(),panel.height(),accent); graphics.fill(panel.x(),panel.y(),panel.right(),panel.y()+3,accent);
        int x=panel.x()+16,y=panel.y()+13;
        graphics.text(font,Component.literal(victory?"승리":"패배"),x,y,accent,true);
        String encounter=encounterLabel(ClientBattleState.encounterId()); if(!encounter.isBlank()) graphics.text(font,Component.literal(UiTextLayout.fit(encounter,panel.width()-120)),x,y+15,SECONDARY,false);
        if(!victory){ graphics.text(font,Component.literal("보상 없음"),x,y+42,DANGER,true); graphics.text(font,Component.literal("패배 시 경험치와 전투 보상은 지급되지 않습니다."),x,y+59,TEXT,false); graphics.text(font,Component.literal("파티를 정비한 뒤 다시 도전할 수 있습니다."),x,y+76,SECONDARY,false); super.extractRenderState(graphics,mouseX,mouseY,partialTick); return; }
        var result=snapshot.result(); if(result.firstClear()) graphics.text(font,Component.literal("첫 클리어"),panel.right()-74,y,GOLD,true);
        int rewardTop=y+(encounter.isBlank()?28:38); graphics.text(font,Component.literal("보상"),x,rewardTop,SECONDARY,true);
        int cardY=rewardTop+14,cardGap=5,cardW=Math.max(72,(panel.width()-32-cardGap*3)/4);
        rewardCard(graphics,x,cardY,cardW,"골드",result.gold(),GOLD); rewardCard(graphics,x+(cardW+cardGap),cardY,cardW,"경험치",result.xp(),GAUGE); rewardCard(graphics,x+(cardW+cardGap)*2,cardY,cardW,"크리스탈",result.crystal(),result.crystal()>0?GAUGE:MUTED); rewardCard(graphics,x+(cardW+cardGap)*3,cardY,cardW,"정수",result.starEssence(),result.starEssence()>0?GOLD:MUTED);
        int extraY=cardY+34;
        for(String item:result.equipmentRewards()){graphics.text(font,Component.literal(UiTextLayout.fit("획득 · "+item,panel.width()-32)),x,extraY,GREEN,true);extraY+=14;}
        for(var notice:ClientBattleState.resultNotices()){graphics.text(font,Component.literal(UiTextLayout.fit(notice.text(),panel.width()-32)),x,extraY,notice.is("INVENTORY_FULL")?DANGER:GAUGE,true);extraY+=14;}
        if(!result.firstClear()&&result.equipmentRewards().isEmpty()&&ClientBattleState.resultNotices().isEmpty()){graphics.text(font,Component.literal("반복 클리어 · 반복 보상만 지급"),x,extraY,SECONDARY,false);extraY+=14;}
        int sectionY=Math.max(extraY+3,cardY+39); graphics.fill(x,sectionY,panel.right()-16,sectionY+1,0x55707987); graphics.text(font,Component.literal("파티 성장 · 참가자 전원"),x,sectionY+8,SECONDARY,true);
        int rowY=sectionY+24,rowW=panel.width()-32;
        for(int i=0;i<result.party().size()&&i<4;i++){drawPartyXp(graphics,x,rowY,rowW,result.party().get(i));rowY+=28;}
        if(cleanupOpen) drawCleanupDialog(graphics,panel); super.extractRenderState(graphics,mouseX,mouseY,partialTick);
    }

    private void drawCleanupDialog(GuiGraphicsExtractor graphics,Rect panel){
        int dialogW=Math.min(430,panel.width()-28),dialogH=Math.min(244,panel.height()-36),dx=panel.x()+(panel.width()-dialogW)/2,dy=panel.y()+(panel.height()-dialogH)/2; var meta=ClientMetaState.snapshot();
        graphics.fill(dx,dy,dx+dialogW,dy+dialogH,CLEANUP_DARK);TurnboundFrameStyle.frame(graphics,dx,dy,dialogW,dialogH,GOLD);graphics.fill(dx,dy,dx+4,dy+dialogH,GOLD);
        graphics.text(font,Component.literal("장비 인벤토리 정리"),dx+16,dy+14,GOLD,true);
        graphics.text(font,Component.literal(UiTextLayout.fit("획득 장비를 넣을 공간이 없습니다. 복귀 전에 공간을 확보하세요.",dialogW-32)),dx+16,dy+31,TEXT,false);
        graphics.text(font,Component.literal("인벤토리  "+meta.equipment().size()+" / "+EquipmentInventory.MAX_INSTANCES+" · 대기 "+meta.pendingEquipment().size()),dx+16,dy+49,SECONDARY,false);
        int yy=dy+67;
        if(!meta.pendingEquipment().isEmpty()){var pending=meta.pendingEquipment().getFirst();graphics.text(font,Component.literal(UiTextLayout.fit("대기 중 · "+pending.tier()+" "+pending.name(),dialogW-32)),dx+16,yy,GAUGE,true);yy+=51;} else {graphics.text(font,Component.literal(UiTextLayout.fit("판매 가능한 기존 장비를 정리하면 새 보상을 수령할 수 있습니다.",dialogW-32)),dx+16,yy,SECONDARY,false);yy+=34;}
        if(meta.equipment().isEmpty()&&meta.pendingEquipment().isEmpty()) graphics.text(font,Component.literal("장비 목록을 불러오는 중…"),dx+16,yy,MUTED,false); else {long sellable=meta.equipment().stream().filter(ClientMetaState.EquipmentRow::sellable).count();graphics.text(font,Component.literal("판매 가능 장비 "+sellable+"개"),dx+16,Math.min(dy+dialogH-48,yy),SECONDARY,false);}
    }

    private void rewardCard(GuiGraphicsExtractor graphics,int x,int y,int width,String label,int value,int color){graphics.fill(x,y,x+width,y+27,0xA4141922);graphics.fill(x,y,x+2,y+27,color);graphics.text(font,Component.literal(label),x+7,y+5,SECONDARY,false);graphics.text(font,Component.literal(value<=0?"-":"+"+value),x+7,y+16,color,true);}

    private void drawPartyXp(GuiGraphicsExtractor graphics,int x,int y,int width,ClientBattleState.PartyXp member){
        boolean levelUp=member.levelAfter()>member.levelBefore(),cap=member.xpToNextAfter()<=0;String level=levelUp?"Lv."+member.levelBefore()+" → "+member.levelAfter():"Lv."+member.levelAfter();
        graphics.text(font,Component.literal(UiTextLayout.fit(member.name(),88)),x,y,TEXT,true);graphics.text(font,Component.literal(level),x+96,y,levelUp?GOLD:SECONDARY,true);if(levelUp)graphics.text(font,Component.literal("레벨 상승"),x+width-56,y,GOLD,true);else if(cap)graphics.text(font,Component.literal("성급 한계"),x+width-52,y,GAUGE,true);
        int barX=x,barY=y+13,barW=Math.max(90,width-105);graphics.fill(barX,barY,barX+barW,barY+4,0xE0080A0E);int fill=cap?barW:(int)Math.round(barW*Math.min(1.0,member.xpAfter()/(double)Math.max(1,member.xpToNextAfter())));if(fill>0)graphics.fill(barX,barY,barX+fill,barY+4,GAUGE);String xp=cap?"한계":member.xpAfter()+" / "+member.xpToNextAfter();graphics.text(font,Component.literal(xp),barX+barW+8,y+9,cap?GAUGE:SECONDARY,false);
    }

    private String encounterLabel(String rawId){if(rawId==null||rawId.isBlank())return"";try{if(rawId.matches("HARD_B0[1-5]")){String bossId=rawId.substring("HARD_".length());return CanonicalData.definition(bossId).name()+" · 하드";}if(rawId.matches("RIFT_F\\d{2}")){int floor=Integer.parseInt(rawId.substring("RIFT_F".length()));return"균열 관문 · F"+floor+" · Lv."+V04Catalogs.riftFloor(floor).level();}String canonical=CampaignProgressStore.canonicalEncounterId(rawId);if(V04Catalogs.hasEncounter(canonical))return V04Catalogs.encounter(canonical).label();}catch(RuntimeException ignored){}return rawId;}

    private Rect panel(){ClientBattleState.Result result=ClientBattleState.snapshot().result();int extras=result.equipmentRewards().size()+ClientBattleState.resultNotices().size(),partyRows=Math.min(4,result.party().size());int desiredW=520;int desiredH=184+extras*14+partyRows*28;if(!"ALLY_VICTORY".equals(ClientBattleState.snapshot().outcome()))desiredH=166;int w=Math.max(320,Math.min(desiredW,width-20));int h=Math.max(166,Math.min(desiredH,height-20));return new Rect((width-w)/2,Math.max(10,height/2-h/2),w,h);}
    private static int tierRank(String tier){return switch(tier){case"T1"->1;case"T2"->2;case"T3"->3;case"T4"->4;default->5;};}
    @Override public boolean keyPressed(KeyEvent event){if(cleanupOpen&&event.key()==GLFW.GLFW_KEY_ESCAPE){cleanupOpen=false;rebuild();return true;}if(resultVisible()&&(event.key()==GLFW.GLFW_KEY_R||event.key()==GLFW.GLFW_KEY_ENTER||event.key()==GLFW.GLFW_KEY_KP_ENTER))continueField();return true;}
    @Override public boolean shouldCloseOnEsc(){return false;} @Override public boolean isPauseScreen(){return false;} @Override public void onClose(){}
    private record Rect(int x,int y,int width,int height){int right(){return x+width;}int bottom(){return y+height;}}
}
