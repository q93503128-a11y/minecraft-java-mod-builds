package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Routes the persistent world shell between Radia and authored chapter field sessions. */
public final class WorldSessionRouter {
    private WorldSessionRouter() {}

    public static boolean active(ServerPlayer p) {
        return RadiaHubSessionManager.active(p) || FieldSessionManager.active(p) || GloamwoodSessionManager.active(p)
                || BrokenAqueductSessionManager.active(p) || EmberQuarrySessionManager.active(p);
    }
    public static void enterInitial(ServerPlayer p) { RadiaHubSessionManager.enter(p); }
    public static void tick(ServerPlayer p) {
        if (RadiaHubSessionManager.active(p)) RadiaHubSessionManager.tick(p);
        else if (GloamwoodSessionManager.active(p)) GloamwoodSessionManager.tick(p);
        else if (BrokenAqueductSessionManager.active(p)) BrokenAqueductSessionManager.tick(p);
        else if (EmberQuarrySessionManager.active(p)) EmberQuarrySessionManager.tick(p);
        else FieldSessionManager.tick(p);
    }
    public static boolean interactEntity(ServerPlayer p, Entity e) {
        if (RadiaHubSessionManager.active(p)) return RadiaHubSessionManager.interactEntity(p,e);
        if (GloamwoodSessionManager.active(p)) return GloamwoodSessionManager.interactEntity(p,e);
        if (BrokenAqueductSessionManager.active(p)) return BrokenAqueductSessionManager.interactEntity(p,e);
        if (EmberQuarrySessionManager.active(p)) return EmberQuarrySessionManager.interactEntity(p,e);
        return FieldSessionManager.interactEntity(p,e);
    }
    public static void command(ServerPlayer p,String command) {
        if(RadiaHubSessionManager.active(p)){RadiaHubSessionManager.command(p,command);return;}
        if(GloamwoodSessionManager.active(p)){GloamwoodSessionManager.command(p,command);return;}
        if(BrokenAqueductSessionManager.active(p)){BrokenAqueductSessionManager.command(p,command);return;}
        if(EmberQuarrySessionManager.active(p)){EmberQuarrySessionManager.command(p,command);return;}
        if(FieldSessionManager.active(p)&&command!=null&&command.equals("TRAVEL|"+AsterMarchRegionCatalog.FT_RADIA)){FieldSessionManager.remove(p);RadiaHubSessionManager.enter(p);return;}
        FieldSessionManager.command(p,command);
    }
    public static void onBattleEnded(ServerPlayer p,String id,BattleOutcome outcome) {
        if(RadiaHubSessionManager.active(p))RadiaHubSessionManager.onBattleEnded(p,id,outcome);
        else if(GloamwoodSessionManager.active(p))GloamwoodSessionManager.onBattleEnded(p,id,outcome);
        else if(BrokenAqueductSessionManager.active(p))BrokenAqueductSessionManager.onBattleEnded(p,id,outcome);
        else if(EmberQuarrySessionManager.active(p))EmberQuarrySessionManager.onBattleEnded(p,id,outcome);
        else FieldSessionManager.onBattleEnded(p,id,outcome);
    }
    public static void remove(ServerPlayer p){RadiaHubSessionManager.remove(p);GloamwoodSessionManager.remove(p);BrokenAqueductSessionManager.remove(p);EmberQuarrySessionManager.remove(p);FieldSessionManager.remove(p);}
    public static void clearAll(Iterable<ServerPlayer> ps){RadiaHubSessionManager.clearAll(ps);GloamwoodSessionManager.clearAll(ps);BrokenAqueductSessionManager.clearAll(ps);EmberQuarrySessionManager.clearAll(ps);FieldSessionManager.clearAll(ps);}
}
