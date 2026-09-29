package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.DrabyelServiceActors;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Shared physical service-NPC runtime for New Drabyel.
 *
 * <p>Static surveyed coordinates remain supported, but unpromoted services can also be placed automatically from
 * source-backed New Drabyel anchors after the bound 26.2 world passes live collision/content checks. The runtime
 * never edits terrain and never teleports the player into a service menu.</p>
 */
final class DrabyelHubServiceRuntime {
    private static final String COMMON_TAG="turnbound_drabyel_service";
    private static final String LOCATOR_PREFIX=COMMON_TAG+":";
    private static final double MATERIALIZE_RADIUS=96.0D;
    private static final Map<String,UUID> ACTORS=new HashMap<>();

    private static ServerLevel boundLevel;
    private static long lastTick=Long.MIN_VALUE;

    private DrabyelHubServiceRuntime(){}

    static void tick(ServerPlayer caller){
        if(caller==null||!(caller.level() instanceof ServerLevel level))return;
        bind(level);
        long gameTime=level.getGameTime();
        if(lastTick==gameTime)return;
        lastTick=gameTime;

        if(!hubDemanded(level)){
            for(String locator:List.copyOf(ACTORS.keySet()))discard(level,locator);
            return;
        }

        Set<String> active=new HashSet<>();
        for(var service:DrabyelHubAutoPlacement.runtimeServices(level)){
            if(!DrabyelServiceActors.supports(service.visualAsset()))continue;
            active.add(service.locator());
            if(!demanded(level,service)){
                discard(level,service.locator());
                continue;
            }
            BattleActorEntity actor=ensure(level,service);
            if(actor!=null)updatePresentation(level,service,actor,gameTime);
        }

        for(String locator:List.copyOf(ACTORS.keySet())){
            if(!active.contains(locator))discard(level,locator);
        }
    }

    static boolean interact(ServerPlayer player,Entity target){
        if(player==null||target==null)return false;
        String locator=serviceLocator(target);
        if(locator==null)return false;
        if(!(player.level() instanceof ServerLevel level))return false;
        var service=DrabyelHubAutoPlacement.runtimeService(level,locator);
        if(service==null||service.runtimePosition()==null)return false;
        double radius=service.interactionRadius()+1.0D;
        Vec3 pos=vec(service.runtimePosition());
        if(player.position().distanceToSqr(pos)>radius*radius)return false;

        if("SUMMON".equals(service.role())&&!DrehmalContentUnlocks.summonUnlocked(player.getUUID())){
            FieldNetwork.showDialogue(player, service.playerLabel(),
                    "아직 정령의 흔적이 잠잠합니다. 캐피털 밸리의 경고 동굴에 자리 잡은 강적을 넘고 다시 찾아오세요.");
            return true;
        }

        var server=player.level().getServer();
        if(server!=null){
            ExternalWorldSavedData.get(server).markOnboardingFlag(
                    player.getUUID(),
                    DrehmalContextualOnboarding.serviceFlag(service.role()));
        }
        if(target instanceof BattleActorEntity actor)actor.playServiceGreeting();
        if ("GREETER".equals(service.role())) {
            FieldNetwork.showDialogue(player, service.playerLabel(),
                    "뉴 드라비엘에 잘 왔어요. 대장간과 시장에서 장비를 정비하고, 마구간에서 길을 확인해 보세요. 준비가 끝나면 서쪽 길이 다음 여정으로 이어집니다.");
            return true;
        }
        if ("STORY".equals(service.role())) {
            FieldNetwork.showDialogue(player, service.playerLabel(),
                    "캐피털 밸리 길목에는 아직 지나치기 쉬운 위험과 오래된 흔적이 남아 있습니다. 서두르지 말고 마을에서 준비한 뒤 움직이세요.");
            return true;
        }
        String hint=service.facilityHint();
        if(hint==null||hint.isBlank())return true;
        MetaNetwork.open(player,hint);
        return true;
    }

    static boolean nearFacility(ServerPlayer player,String facilityHint){
        if(player==null||facilityHint==null||facilityHint.isBlank()||!nearHub(player,160.0D))return false;
        if(!(player.level() instanceof ServerLevel level))return false;
        for(var service:DrabyelHubAutoPlacement.runtimeServices(level)){
            if(!facilityHint.equals(service.facilityHint())||service.runtimePosition()==null)continue;
            double radius=service.interactionRadius()+1.5D;
            if(player.position().distanceToSqr(vec(service.runtimePosition()))<=radius*radius)return true;
        }
        return false;
    }

    static String serviceLocator(Entity entity){
        if(entity==null)return null;
        for(String tag:entity.entityTags()){
            if(tag.startsWith(LOCATOR_PREFIX))return tag.substring(LOCATOR_PREFIX.length());
        }
        return null;
    }

    static DrabyelInteractionPromptRules.Prompt prompt(ServerPlayer player){
        if(player==null||BattleSessionManager.exists(player)||player.isSpectator()||!nearHub(player,160.0D))return DrabyelInteractionPromptRules.none();
        if(!(player.level() instanceof ServerLevel level))return DrabyelInteractionPromptRules.none();
        return DrabyelInteractionPromptRules.nearest(
                DrabyelHubAutoPlacement.runtimeServices(level),
                player.getX(),player.getY(),player.getZ(),
                DrabyelServiceActors::supports);
    }

    static Set<String> availableRoles(ServerPlayer player){
        if(player==null||!nearHub(player,160.0D)||!(player.level() instanceof ServerLevel level))return Set.of();
        Set<String> roles=new HashSet<>();
        for(var service:DrabyelHubAutoPlacement.runtimeServices(level)){
            if(DrabyelServiceActors.supports(service.visualAsset()))roles.add(service.role());
        }
        return Set.copyOf(roles);
    }

    static void clear(){
        if(boundLevel!=null){
            for(String locator:List.copyOf(ACTORS.keySet()))discard(boundLevel,locator);
        }
        ACTORS.clear();
        DrabyelHubAutoPlacement.clear();
        boundLevel=null;
        lastTick=Long.MIN_VALUE;
    }

    private static void bind(ServerLevel level){
        if(boundLevel==level)return;
        clear();
        boundLevel=level;
    }

    private static boolean nearHub(ServerPlayer player,double radius){
        var hub=DrehmalWorldProfile.enabled(DrehmalWorldProfile.HUB_LOCATOR);
        if(player==null||hub==null)return false;
        double dx=player.getX()-(hub.x()+0.5D),dz=player.getZ()-(hub.z()+0.5D);
        return dx*dx+dz*dz<=radius*radius;
    }

    private static boolean hubDemanded(ServerLevel level){
        var hub=DrehmalWorldProfile.enabled(DrehmalWorldProfile.HUB_LOCATOR);
        if(hub==null)return false;
        double radiusSq=160.0D*160.0D;
        for(ServerPlayer player:level.players()){
            if(!ExternalWorldBootstrap.active(player)||BattleSessionManager.exists(player)||player.isSpectator())continue;
            double dx=player.getX()-(hub.x()+0.5D),dz=player.getZ()-(hub.z()+0.5D);
            if(dx*dx+dz*dz<=radiusSq)return true;
        }
        return false;
    }

    private static boolean demanded(ServerLevel level,DrabyelHubServiceCatalog.Service service){
        Vec3 center=vec(service.runtimePosition());
        double radiusSq=MATERIALIZE_RADIUS*MATERIALIZE_RADIUS;
        for(ServerPlayer player:level.players()){
            if(!ExternalWorldBootstrap.active(player)||BattleSessionManager.exists(player)||player.isSpectator())continue;
            if(player.position().distanceToSqr(center)<=radiusSq)return true;
        }
        return false;
    }

    private static BattleActorEntity ensure(ServerLevel level,DrabyelHubServiceCatalog.Service service){
        UUID cached=ACTORS.get(service.locator());
        Entity entity=cached==null?null:level.getEntity(cached);
        if(entity instanceof BattleActorEntity actor&&service.locator().equals(serviceLocator(actor))){
            configure(actor,service);
            return actor;
        }
        if(cached!=null)ACTORS.remove(service.locator());

        Vec3 center=vec(service.runtimePosition());
        AABB area=new AABB(center.x-3,center.y-2,center.z-3,center.x+3,center.y+4,center.z+3);
        BattleActorEntity found=null;
        for(BattleActorEntity candidate:level.getEntitiesOfClass(BattleActorEntity.class,area)){
            if(!service.locator().equals(serviceLocator(candidate)))continue;
            if(found==null)found=candidate;else candidate.discard();
        }
        if(found==null){
            found=DrabyelServiceActors.spawn(level,service.visualAsset(),center,service.runtimeYaw());
            if(found==null)return null;
            found.addTag(COMMON_TAG);
            found.addTag(LOCATOR_PREFIX+service.locator());
        }
        configure(found,service);
        ACTORS.put(service.locator(),found.getUUID());
        return found;
    }

    private static void configure(BattleActorEntity actor,DrabyelHubServiceCatalog.Service service){
        Vec3 center=vec(service.runtimePosition());
        actor.setPos(center.x,center.y,center.z);
        if(service.runtimeYaw()!=null){
            actor.setYRot(service.runtimeYaw());
            actor.setYHeadRot(service.runtimeYaw());
            actor.setYBodyRot(service.runtimeYaw());
        }
        actor.setInvulnerable(true);
        actor.setCustomName(Component.literal(service.playerLabel()).withStyle(color(service.role())));
        actor.setCustomNameVisible(false);
        actor.setFieldWalking(false);
        actor.addTag(COMMON_TAG);
        actor.addTag(LOCATOR_PREFIX+service.locator());
    }

    private static void updatePresentation(ServerLevel level,DrabyelHubServiceCatalog.Service service,BattleActorEntity actor,long gameTime){
        ServerPlayer nearest=nearest(level,actor);
        double distance=nearest==null?Double.MAX_VALUE:Math.sqrt(actor.distanceToSqr(nearest));

        // R_PG-style field readability: service identity belongs to the close-range interaction prompt,
        // not a nameplate floating over town NPCs from across the street.
        actor.setCustomNameVisible(false);

        if(nearest!=null&&distance<=service.interactionRadius()+2.0D){
            face(actor,nearest);
            return;
        }

        if("BLACKSMITH".equals(service.role())&&gameTime%120L==Math.floorMod(service.locator().hashCode(),120)){
            actor.playServiceWork();
        }
    }

    private static ServerPlayer nearest(ServerLevel level,BattleActorEntity actor){
        ServerPlayer best=null;
        double bestDistance=Double.MAX_VALUE;
        for(ServerPlayer player:level.players()){
            if(!ExternalWorldBootstrap.active(player)||BattleSessionManager.exists(player)||player.isSpectator())continue;
            double distance=actor.distanceToSqr(player);
            if(distance<bestDistance){bestDistance=distance;best=player;}
        }
        return best;
    }

    private static void face(BattleActorEntity actor,ServerPlayer player){
        double dx=player.getX()-actor.getX(),dz=player.getZ()-actor.getZ();
        if(dx*dx+dz*dz<=0.000001D)return;
        float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));
        actor.setYRot(yaw);actor.setYHeadRot(yaw);actor.setYBodyRot(yaw);
    }

    private static void discard(ServerLevel level,String locator){
        UUID id=ACTORS.remove(locator);
        if(id==null)return;
        Entity entity=level.getEntity(id);
        if(entity!=null&&locator.equals(serviceLocator(entity)))entity.discard();
    }

    private static Vec3 vec(DrabyelHubServiceCatalog.Position position){
        return new Vec3(position.x()+0.5D,position.y(),position.z()+0.5D);
    }

    private static ChatFormatting color(String role){
        return switch(role){
            case "MARKET","BLACKSMITH"->ChatFormatting.GOLD;
            case "TRAVEL"->ChatFormatting.AQUA;
            case "SUMMON"->ChatFormatting.LIGHT_PURPLE;
            default->ChatFormatting.WHITE;
        };
    }
}
