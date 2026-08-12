package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
import net.minecraft.world.phys.Vec3;

/** Phase 2C: bespoke 6C battlefield-control presentation. */
final class SixthCircleVisualIdentity {
    private SixthCircleVisualIdentity() {}
    static boolean owns(SpellDefinition spell){ return spell != null && spell.circle() == 6; }

    static void appendCharge(SpellDefinition spell, SpellPresentationProfile.Profile profile, double outer, double rotation,
                             double progress, Vec3 direction, Vec3 target, double range, ArcaneWorldMesh.Builder m) {
        switch (spell.id()) {
            case "disintegrate" -> beamCharge(direction,target,outer,rotation,progress,range,m);
            case "globe_of_invulnerability" -> wardCharge(direction,target,outer,rotation,progress,range,m);
            case "mass_suggestion" -> mindfieldCharge(direction,target,outer,rotation,progress,range,m);
            case "move_earth" -> earthCharge(direction,target,outer,rotation,progress,range,m);
            case "sunbeam" -> sunbeamCharge(direction,target,outer,rotation,progress,range,m);
            case "true_seeing" -> sightCharge(direction,target,outer,rotation,progress,range,m);
            case "freezing_sphere" -> orbCharge(direction,target,outer,rotation,progress,range,m);
            case "eyebite" -> eyeCharge(direction,target,outer,rotation,progress,range,m);
            case "flesh_to_stone" -> stoneCharge(direction,target,outer,rotation,progress,range,m);
            case "circle_of_death" -> deathfieldCharge(direction,target,outer,rotation,progress,range,m);
            case "solar_guard" -> solarguardCharge(direction,target,outer,rotation,progress,range,m);
            default -> {}
        }
    }

    static void appendRelease(SpellDefinition spell, Vec3 direction, Vec3 target, double range, double age,
                              double motion, double pf, ArcaneWorldMesh.Builder m) {
        switch (spell.id()) {
            case "disintegrate" -> beamRelease(direction,target,range,age,motion,pf,m);
            case "globe_of_invulnerability" -> wardRelease(direction,target,range,age,motion,pf,m);
            case "mass_suggestion" -> mindfieldRelease(direction,target,range,age,motion,pf,m);
            case "move_earth" -> earthRelease(direction,target,range,age,motion,pf,m);
            case "sunbeam" -> sunbeamRelease(direction,target,range,age,motion,pf,m);
            case "true_seeing" -> sightRelease(direction,target,range,age,motion,pf,m);
            case "freezing_sphere" -> orbRelease(direction,target,range,age,motion,pf,m);
            case "eyebite" -> eyeRelease(direction,target,range,age,motion,pf,m);
            case "flesh_to_stone" -> stoneRelease(direction,target,range,age,motion,pf,m);
            case "circle_of_death" -> deathfieldRelease(direction,target,range,age,motion,pf,m);
            case "solar_guard" -> solarguardRelease(direction,target,range,age,motion,pf,m);
            default -> {}
        }
    }
    private static void beamCharge(Vec3 d,Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);double q=phase(p,.03,.95);
        for(int i=0;i<3;i++){Vec3 c=safe(d).scale(o*(.12+i*.18));double rr=o*(.64-i*.12)*q;
            m.polygon(f,c,rr,6+i,r*(i%2==0?1:-.7),1.14F);m.runeChords(f,c,rr*.82,6+i,2,r*.4+i,.74F);}
        Vec3 tip=safe(d).scale(o*1.12*q);m.shard(tip,d,f,o*.92*q,o*.065,1.24F,.28F);
        for(int i=0;i<6;i++)m.line(f.point(r+i*Math.PI/3,o*.42*q),tip,i%2==0?1.08F:.62F);
    }
    private static void beamRelease(Vec3 d,Vec3 t,double range,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);Vec3 path=safeTarget(t,d,range);double len=path.length()*motion;
        m.beamPrism(safe(d).scale(.62),d,f,Math.max(.2,len-.62),.075*pf,1.24F,.28F);
        m.helix(safe(d).scale(.55),d,f,Math.max(.2,len),.17*pf,5,42,1.0F,true);
        if(motion>.86){Vec3 end=path.scale(motion);for(int i=0;i<9;i++){double a=age*8+i*2.399;Vec3 off=f.point(a,.14+.07*(i%3));m.shard(end.add(off),off,f,.34,.024,1.05F,.18F);}}
    }

    private static void wardCharge(Vec3 d,Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        double q=phase(p,.04,.95),rad=o*.88*q;Vec3 c=new Vec3(0,1.0,0);
        m.sphere(c,rad,6,1.10F);
        for(int i=0;i<4;i++){ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.fromNormal(
                new Vec3(Math.cos(r+i*Math.PI/4),.28,Math.sin(r+i*Math.PI/4)),new Vec3(0,1,0));
            m.brokenBand(b,c,rad*.83,rad,56,5+i%2,1.02F,.18F);}
        if(p>.64)m.star(ArcaneWorldMesh.Basis.ground(),Vec3.ZERO,o*.74,o*.29,8,-r,.96F);
    }
    private static void wardRelease(Vec3 d,Vec3 t,double range,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        double p=ease(age/.18),fade=fade(age,.86),rad=(2.9+pf*.55)*p;Vec3 c=new Vec3(0,1.0,0);
        m.sphere(c,rad,7,1.14F);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();
        for(int i=0;i<3;i++)m.brokenBand(g,c,rad*(.70+i*.08),rad*(.74+i*.08),62,5+i,1.0F,(float)(.16*fade));
    }

    private static void mindfieldCharge(Vec3 d,Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=ground(t);double q=phase(p,.05,.95);
        double rad=Math.max(o*1.1,Math.min(range*.26,o*2.8))*q;m.brokenBand(g,c,rad*.82,rad,76,7,1.02F,.18F);
        Vec3 hub=c.add(0,2.1*q,0);for(int i=0;i<8;i++){double a=r+i*Math.PI/4;Vec3 n=c.add(g.point(a,rad*.72)).add(0,.7+(i%3)*.45,0);
            m.diamond(g,n,o*.11*q,a,1.05F,.20F);m.line(n,hub,i%3==0?1.05F:.62F);}m.polygon(g,hub,o*.34*q,7,-r,.96F);
    }
    private static void mindfieldRelease(Vec3 d,Vec3 t,double range,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=ground(t);double max=Math.max(7,range*.26),p=ease(age/.24),f=fade(age,.82);
        for(int i=0;i<4;i++){double rr=max*clamp01(p-i*.10);if(rr>.1)m.brokenBand(g,c.add(0,.4+i*.28,0),rr*.94,rr,72,7,1.0F,(float)(.20*f));}
        for(int i=0;i<8;i++){double a=age*2+i*Math.PI/4;Vec3 n=c.add(g.point(a,max*.68));m.line(n.add(0,.3,0),n.add(0,2.2,0),.66F);}
    }

    private static void earthCharge(Vec3 d,Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=ground(t);double q=phase(p,.04,.94);
        double rad=Math.max(o,Math.min(range*.30,o*2.9))*q;m.polygon(g,c,rad,4,r+Math.PI/4,1.18F);
        for(int i=0;i<4;i++){double a=r+i*Math.PI/2;Vec3 a0=c.add(g.point(a,rad*.18)),a1=c.add(g.point(a+.18,rad*.95));
            m.line(a0,a1,1.24F);m.line(c.add(g.point(a-.25,rad*.52)),c.add(g.point(a+.27,rad*.76)),.76F);}
        if(p>.62)m.brokenBand(g,c,rad*.58,rad*.72,60,6,.94F,.16F);
    }
    private static void earthRelease(Vec3 d,Vec3 t,double range,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=ground(t);double max=Math.max(8,range*.55),p=ease(age/.28),f=fade(age,.82);
        for(int ring=1;ring<=5;ring++){double rr=max*ring/5.0*p;m.polygon(g,c.add(0,.06*ring,0),rr,4,age*.4+ring*.31,1.18F);
            for(int i=0;i<4;i++){double a=i*Math.PI/2+ring*.19;Vec3 s=c.add(g.point(a,rr*.62)),e=c.add(g.point(a+.18,rr));m.line(s,e,1.08F);}}
        if(age>.45)m.brokenBand(g,c,max*.82,max,72,8,.88F,(float)(.12*f));
    }

    private static void sunbeamCharge(Vec3 d,Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);double q=phase(p,.03,.96);
        m.star(f,Vec3.ZERO,o*.86*q,o*.34*q,12,r,1.18F);m.band(f,Vec3.ZERO,o*.47*q,o*.62*q,50,1.14F,.22F);
        for(int i=0;i<8;i++){double a=r+i*Math.PI/4;Vec3 c=f.point(a,o*.74*q);m.diamond(f,c,o*.12*q,a,1.16F,.24F);}
        if(p>.72)m.beamPrism(safe(d).scale(o*.16),d,f,o*.9*q,o*.055,1.18F,.20F);
    }
    private static void sunbeamRelease(Vec3 d,Vec3 t,double range,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);Vec3 path=safeTarget(t,d,range);double fade=fade(age,.88),rad=.24*pf;
        m.beamPrism(safe(d).scale(.6),d,f,path.length(),rad,1.30F,(float)(.34*fade));m.ribbon(safe(d).scale(.6),d,f,path.length(),rad*2.6,3,48,1.1F,(float)(.18*fade));
        for(int i=0;i<4;i++)m.brokenBand(f,path.scale(.25+i*.22),rad*(2.1+i*.25),rad*(2.45+i*.28),34,5,1.06F,(float)(.16*fade));
    }

    private static void sightCharge(Vec3 d,Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis face=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));double q=phase(p,.05,.94);Vec3 head=new Vec3(0,1.72,0);
        for(int i=0;i<3;i++){double rr=o*(.35+i*.18)*q;ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.fromNormal(
                new Vec3(Math.cos(r+i*1.2),.2,Math.sin(r+i*1.2)),new Vec3(0,1,0));m.brokenBand(b,head,rr*.82,rr,46,5,1.02F,.20F);}
        m.diamond(face,head,o*.24*q,r,1.14F,.26F);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();
        for(int i=0;i<6;i++)m.line(head,g.point(r+i*Math.PI/3,o*.8*q).add(0,1.0,0),.66F);
    }
    private static void sightRelease(Vec3 d,Vec3 t,double range,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double max=Math.max(20,range),p=ease(age/.30);
        for(int i=0;i<5;i++){double rr=max*clamp01(p-i*.09);if(rr>.1)m.arc(g,new Vec3(0,.7+i*.14,0),rr,age*2+i*.4,Math.PI*1.45,52,1.0F);}
        for(int i=0;i<8;i++){double a=age*1.6+i*Math.PI/4;m.line(new Vec3(0,1.7,0),g.point(a,max*.68*p).add(0,1.0,0),.55F);}
    }

    private static void orbCharge(Vec3 d,Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);double q=phase(p,.03,.95);Vec3 c=safe(d).scale(o*.24);
        m.sphere(c,o*.30*q,4,1.02F);m.polygon(f,c,o*.64*q,8,r,1.10F);m.polygon(f,c,o*.48*q,6,-r*.7,.82F);
        for(int i=0;i<8;i++){double a=r+i*Math.PI/4;Vec3 n=c.add(f.point(a,o*.62*q));m.shard(n,n.subtract(c),f,o*.28*q,o*.045,1.10F,.22F);}
    }
    private static void orbRelease(Vec3 d,Vec3 t,double range,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d),g=ArcaneWorldMesh.Basis.ground();Vec3 path=safeTarget(t,d,range),pos=path.scale(motion);
        double rad=(.48+pf*.12)*(1+.08*Math.sin(age*20));m.orb(pos,rad,28,1.18F,.38F);m.sphere(pos,rad*1.18,5,.88F);
        for(int i=1;i<=4;i++){Vec3 e=pos.subtract(safe(d).scale(i*rad*.9));m.brokenBand(f,e,rad*(.44-i*.05),rad*(.60-i*.05),24,4,.88F,.14F);}
        if(motion>=.98){double blast=7*Math.max(1,Math.sqrt(range/25.0));m.brokenBand(g,ground(path),blast*.74,blast,90,8,1.14F,.22F);
            for(int i=0;i<14;i++){double a=i*Math.PI*2/14;Vec3 axis=g.point(a,1);m.shard(ground(path).add(g.point(a,blast*.52)),axis,ArcaneWorldMesh.Basis.facing(axis),1.8+pf*.4,.11,1.04F,.22F);}}
    }

    private static void eyeCharge(Vec3 d,Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(safe(t));Vec3 c=t.add(0,1.45,0);double q=phase(p,.05,.94);
        m.arc(f,c,o*.62*q,Math.PI*.12,Math.PI*.76,28,1.18F);m.arc(f,c,o*.62*q,Math.PI*1.12,Math.PI*.76,28,1.18F);
        m.diamond(f,c,o*.24*q,r,1.14F,.24F);m.line(c.add(f.right().scale(-o*.56*q)),c.add(f.right().scale(o*.56*q)),.78F);
        if(p>.64)m.brokenBand(f,c,o*.72*q,o*.82*q,54,6,.96F,.18F);
    }
    private static void eyeRelease(Vec3 d,Vec3 t,double range,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        Vec3 path=t.lengthSqr()<1e-8?new Vec3(0,1.2,8):t;ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(path);double fade=fade(age,.82);
        Vec3 eye=path.add(0,1.35,0);m.beamPrism(new Vec3(0,1.45,0),path,f,path.length(),.055*pf,1.08F,(float)(.24*fade));
        m.arc(f,eye,.95,0,Math.PI,28,1.18F);m.arc(f,eye,.95,Math.PI,Math.PI,28,1.18F);m.diamond(f,eye,.28,age*2,1.12F,.24F);
    }

    private static void stoneCharge(Vec3 d,Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=t;double rad=o*.58;
        for(int i=0;i<5;i++){double y=-.35+i*.55,s=phase(p,.08+i*.10,.94);if(s>0)m.polygon(g,c.add(0,y,0),rad*(.88-i*.05)*s,6,r+i*.32,1.04F);}
        double q=phase(p,.05,.94);for(int i=0;i<6;i++){double a=r+i*Math.PI/3;Vec3 b=c.add(g.point(a,rad*q)).add(0,-.4,0);m.line(b,b.add(0,2.7*q,0),i%2==0?1.12F:.72F);}
    }
    private static void stoneRelease(Vec3 d,Vec3 t,double range,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double p=ease(age/.22),fade=fade(age,.84),rad=1.25+pf*.12;
        for(int i=0;i<6;i++){double y=-.35+i*.48,q=clamp01(p-i*.09);if(q>0)m.polygonPlate(g,t.add(0,y,0),rad*(1-i*.035)*q,6,i*.28,.88F,(float)(.14*fade));}
        for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 b=t.add(g.point(a,rad)).add(0,-.4,0);m.line(b,b.add(0,2.7*p,0),.92F);}
    }

    private static void deathfieldCharge(Vec3 d,Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=ground(t);double q=phase(p,.04,.95),rad=Math.max(o,Math.min(range*.30,o*2.8))*q;
        m.band(g,c,rad*.88,rad,80,1.0F,.18F);m.star(g,c,rad*.70,rad*.28,9,r,1.08F);
        for(int i=0;i<12;i++){double a=r+i*Math.PI/6;m.line(c.add(g.point(a,rad*.18)),c.add(g.point(a+(i%2==0?.05:-.05),rad*.92)),i%3==0?1.08F:.62F);}
    }
    private static void deathfieldRelease(Vec3 d,Vec3 t,double range,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=ground(t);double max=10*Math.max(1,Math.sqrt(range/25.0)),p=ease(age/.34),f=fade(age,.78);
        for(int i=0;i<3;i++){double rr=max*clamp01(p-i*.11);if(rr>.1)m.brokenBand(g,c.add(0,.12+i*.11,0),rr*.90,rr,84,7+i,1.08F,(float)(.22*f));}
        for(int i=0;i<9;i++){double a=age*1.4+i*Math.PI*2/9;Vec3 n=c.add(g.point(a,max*.62*p));m.line(n,n.add(0,.9+.2*(i%3),0),.62F);}
    }

    private static void solarguardCharge(Vec3 d,Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.04,.95);Vec3 c=new Vec3(0,1.0,0);
        m.sphere(c,o*.58*q,4,1.08F);for(int i=0;i<6;i++){double a=r+i*Math.PI/3;Vec3 n=c.add(g.point(a,o*.72*q));
            m.star(g,n,o*.18*q,o*.07*q,6,a,1.12F);m.line(n,c,.72F);}m.brokenBand(g,Vec3.ZERO,o*.78*q,o*.92*q,64,6,1.04F,.18F);
    }
    private static void solarguardRelease(Vec3 d,Vec3 t,double range,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double p=ease(age/.20),f=fade(age,.86),rad=(3.4+pf*.45)*p;Vec3 c=new Vec3(0,1,0);
        m.sphere(c,rad*.74,5,1.08F);for(int i=0;i<6;i++){double a=age*1.2+i*Math.PI/3;Vec3 n=c.add(g.point(a,rad));m.star(g,n,.42+.08*pf,.18,6,-a,1.12F);m.line(n,c,.68F);}
        m.brokenBand(g,Vec3.ZERO,rad*.82,rad,68,6,1.02F,(float)(.18*f));
    }

    private static Vec3 ground(Vec3 v){return new Vec3(v.x,Math.min(.18,v.y),v.z);}
    private static Vec3 safe(Vec3 v){return v.lengthSqr()<1e-8?new Vec3(0,0,1):v.normalize();}
    private static Vec3 safeTarget(Vec3 t,Vec3 d,double fallback){return t.lengthSqr()<1e-8?safe(d).scale(fallback):t;}
    private static double phase(double p,double a,double b){return clamp01((p-a)/Math.max(.0001,b-a));}
    private static double ease(double v){v=clamp01(v);return 1-Math.pow(1-v,3);}
    private static double fade(double age,double start){return age<=start?1:clamp01((1-age)/Math.max(.001,1-start));}
    private static double clamp01(double v){return Math.max(0,Math.min(1,v));}
}
