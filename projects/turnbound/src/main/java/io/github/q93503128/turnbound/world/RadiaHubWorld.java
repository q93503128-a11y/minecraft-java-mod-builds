package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Authored v0.4 Radia hub containing the ten canonical facilities around FT_RADIA. */
public final class RadiaHubWorld {
    private static final int Y = 65;
    private static final int MARKER_Y = 57;

    public record Facility(String id, String label, Vec3 position) {}
    public record BuiltHub(Vec3 spawn, Vec3 director, Vec3 partyConsole, Vec3 relay, Vec3 southGate,
                           List<Vec3> tutorialPedestals, List<Vec3> tutorialBattleAnchors, List<Facility> facilities) {
        public BuiltHub {
            tutorialPedestals = List.copyOf(tutorialPedestals);
            tutorialBattleAnchors = List.copyOf(tutorialBattleAnchors);
            facilities = List.copyOf(facilities);
        }
    }

    private static final List<Facility> FACILITIES = List.of(
            f("RELAY_HALL", "Relay Hall", 0, -8),
            f("ECHO_ARCHIVE", "Echo Archive", -56, 8),
            f("FORGE_ANNEX", "Forge Annex", 56, 8),
            f("MARKET_ROW", "Market Row", -57, 55),
            f("TRAINING_YARD", "Training Yard", 57, 58),
            f("RIFT_GATE", "Rift Gate", -82, -58),
            f("SOUTH_GATE", "South Gate", 0, 112),
            f("MEMORIAL_STEPS", "Memorial Steps", -28, -60),
            f("CLOCK_TOWER", "Clock Tower", 22, -62),
            f("BARRACKS", "Barracks", 72, -26));

    private RadiaHubWorld() {}

    public static BuiltHub build(ServerLevel level) {
        if (!hasMarker(level)) {
            plaza(level); roads(level); starterGuidance(level);
            building(level, -12,-20,25,24, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE);
            interiorLights(level, -12,-20,25,24);
            // Echo Archive deliberately stays dark and amethyst-lit as part of its archive/rift atmosphere.
            building(level, -69,-2,26,22, Blocks.DARK_OAK_PLANKS, Blocks.AMETHYST_BLOCK);
            building(level, 44,-2,25,22, Blocks.STONE_BRICKS, Blocks.IRON_BLOCK);
            interiorLights(level, 44,-2,25,22);
            market(level); training(level); rift(level); memorial(level); clock(level); barracks(level);
            southGate(level, false); relay(level); writeMarker(level);
        }
        return built();
    }

    public static boolean contains(Vec3 p) {
        return p != null && AsterMarchRegionCatalog.RADIA.contains(p.x,p.z) && p.y >= 56 && p.y <= 98;
    }

    public static void setSouthGateOpen(ServerLevel level, boolean open) { southGate(level, open); }

    private static BuiltHub built() {
        // Opening spawn is deliberately not the fast-travel relay. The player starts facing Director Iven.
        return new BuiltHub(new Vec3(0.5,66,12.5), new Vec3(0.5,66,-1.5), new Vec3(7.5,66,2.5),
                new Vec3(0,66,24), new Vec3(0,66,110),
                List.of(new Vec3(50,66,49),new Vec3(50,66,59),new Vec3(50,66,69)),
                List.of(new Vec3(62,66,48),new Vec3(62,66,59),new Vec3(62,66,70)), FACILITIES);
    }

    private static Facility f(String id,String label,double x,double z){ return new Facility(id,label,new Vec3(x,66,z)); }

    private static void plaza(ServerLevel l){ pad(l,0,20,23,Blocks.STONE_BRICKS,Blocks.POLISHED_ANDESITE); }
    private static void roads(ServerLevel l){
        road(l,0,-116,0,120,5); road(l,-88,20,88,20,4); road(l,-58,20,-58,68,3);
        road(l,58,20,58,78,3); road(l,-88,-26,88,-26,3);
        for(Facility f:FACILITIES) levelCircle(l,(int)f.position().x,(int)f.position().z,
                f.id().equals("TRAINING_YARD")?20:f.id().equals("MARKET_ROW")?17:13);
    }

    /** Embedded gold route marks communicate the opening flow without floating debug text. */
    private static void starterGuidance(ServerLevel l){
        for(int z=8;z>=0;z-=4) set(l,0,Y,z,Blocks.GOLD_BLOCK);
        for(int x=12;x<=56;x+=11) set(l,x,Y,20,Blocks.GOLD_BLOCK);
        for(int z=28;z<=40;z+=6) set(l,58,Y,z,Blocks.GOLD_BLOCK);
        for(int z=36;z<=100;z+=16) set(l,0,Y,z,Blocks.GOLD_BLOCK);
        // Training entrance arch at the end of the east guide road.
        for(int y=Y+1;y<=Y+4;y++){ set(l,53,y,40,Blocks.STONE_BRICKS); set(l,63,y,40,Blocks.STONE_BRICKS); }
        for(int x=53;x<=63;x++) set(l,x,Y+5,40,Blocks.POLISHED_ANDESITE);
        set(l,53,Y+5,40,Blocks.LANTERN); set(l,63,Y+5,40,Blocks.LANTERN);
    }

    private static void market(ServerLevel l){
        pad(l,-57,55,18,Blocks.SMOOTH_STONE,Blocks.STONE_BRICKS);
        for(int x=-72;x<=-42;x+=10) stall(l,x,48,Blocks.SPRUCE_PLANKS);
        for(int x=-67;x<=-47;x+=10) stall(l,x,62,Blocks.OAK_PLANKS);
    }

    private static void training(ServerLevel l){
        pad(l,57,59,21,Blocks.COARSE_DIRT,Blocks.SMOOTH_STONE);
        for(int z:new int[]{48,59,70}) for(int dx=-7;dx<=7;dx++) set(l,62+dx,Y,z,Blocks.SMOOTH_STONE);
        for(int z=42;z<=76;z+=4){ set(l,38,Y+1,z,Blocks.OAK_FENCE); set(l,78,Y+1,z,Blocks.OAK_FENCE); }
        for(int x=38;x<=78;x+=4){
            // The old build fenced the tutorial actors in completely. Keep a wide north entrance aligned to the road.
            if(x<52||x>64) set(l,x,Y+1,40,Blocks.OAK_FENCE);
            set(l,x,Y+1,78,Blocks.OAK_FENCE);
        }
        for(int x=52;x<=64;x++) set(l,x,Y+1,40,Blocks.AIR);
    }

    private static void rift(ServerLevel l){
        pad(l,-82,-58,13,Blocks.DEEPSLATE_TILES,Blocks.OBSIDIAN);
        for(int x=-88;x<=-76;x++){ set(l,x,Y+1,-66,Blocks.OBSIDIAN); set(l,x,Y+6,-66,Blocks.OBSIDIAN); }
        for(int y=Y+1;y<=Y+6;y++){ set(l,-88,y,-66,Blocks.OBSIDIAN); set(l,-76,y,-66,Blocks.OBSIDIAN); }
        set(l,-82,Y+1,-65,Blocks.CRYING_OBSIDIAN);
    }

    private static void memorial(ServerLevel l){
        for(int z=-71;z<=-51;z++) for(int x=-38;x<=-18;x++) set(l,x,Y+Math.max(0,(-51-z)/4),z,Blocks.POLISHED_ANDESITE);
        for(int x=-35;x<=-21;x+=7){ set(l,x,Y+6,-70,Blocks.CHISELED_STONE_BRICKS); set(l,x,Y+7,-70,Blocks.SOUL_LANTERN); }
    }

    private static void clock(ServerLevel l){
        building(l,14,-70,17,18,Blocks.STONE_BRICKS,Blocks.QUARTZ_BLOCK);
        interiorLights(l,14,-70,17,18);
        for(int y=Y+5;y<=Y+18;y++) set(l,22,y,-62,y%4==0?Blocks.QUARTZ_BLOCK:Blocks.STONE_BRICKS);
        set(l,22,Y+19,-62,Blocks.GLOWSTONE);
    }

    private static void barracks(ServerLevel l){
        building(l,60,-38,25,25,Blocks.STONE_BRICKS,Blocks.SPRUCE_PLANKS);
        interiorLights(l,60,-38,25,25);
        for(int z=-33;z<=-19;z+=4){ set(l,66,Y+1,z,Blocks.IRON_BARS); set(l,78,Y+1,z,Blocks.TARGET); }
    }

    private static void southGate(ServerLevel l,boolean open){
        int z=116;
        for(int x=-14;x<=14;x++){
            if(Math.abs(x)<=5){ for(int y=Y+1;y<=Y+5;y++) set(l,x,y,z,open?Blocks.AIR:Blocks.IRON_BARS); }
            else for(int y=Y+1;y<=Y+7;y++) set(l,x,y,z,Blocks.STONE_BRICKS);
        }
        for(int x=-16;x<=16;x++) set(l,x,Y,z,Blocks.POLISHED_ANDESITE);
        post(l,-12,Y+7,z,true); post(l,12,Y+7,z,true);
    }

    private static void relay(ServerLevel l){ pad(l,0,24,5,Blocks.POLISHED_ANDESITE,Blocks.STONE_BRICKS); set(l,0,Y+1,24,Blocks.AMETHYST_BLOCK); set(l,0,Y+2,24,Blocks.BEACON); }

    private static void building(ServerLevel l,int x0,int z0,int w,int d,Block wall,Block accent){
        for(int x=x0;x<x0+w;x++) for(int z=z0;z<z0+d;z++){
            column(l,x,z,Blocks.SMOOTH_STONE);
            boolean edge=x==x0||x==x0+w-1||z==z0||z==z0+d-1;
            if(edge) for(int y=Y+1;y<=Y+5;y++) set(l,x,y,z,(x+z+y)%7==0?accent:wall);
            else for(int y=Y+1;y<=Y+5;y++) set(l,x,y,z,Blocks.AIR);
            set(l,x,Y+6,z,Blocks.DEEPSLATE_TILE_SLAB);
        }
        int door=x0+w/2; for(int y=Y+1;y<=Y+3;y++) set(l,door,y,z0+d-1,Blocks.AIR);
    }

    /** Ceiling-integrated light grid for ordinary civic interiors; atmospheric facilities opt out. */
    private static void interiorLights(ServerLevel l,int x0,int z0,int w,int d){
        for(int x=x0+4;x<x0+w-3;x+=6){
            for(int z=z0+4;z<z0+d-3;z+=6) set(l,x,Y+6,z,Blocks.SEA_LANTERN);
        }
    }

    private static void stall(ServerLevel l,int x,int z,Block wood){
        for(int dx=-3;dx<=3;dx++) for(int dz=-2;dz<=2;dz++) set(l,x+dx,Y,z+dz,wood);
        for(int dx:new int[]{-3,3}) for(int dz:new int[]{-2,2}){ set(l,x+dx,Y+1,z+dz,Blocks.OAK_FENCE); set(l,x+dx,Y+2,z+dz,Blocks.OAK_FENCE); }
        for(int dx=-4;dx<=4;dx++) for(int dz=-3;dz<=3;dz++) set(l,x+dx,Y+3,z+dz,Blocks.SPRUCE_SLAB);
    }

    private static void road(ServerLevel l,int x0,int z0,int x1,int z1,int hw){
        int n=Math.max(1,Math.max(Math.abs(x1-x0),Math.abs(z1-z0)));
        for(int i=0;i<=n;i++){ double t=i/(double)n; int x=(int)Math.round(x0+(x1-x0)*t),z=(int)Math.round(z0+(z1-z0)*t);
            for(int dx=-hw;dx<=hw;dx++) for(int dz=-hw;dz<=hw;dz++) if(Math.abs(dx)+Math.abs(dz)<=hw+1) column(l,x+dx,z+dz,Math.abs(dx)+Math.abs(dz)<=1?Blocks.POLISHED_ANDESITE:Blocks.STONE_BRICKS);
        }
    }

    private static void pad(ServerLevel l,int cx,int cz,int r,Block inner,Block rim){
        for(int x=cx-r;x<=cx+r;x++) for(int z=cz-r;z<=cz+r;z++){ int d=(x-cx)*(x-cx)+(z-cz)*(z-cz); if(d<=r*r) column(l,x,z,d>(r-2)*(r-2)?rim:inner); }
    }
    private static void levelCircle(ServerLevel l,int cx,int cz,int r){ for(int x=cx-r;x<=cx+r;x++) for(int z=cz-r;z<=cz+r;z++) if((x-cx)*(x-cx)+(z-cz)*(z-cz)<=r*r) column(l,x,z,Blocks.GRASS_BLOCK); }
    private static void column(ServerLevel l,int x,int z,Block ground){ for(int y=Y-3;y<Y;y++) set(l,x,y,z,Blocks.DIRT); set(l,x,Y,z,ground); for(int y=Y+1;y<=Y+10;y++) set(l,x,y,z,Blocks.AIR); }
    private static void post(ServerLevel l,int x,int y,int z,boolean soul){ set(l,x,y+1,z,Blocks.COBBLESTONE_WALL); set(l,x,y+2,z,soul?Blocks.SOUL_LANTERN:Blocks.LANTERN); }
    private static boolean hasMarker(ServerLevel l){ return l.getBlockState(new BlockPos(0,MARKER_Y,20)).is(Blocks.LODESTONE)&&l.getBlockState(new BlockPos(1,MARKER_Y,20)).is(Blocks.AMETHYST_BLOCK)&&l.getBlockState(new BlockPos(2,MARKER_Y,20)).is(Blocks.EMERALD_BLOCK)&&l.getBlockState(new BlockPos(3,MARKER_Y,20)).is(Blocks.GOLD_BLOCK); }
    private static void writeMarker(ServerLevel l){ set(l,0,MARKER_Y,20,Blocks.LODESTONE);set(l,1,MARKER_Y,20,Blocks.AMETHYST_BLOCK);set(l,2,MARKER_Y,20,Blocks.EMERALD_BLOCK);set(l,3,MARKER_Y,20,Blocks.GOLD_BLOCK); }
    private static void set(ServerLevel l,int x,int y,int z,Block b){ l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2); }
}
