package kr.moonseungjun.anomalousclasses.state;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Mental Dominator direct skills. All costs, targeting, ownership, timers, and
 * damage execute on the logical server. Status and modifiers never duplicate mobs.
 */
public final class MentalDominatorCombatService {
    private static final String STUN_TAG = "anomalousclasses_mental_stun";
    private static final ResourceLocation ATTACK_BOOST = ResourceLocation.fromNamespaceAndPath(
            AnomalousClasses.MOD_ID, "mental_overload_attack");
    private static final ResourceLocation SPEED_BOOST = ResourceLocation.fromNamespaceAndPath(
            AnomalousClasses.MOD_ID, "mental_overload_speed");

    public static final int SHOCK = 0, SUGGESTION = 1, OVERLOAD = 2, CONTAINMENT = 3;
    private enum Spell {
        SHOCK, SUGGESTION, OVERLOAD, CONTAINMENT;
        static Spell of(int value) { return value >= 0 && value < values().length ? values()[value] : null; }
    }
    private record Stun(Mob mob, long expires) {}
    private record Overload(Mob mob, long expires) {}
    private record Echo(UUID target, long expires) {}

    private static final Map<UUID, EnumMap<Spell, Long>> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Stun> STUNS = new HashMap<>();
    private static final Map<UUID, Overload> OVERLOADS = new HashMap<>();
    private static final Map<UUID, Echo> ECHOES = new HashMap<>();
    private static MinecraftServer activeServer;
    private static long now;

    private MentalDominatorCombatService() {}

    public static void use(ServerPlayer player, int ability) {
        if (player.getServer() == null || !player.isAlive() || !MentalDominatorService.isMental(player)) return;
        Spell spell = Spell.of(ability);
        if (spell == null) return;
        if (MentalDominatorService.isChanneling(player)) {
            tell(player, "message.anomalousclasses.mental_spell.channeling");
            return;
        }
        long ready = COOLDOWNS.getOrDefault(player.getUUID(), new EnumMap<>(Spell.class))
                .getOrDefault(spell, 0L);
        if (now < ready) {
            player.displayClientMessage(Component.translatable("message.anomalousclasses.mental_spell.cooldown",
                    (ready - now + 19) / 20), true);
            return;
        }

        MentalDominatorConfig cfg = MentalDominatorConfig.VALUES;
        double cost = switch (spell) {
            case SHOCK -> MentalDominatorConfig.value(cfg.shockCost);
            case SUGGESTION -> MentalDominatorConfig.value(cfg.suggestionCost);
            case OVERLOAD -> MentalDominatorConfig.value(cfg.overloadCost);
            case CONTAINMENT -> MentalDominatorConfig.value(cfg.containmentCost);
        };
        if (!MentalDominatorService.hasPsyche(player, cost)) {
            tell(player, "message.anomalousclasses.mental.insufficient_psyche");
            return;
        }

        Mob aimed = spell == Spell.CONTAINMENT ? null : MentalDominatorService.findAimedMob(player);
        if (spell != Spell.CONTAINMENT && (aimed == null || !aimed.isAlive())) {
            tell(player, "message.anomalousclasses.mental.no_target");
            return;
        }
        MentalDominatorSavedData data = MentalDominatorSavedData.get(player.getServer());
        if (spell == Spell.OVERLOAD) {
            MentalDominatorSavedData.ControlledRecord owned = data.controlled(aimed.getUUID());
            if (owned == null || !owned.ownerId().equals(player.getUUID())
                    || owned.linkState() != MentalDominatorSavedData.LinkState.ACTIVE
                    || OVERLOADS.containsKey(aimed.getUUID())) {
                tell(player, "message.anomalousclasses.mental_spell.need_controlled");
                return;
            }
        } else if (aimed != null && !MentalDominatorService.isValidCombatTarget(player, aimed)) {
            tell(player, "message.anomalousclasses.mental_spell.invalid_target");
            return;
        }

        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        List<Mob> affected = List.of();
        if (spell == Spell.CONTAINMENT) {
            double radius = MentalDominatorConfig.value(cfg.containmentRadius)
                    + 0.12 * MentalDominatorRules.passiveRanks(profile, "containment_expansion");
            affected = player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius),
                    target -> target.isAlive() && !target.isNoAi()
                            && target.distanceToSqr(player) <= radius * radius
                            && MentalDominatorService.isValidCombatTarget(player, target));
            if (affected.isEmpty()) {
                tell(player, "message.anomalousclasses.mental_spell.no_targets");
                return;
            }
        }
        // All invalid and empty-target requests return before the server spends anything.
        if (!MentalDominatorService.spendPsyche(player, cost)) {
            tell(player, "message.anomalousclasses.mental.insufficient_psyche");
            return;
        }
        int level = profile.progress(ClassId.MENTAL_DOMINATOR).level();
        int cooldown = switch (spell) {
            case SHOCK -> MentalDominatorConfig.value(cfg.shockCooldown);
            case SUGGESTION -> MentalDominatorConfig.value(cfg.suggestionCooldown);
            case OVERLOAD -> MentalDominatorConfig.value(cfg.overloadCooldown);
            case CONTAINMENT -> MentalDominatorConfig.value(cfg.containmentCooldown);
        };
        COOLDOWNS.computeIfAbsent(player.getUUID(), ignored -> new EnumMap<>(Spell.class))
                .put(spell, now + cooldown);
        ServerLevel world = player.serverLevel();
        switch (spell) {
            case SHOCK -> {
                float damage = (float) Math.min(150.0, MentalDominatorConfig.value(cfg.shockDamage)
                        * (1.0 + 0.035 * (Math.max(1, level) - 1)));
                aimed.hurt(player.damageSources().playerAttack(player), damage);
                stun(aimed, 12);
                ECHOES.put(player.getUUID(), new Echo(aimed.getUUID(), now + 80));
                particles(world, aimed, ParticleTypes.ENCHANTED_HIT, 22);
                tell(player, "message.anomalousclasses.mental_spell.shock");
            }
            case SUGGESTION -> {
                double attack = MentalDominatorRules.threat(aimed).attackDamage();
                float base = (float) Math.max(3.0, Math.min(200.0, attack * 1.5 + 3.0));
                float damage = (float) (base * (1.0 + 0.01 * Math.min(25,
                        MentalDominatorRules.passiveRanks(profile, "suggestion_depth"))));
                aimed.hurt(aimed.damageSources().mobAttack(aimed), damage);
                particles(world, aimed, ParticleTypes.WITCH, 32);
                tell(player, "message.anomalousclasses.mental_spell.suggestion");
            }
            case OVERLOAD -> {
                int ticks = MentalDominatorConfig.value(cfg.overloadDuration)
                        + (int) Math.round(2.4 * MentalDominatorRules.passiveRanks(profile, "overload_duration"));
                overload(aimed, ticks);
                particles(world, aimed, ParticleTypes.END_ROD, 38);
                tell(player, "message.anomalousclasses.mental_spell.overload");
            }
            case CONTAINMENT -> {
                for (Mob mob : affected) {
                    double threat = MentalDominatorRules.threat(mob).score();
                    int ticks = Math.max(6, (int) Math.round(
                            MentalDominatorConfig.value(cfg.containmentDuration)
                                    / (1.0 + Math.sqrt(threat) / 12.0)));
                    stun(mob, ticks);
                    particles(world, mob, ParticleTypes.PORTAL, 9);
                }
                world.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0,
                        player.getZ(), 60, 2.3, 0.8, 2.3, 0.1);
                tell(player, "message.anomalousclasses.mental_spell.containment");
            }
        }
        world.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0F, spell == Spell.CONTAINMENT ? 0.65F : 1.2F);
    }

    public static double echoMultiplier(ServerPlayer owner, Mob target, PlayerClassProfile profile) {
        Echo echo = ECHOES.get(owner.getUUID());
        if (echo == null || echo.expires() <= now || !echo.target().equals(target.getUUID())) return 1.0;
        return 1.0 + 0.008 * Math.min(20, MentalDominatorRules.passiveRanks(profile, "shock_echo"));
    }

    public static double upkeepMultiplier(UUID entityId) {
        Overload effect = OVERLOADS.get(entityId);
        return effect != null && effect.expires() > now ? 1.5 : 1.0;
    }

    public static boolean isStunned(UUID entityId) { return STUNS.containsKey(entityId); }

    public static void tick(MinecraftServer server, long tick) {
        if (activeServer != server) {
            COOLDOWNS.clear();
            ECHOES.clear();
            STUNS.clear();
            OVERLOADS.clear();
            activeServer = server;
        }
        now = tick;
        Iterator<Map.Entry<UUID, Stun>> stunIt = STUNS.entrySet().iterator();
        while (stunIt.hasNext()) {
            Stun status = stunIt.next().getValue();
            if (status.expires() <= tick || !status.mob().isAlive() || status.mob().isRemoved()) {
                clearStun(status.mob());
                stunIt.remove();
            }
        }
        MentalDominatorSavedData data = MentalDominatorSavedData.get(server);
        Iterator<Map.Entry<UUID, Overload>> buffIt = OVERLOADS.entrySet().iterator();
        while (buffIt.hasNext()) {
            Map.Entry<UUID, Overload> entry = buffIt.next();
            Overload status = entry.getValue();
            MentalDominatorSavedData.ControlledRecord record = data.controlled(entry.getKey());
            if (status.expires() <= tick || !status.mob().isAlive() || status.mob().isRemoved()
                    || record == null || record.linkState() != MentalDominatorSavedData.LinkState.ACTIVE) {
                clearOverload(status.mob());
                buffIt.remove();
            }
        }
        ECHOES.entrySet().removeIf(entry -> entry.getValue().expires() <= tick);
        if (tick % 1200L == 0L) {
            COOLDOWNS.entrySet().removeIf(entry ->
                    entry.getValue().values().stream().allMatch(expiry -> expiry <= tick));
        }
    }

    public static void onMobJoin(Mob mob) {
        // Recover after crash/restart if a transient stun was serialized.
        if (mob.getPersistentData().contains(STUN_TAG)) clearStun(mob);
        STUNS.remove(mob.getUUID());
        OVERLOADS.remove(mob.getUUID());
    }

    public static void onMobLeave(Mob mob) {
        if (STUNS.remove(mob.getUUID()) != null) clearStun(mob);
        if (OVERLOADS.remove(mob.getUUID()) != null) clearOverload(mob);
    }

    private static void stun(Mob mob, int duration) {
        if (mob.isNoAi() && !mob.getPersistentData().contains(STUN_TAG)) return;
        if (!mob.getPersistentData().contains(STUN_TAG)) {
            CompoundTag state = new CompoundTag();
            state.putBoolean("wasNoAi", mob.isNoAi());
            mob.getPersistentData().put(STUN_TAG, state);
        }
        mob.setNoAi(true);
        mob.getNavigation().stop();
        Stun previous = STUNS.get(mob.getUUID());
        STUNS.put(mob.getUUID(), new Stun(mob,
                Math.max(now + duration, previous == null ? 0 : previous.expires())));
    }

    private static void clearStun(Mob mob) {
        if (!mob.getPersistentData().contains(STUN_TAG)) return;
        CompoundTag original = mob.getPersistentData().getCompound(STUN_TAG);
        mob.setNoAi(original.getBoolean("wasNoAi"));
        mob.getPersistentData().remove(STUN_TAG);
    }

    private static void overload(Mob mob, int duration) {
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attack != null) {
            attack.removeModifier(ATTACK_BOOST);
            attack.addTransientModifier(new AttributeModifier(ATTACK_BOOST, 0.30,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (speed != null) {
            speed.removeModifier(SPEED_BOOST);
            speed.addTransientModifier(new AttributeModifier(SPEED_BOOST, 0.15,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        OVERLOADS.put(mob.getUUID(), new Overload(mob, now + duration));
    }

    private static void clearOverload(Mob mob) {
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attack != null) attack.removeModifier(ATTACK_BOOST);
        if (speed != null) speed.removeModifier(SPEED_BOOST);
    }

    private static void particles(ServerLevel world, Mob mob, ParticleOptions type, int count) {
        world.sendParticles(type, mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(),
                count, mob.getBbWidth() * 0.3, mob.getBbHeight() * 0.3,
                mob.getBbWidth() * 0.3, 0.1);
    }

    private static void tell(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
