package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.augment.active.AuthorityPowerCatalog;
import dev.moonseungjun.augmentationsurvival.augment.active.AuthorityTheftService;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Optional-mod-safe authority icons. Uses a registered boss spawn egg when the mod provides one. */
public final class AuthorityPowerIconsClient {
    private static final Map<String, String> ENTITY_IDS = new HashMap<>();
    private static final Map<String, net.minecraft.world.item.Item> ICON_ITEMS = new HashMap<>();

    private AuthorityPowerIconsClient() { }

    public static String entityId(AuthorityPowerCatalog.Entry entry) {
        return ENTITY_IDS.computeIfAbsent(entry.id(), ignored -> resolveEntityId(entry));
    }

    private static String resolveEntityId(AuthorityPowerCatalog.Entry entry) {
        String id = entry.id();
        if (id.startsWith(AuthorityTheftService.FALLBACK_PREFIX))
            return id.substring(AuthorityTheftService.FALLBACK_PREFIX.length());
        String namespace = switch (entry.group()) {
            case "Chrono Dawn" -> "chronodawn";
            case "World Bosses" -> "worldbosses";
            case "MCEndgame" -> "mcendgame";
            default -> "minecraft";
        };
        String bossName = normalize(entry.bossName());
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (key == null || !namespace.equals(key.getNamespace())) continue;
            String path = normalize(key.getPath());
            if (path.equals(bossName) || path.contains(bossName) || bossName.contains(path)) return key.toString();
        }
        return "";
    }

    public static ItemStack stack(AuthorityPowerCatalog.Entry entry) {
        return new ItemStack(ICON_ITEMS.computeIfAbsent(entry.id(), ignored -> resolveIconItem(entry)));
    }

    private static net.minecraft.world.item.Item resolveIconItem(AuthorityPowerCatalog.Entry entry) {
        String id = entry.id();
        if (id.startsWith(AuthorityTheftService.FALLBACK_PREFIX)) {
            ItemStack fallback = EntityIconClient.stack(id.substring(AuthorityTheftService.FALLBACK_PREFIX.length()));
            if (!fallback.is(Items.EGG)) return fallback.getItem();
            return Items.NETHER_STAR;
        }

        String entity = entityId(entry);
        if (!entity.isBlank()) {
            ItemStack egg = EntityIconClient.stack(entity);
            if (!egg.is(Items.EGG)) return egg.getItem();
        }

        return switch (id) {
            case "vanilla_wither" -> Items.WITHER_SKELETON_SKULL;
            case "vanilla_ender_dragon" -> Items.DRAGON_HEAD;
            case "vanilla_warden" -> Items.SCULK_CATALYST;
            case "vanilla_elder_guardian" -> Items.PRISMARINE_BRICKS;
            case "chronodawn_time_guardian", "chronodawn_time_tyrant", "chronodawn_clockwork_colossus" -> Items.CLOCK;
            case "chronodawn_chronos_warden" -> Items.IRON_AXE;
            case "chronodawn_temporal_phantom" -> Items.PHANTOM_MEMBRANE;
            case "chronodawn_entropy_keeper" -> Items.FERMENTED_SPIDER_EYE;
            case "worldbosses_sylvaron" -> Items.MOSS_BLOCK;
            case "worldbosses_aureon", "worldbosses_storm_griffin" -> Items.TRIDENT;
            case "worldbosses_nekhraz" -> Items.SAND;
            case "worldbosses_varkhul", "worldbosses_dragon_mother" -> Items.FIRE_CHARGE;
            case "worldbosses_morvath" -> Items.SOUL_LANTERN;
            case "worldbosses_elyndra", "worldbosses_shadow_king" -> Items.ENDER_EYE;
            case "worldbosses_astrael" -> Items.IRON_CHAIN;
            case "worldbosses_shadow_weaver" -> Items.STRING;
            case "mcendgame_arachne" -> Items.SPIDER_EYE;
            case "mcendgame_bonecrusher" -> Items.BONE;
            case "mcendgame_elf_duelist" -> Items.IRON_SWORD;
            case "mcendgame_beakburn" -> Items.BLAZE_POWDER;
            case "mcendgame_beastweaver" -> Items.VINE;
            default -> Items.NETHER_STAR;
        };
    }

    private static String normalize(String value) {
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
