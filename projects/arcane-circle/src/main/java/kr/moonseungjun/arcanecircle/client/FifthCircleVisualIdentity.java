package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellMetrics;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
import net.minecraft.world.phys.Vec3;

/** Phase 2B authored presentation for all 5C normal and fusion formulae. */
final class FifthCircleVisualIdentity {
    private FifthCircleVisualIdentity() {}

    static boolean owns(SpellDefinition spell) { return spell != null && spell.circle() == 5; }

    static void appendCharge(SpellDefinition spell, SpellPresentationProfile.Profile profile,
                             double outer, double rotation, double progress, Vec3 direction,
                             Vec3 targetOffset, double effectiveRange, ArcaneWorldMesh.Builder mesh) {
        switch (spell.id()) {
            case "cloudkill" -> cloudkillCollectors(targetOffset, outer, rotation, progress, effectiveRange, mesh);
            case "wall_of_force" -> forceWallAnchors(direction, targetOffset, rotation, progress, effectiveRange, mesh);
            case "hold_monster" -> holdMonsterBraces(targetOffset, outer, rotation, progress, mesh);
            case "passwall" -> passwallFrames(direction, targetOffset, outer, progress, mesh);
            case "insect_plague" -> insectPlagueBeacons(targetOffset, outer, rotation, progress, effectiveRange, mesh);
            case "telekinesis" -> telekinesisVectorRig(direction, targetOffset, outer, rotation, progress, mesh);
            case "cone_of_cold" -> coneColdAperture(direction, outer, rotation, progress, mesh);
            case "flame_strike" -> flameStrikeJudgement(targetOffset, outer, rotation, progress, mesh);
            case "dominate_person" -> dominateCrown(targetOffset, outer, rotation, progress, mesh);
            case "mass_cure_wounds" -> massCureLattice(outer, rotation, progress, effectiveRange, mesh);
            case "chain_lightning" -> chainLightningRouter(direction, outer, rotation, progress, mesh);
            case "arcane_hand" -> arcaneHandAssembly(direction, targetOffset, outer, progress, mesh);
            case "teleportation_circle" -> teleportCircleAddress(targetOffset, outer, rotation, progress, effectiveRange, mesh);
            default -> { }
        }
    }

    static void appendRelease(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                              double effectiveRange, double age, double motionProgress,
                              double powerFactor, ArcaneWorldMesh.Builder mesh) {
        switch (spell.id()) {
            case "cloudkill" -> cloudkillVolume(targetOffset, effectiveRange, age, powerFactor, mesh);
            case "wall_of_force" -> forceWallInstallation(direction, targetOffset, effectiveRange, age, powerFactor, mesh);
            case "hold_monster" -> holdMonsterLock(targetOffset, age, powerFactor, mesh);
            case "passwall" -> passwallTunnel(direction, targetOffset, age, powerFactor, mesh);
            case "insect_plague" -> insectSwarmVolume(targetOffset, effectiveRange, age, powerFactor, mesh);
            case "telekinesis" -> telekinesisVectors(direction, targetOffset, age, motionProgress, powerFactor, mesh);
            case "cone_of_cold" -> coneColdFracture(direction, effectiveRange, age, powerFactor, mesh);
            case "flame_strike" -> flameStrikePillar(targetOffset, effectiveRange, age, powerFactor, mesh);
            case "dominate_person" -> dominateYoke(targetOffset, age, powerFactor, mesh);
            case "mass_cure_wounds" -> massCureBloom(effectiveRange, age, powerFactor, mesh);
            case "chain_lightning" -> chainLightningFork(direction, targetOffset, effectiveRange, age, powerFactor, mesh);
            case "arcane_hand" -> arcaneHandGrip(direction, targetOffset, age, powerFactor, mesh);
            case "teleportation_circle" -> teleportCircleTransit(direction, targetOffset, effectiveRange, age, powerFactor, mesh);
            default -> { }
        }
    }

    private static void cloudkillCollectors(Vec3 target, double outer, double rot, double p, double range, ArcaneWorldMesh.Builder m) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground(); Vec3 c=groundTarget(target);
        double r=SpellMetrics.effectRadius("cloudkill",range,5)*phase(p,.12,.88);
        if(r<.1)return; m.brokenBand(g,c,r*.82,r,72,7,1.05F,.24F);
        for(int i=0;i<5;i++){double a=rot+i*Math.PI*2/5;Vec3 n=c.add(g.point(a,r*.66));double q=phase(p,.22+i*.06,.92);m.band(g,n,.20*q,.34*q,24,1.06F,.22F);m.line(n,n.add(0,1.0+q*2.4,0),.78F);}
        if(p>.62)m.brokenBand(g,c.add(0,3.4*phase(p,.62,1),0),r*.52,r*.68,58,6,.88F,.18F);
    }
    private static void forceWallAnchors(Vec3 dir, Vec3 target, double rot, double p, double range, ArcaneWorldMesh.Builder m){
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(dir);Vec3 c=groundTarget(target);double w=SpellMetrics.wallWidth("wall_of_force",range,5);double half=w*.5;int count=7;
        for(int i=0;i<count;i++){double q=phase(p,.08+i*.07,.72+i*.03);if(q<=0)continue;double x=-half+w*i/(count-1.0);Vec3 b=c.add(f.right().scale(x));m.diamond(f,b.add(0,.45,0),.28+.18*q,rot+i*.31,1.16F,.26F);m.line(b,b.add(0,1.0+2.7*q,0),1.0F);if(i>0)m.line(c.add(f.right().scale(-half+w*(i-1)/(count-1.0))).add(0,.18,0),b.add(0,.18,0),.72F);}
    }
    private static void holdMonsterBraces(Vec3 target,double outer,double rot,double p,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=target;double q=phase(p,.08,.9),r=outer*.72; m.polygon(g,c,r*q,8,rot,.96F);for(int i=0;i<4;i++){double a=rot+i*Math.PI/2;Vec3 b=c.add(g.point(a,r*q));m.line(b.add(0,-.8,0),b.add(0,2.4*q,0),1.18F);m.line(b.add(0,1.2*q,0),c.add(0,1.2*q,0),.72F);}if(p>.7)m.brokenBand(g,c.add(0,2.4*q,0),r*.62,r*.82,42,5,1.02F,.22F);}
    private static void passwallFrames(Vec3 dir,Vec3 target,double outer,double p,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(dir);double q=phase(p,.05,.88);Vec3 near=dir.scale(Math.min(2.1,target.length()*.22));Vec3 far=target;rectFrame(m,f,near,1.0*q,1.7*q,1.14F);if(p>.32)rectFrame(m,f,far,1.15*q,1.85*q,1.02F);if(p>.48){for(int i=-1;i<=1;i++){Vec3 off=f.right().scale(i*.72*q);m.line(near.add(off).add(0,1.5*q,0),far.add(off).add(0,1.5*q,0),i==0?1.08F:.64F);}}}
    private static void insectPlagueBeacons(Vec3 target,double outer,double rot,double p,double range,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=groundTarget(target);double r=SpellMetrics.effectRadius("insect_plague",range,5);for(int i=0;i<8;i++){double q=phase(p,.08+i*.055,.72+i*.03);if(q<=0)continue;double a=rot+i*Math.PI*2/8;Vec3 n=c.add(g.point(a,r*.66));m.brokenBand(g,n,.16*q,.30*q,20,4,.92F,.24F);m.line(n,n.add(0,.5+1.8*q,0),.72F);}if(p>.58)m.runeChords(g,c,r*.70,8,3,rot*.35,.72F);}
    private static void telekinesisVectorRig(Vec3 dir,Vec3 target,double outer,double rot,double p,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(dir);Vec3 c=target;double q=phase(p,.08,.9);m.brokenBand(f,c,outer*.44*q,outer*.62*q,48,6,1.04F,.22F);for(int i=0;i<5;i++){double a=rot+i*Math.PI*2/5;Vec3 h=c.add(f.point(a,outer*.76*q));m.line(h,c, i==0?1.24F:.78F);m.diamond(f,h,outer*.12*q,a,1.12F,.24F);}m.line(Vec3.ZERO,c.scale(q),1.02F);}
    private static void coneColdAperture(Vec3 dir,double outer,double rot,double p,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(dir);double q=phase(p,.06,.92);m.polygon(f,Vec3.ZERO,outer*.72*q,6,rot,1.18F);m.polygon(f,Vec3.ZERO,outer*.46*q,6,-rot*.6,.84F);for(int i=0;i<6;i++){double a=rot+i*Math.PI/3;Vec3 tip=f.point(a,outer*.82*q);m.shard(tip,tip,f,outer*.42*q,outer*.055,1.08F,.28F);}if(p>.66)m.brokenBand(f,Vec3.ZERO,outer*.86,outer,58,6,1.08F,.26F);}
    private static void flameStrikeJudgement(Vec3 target,double outer,double rot,double p,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=groundTarget(target);double q=phase(p,.05,.9);m.star(g,c,outer*.74*q,outer*.28*q,7,rot,1.16F);m.runeRing(g,c,outer*.60*q,14,outer*.035,0x51A7,rot,.70F);if(p>.46){Vec3 sky=c.add(0,Math.max(6,outer*2.7)*phase(p,.46,1),0);m.brokenBand(g,sky,outer*.58*q,outer*.82*q,62,6,1.04F,.24F);m.line(c,sky,.76F);}}
    private static void dominateCrown(Vec3 target,double outer,double rot,double p,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 head=target.add(0,1.75,0);double q=phase(p,.08,.9),r=outer*.54*q;m.brokenBand(g,head,r*.78,r,44,5,1.06F,.24F);for(int i=0;i<5;i++){double a=rot+i*Math.PI*2/5;Vec3 b=head.add(g.point(a,r));Vec3 tip=b.add(0,.45*q+(i%2)*.2*q,0);m.line(b,tip,1.12F);m.line(tip,head.add(0,.15,0),.68F);}if(p>.62)m.line(head,target.add(0,.7,0),1.0F);}
    private static void massCureLattice(double outer,double rot,double p,double range,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double q=phase(p,.05,.92),r=Math.min(range*.38,outer*2.7)*q;m.star(g,Vec3.ZERO,r,r*.42,6,rot,1.0F);for(int i=0;i<6;i++){Vec3 n=g.point(rot+i*Math.PI/3,r*.78);m.band(g,n,outer*.12*q,outer*.20*q,20,1.02F,.20F);}m.brokenBand(g,Vec3.ZERO,r*.88,r,64,7,1.02F,.20F);}
    private static void chainLightningRouter(Vec3 dir,double outer,double rot,double p,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(dir);double q=phase(p,.04,.94);Vec3 hub=dir.scale(outer*.38);m.diamond(f,hub,outer*.26*q,rot,1.18F,.28F);for(int i=0;i<4;i++){double a=rot+i*Math.PI/2;Vec3 gate=hub.add(f.point(a,outer*.62*q));m.brokenBand(f,gate,outer*.10*q,outer*.18*q,20,4,1.06F,.22F);m.line(hub,gate,i==0?1.28F:.76F);}if(p>.68)m.shard(hub,dir,f,outer*.75,outer*.10,1.18F,.30F);}
    private static void arcaneHandAssembly(Vec3 dir,Vec3 target,double outer,double p,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(dir);Vec3 palm=target;double q=phase(p,.05,.92),s=outer*.42*q;m.polygonPlate(f,palm,s,5,0,1.04F,.18F);for(int i=-2;i<=2;i++){Vec3 base=palm.add(f.right().scale(i*s*.34)).add(f.up().scale(s*.22));Vec3 tip=base.add(f.up().scale(s*(.85-Math.abs(i)*.08))).add(dir.scale(s*.18));m.line(base,tip,1.16F);m.orb(tip,s*.10,10,.92F,.18F);}m.line(Vec3.ZERO,palm.scale(q),.82F);}
    private static void teleportCircleAddress(Vec3 target,double outer,double rot,double p,double range,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=groundTarget(target);double q=phase(p,.04,.94),r=Math.min(Math.max(outer,2.2),Math.max(outer,range*.15))*q;m.band(g,c,r*.82,r,72,1.08F,.24F);m.runeRing(g,c,r*.68,18,r*.035,0x7E1E,rot,.76F);for(int i=0;i<4;i++){double a=rot+i*Math.PI/2;Vec3 n=c.add(g.point(a,r*.92));m.line(n,n.add(0,1.8*q,0),1.02F);} }

    private static void cloudkillVolume(Vec3 target,double range,double age,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=groundTarget(target);double p=ease(age/.25),fade=fade(age,.78),r=SpellMetrics.effectRadius("cloudkill",range,5)*p; m.disc(g,c,r,56,.62F,(float)(.08*fade));for(int layer=0;layer<6;layer++){double y=.3+layer*.72;double rr=r*(.94-layer*.055);m.brokenBand(g,c.add(0,y,0),rr*.78,rr,54,5+layer%2,.94F,(float)(.20*fade));}for(int i=0;i<9;i++){double a=age*3.2+i*2.399;Vec3 mote=c.add(g.point(a,r*(.24+(i%4)*.17))).add(0,.5+(i%3)*.8,0);m.orb(mote,.18+.05*(i%2),12,.82F,(float)(.16*fade));}}
    private static void forceWallInstallation(Vec3 dir,Vec3 target,double range,double age,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(dir);Vec3 c=groundTarget(target);double p=ease(age/.27),fade=fade(age,.82),w=SpellMetrics.wallWidth("wall_of_force",range,5)*p,h=(3.8+pf*.8)*p;int panels=Math.max(6,Math.min(18,(int)Math.ceil(w/2.0)));for(int i=0;i<panels;i++){double a=(i/(double)panels-.5)*w,b=((i+1)/(double)panels-.5)*w;Vec3 p0=c.add(f.right().scale(a)),p1=c.add(f.right().scale(b));m.face(p0,p1,p1.add(0,h,0),p0.add(0,h,0),i%2==0?1.08F:.86F,(float)(.22*fade));m.line(p0,p0.add(0,h,0),1.02F);}m.line(c.add(f.right().scale(-w*.5)),c.add(f.right().scale(w*.5)),1.22F);}
    private static void holdMonsterLock(Vec3 target,double age,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double p=ease(age/.22),fade=fade(age,.82),r=(1.2+.28*pf)*p,h=(2.5+.5*pf)*p;m.brokenBand(g,target,r*.80,r,48,5,1.08F,(float)(.26*fade));for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 b=target.add(g.point(a,r));m.line(b.add(0,-.5,0),b.add(0,h,0),i%2==0?1.20F:.78F);}m.polygonPlate(g,target.add(0,h,0),r,8,age,.84F,(float)(.16*fade));}
    private static void passwallTunnel(Vec3 dir,Vec3 target,double age,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(dir);double p=ease(age/.24),fade=fade(age,.84);Vec3 near=dir.scale(Math.min(2.0,target.length()*.22)),far=target;rectFrame(m,f,near,1.05*p,1.85*p,1.2F);rectFrame(m,f,far,1.18*p,1.95*p,1.02F);for(int i=-2;i<=2;i++){double x=i*.48*p;Vec3 a=near.add(f.right().scale(x)).add(0,1.55*p,0),b=far.add(f.right().scale(x)).add(0,1.55*p,0);m.line(a,b,i==0?1.12F:.62F);}for(int s=1;s<4;s++){Vec3 c=near.scale(1-s/4.0).add(far.scale(s/4.0));rectFrame(m,f,c,1.0*p,1.72*p,(float)(.72*fade));}}
    private static void insectSwarmVolume(Vec3 target,double range,double age,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=groundTarget(target);double p=ease(age/.28),fade=fade(age,.80),r=SpellMetrics.effectRadius("insect_plague",range,5)*p;m.brokenBand(g,c,r*.88,r,64,7,1.0F,(float)(.20*fade));for(int i=0;i<18;i++){double a=age*(4.2+(i%3)*.4)+i*2.399;double rr=r*(.18+(i%6)*.13);Vec3 n=c.add(g.point(a,rr)).add(0,.3+(i%5)*.58,0);m.orb(n,.10+.025*(i%3),9,.88F,(float)(.22*fade));if(i%3==0)m.line(n,n.add(g.point(a+1.4,.35)),.56F);}}
    private static void telekinesisVectors(Vec3 dir,Vec3 target,double age,double motion,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(dir);double p=ease(age/.22),fade=fade(age,.82);Vec3 c=target.scale(.72+.28*motion);m.brokenBand(f,c,.58*pf*p,.82*pf*p,42,5,1.08F,(float)(.24*fade));for(int i=0;i<5;i++){double a=age*2.8+i*Math.PI*2/5;Vec3 h=c.add(f.point(a,1.05*pf*p));m.line(h,c,1.0F);m.shard(h,c.subtract(h),f,.62,.10,1.0F,(float)(.22*fade));}m.line(Vec3.ZERO,c,1.18F);}
    private static void coneColdFracture(Vec3 dir,double range,double age,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(dir);double p=ease(age/.31),fade=fade(age,.76),len=SpellMetrics.waveLength(range)*p,end=SpellMetrics.waveEndRadius("cone_of_cold",range,5)*p*pf;m.cone(Vec3.ZERO,dir,f,len,end,14,5,.72F);for(int s=1;s<=5;s++){double t=s/5.0;Vec3 c=dir.scale(len*t);double r=end*t;m.polygon(f,c,r,6,age*1.2+s*.3,.84F);for(int i=0;i<3;i++){Vec3 n=c.add(f.point(i*2.094+age,r*.72));m.shard(n,dir,f,.75+.32*t,.08+.04*t,1.05F,(float)(.20*fade));}}}
    private static void flameStrikePillar(Vec3 target,double range,double age,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 c=groundTarget(target);double p=ease(age/.22),fade=fade(age,.80),r=Math.max(5.0,range*.25)*p,h=(10.0+pf*3.0)*p;m.band(g,c,r*.82,r,64,1.10F,(float)(.24*fade));for(int i=0;i<7;i++){double a=i*Math.PI*2/7;Vec3 b=c.add(g.point(a,r*.55));m.shard(b.add(0,h*.5,0),new Vec3(0,-1,0),ArcaneWorldMesh.Basis.facing(new Vec3(0,-1,0)),h,r*.10,1.16F,(float)(.30*fade));}m.orb(c.add(0,.5,0),r*.48,28,1.18F,(float)(.26*fade));}
    private static void dominateYoke(Vec3 target,double age,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double p=ease(age/.22),fade=fade(age,.83),r=(.9+.2*pf)*p;Vec3 head=target.add(0,1.75,0);m.brokenBand(g,head,r*.78,r,46,5,1.08F,(float)(.28*fade));for(int i=0;i<5;i++){double a=i*Math.PI*2/5+age;Vec3 b=head.add(g.point(a,r));m.line(b,b.add(0,.65*p,0),1.12F);m.line(b.add(0,.65*p,0),head.add(0,.18,0),.72F);}m.line(head,target.add(0,.65,0),1.16F);m.brokenBand(g,target.add(0,.65,0),r*.38,r*.54,34,4,.92F,(float)(.22*fade));}
    private static void massCureBloom(double range,double age,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double p=ease(age/.28),fade=fade(age,.80),r=Math.max(3.0,range*.42)*p;m.brokenBand(g,Vec3.ZERO,r*.90,r,68,7,1.08F,(float)(.20*fade));for(int i=0;i<8;i++){double a=i*Math.PI/4+age*.6;Vec3 n=g.point(a,r*.72);m.starPlate(g,n,.52*pf*p,.18*pf*p,4,a,1.08F,(float)(.18*fade));m.line(Vec3.ZERO,n,.62F);}m.orb(new Vec3(0,.8,0),.75*pf*p,20,1.02F,(float)(.18*fade));}
    private static void chainLightningFork(Vec3 dir,Vec3 target,double range,double age,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(dir);double p=ease(age/.15),fade=fade(age,.82);double len=Math.min(range,Math.max(target.length(),range*.72))*p;Vec3 hub=dir.scale(len*.18);m.brokenBand(f,hub,.28*pf,.45*pf,34,4,1.12F,(float)(.28*fade));for(int branch=0;branch<4;branch++){double side=(branch-1.5)*.55*pf;Vec3 mid=dir.scale(len*.52).add(f.right().scale(side)).add(f.up().scale(((branch&1)==0?1:-1)*.26*pf));Vec3 end=dir.scale(len).add(f.right().scale(side*1.45));jagged(m,hub,mid,f,branch,1.14F);jagged(m,mid,end,f,branch+5,1.02F);m.orb(end,.16*pf,10,1.08F,(float)(.22*fade));}}
    private static void arcaneHandGrip(Vec3 dir,Vec3 target,double age,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(dir);double p=ease(age/.25),fade=fade(age,.84),s=(.85+.18*pf)*p;Vec3 palm=target;m.polygonPlate(f,palm,s,5,age,1.06F,(float)(.20*fade));for(int i=-2;i<=2;i++){Vec3 base=palm.add(f.right().scale(i*s*.35)).add(f.up().scale(s*.25));double curl=.35+.65*Math.min(1,age/.48);Vec3 tip=base.add(f.up().scale(s*(.95-Math.abs(i)*.08))).add(dir.scale(s*.55*curl));m.line(base,tip,1.18F);m.orb(tip,s*.11,10,.96F,(float)(.20*fade));}m.brokenBand(f,palm,s*.70,s*.92,42,5,1.02F,(float)(.20*fade));}
    private static void teleportCircleTransit(Vec3 dir,Vec3 target,double range,double age,double pf,ArcaneWorldMesh.Builder m){ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double p=ease(age/.23),fade=fade(age,.84),r=Math.max(2.2,Math.min(6.5,range*.17))*p;Vec3 near=Vec3.ZERO,far=groundTarget(target);m.band(g,near,r*.80,r,68,1.08F,(float)(.24*fade));m.band(g,far,r*.80,r,68,1.02F,(float)(.22*fade));for(int i=0;i<6;i++){double a=i*Math.PI/3+age;Vec3 n0=near.add(g.point(a,r*.82)),n1=far.add(g.point(a,r*.82));m.line(n0,n0.add(0,1.8*p,0),.94F);m.line(n1,n1.add(0,1.8*p,0),.94F);if(age>.18)m.line(n0.add(0,1.25*p,0),n1.add(0,1.25*p,0),.54F);} }

    private static void rectFrame(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis b,Vec3 c,double halfW,double halfH,float width){Vec3 r=b.right().scale(halfW),u=b.up().scale(halfH);Vec3 a=c.subtract(r).subtract(u),d=c.add(r).subtract(u),e=c.add(r).add(u),f=c.subtract(r).add(u);m.line(a,d,width);m.line(d,e,width);m.line(e,f,width);m.line(f,a,width);}
    private static void jagged(ArcaneWorldMesh.Builder m,Vec3 a,Vec3 b,ArcaneWorldMesh.Basis basis,int seed,float width){Vec3 prev=a;for(int i=1;i<=6;i++){double t=i/6.0;Vec3 cur=a.scale(1-t).add(b.scale(t));if(i<6){double s=((seed+i)%2==0?1:-1)*(.10+.025*(seed%3));cur=cur.add(basis.right().scale(s)).add(basis.up().scale(((seed+i)%3-1)*.08));}m.line(prev,cur,width*(i%2==0?.72F:1.0F));prev=cur;}}
    private static Vec3 groundTarget(Vec3 v){return new Vec3(v.x,Math.max(-1.2,Math.min(1.2,v.y)),v.z);}
    private static double phase(double p,double start,double end){if(end<=start)return p>=end?1:0;return clamp((p-start)/(end-start));}
    private static double ease(double t){return 1-Math.pow(1-clamp(t),2.2);}
    private static double fade(double age,double start){return 1-clamp((age-start)/Math.max(.05,1-start));}
    private static double clamp(double v){return Math.max(0,Math.min(1,v));}
}
