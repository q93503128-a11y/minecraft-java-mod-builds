package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
import net.minecraft.world.phys.Vec3;

/** Phase 3 authored presentation for every 7C-9C normal and fusion formula. */
final class ArchmageVisualIdentity {
    private ArchmageVisualIdentity() {}
    static boolean owns(SpellDefinition spell) { return spell != null && spell.circle() >= 7 && spell.circle() <= 9; }

    static void appendCharge(SpellDefinition spell, SpellPresentationProfile.Profile profile,
            double outer, double rotation, double progress, Vec3 direction, Vec3 target,
            double range, ArcaneWorldMesh.Builder mesh) {
        switch (spell.id()) {
            case "delayed_blast_fireball" -> delayedBlastCore(target,outer,rotation,progress,mesh);
            case "etherealness" -> etherealPhaseShell(outer,rotation,progress,mesh);
            case "finger_of_death" -> deathFingerWrit(target,outer,rotation,progress,mesh);
            case "fire_storm" -> fireStormSkyGrid(target,outer,rotation,progress,mesh);
            case "forcecage" -> forceCageArchitecture(target,outer,rotation,progress,mesh);
            case "plane_shift" -> planeShiftLayers(direction,outer,rotation,progress,mesh);
            case "prismatic_spray" -> prismaticFanAperture(direction,outer,rotation,progress,mesh);
            case "reverse_gravity" -> reverseGravityInverter(target,outer,rotation,progress,range,mesh);
            case "simulacrum" -> simulacrumMirror(outer,rotation,progress,mesh);
            case "teleport" -> teleportAddress(target,outer,rotation,progress,mesh);
            case "void_lance" -> voidLanceSeam(direction,outer,rotation,progress,mesh);
            case "winter_domain" -> winterDomainCrown(outer,rotation,progress,range,mesh);

            case "antimagic_field" -> antimagicNullEngine(outer,rotation,progress,range,mesh);
            case "clone" -> cloneHeartCircuit(outer,rotation,progress,mesh);
            case "control_weather" -> weatherCommandAxes(outer,rotation,progress,range,mesh);
            case "demiplane" -> demiplaneRoom(direction,outer,rotation,progress,mesh);
            case "dominate_monster" -> monsterYoke(target,outer,rotation,progress,mesh);
            case "earthquake" -> earthquakeFaultMap(target,outer,rotation,progress,range,mesh);
            case "feeblemind" -> feeblemindLattice(target,outer,rotation,progress,mesh);
            case "incendiary_cloud" -> incendiaryCanopy(target,outer,rotation,progress,range,mesh);
            case "maze" -> mazeFold(target,outer,rotation,progress,mesh);
            case "sunburst" -> sunburstLens(target,outer,rotation,progress,mesh);
            case "astral_prison" -> astralPrisonFold(target,outer,rotation,progress,mesh);
            case "phoenix_requiem" -> phoenixRequiemWings(outer,rotation,progress,range,mesh);

            case "meteor_swarm" -> meteorCommandArray(target,outer,rotation,progress,mesh);
            case "power_word_kill" -> powerWordExecution(target,outer,rotation,progress,mesh);
            case "prismatic_wall" -> prismaticWallLoom(direction,target,outer,rotation,progress,range,mesh);
            case "shapechange" -> shapechangeTotem(outer,rotation,progress,mesh);
            case "time_stop" -> timeStopClockwork(outer,rotation,progress,range,mesh);
            case "true_polymorph" -> polymorphScaffold(target,outer,rotation,progress,mesh);
            case "weird" -> weirdMaskCouncil(target,outer,rotation,progress,range,mesh);
            case "wish" -> wishRealityKnot(outer,rotation,progress,mesh);
            case "gate" -> gateMonument(direction,target,outer,rotation,progress,mesh);
            case "foresight" -> foresightEye(outer,rotation,progress,mesh);
            case "world_sunder" -> worldSunderFault(target,outer,rotation,progress,range,mesh);
            default -> {}
        }
    }

    static void appendRelease(SpellDefinition spell, Vec3 direction, Vec3 target, double range,
            double age, double motion, double pf, ArcaneWorldMesh.Builder mesh) {
        switch (spell.id()) {
            case "delayed_blast_fireball" -> delayedBlastRelease(target,range,age,pf,mesh);
            case "etherealness" -> etherealRelease(age,pf,mesh);
            case "finger_of_death" -> fingerDeathRelease(direction,target,age,motion,pf,mesh);
            case "fire_storm" -> fireStormRelease(target,age,motion,pf,mesh);
            case "forcecage" -> forceCageRelease(target,age,pf,mesh);
            case "plane_shift" -> planeShiftRelease(direction,target,age,pf,mesh);
            case "prismatic_spray" -> prismaticSprayRelease(direction,range,age,pf,mesh);
            case "reverse_gravity" -> reverseGravityRelease(target,range,age,pf,mesh);
            case "simulacrum" -> simulacrumRelease(age,pf,mesh);
            case "teleport" -> teleportRelease(direction,target,age,pf,mesh);
            case "void_lance" -> voidLanceRelease(direction,range,age,motion,pf,mesh);
            case "winter_domain" -> winterDomainRelease(range,age,pf,mesh);

            case "antimagic_field" -> antimagicRelease(range,age,pf,mesh);
            case "clone" -> cloneRelease(age,pf,mesh);
            case "control_weather" -> weatherRelease(range,age,pf,mesh);
            case "demiplane" -> demiplaneRelease(direction,age,pf,mesh);
            case "dominate_monster" -> dominateMonsterRelease(target,age,pf,mesh);
            case "earthquake" -> earthquakeRelease(target,range,age,pf,mesh);
            case "feeblemind" -> feeblemindRelease(target,age,pf,mesh);
            case "incendiary_cloud" -> incendiaryRelease(target,range,age,pf,mesh);
            case "maze" -> mazeRelease(target,age,pf,mesh);
            case "sunburst" -> sunburstRelease(target,range,age,pf,mesh);
            case "astral_prison" -> astralPrisonRelease(target,age,pf,mesh);
            case "phoenix_requiem" -> phoenixRelease(range,age,pf,mesh);

            case "meteor_swarm" -> meteorSwarmRelease(target,age,motion,pf,mesh);
            case "power_word_kill" -> powerWordRelease(target,age,pf,mesh);
            case "prismatic_wall" -> prismaticWallRelease(direction,target,range,age,pf,mesh);
            case "shapechange" -> shapechangeRelease(age,pf,mesh);
            case "time_stop" -> timeStopRelease(range,age,pf,mesh);
            case "true_polymorph" -> polymorphRelease(target,age,pf,mesh);
            case "weird" -> weirdRelease(target,range,age,pf,mesh);
            case "wish" -> wishRelease(age,pf,mesh);
            case "gate" -> gateRelease(direction,target,age,pf,mesh);
            case "foresight" -> foresightRelease(age,pf,mesh);
            case "world_sunder" -> worldSunderRelease(target,range,age,pf,mesh);
            default -> {}
        }
    }

    // 7C charge identities
    private static void delayedBlastCore(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t); double q=phase(p,.03,.94); ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();
        m.orb(c.add(0,1.1,0),o*.20*q,22,1.18F,.32F); m.star(g,c,o*.82*q,o*.31*q,9,r,1.2F);
        for(int i=0;i<6;i++){double a=r+i*Math.PI/3;Vec3 n=c.add(g.point(a,o*.63*q));m.line(n,c.add(0,1.1,0),i%2==0?1.1F:.7F);}
    }
    private static void etherealPhaseShell(double o,double r,double p,ArcaneWorldMesh.Builder m){
        double q=phase(p,.02,.95); for(int i=0;i<5;i++){ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.fromNormal(new Vec3(Math.cos(r+i),.35,Math.sin(r+i)),new Vec3(0,1,0));m.brokenBand(b,new Vec3(0,1.0+i*.12,0),o*(.34+i*.055)*q,o*(.39+i*.055)*q,48,5,1.0F,.20F);}
    }
    private static void deathFingerWrit(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(t);double q=phase(p,.04,.96);Vec3 c=t;
        m.runeGlyph(f,c,o*.34*q,0xD347,r,1.28F);m.diamond(f,c,o*.72*q,r,1.1F,.22F);m.line(Vec3.ZERO,c.scale(q),1.35F);
    }
    private static void fireStormSkyGrid(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.03,.94),y=19*q;
        m.brokenBand(g,c.add(0,y,0),o*.68*q,o*.94*q,84,7,1.14F,.24F);
        for(int i=0;i<6;i++){double a=r+i*Math.PI/3;Vec3 n=c.add(g.point(a,5)).add(0,y,0);m.band(g,n,.30*q,.52*q,28,1.16F,.25F);m.line(n,n.add(0,-y,0),.74F);}
    }
    private static void forceCageArchitecture(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        cage(t,Math.max(1.8,o*.62),3.8,phase(p,.04,.94),r,m,true);
    }
    private static void planeShiftLayers(Vec3 d,double o,double r,double p,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);double q=phase(p,.04,.94);
        for(int i=0;i<5;i++){Vec3 c=d.scale(.8+i*.42);m.brokenBand(f,c,o*(.38+i*.06)*q,o*(.43+i*.06)*q,54,5+i%2,1.0F,.19F);}
        m.line(d.scale(.7),d.scale(3.2*q),1.0F);
    }
    private static void prismaticFanAperture(Vec3 d,double o,double r,double p,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);double q=phase(p,.04,.94);
        for(int i=-3;i<=3;i++){double a=i*.18;Vec3 tip=f.point(a,o*.76*q);m.line(Vec3.ZERO,tip,1.1F);m.diamond(f,tip,o*.12*q,r+i*.5,1.12F,.22F);}
        m.arc(f,Vec3.ZERO,o*.88*q,-1.2,2.4,42,1.22F);
    }
    private static void reverseGravityInverter(Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.03,.94),rad=Math.max(10,range*.32)*q;
        m.brokenBand(g,c,rad*.82,rad,80,7,1.03F,.18F);for(int i=0;i<8;i++){double a=r+i*Math.PI/4;Vec3 b=c.add(g.point(a,rad*.68));m.line(b,b.add(0,2.4*q,0),1.05F);m.line(b.add(0,2.4*q,0),b.add(g.point(a,.42*q)).add(0,1.8*q,0),.76F);}
    }
    private static void simulacrumMirror(double o,double r,double p,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));double q=phase(p,.04,.94);
        for(int s=-1;s<=1;s+=2){Vec3 c=new Vec3(s*o*.55,1.0,0);m.polygonPlate(f,c,o*.34*q,6,r*s,1.0F,.16F);m.line(new Vec3(0,.2,0),c,.76F);}
        m.brokenBand(ArcaneWorldMesh.Basis.ground(),new Vec3(0,.05,0),o*.58*q,o*.72*q,56,6,1.0F,.19F);
    }
    private static void teleportAddress(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){portalAddress(t,o,r,p,m,6);}
    private static void voidLanceSeam(Vec3 d,double o,double r,double p,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);double q=phase(p,.03,.96);m.star(f,Vec3.ZERO,o*.78*q,o*.18*q,6,r,1.3F);m.line(f.right().scale(-o*q),f.right().scale(o*q),1.35F);m.shard(d.scale(o*.3),d,f,o*1.2*q,o*.09,1.16F,.24F);
    }
    private static void winterDomainCrown(double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.03,.94),rad=Math.max(6,range*.45)*q;
        m.star(g,Vec3.ZERO,rad,rad*.56,12,r,1.18F);for(int i=0;i<12;i++){double a=r+i*Math.PI/6;Vec3 n=g.point(a,rad);m.shard(n,new Vec3(0,1,0),ArcaneWorldMesh.Basis.facing(new Vec3(0,1,0)),1.4*q,.14*q,1.05F,.20F);}
    }

    // 8C charge identities
    private static void antimagicNullEngine(double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.04,.94),rad=Math.max(9.0,range*.50)*q;
        m.brokenBand(g,Vec3.ZERO,rad*.88,rad,90,4,1.0F,.13F);for(int i=0;i<10;i++){double a=r+i*Math.PI/5;Vec3 a0=g.point(a,rad*.35),a1=g.point(a+.32,rad*.84);m.line(a0,a1,i%2==0?1.25F:.65F);}
    }
    private static void cloneHeartCircuit(double o,double r,double p,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));double q=phase(p,.04,.94);Vec3 h=new Vec3(0,1.15,0);
        m.diamond(f,h,o*.26*q,r,1.18F,.28F);for(int s=-1;s<=1;s+=2){Vec3 c=h.add(s*o*.7,0,0);m.orb(c,o*.16*q,18,.96F,.20F);m.line(h,c,1.08F);}m.brokenBand(ArcaneWorldMesh.Basis.ground(),Vec3.ZERO,o*.74*q,o*.86*q,68,7,1.0F,.18F);
    }
    private static void weatherCommandAxes(double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.03,.94),rad=Math.max(18,range*.55)*q,y=24*q;
        m.brokenBand(g,new Vec3(0,y,0),rad*.78,rad,104,7,1.05F,.17F);for(int i=0;i<8;i++){double a=r+i*Math.PI/4;Vec3 n=g.point(a,rad*.72).add(0,y,0);m.line(n,new Vec3(0,0,0),i%2==0?1.05F:.62F);}
    }
    private static void demiplaneRoom(Vec3 d,double o,double r,double p,ArcaneWorldMesh.Builder m){
        double q=phase(p,.04,.94);ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);Vec3 c=d.scale(2.0);
        roomFrame(c,f,o*.72*q,o*.58*q,o*.9*q,m);roomFrame(c.add(d.scale(o*.8*q)),f,o*.64*q,o*.52*q,o*.76*q,m);
        m.line(c,c.add(d.scale(o*.8*q)),1.0F);
    }
    private static void monsterYoke(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        double q=phase(p,.04,.94);ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(t);Vec3 c=t.add(0,1.35,0);
        m.star(f,c,o*.72*q,o*.28*q,8,r,1.28F);for(int i=0;i<6;i++){double a=r+i*Math.PI/3;Vec3 n=c.add(f.point(a,o*.82*q));m.line(n,c,1.0F);m.line(n,n.add(0,-1.4*q,0),.72F);}
    }
    private static void earthquakeFaultMap(Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.03,.94),rad=Math.max(8,range*.65)*q;
        for(int i=0;i<12;i++){double a=r+i*Math.PI/6;Vec3 start=c.add(g.point(a,rad*.12)),mid=c.add(g.point(a+.12*Math.sin(i),rad*.55)),end=c.add(g.point(a+.21*Math.cos(i),rad));m.line(start,mid,1.25F);m.line(mid,end,.85F);}m.brokenBand(g,c,rad*.90,rad,80,8,.92F,.15F);
    }
    private static void feeblemindLattice(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(t);Vec3 c=t.add(0,1.5,0);double q=phase(p,.04,.94);
        m.runeChords(f,c,o*.65*q,9,4,r,1.12F);for(int i=0;i<5;i++){Vec3 n=c.add(f.point(r+i*1.256,o*.8*q));m.line(n,c,.85F);}
    }
    private static void incendiaryCanopy(Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.03,.94),rad=Math.max(10,range*.30)*q;
        for(int layer=0;layer<4;layer++)m.brokenBand(g,c.add(0,3+layer*1.8,0),rad*(.70-layer*.05),rad*(.85-layer*.05),66,5+layer,1.05F,.20F);
        for(int i=0;i<7;i++){double a=r+i*6.283/7;Vec3 n=c.add(g.point(a,rad*.62)).add(0,5.4*q,0);m.shard(n,new Vec3(0,-1,0),ArcaneWorldMesh.Basis.facing(new Vec3(0,1,0)),1.2*q,.16*q,1.12F,.24F);}
    }
    private static void mazeFold(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        Vec3 c=t;ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(t);double q=phase(p,.03,.94);
        for(int i=0;i<6;i++){double s=o*(.28+i*.10)*q;m.polygon(f,c.add(0,(i%2)*.18,0),s,4,r+i*.25,i%2==0?1.2F:.7F);}
        for(int i=0;i<4;i++)m.line(c.add(f.point(r+i*Math.PI/2,o*.25*q)),c.add(f.point(r+(i+1)*Math.PI/2,o*.82*q)),.82F);
    }
    private static void sunburstLens(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t).add(0,12*phase(p,.40,.96),0);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.03,.94);
        m.disc(g,c,o*.62*q,56,1.18F,.20F);m.star(g,c,o*q,o*.34*q,16,r,1.28F);m.line(c,ground(t),1.0F);
    }
    private static void astralPrisonFold(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        double q=phase(p,.03,.94);cage(t,o*.62,4.6,q,r,m,true);ArcaneWorldMesh.Basis a=ArcaneWorldMesh.Basis.fromNormal(new Vec3(1,.6,1),new Vec3(0,1,0));m.brokenBand(a,t.add(0,1.4,0),o*.64*q,o*.74*q,58,6,1.08F,.20F);
    }
    private static void phoenixRequiemWings(double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        double q=phase(p,.03,.94);ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));Vec3 c=new Vec3(0,1.0,0);
        for(int s=-1;s<=1;s+=2){Vec3 root=c.add(f.right().scale(s*.3));Vec3 a=root.add(f.right().scale(s*o*.72*q)).add(f.up().scale(o*.62*q));Vec3 b=root.add(f.right().scale(s*o*q)).add(f.up().scale(o*.08*q));m.triangle(root,a,b,1.15F,.22F);m.line(a,b,1.2F);}
        double rad=Math.max(8,range*.42)*q;m.brokenBand(ArcaneWorldMesh.Basis.ground(),Vec3.ZERO,rad*.82,rad,72,7,1.0F,.18F);
    }

    // 9C charge identities
    private static void meteorCommandArray(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.02,.96);Vec3 sky=c.add(0,30*q,0);
        m.star(g,sky,o*.82*q,o*.34*q,12,r,1.35F);m.brokenBand(g,sky,o*.88*q,o*q,108,7,1.16F,.24F);
        for(int i=0;i<4;i++){double a=r+Math.PI/4+i*Math.PI/2;Vec3 n=sky.add(g.point(a,10));m.orb(n,1.1*q,22,1.15F,.28F);m.line(n,c.add(g.point(a,10)),.78F);}
    }
    private static void powerWordExecution(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(t);Vec3 c=t.add(0,1.2,0);double q=phase(p,.02,.98);
        m.runeGlyph(f,c,o*.34*q,0xDEAD,r,1.5F);m.polygon(f,c,o*.62*q,3,r,1.4F);m.line(c.add(f.right().scale(-o*.42*q)),c.add(f.right().scale(o*.42*q)),1.6F);
        if(p>.72)m.diamond(f,c,o*.18, -r,1.24F,.30F);
    }
    private static void prismaticWallLoom(Vec3 d,Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);Vec3 c=ground(t);double q=phase(p,.02,.96),half=Math.max(12,range*.25)*q;
        for(int layer=0;layer<7;layer++){double z=(layer-3)*.16;Vec3 a=c.add(f.right().scale(-half)).add(d.scale(z));Vec3 b=c.add(f.right().scale(half)).add(d.scale(z));m.line(a,b,1.35F-layer*.08F);for(int k=0;k<=6;k++){Vec3 n=a.add(b.subtract(a).scale(k/6.0));m.line(n,n.add(0,4.8*q,0),.58F);}}
    }
    private static void shapechangeTotem(double o,double r,double p,ArcaneWorldMesh.Builder m){
        double q=phase(p,.02,.96);ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));Vec3 c=new Vec3(0,1,0);
        m.polygon(f,c,o*.45*q,6,r,1.18F);for(int i=0;i<4;i++){double a=r+i*Math.PI/2;Vec3 horn=c.add(f.point(a,o*.38*q));m.shard(horn,f.point(a,1),f,o*.58*q,o*.08*q,1.15F,.22F);}m.brokenBand(ArcaneWorldMesh.Basis.ground(),Vec3.ZERO,o*.74*q,o*.86*q,72,6,1.0F,.18F);
    }
    private static void timeStopClockwork(double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.02,.97),rad=Math.max(20,range*.55)*q;
        m.brokenBand(g,Vec3.ZERO,rad*.91,rad,112,4,1.08F,.18F);for(int ring=1;ring<=3;ring++){double rr=rad*(.25+ring*.18);m.circle(g,Vec3.ZERO,rr,56,ring==3?1.25F:.72F);}
        for(int i=0;i<12;i++){double a=r+i*Math.PI/6;Vec3 a0=g.point(a,rad*.72),a1=g.point(a,rad*.88);m.line(a0,a1,i%3==0?1.4F:.7F);}m.line(Vec3.ZERO,g.point(r,rad*.62),1.55F);m.line(Vec3.ZERO,g.point(r+2.1,rad*.42),1.2F);
    }
    private static void polymorphScaffold(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(t);Vec3 c=t.add(0,1.0,0);double q=phase(p,.02,.96);
        for(int i=0;i<3;i++){int sides=4+i*2;m.polygon(f,c,o*(.34+i*.19)*q,sides,r+i*.31,1.1F);}for(int i=0;i<6;i++){double a=r+i*Math.PI/3;m.line(c.add(f.point(a,o*.34*q)),c.add(f.point(a+.28,o*.72*q)),.84F);}
    }
    private static void weirdMaskCouncil(Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(t);double q=phase(p,.02,.96),rad=Math.max(14,range*.35)*q;
        for(int i=0;i<7;i++){double a=r+i*6.283/7;Vec3 n=c.add(Math.cos(a)*rad*.68,1.1+(i%3)*.55,Math.sin(a)*rad*.68);m.polygon(f,n,o*.18*q,5,a,1.05F);m.line(n,c.add(0,.6,0),.58F);}m.brokenBand(ArcaneWorldMesh.Basis.ground(),c,rad*.86,rad,82,7,.95F,.15F);
    }
    private static void wishRealityKnot(double o,double r,double p,ArcaneWorldMesh.Builder m){
        double q=phase(p,.02,.98);Vec3 c=new Vec3(0,1.0,0);ArcaneWorldMesh.Basis a=ArcaneWorldMesh.Basis.fromNormal(new Vec3(1,1,.2),new Vec3(0,1,0));ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.fromNormal(new Vec3(-.3,1,1),new Vec3(1,0,0));
        m.star(a,c,o*.56*q,o*.22*q,9,r,1.2F);m.star(b,c,o*.47*q,o*.18*q,7,-r*.8,1.05F);m.orb(c,o*.12*q,18,1.2F,.30F);for(int i=0;i<5;i++)m.line(c,c.add(a.point(r+i*1.257,o*.78*q)),.72F);
    }
    private static void gateMonument(Vec3 d,Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);Vec3 c=ground(t);double q=phase(p,.02,.96),w=o*.72*q,h=o*.92*q;
        roomFrame(c.add(0,h*.5,0),f,w,h,o*.18*q,m);m.brokenBand(f,c.add(0,h*.52,0),w*.82,w,96,7,1.15F,.22F);
        Vec3 far=c.add(d.scale(o*.85*q));roomFrame(far.add(0,h*.5,0),f,w*.78,h*.88,o*.14*q,m);
        for(int s=-1;s<=1;s+=2)m.line(c.add(f.right().scale(s*w)).add(0,h*.5,0),far.add(f.right().scale(s*w*.78)).add(0,h*.48,0),1.0F);
    }
    private static void foresightEye(double o,double r,double p,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));Vec3 c=new Vec3(0,1.55,0);double q=phase(p,.02,.98);
        m.arc(f,c,o*.55*q,Math.PI,Math.PI,36,1.24F);m.arc(f,c,o*.55*q,0,Math.PI,36,1.24F);m.orb(c,o*.12*q,16,1.15F,.28F);
        for(int i=1;i<=5;i++){Vec3 tick=c.add(f.right().scale(o*(.22+i*.12)*q));m.line(tick.add(f.up().scale(-.10)),tick.add(f.up().scale(.10)),.72F);}
    }
    private static void worldSunderFault(Vec3 t,double o,double r,double p,double range,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.02,.96),rad=Math.max(12,range*.38)*q;
        Vec3 prev=c.add(g.point(r,rad*.05));for(int i=1;i<=9;i++){double rr=rad*i/9.0;Vec3 cur=c.add(g.point(r+Math.sin(i*2.1)*.16,rr));m.line(prev,cur,i%3==0?1.7F:1.05F);prev=cur;}
        for(int branch=0;branch<6;branch++){double a=r+(branch-2.5)*.24;Vec3 a0=c.add(g.point(a,rad*.25)),a1=c.add(g.point(a+(branch%2==0?.35:-.35),rad*(.55+branch*.05)));m.line(a0,a1,.82F);}
        m.brokenBand(g,c,rad*.88,rad,92,8,1.0F,.16F);
    }

    // Releases. Geometry matches authoritative high-circle footprints where the server has one.
    private static void delayedBlastRelease(Vec3 t,double range,double age,double pf,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);double r=10*Math.max(1,Math.sqrt(range/25.0)),q=ease(age/.45),f=fade(age,.78);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();
        m.orb(c.add(0,1,0),.3+1.2*q,24,1.18F,(float)(.30*f));if(age>.48){double e=phase(age,.48,.82);m.disc(g,c,r*e,72,1.08F,(float)(.22*f));m.brokenBand(g,c,r*.86*e,r*e,84,7,1.18F,(float)(.25*f));}
    }
    private static void etherealRelease(double age,double pf,ArcaneWorldMesh.Builder m){
        double f=fade(age,.82);for(int i=0;i<5;i++){double y=.4+i*.45;ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.fromNormal(new Vec3(Math.sin(age*5+i),1,Math.cos(age*5+i)),new Vec3(1,0,0));m.brokenBand(b,new Vec3(0,y,0),.7+i*.08,.84+i*.08,42,5,.94F,(float)(.16*f));}
    }
    private static void fingerDeathRelease(Vec3 d,Vec3 t,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        Vec3 end=t.lengthSqr()<.01?d.scale(16):t;double q=ease(Math.min(1,motion)),f=fade(age,.72);Vec3 cur=end.scale(q);ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.facing(d);m.beamPrism(Vec3.ZERO,cur,b,cur.length(),.08*pf,1.12F,(float)(.24*f));if(q>.92)m.brokenBand(b,end,.62,.82,42,5,1.15F,(float)(.26*f));
    }
    private static void fireStormRelease(Vec3 t,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double f=fade(age,.86),drop=ease(Math.min(1,motion));
        for(int i=0;i<6;i++){double a=Math.PI*2*i/6;Vec3 impact=c.add(g.point(a,5));Vec3 sky=impact.add(0,19,0),head=sky.add(impact.subtract(sky).scale(drop));m.orb(head,.45*pf,18,1.14F,(float)(.28*f));m.line(sky,head,.72F);if(drop>.94)m.disc(g,impact,3.75*phase(drop,.94,1),40,1.1F,(float)(.22*f));}
    }
    private static void forceCageRelease(Vec3 t,double age,double pf,ArcaneWorldMesh.Builder m){cage(t,1.8+pf*.25,4.1,ease(age/.18),age*2.2,m,true);}
    private static void planeShiftRelease(Vec3 d,Vec3 t,double age,double pf,ArcaneWorldMesh.Builder m){portalRelease(d,t,age,pf,m,5);}
    private static void prismaticSprayRelease(Vec3 d,double range,double age,double pf,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);double q=ease(age/.22),fade=fade(age,.68);for(int i=-3;i<=3;i++){Vec3 dir=d.add(f.right().scale(i*.11)).add(f.up().scale((i%2)*.05)).normalize();m.beamPrism(Vec3.ZERO,dir,f,range*q,.05*pf,1.08F,(float)(.20*fade));}
    }
    private static void reverseGravityRelease(Vec3 t,double range,double age,double pf,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);double rad=Math.max(10,range*.32),q=ease(age/.24),f=fade(age,.84);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();m.brokenBand(g,c,rad*.88*q,rad*q,80,7,1.0F,(float)(.18*f));for(int i=0;i<10;i++){double a=i*6.283/10;Vec3 b=c.add(g.point(a,rad*.68));m.line(b,b.add(0,5*q,0),i%2==0?1.0F:.55F);}}
    private static void simulacrumRelease(double age,double pf,ArcaneWorldMesh.Builder m){double q=ease(age/.24),f=fade(age,.84);ArcaneWorldMesh.Basis fce=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));for(int s=-1;s<=1;s+=2){Vec3 c=new Vec3(s*.85*q,1,0);m.polygonPlate(fce,c,.52*q,6,age*s*2,1.0F,(float)(.18*f));}m.line(new Vec3(-.85*q,1,0),new Vec3(.85*q,1,0),.76F);}
    private static void teleportRelease(Vec3 d,Vec3 t,double age,double pf,ArcaneWorldMesh.Builder m){portalRelease(d,t,age,pf,m,7);}
    private static void voidLanceRelease(Vec3 d,double range,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        double length=Math.max(8,range),q=ease(Math.min(1,motion)),f=fade(age,.76);ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.facing(d);Vec3 head=d.scale(length*q);m.beamPrism(Vec3.ZERO,d,b,length*q,.10*pf,1.08F,(float)(.23*f));m.shard(head,d,b,.9*pf,.14*pf,1.18F,(float)(.28*f));
    }
    private static void winterDomainRelease(double range,double age,double pf,ArcaneWorldMesh.Builder m){
        double rad=Math.max(6,range*.45),q=ease(age/.26),f=fade(age,.88);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();m.disc(g,Vec3.ZERO,rad*q,76,.88F,(float)(.12*f));m.star(g,Vec3.ZERO,rad*q,rad*.58*q,12,age*1.2,1.12F);for(int i=0;i<12;i++){double a=i*6.283/12;m.shard(g.point(a,rad*.82*q),new Vec3(0,1,0),ArcaneWorldMesh.Basis.facing(new Vec3(0,1,0)),1.3*q,.13*q,1.0F,(float)(.20*f));}}
    private static void antimagicRelease(double range,double age,double pf,ArcaneWorldMesh.Builder m){
        double rad=Math.max(9,range*.50),q=ease(age/.22),f=fade(age,.86);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();m.disc(g,Vec3.ZERO,rad*q,78,.58F,(float)(.08*f));m.brokenBand(g,Vec3.ZERO,rad*.9*q,rad*q,84,3,.96F,(float)(.14*f));for(int i=0;i<8;i++){double a=i*.785+age;m.line(g.point(a,rad*.28*q),g.point(a+.34,rad*.82*q),1.0F);}}
    private static void cloneRelease(double age,double pf,ArcaneWorldMesh.Builder m){
        double q=ease(age/.24),f=fade(age,.88);Vec3 h=new Vec3(0,1.1,0);ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));m.orb(h,.34*q,20,1.14F,(float)(.26*f));for(int s=-1;s<=1;s+=2){Vec3 c=h.add(s*1.1,0,0);m.orb(c,.28*q,18,.95F,(float)(.18*f));m.line(h,c,1.05F);} }
    private static void weatherRelease(double range,double age,double pf,ArcaneWorldMesh.Builder m){
        double rad=Math.max(18,range*.55),q=ease(age/.28),f=fade(age,.90);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();for(int layer=0;layer<5;layer++)m.brokenBand(g,new Vec3(0,8+layer*2.6,0),rad*(.60+layer*.05)*q,rad*(.68+layer*.05)*q,72,5+layer%2,1.0F,(float)(.16*f));for(int i=0;i<8;i++){double a=age*2+i*.785;Vec3 n=g.point(a,rad*.65*q).add(0,16,0);m.line(n,n.add(g.point(a+.4,rad*.22)).add(0,-10*q,0),i%2==0?1.15F:.65F);}}
    private static void demiplaneRelease(Vec3 d,double age,double pf,ArcaneWorldMesh.Builder m){portalRelease(d,d.scale(4),age,pf,m,8);}
    private static void dominateMonsterRelease(Vec3 t,double age,double pf,ArcaneWorldMesh.Builder m){double q=ease(age/.18),f=fade(age,.82);ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.facing(t);Vec3 c=t.add(0,1.3,0);m.star(b,c,1.7*q,.62*q,8,age,1.3F);for(int i=0;i<6;i++){Vec3 n=c.add(b.point(i*1.047,2.1*q));m.line(n,c,1.0F);m.line(n,n.add(0,-1.8*q,0),.65F);}m.orb(c,.22*q,14,1.1F,(float)(.25*f));}
    private static void earthquakeRelease(Vec3 t,double range,double age,double pf,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);double rad=Math.max(8,range*.65),q=ease(age/.30),f=fade(age,.90);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();for(int i=0;i<14;i++){double a=i*.449;Vec3 a0=c.add(g.point(a,rad*.06)),a1=c.add(g.point(a+.10*Math.sin(i*2),rad*.56*q)),a2=c.add(g.point(a+.22*Math.cos(i),rad*q));m.line(a0,a1,1.2F);m.line(a1,a2,.82F);}m.brokenBand(g,c,rad*.9*q,rad*q,88,8,.92F,(float)(.15*f));}
    private static void feeblemindRelease(Vec3 t,double age,double pf,ArcaneWorldMesh.Builder m){double q=ease(age/.18),f=fade(age,.75);ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.facing(t);Vec3 c=t.add(0,1.45,0);m.runeChords(b,c,1.25*q,9,4,age*2,1.18F);if(age>.26)for(int i=0;i<7;i++){Vec3 n=c.add(b.point(i*.898,1.25));m.line(n,n.add(b.point(i*.898+.44,.8*phase(age,.26,.62))),.78F);}m.orb(c,.18*q,14,1.1F,(float)(.22*f));}
    private static void incendiaryRelease(Vec3 t,double range,double age,double pf,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);double rad=Math.max(10,range*.30),q=ease(age/.25),f=fade(age,.90);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();m.disc(g,c,rad*q,78,.78F,(float)(.10*f));for(int layer=0;layer<6;layer++)m.brokenBand(g,c.add(0,.5+layer*.9,0),rad*(.92-layer*.05)*q,rad*(1-layer*.05)*q,58,5+layer%3,1.0F,(float)(.19*f));for(int i=0;i<9;i++){double a=age*3+i*.698;Vec3 n=c.add(g.point(a,rad*.68*q)).add(0,.8+(i%4)*.7,0);m.orb(n,.25+.04*(i%3),14,1.08F,(float)(.22*f));}}
    private static void mazeRelease(Vec3 t,double age,double pf,ArcaneWorldMesh.Builder m){double q=ease(age/.22),f=fade(age,.84);ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.facing(t);for(int i=0;i<8;i++){double s=(.55+i*.22)*q;m.polygon(b,t.add(0,(i%3)*.12,0),s,4,age*(i%2==0?1:-1)+i*.27,i%2==0?1.18F:.64F);}m.orb(t.add(0,1,0),.2*q,14,1.0F,(float)(.18*f));}
    private static void sunburstRelease(Vec3 t,double range,double age,double pf,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);double rad=14*Math.max(1,Math.sqrt(range/25.0)),q=ease(age/.20),f=fade(age,.78);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 sky=c.add(0,12,0);m.orb(sky,1.0*q,24,1.2F,(float)(.32*f));m.line(sky,c,1.1F);if(age>.24){double e=phase(age,.24,.66);m.disc(g,c,rad*e,88,1.18F,(float)(.25*f));m.star(g,c,rad*e,rad*.62*e,16,age,1.24F);}}
    private static void astralPrisonRelease(Vec3 t,double age,double pf,ArcaneWorldMesh.Builder m){double q=ease(age/.19);cage(t,2.2+pf*.25,4.8,q,age*1.4,m,true);ArcaneWorldMesh.Basis a=ArcaneWorldMesh.Basis.fromNormal(new Vec3(1,.7,1),new Vec3(0,1,0));m.brokenBand(a,t.add(0,1.5,0),1.8*q,2.1*q,56,6,1.05F,.19F);}
    private static void phoenixRelease(double range,double age,double pf,ArcaneWorldMesh.Builder m){
        double rad=Math.max(8,range*.42),q=ease(age/.22),f=fade(age,.88);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();m.disc(g,Vec3.ZERO,rad*q,82,1.0F,(float)(.14*f));ArcaneWorldMesh.Basis face=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));Vec3 c=new Vec3(0,1.2,0);for(int s=-1;s<=1;s+=2){Vec3 a=c.add(face.right().scale(s*3.2*q)).add(face.up().scale(2.4*q)),b=c.add(face.right().scale(s*4.8*q));m.triangle(c,a,b,1.16F,(float)(.23*f));}m.orb(c,.5*q,20,1.18F,(float)(.30*f));}
    private static void meteorSwarmRelease(Vec3 t,double age,double motion,double pf,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double drop=ease(Math.min(1,motion)),f=fade(age,.92);
        for(int i=0;i<4;i++){double a=Math.PI/4+i*Math.PI/2;Vec3 impact=c.add(g.point(a,10)),sky=impact.add(0,28,0),head=sky.add(impact.subtract(sky).scale(drop));m.orb(head,.95*pf,26,1.18F,(float)(.34*f));m.helix(head,sky.subtract(impact),ArcaneWorldMesh.Basis.facing(sky.subtract(impact)),2.5,.32,2,24,.72F,true);if(drop>.92){double e=phase(drop,.92,1);m.disc(g,impact,11*e,80,1.15F,(float)(.25*f));m.brokenBand(g,impact,9.4*e,11*e,72,7,1.2F,(float)(.24*f));}}
    }
    private static void powerWordRelease(Vec3 t,double age,double pf,ArcaneWorldMesh.Builder m){
        double q=ease(age/.13),f=fade(age,.62);ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.facing(t);Vec3 c=t.add(0,1.2,0);m.runeGlyph(b,c,.72*q,0xDEAD,0,1.65F);m.polygon(b,c,1.15*q,3,age*.3,1.45F);if(age>.18)m.line(c.add(b.right().scale(-1.05)),c.add(b.right().scale(1.05)),1.8F);m.orb(c,.16*q,14,1.2F,(float)(.30*f));}
    private static void prismaticWallRelease(Vec3 d,Vec3 t,double range,double age,double pf,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);Vec3 c=ground(t);double half=Math.max(12,range*.25),q=ease(age/.20),fade=fade(age,.90);
        for(int layer=0;layer<7;layer++){double z=(layer-3)*.18;Vec3 left=c.add(f.right().scale(-half*q)).add(d.scale(z)),right=c.add(f.right().scale(half*q)).add(d.scale(z));for(int k=0;k<12;k++){double a=k/12.0,b=(k+1)/12.0;Vec3 p0=left.add(right.subtract(left).scale(a)),p1=left.add(right.subtract(left).scale(b));m.face(p0,p1,p1.add(0,5*q,0),p0.add(0,5*q,0),1.0F-layer*.035F,(float)(.12*fade));}m.line(left,right,1.1F);}}
    private static void shapechangeRelease(double age,double pf,ArcaneWorldMesh.Builder m){double q=ease(age/.25),f=fade(age,.88);ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));Vec3 c=new Vec3(0,1.0,0);m.polygonPlate(b,c,.95*q,6,age,1.0F,(float)(.17*f));for(int i=0;i<6;i++){Vec3 n=c.add(b.point(i*1.047,.72*q));m.shard(n,b.point(i*1.047,1),b,1.0*q,.12*q,1.12F,(float)(.22*f));}}
    private static void timeStopRelease(double range,double age,double pf,ArcaneWorldMesh.Builder m){
        double rad=Math.max(20,range*.55),q=ease(age/.18),f=fade(age,.92);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();m.disc(g,Vec3.ZERO,rad*q,96,.72F,(float)(.08*f));for(int ring=1;ring<=4;ring++)m.circle(g,Vec3.ZERO,rad*(.18+ring*.19)*q,64,ring==4?1.4F:.7F);for(int i=0;i<12;i++){double a=i*.524;m.line(g.point(a,rad*.72*q),g.point(a,rad*.91*q),i%3==0?1.45F:.68F);}m.line(Vec3.ZERO,g.point(.05,rad*.61*q),1.7F);m.line(Vec3.ZERO,g.point(2.05,rad*.42*q),1.3F);}
    private static void polymorphRelease(Vec3 t,double age,double pf,ArcaneWorldMesh.Builder m){double q=ease(age/.22),f=fade(age,.82);ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.facing(t);Vec3 c=t.add(0,1.0,0);for(int i=0;i<4;i++){int sides=4+i*2;m.polygon(b,c,(.6+i*.34)*q,sides,age*(i%2==0?1:-1)+i*.22,1.1F);}m.orb(c,.28*q,18,1.08F,(float)(.22*f));}
    private static void weirdRelease(Vec3 t,double range,double age,double pf,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);double rad=Math.max(14,range*.35),q=ease(age/.25),f=fade(age,.88);ArcaneWorldMesh.Basis face=ArcaneWorldMesh.Basis.facing(t);for(int i=0;i<9;i++){double a=age*1.4+i*6.283/9;Vec3 n=c.add(Math.cos(a)*rad*.68*q,1+(i%3)*.55,Math.sin(a)*rad*.68*q);m.polygonPlate(face,n,.36*q,5,a,1.0F,(float)(.15*f));m.line(n,c.add(0,.5,0),.55F);}m.brokenBand(ArcaneWorldMesh.Basis.ground(),c,rad*.88*q,rad*q,84,7,.94F,(float)(.15*f));}
    private static void wishRelease(double age,double pf,ArcaneWorldMesh.Builder m){
        double q=ease(age/.25),f=fade(age,.92);Vec3 c=new Vec3(0,1,0);ArcaneWorldMesh.Basis a=ArcaneWorldMesh.Basis.fromNormal(new Vec3(1,1,.2),new Vec3(0,1,0)),b=ArcaneWorldMesh.Basis.fromNormal(new Vec3(-.3,1,1),new Vec3(1,0,0));m.star(a,c,1.6*q,.62*q,9,age,1.24F);m.star(b,c,1.35*q,.48*q,7,-age*.8,1.08F);m.orb(c,.28*q,20,1.22F,(float)(.32*f));for(int i=0;i<6;i++){double aa=age+i*1.047;m.line(c,c.add(a.point(aa,2.1*q)),.72F);}}
    private static void gateRelease(Vec3 d,Vec3 t,double age,double pf,ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(d);Vec3 c=ground(t);double q=ease(age/.25),fade=fade(age,.92),w=7.5*q,h=9*q;roomFrame(c.add(0,h*.5,0),f,w,h,1.3,m);Vec3 far=c.add(d.scale(8*q));roomFrame(far.add(0,h*.5,0),f,w*.78,h*.88,1.0,m);for(int s=-1;s<=1;s+=2)m.line(c.add(f.right().scale(s*w)).add(0,h*.5,0),far.add(f.right().scale(s*w*.78)).add(0,h*.48,0),1.15F);m.face(c.add(f.right().scale(-w)),c.add(f.right().scale(w)),c.add(f.right().scale(w)).add(0,h,0),c.add(f.right().scale(-w)).add(0,h,0),.82F,(float)(.08*fade));}
    private static void foresightRelease(double age,double pf,ArcaneWorldMesh.Builder m){double q=ease(age/.18),f=fade(age,.90);ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));Vec3 c=new Vec3(0,1.55,0);m.arc(b,c,.82*q,Math.PI,Math.PI,36,1.28F);m.arc(b,c,.82*q,0,Math.PI,36,1.28F);m.orb(c,.17*q,16,1.18F,(float)(.28*f));for(int i=1;i<=7;i++){Vec3 n=c.add(b.right().scale((.42+i*.16)*q));m.line(n.add(b.up().scale(-.1)),n.add(b.up().scale(.1)),.7F);}}
    private static void worldSunderRelease(Vec3 t,double range,double age,double pf,ArcaneWorldMesh.Builder m){
        Vec3 c=ground(t);double rad=Math.max(12,range*.38),q=ease(age/.30),f=fade(age,.90);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 prev=c;for(int i=1;i<=12;i++){double rr=rad*i/12.0;Vec3 cur=c.add(g.point(Math.sin(i*2.1)*.18,rr*q));m.line(prev,cur,i%3==0?1.8F:1.12F);prev=cur;}for(int branch=0;branch<9;branch++){double a=(branch-4)*.19;Vec3 a0=c.add(g.point(a,rad*.26*q)),a1=c.add(g.point(a+(branch%2==0?.38:-.38),rad*(.52+(branch%4)*.08)*q));m.line(a0,a1,.82F);}m.brokenBand(g,c,rad*.88*q,rad*q,96,8,1.0F,(float)(.17*f));}

    // Shared structural primitives. They do not choose a spell identity; wrappers above do.
    private static void cage(Vec3 target,double radius,double height,double q,double rot,ArcaneWorldMesh.Builder m,boolean roof){
        Vec3 c=ground(target);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();int n=8;
        for(int i=0;i<n;i++){double a=rot+i*6.283/n;Vec3 b=c.add(g.point(a,radius*q));m.line(b,b.add(0,height*q,0),i%2==0?1.25F:.75F);if(i>0){Vec3 prev=c.add(g.point(rot+(i-1)*6.283/n,radius*q));m.line(prev.add(0,height*.45*q,0),b.add(0,height*.45*q,0),.65F);m.line(prev.add(0,height*.82*q,0),b.add(0,height*.82*q,0),.65F);}}
        if(roof)m.polygon(g,c.add(0,height*q,0),radius*q,n,rot,1.0F);
    }
    private static void roomFrame(Vec3 c,ArcaneWorldMesh.Basis f,double halfWidth,double height,double depth,ArcaneWorldMesh.Builder m){
        Vec3 r=f.right().scale(halfWidth),u=f.up().scale(height*.5),n=f.normal().scale(depth*.5);Vec3[] a={c.subtract(r).subtract(u).subtract(n),c.add(r).subtract(u).subtract(n),c.add(r).add(u).subtract(n),c.subtract(r).add(u).subtract(n)};Vec3[] b={c.subtract(r).subtract(u).add(n),c.add(r).subtract(u).add(n),c.add(r).add(u).add(n),c.subtract(r).add(u).add(n)};for(int i=0;i<4;i++){m.line(a[i],a[(i+1)%4],1.05F);m.line(b[i],b[(i+1)%4],.75F);m.line(a[i],b[i],.65F);}
    }
    private static void portalAddress(Vec3 t,double o,double r,double p,ArcaneWorldMesh.Builder m,int nodes){
        Vec3 c=ground(t);ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.04,.94);m.band(g,c,o*.62*q,o*.78*q,72,1.08F,.22F);m.runeRing(g,c,o*.56*q,18,o*.03,0x71E,r,.74F);for(int i=0;i<nodes;i++){double a=r+i*6.283/nodes;Vec3 n=c.add(g.point(a,o*.76*q));m.line(n,n.add(0,2.2*q,0),i%2==0?1.0F:.6F);}}
    private static void portalRelease(Vec3 d,Vec3 t,double age,double pf,ArcaneWorldMesh.Builder m,int ribs){
        double q=ease(age/.24),f=fade(age,.86);ArcaneWorldMesh.Basis b=ArcaneWorldMesh.Basis.facing(d);Vec3 near=d.scale(1.2),far=t.lengthSqr()<.01?d.scale(6):t;m.brokenBand(b,near,1.0*q,1.3*q,56,6,1.08F,(float)(.22*f));m.brokenBand(b,far,1.1*q,1.45*q,62,7,1.02F,(float)(.20*f));for(int i=0;i<ribs;i++){double a=i*6.283/ribs;Vec3 a0=near.add(b.point(a,1.18*q)),a1=far.add(b.point(a,1.28*q));m.line(a0,a1,i%2==0?1.0F:.58F);}}
    private static Vec3 ground(Vec3 v){return new Vec3(v.x,Math.min(.15,v.y),v.z);}
    private static double phase(double p,double a,double b){if(p<=a)return 0;if(p>=b)return 1;double t=(p-a)/(b-a);return t*t*(3-2*t);}
    private static double ease(double t){t=Math.max(0,Math.min(1,t));return 1-Math.pow(1-t,3);}
    private static double fade(double age,double start){return age<=start?1:Math.max(0,1-(age-start)/Math.max(.001,1-start));}
}
