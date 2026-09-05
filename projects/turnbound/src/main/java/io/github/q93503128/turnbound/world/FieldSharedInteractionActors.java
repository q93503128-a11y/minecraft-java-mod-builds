package io.github.q93503128.turnbound.world;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * World-shared physical interaction actors for authored field chapters.
 *
 * These actors are presentation/interaction anchors only. Player quest state remains in CampaignProgressStore,
 * so clicking the same shared object can advance different players independently without duplicating entities.
 */
public final class FieldSharedInteractionActors {
    static final String COMMON_TAG = "turnbound_field_shared_actor";
    private static final String ROLE_PREFIX = "turnbound_field_role:";
    private static final AABB WORLD_AREA = new AABB(-520, 42, -520, 520, 112, 520);
    private static final Set<String> LEGACY_NAMES = Set.of(
            "남문 정찰관", "남문 마을 계전석", "남문 초원 계전소 · FT_MEADOW",
            "그늘숲 계전소 · FT_GLOAM", "포자등불 조사 1/3", "포자등불 조사 2/3", "포자등불 조사 3/3",
            "붕괴 수로 계전소 · FT_AQUEDUCT", "수로 압력 밸브 1/2", "수로 압력 밸브 2/2",
            "잿불 채석장 계전소 · FT_QUARRY", "Relay 핵 파편 1/2", "Relay 핵 파편 2/2",
            "구 중계소 계전소 · FT_RELAY", "세라크 기록 1/4", "세라크 기록 2/4", "세라크 기록 3/4", "세라크 기록 4/4",
            "Relay 재연결 콘솔");

    public enum Role {
        SOUTHGATE_SCOUT, SOUTHGATE_RELAY_VILLAGE, SOUTHGATE_RELAY_MEADOW,
        GLOAM_RELAY, GLOAM_SPORE_1, GLOAM_SPORE_2, GLOAM_SPORE_3,
        AQUEDUCT_RELAY, AQUEDUCT_VALVE_1, AQUEDUCT_VALVE_2,
        QUARRY_RELAY, QUARRY_CORE_1, QUARRY_CORE_2,
        OLD_RELAY_FT, OLD_RELAY_RECORD_1, OLD_RELAY_RECORD_2, OLD_RELAY_RECORD_3, OLD_RELAY_RECORD_4,
        OLD_RELAY_FINAL_CONSOLE
    }

    private record Spec(Role role, Vec3 pos, String label, Item item, ChatFormatting color, Item head) {}

    private FieldSharedInteractionActors() {}

    public static Role role(Entity entity) {
        if (!(entity instanceof ArmorStand)) return null;
        for (String tag : entity.entityTags()) {
            if (!tag.startsWith(ROLE_PREFIX)) continue;
            try { return Role.valueOf(tag.substring(ROLE_PREFIX.length())); }
            catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }

    static String roleTag(Role role) { return ROLE_PREFIX + role.name(); }

    public static int gloamSporeIndex(Role role) {
        return switch (role) {
            case GLOAM_SPORE_1 -> 0;
            case GLOAM_SPORE_2 -> 1;
            case GLOAM_SPORE_3 -> 2;
            default -> -1;
        };
    }

    public static int aqueductValveIndex(Role role) {
        return switch (role) {
            case AQUEDUCT_VALVE_1 -> 0;
            case AQUEDUCT_VALVE_2 -> 1;
            default -> -1;
        };
    }

    public static int quarryCoreIndex(Role role) {
        return switch (role) {
            case QUARRY_CORE_1 -> 0;
            case QUARRY_CORE_2 -> 1;
            default -> -1;
        };
    }

    public static int oldRelayRecordIndex(Role role) {
        return switch (role) {
            case OLD_RELAY_RECORD_1 -> 0;
            case OLD_RELAY_RECORD_2 -> 1;
            case OLD_RELAY_RECORD_3 -> 2;
            case OLD_RELAY_RECORD_4 -> 3;
            default -> -1;
        };
    }

    public static void ensureSouthgate(ServerLevel level, StarterSliceWorld.BuiltSlice slice, SouthgateChapterWorld.BuiltChapter chapter) {
        removeLegacyActors(level);
        ensure(level, new Spec(Role.SOUTHGATE_SCOUT, slice.npc(), "남문 정찰관", Items.SPYGLASS, ChatFormatting.AQUA, Items.LEATHER_HELMET));
        ensure(level, new Spec(Role.SOUTHGATE_RELAY_VILLAGE, slice.relay(), "남문 마을 계전석", Items.COMPASS, ChatFormatting.LIGHT_PURPLE, Items.AMETHYST_SHARD));
        ensure(level, new Spec(Role.SOUTHGATE_RELAY_MEADOW, chapter.meadowRelay(), "남문 초원 계전소", Items.COMPASS, ChatFormatting.LIGHT_PURPLE, Items.AMETHYST_SHARD));
    }

    public static void ensureGloamwood(ServerLevel level, GloamwoodChapterWorld.BuiltChapter chapter) {
        removeLegacyActors(level);
        ensure(level, new Spec(Role.GLOAM_RELAY, chapter.fastTravel(), "그늘숲 계전소", Items.AMETHYST_SHARD, ChatFormatting.LIGHT_PURPLE, null));
        for (int i = 0; i < Math.min(3, chapter.sporeLanterns().size()); i++) {
            ensure(level, new Spec(switch (i) { case 0 -> Role.GLOAM_SPORE_1; case 1 -> Role.GLOAM_SPORE_2; default -> Role.GLOAM_SPORE_3; },
                    chapter.sporeLanterns().get(i), "포자등불 " + (i + 1), Items.GLOW_BERRIES, ChatFormatting.GREEN, null));
        }
    }

    public static void ensureAqueduct(ServerLevel level, BrokenAqueductChapterWorld.BuiltChapter chapter) {
        removeLegacyActors(level);
        ensure(level, new Spec(Role.AQUEDUCT_RELAY, chapter.fastTravel(), "붕괴 수로 계전소", Items.AMETHYST_SHARD, ChatFormatting.LIGHT_PURPLE, null));
        for (int i = 0; i < Math.min(2, chapter.valves().size()); i++) {
            ensure(level, new Spec(i == 0 ? Role.AQUEDUCT_VALVE_1 : Role.AQUEDUCT_VALVE_2,
                    chapter.valves().get(i), "수로 압력 밸브 " + (i + 1), Items.LEVER, ChatFormatting.AQUA, null));
        }
    }

    public static void ensureQuarry(ServerLevel level, EmberQuarryChapterWorld.BuiltChapter chapter, int availableCores) {
        removeLegacyActors(level);
        ensure(level, new Spec(Role.QUARRY_RELAY, chapter.fastTravel(), "잿불 채석장 계전소", Items.AMETHYST_SHARD, ChatFormatting.LIGHT_PURPLE, null));
        for (int i = 0; i < Math.min(Math.min(2, availableCores), chapter.corePickupPositions().size()); i++) {
            ensure(level, new Spec(i == 0 ? Role.QUARRY_CORE_1 : Role.QUARRY_CORE_2,
                    chapter.corePickupPositions().get(i), "Relay 핵 파편 " + (i + 1), Items.BLAZE_POWDER, ChatFormatting.GOLD, null));
        }
    }

    public static void ensureOldRelay(ServerLevel level, OldRelayStationWorld.BuiltChapter chapter, boolean finalConsoleAvailable) {
        removeLegacyActors(level);
        ensure(level, new Spec(Role.OLD_RELAY_FT, chapter.fastTravel(), "구 중계소 계전소", Items.AMETHYST_SHARD, ChatFormatting.LIGHT_PURPLE, null));
        for (int i = 0; i < Math.min(4, chapter.recordConsoles().size()); i++) {
            Role role = switch (i) {
                case 0 -> Role.OLD_RELAY_RECORD_1;
                case 1 -> Role.OLD_RELAY_RECORD_2;
                case 2 -> Role.OLD_RELAY_RECORD_3;
                default -> Role.OLD_RELAY_RECORD_4;
            };
            ensure(level, new Spec(role, chapter.recordConsoles().get(i), "세라크 기록 " + (i + 1), Items.WRITABLE_BOOK, ChatFormatting.AQUA, null));
        }
        if (finalConsoleAvailable) {
            ensure(level, new Spec(Role.OLD_RELAY_FINAL_CONSOLE, chapter.relayConsole(), "Relay 재연결 콘솔", Items.COMPARATOR, ChatFormatting.LIGHT_PURPLE, null));
        }
    }

    private static void ensure(ServerLevel level, Spec spec) {
        ArmorStand found = null;
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, WORLD_AREA)) {
            Role role = role(stand);
            if (role != spec.role()) continue;
            if (found == null) found = stand;
            else stand.discard();
        }
        if (found == null || found.isRemoved()) {
            found = new ArmorStand(level, spec.pos().x, spec.pos().y, spec.pos().z);
            level.addFreshEntity(found);
        }
        configure(found, spec);
    }

    private static void configure(ArmorStand stand, Spec spec) {
        stand.setPos(spec.pos().x, spec.pos().y, spec.pos().z);
        stand.setDeltaMovement(Vec3.ZERO);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setShowArms(true);
        stand.setCustomName(Component.literal(spec.label()).withStyle(spec.color()));
        stand.setCustomNameVisible(true);
        stand.setItemSlot(EquipmentSlot.MAINHAND, spec.item().getDefaultInstance());
        if (spec.head() != null) stand.setItemSlot(EquipmentSlot.HEAD, spec.head().getDefaultInstance());
        stand.addTag(COMMON_TAG);
        stand.addTag(roleTag(spec.role()));
    }

    private static void removeLegacyActors(ServerLevel level) {
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, WORLD_AREA)) {
            if (role(stand) != null) continue;
            Component name = stand.getCustomName();
            if (name != null && LEGACY_NAMES.contains(name.getString())) stand.discard();
        }
    }

    /** Removes duplicate canonical actors without touching any unrelated ArmorStand. */
    public static void dedupe(ServerLevel level) {
        Map<Role, ArmorStand> first = new EnumMap<>(Role.class);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, WORLD_AREA)) {
            Role role = role(stand);
            if (role == null) continue;
            if (first.putIfAbsent(role, stand) != null) stand.discard();
        }
    }
}
