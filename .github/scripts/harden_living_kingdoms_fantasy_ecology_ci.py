from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PATH = ROOT / "projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenFantasyEcologyManager.java"
text = PATH.read_text(encoding="utf-8")


def require(ok: bool, message: str) -> None:
    if not ok:
        raise SystemExit(message)


if "CI_TICKET_REFRESH_INTERVAL" not in text:
    text = text.replace(
        "    private static final int SPAWN_INTERVAL = 100;\n",
        "    private static final int SPAWN_INTERVAL = 100;\n"
        "    private static final int CI_TICKET_REFRESH_INTERVAL = 100;\n",
        1,
    )
    text = text.replace(
        "    private static boolean ciPrepared;\n    private static boolean ciPassed;\n",
        "    private static boolean ciPrepared;\n    private static boolean ciPassed;\n"
        "    private static long ciTicketRefreshes;\n",
        1,
    )
    text = text.replace(
        "        ciPrepared = false;\n        ciPassed = false;\n",
        "        ciPrepared = false;\n        ciPassed = false;\n"
        "        ciTicketRefreshes = 0L;\n",
        1,
    )

    old_ci = '''        if (isCi()) {
            prepareCi(level);
            verifyCi(level);
        }'''
    new_ci = '''        if (isCi()) {
            prepareCi(level);
            if (level.getGameTime() % CI_TICKET_REFRESH_INTERVAL == 0L) {
                refreshCiTickets(level);
            }
            verifyCi(level);
        }'''
    require(old_ci in text, "ecology CI tick anchor missing")
    text = text.replace(old_ci, new_ci, 1)

    anchor = '''    private static void verifyCi(ServerLevel level) {
'''
    helper = '''    private static void refreshCiTickets(ServerLevel level) {
        if (CI_TICKETS.isEmpty() || ciPassed) return;
        int loaded = 0;
        for (long packed : Set.copyOf(CI_TICKETS)) {
            ChunkPos chunk = new ChunkPos(unpackX(packed), unpackZ(packed));
            level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 0);
            if (level.hasChunk(chunk.x(), chunk.z())) loaded++;
        }
        ciTicketRefreshes++;
        if (ciTicketRefreshes == 1L || ciTicketRefreshes % 10L == 0L) {
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_FANTASY_ECOLOGY_TICKET_REFRESH refresh={} retained={} loaded={} interval_ticks={} timeout_safe_refresh=true forced_chunks=false",
                    ciTicketRefreshes, CI_TICKETS.size(), loaded, CI_TICKET_REFRESH_INTERVAL);
        }
    }

'''
    require(anchor in text, "ecology verify anchor missing")
    text = text.replace(anchor, helper + anchor, 1)

    old_pass = '''"LK_ERDEN_FANTASY_ECOLOGY_PASS revision=1 registered_species=3 silver_hart=true ash_hound=true river_wisp=true actual_custom_entity_types=true actual_entity_instances=true northern_forest_spawn=true western_hill_spawn=true silver_river_spawn=true capital_spawn=false player_loaded_runtime=true local_species_cap={} forced_citywide=false ci_sample_chunks=3 ci_tickets_released=true",
                LOCAL_SPECIES_CAP);'''
    new_pass = '''"LK_ERDEN_FANTASY_ECOLOGY_PASS revision=1 registered_species=3 silver_hart=true ash_hound=true river_wisp=true actual_custom_entity_types=true actual_entity_instances=true northern_forest_spawn=true western_hill_spawn=true silver_river_spawn=true capital_spawn=false player_loaded_runtime=true local_species_cap={} forced_citywide=false ci_sample_chunks=3 ci_tickets_released=true ci_ticket_refreshes={} timeout_safe_refresh=true",
                LOCAL_SPECIES_CAP, ciTicketRefreshes);'''
    require(old_pass in text, "ecology PASS log anchor missing")
    text = text.replace(old_pass, new_pass, 1)

# Fast worlds can finish the three-sample audit before the first 100-tick cadence. Exercise the
# exact refresh path once immediately after all bounded sample tickets are registered, then keep the
# periodic refresh above as the timeout protection. This remains CI-only and never force-loads
# ecology chunks in normal gameplay.
if "ciTicketRefreshes == 0L" not in text:
    old_prepare = '''        for (Sample sample : CI_SAMPLES) {
            ChunkPos chunk = new ChunkPos(sample.x() >> 4, sample.z() >> 4);
            long packed = pack(chunk.x(), chunk.z());
            if (CI_TICKETS.add(packed)) {
                level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 0);
            }
        }
        if (ciPrepared) return;'''
    new_prepare = '''        for (Sample sample : CI_SAMPLES) {
            ChunkPos chunk = new ChunkPos(sample.x() >> 4, sample.z() >> 4);
            long packed = pack(chunk.x(), chunk.z());
            if (CI_TICKETS.add(packed)) {
                level.getChunkSource().addTicketAndLoadWithRadius(TicketType.PORTAL, chunk, 0);
            }
        }
        if (ciTicketRefreshes == 0L && !CI_TICKETS.isEmpty()) {
            refreshCiTickets(level);
        }
        if (ciPrepared) return;'''
    require(old_prepare in text, "ecology CI prepare ticket anchor missing")
    text = text.replace(old_prepare, new_prepare, 1)

for token in [
    "CI_TICKET_REFRESH_INTERVAL = 100",
    "refreshCiTickets(level)",
    "ciTicketRefreshes == 0L",
    "LK_ERDEN_FANTASY_ECOLOGY_TICKET_REFRESH",
    "ci_ticket_refreshes={}",
    "timeout_safe_refresh=true",
]:
    require(token in text, "missing ecology CI ticket invariant: " + token)

PATH.write_text(text, encoding="utf-8")
print("Hardened bounded fantasy ecology CI tickets and exercises the refresh path before fast-world PASS.")
