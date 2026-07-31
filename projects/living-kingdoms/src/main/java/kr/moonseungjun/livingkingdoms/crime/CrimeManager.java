package kr.moonseungjun.livingkingdoms.crime;

import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.profile.OriginProfileManager;
import kr.moonseungjun.livingkingdoms.world.StarterNpcManager;
import kr.moonseungjun.livingkingdoms.world.StarterRealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.List;
import java.util.Set;

/** First playable crime loop: crimes create a local warrant and guards must physically catch the player. */
public final class CrimeManager {
    private static final int ARREST_TIME = 100;
    private static final List<String> GUARD_NAME_MARKERS = List.of(
            "에르덴 변경경비대", "실바나 수림경비대", "카르둠 산문수호자"
    );

    private CrimeManager() {
    }

    public static void handleDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        Entity victim = event.getEntity();

        if (attacker instanceof ServerPlayer player && victim instanceof Villager villager
                && player.level() instanceof ServerLevel level && isLivingRealm(level)) {
            String jurisdiction = jurisdictionAt(villager.blockPosition());
            if (jurisdiction != null) {
                reportCrime(level, player, jurisdiction, 8, "주민 폭행");
            }
        }

        if (attacker instanceof ServerPlayer player && isGuard(victim)
                && player.level() instanceof ServerLevel level) {
            CrimeSavedData data = level.getDataStorage().computeIfAbsent(CrimeSavedData.TYPE);
            CrimeSavedData.CrimeRecord record = data.addResistance(player.getUUID(), level.getGameTime());
            player.sendSystemMessage(Component.literal(
                    "§c[공무집행 방해] §f경비병에게 저항했습니다. 추격 단계가 §e" + record.resistance() + "§f로 상승합니다."
            ));
        }
    }

    public static void handleDeath(LivingDeathEvent event) {
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof ServerPlayer player)
                || !(event.getEntity() instanceof Villager villager)
                || !(player.level() instanceof ServerLevel level)
                || !isLivingRealm(level)) {
            return;
        }
        String jurisdiction = jurisdictionAt(villager.blockPosition());
        if (jurisdiction == null) return;
        StarterNpcManager.markDeadIfStarter(level, villager);
        reportCrime(level, player, jurisdiction, 30, "살인");
    }

    public static void handleBlockBreak(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)
                || player.isCreative()
                || !isLivingRealm(event.getLevel())) {
            return;
        }

        ServerLevel level = event.getLevel();
        String jurisdiction = jurisdictionAt(event.getPos());
        if (jurisdiction == null || isOwnResidence(player, event.getPos())
                || !isPropertyBlock(event.getState().getBlock())) {
            return;
        }
        reportCrime(level, player, jurisdiction, 2, "재산 훼손");
    }

    public static void tickPlayer(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || !isLivingRealm(level)) {
            return;
        }
        if (level.getGameTime() % 20L != 0L) {
            return;
        }

        CrimeSavedData data = level.getDataStorage().computeIfAbsent(CrimeSavedData.TYPE);
        CrimeSavedData.CrimeRecord record = data.record(player.getUUID());
        if (record.wanted() <= 0) {
            return;
        }

        String local = jurisdictionAt(player.blockPosition());
        if (local == null || !record.wantedHere(local)) {
            endLocalPursuit(level, player);
            if (level.getGameTime() % 200L == 0L) {
                player.sendSystemMessage(Component.literal(
                        "§6[수배 유지] §f왕국 경계를 벗어나 현장 추격은 중단됐지만 수배는 남아 있습니다."
                ));
            }
            return;
        }

        List<Mob> guards = guardsNear(level, player, 72.0);
        int wantedTier = Math.max(1, record.wanted() / 10);
        int desired = Math.min(8, 1 + wantedTier / 2 + record.resistance() / 3);
        if (guards.size() < desired && level.getGameTime() % 100L == 0L) {
            spawnGuardWave(level, player, local, desired - guards.size(), wantedTier);
            guards = guardsNear(level, player, 72.0);
        }

        if (!guards.isEmpty() && level.getGameTime() - record.lastCrimeTick() > 400L
                && level.getGameTime() % 400L == 0L) {
            record = data.addResistance(player.getUUID(), level.getGameTime());
            player.sendSystemMessage(Component.literal(
                    "§c[추격 강화] §f계속 저항해 증원 경비대가 호출됩니다. 단계 §e" + record.resistance()
            ));
        }

        Mob closest = closestGuard(guards, player);
        if (closest == null) return;

        if (player.getHealth() <= 6.0F && closest.distanceToSqr(player) <= 12.25) {
            int arrestTicks = record.arrestTicks() + 20;
            data.setArrestTicks(player.getUUID(), arrestTicks);
            for (Mob guard : guards) guard.setTarget(null);
            player.sendSystemMessage(Component.literal(
                    arrestTicks < ARREST_TIME
                            ? "§6[체포 시도] §f경비병이 제압하려 합니다. 달아나거나 다시 저항할 수 있습니다."
                            : "§c[체포됨] §f경비대에게 붙잡혀 구금 시설로 호송됩니다."
            ));
            if (arrestTicks >= ARREST_TIME) {
                completeArrest(level, player, local, data);
            }
        } else if (record.arrestTicks() > 0) {
            data.setArrestTicks(player.getUUID(), 0);
            for (Mob guard : guards) guard.setTarget(player);
            player.sendSystemMessage(Component.literal("§c[체포 저항] §f제압에서 벗어났습니다. 추격이 재개됩니다."));
        } else {
            for (Mob guard : guards) {
                if (guard.getTarget() != player) guard.setTarget(player);
            }
        }
    }

    private static void reportCrime(ServerLevel level, ServerPlayer player, String jurisdiction,
                                    int severity, String description) {
        CrimeSavedData data = level.getDataStorage().computeIfAbsent(CrimeSavedData.TYPE);
        CrimeSavedData.CrimeRecord record = data.addCrime(
                player.getUUID(), jurisdiction, severity, level.getGameTime()
        );
        player.sendSystemMessage(Component.literal(
                "§c[범죄: " + description + "] §f수배도 §e" + record.wanted()
                        + "§f. 경비병이 목격하거나 신고를 받으면 추격합니다."
        ));
    }

    private static void spawnGuardWave(ServerLevel level, ServerPlayer player, String jurisdiction,
                                       int count, int wantedTier) {
        for (int i = 0; i < count; i++) {
            Identifier id = switch (jurisdiction) {
                case "silvana_forest" -> Identifier.fromNamespaceAndPath("minecraft", "skeleton");
                case "kardum_league" -> Identifier.fromNamespaceAndPath("minecraft", "iron_golem");
                default -> Identifier.fromNamespaceAndPath("minecraft", "vindicator");
            };
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if (type == null) continue;
            Entity created = type.create(level, EntitySpawnReason.COMMAND);
            if (!(created instanceof Mob guard)) continue;

            BlockPos spawn = safeGuardSpawn(level, player, i);
            guard.setPos(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            guard.setCustomName(Component.literal(guardName(jurisdiction, wantedTier)));
            guard.setCustomNameVisible(true);
            guard.setPersistenceRequired();
            if (!jurisdiction.equals("kardum_league")) {
                guard.setItemSlot(EquipmentSlot.MAINHAND,
                        new ItemStack(jurisdiction.equals("silvana_forest") ? Items.BOW : Items.IRON_SWORD));
            }
            guard.setTarget(player);
            level.addFreshEntity(guard);
        }
        player.sendSystemMessage(Component.literal("§c[경비대 출동] §f주변 경비병들이 현장으로 접근합니다."));
    }

    private static String guardName(String jurisdiction, int tier) {
        String rank = tier >= 5 ? "정예 " : tier >= 3 ? "증원 " : "";
        return switch (jurisdiction) {
            case "silvana_forest" -> rank + "실바나 수림경비대";
            case "kardum_league" -> rank + "카르둠 산문수호자";
            default -> rank + "에르덴 변경경비대";
        };
    }

    private static boolean isGuard(Entity entity) {
        String name = entity.getName().getString();
        for (String marker : GUARD_NAME_MARKERS) {
            if (name.contains(marker)) return true;
        }
        return false;
    }

    private static BlockPos safeGuardSpawn(ServerLevel level, ServerPlayer player, int index) {
        double angle = (level.getGameTime() * 0.07) + index * 2.399963229728653;
        int x = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * (13 + index * 2));
        int z = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * (13 + index * 2));
        for (int y = 110; y >= 61; y--) {
            BlockPos floor = new BlockPos(x, y, z);
            if (!level.getBlockState(floor).isAir()
                    && level.getBlockState(floor.above()).isAir()
                    && level.getBlockState(floor.above(2)).isAir()) {
                return floor.above();
            }
        }
        return new BlockPos(x, 66, z);
    }

    private static void completeArrest(ServerLevel level, ServerPlayer player, String jurisdiction,
                                       CrimeSavedData data) {
        BlockPos jail = switch (jurisdiction) {
            case "silvana_forest" -> new BlockPos(1268, 67, 28);
            case "kardum_league" -> new BlockPos(-1163, 68, 41);
            default -> new BlockPos(8, 66, -59);
        };
        endLocalPursuit(level, player);
        player.teleportTo(level, jail.getX() + 0.5, jail.getY(), jail.getZ() + 0.5,
                Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
        player.setHealth(Math.max(player.getHealth(), 10.0F));
        data.settleAfterArrest(player.getUUID());
        player.sendSystemMessage(Component.literal(
                "§6[구금] §f현장 체포 뒤 구금 시설로 호송됐습니다. 일부 수배도가 남을 수 있습니다."
        ));
    }

    private static List<Mob> guardsNear(ServerLevel level, ServerPlayer player, double radius) {
        AABB area = player.getBoundingBox().inflate(radius);
        return level.getEntitiesOfClass(Mob.class, area, CrimeManager::isGuard);
    }

    private static Mob closestGuard(List<Mob> guards, ServerPlayer player) {
        Mob best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Mob guard : guards) {
            double distance = guard.distanceToSqr(player);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = guard;
            }
        }
        return best;
    }

    private static void endLocalPursuit(ServerLevel level, ServerPlayer player) {
        for (Mob guard : guardsNear(level, player, 96.0)) {
            guard.setTarget(null);
            guard.discard();
        }
    }

    private static String jurisdictionAt(BlockPos pos) {
        if (distanceSquared(pos, 0, 0) <= 280 * 280) return "erden_kingdom";
        if (distanceSquared(pos, 1240, 35) <= 260 * 260) return "silvana_forest";
        if (distanceSquared(pos, -1170, 38) <= 260 * 260) return "kardum_league";
        return null;
    }

    private static int distanceSquared(BlockPos pos, int x, int z) {
        int dx = pos.getX() - x;
        int dz = pos.getZ() - z;
        return dx * dx + dz * dz;
    }

    private static boolean isLivingRealm(ServerLevel level) {
        return level.dimension().equals(StarterRealmManager.REALM_KEY);
    }

    private static boolean isOwnResidence(ServerPlayer player, BlockPos pos) {
        return OriginProfileManager.profile(player.getUUID()).map(profile -> {
            PlayableOriginCatalog.ResidenceOption home = PlayableOriginCatalog.residences().get(profile.residenceId());
            if (home == null) return false;
            int dx = pos.getX() - home.spawnX();
            int dz = pos.getZ() - home.spawnZ();
            return dx * dx + dz * dz <= 16 * 16;
        }).orElse(false);
    }

    private static boolean isPropertyBlock(Block block) {
        return block == Blocks.OAK_PLANKS || block == Blocks.SPRUCE_PLANKS || block == Blocks.DARK_OAK_PLANKS
                || block == Blocks.BIRCH_PLANKS || block == Blocks.STONE_BRICKS || block == Blocks.DEEPSLATE_BRICKS
                || block == Blocks.POLISHED_DEEPSLATE || block == Blocks.POLISHED_ANDESITE
                || block == Blocks.CALCITE || block == Blocks.GLASS || block == Blocks.GLASS_PANE
                || block == Blocks.IRON_BARS || block == Blocks.BRICKS || block == Blocks.IRON_BLOCK
                || block == Blocks.BARREL || block == Blocks.BLAST_FURNACE || block == Blocks.ANVIL
                || block == Blocks.LANTERN || block == Blocks.SOUL_LANTERN;
    }
}
