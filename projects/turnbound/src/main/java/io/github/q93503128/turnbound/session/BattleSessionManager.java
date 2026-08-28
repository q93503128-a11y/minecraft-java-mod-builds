package io.github.q93503128.turnbound.session;

import net.minecraft.server.level.ServerPlayer;
import java.util.HashMap; import java.util.Map; import java.util.UUID;

public final class BattleSessionManager {
    private static final Map<UUID,BattleSession> SESSIONS=new HashMap<>(); private BattleSessionManager(){}
    public static void start(ServerPlayer p){ end(p); BattleSession s=new BattleSession(p); SESSIONS.put(p.getUUID(),s); BattleNetwork.sync(p,s); }
    public static boolean active(ServerPlayer p){ BattleSession s=SESSIONS.get(p.getUUID()); return s!=null&&!s.finished(); }
    public static boolean exists(ServerPlayer p){return SESSIONS.containsKey(p.getUUID());}
    public static void tick(ServerPlayer p){BattleSession s=SESSIONS.get(p.getUUID()); if(s!=null){s.tick(p); if(p.tickCount%5==0)BattleNetwork.sync(p,s);}}
    public static void command(ServerPlayer p,String cmd){ BattleSession s=SESSIONS.get(p.getUUID()); if(s==null)return; String[] a=cmd.split("\\|",-1); switch(a[0]){ case "ACT" -> {if(a.length>=4)s.action(p,a[1],a[2],a[3]);} case "AUTO"->s.toggleAuto(p); case "SPEED"->s.toggleSpeed(p); case "FLEE"->end(p); default->{} } }
    public static void end(ServerPlayer p){BattleSession old=SESSIONS.remove(p.getUUID()); if(old!=null)old.cleanup(p); BattleNetwork.close(p);}
    public static void clearAll(Iterable<ServerPlayer> players){for(ServerPlayer p:players)end(p); SESSIONS.clear();}
}
