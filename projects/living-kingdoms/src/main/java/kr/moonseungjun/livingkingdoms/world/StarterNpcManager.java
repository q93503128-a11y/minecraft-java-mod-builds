package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.profile.OriginProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/** Named citizens anchored to the terrain-surveyed homeland layout. */
public final class StarterNpcManager {
    private static final String DONE_PREFIX = "done:";
    private static final Identifier VILLAGER_ID = Identifier.fromNamespaceAndPath("minecraft", "villager");

    private StarterNpcManager() {
    }

    public static void ensureForPlayer(ServerPlayer player, OriginProfile profile) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null) return;
        RealmSitePlanner.ensureBuilt(realm, profile.homelandId());

        StarterNpcLifeSavedData life = realm.getDataStorage().computeIfAbsent(StarterNpcLifeSavedData.TYPE);
        for (NpcDefinition definition : definitions(realm, profile.homelandId())) {
            if (life.isDead(definition.id())) continue;
            Villager existing = findExisting(realm, definition);
            if (existing == null) spawn(realm, definition);
            else repairExisting(realm, existing, definition);
        }
    }

    public static void handleInteraction(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof Villager villager)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        NpcDefinition definition = definitionByVillager(level, villager);
        if (definition == null) return;

        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal("§6[" + definition.name() + "] §f" + definition.dialogue()));

        StarterNpcProgressSavedData progress = level.getDataStorage().computeIfAbsent(StarterNpcProgressSavedData.TYPE);
        if (progress.markMet(player.getUUID(), definition.id())) {
            player.sendSystemMessage(Component.literal("§7새로운 이웃을 알게 되었습니다."));
        }

        String completionId = DONE_PREFIX + definition.homelandId();
        if (!progress.hasMet(player.getUUID(), completionId)
                && metAll(level, progress, player, definition.homelandId())) {
            progress.markMet(player.getUUID(), completionId);
            give(player, new ItemStack(Items.EMERALD, 3));
            player.giveExperiencePoints(20);
            player.sendSystemMessage(Component.literal(
                    "§a[첫 부탁 완료] §f지역 주민들에게 인사를 마쳤습니다. §e에메랄드 3개§f와 경험치를 받았습니다."
            ));
        }
    }

    public static void markDeadIfStarter(ServerLevel level, Villager villager) {
        NpcDefinition definition = definitionByVillager(level, villager);
        if (definition == null) return;
        level.getDataStorage().computeIfAbsent(StarterNpcLifeSavedData.TYPE).markDead(definition.id());
        LivingKingdoms.LOGGER.info("Named citizen {} died and will remain dead", definition.id());
    }

    public static boolean isStarterNpc(Villager villager) {
        return villager.level() instanceof ServerLevel level && definitionByVillager(level, villager) != null;
    }

    private static boolean metAll(ServerLevel level, StarterNpcProgressSavedData progress,
                                  ServerPlayer player, String homelandId) {
        for (NpcDefinition definition : definitions(level, homelandId)) {
            if (!progress.hasMet(player.getUUID(), definition.id())) return false;
        }
        return true;
    }

    private static Villager findExisting(ServerLevel level, NpcDefinition definition) {
        AABB area = new AABB(
                definition.x() - 24.0, definition.y() - 18.0, definition.z() - 24.0,
                definition.x() + 24.0, definition.y() + 22.0, definition.z() + 24.0
        );
        List<Villager> matches = level.getEntitiesOfClass(
                Villager.class, area,
                villager -> definition.name().equals(villager.getName().getString())
        );
        return matches.isEmpty() ? null : matches.getFirst();
    }

    private static void spawn(ServerLevel level, NpcDefinition definition) {
        EntityType<?> villagerType = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (villagerType == null) {
            LivingKingdoms.LOGGER.error("Minecraft villager entity type is unavailable");
            return;
        }
        Entity created = villagerType.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) {
            LivingKingdoms.LOGGER.error("Failed to create named citizen {}", definition.id());
            return;
        }
        int standingY = safeStandingY(level, definition.x(), definition.y(), definition.z());
        villager.setPos(definition.x() + 0.5, standingY, definition.z() + 0.5);
        villager.setCustomName(Component.literal(definition.name()));
        villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        villager.setInvulnerable(false);
        if (!level.addFreshEntity(villager)) {
            LivingKingdoms.LOGGER.error("Failed to add named citizen {}", definition.id());
        }
    }

    private static void repairExisting(ServerLevel level, Villager villager, NpcDefinition definition) {
        int standingY = safeStandingY(level, definition.x(), definition.y(), definition.z());
        boolean unsafe = villager.getY() < standingY - 0.5
                || !level.getBlockState(villager.blockPosition()).isAir()
                || !level.getBlockState(villager.blockPosition().above()).isAir();
        if (unsafe || villager.distanceToSqr(definition.x() + 0.5, standingY, definition.z() + 0.5) > 400.0) {
            villager.setPos(definition.x() + 0.5, standingY, definition.z() + 0.5);
        }
        villager.setInvulnerable(false);
        villager.setPersistenceRequired();
    }

    private static int safeStandingY(ServerLevel level, int x, int preferredY, int z) {
        for (int offset = 0; offset <= 28; offset++) {
            int[] candidates = offset == 0 ? new int[]{preferredY} : new int[]{preferredY + offset, preferredY - offset};
            for (int standingY : candidates) {
                BlockPos feet = new BlockPos(x, standingY, z);
                if (!level.getBlockState(feet.below()).isAir()
                        && level.getBlockState(feet).isAir()
                        && level.getBlockState(feet.above()).isAir()) {
                    return standingY;
                }
            }
        }
        return RealmSitePlanner.surfaceY(level, x, z) + 1;
    }

    private static NpcDefinition definitionByVillager(ServerLevel level, Villager villager) {
        String name = villager.getName().getString();
        for (String homelandId : List.of("erden_kingdom", "silvana_forest", "kardum_league")) {
            for (NpcDefinition definition : definitions(level, homelandId)) {
                if (definition.name().equals(name)) return definition;
            }
        }
        return null;
    }

    private static List<NpcDefinition> definitions(ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.ensureBuilt(level, homelandId);
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = site.baseY();
        return switch (homelandId) {
            case "silvana_forest" -> List.of(
                    new NpcDefinition("silvana_warden", homelandId, "수관지기 리에나", cx + 8, y + 23, cz + 5,
                            "숲은 길을 숨기지만, 길을 잃은 이를 버리지는 않습니다."),
                    new NpcDefinition("silvana_herbalist", homelandId, "약초사 세릴", cx + 86, y + 1, cz + 80,
                            "달샘 주변의 은빛 잎은 해가 진 뒤에만 향을 냅니다."),
                    new NpcDefinition("silvana_neighbor", homelandId, "주민 아일로", cx + 45, y + 3, cz + 58,
                            "새 이웃이군요. 수관 다리를 건널 때 아래를 너무 오래 보지는 마세요.")
            );
            case "kardum_league" -> List.of(
                    new NpcDefinition("kardum_gatekeeper", homelandId, "산문지기 브로간", cx, y + 1, cz - 80,
                            "카르둠에서는 이름보다 네가 만든 것이 오래 남지."),
                    new NpcDefinition("kardum_smith", homelandId, "대장장이 도르마", cx, y + 5, cz + 48,
                            "좋은 쇠는 불을 두려워하지 않고, 좋은 장인은 실패를 숨기지 않아."),
                    new NpcDefinition("kardum_neighbor", homelandId, "광부 케른", cx - 70, y + 2, cz + 40,
                            "낮은 갱도에는 아직 들어가지 마. 먼저 산의 울림부터 익혀.")
            );
            default -> List.of(
                    new NpcDefinition("erden_guide", "erden_kingdom", "길잡이 마렌", cx + 4, y + 1, cz + 8,
                            "이곳은 로엔 변경백령의 중심도시입니다. 시장과 행정청, 성문 방향부터 익혀 두세요."),
                    new NpcDefinition("erden_fisher", "erden_kingdom", "어부 로안", cx - 165, y + 1, cz + 68,
                            "수로는 서쪽 강항구와 이어집니다. 아침 물살이 가장 잔잔하지요."),
                    new NpcDefinition("erden_neighbor", "erden_kingdom", "주민 엘라", cx + 30, y + 1, cz + 35,
                            "광장 종탑을 기준으로 북쪽은 내성, 남쪽은 주거구입니다.")
            );
        };
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private record NpcDefinition(String id, String homelandId, String name,
                                 int x, int y, int z, String dialogue) {
    }
}
