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
import java.util.*;

final class DrehmalFieldNpcRuntime {
    private static final String TAG="turnbound_drehmal_field_npc";
    private static final String PREFIX=TAG+":";
    private static final double MATERIALIZE_RADIUS=80.0D;
    private static final Map<String,UUID> ACTORS=new LinkedHashMap<>();
    private static ServerLevel boundLevel;
    private static long lastTick=Long.MIN_VALUE;

    private DrehmalFieldNpcRuntime(){}

    static void tick(ServerPlayer caller){
        if(caller==null||!(caller.level() instanceof ServerLevel level))return;
        if(boundLevel!=level){clear();boundLevel=level;}
        if(lastTick==level.getGameTime())return;
        lastTick=level.getGameTime();

        Set<String> active=new HashSet<>();
        for(var npc:DrehmalFieldNpcCatalog.all()){
            var site=DrehmalAdaptiveRoutePlacement.site(caller,npc.siteLocator());
            if(site==null||site.runtimePosition()==null||!DrabyelServiceActors.supports(npc.visualAsset()))continue;
            active.add(npc.locator());
            Vec3 pos=vec(site.runtimePosition());
            if(!demanded(level,pos)){discard(level,npc.locator());continue;}
            if(!DrehmalAdaptiveRoutePlacement.sourceContentClear(
                    level,site.runtimePosition().x(),site.runtimePosition().y(),site.runtimePosition().z(),2.75D)){
                discard(level,npc.locator());
                continue;
            }
            BattleActorEntity actor=ensure(level,npc,pos);
            if(actor!=null)present(level,npc,actor);
        }
        for(String locator:List.copyOf(ACTORS.keySet()))if(!active.contains(locator))discard(level,locator);
    }

    static boolean interact(ServerPlayer player,Entity target){
        String locator=locator(target);
        if(player==null||locator==null)return false;
        var npc=DrehmalFieldNpcCatalog.npc(locator);
        if(npc==null)return false;
        var site=DrehmalAdaptiveRoutePlacement.site(player,npc.siteLocator());
        if(site==null||site.runtimePosition()==null)return false;
        Vec3 pos=vec(site.runtimePosition());
        double radius=npc.interactionRadius()+1.0D;
        if(player.position().distanceToSqr(pos)>radius*radius)return false;
        if(target instanceof BattleActorEntity actor){face(actor,player);actor.playServiceGreeting();}
        player.sendSystemMessage(Component.literal(npc.playerLabel()+": ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(npc.dialogue()).withStyle(ChatFormatting.WHITE)));
        return true;
    }

    static boolean isNpc(Entity target){return locator(target)!=null;}

    static void clear(){
        if(boundLevel!=null)for(String locator:List.copyOf(ACTORS.keySet()))discard(boundLevel,locator);
        ACTORS.clear();boundLevel=null;lastTick=Long.MIN_VALUE;
    }

    private static boolean demanded(ServerLevel level,Vec3 pos){
        double radiusSq=MATERIALIZE_RADIUS*MATERIALIZE_RADIUS;
        for(ServerPlayer player:level.players()){
            if(!ExternalWorldBootstrap.active(player)||BattleSessionManager.exists(player)||player.isSpectator())continue;
            if(player.position().distanceToSqr(pos)<=radiusSq)return true;
        }
        return false;
    }

    private static BattleActorEntity ensure(ServerLevel level,DrehmalFieldNpcCatalog.Npc npc,Vec3 pos){
        UUID cached=ACTORS.get(npc.locator());Entity raw=cached==null?null:level.getEntity(cached);
        if(raw instanceof BattleActorEntity actor&&npc.locator().equals(locator(actor))){configure(actor,npc,pos);return actor;}
        if(cached!=null)ACTORS.remove(npc.locator());

        AABB box=new AABB(pos.x-3,pos.y-2,pos.z-3,pos.x+3,pos.y+4,pos.z+3);
        for(BattleActorEntity candidate:level.getEntitiesOfClass(BattleActorEntity.class,box)){
            if(!npc.locator().equals(locator(candidate)))continue;
            configure(candidate,npc,pos);ACTORS.put(npc.locator(),candidate.getUUID());return candidate;
        }
        BattleActorEntity actor=DrabyelServiceActors.spawn(level,npc.visualAsset(),pos,0.0F);
        if(actor==null)return null;
        actor.addTag(TAG);actor.addTag(PREFIX+npc.locator());configure(actor,npc,pos);ACTORS.put(npc.locator(),actor.getUUID());
        return actor;
    }

    private static void configure(BattleActorEntity actor,DrehmalFieldNpcCatalog.Npc npc,Vec3 pos){
        actor.setPos(pos.x,pos.y,pos.z);actor.setInvulnerable(true);actor.setFieldWalking(false);
        actor.setCustomName(Component.literal(npc.playerLabel()).withStyle(ChatFormatting.GOLD));actor.setCustomNameVisible(false);
        actor.addTag(TAG);actor.addTag(PREFIX+npc.locator());
    }

    private static void present(ServerLevel level,DrehmalFieldNpcCatalog.Npc npc,BattleActorEntity actor){
        ServerPlayer nearest=null;double best=Double.MAX_VALUE;
        for(ServerPlayer player:level.players()){
            if(!ExternalWorldBootstrap.active(player)||BattleSessionManager.exists(player)||player.isSpectator())continue;
            double d=actor.distanceToSqr(player);if(d<best){best=d;nearest=player;}
        }
        actor.setCustomNameVisible(nearest!=null&&best<=36.0D);
        if(nearest!=null&&best<=64.0D)face(actor,nearest);
    }

    private static void face(BattleActorEntity actor,ServerPlayer player){
        double dx=player.getX()-actor.getX(),dz=player.getZ()-actor.getZ();if(dx*dx+dz*dz<0.0001D)return;
        float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));actor.setYRot(yaw);actor.setYHeadRot(yaw);actor.setYBodyRot(yaw);
    }

    private static String locator(Entity entity){
        if(entity==null)return null;
        for(String tag:entity.entityTags())if(tag.startsWith(PREFIX))return tag.substring(PREFIX.length());
        return null;
    }
    private static void discard(ServerLevel level,String locator){
        UUID id=ACTORS.remove(locator);if(id==null)return;Entity entity=level.getEntity(id);if(entity!=null&&locator.equals(locator(entity)))entity.discard();
    }
    private static Vec3 vec(DrehmalFirstRouteCatalog.Position p){return new Vec3(p.x()+0.5D,p.y(),p.z()+0.5D);}
}
