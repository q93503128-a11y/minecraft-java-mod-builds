package kr.moonseungjun.arcanecircle.client;

import net.minecraft.world.phys.Vec3;

import static kr.moonseungjun.arcanecircle.client.ManualSpellVisuals.*;

/** Second-circle visuals: each spell owns a separate silhouette. */
final class ManualCircle2Visuals {
    private ManualCircle2Visuals() {}

    static void draw(String id, ArcaneWorldMesh.Builder m, Context c) {
        double p=c.reveal(),t=c.elapsed();ArcaneWorldMesh.Basis g=ground(),f=face(c);
        switch(id){
            case "scorching_ray" -> {
                Vec3 hub=c.direction().scale(.78);for(int i=0;i<3;i++){double a=(i-1)*.42;Vec3 n=hub.add(f.right().scale(a));m.polygon(f,n,.22+.07*p,3,t*(i%2==0?.19:-.16)+i,.58F);m.line(n,n.add(c.direction().scale(.42)),.48F);}if(c.release())for(int i=0;i<3;i++){double off=(i-1)*.16;m.line(f.right().scale(off),c.target().add(f.right().scale(off*.35)),i==1?.88F:.52F);}
            }
            case "misty_step" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=.68+.20*p;m.polygon(g,base.add(0,.03,0),r,4,Math.PI/4+t*.10,.62F);m.polygon(g,base.add(0,.035,0),r*.64,4,-t*.12,.38F);for(int i=0;i<4;i++){double a=Math.PI/4+i*Math.PI/2;Vec3 n=base.add(g.point(a,r));m.line(n,n.add(0,.55+.10*i,0),.34F);}
            }
            case "web" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=1.45+.35*p;m.polygon(g,center.add(0,.03,0),r,8,t*.025,.46F);m.circle(g,center.add(0,.035,0),r*.62,32,.28F);for(int i=0;i<8;i++){double a=i*Math.PI/4;m.line(center.add(g.point(a,r*.18)),center.add(g.point(a,r)),i%2==0?.42F:.26F);}for(int i=0;i<4;i++)m.arc(g,center.add(0,.04,0),r*(.28+i*.17),i*.55,Math.PI*.88,20,.20F);
            }
            case "mirror_image" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;for(int i=0;i<3;i++){double a=i*Math.PI*2/3+t*.55;Vec3 node=base.add(g.point(a,1.05)).add(0,.92+.12*Math.sin(t*1.4+i),0);m.diamond(f,node,.34,a,1.06F,.17F);m.line(node.add(0,-.34,0),node.add(0,.34,0),.30F);}m.arc(g,base.add(0,.03,0),.86,t*.20,Math.PI*1.55,30,.28F);
            }
            case "invisibility" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;for(int i=0;i<3;i++){double y=.32+i*.42;m.arc(g,base.add(0,y,0),.62+i*.10,t*(.42-i*.08)+i*.7,Math.PI*1.22,28,i==0?.46F:.28F);}m.arc(f,base.add(0,1.05,0),.48,-t*.18,Math.PI*1.15,24,.24F);
            }
            case "gust_of_wind" -> {
                Vec3 o=c.direction().scale(.24);double len=1.8+1.5*p;for(int i=0;i<4;i++){double rr=.18+i*.13;Vec3 start=o.add(f.point(i*.9,rr));m.helix(start,c.direction(),f,len,rr,2,30,i==0?.48F:.28F,true);}if(c.release())m.cone(Vec3.ZERO,c.direction(),f,Math.max(2.6,c.range()*.62),1.4,8,3,.34F);
            }
            case "hold_person" -> {
                Vec3 base=(c.release()?c.target():c.target().scale(p));double r=.82+.20*p;m.band(g,base.add(0,.03,0),r*.82,r,30,1.04F,.10F);for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 foot=base.add(g.point(a,r)),top=foot.add(0,1.85*p+.15,0);m.line(foot,top,i%3==0?.68F:.38F);m.line(top,base.add(0,1.85*p+.15,0),.24F);}if(c.release())m.runeGlyph(g,base.add(0,1.95,0),.36,0x2202,-t*.08,.42F);
            }
            case "shatter" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=.74+.36*p;m.polygon(f,center,r*.54,6,t*.10,.58F);for(int i=0;i<8;i++){double a=i*Math.PI/4+.12;Vec3 mid=center.add(f.point(a,r*.45)),tip=center.add(f.point(a+(i%2==0?.10:-.08),r));m.line(center,mid,i%2==0?.52F:.30F);m.line(mid,tip,.34F);}if(c.release())m.circle(g,center.add(0,.03,0),1.25,36,.42F);
            }
            case "blur" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;for(int i=-2;i<=2;i++){double x=i*.15+Math.sin(t*4.8+i)*.06;m.arc(f,base.add(x,1.0,0),.68,t*.74+i,Math.PI*1.12,24,i==0?.52F:.22F);}m.arc(g,base.add(0,.04,0),.74,-t*.31,Math.PI*1.28,28,.26F);
            }
            case "levitate" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;for(int i=0;i<4;i++){double y=.06+i*.34;double r=.78-i*.11;m.arc(g,base.add(0,y,0),r,t*(.26-i*.04)+i*.4,Math.PI*1.55,30,i==0?.52F:.28F);}for(int i=0;i<4;i++){double a=i*Math.PI/2+t*.10;Vec3 n=base.add(g.point(a,.62));m.line(n,n.add(0,1.25,0),.24F);}
            }
            default -> throw new IllegalStateException("Circle2 visual missing: "+id);
        }
    }
}
