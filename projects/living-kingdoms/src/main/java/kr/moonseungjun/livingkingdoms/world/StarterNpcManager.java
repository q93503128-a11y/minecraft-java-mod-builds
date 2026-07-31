package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

public final class StarterNpcManager {
    private static final String NPC_PREFIX = "lk_npc_";
    private static final String MET_PREFIX = "lk_met_";
    private static final String DONE_PREFIX = "lk_intro_done_";

    private StarterNpcManager() {
    }

    public static void ensureForPlayer(ServerPlayer player, OriginProfile profile) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) {
            return;
        }

        for (NpcDefinition definition : definitions(profile.homelandId())) {
            if (!exists(realm, definition)) {
                spawn(realm, definition);
            }
        }
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)) {
            return;
        }

        String npcId = villager.getTags().stream()
                .filter(tag -> tag.startsWith(NPC_PREFIX))
                .findFirst()
                .orElse(null);
        if (npcId == null) {
            return;
        }

        NpcDefinition definition = definitionByTag(npcId);
        if (definition == null) {
            return;
        }

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal("§6[" + definition.name() + "] §f" + definition.dialogue()));

        String metTag = MET_PREFIX + definition.id();
        if (player.addTag(metTag)) {
            player.sendSystemMessage(Component.literal("§7새로운 이웃을 알게 되었습니다."));
        }

        String homelandId = definition.homelandId();
        String doneTag = DONE_PREFIX + homelandId;
        if (!player.getTags().contains(doneTag) && metAll(player, homelandId)) {
            player.addTag(doneTag);
            give(player, new ItemStack(Items.EMERALD, 3));
            player.giveExperiencePoints(20);
            player.sendSystemMessage(Component.literal(
                    "§a[첫 부탁 완료] §f지역 주민들에게 인사를 마쳤습니다. §e에메랄드 3개§f와 경험치를 받았습니다."
            ));
        }
    }

    private static boolean metAll(ServerPlayer player, String homelandId) {
        for (NpcDefinition definition : definitions(homelandId)) {
            if (!player.getTags().contains(MET_PREFIX + definition.id())) {
                return false;
            }
        }
        return true;
    }

    private static boolean exists(ServerLevel level, NpcDefinition definition) {
        AABB area = new AABB(
                definition.x() - 10.0, definition.y() - 5.0, definition.z() - 10.0,
                definition.x() + 10.0, definition.y() + 8.0, definition.z() + 10.0
        );
        return !level.getEntitiesOfClass(
                Villager.class,
                area,
                villager -> villager.getTags().contains(NPC_PREFIX + definition.id())
        ).isEmpty();
    }

    private static void spawn(ServerLevel level, NpcDefinition definition) {
        Villager villager = EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
        if (villager == null) {
            LivingKingdoms.LOGGER.error("Failed to create starter NPC {}", definition.id());
            return;
        }
        villager.setPos(definition.x() + 0.5, definition.y(), definition.z() + 0.5);
        villager.setCustomName(Component.literal(definition.name()));
        villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        villager.setInvulnerable(true);
        villager.addTag(NPC_PREFIX + definition.id());
        if (!level.addFreshEntity(villager)) {
            LivingKingdoms.LOGGER.error("Failed to add starter NPC {} to the realm", definition.id());
        }
    }

    private static NpcDefinition definitionByTag(String tag) {
        for (String homelandId : List.of("erden_kingdom", "silvana_forest", "kardum_league")) {
            for (NpcDefinition definition : definitions(homelandId)) {
                if ((NPC_PREFIX + definition.id()).equals(tag)) {
                    return definition;
                }
            }
        }
        return null;
    }

    private static List<NpcDefinition> definitions(String homelandId) {
        return switch (homelandId) {
            case "silvana_forest" -> List.of(
                    new NpcDefinition("silvana_warden", homelandId, "수관지기 리에나", 1216, 82, 11,
                            "숲은 길을 숨기지만, 길을 잃은 이를 버리지는 않습니다."),
                    new NpcDefinition("silvana_herbalist", homelandId, "약초사 세릴", 1280, 67, 78,
                            "달샘 주변의 은빛 잎은 해가 진 뒤에만 향을 냅니다."),
                    new NpcDefinition("silvana_neighbor", homelandId, "주민 아일로", 1291, 67, 74,
                            "새 이웃이군요. 수관 다리를 건널 때 아래를 너무 오래 보지는 마세요.")
            );
            case "kardum_league" -> List.of(
                    new NpcDefinition("kardum_gatekeeper", homelandId, "산문지기 브로간", -1194, 68, 8,
                            "카르둠에서는 이름보다 네가 만든 것이 오래 남지."),
                    new NpcDefinition("kardum_smith", homelandId, "대장장이 도르마", -1164, 68, 44,
                            "좋은 쇠는 불을 두려워하지 않고, 좋은 장인은 실패를 숨기지 않아."),
                    new NpcDefinition("kardum_neighbor", homelandId, "광부 케른", -1128, 68, 92,
                            "낮은 갱도에는 아직 들어가지 마. 먼저 산의 울림부터 익혀.")
            );
            default -> List.of(
                    new NpcDefinition("erden_guide", "erden_kingdom", "길잡이 마렌", 3, 66, 6,
                            "여기는 에르덴 변경입니다. 시장길과 강변길부터 익혀 두세요."),
                    new NpcDefinition("erden_fisher", "erden_kingdom", "어부 로안", -104, 66, 88,
                            "은빛강은 아침 물살이 잔잔합니다. 낚싯줄을 너무 멀리 던지진 마세요."),
                    new NpcDefinition("erden_neighbor", "erden_kingdom", "주민 엘라", 18, 66, 18,
                            "새로 왔다면 광장 등불을 기준으로 길을 찾으면 됩니다.")
            );
        };
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private record NpcDefinition(
            String id,
            String homelandId,
            String name,
            int x,
            int y,
            int z,
            String dialogue
    ) {
    }
}
