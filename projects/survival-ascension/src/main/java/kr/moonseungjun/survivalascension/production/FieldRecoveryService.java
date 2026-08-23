package kr.moonseungjun.survivalascension.production;

import kr.moonseungjun.survivalascension.apex.ApexHuntSystem;
import kr.moonseungjun.survivalascension.endgame.AscensionTrialSystem;
import kr.moonseungjun.survivalascension.expedition.ExpeditionIncidentSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Set;

public final class FieldRecoveryService {
    public static final int ARM_RADIUS = 4;
    public static final int DEATH_RADIUS = 96;
    public static final int SUPPLY_CHARGE_COST = 1;

    private FieldRecoveryService() {}

    public static void configure(ServerPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("§a[현장 복귀] §f크리에이티브/관전자 상태에서는 복귀 계약을 설정할 수 없습니다."));
            return;
        }

        FieldRecoveryData data = FieldRecoveryData.get(player);
        if (data.pending(player) != null) {
            if (tryRecoverNow(player, true)) return;
            OutpostData.OutpostEntry replacement = OutpostService.nearestActiveOutpost(player, ARM_RADIUS);
            if (replacement != null) {
                data.rearmPending(player, replacement.dimension(), replacement.pos());
                player.sendSystemMessage(Component.literal("§a[현장 복귀 재지정] §f이전 복귀 지점을 사용할 수 없어 현재 전초기지로 계약을 옮겼습니다. §7추가 보급권 소모 없음"));
                sendStatus(player);
                return;
            }
            player.sendSystemMessage(Component.literal("§a[현장 복귀] §f대기 중인 복귀 지점을 지금 사용할 수 없습니다. §7대상 청크/전초 구조를 복구하거나 활성 전초기지 4블록 안에서 다시 선택하세요."));
            return;
        }

        OutpostData.OutpostEntry outpost = OutpostService.nearestActiveOutpost(player, ARM_RADIUS);
        if (outpost == null) {
            player.sendSystemMessage(Component.literal("§a[현장 복귀] §f활성 전초기지 4블록 안에서 설정해야 합니다."));
            sendStatus(player);
            return;
        }

        FieldRecoveryData.RecoveryPoint armed = data.armed(player);
        if (armed != null) {
            if (armed.dimension().equals(outpost.dimension()) && armed.pos().equals(outpost.pos())) {
                player.sendSystemMessage(Component.literal("§a[현장 복귀] §f이 전초기지가 이미 복귀 거점으로 지정되어 있습니다. §7일반 사망 시 96블록 이내에서 1회 작동"));
                return;
            }
            data.arm(player, outpost.dimension(), outpost.pos());
            player.sendSystemMessage(Component.literal("§a[현장 복귀 재지정] §f보유 중인 1회 복귀 계약을 현재 전초기지로 옮겼습니다. §7추가 보급권 소모 없음"));
            sendStatus(player);
            return;
        }

        ProductionData production = ProductionData.get(player);
        if (!production.consumeSupplyCharge(player)) {
            player.sendSystemMessage(Component.literal("§a[현장 복귀] §f1회 복귀 계약 설정에는 §e현장 보급권 1개§f가 필요합니다."));
            return;
        }
        data.arm(player, outpost.dimension(), outpost.pos());
        player.sendSystemMessage(Component.literal("§a[현장 복귀 계약] §f현재 전초기지를 1회 복귀 거점으로 지정했습니다. §7보급권1 소비 · 일반 사망 반경 " + DEATH_RADIUS + "블록"));
        sendStatus(player);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        FieldRecoveryData data = FieldRecoveryData.get(player);
        FieldRecoveryData.RecoveryPoint armed = data.armed(player);
        if (armed == null || data.pending(player) != null) return;
        if (ExpeditionIncidentSystem.isActive(player) || ApexHuntSystem.isActive(player)
                || AscensionTrialSystem.isActive(player) || OutpostSiegeSystem.isActive(player)) {
            player.sendSystemMessage(Component.literal("§7[현장 복귀] 사건·전초 방어·정점 사냥·승천 시련 중 사망은 복귀 계약을 소비하지 않습니다."));
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!armed.dimension().equals(level.dimension().toString())) return;
        if (armed.pos().distSqr(player.blockPosition()) > DEATH_RADIUS * DEATH_RADIUS) return;
        if (!OutpostData.get(player).isOutpost(player, armed.dimension(), armed.pos())) return;
        if (!OutpostService.isRecoveryOperational(player, level, armed.dimension(), armed.pos())) return;
        if (data.queuePending(player)) {
            player.sendSystemMessage(Component.literal("§a[현장 복귀 예약] §f사망 위치가 지정 전초기지 작전권 안입니다. 부활 후 전초기지로 1회 복귀합니다."));
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        server.execute(() -> tryRecoverNow(player, false));
    }

    public static void sendStatus(ServerPlayer player) {
        FieldRecoveryData data = FieldRecoveryData.get(player);
        FieldRecoveryData.RecoveryPoint pending = data.pending(player);
        FieldRecoveryData.RecoveryPoint armed = data.armed(player);
        if (pending != null) {
            player.sendSystemMessage(Component.literal("§a[현장 복귀] §f복귀 대기 §e" + coords(pending) + " §7· 성공 " + data.recoveries(player) + "회"));
        } else if (armed != null) {
            player.sendSystemMessage(Component.literal("§a[현장 복귀] §f계약 준비 §e" + coords(armed) + " §7· 반경 " + DEATH_RADIUS + " · 성공 " + data.recoveries(player) + "회"));
        } else {
            player.sendSystemMessage(Component.literal("§a[현장 복귀] §f미설정 §7· 활성 전초기지에서 보급권1로 1회 계약 가능 · 성공 " + data.recoveries(player) + "회"));
        }
    }

    private static boolean tryRecoverNow(ServerPlayer player, boolean manual) {
        FieldRecoveryData data = FieldRecoveryData.get(player);
        FieldRecoveryData.RecoveryPoint pending = data.pending(player);
        if (pending == null) return false;
        MinecraftServer server = ((ServerLevel) player.level()).getServer();
        ServerLevel target = findLevel(server, pending.dimension());
        if (target == null || !OutpostData.get(player).isOutpost(player, pending.dimension(), pending.pos())
                || !OutpostService.isRecoveryOperational(player, target, pending.dimension(), pending.pos())) {
            if (manual) player.sendSystemMessage(Component.literal("§a[현장 복귀] §f대상 전초기지가 현재 작동하지 않습니다."));
            return false;
        }
        BlockPos arrival = findSafeArrival(player, target, pending.pos());
        if (arrival == null) {
            if (manual) player.sendSystemMessage(Component.literal("§a[현장 복귀] §f전초기지 주변에 안전한 도착 공간이 없습니다."));
            return false;
        }
        boolean moved = player.teleportTo(target, arrival.getX() + 0.5D, arrival.getY(), arrival.getZ() + 0.5D,
                Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
        if (!moved) {
            if (manual) player.sendSystemMessage(Component.literal("§a[현장 복귀] §f차원 이동이 거부되어 계약을 보존했습니다."));
            return false;
        }
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
        data.completePending(player);
        player.sendSystemMessage(Component.literal("§a[현장 복귀 완료] §f지정 전초기지로 복귀했습니다. §7계약 1회 소진 · 누적 " + data.recoveries(player) + "회"));
        return true;
    }

    private static ServerLevel findLevel(MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().toString().equals(dimension)) return level;
        }
        return null;
    }

    private static BlockPos findSafeArrival(ServerPlayer player, ServerLevel level, BlockPos anchor) {
        for (int radius = 0; radius <= OutpostService.STRUCTURE_RADIUS; radius++) {
            for (int dy = 1; dy >= -1; dy--) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (radius > 0 && Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        BlockPos feet = anchor.offset(dx, dy, dz);
                        if (!level.hasChunkAt(feet) || !level.mayInteract(player, feet)) continue;
                        BlockPos below = feet.below();
                        BlockState floor = level.getBlockState(below);
                        BlockState body = level.getBlockState(feet);
                        BlockState head = level.getBlockState(feet.above());
                        if (!floor.isFaceSturdy(level, below, Direction.UP)) continue;
                        if (!body.getCollisionShape(level, feet).isEmpty() || !head.getCollisionShape(level, feet.above()).isEmpty()) continue;
                        if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) continue;
                        return feet.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static String coords(FieldRecoveryData.RecoveryPoint point) {
        return point.x() + ", " + point.y() + ", " + point.z();
    }
}
