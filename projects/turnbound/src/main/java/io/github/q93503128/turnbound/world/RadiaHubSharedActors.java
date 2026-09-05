package io.github.q93503128.turnbound.world;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns the physical Radia interaction actors as world-shared entities.
 *
 * Player sessions never own or despawn these ArmorStands. Identity lives on persistent entity command tags, so
 * a second player entering Radia reuses the same actors and a server restart can recover the persisted entities.
 */
public final class RadiaHubSharedActors {
    private static final AABB HUB_ACTOR_AREA = new AABB(-132, 54, -116, 132, 100, 132);

    private static final Set<String> LEGACY_NAMES = Set.of(
            "Director Iven",
            "라디아 계전소",
            "라디아 계전소 · FT_RADIA",
            "South Gate",
            "전투 훈련 1",
            "전투 훈련 2",
            "전투 훈련 3",
            "Echo Archive · 소환",
            "Forge Annex · 장비 강화",
            "Market Row · 장비 상점",
            "Training Yard · 자유 훈련",
            "Rift Gate · 균열 관문",
            "Rift Gate · Endgame",
            "Memorial Steps · 인연 기록",
            "Memorial Steps · Character Quest",
            "Clock Tower · 멈춘 시계탑",
            "Clock Tower · CQ_P02",
            "Barracks · 수비대 기록");

    private record Spec(RadiaHubActorCatalog.Role role, Vec3 authored, String name, Item item, ChatFormatting color) {}

    private RadiaHubSharedActors() {}

    public static void ensure(ServerLevel level, RadiaHubWorld.BuiltHub hub) {
        if (level == null || hub == null) return;

        Map<RadiaHubActorCatalog.Role, ArmorStand> existing = new EnumMap<>(RadiaHubActorCatalog.Role.class);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, HUB_ACTOR_AREA)) {
            RadiaHubActorCatalog.Role role = role(stand);
            if (role != null) {
                ArmorStand first = existing.putIfAbsent(role, stand);
                if (first != null) stand.discard();
                continue;
            }
            if (stand.entityTags().contains(RadiaHubActorCatalog.COMMON_TAG) || legacyActor(stand)) {
                stand.discard();
            }
        }

        for (Spec spec : specs(hub)) {
            ArmorStand stand = existing.get(spec.role());
            if (stand == null || stand.isRemoved()) {
                spawn(level, spec);
            } else {
                configure(level, stand, spec);
            }
        }
    }

    public static RadiaHubActorCatalog.Role role(Entity entity) {
        if (!(entity instanceof ArmorStand)) return null;
        for (String tag : entity.entityTags()) {
            RadiaHubActorCatalog.Role role = RadiaHubActorCatalog.fromTag(tag);
            if (role != null) return role;
        }
        return null;
    }

    private static ArmorStand spawn(ServerLevel level, Spec spec) {
        Vec3 pos = safeActorPosition(level, spec.authored());
        ArmorStand stand = new ArmorStand(level, pos.x, pos.y, pos.z);
        configure(level, stand, spec);
        level.addFreshEntity(stand);
        return stand;
    }

    private static void configure(ServerLevel level, ArmorStand stand, Spec spec) {
        Vec3 pos = safeActorPosition(level, spec.authored());
        stand.setPos(pos.x, pos.y, pos.z);
        stand.setDeltaMovement(Vec3.ZERO);
        stand.setInvulnerable(true);
        stand.setNoGravity(true);
        stand.setShowArms(true);
        stand.setCustomName(Component.literal(spec.name()).withStyle(spec.color()));
        stand.setCustomNameVisible(true);
        stand.setItemSlot(EquipmentSlot.MAINHAND, spec.item().getDefaultInstance());
        stand.addTag(RadiaHubActorCatalog.COMMON_TAG);
        stand.addTag(RadiaHubActorCatalog.roleTag(spec.role()));
    }

    private static List<Spec> specs(RadiaHubWorld.BuiltHub hub) {
        List<Spec> specs = new ArrayList<>();
        specs.add(new Spec(RadiaHubActorCatalog.Role.DIRECTOR,
                new Vec3(0.5, hub.director().y, 6.5), "Director Iven", Items.SPYGLASS, ChatFormatting.GOLD));
        specs.add(new Spec(RadiaHubActorCatalog.Role.RELAY,
                hub.relay(), "라디아 계전소", Items.AMETHYST_SHARD, ChatFormatting.LIGHT_PURPLE));
        specs.add(new Spec(RadiaHubActorCatalog.Role.SOUTH_GATE,
                hub.southGate(), "South Gate", Items.IRON_SWORD, ChatFormatting.GREEN));

        for (int i = 0; i < hub.tutorialPedestals().size() && i < 3; i++) {
            RadiaHubActorCatalog.Role role = switch (i) {
                case 0 -> RadiaHubActorCatalog.Role.TUTORIAL_1;
                case 1 -> RadiaHubActorCatalog.Role.TUTORIAL_2;
                default -> RadiaHubActorCatalog.Role.TUTORIAL_3;
            };
            specs.add(new Spec(role, hub.tutorialPedestals().get(i), "전투 훈련 " + (i + 1),
                    i == 2 ? Items.TNT : Items.IRON_SWORD, ChatFormatting.YELLOW));
        }

        for (RadiaHubWorld.Facility facility : hub.facilities()) {
            RadiaHubActorCatalog.Role role = RadiaHubActorCatalog.facilityRole(facility.id());
            if (role == null) continue;
            specs.add(new Spec(role, facility.position(), label(role), item(role), color(role)));
        }
        return specs;
    }

    private static boolean legacyActor(ArmorStand stand) {
        Component name = stand.getCustomName();
        return name != null && LEGACY_NAMES.contains(name.getString());
    }

    private static Vec3 safeActorPosition(ServerLevel level, Vec3 authored) {
        BlockPos origin = BlockPos.containing(authored.x, authored.y, authored.z);
        int[][] offsets = {
                {0, 0}, {0, 1}, {1, 0}, {0, -1}, {-1, 0},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1},
                {0, 2}, {2, 0}, {0, -2}, {-2, 0},
                {0, 3}, {3, 0}, {0, -3}, {-3, 0}
        };
        int[] vertical = {0, 1, -1, 2, -2};
        for (int dy : vertical) {
            for (int[] offset : offsets) {
                BlockPos p = origin.offset(offset[0], dy, offset[1]);
                var feet = level.getBlockState(p);
                var head = level.getBlockState(p.above());
                var floor = level.getBlockState(p.below());
                if (feet.isAir() && head.isAir() && !floor.isAir() && floor.getFluidState().isEmpty()) {
                    return Vec3.atBottomCenterOf(p);
                }
            }
        }
        return authored;
    }

    private static String label(RadiaHubActorCatalog.Role role) {
        return switch (role) {
            case ECHO_ARCHIVE -> "Echo Archive · 소환";
            case FORGE_ANNEX -> "Forge Annex · 장비 강화";
            case MARKET_ROW -> "Market Row · 장비 상점";
            case TRAINING_YARD -> "Training Yard · 자유 훈련";
            case RIFT_GATE -> "Rift Gate · 균열 관문";
            case MEMORIAL_STEPS -> "Memorial Steps · 인연 기록";
            case CLOCK_TOWER -> "Clock Tower · 멈춘 시계탑";
            case BARRACKS -> "Barracks · 수비대 기록";
            default -> role.name();
        };
    }

    private static Item item(RadiaHubActorCatalog.Role role) {
        return switch (role) {
            case ECHO_ARCHIVE -> Items.BOOK;
            case FORGE_ANNEX -> Items.IRON_INGOT;
            case MARKET_ROW -> Items.EMERALD;
            case TRAINING_YARD -> Items.IRON_SWORD;
            case RIFT_GATE -> Items.ENDER_PEARL;
            case MEMORIAL_STEPS -> Items.PAPER;
            case CLOCK_TOWER -> Items.CLOCK;
            case BARRACKS -> Items.SHIELD;
            default -> Items.COMPASS;
        };
    }

    private static ChatFormatting color(RadiaHubActorCatalog.Role role) {
        return switch (role) {
            case ECHO_ARCHIVE, RIFT_GATE, MEMORIAL_STEPS -> ChatFormatting.LIGHT_PURPLE;
            case FORGE_ANNEX, MARKET_ROW -> ChatFormatting.GOLD;
            case TRAINING_YARD, BARRACKS -> ChatFormatting.GREEN;
            case CLOCK_TOWER -> ChatFormatting.AQUA;
            default -> ChatFormatting.WHITE;
        };
    }
}
