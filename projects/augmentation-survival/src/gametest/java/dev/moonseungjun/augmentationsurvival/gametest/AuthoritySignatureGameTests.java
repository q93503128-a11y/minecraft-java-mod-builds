package dev.moonseungjun.augmentationsurvival.gametest;

import dev.moonseungjun.augmentationsurvival.augment.active.AuthorityPowerCatalog;
import dev.moonseungjun.augmentationsurvival.augment.active.AuthorityTheftService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import java.util.HashSet;
import java.lang.reflect.Method;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;

/** Regression contracts for server-owned mythic authority collection. */
public final class AuthoritySignatureGameTests implements CustomTestMethodInvoker {
    @GameTest
    public void canonicalAuthoritySourcesHaveDistinctSupportedIdentities(GameTestHelper helper) {
        require(AuthorityPowerCatalog.ENTRIES.size() == 26,
                "Expected 26 bound boss signatures (four vanilla + six Chrono Dawn + eleven World Bosses + five MCEndgame)");
        var ids = new HashSet<String>();
        for (var entry : AuthorityPowerCatalog.ENTRIES) {
            require(ids.add(entry.id()), "duplicate authority identity " + entry.id());
            require(AuthorityTheftService.supports(entry.id()), "unbound authority " + entry.id());
            require(!entry.effect().isBlank(), "undocumented authority effect " + entry.id());
        }
        require(ids.contains("vanilla_warden") && ids.contains("vanilla_elder_guardian"),
                "Warden and Elder Guardian signatures must both be represented");
        helper.succeed();
    }

    @GameTest
    public void disablingAllAuthorityPowersCannotSpendTheGlobalCooldown(GameTestHelper helper) {
        ServerPlayer player = GameTestPlayers.survivalPlayer(helper);
        String authority = AuthorityTheftService.AUTHORITY_THEFT_ID;
        String wither = "vanilla_wither";
        var disabled = AugmentationPlayerState.initial()
                .withAugmentLevel(authority, 1)
                .withStolenAuthority(wither)
                .withPersistentInt(AuthorityTheftService.DISABLED_PREFIX + wither, 1);
        AugmentationPlayerStateService.replace(player, disabled);
        require(!AuthorityTheftService.isEnabled(AugmentationPlayerStateService.state(player), wither),
                "disabled authority incorrectly considered enabled");
        require(!AuthorityTheftService.tryRelease(player),
                "disabled authority incorrectly started a release sequence");
        String cd = authority + "#release";
        require(AugmentationPlayerStateService.state(player).cooldownReadyTick(cd) <= player.level().getGameTime(),
                "disabled authority consumed its 120-second cooldown");
        AugmentationPlayerStateService.replace(player,
                AugmentationPlayerStateService.state(player)
                        .withPersistentInt(AuthorityTheftService.DISABLED_PREFIX + wither, 0));
        require(AuthorityTheftService.isEnabled(AugmentationPlayerStateService.state(player), wither),
                "re-enabled authority not selectable");
        require(AuthorityTheftService.tryRelease(player),
                "re-enabled authority failed to begin a server-owned release");
        require(AugmentationPlayerStateService.state(player).cooldownReadyTick(cd)
                        >= player.level().getGameTime() + 120 * 20 - 1,
                "enabled release did not reserve the canonical 120-second cooldown");
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
