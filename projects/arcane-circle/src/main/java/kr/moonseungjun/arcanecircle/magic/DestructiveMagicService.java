package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Server-authoritative terrain rupture for physically destructive spells.
 *
 * Alpha.47 makes the physical footprint obey the presented spell footprint. Large catastrophe
 * spells are no longer squeezed into a small fixed crater and then silently clipped by the
 * per-tick edit budget. The visible footprint is tiled into bounded rupture cells and those cells
 * are committed over subsequent ticks. Global scan/change/drop budgets still protect the server.
 */
public final class DestructiveMagicService {
    public enum TerrainClass { MAJOR, CONDITIONAL, NONE }

    private static final Set<String> MAJOR = Set.of(
            "disintegrate", "delayed_blast_fireball", "fire_storm", "earthquake",
            "meteor_swarm", "world_sunder", "arcane_annihilation");
    private static final Set<String> CONDITIONAL = Set.of(
            "fireball", "shatter", "flame_strike", "meteor_shard", "move_earth",
            "lightning_bolt", "thunderwave", "gust_of_wind");

    // Hard server safety budgets. Catastrophes now continue next tick instead of disappearing here.
    private static final int MAX_BLOCK_CHANGES_PER_TICK = 720;
    private static final int MAX_BLOCK_SCANS_PER_TICK = 48_000;
    private static final int MAX_DROPPED_BLOCKS_PER_TICK = 96;
    private static final int MAX_PENDING_CELLS_PER_LEVEL = 2_048;
    private static final int MAX_CELLS_PER_TICK = 7;

    private static final Map<ServerLevel, TickBudget> BUDGETS = new WeakHashMap<>();
    private static final Map<ServerLevel, Deque<RuptureTask>> PENDING = new WeakHashMap<>();
    private static final Map<ServerLevel, Long> LAST_SCHEDULER_TICK = new WeakHashMap<>();

    private record Candidate(BlockPos pos, double overload) {}
    /** cellRadius is the maximum radius scanned by one bounded world-edit cell. */
    private record Profile(double radiusScale, double baseEnergy, int baseBlocks, boolean drops,
                           double cellRadius, double verticalScale, double maxFootprint) {}
    private record RuptureTask(UUID ownerId, String spellId, Vec3 center, double radius,
                               double power, long dueTick) {}

    private static final class TickBudget {
        private long tick = Long.MIN_VALUE;
        private int changes;
        private int scans;
        private int droppedBlocks;

        void reset(long currentTick) {
            if (tick == currentTick) return;
            tick = currentTick;
            changes = 0;
            scans = 0;
            droppedBlocks = 0;
        }

        boolean scanAvailable() { return scans < MAX_BLOCK_SCANS_PER_TICK; }
        int changesRemaining() { return Math.max(0, MAX_BLOCK_CHANGES_PER_TICK - changes); }
        int dropChangesRemaining() { return Math.max(0, MAX_DROPPED_BLOCKS_PER_TICK - droppedBlocks); }
        void scanned() { scans++; }
        void changed(boolean drops) {
            changes++;
            if (drops) droppedBlocks++;
        }
    }

    private DestructiveMagicService() {}

    public static TerrainClass classification(String spellId) {
        if (MAJOR.contains(spellId)) return TerrainClass.MAJOR;
        if (CONDITIONAL.contains(spellId)) return TerrainClass.CONDITIONAL;
        return TerrainClass.NONE;
    }

    /**
     * Executes the dense core now and schedules the rest of an oversized visible footprint.
     * Callers therefore do not need spell-specific hard caps just to stay within one server tick.
     */
    public static int impact(ServerPlayer player, String spellId, Vec3 center,
                             double requestedRadius, double power) {
        Profile profile = profile(spellId);
        if (profile == null || center == null) return 0;
        ServerLevel level = (ServerLevel) player.level();
        double footprint = Math.max(.75, Math.min(profile.maxFootprint(),
                Math.max(.75, requestedRadius) * profile.radiusScale()));
        double core = Math.min(footprint, profile.cellRadius());
        int changed = impactNow(player, spellId, center, core, power);
        if (footprint > profile.cellRadius() * 1.08) {
            scheduleFootprint(level, player.getUUID(), spellId, center, footprint, power, profile);
        }
        return changed;
    }

    /** Process queued catastrophe cells once per level/tick. Safe to call from every player tick. */
    public static void tick(ServerLevel level) {
        if (level == null) return;
        long now = level.getGameTime();
        Long previous = LAST_SCHEDULER_TICK.put(level, now);
        if (previous != null && previous == now) return;
        Deque<RuptureTask> queue = PENDING.get(level);
        if (queue == null || queue.isEmpty()) return;
        TickBudget budget = budget(level);
        int processed = 0;
        Iterator<RuptureTask> iterator = queue.iterator();
        while (iterator.hasNext() && processed < MAX_CELLS_PER_TICK
                && budget.scanAvailable() && budget.changesRemaining() > 0) {
            RuptureTask task = iterator.next();
            if (task.dueTick() > now) continue;
            Entity raw = level.getEntity(task.ownerId());
            iterator.remove();
            if (!(raw instanceof ServerPlayer owner) || !owner.isAlive()) continue;
            impactNow(owner, task.spellId(), task.center(), task.radius(), task.power());
            processed++;
        }
        if (queue.isEmpty()) PENDING.remove(level);
    }

    public static void clearAll() {
        PENDING.clear();
        BUDGETS.clear();
        LAST_SCHEDULER_TICK.clear();
    }

    /** Narrow line damage for lightning/disintegrate style spells. */
    public static int ray(ServerPlayer player, String spellId, Vec3 start, Vec3 end, double power) {
        if (start == null || end == null) return 0;
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < .05) return 0;
        Vec3 unit = delta.scale(1.0 / length);
        int changed = 0;
        int samples = Math.min(128, Math.max(1, (int) Math.ceil(length / .70)));
        for (int i = 1; i <= samples; i++) {
            Vec3 at = start.add(unit.scale(length * i / (double) samples));
            changed += impact(player, spellId, at, 1.18, power);
            if (changed >= 150 || budget((ServerLevel) player.level()).changesRemaining() <= 0) break;
        }
        return changed;
    }

    /**
     * 8C Earthquake owns the whole battlefield footprint. The epicenter collapses first and the
     * outer faults arrive over subsequent ticks instead of being dropped after the first 720 edits.
     */
    public static int quakeField(ServerPlayer player, Vec3 center, double requestedRadius, double power) {
        if (center == null) return 0;
        ServerLevel level = (ServerLevel) player.level();
        double field = Math.max(11.0, Math.min(56.0, requestedRadius));
        int changed = impact(player, "earthquake", center.add(0, -.35, 0), field, power * 1.14);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2.0 * i / 12.0 + Math.sin(i * 1.91) * .15;
            double d = field * (.38 + .038 * (i % 4));
            Vec3 at = center.add(Math.cos(a) * d, -.22 - .08 * (i % 3), Math.sin(a) * d);
            queue(level, new RuptureTask(player.getUUID(), "earthquake", at,
                    Math.min(9.5, Math.max(4.0, field * (.16 + .012 * (i % 3)))),
                    power * (.68 + .045 * (i % 4)), level.getGameTime() + 1 + i / 2));
        }
        return changed;
    }

    /**
     * A visible Meteor Swarm body now owns a crater proportional to that body. Core, deep bowl and
     * fractured rim are spread over a handful of ticks so a large meteor is not reduced to a pinhole.
     */
    public static int meteorCrater(ServerPlayer player, Vec3 center, double radius, double power) {
        if (center == null) return 0;
        ServerLevel level = (ServerLevel) player.level();
        double r = Math.max(4.2, Math.min(20.0, radius * 1.58));
        int changed = impact(player, "meteor_swarm", center.add(0, -.45, 0), r, power * 1.18);
        queue(level, new RuptureTask(player.getUUID(), "meteor_swarm", center.add(0, -2.1, 0),
                Math.min(10.0, r * .68), power * 1.08, level.getGameTime() + 1));
        for (int i = 0; i < 7; i++) {
            double a = i * Math.PI * 2.0 / 7.0 + .41;
            Vec3 rim = center.add(Math.cos(a) * r * .52, -.24 - .10 * (i % 2), Math.sin(a) * r * .52);
            queue(level, new RuptureTask(player.getUUID(), "meteor_swarm", rim,
                    Math.min(7.5, Math.max(2.8, r * .34)), power * (.67 + .035 * (i % 3)),
                    level.getGameTime() + 2 + i / 2));
        }
        return changed;
    }

    /**
     * Arcane Annihilation deletes a corridor progressively along the visible beam. Thickness grows
     * with power instead of being locked to the old 3.75-block bore.
     */
    public static int annihilationCorridor(ServerPlayer player, Vec3 start, Vec3 end, double power) {
        if (start == null || end == null) return 0;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0) return 0;
        Vec3 unit = delta.scale(1.0 / length);
        double bore = Math.max(2.15, Math.min(7.0, 1.9 + Math.sqrt(Math.max(.1, power)) * .30));
        double first = Math.min(length, 2.8); // never erase the block directly under the caster.
        double spacing = Math.max(1.65, bore * .76);
        int changed = 0;
        int serial = 0;
        for (double d = first; d <= length + .001; d += spacing) {
            double t = d / Math.max(1.0, length);
            Vec3 at = start.add(unit.scale(d));
            double radius = bore * (.82 + .22 * t);
            double localPower = power * (.86 + .19 * t);
            if (serial == 0) changed += impactNow(player, "arcane_annihilation", at, radius, localPower);
            else queue(level, new RuptureTask(player.getUUID(), "arcane_annihilation", at, radius,
                    localPower, level.getGameTime() + serial / 4));
            serial++;
        }
        return changed;
    }

    /**
     * World Sunder is a travelling continental cut. Its length follows range up to a generous
     * safety ceiling and the fracture nodes execute in sequence so the visible crack and terrain agree.
     */
    public static int fissure(ServerPlayer player, String spellId, Vec3 center,
                              Vec3 direction, double range, double power) {
        if (center == null) return 0;
        ServerLevel level = (ServerLevel) player.level();
        Vec3 flat = direction == null ? Vec3.ZERO : new Vec3(direction.x, 0.0, direction.z);
        if (flat.lengthSqr() < 1.0E-8) flat = new Vec3(0.0, 0.0, 1.0);
        flat = flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        double halfLength = Math.max(14.0, Math.min(72.0, range * .55));
        int points = Math.max(13, Math.min(31, (int) Math.ceil(halfLength * 2.0 / 3.8)));
        int changed = 0;
        for (int i = 0; i < points; i++) {
            double t = points <= 1 ? 0.0 : -1.0 + 2.0 * i / (points - 1.0);
            double weight = 1.0 - Math.abs(t);
            double wobble = Math.sin(i * 1.41) * (1.20 + weight * 1.85);
            Vec3 at = center.add(flat.scale(t * halfLength)).add(right.scale(wobble))
                    .add(0, -.32 - weight * 1.05, 0);
            double radius = 3.5 + weight * 5.4;
            double localPower = power * (.70 + weight * .50);
            if (i == points / 2) changed += impactNow(player, spellId, at, radius, localPower);
            else queue(level, new RuptureTask(player.getUUID(), spellId, at, radius, localPower,
                    level.getGameTime() + 1 + Math.abs(i - points / 2) / 2));
        }
        return changed;
    }

    public static void applyPhysicalAftermath(ServerPlayer player, String spellId,
                                              CastTargetSnapshot snapshot, double range, double power) {
        if (snapshot == null || !snapshot.validFor(player)) return;
        switch (spellId) {
            case "lightning_bolt" -> ray(player, "lightning_bolt",
                    snapshot.launchOrigin(), snapshot.target(), power * .72);
            case "thunderwave" -> ray(player, "thunderwave",
                    snapshot.launchOrigin(), snapshot.target(), power * .52);
            case "gust_of_wind" -> ray(player, "gust_of_wind",
                    snapshot.launchOrigin(), snapshot.target(), power * .38);
            case "world_sunder" -> fissure(player, "world_sunder", snapshot.target(),
                    snapshot.launchDirection(), range, power);
            default -> { }
        }
    }

    private static int impactNow(ServerPlayer player, String spellId, Vec3 center,
                                 double exactRadius, double power) {
        Profile profile = profile(spellId);
        if (profile == null || center == null) return 0;
        ServerLevel level = (ServerLevel) player.level();
        TickBudget budget = budget(level);
        if (!budget.scanAvailable() || budget.changesRemaining() <= 0) return 0;
        double radius = Math.max(.75, Math.min(profile.cellRadius(), exactRadius));
        double area = Math.max(1.0, radius * radius / Math.max(1.0, profile.cellRadius() * profile.cellRadius() * .42));
        int desired = Math.max(profile.baseBlocks(), (int) Math.ceil(profile.baseBlocks() * Math.min(2.6, area)));
        int changeLimit = Math.min(desired, budget.changesRemaining());
        if (profile.drops()) changeLimit = Math.min(changeLimit, budget.dropChangesRemaining());
        if (changeLimit <= 0) return 0;

        double powerScale = .72 + .28 * Math.sqrt(Math.max(.1, power));
        double footprintScale = .90 + .10 * Math.sqrt(Math.max(.4, radius / Math.max(.75, profile.cellRadius())));
        double energy = profile.baseEnergy() * powerScale * footprintScale;
        int bound = (int) Math.ceil(radius);
        double vertical = Math.max(1.15, radius * profile.verticalScale());
        List<Candidate> candidates = new ArrayList<>();
        BlockPos origin = BlockPos.containing(center);

        scan:
        for (int x = -bound; x <= bound; x++) {
            for (int z = -bound; z <= bound; z++) {
                for (int y = -(int) Math.ceil(vertical); y <= Math.ceil(vertical); y++) {
                    double nx = x / radius, nz = z / radius, ny = y / vertical;
                    double normalized = Math.sqrt(nx * nx + nz * nz + ny * ny);
                    if (normalized > 1.0) continue;
                    if (!budget.scanAvailable()) break scan;
                    budget.scanned();
                    BlockPos pos = origin.offset(x, y, z);
                    if (!level.hasChunkAt(pos)) continue;
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()) continue;
                    float hardness = state.getDestroySpeed(level, pos);
                    if (hardness < 0) continue;
                    float blast = Math.max(0F, state.getBlock().getExplosionResistance());
                    if (blast >= 1000F) continue;
                    double strength = 1.0 + Math.max(0, hardness) * 2.6 + Math.sqrt(blast) * 1.45;
                    double falloff = Math.pow(Math.max(0.0, 1.0 - normalized), .70);
                    double local = energy * (.20 + .80 * falloff);
                    if (local < strength) continue;
                    candidates.add(new Candidate(pos, local / Math.max(.25, strength)));
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(Candidate::overload).reversed());
        int changed = 0;
        for (Candidate candidate : candidates) {
            if (changed >= changeLimit || budget.changesRemaining() <= 0) break;
            if (profile.drops() && budget.dropChangesRemaining() <= 0) break;
            if (!level.hasChunkAt(candidate.pos())) continue;
            if (level.destroyBlock(candidate.pos(), profile.drops(), player)) {
                changed++;
                budget.changed(profile.drops());
            }
        }
        return changed;
    }

    private static void scheduleFootprint(ServerLevel level, UUID ownerId, String spellId, Vec3 center,
                                          double footprint, double power, Profile profile) {
        double cell = Math.max(2.2, profile.cellRadius() * .88);
        int rings = Math.max(1, (int) Math.ceil((footprint - profile.cellRadius()) / Math.max(2.0, cell * 1.25)));
        int serial = 0;
        for (int ring = 1; ring <= rings; ring++) {
            double q = ring / (double) rings;
            double rr = profile.cellRadius() + (footprint - profile.cellRadius()) * q;
            int points = Math.max(6, Math.min(40,
                    (int) Math.ceil(Math.PI * 2.0 * rr / Math.max(3.0, cell * 1.55))));
            for (int i = 0; i < points; i++) {
                double a = Math.PI * 2.0 * i / points + ring * .37;
                double jitter = .92 + .08 * Math.sin((ring * 31 + i * 17) * .61);
                Vec3 at = center.add(Math.cos(a) * rr * jitter, 0.0, Math.sin(a) * rr * jitter);
                double localRadius = Math.min(profile.cellRadius(), cell * (.84 + .12 * (1.0 - q)));
                double localPower = power * (.96 - .34 * q);
                queue(level, new RuptureTask(ownerId, spellId, at, localRadius, localPower,
                        level.getGameTime() + 1 + serial / 6));
                serial++;
            }
        }
    }

    private static void queue(ServerLevel level, RuptureTask task) {
        Deque<RuptureTask> queue = PENDING.computeIfAbsent(level, ignored -> new ArrayDeque<>());
        if (queue.size() >= MAX_PENDING_CELLS_PER_LEVEL) queue.removeFirst();
        queue.addLast(task);
    }

    private static TickBudget budget(ServerLevel level) {
        TickBudget budget = BUDGETS.computeIfAbsent(level, ignored -> new TickBudget());
        budget.reset(level.getGameTime());
        return budget;
    }

    private static Profile profile(String id) {
        return switch (id) {
            case "fireball" -> new Profile(.82, 8.5, 105, false, 6.5, .58, 12.0);
            case "shatter" -> new Profile(.80, 9.5, 105, true, 5.8, .46, 11.0);
            case "flame_strike" -> new Profile(.78, 12.5, 145, false, 7.5, .78, 15.0);
            case "meteor_shard" -> new Profile(1.00, 15.0, 190, false, 8.5, .74, 18.0);
            case "disintegrate" -> new Profile(.95, 27.0, 48, false, 2.4, .54, 3.2);
            case "delayed_blast_fireball" -> new Profile(1.00, 20.0, 300, false, 10.0, .76, 32.0);
            case "fire_storm" -> new Profile(1.00, 17.0, 145, false, 8.5, .72, 30.0);
            case "move_earth" -> new Profile(.90, 15.0, 275, false, 9.0, .44, 28.0);
            case "earthquake" -> new Profile(1.00, 21.0, 410, false, 10.5, .56, 56.0);
            case "meteor_swarm" -> new Profile(1.00, 24.0, 220, false, 10.0, .76, 22.0);
            case "world_sunder" -> new Profile(1.00, 33.0, 500, false, 10.5, .90, 24.0);
            case "arcane_annihilation" -> new Profile(1.00, 30.0, 125, false, 7.0, .72, 8.0);
            case "lightning_bolt" -> new Profile(.32, 10.5, 20, false, 1.55, .44, 1.8);
            case "thunderwave" -> new Profile(.36, 7.0, 24, false, 1.8, .42, 2.2);
            case "gust_of_wind" -> new Profile(.30, 4.5, 12, false, 1.35, .38, 1.7);
            default -> null;
        };
    }
}
