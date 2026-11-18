package kr.moonseungjun.anomalousclasses.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.entity.PuppetEntity;
import kr.moonseungjun.anomalousclasses.network.payload.BarrierInputPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.animal.TamableAnimal;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Three independent, server-authoritative cylindrical battlefields.
 * Presentation contracts are separate; no opaque client coordinates can deal damage.
 */
public final class BarrierCasterService {
    private static final String BOLT_TAG = "anomalousclasses_barrier_bolt";
    private static final int BOLT_ABILITY = 3, FLASH_ABILITY = 4, AMPLIFY_ABILITY = 5;
    private static final Map<UUID, Runtime> LIVE = new HashMap<>();
    private static MinecraftServer currentServer;
    private BarrierCasterService() {}

    private static final class Runtime {
        BarrierRules.Kind selectedKind = BarrierRules.Kind.DEFENSE;
        BarrierRules.Size selectedSize = BarrierRules.Size.MEDIUM;
        boolean preview;
        final List<Boundary> barriers = new ArrayList<>();
        final Map<UUID, Long> lastCompressionHit = new HashMap<>();
        FlashWall flash;
    }
    private static final class Boundary {
        final UUID id = UUID.randomUUID(), owner;
        final BarrierRules.Kind kind;
        final BarrierRules.Size size;
        final ServerLevel world;
        final Vec3 base;
        final int levelAtCast;
        final double integrityMax;
        final double[] compressionStages = new double[4];
        final double linkFactor;
        final long started, expires;
        double integrity;
        long amplifiedUntil;
        int nextCompressionStage;

        Boundary(ServerPlayer player, BarrierRules.Kind kind, BarrierRules.Size size,
                 Vec3 base, PlayerClassProfile profile, long tick) {
            this.owner = player.getUUID();
            this.kind = kind; this.size = size; this.world = player.serverLevel();
            this.base = base; this.started = tick; this.levelAtCast = profile.progress(ClassId.BARRIER_CASTER).level();
            this.linkFactor = 1.0 + .004 * BarrierRules.rank(profile, "barrier_link");
            for (int i = 0; i < compressionStages.length; i++)
                compressionStages[i] = BarrierRules.compression(i, size, levelAtCast, profile, false, false);
            this.expires = tick + BarrierRules.lifetime(kind, profile);
            this.integrityMax = BarrierRules.integrity(kind, size, levelAtCast, profile);
            this.integrity = integrityMax;
        }
        AABB bounds() {
            return new AABB(base.x - size.radius, base.y, base.z - size.radius,
                    base.x + size.radius, base.y + 4.0, base.z + size.radius);
        }
        boolean active(long now) { return (kind == BarrierRules.Kind.COMPRESSION ? now <= expires : now < expires)
                && (kind == BarrierRules.Kind.COMPRESSION || integrity > 0); }
        boolean amplified(long now) { return now < amplifiedUntil; }
    }
    private static final class FlashWall {
        final ServerLevel world;
        final Vec3 center, normal;
        final long expires;
        double integrity;
        FlashWall(ServerPlayer player, double integrity, long now) {
            this.world = player.serverLevel();
            this.normal = player.getLookAngle().normalize();
            this.center = player.getEyePosition().add(normal.scale(1.5));
            this.expires = now + 60;
            this.integrity = integrity;
        }
    }

    private static Runtime live(ServerPlayer player) {
        if (currentServer != player.getServer()) {
            LIVE.clear();
            currentServer = player.getServer();
        }
        return LIVE.computeIfAbsent(player.getUUID(), ignored -> new Runtime());
    }
    public static boolean current(ServerPlayer player) {
        return player.getServer() != null && player.isAlive()
                && ClassProfileSavedData.get(player.getServer()).profile(player.getUUID())
                .currentClass().filter(id -> id == ClassId.BARRIER_CASTER).isPresent();
    }
    private static void say(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable(key, args), true);
    }

    public static void input(ServerPlayer player, int action) {
        if (!current(player) || action < 0 || action > BarrierInputPayload.CANCEL_PREVIEW) return;
        Runtime runtime = live(player);
        switch (action) {
            case BarrierInputPayload.PREVIEW_OR_PLACE -> {
                if (!runtime.preview) {
                    runtime.preview = true;
                    say(player, "message.anomalousclasses.barrier.preview",
                            label(runtime.selectedKind), (int) runtime.selectedSize.radius);
                } else {
                    if (place(player, runtime.selectedKind, runtime.selectedSize)) runtime.preview = false;
                }
            }
            case BarrierInputPayload.CYCLE_KIND -> {
                runtime.selectedKind = BarrierRules.Kind.values()[
                        (runtime.selectedKind.ordinal() + 1) % BarrierRules.Kind.values().length];
                runtime.preview = true;
                say(player, "message.anomalousclasses.barrier.preview",
                        label(runtime.selectedKind), (int) runtime.selectedSize.radius);
            }
            case BarrierInputPayload.CYCLE_SIZE -> {
                runtime.selectedSize = BarrierRules.Size.values()[
                        (runtime.selectedSize.ordinal() + 1) % BarrierRules.Size.values().length];
                runtime.preview = true;
                say(player, "message.anomalousclasses.barrier.preview",
                        label(runtime.selectedKind), (int) runtime.selectedSize.radius);
            }
            case BarrierInputPayload.REPEAT -> {
                BarrierSavedData saved = BarrierSavedData.get(player.getServer());
                BarrierRules.Kind kind = saved.lastKind(player.getUUID());
                BarrierRules.Size size = saved.lastSize(player.getUUID());
                if (kind == null || size == null)
                    say(player, "message.anomalousclasses.barrier.no_recent");
                else place(player, kind, size);
            }
            case BarrierInputPayload.BOLT -> bolt(player);
            case BarrierInputPayload.FLASH -> flash(player);
            case BarrierInputPayload.AMPLIFY -> amplify(player);
            case BarrierInputPayload.CANCEL_NEAREST -> cancelNearest(player);
            case BarrierInputPayload.CANCEL_PREVIEW -> runtime.preview = false;
            default -> { }
        }
    }
    private static Component label(BarrierRules.Kind kind) {
        return Component.translatable("message.anomalousclasses.barrier.kind."
                + kind.name().toLowerCase(java.util.Locale.ROOT));
    }
    private static long ms() { return System.currentTimeMillis(); }
    private static long tick(MinecraftServer server) { return server.getTickCount(); }
    private static boolean ready(ServerPlayer player, int ability) {
        long remaining = BarrierSavedData.get(player.getServer()).readyAt(player.getUUID(), ability) - ms();
        if (remaining > 0L) {
            say(player, "message.anomalousclasses.barrier.cooldown", (remaining + 999L) / 1000L);
            return false;
        }
        return true;
    }
    private static void cooldown(ServerPlayer player, int ability, int ticks,
                                 BarrierRules.Kind kind, BarrierRules.Size size) {
        BarrierSavedData.get(player.getServer()).cast(player.getUUID(), ability,
                ms() + ticks * 50L, kind, size);
    }

    private static Vec3 aim(ServerPlayer player, double reach) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(reach));
        BlockHitResult hit = player.serverLevel().clip(new ClipContext(start, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 location = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : end;
        return new Vec3(location.x, Math.floor(location.y), location.z);
    }
    private static boolean validLocation(ServerLevel world, Vec3 place) {
        BlockPos pos = BlockPos.containing(place);
        return world.hasChunkAt(pos) && world.getWorldBorder().isWithinBounds(pos)
                && pos.getY() >= world.getMinBuildHeight() && pos.getY() + 4 < world.getMaxBuildHeight();
    }
    private static boolean place(ServerPlayer player, BarrierRules.Kind kind, BarrierRules.Size size) {
        Runtime runtime = live(player);
        runtime.barriers.removeIf(b -> !b.active(tick(player.getServer())));
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        if (runtime.barriers.size() >= BarrierRules.capacity(profile)) {
            say(player, "message.anomalousclasses.barrier.capacity");
            return false;
        }
        if (!ready(player, kind.ordinal())) return false;
        Vec3 point = aim(player, BarrierRules.reach(profile));
        if (!validLocation(player.serverLevel(), point)) {
            say(player, "message.anomalousclasses.barrier.invalid_location");
            return false;
        }
        for (Boundary other : runtime.barriers) {
            if (other.world == player.serverLevel() && other.kind == kind) {
                double dx = other.base.x - point.x, dz = other.base.z - point.z;
                double proximity = Math.min(other.size.radius, size.radius) * .60;
                if (dx * dx + dz * dz < proximity * proximity && Math.abs(other.base.y - point.y) < 3) {
                    say(player, "message.anomalousclasses.barrier.overlap");
                    return false;
                }
            }
        }
        Boundary boundary = new Boundary(player, kind, size, point, profile, tick(player.getServer()));
        runtime.barriers.add(boundary);
        cooldown(player, kind.ordinal(), kind.cooldown, kind, size);
        say(player, "message.anomalousclasses.barrier.placed", label(kind), (int) size.radius);
        // Only first successful use of each kind is a class-progression milestone.
        RpgExperienceService.awardFirst(player, "barrier_first:" + kind.name(), 650L);
        return true;
    }
    private static void bolt(ServerPlayer player) {
        if (!ready(player, BOLT_ABILITY)) return;
        Snowball bolt = new Snowball(player.serverLevel(), player);
        bolt.getPersistentData().putBoolean(BOLT_TAG, true);
        Vec3 origin = player.getEyePosition();
        bolt.getPersistentData().putDouble("barrier_x", origin.x);
        bolt.getPersistentData().putDouble("barrier_y", origin.y);
        bolt.getPersistentData().putDouble("barrier_z", origin.z);
        bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 2.2F, 0);
        if (!player.serverLevel().addFreshEntity(bolt)) return;
        cooldown(player, BOLT_ABILITY, BarrierRules.BOLT_COOLDOWN, null, null);
    }
    public static void onProjectile(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Snowball bolt) || bolt.level().isClientSide()
                || !bolt.getPersistentData().getBoolean(BOLT_TAG)) return;
        var tag = bolt.getPersistentData();
        Vec3 start = new Vec3(tag.getDouble("barrier_x"), tag.getDouble("barrier_y"), tag.getDouble("barrier_z"));
        if (bolt.position().distanceToSqr(start) <= 14.0 * 14.0
                && bolt.getOwner() instanceof ServerPlayer owner && current(owner)
                && event.getRayTraceResult() instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity target
                && validEnemy(owner, target)) {
            PlayerClassProfile profile = ClassProfileSavedData.get(owner.getServer()).profile(owner.getUUID());
            float damage = (float) Math.min(4_000, 5.0 * BarrierRules.power(
                    profile.progress(ClassId.BARRIER_CASTER).level()));
            target.hurt(owner.damageSources().source(DamageTypes.MAGIC, owner), damage);
        }
        event.setCanceled(true);
        bolt.discard();
    }
    private static void flash(ServerPlayer player) {
        if (!ready(player, FLASH_ABILITY)) return;
        Runtime runtime = live(player);
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        runtime.flash = new FlashWall(player, BarrierRules.flashIntegrity(
                profile.progress(ClassId.BARRIER_CASTER).level(), profile), tick(player.getServer()));
        cooldown(player, FLASH_ABILITY, BarrierRules.FLASH_COOLDOWN, null, null);
        say(player, "message.anomalousclasses.barrier.flash");
    }
    private static void amplify(ServerPlayer player) {
        if (!ready(player, AMPLIFY_ABILITY)) return;
        Runtime runtime = live(player);
        long now = tick(player.getServer());
        int applied = 0;
        for (Boundary b : runtime.barriers) {
            if (b.world == player.serverLevel() && b.active(now) && b.amplifiedUntil <= now) {
                b.amplifiedUntil = Math.min(b.expires, now + BarrierRules.AMPLIFY_TICKS);
                applied++;
            }
        }
        if (applied == 0) {
            say(player, "message.anomalousclasses.barrier.no_active");
            return;
        }
        cooldown(player, AMPLIFY_ABILITY, BarrierRules.AMPLIFY_COOLDOWN, null, null);
        say(player, "message.anomalousclasses.barrier.amplified", applied);
    }
    private static void cancelNearest(ServerPlayer player) {
        Runtime runtime = live(player);
        Vec3 point = aim(player, 16.0);
        Boundary closest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (Boundary b : runtime.barriers) {
            if (b.world != player.serverLevel()) continue;
            double d = b.base.distanceToSqr(point);
            if (d < (b.size.radius + 2.0) * (b.size.radius + 2.0) && d < distance) {
                closest = b; distance = d;
            }
        }
        if (closest == null) {
            say(player, "message.anomalousclasses.barrier.no_active");
            return;
        }
        runtime.barriers.remove(closest);
        say(player, "message.anomalousclasses.barrier.dismissed");
    }

    private static boolean validEnemy(ServerPlayer player, LivingEntity entity) {
        return entity instanceof Enemy && !(entity instanceof PuppetEntity)
                && MentalDominatorService.isValidCombatTarget(player, entity)
                && ParasiteSavedData.get(player.getServer()).ownerOf(entity.getUUID()) == null;
    }
    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (currentServer != server) {
            LIVE.clear(); currentServer = server;
        }
        long time = tick(server);
        Iterator<Map.Entry<UUID, Runtime>> it = LIVE.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null || !current(owner)) {
                it.remove();
                continue;
            }
            Runtime runtime = entry.getValue();
            runtime.barriers.removeIf(boundary ->
                    boundary.world != owner.serverLevel() || !boundary.active(time));
            if (runtime.flash != null && (runtime.flash.expires <= time
                    || runtime.flash.integrity <= 0 || runtime.flash.world != owner.serverLevel()))
                runtime.flash = null;
            for (Boundary boundary : runtime.barriers) {
                if (boundary.kind == BarrierRules.Kind.COMPRESSION) compress(boundary, owner, runtime, time);
                if (boundary.kind == BarrierRules.Kind.ISOLATION) isolate(boundary, owner, time);
            }
            runtime.lastCompressionHit.entrySet().removeIf(e -> time - e.getValue() > 120L);
        }
    }
    private static void compress(Boundary b, ServerPlayer owner, Runtime runtime, long now) {
        while (b.nextCompressionStage < BarrierRules.COMPRESSION_TICKS.length
                && now - b.started >= BarrierRules.COMPRESSION_TICKS[b.nextCompressionStage]) {
            int stage = b.nextCompressionStage++;
            for (Mob target : b.world.getEntitiesOfClass(Mob.class, b.bounds().inflate(.5),
                    mob -> validEnemy(owner, mob)
                            && BarrierRules.overlaps(mob.getBoundingBox(), b.base, b.size.radius))) {
                if (now - runtime.lastCompressionHit.getOrDefault(target.getUUID(), -10_000L) < 12L) continue;
                boolean isolated = runtime.barriers.stream().anyMatch(other ->
                        other.kind == BarrierRules.Kind.ISOLATION && other.world == b.world
                        && other.active(now) && BarrierRules.overlaps(target.getBoundingBox(),
                        other.base, other.size.radius));
                float damage = (float) Math.min(4_000, b.compressionStages[stage]
                        * (isolated ? b.linkFactor : 1.0) * (b.amplified(now) ? 1.35 : 1.0));
                if (target.hurt(owner.damageSources().source(DamageTypes.MAGIC, owner), damage)) {
                    runtime.lastCompressionHit.put(target.getUUID(), now);
                    CombatTracker.markOwnedCombat(owner);
                    RpgExperienceService.awardFirst(owner, "barrier_compression_hit", 450L);
                }
            }
        }
    }
    private static void isolate(Boundary b, ServerPlayer owner, long now) {
        for (Mob mob : b.world.getEntitiesOfClass(Mob.class, b.bounds().inflate(2.0),
                target -> validEnemy(owner, target))) {
            Vec3 from = new Vec3(mob.xo, mob.yo, mob.zo);
            Vec3 to = mob.position();
            if (!BarrierRules.crossing(from, to, b.base, b.size.radius)) continue;
            // A strong boss breaks the boundary quickly but is never permanently pinned.
            boolean boss = mob.getMaxHealth() >= 80.0F;
            b.integrity -= (boss ? 26.0 : 4.0) / (b.amplified(now) ? 1.35 : 1.0);
            if (!boss && b.integrity > 0 && validLocation(b.world, from)) {
                mob.teleportTo(from.x, from.y, from.z);
                mob.getNavigation().stop();
            }
            if (b.integrity <= 0.0) break;
        }
    }
    private static boolean protective(ServerPlayer owner, LivingEntity victim) {
        if (victim == owner || owner.isAlliedTo(victim) || victim instanceof AbstractVillager)
            return true;
        if (victim instanceof TamableAnimal tamed && owner.getUUID().equals(tamed.getOwnerUUID()))
            return true;
        if (victim instanceof PuppetEntity puppet && owner.getUUID().equals(puppet.puppetOwner()))
            return true;
        MentalDominatorSavedData.ControlledRecord controlled =
                MentalDominatorSavedData.get(owner.getServer()).controlled(victim.getUUID());
        return controlled != null && owner.getUUID().equals(controlled.ownerId());
    }
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || victim.level().getServer() == null
                || event.getAmount() <= 0.0F) return;
        DamageSource damage = event.getSource();
        Entity cause = damage.getEntity() != null ? damage.getEntity() : damage.getDirectEntity();
        if (cause == null || cause == victim || cause.level() != victim.level()) return;
        Vec3 from = cause.getEyePosition();
        Vec3 to = victim.getEyePosition();
        MinecraftServer server = victim.level().getServer();
        if (currentServer != server) return;
        long now = tick(server);
        for (var entry : LIVE.entrySet()) {
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null || !current(owner) || owner.level() != victim.level()
                    || !protective(owner, victim) || owner.isAlliedTo(cause)) continue;
            Runtime runtime = entry.getValue();
            FlashWall flash = runtime.flash;
            if (flash != null && flash.world == victim.level() && flash.expires > now
                    && flash.integrity > 0.0) {
                double front = from.subtract(flash.center).dot(flash.normal);
                double back = to.subtract(flash.center).dot(flash.normal);
                if (front > 0 && back < 0) {
                    Vec3 cut = from.lerp(to, front / (front - back));
                    if (cut.distanceToSqr(flash.center) <= 2.2 * 2.2) {
                        float amount = event.getAmount();
                        float remaining = blockDamage(event, flash.integrity, cause);
                        flash.integrity -= amount - remaining;
                        return;
                    }
                }
            }
            for (Boundary b : runtime.barriers) {
                if (b.kind != BarrierRules.Kind.DEFENSE || b.world != victim.level()
                        || !b.active(now) || !BarrierRules.inside(to, b.base, b.size.radius)
                        || BarrierRules.inside(from, b.base, b.size.radius)) continue;
                float amount = event.getAmount();
                double efficiency = b.amplified(now) ? 1.35 : 1.0;
                float remaining = blockDamage(event, b.integrity * efficiency, cause);
                b.integrity -= (amount - remaining) / efficiency;
                return; // One chosen boundary takes one hit, never stacked shields.
            }
        }
    }
    private static float blockDamage(LivingIncomingDamageEvent event, double available, Entity source) {
        float initial = event.getAmount();
        float maxBlocked = initial;
        if (source instanceof Mob mob && mob.getMaxHealth() >= 80.0F)
            maxBlocked *= .60F; // Bosses can penetrate even an intact shield.
        float blocked = (float) Math.min(maxBlocked, Math.max(0.0, available));
        float left = Math.max(0, initial - blocked);
        if (left <= 1.0e-4F) event.setCanceled(true);
        else event.setAmount(left);
        return left;
    }
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LIVE.remove(player.getUUID());
    }
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LIVE.remove(event.getEntity().getUUID());
    }
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        LIVE.remove(event.getEntity().getUUID());
    }
    public static void prepareClassChange(ServerPlayer player, ClassId previous) {
        if (previous == ClassId.BARRIER_CASTER) LIVE.remove(player.getUUID());
    }
}
