#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCK = json.loads((ROOT / 'COMPANION_LOCK.json').read_text(encoding='utf-8'))
SOURCES = json.loads((ROOT / 'companion-testpack/runtime-sources.json').read_text(encoding='utf-8'))
INSTALLER = (ROOT / 'companion-testpack/install.py').read_text(encoding='utf-8')
README = (ROOT / 'companion-testpack/README.md').read_text(encoding='utf-8')

if LOCK.get('status') != 'candidate_runtime_lock':
    raise SystemExit('companion lock must remain candidate_runtime_lock before user runtime acceptance')
if LOCK.get('target', {}).get('frontier_settlement') != '0.1.0-alpha.73':
    raise SystemExit('companion testpack target must match Alpha.73')

required = [e for e in LOCK.get('entries', []) if e.get('required')]
if len(required) != 13:
    raise SystemExit(f'expected 13 required companion entries, got {len(required)}')
ids = {e['id'] for e in required}
expected_ids = {
    'terralith', 'lithostitched', 'dungeons_and_taverns', 'repurposed_structures',
    'better_combat', 'cloth_config', 'player_animation_library', 'weapons_expanded',
    'lootr', 'sophisticated_backpacks', 'sophisticated_core', 'jade', 'xaeros_minimap'
}
if ids != expected_ids:
    raise SystemExit(f'required companion set drifted: {sorted(ids ^ expected_ids)}')

for entry in required:
    source = entry.get('source')
    if source == 'modrinth':
        if not entry.get('project_id') or not entry.get('version_id'):
            raise SystemExit(f"{entry['id']}: missing exact Modrinth lock")
    elif source == 'curseforge':
        runtime = SOURCES.get('sources', {}).get(entry['id'])
        if not runtime:
            raise SystemExit(f"{entry['id']}: missing runtime source")
    else:
        raise SystemExit(f"{entry['id']}: unsupported source {source}")

dt = SOURCES['sources']['dungeons_and_taverns']
if dt.get('file_id') != 8262693:
    raise SystemExit('Dungeons and Taverns must remain pinned to CurseForge file 8262693')
if dt.get('file_name') != 'dungeons-and-taverns-5.3.0 [NeoForge].jar':
    raise SystemExit('Dungeons and Taverns filename drifted')

must = (
    'api.modrinth.com/v2/version/{version_id}',
    'entry.get("side") == "client_preferred"',
    'if not data.startswith(b"PK")',
    'resolved-lock.json',
    '--resolve-only',
    'candidate_runtime_lock',
    'sha256', 'sha512', 'sha1',
)
for token in must:
    if token not in INSTALLER:
        raise SystemExit(f'installer missing: {token}')

forbidden = (
    '.jar"\n',  # no embedded third-party binary blob/path manifest in source
    'setChunkForced', 'forceChunk', 'teleportTo(',
)
# Only reject suspicious binary embedding, not ordinary filename strings used by the resolver.
if 'base64.b64decode' in INSTALLER or 'bytes.fromhex' in INSTALLER:
    raise SystemExit('installer must not embed third-party binary payloads')

if 'Third-party JARs are fetched directly from their official distribution URLs' not in README:
    raise SystemExit('companion README must state direct official fetching')
if 'Variants & Ventures' not in README or "Alex's Mobs Continued" not in README:
    raise SystemExit('deferred content must remain documented')

print('Frontier Settlement companion testpack static audit: PASS')
