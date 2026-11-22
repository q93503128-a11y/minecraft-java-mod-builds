package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Four endgame patrons; server-side combat, mitigation and resource spending.
 * Final manifestations, art, and special guardian phases are separate work.
 */
public final class PactTierFourCombatService {
    private static MinecraftServer context;
    private static final Map<UUID, Battle> LIVE = new HashMap<>();
    private static final long FIVE_SECONDS = 100L;
    /** Provisional strong-foe baseline. Bosses and >=70 HP hostiles qualify. */
    private static final double ELITE_MIN_HEALTH = 70.0;
    private static final long FATE_COOLDOWN_MS = 180_000L;

    private static final class Battle {
        long starRecordUntil, starEchoReady, starGuardUntil, starGuardReady;
        long oathBladeReady, oathGuardUntil, oathGuardReady;
        long continentUntil, quakeReady;
        double continentIntegrity;
        long rootCounterReady, rootRegenAt, rootCombatUntil, rootHealWindow;
        double continentAbsorbed;
        final Map<UUID,Long> starFoes=new HashMap<>();
        final Map<UUID,Long> oathFoes=new HashMap<>();
        final Map<UUID,Long> continentFoes=new HashMap<>();
        final Map<UUID,Long> rootedFoes=new HashMap<>();
        float rootHealed;
    }
    private PactTierFourCombatService() {}
    private static Battle state(ServerPlayer player) {
        if(context!=player.getServer()) {LIVE.clear();context=player.getServer();}
        return LIVE.computeIfAbsent(player.getUUID(),key->new Battle());
    }
    public static void clear(ServerPlayer player) {LIVE.remove(player.getUUID());}
    private static long tick(ServerPlayer player) {return player.getServer().getTickCount();}
    private static int depth(ServerPlayer player,String patron) {
        PactSavedData.Bond bond=PactSavedData.get(player.getServer())
                .player(player.getUUID()).bond(patron);
        return bond==null?0:bond.deepening();
    }
    private static boolean strongEnemy(ServerPlayer owner,Mob target) {
        return target.getMaxHealth()>=ELITE_MIN_HEALTH && target(owner,target);
    }
    private static void mark(Map<UUID,Long> observed,UUID target,long expiry) {
        if(observed.size()>=64) {
            observed.entrySet().removeIf(e->e.getValue()<=expiry-240);
            if(observed.size()>=64)observed.clear();
        }
        observed.put(target,expiry);
    }
    private static boolean has(ServerPlayer player,String patron) {
        return PactService.isPact(player) &&
                PactSavedData.get(player.getServer()).player(player.getUUID()).has(patron);
    }
    private static boolean impact(ServerPlayer caster,String id,Mob target,double power) {
        return PactCombatService.impact(caster,target,PactCatalog.get(id),power);
    }
    private static boolean target(ServerPlayer caster,LivingEntity entity) {
        return PactCombatService.enemy(caster,entity) && entity.level()==caster.level();
    }
    private static void hint(ServerPlayer player,String key) {
        player.displayClientMessage(Component.translatable("message.anomalousclasses.pact."+key),true);
    }
    public static boolean cast(ServerPlayer player,PactCatalog.Patron patron,String spell) {
        Battle s=state(player);
        long now=tick(player);
        switch(spell) {
            case "black_star_rebirth" -> {
                Vec3 eye=player.getEyePosition(),look=player.getLookAngle();
                int landed=0;
                boolean recorded=s.starRecordUntil>now;
                for(Mob mob:PactCombatService.cone(player,18,.75,24)) {
                    Vec3 offset=mob.getEyePosition().subtract(eye);
                    double forward=offset.dot(look);
                    if(forward<=0 || forward>18 || offset.subtract(look.scale(forward)).lengthSqr()>25)continue;
                    if(impact(player,"nahar",mob,65)) {
                        landed++;
                        if(recorded)impact(player,"nahar",mob,16.25);
                    }
                }
                if(landed==0){hint(player,"no_target");return false;}
                if(recorded)s.starRecordUntil=0;
                return true;
            }
            case "first_oath_sword" -> {
                var targets=PactCombatService.cone(player,9,.74,18);
                int landed=0;
                for(Mob enemy:targets)if(impact(player,"rakiel",enemy,52))landed++;
                if(landed==0){hint(player,"no_target");return false;}
                if(landed>1)impact(player,"rakiel",targets.get(1),10);
                return true;
            }
            case "continent_skin" -> {
                var bond=PactSavedData.get(player.getServer()).player(player.getUUID()).bond("ormas");
                var profile=ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
                s.continentIntegrity=55*PactBlessingService.relationFactor(bond.relation())
                        *PactBlessingService.focusFactor(profile);
                s.continentUntil=now+200;
                return true;
            }
            case "continent_break" -> {
                int landed=0;
                for(Mob enemy:PactCombatService.cone(player,8,.56,24)) {
                    if(impact(player,"ormas",enemy,60)) {
                        landed++;
                        enemy.knockback(.45,player.getX()-enemy.getX(),player.getZ()-enemy.getZ());
                    }
                }
                if(landed==0)hint(player,"no_target");
                return landed>0;
            }
            case "first_root" -> {
                int landed=0;
                for(Mob enemy:PactCombatService.cone(player,11,.73,24)) {
                    if(impact(player,"viar",enemy,54)) {
                        landed++;
                        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,30,1));
                    }
                }
                if(landed==0)hint(player,"no_target");
                return landed>0;
            }
            default -> {return false;}
        }
    }
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if(event.getEntity() instanceof ServerPlayer victim && PactService.isPact(victim)
                && event.getNewDamage()>0) {
            Battle s=state(victim);
            long now=tick(victim);
            float damage=event.getNewDamage();
            if(has(victim,"nahar") && s.starGuardUntil>now){damage*=.8F;s.starGuardUntil=0;}
            if(has(victim,"rakiel") && s.oathGuardUntil>now){damage*=.85F;s.oathGuardUntil=0;}
            if(has(victim,"ormas")) {
                if(event.getSource().is(DamageTypes.FALL))damage*=.45F;
                if(s.continentUntil>now && s.continentIntegrity>0) {
                    float blocked=(float)Math.min(damage,s.continentIntegrity);
                    s.continentIntegrity-=blocked;
                    damage-=blocked;
                }
            }
            event.setNewDamage(Math.max(0,damage));
        }
        if(event.getSource().getEntity() instanceof ServerPlayer owner
                && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && target(owner,event.getEntity()) && has(owner,"ormas")) {
            Battle s=state(owner);
            if(s.continentUntil>tick(owner) && s.continentIntegrity>0)
                event.setNewDamage((float)Math.min(10000,event.getNewDamage()*1.2));
        }
    }
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if(event.getNewDamage()<=0 || event.getEntity().level().isClientSide())return;
        if(event.getEntity() instanceof ServerPlayer victim && PactService.isPact(victim)
                && event.getSource().getEntity() instanceof Mob attacker
                && target(victim,attacker)) {
            Battle s=state(victim);
            long now=tick(victim);
            if(has(victim,"nahar") && now>=s.starGuardReady) {
                s.starGuardReady=now+320;s.starGuardUntil=now+80;
            }
            if(has(victim,"rakiel") && now>=s.oathGuardReady) {
                s.oathGuardReady=now+320;s.oathGuardUntil=now+60;
            }
            if(has(victim,"viar") && now>=s.rootCounterReady) {
                s.rootCounterReady=now+200;
                impact(victim,"viar",attacker,7);
            }
        }
        if(!(event.getSource().getEntity() instanceof ServerPlayer owner)
                || !target(owner,event.getEntity()) || !PactService.isPact(owner))return;
        Battle s=state(owner);
        long now=tick(owner);
        LivingEntity enemy=event.getEntity();
        boolean weapon=event.getSource().is(DamageTypes.PLAYER_ATTACK);
        if(weapon && has(owner,"nahar")) {
            s.starRecordUntil=now+400;
            if(now>=s.starEchoReady && enemy instanceof Mob mob) {
                s.starEchoReady=now+80;
                impact(owner,"nahar",mob,6);
            }
        }
        if(weapon && has(owner,"rakiel") && now>=s.oathBladeReady && enemy instanceof Mob mob) {
            s.oathBladeReady=now+80;
            impact(owner,"rakiel",mob,6);
        }
        if(weapon && has(owner,"ormas") && now>=s.quakeReady && enemy instanceof Mob) {
            s.quakeReady=now+FIVE_SECONDS;
            int hits=0;
            for(Mob other:owner.serverLevel().getEntitiesOfClass(Mob.class,
                    enemy.getBoundingBox().inflate(3),mob->mob!=enemy&&target(owner,mob))) {
                if(owner.hasLineOfSight(other) && impact(owner,"ormas",other,8) && ++hits>=2)break;
            }
        }
        if(has(owner,"viar")) {
            if(now>=s.rootHealWindow+FIVE_SECONDS || s.rootHealWindow==0) {
                s.rootHealWindow=now;s.rootHealed=0;
            }
            float allowed=Math.max(0,7-s.rootHealed);
            float gained=Math.min(allowed,event.getNewDamage()*.12F);
            if(gained>0 && owner.getHealth()<owner.getMaxHealth()) {
                float actual=Math.min(gained,owner.getMaxHealth()-owner.getHealth());
                owner.heal(actual);s.rootHealed+=actual;
            }
            s.rootCombatUntil=now+140;
        }
    }
    private static boolean contractedAtDeath(ServerPlayer victim,String patron) {
        // Player#isAlive is false after fatal damage; PactService.isPact must
        // never be used as the identity check inside LivingDeathEvent.
        return victim.getServer()!=null
                && ClassProfileSavedData.get(victim.getServer()).profile(victim.getUUID())
                        .currentClass().orElse(null)==ClassId.PACT_CASTER
                && PactSavedData.get(victim.getServer()).player(victim.getUUID()).has(patron);
    }
    /**
     * Vanilla checks its totem before firing LivingDeathEvent. A successful
     * totem never reaches this handler. The optional rescue spends exactly
     * two gold blocks and persists its cooldown before cancelling one death.
     */
    public static void onDeath(LivingDeathEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer victim) || event.isCanceled()
                || !contractedAtDeath(victim,"rakiel"))return;
        PactSavedData saved=PactSavedData.get(victim.getServer());
        long now=System.currentTimeMillis();
        if(saved.player(victim.getUUID()).readyAt("rakiel:fate_refusal")>now)return;
        int available=0;
        for(ItemStack stack:victim.getInventory().items)
            if(stack.is(Items.GOLD_BLOCK))available+=stack.getCount();
        if(available<2)return;
        int remaining=2;
        for(ItemStack stack:victim.getInventory().items) {
            if(!stack.is(Items.GOLD_BLOCK))continue;
            int used=Math.min(remaining,stack.getCount());
            stack.shrink(used);remaining-=used;
            if(remaining==0)break;
        }
        victim.getInventory().setChanged();
        saved.cooldown(victim.getUUID(),"rakiel:fate_refusal",now+FATE_COOLDOWN_MS);
        victim.setHealth(Math.max(1F,victim.getMaxHealth()*.2F));
        event.setCanceled(true);
        hint(victim,"fate_refused");
    }
    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server=event.getServer();
        if(context!=server){LIVE.clear();context=server;}
        if(server.getTickCount()%20!=0)return;
        long now=server.getTickCount();
        var iterator=LIVE.entrySet().iterator();
        while(iterator.hasNext()) {
            var row=iterator.next();
            ServerPlayer owner=server.getPlayerList().getPlayer(row.getKey());
            if(owner==null||!PactService.isPact(owner)){iterator.remove();continue;}
            Battle s=row.getValue();
            if(s.continentUntil<=now)s.continentIntegrity=0;
            if(has(owner,"viar") && s.rootCombatUntil>now && owner.getHealth()<owner.getMaxHealth()
                    && now>=s.rootRegenAt) {
                s.rootRegenAt=now+FIVE_SECONDS;owner.heal(1);
            }
        }
    }
}
