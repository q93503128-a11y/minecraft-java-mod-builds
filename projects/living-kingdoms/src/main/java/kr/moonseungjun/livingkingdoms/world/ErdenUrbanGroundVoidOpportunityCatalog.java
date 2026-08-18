package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Source-only survey for the 77 urban shells that have no safe authored upper floor.
 * It asks a narrower question than the normal topology catalog: how much walkable ground area
 * can be created by adding floor blocks into source AIR only, without deleting a single imported
 * block. No world chunks are read or loaded and this class never mutates a block.
 */
public final class ErdenUrbanGroundVoidOpportunityCatalog {
    public static final int CATALOG_REVISION = 1;
    public static final int EXPECTED_GROUND_ONLY = 77;
    private static final int MIN_USABLE_CELLS = 35;
    private static final int MIN_DEPTH = 6;
    private static final int ROOF_SCAN = 18;
    private static final int[][] NEIGHBORS = {{1,0},{-1,0},{0,1},{0,-1}};

    private static final Map<String, VoidProfile> FRAGMENTS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanGroundVoidOpportunityCatalog() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        int placements = 0;
        int usablePlacements = 0;
        int minimumReachable = Integer.MAX_VALUE;
        int maximumReachable = 0;
        int minimumFloorAdds = Integer.MAX_VALUE;
        int maximumFloorAdds = 0;
        Set<String> groundOnlyFragments = new HashSet<>();

        for (ErdenUrbanPlacedTopologyCatalog.PlacementProfile placement
                : ErdenUrbanPlacedTopologyCatalog.placements().values()) {
            if (ErdenUrbanAuthoredUpperRouteManager.isEligible(placement.entrance())) continue;
            placements++;
            groundOnlyFragments.add(placement.fragmentKey());
            VoidProfile profile = FRAGMENTS.computeIfAbsent(placement.fragmentKey(), key -> {
                ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot = snapshots.get(key);
                if (snapshot == null) throw new IllegalStateException("Missing ground-void source " + key);
                return analyze(snapshot);
            });
            if (profile.usable()) usablePlacements++;
            minimumReachable = Math.min(minimumReachable, profile.reachableCells());
            maximumReachable = Math.max(maximumReachable, profile.reachableCells());
            minimumFloorAdds = Math.min(minimumFloorAdds, profile.newFloorCells());
            maximumFloorAdds = Math.max(maximumFloorAdds, profile.newFloorCells());
        }
        if (placements != EXPECTED_GROUND_ONLY) {
            throw new IllegalStateException("Ground-only Erden placement count drifted: " + placements);
        }
        if (minimumReachable == Integer.MAX_VALUE) minimumReachable = 0;
        if (minimumFloorAdds == Integer.MAX_VALUE) minimumFloorAdds = 0;
        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_GROUND_VOID_SURVEY placements={} fragments={} usable_placements={} reachable_min={} reachable_max={} floor_add_min={} floor_add_max={} min_usable_cells={} min_depth={} source_blocks_cut=0 source_air_only=true source_only=true world_reads=false mutations=0 revision={}",
                placements, groundOnlyFragments.size(), usablePlacements,
                minimumReachable, maximumReachable, minimumFloorAdds, maximumFloorAdds,
                MIN_USABLE_CELLS, MIN_DEPTH, CATALOG_REVISION);
        for (Map.Entry<String, VoidProfile> entry : FRAGMENTS.entrySet()) {
            VoidProfile p = entry.getValue();
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_GROUND_VOID_FRAGMENT fragment={} reachable={} existing_support={} new_floor_cells={} max_depth={} roofed=true connected=true usable={} source_blocks_cut=0 source_air_only=true",
                    entry.getKey(), p.reachableCells(), p.existingSupportedCells(),
                    p.newFloorCells(), p.maxDepth(), p.usable());
        }
    }

    public static VoidProfile profile(String fragmentKey) {
        bootstrap();
        return FRAGMENTS.get(fragmentKey);
    }

    private static VoidProfile analyze(ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot) {
        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks = new HashMap<>();
        int doorY = Integer.MAX_VALUE;
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            blocks.put(localKey(block.x(), block.y(), block.z()), block);
            if (block.state().getBlock() instanceof DoorBlock
                    && block.x() == snapshot.entranceX() && block.z() == snapshot.entranceZ()) {
                doorY = Math.min(doorY, block.y());
            }
        }
        if (doorY == Integer.MAX_VALUE) return VoidProfile.EMPTY;
        int[] inward = inward(snapshot.exteriorSide());
        Node seed = null;
        for (int depth=1; depth<=3 && seed==null; depth++) {
            int x=snapshot.entranceX()+inward[0]*depth;
            int z=snapshot.entranceZ()+inward[1]*depth;
            if (candidate(snapshot,blocks,x,doorY,z)) seed=new Node(x,z);
        }
        if (seed == null && candidate(snapshot,blocks,snapshot.entranceX(),doorY,snapshot.entranceZ())) {
            seed = new Node(snapshot.entranceX(), snapshot.entranceZ());
        }
        if (seed == null) return VoidProfile.EMPTY;

        ArrayDeque<Node> pending = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        pending.add(seed);
        visited.add(columnKey(seed.x(), seed.z()));
        int newFloor=0, supported=0, maxDepth=0;
        while(!pending.isEmpty()) {
            Node n=pending.removeFirst();
            if (sourceAir(blocks,n.x(),doorY-1,n.z())) newFloor++; else supported++;
            int depth=(n.x()-snapshot.entranceX())*inward[0]+(n.z()-snapshot.entranceZ())*inward[1];
            maxDepth=Math.max(maxDepth,depth);
            for(int[] d:NEIGHBORS){
                int x=n.x()+d[0], z=n.z()+d[1];
                long key=columnKey(x,z);
                if(!visited.contains(key) && candidate(snapshot,blocks,x,doorY,z)){
                    visited.add(key); pending.addLast(new Node(x,z));
                }
            }
        }
        boolean usable=visited.size()>=MIN_USABLE_CELLS && maxDepth>=MIN_DEPTH;
        return new VoidProfile(visited.size(),supported,newFloor,maxDepth,usable);
    }

    private static boolean candidate(ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
                                     Map<Long,ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
                                     int x,int feetY,int z){
        if(x<0||x>=snapshot.width()||z<0||z>=snapshot.length()||feetY<=0||feetY+2>=snapshot.height()) return false;
        if(!interiorSide(snapshot,x,z)) return false;
        if(!bodyPassable(blocks.get(localKey(x,feetY,z))) || !bodyPassable(blocks.get(localKey(x,feetY+1,z)))) return false;
        ExternalUrbanFabricBuilder.UrbanSourceBlock floor=blocks.get(localKey(x,feetY-1,z));
        if(floor!=null && !floor.state().isAir() && !supportsBody(floor.state())) return false;
        return roofed(snapshot,blocks,x,feetY,z);
    }

    private static boolean roofed(ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
                                  Map<Long,ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
                                  int x,int feetY,int z){
        int max=Math.min(snapshot.height()-1,feetY+ROOF_SCAN);
        for(int y=feetY+2;y<=max;y++){
            ExternalUrbanFabricBuilder.UrbanSourceBlock b=blocks.get(localKey(x,y,z));
            if(b!=null&&!b.state().isAir()&&supportsBody(b.state())) return true;
        }
        return false;
    }

    private static boolean bodyPassable(ExternalUrbanFabricBuilder.UrbanSourceBlock block){
        if(block==null||block.state().isAir()) return true;
        if(block.state().getBlock() instanceof DoorBlock) return true;
        String id=BuiltInRegistries.BLOCK.getKey(block.state().getBlock()).toString();
        return id.contains("torch")||id.contains("button")||id.contains("pressure_plate")||id.contains("carpet")||id.contains("lantern")||id.endsWith("_sign")||id.endsWith("_wall_sign");
    }

    private static boolean supportsBody(BlockState state){
        if(state.isAir()) return false;
        Block block=state.getBlock();
        if(block instanceof DoorBlock) return false;
        String id=BuiltInRegistries.BLOCK.getKey(block).toString();
        return !(id.equals("minecraft:water")||id.equals("minecraft:lava")||id.contains("torch")||id.contains("button")||id.contains("pressure_plate")||id.contains("carpet")||id.contains("lantern")||id.contains("chain")||id.contains("fence")||id.contains("iron_bars")||id.contains("glass_pane")||id.endsWith("_sign")||id.endsWith("_wall_sign")||id.endsWith("_trapdoor"));
    }

    private static boolean sourceAir(Map<Long,ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,int x,int y,int z){
        ExternalUrbanFabricBuilder.UrbanSourceBlock b=blocks.get(localKey(x,y,z));
        return b==null||b.state().isAir();
    }

    private static boolean interiorSide(ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,int x,int z){
        return switch(snapshot.exteriorSide()){
            case "NORTH" -> z>=snapshot.entranceZ();
            case "SOUTH" -> z<=snapshot.entranceZ();
            case "WEST" -> x>=snapshot.entranceX();
            case "EAST" -> x<=snapshot.entranceX();
            default -> throw new IllegalStateException("Unknown exterior side "+snapshot.exteriorSide());
        };
    }

    private static int[] inward(String side){
        return switch(side){case "NORTH"->new int[]{0,1};case "SOUTH"->new int[]{0,-1};case "WEST"->new int[]{1,0};case "EAST"->new int[]{-1,0};default->throw new IllegalStateException("Unknown exterior side "+side);};
    }

    private static long localKey(int x,int y,int z){return ((long)(x&0x1fffff)<<42)^((long)(y&0x3fffff)<<20)^(z&0xfffffL);}
    private static long columnKey(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    private record Node(int x,int z){}

    public record VoidProfile(int reachableCells,int existingSupportedCells,int newFloorCells,int maxDepth,boolean usable){
        private static final VoidProfile EMPTY=new VoidProfile(0,0,0,0,false);
    }
}
