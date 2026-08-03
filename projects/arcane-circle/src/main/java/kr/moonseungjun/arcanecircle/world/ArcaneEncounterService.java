package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;
import kr.moonseungjun.arcanecircle.magic.MagicPlayerData;
import kr.moonseungjun.arcanecircle.magic.RpgScaleService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent faction outposts and visibly distinct late-game calamity regions. */
public final class ArcaneEncounterService {
    private record Zone(String id,String name,int dx,int dz,int tier,int radius,String bossName){}
    private static final List<Zone> ZONES=List.of(
            new Zone("rift_wastes","균열 황야",720,0,4,62,"균열뿔 마수 그라움"),
            new Zone("frozen_necropolis","빙결 묘역",-1050,360,6,68,"빙관의 마수 흐레스"),
            new Zone("starved_abyss","별먹는 심연",320,1480,8,76,"성식 마수 아포크리온"));
    private static final Map<UUID,Long> WARNED=new HashMap<>();
    private static final String BOSS_PREFIX="arcanecircle_boss_";
    private static final String NAMED_PREFIX="arcanecircle_named_";
    private ArcaneEncounterService(){}

    public static void tick(ServerPlayer player){
        if(!(player.level() instanceof ServerLevel level))return;
        ArcaneEncounterData data=ArcaneEncounterData.get(level.getServer());
        if(player.tickCount%40==0)data.updatePlayer(player);
        if(player.tickCount%20==Math.floorMod(player.getUUID().hashCode(),20)){
            for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,player.getBoundingBox().inflate(72.0),e->e.isAlive()&&e!=player))RpgScaleService.ensureBaseline(entity);
        }
        if(level!=level.getServer().overworld()||player.tickCount%40!=0)return;
        ensureOutposts(level,player,data);
        BlockPos spawn=BlockPos.ZERO;
        for(Zone zone:ZONES){BlockPos center=surface(level,spawn.getX()+zone.dx,spawn.getZ()+zone.dz);double distance=player.distanceToSqr(center.getX()+.5,center.getY()+.5,center.getZ()+.5);
            if(distance<240.0*240.0&&!data.isZoneBuilt(zone.id)){buildZone(level,center,zone);data.markZoneBuilt(zone.id);}
            if(distance<zone.radius*zone.radius){warn(player,zone,data);ensureBoss(level,player,center,zone,data);ensureElites(level,player,center,zone);}
        }
    }

    public static void onDeath(LivingDeathEvent event){
        if(!(event.getEntity().level() instanceof ServerLevel level))return;
        ArcaneEncounterData data=ArcaneEncounterData.get(level.getServer());
        String name=event.getEntity().getName().getString();
        for(FactionProfile.Entry faction:FactionProfile.entries()){
            if(name.contains(faction.representativeName())){
                data.markNamedDead(faction.representativeId());
                broadcast(level,"§8[영구 사망] §f"+faction.representativeName()+"§7이(가) 이 세계에서 사라졌습니다.");
                return;
            }
        }
        for(Zone zone:ZONES){
            if(name.contains(zone.bossName)){
                data.markBossDefeated(zone.id);
                broadcast(level,"§6[마수 토벌] §f"+zone.bossName+"§7이(가) 쓰러졌습니다.");
                return;
            }
        }
    }

    public static String zoneSummary(ServerPlayer player){ServerLevel level=(ServerLevel)player.level();ArcaneEncounterData data=ArcaneEncounterData.get(level.getServer());BlockPos s=BlockPos.ZERO;return ZONES.stream().map(z->z.name+" "+z.tier+"C+ ["+(s.getX()+z.dx)+", "+(s.getZ()+z.dz)+"] "+(data.isBossDefeated(z.id)?"토벌":"활성")).reduce((a,b)->a+"|"+b).orElse("");}

    private static void ensureOutposts(ServerLevel level,ServerPlayer player,ArcaneEncounterData data){BlockPos spawn=BlockPos.ZERO;int[][] offsets={{180,0},{0,180},{-180,0},{0,-180}};List<FactionProfile.Entry> entries=FactionProfile.entries();for(int i=0;i<entries.size();i++){FactionProfile.Entry f=entries.get(i);BlockPos pos=surface(level,spawn.getX()+offsets[i][0],spawn.getZ()+offsets[i][1]);String buildId="faction_"+f.representativeId();if(player.distanceToSqr(pos.getX(),pos.getY(),pos.getZ())<220*220&&!data.isZoneBuilt(buildId)){buildOutpost(level,pos,f.tradition());data.markZoneBuilt(buildId);}if(player.distanceToSqr(pos.getX(),pos.getY(),pos.getZ())<90*90&&!data.isNamedDead(f.representativeId()))ensureRepresentative(level,pos,f);}}
    private static void ensureRepresentative(ServerLevel level,BlockPos pos,FactionProfile.Entry f){String tag=NAMED_PREFIX+f.representativeId();if(!nearby(level,pos,42,tag).isEmpty())return;Villager npc=EntityTypes.VILLAGER.create(level,EntitySpawnReason.EVENT);if(npc==null)return;npc.snapTo(pos.above(),0,0);npc.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),EntitySpawnReason.EVENT,null);ArcaneMageService.registerNamedMage(npc,f.representativeCircle(),f.tradition(),f.role(),f.representativeName(),f.representativeId());level.addFreshEntityWithPassengers(npc);}

    private static void ensureBoss(ServerLevel level,ServerPlayer player,BlockPos center,Zone z,ArcaneEncounterData data){if(data.isBossDefeated(z.id)||!nearby(level,center,z.radius,BOSS_PREFIX+z.id).isEmpty())return;Mob boss=switch(z.id){case"rift_wastes"->EntityTypes.RAVAGER.create(level,EntitySpawnReason.EVENT);case"frozen_necropolis"->EntityTypes.HOGLIN.create(level,EntitySpawnReason.EVENT);default->EntityTypes.WARDEN.create(level,EntitySpawnReason.EVENT);};if(boss==null)return;boss.snapTo(center.above(),0,0);boss.finalizeSpawn(level,level.getCurrentDifficultyAt(center),EntitySpawnReason.EVENT,null);boss.setCustomName(Component.literal("§4[마수:"+z.tier+"환] "+z.bossName));boss.setCustomNameVisible(true);boss.setPersistenceRequired();boss.addTag(BOSS_PREFIX+z.id);boss.addTag("arcanecircle_elite_t"+z.tier);RpgScaleService.ensureBaseline(boss);RpgScaleService.applyExtraHealth(boss,"boss_"+z.id,Math.pow(2.0,z.tier-1)*2.5);boss.addEffect(new MobEffectInstance(MobEffects.STRENGTH,-1,Math.min(8,z.tier),true,false));boss.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,-1,Math.min(4,z.tier/2),true,false));boss.addEffect(new MobEffectInstance(MobEffects.SPEED,-1,Math.min(3,z.tier/3),true,false));boss.addEffect(new MobEffectInstance(MobEffects.GLOWING,-1,0,true,false));AttributeInstance scale=boss.getAttribute(Attributes.SCALE);if(scale!=null)scale.setBaseValue(Math.min(2.25,1.30+z.tier*.11));boss.setTarget(player);level.addFreshEntityWithPassengers(boss);}
    private static void ensureElites(ServerLevel level,ServerPlayer player,BlockPos center,Zone z){String tag="arcanecircle_zone_"+z.id;if(nearby(level,center,z.radius,tag).size()>=Math.min(10,3+z.tier))return;if(level.getRandom().nextInt(3)!=0)return;double angle=level.getRandom().nextDouble()*Math.PI*2;int distance=18+level.getRandom().nextInt(Math.max(8,z.radius-24));BlockPos pos=surface(level,center.getX()+(int)Math.round(Math.cos(angle)*distance),center.getZ()+(int)Math.round(Math.sin(angle)*distance));Mob mob=switch(z.id){case"rift_wastes"->EntityTypes.HUSK.create(level,EntitySpawnReason.EVENT);case"frozen_necropolis"->EntityTypes.STRAY.create(level,EntitySpawnReason.EVENT);default->EntityTypes.ENDERMAN.create(level,EntitySpawnReason.EVENT);};if(mob==null)return;mob.snapTo(pos.above(),0,0);mob.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),EntitySpawnReason.EVENT,null);mob.setCustomName(Component.literal("§5"+z.name+" 정예체 "+z.tier+"환"));mob.setCustomNameVisible(false);mob.setPersistenceRequired();mob.addTag(tag);mob.addTag("arcanecircle_elite_t"+z.tier);RpgScaleService.ensureBaseline(mob);RpgScaleService.applyExtraHealth(mob,"elite_t"+z.tier,Math.pow(1.72,z.tier-1));mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH,-1,Math.max(1,z.tier/2),true,false));mob.addEffect(new MobEffectInstance(MobEffects.SPEED,-1,Math.min(3,z.tier/3),true,false));AttributeInstance scale=mob.getAttribute(Attributes.SCALE);if(scale!=null)scale.setBaseValue(Math.min(1.55,1.02+z.tier*.055));mob.setTarget(player);level.addFreshEntityWithPassengers(mob);}

    private static void warn(ServerPlayer player,Zone z,ArcaneEncounterData data){long now=((ServerLevel)player.level()).getGameTime();long last=WARNED.getOrDefault(player.getUUID(),Long.MIN_VALUE/4);if(now-last<240)return;WARNED.put(player.getUUID(),now);int circle=MagicPlayerData.get(((ServerLevel)player.level()).getServer()).state(player).circle();String danger=circle<z.tier?"§c권장보다 "+(z.tier-circle)+"써클 낮음":"§6교전 가능";ArcaneNoticeService.push(player,Component.literal("§4[마력 재해권] §f"+z.name+" §7· 권장 "+z.tier+"써클 · "+danger+(data.isBossDefeated(z.id)?" · 마수 토벌됨":" · 마수 생존")),45);}

    private static void buildOutpost(ServerLevel level,BlockPos base,MagicTradition faction){Block primary=switch(faction){case ARCANE->Blocks.LAPIS_BLOCK;case DIVINE->Blocks.QUARTZ_BRICKS;case OCCULT->Blocks.AMETHYST_BLOCK;case PRIMAL->Blocks.RED_NETHER_BRICKS;default->Blocks.STONE_BRICKS;};Block glow=faction==MagicTradition.PRIMAL?Blocks.MAGMA_BLOCK:Blocks.SEA_LANTERN;for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)if(Math.abs(x)==6||Math.abs(z)==6||x*x+z*z<18)level.setBlockAndUpdate(base.offset(x,-1,z),primary.defaultBlockState());for(int y=0;y<12;y++)level.setBlockAndUpdate(base.offset(0,y,0),(y%4==3?glow:primary).defaultBlockState());for(int[] p:List.of(new int[]{5,5},new int[]{5,-5},new int[]{-5,5},new int[]{-5,-5}))for(int y=0;y<7;y++)level.setBlockAndUpdate(base.offset(p[0],y,p[1]),(y==6?glow:primary).defaultBlockState());}
    private static void buildZone(ServerLevel level,BlockPos center,Zone z){Block ground=switch(z.id){case"rift_wastes"->Blocks.CRYING_OBSIDIAN;case"frozen_necropolis"->Blocks.PACKED_ICE;default->Blocks.SCULK;};Block dark=switch(z.id){case"rift_wastes"->Blocks.BLACKSTONE;case"frozen_necropolis"->Blocks.BLUE_ICE;default->Blocks.OBSIDIAN;};Block glow=switch(z.id){case"rift_wastes"->Blocks.MAGMA_BLOCK;case"frozen_necropolis"->Blocks.SEA_LANTERN;default->Blocks.AMETHYST_BLOCK;};int r=26;for(int x=-r;x<=r;x++)for(int z0=-r;z0<=r;z0++){int d=x*x+z0*z0;if(d>r*r)continue;if(d>(r-2)*(r-2)||Math.floorMod(x*7+z0*11,13)==0){BlockPos top=surface(level,center.getX()+x,center.getZ()+z0);level.setBlockAndUpdate(top.below(),(d>(r-2)*(r-2)?dark:ground).defaultBlockState());}}for(int[] p:List.of(new int[]{20,0},new int[]{-20,0},new int[]{0,20},new int[]{0,-20})){BlockPos tower=surface(level,center.getX()+p[0],center.getZ()+p[1]);for(int y=0;y<24+z.tier;y++)level.setBlockAndUpdate(tower.offset(0,y,0),(y%6==5?glow:dark).defaultBlockState());}for(int x=-4;x<=4;x++)for(int z0=-4;z0<=4;z0++)level.setBlockAndUpdate(center.offset(x,-1,z0),ground.defaultBlockState());for(int y=0;y<10;y++)level.setBlockAndUpdate(center.offset(0,y,0),(y%3==2?glow:dark).defaultBlockState());}
    private static BlockPos surface(ServerLevel level,int x,int z){int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);return new BlockPos(x,y,z);}
    private static List<Mob> nearby(ServerLevel level,BlockPos center,double radius,String tag){AABB box=new AABB(center.getX()-radius,center.getY()-48,center.getZ()-radius,center.getX()+radius,center.getY()+96,center.getZ()+radius);return level.getEntitiesOfClass(Mob.class,box,e->e.isAlive()&&hasTag(e,tag));}
    private static boolean hasTag(Mob entity,String tag){
        boolean added=entity.addTag(tag);
        if(added)entity.removeTag(tag);
        return !added;
    }
        private static void broadcast(ServerLevel level,String text){for(ServerPlayer p:level.getServer().getPlayerList().getPlayers())p.sendSystemMessage(Component.literal(text));}
}
