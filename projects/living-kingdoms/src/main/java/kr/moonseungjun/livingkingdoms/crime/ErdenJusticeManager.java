package kr.moonseungjun.livingkingdoms.crime;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.world.ErdenCapitalStreamingBuilder;
import kr.moonseungjun.livingkingdoms.world.ErdenPopulationSavedData;
import kr.moonseungjun.livingkingdoms.world.ExternalDistrictBuildingBuilder;
import kr.moonseungjun.livingkingdoms.world.RealmJurisdiction;
import kr.moonseungjun.livingkingdoms.world.RealmSitePlanner;
import kr.moonseungjun.livingkingdoms.world.StarterRealmManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Erden's loaded-city civic justice chain.
 *
 * <p>A crime does not become a magical warrant. A named, living resident must be physically loaded
 * near the incident, walk to a named guard-post worker and reach reporting distance. Only then is
 * the warrant written. The same population-backed guard walks to the suspect and must remain in
 * arrest range before custody begins. Detained suspects are brought before one of the authored
 * citizen courts and serve a bounded custodial sentence before the warrant is settled.</p>
 */
public final class ErdenJusticeManager {
    public static final String JURISDICTION = "erden_kingdom";
    private static final int PROCESS_INTERVAL = 10;
    private static final long WITNESS_EXPIRY_TICKS = 600L;
    private static final double WITNESS_RADIUS_SQR = 24.0D * 24.0D;
    private static final double REPORT_REACH_SQR = 9.0D;
    private static final double ARREST_REACH_SQR = 9.0D;
    private static final int ARREST_HOLD_TICKS = 60;
    private static final long DETENTION_BEFORE_TRIAL_TICKS = 100L;
    private static final long TRIAL_HEARING_TICKS = 40L;
    private static final double WALK_SPEED = 0.78D;
    private static final int MIN_SENTENCE_TICKS = 100;
    private static final int MAX_SENTENCE_TICKS = 1200;

    private ErdenJusticeManager() {
    }

    public static void observeCrime(ServerLevel level, ServerPlayer suspect,
                                    int severity, String offense, BlockPos incident) {
        if (!JURISDICTION.equals(RealmJurisdiction.at(level, incident))) return;
        ErdenJusticeSavedData data = level.getDataStorage().computeIfAbsent(ErdenJusticeSavedData.TYPE);
        ErdenJusticeSavedData.CaseRecord previous = data.caseFor(suspect.getUUID());
        ErdenJusticeSavedData.CaseRecord record = data.observe(
                suspect.getUUID(), offense, severity,
                incident.getX(), incident.getY(), incident.getZ(), level.getGameTime());
        if (previous == null) {
            suspect.sendSystemMessage(Component.literal(
                    "§6[사건 발생] §f에르덴에서는 실제 목격자가 경비대에 신고해야 수배가 발부됩니다."
            ));
            LivingKingdoms.LOGGER.info(
                    "Erden justice case observed case={} suspect={} offense={} severity={} immediate_warrant=false",
                    record.id(), suspect.getScoreboardName(), offense, record.severity());
        } else {
            LivingKingdoms.LOGGER.info(
                    "Erden justice case escalated case={} suspect={} offense={} severity={}",
                    record.id(), suspect.getScoreboardName(), offense, record.severity());
        }
    }

    public static boolean hasActiveCase(UUID suspect) {
        MinecraftServer server = StarterRealmManager.server();
        if (server == null) return false;
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null) return false;
        return level.getDataStorage().computeIfAbsent(ErdenJusticeSavedData.TYPE).caseFor(suspect) != null;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.getLevel(StarterRealmManager.REALM_KEY);
        if (level == null || !RealmSitePlanner.isBuilt(level, JURISDICTION)
                || level.getGameTime() % PROCESS_INTERVAL != 0L) return;
        process(level, level.getDataStorage().computeIfAbsent(ErdenJusticeSavedData.TYPE));
    }

    private static void process(ServerLevel level, ErdenJusticeSavedData data) {
        long tick = level.getGameTime();
        ErdenPopulationSavedData population = level.getDataStorage().computeIfAbsent(ErdenPopulationSavedData.TYPE);
        Map<String, ErdenPopulationSavedData.Resident> roster = livingRoster(population);
        Map<String, Villager> loaded = loadedResidents(level, roster);
        Set<String> guards = new HashSet<>();
        for (ErdenPopulationSavedData.Resident resident : roster.values()) {
            if (resident.workRole().equals("guard_post")) guards.add(resident.name());
        }

        for (ErdenJusticeSavedData.CaseRecord record : data.cases()) {
            ServerPlayer suspect = onlineSuspect(level, record.suspectId());
            switch (record.stage()) {
                case "observed" -> processObserved(level, data, record, suspect, loaded, guards, tick);
                case "reporting" -> processReporting(level, data, record, suspect, loaded, guards, tick);
                case "arresting" -> processArrest(level, data, record, suspect, loaded, guards, tick);
                case "detained" -> processDetention(level, data, record, suspect, tick);
                case "trial" -> processTrial(level, data, record, suspect, tick);
                case "sentenced" -> processSentence(level, data, record, suspect, tick);
                default -> data.close(record.id());
            }
        }
    }

    private static void processObserved(
            ServerLevel level,
            ErdenJusticeSavedData data,
            ErdenJusticeSavedData.CaseRecord record,
            ServerPlayer suspect,
            Map<String, Villager> loaded,
            Set<String> guardNames,
            long tick) {
        if (tick - record.createdTick() > WITNESS_EXPIRY_TICKS) {
            data.close(record.id());
            if (suspect != null) suspect.sendSystemMessage(Component.literal(
                    "§7[사건 종결] §f확인 가능한 목격 신고가 없어 사건이 수배로 전환되지 않았습니다."
            ));
            return;
        }
        Villager witness = loaded.values().stream()
                .filter(Villager::isAlive)
                .filter(villager -> !guardNames.contains(villager.getName().getString()))
                .filter(villager -> villager.distanceToSqr(
                        record.incidentX() + 0.5D, record.incidentY(), record.incidentZ() + 0.5D)
                        <= WITNESS_RADIUS_SQR)
                .min(Comparator.comparingDouble(villager -> villager.distanceToSqr(
                        record.incidentX() + 0.5D, record.incidentY(), record.incidentZ() + 0.5D)))
                .orElse(null);
        if (witness == null) return;
        data.assignWitness(record.id(), witness.getName().getString(), tick);
        if (suspect != null) suspect.sendSystemMessage(Component.literal(
                "§c[목격] §f" + witness.getName().getString() + "이(가) 사건을 목격하고 경비대에 신고하러 갑니다."
        ));
        LivingKingdoms.LOGGER.info(
                "Erden justice witness assigned case={} witness={} actual_resident=true physical_report_required=true",
                record.id(), witness.getName().getString());
    }

    private static void processReporting(
            ServerLevel level,
            ErdenJusticeSavedData data,
            ErdenJusticeSavedData.CaseRecord record,
            ServerPlayer suspect,
            Map<String, Villager> loaded,
            Set<String> guardNames,
            long tick) {
        Villager witness = loaded.get(record.witnessName());
        if (witness == null || !witness.isAlive()) {
            if (tick - record.stageTick() > WITNESS_EXPIRY_TICKS) data.close(record.id());
            return;
        }
        Villager guard = nearestGuard(loaded, guardNames, witness.blockPosition());
        if (guard == null) return;
        if (witness.distanceToSqr(guard) > REPORT_REACH_SQR) {
            witness.getNavigation().moveTo(guard.getX(), guard.getY(), guard.getZ(), WALK_SPEED);
            return;
        }
        witness.getNavigation().stop();
        CrimeSavedData crime = level.getDataStorage().computeIfAbsent(CrimeSavedData.TYPE);
        UUID suspectId = UUID.fromString(record.suspectId());
        CrimeSavedData.CrimeRecord warrant = crime.addCrime(
                suspectId, JURISDICTION, record.severity(), tick);
        data.issueWarrant(record.id(), guard.getName().getString(), tick);
        if (suspect != null) suspect.sendSystemMessage(Component.literal(
                "§c[에르덴 수배] §f목격 신고가 경비대에 전달됐습니다. 수배도 §e" + warrant.wanted()
                        + "§f. 실제 경비대가 체포를 시도합니다."
        ));
        LivingKingdoms.LOGGER.info(
                "Erden justice report delivered case={} witness={} guard={} actual_guard_worker=true warrant_after_report=true",
                record.id(), witness.getName().getString(), guard.getName().getString());
    }

    private static void processArrest(
            ServerLevel level,
            ErdenJusticeSavedData data,
            ErdenJusticeSavedData.CaseRecord record,
            ServerPlayer suspect,
            Map<String, Villager> loaded,
            Set<String> guardNames,
            long tick) {
        if (suspect == null) return;
        if (!JURISDICTION.equals(RealmJurisdiction.at(level, suspect.blockPosition()))) {
            data.setHoldTicks(record.id(), 0);
            return;
        }
        Villager guard = loaded.get(record.guardName());
        if (guard == null || !guard.isAlive() || !guardNames.contains(record.guardName())) {
            guard = nearestGuard(loaded, guardNames, suspect.blockPosition());
            if (guard == null) return;
            data.reassignGuard(record.id(), guard.getName().getString(), tick);
        }
        guard.setPersistenceRequired();
        if (guard.distanceToSqr(suspect) > ARREST_REACH_SQR) {
            if (record.holdTicks() != 0) data.setHoldTicks(record.id(), 0);
            guard.getNavigation().moveTo(suspect.getX(), suspect.getY(), suspect.getZ(), WALK_SPEED + 0.08D);
            return;
        }
        guard.getNavigation().stop();
        int hold = record.holdTicks() + PROCESS_INTERVAL;
        data.setHoldTicks(record.id(), hold);
        if (hold < ARREST_HOLD_TICKS) return;
        detain(level, data, record, suspect, guard, tick);
    }

    private static void detain(
            ServerLevel level,
            ErdenJusticeSavedData data,
            ErdenJusticeSavedData.CaseRecord record,
            ServerPlayer suspect,
            Villager guard,
            long tick) {
        BlockPos jail = RealmJurisdiction.jail(level, JURISDICTION);
        suspect.setDeltaMovement(0.0D, 0.0D, 0.0D);
        suspect.teleportTo(level, jail.getX() + 0.5D, jail.getY(), jail.getZ() + 0.5D,
                Set.<Relative>of(), suspect.getYRot(), suspect.getXRot(), true);
        data.advance(record.id(), "detained", tick);
        suspect.sendSystemMessage(Component.literal(
                "§6[체포·구금] §f" + guard.getName().getString()
                        + "에게 현장에서 체포됐습니다. 시민법정 심리를 기다립니다."
        ));
        LivingKingdoms.LOGGER.info(
                "Erden justice detained case={} guard={} proximity_arrest=true synthetic_guard=false",
                record.id(), guard.getName().getString());
    }

    private static void processDetention(
            ServerLevel level,
            ErdenJusticeSavedData data,
            ErdenJusticeSavedData.CaseRecord record,
            ServerPlayer suspect,
            long tick) {
        if (suspect == null || tick - record.stageTick() < DETENTION_BEFORE_TRIAL_TICKS) return;
        ExternalDistrictBuildingBuilder.BuildingEntrance court = nearestCourt(suspect.blockPosition());
        if (court == null) return;
        int chunkX = court.roadX() >> 4;
        int chunkZ = court.roadZ() >> 4;
        if (!ErdenCapitalStreamingBuilder.isChunkBuilt(level, chunkX, chunkZ)) {
            ErdenCapitalStreamingBuilder.requestChunk(level, chunkX, chunkZ);
            return;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, court.roadX(), court.roadZ());
        suspect.teleportTo(level, court.roadX() + 0.5D, y, court.roadZ() + 0.5D,
                Set.<Relative>of(), suspect.getYRot(), suspect.getXRot(), true);
        data.beginTrial(record.id(), court.role(), court.roadX(), court.roadZ(), tick);
        suspect.sendSystemMessage(Component.literal(
                "§6[시민법정] §f" + court.role() + "에서 사건 심리가 시작됩니다."
        ));
        LivingKingdoms.LOGGER.info(
                "Erden justice trial opened case={} court={} actual_citizen_court=true bounded_court_chunk=true",
                record.id(), court.role());
    }

    private static void processTrial(
            ServerLevel level,
            ErdenJusticeSavedData data,
            ErdenJusticeSavedData.CaseRecord record,
            ServerPlayer suspect,
            long tick) {
        if (suspect == null || tick - record.stageTick() < TRIAL_HEARING_TICKS) return;
        CrimeSavedData crime = level.getDataStorage().computeIfAbsent(CrimeSavedData.TYPE);
        CrimeSavedData.CrimeRecord wanted = crime.record(suspect.getUUID());
        int sentenceTicks = sentenceTicks(record.severity(), wanted.resistance());
        data.advance(record.id(), "sentenced", tick);
        BlockPos jail = RealmJurisdiction.jail(level, JURISDICTION);
        suspect.teleportTo(level, jail.getX() + 0.5D, jail.getY(), jail.getZ() + 0.5D,
                Set.<Relative>of(), suspect.getYRot(), suspect.getXRot(), true);
        suspect.sendSystemMessage(Component.literal(
                "§c[판결] §f유죄. 죄목: " + record.offense() + ", 형기 §e"
                        + Math.max(5, sentenceTicks / 20) + "초§f. 저항 기록도 형량에 반영됩니다."
        ));
        LivingKingdoms.LOGGER.info(
                "Erden justice verdict case={} guilty=true severity={} resistance={} sentence_ticks={} court={}",
                record.id(), record.severity(), wanted.resistance(), sentenceTicks, record.courtRole());
    }

    private static void processSentence(
            ServerLevel level,
            ErdenJusticeSavedData data,
            ErdenJusticeSavedData.CaseRecord record,
            ServerPlayer suspect,
            long tick) {
        if (suspect == null) return;
        CrimeSavedData crime = level.getDataStorage().computeIfAbsent(CrimeSavedData.TYPE);
        CrimeSavedData.CrimeRecord wanted = crime.record(suspect.getUUID());
        int sentenceTicks = sentenceTicks(record.severity(), wanted.resistance());
        BlockPos jail = RealmJurisdiction.jail(level, JURISDICTION);
        if (suspect.blockPosition().distSqr(jail) > 12L * 12L) {
            suspect.teleportTo(level, jail.getX() + 0.5D, jail.getY(), jail.getZ() + 0.5D,
                    Set.<Relative>of(), suspect.getYRot(), suspect.getXRot(), true);
        }
        if (tick - record.stageTick() < sentenceTicks) return;
        crime.settleAfterArrest(suspect.getUUID());
        data.close(record.id());
        suspect.sendSystemMessage(Component.literal(
                "§a[형 집행 완료] §f에르덴 시민법정의 형기를 마쳐 석방됐습니다."
        ));
        LivingKingdoms.LOGGER.info(
                "Erden justice sentence completed case={} physical_witness=true physical_report=true resident_guard=true court_trial=true sentence_executed=true",
                record.id());
    }

    private static int sentenceTicks(int severity, int resistance) {
        return Math.min(MAX_SENTENCE_TICKS,
                Math.max(MIN_SENTENCE_TICKS, 100 + Math.max(1, severity) * 6 + Math.max(0, resistance) * 20));
    }

    private static Map<String, ErdenPopulationSavedData.Resident> livingRoster(
            ErdenPopulationSavedData population) {
        Map<String, ErdenPopulationSavedData.Resident> roster = new HashMap<>();
        for (ErdenPopulationSavedData.Household household : population.households()) {
            for (ErdenPopulationSavedData.Resident resident : household.residents()) {
                if (!population.isDead(resident.id())) roster.putIfAbsent(resident.name(), resident);
            }
        }
        return roster;
    }

    private static Map<String, Villager> loadedResidents(
            ServerLevel level,
            Map<String, ErdenPopulationSavedData.Resident> roster) {
        Map<String, Villager> loaded = new HashMap<>();
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class, capitalBounds(level),
                villager -> roster.containsKey(villager.getName().getString()))) {
            loaded.putIfAbsent(villager.getName().getString(), villager);
        }
        return loaded;
    }

    private static Villager nearestGuard(
            Map<String, Villager> loaded,
            Set<String> guardNames,
            BlockPos origin) {
        return loaded.values().stream()
                .filter(Villager::isAlive)
                .filter(villager -> guardNames.contains(villager.getName().getString()))
                .min(Comparator.comparingDouble(villager -> villager.distanceToSqr(
                        origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D)))
                .orElse(null);
    }

    private static ExternalDistrictBuildingBuilder.BuildingEntrance nearestCourt(BlockPos origin) {
        return ExternalDistrictBuildingBuilder.entrances().stream()
                .filter(entrance -> entrance.role().startsWith("citizen_court_"))
                .min(Comparator.comparingLong(entrance -> distanceSquared2d(
                        origin.getX(), origin.getZ(), entrance.roadX(), entrance.roadZ())))
                .orElse(null);
    }

    private static ServerPlayer onlineSuspect(ServerLevel level, String suspectId) {
        try {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(UUID.fromString(suspectId));
            return player != null && player.level() == level ? player : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static AABB capitalBounds(ServerLevel level) {
        return new AABB(
                ErdenCapitalStreamingBuilder.WEST_WALL_X - 16,
                level.getMinY(),
                ErdenCapitalStreamingBuilder.NORTH_WALL_Z - 16,
                ErdenCapitalStreamingBuilder.EAST_WALL_X + 16,
                level.getMaxY(),
                ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + 16);
    }

    private static long distanceSquared2d(int x1, int z1, int x2, int z2) {
        long dx = (long) x1 - x2;
        long dz = (long) z1 - z2;
        return dx * dx + dz * dz;
    }
}
