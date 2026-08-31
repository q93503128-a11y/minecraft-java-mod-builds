package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.session.BattleSessionManager;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Physical post-story Rift Gate / Hard Boss selection hall inside Radia. */
public final class RadiaEndgameAtrium {
    public record Selector(String encounterId, String label, Vec3 position, String type, int level, boolean milestone) {}

    private static final int MARKER_X=-118, MARKER_Y=55, MARKER_Z=-104;
    private static final Vec3 CENTER = new Vec3(-100.0,66.0,-88.0);
    private static final double ACTIVE_RADIUS_SQ = 74.0 * 74.0;
    private static final List<Selector> SELECTORS = selectors();
    private static final Map<UUID, Map<UUID, Selector>> ACTORS = new ConcurrentHashMap<>();

    private RadiaEndgameAtrium() {}

    public static List<Selector> selectorsView(){ return SELECTORS; }

    public static void build(ServerLevel l){
        if(hasMarker(l)) return;
        floor(l); entrance(l); hardGallery(l); riftGrid(l); milestoneArches(l); writeMarker(l);
    }

    public static void sync(ServerLevel l, ServerPlayer p){
        build(l);
        UUID pid=p.getUUID();
        Map<UUID,Selector> actors=ACTORS.computeIfAbsent(pid, ignored->new LinkedHashMap<>());
        boolean active=CampaignContentUnlocks.endgame(pid) && RadiaHubSessionManager.active(p)
                && p.position().distanceToSqr(CENTER)<=ACTIVE_RADIUS_SQ;
        if(!active){ despawn(l,actors); return; }

        for(var entry:List.copyOf(actors.entrySet())){
            if(l.getEntity(entry.getKey())==null) actors.remove(entry.getKey());
        }
        for(Selector s:SELECTORS){
            if(actors.containsValue(s)) continue;
            ArmorStand a=new ArmorStand(l,s.position().x,s.position().y,s.position().z);
            a.setInvulnerable(true);a.setNoGravity(true);a.setShowArms(true);
            ChatFormatting c=s.type().equals("HARD")?ChatFormatting.RED:(s.milestone()?ChatFormatting.LIGHT_PURPLE:ChatFormatting.AQUA);
            a.setCustomName(Component.literal(s.label()).withStyle(c,ChatFormatting.BOLD));a.setCustomNameVisible(true);
            a.setItemSlot(EquipmentSlot.MAINHAND,itemFor(s).getDefaultInstance());
            l.addFreshEntity(a);actors.put(a.getUUID(),s);
        }
    }

    public static boolean interact(ServerPlayer p,Entity target){
        if(p==null||target==null) return false;
        Map<UUID,Selector> actors=ACTORS.get(p.getUUID()); if(actors==null) return false;
        Selector s=actors.get(target.getUUID()); if(s==null) return false;
        if(!RadiaHubSessionManager.active(p)){p.sendSystemMessage(Component.literal("TURNBOUND · Endgame 입장은 라디아에서만 가능합니다."));return true;}
        if(!EndgameEncounterCatalog.unlocked(p.getUUID(),s.encounterId())){p.sendSystemMessage(Component.literal("TURNBOUND · 아직 잠긴 Endgame 전투입니다.").withStyle(ChatFormatting.GRAY));return true;}
        p.sendSystemMessage(Component.literal(s.label()+" · 권장/적 레벨 "+s.level()).withStyle(s.type().equals("HARD")?ChatFormatting.RED:ChatFormatting.AQUA));
        BattleSessionManager.startEncounter(p,s.encounterId());
        return true;
    }

    public static void remove(ServerPlayer p){
        if(p==null||!(p.level() instanceof ServerLevel l)) return;
        Map<UUID,Selector> actors=ACTORS.remove(p.getUUID()); if(actors!=null) despawn(l,actors);
    }

    private static List<Selector> selectors(){
        ArrayList<Selector> out=new ArrayList<>();
        int[] hardLv={6,10,13,16,20};
        for(int i=1;i<=5;i++){
            String boss="B0"+i; String id=EndgameEncounterCatalog.hardId(boss);
            out.add(new Selector(id,CanonicalData.definition(boss).name()+" · Hard",new Vec3(-116+(i-1)*8,66,-103),"HARD",hardLv[i-1]+5,true));
        }
        for(int floor=1;floor<=30;floor++){
            int index=floor-1,col=index%5,row=index/5;
            V04Catalogs.RiftFloor spec=V04Catalogs.riftFloor(floor);
            out.add(new Selector(EndgameEncounterCatalog.riftId(floor),"Rift F"+floor+" · Lv"+spec.level(),
                    new Vec3(-116+col*8,66,-94+row*4),"RIFT",spec.level(),spec.hardBossPattern()||floor%10==0));
        }
        return List.copyOf(out);
    }

    private static Item itemFor(Selector s){
        if(s.type().equals("HARD")) return Items.NETHERITE_SWORD;
        return s.milestone()?Items.AMETHYST_SHARD:Items.ENDER_EYE;
    }

    private static void floor(ServerLevel l){
        for(int x=-124;x<=-76;x++) for(int z=-108;z<=-69;z++){
            set(l,x,62,z,Blocks.DEEPSLATE);set(l,x,63,z,Blocks.DEEPSLATE);set(l,x,64,z,Blocks.DEEPSLATE);
            boolean rim=x==-124||x==-76||z==-108||z==-69;
            set(l,x,65,z,rim?Blocks.POLISHED_BLACKSTONE_BRICKS:(((x+z)&3)==0?Blocks.DEEPSLATE_TILES:Blocks.POLISHED_DEEPSLATE));
            for(int y=66;y<=74;y++) set(l,x,y,z,Blocks.AIR);
            if(rim&&((x+z)&1)==0) for(int y=66;y<=71;y++) set(l,x,y,z,Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
    }

    private static void entrance(ServerLevel l){
        for(int z=-69;z<=-58;z++) for(int x=-86;x<=-78;x++){
            set(l,x,65,z,Blocks.POLISHED_DEEPSLATE);for(int y=66;y<=71;y++) set(l,x,y,z,Blocks.AIR);
        }
        for(int y=66;y<=73;y++){set(l,-87,y,-68,Blocks.OBSIDIAN);set(l,-77,y,-68,Blocks.OBSIDIAN);}
        for(int x=-87;x<=-77;x++) set(l,x,73,-68,Blocks.CRYING_OBSIDIAN);
        set(l,-82,66,-69,Blocks.AMETHYST_BLOCK);
    }

    private static void hardGallery(ServerLevel l){
        for(int i=0;i<5;i++){
            int x=-116+i*8,z=-103; pedestal(l,x,65,z,Blocks.POLISHED_BLACKSTONE,hardCore(i));
            for(int y=66;y<=70;y++){set(l,x-3,y,z-3,Blocks.DEEPSLATE_BRICKS);set(l,x+3,y,z-3,Blocks.DEEPSLATE_BRICKS);}
            set(l,x-3,71,z-3,Blocks.SOUL_LANTERN);set(l,x+3,71,z-3,Blocks.SOUL_LANTERN);
        }
    }

    private static Block hardCore(int i){return switch(i){case 0->Blocks.MOSSY_STONE_BRICKS;case 1->Blocks.MOSS_BLOCK;case 2->Blocks.IRON_BLOCK;case 3->Blocks.MAGMA_BLOCK;default->Blocks.CRYING_OBSIDIAN;};}

    private static void riftGrid(ServerLevel l){
        for(int f=1;f<=30;f++){
            int index=f-1,col=index%5,row=index/5,x=-116+col*8,z=-94+row*4;
            boolean mark=V04Catalogs.riftFloor(f).hardBossPattern()||f%10==0;
            pedestal(l,x,65,z,mark?Blocks.CRYING_OBSIDIAN:Blocks.POLISHED_DEEPSLATE,mark?Blocks.AMETHYST_BLOCK:Blocks.OBSIDIAN);
        }
    }

    private static void milestoneArches(ServerLevel l){
        for(int row:new int[]{1,3,5}){
            int z=-94+row*4;
            for(int y=66;y<=72;y++){set(l,-121,y,z,Blocks.OBSIDIAN);set(l,-79,y,z,Blocks.OBSIDIAN);}
            set(l,-121,73,z,Blocks.SOUL_LANTERN);set(l,-79,73,z,Blocks.SOUL_LANTERN);
        }
        for(int x=-108;x<=-92;x++) set(l,x,65,-71,Blocks.AMETHYST_BLOCK);
        set(l,-100,66,-71,Blocks.BEACON);
    }

    private static void pedestal(ServerLevel l,int x,int y,int z,Block base,Block core){
        for(int dx=-2;dx<=2;dx++) for(int dz=-1;dz<=1;dz++) set(l,x+dx,y,z+dz,base);
        set(l,x,y+1,z,core);
    }

    private static void despawn(ServerLevel l,Map<UUID,Selector> actors){
        for(UUID id:List.copyOf(actors.keySet())){Entity e=l.getEntity(id);if(e!=null)e.discard();actors.remove(id);}
    }

    private static boolean hasMarker(ServerLevel l){return l.getBlockState(new BlockPos(MARKER_X,MARKER_Y,MARKER_Z)).is(Blocks.LODESTONE)&&l.getBlockState(new BlockPos(MARKER_X+1,MARKER_Y,MARKER_Z)).is(Blocks.CRYING_OBSIDIAN)&&l.getBlockState(new BlockPos(MARKER_X+2,MARKER_Y,MARKER_Z)).is(Blocks.AMETHYST_BLOCK);}
    private static void writeMarker(ServerLevel l){set(l,MARKER_X,MARKER_Y,MARKER_Z,Blocks.LODESTONE);set(l,MARKER_X+1,MARKER_Y,MARKER_Z,Blocks.CRYING_OBSIDIAN);set(l,MARKER_X+2,MARKER_Y,MARKER_Z,Blocks.AMETHYST_BLOCK);}
    private static void set(ServerLevel l,int x,int y,int z,Block b){l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);}
}
