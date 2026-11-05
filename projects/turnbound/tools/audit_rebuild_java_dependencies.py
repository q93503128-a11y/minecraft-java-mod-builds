#!/usr/bin/env python3
"""Conservative class-level dependency audit; never delete from this result alone.

The scanner intentionally treats references inside comments/strings as edges: this
overestimates reachability rather than deleting a potentially referenced class.
Investigate reflection, game event discovery, native API registries, resources and
test fixtures before applying any physical-removal suggestion.
"""
from collections import defaultdict, deque
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "src/main/java/io/github/q93503128/turnbound"
TEST_ROOT = ROOT / "src/test/java"
PATTERN = re.compile(r"\b[A-Z][A-Za-z0-9_]*\b")

def files_in(root):
    return sorted(root.rglob("*.java"))

def analyze():
    files = files_in(JAVA_ROOT)
    names = defaultdict(set)
    for f in files:
        names[f.stem].add(f)
    ambiguities = sorted(k for k, v in names.items() if len(v) > 1)
    if ambiguities:
        raise SystemExit(f"Cannot audit duplicate Java class stems: {ambiguities}")
    known = set(names)
    edges = {}
    for f in files:
        txt = f.read_text("utf-8")
        edges[f.stem] = PATTERN.findall(txt)
    edge_map = {k: (set(v) & known) - {k} for k, v in edges.items()}
    roots = {"Turnbound", "TurnboundClient"}
    # These are JVM entrypoints. Auto-discovered subscribers are included so
    # they cannot be misclassified if the separate subscriber guard is weakened.
    for f in files:
        txt = f.read_text("utf-8")
        if re.search(r"@(?:[\w.]*EventBusSubscriber|JourneyMapPlugin)\b", txt):
            roots.add(f.stem)
    roots &= known
    def closure(start):
        reached = set(start)
        queue = deque(start)
        while queue:
            node = queue.popleft()
            for nxt in edge_map[node] - reached:
                reached.add(nxt)
                queue.append(nxt)
        return reached
    prod = closure(roots)
    tested = set()
    test_file_count = 0
    for f in files_in(TEST_ROOT):
        test_file_count += 1
        tested |= (set(PATTERN.findall(f.read_text("utf-8"))) & known)
    test_closure = closure(tested)
    disconnected = known - prod
    unreferenced = known - (prod | test_closure)
    per_package = defaultdict(lambda: {"total":0,"productionReachable":0,"testReachable":0,"disconnected":0,"unreferenced":0})
    for name, paths in names.items():
        relative = next(iter(paths)).relative_to(JAVA_ROOT)
        package = relative.parts[0] if len(relative.parts)>1 else "(root)"
        record=per_package[package]
        record["total"]+=1
        if name in prod: record["productionReachable"]+=1
        if name in test_closure: record["testReachable"]+=1
        if name in disconnected: record["disconnected"]+=1
        if name in unreferenced: record["unreferenced"]+=1
    return {
        "caveat": "Conservative identifier graph, not bytecode proof. Non-Java reflection, service loaders and asset metadata need separate review.",
        "sourceFiles":len(files),
        "testSourceFiles":test_file_count,
        "roots":sorted(roots),
        "productionReachable":len(prod),
        "testReachable":len(test_closure),
        "disconnectedFromProduction":len(disconnected),
        "unreferencedByProductionAndTest":len(unreferenced),
        "packages":dict(sorted(per_package.items())),
        "potentiallyRemovable":sorted(unreferenced),
        "disconnectedFromProductionNames":sorted(disconnected),
    }

if __name__=="__main__":
    report=analyze()
    dest=ROOT / "build/rebuild-java-audit.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(report, indent=2, ensure_ascii=False), "utf-8")
    print("=== TURNBOUND CONSERVATIVE REACHABILITY AUDIT ===")
    print(json.dumps({k:v for k,v in report.items() if k not in ("disconnectedFromProductionNames",)}, ensure_ascii=False, indent=2))
    print(f"Full audit: {dest}")
