package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Alpha.59 seventh-circle authority.
 *
 * Four already-strong player meanings are deliberately delegated instead of reimplemented:
 * Etherealness -> HighUtilitySpellService, Forcecage -> HighControlSpellService,
 * Plane Shift -> PlanarSpellService, Simulacrum -> SimulacrumService. The remaining six spells
 * use the immutable CastTargetSnapshot consumed by the same authored 7C presentation.
 */
public final class SeventhCircleSpellService {
    private static final Set<String> HANDLED = Set.of(
            "delayed_blast_fireball", "etherealness", "finger_of_death", "fire_storm",
            "forcecage", "plane_shift", "prismatic_spray", "reverse_gravity", "simulacrum", "teleport");

    public static final int NPC_ETHEREAL_TICKS = 360;
    public static final int NPC_FORCECAGE_TICKS = 400;
    public static final int REVERSE_GRAVITY_TICKS = 160;
    private static final double FORCECAGE_RADIUS = 3.1;
    private static final double FORCECAGE_DOWN = .75;
    private static final double FORCECAGE_UP = 4.2;

    private static final Map<UUID, NpcEtherealState> NPC_ETHEREAL = new HashMap<>();
    private static final Map<UUID, NpcCageState> NPC_CAGES = new HashMap<>();
    private static final Map<UUID, NpcCopyState> NPC_COPIES = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private SeventhCircleSpellService() {}

    public static boolean handles(String spellId) { return HANDLED.contains(spellId); }

    public static boolean execute(ServerPlayer caster, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (caster == null || snapshot == null || !snapshot.validFor(caster)) return false;
        ServerLevel level = (ServerLevel) caster.level();
        return switch (spellId) {
            case "delayed_blast_fireball" -> delayedBlast(level, caster, snapshot, range, power, true);
            case "etherealness" -> HighUtilitySpellService.execute(caster, spellId, range, power, snapshot);
            case "finger_of_death" -> fingerOfDeath(level, caster, null, snapshot, power);
            case "fire_storm" -> fireStorm(level, caster, snapshot, range, power, true);
            case "forcecage" -> HighControlSpellService.execute(caster, spellId, range, power, snapshot);
            case "plane_shift" -> PlanarSpellService.execute(caster, spellId);
            case "prismatic_spray" -> prismaticSpray(level, caster, snapshot, range, power);
            case "reverse_gravity" -> reverseGravity(level, caster, snapshot, range, power);
            case "simulacrum" -> SimulacrumService.execute(caster, snapshot);
            case "teleport" -> teleport(level, caster, snapshot.target(), true);
            default -> false;
        };
    }

    public static boolean executeNpc(ServerLevel level, Mob caster, LivingEntity target,
                                     SpellDefinition spell, double range, double power,
                                     CastTargetSnapshot snapshot) {
        if (level == null || caster == null || spell == null || snapshot == null
                || !snapshot.validFor(caster)) return false;
        return switch (spell.id()) {
            case "delayed_blast_fireball" -> delayedBlast(level, caster, snapshot, range, power, false);
            case "etherealness" -> npcEthereal(level, caster, power);
            case "finger_of_death" -> fingerOfDeath(level, caster, target, snapshot, power);
            case "fire_storm" -> fireStorm(level, caster, snapshot, range, power, false);
            case "forcecage" -> npcForcecage(level, caster, target, snapshot);
            // Player Plane Shift remains the full cross-dimension party transport. An NPC combat caster
            // instead uses the same self-relocation role as a safe planar disengage and never damages its target.
            case "plane_shift" -> npcPlaneShift(level, caster, target, snapshot);
            case "prismatic_spray" -> prismaticSpray(level, caster, snapshot, range, power);
            case "reverse_gravity" -> reverseGravity(level, caster, snapshot, range, power);
            case "simulacrum" -> npcSimulacrum(level, caster, target, snapshot);
            case "teleport" -> teleport(level, caster, snapshot.target(), false);
            default -> false;
        };
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        tickNpcEthereal(level, now);
        tickNpcCages(level, now);
        tickNpcCopies(level);
    }

    /** NPC Etherealness mirrors the player's 88% ordinary-damage phase reduction. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target) || target instanceof ServerPlayer
                || event.getAmount() <= 0.0F || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        NpcEtherealState state = NPC_ETHEREAL.get(target.getUUID());
        if (state == null || state.level != target.level() || !state.active()) return;
        event.setAmount(Math.max(0.0F, event.getAmount() * .12F));
    }

    /** Clears seventh-circle maintained NPC state affecting or owned by this entity. */
    public static void clear(LivingEntity subject) {
        if (subject == null) return;
        UUID id = subject.getUUID();
        NpcEtherealState ethereal = NPC_ETHEREAL.remove(id);
        if (ethereal != null) restoreNpcEthereal(ethereal);

        Iterator<Map.Entry<UUID, NpcCageState>> cages = NPC_CAGES.entrySet().iterator();
        while (cages.hasNext()) {
            NpcCageState state = cages.next().getValue();
            if (!state.ownerId.equals(id) && !state.targetId.equals(id)) continue;
            cages.remove();
        }

        Iterator<Map.Entry<UUID, NpcCopyState>> copies = NPC_COPIES.entrySet().iterator();
        while (copies.hasNext()) {
            NpcCopyState state = copies.next().getValue();
            if (!state.ownerId.equals(id) && !state.copyId.equals(id)) continue;
            discardNpcCopy(state);
            copies.remove();
        }
    }

    public static void clearAll() {
        for (NpcEtherealState state : new ArrayList<>(NPC_ETHEREAL.values())) restoreNpcEthereal(state);
        NPC_ETHEREAL.clear();
        NPC_CAGES.clear();
        for (NpcCopyState state : new ArrayList<>(NPC_COPIES.values())) discardNpcCopy(state);
        NPC_COPIES.clear();
        LAST_TICK.clear();
    }

    private static boolean delayedBlast(ServerLevel level, LivingEntity caster, CastTargetSnapshot snapshot,
                                        double range, double power, boolean destructiveTerrain) {
        Vec3 center = snapshot.target();
        double radius = Math.max(9.0, Math.min(13.0, 8.0 + range * .10));
        boolean hit = false;
        for (LivingEntity target : enemies(level, caster, center, radius, Math.max(6.0, radius * .72))) {
            double distance = Math.sqrt(center.distanceToSqr(target.position()));
            double falloff = Math.max(.42, 1.0 - distance / Math.max(1.0, radius) * .58);
            ArcaneDamage.hurt(level, caster, target, (float) (power * falloff));
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 300));
            Vec3 away = horizontalAway(center, target.position());
            target.push(away.x * (1.15 + falloff * .85), .38 + falloff * .42, away.z * (1.15 + falloff * .85));
            hit = true;
        }
        if (destructiveTerrain && caster instanceof ServerPlayer player)
            DestructiveMagicService.impact(player, "delayed_blast_fireball", center, radius, power);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.25F, .64F);
        return hit || destructiveTerrain || true;
    }

    private static boolean fingerOfDeath(ServerLevel level, LivingEntity caster, LivingEntity fallback,
                                         CastTargetSnapshot snapshot, double power) {
        LivingEntity target = targetEntity(level, caster, fallback, snapshot);
        if (!validEnemy(caster, target)) return false;
        double threshold = Math.max(32.0, power * .72);
        boolean ordinary = target.getMaxHealth() <= Math.max(260.0, power * 2.8);
        float amount = (float) (power * 1.62);
        if (ordinary && target.getHealth() <= threshold)
            amount = Math.max(amount, target.getHealth() + target.getMaxHealth() * .35F + 4.0F);
        ArcaneDamage.hurt(level, caster, target, amount);
        if (target.isAlive()) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 360, 4, true, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 360, 3, true, false));
        }
        level.playSound(null, target.blockPosition(), SoundEvents.WITHER_HURT,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .95F, .62F);
        return true;
    }

    private static boolean fireStorm(ServerLevel level, LivingEntity caster, CastTargetSnapshot snapshot,
                                     double range, double power, boolean destructiveTerrain) {
        Vec3 center = snapshot.target();
        double patternRadius = 5.0;
        double pillarRadius = 3.75;
        List<Vec3> pillars = new ArrayList<>(6);
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2.0 * i / 6.0;
            pillars.add(center.add(Math.cos(angle) * patternRadius, 0.0, Math.sin(angle) * patternRadius));
        }

        Set<UUID> hit = new HashSet<>();
        AABB field = new AABB(center, center).inflate(patternRadius + pillarRadius + 2.0, 8.0,
                patternRadius + pillarRadius + 2.0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, field,
                value -> validEnemy(caster, value))) {
            double nearest = pillars.stream().mapToDouble(p -> p.distanceToSqr(target.position())).min().orElse(Double.MAX_VALUE);
            if (nearest > pillarRadius * pillarRadius) continue;
            double falloff = Math.max(.58, 1.0 - Math.sqrt(nearest) / pillarRadius * .42);
            ArcaneDamage.hurt(level, caster, target, (float) (power * falloff));
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 260));
            hit.add(target.getUUID());
        }

        if (destructiveTerrain && caster instanceof ServerPlayer player) {
            for (Vec3 pillar : pillars)
                DestructiveMagicService.impact(player, "fire_storm", pillar, pillarRadius, power * .72);
        }
        level.playSound(null, BlockPos.containing(center), SoundEvents.BLAZE_SHOOT,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, 1.15F, .58F);
        return !hit.isEmpty() || destructiveTerrain || true;
    }

    private static boolean prismaticSpray(ServerLevel level, LivingEntity caster, CastTargetSnapshot snapshot,
                                          double range, double power) {
        Vec3 origin = snapshot.launchOrigin();
        Vec3 direction = snapshot.launchDirection();
        double length = Math.max(12.0, range);
        AABB box = new AABB(origin, origin.add(direction.scale(length))).inflate(length * .38 + 2.0);
        int affected = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                value -> validEnemy(caster, value))) {
            Vec3 relative = target.getEyePosition().subtract(origin);
            double projection = relative.dot(direction);
            if (projection < 0.0 || projection > length) continue;
            double allowed = 1.20 + projection * .30 + target.getBbWidth() * .55;
            if (relative.subtract(direction.scale(projection)).lengthSqr() > allowed * allowed) continue;
            double scale = Math.max(.62, 1.0 - projection / length * .28);
            ArcaneDamage.hurt(level, caster, target, (float) (power * scale));
            applyPrismaticCondition(target, Math.floorMod(target.getUUID().hashCode(), 5));
            affected++;
        }
        return affected > 0;
    }

    private static void applyPrismaticCondition(LivingEntity target, int band) {
        switch (band) {
            case 0 -> target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 240));
            case 1 -> {
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 360));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 220, 3, true, false));
            }
            case 2 -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 260, 4, true, false));
            case 3 -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 220, 3, true, false));
            default -> {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 220, 2, true, false));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 220, 0, true, false));
            }
        }
    }

    private static boolean reverseGravity(ServerLevel level, LivingEntity caster, CastTargetSnapshot snapshot,
                                          double range, double power) {
        Vec3 center = snapshot.target();
        double radius = Math.max(10.0, Math.min(15.0, range * .34));
        int affected = 0;
        for (LivingEntity target : enemies(level, caster, center, radius, 7.5)) {
            ArcaneDamage.hurt(level, caster, target, (float) (power * .34));
            target.push(0.0, 2.6 + Math.min(1.6, power / 90.0), 0.0);
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, REVERSE_GRAVITY_TICKS, 5, true, false));
            affected++;
        }
        return affected > 0;
    }

    private static boolean npcEthereal(ServerLevel level, Mob caster, double power) {
        NpcEtherealState previous = NPC_ETHEREAL.remove(caster.getUUID());
        if (previous != null) restoreNpcEthereal(previous);
        int duration = NPC_ETHEREAL_TICKS + (int) Math.min(240.0, Math.max(0.0, power));
        NpcEtherealState state = new NpcEtherealState(level, caster.getUUID(), level.getGameTime() + duration,
                caster.noPhysics, caster.isNoGravity(), caster.isInvisible());
        NPC_ETHEREAL.put(caster.getUUID(), state);
        applyNpcEthereal(caster);
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration + 40, 0, true, false));
        return true;
    }

    private static void tickNpcEthereal(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcEtherealState>> iterator = NPC_ETHEREAL.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcEtherealState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.entityId);
            if (!(raw instanceof Mob caster) || !caster.isAlive() || caster.isRemoved() || now >= state.expiresAt) {
                restoreNpcEthereal(state);
                iterator.remove();
                continue;
            }
            applyNpcEthereal(caster);
        }
    }

    private static void applyNpcEthereal(Mob caster) {
        caster.noPhysics = true;
        caster.setNoGravity(true);
        caster.setInvisible(true);
    }

    private static void restoreNpcEthereal(NpcEtherealState state) {
        Entity raw = state.level.getEntity(state.entityId);
        if (!(raw instanceof Mob caster) || caster.isRemoved()) return;
        caster.noPhysics = state.oldNoPhysics;
        caster.setNoGravity(state.oldNoGravity);
        caster.setInvisible(state.oldInvisible);
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false));
        WorldMagicService.cancelRelease(caster, "etherealness");
    }

    private static boolean npcForcecage(ServerLevel level, Mob caster, LivingEntity fallback,
                                        CastTargetSnapshot snapshot) {
        LivingEntity target = targetEntity(level, caster, fallback, snapshot);
        if (!validEnemy(caster, target)) return false;
        NPC_CAGES.put(target.getUUID(), new NpcCageState(level, caster.getUUID(), target.getUUID(),
                target.position(), level.getGameTime() + NPC_FORCECAGE_TICKS));
        level.playSound(null, target.blockPosition(), SoundEvents.BEACON_DEACTIVATE,
                SoundSource.HOSTILE, .9F, .82F);
        return true;
    }

    private static void tickNpcCages(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, NpcCageState>> iterator = NPC_CAGES.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcCageState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawTarget = level.getEntity(state.targetId);
            Entity rawOwner = level.getEntity(state.ownerId);
            if (!(rawTarget instanceof LivingEntity target) || !target.isAlive() || target.isRemoved()
                    || !(rawOwner instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                    || now >= state.expiresAt) {
                iterator.remove();
                continue;
            }
            applyNpcCage(target, state.anchor);
        }
    }

    private static void applyNpcCage(LivingEntity target, Vec3 anchor) {
        Vec3 at = target.position();
        double dx = at.x - anchor.x;
        double dz = at.z - anchor.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double y = Math.max(anchor.y - FORCECAGE_DOWN, Math.min(anchor.y + FORCECAGE_UP, at.y));
        if (horizontal <= FORCECAGE_RADIUS && y == at.y) return;
        double x = at.x, z = at.z;
        if (horizontal > FORCECAGE_RADIUS && horizontal > 1.0E-8) {
            double scale = FORCECAGE_RADIUS / horizontal;
            x = anchor.x + dx * scale;
            z = anchor.z + dz * scale;
        }
        target.snapTo(x, y, z, target.getYRot(), target.getXRot());
        target.setDeltaMovement(Vec3.ZERO);
    }

    private static boolean npcPlaneShift(ServerLevel level, Mob caster, LivingEntity target,
                                         CastTargetSnapshot snapshot) {
        Vec3 away = target == null ? snapshot.launchDirection().scale(-1.0)
                : caster.position().subtract(target.position());
        away = horizontalDirection(away);
        Vec3 desired = caster.position().add(away.scale(28.0));
        return teleport(level, caster, desired, false);
    }

    private static boolean npcSimulacrum(ServerLevel level, Mob caster, LivingEntity target,
                                         CastTargetSnapshot snapshot) {
        LivingEntity locked = targetEntity(level, caster, target, snapshot);
        Mob source = locked instanceof Mob mob && mob.isAlive() && !mob.isRemoved() ? mob : caster;
        NpcCopyState previous = NPC_COPIES.remove(caster.getUUID());
        if (previous != null) discardNpcCopy(previous);

        Entity raw = source.getType().create(level, EntitySpawnReason.EVENT);
        if (!(raw instanceof Mob copy)) return false;
        Vec3 side = new Vec3(-snapshot.launchDirection().z, 0.0, snapshot.launchDirection().x);
        if (side.lengthSqr() < 1.0E-8) side = new Vec3(1.0, 0.0, 0.0);
        else side = side.normalize();
        Vec3 at = caster.position().add(side.scale(2.2));
        copy.snapTo(at.x, at.y, at.z, caster.getYRot(), caster.getXRot());
        copy.finalizeSpawn(level, level.getCurrentDifficultyAt(caster.blockPosition()), EntitySpawnReason.EVENT, null);
        scaleAttribute(source, copy, Attributes.MAX_HEALTH, .50, 1.0);
        scaleAttribute(source, copy, Attributes.ATTACK_DAMAGE, .72, 1.0);
        scaleAttribute(source, copy, Attributes.ARMOR, .72, 0.0);
        scaleAttribute(source, copy, Attributes.ARMOR_TOUGHNESS, .72, 0.0);
        scaleAttribute(source, copy, Attributes.MOVEMENT_SPEED, 1.0, .08);
        scaleAttribute(source, copy, Attributes.SCALE, 1.0, .25);
        copy.setHealth(copy.getMaxHealth());
        copy.setCustomName(Component.literal("§b[NPC 시뮬라크럼] §f" + source.getName().getString()));
        copy.setCustomNameVisible(true);
        copy.setPersistenceRequired();
        copy.addTag("arcanecircle_npc_simulacrum");
        level.addFreshEntityWithPassengers(copy);
        LivingEntity combatTarget = target != null && target.isAlive() && !caster.isAlliedTo(target) ? target : caster.getTarget();
        if (combatTarget != null && combatTarget != copy) copy.setTarget(combatTarget);
        NPC_COPIES.put(caster.getUUID(), new NpcCopyState(level, caster.getUUID(), copy.getUUID()));
        return true;
    }

    private static void tickNpcCopies(ServerLevel level) {
        Iterator<Map.Entry<UUID, NpcCopyState>> iterator = NPC_COPIES.entrySet().iterator();
        while (iterator.hasNext()) {
            NpcCopyState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawCopy = level.getEntity(state.copyId);
            if (!(rawOwner instanceof Mob owner) || !owner.isAlive() || owner.isRemoved()
                    || !(rawCopy instanceof Mob copy) || !copy.isAlive() || copy.isRemoved()) {
                discardNpcCopy(state);
                iterator.remove();
                continue;
            }
            LivingEntity target = owner.getTarget();
            if (target != null && target.isAlive() && target != copy && !owner.isAlliedTo(target)) copy.setTarget(target);
            else if (copy.getTarget() == owner || (copy.getTarget() != null && owner.isAlliedTo(copy.getTarget()))) copy.setTarget(null);
            if (copy.distanceToSqr(owner) > 64.0) copy.getNavigation().moveTo(owner, 1.12);
        }
    }

    private static void discardNpcCopy(NpcCopyState state) {
        Entity raw = state.level.getEntity(state.copyId);
        if (raw != null && !raw.isRemoved()) raw.discard();
    }

    private static void scaleAttribute(Mob source, Mob copy,
                                       net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                       double factor, double minimum) {
        AttributeInstance from = source.getAttribute(attribute);
        AttributeInstance to = copy.getAttribute(attribute);
        if (from == null || to == null) return;
        to.setBaseValue(Math.max(minimum, from.getBaseValue() * factor));
    }

    private static boolean teleport(ServerLevel level, LivingEntity caster, Vec3 desired, boolean playerNotice) {
        BlockPos destination = findSafe(level, desired, 16);
        if (destination == null) return false;
        double x = destination.getX() + .5;
        double y = destination.getY();
        double z = destination.getZ() + .5;
        Vec3 from = caster.position();
        if (caster instanceof ServerPlayer player) {
            player.teleportTo(x, y, z);
            if (playerNotice) ArcaneNoticeService.push(player, Component.literal("§5[텔레포트] §f고정된 목적지의 안전한 착지점으로 이동했습니다."), 55);
        } else {
            caster.snapTo(x, y, z, caster.getYRot(), caster.getXRot());
        }
        level.playSound(null, BlockPos.containing(from), SoundEvents.ENDERMAN_TELEPORT,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .9F, .78F);
        level.playSound(null, destination, SoundEvents.ENDERMAN_TELEPORT,
                caster instanceof ServerPlayer ? SoundSource.PLAYERS : SoundSource.HOSTILE, .9F, 1.06F);
        return true;
    }

    private static BlockPos findSafe(ServerLevel level, Vec3 desired, int verticalSearch) {
        int baseX = (int) Math.floor(desired.x);
        int baseZ = (int) Math.floor(desired.z);
        int baseY = (int) Math.floor(Math.max(level.getMinY() + 2, Math.min(level.getMaxY() - 3, desired.y)));
        for (int radius = 0; radius <= 6; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                for (int d = 0; d <= verticalSearch; d++) {
                    int[] ys = d == 0 ? new int[]{baseY} : new int[]{baseY + d, baseY - d};
                    for (int y : ys) {
                        if (y <= level.getMinY() + 1 || y >= level.getMaxY() - 2) continue;
                        BlockPos feet = new BlockPos(baseX + dx, y, baseZ + dz);
                        if (!level.getBlockState(feet.below()).blocksMotion()) continue;
                        if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) continue;
                        return feet;
                    }
                }
            }
        }
        return null;
    }

    private static List<LivingEntity> enemies(ServerLevel level, LivingEntity caster, Vec3 center,
                                              double radius, double vertical) {
        return level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius, vertical, radius), value -> validEnemy(caster, value)
                        && center.distanceToSqr(value.position()) <= radius * radius + vertical * vertical * .15);
    }

    private static LivingEntity targetEntity(ServerLevel level, LivingEntity caster, LivingEntity fallback,
                                             CastTargetSnapshot snapshot) {
        if (snapshot.targetEntityId() != null) {
            Entity raw = level.getEntity(snapshot.targetEntityId());
            if (raw instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) return living;
        }
        return fallback != null && fallback.isAlive() && !fallback.isRemoved() ? fallback : null;
    }

    private static boolean validEnemy(LivingEntity caster, LivingEntity target) {
        return target != null && target != caster && target.isAlive() && !target.isRemoved() && !caster.isAlliedTo(target);
    }

    private static Vec3 horizontalAway(Vec3 center, Vec3 target) {
        return horizontalDirection(target.subtract(center));
    }

    private static Vec3 horizontalDirection(Vec3 value) {
        Vec3 horizontal = new Vec3(value.x, 0.0, value.z);
        if (horizontal.lengthSqr() < 1.0E-8) return new Vec3(1.0, 0.0, 0.0);
        return horizontal.normalize();
    }

    private static final class NpcEtherealState {
        private final ServerLevel level;
        private final UUID entityId;
        private final long expiresAt;
        private final boolean oldNoPhysics;
        private final boolean oldNoGravity;
        private final boolean oldInvisible;

        private NpcEtherealState(ServerLevel level, UUID entityId, long expiresAt,
                                 boolean oldNoPhysics, boolean oldNoGravity, boolean oldInvisible) {
            this.level = level;
            this.entityId = entityId;
            this.expiresAt = expiresAt;
            this.oldNoPhysics = oldNoPhysics;
            this.oldNoGravity = oldNoGravity;
            this.oldInvisible = oldInvisible;
        }
        private boolean active() { return level.getGameTime() < expiresAt; }
    }

    private record NpcCageState(ServerLevel level, UUID ownerId, UUID targetId, Vec3 anchor, long expiresAt) {}
    private record NpcCopyState(ServerLevel level, UUID ownerId, UUID copyId) {}
}
