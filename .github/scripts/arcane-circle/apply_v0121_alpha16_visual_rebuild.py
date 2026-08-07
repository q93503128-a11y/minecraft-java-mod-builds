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
        raise SystemExit(f"{rel}: expected one target, found {count}: {old[:100]!r}")
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
    replace_once("gradle.properties", "mod_version=0.12.1-alpha.15", "mod_version=0.12.1-alpha.16", "mod_version=0.12.1-alpha.16")
    path = "src/main/java/kr/moonseungjun/arcanecircle/ArcaneCircle.java"
    text = read(path)
    if 'public static final String VERSION = "0.12.1-alpha.16";' not in text:
        text, n = re.subn(r'public static final String VERSION = "0\.12\.1-alpha\.\d+";',
                          'public static final String VERSION = "0.12.1-alpha.16";', text, count=1)
        if n != 1:
            raise SystemExit(f"{path}: VERSION constant not found")
        write(path, text)


def patch_anchor_visibility() -> None:
    path = "src/main/java/kr/moonseungjun/arcanecircle/magic/WorldMagicService.java"
    replacement = '''    private static Vec3 anchorCenter(ServerPlayer player, SpellDefinition spell, double range, Vec3 look) {
        return switch (spell.sigilAnchor()) {
            case FRONT -> visibleFrontAnchor(player, spell, look);
            case FEET, GROUND_SELF -> player.position().add(0.0, 0.055, 0.0);
            case BODY -> player.position().add(0.0, 1.0, 0.0);
            case GROUND_TARGET -> aimGround(player, Math.max(4.0, range));
            case TARGET -> visiblePoint(player, look, Math.min(Math.max(3.0, range * 0.72), 18.0));
        };
    }

    /**
     * Keeps a front-facing charge sigil on the caster side of nearby collision geometry. The old
     * fixed 1.6-1.9 block offset could put the entire plate behind a wall when casting indoors.
     */
    private static Vec3 visibleFrontAnchor(ServerPlayer player, SpellDefinition spell, Vec3 look) {
        double desired = 1.02 + Math.min(0.58, spell.circle() * 0.065);
        return visiblePoint(player, look, desired);
    }

    private static Vec3 visiblePoint(ServerPlayer player, Vec3 look, double desiredDistance) {
        ServerLevel level = (ServerLevel) player.level();
        Vec3 origin = player.getEyePosition();
        Vec3 direction = safeDirection(look);
        double minDistance = 0.48;
        Vec3 lastVisible = origin.add(direction.scale(minDistance));
        double max = Math.max(minDistance, desiredDistance);
        for (double distance = minDistance; distance <= max + 1.0E-6; distance += 0.07) {
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
        double max = Math.min(Math.max(2.0, range), 28.0);

        // March from the caster outward and stop at the first collision. This prevents a target
        // glyph from being selected on a floor hidden behind a wall or closed door.
        for (double distance = 0.70; distance <= max; distance += 0.32) {
            Vec3 sample = origin.add(look.scale(distance));
            BlockPos samplePos = BlockPos.containing(sample);
            BlockState sampleState = level.getBlockState(samplePos);
            if (!sampleState.getCollisionShape(level, samplePos).isEmpty()) {
                if (sampleState.isFaceSturdy(level, samplePos, Direction.UP)) {
                    bestVisibleFloor = Vec3.atCenterOf(samplePos.above()).add(0.0, -0.48, 0.0);
                }
                break;
            }
            for (int down = 0; down <= 10; down++) {
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

'''
    replace_block(path,
                  "    private static Vec3 anchorCenter(ServerPlayer player, SpellDefinition spell, double range, Vec3 look) {",
                  "    private static Vec3 safeDirection(Vec3 value) {",
                  replacement,
                  "visibleFrontAnchor(ServerPlayer player, SpellDefinition spell, Vec3 look)")


def patch_mesh_color_and_glow() -> None:
    path = "src/main/java/kr/moonseungjun/arcanecircle/client/ArcaneWorldMesh.java"
    text = read(path)
    if "VIVID_SATURATION=1.72" in text:
        return
    old_submit = '''    void submit(PoseStack poseStack,SubmitNodeCollector collector,int argb,float windowScale){
        if(faces.isEmpty()&&segments.isEmpty())return;
        if(!faces.isEmpty())collector.submitCustomGeometry(poseStack,RenderTypes.debugFilledBox(),(pose,out)->{
            for(Face face:faces){int color=tone(argb,face.brightness,face.alpha);vertex(out,pose,face.a,color);vertex(out,pose,face.b,color);vertex(out,pose,face.c,color);vertex(out,pose,face.d,color);}
        });
        if(!segments.isEmpty()){
            submitLines(poseStack,collector,tone(argb,.58,.72),windowScale*2.10F);
            submitLines(poseStack,collector,tone(argb,1.02,1.0),windowScale*.92F);
        }
    }
'''
    new_submit = '''    void submit(PoseStack poseStack,SubmitNodeCollector collector,int argb,float windowScale){
        if(faces.isEmpty()&&segments.isEmpty())return;
        if(!faces.isEmpty())collector.submitCustomGeometry(poseStack,RenderTypes.debugFilledBox(),(pose,out)->{
            for(Face face:faces){int color=tone(argb,face.brightness,face.alpha);vertex(out,pose,face.a,color);vertex(out,pose,face.b,color);vertex(out,pose,face.c,color);vertex(out,pose,face.d,color);}
        });
        if(!segments.isEmpty()){
            // Three edge passes create a saturated halo + readable mid edge + white-hot colored core.
            // This keeps the effect punchy without relying on thousands of vanilla particles.
            submitLines(poseStack,collector,tone(argb,.64,.24),windowScale*4.10F);
            submitLines(poseStack,collector,tone(argb,.88,.58),windowScale*2.05F);
            submitLines(poseStack,collector,tone(argb,1.06,1.0),windowScale*.82F);
        }
    }
'''
    if old_submit not in text:
        raise SystemExit(f"{path}: submit method anchor changed")
    text = text.replace(old_submit, new_submit, 1)
    old_tone = '''    private static final double SATURATION_BOOST=1.28;
    private static final double ALPHA_BOOST=1.32;
    private static int tone(int argb,double brightness,double alphaScale){
        int baseA=(argb>>>24)&255,baseR=(argb>>>16)&255,baseG=(argb>>>8)&255,baseB=argb&255;
        double average=(baseR+baseG+baseB)/3.0;
        int a=(int)Math.round(baseA*Math.min(1.0,alphaScale*ALPHA_BOOST));
        int r=(int)Math.round((average+(baseR-average)*SATURATION_BOOST)*brightness);
        int g=(int)Math.round((average+(baseG-average)*SATURATION_BOOST)*brightness);
        int b=(int)Math.round((average+(baseB-average)*SATURATION_BOOST)*brightness);
        return(clamp(a)<<24)|(clamp(r)<<16)|(clamp(g)<<8)|clamp(b);
    }
'''
    new_tone = '''    private static final double VIVID_SATURATION=1.72;
    private static final double FACE_ALPHA_BOOST=1.52;
    private static int tone(int argb,double brightness,double alphaScale){
        int baseA=(argb>>>24)&255,baseR=(argb>>>16)&255,baseG=(argb>>>8)&255,baseB=argb&255;
        // Luma-based saturation keeps the dominant school color strong instead of bleaching it
        // toward pastel white when brightness is raised.
        double luma=baseR*.2126+baseG*.7152+baseB*.0722;
        int a=(int)Math.round(baseA*Math.min(1.0,alphaScale*FACE_ALPHA_BOOST));
        int r=(int)Math.round((luma+(baseR-luma)*VIVID_SATURATION)*brightness);
        int g=(int)Math.round((luma+(baseG-luma)*VIVID_SATURATION)*brightness);
        int b=(int)Math.round((luma+(baseB-luma)*VIVID_SATURATION)*brightness);
        return(clamp(a)<<24)|(clamp(r)<<16)|(clamp(g)<<8)|clamp(b);
    }
'''
    if old_tone not in text:
        raise SystemExit(f"{path}: tone method anchor changed")
    write(path, text.replace(old_tone, new_tone, 1))


def patch_world_magic_tracker() -> None:
    path = "src/main/java/kr/moonseungjun/arcanecircle/client/WorldMagicTracker.java"
    text = read(path)
    if "GRAND_ARRAY_RADIUS" in text:
        return
    text = text.replace("    private static final int MAX_CHARGE_GEOMETRY = 920;\n",
                        "    private static final int MAX_CHARGE_GEOMETRY = 3200;\n", 1)
    text = text.replace("    private static final int MAX_RELEASE_GEOMETRY = 1280;\n",
                        "    private static final int MAX_RELEASE_GEOMETRY = 2200;\n", 1)
    text = text.replace("    private static final int MAX_VISUALS = 18;\n",
                        "    private static final int MAX_VISUALS = 14;\n", 1)
    text = text.replace("    private static final int MAX_FRAME = 4200;\n",
                        "    private static final int MAX_FRAME = 18000;\n", 1)
    insert_anchor = "    private static final long CHARGE_TTL = 2_250_000_000L;\n"
    text = text.replace(insert_anchor, insert_anchor +
                        "    private static final double[] GRAND_ARRAY_RADIUS = {0.0, 0.68, 0.82, 0.98, 1.18, 1.45, 1.82, 2.30, 2.92, 3.72};\n", 1)

    start = text.find("    private static ArcaneWorldMesh buildCharge(Visual visual) {")
    end = text.find("    private static ArcaneWorldMesh buildRelease(Visual visual, double age) {", start)
    if start < 0 or end < 0:
        raise SystemExit(f"{path}: buildCharge block not found")
    charge = '''    private static ArcaneWorldMesh buildCharge(Visual visual) {
        SpellDefinition spell = visual.spell;
        ArcaneWorldMesh.Builder mesh = ArcaneWorldMesh.builder(MAX_CHARGE_GEOMETRY);
        ArcaneWorldMesh.Basis basis = basis(spell, visual.direction);
        int circle = clampCircle(spell.circle());
        double p = Math.max(0.04, visual.progress);
        double time = Math.max(0.0, (System.nanoTime() - visual.startedAt) / 1_000_000_000.0);
        double rangeScale = clamp(Math.pow(Math.max(0.25,
                visual.range / Math.max(4.0, spell.range())), 0.08), 0.92, 1.12);
        double outer = GRAND_ARRAY_RADIUS[circle] * rangeScale * (visual.fusion ? 1.16 : 1.0);
        double rotation = Math.floorMod(spell.id().hashCode(), 360) * Math.PI / 180.0
                + time * (0.30 + circle * 0.045);
        double counter = -rotation * (0.72 + circle * 0.018);
        double pulse = 0.965 + Math.sin(time * (2.4 + circle * 0.12)) * 0.035;
        outer *= pulse;

        // A dark translucent plate gives the saturated circuitry enough contrast against bright
        // terrain. It stays subtle; the emissive-looking edges carry the spectacle.
        mesh.disc(basis, Vec3.ZERO, outer * (0.94 + p * 0.04), 52 + circle * 7,
                0.30F, (float) (0.035 + p * 0.075));

        // Circle count remains semantically readable: a 1C spell has one complete circuit and a
        // 9C spell has nine, but radius and line density grow much faster after 5C.
        for (int layer = 1; layer <= circle && !mesh.full(); layer++) {
            double t = layer / (double) circle;
            double radius = outer * (0.15 + 0.80 * t);
            double thickness = outer * (0.012 + (layer % 3) * 0.007 + circle * 0.0016);
            float brightness = (float) (0.90 + 0.30 * t + (layer % 2) * 0.09);
            float alpha = (float) ((0.20 + 0.30 * p) * (0.76 + 0.24 * t));
            mesh.band(basis, Vec3.ZERO, Math.max(0.01, radius - thickness), radius,
                    48 + layer * 6, brightness, alpha);
        }

        // Outer crown and school seal establish the main silhouette before small runes appear.
        mesh.brokenBand(basis, Vec3.ZERO, outer * 0.965, outer * 1.015,
                68 + circle * 7, 5 + circle % 4, 1.24F, (float) (0.28 + p * 0.24));
        double sealRadius = outer * (0.31 + Math.min(0.10, circle * 0.011));
        schoolSeal(mesh, spell, basis, sealRadius, rotation, Math.min(1.0, p * 1.22));

        if (circle >= 2) {
            mesh.runeChords(basis, Vec3.ZERO, outer * 0.49,
                    6 + circle * 2, 2 + circle % 4, counter,
                    (float) (0.72 + circle * 0.045));
        }
        if (circle >= 3) {
            int nodes = 3 + circle / 2;
            for (int i = 0; i < nodes && !mesh.full(); i++) {
                double angle = rotation * 0.62 + Math.PI * 2.0 * i / nodes;
                Vec3 node = basis.point(angle, outer * 0.70);
                double r = outer * (0.035 + circle * 0.0025);
                mesh.diamond(basis, node, r * 1.55, -angle, 1.30F, 0.48F);
                mesh.line(node, basis.point(angle, outer * 0.86), 0.84F);
            }
        }
        if (circle >= 4) {
            mesh.starPlate(basis, Vec3.ZERO, outer * 0.61, outer * 0.39,
                    4 + circle / 2, counter, 0.72F, (float) (0.10 + p * 0.13));
            mesh.star(basis, Vec3.ZERO, outer * 0.76, outer * 0.57,
                    5 + circle / 2, rotation * 0.48, 1.12F);
        }

        int glyphs = Math.min(28, 5 + circle * 3);
        int visible = Math.max(1, (int) Math.ceil(glyphs * Math.min(1.0, p * 1.08)));
        for (int i = 0; i < visible && !mesh.full(); i++) {
            double angle = counter + Math.PI * 2.0 * i / glyphs;
            Vec3 glyph = basis.point(angle, outer * (0.79 + (i % 2) * 0.045));
            double size = outer * (0.022 + (i % 4) * 0.005);
            if ((i % 4) == 0) mesh.diamond(basis, glyph, size * 1.48, -angle, 1.34F, 0.52F);
            else if ((i % 4) == 1) mesh.disc(basis, glyph, size, 14, 1.18F, 0.42F);
            else if ((i % 4) == 2) mesh.polygonPlate(basis, glyph, size * 1.24, 3, angle, 1.24F, 0.44F);
            else mesh.starPlate(basis, glyph, size * 1.38, size * 0.48, 4, -angle, 1.28F, 0.42F);
        }

        // 5C is the first visibly "advanced" array: a second counter-rotating script band and
        // radial circuit spokes appear, rather than only adding another ring.
        if (circle >= 5) {
            mesh.brokenBand(basis, Vec3.ZERO, outer * 0.57, outer * 0.615,
                    72 + circle * 6, 4, 1.12F, (float) (0.23 + p * 0.22));
            int spokes = 8 + circle * 2;
            for (int i = 0; i < spokes && !mesh.full(); i++) {
                double angle = rotation * 0.38 + Math.PI * 2.0 * i / spokes;
                Vec3 a = basis.point(angle, outer * (0.34 + (i % 3) * 0.035));
                Vec3 b = basis.point(angle + (i % 2 == 0 ? 0.045 : -0.045), outer * 0.54);
                mesh.line(a, b, i % 4 == 0 ? 1.34F : 0.72F);
            }
        }

        // 6C+ breaks out of the flat plane. Multiple gyroscopic circuits make the jump in mage
        // tier readable even from the side and prevent high circles from looking like scaled 1C art.
        if (circle >= 6) {
            Vec3 normal = basis.normal();
            ArcaneWorldMesh.Basis tiltA = ArcaneWorldMesh.Basis.fromNormal(
                    basis.right().add(normal.scale(0.66)), basis.up());
            ArcaneWorldMesh.Basis tiltB = ArcaneWorldMesh.Basis.fromNormal(
                    basis.up().add(normal.scale(0.58)), basis.right());
            mesh.brokenBand(tiltA, Vec3.ZERO, outer * 0.79, outer * 0.86,
                    72 + circle * 5, 5, 1.24F, (float) (0.24 + p * 0.22));
            mesh.brokenBand(tiltB, Vec3.ZERO, outer * 0.63, outer * 0.70,
                    66 + circle * 5, 6, 1.02F, (float) (0.20 + p * 0.19));
            int orbiters = 4 + circle / 2;
            for (int i = 0; i < orbiters && !mesh.full(); i++) {
                double angle = -time * (0.52 + circle * 0.03) + Math.PI * 2.0 * i / orbiters;
                Vec3 node = tiltA.point(angle, outer * 0.94);
                mesh.orb(node, outer * 0.035, 16, 1.24F, 0.48F);
            }
        }

        if (circle >= 7) {
            Vec3 normal = basis.normal();
            ArcaneWorldMesh.Basis crown = ArcaneWorldMesh.Basis.fromNormal(
                    basis.right().add(basis.up()).add(normal.scale(0.82)), basis.up());
            mesh.brokenBand(crown, Vec3.ZERO, outer * 1.05, outer * 1.10,
                    84 + circle * 6, 7, 1.18F, (float) (0.18 + p * 0.20));
            mesh.star(crown, Vec3.ZERO, outer * 0.47, outer * 0.28,
                    7, time * 0.62, 1.05F);
        }

        // 8C receives orbiting sub-arrays rather than just more line density.
        if (circle >= 8) {
            int satellites = circle == 9 ? 9 : 6;
            for (int i = 0; i < satellites && !mesh.full(); i++) {
                double angle = rotation * 0.34 + Math.PI * 2.0 * i / satellites;
                Vec3 node = basis.point(angle, outer * 1.18);
                double sub = outer * (circle == 9 ? 0.105 : 0.090);
                mesh.disc(basis, node, sub, 22, 0.54F, 0.16F);
                mesh.band(basis, node, sub * 0.68, sub, 30, 1.34F, 0.50F);
                mesh.star(basis, node, sub * 0.62, sub * 0.26,
                        circle == 9 ? 5 : 4, -counter + i, 0.94F);
            }
            mesh.orb(Vec3.ZERO, outer * (circle == 9 ? 0.34 : 0.27),
                    32 + circle * 3, 0.86F, (float) (0.10 + p * 0.12));
        }

        // 9C grand array: three mutually tilted crowns, nine satellite sigils and a dense central
        // seal. This is deliberately several times the physical size and geometry budget of 1C.
        if (circle == 9) {
            Vec3 normal = basis.normal();
            ArcaneWorldMesh.Basis axisA = ArcaneWorldMesh.Basis.fromNormal(
                    basis.right().add(normal.scale(0.92)), basis.up());
            ArcaneWorldMesh.Basis axisB = ArcaneWorldMesh.Basis.fromNormal(
                    basis.up().add(normal.scale(0.88)), basis.right());
            ArcaneWorldMesh.Basis axisC = ArcaneWorldMesh.Basis.fromNormal(
                    basis.right().subtract(basis.up()).add(normal.scale(0.74)), basis.up());
            mesh.brokenBand(axisA, Vec3.ZERO, outer * 1.24, outer * 1.30, 104, 8, 1.32F, 0.46F);
            mesh.brokenBand(axisB, Vec3.ZERO, outer * 1.10, outer * 1.16, 96, 7, 1.18F, 0.40F);
            mesh.brokenBand(axisC, Vec3.ZERO, outer * 0.91, outer * 0.97, 92, 6, 1.08F, 0.36F);
            mesh.starPlate(basis, Vec3.ZERO, outer * 0.29, outer * 0.105,
                    9, -time * 0.86, 1.36F, (float) (0.24 + p * 0.22));
            for (int i = 0; i < 9 && !mesh.full(); i++) {
                double angle = time * 0.24 + Math.PI * 2.0 * i / 9.0;
                Vec3 outerNode = basis.point(angle, outer * 1.38);
                Vec3 innerNode = basis.point(angle + (i % 2 == 0 ? 0.08 : -0.08), outer * 0.92);
                mesh.line(innerNode, outerNode, i % 3 == 0 ? 1.62F : 0.92F);
                mesh.diamond(basis, outerNode, outer * 0.037, angle, 1.42F, 0.56F);
            }
        }

        if (visual.fusion && visual.ingredients >= 2) {
            int count = Math.min(3, visual.ingredients);
            for (int i = 0; i < count && !mesh.full(); i++) {
                double angle = -rotation + Math.PI * 2.0 * i / count;
                Vec3 node = basis.point(angle, outer * 1.48);
                double radius = outer * (0.10 + count * 0.010);
                mesh.disc(basis, node, radius, 24, 0.62F, 0.16F);
                mesh.band(basis, node, radius * 0.64, radius, 34, 1.38F, 0.54F);
                mesh.star(basis, node, radius * 0.62, radius * 0.24,
                        3 + i, rotation + i, 1.08F);
            }
            mesh.brokenBand(basis, Vec3.ZERO, outer * 1.53, outer * 1.59,
                    88, 7, 1.30F, 0.42F);
        }
        return mesh.build();
    }

'''
    text = text[:start] + charge + text[end:]

    old_power = '''        double powerFactor = clamp(Math.pow(Math.max(0.08,
                visual.power / Math.max(1.0, spell.power())), 0.18), 0.82, 2.0);
'''
    new_power = '''        double powerFactor = clamp(Math.pow(Math.max(0.08,
                visual.power / Math.max(1.0, spell.power())), 0.18), 0.82, 2.0)
                * spectacleScale(circle);
'''
    if old_power not in text:
        raise SystemExit(f"{path}: release power factor anchor not found")
    text = text.replace(old_power, new_power, 1)

    # Add a residual high-circle crown before every release mesh is returned.
    old_return = '''        if (visual.fusion) {
            double ring = 0.48 + circle * 0.07;
            mesh.brokenBand(facing, Vec3.ZERO, ring, ring + 0.055,
                    48, 5, 1.20F, (float) (0.18 * (1.0 - age)));
        }
        return mesh.build();
    }
'''
    new_return = '''        if (visual.fusion) {
            double ring = 0.48 + circle * 0.07;
            mesh.brokenBand(facing, Vec3.ZERO, ring, ring + 0.055,
                    48, 5, 1.20F, (float) (0.18 * (1.0 - age)));
        }
        addReleaseCrown(mesh, visual, age);
        return mesh.build();
    }

    private static void addReleaseCrown(ArcaneWorldMesh.Builder mesh, Visual visual, double age) {
        int circle = clampCircle(visual.spell.circle());
        if (circle < 6) return;
        double fade = clamp((1.0 - age) / 0.36, 0.0, 1.0);
        if (fade <= 0.0) return;
        ArcaneWorldMesh.Basis facing = ArcaneWorldMesh.Basis.facing(visual.direction);
        double base = GRAND_ARRAY_RADIUS[circle] * (0.54 + (circle - 6) * 0.045);
        double rot = (System.nanoTime() - visual.startedAt) / 1_000_000_000.0 * 0.74;
        mesh.brokenBand(facing, Vec3.ZERO, base * 0.83, base, 68 + circle * 5,
                5 + circle % 3, 1.30F, (float) (0.42 * fade));
        mesh.star(facing, Vec3.ZERO, base * 0.66, base * 0.42,
                5 + circle / 2, rot, 1.22F);
        if (circle >= 8) {
            ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                    facing.right().add(visual.direction.scale(0.70)), facing.up());
            mesh.brokenBand(tilt, Vec3.ZERO, base * 0.72, base * 0.80,
                    72, 6, 1.14F, (float) (0.32 * fade));
        }
        if (circle == 9) {
            ArcaneWorldMesh.Basis tilt = ArcaneWorldMesh.Basis.fromNormal(
                    facing.up().add(visual.direction.scale(0.78)), facing.right());
            mesh.brokenBand(tilt, Vec3.ZERO, base * 0.95, base * 1.03,
                    84, 7, 1.28F, (float) (0.36 * fade));
        }
    }

    private static double spectacleScale(int circle) {
        return switch (clampCircle(circle)) {
            case 1, 2, 3 -> 1.0;
            case 4 -> 1.04;
            case 5 -> 1.10;
            case 6 -> 1.18;
            case 7 -> 1.29;
            case 8 -> 1.43;
            case 9 -> 1.62;
            default -> 1.0;
        };
    }
'''
    if old_return not in text:
        raise SystemExit(f"{path}: buildRelease return anchor not found")
    text = text.replace(old_return, new_return, 1)

    old_color = '''    private static int color(SpellDefinition spell) {
        return switch (spell.school()) {
            case FIRE -> 0xEFFF633D;
            case FROST -> 0xEF69E4FF;
            case WIND -> 0xE873E8C2;
            case WARD -> 0xEFC89AFF;
            case LIFE -> 0xEF74E894;
            case SPACE -> 0xEFA778FF;
            default -> 0xEF829FFF;
        };
    }
'''
    new_color = '''    private static int color(SpellDefinition spell) {
        // Fully saturated school palette. Geometry alpha is controlled per face, so the base color
        // itself should never start washed out.
        return switch (spell.school()) {
            case FIRE -> 0xFFFF2A08;
            case FROST -> 0xFF18C8FF;
            case WIND -> 0xFF00F0A8;
            case WARD -> 0xFF8A35FF;
            case LIFE -> 0xFF25E85A;
            case SPACE -> 0xFFD51CFF;
            default -> 0xFF3E63FF;
        };
    }
'''
    if old_color not in text:
        raise SystemExit(f"{path}: color palette anchor not found")
    write(path, text.replace(old_color, new_color, 1))


def main() -> None:
    patch_version()
    patch_anchor_visibility()
    patch_mesh_color_and_glow()
    patch_world_magic_tracker()
    print("Arcane Circle alpha.16 high-circle visual rebuild migration: PASS")


if __name__ == "__main__":
    main()
