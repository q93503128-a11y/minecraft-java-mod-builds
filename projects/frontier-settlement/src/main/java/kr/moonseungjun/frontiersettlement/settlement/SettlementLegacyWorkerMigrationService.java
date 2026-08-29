package kr.moonseungjun.frontiersettlement.settlement;

import kr.moonseungjun.frontiersettlement.content.FrontierContent;
import kr.moonseungjun.frontiersettlement.content.FrontierWorkerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** One-way migration of loaded pre-Alpha.84 Frontier-managed vanilla villagers. */
public final class SettlementLegacyWorkerMigrationService {
    private static final Set<String> LEGACY_NAMES = Set.of(
            "건설 주민", "벌목 주민", "농사 주민", "채석 주민", "광산 주민",
            "작업장 주민", "고급 제작 주민", "건설 보급 주민", "운송 주민");

    private SettlementLegacyWorkerMigrationService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        if (server.getTickCount() % 10 != 0) return;
        ServerLevel level = server.overworld();
        List<Villager> legacy = new ArrayList<>(level.getEntitiesOfClass(
                Villager.class, searchBounds(data), SettlementLegacyWorkerMigrationService::isManagedLegacy));
        for (Villager old : legacy) migrateOne(level, old);
    }

    private static boolean isManagedLegacy(Villager villager) {
        for (String tag : villager.entityTags()) {
            if (tag.startsWith("frontier_settlement_")) return true;
        }
        Component name = villager.getCustomName();
        if (name == null) return false;
        String value = name.getString();
        if (LEGACY_NAMES.contains(value)) return true;
        return value.startsWith("운송 주민 #")
                || value.startsWith("전초 벌목 주민 #")
                || value.startsWith("전초 채석 주민 #")
                || value.startsWith("전초 광산 주민 #")
                || value.startsWith("전초 농업 주민 #")
                || value.startsWith("전초 어업 주민 #");
    }

    private static void migrateOne(ServerLevel level, Villager old) {
        FrontierWorkerEntity worker = new FrontierWorkerEntity(FrontierContent.FRONTIER_WORKER.get(), level);
        worker.setPos(old.getX(), old.getY(), old.getZ());
        worker.setYRot(old.getYRot());
        worker.setXRot(old.getXRot());
        worker.setCustomName(old.getCustomName());
        worker.setCustomNameVisible(old.isCustomNameVisible());
        worker.setPersistenceRequired();
        worker.setHealth(Math.min(old.getHealth(), worker.getMaxHealth()));
        worker.setDeltaMovement(old.getDeltaMovement());
        for (String tag : old.entityTags()) worker.addTag(tag);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            worker.setItemSlot(slot, old.getItemBySlot(slot).copy());
        }
        if (!level.addFreshEntity(worker)) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) old.setItemSlot(slot, ItemStack.EMPTY);
        old.discard();
    }

    private static AABB searchBounds(SettlementData data) {
        BlockPos center = data.centerPos();
        double minX = center.getX(), minY = center.getY(), minZ = center.getZ();
        double maxX = center.getX(), maxY = center.getY(), maxZ = center.getZ();
        for (BuildingRecord building : data.buildings()) {
            minX = Math.min(minX, building.originX());
            minY = Math.min(minY, building.originY());
            minZ = Math.min(minZ, building.originZ());
            maxX = Math.max(maxX, building.originX() + building.rotatedWidth());
            maxY = Math.max(maxY, building.originY() + building.buildingType().clearHeight() + 1);
            maxZ = Math.max(maxZ, building.originZ() + building.rotatedDepth());
        }
        for (RoadSegment road : data.roads()) for (BlockPos pos : road.centers()) {
            minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY()); minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX()); maxY = Math.max(maxY, pos.getY()); maxZ = Math.max(maxZ, pos.getZ());
        }
        for (OutpostRecord outpost : data.outposts()) {
            minX = Math.min(minX, outpost.centerX()); minY = Math.min(minY, outpost.centerY()); minZ = Math.min(minZ, outpost.centerZ());
            maxX = Math.max(maxX, outpost.centerX()); maxY = Math.max(maxY, outpost.centerY()); maxZ = Math.max(maxZ, outpost.centerZ());
        }
        return new AABB(minX - 96.0D, minY - 96.0D, minZ - 96.0D,
                maxX + 97.0D, maxY + 97.0D, maxZ + 97.0D);
    }
}
