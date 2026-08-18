from pathlib import Path

ROOT = Path('.github/workflows')
NEW_RESIDENCE = ('Prepared Erden residence modes buildings=233 authored_ground_candidates=233 '
                 'authored_upper_candidates=233 ground_only_candidates=0 synthetic_upper_created=0')
OLD_RESIDENCE = ('Prepared Erden residence modes buildings=233 ground_only_candidates=77 '
                 'authored_upper_candidates=156 fresh_synthetic_upper_created=0')


def replace_required(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        if new in text:
            return text
        raise SystemExit(f'{label}: expected text not found')
    return text.replace(old, new)


def patch_residence() -> None:
    path = ROOT / 'audit-living-kingdoms-residence-modes.yml'
    s = path.read_text(encoding='utf-8')
    s = replace_required(s, "grep -F 'EXPECTED_GROUND_ONLY_BUILDINGS = 77'", "grep -F 'EXPECTED_GROUND_ONLY_BUILDINGS = 0'", 'residence ground-only source guard')
    s = replace_required(s, "grep -F 'fresh_synthetic_upper_created=0' \"$root/ErdenUrbanLifeManager.java\"", "grep -F 'synthetic_upper_created=0' \"$root/ErdenUrbanLifeManager.java\"", 'residence synthetic source guard')
    s = replace_required(s, OLD_RESIDENCE, NEW_RESIDENCE, 'residence mode marker')
    s = replace_required(s, 'LK_URBAN_LIFE_DIAGNOSTIC_PASS upper_floor=false ground_only=true', 'LK_ERDEN_AUTHORED_GROUND_MATERIALIZER_PASS', 'residence ground diagnostic marker')

    first = f"            if grep -Fq '{NEW_RESIDENCE}' logs/residence-server.log \\\n"
    expanded = ("            if grep -Fq 'LK_ERDEN_AUTHORED_GROUND_FUNCTIONAL_PLAN_PASS placements=233' logs/residence-server.log \\\n"
                f"              && grep -Fq '{NEW_RESIDENCE}' logs/residence-server.log \\\n"
                "              && grep -Fq 'LK_URBAN_INTERIOR_DIAGNOSTIC_PASS' logs/residence-server.log \\\n")
    s = replace_required(s, first, expanded, 'residence pass preconditions')

    bad_pipeline = "          grep -F 'LK_ERDEN_AUTHORED_GROUND_MATERIALIZER_PASS' logs/residence-server.log | grep -F 'synthetic_fallback=false' | grep -F 'fresh_synthetic_upper_created=0'\n"
    good_pipeline = ("          grep -F 'LK_ERDEN_AUTHORED_GROUND_FUNCTIONAL_PLAN_PASS placements=233' logs/residence-server.log | grep -F 'source_floor_reused=true' | grep -F 'source_blocks_cut=0'\n"
                     "          grep -F 'LK_ERDEN_AUTHORED_GROUND_MATERIALIZER_PASS' logs/residence-server.log | grep -F 'source_floor_reused=true' | grep -F 'source_air_fixtures=true' | grep -F 'source_blocks_cut=0'\n"
                     "          grep -F 'LK_URBAN_INTERIOR_DIAGNOSTIC_PASS' logs/residence-server.log | grep -F 'authored_ground=true' | grep -F 'synthetic_room=false' | grep -F 'source_blocks_cut=0'\n")
    s = replace_required(s, bad_pipeline, good_pipeline, 'residence ground proof pipeline')

    old_echo = "          echo 'LK_ERDEN_RESIDENCE_MODES_AUDIT_PASS source_catalog_buildings=233 authored_upper=156 ground_only=77 fresh_synthetic_upper=0 population=231 workers=154 loaded_restore_sample=true loaded_restore_rollbacks=0 authored_upper_source_blocks_cut=0 forced_citywide=false' | tee logs/residence-pass.txt\n"
    new_echo = ("          if grep -Fq 'ground_only_candidates=77' logs/residence-server.log; then\n"
                "            echo 'Legacy Erden ground-only residence marker reappeared.' >&2\n"
                "            exit 1\n"
                "          fi\n"
                "          echo 'LK_ERDEN_RESIDENCE_MODES_AUDIT_PASS source_catalog_buildings=233 authored_ground=233 authored_upper=233 ground_only=0 synthetic_upper=0 population=231 workers=154 loaded_restore_sample=true loaded_restore_rollbacks=0 source_blocks_cut=0 forced_citywide=false' | tee logs/residence-pass.txt\n")
    s = replace_required(s, old_echo, new_echo, 'residence final audit marker')
    path.write_text(s, encoding='utf-8')


def patch_inventory() -> None:
    path = ROOT / 'audit-living-kingdoms-exterior-inventory.yml'
    s = path.read_text(encoding='utf-8')
    guard = "          grep -F 'public static final int EXPECTED_CANDIDATES = 116;' src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanNewFloorStructuralApprovalCatalog.java\n"
    expanded_guard = (guard
        + "          grep -F 'public static final int INTERIOR_REVISION = 2;' src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanInteriorBuilder.java\n"
        + "          grep -F 'public static final int EXPECTED_PLANS = 233;' src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanAuthoredGroundPlanCatalog.java\n"
        + "          grep -F 'EXPECTED_GROUND_ONLY_BUILDINGS = 0' src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenUrbanResidenceResolver.java\n")
    s = replace_required(s, guard, expanded_guard, 'inventory authored source guards')
    s = replace_required(s, OLD_RESIDENCE, NEW_RESIDENCE, 'inventory residence marker')
    s = replace_required(s, 'LK_URBAN_LIFE_DIAGNOSTIC_PASS upper_floor=false ground_only=true', 'LK_ERDEN_AUTHORED_GROUND_MATERIALIZER_PASS', 'inventory ground diagnostic marker')

    needle = "              && grep -Fq 'LK_ERDEN_AUTHORED_NEW_FLOOR_PASS candidates=116' ../../logs/exterior-inventory-server.log \\\n"
    replacement = ("              && grep -Fq 'LK_ERDEN_AUTHORED_GROUND_FUNCTIONAL_PLAN_PASS placements=233' ../../logs/exterior-inventory-server.log \\\n"
                   "              && grep -Fq 'LK_URBAN_INTERIOR_DIAGNOSTIC_PASS' ../../logs/exterior-inventory-server.log \\\n"
                   + needle)
    s = replace_required(s, needle, replacement, 'inventory pass authored ground conditions')

    post = "          grep -F 'LK_ERDEN_AUTHORED_NEW_FLOOR_PASS candidates=116' ../../logs/exterior-inventory-server.log\n"
    post_new = ("          grep -F 'LK_ERDEN_AUTHORED_GROUND_FUNCTIONAL_PLAN_PASS placements=233' ../../logs/exterior-inventory-server.log | grep -F 'source_floor_reused=true' | grep -F 'source_blocks_cut=0'\n"
                "          grep -F 'LK_ERDEN_AUTHORED_GROUND_MATERIALIZER_PASS' ../../logs/exterior-inventory-server.log | grep -F 'source_air_fixtures=true' | grep -F 'source_blocks_cut=0'\n"
                "          grep -F 'LK_URBAN_INTERIOR_DIAGNOSTIC_PASS' ../../logs/exterior-inventory-server.log | grep -F 'authored_ground=true' | grep -F 'synthetic_room=false' | grep -F 'source_blocks_cut=0'\n"
                + post)
    s = replace_required(s, post, post_new, 'inventory post authored ground proof')

    tail_guard = "          if grep -Eqi 'Unsupported exterior resource|Unknown Erden supply role|Invalid Erden exterior residence|Exception loading level|watchdog|Item debris remained|authored new-floor verification failed' ../../logs/exterior-inventory-server.log; then\n"
    tail_new = ("          if grep -Fq 'ground_only_candidates=77' ../../logs/exterior-inventory-server.log; then\n"
                "            echo 'Legacy Erden ground-only residence marker reappeared.' >&2\n"
                "            exit 1\n"
                "          fi\n\n" + tail_guard)
    s = replace_required(s, tail_guard, tail_new, 'inventory legacy negative guard')
    path.write_text(s, encoding='utf-8')


def patch_build() -> None:
    path = ROOT / 'build-living-kingdoms.yml'
    s = path.read_text(encoding='utf-8')
    population_condition = "              && grep -Fq 'LK_ERDEN_POPULATION_DIAGNOSTIC_PASS households=77 residents=231 workers=154' ../../logs/server-smoke.log \\\n"
    expanded = ("              && grep -Fq 'LK_ERDEN_AUTHORED_GROUND_FUNCTIONAL_PLAN_PASS placements=233' ../../logs/server-smoke.log \\\n"
                f"              && grep -Fq '{NEW_RESIDENCE}' ../../logs/server-smoke.log \\\n"
                "              && grep -Fq 'LK_URBAN_INTERIOR_DIAGNOSTIC_PASS' ../../logs/server-smoke.log \\\n"
                "              && grep -Fq 'LK_ERDEN_AUTHORED_UPPER_ROUTE_PASS' ../../logs/server-smoke.log \\\n"
                + population_condition)
    s = replace_required(s, population_condition, expanded, 'canonical build authored interior gate')

    household = "          grep -F 'Prepared Erden household population households=77 residents=231 workers=154 dependents=77 owned_homes=77 assigned_workplaces=154 vacancies=2 shifts=early,late,night' ../../logs/server-smoke.log\n"
    proof = ("          grep -F 'LK_ERDEN_AUTHORED_GROUND_FUNCTIONAL_PLAN_PASS placements=233' ../../logs/server-smoke.log | grep -F 'source_floor_reused=true' | grep -F 'source_blocks_cut=0'\n"
             f"          grep -F '{NEW_RESIDENCE}' ../../logs/server-smoke.log\n"
             "          grep -F 'LK_URBAN_INTERIOR_DIAGNOSTIC_PASS' ../../logs/server-smoke.log | grep -F 'authored_ground=true' | grep -F 'synthetic_room=false' | grep -F 'source_blocks_cut=0'\n"
             "          grep -F 'LK_ERDEN_AUTHORED_UPPER_ROUTE_PASS' ../../logs/server-smoke.log | grep -F 'source_blocks_cut=0'\n"
             + household)
    s = replace_required(s, household, proof, 'canonical build authored interior proof')

    class_line = "            ErdenCapitalStreamingBuilder ExternalDistrictBuildingBuilder ExternalUrbanFabricBuilder \\\n            ErdenUrbanInfrastructureBuilder ErdenUrbanInteriorBuilder ErdenUrbanLifeManager \\\n"
    class_new = ("            ErdenCapitalStreamingBuilder ExternalDistrictBuildingBuilder ExternalUrbanFabricBuilder \\\n"
                 "            ErdenUrbanInfrastructureBuilder ErdenUrbanInteriorBuilder ErdenUrbanLifeManager \\\n"
                 "            ErdenUrbanAuthoredGroundPlanCatalog ErdenUrbanAuthoredGroundMaterializer ErdenUrbanResidenceResolver \\\n")
    s = replace_required(s, class_line, class_new, 'canonical jar authored classes')

    legacy_guard = "          if grep -Eq 'silvana_forest|kardum_league|woodland_mansion|deepslate_mega_base|Relocated four Erden town houses' ../../logs/server-smoke.log; then\n"
    legacy_new = ("          if grep -Fq 'ground_only_candidates=77' ../../logs/server-smoke.log; then\n"
                  "            echo 'Legacy Erden ground-only residence marker reappeared.' >&2\n"
                  "            exit 1\n"
                  "          fi\n"
                  + legacy_guard)
    s = replace_required(s, legacy_guard, legacy_new, 'canonical legacy negative guard')
    path.write_text(s, encoding='utf-8')


def patch_physical_economy() -> None:
    path = ROOT / 'validate-living-kingdoms-physical-economy.yml'
    s = path.read_text(encoding='utf-8')
    s = replace_required(s, '# revision: 4', '# revision: 5', 'physical economy audit revision')
    condition = "            if grep -Fq 'LK_ERDEN_PHYSICAL_ECONOMY_PASS sites=156 warehouses=15 wallets=77' \"$LOG_FILE\" \\\n"
    condition_new = ("            if grep -Fq 'Requested Erden physical-economy authored interior CI samples sites=3' \"$LOG_FILE\" \\\n"
                     "              && grep -Fq 'LK_ERDEN_PHYSICAL_ECONOMY_PASS sites=156 warehouses=15 wallets=77' \"$LOG_FILE\" \\\n")
    s = replace_required(s, condition, condition_new, 'physical economy authored sample gate')
    prep = "          grep -F 'Prepared Erden physical economy sites=156 warehouses=15 wallets=77 container_resources=8' \"$LOG_FILE\"\n"
    prep_new = ("          grep -F 'Requested Erden physical-economy authored interior CI samples sites=3' \"$LOG_FILE\" | grep -F 'bounded_plan_chunks=true' | grep -F 'persistent_forced_chunks=false'\n"
                + prep)
    s = replace_required(s, prep, prep_new, 'physical economy authored sample proof')
    path.write_text(s, encoding='utf-8')


patch_residence()
patch_inventory()
patch_build()
patch_physical_economy()
print('Aligned Living Kingdoms authored interior audit contracts')
