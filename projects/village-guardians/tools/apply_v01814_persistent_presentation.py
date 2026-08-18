#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected one match in {path.name}, got {count}: {old[:220]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def all_existing(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old in text:
        path.write_text(text.replace(old, new), encoding="utf-8")


def main() -> None:
    once(ROOT / "gradle.properties", "mod_version=0.18.13-alpha.1", "mod_version=0.18.14-alpha.1")
    for test in (ROOT / "tools").glob("test_*.py"):
        all_existing(test, "mod_version=0.18.13-alpha.1", "mod_version=0.18.14-alpha.1")

    old_contract = ROOT / "tools/test_v01811_defense_polish.py"
    once(old_contract,
'''    assert "turretCap" in turret and "POLISHED_BLACKSTONE_BRICK_WALL" in turret
''',
'''    assert "Blocks.BARRIER" in turret and "VillageTurretPresentationSystem.show" in turret
''')

    turret = JAVA / "VillagePlacedTurretSystem.java"
    once(turret,
'''        if (server != null) rebuildVisuals(server.overworld());''',
'''        if (server != null) {
            ServerLevel level = server.overworld();
            VillageTurretPresentationSystem.initialize(level, states());
            rebuildVisuals(level);
        }''')
    once(turret,
'''    public static void tick(MinecraftServer server) {
        if (server == null) return;
        tickDisruptions();
        if (!VillageRaidSystem.isActive()) {
            PENDING_BOMBARDS.clear();
            DISABLED_TICKS.clear();
                return;
        }
        combatTicks++;
        ServerLevel level = server.overworld();
        resolveBombards(level);''',
'''    public static void tick(MinecraftServer server) {
        if (server == null) return;
        tickDisruptions();
        ServerLevel level = server.overworld();
        VillageTurretPresentationSystem.tick(level, states());
        if (!VillageRaidSystem.isActive()) {
            PENDING_BOMBARDS.clear();
            DISABLED_TICKS.clear();
            return;
        }
        combatTicks++;
        resolveBombards(level);''')
    once(turret,
'''        if (target == null) return;
        float damage = (state.type().damage() + (state.level() - 1) * state.type().damage() * 0.16f)''',
'''        if (target == null) return;
        VillageTurretPresentationSystem.aim(level, state,
                target.position().add(0.0, target.getBbHeight() * 0.55, 0.0));
        float damage = (state.type().damage() + (state.level() - 1) * state.type().damage() * 0.16f)''')
    once(turret,
'''        if (player.level() instanceof ServerLevel level) clearVisual(level, state.pos());''',
'''        if (player.level() instanceof ServerLevel level) clearVisual(level, state);''')
    once(turret,
'''    private static void rebuildVisuals(ServerLevel level) { for (TurretState state : TURRETS.values()) buildVisual(level, state); }
    private static void buildVisual(ServerLevel level, TurretState state) {
        if (!state.active()) { buildWreck(level, state); return; }
        Block base = state.level() >= 4 ? Blocks.POLISHED_BLACKSTONE_BRICK_WALL : Blocks.STONE_BRICK_WALL;
        VillageFortressTerrain.set(level, state.pos(), base);
        VillageFortressTerrain.set(level, state.pos().above(), state.type().visual());
        VillageFortressTerrain.set(level, state.pos().above(2), turretCap(state.type()));
    }
    private static Block turretCap(TurretType type) {
        return switch (type) {
            case BALLISTA, REPEATER, PIERCER -> Blocks.IRON_BARS;
            case FLAME -> Blocks.SOUL_LANTERN;
            case FROST -> Blocks.BLUE_ICE;
            case CHAIN -> Blocks.REDSTONE_LAMP;
            case BOMBARD -> Blocks.HEAVY_CORE;
            case NULLIFIER -> Blocks.END_ROD;
            case ANTI_AIR -> Blocks.IRON_BARS;
            case BEACON -> Blocks.SEA_LANTERN;
        };
    }
    private static void buildWreck(ServerLevel level, TurretState state) {
        VillageFortressTerrain.set(level, state.pos(), Blocks.CRACKED_STONE_BRICKS);
        VillageFortressTerrain.set(level, state.pos().above(), Blocks.AIR);
        VillageFortressTerrain.set(level, state.pos().above(2), Blocks.AIR);
    }
    private static void clearVisual(ServerLevel level, BlockPos pos) {
        VillageFortressTerrain.set(level, pos, Blocks.AIR);
        VillageFortressTerrain.set(level, pos.above(), Blocks.AIR);
        VillageFortressTerrain.set(level, pos.above(2), Blocks.AIR);
    }''',
'''    private static void rebuildVisuals(ServerLevel level) { for (TurretState state : TURRETS.values()) buildVisual(level, state); }
    private static void buildVisual(ServerLevel level, TurretState state) {
        if (!state.active()) { buildWreck(level, state); return; }
        Block base = state.level() >= 4 ? Blocks.POLISHED_BLACKSTONE_BRICK_WALL : Blocks.STONE_BRICK_WALL;
        VillageFortressTerrain.set(level, state.pos(), base);
        // Collision and raycast footprint stay physical; the visible machinery is a synchronized procedural mesh actor.
        VillageFortressTerrain.set(level, state.pos().above(), Blocks.BARRIER);
        VillageFortressTerrain.set(level, state.pos().above(2), Blocks.BARRIER);
        VillageTurretPresentationSystem.show(level, state);
    }
    private static void buildWreck(ServerLevel level, TurretState state) {
        VillageFortressTerrain.set(level, state.pos(), Blocks.CRACKED_STONE_BRICKS);
        VillageFortressTerrain.set(level, state.pos().above(), Blocks.AIR);
        VillageFortressTerrain.set(level, state.pos().above(2), Blocks.AIR);
        VillageTurretPresentationSystem.show(level, state);
    }
    private static void clearVisual(ServerLevel level, TurretState state) {
        VillageTurretPresentationSystem.remove(level, state.id());
        BlockPos pos = state.pos();
        VillageFortressTerrain.set(level, pos, Blocks.AIR);
        VillageFortressTerrain.set(level, pos.above(), Blocks.AIR);
        VillageFortressTerrain.set(level, pos.above(2), Blocks.AIR);
    }''')

    effect = JAVA / "VillageSkillEffectEntity.java"
    once(effect,
'''        if (followsOwner() && owner != null && owner.isAlive()) {
            if (tracksOwnerLook()) {
                Vec3 look = owner.getLookAngle();
                if (kind().startsWith("warden_")) look = new Vec3(look.x, 0.0, look.z);
                if (look.lengthSqr() > 1.0E-6) setDirection(look.normalize());
            }
            Vec3 target = switch (kind()) {
                case "ranger_energy_charge" -> owner.getEyePosition().add(direction().scale(2.35));
                case "ranger_focus" -> owner.getEyePosition().add(direction().scale(1.15));
                case "vanguard_slam_charge" -> owner.position().add(0.0, 0.2, 0.0);
                default -> owner.position();
            };
            setPos(target);
        } else if (speed() != 0.0f) {''',
'''        if (followsOwner()) {
            if (owner == null || !owner.isAlive()) {
                discard();
                return;
            }
            if (tracksOwnerLook()) {
                Vec3 look = owner.getLookAngle();
                if (kind().startsWith("warden_")) look = new Vec3(look.x, 0.0, look.z);
                if (look.lengthSqr() > 1.0E-6) setDirection(look.normalize());
            }
            Vec3 target = switch (kind()) {
                case "ranger_energy_charge" -> owner.getEyePosition().add(direction().scale(2.35));
                case "ranger_focus" -> owner.getEyePosition().add(direction().scale(1.15));
                case "vanguard_slam_charge" -> owner.position().add(0.0, 0.2, 0.0);
                default -> owner.position();
            };
            setPos(target);
        } else if (speed() != 0.0f) {''')
    once(effect,
'''    private boolean followsOwner() {
        return switch (kind()) {''',
'''    private boolean followsOwner() {
        if (kind().startsWith("elite_aura_")) return true;
        return switch (kind()) {''')

    mesh = JAVA / "VillageSkillMeshLibrary.java"
    once(mesh,
'''        Random random = new Random(state.seed);

        switch (state.kind) {''',
'''        Random random = new Random(state.seed);

        if (state.kind.startsWith("turret_wreck_")) {
            renderTurretWreck(pose, out, basis, age, state.kind, state.extra);
            return;
        }

        switch (state.kind) {''')
    once(mesh,
'''            case "siege_structure_impact" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 4);
            default -> renderFallbackRune(pose, out, basis, age, progress);''',
'''            case "siege_structure_impact" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 4);

            case "turret_body_ballista" -> renderTurretBody(pose, out, basis, age, state.extra, 0);
            case "turret_body_repeater" -> renderTurretBody(pose, out, basis, age, state.extra, 1);
            case "turret_body_piercer" -> renderTurretBody(pose, out, basis, age, state.extra, 2);
            case "turret_body_flame" -> renderTurretBody(pose, out, basis, age, state.extra, 3);
            case "turret_body_frost" -> renderTurretBody(pose, out, basis, age, state.extra, 4);
            case "turret_body_chain" -> renderTurretBody(pose, out, basis, age, state.extra, 5);
            case "turret_body_bombard" -> renderTurretBody(pose, out, basis, age, state.extra, 6);
            case "turret_body_nullifier" -> renderTurretBody(pose, out, basis, age, state.extra, 7);
            case "turret_body_anti_air" -> renderTurretBody(pose, out, basis, age, state.extra, 8);
            case "turret_body_beacon" -> renderTurretBody(pose, out, basis, age, state.extra, 9);

            case "elite_aura_grappler" -> renderEliteAura(pose, out, basis, age, 0);
            case "elite_aura_firebrand" -> renderEliteAura(pose, out, basis, age, 1);
            case "elite_aura_assassin" -> renderEliteAura(pose, out, basis, age, 2);
            case "elite_aura_plague_weaver" -> renderEliteAura(pose, out, basis, age, 3);
            case "elite_aura_shock_rider" -> renderEliteAura(pose, out, basis, age, 4);
            case "elite_grapple_line" -> renderPath(pose, out, state, age, progress, 0xD9C19A, false);
            case "elite_firebrand_throw" -> renderEliteThrow(pose, out, state, age, progress);
            case "elite_firebrand_impact" -> renderEliteZone(pose, out, basis, age, progress, state.extra, 0);
            case "elite_plague_warning" -> renderEliteZone(pose, out, basis, age, progress, state.extra, 1);
            case "elite_plague_impact" -> renderEliteZone(pose, out, basis, age, progress, state.extra, 2);
            default -> renderFallbackRune(pose, out, basis, age, progress);''')
    once(mesh,
'''    private static void renderFallbackRune(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {''',
'''    private static void renderTurretBody(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, String extra, int style) {
        TurretPresentation presentation = parseTurretPresentation(extra);
        double scale = 1.0 + (presentation.level() - 1) * 0.075;
        boolean disabled = presentation.disabled();
        int metal = disabled ? rgba(94, 91, 86, 210) : rgba(166, 174, 177, 230);
        int dark = rgba(64, 69, 72, 235);
        int bright = switch (style) {
            case 3 -> rgba(255, 101, 39, disabled ? 90 : 225);
            case 4 -> rgba(113, 218, 255, disabled ? 90 : 220);
            case 5 -> rgba(120, 202, 255, disabled ? 90 : 225);
            case 7 -> rgba(207, 127, 255, disabled ? 90 : 225);
            case 9 -> rgba(119, 244, 183, disabled ? 90 : 225);
            default -> rgba(255, 213, 126, disabled ? 90 : 215);
        };

        verticalPillarAt(pose, out, b, b.local(0, 0.72, 0), 0.34 * scale, 0.72 * scale, dark);
        ring(pose, out, b, 0.46 * scale, 1.40, 0.07, 40, metal, age * 0.002);

        switch (style) {
            case 0 -> {
                Vec3 railA = b.local(0, 1.48, -0.28);
                Vec3 railB = b.local(0, 1.48, 1.28 * scale);
                prism(pose, out, railA, railB, 0.105 * scale, metal);
                prism(pose, out, b.local(-1.00 * scale, 1.48, 0.34), b.local(1.00 * scale, 1.48, 0.34), 0.085, metal);
                prism(pose, out, b.local(-1.00 * scale, 1.48, 0.34), railB, 0.025, bright);
                prism(pose, out, b.local(1.00 * scale, 1.48, 0.34), railB, 0.025, bright);
            }
            case 1 -> {
                for (int i = -1; i <= 1; i++) {
                    Vec3 a = b.local(i * 0.24, 1.43 + Math.abs(i) * 0.08, 0.02);
                    Vec3 z = b.local(i * 0.24, 1.43 + Math.abs(i) * 0.08, 1.12 * scale);
                    prism(pose, out, a, z, 0.072, i == 0 ? bright : metal);
                }
                sphere(pose, out, b.local(0, 1.45, -0.08), 0.35 * scale, 8, 12, dark);
            }
            case 2 -> {
                prism(pose, out, b.local(0, 1.52, -0.34), b.local(0, 1.52, 1.62 * scale), 0.145 * scale, metal);
                ring(pose, out, b, 0.30 * scale, 1.52, 0.09, 32, bright, 0.0);
                prism(pose, out, b.local(-0.48, 1.32, 0.08), b.local(0.48, 1.32, 0.08), 0.09, dark);
            }
            case 3 -> {
                sphere(pose, out, b.local(-0.38, 1.26, -0.05), 0.30 * scale, 8, 12, dark);
                sphere(pose, out, b.local(0.38, 1.26, -0.05), 0.30 * scale, 8, 12, dark);
                prism(pose, out, b.local(0, 1.48, 0.02), b.local(0, 1.48, 1.18 * scale), 0.16, metal);
                sphere(pose, out, b.local(0, 1.48, 1.24 * scale), 0.19, 7, 10, bright);
            }
            case 4 -> {
                prism(pose, out, b.local(0, 1.43, -0.08), b.local(0, 1.43, 0.96 * scale), 0.11, metal);
                crystal(pose, out, b.local(0, 1.25, 0.84 * scale), 0.88 * scale, 0.28 * scale, bright);
                for (int side : new int[]{-1, 1}) {
                    crystal(pose, out, b.local(side * 0.42, 1.12, 0.08), 0.55, 0.16, withAlpha(bright, 180));
                }
            }
            case 5 -> {
                verticalPillarAt(pose, out, b, b.local(0, 1.34, 0), 0.17, 1.18 * scale, bright);
                sphere(pose, out, b.local(0, 2.48 * scale, 0), 0.24, 8, 12, bright);
                ring(pose, out, b, 0.64 * scale, 1.74, 0.055, 44, bright, age * 0.055);
                ring(pose, out, b, 0.46 * scale, 2.08, 0.045, 36, withAlpha(bright, 165), -age * 0.075);
            }
            case 6 -> {
                Vec3 a = b.local(0, 1.18, -0.26);
                Vec3 z = b.local(0, 2.22 * scale, 0.92 * scale);
                prism(pose, out, a, z, 0.22 * scale, metal);
                sphere(pose, out, a, 0.36 * scale, 8, 12, dark);
                ring(pose, out, Basis.from(z.subtract(a)), 0.27, 0.02, 0.08, 32, bright, 0.0);
            }
            case 7 -> {
                sphere(pose, out, b.local(0, 1.62, 0), 0.33 * scale, 9, 14, bright);
                ring(pose, out, b, 0.70 * scale, 1.62, 0.055, 48, bright, age * 0.045);
                ring(pose, out, b, 0.52 * scale, 1.62, 0.045, 40, withAlpha(bright, 160), -age * 0.07);
                prism(pose, out, b.local(0, 1.58, 0.18), b.local(0, 1.58, 0.96), 0.08, metal);
            }
            case 8 -> {
                for (int side : new int[]{-1, 1}) {
                    Vec3 a = b.local(side * 0.26, 1.24, -0.18);
                    Vec3 z = b.local(side * 0.26, 2.25 * scale, 1.18 * scale);
                    prism(pose, out, a, z, 0.095, side < 0 ? metal : bright);
                }
                prism(pose, out, b.local(-0.56, 1.24, -0.12), b.local(0.56, 1.24, -0.12), 0.09, dark);
            }
            case 9 -> {
                verticalPillarAt(pose, out, b, b.local(0, 1.28, 0), 0.20, 1.42 * scale, bright);
                crystal(pose, out, b.local(0, 2.42 * scale, 0), 0.62, 0.24, bright);
                for (int i = 0; i < 3; i++) {
                    ring(pose, out, b, (0.44 + i * 0.18) * scale, 1.78 + i * 0.34,
                            0.045, 42, withAlpha(bright, 185 - i * 25), age * (0.035 + i * 0.012));
                }
            }
        }

        if (presentation.level() >= 3) {
            ring(pose, out, b, 0.58 * scale, 1.06, 0.035, 40,
                    withAlpha(bright, 110 + presentation.level() * 15), -age * 0.025);
        }
        if (presentation.disabled()) {
            ring(pose, out, b, 0.66 * scale, 1.46, 0.06, 44,
                    rgba(255, 67, 78, (int) (120 + 70 * (0.5 + 0.5 * Math.sin(age * 0.34)))), age * 0.08);
        }
    }

    private static void renderTurretWreck(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, String kind, String extra) {
        TurretPresentation presentation = parseTurretPresentation(extra);
        double scale = 0.92 + presentation.level() * 0.045;
        int dark = rgba(61, 58, 55, 225);
        int ember = kind.contains("flame") || kind.contains("bombard")
                ? rgba(255, 92, 43, 110) : rgba(137, 151, 158, 100);
        prism(pose, out, b.local(-0.46 * scale, 0.25, -0.34), b.local(0.52 * scale, 0.88, 0.26), 0.13, dark);
        prism(pose, out, b.local(0.44 * scale, 0.22, -0.25), b.local(-0.34 * scale, 0.66, 0.62), 0.10, dark);
        prism(pose, out, b.local(-0.18, 0.18, 0.52), b.local(0.65, 0.42, 0.86), 0.075, dark);
        sphere(pose, out, b.local(0.04, 0.55, 0.04), 0.20, 7, 10, ember);
    }

    private static TurretPresentation parseTurretPresentation(String extra) {
        int level = 1;
        boolean disabled = false;
        if (extra != null && !extra.isBlank()) {
            String[] parts = extra.split("\\|", -1);
            try { level = Math.max(1, Math.min(5, Integer.parseInt(parts[0]))); }
            catch (NumberFormatException ignored) {}
            disabled = parts.length > 1 && "1".equals(parts[1]);
        }
        return new TurretPresentation(level, disabled);
    }

    private record TurretPresentation(int level, boolean disabled) {}

    private static void renderEliteAura(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, int style) {
        int color = switch (style) {
            case 0 -> rgba(220, 196, 145, 165);
            case 1 -> rgba(255, 99, 43, 175);
            case 2 -> rgba(155, 128, 214, 145);
            case 3 -> rgba(122, 218, 104, 160);
            default -> rgba(105, 196, 255, 175);
        };
        double pulse = 0.92 + 0.08 * Math.sin(age * 0.16);
        ring(pose, out, b, 0.72 * pulse, 0.06, 0.045, 42, color, age * 0.025);
        if (style == 0) {
            for (int side : new int[]{-1, 1}) {
                spike(pose, out, b.local(side * 0.54, 0.74, 0.0), b.local(side * 0.86, 1.12, 0.25), 0.055, color);
            }
        } else if (style == 1) {
            for (int i = 0; i < 3; i++) {
                double a = age * 0.05 + i * TAU / 3.0;
                sphere(pose, out, b.local(Math.cos(a) * 0.48, 1.05 + i * 0.16, Math.sin(a) * 0.48),
                        0.10, 6, 8, color);
            }
        } else if (style == 2) {
            for (int i = 0; i < 3; i++) {
                slashArc(pose, out, b, age * 0.045 + i * TAU / 3.0,
                        0.78 + i * 0.12, 0.82 + i * 0.18, 0.66, 0.045, color);
            }
        } else if (style == 3) {
            helixRibbon(pose, out, b, age * 0.035, 0.46, 1.55, 18, withAlpha(color, 95));
            ring(pose, out, b, 0.92 * pulse, 0.08, 0.055, 46, color, -age * 0.018);
        } else {
            jaggedBolt(pose, out, b.local(-0.42, 0.20, 0), b.local(0.42, 1.65, 0.12),
                    7, 0.035, color, (long) age / 3L + 17L);
        }
    }

    private static void renderEliteThrow(
            PoseStack.Pose pose, VertexConsumer out, VillageSkillEffectRenderState state,
            double age, double progress) {
        Vec3 origin = new Vec3(state.x, state.y, state.z);
        List<Vec3> points = parsePoints(state.extra, origin);
        if (points.size() < 2) return;
        Vec3 a = points.get(0).subtract(origin);
        Vec3 z = points.get(1).subtract(origin);
        double horizontal = Math.hypot(z.x - a.x, z.z - a.z);
        Vec3 control = a.lerp(z, 0.5).add(0.0, Math.max(2.0, horizontal * 0.16), 0.0);
        Vec3 previous = a;
        for (int i = 1; i <= 14; i++) {
            double t = i / 14.0;
            Vec3 current = bezier(a, control, z, t);
            prism(pose, out, previous, current, 0.035,
                    rgba(255, 83, 34, (int) (90 * (1.0 - progress * 0.5))));
            previous = current;
        }
        Vec3 projectile = bezier(a, control, z, clamp(progress * 1.04, 0.0, 1.0));
        sphere(pose, out, projectile, 0.19, 7, 10, rgba(255, 102, 39, 230));
        sphere(pose, out, projectile, 0.08, 6, 8, rgba(255, 231, 143, 245));
    }

    private static void renderEliteZone(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress,
            String encodedRadius, int style) {
        double radius = 4.0;
        try { radius = Double.parseDouble(encodedRadius); }
        catch (NumberFormatException ignored) {}
        radius = Math.max(1.0, radius);
        int color = style == 0
                ? rgba(255, 86, 38, (int) (205 * (1.0 - progress * 0.75)))
                : rgba(110, 214, 91, (int) ((style == 1 ? 155 : 210) * (1.0 - progress * 0.72)));
        double visibleRadius = style == 1 ? radius : 0.45 + radius * Math.min(1.0, progress * 2.2);
        ring(pose, out, b, visibleRadius, 0.045, style == 1 ? 0.11 : 0.18, 72, color, age * 0.012);
        if (style == 1) {
            ring(pose, out, b, radius * 0.72, 0.05, 0.045, 56, withAlpha(color, 100), -age * 0.018);
            for (int i = 0; i < 8; i++) {
                double a = i * TAU / 8.0 + age * 0.006;
                chevron(pose, out, b, a, radius * 0.88, 0.06, 0.44, withAlpha(color, 120));
            }
        } else if (style == 2) {
            sphere(pose, out, Vec3.ZERO, Math.max(0.3, visibleRadius * 0.22), 8, 12, withAlpha(color, 80));
        }
    }

    private static void renderFallbackRune(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {''')

    print("[PASS] applied v0.18.14 persistent presentation patch")


if __name__ == "__main__":
    main()
