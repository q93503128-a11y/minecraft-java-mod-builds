package kr.moonseungjun.arcanecircle.client;

import net.minecraft.world.phys.Vec3;

import static kr.moonseungjun.arcanecircle.client.ManualSpellVisuals.*;

/** Fourth-circle visuals: strategic walls, stronger wards and disabling magic. */
final class ManualCircle4Visuals {
    private ManualCircle4Visuals() {}

    static void draw(String id, ArcaneWorldMesh.Builder m, Context c) {
        double p=c.reveal(),t=c.elapsed();ArcaneWorldMesh.Basis g=ground(),f=face(c);
        switch(id){
            case "wall_of_fire" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double width=4.8+3.0*p;Vec3 right=f.right();m.line(center.add(right.scale(-width*.5)),center.add(right.scale(width*.5)),.48F);for(int i=0;i<11;i++){double q=i/10.0,x=(q-.5)*width;Vec3 foot=center.add(right.scale(x));double h=.7+1.5*Math.abs(Math.sin(i*1.3+t*1.2))*p;m.shard(foot.add(0,h*.48,0),new Vec3(0,1,0),g,h,.12+.02*(i%3),1.08F,.20F);}if(c.release())m.brokenBand(g,center.add(0,.03,0),width*.24,width*.30,42,5,1.04F,.08F);
            }
            case "ice_storm" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p),sky=center.add(0,4.2+1.5*p,0);double r=2.0+.6*p;m.polygon(g,sky,r*.76,8,t*.04,.52F);m.runeChords(g,sky,r*.58,8,3,-t*.025,.30F);for(int i=0;i<10;i++){double a=i*Math.PI*2/10+t*.02;Vec3 top=sky.add(g.point(a,r*.64)),bot=center.add(g.point(a+.11,r*.58));m.shard(top.add(bot).scale(.5),bot.subtract(top),f,top.distanceTo(bot),.07,1.04F,.16F);}if(c.release())m.circle(g,center.add(0,.03,0),r*1.05,52,.34F);
            }
            case "greater_invisibility" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;for(int i=0;i<4;i++){double y=.28+i*.38;m.arc(g,base.add(0,y,0),.72+i*.12,t*(.46-i*.07)+i*.8,Math.PI*1.36,30,i==0?.48F:.26F);}m.circle(f,base.add(0,1.03,0),.68,34,.30F);m.circle(sideX(),base.add(0,1.03,0),.54,30,.24F);for(int i=0;i<4;i++){double a=i*Math.PI/2+t*.08;Vec3 n=base.add(g.point(a,.90)).add(0,.95,0);m.runeGlyph(f,n,.11,0x4403+i*23,-a,.24F);}
            }
            case "resilient_sphere" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;Vec3 center=base.add(0,1.15,0);double r=1.12+.18*p;m.sphere(center,r,6,.58F);m.polygon(g,base.add(0,.04,0),r*1.04,8,t*.04,.42F);m.runeChords(g,base.add(0,.045,0),r*.78,8,3,-t*.03,.26F);
            }
            case "dimension_door" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double h=2.2+1.0*p,w=1.15+.35*p;Vec3 center=base.add(0,h*.5,0);for(int d=-2;d<=2;d++){Vec3 cc=center.add(f.normal().scale(d*.055));m.polygon(f,cc,w*(1-Math.abs(d)*.045),6,t*(d%2==0?.08:-.06)+d*.12,d==0?.64F:.28F);}m.line(base.add(f.right().scale(-w)),base.add(f.right().scale(-w)).add(0,h,0),.58F);m.line(base.add(f.right().scale(w)),base.add(f.right().scale(w)).add(0,h,0),.58F);m.brokenBand(g,base.add(0,.03,0),w*.72,w*.94,36,5,1.02F,.10F);
            }
            case "stoneskin" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.05;Vec3 n=base.add(g.point(a,.70+.10*(i%2))).add(0,.38+.22*(i%5),0);m.polygon(f,n,.20+.025*(i%3),5,-a,.48F);if(i%2==0)m.line(n,base.add(0,.92,0),.20F);}m.polygon(g,base.add(0,.04,0),.82,8,-t*.025,.30F);
            }
            case "confusion" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=1.35+.28*p;for(int i=0;i<5;i++){double rr=r*(.35+i*.13);m.arc(g,center.add(0,.04+i*.015,0),rr,t*(i%2==0?.28:-.22)+i*.7,Math.PI*(.72+.08*i),22,.24F+i*.04F);}for(int i=0;i<6;i++){double a=i*Math.PI/3+t*.11;Vec3 n=center.add(g.point(a,r*.84)).add(0,.20+.10*(i%3),0);m.runeGlyph(f,n,.13,0x4406+i*29,a+t*.10,.28F);}
            }
            case "blight" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p);Vec3 core=target.add(0,.86,0);m.star(f,core,.62,.18,6,t*.09,.58F);m.circle(f,core,.31,26,.34F);for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 out=core.add(f.point(a,.72));m.line(out,core,i%2==0?.52F:.30F);m.line(out,out.add(0,-.28-.06*(i%3),0),.22F);}if(c.release())m.brokenBand(g,target.add(0,.03,0),.84,1.02,38,6,1.0F,.08F);
            }
            case "freedom_of_movement" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;m.helix(base.add(0,.02,0),new Vec3(0,1,0),f,1.78,.62,3,44,.38F,true);m.arc(g,base.add(0,.04,0),1.02,t*.44,Math.PI*1.56,34,.52F);for(int i=0;i<4;i++){double a=i*Math.PI/2-t*.08;Vec3 n=base.add(g.point(a,.86)).add(0,.74+.18*i,0);m.line(n,n.add(g.point(a+.72,.28)),.28F);}
            }
            case "phantasmal_killer" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p),head=target.add(0,1.35,0);double r=.84+.24*p;m.star(f,head,r,r*.31,4,-t*.08,.58F);m.polygon(f,head,r*.72,4,Math.PI/4+t*.06,.42F);m.runeChords(f,head,r*.54,8,3,-t*.04,.28F);for(int i=0;i<4;i++){double a=Math.PI/4+i*Math.PI/2;Vec3 n=head.add(f.point(a,r));m.line(n,target.add(g.point(a,r*.48)).add(0,.04,0),.26F);}if(c.release())m.circle(g,target.add(0,.03,0),1.12,44,.30F);
            }
            default -> throw new IllegalStateException("Circle4 visual missing: "+id);
        }
    }
}
