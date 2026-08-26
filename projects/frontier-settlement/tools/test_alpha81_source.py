#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
CLIENT = JAVA / 'client'
NETWORK = JAVA / 'network'
A80 = ROOT / 'tools/test_alpha80_source.py'
_real_read = Path.read_text

def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.81', 'mod_version=0.1.0-alpha.80')
        s = s.replace(', plus Alpha.81 first-run UI, category-first construction palette, in-game guide, pre-founding HUD guidance, and Korean companion language overlays.', '.')
    elif self.name == 'SettlementNetwork.java':
        s = s.replace('private static final String PROTOCOL = "8";', 'private static final String PROTOCOL = "7";')
    return s

Path.read_text = legacy_read
try:
    chain = _real_read(A80, encoding='utf-8').replace(
        "print('Frontier Settlement alpha.23-80 cumulative source audit: PASS')", 'pass')
    ns = {'__file__': str(A80), '__name__': '__main__'}
    exec(compile(chain, str(A80), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read

def text(path):
    return Path(path).read_text(encoding='utf-8')

def must(src, tokens, label):
    for token in tokens:
        if token not in src:
            raise SystemExit(f'{label} missing: {token}')

props = text(ROOT / 'gradle.properties')
network = text(NETWORK / 'SettlementNetwork.java')
payload = text(NETWORK / 'FoundSettlementRequestPayload.java')
placement = text(CLIENT / 'BuildingPlacementClient.java')
start = text(CLIENT / 'SettlementStartScreen.java')
guide = text(CLIENT / 'SettlementGuideScreen.java')
palette = text(CLIENT / 'BuildingPaletteScreen.java')
hud = text(CLIENT / 'SettlementHudOverlay.java')

must(props, ('mod_version=0.1.0-alpha.81', 'Alpha.81 first-run UI', 'Korean companion language overlays'), 'alpha.81 props')
must(network, (
    'private static final String PROTOCOL = "8";',
    'registrar.playToServer(FoundSettlementRequestPayload.TYPE',
    'SettlementNetwork::handleFoundSettlementRequest',
    'SettlementService.foundAt(player, player.blockPosition())',
), 'alpha.81 founding network')
must(payload, ('found_settlement_request', 'writeBoolean(payload.confirm())', 'readBoolean()'), 'alpha.81 founding payload')
must(placement, (
    'if (ClientSettlementState.snapshot().founded()) minecraft.gui.setScreen(new BuildingPaletteScreen());',
    'else minecraft.gui.setScreen(new SettlementStartScreen());',
), 'alpha.81 B first-run routing')
must(start, ('현재 위치에 개척지 세우기', 'FoundSettlementRequestPayload(true)', '시작 방법', '공동 창고'), 'alpha.81 start screen')
must(guide, ('PAGE_COUNT = 4', '주택 → 벌목소 → 농장 → 채석장 → 창고', '도로 계획', '언로드 지역은 강제로 로드하지 않으며'), 'alpha.81 guide')
must(palette, (
    'FOUNDATION("기반"', 'PRODUCTION("생산"', 'SERVICES("제작·서비스"', 'DEFENSE("방어"', 'INFRA("인프라"',
    'Component.literal("가이드")', '건설 가능', '자원 부족',
), 'alpha.81 category palette')
must(hud, ('공동 개척지 없음 · B를 눌러 시작', 'Frontier Settlement'), 'alpha.81 pre-founding HUD')

langs = {
    'weaponsexpanded': ROOT / 'src/main/resources/assets/weaponsexpanded/lang/ko_kr.json',
    'variantsandventures': ROOT / 'src/main/resources/assets/variantsandventures/lang/ko_kr.json',
    'repurposed_structures': ROOT / 'src/main/resources/assets/repurposed_structures/lang/ko_kr.json',
    'yet_another_config_lib_v3': ROOT / 'src/main/resources/assets/yet_another_config_lib_v3/lang/ko_kr.json',
}
minimum = {'weaponsexpanded': 110, 'variantsandventures': 115, 'repurposed_structures': 150, 'yet_another_config_lib_v3': 25}
for namespace, path in langs.items():
    data = json.loads(text(path))
    if len(data) < minimum[namespace]:
        raise SystemExit(f'alpha.81 Korean overlay unexpectedly sparse: {namespace} {len(data)}')
    for key, value in data.items():
        if not isinstance(key, str) or not isinstance(value, str) or not value.strip():
            raise SystemExit(f'alpha.81 invalid language entry: {namespace}:{key}')

weapon = json.loads(text(langs['weaponsexpanded']))
if weapon.get('item.weaponsexpanded.iron_katana') != '철 카타나':
    raise SystemExit('alpha.81 Weapons Expanded key coverage mismatch')
vv = json.loads(text(langs['variantsandventures']))
if vv.get('entity.variantsandventures.gelid') != '젤리드':
    raise SystemExit('alpha.81 Variants & Ventures key coverage mismatch')
rs = json.loads(text(langs['repurposed_structures']))
if rs.get('structure.repurposed_structures.village_cherry') != '벚나무 마을':
    raise SystemExit('alpha.81 Repurposed Structures key coverage mismatch')
yacl = json.loads(text(langs['yet_another_config_lib_v3']))
if yacl.get('yacl.gui.save') != '변경 사항 저장':
    raise SystemExit('alpha.81 YACL key coverage mismatch')

# Translation overlays are resource-only. Frontier must not turn optional/candidate companions into Java dependencies.
for path in JAVA.rglob('*.java'):
    src = text(path)
    for forbidden in (
        'import com.github.fabriciuss',
        'import net.blay09.mods.balm',
        'import snownee.jade',
        'import xaero.',
        'import dev.isxander.yacl',
        'import com.faboslav',
        'import net.p3pp3rf1y',
    ):
        if forbidden in src:
            raise SystemExit(f'alpha.81 hard companion Java dependency: {path.relative_to(ROOT)} -> {forbidden}')

print('Frontier Settlement alpha.23-81 cumulative source audit: PASS')
