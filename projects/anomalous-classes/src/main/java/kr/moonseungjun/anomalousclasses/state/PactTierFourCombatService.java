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
                int stage=depth(player,"nahar"),landed=0;
                boolean recorded=s.starRecordUntil>now;
                Mob first=null;
                for(Mob mob:PactCombatService.cone(player,18+stage,.75,24)) {
                    Vec3 offset=mob.getEyePosition().subtract(eye);
                    double forward=offset.dot(look);
                    if(forward<=0 || forward>18+stage
                            || offset.subtract(look.scale(forward)).lengthSqr()>25)continue;
                    boolean elite=stage>=1 && recorded && strongEnemy(player,mob);
                    if(elite)mark(s.starFoes,mob.getUUID(),now+240);
                    if(impact(player,"nahar",mob,65)) {
                        landed++;
                        if(first==null)first=mob;
                        if(recorded && mob.isAlive())
                            impact(player,"nahar",mob,16.25+5*stage);
                    } else if(elite) s.starFoes.remove(mob.getUUID());
                }
                if(landed==0){hint(player,"no_target");return false;}
                if(recorded && first!=null)
                    PactDeepeningService.record(player,"nahar","rebirth_recorded_landed",first);
                if(recorded) {
                    s.starRecordUntil=0;
                    if(stage>=2)s.starGuardUntil=Math.max(s.starGuardUntil,now+80);
                }
                return true;
            }
            case "first_oath_sword" -> {
                int stage=depth(player,"rakiel");
                var targets=PactCombatService.cone(player,9+stage,.74,18);
                boolean guarded=s.oathGuardUntil>now;
                int landed=0;
                Mob first=null,extra=null;
                for(Mob enemy:targets) {
                    boolean elite=stage>=1 && guarded && strongEnemy(player,enemy);
                    if(elite)mark(s.oathFoes,enemy.getUUID(),now+240);
                    if(impact(player,"rakiel",enemy,52)) {
                        landed++;
                        if(first==null)first=enemy;
                        else if(extra==null)extra=enemy;
                    } else if(elite)s.oathFoes.remove(enemy.getUUID());
                }
                if(landed==0){hint(player,"no_target");return false;}
                if(guarded && first!=null) {
                    PactDeepeningService.record(player,"rakiel","oath_sword_after_guard",first);
                    if(stage>=2)s.oathGuardUntil=Math.max(s.oathGuardUntil,now+80);
                }
                if(landed>1 && extra!=null && extra.isAlive())
                    impact(player,"rakiel",extra,10+3*stage);
                return true;
            }
            case "continent_skin" -> {
                var bond=PactSavedData.get(player.getServer()).player(player.getUUID()).bond("ormas");
                var profile=ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
                int stage=depth(player,"ormas");
                s.continentIntegrity=55*PactBlessingService.relationFactor(bond.relation())
                        *PactBlessingService.focusFactor(profile)
                        *PactBlessingService.deepeningFactor(patron,stage);
                s.continentUntil=now+200+20L*stage;
                s.continentAbsorbed=0;
                return true;
            }
            case "continent_break" -> {
                int stage=depth(player,"ormas");
                boolean armored=s.continentUntil>now && s.continentIntegrity>0;
                boolean tested=s.continentAbsorbed>=16 && armored;
                int landed=0;
                Mob first=null;
                for(Mob enemy:PactCombatService.cone(player,8,.56,24)) {
                    boolean elite=stage>=1 && tested && strongEnemy(player,enemy);
                    if(elite)mark(s.continentFoes,enemy.getUUID(),now+240);
                    if(impact(player,"ormas",enemy,60*(tested && stage>=2?1.25:1.0))) {
                        landed++;
                        if(first==null)first=enemy;
                        if(enemy.isAlive())enemy.knockback(.45+.05*stage,
                                player.getX()-enemy.getX(),player.getZ()-enemy.getZ());
                    } else if(elite)s.continentFoes.remove(enemy.getUUID());
                }
                if(landed==0){hint(player,"no_target");return false;}
                if(armored && first!=null)
                    PactDeepeningService.record(player,"ormas","armored_continent_break",first);
                if(tested && stage>=2)s.continentAbsorbed=0;
                return true;
            }
            case "first_root" -> {
                int stage=depth(player,"viar"),landed=0;
                for(Mob enemy:PactCombatService.cone(player,11+stage,.73,24)) {
                    if(impact(player,"viar",enemy,54)) {
                        landed++;
                        if(enemy.isAlive()) {
                            mark(s.rootedFoes,enemy.getUUID(),now+160);
                            enemy.addEffect(new MobEffectInstance(
                                    MobEffects.MOVEMENT_SLOWDOWN,30+15*stage,1));
                        }
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
                    if(blocked>0 && event.getSource().getEntity() instanceof Mob foe
                            && target(victim,foe))
                        s.continentAbsorbed=Math.min(80,s.continentAbsorbed+blocked);
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
                s.starGuardReady=now+320;
                s.starGuardUntil=now+80+20L*depth(victim,"nahar");
            }
            if(has(victim,"rakiel") && now>=s.oathGuardReady) {
                s.oathGuardReady=now+320;
                s.oathGuardUntil=now+60+20L*depth(victim,"rakiel");
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
            s.starRecordUntil=now+400+80L*depth(owner,"nahar");
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
            int stage=depth(owner,"viar");
            float allowed=Math.max(0,(7+2*stage)-s.rootHealed);
            float gained=Math.min(allowed,event.getNewDamage()*(.12F+.02F*stage));
            if(gained>0 && owner.getHealth()<owner.getMaxHealth()) {
                float actual=Math.min(gained,owner.getMaxHealth()-owner.getHealth());
                owner.heal(actual);s.rootHealed+=actual;
                Long rootedUntil=s.rootedFoes.get(enemy.getUUID());
                if(actual>=.5F && rootedUntil!=null && rootedUntil>now
                        && enemy instanceof Mob mob) {
                    // Require a real wound, a previous root hit, and healing
                    // from a successful follow-up on that rooted hostile.
                    s.rootedFoes.remove(enemy.getUUID());
                    PactDeepeningService.record(owner,"viar","roots_to_real_healing",mob);
                    PactDeepeningService.recordViarBiome(owner,mob);
                }
            }
            s.rootCombatUntil=now+140;
        }
    }
    /**
     * Stage-two high-threat conquest: actual hostile death with this caster as
     * the attributed attacker AND a recent successful themed spell strike.
     * Ordinary monster deaths, owned/controlled targets and idle XP farms
     * cannot grant a conquest. Direct magic finishing blows also qualify.
     */
    private static void onEliteDeath(LivingDeathEvent event,Mob hostile) {
        if(!(event.getSource().getEntity() instanceof ServerPlayer owner)
                || owner.getServer()==null || !PactService.isPact(owner)
                || hostile.level()!=owner.level() || !(hostile instanceof Enemy)
                || hostile.getMaxHealth()<ELITE_MIN_HEALTH
                || hostile instanceof PuppetEntity
                || owner.isAlliedTo(hostile)
                || hostile.getPersistentData().getBoolean("anomalousclasses_trial_guardian")
                || MentalDominatorSavedData.get(owner.getServer()).controlled(hostile.getUUID())!=null
                || ParasiteSavedData.get(owner.getServer()).ownerOf(hostile.getUUID())!=null)return;
        Battle battle=state(owner);
        long time=tick(owner);
        Long star=battle.starFoes.remove(hostile.getUUID());
        if(star!=null && star>=time && has(owner,"nahar"))
            PactDeepeningService.record(owner,"nahar","star_recorded_elite_victory",hostile);
        Long oath=battle.oathFoes.remove(hostile.getUUID());
        if(oath!=null && oath>=time && has(owner,"rakiel"))
            PactDeepeningService.record(owner,"rakiel","oath_guarded_elite_victory",hostile);
        Long stone=battle.continentFoes.remove(hostile.getUUID());
        if(stone!=null && stone>=time && has(owner,"ormas"))
            PactDeepeningService.record(owner,"ormas","continent_warded_elite_victory",hostile);
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
        if(event.isCanceled())return;
        if(event.getEntity() instanceof Mob foe) {
            onEliteDeath(event,foe);
            return;
        }
        if(!(event.getEntity() instanceof ServerPlayer victim)
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
        float restore=depth(victim,"rakiel")>=2?.28F:.20F;
        victim.setHealth(Math.max(1F,victim.getMaxHealth()*restore));
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
            if(s.continentUntil<=now) {
                s.continentIntegrity=0;
                s.continentAbsorbed=0;
            }
            s.rootedFoes.entrySet().removeIf(e->e.getValue()<=now);
            s.starFoes.entrySet().removeIf(e->e.getValue()<=now);
            s.oathFoes.entrySet().removeIf(e->e.getValue()<=now);
            s.continentFoes.entrySet().removeIf(e->e.getValue()<=now);
            if(has(owner,"viar") && s.rootCombatUntil>now && owner.getHealth()<owner.getMaxHealth()
                    && now>=s.rootRegenAt) {
                s.rootRegenAt=now+FIVE_SECONDS;owner.heal(1);
            }
        }
    }
}
