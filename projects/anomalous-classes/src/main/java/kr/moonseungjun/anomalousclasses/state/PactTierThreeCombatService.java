package kr.moonseungjun.anomalousclasses.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Tier-three combat: four different strong stand-alone powers with automatic situational
 * passives. No arbitrary clone of external mob skills and no terrain destruction.
 */
public final class PactTierThreeCombatService {
    private static MinecraftServer activeServer;
    private static final Map<UUID, Battle> ACTIVE = new HashMap<>();

    private static final class Battle {
        final Map<UUID, Long> verdictMarks = new HashMap<>();
        final Map<UUID, Long> fireMarks = new HashMap<>();
        long verdictGuardReady, verdictGuardUntil, verdictEchoReady;
        long fireAfterglowUntil, fireEchoReady;
        long stoneUntil, stoneReflectReady, stoneStrikeReady;
        double stoneIntegrity;
        long recordAdaptUntil, recordAdaptReady, focusUntil, focusReady;
        String lastObserved = "";
    }
    private PactTierThreeCombatService() {}
    private static Battle battle(ServerPlayer player) {
        if (activeServer != player.getServer()) {
            ACTIVE.clear();
            activeServer = player.getServer();
        }
        return ACTIVE.computeIfAbsent(player.getUUID(), key -> new Battle());
    }
    public static void clear(ServerPlayer player) { ACTIVE.remove(player.getUUID()); }
    private static boolean has(ServerPlayer player, String id) {
        return PactService.isPact(player)
                && PactSavedData.get(player.getServer()).player(player.getUUID()).has(id);
    }
    private static long now(ServerPlayer player) { return player.getServer().getTickCount(); }
    private static boolean hit(ServerPlayer owner, PactCatalog.Patron patron, Mob target, double amount) {
        return PactCombatService.impact(owner, target, patron, amount);
    }
    private static void hint(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    public static boolean cast(ServerPlayer player, PactCatalog.Patron patron, String spell) {
        Battle state = battle(player);
        long tick = now(player);
        switch (spell) {
            case "final_verdict" -> {
                int stage=depth(player,"sevenwing");
                var targets=PactCombatService.cone(player,7+stage,.78,18);
                if(targets.isEmpty()){hint(player,"message.anomalousclasses.pact.no_target");return false;}
                int landed=0;
                Mob proofTarget=null,secondary=null;
                for(Mob target:targets) {
                    boolean marked=state.verdictMarks.getOrDefault(target.getUUID(),0L)>tick;
                    double modifier=marked?1.25:1.0;
                    if(hit(player,patron,target,28*modifier)) {
                        landed++;
                        if(marked && proofTarget==null)proofTarget=target;
                        if(secondary==null && target!=proofTarget)secondary=target;
                    }
                }
                if(landed==0)return false;
                if(proofTarget!=null){
                    PactDeepeningService.record(player,"sevenwing","marked_verdict_landed",proofTarget);
                    if(stage>=2)state.verdictGuardUntil=Math.max(state.verdictGuardUntil,tick+60);
                }
                if(landed>=2 && secondary!=null && secondary.isAlive()
                        && tick>=state.verdictEchoReady) {
                    hit(player,patron,secondary,6+2*stage);
                    state.verdictEchoReady=tick+120;
                }
                return true;
            }
            case "royal_flame" -> {
                int stage=depth(player,"merzad");
                var targets=PactCombatService.cone(player,9,.70,18);
                if(targets.isEmpty()){hint(player,"message.anomalousclasses.pact.no_target");return false;}
                int landed=0,chained=0;
                Mob proofTarget=null;
                for(Mob target:targets) {
                    boolean marked=state.fireMarks.getOrDefault(target.getUUID(),0L)>tick;
                    if(hit(player,patron,target,30)) {
                        landed++;
                        if(marked) {
                            if(proofTarget==null)proofTarget=target;
                            if(chained<3+(stage>=2?1:0) && target.isAlive()) {
                                hit(player,patron,target,8+2*stage);
                                chained++;
                            }
                        }
                        state.fireMarks.put(target.getUUID(),tick+120+20L*stage);
                    }
                }
                if(landed==0)return false;
                if(proofTarget!=null)
                    PactDeepeningService.record(player,"merzad","crowned_flame_chain",proofTarget);
                state.fireAfterglowUntil=tick+100+20L*stage;
                return true;
            }
            case "kings_stone_armor" -> {
                double relation=PactBlessingService.relationFactor(
                        PactSavedData.get(player.getServer()).player(player.getUUID()).bond("taron").relation());
                double focus=PactBlessingService.focusFactor(
                        ClassProfileSavedData.get(player.getServer()).profile(player.getUUID()));
                int stage=depth(player,"taron");
                state.stoneIntegrity=34.0*relation*focus*
                        PactBlessingService.deepeningFactor(patron,stage);
                state.stoneUntil=tick+180+20L*stage;
                state.stoneShatterFollowupUntil=0;
                return true;
            }
            case "mountain_sunder" -> {
                var targets=PactCombatService.cone(player,7,.78,16);
                if(targets.isEmpty()){hint(player,"message.anomalousclasses.pact.no_target");return false;}
                int stage=depth(player,"taron");
                boolean armored=state.stoneUntil>tick && state.stoneIntegrity>0;
                boolean shatteredResponse=stage>=2 && state.stoneShatterFollowupUntil>tick;
                int landed=0;
                Mob proofTarget=null;
                for(Mob target:targets) {
                    if(hit(player,patron,target,28*(shatteredResponse?1.25:1.0))) {
                        landed++;
                        if(proofTarget==null)proofTarget=target;
                        target.knockback(.35+.05*stage,player.getX()-target.getX(),
                                player.getZ()-target.getZ());
                    }
                }
                if(landed>0 && armored)
                    PactDeepeningService.record(player,"taron","armored_mountain_sunder",proofTarget);
                if(landed>0 && shatteredResponse)state.stoneShatterFollowupUntil=0;
                return landed>0;
            }
            case "record_reprise" -> {
                Mob target=PactCombatService.aimed(player,16);
                if(target==null){hint(player,"message.anomalousclasses.pact.no_target");return false;}
                var observations=PactSavedData.get(player.getServer()).player(player.getUUID()).observations();
                boolean fresh=!state.lastObserved.isBlank() && state.recordAdaptUntil>tick
                        && observations.contains(state.lastObserved);
                if(!hit(player,patron,target,25))return false;
                int stage=depth(player,"arken");
                if(fresh)
                    PactDeepeningService.record(player,"arken","observed_record_reprise",target);
                if(!observations.isEmpty()) {
                    String recent=observations.get(observations.size()-1);
                    switch(recent) {
                        case "melee" -> {if(target.isAlive())hit(player,patron,target,3+stage);}
                        case "projectile" -> {if(target.isAlive())hit(player,patron,target,4+stage);}
                        case "fire" -> {if(target.isAlive())target.setRemainingFireTicks(
                                Math.max(target.getRemainingFireTicks(),40+15*stage));}
                        case "impact" -> {if(target.isAlive())target.knockback(.6+.1*stage,
                                player.getX()-target.getX(),player.getZ()-target.getZ());}
                        default -> { }
                    }
                    if(fresh && stage>=2 && !state.lastRepriseProfile.isBlank()
                            && !state.lastRepriseProfile.equals(recent) && target.isAlive())
                        hit(player,patron,target,6);
                    if(fresh)state.lastRepriseProfile=recent;
                }
                state.focusUntil=tick+100+20L*stage;
                return true;
            }
            default -> {return false;}
        }
    }

    private static String profile(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) return "fire";
        if (source.getDirectEntity() instanceof Projectile
                || source.is(DamageTypeTags.IS_PROJECTILE)) return "projectile";
        if (source.is(DamageTypes.FALL) || source.is(DamageTypes.FLY_INTO_WALL)) return "impact";
        if (source.getEntity() instanceof Mob) return "melee";
        return "";
    }
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        // Incoming damage: shield, divine protection, and observed-profile adaptation.
        if(event.getEntity() instanceof ServerPlayer victim && PactService.isPact(victim)) {
            Battle state=battle(victim);
            long tick=now(victim);
            float value=event.getNewDamage();
            Entity source=event.getSource().getEntity();
            if(value<=0) return;
            if (has(victim,"merzad") && event.getSource().is(DamageTypeTags.IS_FIRE))
                value*=.75F;
            if (has(victim,"sevenwing") && state.verdictGuardUntil>tick) {
                value*=.85F; state.verdictGuardUntil=0;
            }
            if (has(victim,"taron") && state.stoneUntil>tick && state.stoneIntegrity>0) {
                float blocked=(float)Math.min(value,state.stoneIntegrity);
                state.stoneIntegrity-=blocked;
                value-=blocked;
                if (blocked>0 && source instanceof Mob mob
                        && PactCombatService.enemy(victim,mob) && tick>=state.stoneReflectReady) {
                    state.stoneReflectReady=tick+140;
                    // Reflected damage cannot recursively trigger a second reflection.
                    PactCatalog.Patron patron=PactCatalog.get("taron");
                    hit(victim,patron,mob,6);
                }
            }
            String damageType=profile(event.getSource());
            if (has(victim,"arken") && !damageType.isBlank()
                    && damageType.equals(state.lastObserved)
                    && state.recordAdaptUntil>tick && tick>=state.recordAdaptReady) {
                value*=.88F;
                state.recordAdaptReady=tick+200;
                state.recordAdaptUntil=0;
            }
            event.setNewDamage(Math.max(0,value));
        }
        // The stone shell empowers the owner's ordinary weapon hits, not every magic hit.
        if(event.getSource().getEntity() instanceof ServerPlayer owner
                && event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && PactService.isPact(owner) && PactCombatService.enemy(owner,event.getEntity())
                && has(owner,"taron")) {
            Battle state=battle(owner);
            if(state.stoneUntil>now(owner) && state.stoneIntegrity>0)
                event.setNewDamage((float)Math.min(10000,event.getNewDamage()*1.15));
        }
    }
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if(event.getEntity().level().isClientSide() || event.getNewDamage()<=0
                || event.getEntity().level().getServer()==null) return;
        if(event.getEntity() instanceof ServerPlayer victim && PactService.isPact(victim)) {
            Battle state=battle(victim);
            long tick=now(victim);
            if (event.getSource().getEntity() instanceof Mob attacker
                    && PactCombatService.enemy(victim,attacker)) {
                if(has(victim,"sevenwing")) {
                    state.verdictMarks.put(attacker.getUUID(),tick+140);
                    if(tick>=state.verdictGuardReady) {
                        state.verdictGuardReady=tick+320;
                        state.verdictGuardUntil=tick+60;
                    }
                }
                if(has(victim,"arken")) {
                    String detected=profile(event.getSource());
                    if(!detected.isBlank()) {
                        PactSavedData.get(victim.getServer()).observe(victim.getUUID(),detected);
                        state.lastObserved=detected;
                        state.recordAdaptUntil=tick+120;
                    }
                }
            }
            return;
        }
        if(!(event.getSource().getEntity() instanceof ServerPlayer owner)
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !PactService.isPact(owner)
                || !PactCombatService.enemy(owner,event.getEntity())) return;
        Battle state=battle(owner);
        long tick=now(owner);
        LivingEntity victim=event.getEntity();
        if(has(owner,"sevenwing")) state.verdictMarks.put(victim.getUUID(),tick+140);
        if(has(owner,"merzad")) {
            state.fireMarks.put(victim.getUUID(),tick+120);
            if(state.fireAfterglowUntil>tick && tick>=state.fireEchoReady) {
                state.fireEchoReady=tick+140;
                state.fireAfterglowUntil=0;
                if(victim instanceof Mob mob) hit(owner,PactCatalog.get("merzad"),mob,6);
            }
        }
        if(has(owner,"taron") && tick>=state.stoneStrikeReady && victim instanceof Mob mob) {
            state.stoneStrikeReady=tick+100;
            hit(owner,PactCatalog.get("taron"),mob,4);
        }
        if(has(owner,"arken") && state.focusUntil>tick && tick>=state.focusReady
                && victim instanceof Mob mob) {
            state.focusReady=tick+140;
            state.focusUntil=0;
            hit(owner,PactCatalog.get("arken"),mob,5);
        }
    }
    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server=event.getServer();
        if(activeServer!=server) {ACTIVE.clear();activeServer=server;}
        if(server.getTickCount()%20!=0) return;
        long tick=server.getTickCount();
        var rows=ACTIVE.entrySet().iterator();
        while(rows.hasNext()) {
            var row=rows.next();
            ServerPlayer player=server.getPlayerList().getPlayer(row.getKey());
            if(player==null || !PactService.isPact(player)) {rows.remove();continue;}
            Battle state=row.getValue();
            state.verdictMarks.entrySet().removeIf(e->e.getValue()<=tick);
            state.fireMarks.entrySet().removeIf(e->e.getValue()<=tick);
            if (state.stoneUntil<=tick) state.stoneIntegrity=0;
        }
    }
}
