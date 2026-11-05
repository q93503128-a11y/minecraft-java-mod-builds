package dev.moonseungjun.testmod;

import net.fabricmc.api.ModInitializer;
import dev.moonseungjun.testmod.network.SkillNetworking;

public final class TestMod implements ModInitializer {
    public static final String MOD_ID = "moon_test";

    @Override
    public void onInitialize() {
        SkillNetworking.registerCommon();
    }
}
