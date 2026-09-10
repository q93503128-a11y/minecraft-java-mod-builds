package dev.moonseungjun.fishinggame.fishing;

import net.minecraft.world.entity.Mob;

final class FishingSession {
    FishingStage stage = FishingStage.WAITING_FOR_HOOK;
    final long castTick;
    final FishingLocation location;
    long biteTick = Long.MAX_VALUE;
    long visualStartTick = Long.MAX_VALUE;
    FishSpecies species;
    Mob visualFish;
    double visualAngle;
    float tension = 0.42f;
    float progress;
    boolean reelHeld;
    int hudCooldown;
    int dryTicks;
    long nextBurstTick = Long.MAX_VALUE;
    int burstTicks;
    float burstStrength;
    double burstHeading;

    FishingSession(long castTick, FishingLocation location) {
        this.castTick = castTick;
        this.location = location;
    }
}
