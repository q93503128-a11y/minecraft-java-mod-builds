package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Authored Chapter 5 Old Relay Station route with four record rooms, exact FT_RELAY, B05 and final console. */
public final class OldRelayStationWorld {
    private static final int MARKER_X = 365;
    private static final int MARKER_Y = 57;
    private static final int MARKER_Z = -305;

    private record Node(double x, int groundY, double z) {}
    public record EncounterPoint(String id, Vec3 fieldPosition, Vec3 battleAnchor, float battleYaw) {}
    public record BuiltChapter(Vec3 entry, Vec3 fastTravel, Vec3 bossAnchor, float bossYaw,
                               List<Vec3> recordConsoles, Vec3 relayConsole, List<EncounterPoint> encounters) {
        public BuiltChapter { recordConsoles = List.copyOf(recordConsoles); encounters = List.copyOf(encounters); }
    }

    private OldRelayStationWorld() {}

    public static BuiltChapter build(ServerLevel level) {
        BuiltChapter chapter = built();
        if (!hasMarker(level)) {
            List<Node> route = route();
            for (int i = 0; i < route.size() - 1; i++) buildSegment(level, route.get(i), route.get(i + 1));
            for (int[] c : new int[][]{{290,67,-205},{320,67,-245},{365,67,-305},{395,66,-285},{410,65,-330},{455,65,-310},{430,65,-350}}) {
                clearing(level, c[0], c[1], c[2], c[0] == 430 ? 23 : 16);
            }
            buildEntranceGate(level, false);
            buildFastTravel(level);
            buildRecordRooms(level);
            buildBossGate(level, false);
            buildBossChamber(level);
            buildFinalConsoleSite(level);
            buildRelayArchitecture(level);
            writeMarker(level);
        }
        return chapter;
    }

    public static boolean contains(Vec3 p) {
        if (p == null || p.y < 48 || p.y > 104) return false;
        return p.x >= 242 && p.x <= AsterMarchRegionCatalog.OLD_RELAY.maxX() + 8
                && p.z >= AsterMarchRegionCatalog.OLD_RELAY.minZ() - 8 && p.z <= -162;
    }

    public static void setEntranceOpen(ServerLevel level, boolean open) { buildEntranceGate(level, open); }
    public static void setBossGateOpen(ServerLevel level, boolean open) { buildBossGate(level, open); }

    private static BuiltChapter built() {
        AsterMarchRegionCatalog.Anchor ft = AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_RELAY);
        AsterMarchRegionCatalog.Anchor boss = AsterMarchRegionCatalog.boss(AsterMarchRegionCatalog.B05);
        return new BuiltChapter(
                new Vec3(270.0, 68.0, -185.0),
                new Vec3(ft.x(), ft.y(), ft.z()),
                new Vec3(boss.x(), boss.y(), boss.z()), boss.yaw(),
                List.of(new Vec3(305,68,-225), new Vec3(345,68,-272), new Vec3(400,67,-300), new Vec3(418,66,-325)),
                new Vec3(458,66,-350),
                List.of(
                        point("ENC_R01",290,68,-205,293,68,-205,90),
                        point("ENC_R02",320,68,-245,323,68,-245,90),
                        point("ENC_R03",395,67,-285,398,67,-285,90),
                        point("ENC_R04",410,66,-330,413,66,-330,90),
                        point("ENC_R05",455,66,-310,458,66,-310,90),
                        new EncounterPoint("BATTLE_B05",new Vec3(417,66,-350),new Vec3(boss.x(),boss.y(),boss.z()),boss.yaw())));
    }

    private static EncounterPoint point(String id,double fx,double fy,double fz,double bx,double by,double bz,float yaw){return new EncounterPoint(id,new Vec3(fx,fy,fz),new Vec3(bx,by,bz),yaw);}
    private static List<Node> route(){return List.of(new Node(250,67,-170),new Node(270,67,-185),new Node(290,67,-205),new Node(305,67,-225),new Node(320,67,-245),new Node(345,67,-272),new Node(365,67,-305),new Node(395,66,-285),new Node(400,66,-300),new Node(410,65,-330),new Node(418,65,-325),new Node(430,65,-350),new Node(455,65,-310),new Node(458,65,-350));}

    private static void buildSegment(ServerLevel l,Node a,Node b){int steps=Math.max(1,(int)Math.ceil(Math.max(Math.abs(b.x-a.x),Math.abs(b.z-a.z))));for(int s=0;s<=steps;s++){double t=s/(double)steps;int cx=(int)Math.round(lerp(a.x,b.x,t)),cz=(int)Math.round(lerp(a.z,b.z,t)),y=(int)Math.round(lerp(a.groundY,b.groundY,t));double dx=b.x-a.x,dz=b.z-a.z,len=Math.max(.001,Math.sqrt(dx*dx+dz*dz)),rx=-dz/len,rz=dx/len;for(int o=-6;o<=6;o++){int x=(int)Math.round(cx+rx*o),z=(int)Math.round(cz+rz*o);for(int fy=y-4;fy<y;fy++)set(l,x,fy,z,Blocks.DEEPSLATE);Block g=Math.abs(o)<=2?(((cx+cz+o)&3)==0?Blocks.CRACKED_DEEPSLATE_BRICKS:Blocks.DEEPSLATE_TILES):(((cx*7+cz*13+o)&7)==0?Blocks.CRYING_OBSIDIAN:Blocks.DEEPSLATE_BRICKS);set(l,x,y,z,g);for(int ay=y+1;ay<=y+10;ay++)set(l,x,ay,z,Blocks.AIR);}}}
    private static void clearing(ServerLevel l,int cx,int y,int cz,int r){for(int x=cx-r;x<=cx+r;x++)for(int z=cz-r;z<=cz+r;z++){int d=(x-cx)*(x-cx)+(z-cz)*(z-cz);if(d>r*r)continue;for(int fy=y-4;fy<y;fy++)set(l,x,fy,z,Blocks.DEEPSLATE);set(l,x,y,z,((x*19+z*11)&7)==0?Blocks.CRYING_OBSIDIAN:Blocks.DEEPSLATE_TILES);for(int ay=y+1;ay<=y+12;ay++)set(l,x,ay,z,Blocks.AIR);}}

    private static void buildEntranceGate(ServerLevel l,boolean open){int z=-178,y=67;for(int x=258;x<=282;x++){boolean center=x>=266&&x<=274;for(int dy=1;dy<=7;dy++){if(center)set(l,x,y+dy,z,open?Blocks.AIR:Blocks.IRON_BARS);else set(l,x,y+dy,z,((x+dy)&3)==0?Blocks.OBSIDIAN:Blocks.DEEPSLATE_TILES);}}for(int x=257;x<=283;x++)set(l,x,y+8,z,Blocks.DEEPSLATE_TILES);}
    private static void buildFastTravel(ServerLevel l){int cx=365,y=67,cz=-305;for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++)if(dx*dx+dz*dz<=25)set(l,cx+dx,y,cz+dz,((dx+dz)&1)==0?Blocks.POLISHED_DEEPSLATE:Blocks.DEEPSLATE_TILES);set(l,cx,y+1,cz,Blocks.AMETHYST_BLOCK);set(l,cx,y+2,cz,Blocks.BEACON);for(int[] p:new int[][]{{-6,-6},{6,-6},{-6,6},{6,6}})post(l,cx+p[0],y,cz+p[1]);}

    private static void buildRecordRooms(ServerLevel l){int[][] rooms={{305,67,-225},{345,67,-272},{400,66,-300},{418,65,-325}};for(int i=0;i<rooms.length;i++){int cx=rooms[i][0],y=rooms[i][1],cz=rooms[i][2];for(int x=cx-7;x<=cx+7;x++)for(int z=cz-6;z<=cz+6;z++){boolean edge=x==cx-7||x==cx+7||z==cz-6||z==cz+6;if(edge)for(int dy=1;dy<=5;dy++)set(l,x,y+dy,z,(x+z+dy)%5==0?Blocks.IRON_BLOCK:Blocks.DEEPSLATE_BRICKS);set(l,x,y+6,z,Blocks.DEEPSLATE_TILE_SLAB);}for(int dy=1;dy<=3;dy++)set(l,cx,y+dy,cz+6,Blocks.AIR);set(l,cx,y+1,cz-2,Blocks.LECTERN);set(l,cx,y+2,cz-2,i%2==0?Blocks.REDSTONE_LAMP:Blocks.AMETHYST_BLOCK);}}

    private static void buildBossGate(ServerLevel l,boolean open){int x=420,y=65;for(int z=-362;z<=-338;z++){boolean center=z>=-355&&z<=-345;for(int dy=1;dy<=8;dy++){if(center)set(l,x,y+dy,z,open?Blocks.AIR:Blocks.IRON_BARS);else set(l,x,y+dy,z,((z+dy)&3)==0?Blocks.CRYING_OBSIDIAN:Blocks.REINFORCED_DEEPSLATE);}}for(int z=-363;z<=-337;z++)set(l,x,y+9,z,Blocks.REINFORCED_DEEPSLATE);}
    private static void buildBossChamber(ServerLevel l){for(int x=420;x<=460;x++)for(int z=-370;z<=-330;z++){int d=(x-440)*(x-440)+(z+350)*(z+350);if(d>21*21)continue;set(l,x,65,z,d>18*18?Blocks.OBSIDIAN:Blocks.DEEPSLATE_TILES);for(int y=66;y<=78;y++)set(l,x,y,z,Blocks.AIR);}for(int a=0;a<8;a++){double r=a*Math.PI/4;int x=440+(int)Math.round(Math.cos(r)*17),z=-350+(int)Math.round(Math.sin(r)*17);for(int dy=1;dy<=7;dy++)set(l,x,65+dy,z,Blocks.CRYING_OBSIDIAN);set(l,x,73,z,Blocks.SOUL_LANTERN);}}
    private static void buildFinalConsoleSite(ServerLevel l){int x=458,y=65,z=-350;for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)set(l,x+dx,y,z+dz,Blocks.POLISHED_DEEPSLATE);set(l,x,y+1,z,Blocks.IRON_BLOCK);set(l,x,y+2,z,Blocks.AMETHYST_BLOCK);set(l,x,y+3,z,Blocks.REDSTONE_LAMP);}
    private static void buildRelayArchitecture(ServerLevel l){int[][] pylons={{280,-220,67},{330,-300,67},{380,-250,66},{390,-360,65},{470,-280,65},{480,-390,65}};for(int[] p:pylons){for(int dy=1;dy<=9;dy++)set(l,p[0],p[2]+dy,p[1],dy%3==0?Blocks.IRON_BLOCK:Blocks.DEEPSLATE_BRICKS);set(l,p[0],p[2]+10,p[1],Blocks.AMETHYST_BLOCK);}}
    private static void post(ServerLevel l,int x,int y,int z){set(l,x,y+1,z,Blocks.COBBLED_DEEPSLATE_WALL);set(l,x,y+2,z,Blocks.SOUL_LANTERN);}
    private static boolean hasMarker(ServerLevel l){return l.getBlockState(new BlockPos(MARKER_X,MARKER_Y,MARKER_Z)).is(Blocks.LODESTONE)&&l.getBlockState(new BlockPos(MARKER_X+1,MARKER_Y,MARKER_Z)).is(Blocks.REINFORCED_DEEPSLATE)&&l.getBlockState(new BlockPos(MARKER_X+2,MARKER_Y,MARKER_Z)).is(Blocks.AMETHYST_BLOCK);}
    private static void writeMarker(ServerLevel l){set(l,MARKER_X,MARKER_Y,MARKER_Z,Blocks.LODESTONE);set(l,MARKER_X+1,MARKER_Y,MARKER_Z,Blocks.REINFORCED_DEEPSLATE);set(l,MARKER_X+2,MARKER_Y,MARKER_Z,Blocks.AMETHYST_BLOCK);}
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
    private static void set(ServerLevel l,int x,int y,int z,Block b){l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);}
}
