package dev.moonseungjun.augmentationsurvival.client;

/** Client presentation cache for server-owned protection pools. */
public final class ClientProtectionHudState {
    private static float shield;
    private static float shieldCapacity;
    private static float augmentAbsorption;
    private static float augmentAbsorptionCapacity;

    private ClientProtectionHudState() { }

    public static void apply(float shieldValue, float shieldCap, float absorptionValue, float absorptionCap) {
        shield = Math.max(0.0F, shieldValue);
        shieldCapacity = Math.max(shield, shieldCap);
        augmentAbsorption = Math.max(0.0F, absorptionValue);
        augmentAbsorptionCapacity = Math.max(augmentAbsorption, absorptionCap);
    }

    public static void clear() { apply(0.0F, 0.0F, 0.0F, 0.0F); }
    public static float shield() { return shield; }
    public static float shieldCapacity() { return shieldCapacity; }
    public static float augmentAbsorption() { return augmentAbsorption; }
    public static float augmentAbsorptionCapacity() { return augmentAbsorptionCapacity; }
}
