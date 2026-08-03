from pathlib import Path

root = Path("projects/living-kingdoms/src/main/java")
replacements = {
    "ErdenPhysicalEconomyManager.EXPECTED_SITES": "ErdenAuthoritativeEconomyManager.EXPECTED_SITES",
    "ErdenPhysicalEconomyManager.EXPECTED_WAREHOUSES": "ErdenAuthoritativeEconomyManager.EXPECTED_WAREHOUSES",
    "ErdenPhysicalEconomyManager.EXPECTED_WALLETS": "ErdenAuthoritativeEconomyManager.EXPECTED_WALLETS",
    "ErdenPhysicalEconomyManager.ciEntrances()": "ErdenAuthoritativeEconomyManager.ciEntrances()",
}

changed = []
counts = {key: 0 for key in replacements}
for path in sorted(root.rglob("*.java")):
    text = path.read_text(encoding="utf-8")
    updated = text
    for old, new in replacements.items():
        occurrences = updated.count(old)
        counts[old] += occurrences
        updated = updated.replace(old, new)
    if updated != text:
        path.write_text(updated, encoding="utf-8")
        changed.append(path)

expected_total = 10
actual_total = sum(counts.values())
if actual_total != expected_total:
    raise SystemExit(f"Expected {expected_total} obsolete references, found {actual_total}: {counts}")
if not changed:
    raise SystemExit("No Java files changed")
remaining = []
for path in sorted(root.rglob("*.java")):
    if "ErdenPhysicalEconomyManager" in path.read_text(encoding="utf-8"):
        remaining.append(str(path))
if remaining:
    raise SystemExit(f"Obsolete references remain: {remaining}")
print(f"Updated {len(changed)} files and removed {actual_total} obsolete references")
