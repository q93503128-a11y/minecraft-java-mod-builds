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

if LOCK.get('status') != 'candidate_runtime_lock':
    raise SystemExit('companion lock must remain candidate_runtime_lock before real runtime acceptance')
if LOCK.get('target', {}).get('frontier_settlement') != '0.1.0-alpha.73':
    raise SystemExit('companion target must match Alpha.73')

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

dt = SOURCES['sources']['dungeons_and_taverns']
if dt.get('file_id') != 8262693:
    raise SystemExit('Dungeons and Taverns file id drifted')
if dt.get('sha256') != '2ca47414352ef2fbbbdb61af678e2a0bc3facb6093262f4caac43e35e8022d9b':
    raise SystemExit('Dungeons and Taverns canonical SHA-256 drifted')
client_dt = next(f for f in CLIENT['files'] if f['id'] == 'dungeons_and_taverns')
if client_dt['sha256'] != dt['sha256']:
    raise SystemExit('Dungeons and Taverns runtime source/hash lock mismatch')

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

print('Frontier Settlement companion testpack static audit: PASS')
