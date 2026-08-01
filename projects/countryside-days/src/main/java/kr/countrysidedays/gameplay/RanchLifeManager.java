package kr.countrysidedays.gameplay;

import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Persistent, low-frequency livestock needs and production for every private ranch. */
public final class RanchLifeManager {
    private static final String OWNER_PREFIX = "cd_owner_";
    private static final String HUNGER_PREFIX = "cd_hunger_";
    private static final String LAST_DAY_PREFIX = "cd_lastday_";
    private static final String ATE_DAY_PREFIX = "cd_ateday_";
    private static final String DRANK_DAY_PREFIX = "cd_drankday_";
    private static final String BRED_DAY_PREFIX = "cd_bredday_";
    private static final int MAX_HUNGER = 100;
    private static final int DAILY_HUNGER_LOSS = 18;
    private static final int FEED_THRESHOLD = 55;
    private static final int PRODUCTION_HUNGER = 70;
    private static final int MAX_ANIMALS_PER_SPECIES = 8;

    private RanchLifeManager() {
    }

    public static void initializeAnimal(Animal animal, CountrysideWorldData.PlayerEstate estate) {
        int currentDay = animal.level() instanceof ServerLevel level ? gameDay(level) : 0;
        animal.addTag(OWNER_PREFIX + estate.ownerUuid());
        setIntTag(animal, HUNGER_PREFIX, MAX_HUNGER);
        setIntTag(animal, LAST_DAY_PREFIX, currentDay);
        setIntTag(animal, ATE_DAY_PREFIX, -1);
        setIntTag(animal, DRANK_DAY_PREFIX, -1);
        setIntTag(animal, BRED_DAY_PREFIX, -1);
        animal.setPersistenceRequired();
        updateName(animal, estate.ownerName(), MAX_HUNGER, true, true);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % 100L != 0L) return;

        CountrysideWorldData data = CountrysideWorldData.get(event.getServer());
        for (CountrysideWorldData.PlayerEstate estate : data.estates()) {
            tickEstate(level, data, estate);
        }
    }

    public static void onUseBlock(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.BLOCK
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getPlayer() instanceof ServerPlayer player)) return;

        CountrysideWorldData data = CountrysideWorldData.get(level.getServer());
        CountrysideWorldData.PlayerEstate estate = data.estateAt(event.getPos()).orElse(null);
        if (estate == null
                || !event.getPos().equals(PlayerEstateLayout.ranchCollectionBarrel(estate.originPos()))) return;

        event.cancelWithResult(InteractionResult.SUCCESS_SERVER);
        if (!estate.isOwner(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.countrysidedays.ranch_collection_owner_only"));
            return;
        }

        CountrysideWorldData.RanchProducts products = data.claimRanchProducts(player.getUUID());
        if (products.total() <= 0) {
            player.sendOverlayMessage(Component.translatable("message.countrysidedays.ranch_collection_empty"));
            return;
        }

        giveOrDropCount(player, Items.EGG, products.eggs());
        giveOrDropCount(player, Items.MILK_BUCKET, products.milk());
        giveOrDropCount(player, Blocks.WOOL.pick(DyeColor.WHITE), products.wool());
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.ranch_collection_claimed",
                products.eggs(), products.milk(), products.wool()
        ));
    }

    public static void onAnimalInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Animal animal)
                || !(event.getEntity() instanceof ServerPlayer player)) return;

        Optional<String> owner = ownerUuid(animal);
        if (owner.isEmpty()) return;

        if (!owner.get().equals(player.getUUID().toString())) {
            event.setCanceled(true);
            player.sendOverlayMessage(Component.translatable("message.countrysidedays.livestock_not_owner"));
            return;
        }

        int hunger = getIntTag(animal, HUNGER_PREFIX, MAX_HUNGER);
        CountrysideWorldData data = CountrysideWorldData.get(player.level().getServer());
        CountrysideWorldData.PlayerEstate estate = data.estate(player.getUUID()).orElse(null);
        boolean hay = estate != null && hasHay(player.level(), estate.originPos());
        boolean water = estate != null && hasWater(player.level(), estate.originPos());
        player.sendSystemMessage(Component.translatable(
                "message.countrysidedays.livestock_status",
                speciesName(animal), hunger,
                hay ? "충분" : "없음",
                water ? "충분" : "없음"
        ));
        // Owner interaction remains uncancelled so milking, shearing and leads still work.
    }

    public static void onAnimalDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Animal animal)) return;
        Optional<String> owner = ownerUuid(animal);
        if (owner.isEmpty()) return;

        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Player player && !owner.get().equals(player.getUUID().toString())) {
            event.setCanceled(true);
            player.sendOverlayMessage(Component.translatable("message.countrysidedays.livestock_not_owner"));
        }
    }

    private static void tickEstate(
            ServerLevel level,
            CountrysideWorldData data,
            CountrysideWorldData.PlayerEstate estate
    ) {
        BlockPos origin = estate.originPos();
        List<Animal> animals = level.getEntitiesOfClass(
                Animal.class,
                ranchBounds(origin),
                animal -> ownerUuid(animal).map(estate.ownerUuid()::equals).orElse(false)
        );
        if (animals.isEmpty()) return;

        int currentDay = gameDay(level);
        for (Animal animal : animals) applyDailyHunger(animal, currentDay);
        handleSharedMeal(level, estate, animals, currentDay);
        for (Animal animal : animals) {
            int hunger = getIntTag(animal, HUNGER_PREFIX, MAX_HUNGER);
            if (hunger <= 0) {
                animal.setCustomName(Component.literal(
                        estate.ownerName() + "의 " + speciesName(animal) + " [굶주림]"
                ));
                animal.kill(level);
                continue;
            }
            updateName(animal, estate.ownerName(), hunger, hasHay(level, origin), hasWater(level, origin));
        }
        breedFedAnimals(level, estate, animals, currentDay);
        recordDailyProduction(data, estate, animals, currentDay);
    }

    private static void recordDailyProduction(
            CountrysideWorldData data,
            CountrysideWorldData.PlayerEstate estate,
            List<Animal> animals,
            int currentDay
    ) {
        if (currentDay <= 0 || currentDay <= estate.lastRanchProductionDay()) return;
        long chickens = healthyAdults(animals, "chicken");
        long cows = healthyAdults(animals, "cow");
        long sheep = healthyAdults(animals, "sheep");
        int eggs = chickens <= 0 ? 0 : Math.max(1, (int) chickens / 2);
        int milk = cows <= 0 ? 0 : Math.max(1, (int) cows / 2);
        int wool = sheep <= 0 ? 0 : Math.max(1, (int) sheep / 2);
        if (eggs + milk + wool > 0) {
            data.recordRanchProduction(java.util.UUID.fromString(estate.ownerUuid()), currentDay, eggs, milk, wool);
        }
    }

    private static long healthyAdults(List<Animal> animals, String species) {
        return animals.stream()
                .filter(Entity::isAlive)
                .filter(animal -> !animal.isBaby())
                .filter(animal -> animal.getType().toString().contains(species))
                .filter(animal -> getIntTag(animal, HUNGER_PREFIX, 0) >= PRODUCTION_HUNGER)
                .count();
    }

    private static void applyDailyHunger(Animal animal, int currentDay) {
        int hunger = getIntTag(animal, HUNGER_PREFIX, MAX_HUNGER);
        int lastDay = getIntTag(animal, LAST_DAY_PREFIX, currentDay);
        if (currentDay <= lastDay) return;
        int elapsed = Math.min(10, currentDay - lastDay);
        setIntTag(animal, HUNGER_PREFIX, Math.max(0, hunger - elapsed * DAILY_HUNGER_LOSS));
        setIntTag(animal, LAST_DAY_PREFIX, currentDay);
    }

    private static void handleSharedMeal(
            ServerLevel level,
            CountrysideWorldData.PlayerEstate estate,
            List<Animal> animals,
            int currentDay
    ) {
        List<Animal> hungry = animals.stream()
                .filter(Entity::isAlive)
                .filter(animal -> getIntTag(animal, HUNGER_PREFIX, MAX_HUNGER) <= FEED_THRESHOLD)
                .toList();
        if (hungry.isEmpty()) return;

        BlockPos feeder = PlayerEstateLayout.hayFeeder(estate.originPos());
        boolean groupAte = hungry.stream().allMatch(animal -> getIntTag(animal, ATE_DAY_PREFIX, -1) == currentDay);
        if (!groupAte) {
            hungry.forEach(animal -> moveTo(animal, feeder, 0.55));
            boolean reachedFood = hungry.stream().anyMatch(animal -> near(animal, feeder, 3.2));
            if (reachedFood && hasHay(level, estate.originPos())) {
                consumeOneHay(level, estate.originPos());
                hungry.forEach(animal -> setIntTag(animal, ATE_DAY_PREFIX, currentDay));
                groupAte = true;
            }
        }

        if (!groupAte) return;
        BlockPos trough = PlayerEstateLayout.waterTrough(estate.originPos());
        boolean groupDrank = hungry.stream().allMatch(animal -> getIntTag(animal, DRANK_DAY_PREFIX, -1) == currentDay);
        if (!groupDrank) {
            hungry.forEach(animal -> moveTo(animal, trough, 0.55));
            boolean reachedWater = hungry.stream().anyMatch(animal -> near(animal, trough, 3.0));
            if (reachedWater && hasWater(level, estate.originPos())) {
                consumeWater(level, estate.originPos());
                hungry.forEach(animal -> setIntTag(animal, DRANK_DAY_PREFIX, currentDay));
                groupDrank = true;
            }
        }

        if (groupDrank) hungry.forEach(animal -> setIntTag(animal, HUNGER_PREFIX, MAX_HUNGER));
    }

    private static void breedFedAnimals(
            ServerLevel level,
            CountrysideWorldData.PlayerEstate estate,
            List<Animal> animals,
            int currentDay
    ) {
        if (currentDay <= 0 || currentDay % 3 != 0) return;
        List<Animal> adults = new ArrayList<>();
        for (Animal animal : animals) {
            if (animal.isAlive()
                    && !animal.isBaby()
                    && getIntTag(animal, HUNGER_PREFIX, 0) >= 80
                    && getIntTag(animal, BRED_DAY_PREFIX, -1) < currentDay) {
                adults.add(animal);
            }
        }

        for (int i = 0; i < adults.size(); i++) {
            Animal first = adults.get(i);
            long sameSpecies = animals.stream()
                    .filter(Entity::isAlive)
                    .filter(animal -> animal.getType() == first.getType())
                    .count();
            if (sameSpecies >= MAX_ANIMALS_PER_SPECIES) continue;
            Animal second = adults.stream()
                    .skip(i + 1L)
                    .filter(animal -> animal.getType() == first.getType())
                    .findFirst()
                    .orElse(null);
            if (second == null) continue;

            AgeableMob child = first.getBreedOffspring(level, second);
            if (!(child instanceof Animal baby)) continue;
            baby.setAge(AgeableMob.BABY_START_AGE);
            baby.setPos((first.getX() + second.getX()) * 0.5, first.getY(),
                    (first.getZ() + second.getZ()) * 0.5);
            initializeAnimal(baby, estate);
            setIntTag(baby, LAST_DAY_PREFIX, currentDay);
            setIntTag(first, BRED_DAY_PREFIX, currentDay);
            setIntTag(second, BRED_DAY_PREFIX, currentDay);
            level.addFreshEntity(baby);
            break;
        }
    }

    private static int gameDay(ServerLevel level) {
        return (int) Math.max(0L, level.getGameTime() / 24000L);
    }

    private static void moveTo(Animal animal, BlockPos target, double speed) {
        if (near(animal, target, 2.0)) return;
        animal.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, speed);
    }

    private static boolean near(Animal animal, BlockPos target, double distance) {
        return animal.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5)
                <= distance * distance;
    }

    private static boolean hasHay(ServerLevel level, BlockPos origin) {
        BlockPos feeder = PlayerEstateLayout.hayFeeder(origin);
        return level.getBlockState(feeder).is(Blocks.HAY_BLOCK)
                || level.getBlockState(feeder.above()).is(Blocks.HAY_BLOCK);
    }

    private static void consumeOneHay(ServerLevel level, BlockPos origin) {
        BlockPos feeder = PlayerEstateLayout.hayFeeder(origin);
        if (level.getBlockState(feeder.above()).is(Blocks.HAY_BLOCK)) {
            level.setBlock(feeder.above(), Blocks.AIR.defaultBlockState(), 3);
        } else if (level.getBlockState(feeder).is(Blocks.HAY_BLOCK)) {
            level.setBlock(feeder, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static boolean hasWater(ServerLevel level, BlockPos origin) {
        return level.getBlockState(PlayerEstateLayout.waterTrough(origin)).is(Blocks.WATER);
    }

    private static void consumeWater(ServerLevel level, BlockPos origin) {
        level.setBlock(PlayerEstateLayout.waterTrough(origin), Blocks.AIR.defaultBlockState(), 3);
    }

    private static AABB ranchBounds(BlockPos origin) {
        return new AABB(
                origin.getX() + 6.0, origin.getY(), origin.getZ() + 2.0,
                origin.getX() + 29.0, origin.getY() + 10.0, origin.getZ() + 28.0
        );
    }

    private static Optional<String> ownerUuid(Entity entity) {
        return entity.entityTags().stream()
                .filter(tag -> tag.startsWith(OWNER_PREFIX))
                .map(tag -> tag.substring(OWNER_PREFIX.length()))
                .findFirst();
    }

    private static int getIntTag(Entity entity, String prefix, int fallback) {
        for (String tag : entity.entityTags()) {
            if (!tag.startsWith(prefix)) continue;
            try {
                return Integer.parseInt(tag.substring(prefix.length()));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static void setIntTag(Entity entity, String prefix, int value) {
        Set<String> tags = Set.copyOf(entity.entityTags());
        for (String tag : tags) if (tag.startsWith(prefix)) entity.removeTag(tag);
        entity.addTag(prefix + value);
    }

    private static void updateName(
            Animal animal,
            String ownerName,
            int hunger,
            boolean hayAvailable,
            boolean waterAvailable
    ) {
        String suffix;
        if (hunger <= 15) suffix = " [굶주림]";
        else if (hunger <= 35) suffix = " [매우 배고픔]";
        else if (hunger <= FEED_THRESHOLD && !hayAvailable) suffix = " [먹이 없음]";
        else if (hunger <= FEED_THRESHOLD && !waterAvailable) suffix = " [물 없음]";
        else if (hunger <= FEED_THRESHOLD) suffix = " [배고픔]";
        else suffix = "";
        animal.setCustomName(Component.literal(ownerName + "의 " + speciesName(animal) + suffix));
        animal.setCustomNameVisible(!suffix.isEmpty());
    }

    private static String speciesName(Animal animal) {
        String id = animal.getType().toString();
        if (id.contains("cow")) return "소";
        if (id.contains("sheep")) return "양";
        if (id.contains("chicken")) return "닭";
        return "가축";
    }

    private static void giveOrDropCount(ServerPlayer player, ItemLike item, int count) {
        if (count <= 0) return;
        ItemStack sample = new ItemStack(item);
        int maxStackSize = Math.max(1, sample.getMaxStackSize());
        int remaining = count;
        while (remaining > 0) {
            int amount = Math.min(maxStackSize, remaining);
            giveOrDrop(player, new ItemStack(item, amount));
            remaining -= amount;
        }
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
