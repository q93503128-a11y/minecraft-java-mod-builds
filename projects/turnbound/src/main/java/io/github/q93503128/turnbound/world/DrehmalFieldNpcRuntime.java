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
    private static final double MATERIALIZE_RADIUS=120.0D;
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
            var site=site(level,caller,npc.siteLocator());
            if(site==null||site.runtimePosition()==null||!DrabyelServiceActors.supports(npc.visualAsset()))continue;
            active.add(npc.locator());
            Vec3 pos=vec(site.runtimePosition());
            if(!demanded(level,npc,pos)){discard(level,npc.locator());continue;}
            if(!DrehmalAdaptiveRoutePlacement.fieldProxyContentClear(
                    level,site.runtimePosition().x(),site.runtimePosition().y(),site.runtimePosition().z())){
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
        if(npc==null||!eligible(player,npc))return false;
        var site=site(player,npc.siteLocator());
        if(site==null||site.runtimePosition()==null)return false;
        Vec3 pos=vec(site.runtimePosition());
        double radius=npc.interactionRadius()+1.0D;
        if(player.position().distanceToSqr(pos)>radius*radius)return false;
        if(target instanceof BattleActorEntity actor){face(actor,player);actor.playServiceGreeting();}
        var server=player.level().getServer();
        if("turnbound:npc/avsal/contract_broker".equals(npc.locator())){
            if(server!=null){
                ExternalWorldSavedData data=ExternalWorldSavedData.get(server);
                Set<String> flags=data.onboardingFlags(player.getUUID());
                Set<String> clears=CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
                if(clears.contains("AV_FIRST_BOSS")&&!flags.contains(AvsalExpansionProgress.FIRST_BOSS_REPORTED)){
                    data.markOnboardingFlag(player.getUUID(),AvsalExpansionProgress.FIRST_BOSS_CLEARED);
                    data.markOnboardingFlag(player.getUUID(),AvsalExpansionProgress.FIRST_BOSS_REPORTED);
                    FieldNetwork.showDialogue(player,npc.playerLabel(),
                            "북쪽 수로문이 열렸다는 소식이 벌써 여기까지 왔어요. 카르논과 호위대가 멈췄다면 당분간 운송대가 폐허 안쪽까지 접근할 수 있겠네요.\n\n첫 원정 기록은 제가 정리하겠습니다. 이후에는 여기서 아브살 지역 의뢰를 계속 받을 수 있어요.");
                    ExternalWorldBootstrap.refreshFieldContext(player);
                    return true;
                }
            }
            if(RegionalContractService.interact(player,npc.playerLabel()))return true;
        }
        String dialogue=npc.dialogue();
        if(server!=null){
            ExternalWorldSavedData data=ExternalWorldSavedData.get(server);
            if(!npc.progressFlag().isBlank()
                    && (npc.progressRequiresFlag().isBlank() || data.onboardingFlag(player.getUUID(),npc.progressRequiresFlag()))
                    && !data.onboardingFlag(player.getUUID(),npc.progressFlag())){
                data.markOnboardingFlag(player.getUUID(),npc.progressFlag());
                if(npc.progressFlag().startsWith("AVSAL_MQ_AV02_")){
                    int count=AvsalExpansionProgress.investigationCount(data.onboardingFlags(player.getUUID()));
                    dialogue=dialogue+"\n\n조사 진척 "+Math.min(count,2)+"/2";
                    if(AvsalExpansionProgress.reconcileInvestigation(player))dialogue=dialogue+" · 필요한 단서를 충분히 확보했습니다.";
                }else if(npc.progressFlag().startsWith("DRABYEL_LOCAL_")){
                    int count=DrabyelLocalArcProgress.count(data.onboardingFlags(player.getUUID()));
                    dialogue=dialogue+"\n\n조사 진척 "+Math.min(count,2)+"/2";
                    if(DrabyelLocalArcProgress.reconcile(player))dialogue=dialogue+" · 두 곳을 확인했습니다. 아렌에게 돌아갈 수 있습니다.";
                }
            }
            if(!npc.questOfferFlag().isBlank()){
                boolean first=!data.onboardingFlag(player.getUUID(),npc.questOfferFlag());
                if(first){data.markOnboardingFlag(player.getUUID(),npc.questOfferFlag());dialogue=dialogue+"\n\n"+npc.questOfferDialogue();}
            }
        }
        FieldNetwork.showDialogue(player, npc.playerLabel(), dialogue);
        ExternalWorldBootstrap.refreshFieldContext(player);
        return true;
    }

    static boolean isNpc(Entity target){return locator(target)!=null;}

    static boolean nearNpc(ServerPlayer player,String npcLocator){
        if(player==null||npcLocator==null||npcLocator.isBlank())return false;
        var npc=DrehmalFieldNpcCatalog.npc(npcLocator);
        if(npc==null)return false;
        var site=site(player,npc.siteLocator());
        if(site==null||site.runtimePosition()==null)return false;
        double radius=npc.interactionRadius()+2.0D;
        return player.position().distanceToSqr(vec(site.runtimePosition()))<=radius*radius;
    }

    static void clear(){
        if(boundLevel!=null)for(String locator:List.copyOf(ACTORS.keySet()))discard(boundLevel,locator);
        ACTORS.clear();boundLevel=null;lastTick=Long.MIN_VALUE;
    }

    private static DrehmalFirstRouteCatalog.Site site(ServerLevel level,ServerPlayer player,String locator){
        var first=DrehmalAdaptiveRoutePlacement.site(player,locator);
        if(first!=null)return first;
        var local=DrabyelLocalArcRuntime.site(player,locator);
        if(local!=null)return local;
        var avsal=AvsalExpansionRuntime.site(player,locator);
        if(avsal!=null)return avsal;
        if(level!=null){
            for(ServerPlayer candidate:level.players()){
                if(candidate==player||!ExternalWorldBootstrap.active(candidate))continue;
                local=DrabyelLocalArcRuntime.site(candidate,locator);
                if(local!=null)return local;
                if(!AvsalExpansionRuntime.active(candidate))continue;
                avsal=AvsalExpansionRuntime.site(candidate,locator);
                if(avsal!=null)return avsal;
            }
        }
        return null;
    }

    private static DrehmalFirstRouteCatalog.Site site(ServerPlayer player,String locator){
        return player!=null && player.level() instanceof ServerLevel level ? site(level,player,locator) : null;
    }

    private static boolean demanded(ServerLevel level,DrehmalFieldNpcCatalog.Npc npc,Vec3 pos){
        double radiusSq=MATERIALIZE_RADIUS*MATERIALIZE_RADIUS;
        for(ServerPlayer player:level.players()){
            if(!ExternalWorldBootstrap.active(player)||BattleSessionManager.exists(player)||player.isSpectator())continue;
            if(!eligible(player,npc))continue;
            if(player.position().distanceToSqr(pos)<=radiusSq)return true;
        }
        return false;
    }

    private static boolean eligible(ServerPlayer player,DrehmalFieldNpcCatalog.Npc npc){
        if(player==null||npc==null)return false;
        if(!AvsalExpansionRuntime.ownsSite(npc.siteLocator()))return true;
        var server=player.level().getServer();
        if(server==null)return false;
        Set<String> flags=ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        if(!npc.progressRequiresFlag().isBlank())return flags.contains(npc.progressRequiresFlag());
        return flags.contains(AvsalExpansionProgress.OUTSKIRTS_REACHED);
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
        actor.setGlowingTag(false);
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
