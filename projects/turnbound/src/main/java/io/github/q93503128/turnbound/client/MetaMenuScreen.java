package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.CharacterPassiveCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.network.MetaCommandPayload;
import io.github.q93503128.turnbound.network.PartyCommandPayload;
import io.github.q93503128.turnbound.progression.GachaCatalog;
import io.github.q93503128.turnbound.progression.GrowthRulesV1;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
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
    public enum Tab { HOME, PARTY, COOP, CHARACTERS, EQUIPMENT, ARCHIVE, QUESTS, CODEX, CHALLENGES, SYSTEM }
    private enum DetailTab { OVERVIEW, SKILLS, EQUIPMENT, GROWTH }
    private enum OwnershipFilter { ALL, OWNED, UNOWNED }
    private enum RoleFilter { ALL, DPS, SUPPORT, TANK, SUMMON }
    private enum EquipSort { TIER, LEVEL, STAT }
    private record DetailLine(String text, int color) {}

    private static final int TEXT=0xFFF4F0E6, SECONDARY=0xFFAEB7C6, MUTED=0xFF707987;
    private static final int BLUE=0xFF6DC6FF, GREEN=0xFF62D39A, GOLD=0xFFFFC857, PURPLE=0xFFC794FF, DANGER=0xFFFF6B6B;
    private static final int CONTENT_OFFSET=72, FOOTER_OFFSET=30, CONTROL_H=16;
    private static final int COMPACT_CONTENT_OFFSET=68, COMPACT_FOOTER_OFFSET=26;

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
    private int selectedSkillIndex;
    private int skillDescriptionScroll;
    private boolean archiveLogOpen;
    private int archiveLogScroll;
    private long seenPartyRevision=-1L;

    public MetaMenuScreen(Tab tab){
        super(Component.literal("TURNBOUND"));
        this.tab=tab==null?Tab.HOME:tab;
        draftParty.addAll(ClientMetaState.snapshot().activeParty());
        seenPartyRevision=ClientMultiplayerPartyState.revision();
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
        panelWidth=Math.min(660,Math.max(1,width-margin*2));
        panelHeight=Math.min(390,Math.max(1,height-margin*2));
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
            case COOP->buildCoop();
            case CHARACTERS->buildCharacters();
            case EQUIPMENT->buildEquipment();
            case ARCHIVE->buildArchive();
            case QUESTS->buildQuests();
            case CODEX->buildCodex();
            case CHALLENGES->buildChallenges();
            case SYSTEM->buildSystem();
        }
    }

    private int contentTop(){return top+contentOffset;}
    private int contentBottom(){return top+panelHeight-footerOffset;}

    private void buildTabs(){
        if(tab==Tab.HOME)return;
        addRenderableWidget(new BattleHudButton(left+12,top+45,82,CONTROL_H,Component.literal("← 메뉴"),MUTED,ignored->switchTab(Tab.HOME)));
    }

    private void buildHome(){
        var snapshot=ClientMetaState.snapshot();
        int px=homePanelX(),py=homePanelY(),pw=homePanelW(),ph=homePanelH();
        int leftW=Math.max(185,pw*54/100);
        int partyX=px+18,partyY=py+38;
        int partyAreaW=leftW-26;
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
            String detail=(row.awakened()?"각성":"★"+row.nativeStar())+" · "+levelLabel(row);
            addRenderableWidget(new FoozlePortraitButton(
                    x,y,cardW,cardH,row.id(),row.name(),detail,false,
                    ignored->openCharacterFromHome(row.id())));
        }
        if(party.isEmpty()){
            addRenderableWidget(new BattleHudButton(
                    partyX,partyY,partyAreaW,30,Component.literal("파티 편성"),MUTED,
                    ignored->switchTab(Tab.PARTY)));
        }

        int menuX=px+leftW+8;
        int menuAreaW=pw-leftW-22;
        int orbGap=6;
        int rowGap=6;
        Tab[] destinations={Tab.PARTY,Tab.EQUIPMENT,Tab.QUESTS,Tab.ARCHIVE,Tab.CODEX,Tab.CHALLENGES,Tab.SYSTEM};
        String[] labels={"편성","장비","퀘스트","기록","도감","고난도","설정"};
        int menuCols=3;
        int menuRows=(destinations.length+menuCols-1)/menuCols;
        int orbSize=Math.max(36,Math.min(48,Math.min(
                (menuAreaW-orbGap*(menuCols-1))/menuCols,
                (ph-70-rowGap*(menuRows-1))/menuRows)));
        int menuHeight=orbSize*menuRows+rowGap*(menuRows-1);
        int menuY=py+Math.max(34,(ph-menuHeight)/2);
        for(int i=0;i<destinations.length;i++){
            final Tab destination=destinations[i];
            int col=i%menuCols,row=i/menuCols;
            addRenderableWidget(new FoozleOrbButton(
                    menuX+col*(orbSize+orbGap),
                    menuY+row*(orbSize+rowGap),
                    orbSize,
                    labels[i],
                    ignored->switchTab(destination)));
        }
    }

    private int homePanelW(){return Math.min(620,Math.max(1,width-16));}
    private int homePanelH(){return Math.min(330,Math.max(1,height-12));}
    private int homePanelX(){return (width-homePanelW())/2;}
    private int homePanelY(){return (height-homePanelH())/2;}

    private void buildParty(){
        var owned=ClientMetaState.snapshot().characters().stream().filter(ClientMetaState.CharacterRow::owned).toList();
        int gridTop=contentTop()+2,gap=4;
        int cols=panelWidth>=700?5:panelWidth>=520?4:3;
        int cardH=36;
        int footerTop=top+panelHeight-72;
        int rows=UiPaging.rowsThatFit(gridTop,footerTop-5,cardH+4,2),per=cols*rows;
        setPaging(owned.size(),per);
        int start=page*per,end=Math.min(owned.size(),start+per),cardW=(panelWidth-24-gap*(cols-1))/cols;
        for(int i=start;i<end;i++){
            var row=owned.get(i);
            int local=i-start,xx=left+12+(local%cols)*(cardW+gap),yy=gridTop+(local/cols)*(cardH+4);
            boolean selected=draftParty.contains(row.id());
            String detail=(row.awakened()?"각성":"★"+row.nativeStar())+" · "+levelLabel(row)+" · CP "+row.cp();
            addRenderableWidget(new FoozlePortraitButton(
                    xx,yy,cardW,cardH,row.id(),row.name(),detail,false,selected,
                    ignored->toggleParty(row.id())));
        }

        int footerX=left+12,footerW=panelWidth-24,footerGap=4;
        int actionW=Math.min(104,Math.max(82,footerW/6));
        int presetAreaW=footerW-actionW-footerGap;
        int presetW=(presetAreaW-footerGap*2)/3;
        int row1=top+panelHeight-43,row2=row1+20;
        for(int slot=1;slot<=3;slot++){
            final int s=slot;
            int xx=footerX+(slot-1)*(presetW+footerGap);
            var preset=ClientMetaState.snapshot().partyPresets().size()>=slot?ClientMetaState.snapshot().partyPresets().get(slot-1):List.<String>of();
            var load=new BattleHudButton(xx,row1,presetW,17,
                    Component.literal("프리셋 "+slot+" 불러오기"),preset.isEmpty()?SECONDARY:BLUE,ignored->send("PRESET_LOAD|"+s));
            load.active=!preset.isEmpty();addRenderableWidget(load);
            addRenderableWidget(new BattleHudButton(xx,row2,presetW,17,
                    Component.literal("프리셋 "+slot+" 저장"),GREEN,ignored->send("PRESET_SAVE|"+s)));
        }
        int actionX=footerX+presetAreaW+footerGap;
        addRenderableWidget(new BattleHudButton(actionX,row1,actionW,17,Component.literal("협동 파티"),BLUE,ignored->switchTab(Tab.COOP)));
        addRenderableWidget(new BattleHudButton(actionX,row2,actionW,17,
                Component.literal("편성 적용 "+draftParty.size()+"/4"),GREEN,ignored->saveParty()));
        buildPagerAt(top+panelHeight-65);
    }


    private void buildCoop(){
        var party=ClientMultiplayerPartyState.snapshot();
        int x=left+16,w=panelWidth-32;
        int cursor=contentTop()+25;

        if(party.size()>1){
            addRenderableWidget(new BattleHudButton(
                    x+w-62,contentTop()+1,62,18,Component.literal("파티 탈퇴"),DANGER,
                    ignored->partyCommand("LEAVE")));
        }

        if(party.pendingInvite()!=null){
            int actionW=Math.min(58,Math.max(46,w/7));
            addRenderableWidget(new BattleHudButton(
                    x+w-actionW*2-4,cursor+5,actionW,20,Component.literal("수락"),GREEN,
                    ignored->partyCommand("ACCEPT")));
            addRenderableWidget(new BattleHudButton(
                    x+w-actionW,cursor+5,actionW,20,Component.literal("거절"),DANGER,
                    ignored->partyCommand("DECLINE")));
            cursor+=38;
        }

        int memberRowsY=cursor+14;
        for(int i=0;i<party.members().size();i++){
            var member=party.members().get(i);
            if(party.localLeader()&&!member.self()){
                addRenderableWidget(new BattleHudButton(
                        x+w-56,memberRowsY+i*28+4,52,19,Component.literal("내보내기"),DANGER,
                        ignored->partyCommand("KICK|"+member.id())));
            }
        }

        int candidateHeaderY=memberRowsY+Math.max(1,party.members().size())*28+8;
        int candidateRowsY=candidateHeaderY+14;
        int available=Math.max(26,contentBottom()-candidateRowsY-2);
        int per=Math.max(1,available/26);
        setPaging(party.candidates().size(),per);
        int start=page*per,end=Math.min(party.candidates().size(),start+per);
        for(int i=start;i<end;i++){
            var candidate=party.candidates().get(i);
            int row=candidateRowsY+(i-start)*26;
            var invite=new BattleHudButton(
                    x+w-56,row+3,52,19,Component.literal(candidate.inBattle()?"전투 중":"초대"),candidate.inBattle()?MUTED:BLUE,
                    ignored->partyCommand("INVITE|"+candidate.id()));
            invite.active=!candidate.inBattle();
            addRenderableWidget(invite);
        }
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
        int gridTop=y+21,gridBottom=contentBottom()-22,cols=panelWidth>=600?6:panelWidth>=500?5:4,rowH=34,cardGap=3;
        int visibleRows=UiPaging.rowsThatFit(gridTop,gridBottom,rowH+4,2),per=cols*visibleRows;
        setPaging(rows.size(),per);
        int start=page*per,end=Math.min(rows.size(),start+per),cardW=(panelWidth-32-cardGap*(cols-1))/cols;
        for(int i=start;i<end;i++){
            var row=rows.get(i);
            int local=i-start,xx=left+16+(local%cols)*(cardW+cardGap),yy=gridTop+(local/cols)*(rowH+4);
            String detail=row.owned()?(row.awakened()?"각성":"★"+row.nativeStar())+" · "+levelLabel(row)+" · "+primaryRoleLabel(row.primaryRole())
                    :"★"+row.nativeStar()+" · "+primaryRoleLabel(row.primaryRole());
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
        int gap=3,available=panelWidth-32,tabW=(available-gap*(DetailTab.values().length-1))/DetailTab.values().length,tx=x;
        for(DetailTab value:DetailTab.values()){
            addRenderableWidget(new BattleHudButton(tx,y,tabW,CONTROL_H,Component.literal(detailLabel(value)),value==detailTab?BLUE:MUTED,ignored->switchDetail(value)));
            tx+=tabW+gap;
        }
        if(detailTab==DetailTab.SKILLS){
            var definition=CanonicalData.definition(row.id(),Math.max(1,row.effectiveLevel()),Math.max(1,row.star()),row.awakened());
            var skills=definition.skills();
            var passives=CharacterPassiveCatalog.forOwner(row.id());
            int slotCount=skills.size()+(passives.isEmpty()?0:1);
            selectedSkillIndex=Math.max(0,Math.min(selectedSkillIndex,Math.max(0,slotCount-1)));
            if(slotCount>0){
                int sx=skillRailX(),by=skillRailY(),bw=skillRailW(),buttonH=22,gapSkill=4;
                for(int i=0;i<skills.size();i++){
                    final int index=i;
                    var skill=skills.get(i);
                    String prefix=skill.isBasic()?"기본 · ":"기술 · ";
                    addRenderableWidget(new BattleHudButton(
                            sx,by+i*(buttonH+gapSkill),bw,buttonH,
                            Component.literal(prefix+skill.name()),
                            i==selectedSkillIndex?(skill.isBasic()?GREEN:GOLD):MUTED,
                            ignored->{selectedSkillIndex=index;skillDescriptionScroll=0;rebuild();}));
                }
                if(!passives.isEmpty()){
                    int index=skills.size();
                    addRenderableWidget(new BattleHudButton(
                            sx,by+index*(buttonH+gapSkill),bw,buttonH,
                            Component.literal("패시브 · 고유 효과"),
                            selectedSkillIndex==index?PURPLE:MUTED,
                            ignored->{selectedSkillIndex=index;skillDescriptionScroll=0;rebuild();}));
                }
            }
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
        ensureEquipmentTarget();
        int listW=equipmentListWidth();
        addRenderableWidget(new BattleHudButton(x,y,96,CONTROL_H,Component.literal("부위 · "+slotLabel(equipSlotFilter)),MUTED,ignored->cycleEquipSlot()));
        addRenderableWidget(new BattleHudButton(x+102,y,96,CONTROL_H,Component.literal("정렬 · "+sortLabel(equipSort)),MUTED,ignored->cycleEquipSort()));

        int targetX=x+204,targetW=Math.max(116,left+panelWidth-16-targetX);
        if(!ownedEquipmentTargets().isEmpty()){
            int arrowW=24,centerW=Math.max(64,targetW-arrowW*2-6);
            addRenderableWidget(new BattleHudButton(targetX,y,arrowW,CONTROL_H,Component.literal("‹"),MUTED,ignored->cycleEquipmentTarget(-1)));
            addRenderableWidget(new BattleHudButton(targetX+arrowW+3,y,centerW,CONTROL_H,
                    Component.literal("장착 대상 · "+equipmentTargetName()),GREEN,ignored->cycleEquipmentTarget(1)));
            addRenderableWidget(new BattleHudButton(targetX+arrowW+3+centerW+3,y,arrowW,CONTROL_H,Component.literal("›"),MUTED,ignored->cycleEquipmentTarget(1)));
        }

        List<ClientMetaState.EquipmentRow> rows=filteredEquipment();
        int listTop=y+21,rowH=19;
        int per=UiPaging.rowsThatFit(listTop,contentBottom(),rowH+2,4);
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
        int actionY=Math.min(contentBottom()-19,y+116);
        var equip=new BattleHudButton(rx,actionY,rw,17,
                Component.literal(equipmentTargetCharacterId.isBlank()?"장착할 캐릭터 선택":"이 캐릭터에게 장착"),GREEN,ignored->equipSelected());
        equip.active=!equipmentTargetCharacterId.isBlank();
        addRenderableWidget(equip);
    }

    private void buildArchive(){
        // Global E-menu archive is deliberately read-only.
        // Summon and Star Essence exchange remain physical NPC/facility actions.
        archiveLogOpen=true;
        currentTotal=0;
        currentPerPage=1;
    }

    private void buildQuests(){
        var snapshot=ClientMetaState.snapshot();
        List<ClientMetaState.RegionQuestRow> quests=questRows();
        int y=contentTop()+4;
        boolean detailed=quests.stream().anyMatch(q->q.objectiveSpecified()&&!q.chestRule().isBlank());
        int questStep=detailed?32:19;
        int questRows=UiPaging.rowsThatFit(y+20,contentBottom(),questStep,3);
        int challengeRows=UiPaging.rowsThatFit(y+20,contentBottom(),19,4);
        int rows=Math.max(1,Math.min(questRows,challengeRows));
        setPaging(Math.max(quests.size(),snapshot.challenges().size()),rows);
        buildPager();
    }


    private static List<ClientMetaState.RegionQuestRow> questRows(){
        return ClientMetaState.snapshot().regionQuests().stream()
                .sorted(Comparator.comparing(ClientMetaState.RegionQuestRow::completed)
                        .thenComparingInt(row->questCategoryPriority(row.region()))
                        .thenComparing(ClientMetaState.RegionQuestRow::id))
                .toList();
    }

    private static int questCategoryPriority(String category){
        if(category==null)return 9;
        if(category.startsWith("메인"))return 0;
        if(category.startsWith("서브")||category.startsWith("지역 목표"))return 1;
        if(category.startsWith("숨은"))return 2;
        if(category.startsWith("지역 의뢰")||category.startsWith("반복"))return 3;
        return 4;
    }

    private void buildCodex(){
        int x=left+16,y=contentTop(),gap=3,count=4,w=(panelWidth-32-gap*(count-1))/count;
        for(String category:List.of("CHARACTERS","ENEMIES","BOSSES","EQUIPMENT")){
            addRenderableWidget(new BattleHudButton(x,y,w,15,Component.literal(codexLabel(category)),category.equals(codexCategory)?BLUE:MUTED,ignored->selectCodex(category)));
            x+=w+gap;
        }
        if("CHARACTERS".equals(codexCategory)){
            buildCodexCharacters(y+18);
            return;
        }
        List<ClientMetaState.CodexRow> rows=ClientMetaState.snapshot().codex().stream().filter(r->r.category().equals(codexCategory)).toList();
        int gridTop=y+21,gridBottom=contentBottom()-22,cols=panelWidth>=600?6:panelWidth>=500?5:4,rowH=27,cardGap=3;
        int visible=UiPaging.rowsThatFit(gridTop,gridBottom,rowH+4,2),per=cols*visible;
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

    private void buildCodexCharacters(int y){
        int gap=4,bw=(panelWidth-32-gap*2)/3,x=left+16;
        addRenderableWidget(new BattleHudButton(x,y,bw,15,Component.literal("보유 · "+ownershipLabel()),BLUE,ignored->cycleOwnership()));x+=bw+gap;
        addRenderableWidget(new BattleHudButton(x,y,bw,15,Component.literal("성급 · "+(starFilter==0?"전체":"★"+starFilter)),GOLD,ignored->cycleStar()));x+=bw+gap;
        addRenderableWidget(new BattleHudButton(x,y,bw,15,Component.literal("역할 · "+roleLabel(roleFilter)),MUTED,ignored->cycleRole()));

        List<ClientMetaState.CharacterRow> rows=filteredCodexCharacters();
        int gridTop=y+18,gridBottom=contentBottom()-22,cols=panelWidth>=600?6:panelWidth>=500?5:4,rowH=31,cardGap=3;
        int visibleRows=UiPaging.rowsThatFit(gridTop,gridBottom,rowH+4,2),per=cols*visibleRows;
        setPaging(rows.size(),per);
        int start=page*per,end=Math.min(rows.size(),start+per),cardW=(panelWidth-32-cardGap*(cols-1))/cols;
        for(int i=start;i<end;i++){
            var row=rows.get(i);
            int local=i-start,xx=left+16+(local%cols)*(cardW+cardGap),yy=gridTop+(local/cols)*(rowH+4);
            String detail=(row.awakened()&&row.owned()?"각성 · ":"")+"★"+row.nativeStar()+" · "+primaryRoleLabel(row.primaryRole());
            addRenderableWidget(new FoozlePortraitButton(
                    xx,yy,cardW,rowH,row.id(),row.name(),detail,!row.owned(),
                    ignored->openCharacter(row.id())));
        }
        buildPager();
    }

    private void buildSystem(){
        int x=left+16,y=contentTop()+4,w=panelWidth-32,gap=4;
        int half=(w-gap)/2;
        boolean music=TurnboundClientSettings.musicEnabled();
        boolean sfx=TurnboundClientSettings.sfxEnabled();
        boolean camera=TurnboundClientSettings.impactCameraEnabled();
        boolean minimap=TurnboundClientSettings.minimapEnabled();

        addRenderableWidget(new BattleHudButton(
                x,y,half,22,Component.literal("음악 · "+(music?"켬":"끔")),music?GREEN:MUTED,
                ignored->{TurnboundClientSettings.toggleMusic();rebuild();}));
        addRenderableWidget(new BattleHudButton(
                x+half+gap,y,half,22,Component.literal("음악 음량 · "+TurnboundClientSettings.musicPercent()+"%"),
                music?BLUE:MUTED,ignored->{TurnboundClientSettings.cycleMusicVolume();rebuild();}));

        y+=28;
        addRenderableWidget(new BattleHudButton(
                x,y,half,22,Component.literal("효과음 · "+(sfx?"켬":"끔")),sfx?GREEN:MUTED,
                ignored->{TurnboundClientSettings.toggleSfx();rebuild();}));
        addRenderableWidget(new BattleHudButton(
                x+half+gap,y,half,22,Component.literal("효과음 음량 · "+TurnboundClientSettings.sfxPercent()+"%"),
                sfx?BLUE:MUTED,ignored->{TurnboundClientSettings.cycleSfxVolume();rebuild();}));

        y+=28;
        addRenderableWidget(new BattleHudButton(
                x,y,half,22,Component.literal("타격 카메라 · "+(camera?"켬":"끔")),camera?GOLD:MUTED,
                ignored->{TurnboundClientSettings.toggleImpactCamera();rebuild();}));
        addRenderableWidget(new BattleHudButton(
                x+half+gap,y,half,22,Component.literal("미니맵 · "+(minimap?"켬":"끔")),minimap?BLUE:MUTED,
                ignored->{TurnboundClientSettings.toggleMinimap();rebuild();}));
        setPaging(0,1);
    }

    private void buildChallenges(){
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

    private void buildPager(){ buildPagerAt(top+panelHeight-22); }

    private void buildPagerAt(int y){
        int pages=UiPaging.pageCount(currentTotal,currentPerPage);
        if(pages<=1)return;
        int center=left+panelWidth/2;
        var prev=new BattleHudButton(center-78,y,52,16,Component.literal("< 이전"),MUTED,ignored->movePage(-1));prev.active=page>0;addRenderableWidget(prev);
        var next=new BattleHudButton(center+26,y,52,16,Component.literal("다음 >"),MUTED,ignored->movePage(1));next.active=page+1<pages;addRenderableWidget(next);
    }

    @Override
    public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY){
        if(tab==Tab.ARCHIVE&&archiveLogOpen&&scrollY!=0){
            int visible=Math.max(1,(contentBottom()-(contentTop()+42))/18);
            int max=Math.max(0,ClientMetaState.snapshot().archiveHistory().size()-visible);
            archiveLogScroll=Math.max(0,Math.min(max,archiveLogScroll+(scrollY>0?-1:1)));
            return true;
        }
        if(!selectedCharacterId.isBlank()&&detailTab==DetailTab.SKILLS&&scrollY!=0){
            skillDescriptionScroll=Math.max(0,skillDescriptionScroll+(scrollY>0?-1:1));
            return true;
        }
        if(currentTotal>currentPerPage&&scrollY!=0){movePage(scrollY>0?-1:1);return true;}
        return super.mouseScrolled(mouseX,mouseY,scrollX,scrollY);
    }

    @Override
    public void tick(){
        super.tick();
        if(tab==Tab.COOP&&seenPartyRevision!=ClientMultiplayerPartyState.revision()){
            seenPartyRevision=ClientMultiplayerPartyState.revision();
            rebuild();
        }
    }

    private void movePage(int delta){page=UiPaging.clampPage(page+delta,currentTotal,currentPerPage);rebuild();}
    private void openArchiveLog(){archiveLogOpen=true;archiveLogScroll=0;rebuild();}
    private void closeArchiveLog(){archiveLogOpen=false;archiveLogScroll=0;rebuild();}
    private void rebuild(){clearWidgets();init();}
    private void switchTab(Tab value){if(value==tab)return;tab=value;page=0;selectedCharacterId="";selectedEquipmentId="";archiveLogOpen=false;rebuild();}
    private void openCharacterFromHome(String id){tab=Tab.CHARACTERS;selectedCharacterId=id;detailTab=DetailTab.OVERVIEW;selectedSkillIndex=0;skillDescriptionScroll=0;page=0;rebuild();}
    private void openMap(){Minecraft.getInstance().gui.setScreen(new DrehmalWorldMapScreen());}
    private void toggleParty(String id){if(draftParty.contains(id)){if(draftParty.size()>1)draftParty.remove(id);}else if(draftParty.size()<4)draftParty.add(id);rebuild();}
    private void saveParty(){send("PARTY|"+String.join(",",draftParty));}
    private void openCharacter(String id){tab=Tab.CHARACTERS;selectedCharacterId=id;detailTab=DetailTab.OVERVIEW;selectedSkillIndex=0;skillDescriptionScroll=0;page=0;rebuild();}
    private void closeCharacter(){tab=Tab.CODEX;codexCategory="CHARACTERS";selectedCharacterId="";skillDescriptionScroll=0;page=0;rebuild();}
    private void switchDetail(DetailTab d){detailTab=d;skillDescriptionScroll=0;if(d==DetailTab.SKILLS)selectedSkillIndex=0;rebuild();}
    private void cycleOwnership(){ownershipFilter=OwnershipFilter.values()[(ownershipFilter.ordinal()+1)%OwnershipFilter.values().length];page=0;rebuild();}
    private void cycleStar(){starFilter=switch(starFilter){case 0->1;case 1->2;case 2->3;case 3->4;case 4->5;default->0;};page=0;rebuild();}
    private void cycleLevel(){minimumLevel=minimumLevel==0?10:minimumLevel>=60?0:minimumLevel+10;page=0;rebuild();}
    private void cycleRole(){roleFilter=RoleFilter.values()[(roleFilter.ordinal()+1)%RoleFilter.values().length];page=0;rebuild();}
    private void cycleEquipSlot(){List<String>v=List.of("ALL","WEAPON","ARMOR","ACCESSORY","SIGNATURE");equipSlotFilter=v.get((v.indexOf(equipSlotFilter)+1)%v.size());page=0;rebuild();}
    private void cycleEquipSort(){equipSort=EquipSort.values()[(equipSort.ordinal()+1)%EquipSort.values().length];page=0;rebuild();}
    private void selectEquipment(String id){selectedEquipmentId=id;if(equipmentTargetCharacterId.isBlank())equipmentTargetCharacterId=ClientMetaState.snapshot().activeParty().stream().findFirst().orElse("");rebuild();}
    private void selectEquipmentTarget(String id){equipmentTargetCharacterId=id;rebuild();}

    private List<ClientMetaState.CharacterRow> ownedEquipmentTargets(){
        return ClientMetaState.snapshot().characters().stream()
                .filter(ClientMetaState.CharacterRow::owned)
                .sorted(Comparator.comparingInt(ClientMetaState.CharacterRow::nativeStar).reversed()
                        .thenComparing(ClientMetaState.CharacterRow::name))
                .toList();
    }

    private void ensureEquipmentTarget(){
        var owned=ownedEquipmentTargets();
        if(owned.isEmpty()){equipmentTargetCharacterId="";return;}
        boolean valid=owned.stream().anyMatch(row->row.id().equals(equipmentTargetCharacterId));
        if(valid)return;
        equipmentTargetCharacterId=ClientMetaState.snapshot().activeParty().stream()
                .filter(id->owned.stream().anyMatch(row->row.id().equals(id)))
                .findFirst().orElse(owned.getFirst().id());
    }

    private void cycleEquipmentTarget(int delta){
        var owned=ownedEquipmentTargets();
        if(owned.isEmpty()){equipmentTargetCharacterId="";rebuild();return;}
        int index=0;
        for(int i=0;i<owned.size();i++)if(owned.get(i).id().equals(equipmentTargetCharacterId)){index=i;break;}
        index=Math.floorMod(index+delta,owned.size());
        equipmentTargetCharacterId=owned.get(index).id();
        rebuild();
    }

    private String equipmentTargetName(){
        return equipmentTargetCharacterId.isBlank()?"선택 없음":characterName(equipmentTargetCharacterId);
    }

    private int equipmentListWidth(){return Math.min(310,Math.max(210,panelWidth/2-12));}

    private void equipSelected(){if(!selectedEquipmentId.isBlank()&&!equipmentTargetCharacterId.isBlank())send("EQUIP|"+equipmentTargetCharacterId+"|"+selectedEquipmentId);}
    private void sellSelected(){var e=equipment(selectedEquipmentId);if(e!=null&&e.sellable())send("SELL|"+e.instanceId());}
    private void selectCodex(String c){codexCategory=c;page=0;rebuild();}
    private void selectEndgame(String id){selectedEndgameId=id;rebuild();}
    private static void send(String command){ClientPacketDistributor.sendToServer(new MetaCommandPayload(command));}
    private static void partyCommand(String command){ClientPacketDistributor.sendToServer(new PartyCommandPayload(command));}

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
        int pageTitleX=left+116;
        int pageTitleW=Math.max(48,panelWidth-132);
        graphics.text(font,Component.literal(UiTextLayout.fit(title(tab),pageTitleW)),pageTitleX,top+56,TEXT,true);
        switch(tab){
            case HOME->{}
            case PARTY->drawParty(graphics);
            case COOP->drawCoop(graphics);
            case CHARACTERS->drawCharacters(graphics);
            case EQUIPMENT->drawEquipment(graphics);
            case ARCHIVE->drawArchive(graphics);
            case QUESTS->drawQuests(graphics);
            case CODEX->drawCodex(graphics);
            case CHALLENGES->drawChallenges(graphics);
            case SYSTEM->drawSystem(graphics);
        }
        int pages=UiPaging.pageCount(currentTotal,currentPerPage);
        if(pages>1){
            String p=(page+1)+" / "+pages+" · 휠 스크롤";
            int pageY=tab==Tab.PARTY?top+panelHeight-61:top+panelHeight-27;
            graphics.text(font,Component.literal(p),left+panelWidth/2-font.width(p)/2,pageY,MUTED,false);
        }
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
    }

    private void drawHome(GuiGraphicsExtractor g){
        int px=homePanelX(),py=homePanelY(),pw=homePanelW(),ph=homePanelH();
        int leftW=Math.max(185,pw*57/100);

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
        // The roster is the information surface; preset/XP policy text does not consume a row.
    }


    private void drawCoop(GuiGraphicsExtractor g){
        var party=ClientMultiplayerPartyState.snapshot();
        int x=left+16,w=panelWidth-32;
        int hintY=contentTop()+3;
        TurnboundUiSkin.inset(g,x,hintY,w,17);
        String leaderName=party.members().stream().filter(ClientMultiplayerPartyState.Member::leader)
                .map(ClientMultiplayerPartyState.Member::name).findFirst().orElse("나");
        String header="협동 파티 "+party.size()+"/"+party.maxPlayers()+" · 파티장 "+leaderName+" · 가까운 파티원만 필드 전투에 합류";
        g.text(font,Component.literal(UiTextLayout.fit(header,w-(party.size()>1?72:12))),x+6,hintY+5,SECONDARY,false);

        int cursor=contentTop()+25;
        if(party.pendingInvite()!=null){
            TurnboundUiSkin.inset(g,x,cursor,w,31);
            String invite=party.pendingInvite().name()+"님의 협동 파티 초대";
            g.text(font,Component.literal(UiTextLayout.fit(invite,Math.max(60,w-132))),x+8,cursor+11,GOLD,true);
            cursor+=38;
        }

        g.text(font,Component.literal("파티원"),x,cursor+2,SECONDARY,true);
        int memberRowsY=cursor+14;
        if(party.members().isEmpty()){
            g.text(font,Component.literal("파티 상태를 불러오는 중..."),x+6,memberRowsY+7,MUTED,false);
        }else{
            for(int i=0;i<party.members().size();i++){
                var member=party.members().get(i);
                int row=memberRowsY+i*28;
                TurnboundUiSkin.inset(g,x,row,w,24);
                int accent=member.self()?BLUE:member.leader()?GOLD:MUTED;
                g.fill(x,row,x+2,row+24,accent);
                String role=(member.self()?"나 · ":"")+(member.leader()?"파티장 · ":"");
                String state=!member.online()?"오프라인":member.inBattle()?"전투 중":!member.sameLevel()?"다른 차원":"필드";
                String location=member.sameLevel()?" · "+Math.round(member.x())+", "+Math.round(member.y())+", "+Math.round(member.z()):"";
                int reserve=party.localLeader()&&!member.self()?64:8;
                g.text(font,Component.literal(UiTextLayout.fit(role+member.name(),Math.max(50,w-reserve-170))),x+8,row+5,TEXT,true);
                String status=state+location;
                int sx=x+w-reserve-font.width(UiTextLayout.fit(status,150));
                g.text(font,Component.literal(UiTextLayout.fit(status,150)),Math.max(x+90,sx),row+5,
                        member.inBattle()?GOLD:member.online()?SECONDARY:MUTED,false);
            }
        }

        int candidateHeaderY=memberRowsY+Math.max(1,party.members().size())*28+8;
        g.text(font,Component.literal("같은 차원의 온라인 플레이어"),x,candidateHeaderY+2,SECONDARY,true);
        int candidateRowsY=candidateHeaderY+14;
        int start=page*currentPerPage;
        int end=Math.min(party.candidates().size(),start+currentPerPage);
        if(start>=end){
            g.text(font,Component.literal(party.localLeader()&&party.size()<party.maxPlayers()
                    ?"현재 초대할 수 있는 플레이어가 없습니다."
                    :"파티장만 빈 자리가 있을 때 초대할 수 있습니다."),x+6,candidateRowsY+7,MUTED,false);
        }else{
            for(int i=start;i<end;i++){
                var candidate=party.candidates().get(i);
                int row=candidateRowsY+(i-start)*26;
                TurnboundUiSkin.inset(g,x,row,w,22);
                String location=Math.round(candidate.x())+", "+Math.round(candidate.y())+", "+Math.round(candidate.z());
                g.text(font,Component.literal(UiTextLayout.fit(candidate.name(),Math.max(50,w-190))),x+8,row+5,TEXT,true);
                String state=candidate.inBattle()?"전투 중":"좌표 "+location;
                g.text(font,Component.literal(UiTextLayout.fit(state,120)),x+w-62-font.width(UiTextLayout.fit(state,120)),row+5,
                        candidate.inBattle()?GOLD:SECONDARY,false);
            }
        }

        if(!party.feedback().isBlank()){
            String feedback=UiTextLayout.fit(party.feedback(),w);
            g.text(font,Component.literal(feedback),x,contentBottom()-12,BLUE,false);
        }
    }

    private int skillRailX(){return left+16;}
    private int skillRailY(){return contentTop()+58;}
    private int skillRailW(){return Math.min(compactLayout?132:156,Math.max(112,panelWidth/4));}
    private int skillDetailX(){return skillRailX()+skillRailW()+10;}
    private int skillDetailW(){return Math.max(120,left+panelWidth-16-skillDetailX());}

    private void drawCharacters(GuiGraphicsExtractor g){
        if(selectedCharacterId.isBlank()) return;
        var r=character(selectedCharacterId);if(r==null)return;
        boolean skillView=detailTab==DetailTab.SKILLS;
        int x,y,w;
        if(skillView){
            int headerX=left+16,headerW=Math.max(80,panelWidth-32);
            y=contentTop()+27;
            String header=r.name()+" · "+(r.owned()?(r.awakened()?"각성 · ":"")+"★"+r.nativeStar()+" "+levelLabel(r):"미보유 · ★"+r.nativeStar())
                    +" · "+primaryRoleLabel(r.primaryRole());
            g.text(font,Component.literal(UiTextLayout.fit(header,headerW)),headerX,y,r.owned()?TEXT:MUTED,true);
            g.text(font,Component.literal("기술 목록"),skillRailX(),contentTop()+42,SECONDARY,true);
            x=skillDetailX();w=skillDetailW();
        }else{
            int portraitX=left+18,portraitY=contentTop()+27;
            int portraitSize=Math.min(116,Math.max(76,Math.min(panelWidth/5,contentBottom()-portraitY-6)));
            TurnboundUiSkin.orbBase(g,portraitX,portraitY,portraitSize);
            int portraitInset=Math.max(8,portraitSize/8);
            TurnboundPortraitRenderer.extractBust(
                    g,r.id(),
                    portraitX+portraitInset,portraitY+portraitInset,
                    portraitX+portraitSize-portraitInset,portraitY+portraitSize-portraitInset,
                    !r.owned());
            TurnboundUiSkin.orbOverlay(g,portraitX,portraitY,portraitSize,r.owned(),false,false);
            x=portraitX+portraitSize+14;y=contentTop()+30;w=Math.max(80,left+panelWidth-18-x);
            g.text(font,Component.literal(UiTextLayout.fit(r.name()+" · "+(r.owned()?(r.awakened()?"각성 · ":"")+"★"+r.nativeStar()+" "+levelLabel(r):"미보유 · ★"+r.nativeStar()),w)),x,y,r.owned()?TEXT:MUTED,true);
            g.text(font,Component.literal(UiTextLayout.fit(r.role(),w)),x,y+14,SECONDARY,false);
        }
        switch(detailTab){
            case OVERVIEW->{
                g.text(font,Component.literal("HP "+r.hp()+"   ATK "+r.attack()+"   DEF "+r.defense()+"   SPD "+r.speed()),x,y+34,GREEN,false);
                g.text(font,Component.literal("전투력 "+r.cp()+" · "+primaryRoleLabel(r.primaryRole())+" · "+r.difficulty()),x,y+51,TEXT,false);
            }
            case SKILLS->{
                var d=CanonicalData.definition(r.id(),Math.max(1,r.effectiveLevel()),Math.max(1,r.star()),r.awakened());
                var skills=d.skills();
                var passives=CharacterPassiveCatalog.forOwner(r.id());
                int slotCount=skills.size()+(passives.isEmpty()?0:1);
                if(slotCount<=0)break;
                int index=Math.max(0,Math.min(selectedSkillIndex,slotCount-1));
                boolean passiveSelected=index>=skills.size();
                int detailY=skillRailY()-4;
                int boxH=Math.max(92,contentBottom()-detailY-3);
                TurnboundUiSkin.inset(g,x,detailY,w,boxH);

                List<DetailLine> details=new ArrayList<>();
                String title;
                String metaLine;
                int titleColor;
                if(passiveSelected){
                    title="패시브";
                    metaLine="고유 지속 효과";
                    titleColor=PURPLE;
                    if(passives.isEmpty()){
                        details.add(new DetailLine("고유 패시브 없음",MUTED));
                    }else{
                        for(var passive:passives){
                            details.add(new DetailLine(passive.name(),GOLD));
                            for(String line:UiTextLayout.wrap(passive.description(),Math.max(100,w-20),120)){
                                details.add(new DetailLine(line,SECONDARY));
                            }
                        }
                    }
                }else{
                    var skill=skills.get(index);
                    title=skill.name();
                    metaLine=(skill.isBasic()?"기본 공격":"액티브")+" · "
                            +(skill.cooldown()<=0?"쿨타임 없음":"쿨타임 "+skill.cooldown()+"턴");
                    titleColor=skill.isBasic()?GREEN:GOLD;
                    for(String line:UiTextLayout.wrap(skill.description(),Math.max(100,w-20),120)){
                        details.add(new DetailLine(line,TEXT));
                    }
                }

                int bodyY=detailY+45;
                int visible=Math.max(1,(detailY+boxH-bodyY-8)/12);
                int maxScroll=Math.max(0,details.size()-visible);
                skillDescriptionScroll=Math.max(0,Math.min(skillDescriptionScroll,maxScroll));
                String scroll=maxScroll>0?" · 휠 "+(skillDescriptionScroll+1)+"/"+(maxScroll+1):"";
                g.text(font,Component.literal(UiTextLayout.fit(title,w-20)),x+10,detailY+9,titleColor,true);
                g.text(font,Component.literal(UiTextLayout.fit(metaLine+scroll,w-20)),x+10,detailY+24,SECONDARY,false);
                g.text(font,Component.literal("효과"),x+10,detailY+36,MUTED,true);
                int end=Math.min(details.size(),skillDescriptionScroll+visible),cursor=bodyY;
                for(int i=skillDescriptionScroll;i<end;i++){
                    DetailLine line=details.get(i);
                    g.text(font,Component.literal(UiTextLayout.fit(line.text(),w-20)),x+10,cursor,line.color(),false);
                    cursor+=12;
                }
            }
            case EQUIPMENT->{
                int yy=y+34;
                for(String slot:List.of("WEAPON","ARMOR","ACCESSORY","SIGNATURE")){
                    var item=ClientMetaState.snapshot().equipment().stream().filter(e->e.equippedCharacterId().equals(r.id())&&e.slot().equals(slot)).findFirst().orElse(null);
                    String text=slotLabel(slot)+" · "+(item==null?"비어 있음":item.name()+" +"+item.enhancement());
                    g.text(font,Component.literal(UiTextLayout.fit(text,w)),x,yy,item==null?MUTED:tierColor(item.tier()),false);
                    yy+=18;
                }
            }
            case GROWTH->{
                var trial=ClientSignatureTrialState.forCharacter(r.id());
                String status=r.awakened()?"각성 완료":trial!=null&&trial.awakeningReady()?"각성 가능":"선행 조건 진행 중";
                TurnboundUiSkin.inset(g,x,y+32,w,76);
                g.text(font,Component.literal("레벨 · "+r.level()+" / "+GrowthRulesV1.maxLevel()
                        +(r.bonusLevel()>0?"  +"+r.bonusLevel()+" · 실질 "+r.effectiveLevel():"")),x+8,y+40,TEXT,true);
                g.text(font,Component.literal("전투 경험치로 레벨 성장"),x+8,y+56,SECONDARY,false);
                g.text(font,Component.literal("각성 · "+status),x+8,y+74,r.awakened()?GREEN:GOLD,true);
                g.text(font,Component.literal(UiTextLayout.fit("Lv60 + 개인 퀘스트 + "+GrowthRulesV1.awakeningGoldCost()+" Gold",w-16)),x+8,y+90,SECONDARY,false);
                if(trial!=null&&!trial.title().isBlank()&&!trial.objective().isBlank())
                    g.text(font,Component.literal(UiTextLayout.fit("전용 장비 시련 · "+trial.title()+" · "+trial.objective(),w)),x,y+116,SECONDARY,false);
            }
        }
    }

    private void drawEquipment(GuiGraphicsExtractor g){
        var selected=equipment(selectedEquipmentId);if(selected==null)return;
        int listW=equipmentListWidth(),x=left+26+listW,y=contentTop()+23,w=panelWidth-listW-58;
        g.text(font,Component.literal(UiTextLayout.fit(selected.tier()+" · "+selected.name()+" +"+selected.enhancement(),w)),x,y,tierColor(selected.tier()),true);
        String current=statTypeLabel(selected.mainType())+" "+stat(selected.mainValue())+" · "+statTypeLabel(selected.subType())+" "+stat(selected.subValue());
        g.text(font,Component.literal(UiTextLayout.fit("현재 · "+current,w)),x,y+16,TEXT,false);
        if(selected.enhancement()<GrowthRulesV1.maxEnhancement()){
            double nextMain=equipmentMainAt(selected.itemId(),selected.enhancement()+1);
            String next="+"+(selected.enhancement()+1)+" · "+statTypeLabel(selected.mainType())+" "+stat(nextMain)
                    +" · "+statTypeLabel(selected.subType())+" "+stat(selected.subValue());
            g.text(font,Component.literal(UiTextLayout.fit(next,w)),x,y+32,GREEN,false);
        }else{
            g.text(font,Component.literal("강화 최대치"),x,y+32,GREEN,false);
        }
        g.text(font,Component.literal(UiTextLayout.fit("+10 최대 · "+statTypeLabel(selected.mainType())+" "+stat(selected.mainAt20())
                +" · "+statTypeLabel(selected.subType())+" "+stat(selected.subAt20()),w)),x,y+48,GOLD,false);
        String effect=equipmentEffect(selected);
        if(!effect.isBlank())g.text(font,Component.literal(UiTextLayout.fit("특성 · "+effect,w)),x,y+64,PURPLE,false);
        String owner=selected.equippedCharacterId().isBlank()?"미장착":characterName(selected.equippedCharacterId());
        g.text(font,Component.literal(UiTextLayout.fit("현재 장착 · "+owner,w)),x,y+80,SECONDARY,false);
        g.text(font,Component.literal(UiTextLayout.fit("장착 대상 · "+equipmentTargetName(),w)),x,y+94,GREEN,false);
    }

    private void drawArchive(GuiGraphicsExtractor g){
        var s=ClientMetaState.snapshot();
        if(archiveLogOpen){
            int topY=contentTop()+34;
            g.text(font,Component.literal("소환 기록 · 최신순"),left+16,topY-10,TEXT,true);
            int visible=Math.max(1,(contentBottom()-(topY+8))/18);
            int maxScroll=Math.max(0,s.archiveHistory().size()-visible);
            archiveLogScroll=Math.max(0,Math.min(archiveLogScroll,maxScroll));
            int yy=topY+8;
            for(int row=0;row<visible;row++){
                int index=s.archiveHistory().size()-1-(archiveLogScroll+row);
                if(index<0)break;
                var r=s.archiveHistory().get(index);
                String text="★"+r.nativeStars()+" · "+r.name()+(r.newlyOwned()?" · 신규"
                        :" · 별의 정수 +"+r.essenceGranted()
                        +(r.bonusLevelGranted()>0?" · +레벨 +1 (+"+r.bonusLevelAfter()+")":" · +레벨 MAX"));
                g.text(font,Component.literal(UiTextLayout.fit(text,panelWidth-32)),left+16,yy,r.newlyOwned()?GREEN:SECONDARY,false);
                yy+=18;
            }
            if(maxScroll>0){
                String hint="휠 스크롤 · "+(archiveLogScroll+1)+" / "+(maxScroll+1);
                g.text(font,Component.literal(hint),left+panelWidth-16-font.width(hint),contentBottom()-10,MUTED,false);
            }
            return;
        }

        String pity="★5 천장 "+s.fiveStarPity()+" / "+GachaCatalog.HARD_PITY
                +" · 확률 상승 "+GachaCatalog.SOFT_PITY_START+"회부터";
        String upperRates=String.format(Locale.ROOT,"★5 %.0f%%   ★4 %.0f%%   ★3 %.0f%%",
                GachaCatalog.BASE_FIVE_STAR_RATE*100.0,GachaCatalog.FOUR_STAR_RATE*100.0,GachaCatalog.THREE_STAR_RATE*100.0);
        String lowerRates=String.format(Locale.ROOT,"★2 %.0f%%   ★1 %.0f%%",
                GachaCatalog.TWO_STAR_RATE*100.0,GachaCatalog.ONE_STAR_RATE*100.0);

        if(compactLayout){
            int x=left+16,w=Math.max(1,panelWidth-32),y=contentTop()+57;
            g.text(font,Component.literal(UiTextLayout.fit(pity,w)),x,y,GOLD,false);
            g.text(font,Component.literal("소환 확률"),x,y+13,TEXT,true);
            g.text(font,Component.literal(UiTextLayout.fit(upperRates,w)),x,y+26,GOLD,false);
            g.text(font,Component.literal(UiTextLayout.fit(lowerRates,w)),x,y+39,SECONDARY,false);
            g.text(font,Component.literal(UiTextLayout.fit("10회 소환 · 최소 ★4 이상 1명 보장",w)),x,y+52,GREEN,false);
            return;
        }

        int y=contentTop()+34;
        g.text(font,Component.literal(pity),left+16,y-7,GOLD,false);

        int boxX=left+16,boxY=y+12,boxW=Math.min(310,panelWidth-32),boxH=82;
        TurnboundUiSkin.inset(g,boxX,boxY,boxW,boxH);
        g.text(font,Component.literal("소환 확률"),boxX+10,boxY+8,TEXT,true);
        g.text(font,Component.literal(upperRates),boxX+10,boxY+27,GOLD,false);
        g.text(font,Component.literal(lowerRates),boxX+10,boxY+45,SECONDARY,false);
        g.text(font,Component.literal("10회 소환 · 최소 ★4 이상 1명 보장"),boxX+10,boxY+63,GREEN,false);

        int infoX=boxX+boxW+12;
        int infoW=left+panelWidth-16-infoX;
        if(infoW>150){
            TurnboundUiSkin.inset(g,infoX,boxY,infoW,boxH);
            g.text(font,Component.literal("소환 규칙"),infoX+10,boxY+8,TEXT,true);
            g.text(font,Component.literal("1회 300 · 10회 3000 크리스탈"),infoX+10,boxY+27,SECONDARY,false);
            g.text(font,Component.literal("중복 → 별의 정수 + +레벨(최대 +10)"),infoX+10,boxY+45,SECONDARY,false);
            g.text(font,Component.literal("기간 한정 배너 없음"),infoX+10,boxY+63,MUTED,false);
        }
    }

    private void drawQuests(GuiGraphicsExtractor g){
        var snapshot=ClientMetaState.snapshot();
        List<ClientMetaState.RegionQuestRow> quests=questRows();
        long activeCount=quests.stream().filter(q->!q.completed()).count();
        long completedCount=quests.size()-activeCount;
        int y=contentTop()+4,paneGap=12,paneW=(panelWidth-44-paneGap)/2,leftX=left+16,rightX=leftX+paneW+paneGap;
        String questHeader="퀘스트 · 진행 "+activeCount+" / 완료 "+completedCount;
        g.text(font,Component.literal(UiTextLayout.fit(questHeader,paneW)),leftX,y,TEXT,true);
        g.text(font,Component.literal("업적"),rightX,y,TEXT,true);
        boolean detailed=quests.stream().anyMatch(q->q.objectiveSpecified()&&!q.chestRule().isBlank());
        int questStep=detailed?32:19;
        int start=page*currentPerPage,yy=y+20;
        for(int i=start;i<Math.min(quests.size(),start+currentPerPage);i++){
            var q=quests.get(i);
            String text=(q.completed()?"✓ ":"○ ")+q.region()+" · "+q.id();
            g.text(font,Component.literal(UiTextLayout.fit(text,paneW)),leftX,yy,q.completed()?GREEN:TEXT,false);
            if(detailed&&q.objectiveSpecified()&&!q.chestRule().isBlank()){
                String detail=UiTextLayout.fit(q.chestRule(),Math.max(40,paneW-10));
                g.text(font,Component.literal(detail),leftX+10,yy+13,q.completed()?MUTED:SECONDARY,false);
            }
            yy+=questStep;
        }
        yy=y+20;
        for(int i=start;i<Math.min(snapshot.challenges().size(),start+currentPerPage);i++){
            var c=snapshot.challenges().get(i);
            String text=(c.completed()?"✓ ":"○ ")+c.ordinal()+". "+c.label();
            g.text(font,Component.literal(UiTextLayout.fit(text,paneW)),rightX,yy,c.completed()?GREEN:c.autoEvaluable()?TEXT:GOLD,false);
            yy+=19;
        }
    }

    private void drawCodex(GuiGraphicsExtractor g){
        if("CHARACTERS".equals(codexCategory))return;
        List<ClientMetaState.CodexRow> rows=ClientMetaState.snapshot().codex().stream().filter(r->r.category().equals(codexCategory)).toList();
        int start=page*currentPerPage,end=Math.min(rows.size(),start+currentPerPage),gridTop=contentTop()+21,cols=panelWidth>=600?6:panelWidth>=500?5:4,rowH=27,gap=3,cardW=(panelWidth-32-gap*(cols-1))/cols;
        for(int i=start;i<end;i++){
            var r=rows.get(i);
            int local=i-start,x=left+16+(local%cols)*(cardW+gap),y=gridTop+(local/cols)*(rowH+4);
            String detail=r.detailUnlocked()?r.summary():r.discovered()?"상세 정보 잠김":"미발견";
            g.text(font,Component.literal(UiTextLayout.fit(detail,cardW-16)),x+8,y+16,r.detailUnlocked()?SECONDARY:MUTED,false);
        }
    }

    private void drawSystem(GuiGraphicsExtractor g){
        int x=left+16,y=contentTop()+96,w=panelWidth-32;
        TurnboundUiSkin.inset(g,x,y,w,58);
        g.text(font,Component.literal("TURNBOUND 전용 설정"),x+8,y+8,TEXT,true);
        g.text(font,Component.literal(UiTextLayout.fit(
                "Minecraft의 그래픽·키·전체 음량은 ESC 설정을 그대로 사용합니다.",w-16)),x+8,y+23,SECONDARY,false);
        g.text(font,Component.literal(UiTextLayout.fit(
                "여기서는 게임 음악/효과음, 전투 타격 카메라, 탐색 미니맵만 조정합니다.",w-16)),x+8,y+37,MUTED,false);
    }

    private void drawChallenges(GuiGraphicsExtractor g){
        var selected=endgame(selectedEndgameId);if(selected==null)return;
        int y=contentBottom()-24;
        String s=(selected.cleared()?"클리어 · ":"")+selected.label()+" · Lv."+selected.level();
        g.text(font,Component.literal(UiTextLayout.fit(s,panelWidth-200)),left+16,y,selected.cleared()?GREEN:TEXT,false);
    }

    private List<ClientMetaState.CharacterRow> filteredCodexCharacters(){
        Comparator<ClientMetaState.CharacterRow> c=Comparator.comparingInt(ClientMetaState.CharacterRow::nativeStar)
                .reversed().thenComparing(ClientMetaState.CharacterRow::id);
        return ClientMetaState.snapshot().characters().stream()
                .filter(r->ownershipFilter==OwnershipFilter.ALL||(ownershipFilter==OwnershipFilter.OWNED)==r.owned())
                .filter(r->starFilter==0||r.nativeStar()==starFilter)
                .filter(r->roleFilter==RoleFilter.ALL||r.primaryRole().equals(roleFilter.name()))
                .sorted(c).toList();
    }

    private List<ClientMetaState.CharacterRow> filteredCharacters(){
        Comparator<ClientMetaState.CharacterRow> c=Comparator.comparingInt(ClientMetaState.CharacterRow::nativeStar).reversed().thenComparing(ClientMetaState.CharacterRow::id);
        return ClientMetaState.snapshot().characters().stream()
                .filter(r->ownershipFilter==OwnershipFilter.ALL||(ownershipFilter==OwnershipFilter.OWNED)==r.owned())
                .filter(r->starFilter==0||r.nativeStar()==starFilter)
                .filter(r->minimumLevel==0||(r.owned()&&r.effectiveLevel()>=minimumLevel))
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
    private static String label(Tab t){return switch(t){case HOME->"빠른 메뉴";case PARTY->"편성";case COOP->"협동";case CHARACTERS->"캐릭터";case EQUIPMENT->"장비";case ARCHIVE->"기록";case QUESTS->"퀘스트";case CODEX->"도감";case CHALLENGES->"고난도";case SYSTEM->"설정";};}
    private static String title(Tab t){return switch(t){case HOME->"빠른 메뉴";case PARTY->"전투 파티 편성";case COOP->"협동 파티";case CHARACTERS->"캐릭터";case EQUIPMENT->"장비";case ARCHIVE->"소환 기록";case QUESTS->"퀘스트";case CODEX->"도감";case CHALLENGES->"고난도 콘텐츠";case SYSTEM->"설정";};}
    private static String detailLabel(DetailTab d){return switch(d){case OVERVIEW->"개요";case SKILLS->"스킬";case EQUIPMENT->"장비";case GROWTH->"성장";};}
    private static String levelLabel(ClientMetaState.CharacterRow row){
        if(row==null)return"Lv.0";
        return "Lv."+row.level()+(row.bonusLevel()>0?" +"+row.bonusLevel():"");
    }

    private static String primaryRoleLabel(String r){return switch(r){case"DPS"->"공격";case"SUPPORT"->"지원";case"TANK"->"수호";case"SUMMON"->"소환";default->r;};}
    private static String slotLabel(String s){return switch(s){case"WEAPON"->"무기";case"ARMOR"->"방어구";case"ACCESSORY"->"장신구";case"SIGNATURE"->"전용 장비";case"ALL"->"전체";default->s;};}
    private static String codexLabel(String c){return switch(c){case"CHARACTERS"->"캐릭터";case"ENEMIES"->"적";case"BOSSES"->"보스";case"EQUIPMENT"->"장비";default->"도감";};}
    private static int tierRank(String t){return switch(t){case"SIGNATURE"->5;case"T4"->4;case"T3"->3;case"T2"->2;case"T1"->1;default->0;};}
    private static int tierColor(String t){return switch(t){case"SIGNATURE"->0xFFC794FF;case"T4"->0xFFFFC857;case"T3"->0xFFB68CFF;case"T2"->0xFF6DC6FF;default->0xFFAEB7C6;};}
    private static String stat(double v){return Math.abs(v)<=1.0?String.format(Locale.ROOT,"%.1f%%",v*100):String.format(Locale.ROOT,"%.1f",v);}
    private static String statTypeLabel(String t){return switch(t){case"HP_FLAT"->"HP";case"HP_PERCENT","HP_PCT"->"HP%";case"ATK_FLAT"->"ATK";case"ATK_PERCENT","ATK_PCT"->"ATK%";case"DEF_FLAT"->"DEF";case"DEF_PERCENT","DEF_PCT"->"DEF%";case"SPD_FLAT"->"SPD";case"SPD_PERCENT","SPD_PCT"->"SPD%";default->t;};}

    private static double equipmentMainAt(String itemId,int level){
        try{var s=V04Catalogs.equipment(itemId);return EquipmentInventory.scaledMain(s.main().type(),s.main().value(),level);}
        catch(RuntimeException ignored){var s=V04Catalogs.signature(itemId);return EquipmentInventory.scaledMain(s.main().type(),s.main().value(),level);}
    }
    private static String equipmentEffect(ClientMetaState.EquipmentRow row){
        try{return ruleLabel(V04Catalogs.equipment(row.itemId()).fixedEffect());}
        catch(RuntimeException ignored){
            var s=V04Catalogs.signature(row.itemId());
            String base=ruleLabel(s.baseRule());
            if(row.enhancement()>=10)return base+" / +10 "+ruleLabel(s.milestone10());
            if(row.enhancement()>=5)return base+" / +5 "+ruleLabel(s.milestone5());
            return base+" / 다음 +5 "+ruleLabel(s.milestone5());
        }
    }
    private static String ruleLabel(String rule){
        if(rule==null||rule.isBlank())return"";
        if(rule.startsWith("START_GAUGE_"))return"전투 시작 Gauge +"+rule.substring("START_GAUGE_".length());
        if(rule.startsWith("DIRECT_HIT_GAUGE_"))return"직접 피격 시 Gauge +"+rule.substring("DIRECT_HIT_GAUGE_".length());
        if(rule.startsWith("ALLY_GAUGE_GRANT_PLUS_"))return"아군 Gauge 부여량 +"+rule.substring("ALLY_GAUGE_GRANT_PLUS_".length());
        if(rule.startsWith("SINGLE_DIRECT_DAMAGE_"))return"단일 직접 피해 +"+rule.substring("SINGLE_DIRECT_DAMAGE_".length())+"%";
        if(rule.startsWith("EXECUTE_DIRECT_DAMAGE_"))return"HP 40% 이하 대상 직접 피해 +"+rule.substring("EXECUTE_DIRECT_DAMAGE_".length())+"%";
        if(rule.startsWith("HEAL_DONE_"))return"주는 회복량 +"+rule.substring("HEAL_DONE_".length())+"%";
        if(rule.startsWith("HEAL_RECEIVED_"))return"받는 회복량 +"+rule.substring("HEAL_RECEIVED_".length())+"%";
        if(rule.startsWith("BARRIER_RECEIVED_"))return"받는 Barrier +"+rule.substring("BARRIER_RECEIVED_".length())+"%";
        if(rule.startsWith("REVIVE_HP_PLUS_"))return"부활 HP +"+rule.substring("REVIVE_HP_PLUS_".length())+"%p";
        if(rule.startsWith("REACTION_DAMAGE_"))return"반응 공격 피해 +"+rule.substring("REACTION_DAMAGE_".length())+"%";
        if(rule.startsWith("HIGH_HP_DR_"))return"HP 80% 이상 피해 감소 "+rule.substring("HIGH_HP_DR_".length())+"%";
        return switch(rule){
            case"SIG_P01_FOCUS3_ACTIVE_GAUGE_100"->"집중 3 액티브 사용 시 Gauge +100";
            case"SIG_P01_FOCUS_KILL_CARRY_2"->"집중 3 대상 처치 후 다음 대상에 집중 2 계승";
            case"SIG_P01_BREAKER_FOLLOWUP_PLUS_25"->"집중 3 파쇄 추가타 위력 +25%p";
            case"SIG_P02_BASIC_SELF_GAUGE_60"->"가속 사용 시 자신 Gauge +60";
            case"SIG_P02_TIME_LEAP_SPEED_20"->"시간 도약 대상 SPD +20%";
            case"SIG_P02_TIME_LEAP_ECHO_120"->"시간 도약 시 다른 아군 Gauge +120";
            case"SIG_P03_REDIRECT_DR_20"->"대신 받는 피해 20% 감소";
            case"SIG_P03_COUNTER_PLUS_20"->"보호 전환 반격 위력 +20%p";
            case"SIG_P03_REDIRECT_TARGET_GAUGE_100"->"보호받은 아군 피격 시 Gauge +100";
            case"SIG_P04_OVERHEAL_BARRIER_60"->"초과 회복 60%를 Barrier로 전환";
            case"SIG_P04_REVIVE_AEGIS"->"부활 대상 2행동간 피해 감소 25%";
            case"SIG_P04_SANCTUARY_GAUGE_120"->"Sanctuary 긴급 회복 시 Gauge +120";
            case"SIG_P05_FIRST_FOLLOWUP_PLUS_25"->"첫 추격 사격 위력 +25%p";
            case"SIG_P05_FOLLOWUP_LIMIT_2"->"자신 턴 사이 추격 사격 최대 2회";
            case"SIG_P05_SECOND_FOLLOWUP_GAUGE_160"->"두 번째 추격 후 Gauge +160";
            case"SIG_P06_RECORD_HEAL_4"->"Record 획득당 MaxHP 4% 회복";
            case"SIG_P06_RECORD5_DAMAGE_20"->"Record 5에서 직접 피해 +20%";
            case"SIG_P06_RETURN_CONDOLENCE_RESET"->"복귀 시 조문 초기화 + Barrier 20%";
            case"SIG_P07_PARTNER_VITALITY"->"계약수 HP/ATK/DEF 강화";
            case"SIG_P07_COMMAND_EMPOWER"->"공명 명령 Bond·계약수 추가타 강화";
            case"SIG_P07_SUMMON_DEATH_WARD"->"계약수 전투불능 시 파티 Barrier";
            case"SIG_P08_LOW_HP_SPEED_15"->"HP 50% 이하 SPD +15%";
            case"SIG_P08_LOW_HP_BASIC_PLUS_30"->"HP 50% 이하 난격 위력 +30%p";
            case"SIG_P08_BLOOD_RUSH"->"저HP 피의 돌진 후 Gauge +180·피해 감소";
            default->"";
        };
    }
    private static String characterName(String id){return ClientMetaState.snapshot().characters().stream().filter(r->r.id().equals(id)).map(ClientMetaState.CharacterRow::name).findFirst().orElse(id);}

    @Override public boolean isPauseScreen(){return false;}
}
