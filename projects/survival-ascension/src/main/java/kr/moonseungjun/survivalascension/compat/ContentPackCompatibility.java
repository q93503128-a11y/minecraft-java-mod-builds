package kr.moonseungjun.survivalascension.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.common.Tags;

/**
 * Small, dependency-free compatibility seam for content supplied by the Survival Ascension pack.
 *
 * External mods are never linked by their implementation classes here. Common Minecraft/NeoForge
 * contracts keep Survival Ascension loadable when any optional content mod is absent and also let
 * future correctly-tagged content participate without another hard-coded registry list.
 */
public final class ContentPackCompatibility {
    private ContentPackCompatibility() {}

    /** Hostile mobs remain the normal combat target; common-tagged bosses are included even when
     * their implementation does not use Minecraft's Enemy marker. */
    public static boolean isCombatTarget(LivingEntity entity) {
        return entity instanceof Enemy || entity.getType().is(Tags.EntityTypes.BOSSES);
    }
}
