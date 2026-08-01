
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

public final class SpellSigilService {
    private SpellSigilService() {}

    public static void renderCharge(ServerPlayer player, SpellDefinition spell, double effectiveRange) {
        render(player, spell, effectiveRange, false);
    }

    public static void renderRelease(ServerPlayer player, SpellDefinition spell, double effectiveRange) {
        render(player, spell, effectiveRange, true);
    }

    private static void render(ServerPlayer player, SpellDefinition spell, double range, boolean release) {
        ServerLevel level = (ServerLevel) player.level();
        double rangeRatio = spell.range() <= 0.0 ? 1.0 : Math.max(0.75, Math.min(3.2, range / spell.range()));
        SpellWorldLore.SigilFamily family = SpellWorldLore.sigilFamily(spell.id());
        double familyScale = switch (family) {
            case LANCE -> 0.88; case STAR -> 1.20; case HEX -> 1.12; case PORTAL -> 1.18;
            case EYE -> 0.98; case SEAL -> 1.10; case CLOCK -> 1.30; case SPIRAL -> 1.05;
            case STORM -> 1.45; case CROWN -> 1.38;
        };
        double radius = (0.62 + spell.circle() * 0.17) * Math.sqrt(rangeRatio) * familyScale
                * (release ? 1.18 : 1.0);
        Anchor anchor = anchor(player, spell, range);
        draw(level, anchor.center(), anchor.right(), anchor.up(), spell, family, radius, release);
    }

    private static Anchor anchor(ServerPlayer player, SpellDefinition spell, double range) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 upReference = Math.abs(look.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = look.cross(upReference).normalize();
        Vec3 up = right.cross(look).normalize();
        return switch (spell.sigilAnchor()) {
            case FRONT -> new Anchor(player.getEyePosition().add(look.scale(1.32 + spell.circle() * 0.07)), right, up);
            case FEET, GROUND_SELF -> horizontal(player.position().add(0, 0.10, 0));
            case BODY -> new Anchor(player.position().add(0, 1.0, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1));
            case GROUND_TARGET -> horizontal(aimGround(player, Math.max(5.0, range)));
            case TARGET -> horizontal(target(player, Math.max(7.0, range)).map(Mob::position)
                    .orElse(player.getEyePosition().add(look.scale(2.2))).add(0, 0.08, 0));
        };
    }

    private static Anchor horizontal(Vec3 center) {
        return new Anchor(center, new Vec3(1, 0, 0), new Vec3(0, 0, 1));
    }

    private static void draw(ServerLevel level, Vec3 center, Vec3 right, Vec3 up, SpellDefinition spell,
                             SpellWorldLore.SigilFamily family, double r, boolean release) {
        ParticleOptions main = particle(spell);
        int circle = spell.circle();
        int quality = release ? 2 : 1;
        int signature = Math.floorMod(spell.id().hashCode(), 997);
        double rotation = (signature % 24) * Math.PI / 12.0;
        ring(level, center, right, up, r, ParticleTypes.END_ROD, 42 + circle * 5);
        for (int i = 1; i <= Math.min(4, 1 + circle / 2); i++) {
            ring(level, center, right, up, r * (1.0 - i * 0.14), i % 2 == 0 ? ParticleTypes.END_ROD : main,
                    30 + circle * 3);
        }
        switch (family) {
            case LANCE -> {
                polygon(level, center, right, up, r * 0.78, 3 + circle % 3, main, 10);
                line(level, center.add(up.scale(-r * 0.75)), center.add(up.scale(r * 0.75)), ParticleTypes.END_ROD, 18);
                line(level, center.add(right.scale(-r * 0.35)), center.add(right.scale(r * 0.35)), main, 12);
            }
            case STAR -> {
                star(level, center, right, up, r * 0.82, r * 0.35, 5 + circle % 4, main);
                ring(level, center, right, up, r * 0.23, ParticleTypes.END_ROD, 20);
            }
            case HEX -> {
                polygon(level, center, right, up, r * 0.84, 6, ParticleTypes.END_ROD, 12);
                polygon(level, center, right, up, r * 0.58, 6, main, 10);
                for (int i = 0; i < 6; i++) radial(level, center, right, up, r * 0.25, r * 0.82,
                        i * Math.PI / 3.0, main);
            }
            case PORTAL -> {
                polygon(level, center, right, up, r * 0.82, 4, main, 14);
                polygon(level, center, right, up, r * 0.58, 4, ParticleTypes.END_ROD, 12);
                polygon(level, center, right, up, r * 0.34, 4, main, 10);
                line(level, center.add(right.scale(-r * 0.78)).add(up.scale(-r * 0.78)),
                        center.add(right.scale(r * 0.78)).add(up.scale(r * 0.78)), ParticleTypes.PORTAL, 20);
                line(level, center.add(right.scale(-r * 0.78)).add(up.scale(r * 0.78)),
                        center.add(right.scale(r * 0.78)).add(up.scale(-r * 0.78)), ParticleTypes.PORTAL, 20);
            }
            case EYE -> {
                arc(level, center, right, up, r * 0.84, 0.0, Math.PI, main, 24);
                arc(level, center, right, up.scale(-1), r * 0.84, 0.0, Math.PI, main, 24);
                ring(level, center, right, up, r * 0.28, ParticleTypes.END_ROD, 24);
                line(level, center.add(up.scale(-r * 0.48)), center.add(up.scale(r * 0.48)), main, 14);
            }
            case SEAL -> {
                polygon(level, center, right, up, r * 0.80, 5, main, 12);
                polygon(level, center, right, up, r * 0.48, 5, ParticleTypes.END_ROD, 10);
                line(level, center.add(right.scale(-r * 0.65)), center.add(right.scale(r * 0.65)), main, 16);
                line(level, center.add(up.scale(-r * 0.65)), center.add(up.scale(r * 0.65)), main, 16);
            }
            case CLOCK -> {
                ring(level, center, right, up, r * 0.82, main, 54);
                for (int i = 0; i < 12; i++) radial(level, center, right, up, r * 0.68, r * 0.82,
                        i * Math.PI / 6.0, ParticleTypes.END_ROD);
                line(level, center, center.add(up.scale(r * 0.56)), main, 14);
                line(level, center, center.add(right.scale(r * 0.40)), ParticleTypes.END_ROD, 12);
            }
            case SPIRAL -> {
                spiral(level, center, right, up, r * 0.82, main, 58);
                spiral(level, center, right.scale(-1), up, r * 0.58, ParticleTypes.END_ROD, 42);
            }
            case STORM -> {
                star(level, center, right, up, r * 0.86, r * 0.45, 8, main);
                for (int i = 0; i < 4; i++) arc(level, center, right, up, r * (0.28 + i * 0.14),
                        i * 0.6, i * 0.6 + Math.PI * 1.35, i % 2 == 0 ? main : ParticleTypes.END_ROD, 26);
            }
            case CROWN -> {
                polygon(level, center, right, up, r * 0.84, 9, ParticleTypes.END_ROD, 10);
                star(level, center, right, up, r * 0.68, r * 0.28, 9, main);
            }
        }
        // Every spell receives a stable signature glyph in addition to its discipline family.
        int signatureSides = 3 + signature % 7;
        polygonRotated(level, center, right, up, r * (0.30 + (signature % 4) * 0.07),
                signatureSides, rotation, signature % 2 == 0 ? main : ParticleTypes.END_ROD, 7);
        int spokes = 2 + signature % 6;
        for (int i = 0; i < spokes; i++) {
            radial(level, center, right, up, r * 0.12, r * (0.46 + (signature % 3) * 0.08),
                    rotation + Math.PI * 2.0 * i / spokes, i % 2 == 0 ? main : ParticleTypes.END_ROD);
        }
        int runes = Math.min(24, 5 + circle * 2 + signature % 4);
        for (int i = 0; i < runes; i++) {
            double angle = Math.PI * 2.0 * i / runes;
            Vec3 p = center.add(right.scale(Math.cos(angle) * r * 1.08)).add(up.scale(Math.sin(angle) * r * 1.08));
            level.sendParticles(i % 3 == 0 ? ParticleTypes.END_ROD : main, p.x, p.y, p.z, quality, 0, 0, 0, 0);
        }
    }

    private static ParticleOptions particle(SpellDefinition spell) {
        return switch (spell.school()) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case WIND -> ParticleTypes.CLOUD;
            case WARD -> ParticleTypes.END_ROD;
            case LIFE -> ParticleTypes.HAPPY_VILLAGER;
            case SPACE -> ParticleTypes.REVERSE_PORTAL;
            default -> ParticleTypes.ENCHANT;
        };
    }

    private static void ring(ServerLevel level, Vec3 c, Vec3 r, Vec3 u, double radius,
                             ParticleOptions particle, int points) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2.0 * i / points;
            Vec3 p = c.add(r.scale(Math.cos(a) * radius)).add(u.scale(Math.sin(a) * radius));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static void polygon(ServerLevel level, Vec3 c, Vec3 r, Vec3 u, double radius,
                                int sides, ParticleOptions particle, int edgePoints) {
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < sides; i++) {
            double a = -Math.PI / 2.0 + Math.PI * 2.0 * i / sides;
            vertices.add(c.add(r.scale(Math.cos(a) * radius)).add(u.scale(Math.sin(a) * radius)));
        }
        for (int i = 0; i < sides; i++) line(level, vertices.get(i), vertices.get((i + 1) % sides), particle, edgePoints);
    }

    private static void polygonRotated(ServerLevel level, Vec3 c, Vec3 r, Vec3 u, double radius,
                                       int sides, double rotation, ParticleOptions particle, int edgePoints) {
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < sides; i++) {
            double a = rotation - Math.PI / 2.0 + Math.PI * 2.0 * i / sides;
            vertices.add(c.add(r.scale(Math.cos(a) * radius)).add(u.scale(Math.sin(a) * radius)));
        }
        for (int i = 0; i < sides; i++)
            line(level, vertices.get(i), vertices.get((i + 1) % sides), particle, edgePoints);
    }

    private static void star(ServerLevel level, Vec3 c, Vec3 r, Vec3 u, double outer, double inner,
                             int points, ParticleOptions particle) {
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < points * 2; i++) {
            double a = -Math.PI / 2.0 + Math.PI * i / points;
            double radius = i % 2 == 0 ? outer : inner;
            vertices.add(c.add(r.scale(Math.cos(a) * radius)).add(u.scale(Math.sin(a) * radius)));
        }
        for (int i = 0; i < vertices.size(); i++) line(level, vertices.get(i), vertices.get((i + 1) % vertices.size()), particle, 8);
    }

    private static void spiral(ServerLevel level, Vec3 c, Vec3 r, Vec3 u, double radius,
                               ParticleOptions particle, int points) {
        Vec3 previous = c;
        for (int i = 1; i <= points; i++) {
            double t = i / (double) points;
            double a = t * Math.PI * 5.0;
            Vec3 next = c.add(r.scale(Math.cos(a) * radius * t)).add(u.scale(Math.sin(a) * radius * t));
            line(level, previous, next, particle, 2);
            previous = next;
        }
    }

    private static void arc(ServerLevel level, Vec3 c, Vec3 r, Vec3 u, double radius,
                            double start, double end, ParticleOptions particle, int points) {
        for (int i = 0; i <= points; i++) {
            double a = start + (end - start) * i / points;
            Vec3 p = c.add(r.scale(Math.cos(a) * radius)).add(u.scale(Math.sin(a) * radius));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static void radial(ServerLevel level, Vec3 c, Vec3 r, Vec3 u, double inner, double outer,
                               double angle, ParticleOptions particle) {
        line(level, c.add(r.scale(Math.cos(angle) * inner)).add(u.scale(Math.sin(angle) * inner)),
                c.add(r.scale(Math.cos(angle) * outer)).add(u.scale(Math.sin(angle) * outer)), particle, 8);
    }

    private static void line(ServerLevel level, Vec3 a, Vec3 b, ParticleOptions particle, int points) {
        for (int i = 0; i <= points; i++) {
            Vec3 p = a.lerp(b, i / (double) points);
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static Vec3 aimGround(ServerPlayer player, double range) {
        Vec3 look = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(look.scale(range));
        return new Vec3(end.x, Math.max(player.level().getMinY() + 1, end.y), end.z);
    }

    private static Optional<Mob> target(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(2.5);
        return player.level().getEntitiesOfClass(Mob.class, box, mob -> mob.isAlive()
                        && !player.isAlliedTo(mob)
                        && (!(mob instanceof TamableAnimal tame) || !tame.isTame() || !tame.isOwnedBy(player))).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(eye);
                    double projection = to.dot(look);
                    return projection >= 0 && projection <= range
                            && to.subtract(look.scale(projection)).length() <= Math.max(1.2, mob.getBbWidth() + 0.8);
                }).min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
    }

    private record Anchor(Vec3 center, Vec3 right, Vec3 up) {}
}
