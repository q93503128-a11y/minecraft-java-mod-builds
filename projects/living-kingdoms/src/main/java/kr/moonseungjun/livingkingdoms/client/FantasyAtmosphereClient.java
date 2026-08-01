package kr.moonseungjun.livingkingdoms.client;

import kr.moonseungjun.livingkingdoms.world.StarterRealmManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * A restrained RPG atmosphere profile for the authored realm.
 *
 * <p>This deliberately uses NeoForge viewport hooks instead of replacing Minecraft core shaders.
 * It keeps other dimensions and user-installed shader packs untouched while giving the Living
 * Kingdoms realm warmer dawns, calmer daylight and cooler, deeper nights.</p>
 */
public final class FantasyAtmosphereClient {
    private FantasyAtmosphereClient() {
    }

    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || event.getCamera().getEntity() == null) return;
        if (!event.getCamera().getEntity().level().dimension().equals(StarterRealmManager.REALM_KEY)) return;

        float day = dayFraction(minecraft.level.getDayTime(), event.getPartialTick());
        Atmosphere tone = tone(day);
        event.setRed(mix(event.getRed(), tone.red(), tone.strength()));
        event.setGreen(mix(event.getGreen(), tone.green(), tone.strength()));
        event.setBlue(mix(event.getBlue(), tone.blue(), tone.strength()));
    }

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || event.getCamera().getEntity() == null) return;
        if (!event.getCamera().getEntity().level().dimension().equals(StarterRealmManager.REALM_KEY)) return;
        if (event.getType() != FogType.NONE) return;

        float day = dayFraction(minecraft.level.getDayTime(), event.getPartialTick());
        float daylight = daylight(day);

        // Keep distant silhouettes readable by day, but give night travel a stronger sense of depth.
        float farScale = 0.84F + daylight * 0.11F;
        float nearScale = 0.78F + daylight * 0.14F;
        event.scaleFarPlaneDistance(farScale);
        event.scaleNearPlaneDistance(nearScale);
        event.setCanceled(true);
    }

    private static float dayFraction(long dayTime, double partialTick) {
        return (float) (((dayTime % 24_000L) + partialTick) / 24_000.0);
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
        // Noon is near 0.5 in Minecraft's day cycle; midnight wraps around 0/1.
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
