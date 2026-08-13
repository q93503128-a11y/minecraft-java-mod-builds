package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.item.ArcaneStaffItem.StaffProfile;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.network.ChooseTraditionPayload;
import kr.moonseungjun.arcanecircle.network.EquipSpellPayload;
import kr.moonseungjun.arcanecircle.network.PurchaseAcademyItemPayload;
import kr.moonseungjun.arcanecircle.network.QuestActionPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
import kr.moonseungjun.arcanecircle.registry.ModItems;
import kr.moonseungjun.arcanecircle.world.AcademyOfferCatalog;
import kr.moonseungjun.arcanecircle.world.MagicTradition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Responsive spellbook presentation. Navigation, circle rail, browser, detail reader and loadout
 * dock each own a non-overlapping region. Long explanatory text is clipped/wrapped inside its
 * reader instead of being allowed to collide with interaction controls.
 */
public final class GrimoireScreen extends Screen {
    private static final List<Tab> TABS=List.of(
            new Tab("atlas","주문","Ⅰ"),new Tab("recipes","융합","Ⅱ"),new Tab("staffs","지팡이","Ⅲ"),
            new Tab("academy","마도회","Ⅳ"),new Tab("quests","의뢰","Ⅴ"),new Tab("core","마력핵","Ⅵ"));
    private static final Map<String,Integer> SAVED_SCROLL=new HashMap<>();
    private static int activeSlot=-1;
    private static int atlasCircle=1,fusionCircle=1,academyCircle=1;
    private static String inspectedSpellId="",selectedStaffId="",equipCandidateId="";
    private static MagicTradition inspectedTradition=MagicTradition.ARCANE;

    private final String page;
    private int scroll;
    private String notice="";
    private long noticeUntil;

    public GrimoireScreen(String page){
        super(Minecraft.getInstance(),Minecraft.getInstance().font,Component.literal("구중 마도서"));
        this.page=normalize(page);
        this.scroll=SAVED_SCROLL.getOrDefault(scrollKey(),0);
    }

    @Override protected void init(){super.init();normalizeSelections();scroll=clamp(scroll,0,maxScroll(layout()));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean shouldCloseOnEsc(){return true;}
    @Override public void onClose(){saveScroll();super.onClose();}

    @Override
    public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){
        Layout l=layout();
        if(inside(event.x(),event.y(),l.close())){onClose();return true;}
        for(int i=0;i<TABS.size();i++)if(inside(event.x(),event.y(),l.tab(i))){request(TABS.get(i).id());return true;}
        return switch(page){
            case "recipes" -> clickRecipes(event,l)||super.mouseClicked(event,doubleClick);
            case "staffs" -> clickStaffs(event,l)||super.mouseClicked(event,doubleClick);
            case "academy" -> clickAcademy(event,l)||super.mouseClicked(event,doubleClick);
            case "quests" -> clickQuests(event,l)||super.mouseClicked(event,doubleClick);
            case "core" -> super.mouseClicked(event,doubleClick);
            default -> clickAtlas(event,l,doubleClick)||super.mouseClicked(event,doubleClick);
        };
    }

    private boolean clickAtlas(MouseButtonEvent e,Layout l,boolean doubleClick){
        for(int c=1;c<=9;c++)if(inside(e.x(),e.y(),l.circleIndex(c))){atlasCircle=c;scroll=0;ensureInspectedSpell();equipCandidateId="";activeSlot=-1;saveScroll();return true;}
        for(int i=0;i<5;i++)if(inside(e.x(),e.y(),l.loadout(i))){
            SpellDefinition candidate=SpellCatalog.spell(equipCandidateId).orElse(null);
            if(candidate!=null&&usable(candidate)){
                equipTo(candidate,i);activeSlot=-1;equipCandidateId="";
            }else{
                activeSlot=activeSlot==i?-1:i;
                String current=ArcaneClientState.slot(i);
                if(activeSlot>=0&&!current.isBlank())inspectedSpellId=current;
                notice(activeSlot<0?"장착 슬롯 선택 취소":(i+1)+"번 슬롯 선택 · 장착할 주문을 클릭");
            }
            return true;
        }
        List<SpellDefinition> spells=SpellCatalog.spellsInCircle(atlasCircle);
        for(int i=0;i<spells.size();i++)if(inside(e.x(),e.y(),l.spellTile(i,scroll,spells.size()))){
            SpellDefinition s=spells.get(i);inspectedSpellId=s.id();equipCandidateId=s.id();
            if(activeSlot>=0&&usable(s)){int slot=activeSlot;equipTo(s,slot);activeSlot=-1;equipCandidateId="";}
            else if(doubleClick)quickEquip(s);
            else notice(usable(s)?"주문 선택 · 슬롯 클릭 / 더블클릭 빠른 장착":"주문 정보 열람");
            return true;
        }
        if(inside(e.x(),e.y(),l.primaryAction())){SpellDefinition s=inspectedSpell();if(s!=null){if(activeSlot>=0){int slot=activeSlot;equipTo(s,slot);activeSlot=-1;equipCandidateId="";}else quickEquip(s);}return true;}
        return false;
    }

    private boolean clickRecipes(MouseButtonEvent e,Layout l){
        for(int c=1;c<=9;c++)if(inside(e.x(),e.y(),l.circleIndex(c))){fusionCircle=c;scroll=0;saveScroll();return true;}
        return false;
    }

    private boolean clickStaffs(MouseButtonEvent e,Layout l){
        List<StaffProfile> profiles=ModItems.profiles();
        for(int i=0;i<profiles.size();i++)if(inside(e.x(),e.y(),l.listRow(i,scroll,31))){String id=profiles.get(i).id();selectedStaffId=id.equals(selectedStaffId)?"":id;return true;}
        return false;
    }

    private boolean clickAcademy(MouseButtonEvent e,Layout l){
        MagicTradition[] ts=traditions();
        for(int i=0;i<ts.length;i++)if(inside(e.x(),e.y(),l.tradition(i))){inspectedTradition=ts[i];notice(inspectedTradition.displayName()+" 열람");return true;}
        if(inside(e.x(),e.y(),l.traditionJoin())){ClientPacketDistributor.sendToServer(new ChooseTraditionPayload(inspectedTradition.name()));notice(inspectedTradition.displayName()+" 소속 등록 요청");return true;}
        for(int c=1;c<=9;c++)if(inside(e.x(),e.y(),l.circleIndex(c))){academyCircle=c;scroll=0;saveScroll();return true;}
        List<AcademyOfferCatalog.Offer> offers=AcademyOfferCatalog.forCircle(academyCircle);
        for(int i=0;i<offers.size();i++)if(inside(e.x(),e.y(),l.offerRow(i,scroll))){ClientPacketDistributor.sendToServer(new PurchaseAcademyItemPayload(offers.get(i).id()));notice(offers.get(i).displayName()+" 구매 요청");return true;}
        return false;
    }

    private boolean clickQuests(MouseButtonEvent e,Layout l){
        String offered=ArcaneClientState.text("quest_offer_id","");
        if(!offered.isBlank()){
            if(inside(e.x(),e.y(),l.questAccept())){ClientPacketDistributor.sendToServer(new QuestActionPayload("accept"));notice("의뢰 수락 요청");return true;}
            if(inside(e.x(),e.y(),l.questReject())){ClientPacketDistributor.sendToServer(new QuestActionPayload("reject"));notice("의뢰 거절 요청");return true;}
        }
        int count=Math.min(3,ArcaneClientState.integer("quest_count",0));
        for(int i=0;i<count;i++){
            int p=ArcaneClientState.integer("quest_"+i+"_progress",0),t=ArcaneClientState.integer("quest_"+i+"_target",0);
            if(t>0&&p>=t&&inside(e.x(),e.y(),l.questClaim(i,scroll))){ClientPacketDistributor.sendToServer(new QuestActionPayload("claim:"+i));notice((i+1)+"번 의뢰 보상 수령 요청");return true;}
        }
        return false;
    }

    @Override public boolean mouseScrolled(double mouseX,double mouseY,double scrollX,double scrollY){
        Layout l=layout();if(scrollY==0||!inside(mouseX,mouseY,l.viewport()))return false;
        scroll=clamp(scroll+(scrollY<0?32:-32),0,maxScroll(l));saveScroll();return true;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partialTick){
        Layout l=layout();
        g.fill(0,0,width,height,0xD708080A);
        drawBook(g,l);
        drawSpine(g,l,mouseX,mouseY);
        switch(page){
            case "recipes" -> drawRecipes(g,l,mouseX,mouseY);
            case "staffs" -> drawStaffs(g,l,mouseX,mouseY);
            case "academy" -> drawAcademy(g,l,mouseX,mouseY);
            case "quests" -> drawQuests(g,l,mouseX,mouseY);
            case "core" -> drawCore(g,l);
            default -> drawAtlas(g,l,mouseX,mouseY);
        }
        drawNotice(g,l);
        super.extractRenderState(g,mouseX,mouseY,partialTick);
    }

    private void drawBook(GuiGraphicsExtractor g,Layout l){
        g.fill(l.left()-5,l.top()+5,l.right()+5,l.bottom()+7,0xB0000000);
        g.fill(l.left(),l.top(),l.right(),l.bottom(),0xF3151413);
        g.fill(l.left()+1,l.top()+1,l.left()+54,l.bottom()-1,0xFA0C0D10);
        g.fill(l.left()+54,l.top()+1,l.left()+56,l.bottom()-1,0xFF5B4935);
        g.fill(l.left()+57,l.top()+1,l.right()-1,l.bottom()-1,0xF21B1814);
        g.fill(l.left()+62,l.top()+22,l.right()-8,l.top()+23,0xFF4B4033);
        g.fill(l.right()-3,l.top()+8,l.right()-1,l.bottom()-8,0xFF8A704A);
        g.fill(l.left()+60,l.bottom()-3,l.right()-4,l.bottom()-1,0xFF6B5538);
        Rect close=l.close();
        g.text(font,Component.literal("×"),close.x()+6,close.y()+4,inside(lastMouseX,lastMouseY,close)?0xFFFFFFFF:0xFFC9BDAA);
    }

    private int lastMouseX,lastMouseY;

    private void drawSpine(GuiGraphicsExtractor g,Layout l,int mouseX,int mouseY){
        lastMouseX=mouseX;lastMouseY=mouseY;
        g.centeredText(font,Component.literal("九"),l.left()+27,l.top()+10,0xFFD8B875);
        for(int i=0;i<TABS.size();i++){
            Rect r=l.tab(i);Tab t=TABS.get(i);boolean active=t.id().equals(page),hover=inside(mouseX,mouseY,r);
            if(active){g.fill(r.x()+1,r.y()+3,r.x()+4,r.bottom()-3,0xFFE0B96B);ArcaneRenderUtil.diamond(g,r.x()+11,r.y()+r.h()/2,3,0xFFFFD786);}
            g.text(font,Component.literal(t.roman()),r.x()+18,r.y()+4,active?0xFFFFD990:hover?0xFFD7C7AE:0xFF766F66);
            tiny(g,t.label(),r.x()+18,r.y()+15,active?0xFFE8DDCB:hover?0xFFBDB1A0:0xFF706A62,.56F,false);
        }
    }

    private void drawAtlas(GuiGraphicsExtractor g,Layout l,int mouseX,int mouseY){
        ensureInspectedSpell();Rect b=l.body();
        title(g,b,"주문 도감",atlasCircle+"써클 · "+circleSubtitle(atlasCircle));
        if(b.w()>360)tiny(g,"주문↔슬롯 양방향 장착 · 더블클릭 빠른 장착",b.right()-4,b.y()+15,0xFF746D64,.48F,true);
        drawCircleIndex(g,l,atlasCircle,mouseX,mouseY,false);
        Rect v=l.browserViewport();
        g.enableScissor(v.x(),v.y(),v.right(),v.bottom());
        List<SpellDefinition> spells=SpellCatalog.spellsInCircle(atlasCircle);Set<String> known=ArcaneClientState.known();
        for(int i=0;i<spells.size();i++)drawSpellTile(g,l.spellTile(i,scroll,spells.size()),spells.get(i),known.contains(spells.get(i).id()),mouseX,mouseY);
        g.disableScissor();
        drawSpellDetail(g,l,inspectedSpell(),mouseX,mouseY);
        for(int i=0;i<5;i++)drawLoadout(g,l.loadout(i),i,mouseX,mouseY);
    }

    private void drawCircleIndex(GuiGraphicsExtractor g,Layout l,int selected,int mouseX,int mouseY,boolean shop){
        for(int c=1;c<=9;c++){
            Rect r=l.circleIndex(c);boolean active=c==selected,hover=inside(mouseX,mouseY,r),unlocked=c<=ArcaneClientState.integer("circle",1);
            int color=unlocked?circleColor(c):0xFF4D4A47;int cy=r.y()+r.h()/2,ring=Math.max(4,Math.min(8,r.h()/2-1));
            if(active)g.fill(r.x(),r.y()+2,r.x()+2,r.bottom()-2,color);
            g.centeredText(font,Component.literal(Integer.toString(c)),r.x()+r.w()/2,cy-4,active?0xFFF5E2BC:hover?0xFFD9C7A8:unlocked?0xFFA39A8C:0xFF5C5852);
            if(active||hover)ArcaneRenderUtil.ring(g,r.x()+r.w()/2,cy,ring,color);
            if(shop){int count=AcademyOfferCatalog.forCircle(c).size();tiny(g,Integer.toString(count),r.right()-2,r.bottom()-5,color,.45F,true);}
        }
    }

    private void drawSpellTile(GuiGraphicsExtractor g,Rect r,SpellDefinition s,boolean known,int mouseX,int mouseY){
        if(r.bottom()<0||r.y()>height)return;boolean selected=s.id().equals(inspectedSpellId),hover=inside(mouseX,mouseY,r),usable=known&&s.circle()<=ArcaneClientState.integer("circle",1);
        int accent=ArcaneRenderUtil.schoolColor(s.school());int cx=r.x()+r.w()/2,cy=r.y()+15;
        if(selected){g.fill(r.x()+5,r.bottom()-2,r.right()-5,r.bottom(),accent);ArcaneRenderUtil.ring(g,cx,cy,12,0xFFFFD28A);}
        else if(hover)ArcaneRenderUtil.ring(g,cx,cy,11,accent);
        ArcaneRenderUtil.ring(g,cx,cy,9,usable?accent:0xFF55524E);
        if(usable)ArcaneRenderUtil.spellRune(g,cx,cy,s,5,0xFFF6EEE0);else ArcaneRenderUtil.diamond(g,cx,cy,4,0xFF655F57);
        tiny(g,fit(s.name(),Math.max(24,r.w()*2)),cx,r.y()+30,usable?0xFFDCD2C2:0xFF716C64,.52F,true);
        if(ArcaneClientState.slots().contains(s.id()))tiny(g,"◆",r.right()-5,r.y()+2,0xFFFFD27C,.48F,true);
    }

    private void drawSpellDetail(GuiGraphicsExtractor g,Layout l,SpellDefinition s,int mouseX,int mouseY){
        Rect d=l.detail();if(d.w()<70||d.h()<56||s==null)return;
        g.enableScissor(d.x(),d.y(),d.right(),d.bottom());
        g.fill(d.x(),d.y()+3,d.x()+1,d.bottom()-3,0xFF554838);
        int accent=ArcaneRenderUtil.schoolColor(s.school());int cx=d.x()+d.w()/2;Rect a=l.primaryAction();
        boolean usable=usable(s);int empty=firstEmptySlot();
        String label=!usable?"습득 필요":activeSlot>=0?(activeSlot+1)+"번 슬롯에 장착":empty>=0?"빠른 장착 · "+(empty+1)+"번":"교체할 슬롯을 클릭";

        if(l.compact()){
            int iconY=d.y()+15;
            ArcaneRenderUtil.ring(g,cx,iconY,11,accent);ArcaneRenderUtil.ring(g,cx,iconY,8,0xFF6E5E49);ArcaneRenderUtil.spellRune(g,cx,iconY,s,5,0xFFFFF2DC);
            g.centeredText(font,Component.literal(fit(s.name(),Math.max(36,d.w()-8))),cx,d.y()+29,0xFFF0E4D1);
            tiny(g,s.circle()+"C · "+s.school().displayName(),cx,d.y()+41,accent,.52F,true);
            tiny(g,fit("MP "+s.manaCost()+" · 쿨 "+one(s.cooldownTicks()/20.0)+"s",d.w()*2),cx,d.y()+53,0xFFCDBDA7,.50F,true);
            tiny(g,fit("범위 "+one(s.range())+" · 숙련 "+ArcaneClientState.mastery(s.id())+"/"+SpellCatalog.masteryRequired(s.id()),d.w()*2),cx,d.y()+63,0xFF9F9587,.48F,true);
            action(g,a,label,inside(mouseX,mouseY,a),usable&&(activeSlot>=0||firstEmptySlot()>=0),accent);
            g.disableScissor();return;
        }

        ArcaneRenderUtil.ring(g,cx,d.y()+35,23,accent);ArcaneRenderUtil.ring(g,cx,d.y()+35,17,0xFF6E5E49);ArcaneRenderUtil.spellRune(g,cx,d.y()+35,s,11,0xFFFFF2DC);
        g.centeredText(font,Component.literal(fit(s.name(),d.w()-12)),cx,d.y()+65,0xFFF0E4D1);
        tiny(g,s.circle()+"C · "+s.school().displayName()+" · "+s.sigilAnchor().displayName(),cx,d.y()+79,accent,.58F,true);
        int descStart=d.y()+94,statsHeight=44;
        int maxDescLines=Math.max(0,Math.min(4,(a.y()-descStart-10-statsHeight-4)/9));
        List<String> desc=wrap(s.description(),d.w()-18,maxDescLines);int y=descStart;
        for(String line:desc){tiny(g,line,d.x()+9,y,0xFFB8AD9E,.62F,false);y+=9;}
        g.fill(d.x()+8,y+3,d.right()-8,y+4,0xFF493E31);y+=10;
        tiny(g,"MP",d.x()+9,y,0xFF877D71,.54F,false);tiny(g,Integer.toString(s.manaCost()),d.x()+35,y,0xFFE7D5B8,.62F,false);
        tiny(g,"쿨",d.x()+9,y+12,0xFF877D71,.54F,false);tiny(g,one(s.cooldownTicks()/20.0)+"s",d.x()+35,y+12,0xFFE7D5B8,.62F,false);
        tiny(g,"범위",d.x()+9,y+24,0xFF877D71,.54F,false);tiny(g,one(s.range()),d.x()+35,y+24,0xFFE7D5B8,.62F,false);
        tiny(g,"숙련",d.x()+9,y+36,0xFF877D71,.54F,false);tiny(g,ArcaneClientState.mastery(s.id())+" / "+SpellCatalog.masteryRequired(s.id()),d.x()+35,y+36,0xFFE7D5B8,.62F,false);
        action(g,a,label,inside(mouseX,mouseY,a),usable&&(activeSlot>=0||firstEmptySlot()>=0),accent);
        g.disableScissor();
    }

    private void drawLoadout(GuiGraphicsExtractor g,Rect r,int slot,int mouseX,int mouseY){
        SpellDefinition s=SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null);boolean active=activeSlot==slot,hover=inside(mouseX,mouseY,r);int accent=s==null?0xFF625D55:ArcaneRenderUtil.schoolColor(s.school());
        int cx=r.x()+r.w()/2,cy=r.y()+r.h()/2,rad=Math.max(5,Math.min(9,Math.min(r.w(),r.h())/2-2));
        if(active){ArcaneRenderUtil.ring(g,cx,cy,rad+2,0xFFFFD275);ArcaneRenderUtil.diamond(g,cx,cy,rad,0x26FFD275);}else ArcaneRenderUtil.ring(g,cx,cy,rad+1,hover?accent:0xFF514C46);
        if(s!=null)ArcaneRenderUtil.spellRune(g,cx,cy,s,Math.max(3,rad-3),active?0xFFFFE6AA:0xFFEAE0D1);else ArcaneRenderUtil.diamond(g,cx,cy,Math.max(2,rad-4),0xFF655F57);
        tiny(g,Integer.toString(slot+1),r.x()+3,r.y()+2,active?0xFFFFD584:0xFF8B8378,.50F,false);
        if(s!=null&&ArcaneClientState.cooldownRemainingTicks(slot)>0)g.fill(r.x()+2,r.bottom()-2,r.x()+2+(int)((r.w()-4)*ArcaneClientState.cooldownFraction(slot)),r.bottom()-1,0xFFB34D52);
    }

    private void drawRecipes(GuiGraphicsExtractor g,Layout l,int mouseX,int mouseY){
        Rect b=l.body();title(g,b,"융합 연구",fusionCircle+"써클 결과식");drawCircleIndex(g,l,fusionCircle,mouseX,mouseY,false);Rect v=l.viewport();
        g.enableScissor(v.x(),v.y(),v.right(),v.bottom());List<SpellCatalog.FusionFormula> formulas=fusionsInCircle(fusionCircle);
        for(int i=0;i<formulas.size();i++){Rect r=l.listRow(i,scroll,43);SpellCatalog.FusionFormula f=formulas.get(i);SpellDefinition result=SpellCatalog.spell(f.result()).orElseThrow();boolean circleReady=result.circle()<=ArcaneClientState.integer("circle",1);boolean learned=f.ingredients().stream().allMatch(id->ArcaneClientState.known().contains(id));boolean cool=f.ingredients().stream().allMatch(id->ArcaneClientState.cooldownRemainingTicks(id)<=0);boolean ready=circleReady&&learned&&cool;int accent=ArcaneRenderUtil.schoolColor(result.school());rule(g,r.y()-1,r.x(),r.right(),ready?accent:0xFF5B4A49);ArcaneRenderUtil.ring(g,r.x()+15,r.y()+17,9,ready?accent:0xFF5B5650);ArcaneRenderUtil.spellRune(g,r.x()+15,r.y()+17,result,5,ready?0xFFF4E8D6:0xFF716B64);g.text(font,Component.literal(fit(result.name(),r.w()-44)),r.x()+31,r.y()+5,ready?0xFFE9DECD:0xFF817970);String chain=f.ingredients().stream().map(id->SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id)).reduce((a,c)->a+" + "+c).orElse("");tiny(g,fit(chain,(r.w()-40)*2),r.x()+31,r.y()+19,0xFF9E9487,.55F,false);String status=!circleReady?"써클 부족":!learned?"재료 주문 미습득":!cool?"재료 쿨타임":"융합 가능";tiny(g,status,r.right()-5,r.y()+32,ready?0xFF75CEA2:0xFFB97474,.52F,true);}
        g.disableScissor();
    }

    private void drawStaffs(GuiGraphicsExtractor g,Layout l,int mouseX,int mouseY){
        Rect b=l.body();title(g,b,"지팡이 서고","클릭하면 제원과 조합법을 펼칩니다");Rect v=l.viewport();List<StaffProfile> profiles=ModItems.profiles();
        int listW=l.isWide()?Math.max(190,v.w()/2):v.w();Rect list=new Rect(v.x(),v.y(),listW,v.h());g.enableScissor(list.x(),list.y(),list.right(),list.bottom());
        for(int i=0;i<profiles.size();i++){Rect r=new Rect(list.x(),list.y()+i*31-scroll,list.w(),29);StaffProfile p=profiles.get(i);boolean selected=p.id().equals(selectedStaffId),equipped=p.id().equals(ArcaneClientState.text("staff_id","none"));int accent=p.favoredSchool()==null?0xFFC9A568:ArcaneRenderUtil.schoolColor(p.favoredSchool());if(selected)g.fill(r.x(),r.y()+3,r.x()+2,r.bottom()-3,accent);ArcaneRenderUtil.diamond(g,r.x()+12,r.y()+14,selected?6:4,equipped?0xFFFFD47B:accent);g.text(font,Component.literal(fit(p.displayName(),r.w()-34)),r.x()+25,r.y()+5,selected?0xFFF0E1CA:0xFFB8AE9F);tiny(g,fit(staffStats(p),(r.w()-34)*2),r.x()+25,r.y()+18,0xFF786F65,.50F,false);rule(g,r.bottom(),r.x()+24,r.right()-4,0xFF3B342C);}
        g.disableScissor();if(l.isWide())drawStaffDetail(g,l,selectedStaffId);
    }

    private void drawStaffDetail(GuiGraphicsExtractor g,Layout l,String id){
        Rect d=l.detail();if(d.w()<70||d.h()<45)return;g.enableScissor(d.x(),d.y(),d.right(),d.bottom());g.fill(d.x(),d.y()+4,d.x()+1,d.bottom()-4,0xFF554838);StaffProfile p=id.isBlank()?StaffProfile.NONE:ModItems.profile(id);int cx=d.x()+d.w()/2;int iconR=Math.max(9,Math.min(18,d.h()/7));ArcaneRenderUtil.diamond(g,cx,d.y()+iconR+7,iconR,p==StaffProfile.NONE?0xFF5D564D:0xFFD2AE70);g.centeredText(font,Component.literal(fit(p.displayName(),Math.max(40,d.w()-8))),cx,d.y()+iconR*2+13,0xFFEADCC7);int y=d.y()+iconR*2+30,remaining=Math.max(0,d.bottom()-y-4),lines=Math.max(0,Math.min(4,remaining/9));for(String line:wrap(p.summary(),d.w()-18,lines)){tiny(g,line,d.x()+9,y,0xFFAFA496,.60F,false);y+=9;}if(y+8<d.bottom()){y+=5;int recipeLines=Math.max(0,Math.min(3,(d.bottom()-y-3)/9));for(String line:wrap(p.recipeHint().isBlank()?"제작 정보 없음":"제작 · "+p.recipeHint(),d.w()-18,recipeLines)){tiny(g,line,d.x()+9,y,0xFFD0B789,.56F,false);y+=9;}}g.disableScissor();
    }

    private void drawAcademy(GuiGraphicsExtractor g,Layout l,int mouseX,int mouseY){
        Rect b=l.body();title(g,b,"마도회 기록부","아르카나 "+ArcaneClientState.longInteger("marks",0));MagicTradition current=MagicTradition.parse(ArcaneClientState.text("tradition","UNBOUND"));MagicTradition[] ts=traditions();
        for(int i=0;i<ts.length;i++){Rect r=l.tradition(i);boolean active=ts[i]==inspectedTradition,joined=ts[i]==current,hover=inside(mouseX,mouseY,r);if(active)g.fill(r.x()+5,r.bottom()-2,r.right()-5,r.bottom(),0xFFD2AE6B);g.centeredText(font,Component.literal(ts[i].displayName()),r.x()+r.w()/2,r.y()+5,joined?0xFFFFD984:active?0xFFEADCC7:hover?0xFFC9BAA4:0xFF777067);}
        action(g,l.traditionJoin(),current==inspectedTradition?"현재 소속":"소속 등록",inside(mouseX,mouseY,l.traditionJoin()),true,0xFFD2AE6B);
        drawCircleIndex(g,l,academyCircle,mouseX,mouseY,true);Rect v=l.academyOffers();g.enableScissor(v.x(),v.y(),v.right(),v.bottom());List<AcademyOfferCatalog.Offer> offers=AcademyOfferCatalog.forCircle(academyCircle);long marks=ArcaneClientState.longInteger("marks",0);
        for(int i=0;i<offers.size();i++){Rect r=l.offerRow(i,scroll);AcademyOfferCatalog.Offer o=offers.get(i);boolean enough=marks>=o.basePrice();rule(g,r.bottom()-1,r.x(),r.right(),0xFF3E362D);ArcaneRenderUtil.diamond(g,r.x()+11,r.y()+14,4,enough?circleColor(academyCircle):0xFF6A5552);g.text(font,Component.literal(fit(o.displayName(),r.w()-90)),r.x()+24,r.y()+5,enough?0xFFDED2BF:0xFF8A7E74);tiny(g,fit(o.description(),(r.w()-90)*2),r.x()+24,r.y()+18,0xFF756D64,.50F,false);tiny(g,o.basePrice()+" A",r.right()-5,r.y()+10,enough?0xFFFFD179:0xFFB46F72,.58F,true);}
        g.disableScissor();
        Rect note=l.academyNote();String prefix="faction_"+inspectedTradition.name().toLowerCase(Locale.ROOT);tiny(g,fit(inspectedTradition.strength(),note.w()*2),note.x(),note.y()+2,0xFF82C6A4,.54F,false);tiny(g,fit(inspectedTradition.weakness(),note.w()*2),note.x(),note.y()+13,0xFFC47E7E,.54F,false);tiny(g,fit(ArcaneClientState.text(prefix+"_headquarters",""),note.w()*2),note.x(),note.y()+24,0xFF8C8378,.52F,false);
    }

    private void drawQuests(GuiGraphicsExtractor g,Layout l,int mouseX,int mouseY){
        Rect b=l.body();title(g,b,"의뢰 문서","수락 전 난이도와 보상을 확인합니다");Rect v=l.viewport();String offered=ArcaneClientState.text("quest_offer_id","");int y=v.y()-scroll;
        if(offered.isBlank()){tiny(g,"새 제안 없음",v.x()+4,y+6,0xFF8E867C,.65F,false);tiny(g,"마도사 주민과 대화하면 새 의뢰가 기록됩니다.",v.x()+4,y+20,0xFF6E685F,.54F,false);y+=43;}else{String diff=ArcaneClientState.text("quest_offer_difficulty_name","견습"),desc=ArcaneClientState.text("quest_offer_desc","마도 의뢰");long reward=ArcaneClientState.longInteger("quest_offer_reward",0);g.text(font,Component.literal("["+diff+"] "+fit(desc,v.w()-190)),v.x()+4,y+4,0xFFEAD8B8);tiny(g,"목표 "+ArcaneClientState.integer("quest_offer_target",0)+" · "+reward+" A",v.x()+4,y+20,0xFFC9A66A,.58F,false);action(g,l.questAccept(),"수락",inside(mouseX,mouseY,l.questAccept()),true,0xFF78B690);action(g,l.questReject(),"거절",inside(mouseX,mouseY,l.questReject()),true,0xFFB66F72);y+=49;}rule(g,y,v.x(),v.right(),0xFF4A4034);y+=8;
        int count=Math.min(3,ArcaneClientState.integer("quest_count",0));if(count==0){tiny(g,"진행 중인 의뢰 없음",v.x()+4,y+8,0xFF756E65,.60F,false);return;}
        for(int i=0;i<count;i++){int progress=ArcaneClientState.integer("quest_"+i+"_progress",0),target=Math.max(1,ArcaneClientState.integer("quest_"+i+"_target",1));long reward=ArcaneClientState.longInteger("quest_"+i+"_reward",0);String diff=ArcaneClientState.text("quest_"+i+"_difficulty_name","견습"),desc=ArcaneClientState.text("quest_"+i+"_desc","마도 의뢰");Rect r=l.questRow(i,scroll,offered.isBlank()?51:57);boolean complete=progress>=target;g.text(font,Component.literal((i+1)+" · ["+diff+"] "+fit(desc,r.w()-120)),r.x()+3,r.y()+4,complete?0xFFFFD37D:0xFFD6CBBA);tiny(g,progress+" / "+target+" · "+reward+" A",r.x()+3,r.y()+19,complete?0xFFD9B36D:0xFF8C8378,.56F,false);int lineW=r.w()-105;g.fill(r.x()+3,r.y()+32,r.x()+3+lineW,r.y()+34,0xFF3A342D);g.fill(r.x()+3,r.y()+32,r.x()+3+(int)(lineW*Math.min(1,progress/(double)target)),r.y()+34,complete?0xFFD1AC68:0xFF7B6B88);if(complete)action(g,l.questClaim(i,scroll),"보상",inside(mouseX,mouseY,l.questClaim(i,scroll)),true,0xFFD1AC68);}
    }

    private void drawCore(GuiGraphicsExtractor g,Layout l){
        Rect b=l.body();title(g,b,"마력핵","현재 마도사 상태");Rect v=l.viewport();if(v.w()<44||v.h()<38)return;g.enableScissor(v.x(),v.y(),v.right(),v.bottom());
        int circle=ArcaneClientState.integer("circle",1),accent=circleColor(circle);int topH=Math.max(56,Math.min(108,(int)Math.round(v.h()*.58)));int sealZone=Math.max(58,Math.min(145,v.w()/3));int sealR=Math.max(16,Math.min(34,Math.min(sealZone/3,topH/3)));int cx=v.x()+sealZone/2,cy=v.y()+topH/2;
        ArcaneRenderUtil.ring(g,cx,cy,sealR,accent);ArcaneRenderUtil.ring(g,cx,cy,Math.max(10,sealR-8),0xFF695942);ArcaneRenderUtil.diamond(g,cx,cy,Math.max(7,sealR-18),0xFFE9D5AC);g.centeredText(font,Component.literal(circle+"C"),cx,cy-4,0xFF251B10);
        int sx=v.x()+sealZone+6,sy=v.y()+7,statW=Math.max(38,v.right()-sx-4);List<String> stats=List.of("MP  "+ArcaneClientState.integer("mana",0)+" / "+ArcaneClientState.integer("max",100),"회복  "+one(ArcaneClientState.regenPerSecond())+" /초","통찰  "+ArcaneClientState.integer("insight",0),"아르카나  "+ArcaneClientState.longInteger("marks",0));int statStep=Math.max(11,Math.min(16,(topH-12)/4));for(String s:stats){tiny(g,fit(s,statW*2),sx,sy,0xFFD8CCBA,.62F,false);sy+=statStep;}
        int gearTop=v.y()+topH;if(gearTop<v.bottom()-11){rule(g,gearTop,v.x()+2,v.right()-2,0xFF4B4033);tiny(g,"장비 · 소속",v.x()+3,gearTop+4,0xFF827A70,.50F,false);gearTop+=14;MagicTradition t=MagicTradition.parse(ArcaneClientState.text("tradition","UNBOUND"));List<String> gear=List.of("지팡이 · "+ArcaneClientState.text("staff","맨손"),"모자 · "+ArcaneClientState.text("gear_hat","없음"),"로브 · "+ArcaneClientState.text("gear_robe","없음"),"마도화 · "+ArcaneClientState.text("gear_boots","없음"),"소속 · "+t.displayName());int cols=v.w()>=235?2:1,rows=(gear.size()+cols-1)/cols,gap=7,colW=Math.max(35,(v.w()-gap*(cols-1))/cols),available=Math.max(1,v.bottom()-gearTop-2),rowH=Math.max(8,Math.min(12,available/Math.max(1,rows)));for(int i=0;i<gear.size();i++){int row=i/cols,col=i%cols,y=gearTop+row*rowH;if(y>=v.bottom()-5)break;int x=v.x()+col*(colW+gap);tiny(g,fit(gear.get(i),colW*2),x,y,0xFF9F9587,.54F,false);}}
        g.disableScissor();
    }

    private void title(GuiGraphicsExtractor g,Rect body,String title,String sub){g.text(font,Component.literal(title),body.x()+2,body.y()+2,0xFFF0E2CD);tiny(g,sub,body.x()+2,body.y()+15,0xFF827A70,.54F,false);}
    private void action(GuiGraphicsExtractor g,Rect r,String label,boolean hover,boolean enabled,int accent){int c=enabled?(hover?0xFFFFDE9A:accent):0xFF5A554F;g.fill(r.x(),r.bottom()-1,r.right(),r.bottom(),c);if(hover&&enabled)g.fill(r.x(),r.y(),r.x()+2,r.bottom(),c);g.centeredText(font,Component.literal(fit(label,r.w()-8)),r.x()+r.w()/2,r.y()+5,enabled?(hover?0xFFFFF0D0:0xFFD8C9B3):0xFF6C665E);}
    private void rule(GuiGraphicsExtractor g,int y,int x0,int x1,int color){g.fill(x0,y,x1,y+1,color);}
    private void drawNotice(GuiGraphicsExtractor g,Layout l){String server=ArcaneClientState.noticeText();String shown=!server.isBlank()?server:(!notice.isBlank()&&System.currentTimeMillis()<=noticeUntil?notice:"");if(shown.isBlank())return;int w=Math.min(l.body().w()-20,Math.max(120,font.width(shown)+18)),x=l.body().x()+l.body().w()/2-w/2,y=l.top()+7;g.fill(x,y,x+w,y+16,0xEE0B0B0D);g.fill(x,y,x+2,y+16,0xFFD2AE6B);g.centeredText(font,Component.literal(fit(shown,w-10)),x+w/2,y+4,0xFFEADDC9);}
    private void tiny(GuiGraphicsExtractor g,String text,int x,int y,int color,float scale,boolean centered){g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);if(centered)g.centeredText(font,Component.literal(text),0,0,color);else g.text(font,Component.literal(text),0,0,color);g.pose().popMatrix();}

    private boolean usable(SpellDefinition s){return s!=null&&ArcaneClientState.known().contains(s.id())&&s.circle()<=ArcaneClientState.integer("circle",1);}
    private int firstEmptySlot(){for(int i=0;i<5;i++)if(ArcaneClientState.slot(i).isBlank())return i;return -1;}
    private void equipTo(SpellDefinition s,int slot){if(slot<0||slot>=5){notice("잘못된 장착 슬롯입니다");return;}if(!usable(s)){notice(s==null?"장착할 주문이 없습니다":"아직 사용할 수 없는 주문입니다");return;}ClientPacketDistributor.sendToServer(new EquipSpellPayload(s.id(),slot));notice((slot+1)+"번 슬롯 · "+s.name());}
    private void quickEquip(SpellDefinition s){if(!usable(s)){notice("아직 사용할 수 없는 주문입니다");return;}int slot=firstEmptySlot();if(slot<0){notice("빈 슬롯 없음 · 교체할 슬롯을 먼저 클릭하세요");return;}equipTo(s,slot);equipCandidateId="";}
    private void request(String next){saveScroll();ClientPacketDistributor.sendToServer(new RequestGrimoirePayload(next));}
    private void notice(String text){notice=text;noticeUntil=System.currentTimeMillis()+1800L;}
    private void saveScroll(){SAVED_SCROLL.put(scrollKey(),scroll);}
    private String scrollKey(){return page+":"+("atlas".equals(page)?atlasCircle:"recipes".equals(page)?fusionCircle:"academy".equals(page)?academyCircle:0);}
    private void normalizeSelections(){atlasCircle=clamp(atlasCircle,1,9);fusionCircle=clamp(fusionCircle,1,9);academyCircle=clamp(academyCircle,1,9);ensureInspectedSpell();}
    private void ensureInspectedSpell(){List<SpellDefinition> list=SpellCatalog.spellsInCircle(atlasCircle);if(list.isEmpty()){inspectedSpellId="";return;}if(list.stream().noneMatch(s->s.id().equals(inspectedSpellId)))inspectedSpellId=list.getFirst().id();}
    private SpellDefinition inspectedSpell(){return SpellCatalog.spell(inspectedSpellId).orElseGet(()->SpellCatalog.spellsInCircle(atlasCircle).isEmpty()?null:SpellCatalog.spellsInCircle(atlasCircle).getFirst());}

    private int maxScroll(Layout l){return switch(page){case "atlas"->{int count=SpellCatalog.spellsInCircle(atlasCircle).size();yield l.maxTileScroll(count);}case "recipes"->Math.max(0,fusionsInCircle(fusionCircle).size()*43-l.viewport().h());case "staffs"->Math.max(0,ModItems.profiles().size()*31-l.viewport().h());case "academy"->Math.max(0,AcademyOfferCatalog.forCircle(academyCircle).size()*31-l.academyOffers().h());case "quests"->Math.max(0,Math.min(3,ArcaneClientState.integer("quest_count",0))*48-l.viewport().h()+70);default->0;};}
    private Layout layout(){int w=Math.min(780,Math.max(220,width-28)),h=Math.min(450,Math.max(180,height-24));w=Math.min(w,Math.max(1,width-8));h=Math.min(h,Math.max(1,height-8));return new Layout((width-w)/2,(height-h)/2,w,h);}
    private List<String> wrap(String value,int pixels,int maxLines){List<String> out=new ArrayList<>();if(value==null||value.isBlank())return out;String remain=value.trim();for(int line=0;line<maxLines&&!remain.isEmpty();line++){if(font.width(remain)<=pixels){out.add(remain);break;}int cut=remain.length();while(cut>1&&font.width(remain.substring(0,cut))>pixels)cut--;int space=remain.lastIndexOf(' ',cut);if(space>Math.max(1,cut/2))cut=space;String part=remain.substring(0,cut).trim();out.add(line==maxLines-1?fit(part+"…",pixels):part);remain=remain.substring(cut).trim();}return out;}
    private String fit(String value,int pixels){if(value==null||pixels<=0)return"";if(font.width(value)<=pixels)return value;String suffix="…";int allowed=Math.max(0,pixels-font.width(suffix)),end=value.length();while(end>0&&font.width(value.substring(0,end))>allowed)end--;return end<=0?suffix:value.substring(0,end)+suffix;}
    private static String staffStats(StaffProfile p){return "MP "+signed(p.maxManaBonus())+" · 위력 "+signed((int)Math.round((p.powerMultiplier()-1)*100))+"% · 범위 "+signed((int)Math.round((p.rangeMultiplier()-1)*100))+"%";}
    private static String signed(int v){return v>=0?"+"+v:Integer.toString(v);}
    private static String one(double v){return String.format(Locale.ROOT,"%.1f",v);}
    private static List<SpellCatalog.FusionFormula> fusionsInCircle(int c){return SpellCatalog.fusions().stream().filter(f->SpellCatalog.spell(f.result()).map(s->s.circle()==c).orElse(false)).toList();}
    private static MagicTradition[] traditions(){return new MagicTradition[]{MagicTradition.ARCANE,MagicTradition.DIVINE,MagicTradition.OCCULT,MagicTradition.PRIMAL};}
    private static String normalize(String p){return "recipes".equals(p)||"staffs".equals(p)||"academy".equals(p)||"quests".equals(p)||"core".equals(p)?p:"atlas";}
    private static boolean inside(double x,double y,Rect r){return x>=r.x()&&y>=r.y()&&x<r.right()&&y<r.bottom();}
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private static int circleColor(int c){return switch(c){case 1->0xFF82A9D6;case 2->0xFF78B9C9;case 3->0xFF80B99A;case 4->0xFFD0B06D;case 5->0xFFD18B6D;case 6->0xFFB18AC5;case 7->0xFF9B79D0;case 8->0xFF806FC6;default->0xFFE0C56F;};}
    private static String circleSubtitle(int c){return switch(c){case 1->"기초 회로";case 2->"전투 입문";case 3->"정규 마도";case 4->"상급 전술";case 5->"전장 지배";case 6->"대마법사";case 7->"초월 마법";case 8->"신화 마법";default->"세계급 의식";};}

    private record Tab(String id,String label,String roman){}
    private record Rect(int x,int y,int w,int h){int right(){return x+w;}int bottom(){return y+h;}}
    private record Layout(int left,int top,int panelW,int panelH){
        int right(){return left+panelW;}int bottom(){return top+panelH;}boolean isWide(){return panelW>=590;}boolean compact(){return panelH<320||body().h()<250;}
        int tabStep(){return Math.max(20,Math.min(38,Math.max(20,(panelH-28)/6)));}
        Rect close(){return new Rect(right()-24,top+5,18,18);}Rect tab(int i){int step=tabStep(),h=Math.max(18,Math.min(32,step-2));return new Rect(left+4,top+25+i*step,48,h);}Rect body(){return new Rect(left+66,top+27,Math.max(96,panelW-78),Math.max(78,panelH-38));}
        int footerHeight(){return compact()?29:32;}int footerTop(){return body().bottom()-footerHeight();}int contentTop(){return body().y()+31;}int contentBottom(){return Math.max(contentTop()+1,footerTop()-4);}
        Rect viewport(){Rect b=body();return new Rect(b.x()+38,contentTop(),Math.max(32,b.w()-40),Math.max(1,contentBottom()-contentTop()));}
        int circleStep(){int available=Math.max(9,contentBottom()-(body().y()+28));return Math.max(1,available/9);}
        Rect circleIndex(int c){Rect b=body();int step=circleStep(),y=b.y()+28+(c-1)*step,h=Math.max(1,Math.min(22,step));return new Rect(b.x(),y,30,h);}
        int detailWidth(){Rect b=body();int available=Math.max(0,b.w()-38);if(available<178)return 0;int preferred=Math.max(96,Math.min(205,b.w()/3)),minBrowser=66,maxDetail=Math.max(70,available-minBrowser-5);return Math.min(preferred,maxDetail);}
        Rect detail(){Rect b=body();int w=detailWidth(),y=contentTop(),h=Math.max(1,contentBottom()-y);return new Rect(w<=0?b.right():b.right()-w,y,w,h);}
        Rect browserViewport(){Rect b=body(),d=detail();int x=b.x()+38,y=contentTop(),right=d.w()>0?d.x()-5:b.right();return new Rect(x,y,Math.max(1,right-x),Math.max(1,contentBottom()-y));}
        Rect spellTile(int i,int scroll,int count){Rect v=browserViewport();int cols=v.w()>=340?5:v.w()>=230?4:v.w()>=150?3:v.w()>=88?2:1;int gap=3,w=Math.max(16,(v.w()-gap*(cols-1))/cols),row=i/cols,col=i%cols;return new Rect(v.x()+col*(w+gap),v.y()+row*48-scroll,w,44);}int maxTileScroll(int count){Rect v=browserViewport();int cols=v.w()>=340?5:v.w()>=230?4:v.w()>=150?3:v.w()>=88?2:1;return Math.max(0,((count+cols-1)/cols)*48-v.h());}
        Rect loadout(int i){Rect b=body();int gap=4,w=Math.max(16,(b.w()-gap*4)/5);return new Rect(b.x()+i*(w+gap),footerTop()+2,w,Math.max(18,footerHeight()-4));}
        Rect primaryAction(){Rect d=detail();if(d.w()<70)return new Rect(d.x(),d.bottom(),0,0);int h=compact()?17:20;return new Rect(d.x()+8,Math.max(d.y(),d.bottom()-h),Math.max(54,d.w()-16),Math.min(h,d.h()));}
        Rect listRow(int i,int scroll,int h){Rect v=viewport();return new Rect(v.x(),v.y()+i*h-scroll,v.w(),h-2);}
        boolean stackedTraditions(){return body().w()<310;}Rect tradition(int i){Rect b=body();int x=b.x()+36;if(stackedTraditions()){int gap=3,w=Math.max(42,(b.w()-42-gap)/2);return new Rect(x+(i%2)*(w+gap),b.y()+26+(i/2)*18,w,16);}int reserve=102,w=Math.max(44,(b.w()-36-reserve)/4);return new Rect(x+i*w,b.y()+26,w,20);}Rect traditionJoin(){Rect b=body();return stackedTraditions()?new Rect(b.right()-92,b.y()+63,88,18):new Rect(b.right()-100,b.y()+26,94,20);}Rect academyOffers(){Rect v=viewport();int offset=stackedTraditions()?48:28;return new Rect(v.x(),v.y()+offset,v.w(),Math.max(1,v.h()-offset-30));}Rect offerRow(int i,int scroll){Rect v=academyOffers();return new Rect(v.x(),v.y()+i*31-scroll,v.w(),29);}Rect academyNote(){Rect v=viewport();return new Rect(v.x(),Math.max(v.y(),v.bottom()-29),v.w(),29);}
        Rect questAccept(){Rect v=viewport();return new Rect(Math.max(v.x(),v.right()-88),v.y()+3,40,19);}Rect questReject(){Rect v=viewport();return new Rect(Math.max(v.x(),v.right()-43),v.y()+3,40,19);}Rect questRow(int i,int scroll,int baseY){Rect v=viewport();return new Rect(v.x(),v.y()+baseY+i*48-scroll,v.w(),45);}Rect questClaim(int i,int scroll){Rect r=questRow(i,scroll,51);return new Rect(Math.max(r.x(),r.right()-48),r.y()+7,44,20);}
    }
}
