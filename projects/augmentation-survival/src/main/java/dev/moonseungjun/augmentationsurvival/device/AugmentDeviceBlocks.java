package dev.moonseungjun.augmentationsurvival.device;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Registry for the legacy placed devices and their portable player-facing items. */
public final class AugmentDeviceBlocks {
    public static final BlockItemId AUGMENT_DEVICE_ID = createId("augment_device");
    public static final BlockItemId ADVANCED_AUGMENT_DEVICE_ID = createId("advanced_augment_device");

    public static final Block AUGMENT_DEVICE = register(
            AUGMENT_DEVICE_ID,
            false,
            properties -> new AugmentDeviceBlock(false, properties),
            BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL)
    );
    public static final Block ADVANCED_AUGMENT_DEVICE = register(
            ADVANCED_AUGMENT_DEVICE_ID,
            true,
            properties -> new AugmentDeviceBlock(true, properties),
            BlockBehaviour.Properties.of().strength(4.5F).sound(SoundType.METAL)
    );

    private AugmentDeviceBlocks() {
    }

    public static void initialize() {
        // Static initialization performs registration.
    }

    private static BlockItemId createId(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, path);
        return BlockItemId.create(id, id);
    }

    private static Block register(
            BlockItemId id,
            boolean advanced,
            Function<BlockBehaviour.Properties, Block> blockFactory,
            BlockBehaviour.Properties properties
    ) {
        Block block = registerBlock(id.block(), blockFactory, properties);
        BlockItem blockItem = new PortableAugmentDeviceItem(
                block,
                advanced,
                new Item.Properties().useBlockDescriptionPrefix().setId(id.item())
        );
        Registry.register(BuiltInRegistries.ITEM, id.item(), blockItem);
        return block;
    }

    private static Block registerBlock(
            ResourceKey<Block> id,
            Function<BlockBehaviour.Properties, Block> blockFactory,
            BlockBehaviour.Properties properties
    ) {
        Block block = blockFactory.apply(properties.setId(id));
        return Registry.register(BuiltInRegistries.BLOCK, id, block);
    }
}
