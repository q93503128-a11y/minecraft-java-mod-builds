package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.ArcaneFieldService;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Runtime bridge between authoritative spell events and the explicit hand-authored visual registry. */
public final class WorldMagicTracker {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "world_magic_manual_v4"));
    private static final Map<UUID, Visual> CHARGES = new HashMap<>();
    private static final List<Visual> RELEASES = new ArrayList<>();
    private static final int MAX_VISUALS = 10;
    private static final int MAX_FRAME = 14000;
    private static final int MAX_ENTRY = 4000;
    private static final double MAX_DISTANCE_SQR = 224.0 * 224.0;
    private static final long CHARGE_TTL = 2_250_000_000L;

    record CasterPoseSnapshot(int family, float progress, boolean release) {}
    private WorldMagicTracker() {}

    static CasterPoseSnapshot castingPose(UUID caster) {
        Visual charge=CHARGES.get(caster);
        if(charge!=null)return new CasterPoseSnapshot(ManualSpellVisuals.castingFamily(charge.spell.id()),
                (float)clamp(charge.progress,0,1),false);
        long now=System.nanoTime();
        for(int i=RELEASES.size()-1;i>=0;i--){
            Visual visual=RELEASES.get(i);
            if(!visual.caster.equals(caster))continue;
            float age=(float)clamp((now-visual.startedAt)/(double)Math.max(1L,visual.expiresAt-visual.startedAt),0,1);
            return new CasterPoseSnapshot(ManualSpellVisuals.castingFamily(visual.spell.id()),age,true);
        }
        return new CasterPoseSnapshot(0,0F,false);
    }

    public static void accept(WorldMagicPayload payload) {
        Map<String,String> values=parse(payload.state());
        String kind=values.getOrDefault("kind","");
        UUID caster;
        try{caster=UUID.fromString(values.getOrDefault("caster",""));}catch(Exception ignored){return;}
        if("stop".equals(kind)){CHARGES.remove(caster);return;}

        SpellDefinition spell=SpellCatalog.spell(values.getOrDefault("spell","")).orElse(null);
        if(spell==null)return;
        boolean fusion=integer(values,"fusion",0)!=0;
        int ingredients=Math.max(0,integer(values,"ingredients",0));
        Vec3 center=new Vec3(decimal(values,"x",0),decimal(values,"y",0),decimal(values,"z",0));
        Vec3 direction=safeDirection(new Vec3(decimal(values,"dx",0),decimal(values,"dy",0),decimal(values,"dz",1)));
        double range=Math.max(.1,decimal(values,"range",spell.range()));
        Vec3 target=new Vec3(decimal(values,"tx",center.x+direction.x*range),
                decimal(values,"ty",center.y+direction.y*range),decimal(values,"tz",center.z+direction.z*range));
        double power=Math.max(.1,decimal(values,"power",Math.max(.1,spell.power())));
        double progress=clamp(decimal(values,"progress",1),0,1);
        int duration=Math.max(3,integer(values,"duration",10));
        if("time_stop".equals(spell.id()))duration=ArcaneFieldService.TIME_STOP_TICKS;
        else if("antimagic_field".equals(spell.id()))duration=ArcaneFieldService.ANTIMAGIC_TICKS;
        int impactTicks=Math.max(0,integer(values,"impact",0));
        long seed=longValue(values,"seed",0L);
        double impactAge=clamp(impactTicks/(double)Math.max(1,duration),.04,.92);
        long now=System.nanoTime();

        if("charge".equals(kind)){
            Visual previous=CHARGES.get(caster);
            long started=previous!=null&&previous.spell.id().equals(spell.id())?previous.startedAt:now;
            CHARGES.put(caster,new Visual(caster,spell,fusion,ingredients,center,target,direction,range,power,
                    progress,started,now+CHARGE_TTL,false,0,seed));
            return;
        }
        if("release".equals(kind)){
            while(RELEASES.size()>=MAX_VISUALS)RELEASES.removeFirst();
            RELEASES.add(new Visual(caster,spell,fusion,ingredients,center,target,direction,range,power,1,
                    now,now+duration*50_000_000L,true,impactAge,seed));
            CHARGES.remove(caster);
        }
    }

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        long now=System.nanoTime();
        CHARGES.values().removeIf(v->v.expiresAt<now);
        RELEASES.removeIf(v->v.expiresAt<now);
        if(CHARGES.isEmpty()&&RELEASES.isEmpty())return;
        List<RenderEntry> entries=new ArrayList<>();

        for(Visual v:CHARGES.values()){
            int color=ManualSpellVisuals.color(v.spell.id());
            ArcaneWorldMesh mesh=ManualSpellVisuals.charge(v.spell,v.direction,targetOffset(v),v.range,v.power,
                    v.progress,v.startedAt,v.seed);
            if(mesh.size()>0)entries.add(new RenderEntry(v.center,mesh,color));
        }

        for(Visual v:RELEASES){
            double age=clamp((now-v.startedAt)/(double)Math.max(1L,v.expiresAt-v.startedAt),0,1);
            double durationSeconds=Math.max(.05,(v.expiresAt-v.startedAt)/1_000_000_000.0);
            double elapsedSeconds=age*durationSeconds;
            int color=ManualSpellVisuals.color(v.spell.id());
            ArcaneWorldMesh mesh=ManualSpellVisuals.release(v.spell,v.direction,targetOffset(v),v.range,v.power,
                    age,elapsedSeconds,durationSeconds,v.seed);
            if(mesh.size()>0)entries.add(new RenderEntry(v.center,mesh,color));

            if("prismatic_wall".equals(v.spell.id())){
                for(int layer=0;layer<7;layer++)entries.add(new RenderEntry(v.center,
                        ManualSpellVisuals.prismaticWallLayer(v.direction,targetOffset(v),v.range,age,elapsedSeconds,layer),
                        ManualSpellVisuals.prismaticColor(layer)));
            } else if("prismatic_spray".equals(v.spell.id())) {
                for(int layer=0;layer<7;layer++)entries.add(new RenderEntry(v.center,
                        ManualSpellVisuals.prismaticSprayLayer(v.direction,v.range,age,layer),
                        ManualSpellVisuals.prismaticColor(layer)));
            }
        }
        event.getRenderState().setRenderData(DATA_KEY,List.copyOf(entries));
    }

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        List<RenderEntry> entries=event.getLevelRenderState().getRenderData(DATA_KEY);
        if(entries==null||entries.isEmpty())return;
        Vec3 camera=event.getLevelRenderState().cameraRenderState.pos;
        float base=Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
        float scale=Math.max(.72F,base*.82F);
        int used=0;
        for(RenderEntry entry:entries){
            if(used>=MAX_FRAME)break;
            int cost=entry.mesh.size();if(cost<=0||cost>MAX_ENTRY||used+cost>MAX_FRAME)continue;
            Vec3 offset=entry.center.subtract(camera);
            if(offset.lengthSqr()>MAX_DISTANCE_SQR)continue;
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(offset.x,offset.y,offset.z);
            entry.mesh.submit(event.getPoseStack(),event.getSubmitNodeCollector(),entry.argb,scale);
            event.getPoseStack().popPose();
            used+=cost;
        }
    }

    private static Vec3 targetOffset(Visual visual){return visual.target.subtract(visual.center);}
    private static Map<String,String> parse(String state){Map<String,String> result=new HashMap<>();for(String part:state.split(";")){int i=part.indexOf('=');if(i>0)result.put(part.substring(0,i),part.substring(i+1));}return result;}
    private static int integer(Map<String,String> values,String key,int fallback){try{return Integer.parseInt(values.getOrDefault(key,Integer.toString(fallback)));}catch(Exception ignored){return fallback;}}
    private static long longValue(Map<String,String> values,String key,long fallback){try{return Long.parseLong(values.getOrDefault(key,Long.toString(fallback)));}catch(Exception ignored){return fallback;}}
    private static double decimal(Map<String,String> values,String key,double fallback){try{return Double.parseDouble(values.getOrDefault(key,Double.toString(fallback)));}catch(Exception ignored){return fallback;}}
    private static Vec3 safeDirection(Vec3 value){return value==null||value.lengthSqr()<1.0E-8?new Vec3(0,0,1):value.normalize();}
    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}

    private record Visual(UUID caster, SpellDefinition spell, boolean fusion, int ingredients,
                          Vec3 center, Vec3 target, Vec3 direction, double range, double power, double progress,
                          long startedAt, long expiresAt, boolean release, double impactAge, long seed) {}
    private record RenderEntry(Vec3 center, ArcaneWorldMesh mesh, int argb) {}
}
