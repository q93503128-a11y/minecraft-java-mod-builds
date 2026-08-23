package kr.moonseungjun.arcanecircle.magic;

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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** High-circle utility spells with real persistent state instead of generic potion aliases. */
public final class HighUtilitySpellService {
    private static final Set<String> HANDLED = Set.of("clone", "true_polymorph", "maze", "etherealness");
    private static final int CLONE_TICKS = 1800;
    private static final int TRUE_POLYMORPH_TICKS = 480;
    private static final int MAZE_TICKS = 480;

    private static final Map<UUID, EtherealState> ETHEREAL = new HashMap<>();
    private static final Map<UUID, CloneState> CLONES = new HashMap<>();
    private static final Map<UUID, PolymorphState> POLYMORPHS = new HashMap<>();
    private static final Map<UUID, MazeState> MAZES = new HashMap<>();
    private static final Map<ServerLevel, Long> LAST_TICK = new WeakHashMap<>();

    private HighUtilitySpellService() {}

    public static boolean handles(String spellId) { return HANDLED.contains(spellId); }

    public static boolean execute(ServerPlayer player, String spellId, double range, double power,
                                  CastTargetSnapshot snapshot) {
        if (player == null || snapshot == null || !snapshot.validFor(player)) return false;
        return switch (spellId) {
            case "clone" -> cloneCreature(player, snapshot);
            case "true_polymorph" -> truePolymorph(player, snapshot);
            case "maze" -> maze(player, snapshot);
            case "etherealness" -> etherealness(player, power);
            default -> false;
        };
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Long previous = LAST_TICK.put(level, now);
        if (previous != null && previous == now) return;
        tickEthereal(level, now);
        tickClones(level, now);
        tickPolymorphs(level, now);
        tickMazes(level, now);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getAmount() <= 0.0F) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        EtherealState state = ETHEREAL.get(player.getUUID());
        if (state == null || state.level != player.level() || !state.active()) return;
        event.setAmount(Math.max(0.0F, event.getAmount() * 0.12F));
    }

    public static void clear(ServerPlayer player) {
        if (player == null) return;
        UUID ownerId = player.getUUID();
        EtherealState ethereal = ETHEREAL.remove(ownerId);
        if (ethereal != null) restoreEthereal(player, ethereal);
        CloneState clone = CLONES.remove(ownerId);
        if (clone != null) discardClone(clone);
        restoreOwnedStates(ownerId);
    }

    public static void clearAll() {
        for (EtherealState state : new ArrayList<>(ETHEREAL.values())) {
            Entity raw = state.level.getEntity(state.playerId);
            if (raw instanceof ServerPlayer player) restoreEthereal(player, state);
        }
        ETHEREAL.clear();
        for (CloneState state : new ArrayList<>(CLONES.values())) discardClone(state);
        CLONES.clear();
        for (PolymorphState state : new ArrayList<>(POLYMORPHS.values())) restorePolymorph(state, false);
        POLYMORPHS.clear();
        for (MazeState state : new ArrayList<>(MAZES.values())) restoreMaze(state);
        MAZES.clear();
        LAST_TICK.clear();
    }

    private static boolean cloneCreature(ServerPlayer player, CastTargetSnapshot snapshot) {
        Mob source = targetMob(player, snapshot);
        if (source == null) return false;
        ServerLevel level = (ServerLevel) player.level();
        CloneState old = CLONES.remove(player.getUUID());
        if (old != null) discardClone(old);
        Entity rawClone = source.getType().create(level, EntitySpawnReason.EVENT);
        if (!(rawClone instanceof Mob clone)) return false;
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0.0, look.x);
        right = right.lengthSqr() < 1.0E-8 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        Vec3 spawn = source.position().add(right.scale(1.8));
        clone.snapTo(spawn.x, spawn.y, spawn.z, source.getYRot(), source.getXRot());
        clone.finalizeSpawn(level, level.getCurrentDifficultyAt(source.blockPosition()), EntitySpawnReason.EVENT, null);
        copyCombatBody(source, clone);
        clone.setCustomName(Component.literal("§d[클론] §f" + source.getName().getString()));
        clone.setCustomNameVisible(true);
        clone.setPersistenceRequired();
        clone.addTag("arcanecircle_clone");
        clone.addTag("arcanecircle_clone_owner_" + player.getUUID());
        LivingEntity initial = source.getTarget();
        if (initial != null && initial.isAlive() && initial != player && !player.isAlliedTo(initial)) clone.setTarget(initial);
        level.addFreshEntityWithPassengers(clone);
        CLONES.put(player.getUUID(), new CloneState(level, player.getUUID(), clone.getUUID(),
                level.getGameTime() + CLONE_TICKS));
        level.playSound(null, clone.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.05F, .72F);
        ArcaneNoticeService.push(player, Component.literal("§d[클론] §f" + source.getName().getString()
                + "의 전투 육체를 90초간 복제했습니다. §7한 번에 1체만 유지되며 시전자나 자신을 공격하는 적을 자동 반격합니다."), 110);
        return true;
    }

    private static void copyCombatBody(Mob source, Mob clone) {
        copyAttribute(source, clone, Attributes.MAX_HEALTH, source.getMaxHealth());
        copyAttribute(source, clone, Attributes.ATTACK_DAMAGE, Double.NaN);
        copyAttribute(source, clone, Attributes.ARMOR, Double.NaN);
        copyAttribute(source, clone, Attributes.ARMOR_TOUGHNESS, Double.NaN);
        copyAttribute(source, clone, Attributes.MOVEMENT_SPEED, Double.NaN);
        copyAttribute(source, clone, Attributes.SCALE, Double.NaN);
        clone.setHealth(clone.getMaxHealth());
    }

    private static void copyAttribute(Mob source, Mob clone,
                                      net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                      double explicitMinimum) {
        AttributeInstance from = source.getAttribute(attribute);
        AttributeInstance to = clone.getAttribute(attribute);
        if (from == null || to == null) return;
        double value = Double.isNaN(explicitMinimum) ? from.getBaseValue() : Math.max(explicitMinimum, from.getBaseValue());
        to.setBaseValue(value);
    }

    private static void tickClones(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, CloneState>> iterator = CLONES.entrySet().iterator();
        while (iterator.hasNext()) {
            CloneState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawOwner = level.getEntity(state.ownerId);
            Entity rawClone = level.getEntity(state.cloneId);
            if (!(rawOwner instanceof ServerPlayer owner) || !owner.isAlive() || owner.isSpectator()
                    || !(rawClone instanceof Mob clone) || !clone.isAlive() || clone.isRemoved()
                    || now >= state.expiresAt) {
                discardClone(state);
                iterator.remove();
                continue;
            }
            LivingEntity current = clone.getTarget();
            if (current != null && (!current.isAlive() || current == owner || owner.isAlliedTo(current))) clone.setTarget(null);
            if (clone.getTarget() == null) {
                Mob threat = level.getEntitiesOfClass(Mob.class, clone.getBoundingBox().inflate(24.0),
                                candidate -> candidate != clone && candidate.isAlive() && !candidate.isRemoved()
                                        && !owner.isAlliedTo(candidate)
                                        && (candidate.getTarget() == owner || candidate.getTarget() == clone))
                        .stream().min(Comparator.comparingDouble(clone::distanceToSqr)).orElse(null);
                if (threat != null) clone.setTarget(threat);
            }
            if (clone.getTarget() == null && clone.distanceToSqr(owner) > 100.0) clone.getNavigation().moveTo(owner, 1.16);
        }
    }

    private static void discardClone(CloneState state) {
        Entity rawClone = state.level.getEntity(state.cloneId);
        if (rawClone instanceof Mob clone && !clone.isRemoved()) clone.discard();
        Entity rawOwner = state.level.getEntity(state.ownerId);
        if (rawOwner instanceof LivingEntity owner) WorldMagicService.cancelRelease(owner, "clone");
    }

    private static boolean truePolymorph(ServerPlayer player, CastTargetSnapshot snapshot) {
        Mob original = targetMob(player, snapshot);
        if (original == null) return false;
        PolymorphState existing = POLYMORPHS.remove(original.getUUID());
        if (existing != null) restorePolymorph(existing, false);
        ServerLevel level = (ServerLevel) player.level();
        Mob proxy = createPolymorphBody(level, original);
        if (proxy == null) return false;
        Vec3 anchor = original.position();
        proxy.snapTo(anchor.x, anchor.y, anchor.z, original.getYRot(), original.getXRot());
        proxy.finalizeSpawn(level, level.getCurrentDifficultyAt(original.blockPosition()), EntitySpawnReason.EVENT, null);
        proxy.setCustomName(Component.literal("§d[완전한 변신] §f" + original.getName().getString()));
        proxy.setCustomNameVisible(true);
        proxy.setPersistenceRequired();
        proxy.addTag("arcanecircle_true_polymorph_proxy");
        level.addFreshEntityWithPassengers(proxy);
        PolymorphState state = new PolymorphState(level, player.getUUID(), original.getUUID(), proxy.getUUID(),
                anchor, original.getYRot(), original.getXRot(), original.getHealth(), level.getGameTime() + TRUE_POLYMORPH_TICKS,
                original.isInvisible(), original.isInvulnerable(), original.isNoGravity(), original.isSilent(),
                original.isNoAi(), original.noPhysics);
        POLYMORPHS.put(original.getUUID(), state);
        stash(original);
        level.playSound(null, proxy.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 1.0F, .58F);
        ArcaneNoticeService.push(player, Component.literal("§d[완전한 변신] §f대상의 실제 몸체를 "
                + proxy.getType().getDescription().getString() + " 형태로 24초간 교체했습니다."), 95);
        return true;
    }

    private static Mob createPolymorphBody(ServerLevel level, Mob original) {
        return switch (Math.floorMod(original.getUUID().hashCode(), 4)) {
            case 0 -> EntityTypes.RABBIT.create(level, EntitySpawnReason.EVENT);
            case 1 -> EntityTypes.CHICKEN.create(level, EntitySpawnReason.EVENT);
            case 2 -> EntityTypes.PIG.create(level, EntitySpawnReason.EVENT);
            default -> EntityTypes.SHEEP.create(level, EntitySpawnReason.EVENT);
        };
    }

    private static void tickPolymorphs(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, PolymorphState>> iterator = POLYMORPHS.entrySet().iterator();
        while (iterator.hasNext()) {
            PolymorphState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity rawProxy = level.getEntity(state.proxyId);
            if (rawProxy instanceof Mob proxy && proxy.isAlive() && !proxy.isRemoved()) {
                state.lastPosition = proxy.position();
                state.lastYRot = proxy.getYRot();
                state.lastXRot = proxy.getXRot();
                if (now < state.expiresAt) continue;
                restorePolymorph(state, false);
                iterator.remove();
                continue;
            }
            restorePolymorph(state, true);
            iterator.remove();
        }
    }

    private static void restorePolymorph(PolymorphState state, boolean proxyBroken) {
        Entity rawOriginal = state.level.getEntity(state.originalId);
        if (!(rawOriginal instanceof Mob original)) return;
        Entity rawProxy = state.level.getEntity(state.proxyId);
        if (rawProxy instanceof Mob proxy && !proxy.isRemoved()) {
            state.lastPosition = proxy.position();
            state.lastYRot = proxy.getYRot();
            state.lastXRot = proxy.getXRot();
            proxy.discard();
        }
        restoreMobFlags(original, state.oldInvisible, state.oldInvulnerable, state.oldNoGravity,
                state.oldSilent, state.oldNoAi, state.oldNoPhysics);
        Vec3 pos = state.lastPosition == null ? state.anchor : state.lastPosition;
        original.snapTo(pos.x, pos.y, pos.z, state.lastYRot, state.lastXRot);
        original.setDeltaMovement(Vec3.ZERO);
        original.setHealth(proxyBroken ? Math.max(1.0F, state.originalHealth * .35F) : state.originalHealth);
        Entity rawOwner = state.level.getEntity(state.ownerId);
        if (rawOwner instanceof LivingEntity owner) WorldMagicService.cancelRelease(owner, "true_polymorph");
    }

    private static boolean maze(ServerPlayer player, CastTargetSnapshot snapshot) {
        Mob target = targetMob(player, snapshot);
        if (target == null) return false;
        MazeState existing = MAZES.remove(target.getUUID());
        if (existing != null) restoreMaze(existing);
        ServerLevel level = (ServerLevel) player.level();
        MazeState state = new MazeState(level, player.getUUID(), target.getUUID(), target.position(),
                target.getYRot(), target.getXRot(), level.getGameTime() + MAZE_TICKS,
                target.isInvisible(), target.isInvulnerable(), target.isNoGravity(), target.isSilent(),
                target.isNoAi(), target.noPhysics);
        MAZES.put(target.getUUID(), state);
        target.addTag("arcanecircle_maze_exile");
        stash(target);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS, 1.0F, .48F);
        ArcaneNoticeService.push(player, Component.literal("§5[미궁] §f" + target.getName().getString()
                + "을 24초간 전장에서 완전히 추방했습니다. §7귀환 뒤 6초 동안 미궁 후유증으로 방향감각과 전투력이 흔들립니다."), 100);
        return true;
    }

    private static void tickMazes(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, MazeState>> iterator = MAZES.entrySet().iterator();
        while (iterator.hasNext()) {
            MazeState state = iterator.next().getValue();
            if (state.level != level || now < state.expiresAt) continue;
            restoreMaze(state);
            iterator.remove();
        }
    }

    private static void restoreMaze(MazeState state) {
        Entity raw = state.level.getEntity(state.targetId);
        if (!(raw instanceof Mob target)) return;
        restoreMobFlags(target, state.oldInvisible, state.oldInvulnerable, state.oldNoGravity,
                state.oldSilent, state.oldNoAi, state.oldNoPhysics);
        target.removeTag("arcanecircle_maze_exile");
        target.snapTo(state.anchor.x, state.anchor.y, state.anchor.z, state.yRot, state.xRot);
        target.setDeltaMovement(Vec3.ZERO);
        target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 1, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 3, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 3, true, false));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, true, false));
        Entity rawOwner = state.level.getEntity(state.ownerId);
        if (rawOwner instanceof LivingEntity owner) WorldMagicService.cancelRelease(owner, "maze");
    }

    private static boolean etherealness(ServerPlayer player, double power) {
        ServerLevel level = (ServerLevel) player.level();
        EtherealState old = ETHEREAL.remove(player.getUUID());
        if (old != null) restoreEthereal(player, old);
        int duration = 360 + (int) Math.min(240.0, Math.max(0.0, power));
        EtherealState state = new EtherealState(level, player.getUUID(), level.getGameTime() + duration,
                player.getAbilities().mayfly, player.getAbilities().flying, player.noPhysics,
                player.isNoGravity(), player.isInvisible());
        ETHEREAL.put(player.getUUID(), state);
        applyEthereal(player);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration, 0, true, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration + 40, 0, true, false));
        ArcaneNoticeService.push(player, Component.literal("§b[에테르화] §f" + one(duration / 20.0)
                + "초 동안 물질 충돌을 벗어나 자유 비행하며 일반 피해의 88%를 위상 밖으로 흘립니다."), 100);
        return true;
    }

    private static void tickEthereal(ServerLevel level, long now) {
        Iterator<Map.Entry<UUID, EtherealState>> iterator = ETHEREAL.entrySet().iterator();
        while (iterator.hasNext()) {
            EtherealState state = iterator.next().getValue();
            if (state.level != level) continue;
            Entity raw = level.getEntity(state.playerId);
            if (!(raw instanceof ServerPlayer player) || !player.isAlive() || now >= state.expiresAt) {
                if (raw instanceof ServerPlayer player) restoreEthereal(player, state);
                iterator.remove();
                continue;
            }
            applyEthereal(player);
        }
    }

    private static void applyEthereal(ServerPlayer player) {
        player.noPhysics = true;
        player.setNoGravity(true);
        player.setInvisible(true);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    private static void restoreEthereal(ServerPlayer player, EtherealState state) {
        player.noPhysics = state.oldNoPhysics;
        player.setNoGravity(state.oldNoGravity);
        player.setInvisible(state.oldInvisible);
        player.getAbilities().mayfly = state.oldMayfly;
        player.getAbilities().flying = state.oldFlying && state.oldMayfly;
        player.onUpdateAbilities();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false));
        WorldMagicService.cancelRelease(player, "etherealness");
    }

    private static void restoreOwnedStates(UUID ownerId) {
        for (UUID id : POLYMORPHS.entrySet().stream()
                .filter(entry -> entry.getValue().ownerId.equals(ownerId)).map(Map.Entry::getKey).toList()) {
            PolymorphState state = POLYMORPHS.remove(id);
            if (state != null) restorePolymorph(state, false);
        }
        for (UUID id : MAZES.entrySet().stream()
                .filter(entry -> entry.getValue().ownerId.equals(ownerId)).map(Map.Entry::getKey).toList()) {
            MazeState state = MAZES.remove(id);
            if (state != null) restoreMaze(state);
        }
    }

    private static void stash(Mob mob) {
        mob.setNoAi(true);
        mob.noPhysics = true;
        mob.addTag("arcanecircle_high_utility_stashed");
        mob.setInvisible(true);
        mob.setInvulnerable(true);
        mob.setSilent(true);
        mob.setNoGravity(true);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.setPersistenceRequired();
    }

    private static void restoreMobFlags(Mob mob, boolean invisible, boolean invulnerable,
                                        boolean noGravity, boolean silent, boolean noAi, boolean noPhysics) {
        mob.noPhysics = noPhysics;
        mob.removeTag("arcanecircle_high_utility_stashed");
        mob.setInvisible(invisible);
        mob.setInvulnerable(invulnerable);
        mob.setNoGravity(noGravity);
        mob.setSilent(silent);
        mob.setNoAi(noAi);
    }

    private static Mob targetMob(ServerPlayer player, CastTargetSnapshot snapshot) {
        LivingEntity target = snapshot.targetEntity(player).orElse(null);
        if (!(target instanceof Mob mob) || !mob.isAlive() || mob.isRemoved()) return null;
        return POLYMORPHS.containsKey(mob.getUUID()) || MAZES.containsKey(mob.getUUID()) ? null : mob;
    }

    private static String one(double value) { return String.format(java.util.Locale.ROOT, "%.1f", value); }

    private static final class EtherealState {
        private final ServerLevel level; private final UUID playerId; private final long expiresAt;
        private final boolean oldMayfly, oldFlying, oldNoPhysics, oldNoGravity, oldInvisible;
        private EtherealState(ServerLevel level, UUID playerId, long expiresAt, boolean oldMayfly,
                              boolean oldFlying, boolean oldNoPhysics, boolean oldNoGravity, boolean oldInvisible) {
            this.level=level; this.playerId=playerId; this.expiresAt=expiresAt; this.oldMayfly=oldMayfly;
            this.oldFlying=oldFlying; this.oldNoPhysics=oldNoPhysics; this.oldNoGravity=oldNoGravity; this.oldInvisible=oldInvisible;
        }
        private boolean active() { return level.getGameTime() < expiresAt; }
    }

    private static final class CloneState {
        private final ServerLevel level; private final UUID ownerId, cloneId; private final long expiresAt;
        private CloneState(ServerLevel level, UUID ownerId, UUID cloneId, long expiresAt) {
            this.level=level; this.ownerId=ownerId; this.cloneId=cloneId; this.expiresAt=expiresAt;
        }
    }

    private static final class PolymorphState {
        private final ServerLevel level; private final UUID ownerId, originalId, proxyId; private final Vec3 anchor;
        private final float originalHealth; private final long expiresAt;
        private final boolean oldInvisible, oldInvulnerable, oldNoGravity, oldSilent, oldNoAi, oldNoPhysics;
        private Vec3 lastPosition; private float lastYRot, lastXRot;
        private PolymorphState(ServerLevel level, UUID ownerId, UUID originalId, UUID proxyId,
                               Vec3 anchor, float yRot, float xRot, float originalHealth, long expiresAt,
                               boolean oldInvisible, boolean oldInvulnerable, boolean oldNoGravity,
                               boolean oldSilent, boolean oldNoAi, boolean oldNoPhysics) {
            this.level=level; this.ownerId=ownerId; this.originalId=originalId; this.proxyId=proxyId; this.anchor=anchor;
            this.originalHealth=originalHealth; this.expiresAt=expiresAt; this.oldInvisible=oldInvisible;
            this.oldInvulnerable=oldInvulnerable; this.oldNoGravity=oldNoGravity; this.oldSilent=oldSilent;
            this.oldNoAi=oldNoAi; this.oldNoPhysics=oldNoPhysics; this.lastPosition=anchor; this.lastYRot=yRot; this.lastXRot=xRot;
        }
    }

    private static final class MazeState {
        private final ServerLevel level; private final UUID ownerId, targetId; private final Vec3 anchor;
        private final float yRot, xRot; private final long expiresAt;
        private final boolean oldInvisible, oldInvulnerable, oldNoGravity, oldSilent, oldNoAi, oldNoPhysics;
        private MazeState(ServerLevel level, UUID ownerId, UUID targetId, Vec3 anchor, float yRot, float xRot,
                          long expiresAt, boolean oldInvisible, boolean oldInvulnerable, boolean oldNoGravity,
                          boolean oldSilent, boolean oldNoAi, boolean oldNoPhysics) {
            this.level=level; this.ownerId=ownerId; this.targetId=targetId; this.anchor=anchor; this.yRot=yRot; this.xRot=xRot;
            this.expiresAt=expiresAt; this.oldInvisible=oldInvisible; this.oldInvulnerable=oldInvulnerable;
            this.oldNoGravity=oldNoGravity; this.oldSilent=oldSilent; this.oldNoAi=oldNoAi; this.oldNoPhysics=oldNoPhysics;
        }
    }
}
