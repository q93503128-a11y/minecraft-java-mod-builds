package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.progression.GachaCatalog;
import io.github.q93503128.turnbound.progression.GrowthRulesV1;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Responsive management screen. Dense collections are paged and PC layouts favor information density. */
public final class MetaMenuScreen extends Screen {
    public enum Tab { HOME, PARTY, CHARACTERS, EQUIPMENT, ARCHIVE, QUESTS, CODEX, SYSTEM }
    private enum DetailTab { OVERVIEW, SKILLS, EQUIPMENT, GROWTH }
    private enum OwnershipFilter { ALL, OWNED, UNOWNED }
    private enum RoleFilter { ALL, DPS, SUPPORT, TANK, SUMMON }
    private enum EquipSort { TIER, LEVEL, STAT }

    private static final int TEXT=0xFFF4F0E6, SECONDARY=0xFFAEB7C6, MUTED=0xFF707987;
    private static final int BLUE=0xFF6DC6FF, GREEN=0xFF62D39A, GOLD=0xFFFFC857, DANGER=0xFFFF6B6B;
    private static final int CONTENT_OFFSET=88, FOOTER_OFFSET=42, CONTROL_H=20;
    private static final int COMPACT_CONTENT_OFFSET=66, COMPACT_FOOTER_OFFSET=24;

    private Tab tab;
    private final List<String> draftParty=new ArrayList<>();
    private int page,left,top,panelWidth,panelHeight,currentTotal,currentPerPage=1;
    private int contentOffset=CONTENT_OFFSET,footerOffset=FOOTER_OFFSET;
    private boolean compactLayout;
    private OwnershipFilter ownershipFilter=OwnershipFilter.ALL;
    private RoleFilter roleFilter=RoleFilter.ALL;
    private int starFilter,minimumLevel;
    private String selectedCharacterId="";
    private DetailTab detailTab=DetailTab.OVERVIEW;
    private EquipSort equipSort=EquipSort.TIER;
    private String equipSlotFilter="ALL",selectedEquipmentId="",equipmentTargetCharacterId="";
    private String codexCategory="CHARACTERS",selectedEndgameId="";

    public MetaMenuScreen(Tab tab){
        super(Component.literal("TURNBOUND"));
        this.tab=tab==null?Tab.HOME:tab;
        draftParty.addAll(ClientMetaState.snapshot().activeParty());
    }

    public Tab tab(){return tab;}

    public void refreshSnapshot(){
        if(tab==Tab.PARTY){draftParty.clear();draftParty.addAll(ClientMetaState.snapshot().activeParty());}
        if(!selectedCharacterId.isBlank()&&character(selectedCharacterId)==null)selectedCharacterId="";
        if(!selectedEquipmentId.isBlank()&&equipment(selectedEquipmentId)==null)selectedEquipmentId="";
        if(!selectedEndgameId.isBlank()&&endgame(selectedEndgameId)==null)selectedEndgameId="";
        rebuild();
    }

    @Override
    protected void init(){
        super.init();
        compactLayout=height<330||width<520;
        int margin=compactLayout?6:12;
        panelWidth=Math.min(980,Math.max(1,width-margin*2));
        panelHeight=Math.min(620,Math.max(1,height-margin*2));
        left=(width-panelWidth)/2;
        top=(height-panelHeight)/2;
        contentOffset=compactLayout?COMPACT_CONTENT_OFFSET:CONTENT_OFFSET;
        footerOffset=compactLayout?COMPACT_FOOTER_OFFSET:FOOTER_OFFSET;
        currentTotal=0;
        currentPerPage=1;
        buildTabs();
        switch(tab){
            case HOME->buildHome();
            case PARTY->buildParty();
            case CHARACTERS->buildCharacters();
            case EQUIPMENT->buildEquipment();
            case ARCHIVE->buildArchive();
            case QUESTS->buildQuests();
            case CODEX->buildCodex();
            case SYSTEM->buildSystem();
        }
    }

    private int contentTop(){return top+contentOffset;}
    private int contentBottom(){return top+panelHeight-footerOffset;}

    private void buildTabs(){
        if(tab==Tab.HOME)return;
        addRenderableWidget(new BattleHudButton(left+14,top+49,92,CONTROL_H,Component.literal("← 빠른 메뉴"),MUTED,ignored->switchTab(Tab.HOME)));
    }

    private void buildHome(){
        var snapshot=ClientMetaState.snapshot();
        int px=homePanelX(),py=homePanelY(),pw=homePanelW(),ph=homePanelH();
        int leftW=Math.max(190,pw*58/100);
        int partyX=px+22,partyY=py+38;
        int partyAreaW=leftW-32;
        int cardGap=8;
        int cardW=Math.max(72,(partyAreaW-cardGap)/2);
        int cardH=Math.max(68,Math.min(86,(ph-72-cardGap)/2));

        List<String> party=snapshot.activeParty().stream().limit(4).toList();
        for(int i=0;i<party.size();i++){
            var row=character(party.get(i));
            if(row==null)continue;
            int col=i%2,rowIndex=i/2;
            int x=partyX+col*(cardW+cardGap);
            int y=partyY+rowIndex*(cardH+cardGap);
            String detail=(row.awakened()?"각성":"★"+row.nativeStar())+" · Lv."+row.level();
            addRenderableWidget(new FoozlePortraitButton(
                    x,y,cardW,cardH,row.id(),row.name(),detail,false,
                    ignored->openCharacterFromHome(row.id())));
        }
        if(party.isEmpty()){
            addRenderableWidget(new BattleHudButton(
                    partyX,partyY,partyAreaW,30,Component.literal("파티 편성"),MUTED,
                    ignored->switchTab(Tab.PARTY)));
        }

        int menuX=px+leftW+10;
        int menuY=py+46;
        int menuAreaW=pw-leftW-28;
        int orbGap=6;
        int orbSize=Math.max(50,Math.min(68,(menuAreaW-orbGap)/2));
        int rowGap=8;
        addRenderableWidget(new FoozleOrbButton(menuX,menuY,orbSize,"파티",ignored->switchTab(Tab.PARTY)));
        addRenderableWidget(new FoozleOrbButton(menuX+orbSize+orbGap,menuY,orbSize,"장비",ignored->switchTab(Tab.EQUIPMENT)));
        addRenderableWidget(new FoozleOrbButton(menuX,menuY+orbSize+rowGap,orbSize,"퀘스트",ignored->switchTab(Tab.QUESTS)));
        addRenderableWidget(new FoozleOrbButton(menuX+orbSize+orbGap,menuY+orbSize+rowGap,orbSize,"소환",ignored->switchTab(Tab.ARCHIVE)));
    }

    private int homePanelW(){return Math.min(620,Math.max(320,width-16));}
    private int homePanelH(){return Math.min(330,Math.max(210,height-12));}
    private int homePanelX(){return (width-homePanelW())/2;}
    private int homePanelY(){return (height-homePanelH())/2;}

    private void buildParty(){
        var owned=ClientMetaState.snapshot().characters().stream().filter(ClientMetaState.CharacterRow::owned).toList();
        int gridTop=contentTop()+4,gap=3,cols=panelWidth>=820?4:panelWidth>=620?3:panelWidth>=440?2:1,cardH=34;
        int footerReserve=56;
        int rows=UiPaging.rowsThatFit(gridTop,top+panelHeight-footerReserve,cardH+3,2),per=cols*rows;
        setPaging(owned.size(),per);
        int start=page*per,end=Math.min(owned.size(),start+per),cardW=(panelWidth-32-gap*(cols-1))/cols;
        for(int i=start;i<end;i++){
            var row=owned.get(i);
            int local=i-start,xx=left+16+(local%cols)*(cardW+gap),yy=gridTop+(local/cols)*(cardH+3);
            boolean selected=draftParty.contains(row.id());
            String detail=(row.awakened()?"각성":"★"+row.nativeStar())+" · Lv."+row.level()+" · CP "+row.cp();
            addRenderableWidget(new FoozlePortraitButton(
                    xx,yy,cardW,cardH,row.id(),row.name(),detail,false,selected,
                    ignored->toggleParty(row.id())));
        }
        int py=top+panelHeight-50,px=left+16;
        for(int slot=1;slot<=3;slot++){
            final int s=slot;
            var preset=ClientMetaState.snapshot().partyPresets().size()>=slot?ClientMetaState.snapshot().partyPresets().get(slot-1):List.<String>of();
            int bw=Math.min(80,Math.max(60,(panelWidth-224)/7));
            var load=new BattleHudButton(px,py,bw,20,Component.literal("P"+slot+" 불러오기"),preset.isEmpty()?MUTED:BLUE,ignored->send("PRESET_LOAD|"+s));
            load.active=!preset.isEmpty();
            addRenderableWidget(load);
            addRenderableWidget(new BattleHudButton(px+bw+3,py,46,20,Component.literal("저장"),GREEN,ignored->send("PRESET_SAVE|"+s)));
            px+=bw+52;
        }
        addRenderableWidget(new BattleHudButton(left+panelWidth-128,py,112,20,Component.literal("편성 저장 "+draftParty.size()+"/4"),GREEN,ignored->saveParty()));
        buildPager();
    }

    private void buildCharacters(){
        if(!selectedCharacterId.isBlank()){buildCharacterDetail();return;}
        int y=contentTop(),gap=4,bw=(panelWidth-32-gap*3)/4,x=left+16;
        addRenderableWidget(new BattleHudButton(x,y,bw,CONTROL_H,Component.literal("보유 · "+ownershipLabel()),BLUE,ignored->cycleOwnership()));x+=bw+gap;
        addRenderableWidget(new BattleHudButton(x,y,bw,CONTROL_H,Component.literal("성급 · "+(starFilter==0?"전체":"★"+starFilter)),GOLD,ignored->cycleStar()));x+=bw+gap;
        addRenderableWidget(new BattleHudButton(x,y,bw,CONTROL_H,Component.literal("레벨 · "+(minimumLevel==0?"전체":minimumLevel+"+")),GREEN,ignored->cycleLevel()));x+=bw+gap;
        addRenderableWidget(new BattleHudButton(x,y,bw,CONTROL_H,Component.literal("역할 · "+roleLabel(roleFilter)),MUTED,ignored->cycleRole()));

        List<ClientMetaState.CharacterRow> rows=filteredCharacters();
        int gridTop=y+27,cols=panelWidth>=860?4:panelWidth>=640?3:2,rowH=40,cardGap=4;
        int visibleRows=UiPaging.rowsThatFit(gridTop,contentBottom(),rowH+4,2),per=cols*visibleRows;
        setPaging(rows.size(),per);
        int start=page*per,end=Math.min(rows.size(),start+per),cardW=(panelWidth-32-cardGap*(cols-1))/cols;
        for(int i=start;i<end;i++){
            var row=rows.get(i);
            int local=i-start,xx=left+16+(local%cols)*(cardW+cardGap),yy=gridTop+(local/cols)*(rowH+4);
            String detail=row.owned()?(row.awakened()?"각성":"★"+row.nativeStar())+" · Lv."+row.level()+" · "+primaryRoleLabel(row.primaryRole())
                    :"미보유 · ★"+row.nativeStar()+" · "+primaryRoleLabel(row.primaryRole());
            addRenderableWidget(new FoozlePortraitButton(
                    xx,yy,cardW,rowH,row.id(),row.name(),detail,!row.owned(),
                    ignored->openCharacter(row.id())));
        }
        buildPager();
    }

    private void buildCharacterDetail(){
        var row=character(selectedCharacterId);
        if(row==null){selectedCharacterId="";return;}
        int y=contentTop(),x=left+16;
        addRenderableWidget(new BattleHudButton(x,y,68,CONTROL_H,Component.literal("← 목록"),MUTED,ignored->closeCharacter()));
        int gap=3,available=panelWidth-106,tabW=(available-gap*(DetailTab.values().length-1))/DetailTab.values().length,tx=x+76;
        for(DetailTab value:DetailTab.values()){
            addRenderableWidget(new BattleHudButton(tx,y,tabW,CONTROL_H,Component.literal(detailLabel(value)),value==detailTab?BLUE:MUTED,ignored->switchDetail(value)));
            tx+=tabW+gap;
        }
        if(detailTab==DetailTab.GROWTH&&row.owned()){
            int by=top+panelHeight-34;
            var trial=ClientSignatureTrialState.forCharacter(row.id());
            boolean ready=trial!=null&&trial.awakeningReady();
            String label=row.awakened()?"각성 완료":ready?"각성 · "+GrowthRulesV1.awakeningGoldCost()+"G":"각성 잠김";
            var b=new BattleHudButton(left+panelWidth-166,by,150,20,Component.literal(label),row.awakened()?GREEN:ready?BLUE:MUTED,ignored->send("AWAKEN|"+row.id()));
            b.active=!row.awakened()&&ready;
            addRenderableWidget(b);
        }
    }

    private void buildEquipment(){
        int y=contentTop(),x=left+16;
        addRenderableWidget(new BattleHudButton(x,y,106,CONTROL_H,Component.literal("부위 · "+slotLabel(equipSlotFilter)),MUTED,ignored->cycleEquipSlot()));
        addRenderableWidget(new BattleHudButton(x+112,y,106,CONTROL_H,Component.literal("정렬 · "+sortLabel(equipSort)),MUTED,ignored->cycleEquipSort()));
        List<ClientMetaState.EquipmentRow> rows=filteredEquipment();
        int listTop=y+27,listW=Math.min(420,Math.max(240,panelWidth/2-18)),rowH=23;
        int per=UiPaging.rowsThatFit(listTop,contentBottom(),rowH+3,3);
        setPaging(rows.size(),per);
        int start=page*per,end=Math.min(rows.size(),start+per);
        for(int i=start;i<end;i++){
            var row=rows.get(i);
            int yy=listTop+(i-start)*(rowH+3);
            String owner=row.equippedCharacterId().isBlank()?"":" · "+characterName(row.equippedCharacterId());
            addRenderableWidget(new BattleHudButton(left+16,yy,listW,rowH,Component.literal(row.tier()+" · "+row.name()+" +"+row.enhancement()+owner),row.instanceId().equals(selectedEquipmentId)?BLUE:tierColor(row.tier()),ignored->selectEquipment(row.instanceId())));
        }
        var selected=equipment(selectedEquipmentId);
        int rx=left+26+listW,rw=panelWidth-listW-58;
        if(selected!=null)buildEquipmentActions(selected,rx,listTop,rw);else drawPendingButtons(rx,listTop,rw);
        buildPager();
    }

    private void drawPendingButtons(int rx,int y,int rw){
        var pending=ClientMetaState.snapshot().pendingEquipment();
        if(pending.isEmpty())return;
        var p=pending.getFirst();
        if(p.claimable())addRenderableWidget(new BattleHudButton(rx,y,Math.max(80,rw),20,Component.literal("대기 보상 수령 · "+p.name()),GREEN,ignored->send("REWARD_CLAIM|"+p.instanceId())));
        if(p.immediateSellable())addRenderableWidget(new BattleHudButton(rx,y+24,Math.max(80,rw),20,Component.literal("대기 보상 판매 · "+p.salePrice()+"G"),GOLD,ignored->send("REWARD_SELL|"+p.instanceId())));
    }

    private void buildEquipmentActions(ClientMetaState.EquipmentRow selected,int rx,int y,int rw){
        rw=Math.max(120,rw);
        int by=y+66;
        if(FacilityUiAccess.forge()){
            addRenderableWidget(new BattleHudButton(rx,by,Math.min(108,rw),20,Component.literal(selected.enhancement()>=GrowthRulesV1.maxEnhancement()?"+10 완료":"강화 +1"),GOLD,ignored->send("ENHANCE|"+selected.instanceId())));
        }
        int targetY=by+(FacilityUiAccess.forge()?28:6);
        var owned=ClientMetaState.snapshot().characters().stream().filter(ClientMetaState.CharacterRow::owned).toList();
        int cols=2,gap=3,bw=(rw-gap)/2;
        for(int i=0;i<Math.min(owned.size(),4);i++){
            var c=owned.get(i);
            int xx=rx+(i%cols)*(bw+gap),yy=targetY+(i/cols)*24;
            addRenderableWidget(new BattleHudButton(xx,yy,bw,20,Component.literal(c.name()),c.id().equals(equipmentTargetCharacterId)?GREEN:MUTED,ignored->selectEquipmentTarget(c.id())));
        }
        int actionY=targetY+52;
        var equip=new BattleHudButton(rx,actionY,Math.min(108,rw),20,Component.literal("장착"),GREEN,ignored->equipSelected());
        equip.active=!equipmentTargetCharacterId.isBlank();
        addRenderableWidget(equip);
        var sell=new BattleHudButton(rx,actionY+24,Math.min(184,rw),20,Component.literal(selected.sellable()?"판매 · "+selected.salePrice()+"G":"판매 불가"),selected.sellable()?GOLD:MUTED,ignored->sellSelected());
        sell.active=selected.sellable();
        addRenderableWidget(sell);
    }

    private void buildArchive(){
        var s=ClientMetaState.snapshot();
        int y=contentTop();
        boolean canSummon=FacilityUiAccess.archive();
        var one=new BattleHudButton(left+16,y,120,22,Component.literal("1회 소환 · 300"),canSummon?BLUE:MUTED,ignored->send("SUMMON1"));
        one.active=canSummon&&s.crystal()>=GachaCatalog.SINGLE_COST;addRenderableWidget(one);
        var ten=new BattleHudButton(left+142,y,136,22,Component.literal("10회 소환 · 3000"),canSummon?GOLD:MUTED,ignored->send("SUMMON10"));
        ten.active=canSummon&&s.crystal()>=GachaCatalog.TEN_COST;addRenderableWidget(ten);
        if(s.starterArchiveAvailable()){
            var starter=new BattleHudButton(left+284,y,154,22,Component.literal("초기 10회 · 3000"),canSummon?GREEN:MUTED,ignored->send("STARTER"));
            starter.active=canSummon&&s.crystal()>=GachaCatalog.TEN_COST;addRenderableWidget(starter);
        }
        int listTop=y+34,rowH=18,per=UiPaging.rowsThatFit(listTop,contentBottom(),rowH,5);
        setPaging(s.archiveHistory().size(),per);
        buildPager();
    }

    private void buildQuests(){
        var s=ClientMetaState.snapshot();
        int y=contentTop()+4,paneGap=12;
        int rows=UiPaging.rowsThatFit(y+20,contentBottom(),19,4);
        setPaging(Math.max(s.regionQuests().size(),s.challenges().size()),rows);
        buildPager();
    }

    private void buildCodex(){
        int x=left+16,y=contentTop(),gap=4,count=5,w=(panelWidth-32-gap*(count-1))/count;
        for(String category:List.of("CHARACTERS","ENEMIES","BOSSES","EQUIPMENT","TUTORIAL")){
            addRenderableWidget(new BattleHudButton(x,y,w,CONTROL_H,Component.literal(codexLabel(category)),category.equals(codexCategory)?BLUE:MUTED,ignored->selectCodex(category)));
            x+=w+gap;
        }
        List<ClientMetaState.CodexRow> rows=ClientMetaState.snapshot().codex().stream().filter(r->r.category().equals(codexCategory)).toList();
        int gridTop=y+27,cols=panelWidth>=860?4:panelWidth>=640?3:2,rowH=36,cardGap=4;
        int visible=UiPaging.rowsThatFit(gridTop,contentBottom(),rowH+4,2),per=cols*visible;
        setPaging(rows.size(),per);
        int start=page*per,end=Math.min(rows.size(),start+per),cardW=(panelWidth-32-cardGap*(cols-1))/cols;
        for(int i=start;i<end;i++){
            var row=rows.get(i);
            int local=i-start,xx=left+16+(local%cols)*(cardW+cardGap),yy=gridTop+(local/cols)*(rowH+4);
            String name=((row.category().equals("ENEMIES")||row.category().equals("BOSSES"))&&!row.discovered())?"???":row.name();
            addRenderableWidget(new BattleHudButton(xx,yy,cardW,rowH,Component.literal(name),row.detailUnlocked()?BLUE:row.discovered()?SECONDARY:MUTED,ignored->{}));
        }
        buildPager();
    }

    private void buildSystem(){
        var rows=ClientMetaState.snapshot().endgame();
        if(selectedEndgameId.isBlank()||endgame(selectedEndgameId)==null)selectedEndgameId=rows.stream().filter(ClientMetaState.EndgameRow::unlocked).map(ClientMetaState.EndgameRow::id).findFirst().orElse("");
        int y=contentTop()+2,listW=Math.min(250,panelWidth/3),rowH=24;
        List<ClientMetaState.EndgameRow> hard=rows.stream().filter(r->"HARD".equals(r.kind())).toList();
        for(int i=0;i<hard.size();i++){
            var r=hard.get(i);
            var b=new BattleHudButton(left+16,y+i*(rowH+3),listW,rowH,Component.literal((r.cleared()?"✓ ":"")+r.label()),r.id().equals(selectedEndgameId)?BLUE:r.cleared()?GREEN:r.unlocked()?DANGER:MUTED,ignored->selectEndgame(r.id()));
            b.active=r.unlocked();addRenderableWidget(b);
        }
        List<ClientMetaState.EndgameRow> rifts=rows.stream().filter(r->"RIFT".equals(r.kind())).toList();
        int gridX=left+26+listW,available=panelWidth-listW-42,cols=Math.max(3,Math.min(7,available/68)),gap=4,cellW=(available-gap*(cols-1))/cols,gridTop=y;
        int visibleRows=UiPaging.rowsThatFit(gridTop,contentBottom()-30,23+4,3),per=cols*visibleRows;
        setPaging(rifts.size(),per);
        int start=page*per,end=Math.min(rifts.size(),start+per);
        for(int i=start;i<end;i++){
            var r=rifts.get(i);
            int local=i-start,xx=gridX+(local%cols)*(cellW+gap),yy=gridTop+(local/cols)*27;
            var b=new BattleHudButton(xx,yy,cellW,23,Component.literal(r.label()),r.id().equals(selectedEndgameId)?BLUE:r.cleared()?GREEN:r.hardPattern()?GOLD:r.unlocked()?SECONDARY:MUTED,ignored->selectEndgame(r.id()));
            b.active=r.unlocked();addRenderableWidget(b);
        }
        var selected=endgame(selectedEndgameId);
        if(selected!=null&&selected.unlocked())addRenderableWidget(new BattleHudButton(left+panelWidth-158,top+panelHeight-34,142,20,Component.literal("브리핑 / 출전"),"HARD".equals(selected.kind())?DANGER:BLUE,ignored->send("START|"+selected.id())));
        buildPager();
    }

    private void setPaging(int total,int per){currentTotal=Math.max(0,total);currentPerPage=Math.max(1,per);page=UiPaging.clampPage(page,currentTotal,currentPerPage);}

    private void buildPager(){
        int pages=UiPaging.pageCount(currentTotal,currentPerPage);
        if(pages<=1)return;
        int y=top+panelHeight-31,center=left+panelWidth/2;
        var prev=new BattleHudButton(center-100,y,62,19,Component.literal("< 이전"),MUTED,ignored->movePage(-1));prev.active=page>0;addRenderableWidget(prev);
        var next=new BattleHudButton(center+38,y,62,19,Component.literal("다음 >"),MUTED,ignored->movePage(1));next.active=page+1<pages;addRenderableWidget(next);
    }

    @Override
    public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY){
        if(currentTotal>currentPerPage&&scrollY!=0){movePage(scrollY>0?-1:1);return true;}
        return super.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
    }

    private void movePage(int delta){page=UiPaging.clampPage(page+delta,currentTotal,currentPerPage);rebuild();}
    private void rebuild(){clearWidgets();init();}
    private void switchTab(Tab value){if(value==tab)return;tab=value;page=0;selectedCharacterId="";selectedEquipmentId="";rebuild();}
    private void openCharacterFromHome(String id){tab=Tab.CHARACTERS;selectedCharacterId=id;detailTab=DetailTab.OVERVIEW;page=0;rebuild();}
    private void openMap(){Minecraft.getInstance().gui.setScreen(new DrehmalWorldMapScreen());}
    private void toggleParty(String id){if(draftParty.contains(id)){if(draftParty.size()>1)draftParty.remove(id);}else if(draftParty.size()<4)draftParty.add(id);rebuild();}
    private void saveParty(){send("PARTY|"+String.join(",",draftParty));}
    private void openCharacter(String id){selectedCharacterId=id;detailTab=DetailTab.OVERVIEW;page=0;rebuild();}
    private void closeCharacter(){selectedCharacterId="";page=0;rebuild();}
    private void switchDetail(DetailTab d){detailTab=d;rebuild();}
    private void cycleOwnership(){ownershipFilter=OwnershipFilter.values()[(ownershipFilter.ordinal()+1)%OwnershipFilter.values().length];page=0;rebuild();}
    private void cycleStar(){starFilter=switch(starFilter){case 0->3;case 3->4;case 4->5;default->0;};page=0;rebuild();}
    private void cycleLevel(){minimumLevel=minimumLevel==0?10:minimumLevel>=60?0:minimumLevel+10;page=0;rebuild();}
    private void cycleRole(){roleFilter=RoleFilter.values()[(roleFilter.ordinal()+1)%RoleFilter.values().length];page=0;rebuild();}
    private void cycleEquipSlot(){List<String>v=List.of("ALL","WEAPON","ARMOR","ACCESSORY","SIGNATURE");equipSlotFilter=v.get((v.indexOf(equipSlotFilter)+1)%v.size());page=0;rebuild();}
    private void cycleEquipSort(){equipSort=EquipSort.values()[(equipSort.ordinal()+1)%EquipSort.values().length];page=0;rebuild();}
    private void selectEquipment(String id){selectedEquipmentId=id;if(equipmentTargetCharacterId.isBlank())equipmentTargetCharacterId=ClientMetaState.snapshot().activeParty().stream().findFirst().orElse("");rebuild();}
    private void selectEquipmentTarget(String id){equipmentTargetCharacterId=id;rebuild();}
    private void equipSelected(){if(!selectedEquipmentId.isBlank()&&!equipmentTargetCharacterId.isBlank())send("EQUIP|"+equipmentTargetCharacterId+"|"+selectedEquipmentId);}
    private void sellSelected(){var e=equipment(selectedEquipmentId);if(e!=null&&e.sellable())send("SELL|"+e.instanceId());}
    private void selectCodex(String c){codexCategory=c;page=0;rebuild();}
    private void selectEndgame(String id){selectedEndgameId=id;rebuild();}
    private static void send(String command){ClientPacketDistributor.sendToServer(new MetaCommandPayload(command));}

    @Override
    public boolean keyPressed(KeyEvent event){
        if(event.key()==GLFW.GLFW_KEY_E){onClose();return true;}
        if(event.key()==GLFW.GLFW_KEY_ESCAPE){
            if(!selectedCharacterId.isBlank()){selectedCharacterId="";tab=Tab.HOME;page=0;rebuild();return true;}
            if(tab!=Tab.HOME){switchTab(Tab.HOME);return true;}
            onClose();return true;
        }
        return super.keyPressed(event);
    }

    @Override public void extractBackground(@NotNull GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){}

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){
        if(tab==Tab.HOME){
            drawHome(graphics);
            super.extractRenderState(graphics,mouseX,mouseY,partialTick);
            return;
        }

        TurnboundFrameStyle.frame(graphics,left,top,panelWidth,panelHeight,BLUE);
        TurnboundFrameStyle.inset(graphics,left+12,top+27,panelWidth-24,20);
        graphics.text(font,Component.literal("TURNBOUND"),left+16,top+12,TEXT,true);
        var s=ClientMetaState.snapshot();
        String resources="골드 "+s.gold()+"   크리스탈 "+s.crystal()+"   별의 정수 "+s.essence()+"   파티 CP "+s.partyCp();
        graphics.text(font,Component.literal(UiTextLayout.fit(resources,panelWidth-40)),left+20,top+33,SECONDARY,false);
        graphics.text(font,Component.literal(title(tab)),left+16,contentTop()-13,TEXT,true);
        switch(tab){
            case HOME->{}
            case PARTY->drawParty(graphics);
            case CHARACTERS->drawCharacters(graphics);
            case EQUIPMENT->drawEquipment(graphics);
            case ARCHIVE->drawArchive(graphics);
            case QUESTS->drawQuests(graphics);
            case CODEX->drawCodex(graphics);
            case SYSTEM->drawSystem(graphics);
        }
        int pages=UiPaging.pageCount(currentTotal,currentPerPage);
        if(pages>1){
            String p=(page+1)+" / "+pages+" · 휠 스크롤";
            graphics.text(font,Component.literal(p),left+panelWidth/2-font.width(p)/2,top+panelHeight-27,MUTED,false);
        }
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
    }

    private void drawHome(GuiGraphicsExtractor g){
        int px=homePanelX(),py=homePanelY(),pw=homePanelW(),ph=homePanelH();
        int leftW=Math.max(190,pw*58/100);

        TurnboundUiSkin.panel(g,px,py,pw,ph);
        g.text(font,Component.literal("TURNBOUND"),px+22,py+16,TEXT,true);
        g.text(font,Component.literal("현재 파티"),px+22,py+30,SECONDARY,false);
        g.text(font,Component.literal("여행 준비"),px+leftW+12,py+30,SECONDARY,false);

        var s=ClientMetaState.snapshot();
        String resources="골드 "+s.gold()+"  ·  크리스탈 "+s.crystal()+"  ·  별의 정수 "+s.essence()+"  ·  파티 CP "+s.partyCp();
        g.text(font,Component.literal(UiTextLayout.fit(resources,pw-44)),
                px+22,py+ph-22,SECONDARY,false);
        g.text(font,Component.literal("M 지도  ·  N 미니맵  ·  E 닫기"),
                px+pw-22-font.width("M 지도  ·  N 미니맵  ·  E 닫기"),py+16,MUTED,false);
    }

    private void drawParty(GuiGraphicsExtractor g){
        String hint="최대 4인 · 전투 참가 100% 경험치 · 대기 보유 캐릭터 20%";
        int hintX=compactLayout?left+112:left+132;
        int hintW=Math.max(80,left+panelWidth-16-hintX);
        g.text(font,Component.literal(UiTextLayout.fit(hint,hintW)),hintX,contentTop()-13,SECONDARY,false);
    }

    private void drawCharacters(GuiGraphicsExtractor g){
        if(selectedCharacterId.isBlank()) return;
        var r=character(selectedCharacterId);if(r==null)return;
        int portraitX=left+18,portraitY=contentTop()+27;
        int portraitSize=Math.min(150,Math.max(92,Math.min(panelWidth/4,contentBottom()-portraitY-6)));
        TurnboundUiSkin.orbBase(g,portraitX,portraitY,portraitSize);
        int portraitInset=Math.max(10,portraitSize/7);
        TurnboundPortraitRenderer.extractBust(
                g,r.id(),
                portraitX+portraitInset,portraitY+portraitInset,
                portraitX+portraitSize-portraitInset,portraitY+portraitSize-portraitInset,
                !r.owned());
        TurnboundUiSkin.orbOverlay(g,portraitX,portraitY,portraitSize,r.owned(),false,false);
        int x=portraitX+portraitSize+16,y=contentTop()+30,w=Math.max(80,left+panelWidth-18-x);
        g.text(font,Component.literal(UiTextLayout.fit(r.name()+" · "+(r.owned()?(r.awakened()?"각성 · ":"")+"★"+r.nativeStar()+" Lv."+r.level():"미보유 · ★"+r.nativeStar()),w)),x,y,r.owned()?TEXT:MUTED,true);
        g.text(font,Component.literal(UiTextLayout.fit(r.role(),w)),x,y+15,SECONDARY,false);
        switch(detailTab){
            case OVERVIEW->{
                g.text(font,Component.literal("HP "+r.hp()+"   ATK "+r.attack()+"   DEF "+r.defense()+"   SPD "+r.speed()),x,y+38,GREEN,false);
                g.text(font,Component.literal("전투력 "+r.cp()+" · "+primaryRoleLabel(r.primaryRole())+" · "+r.difficulty()),x,y+56,TEXT,false);
            }
            case SKILLS->{
                var d=CanonicalData.definition(r.id(),Math.max(1,r.level()),Math.max(1,r.star()),r.awakened());
                int yy=y+36;
                for(var skill:d.skills()){
                    g.text(font,Component.literal(UiTextLayout.fit(skill.name()+" · 쿨타임 "+skill.cooldown()+" · "+skill.description(),w)),x,yy,skill.isBasic()?SECONDARY:GOLD,false);
                    yy+=19;
                }
            }
            case EQUIPMENT->{
                int yy=y+36;
                for(String slot:List.of("WEAPON","ARMOR","ACCESSORY","SIGNATURE")){
                    var item=ClientMetaState.snapshot().equipment().stream().filter(e->e.equippedCharacterId().equals(r.id())&&e.slot().equals(slot)).findFirst().orElse(null);
                    String text=slotLabel(slot)+" · "+(item==null?"비어 있음":item.name()+" +"+item.enhancement());
                    g.text(font,Component.literal(UiTextLayout.fit(text,w)),x,yy,item==null?MUTED:tierColor(item.tier()),false);
                    yy+=19;
                }
            }
            case GROWTH->{
                var trial=ClientSignatureTrialState.forCharacter(r.id());
                String status=r.awakened()?"각성 완료":trial!=null&&trial.awakeningReady()?"각성 가능":"선행 조건 진행 중";
                g.text(font,Component.literal(status),x,y+38,r.awakened()?GREEN:GOLD,true);
                g.text(font,Component.literal("각성 조건 · Lv60 · 개인 퀘스트 · "+GrowthRulesV1.awakeningGoldCost()+" Gold"),x,y+58,SECONDARY,false);
                if(trial!=null)g.text(font,Component.literal(UiTextLayout.fit("전용 장비 시련 · "+trial.title()+" · "+trial.objective(),w)),x,y+76,SECONDARY,false);
            }
        }
    }

    private void drawEquipment(GuiGraphicsExtractor g){
        var selected=equipment(selectedEquipmentId);if(selected==null)return;
        int listW=Math.min(420,Math.max(240,panelWidth/2-18)),x=left+26+listW,y=contentTop()+29,w=panelWidth-listW-58;
        g.text(font,Component.literal(UiTextLayout.fit(selected.tier()+" · "+selected.name()+" +"+selected.enhancement(),w)),x,y,tierColor(selected.tier()),true);
        g.text(font,Component.literal(UiTextLayout.fit(statTypeLabel(selected.mainType())+" "+stat(selected.mainValue())+" · "+statTypeLabel(selected.subType())+" "+stat(selected.subValue()),w)),x,y+18,TEXT,false);
        g.text(font,Component.literal(UiTextLayout.fit("+10 · "+statTypeLabel(selected.mainType())+" "+stat(selected.mainAt20())+" / "+statTypeLabel(selected.subType())+" "+stat(selected.subAt20()),w)),x,y+36,GOLD,false);
    }

    private void drawArchive(GuiGraphicsExtractor g){
        var s=ClientMetaState.snapshot();
        int y=contentTop()+35;
        g.text(font,Component.literal("★5 천장 "+s.fiveStarPity()+" / "+GachaCatalog.HARD_PITY+" · ★5 "+Math.round(GachaCatalog.BASE_FIVE_STAR_RATE*100.0)+"% · 10회 최소 ★4"),left+16,y-10,GOLD,false);
        int start=page*currentPerPage,end=Math.min(s.archiveHistory().size(),start+currentPerPage),yy=y+8;
        for(int i=start;i<end;i++){
            var r=s.archiveHistory().get(i);
            String text="★"+r.nativeStars()+" · "+r.name()+(r.newlyOwned()?" · 신규":" · 별의 정수 +"+r.essenceGranted());
            g.text(font,Component.literal(UiTextLayout.fit(text,panelWidth-32)),left+16,yy,r.newlyOwned()?GREEN:SECONDARY,false);
            yy+=18;
        }
    }

    private void drawQuests(GuiGraphicsExtractor g){
        var s=ClientMetaState.snapshot();
        int y=contentTop()+4,paneGap=12,paneW=(panelWidth-44-paneGap)/2,leftX=left+16,rightX=leftX+paneW+paneGap;
        g.text(font,Component.literal("퀘스트"),leftX,y,TEXT,true);
        g.text(font,Component.literal("도전"),rightX,y,TEXT,true);
        int start=page*currentPerPage,yy=y+20;
        for(int i=start;i<Math.min(s.regionQuests().size(),start+currentPerPage);i++){
            var q=s.regionQuests().get(i);
            String text=(q.completed()?"✓ ":"○ ")+q.region()+" · "+q.id();
            g.text(font,Component.literal(UiTextLayout.fit(text,paneW)),leftX,yy,q.completed()?GREEN:TEXT,false);
            yy+=19;
        }
        yy=y+20;
        for(int i=start;i<Math.min(s.challenges().size(),start+currentPerPage);i++){
            var c=s.challenges().get(i);
            String text=(c.completed()?"✓ ":"○ ")+c.ordinal()+". "+c.label();
            g.text(font,Component.literal(UiTextLayout.fit(text,paneW)),rightX,yy,c.completed()?GREEN:c.autoEvaluable()?TEXT:GOLD,false);
            yy+=19;
        }
    }

    private void drawCodex(GuiGraphicsExtractor g){
        List<ClientMetaState.CodexRow> rows=ClientMetaState.snapshot().codex().stream().filter(r->r.category().equals(codexCategory)).toList();
        int start=page*currentPerPage,end=Math.min(rows.size(),start+currentPerPage),gridTop=contentTop()+27,cols=panelWidth>=860?4:panelWidth>=640?3:2,rowH=36,gap=4,cardW=(panelWidth-32-gap*(cols-1))/cols;
        for(int i=start;i<end;i++){
            var r=rows.get(i);
            int local=i-start,x=left+16+(local%cols)*(cardW+gap),y=gridTop+(local/cols)*(rowH+4);
            String detail=r.detailUnlocked()?r.summary():r.discovered()?"상세 정보 잠김":"미발견";
            g.text(font,Component.literal(UiTextLayout.fit(detail,cardW-16)),x+8,y+22,r.detailUnlocked()?SECONDARY:MUTED,false);
        }
    }

    private void drawSystem(GuiGraphicsExtractor g){
        var selected=endgame(selectedEndgameId);if(selected==null)return;
        int y=contentBottom()-24;
        String s=(selected.cleared()?"클리어 · ":"")+selected.label()+" · Lv."+selected.level();
        g.text(font,Component.literal(UiTextLayout.fit(s,panelWidth-200)),left+16,y,selected.cleared()?GREEN:TEXT,false);
    }

    private List<ClientMetaState.CharacterRow> filteredCharacters(){
        Comparator<ClientMetaState.CharacterRow> c=Comparator.comparingInt(ClientMetaState.CharacterRow::nativeStar).reversed().thenComparing(ClientMetaState.CharacterRow::id);
        return ClientMetaState.snapshot().characters().stream()
                .filter(r->ownershipFilter==OwnershipFilter.ALL||(ownershipFilter==OwnershipFilter.OWNED)==r.owned())
                .filter(r->starFilter==0||r.nativeStar()==starFilter)
                .filter(r->minimumLevel==0||(r.owned()&&r.level()>=minimumLevel))
                .filter(r->roleFilter==RoleFilter.ALL||r.primaryRole().equals(roleFilter.name()))
                .sorted(c).toList();
    }

    private List<ClientMetaState.EquipmentRow> filteredEquipment(){
        Comparator<ClientMetaState.EquipmentRow> c=switch(equipSort){
            case LEVEL->Comparator.comparingInt(ClientMetaState.EquipmentRow::enhancement).reversed().thenComparing(ClientMetaState.EquipmentRow::name);
            case STAT->Comparator.comparing(ClientMetaState.EquipmentRow::mainType).thenComparing(ClientMetaState.EquipmentRow::name);
            case TIER->Comparator.comparingInt((ClientMetaState.EquipmentRow r)->tierRank(r.tier())).reversed().thenComparing(ClientMetaState.EquipmentRow::name);
        };
        return ClientMetaState.snapshot().equipment().stream().filter(r->equipSlotFilter.equals("ALL")||r.slot().equals(equipSlotFilter)).sorted(c).toList();
    }

    private ClientMetaState.CharacterRow character(String id){return ClientMetaState.snapshot().characters().stream().filter(r->r.id().equals(id)).findFirst().orElse(null);}
    private ClientMetaState.EquipmentRow equipment(String id){return ClientMetaState.snapshot().equipment().stream().filter(r->r.instanceId().equals(id)).findFirst().orElse(null);}
    private ClientMetaState.EndgameRow endgame(String id){return ClientMetaState.snapshot().endgame().stream().filter(r->r.id().equals(id)).findFirst().orElse(null);}

    private String ownershipLabel(){return switch(ownershipFilter){case ALL->"전체";case OWNED->"보유";case UNOWNED->"미보유";};}
    private static String roleLabel(RoleFilter r){return switch(r){case ALL->"전체";case DPS->"공격";case SUPPORT->"지원";case TANK->"수호";case SUMMON->"소환";};}
    private static String sortLabel(EquipSort s){return switch(s){case TIER->"등급";case LEVEL->"강화";case STAT->"능력치";};}
    private static String label(Tab t){return switch(t){case HOME->"빠른 메뉴";case PARTY->"파티";case CHARACTERS->"캐릭터";case EQUIPMENT->"장비";case ARCHIVE->"소환";case QUESTS->"퀘스트";case CODEX->"도감";case SYSTEM->"도전";};}
    private static String title(Tab t){return switch(t){case HOME->"빠른 메뉴";case PARTY->"파티 편성";case CHARACTERS->"캐릭터";case EQUIPMENT->"장비";case ARCHIVE->"소환 / 기록";case QUESTS->"퀘스트";case CODEX->"도감";case SYSTEM->"도전 콘텐츠";};}
    private static String detailLabel(DetailTab d){return switch(d){case OVERVIEW->"개요";case SKILLS->"스킬";case EQUIPMENT->"장비";case GROWTH->"성장";};}
    private static String primaryRoleLabel(String r){return switch(r){case"DPS"->"공격";case"SUPPORT"->"지원";case"TANK"->"수호";case"SUMMON"->"소환";default->r;};}
    private static String slotLabel(String s){return switch(s){case"WEAPON"->"무기";case"ARMOR"->"방어구";case"ACCESSORY"->"장신구";case"SIGNATURE"->"전용 장비";case"ALL"->"전체";default->s;};}
    private static String codexLabel(String c){return switch(c){case"CHARACTERS"->"캐릭터";case"ENEMIES"->"적";case"BOSSES"->"보스";case"EQUIPMENT"->"장비";default->"튜토리얼";};}
    private static int tierRank(String t){return switch(t){case"SIGNATURE"->5;case"T4"->4;case"T3"->3;case"T2"->2;case"T1"->1;default->0;};}
    private static int tierColor(String t){return switch(t){case"SIGNATURE"->0xFFC794FF;case"T4"->0xFFFFC857;case"T3"->0xFFB68CFF;case"T2"->0xFF6DC6FF;default->0xFFAEB7C6;};}
    private static String stat(double v){return Math.abs(v)<=1.0?String.format(Locale.ROOT,"%.1f%%",v*100):String.format(Locale.ROOT,"%.1f",v);}
    private static String statTypeLabel(String t){return switch(t){case"HP_FLAT"->"HP";case"HP_PERCENT"->"HP%";case"ATK_FLAT"->"ATK";case"ATK_PERCENT"->"ATK%";case"DEF_FLAT"->"DEF";case"DEF_PERCENT"->"DEF%";case"SPD_FLAT"->"SPD";case"SPD_PERCENT"->"SPD%";default->t;};}
    private static String characterName(String id){return ClientMetaState.snapshot().characters().stream().filter(r->r.id().equals(id)).map(ClientMetaState.CharacterRow::name).findFirst().orElse(id);}

    @Override public boolean isPauseScreen(){return false;}
}
