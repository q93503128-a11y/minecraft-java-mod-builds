package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentPresentationCatalog;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import java.lang.reflect.Method;
import java.util.Locale;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** Player-facing text regressions for the tier the next manual pass will cover. */
public final class LegendaryPresentationGameTests implements CustomTestMethodInvoker {
    @GameTest(maxTicks = 20)
    public void legendaryTierUsesHandAuthoredPlayerFacingDescriptions(GameTestHelper helper) {
        int checked = 0;
        for (var definition : AugmentCatalog.allDefinitions()) {
            if (definition.tier() != AugmentTier.LEGENDARY) continue;
            checked++;
            AugmentPresentationCatalog.Entry entry = AugmentPresentationCatalog.get(definition);
            String text = (entry.summary() + " " + entry.detail()).toLowerCase(Locale.ROOT);
            require(!entry.summary().isBlank() && !entry.detail().isBlank(),
                    "legendary presentation is blank: " + definition.id());
            for (String forbidden : new String[] {"root", "uuid", "server", "compat", "namespace", "debug", "whitelist"}) {
                require(!text.contains(forbidden),
                        "legendary player-facing text leaked internal term '" + forbidden + "': " + definition.id());
            }
        }
        require(checked == 20, "expected 20 legendary presentations, checked=" + checked);
        GameTestPlayers.succeed(helper);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @Override
    public void invokeTestMethod(GameTestHelper helper, Method method) throws ReflectiveOperationException {
        GameTestPlayers.invoke(this, helper, method);
    }
}
