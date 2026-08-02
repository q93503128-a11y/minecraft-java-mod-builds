#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/arcanecircle"


def patch(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise RuntimeError(f"missing patch token in {path}: {old[:120]!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")


def replace_method(source: str, signature: str, replacement: str) -> str:
    start = source.find(signature)
    if start < 0:
        raise RuntimeError(f"missing method signature: {signature}")
    brace = source.find("{", start)
    depth = 0
    end = -1
    for index in range(brace, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                end = index + 1
                break
    if end < 0:
        raise RuntimeError(f"unclosed method: {signature}")
    return source[:start] + replacement + source[end:]


def strip_calls(source: str, token: str) -> str:
    while token in source:
        pos = source.index(token)
        paren = source.find("(", pos)
        depth = 0
        end = -1
        for index in range(paren, len(source)):
            char = source[index]
            if char == "(":
                depth += 1
            elif char == ")":
                depth -= 1
                if depth == 0:
                    semicolon = source.find(";", index)
                    if semicolon < 0:
                        raise RuntimeError(f"missing semicolon after {token}")
                    end = semicolon + 1
                    break
        if end < 0:
            raise RuntimeError(f"unclosed call: {token}")
        source = source[:pos] + "WorldMagicService.noParticles();" + source[end:]
    return source


version_file = ROOT / "gradle.properties"
version_text = version_file.read_text(encoding="utf-8")
if "mod_version=0.12.0-alpha.1" in version_text:
    print("Arcane Circle v0.12 source already applied")
    raise SystemExit(0)
if "mod_version=0.11.0-alpha.1" not in version_text:
    raise RuntimeError("v0.12 migration requires the published v0.11 source")

patch(version_file, "mod_version=0.11.0-alpha.1", "mod_version=0.12.0-alpha.1")
patch(JAVA / "ArcaneCircle.java", 'VERSION = "0.11.0-alpha.1"', 'VERSION = "0.12.0-alpha.1"')
patch(JAVA / "network/ArcaneNetwork.java", 'PROTOCOL_VERSION = "ninefold-arcana-11"',
      'PROTOCOL_VERSION = "ninefold-arcana-12"')
index_path = ROOT / "src/main/resources/data/arcanecircle/spell_catalog/index.json"
index = json.loads(index_path.read_text(encoding="utf-8"))
index["version"] = "0.12.0-alpha.1"
index["visual_core"] = "world_geometry_no_particles"
index["fusion_casting"] = "charged_release"
index_path.write_text(json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# Dedicated clientbound world visual payload.
(JAVA / "network/WorldMagicPayload.java").write_text(r'''package kr.moonseungjun.arcanecircle.network;

import io.netty.buffer.ByteBuf;
import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record WorldMagicPayload(String state) implements CustomPacketPayload {
    public static final Type<WorldMagicPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "world_magic"));
    public static final StreamCodec<ByteBuf, WorldMagicPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, WorldMagicPayload::state, WorldMagicPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
''', encoding="utf-8")

# Server authority broadcasts charge/release geometry to every nearby client.
(JAVA / "magic/WorldMagicService.java").write_text(r'''package kr.moonseungjun.arcanecircle.magic;

import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

public final class WorldMagicService {
    private WorldMagicService() {}

    public static void charge(ServerPlayer player, SpellDefinition spell, boolean fusion,
                              List<String> ingredients, double range, double progress) {
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 center = anchorCenter(player, spell, range, direction);
        send(player, encode("charge", player, spell, fusion, ingredients, center, direction,
                range, spell.power(), Math.max(0.0, Math.min(1.0, progress)), 8));
    }

    public static void release(ServerPlayer player, MagicPlayerData.CastPreparation cast) {
        SpellDefinition spell = cast.spell();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 center = anchorCenter(player, spell, cast.range(), direction);
        int duration = 10 + spell.circle() * 5 + (cast.fusion() ? 8 : 0);
        send(player, encode("release", player, spell, cast.fusion(), cast.ingredients(), center, direction,
                cast.range(), cast.power(), 1.0, duration));
    }

    public static void stop(ServerPlayer player) {
        send(player, "kind=stop;caster=" + player.getUUID());
    }

    public static void noParticles() {
        // Deliberate no-op. Core spell visuals are submitted as world geometry on clients.
    }

    private static void send(ServerPlayer player, String state) {
        ServerLevel level = (ServerLevel) player.level();
        PacketDistributor.sendToPlayersNear(level, null, player.getX(), player.getY(), player.getZ(),
                128.0, new WorldMagicPayload(state));
    }

    private static String encode(String kind, ServerPlayer player, SpellDefinition spell, boolean fusion,
                                 List<String> ingredients, Vec3 center, Vec3 direction, double range,
                                 double power, double progress, int duration) {
        return String.format(Locale.ROOT,
                "kind=%s;caster=%s;spell=%s;fusion=%d;ingredients=%d;x=%.5f;y=%.5f;z=%.5f;dx=%.5f;dy=%.5f;dz=%.5f;range=%.4f;power=%.4f;progress=%.4f;duration=%d",
                kind, player.getUUID(), spell.id(), fusion ? 1 : 0, ingredients.size(),
                center.x, center.y, center.z, direction.x, direction.y, direction.z,
                range, power, progress, duration);
    }

    private static Vec3 anchorCenter(ServerPlayer player, SpellDefinition spell, double range, Vec3 look) {
        return switch (spell.sigilAnchor()) {
            case FRONT -> player.getEyePosition().add(look.scale(1.55 + spell.circle() * 0.035));
            case FEET, GROUND_SELF -> player.position().add(0.0, 0.055, 0.0);
            case BODY -> player.position().add(0.0, 1.0, 0.0);
            case GROUND_TARGET -> aimGround(player, Math.max(4.0, range));
            case TARGET -> player.getEyePosition().add(look.scale(Math.min(Math.max(3.0, range * 0.72), 18.0)));
        };
    }

    private static Vec3 aimGround(ServerPlayer player, double range) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.getEyePosition();
        for (int step = (int) Math.max(2, Math.floor(Math.min(range, 28.0))); step >= 2; step--) {
            BlockPos candidate = BlockPos.containing(origin.add(look.scale(step)));
            for (int down = 0; down <= 10; down++) {
                BlockPos floor = candidate.below(down);
                BlockState state = level.getBlockState(floor);
                if (state.isFaceSturdy(level, floor, Direction.UP)) {
                    return Vec3.atCenterOf(floor.above()).add(0.0, -0.48, 0.0);
                }
            }
        }
        return player.position().add(new Vec3(look.x, 0.0, look.z).normalize().scale(Math.min(6.0, range)))
                .add(0.0, 0.055, 0.0);
    }
}
''', encoding="utf-8")

# World-space non-particle renderer. Geometry is visible to nearby players and follows actual coordinates.
(JAVA / "client/WorldMagicTracker.java").write_text(r'''package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WorldMagicTracker {
    private static final ContextKey<List<RenderEntry>> DATA_KEY = new ContextKey<>(
            Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "world_magic_geometry"));
    private static final Map<UUID, Visual> CHARGES = new HashMap<>();
    private static final List<Visual> RELEASES = new ArrayList<>();

    private WorldMagicTracker() {}

    public static void accept(WorldMagicPayload payload) {
        Map<String, String> values = parse(payload.state());
        String kind = values.getOrDefault("kind", "");
        UUID caster;
        try { caster = UUID.fromString(values.getOrDefault("caster", "")); }
        catch (IllegalArgumentException ignored) { return; }
        if ("stop".equals(kind)) {
            CHARGES.remove(caster);
            return;
        }
        SpellDefinition spell = SpellCatalog.spell(values.getOrDefault("spell", "")).orElse(null);
        if (spell == null) return;
        boolean fusion = integer(values, "fusion", 0) != 0;
        int ingredients = Math.max(0, integer(values, "ingredients", 0));
        Vec3 center = new Vec3(decimal(values, "x", 0), decimal(values, "y", 0), decimal(values, "z", 0));
        Vec3 direction = new Vec3(decimal(values, "dx", 0), decimal(values, "dy", 0), decimal(values, "dz", 1));
        if (direction.lengthSqr() < 0.00001) direction = new Vec3(0, 0, 1);
        direction = direction.normalize();
        double range = Math.max(0.1, decimal(values, "range", spell.range()));
        double power = Math.max(0.1, decimal(values, "power", spell.power()));
        double progress = Math.max(0.0, Math.min(1.0, decimal(values, "progress", 1.0)));
        int duration = Math.max(2, integer(values, "duration", 10));
        long now = System.nanoTime();
        if ("charge".equals(kind)) {
            VoxelShape geometry = buildCharge(spell, fusion, ingredients, direction, progress, range);
            CHARGES.put(caster, new Visual(caster, center, geometry, color(spell), now + 550_000_000L));
        } else if ("release".equals(kind)) {
            VoxelShape geometry = buildRelease(spell, fusion, ingredients, direction, range, power);
            RELEASES.add(new Visual(caster, center, geometry, color(spell),
                    now + duration * 50_000_000L));
            CHARGES.remove(caster);
        }
    }

    public static void onExtract(ExtractLevelRenderStateEvent event) {
        long now = System.nanoTime();
        CHARGES.values().removeIf(visual -> visual.expiresAt < now);
        RELEASES.removeIf(visual -> visual.expiresAt < now);
        if (CHARGES.isEmpty() && RELEASES.isEmpty()) return;
        List<RenderEntry> entries = new ArrayList<>();
        for (Visual visual : CHARGES.values()) entries.add(visual.renderEntry());
        for (Visual visual : RELEASES) entries.add(visual.renderEntry());
        event.getRenderState().setRenderData(DATA_KEY, List.copyOf(entries));
    }

    public static void onSubmit(SubmitCustomGeometryEvent event) {
        List<RenderEntry> entries = event.getLevelRenderState().getRenderData(DATA_KEY);
        if (entries == null || entries.isEmpty()) return;
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        float baseWidth = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
        for (RenderEntry entry : entries) {
            Vec3 offset = entry.center.subtract(camera);
            event.getPoseStack().pushPose();
            event.getPoseStack().translate(offset.x, offset.y, offset.z);
            event.getSubmitNodeCollector().submitShapeOutline(event.getPoseStack(), entry.geometry,
                    RenderTypes.lines(), entry.argb, Math.max(1.2F, baseWidth * 1.25F), false);
            event.getPoseStack().popPose();
        }
    }

    private static VoxelShape buildCharge(SpellDefinition spell, boolean fusion, int ingredients,
                                          Vec3 normal, double progress, double range) {
        Basis basis = basis(spell, normal);
        VoxelShape shape = Shapes.empty();
        double outer = 0.42 + spell.circle() * 0.095 + Math.min(0.32, range * 0.008) + (fusion ? 0.18 : 0.0);
        int ringPoints = 38 + spell.circle() * 4;
        // The primary concentric count is exactly the spell circle: 1C=1, 2C=2 ... 9C=9.
        for (int ring = 0; ring < spell.circle(); ring++) {
            double radius = outer * (1.0 - ring * 0.66 / Math.max(1.0, spell.circle()));
            double localProgress = Math.max(0.0, Math.min(1.0, progress * spell.circle() - ring));
            shape = Shapes.or(shape, partialCircle(basis, radius, ringPoints, localProgress, 0.018));
        }
        if (progress >= 0.16) {
            int sides = 3 + Math.floorMod(spell.id().hashCode(), 6);
            shape = Shapes.or(shape, polygon(basis, outer * 0.56, sides,
                    Math.max(0.0, Math.min(1.0, (progress - 0.16) / 0.46)), 0.019));
        }
        if (progress >= 0.36) {
            int spokes = Math.min(14, 3 + spell.circle() + Math.floorMod(spell.id().hashCode(), 3));
            int shown = (int) Math.floor(spokes * Math.min(1.0, (progress - 0.36) / 0.42));
            for (int i = 0; i < shown; i++) {
                double angle = Math.PI * 2.0 * i / spokes;
                shape = Shapes.or(shape, segment(basis.point(angle, outer * 0.28),
                        basis.point(angle, outer * 0.92), 8, 0.017));
            }
        }
        if (spell.circle() >= 3 && progress >= 0.60) {
            int satellites = Math.min(8, spell.circle() - 1);
            for (int i = 0; i < satellites; i++) {
                double angle = Math.PI * 2.0 * i / satellites;
                Vec3 satelliteCenter = basis.point(angle, outer * 1.22);
                shape = Shapes.or(shape, circleAround(basis, satelliteCenter, outer * 0.16,
                        20 + spell.circle(), 0.016));
                shape = Shapes.or(shape, segment(basis.point(angle, outer), satelliteCenter, 5, 0.015));
            }
        }
        if (fusion && ingredients >= 2 && progress >= 0.42) {
            for (int i = 0; i < ingredients; i++) {
                double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / ingredients;
                Vec3 satelliteCenter = basis.point(angle, outer * 1.48);
                shape = Shapes.or(shape, circleAround(basis, satelliteCenter, outer * 0.25,
                        28, 0.022));
                shape = Shapes.or(shape, polygonAround(basis, satelliteCenter, outer * 0.17,
                        3 + i, 0.019));
                shape = Shapes.or(shape, segment(Vec3.ZERO, satelliteCenter, 9, 0.017));
            }
        }
        return shape;
    }

    private static VoxelShape buildRelease(SpellDefinition spell, boolean fusion, int ingredients,
                                           Vec3 direction, double range, double power) {
        Basis basis = basis(spell, direction);
        VoxelShape shape = buildCharge(spell, fusion, ingredients, direction, 1.0, range);
        double scale = 0.65 + spell.circle() * 0.13 + Math.min(0.8, power * 0.015);
        switch (spell.sigilAnchor()) {
            case FRONT -> {
                double length = Math.min(34.0, Math.max(3.0, range));
                Vec3 end = direction.scale(length);
                shape = Shapes.or(shape, segment(Vec3.ZERO, end, Math.max(20, (int) (length * 3)),
                        0.035 + spell.circle() * 0.008));
                if (spell.circle() >= 4) {
                    Vec3 side = basis.right.scale(0.14 + spell.circle() * 0.025);
                    shape = Shapes.or(shape, segment(side, end.add(side), 30, 0.024));
                    shape = Shapes.or(shape, segment(side.scale(-1), end.add(side.scale(-1)), 30, 0.024));
                }
                shape = Shapes.or(shape, sphereLattice(end, 0.28 + spell.circle() * 0.09, spell.circle()));
            }
            case FEET, GROUND_SELF, GROUND_TARGET -> {
                Basis ground = Basis.ground();
                double radius = Math.min(17.0, Math.max(1.8, range * 0.24 + spell.circle() * 0.42));
                int rings = Math.max(2, Math.min(9, spell.circle()));
                for (int i = 1; i <= rings; i++) {
                    shape = Shapes.or(shape, circle(ground, radius * i / rings, 48 + i * 4, 0.035));
                }
                int pillars = 4 + spell.circle();
                for (int i = 0; i < pillars; i++) {
                    double angle = Math.PI * 2.0 * i / pillars;
                    Vec3 base = ground.point(angle, radius * 0.82);
                    shape = Shapes.or(shape, segment(base, base.add(0, 1.2 + spell.circle() * 0.32, 0),
                            10 + spell.circle(), 0.04));
                }
            }
            case BODY -> {
                double radius = 1.15 + spell.circle() * 0.22;
                shape = Shapes.or(shape, sphereLattice(new Vec3(0, -0.75, 0), radius, spell.circle() + 2));
            }
            case TARGET -> {
                double radius = 0.75 + spell.circle() * 0.15;
                Basis ground = Basis.ground();
                for (int level = 0; level < 3 + spell.circle() / 2; level++) {
                    Vec3 offset = new Vec3(0, -0.8 + level * 0.48, 0);
                    shape = Shapes.or(shape, circleAround(ground, offset, radius, 36, 0.028));
                }
                int bars = 5 + spell.circle();
                for (int i = 0; i < bars; i++) {
                    double angle = Math.PI * 2.0 * i / bars;
                    Vec3 low = ground.point(angle, radius).add(0, -0.8, 0);
                    shape = Shapes.or(shape, segment(low, low.add(0, 1.9 + spell.circle() * 0.08, 0),
                            12, 0.03));
                }
            }
        }
        if (spell.school() == SpellDefinition.School.SPACE) {
            Basis portal = basis(spell, direction);
            double portalRadius = 0.9 + spell.circle() * 0.16;
            shape = Shapes.or(shape, circle(portal, portalRadius, 72, 0.035));
            shape = Shapes.or(shape, circleAround(portal, direction.scale(0.28), portalRadius * 0.82, 60, 0.03));
        }
        return shape;
    }

    private static Basis basis(SpellDefinition spell, Vec3 normal) {
        return switch (spell.sigilAnchor()) {
            case FEET, GROUND_SELF, GROUND_TARGET -> Basis.ground();
            default -> Basis.facing(normal);
        };
    }

    private static VoxelShape sphereLattice(Vec3 center, double radius, int detail) {
        VoxelShape shape = Shapes.empty();
        Basis ground = Basis.ground();
        shape = Shapes.or(shape, circleAround(ground, center, radius, 48, 0.032));
        Basis verticalX = new Basis(new Vec3(1, 0, 0), new Vec3(0, 1, 0));
        Basis verticalZ = new Basis(new Vec3(0, 0, 1), new Vec3(0, 1, 0));
        shape = Shapes.or(shape, circleAround(verticalX, center, radius, 48, 0.032));
        shape = Shapes.or(shape, circleAround(verticalZ, center, radius, 48, 0.032));
        for (int i = 1; i < Math.min(5, detail); i++) {
            double y = radius * (-0.65 + i * 1.3 / Math.min(5, detail));
            double r = Math.sqrt(Math.max(0.05, radius * radius - y * y));
            shape = Shapes.or(shape, circleAround(ground, center.add(0, y, 0), r, 36, 0.022));
        }
        return shape;
    }

    private static VoxelShape partialCircle(Basis basis, double radius, int points, double progress, double size) {
        VoxelShape shape = Shapes.empty();
        int shown = Math.max(0, Math.min(points, (int) Math.ceil(points * progress)));
        for (int i = 0; i < shown; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / points;
            shape = Shapes.or(shape, pointBox(basis.point(angle, radius), size));
        }
        return shape;
    }

    private static VoxelShape circle(Basis basis, double radius, int points, double size) {
        return partialCircle(basis, radius, points, 1.0, size);
    }

    private static VoxelShape circleAround(Basis basis, Vec3 center, double radius, int points, double size) {
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            shape = Shapes.or(shape, pointBox(center.add(basis.point(angle, radius)), size));
        }
        return shape;
    }

    private static VoxelShape polygon(Basis basis, double radius, int sides, double progress, double size) {
        return polygonAround(basis, Vec3.ZERO, radius, sides, size, progress);
    }

    private static VoxelShape polygonAround(Basis basis, Vec3 center, double radius, int sides, double size) {
        return polygonAround(basis, center, radius, sides, size, 1.0);
    }

    private static VoxelShape polygonAround(Basis basis, Vec3 center, double radius, int sides,
                                            double size, double progress) {
        VoxelShape shape = Shapes.empty();
        int shownEdges = Math.max(0, Math.min(sides, (int) Math.ceil(sides * progress)));
        List<Vec3> vertices = new ArrayList<>();
        for (int i = 0; i < sides; i++) {
            double angle = -Math.PI / 2.0 + Math.PI * 2.0 * i / sides;
            vertices.add(center.add(basis.point(angle, radius)));
        }
        for (int i = 0; i < shownEdges; i++) {
            shape = Shapes.or(shape, segment(vertices.get(i), vertices.get((i + 1) % sides), 10, size));
        }
        return shape;
    }

    private static VoxelShape segment(Vec3 start, Vec3 end, int points, double size) {
        VoxelShape shape = Shapes.empty();
        for (int i = 0; i <= points; i++) {
            shape = Shapes.or(shape, pointBox(start.lerp(end, i / (double) points), size));
        }
        return shape;
    }

    private static VoxelShape pointBox(Vec3 point, double half) {
        return Shapes.create(new AABB(point.x - half, point.y - half, point.z - half,
                point.x + half, point.y + half, point.z + half));
    }

    private static int color(SpellDefinition spell) {
        return switch (spell.school()) {
            case FIRE -> 0xFFFF7048;
            case FROST -> 0xFF6DE4FF;
            case WIND -> 0xFF76E6BD;
            case WARD -> 0xFFC595FF;
            case LIFE -> 0xFF73E38E;
            case SPACE -> 0xFFA382FF;
            default -> 0xFF82A8FF;
        };
    }

    private static Map<String, String> parse(String state) {
        Map<String, String> values = new HashMap<>();
        for (String part : state.split(";")) {
            int index = part.indexOf('=');
            if (index > 0) values.put(part.substring(0, index), part.substring(index + 1));
        }
        return values;
    }

    private static int integer(Map<String, String> values, String key, int fallback) {
        try { return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static double decimal(Map<String, String> values, String key, double fallback) {
        try { return Double.parseDouble(values.getOrDefault(key, Double.toString(fallback))); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private record Visual(UUID caster, Vec3 center, VoxelShape geometry, int argb, long expiresAt) {
        RenderEntry renderEntry() { return new RenderEntry(center, geometry, argb); }
    }
    private record RenderEntry(Vec3 center, VoxelShape geometry, int argb) {}
    private record Basis(Vec3 right, Vec3 up) {
        static Basis ground() { return new Basis(new Vec3(1, 0, 0), new Vec3(0, 0, 1)); }
        static Basis facing(Vec3 normal) {
            Vec3 reference = Math.abs(normal.y) > 0.92 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 right = normal.cross(reference).normalize();
            Vec3 up = right.cross(normal).normalize();
            return new Basis(right, up);
        }
        Vec3 point(double angle, double radius) {
            return right.scale(Math.cos(angle) * radius).add(up.scale(Math.sin(angle) * radius));
        }
    }
}
''', encoding="utf-8")

# Smaller separated square HUD, half-scale names, no screen-space magic circle, inventory status on the left.
(JAVA / "client/ArcaneHud.java").write_text(r'''package kr.moonseungjun.arcanecircle.client;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;

public final class ArcaneHud {
    private static final Identifier LAYER_ID = Identifier.fromNamespaceAndPath(ArcaneCircle.MOD_ID, "spell_hotbar");

    private ArcaneHud() {}

    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, ArcaneHud::renderWorldHud);
    }

    private static void renderWorldHud(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null || !ArcaneClientState.ready()) return;
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        Font font = minecraft.font;
        int gap = width >= 520 ? 6 : 5;
        int slotSize = width >= 520 ? 25 : width >= 360 ? 23 : 21;
        int total = slotSize * 5 + gap * 4;
        int startX = Math.max(4, (width - total) / 2);
        int y = Math.max(8, height - slotSize - 54);
        drawMana(g, font, startX, y);
        for (int slot = 0; slot < 5; slot++) {
            drawSlot(g, font, startX + slot * (slotSize + gap), y, slotSize, slot);
        }
        drawFusionQueue(g, font, width, y - 15);
    }

    private static void drawMana(GuiGraphicsExtractor g, Font font, int startX, int y) {
        int mana = ArcaneClientState.integer("mana", 0);
        int max = Math.max(1, ArcaneClientState.integer("max", 100));
        int w = Math.min(62, Math.max(42, startX - 10));
        int x = Math.max(4, startX - w - 7);
        int fill = (int) Math.round((w - 2) * Math.min(1.0, mana / (double) max));
        tinyText(g, font, ArcaneClientState.integer("circle", 1) + "C " + mana + "/" + max,
                x, y + 3, 0xFFE7DDF7, 0.62F, false);
        g.fill(x, y + 13, x + w, y + 17, 0xD9050912);
        g.fill(x + 1, y + 14, x + 1 + fill, y + 16, 0xEF5E8EEB);
    }

    private static void drawSlot(GuiGraphicsExtractor g, Font font, int x, int y, int size, int slot) {
        SpellDefinition spell = SpellCatalog.spell(ArcaneClientState.slot(slot)).orElse(null);
        int color = spell == null ? 0xFF606475 : ArcaneRenderUtil.schoolColor(spell.school());
        int dark = spell == null ? 0xFF121620 : ArcaneRenderUtil.schoolDark(spell.school());
        int remaining = ArcaneClientState.cooldownRemainingTicks(slot);
        boolean charging = ArcaneClientState.isChargingSlot(slot);
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, charging ? 0xFFFFD36B : 0xD9040610);
        g.fill(x, y, x + size, y + size, remaining > 0 ? dark : 0xEB101827);
        g.fill(x, y + size - 2, x + size, y + size, color);
        tinyText(g, font, Integer.toString(slot + 1), x + 2, y + 1, 0xFF98A3B7, 0.55F, false);
        if (spell == null) return;
        int iconY = y + 10;
        ArcaneRenderUtil.ring(g, x + size / 2, iconY, Math.max(4, size / 5), remaining > 0 ? 0xFF686A74 : color);
        ArcaneRenderUtil.spellRune(g, x + size / 2, iconY, spell, Math.max(3, size / 7),
                remaining > 0 ? 0xFF777A84 : 0xFFF8F2FF);
        String name = fitName(font, spell.name(), (size - 2) * 2);
        tinyText(g, font, name, x + size / 2, y + size - 7,
                remaining > 0 ? 0xFF7E7F88 : charging ? 0xFFFFE0A2 : 0xFFE7DFEC, 0.50F, true);
        if (remaining > 0) {
            String seconds = remaining >= 200 ? Integer.toString((int) Math.ceil(remaining / 20.0))
                    : String.format("%.1f", remaining / 20.0);
            tinyText(g, font, seconds, x + size / 2, iconY - 2, 0xFFFFFFFF, 0.55F, true);
            int fill = (int) Math.round((size - 2) * ArcaneClientState.cooldownFraction(slot));
            g.fill(x + 1, y + size - 3, x + 1 + fill, y + size - 1, 0xFFE46D78);
        } else if (charging) {
            int fill = (int) Math.round((size - 2) * ArcaneClientState.chargingFraction());
            g.fill(x + 1, y + size - 3, x + 1 + fill, y + size - 1,
                    ArcaneClientState.chargingReady() ? 0xFFFFD36B : color);
        }
    }

    private static void drawFusionQueue(GuiGraphicsExtractor g, Font font, int width, int y) {
        List<String> queue = ArcaneClientState.queue();
        if (queue.isEmpty()) return;
        String result = ArcaneClientState.queueResult();
        int boxWidth = Math.min(width - 12, 190);
        int x = (width - boxWidth) / 2;
        g.fill(x, y, x + boxWidth, y + 11, 0xED080B16);
        double progress = ArcaneClientState.fusionChargingFraction();
        int fill = (int) Math.round(boxWidth * progress);
        g.fill(x, y, x + fill, y + 2, result.isBlank() ? 0xFF7E67AD : 0xFFFFC861);
        String chain = queue.stream().map(id -> SpellCatalog.spell(id).map(SpellDefinition::name).orElse(id))
                .reduce((a, b) -> a + "+" + b).orElse("");
        String suffix = result.isBlank() ? "" : "→" + SpellCatalog.spell(result).map(SpellDefinition::name).orElse(result);
        tinyText(g, font, compactName("X " + chain + suffix, 34), width / 2, y + 3,
                result.isBlank() ? 0xFFD4B8F1 : ArcaneClientState.fusionChargingReady() ? 0xFFFFE5A1 : 0xFFFFD889,
                0.58F, true);
    }

    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen) || !ArcaneClientState.ready()) return;
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        GuiGraphicsExtractor g = event.getGuiGraphics();
        Font font = minecraft.font;
        int inventoryRight = width / 2 + 88;
        int inventoryLeft = width / 2 - 88;
        int sideSpace = Math.max(inventoryLeft, width - inventoryRight);
        if (sideSpace < 142) return;
        int panelW = Math.min(154, sideSpace - 12);
        int x = inventoryLeft - panelW - 7;
        if (x < 5) x = inventoryRight + 7;
        int y = Math.max(5, (height - 104) / 2);
        panel(g, x, y, panelW, 104, "마력핵");
        int lineY = y + 27;
        g.text(font, Component.literal(ArcaneClientState.integer("circle", 1) + "C  MP "
                + ArcaneClientState.integer("mana", 0) + "/" + ArcaneClientState.integer("max", 100)),
                x + 7, lineY, 0xFFC9D8F2);
        g.text(font, Component.literal("회복 " + String.format("%.1f", ArcaneClientState.regenPerSecond()) + "/초"),
                x + 7, lineY + 14, 0xFF8ED6C0);
        g.text(font, Component.literal(compactName(ArcaneClientState.text("staff", "맨손"), 18)),
                x + 7, lineY + 30, 0xFFFFD58D);
        g.text(font, Component.literal("C 마도서 · 1~5 시전 · X 융합"), x + 7, y + 85, 0xFF81778F);
    }

    private static void tinyText(GuiGraphicsExtractor g, Font font, String text, int x, int y,
                                 int color, float scale, boolean centered) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        if (centered) g.centeredText(font, Component.literal(text), 0, 0, color);
        else g.text(font, Component.literal(text), 0, 0, color);
        g.pose().popMatrix();
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, String title) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF604779);
        g.fill(x, y, x + w, y + h, 0xF20A0F1D);
        g.fill(x + 3, y + 3, x + w - 3, y + 21, 0xD1241A38);
        g.centeredText(Minecraft.getInstance().font, Component.literal(title), x + w / 2, y + 8, 0xFFEAD9FF);
    }

    private static String fitName(Font font, String value, int pixels) {
        if (value == null || pixels <= 0) return "";
        if (font.width(value) <= pixels) return value;
        String suffix = "…";
        int end = value.length();
        while (end > 0 && font.width(value.substring(0, end) + suffix) > pixels) end--;
        return end <= 0 ? suffix : value.substring(0, end) + suffix;
    }

    private static String compactName(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }
}
''', encoding="utf-8")

# Strongly suppress vanilla 1-9 switching and preserve the selected item slot.
(JAVA / "client/ArcaneClient.java").write_text(r'''package kr.moonseungjun.arcanecircle.client;

import com.mojang.blaze3d.platform.InputConstants;
import kr.moonseungjun.arcanecircle.network.BeginCastPayload;
import kr.moonseungjun.arcanecircle.network.CommitFusionPayload;
import kr.moonseungjun.arcanecircle.network.QueueFusionPayload;
import kr.moonseungjun.arcanecircle.network.ReleaseCastPayload;
import kr.moonseungjun.arcanecircle.network.RequestGrimoirePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class ArcaneClient {
    private static final KeyMapping GRIMOIRE_KEY = new KeyMapping(
            "key.arcanecircle.grimoire", InputConstants.KEY_C, KeyMapping.Category.MISC);
    private static final KeyMapping FUSION_MODIFIER_KEY = new KeyMapping(
            "key.arcanecircle.fusion_modifier", InputConstants.KEY_X, KeyMapping.Category.MISC);
    private static final KeyMapping[] SLOT_KEYS = {
            new KeyMapping("key.arcanecircle.slot_1", InputConstants.KEY_1, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_2", InputConstants.KEY_2, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_3", InputConstants.KEY_3, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_4", InputConstants.KEY_4, KeyMapping.Category.MISC),
            new KeyMapping("key.arcanecircle.slot_5", InputConstants.KEY_5, KeyMapping.Category.MISC)
    };
    private static final boolean[] SLOT_WAS_DOWN = new boolean[5];
    private static boolean fusionWasDown;
    private static int protectedSelectedSlot = -1;
    private static boolean numberInputActive;

    private ArcaneClient() {}

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GRIMOIRE_KEY);
        event.register(FUSION_MODIFIER_KEY);
        for (KeyMapping key : SLOT_KEYS) event.register(key);
    }

    public static void onClientTickPre(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gui.screen() != null) return;
        protectedSelectedSlot = minecraft.player.getInventory().getSelectedSlot();
        numberInputActive = false;
        for (KeyMapping vanilla : minecraft.options.keyHotbarSlots) {
            numberInputActive |= vanilla.isDown();
            vanilla.setDown(false);
            while (vanilla.consumeClick()) {}
        }
    }

    public static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            resetInput();
            ArcaneClientState.reset();
            drainClicks();
            return;
        }
        if (numberInputActive && protectedSelectedSlot >= 0
                && minecraft.player.getInventory().getSelectedSlot() != protectedSelectedSlot) {
            minecraft.player.getInventory().setSelectedSlot(protectedSelectedSlot);
        }
        if (minecraft.gui.screen() != null) {
            while (GRIMOIRE_KEY.consumeClick()) {}
            drainSlotClicks();
            boolean hadActiveInput = fusionWasDown;
            for (int slot = 0; slot < SLOT_WAS_DOWN.length; slot++) {
                hadActiveInput |= SLOT_WAS_DOWN[slot];
                SLOT_WAS_DOWN[slot] = false;
            }
            if (hadActiveInput || FUSION_MODIFIER_KEY.isDown()) {
                ClientPacketDistributor.sendToServer(new CommitFusionPayload(1));
            }
            fusionWasDown = false;
            return;
        }
        while (GRIMOIRE_KEY.consumeClick()) {
            ClientPacketDistributor.sendToServer(new RequestGrimoirePayload("atlas"));
        }
        boolean fusionDown = FUSION_MODIFIER_KEY.isDown();
        if (!fusionWasDown && fusionDown) {
            ClientPacketDistributor.sendToServer(new CommitFusionPayload(1));
            for (int slot = 0; slot < SLOT_WAS_DOWN.length; slot++) SLOT_WAS_DOWN[slot] = false;
        }
        for (int slot = 0; slot < SLOT_KEYS.length; slot++) {
            boolean down = SLOT_KEYS[slot].isDown();
            if (down && !SLOT_WAS_DOWN[slot]) {
                if (fusionDown) ClientPacketDistributor.sendToServer(new QueueFusionPayload(slot));
                else ClientPacketDistributor.sendToServer(new BeginCastPayload(slot));
            } else if (!down && SLOT_WAS_DOWN[slot] && !fusionWasDown) {
                ClientPacketDistributor.sendToServer(new ReleaseCastPayload(slot));
            }
            SLOT_WAS_DOWN[slot] = down;
            while (SLOT_KEYS[slot].consumeClick()) {}
        }
        if (fusionWasDown && !fusionDown) {
            ClientPacketDistributor.sendToServer(new CommitFusionPayload(0));
            for (int slot = 0; slot < SLOT_WAS_DOWN.length; slot++) SLOT_WAS_DOWN[slot] = false;
        }
        fusionWasDown = fusionDown;
    }

    private static void resetInput() {
        fusionWasDown = false;
        protectedSelectedSlot = -1;
        numberInputActive = false;
        for (int slot = 0; slot < SLOT_WAS_DOWN.length; slot++) SLOT_WAS_DOWN[slot] = false;
    }
    private static void drainClicks() {
        while (GRIMOIRE_KEY.consumeClick()) {}
        drainSlotClicks();
    }
    private static void drainSlotClicks() {
        for (KeyMapping key : SLOT_KEYS) while (key.consumeClick()) {}
    }
}
''', encoding="utf-8")

# Client state fields for fusion charge bar.
state_path = JAVA / "client/ArcaneClientState.java"
patch(state_path,
'''    public static boolean queueCanExtend() {
        return integer("queue_extend", 0) != 0;
    }
''',
'''    public static boolean queueCanExtend() {
        return integer("queue_extend", 0) != 0;
    }

    public static int fusionChargingTicks() {
        int snapshotTicks = integer("fusion_charge_ticks", 0);
        if (text("fusion_charging", "").isBlank()) return 0;
        long elapsed = Math.max(0L, (System.nanoTime() - updatedAtNanos) / 50_000_000L);
        return snapshotTicks + (int) Math.min(Integer.MAX_VALUE, elapsed);
    }

    public static int fusionChargingRequiredTicks() {
        return Math.max(0, integer("fusion_charge_required", 0));
    }

    public static double fusionChargingFraction() {
        int required = fusionChargingRequiredTicks();
        if (required <= 0) return 0.0;
        return Math.min(1.0, fusionChargingTicks() / (double) required);
    }

    public static boolean fusionChargingReady() {
        int required = fusionChargingRequiredTicks();
        return required > 0 && fusionChargingTicks() >= required;
    }
''')

# Register client world geometry events and payload.
client_main = JAVA / "ArcaneCircleClient.java"
patch(client_main, 'import kr.moonseungjun.arcanecircle.client.ClientNetworkHandlers;\n',
      'import kr.moonseungjun.arcanecircle.client.ClientNetworkHandlers;\nimport kr.moonseungjun.arcanecircle.client.WorldMagicTracker;\n')
patch(client_main, '        NeoForge.EVENT_BUS.addListener(ArcaneHud::onScreenRender);\n',
      '        NeoForge.EVENT_BUS.addListener(ArcaneHud::onScreenRender);\n'
      '        NeoForge.EVENT_BUS.addListener(WorldMagicTracker::onExtract);\n'
      '        NeoForge.EVENT_BUS.addListener(WorldMagicTracker::onSubmit);\n')

handlers = JAVA / "client/ClientNetworkHandlers.java"
patch(handlers, 'import kr.moonseungjun.arcanecircle.network.GrimoireSnapshotPayload;\n',
      'import kr.moonseungjun.arcanecircle.network.GrimoireSnapshotPayload;\n'
      'import kr.moonseungjun.arcanecircle.network.WorldMagicPayload;\n')
patch(handlers, '        event.register(GrimoireSnapshotPayload.TYPE, ClientNetworkHandlers::handleSnapshot);\n',
      '        event.register(GrimoireSnapshotPayload.TYPE, ClientNetworkHandlers::handleSnapshot);\n'
      '        event.register(WorldMagicPayload.TYPE, ClientNetworkHandlers::handleWorldMagic);\n')
patch(handlers, '    private static void handleSnapshot(GrimoireSnapshotPayload payload, IPayloadContext context) {\n',
'''    private static void handleWorldMagic(WorldMagicPayload payload, IPayloadContext context) {
        WorldMagicTracker.accept(payload);
    }

    private static void handleSnapshot(GrimoireSnapshotPayload payload, IPayloadContext context) {
''')

network = JAVA / "network/ArcaneNetwork.java"
patch(network, '        registrar.playToClient(GrimoireSnapshotPayload.TYPE, GrimoireSnapshotPayload.STREAM_CODEC);\n',
      '        registrar.playToClient(GrimoireSnapshotPayload.TYPE, GrimoireSnapshotPayload.STREAM_CODEC);\n'
      '        registrar.playToClient(WorldMagicPayload.TYPE, WorldMagicPayload.STREAM_CODEC);\n')
patch(network, '                + ";queue_extend=" + (SpellCatalog.canExtend(queue) ? 1 : 0)\n',
'''                + ";queue_extend=" + (SpellCatalog.canExtend(queue) ? 1 : 0)
                + ";fusion_charging=" + SpellCastingService.fusionChargingSpell(player)
                + ";fusion_charge_ticks=" + SpellCastingService.fusionChargingTicks(player)
                + ";fusion_charge_required=" + SpellCastingService.fusionChargingRequiredTicks(player)
''')

# Server casting: fusion has a real charge state; all core visuals use client world geometry.
casting_path = JAVA / "magic/SpellCastingService.java"
casting = casting_path.read_text(encoding="utf-8")
casting = casting.replace(
'''    private static final class FusionQueueState {
        private final List<String> ingredients = new ArrayList<>();
        private long updatedAt;
    }
''',
'''    private static final class FusionQueueState {
        private final List<String> ingredients = new ArrayList<>();
        private long updatedAt;
        private long chargeStartedAt = -1L;
        private int requiredTicks;
        private String resultId = "";
    }
''')
casting = casting.replace(
'''        ChargeState charge = new ChargeState(slot, cast.spell().id(), serverClock(player), required);
        CHARGES.put(player.getUUID(), charge);
''',
'''        ChargeState charge = new ChargeState(slot, cast.spell().id(), serverClock(player), required);
        CHARGES.put(player.getUUID(), charge);
        WorldMagicService.charge(player, cast.spell(), false, List.of(), cast.range(), 0.0);
''')
casting = casting.replace(
'''        long elapsed = serverClock(player) - charge.startedAt;
        CHARGES.remove(player.getUUID());
''',
'''        long elapsed = serverClock(player) - charge.startedAt;
        CHARGES.remove(player.getUUID());
        WorldMagicService.stop(player);
''', 1)
casting = casting.replace(
'''    public static void tickCharge(ServerPlayer player) {
        ChargeState charge = CHARGES.get(player.getUUID());
''',
'''    public static void tickCharge(ServerPlayer player) {
        tickFusion(player);
        ChargeState charge = CHARGES.get(player.getUUID());
''')
casting = casting.replace(
'''        // The client draws one persistent vector seal. Completion only arms the spell;
        // releaseSlotCharge performs the cast when the player lets go.
''',
'''        if ((elapsed & 1L) == 0L) {
            WorldMagicService.charge(player, spell, false, List.of(), cast.range(),
                    Math.min(1.0, elapsed / (double) Math.max(1, charge.requiredTicks)));
        }
''')
casting = casting.replace(
'''    public static void cancelCharge(ServerPlayer player, boolean notify) {
        ChargeState removed = CHARGES.remove(player.getUUID());
''',
'''    public static void cancelCharge(ServerPlayer player, boolean notify) {
        ChargeState removed = CHARGES.remove(player.getUUID());
        if (removed != null) WorldMagicService.stop(player);
''')
old_exact = '''        if (exact.isPresent()) {
            SpellDefinition result = SpellCatalog.spell(exact.get().result()).orElseThrow();
            String extension = SpellCatalog.canExtend(queue.ingredients) ? " §8· 세 번째 회로 추가 가능" : "";
            player.sendOverlayMessage(Component.literal("§5[융합 준비] §d" + names + " §f→ §e"
                    + result.name() + " §7· X를 놓아 시전" + extension));
        } else {
            player.sendOverlayMessage(Component.literal("§5[융합 대기] §d" + names + " §7· 후보 "
                    + candidates.size() + "개 · 주문을 하나 더 선택"));
        }
'''
new_exact = '''        if (exact.isPresent()) {
            SpellDefinition result = SpellCatalog.spell(exact.get().result()).orElseThrow();
            if (!result.id().equals(queue.resultId)) {
                queue.resultId = result.id();
                queue.chargeStartedAt = now;
                queue.requiredTicks = requiredFusionCastTicks(player, result, queue.ingredients.size());
            }
            MagicPlayerData.CastPreparation fusion = data(player).prepareFusion(player, queue.ingredients);
            if (fusion.accepted()) {
                WorldMagicService.charge(player, result, true, queue.ingredients, fusion.range(), 0.0);
            }
            String extension = SpellCatalog.canExtend(queue.ingredients) ? " §8· 세 번째 회로 추가 가능" : "";
            player.sendOverlayMessage(Component.literal("§5[융합 전개] §d" + names + " §f→ §e"
                    + result.name() + " §7· " + String.format("%.1f", queue.requiredTicks / 20.0)
                    + "초 유지 후 X를 놓아 시전" + extension));
        } else {
            queue.resultId = "";
            queue.chargeStartedAt = -1L;
            queue.requiredTicks = 0;
            WorldMagicService.stop(player);
            player.sendOverlayMessage(Component.literal("§5[융합 대기] §d" + names + " §7· 후보 "
                    + candidates.size() + "개 · 주문을 하나 더 선택"));
        }
'''
if old_exact not in casting:
    raise RuntimeError("fusion exact branch not found")
casting = casting.replace(old_exact, new_exact)
casting = replace_method(casting, "    public static void commitFusion(ServerPlayer player)", r'''    public static void commitFusion(ServerPlayer player) {
        FusionQueueState queue = FUSION_QUEUES.remove(player.getUUID());
        WorldMagicService.stop(player);
        if (queue == null || queue.ingredients.isEmpty()) return;
        long now = serverClock(player);
        if (now - queue.updatedAt > QUEUE_TIMEOUT_TICKS) {
            player.sendOverlayMessage(Component.literal("§7[융합 취소] 회로 유지 시간이 지나 해제되었습니다."));
            return;
        }
        List<String> ingredients = List.copyOf(queue.ingredients);
        if (ingredients.size() < 2 || queue.resultId.isBlank() || queue.chargeStartedAt < 0L) {
            player.sendOverlayMessage(Component.literal("§7[융합 취소] 완성된 융합식과 전개 시간이 필요합니다."));
            return;
        }
        long elapsed = now - queue.chargeStartedAt;
        if (elapsed < queue.requiredTicks) {
            int percent = (int) Math.round(100.0 * elapsed / Math.max(1, queue.requiredTicks));
            player.sendOverlayMessage(Component.literal("§7[융합 취소] 복합 회로 전개 " + percent
                    + "% · 완성 전에 X를 놓았습니다."));
            return;
        }
        MagicPlayerData data = data(player);
        MagicPlayerData.CastPreparation cast = data.prepareFusion(player, ingredients);
        if (!cast.accepted() || !queue.resultId.equals(cast.spell().id())) {
            fail(player, cast.accepted() ? "융합 중 결과 회로가 변경되었습니다." : cast.message());
            return;
        }
        castPrepared(player, data, cast);
    }''')
casting = casting.replace(
'''    public static void clearFusion(ServerPlayer player, boolean notify) {
        FusionQueueState removed = FUSION_QUEUES.remove(player.getUUID());
''',
'''    public static void clearFusion(ServerPlayer player, boolean notify) {
        FusionQueueState removed = FUSION_QUEUES.remove(player.getUUID());
        if (removed != null) WorldMagicService.stop(player);
''')
insert_before_data = '''    private static MagicPlayerData data(ServerPlayer player) {
'''
extra_methods = r'''    public static String fusionChargingSpell(ServerPlayer player) {
        FusionQueueState state = FUSION_QUEUES.get(player.getUUID());
        return state == null ? "" : state.resultId;
    }

    public static int fusionChargingTicks(ServerPlayer player) {
        FusionQueueState state = FUSION_QUEUES.get(player.getUUID());
        if (state == null || state.chargeStartedAt < 0L) return 0;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, serverClock(player) - state.chargeStartedAt));
    }

    public static int fusionChargingRequiredTicks(ServerPlayer player) {
        FusionQueueState state = FUSION_QUEUES.get(player.getUUID());
        return state == null ? 0 : state.requiredTicks;
    }

    public static int requiredFusionCastTicks(ServerPlayer player, SpellDefinition result, int ingredientCount) {
        MagicPlayerData.MageState state = data(player).state(player);
        int direct = requiredCastTicks(player, result);
        int masteryTier = SpellCatalog.masteryTier(state.mastery(result.id()));
        boolean registered = state.known().contains(result.id());
        int unfamiliarPenalty = registered ? 7 : 18 + ingredientCount * 5 + result.circle() * 2;
        return Math.max(direct + 5, direct + unfamiliarPenalty - masteryTier * 2);
    }

    private static void tickFusion(ServerPlayer player) {
        FusionQueueState queue = FUSION_QUEUES.get(player.getUUID());
        if (queue == null || queue.resultId.isBlank() || queue.chargeStartedAt < 0L) return;
        long now = serverClock(player);
        if (now - queue.updatedAt > QUEUE_TIMEOUT_TICKS) {
            FUSION_QUEUES.remove(player.getUUID());
            WorldMagicService.stop(player);
            return;
        }
        SpellDefinition result = SpellCatalog.spell(queue.resultId).orElse(null);
        if (result == null) {
            FUSION_QUEUES.remove(player.getUUID());
            WorldMagicService.stop(player);
            return;
        }
        MagicPlayerData.CastPreparation cast = data(player).prepareFusion(player, queue.ingredients);
        if (!cast.accepted() || !queue.resultId.equals(cast.spell().id())) {
            FUSION_QUEUES.remove(player.getUUID());
            WorldMagicService.stop(player);
            return;
        }
        long elapsed = now - queue.chargeStartedAt;
        if ((elapsed & 1L) == 0L) {
            WorldMagicService.charge(player, result, true, queue.ingredients, cast.range(),
                    Math.min(1.0, elapsed / (double) Math.max(1, queue.requiredTicks)));
        }
    }

'''
if insert_before_data not in casting:
    raise RuntimeError("data method insertion point missing")
casting = casting.replace(insert_before_data, extra_methods + insert_before_data, 1)
casting = casting.replace(
'''        CombatGrowthService.Snapshot combatSnapshot = CombatGrowthService.capture(player, cast.range());
        releasePrelude(player, cast);
''',
'''        CombatGrowthService.Snapshot combatSnapshot = CombatGrowthService.capture(player, cast.range());
        WorldMagicService.release(player, cast);
        releasePrelude(player, cast);
''')
# Remove every explicit particle spawn from the server spell implementation.
casting = strip_calls(casting, "level.sendParticles")
# Visual helper bodies remain API-compatible but do no server-side work.
for signature, replacement in [
    ("    private static void renderAnchoredSigil(", "    private static void renderAnchoredSigil(ServerLevel level, ServerPlayer player, SpellDefinition spell, double range, double radius, int density) {}"),
    ("    private static void verticalSigil(", "    private static void verticalSigil(ServerLevel level, Vec3 center, Vec3 normal, SpellDefinition spell, double radius, int density) {}"),
    ("    private static void planeRing(", "    private static void planeRing(ServerLevel level, Vec3 center, Vec3 right, Vec3 up, double radius, ParticleOptions particle, int points) {}"),
    ("    private static void planePolygon(", "    private static void planePolygon(ServerLevel level, Vec3 center, Vec3 right, Vec3 up, double radius, int sides, ParticleOptions particle, int pointsPerEdge) {}"),
    ("    private static void planeLine(", "    private static void planeLine(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points) {}"),
    ("    private static void horizontalSigil(", "    private static void horizontalSigil(ServerLevel level, Vec3 center, SpellDefinition spell, double radius, int density) {}"),
    ("    private static void healingVisual(", "    private static void healingVisual(ServerLevel level, Vec3 center, double scale) {}"),
    ("    private static void spiralBeam(", "    private static void spiralBeam(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions core, ParticleOptions accent, int points) {}"),
    ("    private static void particleLine(", "    private static void particleLine(ServerLevel level, Vec3 start, Vec3 end, ParticleOptions particle, int points) {}"),
    ("    private static void ring(", "    private static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions particle, int points) {}"),
    ("    private static void dome(", "    private static void dome(ServerLevel level, Vec3 center, double radius, ParticleOptions particle) {}"),
    ("    private static void burst(", "    private static void burst(ServerLevel level, Vec3 center, ParticleOptions particle, int count, double spread) {}"),
]:
    if signature in casting:
        casting = replace_method(casting, signature, replacement)
if "sendParticles(" in casting:
    raise RuntimeError("particle spawn remains in SpellCastingService")
casting_path.write_text(casting, encoding="utf-8")

# Runtime JAR must contain world geometry and must not regress to screen/particle circles.
verify = ROOT / "tools/verify_jar.py"
verify_text = verify.read_text(encoding="utf-8")
needle = '    "kr/moonseungjun/arcanecircle/ArcaneCircle.class",\n'
addition = ('    "kr/moonseungjun/arcanecircle/ArcaneCircle.class",\n'
            '    "kr/moonseungjun/arcanecircle/network/WorldMagicPayload.class",\n'
            '    "kr/moonseungjun/arcanecircle/magic/WorldMagicService.class",\n'
            '    "kr/moonseungjun/arcanecircle/client/WorldMagicTracker.class",\n')
if needle not in verify_text:
    raise RuntimeError("verify_jAR required entry insertion point missing")
verify.write_text(verify_text.replace(needle, addition), encoding="utf-8")

print("Arcane Circle v0.12 world geometry, charged fusion and compact HUD overhaul applied")
