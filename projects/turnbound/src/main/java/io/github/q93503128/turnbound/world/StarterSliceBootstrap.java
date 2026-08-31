package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Command-free staged world bootstrap; after terrain prep the player now enters canonical FT_RADIA. */
public final class StarterSliceBootstrap {
    private static final int COLUMN_BUDGET_PER_TICK=160;
    private static final Map<UUID,StarterSliceWorld.BuildJob> JOBS=new LinkedHashMap<>();
    private StarterSliceBootstrap(){}
    public static void tick(ServerPlayer player){
        if(player.level().dimension()!=Level.OVERWORLD||player.tickCount<40) return;
        if(WorldSessionRouter.active(player)||BattleSessionManager.exists(player)) return;
        ServerLevel level=(ServerLevel)player.level(); StarterSliceWorld.BuildJob job=JOBS.get(player.getUUID());
        if(job==null){ StarterSliceWorld.BuiltSlice existing=StarterSliceWorld.findExisting(level); if(existing!=null){ player.setNoGravity(false); WorldSessionRouter.enterInitial(player); return; } job=StarterSliceWorld.begin(level); JOBS.put(player.getUUID(),job); player.setNoGravity(true); player.setDeltaMovement(Vec3.ZERO); FieldNetwork.sync(player,FieldUiSnapshot.loading(job.stageLabel(),0)); }
        lock(player); clearVanillaMobs(level,job.baseY()); boolean done=job.tick(level,COLUMN_BUDGET_PER_TICK);
        if(done){ JOBS.remove(player.getUUID()); player.setNoGravity(false); WorldSessionRouter.enterInitial(player); return; }
        if((player.tickCount&1)==0) FieldNetwork.sync(player,FieldUiSnapshot.loading(job.stageLabel(),job.progressPercent()));
    }
    public static boolean building(ServerPlayer p){ return JOBS.containsKey(p.getUUID()); }
    public static void remove(ServerPlayer p){ if(JOBS.remove(p.getUUID())!=null){p.setNoGravity(false);FieldNetwork.close(p);} }
    public static void clearAll(Iterable<ServerPlayer> ps){for(ServerPlayer p:ps)remove(p);JOBS.clear();}
    private static void lock(ServerPlayer p){p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;}
    private static void clearVanillaMobs(ServerLevel l,int y){ AABB a=new AABB(-140,y-12,-120,140,y+32,205);for(Mob m:l.getEntitiesOfClass(Mob.class,a))m.discard(); }
}
