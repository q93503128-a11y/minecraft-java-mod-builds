package kr.moonseungjun.villageguardians;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles local/high-frequency actions before legacy fallbacks can reopen obsolete screens.
 * Navigation stays immediate; cheap repeatable supply purchases refresh the shop without a result modal.
 */
public final class VillageLocalActionSystem {
    private VillageLocalActionSystem() {}

    public static boolean handle(ServerPlayer player, String action) {
        if (player == null || action == null) return false;

        if (requiresSiegeCommandAccess(action) && !VillageLocationRules.isNearTownHall(player)) {
            player.sendSystemMessage(Component.literal("§c성벽·포탑 관리 동작은 마을 회관 지휘대 근처에서만 실행할 수 있습니다."));
            return true;
        }

        if (action.startsWith("facility:")) {
            VillageProgressionSystem.Building building = VillageProgressionSystem.Building.fromId(
                    action.substring("facility:".length()));
            if (building == null) {
                player.sendSystemMessage(Component.literal("§c알 수 없는 시설입니다."));
            } else if (building == VillageProgressionSystem.Building.WALLS) {
                VillageSiegeCommandUi.open(player);
            } else {
                VillageUiController.openBuilding(player, building);
            }
            return true;
        }

        // Compatibility guard: old clients or stale saved UI actions must never reopen the retired
        // fixed-corner-tower production screen. All such actions route to the phase-2 siege command surface.
        if (action.equals("open_tower_control") || action.equals("tower_status")
                || action.startsWith("tower_open:") || action.startsWith("tower_branch:")
                || action.startsWith("tower_upgrade:")) {
            VillageSiegeCommandUi.open(player);
            return true;
        }

        if (action.equals("siege_command")) { VillageSiegeCommandUi.open(player); return true; }
        if (action.equals("siege_turret_catalog")) { VillageSiegeCommandUi.openTurretCatalog(player); return true; }
        if (action.equals("siege_turret_list")) { VillageSiegeCommandUi.openTurretList(player); return true; }
        if (action.equals("siege_turret_repair_all")) {
            player.sendSystemMessage(Component.literal("§b" + VillagePlacedTurretSystem.repairAll(player)));
            VillageSiegeCommandUi.openTurretList(player); return true;
        }
        if (action.equals("siege_turret_cancel")) {
            player.sendSystemMessage(Component.literal("§b" + VillagePlacedTurretSystem.cancelPlacement(player)));
            VillageSiegeCommandUi.openTurretCatalog(player); return true;
        }
        if (action.startsWith("siege_segment_open:")) {
            VillageSiegeCommandUi.openSegment(player, VillageSiegeSegmentSystem.Segment.fromId(action.substring(19)));
            return true;
        }
        if (action.startsWith("siege_segment_repair:")) {
            VillageSiegeSegmentSystem.Segment segment = VillageSiegeSegmentSystem.Segment.fromId(action.substring(21));
            player.sendSystemMessage(Component.literal("§b" + VillageSiegeSegmentSystem.repair(player, segment)));
            VillageSiegeCommandUi.openSegment(player, segment); return true;
        }
        if (action.startsWith("siege_segment_upgrade:")) {
            VillageSiegeSegmentSystem.Segment segment = VillageSiegeSegmentSystem.Segment.fromId(action.substring(22));
            player.sendSystemMessage(Component.literal("§b" + VillageSiegeSegmentSystem.upgrade(player, segment)));
            VillageSiegeCommandUi.openSegment(player, segment); return true;
        }
        if (action.startsWith("siege_turret_select:")) {
            VillagePlacedTurretSystem.TurretType type = VillagePlacedTurretSystem.TurretType.fromId(action.substring(20));
            player.sendSystemMessage(Component.literal("§b" + VillagePlacedTurretSystem.selectPlacement(player, type)));
            return true;
        }
        if (action.startsWith("siege_turret_open:")) {
            VillageSiegeCommandUi.openTurret(player, parseInt(action.substring(18), -1)); return true;
        }
        if (action.startsWith("siege_turret_repair:")) {
            int id = parseInt(action.substring(20), -1);
            player.sendSystemMessage(Component.literal("§b" + VillagePlacedTurretSystem.repair(player, id)));
            VillageSiegeCommandUi.openTurret(player, id); return true;
        }
        if (action.startsWith("siege_turret_upgrade:")) {
            int id = parseInt(action.substring(21), -1);
            player.sendSystemMessage(Component.literal("§b" + VillagePlacedTurretSystem.upgrade(player, id)));
            VillageSiegeCommandUi.openTurret(player, id); return true;
        }
        if (action.startsWith("siege_turret_dismantle:")) {
            int id = parseInt(action.substring(23), -1);
            player.sendSystemMessage(Component.literal("§b" + VillagePlacedTurretSystem.dismantle(player, id)));
            VillageSiegeCommandUi.openTurretList(player); return true;
        }

        if (action.equals("open_mercenary_command")) {
            VillageMercenaryDeploymentSystem.openCommand(player); return true;
        }
        if (action.startsWith("merc_class:")) {
            VillageMercenaryDeploymentSystem.openClass(player,
                    VillageMercenarySystem.MercenaryClass.fromId(action.substring(11))); return true;
        }
        if (action.startsWith("merc_hire:")) {
            VillageMercenarySystem.MercenaryClass kind = VillageMercenarySystem.MercenaryClass.fromId(action.substring(10));
            if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)) {
                player.sendSystemMessage(Component.literal("§c용병 고용은 병영 단말기 근처에서만 가능합니다."));
            } else {
                player.sendSystemMessage(Component.literal("§b" + VillageMercenarySystem.hire(player, kind)));
            }
            VillageMercenaryDeploymentSystem.openClass(player, kind); return true;
        }
        if (action.startsWith("merc_deploy:")) {
            if (!VillageMercenaryDeploymentSystem.canOpenAt(player)) {
                player.sendSystemMessage(Component.literal("§c용병 배치는 병영 또는 마을 회관 근처에서만 변경할 수 있습니다."));
                return true;
            }
            String[] parts = action.split(":", 3);
            VillageMercenarySystem.MercenaryClass kind = parts.length >= 2
                    ? VillageMercenarySystem.MercenaryClass.fromId(parts[1]) : null;
            VillageMercenaryDeploymentSystem.Deployment zone = parts.length >= 3
                    ? VillageMercenaryDeploymentSystem.Deployment.fromId(parts[2]) : null;
            player.sendSystemMessage(Component.literal("§b" + VillageMercenaryDeploymentSystem.setDeployment(player, kind, zone)));
            VillageMercenaryDeploymentSystem.openClass(player, kind); return true;
        }

        switch (action) {
            case "buy_arrows" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                    player.sendSystemMessage(Component.literal("§c화살 구매는 창고 단말기 근처에서만 가능합니다."));
                } else {
                    player.sendSystemMessage(Component.literal("§b" + VillageProgressionSystem.buyArrows(player)));
                    VillageUiController.openEquipmentShop(player);
                }
                return true;
            }
            case "buy_food" -> {
                if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.STOREHOUSE)) {
                    player.sendSystemMessage(Component.literal("§c식량 구매는 창고 단말기 근처에서만 가능합니다."));
                } else {
                    player.sendSystemMessage(Component.literal("§b" + VillageProgressionSystem.buyFood(player)));
                    VillageUiController.openEquipmentShop(player);
                }
                return true;
            }
            case "use_infirmary" -> {
                VillageUiController.openResult(player, "의무소",
                        "의무소는 자동 버프 건물입니다. 낮에는 마을 안에서 체력이 항상 완전히 회복됩니다.",
                        "open_dashboard");
                return true;
            }
            case "train" -> {
                VillageUiController.openResult(player, "병영 훈련",
                        "전투 훈련은 패시브로 변경되었습니다. 현재 모든 경험치 획득량 +"
                                + (VillageProgressionSystem.experienceMultiplierPercent() - 100) + "%",
                        "open_dashboard");
                return true;
            }
            case "hire_mercenary" -> {
                VillageMercenaryDeploymentSystem.openCommand(player);
                return true;
            }
            default -> { return false; }
        }
    }

    private static boolean requiresSiegeCommandAccess(String action) {
        return action.equals("siege_turret_repair_all")
                || action.startsWith("siege_segment_repair:")
                || action.startsWith("siege_segment_upgrade:")
                || action.startsWith("siege_turret_select:")
                || action.startsWith("siege_turret_repair:")
                || action.startsWith("siege_turret_upgrade:")
                || action.startsWith("siege_turret_dismantle:");
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
