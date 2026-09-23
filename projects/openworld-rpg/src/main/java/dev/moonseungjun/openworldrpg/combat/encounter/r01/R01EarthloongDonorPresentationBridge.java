package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Dependency-only bridge to the pinned Earthloong public synced SkillNumber state.
 *
 * <p>This does not call donor combat procedures and never accepts donor damage. It only drives
 * already-present donor animation states for actions with an audited technical presentation
 * candidate. Tail Scythe deliberately has no candidate and therefore fails closed.</p>
 */
public final class R01EarthloongDonorPresentationBridge {
    private static final String EARTHLOONG_ID = "threateningly_mobs:the_earthloong";
    private static final String SKILL_FIELD = "DATA_SkillNumber";
    private static final R01EarthloongEncounterData DATA =
            R01EarthloongEncounterDataLoader.loadBundled();
    private static final Map<Class<?>, EntityDataAccessor<Integer>> SKILL_ACCESSORS =
            new ConcurrentHashMap<>();

    private R01EarthloongDonorPresentationBridge() {
    }

    public static PresentationApplication startTechnicalCandidate(
            LivingEntity earthloong,
            R01EarthloongEncounterData.ActionId action
    ) {
        Objects.requireNonNull(earthloong, "earthloong");
        Objects.requireNonNull(action, "action");

        if (!isEarthloong(earthloong)) {
            return PresentationApplication.rejected();
        }

        var physical = DATA.physicalBindingsById().get(action);
        if (physical != null && physical.hasDonorPresentationCandidate()) {
            int skillNumber = physical.donorSkillNumber();
            earthloong.getEntityData().set(resolveSkillAccessor(earthloong), skillNumber);
            return new PresentationApplication(
                    true,
                    skillNumber,
                    physical.donorAnimationTicks()
            );
        }

        var spaceControl = DATA.spaceControlBindingsById().get(action);
        if (spaceControl == null || !spaceControl.hasDonorPresentationCandidate()) {
            return PresentationApplication.rejected();
        }

        int skillNumber = spaceControl.donorSkillNumber();
        earthloong.getEntityData().set(resolveSkillAccessor(earthloong), skillNumber);
        return new PresentationApplication(
                true,
                skillNumber,
                spaceControl.donorAnimationTicks()
        );
    }

    /**
     * Verification-only escape hatch for evaluating an audited donor animation state against an
     * authored project action before that mapping is promoted into normal gameplay.
     *
     * <p>Normal encounter scheduling must continue to use {@link #startTechnicalCandidate}; this
     * method exists only behind the explicit M0 player-verification command path.</p>
     */
    public static PresentationApplication startVerificationCandidate(
            LivingEntity earthloong,
            int donorSkillNumber,
            int donorAnimationTicks
    ) {
        Objects.requireNonNull(earthloong, "earthloong");
        if (!isEarthloong(earthloong)
                || donorSkillNumber < 1
                || donorSkillNumber > 4
                || donorAnimationTicks <= 0) {
            return PresentationApplication.rejected();
        }
        earthloong.getEntityData().set(resolveSkillAccessor(earthloong), donorSkillNumber);
        return new PresentationApplication(true, donorSkillNumber, donorAnimationTicks);
    }

    public static boolean resetTechnicalCandidate(LivingEntity earthloong) {
        Objects.requireNonNull(earthloong, "earthloong");
        if (!isEarthloong(earthloong)) {
            return false;
        }
        earthloong.getEntityData().set(resolveSkillAccessor(earthloong), 0);
        return true;
    }

    public static int currentSkillNumber(LivingEntity earthloong) {
        Objects.requireNonNull(earthloong, "earthloong");
        if (!isEarthloong(earthloong)) {
            throw new IllegalArgumentException("Entity is not the bound R01 Earthloong.");
        }
        return earthloong.getEntityData().get(resolveSkillAccessor(earthloong));
    }

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Integer> resolveSkillAccessor(LivingEntity earthloong) {
        return SKILL_ACCESSORS.computeIfAbsent(earthloong.getClass(), type -> {
            try {
                Field field = type.getField(SKILL_FIELD);
                Object raw = field.get(null);
                if (!(raw instanceof EntityDataAccessor<?> accessor)) {
                    throw new IllegalStateException(
                            "Earthloong " + SKILL_FIELD + " is not an EntityDataAccessor."
                    );
                }
                return (EntityDataAccessor<Integer>) accessor;
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(
                        "Pinned Earthloong synced animation state is unavailable.",
                        exception
                );
            }
        });
    }

    private static boolean isEarthloong(LivingEntity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && EARTHLOONG_ID.equals(id.toString());
    }

    public record PresentationApplication(
            boolean accepted,
            int donorSkillNumber,
            int donorAnimationTicks
    ) {
        public PresentationApplication {
            if (accepted) {
                if (donorSkillNumber < 1 || donorSkillNumber > 4 || donorAnimationTicks <= 0) {
                    throw new IllegalArgumentException(
                            "Accepted Earthloong presentation mapping must be valid."
                    );
                }
            } else if (donorSkillNumber != 0 || donorAnimationTicks != 0) {
                throw new IllegalArgumentException(
                        "Rejected Earthloong presentation mapping must be empty."
                );
            }
        }

        public static PresentationApplication rejected() {
            return new PresentationApplication(false, 0, 0);
        }
    }
}
