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

/** Optional non-reward lore discoveries that give the authored map exploration density without changing economy. */
public final class ExplorationCodexSites {
    private enum Theme { RADIA, SOUTHGATE, GLOAMWOOD, AQUEDUCT, QUARRY, RELAY }
    private record Lore(String title, Vec3 pos, String text, Item item, ChatFormatting color, Theme theme) {}

    private static final int MARKER_X=466,MARKER_Y=46,MARKER_Z=-92;
    private static final List<Lore> LORE=List.of(
            new Lore("라디아 외벽 Relay 표식",new Vec3(112,66,-92),"라디아 외벽의 오래된 계전 표식. 다섯 방향으로 갈라지는 선이 한 점에서 끊겨 있다.",Items.COMPASS,ChatFormatting.AQUA,Theme.RADIA),

            new Lore("남문 감시탑 마지막 교대",new Vec3(154,67,326),"감시탑 교대판에는 그라울 출현 전날부터 순찰 복귀 시간이 점점 늦어졌다는 기록이 남아 있다.",Items.SPYGLASS,ChatFormatting.YELLOW,Theme.SOUTHGATE),
            new Lore("초원 순찰대 야영 흔적",new Vec3(58,67,200),"다시 불을 피울 수 있을 만큼 정돈된 야영지다. 급히 철수했지만 다음 순찰대를 위한 장작과 물통은 남겨 두었다.",Items.CAMPFIRE,ChatFormatting.GOLD,Theme.SOUTHGATE),
            new Lore("그라울의 돌진 자국",new Vec3(326,68,261),"울타리와 돌무더기가 같은 방향으로 밀려 있다. 이곳을 지난 것은 단순한 야수 한 마리가 아니었다.",Items.BONE,ChatFormatting.RED,Theme.SOUTHGATE),

            new Lore("그늘숲 삼켜진 옛길",new Vec3(-8,70,-286),"이끼 아래의 돌길은 숲보다 오래됐다. 뿌리는 길을 부수지 않고 오히려 그 선을 따라 자라고 있다.",Items.MOSS_BLOCK,ChatFormatting.DARK_GREEN,Theme.GLOAMWOOD),
            new Lore("버려진 포자 조사 야영지",new Vec3(-68,70,-226),"조사 도구는 가지런하지만 식량만 사라졌다. 야영지를 버린 사람들은 싸우기보다 더 깊은 숲을 향해 움직였다.",Items.SHEARS,ChatFormatting.GREEN,Theme.GLOAMWOOD),
            new Lore("가시어미의 외곽 꽃밭",new Vec3(-57,72,-424),"주변의 작은 꽃들이 모두 같은 방향으로 고개를 돌리고 있다. 숲의 중심이 어디인지 말없이 가리키는 듯하다.",Items.FLOWERING_AZALEA,ChatFormatting.LIGHT_PURPLE,Theme.GLOAMWOOD),

            new Lore("붕괴 수로 감시 난간",new Vec3(-292,66,61),"난간 아래의 물길과 명령선 표식이 서로 다른 방향을 가리킨다. 시설은 물보다 통제를 먼저 설계한 흔적이 있다.",Items.IRON_INGOT,ChatFormatting.GRAY,Theme.AQUEDUCT),
            new Lore("정비반 휴게 벽감",new Vec3(-162,66,4),"짧은 교대 시간을 위해 만든 작은 벽감이다. 벽면에는 손바닥 높이마다 작업자들이 남긴 눈금이 새겨져 있다.",Items.WRITABLE_BOOK,ChatFormatting.YELLOW,Theme.AQUEDUCT),
            new Lore("ORO 보안문 잔해",new Vec3(-413,64,53),"수문보다 안쪽을 향해 세워진 보안문. 외부 침입보다 내부 이탈을 막는 데 더 적합한 구조다.",Items.REDSTONE,ChatFormatting.RED,Theme.AQUEDUCT),

            new Lore("잿불 채석장 냉각 가설대",new Vec3(2,69,389),"고열 구간을 가로지르는 냉각 가설대. 물이 끊긴 뒤에도 작업자들은 빈 배관을 표지처럼 남겼다.",Items.WATER_BUCKET,ChatFormatting.BLUE,Theme.QUARRY),
            new Lore("채석장 교대 휴게지",new Vec3(-95,69,345),"작업복을 털어 놓던 작은 휴게지. 벽에 그어진 교대표는 마지막 날까지 평소와 똑같이 이어진다.",Items.NAME_TAG,ChatFormatting.GOLD,Theme.QUARRY),
            new Lore("콜바크 경고 가설문",new Vec3(54,63,438),"상부 가설문에 남은 그을음은 바깥쪽보다 안쪽이 짙다. 거상은 아래에서 올라왔던 것 같다.",Items.IRON_PICKAXE,ChatFormatting.RED,Theme.QUARRY),

            new Lore("구 중계소 외곽 신호문",new Vec3(282,68,-196),"외곽 신호문은 중계소 안쪽이 아니라 동쪽 먼 곳을 향해 정렬돼 있다. 끊긴 선 뒤에도 신호 방향만은 남았다.",Items.RECOVERY_COMPASS,ChatFormatting.LIGHT_PURPLE,Theme.RELAY),
            new Lore("중계소 임시 구호 구역",new Vec3(336,68,-258),"정비 부품 사이에 응급 처치 도구가 섞여 있다. 이곳은 어느 순간 기계보다 사람을 먼저 고치는 장소가 됐다.",Items.GOLDEN_APPLE,ChatFormatting.AQUA,Theme.RELAY),
            new Lore("세라크 관측 척추",new Vec3(411,66,-347),"보스실로 이어지는 좁은 관측로. 모든 측정 장치가 한 점이 아니라 여러 방향의 균열을 동시에 기록하도록 배치돼 있다.",Items.CLOCK,ChatFormatting.DARK_PURPLE,Theme.RELAY));

    private static final Map<UUID,Map<UUID,Lore>> ACTORS=new ConcurrentHashMap<>();
    private ExplorationCodexSites(){}

    public static void build(ServerLevel l){if(hasMarker(l))return;for(Lore lore:LORE)scene(l,lore);writeMarker(l);}

    public static void sync(ServerLevel l,ServerPlayer p){
        build(l);
        Map<UUID,Lore>a=ACTORS.computeIfAbsent(p.getUUID(),x->new LinkedHashMap<>());
        for(var e:List.copyOf(a.entrySet())){
            Entity entity=l.getEntity(e.getKey());
            if(entity==null||p.position().distanceToSqr(e.getValue().pos())>10000){if(entity!=null)entity.discard();a.remove(e.getKey());}
        }
        for(Lore lore:LORE){
            if(p.position().distanceToSqr(lore.pos())>6400||a.containsValue(lore))continue;
            ArmorStand s=new ArmorStand(l,lore.pos().x,lore.pos().y,lore.pos().z);
            s.setInvulnerable(true);s.setNoGravity(true);s.setShowArms(true);
            s.setCustomName(Component.literal(lore.title()).withStyle(lore.color()));s.setCustomNameVisible(true);
            s.setItemSlot(EquipmentSlot.MAINHAND,lore.item().getDefaultInstance());l.addFreshEntity(s);a.put(s.getUUID(),lore);
        }
    }

    public static boolean interact(ServerPlayer p,Entity target){
        Map<UUID,Lore>a=ACTORS.get(p.getUUID());if(a==null)return false;Lore lore=a.get(target.getUUID());if(lore==null)return false;
        p.sendSystemMessage(Component.literal("Aster March 기록 · "+lore.title()).withStyle(lore.color(),ChatFormatting.BOLD));
        p.sendSystemMessage(Component.literal(lore.text()).withStyle(ChatFormatting.GRAY));return true;
    }

    public static void remove(ServerPlayer p){
        if(p==null||!(p.level() instanceof ServerLevel l))return;Map<UUID,Lore>a=ACTORS.remove(p.getUUID());if(a==null)return;
        for(UUID id:a.keySet()){Entity e=l.getEntity(id);if(e!=null)e.discard();}
    }

    private static void scene(ServerLevel l,Lore lore){
        int x=(int)Math.round(lore.pos().x),y=(int)Math.round(lore.pos().y)-1,z=(int)Math.round(lore.pos().z);
        switch(lore.theme()){
            case RADIA -> radiaScene(l,x,y,z);
            case SOUTHGATE -> southgateScene(l,x,y,z);
            case GLOAMWOOD -> gloamScene(l,x,y,z);
            case AQUEDUCT -> aqueductScene(l,x,y,z);
            case QUARRY -> quarryScene(l,x,y,z);
            case RELAY -> relayScene(l,x,y,z);
        }
    }

    private static void radiaScene(ServerLevel l,int x,int y,int z){
        disc(l,x,y,z,3,Blocks.POLISHED_ANDESITE,Blocks.STONE_BRICKS);
        set(l,x,y+1,z,Blocks.CHISELED_STONE_BRICKS);set(l,x,y+2,z,Blocks.LANTERN);
    }

    private static void southgateScene(ServerLevel l,int x,int y,int z){
        disc(l,x,y,z,3,Blocks.DIRT_PATH,Blocks.GRASS_BLOCK);
        set(l,x,y+1,z,Blocks.OAK_FENCE);set(l,x,y+2,z,Blocks.LANTERN);
        set(l,x-2,y+1,z+2,Blocks.COBBLESTONE_WALL);set(l,x+2,y+1,z-2,Blocks.COBBLESTONE_WALL);
    }

    private static void gloamScene(ServerLevel l,int x,int y,int z){
        disc(l,x,y,z,3,Blocks.MOSS_BLOCK,Blocks.PODZOL);
        set(l,x,y+1,z,Blocks.DARK_OAK_LOG);set(l,x,y+2,z,Blocks.SOUL_LANTERN);
        set(l,x-2,y+1,z,Blocks.MOSS_CARPET);set(l,x+2,y+1,z,Blocks.BROWN_MUSHROOM);
    }

    private static void aqueductScene(ServerLevel l,int x,int y,int z){
        disc(l,x,y,z,3,Blocks.STONE_BRICKS,Blocks.CRACKED_STONE_BRICKS);
        set(l,x,y+1,z,Blocks.IRON_BARS);set(l,x,y+2,z,Blocks.REDSTONE_LAMP);
        set(l,x-2,y+1,z+1,Blocks.MOSSY_STONE_BRICKS);set(l,x+2,y+1,z-1,Blocks.IRON_BLOCK);
    }

    private static void quarryScene(ServerLevel l,int x,int y,int z){
        disc(l,x,y,z,3,Blocks.TUFF,Blocks.BASALT);
        set(l,x,y+1,z,Blocks.CHAIN);set(l,x,y+2,z,Blocks.LANTERN);
        set(l,x-2,y+1,z,Blocks.MAGMA_BLOCK);set(l,x+2,y+1,z,Blocks.BLACKSTONE);
    }

    private static void relayScene(ServerLevel l,int x,int y,int z){
        disc(l,x,y,z,3,Blocks.DEEPSLATE_TILES,Blocks.DEEPSLATE_BRICKS);
        set(l,x,y+1,z,Blocks.CRYING_OBSIDIAN);set(l,x,y+2,z,Blocks.SOUL_LANTERN);
        set(l,x-2,y+1,z,Blocks.IRON_BARS);set(l,x+2,y+1,z,Blocks.AMETHYST_BLOCK);
    }

    private static void disc(ServerLevel l,int cx,int y,int cz,int r,Block a,Block b){
        for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(dx*dx+dz*dz>r*r)continue;set(l,cx+dx,y,cz+dz,Math.floorMod(dx*13+dz*7,4)==0?b:a);
        }
    }

    /** Bookshelf marker intentionally replaces the previous v1 marker so existing worlds receive the expanded sites once. */
    private static boolean hasMarker(ServerLevel l){return l.getBlockState(new BlockPos(MARKER_X,MARKER_Y,MARKER_Z)).is(Blocks.LODESTONE)&&l.getBlockState(new BlockPos(MARKER_X+1,MARKER_Y,MARKER_Z)).is(Blocks.LECTERN)&&l.getBlockState(new BlockPos(MARKER_X+2,MARKER_Y,MARKER_Z)).is(Blocks.BOOKSHELF);}
    private static void writeMarker(ServerLevel l){set(l,MARKER_X,MARKER_Y,MARKER_Z,Blocks.LODESTONE);set(l,MARKER_X+1,MARKER_Y,MARKER_Z,Blocks.LECTERN);set(l,MARKER_X+2,MARKER_Y,MARKER_Z,Blocks.BOOKSHELF);}
    private static void set(ServerLevel l,int x,int y,int z,Block b){l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);}
}
