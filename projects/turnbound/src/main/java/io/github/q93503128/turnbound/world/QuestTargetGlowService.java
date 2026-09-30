package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

/** Per-tick server target set used by physical NPC/object outline presentation. */
final class QuestTargetGlowService {
    private record Cache(long tick,Set<String> ids){}
    private static final Map<ServerLevel,Cache> CACHE=new IdentityHashMap<>();
    private QuestTargetGlowService(){}

    static boolean shouldGlow(ServerLevel level,String locator){
        return level!=null&&locator!=null&&!locator.isBlank()&&targets(level).contains(locator);
    }

    private static Set<String> targets(ServerLevel level){
        long tick=level.getGameTime();
        Cache cached=CACHE.get(level);
        if(cached!=null&&cached.tick()==tick)return cached.ids();
        Set<String> ids=new LinkedHashSet<>();
        for(var player:level.players()){
            if(!ExternalWorldBootstrap.active(player)||player.isSpectator()||BattleSessionManager.exists(player))continue;
            FieldUiSnapshot snapshot=DrehmalFirstRouteRuntime.explorationSnapshot(player);
            if(snapshot.navigation().active())ids.add(snapshot.navigation().id());
            for(var point:snapshot.mapPoints())if(point.objective()&&point.active())ids.add(point.id());
        }
        Set<String> frozen=Set.copyOf(ids);
        CACHE.put(level,new Cache(tick,frozen));
        return frozen;
    }

    static void clear(){CACHE.clear();}
}
