from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PATH = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenRiverPortManager.java"
text = PATH.read_text(encoding="utf-8")


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


if "import net.minecraft.world.entity.MoverType;" not in text:
    text = text.replace(
        "import net.minecraft.world.entity.EntityType;\n",
        "import net.minecraft.world.entity.EntityType;\nimport net.minecraft.world.entity.MoverType;\n",
        1,
    )
if "import net.minecraft.world.phys.Vec3;" not in text:
    text = text.replace(
        "import net.minecraft.world.level.block.Blocks;\n",
        "import net.minecraft.world.level.block.Blocks;\nimport net.minecraft.world.phys.Vec3;\n",
        1,
    )

old_state = '''    private static double ciTravelled;
    private static double ciLastBoatX = Double.NaN;
    private static double ciLastBoatZ = Double.NaN;'''
new_state = '''    private static double ciTravelled;
    private static double ciLastBoatX = Double.NaN;
    private static double ciLastBoatZ = Double.NaN;
    private static long ciLastProgressTick;
    private static int ciLastWaypoint = -1;'''
if "ciLastProgressTick" not in text:
    require(old_state in text, "river-port CI state anchor not found")
    text = text.replace(old_state, new_state, 1)

old_reset = '''        ciTravelled = 0.0D;
        ciLastBoatX = Double.NaN;
        ciLastBoatZ = Double.NaN;'''
new_reset = '''        ciTravelled = 0.0D;
        ciLastBoatX = Double.NaN;
        ciLastBoatZ = Double.NaN;
        ciLastProgressTick = 0L;
        ciLastWaypoint = -1;'''
if "ciLastWaypoint = -1;" not in text[text.index("private static void reset"):]:
    require(old_reset in text, "river-port reset anchor not found")
    text = text.replace(old_reset, new_reset, 1)

old_spawn = '''            LivingKingdoms.LOGGER.info(
                    "Materialized Erden supply barge shipment={} resource={} amount={} real_entity=true escrow_linked=true loaded_only={} route_points={}",
                    shipment.id(), shipment.resource(), shipment.amount(), !isPortCi(), route.size());'''
new_spawn = '''            if (isPortCi()) {
                ciLastProgressTick = level.getGameTime();
                ciLastWaypoint = port.waypoint();
            }
            LivingKingdoms.LOGGER.info(
                    "Materialized Erden supply barge shipment={} resource={} amount={} real_entity=true escrow_linked=true loaded_only={} route_points={}",
                    shipment.id(), shipment.resource(), shipment.amount(), !isPortCi(), route.size());'''
if "ciLastProgressTick = level.getGameTime();" not in text:
    require(old_spawn in text, "river-port spawn log anchor not found")
    text = text.replace(old_spawn, new_spawn, 1)

old_waypoint = '''        if (distance <= 4.0D) {
            int next = index + 1;
            port.setWaypoint(next);
            if (next >= route.size()) {
                boat.setDeltaMovement(0.0D, boat.getDeltaMovement().y, 0.0D);
                port.markDocked();
                LivingKingdoms.LOGGER.info(
                        "Erden supply barge docked shipment={} resource={} amount={} at_west_wharf=true actual_water_route=true customs_ready=true",
                        shipment.id(), shipment.resource(), shipment.amount());
            }
            return;
        }

        double vx = dx / distance * VESSEL_SPEED;
        double vz = dz / distance * VESSEL_SPEED;
        boat.setDeltaMovement(vx, boat.getDeltaMovement().y, vz);
        boat.setYRot((float) Math.toDegrees(Math.atan2(-vx, vz)));'''
new_waypoint = '''        if (distance <= 4.0D) {
            int next = index + 1;
            port.setWaypoint(next);
            if (isPortCi()) {
                ciLastProgressTick = level.getGameTime();
                ciLastWaypoint = next;
            }
            if (next >= route.size()) {
                boat.setDeltaMovement(0.0D, boat.getDeltaMovement().y, 0.0D);
                port.markDocked();
                LivingKingdoms.LOGGER.info(
                        "Erden supply barge docked shipment={} resource={} amount={} at_west_wharf=true actual_water_route=true customs_ready=true",
                        shipment.id(), shipment.resource(), shipment.amount());
            }
            return;
        }

        // An unmanned vanilla boat can damp externally assigned velocity before it makes reliable
        // route progress. Move the real entity through Minecraft collision resolution by the exact
        // distance that the configured 0.20 m/tick speed represents over this 5-tick controller
        // interval. This is not a waypoint teleport: collisions still constrain the entity.
        double step = Math.min(Math.max(0.0D, distance - 3.0D), VESSEL_SPEED * VESSEL_INTERVAL);
        double moveX = dx / distance * step;
        double moveZ = dz / distance * step;
        double beforeX = boat.getX();
        double beforeZ = boat.getZ();
        boat.move(MoverType.SELF, new Vec3(moveX, 0.0D, moveZ));
        boat.setDeltaMovement(0.0D, boat.getDeltaMovement().y, 0.0D);
        boat.setYRot((float) Math.toDegrees(Math.atan2(-moveX, moveZ)));

        double actualX = boat.getX() - beforeX;
        double actualZ = boat.getZ() - beforeZ;
        double actualMoved = Math.sqrt(actualX * actualX + actualZ * actualZ);
        if (isPortCi()) {
            long now = level.getGameTime();
            if (actualMoved >= 0.05D || ciLastWaypoint != port.waypoint()) {
                ciLastProgressTick = now;
                ciLastWaypoint = port.waypoint();
            }
            if (now % 100L == 0L) {
                LivingKingdoms.LOGGER.info(
                        "LK_ERDEN_RIVER_PORT_PROGRESS waypoint={} x={} y={} z={} target_x={} target_z={} distance={} moved={} travelled={} water_target={} entity_loaded=true collision_move=true",
                        port.waypoint(), Math.round(boat.getX()), Math.round(boat.getY()), Math.round(boat.getZ()),
                        target.x(), target.z(), Math.round(distance), String.format(java.util.Locale.ROOT, "%.3f", actualMoved),
                        Math.round(ciTravelled), waterAt(level, target));
            }
            if (ciLastProgressTick > 0L
                    && now - ciLastProgressTick > 400L
                    && routeChunkReady(level, port, target)
                    && waterAt(level, target)) {
                LivingKingdoms.LOGGER.error(
                        "LK_ERDEN_RIVER_PORT_STALL waypoint={} x={} z={} target_x={} target_z={} distance={} travelled={} stalled_ticks={} entity_loaded=true route_ready=true water_target=true",
                        port.waypoint(), Math.round(boat.getX()), Math.round(boat.getZ()), target.x(), target.z(),
                        Math.round(distance), Math.round(ciTravelled), now - ciLastProgressTick);
                throw new IllegalStateException("LK_ERDEN_RIVER_PORT_STALL physical barge made no collision-resolved progress");
            }
        }'''
if "collision_move=true" not in text:
    require(old_waypoint in text, "river-port velocity steering block not found")
    text = text.replace(old_waypoint, new_waypoint, 1)

require("MoverType.SELF" in text, "collision-aware river-port movement missing")
require("new Vec3(moveX, 0.0D, moveZ)" in text, "river-port movement vector missing")
require("LK_ERDEN_RIVER_PORT_STALL" in text, "river-port CI stall guard missing")
require("collision_move=true" in text, "river-port progress evidence missing")
PATH.write_text(text, encoding="utf-8")
print("Patched Silver River barge to collision-resolved physical movement with bounded CI stall diagnostics.")
