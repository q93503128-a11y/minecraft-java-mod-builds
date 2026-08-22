package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.economy.GuildWorkOrderManager;
import kr.moonseungjun.livingkingdoms.economy.RealmEconomyManager;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
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

/** Named Erden citizens anchored to the completed active capital layout. */
public final class StarterNpcManager {
    private static final String DONE_PREFIX = "done:";
    private static final Identifier VILLAGER_ID = Identifier.fromNamespaceAndPath("minecraft", "villager");

    private StarterNpcManager() {
    }

    public static void ensureForPlayer(ServerPlayer player, OriginProfile profile) {
        if (!PlayableOriginCatalog.DEFAULT_HOMELAND.equals(profile.homelandId())) {
            throw new IllegalStateException("Inactive homeland reached starter NPC manager: " + profile.homelandId());
        }
        ServerLevel realm = player.level().getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (realm == null || !RealmSitePlanner.isBuilt(realm, PlayableOriginCatalog.DEFAULT_HOMELAND)) return;

        StarterNpcLifeSavedData life = realm.getDataStorage().computeIfAbsent(StarterNpcLifeSavedData.TYPE);
        if (life.requiresSpawnTrackingMigration()) {
            List<String> previouslyManaged = definitions(realm).stream()
                    .filter(definition -> !life.isDead(definition.id()))
                    .map(NpcDefinition::id)
                    .toList();
            life.migratePreviouslyManaged(previouslyManaged);
            LivingKingdoms.LOGGER.info(
                    "Migrated named Erden citizen spawn tracking citizens={} revision={}",
                    previouslyManaged.size(), StarterNpcLifeSavedData.SPAWN_TRACKING_REVISION);
        }

        for (NpcDefinition definition : definitions(realm)) {
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

        // Managed citizens never open vanilla villager barter menus.
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

        String completionId = DONE_PREFIX + PlayableOriginCatalog.DEFAULT_HOMELAND;
        if (!progress.hasMet(player.getUUID(), completionId) && metAll(level, progress, player)) {
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
                                  ServerPlayer player) {
        for (NpcDefinition definition : definitions(level)) {
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
        for (NpcDefinition definition : definitions(level)) {
            if (definition.name().equals(name)) return definition;
        }
        return null;
    }

    private static List<NpcDefinition> definitions(ServerLevel level) {
        RealmSiteLayoutSavedData.RealmSite site = RealmSitePlanner.site(
                level, PlayableOriginCatalog.DEFAULT_HOMELAND);
        if (site == null || !site.built() || site.revision() < RealmSitePlanner.LAYOUT_REVISION) return List.of();
        int cx = site.centerX();
        int cz = site.centerZ();
        int y = site.baseY();
        return List.of(
                new NpcDefinition("erden_guide", "기록관 마렌", cx + 4, y + 1, cz + 8,
                        "이곳은 로엔 변경백령의 중심도시입니다. 시장과 행정청, 성문부터 익혀 두세요."),
                new NpcDefinition("erden_fisher", "어업조합원 로안", cx - 165, y + 1, cz + 68,
                        "수로는 서쪽 강항구와 이어집니다. 어획량과 수위는 매일 장부에 적습니다."),
                new NpcDefinition("erden_neighbor", "석공 엘라", cx + 30, y + 1, cz + 35,
                        "광장 종탑을 기준으로 북쪽은 내성, 남쪽은 주거구입니다. 배수로를 밟지 마세요."),
                new NpcDefinition("erden_clerk", "시장서기 페른", cx - 70, y + 1, cz + 18,
                        "주간 시장과 계절 장시는 허가가 다릅니다. 거래 기록이 없으면 분쟁 때 보호받지 못합니다."),
                new NpcDefinition("erden_smith", "철공조합장 하벨", cx + 78, y + 1, cz - 35,
                        "원료와 노임을 내면 조합이 책임지고 제작합니다. 개인 조합대는 품질 보증이 없지요."),
                new NpcDefinition("erden_apothecary", "약제사 미라", cx - 72, y + 1, cz - 76,
                        "약은 효과만큼 부작용과 보관기한도 중요합니다. 출처 없는 약초는 받지 않습니다."),
                new NpcDefinition("erden_sergeant", "성문부사관 토렌", cx, y + 1, cz - 94,
                        "성문은 방벽이면서 세관입니다. 야간 통행과 무기 반입은 기록됩니다."),
                new NpcDefinition("erden_carter", "마차조합원 베아", cx - 96, y + 1, cz + 70,
                        "곡물 한 자루의 값에는 길 상태, 마구 손실, 통행세가 전부 들어갑니다.")
        );
    }

    private record NpcDefinition(String id, String name, int x, int y, int z, String dialogue) {
    }
}
