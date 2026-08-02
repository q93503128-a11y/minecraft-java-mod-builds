package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Temporary developer-facing outdoor arena for observing real Z/X skill motion. */
public final class VillageSkillTestSystem {
    private static final int ARENA_RADIUS = 16;
    private static final Set<UUID> ENABLED = new HashSet<>();
    private static final Set<UUID> DUMMIES = new HashSet<>();
    private static final Map<UUID, UUID> OWNERS = new HashMap<>();
    private static final Map<UUID, ReturnPoint> RETURN_POINTS = new HashMap<>();
    private static final Map<String, String> TEST_LOADOUTS = new HashMap<>();
    private static final Map<UUID, VillageRole> TEST_ROLES = new HashMap<>();

    private VillageSkillTestSystem() {}

    public static void initializeServer(MinecraftServer server) {
        ENABLED.clear();
        DUMMIES.clear();
        OWNERS.clear();
        RETURN_POINTS.clear();
        TEST_LOADOUTS.clear();
        TEST_ROLES.clear();
    }

    public static boolean recognize(Mob mob) {
        if (mob == null || !OWNERS.containsKey(mob.getUUID())) return false;
        mob.setNoAi(true);
        mob.setCanPickUpLoot(false);
        VillageWorldSystem.markAllowedGameMob(mob);
        DUMMIES.add(mob.getUUID());
        return true;
    }

    public static boolean isEnabled(ServerPlayer player) {
        return player != null && ENABLED.contains(player.getUUID());
    }

    public static String enable(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return "현재 월드에서는 시험장을 열 수 없습니다.";
        if (VillageCouncilState.currentPhase() != VillageTimePhase.DAY || VillageRaidSystem.isRaidLocked()) {
            return "기술 시험장은 낮 정비 시간에만 이용할 수 있습니다.";
        }
        BlockPos arena = arenaCenter();
        if (arena == null) return "마을 중심이 없어 외부 시험장을 배치할 수 없습니다.";

        if (!isEnabled(player)) {
            RETURN_POINTS.put(player.getUUID(), new ReturnPoint(
                    player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        }
        buildArena(level, arena);
        ENABLED.add(player.getUUID());
        selectedRole(player);
        ensureDefaultLoadout(player);

        BlockPos start = arena.offset(0, 0, 10);
        player.teleportTo(level, start.getX() + 0.5, start.getY(), start.getZ() + 0.5,
                Set.of(), 180.0f, 0.0f, true);
        player.setDeltaMovement(Vec3.ZERO);
        String targets = spawnTargets(player);
        return "외부 기술 시험장으로 이동했습니다."
                + "\n금색 바닥 관리함은 직업, 청금석 바닥 관리함은 Z/X 기술을 담당합니다."
                + "\n기술을 장착하면 창이 닫히며, 그 뒤 Z/X로 실제 시전합니다. K는 기술 관리함을 엽니다. "
                + targets;
    }

    public static String disable(ServerPlayer player) {
        String result = clearTargets(player);
        ENABLED.remove(player.getUUID());
        clearLoadout(player.getUUID());
        TEST_ROLES.remove(player.getUUID());
        ReturnPoint point = RETURN_POINTS.remove(player.getUUID());
        if (point != null && player.level() instanceof ServerLevel level) {
            player.teleportTo(level, point.x(), point.y(), point.z(), Set.of(),
                    point.yRot(), point.xRot(), true);
            player.setDeltaMovement(Vec3.ZERO);
        }
        return "기술 시험 모드를 종료하고 원래 위치로 복귀했습니다. " + result;
    }

    public static VillageRole selectedRole(ServerPlayer player) {
        if (player == null) return VillageRole.VANGUARD;
        VillageRole selected = TEST_ROLES.get(player.getUUID());
        if (selected != null) return selected;
        selected = VillageCouncilState.roleOf(player.getUUID()).orElse(VillageRole.VANGUARD);
        TEST_ROLES.put(player.getUUID(), selected);
        return selected;
    }

    public static String selectRole(ServerPlayer player, String roleId) {
        if (!isEnabled(player)) return "먼저 외부 기술 시험장을 활성화해야 합니다.";
        VillageRole role = VillageRole.parse(roleId).orElse(null);
        if (role == null) return "알 수 없는 시험 직업입니다.";
        TEST_ROLES.put(player.getUUID(), role);
        clearLoadout(player.getUUID());
        ensureDefaultLoadout(player);
        return role.displayName() + "을(를) 시험 직업으로 선택했습니다. 실제 직업과 성장 데이터는 바뀌지 않습니다.";
    }

    public static String equip(ServerPlayer player, String skillId, int slot) {
        if (!isEnabled(player)) return "먼저 외부 기술 시험장을 활성화해야 합니다.";
        VillageRoleSkillSystem.ActiveSkill skill =
                VillageRoleSkillSystem.ActiveSkill.parse(skillId).orElse(null);
        VillageRole role = selectedRole(player);
        if (skill == null || skill.role() != role) {
            return "현재 직업의 기술만 시험 슬롯에 장착할 수 있습니다.";
        }
        int safeSlot = slot == 1 ? 1 : 0;
        int otherSlot = safeSlot == 0 ? 1 : 0;
        if (skill.id().equals(TEST_LOADOUTS.get(loadoutKey(player.getUUID(), otherSlot)))) {
            TEST_LOADOUTS.remove(loadoutKey(player.getUUID(), otherSlot));
        }
        TEST_LOADOUTS.put(loadoutKey(player.getUUID(), safeSlot), skill.id());
        return skill.displayName() + "을(를) 시험 슬롯 " + (safeSlot == 0 ? "Z" : "X")
                + "에 임시 장착했습니다.";
    }

    public static Optional<VillageRoleSkillSystem.ActiveSkill> equippedSkill(ServerPlayer player, int slot) {
        if (!isEnabled(player)) return Optional.empty();
        VillageRole role = selectedRole(player);
        return VillageRoleSkillSystem.ActiveSkill.parse(
                        TEST_LOADOUTS.get(loadoutKey(player.getUUID(), slot == 1 ? 1 : 0)))
                .filter(skill -> skill.role() == role);
    }

    public static String loadoutSummary(ServerPlayer player) {
        String first = equippedSkill(player, 0)
                .map(VillageRoleSkillSystem.ActiveSkill::displayName).orElse("비어 있음");
        String second = equippedSkill(player, 1)
                .map(VillageRoleSkillSystem.ActiveSkill::displayName).orElse("비어 있음");
        return "Z: " + first + " | X: " + second;
    }

    public static String spawnTargets(ServerPlayer player) {
        if (!isEnabled(player)) return "먼저 기술 시험 모드를 활성화해야 합니다.";
        if (!(player.level() instanceof ServerLevel level)) return "현재 월드에서는 표적을 만들 수 없습니다.";
        BlockPos arena = arenaCenter();
        if (arena == null) return "시험장 위치를 찾을 수 없습니다.";
        clearTargets(player);

        int spawned = 0;
        for (int i = 0; i < 6; i++) {
            var dummy = EntityTypes.HUSK.create(level, EntitySpawnReason.EVENT);
            if (dummy == null) continue;
            int row = i / 3;
            int column = i % 3 - 1;
            BlockPos pos = arena.offset(column * 4, 0, 4 - row * 6);
            dummy.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            dummy.setNoAi(true);
            dummy.setCanPickUpLoot(false);
            dummy.setSilent(true);

            var hp = dummy.getAttribute(Attributes.MAX_HEALTH);
            if (hp != null) hp.setBaseValue(240 + i * 80);
            var knockback = dummy.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (knockback != null) knockback.setBaseValue(i < 3 ? 0.25 : 0.75);
            dummy.setHealth(dummy.getMaxHealth());
            dummy.setCustomName(Component.literal("기술 시험 표적 " + (i + 1)
                    + " · 체력 " + Math.round(dummy.getMaxHealth())
                    + " · 밀림 " + (i < 3 ? "큼" : "작음")));
            dummy.setCustomNameVisible(true);

            UUID id = dummy.getUUID();
            DUMMIES.add(id);
            OWNERS.put(id, player.getUUID());
            VillageWorldSystem.markAllowedGameMob(dummy);
            if (level.addFreshEntity(dummy)) spawned++;
            else {
                DUMMIES.remove(id);
                OWNERS.remove(id);
                VillageWorldSystem.unmarkAllowedGameMob(id);
            }
        }
        return "시험 표적 " + spawned + "개를 시험장 중앙에 배치했습니다.";
    }

    public static String clearTargets(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return "표적을 정리할 수 없습니다.";
        int count = 0;
        UUID owner = player.getUUID();
        for (UUID id : new HashSet<>(DUMMIES)) {
            if (!owner.equals(OWNERS.get(id))) continue;
            Entity entity = level.getEntity(id);
            if (entity != null) entity.discard();
            DUMMIES.remove(id);
            OWNERS.remove(id);
            VillageWorldSystem.unmarkAllowedGameMob(id);
            count++;
        }
        return "시험 표적 " + count + "개 정리";
    }

    public static void clearAll(MinecraftServer server) {
        for (UUID id : new HashSet<>(DUMMIES)) {
            Entity entity = server.overworld().getEntity(id);
            if (entity != null) entity.discard();
            VillageWorldSystem.unmarkAllowedGameMob(id);
        }
        DUMMIES.clear();
        OWNERS.clear();
        ENABLED.clear();
        RETURN_POINTS.clear();
        TEST_LOADOUTS.clear();
        TEST_ROLES.clear();
    }

    public static boolean isTestDummy(Entity entity) {
        return entity != null && DUMMIES.contains(entity.getUUID());
    }

    public static List<Mob> targetsNear(
            ServerLevel level, ServerPlayer player, double radius, int limit) {
        double squared = radius * radius;
        UUID owner = player.getUUID();
        List<Mob> result = new ArrayList<>();
        for (UUID id : new HashSet<>(DUMMIES)) {
            if (!owner.equals(OWNERS.get(id))) continue;
            Entity entity = level.getEntity(id);
            if (entity instanceof Mob mob && mob.isAlive() && mob.distanceToSqr(player) <= squared) {
                result.add(mob);
            }
        }
        result.sort(Comparator.comparingDouble(player::distanceToSqr));
        int capped = Math.max(0, limit);
        return result.size() <= capped ? result : new ArrayList<>(result.subList(0, capped));
    }

    private static void ensureDefaultLoadout(ServerPlayer player) {
        if (equippedSkill(player, 0).isPresent() || equippedSkill(player, 1).isPresent()) return;
        List<VillageRoleSkillSystem.ActiveSkill> skills =
                VillageRoleSkillSystem.skillsFor(selectedRole(player));
        if (!skills.isEmpty()) TEST_LOADOUTS.put(loadoutKey(player.getUUID(), 0), skills.get(0).id());
        if (skills.size() > 1) TEST_LOADOUTS.put(loadoutKey(player.getUUID(), 1), skills.get(1).id());
    }

    private static void clearLoadout(UUID owner) {
        TEST_LOADOUTS.remove(loadoutKey(owner, 0));
        TEST_LOADOUTS.remove(loadoutKey(owner, 1));
    }

    private static String loadoutKey(UUID owner, int slot) {
        return owner + "|" + (slot == 1 ? 1 : 0);
    }

    public static boolean handleManagementBox(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel)
                || !isEnabled(player)) return false;
        BlockPos clicked = event.getPos();
        BlockPos roleBox = roleManagementBoxPosition();
        BlockPos skillBox = skillManagementBoxPosition();
        if (roleBox == null || skillBox == null) return false;
        if (!clicked.equals(roleBox) && !clicked.equals(skillBox)) return false;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (clicked.equals(roleBox)) VillageUiController.openSkillTestRoleManager(player);
        else VillageUiController.openSkillTestSkillManager(player);
        return true;
    }

    public static BlockPos roleManagementBoxPosition() {
        BlockPos arena = arenaCenter();
        return arena == null ? null : arena.offset(-3, 0, 14);
    }

    public static BlockPos skillManagementBoxPosition() {
        BlockPos arena = arenaCenter();
        return arena == null ? null : arena.offset(3, 0, 14);
    }

    private static BlockPos arenaCenter() {
        return VillageCouncilState.villageCenter()
                .map(center -> center.offset(VillageWorldSystem.FORTRESS_RADIUS + 44, 0, 0))
                .orElse(null);
    }

    private static void buildArena(ServerLevel level, BlockPos center) {
        for (int dx = -ARENA_RADIUS; dx <= ARENA_RADIUS; dx++) {
            for (int dz = -ARENA_RADIUS; dz <= ARENA_RADIUS; dz++) {
                boolean edge = Math.abs(dx) == ARENA_RADIUS || Math.abs(dz) == ARENA_RADIUS;
                Block floor = edge ? Blocks.POLISHED_DEEPSLATE : Blocks.SMOOTH_STONE;
                VillageFortressTerrain.set(level, center.offset(dx, -1, dz), floor);
                for (int y = 0; y <= 8; y++) {
                    VillageFortressTerrain.set(level, center.offset(dx, y, dz), Blocks.AIR);
                }
                if (edge) VillageFortressTerrain.set(level, center.offset(dx, 0, dz), Blocks.STONE_BRICK_WALL);
            }
        }
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                VillageFortressTerrain.set(level,
                        center.offset(sx * (ARENA_RADIUS - 1), -1, sz * (ARENA_RADIUS - 1)),
                        Blocks.SEA_LANTERN);
            }
        }
        for (int x = -2; x <= 2; x++) {
            VillageFortressTerrain.set(level, center.offset(x, -1, 10), Blocks.GOLD_BLOCK);
        }
        BlockPos roleBox = roleManagementBoxPosition();
        BlockPos skillBox = skillManagementBoxPosition();
        if (roleBox != null) {
            VillageFortressTerrain.set(level, roleBox.below(), Blocks.GOLD_BLOCK);
            VillageFortressTerrain.set(level, roleBox, Blocks.BARREL);
            VillageFortressTerrain.set(level, roleBox.west(), Blocks.SEA_LANTERN);
        }
        if (skillBox != null) {
            VillageFortressTerrain.set(level, skillBox.below(), Blocks.LAPIS_BLOCK);
            VillageFortressTerrain.set(level, skillBox, Blocks.BARREL);
            VillageFortressTerrain.set(level, skillBox.east(), Blocks.SEA_LANTERN);
        }
    }

    private record ReturnPoint(double x, double y, double z, float yRot, float xRot) {}
}
