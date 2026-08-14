from pathlib import Path

p = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenRiverPortManager.java")
s = p.read_text(encoding="utf-8")


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


require("MoverType.SELF" in s, "collision-resolved barge movement must already exist")
require("ciBestTargetDistance" in s, "waypoint-distance stall diagnostics must already exist")

if "CI_TICKET_REFRESH_INTERVAL" not in s:
    s = s.replace(
        "    private static final int VESSEL_INTERVAL = 5;\n",
        "    private static final int VESSEL_INTERVAL = 5;\n"
        "    private static final int CI_TICKET_REFRESH_INTERVAL = 100;\n",
        1)
    s = s.replace(
        "    private static double ciBestTargetDistance = Double.POSITIVE_INFINITY;\n",
        "    private static double ciBestTargetDistance = Double.POSITIVE_INFINITY;\n"
        "    private static long ciTicketRefreshes;\n",
        1)
    s = s.replace(
        "        ciBestTargetDistance = Double.POSITIVE_INFINITY;\n    }\n",
        "        ciBestTargetDistance = Double.POSITIVE_INFINITY;\n"
        "        ciTicketRefreshes = 0L;\n    }\n",
        1)

    old_tick = '''        if (isPortCi()) prepareCi(level);
        advanceConstruction(level);'''
    new_tick = '''        if (isPortCi()) {
            prepareCi(level);
            if (level.getGameTime() % CI_TICKET_REFRESH_INTERVAL == 0L) {
                refreshCiTickets(level);
            }
        }
        advanceConstruction(level);'''
    require(old_tick in s, "river-port CI tick anchor missing")
    s = s.replace(old_tick, new_tick, 1)

    anchor = '''    private static void verifyCi(ServerLevel level) {
'''
    helper = '''    private static void refreshCiTickets(ServerLevel level) {
        if (CI_RETAINED.isEmpty()) return;
        int loaded = 0;
        for (long packed : Set.copyOf(CI_RETAINED)) {
            ChunkPos chunk = new ChunkPos(unpackX(packed), unpackZ(packed));
            level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 0);
            if (level.hasChunk(chunk.x, chunk.z)) loaded++;
        }
        ciTicketRefreshes++;
        if (ciTicketRefreshes == 1L || ciTicketRefreshes % 5L == 0L) {
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_RIVER_PORT_TICKET_REFRESH refresh={} retained={} loaded={} interval_ticks={} timeout_safe_refresh=true async_ticket=true forced_chunks=false",
                    ciTicketRefreshes, CI_RETAINED.size(), loaded, CI_TICKET_REFRESH_INTERVAL);
        }
    }

'''
    require(anchor in s, "river-port verify anchor missing")
    s = s.replace(anchor, helper + anchor, 1)

    old_pass = '''"LK_ERDEN_RIVER_PORT_PASS revision=1 silver_river_navigable=true west_wharf_physical=true customs_house=true shipyard=true supply_barge_escrow_linked=true real_boat_entity=true actual_water_movement=true travelled_metres={} loaded_only_runtime=true forced_citywide=false ci_corridor_only=true ci_corridor_retained_until_pass=true ci_tickets_released_at_pass=true",
                Math.round(ciTravelled));'''
    new_pass = '''"LK_ERDEN_RIVER_PORT_PASS revision=1 silver_river_navigable=true west_wharf_physical=true customs_house=true shipyard=true supply_barge_escrow_linked=true real_boat_entity=true actual_water_movement=true travelled_metres={} loaded_only_runtime=true forced_citywide=false ci_corridor_only=true ci_corridor_retained_until_pass=true ci_tickets_released_at_pass=true ci_ticket_refreshes={} timeout_safe_refresh=true",
                Math.round(ciTravelled), ciTicketRefreshes);'''
    require(old_pass in s, "river-port PASS log anchor missing")
    s = s.replace(old_pass, new_pass, 1)

for token in [
    "CI_TICKET_REFRESH_INTERVAL = 100",
    "refreshCiTickets(level)",
    "addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 0)",
    "LK_ERDEN_RIVER_PORT_TICKET_REFRESH",
    "timeout_safe_refresh=true",
    "ci_ticket_refreshes={}",
]:
    require(token in s, "missing river-port ticket lifecycle invariant: " + token)

p.write_text(s, encoding="utf-8")
print("Refreshes bounded river-port CI tickets before ticket-type expiry; product runtime is unchanged.")
