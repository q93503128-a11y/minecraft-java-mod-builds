from __future__ import annotations

from pathlib import Path

repo = Path.cwd()
root = repo / 'projects/arcane-circle'


def read(rel: str) -> str:
    return (root / rel).read_text(encoding='utf-8')


def write(rel: str, value: str) -> None:
    path = root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(value, encoding='utf-8')


def replace_exact(rel: str, old: str, new: str, count: int = 1) -> None:
    value = read(rel)
    actual = value.count(old)
    assert actual == count, f'{rel}: expected {count} occurrences, found {actual}: {old[:120]!r}'
    write(rel, value.replace(old, new, count))


def replace_block(rel: str, start_marker: str, end_marker: str, new_block: str) -> None:
    value = read(rel)
    start = value.index(start_marker)
    end = value.index(end_marker, start)
    write(rel, value[:start] + new_block + value[end:])


# -----------------------------------------------------------------------------
# Version
# -----------------------------------------------------------------------------
replace_exact('gradle.properties', 'mod_version=0.12.1-alpha.28', 'mod_version=0.12.1-alpha.29')
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
              'VERSION = "0.12.1-alpha.28"', 'VERSION = "0.12.1-alpha.29"')
replace_exact('src/main/resources/data/arcanecircle/spell_catalog/index.json',
              '"version": "0.12.1-alpha.28"', '"version": "0.12.1-alpha.29"')

# -----------------------------------------------------------------------------
# Grimoire: true compact layout for high GUI scale / short logical screens.
# -----------------------------------------------------------------------------
replace_block(
    'src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java',
    '    private void drawCircleIndex(',
    '    private void drawSpellTile(',
    '''    private void drawCircleIndex(GuiGraphicsExtractor g,Layout l,int selected,int mouseX,int mouseY,boolean shop){
        for(int c=1;c<=9;c++){
            Rect r=l.circleIndex(c);boolean active=c==selected,hover=inside(mouseX,mouseY,r),unlocked=c<=ArcaneClientState.integer("circle",1);
            int color=unlocked?circleColor(c):0xFF4D4A47;int cy=r.y()+r.h()/2,ring=Math.max(4,Math.min(8,r.h()/2-1));
            if(active)g.fill(r.x(),r.y()+2,r.x()+2,r.bottom()-2,color);
            g.centeredText(font,Component.literal(Integer.toString(c)),r.x()+r.w()/2,cy-4,active?0xFFF5E2BC:hover?0xFFD9C7A8:unlocked?0xFFA39A8C:0xFF5C5852);
            if(active||hover)ArcaneRenderUtil.ring(g,r.x()+r.w()/2,cy,ring,color);
            if(shop){int count=AcademyOfferCatalog.forCircle(c).size();tiny(g,Integer.toString(count),r.right()-2,r.bottom()-5,color,.45F,true);}
        }
    }

''')

replace_block(
    'src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java',
    '    private void drawSpellDetail(',
    '    private void drawLoadout(',
    '''    private void drawSpellDetail(GuiGraphicsExtractor g,Layout l,SpellDefinition s,int mouseX,int mouseY){
        Rect d=l.detail();if(d.w()<70||d.h()<56||s==null)return;
        g.enableScissor(d.x(),d.y(),d.right(),d.bottom());
        g.fill(d.x(),d.y()+3,d.x()+1,d.bottom()-3,0xFF554838);
        int accent=ArcaneRenderUtil.schoolColor(s.school());int cx=d.x()+d.w()/2;Rect a=l.primaryAction();
        boolean usable=ArcaneClientState.known().contains(s.id())&&s.circle()<=ArcaneClientState.integer("circle",1);
        String label=activeSlot<0?"아래에서 장착 슬롯 선택":usable?(activeSlot+1)+"번 슬롯에 각인":"습득 필요";

        if(l.compact()){
            int iconY=d.y()+15;
            ArcaneRenderUtil.ring(g,cx,iconY,11,accent);ArcaneRenderUtil.ring(g,cx,iconY,8,0xFF6E5E49);ArcaneRenderUtil.spellRune(g,cx,iconY,s,5,0xFFFFF2DC);
            g.centeredText(font,Component.literal(fit(s.name(),Math.max(36,d.w()-8))),cx,d.y()+29,0xFFF0E4D1);
            tiny(g,s.circle()+"C · "+s.school().displayName(),cx,d.y()+41,accent,.52F,true);
            tiny(g,fit("MP "+s.manaCost()+" · 쿨 "+one(s.cooldownTicks()/20.0)+"s",d.w()*2),cx,d.y()+53,0xFFCDBDA7,.50F,true);
            tiny(g,fit("범위 "+one(s.range())+" · 숙련 "+ArcaneClientState.mastery(s.id())+"/"+SpellCatalog.masteryRequired(s.id()),d.w()*2),cx,d.y()+63,0xFF9F9587,.48F,true);
            action(g,a,label,inside(mouseX,mouseY,a),usable&&activeSlot>=0,accent);
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
        action(g,a,label,inside(mouseX,mouseY,a),usable&&activeSlot>=0,accent);
        g.disableScissor();
    }

''')

replace_exact(
    'src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java',
    '    private Layout layout(){int w=Math.min(780,Math.max(340,width-28)),h=Math.min(450,Math.max(260,height-24));w=Math.min(w,Math.max(1,width-8));h=Math.min(h,Math.max(1,height-8));return new Layout((width-w)/2,(height-h)/2,w,h);}',
    '    private Layout layout(){int w=Math.min(780,Math.max(220,width-28)),h=Math.min(450,Math.max(180,height-24));w=Math.min(w,Math.max(1,width-8));h=Math.min(h,Math.max(1,height-8));return new Layout((width-w)/2,(height-h)/2,w,h);}')

grim = read('src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java')
layout_start = grim.index('    private record Layout(int left,int top,int panelW,int panelH){')
layout_end = grim.rfind('\n    }\n}')
assert layout_end > layout_start
new_layout = '''    private record Layout(int left,int top,int panelW,int panelH){
        int right(){return left+panelW;}int bottom(){return top+panelH;}boolean isWide(){return panelW>=590;}boolean compact(){return panelH<300||body().h()<240;}
        int tabStep(){return Math.max(27,Math.min(38,Math.max(27,(panelH-36)/6)));}
        Rect close(){return new Rect(right()-24,top+5,18,18);}Rect tab(int i){int step=tabStep(),h=Math.max(23,Math.min(32,step-2));return new Rect(left+4,top+28+i*step,48,h);}Rect body(){return new Rect(left+66,top+27,Math.max(96,panelW-78),Math.max(96,panelH-38));}
        Rect viewport(){Rect b=body();return new Rect(b.x()+38,b.y()+31,Math.max(48,b.w()-40),Math.max(42,b.h()-38));}
        int circleStep(){Rect b=body();return Math.max(13,Math.min(29,Math.max(13,(b.h()-34)/9)));}
        Rect circleIndex(int c){Rect b=body();int step=circleStep(),h=Math.max(12,Math.min(22,step));return new Rect(b.x(),b.y()+28+(c-1)*step,30,h);}
        int footerHeight(){return compact()?29:32;}int footerTop(){return body().bottom()-footerHeight();}
        int detailWidth(){Rect b=body();int preferred=Math.max(96,Math.min(205,b.w()/3));int minBrowser=Math.max(52,Math.min(90,b.w()/3));int maxDetail=Math.max(70,b.w()-38-minBrowser-5);return Math.min(preferred,maxDetail);}
        Rect detail(){Rect b=body();int w=detailWidth(),y=b.y()+31,h=Math.max(56,footerTop()-y-5);return new Rect(b.right()-w,y,w,h);}
        Rect browserViewport(){Rect b=body(),d=detail();int x=b.x()+38,y=b.y()+31;return new Rect(x,y,Math.max(52,d.x()-x-5),Math.max(52,footerTop()-y-5));}
        Rect spellTile(int i,int scroll,int count){Rect v=browserViewport();int cols=v.w()>=340?5:v.w()>=230?4:v.w()>=150?3:2;int gap=3,w=Math.max(20,(v.w()-gap*(cols-1))/cols),row=i/cols,col=i%cols;return new Rect(v.x()+col*(w+gap),v.y()+row*48-scroll,w,44);}int maxTileScroll(int count){Rect v=browserViewport();int cols=v.w()>=340?5:v.w()>=230?4:v.w()>=150?3:2;return Math.max(0,((count+cols-1)/cols)*48-v.h());}
        Rect loadout(int i){Rect b=body();int gap=4,w=Math.max(20,(b.w()-gap*4)/5);return new Rect(b.x()+i*(w+gap),footerTop()+2,w,Math.max(20,footerHeight()-4));}
        Rect primaryAction(){Rect d=detail();int h=compact()?17:20;return new Rect(d.x()+8,d.bottom()-h,Math.max(54,d.w()-16),h);}
        Rect listRow(int i,int scroll,int h){Rect v=viewport();return new Rect(v.x(),v.y()+i*h-scroll,v.w(),h-2);}
        Rect tradition(int i){Rect b=body();int x=b.x()+36,w=Math.max(55,(b.w()-160)/4);return new Rect(x+i*w,b.y()+26,w,20);}Rect traditionJoin(){Rect b=body();return new Rect(b.right()-100,b.y()+26,94,20);}Rect academyOffers(){Rect v=viewport();return new Rect(v.x(),v.y()+28,v.w(),Math.max(30,v.h()-54));}Rect offerRow(int i,int scroll){Rect v=academyOffers();return new Rect(v.x(),v.y()+i*31-scroll,v.w(),29);}Rect academyNote(){Rect v=viewport();return new Rect(v.x(),Math.max(v.y(),v.bottom()-29),v.w(),29);}
        Rect questAccept(){Rect v=viewport();return new Rect(v.right()-88,v.y()+3,40,19);}Rect questReject(){Rect v=viewport();return new Rect(v.right()-43,v.y()+3,40,19);}Rect questRow(int i,int scroll,int baseY){Rect v=viewport();return new Rect(v.x(),v.y()+baseY+i*48-scroll,v.w(),45);}Rect questClaim(int i,int scroll){Rect r=questRow(i,scroll,51);return new Rect(r.right()-48,r.y()+7,44,20);}
    }'''
grim = grim[:layout_start] + new_layout + grim[layout_end + len('\n    }'):]
write('src/main/java/kr/moonseungjun/arcanecircle/client/GrimoireScreen.java', grim)

# -----------------------------------------------------------------------------
# Sigils: range-reactive sizing plus a clean authored Meteor ritual.
# -----------------------------------------------------------------------------
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneSigilDirector.java',
              '    private static final int BUDGET = 7200;', '    private static final int BUDGET = 1400;')
replace_exact(
    'src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneSigilDirector.java',
    '''        double breath = 1.0 + Math.sin(time * (1.25 + profile.complexity() * .12)) * .025 * p;
        double radius = Math.max(.42, profile.radius()) * (.58 + .42 * p) * breath * (fusion ? 1.10 : 1.0);
        int seed = spell.id().hashCode();
        double rotation = time * (.11 + profile.complexity() * .018) + seed * .00031;
        ArcaneWorldMesh.Basis primary = primaryBasis(profile.sigil(), direction);

        formulaFrame(mesh, spell, profile, primary, radius, p, rotation, seed);
        schoolFormula(mesh, spell, primary, radius, p, rotation, seed);
        anchorFormula(mesh, spell, profile, primary, direction, targetOffset, radius, p, rotation, seed);
        if (fusion) fusionFormula(mesh, primary, radius, p, rotation, seed);
        return mesh.build();''',
    '''        double breath = 1.0 + Math.sin(time * (1.25 + profile.complexity() * .12)) * .025 * p;
        double rangeScale = sigilRangeScale(spell, profile, range);
        double radius = Math.max(.42, profile.radius()) * rangeScale * (.58 + .42 * p) * breath * (fusion ? 1.10 : 1.0);
        int seed = spell.id().hashCode();
        double rotation = time * (.11 + profile.complexity() * .018) + seed * .00031;
        ArcaneWorldMesh.Basis primary = primaryBasis(profile.sigil(), direction);

        if ("meteor_swarm".equals(spell.id())) {
            meteorRitual(mesh, primary, radius, p, rotation, seed);
            if (fusion) fusionFormula(mesh, primary, radius, p, rotation, seed);
            return mesh.build();
        }
        formulaFrame(mesh, spell, profile, primary, radius, p, rotation, seed);
        schoolFormula(mesh, spell, primary, radius, p, rotation, seed);
        anchorFormula(mesh, spell, profile, primary, direction, targetOffset, radius, p, rotation, seed);
        if (fusion) fusionFormula(mesh, primary, radius, p, rotation, seed);
        return mesh.build();''')

sigil_path='src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneSigilDirector.java'
sigil=read(sigil_path)
marker='    private static void skyRitual(ArcaneWorldMesh.Builder m, SpellDefinition spell,'
insert='''    private static double sigilRangeScale(SpellDefinition spell, SpellPresentationProfile.Profile profile, double range) {
        double base=Math.max(1.0,spell.range());
        double ratio=clamp(Math.max(.1,range)/base,.45,4.0);
        double exponent=switch(profile.sigil()){
            case SKY_RITUAL -> .52;
            case GROUND_SEAL, QUAD_ARRAY, WALL_MATRIX -> .44;
            case PORTAL_GATE -> .30;
            case TARGET_SEAL -> .20;
            case FRONT_COMPACT, FRONT_LANCE -> .12;
            case BODY_HALO, FEET_RUNE -> .08;
        };
        double max=profile.sigil()==SpellPresentationProfile.SigilStyle.SKY_RITUAL?1.78:1.52;
        return clamp(Math.pow(ratio,exponent),.78,max);
    }

    private static void meteorRitual(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b,
                                     double r, double p, double rotation, int seed) {
        double outer=r*(.78+.22*p),inner=outer*.72;
        m.circle(b,Vec3.ZERO,outer,96,1.34F);
        m.circle(b,Vec3.ZERO,outer*.89,84,.66F);
        m.circle(b,b.normal().scale(r*.045),inner,72,.78F);
        m.runeRing(b,Vec3.ZERO,outer*.94,12,r*.050,seed,-rotation*.18,.72F);
        m.star(b,Vec3.ZERO,inner*.58,inner*.34,8,rotation*.16,1.04F);
        m.polygon(b,Vec3.ZERO,inner*.42,4,-rotation*.12+Math.PI/4.0,.72F);
        m.circle(b,Vec3.ZERO,inner*.23,40,1.18F);
        double orbit=outer*.63,child=outer*.145;
        Vec3 down=b.normal().scale(Math.max(1.8,r*.11));
        for(int i=0;i<4;i++){
            double a=Math.PI/4.0+i*Math.PI/2.0+rotation*.035;
            Vec3 c=b.point(a,orbit),rail=b.point(a,inner*.72);
            m.line(rail,c,i%2==0?1.12F:.78F);
            m.circle(b,c,child,34,1.22F);
            m.polygon(b,c,child*.68,4,-a+Math.PI/4.0,.86F);
            m.runeGlyph(b,c,child*.42,seed+i*101,-rotation*.20,.82F);
            m.line(c,c.add(down),1.22F);
            if(p>.72)m.circle(b,c.add(down.scale(.42)),child*.58,24,.64F);
        }
        if(p>.82){
            for(int i=0;i<4;i++){
                double a=i*Math.PI/2.0+rotation*.08;
                m.arc(b,Vec3.ZERO,outer*1.07,a,Math.PI*.34,20,i==0?1.08F:.66F);
            }
        }
    }

'''
assert marker in sigil
sigil=sigil.replace(marker,insert+marker,1)
write(sigil_path,sigil)

# -----------------------------------------------------------------------------
# Presentation profile: Meteor has enough altitude for a true sky ritual.
# -----------------------------------------------------------------------------
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/magic/SpellPresentationProfile.java',
              'put("meteor_swarm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 18.0, 6, 4, 0, 30, 2.55, 30);',
              'put("meteor_swarm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 18.0, 6, 4, 0, 42, 2.55, 30);')

# -----------------------------------------------------------------------------
# Cinematic director: stronger Meteor body and bounded Prismatic Wall rendering.
# -----------------------------------------------------------------------------
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/client/SpellCinematicDirector.java',
              '    private static final int CHARGE_BUDGET=7600;\n    private static final int RELEASE_BUDGET=9800;',
              '    private static final int CHARGE_BUDGET=1400;\n    private static final int RELEASE_BUDGET=2600;')
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/client/SpellCinematicDirector.java',
              '        if ("meteor_swarm".equals(spell.id())) { meteorSwarm(m,targetOffset,age,impactAge,scale); return m.build(); }',
              '        if ("meteor_swarm".equals(spell.id())) { meteorSwarm(m,targetOffset,age,impactAge,scale); return m.build(); }\n        if ("prismatic_wall".equals(spell.id())) { prismaticWallFrame(m,face,targetOffset,range,age,spell.circle()); return m.build(); }')
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/client/SpellCinematicDirector.java',
              '        ArcaneWorldMesh.Builder m=ArcaneWorldMesh.builder(900);',
              '        ArcaneWorldMesh.Builder m=ArcaneWorldMesh.builder(48);')
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/client/SpellCinematicDirector.java',
              '            case "time_stop" -> 0xFF64EFFF;',
              '            case "meteor_swarm" -> 0xFFFF641F;\n            case "time_stop" -> 0xFF64EFFF;')
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/client/SpellCinematicDirector.java',
              'id.equals("meteor_swarm")?28:14,1.9);', 'id.equals("meteor_swarm")?42:14,1.9);')

director_path='src/main/java/kr/moonseungjun/arcanecircle/client/SpellCinematicDirector.java'
director=read(director_path)
start=director.index('    private static void meteorSwarm(')
end=director.index('    private static void executionWord(',start)
new_meteor='''    private static void prismaticWallFrame(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis b,Vec3 target,double range,double age,int circle){
        double width=SpellMetrics.wallWidth("prismatic_wall",range,circle),h=2.6+circle*.22,p=clamp(age/.22,0,1);Vec3 right=b.right();
        Vec3 left=target.add(right.scale(-width*.5)),rightEdge=target.add(right.scale(width*.5)),up=new Vec3(0,h*p,0);
        m.line(left,rightEdge,1.22F).line(left.add(up),rightEdge.add(up),1.22F).line(left,left.add(up),1.30F).line(rightEdge,rightEdge.add(up),1.30F);
        for(int i=1;i<7;i++){Vec3 base=target.add(right.scale((i/7.0-.5)*width));m.line(base,base.add(up),i==3?1.05F:.58F);}
    }

    private static void meteorSwarm(ArcaneWorldMesh.Builder m,Vec3 target,double age,double impact,double scale){
        ArcaneWorldMesh.Basis down=ArcaneWorldMesh.Basis.facing(new Vec3(0,-1,0));ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();
        double imp=Math.max(.15,impact<=0?.62:impact),t=clamp(age/imp,0,1),fallHeight=42.0;double[][] o={{-10,-10},{10,-10},{-10,10},{10,10}};
        for(int i=0;i<4;i++){
            Vec3 hit=target.add(o[i][0],0,o[i][1]);Vec3 pos=hit.add(0,fallHeight*(1-easeIn(t)),0);double head=1.34*scale;
            m.orb(pos,head,30,1.24F,.52F);m.shard(pos.add(0,1.25*scale,0),new Vec3(0,-1,0),down,5.2*scale,.78*scale,1.22F,.42F);
            m.beamPrism(pos.add(0,3.0*scale,0),new Vec3(0,-1,0),down,4.0*scale,.20*scale,1.14F,.24F);
            for(int tail=0;tail<3;tail++){double a=tail*Math.PI*2/3.0+i*.37;Vec3 edge=pos.add(g.point(a,.62*scale));m.line(edge,edge.add(0,3.8*scale,0),tail==0?1.05F:.62F);}
            if(age>=imp){double a=clamp((age-imp)/.32,0,1),e=easeOut(a),r=11*e;m.band(g,hit,r*.84,r,64,1.24F,(float)(.36*(1-a)));m.circle(g,hit,r*.58,48,.82F);for(int ray=0;ray<8;ray++){double q=ray*Math.PI/4.0;m.line(hit.add(g.point(q,r*.12)),hit.add(g.point(q,r)),ray%2==0?1.08F:.58F);}}
        }
        if(age>=imp){double a=clamp((age-imp)/.40,0,1),r=18*easeOut(a);m.circle(g,target,r,80,1.18F);m.circle(g,target,r*.72,64,.58F);}
    }
'''
director=director[:start]+new_meteor+director[end:]
write(director_path,director)

# -----------------------------------------------------------------------------
# Runtime render hard caps. Prismatic Wall no longer stacks a full release echo.
# -----------------------------------------------------------------------------
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java',
              '    private static final int MAX_VISUALS = 16;\n    private static final int MAX_FRAME = 68000;',
              '    private static final int MAX_VISUALS = 10;\n    private static final int MAX_FRAME = 9000;\n    private static final int MAX_ENTRY = 2800;')
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java',
              '''            ArcaneWorldMesh echo=ArcaneSigilDirector.releaseEcho(v.spell,v.direction,targetOffset(v),v.range,age,v.fusion,v.startedAt);
            if(echo.size()>0)entries.add(new RenderEntry(v.center,echo,ArcaneSigilDirector.releaseEchoColor(color,age)));''',
              '''            if(!"prismatic_wall".equals(v.spell.id())){
                ArcaneWorldMesh echo=ArcaneSigilDirector.releaseEcho(v.spell,v.direction,targetOffset(v),v.range,age,v.fusion,v.startedAt);
                if(echo.size()>0)entries.add(new RenderEntry(v.center,echo,ArcaneSigilDirector.releaseEchoColor(color,age)));
            }''')
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java',
              '''        for(RenderEntry entry:entries){
            if(used>=MAX_FRAME)break;
            Vec3 offset=entry.center.subtract(camera);
            if(offset.lengthSqr()>MAX_DISTANCE_SQR)continue;
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(offset.x,offset.y,offset.z);
            entry.mesh.submit(event.getPoseStack(),event.getSubmitNodeCollector(),entry.argb,scale);
            event.getPoseStack().popPose();
            used+=entry.mesh.size();
        }''',
              '''        for(RenderEntry entry:entries){
            if(used>=MAX_FRAME)break;
            int cost=entry.mesh.size();if(cost<=0||cost>MAX_ENTRY||used+cost>MAX_FRAME)continue;
            Vec3 offset=entry.center.subtract(camera);
            if(offset.lengthSqr()>MAX_DISTANCE_SQR)continue;
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(offset.x,offset.y,offset.z);
            entry.mesh.submit(event.getPoseStack(),event.getSubmitNodeCollector(),entry.argb,scale);
            event.getPoseStack().popPose();
            used+=cost;
        }''')

# -----------------------------------------------------------------------------
# Server effects: keep Meteor impact path synchronized to the 42-block visual descent.
# -----------------------------------------------------------------------------
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/magic/HighCircleSpellEffects.java',
              '            Vec3 sky = impact.add(0, 28, 0);', '            Vec3 sky = impact.add(0, 42, 0);')
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/magic/HighCircleSpellEffects.java',
              '        Vec3 right = player.getLookAngle().cross(new Vec3(0, 1, 0)).normalize();\n        double half = Math.max(12.0, range * 0.25);',
              '        double half = Math.max(12.0, Math.min(36.0, range * 0.25));')

# -----------------------------------------------------------------------------
# Real Light spell: temporary invisible vanilla light blocks that follow the caster.
# -----------------------------------------------------------------------------
light_service='''package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Temporary server-authoritative illumination for the Light spell. */
public final class ArcaneLightService {
    private static final int LIGHT_LEVEL=15;
    private static final int REFRESH_INTERVAL=4;
    private static final int[][] OFFSETS={{0,1,0},{3,1,0},{-3,1,0},{0,1,3},{0,1,-3}};
    private static final Map<UUID,LightState> ACTIVE=new HashMap<>();

    private ArcaneLightService(){}

    public static void illuminate(ServerPlayer player,int durationTicks){
        ServerLevel level=(ServerLevel)player.level();long now=level.getGameTime();
        LightState state=ACTIVE.computeIfAbsent(player.getUUID(),ignored->new LightState());
        state.untilTick=Math.max(state.untilTick,now+Math.max(20,durationTicks));refresh(player,state);
    }

    public static void tick(ServerPlayer player){
        LightState state=ACTIVE.get(player.getUUID());if(state==null)return;ServerLevel level=(ServerLevel)player.level();
        if(level.getGameTime()>=state.untilTick){clear(player,state);ACTIVE.remove(player.getUUID());return;}
        if(player.tickCount%REFRESH_INTERVAL==0)refresh(player,state);
    }

    public static void clear(ServerPlayer player){LightState state=ACTIVE.remove(player.getUUID());if(state!=null)clear(player,state);}

    public static void clearAll(MinecraftServer server){for(LightState state:ACTIVE.values())clear(server,state);ACTIVE.clear();}

    private static void refresh(ServerPlayer player,LightState state){
        ServerLevel level=(ServerLevel)player.level();MinecraftServer server=level.getServer();
        if(state.dimension!=null&&!state.dimension.equals(level.dimension())){clear(server,state);state.positions.clear();}
        state.dimension=level.dimension();BlockPos base=player.blockPosition();Set<BlockPos> desired=new HashSet<>();
        for(int[] off:OFFSETS)desired.add(base.offset(off[0],off[1],off[2]));
        Set<BlockPos> old=new HashSet<>(state.positions);
        for(BlockPos pos:old)if(!desired.contains(pos)){removeOwned(level,pos);state.positions.remove(pos);}
        BlockState light=Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL,LIGHT_LEVEL);
        for(BlockPos pos:desired){
            if(state.positions.contains(pos)&&level.getBlockState(pos).is(Blocks.LIGHT))continue;
            if(level.getBlockState(pos).isAir()&&level.setBlock(pos,light,3))state.positions.add(pos.immutable());
        }
    }

    private static void clear(ServerPlayer player,LightState state){clear(((ServerLevel)player.level()).getServer(),state);state.positions.clear();state.dimension=null;}
    private static void clear(MinecraftServer server,LightState state){
        if(state.dimension==null)return;ServerLevel level=server.getLevel(state.dimension);if(level==null)return;
        for(BlockPos pos:state.positions)removeOwned(level,pos);
    }
    private static void removeOwned(ServerLevel level,BlockPos pos){if(level.getBlockState(pos).is(Blocks.LIGHT))level.removeBlock(pos,false);}

    private static final class LightState{
        long untilTick;ResourceKey<Level> dimension;final Set<BlockPos> positions=new HashSet<>();
    }
}
'''
write('src/main/java/kr/moonseungjun/arcanecircle/magic/ArcaneLightService.java',light_service)
replace_exact('src/main/java/kr/moonseungjun/arcanecircle/magic/ExpandedSpellEffects.java',
              '''    private static boolean light(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0));
        shell(player, 1.0, ParticleTypes.END_ROD);
        return true;
    }''',
              '''    private static boolean light(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1800, 0));
        ArcaneLightService.illuminate(player,1800);
        shell(player, 1.0, ParticleTypes.END_ROD);
        return true;
    }''')

arc='src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java'
replace_exact(arc,'import kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;','import kr.moonseungjun.arcanecircle.magic.ArcaneLightService;\nimport kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;')
replace_exact(arc,
              '''    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SpellCastingService.clearSession(event.getEntity().getUUID());''',
              '''    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if(event.getEntity() instanceof ServerPlayer player)ArcaneLightService.clear(player);
        SpellCastingService.clearSession(event.getEntity().getUUID());''')
replace_exact(arc,
              '''    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SpellCastingService.clearSession(player.getUUID());''',
              '''    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ArcaneLightService.clear(player);
        SpellCastingService.clearSession(player.getUUID());''')
replace_exact(arc,
              '''    private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        SpellCastingService.clearSession(player.getUUID());''',
              '''    private void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ArcaneLightService.clear(player);
        SpellCastingService.clearSession(player.getUUID());''')
replace_exact(arc,
              '''        SpellCastingService.tickCharge(player);
        SpellKineticsService.tick(player);''',
              '''        SpellCastingService.tickCharge(player);
        ArcaneLightService.tick(player);
        SpellKineticsService.tick(player);''')
replace_exact(arc,
              '''    private void onServerStopped(ServerStoppedEvent event) {
        SpellCastingService.clearAllSessions();''',
              '''    private void onServerStopped(ServerStoppedEvent event) {
        ArcaneLightService.clearAll(event.getServer());
        SpellCastingService.clearAllSessions();''')

# -----------------------------------------------------------------------------
# Contracts, docs and validation.
# -----------------------------------------------------------------------------
replace_exact('src/main/resources/data/arcanecircle/spell_catalog/index.json','"charge_primitive_cap": 320','"charge_primitive_cap": 1400')
replace_exact('src/main/resources/data/arcanecircle/spell_catalog/index.json','"release_primitive_cap": 512','"release_primitive_cap": 2600')
replace_exact('src/main/resources/data/arcanecircle/spell_catalog/index.json','"frame_primitive_cap": 1024','"frame_primitive_cap": 9000')

change=read('CHANGELOG.md')
change='''# Changelog\n\n## 0.12.1-alpha.29\n- 높은 GUI 배율/낮은 논리 해상도에서 마도서의 왼쪽 탭, 1~9써클 인덱스, 상세 정보와 하단 장착 스트립이 겹치거나 화면 밖으로 나가던 고정 간격을 반응형 레이아웃으로 교체했다.\n- 주문 마법진 크기에 실제 최종 사거리 비율을 반영한다. 투사체 전면진은 완만하게, 지면/장벽/상공 의식은 더 강하게 반응하며 과도한 확대는 제한한다.\n- Meteor Swarm 시전진을 범용 고복잡도 원에서 전용 천체 강하 의식으로 재작성했다. 상공 42블록 주술식, 네 개 독립 낙하점 회로, 수직 강하 레일, 대형 운석과 이중 충격파를 사용한다.\n- Light가 Night Vision만 주던 문제를 수정해 90초 동안 플레이어를 따라가는 임시 레벨 15 월드 광원을 만든다. 만료/로그아웃/차원 이동/서버 종료 시 정리한다.\n- Prismatic Wall은 7색 패널 아래에 중복된 대형 면을 다시 그리지 않고 경계 프레임만 사용한다. 프레임/엔트리/전체 primitive 하드캡을 실제 런타임에 적용해 렌더 폭주를 차단한다.\n- Java 25 정본 빌드와 JAR 감사 계약에 신규 광원 서비스 및 alpha.29 성능/레이아웃 회귀검사를 추가했다.\n\n'''+change.split('\n',2)[2]
write('CHANGELOG.md',change)

readme=read('README.md')
needle='모든 주문은 시전 단계에서 읽을 수 있는 실제 술식 마법진을 갖는다. 단, 같은 원을 복붙하지 않는다. 학파·주문·앵커에 따라 룬, 보조진, 평면, 3D 깊이와 전개 순서가 달라지고, 높은 써클이라는 이유만으로 무조건 커지지는 않는다. 완성된 술식에서 투사체·게이트·폭풍·영역·충돌·잔류가 이어진다.'
replacement=needle+' 최종 사거리 증가는 술식 종류에 맞는 비율로 마법진 크기에도 반영되며, 상공/영역 의식은 전면 발사진보다 크게 반응한다.'
assert needle in readme
readme=readme.replace(needle,replacement,1)
write('README.md',readme)

# source audit
p='tools/test_current_source.py';t=read(p)
t=t.replace("assert 'mod_version=0.12.1-alpha.28' in gradle\nassert 'VERSION = \"0.12.1-alpha.28\"' in main\nassert '\"version\": \"0.12.1-alpha.28\"' in index",
            "assert 'mod_version=0.12.1-alpha.29' in gradle\nassert 'VERSION = \"0.12.1-alpha.29\"' in main\nassert '\"version\": \"0.12.1-alpha.29\"' in index")
t=t.replace("for token in ['formulaFrame','schoolFormula','anchorFormula','skyRitual','meteor_swarm','runeRing','brokenBand','fusionFormula']:",
            "for token in ['formulaFrame','schoolFormula','anchorFormula','skyRitual','meteorRitual','sigilRangeScale','meteor_swarm','runeRing','brokenBand','fusionFormula']:")
t=t.replace("assert 'add(0,28*(1-easeIn(t)),0)' in director", "assert 'fallHeight=42.0' in director and 'prismaticWallFrame' in director")
t=t.replace("for token in ['drawSpine','circleIndex','browserViewport','detail()','spellTile','primaryAction','drawLoadout','enableScissor','mouseScrolled']:",
            "for token in ['drawSpine','circleIndex','circleStep','compact()','browserViewport','detailWidth','detail()','spellTile','primaryAction','drawLoadout','enableScissor','mouseScrolled']:")
t=t.replace("assert 'Math.max(125,Math.min(205,b.w()/3))' in grimoire", "assert 'Math.max(22,Math.min(29' not in grimoire\nassert 'MAX_FRAME = 9000' in tracker and 'MAX_ENTRY = 2800' in tracker\nassert '!\"prismatic_wall\".equals(v.spell.id())' in tracker")
t=t.replace("mage_gear=text(magic/'MageGearService.java')", "light=text(magic/'ArcaneLightService.java')\nfor token in ['Blocks.LIGHT','LightBlock.LEVEL','illuminate','clearAll']:\n    assert token in light, f'Light world-illumination regression: {token}'\nassert 'ArcaneLightService.illuminate(player,1800)' in text(magic/'ExpandedSpellEffects.java')\nmage_gear=text(magic/'MageGearService.java')",1)
write(p,t)

# jar audit
p='tools/verify_jar.py';t=read(p)
t=t.replace('    "kr/moonseungjun/arcanecircle/magic/WorldMagicService.class",',
            '    "kr/moonseungjun/arcanecircle/magic/WorldMagicService.class",\n    "kr/moonseungjun/arcanecircle/magic/ArcaneLightService.class",')
write(p,t)

# Project contract gets a stable alpha.29 behavior note; a direct connector commit will later trigger canonical CI.
project=read('PROJECT.md').rstrip()+'''\n\n## Alpha.29 runtime contracts\n- Grimoire layout must fit all six spine tabs and all nine circle indices at high GUI scale without overlap.\n- Sigil radius reacts to final range by spell geometry family; it is not a raw 1:1 range circle.\n- Light uses temporary vanilla Light blocks and must clean them on expiry/session/dimension/server shutdown.\n- Prismatic rendering is bounded by per-entry and per-frame primitive caps.\n'''
write('PROJECT.md',project)

# The one-shot maintenance mechanism must not survive into the canonical tree.
(repo/'.github/scripts/arcane_alpha29_20260813.py').unlink()
(repo/'.github/workflows/maintenance-arcane29-20260813.yml').unlink()
print('Arcane Circle alpha.29 patch applied')
