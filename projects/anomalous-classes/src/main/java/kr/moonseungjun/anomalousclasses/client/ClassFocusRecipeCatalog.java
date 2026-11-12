package kr.moonseungjun.anomalousclasses.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import kr.moonseungjun.anomalousclasses.state.FocusTier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Reads the same catalog used to generate the server's vanilla crafting recipe JSON. */
final class ClassFocusRecipeCatalog {
    private static final ResourceLocation CATALOG = ResourceLocation.fromNamespaceAndPath(
            AnomalousClasses.MOD_ID, "focus_recipes.json");

    record Ingredient(List<ResourceLocation> items) {
        Ingredient {
            items = List.copyOf(items);
            if (items.isEmpty()) throw new IllegalArgumentException("focus recipe ingredient has no items");
        }

        ItemStack stack(int alternative) {
            ResourceLocation id = items.get(Math.floorMod(alternative, items.size()));
            Item item = BuiltInRegistries.ITEM.getOptional(id)
                    .orElseThrow(() -> new IllegalStateException("Unknown focus recipe item " + id));
            return new ItemStack(item);
        }
    }

    record Recipe(ClassId classId, FocusTier tier, List<String> pattern, Map<Character, Ingredient> key) {
        Recipe {
            pattern = List.copyOf(pattern);
            key = Map.copyOf(key);
        }

        Ingredient ingredientAt(int row, int column) {
            if (row < 0 || row >= pattern.size() || column < 0 || column >= pattern.get(row).length()) return null;
            char symbol = pattern.get(row).charAt(column);
            return symbol == '.' ? null : key.get(symbol);
        }

        int requiredSlots(char symbol) {
            int count = 0;
            for (String row : pattern) for (int index = 0; index < row.length(); index++) {
                if (row.charAt(index) == symbol) count++;
            }
            return count;
        }

        List<Character> ingredientSymbols() {
            List<Character> symbols = new ArrayList<>();
            for (String row : pattern) for (int index = 0; index < row.length(); index++) {
                char symbol = row.charAt(index);
                if (symbol != '.' && !symbols.contains(symbol)) symbols.add(symbol);
            }
            return List.copyOf(symbols);
        }
    }

    private ClassFocusRecipeCatalog() {}

    static List<Recipe> load(ResourceManager resources) {
        Resource resource = resources.getResource(CATALOG).orElseThrow(
                () -> new IllegalStateException("Missing class focus recipe catalog " + CATALOG));
        try (Reader reader = resource.openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray recipes = root.getAsJsonArray("recipes");
            List<Recipe> result = new ArrayList<>(recipes.size());
            for (int index = 0; index < recipes.size(); index++) {
                JsonObject entry = recipes.get(index).getAsJsonObject();
                ClassId classId = ClassId.fromKey(entry.get("class").getAsString()).orElseThrow();
                FocusTier tier = FocusTier.fromKey(entry.get("tier").getAsString()).orElseThrow();
                JsonArray rows = entry.getAsJsonArray("pattern");
                List<String> pattern = new ArrayList<>(rows.size());
                for (int row = 0; row < rows.size(); row++) pattern.add(rows.get(row).getAsString());

                JsonObject rawKey = entry.getAsJsonObject("key");
                Map<Character, Ingredient> key = new HashMap<>();
                for (Map.Entry<String, com.google.gson.JsonElement> symbol : rawKey.entrySet()) {
                    List<ResourceLocation> itemIds = new ArrayList<>();
                    JsonArray values = symbol.getValue().getAsJsonArray();
                    for (int item = 0; item < values.size(); item++) {
                        itemIds.add(parseLocation(values.get(item).getAsString()));
                    }
                    if (symbol.getKey().length() != 1) throw new IllegalStateException("Invalid focus symbol");
                    key.put(symbol.getKey().charAt(0), new Ingredient(itemIds));
                }
                result.add(new Recipe(classId, tier, pattern, key));
            }
            return List.copyOf(result);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to read class focus recipes", exception);
        }
    }

    static Optional<Recipe> find(List<Recipe> recipes, ClassId classId, FocusTier tier) {
        return recipes.stream().filter(recipe -> recipe.classId() == classId && recipe.tier() == tier).findFirst();
    }

    private static ResourceLocation parseLocation(String value) {
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            throw new IllegalStateException("Malformed focus recipe item id " + value);
        }
        return ResourceLocation.fromNamespaceAndPath(value.substring(0, separator), value.substring(separator + 1));
    }
}
