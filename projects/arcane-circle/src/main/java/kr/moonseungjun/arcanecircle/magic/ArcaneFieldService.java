package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.world.ArcaneMageService;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative sustained magic whose gameplay cannot be represented by one potion pulse.
 * Time Stop actually halts non-allied mob AI and Arcane casting; Antimagic Field continuously
 * suppresses spell-like status effects and Arcane casting. Wish cleanses harmful effects without
 * deleting the player's beneficial buffs.
 */
public final class ArcaneFieldService {
    public static final int TIME_STOP_TICKS = 160;
    public static final int ANTIMAGIC_TICKS = 320;

    private static final Map<UUID, AntimagicField> ANTIMAGIC = new HashMap<>();
    private static final Map<UUID, TimeField> TIME_FIELDS = new HashMap<>();
    private static final Map<UUID, FrozenMob> FROZEN_MOBS = new HashMap<>();
    private static final Map<UUID, FrozenEntity> FROZEN_ENTITIES = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private ArcaneFieldService() {}

    public static boolean handles(String spellId) {
        return "antimagic_field".equals(spellId)
                || "time_stop".equals(spellId)
                || "wish".equals(spellId);
    }

    public static boolean executeSpecial(ServerPlayer player, String spellId, double range,
                                         double power, CastTargetSnapshot snapshot) {
        return switch (spellId) {
            case "antimagic_field" -> activateAntimagic(player, range);
            case "time_stop" -> activateTimeStop(player, range, snapshot);
            case "wish" -> fulfillWish(player);
            default -> false;
        };
    }

    public static boolean blocksCasting(LivingEntity caster) {
        if (caster == null || !caster.isAlive()) return false;
        if (SpellGameplayService.blocksCasting(caster)) return true;
        if (HighControlSpellService.blocksCasting(caster)) return true;
        for (AntimagicField field : ANTIMAGIC.values()) {
            if (!field.active() || field.level() != caster.level()) continue;
            Entity owner = field.level().getEntity(field.ownerId());
            if (!(owner instanceof LivingEntity livingOwner) || !livingOwner.isAlive()) continue;
            if (livingOwner.position().distanceToSqr(caster.position()) <= field.radius() * field.radius()) return true;
        }
        for (TimeField field : TIME_FIELDS.values()) {
            if (!field.active() || field.level() != caster.level()) continue;
            Entity rawOwner = field.level().getEntity(field.ownerId());
            if (!(rawOwner instanceof LivingEntity owner) || !owner.isAlive()) continue;
            if (owner.getUUID().equals(caster.getUUID()) || owner.isAlliedTo(caster)) continue;
            if (field.center().distanceToSqr(caster.position()) <= field.radius() * field.radius()) return true;
        }
        return false;
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        cleanupExpired(level);
        applyAntimagic(level);
        applyTimeStop(level);
    }

    public static void clear(UUID ownerId) {
        ANTIMAGIC.remove(ownerId);
        TimeField removed = TIME_FIELDS.remove(ownerId);
        // Recompute all remaining fields immediately. Restoring the whole level here gave a second
        // active Time Stop a one-tick hole whenever another owner left or changed dimension.
        if (removed != null) applyTimeStop(removed.level());
    }

    public static void clearAll() {
        for (FrozenMob frozen : FROZEN_MOBS.values()) restore(frozen);
        for (FrozenEntity frozen : FROZEN_ENTITIES.values()) restore(frozen);
        FROZEN_MOBS.clear();
        FROZEN_ENTITIES.clear();
        ANTIMAGIC.clear();
        TIME_FIELDS.clear();
        LAST_TICK.clear();
    }

    private static boolean activateAntimagic(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        double radius = Math.max(12.0, Math.min(24.0, range * .85));
        ANTIMAGIC.put(player.getUUID(), new AntimagicField(level, player.getUUID(), radius,
                level.getGameTime() + ANTIMAGIC_TICKS));
        ArcaneNoticeService.push(player, Component.literal("§5[반마법장] §f" + one(ANTIMAGIC_TICKS / 20.0) + "초 · 반경 "
                + one(radius) + " · 상태효과와 Arcane 시전을 지속 억제"), 100);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 1.0F, .72F);
        applyAntimagicField(level, player, radius);
        return true;
    }

    private static boolean activateTimeStop(ServerPlayer player, double range, CastTargetSnapshot snapshot) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 center = snapshot != null && snapshot.validFor(player) ? snapshot.target() : player.position();
        double radius = Math.max(20.0, Math.min(48.0, range * .75));
        TIME_FIELDS.put(player.getUUID(), new TimeField(level, player.getUUID(), center, radius,
                level.getGameTime() + TIME_STOP_TICKS));
        ArcaneNoticeService.push(player, Component.literal("§b[시간 정지] §f" + one(TIME_STOP_TICKS / 20.0) + "초 · 반경 "
                + one(radius) + " · 비아군의 AI·이동·Arcane 시전 정지"), 100);
        level.playSound(null, BlockPos.containing(center), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.15F, .54F);
        return true;
    }

    private static boolean fulfillWish(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        player.setHealth(player.getMaxHealth());
        cleanseHarmful(player);
        player.setRemainingFireTicks(0);
        player.setTicksFrozen(0);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 5));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 12));
        ArcaneNoticeService.push(player, Component.literal(
                "§6[소원 성취] §f체력·마력 회복 · 해로운 상태 제거 · 기존 이로운 버프 보존 · 다른 주문 쿨타임 초기화"), 120);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 1.0F, .82F);
        return true;
    }

    private static void cleanupExpired(ServerLevel level) {
        Iterator<Map.Entry<UUID, AntimagicField>> anti = ANTIMAGIC.entrySet().iterator();
        while (anti.hasNext()) {
            AntimagicField field = anti.next().getValue();
            if (field.level() != level) continue;
            Entity owner = level.getEntity(field.ownerId());
            if (!field.active() || !(owner instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) anti.remove();
        }
        Iterator<Map.Entry<UUID, TimeField>> time = TIME_FIELDS.entrySet().iterator();
        while (time.hasNext()) {
            TimeField field = time.next().getValue();
            if (field.level() != level) continue;
            Entity owner = level.getEntity(field.ownerId());
            if (!field.active() || !(owner instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) time.remove();
        }
    }

    private static void applyAntimagic(ServerLevel level) {
        for (AntimagicField field : ANTIMAGIC.values()) {
            if (field.level() != level || !field.active()) continue;
            Entity rawOwner = level.getEntity(field.ownerId());
            if (!(rawOwner instanceof ServerPlayer owner) || !owner.isAlive()) continue;
            applyAntimagicField(level, owner, field.radius());
        }
    }

    private static void applyAntimagicField(ServerLevel level, ServerPlayer owner, double radius) {
        AABB box = owner.getBoundingBox().inflate(radius, radius * .75, radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> value.isAlive() && !value.isRemoved()
                        && owner.position().distanceToSqr(value.position()) <= radius * radius)) {
            SpellGameplayService.clear(entity);
            HighControlSpellService.clear(entity);
            suppressMagicEffects(entity);
            if (entity instanceof ServerPlayer player) {
                if (player == owner) {
                    SpellKineticsService.clear(player.getUUID());
                    clearFusion(player);
                } else {
                    suppressPlayerCasting(player);
                }
            } else if (entity instanceof Mob mob && ArcaneMageService.isMage(mob)) {
                WorldMagicService.stop(mob);
            }
        }
    }

    private static void applyTimeStop(ServerLevel level) {
        Set<UUID> shouldRemainFrozen = new HashSet<>();
        Set<UUID> shouldRemainFrozenEntities = new HashSet<>();
        for (TimeField field : TIME_FIELDS.values()) {
            if (field.level() != level || !field.active()) continue;
            Entity rawOwner = field.level().getEntity(field.ownerId());
            if (!(rawOwner instanceof ServerPlayer owner) || !owner.isAlive()) continue;
            double radius = field.radius();
            AABB box = new AABB(field.center(), field.center()).inflate(radius, radius * .75, radius);

            for (Mob mob : level.getEntitiesOfClass(Mob.class, box,
                    value -> value.isAlive() && !value.isRemoved()
                            && !owner.isAlliedTo(value)
                            && field.center().distanceToSqr(value.position()) <= radius * radius)) {
                shouldRemainFrozen.add(mob.getUUID());
                FROZEN_MOBS.computeIfAbsent(mob.getUUID(), ignored -> new FrozenMob(level, mob.getUUID(), mob.isNoAi()));
                mob.setNoAi(true);
                mob.setDeltaMovement(Vec3.ZERO);
                if (ArcaneMageService.isMage(mob)) WorldMagicService.stop(mob);
            }

            for (ServerPlayer target : level.getEntitiesOfClass(ServerPlayer.class, box,
                    value -> value.isAlive() && !value.isSpectator()
                            && value != owner && !owner.isAlliedTo(value)
                            && field.center().distanceToSqr(value.position()) <= radius * radius)) {
                target.setDeltaMovement(Vec3.ZERO);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 5, 255, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5, 255, true, false));
                suppressPlayerCasting(target);
            }

            // Projectiles, dropped items and other visible non-living motion stop in place too.
            for (Entity entity : level.getEntitiesOfClass(Entity.class, box,
                    value -> !value.isRemoved() && !(value instanceof LivingEntity)
                            && field.center().distanceToSqr(value.position()) <= radius * radius)) {
                shouldRemainFrozenEntities.add(entity.getUUID());
                FROZEN_ENTITIES.computeIfAbsent(entity.getUUID(), ignored ->
                        new FrozenEntity(level, entity.getUUID(), entity.getDeltaMovement(), entity.isNoGravity()));
                entity.setDeltaMovement(Vec3.ZERO);
                entity.setNoGravity(true);
            }
        }

        Iterator<Map.Entry<UUID, FrozenMob>> iterator = FROZEN_MOBS.entrySet().iterator();
        while (iterator.hasNext()) {
            FrozenMob frozen = iterator.next().getValue();
            if (frozen.level() != level || shouldRemainFrozen.contains(frozen.entityId())) continue;
            restore(frozen);
            iterator.remove();
        }
        Iterator<Map.Entry<UUID, FrozenEntity>> moving = FROZEN_ENTITIES.entrySet().iterator();
        while (moving.hasNext()) {
            FrozenEntity frozen = moving.next().getValue();
            if (frozen.level() != level || shouldRemainFrozenEntities.contains(frozen.entityId())) continue;
            restore(frozen);
            moving.remove();
        }
    }

    private static void suppressPlayerCasting(ServerPlayer player) {
        if (!SpellCastingService.chargingSpell(player).isBlank()) SpellCastingService.cancelCharge(player, false);
        clearFusion(player);
        SpellKineticsService.cancel(player);
    }

    private static void clearFusion(ServerPlayer player) {
        if (!SpellCastingService.pendingFusion(player).isEmpty()) SpellCastingService.clearFusion(player, false);
    }

    private static void suppressMagicEffects(LivingEntity entity) {
        entity.removeEffect(MobEffects.ABSORPTION);
        entity.removeEffect(MobEffects.RESISTANCE);
        entity.removeEffect(MobEffects.REGENERATION);
        entity.removeEffect(MobEffects.SPEED);
        entity.removeEffect(MobEffects.STRENGTH);
        entity.removeEffect(MobEffects.INVISIBILITY);
        entity.removeEffect(MobEffects.FIRE_RESISTANCE);
        entity.removeEffect(MobEffects.NIGHT_VISION);
        entity.removeEffect(MobEffects.LUCK);
        entity.removeEffect(MobEffects.JUMP_BOOST);
        entity.removeEffect(MobEffects.SLOW_FALLING);
        entity.removeEffect(MobEffects.SLOWNESS);
        entity.removeEffect(MobEffects.WEAKNESS);
        entity.removeEffect(MobEffects.BLINDNESS);
        entity.removeEffect(MobEffects.NAUSEA);
        entity.removeEffect(MobEffects.WITHER);
        entity.removeEffect(MobEffects.POISON);
        entity.removeEffect(MobEffects.MINING_FATIGUE);
        entity.removeEffect(MobEffects.LEVITATION);
        entity.removeEffect(MobEffects.DARKNESS);
    }

    private static void cleanseHarmful(ServerPlayer player) {
        player.removeEffect(MobEffects.SLOWNESS);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.NAUSEA);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.MINING_FATIGUE);
        player.removeEffect(MobEffects.LEVITATION);
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.HUNGER);
    }

    private static void restoreFrozenLevel(ServerLevel level) {
        Iterator<Map.Entry<UUID, FrozenMob>> iterator = FROZEN_MOBS.entrySet().iterator();
        while (iterator.hasNext()) {
            FrozenMob frozen = iterator.next().getValue();
            if (frozen.level() != level) continue;
            restore(frozen);
            iterator.remove();
        }
    }

    private static void restore(FrozenMob frozen) {
        Entity raw = frozen.level().getEntity(frozen.entityId());
        if (raw instanceof Mob mob && mob.isAlive() && !mob.isRemoved()) mob.setNoAi(frozen.wasNoAi());
    }

    private static void restore(FrozenEntity frozen) {
        Entity raw = frozen.level().getEntity(frozen.entityId());
        if (raw == null || raw.isRemoved()) return;
        raw.setNoGravity(frozen.wasNoGravity());
        raw.setDeltaMovement(frozen.velocity());
    }

    private static String one(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private record AntimagicField(ServerLevel level, UUID ownerId, double radius, long expiresAt) {
        boolean active() { return level.getGameTime() < expiresAt; }
    }

    private record TimeField(ServerLevel level, UUID ownerId, Vec3 center, double radius, long expiresAt) {
        boolean active() { return level.getGameTime() < expiresAt; }
    }

    private record FrozenMob(ServerLevel level, UUID entityId, boolean wasNoAi) {}
    private record FrozenEntity(ServerLevel level, UUID entityId, Vec3 velocity, boolean wasNoGravity) {}
}
