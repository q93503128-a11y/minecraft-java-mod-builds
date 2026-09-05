package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.presentation.PersonalPresentationIsolation;
import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Small presentation beats for meaningful P01-P08 collection/growth milestones. */
public final class AsterMarchCharacterMilestones {
    private static final List<String> CORE = List.of("P01","P02","P03","P04","P05","P06","P07","P08");
    private record State(int level, int star, boolean trial, boolean awakened) {}
    private static final Map<UUID, Map<String, State>> LAST = new ConcurrentHashMap<>();

    private AsterMarchCharacterMilestones() {}

    public static void tick(ServerLevel level, ServerPlayer player) {
        if (level == null || player == null) return;
        UUID id = player.getUUID();
        var snapshot = CampaignProgressStore.snapshot(id);
        Map<String, State> now = capture(snapshot.profile().ownedCharacters(), snapshot.characters(), snapshot.growth());
        Map<String, State> before = LAST.get(id);
        if (before == null) {
            LAST.put(id, now);
            return;
        }

        for (String characterId : CORE) {
            State current = now.get(characterId);
            if (current == null) continue;
            State previous = before.get(characterId);
            if (previous == null) {
                present(level, player, characterId, "새 캐릭터가 동료 명단에 합류했다.", 30);
                continue;
            }
            if (current.star() > previous.star()) {
                present(level, player, characterId, "성급 상승 · ★" + current.star(), current.star() >= 6 ? 38 : 24);
            }
            int levelMark = crossed(previous.level(), current.level());
            if (levelMark > 0) {
                present(level, player, characterId, "Lv" + levelMark + " 도달", levelMark >= 60 ? 32 : 18);
            }
            if (!previous.trial() && current.trial()) {
                present(level, player, characterId, "Signature Trial 완료 · 전용 장비 획득", 42);
            }
            if (!previous.awakened() && current.awakened()) {
                present(level, player, characterId, "각성 완료", 52);
            }
        }
        LAST.put(id, now);
    }

    public static void remove(ServerPlayer player) {
        if (player != null) LAST.remove(player.getUUID());
    }

    private static Map<String, State> capture(Set<String> owned,
                                               Map<String, CharacterProgression.State> characters,
                                               Map<String, CharacterGrowthRules.State> growth) {
        Map<String, State> out = new LinkedHashMap<>();
        for (String id : CORE) {
            if (!owned.contains(id)) continue;
            CharacterProgression.State level = characters.get(id);
            CharacterGrowthRules.State g = growth.get(id);
            if (level == null || g == null) continue;
            out.put(id, new State(level.level(), g.currentStar(), g.signatureTrialCleared(), g.awakened()));
        }
        return Map.copyOf(out);
    }

    private static int crossed(int before, int now) {
        for (int mark : new int[]{60, 40, 20}) if (before < mark && now >= mark) return mark;
        return 0;
    }

    private static void present(ServerLevel level, ServerPlayer player, String id, String text, int count) {
        String name = CanonicalData.definition(id).name();
        player.sendSystemMessage(Component.literal(name + " · " + text).withStyle(color(id), ChatFormatting.BOLD));
        Vec3 p = player.position().add(0, 1.05, 0);
        ParticleOptions particle = particle(id);
        PersonalPresentationIsolation.particles(level, player, particle,
                p.x, p.y, p.z, count, 1.2, 1.0, 1.2, 0.025);
        if (text.startsWith("각성")) {
            PersonalPresentationIsolation.particles(level, player, ParticleTypes.END_ROD,
                    p.x, p.y + 0.25, p.z, 20, 0.8, 1.15, 0.8, 0.02);
        }
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

    private static ChatFormatting color(String id) {
        return switch (id) {
            case "P02" -> ChatFormatting.AQUA;
            case "P03" -> ChatFormatting.GREEN;
            case "P04" -> ChatFormatting.GOLD;
            case "P05" -> ChatFormatting.YELLOW;
            case "P06" -> ChatFormatting.LIGHT_PURPLE;
            case "P07" -> ChatFormatting.BLUE;
            case "P08" -> ChatFormatting.RED;
            default -> ChatFormatting.WHITE;
        };
    }
}
