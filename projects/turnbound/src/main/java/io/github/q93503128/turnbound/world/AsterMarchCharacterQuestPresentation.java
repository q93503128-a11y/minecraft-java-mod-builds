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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Character Quest presentation layered over the existing investigation-site authority.
 *
 * No quest counters or rewards live here. Field-side quest owner models are shared world presentation while each
 * player's availability, completion baseline, dialogue and closure remain personal. This prevents two players from
 * producing stacked copies of the same hero/Toto at one authored investigation site.
 */
public final class AsterMarchCharacterQuestPresentation {
    private record Bundle(CharacterQuestWorldSites.Site site, UUID owner, UUID companion) {}

    private static final double ACTOR_RADIUS_SQ = 58.0 * 58.0;
    private static final String COMMON_TAG = "turnbound_cq_shared_actor";
    private static final String QUEST_TAG_PREFIX = "turnbound_cq_quest:";
    private static final String OWNER_TAG = "turnbound_cq_owner";
    private static final String COMPANION_TAG = "turnbound_cq_companion";

    /** quest id -> one shared physical owner/companion bundle. */
    private static final Map<String, Bundle> BUNDLES = new ConcurrentHashMap<>();
    /** player -> quest ids whose shared field actors this player currently needs. */
    private static final Map<UUID, Set<String>> OBSERVATIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> SEEN_COMPLETED = new ConcurrentHashMap<>();

    private AsterMarchCharacterQuestPresentation() {}

    public static void sync(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        UUID playerId = player.getUUID();
        Set<String> completed = CampaignProgressStore.snapshot(playerId).quests().completed();
        seedCompletionMoments(level, player, completed);

        Set<String> desired = new HashSet<>();
        // The four Radia investigations already sit inside facility scenes. Field sites benefit most from the real owner model.
        if (!RadiaHubSessionManager.active(player)) {
            for (CharacterQuestWorldSites.Site site : CharacterQuestWorldSites.sites()) {
                if (completed.contains(site.questId()) || !QuestMenuContentService.available(playerId, site.characterId())) continue;
                if (player.position().distanceToSqr(site.position()) > ACTOR_RADIUS_SQ) continue;
                desired.add(site.questId());
                ensureShared(level, site);
                if (player.tickCount % 20 == 0 && player.position().distanceToSqr(site.position()) <= 18.0 * 18.0) {
                    ambient(level, site);
                }
            }
        }

        Set<String> previous = OBSERVATIONS.put(playerId, Set.copyOf(desired));
        if (previous != null) {
            for (String questId : previous) if (!desired.contains(questId)) discardIfUnobserved(level, questId);
        }
    }

    public static boolean interact(ServerPlayer player, Entity target) {
        if (player == null || target == null || !(player.level() instanceof ServerLevel level)) return false;
        Bundle bundle = bundleForEntity(target.getUUID());
        if (bundle == null) return false;
        CharacterQuestWorldSites.Site site = bundle.site();
        UUID playerId = player.getUUID();

        if (!QuestMenuContentService.available(playerId, site.characterId())) {
            player.sendSystemMessage(Component.literal("아직 이 인연 기록을 조사할 수 없다.").withStyle(ChatFormatting.GRAY));
            return true;
        }
        if (CampaignProgressStore.snapshot(playerId).quests().completed().contains(site.questId())) {
            player.sendSystemMessage(Component.literal("이미 정리한 인연 기록이다.").withStyle(ChatFormatting.GRAY));
            return true;
        }

        if (target.getUUID().equals(bundle.owner())) {
            player.sendSystemMessage(Component.literal(CanonicalData.definition(site.characterId()).name() + " · "
                    + investigationLine(site.characterId())).withStyle(site.color(), ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("주변의 기록과 조사 지점을 확인해.").withStyle(ChatFormatting.GRAY));
            focusClue(level, site);
            return true;
        }
        if (bundle.companion() != null && target.getUUID().equals(bundle.companion())) {
            player.sendSystemMessage(Component.literal("토토가 오래된 계약 제단 쪽을 바라본다.")
                    .withStyle(ChatFormatting.BLUE));
            focusClue(level, site);
            return true;
        }
        return false;
    }

    /** Battle drops only this player's observation; another nearby player keeps the shared actor alive. */
    public static void cancelForBattle(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        removeObservation(level, player.getUUID());
    }

    public static void remove(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        removeObservation(level, player.getUUID());
        SEEN_COMPLETED.remove(player.getUUID());
    }

    private static void seedCompletionMoments(ServerLevel level, ServerPlayer player, Set<String> completed) {
        UUID playerId = player.getUUID();
        Set<String> seen = SEEN_COMPLETED.get(playerId);
        if (seen == null) {
            seen = new HashSet<>();
            for (CharacterQuestWorldSites.Site site : CharacterQuestWorldSites.sites()) {
                if (completed.contains(site.questId())) seen.add(site.questId());
            }
            SEEN_COMPLETED.put(playerId, seen);
            return;
        }
        for (CharacterQuestWorldSites.Site site : CharacterQuestWorldSites.sites()) {
            if (completed.contains(site.questId()) && seen.add(site.questId())) completionMoment(level, player, site);
        }
    }

    private static void ensureShared(ServerLevel level, CharacterQuestWorldSites.Site site) {
        Bundle current = BUNDLES.get(site.questId());
        BattleActorEntity owner = current == null ? null : actor(level, current.owner());
        BattleActorEntity companion = current == null ? null : actor(level, current.companion());
        if (owner == null) owner = recover(level, site, OWNER_TAG);
        if ("P07".equals(site.characterId()) && companion == null) companion = recover(level, site, COMPANION_TAG);

        removeLegacyActors(level, site, owner, companion);
        Vec3 clue = site.position();
        Vec3 ownerPos = clue.add(ownerOffset(site.characterId()));
        if (owner == null) owner = TurnboundBattleActors.spawn(level, site.characterId(), ownerPos, yawToward(ownerPos, clue));
        if (owner == null) return;
        configure(owner, site, OWNER_TAG, ownerPos, clue,
                CanonicalData.definition(site.characterId()).name() + " · 조사 중", site.color());

        UUID companionId = null;
        if ("P07".equals(site.characterId())) {
            Vec3 totoPos = clue.add(-2.0, 0.0, -1.2);
            if (companion == null) companion = TurnboundBattleActors.spawn(level, "P07_SUMMON", totoPos, yawToward(totoPos, clue));
            if (companion != null) {
                configure(companion, site, COMPANION_TAG, totoPos, clue, "토토", ChatFormatting.BLUE);
                companionId = companion.getUUID();
            }
        } else if (companion != null) {
            companion.discard();
        }
        BUNDLES.put(site.questId(), new Bundle(site, owner.getUUID(), companionId));
    }

    private static void configure(BattleActorEntity actor, CharacterQuestWorldSites.Site site, String roleTag,
                                  Vec3 pos, Vec3 clue, String label, ChatFormatting color) {
        float yaw = yawToward(pos, clue);
        actor.setPos(pos.x, pos.y, pos.z);
        actor.setDeltaMovement(Vec3.ZERO);
        actor.setYRot(yaw);
        actor.setYHeadRot(yaw);
        actor.setYBodyRot(yaw);
        actor.setFieldWalking(false);
        actor.setCustomName(Component.literal(label).withStyle(color, ChatFormatting.BOLD));
        actor.setCustomNameVisible(true);
        actor.addTag(COMMON_TAG);
        actor.addTag(QUEST_TAG_PREFIX + site.questId());
        actor.addTag(roleTag);
    }

    private static BattleActorEntity recover(ServerLevel level, CharacterQuestWorldSites.Site site, String roleTag) {
        Vec3 clue = site.position();
        AABB area = new AABB(clue.x - 5.5, clue.y - 2.0, clue.z - 5.5, clue.x + 5.5, clue.y + 4.0, clue.z + 5.5);
        BattleActorEntity first = null;
        String questTag = QUEST_TAG_PREFIX + site.questId();
        for (BattleActorEntity actor : level.getEntitiesOfClass(BattleActorEntity.class, area)) {
            if (!actor.entityTags().contains(COMMON_TAG)
                    || !actor.entityTags().contains(questTag)
                    || !actor.entityTags().contains(roleTag)) continue;
            if (first == null) first = actor;
            else actor.discard();
        }
        return first;
    }

    private static void removeLegacyActors(ServerLevel level, CharacterQuestWorldSites.Site site,
                                           BattleActorEntity canonicalOwner, BattleActorEntity canonicalCompanion) {
        Vec3 clue = site.position();
        AABB area = new AABB(clue.x - 5.5, clue.y - 2.0, clue.z - 5.5, clue.x + 5.5, clue.y + 4.0, clue.z + 5.5);
        String ownerName = CanonicalData.definition(site.characterId()).name() + " · 조사 중";
        for (BattleActorEntity actor : level.getEntitiesOfClass(BattleActorEntity.class, area)) {
            if (actor == canonicalOwner || actor == canonicalCompanion || actor.entityTags().contains(COMMON_TAG)) continue;
            Component name = actor.getCustomName();
            if (name == null) continue;
            String text = name.getString();
            if (ownerName.equals(text) || ("P07".equals(site.characterId()) && "토토".equals(text))) actor.discard();
        }
    }

    private static Bundle bundleForEntity(UUID entityId) {
        if (entityId == null) return null;
        for (Bundle bundle : BUNDLES.values()) {
            if (entityId.equals(bundle.owner()) || entityId.equals(bundle.companion())) return bundle;
        }
        return null;
    }

    private static void removeObservation(ServerLevel level, UUID playerId) {
        Set<String> removed = OBSERVATIONS.remove(playerId);
        if (removed == null) return;
        for (String questId : removed) discardIfUnobserved(level, questId);
    }

    private static void discardIfUnobserved(ServerLevel level, String questId) {
        for (Set<String> observed : OBSERVATIONS.values()) if (observed.contains(questId)) return;
        Bundle bundle = BUNDLES.remove(questId);
        if (bundle == null) return;
        Entity owner = level.getEntity(bundle.owner());
        if (owner != null) owner.discard();
        Entity companion = bundle.companion() == null ? null : level.getEntity(bundle.companion());
        if (companion != null) companion.discard();
    }

    private static BattleActorEntity actor(ServerLevel level, UUID id) {
        if (id == null) return null;
        Entity entity = level.getEntity(id);
        return entity instanceof BattleActorEntity actor && !actor.isRemoved() ? actor : null;
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

    private static float yawToward(Vec3 from, Vec3 to) {
        return (float)Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
    }
}
