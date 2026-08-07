from __future__ import annotations

from pathlib import Path
import re

ROOT = Path.cwd()


def read(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def write(rel: str, text: str) -> None:
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_once(rel: str, old: str, new: str, marker: str | None = None) -> None:
    text = read(rel)
    if marker and marker in text:
        return
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{rel}: expected one target, found {count}: {old[:120]!r}")
    write(rel, text.replace(old, new, 1))


def replace_block(rel: str, start_marker: str, end_marker: str, replacement: str, done_marker: str) -> None:
    text = read(rel)
    if done_marker in text:
        return
    start = text.find(start_marker)
    end = text.find(end_marker, start + len(start_marker))
    if start < 0 or end < 0:
        raise SystemExit(f"{rel}: block markers not found: {start_marker!r} -> {end_marker!r}")
    write(rel, text[:start] + replacement + text[end:])


def patch_version() -> None:
    replace_once("gradle.properties", "mod_version=0.12.1-alpha.16", "mod_version=0.12.1-alpha.17", "mod_version=0.12.1-alpha.17")
    path = "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java"
    text = read(path)
    if 'public static final String VERSION = "0.12.1-alpha.17";' not in text:
        text, n = re.subn(r'public static final String VERSION = "0\.12\.1-alpha\.\d+";',
                          'public static final String VERSION = "0.12.1-alpha.17";', text, count=1)
        if n != 1:
            raise SystemExit(f"{path}: VERSION constant not found")
        write(path, text)


def write_presentation_profile() -> None:
    path = "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellPresentationProfile.java"
    content = r'''package kr.moonseungjun.arcanecircle.magic;

import java.util.HashMap;
import java.util.Map;

/**
 * Visual/kinetic identity of a spell. Circle rank limits how sophisticated a formula may be, but
 * it does not dictate physical size: a 9C death command can be compact while Meteor Strike owns
 * the sky. Values are authored per spell where silhouette matters and fall back conservatively.
 */
public final class SpellPresentationProfile {
    public enum SigilStyle {
        FRONT_COMPACT, FRONT_LANCE, GROUND_SEAL, TARGET_SEAL, BODY_HALO, FEET_RUNE,
        SKY_RITUAL, QUAD_ARRAY, WALL_MATRIX, PORTAL_GATE
    }
    public enum MotionStyle {
        SNAP, DART, BOLT, HEAVY_ORB, MISSILE_SWARM, LANCE, BEAM, WAVE,
        FIELD, SKY_DROP, STORM, PORTAL, PRISON, WALL, TARGET_BURST, AURA
    }

    public record Profile(SigilStyle sigil, MotionStyle motion, double radius, int complexity,
                          int satellites, double projectileSpeed, double skyHeight,
                          double releaseScale, int fixedImpactTicks) {
        public Profile {
            radius = Math.max(0.35, radius);
            complexity = Math.max(1, Math.min(6, complexity));
            satellites = Math.max(0, Math.min(12, satellites));
            projectileSpeed = Math.max(0.0, projectileSpeed);
            skyHeight = Math.max(0.0, skyHeight);
            releaseScale = Math.max(0.45, releaseScale);
            fixedImpactTicks = Math.max(0, fixedImpactTicks);
        }
    }

    private static final Map<String, Profile> AUTHORED = new HashMap<>();

    static {
        // Low/mid projectile language: deliberately different speeds and launch silhouettes.
        put("magic_missile", FRONT_COMPACT, MISSILE_SWARM, 0.92, 2, 3, 56, 0, 0.90, 0);
        put("fire_bolt", FRONT_COMPACT, BOLT, 0.72, 1, 0, 44, 0, 0.86, 0);
        put("ray_of_frost", FRONT_LANCE, BEAM, 0.68, 2, 0, 0, 0, 0.86, 0);
        put("scorching_ray", FRONT_LANCE, BEAM, 1.10, 3, 3, 0, 0, 1.00, 0);
        put("fireball", FRONT_COMPACT, HEAVY_ORB, 1.38, 3, 0, 28, 0, 1.15, 0);
        put("lightning_bolt", FRONT_LANCE, BEAM, 1.18, 3, 0, 0, 0, 1.10, 0);
        put("ice_knife", FRONT_LANCE, DART, 0.88, 2, 0, 48, 0, 0.92, 0);
        put("chromatic_orb", FRONT_COMPACT, HEAVY_ORB, 1.18, 3, 0, 34, 0, 1.08, 0);
        put("cone_of_cold", FRONT_COMPACT, WAVE, 1.85, 4, 0, 0, 0, 1.18, 0);
        put("chain_lightning", FRONT_LANCE, BEAM, 1.45, 4, 4, 0, 0, 1.16, 0);

        // High-circle spells: size follows fiction, not rank.
        put("flame_strike", SKY_RITUAL, SKY_DROP, 6.4, 4, 1, 0, 13, 1.45, 10);
        put("disintegrate", FRONT_LANCE, BEAM, 1.25, 5, 0, 0, 0, 1.16, 0);
        put("sunbeam", FRONT_LANCE, BEAM, 2.10, 4, 1, 0, 0, 1.34, 0);
        put("freezing_sphere", FRONT_COMPACT, HEAVY_ORB, 1.72, 5, 0, 23, 0, 1.42, 0);
        put("circle_of_death", GROUND_SEAL, FIELD, 5.2, 5, 0, 0, 0, 1.35, 0);
        put("delayed_blast_fireball", TARGET_SEAL, TARGET_BURST, 3.4, 5, 4, 0, 0, 1.65, 18);
        put("finger_of_death", TARGET_SEAL, TARGET_BURST, 2.15, 5, 0, 0, 0, 1.18, 2);
        put("fire_storm", SKY_RITUAL, SKY_DROP, 10.5, 5, 6, 0, 19, 1.78, 14);
        put("forcecage", TARGET_SEAL, PRISON, 3.25, 5, 4, 0, 0, 1.28, 2);
        put("prismatic_spray", FRONT_COMPACT, WAVE, 2.55, 5, 7, 0, 0, 1.44, 0);
        put("reverse_gravity", GROUND_SEAL, FIELD, 8.2, 5, 4, 0, 0, 1.48, 4);
        put("teleport", PORTAL_GATE, PORTAL, 4.2, 5, 2, 0, 0, 1.36, 0);
        put("antimagic_field", GROUND_SEAL, FIELD, 6.8, 5, 0, 0, 0, 1.34, 0);
        put("control_weather", SKY_RITUAL, STORM, 16.0, 5, 8, 0, 24, 1.82, 8);
        put("earthquake", QUAD_ARRAY, FIELD, 11.0, 5, 4, 0, 0, 1.74, 4);
        put("incendiary_cloud", SKY_RITUAL, STORM, 10.0, 5, 5, 0, 10, 1.62, 6);
        put("sunburst", SKY_RITUAL, SKY_DROP, 8.5, 5, 1, 0, 12, 1.64, 8);

        // 9C: deliberately non-monotonic physical scale.
        put("meteor_swarm", SKY_RITUAL, SKY_DROP, 16.0, 6, 4, 0, 28, 2.45, 28);
        put("power_word_kill", TARGET_SEAL, TARGET_BURST, 2.35, 6, 0, 0, 0, 1.28, 2);
        put("prismatic_wall", WALL_MATRIX, WALL, 10.5, 6, 4, 0, 0, 2.05, 4);
        put("shapechange", BODY_HALO, AURA, 2.85, 6, 3, 0, 0, 1.44, 0);
        put("time_stop", GROUND_SEAL, FIELD, 8.8, 6, 12, 0, 0, 1.72, 3);
        put("true_polymorph", TARGET_SEAL, TARGET_BURST, 3.6, 6, 4, 0, 0, 1.44, 3);
        put("weird", QUAD_ARRAY, FIELD, 7.4, 6, 4, 0, 0, 1.62, 3);
        put("wish", BODY_HALO, AURA, 3.1, 6, 6, 0, 0, 1.55, 0);
        put("gate", PORTAL_GATE, PORTAL, 10.5, 6, 4, 0, 0, 2.20, 0);
        put("foresight", BODY_HALO, AURA, 1.75, 6, 2, 0, 0, 1.20, 0);

        // Fusion identities.
        put("void_lance", FRONT_LANCE, LANCE, 1.45, 6, 2, 72, 0, 1.55, 0);
        put("winter_domain", QUAD_ARRAY, FIELD, 9.2, 6, 4, 0, 0, 1.65, 2);
        put("astral_prison", TARGET_SEAL, PRISON, 4.2, 6, 6, 0, 0, 1.62, 2);
        put("phoenix_requiem", SKY_RITUAL, STORM, 10.8, 6, 6, 0, 14, 1.90, 6);
        put("world_sunder", QUAD_ARRAY, FIELD, 14.0, 6, 4, 0, 0, 2.25, 5);
        put("solar_guard", BODY_HALO, AURA, 4.4, 5, 6, 0, 0, 1.46, 0);
        put("teleportation_circle", PORTAL_GATE, PORTAL, 5.0, 4, 4, 0, 0, 1.45, 0);
        put("thunder_cage", TARGET_SEAL, PRISON, 2.8, 4, 4, 0, 0, 1.26, 2);
    }

    private SpellPresentationProfile() {}

    private static void put(String id, SigilStyle sigil, MotionStyle motion, double radius,
                            int complexity, int satellites, double speed, double skyHeight,
                            double releaseScale, int impactTicks) {
        AUTHORED.put(id, new Profile(sigil, motion, radius, complexity, satellites, speed,
                skyHeight, releaseScale, impactTicks));
    }

    public static Profile profile(SpellDefinition spell) {
        Profile explicit = AUTHORED.get(spell.id());
        if (explicit != null) return explicit;
        int c = Math.max(1, Math.min(9, spell.circle()));
        int complexity = Math.max(1, Math.min(6, 1 + c / 2));
        double radius = 0.58 + c * 0.18;
        SigilStyle sigil = switch (spell.sigilAnchor()) {
            case FRONT -> SigilStyle.FRONT_COMPACT;
            case FEET -> SigilStyle.FEET_RUNE;
            case GROUND_SELF, GROUND_TARGET -> SigilStyle.GROUND_SEAL;
            case BODY -> SigilStyle.BODY_HALO;
            case TARGET -> SigilStyle.TARGET_SEAL;
        };
        MotionStyle motion = switch (SpellArchetype.mode(spell.id())) {
            case PROJECTILE -> MotionStyle.BOLT;
            case CHANNEL -> MotionStyle.BEAM;
            case FIELD -> MotionStyle.FIELD;
            case INSTANT -> switch (spell.sigilAnchor()) {
                case BODY, FEET -> MotionStyle.AURA;
                case GROUND_SELF, GROUND_TARGET -> MotionStyle.FIELD;
                case TARGET -> MotionStyle.TARGET_BURST;
                case FRONT -> MotionStyle.SNAP;
            };
        };
        double speed = motion == MotionStyle.BOLT ? Math.max(30.0, 48.0 - c * 1.4) : 0.0;
        return new Profile(sigil, motion, radius, complexity, 0, speed, 0.0,
                1.0 + Math.max(0, c - 5) * 0.05, 0);
    }

    public static int impactDelayTicks(SpellDefinition spell, double distance) {
        Profile profile = profile(spell);
        if (profile.fixedImpactTicks() > 0) return profile.fixedImpactTicks();
        if (profile.projectileSpeed() <= 0.0) return 0;
        double seconds = Math.max(0.0, distance) / profile.projectileSpeed();
        return Math.max(1, Math.min(34, (int) Math.round(seconds * 20.0)));
    }

    public static int releaseDurationTicks(SpellDefinition spell, double distance) {
        Profile profile = profile(spell);
        int impact = impactDelayTicks(spell, distance);
        return switch (profile.motion()) {
            case DART, BOLT, HEAVY_ORB, MISSILE_SWARM, LANCE -> Math.max(6, impact + 7);
            case SKY_DROP -> Math.max(18, impact + 16);
            case BEAM -> 12;
            case WAVE -> 18;
            case TARGET_BURST -> Math.max(14, impact + 12);
            case STORM -> 42;
            case PORTAL -> 34;
            case PRISON, WALL -> 30;
            case FIELD -> 34;
            case AURA -> 28;
            case SNAP -> 12;
        };
    }
}
'''
    write(path, content)


def patch_mesh() -> None:
    path = "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.java"
    text = read(path)
    text = text.replace("private static final double VIVID_SATURATION=1.72;", "private static final double VIVID_SATURATION=2.05;")
    text = text.replace("private static final double FACE_ALPHA_BOOST=1.52;", "private static final double FACE_ALPHA_BOOST=1.78;")
    text = text.replace("submitLines(poseStack,collector,tone(argb,.64,.24),windowScale*4.10F);",
                        "submitLines(poseStack,collector,tone(argb,.58,.34),windowScale*4.60F);")
    text = text.replace("submitLines(poseStack,collector,tone(argb,.88,.58),windowScale*2.05F);",
                        "submitLines(poseStack,collector,tone(argb,.82,.76),windowScale*2.20F);")
    text = text.replace("submitLines(poseStack,collector,tone(argb,1.06,1.0),windowScale*.82F);",
                        "submitLines(poseStack,collector,tone(argb,.98,1.0),windowScale*.78F);")
    if "Builder runeGlyph(" not in text:
        anchor = "        Builder sphere(Vec3 center,double radius,int detail,float width){"
        insert = r'''        Builder runeGlyph(Basis basis,Vec3 center,double size,int seed,double rotation,float width){
            Vec3 x=basis.point(rotation,size),y=basis.point(rotation+Math.PI/2.0,size);
            Vec3 nx=x.scale(-1),ny=y.scale(-1);
            int pattern=Math.floorMod(seed,8);
            line(center.add(nx),center.add(x),width);
            if((pattern&1)!=0)line(center.add(ny),center.add(y),width*.82F);
            if((pattern&2)!=0)line(center.add(nx.scale(.75)).add(ny.scale(.70)),center.add(x.scale(.70)).add(y.scale(.78)),width*.72F);
            if((pattern&4)!=0)line(center.add(nx.scale(.72)).add(y.scale(.68)),center.add(x.scale(.78)).add(ny.scale(.65)),width*.72F);
            double cap=size*.55;
            if(pattern%3==0)diamond(basis,center.add(y.scale(.72)),cap*.48,rotation,1.16F,.42F);
            else if(pattern%3==1)polygon(basis,center.add(x.scale(.42)),cap*.46,3,rotation+.35,width*.68F);
            else circle(basis,center.add(ny.scale(.58)),cap*.34,10,width*.62F);
            return this;
        }
        Builder runeRing(Basis basis,Vec3 center,double radius,int count,double size,int seed,double rotation,float width){
            int n=Math.max(4,count);
            for(int i=0;i<n&&!full();i++){
                double a=rotation+Math.PI*2.0*i/n;
                Vec3 at=center.add(basis.point(a,radius));
                runeGlyph(basis,at,size,seed+i,a+Math.PI/2.0,width*(i%5==0?1.22F:.76F));
            }
            return this;
        }
'''
        if anchor not in text:
            raise SystemExit(f"{path}: rune insertion anchor missing")
        text = text.replace(anchor, insert + anchor, 1)
    write(path, text)


def patch_world_magic_service() -> None:
    path = "src/main/java/kr/moonseungjun/arcanecircle/magic/WorldMagicService.java"
    content = r'''package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Server-authoritative visual event broadcaster for both players and NPC mages. */
public final class WorldMagicService {
    private WorldMagicService() {}

    public static void charge(ServerPlayer player, SpellDefinition spell, boolean fusion,
                              List<String> ingredients, double range, double progress) {
        Vec3 direction = safeDirection(player.getLookAngle());
        Vec3 target = targetPoint(player, spell, range, direction);
        Vec3 center = presentationCenter(player, spell, target, direction);
        send(player, encode("charge", player, spell, fusion, ingredients.size(), center, target,
                direction, range, spell.power(), clamp01(progress), 8, 0));
    }

    public static void release(ServerPlayer player, MagicPlayerData.CastPreparation cast) {
        SpellDefinition spell = cast.spell();
        Vec3 direction = safeDirection(player.getLookAngle());
        Vec3 target = targetPoint(player, spell, cast.range(), direction);
        Vec3 center = presentationCenter(player, spell, target, direction);
        double travelDistance = Math.max(0.0, target.distanceTo(center));
        int impactTicks = SpellPresentationProfile.impactDelayTicks(spell, kineticDistanceForVisual(player, spell, cast.range(), center, target));
        int duration = SpellPresentationProfile.releaseDurationTicks(spell, travelDistance);
        send(player, encode("release", player, spell, cast.fusion(), cast.ingredients().size(), center, target,
                direction, cast.range(), cast.power(), 1.0, duration, impactTicks));
    }

    public static void charge(LivingEntity caster, LivingEntity targetEntity, SpellDefinition spell,
                              double progress, double range, double power) {
        if (!(caster.level() instanceof ServerLevel)) return;
        Vec3 direction = targetEntity == null ? safeDirection(caster.getLookAngle())
                : safeDirection(targetEntity.getEyePosition().subtract(caster.getEyePosition()));
        Vec3 target = targetEntity == null ? caster.getEyePosition().add(direction.scale(Math.max(3.0, range)))
                : targetEntity.position();
        Vec3 center = presentationCenter(caster, spell, target, direction);
        send(caster, encode("charge", caster, spell, false, 0, center, target, direction,
                range, power, clamp01(progress), 8, 0));
    }

    public static void release(LivingEntity caster, LivingEntity targetEntity, SpellDefinition spell,
                               double range, double power) {
        if (!(caster.level() instanceof ServerLevel)) return;
        Vec3 direction = targetEntity == null ? safeDirection(caster.getLookAngle())
                : safeDirection(targetEntity.getEyePosition().subtract(caster.getEyePosition()));
        Vec3 target = targetEntity == null ? caster.getEyePosition().add(direction.scale(Math.max(3.0, range)))
                : targetEntity.position();
        Vec3 center = presentationCenter(caster, spell, target, direction);
        double distance = target.distanceTo(center);
        int impact = SpellPresentationProfile.impactDelayTicks(spell, Math.max(0.0, distance));
        int duration = SpellPresentationProfile.releaseDurationTicks(spell, Math.max(0.0, distance));
        send(caster, encode("release", caster, spell, false, 0, center, target, direction,
                range, power, 1.0, duration, impact));
    }

    public static void stop(ServerPlayer player) { stop((LivingEntity) player); }
    public static void stop(LivingEntity caster) { send(caster, "kind=stop;caster=" + caster.getUUID()); }

    private static void send(LivingEntity caster, String state) {
        if (!(caster.level() instanceof ServerLevel level)) return;
        PacketDistributor.sendToPlayersNear(level, null, caster.getX(), caster.getY(), caster.getZ(),
                160.0, new WorldMagicPayload(state));
    }

    private static String encode(String kind, LivingEntity caster, SpellDefinition spell, boolean fusion,
                                 int ingredientCount, Vec3 center, Vec3 target, Vec3 direction, double range,
                                 double power, double progress, int duration, int impactTicks) {
        return String.format(Locale.ROOT,
                "kind=%s;caster=%s;spell=%s;fusion=%d;ingredients=%d;x=%.5f;y=%.5f;z=%.5f;tx=%.5f;ty=%.5f;tz=%.5f;dx=%.5f;dy=%.5f;dz=%.5f;range=%.4f;power=%.4f;progress=%.4f;duration=%d;impact=%d",
                kind, caster.getUUID(), spell.id(), fusion ? 1 : 0, ingredientCount,
                center.x, center.y, center.z, target.x, target.y, target.z,
                direction.x, direction.y, direction.z, range, power, progress, duration, impactTicks);
    }

    private static Vec3 presentationCenter(LivingEntity caster, SpellDefinition spell, Vec3 target, Vec3 look) {
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        return switch (profile.sigil()) {
            case SKY_RITUAL -> target.add(0.0, profile.skyHeight(), 0.0);
            case TARGET_SEAL -> target.add(0.0, 1.05, 0.0);
            case GROUND_SEAL, QUAD_ARRAY -> target.add(0.0, 0.055, 0.0);
            case WALL_MATRIX -> target.add(0.0, Math.max(1.2, profile.radius() * 0.34), 0.0);
            case PORTAL_GATE -> target.add(0.0, Math.max(1.1, profile.radius() * 0.52), 0.0);
            case BODY_HALO -> caster.position().add(0.0, 1.0, 0.0);
            case FEET_RUNE -> caster.position().add(0.0, 0.055, 0.0);
            case FRONT_COMPACT, FRONT_LANCE -> caster instanceof ServerPlayer player
                    ? visibleFrontAnchor(player, profile, look)
                    : caster.getEyePosition().add(safeDirection(look).scale(profile.sigil() == SpellPresentationProfile.SigilStyle.FRONT_LANCE ? 1.15 : 1.00));
        };
    }

    private static Vec3 targetPoint(ServerPlayer player, SpellDefinition spell, double range, Vec3 look) {
        Optional<Mob> target = aimedMob(player, range);
        return switch (spell.sigilAnchor()) {
            case GROUND_TARGET -> target.map(mob -> groundUnder(player, mob.position())).orElseGet(() -> aimGround(player, Math.max(4.0, range)));
            case TARGET -> target.<Vec3>map(Mob::getEyePosition).orElseGet(() -> visiblePoint(player, look, Math.min(Math.max(3.0, range), 72.0)));
            case FRONT -> target.<Vec3>map(Mob::getEyePosition).orElseGet(() -> visiblePoint(player, look, Math.min(Math.max(3.0, range), 72.0)));
            case FEET, GROUND_SELF -> player.position().add(0.0, 0.055, 0.0);
            case BODY -> player.position().add(0.0, 1.0, 0.0);
        };
    }

    private static Optional<Mob> aimedMob(ServerPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = safeDirection(player.getLookAngle());
        double max = Math.min(80.0, Math.max(2.0, range));
        AABB box = player.getBoundingBox().expandTowards(look.scale(max)).inflate(2.4);
        return player.level().getEntitiesOfClass(Mob.class, box, mob -> mob.isAlive()
                        && !player.isAlliedTo(mob)
                        && (!(mob instanceof TamableAnimal tame) || !tame.isTame() || !tame.isOwnedBy(player))).stream()
                .filter(mob -> {
                    Vec3 to = mob.getEyePosition().subtract(eye);
                    double projection = to.dot(look);
                    return projection >= 0.0 && projection <= max
                            && to.subtract(look.scale(projection)).length() <= Math.max(1.25, mob.getBbWidth() + 0.8);
                })
                .min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
    }

    private static Vec3 groundUnder(ServerPlayer player, Vec3 around) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos base = BlockPos.containing(around);
        for (int down = 0; down <= 8; down++) {
            BlockPos floor = base.below(down);
            BlockState state = level.getBlockState(floor);
            if (state.isFaceSturdy(level, floor, Direction.UP))
                return Vec3.atCenterOf(floor.above()).add(0.0, -0.48, 0.0);
        }
        return around;
    }

    private static Vec3 visibleFrontAnchor(ServerPlayer player, SpellPresentationProfile.Profile profile, Vec3 look) {
        double desired = profile.sigil() == SpellPresentationProfile.SigilStyle.FRONT_LANCE ? 1.18 : 0.96;
        return visiblePoint(player, look, desired);
    }

    private static Vec3 visiblePoint(ServerPlayer player, Vec3 look, double desiredDistance) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 origin = player.getEyePosition();
        Vec3 direction = safeDirection(look);
        double minDistance = 0.44;
        Vec3 lastVisible = origin.add(direction.scale(minDistance));
        double max = Math.max(minDistance, desiredDistance);
        for (double distance = minDistance; distance <= max + 1.0E-6; distance += 0.08) {
            Vec3 point = origin.add(direction.scale(distance));
            BlockPos pos = BlockPos.containing(point);
            BlockState state = level.getBlockState(pos);
            if (!state.getCollisionShape(level, pos).isEmpty()) break;
            lastVisible = point;
        }
        return lastVisible;
    }

    private static Vec3 aimGround(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = safeDirection(player.getLookAngle());
        Vec3 origin = player.getEyePosition();
        Vec3 bestVisibleFloor = null;
        double max = Math.min(Math.max(2.0, range), 72.0);
        for (double distance = 0.70; distance <= max; distance += 0.32) {
            Vec3 sample = origin.add(look.scale(distance));
            BlockPos samplePos = BlockPos.containing(sample);
            BlockState sampleState = level.getBlockState(samplePos);
            if (!sampleState.getCollisionShape(level, samplePos).isEmpty()) {
                if (sampleState.isFaceSturdy(level, samplePos, Direction.UP))
                    bestVisibleFloor = Vec3.atCenterOf(samplePos.above()).add(0.0, -0.48, 0.0);
                break;
            }
            for (int down = 0; down <= 12; down++) {
                BlockPos floor = samplePos.below(down);
                BlockState state = level.getBlockState(floor);
                if (state.isFaceSturdy(level, floor, Direction.UP)) {
                    bestVisibleFloor = Vec3.atCenterOf(floor.above()).add(0.0, -0.48, 0.0);
                    break;
                }
            }
        }
        if (bestVisibleFloor != null) return bestVisibleFloor;
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-8) flat = new Vec3(0.0, 0.0, 1.0);
        return player.position().add(flat.normalize().scale(Math.min(3.0, range))).add(0.0, 0.055, 0.0);
    }

    private static double kineticDistanceForVisual(ServerPlayer player, SpellDefinition spell, double range,
                                                   Vec3 center, Vec3 target) {
        if (SpellPresentationProfile.profile(spell).motion() == SpellPresentationProfile.MotionStyle.SKY_DROP)
            return Math.max(0.0, target.distanceTo(center));
        return aimedMob(player, range).map(mob -> mob.getEyePosition().distanceTo(center))
                .orElse(Math.max(0.0, target.distanceTo(center)));
    }

    private static Vec3 safeDirection(Vec3 value) {
        return value == null || value.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : value.normalize();
    }
    private static double clamp01(double value) { return Math.max(0.0, Math.min(1.0, value)); }
}
'''
    write(path, content)


def patch_kinetics() -> None:
    path = "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellKineticsService.java"
    text = read(path)
    if "projectileImpactDelay" not in text:
        old = '''        if (mode == SpellArchetype.Mode.INSTANT || mode == SpellArchetype.Mode.PROJECTILE) {
            boolean executed = SpellCastingService.executeResolved(player, cast.spell().id(),
                    cast.range(), cast.power());
            SpellCastingService.finishKineticCast(player, cast, snapshot, executed);
            return executed;
        }
'''
        new = '''        if (mode == SpellArchetype.Mode.INSTANT) {
            boolean executed = SpellCastingService.executeResolved(player, cast.spell().id(),
                    cast.range(), cast.power());
            SpellCastingService.finishKineticCast(player, cast, snapshot, executed);
            return executed;
        }
        if (mode == SpellArchetype.Mode.PROJECTILE) {
            int projectileImpactDelay = SpellPresentationProfile.impactDelayTicks(cast.spell(),
                    SpellCastingService.kineticDistance(player, cast.range()));
            if (projectileImpactDelay <= 1) {
                boolean executed = SpellCastingService.executeResolved(player, cast.spell().id(), cast.range(), cast.power());
                SpellCastingService.finishKineticCast(player, cast, snapshot, executed);
                return executed;
            }
            enqueue(player, new PendingCast(cast, snapshot, clock(player) + projectileImpactDelay,
                    0, 1, cast.power(), false));
            return true;
        }
'''
        if old not in text:
            raise SystemExit(f"{path}: instant/projectile block changed")
        text = text.replace(old, new, 1)
        old_queue = '''        List<PendingCast> queue = PENDING.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
        while (queue.size() >= MAX_PENDING_PER_PLAYER) {
            PendingCast dropped = queue.removeFirst();
            SpellCastingService.finishKineticCast(player, dropped.cast(), dropped.snapshot(), dropped.anyExecuted());
        }
        queue.add(new PendingCast(cast, snapshot, clock(player) + interval, interval,
                remaining, pulsePower, first));
        return true;
'''
        new_queue = '''        enqueue(player, new PendingCast(cast, snapshot, clock(player) + interval, interval,
                remaining, pulsePower, first));
        return true;
'''
        if old_queue not in text:
            raise SystemExit(f"{path}: pending queue block changed")
        text = text.replace(old_queue, new_queue, 1)
        anchor = '''    public static void tick(ServerPlayer player) {
'''
        helper = '''    private static void enqueue(ServerPlayer player, PendingCast pending) {
        List<PendingCast> queue = PENDING.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
        while (queue.size() >= MAX_PENDING_PER_PLAYER) {
            PendingCast dropped = queue.removeFirst();
            SpellCastingService.finishKineticCast(player, dropped.cast(), dropped.snapshot(), dropped.anyExecuted());
        }
        queue.add(pending);
    }

'''
        text = text.replace(anchor, helper + anchor, 1)
    write(path, text)


def patch_cast_distance() -> None:
    path = "src/main/java/kr/moonseungjun/arcanecircle/magic/SpellCastingService.java"
    text = read(path)
    if "static double kineticDistance(ServerPlayer player, double range)" in text:
        return
    anchor = '''    private static MagicPlayerData data(ServerPlayer player) {
'''
    helper = '''    static double kineticDistance(ServerPlayer player, double range) {
        Vec3 start = frontOrigin(player, 1.0);
        Optional<Mob> target = lookTarget(player, range);
        return target.map(mob -> Math.max(0.0, mob.getEyePosition().distanceTo(start)))
                .orElse(Math.max(1.0, range));
    }

'''
    if anchor not in text:
        raise SystemExit(f"{path}: kineticDistance insertion anchor missing")
    write(path, text.replace(anchor, helper + anchor, 1))


def patch_tracker() -> None:
    path = "src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java"
    text = read(path)
    if "SpellPresentationProfile" not in text:
        text = text.replace("import kr.moonseungjun.arcanecircle.magic.SpellMetrics;\n",
                            "import kr.moonseungjun.arcanecircle.magic.SpellMetrics;\nimport kr.moonseungjun.arcanecircle.magic.SpellPresentationProfile;\n", 1)
    text = re.sub(r'\n    private static final double\[\] GRAND_ARRAY_RADIUS = \{[^\n]+\};', '', text, count=1)

    if 'decimal(values, "tx"' not in text:
        old = '''        Vec3 center = new Vec3(decimal(values, "x", 0.0), decimal(values, "y", 0.0),
                decimal(values, "z", 0.0));
        Vec3 direction = safeDirection(new Vec3(decimal(values, "dx", 0.0),
                decimal(values, "dy", 0.0), decimal(values, "dz", 1.0)));
        double range = Math.max(0.1, decimal(values, "range", spell.range()));
        double power = Math.max(0.1, decimal(values, "power", Math.max(0.1, spell.power())));
        double progress = clamp(decimal(values, "progress", 1.0), 0.0, 1.0);
        int duration = Math.max(3, integer(values, "duration", 10));
'''
        new = '''        Vec3 center = new Vec3(decimal(values, "x", 0.0), decimal(values, "y", 0.0),
                decimal(values, "z", 0.0));
        Vec3 direction = safeDirection(new Vec3(decimal(values, "dx", 0.0),
                decimal(values, "dy", 0.0), decimal(values, "dz", 1.0)));
        double range = Math.max(0.1, decimal(values, "range", spell.range()));
        Vec3 target = new Vec3(decimal(values, "tx", center.x + direction.x * range),
                decimal(values, "ty", center.y + direction.y * range),
                decimal(values, "tz", center.z + direction.z * range));
        double power = Math.max(0.1, decimal(values, "power", Math.max(0.1, spell.power())));
        double progress = clamp(decimal(values, "progress", 1.0), 0.0, 1.0);
        int duration = Math.max(3, integer(values, "duration", 10));
        int impactTicks = Math.max(0, integer(values, "impact", 0));
        double impactAge = clamp(impactTicks / (double) Math.max(1, duration), 0.04, 0.92);
'''
        if old not in text:
            raise SystemExit(f"{path}: payload parse block changed")
        text = text.replace(old, new, 1)
        text = text.replace('new Visual(caster, spell, fusion, ingredients, center, direction,\n                    range, power, progress, started, now + CHARGE_TTL, false)',
                            'new Visual(caster, spell, fusion, ingredients, center, target, direction,\n                    range, power, progress, started, now + CHARGE_TTL, false, 0.0)', 1)
        text = text.replace('new Visual(caster, spell, fusion, ingredients, center, direction,\n                    range, power, 1.0, now, now + duration * 50_000_000L, true)',
                            'new Visual(caster, spell, fusion, ingredients, center, target, direction,\n                    range, power, 1.0, now, now + duration * 50_000_000L, true, impactAge)', 1)

    start = text.find("    private static ArcaneWorldMesh buildCharge(Visual visual) {")
    end = text.find("    private static ArcaneWorldMesh buildRelease(Visual visual, double age) {", start)
    if start < 0 or end < 0:
        raise SystemExit(f"{path}: buildCharge block missing")
    charge = r'''    private static ArcaneWorldMesh buildCharge(Visual visual) {
        SpellDefinition spell = visual.spell;
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(MAX_CHARGE_GEOMETRY);
        ArcaneWorldMesh.Basis basis = presentationBasis(profile, visual.direction);
        int complexity = profile.complexity();
        double p = Math.max(0.04, visual.progress);
        double time = Math.max(0.0, (System.nanoTime() - visual.startedAt) / 1_000_000_000.0);
        double outer = profile.radius() * (visual.fusion ? 1.12 : 1.0)
                * (0.97 + Math.sin(time * 2.2) * 0.03);
        double rotation = Math.floorMod(spell.id().hashCode(), 360) * Math.PI / 180.0 + time * 0.34;
        double counter = -rotation * 0.73;

        mesh.disc(basis, Vec3.ZERO, outer * 0.96, 52 + complexity * 12,
                0.20F, (float) (0.045 + p * 0.075));
        int bands = 1 + complexity;
        for (int layer = 0; layer < bands && !mesh.full(); layer++) {
            double t = layer / (double) Math.max(1, bands - 1);
            double r = outer * (0.20 + t * 0.76);
            double width = outer * (0.010 + (layer % 3) * 0.006);
            if ((layer & 1) == 0)
                mesh.band(basis, Vec3.ZERO, Math.max(0.01, r - width), r,
                        46 + complexity * 10, 1.18F, (float) (0.28 + p * 0.27));
            else
                mesh.brokenBand(basis, Vec3.ZERO, Math.max(0.01, r - width), r,
                        52 + complexity * 11, 4 + layer % 4, 1.12F, (float) (0.23 + p * 0.24));
        }
        schoolSeal(mesh, spell, basis, outer * 0.34, rotation, Math.min(1.0, p * 1.28));
        mesh.runeChords(basis, Vec3.ZERO, outer * 0.51, 7 + complexity * 3,
                2 + complexity % 4, counter, 0.92F);
        mesh.runeRing(basis, Vec3.ZERO, outer * 0.79, 8 + complexity * 4,
                outer * 0.025, spell.id().hashCode(), counter, 0.78F);

        switch (profile.sigil()) {
            case QUAD_ARRAY, WALL_MATRIX -> buildQuadArray(mesh, basis, spell, outer, complexity, rotation, p);
            case SKY_RITUAL -> buildSkyRitualArray(mesh, basis, spell, outer, complexity,
                    Math.max(1, profile.satellites()), rotation, p);
            case FRONT_LANCE -> buildLanceArray(mesh, basis, spell, outer, complexity, rotation, p);
            case PORTAL_GATE -> buildPortalArray(mesh, basis, spell, outer, complexity, rotation, p);
            case BODY_HALO -> buildHaloArray(mesh, basis, outer, complexity, rotation, p);
            case TARGET_SEAL -> buildTargetArray(mesh, basis, spell, outer, complexity, rotation, p);
            default -> {
                if (profile.satellites() > 0)
                    buildOrbitingSubArrays(mesh, basis, spell, outer, profile.satellites(), rotation, p);
            }
        }
        if (visual.fusion) {
            mesh.brokenBand(basis, Vec3.ZERO, outer * 1.08, outer * 1.14,
                    72 + complexity * 10, 6, 1.30F, 0.46F);
        }
        return mesh.build();
    }

    private static void buildQuadArray(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                       SpellDefinition spell, double outer, int complexity,
                                       double rotation, double p) {
        double d = outer * 0.78;
        Vec3[] nodes = {basis.right().scale(d).add(basis.up().scale(d)),
                basis.right().scale(-d).add(basis.up().scale(d)),
                basis.right().scale(-d).add(basis.up().scale(-d)),
                basis.right().scale(d).add(basis.up().scale(-d))};
        for (int i = 0; i < 4 && !mesh.full(); i++) {
            Vec3 a = nodes[i], b = nodes[(i + 1) % 4];
            mesh.line(a, b, 1.22F);
            double sub = outer * 0.27;
            mesh.band(basis, a, sub * 0.78, sub, 40, 1.28F, (float) (0.34 + p * 0.20));
            mesh.runeRing(basis, a, sub * 0.61, 8 + complexity * 2, sub * 0.055,
                    spell.id().hashCode() + i * 17, -rotation + i, 0.72F);
            schoolSeal(mesh, spell, basis, sub * 0.34, rotation + i, p);
        }
    }

    private static void buildSkyRitualArray(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                            SpellDefinition spell, double outer, int complexity,
                                            int satellites, double rotation, double p) {
        mesh.brokenBand(basis, Vec3.ZERO, outer * 1.03, outer * 1.085,
                88 + complexity * 14, 7, 1.34F, (float) (0.35 + p * 0.20));
        mesh.runeRing(basis, Vec3.ZERO, outer * 0.90, 16 + complexity * 6,
                outer * 0.020, spell.id().hashCode() ^ 0x5A17, rotation * 0.42, 0.82F);
        buildOrbitingSubArrays(mesh, basis, spell, outer, satellites, rotation, p);
        ArcaneWorldMesh.Basis tiltA = ArcaneWorldMesh.Basis.fromNormal(
                basis.right().add(basis.normal().scale(0.72)), basis.up());
        ArcaneWorldMesh.Basis tiltB = ArcaneWorldMesh.Basis.fromNormal(
                basis.up().add(basis.normal().scale(0.64)), basis.right());
        mesh.brokenBand(tiltA, Vec3.ZERO, outer * 0.56, outer * 0.61,
                76, 6, 1.16F, 0.34F);
        if (complexity >= 5)
            mesh.brokenBand(tiltB, Vec3.ZERO, outer * 0.42, outer * 0.47,
                    70, 5, 1.10F, 0.30F);
    }

    private static void buildOrbitingSubArrays(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                               SpellDefinition spell, double outer, int satellites,
                                               double rotation, double p) {
        int count = Math.max(1, Math.min(12, satellites));
        for (int i = 0; i < count && !mesh.full(); i++) {
            double angle = rotation * 0.36 + Math.PI * 2.0 * i / count;
            Vec3 node = basis.point(angle, outer * 1.22);
            double sub = outer * (count <= 4 ? 0.18 : 0.115);
            mesh.band(basis, node, sub * 0.74, sub, 34, 1.32F, (float) (0.36 + p * 0.18));
            mesh.runeRing(basis, node, sub * 0.55, 6 + Math.min(8, count), sub * 0.06,
                    spell.id().hashCode() + i * 31, -rotation + i, 0.68F);
            mesh.line(basis.point(angle, outer * 0.96), node, i % 3 == 0 ? 1.18F : 0.68F);
        }
    }

    private static void buildLanceArray(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                        SpellDefinition spell, double outer, int complexity,
                                        double rotation, double p) {
        mesh.star(basis, Vec3.ZERO, outer * 0.82, outer * 0.34,
                4 + complexity, rotation, 1.34F);
        for (int i = 0; i < Math.max(2, complexity - 1); i++) {
            double a = rotation + Math.PI * 2.0 * i / Math.max(2, complexity - 1);
            mesh.line(basis.point(a, outer * 0.18), basis.point(a, outer * 1.04), 1.06F);
        }
        mesh.runeRing(basis, Vec3.ZERO, outer * 0.68, 10 + complexity * 3,
                outer * 0.025, spell.id().hashCode(), -rotation, 0.80F);
    }

    private static void buildPortalArray(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                         SpellDefinition spell, double outer, int complexity,
                                         double rotation, double p) {
        mesh.band(basis, Vec3.ZERO, outer * 0.77, outer, 72 + complexity * 8,
                1.32F, (float) (0.38 + p * 0.22));
        mesh.brokenBand(basis, Vec3.ZERO, outer * 1.08, outer * 1.15,
                86, 7, 1.18F, 0.38F);
        mesh.runeRing(basis, Vec3.ZERO, outer * 0.88, 18 + complexity * 5,
                outer * 0.022, spell.id().hashCode(), rotation, 0.82F);
    }

    private static void buildHaloArray(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                       double outer, int complexity, double rotation, double p) {
        ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                basis.right().add(basis.normal().scale(0.82)), basis.up());
        mesh.brokenBand(tilt, Vec3.ZERO, outer * 0.72, outer * 0.80,
                68 + complexity * 6, 6, 1.18F, 0.34F);
        mesh.star(basis, Vec3.ZERO, outer * 0.62, outer * 0.26,
                4 + complexity, -rotation, 1.18F);
    }

    private static void buildTargetArray(ArcaneWorldMesh.Builder mesh, ArcaneWorldMesh.Basis basis,
                                         SpellDefinition spell, double outer, int complexity,
                                         double rotation, double p) {
        mesh.star(basis, Vec3.ZERO, outer * 0.86, outer * 0.48,
                5 + complexity, rotation * 0.56, 1.34F);
        mesh.runeRing(basis, Vec3.ZERO, outer * 0.64, 12 + complexity * 4,
                outer * 0.026, spell.id().hashCode() ^ 0x77, -rotation, 0.84F);
        if (complexity >= 5)
            mesh.brokenBand(basis, Vec3.ZERO, outer * 1.02, outer * 1.10,
                    82, 6, 1.34F, 0.44F);
    }

'''
    text = text[:start] + charge + text[end:]

    # Replace release dispatcher with authored presentation profile routing.
    start = text.find("    private static ArcaneWorldMesh buildRelease(Visual visual, double age) {")
    end = text.find("    private static void buildProjectile(", start)
    if start < 0 or end < 0:
        raise SystemExit(f"{path}: release dispatcher block missing")
    release = r'''    private static ArcaneWorldMesh buildRelease(Visual visual, double age) {
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(MAX_RELEASE_GEOMETRY);
        SpellDefinition spell = visual.spell;
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(spell);
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(visual.direction);
        double powerFactor = clamp(Math.pow(Math.max(0.08,
                visual.power / Math.max(1.0, spell.power())), 0.18), 0.82, 2.0) * profile.releaseScale();
        switch (profile.motion()) {
            case MISSILE_SWARM -> buildMissileSwarm(mesh, visual, facing, age, powerFactor);
            case HEAVY_ORB -> buildElementalOrb(mesh, visual, facing, age, powerFactor);
            case DART, BOLT -> buildProjectile(mesh, visual, facing, age, powerFactor);
            case LANCE -> buildLance(mesh, visual, facing, age, powerFactor);
            case BEAM -> buildBeam(mesh, visual, facing, age, powerFactor);
            case WAVE -> buildWave(mesh, visual, facing, age, powerFactor);
            case SKY_DROP -> buildMeteor(mesh, visual, age, powerFactor);
            case STORM -> buildStorm(mesh, visual, age, powerFactor);
            case PORTAL -> buildPortal(mesh, visual, age, powerFactor);
            case PRISON -> buildPrison(mesh, visual, age, powerFactor);
            case WALL -> buildWall(mesh, visual, age, powerFactor);
            case FIELD -> buildField(mesh, visual, age, powerFactor);
            case AURA -> buildAura(mesh, visual, age, powerFactor);
            case TARGET_BURST -> buildTargetBurst(mesh, visual, age, powerFactor);
            case SNAP -> {
                switch (spell.sigilAnchor()) {
                    case FEET, GROUND_SELF, GROUND_TARGET -> buildField(mesh, visual, age, powerFactor);
                    case BODY -> buildAura(mesh, visual, age, powerFactor);
                    case TARGET -> buildTargetBurst(mesh, visual, age, powerFactor);
                    case FRONT -> buildProjectile(mesh, visual, facing, age, powerFactor);
                }
            }
        }
        return mesh.build();
    }

    private static ArcaneWorldMesh.Basis presentationBasis(SpellPresentationProfile.Profile profile, Vec3 direction) {
        return switch (profile.sigil()) {
            case SKY_RITUAL, GROUND_SEAL, QUAD_ARRAY, FEET_RUNE -> ArcaneWorldMesh.Basis.ground();
            case WALL_MATRIX, PORTAL_GATE, TARGET_SEAL, FRONT_COMPACT, FRONT_LANCE -> ArcaneWorldMesh.Basis.facing(direction);
            case BODY_HALO -> ArcaneWorldMesh.Basis.ground();
        };
    }

    private static Vec3 targetOffset(Visual visual) {
        Vec3 delta = visual.target.subtract(visual.center);
        if (delta.lengthSqr() < 1.0E-8) return visual.direction.scale(Math.max(1.0, visual.range));
        return delta;
    }

    private static double travelAge(Visual visual, double age) {
        double impact = visual.impactAge <= 0.0 ? 0.78 : visual.impactAge;
        return clamp(age / Math.max(0.04, impact), 0.0, 1.0);
    }

    private static double motionProgress(Visual visual, double age) {
        double t = travelAge(visual, age);
        return switch (SpellPresentationProfile.profile(visual.spell).motion()) {
            case DART, LANCE -> 1.0 - Math.pow(1.0 - t, 2.35);
            case BOLT -> 1.0 - Math.pow(1.0 - t, 1.72);
            case HEAVY_ORB -> Math.pow(t, 1.12);
            case MISSILE_SWARM -> 1.0 - Math.pow(1.0 - t, 1.55);
            default -> t;
        };
    }

'''
    text = text[:start] + release + text[end:]

    # Projectile path is the authoritative target offset, not an arbitrary max-range line.
    text = text.replace('''        double travel = Math.min(72.0, Math.max(3.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.35);
        Vec3 position = visual.direction.scale(travel * eased);
''', '''        Vec3 path = targetOffset(visual);
        double eased = motionProgress(visual, age);
        Vec3 position = path.scale(eased);
''', 1)
    text = text.replace('''            double back = core * (1.1 + i * 1.18) + travel * 0.018 * i;
''', '''            double back = core * (1.1 + i * 1.18) + path.length() * 0.018 * i;
''', 1)
    text = text.replace('''            Vec3 end = visual.direction.scale(travel);
''', '''            Vec3 end = path;
''', 1)

    text = text.replace('''        double travel = Math.min(72.0, Math.max(4.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.45);
''', '''        Vec3 path = targetOffset(visual);
        double eased = motionProgress(visual, age);
''', 1)
    text = text.replace('''            Vec3 position = visual.direction.scale(travel * eased - i * body * 0.55)
                    .add(facing.point(phase, spread));
''', '''            double stagger = clamp((travelAge(visual, age) - i * 0.055) / Math.max(0.35, 1.0 - i * 0.055), 0.0, 1.0);
            Vec3 launch = facing.point(Math.PI * 2.0 * i / count, body * 2.6);
            Vec3 position = launch.scale(1.0 - stagger).add(path.scale(1.0 - Math.pow(1.0 - stagger, 1.55)))
                    .add(facing.point(phase, spread));
''', 1)
    text = text.replace('''            Vec3 end = visual.direction.scale(travel);
''', '''            Vec3 end = path;
''', 1)

    text = text.replace('''        double travel = Math.min(72.0, Math.max(4.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.28);
        Vec3 position = visual.direction.scale(travel * eased);
''', '''        Vec3 path = targetOffset(visual);
        double eased = motionProgress(visual, age);
        Vec3 position = path.scale(eased);
''', 1)
    text = text.replace('''            Vec3 end = visual.direction.scale(travel);
''', '''            Vec3 end = path;
''', 1)

    text = text.replace('''        double travel = Math.min(72.0, Math.max(4.0, visual.range));
        double eased = 1.0 - Math.pow(1.0 - age, 1.45);
        Vec3 position = visual.direction.scale(travel * eased);
''', '''        Vec3 path = targetOffset(visual);
        double eased = motionProgress(visual, age);
        Vec3 position = path.scale(eased);
''', 1)
    text = text.replace('''            Vec3 end = visual.direction.scale(travel);
''', '''            Vec3 end = path;
''', 1)

    # Sky-drop effects originate at the actual sky ritual and converge on the actual target.
    meteor_start = text.find("    private static void buildMeteor(ArcaneWorldMesh.Builder mesh, Visual visual,")
    meteor_end = text.find("    private static void buildStorm(", meteor_start)
    if meteor_start < 0 or meteor_end < 0:
        raise SystemExit(f"{path}: meteor block missing")
    meteor = r'''    private static void buildMeteor(ArcaneWorldMesh.Builder mesh, Visual visual,
                                    double age, double powerFactor) {
        SpellPresentationProfile.Profile profile = SpellPresentationProfile.profile(visual.spell);
        ArcaneWorldMesh.Basis sky = ArcaneWorldMesh.Basis.ground();
        int count = Math.max(1, profile.satellites());
        if ("meteor_swarm".equals(visual.spell.id())) count = 4;
        Vec3 target = targetOffset(visual);
        double fall = travelAge(visual, age);
        double body = Math.max(0.55, (0.55 + visual.spell.circle() * 0.10) * powerFactor);
        double spread = "meteor_swarm".equals(visual.spell.id()) ? 10.0
                : "fire_storm".equals(visual.spell.id()) ? 6.0 : 0.0;

        // Keep the summoning array visible while the payload exits it.
        double sealFade = clamp((visual.impactAge + 0.12 - age) / 0.18, 0.0, 1.0);
        double sealRadius = profile.radius();
        mesh.brokenBand(sky, Vec3.ZERO, sealRadius * 0.92, sealRadius,
                112, 7, 1.30F, (float) (0.42 * sealFade));
        mesh.runeRing(sky, Vec3.ZERO, sealRadius * 0.82, 24 + profile.complexity() * 5,
                sealRadius * 0.018, visual.spell.id().hashCode(), age * 0.7, 0.84F);

        for (int i = 0; i < count && !mesh.full(); i++) {
            double angle = Math.PI * 2.0 * i / count + Math.PI / 4.0;
            Vec3 impact = target.add(sky.point(angle, count == 1 ? 0.0 : spread));
            Vec3 mouth = sky.point(angle, sealRadius * (count == 1 ? 0.0 : 0.54));
            double local = clamp((fall - i * 0.035) / Math.max(0.55, 1.0 - i * 0.035), 0.0, 1.0);
            Vec3 meteor = mouth.scale(1.0 - local).add(impact.scale(local));
            Vec3 descent = impact.subtract(mouth);
            Vec3 axis = descent.lengthSqr() < 1.0E-8 ? new Vec3(0.0, -1.0, 0.0) : descent.normalize();
            ArcaneWorldMesh.Basis bodyBasis = ArcaneWorldMesh.Basis.facing(axis);
            mesh.orb(meteor, body * (0.72 + i * 0.04), 34, 1.28F, 0.66F);
            mesh.shard(meteor.subtract(axis.scale(body * 1.2)), axis, bodyBasis,
                    body * 5.4, body * 0.68, 1.18F, 0.52F);
            mesh.ribbon(meteor.subtract(axis.scale(body * 4.2)), axis, bodyBasis,
                    body * 4.4, body * 1.25, 2, 24, 1.08F, 0.34F);
        }

        if (age >= visual.impactAge) {
            double impactAge = clamp((age - visual.impactAge) / Math.max(0.08, 1.0 - visual.impactAge), 0.0, 1.0);
            double effect = Math.min(64.0, SpellMetrics.effectRadius(visual.spell.id(), visual.range,
                    clampCircle(visual.spell.circle())));
            double ring = Math.max(body * 1.6, effect * Math.min(1.0, impactAge * 2.2));
            mesh.band(sky, target, ring * 0.84, ring, 84, 1.34F,
                    (float) (0.62 * (1.0 - impactAge)));
            mesh.orb(target.add(0.0, body * 0.45, 0.0),
                    body * (1.2 + impactAge * 4.0), 38, 1.30F,
                    (float) (0.58 * (1.0 - impactAge)));
        }
    }

'''
    text = text[:meteor_start] + meteor + text[meteor_end:]

    # Beams stop at the authored target rather than blindly drawing to max range.
    text = text.replace('''        double length = Math.min(72.0, Math.max(4.0, visual.range));
''', '''        double length = Math.max(0.6, targetOffset(visual).length());
''', 1)

    # More saturated base palette.
    text = text.replace('''            case FIRE -> 0xFFFF2A08;
            case FROST -> 0xFF18C8FF;
            case WIND -> 0xFF00F0A8;
            case WARD -> 0xFF8A35FF;
            case LIFE -> 0xFF25E85A;
            case SPACE -> 0xFFD51CFF;
            default -> 0xFF3E63FF;
''', '''            case FIRE -> 0xFFFF2100;
            case FROST -> 0xFF00CFFF;
            case WIND -> 0xFF00FF9C;
            case WARD -> 0xFF8E22FF;
            case LIFE -> 0xFF18F044;
            case SPACE -> 0xFFD000FF;
            default -> 0xFF3454FF;
''', 1)

    text = text.replace('''    private record Visual(UUID caster, SpellDefinition spell, boolean fusion, int ingredients,
                          Vec3 center, Vec3 direction, double range, double power, double progress,
                          long startedAt, long expiresAt, boolean release) {}
''', '''    private record Visual(UUID caster, SpellDefinition spell, boolean fusion, int ingredients,
                          Vec3 center, Vec3 target, Vec3 direction, double range, double power, double progress,
                          long startedAt, long expiresAt, boolean release, double impactAge) {}
''', 1)
    write(path, text)


def main() -> None:
    patch_version()
    write_presentation_profile()
    patch_mesh()
    patch_world_magic_service()
    patch_kinetics()
    patch_cast_distance()
    patch_tracker()
    print("Arcane Circle alpha.17 spell-presentation migration: PASS")


if __name__ == "__main__":
    main()
