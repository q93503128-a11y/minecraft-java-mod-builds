package kr.moonseungjun.survivalascension.infrastructure;

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
            for (InfrastructureProject project : InfrastructureProject.values()) sendStatus(player, project);
            return;
        }
        InfrastructureProject project = InfrastructureProject.fromId(projectId);
        if (project == null) {
            player.sendSystemMessage(Component.literal("§c[인프라] §f알 수 없는 프로젝트입니다."));
            return;
        }
        if (ACTION_STATUS.equals(action)) {
            sendStatus(player, project);
            return;
        }
        if (ACTION_FUND.equals(action)) {
            fund(player, project);
            return;
        }
        player.sendSystemMessage(Component.literal("§c[인프라] §f알 수 없는 작업입니다."));
    }

    private static void fund(ServerPlayer player, InfrastructureProject project) {
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("§6[인프라] §f크리에이티브/관전자 상태에서는 공동 프로젝트에 자원을 투입할 수 없습니다."));
            return;
        }
        InfrastructureData data = InfrastructureData.get(player);
        boolean wasComplete = data.isComplete(project);
        if (wasComplete) {
            player.sendSystemMessage(Component.literal("§6[인프라] §e" + project.koreanName() + "§f은 이미 완공되었습니다."));
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
        }
    }

    public static void sendStatus(ServerPlayer player, InfrastructureProject project) {
        InfrastructureData data = InfrastructureData.get(player);
        if (data.isComplete(project)) {
            player.sendSystemMessage(Component.literal("§6[인프라] §e" + project.koreanName() + " §a완공 §7· §f" + project.benefit()));
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
