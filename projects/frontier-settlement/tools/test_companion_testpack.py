#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / 'companion-testpack'
LOCK = json.loads((ROOT / 'COMPANION_LOCK.json').read_text(encoding='utf-8'))
SOURCES = json.loads((PACK / 'runtime-sources.json').read_text(encoding='utf-8'))
CLIENT = json.loads((PACK / 'resolved-lock.client.json').read_text(encoding='utf-8'))
SERVER = json.loads((PACK / 'resolved-lock.server.json').read_text(encoding='utf-8'))
INSTALLER = (PACK / 'install.py').read_text(encoding='utf-8')
README = (PACK / 'README.md').read_text(encoding='utf-8')


def read_gradle_properties() -> dict[str, str]:
    values: dict[str, str] = {}
    for raw in (ROOT / 'gradle.properties').read_text(encoding='utf-8').splitlines():
        line = raw.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        key, value = line.split('=', 1)
        values[key.strip()] = value.strip()
    return values


CURRENT_VERSION = read_gradle_properties().get('mod_version', '')
if not CURRENT_VERSION:
    raise SystemExit('gradle.properties is missing mod_version')
if LOCK.get('status') != 'candidate_runtime_lock':
    raise SystemExit('companion lock must remain candidate_runtime_lock before real runtime acceptance')
if LOCK.get('target', {}).get('frontier_settlement') != CURRENT_VERSION:
    raise SystemExit(f'companion target must match current Frontier version {CURRENT_VERSION}')

required = [e for e in LOCK.get('entries', []) if e.get('required')]
expected_client = {
    'terralith', 'lithostitched', 'dungeons_and_taverns', 'repurposed_structures',
    'better_combat', 'cloth_config', 'player_animation_library', 'weapons_expanded',
    'lootr', 'sophisticated_backpacks', 'sophisticated_core', 'jade',
    'variants_and_ventures', 'resourceful_lib', 'yacl', 'xaeros_minimap'
}
expected_server = expected_client - {'xaeros_minimap'}
if {e['id'] for e in required} != expected_client or len(required) != 16:
    raise SystemExit('required companion set drifted')

for resolved, profile, expected in ((CLIENT, 'client', expected_client), (SERVER, 'server', expected_server)):
    if resolved.get('profile') != profile or not resolved.get('resolution', {}).get('verified'):
        raise SystemExit(f'{profile} resolved lock is not verified')
    if resolved.get('target', {}).get('frontier_settlement') != CURRENT_VERSION:
        raise SystemExit(f'{profile} resolved target must match current Frontier version {CURRENT_VERSION}')
    files = resolved.get('files', [])
    if {f['id'] for f in files} != expected:
        raise SystemExit(f'{profile} resolved set drifted')
    for f in files:
        for algorithm, length in (('sha1', 40), ('sha256', 64), ('sha512', 128)):
            value = f.get(algorithm, '').lower()
            if len(value) != length or any(c not in '0123456789abcdef' for c in value):
                raise SystemExit(f"{profile}/{f['id']}: invalid {algorithm}")

server_by_id = {f['id']: f for f in SERVER['files']}
for f in CLIENT['files']:
    other = server_by_id.get(f['id'])
    if other:
        for key in ('source', 'version', 'filename', 'sha1', 'sha256', 'sha512'):
            if f.get(key) != other.get(key):
                raise SystemExit(f"{f['id']}: client/server {key} mismatch")

if any(f['id'] == 'xaeros_minimap' for f in SERVER['files']):
    raise SystemExit('Xaero client-preferred binary must not be in server lock')

for mod_id in ('variants_and_ventures', 'resourceful_lib', 'yacl'):
    cf = next(f for f in CLIENT['files'] if f['id'] == mod_id)
    sf = next(f for f in SERVER['files'] if f['id'] == mod_id)
    if cf['sha256'] != sf['sha256']:
        raise SystemExit(f'{mod_id}: client/server resolved binary mismatch')

# Dungeons and Taverns deliberately moved its auto-install authority from the old CurseForge
# numeric file endpoint to the official Modrinth 5.3.0 version. The binary itself is unchanged:
# all three committed hashes must match the already verified client/server lock.
dt = SOURCES['sources']['dungeons_and_taverns']
if dt.get('source') != 'modrinth' or dt.get('project_id') != 'tpehi7ww' or dt.get('version_id') != 'UP9sRfQF':
    raise SystemExit('Dungeons and Taverns canonical Modrinth source/version drifted')
if dt.get('version') != '5.3.0' or dt.get('loader') != 'neoforge' or dt.get('minecraft') != '26.2':
    raise SystemExit('Dungeons and Taverns runtime compatibility target drifted')
expected_dt_hashes = {
    'sha1': '64e6b6e2b3fdb948d49cdc7aea23c1b693fd5156',
    'sha256': '2ca47414352ef2fbbbdb61af678e2a0bc3facb6093262f4caac43e35e8022d9b',
    'sha512': 'c4b6e6e5be6fa77f9d12d584a10fd541377a75ec926aba12e55d360518425aadf8832e1e9eb5a774a7388bd340879ab79ddd115b9e5263e6f44d23c021f9a111',
}
for algorithm, expected_hash in expected_dt_hashes.items():
    if dt.get(algorithm) != expected_hash:
        raise SystemExit(f'Dungeons and Taverns canonical {algorithm.upper()} drifted')
client_dt = next(f for f in CLIENT['files'] if f['id'] == 'dungeons_and_taverns')
server_dt = next(f for f in SERVER['files'] if f['id'] == 'dungeons_and_taverns')
for algorithm in ('sha1', 'sha256', 'sha512'):
    if client_dt.get(algorithm) != dt.get(algorithm) or server_dt.get(algorithm) != dt.get(algorithm):
        raise SystemExit(f'Dungeons and Taverns runtime source/{algorithm} lock mismatch')

for token in (
    'PINNED_RESOLVED_PATH', 'apply_committed_pin',
    'same locked version must match committed resolved-lock.client.json hashes when present',
    'if not data.startswith(b"PK")', '--resolve-only', 'candidate_runtime_lock',
):
    if token not in INSTALLER:
        raise SystemExit(f'installer missing pin/runtime invariant: {token}')
if 'base64.b64decode' in INSTALLER or 'bytes.fromhex' in INSTALLER:
    raise SystemExit('installer must not embed third-party binary payloads')
if list(PACK.rglob('*.jar')):
    raise SystemExit('third-party JARs must not be committed under companion-testpack')

if 'Third-party JARs are fetched directly from their official distribution URLs' not in README:
    raise SystemExit('companion README must state official direct fetching')
if 'Variants & Ventures' not in README or "Alex's Mobs Continued" not in README:
    raise SystemExit('promoted/deferred companion content must remain documented')

print(f'Frontier Settlement {CURRENT_VERSION} companion testpack static audit: PASS')
