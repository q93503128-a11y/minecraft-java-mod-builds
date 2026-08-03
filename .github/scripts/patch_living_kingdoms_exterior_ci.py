from pathlib import Path

path = Path("projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world/ErdenKingdomExteriorBuilder.java")
text = path.read_text(encoding="utf-8")

replacements = [
    (
        "    private static final int TICK_BUDGET = 2_000;\n    private static final int CI_FORCE_BUDGET = 1;",
        "    private static final int TICK_BUDGET = 2_000;\n    private static final int CI_TICK_BUDGET = 16_000;\n    private static final int CI_FORCE_BUDGET = 1;\n    private static final int CI_MAX_IN_FLIGHT = 3;",
    ),
    (
        "    private static final Set<Long> CI_LOADING = new HashSet<>();\n    private static final Set<Long> QUEUED = new HashSet<>();",
        "    private static final Set<Long> CI_LOADING = new HashSet<>();\n    private static final Set<Long> CI_REQUIRED = new HashSet<>();\n    private static final Set<Long> QUEUED = new HashSet<>();",
    ),
    (
        "        ChunkPos chunk = event.getChunk().getPos();\n        if (!intersectsExterior(chunk)) return;\n        enqueue(level, pack(chunk.x(), chunk.z()), false);",
        "        ChunkPos chunk = event.getChunk().getPos();\n        if (!intersectsExterior(chunk)) return;\n        long packed = pack(chunk.x(), chunk.z());\n        if (isCi() && !CI_REQUIRED.contains(packed)) return;\n        enqueue(level, packed, false);",
    ),
    (
        "        active.plan.apply(level, TICK_BUDGET);",
        "        active.plan.apply(level, isCi() ? CI_TICK_BUDGET : TICK_BUDGET);",
    ),
    (
        "        CI_LOADING.clear();\n        QUEUED.clear();",
        "        CI_LOADING.clear();\n        CI_REQUIRED.clear();\n        QUEUED.clear();",
    ),
    (
        "        CI_REQUESTS.addAll(unique);",
        "        CI_REQUIRED.addAll(unique);\n        CI_REQUESTS.addAll(unique);",
    ),
    (
        "        for (int forced = 0; forced < CI_FORCE_BUDGET && !CI_REQUESTS.isEmpty(); forced++) {",
        "        for (int forced = 0; forced < CI_FORCE_BUDGET\n                && !CI_REQUESTS.isEmpty()\n                && RETAINED.size() < CI_MAX_IN_FLIGHT; forced++) {",
    ),
    (
        "                \"Requested Erden exterior CI anchors nodes={} request_queue={} metre_scale=true streamed=true staggered=true synchronous_get_chunk=false\",",
        "                \"Requested Erden exterior CI anchors nodes={} request_queue={} metre_scale=true streamed=true staggered=true synchronous_get_chunk=false max_in_flight={}\",",
    ),
    (
        "                ErdenKingdomSupplyCatalog.nodes().size(), CI_REQUESTS.size());",
        "                ErdenKingdomSupplyCatalog.nodes().size(), CI_REQUESTS.size(), CI_MAX_IN_FLIGHT);",
    ),
]

for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match, got {count}: {old[:100]!r}")
    text = text.replace(old, new)

path.write_text(text, encoding="utf-8")
