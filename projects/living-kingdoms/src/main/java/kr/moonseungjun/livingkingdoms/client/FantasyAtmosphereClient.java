package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.world.StarterRealmManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Restrained RPG atmosphere for the authored realm.
 *
 * <p>The effect uses supported NeoForge viewport hooks instead of replacing Minecraft core
 * shaders, so other dimensions and user-installed shader packs remain untouched.</p>
 */
public final class FantasyAtmosphereClient {
    private FantasyAtmosphereClient() {
    }

    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!insideLivingRealm(minecraft)) return;

        float day = dayFraction(minecraft.level.getGameTime(), event.getPartialTick());
        Atmosphere tone = tone(day);
        event.setRed(mix(event.getRed(), tone.red(), tone.strength()));
        event.setGreen(mix(event.getGreen(), tone.green(), tone.strength()));
        event.setBlue(mix(event.getBlue(), tone.blue(), tone.strength()));
    }

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!insideLivingRealm(minecraft) || event.getType() != FogType.NONE) return;

        float day = dayFraction(minecraft.level.getGameTime(), event.getPartialTick());
        float daylight = daylight(day);
        event.scaleFarPlaneDistance(0.84F + daylight * 0.11F);
        event.scaleNearPlaneDistance(0.78F + daylight * 0.14F);
    }

    private static boolean insideLivingRealm(Minecraft minecraft) {
        return minecraft.level != null
                && minecraft.level.dimension().equals(StarterRealmManager.REALM_KEY);
    }

    private static float dayFraction(long gameTime, double partialTick) {
        return (float) (((gameTime % 24_000L) + partialTick) / 24_000.0);
    }

    private static Atmosphere tone(float day) {
        float dawn = bell(day, 0.235F, 0.085F);
        float dusk = bell(day, 0.765F, 0.095F);
        float night = 1.0F - daylight(day);
        float warm = Math.max(dawn, dusk);

        float red = 0.63F + warm * 0.28F - night * 0.19F;
        float green = 0.71F + warm * 0.03F - night * 0.28F;
        float blue = 0.76F - warm * 0.20F - night * 0.25F;
        float strength = 0.08F + warm * 0.16F + night * 0.10F;
        return new Atmosphere(clamp(red), clamp(green), clamp(blue), clamp(strength));
    }

    private static float daylight(float day) {
        float distanceFromNoon = Math.abs(day - 0.5F) * 2.0F;
        return smoothstep(1.0F - distanceFromNoon);
    }

    private static float bell(float value, float center, float radius) {
        float distance = Math.abs(value - center);
        distance = Math.min(distance, 1.0F - distance);
        return smoothstep(1.0F - distance / radius);
    }

    private static float smoothstep(float value) {
        float x = clamp(value);
        return x * x * (3.0F - 2.0F * x);
    }

    private static float mix(float from, float to, float amount) {
        return from + (to - from) * clamp(amount);
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private record Atmosphere(float red, float green, float blue, float strength) {
    }
}
