package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.ArcaneDamage;
import kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;
import kr.moonseungjun.arcanecircle.magic.RpgScaleService;
import kr.moonseungjun.arcanecircle.network.ArcaneNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Every marked mage casts, retaliates, and scales geometrically with circle rank. */
public final class ArcaneMageService {
    private static final String NAME_PREFIX = "[마도사:";
    private static final Set<String> SPELLCASTER_TYPES = Set.of("witch", "evoker", "illusioner");
    private static final int[] CIRCLE_WEIGHTS = {59_000,25_000,10_000,4_000,1_500,400,80,20};
    private static final Map<UUID,Long> LAST_CAST=new HashMap<>();
    private static final Map<UUID,MageProfile> PROFILES=new HashMap<>();
    private ArcaneMageService(){}

    public static void tickNear(ServerPlayer player){
        if(!(player.level() instanceof ServerLevel level)||player.isSpectator())return;
        List<Villager> villagers=level.getEntitiesOfClass(Villager.class,player.getBoundingBox().inflate(72.0),v->v.isAlive()&&!v.isBaby());
        ensureVillageResidents(level,villagers.stream().filter(v->level.isVillage(v.blockPosition())).toList());
        for(Villager villager:villagers)if(isMage(villager)){applyMageStats(villager);castResidentSpell(level,villager);}
        for(Mob mob:level.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(72.0),v->v.isAlive()&&!(v instanceof Villager))){
            if(SPELLCASTER_TYPES.contains(typePath(mob)))ensureNaturalMage(mob);
            if(isMage(mob)){applyMageStats(mob);castHostileSpell(level,mob);}
        }
        if((level.getGameTime()&255L)==0L){LAST_CAST.entrySet().removeIf(e->level.getGameTime()-e.getValue()>2400L);PROFILES.entrySet().removeIf(e->level.getEntity(e.getKey())==null);}
    }

    public static void onInteract(PlayerInteractEvent.EntityInteract event){
        if(event.getHand()!=InteractionHand.MAIN_HAND||!(event.getEntity() instanceof ServerPlayer player)
                ||!(event.getTarget() instanceof Villager villager)||!isMage(villager))return;
        event.setCanceled(true); MageProfile mage=profile(villager);
        ArcaneQuestData.get(((ServerLevel)player.level()).getServer()).offer(player,mage.circle(),mage.affiliation());
        player.sendSystemMessage(Component.literal(color(mage.affiliation())+"["+mage.circle()+"써클 "+mage.role().displayName()+" 마도사] §f"
                +visibleName(villager)+" §7· "+mage.affiliation().displayName()+" · 의뢰 난이도와 보상은 고정 등급제로 결정됩니다."));
        ArcaneNetwork.openPage(player,"quests");
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event){
        if(!(event.getEntity() instanceof Mob mage)||!isMage(mage))return;
        Entity source=event.getSource().getEntity(); if(source instanceof LivingEntity attacker&&attacker!=mage){mage.setTarget(attacker);LAST_CAST.put(mage.getUUID(),Long.MIN_VALUE/4);}
    }

    public static void registerNamedMage(Mob entity,int circle,MagicTradition affiliation,MageSociety.Role role,String name,String id){
        mark(entity,new MageProfile(clamp(circle,1,9),affiliation,role),name);entity.addTag("arcanecircle_named_"+id);entity.setPersistenceRequired();applyMageStats(entity);
    }

    public static boolean isMage(Entity entity){return PROFILES.containsKey(entity.getUUID())||parseProfile(entity)!=null;}
    public static int circle(Entity entity){return profile(entity).circle();}
    public static MagicTradition affiliation(Entity entity){return profile(entity).affiliation();}
    public static MageSociety.Role role(Entity entity){return profile(entity).role();}
    public static MagicTradition affiliation(ServerPlayer player){return ArcaneWorldData.get(((ServerLevel)player.level()).getServer()).tradition(player);}
    public static boolean autoHostile(Entity left,Entity right){return isMage(left)&&isMage(right)&&MageSociety.hostile(affiliation(left),affiliation(right));}

    private static MageProfile profile(Entity entity){MageProfile p=PROFILES.get(entity.getUUID());if(p!=null)return p;p=parseProfile(entity);if(p!=null){PROFILES.put(entity.getUUID(),p);return p;}return new MageProfile(1,MagicTradition.UNBOUND,MageSociety.Role.WANDERER);}
    private static MageProfile parseProfile(Entity entity){Component n=entity.getCustomName();if(n==null)return null;String s=n.getString();int start=s.indexOf(NAME_PREFIX);if(start<0)return null;int end=s.indexOf(']',start+NAME_PREFIX.length());if(end<0)return null;String[] p=s.substring(start+NAME_PREFIX.length(),end).split(":");try{return new MageProfile(clamp(Integer.parseInt(p[0]),1,9),p.length>=2?MagicTradition.parse(p[1]):MagicTradition.UNBOUND,p.length>=3?MageSociety.Role.parse(p[2]):MageSociety.Role.WANDERER);}catch(NumberFormatException ignored){return null;}}

    private static void ensureVillageResidents(ServerLevel level,List<Villager> villagers){if(villagers.size()<4)return;int seed=villagers.stream().mapToInt(v->v.getUUID().hashCode()).min().orElse(0);int roll=Math.floorMod(seed,100);int desired=roll<52?0:roll<90?1:2;desired=Math.min(desired,Math.max(0,villagers.size()/7));long existing=villagers.stream().filter(ArcaneMageService::isMage).count();if(existing>=desired)return;villagers.stream().filter(v->!isMage(v)).sorted(Comparator.comparingInt(v->Math.floorMod(v.getUUID().hashCode(),100_000))).limit(desired-existing).forEach(ArcaneMageService::promoteResident);}
    private static void promoteResident(Villager v){int c=weightedCircle(v.getUUID(),5);MagicTradition a=residentAffiliation(v.getUUID());mark(v,new MageProfile(c,a,residentRole(v.getUUID(),a)),null);v.setPersistenceRequired();applyMageStats(v);}
    private static void ensureNaturalMage(Mob mob){if(isMage(mob))return;String type=typePath(mob);int cap="illusioner".equals(type)?7:"evoker".equals(type)?6:5;int min="illusioner".equals(type)?3:"evoker".equals(type)?2:1;int c=Math.max(min,weightedCircle(mob.getUUID(),cap));MagicTradition a=naturalAffiliation(mob.getUUID(),type);mark(mob,new MageProfile(c,a,naturalRole(mob.getUUID(),type,a)),null);mob.setPersistenceRequired();applyMageStats(mob);}

    private static void mark(Entity entity,MageProfile p,String name){PROFILES.put(entity.getUUID(),p);String suffix=name==null||name.isBlank()?p.circle()+"써클 "+p.role().displayName()+" 마도사":name;entity.setCustomName(Component.literal(color(p.affiliation())+"[마도사:"+p.circle()+":"+p.affiliation().name()+":"+p.role().name()+"] "+suffix));entity.setCustomNameVisible(name!=null&&!name.isBlank());}
    private static void applyMageStats(Mob mage){MageProfile p=profile(mage);RpgScaleService.ensureBaseline(mage);RpgScaleService.applyExtraHealth(mage,"mage_c"+p.circle(),Math.pow(1.85,p.circle()-1));AttributeInstance attack=mage.getAttribute(Attributes.ATTACK_DAMAGE);if(attack!=null&&mage.addTag("arcanecircle_mage_attack")){attack.setBaseValue(Math.min(500.0,Math.max(attack.getBaseValue(),2.0)*Math.pow(1.28,p.circle()-1)));mage.addTag("arcanecircle_mage_attack");}if(p.circle()>=5)mage.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,50,Math.min(3,(p.circle()-4)/2),true,false));}

    private static void castResidentSpell(ServerLevel level,Villager caster){MageProfile p=profile(caster);long now=level.getGameTime();int interval=Math.max(18,(int)Math.round((92-p.circle()*7)*p.affiliation().cooldownMultiplier()));if(!ready(caster,now,interval))return;LivingEntity target=findResidentTarget(level,caster,p);if(target==null){if(caster.getHealth()<caster.getMaxHealth()&&(p.role()==MageSociety.Role.HOUSEHOLD||p.role()==MageSociety.Role.SCHOLAR))caster.heal((float)(1.0+Math.pow(1.45,p.circle()-1)));return;}float damage=spellDamage(p,1.0F);ArcaneDamage.hurt(level,caster,target,damage);if(target instanceof Mob mob)mob.setTarget(caster);applyControl(caster,target,p);level.playSound(null,caster.blockPosition(),SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.NEUTRAL,0.55F,1.5F-p.circle()*0.035F);}
    private static LivingEntity findResidentTarget(ServerLevel level,Villager caster,MageProfile p){LivingEntity attacker=recentAttacker(caster);if(attacker!=null&&caster.distanceToSqr(attacker)<=40*40)return attacker;LivingEntity hostileMage=level.getEntitiesOfClass(Mob.class,caster.getBoundingBox().inflate(18),v->v.isAlive()&&isMage(v)&&MageSociety.hostile(p.affiliation(),affiliation(v))).stream().min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);if(hostileMage!=null)return hostileMage;Mob enemy=level.getEntitiesOfClass(Mob.class,caster.getBoundingBox().inflate(15),v->v.isAlive()&&v instanceof Enemy&&!isMage(v)).stream().min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);if(enemy!=null)return enemy;if(p.role()!=MageSociety.Role.VILLAIN&&p.affiliation()!=MagicTradition.PRIMAL)return null;return level.getEntitiesOfClass(ServerPlayer.class,caster.getBoundingBox().inflate(18),v->v.isAlive()&&!v.isSpectator()&&MageSociety.hostile(p.affiliation(),affiliation(v))).stream().min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);}
    private static void castHostileSpell(ServerLevel level,Mob caster){MageProfile p=profile(caster);LivingEntity attacker=recentAttacker(caster);LivingEntity target=attacker!=null?attacker:caster.getTarget();if(target==null||!target.isAlive())return;boolean retaliating=attacker!=null&&target==attacker;if(retaliating)caster.setTarget(target);MagicTradition ta=target instanceof ServerPlayer sp?affiliation(sp):isMage(target)?affiliation(target):MagicTradition.UNBOUND;if(!retaliating&&MageSociety.avoidsAutoTarget(p.affiliation(),ta)){caster.setTarget(null);return;}if(caster.distanceToSqr(target)>36*36)return;int interval=Math.max(16,(int)Math.round((94-p.circle()*7)*p.affiliation().cooldownMultiplier()));if(!ready(caster,level.getGameTime(),interval))return;ArcaneDamage.hurt(level,caster,target,spellDamage(p,1.08F));applyControl(caster,target,p);level.playSound(null,caster.blockPosition(),SoundEvents.EVOKER_CAST_SPELL,SoundSource.HOSTILE,0.75F,1.25F-p.circle()*0.03F);}
    private static float spellDamage(MageProfile p,float base){double role=p.role()==MageSociety.Role.VILLAIN?1.28:p.role()==MageSociety.Role.WARDEN?1.12:1.0;return(float)Math.min(420.0,base*2.1*Math.pow(1.72,p.circle()-1)*role*p.affiliation().powerMultiplier());}
    private static void applyControl(LivingEntity caster,LivingEntity target,MageProfile p){switch((p.circle()+p.role().ordinal())%4){case 0->target.addEffect(new MobEffectInstance(MobEffects.LEVITATION,16+p.circle()*2,0));case 1->target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,40+p.circle()*6,Math.min(4,p.circle()/2)));case 2->target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,45+p.circle()*7,Math.min(3,p.circle()/3)));default->target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(),30+p.circle()*12));}if(p.circle()>=5)pushAway(caster,target,0.22+p.circle()*0.045);}

    private static int weightedCircle(UUID uuid,int cap){cap=clamp(cap,1,8);int total=0;for(int i=0;i<cap;i++)total+=CIRCLE_WEIGHTS[i];int roll=Math.floorMod(uuid.hashCode()*31+uuid.toString().hashCode(),total),sum=0;for(int i=0;i<cap;i++){sum+=CIRCLE_WEIGHTS[i];if(roll<sum)return i+1;}return cap;}
    private static MagicTradition residentAffiliation(UUID u){int r=Math.floorMod(u.hashCode(),100);return r<58?MagicTradition.ARCANE:r<78?MagicTradition.DIVINE:r<96?MagicTradition.OCCULT:MagicTradition.PRIMAL;}
    private static MageSociety.Role residentRole(UUID u,MagicTradition a){int r=Math.floorMod(u.toString().hashCode(),100);if(a==MagicTradition.PRIMAL)return r<72?MageSociety.Role.VILLAIN:MageSociety.Role.WANDERER;return r<32?MageSociety.Role.HOUSEHOLD:r<58?MageSociety.Role.LICENSED:r<78?MageSociety.Role.SCHOLAR:r<94?MageSociety.Role.WARDEN:MageSociety.Role.WANDERER;}
    private static MagicTradition naturalAffiliation(UUID u,String type){int r=Math.floorMod(u.hashCode(),100);if("witch".equals(type))return r<52?MagicTradition.OCCULT:r<78?MagicTradition.PRIMAL:r<92?MagicTradition.UNBOUND:r<97?MagicTradition.ARCANE:MagicTradition.DIVINE;return r<70?MagicTradition.PRIMAL:r<88?MagicTradition.OCCULT:r<95?MagicTradition.UNBOUND:r<98?MagicTradition.ARCANE:MagicTradition.DIVINE;}
    private static MageSociety.Role naturalRole(UUID u,String type,MagicTradition a){int r=Math.floorMod(u.toString().hashCode(),100);if(a==MagicTradition.PRIMAL||"evoker".equals(type))return r<78?MageSociety.Role.VILLAIN:MageSociety.Role.WARDEN;if("witch".equals(type))return r<45?MageSociety.Role.WANDERER:r<78?MageSociety.Role.SCHOLAR:MageSociety.Role.HOUSEHOLD;return r<55?MageSociety.Role.WARDEN:MageSociety.Role.SCHOLAR;}
    private static LivingEntity recentAttacker(LivingEntity c){LivingEntity a=c.getLastHurtByMob();return a==null||!a.isAlive()||a==c?null:a;}
    private static void pushAway(LivingEntity c,LivingEntity t,double strength){Vec3 d=t.position().subtract(c.position());Vec3 p=new Vec3(d.x,0,d.z);if(p.lengthSqr()<.00001)return;p=p.normalize().scale(strength);t.push(p.x,.12,p.z);}
    private static boolean ready(Entity e,long now,int interval){long last=LAST_CAST.getOrDefault(e.getUUID(),Long.MIN_VALUE/4);int phase=Math.floorMod(e.getUUID().hashCode(),Math.max(1,interval));if((now+phase)%interval!=0L||now==last)return false;LAST_CAST.put(e.getUUID(),now);return true;}
    private static String visibleName(Entity e){String s=e.getName().getString();int i=s.indexOf("] ");return i>=0?s.substring(i+2):s;}
    private static String color(MagicTradition a){return switch(a){case ARCANE->"§9";case DIVINE->"§f";case OCCULT->"§5";case PRIMAL->"§4";default->"§7";};}
    private static String typePath(Entity e){var k=BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());return k==null?"":k.getPath();}
    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private record MageProfile(int circle,MagicTradition affiliation,MageSociety.Role role){}
}
