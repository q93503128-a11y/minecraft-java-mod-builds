package kr.moonseungjun.livingkingdoms.profile;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.foundation.FoundationCatalog;
import kr.moonseungjun.livingkingdoms.foundation.PlayableOriginCatalog;
import kr.moonseungjun.livingkingdoms.network.OpenOriginScreenPayload;
import kr.moonseungjun.livingkingdoms.network.OriginSubmissionResultPayload;
import kr.moonseungjun.livingkingdoms.network.SubmitOriginPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

public final class OriginProfileManager {
    public static final int ORIGIN_SCHEMA_VERSION = 1;

    private static LivingKingdomsSavedData savedData;

    private OriginProfileManager() {
    }

    public static synchronized void initialize(MinecraftServer server) {
        FoundationCatalog.bootstrap();
        PlayableOriginCatalog.residences();
        savedData = server.overworld().getDataStorage().computeIfAbsent(LivingKingdomsSavedData.TYPE);
        LivingKingdoms.LOGGER.info("Loaded {} Living Kingdoms player origin profile(s)", savedData.profileCount());
    }

    public static synchronized Optional<OriginProfile> profile(UUID playerId) {
        return savedData == null ? Optional.empty() : savedData.profile(playerId);
    }

    public static synchronized boolean requiresSelection(UUID playerId) {
        return profile(playerId).isEmpty();
    }

    public static void requestSelection(ServerPlayer player) {
        if (requiresSelection(player.getUUID())) {
            PacketDistributor.sendToPlayer(player, new OpenOriginScreenPayload(ORIGIN_SCHEMA_VERSION));
        }
    }

    public static synchronized OriginSubmissionResultPayload submit(ServerPlayer player, SubmitOriginPayload payload) {
        if (savedData == null) {
            return new OriginSubmissionResultPayload(false, "세계 저장 데이터가 아직 준비되지 않았습니다.");
        }
        if (savedData.profile(player.getUUID()).isPresent()) {
            return new OriginSubmissionResultPayload(false, "이미 출신 선택을 완료했습니다.");
        }
        if (!isSafeId(payload.speciesId()) || !isSafeId(payload.homelandId())
                || !isSafeId(payload.backgroundId()) || !isSafeId(payload.residenceId())) {
            return new OriginSubmissionResultPayload(false, "허용되지 않은 선택 ID입니다.");
        }

        PlayableOriginCatalog.ValidationResult validation = PlayableOriginCatalog.validate(
                payload.speciesId(), payload.homelandId(), payload.backgroundId(), payload.residenceId()
        );
        if (!validation.valid()) {
            String reason = validation.errors().isEmpty()
                    ? "선택 조합이 유효하지 않습니다."
                    : validation.errors().getFirst();
            return new OriginSubmissionResultPayload(false, reason);
        }

        OriginProfile profile = new OriginProfile(
                payload.speciesId(),
                payload.homelandId(),
                payload.backgroundId(),
                payload.residenceId(),
                player.level().getGameTime()
        );
        savedData.putProfile(player.getUUID(), profile);
        LivingKingdoms.LOGGER.info(
                "Player {} completed origin selection: species={}, homeland={}, background={}, residence={}",
                player.getGameProfile().name(), profile.speciesId(), profile.homelandId(),
                profile.backgroundId(), profile.residenceId()
        );
        return new OriginSubmissionResultPayload(true, "출신이 확정되었습니다. 선택한 거주지로 이동합니다.");
    }

    private static boolean isSafeId(String value) {
        return value != null && value.length() <= 64 && value.matches("[a-z0-9_]+");
    }
}
