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

# Preserve Alpha.23-47 runtime/source invariants; current Alpha.48 docs are bound after API compile succeeds.
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

# Migration must never route through recruitment consumption.
for source, migration, label in ((barracks, 'migrateLegacySoldier', 'barracks'), (military, 'migrateLegacySentry', 'outpost')):
    start = source.find('private static FrontierSoldierEntity ' + migration)
    end = source.find('\n    }', start)
    body = source[start:end]
    forbid(body, ('consumeMetalAndFood', 'consumeLocalSupply', 'RECRUIT_FOOD_COST', 'RECRUIT_METAL_COST'),
           f'alpha.48 {label} legacy migration must be free 1:1 conversion, not recruitment')

props = text(ROOT / 'gradle.properties')
lock = text(ROOT / 'COMPANION_LOCK.json')
must(props, ('mod_version=0.1.0-alpha.48', 'supplied humanoid military presentation without server-side weapon minting'),
     'alpha.48 properties')
must(lock, ('"frontier_settlement": "0.1.0-alpha.48"',
            'client-only humanoid/service-sword presentation',
            'visual sword is never a server ItemStack',
            'no Better Combat or Weapons Expanded Java class becomes a hard dependency',
            '"status": "candidate_runtime_lock"'), 'alpha.48 companion lock')

print('Frontier Settlement alpha.48 source audit: PASS')
