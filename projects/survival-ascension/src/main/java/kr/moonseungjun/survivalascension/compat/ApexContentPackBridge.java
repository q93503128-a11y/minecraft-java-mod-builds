package kr.moonseungjun.survivalascension.compat;

import kr.moonseungjun.survivalascension.SurvivalAscension;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Data-only seam for optional content-pack escorts inside Survival-owned Apex hunts.
 *
 * The allowlists live in Survival entity-type tags and every entry is optional. No third-party
 * implementation class or registry ID is linked from Java, so the standalone mod still loads when
 * the content pack is absent. Apex replacement is intentionally limited to one initial escort slot.
 */
public final class ApexContentPackBridge {
    private static final TagKey<EntityType<?>> APEX_ESCORTS_TIER_0 = tag("apex_escorts_tier_0");
    private static final TagKey<EntityType<?>> APEX_ESCORTS_TIER_1 = tag("apex_escorts_tier_1");
    private static final TagKey<EntityType<?>> APEX_ESCORTS_TIER_2 = tag("apex_escorts_tier_2");

    private ApexContentPackBridge() {}

    public static String randomEscortId(RandomSource random, int worldStage) {
        List<String> pool = escortIds(worldStage);
        return pool.isEmpty() ? null : pool.get(random.nextInt(pool.size()));
    }

    public static List<String> escortIds(int worldStage) {
        TagKey<EntityType<?>> tag = escortTag(worldStage);
        List<String> ids = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.ENTITY_TYPE.keySet()) {
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
            if (type == null || type.getCategory() != MobCategory.MONSTER) continue;
            if (!type.builtInRegistryHolder().is(tag)) continue;
            if (type.builtInRegistryHolder().is(Tags.EntityTypes.BOSSES) || blockedPath(id)) continue;
            ids.add(id.toString());
        }
        ids.sort(String::compareTo);
        return List.copyOf(ids);
    }

    public static void onServerStarted(ServerStartedEvent event) {
        for (int tier = 0; tier <= 2; tier++) {
            SurvivalAscension.LOGGER.info("[content-census] apex_escort_tier_{}={}", tier,
                    String.join(",", escortIds(tier)));
        }
    }

    private static TagKey<EntityType<?>> escortTag(int worldStage) {
        return switch (Math.max(0, Math.min(2, worldStage))) {
            case 0 -> APEX_ESCORTS_TIER_0;
            case 1 -> APEX_ESCORTS_TIER_1;
            default -> APEX_ESCORTS_TIER_2;
        };
    }

    private static boolean blockedPath(Identifier id) {
        String path = id.getPath().toLowerCase(java.util.Locale.ROOT);
        for (String blocked : List.of("boss", "curator", "cantor", "guardian", "warden", "anchor", "core", "projectile", "dummy")) {
            if (path.contains(blocked)) return true;
        }
        return false;
    }

    private static TagKey<EntityType<?>> tag(String path) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, path));
    }
}
