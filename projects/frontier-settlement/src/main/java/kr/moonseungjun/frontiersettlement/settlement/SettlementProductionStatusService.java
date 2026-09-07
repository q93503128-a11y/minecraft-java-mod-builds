package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.server.level.ServerLevel;
import java.util.HashMap;
import java.util.Map;

/** Presentation-only status cache fed by existing production AI; never simulation authority. */
public final class SettlementProductionStatusService {
    private static final long STALE_AFTER_TICKS = 200L;
    private static final Map<BuildingKey, StatusEntry> STATUS = new HashMap<>();
    private SettlementProductionStatusService() {}
    private record BuildingKey(String type,int x,int y,int z,int rotation) {}
    private record StatusEntry(String text,long tick) {}
    public static void mark(ServerLevel level, BuildingRecord building, String text) {
        if(level==null||building==null||text==null||text.isBlank()) return;
        STATUS.put(key(building),new StatusEntry(text,level.getGameTime()));
    }
    public static String statusFor(ServerLevel level, BuildingRecord building) {
        if(level==null||building==null) return "상태 확인 중";
        if(!level.hasChunkAt(building.workCenter())) return "청크 미로드";
        StatusEntry e=STATUS.get(key(building));
        if(e==null||level.getGameTime()-e.tick()>STALE_AFTER_TICKS) return "상태 확인 중";
        return e.text();
    }
    public static void clear(){ STATUS.clear(); }
    private static BuildingKey key(BuildingRecord b){ return new BuildingKey(b.type(),b.originX(),b.originY(),b.originZ(),b.rotation()); }
}
