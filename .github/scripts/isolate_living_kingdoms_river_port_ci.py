from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]
FIRE = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenFireResponseManager.java"
PORT = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenRiverPortManager.java"
JUSTICE = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/crime/ErdenJusticeManager.java"
CRIME = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/crime/CrimeManager.java"
MAIN = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/LivingKingdoms.java"
STATUS = ROOT / "projects/living-kingdoms/docs/ERDEN_IMPLEMENTATION_STATUS.md"


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


port = PORT.read_text(encoding="utf-8")
require("LIVING_KINGDOMS_CI_RIVER_PORT_TEST" in port, "river-port CI fixture is not isolated")
require("LIVING_KINGDOMS_CI_REALM_TEST" not in port, "river-port still uses generic realm fixture")
require("ci_corridor_retained_until_pass=true" in port, "river-port ticket lifecycle proof missing")

justice = JUSTICE.read_text(encoding="utf-8")
require("event_time_witness=true" in justice, "event-time witness proof missing")
require("retroactive_witness=false" in justice, "retroactive witness rejection missing")
require("synthetic_guard=false" in justice, "resident-guard proof missing")
require("ErdenJusticeManager.observeCrime(" in CRIME.read_text(encoding="utf-8"), "Erden crime routing missing")
require("ErdenJusticeManager.onServerTick(event);" in MAIN.read_text(encoding="utf-8"), "justice tick wiring missing")

fire = FIRE.read_text(encoding="utf-8")
require("LIVING_KINGDOMS_CI_FIRE_RESPONSE_TEST" in fire, "fire CI fixture is not isolated")
require("LIVING_KINGDOMS_CI_REALM_TEST" not in fire, "fire still uses generic realm fixture")

new_search = '''    private static BlockPos findCiFireSupport(
            ServerLevel level,
            ErdenUrbanInfrastructureBuilder.FireCistern cistern) {
        int chunkMinX = (cistern.x() >> 4) << 4;
        int chunkMinZ = (cistern.z() >> 4) << 4;
        BlockPos best = null;
        long bestDistance = Long.MAX_VALUE;
        int examined = 0;
        for (int x = chunkMinX; x <= chunkMinX + 15; x++) {
            for (int z = chunkMinZ; z <= chunkMinZ + 15; z++) {
                long dx = (long) x - cistern.x();
                long dz = (long) z - cistern.z();
                long distance = dx * dx + dz * dz;
                if (distance < 25L || distance > 196L) continue;
                int preferredY = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z)) + 1;
                for (int vertical = 0; vertical <= 8; vertical++) {
                    int[] ys = vertical == 0
                            ? new int[]{preferredY}
                            : new int[]{preferredY + vertical, preferredY - vertical};
                    for (int y : ys) {
                        if (y <= level.getMinY() || y >= level.getMaxY() - 1) continue;
                        examined++;
                        BlockPos support = new BlockPos(x, y, z);
                        BlockState below = level.getBlockState(support.below());
                        if (below.isAir() || !below.getFluidState().isEmpty()) continue;
                        if (!level.getBlockState(support).isAir()
                                || !level.getBlockState(support.above()).isAir()) continue;
                        if (distance < bestDistance) {
                            best = support;
                            bestDistance = distance;
                        }
                    }
                }
            }
        }
        if (best != null) {
            LivingKingdoms.LOGGER.info(
                    "Selected bounded Erden fire CI support={} examined={} same_chunk=true two_block_air=true stable_ground=true",
                    best, examined);
        }
        return best;
    }
'''
if "Selected bounded Erden fire CI support=" not in fire:
    fire, count = re.subn(
        r'    private static BlockPos findCiFireSupport\(.*?\n    \}\n\n    private static BlockPos safeStandingPosition',
        lambda _: new_search + '\n    private static BlockPos safeStandingPosition',
        fire,
        count=1,
        flags=re.S,
    )
    require(count == 1, "could not replace fragile fire CI support search")
if "import net.minecraft.world.level.block.state.BlockState;" not in fire:
    fire = fire.replace(
        "import net.minecraft.world.level.block.Blocks;\n",
        "import net.minecraft.world.level.block.Blocks;\nimport net.minecraft.world.level.block.state.BlockState;\n",
    )
require("Selected bounded Erden fire CI support=" in fire, "bounded fire support marker missing")
require("distance < 25L || distance > 196L" in fire, "bounded support radius missing")
FIRE.write_text(fire, encoding="utf-8")

status = STATUS.read_text(encoding="utf-8")
require("범행 순간 실제 로드된 주민만 목격자로 확정" in status, "Erden status lost justice implementation")

print("Widened the isolated fire-response CI support search without changing production fire behavior.")
