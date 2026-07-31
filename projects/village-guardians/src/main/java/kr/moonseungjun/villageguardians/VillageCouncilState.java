package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class VillageCouncilState {
    public static final int VILLAGE_RADIUS = 64;
    public static final float VILLAGE_DEFENSE_XP_MULTIPLIER = 1.5f;

    private static final Map<UUID, VillageRole> ROLES = new LinkedHashMap<>();
    private static final Map<UUID, RpgProgress> RPG_PROGRESS = new LinkedHashMap<>();

    private static VillageSavedData savedData;
    private static UUID mayorId;
    private static String mayorName = "없음";
    private static int villageDay = 1;
    private static VillageTimePhase timePhase = VillageTimePhase.MORNING;
    private static BlockPos villageCenter;
    private static Proposal activeProposal;

    private VillageCouncilState() {
    }

    public static synchronized void initializeServer(MinecraftServer server) {
        savedData = server.overworld().getDataStorage().computeIfAbsent(VillageSavedData.TYPE);

        ROLES.clear();
        ROLES.putAll(savedData.roles());
        RPG_PROGRESS.clear();
        RPG_PROGRESS.putAll(savedData.rpgProgression());
        mayorId = savedData.mayorId().orElse(null);
        mayorName = mayorId == null ? "없음" : savedData.mayorName();
        villageDay = savedData.villageDay();
        timePhase = savedData.timePhase();
        villageCenter = savedData.villageCenter().orElse(null);
        activeProposal = null;
        freezeAndApplyTime(server);
    }

    public static synchronized void registerPlayer(ServerPlayer player) {
        boolean changed = false;
        if (mayorId == null) {
            mayorId = player.getUUID();
            mayorName = player.getGameProfile().name();
            changed = true;
            broadcast(player.level().getServer(), "§6" + mayorName + "§f 님이 첫 임시 촌장이 되었습니다.");
        }
        if (!RPG_PROGRESS.containsKey(player.getUUID())) {
            RPG_PROGRESS.put(player.getUUID(), RpgProgress.initial());
            changed = true;
        }
        if (changed) {
            persist();
        }
    }

    public static synchronized boolean isMayor(ServerPlayer player) {
        return player.getUUID().equals(mayorId);
    }

    public static synchronized Optional<VillageRole> roleOf(UUID playerId) {
        return Optional.ofNullable(ROLES.get(playerId));
    }

    public static synchronized int levelOf(UUID playerId) {
        return progressOf(playerId).level();
    }

    public static synchronized RpgProgress progressOf(UUID playerId) {
        return RPG_PROGRESS.getOrDefault(playerId, RpgProgress.initial());
    }

    public static synchronized Optional<BlockPos> villageCenter() {
        return Optional.ofNullable(villageCenter);
    }

    public static synchronized int currentDay() {
        return villageDay;
    }

    public static synchronized VillageTimePhase currentPhase() {
        return timePhase;
    }

    public static synchronized String setVillageCenter(ServerPlayer actor) {
        if (!isMayor(actor)) {
            return "촌장만 마을 중심을 지정할 수 있습니다.";
        }
        MinecraftServer server = actor.level().getServer();
        if (server == null || actor.level() != server.overworld()) {
            return "마을 중심은 오버월드에서만 지정할 수 있습니다.";
        }

        villageCenter = actor.blockPosition().immutable();
        persist();
        broadcast(server, "§6[마을 지정] §f마을 중심이 " + formatPos(villageCenter)
                + "에 지정되었습니다. 보호·방어 영역 반경은 " + VILLAGE_RADIUS + "블록입니다.");
        return "마을 중심 지정 완료: " + formatPos(villageCenter);
    }

    public static synchronized boolean isInsideVillage(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null || villageCenter == null || player.level() != server.overworld()) {
            return false;
        }
        return distanceSquared(player.blockPosition(), villageCenter) <= (long) VILLAGE_RADIUS * VILLAGE_RADIUS;
    }

    public static synchronized String villageStatus(ServerPlayer viewer) {
        if (villageCenter == null) {
            return "§6[마을 영역] §f중심 미지정 | 첫 촌장 접속 시 전용 마을이 자동 배치됩니다.";
        }

        MinecraftServer server = viewer.level().getServer();
        if (server == null || viewer.level() != server.overworld()) {
            return "§6[마을 영역] §f중심 " + formatPos(villageCenter)
                    + " | 반경 " + VILLAGE_RADIUS + " | 현재 다른 차원에 있습니다.";
        }

        double distance = Math.sqrt(distanceSquared(viewer.blockPosition(), villageCenter));
        return "§6[마을 영역] §f중심 " + formatPos(villageCenter)
                + " | 반경 " + VILLAGE_RADIUS
                + " | 중심까지 " + Math.round(distance) + "블록"
                + " | 현재 " + (distance <= VILLAGE_RADIUS ? "마을 내부" : "마을 외부");
    }

    public static synchronized String chooseRole(ServerPlayer player, VillageRole role) {
        ROLES.put(player.getUUID(), role);
        RPG_PROGRESS.putIfAbsent(player.getUUID(), RpgProgress.initial());
        persist();
        return player.getGameProfile().name() + "님의 역할이 " + role.displayName() + "(으)로 정해졌습니다.";
    }

    public static synchronized String transferMayor(ServerPlayer actor, ServerPlayer target) {
        if (!isMayor(actor)) {
            return "촌장만 촌장직을 넘길 수 있습니다.";
        }
        mayorId = target.getUUID();
        mayorName = target.getGameProfile().name();
        activeProposal = null;
        persist();
        VillageStarterKit.grantMayorCaller(target);
        broadcast(actor.level().getServer(), "§6촌장직이 " + mayorName + "§f 님에게 넘어갔습니다.");
        return "촌장직 이전 완료";
    }

    public static synchronized String proposeAdvanceTime(ServerPlayer proposer) {
        if (!isMayor(proposer)) {
            return "촌장만 마을 전체 안건을 발의할 수 있습니다.";
        }
        if (VillageRaidSystem.isRaidLocked()) {
            return "습격이 끝날 때까지 시간을 진행할 수 없습니다.";
        }
        if (activeProposal != null) {
            return "이미 진행 중인 안건이 있습니다.";
        }

        MinecraftServer server = proposer.level().getServer();
        if (server == null) {
            return "서버 상태를 확인할 수 없습니다.";
        }
        activeProposal = new Proposal("advance_time", proposer.getUUID(), new LinkedHashMap<>());
        activeProposal.votes().put(proposer.getUUID(), true);
        broadcast(server, "§e[마을 투표] §f다음 시간 단계로 진행할지 투표합니다. /vg vote yes 또는 /vg vote no");
        evaluateProposal(server);
        return "시간 진행 안건을 발의했습니다.";
    }

    public static synchronized String vote(ServerPlayer player, boolean yes) {
        if (activeProposal == null) {
            return "현재 진행 중인 안건이 없습니다.";
        }

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return "서버 상태를 확인할 수 없습니다.";
        }
        activeProposal.votes().put(player.getUUID(), yes);
        String result = player.getGameProfile().name() + "님이 " + (yes ? "찬성" : "반대") + "에 투표했습니다.";
        broadcast(server, result);
        evaluateProposal(server);
        return result;
    }

    public static synchronized ExperienceResult grantExperience(ServerPlayer player, int requestedAmount) {
        int amount = Math.max(0, requestedAmount);
        RpgProgress previous = progressOf(player.getUUID());
        int level = previous.level();
        int experience = previous.experience();
        int levelsGained = 0;

        if (level < RpgProgress.MAX_LEVEL) {
            experience += amount;
            while (level < RpgProgress.MAX_LEVEL) {
                int required = 60 + level * 40;
                if (experience < required) {
                    break;
                }
                experience -= required;
                level++;
                levelsGained++;
            }
        }

        RpgProgress updated = new RpgProgress(level, experience);
        RPG_PROGRESS.put(player.getUUID(), updated);
        persist();

        if (levelsGained > 0) {
            player.heal(player.getMaxHealth());
            broadcast(player.level().getServer(), "§d[성장] §f" + player.getGameProfile().name()
                    + " 님이 레벨 " + level + "에 도달했습니다. 전투력이 크게 상승합니다!");
        }
        return new ExperienceResult(amount, previous, updated, levelsGained);
    }

    public static synchronized String rpgStatus(ServerPlayer player) {
        RpgProgress progress = progressOf(player.getUUID());
        String next = progress.level() >= RpgProgress.MAX_LEVEL
                ? "최고 레벨"
                : progress.experience() + "/" + progress.experienceToNextLevel() + " XP";
        return "§d[RPG 성장] §f레벨 " + progress.level()
                + " | " + next
                + " | 공격 x" + String.format(java.util.Locale.ROOT, "%.2f", VillageRpgSystem.outgoingDamageMultiplier(progress.level()))
                + " | 받는 피해 " + Math.round(VillageRpgSystem.incomingDamageMultiplier(progress.level()) * 100.0f) + "%"
                + " | 추가 체력 " + VillageRpgSystem.bonusHealthPoints(progress.level());
    }

    public static synchronized String status(MinecraftServer server, ServerPlayer viewer) {
        String viewerRole = ROLES.containsKey(viewer.getUUID())
                ? ROLES.get(viewer.getUUID()).displayName()
                : "미선택";
        String voteStatus = activeProposal == null
                ? "진행 중인 안건 없음"
                : activeProposal.id() + " / 찬성 " + countVotes(server, true)
                + " / 반대 " + countVotes(server, false)
                + " / 통과 기준 " + majority(server);
        String territoryStatus = villageCenter == null
                ? "중심 미지정"
                : (isInsideVillage(viewer) ? "마을 내부" : "마을 외부");

        return "§6[마을 현황] §f촌장: " + mayorName
                + " | 내 역할: " + viewerRole
                + " | 레벨 " + levelOf(viewer.getUUID())
                + " | " + territoryStatus
                + " | 제 " + villageDay + "일 " + timePhase.koreanName()
                + " | " + voteStatus;
    }

    public static synchronized void enforceFrozenTime(MinecraftServer server) {
        freezeAndApplyTime(server);
    }

    public static synchronized void completeRaid(MinecraftServer server) {
        if (timePhase == VillageTimePhase.NIGHT) {
            villageDay++;
        }
        timePhase = VillageTimePhase.MORNING;
        activeProposal = null;
        persist();
        freezeAndApplyTime(server);
        broadcast(server, "§b마을 시간이 제 " + villageDay + "일 아침으로 진행되었습니다. 정비를 시작하세요.");
    }

    private static void evaluateProposal(MinecraftServer server) {
        if (activeProposal == null) {
            return;
        }

        int required = majority(server);
        int yesVotes = countVotes(server, true);
        int noVotes = countVotes(server, false);

        if (yesVotes >= required) {
            String proposalId = activeProposal.id();
            activeProposal = null;
            if ("advance_time".equals(proposalId)) {
                advanceTime(server);
            }
            broadcast(server, "§a[투표 통과] §f마을 결정이 실행되었습니다.");
        } else if (noVotes >= required) {
            activeProposal = null;
            broadcast(server, "§c[투표 부결] §f안건이 취소되었습니다.");
        }
    }

    private static void advanceTime(MinecraftServer server) {
        VillageTimePhase previous = timePhase;
        timePhase = timePhase.next();
        if (previous == VillageTimePhase.NIGHT && timePhase == VillageTimePhase.MORNING) {
            villageDay++;
        }
        persist();
        freezeAndApplyTime(server);
        broadcast(server, "§b마을 시간이 제 " + villageDay + "일 " + timePhase.koreanName() + "(으)로 진행되었습니다.");
        VillageRaidSystem.onPhaseChanged(server, timePhase);
    }

    private static void persist() {
        if (savedData != null) {
            savedData.replaceState(mayorId, mayorName, villageDay, timePhase, villageCenter, ROLES, RPG_PROGRESS);
        }
    }

    private static void freezeAndApplyTime(MinecraftServer server) {
        var overworld = server.overworld();
        overworld.getGameRules().set(GameRules.ADVANCE_TIME, false, server);
        var defaultClock = overworld.dimensionType().defaultClock().orElse(null);
        if (defaultClock != null) {
            overworld.clockManager().setTotalTicks(defaultClock, timePhase.minecraftTime());
        }
    }

    private static int majority(MinecraftServer server) {
        int online = Math.max(1, server.getPlayerList().getPlayerCount());
        return online / 2 + 1;
    }

    private static int countVotes(MinecraftServer server, boolean value) {
        if (activeProposal == null) {
            return 0;
        }
        return (int) activeProposal.votes().entrySet().stream()
                .filter(entry -> server.getPlayerList().getPlayer(entry.getKey()) != null)
                .filter(entry -> entry.getValue() == value)
                .count();
    }

    private static long distanceSquared(BlockPos first, BlockPos second) {
        long dx = (long) first.getX() - second.getX();
        long dy = (long) first.getY() - second.getY();
        long dz = (long) first.getZ() - second.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static String formatPos(BlockPos pos) {
        return "(" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }

    private static void broadcast(MinecraftServer server, String text) {
        if (server != null) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(text), false);
        }
    }

    public record ExperienceResult(
            int awardedExperience,
            RpgProgress previous,
            RpgProgress current,
            int levelsGained) {
    }

    private record Proposal(String id, UUID proposer, Map<UUID, Boolean> votes) {
    }
}
