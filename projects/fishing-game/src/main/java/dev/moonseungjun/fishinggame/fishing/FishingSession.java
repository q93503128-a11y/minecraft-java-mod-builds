package dev.moonseungjun.fishinggame.fishing;

final class FishingSession {
    FishingStage stage = FishingStage.WAITING_FOR_HOOK;
    final long castTick;
    final FishingLocation location;
    long biteTick = Long.MAX_VALUE;
    FishSpecies species;
    float tension = 0.42f;
    float progress;
    boolean reelHeld;
    int hudCooldown;
    int dryTicks;

    FishingSession(long castTick, FishingLocation location) {
        this.castTick = castTick;
        this.location = location;
    }
}
