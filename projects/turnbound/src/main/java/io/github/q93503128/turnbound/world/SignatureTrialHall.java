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
 * Physical Signature/Awakening hall. Exact trial enemy rosters are Canon Gaps, so this hall exposes readiness,
 * requirements and locked seals without fabricating battles. Once a trial roster becomes canonical the seal is the
 * stable physical entry point that can be wired to it.
 */
public final class SignatureTrialHall {
    public record Seal(String characterId,String title,Vec3 pos,String requirement,Item item,ChatFormatting color){}
    private static final int MARKER_X=88,MARKER_Y=55,MARKER_Z=92;
    private static final Vec3 CENTER=new Vec3(88,66,92);
    private static final List<Seal> SEALS=List.of(
            new Seal("P01","결투의 잔향",new Vec3(72,66,86),"P01 ★6 Lv60 · Character Quest · 2인 이하 · 특수 엘리트",Items.IRON_SWORD,ChatFormatting.AQUA),
            new Seal("P02","P02 Signature Trial",new Vec3(80,66,86),"P02 ★6 Lv60 · SPD≤80 아군 2명 · 22 행동 이내",Items.CLOCK,ChatFormatting.AQUA),
            new Seal("P03","P03 Signature Trial",new Vec3(88,66,86),"P03 ★6 Lv60 · 지정 NPC 10 적 행동 생존",Items.SHIELD,ChatFormatting.GREEN),
            new Seal("P04","P04 Signature Trial",new Vec3(96,66,86),"P04 ★6 Lv60 · 아군 사망 후 최종 전원 생존",Items.GOLDEN_APPLE,ChatFormatting.GOLD),
            new Seal("P05","P05 Signature Trial",new Vec3(104,66,86),"P05 ★6 Lv60 · Follow-up 10 · 25 행동 이내",Items.CROSSBOW,ChatFormatting.YELLOW),
            new Seal("P06","P06 Signature Trial",new Vec3(76,66,98),"P06 ★6 Lv60 · 자가부활 · 기억 5 이상",Items.WRITABLE_BOOK,ChatFormatting.LIGHT_PURPLE),
            new Seal("P07","P07 Signature Trial",new Vec3(88,66,98),"P07 ★6 Lv60 · Toto 사망/재소환 · Marion 생존",Items.NAME_TAG,ChatFormatting.BLUE),
            new Seal("P08","P08 Signature Trial",new Vec3(100,66,98),"P08 ★6 Lv60 · HP 1 생존 · 종료 HP≤30%",Items.IRON_AXE,ChatFormatting.RED));
    private static final Map<UUID,Map<UUID,Seal>> ACTORS=new ConcurrentHashMap<>();
    private SignatureTrialHall(){}

    public static void build(ServerLevel l){if(hasMarker(l))return;hall(l);writeMarker(l);}
    public static void sync(ServerLevel l,ServerPlayer p){build(l);Map<UUID,Seal>a=ACTORS.computeIfAbsent(p.getUUID(),x->new LinkedHashMap<>());boolean active=CampaignContentUnlocks.signatureActual(p.getUUID())&&RadiaHubSessionManager.active(p)&&p.position().distanceToSqr(CENTER)<5200;if(!active){despawn(l,a);return;}for(var e:List.copyOf(a.entrySet()))if(l.getEntity(e.getKey())==null)a.remove(e.getKey());for(Seal s:SEALS){if(a.containsValue(s))continue;ArmorStand stand=new ArmorStand(l,s.pos().x,s.pos().y,s.pos().z);stand.setInvulnerable(true);stand.setNoGravity(true);stand.setShowArms(true);stand.setCustomName(Component.literal(s.title()).withStyle(s.color(),ChatFormatting.BOLD));stand.setCustomNameVisible(true);stand.setItemSlot(EquipmentSlot.MAINHAND,s.item().getDefaultInstance());l.addFreshEntity(stand);a.put(stand.getUUID(),s);}}
    public static boolean interact(ServerPlayer p,Entity target){Map<UUID,Seal>a=ACTORS.get(p.getUUID());if(a==null)return false;Seal s=a.get(target.getUUID());if(s==null)return false;var growth=CampaignProgressStore.growth(p.getUUID(),s.characterId());var level=CampaignProgressStore.character(p.getUUID(),s.characterId());boolean cq=growth.characterQuestComplete();boolean ready=level.level()==60&&growth.currentStar()==6&&cq&&!growth.signatureTrialCleared();p.sendSystemMessage(Component.literal(s.title()+" · "+s.requirement()).withStyle(s.color()));if(growth.signatureTrialCleared()){p.sendSystemMessage(Component.literal("첫 클리어 완료 · 전용 장비/각성 Core 획득 기록 있음").withStyle(ChatFormatting.GREEN));}else if(ready){p.sendSystemMessage(Component.literal("기본 입장 조건 충족 · 전투 편성 정본이 아직 없어 Trial seal 대기 상태").withStyle(ChatFormatting.YELLOW));}else{p.sendSystemMessage(Component.literal("필요: Character Quest 완료 + ★6 + Lv60").withStyle(ChatFormatting.GRAY));}return true;}
    public static void remove(ServerPlayer p){if(p==null||!(p.level() instanceof ServerLevel l))return;Map<UUID,Seal>a=ACTORS.remove(p.getUUID());if(a!=null)despawn(l,a);}
    private static void hall(ServerLevel l){for(int x=66;x<=110;x++)for(int z=80;z<=104;z++){boolean edge=x==66||x==110||z==80||z==104;for(int y=62;y<=64;y++)set(l,x,y,z,Blocks.STONE);set(l,x,65,z,edge?Blocks.POLISHED_BLACKSTONE_BRICKS:(((x+z)&1)==0?Blocks.POLISHED_ANDESITE:Blocks.STONE_BRICKS));for(int y=66;y<=73;y++)set(l,x,y,z,Blocks.AIR);if(edge&&((x+z)&2)==0)for(int y=66;y<=70;y++)set(l,x,y,z,Blocks.STONE_BRICKS);}for(Seal s:SEALS){int x=(int)s.pos().x,z=(int)s.pos().z;for(int dx=-2;dx<=2;dx++)for(int dz=-1;dz<=1;dz++)set(l,x+dx,65,z+dz,Blocks.POLISHED_DEEPSLATE);set(l,x,66,z,Blocks.AMETHYST_BLOCK);}for(int x=84;x<=92;x++)set(l,x,65,104,Blocks.AIR);set(l,88,66,81,Blocks.BEACON);}
    private static void despawn(ServerLevel l,Map<UUID,Seal>a){for(UUID id:List.copyOf(a.keySet())){Entity e=l.getEntity(id);if(e!=null)e.discard();a.remove(id);}}
    private static boolean hasMarker(ServerLevel l){return l.getBlockState(new BlockPos(MARKER_X,MARKER_Y,MARKER_Z)).is(Blocks.LODESTONE)&&l.getBlockState(new BlockPos(MARKER_X+1,MARKER_Y,MARKER_Z)).is(Blocks.BEACON)&&l.getBlockState(new BlockPos(MARKER_X+2,MARKER_Y,MARKER_Z)).is(Blocks.AMETHYST_BLOCK);}
    private static void writeMarker(ServerLevel l){set(l,MARKER_X,MARKER_Y,MARKER_Z,Blocks.LODESTONE);set(l,MARKER_X+1,MARKER_Y,MARKER_Z,Blocks.BEACON);set(l,MARKER_X+2,MARKER_Y,MARKER_Z,Blocks.AMETHYST_BLOCK);}
    private static void set(ServerLevel l,int x,int y,int z,Block b){l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);}
}
