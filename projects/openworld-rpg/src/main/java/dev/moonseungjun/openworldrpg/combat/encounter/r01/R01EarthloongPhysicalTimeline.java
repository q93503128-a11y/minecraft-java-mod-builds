package dev.moonseungjun.openworldrpg.combat.encounter.r01;

import java.util.Objects;

public record R01EarthloongPhysicalTimeline(int tellTicks, int activeTicks, int recoveryTicks) {
    public R01EarthloongPhysicalTimeline {
        if (tellTicks < 0 || activeTicks <= 0 || recoveryTicks < 0) {
            throw new IllegalArgumentException("Invalid Earthloong physical timeline.");
        }
    }

    public static R01EarthloongPhysicalTimeline from(
            R01EarthloongEncounterData.PhysicalBindingRule binding) {
        Objects.requireNonNull(binding, "binding");
        int activeTicks = 1;
        if (binding.hasDonorPresentationCandidate()) {
            activeTicks = binding.donorAnimationTicks() - binding.tellTicks() - binding.recoveryTicks();
            if (activeTicks <= 0) {
                throw new IllegalArgumentException(
                        "Earthloong donor animation leaves no active window: " + binding.action());
            }
        }
        return new R01EarthloongPhysicalTimeline(
                binding.tellTicks(), activeTicks, binding.recoveryTicks());
    }

    public int totalTicks() {
        return Math.addExact(Math.addExact(tellTicks, activeTicks), recoveryTicks);
    }

    public Phase phaseAtElapsedTick(long elapsedTick) {
        if (elapsedTick < 0L) {
            throw new IllegalArgumentException("elapsedTick must be non-negative.");
        }
        if (elapsedTick < tellTicks) return Phase.TELEGRAPH;
        if (elapsedTick < (long) tellTicks + activeTicks) return Phase.ACTIVE;
        if (elapsedTick < totalTicks()) return Phase.RECOVERY;
        return Phase.COMPLETE;
    }

    public enum Phase { TELEGRAPH, ACTIVE, RECOVERY, COMPLETE }
}
