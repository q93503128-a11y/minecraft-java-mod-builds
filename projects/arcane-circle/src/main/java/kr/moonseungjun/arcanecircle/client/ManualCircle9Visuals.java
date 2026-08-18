package kr.moonseungjun.arcanecircle.client;

import net.minecraft.world.phys.Vec3;

import static kr.moonseungjun.arcanecircle.client.ManualSpellVisuals.*;

/** Ninth-circle visuals: ten individually authored archmage rites; no rank-template decoration. */
final class ManualCircle9Visuals {
    private ManualCircle9Visuals() {}

    static void draw(String id, ArcaneWorldMesh.Builder m, Context c) {
        double p=c.reveal(),t=c.elapsed();ArcaneWorldMesh.Basis g=ground(),f=face(c),x=sideX(),z=sideZ();
        switch(id){
            case "meteor_swarm" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p),sky=center.add(0,22.0+7.0*p,0);double r=6.6+2.4*p;
                m.polygon(g,sky,r,12,t*.006,.52F);m.polygon(g,sky,r*.82,9,-t*.008+.13,.44F);m.runeChords(g,sky,r*.66,12,5,t*.005,.34F);m.brokenBand(g,sky,r*.90,r,108,10,1.05F,.10F);m.circle(x,sky,r*.42,58,.30F);m.circle(z,sky,r*.55,64,.34F);
                for(int i=0;i<9;i++){double a=i*Math.PI*2/9-t*.005;Vec3 node=sky.add(g.point(a,r*.86));m.circle(g,node,.32,22,.34F);m.polygon(g,node,.24,3+i%4,a+t*.010,.30F);m.runeGlyph(g,node,.12,0x9900+i*157,-a,.28F);m.line(node,sky.add(g.point(a+.06*(i%2==0?1:-1),r*.48)),.24F);}
                for(int i=0;i<16;i++){double a=i*Math.PI*2/16+.07*Math.sin(i*1.7);double d=r*(.18+.035*(i%8));Vec3 hit=center.add(g.point(a,d));Vec3 start=sky.add(g.point(a*.72,r*(.22+.025*(i%7))));m.line(start,hit,i%4==0?.58F:.32F);if(c.release()){double fall=Math.max(0,1-Math.min(1,c.age()*(1.35+.025*i)));Vec3 meteor=hit.add(0,fall*(sky.y-center.y),0);m.orb(meteor,.28+.025*(i%4),14,1.12F,.17F);m.line(meteor,meteor.add(0,1.4+.15*(i%3),0),.26F);}}
                if(c.release()){m.star(g,center.add(0,.04,0),r*.72,r*.30,16,t*.018,.44F);m.circle(g,center.add(0,.045,0),r*.94,76,.40F);}
            }
            case "power_word_kill" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p),mark=target.add(0,1.10,0);double r=1.65+.30*p;
                m.polygon(f,mark,r,8,Math.PI/8,.56F);m.polygon(f,mark,r*.78,4,Math.PI/4-t*.012,.48F);m.polygon(f,mark,r*.58,8,t*.016,.38F);m.runeChords(f,mark,r*.50,8,3,-t*.014,.32F);m.circle(x,mark,r*.44,46,.28F);m.circle(z,mark,r*.34,42,.24F);
                for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 node=mark.add(f.point(a,r*.90));m.runeGlyph(f,node,.14,0x9901+i*163,-a,.28F);if(i%2==0)m.line(node,mark.add(f.point(a+Math.PI/4,r*.44)),.24F);}if(c.release()){m.line(mark.add(f.right().scale(-r*1.22)),mark.add(f.right().scale(r*1.22)),.82F);m.line(mark.add(f.up().scale(-r*1.22)),mark.add(f.up().scale(r*1.22)),.82F);m.diamond(f,mark,r*.36,-t*.12,1.14F,.18F);}
            }
            case "prismatic_wall" -> {
                Vec3 base=c.release()?c.target():c.target().scale(p);double width=7.4+4.0*p,h=4.6+1.9*p;Vec3 right=f.right();m.polygon(g,base.add(0,.04,0),width*.42,14,t*.008,.46F);m.runeChords(g,base.add(0,.045,0),width*.34,14,5,-t*.010,.30F);for(int i=0;i<7;i++){double x0=(i-3)*width/7.0;Vec3 foot=base.add(right.scale(x0)),top=foot.add(0,h,0);m.line(foot,top,i==3?.68F:.38F);Vec3 node=foot.add(0,h*(.40+.055*(i%3)),0);m.diamond(f,node,width*.035,t*(i%2==0?.08:-.07),1.10F,.12F);m.runeGlyph(f,node,width*.018,0x9902+i*167,-t*.025,.24F);}m.line(base.add(right.scale(-width*.5)),base.add(right.scale(width*.5)),.46F);m.line(base.add(right.scale(-width*.5)).add(0,h,0),base.add(right.scale(width*.5)).add(0,h,0),.52F);
            }
            case "shapechange" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=1.55+.25*p;m.polygon(g,base.add(0,.04,0),r,9,t*.012,.46F);m.runeChords(g,base.add(0,.045,0),r*.74,9,4,-t*.014,.30F);for(int layer=0;layer<7;layer++){double y=.18+layer*.29,rr=.48+layer*.115+.07*Math.sin(t*1.1+layer);m.polygon(g,base.add(0,y,0),rr,3+(layer%5),t*(layer%2==0?.12:-.10)+layer*.18,layer%2==0?.48F:.30F);}for(int i=0;i<9;i++){double a=i*Math.PI*2/9-t*.012;Vec3 node=base.add(g.point(a,r*.92)).add(0,.60+.18*(i%4),0);m.runeGlyph(f,node,.13,0x9903+i*173,-a,.26F);m.line(node,base.add(0,1.0,0),.20F);}m.circle(x,base.add(0,1.05,0),.92,44,.26F);m.circle(z,base.add(0,1.05,0),1.12,48,.28F);
            }
            case "time_stop" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=4.4+1.25*p;m.circle(g,base.add(0,.04,0),r,92,.56F);m.polygon(g,base.add(0,.045,0),r*.88,12,t*.006,.48F);m.runeChords(g,base.add(0,.05,0),r*.68,12,5,-t*.005,.34F);m.brokenBand(g,base.add(0,.055,0),r*.52,r*.60,72,8,1.03F,.09F);for(int i=0;i<12;i++){double a=i*Math.PI/6;Vec3 tick=base.add(g.point(a,r*.93));m.line(tick,base.add(g.point(a,r*.82)),i%3==0?.62F:.32F);if(i%3==0)m.runeGlyph(g,tick,.18,0x9904+i*179,-a,.26F);}Vec3 hub=base.add(0,1.38,0);m.circle(x,hub,r*.34,56,.30F);m.circle(z,hub,r*.46,62,.34F);m.polygon(x,hub,r*.26,12,-t*.004,.30F);m.line(hub,hub.add(g.point(-Math.PI/2+t*.012,r*.28)),.66F);m.line(hub,hub.add(g.point(-Math.PI/2-t*.030,r*.20)),.46F);
            }
            case "true_polymorph" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p);double r=1.82+.30*p;m.polygon(g,target.add(0,.04,0),r,10,t*.010,.48F);m.runeChords(g,target.add(0,.045,0),r*.74,10,3,-t*.012,.32F);for(int layer=0;layer<8;layer++){double y=.16+layer*.27,rr=.52+layer*.105+.08*Math.sin(t*.9+layer);int sides=3+(layer*2%7);m.polygon(g,target.add(0,y,0),rr,sides,t*(layer%2==0?.10:-.09)+layer*.21,layer%2==0?.46F:.28F);}m.circle(x,target.add(0,1.10,0),1.02,48,.28F);m.circle(z,target.add(0,1.10,0),1.24,52,.30F);for(int i=0;i<10;i++){double a=i*Math.PI/5;Vec3 node=target.add(g.point(a,r*.90)).add(0,.44+.16*(i%5),0);m.runeGlyph(f,node,.13,0x9905+i*181,-a,.24F);}
            }
            case "weird" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=4.1+1.25*p;m.polygon(g,center.add(0,.04,0),r,13,t*.007,.46F);m.runeChords(g,center.add(0,.045,0),r*.76,13,5,-t*.009,.34F);m.brokenBand(g,center.add(0,.05,0),r*.58,r*.66,74,9,1.02F,.08F);for(int i=0;i<13;i++){double a=i*Math.PI*2/13+t*.006;Vec3 node=center.add(g.point(a,r*.86)).add(0,.35+.18*(i%5),0);m.star(f,node,.28+.03*(i%3),.09,4+(i%3),-a+t*.014,.34F);m.runeGlyph(f,node,.11,0x9906+i*191,a,.22F);m.line(node,center.add(0,.92,0),.18F);}Vec3 eye=center.add(0,1.5,0);m.runeChords(f,eye,.82,9,4,-t*.012,.30F);if(c.release())m.circle(x,eye,1.18,48,.28F);
            }
            case "wish" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=3.7+1.0*p;m.star(g,base.add(0,.04,0),r,r*.46,9,-t*.006,.52F);m.polygon(g,base.add(0,.045,0),r*.82,9,t*.008,.44F);m.runeChords(g,base.add(0,.05,0),r*.64,9,4,-t*.007,.34F);m.brokenBand(g,base.add(0,.055,0),r*.90,r,90,9,1.04F,.10F);Vec3 crown=base.add(0,4.2,0);m.polygon(g,crown,r*.62,9,-t*.006+.16,.38F);m.circle(x,crown,r*.30,50,.28F);m.circle(z,crown,r*.40,54,.30F);for(int i=0;i<9;i++){double a=i*Math.PI*2/9+t*.005;Vec3 node=base.add(g.point(a,r*.88)).add(0,.45+.35*(i%3),0);m.circle(g,node,.24,20,.30F);m.polygon(g,node,.18,3+i%4,-a+t*.010,.28F);m.runeGlyph(g,node,.09,0x9907+i*193,a,.24F);m.line(node,crown.add(g.point(a,r*.34)),.22F);}m.runeGlyph(g,crown,.34,0x99F7,-t*.010,.38F);
            }
            case "gate" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double h=6.0+2.0*p,w=3.0+.65*p;Vec3 center=base.add(0,h*.52,0);m.polygon(f,center,w,12,t*.008,.54F);m.polygon(f,center,w*.82,8,-t*.010+.12,.44F);m.runeChords(f,center,w*.64,12,5,t*.007,.34F);m.circle(x,center,w*.50,52,.28F);m.circle(z,center,w*.62,58,.32F);for(int i=0;i<9;i++){double a=i*Math.PI*2/9;Vec3 node=center.add(f.point(a,w*.90));m.circle(f,node,.18,18,.28F);m.runeGlyph(f,node,.10,0x9908+i*197,-a,.24F);m.line(node,center.add(f.point(a+.06*(i%2==0?1:-1),w*.52)),.22F);}m.polygon(g,base.add(0,.04,0),w*.92,12,-t*.007,.40F);m.runeChords(g,base.add(0,.045,0),w*.70,12,5,t*.006,.28F);m.brokenBand(g,base.add(0,.05,0),w*.98,w*1.10,72,9,1.04F,.09F);m.line(base.add(f.right().scale(-w)),base.add(f.right().scale(-w)).add(0,h,0),.56F);m.line(base.add(f.right().scale(w)),base.add(f.right().scale(w)).add(0,h,0),.56F);
            }
            case "foresight" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO,eye=base.add(0,2.15,0);double r=1.62+.26*p;m.circle(f,eye,r,58,.50F);m.polygon(f,eye,r*.80,9,t*.008,.40F);m.runeChords(f,eye,r*.58,9,4,-t*.010,.34F);m.runeGlyph(f,eye,r*.24,0x9909,t*.012,.40F);m.polygon(g,base.add(0,.04,0),1.72,12,-t*.006,.42F);m.runeChords(g,base.add(0,.045,0),1.36,12,5,t*.005,.30F);m.circle(x,base.add(0,1.05,0),1.08,48,.28F);m.circle(z,base.add(0,1.05,0),1.30,52,.30F);for(int i=0;i<9;i++){double a=i*Math.PI*2/9-t*.008;Vec3 node=base.add(g.point(a,1.58)).add(0,.32+.20*(i%4),0);m.runeGlyph(f,node,.12,0x9919+i*199,-a,.24F);m.line(node,eye,.18F);}if(c.release()){double future=.18+.06*Math.sin(t*2.1);for(int i=0;i<4;i++){double a=t*.18+i*Math.PI/2;Vec3 a0=base.add(g.point(a,1.05)),a1=base.add(g.point(a+.future,1.52)).add(0,.18,0);m.line(a0,a1,.24F);}}
            }
            default -> throw new IllegalStateException("Circle9 visual missing: "+id);
        }
    }
}
