package kr.moonseungjun.arcanecircle.client;

import net.minecraft.world.phys.Vec3;

import static kr.moonseungjun.arcanecircle.client.ManualSpellVisuals.*;

/** Sixth-circle visuals: every major spell has its own authored high-magic grammar. */
final class ManualCircle6Visuals {
    private ManualCircle6Visuals() {}

    static void draw(String id, ArcaneWorldMesh.Builder m, Context c) {
        double p=c.reveal(),t=c.elapsed();ArcaneWorldMesh.Basis g=ground(),f=face(c),x=sideX();
        switch(id){
            case "disintegrate" -> {
                Vec3 hub=c.direction().scale(.88);double r=.58+.24*p;m.polygon(f,hub,r,6,t*.04,.62F);m.runeChords(f,hub,r*.72,6,2,-t*.05,.38F);m.circle(f,hub,r*.38,28,.30F);for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 n=hub.add(f.point(a,r));m.runeGlyph(f,n,.11,0x6600+i*43,-a,.26F);}if(c.release()){double len=Math.max(3.0,c.target().length());m.beamPrism(Vec3.ZERO,c.direction(),f,len,.075,1.18F,.20F);for(int i=1;i<=5;i++){Vec3 n=c.direction().scale(len*i/6.0);m.polygon(f,n,.24,6,t*.08+i*.22,.34F);}}
            }
            case "globe_of_invulnerability" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO,center=base.add(0,1.15,0);double r=1.65+.30*p;m.sphere(center,r,7,.62F);m.polygon(g,base.add(0,.04,0),r*1.02,10,t*.025,.48F);m.runeChords(g,base.add(0,.045,0),r*.78,10,3,-t*.025,.30F);m.circle(x,center,r*.74,48,.30F);for(int i=0;i<6;i++){double a=i*Math.PI/3+t*.04;Vec3 n=base.add(g.point(a,r*.92)).add(0,.78+.18*(i%2),0);m.runeGlyph(f,n,.13,0x6601+i*61,a,.28F);}
            }
            case "mass_suggestion" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=2.05+.55*p;m.polygon(g,center.add(0,.04,0),r,12,t*.018,.46F);m.runeChords(g,center.add(0,.045,0),r*.72,12,5,-t*.016,.30F);Vec3 hub=center.add(0,.95,0);m.runeGlyph(f,hub,.34,0x6602,t*.05,.46F);for(int i=0;i<8;i++){double a=i*Math.PI/4-t*.04;Vec3 n=center.add(g.point(a,r*.78)).add(0,.55+.14*(i%3),0);m.diamond(f,n,.17,a,1.08F,.14F);m.line(n,hub,.22F);}if(c.release())m.circle(x,hub,.72,40,.26F);
            }
            case "move_earth" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=2.4+.9*p;m.polygon(g,center.add(0,.03,0),r,8,t*.012,.48F);for(int i=0;i<12;i++){double a=i*Math.PI*2/12+.03*Math.sin(i*2.1);Vec3 a0=center.add(g.point(a,r*.18)),a1=center.add(g.point(a+.09*Math.sin(i),r*.62)),a2=center.add(g.point(a-.06*Math.cos(i*.8),r));m.line(a0,a1,i%3==0?.52F:.28F);m.line(a1,a2,.24F);if(c.release()&&i%2==0)m.shard(a1.add(0,.35,0),new Vec3(0,1,0),g,.75+.10*(i%3),.14,1.04F,.16F);}m.brokenBand(g,center.add(0,.035,0),r*.70,r*.82,56,7,1.02F,.08F);
            }
            case "sunbeam" -> {
                Vec3 hub=c.direction().scale(.90);double r=.72+.24*p;m.star(f,hub,r,r*.46,8,-t*.035,.62F);m.polygon(f,hub,r*.62,8,t*.025,.38F);m.circle(f,hub,r*.28,28,.34F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=hub.add(f.point(a,r));m.line(n,hub,i%2==0?.44F:.28F);}if(c.release()){double len=Math.max(3.0,c.target().length());m.beamPrism(Vec3.ZERO,c.direction(),f,len,.16,1.18F,.24F);m.star(f,c.target(),.74,.28,8,t*.12,.52F);}
            }
            case "true_seeing" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO,eye=base.add(0,1.72,0);m.arc(f,eye,1.02,.10,Math.PI*.82,38,.58F);m.arc(f,eye,1.02,Math.PI+ .10,Math.PI*.82,38,.58F);m.circle(f,eye,.34,30,.64F);m.runeChords(f,eye,.26,8,3,-t*.035,.34F);m.polygon(g,base.add(0,.04,0),1.32,12,t*.018,.42F);for(int i=0;i<6;i++){double a=i*Math.PI/3-t*.03;Vec3 n=base.add(g.point(a,1.18)).add(0,.42+.18*(i%3),0);m.runeGlyph(f,n,.12,0x6605+i*47,a,.26F);}
            }
            case "freezing_sphere" -> {
                Vec3 hub=c.direction().scale(.92);double r=.48+.38*p;m.orb(hub,r*.48,18,1.08F,.16F);m.polygon(f,hub,r,8,t*.04,.58F);m.runeChords(f,hub,r*.72,8,3,-t*.045,.34F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=hub.add(f.point(a,r));m.shard(n,c.direction(),f,.32,.055,1.04F,.14F);}if(c.release()){m.circle(g,c.target().add(0,.03,0),2.35,58,.42F);m.polygon(g,c.target().add(0,.035,0),1.72,8,-t*.03,.36F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=c.target().add(g.point(a,1.45));m.shard(n.add(0,.46,0),new Vec3(0,1,0),g,.92,.10,1.05F,.16F);}}
            }
            case "eyebite" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p),eye=target.add(0,1.42,0);double r=.92+.24*p;m.arc(f,eye,r,.06,Math.PI*.88,36,.56F);m.arc(f,eye,r,Math.PI+.06,Math.PI*.88,36,.56F);m.circle(f,eye,r*.28,28,.62F);m.star(f,eye,r*.52,r*.18,6,-t*.05,.36F);for(int i=0;i<6;i++){double a=i*Math.PI/3+t*.025;Vec3 n=eye.add(f.point(a,r*.92));m.runeGlyph(f,n,.11,0x6607+i*53,-a,.24F);}if(c.release())m.brokenBand(g,target.add(0,.03,0),.92,1.12,42,6,1.0F,.08F);
            }
            case "flesh_to_stone" -> {
                Vec3 base=c.release()?c.target():c.target().scale(p);m.polygon(g,base.add(0,.04,0),1.18,8,t*.02,.44F);for(int i=0;i<10;i++){double a=i*Math.PI/5+t*.025;Vec3 n=base.add(g.point(a,.76+.10*(i%2))).add(0,.30+.18*(i%6),0);m.polygon(f,n,.24+.025*(i%3),5,-a,.42F);if(i%2==0)m.line(n,base.add(0,.92,0),.20F);}if(c.release()){m.circle(f,base.add(0,1.0,0),.88,42,.30F);m.polygon(f,base.add(0,1.0,0),.68,6,-t*.02,.26F);}
            }
            case "circle_of_death" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=2.65+.85*p;m.circle(g,center.add(0,.03,0),r,68,.58F);m.polygon(g,center.add(0,.035,0),r*.82,12,t*.018,.42F);m.runeChords(g,center.add(0,.04,0),r*.62,12,5,-t*.016,.32F);for(int i=0;i<12;i++){double a=i*Math.PI/6;Vec3 n=center.add(g.point(a,r*.92));m.runeGlyph(g,n,.16,0x6609+i*67,-a,.30F);m.line(n,center.add(g.point(a+.08*(i%2==0?1:-1),r*.48)),.22F);}if(c.release())m.brokenBand(g,center.add(0,.045,0),r*1.02,r*1.12,64,8,1.04F,.10F);
            }
            default -> throw new IllegalStateException("Circle6 visual missing: "+id);
        }
    }
}
