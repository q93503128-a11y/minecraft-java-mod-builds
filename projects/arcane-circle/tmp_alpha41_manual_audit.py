#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent
CLIENT = ROOT / 'src/main/java/kr/moonseungjun/arcanecircle/client'


def replace_case(path: Path, name: str, next_name: str | None, body: str) -> None:
    s = path.read_text(encoding='utf-8')
    start = s.index(f'            case "{name}" -> {{')
    end = s.index(f'            case "{next_name}" -> {{', start) if next_name else s.index('            default ->', start)
    path.write_text(s[:start] + body.rstrip() + '\n' + s[end:], encoding='utf-8')


# 7C planar/sustained identities: deliberately different spatial grammars.
p = CLIENT / 'ManualCircle7Visuals.java'
replace_case(p, 'etherealness', 'finger_of_death', r'''            case "etherealness" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;
                double r=1.24+.18*p, phase=c.release()?0.0:t*.025;
                m.circle(g,base.add(0,.04,0),r,48,.40F);
                m.brokenBand(g,base.add(0,.045,0),r*1.03,r*1.13,52,7,1.0F,.08F);
                // Two phase-separated copies of the body-space seal: half a step outside the material plane.
                for(int layer=-1;layer<=1;layer+=2){
                    Vec3 ghost=base.add(f.normal().scale(layer*(.22+.05*p))).add(0,1.02,0);
                    m.polygon(f,ghost,r*.66,7,phase*layer+layer*.11,.34F);
                    m.runeChords(f,ghost,r*.48,7,3,-phase*layer,.24F);
                    for(int i=0;i<7;i++){
                        double a=i*Math.PI*2/7+phase;
                        Vec3 n=ghost.add(f.point(a,r*.62));
                        m.line(n,n.add(f.normal().scale(layer*.30)),.18F);
                    }
                }
                m.circle(x,base.add(0,1.0,0),r*.78,44,.34F);
                m.circle(z,base.add(0,1.0,0),r*.62,40,.30F);
            }''')
replace_case(p, 'plane_shift', 'prismatic_spray', r'''            case "plane_shift" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;
                double r=1.72+.30*p,h=3.6*p+.85;
                Vec3 center=base.add(0,h*.52,0);
                // Seven parallel world-sheets: crossing planes, not a generic teleport doorway.
                for(int layer=-3;layer<=3;layer++){
                    Vec3 sheet=center.add(f.normal().scale(layer*.16));
                    double rr=r*(1.0-Math.abs(layer)*.055);
                    m.polygon(f,sheet,rr,7,t*(layer%2==0?.020:-.016)+layer*.13,layer==0?.54F:.24F);
                    if(Math.abs(layer)<=1)m.runeChords(f,sheet,rr*.58,7,3,-t*.012+layer*.21,.22F);
                }
                for(int i=0;i<7;i++){
                    double a=i*Math.PI*2/7;
                    Vec3 near=center.add(f.normal().scale(-.48)).add(f.point(a,r*.82));
                    Vec3 far=center.add(f.normal().scale(.48)).add(f.point(a+.10*(i%2==0?1:-1),r*.72));
                    m.line(near,far,i%2==0?.32F:.20F);
                    m.runeGlyph(f,near,.12,0x7705+i*89,-a,.24F);
                }
                m.polygon(g,base.add(0,.04,0),r*.90,7,-t*.018,.36F);
                m.line(base.add(0,.06,0),base.add(0,h,0),.42F);
            }''')
replace_case(p, 'simulacrum', 'teleport', r'''            case "simulacrum" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;
                Vec3 echo=base.add(-1.12,1.05,.18), heart=echo.add(0,.08,0);
                double r=.76+.22*p;
                // A complete contracted effigy replaces the old single side-diamond.
                m.polygon(f,echo,r,6,t*.018,.46F);
                m.runeChords(f,echo,r*.72,6,2,-t*.025,.32F);
                m.circle(f,heart,r*.30,24,.34F);
                m.runeGlyph(f,heart,r*.18,0x7708,t*.035,.38F);
                Vec3 head=echo.add(0,.72,0), chest=echo, hip=echo.add(0,-.54,0);
                m.circle(f,head,.22,18,.28F);
                m.line(head,chest,.30F);m.line(chest,hip,.30F);
                m.line(chest,chest.add(f.right().scale(.46)).add(0,.12,0),.24F);
                m.line(chest,chest.add(f.right().scale(-.46)).add(0,.12,0),.24F);
                m.line(hip,hip.add(f.right().scale(.30)).add(0,-.56,0),.24F);
                m.line(hip,hip.add(f.right().scale(-.30)).add(0,-.56,0),.24F);
                m.line(base.add(0,.92,0),heart,.34F);
                m.polygon(g,base.add(0,.04,0),1.18,7,t*.016,.34F);
                for(int i=0;i<7;i++){double a=i*Math.PI*2/7;Vec3 n=base.add(g.point(a,1.06));m.runeGlyph(g,n,.10,0x7718+i*31,-a,.22F);}
            }''')
replace_case(p, 'teleport', None, r'''            case "teleport" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;
                double r=2.05+.38*p,h=4.2*p+.95;
                Vec3 center=base.add(0,h*.50,0);
                // Coordinate-lock frames converge on one destination; unlike Plane Shift's separated sheets.
                for(int depth=0;depth<5;depth++){
                    Vec3 frame=center.add(f.normal().scale((depth-2)*.12));
                    double rr=r*(1.0-depth*.075);
                    m.polygon(f,frame,rr,8,t*(depth%2==0?.018:-.016)+depth*.11,depth==0?.52F:.28F);
                }
                m.runeChords(f,center,r*.54,8,3,t*.014,.28F);
                for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=center.add(f.point(a,r*.90));m.runeGlyph(f,n,.14,0x7709+i*97,-a,.26F);m.line(n,center.add(f.point(a,r*.48)),.20F);}
                m.polygon(g,base.add(0,.04,0),r*.86,8,-t*.014,.34F);
                m.brokenBand(g,base.add(0,.045,0),r*.94,r*1.05,52,7,1.02F,.08F);
                if(c.release()){
                    double collapse=Math.max(.18,1.0-c.age()*2.4);
                    m.polygon(f,center,r*.46*collapse,4,Math.PI/4,.52F);
                    m.line(base.add(0,.06,0),center,.28F);
                }
            }''')

# 8C: semantic identities instead of repeated circle/polygon arrangements.
p = CLIENT / 'ManualCircle8Visuals.java'
replace_case(p, 'antimagic_field', 'clone', r'''            case "antimagic_field" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;
                double r=2.55+.48*p, spin=c.release()?0.0:t*.012;
                m.polygon(g,base.add(0,.04,0),r,12,spin,.50F);
                m.runeChords(g,base.add(0,.045,0),r*.78,12,5,-spin,.34F);
                // Broken null circuits: nothing closes cleanly inside a field that cancels magic.
                for(int level=0;level<3;level++){
                    double y=.42+level*.62,rr=r*(.72-level*.12);
                    m.arc(g,base.add(0,y,0),rr,.22+level*.83,Math.PI*1.08,38,.32F);
                    m.arc(g,base.add(0,y+.02,0),rr,-2.28+level*.51,Math.PI*.58,24,.22F);
                }
                Vec3 core=base.add(0,1.18,0);
                m.circle(x,core,r*.36,46,.28F);m.circle(z,core,r*.48,50,.30F);
                for(int i=0;i<8;i++){
                    double a=i*Math.PI/4;
                    Vec3 outer=base.add(g.point(a,r*.90)).add(0,.30+.18*(i%4),0);
                    Vec3 inner=core.add(g.point(a+.18*(i%2==0?1:-1),r*.18));
                    m.runeGlyph(f,outer,.15,0x8800+i*101,-a,.28F);
                    m.line(outer,inner,.24F);
                    m.line(inner,inner.add(f.normal().scale((i%2==0?1:-1)*.28)),.18F);
                }
                if(c.release())m.brokenBand(g,base.add(0,.05,0),r*.50,r*.60,70,5,1.04F,.09F);
            }''')
replace_case(p, 'clone', 'control_weather', r'''            case "clone" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;
                Vec3 reserve=base.add(0,.70,-.88), heart=reserve.add(0,.12,0);
                double r=.96+.24*p;
                // Rear life-reserve vessel and self seal make the substitution contract visible while active.
                m.polygon(f,reserve,r,8,t*.022,.50F);
                m.polygon(f,reserve,r*.72,4,Math.PI/4-t*.018,.38F);
                m.runeChords(f,reserve,r*.54,8,3,-t*.020,.30F);
                m.circle(f,heart,r*.25,24,.36F);m.runeGlyph(f,heart,r*.17,0x8801,t*.035,.40F);
                Vec3 self=base.add(0,.92,0);
                m.circle(f,self,.44,30,.30F);m.runeGlyph(f,self,.18,0x88C1,-t*.028,.30F);
                m.line(self,heart,.36F);
                for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=base.add(g.point(a,1.28));m.runeGlyph(g,n,.11,0x8811+i*37,a,.24F);m.line(n,(i%2==0?heart:self),.18F);}
                m.polygon(g,base.add(0,.04,0),1.42,8,-t*.015,.38F);
                m.circle(x,base.add(0,1.02,0),.88,42,.24F);
            }''')
replace_case(p, 'demiplane', 'dominate_monster', r'''            case "demiplane" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;
                double r=2.55+.46*p,h=5.1*p+1.15;
                Vec3 center=base.add(0,h*.50,0);
                // Six receding frames create visible depth into a pocket space.
                for(int depth=0;depth<6;depth++){
                    Vec3 frame=center.add(f.normal().scale(depth*.22));
                    double rr=r*(1.0-depth*.085);
                    m.polygon(f,frame,rr,10-depth%2*2,t*(depth%2==0?.012:-.010)+depth*.12,depth==0?.54F:.24F);
                    if(depth<3)m.runeChords(f,frame,rr*.60,10,3,-t*.008+depth*.17,.20F);
                }
                for(int i=0;i<8;i++){
                    double a=i*Math.PI/4;
                    Vec3 near=center.add(f.point(a,r*.88));
                    Vec3 far=center.add(f.normal().scale(1.1)).add(f.point(a,r*.50));
                    m.line(near,far,i%2==0?.30F:.18F);
                    m.runeGlyph(f,near,.15,0x8803+i*109,-a,.24F);
                }
                m.polygon(g,base.add(0,.04,0),r*.88,10,-t*.010,.36F);
                m.brokenBand(g,base.add(0,.045,0),r*.96,r*1.07,62,8,1.02F,.09F);
            }''')
replace_case(p, 'dominate_monster', 'earthquake', r'''            case "dominate_monster" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p);
                Vec3 head=target.add(0,1.62,0), crown=head.add(0,.72,0);
                double r=1.32+.28*p;
                m.polygon(f,head,r,8,t*.014,.50F);m.star(f,head,r*.82,r*.32,8,-t*.018,.40F);
                m.runeChords(f,head,r*.62,8,3,t*.012,.32F);m.runeGlyph(f,head,r*.28,0x8804,-t*.035,.42F);
                m.star(g,crown,.72,.30,8,t*.022,.42F);m.circle(g,crown,.42,28,.28F);
                for(int i=0;i<8;i++){
                    double a=i*Math.PI/4;
                    Vec3 command=crown.add(g.point(a,.66));
                    Vec3 bind=target.add(g.point(a,.66)).add(0,.64+.12*(i%3),0);
                    m.runeGlyph(f,command,.12,0x8814+i*113,a,.24F);
                    m.line(command,bind,i%2==0?.34F:.20F);m.line(bind,head,.18F);
                }
                m.polygon(g,target.add(0,.04,0),1.42,8,-t*.012,.34F);
                m.circle(x,target.add(0,1.0,0),.94,42,.24F);
            }''')
replace_case(p, 'maze', 'sunburst', r'''            case "maze" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p), center=target.add(0,.06,0);
                double r=2.25+.38*p;
                // Four broken square corridors with different exits: this must read as a maze, not a ritual disc.
                for(int layer=0;layer<4;layer++){
                    double rr=r*(1.0-layer*.18), rot=Math.PI/4+layer*.17;
                    Vec3[] q=new Vec3[4];
                    for(int i=0;i<4;i++)q[i]=center.add(g.point(rot+i*Math.PI/2,rr));
                    int gap=(layer*3+1)%4;
                    for(int side=0;side<4;side++)if(side!=gap)m.line(q[side],q[(side+1)%4],layer==0?.48F:.28F);
                    if(layer<3){Vec3 outer=q[(gap+1)%4],inner=center.add(g.point(rot+(gap+1)*Math.PI/2,rr*.72));m.line(outer,inner,.24F);}
                }
                Vec3 hub=target.add(0,1.12,0);
                m.circle(x,hub,1.12,46,.28F);m.circle(z,hub,.86,42,.24F);
                for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=target.add(g.point(a,r*.88)).add(0,.28+.12*(i%4),0);m.runeGlyph(f,n,.13,0x8808+i*137,-a,.24F);m.line(n,hub,.18F);}
                if(c.release())m.polygon(f,hub,.72,6,-t*.010+.20,.30F);
            }''')

# 9C: release semantics and sustained authority must be unique spell-by-spell.
p = CLIENT / 'ManualCircle9Visuals.java'
replace_case(p, 'power_word_kill', 'prismatic_wall', r'''            case "power_word_kill" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p),mark=target.add(0,1.10,0);
                double r=1.68+.32*p, snap=c.release()?Math.max(.08,1.0-c.age()*5.2):1.0;
                m.polygon(f,mark,r,8,Math.PI/8,.56F);m.polygon(f,mark,r*.78,4,Math.PI/4-t*.012,.48F);
                m.polygon(f,mark,r*.58,8,t*.016,.38F);m.runeChords(f,mark,r*.50,8,3,-t*.014,.32F);
                m.circle(x,mark,r*.44,46,.28F);m.circle(z,mark,r*.34,42,.24F);
                for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 node=mark.add(f.point(a,r*.90));m.runeGlyph(f,node,.14,0x9901+i*163,-a,.28F);if(i%2==0)m.line(node,mark.add(f.point(a+Math.PI/4,r*.44)),.24F);}
                if(c.release()){
                    // Four execution bars snap shut at once: compact ninth-circle authority.
                    for(int i=0;i<4;i++){double a=i*Math.PI/2;Vec3 outer=mark.add(f.point(a,r*1.35));Vec3 inner=mark.add(f.point(a,r*.12*snap));m.line(outer,inner,.92F);}
                    m.diamond(f,mark,r*.36*snap,-t*.12,1.16F,.20F);m.runeGlyph(f,mark,r*.18,0x99D1,0,.52F);
                }
            }''')
replace_case(p, 'prismatic_wall', 'shapechange', r'''            case "prismatic_wall" -> {
                Vec3 base=c.release()?c.target():c.target().scale(p);double width=7.6+4.2*p,h=4.8+2.0*p;
                Vec3 right=f.right(), topCenter=base.add(0,h+.28,0);
                m.polygon(g,base.add(0,.04,0),width*.43,14,t*.008,.46F);m.runeChords(g,base.add(0,.045,0),width*.35,14,5,-t*.010,.30F);
                m.arc(f,topCenter,width*.48,Math.PI,-Math.PI,54,.38F);
                for(int i=0;i<7;i++){
                    double x0=(i-3)*width/7.0;Vec3 foot=base.add(right.scale(x0)),top=foot.add(0,h,0),mid=foot.add(0,h*.52,0);
                    m.line(foot,top,i==3?.70F:.40F);m.diamond(f,mid,width*.036,t*(i%2==0?.08:-.07),1.10F,.12F);m.runeGlyph(f,mid,width*.019,0x9902+i*167,-t*.025,.24F);
                    Vec3 crown=top.add(0,.24,0);m.polygon(f,crown,width*.030,3+(i%3),i*.34-t*.02,.30F);m.runeGlyph(f,crown,width*.014,0x9922+i*211,i*.4,.22F);
                    if(i<6){Vec3 next=base.add(right.scale((i-2)*width/7.0)).add(0,h*.74,0);m.line(mid,next,.20F);}
                }
                m.line(base.add(right.scale(-width*.5)),base.add(right.scale(width*.5)),.48F);m.line(base.add(right.scale(-width*.5)).add(0,h,0),base.add(right.scale(width*.5)).add(0,h,0),.54F);
            }''')
replace_case(p, 'shapechange', 'time_stop', r'''            case "shapechange" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=1.58+.27*p;
                m.polygon(g,base.add(0,.04,0),r,9,t*.012,.46F);m.runeChords(g,base.add(0,.045,0),r*.74,9,4,-t*.014,.30F);
                for(int layer=0;layer<7;layer++){double y=.18+layer*.29,rr=.48+layer*.115+.07*Math.sin(t*1.1+layer);m.polygon(g,base.add(0,y,0),rr,3+(layer%5),t*(layer%2==0?.12:-.10)+layer*.18,layer%2==0?.48F:.30F);}
                // Combat-form claws separate self Shapechange from target rewrite True Polymorph.
                Vec3 shoulder=base.add(0,1.35,0);
                for(int side:new int[]{-1,1}){Vec3 root=shoulder.add(f.right().scale(side*.24));Vec3 claw=root.add(f.right().scale(side*.78)).add(0,.18,0);m.line(root,claw,.34F);for(int k=0;k<3;k++)m.line(claw,claw.add(f.right().scale(side*(.28+.08*k))).add(f.up().scale(.18-.12*k)),.24F);}
                for(int i=0;i<9;i++){double a=i*Math.PI*2/9-t*.012;Vec3 node=base.add(g.point(a,r*.92)).add(0,.60+.18*(i%4),0);m.runeGlyph(f,node,.13,0x9903+i*173,-a,.26F);m.line(node,base.add(0,1.0,0),.20F);}
                m.circle(x,base.add(0,1.05,0),.94,44,.26F);m.circle(z,base.add(0,1.05,0),1.14,48,.28F);
            }''')
replace_case(p, 'time_stop', 'true_polymorph', r'''            case "time_stop" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;
                double r=4.5+1.3*p, spin=c.release()?0.0:t*.006;
                m.circle(g,base.add(0,.04,0),r,96,.58F);m.polygon(g,base.add(0,.045,0),r*.88,12,spin,.48F);
                m.runeChords(g,base.add(0,.05,0),r*.68,12,5,-spin,.34F);m.brokenBand(g,base.add(0,.055,0),r*.52,r*.60,72,8,1.03F,.09F);
                for(int i=0;i<12;i++){double a=i*Math.PI/6;Vec3 tick=base.add(g.point(a,r*.93));m.line(tick,base.add(g.point(a,r*.82)),i%3==0?.62F:.32F);if(i%3==0)m.runeGlyph(g,tick,.18,0x9904+i*179,-a,.26F);if(c.release())m.line(tick,tick.add(0,2.65+.18*(i%3),0),i%3==0?.34F:.18F);}
                Vec3 hub=base.add(0,1.42,0),upper=base.add(0,2.82,0);
                m.circle(x,hub,r*.34,56,.30F);m.circle(z,hub,r*.46,62,.34F);m.polygon(x,hub,r*.26,12,-spin,.30F);
                m.circle(x,upper,r*.22,48,.24F);m.circle(z,upper,r*.30,52,.26F);m.polygon(z,upper,r*.18,12,spin,.24F);
                double hand=c.release()?-.72:-Math.PI/2+t*.012;
                m.line(hub,hub.add(g.point(hand,r*.28)),.68F);m.line(hub,hub.add(g.point(c.release()?1.92:-Math.PI/2-t*.030,r*.20)),.46F);
            }''')
replace_case(p, 'true_polymorph', 'weird', r'''            case "true_polymorph" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p);double r=1.86+.32*p;
                m.polygon(g,target.add(0,.04,0),r,10,t*.010,.48F);m.runeChords(g,target.add(0,.045,0),r*.74,10,3,-t*.012,.32F);
                // Rewrite lattice: horizontal forms are stitched by vertical identity rails.
                for(int layer=0;layer<8;layer++){
                    double y=.16+layer*.27,rr=.52+layer*.105+.08*Math.sin(t*.9+layer);int sides=3+(layer*2%7);
                    m.polygon(g,target.add(0,y,0),rr,sides,t*(layer%2==0?.10:-.09)+layer*.21,layer%2==0?.46F:.28F);
                    if(layer<7)for(int k=0;k<4;k++){double a=k*Math.PI/2+layer*.11;Vec3 a0=target.add(g.point(a,rr)).add(0,y,0),a1=target.add(g.point(a+.12,.52+(layer+1)*.105)).add(0,y+.27,0);m.line(a0,a1,.18F);}
                }
                Vec3 seal=target.add(0,2.42,0);m.polygon(f,seal,.86,10,-t*.012,.34F);m.runeGlyph(f,seal,.28,0x99A5,t*.018,.34F);
                m.circle(x,target.add(0,1.10,0),1.04,48,.28F);m.circle(z,target.add(0,1.10,0),1.26,52,.30F);
                for(int i=0;i<10;i++){double a=i*Math.PI/5;Vec3 node=target.add(g.point(a,r*.90)).add(0,.44+.16*(i%5),0);m.runeGlyph(f,node,.13,0x9905+i*181,-a,.24F);m.line(node,seal,.16F);}
            }''')
replace_case(p, 'weird', 'wish', r'''            case "weird" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=4.2+1.3*p;
                m.polygon(g,center.add(0,.04,0),r,13,t*.007,.46F);m.runeChords(g,center.add(0,.045,0),r*.76,13,5,-t*.009,.34F);m.brokenBand(g,center.add(0,.05,0),r*.58,r*.66,74,9,1.02F,.08F);
                for(int i=0;i<13;i++){double a=i*Math.PI*2/13+t*.006;Vec3 node=center.add(g.point(a,r*.86)).add(0,.35+.18*(i%5),0);m.star(f,node,.28+.03*(i%3),.09,4+(i%3),-a+t*.014,.34F);m.runeGlyph(f,node,.11,0x9906+i*191,a,.22F);m.line(node,center.add(0,.92,0),.18F);}
                // Three mutually tilted nightmare eyes keep the domain from reading as a flat floor circle.
                Vec3[] eyes={center.add(0,1.55,0),center.add(1.45,2.05,.35),center.add(-1.25,1.78,-.55)};ArcaneWorldMesh.Basis[] planes={f,x,z};
                for(int i=0;i<3;i++){m.runeChords(planes[i],eyes[i],.78-.10*i,9,4,-t*(.012+.004*i)+i,.30F);m.circle(planes[i],eyes[i],.26+.04*i,24,.26F);m.line(eyes[i],center.add(0,.42,0),.20F);}
                if(c.release())for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 a0=center.add(g.point(a,r*.30)),a1=center.add(g.point(a+.22*(i%2==0?1:-1),r*.92)).add(0,.18*(i%3),0);m.line(a0,a1,.34F);}
            }''')
replace_case(p, 'wish', 'gate', r'''            case "wish" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=3.8+1.05*p;
                m.star(g,base.add(0,.04,0),r,r*.46,9,-t*.006,.52F);m.polygon(g,base.add(0,.045,0),r*.82,9,t*.008,.44F);m.runeChords(g,base.add(0,.05,0),r*.64,9,4,-t*.007,.34F);m.brokenBand(g,base.add(0,.055,0),r*.90,r,90,9,1.04F,.10F);
                Vec3 crown=base.add(0,4.35,0),mid=base.add(0,2.25,0);m.polygon(g,crown,r*.64,9,-t*.006+.16,.40F);m.polygon(g,mid,r*.48,9,t*.005,.30F);m.circle(x,crown,r*.30,50,.28F);m.circle(z,crown,r*.40,54,.30F);
                for(int i=0;i<9;i++){double a=i*Math.PI*2/9+t*.005;Vec3 low=base.add(g.point(a,r*.88)).add(0,.45+.20*(i%3),0),middle=mid.add(g.point(a+.08*(i%2==0?1:-1),r*.42)),high=crown.add(g.point(a,r*.34));m.circle(g,low,.24,20,.30F);m.polygon(g,low,.18,3+i%4,-a+t*.010,.28F);m.runeGlyph(g,low,.09,0x9907+i*193,a,.24F);m.line(low,middle,.24F);m.line(middle,high,.24F);m.runeGlyph(g,middle,.10,0x99B7+i*223,-a,.22F);}
                m.runeGlyph(g,crown,.36,0x99F7,-t*.010,.40F);if(c.release())m.runeChords(g,mid,r*.34,9,4,0,.32F);
            }''')
replace_case(p, 'gate', 'foresight', r'''            case "gate" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double h=6.2+2.1*p,w=3.1+.70*p;Vec3 center=base.add(0,h*.52,0);
                // Seven receding portal frames and perspective rails make the aperture visibly deep.
                for(int depth=0;depth<7;depth++){Vec3 frame=center.add(f.normal().scale(depth*.24));double rr=w*(1.0-depth*.075);m.polygon(f,frame,rr,12-depth%2*4,t*(depth%2==0?.008:-.007)+depth*.10,depth==0?.56F:.22F);if(depth<3)m.runeChords(f,frame,rr*.62,12,5,-t*.006+depth*.14,.20F);}
                for(int i=0;i<9;i++){double a=i*Math.PI*2/9;Vec3 node=center.add(f.point(a,w*.92));m.circle(f,node,.18,18,.28F);m.runeGlyph(f,node,.10,0x9908+i*197,-a,.24F);m.line(node,center.add(f.normal().scale(1.35)).add(f.point(a,w*.46)),.22F);}
                for(double a:new double[]{0,Math.PI/2,Math.PI,Math.PI*1.5})m.line(center.add(f.point(a,w*.96)),center.add(f.normal().scale(1.55)).add(f.point(a,w*.42)),.30F);
                m.polygon(g,base.add(0,.04,0),w*.94,12,-t*.007,.40F);m.runeChords(g,base.add(0,.045,0),w*.72,12,5,t*.006,.28F);m.brokenBand(g,base.add(0,.05,0),w*1.00,w*1.12,72,9,1.04F,.09F);m.line(base.add(f.right().scale(-w)),base.add(f.right().scale(-w)).add(0,h,0),.56F);m.line(base.add(f.right().scale(w)),base.add(f.right().scale(w)).add(0,h,0),.56F);
            }''')
replace_case(p, 'foresight', None, r'''            case "foresight" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO,eye=base.add(0,2.18,0);double r=1.66+.28*p;
                m.circle(f,eye,r,58,.50F);m.polygon(f,eye,r*.80,9,t*.008,.40F);m.runeChords(f,eye,r*.58,9,4,-t*.010,.34F);m.runeGlyph(f,eye,r*.24,0x9909,t*.012,.40F);m.polygon(g,base.add(0,.04,0),1.76,12,-t*.006,.42F);m.runeChords(g,base.add(0,.045,0),1.40,12,5,t*.005,.30F);m.circle(x,base.add(0,1.05,0),1.10,48,.28F);m.circle(z,base.add(0,1.05,0),1.32,52,.30F);
                for(int i=0;i<9;i++){double a=i*Math.PI*2/9-t*.008;Vec3 node=base.add(g.point(a,1.62)).add(0,.32+.20*(i%4),0);m.runeGlyph(f,node,.12,0x9919+i*199,-a,.24F);m.line(node,eye,.18F);}
                // Four possible future trajectories split from the present and reconnect to the observing eye.
                for(int i=0;i<4;i++){double lateral=(i-1.5)*.26,sway=.10*Math.sin(t*.7+i);Vec3 a0=base.add(0,.16,0),a1=a0.add(c.direction().scale(.78)).add(f.right().scale(lateral)),a2=a0.add(c.direction().scale(1.55)).add(f.right().scale(lateral*1.65+sway)).add(0,.16+.08*i,0);m.line(a0,a1,i==1||i==2?.30F:.20F);m.line(a1,a2,.20F);m.line(a2,eye,.16F);}
                if(c.release()){double futureOffset=.18+.06*Math.sin(t*2.1);for(int i=0;i<4;i++){double a=t*.18+i*Math.PI/2;Vec3 a0=base.add(g.point(a,1.05)),a1=base.add(g.point(a+futureOffset,1.52)).add(0,.18,0);m.line(a0,a1,.24F);}}
            }''')

# Each Prismatic Wall color panel also gets hand-authored internal geometry.
manual = CLIENT / 'ManualSpellVisuals.java'
s = manual.read_text(encoding='utf-8')
start = s.index('    static ArcaneWorldMesh prismaticWallLayer(')
end = s.index('    static ArcaneWorldMesh prismaticSprayLayer(', start)
wall = r'''    static ArcaneWorldMesh prismaticWallLayer(Vec3 direction,Vec3 target,double range,double age,double elapsed,int layer){
        ArcaneWorldMesh.Builder m=ArcaneWorldMesh.builder(340);
        ArcaneWorldMesh.Basis face=ArcaneWorldMesh.Basis.facing(flat(direction));
        double width=Math.max(15.0,range*.54), panel=width/7.0;
        double x0=-width*.5+layer*panel,x1=x0+panel;
        double rise=smooth(clamp(elapsed/.30,0,1)),height=6.6*rise;
        double fade=age<.90?1.0:clamp((1-age)/.10,0,1);
        Vec3 a=target.add(face.right().scale(x0)),b=target.add(face.right().scale(x1)),up=new Vec3(0,height,0);
        m.face(a,b,b.add(up),a.add(up),1.12F,(float)(.34*fade));
        m.line(a,a.add(up),1.04F);m.line(b,b.add(up),1.04F);m.line(a.add(up),b.add(up),.82F);
        Vec3 center=a.add(b).scale(.5).add(0,height*.48,0),low=center.add(0,-height*.30,0),high=center.add(0,height*.32,0);
        m.diamond(face,center,panel*.22,elapsed*(layer%2==0?.22:-.18),1.16F,(float)(.24*fade));m.runeGlyph(face,center,panel*.12,0x901+layer*131,-elapsed*.07,.48F);
        m.polygon(face,low,panel*.18,3+(layer%3),layer*.31+elapsed*.04,.34F);m.polygon(face,high,panel*.16,4+(layer%2),-layer*.27-elapsed*.035,.32F);
        m.runeGlyph(face,low,panel*.075,0xA01+layer*173,layer*.4,.30F);m.runeGlyph(face,high,panel*.070,0xB01+layer*179,-layer*.3,.28F);
        m.line(a.add(0,height*.16,0),b.add(0,height*.84,0),.22F);m.line(b.add(0,height*.16,0),a.add(0,height*.84,0),.22F);
        return m.build();
    }

'''
manual.write_text(s[:start] + wall + s[end:], encoding='utf-8')

# Version/document/audit contracts.
gradle = ROOT / 'gradle.properties'
s = gradle.read_text(encoding='utf-8').replace('mod_version=0.12.1-alpha.40','mod_version=0.12.1-alpha.41')
s = re.sub(r'# alpha\.40[^\n]*', '# alpha.41 manual visual audit: spell-specific spatial depth, null, control, time, execution and persistence grammar', s)
gradle.write_text(s, encoding='utf-8')
main = ROOT / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java'
main.write_text(main.read_text(encoding='utf-8').replace('VERSION = "0.12.1-alpha.40"','VERSION = "0.12.1-alpha.41"'), encoding='utf-8')
index = ROOT / 'src/main/resources/data/arcanecircle/spell_catalog/index.json'
index.write_text(index.read_text(encoding='utf-8').replace('"version": "0.12.1-alpha.40"','"version": "0.12.1-alpha.41"'), encoding='utf-8')
project = ROOT / 'PROJECT.md'
s = project.read_text(encoding='utf-8')
note = '''\n## Alpha.41 hand-authored visual audit\n- High-circle presentation is reviewed spell-by-spell; no rank/school/form decorator may substitute for authored composition.\n- Antimagic Field uses broken null circuits, Maze uses broken square corridors, and Time Stop freezes its own clock geometry after release.\n- Plane Shift, Teleport, Demiplane and Gate deliberately use different spatial-depth grammars.\n- Power Word Kill uses compact execution closure; Wish uses a three-tier nine-contract reality lattice; Foresight keeps visible branching future trajectories.\n- Prismatic Wall panels own upper/lower sub-seals and crossed internal lattice while retaining the authoritative 14-second lifetime.\n'''
if '## Alpha.41 hand-authored visual audit' not in s:
    s += note
project.write_text(s, encoding='utf-8')

audit = ROOT / 'tools/test_current_source.py'
s = audit.read_text(encoding='utf-8').replace('0.12.1-alpha.40','0.12.1-alpha.41')
marker = '# 3P artifact regression: no synthetic player body/filled-box overlay may return.'
block = '''# Alpha.41 manual high-circle audit: actual spell-specific structures, not generic rank decorators.\nfor token in ['for(int depth=0;depth<6;depth++)','Four broken square corridors','spin=c.release()?0.0:t*.012',\n              'Four execution bars snap shut','spin=c.release()?0.0:t*.006','Three mutually tilted nightmare eyes',\n              'Four possible future trajectories','Six receding frames','Seven parallel world-sheets']:\n    assert token in combined, token\nfor token in ['m.polygon(face,low','m.polygon(face,high','m.line(a.add(0,height*.16,0),b.add(0,height*.84,0)']:\n    assert token in manual, token\nassert '## Alpha.41 hand-authored visual audit' in text(root/'PROJECT.md')\n\n'''
if '# Alpha.41 manual high-circle audit:' not in s:
    s = s.replace(marker, block + marker)
audit.write_text(s, encoding='utf-8')
print('alpha.41 manual authored visual refinement applied')
