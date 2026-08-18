package kr.moonseungjun.arcanecircle.client;

import net.minecraft.world.phys.Vec3;

import static kr.moonseungjun.arcanecircle.client.ManualSpellVisuals.*;

/** Fifth-circle visuals: battlefield-scale authored arrangements. */
final class ManualCircle5Visuals {
    private ManualCircle5Visuals() {}

    static void draw(String id, ArcaneWorldMesh.Builder m, Context c) {
        double p=c.reveal(),t=c.elapsed();ArcaneWorldMesh.Basis g=ground(),f=face(c);
        switch(id){
            case "cone_of_cold" -> {
                Vec3 origin=c.direction().scale(.20);double len=2.4+2.0*p,endR=1.2+1.1*p;m.cone(origin,c.direction(),f,len,endR,10,4,.42F);for(int i=0;i<6;i++){double a=i*Math.PI/3+t*.03;Vec3 n=origin.add(c.direction().scale(len*.58)).add(f.point(a,endR*.52));m.polygon(f,n,.18+.04*(i%2),6,-a,.28F);}if(c.release())m.polygon(g,c.target().add(0,.03,0),2.15,6,t*.03,.42F);
            }
            case "wall_of_force" -> {
                Vec3 base=c.release()?c.target():c.target().scale(p);double width=5.5+3.2*p,h=2.4+1.0*p;for(int i=0;i<9;i++){double q=i/8.0,x=(q-.5)*width;Vec3 node=base.add(f.right().scale(x)).add(0,h*(.35+.28*(i%2)),0);m.polygon(f,node,.38,6,(i%2==0?1:-1)*t*.06,.48F);if(i>0){double px=((i-1)/8.0-.5)*width;Vec3 prev=base.add(f.right().scale(px)).add(0,h*(.35+.28*((i-1)%2)),0);m.line(prev,node,.26F);}}m.line(base.add(f.right().scale(-width*.5)),base.add(f.right().scale(-width*.5)).add(0,h,0),.54F);m.line(base.add(f.right().scale(width*.5)),base.add(f.right().scale(width*.5)).add(0,h,0),.54F);
            }
            case "cloudkill" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=2.2+.7*p;m.brokenBand(g,center.add(0,.04,0),r*.78,r,58,7,1.02F,.10F);for(int i=0;i<9;i++){double a=i*Math.PI*2/9+t*.04;Vec3 n=center.add(g.point(a,r*(.45+.04*(i%3)))).add(0,.35+.18*(i%4),0);m.circle(f,n,.28+.04*(i%2),20,.28F);m.line(n,n.add(g.point(a+.9,.36)),.20F);}m.polygon(g,center.add(0,.03,0),r*.52,9,-t*.02,.30F);
            }
            case "telekinesis" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p),core=target.add(0,.95,0);m.circle(f,core,.74,40,.48F);m.runeChords(f,core,.56,8,3,t*.06,.32F);for(int i=0;i<6;i++){double a=i*Math.PI/3+t*.14;Vec3 n=core.add(f.point(a,.88));m.diamond(f,n,.15,-a,1.08F,.14F);m.line(n,core,.24F);}m.line(Vec3.ZERO.add(0,.90,0),core,.36F);
            }
            case "flame_strike" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p),sky=center.add(0,5.2+1.4*p,0);double r=1.55+.45*p;m.star(g,sky,r,r*.42,8,-t*.05,.52F);m.circle(g,sky,r*.58,42,.30F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=sky.add(g.point(a,r*.72));m.line(n,center.add(g.point(a,r*.24)),i%2==0?.52F:.30F);}if(c.release()){m.beamPrism(sky,new Vec3(0,-1,0),f,Math.max(1.0,sky.y-center.y),.18,1.12F,.18F);m.star(g,center.add(0,.03,0),1.65,.50,8,t*.12,.52F);}
            }
            case "hold_monster" -> {
                Vec3 base=c.release()?c.target():c.target().scale(p);double r=1.12+.22*p,h=2.55*p+.25;m.band(g,base.add(0,.03,0),r*.82,r,38,1.04F,.10F);m.polygon(g,base.add(0,.04,0),r*.68,8,t*.03,.34F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 foot=base.add(g.point(a,r)),top=foot.add(0,h,0);m.line(foot,top,i%2==0?.58F:.34F);m.line(top,base.add(0,h,0),.24F);}if(c.release())m.runeGlyph(g,base.add(0,h+.02,0),.42,0x5505,-t*.07,.40F);
            }
            case "mass_cure_wounds" -> {
                Vec3 center=c.release()?c.target():Vec3.ZERO;double r=1.65+.55*p;m.star(g,center.add(0,.04,0),r,r*.52,8,t*.03,.42F);m.circle(g,center.add(0,.05,0),r*.60,42,.30F);for(int i=0;i<8;i++){double a=i*Math.PI/4-t*.025;Vec3 n=center.add(g.point(a,r*.82)).add(0,.30+.12*(i%3),0);m.circle(f,n,.20,18,.28F);m.line(n,center.add(0,.85,0),.22F);}if(c.release())m.helix(center.add(0,.02,0),new Vec3(0,1,0),f,1.55,.54,2,38,.28F,true);
            }
            case "passwall" -> {
                Vec3 base=c.release()?c.target():c.target().scale(p);double w=1.55+.36*p,h=2.35+1.0*p;Vec3 center=base.add(0,h*.5,0);m.polygon(f,center,w,4,Math.PI/4,.58F);m.polygon(f,center,w*.74,4,-t*.05,.34F);for(int i=0;i<4;i++){double a=Math.PI/4+i*Math.PI/2;Vec3 n=center.add(f.point(a,w*.88));m.runeGlyph(f,n,.14,0x5507+i*37,a,.28F);}m.brokenBand(g,base.add(0,.03,0),w*.70,w*.94,42,6,1.02F,.09F);
            }
            case "dominate_person" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p),head=target.add(0,1.50,0);double r=.88+.24*p;m.polygon(f,head,r,6,t*.035,.48F);m.runeChords(f,head,r*.66,6,2,-t*.04,.30F);m.runeGlyph(f,head,.26,0x5508,t*.08,.42F);for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 n=head.add(f.point(a,r));m.line(n,target.add(0,.76,0),i%2==0?.36F:.22F);}if(c.release())m.circle(g,target.add(0,.03,0),1.08,42,.28F);
            }
            case "insect_plague" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=2.15+.65*p;m.brokenBand(g,center.add(0,.04,0),r*.76,r,60,6,1.02F,.08F);for(int i=0;i<12;i++){double a=i*Math.PI*2/12+t*(i%2==0?.10:-.08);Vec3 n=center.add(g.point(a,r*(.42+.035*(i%5)))).add(0,.25+.13*(i%4),0);m.diamond(f,n,.10+.02*(i%3),a,1.08F,.12F);Vec3 n2=n.add(g.point(a+.85,.28));m.line(n,n2,.18F);}m.polygon(g,center.add(0,.03,0),r*.50,12,-t*.02,.28F);
            }
            default -> throw new IllegalStateException("Circle5 visual missing: "+id);
        }
    }
}
