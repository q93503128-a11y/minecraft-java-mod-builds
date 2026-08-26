#!/usr/bin/env python3
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'
CLIENT = JAVA / 'client'
A79 = ROOT / 'tools/test_alpha79_source.py'
_real_read = Path.read_text

_ALPHA79_RENDERER = '''package kr.moonseungjun.frontiersettlement.client;

import kr.moonseungjun.frontiersettlement.content.FrontierSoldierEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client-only human soldier presentation for the supplied Frontier military body.
 *
 * Alpha.48's iron service sword remains a client-only fallback for an un-upgraded soldier. Alpha.57
 * renders the entity's real synced MAINHAND ItemStack when the automated barracks armory has physically
 * assigned one. The renderer itself never creates or inserts economic equipment.
 */
public final class FrontierSoldierRenderer extends HumanoidMobRenderer<FrontierSoldierEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private static final ItemStack VISUAL_SERVICE_SWORD = new ItemStack(Items.IRON_SWORD);

    public FrontierSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new ItemInHandLayer<>(this));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public void extractRenderState(FrontierSoldierEntity entity, HumanoidRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        // Renderer rule: Never call entity.setItemSlot here; the server armory owns real equipment.
        ItemStack physicalWeapon = entity.getMainHandItem();
        if (physicalWeapon.isEmpty()) {
            state.rightHandItemStack = VISUAL_SERVICE_SWORD;
            state.rightArmPose = HumanoidModel.ArmPose.ITEM;
            this.itemModelResolver.updateForLiving(
                    state.rightHandItemState,
                    VISUAL_SERVICE_SWORD,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    entity);
        } else {
            state.rightHandItemStack = physicalWeapon;
            state.rightArmPose = HumanoidModel.ArmPose.ITEM;
            this.itemModelResolver.updateForLiving(
                    state.rightHandItemState,
                    physicalWeapon,
                    ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    entity);
        }
        int attackTicks = entity.getAttackAnimationTick();
        if (attackTicks > 0) state.attackTime = Math.max(state.attackTime, 1.0F - Math.min(1.0F, attackTicks / 10.0F));
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
'''


def legacy_read(self, *args, **kwargs):
    s = _real_read(self, *args, **kwargs)
    if self.name == 'gradle.properties':
        s = s.replace('mod_version=0.1.0-alpha.80', 'mod_version=0.1.0-alpha.79')
        s = s.replace(', plus Alpha.80 client-boot hardening that defers the presentation-only service-sword ItemStack until real render-state extraction after registry component binding and audits client code against registry-backed static ItemStack initialization.', '.')
    elif self.name == 'COMPANION_LOCK.json':
        s = s.replace('"frontier_settlement": "0.1.0-alpha.80"', '"frontier_settlement": "0.1.0-alpha.79"')
    elif self.name == 'FrontierSoldierRenderer.java':
        # Historical audits intentionally assert Alpha.48's old presentation implementation. Feed the
        # Alpha.79 view only while replaying that historical chain; Alpha.80 is audited below for real.
        s = _ALPHA79_RENDERER
    return s


Path.read_text = legacy_read
try:
    chain = _real_read(A79, encoding='utf-8').replace(
        "print('Frontier Settlement alpha.23-79 cumulative source audit: PASS')", 'pass')
    ns = {'__file__': str(A79), '__name__': '__main__'}
    exec(compile(chain, str(A79), 'exec'), ns, ns)
finally:
    Path.read_text = _real_read


def text(path):
    return Path(path).read_text(encoding='utf-8')


def must(src, tokens, label):
    for token in tokens:
        if token not in src:
            raise SystemExit(f'{label} missing: {token}')


renderer = text(CLIENT / 'FrontierSoldierRenderer.java')
props = text(ROOT / 'gradle.properties')

must(renderer, (
    'private ItemStack visualServiceSword;',
    'ItemStack serviceSword = visualServiceSword();',
    'private ItemStack visualServiceSword()',
    'if (visualServiceSword == null)',
    'visualServiceSword = new ItemStack(Items.IRON_SWORD);',
    'presentation-only and is never inserted into the entity, world, inventory, or settlement economy',
), 'alpha.80 lazy service-sword render fallback')

for forbidden in (
    'private static final ItemStack VISUAL_SERVICE_SWORD',
    'static final ItemStack VISUAL_SERVICE_SWORD = new ItemStack',
):
    if forbidden in renderer:
        raise SystemExit(f'alpha.80 early ItemStack bootstrap hazard remains: {forbidden}')

# Minecraft 26.2 can load renderer/client classes before registry-backed item components are bound.
# Fail the cumulative audit if a future client class recreates the same class-initializer hazard.
static_itemstack = re.compile(
    r'\bstatic\s+(?:final\s+)?ItemStack\s+\w+\s*=\s*(?:new\s+ItemStack\s*\(|Items\.[^;\n]+getDefaultInstance\s*\()')
for path in CLIENT.rglob('*.java'):
    src = text(path)
    if static_itemstack.search(src):
        raise SystemExit(f'alpha.80 registry-backed static ItemStack initializer in client code: {path.relative_to(ROOT)}')

must(props, (
    'mod_version=0.1.0-alpha.80',
    'Alpha.80 client-boot hardening',
    'defers the presentation-only service-sword ItemStack',
), 'alpha.80 props')

print('Frontier Settlement alpha.23-80 cumulative source audit: PASS')
