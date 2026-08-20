package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;

/** One semi-real, commandable ice duplicate per caster. */
public final class SimulacrumService {
    private static final String SPELL_ID = "simulacrum";
    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private SimulacrumService() {}

    public static boolean handles(String id) { return SPELL_ID.equals(id); }

    public static boolean execute(ServerPlayer caster, CastTargetSnapshot snapshot) {
        LivingEntity raw = snapshot.targetEntity(caster).orElse(null);
        if (!(raw instanceof Mob source) || !source.isAlive() || source.isRemoved()) {
            ArcaneNoticeService.push(caster, Component.literal("§c[시뮬라크럼] §f복제할 생명체를 조준해야 합니다."), 65);
            return false;
        }
        removeOwned(caster.getUUID(), true);
        ServerLevel level = caster.serverLevel();
        Entity created = source.getType().create(level, EntitySpawnReason.EVENT);
        if (!(created instanceof Mob copy)) return false;

        Vec3 right = new Vec3(-caster.getLookAngle().z, 0.0, caster.getLookAngle().x);
        if (right.lengthSqr() < 1.0E-8) right = new Vec3(1.0, 0.0, 0.0);
        else right = right.normalize();
        Vec3 at = caster.position().add(right.scale(2.2));
        copy.snapTo(at.x, at.y, at.z, caster.getYRot(), 0.0F);
        copy.finalizeSpawn(level, level.getCurrentDifficultyAt(caster.blockPosition()), EntitySpawnReason.EVENT, null);
        scaleAttribute(source, copy, Attributes.MAX_HEALTH, .50, 1.0);
        scaleAttribute(source, copy, Attributes.ATTACK_DAMAGE, .72, 1.0);
        scaleAttribute(source, copy, Attributes.ARMOR, .72, 0.0);
        scaleAttribute(source, copy, Attributes.ARMOR_TOUGHNESS, .72, 0.0);
        scaleAttribute(source, copy, Attributes.MOVEMENT_SPEED, 1.0, .08);
        scaleAttribute(source, copy, Attributes.SCALE, 1.0, .25);
        copy.setHealth(copy.getMaxHealth());
        copy.setCustomName(Component.literal("§b[시뮬라크럼] §f" + source.getName().getString()));
        copy.setCustomNameVisible(true);
        copy.setPersistenceRequired();
        copy.addTag("arcanecircle_simulacrum");
        level.addFreshEntityWithPassengers(copy);
        ACTIVE.put(caster.getUUID(), new State(level, caster.getUUID(), copy.getUUID(), Mode.FOLLOW,
                caster.position(), null, 0L));
        level.playSound(null, copy.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.0F, .72F);
        ArcaneNoticeService.push(caster, Component.literal("§b[시뮬라크럼] §f" + source.getName().getString()
                + "의 반실체 얼음 복제체를 만들었습니다. §7최대 체력 50%, 전투력 약 72%, 웅크린 채 G키로 명령합니다."), 100);
        return true;
    }

    public static boolean useAuthority(ServerPlayer caster) {
        if (!caster.isShiftKeyDown()) return false;
        State state = ACTIVE.get(caster.getUUID());
        if (state == null || state.level != caster.level()) return false;
        Entity raw = state.level.getEntity(state.entityId);
        if (!(raw instanceof Mob copy) || !copy.isAlive()) { ACTIVE.remove(caster.getUUID()); return false; }
        Optional<Mob> looked = lookTarget(caster, 28.0);
        if (looked.isPresent()) {
            Mob target = looked.get();
            state.mode = Mode.ASSAULT;
            state.commandTarget = target.getUUID();
            state.commandExpires = state.level.getGameTime() + 400;
            copy.setTarget(target);
            ArcaneNoticeService.push(caster, Component.literal("§c[시뮬라크럼] §f집중 공격 명령: " + target.getName().getString()), 65);
            return true;
        }
        if (state.mode == Mode.GUARD) {
            state.mode = Mode.FOLLOW;
            state.commandTarget = null;
            ArcaneNoticeService.push(caster, Component.literal("§b[시뮬라크럼] §f추종 모드: 시전자를 따라 이동합니다."), 55);
        } else {
            state.mode = Mode.GUARD;
            state.guard = caster.position();
            state.commandTarget = null;
            copy.setTarget(null);
            ArcaneNoticeService.push(caster, Component.literal("§e[시뮬라크럼] §f수호 모드: 현재 지점을 지키며 주변 적을 공격합니다."), 55);
        }
        return true;
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        Iterator<Map.Entry<UUID, State>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            State state = it.next().getValue();
            if (state.level != level) continue;
            Entity rawCopy = level.getEntity(state.entityId);
            Entity rawOwner = level.getEntity(state.ownerId);
            if (!(rawCopy instanceof Mob copy) || !copy.isAlive() || copy.isRemoved()) {
                if (rawOwner instanceof LivingEntity owner) WorldMagicService.cancelRelease(owner, SPELL_ID);
                it.remove();
                continue;
            }
            if (!(rawOwner instanceof ServerPlayer owner) || !owner.isAlive()) {
                copy.setTarget(null);
                copy.getNavigation().stop();
                continue;
            }
            LivingEntity target = resolveTarget(state, owner, copy, now);
            if (target != null) copy.setTarget(target);
            else if (copy.getTarget() == owner || (copy.getTarget() != null && owner.isAlliedTo(copy.getTarget()))) copy.setTarget(null);
            if (state.mode == Mode.GUARD) {
                if (copy.position().distanceToSqr(state.guard) > 36.0)
                    copy.getNavigation().moveTo(state.guard.x, state.guard.y, state.guard.z, 1.05);
            } else if (target == null && copy.distanceToSqr(owner) > 20.0) {
                copy.getNavigation().moveTo(owner, 1.12);
            }
        }
    }

    public static void clear(ServerPlayer owner) { if (owner != null) removeOwned(owner.getUUID(), false); }

    public static void clearAll() {
        for (State state : ACTIVE.values()) {
            Entity raw = state.level.getEntity(state.entityId);
            if (raw != null) raw.discard();
        }
        ACTIVE.clear();
        LAST_TICK.clear();
    }

    private static LivingEntity resolveTarget(State state, ServerPlayer owner, Mob copy, long now) {
        if (state.mode == Mode.ASSAULT && state.commandTarget != null && now < state.commandExpires) {
            Entity raw = state.level.getEntity(state.commandTarget);
            if (raw instanceof LivingEntity living && living.isAlive() && living != owner && !owner.isAlliedTo(living)) return living;
        } else if (state.mode == Mode.ASSAULT) {
            state.mode = Mode.FOLLOW;
            state.commandTarget = null;
        }
        Vec3 center = state.mode == Mode.GUARD ? state.guard : owner.position();
        double radius = state.mode == Mode.GUARD ? 13.0 : 18.0;
        return state.level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(center, center).inflate(radius, radius * .7, radius),
                        e -> e.isAlive() && e != owner && e != copy && !owner.isAlliedTo(e) && !isArcaneCopy(e.getUUID()))
                .stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(copy))).orElse(null);
    }

    private static Optional<Mob> lookTarget(ServerPlayer owner, double range) {
        Vec3 eye = owner.getEyePosition();
        Vec3 look = owner.getLookAngle().normalize();
        return owner.serverLevel().getEntitiesOfClass(Mob.class,
                        owner.getBoundingBox().expandTowards(look.scale(range)).inflate(2.5),
                        m -> m.isAlive() && !owner.isAlliedTo(m) && !isArcaneCopy(m.getUUID()))
                .stream().filter(m -> {
                    Vec3 to = m.getEyePosition().subtract(eye);
                    double projection = to.dot(look);
                    return projection >= 0.0 && projection <= range
                            && to.subtract(look.scale(projection)).length() <= Math.max(1.4, m.getBbWidth() + .9);
                }).min(Comparator.comparingDouble(m -> m.distanceToSqr(owner)));
    }

    private static boolean isArcaneCopy(UUID entityId) {
        return ACTIVE.values().stream().anyMatch(s -> s.entityId.equals(entityId));
    }

    private static void scaleAttribute(Mob source, Mob copy,
                                       net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
                                       double factor, double minimum) {
        AttributeInstance from = source.getAttribute(attr);
        AttributeInstance to = copy.getAttribute(attr);
        if (from == null || to == null) return;
        to.setBaseValue(Math.max(minimum, from.getBaseValue() * factor));
    }

    private static void removeOwned(UUID ownerId, boolean replacing) {
        State state = ACTIVE.remove(ownerId);
        if (state == null) return;
        Entity raw = state.level.getEntity(state.entityId);
        if (raw != null) raw.discard();
        if (!replacing) {
            Entity owner = state.level.getEntity(ownerId);
            if (owner instanceof LivingEntity living) WorldMagicService.cancelRelease(living, SPELL_ID);
        }
    }

    private enum Mode { FOLLOW, GUARD, ASSAULT }

    private static final class State {
        private final ServerLevel level;
        private final UUID ownerId;
        private final UUID entityId;
        private Mode mode;
        private Vec3 guard;
        private UUID commandTarget;
        private long commandExpires;

        private State(ServerLevel level, UUID ownerId, UUID entityId, Mode mode, Vec3 guard,
                      UUID commandTarget, long commandExpires) {
            this.level = level;
            this.ownerId = ownerId;
            this.entityId = entityId;
            this.mode = mode;
            this.guard = guard;
            this.commandTarget = commandTarget;
            this.commandExpires = commandExpires;
        }
    }
}
