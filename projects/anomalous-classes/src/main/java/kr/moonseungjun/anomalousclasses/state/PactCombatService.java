package kr.moonseungjun.anomalousclasses.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** First four patrons: their combat identities work without any extra contract. */
public final class PactCombatService {
    private static MinecraftServer activeServer;
    private static final Map<UUID, Battle> BATTLES = new HashMap<>();
    private record TimedHit(Mob target, long due) {}
    private record Ward(UUID owner, UUID protectedId, long until) {}
    private static final class Battle {
        int page;
        long starredUntil, stepUntil, guardianUntil, bulwarkUntil, lastGuard, lastBulwark;
        long emberUntil, lastEmberHeal, lastIsaelAnswer;
        int urgenStrikes;
        Vec3 lastStrikePosition;
        final Map<UUID, Long> recentExtras = new HashMap<>();
        final Map<UUID, TimedHit> burning = new HashMap<>();
        final Map<UUID, Long> marks = new HashMap<>();
        Ward ward;
    }
    private PactCombatService() {}
    private static Battle battle(ServerPlayer player) {
        if (activeServer != player.getServer()) {
            BATTLES.clear();
            activeServer = player.getServer();
        }
        return BATTLES.computeIfAbsent(player.getUUID(), key -> new Battle());
    }
    private static boolean owned(ServerPlayer player, String patron) {
        return PactService.isPact(player) && PactSavedData.get(player.getServer())
                .player(player.getUUID()).has(patron);
    }
    static boolean enemy(ServerPlayer player, LivingEntity entity) {
        return entity instanceof Mob && entity instanceof Enemy
                && !(entity instanceof PuppetEntity)
                && MentalDominatorService.isValidCombatTarget(player, entity)
                && ParasiteSavedData.get(player.getServer()).ownerOf(entity.getUUID()) == null;
    }
    private static void tell(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key,args),true);
    }
    private record Slot(PactCatalog.Patron patron, PactCatalog.Ability spell) {}
    private static List<Slot> slots(ServerPlayer player) {
        List<Slot> slots = new ArrayList<>();
        for (PactSavedData.Bond bond : PactSavedData.get(player.getServer())
                .player(player.getUUID()).bonds()) {
            PactCatalog.Patron patron = PactCatalog.get(bond.id());
            if (patron == null || !patron.enabled()) continue;
            for (var spell : patron.abilities()) slots.add(new Slot(patron,spell));
        }
        return slots;
    }
    public static void input(ServerPlayer player, int action) {
        if (!PactService.isPact(player) || action < 0 || action > 6) return;
        Battle state = battle(player);
        List<Slot> all = slots(player);
        if (action == 6) {
            if (all.size() <= 6) return;
            int pages = (all.size()+5)/6;
            state.page = (state.page+1) % pages;
            tell(player, "message.anomalousclasses.pact.page",state.page+1,pages);
            return;
        }
        int selected = state.page*6+action;
        if (selected >= all.size()) return;
        Slot slot=all.get(selected);
        var patron=slot.patron();
        var spell=slot.spell();
        var cooldowns=PactSavedData.get(player.getServer());
        String key=patron.id()+":"+spell.id();
        long now=System.currentTimeMillis();
        long remain=cooldowns.player(player.getUUID()).readyAt(key)-now;
        if (remain>0) {
            tell(player,"message.anomalousclasses.pact.cooldown",(remain+999)/1000);return;
        }
        if (!cast(player,patron,spell)) return;
        cooldowns.cooldown(player.getUUID(),key, now+spell.cooldownSeconds()*1000L);
        tell(player,"message.anomalousclasses.pact.cast",
                Component.translatable("pact_ability.anomalousclasses."+spell.id()));
    }
    static float power(ServerPlayer player, PactCatalog.Patron patron, double damage) {
        var data=PactSavedData.get(player.getServer()).player(player.getUUID());
        PactSavedData.Bond bond=data.bond(patron.id());
        if (bond==null) return 0;
        var profile=ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        return (float)Math.min(10000.0,damage * PactBlessingService.relationFactor(bond.relation())
                * PactBlessingService.focusFactor(profile));
    }
    static Mob aimed(ServerPlayer player,double reach) {
        Vec3 eye=player.getEyePosition();
        Vec3 end=eye.add(player.getLookAngle().scale(reach));
        BlockHitResult block=player.serverLevel().clip(new ClipContext(eye,end,
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        double blocked=block.getType()==HitResult.Type.BLOCK
                ? Math.sqrt(block.getLocation().distanceToSqr(eye)) : reach;
        Mob best=null;
        double bestScore=Double.POSITIVE_INFINITY;
        for (Mob candidate:player.serverLevel().getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(reach),e->enemy(player,e))) {
            Vec3 relative=candidate.getEyePosition().subtract(eye);
            double distance=relative.length();
            if (distance<.01 || distance>reach || distance>blocked+.8) continue;
            double angle=relative.normalize().dot(player.getLookAngle());
            if (angle<.92 || !player.hasLineOfSight(candidate)) continue;
            double score=distance+(1-angle)*10;
            if (score<bestScore) {bestScore=score;best=candidate;}
        }
        return best;
    }
    static List<Mob> cone(ServerPlayer player,double reach,double cosine,int maximum) {
        List<Mob> out=new ArrayList<>();
        Vec3 eye=player.getEyePosition();
        Vec3 look=player.getLookAngle();
        for (Mob mob:player.serverLevel().getEntitiesOfClass(Mob.class,
                player.getBoundingBox().inflate(reach), e->enemy(player,e))) {
            Vec3 toward=mob.getEyePosition().subtract(eye);
            double length=toward.length();
            if (length<=reach && length>.1 && toward.normalize().dot(look)>=cosine
                    && player.hasLineOfSight(mob)) out.add(mob);
        }
        out.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
        if (out.size()>maximum) return out.subList(0,maximum);
        return out;
    }
    static boolean impact(ServerPlayer player,Mob mob,PactCatalog.Patron patron,double damage) {
        boolean landed=mob.hurt(player.damageSources().source(DamageTypes.MAGIC,player),
                power(player,patron,damage));
        if (landed) CombatTracker.markOwnedCombat(player);
        return landed;
    }
    private static boolean cast(ServerPlayer owner,PactCatalog.Patron patron,PactCatalog.Ability ability) {
        String id=ability.id();
        Battle state=battle(owner);
        long now=owner.getServer().getTickCount();
        switch(id) {
            case "mark" -> {
                Mob target=aimed(owner,16);
                if (target==null) return noTarget(owner);
                if (impact(owner,target,patron,7)) state.marks.put(target.getUUID(),now+160);
                return true;
            }
            case "ward" -> {
                LivingEntity target=owner;
                // Self-protection is the default; friendly support is available from aim.
                var eye=owner.getEyePosition();
                for (var ally:owner.serverLevel().getEntitiesOfClass(LivingEntity.class,
                        owner.getBoundingBox().inflate(12),e->e!=owner&&e.isAlive())) {
                    if ((owner.isAlliedTo(ally) || ally instanceof AbstractVillager
                            || (ally instanceof TamableAnimal tame
                                && owner.getUUID().equals(tame.getOwnerUUID())))
                            && owner.getLookAngle().dot(ally.getEyePosition().subtract(eye).normalize())>.985
                            && owner.hasLineOfSight(ally)) {
                        target=ally; break;
                    }
                }
                state.ward=new Ward(owner.getUUID(),target.getUUID(),now+120);
                return true;
            }
            case "ashen_strike" -> {
                Mob target=aimed(owner,4);
                if (target==null) return noTarget(owner);
                float cost=Math.max(2F,owner.getMaxHealth()*.03F);
                if (owner.getHealth() <= cost+1F) {
                    tell(owner,"message.anomalousclasses.pact.need_health");return false;
                }
                owner.setHealth(owner.getHealth()-cost);
                impact(owner,target,patron,12);
                for (Mob other:owner.serverLevel().getEntitiesOfClass(Mob.class,
                        target.getBoundingBox().inflate(3),m->m!=target&&enemy(owner,m))) {
                    if (other.distanceToSqr(target)<=9) impact(owner,other,patron,5);
                }
                return true;
            }
            case "giants_fist" -> {
                Mob target=aimed(owner,3.5);
                if (target==null) return noTarget(owner);
                if (impact(owner,target,patron,10))
                    target.knockback(.7,owner.getX()-target.getX(),owner.getZ()-target.getZ());
                return true;
            }
            case "ground_shatter" -> {
                List<Mob> targets=cone(owner,6,.58,5);
                if (targets.isEmpty()) return noTarget(owner);
                for (Mob target:targets)
                    if (impact(owner,target,patron,9))
                        target.knockback(.4,owner.getX()-target.getX(),owner.getZ()-target.getZ());
                return true;
            }
            case "star_step" -> {
                Vec3 look=owner.getLookAngle();
                Vec3 eye=owner.getEyePosition();
                BlockHitResult obstruction=owner.serverLevel().clip(new ClipContext(eye,eye.add(look.scale(8)),
                        ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
                double limit=obstruction.getType()==HitResult.Type.BLOCK
                        ? Math.max(0,Math.sqrt(obstruction.getLocation().distanceToSqr(eye))-.65):8;
                for (double distance=Math.min(8,limit);distance>=1.0;distance-=.5) {
                    Vec3 destination=owner.position().add(look.scale(distance));
                    BlockPos block=BlockPos.containing(destination);
                    if (!owner.serverLevel().hasChunkAt(block)
                            || !owner.serverLevel().getWorldBorder().isWithinBounds(block)
                            || destination.y<owner.serverLevel().getMinBuildHeight()+1) continue;
                    AABB newBox=owner.getBoundingBox().move(destination.subtract(owner.position()));
                    if (!owner.serverLevel().noCollision(owner,newBox)) continue;
                    if (!owner.serverLevel().getBlockState(block.below()).isSolid()) continue;
                    owner.teleportTo(destination.x,destination.y,destination.z);
                    state.starredUntil=now+60;
                    state.stepUntil=now+40;
                    return true;
                }
                tell(owner,"message.anomalousclasses.pact.no_space");return false;
            }
            case "starlight" -> {
                Mob target=aimed(owner,15);
                if (target==null) return noTarget(owner);
                double bonus=state.starredUntil>now ? 1.25:1.0;
                if (impact(owner,target,patron,9*bonus)) state.starredUntil=0;
                return true;
            }
            default -> {
                if (patron.tier()==2) return PactTierTwoCombatService.cast(owner,patron,id);
                if (patron.tier()==3) return PactTierThreeCombatService.cast(owner,patron,id);
                return false;
            }
        }
    }
    private static boolean noTarget(ServerPlayer owner) {
        tell(owner,"message.anomalousclasses.pact.no_target");return false;
    }
    private static boolean once(Battle state,UUID id,long now,int cooldown) {
        long ready=state.recentExtras.getOrDefault(id,0L);
        if (now<ready) return false;
        state.recentExtras.put(id,now+cooldown);
        return true;
    }
    private static void incoming(ServerPlayer player,LivingDamageEvent.Pre event) {
        if (!PactService.isPact(player)) return;
        Battle state=battle(player);
        long now=player.getServer().getTickCount();
        float value=event.getNewDamage();
        if (value<=0) return;
        if (owned(player,"ridna") && event.getSource().is(DamageTypes.FALL)) value*=.7F;
        if (owned(player,"ridna") && state.stepUntil>now) value*=.85F;
        if (owned(player,"velkan") && event.getSource().is(DamageTypeTags.IS_FIRE)) value*=.8F;
        if (owned(player,"isael") && state.guardianUntil>now) {
            value*=.90F;state.guardianUntil=0;
        }
        if (owned(player,"urgen") && state.bulwarkUntil>now) {
            value*=.90F;state.bulwarkUntil=0;
        }
        event.setNewDamage(value);
    }
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player) incoming(player,event);
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && PactService.isPact(attacker) && enemy(attacker,event.getEntity())) {
            Battle combat=battle(attacker);
            long now=attacker.getServer().getTickCount();
            double factor=1.0;
            if (owned(attacker,"velkan") && combat.emberUntil>now) factor*=1.1;
            if (owned(attacker,"isael") && combat.marks.getOrDefault(
                    event.getEntity().getUUID(),0L)>now) factor*=1.2;
            event.setNewDamage((float)Math.min(10000,event.getNewDamage()*factor));
        }
        if (event.getEntity().level().isClientSide()
                || event.getEntity().level().getServer()==null || event.getNewDamage()<=0) return;
        MinecraftServer server=event.getEntity().level().getServer();
        long now=server.getTickCount();
        for (var pair:BATTLES.entrySet()) {
            ServerPlayer owner=server.getPlayerList().getPlayer(pair.getKey());
            if (owner==null||!owned(owner,"isael") || owner.level()!=event.getEntity().level()) continue;
            Battle state=pair.getValue();
            Ward ward=state.ward;
            if (ward==null||ward.until()<=now||!ward.protectedId().equals(event.getEntity().getUUID()))
                continue;
            var source=event.getSource().getEntity();
            if (source==owner || source!=null&&owner.isAlliedTo(source)) continue;
            float before=event.getNewDamage();
            float reduction=Math.min(10F,before*.35F);
            if (reduction<=0) continue;
            event.setNewDamage(Math.max(0,before-reduction));
            state.ward=null;
            state.lastIsaelAnswer=now+120;
            if (event.getEntity() instanceof AbstractVillager
                    && now-state.lastGuard>600) {
                state.lastGuard=now;
                PactSavedData.get(server).relation(owner.getUUID(),"isael",1);
                PactBlessingService.apply(owner);
            }
            break;
        }
    }
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getEntity().level().isClientSide() || event.getEntity().level().getServer()==null) return;
        MinecraftServer server=event.getEntity().level().getServer();
        long now=server.getTickCount();
        if (event.getEntity() instanceof ServerPlayer victim && PactService.isPact(victim)) {
            Battle state=battle(victim);
            if (owned(victim,"isael") && (state.lastGuard==0 || now>=state.lastGuard+360)) {
                state.guardianUntil=now+60;state.lastGuard=now;
            }
            if (owned(victim,"urgen") && (state.lastBulwark==0 || now>=state.lastBulwark+360)) {
                state.bulwarkUntil=now+60;state.lastBulwark=now;
            }
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer owner)
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !PactService.isPact(owner) || !enemy(owner,event.getEntity())) return;
        Battle state=battle(owner);
        LivingEntity target=event.getEntity();
        if (owned(owner,"isael")) {
            if (once(state,target.getUUID(),now,60)) {
                target.hurt(owner.damageSources().source(DamageTypes.MAGIC,owner),2);
                if (state.lastIsaelAnswer>now) {
                    target.hurt(owner.damageSources().source(DamageTypes.MAGIC,owner),3);
                    state.lastIsaelAnswer=0;
                }
            }
        }
        if (owned(owner,"velkan")) {
            if (once(state,target.getUUID(),now,80))
                state.burning.put(target.getUUID(),new TimedHit((Mob)target,now+40));
            state.emberUntil=now+120;
            if (now-state.lastEmberHeal>100 && owner.getHealth()<owner.getMaxHealth()) {
                owner.heal(1.0F);state.lastEmberHeal=now;
            }
        }
        if (owned(owner,"urgen")) {
            state.urgenStrikes++;
            if (state.urgenStrikes>=3 && once(state,UUID.nameUUIDFromBytes(("urgen:"+owner.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),now,100)) {
                state.urgenStrikes=0;
                target.hurt(owner.damageSources().source(DamageTypes.MAGIC,owner),3);
            }
        }
        if (owned(owner,"ridna")) {
            Vec3 position=owner.position();
            if (state.lastStrikePosition==null) state.lastStrikePosition=position;
            else if (state.lastStrikePosition.distanceToSqr(position)>1) {
                if (once(state,UUID.nameUUIDFromBytes(("ridna:"+owner.getUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),now,120))
                    target.hurt(owner.damageSources().source(DamageTypes.MAGIC,owner),2);
                state.lastStrikePosition=position;
            }
        }
    }
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || !owned(player,"isael")) return;
        if (!(event.getEntity() instanceof AbstractVillager)
                && !(event.getEntity() instanceof IronGolem golem && golem.getTarget()!=player))
            return;
        PactSavedData.get(player.getServer()).relation(player.getUUID(),"isael",-6);
        PactBlessingService.apply(player);
        tell(player,"message.anomalousclasses.pact.isael_breach");
    }
    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server=event.getServer();
        if (activeServer!=server) {BATTLES.clear();activeServer=server;}
        long now=server.getTickCount();
        var owners=BATTLES.entrySet().iterator();
        while (owners.hasNext()) {
            var pair=owners.next();
            ServerPlayer player=server.getPlayerList().getPlayer(pair.getKey());
            if (player==null||!PactService.isPact(player)) {owners.remove();continue;}
            Battle state=pair.getValue();
            state.marks.entrySet().removeIf(e->e.getValue()<=now);
            state.burning.entrySet().removeIf(entry->{
                TimedHit burn=entry.getValue();
                if (now<burn.due()) return false;
                if (burn.target().isAlive()&&burn.target().level()==player.level()
                        &&enemy(player,burn.target())) {
                    burn.target().hurt(player.damageSources().source(DamageTypes.MAGIC,player),2);
                }
                return true;
            });
            state.recentExtras.entrySet().removeIf(e->e.getValue()+240<now);
            if (state.ward!=null && state.ward.until()<=now) state.ward=null;
        }
    }
    public static void removeBond(ServerPlayer owner,String patron) {
        // Active marks, wards, page selection and ambient triggers must not survive release.
        BATTLES.remove(owner.getUUID());
        PactTierTwoCombatService.clear(owner);
        PactTierThreeCombatService.clear(owner);
        PactTrialService.clear(owner);
    }
    public static void clear(ServerPlayer player) {
        BATTLES.remove(player.getUUID());
        PactTierTwoCombatService.clear(player);
        PactTierThreeCombatService.clear(player);
        PactTrialService.clear(player);
    }
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        BATTLES.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            PactTierTwoCombatService.clear(player);
            PactTierThreeCombatService.clear(player);
            PactTrialService.clear(player);
        }
    }
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        BATTLES.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            PactTierTwoCombatService.clear(player);
            PactTierThreeCombatService.clear(player);
            PactTrialService.clear(player);
            PactService.expire(player);
        }
    }
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer owner) {
            BATTLES.remove(owner.getUUID());
            PactTierTwoCombatService.clear(owner);
            PactTierThreeCombatService.clear(owner);
            PactTrialService.clear(owner);
            PactService.expire(owner);
        }
    }
}
