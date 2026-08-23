package kr.moonseungjun.survivalascension.infrastructure;

import kr.moonseungjun.survivalascension.apex.ApexHuntSystem;
import kr.moonseungjun.survivalascension.endgame.AscensionTrialSystem;
import kr.moonseungjun.survivalascension.expedition.ExpeditionIncidentSystem;
import kr.moonseungjun.survivalascension.production.ProductionService;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class InfrastructureService {
    public static final String ACTION_FUND = "fund";
    public static final String ACTION_STATUS = "status";
    public static final String ALL_PROJECTS = "all";

    private InfrastructureService() {}

    public static void perform(ServerPlayer player, String projectId, String action) {
        if (ACTION_STATUS.equals(action) && ALL_PROJECTS.equals(projectId)) {
            WorldAscensionData world = WorldAscensionData.get(((ServerLevel) player.level()).getServer());
            player.sendSystemMessage(Component.literal("§5[월드 승천] §f단계 §d" + world.stage() + "§7/§f2 §7· §d" + world.stageName()));
            for (InfrastructureProject project : InfrastructureProject.values()) sendStatus(player, project);
            return;
        }
        InfrastructureProject project = InfrastructureProject.fromId(projectId);
        if (project == null) {
            player.sendSystemMessage(Component.literal("§c[인프라] §f알 수 없는 프로젝트입니다."));
            return;
        }
        if (project == InfrastructureProject.INDUSTRIAL_WORKS
                && (ProductionService.ACTION_STATUS.equals(action)
                || ProductionService.ACTION_DISPATCH.equals(action)
                || ProductionService.ACTION_DEPOT_TOGGLE.equals(action)
                || action.startsWith(ProductionService.ACTION_PREFIX))) {
            ProductionService.perform(player, action);
            return;
        }
        if (ACTION_STATUS.equals(action)) { sendStatus(player, project); return; }
        if (ACTION_FUND.equals(action)) { fund(player, project); return; }
        player.sendSystemMessage(Component.literal("§c[인프라] §f알 수 없는 작업입니다."));
    }

    private static void fund(ServerPlayer player, InfrastructureProject project) {
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("§6[인프라] §f크리에이티브/관전자 상태에서는 공동 프로젝트에 자원을 투입할 수 없습니다."));
            return;
        }
        WorldAscensionData world = WorldAscensionData.get(((ServerLevel) player.level()).getServer());
        if (world.stage() < project.requiredWorldStage()) {
            player.sendSystemMessage(Component.literal("§6[인프라] §e" + project.koreanName() + "§f은 월드 승천 §d" + project.requiredWorldStage() + "단계§f부터 건설할 수 있습니다."));
            return;
        }
        InfrastructureData data = InfrastructureData.get(player);
        boolean wasComplete = data.isComplete(project);
        if (wasComplete) {
            if (project == InfrastructureProject.APEX_TRACKING_POST) {
                ApexHuntSystem.tryStart(player);
            } else if (project == InfrastructureProject.INDUSTRIAL_WORKS) {
                ProductionService.sendStatus(player);
            } else if (project == InfrastructureProject.ASCENSION_NEXUS) {
                if (ExpeditionIncidentSystem.isActive(player)) {
                    player.sendSystemMessage(Component.literal("§5[승천 시련] §f진행 중인 §e현장 사건§f을 먼저 끝내거나 실패 처리한 뒤 시작하세요."));
                    return;
                }
                if (ApexHuntSystem.isActive(player)) {
                    player.sendSystemMessage(Component.literal("§5[승천 시련] §f진행 중인 §e정점 사냥§f을 먼저 끝내거나 실패 처리한 뒤 시작하세요."));
                    return;
                }
                AscensionTrialSystem.tryStart(player);
            } else {
                player.sendSystemMessage(Component.literal("§6[인프라] §e" + project.koreanName() + "§f은 이미 완공되었습니다."));
            }
            return;
        }

        int consumed = 0;
        for (int i = 0; i < project.requirements().size(); i++) {
            InfrastructureProject.Requirement requirement = project.requirements().get(i);
            int remaining = data.remaining(project, i);
            if (remaining <= 0) continue;
            int take = Math.min(remaining, countItem(player, requirement.item()));
            if (take <= 0) continue;
            consumeItem(player, requirement.item(), take);
            data.addContribution(project, i, take);
            consumed += take;
        }

        if (consumed <= 0) {
            player.sendSystemMessage(Component.literal("§6[인프라] §f현재 인벤토리에 이 프로젝트가 더 필요로 하는 재료가 없습니다."));
        } else {
            player.sendSystemMessage(Component.literal("§6[인프라] §f" + project.koreanName() + "에 자원 §e" + consumed + "개§f를 투입했습니다."));
        }
        sendStatus(player, project);

        if (!wasComplete && data.isComplete(project)) {
            MinecraftServer server = ((ServerLevel) player.level()).getServer();
            Component message = Component.literal("§6[인프라 완공] §e" + project.koreanName() + " §f— " + project.benefit());
            for (ServerPlayer online : server.getPlayerList().getPlayers()) online.sendSystemMessage(message);
            if (project == InfrastructureProject.INDUSTRIAL_WORKS) {
                player.sendSystemMessage(Component.literal("§3[산업 생산망] §f이제 M → 인프라 → 산업 가공소에서 4계통 생산과 현장 배럴 물류를 사용할 수 있습니다."));
            } else if (project == InfrastructureProject.APEX_TRACKING_POST) {
                player.sendSystemMessage(Component.literal("§4[정점 사냥] §f이제 완수한 원정권 안에서 M → 인프라 → 정점 추적소를 다시 선택하면 그 지역의 정점 강적을 추적합니다."));
            } else if (project == InfrastructureProject.ASCENSION_NEXUS) {
                player.sendSystemMessage(Component.literal("§5[승천 시련] §f이제 M → 인프라 → 승천 중추를 다시 선택하면 반복 시련을 개방할 수 있습니다."));
            }
        }
    }

    public static void sendStatus(ServerPlayer player, InfrastructureProject project) {
        WorldAscensionData world = WorldAscensionData.get(((ServerLevel) player.level()).getServer());
        if (world.stage() < project.requiredWorldStage()) {
            player.sendSystemMessage(Component.literal("§6[인프라] §e" + project.koreanName() + " §c잠김 §7· §f월드 승천 " + project.requiredWorldStage() + "단계 필요"));
            return;
        }
        InfrastructureData data = InfrastructureData.get(player);
        if (data.isComplete(project)) {
            player.sendSystemMessage(Component.literal("§6[인프라] §e" + project.koreanName() + " §a완공 §7· §f" + project.benefit()));
            if (project == InfrastructureProject.INDUSTRIAL_WORKS) {
                player.sendSystemMessage(Component.literal("  §3- 산업 생산망 §f4계통 1세트 → 보급권1 · 보급권으로 실물 출고 또는 배럴 물류 거점 등록"));
            } else if (project == InfrastructureProject.APEX_TRACKING_POST) {
                player.sendSystemMessage(Component.literal("  §4- 정점 사냥 추적 §f메아리8 · 자수정32 · 금32 §7· 완수한 원정권 현지에서 시작"));
            } else if (project == InfrastructureProject.ASCENSION_NEXUS) {
                player.sendSystemMessage(Component.literal("  §5- 승천 시련 입장 §f메아리 조각 32 · 자수정 조각 64 · 드래곤의 숨결 8"));
            }
            return;
        }
        player.sendSystemMessage(Component.literal("§6[인프라] §e" + project.koreanName() + " §7· §f" + project.benefit()));
        for (int i = 0; i < project.requirements().size(); i++) {
            InfrastructureProject.Requirement requirement = project.requirements().get(i);
            int current = data.contributed(project, i);
            player.sendSystemMessage(Component.literal("  §7- §f" + requirement.label() + " §e" + current + "§7/§f" + requirement.amount()));
        }
    }

    private static int countItem(ServerPlayer player, Item item) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static void consumeItem(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        player.getInventory().setChanged();
    }
}
