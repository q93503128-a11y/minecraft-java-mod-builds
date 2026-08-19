package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.ArcaneFieldService;
import kr.moonseungjun.arcanecircle.magic.MeteorBarragePattern;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;
import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Runtime bridge between server-authoritative spell events and the cinematic director. */
public final class WorldMagicTracker {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "world_magic_cinematic_v4"));
    private static final Map<UUID, Visual> CHARGES = new HashMap<>();
    private static final List<Visual> RELEASES = new ArrayList<>();
    private static final int MAX_VISUALS = 32;
    private static final int MAX_FRAME = 14500;
    private static final int MAX_ENTRY = 4000;
    private static final double MAX_DISTANCE_SQR = 224.0 * 224.0;
    private static final double DETAIL_DISTANCE_SQR = 96.0 * 96.0;
    private static final double SILHOUETTE_DISTANCE_SQR = 160.0 * 160.0;
    private static final long CHARGE_TTL = 2_250_000_000L;
    private static Object LAST_LEVEL;

    record CasterPoseSnapshot(int family, float progress, boolean release) {}
    private WorldMagicTracker() {}

    static CasterPoseSnapshot castingPose(UUID caster) {
        Visual charge=CHARGES.get(caster);
        if(charge!=null)return new CasterPoseSnapshot(SpellCinematicDirector.castingFamily(charge.spell),
                (float)clamp(charge.progress,0,1),false);
        long now=System.nanoTime();
        for(int i=RELEASES.size()-1;i>=0;i--){
            Visual visual=RELEASES.get(i);
            if(!visual.caster.equals(caster))continue;
            float age=(float)clamp((now-visual.startedAt)/(double)Math.max(1L,visual.expiresAt-visual.startedAt),0,1);
            return new CasterPoseSnapshot(SpellCinematicDirector.castingFamily(visual.spell),age,true);
        }
        return new CasterPoseSnapshot(0,0F,false);
    }

    public static void accept(WorldMagicPayload payload) {
        syncLevelIdentity();
        Map<String,String> values=parse(payload.state());
        String kind=values.getOrDefault("kind","");
        UUID caster;
        try{caster=UUID.fromString(values.getOrDefault("caster",""));}catch(Exception ignored){return;}
        if("stop".equals(kind)){CHARGES.remove(caster);return;}
        if("clear".equals(kind)){
            CHARGES.remove(caster);
            RELEASES.removeIf(v->v.caster.equals(caster));
            return;
        }
        if("cancel".equals(kind)){
            String spellId=values.getOrDefault("spell","");
            RELEASES.removeIf(v->v.caster.equals(caster)&&v.spell.id().equals(spellId));
            return;
        }

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
                    progress,started,now+CHARGE_TTL,false,0,seed,false,Vec3.ZERO));
            return;
        }
        if("release".equals(kind)){
            if(singletonRelease(spell))
                RELEASES.removeIf(v->v.caster.equals(caster)&&v.spell.id().equals(spell.id()));
            evictForCapacity();
            boolean attached=followsCaster(spell);
            Vec3 attachOffset=attached?attachmentOffset(caster,spell,center):Vec3.ZERO;
            RELEASES.add(new Visual(caster,spell,fusion,ingredients,center,target,direction,range,power,1,
                    now,now+duration*50_000_000L,true,impactAge,seed,attached,attachOffset));
            CHARGES.remove(caster);
        }
    }

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        syncLevelIdentity();
        long now=System.nanoTime();
        CHARGES.values().removeIf(v->v.expiresAt<now);
        RELEASES.removeIf(v->v.expiresAt<now
                ||(v.attached&&findLiving(v.caster)==null&&now-v.startedAt>500_000_000L));
        if(CHARGES.isEmpty()&&RELEASES.isEmpty())return;
        List<RenderEntry> entries=new ArrayList<>();
        for(Visual v:CHARGES.values()){
            Vec3 center=renderCenter(v);
            int color=SpellCinematicDirector.color(v.spell);
            if(!ArcaneSpellVisualOverhaul.replacesBaseSigil(v.spell)){
                ArcaneWorldMesh sigilMesh=MeteorBarragePattern.withSeed(v.seed,
                        ()->ArcaneSigilDirector.charge(v.spell,v.direction,targetOffset(v),v.range,v.progress,v.fusion,v.startedAt));
                entries.add(new RenderEntry(center,sigilMesh,color,42,1.0F));
            }
            ArcaneWorldMesh authoredSigil=MeteorBarragePattern.withSeed(v.seed,
                    ()->ArcaneSpellVisualOverhaul.chargeSigil(v.spell,v.direction,v.progress,v.range,v.startedAt));
            if(authoredSigil.size()>0)entries.add(new RenderEntry(center,authoredSigil,color,58,1.0F));
            if(!ArcaneSpellVisualOverhaul.replacesBaseChargeBody(v.spell)){
                ArcaneWorldMesh cinematicMesh=MeteorBarragePattern.withSeed(v.seed,
                        ()->SpellCinematicDirector.charge(v.spell,v.direction,targetOffset(v),v.range,v.power,v.progress,v.fusion,v.startedAt));
                entries.add(new RenderEntry(center,cinematicMesh,color,52,1.0F));
            }
            ArcaneWorldMesh authoredBody=MeteorBarragePattern.withSeed(v.seed,
                    ()->ArcaneSpellVisualOverhaul.chargeBody(v.spell,v.direction,targetOffset(v),v.progress,v.range,v.startedAt));
            if(authoredBody.size()>0)entries.add(new RenderEntry(center,authoredBody,color,64,1.0F));
            if(v.spell.circle()>=7){
                if(!HighCircleMaintenanceOverlay.replacesChargeTimeline(v.spell)){
                    ArcaneWorldMesh timeline=MeteorBarragePattern.withSeed(v.seed,
                            ()->AuthoredHighCircleTimeline.charge(v.spell,v.direction,targetOffset(v),v.range,v.progress,v.startedAt,v.seed));
                    if(timeline.size()>0)entries.add(new RenderEntry(center,timeline,color,94,1.0F));
                }
                ArcaneWorldMesh staging=HighCircleMaintenanceOverlay.charge(v.spell,v.direction,targetOffset(v),v.progress,v.startedAt,v.seed);
                if(staging.size()>0)entries.add(new RenderEntry(center,staging,color,102,1.0F));
            }
        }
        for(Visual v:RELEASES){
            double age=clamp((now-v.startedAt)/(double)Math.max(1L,v.expiresAt-v.startedAt),0,1);
            double durationSeconds=Math.max(.05,(v.expiresAt-v.startedAt)/1_000_000_000.0);
            double elapsedSeconds=Math.max(0.0,(now-v.startedAt)/1_000_000_000.0);
            float opacity=releaseOpacity(v,now);
            Vec3 center=renderCenter(v);
            int color=SpellCinematicDirector.color(v.spell);
            boolean regalia=PersistentBuffRegalia.handles(v.spell);
            boolean castingAfterglow=!regalia||elapsedSeconds<.85;
            Vec3 presentationDirection=regalia?maintainedDirection(v):v.direction;
            if(castingAfterglow&&!"prismatic_wall".equals(v.spell.id())&&!ArcaneSpellVisualOverhaul.replacesBaseSigil(v.spell)){
                ArcaneWorldMesh echo=MeteorBarragePattern.withSeed(v.seed,
                        ()->ArcaneSigilDirector.releaseEcho(v.spell,v.direction,targetOffset(v),v.range,age,v.fusion,v.startedAt));
                if(echo.size()>0)entries.add(new RenderEntry(center,echo,ArcaneSigilDirector.releaseEchoColor(color,age),36,opacity));
            }
            if(castingAfterglow&&!"prismatic_wall".equals(v.spell.id())&&!ArcaneSpellVisualOverhaul.replacesBaseRelease(v.spell)){
                ArcaneWorldMesh releaseMesh=MeteorBarragePattern.withSeed(v.seed,
                        ()->SpellCinematicDirector.release(v.spell,v.direction,targetOffset(v),v.range,v.power,
                                age,v.impactAge,v.fusion,v.ingredients));
                entries.add(new RenderEntry(center,releaseMesh,color,70,opacity));
            }
            if(castingAfterglow){
                ArcaneWorldMesh authoredRelease=MeteorBarragePattern.withSeed(v.seed,
                        ()->ArcaneSpellVisualOverhaul.release(v.spell,v.direction,targetOffset(v),v.range,v.power,
                                age,elapsedSeconds,durationSeconds,v.seed));
                if(authoredRelease.size()>0)entries.add(new RenderEntry(center,authoredRelease,color,82,opacity));
                if(v.spell.circle()>=7){
                    ArcaneWorldMesh timeline=MeteorBarragePattern.withSeed(v.seed,
                            ()->AuthoredHighCircleTimeline.release(v.spell,v.direction,targetOffset(v),v.range,
                                    age,v.impactAge,elapsedSeconds,durationSeconds,v.seed));
                    if(timeline.size()>0)entries.add(new RenderEntry(center,timeline,color,100,opacity));
                    ArcaneWorldMesh maintenance=HighCircleMaintenanceOverlay.release(v.spell,v.direction,targetOffset(v),
                            elapsedSeconds,durationSeconds,v.seed);
                    if(maintenance.size()>0)entries.add(new RenderEntry(center,maintenance,color,108,opacity));
                }
            }
            if(regalia){
                ArcaneWorldMesh maintained=MeteorBarragePattern.withSeed(v.seed,
                        ()->PersistentBuffRegalia.release(v.spell,presentationDirection,elapsedSeconds,durationSeconds,v.seed));
                if(maintained.size()>0)entries.add(new RenderEntry(center,maintained,color,116,opacity));
            }
            if("prismatic_wall".equals(v.spell.id())){
                for(int layer=0;layer<7;layer++)entries.add(new RenderEntry(center,
                        ArcaneSpellVisualOverhaul.prismaticWallLayer(v.spell,v.direction,targetOffset(v),v.range,
                                age,elapsedSeconds,layer),SpellCinematicDirector.prismaticColor(layer),96,opacity));
            }else if("prismatic_spray".equals(v.spell.id())){
                for(int layer=0;layer<7;layer++)entries.add(new RenderEntry(center,
                        SpellCinematicDirector.prismaticAccent(v.spell,v.direction,targetOffset(v),v.range,age,layer),
                        SpellCinematicDirector.prismaticColor(layer),88,opacity));
            }
        }
        entries.sort(Comparator.comparingInt(RenderEntry::priority).reversed());
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
            Vec3 offset=entry.center.subtract(camera);
            double distanceSqr=offset.lengthSqr();
            if(distanceSqr>MAX_DISTANCE_SQR)continue;
            if(distanceSqr>SILHOUETTE_DISTANCE_SQR&&entry.priority<90)continue;
            if(distanceSqr>DETAIL_DISTANCE_SQR&&entry.priority<58)continue;
            int cost=entry.mesh.size();
            if(cost<=0||cost>MAX_ENTRY||used+cost>MAX_FRAME)continue;
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(offset.x,offset.y,offset.z);
            entry.mesh.submit(event.getPoseStack(),event.getSubmitNodeCollector(),withOpacity(entry.argb,entry.opacity),scale);
            event.getPoseStack().popPose();
            used+=cost;
        }
    }

    private static boolean followsCaster(SpellDefinition spell){
        if("time_stop".equals(spell.id()))return false;
        if(PersistentBuffRegalia.handles(spell))return true;
        if("antimagic_field".equals(spell.id())||"control_weather".equals(spell.id()))return true;
        SpellPresentationProfile.SigilStyle sigil=SpellPresentationProfile.profile(spell).sigil();
        return sigil==SpellPresentationProfile.SigilStyle.BODY_HALO
                ||sigil==SpellPresentationProfile.SigilStyle.FEET_RUNE;
    }

    private static boolean singletonRelease(SpellDefinition spell){
        if(followsCaster(spell)||"time_stop".equals(spell.id()))return true;
        return switch(spell.id()){
            case "grease","web","slow","sleet_storm","cloudkill","insect_plague",
                    "incendiary_cloud","winter_domain","wall_of_fire","wall_of_force",
                    "wind_wall","wall_of_ice","prismatic_wall" -> true;
            default -> false;
        };
    }

    private static void evictForCapacity(){
        while(RELEASES.size()>=MAX_VISUALS){
            int victim=-1;
            for(int i=0;i<RELEASES.size();i++){
                Visual v=RELEASES.get(i);
                if(v.expiresAt-v.startedAt<4_000_000_000L){victim=i;break;}
            }
            if(victim<0)victim=0;
            RELEASES.remove(victim);
        }
    }

    private static Vec3 maintainedDirection(Visual visual){
        LivingEntity entity=findLiving(visual.caster);
        if(entity!=null){
            Vec3 look=entity.getLookAngle();
            Vec3 flat=new Vec3(look.x,0.0,look.z);
            if(flat.lengthSqr()>1.0E-8)return flat.normalize();
        }
        return visual.direction;
    }

    private static Vec3 attachmentOffset(UUID caster,SpellDefinition spell,Vec3 originalCenter){
        SpellPresentationProfile.Profile profile=SpellPresentationProfile.profile(spell);
        if("control_weather".equals(spell.id()))return new Vec3(0,profile.skyHeight(),0);
        LivingEntity entity=findLiving(caster);
        if(entity!=null)return originalCenter.subtract(entity.position());
        SpellPresentationProfile.SigilStyle sigil=profile.sigil();
        if(sigil==SpellPresentationProfile.SigilStyle.BODY_HALO)return new Vec3(0,1.0,0);
        if(sigil==SpellPresentationProfile.SigilStyle.FEET_RUNE||"antimagic_field".equals(spell.id()))return new Vec3(0,.055,0);
        return Vec3.ZERO;
    }

    private static Vec3 renderCenter(Visual visual){
        if(visual.attached){
            LivingEntity entity=findLiving(visual.caster);
            if(entity!=null)return entity.position().add(visual.attachOffset);
        }
        return visual.center;
    }

    private static LivingEntity findLiving(UUID id){
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft.player!=null&&minecraft.player.getUUID().equals(id))return minecraft.player;
        if(minecraft.level==null||minecraft.player==null)return null;
        return minecraft.level.getEntitiesOfClass(LivingEntity.class,
                        minecraft.player.getBoundingBox().inflate(224.0), value->value.getUUID().equals(id)).stream()
                .findFirst().orElse(null);
    }

    private static void syncLevelIdentity(){
        Object current=Minecraft.getInstance().level;
        if(current==LAST_LEVEL)return;
        CHARGES.clear();
        RELEASES.clear();
        LAST_LEVEL=current;
    }

    private static float releaseOpacity(Visual visual,long now){
        long remaining=Math.max(0L,visual.expiresAt-now);
        long total=Math.max(1L,visual.expiresAt-visual.startedAt);
        long window=Math.min(750_000_000L,Math.max(220_000_000L,total/12L));
        return remaining>=window?1.0F:(float)smooth(remaining/(double)window);
    }

    private static int withOpacity(int argb,float opacity){
        int alpha=(argb>>>24)&255;
        int next=(int)Math.round(alpha*clamp(opacity,0,1));
        return (Math.max(0,Math.min(255,next))<<24)|(argb&0x00FFFFFF);
    }

    private static Vec3 targetOffset(Visual visual){
        return visual.target.subtract(visual.center);
    }
    private static Map<String,String> parse(String state){Map<String,String> result=new HashMap<>();for(String part:state.split(";")){int i=part.indexOf('=');if(i>0)result.put(part.substring(0,i),part.substring(i+1));}return result;}
    private static int integer(Map<String,String> values,String key,int fallback){try{return Integer.parseInt(values.getOrDefault(key,Integer.toString(fallback)));}catch(Exception ignored){return fallback;}}
    private static long longValue(Map<String,String> values,String key,long fallback){try{return Long.parseLong(values.getOrDefault(key,Long.toString(fallback)));}catch(Exception ignored){return fallback;}}
    private static double decimal(Map<String,String> values,String key,double fallback){try{return Double.parseDouble(values.getOrDefault(key,Double.toString(fallback)));}catch(Exception ignored){return fallback;}}
    private static Vec3 safeDirection(Vec3 value){return value==null||value.lengthSqr()<1.0E-8?new Vec3(0,0,1):value.normalize();}
    private static double clamp(double value,double min,double max){return Math.max(min,Math.min(max,value));}
    private static double smooth(double value){double t=clamp(value,0,1);return t*t*(3.0-2.0*t);}

    private record Visual(UUID caster, SpellDefinition spell, boolean fusion, int ingredients,
                          Vec3 center, Vec3 target, Vec3 direction, double range, double power, double progress,
                          long startedAt, long expiresAt, boolean release, double impactAge, long seed,
                          boolean attached, Vec3 attachOffset) {}
    private record RenderEntry(Vec3 center, ArcaneWorldMesh mesh, int argb, int priority, float opacity) {}
}
