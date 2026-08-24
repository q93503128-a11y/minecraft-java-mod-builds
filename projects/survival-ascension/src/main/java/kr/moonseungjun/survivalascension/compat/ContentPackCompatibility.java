package kr.moonseungjun.survivalascension.compat;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.common.Tags;

/**
 * Small, dependency-free compatibility seam for content supplied by the Survival Ascension pack.
 *
 * External mods are never linked by their implementation classes here. Common Minecraft/NeoForge
 * contracts plus Survival-owned data tags keep Survival Ascension loadable when optional content is
 * absent and let future compatible content participate without another hard-coded registry list.
 */
public final class ContentPackCompatibility {
    private static final TagKey<EntityType<?>> EXPEDITION_MAJOR_TARGETS = TagKey.create(
            Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, "expedition_major_targets")
    );

    private ContentPackCompatibility() {}

    /** Hostile mobs remain the normal combat target; common-tagged bosses and Survival-tagged
     * major targets are included even when their implementation does not use Minecraft's Enemy marker. */
    public static boolean isCombatTarget(LivingEntity entity) {
        return entity instanceof Enemy
                || entity.getType().builtInRegistryHolder().is(Tags.EntityTypes.BOSSES)
                || isMajorExpeditionTarget(entity);
    }

    /** Data-driven major-target contract. Optional content IDs live in datapack JSON, never here. */
    public static boolean isMajorExpeditionTarget(LivingEntity entity) {
        return entity.getType().builtInRegistryHolder().is(EXPEDITION_MAJOR_TARGETS);
    }
}
