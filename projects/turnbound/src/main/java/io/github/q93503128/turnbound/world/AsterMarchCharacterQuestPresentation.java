package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import io.github.q93503128.turnbound.presentation.TurnboundBattleActors;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Character Quest presentation layered over the existing investigation-site authority.
 *
 * No quest counters or rewards live here. Unfinished field-side quests gain the actual owner actor beside the clue,
 * P07 gains Toto as a second contract participant, and newly completed quests receive a short owner-specific closure.
 * Radia-local quests intentionally keep their existing static clue actors because the hub owns its own mob cleanup.
 */
public final class AsterMarchCharacterQuestPresentation {
    private record Bundle(CharacterQuestWorldSites.Site site, UUID owner, UUID companion) {}

    private static final double ACTOR_RADIUS_SQ = 58.0 * 58.0;
    private static final double DESPAWN_RADIUS_SQ = 74.0 * 74.0;
    private static final Map<UUID, Map<String, Bundle>> BUNDLES = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> SEEN_COMPLETED = new ConcurrentHashMap<>();

    private AsterMarchCharacterQuestPresentation() {}

    public static void sync(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        UUID playerId = player.getUUID();
        Set<String> completed = CampaignProgressStore.snapshot(playerId).quests().completed();
        Set<String> seen = SEEN_COMPLETED.get(playerId);
        if (seen == null) {
            seen = new HashSet<>();
            for (CharacterQuestWorldSites.Site site : CharacterQuestWorldSites.sites()) {
                if (completed.contains(site.questId())) seen.add(site.questId());
            }
            SEEN_COMPLETED.put(playerId, seen);
        } else {
            for (CharacterQuestWorldSites.Site site : CharacterQuestWorldSites.sites()) {
                if (completed.contains(site.questId()) && seen.add(site.questId())) completionMoment(level, player, site);
            }
        }

        Map<String, Bundle> bundles = BUNDLES.computeIfAbsent(playerId, ignored -> new HashMap<>());
        for (var entry : List.copyOf(bundles.entrySet())) {
            Bundle bundle = entry.getValue();
            boolean keep = !completed.contains(bundle.site().questId())
                    && QuestMenuContentService.available(playerId, bundle.site().characterId())
                    && player.position().distanceToSqr(bundle.site().position()) <= DESPAWN_RADIUS_SQ
                    && entityAlive(level, bundle.owner());
            if (!keep) {
                despawn(level, bundle);
                bundles.remove(entry.getKey());
            }
        }

        // The four Radia investigations already sit inside facility scenes. Field sites benefit most from the real owner model.
        if (RadiaHubSessionManager.active(player)) return;
        for (CharacterQuestWorldSites.Site site : CharacterQuestWorldSites.sites()) {
            if (completed.contains(site.questId()) || !QuestMenuContentService.available(playerId, site.characterId())) continue;
            if (player.position().distanceToSqr(site.position()) > ACTOR_RADIUS_SQ) continue;
            if (bundles.containsKey(site.questId())) continue;
            Bundle bundle = spawn(level, site);
            if (bundle != null) bundles.put(site.questId(), bundle);
        }

        if (player.tickCount % 20 == 0) {
            for (Bundle bundle : bundles.values()) {
                if (player.position().distanceToSqr(bundle.site().position()) <= 18.0 * 18.0) ambient(level, bundle.site());
            }
        }
    }

    public static boolean interact(ServerPlayer player, Entity target) {
        if (player == null || target == null) return false;
        Map<String, Bundle> bundles = BUNDLES.get(player.getUUID());
        if (bundles == null) return false;
        for (Bundle bundle : bundles.values()) {
            if (target.getUUID().equals(bundle.owner())) {
                CharacterQuestWorldSites.Site site = bundle.site();
                player.sendSystemMessage(Component.literal(CanonicalData.definition(site.characterId()).name() + " · "
                        + investigationLine(site.characterId())).withStyle(site.color(), ChatFormatting.BOLD));
                player.sendSystemMessage(Component.literal("주변의 기록과 조사 지점을 확인해.").withStyle(ChatFormatting.GRAY));
                if (player.level() instanceof ServerLevel level) focusClue(level, site);
                return true;
            }
            if (bundle.companion() != null && target.getUUID().equals(bundle.companion())) {
                player.sendSystemMessage(Component.literal("토토가 오래된 계약 제단 쪽을 바라본다.")
                        .withStyle(ChatFormatting.BLUE));
                if (player.level() instanceof ServerLevel level) focusClue(level, bundle.site());
                return true;
            }
        }
        return false;
    }

    /** Despawns only visual companions during combat while preserving completion baselines for the current session. */
    public static void cancelForBattle(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        Map<String, Bundle> bundles = BUNDLES.remove(player.getUUID());
        if (bundles != null) for (Bundle bundle : bundles.values()) despawn(level, bundle);
    }

    public static void remove(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        cancelForBattle(level, player);
        SEEN_COMPLETED.remove(player.getUUID());
    }

    private static Bundle spawn(ServerLevel level, CharacterQuestWorldSites.Site site) {
        Vec3 clue = site.position();
        Vec3 ownerPos = clue.add(ownerOffset(site.characterId()));
        BattleActorEntity owner = TurnboundBattleActors.spawn(level, site.characterId(), ownerPos, yawToward(ownerPos, clue));
        if (owner == null) return null;
        owner.setFieldWalking(false);
        owner.setCustomName(Component.literal(CanonicalData.definition(site.characterId()).name() + " · 조사 중")
                .withStyle(site.color(), ChatFormatting.BOLD));
        owner.setCustomNameVisible(true);

        UUID companion = null;
        if ("P07".equals(site.characterId())) {
            Vec3 totoPos = clue.add(-2.0, 0.0, -1.2);
            BattleActorEntity toto = TurnboundBattleActors.spawn(level, "P07_SUMMON", totoPos, yawToward(totoPos, clue));
            if (toto != null) {
                toto.setFieldWalking(false);
                toto.setCustomName(Component.literal("토토").withStyle(ChatFormatting.BLUE));
                toto.setCustomNameVisible(true);
                companion = toto.getUUID();
            }
        }
        focusClue(level, site);
        return new Bundle(site, owner.getUUID(), companion);
    }

    private static Vec3 ownerOffset(String id) {
        return switch (id) {
            case "P01", "P05" -> new Vec3(2.2, 0, 1.2);
            case "P07" -> new Vec3(2.0, 0, -1.0);
            case "P08" -> new Vec3(-2.2, 0, 1.0);
            default -> new Vec3(2.0, 0, 1.0);
        };
    }

    private static void ambient(ServerLevel level, CharacterQuestWorldSites.Site site) {
        ParticleOptions primary = particle(site.characterId());
        Vec3 p = site.position().add(0, 1.1, 0);
        level.sendParticles(primary, p.x, p.y, p.z, 4, 1.2, 0.7, 1.2, 0.01);
        if ("P07".equals(site.characterId())) {
            level.sendParticles(ParticleTypes.ENCHANT, p.x, p.y + 0.2, p.z, 3, 0.8, 0.45, 0.8, 0.01);
        }
    }

    private static void focusClue(ServerLevel level, CharacterQuestWorldSites.Site site) {
        Vec3 p = site.position().add(0, 1.0, 0);
        level.sendParticles(particle(site.characterId()), p.x, p.y, p.z, 18, 1.1, 0.9, 1.1, 0.02);
    }

    private static void completionMoment(ServerLevel level, ServerPlayer player, CharacterQuestWorldSites.Site site) {
        String name = CanonicalData.definition(site.characterId()).name();
        player.sendSystemMessage(Component.literal(name + " · " + completionLine(site.characterId()))
                .withStyle(site.color(), ChatFormatting.BOLD));
        Vec3 p = player.position().add(0, 1.0, 0);
        level.sendParticles(particle(site.characterId()), p.x, p.y, p.z, 24, 1.3, 1.0, 1.3, 0.025);
    }

    private static String investigationLine(String id) {
        return switch (id) {
            case "P01" -> "마지막 장부터 보자. 남은 사람이 누구였는지 기록은 거짓말하지 않아.";
            case "P02" -> "멈춘 시각과 지금 Relay 박동을 겹쳐 보면 원인이 보여.";
            case "P03" -> "대피 명단을 확인하자. 성문이 무너진 순서를 알아야 해.";
            case "P04" -> "이름이 빠진 사람부터 다시 맞춰 보자.";
            case "P05" -> "표식 하나가 잘못되면 사람의 길도 바뀌어. 원래 경로를 찾자.";
            case "P06" -> "이름이 지워진 기록도 끝까지 남겨 두면 다시 읽을 수 있어.";
            case "P07" -> "계약문을 직접 보자. 토토가 무엇으로 기록돼 있는지가 중요해.";
            case "P08" -> "작업기록부터 봐. 그날 내가 어느 통로로 갔는지 남아 있을 거야.";
            default -> "기록을 직접 확인하자.";
        };
    }

    private static String completionLine(String id) {
        return switch (id) {
            case "P01" -> "끝까지 남았던 이유가 기록에 남아 있었네.";
            case "P02" -> "시간이 멈춘 게 아니라 같은 순간을 되짚고 있었어.";
            case "P03" -> "이번엔 누가 마지막에 남았는지 분명해졌어.";
            case "P04" -> "돌아온 사람들의 이름을 다시 놓치지 않을게.";
            case "P05" -> "잘못된 표식의 시작점은 찾았어. 같은 실수는 반복하지 않아.";
            case "P06" -> "이제 이 기록은 무명이 아니야.";
            case "P07" -> "토토는 처음부터 계약의 한쪽이었어.";
            case "P08" -> "도망친 길이 아니라 사람을 데리러 간 길이었어. 그걸로 됐어.";
            default -> "기록이 정리됐어.";
        };
    }

    private static ParticleOptions particle(String id) {
        return switch (id) {
            case "P01" -> ParticleTypes.CRIT;
            case "P02" -> ParticleTypes.ENCHANT;
            case "P03" -> ParticleTypes.CLOUD;
            case "P04" -> ParticleTypes.END_ROD;
            case "P05" -> ParticleTypes.ELECTRIC_SPARK;
            case "P06" -> ParticleTypes.SOUL;
            case "P07" -> ParticleTypes.ENCHANT;
            case "P08" -> ParticleTypes.SMALL_FLAME;
            default -> ParticleTypes.END_ROD;
        };
    }

    private static boolean entityAlive(ServerLevel level, UUID id) {
        return id != null && level.getEntity(id) != null;
    }

    private static void despawn(ServerLevel level, Bundle bundle) {
        if (bundle == null) return;
        Entity owner = bundle.owner() == null ? null : level.getEntity(bundle.owner());
        if (owner != null) owner.discard();
        Entity companion = bundle.companion() == null ? null : level.getEntity(bundle.companion());
        if (companion != null) companion.discard();
    }

    private static float yawToward(Vec3 from, Vec3 to) {
        return (float)Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
    }
}
