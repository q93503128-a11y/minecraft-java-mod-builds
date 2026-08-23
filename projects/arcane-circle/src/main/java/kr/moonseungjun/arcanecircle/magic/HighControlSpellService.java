package kr.moonseungjun.arcanecircle.magic;

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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** High-circle behavioral, spatial and casting control runtime. */
public final class HighControlSpellService {
    private static final Set<String> HANDLED = Set.of(
            "mass_suggestion", "forcecage", "dominate_monster", "feeblemind");
    private static final int MASS_SUGGESTION_TICKS = 160;
    private static final int FORCECAGE_TICKS = 400;
    private static final int DOMINATE_TICKS = 1200;
    private static final int FEEBLEMIND_TICKS = 1800;
    private static final double FORCECAGE_RADIUS = 3.1;
    private static final double FORCECAGE_DOWN = .75;
    private static final double FORCECAGE_UP = 4.2;

    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private HighControlSpellService() {}
    public static boolean handles(String spellId) { return HANDLED.contains(spellId); }

    public static boolean execute(ServerPlayer caster, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        return switch (spellId) {
            case "mass_suggestion" -> massSuggestion(caster, range, snapshot);
            case "forcecage" -> forcecage(caster, snapshot);
            case "dominate_monster" -> dominateMonster(caster, snapshot);
            case "feeblemind" -> feeblemind(caster, power, snapshot);
            default -> false;
        };
    }

    public static boolean blocksCasting(LivingEntity caster) {
        if (caster == null || !caster.isAlive()) return false;
        State state = ACTIVE.get(caster.getUUID());
        if (state == null || !state.active() || state.level != caster.level()) return false;
        return !"forcecage".equals(state.spellId);
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        Iterator<Map.Entry<UUID, State>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            State state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawTarget = level.getEntity(state.targetId);
            Entity rawOwner = level.getEntity(state.ownerId);
            if (!(rawTarget instanceof Mob target) || !target.isAlive() || target.isRemoved()
                    || !(rawOwner instanceof ServerPlayer owner) || !owner.isAlive() || owner.isSpectator()
                    || now >= state.expiresAt) {
                restore(state);
                iterator.remove();
                continue;
            }
            switch (state.spellId) {
                case "mass_suggestion" -> applySuggestion(target, state);
                case "forcecage" -> applyForcecage(target, state);
                case "dominate_monster" -> applyDomination(owner, target, state);
                case "feeblemind" -> applyFeeblemind(target);
                default -> { }
            }
        }
    }

    /** Unified high-control cleanup also closes maintained ninth-circle state for Dispel/Antimagic. */
    public static void clear(LivingEntity subject) {
        if (subject == null) return;
        UUID id = subject.getUUID();
        Iterator<Map.Entry<UUID, State>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            State state = iterator.next().getValue();
            if (!state.targetId.equals(id) && !state.ownerId.equals(id)) continue;
            restore(state);
            iterator.remove();
        }
        NinthCircleSpellService.clear(subject);
    }

    public static void clearAll() {
        for (State state : ACTIVE.values()) restore(state);
        ACTIVE.clear();
        LAST_TICK.clear();
    }

    private static boolean massSuggestion(ServerPlayer caster, double range, CastTargetSnapshot snapshot) {
        ServerLevel level = (ServerLevel) caster.level();
        Vec3 center = snapshot.target();
        double radius = Math.max(8.0, Math.min(14.0, range * .30));
        long expiresAt = level.getGameTime() + MASS_SUGGESTION_TICKS;
        int affected = 0;
        for (Mob target : level.getEntitiesOfClass(Mob.class,
                new AABB(center, center).inflate(radius, Math.max(6.0, radius * .7), radius),
                mob -> mob.isAlive() && !mob.isRemoved() && !caster.isAlliedTo(mob)
                        && center.distanceToSqr(mob.position()) <= radius * radius)) {
            UUID oldTarget = target.getTarget() == null ? null : target.getTarget().getUUID();
            Vec3 destination = retreatDestination(caster, target, center);
            replace(new State(level, caster.getUUID(), target.getUUID(), "mass_suggestion",
                    expiresAt, destination, oldTarget));
            target.setTarget(null);
            target.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.20);
            WorldMagicService.stop(target);
            affected++;
        }
        if (affected <= 0) return false;
        ArcaneNoticeService.push(caster, Component.literal("§d[대규모 제안] §f" + affected
                + "체에게 전투에서 물러나라는 정신 명령을 내렸습니다. §7약 8초 동안 공격을 끊고 전장을 이탈합니다."), 90);
        level.playSound(null, caster.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 1.0F, .92F);
        return true;
    }

    private static boolean forcecage(ServerPlayer caster, CastTargetSnapshot snapshot) {
        Mob target = targetMob(caster, snapshot);
        if (target == null) {
            ArcaneNoticeService.push(caster, Component.literal("§c[역장 감옥] §f가둘 생명체를 정확히 조준해야 합니다."), 60);
            return false;
        }
        ServerLevel level = (ServerLevel) caster.level();
        UUID oldTarget = target.getTarget() == null ? null : target.getTarget().getUUID();
        replace(new State(level, caster.getUUID(), target.getUUID(), "forcecage",
                level.getGameTime() + FORCECAGE_TICKS, target.position(), oldTarget));
        ArcaneNoticeService.push(caster, Component.literal("§5[역장 감옥] §f" + target.getName().getString()
                + "을 고정된 다층 역장 안에 가뒀습니다. §7대상은 행동·공격·시전할 수 있지만 약 20초 동안 감옥 밖으로 나갈 수 없습니다."), 95);
        level.playSound(null, target.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 1.0F, .82F);
        return true;
    }

    private static boolean dominateMonster(ServerPlayer caster, CastTargetSnapshot snapshot) {
        Mob target = targetMob(caster, snapshot);
        if (target == null) {
            ArcaneNoticeService.push(caster, Component.literal("§c[괴물 지배] §f지배할 비플레이어 생명체를 조준해야 합니다."), 60);
            return false;
        }
        ServerLevel level = (ServerLevel) caster.level();
        UUID oldTarget = target.getTarget() == null ? null : target.getTarget().getUUID();
        replace(new State(level, caster.getUUID(), target.getUUID(), "dominate_monster",
                level.getGameTime() + DOMINATE_TICKS, caster.position(), oldTarget));
        target.setTarget(null);
        target.getNavigation().stop();
        WorldMagicService.stop(target);
        ArcaneNoticeService.push(caster, Component.literal("§5[괴물 지배] §f" + target.getName().getString()
                + "의 전투 진영을 60초 동안 탈취했습니다. §7시전자를 공격할 수 없고 주변 위협과 싸우며, 전투가 없으면 시전자를 추종합니다."), 110);
        level.playSound(null, target.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 1.05F, .62F);
        return true;
    }

    private static boolean feeblemind(ServerPlayer caster, double power, CastTargetSnapshot snapshot) {
        Mob target = targetMob(caster, snapshot);
        if (target == null) {
            ArcaneNoticeService.push(caster, Component.literal("§c[정신 붕괴] §f정신 회로를 붕괴시킬 생명체를 조준해야 합니다."), 60);
            return false;
        }
        ServerLevel level = (ServerLevel) caster.level();
        UUID oldTarget = target.getTarget() == null ? null : target.getTarget().getUUID();
        replace(new State(level, caster.getUUID(), target.getUUID(), "feeblemind",
                level.getGameTime() + FEEBLEMIND_TICKS, target.position(), oldTarget));
        ArcaneDamage.hurt(level, caster, target, (float) Math.max(1.0, power * .65));
        WorldMagicService.stop(target);
        applyFeeblemind(target);
        ArcaneNoticeService.push(caster, Component.literal("§5[정신 붕괴] §f" + target.getName().getString()
                + "의 사고·마법 회로를 90초 동안 붕괴시켰습니다. §7Arcane 시전은 완전히 봉쇄되고 공격력·행동속도·시야가 크게 무너집니다."), 115);
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, .95F, .48F);
        return true;
    }

    private static void applySuggestion(Mob target, State state) {
        target.setTarget(null);
        if (target.position().distanceToSqr(state.anchor) > 9.0)
            target.getNavigation().moveTo(state.anchor.x, state.anchor.y, state.anchor.z, 1.20);
    }

    private static void applyForcecage(Mob target, State state) {
        Vec3 at = target.position();
        double dx = at.x - state.anchor.x;
        double dz = at.z - state.anchor.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double y = Math.max(state.anchor.y - FORCECAGE_DOWN, Math.min(state.anchor.y + FORCECAGE_UP, at.y));
        if (horizontal <= FORCECAGE_RADIUS && y == at.y) return;
        double x = at.x, z = at.z;
        if (horizontal > FORCECAGE_RADIUS && horizontal > 1.0E-8) {
            double scale = FORCECAGE_RADIUS / horizontal;
            x = state.anchor.x + dx * scale;
            z = state.anchor.z + dz * scale;
        }
        target.snapTo(x, y, z, target.getYRot(), target.getXRot());
        target.setDeltaMovement(Vec3.ZERO);
    }

    private static void applyDomination(ServerPlayer owner, Mob target, State state) {
        LivingEntity current = target.getTarget();
        if (current == owner || (current != null && owner.isAlliedTo(current))) target.setTarget(null);
        Mob threat = state.level.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(28.0),
                        candidate -> candidate.isAlive() && !candidate.isRemoved() && candidate != target
                                && !owner.isAlliedTo(candidate) && !dominatedBy(owner.getUUID(), candidate.getUUID())
                                && (candidate.getTarget() == owner || candidate.getTarget() == target))
                .stream().min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(target))).orElse(null);
        if (threat != null) target.setTarget(threat);
        else if (target.distanceToSqr(owner) > 20.0) {
            target.setTarget(null);
            target.getNavigation().moveTo(owner, 1.12);
        }
        target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 10, 0, true, false));
    }

    private static void applyFeeblemind(Mob target) {
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 12, 7, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 12, 6, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 12, 2, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 12, 1, true, false));
    }

    private static Mob targetMob(ServerPlayer caster, CastTargetSnapshot snapshot) {
        LivingEntity raw = snapshot.targetEntity(caster).orElse(null);
        return raw instanceof Mob mob && mob.isAlive() && !mob.isRemoved() && !caster.isAlliedTo(mob) ? mob : null;
    }

    private static Vec3 retreatDestination(ServerPlayer caster, Mob target, Vec3 center) {
        Vec3 away = new Vec3(target.getX() - center.x, 0.0, target.getZ() - center.z);
        if (away.lengthSqr() < 1.0E-6) away = new Vec3(target.getX() - caster.getX(), 0.0, target.getZ() - caster.getZ());
        if (away.lengthSqr() < 1.0E-6) away = new Vec3(1.0, 0.0, 0.0);
        return target.position().add(away.normalize().scale(22.0));
    }

    private static void replace(State state) {
        State previous = ACTIVE.remove(state.targetId);
        if (previous != null) restore(previous);
        ACTIVE.put(state.targetId, state);
    }

    private static boolean dominatedBy(UUID ownerId, UUID targetId) {
        State state = ACTIVE.get(targetId);
        return state != null && state.ownerId.equals(ownerId)
                && "dominate_monster".equals(state.spellId) && state.active();
    }

    private static void restore(State state) {
        Entity raw = state.level.getEntity(state.targetId);
        if (!(raw instanceof Mob target) || !target.isAlive() || target.isRemoved()) return;
        if ("mass_suggestion".equals(state.spellId) || "dominate_monster".equals(state.spellId)) {
            target.getNavigation().stop();
            LivingEntity oldTarget = null;
            if (state.oldTargetId != null) {
                Entity candidate = state.level.getEntity(state.oldTargetId);
                if (candidate instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) oldTarget = living;
            }
            target.setTarget(oldTarget);
        }
    }

    private static final class State {
        private final ServerLevel level;
        private final UUID ownerId, targetId;
        private final String spellId;
        private final long expiresAt;
        private final Vec3 anchor;
        private final UUID oldTargetId;

        private State(ServerLevel level, UUID ownerId, UUID targetId, String spellId,
                      long expiresAt, Vec3 anchor, UUID oldTargetId) {
            this.level=level; this.ownerId=ownerId; this.targetId=targetId; this.spellId=spellId;
            this.expiresAt=expiresAt; this.anchor=anchor; this.oldTargetId=oldTargetId;
        }
        private boolean active() { return level.getGameTime() < expiresAt; }
    }
}
