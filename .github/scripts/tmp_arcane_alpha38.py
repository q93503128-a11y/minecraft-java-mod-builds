from pathlib import Path
import re

root = Path('projects/arcane-circle')
client = root / 'src/main/java/kr/moonseungjun/arcanecircle/client'
magic = root / 'src/main/java/kr/moonseungjun/arcanecircle/magic'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, body):
    path.write_text(body, encoding='utf-8')


def rep(path, old, new, count=1):
    body = read(path)
    found = body.count(old)
    if found != count:
        raise SystemExit(f'{path}: expected {count}, found {found}: {old[:120]!r}')
    write(path, body.replace(old, new, count))


def sub_all(path, pattern, repl, minimum=1):
    body = read(path)
    body2, count = re.subn(pattern, repl, body, flags=re.M)
    if count < minimum:
        raise SystemExit(f'{path}: pattern not found: {pattern[:120]!r}')
    write(path, body2)
    return count


# ---------------------------------------------------------------------------
# 1. Destruction is authored per catastrophic spell instead of sharing one
#    tiny 10.5-block spherical cap.
# ---------------------------------------------------------------------------
destruction = r'''package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server-authoritative terrain rupture for physically destructive spells.
 *
 * Alpha.38 separates footprint from visual hype: surgical spells stay narrow, while genuine
 * catastrophe-class magic owns broad/deep terrain shapes.  Every shape still funnels through the
 * same hardness, blast-resistance, chunk, fluid and block-entity protections and a shared per-tick
 * budget, so larger spectacle does not mean an unbounded world-edit spike.
 */
public final class DestructiveMagicService {
    public enum TerrainClass { MAJOR, CONDITIONAL, NONE }

    private static final Set<String> MAJOR = Set.of(
            "disintegrate", "delayed_blast_fireball", "fire_storm", "earthquake",
            "meteor_swarm", "world_sunder", "arcane_annihilation");
    private static final Set<String> CONDITIONAL = Set.of(
            "fireball", "shatter", "flame_strike", "meteor_shard", "move_earth",
            "lightning_bolt", "thunderwave", "gust_of_wind");

    // Catastrophe spells may touch a lot of terrain, but all calls on a level still share these.
    private static final int MAX_BLOCK_CHANGES_PER_TICK = 720;
    private static final int MAX_BLOCK_SCANS_PER_TICK = 48_000;
    private static final int MAX_DROPPED_BLOCKS_PER_TICK = 96;
    private static final Map<ServerLevel, TickBudget> BUDGETS = new WeakHashMap<>();

    private record Candidate(BlockPos pos, double overload) {}
    private record Profile(double radiusScale, double baseEnergy, int maxBlocks, boolean drops,
                           double maxRadius, double verticalScale) {}

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

    public static int impact(ServerPlayer player, String spellId, Vec3 center,
                             double requestedRadius, double power) {
        Profile profile = profile(spellId);
        if (profile == null || center == null) return 0;
        ServerLevel level = (ServerLevel) player.level();
        TickBudget budget = budget(level);
        int changeLimit = Math.min(profile.maxBlocks(), budget.changesRemaining());
        if (profile.drops()) changeLimit = Math.min(changeLimit, budget.dropChangesRemaining());
        if (changeLimit <= 0 || !budget.scanAvailable()) return 0;

        double radius = Math.max(.75, Math.min(profile.maxRadius(),
                Math.max(.75, requestedRadius) * profile.radiusScale()));
        double energy = profile.baseEnergy() * (.78 + .22 * Math.sqrt(Math.max(.1, power)));
        int bound = (int) Math.ceil(radius);
        List<Candidate> candidates = new ArrayList<>();
        BlockPos origin = BlockPos.containing(center);
        double vertical = Math.max(1.15, radius * profile.verticalScale());

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
                    double falloff = Math.pow(Math.max(0.0, 1.0 - normalized), .74);
                    double local = energy * (.18 + .82 * falloff);
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

    /** Narrow line damage for lightning/disintegrate style spells. */
    public static int ray(ServerPlayer player, String spellId, Vec3 start, Vec3 end, double power) {
        if (start == null || end == null) return 0;
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < .05) return 0;
        Vec3 unit = delta.scale(1.0 / length);
        int changed = 0;
        int samples = Math.min(96, Math.max(1, (int) Math.ceil(length / .70)));
        for (int i = 1; i <= samples; i++) {
            Vec3 at = start.add(unit.scale(length * i / (double) samples));
            changed += impact(player, spellId, at, 1.18, power);
            if (changed >= 120 || budget((ServerLevel) player.level()).changesRemaining() <= 0) break;
        }
        return changed;
    }

    /**
     * 8C Earthquake is a battlefield event, not one round crater.  A dense epicenter fails first,
     * then eight uneven secondary foci tear the outer ground.  Shared budgets keep it bounded.
     */
    public static int quakeField(ServerPlayer player, Vec3 center, double requestedRadius, double power) {
        if (center == null) return 0;
        double field = Math.max(11.0, Math.min(24.0, requestedRadius));
        int changed = impact(player, "earthquake", center.add(0, -.35, 0), field * .58, power * 1.14);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0 * i / 8.0 + Math.sin(i * 1.91) * .12;
            double d = field * (.42 + .055 * (i % 3));
            Vec3 at = center.add(Math.cos(a) * d, -.18 - .08 * (i % 2), Math.sin(a) * d);
            changed += impact(player, "earthquake", at, field * (.27 + .025 * (i % 2)),
                    power * (.72 + .05 * (i % 3)));
            TickBudget tick = budget((ServerLevel) player.level());
            if (!tick.scanAvailable() || tick.changesRemaining() <= 0) break;
        }
        return changed;
    }

    /**
     * One Meteor Swarm projectile creates a deep bowl plus an irregular fractured rim.  Sixteen
     * staggered strikes therefore leave a real bombardment field instead of sixteen pinholes.
     */
    public static int meteorCrater(ServerPlayer player, Vec3 center, double radius, double power) {
        if (center == null) return 0;
        double r = Math.max(4.2, Math.min(9.5, radius * 1.38));
        int changed = impact(player, "meteor_swarm", center.add(0, -.45, 0), r, power * 1.16);
        changed += impact(player, "meteor_swarm", center.add(0, -1.75, 0), r * .70, power * 1.05);
        for (int i = 0; i < 5; i++) {
            double a = i * Math.PI * 2.0 / 5.0 + .41;
            Vec3 rim = center.add(Math.cos(a) * r * .46, -.20, Math.sin(a) * r * .46);
            changed += impact(player, "meteor_swarm", rim, r * .34, power * .72);
            TickBudget tick = budget((ServerLevel) player.level());
            if (!tick.scanAvailable() || tick.changesRemaining() <= 0) break;
        }
        return changed;
    }

    /**
     * Arcane Annihilation is a true deletion corridor.  It samples a thick beam from safely in
     * front of the caster to the sealed endpoint and carves through intervening terrain.
     */
    public static int annihilationCorridor(ServerPlayer player, Vec3 start, Vec3 end, double power) {
        if (start == null || end == null) return 0;
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length < 1.0) return 0;
        Vec3 unit = delta.scale(1.0 / length);
        double bore = Math.max(2.15, Math.min(3.75, 2.0 + Math.sqrt(Math.max(.1, power)) * .16));
        int changed = 0;
        double first = Math.min(length, 2.8); // never erase the block directly under the caster.
        for (double d = first; d <= length + .001; d += 2.35) {
            double t = d / Math.max(1.0, length);
            Vec3 at = start.add(unit.scale(d));
            changed += impact(player, "arcane_annihilation", at, bore * (.86 + .20 * t),
                    power * (.88 + .16 * t));
            TickBudget tick = budget((ServerLevel) player.level());
            if (changed >= 520 || !tick.scanAvailable() || tick.changesRemaining() <= 0) break;
        }
        return changed;
    }

    /**
     * World Sunder is a long, deep thirteen-point cut.  The center breaks widest/deepest and the
     * crack meanders instead of reading as a row of identical circular explosions.
     */
    public static int fissure(ServerPlayer player, String spellId, Vec3 center,
                              Vec3 direction, double range, double power) {
        if (center == null) return 0;
        Vec3 flat = direction == null ? Vec3.ZERO : new Vec3(direction.x, 0.0, direction.z);
        if (flat.lengthSqr() < 1.0E-8) flat = new Vec3(0.0, 0.0, 1.0);
        flat = flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        double halfLength = Math.max(12.0, Math.min(28.0, range * .34));
        int changed = 0;
        for (int i = -6; i <= 6; i++) {
            double t = i / 6.0;
            double weight = 1.0 - Math.abs(t);
            double wobble = Math.sin((i + 6) * 1.41) * (1.15 + weight * 1.45);
            Vec3 at = center.add(flat.scale(t * halfLength)).add(right.scale(wobble))
                    .add(0, -.28 - weight * .72, 0);
            changed += impact(player, spellId, at, 3.8 + weight * 3.8,
                    power * (.74 + weight * .42));
            TickBudget tickBudget = budget((ServerLevel) player.level());
            if (!tickBudget.scanAvailable() || tickBudget.changesRemaining() <= 0) break;
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
            default -> {
            }
        }
    }

    private static TickBudget budget(ServerLevel level) {
        TickBudget budget = BUDGETS.computeIfAbsent(level, ignored -> new TickBudget());
        budget.reset(level.getGameTime());
        return budget;
    }

    private static Profile profile(String id) {
        return switch (id) {
            case "fireball" -> new Profile(.78, 8.2, 100, false, 6.0, .58);
            case "shatter" -> new Profile(.76, 9.2, 100, true, 5.5, .46);
            case "flame_strike" -> new Profile(.72, 12.0, 130, false, 7.0, .78);
            case "meteor_shard" -> new Profile(1.00, 14.5, 180, false, 8.5, .74);
            case "disintegrate" -> new Profile(.90, 26.0, 44, false, 2.1, .54);
            case "delayed_blast_fireball" -> new Profile(1.00, 18.5, 280, false, 12.0, .76);
            case "fire_storm" -> new Profile(.92, 15.5, 120, false, 8.5, .72);
            case "move_earth" -> new Profile(.78, 14.0, 260, false, 12.0, .44);
            case "earthquake" -> new Profile(1.00, 19.5, 380, false, 18.0, .52);
            case "meteor_swarm" -> new Profile(1.00, 22.5, 190, false, 10.0, .72);
            case "world_sunder" -> new Profile(1.00, 31.0, 480, false, 15.0, .82);
            case "arcane_annihilation" -> new Profile(1.00, 28.0, 110, false, 3.8, .70);
            case "lightning_bolt" -> new Profile(.30, 10.0, 18, false, 1.4, .44);
            case "thunderwave" -> new Profile(.34, 6.6, 22, false, 1.6, .42);
            case "gust_of_wind" -> new Profile(.28, 4.2, 10, false, 1.25, .38);
            default -> null;
        };
    }
}
'''
write(magic / 'DestructiveMagicService.java', destruction)

# Gameplay call sites use the new authored catastrophe shapes.
high = magic / 'HighCircleSpellEffects.java'
rep(high,
    '        DestructiveMagicService.impact(player,huge?"earthquake":"move_earth",center,r,power);\n',
    '        if (huge) DestructiveMagicService.quakeField(player, center, r, power);\n'
    '        else DestructiveMagicService.impact(player, "move_earth", center, r, power);\n')
rep(high,
    '        DestructiveMagicService.impact(player,"meteor_swarm",impact,radius,power*strike.scale());\n',
    '        DestructiveMagicService.meteorCrater(player, impact, radius, power * strike.scale());\n')

casting = magic / 'SpellCastingService.java'
rep(casting,
    '        DestructiveMagicService.ray(player,"arcane_annihilation",start,end,power);\n',
    '        DestructiveMagicService.annihilationCorridor(player, start, end, power);\n')
rep(casting,
    '        for (int index = 0; index < targets.size(); index++) {\n'
    '            targets.get(index).hurtServer(level, level.damageSources().playerAttack(player),\n'
    '                    (float) (power * Math.max(0.65, 1.15 - index * 0.08)));\n'
    '        }\n'
    '        return true;\n'
    '    }\n\n'
    '    private static Optional<Mob> lookTarget',
    '        for (int index = 0; index < targets.size(); index++) {\n'
    '            targets.get(index).hurtServer(level, level.damageSources().playerAttack(player),\n'
    '                    (float) (power * Math.max(0.65, 1.15 - index * 0.08)));\n'
    '        }\n'
    '        level.playSound(null, BlockPos.containing(end), SoundEvents.GENERIC_EXPLODE.value(),\n'
    '                SoundSource.PLAYERS, 1.35F, 0.52F);\n'
    '        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE,\n'
    '                SoundSource.PLAYERS, .85F, .60F);\n'
    '        return true;\n'
    '    }\n\n'
    '    private static Optional<Mob> lookTarget')

# ---------------------------------------------------------------------------
# 2. Physical scale and visual scale agree for catastrophe-class spells.
# ---------------------------------------------------------------------------
presentation = magic / 'SpellPresentationProfile.java'
profile_lines = {
    'delayed_blast_fireball': '        put("delayed_blast_fireball", SigilStyle.TARGET_SEAL, MotionStyle.TARGET_BURST, 4.60, 6, 8, 0, 0, 2.05, 18);',
    'fire_storm': '        put("fire_storm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 13.80, 6, 9, 0, 24, 2.28, 14);',
    'earthquake': '        put("earthquake", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 15.50, 6, 12, 0, 0, 2.22, 4);',
    'meteor_swarm': '        put("meteor_swarm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 22.00, 6, 12, 0, 52, 3.10, MeteorBarragePattern.firstImpactTick());',
    'world_sunder': '        put("world_sunder", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 18.00, 6, 12, 0, 0, 2.85, 5);',
    'meteor_shard': '        put("meteor_shard", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 7.20, 5, 4, 0, 16, 1.72, 8);',
}
for spell_id, line in profile_lines.items():
    sub_all(presentation, rf'^\s*put\("{spell_id}"[^\n]*\);$', line, 1)
body = read(presentation)
if 'put("arcane_annihilation"' not in body:
    needle = '        put("world_sunder", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 18.00, 6, 12, 0, 0, 2.85, 5);\n'
    if needle not in body:
        raise SystemExit('world_sunder final profile insertion point missing')
    body = body.replace(needle,
        '        put("arcane_annihilation", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 2.80, 6, 6, 0, 0, 2.35, 0);\n' + needle, 1)
    write(presentation, body)
else:
    sub_all(presentation, r'^\s*put\("arcane_annihilation"[^\n]*\);$',
            '        put("arcane_annihilation", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 2.80, 6, 6, 0, 0, 2.35, 0);', 1)

# ---------------------------------------------------------------------------
# 3. Catastrophe charge circles get their own authority layer rather than only
#    becoming a larger copy of the normal school formula.
# ---------------------------------------------------------------------------
over = client / 'ArcaneSpellVisualOverhaul.java'
rep(over,
'''    private static final Set<String> BUFFS = Set.of(
            "shield", "feather_fall", "mage_armor", "mirror_image", "invisibility", "blur", "fly", "haste",
            "protection_from_energy", "greater_invisibility", "resilient_sphere", "stoneskin",
            "freedom_of_movement", "true_seeing", "globe_of_invulnerability", "simulacrum", "clone",
            "fire_shield", "solar_guard", "shapechange", "foresight");
''',
'''    private static final Set<String> BUFFS = Set.of(
            "shield", "feather_fall", "mage_armor", "mirror_image", "invisibility", "blur", "fly", "haste",
            "protection_from_energy", "greater_invisibility", "resilient_sphere", "stoneskin",
            "freedom_of_movement", "true_seeing", "globe_of_invulnerability", "simulacrum", "clone",
            "fire_shield", "solar_guard", "shapechange", "foresight");
    private static final Set<String> CATASTROPHIC = Set.of(
            "delayed_blast_fireball", "fire_storm", "earthquake", "meteor_swarm",
            "world_sunder", "arcane_annihilation");
''')
rep(over,
'        if (spell.circle() >= 6) highCircleCrown(m, basis, r, p, time, seed, spell.circle());\n'
'        return m.build();\n',
'        if (spell.circle() >= 6) highCircleCrown(m, basis, r, p, time, seed, spell.circle());\n'
'        if (CATASTROPHIC.contains(spell.id())) catastrophicAuthority(m, basis, r, p, time, seed, spell.id());\n'
'        return m.build();\n')
marker = '    private static void portalContract(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis g, double r,\n'
if marker not in read(over):
    raise SystemExit('portalContract marker missing')
cat_method = r'''    /** Catastrophe-only charge authority: converging break seals + cross-plane lock rings. */
    private static void catastrophicAuthority(ArcaneWorldMesh.Builder m, ArcaneWorldMesh.Basis b, double r,
                                              double p, double t, int seed, String id) {
        if (p < .28) return;
        double wake = smooth(clamp((p - .28) / .72, 0.0, 1.0));
        double outer = r * (1.08 + .10 * wake);
        m.brokenBand(b, Vec3.ZERO, outer * .90, outer, 88, 9, 1.14F, .11F);
        m.brokenBand(b, Vec3.ZERO, outer * .66, outer * .72, 72, 7, 1.06F, .09F);
        int anchors = "meteor_swarm".equals(id) ? 8 : 6;
        for (int i = 0; i < anchors; i++) {
            double a = i * Math.PI * 2.0 / anchors + t * (i % 2 == 0 ? .018 : -.015);
            Vec3 outerNode = b.point(a, outer * .96);
            Vec3 innerNode = b.point(a + (i % 2 == 0 ? .13 : -.11), outer * .72);
            m.line(outerNode, innerNode, i % 3 == 0 ? .68F : .34F);
            m.runeGlyph(b, outerNode, r * .052, seed + i * 211, -a + t * .025, .38F);
        }
        if (p > .58) {
            ArcaneWorldMesh.Basis cross = ArcaneWorldMesh.Basis.fromNormal(b.right(), b.normal());
            double crossR = r * (.42 + .16 * wake);
            m.circle(cross, b.normal().scale(r * .06), crossR, 54, .46F);
            m.brokenBand(cross, b.normal().scale(-r * .05), crossR * .72, crossR * .82,
                    48, 6, 1.08F, .08F);
        }
        if (p > .76) {
            Vec3 n = b.normal();
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4.0 + i * Math.PI / 2.0;
                Vec3 base = b.point(a, r * .52);
                double h = r * (.24 + .05 * (i % 2));
                m.line(base.add(n.scale(-h)), base.add(n.scale(h)), i % 2 == 0 ? .66F : .42F);
                m.diamond(b, base.add(n.scale(h)), r * .045, a + t * .03, 1.10F, .14F);
            }
        }
    }

'''
write(over, read(over).replace(marker, cat_method + marker, 1))

# ---------------------------------------------------------------------------
# 4. Release cinematics: anticipation -> collapse -> primary impact ->
#    aftershock.  Destructive spells no longer share the generic burst body.
# ---------------------------------------------------------------------------
director = client / 'SpellCinematicDirector.java'
rep(director,
'''        if ("meteor_swarm".equals(spell.id())) { meteorSwarm(m,targetOffset,age,impactAge,scale); return m.build(); }
        if ("prismatic_wall".equals(spell.id())) { prismaticWallFrame(m,face,targetOffset,range,age,spell.circle()); return m.build(); }
''',
'''        if ("meteor_swarm".equals(spell.id())) { meteorSwarm(m,targetOffset,age,impactAge,scale); return m.build(); }
        if ("meteor_shard".equals(spell.id())) { meteorShardImpact(m,targetOffset,age,impactAge,scale); return m.build(); }
        if ("delayed_blast_fireball".equals(spell.id())) { delayedCataclysm(m,ground,targetOffset,age,impactAge,scale); return m.build(); }
        if ("arcane_annihilation".equals(spell.id())) { annihilationBeam(m,face,direction,targetOffset,age,scale,seed); return m.build(); }
        if ("prismatic_wall".equals(spell.id())) { prismaticWallFrame(m,face,targetOffset,range,age,spell.circle()); return m.build(); }
''')

old_meteor = r'''    private static void meteorSwarm(ArcaneWorldMesh.Builder m,Vec3 target,double age,double impact,double scale){
        ArcaneWorldMesh.Basis down=ArcaneWorldMesh.Basis.facing(new Vec3(0,-1,0));
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();
        double nowTicks=age*MeteorBarragePattern.durationTicks();
        for(int i=0;i<MeteorBarragePattern.count();i++){
            MeteorBarragePattern.Strike s=MeteorBarragePattern.strike(i);
            double local=(nowTicks-(s.impactTick()-8.0))/8.0;
            if(local<0.0||local>1.55)continue;
            Vec3 hit=target.add(s.offsetX(),0,s.offsetZ());
            double fall=clamp(local,0,1),meteorScale=scale*(.42+.30*s.scale());
            Vec3 pos=hit.add(0,s.fallHeight()*(1-easeIn(fall)),0);
            if(local<=1.0){
                m.orb(pos,.42*meteorScale,18,1.24F,.48F);
                m.shard(pos.add(0,.55*meteorScale,0),new Vec3(0,-1,0),down,1.85*meteorScale,.24*meteorScale,1.20F,.38F);
                Vec3 tail=pos.add(0,2.4+meteorScale*1.2,0); m.line(tail,pos,.68F);
            }else{
                double impactAge=(local-1.0)*.44;
                impactRing(m,g,hit,meteorScale*(1.05+s.scale()*.45),impactAge);
                for(int q=0;q<5;q++){double a=q*Math.PI*2/5.0+i*.73;m.line(hit.add(g.point(a,.18)),hit.add(g.point(a,meteorScale*(.8+impactAge*2.2))),.42F);}
            }
        }
    }
'''
new_meteor = r'''    private static void meteorSwarm(ArcaneWorldMesh.Builder m,Vec3 target,double age,double impact,double scale){
        ArcaneWorldMesh.Basis down=ArcaneWorldMesh.Basis.facing(new Vec3(0,-1,0));
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground();
        double nowTicks=age*MeteorBarragePattern.durationTicks();
        // The sky stays claimed for the whole barrage instead of the ritual disappearing before impact.
        Vec3 crown=target.add(0,14.0+scale*2.2,0);
        double crownFade=clamp((1.0-age)/.18,0,1), crownR=(7.0+scale*2.5)*(1.0-.08*Math.sin(age*6.0));
        m.brokenBand(g,crown,crownR*.78,crownR,96,8,1.16F,(float)(.16*crownFade));
        m.brokenBand(g,crown,crownR*.50,crownR*.58,72,7,1.08F,(float)(.11*crownFade));
        for(int a=0;a<8;a++){
            double ang=a*Math.PI/4.0-age*.20;
            Vec3 node=crown.add(g.point(ang,crownR*.88));
            m.line(node,node.add(0,-2.8-scale*.35,0),a%2==0?.70F:.38F);
            if(a%2==0)m.runeGlyph(g,node,.26*scale,a*97,ang,.38F);
        }
        for(int i=0;i<MeteorBarragePattern.count();i++){
            MeteorBarragePattern.Strike s=MeteorBarragePattern.strike(i);
            double local=(nowTicks-(s.impactTick()-9.0))/9.0;
            if(local<0.0||local>1.62)continue;
            Vec3 hit=target.add(s.offsetX(),0,s.offsetZ());
            double fall=clamp(local,0,1),meteorScale=scale*(.48+.34*s.scale());
            Vec3 pos=hit.add(0,s.fallHeight()*(1-easeIn(fall)),0);
            if(local<=1.0){
                m.orb(pos,.48*meteorScale,20,1.28F,.52F);
                m.shard(pos.add(0,.62*meteorScale,0),new Vec3(0,-1,0),down,2.15*meteorScale,.28*meteorScale,1.24F,.42F);
                for(int tail=0;tail<3;tail++){
                    double ta=tail*Math.PI*2/3.0+i*.31;
                    Vec3 tip=pos.add(g.point(ta,.18*meteorScale)).add(0,3.0+meteorScale*(1.0+tail*.18),0);
                    m.line(tip,pos,tail==0?.82F:.46F);
                }
            }else{
                double hitAge=(local-1.0)*.54;
                impactRing(m,g,hit,meteorScale*(1.35+s.scale()*.62),hitAge);
                impactRing(m,g,hit,meteorScale*(.82+s.scale()*.32),Math.max(0,hitAge-.08));
                for(int q=0;q<8;q++){
                    double qa=q*Math.PI/4.0+i*.73;
                    double reach=meteorScale*(1.05+s.scale()*.45+hitAge*3.1);
                    m.line(hit.add(g.point(qa,.18)),hit.add(g.point(qa+.08*Math.sin(q+i),reach)),q%3==0?.68F:.38F);
                }
                for(int q=0;q<4;q++){
                    double qa=q*Math.PI/2.0+i*.41;
                    Vec3 base=hit.add(g.point(qa,meteorScale*(.55+.12*q)));
                    m.shard(base.add(0,.35+q*.10,0),new Vec3(0,1,0),g,.75+q*.20,.10+.02*q,1.10F,.20F);
                }
            }
        }
    }
'''
rep(director, old_meteor, new_meteor)

rep(director,
'    private static void fireStorm(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis g,Vec3 target,double age,double scale){for(int i=0;i<6;i++){double a=Math.PI*2*i/6;Vec3 hit=target.add(g.point(a,4.0+i*.35));double h=(6.5+i*.5)*(1-clamp(age/.72,0,1));Vec3 top=hit.add(0,h,0);m.beamPrism(top,new Vec3(0,-1,0),ArcaneWorldMesh.Basis.facing(new Vec3(0,-1,0)),Math.max(.2,h),.18*scale,1.18F,.32F);impactRing(m,g,hit,scale*(.8+i*.06),Math.max(0,age-.55));}}\n',
'''    private static void fireStorm(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis g,Vec3 target,double age,double scale){
        Vec3 crown=target.add(0,9.0+scale,0); double contract=1.0-clamp(age/.58,0,1);
        m.brokenBand(g,crown,5.2*scale*(.72+.28*contract),5.8*scale*(.72+.28*contract),76,7,1.12F,.14F);
        for(int i=0;i<6;i++){
            double a=Math.PI*2*i/6; Vec3 hit=target.add(g.point(a,5.0));
            double h=(9.0+i*.55)*(1-clamp(age/.70,0,1)); Vec3 top=hit.add(0,h,0);
            m.beamPrism(top,new Vec3(0,-1,0),ArcaneWorldMesh.Basis.facing(new Vec3(0,-1,0)),Math.max(.2,h),.24*scale,1.20F,.34F);
            if(age>.46){
                double local=Math.max(0,age-.46);
                impactRing(m,g,hit,scale*1.55,local);
                impactRing(m,g,hit,scale*.95,Math.max(0,local-.08));
                for(int q=0;q<5;q++){double qa=q*Math.PI*2/5.0+i*.31;m.line(hit.add(g.point(qa,.15)),hit.add(g.point(qa,scale*(1.0+local*2.2))),q==0?.70F:.36F);}
            }
        }
    }
''')
rep(director,
'    private static void worldFault(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis g,Vec3 target,double range,double age,double scale,String id){double r=Math.max(7,SpellMetrics.effectRadius(id,range,9))*clamp(age/.55,0,1);int n="world_sunder".equals(id)?14:10;for(int i=0;i<n;i++){double a=Math.PI*2*i/n+i*.17;Vec3 p0=target.add(g.point(a,r*.05));Vec3 p1=target.add(g.point(a+.12*Math.sin(i*2.1),r*.50));Vec3 p2=target.add(g.point(a-.09*Math.cos(i*.7),r));m.line(p0,p1,i%3==0?1.45F:.82F).line(p1,p2,.72F);} }\n',
'''    private static void worldFault(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis g,Vec3 target,double range,double age,double scale,String id){
        double grow=clamp(age/.48,0,1),r=Math.max(8,SpellMetrics.effectRadius(id,range,9))*grow;
        int n="world_sunder".equals(id)?22:16;
        if(grow>.18){
            m.brokenBand(g,target,r*.36,r*.42,72,7,1.08F,.11F);
            m.brokenBand(g,target,r*.72,r*.78,88,9,1.04F,.08F);
        }
        for(int i=0;i<n;i++){
            double a=Math.PI*2*i/n+i*.17;
            Vec3 p0=target.add(g.point(a,r*.04));
            Vec3 p1=target.add(g.point(a+.15*Math.sin(i*2.1),r*(.38+.05*(i%3))));
            Vec3 p2=target.add(g.point(a-.12*Math.cos(i*.7),r*(.88+.04*(i%4))));
            m.line(p0,p1,i%3==0?1.58F:.88F).line(p1,p2,i%4==0?.82F:.52F);
            if(age>.28&&i%3==0){Vec3 c=p1.add(0,.28+.10*(i%4),0);m.shard(c,new Vec3(0,1,0),g,.55+.14*(i%4),.10+.015*(i%3),1.06F,.18F);}
        }
        if("world_sunder".equals(id)&&age>.36){
            for(int i=-5;i<=5;i++){double x=i*r*.13;Vec3 c=target.add(x,0,Math.sin(i*1.7)*r*.10);m.line(c.add(-.35,0,-.20),c.add(.42,0,.26),i==0?1.72F:.72F);}
        }
    }
''')

insert_point = '    private static void executionWord(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis b,Vec3 target,double age,double scale)'
body = read(director)
if insert_point not in body:
    raise SystemExit('director insertion marker missing')
extra_methods = r'''    private static void meteorShardImpact(ArcaneWorldMesh.Builder m,Vec3 target,double age,double impact,double scale){
        ArcaneWorldMesh.Basis g=ArcaneWorldMesh.Basis.ground(); ArcaneWorldMesh.Basis down=ArcaneWorldMesh.Basis.facing(new Vec3(0,-1,0));
        double imp=Math.max(.18,impact<=0?.42:impact),t=clamp(age/imp,0,1),alt=14.0+scale*2.0;
        Vec3 sky=target.add(0,alt,0); double ring=3.4*scale*(1.0-.22*t);
        m.brokenBand(g,sky,ring*.74,ring,64,7,1.12F,.14F);
        Vec3 pos=target.add(0,alt*(1-easeIn(t)),0);
        if(t<1){m.orb(pos,.44*scale,18,1.24F,.46F);m.shard(pos.add(0,.5*scale,0),new Vec3(0,-1,0),down,2.0*scale,.26*scale,1.22F,.40F);}
        if(age>=imp){double local=age-imp;impactRing(m,g,target,scale*2.0,local);for(int i=0;i<8;i++){double a=i*Math.PI/4.0;m.line(target.add(g.point(a,.15)),target.add(g.point(a,scale*(1.2+local*3.0))),i%2==0?.74F:.40F);}}
    }

    private static void delayedCataclysm(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis g,Vec3 target,double age,double impact,double scale){
        double imp=Math.max(.20,impact<=0?.62:impact),t=clamp(age/imp,0,1);
        double contract=1.0-easeIn(t),outer=(4.8*scale)*(.38+.62*contract);
        Vec3 core=target.add(0,1.10+.35*contract,0);
        m.orb(core,.48*scale*(.72+.28*contract),22,1.30F,.50F);
        m.brokenBand(g,target.add(0,.06,0),outer*.84,outer,84,8,1.16F,.15F);
        m.brokenBand(g,target.add(0,.09,0),outer*.52,outer*.60,64,7,1.08F,.10F);
        for(int i=0;i<8;i++){double a=i*Math.PI/4.0-age*.9;Vec3 n=target.add(g.point(a,outer*.92));m.line(n,core,i%2==0?.62F:.34F);}
        if(age>=imp){double local=age-imp;impactRing(m,g,target,scale*3.0,local);impactRing(m,g,target,scale*1.8,Math.max(0,local-.07));for(int i=0;i<12;i++){double a=i*Math.PI/6.0;m.line(target.add(g.point(a,.2)),target.add(g.point(a,scale*(1.6+local*4.2))),i%3==0?.86F:.42F);}}
    }

    private static void annihilationBeam(ArcaneWorldMesh.Builder m,ArcaneWorldMesh.Basis face,Vec3 direction,Vec3 target,double age,double scale,int seed){
        Vec3 axis=target.lengthSqr()>.01?target:direction.scale(12.0); double len=Math.max(.5,axis.length()); Vec3 n=axis.scale(1.0/len);
        ArcaneWorldMesh.Basis beam=ArcaneWorldMesh.Basis.facing(n); double bore=.16*scale*(1.0+.22*Math.sin(age*18.0));
        m.beamPrism(Vec3.ZERO,n,beam,len,bore*1.35,1.16F,.30F);
        m.beamPrism(Vec3.ZERO,n,beam,len,bore*.72,1.30F,.42F);
        int gates=Math.max(4,Math.min(10,(int)(len/4.0)));
        for(int i=1;i<=gates;i++){double t=i/(double)(gates+1);Vec3 c=n.scale(len*t);double r=(.34+.10*(i%3))*scale;m.polygon(beam,c,r,6,age*(i%2==0?1.8:-1.4)+i,.52F);}
        if(age>.55){double local=(age-.55)/.45;impactRing(m,ArcaneWorldMesh.Basis.ground(),target,scale*1.75,local*.24);for(int i=0;i<10;i++){double a=i*Math.PI/5.0+seed*.0001;m.line(target.add(face.point(a,.12)),target.add(face.point(a,scale*(.8+local*1.7))),i%2==0?.78F:.40F);}}
    }

'''
write(director, body.replace(insert_point, extra_methods + insert_point, 1))

# Signature authoring for the new unique bodies.
body = read(director)
old = '            case "flame_strike","fire_storm","meteor_swarm","delayed_blast_fireball" -> sig(Form.SKY,id.equals("meteor_swarm")?2.3:1.2,6,id.equals("meteor_swarm")?12:id.equals("fire_storm")?6:1,id.equals("meteor_swarm")?12:3,id.equals("meteor_swarm")?34:14,1.9);\n'
new = '''            case "flame_strike","fire_storm","meteor_swarm","delayed_blast_fireball","meteor_shard" -> sig(Form.SKY,
                    id.equals("meteor_swarm")?2.65:id.equals("fire_storm")?1.55:id.equals("delayed_blast_fireball")?1.48:id.equals("meteor_shard")?1.28:1.2,
                    7,id.equals("meteor_swarm")?12:id.equals("fire_storm")?6:id.equals("meteor_shard")?1:1,
                    id.equals("meteor_swarm")?14:id.equals("fire_storm")?5:3,
                    id.equals("meteor_swarm")?46:id.equals("fire_storm")?20:id.equals("meteor_shard")?16:14,1.9);
            case "arcane_annihilation" -> sig(Form.RAY,1.90,7,6,.30,0,4.4);
'''
if old not in body:
    raise SystemExit('director SKY signature line missing')
write(director, body.replace(old, new, 1))

# Mechanical descriptions now admit the larger physical footprint.
summary = magic / 'SpellEffectSummary.java'
body = read(summary)
body = body.replace('16발 시드형 연속 폭격 · 개별 피해·화상·지형 파괴',
                    '16발 시드형 연속 폭격 · 개별 피해·화상·대형 충돌구 지형 파괴')
body = body.replace('목표 지면 초대형 피해·띄우기 · 방향성 실제 세계 균열',
                    '목표 지면 초대형 피해·띄우기 · 장거리·심층 실제 세계 균열')
write(summary, body)

# ---------------------------------------------------------------------------
# 5. Version + project/audit contracts.
# ---------------------------------------------------------------------------
for path in [root / 'gradle.properties', root / 'src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java',
             root / 'src/main/resources/data/arcanecircle/spell_catalog/index.json']:
    body = read(path)
    if '0.12.1-alpha.37' not in body:
        raise SystemExit(f'{path}: alpha.37 marker missing')
    write(path, body.replace('0.12.1-alpha.37', '0.12.1-alpha.38'))

project = root / 'PROJECT.md'
body = read(project)
append = r'''

## Alpha.38 catastrophic-impact + cinematic-detail contracts

- Catastrophe-class terrain magic is no longer capped by one universal 10.5-block sphere. Every destructive profile owns a maximum radius, depth ratio, energy and block budget while still sharing loaded-chunk, block-entity, fluid, hardness/resistance and per-level tick protections.
- The shared no-drop destruction ceiling is 720 changed blocks / 48,000 scanned cells per level tick; drop-producing Shatter remains separately capped at 96. Large spells gain footprint by authored multi-focus shapes and staggered impacts, not an unbounded synchronous world edit.
- Meteor Swarm uses 16 independently seeded deep crater events with fractured rims. Fire Storm's six columns can devastate six real impact zones. Earthquake uses a dense epicenter plus eight irregular secondary foci. World Sunder uses a long/deep thirteen-point meandering cut. Arcane Annihilation bores a thick deletion corridor from safely in front of the caster to the locked endpoint.
- Catastrophe visuals use anticipation -> convergence/collapse -> primary impact -> secondary shock/fracture. Meteor Swarm keeps a sky authority crown alive across the barrage; Fire Storm has a contracting sky lattice and six synchronized columns; Delayed Blast Fireball visibly compresses before detonation; Arcane Annihilation owns a multi-gate beam body; World Sunder/Earthquake draw multiple fault bands and lifted debris accents.
- Catastrophe charge formulae add a dedicated authority layer on top of the 6C+ hierarchy: broken outer seals, converging anchors, a cross-plane lock ring and final vertical authority pylons. This is restricted to the genuinely destructive spells so high-circle support/control magic keeps a different silhouette.
'''
if '## Alpha.38 catastrophic-impact + cinematic-detail contracts' not in body:
    body += append
write(project, body)

audit = root / 'tools/test_current_source.py'
body = read(audit).replace('0.12.1-alpha.37', '0.12.1-alpha.38')
body = body.replace("'worldFault','phoenix'", "'worldFault','phoenix','delayedCataclysm','annihilationBeam','meteorShardImpact'")
old = '''# Destruction budgets/classification and explicit World Sunder fissure.
destruction=text(magic/'DestructiveMagicService.java')
for token in ['getDestroySpeed','getExplosionResistance','destroyBlock','hasChunkAt','MAX_BLOCK_CHANGES_PER_TICK',
'MAX_BLOCK_SCANS_PER_TICK','MAX_DROPPED_BLOCKS_PER_TICK','dropChangesRemaining','TerrainClass','MAJOR','CONDITIONAL',
'lightning_bolt','thunderwave','gust_of_wind','fissure','seven-point cut','case "world_sunder" -> fissure']:
    assert token in destruction, token
assert 'case "move_earth" -> new Profile(.54, 11.0, 170, false)' in destruction
assert 'case "earthquake" -> new Profile(.58, 14.5, 240, false)' in destruction
assert 'case "world_sunder" -> new Profile(.62, 28.0, 320, false)' in destruction
'''
new = '''# Destruction budgets/classification and catastrophe-shaped terrain impact.
destruction=text(magic/'DestructiveMagicService.java')
for token in ['getDestroySpeed','getExplosionResistance','destroyBlock','hasChunkAt','MAX_BLOCK_CHANGES_PER_TICK = 720',
'MAX_BLOCK_SCANS_PER_TICK = 48_000','MAX_DROPPED_BLOCKS_PER_TICK = 96','dropChangesRemaining','TerrainClass','MAJOR','CONDITIONAL',
'lightning_bolt','thunderwave','gust_of_wind','fissure','thirteen-point cut','case "world_sunder" -> fissure',
'quakeField','eight uneven secondary foci','meteorCrater','deep bowl','annihilationCorridor','true deletion corridor']:
    assert token in destruction, token
for token in ['case "move_earth" -> new Profile(.78, 14.0, 260, false, 12.0, .44)',
              'case "earthquake" -> new Profile(1.00, 19.5, 380, false, 18.0, .52)',
              'case "meteor_swarm" -> new Profile(1.00, 22.5, 190, false, 10.0, .72)',
              'case "world_sunder" -> new Profile(1.00, 31.0, 480, false, 15.0, .82)',
              'case "arcane_annihilation" -> new Profile(1.00, 28.0, 110, false, 3.8, .70)']:
    assert token in destruction, token
assert 'DestructiveMagicService.quakeField(player, center, r, power)' in text(magic/'HighCircleSpellEffects.java')
assert 'DestructiveMagicService.meteorCrater(player, impact, radius, power * strike.scale())' in text(magic/'HighCircleSpellEffects.java')
assert 'DestructiveMagicService.annihilationCorridor(player, start, end, power)' in casting_service
'''
if old not in body:
    raise SystemExit('old destruction audit block missing')
body = body.replace(old, new, 1)
# Strengthen visual regressions near alpha37 block without creating another ordering dependency.
needle = "assert 'ArcaneBuffRuntime.apply(player, \"solar_guard\", power, range)' in fusion\n"
extra = '''assert 'CATASTROPHIC = Set.of' in overhaul and 'catastrophicAuthority' in overhaul
for token in ['delayedCataclysm','annihilationBeam','meteorShardImpact','sky authority crown']:
    assert token in director, token
for token in ['put("meteor_swarm", SigilStyle.SKY_RITUAL, MotionStyle.SKY_DROP, 22.00',
              'put("earthquake", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 15.50',
              'put("world_sunder", SigilStyle.QUAD_ARRAY, MotionStyle.FIELD, 18.00',
              'put("arcane_annihilation", SigilStyle.FRONT_LANCE, MotionStyle.BEAM, 2.80']:
    assert token in presentation, token
'''
if needle not in body:
    raise SystemExit('alpha37 audit insertion point missing')
body = body.replace(needle, needle + extra, 1)
write(audit, body)

print('Arcane Circle alpha.38 catastrophic impact overhaul applied')
