package kr.countrysidedays.gameplay;

import kr.countrysidedays.registry.ModItems;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Optional;

/** Extra residents, estate workers, rest days and deterministic daily market prices. */
public final class VillageLifeManager {
    private static final Identifier VILLAGER_ID = Identifier.fromNamespaceAndPath("minecraft", "villager");
    private static final String PUBLIC_ROLE_PREFIX = "cd_public_role_";
    private static final String WORKER_OWNER_PREFIX = "cd_worker_owner_";
    private static final String WORKER_ROLE_PREFIX = "cd_worker_role_";
    private static final String FARM_ROLE = "farm";
    private static final String RANCH_ROLE = "ranch";

    private static final long MORNING_START = 2000L;
    private static final long LUNCH_START = 6000L;
    private static final long LUNCH_END = 7200L;
    private static final long SOCIAL_START = 12000L;
    private static final long NIGHT_START = 13500L;

    private static final List<ResidentRole> PUBLIC_ROLES = List.of(
            new ResidentRole("제빵사 미나", "baker", -39, -16, -22, -20, -8, 5,
                    VillagerProfession.FARMER, true),
            new ResidentRole("목수 우진", "carpenter", 39, -16, 21, -20, -6, 5,
                    VillagerProfession.TOOLSMITH, true),
            new ResidentRole("우편배달부 하람", "courier", -45, -37, 0, -25, -4, 5,
                    VillagerProfession.CARTOGRAPHER, false),
            new ResidentRole("어부 세진", "fisher", -43, -37, -42, 36, -2, 5,
                    VillagerProfession.FISHERMAN, false),
            new ResidentRole("정원사 나래", "gardener", -39, 20, 18, 24, 0, 5,
                    VillagerProfession.FARMER, false),
            new ResidentRole("재봉사 유리", "tailor", -45, 9, -32, 8, 2, 5,
                    VillagerProfession.SHEPHERD, true),
            new ResidentRole("선생님 지호", "teacher", 43, -37, -22, -37, 4, 5,
                    VillagerProfession.LIBRARIAN, true),
            new ResidentRole("요리사 다온", "cook", 39, 20, -17, 10, 6, 5,
                    VillagerProfession.BUTCHER, true),
            new ResidentRole("의원 수현", "healer", 43, 9, 21, -37, -8, 7,
                    VillagerProfession.CLERIC, true),
            new ResidentRole("꽃집 주인 봄이", "florist", -43, 9, 25, 24, -5, 7,
                    VillagerProfession.FARMER, true),
            new ResidentRole("대장장이 건우", "smith", 45, 9, 28, 8, -2, 7,
                    VillagerProfession.WEAPONSMITH, true),
            new ResidentRole("도서관지기 은채", "librarian", 45, -37, -19, -37, 2, 7,
                    VillagerProfession.LIBRARIAN, true),
            new ResidentRole("과수원지기 연우", "orchard", -23, 36, 15, 24, 5, 7,
                    VillagerProfession.FARMER, false),
            new ResidentRole("양봉가 초롱", "beekeeper", -21, 36, 25, 24, 8, 7,
                    VillagerProfession.FARMER, false),
            new ResidentRole("낙농 일꾼 태린", "dairy", 20, 36, 36, 36, -5, 9,
                    VillagerProfession.SHEPHERD, false),
            new ResidentRole("양계 농부 민호", "poultry", 22, 36, 46, 40, 5, 9,
                    VillagerProfession.FARMER, false)
    );

    private VillageLifeManager() {
    }

    public static void prepareNewEstate(ServerLevel level, BlockPos origin) {
        for (int x = -26; x <= -8; x++) {
            for (int z = 3; z <= 19; z++) {
                BlockPos cropPos = origin.offset(x, 1, z);
                BlockState state = level.getBlockState(cropPos);
                if (state.is(Blocks.WHEAT) || state.is(Blocks.CARROTS) || state.is(Blocks.POTATOES)) {
                    level.setBlock(cropPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        level.setBlock(
                PlayerEstateLayout.ranchSupplyBarrel(origin),
                Blocks.BARREL.defaultBlockState(),
                Block.UPDATE_ALL
        );
    }

    public static void ensureVillageLife(
            ServerLevel level,
            BlockPos villageOrigin,
            CountrysideWorldData data
    ) {
        for (ResidentRole role : PUBLIC_ROLES) ensurePublicResident(level, villageOrigin, role);
        for (CountrysideWorldData.PlayerEstate estate : data.estates()) {
            ensureEstateWorker(level, estate, FARM_ROLE, PlayerEstateLayout.farmGate(estate.originPos()));
            ensureEstateWorker(level, estate, RANCH_ROLE, PlayerEstateLayout.ranchGate(estate.originPos()));
        }
        refreshMarkets(level, villageOrigin, gameDay(level));
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % 40L != 0L) return;

        CountrysideWorldData data = CountrysideWorldData.get(event.getServer());
        Optional<BlockPos> village = data.homesteadOrigin();
        if (village.isEmpty()) return;
        BlockPos villageOrigin = village.get();

        if (level.getGameTime() % 200L == 0L) ensureVillageLife(level, villageOrigin, data);
        tickPublicResidents(level, villageOrigin);
        tickEstateWorkers(level, villageOrigin, data);
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Villager villager)) return;

        String name = villager.getName().getString();
        if (!isMarketKeeper(name)) return;

        long day = gameDay(player.level());
        if (isHoliday(day)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            player.sendSystemMessage(Component.translatable("message.countrysidedays.market_holiday"));
            return;
        }

        configureDailyMarket(player.level(), villager, name, day);
        player.sendOverlayMessage(Component.translatable(
                "message.countrysidedays.market_price_day",
                day + 1
        ));
    }

    public static boolean isHoliday(long day) {
        return Math.floorMod(day, 7L) == 6L;
    }

    public static int dailyCoinPrice(int base, long day, int salt) {
        int delta = Math.floorMod(Long.hashCode(day * 97L + salt * 31L), 5) - 2;
        return Math.max(1, base + delta);
    }

    public static int dailyInputCount(int base, long day, int salt) {
        int percent = switch (Math.floorMod(Long.hashCode(day * 53L + salt * 17L), 5)) {
            case 0 -> 80;
            case 1 -> 90;
            case 2 -> 100;
            case 3 -> 110;
            default -> 120;
        };
        return Math.max(1, Math.round(base * percent / 100.0F));
    }

    public static boolean isProduceArbitrageSafe(long day, int salt) {
        int salePrice = dailyCoinPrice(3, day, salt);
        int buybackCount = dailyInputCount(20, day, salt + 1);
        int buybackReward = dailyCoinPrice(1, day, salt + 2);
        int cheapestRepurchaseCost = ((buybackCount + 3) / 4) * salePrice;
        return cheapestRepurchaseCost > buybackReward;
    }

    private static void tickPublicResidents(ServerLevel level, BlockPos villageOrigin) {
        long time = Math.floorMod(level.getGameTime(), 24000L);
        long day = gameDay(level);
        for (ResidentRole role : PUBLIC_ROLES) {
            findTagged(level, PUBLIC_ROLE_PREFIX + role.id(), villageOrigin, 110.0)
                    .ifPresent(villager -> {
                        BlockPos target = publicTarget(villageOrigin, role, day, time);
                        boolean stationary = !isHoliday(day)
                                && time >= MORNING_START
                                && time < SOCIAL_START
                                && !isLunch(time)
                                && role.stationaryAtWork();
                        navigate(level, villager, target, stationary ? 0.43 : 0.48, stationary);
                    });
        }
    }

    private static void tickEstateWorkers(
            ServerLevel level,
            BlockPos villageOrigin,
            CountrysideWorldData data
    ) {
        long time = Math.floorMod(level.getGameTime(), 24000L);
        long day = gameDay(level);
        boolean workTime = !isHoliday(day) && time >= MORNING_START && time < SOCIAL_START && !isLunch(time);

        for (CountrysideWorldData.PlayerEstate estate : data.estates()) {
            BlockPos origin = estate.originPos();
            Optional<Villager> farmer = findEstateWorker(level, estate, FARM_ROLE);
            Optional<Villager> rancher = findEstateWorker(level, estate, RANCH_ROLE);

            BlockPos farmTarget = PlayerEstateLayout.farm(origin).offset(2, 0, 0);
            long ranchPhase = Math.floorMod(level.getGameTime() / 200L, 3L);
            BlockPos ranchTarget = ranchPhase == 0L
                    ? PlayerEstateLayout.ranchSupplyBarrel(origin).above()
                    : ranchPhase == 1L
                    ? PlayerEstateLayout.hayFeeder(origin).north()
                    : PlayerEstateLayout.waterTrough(origin).north();

            if (workTime) {
                farmer.ifPresent(v -> navigate(level, v, farmTarget, 0.54, false));
                rancher.ifPresent(v -> navigate(level, v, ranchTarget, 0.54, false));
                if (level.getGameTime() % 200L == 0L) {
                    workFarm(level, estate);
                    workRanch(level, estate);
                }
                continue;
            }

            BlockPos farmOffDuty;
            BlockPos ranchOffDuty;
            if (isHoliday(day) || isLunch(time)) {
                farmOffDuty = villageOrigin.offset(-4, 1, 5);
                ranchOffDuty = villageOrigin.offset(4, 1, 5);
            } else {
                farmOffDuty = PlayerEstateLayout.homeDoor(origin).south();
                ranchOffDuty = PlayerEstateLayout.homeDoor(origin).south(2);
            }
            farmer.ifPresent(v -> navigate(level, v, farmOffDuty, 0.45, false));
            rancher.ifPresent(v -> navigate(level, v, ranchOffDuty, 0.45, false));
        }
    }

    private static BlockPos publicTarget(BlockPos origin, ResidentRole role, long day, long time) {
        if (isHoliday(day)) return origin.offset(role.socialX(), 1, role.socialZ());
        if (time < MORNING_START || time >= NIGHT_START) return origin.offset(role.homeX(), 1, role.homeZ());
        if (isLunch(time) || time >= SOCIAL_START) return origin.offset(role.socialX(), 1, role.socialZ());
        return origin.offset(role.workX(), 1, role.workZ());
    }

    private static boolean isLunch(long time) {
        return time >= LUNCH_START && time < LUNCH_END;
    }

    private static void workFarm(ServerLevel level, CountrysideWorldData.PlayerEstate estate) {
        BlockPos origin = estate.originPos();
        Container storage = containerAt(level, PlayerEstateLayout.farmStorageBarrel(origin));
        if (storage == null) return;

        int offset = Math.floorMod((int) (level.getGameTime() / 200L), 19 * 17);
        for (int i = 0; i < 19 * 17; i++) {
            int index = Math.floorMod(offset + i, 19 * 17);
            int x = -26 + index % 19;
            int z = 3 + index / 19;
            BlockPos cropPos = origin.offset(x, 1, z);
            BlockState crop = level.getBlockState(cropPos);
            if (!crop.hasProperty(BlockStateProperties.AGE_7)
                    || crop.getValue(BlockStateProperties.AGE_7) < 7) continue;

            if (crop.is(Blocks.WHEAT)) {
                storeOrDrop(level, storage, PlayerEstateLayout.farmStorageBarrel(origin),
                        new ItemStack(Items.WHEAT, 1 + level.getRandom().nextInt(3)));
                storeOrDrop(level, storage, PlayerEstateLayout.farmStorageBarrel(origin),
                        new ItemStack(Items.WHEAT_SEEDS, 1 + level.getRandom().nextInt(2)));
            } else if (crop.is(Blocks.CARROTS)) {
                storeOrDrop(level, storage, PlayerEstateLayout.farmStorageBarrel(origin),
                        new ItemStack(Items.CARROT, 2 + level.getRandom().nextInt(3)));
            } else if (crop.is(Blocks.POTATOES)) {
                storeOrDrop(level, storage, PlayerEstateLayout.farmStorageBarrel(origin),
                        new ItemStack(Items.POTATO, 2 + level.getRandom().nextInt(3)));
            } else {
                continue;
            }
            level.setBlock(cropPos, crop.setValue(BlockStateProperties.AGE_7, 0), Block.UPDATE_ALL);
            return;
        }

        for (int i = 0; i < 19 * 17; i++) {
            int index = Math.floorMod(offset + i, 19 * 17);
            int x = -26 + index % 19;
            int z = 3 + index / 19;
            BlockPos cropPos = origin.offset(x, 1, z);
            if (!level.getBlockState(cropPos).isAir()
                    || !level.getBlockState(cropPos.below()).is(Blocks.FARMLAND)) continue;

            if (consumeOne(storage, Items.WHEAT_SEEDS)) {
                level.setBlock(cropPos, Blocks.WHEAT.defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
            if (consumeOne(storage, Items.CARROT)) {
                level.setBlock(cropPos, Blocks.CARROTS.defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
            if (consumeOne(storage, Items.POTATO)) {
                level.setBlock(cropPos, Blocks.POTATOES.defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
        }
    }

    private static void workRanch(ServerLevel level, CountrysideWorldData.PlayerEstate estate) {
        BlockPos origin = estate.originPos();
        BlockPos supplyPos = PlayerEstateLayout.ranchSupplyBarrel(origin);
        Container supply = containerAt(level, supplyPos);
        if (supply == null) return;

        BlockPos feeder = PlayerEstateLayout.hayFeeder(origin);
        if (!level.getBlockState(feeder).is(Blocks.HAY_BLOCK)
                && !level.getBlockState(feeder.above()).is(Blocks.HAY_BLOCK)
                && consumeOne(supply, Blocks.HAY_BLOCK.asItem())) {
            level.setBlock(feeder, Blocks.HAY_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }

        BlockPos trough = PlayerEstateLayout.waterTrough(origin);
        if (!level.getBlockState(trough).is(Blocks.WATER) && consumeOne(supply, Items.WATER_BUCKET)) {
            level.setBlock(trough, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
            storeOrDrop(level, supply, supplyPos, new ItemStack(Items.BUCKET));
        }
    }

    private static Container containerAt(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof Container container ? container : null;
    }

    private static boolean consumeOne(Container container, Item item) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.is(item)) continue;
            stack.shrink(1);
            container.setChanged();
            return true;
        }
        return false;
    }

    private static void storeOrDrop(ServerLevel level, Container container, BlockPos pos, ItemStack incoming) {
        for (int slot = 0; slot < container.getContainerSize() && !incoming.isEmpty(); slot++) {
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
                int moved = Math.min(incoming.getCount(), incoming.getMaxStackSize());
                ItemStack placed = incoming.copy();
                placed.setCount(moved);
                container.setItem(slot, placed);
                incoming.shrink(moved);
                continue;
            }
            if (!existing.is(incoming.getItem()) || existing.getCount() >= existing.getMaxStackSize()) continue;
            int moved = Math.min(incoming.getCount(), existing.getMaxStackSize() - existing.getCount());
            existing.grow(moved);
            incoming.shrink(moved);
        }
        container.setChanged();
        if (!incoming.isEmpty()) Block.popResource(level, pos.above(), incoming);
    }

    private static void ensurePublicResident(ServerLevel level, BlockPos origin, ResidentRole role) {
        String tag = PUBLIC_ROLE_PREFIX + role.id();
        Villager villager = findTagged(level, tag, origin, 110.0).orElse(null);
        if (villager == null) {
            villager = spawnVillager(level, role.name(), origin.offset(role.homeX(), 1, role.homeZ()));
            if (villager != null) villager.addTag(tag);
        }
        if (villager == null) return;
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(level.registryAccess(), role.profession())
                .withLevel(3));
    }

    private static void ensureEstateWorker(
            ServerLevel level,
            CountrysideWorldData.PlayerEstate estate,
            String role,
            BlockPos spawn
    ) {
        if (findEstateWorker(level, estate, role).isPresent()) return;
        String visibleRole = FARM_ROLE.equals(role) ? "농장 일꾼 새봄" : "목장 일꾼 태호";
        Villager villager = spawnVillager(level, visibleRole + " · " + estate.ownerName(), spawn.above());
        if (villager == null) return;
        villager.addTag(WORKER_OWNER_PREFIX + estate.ownerUuid());
        villager.addTag(WORKER_ROLE_PREFIX + role);
        VillagerProfession profession = FARM_ROLE.equals(role)
                ? VillagerProfession.FARMER
                : VillagerProfession.SHEPHERD;
        villager.setVillagerData(villager.getVillagerData()
                .withProfession(level.registryAccess(), profession)
                .withLevel(2));
    }

    private static Optional<Villager> findEstateWorker(
            ServerLevel level,
            CountrysideWorldData.PlayerEstate estate,
            String role
    ) {
        String ownerTag = WORKER_OWNER_PREFIX + estate.ownerUuid();
        String roleTag = WORKER_ROLE_PREFIX + role;
        return level.getEntitiesOfClass(
                Villager.class,
                new AABB(estate.originPos()).inflate(180.0, 24.0, 180.0),
                villager -> villager.entityTags().contains(ownerTag) && villager.entityTags().contains(roleTag)
        ).stream().findFirst();
    }

    private static Optional<Villager> findTagged(
            ServerLevel level,
            String tag,
            BlockPos centre,
            double radius
    ) {
        return level.getEntitiesOfClass(
                Villager.class,
                new AABB(centre).inflate(radius, 24.0, radius),
                villager -> villager.entityTags().contains(tag)
        ).stream().findFirst();
    }

    private static Villager spawnVillager(ServerLevel level, String name, BlockPos pos) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (type == null) return null;
        Entity created = type.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) return null;
        BlockPos safe = nearestWalkable(level, pos);
        villager.setPos(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
        villager.setCustomName(Component.literal(name));
        villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        villager.setInvulnerable(true);
        return level.addFreshEntity(villager) ? villager : null;
    }

    private static void navigate(
            ServerLevel level,
            Villager villager,
            BlockPos requestedTarget,
            double speed,
            boolean stationaryAtTarget
    ) {
        BlockPos target = nearestWalkable(level, requestedTarget);
        if (!isWalkable(level, villager.blockPosition()) && villager.getPose() != Pose.SITTING) {
            villager.getNavigation().stop();
            villager.setPos(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        }
        if (villager.blockPosition().distSqr(target) <= 2.25) {
            villager.getNavigation().stop();
            villager.setPose(Pose.STANDING);
            villager.setNoAi(stationaryAtTarget);
            return;
        }
        villager.setNoAi(false);
        villager.setPose(Pose.STANDING);
        villager.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
    }

    private static BlockPos nearestWalkable(ServerLevel level, BlockPos target) {
        if (isWalkable(level, target)) return target;
        for (int radius = 1; radius <= 5; radius++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        BlockPos candidate = target.offset(dx, dy, dz);
                        if (isWalkable(level, candidate)) return candidate;
                    }
                }
            }
        }
        return target;
    }

    private static boolean isWalkable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && !level.getBlockState(pos.below()).isAir();
    }

    private static long gameDay(ServerLevel level) {
        return Math.max(0L, level.getGameTime() / 24000L);
    }

    private static boolean isMarketKeeper(String name) {
        return RuralNpcManager.FARMER_NAME.equals(name)
                || RuralNpcManager.RANCHER_NAME.equals(name)
                || RuralNpcManager.HALL_KEEPER_NAME.equals(name);
    }

    private static void refreshMarkets(ServerLevel level, BlockPos origin, long day) {
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class,
                new AABB(origin).inflate(110.0, 24.0, 110.0),
                villager -> isMarketKeeper(villager.getName().getString())
        )) {
            configureDailyMarket(level, villager, villager.getName().getString(), day);
        }
    }

    private static void configureDailyMarket(ServerLevel level, Villager villager, String name, long day) {
        MerchantOffers offers = new MerchantOffers();
        if (isHoliday(day)) {
            villager.setOffers(offers);
            return;
        }

        if (RuralNpcManager.FARMER_NAME.equals(name)) {
            offers.add(buyOffer(Items.CARROT, dailyInputCount(20, day, 1), dailyCoinPrice(1, day, 2)));
            offers.add(buyOffer(Items.POTATO, dailyInputCount(20, day, 3), dailyCoinPrice(1, day, 4)));
            offers.add(offer(dailyCoinPrice(1, day, 5), Items.WHEAT_SEEDS, 8));
            offers.add(offer(dailyCoinPrice(3, day, 6), Items.CARROT, 4));
            offers.add(offer(dailyCoinPrice(3, day, 7), Items.POTATO, 4));
            offers.add(offer(dailyCoinPrice(3, day, 8), Blocks.HAY_BLOCK, 1));
            offers.add(offer(dailyCoinPrice(4, day, 9), Items.WATER_BUCKET, 1));
            offers.add(offer(dailyCoinPrice(3, day, 10), Items.HONEY_BOTTLE, 1));
            offers.add(offer(dailyCoinPrice(1, day, 11), Items.BOWL, 4));
        } else if (RuralNpcManager.RANCHER_NAME.equals(name)) {
            offers.add(buyOffer(Items.EGG, dailyInputCount(6, day, 12), dailyCoinPrice(2, day, 13)));
            offers.add(buyOffer(Items.MILK_BUCKET, dailyInputCount(1, day, 14), dailyCoinPrice(3, day, 15)));
            offers.add(buyOffer(Blocks.WOOL.pick(DyeColor.WHITE), dailyInputCount(2, day, 16), dailyCoinPrice(2, day, 17)));
            offers.add(offer(dailyCoinPrice(2, day, 18), Items.WHEAT, 8));
            offers.add(offer(dailyCoinPrice(3, day, 19), Blocks.OAK_FENCE_GATE, 2));
            offers.add(offer(dailyCoinPrice(4, day, 20), Items.LEAD, 1));
            offers.add(offer(dailyCoinPrice(6, day, 21), Items.NAME_TAG, 1));
            offers.add(offer(dailyCoinPrice(4, day, 22), Items.SHEARS, 1));
        } else {
            offers.add(offer(dailyCoinPrice(2, day, 23), Blocks.FLOWER_POT, 2));
            offers.add(offer(dailyCoinPrice(3, day, 24), Blocks.LANTERN, 2));
            offers.add(offer(dailyCoinPrice(4, day, 25), Items.ITEM_FRAME, 2));
            offers.add(offer(dailyCoinPrice(5, day, 26), Items.PAINTING, 1));
            offers.add(offer(dailyCoinPrice(5, day, 27), Blocks.BOOKSHELF, 2));
            offers.add(offer(dailyCoinPrice(3, day, 28), Blocks.CARPET.pick(DyeColor.YELLOW), 8));
            offers.add(offer(dailyCoinPrice(4, day, 29), Blocks.OAK_STAIRS, 12));
        }
        villager.setOffers(offers);
    }

    private static MerchantOffer offer(int coins, ItemLike result, int count) {
        return new MerchantOffer(
                new ItemCost(ModItems.VILLAGE_COIN.get(), coins),
                new ItemStack(result, count),
                999,
                1,
                0.0F
        );
    }

    private static MerchantOffer buyOffer(ItemLike input, int count, int coins) {
        return new MerchantOffer(
                new ItemCost(input, count),
                new ItemStack(ModItems.VILLAGE_COIN.get(), coins),
                999,
                1,
                0.0F
        );
    }

    private record ResidentRole(
            String name,
            String id,
            int homeX,
            int homeZ,
            int workX,
            int workZ,
            int socialX,
            int socialZ,
            VillagerProfession profession,
            boolean stationaryAtWork
    ) {
    }
}
