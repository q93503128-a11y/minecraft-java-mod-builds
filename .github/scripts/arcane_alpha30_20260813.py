from pathlib import Path

repo = Path(__file__).resolve().parents[2]
root = repo / 'projects/arcane-circle'
client = root / 'src/main/java/kr/moonseungjun/arcanecircle/client'
magic = root / 'src/main/java/kr/moonseungjun/arcanecircle/magic'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, value):
    path.write_text(value, encoding='utf-8')


def replace_once(path, old, new):
    text = read(path)
    if old not in text:
        raise SystemExit(f'missing replacement anchor in {path}: {old[:120]!r}')
    if text.count(old) != 1:
        raise SystemExit(f'non-unique replacement anchor in {path}: {old[:120]!r}')
    write(path, text.replace(old, new, 1))


def replace_block(path, start, end, new_block):
    text = read(path)
    i = text.find(start)
    if i < 0:
        raise SystemExit(f'missing block start in {path}: {start!r}')
    j = text.find(end, i + len(start))
    if j < 0:
        raise SystemExit(f'missing block end in {path}: {end!r}')
    write(path, text[:i] + new_block + text[j:])


# Version bump.
replace_once(root / 'gradle.properties', 'mod_version=0.12.1-alpha.29', 'mod_version=0.12.1-alpha.30')
replace_once(root / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
             'VERSION = "0.12.1-alpha.29"', 'VERSION = "0.12.1-alpha.30"')
replace_once(root / 'src/main/resources/data/arcanecircle/spell_catalog/index.json',
             '"version": "0.12.1-alpha.29"', '"version": "0.12.1-alpha.30"')

# Grimoire: layout owns every region; text no longer competes with slot geometry.
g = client / 'GrimoireScreen.java'
replace_once(g,
'''/**
 * Alpha.26 grimoire.  It is intentionally not a dashboard of cards: navigation lives on the book
 * spine, circles are an index, spell browsing is icon-first, and one selected subject owns the
 * reading page.  All gameplay data and packet actions are the same as before.
 */''',
'''/**
 * Responsive spellbook presentation. Navigation, circle rail, browser, detail reader and loadout
 * dock each own a non-overlapping region. Long explanatory text is clipped/wrapped inside its
 * reader instead of being allowed to collide with interaction controls.
 */''')
replace_once(g,
             'private static String inspectedSpellId="",selectedStaffId="";',
             'private static String inspectedSpellId="",selectedStaffId="",equipCandidateId="";')
replace_once(g,
             'default -> clickAtlas(event,l)||super.mouseClicked(event,doubleClick);',
             'default -> clickAtlas(event,l,doubleClick)||super.mouseClicked(event,doubleClick);')

new_click_atlas = '''    private boolean clickAtlas(MouseButtonEvent e,Layout l,boolean doubleClick){
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

'''
replace_block(g,
              '    private boolean clickAtlas(MouseButtonEvent e,Layout l){',
              '    private boolean clickRecipes',
              new_click_atlas)

replace_once(g,
'''        title(g,b,"주문 도감",atlasCircle+"써클 · "+circleSubtitle(atlasCircle));
        drawCircleIndex(g,l,atlasCircle,mouseX,mouseY,false);''',
'''        title(g,b,"주문 도감",atlasCircle+"써클 · "+circleSubtitle(atlasCircle));
        if(b.w()>360)tiny(g,"주문↔슬롯 양방향 장착 · 더블클릭 빠른 장착",b.right()-4,b.y()+15,0xFF746D64,.48F,true);
        drawCircleIndex(g,l,atlasCircle,mouseX,mouseY,false);''')

replace_once(g,
'''        boolean usable=ArcaneClientState.known().contains(s.id())&&s.circle()<=ArcaneClientState.integer("circle",1);
        String label=activeSlot<0?"아래에서 장착 슬롯 선택":usable?(activeSlot+1)+"번 슬롯에 각인":"습득 필요";''',
'''        boolean usable=usable(s);int empty=firstEmptySlot();
        String label=!usable?"습득 필요":activeSlot>=0?(activeSlot+1)+"번 슬롯에 장착":empty>=0?"빠른 장착 · "+(empty+1)+"번":"교체할 슬롯을 클릭";''')
replace_once(g,
             'action(g,a,label,inside(mouseX,mouseY,a),usable&&activeSlot>=0,accent);',
             'action(g,a,label,inside(mouseX,mouseY,a),usable&&(activeSlot>=0||firstEmptySlot()>=0),accent);')
# The compact branch has its own action call.
replace_once(g,
             'action(g,a,label,inside(mouseX,mouseY,a),usable&&activeSlot>=0,accent);\n            g.disableScissor();return;',
             'action(g,a,label,inside(mouseX,mouseY,a),usable&&(activeSlot>=0||firstEmptySlot()>=0),accent);\n            g.disableScissor();return;')

new_loadout = '''    private void drawLoadout(GuiGraphicsExtractor g,Rect r,int slot,int mouseX,int mouseY){
        SpellDefinition s=SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null);boolean active=activeSlot==slot,hover=inside(mouseX,mouseY,r);int accent=s==null?0xFF625D55:ArcaneRenderUtil.schoolColor(s.school());
        int cx=r.x()+r.w()/2,cy=r.y()+r.h()/2,rad=Math.max(5,Math.min(9,Math.min(r.w(),r.h())/2-2));
        if(active){ArcaneRenderUtil.ring(g,cx,cy,rad+2,0xFFFFD275);ArcaneRenderUtil.diamond(g,cx,cy,rad,0x26FFD275);}else ArcaneRenderUtil.ring(g,cx,cy,rad+1,hover?accent:0xFF514C46);
        if(s!=null)ArcaneRenderUtil.spellRune(g,cx,cy,s,Math.max(3,rad-3),active?0xFFFFE6AA:0xFFEAE0D1);else ArcaneRenderUtil.diamond(g,cx,cy,Math.max(2,rad-4),0xFF655F57);
        tiny(g,Integer.toString(slot+1),r.x()+3,r.y()+2,active?0xFFFFD584:0xFF8B8378,.50F,false);
        if(s!=null&&ArcaneClientState.cooldownRemainingTicks(slot)>0)g.fill(r.x()+2,r.bottom()-2,r.x()+2+(int)((r.w()-4)*ArcaneClientState.cooldownFraction(slot)),r.bottom()-1,0xFFB34D52);
    }

'''
replace_block(g,
              '    private void drawLoadout(GuiGraphicsExtractor g,Rect r,int slot,int mouseX,int mouseY){',
              '    private void drawRecipes',
              new_loadout)

new_staff_detail = '''    private void drawStaffDetail(GuiGraphicsExtractor g,Layout l,String id){
        Rect d=l.detail();if(d.w()<70||d.h()<45)return;g.enableScissor(d.x(),d.y(),d.right(),d.bottom());g.fill(d.x(),d.y()+4,d.x()+1,d.bottom()-4,0xFF554838);StaffProfile p=id.isBlank()?StaffProfile.NONE:ModItems.profile(id);int cx=d.x()+d.w()/2;int iconR=Math.max(9,Math.min(18,d.h()/7));ArcaneRenderUtil.diamond(g,cx,d.y()+iconR+7,iconR,p==StaffProfile.NONE?0xFF5D564D:0xFFD2AE70);g.centeredText(font,Component.literal(fit(p.displayName(),Math.max(40,d.w()-8))),cx,d.y()+iconR*2+13,0xFFEADCC7);int y=d.y()+iconR*2+30,remaining=Math.max(0,d.bottom()-y-4),lines=Math.max(0,Math.min(4,remaining/9));for(String line:wrap(p.summary(),d.w()-18,lines)){tiny(g,line,d.x()+9,y,0xFFAFA496,.60F,false);y+=9;}if(y+8<d.bottom()){y+=5;int recipeLines=Math.max(0,Math.min(3,(d.bottom()-y-3)/9));for(String line:wrap(p.recipeHint().isBlank()?"제작 정보 없음":"제작 · "+p.recipeHint(),d.w()-18,recipeLines)){tiny(g,line,d.x()+9,y,0xFFD0B789,.56F,false);y+=9;}}g.disableScissor();
    }

'''
replace_block(g,
              '    private void drawStaffDetail(GuiGraphicsExtractor g,Layout l,String id){',
              '    private void drawAcademy',
              new_staff_detail)

new_core = '''    private void drawCore(GuiGraphicsExtractor g,Layout l){
        Rect b=l.body();title(g,b,"마력핵","현재 마도사 상태");Rect v=l.viewport();if(v.w()<44||v.h()<38)return;g.enableScissor(v.x(),v.y(),v.right(),v.bottom());
        int circle=ArcaneClientState.integer("circle",1),accent=circleColor(circle);int topH=Math.max(56,Math.min(108,(int)Math.round(v.h()*.58)));int sealZone=Math.max(58,Math.min(145,v.w()/3));int sealR=Math.max(16,Math.min(34,Math.min(sealZone/3,topH/3)));int cx=v.x()+sealZone/2,cy=v.y()+topH/2;
        ArcaneRenderUtil.ring(g,cx,cy,sealR,accent);ArcaneRenderUtil.ring(g,cx,cy,Math.max(10,sealR-8),0xFF695942);ArcaneRenderUtil.diamond(g,cx,cy,Math.max(7,sealR-18),0xFFE9D5AC);g.centeredText(font,Component.literal(circle+"C"),cx,cy-4,0xFF251B10);
        int sx=v.x()+sealZone+6,sy=v.y()+7,statW=Math.max(38,v.right()-sx-4);List<String> stats=List.of("MP  "+ArcaneClientState.integer("mana",0)+" / "+ArcaneClientState.integer("max",100),"회복  "+one(ArcaneClientState.regenPerSecond())+" /초","통찰  "+ArcaneClientState.integer("insight",0),"아르카나  "+ArcaneClientState.longInteger("marks",0));int statStep=Math.max(11,Math.min(16,(topH-12)/4));for(String s:stats){tiny(g,fit(s,statW*2),sx,sy,0xFFD8CCBA,.62F,false);sy+=statStep;}
        int gearTop=v.y()+topH;if(gearTop<v.bottom()-11){rule(g,gearTop,v.x()+2,v.right()-2,0xFF4B4033);tiny(g,"장비 · 소속",v.x()+3,gearTop+4,0xFF827A70,.50F,false);gearTop+=14;MagicTradition t=MagicTradition.parse(ArcaneClientState.text("tradition","UNBOUND"));List<String> gear=List.of("지팡이 · "+ArcaneClientState.text("staff","맨손"),"모자 · "+ArcaneClientState.text("gear_hat","없음"),"로브 · "+ArcaneClientState.text("gear_robe","없음"),"마도화 · "+ArcaneClientState.text("gear_boots","없음"),"소속 · "+t.displayName());int cols=v.w()>=235?2:1,rows=(gear.size()+cols-1)/cols,gap=7,colW=Math.max(35,(v.w()-gap*(cols-1))/cols),available=Math.max(1,v.bottom()-gearTop-2),rowH=Math.max(8,Math.min(12,available/Math.max(1,rows)));for(int i=0;i<gear.size();i++){int row=i/cols,col=i%cols,y=gearTop+row*rowH;if(y>=v.bottom()-5)break;int x=v.x()+col*(colW+gap);tiny(g,fit(gear.get(i),colW*2),x,y,0xFF9F9587,.54F,false);}}
        g.disableScissor();
    }

'''
replace_block(g,
              '    private void drawCore(GuiGraphicsExtractor g,Layout l){',
              '    private void title',
              new_core)

new_equip_helpers = '''    private boolean usable(SpellDefinition s){return s!=null&&ArcaneClientState.known().contains(s.id())&&s.circle()<=ArcaneClientState.integer("circle",1);}
    private int firstEmptySlot(){for(int i=0;i<5;i++)if(ArcaneClientState.slot(i).isBlank())return i;return -1;}
    private void equipTo(SpellDefinition s,int slot){if(slot<0||slot>=5){notice("잘못된 장착 슬롯입니다");return;}if(!usable(s)){notice(s==null?"장착할 주문이 없습니다":"아직 사용할 수 없는 주문입니다");return;}ClientPacketDistributor.sendToServer(new EquipSpellPayload(s.id(),slot));notice((slot+1)+"번 슬롯 · "+s.name());}
    private void quickEquip(SpellDefinition s){if(!usable(s)){notice("아직 사용할 수 없는 주문입니다");return;}int slot=firstEmptySlot();if(slot<0){notice("빈 슬롯 없음 · 교체할 슬롯을 먼저 클릭하세요");return;}equipTo(s,slot);equipCandidateId="";}
'''
replace_block(g,
              '    private void equip(SpellDefinition s){',
              '    private void request',
              new_equip_helpers + '    private void request')

# Replace the Layout tail completely so every region is derived from the same safe content rectangle.
layout_start = '    private record Layout(int left,int top,int panelW,int panelH){'
text = read(g)
i = text.find(layout_start)
if i < 0:
    raise SystemExit('missing Grimoire Layout record')
new_layout = '''    private record Layout(int left,int top,int panelW,int panelH){
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
'''
write(g, text[:i] + new_layout)

# Sigils: high-complexity formulas occupy multiple geometric planes instead of becoming denser flat line art.
s = client / 'ArcaneSigilDirector.java'
replace_once(s, 'private static final int BUDGET = 1400;', 'private static final int BUDGET = 2200;')
replace_once(s,
'''        formulaFrame(mesh, spell, profile, primary, radius, p, rotation, seed);
        schoolFormula(mesh, spell, primary, radius, p, rotation, seed);
        anchorFormula(mesh, spell, profile, primary, direction, targetOffset, radius, p, rotation, seed);''',
'''        formulaFrame(mesh, spell, profile, primary, radius, p, rotation, seed);
        schoolFormula(mesh, spell, primary, radius, p, rotation, seed);
        geometricDepth(mesh, spell, profile, primary, radius, p, rotation, seed);
        anchorFormula(mesh, spell, profile, primary, direction, targetOffset, radius, p, rotation, seed);''')

insert_before = '    private static double sigilRangeScale(SpellDefinition spell, SpellPresentationProfile.Profile profile, double range) {'
text = read(s)
pos = text.find(insert_before)
if pos < 0:
    raise SystemExit('missing sigilRangeScale insertion point')
geometric_depth = '''    private static void geometricDepth(ArcaneWorldMesh.Builder m,SpellDefinition spell,SpellPresentationProfile.Profile profile,ArcaneWorldMesh.Basis b,double r,double p,double rotation,int seed){
        int detail=profile.complexity();if(detail<4||p<.36)return;Vec3 n=b.normal();double depth=r*(detail>=6?.13:.085)*(.45+.55*p);int sides=5+Math.floorMod(seed,4);
        m.polygon(b,n.scale(depth),r*.49,sides,rotation*.21,.82F);m.polygon(b,n.scale(-depth),r*.37,sides+1,-rotation*.17+.23,.62F);
        for(int i=0;i<6;i++){double a=rotation*.08+i*Math.PI/3.0;Vec3 top=b.point(a,r*.49).add(n.scale(depth)),bottom=b.point(a+.18,r*.37).add(n.scale(-depth));m.line(top,bottom,i%3==0?.86F:.48F);}
        switch(profile.sigil()){
            case FRONT_COMPACT,FRONT_LANCE -> {for(int i=1;i<=2;i++){double d=depth*(.45+i*.55);m.circle(b,n.scale(d),r*(.28-i*.055),30+i*8,i==1?.86F:.58F);}if(detail>=6)m.helix(n.scale(-depth*1.35),n,b,depth*2.7,r*.19,2,34,.48F,false);}
            case GROUND_SEAL,QUAD_ARRAY,SKY_RITUAL -> {ArcaneWorldMesh.Basis x=ArcaneWorldMesh.Basis.fromNormal(b.right(),b.up()),z=ArcaneWorldMesh.Basis.fromNormal(b.up(),b.right());m.brokenBand(x,Vec3.ZERO,r*.27,r*.31,38,5,.82F,.10F);m.brokenBand(z,Vec3.ZERO,r*.34,r*.38,42,6,.72F,.09F);if(detail>=6){m.circle(x,Vec3.ZERO,r*.48,46,.58F);m.circle(z,Vec3.ZERO,r*.54,50,.52F);}}
            case TARGET_SEAL -> {ArcaneWorldMesh.Basis x=ArcaneWorldMesh.Basis.fromNormal(b.right(),b.up());m.brokenBand(x,Vec3.ZERO,r*.35,r*.40,40,4,.86F,.11F);for(int i=0;i<4;i++){double a=Math.PI/4+i*Math.PI/2;Vec3 c=b.point(a,r*.58);m.line(c.add(n.scale(-depth)),c.add(n.scale(depth)),.72F);}}
            case BODY_HALO,FEET_RUNE -> {ArcaneWorldMesh.Basis x=ArcaneWorldMesh.Basis.fromNormal(b.right(),b.up());m.circle(x,Vec3.ZERO,r*.46,42,.68F);if(detail>=6)m.brokenBand(x,n.scale(depth*.25),r*.57,r*.62,46,5,.72F,.10F);}
            case WALL_MATRIX -> {Vec3 up=b.up(),right=b.right();double w=r*.52,h=r*.36;for(int layer=-1;layer<=1;layer++){Vec3 o=n.scale(layer*depth*.62);m.line(o.add(right.scale(-w)).add(up.scale(-h)),o.add(right.scale(w)).add(up.scale(-h)),.55F);m.line(o.add(right.scale(-w)).add(up.scale(h)),o.add(right.scale(w)).add(up.scale(h)),.55F);}}
            case PORTAL_GATE -> {if(detail>=5){ArcaneWorldMesh.Basis x=ArcaneWorldMesh.Basis.fromNormal(b.right(),b.up());m.circle(x,Vec3.ZERO,r*.34,38,.60F);m.runeGlyph(b,n.scale(depth*.72),r*.12,seed^0x71A5,rotation*.31,.76F);}}
        }
        if(detail>=6){for(int i=0;i<6;i++){double a=i*Math.PI/3.0-rotation*.05;Vec3 c=b.point(a,r*.66).add(n.scale((i%2==0?1:-1)*depth*.55));m.runeGlyph(b,c,r*.065,seed+i*97,-a,.58F);}}
    }

'''
write(s, text[:pos] + geometric_depth + text[pos:])

old_meteor_start = '    private static void meteorRitual(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b,'
old_meteor_end = '    private static void skyRitual'
new_meteor = '''    private static void meteorRitual(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b,
                                     double r, double p, double rotation, int seed) {
        double outer=r*(.78+.22*p),inner=outer*.70,depth=Math.max(1.1,r*.095);Vec3 n=b.normal();
        m.brokenBand(b,Vec3.ZERO,outer*.94,outer,104,7,1.32F,.17F);m.circle(b,Vec3.ZERO,outer*.87,88,.72F);m.runeRing(b,Vec3.ZERO,outer*.91,16,r*.043,seed,-rotation*.16,.68F);
        m.polygon(b,Vec3.ZERO,inner*.72,8,rotation*.10,1.02F);m.polygon(b,Vec3.ZERO,inner*.54,4,-rotation*.13+Math.PI/4.0,.82F);m.runeChords(b,Vec3.ZERO,inner*.39,8,3,rotation*.07,.64F);m.circle(b,Vec3.ZERO,inner*.20,36,1.15F);
        m.brokenBand(b,n.scale(depth),inner*.43,inner*.48,52,5,.88F,.10F);m.brokenBand(b,n.scale(-depth*.72),inner*.28,inner*.33,44,4,.76F,.09F);
        ArcaneWorldMesh.Basis armA=ArcaneWorldMesh.Basis.fromNormal(b.right(),b.up()),armB=ArcaneWorldMesh.Basis.fromNormal(b.up(),b.right());m.circle(armA,Vec3.ZERO,inner*.40,48,.64F);m.circle(armB,Vec3.ZERO,inner*.47,52,.58F);
        double orbit=outer*.64,child=outer*.14;for(int i=0;i<4;i++){double a=Math.PI/4.0+i*Math.PI/2.0+rotation*.025;Vec3 c=b.point(a,orbit),upper=c.add(n.scale(depth*.72)),lower=c.add(n.scale(-depth*.55)),rail=b.point(a,inner*.76);m.line(rail,upper,i%2==0?1.08F:.74F);m.brokenBand(b,upper,child*.78,child,34,4,1.18F,.14F);m.polygon(b,upper,child*.64,4,-a+Math.PI/4.0,.84F);m.runeGlyph(b,upper,child*.40,seed+i*101,-rotation*.18,.80F);m.circle(b,lower,child*.58,26,.58F);m.line(upper,lower,1.16F);m.line(lower,lower.add(n.scale(-Math.max(2.0,r*.12))),1.28F);for(int q=0;q<4;q++){double qa=q*Math.PI/2.0;Vec3 node=upper.add(b.point(qa,child*.82));m.line(node,node.add(n.scale(-depth*.9)),.46F);}}
        if(p>.78){for(int i=0;i<8;i++){double a=i*Math.PI/4.0+rotation*.045;Vec3 c=b.point(a,outer*1.04);m.runeGlyph(b,c,r*.050,seed^i*131,a,.58F);}}
    }

'''
replace_block(s, old_meteor_start, old_meteor_end, new_meteor)

# Current-source audit now protects the responsive geometry and the faster equipment interaction.
test = root / 'tools/test_current_source.py'
replace_once(test, "assert 'mod_version=0.12.1-alpha.29' in gradle", "assert 'mod_version=0.12.1-alpha.30' in gradle")
replace_once(test, "assert 'VERSION = \"0.12.1-alpha.29\"' in main", "assert 'VERSION = \"0.12.1-alpha.30\"' in main")
replace_once(test, "assert '\"version\": \"0.12.1-alpha.29\"' in index", "assert '\"version\": \"0.12.1-alpha.30\"' in index")
replace_once(test,
             "for token in ['formulaFrame','schoolFormula','anchorFormula','skyRitual','meteorRitual','sigilRangeScale','meteor_swarm','runeRing','brokenBand','fusionFormula']:",
             "for token in ['formulaFrame','schoolFormula','geometricDepth','anchorFormula','skyRitual','meteorRitual','sigilRangeScale','meteor_swarm','runeRing','brokenBand','helix','fusionFormula']:")
replace_once(test,
             "for token in ['drawSpine','circleIndex','circleStep','compact()','browserViewport','detailWidth','detail()','spellTile','primaryAction','drawLoadout','enableScissor','mouseScrolled']:",
             "for token in ['drawSpine','circleIndex','circleStep','contentBottom','compact()','browserViewport','detailWidth','detail()','spellTile','primaryAction','drawLoadout','equipCandidateId','firstEmptySlot','quickEquip','enableScissor','mouseScrolled']:")

# Documentation: keep the product rule, not implementation trivia.
changelog = root / 'CHANGELOG.md'
replace_once(changelog, '# Changelog\n\n', '''# Changelog\n\n## 0.12.1-alpha.30\n- 마도서 화면을 header/content/footer 소유권 기반 레이아웃으로 다시 나눴다. 1~9써클 레일은 장착 도크 위의 실제 남은 높이만 사용하고, 상세/서고/마력핵 설명은 각 viewport에서 scissor 처리한다.\n- 하단 장착 도크에서 긴 주문명을 제거하고 인장·번호·쿨다운 상태만 표시해 작은 GUI 해상도에서도 텍스트가 다른 슬롯을 침범하지 않는다.\n- 장착 UX를 양방향으로 바꿨다. 주문을 고른 뒤 슬롯을 누르거나 슬롯을 고른 뒤 주문을 누르면 즉시 장착되며, 주문 더블클릭은 첫 빈 슬롯에 빠르게 장착한다.\n- 마력핵 화면을 고정 세로 목록에서 상단 핵/스탯 + 하단 1~2열 장비·소속 구조로 바꿔 모든 설명을 화면 내부에 제한한다.\n- 4~6단계 복잡도의 마법진에 앞뒤 깊이 평면, 직교 환, 연결축, 축방향 렌즈와 룬 노드를 추가했다. 고써클 복잡도는 평면 선 밀도가 아니라 3D 기하 위계로 증가한다.\n- Meteor Swarm 의식을 상·하층 계산환, 두 직교 천구환, 네 독립 낙하지정 회로와 수직 강하축으로 재구성해 중앙 선뭉침을 제거했다.\n\n''')
project = root / 'PROJECT.md'
replace_once(project, '## Alpha.29 runtime contracts', '## Alpha.30 runtime contracts')
replace_once(project,
             '- Grimoire layout must fit all six spine tabs and all nine circle indices at high GUI scale without overlap.',
             '- Grimoire layout assigns header/content/footer ownership; circle rail, detail reader and loadout dock may never overlap even at high GUI scale.\n- Spell equipment supports spell→slot, slot→spell and double-click-to-first-empty-slot flows without an extra confirmation button.')
replace_once(project,
             '- Canonical Java 25 verification is required after the alpha.29 corrective source commit; source-only audit is not sufficient.',
             '- High-complexity sigils gain depth through orthogonal planes, axial structures and role-specific sub-arrays rather than flat line density.\n- Canonical Java 25 verification is required after the alpha.30 source commit; source-only audit is not sufficient.')
readme = root / 'README.md'
replace_once(readme,
             '- `GrimoireScreen`: 기능 인덱스 + 1~9써클 인장 + 주문 브라우저 + 선택 주문 상세 + 장착 스트립',
             '- `GrimoireScreen`: 기능 인덱스 + 안전영역 기반 1~9써클 레일 + 주문 브라우저 + 선택 주문 상세 + 양방향 장착 도크')
replace_once(readme,
             '모든 주문은 시전 단계에서 읽을 수 있는 실제 술식 마법진을 갖는다. 단, 같은 원을 복붙하지 않는다.',
             '마도서 장착은 주문→슬롯, 슬롯→주문 어느 순서로도 가능하고 주문 더블클릭으로 첫 빈 슬롯에 빠르게 장착할 수 있다. 하단 도크는 긴 이름 대신 인장과 상태만 보여 작은 GUI에서도 정보가 충돌하지 않는다.\n\n모든 주문은 시전 단계에서 읽을 수 있는 실제 술식 마법진을 갖는다. 단, 같은 원을 복붙하지 않는다.')
replace_once(readme,
             '학파·주문·앵커에 따라 룬, 보조진, 평면, 3D 깊이와 전개 순서가 달라지고, 높은 써클이라는 이유만으로 무조건 커지지는 않는다.',
             '학파·주문·앵커에 따라 룬, 보조진, 평면, 직교환, 축방향 구조, 3D 깊이와 전개 순서가 달라지고, 높은 써클이라는 이유만으로 무조건 커지지는 않는다.')

# Remove this one-shot machinery before the source audit so the canonical tree stays clean.
(repo / '.github/scripts/arcane_alpha30_20260813.py').unlink(missing_ok=True)
(repo / '.github/workflows/maintenance-arcane30-20260813.yml').unlink(missing_ok=True)

print('Arcane Circle alpha.30 patch applied')
