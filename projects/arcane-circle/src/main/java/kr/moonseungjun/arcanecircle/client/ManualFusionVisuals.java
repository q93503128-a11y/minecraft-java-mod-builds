package kr.moonseungjun.arcanecircle.client;

import net.minecraft.world.phys.Vec3;

import static kr.moonseungjun.arcanecircle.client.ManualSpellVisuals.*;

/** Fusion spells are authored as their own spells, never as automatic overlays of ingredients. */
final class ManualFusionVisuals {
    private ManualFusionVisuals() {}

    static void draw(String id, ArcaneWorldMesh.Builder m, Context c) {
        double p=c.reveal(),t=c.elapsed();ArcaneWorldMesh.Basis g=ground(),f=face(c),x=sideX(),z=sideZ();
        switch(id){
            case "burning_hands" -> {
                Vec3 hub=c.direction().scale(.36);double r=.62+.28*p;m.star(f,hub,r,r*.28,5,t*.12,.52F);for(int i=0;i<5;i++){double a=(i-2)*.26;Vec3 end=c.direction().scale(1.6+1.1*p).add(f.right().scale(Math.sin(a)*(1.05+.35*p)));m.line(hub,end,i==2?.66F:.34F);}if(c.release())m.cone(Vec3.ZERO,c.direction(),f,Math.max(2.4,c.range()*.58),1.5,9,3,.34F);
            }
            case "ice_knife" -> {
                Vec3 hub=c.direction().scale(.82);double r=.42+.20*p;m.polygon(f,hub,r,6,t*.04,.48F);m.shard(hub.add(c.direction().scale(.28)),c.direction(),f,.95+.45*p,.11,1.10F,.18F);for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 n=hub.add(f.point(a,r));m.line(n,hub,.24F);}if(c.release()){m.polygon(g,c.target().add(0,.03,0),1.05,6,-t*.08,.38F);for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 n=c.target().add(g.point(a,.82));m.shard(n.add(0,.25,0),new Vec3(0,1,0),g,.52,.08,1.04F,.14F);}}
            }
            case "chromatic_orb" -> {
                Vec3 hub=c.direction().scale(.86);double r=.54+.24*p;m.orb(hub,r*.42,18,1.10F,.16F);m.polygon(f,hub,r,7,t*.06,.48F);for(int i=0;i<7;i++){double a=i*Math.PI*2/7+t*.03;Vec3 n=hub.add(f.point(a,r*.90));m.diamond(f,n,.11,a,1.08F,.12F);m.line(n,hub,.20F);}if(c.release())m.circle(f,c.target(),.72,34,.34F);
            }
            case "wind_wall" -> {
                Vec3 base=c.release()?c.target():c.target().scale(p);double width=4.8+2.5*p;for(int i=0;i<11;i++){double q=i/10.0,x0=(q-.5)*width;Vec3 foot=base.add(f.right().scale(x0)),top=foot.add(f.right().scale(Math.sin(i+t)*.24)).add(0,2.4+.6*Math.sin(i*.7+t),0);m.line(foot,top,i%3==0?.48F:.28F);}m.arc(g,base.add(0,.03,0),width*.42,t*.18,Math.PI*1.42,40,.30F);
            }
            case "counterspell" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p);double r=.88+.24*p;m.polygon(f,target,r,8,t*.025,.50F);m.polygon(f,target,r*.68,4,Math.PI/4-t*.04,.38F);m.runeChords(f,target,r*.52,8,3,t*.02,.28F);m.line(target.add(f.point(Math.PI/4,r*.94)),target.add(f.point(Math.PI*5/4,r*.94)),.62F);m.line(target.add(f.point(Math.PI*3/4,r*.94)),target.add(f.point(Math.PI*7/4,r*.94)),.62F);if(c.release())m.brokenBand(f,target,r*.98,r*1.12,44,5,1.04F,.08F);
            }
            case "fire_shield" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;m.polygon(g,base.add(0,.04,0),1.05,6,t*.04,.42F);for(int i=0;i<6;i++){double a=i*Math.PI/3+t*.18;Vec3 n=base.add(g.point(a,.96)).add(0,.78+.28*Math.sin(a+t),0);m.star(f,n,.23,.09,4,-a,.48F);m.line(n,base.add(0,.92,0),.20F);}m.circle(f,base.add(0,1.0,0),.72,36,.28F);
            }
            case "wall_of_ice" -> {
                Vec3 base=c.release()?c.target():c.target().scale(p);double width=5.2+2.8*p;for(int i=0;i<11;i++){double q=i/10.0,x0=(q-.5)*width,h=1.7+1.0*((i*37)%5)/4.0;Vec3 foot=base.add(f.right().scale(x0));m.shard(foot.add(0,h*.48,0),new Vec3(0,1,0),g,h,.16+.02*(i%4),1.06F,.19F);}m.polygon(g,base.add(0,.03,0),width*.38,6,t*.02,.30F);
            }
            case "chain_lightning" -> {
                Vec3 hub=c.direction().scale(.76);m.polygon(f,hub,.72,6,t*.05,.52F);m.runeChords(f,hub,.52,6,2,-t*.06,.30F);if(c.release()){Vec3 prev=Vec3.ZERO;for(int i=1;i<=9;i++){double q=i/9.0;Vec3 n=c.target().scale(q).add(f.right().scale(Math.sin(i*2.2)*(.15+.05*(i%3)))).add(f.up().scale(Math.cos(i*1.6)*.10));m.line(prev,n,i%3==0?.78F:.46F);if(i%3==0)m.polygon(f,n,.20,6,t*.08+i,.28F);prev=n;}}
            }
            case "arcane_hand" -> {
                Vec3 target=c.release()?c.target():c.target().scale(p),palm=target.add(0,.95,0);m.polygon(f,palm,.72,5,-Math.PI/2+t*.03,.48F);for(int i=0;i<5;i++){double a=-.72+i*.36;Vec3 root=palm.add(f.right().scale((i-2)*.16)).add(f.up().scale(.20)),tip=root.add(f.up().scale(.62+.08*(2-Math.abs(i-2)))).add(f.right().scale(Math.sin(a)*.12));m.line(root,tip,i==2?.52F:.30F);m.circle(f,tip,.08,12,.24F);}m.runeGlyph(f,palm,.22,0xF808,t*.05,.34F);if(c.release())m.circle(g,target.add(0,.03,0),.92,38,.28F);
            }
            case "teleportation_circle" -> {
                Vec3 base=c.release()?c.target():c.target().scale(p);double r=1.85+.35*p;m.polygon(g,base.add(0,.04,0),r,10,t*.018,.48F);m.polygon(g,base.add(0,.045,0),r*.76,6,-t*.024+.12,.36F);m.runeChords(g,base.add(0,.05,0),r*.56,10,3,t*.014,.28F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=base.add(g.point(a,r*.90));m.runeGlyph(g,n,.13,0xF809+i*71,-a,.24F);}m.circle(x,base.add(0,.75,0),r*.38,38,.24F);
            }
            case "steam_burst" -> {
                Vec3 hub=c.direction().scale(.42);double r=.66+.28*p;m.circle(f,hub,r,34,.42F);m.polygon(f,hub,r*.72,6,t*.08,.36F);for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.20;Vec3 n=hub.add(f.point(a,r*.84));m.line(n,n.add(c.direction().scale(.42+.12*(i%3))),.30F);}if(c.release()){m.cone(Vec3.ZERO,c.direction(),f,Math.max(2.4,c.range()*.58),1.65,10,3,.30F);m.circle(f,c.target(),.82,36,.28F);}
            }
            case "frost_step" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;m.polygon(g,base.add(0,.04,0),.92,6,t*.05,.42F);m.polygon(g,base.add(0,.045,0),.64,4,-t*.07+.18,.32F);for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 n=base.add(g.point(a,.84));m.shard(n.add(0,.18,0),new Vec3(0,1,0),g,.36,.07,1.04F,.13F);}if(c.release())m.arc(g,base.add(0,.05,0),1.18,-t*.14,Math.PI*1.60,36,.34F);
            }
            case "thunder_cage" -> {
                Vec3 base=c.release()?c.target():c.target().scale(p);double r=1.25+.22*p,h=2.7*p+.35;m.polygon(g,base.add(0,.04,0),r,8,t*.025,.46F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 foot=base.add(g.point(a,r)),top=foot.add(0,h,0);m.line(foot,top,i%2==0?.58F:.34F);if(i%2==0){Vec3 zig=base.add(g.point(a+.18,r*.64)).add(0,h*.52,0);m.line(foot,zig,.30F);m.line(zig,top,.30F);}}if(c.release())m.runeChords(g,base.add(0,h+.02,0),r*.68,8,3,-t*.04,.30F);
            }
            case "solar_guard" -> {
                Vec3 base=c.release()?c.target():Vec3.ZERO;double r=1.45+.24*p;m.star(g,base.add(0,.04,0),r,r*.52,8,t*.025,.48F);m.polygon(g,base.add(0,.045,0),r*.78,8,-t*.03,.36F);for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.13;Vec3 n=base.add(g.point(a,r*.88)).add(0,.86+.22*Math.sin(a+t),0);m.star(f,n,.25,.10,4,-a,.46F);m.line(n,base.add(0,1.02,0),.20F);}m.circle(f,base.add(0,1.08,0),.78,40,.34F);m.runeGlyph(f,base.add(0,1.08,0),.25,0xF80D,-t*.03,.32F);
            }
            case "void_lance" -> {
                Vec3 hub=c.direction().scale(.92);double r=.92+.25*p;m.polygon(f,hub,r,8,t*.018,.50F);m.runeChords(f,hub,r*.68,8,3,-t*.022,.32F);m.circle(x,hub,r*.42,38,.26F);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 n=hub.add(f.point(a,r*.88));m.runeGlyph(f,n,.12,0xF80E+i*83,-a,.24F);}if(c.release()){double len=Math.max(3,c.target().length());m.beamPrism(Vec3.ZERO,c.direction(),f,len,.12,1.16F,.20F);for(int i=1;i<6;i++){Vec3 n=c.direction().scale(len*i/6.0);m.polygon(f,n,.30,8,t*.04+i*.21,.30F);}}
            }
            case "winter_domain" -> {
                Vec3 center=c.release()?c.target():Vec3.ZERO;double r=3.1+.95*p;m.polygon(g,center.add(0,.04,0),r,12,t*.012,.46F);m.runeChords(g,center.add(0,.045,0),r*.76,12,5,-t*.014,.32F);m.brokenBand(g,center.add(0,.05,0),r*.58,r*.68,62,8,1.02F,.08F);for(int i=0;i<12;i++){double a=i*Math.PI/6;Vec3 n=center.add(g.point(a,r*.86));m.shard(n.add(0,.35,0),new Vec3(0,1,0),g,.70+.10*(i%4),.10,1.04F,.15F);if(i%3==0)m.runeGlyph(g,n,.13,0xF80F+i*89,-a,.24F);}m.circle(x,center.add(0,1.0,0),r*.34,44,.26F);
            }
            case "astral_prison" -> {
                Vec3 base=c.release()?c.target():c.target().scale(p);double r=1.75+.28*p,h=3.6*p+.45;m.polygon(g,base.add(0,.04,0),r,10,t*.012,.48F);m.polygon(g,base.add(0,h,0),r,10,-t*.014,.42F);m.runeChords(g,base.add(0,.045,0),r*.68,10,3,t*.010,.30F);for(int i=0;i<10;i++){double a=i*Math.PI/5;Vec3 foot=base.add(g.point(a,r)),top=base.add(g.point(a+.05*(i%2==0?1:-1),r)).add(0,h,0);m.line(foot,top,i%2==0?.54F:.30F);}Vec3 core=base.add(0,h*.52,0);m.circle(x,core,r*.72,46,.28F);m.circle(z,core,r*.54,42,.24F);if(c.release())m.runeGlyph(g,base.add(0,h+.04,0),.48,0xF810,-t*.025,.36F);
            }
            case "phoenix_requiem" -> {
                Vec3 center=c.release()?c.target():Vec3.ZERO;double r=3.4+1.0*p;m.star(g,center.add(0,.04,0),r,r*.46,12,-t*.012,.50F);m.polygon(g,center.add(0,.045,0),r*.78,8,t*.014,.40F);m.runeChords(g,center.add(0,.05,0),r*.60,12,5,-t*.010,.32F);Vec3 phoenix=center.add(0,1.45,0);for(int s:new int[]{-1,1})for(int i=0;i<5;i++){Vec3 root=phoenix.add(s*.12,.10,0),joint=phoenix.add(s*(.62+i*.26),.42-i*.05,.06*Math.sin(t+i)),tip=phoenix.add(s*(1.18+i*.25),.12-i*.13,.10*Math.cos(t*1.2+i));m.line(root,joint,i==0?.54F:.32F);m.line(joint,tip,.28F);}m.circle(f,phoenix,.62,40,.34F);if(c.release()){for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.02;Vec3 n=center.add(g.point(a,r*.78));m.shard(n.add(0,.75,0),new Vec3(0,1,0),g,1.5,.14,1.08F,.17F);}}
            }
            case "world_sunder" -> {
                Vec3 center=c.release()?c.target():c.target().scale(p);double r=5.4+1.8*p;m.polygon(g,center.add(0,.03,0),r,13,t*.006,.48F);m.runeChords(g,center.add(0,.035,0),r*.76,13,5,-t*.008,.34F);m.brokenBand(g,center.add(0,.04,0),r*.58,r*.66,82,9,1.03F,.09F);for(int i=0;i<13;i++){double a=i*Math.PI*2/13+.05*Math.sin(i*1.9);Vec3 node=center.add(g.point(a,r*.88));m.runeGlyph(g,node,.18,0xF812+i*157,-a,.28F);Vec3 mid=center.add(g.point(a+.10*Math.sin(i),r*.52));m.line(node,mid,i%3==0?.50F:.28F);m.line(mid,center.add(g.point(a-.08*Math.cos(i*.8),r*.18)),.24F);}Vec3 core=center.add(0,1.2,0);m.circle(x,core,r*.26,48,.28F);m.circle(z,core,r*.34,52,.30F);if(c.release()){Vec3 prev=center.add(g.point(Math.PI,r*.94));for(int i=1;i<=13;i++){double q=i/13.0;double xx=(q*2-1)*r*.94,zz=Math.sin(i*1.43)*r*.12;Vec3 n=center.add(g.right().scale(xx)).add(g.up().scale(zz));m.line(prev,n,i%3==0?.72F:.42F);if(i%2==0)m.shard(n.add(0,.45,0),new Vec3(0,1,0),g,.88,.13,1.06F,.16F);prev=n;}}
            }
            default -> throw new IllegalStateException("Fusion visual missing: "+id);
        }
    }
}
