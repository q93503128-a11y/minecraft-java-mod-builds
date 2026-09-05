package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Physical post-story Rift Gate, Normal/Hard rematch hall and Challenge board inside Radia. */
public final class RadiaEndgameAtrium {
    public record Selector(String encounterId, String label, Vec3 position, String type, int level, boolean milestone) {}

    private static final int MARKER_X=-118, MARKER_Y=55, MARKER_Z=-104;
    private static final Vec3 CENTER = new Vec3(-100.0,66.0,-88.0);
    private static final Vec3 CHALLENGE_BOARD_POS = new Vec3(-121.0,66.0,-86.0);
    private static final double ACTIVE_RADIUS_SQ = 74.0 * 74.0;
    private static final String SCOPE="radia_endgame_atrium";
    private static final String SELECTOR_KEY_PREFIX="endgame_selector:";
    private static final String CHALLENGE_KEY="endgame_challenge_board";
    private static final List<Selector> SELECTORS = selectors();
    private static final ConcurrentHashMap<UUID, String> SELECTED = new ConcurrentHashMap<>();

    private RadiaEndgameAtrium() {}
    public static List<Selector> selectorsView(){ return SELECTORS; }

    public static void build(ServerLevel l){
        if(hasMarker(l)) return;
        floor(l); entrance(l); bossGallery(l); riftGrid(l); milestoneArches(l); challengeBoardStructure(l); writeMarker(l);
    }

    public static void sync(ServerLevel l, ServerPlayer p){
        build(l);
        UUID pid=p.getUUID();
        boolean active=CampaignContentUnlocks.endgame(pid) && RadiaHubSessionManager.active(p)
                && p.position().distanceToSqr(CENTER)<=ACTIVE_RADIUS_SQ;
        List<SharedAuxiliaryActors.Spec> desired=new ArrayList<>();
        if(active){
            for(Selector s:SELECTORS){
                ChatFormatting color=selectorColor(s);
                desired.add(new SharedAuxiliaryActors.Spec(selectorKey(s),s.position(),
                        Component.literal(s.label()).withStyle(color),itemFor(s),false,true,
                        List.of(s.label(),"▶ "+s.label()+" · 선택됨")));
            }
            desired.add(new SharedAuxiliaryActors.Spec(CHALLENGE_KEY,CHALLENGE_BOARD_POS,
                    Component.literal("도전 과제 게시판").withStyle(ChatFormatting.GREEN,ChatFormatting.BOLD),
                    Items.WRITABLE_BOOK,false,true,challengeLegacyNames()));
        }else{
            SELECTED.remove(pid);
        }
        SharedAuxiliaryActors.sync(l,pid,SCOPE,desired);
    }

    public static boolean interact(ServerPlayer p,Entity target){
        if(p==null||target==null) return false;
        UUID pid=p.getUUID();
        String key=SharedAuxiliaryActors.key(target);
        if(CHALLENGE_KEY.equals(key)){
            int done=ChallengeService.completed(pid).size();
            p.sendSystemMessage(Component.literal("도전 과제 · "+done+"/20").withStyle(ChatFormatting.GREEN,ChatFormatting.BOLD));
            MetaNetwork.open(p,"QUESTS");
            return true;
        }

        Selector s=selectorForSharedKey(key); if(s==null) return false;
        if(!RadiaHubSessionManager.active(p)){p.sendSystemMessage(Component.literal("TURNBOUND · Endgame 입장은 라디아에서만 가능합니다."));return true;}
        boolean unlocked;
        if(s.type().equals("NORMAL")){
            Set<String> clears=CampaignProgressStore.snapshot(pid).clearedEncounters();
            unlocked=clears.contains(s.encounterId());
        }else unlocked=EndgameEncounterCatalog.unlocked(pid,s.encounterId());
        if(!unlocked){p.sendSystemMessage(Component.literal("TURNBOUND · 아직 잠긴 재도전입니다.").withStyle(ChatFormatting.GRAY));return true;}

        SELECTED.put(pid,s.encounterId());
        p.sendSystemMessage(Component.literal("선택 · "+s.label()).withStyle(selectorColor(s),ChatFormatting.BOLD));
        EndgameBriefing.send(p,EndgameBriefing.build(pid,s.encounterId(),s.type(),s.level()));
        return true;
    }

    public static void remove(ServerPlayer p){
        if(p==null||!(p.level() instanceof ServerLevel l)) return;
        SharedAuxiliaryActors.removeScope(l,p.getUUID(),SCOPE);
        SELECTED.remove(p.getUUID());
    }

    private static List<Selector> selectors(){
        ArrayList<Selector> out=new ArrayList<>();
        int[] baseLv={6,10,13,16,20};
        for(int i=1;i<=5;i++){
            String boss="B0"+i;String normal="BATTLE_"+boss;String hard=EndgameEncounterCatalog.hardId(boss);double x=-116+(i-1)*8;
            out.add(new Selector(normal,CanonicalData.definition(boss).name()+" · Normal 재도전",new Vec3(x,66,-99),"NORMAL",baseLv[i-1],true));
            out.add(new Selector(hard,CanonicalData.definition(boss).name()+" · Hard",new Vec3(x,66,-105),"HARD",baseLv[i-1]+5,true));
        }
        for(int floor=1;floor<=30;floor++){
            int index=floor-1,col=index%5,row=index/5;
            V04Catalogs.RiftFloor spec=V04Catalogs.riftFloor(floor);
            out.add(new Selector(EndgameEncounterCatalog.riftId(floor),"Rift F"+floor+" · Lv"+spec.level(),
                    new Vec3(-116+col*8,66,-92+row*4),"RIFT",spec.level(),spec.hardBossPattern()||floor%10==0));
        }
        return List.copyOf(out);
    }

    private static String selectorKey(Selector selector){return SELECTOR_KEY_PREFIX+selector.encounterId();}

    private static Selector selectorForSharedKey(String key){
        if(key==null||!key.startsWith(SELECTOR_KEY_PREFIX))return null;
        String id=key.substring(SELECTOR_KEY_PREFIX.length());
        for(Selector selector:SELECTORS)if(selector.encounterId().equals(id))return selector;
        return null;
    }

    private static ChatFormatting selectorColor(Selector s){
        return s.type().equals("HARD")?ChatFormatting.RED:s.type().equals("NORMAL")?ChatFormatting.GOLD:(s.milestone()?ChatFormatting.LIGHT_PURPLE:ChatFormatting.AQUA);
    }

    private static List<String> challengeLegacyNames(){
        ArrayList<String> names=new ArrayList<>();
        for(int i=0;i<=20;i++)names.add("Challenge Board · "+i+"/20");
        return List.copyOf(names);
    }

    private static Item itemFor(Selector s){
        if(s.type().equals("HARD")) return Items.NETHERITE_SWORD;
        if(s.type().equals("NORMAL")) return Items.IRON_SWORD;
        return s.milestone()?Items.AMETHYST_SHARD:Items.ENDER_EYE;
    }

    private static void floor(ServerLevel l){
        for(int x=-124;x<=-76;x++) for(int z=-110;z<=-67;z++){
            set(l,x,62,z,Blocks.DEEPSLATE);set(l,x,63,z,Blocks.DEEPSLATE);set(l,x,64,z,Blocks.DEEPSLATE);
            boolean rim=x==-124||x==-76||z==-110||z==-67;
            set(l,x,65,z,rim?Blocks.POLISHED_BLACKSTONE_BRICKS:(((x+z)&3)==0?Blocks.DEEPSLATE_TILES:Blocks.POLISHED_DEEPSLATE));
            for(int y=66;y<=74;y++) set(l,x,y,z,Blocks.AIR);
            if(rim&&((x+z)&1)==0) for(int y=66;y<=71;y++) set(l,x,y,z,Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
    }

    private static void entrance(ServerLevel l){
        for(int z=-67;z<=-58;z++) for(int x=-86;x<=-78;x++){
            set(l,x,65,z,Blocks.POLISHED_DEEPSLATE);for(int y=66;y<=71;y++) set(l,x,y,z,Blocks.AIR);
        }
        for(int y=66;y<=73;y++){set(l,-87,y,-68,Blocks.OBSIDIAN);set(l,-77,y,-68,Blocks.OBSIDIAN);}
        for(int x=-87;x<=-77;x++) set(l,x,73,-68,Blocks.CRYING_OBSIDIAN);
        set(l,-82,66,-68,Blocks.AMETHYST_BLOCK);
    }

    private static void bossGallery(ServerLevel l){
        for(int i=0;i<5;i++){
            int x=-116+i*8;
            pedestal(l,x,65,-105,Blocks.POLISHED_BLACKSTONE,hardCore(i));
            pedestal(l,x,65,-99,Blocks.STONE_BRICKS,Blocks.GOLD_BLOCK);
            for(int y=66;y<=70;y++){set(l,x-3,y,-108,Blocks.DEEPSLATE_BRICKS);set(l,x+3,y,-108,Blocks.DEEPSLATE_BRICKS);}
            set(l,x-3,71,-108,Blocks.SOUL_LANTERN);set(l,x+3,71,-108,Blocks.SOUL_LANTERN);
        }
    }

    private static Block hardCore(int i){return switch(i){case 0->Blocks.MOSSY_STONE_BRICKS;case 1->Blocks.MOSS_BLOCK;case 2->Blocks.IRON_BLOCK;case 3->Blocks.MAGMA_BLOCK;default->Blocks.CRYING_OBSIDIAN;};}

    /** Visual-only depth bands; floor stats/enemy data remain solely canonical V04Catalogs data. */
    private static void riftGrid(ServerLevel l){
        for(int f=1;f<=30;f++){
            int index=f-1,col=index%5,row=index/5,x=-116+col*8,z=-92+row*4;
            boolean mark=V04Catalogs.riftFloor(f).hardBossPattern()||f%10==0;
            Block base=f<=10?Blocks.POLISHED_DEEPSLATE:f<=20?Blocks.POLISHED_BLACKSTONE:Blocks.CRYING_OBSIDIAN;
            Block core=mark?Blocks.AMETHYST_BLOCK:f<=10?Blocks.OBSIDIAN:f<=20?Blocks.IRON_BLOCK:Blocks.RESPAWN_ANCHOR;
            pedestal(l,x,65,z,base,core);
        }
    }

    private static void milestoneArches(ServerLevel l){
        for(int row:new int[]{1,3,5}){
            int z=-92+row*4;
            for(int y=66;y<=72;y++){set(l,-121,y,z,Blocks.OBSIDIAN);set(l,-79,y,z,Blocks.OBSIDIAN);}
            set(l,-121,73,z,Blocks.SOUL_LANTERN);set(l,-79,73,z,Blocks.SOUL_LANTERN);
        }
        for(int x=-108;x<=-92;x++) set(l,x,65,-69,Blocks.AMETHYST_BLOCK);
        set(l,-100,66,-69,Blocks.BEACON);
    }

    private static void challengeBoardStructure(ServerLevel l){
        for(int z=-90;z<=-82;z++){
            set(l,-123,65,z,Blocks.POLISHED_BLACKSTONE_BRICKS);
            for(int y=66;y<=70;y++) set(l,-123,y,z,(z==-90||z==-82||y==70)?Blocks.POLISHED_BLACKSTONE_BRICKS:Blocks.DEEPSLATE_BRICKS);
        }
        set(l,-122,66,-86,Blocks.LECTERN);set(l,-122,67,-90,Blocks.LANTERN);set(l,-122,67,-82,Blocks.LANTERN);
        set(l,-123,68,-86,Blocks.EMERALD_BLOCK);
    }

    private static void pedestal(ServerLevel l,int x,int y,int z,Block base,Block core){
        for(int dx=-2;dx<=2;dx++) for(int dz=-1;dz<=1;dz++) set(l,x+dx,y,z+dz,base);
        set(l,x,y+1,z,core);
    }

    /** Fourth marker block bumps the atrium presentation schema so existing test worlds rebuild once. */
    private static boolean hasMarker(ServerLevel l){return l.getBlockState(new BlockPos(MARKER_X,MARKER_Y,MARKER_Z)).is(Blocks.LODESTONE)&&l.getBlockState(new BlockPos(MARKER_X+1,MARKER_Y,MARKER_Z)).is(Blocks.CRYING_OBSIDIAN)&&l.getBlockState(new BlockPos(MARKER_X+2,MARKER_Y,MARKER_Z)).is(Blocks.AMETHYST_BLOCK)&&l.getBlockState(new BlockPos(MARKER_X+3,MARKER_Y,MARKER_Z)).is(Blocks.EMERALD_BLOCK);}
    private static void writeMarker(ServerLevel l){set(l,MARKER_X,MARKER_Y,MARKER_Z,Blocks.LODESTONE);set(l,MARKER_X+1,MARKER_Y,MARKER_Z,Blocks.CRYING_OBSIDIAN);set(l,MARKER_X+2,MARKER_Y,MARKER_Z,Blocks.AMETHYST_BLOCK);set(l,MARKER_X+3,MARKER_Y,MARKER_Z,Blocks.EMERALD_BLOCK);}
    private static void set(ServerLevel l,int x,int y,int z,Block b){l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);}
}
