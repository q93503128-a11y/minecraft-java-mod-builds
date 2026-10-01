package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import java.util.Objects;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

/**
 * Server-owned projection of Mage cast speed onto Spell Power's synchronized haste attribute.
 */
public final class PlayerCastSpeedRuntime {
    static final Identifier PROJECT_SPELL_HASTE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(OpenworldRpgMod.MOD_ID, "class_spell_haste");
    static final Identifier SPELL_HASTE_ATTRIBUTE_ID =
            Identifier.fromNamespaceAndPath("spell_power", "haste");

    private PlayerCastSpeedRuntime() {
    }

    public static void synchronize(Player player, double classCastSpeedBonus) {
        Objects.requireNonNull(player, "player");
        if (player.level().isClientSide()) {
            throw new IllegalStateException("Project cast-speed authority is server-only.");
        }
        if (!Double.isFinite(classCastSpeedBonus)
                || classCastSpeedBonus < 0.0
                || classCastSpeedBonus > 1.0) {
            throw new IllegalArgumentException("classCastSpeedBonus must be inside [0, 1].");
        }

        var hasteHolder = BuiltInRegistries.ATTRIBUTE.get(SPELL_HASTE_ATTRIBUTE_ID).orElse(null);
        var haste = hasteHolder == null ? null : player.getAttribute(hasteHolder);
        if (haste == null) {
            if (FabricLoader.getInstance().isModLoaded("spell_power")) {
                throw new IllegalStateException(
                        "Spell Power is loaded but spell_power:haste is unavailable."
                );
            }
            return;
        }

        haste.removeModifier(PROJECT_SPELL_HASTE_MODIFIER_ID);
        if (classCastSpeedBonus > 0.0) {
            haste.addOrUpdateTransientModifier(
                    new AttributeModifier(
                            PROJECT_SPELL_HASTE_MODIFIER_ID,
                            classCastSpeedBonus,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                    )
            );
        }
    }
}
