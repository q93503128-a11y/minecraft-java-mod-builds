package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Readable late-game silhouettes built from vanilla entity combinations.
 *
 * The raid owner remains the authoritative combat actor. Riders are presentation-only:
 * they have no AI, never own raid rewards/XP, and incoming hits are redirected to the owner.
 */
public final class VillageEnemyCompositionSystem {
    private static final String VISUAL_RIDER_TAG = "villageguardians_visual_rider";

    private VillageEnemyCompositionSystem() {}

    public static void attach(
            ServerLevel level,
            Mob owner,
            VillageEnemyArchetypeSystem.Archetype archetype,
            VillageEnemyArchetypeSystem.AerialRole aerialRole,
            int day,
            int wave,
            int index,
            boolean boss) {
        if (level == null || owner == null || archetype == null || !owner.isAlive()) return;

        Mob rider = null;
        if (boss) {
            rider = switch (archetype) {
                case SIEGE_BEAST -> {
                    Mob visual = EntityTypes.PILLAGER.create(level, EntitySpawnReason.EVENT);
                    equip(visual, Items.CROSSBOW, Items.IRON_HELMET, Items.CHAINMAIL_CHESTPLATE);
                    yield visual;
                }
                case IRON_WARLORD -> {
                    Mob visual = EntityTypes.VINDICATOR.create(level, EntitySpawnReason.EVENT);
                    equip(visual, Items.DIAMOND_AXE, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE);
                    yield visual;
                }
                case PLAGUE_ARCHON -> {
                    Mob visual = EntityTypes.WITCH.create(level, EntitySpawnReason.EVENT);
                    equip(visual, Items.NETHER_STAR, Items.WITHER_SKELETON_SKULL, null);
                    yield visual;
                }
                case DREAD_KNIGHT -> {
                    Mob visual = EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.EVENT);
                    equip(visual, Items.NETHERITE_SWORD, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE);
                    yield visual;
                }
                default -> null;
            };
        } else if (aerialRole != null && day >= 30) {
            rider = switch (aerialRole) {
                case RAIDER -> babyZombie(level, Items.IRON_SWORD, Items.LEATHER_HELMET);
                case BOMBARDIER -> babyHusk(level, Items.FIRE_CHARGE, Items.TNT);
                case HARRIER -> babyZombie(level, Items.CROSSBOW, Items.CHAINMAIL_HELMET);
            };
        } else if (archetype == VillageEnemyArchetypeSystem.Archetype.CAVE_STALKER) {
            rider = EntityTypes.BOGGED.create(level, EntitySpawnReason.EVENT);
            equip(rider, Items.BOW, Items.LEATHER_HELMET);
        } else if (archetype == VillageEnemyArchetypeSystem.Archetype.ZOGLIN_BREACHER) {
            rider = EntityTypes.ZOMBIFIED_PIGLIN.create(level, EntitySpawnReason.EVENT);
            equip(rider, Items.GOLDEN_AXE, Items.GOLDEN_HELMET);
        }

        if (rider == null) return;
        configureVisualRider(rider);
        rider.snapTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), owner.getXRot());
        if (!level.addFreshEntity(rider)) return;
        if (!rider.startRiding(owner)) {
            rider.discard();
            return;
        }
        VillageWorldSystem.markAllowedGameMob(rider);
    }

    public static boolean isVisualRider(Entity entity) {
        return entity != null && entity.entityTags().contains(VISUAL_RIDER_TAG);
    }

    public static Mob combatOwner(Entity entity) {
        if (!isVisualRider(entity)) return null;
        Entity vehicle = entity.getVehicle();
        return vehicle instanceof Mob mob && VillageRaidSystem.isRaidEnemy(mob) ? mob : null;
    }

    public static void remove(Entity owner) {
        if (owner == null) return;
        for (Entity passenger : List.copyOf(owner.getPassengers())) {
            if (!isVisualRider(passenger)) continue;
            if (passenger instanceof Mob mob) VillageWorldSystem.unmarkAllowedGameMob(mob.getUUID());
            passenger.stopRiding();
            passenger.discard();
        }
    }

    public static void cleanupOrphans(MinecraftServer server) {
        if (server == null) return;
        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        double radius = VillageWorldSystem.BATTLEFIELD_RADIUS + 180.0;
        AABB area = new AABB(center).inflate(radius, 160.0, radius);
        for (Mob mob : level.getEntitiesOfClass(
                Mob.class, area, VillageEnemyCompositionSystem::isVisualRider)) {
            VillageWorldSystem.unmarkAllowedGameMob(mob.getUUID());
            mob.stopRiding();
            mob.discard();
        }
    }

    private static Mob babyZombie(ServerLevel level, net.minecraft.world.item.Item mainHand,
                                  net.minecraft.world.item.Item head) {
        Mob mob = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
        if (mob instanceof Zombie zombie) zombie.setBaby(true);
        equip(mob, mainHand, head);
        return mob;
    }

    private static Mob babyHusk(ServerLevel level, net.minecraft.world.item.Item mainHand,
                                net.minecraft.world.item.Item head) {
        Mob mob = EntityTypes.HUSK.create(level, EntitySpawnReason.EVENT);
        if (mob instanceof Zombie zombie) zombie.setBaby(true);
        equip(mob, mainHand, head);
        return mob;
    }

    private static void equip(Mob mob, net.minecraft.world.item.Item mainHand,
                              net.minecraft.world.item.Item head) {
        equip(mob, mainHand, head, null);
    }

    private static void equip(Mob mob, net.minecraft.world.item.Item mainHand,
                              net.minecraft.world.item.Item head,
                              net.minecraft.world.item.Item chest) {
        if (mob == null) return;
        if (mainHand != null) mob.setItemSlot(EquipmentSlot.MAINHAND, mainHand.getDefaultInstance());
        if (head != null) mob.setItemSlot(EquipmentSlot.HEAD, head.getDefaultInstance());
        if (chest != null) mob.setItemSlot(EquipmentSlot.CHEST, chest.getDefaultInstance());
    }

    private static void configureVisualRider(Mob rider) {
        rider.addTag(VISUAL_RIDER_TAG);
        rider.setPersistenceRequired();
        rider.setNoAi(true);
        rider.setSilent(true);
        rider.setCanPickUpLoot(false);
        rider.setCustomNameVisible(false);
    }
}
