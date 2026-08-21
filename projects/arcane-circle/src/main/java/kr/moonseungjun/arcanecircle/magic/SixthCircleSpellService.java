package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative deep runtime for the ten direct 6th-circle spells.
 *
 * Sixth circle is battlefield domination rather than a larger generic damage tier. Disintegrate
 * owns a narrow material-breaking ray, NPC Globes reproduce the same lower-circle interception
 * contract as player Globes, NPC Mass Suggestion maintains real retreat behavior, Move Earth is a
 * broad physical upheaval, Sunbeam is a wide piercing radiant line, NPC True Seeing repeatedly
 * reveals hidden life, Freezing Sphere is a fixed radial cryogenic burst, Eyebite maintains a long
 * fear/weakness retreat, NPC Flesh to Stone is a maintained casting-blocking petrification, and
 * Circle of Death preferentially executes weak ordinary enemies instead of acting as a flat blast.
 */
public final class SixthCircleSpellService {
    private static final Set<String> HANDLED = Set.of(
            "disintegrate", "globe_of_invulnerability", "mass_suggestion", "move_earth", "sunbeam",
            "true_seeing", "freezing_sphere", "eyebite", "flesh_to_stone", "circle_of_death");

    public static final int NPC_GLOBE_TICKS = HighWardSpellService.GLOBE_TICKS;
    public static final int NPC_SUGGESTION_TICKS = 160;
    public static final int NPC_TRUE_SEEING_TICKS = 1200;
    public static final int NPC_PETRIFY_TICKS = 360;
    public static final int EYEBITE_TICKS = 360;
    private static final int MAX_GLOBE_BLOCKED_CIRCLE = 5;

    private static final Map<UUID, NpcGlobe> NPC_GLOBES = new HashMap<>();
    private static final Map<UUID, RetreatState> NPC_SUGGESTIONS = new HashMap<>();
    private static final Map<UUID, TrueSightState> NPC_TRUE_SIGHT = new HashMap<>();
    private static final Map<UUID, FearState> EYEBITE_FEAR = new HashMap<>();
    private static final Map<UUID, PetrifyState> NPC_PETRIFY = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private SixthCircleSpellService() {}

    public static boolean handles(String spellId) { return HANDLED.contains(spellId); }

    public static boolean execute(ServerPlayer caster, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        ServerLevel level = (ServerLevel) caster.level();
        return switch (spellId) {
            case "disintegrate" -> disintegrate(level, caster, range, power, snapshot);
            case "globe_of_invulnerability" -> HighWardSpellService.execute(caster, spellId, range, power, snapshot);
            case "mass_suggestion" -> HighControlSpellService.execute(caster, spellId, range, power, snapshot);
            case "move_earth" -> moveEarth(level, caster, range, power, snapshot);
            case "sunbeam" -> sunbeam(level, caster, range, power, snapshot);
            case "true_seeing" -> SpellGameplayService.execute(caster, spellId, range, power, snapshot);
            case "freezing_sphere" -> freezingSphere(level, caster, range, power, snapshot.target());
            case "eyebite" -> eyebite(level, caster, snapshot.targetEntity(caster).orElse(null), power);
            case "flesh_to_stone" -> SpellGameplayService.execute(caster, spellId, range, power, snapshot);
            case "circle_of_death" -> circleOfDeath(level, caster, range, power, snapshot.target());
            default -> false;
        };
    }

    /** NPC mages receive the same spell roles before the generic resolver is allowed to run. */
    public static boolean executeNpc(ServerLevel level, Mob caster, LivingEntity designatedTarget,
                                     SpellDefinition spell, double range, double power,
                                     CastTargetSnapshot snapshot) {
        if (level == null || caster == null || spell == null || snapshot == null
                || !snapshot.validFor(caster) || !handles(spell.id())) return false;
        return switch (spell.id()) {
            case "disintegrate" -> disintegrate(level, caster, range, power, snapshot);
            case "globe_of_invulnerability" -> npcGlobe(level, caster, power);
            case "mass_suggestion" -> npcMassSuggestion(level, caster, range, snapshot.target());
            case "move_earth" -> moveEarth(level, caster, range, power, snapshot);
            case "sunbeam" -> sunbeam(level, caster, range, power, snapshot);
            case "true_seeing" -> npcTrueSeeing(level, caster, range);
            case "freezing_sphere" -> freezingSphere(level, caster, range, power, snapshot.target());
            case "eyebite" -> eyebite(level, caster, designatedTarget, power);
            case "flesh_to_stone" -> npcPetrify(level, caster, designatedTarget, power);
            case "circle_of_death" -> circleOfDeath(level, caster, range, power, snapshot.target());
            default -> false;
        };
    }

    /** Maintained suggestion, Eyebite fear and petrification prevent Arcane casting. */
    public static boolean blocksCasting(LivingEntity caster) {
        if (caster == null || !caster.isAlive() || !(caster.level() instanceof ServerLevel level)) return false;
        long now = level.getGameTime();
        RetreatState suggestion = NPC_SUGGESTIONS.get(caster.getUUID());
        if (suggestion != null && suggestion.level == level && suggestion.expiresAt > now) return true;
        FearState fear = EYEBITE_FEAR.get(caster.getUUID());
        if (fear != null && fear.level == level && fear.expiresAt > now) return true;
        PetrifyState stone = NPC_PETRIFY.get(caster.getUUID());
        return stone != null && stone.level == level && stone.expiresAt > now;
    }

    /** NPC-owned Globes erase hostile 1-5 circle spells crossing inward, matching player Globes. */
    public static boolean intercepts(LivingEntity caster, SpellDefinition spell,
                                     CastTargetSnapshot snapshot, double range) {
        if (caster == null || spell == null || snapshot == null || !snapshot.validFor(caster)
                || spell.circle() <= 0 || spell.circle() > MAX_GLOBE_BLOCKED_CIRCLE) return false;
        ServerLevel level = (ServerLevel) caster.level();
        long now = level.getGameTime();
        SpellPresentationProfile.MotionStyle motion = SpellPresentationProfile.profile(spell).motion();
        for (NpcGlobe globe : NPC_GLOBES.values()) {
            if (globe.level != level || globe.expiresAt <= now) continue;
            Entity rawOwner = level.getEntity(globe.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()) continue;
            if (owner == caster || owner.isAlliedTo(caster)) continue;
            Vec3 center = owner.position().add(0.0, owner.getBbHeight() * .50, 0.0);
            if (caster.position().distanceToSqr(center) <= globe.radius * globe.radius) continue;
            if (!crossesGlobe(spell, motion, snapshot, range, center, globe.radius)) continue;
            WorldMagicService.cancelRelease(caster, spell.id());
            level.playSound(null, BlockPos.containing(center), SoundEvents.AMETHYST_BLOCK_CHIME,
                    caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .88F, 1.72F);
            return true;
        }
        return false;
    }

    public static void tick(ServerLevel level) {
        if (level == null) return;
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        tickNpcGlobes(level, now);
        tickSuggestions(level, now);
        tickTrueSight(level, now);
        tickEyebiteFear(level, now);
        tickPetrify(level, now);
    }

    public static void clear(LivingEntity subject) { if (subject != null) clear(subject.getUUID()); }

    public static void clear(UUID id) {
        if (id == null) return;
        NPC_GLOBES.remove(id);
        NPC_TRUE_SIGHT.remove(id);
        Iterator<Map.Entry<UUID, RetreatState>> suggestions = NPC_SUGGESTIONS.entrySet().iterator();
        while (suggestions.hasNext()) {
            RetreatState state = suggestions.next().getValue();
            if (!state.ownerId.equals(id) && !state.targetId.equals(id)) continue;
            restoreSuggestion(state);
            suggestions.remove();
        }
        Iterator<Map.Entry<UUID, FearState>> fears = EYEBITE_FEAR.entrySet().iterator();
        while (fears.hasNext()) {
            FearState state = fears.next().getValue();
            if (!state.ownerId.equals(id) && !state.targetId.equals(id)) continue;
            restoreFear(state);
            fears.remove();
        }
        Iterator<Map.Entry<UUID, PetrifyState>> stones = NPC_PETRIFY.entrySet().iterator();
        while (stones.hasNext()) {
            PetrifyState state = stones.next().getValue();
            if (!state.ownerId.equals(id) && !state.targetId.equals(id)) continue;
            restorePetrify(state);
            stones.remove();
        }
    }

    public static void clearAll() {
        for (RetreatState state : NPC_SUGGESTIONS.values()) restoreSuggestion(state);
        for (FearState state : EYEBITE_FEAR.values()) restoreFear(state);
        for (PetrifyState state : NPC_PETRIFY.values()) restorePetrify(state);
        NPC_GLOBES.clear();
        NPC_SUGGESTIONS.clear();
        NPC_TRUE_SIGHT.clear();
        EYEBITE_FEAR.clear();
        NPC_PETRIFY.clear();
        LAST_TICK.clear();
    }

    private static boolean disintegrate(ServerLevel level, LivingEntity caster, double range, double power,
                                        CastTargetSnapshot snapshot) {
        Vec3 start = snapshot.launchOrigin();
        Vec3 end = snapshot.target();
        boolean hit = lineDamage(level, caster, start, end, .72, power * 1.25, target -> {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 100, 2, true, false));
        });
        if (caster instanceof ServerPlayer player) {
            DestructiveMagicService.ray(player, "disintegrate", start, end, power);
            ArcaneNoticeService.push(player, Component.literal(
                    "§a[분해] §f고정된 가느다란 분해광선 경로의 생명체와 물질을 함께 붕괴시킵니다."), 65);
        }
        level.playSound(null, BlockPos.containing(end), SoundEvents.AMETHYST_BLOCK_BREAK,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .88F, .58F);
        return hit || snapshot.validFor(caster);
    }

    private static boolean moveEarth(ServerLevel level, LivingEntity caster, double range, double power,
                                     CastTargetSnapshot snapshot) {
        Vec3 center = snapshot.target();
        double radius = Math.max(9.0, Math.min(15.0, SpellMetrics.effectRadius("move_earth", range, 6)));
        boolean hit = false;
        AABB box = new AABB(center, center).inflate(radius, Math.max(7.0, radius * .72), radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> enemy(caster, value) && center.distanceToSqr(value.position()) <= radius * radius)) {
            double distance = Math.sqrt(center.distanceToSqr(target.position()));
            double pressure = Math.max(.55, 1.0 - distance / Math.max(1.0, radius) * .45);
            if (ArcaneDamage.hurt(level, caster, target, (float) (power * .78 * pressure))) hit = true;
            Vec3 away = horizontal(target.position().subtract(center));
            target.push(away.x * (1.05 + .55 * pressure), .72 + .68 * pressure, away.z * (1.05 + .55 * pressure));
        }
        if (caster instanceof ServerPlayer player) {
            DestructiveMagicService.impact(player, "move_earth", center, radius, power);
            ArcaneNoticeService.push(player, Component.literal(
                    "§6[대지 이동] §f고정된 지면을 실제로 뒤엎고 주변 적을 바깥·위쪽으로 밀어 올립니다."), 72);
        }
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.05F, .52F);
        return hit || snapshot.validFor(caster);
    }

    private static boolean sunbeam(ServerLevel level, LivingEntity caster, double range, double power,
                                   CastTargetSnapshot snapshot) {
        Vec3 start = snapshot.launchOrigin();
        Vec3 end = snapshot.target();
        boolean hit = lineDamage(level, caster, start, end, 1.55, power, target -> {
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 160));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 180, 1, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, true, false));
        });
        level.playSound(null, BlockPos.containing(end), SoundEvents.BEACON_POWER_SELECT,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .82F, 1.48F);
        return hit || snapshot.validFor(caster);
    }

    private static boolean freezingSphere(ServerLevel level, LivingEntity caster, double range, double power, Vec3 center) {
        double radius = Math.max(7.5, Math.min(11.5, SpellMetrics.effectRadius("freezing_sphere", range, 6)));
        boolean hit = false;
        AABB box = new AABB(center, center).inflate(radius, Math.max(6.0, radius * .72), radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> enemy(caster, value) && center.distanceToSqr(value.position()) <= radius * radius)) {
            double distance = Math.sqrt(center.distanceToSqr(target.position()));
            double falloff = Math.max(.62, 1.0 - distance / Math.max(1.0, radius) * .38);
            if (ArcaneDamage.hurt(level, caster, target, (float) (power * falloff))) hit = true;
            target.setRemainingFireTicks(0);
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 520));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 220, 4, true, false));
            Vec3 away = horizontal(target.position().subtract(center));
            target.push(away.x * .36, .10, away.z * .36);
        }
        level.playSound(null, BlockPos.containing(center), SoundEvents.GLASS_BREAK,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.10F, .48F);
        return hit || center != null;
    }

    private static boolean eyebite(ServerLevel level, LivingEntity caster, LivingEntity target, double power) {
        if (!enemy(caster, target)) return false;
        ArcaneDamage.hurt(level, caster, target, (float) Math.max(1.0, power * .48));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EYEBITE_TICKS, 4, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, EYEBITE_TICKS, 2, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, EYEBITE_TICKS, 0, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, EYEBITE_TICKS, 3, true, false));
        if (target instanceof Mob mob) {
            FearState previous = EYEBITE_FEAR.remove(mob.getUUID());
            if (previous != null) restoreFear(previous);
            UUID oldTarget = mob.getTarget() == null ? null : mob.getTarget().getUUID();
            FearState state = new FearState(level, caster.getUUID(), mob.getUUID(), oldTarget,
                    level.getGameTime() + EYEBITE_TICKS);
            EYEBITE_FEAR.put(mob.getUUID(), state);
            applyFear(caster, mob, level.getGameTime());
        }
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal(
                    "§5[사악한 눈] §f18초 공포·쇠약을 새겼습니다. §7비플레이어 대상은 효과가 끝날 때까지 시전자에게서 강제로 도주합니다."), 78);
        }
        return true;
    }

    private static void tickEyebiteFear(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, FearState>> iterator = EYEBITE_FEAR.entrySet().iterator();
        while (iterator.hasNext()) {
            FearState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawTarget = level.getEntity(state.targetId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive()
                    || !(rawTarget instanceof Mob target) || !target.isAlive() || target.isRemoved()
                    || now >= state.expiresAt) {
                restoreFear(state);
                iterator.remove();
                continue;
            }
            applyFear(owner, target, now);
        }
    }

    private static void applyFear(LivingEntity owner, Mob target, long now) {
        target.setTarget(null);
        WorldMagicService.stop(target);
        if (now % 5L != 0L) return;
        Vec3 away = horizontal(target.position().subtract(owner.position()));
        Vec3 destination = target.position().add(away.scale(14.0));
        target.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.22);
    }

    private static void restoreFear(FearState state) {
        Entity raw = state.level.getEntity(state.targetId);
        if (!(raw instanceof Mob target) || !target.isAlive() || target.isRemoved()) return;
        target.getNavigation().stop();
        target.setTarget(resolveOldTarget(state.level, state.oldTargetId));
    }

    private static boolean circleOfDeath(ServerLevel level, LivingEntity caster, double range, double power, Vec3 center) {
        double radius = Math.max(10.0, Math.min(16.0, SpellMetrics.effectRadius("circle_of_death", range, 6)));
        boolean hit = false;
        int executions = 0;
        AABB box = new AABB(center, center).inflate(radius, Math.max(7.0, radius * .70), radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> enemy(caster, value) && center.distanceToSqr(value.position()) <= radius * radius)) {
            float effectivePool = target.getHealth() + target.getAbsorptionAmount();
            boolean ordinary = target.getMaxHealth() <= Math.max(80.0, power * 1.55)
                    && target.getBbWidth() <= 2.6F && target.getBbHeight() <= 4.2F;
            boolean weak = effectivePool <= Math.max(18.0, power * .92);
            float damage = ordinary && weak ? Math.max((float) (power * 1.35), effectivePool + 8.0F)
                    : (float) Math.max(1.0, power * .72);
            if (ArcaneDamage.hurt(level, caster, target, damage)) hit = true;
            if (ordinary && weak) executions++;
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2, true, false));
        }
        if (caster instanceof ServerPlayer player) {
            ArcaneNoticeService.push(player, Component.literal("§5[죽음의 원] §f대형 생명 파동 전개"
                    + (executions <= 0 ? "" : " · 약한 일반 적 처형 압박 " + executions + "체")), 75);
        }
        level.playSound(null, BlockPos.containing(center), SoundEvents.WITHER_SPAWN,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .70F, .62F);
        return hit || center != null;
    }

    private static boolean npcGlobe(ServerLevel level, Mob caster, double power) {
        double radius = Math.max(6.0, Math.min(8.0, 6.0 + Math.max(0.0, power - 52.0) / 90.0));
        NPC_GLOBES.put(caster.getUUID(), new NpcGlobe(level, caster.getUUID(), radius,
                level.getGameTime() + NPC_GLOBE_TICKS));
        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, .88F, 1.20F);
        return true;
    }

    private static void tickNpcGlobes(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcGlobe>> iterator = NPC_GLOBES.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcGlobe globe = iterator.next().getValue();
            if (globe.level != level) continue;
            Entity rawOwner = level.getEntity(globe.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved() || now >= globe.expiresAt) {
                iterator.remove();
            }
        }
    }

    private static boolean npcMassSuggestion(ServerLevel level, Mob caster, double range, Vec3 center) {
        double radius = Math.max(8.0, Math.min(14.0, range * .30));
        long expiresAt = level.getGameTime() + NPC_SUGGESTION_TICKS;
        int affected = 0;
        AABB box = new AABB(center, center).inflate(radius, Math.max(6.0, radius * .70), radius);
        for (Mob target : level.getEntitiesOfClass(Mob.class, box,
                value -> enemy(caster, value) && center.distanceToSqr(value.position()) <= radius * radius)) {
            RetreatState old = NPC_SUGGESTIONS.remove(target.getUUID());
            if (old != null) restoreSuggestion(old);
            UUID oldTarget = target.getTarget() == null ? null : target.getTarget().getUUID();
            Vec3 away = horizontal(target.position().subtract(center));
            Vec3 destination = target.position().add(away.scale(22.0));
            NPC_SUGGESTIONS.put(target.getUUID(), new RetreatState(level, caster.getUUID(), target.getUUID(),
                    oldTarget, destination, expiresAt));
            target.setTarget(null);
            target.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.20);
            WorldMagicService.stop(target);
            affected++;
        }
        return affected > 0;
    }

    private static void tickSuggestions(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, RetreatState>> iterator = NPC_SUGGESTIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            RetreatState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawTarget = level.getEntity(state.targetId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive()
                    || !(rawTarget instanceof Mob target) || !target.isAlive() || target.isRemoved()
                    || now >= state.expiresAt) {
                restoreSuggestion(state);
                iterator.remove();
                continue;
            }
            target.setTarget(null);
            WorldMagicService.stop(target);
            if (target.position().distanceToSqr(state.destination) > 9.0) {
                target.getNavigation().moveTo(state.destination.x, state.destination.y, state.destination.z, 1.20);
            }
        }
    }

    private static void restoreSuggestion(RetreatState state) {
        Entity raw = state.level.getEntity(state.targetId);
        if (!(raw instanceof Mob target) || !target.isAlive() || target.isRemoved()) return;
        target.getNavigation().stop();
        target.setTarget(resolveOldTarget(state.level, state.oldTargetId));
    }

    private static boolean npcTrueSeeing(ServerLevel level, Mob caster, double range) {
        NPC_TRUE_SIGHT.put(caster.getUUID(), new TrueSightState(level, caster.getUUID(),
                Math.max(20.0, Math.min(40.0, range)), level.getGameTime() + NPC_TRUE_SEEING_TICKS));
        caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, NPC_TRUE_SEEING_TICKS, 0, true, false));
        return true;
    }

    private static void tickTrueSight(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, TrueSightState>> iterator = NPC_TRUE_SIGHT.entrySet().iterator();
        while (iterator.hasNext()) {
            TrueSightState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved() || now >= state.expiresAt) {
                iterator.remove();
                continue;
            }
            if (now % 10L != 0L) continue;
            AABB box = owner.getBoundingBox().inflate(state.radius);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                    value -> value != owner && value.isAlive() && !value.isRemoved()
                            && !owner.isAlliedTo(value))) {
                target.removeEffect(MobEffects.INVISIBILITY);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 18, 0, true, false));
            }
        }
    }

    private static boolean npcPetrify(ServerLevel level, Mob caster, LivingEntity target, double power) {
        if (!enemy(caster, target)) return false;
        ArcaneDamage.hurt(level, caster, target, (float) Math.max(1.0, power * .62));
        PetrifyState old = NPC_PETRIFY.remove(target.getUUID());
        if (old != null) restorePetrify(old);
        UUID oldTarget = target instanceof Mob mob && mob.getTarget() != null ? mob.getTarget().getUUID() : null;
        NPC_PETRIFY.put(target.getUUID(), new PetrifyState(level, caster.getUUID(), target.getUUID(),
                oldTarget, level.getGameTime() + NPC_PETRIFY_TICKS));
        applyPetrify(target);
        return true;
    }

    private static void tickPetrify(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, PetrifyState>> iterator = NPC_PETRIFY.entrySet().iterator();
        while (iterator.hasNext()) {
            PetrifyState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawTarget = level.getEntity(state.targetId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive()
                    || !(rawTarget instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()
                    || now >= state.expiresAt) {
                restorePetrify(state);
                iterator.remove();
                continue;
            }
            applyPetrify(target);
        }
    }

    private static void applyPetrify(LivingEntity target) {
        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(0.0, Math.min(0.0, motion.y), 0.0);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 8, 255, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 8, 6, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 8, 2, true, false));
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            WorldMagicService.stop(mob);
        } else if (target instanceof ServerPlayer player) {
            if (!SpellCastingService.chargingSpell(player).isBlank()) SpellCastingService.cancelCharge(player, false);
            if (!SpellCastingService.pendingFusion(player).isEmpty()) SpellCastingService.clearFusion(player, false);
            SpellKineticsService.cancel(player);
        }
    }

    private static void restorePetrify(PetrifyState state) {
        Entity raw = state.level.getEntity(state.targetId);
        if (raw instanceof Mob mob && mob.isAlive() && !mob.isRemoved()) {
            mob.setTarget(resolveOldTarget(state.level, state.oldTargetId));
        }
    }

    private static boolean lineDamage(ServerLevel level, LivingEntity caster, Vec3 start, Vec3 end,
                                      double halfWidth, double power,
                                      java.util.function.Consumer<LivingEntity> afterHit) {
        Vec3 delta = end.subtract(start);
        double length = Math.max(.001, delta.length());
        Vec3 unit = delta.scale(1.0 / length);
        boolean hit = false;
        AABB box = new AABB(start, end).inflate(halfWidth + 1.2);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(caster, value))) {
            Vec3 relative = target.getEyePosition().subtract(start);
            double projection = relative.dot(unit);
            if (projection < 0.0 || projection > length) continue;
            double allowed = halfWidth + target.getBbWidth() * .50;
            if (relative.subtract(unit.scale(projection)).lengthSqr() > allowed * allowed) continue;
            if (ArcaneDamage.hurt(level, caster, target, (float) power)) hit = true;
            afterHit.accept(target);
        }
        return hit;
    }

    private static boolean crossesGlobe(SpellDefinition spell, SpellPresentationProfile.MotionStyle motion,
                                        CastTargetSnapshot snapshot, double range,
                                        Vec3 center, double radius) {
        Vec3 target = snapshot.target();
        double footprint = switch (motion) {
            case SKY_DROP, STORM, FIELD -> Math.min(24.0,
                    Math.max(1.5, SpellMetrics.effectRadius(spell.id(), range, spell.circle())));
            case WALL -> Math.min(20.0,
                    Math.max(2.0, SpellMetrics.wallWidth(spell.id(), range, spell.circle()) * .5));
            case WAVE -> Math.min(14.0,
                    Math.max(1.0, SpellMetrics.waveEndRadius(spell.id(), range, spell.circle())));
            case TARGET_BURST, PRISON -> 1.5;
            default -> .85;
        };
        return switch (motion) {
            case SKY_DROP, STORM, FIELD, WALL, TARGET_BURST, PRISON ->
                    target.distanceToSqr(center) <= square(radius + footprint);
            case DART, BOLT, HEAVY_ORB, MISSILE_SWARM, BEAM, LANCE, WAVE ->
                    segmentDistanceSqr(snapshot.launchOrigin(), target, center) <= square(radius + footprint);
            default -> target.distanceToSqr(center) <= square(radius + footprint);
        };
    }

    private static double segmentDistanceSqr(Vec3 start, Vec3 end, Vec3 point) {
        Vec3 delta = end.subtract(start);
        double lengthSqr = delta.lengthSqr();
        if (lengthSqr < 1.0E-8) return point.distanceToSqr(start);
        double t = point.subtract(start).dot(delta) / lengthSqr;
        t = Math.max(0.0, Math.min(1.0, t));
        return point.distanceToSqr(start.add(delta.scale(t)));
    }

    private static LivingEntity resolveOldTarget(ServerLevel level, UUID id) {
        if (id == null) return null;
        Entity raw = level.getEntity(id);
        return raw instanceof LivingEntity living && living.isAlive() && !living.isRemoved() ? living : null;
    }

    private static boolean enemy(LivingEntity owner, LivingEntity target) {
        return owner != null && target != null && target != owner && target.isAlive() && !target.isRemoved()
                && owner.level() == target.level() && !owner.isAlliedTo(target);
    }

    private static Vec3 horizontal(Vec3 value) {
        Vec3 flat = value == null ? Vec3.ZERO : new Vec3(value.x, 0.0, value.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    private static double square(double value) { return value * value; }

    private record NpcGlobe(ServerLevel level, UUID ownerId, double radius, long expiresAt) {}
    private record RetreatState(ServerLevel level, UUID ownerId, UUID targetId, UUID oldTargetId,
                                Vec3 destination, long expiresAt) {}
    private record TrueSightState(ServerLevel level, UUID ownerId, double radius, long expiresAt) {}
    private record FearState(ServerLevel level, UUID ownerId, UUID targetId, UUID oldTargetId,
                             long expiresAt) {}
    private record PetrifyState(ServerLevel level, UUID ownerId, UUID targetId, UUID oldTargetId,
                                long expiresAt) {}
}
