#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
ALPHA47 = ROOT / 'tools/test_alpha47_source.py'


def text(path): return path.read_text(encoding='utf-8')
def must(source, tokens, label):
    for token in tokens:
        if token not in source: raise SystemExit(f'{label} missing: {token}')
def forbid(source, tokens, label):
    for token in tokens:
        if token in source: raise SystemExit(f'{label}: {token}')

# Preserve Alpha.23-47 runtime/source invariants. Alpha.48 owns the current canonical-doc checks below.
alpha47_source = text(ALPHA47)
alpha47_source = alpha47_source.replace("print('Frontier Settlement alpha.47 source audit: PASS')", 'pass')
alpha47_source = alpha47_source.replace("print('Frontier Settlement alpha.47 canonical docs audit: PASS')", 'pass')
alpha47_source = alpha47_source.replace('0.1.0-alpha.47', '0.1.0-alpha.48')
alpha47_source = alpha47_source.split('# Canonical docs are part of Alpha.47 acceptance.')[0]
namespace = {'__file__': str(ALPHA47), '__name__': '__main__'}
exec(compile(alpha47_source, str(ALPHA47), 'exec'), namespace, namespace)

entity = text(JAVA / 'content/FrontierSoldierEntity.java')
content = text(JAVA / 'content/FrontierContent.java')
renderer = text(JAVA / 'client/FrontierSoldierRenderer.java')
client = text(JAVA / 'client/FrontierSettlementClient.java')
barracks = text(JAVA / 'settlement/SettlementBarracksService.java')
military = text(JAVA / 'settlement/SettlementMilitaryOutpostService.java')

must(entity, (
    'public final class FrontierSoldierEntity extends IronGolem',
    'public FrontierSoldierEntity(EntityType<? extends IronGolem> type, Level level)',
    'carries no server-side weapon ItemStack',
), 'alpha.48 inherited military body')
must(content, (
    'DeferredRegister.createEntities(FrontierSettlement.MOD_ID)',
    'FRONTIER_SOLDIER', '"frontier_soldier"', 'FrontierSoldierEntity::new',
    'builder.sized(0.6F, 1.95F).clientTrackingRange(10)',
    'ENTITIES.register(modBus)', 'EntityAttributeCreationEvent',
    'IronGolem.createAttributes().build()',
), 'alpha.48 entity registration and unchanged combat attributes')

must(renderer, (
    'extends HumanoidMobRenderer<FrontierSoldierEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>',
    'ModelLayers.PLAYER', 'new ItemInHandLayer<>(this)',
    'VISUAL_SERVICE_SWORD = new ItemStack(Items.IRON_SWORD)',
    'state.rightHandItemStack = VISUAL_SERVICE_SWORD',
    'state.rightArmPose = HumanoidModel.ArmPose.ITEM',
    'itemModelResolver.updateForLiving(',
    'ItemDisplayContext.THIRD_PERSON_RIGHT_HAND',
    'DefaultPlayerSkin.getDefaultTexture()',
    'Never call entity.setItemSlot',
), 'alpha.48 client-only humanoid armed presentation')
forbid(renderer, ('weaponsexpanded.', 'bettercombat.', 'ModList', 'entity.setItemSlot('),
       'alpha.48 renderer may not create server equipment or hard companion link')
must(client, ('EntityRenderersEvent.RegisterRenderers',
              'event.registerEntityRenderer(FrontierContent.FRONTIER_SOLDIER.get(), FrontierSoldierRenderer::new)'),
     'alpha.48 renderer registration')

must(barracks, (
    'SOLDIERS_PER_BARRACKS = 3', 'RECRUIT_FOOD_COST = 8L', 'RECRUIT_METAL_COST = 2L',
    'new FrontierSoldierEntity(FrontierContent.FRONTIER_SOLDIER.get(), level)',
    'List<IronGolem> legacy', 'migrateLegacySoldier(level, legacy.getFirst())',
    'replacement.setHealth(Math.min(replacement.getMaxHealth(), legacy.getHealth()))',
    'for (String tag : legacy.entityTags()) replacement.addTag(tag)',
    'legacy.discard()', 'event.getDrops().clear()',
), 'alpha.48 supplied barracks body and 1:1 save migration')
forbid(barracks, ('Items.IRON_SWORD', 'setItemSlot(', 'weaponsexpanded.', 'bettercombat.',
                   'data.addPopulation(', 'data.setPopulation(', 'forceChunk', 'teleportTo('),
       'alpha.48 barracks cannot mint weapons/population or alter loading authority')

must(military, (
    'RECRUIT_FOOD_COST = 6L', 'RECRUIT_METAL_COST = 2L',
    'new FrontierSoldierEntity(FrontierContent.FRONTIER_SOLDIER.get(), level)',
    'List<IronGolem> legacy', 'migrateLegacySentry(level, legacy.getFirst())',
    'replacement.setHealth(Math.min(replacement.getMaxHealth(), legacy.getHealth()))',
    'for (String tag : legacy.entityTags()) replacement.addTag(tag)',
    'legacy.discard()', 'event.getDrops().clear()',
), 'alpha.48 supplied remote sentry body and 1:1 save migration')
forbid(military, ('Items.IRON_SWORD', 'setItemSlot(', 'weaponsexpanded.', 'bettercombat.',
                   'data.addPopulation(', 'data.setPopulation(', 'forceChunk', 'teleportTo('),
       'alpha.48 military outpost cannot mint weapons/population or alter loading authority')

# Save migration must never route through recruitment consumption.
for source, migration, label in ((barracks, 'migrateLegacySoldier', 'barracks'), (military, 'migrateLegacySentry', 'outpost')):
    start = source.find('private static FrontierSoldierEntity ' + migration)
    end = source.find('\n    }', start)
    body = source[start:end]
    forbid(body, ('consumeMetalAndFood', 'consumeLocalSupply', 'RECRUIT_FOOD_COST', 'RECRUIT_METAL_COST'),
           f'alpha.48 {label} legacy migration must be free 1:1 conversion, not recruitment')

props = text(ROOT / 'gradle.properties')
lock = text(ROOT / 'COMPANION_LOCK.json')
must(props, ('mod_version=0.1.0-alpha.48',
             'bounded medium-terrain work using real retaining stone',
             'exploration/conquest milestones',
             'real-wood fishing-outpost piers',
             'opt-in physical fish trade',
             'domain relic reforging for compatible external weapons',
             'supplied humanoid military presentation without server-side weapon minting'),
     'alpha.48 properties')
must(lock, ('"frontier_settlement": "0.1.0-alpha.48"',
            'client-only humanoid/service-sword presentation',
            'visual sword is never a server ItemStack',
            'no Better Combat or Weapons Expanded Java class becomes a hard dependency',
            'historical public WaypointsManager API is absent',
            '"status": "candidate_runtime_lock"'), 'alpha.48 companion lock')

# Current canonical docs are part of Alpha.48 acceptance.
readme = text(ROOT / 'README.md')
canonical = text(ROOT / 'CANONICAL_PLAN.md')
gap = text(ROOT / 'COMPLETION_GAP_AUDIT.md')

must(readme, (
    '## Current version: 0.1.0-alpha.48',
    '## Alpha.48 — supplied humanoid military presentation',
    'new `FrontierSoldierEntity` is a distinct Frontier entity type that **extends IronGolem**',
    'visible service sword is a **client render-state-only ItemStack**',
    'barracks still own exactly **3 supplied slots**',
    '**8 real food + 2 real metal**',
    '**6 real food + 2 real metal**',
    'migrate **1:1** to `FrontierSoldierEntity`',
    'charging no recruitment cost again',
    'does **not** claim that soldiers physically consume/equip Weapons Expanded items yet',
), 'alpha.48 README')
forbid(readme, ('## Current version: 0.1.0-alpha.47',
                'The current regular soldier combat body is an **Iron Golem proxy**.'),
       'alpha.48 README stale state')

must(canonical, (
    'Alpha.40–48 deepen systems',
    'Current families are exactly:',
    'builder walks from actual settlement storage carrying real wood/stone stacks',
    'Transport workers belong to a specific outpost',
    'pause at unloaded route boundaries',
    'single authority for outpost transport',
    'tier-visible public works',
    '### Alpha.48 supplied humanoid military presentation',
    '`FrontierSoldierEntity` is a Frontier-owned entity type that **extends `IronGolem`**',
    '**visual service sword is never a server ItemStack**',
    'older loaded tagged Iron Golem soldiers/sentries migrate **1:1**',
    'A physical external-weapon armory/loadout loop remains unfinished',
    '## 14. Current playable slice after Alpha.48',
    '1. **selected-area cut/fill and larger civil engineering**',
    'Alpha.48 humanoid render/attack-animation + legacy migration acceptance',
    'true Xaero settlement/outpost markers only if a stable supported API/seam appears',
), 'alpha.48 canonical plan')
forbid(canonical, ('## 14. Current playable slice after Alpha.47',
                   'Current regular soldier/sentry bodies are Iron Golem proxies.'),
       'alpha.48 canonical stale state')

must(gap, (
    '현재 구현 기준: `0.1.0-alpha.48`',
    '| 사람형 군사 presentation | **완료/부분** |',
    '| 실물 외부무기 군사 armory/loadout | **미구현/부분** |',
    '### Alpha.48 supplied humanoid military 감사',
    '**visual service sword is never a server ItemStack**',
    'migration은 recruit consume 함수를 호출하지 않으므로 이중 과금 없음',
    'actual external-weapon physical armory는 아직 완료가 아님',
    '현재 functional family는 정확히 **15**다',
    '1. **선택영역 절토/성토 + 대형 civil engineering**',
    '## 11. Alpha.48 추가 실플레이 acceptance',
), 'alpha.48 completion gap audit')
forbid(gap, ('현재 구현 기준: `0.1.0-alpha.47`',
             '| 사람형/무기 장비 병사 presentation | **미구현** |'),
       'alpha.48 completion gap stale state')

print('Frontier Settlement alpha.48 source audit: PASS')
print('Frontier Settlement alpha.48 canonical docs audit: PASS')
