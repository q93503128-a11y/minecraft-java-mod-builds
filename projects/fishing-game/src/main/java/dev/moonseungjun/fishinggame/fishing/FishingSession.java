package dev.moonseungjun.fishinggame.fishing;

final class FishingSession {
    FishingStage stage = FishingStage.WAITING_FOR_HOOK;
    long castTick;
    long biteTick;
    FishSpecies species;
    float tension = 0.42f;
    float progress;
    int reelImpulseTicks;
    int hudCooldown;

    FishingSession(long castTick, long biteTick) {
        this.castTick = castTick;
        this.biteTick = biteTick;
    }
}
