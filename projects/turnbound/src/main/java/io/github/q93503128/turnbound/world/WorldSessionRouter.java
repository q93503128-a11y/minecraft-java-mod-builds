package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Routes the persistent world shell between Radia hub and authored field sessions. */
public final class WorldSessionRouter {
    private WorldSessionRouter(){}
    public static boolean active(ServerPlayer p){ return RadiaHubSessionManager.active(p)||FieldSessionManager.active(p); }
    public static void enterInitial(ServerPlayer p){ RadiaHubSessionManager.enter(p); }
    public static void tick(ServerPlayer p){ if(RadiaHubSessionManager.active(p)) RadiaHubSessionManager.tick(p); else FieldSessionManager.tick(p); }
    public static boolean interactEntity(ServerPlayer p,Entity e){ return RadiaHubSessionManager.active(p)?RadiaHubSessionManager.interactEntity(p,e):FieldSessionManager.interactEntity(p,e); }
    public static void command(ServerPlayer p,String command){
        if(RadiaHubSessionManager.active(p)){ RadiaHubSessionManager.command(p,command); return; }
        if(FieldSessionManager.active(p)&&command!=null&&command.equals("TRAVEL|"+AsterMarchRegionCatalog.FT_RADIA)){ FieldSessionManager.remove(p); RadiaHubSessionManager.enter(p); return; }
        FieldSessionManager.command(p,command);
    }
    public static void onBattleEnded(ServerPlayer p,String id,BattleOutcome outcome){ if(RadiaHubSessionManager.active(p)) RadiaHubSessionManager.onBattleEnded(p,id,outcome); else FieldSessionManager.onBattleEnded(p,id,outcome); }
    public static void remove(ServerPlayer p){ RadiaHubSessionManager.remove(p); FieldSessionManager.remove(p); }
    public static void clearAll(Iterable<ServerPlayer> ps){ RadiaHubSessionManager.clearAll(ps); FieldSessionManager.clearAll(ps); }
}
