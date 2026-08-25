#!/usr/bin/env python3
from pathlib import Path

path = Path(__file__).with_name("survival_ascension_058_patch.py")
source = path.read_text(encoding="utf-8")
old = '''def replace_once(rel: str, old: str, new: str) -> None:\n    text = read(rel)\n    count = text.count(old)\n    if count != 1:\n        raise RuntimeError(f"{rel}: expected one anchor, got {count}: {old[:100]!r}")\n    write(rel, text.replace(old, new, 1))\n'''
new = '''def replace_once(rel: str, old: str, new: str) -> None:\n    text = read(rel)\n    count = text.count(old)\n    if count == 1:\n        write(rel, text.replace(old, new, 1))\n        return\n    if (\n        rel.endswith("ExpeditionIncidentSystem.java")\n        and count == 2\n        and old == "        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {\\n"\n        and "renderBoundary(active)" in new\n    ):\n        index = text.rfind(old)\n        write(rel, text[:index] + new + text[index + len(old):])\n        return\n    raise RuntimeError(f"{rel}: expected one anchor, got {count}: {old[:100]!r}")\n'''
count = source.count(old)
if count != 1:
    raise RuntimeError(f"replace_once helper definition drifted: {count}")
patched = source.replace(old, new, 1)

protocol_contract = '''replace_once("tools/test_current_source.py", 'need(network, [\\'PROTOCOL = "8"\\'], "protocol")', 'need(network, [\\'PROTOCOL = "9"\\'], "protocol")')'''
contract_extension = protocol_contract + '''\nreplace_once("tools/test_current_source.py", "fieldMastery ? 65 : SkillTuning.constructionLineLength(level)", "selectedLength(player, level)")'''
count = patched.count(protocol_contract)
if count != 1:
    raise RuntimeError(f"protocol regression-contract anchor drifted: {count}")
patched = patched.replace(protocol_contract, contract_extension, 1)

release_audit_anchor = '''# Release source audit: version bump + explicit 0.58 contracts.'''
content_contract_patch = '''replace_once("tools/test_content_pack_source.py",\n\'\'\'    for forbidden in ("biomesoplenty", "tbos", "amethyst_resonance"):\n        require(forbidden not in compat.lower(), f"hard optional-mod dependency leaked into compatibility seam: {forbidden}")\n        require(forbidden not in affix.lower(), f"hard optional-mod dependency leaked into equipment imprint: {forbidden}")\n        require(forbidden not in reforge.lower(), f"hard optional-mod dependency leaked into equipment service: {forbidden}")\n\'\'\',\n\'\'\'    for forbidden in ("biomesoplenty", "amethyst_resonance"):\n        require(forbidden not in compat.lower(), f"hard optional-mod dependency leaked into compatibility seam: {forbidden}")\n    require("com.nightbeam" not in compat.lower(), "TBS implementation class dependency leaked into compatibility seam")\n    for forbidden in ("biomesoplenty", "tbos", "amethyst_resonance"):\n        require(forbidden not in affix.lower(), f"hard optional-mod dependency leaked into equipment imprint: {forbidden}")\n        require(forbidden not in reforge.lower(), f"hard optional-mod dependency leaked into equipment service: {forbidden}")\n\'\'\')\n\n'''
count = patched.count(release_audit_anchor)
if count != 1:
    raise RuntimeError(f"release audit insertion anchor drifted: {count}")
patched = patched.replace(release_audit_anchor, content_contract_patch + release_audit_anchor, 1)

namespace = {"__file__": str(path), "__name__": "__main__"}
exec(compile(patched, str(path), "exec"), namespace)
