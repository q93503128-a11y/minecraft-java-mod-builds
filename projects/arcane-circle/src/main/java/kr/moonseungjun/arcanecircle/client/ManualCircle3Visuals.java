package kr.moonseungjun.arcanecircle.client;

import net.minecraft.world.phys.Vec3;

import static kr.moonseungjun.arcanecircle.client.ManualSpellVisuals.*;

/** Third-circle visuals: mature combat signatures and persistent utility marks. */
final class ManualCircle3Visuals {
    private ManualCircle3Visuals() {}

    static void draw(String id, ArcaneWorldMesh.Builder m, Context c) {
        double p=c.reveal(),t=c.elapsed();ArcaneWorldMesh.Basis g=ground(),f=face(c);
        switch(id){
            case "fireball" -> {
                Vec3 hub=c.direction().scale(.86);double r=.34+.34*p;m.polygon(f,hub,r,5,t*.18,.72F);m.star(f,hub,r*.82,r*.34,5,-t*.15,.42F);m.orb(hub,r*.34,16,1.08F,.18F);if(c.release()){m.beamPrism(Vec3.ZERO,c.direction(),f,Math.max(2.8,c.target().length()),.085,1.04F,.15F);m.star(g,c.target().add(0,.04,0),1.35,.48,8,t*.20,.64F);m.circle(g,c.target().add(0,.05,0),1.75,42,.38F);}
            }
            case "lightning_bolt" -> {
                Vec3 hub=c.direction().scale(.72);for(int i=0;i<3;i++){Vec3 n=hub.add(c.direction().scale(i*.30));m.polygon(f,n,.24+i*.08,4,Math.PI/4+t*(i%2==0?.13:-.10),.54F);}if(c.release()){Vec3 start=Vec3.ZERO,end=c.target();Vec3 prev=start;for(int i=1;i<=8;i++){double q=i/8.0;Vec3 n=start.add(end.subtract(start).scale(q)).add(f.right().scale(Math.sin(i*2.13)*.12)).add(f.up().scale(Math.cos(i*1.71)*.10));m.line(prev,n,i%3==0?.92F:.58F);prev=n;}m.polygon(f,end,.46,6,-t*.18,.58F);}
            }
            case "fly" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;for(int s:new int[]{-1,1}){Vec3 root=base.add(s*.16,1.18,0);for(int i=0;i<4;i++){Vec3 joint=base.add(s*(.52+i*.20),1.58-i*.12,.06*Math.sin(t*1.5+i));Vec3 tip=base.add(s*(.95+i*.22),1.30-i*.18,.10*Math.cos(t*1.2+i));m.line(root,joint,i==0?.64F:.36F);m.line(joint,tip,.30F);}}m.helix(base.add(0,.04,0),new Vec3(0,1,0),f,1.55,.36,2,34,.30F,true);
            }
            case "haste" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;m.circle(g,base.add(0,.04,0),.98,48,.58F);for(int i=0;i<12;i++){double a=i*Math.PI*2/12+t*.28;Vec3 a0=base.add(g.point(a,.77)),a1=base.add(g.point(a,.99));m.line(a0,a1,i%3==0?.54F:.26F);}m.helix(base.add(0,.05,0),new Vec3(0,1,0),f,1.55,.38,2,34,.34F,true);
            }
            case "dispel_magic" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=.62+.26*p;m.circle(f,center,r,36,.52F);m.polygon(f,center,r*.72,4,Math.PI/4-t*.12,.42F);m.line(center.add(f.point(Math.PI/4,r*.88)),center.add(f.point(Math.PI*5/4,r*.88)),.72F);m.line(center.add(f.point(Math.PI*3/4,r*.88)),center.add(f.point(Math.PI*7/4,r*.88)),.72F);if(c.release())m.brokenBand(f,center,r*.94,r*1.12,40,5,1.0F,.08F);
            }
            case "vampiric_touch" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p);Vec3 self=Vec3.ZERO.add(0,.85,0),victim=target.add(0,.85,0);m.circle(f,self,.42,26,.38F);m.circle(f,victim,.52,30,.52F);m.line(victim,self,.54F);for(int i=0;i<3;i++){double a=t*.31+i*2.09;Vec3 n=victim.add(f.point(a,.48));m.line(n,self.add(f.point(-a,.24)),.24F);}if(c.release())m.runeGlyph(f,self,.18,0x3305,t*.08,.38F);
            }
            case "slow" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=1.12+.28*p;m.brokenBand(g,center.add(0,.04,0),r*.82,r,46,5,1.02F,.10F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 outer=center.add(g.point(a,r)),inner=center.add(g.point(a+(i%2==0?.09:-.07),r*.48));m.line(outer,inner,i%2==0?.46F:.28F);}m.line(center.add(0,.06,0),center.add(g.point(-Math.PI/2+t*.06,r*.42)).add(0,.06,0),.60F);
            }
            case "protection_from_energy" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;for(int i=0;i<5;i++){double a=i*Math.PI*2/5-t*.20;Vec3 n=base.add(g.point(a,.98)).add(0,.92+.16*Math.sin(t+i),0);m.diamond(f,n,.27,a+t*.06,1.12F,.18F);m.line(n,base.add(0,.92,0),.22F);}m.polygon(g,base.add(0,.04,0),.78,5,t*.05,.36F);
            }
            case "sleet_storm" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=1.65+.55*p;Vec3 sky=center.add(0,2.8+1.2*p,0);m.polygon(g,sky,r*.72,6,t*.06,.48F);m.brokenBand(g,sky,r*.52,r*.76,42,6,1.02F,.08F);for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.03;Vec3 n=sky.add(g.point(a,r*.60));m.line(n,center.add(g.point(a+.12,r*.42)),i%2==0?.46F:.28F);}if(c.release())m.circle(g,center.add(0,.03,0),r,48,.30F);
            }
            case "blink" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=.88+.18*p;m.polygon(g,base.add(0,.03,0),r,4,Math.PI/4+t*.11,.52F);for(int i=0;i<4;i++){double a=Math.PI/4+i*Math.PI/2;Vec3 n=base.add(g.point(a,r));Vec3 shifted=n.add(g.point(a+.38,.24));m.line(n,shifted,.46F);m.runeGlyph(g,shifted,.12,0x33B0+i*17,-a,.30F);}m.arc(g,base.add(0,.035,0),r*.63,-t*.18,Math.PI*1.35,28,.28F);
            }
            default -> throw new IllegalStateException("Circle3 visual missing: "+id);
        }
    }
}
