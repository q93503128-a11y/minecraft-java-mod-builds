package dev.moonseungjun.openworldrpg.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class OpenworldRpgMixinPlugin implements IMixinConfigPlugin {
    private static final String BETTER_COMBAT_MIXIN =
            "dev.moonseungjun.openworldrpg.mixin.PlayerAttackAuthorityMixin";
    private static final String SPELL_ENGINE_SINGLE_PRESS_MIXIN =
            "dev.moonseungjun.openworldrpg.mixin.SpellEngineSinglePressMixin";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (BETTER_COMBAT_MIXIN.equals(mixinClassName)) {
            return FabricLoader.getInstance().isModLoaded("bettercombat");
        }
        if (SPELL_ENGINE_SINGLE_PRESS_MIXIN.equals(mixinClassName)) {
            return FabricLoader.getInstance().isModLoaded("spell_engine");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
    }

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
    }
}
