package dev.moonseungjun.openworldrpg.client;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.integration.spellengine.SpellEngineProjectSkillAccess;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.LocalPlayer;

/**
 * Client-only input bridge from the project's five class skill actions into Spell Engine casting.
 */
public final class ProjectSpellClientBridge {
    private static final String MOD_ID = "spell_engine";

    private ProjectSpellClientBridge() {
    }

    public static boolean castSlot(
            LocalPlayer player,
            int slotIndex
    ) {
        if (player == null
                || !FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return false;
        }

        String spellId = SpellEngineProjectSkillAccess
                .publishedSpellId(player, slotIndex)
                .orElse(null);
        if (spellId == null) {
            return false;
        }

        try {
            ClassLoader loader =
                    ProjectSpellClientBridge.class.getClassLoader();
            Class<?> clientCaster = Class.forName(
                    "net.spell_engine.internals.casting.SpellCaster$Client",
                    false,
                    loader
            );
            if (!clientCaster.isInstance(player)) {
                return false;
            }

            Object controller = clientCaster
                    .getMethod("getCastController")
                    .invoke(player);
            @SuppressWarnings("unchecked")
            List<Object> options =
                    (List<Object>) controller.getClass()
                            .getMethod("displayOptions")
                            .invoke(controller);

            Object selected = null;
            for (Object option : options) {
                String optionId = String.valueOf(
                        option.getClass()
                                .getMethod("id")
                                .invoke(option)
                );
                if (spellId.equals(optionId)) {
                    selected = option;
                    break;
                }
            }
            if (selected == null) {
                return false;
            }

            if (ProjectSpellSpec.HUNTER_QUICKSTEP_VOLLEY_ID.equals(spellId)) {
                HunterQuickstepClientBridge.publishMovementIntent(player);
            }

            Method keyHeld = controller.getClass().getMethod(
                    "keyHeld",
                    selected.getClass(),
                    boolean.class,
                    boolean.class
            );
            keyHeld.invoke(
                    controller,
                    selected,
                    true,
                    true
            );
            return true;
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            throw new IllegalStateException(
                    "Project class skill client dispatch failed.",
                    cause == null ? exception : cause
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Pinned Spell Engine client cast contract is unavailable.",
                    exception
            );
        }
    }
}
