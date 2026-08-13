from pathlib import Path
import re

repo=Path(__file__).resolve().parents[2]
root=repo/'projects/arcane-circle'
java=root/'src/main/java/kr/moonseungjun/arcanecircle'
magic=java/'magic'
client=java/'client'

def read(p): return p.read_text(encoding='utf-8')
def write(p,s): p.write_text(s,encoding='utf-8')
def replace_once(p,old,new):
    s=read(p)
    if s.count(old)!=1: raise SystemExit(f'{p}: expected one occurrence, found {s.count(old)}: {old[:80]!r}')
    write(p,s.replace(old,new,1))
def sub_once(p,pattern,repl,flags=re.S):
    s=read(p); n, count=re.subn(pattern,repl,s,count=1,flags=flags)
    if count!=1: raise SystemExit(f'{p}: regex replacement count={count}: {pattern[:100]!r}')
    write(p,n)

# Version bump.
replace_once(root/'gradle.properties','mod_version=0.12.1-alpha.31','mod_version=0.12.1-alpha.32')
replace_once(java/'ArcaneCircle.java','VERSION = "0.12.1-alpha.31"','VERSION = "0.12.1-alpha.32"')
replace_once(root/'src/main/resources/data/arcanecircle/spell_catalog/index.json','"version": "0.12.1-alpha.31"','"version": "0.12.1-alpha.32"')

# Shared deterministic barrage grammar. Server damage and client VFX read this exact table.
write(magic/'MeteorBarragePattern.java',r'''package kr.moonseungjun.arcanecircle.magic;

/** Shared deterministic Meteor Swarm timing/offset grammar for authoritative hits and client VFX. */
public final class MeteorBarragePattern {
    public record Strike(double offsetX, double offsetZ, int impactTick, double scale, double fallHeight) {}

    private static final Strike[] STRIKES = {
            new Strike(-3.2, -1.1, 18, .72, 29), new Strike(4.5, 2.7, 21, .78, 32),
            new Strike(-7.4, 5.4, 24, .68, 28), new Strike(1.9, -6.6, 27, .86, 35),
            new Strike(8.6, -3.0, 30, .74, 31), new Strike(-10.2, -4.9, 33, .92, 38),
            new Strike(6.7, 8.7, 36, .82, 35), new Strike(-2.7, 10.5, 39, .70, 30),
            new Strike(11.1, 4.1, 42, 1.02, 41), new Strike(-8.7, .9, 45, .76, 33),
            new Strike(3.3, 7.2, 48, .88, 36), new Strike(.1, -2.3, 51, 1.12, 43),
            new Strike(-5.6, -9.2, 55, .84, 34), new Strike(9.5, -8.1, 59, .94, 39),
            new Strike(-11.4, 8.2, 63, .80, 33), new Strike(5.2, -.8, 68, 1.18, 44)
    };

    private MeteorBarragePattern() {}
    public static int count() { return STRIKES.length; }
    public static Strike strike(int index) { return STRIKES[Math.max(0, Math.min(STRIKES.length - 1, index))]; }
    public static int firstImpactTick() { return STRIKES[0].impactTick(); }
    public static int lastImpactTick() { return STRIKES[STRIKES.length - 1].impactTick(); }
    public static int durationTicks() { return lastImpactTick() + 12; }
}
''')

# Material-strength-aware, bounded terrain destruction.
write(magic/'DestructiveMagicService.java',r'''package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Server-authoritative terrain rupture for spells whose fiction is explicitly destructive.
 * Weak materials fail farther from the impact; hard/blast-resistant materials require a much
 * stronger local impulse. Unbreakable blocks and block entities are never removed. Every call is
 * bounded so a cinematic spell cannot turn into an unbounded world-edit or item-entity storm.
 */
public final class DestructiveMagicService {
    private record Candidate(BlockPos pos, double overload) {}
    private record Profile(double radiusScale, double baseEnergy, int maxBlocks, boolean drops) {}

    private DestructiveMagicService() {}

    public static int impact(ServerPlayer player, String spellId, Vec3 center,
                             double requestedRadius, double power) {
        Profile profile=profile(spellId);
        if(profile==null||center==null)return 0;
        ServerLevel level=(ServerLevel)player.level();
        double radius=Math.max(.75,Math.min(10.5,requestedRadius*profile.radiusScale()));
        double energy=profile.baseEnergy()*(.84+.16*Math.sqrt(Math.max(.1,power)));
        int bound=(int)Math.ceil(radius);
        List<Candidate> candidates=new ArrayList<>();
        BlockPos origin=BlockPos.containing(center);
        double vertical=Math.max(1.25,radius*.62);
        for(int x=-bound;x<=bound;x++)for(int z=-bound;z<=bound;z++)for(int y=-(int)Math.ceil(vertical);y<=Math.ceil(vertical);y++){
            double nx=x/radius,nz=z/radius,ny=y/vertical;
            double normalized=Math.sqrt(nx*nx+nz*nz+ny*ny);
            if(normalized>1.0)continue;
            BlockPos pos=origin.offset(x,y,z);
            BlockState state=level.getBlockState(pos);
            if(state.isAir()||!state.getFluidState().isEmpty()||state.hasBlockEntity())continue;
            float hardness=state.getDestroySpeed(level,pos);
            if(hardness<0)continue;
            float blast=Math.max(0F,state.getBlock().getExplosionResistance());
            if(blast>=1000F)continue;
            double strength=1.0+Math.max(0,hardness)*2.6+Math.sqrt(blast)*1.45;
            double falloff=Math.pow(Math.max(0.0,1.0-normalized),.78);
            double local=energy*(.20+.80*falloff);
            if(local<strength)continue;
            candidates.add(new Candidate(pos,local/Math.max(.25,strength)));
        }
        candidates.sort(Comparator.comparingDouble(Candidate::overload).reversed());
        int changed=0;
        for(Candidate candidate:candidates){
            if(changed>=profile.maxBlocks())break;
            if(level.destroyBlock(candidate.pos(),profile.drops(),player))changed++;
        }
        return changed;
    }

    public static int ray(ServerPlayer player, String spellId, Vec3 start, Vec3 end, double power) {
        if(start==null||end==null)return 0;
        Vec3 delta=end.subtract(start); double length=delta.length();
        if(length<.05)return 0;
        Vec3 unit=delta.scale(1.0/length); int changed=0;
        int samples=Math.min(72,Math.max(1,(int)Math.ceil(length/.75)));
        for(int i=1;i<=samples;i++){
            Vec3 at=start.add(unit.scale(length*i/(double)samples));
            changed+=impact(player,spellId,at,1.15,power);
            if(changed>=72)break;
        }
        return changed;
    }

    private static Profile profile(String id){
        return switch(id){
            case "fireball" -> new Profile(.72,7.6,72,false);
            case "shatter" -> new Profile(.74,9.0,88,true);
            case "flame_strike" -> new Profile(.62,10.5,96,false);
            case "meteor_shard" -> new Profile(.88,12.5,112,false);
            case "disintegrate" -> new Profile(.78,24.0,18,false);
            case "delayed_blast_fireball" -> new Profile(.92,15.5,150,false);
            case "fire_storm" -> new Profile(.66,11.5,52,false);
            case "move_earth" -> new Profile(.54,11.0,170,true);
            case "earthquake" -> new Profile(.58,14.5,240,true);
            case "meteor_swarm" -> new Profile(.92,16.5,34,false);
            case "world_sunder" -> new Profile(.62,28.0,320,true);
            case "arcane_annihilation" -> new Profile(.70,22.0,48,false);
            default -> null;
        };
    }
}
''')

# Lock target for the duration of a multi-hit authoritative barrage.
p=magic/'WorldMagicService.java'
replace_once(p,
'''    public static double kineticDistance(ServerPlayer player, SpellDefinition spell, double range) {
        Vec3 direction = safeDirection(player.getLookAngle());
        Vec3 target = targetPoint(player, spell, range, direction);
''',
'''    static Vec3 lockedTarget(ServerPlayer player, SpellDefinition spell, double range) {
        Vec3 direction = safeDirection(player.getLookAngle());
        return targetPoint(player, spell, range, direction);
    }

    public static double kineticDistance(ServerPlayer player, SpellDefinition spell, double range) {
        Vec3 direction = safeDirection(player.getLookAngle());
        Vec3 target = targetPoint(player, spell, range, direction);
''')

# Meteor is no longer one PendingCast execution. It owns 16 staggered authoritative impacts.
p=magic/'SpellKineticsService.java'
replace_once(p,'import net.minecraft.server.level.ServerPlayer;\n','import net.minecraft.server.level.ServerPlayer;\nimport net.minecraft.world.phys.Vec3;\n')
replace_once(p,
'''        WorldMagicService.release(player, cast);

        int presentationImpactDelay = SpellPresentationProfile.impactDelayTicks(cast.spell(),
''',
'''        WorldMagicService.release(player, cast);

        if ("meteor_swarm".equals(cast.spell().id())) {
            Vec3 lockedTarget=WorldMagicService.lockedTarget(player,cast.spell(),cast.range());
            MeteorBarragePattern.Strike first=MeteorBarragePattern.strike(0);
            enqueue(player,new PendingCast(cast,snapshot,clock(player)+first.impactTick(),0,
                    MeteorBarragePattern.count(),cast.power(),false,lockedTarget,0));
            ArcaneNoticeService.push(player,Component.literal("§6[운석 폭격] §f"+MeteorBarragePattern.count()+"발 연속 낙하"),75);
            return true;
        }

        int presentationImpactDelay = SpellPresentationProfile.impactDelayTicks(cast.spell(),
''')
replace_once(p,
'''            boolean executed = SpellCastingService.executeResolved(player, pending.cast().spell().id(),
                    pending.cast().range(), pending.pulsePower());
            int remaining = pending.remainingPulses() - 1;
            boolean any = pending.anyExecuted() || executed;
            if (remaining <= 0) {
                iterator.remove();
                SpellCastingService.finishKineticCast(player, pending.cast(), pending.snapshot(), any);
            } else {
                pending.advance(now + pending.interval(), remaining, any);
            }
''',
'''            boolean meteor="meteor_swarm".equals(pending.cast().spell().id())&&pending.lockedTarget()!=null;
            boolean executed=meteor
                    ? HighCircleSpellEffects.meteorImpact(player,pending.lockedTarget(),pending.cast().range(),pending.pulsePower(),pending.pulseIndex())
                    : SpellCastingService.executeResolved(player,pending.cast().spell().id(),pending.cast().range(),pending.pulsePower());
            int remaining=pending.remainingPulses()-1;
            boolean any=pending.anyExecuted()||executed;
            if(remaining<=0){
                iterator.remove();
                SpellCastingService.finishKineticCast(player,pending.cast(),pending.snapshot(),any);
            }else if(meteor){
                int nextIndex=pending.pulseIndex()+1;
                int gap=Math.max(1,MeteorBarragePattern.strike(nextIndex).impactTick()
                        -MeteorBarragePattern.strike(pending.pulseIndex()).impactTick());
                pending.advanceMeteor(now+gap,remaining,any,nextIndex);
            }else{
                pending.advance(now+pending.interval(),remaining,any);
            }
''')
replace_once(p,
'''        private final double pulsePower;
        private boolean anyExecuted;

        private PendingCast(MagicPlayerData.CastPreparation cast,
''',
'''        private final double pulsePower;
        private boolean anyExecuted;
        private final Vec3 lockedTarget;
        private int pulseIndex;

        private PendingCast(MagicPlayerData.CastPreparation cast,
''')
replace_once(p,
'''                            double pulsePower,
                            boolean anyExecuted) {
            this.cast = cast;
            this.snapshot = snapshot;
            this.nextTick = nextTick;
            this.interval = interval;
            this.remainingPulses = remainingPulses;
            this.pulsePower = pulsePower;
            this.anyExecuted = anyExecuted;
        }
''',
'''                            double pulsePower,
                            boolean anyExecuted) {
            this(cast,snapshot,nextTick,interval,remainingPulses,pulsePower,anyExecuted,null,0);
        }

        private PendingCast(MagicPlayerData.CastPreparation cast,
                            CombatGrowthService.Snapshot snapshot,
                            long nextTick, int interval, int remainingPulses,
                            double pulsePower, boolean anyExecuted, Vec3 lockedTarget, int pulseIndex) {
            this.cast=cast; this.snapshot=snapshot; this.nextTick=nextTick; this.interval=interval;
            this.remainingPulses=remainingPulses; this.pulsePower=pulsePower; this.anyExecuted=anyExecuted;
            this.lockedTarget=lockedTarget; this.pulseIndex=pulseIndex;
        }
''')
replace_once(p,
'''        boolean anyExecuted() { return anyExecuted; }

        void advance(long next, int remaining, boolean executed) {
''',
'''        boolean anyExecuted() { return anyExecuted; }
        Vec3 lockedTarget() { return lockedTarget; }
        int pulseIndex() { return pulseIndex; }

        void advance(long next, int remaining, boolean executed) {
''')
replace_once(p,
'''            this.anyExecuted = executed;
        }
''',
'''            this.anyExecuted = executed;
        }

        void advanceMeteor(long next,int remaining,boolean executed,int index){
            this.nextTick=next; this.remainingPulses=remaining; this.anyExecuted=executed; this.pulseIndex=index;
        }
''')

# Presentation timing and satellites now describe a barrage, not a simultaneous quartet.
p=magic/'SpellPresentationProfile.java'
replace_once(p,
'put("meteor_swarm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 18.00, 6, 4, 0, 30, 2.60, 30);',
'put("meteor_swarm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 18.00, 6, 12, 0, 34, 2.60, MeteorBarragePattern.firstImpactTick());')
replace_once(p,
'''    public static int releaseDurationTicks(SpellDefinition spell, double distance) {
        Profile profile = profile(spell);
''',
'''    public static int releaseDurationTicks(SpellDefinition spell, double distance) {
        if("meteor_swarm".equals(spell.id()))return MeteorBarragePattern.durationTicks();
        Profile profile = profile(spell);
''')

# High-circle effects: one impact API shared by kinetics; selected destructive spells now rupture terrain.
p=magic/'HighCircleSpellEffects.java'
replace_once(p,
'''    private static boolean meteorSwarm(ServerPlayer player, double range, double power) {
        Vec3 center = aim(player, range);
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * 2.0 * i / 4.0 + Math.PI / 4.0;
            Vec3 impact = center.add(Math.cos(angle) * 10.0, 0, Math.sin(angle) * 10.0);
            Vec3 sky = impact.add(0, 42, 0);
            line(level(player), sky, impact, ParticleTypes.FLAME, 100);
            blastAt(player, impact, power, 11.0, ParticleTypes.FLAME, true);
        }
        return true;
    }
''',
'''    private static boolean meteorSwarm(ServerPlayer player, double range, double power) {
        Vec3 center=aim(player,range); boolean any=false;
        for(int i=0;i<MeteorBarragePattern.count();i++)any|=meteorImpact(player,center,range,power,i);
        return any;
    }

    static boolean meteorImpact(ServerPlayer player, Vec3 center, double range, double power, int index) {
        MeteorBarragePattern.Strike strike=MeteorBarragePattern.strike(index);
        Vec3 impact=center.add(strike.offsetX(),0,strike.offsetZ());
        double radius=3.0+strike.scale()*1.65;
        double strikePower=power*(.19+.075*strike.scale());
        blastAt(player,impact,strikePower,radius,ParticleTypes.FLAME,true);
        DestructiveMagicService.impact(player,"meteor_swarm",impact,radius,power*strike.scale());
        level(player).playSound(null,BlockPos.containing(impact),SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS,Math.min(1.6F,.82F+(float)strike.scale()*.38F),.62F+(index%4)*.055F);
        return true;
    }
''')
replace_once(p,
'''        line(level, start, end, particle, Math.max(48, (int) range * 3));
        List<Mob> hits = lineTargets(player, start, end, 1.6);
''',
'''        line(level, start, end, particle, Math.max(48, (int) range * 3));
        if(lethal)DestructiveMagicService.ray(player,"disintegrate",start,end,power);
        List<Mob> hits = lineTargets(player, start, end, 1.6);
''')
replace_once(p,
'''        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.55F);
        return true;
    }
''',
'''        DestructiveMagicService.impact(player,huge?"earthquake":"move_earth",center,r,power);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2F, 0.55F);
        return true;
    }
''')
replace_once(p,
'''    private static boolean multiBlast(ServerPlayer player, double range, double power,
                                      ParticleOptions particle, int count, double radius) {
        Vec3 center = aim(player, range);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            Vec3 at = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            blastAt(player, at, power, radius * 0.75, particle, particle == ParticleTypes.FLAME);
        }
        return true;
    }
''',
'''    private static boolean multiBlast(ServerPlayer player, double range, double power,
                                      ParticleOptions particle, int count, double radius) {
        Vec3 center = aim(player, range);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            Vec3 at = center.add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            blastAt(player, at, power, radius * 0.75, particle, particle == ParticleTypes.FLAME);
            if(particle==ParticleTypes.FLAME)DestructiveMagicService.impact(player,"fire_storm",at,radius*.75,power*.72);
        }
        return true;
    }
''')
# Delayed blast is the fire blast in this shared helper call site; wrap only its switch arm.
replace_once(p,
'case "delayed_blast_fireball" -> blast(player, range, power, 10.0, ParticleTypes.FLAME, 280, false);',
'case "delayed_blast_fireball" -> destructiveBlast(player,"delayed_blast_fireball",range,power,10.0,ParticleTypes.FLAME,280,false);')
insert='''\n    private static boolean destructiveBlast(ServerPlayer player,String id,double range,double power,double radius,\n                                            ParticleOptions particle,int fireTicks,boolean freeze){\n        Vec3 center=aim(player,range);\n        boolean result=blast(player,range,power,radius,particle,fireTicks,freeze);\n        DestructiveMagicService.impact(player,id,center,radius*Math.max(1.0,Math.sqrt(range/25.0)),power);\n        return result;\n    }\n'''
replace_once(p,'\n    private static boolean curseTarget(ServerPlayer player, double range, double power, int duration, boolean fear) {',insert+'\n    private static boolean curseTarget(ServerPlayer player, double range, double power, int duration, boolean fear) {')

# Lower/mid destructive effects.
p=magic/'SpellCastingService.java'
replace_once(p,
'''        Vec3 center = lookTarget(player, range).map(Mob::position).orElse(aimGround(player, range));
        return areaAt(player, center, SpellMetrics.effectRadius("fireball", range, 3), power, particle, fire, freeze);
''',
'''        Vec3 center = lookTarget(player, range).map(Mob::position).orElse(aimGround(player, range));
        double radius=SpellMetrics.effectRadius("fireball", range, 3);
        boolean result=areaAt(player,center,radius,power,particle,fire,freeze);
        DestructiveMagicService.impact(player,"fireball",center,radius,power);
        return result;
''')
replace_once(p,
'''        areaAt(player, center, 5.0, power * 1.15, ParticleTypes.FLAME, true, false);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
''',
'''        areaAt(player, center, 5.0, power * 1.15, ParticleTypes.FLAME, true, false);
        DestructiveMagicService.impact(player,"meteor_shard",center,5.0,power);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
''')
# Arcane annihilation cuts a narrow destructive corridor along the same aim line.
sub_once(p,r'(private static boolean arcaneAnnihilation\(ServerPlayer player, double range, double power\) \{\n        ServerLevel level = \(ServerLevel\) player\.level\(\);)',r'\1\n        Vec3 start=player.getEyePosition(); Vec3 end=start.add(player.getLookAngle().normalize().scale(range));\n        DestructiveMagicService.ray(player,"arcane_annihilation",start,end,power);')

p=magic/'ExpandedSpellEffects.java'
replace_once(p,
'''            case "shatter" -> areaDamage(player, aimGround(player, range), SpellMetrics.effectRadius("shatter", range, 2), power,
                    ParticleTypes.CRIT, false, false, false);
''',
'''            case "shatter" -> shatter(player,range,power);
''')
insert='''\n    private static boolean shatter(ServerPlayer player,double range,double power){\n        Vec3 center=aimGround(player,range); double radius=SpellMetrics.effectRadius("shatter",range,2);\n        boolean result=areaDamage(player,center,radius,power,ParticleTypes.CRIT,false,false,false);\n        DestructiveMagicService.impact(player,"shatter",center,radius,power); return result;\n    }\n'''
replace_once(p,'\n    private static boolean ward(ServerPlayer player, double power, int duration, int baseAmplifier) {',insert+'\n    private static boolean ward(ServerPlayer player, double power, int duration, int baseAmplifier) {')
# Insert terrain damage near flame strike return without changing its visual beam structure.
sub_once(p,r'(private static boolean flameStrike\(ServerPlayer player, double range, double power\) \{.*?)(\n        return areaDamage\(player, center,)(.*?\n    \})',r'\1\n        DestructiveMagicService.impact(player,"flame_strike",center,SpellMetrics.effectRadius("flame_strike",range,5),power);\2\3')

p=magic/'FusionSpellEffects.java'
replace_once(p,
'''        sound(level, player, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.52F);
        return true;
    }
''',
'''        DestructiveMagicService.impact(player,"world_sunder",player.position(),radius,power);
        sound(level, player, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.52F);
        return true;
    }
''')

# Client meteor VFX consumes the same 16-strike table and only renders each local fall/impact window.
p=client/'SpellCinematicDirector.java'
replace_once(p,'import kr.moonseungjun.arcanecircle.magic.SpellDefinition;\n','import kr.moonseungjun.arcanecircle.magic.MeteorBarragePattern;\nimport kr.moonseungjun.arcanecircle.magic.SpellDefinition;\n')
sub_once(p,r'    private static void meteorSwarm\(ArcaneWorldMesh\.Builder m,Vec3 target,double age,double impact,double scale\)\{.*?\n    \}\n\n    private static void executionWord',r'''    private static void meteorSwarm(ArcaneWorldMesh.Builder m,Vec3 target,double age,double impact,double scale){
        ArcaneWorldMesh.Basis down=ArcaneWorldMesh.Basis.facing(new Vec3(0,-1,0));
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();
        double nowTicks=age*MeteorBarragePattern.durationTicks();
        for(int i=0;i<MeteorBarragePattern.count();i++){
            MeteorBarragePattern.Strike s=MeteorBarragePattern.strike(i);
            double local=(nowTicks-(s.impactTick()-8.0))/8.0;
            if(local<0.0||local>1.55)continue;
            Vec3 hit=target.add(s.offsetX(),0,s.offsetZ());
            double fall=clamp(local,0,1),meteorScale=scale*(.42+.30*s.scale());
            Vec3 pos=hit.add(0,s.fallHeight()*(1-easeIn(fall)),0);
            if(local<=1.0){
                m.orb(pos,.42*meteorScale,18,1.24F,.48F);
                m.shard(pos.add(0,.55*meteorScale,0),new Vec3(0,-1,0),down,1.85*meteorScale,.24*meteorScale,1.20F,.38F);
                Vec3 tail=pos.add(0,2.4+meteorScale*1.2,0); m.line(tail,pos,.68F);
            }else{
                double impactAge=(local-1.0)*.44;
                impactRing(m,g,hit,meteorScale*(1.05+s.scale()*.45),impactAge);
                for(int q=0;q<5;q++){double a=q*Math.PI*2/5.0+i*.73;m.line(hit.add(g.point(a,.18)),hit.add(g.point(a,meteorScale*(.8+impactAge*2.2))),.42F);}
            }
        }
    }

    private static void executionWord''')
replace_once(p,
'case "flame_strike","fire_storm","meteor_swarm","delayed_blast_fireball" -> sig(Form.SKY,id.equals("meteor_swarm")?2.3:1.2,6,id.equals("meteor_swarm")?4:id.equals("fire_storm")?6:1,id.equals("meteor_swarm")?10:3,id.equals("meteor_swarm")?42:14,1.9);',
'case "flame_strike","fire_storm","meteor_swarm","delayed_blast_fireball" -> sig(Form.SKY,id.equals("meteor_swarm")?2.3:1.2,6,id.equals("meteor_swarm")?12:id.equals("fire_storm")?6:1,id.equals("meteor_swarm")?12:3,id.equals("meteor_swarm")?34:14,1.9);')

# Meteor ritual no longer advertises four fixed drop seals: use twelve small bombardment coordinates.
p=client/'ArcaneSigilDirector.java'
replace_once(p,'import kr.moonseungjun.arcanecircle.magic.SpellDefinition;\n','import kr.moonseungjun.arcanecircle.magic.MeteorBarragePattern;\nimport kr.moonseungjun.arcanecircle.magic.SpellDefinition;\n')
sub_once(p,r'        // Four major drop seals are readable sub-circles rather than giant cubes/rails\.\n        double orbit=outer\*\.695,child=outer\*\.102;\n        for\(int i=0;i<4;i\+\+\)\{.*?\n        \}\n        // Minor zodiac/coordinate marks',r'''        // Bombardment coordinates: many small seals, not four fixed cardinal drop points.
        double child=outer*.052;
        for(int i=0;i<12;i++){
            MeteorBarragePattern.Strike strike=MeteorBarragePattern.strike(i);
            Vec3 raw=new Vec3(strike.offsetX(),0,strike.offsetZ()); double len=Math.max(1.0,raw.length());
            Vec3 c=b.right().scale(raw.x/len*outer*.70).add(b.up().scale(raw.z/len*outer*.70));
            double sr=child*(.82+.28*strike.scale());
            m.circle(b,c,sr,18,.42F); m.polygon(b,c,sr*.60,3+i%4,-rotation*.035+i,.34F);
            m.runeGlyph(b,c,sr*.30,seed+i*101,-rotation*.045,.30F);
            m.line(c,b.point(Math.atan2(strike.offsetZ(),strike.offsetX()),inner*.68),.24F);
        }
        // Minor zodiac/coordinate marks''')

# Source audit: prohibit fixed quartet and enforce new destruction/barrage contracts.
p=root/'tools/test_current_source.py'
s=read(p).replace('0.12.1-alpha.31','0.12.1-alpha.32')
s=s.replace("assert 'double[][] o={{-10,-10},{10,-10},{-10,10},{10,10}}' in director\nassert 'fallHeight=42.0' in director and 'prismaticWallFrame' in director",
'''assert 'MeteorBarragePattern.count()' in director and 's.impactTick()' in director
assert 'double[][] o={{-10,-10},{10,-10},{-10,10},{10,10}}' not in director
assert 'fallHeight=42.0' not in director and 'prismaticWallFrame' in director''')
anchor="assert '!\"prismatic_wall\".equals(v.spell.id())' in tracker\n"
extra='''assert (magic/'MeteorBarragePattern.java').exists() and (magic/'DestructiveMagicService.java').exists()\nbarrage=text(magic/'MeteorBarragePattern.java')\nfor token in ['STRIKES','impactTick','durationTicks','count()']:\n    assert token in barrage, f'meteor barrage regression: {token}'\nassert barrage.count('new Strike(') >= 16\ndestruction=text(magic/'DestructiveMagicService.java')\nfor token in ['getDestroySpeed','getExplosionResistance','destroyBlock','maxBlocks','hasBlockEntity','world_sunder','meteor_swarm','disintegrate']:\n    assert token in destruction, f'destructive magic regression: {token}'\nkinetics=text(magic/'SpellKineticsService.java')\nfor token in ['lockedTarget','meteorImpact','advanceMeteor','MeteorBarragePattern.count()']:\n    assert token in kinetics, f'meteor authoritative timing regression: {token}'\nassert 'DestructiveMagicService.impact(player,\"meteor_swarm\"' in text(magic/'HighCircleSpellEffects.java')\nassert 'DestructiveMagicService.impact(player,\"world_sunder\"' in text(magic/'FusionSpellEffects.java')\n'''
if anchor not in s: raise SystemExit('test audit insertion anchor missing')
s=s.replace(anchor,anchor+extra,1)
write(p,s)

# Docs: concise alpha.32 contract and audit findings.
ch=root/'CHANGELOG.md'; old=read(ch)
write(ch,'## 0.12.1-alpha.32\n- Destructive spells now rupture blocks using hardness + blast resistance with bounded per-impact budgets; unbreakable blocks and block entities remain protected.\n- Meteor Swarm is a shared 16-strike staggered barrage: authoritative server impacts, terrain damage and client VFX consume one deterministic pattern.\n- Replaced the four simultaneous fixed meteors and updated the ritual to show many bombardment coordinates.\n- Audited kinetic timing, world-mutation caps, fixed-quartet regression, JAR verification and source contracts.\n\n'+old)
proj=root/'PROJECT.md'; ps=read(proj)
ps=ps.replace('## Alpha.31 runtime contracts','## Alpha.32 runtime contracts',1)
ps += '\n- Destructive terrain mutation is server-authoritative and strength-aware (`getDestroySpeed` + explosion resistance), bounded per impact, and never removes unbreakable blocks or block entities.\n- Meteor Swarm uses `MeteorBarragePattern` as the sole server/client strike schedule; fixed simultaneous quartet layouts are forbidden.\n'
write(proj,ps)
readme=root/'README.md'; rs=read(readme)
rs=rs.replace('0.12.1-alpha.31','0.12.1-alpha.32')
write(readme,rs)

# Remove maintenance scaffolding from the resulting source commit.
for name in [
    repo/'.github/scripts/arcane_alpha32_destruction_barrage_20260813.py',
    repo/'.github/workflows/maintenance-arcane32-destruction-barrage-20260813.yml']:
    if name.exists(): name.unlink()

print('Arcane Circle alpha.32 destructive magic + meteor barrage patch applied')
