package io.github.q93503128.turnbound.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Physical discoveries for the twelve canonical Region Quest IDs.
 *
 * v0.4 names these quests and defines a region-tier chest reward, but deliberately leaves detailed objectives open.
 * This layer therefore authors discoverable scenes and lore without fabricating kill counts or mandatory objectives.
 */
public final class RegionQuestWorldSites {
    public record Site(String id, String region, String label, Vec3 position, String lore, Item item, ChatFormatting color) {}

    private static final int MARKER_X=-102, MARKER_Y=54, MARKER_Z=334;
    private static final double SPAWN_RADIUS_SQ=96.0*96.0;
    private static final double DESPAWN_RADIUS_SQ=124.0*124.0;
    private static final List<Site> SITES=List.of(
            new Site("RQ_M01_broken_cart","SOUTHGATE","부서진 보급 수레",new Vec3(48,67,192),"초원 순찰대의 오래된 보급 수레. 바퀴 자국은 동쪽으로 급히 꺾여 있다.",Items.CHEST_MINECART,ChatFormatting.YELLOW),
            new Site("RQ_M02_missing_scout","SOUTHGATE","실종 정찰병의 표식",new Vec3(128,67,315),"바위 뒤에 남은 정찰 표식. 누군가 추격을 피해 채석장 방향으로 빠져나갔다.",Items.SPYGLASS,ChatFormatting.GREEN),
            new Site("RQ_M03_fuse_nest","SOUTHGATE","폭발체 도화선 둥지",new Vec3(172,67,308),"마른 풀과 Relay 파편 사이에 E003의 도화선 잔해가 뭉쳐 있다.",Items.STRING,ChatFormatting.RED),
            new Site("RQ_G01_lost_lantern","GLOAMWOOD","잃어버린 포자등불",new Vec3(58,70,-270),"빛이 거의 꺼진 포자등불. 깊은 숲으로 들어간 조사대가 남긴 번호가 새겨져 있다.",Items.SOUL_LANTERN,ChatFormatting.AQUA),
            new Site("RQ_G02_moss_path","GLOAMWOOD","이끼 아래 옛길",new Vec3(-112,71,-366),"두꺼운 이끼 아래에서 오래된 석재 보행로가 드러난다. 숲은 대단절 전부터 사람이 다녔다.",Items.MOSS_BLOCK,ChatFormatting.DARK_GREEN),
            new Site("RQ_G03_root_sample","GLOAMWOOD","거대 뿌리 표본",new Vec3(92,70,-262),"뿌리 내부에 Relay 결정이 목질처럼 자라 있다. 베르나의 장벽과 같은 반응이다.",Items.HANGING_ROOTS,ChatFormatting.LIGHT_PURPLE),
            new Site("RQ_A01_pressure_valve","AQUEDUCT","보조 압력 밸브",new Vec3(-356,65,72),"주 수문과 별개인 보조 압력 밸브. 작동 흔적은 최근까지 이어져 있다.",Items.IRON_INGOT,ChatFormatting.GRAY),
            new Site("RQ_A02_rusted_message","AQUEDUCT","녹슨 정비 메시지",new Vec3(-450,64,-42),"수로 외벽에 남은 정비 문구. ORO 계열 명령이 물길보다 인원 통제를 우선했다고 적혀 있다.",Items.WRITABLE_BOOK,ChatFormatting.GOLD),
            new Site("RQ_A03_flood_cache","AQUEDUCT","침수 비상 저장고",new Vec3(-302,66,158),"무너진 고가 수로 아래 비상 저장고. 물이 빠지며 봉인이 드러났다.",Items.BARREL,ChatFormatting.BLUE),
            new Site("RQ_Q01_worker_tags","QUARRY","작업자 인식표 묶음",new Vec3(78,67,398),"폐쇄 당시 회수되지 못한 작업자 인식표. 몇 개는 라제와 같은 조의 번호다.",Items.NAME_TAG,ChatFormatting.GOLD),
            new Site("RQ_Q02_cooling_route","QUARRY","냉각수 우회로",new Vec3(-124,68,438),"용암 절단면 아래로 이어지는 냉각수 우회관. 오래된 수동 밸브가 아직 남아 있다.",Items.WATER_BUCKET,ChatFormatting.AQUA),
            new Site("RQ_Q03_old_tool","QUARRY","버려진 절단 공구",new Vec3(94,65,424),"급히 내려놓은 채 굳어 버린 절단 공구. 손잡이에 피난 방향을 긁어 표시했다.",Items.IRON_PICKAXE,ChatFormatting.RED));

    /** player -> actor uuid -> site. Discoveries are session-local until Region Quest objectives become fully canonical. */
    private static final Map<UUID,Map<UUID,Site>> ACTORS=new ConcurrentHashMap<>();

    private RegionQuestWorldSites(){}
    public static List<Site> sites(){return SITES;}

    public static void build(ServerLevel l){
        if(hasMarker(l)) return;
        for(Site s:SITES) buildScene(l,s);
        writeMarker(l);
    }

    public static void sync(ServerLevel l,ServerPlayer p){
        build(l);
        Map<UUID,Site> actors=ACTORS.computeIfAbsent(p.getUUID(),ignored->new LinkedHashMap<>());
        for(var e:List.copyOf(actors.entrySet())){
            Entity entity=l.getEntity(e.getKey());
            if(entity==null||p.position().distanceToSqr(e.getValue().position())>DESPAWN_RADIUS_SQ){if(entity!=null)entity.discard();actors.remove(e.getKey());}
        }
        for(Site s:SITES){
            if(p.position().distanceToSqr(s.position())>SPAWN_RADIUS_SQ||actors.containsValue(s))continue;
            ArmorStand a=new ArmorStand(l,s.position().x,s.position().y,s.position().z);a.setInvulnerable(true);a.setNoGravity(true);a.setShowArms(true);
            a.setCustomName(Component.literal(s.label()).withStyle(s.color()));a.setCustomNameVisible(true);a.setItemSlot(EquipmentSlot.MAINHAND,s.item().getDefaultInstance());
            l.addFreshEntity(a);actors.put(a.getUUID(),s);
        }
    }

    public static boolean interact(ServerPlayer p,Entity target){
        if(p==null||target==null)return false;Map<UUID,Site> actors=ACTORS.get(p.getUUID());if(actors==null)return false;Site s=actors.get(target.getUUID());if(s==null)return false;
        p.sendSystemMessage(Component.literal("지역 발견 · "+s.label()).withStyle(s.color(),ChatFormatting.BOLD));
        p.sendSystemMessage(Component.literal(s.lore()).withStyle(ChatFormatting.GRAY));
        p.sendSystemMessage(Component.literal(s.id()+" · 세부 목표는 v0.4 Canon Gap이므로 발견만 기록합니다.").withStyle(ChatFormatting.DARK_GRAY));
        return true;
    }

    public static void remove(ServerPlayer p){if(p==null||!(p.level() instanceof ServerLevel l))return;Map<UUID,Site> actors=ACTORS.remove(p.getUUID());if(actors==null)return;for(UUID id:actors.keySet()){Entity e=l.getEntity(id);if(e!=null)e.discard();}}

    private static void buildScene(ServerLevel l,Site s){int x=(int)Math.round(s.position().x),y=(int)Math.round(s.position().y)-1,z=(int)Math.round(s.position().z);switch(s.region()){
        case"SOUTHGATE"->meadow(l,x,y,z,s.id());case"GLOAMWOOD"->forest(l,x,y,z,s.id());case"AQUEDUCT"->aqueduct(l,x,y,z,s.id());case"QUARRY"->quarry(l,x,y,z,s.id());default->{}}
    }
    private static void meadow(ServerLevel l,int x,int y,int z,String id){pad(l,x,y,z,5,Blocks.GRASS_BLOCK);if(id.contains("cart")){for(int dx=-2;dx<=2;dx++)set(l,x+dx,y+1,z,Blocks.OAK_PLANKS);set(l,x-2,y+2,z,Blocks.OAK_FENCE);set(l,x+2,y+2,z,Blocks.OAK_FENCE);}else if(id.contains("scout")){for(int dy=1;dy<=4;dy++)set(l,x,y+dy,z,Blocks.COBBLESTONE_WALL);set(l,x,y+5,z,Blocks.LANTERN);}else{for(int a=0;a<6;a++){double r=a*Math.PI/3;set(l,x+(int)Math.round(Math.cos(r)*4),y+1,z+(int)Math.round(Math.sin(r)*4),Blocks.TNT);}set(l,x,y+1,z,Blocks.REDSTONE_TORCH);}}
    private static void forest(ServerLevel l,int x,int y,int z,String id){pad(l,x,y,z,5,Blocks.MOSS_BLOCK);if(id.contains("lantern")){set(l,x,y+1,z,Blocks.OAK_FENCE);set(l,x,y+2,z,Blocks.SOUL_LANTERN);}else if(id.contains("moss")){for(int dx=-5;dx<=5;dx++)set(l,x+dx,y,z,dx%2==0?Blocks.MOSSY_STONE_BRICKS:Blocks.MOSS_BLOCK);}else{for(int dx=-5;dx<=5;dx++)set(l,x+dx,y+1,z+(int)Math.round(Math.sin(dx*.7)*2),Blocks.DARK_OAK_LOG);set(l,x,y+2,z,Blocks.AMETHYST_BLOCK);}}
    private static void aqueduct(ServerLevel l,int x,int y,int z,String id){pad(l,x,y,z,5,Blocks.STONE_BRICKS);if(id.contains("valve")){set(l,x,y+1,z,Blocks.IRON_BLOCK);set(l,x,y+2,z,Blocks.REDSTONE_LAMP);}else if(id.contains("message")){for(int dy=1;dy<=5;dy++)set(l,x-4,y+dy,z,Blocks.MOSSY_STONE_BRICKS);set(l,x-3,y+1,z,Blocks.LECTERN);}else{for(int dx=-4;dx<=4;dx++)for(int dz=-3;dz<=3;dz++)if(Math.abs(dx)==4||Math.abs(dz)==3)set(l,x+dx,y+1,z+dz,Blocks.STONE_BRICKS);set(l,x,y+1,z,Blocks.BARREL);}}
    private static void quarry(ServerLevel l,int x,int y,int z,String id){pad(l,x,y,z,6,Blocks.TUFF);if(id.contains("tags")){set(l,x,y+1,z,Blocks.BARREL);for(int dx=-3;dx<=3;dx+=3)set(l,x+dx,y+1,z+3,Blocks.IRON_BARS);}else if(id.contains("cooling")){for(int dx=-5;dx<=5;dx++)set(l,x+dx,y,z,Blocks.WATER);for(int dx=-5;dx<=5;dx+=5)set(l,x+dx,y+1,z,Blocks.IRON_BARS);}else{set(l,x,y+1,z,Blocks.ANVIL);set(l,x+2,y+1,z,Blocks.IRON_BLOCK);set(l,x-2,y+1,z,Blocks.BLAST_FURNACE);}}
    private static void pad(ServerLevel l,int cx,int y,int cz,int r,Block top){for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){if(dx*dx+dz*dz>r*r)continue;for(int fy=y-2;fy<y;fy++)set(l,cx+dx,fy,cz+dz,Blocks.DIRT);set(l,cx+dx,y,cz+dz,top);for(int ay=y+1;ay<=y+4;ay++)set(l,cx+dx,ay,cz+dz,Blocks.AIR);}}
    private static boolean hasMarker(ServerLevel l){return l.getBlockState(new BlockPos(MARKER_X,MARKER_Y,MARKER_Z)).is(Blocks.LODESTONE)&&l.getBlockState(new BlockPos(MARKER_X+1,MARKER_Y,MARKER_Z)).is(Blocks.CHEST)&&l.getBlockState(new BlockPos(MARKER_X+2,MARKER_Y,MARKER_Z)).is(Blocks.MOSS_BLOCK);}
    private static void writeMarker(ServerLevel l){set(l,MARKER_X,MARKER_Y,MARKER_Z,Blocks.LODESTONE);set(l,MARKER_X+1,MARKER_Y,MARKER_Z,Blocks.CHEST);set(l,MARKER_X+2,MARKER_Y,MARKER_Z,Blocks.MOSS_BLOCK);}
    private static void set(ServerLevel l,int x,int y,int z,Block b){l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);}
}
