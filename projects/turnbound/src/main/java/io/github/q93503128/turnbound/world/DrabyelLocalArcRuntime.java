package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** 64-110 block New Drabyel main-quest ring. It adds no terrain and resolves every target against the live world. */
final class DrabyelLocalArcRuntime {
    private static final double OBJECT_MATERIALIZE_RADIUS=96.0D;
    private static final String OBJECT_TAG="turnbound_drabyel_local_clue";
    private static final Map<ServerLevel,Map<String,DrehmalFirstRouteCatalog.Site>> SITE_CACHE=new IdentityHashMap<>();
    private static final Map<ServerLevel,Map<String,UUID>> OBJECTS=new IdentityHashMap<>();
    private static final Map<ServerLevel,Long> LAST_TICK=new IdentityHashMap<>();

    private DrabyelLocalArcRuntime(){}

    static boolean offerReady(ServerPlayer player){
        if(player==null||player.level().getServer()==null)return false;
        Set<String> flags=ExternalWorldSavedData.get(player.level().getServer()).onboardingFlags(player.getUUID());
        Set<String> clears=CampaignProgressStore.snapshot(player.getUUID()).clearedEncounters();
        return DrabyelLocalArcProgress.offerReady(clears,flags,DrabyelHubServiceRuntime.availableRoles(player));
    }

    static boolean accept(ServerPlayer player){return offerReady(player)&&DrabyelLocalArcProgress.mark(player,DrabyelLocalArcProgress.ACCEPTED);}

    static DrehmalContextualOnboarding.Guidance guidance(ServerPlayer player){
        if(player==null||player.level().getServer()==null)return null;
        Set<String> flags=ExternalWorldSavedData.get(player.level().getServer()).onboardingFlags(player.getUUID());
        if(offerReady(player))return new DrehmalContextualOnboarding.Guidance(
                "뉴 드라비엘 입구의 아렌에게 주변 순찰 상황을 확인하십시오.",
                "새 지역으로 멀리 떠나기 전에 마을 바로 바깥부터 확인합니다.");
        if(!DrabyelLocalArcProgress.active(flags))return null;
        return new DrehmalContextualOnboarding.Guidance(
                "뉴 드라비엘 주변 조사 지점 3곳 중 2곳을 확인하십시오. ("+DrabyelLocalArcProgress.count(flags)+"/2)",
                "모든 지점을 돌 필요는 없습니다. 지도와 벽 너머 목표 윤곽선을 따라 가까운 두 곳만 확인하십시오.");
    }

    static FieldUiSnapshot.Navigation navigation(ServerPlayer player){
        if(player==null)return FieldUiSnapshot.Navigation.none();
        if(offerReady(player)){
            var greeter=DrabyelHubServiceRuntime.serviceByRole(player,"GREETER");
            if(greeter!=null&&greeter.runtimePosition()!=null){
                var p=greeter.runtimePosition();return new FieldUiSnapshot.Navigation(greeter.locator(),greeter.playerLabel(),p.x()+0.5D,p.z()+0.5D);
            }
            return FieldUiSnapshot.Navigation.none();
        }
        var server=player.level().getServer();if(server==null)return FieldUiSnapshot.Navigation.none();
        Set<String> flags=ExternalWorldSavedData.get(server).onboardingFlags(player.getUUID());
        if(!DrabyelLocalArcProgress.active(flags))return FieldUiSnapshot.Navigation.none();
        DrehmalFirstRouteCatalog.Site best=null;double bestSq=Double.MAX_VALUE;
        for(var plan:DrabyelLocalArcCatalog.sites()){
            if(flags.contains(plan.progressFlag()))continue;
            var site=site(player,plan.locator());if(site==null||site.runtimePosition()==null)continue;
            var pos=site.runtimePosition();double dx=player.getX()-(pos.x()+0.5D),dz=player.getZ()-(pos.z()+0.5D),d=dx*dx+dz*dz;
            if(d<bestSq){bestSq=d;best=site;}
        }
        if(best==null)return FieldUiSnapshot.Navigation.none();
        var p=best.runtimePosition();return new FieldUiSnapshot.Navigation(best.locator(),best.playerLabel(),p.x()+0.5D,p.z()+0.5D);
    }

    static List<FieldUiSnapshot.MapPoint> objectMapPoints(ServerPlayer player){
        if(player==null||player.level().getServer()==null)return List.of();
        Set<String> flags=ExternalWorldSavedData.get(player.level().getServer()).onboardingFlags(player.getUUID());
        if(!DrabyelLocalArcProgress.active(flags))return List.of();
        List<FieldUiSnapshot.MapPoint> out=new ArrayList<>();
        for(var plan:DrabyelLocalArcCatalog.sites()){
            if(!"CLUE_ZONE".equals(plan.kind())||flags.contains(plan.progressFlag()))continue;
            var site=site(player,plan.locator());if(site==null||site.runtimePosition()==null)continue;
            var p=site.runtimePosition();
            out.add(new FieldUiSnapshot.MapPoint(site.locator(),site.playerLabel(),"QUEST",p.x()+0.5D,p.z()+0.5D,true));
        }
        return List.copyOf(out);
    }

    static void tick(ServerPlayer caller){
        if(caller==null||!(caller.level() instanceof ServerLevel level))return;
        recordProgress(caller);
        long now=level.getGameTime();
        if(Objects.equals(LAST_TICK.get(level),now))return;
        LAST_TICK.put(level,now);
        Map<String,UUID> objects=OBJECTS.computeIfAbsent(level,key->new HashMap<>());
        for(var plan:DrabyelLocalArcCatalog.sites()){
            if(!"CLUE_ZONE".equals(plan.kind()))continue;
            var site=resolved(level).get(plan.locator());
            if(site==null||site.runtimePosition()==null||!needed(level,plan)) { discard(level,objects,plan.locator()); continue; }
            ItemEntity item=ensure(level,objects,plan,site.runtimePosition());
            if(item!=null)present(level,item,plan,site.runtimePosition());
        }
    }

    static void recordProgress(ServerPlayer player){
        if(player==null||player.level().getServer()==null||BattleSessionManager.exists(player))return;
        Set<String> flags=ExternalWorldSavedData.get(player.level().getServer()).onboardingFlags(player.getUUID());
        if(!DrabyelLocalArcProgress.active(flags))return;
        for(var plan:DrabyelLocalArcCatalog.sites()){
            if(!"CLUE_ZONE".equals(plan.kind())||flags.contains(plan.progressFlag()))continue;
            var site=site(player,plan.locator());if(site==null||site.runtimePosition()==null)continue;
            var p=site.runtimePosition();double dx=player.getX()-(p.x()+0.5D),dz=player.getZ()-(p.z()+0.5D);
            if(dx*dx+dz*dz>plan.detectionRadius()*plan.detectionRadius())continue;
            if(DrabyelLocalArcProgress.mark(player,plan.progressFlag())){
                boolean completed=DrabyelLocalArcProgress.reconcile(player);
                String text="CHEST".equals(plan.itemVisual())
                        ?"배송 상자에는 마을 바깥으로 급히 빠져나간 흔적과 끊어진 운송 표식이 남아 있습니다."
                        :"찢긴 순찰 기록에는 같은 구간에서 반복적으로 길을 잃었다는 메모가 남아 있습니다.";
                FieldNetwork.showDialogue(player,plan.playerLabel(),text+(completed?"\n\n두 흔적이 같은 구간을 가리킵니다. 아렌에게 돌아갈 만큼은 확인했습니다.":""));
            }
            break;
        }
    }

    static DrehmalFirstRouteCatalog.Site site(ServerPlayer player,String locator){
        if(player==null||!(player.level() instanceof ServerLevel level)||player.level().getServer()==null)return null;
        Set<String> flags=ExternalWorldSavedData.get(player.level().getServer()).onboardingFlags(player.getUUID());
        if(!DrabyelLocalArcProgress.available(flags))return null;
        return resolved(level).get(locator);
    }

    static boolean ownsSite(String locator){return DrabyelLocalArcCatalog.site(locator)!=null;}

    static void clear(){
        for(var entry:OBJECTS.entrySet()){
            ServerLevel level=entry.getKey();
            for(UUID id:entry.getValue().values()){Entity e=level.getEntity(id);if(e!=null&&e.entityTags().contains(OBJECT_TAG))e.discard();}
        }
        OBJECTS.clear();SITE_CACHE.clear();LAST_TICK.clear();
    }

    private static Map<String,DrehmalFirstRouteCatalog.Site> resolved(ServerLevel level){
        return SITE_CACHE.computeIfAbsent(level,DrabyelLocalArcRuntime::resolve);
    }

    private static Map<String,DrehmalFirstRouteCatalog.Site> resolve(ServerLevel level){
        Map<String,DrehmalFirstRouteCatalog.Site> out=new LinkedHashMap<>();
        for(var plan:DrabyelLocalArcCatalog.sites()){
            DrehmalFirstRouteCatalog.Position p=resolveSite(level,plan);if(p==null)continue;
            out.put(plan.locator(),new DrehmalFirstRouteCatalog.Site(plan.locator(),plan.kind(),DrehmalWorldProfile.HUB_LOCATOR,
                    plan.playerLabel(),p,10,0,true,true));
        }
        return Map.copyOf(out);
    }

    private static DrehmalFirstRouteCatalog.Position resolveSite(ServerLevel level,DrabyelLocalArcCatalog.SitePlan plan){
        for(int[] offset:offsets(plan.searchRadius())){
            int x=plan.seed().x()+offset[0],z=plan.seed().z()+offset[1];
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            BlockPos feet=new BlockPos(x,y,z);
            if(!standing(level,feet)||!DrehmalAdaptiveRoutePlacement.sourceContentClear(level,x,y,z,2.0D))continue;
            return new DrehmalFirstRouteCatalog.Position(x,y,z);
        }
        return null;
    }

    private static boolean needed(ServerLevel level,DrabyelLocalArcCatalog.SitePlan plan){
        var site=resolved(level).get(plan.locator());if(site==null||site.runtimePosition()==null)return false;
        Vec3 pos=vec(site.runtimePosition());double maxSq=OBJECT_MATERIALIZE_RADIUS*OBJECT_MATERIALIZE_RADIUS;
        for(ServerPlayer player:level.players()){
            if(!ExternalWorldBootstrap.active(player)||player.isSpectator()||BattleSessionManager.exists(player)||player.level().getServer()==null)continue;
            Set<String> flags=ExternalWorldSavedData.get(player.level().getServer()).onboardingFlags(player.getUUID());
            if(DrabyelLocalArcProgress.active(flags)&&!flags.contains(plan.progressFlag())&&player.position().distanceToSqr(pos)<=maxSq)return true;
        }
        return false;
    }

    private static ItemEntity ensure(ServerLevel level,Map<String,UUID> objects,DrabyelLocalArcCatalog.SitePlan plan,DrehmalFirstRouteCatalog.Position p){
        UUID id=objects.get(plan.locator());Entity raw=id==null?null:level.getEntity(id);
        if(raw instanceof ItemEntity item&&item.entityTags().contains(OBJECT_TAG))return item;
        if(id!=null)objects.remove(plan.locator());
        ItemStack stack=new ItemStack("CHEST".equals(plan.itemVisual())?Items.CHEST:Items.WRITABLE_BOOK);
        ItemEntity item=new ItemEntity(level,p.x()+0.5D,p.y()+0.35D,p.z()+0.5D,stack);
        item.addTag(OBJECT_TAG);item.addTag(OBJECT_TAG+":"+plan.locator());
        item.setNeverPickUp();
        item.setUnlimitedLifetime();
        if(!level.addFreshEntity(item))return null;
        objects.put(plan.locator(),item.getUUID());return item;
    }

    private static void present(ServerLevel level,ItemEntity item,DrabyelLocalArcCatalog.SitePlan plan,DrehmalFirstRouteCatalog.Position p){
        item.setPos(p.x()+0.5D,p.y()+0.35D,p.z()+0.5D);item.setDeltaMovement(Vec3.ZERO);item.setNoGravity(true);item.setInvulnerable(true);
        item.setPickUpDelay(32767);item.setCustomName(Component.literal(plan.playerLabel()).withStyle(ChatFormatting.GOLD));
        boolean near=false;for(ServerPlayer player:level.players())if(ExternalWorldBootstrap.active(player)&&item.distanceToSqr(player)<=64.0D){near=true;break;}
        item.setCustomNameVisible(near);item.setGlowingTag(false);
    }

    private static void discard(ServerLevel level,Map<String,UUID> objects,String locator){
        UUID id=objects.remove(locator);if(id==null)return;Entity e=level.getEntity(id);if(e!=null&&e.entityTags().contains(OBJECT_TAG))e.discard();
    }

    private static Vec3 vec(DrehmalFirstRouteCatalog.Position p){return new Vec3(p.x()+0.5D,p.y(),p.z()+0.5D);}

    private static boolean standing(ServerLevel level,BlockPos feet){
        BlockPos below=feet.below();
        if(level.getBlockState(below).isAir()||level.getBlockState(below).is(BlockTags.LEAVES)||!level.getFluidState(below).isEmpty())return false;
        for(int dy=0;dy<=2;dy++){BlockPos p=feet.above(dy);if(!level.getBlockState(p).getCollisionShape(level,p).isEmpty()||!level.getFluidState(p).isEmpty())return false;}
        return true;
    }

    private static List<int[]> offsets(int radius){
        List<int[]> out=new ArrayList<>();
        for(int dz=-radius;dz<=radius;dz++)for(int dx=-radius;dx<=radius;dx++)if(dx*dx+dz*dz<=radius*radius)out.add(new int[]{dx,dz});
        out.sort(Comparator.comparingInt(v->v[0]*v[0]+v[1]*v[1]));return out;
    }
}
