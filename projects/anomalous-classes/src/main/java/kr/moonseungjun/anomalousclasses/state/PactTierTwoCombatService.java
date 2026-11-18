package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Tier-two identity: storm mobility, ocean pressure, near-miss observation, and debt-backed combat.
 * None of these passives requires a second contract.
 */
public final class PactTierTwoCombatService {
    private static final Map<UUID, State> LIVE=new HashMap<>();
    private static MinecraftServer serverContext;
    private static final class State {
        long stormUntil, stormLandingPending, chargeUntil, chargeReadyAt, lastStormLanding;
        long pressureUntil, lastSeaHeal, sheroDodgeUntil, lastSheroPrecision;
        long hasteShieldUntil, repayWindow;
        double repaidInWindow, debtRemainder;
        long fullyPaidUntil, lastFullReward;
        long lastWarning;
    }
    private PactTierTwoCombatService() {}
    private static State state(ServerPlayer player) {
        if(serverContext!=player.getServer()){serverContext=player.getServer();LIVE.clear();}
        return LIVE.computeIfAbsent(player.getUUID(),id->new State());
    }
    public static void clear(ServerPlayer player) { LIVE.remove(player.getUUID()); }
    private static boolean has(ServerPlayer player,String id) {
        return PactService.isPact(player) &&
                PactSavedData.get(player.getServer()).player(player.getUUID()).has(id);
    }
    private static boolean hit(ServerPlayer player,PactCatalog.Patron patron,Mob victim,double damage) {
        return PactCombatService.impact(player,victim,patron,damage);
    }
    private static long tick(ServerPlayer player) { return player.getServer().getTickCount(); }
    private static void say(ServerPlayer owner,String key) {
        owner.displayClientMessage(Component.translatable(key),true);
    }

    public static boolean cast(ServerPlayer owner,PactCatalog.Patron patron,String spell) {
        long time=tick(owner);
        State state=state(owner);
        switch(spell) {
            case "storm_leap" -> {
                Vec3 look=owner.getLookAngle();
                Vec3 horizontal=new Vec3(look.x,0,look.z);
                if(horizontal.lengthSqr()<.05) horizontal=new Vec3(0,0,1);
                horizontal=horizontal.normalize();
                Vec3 start=owner.getEyePosition();
                for(double distance=8;distance>=2;distance-=1) {
                    for(double height=5;height>=1;height-=1) {
                        Vec3 newPosition=owner.position().add(horizontal.scale(distance)).add(0,height,0);
                        BlockPos at=BlockPos.containing(newPosition);
                        if(!owner.serverLevel().hasChunkAt(at)
                                || !owner.serverLevel().getWorldBorder().isWithinBounds(at)
                                || newPosition.y>=owner.serverLevel().getMaxBuildHeight()-3) continue;
                        Vec3 toEye=newPosition.add(0,owner.getEyeHeight(),0);
                        BlockHitResult hit=owner.serverLevel().clip(new ClipContext(start,toEye,
                                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
                        if(hit.getType()!=HitResult.Type.MISS) continue;
                        AABB box=owner.getBoundingBox().move(newPosition.subtract(owner.position()));
                        if(!owner.serverLevel().noCollision(owner,box)) continue;
                        boolean landing=false;
                        for(int below=1;below<=6;below++) {
                            BlockPos under=at.below(below);
                            if(owner.serverLevel().getBlockState(under).isSolid()) {
                                landing=true;break;
                            }
                        }
                        if(!landing) continue;
                        owner.teleportTo(newPosition.x,newPosition.y,newPosition.z);
                        state.stormUntil=time+60;
                        state.stormLandingPending=time+100;
                        return true;
                    }
                }
                say(owner,"message.anomalousclasses.pact.no_space");
                return false;
            }
            case "thunder_slam" -> {
                Vec3 center=owner.position().add(owner.getLookAngle().normalize().scale(4));
                int hits=0;
                double power=state.stormUntil>time?1.25:1;
                for(Mob victim:owner.serverLevel().getEntitiesOfClass(Mob.class,
                        new AABB(center.subtract(3.5,3.5,3.5),center.add(3.5,3.5,3.5)),
                        target->PactCombatService.enemy(owner,target))) {
                    if(victim.position().distanceToSqr(center)>12.25
                            || !owner.hasLineOfSight(victim)) continue;
                    if(hit(owner,patron,victim,16*power)) hits++;
                }
                if(hits==0) {
                    say(owner,"message.anomalousclasses.pact.no_target");return false;
                }
                state.stormUntil=0;
                state.chargeUntil=time+60;
                return true;
            }
            case "tide_grasp" -> {
                Mob victim=PactCombatService.aimed(owner,12);
                if(victim==null) {say(owner,"message.anomalousclasses.pact.no_target");return false;}
                if(hit(owner,patron,victim,6)) {
                    Vec3 drag=owner.position().subtract(victim.position());
                    if(drag.lengthSqr()>.1) {
                        drag=drag.normalize().scale(.65);
                        victim.push(drag.x,.15,drag.z);
                        victim.hurtMarked=true;
                    }
                    state.pressureUntil=time+80;
                }
                return true;
            }
            case "abyss_pressure" -> {
                Mob focus=PactCombatService.aimed(owner,9);
                if(focus==null) {say(owner,"message.anomalousclasses.pact.no_target");return false;}
                Vec3 center=focus.position();
                int hits=0;
                for(Mob victim:owner.serverLevel().getEntitiesOfClass(Mob.class,
                        focus.getBoundingBox().inflate(2.5),
                        target->PactCombatService.enemy(owner,target))) {
                    if(victim.position().distanceToSqr(center)>6.25
                            || !owner.hasLineOfSight(victim)) continue;
                    if(hit(owner,patron,victim,16*(owner.isInWater()?1.2:1.0))) hits++;
                }
                if(hits==0) return false;
                state.pressureUntil=time+80;
                return true;
            }
            case "observed_retribution" -> {
                Mob victim=PactCombatService.aimed(owner,14);
                if(victim==null) {say(owner,"message.anomalousclasses.pact.no_target");return false;}
                if(hit(owner,patron,victim,14*(state.sheroDodgeUntil>time?1.35:1.0)))
                    state.sheroDodgeUntil=0;
                return true;
            }
            case "advance_claim" -> {
                PactSavedData save=PactSavedData.get(owner.getServer());
                PactSavedData.Bond bond=save.player(owner.getUUID()).bond("hastel");
                if(bond==null || bond.debt()>12) {
                    say(owner,"message.anomalousclasses.pact.outstanding_debt");return false;
                }
                int damaged=0;
                for(Mob target:owner.serverLevel().getEntitiesOfClass(Mob.class,
                        owner.getBoundingBox().inflate(4),
                        mob->PactCombatService.enemy(owner,mob) && mob.distanceToSqr(owner)<=16)) {
                    if(owner.hasLineOfSight(target) && hit(owner,patron,target,15))damaged++;
                }
                if(damaged==0) {
                    say(owner,"message.anomalousclasses.pact.no_target");return false;
                }
                // The initial damage landed before the new debt exists, so it never self-repays.
                save.debt(owner.getUUID(),"hastel",bond.debt()+12);
                state.hasteShieldUntil=time+120;
                return true;
            }
            default -> {return false;}
        }
    }

    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity() instanceof ServerPlayer player && PactService.isPact(player)) {
            State state=state(player);
            long time=tick(player);
            float damage=event.getNewDamage();
            if (has(player,"karda") && event.getSource().is(DamageTypes.FALL))
                damage*=.70F;
            if (has(player,"hastel")) {
                if (state.hasteShieldUntil>time) damage*=.8F;
                PactSavedData.Bond bond=PactSavedData.get(player.getServer()).player(player.getUUID())
                        .bond("hastel");
                if(bond!=null&&bond.debt()>0) damage*=.9F;
            }
            if (has(player,"nerea") && state.pressureUntil>time) {
                damage*=.85F;
                state.pressureUntil=0;
            }
            event.setNewDamage(damage);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer owner
                && event.getSource().is(DamageTypes.PLAYER_ATTACK) && PactService.isPact(owner)
                && PactCombatService.enemy(owner,event.getEntity())) {
            State state=state(owner);
            if(has(owner,"shero") && CombatTracker.isLocked(owner))
                event.setNewDamage((float)(event.getNewDamage()*1.08));
        }
    }
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || !PactService.isPact(attacker)
                || !PactCombatService.enemy(attacker,event.getEntity())
                || event.getNewDamage()<=0) return;
        State state=state(attacker);
        long now=tick(attacker);
        if (has(attacker,"karda") && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && state.chargeUntil>now && now>=state.chargeReadyAt) {
            state.chargeUntil=0;
            state.chargeReadyAt=now+100;
            event.getEntity().hurt(attacker.damageSources().source(DamageTypes.MAGIC,attacker),3.0F);
        }
        if (has(attacker,"nerea") && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && state.pressureUntil>now) {
            state.pressureUntil=0;
            event.getEntity().hurt(attacker.damageSources().source(DamageTypes.MAGIC,attacker),3.0F);
        }
        if (has(attacker,"nerea") && attacker.isInWater()
                && now-state.lastSeaHeal>=120L && attacker.getHealth()<attacker.getMaxHealth()) {
            state.lastSeaHeal=now;
            attacker.heal(.8F);
        }
        if (has(attacker,"shero") && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && state.sheroDodgeUntil>now && now-state.lastSheroPrecision>=100L) {
            state.lastSheroPrecision=now;
            state.sheroDodgeUntil=0;
            event.getEntity().hurt(attacker.damageSources().source(DamageTypes.MAGIC,attacker),3.0F);
        }
        if (has(attacker,"hastel")) {
            PactSavedData save=PactSavedData.get(attacker.getServer());
            PactSavedData.Bond bond=save.player(attacker.getUUID()).bond("hastel");
            if(bond==null) return;
            if(bond.debt()>0) {
                if(now-state.repayWindow>=20) {
                    state.repayWindow=now;
                    state.repaidInWindow=0;
                }
                double allowed=Math.max(0,5.0-state.repaidInWindow);
                double earned=Math.min(allowed,event.getNewDamage()*.35);
                state.debtRemainder+=earned;
                int repay=(int)Math.min(Math.floor(state.debtRemainder),allowed);
                if(repay>0) {
                    state.debtRemainder-=repay;
                    state.repaidInWindow+=repay;
                    int left=Math.max(0,bond.debt()-repay);
                    save.debt(attacker.getUUID(),"hastel",left);
                    if(left==0) state.fullyPaidUntil=now+160;
                }
            } else if(event.getSource().is(DamageTypes.PLAYER_ATTACK)
                    && state.fullyPaidUntil>now && now>=state.lastFullReward+240) {
                state.fullyPaidUntil=0;
                state.lastFullReward=now;
                event.getEntity().hurt(attacker.damageSources().source(DamageTypes.MAGIC,attacker),4.0F);
            }
        }
    }
    public static void onProjectile(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Projectile projectile)
                || !(projectile.level().getServer() instanceof MinecraftServer server)
                || !(projectile.getOwner() instanceof Mob hostile)
                || event.getRayTraceResult().getType()!=HitResult.Type.BLOCK) return;
        Vec3 impact=event.getRayTraceResult().getLocation();
        for(ServerPlayer player:server.getPlayerList().getPlayers()) {
            if(player.level()!=projectile.level()||!has(player,"shero")
                    || player.distanceToSqr(impact)>9
                    || player.getDeltaMovement().horizontalDistanceSqr()<.02
                    || !PactCombatService.enemy(player,hostile)) continue;
            state(player).sheroDodgeUntil=tick(player)+60;
        }
    }
    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server=event.getServer();
        if (serverContext!=server) {LIVE.clear();serverContext=server;}
        long time=server.getTickCount();
        if (time%10!=0) return;
        // A water-breathing grace must not require casting a skill first.
        if (time%20==0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers())
                if (PactService.isPact(player) && has(player,"nerea") && player.isInWater())
                    player.setAirSupply(player.getMaxAirSupply());
        }
        var it=LIVE.entrySet().iterator();
        while(it.hasNext()) {
            var row=it.next();
            ServerPlayer owner=server.getPlayerList().getPlayer(row.getKey());
            if(owner==null||!PactService.isPact(owner)){it.remove();continue;}
            State state=row.getValue();
            if(has(owner,"karda") && state.stormLandingPending>time && owner.onGround()) {
                state.stormLandingPending=0;
                state.lastStormLanding=time;
                owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,40,0,false,false));
                for(Mob target:owner.serverLevel().getEntitiesOfClass(Mob.class,
                        owner.getBoundingBox().inflate(2),
                        mob->PactCombatService.enemy(owner,mob)&&mob.distanceToSqr(owner)<4)) {
                    if(owner.hasLineOfSight(target))
                        target.hurt(owner.damageSources().source(DamageTypes.MAGIC,owner),4);
                }
            }
            if(has(owner,"shero")&&CombatTracker.isLocked(owner)&&time-state.lastWarning>=80L) {
                for(Mob target:owner.serverLevel().getEntitiesOfClass(Mob.class,
                        owner.getBoundingBox().inflate(8),
                        mob->PactCombatService.enemy(owner,mob) && mob.getTarget()==owner)) {
                    if(owner.hasLineOfSight(target)) {
                        say(owner,"message.anomalousclasses.pact.shero_warning");
                        state.lastWarning=time;
                        break;
                    }
                }
            }
        }
    }
}
