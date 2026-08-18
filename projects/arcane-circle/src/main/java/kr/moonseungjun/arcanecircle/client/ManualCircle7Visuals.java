package kr.moonseungjun.arcanecircle.client;

import net.minecraft.world.phys.Vec3;

import static kr.moonseungjun.arcanecircle.client.ManualSpellVisuals.*;

/** Seventh-circle visuals: each fortress-breaking/planar spell owns a full ritual composition. */
final class ManualCircle7Visuals {
    private ManualCircle7Visuals() {}

    static void draw(String id, ArcaneWorldMesh.Builder m, Context c) {
        double p=c.reveal(),t=c.elapsed();ArcaneWorldMesh.Basis g=ground(),f=face(c),x=sideX(),z=sideZ();
        switch(id){
            case "delayed_blast_fireball" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=2.4+.8*p,compress=c.release()?Math.max(.20,1.0-Math.min(1,c.age()/.42)*.72):1.0;m.polygon(g,center.add(0,.04,0),r,10,t*.018,.50F);m.runeChords(g,center.add(0,.045,0),r*.76,10,3,-t*.022,.34F);m.circle(g,center.add(0,.05,0),r*.42*compress,48,.62F);m.orb(center.add(0,.45,0),.52*compress,20,1.12F,.18F);for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.03;Vec3 n=center.add(g.point(a,r*.88));m.runeGlyph(g,n,.15,0x7700+i*71,-a,.28F);m.line(n,center.add(g.point(a,r*.42*compress)),.26F);}if(c.release()&&c.age()>.42){double burst=1+Math.min(1,(c.age()-.42)/.28)*2.2;m.circle(g,center.add(0,.05,0),r*.58*burst,64,.56F);m.star(g,center.add(0,.055,0),r*.42*burst,r*.18*burst,10,t*.12,.44F);}
            }
            case "etherealness" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=1.24+.18*p;m.circle(g,base.add(0,.04,0),r,48,.40F);m.circle(x,base.add(0,1.0,0),r*.78,44,.34F);m.circle(z,base.add(0,1.0,0),r*.62,40,.30F);for(int i=0;i<7;i++){double a=i*Math.PI*2/7+t*.025;Vec3 n=base.add(g.point(a,r*.92)).add(0,.35+.20*(i%4),0);m.diamond(f,n,.14,a,1.08F,.12F);m.line(n,n.add(f.normal().scale((i%2==0?1:-1)*.34)),.22F);}m.brokenBand(g,base.add(0,.045,0),r*1.03,r*1.13,52,7,1.0F,.08F);
            }
            case "finger_of_death" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p),mark=target.add(0,1.05,0);double r=1.15+.28*p;m.polygon(f,mark,r,7,t*.018,.52F);m.star(f,mark,r*.80,r*.22,7,-t*.022,.44F);m.runeChords(f,mark,r*.58,7,3,t*.016,.30F);for(int i=0;i<7;i++){double a=i*Math.PI*2/7;Vec3 n=mark.add(f.point(a,r));m.runeGlyph(f,n,.13,0x7702+i*79,-a,.26F);}if(c.release()){m.beamPrism(Vec3.ZERO,c.direction(),f,Math.max(3,c.target().length()),.075,1.14F,.18F);m.line(mark.add(f.point(0,r*1.18)),mark.add(f.point(Math.PI,r*1.18)),.62F);}
            }
            case "fire_storm" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p),sky=center.add(0,8.5+2*p,0);double r=3.4+1.0*p;m.polygon(g,sky,r,12,t*.012,.50F);m.runeChords(g,sky,r*.78,12,5,-t*.014,.34F);m.brokenBand(g,sky,r*.52,r*.62,60,7,1.02F,.08F);for(int i=0;i<6;i++){double a=i*Math.PI/3+t*.01;Vec3 node=sky.add(g.point(a,r*.72));m.runeGlyph(g,node,.20,0x7703+i*83,-a,.30F);Vec3 hit=center.add(g.point(a+.14*(i%2==0?1:-1),r*.52));m.line(node,hit,i%2==0?.54F:.34F);if(c.release())m.shard(hit.add(0,1.8,0),new Vec3(0,1,0),g,3.6,.22,1.10F,.20F);}if(c.release())m.star(g,center.add(0,.04,0),r*.66,r*.28,12,t*.04,.38F);
            }
            case "forcecage" -> {
                Vec3 base=c.release()?c.target():c.target().scale(p);double r=1.55+.22*p,h=3.2*p+.35;m.polygon(g,base.add(0,.04,0),r,10,t*.018,.48F);m.polygon(g,base.add(0,h,0),r,10,-t*.018,.42F);for(int i=0;i<10;i++){double a=i*Math.PI/5;Vec3 foot=base.add(g.point(a,r)),top=base.add(g.point(a+.06*(i%2==0?1:-1),r)).add(0,h,0);m.line(foot,top,i%2==0?.56F:.32F);if(i%2==0)m.line(foot,base.add(0,h*.5,0),.20F);}m.circle(x,base.add(0,h*.52,0),r*.74,44,.30F);if(c.release())m.runeGlyph(g,base.add(0,h+.04,0),.48,0x7704,-t*.04,.40F);
            }
            case "plane_shift" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=1.7+.28*p,h=3.5*p+.8;Vec3 center=base.add(0,h*.52,0);m.polygon(f,center,r,7,t*.025,.52F);m.polygon(f,center,r*.74,5,-t*.032+.20,.38F);m.runeChords(f,center,r*.55,7,3,t*.015,.28F);for(int i=0;i<7;i++){double a=i*Math.PI*2/7;Vec3 n=center.add(f.point(a,r*.90));m.runeGlyph(f,n,.14,0x7705+i*89,-a,.26F);}m.polygon(g,base.add(0,.04,0),r*.88,7,-t*.018,.36F);m.circle(x,center,r*.46,38,.26F);
            }
            case "prismatic_spray" -> {
                Vec3 hub=c.direction().scale(.90);double r=.92+.26*p;m.polygon(f,hub,r,7,t*.018,.50F);m.star(f,hub,r*.82,r*.34,7,-t*.022,.42F);m.circle(f,hub,r*.42,34,.28F);for(int i=0;i<7;i++){double a=i*Math.PI*2/7;Vec3 n=hub.add(f.point(a,r*.90));m.diamond(f,n,.14,a,1.08F,.13F);m.line(n,hub,.22F);}if(c.release())m.polygon(f,c.target(),1.12,7,-t*.035,.34F);
            }
            case "reverse_gravity" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=3.0+.85*p;m.polygon(g,center.add(0,.04,0),r,12,t*.014,.46F);m.runeChords(g,center.add(0,.045,0),r*.76,12,5,-t*.012,.30F);for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.02;Vec3 foot=center.add(g.point(a,r*.68)),top=foot.add(0,2.2+.35*(i%3),0);m.line(foot,top,i%2==0?.48F:.28F);m.diamond(g,top,.16,a,1.08F,.12F);}m.circle(x,center.add(0,1.25,0),r*.34,42,.26F);m.circle(z,center.add(0,1.25,0),r*.46,46,.30F);
            }
            case "simulacrum" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO,echo=base.add(-1.05,1.02,.16);double r=.72+.20*p;m.diamond(f,echo,r,t*.018,1.10F,.16F);m.polygon(f,echo,r*.74,6,-t*.025,.34F);m.runeGlyph(f,echo,r*.31,0x7708,t*.035,.38F);m.line(base.add(0,.88,0),echo,.28F);m.polygon(g,base.add(0,.04,0),1.12,7,t*.016,.34F);for(int i=0;i<7;i++){double a=i*Math.PI*2/7;Vec3 n=base.add(g.point(a,1.02));m.runeGlyph(g,n,.10,0x7718+i*31,-a,.22F);}
            }
            case "teleport" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=2.0+.35*p,h=4.1*p+.9;Vec3 center=base.add(0,h*.50,0);m.polygon(f,center,r,8,t*.018,.52F);m.polygon(f,center,r*.76,6,-t*.022+.16,.36F);m.runeChords(f,center,r*.55,8,3,t*.014,.28F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=center.add(f.point(a,r*.90));m.runeGlyph(f,n,.14,0x7709+i*97,-a,.26F);}m.polygon(g,base.add(0,.04,0),r*.84,8,-t*.014,.34F);m.brokenBand(g,base.add(0,.045,0),r*.92,r*1.03,52,7,1.02F,.08F);
            }
            default -> throw new IllegalStateException("Circle7 visual missing: "+id);
        }
    }
}
