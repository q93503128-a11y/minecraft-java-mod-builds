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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative Grand Archmage runtime for the ten direct 6th-circle spells.
 *
 * Sixth circle owns strong professional authority rather than inflated lower-circle damage:
 * Disintegrate is precision material deletion, Globe rejects hostile 1-5C magic, Mass Suggestion
 * imposes a long group retreat order, Move Earth performs deliberate battlefield engineering,
 * Sunbeam holds a piercing solar corridor, True Seeing breaks concealment, Freezing Sphere locks
 * an area into cryogenic denial, Eyebite forces sustained retreat, Flesh to Stone petrifies, and
 * Circle of Death is routed only through the canonical non-execution death doctrine.
 */
public final class SixthCircleSpellService {
    private static final Set<String> HANDLED = Set.of(
            "disintegrate", "globe_of_invulnerability", "mass_suggestion", "move_earth", "sunbeam",
            "true_seeing", "freezing_sphere", "eyebite", "flesh_to_stone", "circle_of_death");

    public static final int NPC_GLOBE_TICKS = HighWardSpellService.GLOBE_TICKS;
    public static final int NPC_SUGGESTION_TICKS = HighControlSpellService.MASS_SUGGESTION_TICKS;
    public static final int NPC_TRUE_SEEING_TICKS = 1200;
    public static final int NPC_PETRIFY_TICKS = 360;
    public static final int EYEBITE_TICKS = 360;
    public static final int SUNBEAM_TICKS = 120;
    public static final int FREEZING_SPHERE_TICKS = 200;
    private static final int SUNBEAM_PULSE_TICKS = 10;
    private static final int FREEZING_PULSE_TICKS = 10;
    private static final int MAX_GLOBE_BLOCKED_CIRCLE = 5;

    private static final Map<UUID, NpcGlobe> NPC_GLOBES = new HashMap<>();
    private static final Map<UUID, RetreatState> NPC_SUGGESTIONS = new HashMap<>();
    private static final Map<UUID, TrueSightState> NPC_TRUE_SIGHT = new HashMap<>();
    private static final Map<UUID, FearState> EYEBITE_FEAR = new HashMap<>();
    private static final Map<UUID, PetrifyState> NPC_PETRIFY = new HashMap<>();
    private static final List<SunbeamField> SUNBEAMS = new ArrayList<>();
    private static final List<FreezingField> FREEZING_FIELDS = new ArrayList<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private SixthCircleSpellService() {}

    public static boolean handles(String spellId) { return HANDLED.contains(spellId); }

    public static double massSuggestionRadius(double range) {
        return Math.max(8.0, Math.min(14.0, range * .30));
    }

    public static double moveEarthLength(double range) { return MoveEarthService.length(range); }
    public static double moveEarthTrenchHalfWidth(double range) { return MoveEarthService.trenchHalfWidth(range); }
    public static double moveEarthBermOffset(double range) { return MoveEarthService.bermOffset(range); }
    public static double sunbeamHalfWidth() { return 1.55; }

    public static double freezingSphereRadius(double range) {
        return Math.max(10.5, Math.min(15.5, Math.max(0.0, range) * .28));
    }

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
            case "circle_of_death" -> DeathDoctrineService.execute(caster, spellId, range, power, snapshot);
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
            case "circle_of_death" -> DeathDoctrineService.executeNpc(level, caster, designatedTarget,
                    spell.id(), range, power, snapshot);
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
        tickSunbeams(level, now);
        tickFreezingFields(level, now);
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
        clearSunbeams(id);
        clearFreezingFields(id);
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
        SUNBEAMS.clear();
        FREEZING_FIELDS.clear();
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

    /**
     * Grand-Archmage earthwork: a long engineered corridor, not a radial earthquake. Player casts
     * physically relocate surface material into a trench and two berms; NPC casts preserve the same
     * battlefield split/knockback authority without editing shared terrain.
     */
    private static boolean moveEarth(ServerLevel level, LivingEntity caster, double range, double power,
                                     CastTargetSnapshot snapshot) {
        Vec3 center = snapshot.target();
        Vec3 forward = horizontal(snapshot.launchDirection());
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        double length = moveEarthLength(range);
        double controlHalfWidth = moveEarthBermOffset(range) + 1.8;
        AABB box = new AABB(center, center).inflate(length * .58, 8.0, length * .58);
        boolean hit = false;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, value -> enemy(caster, value))) {
            Vec3 relative = target.position().subtract(center);
            double along = relative.dot(forward);
            double lateral = relative.dot(right);
            if (Math.abs(along) > length * .52 || Math.abs(lateral) > controlHalfWidth) continue;
            double pressure = Math.max(.58, 1.0 - Math.abs(lateral) / Math.max(1.0, controlHalfWidth) * .42);
            if (ArcaneDamage.hurt(level, caster, target, (float) (power * .44 * pressure))) hit = true;
            double side = lateral < 0.0 ? -1.0 : 1.0;
            target.push(right.x * side * (1.05 + .42 * pressure), .36 + .30 * pressure,
                    right.z * side * (1.05 + .42 * pressure));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 1, true, false));
        }
        if (caster instanceof ServerPlayer player) {
            int moved = MoveEarthService.execute(player, center, forward, range);
            ArcaneNoticeService.push(player, Component.literal(
                    "§6[대지 이동] §f약 " + Math.round(length) + "m 전선을 가로질러 중앙 참호와 양측 토루를 실제 토사 이동으로 조성했습니다."
                            + (moved > 0 ? " §7이동 블록 " + moved + "개" : "")), 86);
        }
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.05F, .52F);
        return hit || snapshot.validFor(caster);
    }

    /** Six-second locked solar corridor; enemies can leave the line, but cannot ignore it while inside. */
    private static boolean sunbeam(ServerLevel level, LivingEntity caster, double range, double power,
                                   CastTargetSnapshot snapshot) {
        clearSunbeams(caster.getUUID());
        Vec3 start = snapshot.launchOrigin();
        Vec3 end = snapshot.target();
        SunbeamField field = new SunbeamField(level, caster.getUUID(), start, end, power,
                level.getGameTime() + SUNBEAM_TICKS, level.getGameTime());
        SUNBEAMS.add(field);
        pulseSunbeam(field, caster, power * .30);
        level.playSound(null, BlockPos.containing(end), SoundEvents.BEACON_POWER_SELECT,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .94F, 1.48F);
        return true;
    }

    private static void tickSunbeams(ServerLevel level, long now) {
        Iterator<SunbeamField> iterator = SUNBEAMS.iterator();
        while (iterator.hasNext()) {
            SunbeamField field = iterator.next();
            if (field.level != level) continue;
            Entity rawOwner = level.getEntity(field.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved() || now >= field.expiresAt) {
                if (rawOwner instanceof LivingEntity living) WorldMagicService.cancelRelease(living, "sunbeam");
                iterator.remove();
                continue;
            }
            if (now < field.nextPulse) continue;
            field.nextPulse = now + SUNBEAM_PULSE_TICKS;
            pulseSunbeam(field, owner, field.power * .16);
        }
    }

    private static void pulseSunbeam(SunbeamField field, LivingEntity caster, double pulsePower) {
        lineDamage(field.level, caster, field.start, field.end, sunbeamHalfWidth(), pulsePower, "sunbeam", target -> {
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 80));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 1, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false));
        });
    }

    private static void clearSunbeams(UUID ownerId) {
        Iterator<SunbeamField> iterator = SUNBEAMS.iterator();
        while (iterator.hasNext()) {
            SunbeamField field = iterator.next();
            if (!field.ownerId.equals(ownerId)) continue;
            Entity raw = field.level.getEntity(ownerId);
            if (raw instanceof LivingEntity owner) WorldMagicService.cancelRelease(owner, "sunbeam");
            iterator.remove();
        }
    }

    /** Ten-second cryogenic denial field, distinct from Cone of Cold or the one-shot Ice Storm. */
    private static boolean freezingSphere(ServerLevel level, LivingEntity caster, double range, double power, Vec3 center) {
        clearFreezingFields(caster.getUUID());
        double radius = freezingSphereRadius(range);
        FreezingField field = new FreezingField(level, caster.getUUID(), center, radius, power,
                level.getGameTime() + FREEZING_SPHERE_TICKS, level.getGameTime() + FREEZING_PULSE_TICKS);
        FREEZING_FIELDS.add(field);
        pulseFreezing(field, caster, power * .62, true);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GLASS_BREAK,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.14F, .46F);
        return true;
    }

    private static void tickFreezingFields(ServerLevel level, long now) {
        Iterator<FreezingField> iterator = FREEZING_FIELDS.iterator();
        while (iterator.hasNext()) {
            FreezingField field = iterator.next();
            if (field.level != level) continue;
            Entity rawOwner = level.getEntity(field.ownerId);
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved() || now >= field.expiresAt) {
                if (rawOwner instanceof LivingEntity living) WorldMagicService.cancelRelease(living, "freezing_sphere");
                iterator.remove();
                continue;
            }
            if (now < field.nextPulse) continue;
            field.nextPulse = now + FREEZING_PULSE_TICKS;
            pulseFreezing(field, owner, field.power * .075, false);
        }
    }

    private static void pulseFreezing(FreezingField field, LivingEntity caster, double pulsePower, boolean initial) {
        AABB box = new AABB(field.center, field.center).inflate(field.radius, Math.max(7.0, field.radius * .72), field.radius);
        for (LivingEntity target : field.level.getEntitiesOfClass(LivingEntity.class, box,
                value -> enemy(caster, value) && field.center.distanceToSqr(value.position()) <= field.radius * field.radius)) {
            double distance = Math.sqrt(field.center.distanceToSqr(target.position()));
            double falloff = Math.max(.62, 1.0 - distance / Math.max(1.0, field.radius) * .38);
            ArcaneDamage.hurtAttributed(field.level, caster, target, (float) Math.max(.5, pulsePower * falloff), "freezing_sphere");
            target.setRemainingFireTicks(0);
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + (initial ? 520 : 120)));
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 18, 5, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 18, 3, true, false));
        }
    }

    private static void clearFreezingFields(UUID ownerId) {
        Iterator<FreezingField> iterator = FREEZING_FIELDS.iterator();
        while (iterator.hasNext()) {
            FreezingField field = iterator.next();
            if (!field.ownerId.equals(ownerId)) continue;
            Entity raw = field.level.getEntity(ownerId);
            if (raw instanceof LivingEntity owner) WorldMagicService.cancelRelease(owner, "freezing_sphere");
            iterator.remove();
        }
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
        double radius = massSuggestionRadius(range);
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
        return lineDamage(level, caster, start, end, halfWidth, power, null, afterHit);
    }

    private static boolean lineDamage(ServerLevel level, LivingEntity caster, Vec3 start, Vec3 end,
                                      double halfWidth, double power, String spellId,
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
            boolean damaged = spellId == null
                    ? ArcaneDamage.hurt(level, caster, target, (float) power)
                    : ArcaneDamage.hurtAttributed(level, caster, target, (float) power, spellId);
            if (damaged) hit = true;
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

    private static final class SunbeamField {
        private final ServerLevel level; private final UUID ownerId; private final Vec3 start, end; private final double power;
        private final long expiresAt; private long nextPulse;
        private SunbeamField(ServerLevel level, UUID ownerId, Vec3 start, Vec3 end, double power,
                             long expiresAt, long nextPulse) {
            this.level=level; this.ownerId=ownerId; this.start=start; this.end=end; this.power=power;
            this.expiresAt=expiresAt; this.nextPulse=nextPulse;
        }
    }

    private static final class FreezingField {
        private final ServerLevel level; private final UUID ownerId; private final Vec3 center;
        private final double radius, power; private final long expiresAt; private long nextPulse;
        private FreezingField(ServerLevel level, UUID ownerId, Vec3 center, double radius, double power,
                              long expiresAt, long nextPulse) {
            this.level=level; this.ownerId=ownerId; this.center=center; this.radius=radius; this.power=power;
            this.expiresAt=expiresAt; this.nextPulse=nextPulse;
        }
    }
}
