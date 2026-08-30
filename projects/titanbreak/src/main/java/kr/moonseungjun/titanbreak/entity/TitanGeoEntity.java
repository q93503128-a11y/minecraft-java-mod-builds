package kr.moonseungjun.titanbreak.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.object.PlayState;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.DefaultAnimations;
import com.geckolib.util.GeckoLibUtil;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Shared animation contract for the non-vanilla TITANBREAK creature presentation.
 * Gameplay remains server-authoritative; this only supplies the client animation state.
 */
public interface TitanGeoEntity extends GeoEntity {
    Map<TitanGeoEntity, AnimatableInstanceCache> GEO_CACHES =
            Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    default void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(DefaultAnimations.genericWalkIdleController());
        controllers.add(new AnimationController<>("Attack", test -> {
            if (test.getDataOrDefault(DataTickets.SWINGING_ARM, false)) {
                return test.setAndContinue(DefaultAnimations.ATTACK_SWING);
            }
            return PlayState.STOP;
        }));
    }

    @Override
    default AnimatableInstanceCache getAnimatableInstanceCache() {
        synchronized (GEO_CACHES) {
            return GEO_CACHES.computeIfAbsent(this, GeckoLibUtil::createInstanceCache);
        }
    }
}
