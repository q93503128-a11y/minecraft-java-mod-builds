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
            }
            case "prismatic_wall" -> {
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
            }
            case "shapechange" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=1.58+.27*p;
                m.polygon(g,base.add(0,.04,0),r,9,t*.012,.46F);m.runeChords(g,base.add(0,.045,0),r*.74,9,4,-t*.014,.30F);
                for(int layer=0;layer<7;layer++){double y=.18+layer*.29,rr=.48+layer*.115+.07*Math.sin(t*1.1+layer);m.polygon(g,base.add(0,y,0),rr,3+(layer%5),t*(layer%2==0?.12:-.10)+layer*.18,layer%2==0?.48F:.30F);}
                // Combat-form claws separate self Shapechange from target rewrite True Polymorph.
                Vec3 shoulder=base.add(0,1.35,0);
                for(int side:new int[]{-1,1}){Vec3 root=shoulder.add(f.right().scale(side*.24));Vec3 claw=root.add(f.right().scale(side*.78)).add(0,.18,0);m.line(root,claw,.34F);for(int k=0;k<3;k++)m.line(claw,claw.add(f.right().scale(side*(.28+.08*k))).add(f.up().scale(.18-.12*k)),.24F);}
                for(int i=0;i<9;i++){double a=i*Math.PI*2/9-t*.012;Vec3 node=base.add(g.point(a,r*.92)).add(0,.60+.18*(i%4),0);m.runeGlyph(f,node,.13,0x9903+i*173,-a,.26F);m.line(node,base.add(0,1.0,0),.20F);}
                m.circle(x,base.add(0,1.05,0),.94,44,.26F);m.circle(z,base.add(0,1.05,0),1.14,48,.28F);
            }
            case "time_stop" -> {
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
            }
            case "true_polymorph" -> {
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
            }
            case "weird" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=4.2+1.3*p;
                m.polygon(g,center.add(0,.04,0),r,13,t*.007,.46F);m.runeChords(g,center.add(0,.045,0),r*.76,13,5,-t*.009,.34F);m.brokenBand(g,center.add(0,.05,0),r*.58,r*.66,74,9,1.02F,.08F);
                for(int i=0;i<13;i++){double a=i*Math.PI*2/13+t*.006;Vec3 node=center.add(g.point(a,r*.86)).add(0,.35+.18*(i%5),0);m.star(f,node,.28+.03*(i%3),.09,4+(i%3),-a+t*.014,.34F);m.runeGlyph(f,node,.11,0x9906+i*191,a,.22F);m.line(node,center.add(0,.92,0),.18F);}
                // Three mutually tilted nightmare eyes keep the domain from reading as a flat floor circle.
                Vec3[] eyes={center.add(0,1.55,0),center.add(1.45,2.05,.35),center.add(-1.25,1.78,-.55)};ArcaneWorldMesh.Basis[] planes={f,x,z};
                for(int i=0;i<3;i++){m.runeChords(planes[i],eyes[i],.78-.10*i,9,4,-t*(.012+.004*i)+i,.30F);m.circle(planes[i],eyes[i],.26+.04*i,24,.26F);m.line(eyes[i],center.add(0,.42,0),.20F);}
                if(c.release())for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 a0=center.add(g.point(a,r*.30)),a1=center.add(g.point(a+.22*(i%2==0?1:-1),r*.92)).add(0,.18*(i%3),0);m.line(a0,a1,.34F);}
            }
            case "wish" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=3.8+1.05*p;
                m.star(g,base.add(0,.04,0),r,r*.46,9,-t*.006,.52F);m.polygon(g,base.add(0,.045,0),r*.82,9,t*.008,.44F);m.runeChords(g,base.add(0,.05,0),r*.64,9,4,-t*.007,.34F);m.brokenBand(g,base.add(0,.055,0),r*.90,r,90,9,1.04F,.10F);
                Vec3 crown=base.add(0,4.35,0),mid=base.add(0,2.25,0);m.polygon(g,crown,r*.64,9,-t*.006+.16,.40F);m.polygon(g,mid,r*.48,9,t*.005,.30F);m.circle(x,crown,r*.30,50,.28F);m.circle(z,crown,r*.40,54,.30F);
                for(int i=0;i<9;i++){double a=i*Math.PI*2/9+t*.005;Vec3 low=base.add(g.point(a,r*.88)).add(0,.45+.20*(i%3),0),middle=mid.add(g.point(a+.08*(i%2==0?1:-1),r*.42)),high=crown.add(g.point(a,r*.34));m.circle(g,low,.24,20,.30F);m.polygon(g,low,.18,3+i%4,-a+t*.010,.28F);m.runeGlyph(g,low,.09,0x9907+i*193,a,.24F);m.line(low,middle,.24F);m.line(middle,high,.24F);m.runeGlyph(g,middle,.10,0x99B7+i*223,-a,.22F);}
                m.runeGlyph(g,crown,.36,0x99F7,-t*.010,.40F);if(c.release())m.runeChords(g,mid,r*.34,9,4,0,.32F);
            }
            case "gate" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double h=6.2+2.1*p,w=3.1+.70*p;Vec3 center=base.add(0,h*.52,0);
                // Seven receding portal frames and perspective rails make the aperture visibly deep.
                for(int depth=0;depth<7;depth++){Vec3 frame=center.add(f.normal().scale(depth*.24));double rr=w*(1.0-depth*.075);m.polygon(f,frame,rr,12-depth%2*4,t*(depth%2==0?.008:-.007)+depth*.10,depth==0?.56F:.22F);if(depth<3)m.runeChords(f,frame,rr*.62,12,5,-t*.006+depth*.14,.20F);}
                for(int i=0;i<9;i++){double a=i*Math.PI*2/9;Vec3 node=center.add(f.point(a,w*.92));m.circle(f,node,.18,18,.28F);m.runeGlyph(f,node,.10,0x9908+i*197,-a,.24F);m.line(node,center.add(f.normal().scale(1.35)).add(f.point(a,w*.46)),.22F);}
                for(double a:new double[]{0,Math.PI/2,Math.PI,Math.PI*1.5})m.line(center.add(f.point(a,w*.96)),center.add(f.normal().scale(1.55)).add(f.point(a,w*.42)),.30F);
                m.polygon(g,base.add(0,.04,0),w*.94,12,-t*.007,.40F);m.runeChords(g,base.add(0,.045,0),w*.72,12,5,t*.006,.28F);m.brokenBand(g,base.add(0,.05,0),w*1.00,w*1.12,72,9,1.04F,.09F);m.line(base.add(f.right().scale(-w)),base.add(f.right().scale(-w)).add(0,h,0),.56F);m.line(base.add(f.right().scale(w)),base.add(f.right().scale(w)).add(0,h,0),.56F);
            }
            case "foresight" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO,eye=base.add(0,2.18,0);double r=1.66+.28*p;
                m.circle(f,eye,r,58,.50F);m.polygon(f,eye,r*.80,9,t*.008,.40F);m.runeChords(f,eye,r*.58,9,4,-t*.010,.34F);m.runeGlyph(f,eye,r*.24,0x9909,t*.012,.40F);m.polygon(g,base.add(0,.04,0),1.76,12,-t*.006,.42F);m.runeChords(g,base.add(0,.045,0),1.40,12,5,t*.005,.30F);m.circle(x,base.add(0,1.05,0),1.10,48,.28F);m.circle(z,base.add(0,1.05,0),1.32,52,.30F);
                for(int i=0;i<9;i++){double a=i*Math.PI*2/9-t*.008;Vec3 node=base.add(g.point(a,1.62)).add(0,.32+.20*(i%4),0);m.runeGlyph(f,node,.12,0x9919+i*199,-a,.24F);m.line(node,eye,.18F);}
                // Four possible future trajectories split from the present and reconnect to the observing eye.
                for(int i=0;i<4;i++){double lateral=(i-1.5)*.26,sway=.10*Math.sin(t*.7+i);Vec3 a0=base.add(0,.16,0),a1=a0.add(c.direction().scale(.78)).add(f.right().scale(lateral)),a2=a0.add(c.direction().scale(1.55)).add(f.right().scale(lateral*1.65+sway)).add(0,.16+.08*i,0);m.line(a0,a1,i==1||i==2?.30F:.20F);m.line(a1,a2,.20F);m.line(a2,eye,.16F);}
                if(c.release()){double futureOffset=.18+.06*Math.sin(t*2.1);for(int i=0;i<4;i++){double a=t*.18+i*Math.PI/2;Vec3 a0=base.add(g.point(a,1.05)),a1=base.add(g.point(a+futureOffset,1.52)).add(0,.18,0);m.line(a0,a1,.24F);}}
            }
            default -> throw new IllegalStateException("Circle9 visual missing: "+id);
        }
    }
}
