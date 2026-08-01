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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Persistent, low-frequency livestock needs for every private ranch. */
public final class RanchLifeManager {
    private static final String OWNER_PREFIX = "cd_owner_";
    private static final String HUNGER_PREFIX = "cd_hunger_";
    private static final String LAST_DAY_PREFIX = "cd_lastday_";
    private static final String ATE_DAY_PREFIX = "cd_ateday_";
    private static final String DRANK_DAY_PREFIX = "cd_drankday_";
    private static final String BRED_DAY_PREFIX = "cd_bredday_";
    private static final int MAX_HUNGER = 100;
    private static final int DAILY_HUNGER_LOSS = 18;
    private static final int FEED_RECOVERY = 55;
    private static final int MAX_ANIMALS_PER_SPECIES = 8;

    private RanchLifeManager() {
    }

    public static void initializeAnimal(Animal animal, CountrysideWorldData.PlayerEstate estate) {
        animal.addTag(OWNER_PREFIX + estate.ownerUuid());
        setIntTag(animal, HUNGER_PREFIX, MAX_HUNGER);
        setIntTag(animal, LAST_DAY_PREFIX, 0);
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
            tickEstate(level, estate);
        }
    }

    public static void onAnimalInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Animal animal)
                || !(event.getEntity() instanceof ServerPlayer player)) return;

        Optional<String> owner = ownerUuid(animal);
        if (owner.isEmpty()) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!owner.get().equals(player.getUUID().toString())) {
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

    private static void tickEstate(ServerLevel level, CountrysideWorldData.PlayerEstate estate) {
        BlockPos origin = estate.originPos();
        List<Animal> animals = level.getEntitiesOfClass(
                Animal.class,
                ranchBounds(origin),
                animal -> ownerUuid(animal).map(estate.ownerUuid()::equals).orElse(false)
        );
        if (animals.isEmpty()) return;

        int currentDay = (int) Math.max(0L, level.getDayTime() / 24000L);
        for (Animal animal : animals) {
            tickAnimal(level, estate, animal, currentDay);
        }
        breedFedAnimals(level, estate, animals, currentDay);
    }

    private static void tickAnimal(
            ServerLevel level,
            CountrysideWorldData.PlayerEstate estate,
            Animal animal,
            int currentDay
    ) {
        int hunger = getIntTag(animal, HUNGER_PREFIX, MAX_HUNGER);
        int lastDay = getIntTag(animal, LAST_DAY_PREFIX, currentDay);
        if (currentDay > lastDay) {
            int elapsed = Math.min(10, currentDay - lastDay);
            hunger = Math.max(0, hunger - elapsed * DAILY_HUNGER_LOSS);
            setIntTag(animal, HUNGER_PREFIX, hunger);
            setIntTag(animal, LAST_DAY_PREFIX, currentDay);
        }

        boolean hayAvailable = hasHay(level, estate.originPos());
        boolean waterAvailable = hasWater(level, estate.originPos());
        if (hunger <= 82) {
            int ateDay = getIntTag(animal, ATE_DAY_PREFIX, -1);
            int drankDay = getIntTag(animal, DRANK_DAY_PREFIX, -1);
            if (ateDay < currentDay) {
                moveTo(animal, PlayerEstateLayout.hayFeeder(estate.originPos()), 0.55);
                if (hayAvailable && near(animal, PlayerEstateLayout.hayFeeder(estate.originPos()), 3.2)) {
                    consumeOneHay(level, estate.originPos());
                    setIntTag(animal, ATE_DAY_PREFIX, currentDay);
                    ateDay = currentDay;
                }
            } else if (drankDay < currentDay) {
                moveTo(animal, PlayerEstateLayout.waterTrough(estate.originPos()), 0.55);
                if (waterAvailable && near(animal, PlayerEstateLayout.waterTrough(estate.originPos()), 3.0)) {
                    setIntTag(animal, DRANK_DAY_PREFIX, currentDay);
                    drankDay = currentDay;
                }
            }
            if (ateDay == currentDay && drankDay == currentDay) {
                hunger = Math.min(MAX_HUNGER, hunger + FEED_RECOVERY);
                setIntTag(animal, HUNGER_PREFIX, hunger);
            }
        }

        if (hunger <= 0) {
            animal.setCustomName(Component.literal(estate.ownerName() + "의 " + speciesName(animal) + " [굶주림]"));
            animal.setHealth(0.0F);
            return;
        }
        updateName(animal, estate.ownerName(), hunger, hayAvailable, waterAvailable);
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
            if (!animal.isBaby()
                    && getIntTag(animal, HUNGER_PREFIX, 0) >= 80
                    && getIntTag(animal, BRED_DAY_PREFIX, -1) < currentDay) {
                adults.add(animal);
            }
        }

        for (int i = 0; i < adults.size(); i++) {
            Animal first = adults.get(i);
            long sameSpecies = animals.stream().filter(animal -> animal.getType() == first.getType()).count();
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
            baby.setPos(
                    (first.getX() + second.getX()) * 0.5,
                    first.getY(),
                    (first.getZ() + second.getZ()) * 0.5
            );
            initializeAnimal(baby, estate);
            setIntTag(baby, LAST_DAY_PREFIX, currentDay);
            setIntTag(first, BRED_DAY_PREFIX, currentDay);
            setIntTag(second, BRED_DAY_PREFIX, currentDay);
            level.addFreshEntity(baby);
            break;
        }
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

    private static AABB ranchBounds(BlockPos origin) {
        return new AABB(
                origin.getX() + 6.0,
                origin.getY(),
                origin.getZ() + 2.0,
                origin.getX() + 29.0,
                origin.getY() + 10.0,
                origin.getZ() + 28.0
        );
    }

    private static Optional<String> ownerUuid(Entity entity) {
        return entity.getTags().stream()
                .filter(tag -> tag.startsWith(OWNER_PREFIX))
                .map(tag -> tag.substring(OWNER_PREFIX.length()))
                .findFirst();
    }

    private static int getIntTag(Entity entity, String prefix, int fallback) {
        for (String tag : entity.getTags()) {
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
        Set<String> tags = Set.copyOf(entity.getTags());
        for (String tag : tags) {
            if (tag.startsWith(prefix)) entity.removeTag(tag);
        }
        entity.addTag(prefix + value);
    }

    private static void updateName(Animal animal, String ownerName, int hunger,
                                   boolean hayAvailable, boolean waterAvailable) {
        String suffix;
        if (!hayAvailable) suffix = " [먹이 없음]";
        else if (!waterAvailable) suffix = " [물 없음]";
        else if (hunger <= 25) suffix = " [굶주림]";
        else if (hunger <= 55) suffix = " [배고픔]";
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
}
