package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.QuestCatalog;
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
 * Physical v0.4 Character Quest investigation sites.
 *
 * The character wiki specifies each story premise but leaves most detailed step-by-step objectives open. v0.4 therefore
 * resolves CHARACTER_STORY through one authored investigation site per quest instead of inventing extra combat counts.
 */
public final class CharacterQuestWorldSites {
    public record Site(String questId, String characterId, String title, String routeHint, Vec3 position,
                       String finding, Item item, ChatFormatting color) {}

    private static final int MARKER_X = 104;
    private static final int MARKER_Y = 55;
    private static final int MARKER_Z = -92;
    private static final double SPAWN_RADIUS_SQ = 112.0 * 112.0;
    private static final double DESPAWN_RADIUS_SQ = 136.0 * 136.0;
    private static final List<Site> SITES = List.of(
            new Site("CQ_P01", "P01", "CQ_P01 · 끝까지 남은 길", "그늘숲 동쪽 · 무너진 북문 초소",
                    new Vec3(52.0, 70.0, -318.0),
                    "낡은 수비일지 마지막 장에 카이렌이 끝까지 철수를 거부했던 기록이 남아 있다.", Items.IRON_SWORD, ChatFormatting.AQUA),
            new Site("CQ_P02", "P02", "CQ_P02 · 멈춘 시계탑", "라디아 · Clock Tower 내부",
                    new Vec3(22.0, 66.0, -62.0),
                    "대단절 시각을 반복하던 계측핵이 현재 Relay 박동과 다시 동기화된다.", Items.CLOCK, ChatFormatting.AQUA),
            new Site("CQ_P03", "P03", "CQ_P03 · 성문은 한 번만 무너진다", "라디아 · Barracks 대피기록 보관실",
                    new Vec3(65.0, 66.0, -30.0),
                    "남문 대피 명단에는 브람이 마지막 민간인보다 뒤에 성문을 떠난 것으로 기록돼 있다.", Items.SHIELD, ChatFormatting.GREEN),
            new Site("CQ_P04", "P04", "CQ_P04 · 살아 돌아온 사람들", "라디아 · Memorial Steps 구호 명부",
                    new Vec3(-20.0, 68.0, -60.0),
                    "찢어진 생존자 명단 조각들이 맞춰지며 누락됐던 귀환자들의 이름이 복원된다.", Items.PAPER, ChatFormatting.GOLD),
            new Site("CQ_P05", "P05", "CQ_P05 · 한 발의 값", "남문 초원 동쪽 · 옛 사냥길 표식",
                    new Vec3(300.0, 67.0, 244.0),
                    "잘못 전달된 사냥 표식과 실제 민간인 이동 경로가 겹쳤던 지점이 확인된다.", Items.CROSSBOW, ChatFormatting.YELLOW),
            new Site("CQ_P06", "P06", "CQ_P06 · 이름을 적는 일", "라디아 · Memorial Steps 무명 기록대",
                    new Vec3(-28.0, 72.0, -70.0),
                    "무명 사망자 기록의 필체와 구 중계소 명부가 일치하며 잃어버린 신원이 복원된다.", Items.WRITABLE_BOOK, ChatFormatting.LIGHT_PURPLE),
            new Site("CQ_P07", "P07", "CQ_P07 · 두 번째 목소리", "그늘숲 서쪽 · 오래된 계약 제단",
                    new Vec3(-142.0, 70.0, -332.0),
                    "낡은 계약문에는 계약수를 소유물이 아니라 계약 당사자로 기록한 문장이 남아 있다.", Items.NAME_TAG, ChatFormatting.BLUE),
            new Site("CQ_P08", "P08", "CQ_P08 · 불길 속에서 웃는 법", "잿불 채석장 동쪽 · 붕괴 작업대",
                    new Vec3(150.0, 66.0, 390.0),
                    "사고 당시 작업기록은 라제가 도망친 것이 아니라 후방 인부를 끌어내기 위해 반대편 통로로 갔음을 보여 준다.", Items.IRON_AXE, ChatFormatting.RED));

    /** player -> spawned actor uuid -> site */
    private static final Map<UUID, Map<UUID, Site>> ACTORS = new ConcurrentHashMap<>();

    private CharacterQuestWorldSites() {}

    public static List<Site> sites() { return SITES; }

    public static String routeHint(String characterId) {
        return SITES.stream().filter(site -> site.characterId().equals(characterId)).map(Site::routeHint).findFirst().orElse("라디아 Character Quest 기록");
    }

    public static void build(ServerLevel level) {
        if (hasMarker(level)) return;
        buildP01(level); buildP02(level); buildP03(level); buildP04(level);
        buildP05(level); buildP06(level); buildP07(level); buildP08(level);
        writeMarker(level);
    }

    public static void sync(ServerLevel level, ServerPlayer player) {
        build(level);
        UUID playerId = player.getUUID();
        Map<UUID, Site> actors = ACTORS.computeIfAbsent(playerId, ignored -> new LinkedHashMap<>());

        for (var entry : List.copyOf(actors.entrySet())) {
            Entity entity = level.getEntity(entry.getKey());
            Site site = entry.getValue();
            boolean keep = entity != null && available(player, site) && !completed(player, site)
                    && player.position().distanceToSqr(site.position()) <= DESPAWN_RADIUS_SQ;
            if (!keep) {
                if (entity != null) entity.discard();
                actors.remove(entry.getKey());
            }
        }

        for (Site site : SITES) {
            if (!available(player, site) || completed(player, site)) continue;
            if (player.position().distanceToSqr(site.position()) > SPAWN_RADIUS_SQ) continue;
            if (actors.containsValue(site)) continue;
            ArmorStand stand = new ArmorStand(level, site.position().x, site.position().y, site.position().z);
            stand.setInvulnerable(true);
            stand.setNoGravity(true);
            stand.setShowArms(true);
            stand.setCustomName(Component.literal(site.title()).withStyle(site.color(), ChatFormatting.BOLD));
            stand.setCustomNameVisible(true);
            stand.setItemSlot(EquipmentSlot.MAINHAND, site.item().getDefaultInstance());
            level.addFreshEntity(stand);
            actors.put(stand.getUUID(), site);
        }
    }

    public static boolean interact(ServerPlayer player, Entity target) {
        if (player == null || target == null) return false;
        Map<UUID, Site> actors = ACTORS.get(player.getUUID());
        if (actors == null) return false;
        Site site = actors.get(target.getUUID());
        if (site == null) return false;
        if (!available(player, site)) {
            player.sendSystemMessage(Component.literal("TURNBOUND · 아직 이 Character Quest를 조사할 수 없습니다.").withStyle(ChatFormatting.GRAY));
            return true;
        }
        if (completed(player, site)) {
            target.discard(); actors.remove(target.getUUID()); return true;
        }

        try {
            QuestCatalog.Quest quest = CampaignProgressStore.completeQuest(player.getUUID(), site.questId());
            QuestCatalog.Reward reward = QuestCatalog.reward(quest);
            CampaignPersistence.saveIfDirty(player);
            player.sendSystemMessage(Component.literal(site.finding()).withStyle(ChatFormatting.WHITE));
            player.sendSystemMessage(Component.literal("Character Quest 완료 · " + quest.name()).withStyle(site.color(), ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("Crystal +" + reward.crystal() + " · Gold +" + reward.gold() + " · Profile Story 해금")
                    .withStyle(ChatFormatting.GOLD));
            target.discard(); actors.remove(target.getUUID());
            MetaNetwork.sync(player);
        } catch (RuntimeException ex) {
            player.sendSystemMessage(Component.literal("TURNBOUND · Character Quest 조건이 아직 충족되지 않았습니다.").withStyle(ChatFormatting.GRAY));
        }
        return true;
    }

    public static void remove(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        Map<UUID, Site> actors = ACTORS.remove(player.getUUID());
        if (actors == null) return;
        for (UUID actorId : actors.keySet()) {
            Entity entity = level.getEntity(actorId);
            if (entity != null) entity.discard();
        }
    }

    private static boolean available(ServerPlayer player, Site site) {
        return QuestMenuContentService.available(player.getUUID(), site.characterId());
    }

    private static boolean completed(ServerPlayer player, Site site) {
        return CampaignProgressStore.snapshot(player.getUUID()).quests().completed().contains(site.questId());
    }

    private static void buildP01(ServerLevel l) {
        Site s = SITES.get(0); int x=(int)s.position().x, y=(int)s.position().y-1, z=(int)s.position().z;
        pad(l,x,y,z,7,Blocks.MOSSY_STONE_BRICKS);
        for(int dz=-6;dz<=6;dz++){ set(l,x-6,y+1,z+dz,Blocks.STONE_BRICKS); if(Math.abs(dz)>2)set(l,x+6,y+1,z+dz,Blocks.CRACKED_STONE_BRICKS); }
        for(int dy=1;dy<=5;dy++){set(l,x-5,y+dy,z-5,Blocks.STONE_BRICKS);set(l,x+5,y+dy,z+5,Blocks.MOSSY_STONE_BRICKS);}
        set(l,x,y+1,z,Blocks.LECTERN); set(l,x-4,y+6,z-5,Blocks.LANTERN);
    }

    private static void buildP02(ServerLevel l) {
        Site s=SITES.get(1); int x=(int)s.position().x,y=(int)s.position().y-1,z=(int)s.position().z;
        for(int r=3;r<=7;r+=2) for(int dx=-r;dx<=r;dx++) for(int dz=-r;dz<=r;dz++) {
            int d=dx*dx+dz*dz; if(d>=r*r-r&&d<=r*r+r) set(l,x+dx,y,z+dz,r==7?Blocks.QUARTZ_BLOCK:Blocks.AMETHYST_BLOCK);
        }
        for(int[] p:new int[][]{{5,0},{-5,0},{0,5},{0,-5}}) set(l,x+p[0],y+1,z+p[1],Blocks.REDSTONE_LAMP);
        set(l,x,y+1,z,Blocks.LECTERN);
    }

    private static void buildP03(ServerLevel l) {
        Site s=SITES.get(2); int x=(int)s.position().x,y=(int)s.position().y-1,z=(int)s.position().z;
        pad(l,x,y,z,6,Blocks.POLISHED_ANDESITE);
        for(int dx=-5;dx<=5;dx+=5){set(l,x+dx,y+1,z-4,Blocks.IRON_BARS);set(l,x+dx,y+2,z-4,Blocks.IRON_BARS);}
        set(l,x,y+1,z,Blocks.LECTERN); set(l,x-3,y+1,z+2,Blocks.BARREL); set(l,x+3,y+1,z+2,Blocks.BARREL);
    }

    private static void buildP04(ServerLevel l) {
        Site s=SITES.get(3); int x=(int)s.position().x,y=(int)s.position().y-1,z=(int)s.position().z;
        pad(l,x,y,z,5,Blocks.SMOOTH_STONE); set(l,x,y+1,z,Blocks.LECTERN);
        for(int dx=-4;dx<=4;dx+=2){set(l,x+dx,y+1,z-3,Blocks.OAK_FENCE);set(l,x+dx,y+2,z-3,Blocks.LANTERN);}
        set(l,x-3,y+1,z+2,Blocks.BARREL);set(l,x+3,y+1,z+2,Blocks.BARREL);
    }

    private static void buildP05(ServerLevel l) {
        Site s=SITES.get(4); int x=(int)s.position().x,y=(int)s.position().y-1,z=(int)s.position().z;
        pad(l,x,y,z,7,Blocks.COARSE_DIRT); set(l,x,y+1,z,Blocks.TARGET);
        set(l,x-4,y+1,z+3,Blocks.CAMPFIRE); set(l,x+4,y+1,z-3,Blocks.BARREL);
        for(int dz=-6;dz<=6;dz+=3){set(l,x-6,y+1,z+dz,Blocks.OAK_FENCE);set(l,x+6,y+1,z+dz,Blocks.OAK_FENCE);}
    }

    private static void buildP06(ServerLevel l) {
        Site s=SITES.get(5); int x=(int)s.position().x,y=(int)s.position().y-1,z=(int)s.position().z;
        pad(l,x,y,z,5,Blocks.POLISHED_ANDESITE); set(l,x,y+1,z,Blocks.LECTERN);
        for(int dx=-4;dx<=4;dx+=2){set(l,x+dx,y+1,z-3,Blocks.CHISELED_STONE_BRICKS);set(l,x+dx,y+2,z-3,Blocks.SOUL_LANTERN);}
    }

    private static void buildP07(ServerLevel l) {
        Site s=SITES.get(6); int x=(int)s.position().x,y=(int)s.position().y-1,z=(int)s.position().z;
        pad(l,x,y,z,7,Blocks.MOSS_BLOCK);
        for(int a=0;a<8;a++){double r=a*Math.PI/4.0;int px=x+(int)Math.round(Math.cos(r)*5),pz=z+(int)Math.round(Math.sin(r)*5);set(l,px,y+1,pz,Blocks.OAK_LOG);set(l,px,y+2,pz,Blocks.REDSTONE_TORCH);}
        set(l,x,y+1,z,Blocks.CHISELED_BOOKSHELF); set(l,x,y+2,z,Blocks.SOUL_LANTERN);
    }

    private static void buildP08(ServerLevel l) {
        Site s=SITES.get(7); int x=(int)s.position().x,y=(int)s.position().y-1,z=(int)s.position().z;
        pad(l,x,y,z,8,Blocks.TUFF); for(int dx=-6;dx<=6;dx+=6) for(int dz=-5;dz<=5;dz+=5) set(l,x+dx,y+1,z+dz,Blocks.IRON_BLOCK);
        set(l,x,y+1,z,Blocks.BLAST_FURNACE); set(l,x-3,y+1,z,Blocks.ANVIL); set(l,x+3,y+1,z,Blocks.REDSTONE_LAMP);
        for(int dx=-2;dx<=2;dx++) set(l,x+dx,y,z+4,Blocks.MAGMA_BLOCK);
    }

    private static void pad(ServerLevel l,int cx,int y,int cz,int r,Block top){
        for(int dx=-r;dx<=r;dx++) for(int dz=-r;dz<=r;dz++) { if(dx*dx+dz*dz>r*r) continue; for(int fy=y-2;fy<y;fy++) set(l,cx+dx,fy,cz+dz,Blocks.DIRT); set(l,cx+dx,y,cz+dz,top); for(int ay=y+1;ay<=y+5;ay++) set(l,cx+dx,ay,cz+dz,Blocks.AIR); }
    }

    private static boolean hasMarker(ServerLevel l){
        return l.getBlockState(new BlockPos(MARKER_X,MARKER_Y,MARKER_Z)).is(Blocks.LODESTONE)
                && l.getBlockState(new BlockPos(MARKER_X+1,MARKER_Y,MARKER_Z)).is(Blocks.CHISELED_BOOKSHELF)
                && l.getBlockState(new BlockPos(MARKER_X+2,MARKER_Y,MARKER_Z)).is(Blocks.AMETHYST_BLOCK);
    }
    private static void writeMarker(ServerLevel l){set(l,MARKER_X,MARKER_Y,MARKER_Z,Blocks.LODESTONE);set(l,MARKER_X+1,MARKER_Y,MARKER_Z,Blocks.CHISELED_BOOKSHELF);set(l,MARKER_X+2,MARKER_Y,MARKER_Z,Blocks.AMETHYST_BLOCK);}
    private static void set(ServerLevel l,int x,int y,int z,Block b){l.setBlock(new BlockPos(x,y,z),b.defaultBlockState(),2);}
}
