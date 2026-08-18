#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected one match in {path.name}, got {count}: {old[:180]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def main() -> None:
    props = ROOT / "gradle.properties"
    replace_once(props, "mod_version=0.18.15-alpha.1", "mod_version=0.18.16-alpha.1")
    for test in (ROOT / "tools").glob("test_*.py"):
        text = test.read_text(encoding="utf-8")
        if "mod_version=0.18.15-alpha.1" in text:
            test.write_text(text.replace("mod_version=0.18.15-alpha.1", "mod_version=0.18.16-alpha.1"), encoding="utf-8")

    raid = JAVA / "VillageRaidSystem.java"
    replace_once(raid,
'''    public static VillageWaveTrait currentTrait() { return currentTrait; }
    public static boolean isActive() { return active; }
    public static boolean isRaidLocked() { return active || countdownTicks > 0; }
''',
'''    public static VillageWaveTrait currentTrait() { return currentTrait; }
    public static boolean isActive() { return active; }
    public static boolean isRaidLocked() { return active || countdownTicks > 0; }

    /** Compact authoritative snapshot for the defense HUD; never reparses formatted status text. */
    public static RaidHudSnapshot hudSnapshot() {
        int north = 0;
        int west = 0;
        int east = 0;
        int rear = 0;
        for (UUID id : ACTIVE_ENEMIES) {
            switch (VillageAttackPlanSystem.frontOf(id)) {
                case NORTH -> north++;
                case NORTH_WEST, WEST -> west++;
                case NORTH_EAST, EAST -> east++;
                case SOUTH_WEST, SOUTH_EAST -> rear++;
            }
        }
        String mode = VillageProgressionSystem.isGameOver() ? "GAME_OVER"
                : countdownTicks > 0 ? "COUNTDOWN"
                : active ? "ACTIVE" : "SAFE";
        int nextSeconds = 0;
        if (countdownTicks > 0) {
            nextSeconds = Math.max(1, (countdownTicks + 19) / 20);
        } else if (active && wave < maxWaves) {
            int ticks = ACTIVE_ENEMIES.isEmpty() && betweenWaveTicks > 0
                    ? betweenWaveTicks
                    : Math.max(0, FORCED_NEXT_WAVE_TICKS - waveElapsedTicks);
            nextSeconds = Math.max(1, (ticks + 19) / 20);
        }
        return new RaidHudSnapshot(active, mode, wave, maxWaves, ACTIVE_ENEMIES.size(), nextSeconds,
                currentTrait.displayName(), north, west, east, rear);
    }

    public record RaidHudSnapshot(
            boolean active,
            String mode,
            int wave,
            int maxWaves,
            int enemyCount,
            int nextSeconds,
            String trait,
            int north,
            int west,
            int east,
            int rear) {}
''')

    command = JAVA / "VillageCommandCenterScreen.java"
    for old, new in (
        ("private static final int OVERLAY = 0x70070A0D;", "private static final int OVERLAY = VillageDefenseUiTheme.BACKDROP;"),
        ("private static final int TEXT = 0xFFF1F4F5;", "private static final int TEXT = VillageDefenseUiTheme.TEXT;"),
        ("private static final int MUTED = 0xFFAAB5BA;", "private static final int MUTED = VillageDefenseUiTheme.MUTED;"),
        ("private static final int CYAN = 0xFF52D9C2;", "private static final int CYAN = VillageDefenseUiTheme.CYAN;"),
        ("private static final int GOLD = 0xFFFFC65C;", "private static final int GOLD = VillageDefenseUiTheme.GOLD;"),
        ("private static final int RED = 0xFFE06E64;", "private static final int RED = VillageDefenseUiTheme.RED;"),
        ("private static final int SURFACE = 0xD1131B1F;", "private static final int SURFACE = VillageDefenseUiTheme.PANEL_SOFT;"),
        ("private static final int SURFACE_2 = 0xD51A252A;", "private static final int SURFACE_2 = VillageDefenseUiTheme.PANEL_ACTIVE;"),
        ("private static final int LINE = 0xA34B6873;", "private static final int LINE = VillageDefenseUiTheme.EDGE;"),
    ):
        replace_once(command, old, new)

    town = JAVA / "VillageTownHallGridScreen.java"
    for old, new in (
        ("private static final int OVERLAY = 0x7805090C;", "private static final int OVERLAY = VillageDefenseUiTheme.BACKDROP;"),
        ("private static final int PANEL = 0xF00B1217;", "private static final int PANEL = VillageDefenseUiTheme.PANEL;"),
        ("private static final int PANEL_2 = 0xE9142027;", "private static final int PANEL_2 = VillageDefenseUiTheme.PANEL_SOFT;"),
        ("private static final int PANEL_3 = 0xE91B2A32;", "private static final int PANEL_3 = VillageDefenseUiTheme.PANEL_ACTIVE;"),
        ("private static final int LINE = 0xB04F6873;", "private static final int LINE = VillageDefenseUiTheme.EDGE;"),
        ("private static final int TEXT = 0xFFF3F5F5;", "private static final int TEXT = VillageDefenseUiTheme.TEXT;"),
        ("private static final int MUTED = 0xFFA8B4B9;", "private static final int MUTED = VillageDefenseUiTheme.MUTED;"),
        ("private static final int CYAN = 0xFF50D9C1;", "private static final int CYAN = VillageDefenseUiTheme.CYAN;"),
        ("private static final int GOLD = 0xFFF2C35D;", "private static final int GOLD = VillageDefenseUiTheme.GOLD;"),
        ("private static final int RED = 0xFFE56A64;", "private static final int RED = VillageDefenseUiTheme.RED;"),
        ("private static final int GREEN = 0xFF76D39A;", "private static final int GREEN = VillageDefenseUiTheme.GREEN;"),
        ("private static final int BLUE = 0xFF7AA9E8;", "private static final int BLUE = VillageDefenseUiTheme.BLUE;"),
    ):
        replace_once(town, old, new)

    effects = JAVA / "VillageDefenseEffectSystem.java"
    replace_once(effects,
'''    private static void spawnLine(ServerLevel level, String kind, Vec3 start, Vec3 end, int duration) {''',
'''    public static void turretPlacementPreview(
            ServerLevel level, Vec3 center, VillagePlacedTurretSystem.TurretType type) {
        if (level == null || center == null || type == null) return;
        VillageSkillEffectEntity.spawn(level, null, "turret_placement_preview", center,
                new Vec3(0.0, 0.0, 1.0), 18, 0.0f, Integer.toString(type.ordinal()));
    }

    public static void turretDeployPulse(
            ServerLevel level, Vec3 center, VillagePlacedTurretSystem.TurretType type) {
        if (level == null || center == null || type == null) return;
        VillageSkillEffectEntity.spawn(level, null, "turret_deploy_pulse", center,
                new Vec3(0.0, 0.0, 1.0), 26, 0.0f, Integer.toString(type.ordinal()));
    }

    public static void turretRepairPulse(ServerLevel level, Vec3 center) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "defense_repair_pulse", center,
                new Vec3(0.0, 0.0, 1.0), 24, 0.0f, "");
    }

    public static void turretUpgradePulse(ServerLevel level, Vec3 center, int levelValue) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "turret_upgrade_burst", center,
                new Vec3(0.0, 0.0, 1.0), 30, 0.0f, Integer.toString(Math.max(1, levelValue)));
    }

    public static void breachAlarm(ServerLevel level, Vec3 center) {
        if (level == null || center == null) return;
        VillageSkillEffectEntity.spawn(level, null, "defense_breach_alarm", center,
                new Vec3(0.0, 0.0, 1.0), 34, 0.0f, "5.0");
    }

    private static void spawnLine(ServerLevel level, String kind, Vec3 start, Vec3 end, int duration) {''')

    turret = JAVA / "VillagePlacedTurretSystem.java"
    replace_once(turret,
'''            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, candidate.getX() + 0.5, candidate.getY() + 0.7,
                    candidate.getZ() + 0.5, 12, 0.45, 0.35, 0.45, 0.03);''',
'''            VillageDefenseEffectSystem.turretPlacementPreview(level,
                    Vec3.atCenterOf(candidate).add(0.0, -0.45, 0.0), pending.type());''')
    replace_once(turret,
'''        buildVisual(level, state);
        PENDING.remove(player.getUUID());''',
'''        buildVisual(level, state);
        VillageDefenseEffectSystem.turretDeployPulse(level,
                Vec3.atCenterOf(state.pos()).add(0.0, -0.45, 0.0), state.type());
        PENDING.remove(player.getUUID());''')
    replace_once(turret,
'''        if (player.level() instanceof ServerLevel level) buildVisual(level, repaired);
        return state.type().displayName() + " #" + id + " 수리 완료 · HP " + maximum + "/" + maximum;''',
'''        if (player.level() instanceof ServerLevel level) {
            buildVisual(level, repaired);
            VillageDefenseEffectSystem.turretRepairPulse(level,
                    Vec3.atCenterOf(repaired.pos()).add(0.0, -0.35, 0.0));
        }
        return state.type().displayName() + " #" + id + " 수리 완료 · HP " + maximum + "/" + maximum;''')
    replace_once(turret,
'''        if (player.level() instanceof ServerLevel level) buildVisual(level, upgraded);
        return state.type().displayName() + " #" + id + " Lv." + newLevel + " 강화 완료";''',
'''        if (player.level() instanceof ServerLevel level) {
            buildVisual(level, upgraded);
            VillageDefenseEffectSystem.turretUpgradePulse(level,
                    Vec3.atCenterOf(upgraded.pos()).add(0.0, -0.35, 0.0), newLevel);
        }
        return state.type().displayName() + " #" + id + " Lv." + newLevel + " 강화 완료";''')
    replace_once(turret,
'''            if (player.level() instanceof ServerLevel level) buildVisual(level, fixed);
            totalCost += cost; repaired++;''',
'''            if (player.level() instanceof ServerLevel level) {
                buildVisual(level, fixed);
                VillageDefenseEffectSystem.turretRepairPulse(level,
                        Vec3.atCenterOf(fixed.pos()).add(0.0, -0.35, 0.0));
            }
            totalCost += cost; repaired++;''')

    segment = JAVA / "VillageSiegeSegmentSystem.java"
    replace_once(segment,
'''import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;''',
'''import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;''')
    replace_once(segment,
'''        if (after == 0) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(''',
'''        if (after == 0) {
            VillageDefenseEffectSystem.breachAlarm(server.overworld(),
                    Vec3.atCenterOf(attackPoint(segment, impact)));
            server.getPlayerList().broadcastSystemMessage(Component.literal(''')

    mesh = JAVA / "VillageSkillMeshLibrary.java"
    replace_once(mesh,
'''            case "siege_structure_impact" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 4);

            case "turret_body_ballista"''',
'''            case "siege_structure_impact" -> renderDefensePulse(pose, out, basis, age, progress, state.extra, 4);
            case "turret_placement_preview" -> renderDefenseMaintenance(pose, out, basis, age, progress, 0);
            case "turret_deploy_pulse" -> renderDefenseMaintenance(pose, out, basis, age, progress, 1);
            case "defense_repair_pulse" -> renderDefenseMaintenance(pose, out, basis, age, progress, 2);
            case "turret_upgrade_burst" -> renderDefenseMaintenance(pose, out, basis, age, progress, 3);
            case "defense_breach_alarm" -> renderDefenseMaintenance(pose, out, basis, age, progress, 4);

            case "turret_body_ballista"''')
    replace_once(mesh,
'''    private static void renderVanguardSpin(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {''',
'''    private static void renderDefenseMaintenance(
            PoseStack.Pose pose, VertexConsumer out, Basis b,
            double age, double progress, int mode) {
        double fade = Math.max(0.0, 1.0 - progress);
        int primary = switch (mode) {
            case 0 -> rgba(82, 222, 197, (int) (165 * fade));
            case 1 -> rgba(244, 197, 95, (int) (210 * fade));
            case 2 -> rgba(112, 214, 155, (int) (195 * fade));
            case 3 -> rgba(255, 213, 112, (int) (220 * fade));
            default -> rgba(232, 72, 72, (int) (225 * fade));
        };
        double radius = mode == 4 ? 2.2 + progress * 3.2 : 0.78 + progress * 1.35;
        ring(pose, out, b, radius, 0.035, mode == 4 ? 0.12 : 0.075,
                mode == 4 ? 64 : 48, primary, age * (mode == 4 ? 0.035 : 0.055));
        ring(pose, out, b, Math.max(0.45, radius * 0.68), 0.055, 0.045,
                40, withAlpha(primary, Math.max(20, (int) (135 * fade))), -age * 0.04);
        if (mode == 0) {
            for (int i = 0; i < 4; i++) {
                double a = i * TAU / 4.0 + age * 0.025;
                chevron(pose, out, b, a, 0.95, 0.12, 0.25, primary);
            }
        } else if (mode == 1) {
            verticalPillar(pose, out, b, 0.34 + progress * 0.18, 2.2 * fade + 0.3, primary);
        } else if (mode == 2) {
            for (int i = 0; i < 3; i++) {
                ring(pose, out, b, 0.72 + i * 0.24, 0.28 + i * 0.34 + progress * 0.55,
                        0.045, 36, withAlpha(primary, Math.max(20, (int) ((170 - i * 22) * fade))), age * 0.02);
            }
        } else if (mode == 3) {
            for (int i = 0; i < 6; i++) {
                double a = i * TAU / 6.0;
                chevron(pose, out, b, a, 0.82 + progress * 0.45,
                        0.45 + progress * 1.25, 0.30, primary);
            }
            verticalPillar(pose, out, b, 0.22, 2.8 * fade + 0.35, primary);
        } else {
            for (int i = 0; i < 8; i++) {
                double a = i * TAU / 8.0 + age * 0.015;
                groundCrack(pose, out, b, a, 0.65, 2.4 + progress * 1.8, 0.08, primary);
            }
        }
    }

    private static void renderVanguardSpin(
            PoseStack.Pose pose, VertexConsumer out, Basis b, double age, double progress) {''')

    print("[PASS] applied v0.18.16 mobile defense HUD and presentation integration")


if __name__ == "__main__":
    main()
