package kr.moonseungjun.arcanecircle.client;

import net.minecraft.world.phys.Vec3;

import static kr.moonseungjun.arcanecircle.client.ManualSpellVisuals.*;

/** Eighth-circle visuals: regional/reality-warping spells with individually authored 3D rites. */
final class ManualCircle8Visuals {
    private ManualCircle8Visuals() {}

    static void draw(String id, ArcaneWorldMesh.Builder m, Context c) {
        double p=c.reveal(),t=c.elapsed();ArcaneWorldMesh.Basis g=ground(),f=face(c),x=sideX(),z=sideZ();
        switch(id){
            case "antimagic_field" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=2.5+.45*p;m.polygon(g,base.add(0,.04,0),r,12,t*.012,.50F);m.runeChords(g,base.add(0,.045,0),r*.78,12,5,-t*.014,.34F);m.brokenBand(g,base.add(0,.05,0),r*.56,r*.66,66,8,1.02F,.09F);Vec3 core=base.add(0,1.18,0);m.circle(x,core,r*.38,48,.30F);m.circle(z,core,r*.51,52,.34F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=base.add(g.point(a,r*.90)).add(0,.28+.18*(i%4),0);m.runeGlyph(f,n,.15,0x8800+i*101,-a,.28F);m.line(n,core,.20F);}if(c.release())m.sphere(core,r*.58,5,.28F);
            }
            case "clone" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO,core=base.add(0,.62,-.82);double r=.88+.22*p;m.circle(f,core,r,48,.50F);m.polygon(f,core,r*.72,8,t*.025,.38F);m.runeChords(f,core,r*.54,8,3,-t*.022,.30F);m.runeGlyph(f,core,r*.26,0x8801,t*.035,.40F);m.polygon(g,base.add(0,.04,0),1.35,8,-t*.015,.38F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=base.add(g.point(a,1.20));m.runeGlyph(g,n,.11,0x8811+i*37,a,.24F);m.line(n,core,.18F);}m.circle(x,base.add(0,1.02,0),.86,42,.24F);
            }
            case "control_weather" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO,sky=base.add(0,12.0,0);double r=5.6+1.7*p;m.polygon(g,sky,r,12,t*.008,.48F);m.polygon(g,sky,r*.78,8,-t*.010+.16,.38F);m.runeChords(g,sky,r*.62,12,5,t*.007,.30F);m.brokenBand(g,sky,r*.86,r*.98,88,9,1.03F,.09F);for(int i=0;i<10;i++){double a=i*Math.PI/5+t*.008;Vec3 n=sky.add(g.point(a,r*.84));m.runeGlyph(g,n,.22,0x8802+i*107,-a,.28F);m.line(n,base.add(g.point(a+.08*(i%2==0?1:-1),r*.38)),i%2==0?.46F:.28F);}m.circle(x,sky,r*.34,50,.26F);m.circle(z,sky,r*.46,56,.30F);
            }
            case "demiplane" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=2.5+.42*p,h=5.0*p+1.1;Vec3 center=base.add(0,h*.50,0);m.polygon(f,center,r,10,t*.012,.52F);m.polygon(f,center,r*.78,6,-t*.016+.12,.38F);m.runeChords(f,center,r*.60,10,3,t*.010,.30F);m.circle(x,center,r*.48,48,.28F);m.circle(z,center,r*.60,52,.30F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=center.add(f.point(a,r*.90));m.runeGlyph(f,n,.16,0x8803+i*109,-a,.26F);}m.polygon(g,base.add(0,.04,0),r*.86,10,-t*.010,.36F);m.brokenBand(g,base.add(0,.045,0),r*.94,r*1.05,62,8,1.02F,.09F);
            }
            case "dominate_monster" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p),head=target.add(0,1.62,0);double r=1.30+.26*p;m.polygon(f,head,r,8,t*.014,.50F);m.star(f,head,r*.82,r*.32,8,-t*.018,.40F);m.runeChords(f,head,r*.62,8,3,t*.012,.32F);m.runeGlyph(f,head,r*.28,0x8804,-t*.035,.42F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=head.add(f.point(a,r*.92));m.runeGlyph(f,n,.13,0x8814+i*113,a,.24F);m.line(n,target.add(0,.74,0),.20F);}m.polygon(g,target.add(0,.04,0),1.38,8,-t*.012,.34F);m.circle(x,target.add(0,1.0,0),.92,42,.24F);
            }
            case "earthquake" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=4.3+1.4*p;m.polygon(g,center.add(0,.03,0),r,12,t*.008,.48F);m.runeChords(g,center.add(0,.035,0),r*.76,12,5,-t*.009,.32F);for(int i=0;i<16;i++){double a=i*Math.PI/8+.05*Math.sin(i*1.7);Vec3 a0=center.add(g.point(a,r*.14)),a1=center.add(g.point(a+.10*Math.sin(i*.9),r*.52)),a2=center.add(g.point(a-.08*Math.cos(i*1.1),r));m.line(a0,a1,i%4==0?.60F:.30F);m.line(a1,a2,.28F);if(c.release()&&i%2==0)m.shard(a1.add(0,.45,0),new Vec3(0,1,0),g,.90+.12*(i%4),.15,1.06F,.18F);}for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.006;Vec3 n=center.add(g.point(a,r*.82));m.runeGlyph(g,n,.18,0x8805+i*127,-a,.28F);}m.brokenBand(g,center.add(0,.04,0),r*.62,r*.70,72,8,1.03F,.09F);
            }
            case "feeblemind" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p),head=target.add(0,1.48,0);double r=1.20+.28*p;m.circle(f,head,r,48,.48F);m.polygon(f,head,r*.78,9,t*.014,.36F);m.runeChords(f,head,r*.56,9,4,-t*.016,.30F);for(int i=0;i<9;i++){double a=i*Math.PI*2/9;Vec3 n=head.add(f.point(a,r*.90));Vec3 cut=head.add(f.point(a+(i%2==0?.12:-.10),r*.42));m.line(n,cut,i%3==0?.46F:.26F);if(i%3==0)m.runeGlyph(f,n,.12,0x8806+i*131,-a,.24F);}if(c.release()){m.line(head.add(f.point(.4,r*.55)),head.add(f.point(Math.PI+.55,r*.58)),.58F);m.line(head.add(f.point(2.1,r*.52)),head.add(f.point(5.0,r*.62)),.48F);}m.brokenBand(g,target.add(0,.03,0),1.02,1.22,46,6,1.0F,.08F);
            }
            case "incendiary_cloud" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=4.0+1.15*p;m.polygon(g,center.add(0,.04,0),r,12,t*.010,.44F);m.brokenBand(g,center.add(0,.045,0),r*.80,r,74,7,1.03F,.09F);for(int i=0;i<14;i++){double a=i*Math.PI*2/14+t*(i%2==0?.018:-.015);Vec3 n=center.add(g.point(a,r*(.42+.025*(i%6)))).add(0,.35+.22*(i%5),0);m.star(f,n,.24+.03*(i%3),.09,4,-a,.34F);m.line(n,n.add(g.point(a+.72,.36)),.20F);}m.runeChords(g,center.add(0,.035,0),r*.58,12,5,-t*.008,.28F);
            }
            case "maze" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p),center=target.add(0,1.0,0);double r=1.85+.32*p;m.polygon(f,center,r,8,t*.012,.46F);m.polygon(f,center,r*.78,6,-t*.016+.20,.36F);m.runeChords(f,center,r*.60,10,3,t*.010,.28F);m.circle(x,center,r*.55,46,.30F);m.circle(z,center,r*.72,52,.32F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=center.add(f.point(a,r*.90));m.runeGlyph(f,n,.14,0x8808+i*137,-a,.24F);m.line(n,center.add(z.point(a+.22,r*.44)),.20F);}m.polygon(g,target.add(0,.04,0),1.45,8,-t*.010,.34F);
            }
            case "sunburst" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p),sky=center.add(0,4.2,0);double r=3.7+1.15*p;m.star(g,sky,r,r*.52,12,-t*.010,.52F);m.polygon(g,sky,r*.76,12,t*.012,.40F);m.runeChords(g,sky,r*.56,12,5,-t*.009,.32F);m.circle(x,sky,r*.34,48,.26F);for(int i=0;i<12;i++){double a=i*Math.PI/6;Vec3 n=sky.add(g.point(a,r*.88));m.runeGlyph(g,n,.17,0x8809+i*149,-a,.28F);m.line(n,center.add(g.point(a,r*.28)),i%3==0?.50F:.28F);}if(c.release()){for(int i=0;i<20;i++){double a=i*Math.PI/10;m.line(center.add(g.point(a,r*.18)),center.add(g.point(a,r*(1.02+.10*(i%3)))),i%5==0?.68F:.34F);}m.circle(g,center.add(0,.04,0),r*.48,56,.42F);}
            }
            default -> throw new IllegalStateException("Circle8 visual missing: "+id);
        }
    }
}
