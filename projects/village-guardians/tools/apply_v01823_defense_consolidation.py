#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/villageguardians"


def load(name: str) -> str:
    return (JAVA / name).read_text(encoding="utf-8")


def save(name: str, text: str) -> None:
    (JAVA / name).write_text(text, encoding="utf-8")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if text.count(old) != 1:
        raise RuntimeError(f"{label}: expected exactly one match, got {text.count(old)}")
    return text.replace(old, new, 1)


props_path = ROOT / "gradle.properties"
props = props_path.read_text(encoding="utf-8")
props = replace_once(props, "mod_version=0.18.22-alpha.1", "mod_version=0.18.23-alpha.1", "version")
props_path.write_text(props, encoding="utf-8")

guardians = load("VillageGuardians.java")
guardians = replace_once(
    guardians,
    "        VillageTowerSpecializationSystem.initializeServer(event.getServer());\n",
    "",
    "retired tower initialization",
)
save("VillageGuardians.java", guardians)

commands = load("VillageCommands.java")
commands = replace_once(
    commands,
    "        VillageUiService.openDashboard(source.getPlayerOrException());",
    "        VillageUiController.openDashboard(source.getPlayerOrException());",
    "menu controller route",
)
commands = replace_once(
    commands,
    "        VillageUiService.openPlayerStatus(source.getPlayerOrException());",
    "        VillageUiController.openStatus(source.getPlayerOrException());",
    "status controller route",
)
save("VillageCommands.java", commands)

save("VillageDefenseSystem.java", '''package kr.moonseungjun.villageguardians;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * Compatibility/status facade for the defense layer.
 *
 * Production combat is owned exclusively by VillagePlacedTurretSystem. Fixed corner towers are
 * fortress architecture only; the retired global four-tower specialization state is not read.
 */
public final class VillageDefenseSystem {
    private VillageDefenseSystem() {}

    public static void reset() {
        // All production defense runtime state is reset by its owning systems.
    }

    public static boolean recognizeDefenseMob(Mob mob) {
        return VillageMercenarySystem.adoptLegacy(mob);
    }

    /** Compatibility facade: production hiring is owned by VillageMercenarySystem. */
    public static int mercenaryHireCost() {
        return VillageMercenarySystem.hireCost(VillageMercenarySystem.MercenaryClass.BASTION);
    }

    public static String hireMercenary(ServerPlayer player) {
        return VillageMercenarySystem.hire(player, VillageMercenarySystem.MercenaryClass.BASTION);
    }

    public static String status(ServerLevel level) {
        return "배치 포탑 " + VillagePlacedTurretSystem.activeCount() + "/"
                + VillagePlacedTurretSystem.count() + "기 가동 · 설치 "
                + VillagePlacedTurretSystem.count() + "/" + VillagePlacedTurretSystem.capacity()
                + " | " + VillageMercenarySystem.status(level.getServer());
    }
}
''')

save("VillageDefenseTowerBuilder.java", '''package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Fixed corner towers are fortress architecture/observation landmarks, not combat emplacements. */
final class VillageDefenseTowerBuilder {
    private VillageDefenseTowerBuilder() {}

    static void build(ServerLevel level, BlockPos center) {
        build(level, center, VillageProgressionSystem.wallLevel());
    }

    static void build(ServerLevel level, BlockPos center, int installedStage) {
        int radius = VillageWorldSystem.FORTRESS_RADIUS - 4;
        BlockPos ballista = center.offset(radius, 13, -radius);
        BlockPos flame = center.offset(-radius, 13, -radius);
        BlockPos frost = center.offset(radius, 13, radius);
        BlockPos arcane = center.offset(-radius, 13, radius);

        clearInstallationPad(level, ballista);
        clearInstallationPad(level, flame);
        clearInstallationPad(level, frost);
        clearInstallationPad(level, arcane);

        if (installedStage >= 1) buildBallista(level, ballista);
        if (installedStage >= 2) buildFlame(level, flame);
        if (installedStage >= 3) buildFrost(level, frost);
        if (installedStage >= 4) buildArcane(level, arcane);
    }

    private static void clearInstallationPad(ServerLevel level, BlockPos base) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                set(level, base.offset(x, 0, z), Blocks.STONE_BRICKS);
                for (int y = 1; y <= 8; y++) set(level, base.offset(x, y, z), Blocks.AIR);
            }
        }
        for (int x = -3; x <= 3; x++) {
            set(level, base.offset(x, 1, -3), Blocks.STONE_BRICK_WALL);
            set(level, base.offset(x, 1, 3), Blocks.STONE_BRICK_WALL);
        }
        for (int z = -2; z <= 2; z++) {
            set(level, base.offset(-3, 1, z), Blocks.STONE_BRICK_WALL);
            set(level, base.offset(3, 1, z), Blocks.STONE_BRICK_WALL);
        }
    }

    private static void buildBallista(ServerLevel level, BlockPos base) {
        platform(level, base, Blocks.DARK_OAK_PLANKS);
        column(level, base, 0, 0, 1, 3, Blocks.STRIPPED_DARK_OAK_WOOD);
        lineX(level, base, -3, 3, 4, 0, Blocks.DARK_OAK_FENCE);
        lineZ(level, base, -2, 2, 4, 0, Blocks.IRON_BARS);
        set(level, base.offset(0, 5, -2), Blocks.END_ROD);
    }

    private static void buildFlame(ServerLevel level, BlockPos base) {
        platform(level, base, Blocks.BRICKS);
        column(level, base, 0, 0, 1, 4, Blocks.POLISHED_BLACKSTONE);
        set(level, base.offset(0, 5, 0), Blocks.CAMPFIRE);
        set(level, base.offset(1, 4, 0), Blocks.IRON_BARS);
        set(level, base.offset(-1, 4, 0), Blocks.IRON_BARS);
    }

    private static void buildFrost(ServerLevel level, BlockPos base) {
        platform(level, base, Blocks.PACKED_ICE);
        column(level, base, 0, 0, 1, 3, Blocks.BLUE_ICE);
        set(level, base.offset(0, 4, 0), Blocks.SEA_LANTERN);
        set(level, base.offset(1, 3, 0), Blocks.AMETHYST_CLUSTER);
        set(level, base.offset(-1, 3, 0), Blocks.AMETHYST_CLUSTER);
    }

    private static void buildArcane(ServerLevel level, BlockPos base) {
        platform(level, base, Blocks.POLISHED_DEEPSLATE);
        column(level, base, 0, 0, 1, 3, Blocks.AMETHYST_BLOCK);
        set(level, base.offset(0, 4, 0), Blocks.END_ROD);
        set(level, base.offset(2, 2, 0), Blocks.CRYING_OBSIDIAN);
        set(level, base.offset(-2, 2, 0), Blocks.CRYING_OBSIDIAN);
        set(level, base.offset(0, 2, 2), Blocks.CRYING_OBSIDIAN);
        set(level, base.offset(0, 2, -2), Blocks.CRYING_OBSIDIAN);
    }

    private static void platform(ServerLevel level, BlockPos base, Block material) {
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) set(level, base.offset(x, 0, z), material);
        }
        for (int x = -3; x <= 3; x++) {
            set(level, base.offset(x, 1, -3), Blocks.STONE_BRICK_WALL);
            set(level, base.offset(x, 1, 3), Blocks.STONE_BRICK_WALL);
        }
        for (int z = -2; z <= 2; z++) {
            set(level, base.offset(-3, 1, z), Blocks.STONE_BRICK_WALL);
            set(level, base.offset(3, 1, z), Blocks.STONE_BRICK_WALL);
        }
    }

    private static void column(ServerLevel level, BlockPos base, int x, int z, int fromY, int toY, Block block) {
        for (int y = fromY; y <= toY; y++) set(level, base.offset(x, y, z), block);
    }

    private static void lineX(ServerLevel level, BlockPos base, int from, int to, int y, int z, Block block) {
        for (int x = from; x <= to; x++) set(level, base.offset(x, y, z), block);
    }

    private static void lineZ(ServerLevel level, BlockPos base, int from, int to, int y, int x, Block block) {
        for (int z = from; z <= to; z++) set(level, base.offset(x, y, z), block);
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlockAndUpdate(pos, block.defaultBlockState());
    }
}
''')

service = load("VillageUiService.java")
service, count = re.subn(
    r"    public static void openTowerControl\(ServerPlayer player\) \{.*?\n    public static void openFunding\(ServerPlayer player\) \{",
    '''    public static void openTowerControl(ServerPlayer player) {
        if (!requireTownHall(player, "방어 지휘는 마을 회관 지휘대 근처에서만 가능합니다.")) return;
        VillageSiegeCommandUi.open(player);
    }

    public static void openFunding(ServerPlayer player) {''',
    service,
    count=1,
    flags=re.S,
)
if count != 1:
    raise RuntimeError(f"legacy tower UI block: expected one match, got {count}")

service, count = re.subn(
    r"        if \(action\.startsWith\(\"tower_open:\"\)\) \{.*?\n        if \(action\.startsWith\(\"use_skill:\"\)\) \{",
    '''        if (action.startsWith("tower_open:") || action.startsWith("tower_branch:")
                || action.startsWith("tower_upgrade:")) {
            // Stale client actions are compatibility redirects only; retired fixed-tower progression cannot mutate state.
            VillageSiegeCommandUi.open(player);
            return;
        }
        if (action.startsWith("use_skill:")) {''',
    service,
    count=1,
    flags=re.S,
)
if count != 1:
    raise RuntimeError(f"legacy tower action block: expected one match, got {count}")

service, count = re.subn(
    r"\n    private static void rebuildTowerVisual\(MinecraftServer server\) \{.*?\n    \}\n",
    "\n",
    service,
    count=1,
    flags=re.S,
)
if count != 1:
    raise RuntimeError(f"legacy tower visual rebuild helper: expected one match, got {count}")

save("VillageUiService.java", service)

for retired in ("VillageTowerSpecializationSystem.java", "VillageTowerProgressData.java"):
    path = JAVA / retired
    if not path.exists():
        raise RuntimeError(f"expected retired source before deletion: {retired}")
    path.unlink()

print("[PASS] staged v0.18.23 defense consolidation")
