package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public final class VillageDefenseSystem {
    private static final String MERCENARY_NAME = "마을 용병";
    private static final int HIRE_IRON_COST = 24;
    private static int towerTicks;

    private VillageDefenseSystem() {
    }

    public static void reset() {
        towerTicks = 0;
    }

    public static boolean recognizeDefenseMob(Mob mob) {
        Component name = mob.getCustomName();
        if (!(mob instanceof IronGolem) || name == null || !MERCENARY_NAME.equals(name.getString())) {
            return false;
        }
        VillageWorldSystem.markAllowedGameMob(mob);
        mob.setPersistenceRequired();
        return true;
    }

    public static String hireMercenary(ServerPlayer player) {
        if (!VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.BARRACKS)) {
            return "병영이 파괴되어 용병을 고용할 수 없습니다.";
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return "현재 월드에서는 용병을 고용할 수 없습니다.";
        }
        int cap = mercenaryCapacity();
        int current = countMercenaries(level);
        if (current >= cap) {
            return "용병 정원이 가득 찼습니다. 현재 " + current + " / " + cap;
        }
        if (countMainInventory(player, Items.IRON_INGOT.getDefaultInstance()) < HIRE_IRON_COST) {
            return "용병 계약에 철 주괴 " + HIRE_IRON_COST + "개가 필요합니다.";
        }

        IronGolem golem = EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.EVENT);
        if (golem == null) {
            return "용병을 소환하지 못했습니다.";
        }
        consumeMainInventory(player, Items.IRON_INGOT.getDefaultInstance(), HIRE_IRON_COST);
        BlockPos barracks = VillageWorldSystem.buildingCenter(VillageProgressionSystem.Building.BARRACKS);
        BlockPos spawn = findSpawn(level, barracks);
        golem.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
        golem.setCustomName(Component.literal(MERCENARY_NAME));
        golem.setCustomNameVisible(true);
        golem.setPlayerCreated(true);
        golem.setPersistenceRequired();
        VillageWorldSystem.markAllowedGameMob(golem);
        if (!level.addFreshEntity(golem)) {
            VillageWorldSystem.unmarkAllowedGameMob(golem.getUUID());
            ItemStack refund = Items.IRON_INGOT.getDefaultInstance();
            refund.setCount(HIRE_IRON_COST);
            player.addItem(refund);
            return "용병 배치에 실패해 철 주괴를 돌려드렸습니다.";
        }
        return "용병 고용 완료 | 현재 " + (current + 1) + " / " + cap;
    }

    public static String status(ServerLevel level) {
        int wallLevel = VillageProgressionSystem.wallLevel();
        String tower = wallLevel <= 0
                ? "방어탑 비활성"
                : "방어탑 Lv." + wallLevel + " · 자동 사격";
        return tower + " | 용병 " + countMercenaries(level) + " / " + mercenaryCapacity();
    }

    public static void tick(MinecraftServer server) {
        if (!VillageRaidSystem.isActive()
                || !VillageProgressionSystem.isOperational(VillageProgressionSystem.Building.WALLS)
                || VillageProgressionSystem.wallLevel() <= 0) {
            towerTicks = 0;
            return;
        }
        int wallLevel = VillageProgressionSystem.wallLevel();
        int interval = Math.max(18, 48 - wallLevel * 6);
        towerTicks++;
        if (towerTicks < interval) {
            return;
        }
        towerTicks = 0;

        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) {
            return;
        }
        int radius = VillageWorldSystem.FORTRESS_RADIUS;
        BlockPos[] towers = {
                center.offset(-radius, 14, -radius),
                center.offset(radius, 14, -radius),
                center.offset(-radius, 14, radius),
                center.offset(radius, 14, radius)
        };
        for (BlockPos tower : towers) {
            Mob target = VillageRaidSystem.nearestActiveEnemy(level, tower, 112.0);
            if (target == null) {
                continue;
            }
            float damage = 3.0f + wallLevel * 1.6f;
            target.hurtServer(level, level.damageSources().magic(), damage);
            if (wallLevel >= 3) {
                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 50 + wallLevel * 10));
            }
            if (wallLevel >= 4) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, wallLevel >= 5 ? 1 : 0));
            }
            level.sendParticles(
                    ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    8, 0.25, 0.25, 0.25, 0.05);
        }
    }

    private static int mercenaryCapacity() {
        return 1 + Math.max(0, VillageProgressionSystem.barracksLevel()) / 2;
    }

    private static int countMercenaries(ServerLevel level) {
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) {
            return 0;
        }
        AABB area = new AABB(center).inflate(VillageWorldSystem.FORTRESS_RADIUS + 12, 48,
                VillageWorldSystem.FORTRESS_RADIUS + 12);
        int count = 0;
        for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, area)) {
            Component name = golem.getCustomName();
            if (golem.isAlive() && name != null && MERCENARY_NAME.equals(name.getString())) {
                count++;
            }
        }
        return count;
    }

    private static BlockPos findSpawn(ServerLevel level, BlockPos origin) {
        for (int radius = 3; radius <= 8; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) {
                        continue;
                    }
                    BlockPos candidate = origin.offset(x, 0, z);
                    if (level.getBlockState(candidate).isAir()
                            && level.getBlockState(candidate.above()).isAir()
                            && !level.getBlockState(candidate.below()).isAir()) {
                        return candidate;
                    }
                }
            }
        }
        return origin.offset(0, 0, 5);
    }

    private static int countMainInventory(ServerPlayer player, ItemStack sample) {
        int count = 0;
        int slots = Math.min(36, player.getInventory().getContainerSize());
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && stack.getItem() == sample.getItem()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void consumeMainInventory(ServerPlayer player, ItemStack sample, int amount) {
        int remaining = amount;
        int slots = Math.min(36, player.getInventory().getContainerSize());
        for (int slot = 0; slot < slots && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || stack.getItem() != sample.getItem()) {
                continue;
            }
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
        player.getInventory().setChanged();
    }
}
