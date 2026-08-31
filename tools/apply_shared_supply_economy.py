#!/usr/bin/env python3
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
F = ROOT / "projects/frontier-settlement"
S = ROOT / "projects/survival-ascension"


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_required(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if new in text:
        return
    if old not in text:
        raise RuntimeError(f"expected patch anchor missing: {path}: {old[:100]!r}")
    path.write_text(text.replace(old, new), encoding="utf-8")


# ---------------------------------------------------------------------------
# Frontier: physical 54-slot shared depot + persistent position registry.
# ---------------------------------------------------------------------------
content_dir = F / "src/main/java/kr/moonseungjun/frontiersettlement/content"
settlement_dir = F / "src/main/java/kr/moonseungjun/frontiersettlement/settlement"

write(content_dir / "FrontierContent.java", r'''package kr.moonseungjun.frontiersettlement.content;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FrontierContent {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FrontierSettlement.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FrontierSettlement.MOD_ID);
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(FrontierSettlement.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FrontierSettlement.MOD_ID);

    public static final DeferredBlock<SupplyDepotBlock> SUPPLY_DEPOT = BLOCKS.registerBlock(
            "supply_depot",
            SupplyDepotBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.WOOD).pushReaction(PushReaction.BLOCK));

    public static final DeferredItem<BlockItem> SUPPLY_DEPOT_ITEM = ITEMS.registerSimpleBlockItem(SUPPLY_DEPOT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupplyDepotBlockEntity>> SUPPLY_DEPOT_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("supply_depot",
                    () -> BlockEntityType.Builder.of(SupplyDepotBlockEntity::new, SUPPLY_DEPOT.get()).build(null));

    public static final DeferredItem<PioneerMarkerItem> PIONEER_MARKER = ITEMS.registerItem(
            "pioneer_marker",
            PioneerMarkerItem::new,
            properties -> properties.stacksTo(1));

    public static final DeferredHolder<EntityType<?>, EntityType<FrontierSoldierEntity>> FRONTIER_SOLDIER =
            ENTITIES.registerEntityType(
                    "frontier_soldier",
                    FrontierSoldierEntity::new,
                    MobCategory.CREATURE,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<FrontierWorkerEntity>> FRONTIER_WORKER =
            ENTITIES.registerEntityType(
                    "frontier_worker",
                    FrontierWorkerEntity::new,
                    MobCategory.CREATURE,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(10));

    private FrontierContent() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(FrontierContent::onEntityAttributes);
    }

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(FRONTIER_SOLDIER.get(), IronGolem.createAttributes().build());
        event.put(FRONTIER_WORKER.get(), FrontierWorkerEntity.createAttributes().build());
    }
}
''')

write(content_dir / "SupplyDepotBlockEntity.java", r'''package kr.moonseungjun.frontiersettlement.content;

import kr.moonseungjun.frontiersettlement.settlement.SupplyDepotRegistryService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.ValueInput;
import net.minecraft.util.ValueOutput;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SupplyDepotBlockEntity extends BaseContainerBlockEntity {
    public static final int SLOT_COUNT = 54;
    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public SupplyDepotBlockEntity(BlockPos pos, BlockState state) {
        super(FrontierContent.SUPPLY_DEPOT_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.frontier_settlement.supply_depot");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.sixRows(containerId, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        ContainerHelper.saveAllItems(output, items);
        super.saveAdditional(output);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level instanceof ServerLevel serverLevel) {
            SupplyDepotRegistryService.tryRegister(serverLevel, worldPosition);
        }
    }
}
'''.replace('import net.minecraft.util.ProblemReporter;\n', ''))

write(content_dir / "SupplyDepotBlock.java", r'''package kr.moonseungjun.frontiersettlement.content;

import com.mojang.serialization.MapCodec;
import kr.moonseungjun.frontiersettlement.settlement.SupplyDepotRegistryService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class SupplyDepotBlock extends BaseEntityBlock {
    public static final MapCodec<SupplyDepotBlock> CODEC = simpleCodec(SupplyDepotBlock::new);

    public SupplyDepotBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SupplyDepotBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SupplyDepotBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof SupplyDepotBlockEntity depot) {
            SupplyDepotRegistryService.tryRegister(serverLevel, pos);
            player.openMenu(depot);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel) SupplyDepotRegistryService.tryRegister(serverLevel, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof SupplyDepotBlockEntity depot) {
            Containers.dropContents(level, pos, depot);
        }
        SupplyDepotRegistryService.unregister(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
''')

write(settlement_dir / "SharedSupplyDepotData.java", r'''package kr.moonseungjun.frontiersettlement.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SharedSupplyDepotData extends SavedData {
    private static final int MAX_DEPOTS = 32;

    public static final SavedDataType<SharedSupplyDepotData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(FrontierSettlement.MOD_ID, "shared_supply_depots"),
            SharedSupplyDepotData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.listOf().optionalFieldOf("positions", List.of()).forGetter(SharedSupplyDepotData::positionLongs)
            ).apply(instance, SharedSupplyDepotData::new))
    );

    private final List<Long> positions = new ArrayList<>();

    public SharedSupplyDepotData() {}

    private SharedSupplyDepotData(List<Long> positions) {
        Set<Long> unique = new LinkedHashSet<>(positions);
        for (long value : unique) {
            if (this.positions.size() >= MAX_DEPOTS) break;
            this.positions.add(value);
        }
    }

    public static SharedSupplyDepotData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private List<Long> positionLongs() {
        return List.copyOf(positions);
    }

    public List<BlockPos> positions() {
        return positions.stream().map(BlockPos::of).toList();
    }

    public boolean add(BlockPos pos) {
        long packed = pos.asLong();
        if (positions.contains(packed) || positions.size() >= MAX_DEPOTS) return false;
        positions.add(packed);
        setDirty();
        return true;
    }

    public boolean remove(BlockPos pos) {
        boolean removed = positions.remove(pos.asLong());
        if (removed) setDirty();
        return removed;
    }
}
''')

write(settlement_dir / "SupplyDepotRegistryService.java", r'''package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public final class SupplyDepotRegistryService {
    public static final int SETTLEMENT_LINK_RADIUS = 128;

    private SupplyDepotRegistryService() {}

    public static boolean tryRegister(ServerLevel level, BlockPos pos) {
        if (!level.dimension().equals(Level.OVERWORLD)) return false;
        SettlementData settlement = SettlementData.get(level.getServer());
        if (!settlement.founded() || !withinLinkRadius(settlement.centerPos(), pos) || !isDepot(level, pos)) return false;
        return SharedSupplyDepotData.get(level.getServer()).add(pos.immutable());
    }

    public static void unregister(ServerLevel level, BlockPos pos) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        SharedSupplyDepotData.get(level.getServer()).remove(pos);
    }

    public static List<BlockPos> loadedPositions(ServerLevel level, SettlementData settlement) {
        if (!settlement.founded() || !level.dimension().equals(Level.OVERWORLD)) return List.of();
        SharedSupplyDepotData registry = SharedSupplyDepotData.get(level.getServer());
        List<BlockPos> result = new ArrayList<>();
        for (BlockPos pos : registry.positions()) {
            if (!withinLinkRadius(settlement.centerPos(), pos)) {
                registry.remove(pos);
                continue;
            }
            if (!level.hasChunkAt(pos)) continue;
            if (!isDepot(level, pos)) {
                registry.remove(pos);
                continue;
            }
            result.add(pos);
        }
        return result;
    }

    private static boolean isDepot(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(FrontierContent.SUPPLY_DEPOT.get())
                && level.getBlockEntity(pos) instanceof Container;
    }

    private static boolean withinLinkRadius(BlockPos center, BlockPos pos) {
        long dx = (long) center.getX() - pos.getX();
        long dy = (long) center.getY() - pos.getY();
        long dz = (long) center.getZ() - pos.getZ();
        return dx * dx + dy * dy + dz * dz <= (long) SETTLEMENT_LINK_RADIUS * SETTLEMENT_LINK_RADIUS;
    }
}
''')

storage_path = settlement_dir / "SettlementStorageService.java"
storage = storage_path.read_text(encoding="utf-8")
if "activeStoragePositions(ServerLevel" not in storage:
    storage = storage.replace("storagePositions(data)", "activeStoragePositions(level, data)")
    anchor = '''    public static List<BlockPos> storagePositions(SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(constructionOfficeSupplyPositions(data));
        positions.addAll(ordinaryStoragePositions(data));
        return new ArrayList<>(positions);
    }
'''
    addition = anchor + '''
    /** Shared depots are opt-in physical storage. Only currently loaded depots join the authoritative
     * town ledger, so an unloaded optional depot never blocks ordinary settlement costs. */
    private static List<BlockPos> activeStoragePositions(ServerLevel level, SettlementData data) {
        Set<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(SupplyDepotRegistryService.loadedPositions(level, data));
        positions.addAll(storagePositions(data));
        return new ArrayList<>(positions);
    }
'''
    if anchor not in storage:
        raise RuntimeError("SettlementStorageService storagePositions anchor changed")
    storage = storage.replace(anchor, addition)

if "depositPositions(ServerLevel level" not in storage:
    storage = storage.replace("depositPositions(data, stack)", "depositPositions(level, data, stack)")
    storage = storage.replace(
        "private static List<BlockPos> depositPositions(SettlementData data, ItemStack stack)",
        "private static List<BlockPos> depositPositions(ServerLevel level, SettlementData data, ItemStack stack)")
    old = '''        if (SettlementInventory.isWood(stack) || SettlementInventory.isStone(stack)) {
            positions.addAll(constructionOfficeSupplyPositions(data));
        }
        positions.addAll(generalStoragePositions(data));
'''
    new = '''        if (SettlementInventory.isWood(stack) || SettlementInventory.isStone(stack)) {
            positions.addAll(constructionOfficeSupplyPositions(data));
        }
        positions.addAll(SupplyDepotRegistryService.loadedPositions(level, data));
        positions.addAll(generalStoragePositions(data));
'''
    if old not in storage:
        raise RuntimeError("SettlementStorageService deposit anchor changed")
    storage = storage.replace(old, new)
storage_path.write_text(storage, encoding="utf-8")

# Frontier resource JSON/model/recipe/tag.
assets = F / "src/main/resources/assets/frontier_settlement"
data = F / "src/main/resources/data/frontier_settlement"
write(assets / "blockstates/supply_depot.json", '''{\n  "variants": {\n    "": { "model": "frontier_settlement:block/supply_depot" }\n  }\n}\n''')
write(assets / "models/block/supply_depot.json", r'''{
  "ambientocclusion": true,
  "textures": {
    "wood": "minecraft:block/dark_oak_planks",
    "metal": "minecraft:block/exposed_copper",
    "particle": "minecraft:block/dark_oak_planks"
  },
  "elements": [
    {"from":[1,0,1],"to":[15,15,15],"faces":{"down":{"texture":"#wood"},"up":{"texture":"#wood"},"north":{"texture":"#wood"},"south":{"texture":"#wood"},"west":{"texture":"#wood"},"east":{"texture":"#wood"}}},
    {"from":[0,1,0],"to":[16,3,16],"faces":{"down":{"texture":"#metal"},"up":{"texture":"#metal"},"north":{"texture":"#metal"},"south":{"texture":"#metal"},"west":{"texture":"#metal"},"east":{"texture":"#metal"}}},
    {"from":[0,12,0],"to":[16,14,16],"faces":{"down":{"texture":"#metal"},"up":{"texture":"#metal"},"north":{"texture":"#metal"},"south":{"texture":"#metal"},"west":{"texture":"#metal"},"east":{"texture":"#metal"}}},
    {"from":[6.5,3,0],"to":[9.5,12,1],"faces":{"north":{"texture":"#metal"},"south":{"texture":"#metal"},"west":{"texture":"#metal"},"east":{"texture":"#metal"},"up":{"texture":"#metal"},"down":{"texture":"#metal"}}}
  ]
}
''')
write(assets / "items/supply_depot.json", '''{\n  "model": {\n    "type": "minecraft:model",\n    "model": "frontier_settlement:block/supply_depot"\n  }\n}\n''')
write(data / "recipe/supply_depot.json", r'''{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "pattern": ["ICI", "CBC", "ICI"],
  "key": {
    "I": "minecraft:iron_ingot",
    "C": "minecraft:copper_ingot",
    "B": "minecraft:barrel"
  },
  "result": {"count": 1, "id": "frontier_settlement:supply_depot"}
}
''')
write(data / "loot_table/blocks/supply_depot.json", r'''{
  "type": "minecraft:block",
  "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "frontier_settlement:supply_depot"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]
}
''')
write(data / "tags/block/shared_supply_depots.json", '''{\n  "replace": false,\n  "values": ["frontier_settlement:supply_depot"]\n}\n''')

for lang_name, additions in {
    "en_us.json": {
        "block.frontier_settlement.supply_depot": "Shared Supply Depot",
        "container.frontier_settlement.supply_depot": "Shared Supply Depot",
    },
    "ko_kr.json": {
        "block.frontier_settlement.supply_depot": "공용 보급고",
        "container.frontier_settlement.supply_depot": "공용 보급고",
    },
}.items():
    p = assets / "lang" / lang_name
    obj = json.loads(p.read_text(encoding="utf-8"))
    obj.update(additions)
    p.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

# ---------------------------------------------------------------------------
# Survival: same physical categories and soft recognition of Frontier depot.
# ---------------------------------------------------------------------------
compat_dir = S / "src/main/java/kr/moonseungjun/survivalascension/compat"
write(compat_dir / "SharedEconomyCompat.java", r'''package kr.moonseungjun.survivalascension.compat;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class SharedEconomyCompat {
    public enum ResourceCategory {
        WOOD("목재"), STONE("석재"), METAL("금속"), FOOD("식량");
        private final String koreanName;
        ResourceCategory(String koreanName) { this.koreanName = koreanName; }
        public String koreanName() { return koreanName; }
    }

    public static final TagKey<Block> SHARED_SUPPLY_DEPOTS = blockTag("shared_supply_depots");
    private static final TagKey<Item> SETTLEMENT_WOOD = itemTag("settlement_wood");
    private static final TagKey<Item> SETTLEMENT_STONE = itemTag("settlement_stone");
    private static final TagKey<Item> SETTLEMENT_METAL = itemTag("settlement_metal");
    private static final TagKey<Item> SETTLEMENT_FOOD = itemTag("settlement_food");
    private static final TagKey<Item> EXPEDITION_RELICS = itemTag("expedition_relics");
    private static final TagKey<Item> C_INGOTS = common("ingots");
    private static final TagKey<Item> C_RAW_MATERIALS = common("raw_materials");
    private static final TagKey<Item> C_STONES = common("stones");
    private static final TagKey<Item> C_COBBLESTONES = common("cobblestones");
    private static final TagKey<Item> C_FOODS = common("foods");

    private static final int WOOD = 1;
    private static final int STONE = 2;
    private static final int METAL = 4;
    private static final int FOOD = 8;

    private SharedEconomyCompat() {}

    public static boolean matches(ResourceCategory category, ItemStack stack) {
        if (stack.isEmpty() || stack.is(EXPEDITION_RELICS)) return false;
        int mask = 0;
        if (rawWood(stack)) mask |= WOOD;
        if (rawStone(stack)) mask |= STONE;
        if (rawMetal(stack)) mask |= METAL;
        if (rawFood(stack)) mask |= FOOD;
        int expected = switch (category) {
            case WOOD -> WOOD;
            case STONE -> STONE;
            case METAL -> METAL;
            case FOOD -> FOOD;
        };
        return mask == expected;
    }

    public static boolean isLogisticsContainerBlock(BlockState state) {
        return state.is(Blocks.BARREL) || isSharedSupplyDepot(state);
    }

    public static boolean isSharedSupplyDepot(BlockState state) {
        return state.is(SHARED_SUPPLY_DEPOTS);
    }

    private static boolean rawWood(ItemStack stack) {
        return stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS) || stack.is(SETTLEMENT_WOOD);
    }

    private static boolean rawStone(ItemStack stack) {
        return stack.is(Items.STONE) || stack.is(Items.COBBLESTONE) || stack.is(Items.DEEPSLATE)
                || stack.is(Items.COBBLED_DEEPSLATE) || stack.is(Items.ANDESITE) || stack.is(Items.DIORITE)
                || stack.is(Items.GRANITE) || stack.is(Items.TUFF) || stack.is(C_STONES)
                || stack.is(C_COBBLESTONES) || stack.is(SETTLEMENT_STONE);
    }

    private static boolean rawMetal(ItemStack stack) {
        return stack.is(Items.IRON_INGOT) || stack.is(Items.RAW_IRON) || stack.is(Items.COPPER_INGOT)
                || stack.is(Items.RAW_COPPER) || stack.is(Items.GOLD_INGOT) || stack.is(Items.RAW_GOLD)
                || stack.is(C_INGOTS) || stack.is(C_RAW_MATERIALS) || stack.is(SETTLEMENT_METAL);
    }

    private static boolean rawFood(ItemStack stack) {
        return stack.get(DataComponents.FOOD) != null || stack.is(Items.WHEAT) || stack.is(Items.CARROT)
                || stack.is(Items.POTATO) || stack.is(Items.BEETROOT) || stack.is(C_FOODS) || stack.is(SETTLEMENT_FOOD);
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("frontier_settlement", path));
    }

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("frontier_settlement", path));
    }

    private static TagKey<Item> common(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }
}
''')

production_dir = S / "src/main/java/kr/moonseungjun/survivalascension/production"
write(production_dir / "ProductionProgram.java", r'''package kr.moonseungjun.survivalascension.production;

import kr.moonseungjun.survivalascension.compat.SharedEconomyCompat;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public enum ProductionProgram {
    METALWORKS(
            "metalworks", "제련 배치",
            List.of(
                    Input.resource(SharedEconomyCompat.ResourceCategory.METAL, 32),
                    Input.resource(SharedEconomyCompat.ResourceCategory.STONE, 12)
            )),
    TIMBERWORKS(
            "timberworks", "구조재 배치",
            List.of(
                    Input.resource(SharedEconomyCompat.ResourceCategory.WOOD, 32),
                    Input.resource(SharedEconomyCompat.ResourceCategory.STONE, 64),
                    Input.resource(SharedEconomyCompat.ResourceCategory.METAL, 6)
            )),
    PROVISIONS(
            "provisions", "식량 배치",
            List.of(Input.resource(SharedEconomyCompat.ResourceCategory.FOOD, 54))),
    PRECISION(
            "precision", "정밀 부품 배치",
            List.of(
                    Input.resource(SharedEconomyCompat.ResourceCategory.METAL, 30),
                    Input.resource(SharedEconomyCompat.ResourceCategory.STONE, 24)
            ));

    private final String id;
    private final String koreanName;
    private final List<Input> inputs;

    ProductionProgram(String id, String koreanName, List<Input> inputs) {
        this.id = id;
        this.koreanName = koreanName;
        this.inputs = inputs;
    }

    public String id() { return id; }
    public String koreanName() { return koreanName; }
    public List<Input> inputs() { return inputs; }

    public static ProductionProgram fromId(String id) {
        for (ProductionProgram program : values()) if (program.id.equals(id)) return program;
        return null;
    }

    public record Input(SharedEconomyCompat.ResourceCategory category, String label, int amount) {
        public static Input resource(SharedEconomyCompat.ResourceCategory category, int amount) {
            return new Input(category, category.koreanName(), amount);
        }
        public boolean matches(ItemStack stack) { return SharedEconomyCompat.matches(category, stack); }
    }
}
''')

field_path = production_dir / "FieldDepotService.java"
field = field_path.read_text(encoding="utf-8")
if "import kr.moonseungjun.survivalascension.compat.SharedEconomyCompat;" not in field:
    field = field.replace(
        "package kr.moonseungjun.survivalascension.production;\n\n",
        "package kr.moonseungjun.survivalascension.production;\n\nimport kr.moonseungjun.survivalascension.compat.SharedEconomyCompat;\n")
field = field.replace("level.getBlockState(anchor).is(Blocks.BARREL)", "SharedEconomyCompat.isLogisticsContainerBlock(level.getBlockState(anchor))")
field = field.replace("level.getBlockState(pos).is(Blocks.BARREL)", "SharedEconomyCompat.isLogisticsContainerBlock(level.getBlockState(pos))")
if "appendNearbySharedSupplyDepots(player, level, resolved);" not in field:
    anchor = '''        resolved.sort(Comparator.comparingDouble(value -> value.pos().distSqr(player.blockPosition())));
        return resolved.stream().map(ResolvedContainer::container).toList();
    }

    private static boolean isUsableAnchor'''
    replacement = '''        appendNearbySharedSupplyDepots(player, level, resolved);
        resolved.sort(Comparator.comparingDouble(value -> value.pos().distSqr(player.blockPosition())));
        return resolved.stream().map(ResolvedContainer::container).toList();
    }

    private static void appendNearbySharedSupplyDepots(ServerPlayer player, ServerLevel level, List<ResolvedContainer> resolved) {
        final int horizontalRadius = 24;
        final int verticalRadius = 8;
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        for (ResolvedContainer value : resolved) seen.add(value.pos());
        BlockPos origin = player.blockPosition();
        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                if (dx * dx + dz * dz > horizontalRadius * horizontalRadius) continue;
                for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (seen.contains(pos) || !level.hasChunkAt(pos)) continue;
                    if (!SharedEconomyCompat.isSharedSupplyDepot(level.getBlockState(pos))) continue;
                    if (!level.mayInteract(player, pos)) continue;
                    if (!(level.getBlockEntity(pos) instanceof Container container)) continue;
                    BlockPos immutable = pos.immutable();
                    seen.add(immutable);
                    resolved.add(new ResolvedContainer(immutable, container));
                }
            }
        }
    }

    private static boolean isUsableAnchor'''
    if anchor not in field:
        raise RuntimeError("FieldDepotService usableContainers anchor changed")
    field = field.replace(anchor, replacement)
field_path.write_text(field, encoding="utf-8")

# Source-level acceptance checks.
assert "SupplyDepotRegistryService.loadedPositions" in storage_path.read_text(encoding="utf-8")
assert "SharedEconomyCompat.isLogisticsContainerBlock" in field_path.read_text(encoding="utf-8")
assert "Input.resource(SharedEconomyCompat.ResourceCategory.FOOD, 54)" in (production_dir / "ProductionProgram.java").read_text(encoding="utf-8")
print("SHARED SUPPLY ECONOMY PATCH APPLIED")
