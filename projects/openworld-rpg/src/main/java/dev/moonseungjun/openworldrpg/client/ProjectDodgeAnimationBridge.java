package dev.moonseungjun.openworldrpg.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional-link bridge into the required gameplay Player Animation Library.
 *
 * <p>Reflection keeps core-profile class loading independent from the gameplay runtime while still
 * making accepted dodge presentation fail closed if the exact admitted animation resource or PAL
 * API is unavailable.</p>
 */
public final class ProjectDodgeAnimationBridge {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ProjectDodgeAnimationBridge.class
            );
    private static final Identifier FORWARD =
            id("dodge_forward");
    private static final Identifier BACKWARD =
            id("dodge_backward");
    private static final Identifier LEFT =
            id("dodge_left");
    private static final Identifier RIGHT =
            id("dodge_right");

    private static volatile ReflectionApi reflectionApi;
    private static volatile boolean reflectionAttempted;
    private static volatile boolean warnedUnavailable;

    private ProjectDodgeAnimationBridge() {
    }

    public static boolean presentationAvailable() {
        ReflectionApi api = api();
        return api != null
                && api.hasAnimation(FORWARD)
                && api.hasAnimation(BACKWARD)
                && api.hasAnimation(LEFT)
                && api.hasAnimation(RIGHT);
    }

    public static boolean playAccepted(
            int entityId,
            int directionCode
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return false;
        }
        Entity entity = client.level.getEntity(entityId);
        if (entity == null) {
            return false;
        }

        ReflectionApi api = api();
        if (api == null) {
            return false;
        }
        Identifier animation = switch (directionCode) {
            case 0 -> FORWARD;
            case 1 -> BACKWARD;
            case 2 -> LEFT;
            case 3 -> RIGHT;
            default -> null;
        };
        return animation != null
                && api.trigger(entity, animation);
    }

    private static ReflectionApi api() {
        if (reflectionAttempted) {
            return reflectionApi;
        }
        synchronized (ProjectDodgeAnimationBridge.class) {
            if (reflectionAttempted) {
                return reflectionApi;
            }
            reflectionAttempted = true;
            try {
                Class<?> accessClass = Class.forName(
                        "com.zigythebird.playeranim.api.PlayerAnimationAccess"
                );
                Class<?> resourcesClass = Class.forName(
                        "com.zigythebird.playeranim.animation.PlayerAnimResources"
                );
                Class<?> modClass = Class.forName(
                        "com.zigythebird.playeranim.PlayerAnimLibMod"
                );
                Class<?> controllerClass = Class.forName(
                        "com.zigythebird.playeranim.animation.PlayerAnimationController"
                );
                Class<?> avatarClass = Class.forName(
                        "net.minecraft.world.entity.Avatar"
                );

                Field layerField = modClass.getField(
                        "ANIMATION_LAYER_ID"
                );
                Object layerId = layerField.get(null);
                Method getLayer = accessClass.getMethod(
                        "getPlayerAnimationLayer",
                        avatarClass,
                        Identifier.class
                );
                Method hasAnimation = resourcesClass.getMethod(
                        "hasAnimation",
                        Identifier.class
                );
                Method trigger = controllerClass.getMethod(
                        "triggerAnimation",
                        Identifier.class
                );
                reflectionApi = new ReflectionApi(
                        avatarClass,
                        layerId,
                        getLayer,
                        hasAnimation,
                        trigger
                );
            } catch (ReflectiveOperationException exception) {
                warnUnavailable(exception);
            }
            return reflectionApi;
        }
    }

    private static void warnUnavailable(Exception exception) {
        if (!warnedUnavailable) {
            warnedUnavailable = true;
            LOGGER.warn(
                    "Openworld RPG dodge presentation is unavailable because the pinned Player Animation Library API could not be resolved.",
                    exception
            );
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(
                "openworld_rpg",
                path
        );
    }

    private record ReflectionApi(
            Class<?> avatarClass,
            Object layerId,
            Method getLayer,
            Method hasAnimation,
            Method trigger
    ) {
        private boolean hasAnimation(
                Identifier animation
        ) {
            try {
                return Boolean.TRUE.equals(
                        hasAnimation.invoke(
                                null,
                                animation
                        )
                );
            } catch (ReflectiveOperationException exception) {
                warnUnavailable(exception);
                return false;
            }
        }

        private boolean trigger(
                Entity entity,
                Identifier animation
        ) {
            if (!avatarClass.isInstance(entity)
                    || !hasAnimation(animation)) {
                return false;
            }
            try {
                Object controller = getLayer.invoke(
                        null,
                        entity,
                        layerId
                );
                if (controller == null) {
                    return false;
                }
                return Boolean.TRUE.equals(
                        trigger.invoke(
                                controller,
                                animation
                        )
                );
            } catch (ReflectiveOperationException exception) {
                warnUnavailable(exception);
                return false;
            }
        }
    }
}
