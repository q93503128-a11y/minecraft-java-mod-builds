package dev.moonseungjun.fishinggame.progression;

public record RodDefinition(
        int tier,
        String id,
        String displayName,
        int price,
        float strength,
        float controlBonus,
        float luck,
        float lureTimeMultiplier
) {
}
