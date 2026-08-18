package kr.moonseungjun.arcanecircle.client;

import net.minecraft.world.phys.Vec3;

import static kr.moonseungjun.arcanecircle.client.ManualSpellVisuals.*;

/** First-circle visuals: compact, readable and deliberately not overloaded. */
final class ManualCircle1Visuals {
    private ManualCircle1Visuals() {}

    static void draw(String id, ArcaneWorldMesh.Builder m, Context c) {
        double p=c.reveal(), t=c.elapsed(); ArcaneWorldMesh.Basis g=ground(), f=face(c);
        switch(id){
            case "magic_missile" -> {
                double r=.34+.16*p; Vec3 hub=c.direction().scale(.72);
                for(int i=0;i<3;i++){double a=t*.34+i*Math.PI*2/3;Vec3 n=hub.add(f.point(a,r));m.diamond(f,n,.11,a,1.12F,.18F);m.line(n,n.add(c.direction().scale(.34+.20*p)),.58F);}
                if(c.release()){for(int i=0;i<3;i++){double a=i*Math.PI*2/3;Vec3 o=f.point(a,.14);Vec3 e=c.target().add(f.point(a,.22));m.line(o,e,i==0?.88F:.52F);}}
            }
            case "fire_bolt" -> {
                Vec3 hub=c.direction().scale(.72);double r=.26+.22*p;m.polygon(f,hub,r,3,t*.25,.72F);m.circle(f,hub,r*.48,18,.42F);
                if(c.release()){m.beamPrism(Vec3.ZERO,c.direction(),f,Math.max(2.4,c.target().length()),.065,1.08F,.22F);m.star(f,c.target(),.36,.14,3,-t*.35,.68F);}
            }
            case "ray_of_frost" -> {
                Vec3 hub=c.direction().scale(.68);double r=.34+.12*p;m.polygon(f,hub,r,6,t*.06,.64F);for(int i=0;i<6;i++){double a=i*Math.PI/3;m.line(hub,hub.add(f.point(a,r)),i%2==0?.58F:.34F);}
                if(c.release()){m.beamPrism(Vec3.ZERO,c.direction(),f,Math.max(2.5,c.target().length()),.045,1.05F,.18F);m.polygon(f,c.target(),.42,6,-t*.12,.62F);}
            }
            case "shield" -> {
                Vec3 center=(c.release()?c.target():Vec3.ZERO).add(0,1.05,.58);double r=.58*(.72+.28*p);m.polygon(f,center,r,6,t*.04,.88F);m.polygon(f,center,r*.72,6,-t*.05+.18,.46F);m.line(center.add(f.point(0,r*.72)),center.add(f.point(Math.PI,r*.72)),.28F);
            }
            case "feather_fall" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;for(int s:new int[]{-1,1})for(int i=0;i<3;i++){Vec3 a=base.add(s*.14,.48+i*.34,0),b=base.add(s*(.48+i*.12),.64+i*.31,.04*Math.sin(t+i));m.line(a,b,i==0?.58F:.34F);m.line(b,b.add(s*.14,-.10,0),.28F);}m.arc(g,base.add(0,.04,0),.62,t*.12,Math.PI*1.30,24,.34F);
            }
            case "light" -> {
                Vec3 center=(c.release()?c.target():Vec3.ZERO).add(0,1.1,0);double r=.30+.10*p;m.star(f,center,r,r*.42,5,-t*.08,.58F);m.circle(f,center,r*.34,18,.36F);if(c.release())m.orb(center,r*.32,14,1.08F,.16F);
            }
            case "grease" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=1.25+.55*p;m.arc(g,center.add(0,.03,0),r,t*.10,Math.PI*1.55,34,.42F);m.arc(g,center.add(0,.035,0),r*.66,-t*.13+1.1,Math.PI*1.35,28,.30F);for(int i=0;i<4;i++){double a=i*Math.PI/2+.45;m.line(center.add(g.point(a,r*.25)),center.add(g.point(a+.18,r*.92)),.22F);}
            }
            case "sleep" -> {
                Vec3 center=(c.release()?c.target():c.target().scale(p)).add(0,.06,0);double r=.92+.22*p;m.arc(g,center,r,-.30+t*.03,Math.PI*1.25,34,.52F);m.arc(g,center.add(.18,0,.05),r*.62,.18-t*.025,Math.PI*1.15,28,.30F);Vec3 moon=center.add(.0,.82,0);m.arc(f,moon,.28,.55,Math.PI*1.15,20,.48F);
            }
            case "thunderwave" -> {
                Vec3 tip=c.direction().scale(.28);double len=1.4+1.2*p;for(int i=0;i<5;i++){double a=(i-2)*.28;Vec3 end=c.direction().scale(len).add(f.right().scale(Math.sin(a)*len*.62));m.line(tip,end,i==2?.82F:.42F);}if(c.release()){for(int k=0;k<3;k++)m.arc(f,c.direction().scale(.7+k*.55),.52+k*.34,-.85,1.70,26,.52F-k*.08F);}
            }
            case "mage_armor" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;for(int i=0;i<4;i++){double a=Math.PI/4+i*Math.PI/2+t*.05;Vec3 node=base.add(g.point(a,.68)).add(0,.70+(i%2)*.55,0);m.diamond(f,node,.24,-a,1.08F,.16F);m.runeGlyph(f,node,.11,0x110+i*31,a,.34F);}m.circle(g,base.add(0,.04,0),.72,32,.30F);
            }
            default -> throw new IllegalStateException("Circle1 visual missing: "+id);
        }
    }
}
