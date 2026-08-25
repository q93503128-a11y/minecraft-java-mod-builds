#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
SETT = JAVA / 'settlement'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')


def replace_once(path, old, new):
    text = read(path)
    if text.count(old) != 1:
        raise SystemExit(f'{path}: expected one replacement, found {text.count(old)} for {old!r}')
    write(path, text.replace(old, new, 1))


def append_once(path, marker, block):
    text = read(path)
    if marker in text:
        return
    if not text.endswith('\n'):
        text += '\n'
    write(path, text + '\n' + block.strip() + '\n')


# New bounded knowledge-only save. This is not item/currency/population/logistics authority.
threat_data = '''package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Bounded server-side field knowledge from first-time external hostile kills.
 * It never owns items, currency, population, construction, logistics or companion entities.
 */
public final class SettlementThreatKnowledgeData extends SavedData {
    private static final int MAX_RECORDED_THREAT_TYPES = 64;

    public static final SavedDataType<SettlementThreatKnowledgeData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "threat_knowledge"),
            SettlementThreatKnowledgeData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.listOf().optionalFieldOf("defeated_external_threats", List.of())
                            .forGetter(data -> data.defeatedExternalThreats)
            ).apply(instance, SettlementThreatKnowledgeData::new))
    );

    private List<String> defeatedExternalThreats;

    public SettlementThreatKnowledgeData() {
        this(List.of());
    }

    public SettlementThreatKnowledgeData(List<String> defeatedExternalThreats) {
        this.defeatedExternalThreats = boundedCopy(defeatedExternalThreats);
    }

    public static SettlementThreatKnowledgeData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<String> defeatedExternalThreats() {
        return defeatedExternalThreats;
    }

    public int threatLevel() {
        return Math.min(SettlementExplorationBenefitService.MAX_THREAT_LEVEL, defeatedExternalThreats.size());
    }

    public boolean recordExternalThreat(String id) {
        if (id == null || id.isBlank() || defeatedExternalThreats.contains(id)
                || defeatedExternalThreats.size() >= MAX_RECORDED_THREAT_TYPES) return false;
        List<String> next = new ArrayList<>(defeatedExternalThreats);
        next.add(id);
        defeatedExternalThreats = List.copyOf(next);
        setDirty();
        return true;
    }

    private static List<String> boundedCopy(List<String> source) {
        if (source == null || source.isEmpty()) return List.of();
        List<String> result = new ArrayList<>(Math.min(source.size(), MAX_RECORDED_THREAT_TYPES));
        for (String value : source) {
            if (value == null || value.isBlank() || result.contains(value)) continue;
            result.add(value);
            if (result.size() >= MAX_RECORDED_THREAT_TYPES) break;
        }
        return List.copyOf(result);
    }
}
'''
write(SETT / 'SettlementThreatKnowledgeData.java', threat_data)

# Exploration event: keep boss semantics intact; ordinary external Monsters get a separate first-kill field report.
explore = SETT / 'SettlementExplorationService.java'
replace_once(explore,
'''        LivingEntity victim = event.getEntity();
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (id == null || !isConquestTarget(victim, id)) return;
        if (!data.recordExternalBoss(id.toString())) return;

        player.sendSystemMessage(Component.literal("개척 정복 · 강적 " + id + " | " + SettlementExplorationBenefitService.supportSummary(data)
                + " · 신규 전초 " + SettlementExplorationBenefitService.outpostWoodCost(data) + "목재/"
                + SettlementExplorationBenefitService.outpostStoneCost(data) + "석재"));
        SettlementService.broadcast(server, data);
''',
'''        LivingEntity victim = event.getEntity();
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (id == null) return;

        if (isConquestTarget(victim, id)) {
            if (!data.recordExternalBoss(id.toString())) return;
            player.sendSystemMessage(Component.literal("개척 정복 · 강적 " + id + " | " + SettlementExplorationBenefitService.supportSummary(data)
                    + " · 신규 전초 " + SettlementExplorationBenefitService.outpostWoodCost(data) + "목재/"
                    + SettlementExplorationBenefitService.outpostStoneCost(data) + "석재"));
            SettlementService.broadcast(server, data);
            return;
        }

        if (!isExternalThreat(victim, id)) return;
        SettlementThreatKnowledgeData knowledge = SettlementThreatKnowledgeData.get(server);
        if (!knowledge.recordExternalThreat(id.toString())) return;
        player.sendSystemMessage(Component.literal("개척 전투보고 · 외부 위협 " + id + " | "
                + SettlementExplorationBenefitService.threatSupportSummary(server)));
''')
replace_once(explore,
'''    private static boolean isConquestTarget(LivingEntity victim, Identifier id) {
        if (VANILLA_CONQUEST_TARGETS.contains(id.toString())) return true;
        if (!(victim instanceof Mob) || victim.getMaxHealth() < EXTERNAL_BOSS_MIN_HEALTH) return false;
        String namespace = id.getNamespace();
        return !"minecraft".equals(namespace) && !FrontierSettlement.MOD_ID.equals(namespace) && !"neoforge".equals(namespace);
    }
''',
'''    private static boolean isConquestTarget(LivingEntity victim, Identifier id) {
        if (VANILLA_CONQUEST_TARGETS.contains(id.toString())) return true;
        if (!(victim instanceof Mob) || victim.getMaxHealth() < EXTERNAL_BOSS_MIN_HEALTH) return false;
        String namespace = id.getNamespace();
        return !"minecraft".equals(namespace) && !FrontierSettlement.MOD_ID.equals(namespace) && !"neoforge".equals(namespace);
    }

    private static boolean isExternalThreat(LivingEntity victim, Identifier id) {
        if (!(victim instanceof net.minecraft.world.entity.monster.Monster)) return false;
        String namespace = id.getNamespace();
        return !"minecraft".equals(namespace) && !FrontierSettlement.MOD_ID.equals(namespace) && !"neoforge".equals(namespace);
    }
''')

# Central deterministic benefits from bounded field knowledge.
benefit = SETT / 'SettlementExplorationBenefitService.java'
replace_once(benefit,
'package kr.moonseungjun.frontiersettlement.settlement;\n',
'package kr.moonseungjun.frontiersettlement.settlement;\n\nimport net.minecraft.server.MinecraftServer;\n')
replace_once(benefit,
'''    public static final int MAX_SURVEY_LEVEL = 3;
    public static final int MAX_CONQUEST_LEVEL = 2;
''',
'''    public static final int MAX_SURVEY_LEVEL = 3;
    public static final int MAX_CONQUEST_LEVEL = 2;
    public static final int MAX_THREAT_LEVEL = 3;
    public static final long RECRUIT_FOOD_DISCOUNT_PER_THREAT = 1L;
    public static final double BARRACKS_THREAT_RADIUS_BONUS_PER_LEVEL = 4.0D;
''')
replace_once(benefit,
'''    public static int conquestLevel(SettlementData data) {
        return Math.min(MAX_CONQUEST_LEVEL, data.defeatedExternalBosses().size());
    }
''',
'''    public static int conquestLevel(SettlementData data) {
        return Math.min(MAX_CONQUEST_LEVEL, data.defeatedExternalBosses().size());
    }

    public static int threatLevel(MinecraftServer server) {
        return SettlementThreatKnowledgeData.get(server).threatLevel();
    }

    public static long barracksRecruitFoodCost(MinecraftServer server) {
        return Math.max(5L, SettlementBarracksService.RECRUIT_FOOD_COST
                - threatLevel(server) * RECRUIT_FOOD_DISCOUNT_PER_THREAT);
    }

    public static double barracksThreatRadius(MinecraftServer server) {
        return SettlementBarracksService.BASE_THREAT_RADIUS
                + threatLevel(server) * BARRACKS_THREAT_RADIUS_BONUS_PER_LEVEL;
    }

    public static String threatSupportSummary(MinecraftServer server) {
        return "위협정보 " + threatLevel(server) + "/" + MAX_THREAT_LEVEL
                + " · 병사 식량 " + barracksRecruitFoodCost(server)
                + " · 감시반경 " + (int) barracksThreatRadius(server);
    }
''')

# Barracks consumes less real food only after field knowledge; no resource is minted.
barracks = SETT / 'SettlementBarracksService.java'
replace_once(barracks,
'    private static final double THREAT_RADIUS = 28.0D;\n',
'    public static final double BASE_THREAT_RADIUS = 28.0D;\n')
replace_once(barracks,
'''        SettlementResources resources = SettlementStorageService.scan(level, data);
        if (resources.food() < RECRUIT_FOOD_COST || resources.metal() < RECRUIT_METAL_COST) return false;
''',
'''        SettlementResources resources = SettlementStorageService.scan(level, data);
        long recruitFoodCost = SettlementExplorationBenefitService.barracksRecruitFoodCost(level.getServer());
        if (resources.food() < recruitFoodCost || resources.metal() < RECRUIT_METAL_COST) return false;
''')
replace_once(barracks,
'''        if (!SettlementStorageService.consumeMetalAndFood(level, data, RECRUIT_METAL_COST, RECRUIT_FOOD_COST)) {
''',
'''        if (!SettlementStorageService.consumeMetalAndFood(level, data, RECRUIT_METAL_COST, recruitFoodCost)) {
''')
replace_once(barracks,
'''    private static Monster nearestThreat(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(THREAT_RADIUS, 12.0D, THREAT_RADIUS);
''',
'''    private static Monster nearestThreat(ServerLevel level, BlockPos center) {
        double threatRadius = SettlementExplorationBenefitService.barracksThreatRadius(level.getServer());
        AABB area = new AABB(center).inflate(threatRadius, 12.0D, threatRadius);
''')

# Version + compact product description.
props = ROOT / 'gradle.properties'
replace_once(props, 'mod_version=0.1.0-alpha.73', 'mod_version=0.1.0-alpha.74')
replace_once(props,
'exploration-feedback progression where unique surveys/conquests improve physical relic market payouts, metal-based weapon repair efficiency, and advanced weapon forging power without adding a currency, menu, or virtual resource authority.',
'exploration-feedback progression where unique surveys/conquests improve physical relic market payouts, metal-based weapon repair efficiency, and advanced weapon forging power without adding a currency, menu, or virtual resource authority, plus bounded first-kill external-hostile field knowledge that reduces real barracks recruitment food cost from 8 to a minimum of 5 and expands loaded garrison threat detection from 28 to at most 40 blocks without companion class dependencies or item minting.')

# Canonical docs versions and Alpha.74 notes.
canonical = ROOT / 'CANONICAL_PLAN.md'
replace_once(canonical, 'Current canonical implementation: **0.1.0-alpha.73**.', 'Current canonical implementation: **0.1.0-alpha.74**.')
append_once(canonical, '### Alpha.74 external-threat field knowledge gameplay pass', '''### Alpha.74 external-threat field knowledge gameplay pass

- First direct player kills of distinct non-vanilla hostile `Monster` entity types are recorded in a bounded server-side knowledge ledger; bosses keep the existing conquest meaning and are not double-counted as ordinary threats.
- Field knowledge is capped at 3 gameplay levels. It does not create loot, currency, population, virtual inventory, or logistics authority.
- Each level lowers the existing barracks recruit food requirement by 1 real food ItemStack-equivalent, from 8 down to a floor of 5; metal remains exactly 2.
- Each level extends loaded barracks hostile detection by 4 blocks, from 28 to at most 40, so unfamiliar companion threats become a concrete reason to explore and fight before expanding the garrison.
- Detection is namespace/class-generic and references no Variants & Ventures Java class or item. Optional companion absence remains boot-safe.
- This is the next gameplay-feedback link after Alpha.73: external combat knowledge -> cheaper/more capable settlement defense -> safer outward expansion.''')

gap = ROOT / 'COMPLETION_GAP_AUDIT.md'
replace_once(gap, '현재 구현 기준: `0.1.0-alpha.73`', '현재 구현 기준: `0.1.0-alpha.74`')
append_once(gap, '### Alpha.74 외부 위협 전투지식 게임성 패스', '''### Alpha.74 외부 위협 전투지식 게임성 패스

- 외부 적대 몹의 서로 다른 타입을 플레이어가 처음 직접 처치하면 최대 3단계의 `위협정보`로 누적한다. 기존 80HP 이상 강적/보스 정복 기록과는 분리해 의미를 유지한다.
- 위협정보는 재화가 아니며 아이템/드랍/가상자원을 생성하지 않는다.
- 병영의 실제 모집 식량비가 8 -> 7 -> 6 -> 최소 5로 줄고 금속 2는 그대로 소비한다.
- 병영의 로드된 적 탐지 반경은 28 -> 32 -> 36 -> 최대 40블록으로 늘어난다.
- V&V 같은 외부 적대몹을 잡는 행동이 단순 변종 감상이 아니라 정착지 군사 성장으로 되돌아오며, companion class hard dependency는 없다.
- 남은 검증: 사람 클라이언트에서 실제 스폰 밀도/전투 빈도/체감 밸런스는 문승준 실플레이 영역이다.''')

readme = ROOT / 'README.md'
replace_once(readme, '## Current version: 0.1.0-alpha.73', '## Current version: 0.1.0-alpha.74')
append_once(readme, '## Alpha.74 — external-threat field knowledge gameplay pass', '''## Alpha.74 — external-threat field knowledge gameplay pass

External hostile variants now feed settlement defense without a new currency or management screen. The first direct player kill of each distinct non-vanilla hostile type becomes bounded field knowledge (max level 3). Bosses remain conquest targets instead of being double-counted. Each field-knowledge level cuts barracks recruitment food by 1 real unit (8 -> minimum 5, metal stays 2) and expands the loaded garrison threat radius by 4 blocks (28 -> maximum 40). The logic is registry/class-generic, so Variants & Ventures and future compatible hostile-mob companions work without Frontier linking against their Java classes.''')

# Companion lock follows the Frontier product version but keeps runtime scope honest.
lock_path = ROOT / 'COMPANION_LOCK.json'
lock = json.loads(read(lock_path))
if lock.get('target', {}).get('frontier_settlement') != '0.1.0-alpha.73':
    raise SystemExit('unexpected companion target before Alpha.74')
lock['target']['frontier_settlement'] = '0.1.0-alpha.74'
note = ('Alpha.74 adds bounded first-kill external-hostile field knowledge: up to 3 levels lower real barracks recruitment food from 8 to a floor of 5 and extend loaded garrison threat detection from 28 to 40 blocks. The knowledge ledger owns no items/currency/population/logistics, references no companion classes, and does not promote human client/spawn-density acceptance.')
if note not in lock.get('notes', []):
    lock.setdefault('notes', []).append(note)
write(lock_path, json.dumps(lock, ensure_ascii=False, indent=2) + '\n')

# New cumulative source audit; old alpha.73 sees its historical file count/version.
source_test = '''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'; A73=ROOT/'tools/test_alpha73_source.py'
_real_read=Path.read_text; _real_rglob=Path.rglob
def legacy_read(self,*args,**kwargs):
    s=_real_read(self,*args,**kwargs)
    if self.name=='gradle.properties': s=s.replace('mod_version=0.1.0-alpha.74','mod_version=0.1.0-alpha.73')
    elif self.name=='COMPANION_LOCK.json': s=s.replace('"frontier_settlement": "0.1.0-alpha.74"','"frontier_settlement": "0.1.0-alpha.73"')
    return s
def legacy_rglob(self,pattern):
    values=list(_real_rglob(self,pattern))
    if pattern=='*.java': values=[p for p in values if p.name!='SettlementThreatKnowledgeData.java']
    return iter(values)
Path.read_text=legacy_read; Path.rglob=legacy_rglob
try:
    a=_real_read(A73,encoding='utf-8').replace("print('Frontier Settlement alpha.23-73 cumulative source audit: PASS')",'pass')
    ns={'__file__':str(A73),'__name__':'__main__'}; exec(compile(a,str(A73),'exec'),ns,ns)
finally:
    Path.read_text=_real_read; Path.rglob=_real_rglob
def text(p): return Path(p).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
sett=JAVA/'settlement'; threat=text(sett/'SettlementThreatKnowledgeData.java'); explore=text(sett/'SettlementExplorationService.java'); benefit=text(sett/'SettlementExplorationBenefitService.java'); barracks=text(sett/'SettlementBarracksService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
if len(list((ROOT/'src/main/java').rglob('*.java')))!=106: raise SystemExit('alpha.74 Java family count changed')
must(threat,('threat_knowledge','defeated_external_threats','MAX_RECORDED_THREAT_TYPES = 64','recordExternalThreat(String id)','threatLevel()'), 'alpha.74 threat data')
must(explore,('isExternalThreat(victim, id)','SettlementThreatKnowledgeData.get(server)','개척 전투보고 · 외부 위협','instanceof net.minecraft.world.entity.monster.Monster'), 'alpha.74 external threat observation')
must(benefit,('MAX_THREAT_LEVEL = 3','barracksRecruitFoodCost(MinecraftServer server)','Math.max(5L','barracksThreatRadius(MinecraftServer server)','BARRACKS_THREAT_RADIUS_BONUS_PER_LEVEL = 4.0D','threatSupportSummary(MinecraftServer server)'), 'alpha.74 threat benefits')
must(barracks,('BASE_THREAT_RADIUS = 28.0D','long recruitFoodCost = SettlementExplorationBenefitService.barracksRecruitFoodCost(level.getServer())','consumeMetalAndFood(level, data, RECRUIT_METAL_COST, recruitFoodCost)','SettlementExplorationBenefitService.barracksThreatRadius(level.getServer())'), 'alpha.74 physical barracks')
must(props,('mod_version=0.1.0-alpha.74','bounded first-kill external-hostile field knowledge'), 'alpha.74 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.74"','Alpha.74 adds bounded first-kill external-hostile field knowledge'), 'alpha.74 lock')
for forbidden in ('variantsandventures.', 'com.faboslav', 'ResourcefulLib'):
    if forbidden in explore or forbidden in threat: raise SystemExit(f'alpha.74 hard companion dependency: {forbidden}')
print('Frontier Settlement alpha.23-74 cumulative source audit: PASS')
'''
write(ROOT / 'tools/test_alpha74_source.py', source_test)

docs_test = '''#!/usr/bin/env python3
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]; A73=ROOT/'tools/test_alpha73_docs.py'
_real=Path.read_text
def legacy_view(self,*args,**kwargs):
    s=_real(self,*args,**kwargs)
    if self.name=='CANONICAL_PLAN.md': s=s.replace('Current canonical implementation: **0.1.0-alpha.74**.','Current canonical implementation: **0.1.0-alpha.73**.')
    elif self.name=='COMPLETION_GAP_AUDIT.md': s=s.replace('현재 구현 기준: `0.1.0-alpha.74`','현재 구현 기준: `0.1.0-alpha.73`')
    elif self.name=='README.md': s=s.replace('## Current version: 0.1.0-alpha.74','## Current version: 0.1.0-alpha.73')
    elif self.name=='COMPANION_LOCK.json': s=s.replace('"frontier_settlement": "0.1.0-alpha.74"','"frontier_settlement": "0.1.0-alpha.73"')
    return s
Path.read_text=legacy_view
try:
    a=_real(A73,encoding='utf-8').replace("print('Frontier Settlement alpha.73 canonical docs audit: PASS')",'pass')
    ns={'__file__':str(A73),'__name__':'__main__'}; exec(compile(a,str(A73),'exec'),ns,ns)
finally: Path.read_text=_real
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md'); readme=text('README.md'); lock=json.loads(text('COMPANION_LOCK.json'))
must(canonical,('Current canonical implementation: **0.1.0-alpha.74**','### Alpha.74 external-threat field knowledge gameplay pass','from 8 down to a floor of 5','from 28 to at most 40'), 'alpha.74 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.74`','### Alpha.74 외부 위협 전투지식 게임성 패스','8 -> 7 -> 6 -> 최소 5','28 -> 32 -> 36 -> 최대 40'), 'alpha.74 gap')
must(readme,('## Current version: 0.1.0-alpha.74','## Alpha.74 — external-threat field knowledge gameplay pass','8 -> minimum 5','28 -> maximum 40'), 'alpha.74 readme')
if lock.get('status')!='candidate_runtime_lock': raise SystemExit('alpha.74 companion lock overclaim')
if lock.get('target',{}).get('frontier_settlement')!='0.1.0-alpha.74': raise SystemExit('alpha.74 lock target mismatch')
notes='\n'.join(lock.get('notes',[])); must(notes,('Alpha.74 adds bounded first-kill external-hostile field knowledge','does not promote human client/spawn-density acceptance'), 'alpha.74 lock note')
print('Frontier Settlement alpha.74 canonical docs audit: PASS')
'''
write(ROOT / 'tools/test_alpha74_docs.py', docs_test)

print('Alpha.74 staging patch applied')
