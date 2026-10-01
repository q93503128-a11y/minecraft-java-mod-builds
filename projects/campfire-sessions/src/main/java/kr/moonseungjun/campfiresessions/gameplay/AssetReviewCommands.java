package kr.moonseungjun.campfiresessions.gameplay;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Development-only external asset inspection helper.
 *
 * <p>This command is registered only when the explicit JVM property
 * {@code campfiresessions.assetReview=true} is present. Normal Campfire builds
 * never expose the command to players.</p>
 */
public final class AssetReviewCommands {
    public static final String ENABLE_PROPERTY = "campfiresessions.assetReview";
    private static final int MAX_KIT_ITEMS = 27;

    private static final Set<String> CONTENT_NAMESPACES = Set.of(
            "skniro_furniture",
            "peterwolfs_boats_and_ships",
            "croptopia",
            "cookingforblockheads",
            "shroomcraft",
            "sophisticatedbackpacks",
            "travelertoolbelt"
    );

    private static final Map<String, Set<String>> CATEGORY_NAMESPACES = Map.of(
            "furniture", Set.of("skniro_furniture"),
            "kitchen", Set.of("cookingforblockheads", "skniro_furniture"),
            "crops", Set.of("croptopia"),
            "mushrooms", Set.of("shroomcraft"),
            "storage", Set.of("sophisticatedbackpacks", "travelertoolbelt"),
            "boats", Set.of("peterwolfs_boats_and_ships")
    );

    private static final Map<String, List<String>> CATEGORY_HINTS = Map.of(
            "furniture", List.of("chair", "table", "sofa", "couch", "bed", "cabinet", "shelf", "lamp", "desk", "bench", "stool"),
            "kitchen", List.of("kitchen", "fridge", "oven", "sink", "counter", "cabinet", "cooking", "spice", "rack"),
            "crops", List.of("seed", "fruit", "berry", "apple", "tomato", "corn", "rice", "crop", "sapling"),
            "mushrooms", List.of("mushroom", "shroom", "fung", "mycel"),
            "storage", List.of("backpack", "bag", "storage", "pouch", "toolbelt", "tool_belt"),
            "boats", List.of("boat", "ship", "sloop", "schooner", "raft", "canoe", "helm", "sail")
    );

    private AssetReviewCommands() {}

    public static boolean enabled() {
        return Boolean.getBoolean(ENABLE_PROPERTY);
    }

    public static void register(RegisterCommandsEvent event) {
        if (!enabled()) {
            return;
        }

        var root = Commands.literal("campfire_review")
                .executes(context -> status(context));

        for (String category : CATEGORY_HINTS.keySet().stream().sorted().toList()) {
            root.then(Commands.literal(category)
                    .executes(context -> giveCategory(context, category)));
        }

        event.getDispatcher().register(root);
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        long externalItems = BuiltInRegistries.ITEM.entrySet().stream()
                .filter(entry -> CONTENT_NAMESPACES.contains(entry.getKey().identifier().getNamespace()))
                .count();

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Campfire asset review enabled: " + externalItems
                                + " external registry items; categories: "
                                + String.join(", ", CATEGORY_HINTS.keySet().stream().sorted().toList())
                ),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int giveCategory(CommandContext<CommandSourceStack> context, String category) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        List<String> hints = CATEGORY_HINTS.get(category);
        Set<String> namespaces = CATEGORY_NAMESPACES.get(category);
        if (hints == null || namespaces == null) {
            return 0;
        }

        List<Map.Entry<net.minecraft.resources.ResourceKey<Item>, Item>> candidates = new ArrayList<>();
        for (Map.Entry<net.minecraft.resources.ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
            Identifier id = entry.getKey().identifier();
            if (!namespaces.contains(id.getNamespace()) || !matches(id, hints)) {
                continue;
            }
            candidates.add(entry);
        }

        candidates.sort(Comparator.comparing(entry -> entry.getKey().identifier().toString()));

        int added = 0;
        int full = 0;
        for (Map.Entry<net.minecraft.resources.ResourceKey<Item>, Item> entry : candidates) {
            if (added >= MAX_KIT_ITEMS) {
                break;
            }

            ItemStack stack = new ItemStack(entry.getValue());
            if (stack.isEmpty()) {
                continue;
            }

            if (player.addItem(stack)) {
                added++;
            } else {
                full++;
            }
        }

        int available = candidates.size();
        int finalAdded = added;
        int finalFull = full;
        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Campfire review kit '" + category + "': "
                                + finalAdded + " items added"
                                + (available > MAX_KIT_ITEMS ? " / " + available + " matched (first " + MAX_KIT_ITEMS + ")" : "")
                                + (finalFull > 0 ? "; inventory full for " + finalFull : "")
                ),
                false
        );

        return Math.max(1, added);
    }

    private static boolean matches(Identifier id, List<String> hints) {
        String path = id.getPath().toLowerCase(Locale.ROOT);
        return hints.stream().anyMatch(path::contains);
    }
}
