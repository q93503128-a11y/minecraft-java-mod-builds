package io.github.q93503128.turnbound.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-shot presentation for campaign gates and chapter hand-offs.
 *
 * Quest counters, encounter clears and physical gates remain authoritative in their existing systems. This layer only
 * notices when one of those canonical states becomes true during the current play session, then gives the change a
 * visible field beat: a short directional trail, a pulse at the newly relevant route and an in-world line of text.
 * Existing saves are seeded silently on first sync so loading a late-game profile never replays every old unlock.
 */
public final class AsterMarchProgressStaging {
    private enum Stage {
        MEADOW_DEEP,
        MEADOW_BOSS,
        CHAPTER_1_CLEAR,
        GLOAM_DEEP,
        GLOAM_BOSS,
        CHAPTER_2_CLEAR,
        AQUEDUCT_LOWER,
        AQUEDUCT_BOSS,
        CHAPTER_3_CLEAR,
        QUARRY_DEEP,
        QUARRY_BOSS,
        CHAPTER_4_CLEAR,
        RELAY_ENTRANCE,
        RELAY_BOSS,
        RELAY_RECONNECT,
        ENDGAME
    }

    private static final Map<UUID, EnumSet<Stage>> SEEN = new ConcurrentHashMap<>();

    private AsterMarchProgressStaging() {}

    public static void tick(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        EnumSet<Stage> now = current(player);
        EnumSet<Stage> seen = SEEN.get(player.getUUID());
        if (seen == null) {
            SEEN.put(player.getUUID(), now.clone());
            return;
        }
        for (Stage stage : Stage.values()) {
            if (now.contains(stage) && !seen.contains(stage)) present(level, player, stage);
        }
        seen.addAll(now);
    }

    public static void remove(ServerPlayer player) {
        if (player != null) SEEN.remove(player.getUUID());
    }

    private static EnumSet<Stage> current(ServerPlayer player) {
        var snapshot = CampaignProgressStore.snapshot(player.getUUID());
        Set<String> clears = snapshot.clearedEncounters();
        Set<String> quests = snapshot.quests().completed();
        Set<String> flags = snapshot.quests().unlockFlags();
        EnumSet<Stage> out = EnumSet.noneOf(Stage.class);

        if (clears.contains("ENC_M01") && clears.contains("ENC_M02")) out.add(Stage.MEADOW_DEEP);
        if (clears.contains("ENC_M04")) out.add(Stage.MEADOW_BOSS);
        if (clears.contains("BATTLE_B01") || quests.contains("MQ_C01_03_graul")) out.add(Stage.CHAPTER_1_CLEAR);

        if (quests.contains("MQ_C02_01_spores") || flags.contains("GLOAM_DEEP_PATH")) out.add(Stage.GLOAM_DEEP);
        if (quests.contains("MQ_C02_02_root_wall") || flags.contains("B02_GATE")) out.add(Stage.GLOAM_BOSS);
        if (clears.contains("BATTLE_B02") || quests.contains("MQ_C02_03_verna")) out.add(Stage.CHAPTER_2_CLEAR);

        if (quests.contains("MQ_C03_01_dry_channel") || flags.contains("AQUEDUCT_LOWER")) out.add(Stage.AQUEDUCT_LOWER);
        if (quests.contains("MQ_C03_02_old_orders") || flags.contains("ORO_ROOM")) out.add(Stage.AQUEDUCT_BOSS);
        if (clears.contains("BATTLE_B03") || quests.contains("MQ_C03_03_oro7")) out.add(Stage.CHAPTER_3_CLEAR);

        if (quests.contains("MQ_C04_01_ash_route") || flags.contains("FT_QUARRY")) out.add(Stage.QUARRY_DEEP);
        if (quests.contains("MQ_C04_02_core_fragment") || flags.contains("B04_GATE")) out.add(Stage.QUARRY_BOSS);
        if (clears.contains("BATTLE_B04") || quests.contains("MQ_C04_03_kolvak")) out.add(Stage.CHAPTER_4_CLEAR);

        if (quests.contains("MQ_C05_01_relay_key") || flags.contains("OLD_RELAY_ENTRANCE")) out.add(Stage.RELAY_ENTRANCE);
        if (quests.contains("MQ_C05_02_serak_record") || flags.contains("B05_GATE")) out.add(Stage.RELAY_BOSS);
        if (clears.contains("BATTLE_B05")) out.add(Stage.RELAY_RECONNECT);
        if (quests.contains("MQ_C05_03_reconnect") || flags.contains("ENDGAME")) out.add(Stage.ENDGAME);
        return out;
    }

    private static void present(ServerLevel level, ServerPlayer player, Stage stage) {
        switch (stage) {
            case MEADOW_DEEP -> cue(level, player, new Vec3(190, 67, 230), ParticleTypes.HAPPY_VILLAGER,
                    ChatFormatting.AQUA, "남문 초원의 봉쇄가 풀렸다. 계전소 너머 심부 길이 이어진다.");
            case MEADOW_BOSS -> cue(level, player, new Vec3(344, 68, 245), ParticleTypes.CLOUD,
                    ChatFormatting.RED, "무너진 봉쇄선 너머에서 거대한 흔적이 이어진다. 그라울의 길이 열렸다.");
            case CHAPTER_1_CLEAR -> cue(level, player, new Vec3(0, 66, -108), ParticleTypes.END_ROD,
                    ChatFormatting.GOLD, "남문 전선이 잠잠해졌다. 라디아 북쪽에서 그늘숲으로 향하는 길이 응답한다.");

            case GLOAM_DEEP -> cue(level, player, new Vec3(-40, 70, -300), ParticleTypes.SPORE_BLOSSOM_AIR,
                    ChatFormatting.DARK_GREEN, "포자등불의 간섭이 잦아들며 숲의 깊은 길이 모습을 드러낸다.");
            case GLOAM_BOSS -> cue(level, player, new Vec3(-35, 72, -426), ParticleTypes.ENCHANT,
                    ChatFormatting.LIGHT_PURPLE, "뿌리 장벽이 갈라졌다. 꽃잎 너머의 중심부로 길이 이어진다.");
            case CHAPTER_2_CLEAR -> cue(level, player, new Vec3(-124, 66, 20), ParticleTypes.END_ROD,
                    ChatFormatting.GOLD, "그늘숲의 맥동이 멎었다. 라디아 서쪽의 오래된 수로가 다시 길을 낸다.");

            case AQUEDUCT_LOWER -> cue(level, player, new Vec3(-320, 67, 20), ParticleTypes.DRIPPING_WATER,
                    ChatFormatting.AQUA, "압력이 돌아오며 하층 수로의 잠금이 풀린다.");
            case AQUEDUCT_BOSS -> cue(level, player, new Vec3(-418, 64, 35), ParticleTypes.ELECTRIC_SPARK,
                    ChatFormatting.RED, "마지막 방위 명령이 끊겼다. ORO-7의 관리실이 노출된다.");
            case CHAPTER_3_CLEAR -> cue(level, player, new Vec3(-52, 68, 296), ParticleTypes.END_ROD,
                    ChatFormatting.GOLD, "수문의 명령음이 사라졌다. 남쪽의 잿불 채석장으로 이어지는 통로가 열린다.");

            case QUARRY_DEEP -> cue(level, player, new Vec3(20, 70, 405), ParticleTypes.ASH,
                    ChatFormatting.YELLOW, "표층 전선이 무너졌다. 열기 너머의 채석장 심부가 드러난다.");
            case QUARRY_BOSS -> cue(level, player, new Vec3(65, 63, 442), ParticleTypes.FLAME,
                    ChatFormatting.RED, "회수한 Relay 핵이 봉쇄 장치를 태워 없앤다. 콜바크의 심부가 열린다.");
            case CHAPTER_4_CLEAR -> cue(level, player, new Vec3(124, 66, -80), ParticleTypes.END_ROD,
                    ChatFormatting.GOLD, "거상의 열이 식어 간다. 라디아 동쪽에서 끊긴 중계 신호가 희미하게 되살아난다.");

            case RELAY_ENTRANCE -> cue(level, player, new Vec3(270, 68, -185), ParticleTypes.PORTAL,
                    ChatFormatting.LIGHT_PURPLE, "Relay 조각이 동쪽 접근로와 맞물렸다. 구 중계소의 문이 다시 열린다.");
            case RELAY_BOSS -> cue(level, player, new Vec3(417, 66, -350), ParticleTypes.SOUL,
                    ChatFormatting.DARK_PURPLE, "마지막 봉쇄 기록이 복원됐다. 관측실 너머에서 세라크의 균열이 흔들린다.");
            case RELAY_RECONNECT -> cue(level, player, new Vec3(458, 66, -350), ParticleTypes.ELECTRIC_SPARK,
                    ChatFormatting.AQUA, "세라크가 쓰러졌다. 멈춘 Relay를 되살릴 마지막 콘솔이 응답한다.");
            case ENDGAME -> cue(level, player, new Vec3(0, 66, 20), ParticleTypes.END_ROD,
                    ChatFormatting.LIGHT_PURPLE, "Aster March Relay가 다시 이어졌다. 라디아에 새로운 전투 신호들이 모여든다.");
        }
    }

    private static void cue(ServerLevel level, ServerPlayer player, Vec3 target, ParticleOptions particle,
                            ChatFormatting color, String line) {
        player.sendSystemMessage(Component.literal(line).withStyle(color, ChatFormatting.BOLD));
        Vec3 origin = player.position().add(0, 0.8, 0);
        Vec3 flat = new Vec3(target.x - origin.x, 0, target.z - origin.z);
        if (flat.lengthSqr() > 0.0001) {
            Vec3 dir = flat.normalize();
            double length = Math.min(14.0, Math.sqrt(flat.lengthSqr()));
            for (double d = 1.5; d <= length; d += 1.6) {
                Vec3 p = origin.add(dir.scale(d));
                level.sendParticles(particle, p.x, p.y + 0.15 * Math.sin(d), p.z, 2, 0.18, 0.12, 0.18, 0.01);
            }
        }
        if (player.position().distanceToSqr(target) <= 48.0 * 48.0) {
            level.sendParticles(particle, target.x, target.y + 1.0, target.z, 24, 2.4, 1.6, 2.4, 0.025);
        }
    }
}
