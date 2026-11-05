package dev.moonseungjun.testmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonConfiguration;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.enums.PlayState;
import dev.moonseungjun.testmod.TestMod;
import dev.moonseungjun.testmod.network.CastStartedPayload;
import dev.moonseungjun.testmod.network.CastRequestPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class YinYangClient implements ClientModInitializer {
    public static final Identifier ANIMATION_LAYER = Identifier.fromNamespaceAndPath(TestMod.MOD_ID, "yin_yang_cast");
    public static final Identifier CAST_ANIMATION = Identifier.fromNamespaceAndPath(TestMod.MOD_ID, "yin_yang.cast");
    private static final List<CastState> CASTS = new ArrayList<>();

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(TestMod.MOD_ID, "experiments")
    );
    private static final KeyMapping CAST_KEY = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.moon_test.yin_yang", InputConstants.KEY_G, CATEGORY)
    );

    @Override
    public void onInitializeClient() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(ANIMATION_LAYER, 1600,
                avatar -> {
                    PlayerAnimationController controller = new PlayerAnimationController(avatar,
                            (animationController, state, animationSetter) -> PlayState.STOP);
                    controller.setFirstPersonMode(FirstPersonMode.THIRD_PERSON_MODEL);
                    controller.setFirstPersonConfiguration(new FirstPersonConfiguration()
                            .setShowLeftArm(true)
                            .setShowRightArm(true));
                    controller.setFirstPersonTransitionLength(5);
                    return controller;
                });

        ClientPlayNetworking.registerGlobalReceiver(CastStartedPayload.TYPE, (payload, context) ->
                context.client().execute(() -> beginCast(payload.casterId())));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (CAST_KEY.consumeClick()) {
                if (client.player != null && client.level != null && client.player.isAlive()) {
                    ClientPlayNetworking.send(new CastRequestPayload());
                }
            }

            if (client.level == null) {
                CASTS.clear();
                return;
            }

            long tick = client.level.getGameTime();
            Iterator<CastState> iterator = CASTS.iterator();
            while (iterator.hasNext()) {
                CastState cast = iterator.next();
                Player player = client.level.getPlayerByUUID(cast.casterId);
                if (player == null || !player.isAlive() || !client.level.dimension().equals(cast.dimension)
                        || tick - cast.startedAt > 132) {
                    iterator.remove();
                    continue;
                }

                int elapsed = (int) (tick - cast.startedAt);
                if (elapsed >= 4) playOnce(client, cast, 4, SoundEvents.AMETHYST_BLOCK_CHIME, 0.72F, 0.58F);
                if (elapsed >= 8) playOnce(client, cast, 8, SoundEvents.BEACON_ACTIVATE, 1.22F, 0.42F);
                if (elapsed >= 29) playOnce(client, cast, 29, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.84F, 0.64F);
                if (elapsed >= 51) playOnce(client, cast, 51, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 0.86F);
                if (elapsed >= 58) playOnce(client, cast, 58, SoundEvents.WARDEN_SONIC_BOOM, 0.76F, 0.66F);

                if (elapsed >= 58 && !cast.launched) {
                    cast.launched = true;
                    cast.projectileOrigin = player.position().add(0, player.getBbHeight() * 0.62, 0)
                            .add(player.getLookAngle().scale(0.72));
                    cast.projectileDirection = player.getLookAngle().normalize();
                    cast.previousProjectilePosition = cast.projectileOrigin;
                }
                if (cast.launched && !cast.impact && elapsed >= 59) {
                    var next = cast.projectileOrigin.add(cast.projectileDirection.scale((elapsed - 58) * 0.82));
                    var hit = client.level.clip(new net.minecraft.world.level.ClipContext(
                            cast.previousProjectilePosition, next,
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE, player));
                    if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                        cast.impact = true;
                        cast.impactAt = elapsed;
                        cast.impactPosition = hit.getLocation();
                        playOnce(client, cast, 59, SoundEvents.TOTEM_USE, 0.8F, 0.74F);
                    } else if (elapsed >= 89) {
                        cast.impact = true;
                        cast.impactAt = elapsed;
                        cast.impactPosition = next;
                        playOnce(client, cast, 59, SoundEvents.TOTEM_USE, 0.8F, 0.74F);
                    } else {
                        cast.previousProjectilePosition = next;
                    }
                }
                if (cast.impact && !cast.fadeSound && elapsed - cast.impactAt >= 9) {
                    cast.fadeSound = true;
                    client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 0.62F));
                }
            }
        });

        YinYangRenderer.register();
    }

    private static void beginCast(UUID casterId) {
        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.level == null) return;
        Player caster = client.level.getPlayerByUUID(casterId);
        if (caster == null || !caster.isAlive()) return;

        CASTS.removeIf(existing -> existing.casterId.equals(casterId));
        CastState cast = new CastState(casterId, client.level.dimension(), client.level.getGameTime());
        CASTS.add(cast);

        Object layer = PlayerAnimationAccess.getPlayerAnimationLayer(caster, ANIMATION_LAYER);
        if (layer instanceof PlayerAnimationController controller) {
            controller.triggerAnimation(CAST_ANIMATION);
        }
        if (client.player != null && client.player.getUUID().equals(casterId)) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.42F));
        }
    }

    private static void playOnce(net.minecraft.client.Minecraft client, CastState cast, int marker,
                                 net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        if (cast.soundMarkers.add(marker)) {
            client.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    public static List<CastState> casts() {
        return CASTS;
    }

    public static PlayerAnimationController controller(Player player) {
        Object layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, ANIMATION_LAYER);
        return layer instanceof PlayerAnimationController controller ? controller : null;
    }

    public static final class CastState {
        public final UUID casterId;
        public final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        public final long startedAt;
        public final java.util.Set<Integer> soundMarkers = new java.util.HashSet<>();
        public boolean launched;
        public boolean impact;
        public boolean fadeSound;
        public int impactAt;
        public net.minecraft.world.phys.Vec3 projectileOrigin;
        public net.minecraft.world.phys.Vec3 projectileDirection;
        public net.minecraft.world.phys.Vec3 previousProjectilePosition;
        public net.minecraft.world.phys.Vec3 impactPosition;

        private CastState(UUID casterId, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, long startedAt) {
            this.casterId = casterId;
            this.dimension = dimension;
            this.startedAt = startedAt;
        }
    }
}
