from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
WORKER = ROOT / "src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementWorkerService.java"
VERIFY = ROOT / "tools/test_current_source.py"
GRADLE = ROOT / "gradle.properties"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one anchor, found {count}")
    return text.replace(old, new, 1)


gradle = GRADLE.read_text(encoding="utf-8")
gradle = replace_once(
    gradle,
    "mod_version=0.1.0-alpha.115",
    "mod_version=0.1.0-alpha.116",
    "Frontier version",
)
alpha115 = "# Alpha.115 worker/workplace authority: same-profession production civilians are matched to completed workplaces by deterministic nearest physical distance instead of unrelated UUID/list indexes; vacancy recruitment uses the same matching so later-building survivors cannot cause duplicate arrivals at the wrong workplace.\n"
alpha116 = alpha115 + "\n# Alpha.116 worker/workplace matching stability: same-profession production civilians use deterministic global minimum-total-distance matching across the currently eligible physical workplaces instead of greedy nearest-pair selection, so one locally closest pair cannot strand another worker at a distant wrong workplace.\n"
if "# Alpha.116 worker/workplace matching stability:" not in gradle:
    gradle = replace_once(gradle, alpha115, alpha116, "Alpha.115 history anchor")
GRADLE.write_text(gradle, encoding="utf-8")

worker = WORKER.read_text(encoding="utf-8")
start_marker = "    private static List<WorkerBuildingAssignment> matchWorkersToBuildings(List<BuildingRecord> buildings,\n"
end_marker = "\n    private static void tryAttractWorker(MinecraftServer server, ServerLevel level, SettlementData data) {"
start = worker.find(start_marker)
end = worker.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit("worker matcher span anchor missing")
replacement = '''    private static List<WorkerBuildingAssignment> matchWorkersToBuildings(List<BuildingRecord> buildings,
                                                                           List<FrontierWorkerEntity> workers) {
        List<BuildingRecord> sortedBuildings = new ArrayList<>(buildings);
        sortedBuildings.sort(Comparator.comparingLong(building -> building.workCenter().asLong()));
        List<FrontierWorkerEntity> sortedWorkers = new ArrayList<>(workers);
        sortedWorkers.sort(Comparator.comparing(worker -> worker.getUUID().toString()));
        if (sortedBuildings.isEmpty() || sortedWorkers.isEmpty()) return List.of();

        // Alpha.116: solve the complete minimum-total-distance bipartite assignment rather than
        // repeatedly taking the single nearest pair. The greedy Alpha.115 matcher could reserve the
        // only reasonable worker for one workplace and strand the remaining worker at a very distant
        // workplace even though a much shorter one-to-one assignment existed. Inputs are sorted first,
        // and equal reduced costs prefer the lower column, so ties remain deterministic without adding
        // a UUID/workplace save ledger or manual worker assignment UI.
        boolean buildingsAreRows = sortedBuildings.size() <= sortedWorkers.size();
        int rowCount = buildingsAreRows ? sortedBuildings.size() : sortedWorkers.size();
        int columnCount = buildingsAreRows ? sortedWorkers.size() : sortedBuildings.size();
        double[] rowPotential = new double[rowCount + 1];
        double[] columnPotential = new double[columnCount + 1];
        int[] columnMatch = new int[columnCount + 1];
        int[] previousColumn = new int[columnCount + 1];

        for (int row = 1; row <= rowCount; row++) {
            columnMatch[0] = row;
            double[] bestReducedCost = new double[columnCount + 1];
            java.util.Arrays.fill(bestReducedCost, Double.POSITIVE_INFINITY);
            boolean[] usedColumn = new boolean[columnCount + 1];
            int column0 = 0;
            do {
                usedColumn[column0] = true;
                int matchedRow = columnMatch[column0];
                double delta = Double.POSITIVE_INFINITY;
                int column1 = 0;
                for (int column = 1; column <= columnCount; column++) {
                    if (usedColumn[column]) continue;
                    double reducedCost = assignmentCost(sortedBuildings, sortedWorkers, buildingsAreRows,
                            matchedRow - 1, column - 1) - rowPotential[matchedRow] - columnPotential[column];
                    if (reducedCost < bestReducedCost[column]) {
                        bestReducedCost[column] = reducedCost;
                        previousColumn[column] = column0;
                    }
                    if (bestReducedCost[column] < delta
                            || (Double.compare(bestReducedCost[column], delta) == 0
                            && (column1 == 0 || column < column1))) {
                        delta = bestReducedCost[column];
                        column1 = column;
                    }
                }
                for (int column = 0; column <= columnCount; column++) {
                    if (usedColumn[column]) {
                        rowPotential[columnMatch[column]] += delta;
                        columnPotential[column] -= delta;
                    } else if (column > 0) {
                        bestReducedCost[column] -= delta;
                    }
                }
                column0 = column1;
            } while (columnMatch[column0] != 0);

            do {
                int column1 = previousColumn[column0];
                columnMatch[column0] = columnMatch[column1];
                column0 = column1;
            } while (column0 != 0);
        }

        List<WorkerBuildingAssignment> result = new ArrayList<>();
        for (int column = 1; column <= columnCount; column++) {
            int row = columnMatch[column];
            if (row == 0) continue;
            BuildingRecord building = buildingsAreRows
                    ? sortedBuildings.get(row - 1)
                    : sortedBuildings.get(column - 1);
            FrontierWorkerEntity worker = buildingsAreRows
                    ? sortedWorkers.get(column - 1)
                    : sortedWorkers.get(row - 1);
            result.add(new WorkerBuildingAssignment(building, worker));
        }
        result.sort(Comparator
                .comparingLong((WorkerBuildingAssignment assignment) -> assignment.building().workCenter().asLong())
                .thenComparing(assignment -> assignment.worker().getUUID().toString()));
        return result;
    }

    private static double assignmentCost(List<BuildingRecord> buildings, List<FrontierWorkerEntity> workers,
                                         boolean buildingsAreRows, int rowIndex, int columnIndex) {
        BuildingRecord building = buildingsAreRows ? buildings.get(rowIndex) : buildings.get(columnIndex);
        FrontierWorkerEntity worker = buildingsAreRows ? workers.get(columnIndex) : workers.get(rowIndex);
        BlockPos work = building.workCenter();
        return worker.distanceToSqr(work.getX() + 0.5D, work.getY(), work.getZ() + 0.5D);
    }
'''
worker = worker[:start] + replacement + worker[end:]
WORKER.write_text(worker, encoding="utf-8")

verify = VERIFY.read_text(encoding="utf-8")
verify = replace_once(
    verify,
    'require("mod_version=0.1.0-alpha.115" in gradle, "current verifier/version drift")',
    'require("mod_version=0.1.0-alpha.116" in gradle, "current verifier/version drift")',
    "verifier version",
)
match_anchor = '''require("matchWorkersToBuildings" in worker and "WorkerBuildingAssignment" in worker,
        "same-profession workers returned to UUID/list-index workplace assignment")
'''
match_extra = match_anchor + '''require("minimum-total-distance bipartite assignment" in worker
        and "rowPotential" in worker and "columnPotential" in worker and "assignmentCost(" in worker,
        "same-profession worker matching regressed to greedy nearest-pair selection")
'''
if "same-profession worker matching regressed to greedy nearest-pair selection" not in verify:
    verify = replace_once(verify, match_anchor, match_extra, "worker-matching verifier anchor")
verify = replace_once(
    verify,
    'print("CURRENT SOURCE CHECK PASS: Frontier Settlement 0.1.0-alpha.115 worker/workplace authority + physical production ecology/balance + prior invariants")',
    'print("CURRENT SOURCE CHECK PASS: Frontier Settlement 0.1.0-alpha.116 minimum-distance worker/workplace matching + physical production ecology/balance + prior invariants")',
    "verifier summary",
)
VERIFY.write_text(verify, encoding="utf-8")

print("Applied Frontier Settlement Alpha.116 minimum-distance worker/workplace matching")
