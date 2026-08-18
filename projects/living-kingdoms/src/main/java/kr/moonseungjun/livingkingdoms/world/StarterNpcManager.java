package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.economy.GuildWorkOrderManager;
import kr.moonseungjun.livingkingdoms.economy.RealmEconomyManager;
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
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

/** Named citizens anchored to a completed terrain-surveyed homeland layout. */
public final class StarterNpcManager {
    private static final String DONE_PREFIX = "done:";
    private static final Identifier VILLAGER_ID = Identifier.fromNamespaceAndPath("minecraft", "villager");
    private static final List<String> HOMELANDS = List.of(
            "erden_kingdom", "silvana_forest", "kardum_league");

    private StarterNpcManager() {
    }

    public static void ensureForPlayer(ServerPlayer player, OriginProfile profile) {
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null || !RealmSitePlanner.isBuilt(realm, profile.homelandId())) return;

        StarterNpcLifeSavedData life = realm.getDataStorage().computeIfAbsent(StarterNpcLifeSavedData.TYPE);
        if (life.requiresSpawnTrackingMigration()) {
            List<String> previouslyManaged = HOMELANDS.stream()
                    .flatMap(homelandId -> definitions(realm, homelandId).stream())
                    .filter(definition -> !life.isDead(definition.id()))
                    .map(NpcDefinition::id)
                    .toList();
            life.migratePreviouslyManaged(previouslyManaged);
            LivingKingdoms.LOGGER.info(
                    "Migrated named citizen spawn tracking homelands=all citizens={} revision={}",
                    previouslyManaged.size(), StarterNpcLifeSavedData.SPAWN_TRACKING_REVISION);
        }

        List<NpcDefinition> definitions = definitions(realm, profile.homelandId());
        for (NpcDefinition definition : definitions) {
            if (life.isDead(definition.id())) continue;
            Villager existing = findExisting(realm, definition);
            if (existing != null) {
                life.markSpawned(definition.id());
                repairExisting(realm, existing, definition);
                continue;
            }
            // A previously spawned entity can be in an unloaded chunk. Absence from the loaded-entity
            // index is not evidence that it died or should be duplicated.
            if (!life.wasSpawned(definition.id()) && spawn(realm, definition)) {
                life.markSpawned(definition.id());
            }
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
            player.sendSystemMessage(Component.literal("§7새로운 시민을 알게 되었습니다."));
        }

        if (GuildWorkOrderManager.isGuildRepresentative(definition.id())) {
            if (player.isShiftKeyDown()) GuildWorkOrderManager.interact(player, definition.id());
            else GuildWorkOrderManager.explain(player, definition.id());
        }

        String completionId = DONE_PREFIX + definition.homelandId();
        if (!progress.hasMet(player.getUUID(), completionId)
                && metAll(level, progress, player, definition.homelandId())) {
            progress.markMet(player.getUUID(), completionId);
            RealmEconomyManager.credit(player, 12L, 3);
            player.sendSystemMessage(Component.literal(
                    "§a[지역 인사 완료] §f주요 시민들과 인사를 마쳤습니다. §e은화 12§f와 명망 3을 받았습니다."
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
        AABB area = ErdenUrbanLifeManager.managesCitizenId(definition.id())
                ? ErdenUrbanLifeManager.managedCitizenBounds(level)
                : new AABB(
                        definition.x() - 24.0, definition.y() - 18.0, definition.z() - 24.0,
                        definition.x() + 24.0, definition.y() + 22.0, definition.z() + 24.0
                );
        List<Villager> matches = level.getEntitiesOfClass(
                Villager.class, area,
                villager -> definition.name().equals(villager.getName().getString())
        );
        return matches.isEmpty() ? null : matches.getFirst();
    }

    private static boolean spawn(ServerLevel level, NpcDefinition definition) {
        EntityType<?> villagerType = BuiltInRegistries.ENTITY_TYPE.getOptional(VILLAGER_ID).orElse(null);
        if (villagerType == null) {
            LivingKingdoms.LOGGER.error("Minecraft villager entity type is unavailable");
            return false;
        }
        Entity created = villagerType.create(level, EntitySpawnReason.COMMAND);
        if (!(created instanceof Villager villager)) {
            LivingKingdoms.LOGGER.error("Failed to create named citizen {}", definition.id());
            return false;
        }
        int standingY = safeStandingY(level, definition.x(), definition.y(), definition.z());
        villager.setPos(definition.x() + 0.5, standingY, definition.z() + 0.5);
        villager.setCustomName(Component.literal(definition.name()));
        villager.setCustomNameVisible(true);
        villager.setPersistenceRequired();
        villager.setInvulnerable(false);
        if (!level.addFreshEntity(villager)) {
            LivingKingdoms.LOGGER.error("Failed to add named citizen {}", definition.id());
            return false;
        }
        return true;
    }

    private static void repairExisting(ServerLevel level, Villager villager, NpcDefinition definition) {
        int standingY = safeStandingY(level, definition.x(), definition.y(), definition.z());
        boolean managedByUrbanLife = ErdenUrbanLifeManager.managesCitizenId(definition.id());
        boolean unsafe = villager.getY() < level.getMinY() + 2.0D
                || !level.getBlockState(villager.blockPosition()).isAir()
                || !level.getBlockState(villager.blockPosition().above()).isAir();
        if (!managedByUrbanLife && villager.getY() < standingY - 0.5D) {
            unsafe = true;
        }
        boolean escapedStaticPost = !managedByUrbanLife
                && villager.distanceToSqr(
                        definition.x() + 0.5, standingY, definition.z() + 0.5) > 400.0D;
        if (unsafe || escapedStaticPost) {
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
        for (String homelandId : HOMELANDS) {
            for (NpcDefinition definition : definitions(level, homelandId)) {
                if (definition.name().equals(name)) return definition;
            }
        }
        return null;
    }

    private static List<NpcDefinition> definitions(ServerLevel level, String homelandId) {
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(level, homelandId);
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) return List.of();
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = site.baseY();
        return switch (homelandId) {
            case "silvana_forest" -> List.of(
                    new NpcDefinition("silvana_warden", homelandId, "수관지기 리에나", cx + 8, y + 23, cz + 5,
                            "숲의 길은 허가와 기억으로 이어집니다. 순찰 증표를 모으면 수관 경비대가 정산합니다."),
                    new NpcDefinition("silvana_herbalist", homelandId, "약초사 세릴", cx + 86, y + 1, cz + 80,
                            "약초는 빛과 계절에 따라 효능이 달라집니다. 채취 기록 없는 재료는 받지 않습니다."),
                    new NpcDefinition("silvana_neighbor", homelandId, "정원사 아일로", cx + 45, y + 3, cz + 58,
                            "수관 아래의 흙길과 배수 홈도 누군가는 매일 돌봐야 합니다."),
                    new NpcDefinition("silvana_lorekeeper", homelandId, "기억지기 나엘", cx - 82, y + 1, cz + 72,
                            "우리의 법은 나무껍질 문서와 증언으로 남습니다. 필사 일은 언제나 부족하지요."),
                    new NpcDefinition("silvana_bowyer", homelandId, "활장이 미레스", cx - 58, y + 16, cz - 30,
                            "활은 목재와 힘줄, 습도를 함께 읽어야 합니다. 단순한 조합법으로 만들 수 없어요."),
                    new NpcDefinition("silvana_pathfinder", homelandId, "길잡이 테리온", cx, y + 1, cz - 92,
                            "수림 물자는 수레보다 짐승과 수관 도르래로 움직입니다."),
                    new NpcDefinition("silvana_waterkeeper", homelandId, "샘지기 오레아", cx + 72, y + 1, cz + 66,
                            "달샘의 물고기는 공동체 몫입니다. 필요한 만큼만 잡고 기록을 남기세요."),
                    new NpcDefinition("silvana_scholar", homelandId, "별력 연구자 리실", cx - 35, y + 15, cz - 22,
                            "계절과 마력 흐름은 같은 달력에 묶여 있습니다. 시장 가격도 그 영향을 받지요.")
            );
            case "kardum_league" -> List.of(
                    new NpcDefinition("kardum_gatekeeper", homelandId, "산문지기 브로간", cx, y + 1, cz - 80,
                            "카르둠에서는 이름보다 네가 만든 것과 지킨 통로가 오래 남지."),
                    new NpcDefinition("kardum_smith", homelandId, "대장장이 도르마", cx - 16, y + 4, cz + 42,
                            "좋은 쇠는 불을 두려워하지 않고, 좋은 장인은 재료 장부를 숨기지 않아."),
                    new NpcDefinition("kardum_miner", homelandId, "광부 케른", cx - 70, y + 2, cz + 40,
                            "광석보다 먼저 지지대와 환기구를 보게. 산은 무모한 사람을 오래 기억한다."),
                    new NpcDefinition("kardum_archivist", homelandId, "석판기록관 베르다", cx - 10, y + 2, cz - 72,
                            "계약과 광맥 지도를 필사할 손이 필요합니다. 글도 광맥처럼 끊기면 안 됩니다."),
                    new NpcDefinition("kardum_provisioner", homelandId, "보급관 로군", cx + 64, y + 4, cz + 38,
                            "지하도시는 한 끼와 한 줄의 밧줄도 계산합니다. 운송 손실이 곧 생존 비용이니까요."),
                    new NpcDefinition("kardum_brewer", homelandId, "균류양조사 헤사", cx + 58, y + 11, cz - 65,
                            "이곳의 약재는 햇빛보다 온도와 균사 상태가 중요합니다."),
                    new NpcDefinition("kardum_quartermaster", homelandId, "병기보급관 울딘", cx - 38, y + 7, cz - 30,
                            "무기는 개인 장난감이 아닙니다. 수량과 수리 이력, 지급 대상이 모두 기록됩니다."),
                    new NpcDefinition("kardum_surveyor", homelandId, "갱도측량사 마르", cx - 96, y + 2, cz - 52,
                            "한 칸의 오차가 갱도 전체를 무너뜨립니다. 공공 보수 자재가 늘 모자랍니다.")
            );
            default -> List.of(
                    new NpcDefinition("erden_guide", "erden_kingdom", "기록관 마렌", cx + 4, y + 1, cz + 8,
                            "이곳은 로엔 변경백령의 중심도시입니다. 시장과 행정청, 성문부터 익혀 두세요."),
                    new NpcDefinition("erden_fisher", "erden_kingdom", "어업조합원 로안", cx - 165, y + 1, cz + 68,
                            "수로는 서쪽 강항구와 이어집니다. 어획량과 수위는 매일 장부에 적습니다."),
                    new NpcDefinition("erden_neighbor", "erden_kingdom", "석공 엘라", cx + 30, y + 1, cz + 35,
                            "광장 종탑을 기준으로 북쪽은 내성, 남쪽은 주거구입니다. 배수로를 밟지 마세요."),
                    new NpcDefinition("erden_clerk", "erden_kingdom", "시장서기 페른", cx - 70, y + 1, cz + 18,
                            "주간 시장과 계절 장시는 허가가 다릅니다. 거래 기록이 없으면 분쟁 때 보호받지 못합니다."),
                    new NpcDefinition("erden_smith", "erden_kingdom", "철공조합장 하벨", cx + 78, y + 1, cz - 35,
                            "원료와 노임을 내면 조합이 책임지고 제작합니다. 개인 조합대는 품질 보증이 없지요."),
                    new NpcDefinition("erden_apothecary", "erden_kingdom", "약제사 미라", cx - 72, y + 1, cz - 76,
                            "약은 효과만큼 부작용과 보관기한도 중요합니다. 출처 없는 약초는 받지 않습니다."),
                    new NpcDefinition("erden_sergeant", "erden_kingdom", "성문부사관 토렌", cx, y + 1, cz - 94,
                            "성문은 방벽이면서 세관입니다. 야간 통행과 무기 반입은 기록됩니다."),
                    new NpcDefinition("erden_carter", "erden_kingdom", "마차조합원 베아", cx - 96, y + 1, cz + 70,
                            "곡물 한 자루의 값에는 길 상태, 마구 손실, 통행세가 전부 들어갑니다.")
            );
        };
    }

    private record NpcDefinition(String id, String homelandId, String name,
                                 int x, int y, int z, String dialogue) {
    }
}
