package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Sparse staged seals: concentric boundaries, radial cells, a central seal and node marks. */
public final class SpellSigilService {
    public static final int CHARGE_STAGES = 7;
    private SpellSigilService() {}

    public static void renderChargeStep(ServerPlayer player, SpellDefinition spell, double effectiveRange, int step) {
        ServerLevel level = (ServerLevel) player.level();
        Seal seal = seal(player, spell, effectiveRange, false);
        drawStep(level, seal, spell, Math.max(0, Math.min(CHARGE_STAGES - 1, step)), false);
    }

    public static void renderReadyPulse(ServerPlayer player, SpellDefinition spell, double effectiveRange) {
        ServerLevel level = (ServerLevel) player.level();
        Seal seal = seal(player, spell, effectiveRange, false);
        ParticleOptions main = particle(spell);
        ring(level, seal, seal.radius(), main, 28);
        nodeMarks(level, seal, spell, main, 1);
    }

    public static void renderRelease(ServerPlayer player, SpellDefinition spell, double effectiveRange) {
        ServerLevel level = (ServerLevel) player.level();
        Seal seal = seal(player, spell, effectiveRange, true);
        for (int step = 0; step < CHARGE_STAGES; step++) drawStep(level, seal, spell, step, true);
        ring(level, seal, seal.radius() * 1.08, ParticleTypes.END_ROD, 20);
    }

    private static Seal seal(ServerPlayer player, SpellDefinition spell, double range, boolean release) {
        double ratio = spell.range() <= 0.0 ? 1.0 : Math.max(0.82, Math.min(2.0, range / spell.range()));
        double radius = Math.min(1.20, (0.44 + spell.circle() * 0.065) * Math.sqrt(ratio)) * (release ? 1.08 : 1.0);
        Anchor anchor = anchor(player, spell, range);
        return new Seal(anchor.center(), anchor.right(), anchor.up(), radius);
    }

    private static Anchor anchor(ServerPlayer player, SpellDefinition spell, double range) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 upReference = Math.abs(look.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = look.cross(upReference).normalize();
        Vec3 up = right.cross(look).normalize();
        return switch (spell.sigilAnchor()) {
            case FRONT -> new Anchor(player.getEyePosition().add(look.scale(1.78)).add(up.scale(-0.34)), right, up);
            case FEET, GROUND_SELF -> horizontal(player.position().add(0, 0.08, 0));
            case BODY -> horizontal(player.position().add(0, 0.12, 0));
            case GROUND_TARGET -> horizontal(aimGround(player, Math.max(5.0, range)).add(0, 0.06, 0));
            case TARGET -> horizontal(target(player, Math.max(7.0, range)).map(Mob::position)
                    .orElse(player.getEyePosition().add(look.scale(2.4))).add(0, 0.06, 0));
        };
    }

    private static Anchor horizontal(Vec3 center) {
        return new Anchor(center, new Vec3(1, 0, 0), new Vec3(0, 0, 1));
    }

    private static void drawStep(ServerLevel level, Seal seal, SpellDefinition spell, int step, boolean release) {
        ParticleOptions main = particle(spell);
        int signature = Math.floorMod(spell.id().hashCode(), 4093);
        double rotation = (signature % 24) * Math.PI / 12.0;
        int divisions = 6 + (spell.circle() >= 4 ? 2 : 0) + (spell.circle() >= 8 ? 2 : 0);
        switch (step) {
            case 0 -> ring(level, seal, seal.radius(), main, release ? 38 : 28);
            case 1 -> ring(level, seal, seal.radius() * 0.86, main, release ? 34 : 24);
            case 2 -> radialCompartments(level, seal, divisions, main, release ? 7 : 5, rotation);
            case 3 -> centralSeal(level, seal, spell, main, release, rotation);
            case 4 -> nodeMarks(level, seal, spell, main, release ? 2 : 1);
            case 5 -> signatureLines(level, seal, signature, main, rotation, release);
            case 6 -> runeTicks(level, seal, spell, main, release);
            default -> { }
        }
    }

    private static void centralSeal(ServerLevel level, Seal s, SpellDefinition spell, ParticleOptions p,
                                    boolean release, double rotation) {
        SpellWorldLore.SigilFamily family = SpellWorldLore.sigilFamily(spell.id());
        int edge = release ? 7 : 5;
        switch (family) {
            case LANCE -> {
                polygon(level, s, s.radius() * 0.58, 3, rotation, p, edge);
                line(level, point(s, 0, -s.radius() * 0.58), point(s, 0, s.radius() * 0.58), p, edge * 2);
            }
            case STAR, STORM, CROWN -> star(level, s, s.radius() * 0.62, s.radius() * 0.28,
                    family == SpellWorldLore.SigilFamily.CROWN ? 9 : family == SpellWorldLore.SigilFamily.STORM ? 8 : 6,
                    rotation, p, edge);
            case HEX, SEAL -> {
                polygon(level, s, s.radius() * 0.62, family == SpellWorldLore.SigilFamily.HEX ? 6 : 5, rotation, p, edge);
                polygon(level, s, s.radius() * 0.38, family == SpellWorldLore.SigilFamily.HEX ? 6 : 5,
                        rotation + Math.PI, p, edge);
            }
            case PORTAL -> {
                polygon(level, s, s.radius() * 0.60, 4, rotation, p, edge);
                polygon(level, s, s.radius() * 0.34, 4, rotation + Math.PI / 4.0, p, edge);
            }
            case EYE -> {
                arc(level, s, s.radius() * 0.62, 0, Math.PI, rotation, p, 15);
                arc(level, s, s.radius() * 0.62, Math.PI, Math.PI * 2, rotation, p, 15);
                ring(level, s, s.radius() * 0.22, p, 16);
            }
            case CLOCK -> {
                ring(level, s, s.radius() * 0.58, p, 26);
                line(level, s.center(), point(s, Math.cos(rotation) * s.radius() * 0.48,
                        Math.sin(rotation) * s.radius() * 0.48), p, 10);
            }
            case SPIRAL -> spiral(level, s, s.radius() * 0.62, rotation, p, release ? 34 : 24);
        }
    }

    private static void radialCompartments(ServerLevel level, Seal s, int divisions, ParticleOptions p,
                                           int points, double rotation) {
        for (int i = 0; i < divisions; i++) {
            double angle = rotation + Math.PI * 2.0 * i / divisions;
            line(level, polar(s, s.radius() * 0.70, angle), polar(s, s.radius() * 0.98, angle), p, points);
        }
    }

    private static void nodeMarks(ServerLevel level, Seal s, SpellDefinition spell, ParticleOptions p, int count) {
        int nodes = Math.min(12, 4 + spell.circle());
        double offset = Math.floorMod(spell.id().hashCode(), 16) * Math.PI / 8.0;
        for (int i = 0; i < nodes; i++) {
            Vec3 point = polar(s, s.radius() * 0.78, offset + Math.PI * 2.0 * i / nodes);
            level.sendParticles(p, point.x, point.y, point.z, count, 0, 0, 0, 0);
        }
    }

    private static void signatureLines(ServerLevel level, Seal s, int signature, ParticleOptions p,
                                       double rotation, boolean release) {
        int spokes = 3 + signature % 5;
        for (int i = 0; i < spokes; i++) {
            double a = rotation + Math.PI * 2.0 * i / spokes;
            double b = a + Math.PI * (2 + signature % 3) / spokes;
            line(level, polar(s, s.radius() * 0.18, a), polar(s, s.radius() * 0.52, b), p, release ? 8 : 5);
        }
    }

    private static void runeTicks(ServerLevel level, Seal s, SpellDefinition spell, ParticleOptions p, boolean release) {
        int ticks = Math.min(20, 8 + spell.circle());
        double offset = Math.floorMod(spell.id().hashCode(), 36) * Math.PI / 18.0;
        for (int i = 0; i < ticks; i++) {
            double a = offset + Math.PI * 2.0 * i / ticks;
            double length = (i + spell.circle()) % 3 == 0 ? 0.09 : 0.05;
            line(level, polar(s, s.radius() * (1.01 - length), a), polar(s, s.radius() * 1.01, a), p,
                    release ? 3 : 2);
        }
    }

    private static ParticleOptions particle(SpellDefinition spell) {
        return switch (spell.school()) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case WIND -> ParticleTypes.CLOUD;
            case WARD -> ParticleTypes.ENCHANT;
            case LIFE -> ParticleTypes.HAPPY_VILLAGER;
            case SPACE -> ParticleTypes.REVERSE_PORTAL;
            default -> ParticleTypes.ENCHANT;
        };
    }

    private static void ring(ServerLevel level, Seal s, double radius, ParticleOptions p, int points) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2.0 * i / points;
            Vec3 v = polar(s, radius, a);
            level.sendParticles(p, v.x, v.y, v.z, 1, 0, 0, 0, 0);
        }
    }

    private static void polygon(ServerLevel level, Seal s, double radius, int sides, double rotation,
                                ParticleOptions p, int edgePoints) {
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < sides; i++) vertices.add(polar(s, radius, rotation - Math.PI / 2.0 + Math.PI * 2.0 * i / sides));
        for (int i = 0; i < sides; i++) line(level, vertices.get(i), vertices.get((i + 1) % sides), p, edgePoints);
    }

    private static void star(ServerLevel level, Seal s, double outer, double inner, int points, double rotation,
                             ParticleOptions p, int edgePoints) {
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < points * 2; i++) {
            double radius = i % 2 == 0 ? outer : inner;
            vertices.add(polar(s, radius, rotation - Math.PI / 2.0 + Math.PI * i / points));
        }
        for (int i = 0; i < vertices.size(); i++) line(level, vertices.get(i), vertices.get((i + 1) % vertices.size()), p, edgePoints);
    }

    private static void spiral(ServerLevel level, Seal s, double radius, double rotation, ParticleOptions p, int points) {
        Vec3 previous = s.center();
        for (int i = 1; i <= points; i++) {
            double t = i / (double) points;
            Vec3 next = polar(s, radius * t, rotation + t * Math.PI * 4.0);
            line(level, previous, next, p, 2); previous = next;
        }
    }

    private static void arc(ServerLevel level, Seal s, double radius, double start, double end,
                            double rotation, ParticleOptions p, int points) {
        for (int i = 0; i <= points; i++) {
            Vec3 v = polar(s, radius, rotation + start + (end - start) * i / points);
            level.sendParticles(p, v.x, v.y, v.z, 1, 0, 0, 0, 0);
        }
    }

    private static void line(ServerLevel level, Vec3 a, Vec3 b, ParticleOptions p, int points) {
        int safe = Math.max(2, points);
        for (int i = 0; i <= safe; i++) {
            Vec3 v = a.lerp(b, i / (double) safe);
            level.sendParticles(p, v.x, v.y, v.z, 1, 0, 0, 0, 0);
        }
    }

    private static Vec3 polar(Seal s, double radius, double angle) {
        return s.center().add(s.right().scale(Math.cos(angle) * radius)).add(s.up().scale(Math.sin(angle) * radius));
    }
    private static Vec3 point(Seal s, double x, double y) { return s.center().add(s.right().scale(x)).add(s.up().scale(y)); }

    private static Vec3 aimGround(ServerPlayer player, double range) {
        Vec3 end = player.getEyePosition().add(player.getLookAngle().normalize().scale(range));
        return new Vec3(end.x, Math.max(player.level().getMinY() + 1, end.y), end.z);
    }

    private static Optional<Mob> target(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition(); Vec3 look = player.getLookAngle().normalize();
        return player.level().getEntitiesOfClass(Mob.class, new AABB(eye, eye.add(look.scale(range))).inflate(2.0),
                mob -> mob.isAlive() && (!(mob instanceof TamableAnimal tame) || !tame.isTame() || !tame.isOwnedBy(player))).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(eye); double projection = to.dot(look);
                    return projection >= 0 && projection <= range
                            && to.subtract(look.scale(projection)).length() <= Math.max(1.2, mob.getBbWidth() + 0.8);
                }).min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
    }

    private record Anchor(Vec3 center, Vec3 right, Vec3 up) {}
    private record Seal(Vec3 center, Vec3 right, Vec3 up, double radius) {}
}
