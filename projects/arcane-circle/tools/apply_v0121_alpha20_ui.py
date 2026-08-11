#!/usr/bin/env python3
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
PATH=ROOT/"src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java"

def read(): return PATH.read_text(encoding="utf-8")
def write(s): PATH.write_text(s,encoding="utf-8")
def once(old,new,label):
    s=read()
    if new in s and old not in s: return
    if old not in s: raise SystemExit(f"{label}: marker missing")
    write(s.replace(old,new,1))
def between(start,end,replacement,label):
    s=read(); a=s.find(start); b=s.find(end,a+len(start))
    if a<0 or b<0: raise SystemExit(f"{label}: bounds missing")
    write(s[:a]+replacement+s[b:])

once("if (inside(event.x(), event.y(), l.academyCircleCard(circle))) {",
     "if (inside(event.x(), event.y(), l.academyCircleCard(circle, scroll))) {",
     "academy scrolled click")
once("if (inside(event.x(), event.y(), l.questAccept())) {",
     "if (inside(event.x(), event.y(), l.questAccept(scroll))) {","quest accept scroll")
once("if (inside(event.x(), event.y(), l.questReject())) {",
     "if (inside(event.x(), event.y(), l.questReject(scroll))) {","quest reject scroll")
once("inside(event.x(), event.y(), l.questClaim(i))) {",
     "inside(event.x(), event.y(), l.questClaim(i, scroll))) {","quest claim scroll")
once('''        if (scrollY == 0.0 || !inside(mouseX, mouseY, l.content())) return false;
        scroll = clamp(scroll + (scrollY < 0 ? 28 : -28), 0, maxScroll(l));
''','''        if (scrollY == 0.0 || !inside(mouseX, mouseY, l.content())) return false;
        int step = "academy".equals(page) && academyCircle == 0 ? 38
                : "quests".equals(page) ? 50 : 28;
        scroll = clamp(scroll + (scrollY < 0 ? step : -step), 0, maxScroll(l));
''',"page scroll step")

once('''        button(g, l.traditionJoin(), current==inspectedTradition?"현재 소속":"소속 등록", inside(mouseX,mouseY,l.traditionJoin()), true);
        if (academyCircle == 0) { for(int circle=1;circle<=9;circle++) drawCircleCard(g,l.academyCircleCard(circle),circle,mouseX,mouseY,true); return; }
        Rect back=l.academyBack();button(g,back,"‹ 써클",inside(mouseX,mouseY,back),true);g.text(font,Component.literal(academyCircle+"C 상점"),back.right()+8,back.y()+5,0xFFF1E8FA);
''','''        button(g, l.traditionJoin(), current==inspectedTradition?"현재 소속":"소속 등록", inside(mouseX,mouseY,l.traditionJoin()), true);
        if (academyCircle == 0) {
            Rect circles=l.academyCircleViewport();
            g.enableScissor(circles.x(),circles.y(),circles.right(),circles.bottom());
            for(int circle=1;circle<=9;circle++)drawCircleCard(g,l.academyCircleCard(circle,scroll),circle,mouseX,mouseY,true);
            g.disableScissor();
            return;
        }
        Rect back=l.academyBack();button(g,back,"‹ 써클",inside(mouseX,mouseY,back),true);g.text(font,Component.literal(academyCircle+"C 상점"),back.right()+8,back.y()+5,0xFFF1E8FA);
''',"academy viewport")

quest = '''    private void quests(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {
        Rect c=l.content();
        int count=Math.min(3,ArcaneClientState.integer("quest_count",0));
        sectionTitle(g,l,"의뢰 게시판","고정 난이도·고정 보상");
        Rect viewport=l.questViewport();
        g.enableScissor(viewport.x(),viewport.y(),viewport.right(),viewport.bottom());
        String offered=ArcaneClientState.text("quest_offer_id","");
        Rect offer=l.questOffer(scroll);
        g.fill(offer.x(),offer.y(),offer.right(),offer.bottom(),0xFF111A2A);
        g.fill(offer.x(),offer.y(),offer.x()+3,offer.bottom(),offered.isBlank()?0xFF4E5563:0xFFFFC65D);
        if(offered.isBlank()){
            g.text(font,Component.literal("새 제안 없음"),offer.x()+9,offer.y()+8,0xFF8F98A8);
            g.text(font,Component.literal(fit("마도사 주민과 대화하면 난이도와 보상을 먼저 확인합니다.",offer.w()-18)),offer.x()+9,offer.y()+24,0xFF9EABC0);
        }else{
            String difficulty=ArcaneClientState.text("quest_offer_difficulty_name","견습");
            int target=ArcaneClientState.integer("quest_offer_target",0);
            long reward=ArcaneClientState.longInteger("quest_offer_reward",0);
            String desc=ArcaneClientState.text("quest_offer_desc","마도 의뢰");
            MagicTradition issuer=MagicTradition.parse(ArcaneClientState.text("quest_offer_affiliation","UNBOUND"));
            g.text(font,Component.literal(fit("["+difficulty+"] "+desc,offer.w()-190)),offer.x()+9,offer.y()+7,0xFFFFE0A0);
            g.text(font,Component.literal(fit("목표 "+target+" · "+reward+" A · "+issuer.displayName(),offer.w()-190)),offer.x()+9,offer.y()+24,0xFFFFC967);
            button(g,l.questAccept(scroll),"수락",inside(mouseX,mouseY,l.questAccept(scroll)),true);
            button(g,l.questReject(scroll),"거절",inside(mouseX,mouseY,l.questReject(scroll)),true);
        }
        int startY=l.questListY(scroll);
        if(count==0){
            Rect empty=new Rect(c.x(),startY,c.w(),34);
            g.fill(empty.x(),empty.y(),empty.right(),empty.bottom(),0xFF0F1724);
            g.fill(empty.x(),empty.y(),empty.x()+3,empty.bottom(),0xFF343945);
            g.text(font,Component.literal("진행 중인 의뢰 없음 · 최대 3개"),empty.x()+9,empty.y()+12,0xFF697483);
            g.disableScissor();
            return;
        }
        for(int i=0;i<count;i++){
            Rect card=l.questCard(i,scroll);
            int progress=ArcaneClientState.integer("quest_"+i+"_progress",0);
            int target=Math.max(1,ArcaneClientState.integer("quest_"+i+"_target",1));
            long reward=ArcaneClientState.longInteger("quest_"+i+"_reward",0);
            String diff=ArcaneClientState.text("quest_"+i+"_difficulty_name","견습");
            String desc=ArcaneClientState.text("quest_"+i+"_desc","마도 의뢰");
            boolean complete=progress>=target;
            g.fill(card.x(),card.y(),card.right(),card.bottom(),0xFF101827);
            g.fill(card.x(),card.y(),card.x()+3,card.bottom(),complete?0xFFFFC65D:0xFF7560A2);
            int textW=card.w()-105;
            g.text(font,Component.literal(fit((i+1)+". ["+diff+"] "+desc,textW)),card.x()+9,card.y()+6,complete?0xFFFFD36B:0xFFE9E0F1);
            g.text(font,Component.literal(fit(progress+"/"+target+" · "+reward+" A",textW)),card.x()+9,card.y()+21,complete?0xFFFFC65D:0xFF9FC6E8);
            g.fill(card.x()+9,card.y()+36,card.x()+9+Math.max(1,textW-8),card.y()+40,0xFF293244);
            int fill=(int)Math.round(Math.max(0,textW-8)*Math.min(1.0,progress/(double)target));
            g.fill(card.x()+9,card.y()+36,card.x()+9+fill,card.y()+40,complete?0xFFFFC65D:0xFF7569C2);
            if(complete)button(g,l.questClaim(i,scroll),"보상 수령",inside(mouseX,mouseY,l.questClaim(i,scroll)),true);
        }
        g.disableScissor();
    }

'''
between("    private void quests(GuiGraphicsExtractor g, Layout l, int mouseX, int mouseY) {\n",
        "    private void core(GuiGraphicsExtractor g, Layout l) {\n",quest,"quest viewport")

core='''    private void core(GuiGraphicsExtractor g, Layout l) {
        sectionTitle(g,l,"마력핵","");
        Rect c=l.content();
        Rect viewport=l.coreViewport();
        g.enableScissor(viewport.x(),viewport.y(),viewport.right(),viewport.bottom());
        int circle=ArcaneClientState.integer("circle",1);
        int y=viewport.y()-scroll;
        int iconX=c.x()+28;
        int iconY=y+42;
        ArcaneRenderUtil.ring(g,iconX,iconY,18,0xFF9C6ED0);
        ArcaneRenderUtil.diamond(g,iconX,iconY,8,0xFFEAD9FF);
        g.centeredText(font,Component.literal(circle+"C"),iconX,iconY-4,0xFFFFFFFF);
        List<String> status=List.of(
                "MP "+ArcaneClientState.integer("mana",0)+"/"+ArcaneClientState.integer("max",100),
                "회복 "+String.format("%.1f",ArcaneClientState.regenPerSecond())+"/초",
                "통찰 "+ArcaneClientState.integer("insight",0),
                "아르카나 "+ArcaneClientState.longInteger("marks",0L));
        MagicTradition tradition=MagicTradition.parse(ArcaneClientState.text("tradition","UNBOUND"));
        List<String> gear=List.of(
                ArcaneClientState.text("staff","맨손"),
                ArcaneClientState.text("gear_hat","모자 없음"),
                ArcaneClientState.text("gear_robe","로브 없음"),
                ArcaneClientState.text("gear_boots","마도화 없음"),
                "소속 "+tradition.displayName(),
                "강점 "+tradition.strength(),
                "약점 "+tradition.weakness(),
                "위험지대 "+ArcaneClientState.text("zones","미탐지").replace("|"," · "));
        if(c.w()>=520){
            int firstX=c.x()+58;
            int firstW=Math.min(190,(c.w()-66)/2);
            infoPanel(g,firstX,y,firstW,"상태",status);
            int secondX=firstX+firstW+8;
            infoPanel(g,secondX,y,Math.max(120,c.right()-secondX),"장비 / 소속",gear);
        }else{
            infoPanel(g,c.x()+58,y,Math.max(120,c.w()-58),"상태",status);
            infoPanel(g,c.x(),y+100,c.w(),"장비 / 소속",gear);
        }
        g.disableScissor();
    }

'''
between("    private void core(GuiGraphicsExtractor g, Layout l) {\n",
        "    private void infoPanel(GuiGraphicsExtractor g, int x, int y, int w, String title, List<String> lines) {\n",
        core,"core responsive")

once('        g.text(font, Component.literal("구중 마도서"), l.left() + 14, l.top() + 13, 0xFFF2E8FA);\n',
     '        if(l.panelW()>=520)g.text(font, Component.literal("구중 마도서"), l.left() + 14, l.top() + 13, 0xFFF2E8FA);\n',
     "header title")
once('''            case "academy" -> academyCircle == 0 ? 0 : l.maxOfferScroll(AcademyOfferCatalog.forCircle(academyCircle).size());
            case "quests" -> 0;
            case "atlas" -> atlasCircle == 0 ? 0 : l.maxSpellScroll(SpellCatalog.spellsInCircle(atlasCircle).size());
            default -> 0;
''','''            case "academy" -> academyCircle == 0 ? l.maxAcademyCircleScroll(9)
                    : l.maxOfferScroll(AcademyOfferCatalog.forCircle(academyCircle).size());
            case "quests" -> l.maxQuestScroll(Math.min(3, ArcaneClientState.integer("quest_count", 0)));
            case "atlas" -> atlasCircle == 0 ? 0 : l.maxSpellScroll(SpellCatalog.spellsInCircle(atlasCircle).size());
            case "core" -> l.maxCoreScroll();
            default -> 0;
''',"scroll bounds")
once('        Rect tab(int i){int w=58;return new Rect(cx()-TABS.size()*w/2+i*w,top+5,w,25);}\n',
'''        Rect tab(int i){
            int start=panelW>=520?left+120:left+6;
            int end=right()-34;
            int w=Math.max(36,Math.min(70,(end-start)/TABS.size()));
            int total=w*TABS.size();
            int x=start+Math.max(0,(end-start-total)/2)+i*w;
            return new Rect(x,top+5,w,25);
        }
''',"responsive tabs")

old='''        Rect tradition(int i){Rect c=content();int gap=4;int w=(c.w()-gap*3)/4;return new Rect(c.x()+i*(w+gap),c.y()+20,w,20);}
        Rect traditionDetail(){Rect c=content();return new Rect(c.x(),c.y()+44,c.w(),78);}
        Rect traditionJoin(){Rect d=traditionDetail();return new Rect(d.right()-108,d.bottom()-27,100,21);}
        Rect academyCircleCard(int circle){Rect c=content();int cols=c.w()>=420?9:3,gap=4;int w=(c.w()-gap*(cols-1))/cols;int col=(circle-1)%cols,row=(circle-1)/cols;int h=34;return new Rect(c.x()+col*(w+gap),c.y()+136+row*(h+gap),w,h);}
        Rect academyBack(){Rect c=content();return new Rect(c.x(),c.y()+133,66,19);}
        Rect academyViewport(){Rect c=content();return new Rect(c.x(),c.y()+158,c.w(),Math.max(20,c.h()-159));}
        Rect offerCard(int i,int scroll){Rect v=academyViewport();int cols=v.w()>=540?4:2,gap=5;int w=(v.w()-gap*(cols-1))/cols;int row=i/cols,col=i%cols;return new Rect(v.x()+col*(w+gap),v.y()+row*43-scroll,w,38);}
        int maxOfferScroll(int count){Rect v=academyViewport();int cols=v.w()>=540?4:2;return Math.max(0,((count+cols-1)/cols)*43-v.h());}

        Rect questOffer(){Rect c=content();return new Rect(c.x(),c.y()+28,c.w(),48);}
        Rect questAccept(){Rect r=questOffer();return new Rect(r.right()-166,r.y()+13,78,21);}
        Rect questReject(){Rect r=questOffer();return new Rect(r.right()-82,r.y()+13,78,21);}
        int questListY(){return content().y()+84;}
        Rect questCard(int i){Rect c=content();return new Rect(c.x(),questListY()+i*50,c.w(),44);}
        Rect questClaim(int i){Rect r=questCard(i);return new Rect(r.right()-92,r.y()+10,82,23);}
'''
new='''        Rect tradition(int i){Rect c=content();int gap=4;int w=(c.w()-gap*3)/4;return new Rect(c.x()+i*(w+gap),c.y()+20,w,20);}
        Rect traditionDetail(){Rect c=content();return new Rect(c.x(),c.y()+44,c.w(),78);}
        Rect traditionJoin(){Rect d=traditionDetail();return new Rect(d.right()-108,d.bottom()-27,100,21);}
        Rect academyCircleViewport(){Rect c=content();Rect d=traditionDetail();int y=d.bottom()+6;return new Rect(c.x(),y,c.w(),Math.max(18,c.bottom()-y));}
        Rect academyCircleCard(int circle,int scroll){Rect v=academyCircleViewport();int cols=v.w()>=620?9:v.w()>=430?6:3,gap=4;int w=(v.w()-gap*(cols-1))/cols;int col=(circle-1)%cols,row=(circle-1)/cols;int h=34;return new Rect(v.x()+col*(w+gap),v.y()+row*(h+4)-scroll,w,h);}
        int maxAcademyCircleScroll(int count){Rect v=academyCircleViewport();int cols=v.w()>=620?9:v.w()>=430?6:3;int rows=(count+cols-1)/cols;return Math.max(0,rows*38-v.h());}
        Rect academyBack(){Rect c=content();Rect d=traditionDetail();return new Rect(c.x(),d.bottom()+6,66,19);}
        Rect academyViewport(){Rect c=content();Rect b=academyBack();int y=b.bottom()+6;return new Rect(c.x(),y,c.w(),Math.max(18,c.bottom()-y));}
        Rect offerCard(int i,int scroll){Rect v=academyViewport();int cols=v.w()>=540?4:2,gap=5;int w=(v.w()-gap*(cols-1))/cols;int row=i/cols,col=i%cols;return new Rect(v.x()+col*(w+gap),v.y()+row*43-scroll,w,38);}
        int maxOfferScroll(int count){Rect v=academyViewport();int cols=v.w()>=540?4:2;return Math.max(0,((count+cols-1)/cols)*43-v.h());}

        Rect questViewport(){Rect c=content();return new Rect(c.x(),c.y()+24,c.w(),Math.max(18,c.h()-24));}
        Rect questOffer(int scroll){Rect c=content();return new Rect(c.x(),c.y()+28-scroll,c.w(),48);}
        Rect questAccept(int scroll){Rect r=questOffer(scroll);return new Rect(r.right()-166,r.y()+13,78,21);}
        Rect questReject(int scroll){Rect r=questOffer(scroll);return new Rect(r.right()-82,r.y()+13,78,21);}
        int questListY(int scroll){return content().y()+84-scroll;}
        Rect questCard(int i,int scroll){Rect c=content();return new Rect(c.x(),questListY(scroll)+i*50,c.w(),44);}
        Rect questClaim(int i,int scroll){Rect r=questCard(i,scroll);return new Rect(r.right()-92,r.y()+10,82,23);}
        int maxQuestScroll(int count){Rect v=questViewport();return Math.max(0,84+Math.max(1,count)*50-v.h());}

        Rect coreViewport(){Rect c=content();return new Rect(c.x(),c.y()+24,c.w(),Math.max(18,c.h()-24));}
        int maxCoreScroll(){Rect c=content();Rect v=coreViewport();int contentHeight=c.w()>=520?150:242;return Math.max(0,contentHeight-v.h());}
'''
once(old,new,"layout responsive scroll")
print("Arcane Circle alpha.20 UI migration: PASS")
