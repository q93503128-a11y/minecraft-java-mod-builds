#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path('projects/frontier-settlement')
PACK = ROOT / 'companion-testpack'

lock_path = ROOT / 'COMPANION_LOCK.json'
lock = json.loads(lock_path.read_text(encoding='utf-8'))
ids = {e['id'] for e in lock['entries']}
if ids & {'variants_and_ventures', 'resourceful_lib', 'yacl'}:
    raise SystemExit('promotion entries already present')

promoted = [
    {
        'id': 'variants_and_ventures', 'name': 'Variants & Ventures', 'side': 'client_server',
        'source': 'modrinth', 'project_id': 'lNDRiXkY', 'version_id': 'e7ZWTYfe',
        'version': '1.0.26+mc26.2', 'loader': 'neoforge', 'required': True,
        'license': 'CC-BY-NC-ND-4.0', 'integration_role': 'dependency',
        'dependencies': ['resourceful_lib', 'yacl'],
        'note': 'Official binary dependency only; Frontier does not copy or modify its code/assets.'
    },
    {
        'id': 'resourceful_lib', 'name': 'Resourceful Lib', 'side': 'client_server_library',
        'source': 'modrinth', 'project_id': 'G1hIVOrD', 'version_id': 'hu7wvfqr',
        'version': '5.0.0', 'loader': 'neoforge', 'required': True,
        'license': 'MIT', 'integration_role': 'dependency_library', 'dependencies': [],
        'note': 'Required by Variants & Ventures; first official 26.2 NeoForge release selected for 26.2.0.38-beta compatibility testing.'
    },
    {
        'id': 'yacl', 'name': 'YetAnotherConfigLib (YACL)', 'side': 'client_server_library',
        'source': 'modrinth', 'project_id': '1eAoo2KR', 'version_id': 'I01Wcg6G',
        'version': '3.9.5+26.2-neoforge', 'loader': 'neoforge', 'required': True,
        'license': 'LGPL-3.0-or-later', 'integration_role': 'dependency_library', 'dependencies': [],
        'note': 'Required by Variants & Ventures; official 26.2 NeoForge build.'
    }
]
idx = next(i for i, e in enumerate(lock['entries']) if e['id'] == 'xaeros_minimap')
lock['entries'][idx:idx] = promoted
lock['deferred'] = [d for d in lock.get('deferred', []) if d.get('id') != 'variants_and_ventures']
lock['notes'][1] = 'The 13-client/12-server baseline passed exact NeoForge 26.2.0.38-beta dedicated-server loading through the normal EULA gate; full client and fresh-world gameplay acceptance remain pending.'
lock['notes'].append('Variants & Ventures 1.0.26 is promoted into the required candidate stack with Resourceful Lib 5.0.0 and YACL 3.9.5; fresh binary resolution and repeated dedicated-server preflight are required before any expanded-stack runtime claim.')
lock_path.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

status = {
    'schema': 1,
    'status': 'baseline_dedicated_server_pre_eula_pass',
    'frontier': {
        'version': '0.1.0-alpha.73',
        'jar_sha256': 'd300d11a8b6fe595f98c574ef9c4f71a86db6bebaaf74522a97b0bffcc7ef43c'
    },
    'minecraft': '26.2',
    'neoforge': '26.2.0.38-beta',
    'baseline': {
        'required_client_entries': 13,
        'required_server_entries': 12,
        'server_payload_jars_including_frontier': 13,
        'preflight_run_id': 32822428896,
        'conclusion': 'success'
    },
    'scope': {
        'eula_accepted': False,
        'fresh_world_generated': False,
        'client_launched': False,
        'spawn_density_validated': False
    },
    'expanded_candidate': {
        'variants_and_ventures_promoted': True,
        'required_client_entries_expected': 16,
        'required_server_entries_expected': 15,
        'server_payload_jars_including_frontier_expected': 16,
        'binary_resolution_verified': False,
        'dedicated_server_preflight_verified': False
    }
}
(PACK / 'runtime-status.json').write_text(json.dumps(status, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

# Static audit: expand the explicit required set. It is run only after freshly staged resolved locks.
test_path = ROOT / 'tools/test_companion_testpack.py'
test = test_path.read_text(encoding='utf-8')
old_ids = "    'lootr', 'sophisticated_backpacks', 'sophisticated_core', 'jade', 'xaeros_minimap'\n"
new_ids = "    'lootr', 'sophisticated_backpacks', 'sophisticated_core', 'jade',\n    'variants_and_ventures', 'resourceful_lib', 'yacl', 'xaeros_minimap'\n"
if old_ids not in test:
    raise SystemExit('test expected-set anchor missing')
test = test.replace(old_ids, new_ids, 1)
test = test.replace('len(required) != 13', 'len(required) != 16', 1)
anchor = "if any(f['id'] == 'xaeros_minimap' for f in SERVER['files']):\n    raise SystemExit('Xaero client-preferred binary must not be in server lock')\n"
extra = anchor + "\nfor mod_id in ('variants_and_ventures', 'resourceful_lib', 'yacl'):\n    cf = next(f for f in CLIENT['files'] if f['id'] == mod_id)\n    sf = next(f for f in SERVER['files'] if f['id'] == mod_id)\n    if cf['sha256'] != sf['sha256']:\n        raise SystemExit(f'{mod_id}: client/server resolved binary mismatch')\n"
if anchor not in test:
    raise SystemExit('test server-lock anchor missing')
test = test.replace(anchor, extra, 1)
test = test.replace("raise SystemExit('deferred content must remain documented')", "raise SystemExit('promoted/deferred companion content must remain documented')", 1)
test_path.write_text(test, encoding='utf-8')

# README: preserve the proven baseline separately from the expanded candidate.
readme_path = PACK / 'README.md'
readme = readme_path.read_text(encoding='utf-8')
old_intro = '`COMPANION_LOCK.json` remains `candidate_runtime_lock`. Exact binary resolution and hash pinning are now verified, but that is **not** the same thing as full client/server gameplay acceptance.'
new_intro = '`COMPANION_LOCK.json` remains `candidate_runtime_lock`. The original 13-client/12-server baseline passed exact NeoForge 26.2.0.38-beta dedicated-server discovery/loading through the normal EULA gate (run `32822428896`), but full client and fresh-world gameplay acceptance are still pending. Variants & Ventures is now promoted into the required candidate stack and must pass fresh 16-client/15-server resolution plus the same server preflight.'
if old_intro not in readme:
    raise SystemExit('README intro anchor missing')
readme = readme.replace(old_intro, new_intro, 1)
old_counts = """- `resolved-lock.client.json` — exactly 13 required companion files.
- `resolved-lock.server.json` — exactly 12 required files; the client-preferred Xaero's Minimap binary is omitted.
- Resolver run `32820788577` proved that a fresh download still matches the committed SHA-1, SHA-256 and SHA-512 pins."""
new_counts = """- `resolved-lock.client.json` — expanded candidate target: exactly 16 required companion files.
- `resolved-lock.server.json` — expanded candidate target: exactly 15 required files; the client-preferred Xaero's Minimap binary is omitted.
- The resolver downloads every official binary and records SHA-1, SHA-256 and SHA-512; same-version hash drift fails closed."""
if old_counts not in readme:
    raise SystemExit('README count anchor missing')
readme = readme.replace(old_counts, new_counts, 1)
old_deferred = """## Deferred content

These remain outside the baseline pack until the required stack is stable in real play:

- Variants & Ventures 1.0.26+mc26.2 — add after baseline spawn-density/compatibility smoke.
- Alex's Mobs Continued — large content gain, but needs a separate stability and spawn-balance pass.
"""
new_deferred = """## Promoted mob-variant candidate

Variants & Ventures 1.0.26+mc26.2 is now required in the candidate pack together with:

- Resourceful Lib 5.0.0 (NeoForge 26.2)
- YACL 3.9.5+26.2-neoforge

All three are fetched from official Modrinth distributions and are not redistributed by Frontier. Variants & Ventures is dependency-only under its CC-BY-NC-ND license: no code/assets are copied or modified in Frontier. Fresh-world spawn-density acceptance remains separate from binary/server-loading compatibility.

## Deferred content

- Alex's Mobs Continued — large content gain, but needs a separate stability and spawn-balance pass.
"""
if old_deferred not in readme:
    raise SystemExit('README deferred anchor missing')
readme = readme.replace(old_deferred, new_deferred, 1)
readme_path.write_text(readme, encoding='utf-8')

# Resolver: old resolved locks cannot be the precondition for generating new ones.
resolver_path = Path('.github/workflows/frontier-companion-resolve.yml')
resolver = resolver_path.read_text(encoding='utf-8')
old_audit = """      - name: Companion manifest/static audit
        working-directory: ${{ env.PROJECT_DIR }}
        run: |
          set -euo pipefail
          python3 tools/test_companion_testpack.py

"""
if old_audit not in resolver:
    raise SystemExit('resolver old audit block missing')
resolver = resolver.replace(old_audit, '', 1)
resolver = resolver.replace("if len(client['files']) != 13:", "if len(client['files']) != 16:", 1)
resolver = resolver.replace("expected 13 required client companions", "expected 16 required client companions", 1)
marker = '      - name: Upload resolved manifests\n'
full_audit = """      - name: Full companion audit against staged hashes
        working-directory: ${{ env.PROJECT_DIR }}
        run: |
          set -euo pipefail
          python3 tools/test_companion_testpack.py

"""
if marker not in resolver:
    raise SystemExit('resolver upload marker missing')
resolver = resolver.replace(marker, full_audit + marker, 1)
resolver_path.write_text(resolver, encoding='utf-8')

# Dedicated server profile now carries 15 third-party JARs + Frontier.
preflight_path = Path('.github/workflows/frontier-companion-server-preflight.yml')
preflight = preflight_path.read_text(encoding='utf-8')
if '-eq 13' not in preflight:
    raise SystemExit('preflight payload count anchor missing')
preflight = preflight.replace('-eq 13', '-eq 16')
if "resolved-lock.client.json" not in preflight:
    preflight = preflight.replace(
        "      - 'projects/frontier-settlement/companion-testpack/resolved-lock.server.json'\n",
        "      - 'projects/frontier-settlement/companion-testpack/resolved-lock.client.json'\n      - 'projects/frontier-settlement/companion-testpack/resolved-lock.server.json'\n"
    )
preflight_path.write_text(preflight, encoding='utf-8')

print('Variants & Ventures candidate staging patch: PASS')
