package kr.moonseungjun.survivalascension.production;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.Comparator;
import java.util.UUID;

/**
 * Registered logistics barrels can be moved without dumping 27 slots on the ground.
 * Only player-owned SA logistics barrels are affected; ordinary vanilla barrels keep vanilla behavior.
 * No chunk is force-loaded and physical outposts cannot be packed into a portable item.
 */
public final class PortableLogisticsBarrelService {
    private static final String OWNER_KEY = "survivalascension:packed_logistics_owner";
    private static final String ROLE_KEY = "survivalascension:packed_logistics_role";
    private static final String TOKEN_KEY = "survivalascension:packed_logistics_token";
    private static final String ROLE_ANCHOR = "anchor";
    private static final String ROLE_LINKED = "linked";

    private PortableLogisticsBarrelService() {}

    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) return;
        if (player.isCreative() || player.isSpectator() || !event.getState().is(Blocks.BARREL)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        BlockPos pos = event.getPos();
        String dimension = level.dimension().toString();
        FieldDepotData data = FieldDepotData.get(player);
        boolean anchor = data.owns(player, dimension, pos);
        boolean linked = data.isLinkedByOwner(player, dimension, pos);
        if (!anchor && !linked) return;
        if (!level.mayInteract(player, pos)) return;

        FieldDepotData.DepotEntry depot = null;
        if (anchor) {
            depot = data.depots(player).stream()
                    .filter(entry -> entry.dimension().equals(dimension) && entry.pos().equals(pos))
                    .findFirst().orElse(null);
            if (depot == null) return;
            if (OutpostService.isOutpost(player, depot)) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§3[물류 통 포장] §f전초기지로 승격된 거점 통은 포장할 수 없습니다. §7전초기지는 실제 위치에 남는 시설입니다."));
                return;
            }
            int linkedCount = data.linkedCount(player, depot);
            if (linkedCount > 0) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal("§3[물류 통 포장] §f이 거점에는 연결된 창고 통이 §e" + linkedCount
                        + "개§f 있습니다. §7창고 통부터 포장하거나 연결 해제한 뒤 거점 통을 옮기세요."));
                return;
            }
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) return;
        if (containsPackedLogisticsBarrel(container)) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§3[물류 통 포장] §f포장된 물류 통 안에 또 다른 포장된 물류 통을 넣을 수 없습니다. §7무한 중첩 방지를 위해 먼저 꺼내세요."));
            return;
        }

        String token = UUID.randomUUID().toString();
        CompoundTag persistent = blockEntity.getPersistentData();
        persistent.putString(OWNER_KEY, player.getUUID().toString());
        persistent.putString(ROLE_KEY, anchor ? ROLE_ANCHOR : ROLE_LINKED);
        persistent.putString(TOKEN_KEY, token);
        blockEntity.setChanged();

        ItemStack packed = new ItemStack(Items.BARREL);
        CompoundTag blockEntityData = blockEntity.saveWithoutMetadata(level.registryAccess());
        packed.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(blockEntity.getType(), blockEntityData));
        CompoundTag marker = new CompoundTag();
        marker.putString(TOKEN_KEY, token);
        marker.putString(OWNER_KEY, player.getUUID().toString());
        marker.putString(ROLE_KEY, anchor ? ROLE_ANCHOR : ROLE_LINKED);
        CustomData.set(DataComponents.CUSTOM_DATA, packed, marker);
        packed.set(DataComponents.MAX_STACK_SIZE, 1);
        packed.set(DataComponents.ITEM_NAME, Component.literal(anchor ? "포장된 물류 거점 통" : "포장된 물류 창고 통"));

        event.setCanceled(true);
        if (anchor) {
            data.remove(player, depot);
        } else {
            data.removeLink(player, dimension, pos);
        }
        container.clearContent();
        blockEntity.setChanged();
        level.removeBlock(pos, false);

        if (!player.addItem(packed)) player.drop(packed, false);
        player.sendSystemMessage(Component.literal("§b[물류 통 포장] §f내용물 27칸을 그대로 보존해 §e"
                + (anchor ? "물류 거점 통" : "물류 창고 통") + "§f을 포장했습니다. §7새 위치에 설치하면 연결을 자동 복구합니다."));
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!event.getPlacedBlock().is(Blocks.BARREL)) return;

        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return;
        CompoundTag persistent = blockEntity.getPersistentData();
        if (!persistent.contains(TOKEN_KEY) || !persistent.contains(OWNER_KEY) || !persistent.contains(ROLE_KEY)) return;

        String owner = persistent.getStringOr(OWNER_KEY, "");
        String role = persistent.getStringOr(ROLE_KEY, "");
        persistent.remove(TOKEN_KEY);
        persistent.remove(OWNER_KEY);
        persistent.remove(ROLE_KEY);
        blockEntity.setChanged();

        if (!owner.equals(player.getUUID().toString())) {
            player.sendSystemMessage(Component.literal("§3[물류 통 설치] §f내용물은 복원했지만 다른 플레이어가 포장한 통이므로 물류 연결은 자동 복구하지 않았습니다."));
            return;
        }

        String dimension = level.dimension().toString();
        FieldDepotData data = FieldDepotData.get(player);
        if (ROLE_ANCHOR.equals(role)) {
            FieldDepotData.AddResult result = data.add(player, dimension, pos, FieldDepotData.registrationLimit(player));
            if (result == FieldDepotData.AddResult.ADDED || result == FieldDepotData.AddResult.ALREADY_OWNED) {
                player.sendSystemMessage(Component.literal("§b[물류 거점 이전] §f내용물과 거점 등록을 새 위치로 복구했습니다. §7현장 보급권은 추가로 소비하지 않았습니다."));
            } else {
                player.sendSystemMessage(Component.literal("§6[물류 거점 이전] §f내용물은 복원했지만 거점 등록은 자동 복구하지 못했습니다. §7거점 한도나 다른 연결을 확인한 뒤 물류 거점 연결을 선택하세요."));
            }
            return;
        }

        if (ROLE_LINKED.equals(role)) {
            FieldDepotData.DepotEntry nearest = data.depots(player).stream()
                    .filter(depot -> depot.dimension().equals(dimension))
                    .filter(depot -> depot.pos().distSqr(pos) <= FieldDepotData.MAX_LINK_RADIUS * FieldDepotData.MAX_LINK_RADIUS)
                    .filter(depot -> level.hasChunkAt(depot.pos()))
                    .filter(depot -> level.getBlockState(depot.pos()).is(Blocks.BARREL))
                    .filter(depot -> level.mayInteract(player, depot.pos()))
                    .min(Comparator.comparingDouble(depot -> depot.pos().distSqr(pos)))
                    .orElse(null);
            if (nearest == null) {
                player.sendSystemMessage(Component.literal("§6[물류 창고 이전] §f내용물은 복원했습니다. §7자동 연결하려면 자신의 등록 거점 통에서 6블록 안에 설치하세요. 지금 위치에서는 '창고 통 연결'로 수동 연결할 수 있습니다."));
                return;
            }
            FieldDepotData.LinkResult result = data.addLink(player, nearest, pos);
            if (result == FieldDepotData.LinkResult.ADDED || result == FieldDepotData.LinkResult.ALREADY_LINKED) {
                player.sendSystemMessage(Component.literal("§b[물류 창고 이전] §f내용물과 창고 연결을 새 위치로 복구했습니다. §7추가 보급권은 필요하지 않습니다."));
            } else {
                player.sendSystemMessage(Component.literal("§6[물류 창고 이전] §f내용물은 복원했지만 창고 연결은 자동 복구하지 못했습니다. §7거점당 창고 한도와 기존 연결을 확인하세요."));
            }
        }
    }

    private static boolean containsPackedLogisticsBarrel(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !stack.is(Items.BARREL)) continue;
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.contains(TOKEN_KEY)) return true;
        }
        return false;
    }
}
