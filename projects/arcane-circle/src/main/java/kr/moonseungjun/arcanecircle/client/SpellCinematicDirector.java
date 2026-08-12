package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellMetrics;
import net.minecraft.world.phys.Vec3;

/**
 * Ground-up presentation director for alpha.26.
 *
 * The visual is authored from the physical event of the spell (projectile, gate, weather,
 * execution mark, fault, etc.) rather than from a universal magic-circle template.  No external
 * mod assets or code are copied here; the implementation is native Arcane Circle geometry.
 */
final class SpellCinematicDirector {
    static final int CAST_SNAP=1, CAST_AIM=2, CAST_HEAVY=3, CAST_GROUND=4,
            CAST_WARD=5, CAST_PORTAL=6, CAST_RITUAL=7;

    private static final int CHARGE_BUDGET=7600;
    private static final int RELEASE_BUDGET=9800;

    enum Form { NEEDLE, ORB, VOLLEY, RAY, CONE, FIELD, WALL, GATE, PRISON, SKY,
        WEATHER, AURA, MARK, SHIFT, TRANSFORM, CLOCK, TERRAIN, DOMAIN }

    record Signature(Form form,double scale,int detail,int satellites,double spread,double altitude,double tempo) {}

    private SpellCinematicDirector() {}

    static int castingFamily(SpellDefinition spell) {
        return switch (signature(spell).form()) {
            case GATE, SHIFT -> CAST_PORTAL;
            case SKY, WEATHER, CLOCK, DOMAIN -> CAST_RITUAL;
            case FIELD, TERRAIN, CONE, WALL -> CAST_GROUND;
            case PRISON, AURA, TRANSFORM -> CAST_WARD;
            case ORB, VOLLEY -> CAST_HEAVY;
            case NEEDLE, RAY -> CAST_AIM;
            case MARK -> spell.circle() >= 7 ? CAST_RITUAL : CAST_SNAP;
        };
    }

    static ArcaneWorldMesh charge(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                                  double range, double power, double progress, boolean fusion,
                                  long startedAtNanos) {
        Signature s=signature(spell);
        ArcaneWorldMesh.Builder m=ArcaneWorldMesh.builder(CHARGE_BUDGET);
        ArcaneWorldMesh.Basis face=ArcaneWorldMesh.Basis.facing(direction);
        ArcaneWorldMesh.Basis ground=ArcaneWorldMesh.Basis.ground();
        double p=clamp(progress,0,1);
        double time=Math.max(0,(System.nanoTime()-startedAtNanos)/1_000_000_000.0);
        double pulse=.96+Math.sin(time*s.tempo())*.04;
        double unit=(.55+spell.circle()*.075)*s.scale()*pulse*(fusion?1.10:1.0);
        int seed=spell.id().hashCode();

        switch(s.form()) {
            case NEEDLE -> chargeNeedle(m,spell,face,direction,unit,p,time,seed);
            case ORB -> chargeOrb(m,spell,face,direction,unit,p,time,seed,s.detail());
            case VOLLEY -> chargeVolley(m,spell,face,direction,unit,p,time,seed,s.satellites());
            case RAY -> chargeRay(m,spell,face,direction,unit,p,time,seed);
            case CONE -> chargeCone(m,spell,face,direction,unit,p,time,seed);
            case FIELD -> chargeField(m,spell,ground,targetOffset,unit,p,time,seed,false);
            case DOMAIN -> chargeField(m,spell,ground,targetOffset,unit*1.35,p,time,seed,true);
            case WALL -> chargeWall(m,spell,ground,face,targetOffset,range,unit,p,time,seed);
            case GATE -> chargeGate(m,spell,face,direction,targetOffset,unit,p,time,seed);
            case PRISON -> chargePrison(m,spell,targetOffset,unit,p,time,seed);
            case SKY -> chargeSky(m,spell,ground,targetOffset,unit,p,time,seed,s);
            case WEATHER -> chargeWeather(m,spell,ground,targetOffset,unit,p,time,seed,s);
            case AURA -> chargeAura(m,spell,ground,unit,p,time,seed);
            case MARK -> chargeMark(m,spell,face,targetOffset,unit,p,time,seed);
            case SHIFT -> chargeShift(m,spell,face,direction,targetOffset,unit,p,time,seed);
            case TRANSFORM -> chargeTransform(m,spell,ground,unit,p,time,seed);
            case CLOCK -> chargeClock(m,spell,ground,unit,p,time,seed);
            case TERRAIN -> chargeTerrain(m,spell,ground,targetOffset,range,unit,p,time,seed);
        }
        return m.build();
    }

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                                   double range, double power, double age, double impactAge,
                                   boolean fusion, int ingredients) {
        Signature s=signature(spell);
        ArcaneWorldMesh.Builder m=ArcaneWorldMesh.builder(RELEASE_BUDGET);
        ArcaneWorldMesh.Basis face=ArcaneWorldMesh.Basis.facing(direction);
        ArcaneWorldMesh.Basis ground=ArcaneWorldMesh.Basis.ground();
        double scale=powerScale(spell,power)*s.scale()*(fusion?1.08:1.0);
        double travel=clamp(age/Math.max(.05,impactAge<=0?.78:impactAge),0,1);
        double fade=clamp((1-age)/.20,0,1);
        int seed=spell.id().hashCode();

        if ("meteor_swarm".equals(spell.id())) { meteorSwarm(m,targetOffset,age,impactAge,scale); return m.build(); }
        if ("power_word_kill".equals(spell.id())) { executionWord(m,face,targetOffset,age,scale); return m.build(); }
        if ("chain_lightning".equals(spell.id())) { chainLightning(m,face,direction,targetOffset,age,scale,seed); return m.build(); }
        if ("fire_storm".equals(spell.id())) { fireStorm(m,ground,targetOffset,age,scale); return m.build(); }
        if ("world_sunder".equals(spell.id()) || "earthquake".equals(spell.id())) { worldFault(m,ground,targetOffset,range,age,scale,spell.id()); return m.build(); }
        if ("phoenix_requiem".equals(spell.id())) { phoenix(m,face,targetOffset,age,scale); return m.build(); }

        switch(s.form()) {
            case NEEDLE -> releaseNeedle(m,spell,face,direction,targetOffset,travel,age,scale,seed);
            case ORB -> releaseOrb(m,spell,face,direction,targetOffset,travel,age,scale,seed);
            case VOLLEY -> releaseVolley(m,spell,face,direction,targetOffset,travel,age,scale,seed,s.satellites());
            case RAY -> releaseRay(m,spell,face,direction,targetOffset,age,scale,seed);
            case CONE -> releaseCone(m,spell,face,direction,range,age,scale);
            case FIELD -> releaseField(m,spell,ground,targetOffset,range,age,scale,false);
            case DOMAIN -> releaseField(m,spell,ground,targetOffset,range,age,scale,true);
            case WALL -> releaseWall(m,spell,face,targetOffset,range,age,scale);
            case GATE -> releaseGate(m,spell,face,direction,targetOffset,age,scale,seed);
            case PRISON -> releasePrison(m,spell,targetOffset,range,age,scale,seed);
            case SKY -> releaseSky(m,spell,face,targetOffset,age,impactAge,scale,s);
            case WEATHER -> releaseWeather(m,spell,ground,targetOffset,range,age,scale,s);
            case AURA -> releaseAura(m,spell,ground,age,scale,seed);
            case MARK -> releaseMark(m,spell,face,targetOffset,age,scale,seed);
            case SHIFT -> releaseShift(m,spell,face,direction,targetOffset,age,scale,seed);
            case TRANSFORM -> releaseTransform(m,spell,ground,age,scale,seed);
            case CLOCK -> releaseClock(m,spell,ground,age,scale,seed);
            case TERRAIN -> releaseTerrain(m,spell,ground,targetOffset,range,age,scale,seed);
        }
        if(fusion && ingredients>1 && fade>0){
            double r=(.45+ingredients*.12)*scale;
            m.brokenBand(face,Vec3.ZERO,r*.84,r,36+ingredients*8,5,1.14F,(float)(.20*fade));
        }
        return m.build();
    }

    static boolean isPrismatic(SpellDefinition s){return "prismatic_spray".equals(s.id())||"prismatic_wall".equals(s.id());}
    static int prismaticColor(int layer){int[] c={0xFFFF2348,0xFFFF8A24,0xFFFFE63B,0xFF39EE77,0xFF35A9FF,0xFF7657FF,0xFFE055FF};return c[Math.floorMod(layer,c.length)];}
    static ArcaneWorldMesh prismaticAccent(SpellDefinition spell, Vec3 direction, Vec3 targetOffset, double range, double age, int layer){
        ArcaneWorldMesh.Builder m=ArcaneWorldMesh.builder(900);
        ArcaneWorldMesh.Basis face=ArcaneWorldMesh.Basis.facing(direction);
        double offset=(layer-3)*.12;
        if("prismatic_wall".equals(spell.id())){
            double width=SpellMetrics.wallWidth(spell.id(),range,spell.circle());
            Vec3 right=face.right(); Vec3 a=targetOffset.add(right.scale(-width*.5+layer*width/7.0));
            Vec3 b=targetOffset.add(right.scale(-width*.5+(layer+1)*width/7.0));
            m.face(a,b,b.add(0,3.8+spell.circle()*.25,0),a.add(0,3.8+spell.circle()*.25,0),1.05F,(float)(.22*(1-age)));
        }else{
            Vec3 dir=direction.add(face.right().scale(offset)).normalize();
            m.beamPrism(Vec3.ZERO,dir,ArcaneWorldMesh.Basis.facing(dir),Math.max(3,range*.82),.035+layer*.004,1.16F,(float)(.32*(1-age*.55)));
        }
        return m.build();
    }

    static int color(SpellDefinition spell) {
        return switch(spell.id()){
            case "disintegrate" -> 0xFF6BFF22;
            case "sunbeam","sunburst","foresight","true_seeing","solar_guard" -> 0xFFFFE55A;
            case "circle_of_death","finger_of_death","power_word_kill","eyebite" -> 0xFFFF224E;
            case "weird","phantasmal_killer","feeblemind" -> 0xFFFF36D7;
            case "flesh_to_stone" -> 0xFFD4DAE8;
            case "move_earth","earthquake","world_sunder" -> 0xFFFFA43B;
            case "time_stop" -> 0xFF64EFFF;
            case "wish" -> 0xFFF4A8FF;
            case "prismatic_spray","prismatic_wall" -> 0xFFFFFFFF;
            default -> switch(spell.school()){
                case FIRE -> 0xFFFF321A; case FROST -> 0xFF31D9FF; case WIND -> 0xFF3DFFC4;
                case WARD -> 0xFFAA5CFF; case LIFE -> 0xFF4AFF72; case SPACE -> 0xFFE052FF;
                default -> 0xFF5B78FF;
            };
        };
    }

    private static void chargeNeedle(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,double u,double p,double t,int seed){
        Vec3 nose=dir.scale(.75+u*.45), back=dir.scale(.18);
        double open=u*(.28+.45*p);
        m.line(back.add(b.right().scale(-open)),nose,1.20F).line(back.add(b.right().scale(open)),nose,1.20F);
        m.line(back.add(b.up().scale(-open*.55)),nose,0.82F).line(back.add(b.up().scale(open*.55)),nose,0.82F);
        if(p>.35)m.runeGlyph(b,nose.subtract(dir.scale(u*.20)),u*.16,seed,t*.5,0.84F);
    }
    private static void chargeOrb(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,double u,double p,double t,int seed,int detail){
        Vec3 c=dir.scale(.9+u*.25);double r=u*(.18+.38*p);m.orb(c,r,18+detail*2,1.08F,(float)(.18+.20*p));
        for(int i=0;i<3+detail/2;i++){double a=t*(i%2==0?1:-.7)+i*2.17;Vec3 n=c.add(b.point(a,r*(1.5+i*.18)));m.shard(n,dir,b,r*.70,r*.12,1.10F,.30F);}
    }
    private static void chargeVolley(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,double u,double p,double t,int seed,int satellites){
        int n=Math.max(3,Math.min(9,satellites));for(int i=0;i<n;i++){double a=Math.PI*2*i/n+t*.28;Vec3 c=dir.scale(.70+u*.35).add(b.point(a,u*(.45+.12*p)));m.diamond(b,c,u*.12,a,1.12F,.22F);m.line(c,c.add(dir.scale(u*(.35+.30*p))),.76F);}
    }
    private static void chargeRay(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,double u,double p,double t,int seed){
        for(int i=0;i<3;i++){double d=u*(.18+i*.19);Vec3 c=dir.scale(.40+i*u*.22);m.polygon(b,c,d*(.7+.2*p),4+i,t*(i%2==0?.3:-.2)+i,.90F);}m.line(Vec3.ZERO,dir.scale(1.0+u*p),1.35F);
    }
    private static void chargeCone(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,double u,double p,double t,int seed){
        Vec3 tip=dir.scale(.35);double r=u*(.4+.5*p);for(int i=0;i<6;i++){Vec3 e=dir.scale(1.0+u*.35).add(b.point(t*.18+i*Math.PI/3,r));m.line(tip,e,i%2==0?1.1F:.65F);} }
    private static void chargeField(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,Vec3 target,double u,double p,double t,int seed,boolean domain){
        Vec3 c=s.sigilAnchor()==SpellDefinition.SigilAnchor.GROUND_SELF?Vec3.ZERO:target;double r=u*(domain?2.3:1.45)*( .30+.70*p);
        int spokes=domain?12:7;for(int i=0;i<spokes;i++){double a=Math.PI*2*i/spokes+t*.08;Vec3 a0=c.add(g.point(a,r*.20)),a1=c.add(g.point(a+(i%2==0?.12:-.08),r));m.line(a0,a1,i%3==0?1.22F:.68F);}if(p>.45)m.brokenBand(g,c,r*.82,r,56,domain?7:5,1.05F,.20F);
    }
    private static void chargeWall(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis ground,ArcaneWorldMesh.Basis face,Vec3 target,double range,double u,double p,double t,int seed){
        double width=SpellMetrics.wallWidth(s.id(),range,s.circle())*(.22+.78*p);Vec3 right=face.right();int pylons=Math.max(3,Math.min(9,3+s.circle()/2));for(int i=0;i<pylons;i++){double x=(i/(double)(pylons-1)-.5)*width;Vec3 base=target.add(right.scale(x));m.line(base,base.add(0,.8+u*.8,0),i==0||i==pylons-1?1.35F:.72F);if(i>0)m.line(target.add(right.scale(((i-1)/(double)(pylons-1)-.5)*width)),base,.62F);} }
    private static void chargeGate(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,Vec3 target,double u,double p,double t,int seed){
        Vec3 near=dir.scale(.8+u*.25);double h=u*(1.0+.5*p),w=h*.62;gateFrame(m,b,near,w,h,t,seed);if(p>.52)gateFrame(m,b,target,w*(.72+.28*p),h*(.72+.28*p),-t,seed^31);if(p>.70)m.line(near,target,.58F);
    }
    private static void chargePrison(ArcaneWorldMesh.Builder m,SpellDefinition s,Vec3 target,double u,double p,double t,int seed){
        double r=u*(.55+.55*p),h=u*(1.3+.7*p);cage(m,target,r,h,4+Math.min(5,s.circle()/2),t,.34F);
    }
    private static void chargeSky(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,Vec3 target,double u,double p,double t,int seed,Signature sig){
        double y=Math.max(6,sig.altitude())*(.35+.65*p);Vec3 sky=target.add(0,y,0);int n=Math.max(1,sig.satellites());for(int i=0;i<n;i++){double a=Math.PI*2*i/n+t*.11;Vec3 c=sky.add(g.point(a,u*(1.1+n*.08)));m.diamond(g,c,u*.24,a,1.12F,.22F);m.line(c,target,.58F);}if(p>.5)m.brokenBand(g,sky,u*1.2,u*1.5,64,7,1.18F,.20F);
    }
    private static void chargeWeather(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,Vec3 target,double u,double p,double t,int seed,Signature sig){
        Vec3 sky=target.add(0,Math.max(8,sig.altitude()),0);double r=u*(1.3+1.6*p);for(int i=0;i<8;i++){double a=Math.PI*2*i/8+t*.07;Vec3 e=sky.add(g.point(a,r));m.line(sky,e,i%2==0?1.1F:.6F);m.diamond(g,e,u*.16,a,1.0F,.18F);}m.line(sky,target,.75F);
    }
    private static void chargeAura(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,double u,double p,double t,int seed){
        for(int i=0;i<3;i++){double y=-.75+i*.78;double r=u*(.45+i*.17)*(.55+.45*p);m.arc(g,new Vec3(0,y,0),r,t*(i%2==0?.4:-.3)+i,Math.PI*1.45,26,.82F);} }
    private static void chargeMark(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 target,double u,double p,double t,int seed){
        double r=u*(.28+.40*p);m.runeGlyph(b,target,r,seed,t*.18,1.18F);if(p>.60){m.line(target.add(b.right().scale(-r*1.4)),target.add(b.right().scale(r*1.4)),.70F);m.line(target.add(b.up().scale(-r*1.4)),target.add(b.up().scale(r*1.4)),.70F);} }
    private static void chargeShift(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,Vec3 target,double u,double p,double t,int seed){
        Vec3 near=dir.scale(.45);double r=u*(.42+.35*p);m.arc(b,near,r,t,.75*Math.PI,22,1.08F);m.arc(b,near,r,-t+Math.PI, .75*Math.PI,22,.70F);if(p>.55){m.arc(b,target,r*.8,-t,.9*Math.PI,24,1.0F);m.line(near,target,.52F);} }
    private static void chargeTransform(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,double u,double p,double t,int seed){
        for(int i=0;i<5;i++){double y=-.85+i*.47;double r=u*(.25+.08*i)*(.5+.5*p);m.polygon(g,new Vec3(0,y,0),r,3+(i+seed&3),t*.16+i*.4,.76F);} }
    private static void chargeClock(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,double u,double p,double t,int seed){
        double r=u*(1.0+.8*p);m.circle(g,Vec3.ZERO,r,72,1.0F);for(int i=0;i<12;i++){double a=Math.PI*2*i/12;Vec3 a0=g.point(a,r*.82),a1=g.point(a,r);m.line(a0,a1,i%3==0?1.2F:.65F);}m.line(Vec3.ZERO,g.point(-Math.PI/2+t*.08,r*.65),1.3F);m.line(Vec3.ZERO,g.point(-Math.PI/2-t*.22,r*.42),.85F);
    }
    private static void chargeTerrain(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,Vec3 target,double range,double u,double p,double t,int seed){
        double r=Math.max(u*1.3,SpellMetrics.effectRadius(s.id(),range,s.circle())*(.22+.48*p));for(int i=0;i<8+s.circle();i++){double a=Math.PI*2*i/(8+s.circle())+(seed%17)*.03;Vec3 start=target.add(g.point(a,r*.08));Vec3 mid=target.add(g.point(a+.10*Math.sin(i),r*.52));Vec3 end=target.add(g.point(a-.07*Math.cos(i),r));m.line(start,mid,i%3==0?1.25F:.72F).line(mid,end,.62F);} }

    private static void releaseNeedle(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,Vec3 target,double travel,double age,double scale,int seed){Vec3 pos=target.scale(easeOut(travel));double len=(.65+s.circle()*.10)*scale;m.shard(pos,dir,b,len,.10*scale,1.22F,.46F);for(int i=1;i<=3;i++){double t=Math.max(0,easeOut(travel)-i*.035);Vec3 e=target.scale(t);m.line(e.subtract(dir.scale(.12*scale)),e,.45F);}impactSpark(m,b,target,age,scale*.55,seed);}
    private static void releaseOrb(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,Vec3 target,double travel,double age,double scale,int seed){Vec3 pos=target.scale(Math.pow(travel,1.08));double r=(.22+s.circle()*.035)*scale;m.orb(pos,r,20,1.14F,.38F);m.helix(pos.subtract(dir.scale(r*2.2)),dir,b,r*3.0,r*.8,2,28,.62F,true);impactSpark(m,b,target,age,scale,seed);}
    private static void releaseVolley(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,Vec3 target,double travel,double age,double scale,int seed,int satellites){int n=Math.max(3,Math.min(9,satellites));for(int i=0;i<n;i++){double local=clamp((travel-i*.035)/(1-i*.035),0,1);Vec3 lane=b.point(i*2.399+seed*.001,.28*scale*(1-local));Vec3 pos=target.scale(easeOut(local)).add(lane);m.shard(pos,dir,b,.38*scale,.07*scale,1.15F,.38F);}impactSpark(m,b,target,age,scale*.8,seed);}
    private static void releaseRay(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,Vec3 target,double age,double scale,int seed){double len=Math.max(.5,target.length());double reveal=clamp(age/.10,0,1),fade=clamp((1-age)/.20,0,1);m.beamPrism(Vec3.ZERO,dir,b,len*reveal,.035*scale,1.28F,(float)(.48*fade));for(int i=1;i<4;i++){Vec3 c=dir.scale(len*i/4.0);m.polygon(b,c,.13*scale,4+i,age+i,.72F);}if(age>.72)impactSpark(m,b,target,age,scale,seed);}
    private static void releaseCone(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,double range,double age,double scale){double len=SpellMetrics.waveLength(range),rad=SpellMetrics.waveEndRadius(s.id(),range,s.circle());double p=easeOut(clamp(age/.72,0,1));m.cone(Vec3.ZERO,dir,b,len*p,rad*p,7+s.circle()/2,2+s.circle()/3,.62F);}
    private static void releaseField(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,Vec3 target,double range,double age,double scale,boolean domain){double r=SpellMetrics.effectRadius(s.id(),range,s.circle())*(domain?1.08:1.0);double p=clamp(age/.28,0,1),fade=clamp((1-age)/.22,0,1);double rr=r*(.18+.82*easeOut(p));m.band(g,target,rr*.92,rr,72,1.15F,(float)(.34*fade));int spokes=domain?16:8;for(int i=0;i<spokes;i++){double a=Math.PI*2*i/spokes; m.line(target.add(g.point(a,rr*.18)),target.add(g.point(a+(i%2==0?.06:-.06),rr)),i%4==0?1.05F:.55F);} }
    private static void releaseWall(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 target,double range,double age,double scale){double width=SpellMetrics.wallWidth(s.id(),range,s.circle());double h=2.6+s.circle()*.22,p=clamp(age/.22,0,1),fade=clamp((1-age)/.20,0,1);Vec3 right=b.right();int panels=8+Math.min(10,s.circle());for(int i=0;i<panels;i++){double x0=(i/(double)panels-.5)*width,x1=((i+1)/(double)panels-.5)*width;Vec3 a=target.add(right.scale(x0)),c=target.add(right.scale(x1));m.face(a,c,c.add(0,h*p,0),a.add(0,h*p,0),i%2==0?1.0F:.76F,(float)(.25*fade));}}
    private static void releaseGate(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,Vec3 target,double age,double scale,int seed){double f=clamp((1-age)/.20,0,1),h=(1.2+s.circle()*.12)*scale,w=h*.62;gateFrame(m,b,dir.scale(.5),w,h,age*4,seed);gateFrame(m,b,target,w,h,-age*3,seed^31);if(age<.55)m.ribbon(dir.scale(.55),target.normalize(),b,Math.max(1,target.length()-1),w*.5,2,28,1.0F,(float)(.16*f));}
    private static void releasePrison(ArcaneWorldMesh.Builder m,SpellDefinition s,Vec3 target,double range,double age,double scale,int seed){double r=Math.max(.65,SpellMetrics.effectRadius(s.id(),range,s.circle())*.25)*scale,h=2.0+s.circle()*.18,p=clamp(age/.22,0,1),fade=clamp((1-age)/.22,0,1);cage(m,target,r*p,h*p,4+Math.min(6,s.circle()/2),age*2,(float)(.30*fade));}
    private static void releaseSky(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 target,double age,double impact,double scale,Signature sig){double imp=Math.max(.12,impact<=0?.58:impact),t=clamp(age/imp,0,1),alt=Math.max(6,sig.altitude());int n=Math.max(1,sig.satellites());for(int i=0;i<n;i++){double a=Math.PI*2*i/n;Vec3 hit=target.add(Math.cos(a)*sig.spread(),0,Math.sin(a)*sig.spread());Vec3 pos=hit.add(0,alt*(1-easeIn(t)),0);m.shard(pos,new Vec3(0,-1,0),ArcaneWorldMesh.Basis.facing(new Vec3(0,-1,0)),1.6*scale,.22*scale,1.25F,.44F);if(age>=imp)impactRing(m,ArcaneWorldMesh.Basis.ground(),hit,scale*(1.2+s.circle()*.12),age-imp);}}
    private static void releaseWeather(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,Vec3 target,double range,double age,double scale,Signature sig){double r=Math.max(8,SpellMetrics.effectRadius(s.id(),range,s.circle())*1.2),fade=clamp((1-age)/.18,0,1);Vec3 sky=target.add(0,Math.max(7,sig.altitude()),0);m.brokenBand(g,sky,r*.78,r,84,7,1.12F,(float)(.22*fade));for(int i=0;i<10;i++){double a=i*2.399+age;Vec3 p=sky.add(g.point(a,r*(.25+.06*i)));m.line(p,p.add(0,-(2+s.circle()*.5),0),i%3==0?1.0F:.55F);} }
    private static void releaseAura(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,double age,double scale,int seed){double fade=clamp((1-age)/.20,0,1),r=(.8+s.circle()*.10)*scale;for(int i=0;i<4;i++){double y=-.8+i*.58;m.arc(g,new Vec3(0,y,0),r*(.65+i*.12),age*3+i*.6,Math.PI*1.35,28,i%2==0?1.05F:.62F);}m.orb(new Vec3(0,.2,0),r*.55,18,1.05F,(float)(.16*fade));}
    private static void releaseMark(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 target,double age,double scale,int seed){double fade=clamp((1-age)/.15,0,1),r=(.38+s.circle()*.055)*scale*(.7+age);m.runeGlyph(b,target,r,seed,age,1.25F);m.line(target.add(b.right().scale(-r*1.8)),target.add(b.right().scale(r*1.8)),.70F);m.line(target.add(b.up().scale(-r*1.8)),target.add(b.up().scale(r*1.8)),.70F);if(age>.55)m.orb(target,r*.55,16,1.2F,(float)(.28*fade));}
    private static void releaseShift(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis b,Vec3 dir,Vec3 target,double age,double scale,int seed){double r=(.65+s.circle()*.06)*scale,fade=clamp((1-age)/.18,0,1);m.arc(b,Vec3.ZERO,r,age*4,Math.PI*1.5,36,1.08F);m.arc(b,target,r,-age*3,Math.PI*1.5,36,1.08F);for(int i=0;i<5;i++){double t=i/4.0;m.line(b.point(i*1.2,r*.7),target.add(b.point(-i*.9,r*.7)),.45F);} }
    private static void releaseTransform(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,double age,double scale,int seed){double r=(.48+s.circle()*.06)*scale;for(int i=0;i<7;i++){double y=-.9+i*.34,phase=clamp(age*1.4-i*.06,0,1);m.polygon(g,new Vec3(0,y,0),r*(.45+.55*phase),3+Math.floorMod(seed+i,5),age+i*.5,.78F);} }
    private static void releaseClock(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,double age,double scale,int seed){double r=(1.5+s.circle()*.12)*scale,fade=clamp((1-age)/.15,0,1);m.circle(g,Vec3.ZERO,r,84,1.2F);for(int i=0;i<12;i++){double a=Math.PI*2*i/12;m.line(g.point(a,r*.82),g.point(a,r),i%3==0?1.3F:.65F);}double hand=-Math.PI/2+(age<.55?age*6.0:3.3);m.line(Vec3.ZERO,g.point(hand,r*.72),1.45F);if(age>.5)m.brokenBand(g,Vec3.ZERO,r*1.18,r*1.28,72,6,1.1F,(float)(.22*fade));}
    private static void releaseTerrain(ArcaneWorldMesh.Builder m,SpellDefinition s,ArcaneWorldMesh.Basis g,Vec3 target,double range,double age,double scale,int seed){worldFault(m,g,target,range,age,scale,s.id());}

    private static void meteorSwarm(ArcaneWorldMesh.Builder m,Vec3 target,double age,double impact,double scale){ArcaneWorldMesh.Basis down=ArcaneWorldMesh.Basis.facing(new Vec3(0,-1,0));ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double imp=Math.max(.15,impact<=0?.62:impact),t=clamp(age/imp,0,1);double[][] o={{-10,-10},{10,-10},{-10,10},{10,10}};for(int i=0;i<4;i++){Vec3 hit=target.add(o[i][0],0,o[i][1]);Vec3 pos=hit.add(0,28*(1-easeIn(t)),0);m.orb(pos,1.05*scale,26,1.20F,.44F);m.shard(pos,new Vec3(0,-1,0),down,3.1*scale,.62*scale,1.18F,.38F);if(age>=imp){double a=clamp((age-imp)/.30,0,1);double r=11*a;m.band(g,hit,r*.82,r,70,1.22F,(float)(.34*(1-a)));}}}
    private static void executionWord(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis b,Vec3 target,double age,double scale){double r=.42*scale*(1+age*.28);m.runeGlyph(b,target,r,0xDEAD,0,1.55F);m.line(target.add(b.right().scale(-r*1.4)),target.add(b.right().scale(r*1.4)),1.0F);if(age>.35){Vec3 top=target.add(0,1.35,0),bottom=target.add(0,-1.1,0);m.line(top,bottom,1.65F);m.orb(target,.18*scale,14,1.35F,(float)(.42*(1-age)));}}
    private static void chainLightning(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis b,Vec3 dir,Vec3 target,double age,double scale,int seed){int branches=6;Vec3 prev=Vec3.ZERO;for(int i=1;i<=branches;i++){double t=i/(double)branches;Vec3 p=target.scale(t).add(b.point(seed*.001+i*2.17,.18*scale*Math.sin(Math.PI*t)));m.line(prev,p,i%2==0?.72F:1.25F);if(i>2)m.line(p,p.add(b.point(i*.91,.55*scale)),.48F);prev=p;}impactSpark(m,b,target,age,scale,seed);}
    private static void fireStorm(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis g,Vec3 target,double age,double scale){for(int i=0;i<6;i++){double a=Math.PI*2*i/6;Vec3 hit=target.add(g.point(a,4.0+i*.35));double h=(6.5+i*.5)*(1-clamp(age/.72,0,1));Vec3 top=hit.add(0,h,0);m.beamPrism(top,new Vec3(0,-1,0),ArcaneWorldMesh.Basis.facing(new Vec3(0,-1,0)),Math.max(.2,h),.18*scale,1.18F,.32F);impactRing(m,g,hit,scale*(.8+i*.06),Math.max(0,age-.55));}}
    private static void worldFault(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis g,Vec3 target,double range,double age,double scale,String id){double r=Math.max(7,SpellMetrics.effectRadius(id,range,9))*clamp(age/.55,0,1);int n="world_sunder".equals(id)?14:10;for(int i=0;i<n;i++){double a=Math.PI*2*i/n+i*.17;Vec3 p0=target.add(g.point(a,r*.05));Vec3 p1=target.add(g.point(a+.12*Math.sin(i*2.1),r*.50));Vec3 p2=target.add(g.point(a-.09*Math.cos(i*.7),r));m.line(p0,p1,i%3==0?1.45F:.82F).line(p1,p2,.72F);} }
    private static void phoenix(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis b,Vec3 target,double age,double scale){double spread=(1.0+age*2.2)*scale;Vec3 c=target.add(0,.8,0);for(int side:new int[]{-1,1}){Vec3 root=c.add(b.right().scale(.18*side));Vec3 mid=c.add(b.right().scale(spread*.65*side)).add(b.up().scale(spread*.55));Vec3 tip=c.add(b.right().scale(spread*side)).add(b.up().scale(spread*.15));m.line(root,mid,1.45F).line(mid,tip,1.15F);for(int i=1;i<=4;i++){double f=i/4.0;m.line(mid,root.add(b.right().scale(spread*f*side)).add(b.up().scale(-spread*.35*f)),.58F);}}m.orb(c,.32*scale,18,1.25F,(float)(.36*(1-age)));}

    private static void gateFrame(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis b,Vec3 c,double w,double h,double rot,int seed){Vec3 r=b.right().scale(w),u=b.up().scale(h);m.line(c.subtract(r).subtract(u),c.add(r).subtract(u),1.25F);m.line(c.add(r).subtract(u),c.add(r).add(u),1.25F);m.line(c.add(r).add(u),c.subtract(r).add(u),1.25F);m.line(c.subtract(r).add(u),c.subtract(r).subtract(u),1.25F);m.runeGlyph(b,c,u.length()*.22,seed,rot,.72F);}
    private static void cage(ArcaneWorldMesh.Builder m,Vec3 c,double r,double h,int sides,double rot,float alpha){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();for(int i=0;i<sides;i++){double a=rot+Math.PI*2*i/sides;Vec3 bot=c.add(g.point(a,r)).add(0,-h*.5,0),top=bot.add(0,h,0);m.line(bot,top,i%2==0?1.3F:.72F);if(i>0){double prev=rot+Math.PI*2*(i-1)/sides;m.line(c.add(g.point(prev,r)).add(0,h*.5,0),top,.62F);}}}
    private static void impactSpark(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis b,Vec3 target,double age,double scale,int seed){if(age<.68)return;double p=clamp((age-.68)/.32,0,1),r=(.3+1.6*p)*scale;for(int i=0;i<8;i++){double a=Math.PI*2*i/8+seed*.0001;m.line(target.add(b.point(a,r*.18)),target.add(b.point(a,r)),i%2==0?1.05F:.58F);} }
    private static void impactRing(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis g,Vec3 hit,double scale,double localAge){if(localAge<=0)return;double p=clamp(localAge/.30,0,1),r=scale*(.4+2.4*p);m.band(g,hit,r*.82,r,48,1.18F,(float)(.32*(1-p)));}

    private static double powerScale(SpellDefinition s,double power){return clamp(Math.pow(Math.max(.08,power/Math.max(1,s.power())),.18),.82,1.9);}
    private static double easeOut(double t){t=clamp(t,0,1);return 1-Math.pow(1-t,2.2);}
    private static double easeIn(double t){t=clamp(t,0,1);return t*t;}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}

    private static Signature signature(SpellDefinition s){
        String id=s.id(); int c=s.circle();
        return switch(id){
            case "magic_missile" -> sig(Form.VOLLEY,.82,2,5,.18,0,3.2);
            case "fire_bolt","ray_of_frost","ice_knife","void_lance","finger_of_death","disintegrate" -> sig(Form.NEEDLE,id.equals("void_lance")?1.35:1.0,3,1,.1,0,4.0);
            case "scorching_ray","lightning_bolt","sunbeam","chain_lightning","prismatic_spray" -> sig(Form.RAY,1.05,4,id.equals("chain_lightning")?6:1,.2,0,5.2);
            case "fireball","chromatic_orb","freezing_sphere" -> sig(Form.ORB,id.equals("delayed_blast_fireball")?1.45:1.0,4,1,.2,0,2.7);
            case "burning_hands","thunderwave","gust_of_wind","shatter","cone_of_cold","steam_burst" -> sig(Form.CONE,1.0,3,1,.45,0,3.5);
            case "grease","sleep","web","slow","confusion","sleet_storm","ice_storm","cloudkill","insect_plague","antimagic_field","incendiary_cloud" -> sig(Form.FIELD,1.0,4,1,.4,0,2.1);
            case "winter_domain","circle_of_death","sunburst" -> sig(Form.DOMAIN,1.35,6,1,.5,0,1.8);
            case "wall_of_fire","wind_wall","wall_of_ice","wall_of_force","prismatic_wall" -> sig(Form.WALL,1.0,5,1,.5,0,2.4);
            case "misty_step","dimension_door","passwall","plane_shift","teleport","demiplane","gate","teleportation_circle" -> sig(Form.GATE,id.equals("gate")?1.7:1.0,5,2,.4,0,2.2);
            case "blink","etherealness" -> sig(Form.SHIFT,1.0,4,2,.3,0,4.1);
            case "hold_person","hold_monster","resilient_sphere","forcecage","astral_prison","maze","thunder_cage" -> sig(Form.PRISON,id.equals("forcecage")||id.equals("astral_prison")?1.45:1.0,5,1,.2,0,2.5);
            case "flame_strike","fire_storm","meteor_swarm","delayed_blast_fireball" -> sig(Form.SKY,id.equals("meteor_swarm")?2.3:1.2,6,id.equals("meteor_swarm")?4:id.equals("fire_storm")?6:1,id.equals("meteor_swarm")?10:3,id.equals("meteor_swarm")?28:14,1.9);
            case "control_weather" -> sig(Form.WEATHER,2.1,7,8,8,18,1.2);
            case "mage_armor","shield","mirror_image","blur","invisibility","greater_invisibility","stoneskin","freedom_of_movement","true_seeing","globe_of_invulnerability","fire_shield","solar_guard","foresight","haste","protection_from_energy" -> sig(Form.AURA,1.0,4,1,.2,0,2.8);
            case "power_word_kill","eyebite","phantasmal_killer","feeblemind","dominate_person","dominate_monster","mass_suggestion","vampiric_touch" -> sig(Form.MARK,id.equals("power_word_kill")?.72:1.0,5,1,.1,0,3.0);
            case "shapechange","true_polymorph","flesh_to_stone","clone","simulacrum" -> sig(Form.TRANSFORM,1.1,5,1,.2,0,2.0);
            case "time_stop","wish" -> sig(Form.CLOCK,id.equals("wish")?.82:1.3,7,1,.2,0,1.4);
            case "move_earth","earthquake","world_sunder","reverse_gravity" -> sig(Form.TERRAIN,id.equals("world_sunder")?2.0:1.25,6,1,.7,0,1.6);
            case "weird" -> sig(Form.DOMAIN,1.65,7,7,.7,0,1.7);
            case "phoenix_requiem" -> sig(Form.SKY,1.65,7,1,2,11,2.0);
            default -> switch(s.sigilAnchor()){
                case GROUND_SELF,GROUND_TARGET,FEET -> sig(Form.FIELD,.9+c*.035,3+c/3,1,.35,0,2.6);
                case TARGET -> sig(Form.MARK,.85+c*.035,3+c/3,1,.18,0,3.0);
                case BODY -> sig(Form.AURA,.9+c*.03,3+c/3,1,.18,0,2.7);
                case FRONT -> sig(c>=6?Form.ORB:Form.NEEDLE,.88+c*.035,3+c/3,Math.max(1,c/3),.2,0,3.4);
            };
        };
    }
    private static Signature sig(Form f,double scale,int detail,int satellites,double spread,double altitude,double tempo){return new Signature(f,scale,detail,satellites,spread,altitude,tempo);}
}
