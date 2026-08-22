package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Shared seeded Meteor Swarm timing/offset grammar for authoritative hits and client VFX.
 * alpha.64 changes the old fixed sixteen-body battlefield pattern into a range-scaled cityfall.
 */
public final class MeteorBarragePattern {
    public record Strike(double offsetX, double offsetZ, int impactTick, double scale, double fallHeight) {}
    private static final ThreadLocal<Long> ACTIVE_SEED = new ThreadLocal<>();
    private static final ThreadLocal<Double> ACTIVE_RANGE = new ThreadLocal<>();
    private static final Map<Long, Double> RANGE_BY_SEED = new LinkedHashMap<>();
    private static final int MAX_REMEMBERED_RANGES = 192;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));
    private static final double SAFE_VISUAL_RANGE = 108.0;
    private MeteorBarragePattern() {}

    public static int count() { return count(activeRange()); }
    public static int count(double range) { return NinthCircleMagnitude.meteorStrikeCount(range); }
    public static int crownIndex() { return count() - 1; }
    public static int crownIndex(double range) { return count(range) - 1; }
    public static boolean isCrownStrike(int index) { return index == crownIndex(); }
    public static boolean isCrownStrike(double range, int index) { return index == crownIndex(range); }

    public static List<Strike> strikes() { Long seed=ACTIVE_SEED.get(); return strikes(seed==null?0L:seed,activeRange()); }
    public static List<Strike> strikes(long seed) { return strikes(seed, rememberedRange(seed)); }
    public static List<Strike> strikes(long seed, double range) {
        double effectiveRange=sanitizeRange(range);
        int total=count(effectiveRange), ordinary=total-1;
        double field=NinthCircleMagnitude.meteorFieldRadius(effectiveRange);
        long mixedSeed=(seed==0L?0x6A09E667F3BCC909L:seed)^0x5F3759DF4A7C15L;
        Random random=new Random(mixedSeed);
        double rotation=random.nextDouble()*Math.PI*2.0;
        List<Strike> result=new ArrayList<>(total);
        int previousTick=0;
        double minSeparation=Math.max(5.4,field/Math.sqrt(Math.max(1.0,ordinary))*.42);
        for(int index=0;index<ordinary;index++){
            double q=(index+.62)/Math.max(1.0,ordinary);
            double radialFraction=.10+.86*Math.sqrt(q), radial=field*radialFraction;
            double baseAngle=rotation+index*GOLDEN_ANGLE;
            double angle=baseAngle+(random.nextDouble()-.5)*.16;
            double jitteredRadius=radial*(.94+random.nextDouble()*.12);
            double x=Math.cos(angle)*jitteredRadius,z=Math.sin(angle)*jitteredRadius;
            if(tooClose(result,x,z,minSeparation)){x=Math.cos(baseAngle)*radial;z=Math.sin(baseAngle)*radial;}
            double variation=random.nextDouble();
            double scale=NinthCircleMagnitude.meteorOrdinaryScale(effectiveRange,radialFraction,variation);
            if(index%11==0)scale=Math.min(4.20,scale*1.18);
            double fallHeight=NinthCircleMagnitude.meteorFallHeight(effectiveRange,radialFraction,random.nextDouble());
            int tick=24+index*2+index/8+random.nextInt(2);
            tick=Math.max(index==0?22:previousTick+1,tick);
            previousTick=tick;
            result.add(new Strike(x,z,tick,scale,fallHeight));
        }
        int crownTick=previousTick+14;
        result.add(new Strike(0.0,0.0,crownTick,NinthCircleMagnitude.crownScale(effectiveRange),NinthCircleMagnitude.crownFallHeight(effectiveRange)));
        return List.copyOf(result);
    }

    public static Strike strike(int index){List<Strike>s=strikes();return s.get(Math.max(0,Math.min(s.size()-1,index)));}
    public static Strike strike(long seed,int index){List<Strike>s=strikes(seed);return s.get(Math.max(0,Math.min(s.size()-1,index)));}
    public static Strike strike(long seed,double range,int index){List<Strike>s=strikes(seed,range);return s.get(Math.max(0,Math.min(s.size()-1,index)));}
    public static int firstImpactTick(){return strikes().getFirst().impactTick();}
    public static int firstImpactTick(long seed){return strikes(seed).getFirst().impactTick();}
    public static int firstImpactTick(long seed,double range){return strikes(seed,range).getFirst().impactTick();}
    public static int lastImpactTick(){return strikes().getLast().impactTick();}
    public static int lastImpactTick(long seed){return strikes(seed).getLast().impactTick();}
    public static int lastImpactTick(long seed,double range){return strikes(seed,range).getLast().impactTick();}
    public static int durationTicks(){return lastImpactTick()+20;}

    /** WorldMagicService can ask before the scheduler stores equipment-expanded range. */
    public static int durationTicks(long seed){return lastImpactTick(seed,Math.max(rememberedRange(seed),SAFE_VISUAL_RANGE))+20;}
    public static int durationTicks(long seed,double range){return lastImpactTick(seed,range)+20;}
    public static Vec3 position(Vec3 center,Strike strike){return center.add(strike.offsetX(),0.0,strike.offsetZ());}

    public static synchronized void rememberRange(long seed,double range){
        if(seed==0L)return;
        RANGE_BY_SEED.put(seed,sanitizeRange(range));
        while(RANGE_BY_SEED.size()>MAX_REMEMBERED_RANGES){Long first=RANGE_BY_SEED.keySet().iterator().next();RANGE_BY_SEED.remove(first);}
    }

    public static void rememberPayload(String state){
        if(state==null||!state.contains("spell=meteor_swarm"))return;
        long seed=0L;double range=NinthCircleMagnitude.BASE_METEOR_CAST_RANGE;
        for(String part:state.split(";")){
            int split=part.indexOf('=');if(split<=0)continue;
            String key=part.substring(0,split),value=part.substring(split+1);
            try{if("seed".equals(key))seed=Long.parseLong(value);else if("range".equals(key))range=Double.parseDouble(value);}catch(NumberFormatException ignored){}
        }
        rememberRange(seed,range);
    }

    public static synchronized double rememberedRange(long seed){if(seed==0L)return activeRange();return RANGE_BY_SEED.getOrDefault(seed,NinthCircleMagnitude.BASE_METEOR_CAST_RANGE);}
    public static double activeRange(){Double value=ACTIVE_RANGE.get();return value==null?NinthCircleMagnitude.BASE_METEOR_CAST_RANGE:sanitizeRange(value);}
    public static <T>T withSeed(long seed,Supplier<T>action){return withContext(seed,rememberedRange(seed),action);}
    public static <T>T withContext(long seed,double range,Supplier<T>action){
        Long previousSeed=ACTIVE_SEED.get();Double previousRange=ACTIVE_RANGE.get();
        ACTIVE_SEED.set(seed);ACTIVE_RANGE.set(sanitizeRange(range));
        try{return action.get();}finally{if(previousSeed==null)ACTIVE_SEED.remove();else ACTIVE_SEED.set(previousSeed);if(previousRange==null)ACTIVE_RANGE.remove();else ACTIVE_RANGE.set(previousRange);}
    }

    public static long castSeed(UUID caster,long gameTime){
        long value=caster.getMostSignificantBits()^Long.rotateLeft(caster.getLeastSignificantBits(),19)^gameTime*0x9E3779B97F4A7C15L;
        value^=value>>>30;value*=0xBF58476D1CE4E5B9L;value^=value>>>27;value*=0x94D049BB133111EBL;value^=value>>>31;
        return value==0L?0x6A09E667F3BCC909L:value;
    }
    private static boolean tooClose(List<Strike>existing,double x,double z,double minimum){double minimumSq=minimum*minimum;for(Strike strike:existing){double dx=strike.offsetX()-x,dz=strike.offsetZ()-z;if(dx*dx+dz*dz<minimumSq)return true;}return false;}
    private static double sanitizeRange(double range){return Double.isFinite(range)&&range>0.0?range:NinthCircleMagnitude.BASE_METEOR_CAST_RANGE;}
}
