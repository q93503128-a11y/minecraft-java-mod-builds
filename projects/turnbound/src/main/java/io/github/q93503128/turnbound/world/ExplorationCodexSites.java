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
    private record Lore(String title,Vec3 pos,String text,Item item,ChatFormatting color){}
    private static final int MARKER_X=466,MARKER_Y=46,MARKER_Z=-92;
    private static final List<Lore> LORE=List.of(
            new Lore("라디아 외벽 Relay 표식",new Vec3(112,66,-92),"라디아 외벽의 오래된 계전 표식. 다섯 지역으로 갈라지는 전력선이 한 점에서 끊겨 있다.",Items.COMPASS,ChatFormatting.AQUA),
            new Lore("남문 감시탑 마지막 교대",new Vec3(154,67,326),"감시탑 교대판에는 그라울 출현 전날부터 순찰 복귀 시간이 점점 늦어졌다는 기록이 남아 있다.",Items.SPYGLASS,ChatFormatting.YELLOW),
            new Lore("그늘숲 뿌리 아치",new Vec3(92,70,-262),"나무 뿌리가 돌 아치를 삼킨 것이 아니라, Relay 결정이 뿌리의 성장 방향을 바꿔 아치를 만들었다.",Items.AMETHYST_SHARD,ChatFormatting.DARK_GREEN),
            new Lore("붕괴 수로 상부 표지",new Vec3(-302,66,158),"상부 수로 표지에는 물길보다 ORO 명령선이 먼저 복구 대상으로 지정되어 있다.",Items.IRON_INGOT,ChatFormatting.GRAY),
            new Lore("잿불 채석장 냉각 기록",new Vec3(-124,68,438),"냉각수 공급표가 사고 시각에 수동 우회로로 바뀌었다. 누군가 마지막까지 밸브를 지켰다.",Items.WATER_BUCKET,ChatFormatting.BLUE),
            new Lore("구 중계소 외부 신호주",new Vec3(244,68,-164),"세라크가 남긴 신호주는 내부를 향하지 않는다. 동쪽 외부의 더 먼 Relay를 계속 바라보고 있다.",Items.RECOVERY_COMPASS,ChatFormatting.LIGHT_PURPLE));
    private static final Map<UUID,Map<UUID,Lore>> ACTORS=new ConcurrentHashMap<>();
    private ExplorationCodexSites(){}
    public static void build(ServerLevel l){if(hasMarker(l))return;for(Lore lore:LORE)scene(l,lore);writeMarker(l);}
    public static void sync(ServerLevel l,ServerPlayer p){build(l);Map<UUID,Lore>a=ACTORS.computeIfAbsent(p.getUUID(),x->new LinkedHashMap<>());for(var e:List.copyOf(a.entrySet())){Entity entity=l.getEntity(e.getKey());if(entity==null||p.position().distanceToSqr(e.getValue().pos())>10000){if(entity!=null)entity.discard();a.remove(e.getKey());}}for(Lore lore:LORE){if(p.position().distanceToSqr(lore.pos())>6400||a.containsValue(lore))continue;ArmorStand s=new ArmorStand(l,lore.pos().x,lore.pos().y,lore.pos().z);s.setInvulnerable(true);s.setNoGravity(true);s.setShowArms(true);s.setCustomName(Component.literal(lore.title()).withStyle(lore.color()));s.setCustomNameVisible(true);s.setItemSlot(EquipmentSlot.MAINHAND,lore.item().getDefaultInstance());l.addFreshEntity(s);a.put(s.getUUID(),lore);}}
    public static boolean interact(ServerPlayer p,Entity target){Map<UUID,Lore>a=ACTORS.get(p.getUUID());if(a==null)return false;Lore lore=a.get(target.getUUID());if(lore==null)return false;p.sendSystemMessage(Component.literal("Aster March 기록 · "+lore.title()).withStyle(lore.color(),ChatFormatting.BOLD));p.sendSystemMessage(Component.literal(lore.text()).withStyle(ChatFormatting.GRAY));return true;}
    public static void remove(ServerPlayer p){if(p==null||!(p.level() instanceof ServerLevel l))return;Map<UUID,Lore>a=ACTORS.remove(p.getUUID());if(a==null)return;for(UUID id:a.keySet()){Entity e=l.getEntity(id);if(e!=null)e.discard();}}
    private static void scene(ServerLevel l,Lore lore){int x=(int)lore.pos().x,y=(int)lore.pos().y-1,z=(int)lore.pos().z;for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++){if(dx*dx+dz*dz>10)continue;set(l,x+dx,y,z+dz,Blocks.POLISHED_ANDESITE);}set(l,x,y+1,z,Blocks.CHISELED_STONE_BRICKS);set(l,x,y+2,z,Blocks.LANTERN);}
    private static boolean hasMarker(ServerLevel l){return l.getBlockState(new BlockPos(MARKER_X,MARKER_Y,MARKER_Z)).is(Blocks.LODESTONE)&&l.getBlockState(new BlockPos(MARKER_X+1,MARKER_Y,MARKER_Z)).is(Blocks.LECTERN)&&l.getBlockState(new BlockPos(MARKER_X+2,MARKER_Y,MARKER_Z)).is(Blocks.AMETHYST_BLOCK);}
    private static void writeMarker(ServerLevel l){set(l,MARKER_X,MARKER_Y,MARKER_Z,Blocks.LODESTONE);set(l,MARKER_X+1,MARKER_Y,MARKER_Z,Blocks.LECTERN);set(l,MARKER_X+2,MARKER_Y,MARKER_Z,Blocks.AMETHYST_BLOCK);}
    private static void set(ServerLevel l,int x,int y,int z,Block b){l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);}
}
