from pathlib import Path
import re

repo = Path.cwd()
root = repo / 'projects/arcane-circle'
client = root / 'src/main/java/kr/moonseungjun/arcanecircle/client'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, text):
    path.write_text(text, encoding='utf-8')


def replace_once(path, old, new):
    text = read(path)
    if text.count(old) != 1:
        raise RuntimeError(f'{path}: expected one match, found {text.count(old)} for {old[:80]!r}')
    write(path, text.replace(old, new, 1))

# ---------------------------------------------------------------------------
# 1) Preserve the alpha.42 baseline while adding per-segment stroke hierarchy.
# Existing line(a,b,width) retains brightness=1/alpha=1, so circles 1-5 render
# exactly through the same three-pass pipeline as before.
# ---------------------------------------------------------------------------
mesh = client / 'ArcaneWorldMesh.java'
replace_once(mesh,
'''        if(!segments.isEmpty()){
            // Three edge passes create a saturated halo + readable mid edge + white-hot colored core.
            // This keeps the effect punchy without relying on thousands of vanilla particles.
            submitLines(poseStack,collector,tone(argb,.58,.34),windowScale*4.60F);
            submitLines(poseStack,collector,tone(argb,.82,.76),windowScale*2.20F);
            submitLines(poseStack,collector,tone(argb,.98,1.0),windowScale*.78F);
        }
    }
    private void submitLines(PoseStack stack,SubmitNodeCollector collector,int color,float scale){collector.submitCustomGeometry(stack,RenderTypes.lines(),(pose,out)->{for(Segment s:segments){Vec3 d=s.end.subtract(s.start);if(d.lengthSqr()<1e-8)continue;Vec3 n=d.normalize();float w=Math.max(lineFloor,s.width*scale*lineScale);out.addVertex(pose,(float)s.start.x,(float)s.start.y,(float)s.start.z).setColor(color).setNormal(pose,(float)n.x,(float)n.y,(float)n.z).setLineWidth(w);out.addVertex(pose,(float)s.end.x,(float)s.end.y,(float)s.end.z).setColor(color).setNormal(pose,(float)n.x,(float)n.y,(float)n.z).setLineWidth(w);}});}
''',
'''        if(!segments.isEmpty()){
            // Three passes remain the alpha.42 baseline. Per-segment brightness/alpha only lets
            // high-circle authored overlays distinguish authority strokes from construction detail.
            submitLines(poseStack,collector,argb,windowScale*4.60F,.58,.34);
            submitLines(poseStack,collector,argb,windowScale*2.20F,.82,.76);
            submitLines(poseStack,collector,argb,windowScale*.78F,.98,1.0);
        }
    }
    private void submitLines(PoseStack stack,SubmitNodeCollector collector,int argb,float scale,double passBrightness,double passAlpha){collector.submitCustomGeometry(stack,RenderTypes.lines(),(pose,out)->{for(Segment s:segments){Vec3 d=s.end.subtract(s.start);if(d.lengthSqr()<1e-8)continue;Vec3 n=d.normalize();float w=Math.max(lineFloor,s.width*scale*lineScale);int color=tone(argb,passBrightness*s.brightness,passAlpha*s.alpha);out.addVertex(pose,(float)s.start.x,(float)s.start.y,(float)s.start.z).setColor(color).setNormal(pose,(float)n.x,(float)n.y,(float)n.z).setLineWidth(w);out.addVertex(pose,(float)s.end.x,(float)s.end.y,(float)s.end.z).setColor(color).setNormal(pose,(float)n.x,(float)n.y,(float)n.z).setLineWidth(w);}});}
''')
replace_once(mesh,
'''    static Builder builder(int budget){return new Builder(budget,1.0F,.72F);}
    static Builder fineBuilder(int budget){return new Builder(budget,.46F,.34F);}
    record Segment(Vec3 start,Vec3 end,float width){}
''',
'''    static Builder builder(int budget){return new Builder(budget,1.0F,.72F);}
    static Builder fineBuilder(int budget){return new Builder(budget,.46F,.34F);}
    static Builder detailBuilder(int budget){return new Builder(budget,.36F,.18F);}
    record Segment(Vec3 start,Vec3 end,float width,float brightness,float alpha){}
''')
replace_once(mesh,
'''        Builder line(Vec3 a,Vec3 b,float width){if(!full()&&a!=null&&b!=null&&a.distanceToSqr(b)>1e-8)segments.add(new Segment(a,b,width));return this;}
''',
'''        Builder line(Vec3 a,Vec3 b,float width){return line(a,b,width,1.0F,1.0F);}
        Builder line(Vec3 a,Vec3 b,float width,float brightness,float alpha){if(!full()&&a!=null&&b!=null&&a.distanceToSqr(b)>1e-8)segments.add(new Segment(a,b,width,Math.max(.08F,brightness),Math.max(.02F,Math.min(1.0F,alpha))));return this;}
''')

# ---------------------------------------------------------------------------
# 2) High-circle timeline. This is additive only: the alpha.42 sigil/cinematic/
# overhaul layers remain untouched. Every 7-9C spell/fusion is explicitly owned.
# ---------------------------------------------------------------------------
timeline = r'''package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * Hand-authored 7C-9C temporal overlays.
 *
 * This layer never replaces ArcaneSigilDirector, SpellCinematicDirector or
 * ArcaneSpellVisualOverhaul. It adds a small number of deliberately weighted lines after the
 * proven alpha.42 presentation. Each spell owns its own spatial grammar and its own temporal
 * progression; shared helpers below are only drawing primitives.
 *
 * Design study notes: Ars Nouveau demonstrated stateful/tick-based emitters, Iron's Spells keeps
 * client VFX ownership at the spell, and Hex Casting separates strong outer strokes from fine
 * inner strokes. No source/assets from those projects are copied here.
 */
final class AuthoredHighCircleTimeline {
    private static final int CHARGE_BUDGET = 1150;
    private static final int RELEASE_BUDGET = 1500;
    private static final Set<String> AUTHORED = Set.of(
            "delayed_blast_fireball", "etherealness", "finger_of_death", "fire_storm",
            "forcecage", "plane_shift", "prismatic_spray", "reverse_gravity", "simulacrum",
            "teleport", "void_lance", "winter_domain",
            "antimagic_field", "clone", "control_weather", "demiplane", "dominate_monster",
            "earthquake", "feeblemind", "incendiary_cloud", "maze", "sunburst",
            "astral_prison", "phoenix_requiem",
            "meteor_swarm", "power_word_kill", "prismatic_wall", "shapechange", "time_stop",
            "true_polymorph", "weird", "wish", "gate", "foresight", "world_sunder");

    private AuthoredHighCircleTimeline() {}

    static ArcaneWorldMesh charge(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                                  double range, double progress, long startedAtNanos, long seed) {
        if (spell.circle() < 7 || !AUTHORED.contains(spell.id())) return ArcaneWorldMesh.builder(8).build();
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(CHARGE_BUDGET);
        double p = smooth(clamp(progress, 0.0, 1.0));
        double t = Math.max(0.0, (System.nanoTime() - startedAtNanos) / 1_000_000_000.0);
        draw(m, spell.id(), direction, targetOffset, range, p, t, false, 0.0, 0.0, seed);
        return m.build();
    }

    static ArcaneWorldMesh release(SpellDefinition spell, Vec3 direction, Vec3 targetOffset,
                                   double range, double age, double impactAge,
                                   double elapsedSeconds, double durationSeconds, long seed) {
        if (spell.circle() < 7 || !AUTHORED.contains(spell.id())) return ArcaneWorldMesh.builder(8).build();
        ArcaneWorldMesh.Builder m = ArcaneWorldMesh.detailBuilder(RELEASE_BUDGET);
        double life = clamp(age, 0.0, 1.0);
        double impact = clamp(impactAge <= 0.0 ? .55 : impactAge, .04, .94);
        draw(m, spell.id(), direction, targetOffset, range, 1.0, elapsedSeconds,
                true, life, impact, seed);
        return m.build();
    }

    private static void draw(ArcaneWorldMesh.Builder m, String id, Vec3 direction, Vec3 target,
                             double range, double p, double t, boolean release,
                             double age, double impactAge, long seed) {
        switch (id) {
            case "delayed_blast_fireball" -> delayedBlast(m, target, p, t, release, age);
            case "etherealness" -> etherealness(m, direction, p, t, release);
            case "finger_of_death" -> fingerOfDeath(m, direction, target, p, t, release, age);
            case "fire_storm" -> fireStorm(m, target, p, t, release, age);
            case "forcecage" -> forcecage(m, target, p, t, release);
            case "plane_shift" -> planeShift(m, direction, p, t, release);
            case "prismatic_spray" -> prismaticSpray(m, direction, p, t, release);
            case "reverse_gravity" -> reverseGravity(m, target, range, p, t, release);
            case "simulacrum" -> simulacrum(m, direction, p, t, release);
            case "teleport" -> teleport(m, direction, target, p, t, release);
            case "void_lance" -> voidLance(m, direction, target, p, t, release, age);
            case "winter_domain" -> winterDomain(m, p, t, release);

            case "antimagic_field" -> antimagic(m, p, t, release);
            case "clone" -> cloneVessel(m, direction, p, t, release);
            case "control_weather" -> controlWeather(m, target, p, t, release);
            case "demiplane" -> demiplane(m, direction, target, p, t, release);
            case "dominate_monster" -> dominateMonster(m, direction, target, p, t, release);
            case "earthquake" -> earthquake(m, target, range, p, t, release, seed);
            case "feeblemind" -> feeblemind(m, direction, target, p, t, release);
            case "incendiary_cloud" -> incendiaryCloud(m, target, range, p, t, release);
            case "maze" -> maze(m, target, p, t, release);
            case "sunburst" -> sunburst(m, target, p, t, release, age);
            case "astral_prison" -> astralPrison(m, target, p, t, release);
            case "phoenix_requiem" -> phoenixRequiem(m, direction, p, t, release, age);

            case "meteor_swarm" -> meteorSwarm(m, target, p, t, release, age);
            case "power_word_kill" -> powerWordKill(m, direction, target, p, t, release, age);
            case "prismatic_wall" -> prismaticWall(m, direction, target, range, p, t, release);
            case "shapechange" -> shapechange(m, p, t, release);
            case "time_stop" -> timeStop(m, p, t, release);
            case "true_polymorph" -> truePolymorph(m, direction, target, p, t, release);
            case "weird" -> weird(m, target, p, t, release);
            case "wish" -> wish(m, p, t, release);
            case "gate" -> gate(m, direction, target, p, t, release);
            case "foresight" -> foresight(m, direction, p, t, release);
            case "world_sunder" -> worldSunder(m, direction, target, range, p, t, release, age, seed);
            default -> { }
        }
    }

    // 7th circle -------------------------------------------------------------------------------
    private static void delayedBlast(ArcaneWorldMesh.Builder m, Vec3 c, double p, double t, boolean release, double age) {
        ArcaneWorldMesh.Basis g = ArcaneWorldMesh.Basis.ground();
        double r = 3.8 * (.58 + .42 * p), collapse = release ? Math.max(.12, 1.0 - age * 3.8) : 1.0;
        ring(m,g,c,r,64,MAJOR,.90F); ring(m,g,c,r*.68*collapse,48,MID,.62F);
        for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.08;Vec3 fuse=c.add(g.point(a,r*.86));marker(m,g,fuse,.16,DETAIL,.45F);line(m,fuse,c.add(g.point(a,r*.34*collapse)),DETAIL,.34F);}
        if(release){for(int i=0;i<8;i++){double a=i*Math.PI/4;line(m,c.add(g.point(a,r*1.08)),c.add(g.point(a,r*.10*collapse)),MAJOR,.94F);}}
    }

    private static void etherealness(ArcaneWorldMesh.Builder m, Vec3 direction, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction), x=ArcaneWorldMesh.Basis.fromNormal(f.right(),f.up());
        double slip=release?.32:.10+.14*p, spin=release?0:t*.05;
        poly(m,f,f.normal().scale(-slip),1.12,7,spin,MAJOR,.76F); poly(m,f,f.normal().scale(slip),1.12,7,-spin,MID,.54F);
        ring(m,x,new Vec3(0,.92,0),.82,44,MID,.52F);
        for(int i=0;i<7;i++){double a=i*Math.PI*2/7;line(m,f.point(a,1.02).add(f.normal().scale(-slip)),f.point(a+.08,1.02).add(f.normal().scale(slip)),DETAIL,.38F);}
    }

    private static void fingerOfDeath(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double p, double t, boolean release, double age) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction); Vec3 mark=target.add(0,1.0,0); double r=1.25+.25*p;
        poly(m,f,mark,r,5,Math.PI/2,MAJOR,.82F); ring(m,f,mark,r*.52,36,MID,.58F);
        double snap=release?Math.max(.04,1-age*5):1;
        for(int i=0;i<5;i++){double a=i*Math.PI*2/5+t*.018;Vec3 out=mark.add(f.point(a,r*1.24));line(m,out,mark.add(f.point(a,r*.16*snap)),i==0?MAJOR:MID,i==0?.95F:.56F);}
    }

    private static void fireStorm(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t, boolean release, double age) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground(); Vec3 sky=target.add(0,10.5,0); double r=5.0;
        ring(m,g,sky,r,66,MAJOR,.76F); poly(m,g,sky,r*.72,6,t*.025,MID,.54F);
        for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 top=sky.add(g.point(a,r*.74)),hit=target.add(g.point(a,r*.86));marker(m,g,top,.18,DETAIL,.40F);double start=release?clamp(age*2.1-i*.045,0,1):p*.35;line(m,top,top.add(hit.subtract(top).scale(start)),i%2==0?MAJOR:MID,.72F);}
    }

    private static void forcecage(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground(); double r=1.75,h=(2.8+.7*p); Vec3 top=target.add(0,h,0);
        poly(m,g,target,r,8,t*.012,MAJOR,.80F); poly(m,g,top,r*.92,8,-t*.012,MID,.62F);
        for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 foot=target.add(g.point(a,r));line(m,foot,top.add(g.point(a+.05,r*.92)),i%2==0?MAJOR:MID,i%2==0?.82F:.50F);}
        ring(m,ArcaneWorldMesh.Basis.facing(new Vec3(1,0,0)),target.add(0,h*.5,0),r*.72,40,DETAIL,.36F);
    }

    private static void planeShift(ArcaneWorldMesh.Builder m, Vec3 direction, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction); double r=1.55,spin=release?0:t*.025;
        for(int layer=-3;layer<=3;layer++){double d=layer*.14;Vec3 c=f.normal().scale(d).add(0,.92,0);poly(m,f,c,r*(1-Math.abs(layer)*.045),7,spin+layer*.13,layer==0?MAJOR:DETAIL,layer==0?.82F:.30F);}
        for(int i=0;i<7;i++){double a=i*Math.PI*2/7;line(m,f.point(a,r*.94).add(f.normal().scale(-.42)).add(0,.92,0),f.point(a+.08,r*.72).add(f.normal().scale(.42)).add(0,.92,0),MID,.44F);}
    }

    private static void prismaticSpray(ArcaneWorldMesh.Builder m, Vec3 direction, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction); Vec3 hub=direction.scale(.72); double open=.35+.65*p;
        for(int i=0;i<7;i++){double x=(i-3)*.22*open;Vec3 gate=hub.add(f.right().scale(x));poly(m,f,gate,.23,4+i%3,t*(i%2==0?.05:-.04),i==3?MAJOR:DETAIL,i==3?.76F:.34F);Vec3 tip=direction.scale(2.2).add(f.right().scale((i-3)*.68*open));line(m,gate,tip,i==3?MAJOR:MID,.58F);}
    }

    private static void reverseGravity(ArcaneWorldMesh.Builder m, Vec3 target, double range, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground(); double r=Math.max(3.2,range*.16),h=release?4.8:1.2*p;
        poly(m,g,target,r,8,Math.PI/8,MAJOR,.72F); ring(m,g,target,r*.64,48,MID,.48F);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){Vec3 foot=target.add(x*r*.28,0,z*r*.28);line(m,foot,foot.add(0,h*(.72+.07*((x*x+z*z)%4)),0),(x==0||z==0)?MID:DETAIL,(x==0||z==0)?.48F:.26F);}
    }

    private static void simulacrum(ArcaneWorldMesh.Builder m, Vec3 direction, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction);Vec3 echo=f.right().scale(-1.15).add(0,1.0,0),heart=echo.add(0,.05,0);
        poly(m,f,echo,.72,6,t*.025,MAJOR,.76F);ring(m,f,heart,.22,20,MID,.56F);line(m,Vec3.ZERO.add(0,.92,0),heart,MID,.46F);
        Vec3 head=echo.add(0,.70,0),hip=echo.add(0,-.48,0);ring(m,f,head,.18,16,DETAIL,.38F);line(m,head,hip,MID,.46F);line(m,echo.add(f.right().scale(-.42)),echo.add(f.right().scale(.42)),DETAIL,.32F);line(m,hip,hip.add(f.right().scale(-.28)).add(0,-.48,0),DETAIL,.30F);line(m,hip,hip.add(f.right().scale(.28)).add(0,-.48,0),DETAIL,.30F);
    }

    private static void teleport(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction);Vec3 near=direction.scale(1.1).add(0,.9,0),far=target.add(0,.9,0);double r=1.2;
        for(int d=0;d<3;d++){poly(m,f,near.add(f.normal().scale(d*.10)),r*(1-d*.10),8,t*.02+d*.1,d==0?MAJOR:DETAIL,d==0?.72F:.32F);if(target.lengthSqr()>2)poly(m,f,far.add(f.normal().scale(-d*.10)),r*(1-d*.10),8,-t*.02-d*.1,d==0?MAJOR:DETAIL,d==0?.72F:.32F);}
        if(target.lengthSqr()>2)for(int i=0;i<4;i++){double a=Math.PI/4+i*Math.PI/2;line(m,near.add(f.point(a,r*.78)),far.add(f.point(a,r*.78)),MID,.36F);}
    }

    private static void voidLance(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double p, double t, boolean release, double age) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction);double len=Math.max(4,target.length());
        for(int i=0;i<4;i++){Vec3 c=direction.scale(.55+i*.72);double r=.58-i*.09;poly(m,f,c,r,6,t*(i%2==0?.05:-.04)+i*.2,i==0?MAJOR:DETAIL,i==0?.78F:.32F);}
        double reveal=release?clamp(age*3.0,0,1):p*.45;line(m,direction.scale(.5),direction.scale(.5+len*reveal),MAJOR,.86F);
    }

    private static void winterDomain(ArcaneWorldMesh.Builder m, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double r=3.8;
        ring(m,g,Vec3.ZERO,r,64,MAJOR,.72F);poly(m,g,Vec3.ZERO,r*.72,6,t*.015,MID,.52F);
        for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 tip=g.point(a,r*.92);line(m,Vec3.ZERO,tip,MID,.50F);Vec3 mid=g.point(a,r*.58);line(m,mid,mid.add(g.point(a+.55,r*.18)),DETAIL,.30F);line(m,mid,mid.add(g.point(a-.55,r*.18)),DETAIL,.30F);if(release)line(m,tip,tip.add(0,2.2+.25*(i%2),0),DETAIL,.34F);}
    }

    // 8th circle -------------------------------------------------------------------------------
    private static void antimagic(ArcaneWorldMesh.Builder m, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double spin=release?0:t*.018,r=3.0;
        // Intentionally incomplete: a null field should not resemble a stable protective circle.
        arc(m,g,Vec3.ZERO,r,.18+spin,Math.PI*1.28,46,MAJOR,.72F);arc(m,g,Vec3.ZERO,r*.72,-2.3-spin,Math.PI*.78,32,MID,.48F);arc(m,g,new Vec3(0,.85,0),r*.44,.9,Math.PI*1.05,28,DETAIL,.30F);
        for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 out=g.point(a,r*.90);line(m,out,g.point(a+.16*(i%2==0?1:-1),r*.22),i%2==0?MID:DETAIL,i%2==0?.42F:.26F);}
    }

    private static void cloneVessel(ArcaneWorldMesh.Builder m, Vec3 direction, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction),g=ArcaneWorldMesh.Basis.ground();Vec3 vessel=f.normal().scale(-.78).add(0,.78,0),heart=vessel.add(0,.12,0);
        poly(m,f,vessel,.82,8,t*.018,MAJOR,.72F);poly(m,f,vessel,.58,4,Math.PI/4-t*.018,MID,.50F);ring(m,f,heart,.22,20,MID,.58F);line(m,Vec3.ZERO.add(0,.92,0),heart,MAJOR,.62F);ring(m,g,Vec3.ZERO,.95,44,DETAIL,.32F);
    }

    private static void controlWeather(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 sky=target.add(0,13.5,0);double r=7.0;
        ring(m,g,sky,r,76,MAJOR,.68F);poly(m,g,sky,r*.78,8,t*.010,MID,.50F);ring(m,g,sky,r*.38,42,DETAIL,.30F);
        for(int i=0;i<8;i++){double a=i*Math.PI/4+t*.012;Vec3 vane=sky.add(g.point(a,r*.84));line(m,sky.add(g.point(a,r*.40)),vane,MID,.42F);line(m,vane,vane.add(g.point(a+.55,r*.22)),DETAIL,.28F);if(release&&i%2==0)line(m,vane,vane.add(0,-3.0-(i%3),0),DETAIL,.32F);}
    }

    private static void demiplane(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction);Vec3 base=direction.scale(1.8).add(0,1.6,0);double r=2.15;
        for(int d=0;d<6;d++){Vec3 c=base.add(f.normal().scale(d*.24));double rr=r*(1-d*.10);poly(m,f,c,rr,10-d%2*2,t*(d%2==0?.010:-.008)+d*.13,d==0?MAJOR:DETAIL,d==0?.78F:.28F);}
        for(int i=0;i<4;i++){double a=Math.PI/4+i*Math.PI/2;line(m,base.add(f.point(a,r*.88)),base.add(f.normal().scale(1.2)).add(f.point(a,r*.42)),MID,.36F);}
    }

    private static void dominateMonster(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction),g=ArcaneWorldMesh.Basis.ground();Vec3 head=target.add(0,1.62,0),crown=head.add(0,.76,0);double r=1.28;
        poly(m,f,head,r,8,t*.012,MAJOR,.76F);poly(m,g,crown,.72,8,-t*.018,MID,.54F);ring(m,f,head,r*.46,34,DETAIL,.34F);
        for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 cmd=crown.add(g.point(a,.68)),bind=target.add(g.point(a,.66)).add(0,.62+.12*(i%3),0);marker(m,g,cmd,.11,DETAIL,.30F);line(m,cmd,bind,i%2==0?MID:DETAIL,i%2==0?.46F:.28F);line(m,bind,head,DETAIL,.24F);}
    }

    private static void earthquake(ArcaneWorldMesh.Builder m, Vec3 target, double range, double p, double t, boolean release, long seed) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double r=Math.max(5.0,range*.22);
        ring(m,g,target,r*.42,46,MID,.40F);ring(m,g,target,r*.78,58,DETAIL,.28F);
        for(int i=0;i<12;i++){double a=i*Math.PI/6+((seed>>i)&3)*.035;Vec3 a0=target.add(g.point(a,r*.08)),a1=target.add(g.point(a+.13*Math.sin(i*1.7),r*.48)),a2=target.add(g.point(a-.09*Math.cos(i),r*.92));line(m,a0,a1,i%3==0?MAJOR:MID,i%3==0?.68F:.42F);line(m,a1,a2,DETAIL,.28F);if(release&&i%3==0)line(m,a1,a1.add(0,.8+.15*(i%4),0),DETAIL,.32F);}
    }

    private static void feeblemind(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction);Vec3 head=target.add(0,1.55,0);double r=1.12;
        arc(m,f,head,r,.18+t*.015,Math.PI*.72,26,MAJOR,.68F);arc(m,f,head,r,Math.PI+1.0-t*.012,Math.PI*.58,22,MID,.44F);ring(m,f,head,r*.40,30,DETAIL,.30F);
        for(int i=0;i<6;i++){double a=i*Math.PI/3;Vec3 inner=head.add(f.point(a,r*.42)),outer=head.add(f.point(a+(i%2==0?.18:-.14),r*(release?1.45:1.02)));line(m,inner,outer,i%3==0?MID:DETAIL,i%3==0?.44F:.26F);}
    }

    private static void incendiaryCloud(ArcaneWorldMesh.Builder m, Vec3 target, double range, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double r=Math.max(4.2,range*.18);Vec3 canopy=target.add(0,4.8,0);
        ring(m,g,canopy,r,62,MAJOR,.62F);poly(m,g,canopy,r*.72,8,t*.012,MID,.44F);
        for(int i=0;i<10;i++){double a=i*Math.PI/5+t*.016;Vec3 cell=canopy.add(g.point(a,r*(.52+.08*(i%2))));marker(m,g,cell,.18,DETAIL,.28F);if(release)line(m,cell,target.add(g.point(a,r*.72)),i%3==0?MID:DETAIL,i%3==0?.40F:.24F);}
    }

    private static void maze(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double r=2.7;
        for(int layer=0;layer<4;layer++)squareCorridor(m,g,target,r*(1-layer*.19),Math.PI/4+layer*.17,(layer*3+1)%4,layer==0?MAJOR:DETAIL,layer==0?.72F:.30F);
        for(int layer=0;layer<3;layer++){double rr=r*(1-layer*.19);Vec3 a=target.add(g.point(Math.PI/4+(layer*.17)+(layer*3+2)*Math.PI/2,rr));Vec3 b=target.add(g.point(Math.PI/4+((layer+1)*.17)+(layer*3+2)*Math.PI/2,r*(1-(layer+1)*.19)));line(m,a,b,MID,.38F);}
    }

    private static void sunburst(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t, boolean release, double age) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double r=4.8*(release?Math.min(1,age*2.6):.55+.45*p);
        ring(m,g,target,r*.34,40,MAJOR,.72F);for(int i=0;i<20;i++){double a=i*Math.PI/10;double end=r*(i%4==0?1.08:i%2==0?.88:.72);line(m,target.add(g.point(a,r*.36)),target.add(g.point(a,end)),i%4==0?MAJOR:i%2==0?MID:DETAIL,i%4==0?.76F:i%2==0?.46F:.26F);}
    }

    private static void astralPrison(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground(),x=ArcaneWorldMesh.Basis.facing(new Vec3(1,0,0));double r=1.9,h=3.4;Vec3 mid=target.add(0,h*.5,0),top=target.add(0,h,0);
        poly(m,g,target,r,8,t*.012,MAJOR,.74F);poly(m,g,top,r*.82,8,-t*.012,MID,.52F);poly(m,g,target,r*.74,4,Math.PI/4,DETAIL,.34F);ring(m,x,mid,r*.66,42,DETAIL,.32F);
        for(int i=0;i<8;i++){double a=i*Math.PI/4;line(m,target.add(g.point(a,r)),top.add(g.point(a+.08,r*.82)),i%2==0?MID:DETAIL,i%2==0?.44F:.26F);}
    }

    private static void phoenixRequiem(ArcaneWorldMesh.Builder m, Vec3 direction, double p, double t, boolean release, double age) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction);Vec3 heart=new Vec3(0,1.05,0);double spread=1.25+.75*(release?Math.min(1,age*2):p);
        ring(m,f,heart,.32,24,MAJOR,.72F);for(int side:new int[]{-1,1}){Vec3 root=heart.add(f.right().scale(side*.18));Vec3 elbow=heart.add(f.right().scale(side*spread*.68)).add(f.up().scale(spread*.62));Vec3 tip=heart.add(f.right().scale(side*spread)).add(f.up().scale(spread*.10));line(m,root,elbow,MAJOR,.76F);line(m,elbow,tip,MID,.52F);for(int k=1;k<=4;k++){double q=k/4.0;line(m,elbow,root.add(f.right().scale(side*spread*q)).add(f.up().scale(-spread*.34*q)),DETAIL,.28F);}}
    }

    // 9th circle -------------------------------------------------------------------------------
    private static void meteorSwarm(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t, boolean release, double age) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 sky=target.add(0,22,0);double r=7.8;
        // Predictor only; the alpha.42 cinematic remains the actual meteor body.
        ring(m,g,sky,r,84,MAJOR,.62F);ring(m,g,sky,r*.66,60,DETAIL,.26F);
        for(int i=0;i<16;i++){double a=i*Math.PI/8;Vec3 tick=sky.add(g.point(a,r*.92));line(m,tick,sky.add(g.point(a,r*(i%4==0?.72:.82))),i%4==0?MAJOR:DETAIL,i%4==0?.62F:.24F);if(i%4==0)marker(m,g,tick,.20,MID,.38F);}
        if(release){double sweep=age*Math.PI*2;line(m,sky,sky.add(g.point(sweep,r*.62)),MID,.40F);}
    }

    private static void powerWordKill(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double p, double t, boolean release, double age) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction);Vec3 c=target.add(0,1.08,0);double r=1.42,snap=release?Math.max(.04,1-age*6):1;
        poly(m,f,c,r,4,Math.PI/4,MAJOR,.88F);poly(m,f,c,r*.70,8,Math.PI/8,MID,.52F);ring(m,f,c,r*.34,30,DETAIL,.30F);
        for(int i=0;i<4;i++){double a=i*Math.PI/2;line(m,c.add(f.point(a,r*1.35)),c.add(f.point(a,r*.08*snap)),MAJOR,.96F);}
    }

    private static void prismaticWall(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double range, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(flat(direction));double w=Math.max(7.0,range*.38),h=5.8;Vec3 right=f.right();
        line(m,target.add(right.scale(-w*.5)),target.add(right.scale(w*.5)),MAJOR,.72F);line(m,target.add(right.scale(-w*.5)).add(0,h,0),target.add(right.scale(w*.5)).add(0,h,0),MAJOR,.72F);
        for(int i=0;i<7;i++){double x=(i-3)*w/7.0;Vec3 foot=target.add(right.scale(x)),top=foot.add(0,h,0),mid=foot.add(0,h*.52,0);line(m,foot,top,i==3?MAJOR:MID,i==3?.78F:.46F);marker(m,f,foot.add(0,.26,0),.14,DETAIL,.28F);marker(m,f,top.add(0,-.26,0),.14,DETAIL,.28F);if(i<6)line(m,mid,target.add(right.scale((i-2)*w/7.0)).add(0,h*.72,0),DETAIL,.24F);}
    }

    private static void shapechange(ArcaneWorldMesh.Builder m, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground(),f=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1));
        for(int layer=0;layer<7;layer++){double y=.16+layer*.28,rr=.46+layer*.10+.04*Math.sin(t+layer);poly(m,g,new Vec3(0,y,0),rr,3+(layer%5),t*(layer%2==0?.06:-.05)+layer*.21,layer%3==0?MID:DETAIL,layer%3==0?.44F:.26F);}
        Vec3 shoulder=new Vec3(0,1.34,0);for(int side:new int[]{-1,1}){Vec3 root=shoulder.add(f.right().scale(side*.24)),tip=root.add(f.right().scale(side*.86)).add(0,.18,0);line(m,root,tip,MAJOR,.66F);for(int k=0;k<3;k++)line(m,tip,tip.add(f.right().scale(side*(.22+.09*k))).add(f.up().scale(.16-.10*k)),DETAIL,.28F);}
    }

    private static void timeStop(ArcaneWorldMesh.Builder m, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground(),x=ArcaneWorldMesh.Basis.facing(new Vec3(1,0,0));double r=4.2,spin=release?0:t*.018;
        ring(m,g,Vec3.ZERO,r,88,MAJOR,.74F);poly(m,g,Vec3.ZERO,r*.82,12,spin,MID,.50F);ring(m,g,Vec3.ZERO,r*.58,62,DETAIL,.28F);
        for(int i=0;i<12;i++){double a=i*Math.PI/6;line(m,g.point(a,r*.80),g.point(a,r),i%3==0?MAJOR:DETAIL,i%3==0?.62F:.24F);}
        Vec3 hub=new Vec3(0,1.48,0);ring(m,x,hub,r*.32,48,MID,.40F);double hand=release?-.72:-Math.PI/2+t*.09;line(m,hub,hub.add(g.point(hand,r*.28)),MAJOR,.82F);line(m,hub,hub.add(g.point(release?1.92:-Math.PI/2-t*.18,r*.19)),MID,.52F);
    }

    private static void truePolymorph(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground(),f=ArcaneWorldMesh.Basis.facing(direction);Vec3 seal=target.add(0,2.42,0);
        double[] rr=new double[8];for(int layer=0;layer<8;layer++){double y=.16+layer*.27;rr[layer]=.48+layer*.10+.045*Math.sin(t*.8+layer);poly(m,g,target.add(0,y,0),rr[layer],3+(layer*2%7),t*(layer%2==0?.05:-.045)+layer*.20,layer%2==0?MID:DETAIL,layer%2==0?.42F:.24F);}
        for(int layer=0;layer<7;layer++)for(int k=0;k<4;k++){double a=k*Math.PI/2+layer*.11;Vec3 a0=target.add(g.point(a,rr[layer])).add(0,.16+layer*.27,0),a1=target.add(g.point(a+.10,rr[layer+1])).add(0,.16+(layer+1)*.27,0);line(m,a0,a1,DETAIL,.24F);}
        poly(m,f,seal,.78,10,-t*.012,MAJOR,.62F);line(m,target.add(0,1.0,0),seal,MID,.36F);
    }

    private static void weird(ArcaneWorldMesh.Builder m, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(new Vec3(0,0,1)),x=ArcaneWorldMesh.Basis.facing(new Vec3(1,0,0)),g=ArcaneWorldMesh.Basis.ground();ArcaneWorldMesh.Basis[] planes={f,x,g};
        Vec3[] eyes={target.add(0,1.55,0),target.add(1.45,2.05,.35),target.add(-1.25,1.78,-.55)};
        for(int i=0;i<3;i++){ArcaneWorldMesh.Basis b=planes[i];Vec3 e=eyes[i];arc(m,b,e,.82-.10*i,.12,Math.PI*.82,30,i==0?MAJOR:MID,i==0?.70F:.46F);arc(m,b,e,.82-.10*i,Math.PI+.12,Math.PI*.82,30,i==0?MAJOR:MID,i==0?.70F:.46F);ring(m,b,e,.22+.03*i,20,DETAIL,.30F);line(m,e,target.add(0,.52,0),DETAIL,.26F);}
        if(release)for(int i=0;i<6;i++){double a=i*Math.PI/3;line(m,target.add(g.point(a,.8)),target.add(g.point(a+.2*(i%2==0?1:-1),3.6)),MID,.38F);}
    }

    private static void wish(ArcaneWorldMesh.Builder m, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();double r=3.45;Vec3 mid=new Vec3(0,2.15,0),top=new Vec3(0,4.15,0);
        poly(m,g,Vec3.ZERO,r,9,t*.008,MAJOR,.70F);poly(m,g,mid,r*.52,9,-t*.006,MID,.44F);poly(m,g,top,r*.66,9,t*.005+.13,MID,.48F);
        for(int i=0;i<9;i++){double a=i*Math.PI*2/9;Vec3 low=g.point(a,r*.88).add(0,.42+.18*(i%3),0),m0=mid.add(g.point(a+.06*(i%2==0?1:-1),r*.42)),hi=top.add(g.point(a,r*.52));marker(m,g,low,.14,DETAIL,.30F);line(m,low,m0,MID,.38F);line(m,m0,hi,DETAIL,.30F);}
        ring(m,g,top,r*.28,36,DETAIL,.28F);
    }

    private static void gate(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(flat(direction));Vec3 base=direction.scale(2.4).add(0,3.0,0);double r=3.0;
        for(int d=0;d<7;d++){Vec3 c=base.add(f.normal().scale(d*.25));double rr=r*(1-d*.078);poly(m,f,c,rr,12-d%2*4,t*(d%2==0?.008:-.007)+d*.12,d==0?MAJOR:DETAIL,d==0?.82F:.28F);}
        for(int i=0;i<4;i++){double a=Math.PI/4+i*Math.PI/2;line(m,base.add(f.point(a,r*.90)),base.add(f.normal().scale(1.55)).add(f.point(a,r*.44)),MID,.38F);}
        if(target.lengthSqr()>4)line(m,base,target.add(0,2.6,0),DETAIL,.24F);
    }

    private static void foresight(ArcaneWorldMesh.Builder m, Vec3 direction, double p, double t, boolean release) {
        ArcaneWorldMesh.Basis f=ArcaneWorldMesh.Basis.facing(direction),g=ArcaneWorldMesh.Basis.ground();Vec3 eye=new Vec3(0,2.0,0);double r=.88;
        arc(m,f,eye,r,.10,Math.PI*.82,32,MAJOR,.68F);arc(m,f,eye,r,Math.PI+.10,Math.PI*.82,32,MAJOR,.68F);ring(m,f,eye,.25,22,MID,.48F);
        Vec3 origin=new Vec3(0,.08,0);for(int lane=0;lane<4;lane++){double side=(lane-1.5)*.28;Vec3 a=origin.add(f.right().scale(side*.25)),b=direction.scale(1.4).add(f.right().scale(side)).add(0,.32+.16*lane,0),c=direction.scale(3.0).add(f.right().scale(side*(1.5+(lane%2)*.35))).add(0,.50+.10*lane,0);line(m,a,b,lane==1?MID:DETAIL,lane==1?.46F:.26F);line(m,b,c,lane==1?MAJOR:DETAIL,lane==1?.62F:.24F);line(m,c,eye,DETAIL,.20F);}
        ring(m,g,Vec3.ZERO,1.22,48,DETAIL,.28F);
    }

    private static void worldSunder(ArcaneWorldMesh.Builder m, Vec3 direction, Vec3 target, double range, double p, double t, boolean release, double age, long seed) {
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();Vec3 flat=flat(direction);double length=Math.max(24,Math.min(62,range*.78));double grow=release?clamp(age*2.4,0,1):.24+.34*p;
        Vec3 prev=target.subtract(flat.scale(length*.42));
        for(int i=0;i<13;i++){double q=i/12.0;Vec3 along=target.subtract(flat.scale(length*.42)).add(flat.scale(length*q));Vec3 side=new Vec3(-flat.z,0,flat.x).scale(Math.sin(i*1.73+(seed&15)*.03)*(1.1+.18*(i%3)));Vec3 node=along.add(side);if(i>0)line(m,prev,node,i%3==0?MAJOR:MID,i%3==0?.78F:.46F);if(release&&q<=grow)line(m,node,node.add(0,1.0+.24*(i%4),0),i%3==0?MID:DETAIL,i%3==0?.42F:.24F);marker(m,g,node,.13+(i%3)*.02,DETAIL,.26F);prev=node;}
        ring(m,g,target,2.2,42,DETAIL,.24F);
    }

    // Drawing primitives -----------------------------------------------------------------------
    private static final float MAJOR=1.18F, MID=.72F, DETAIL=.38F;

    private static void line(ArcaneWorldMesh.Builder m, Vec3 a, Vec3 b, float width, float alpha){
        float brightness=width>=MAJOR?.98F:width>=MID?.72F:.48F;
        m.line(a,b,width,brightness,alpha);
    }
    private static void ring(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 c, double r, int n, float width, float alpha){
        arc(m,b,c,r,-Math.PI/2,Math.PI*2,n,width,alpha);
    }
    private static void arc(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 c, double r, double start, double sweep, int n, float width, float alpha){
        int count=Math.max(4,n);Vec3 prev=c.add(b.point(start,r));for(int i=1;i<=count;i++){Vec3 cur=c.add(b.point(start+sweep*i/count,r));line(m,prev,cur,width,alpha);prev=cur;}
    }
    private static void poly(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 c, double r, int sides, double rot, float width, float alpha){
        int n=Math.max(3,sides);Vec3 first=c.add(b.point(rot,r)),prev=first;for(int i=1;i<n;i++){Vec3 cur=c.add(b.point(rot+i*Math.PI*2/n,r));line(m,prev,cur,width,alpha);prev=cur;}line(m,prev,first,width,alpha);
    }
    private static void marker(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, Vec3 c, double r, float width, float alpha){
        poly(m,b,c,r,4,Math.PI/4,width,alpha);
    }
    private static void squareCorridor(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, Vec3 c, double r, double rot, int gap, float width, float alpha){
        Vec3[] q=new Vec3[4];for(int i=0;i<4;i++)q[i]=c.add(g.point(rot+i*Math.PI/2,r));for(int i=0;i<4;i++)if(i!=Math.floorMod(gap,4))line(m,q[i],q[(i+1)%4],width,alpha);
    }
    private static Vec3 flat(Vec3 v){Vec3 f=new Vec3(v.x,0,v.z);return f.lengthSqr()<1e-8?new Vec3(0,0,1):f.normalize();}
    private static double smooth(double t){return t*t*(3-2*t);}
    private static double clamp(double v,double a,double b){return Math.max(a,Math.min(b,v));}
}
'''
write(client / 'AuthoredHighCircleTimeline.java', timeline)

# ---------------------------------------------------------------------------
# 3) Wire it AFTER the restored alpha.42 layers. Low circles never call it.
# ---------------------------------------------------------------------------
tracker = client / 'WorldMagicTracker.java'
replace_once(tracker, 'private static final int MAX_FRAME = 12000;\n    private static final int MAX_ENTRY = 3400;',
             'private static final int MAX_FRAME = 14500;\n    private static final int MAX_ENTRY = 4000;')
replace_once(tracker,
'''            ArcaneWorldMesh authoredBody=MeteorBarragePattern.withSeed(v.seed,
                    ()->ArcaneSpellVisualOverhaul.chargeBody(v.spell,v.direction,targetOffset(v),v.progress,v.range,v.startedAt));
            if(authoredBody.size()>0)entries.add(new RenderEntry(v.center,authoredBody,color));
''',
'''            ArcaneWorldMesh authoredBody=MeteorBarragePattern.withSeed(v.seed,
                    ()->ArcaneSpellVisualOverhaul.chargeBody(v.spell,v.direction,targetOffset(v),v.progress,v.range,v.startedAt));
            if(authoredBody.size()>0)entries.add(new RenderEntry(v.center,authoredBody,color));
            if(v.spell.circle()>=7){
                ArcaneWorldMesh timeline=MeteorBarragePattern.withSeed(v.seed,
                        ()->AuthoredHighCircleTimeline.charge(v.spell,v.direction,targetOffset(v),v.range,v.progress,v.startedAt,v.seed));
                if(timeline.size()>0)entries.add(new RenderEntry(v.center,timeline,color));
            }
''')
replace_once(tracker,
'''            ArcaneWorldMesh authoredRelease=MeteorBarragePattern.withSeed(v.seed,
                    ()->ArcaneSpellVisualOverhaul.release(v.spell,v.direction,targetOffset(v),v.range,v.power,
                            age,elapsedSeconds,durationSeconds,v.seed));
            if(authoredRelease.size()>0)entries.add(new RenderEntry(v.center,authoredRelease,color));
''',
'''            ArcaneWorldMesh authoredRelease=MeteorBarragePattern.withSeed(v.seed,
                    ()->ArcaneSpellVisualOverhaul.release(v.spell,v.direction,targetOffset(v),v.range,v.power,
                            age,elapsedSeconds,durationSeconds,v.seed));
            if(authoredRelease.size()>0)entries.add(new RenderEntry(v.center,authoredRelease,color));
            if(v.spell.circle()>=7){
                ArcaneWorldMesh timeline=MeteorBarragePattern.withSeed(v.seed,
                        ()->AuthoredHighCircleTimeline.release(v.spell,v.direction,targetOffset(v),v.range,
                                age,v.impactAge,elapsedSeconds,durationSeconds,v.seed));
                if(timeline.size()>0)entries.add(new RenderEntry(v.center,timeline,color));
            }
''')

# ---------------------------------------------------------------------------
# 4) Version + contract + verification.
# ---------------------------------------------------------------------------
for rel in ['gradle.properties', 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
            'src/main/resources/data/arcanecircle/spell_catalog/index.json', 'tools/test_current_source.py']:
    pth=root/rel
    body=read(pth)
    if '0.12.1-alpha.42' not in body:
        raise RuntimeError(f'{rel}: alpha.42 version marker missing')
    write(pth,body.replace('0.12.1-alpha.42','0.12.1-alpha.43'))

# gradle comment should describe this release, not the rollback release.
gradle=root/'gradle.properties'
body=read(gradle)
body=re.sub(r'# alpha\.42 presentation quality rollback:.*',
            '# alpha.43 reference-informed authored timelines: preserve low-circle sigils, refine 7-9C staging/strokes',body)
write(gradle,body)

project=root/'PROJECT.md'
body=read(project)
section='''\n\n## Alpha.43 reference-informed high-circle VFX\n- Alpha.42 remains the immutable low/mid-circle visual baseline. Circles 1-5 keep the existing `ArcaneSigilDirector` formula frame and are not routed through the new overlay.\n- Public source from Ars Nouveau, Iron's Spells 'n Spellbooks and Hex Casting was studied for architectural lessons only: temporal VFX state, spell-owned client presentation, and stroke hierarchy. No external code, texture, model, shader or asset is copied into Arcane Circle.\n- `AuthoredHighCircleTimeline` explicitly owns all 35 circle-7/8/9 direct/fusion spells. Each spell has a hand-authored temporal/spatial composition layered after the proven alpha.42 sigil/cinematic/overhaul stack.\n- `ArcaneWorldMesh` supports per-segment brightness/alpha while the legacy `line(a,b,width)` path remains visually identical. High-circle overlays use major/mid/detail strokes so added geometry reads as hierarchy instead of a uniform neon wireframe.\n- Charge, release/impact and maintained-state moments are allowed to use different geometry. High-circle refinement must increase staging, depth and readable silhouette before raw line count.\n'''
if '## Alpha.43 reference-informed high-circle VFX' not in body:
    body+=section
write(project,body)

# Source audit: explicit 35-spell ownership, preserved base formula, additive wiring, styled lines.
audit=root/'tools/test_current_source.py'
body=read(audit)
append=r'''

# Alpha.43 reference-informed high-circle authored timeline.
timeline_path=client/'AuthoredHighCircleTimeline.java'
assert timeline_path.exists()
timeline=text(timeline_path)
expected_high_circle={
'delayed_blast_fireball','etherealness','finger_of_death','fire_storm','forcecage','plane_shift','prismatic_spray','reverse_gravity','simulacrum','teleport','void_lance','winter_domain',
'antimagic_field','clone','control_weather','demiplane','dominate_monster','earthquake','feeblemind','incendiary_cloud','maze','sunburst','astral_prison','phoenix_requiem',
'meteor_swarm','power_word_kill','prismatic_wall','shapechange','time_stop','true_polymorph','weird','wish','gate','foresight','world_sunder'}
draw_block=timeline[timeline.index('switch (id) {'):timeline.index('// 7th circle')]
dispatched=set(re.findall(r'case "([a-z0-9_]+)"',draw_block))
assert len(expected_high_circle)==35
assert dispatched==expected_high_circle, (sorted(expected_high_circle-dispatched), sorted(dispatched-expected_high_circle))
for token in ['spell.circle() < 7','detailBuilder(CHARGE_BUDGET)','detailBuilder(RELEASE_BUDGET)',
              'case "time_stop"','case "power_word_kill"','case "maze"','case "gate"','case "world_sunder"',
              'private static final float MAJOR','private static void squareCorridor']:
    assert token in timeline, token
mesh=text(client/'ArcaneWorldMesh.java')
for token in ['detailBuilder(int budget)','record Segment(Vec3 start,Vec3 end,float width,float brightness,float alpha)',
              'Builder line(Vec3 a,Vec3 b,float width,float brightness,float alpha)',
              'passBrightness*s.brightness','passAlpha*s.alpha']:
    assert token in mesh, token
tracker=text(client/'WorldMagicTracker.java')
for token in ['if(v.spell.circle()>=7)','AuthoredHighCircleTimeline.charge','AuthoredHighCircleTimeline.release',
              'MAX_FRAME = 14500','MAX_ENTRY = 4000']:
    assert token in tracker, token
# The alpha.42 low-circle frame is mandatory; this gate prevents another alpha.40 regression.
sigil=text(client/'ArcaneSigilDirector.java')
for token in ['formulaFrame(mesh, spell, profile','m.circle(basis,Vec3.ZERO,outer','inscriptionRing','schoolFormula']:
    assert token in sigil, token
assert 'ManualSpellVisuals' not in tracker
print('alpha43_high_circle_authored_timeline=PASS')
print('alpha42_low_circle_sigil_baseline=preserved')
'''
if 'alpha43_high_circle_authored_timeline=PASS' not in body:
    body+=append
write(audit,body)

verify=root/'tools/verify_jar.py'
body=read(verify)
needle='    "kr/moonseungjun/arcanecircle/client/WorldMagicTracker.class",\n'
if 'AuthoredHighCircleTimeline.class' not in body:
    if needle not in body: raise RuntimeError('verify_jar WorldMagicTracker marker missing')
    body=body.replace(needle,needle+'    "kr/moonseungjun/arcanecircle/client/AuthoredHighCircleTimeline.class",\n',1)
write(verify,body)

# Keep active tools clean; script/workflow remove themselves after this script returns.
print('alpha.43 VFX migration prepared')
