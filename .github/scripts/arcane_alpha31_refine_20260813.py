from pathlib import Path

repo=Path(__file__).resolve().parents[2]
root=repo/'projects/arcane-circle'
client=root/'src/main/java/kr/moonseungjun/arcanecircle/client'


def read(path): return path.read_text(encoding='utf-8')
def write(path,text): path.write_text(text,encoding='utf-8')

def replace_once(path,old,new):
    text=read(path)
    count=text.count(old)
    if count!=1: raise SystemExit(f'expected one anchor in {path}: {old[:100]!r}; found {count}')
    write(path,text.replace(old,new,1))

def replace_block(path,start,end,new_block):
    text=read(path)
    i=text.find(start)
    if i<0: raise SystemExit(f'missing start in {path}: {start}')
    j=text.find(end,i+len(start))
    if j<0: raise SystemExit(f'missing end in {path}: {end}')
    write(path,text[:i]+new_block+text[j:])

# ---------------- version ----------------
replace_once(root/'gradle.properties','mod_version=0.12.1-alpha.30','mod_version=0.12.1-alpha.31')
replace_once(root/'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java','VERSION = "0.12.1-alpha.30"','VERSION = "0.12.1-alpha.31"')
replace_once(root/'src/main/resources/data/arcanecircle/spell_catalog/index.json','"version": "0.12.1-alpha.30"','"version": "0.12.1-alpha.31"')

# ---------------- fine-line mesh mode ----------------
mesh=client/'ArcaneWorldMesh.java'
replace_once(mesh,
'''    private final List<Segment> segments;
    private final List<Face> faces;
    ArcaneWorldMesh(List<Segment> segments,List<Face> faces){this.segments=List.copyOf(segments);this.faces=List.copyOf(faces);}''',
'''    private final List<Segment> segments;
    private final List<Face> faces;
    private final float lineScale;
    private final float lineFloor;
    ArcaneWorldMesh(List<Segment> segments,List<Face> faces,float lineScale,float lineFloor){this.segments=List.copyOf(segments);this.faces=List.copyOf(faces);this.lineScale=lineScale;this.lineFloor=lineFloor;}''')
replace_once(mesh,
'''float w=Math.max(.72F,s.width*scale);''',
'''float w=Math.max(lineFloor,s.width*scale*lineScale);''')
replace_once(mesh,
'''    static Builder builder(int budget){return new Builder(budget);}''',
'''    static Builder builder(int budget){return new Builder(budget,1.0F,.72F);}
    static Builder fineBuilder(int budget){return new Builder(budget,.46F,.34F);}''')
replace_once(mesh,
'''        private final int budget;private final List<Segment> segments=new ArrayList<>();private final List<Face> faces=new ArrayList<>();
        Builder(int budget){this.budget=Math.max(8,budget);}
        int size(){return segments.size()+faces.size()*2;}boolean full(){return size()>=budget;}ArcaneWorldMesh build(){return new ArcaneWorldMesh(segments,faces);}''',
'''        private final int budget;private final float lineScale,lineFloor;private final List<Segment> segments=new ArrayList<>();private final List<Face> faces=new ArrayList<>();
        Builder(int budget,float lineScale,float lineFloor){this.budget=Math.max(8,budget);this.lineScale=Math.max(.1F,lineScale);this.lineFloor=Math.max(.1F,lineFloor);}
        int size(){return segments.size()+faces.size()*2;}boolean full(){return size()>=budget;}ArcaneWorldMesh build(){return new ArcaneWorldMesh(segments,faces,lineScale,lineFloor);}''')

# ---------------- sigil language ----------------
sigil=client/'ArcaneSigilDirector.java'
replace_once(sigil,'ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(BUDGET);','ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.fineBuilder(BUDGET);')

formula='''    private static void formulaFrame(ArcaneWorldMesh.Builder m, SpellDefinition spell,
                                     SpellPresentationProfile.Profile profile, ArcaneWorldMesh.Basis basis,
                                     double r, double p, double rotation, int seed) {
        int detail=profile.complexity();
        double outer=r*(.985+.010*Math.sin(rotation*1.3));
        // Reference language: thin concentric rules first, ornament second. No giant filled ring.
        m.circle(basis,Vec3.ZERO,outer,92+detail*8,.76F);
        m.circle(basis,Vec3.ZERO,outer*.955,84+detail*7,.42F);
        m.circle(basis,Vec3.ZERO,outer*.875,76+detail*6,.54F);
        if(p>.10){
            for(int i=0;i<10;i++){
                double start=rotation*.08+i*Math.PI*2.0/10.0;
                m.arc(basis,Vec3.ZERO,outer*.925,start,Math.PI*.115,12,.54F);
            }
        }
        if(p>.22){
            inscriptionRing(m,basis,outer*.905,10+detail*3,r*(.020+detail*.0018),seed,-rotation*.10,.50F);
        }
        if(p>.32){
            int sides=switch(spell.school()){
                case FIRE -> 3; case FROST,ARCANE -> 6; case WIND -> 5; case WARD -> 8;
                case LIFE -> 7; case SPACE -> 4;
            };
            double geo=outer*.625;
            m.polygon(basis,Vec3.ZERO,geo,sides,rotation*.10,.62F);
            m.polygon(basis,Vec3.ZERO,geo,sides,rotation*.10+Math.PI/Math.max(3,sides),.48F);
            m.circle(basis,Vec3.ZERO,outer*.535,60+detail*4,.40F);
        }
        if(p>.46){
            m.runeChords(basis,Vec3.ZERO,outer*.405,7+detail,2+Math.floorMod(seed,3),rotation*.055,.42F);
            m.circle(basis,Vec3.ZERO,outer*.235,36,.52F);
            m.runeGlyph(basis,Vec3.ZERO,outer*.135,seed^0x51A7,rotation*.16,.58F);
        }
        if(p>.60&&detail>=4){
            int seals=Math.min(6,4+(detail-4));
            for(int i=0;i<seals;i++){
                double a=rotation*.035+i*Math.PI*2.0/seals;
                Vec3 c=basis.point(a,outer*.735);
                double sr=outer*(.068+(i%2)*.010);
                m.circle(basis,c,sr,22,.48F);
                m.circle(basis,c,sr*.72,18,.34F);
                m.polygon(basis,c,sr*.52,3+i%3,-a+.2,.38F);
                m.runeGlyph(basis,c,sr*.30,seed+i*67,-a,.36F);
            }
        }
        if(p>.76&&detail>=5){
            Vec3 n=basis.normal();double d=Math.min(r*.045,.28+detail*.018);
            m.circle(basis,n.scale(d),outer*.49,48,.34F);
            m.circle(basis,n.scale(-d),outer*.355,42,.30F);
        }
    }

'''
replace_block(sigil,'    private static void formulaFrame(', '    private static void schoolFormula',formula)

# Keep three-dimensional structure as a subtle secondary layer; it must never overpower the readable planar formula.
depth='''    private static void geometricDepth(ArcaneWorldMesh.Builder m,SpellDefinition spell,SpellPresentationProfile.Profile profile,ArcaneWorldMesh.Basis b,double r,double p,double rotation,int seed){
        int detail=profile.complexity();if(detail<5||p<.58)return;Vec3 n=b.normal();double depth=Math.min(r*.060,.34+r*.010);double rr=r*(detail>=7?.31:.26);
        m.circle(b,n.scale(depth),rr,40,.34F);m.circle(b,n.scale(-depth*.75),rr*.78,36,.28F);
        for(int i=0;i<4;i++){double a=Math.PI/4.0+i*Math.PI/2.0;Vec3 a0=b.point(a,rr).add(n.scale(depth)),a1=b.point(a+.20,rr*.78).add(n.scale(-depth*.75));m.line(a0,a1,.30F);}
        switch(profile.sigil()){
            case GROUND_SEAL,QUAD_ARRAY,SKY_RITUAL -> {ArcaneWorldMesh.Basis cross=ArcaneWorldMesh.Basis.fromNormal(b.right(),b.up());m.circle(cross,Vec3.ZERO,r*.205,34,.30F);if(detail>=7)m.circle(cross,n.scale(depth*.25),r*.265,38,.26F);}
            case FRONT_LANCE,PORTAL_GATE -> {if(detail>=6){ArcaneWorldMesh.Basis cross=ArcaneWorldMesh.Basis.fromNormal(b.right(),b.up());m.circle(cross,Vec3.ZERO,r*.18,30,.28F);}}
            case TARGET_SEAL,WALL_MATRIX -> {for(int i=0;i<4;i++){double a=i*Math.PI/2.0;Vec3 c=b.point(a,r*.42);m.line(c.add(n.scale(-depth)),c.add(n.scale(depth)),.28F);}}
            case FRONT_COMPACT,BODY_HALO,FEET_RUNE -> {}
        }
    }

'''
replace_block(sigil,'    private static void geometricDepth(', '    private static double sigilRangeScale',depth)

meteor='''    private static void meteorRitual(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b,
                                     double r, double p, double rotation, int seed) {
        double outer=r*(.84+.16*p),inner=outer*.71;Vec3 n=b.normal();double depth=Math.min(r*.050,.42);
        // Celestial formula inspired by dense astrolabe/alchemical diagrams: many fine layers, no iron-frame octagon.
        m.circle(b,Vec3.ZERO,outer,112,.78F);
        m.circle(b,Vec3.ZERO,outer*.965,104,.40F);
        m.circle(b,Vec3.ZERO,outer*.885,92,.52F);
        for(int i=0;i<12;i++){double a=rotation*.035+i*Math.PI*2.0/12.0;m.arc(b,Vec3.ZERO,outer*.925,a,Math.PI*.105,13,.48F);}
        inscriptionRing(m,b,outer*.905,24,outer*.022,seed,-rotation*.075,.48F);

        // Two interlocked triangles + hexagonal calculation chamber, matching the requested classic magic-circle grammar.
        double tri=inner*.82;
        m.polygon(b,Vec3.ZERO,tri,3,rotation*.045+Math.PI/2.0,.60F);
        m.polygon(b,Vec3.ZERO,tri,3,rotation*.045-Math.PI/2.0,.60F);
        m.polygon(b,Vec3.ZERO,inner*.665,6,-rotation*.035,.42F);
        m.circle(b,Vec3.ZERO,inner*.535,64,.38F);
        m.circle(b,Vec3.ZERO,inner*.315,46,.48F);
        m.runeChords(b,Vec3.ZERO,inner*.285,8,3,rotation*.040,.34F);
        m.runeGlyph(b,Vec3.ZERO,inner*.145,seed^0x5A71,-rotation*.08,.48F);

        // Four major drop seals are readable sub-circles rather than giant cubes/rails.
        double orbit=outer*.695,child=outer*.102;
        for(int i=0;i<4;i++){
            double a=Math.PI/4.0+i*Math.PI/2.0+rotation*.020;
            Vec3 c=b.point(a,orbit);
            m.circle(b,c,child,30,.56F);m.circle(b,c,child*.76,24,.36F);
            m.polygon(b,c,child*.58,3,-a+Math.PI/2.0,.42F);
            m.polygon(b,c,child*.58,3,-a-Math.PI/2.0,.42F);
            m.runeGlyph(b,c,child*.30,seed+i*101,-rotation*.06,.38F);
            m.line(b.point(a,inner*.69),c,.30F);
            if(p>.72)m.line(c.add(n.scale(depth*.30)),c.add(n.scale(-Math.max(.85,r*.050))),.30F);
        }
        // Minor zodiac/coordinate marks fill the outer ring without turning into a central line knot.
        if(p>.58){for(int i=0;i<8;i++){double a=i*Math.PI/4.0-rotation*.025;Vec3 c=b.point(a,outer*.795);m.runeGlyph(b,c,outer*.030,seed^((i+1)*131),a,.32F);}}
        if(p>.80){
            m.circle(b,n.scale(depth),inner*.47,52,.30F);m.circle(b,n.scale(-depth*.70),inner*.365,46,.26F);
            ArcaneWorldMesh.Basis cross=ArcaneWorldMesh.Basis.fromNormal(b.right(),b.up());m.circle(cross,Vec3.ZERO,inner*.205,34,.26F);
        }
    }

    private static void inscriptionRing(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis b,double radius,int count,double size,int seed,double rotation,float width){
        int n=Math.max(8,count);for(int i=0;i<n;i++){double a=rotation+i*Math.PI*2.0/n;Vec3 c=b.point(a,radius),t=b.point(a+Math.PI/2.0,size),rad=b.point(a,size*.55);m.line(c.subtract(t),c.add(t),width*(i%4==0?1.0F:.72F));if(((seed+i)&1)==0)m.line(c.subtract(rad),c.add(t.scale(.55)),width*.68F);else m.line(c.add(rad),c.subtract(t.scale(.48)),width*.62F);if(Math.floorMod(seed+i,3)==0)m.line(c.subtract(t.scale(.38)).subtract(rad.scale(.70)),c.add(t.scale(.32)).add(rad.scale(.65)),width*.54F);}
    }

'''
replace_block(sigil,'    private static void meteorRitual(', '    private static void skyRitual',meteor)

# ---------------- academy page layout ----------------
grimoire=client/'GrimoireScreen.java'
academy='''    private void drawAcademy(GuiGraphicsExtractor g,Layout l,int mouseX,int mouseY){
        Rect b=l.body();title(g,b,"마도회 기록부","아르카나 "+ArcaneClientState.longInteger("marks",0));MagicTradition current=MagicTradition.parse(ArcaneClientState.text("tradition","UNBOUND"));MagicTradition[] ts=traditions();
        Rect selector=l.academySelector();g.enableScissor(selector.x(),selector.y(),selector.right(),selector.bottom());
        for(int i=0;i<ts.length;i++){Rect r=l.tradition(i);boolean active=ts[i]==inspectedTradition,joined=ts[i]==current,hover=inside(mouseX,mouseY,r);if(active)g.fill(r.x()+3,r.bottom()-2,r.right()-3,r.bottom(),0xFFD2AE6B);if(joined)ArcaneRenderUtil.diamond(g,r.x()+7,r.y()+r.h()/2,3,0xFFFFD984);g.centeredText(font,Component.literal(fit(ts[i].displayName(),Math.max(18,r.w()-14))),r.x()+r.w()/2+3,r.y()+4,joined?0xFFFFD984:active?0xFFEADCC7:hover?0xFFC9BAA4:0xFF777067);}
        Rect summary=l.academySummary();String state=current==inspectedTradition?"현재 소속":"현재 · "+current.displayName();tiny(g,fit(inspectedTradition.displayName()+" · "+state,Math.max(1,summary.w()*2)),summary.x()+2,summary.y()+4,0xFF8F8578,.50F,false);g.disableScissor();
        Rect join=l.traditionJoin();action(g,join,current==inspectedTradition?"현재 소속":"소속 등록",inside(mouseX,mouseY,join),current!=inspectedTradition,0xFFD2AE6B);
        drawCircleIndex(g,l,academyCircle,mouseX,mouseY,false);
        List<AcademyOfferCatalog.Offer> offers=AcademyOfferCatalog.forCircle(academyCircle);Rect head=l.academyOfferHeader();tiny(g,academyCircle+"써클 주문 · "+offers.size()+"종",head.x()+2,head.y()+2,0xFF9B9184,.52F,false);rule(g,head.bottom()-1,head.x(),head.right(),0xFF40372E);
        Rect v=l.academyOffers();g.enableScissor(v.x(),v.y(),v.right(),v.bottom());long marks=ArcaneClientState.longInteger("marks",0);
        for(int i=0;i<offers.size();i++){Rect r=l.offerRow(i,scroll);AcademyOfferCatalog.Offer o=offers.get(i);boolean enough=marks>=o.basePrice();rule(g,r.bottom()-1,r.x(),r.right(),0xFF3E362D);ArcaneRenderUtil.diamond(g,r.x()+11,r.y()+14,4,enough?circleColor(academyCircle):0xFF6A5552);g.text(font,Component.literal(fit(o.displayName(),Math.max(20,r.w()-90))),r.x()+24,r.y()+5,enough?0xFFDED2BF:0xFF8A7E74);tiny(g,fit(o.description(),Math.max(20,(r.w()-90)*2)),r.x()+24,r.y()+18,0xFF756D64,.50F,false);tiny(g,o.basePrice()+" A",r.right()-5,r.y()+10,enough?0xFFFFD179:0xFFB46F72,.58F,true);}
        g.disableScissor();
        Rect note=l.academyNote();g.enableScissor(note.x(),note.y(),note.right(),note.bottom());String prefix="faction_"+inspectedTradition.name().toLowerCase(Locale.ROOT);int ny=note.y()+2;if(ny<note.bottom()-5){tiny(g,fit(inspectedTradition.strength(),note.w()*2),note.x()+2,ny,0xFF82C6A4,.52F,false);ny+=10;}if(ny<note.bottom()-5){tiny(g,fit(inspectedTradition.weakness(),note.w()*2),note.x()+2,ny,0xFFC47E7E,.52F,false);ny+=10;}if(ny<note.bottom()-5)tiny(g,fit("본거지 · "+ArcaneClientState.text(prefix+"_headquarters",""),note.w()*2),note.x()+2,ny,0xFF8C8378,.50F,false);g.disableScissor();
    }

'''
replace_block(grimoire,'    private void drawAcademy(', '    private void drawQuests',academy)
old_layout='''        boolean stackedTraditions(){return body().w()<310;}Rect tradition(int i){Rect b=body();int x=b.x()+36;if(stackedTraditions()){int gap=3,w=Math.max(42,(b.w()-42-gap)/2);return new Rect(x+(i%2)*(w+gap),b.y()+26+(i/2)*18,w,16);}int reserve=102,w=Math.max(44,(b.w()-36-reserve)/4);return new Rect(x+i*w,b.y()+26,w,20);}Rect traditionJoin(){Rect b=body();return stackedTraditions()?new Rect(b.right()-92,b.y()+63,88,18):new Rect(b.right()-100,b.y()+26,94,20);}Rect academyOffers(){Rect v=viewport();int offset=stackedTraditions()?48:28;return new Rect(v.x(),v.y()+offset,v.w(),Math.max(1,v.h()-offset-30));}Rect offerRow(int i,int scroll){Rect v=academyOffers();return new Rect(v.x(),v.y()+i*31-scroll,v.w(),29);}Rect academyNote(){Rect v=viewport();return new Rect(v.x(),Math.max(v.y(),v.bottom()-29),v.w(),29);}'''
new_layout='''        boolean stackedTraditions(){return viewport().w()<410;}int academySelectorHeight(){return stackedTraditions()?58:42;}Rect academySelector(){Rect v=viewport();return new Rect(v.x(),v.y(),v.w(),Math.min(v.h(),academySelectorHeight()));}Rect tradition(int i){Rect s=academySelector();int gap=4;if(stackedTraditions()){int w=Math.max(28,(s.w()-gap)/2);return new Rect(s.x()+(i%2)*(w+gap),s.y()+(i/2)*18,w,16);}int w=Math.max(28,(s.w()-gap*3)/4);return new Rect(s.x()+i*(w+gap),s.y(),w,18);}Rect academySummary(){Rect s=academySelector();int y=stackedTraditions()?s.y()+38:s.y()+21;int reserve=Math.min(88,Math.max(58,s.w()/3));return new Rect(s.x(),y,Math.max(1,s.w()-reserve-5),18);}Rect traditionJoin(){Rect s=academySelector(),summary=academySummary();int w=Math.min(84,Math.max(54,s.w()/3));return new Rect(Math.max(s.x(),s.right()-w),summary.y(),w,18);}Rect academyOfferHeader(){Rect v=viewport(),s=academySelector();int y=Math.min(v.bottom(),s.bottom()+3);return new Rect(v.x(),y,v.w(),Math.max(1,Math.min(13,v.bottom()-y)));}Rect academyNote(){Rect v=viewport();int h=Math.min(31,Math.max(20,v.h()/5));return new Rect(v.x(),Math.max(v.y(),v.bottom()-h),v.w(),Math.min(h,v.h()));}Rect academyOffers(){Rect v=viewport(),head=academyOfferHeader(),note=academyNote();int y=Math.min(v.bottom(),head.bottom()+2);return new Rect(v.x(),y,v.w(),Math.max(1,note.y()-y-3));}Rect offerRow(int i,int scroll){Rect v=academyOffers();return new Rect(v.x(),v.y()+i*31-scroll,v.w(),29);}'''
replace_once(grimoire,old_layout,new_layout)

# ---------------- source audit ----------------
audit=root/'tools/test_current_source.py'
for old,new in [
    ('mod_version=0.12.1-alpha.30','mod_version=0.12.1-alpha.31'),
    ('VERSION = "0.12.1-alpha.30"','VERSION = "0.12.1-alpha.31"'),
    ('"version": "0.12.1-alpha.30"','"version": "0.12.1-alpha.31"')]: replace_once(audit,old,new)
replace_once(audit,
'''for token in ['formulaFrame','schoolFormula','geometricDepth','anchorFormula','skyRitual','meteorRitual','sigilRangeScale','meteor_swarm','runeRing','brokenBand','helix','fusionFormula']:''',
'''for token in ['formulaFrame','schoolFormula','geometricDepth','anchorFormula','skyRitual','meteorRitual','inscriptionRing','sigilRangeScale','meteor_swarm','runeRing','fusionFormula']:''')
replace_once(audit,
'''for token in ['drawSpine','circleIndex','circleStep','contentBottom','compact()','browserViewport','detailWidth','detail()','spellTile','primaryAction','drawLoadout','equipCandidateId','firstEmptySlot','quickEquip','enableScissor','mouseScrolled']:''',
'''for token in ['drawSpine','circleIndex','circleStep','contentBottom','compact()','browserViewport','detailWidth','detail()','spellTile','primaryAction','drawLoadout','equipCandidateId','firstEmptySlot','quickEquip','academySelector','academySummary','academyOfferHeader','enableScissor','mouseScrolled']:''')
insert="""assert 'fineBuilder' in text(client/'ArcaneWorldMesh.java')\nassert 'lineFloor' in text(client/'ArcaneWorldMesh.java')\nassert 'drawCircleIndex(g,l,academyCircle,mouseX,mouseY,true)' not in grimoire\nassert 'viewport().w()<410' in grimoire\n"""
anchor="assert 'Math.max(22,Math.min(29' not in grimoire\n"
replace_once(audit,anchor,anchor+insert)

# ---------------- docs ----------------
changelog=root/'CHANGELOG.md'
ct=read(changelog)
entry='''# Changelog\n\n## 0.12.1-alpha.31\n- 메테오를 포함한 술식 마법진을 굵은 철골형 다각형에서 얇은 동심환·문자띠형 룬·중첩 삼각/육각 기하·보조 인장 중심의 정밀 술식 문법으로 재설계했다.\n- `ArcaneWorldMesh`에 술식 전용 fine-line 렌더 경로를 추가해 물리 연출의 굵은 광선은 유지하면서 마법진만 훨씬 가는 선으로 그릴 수 있게 했다.\n- Meteor Swarm은 거대 팔각 프레임과 중앙 선뭉치를 제거하고 외곽 3중환, 24개 문자형 룬, 겹친 삼각형/육각 계산실, 4개 독립 낙하지정 인장으로 교체했다.\n- 마도회 페이지의 소속 선택·가입 버튼·써클 주문 목록·소속 설명을 서로 독립 영역으로 분리하고, 좁은 폭에서는 소속 4개를 2x2로 접어 글자 겹침을 차단했다.\n- 주문 목록 시작점과 하단 소속 설명 영역을 고정 분리하고 써클 레일의 불필요한 상품 개수 표기를 제거했다.\n\n'''
if not ct.startswith('# Changelog\n\n'): raise SystemExit('unexpected changelog header')
write(changelog,entry+ct[len('# Changelog\n\n'):])
project=root/'PROJECT.md'
pt=read(project).replace('## Alpha.30 runtime contracts','## Alpha.31 runtime contracts',1)
pt=pt.replace('- High-complexity sigils gain depth through orthogonal planes, axial structures and role-specific sub-arrays rather than flat line density.','- High-complexity sigils prioritize fine concentric rules, inscription rings, nested geometry and balanced satellite seals; 3D depth stays secondary and may not overpower the readable planar formula.\n- Academy affiliation selector, join action, offer header/list and lore note own separate layout regions; narrow widths fold traditions into a 2x2 selector.',1)
pt=pt.replace('- Canonical Java 25 verification is required after the alpha.30 source commit; source-only audit is not sufficient.','- Canonical Java 25 verification is required after the alpha.31 source commit; source-only audit is not sufficient.',1)
write(project,pt)
readme=root/'README.md'
rt=read(readme)
old='모든 주문은 시전 단계에서 읽을 수 있는 실제 술식 마법진을 갖는다. 단, 같은 원을 복붙하지 않는다.'
new='모든 주문은 시전 단계에서 읽을 수 있는 실제 술식 마법진을 갖는다. 기본 시각 문법은 얇은 동심환, 문자띠형 룬, 중첩 기하, 균형 잡힌 보조 인장이며 단순한 굵은 다각형 철골이나 같은 원 복붙을 사용하지 않는다.'
if old not in rt: raise SystemExit('README presentation anchor missing')
write(readme,rt.replace(old,new,1))

# Self-clean active tree before audits/build.
(repo/'.github/scripts/arcane_alpha31_refine_20260813.py').unlink(missing_ok=True)
(repo/'.github/workflows/maintenance-arcane31-refine-20260813.yml').unlink(missing_ok=True)
print('Arcane Circle alpha.31 refinement applied')
