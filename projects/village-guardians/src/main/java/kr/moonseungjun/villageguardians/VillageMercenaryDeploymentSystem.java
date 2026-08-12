package kr.moonseungjun.villageguardians;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Locale;

/** Lightweight pre-battle rally doctrine: choose a class rally point, then mercenaries auto-fight around it. */
public final class VillageMercenaryDeploymentSystem {
    private static final String SEP = "\u001F";
    private static int ticks;
    private VillageMercenaryDeploymentSystem() {}

    public static void reset() { ticks = 0; }

    public static void openCommand(ServerPlayer player) {
        if (!VillageLocationRules.isNear(player, VillageProgressionSystem.Building.BARRACKS)
                && !VillageLocationRules.isNearTownHall(player)) {
            player.sendSystemMessage(Component.literal("§c용병 지휘는 병영 또는 마을 회관에서만 가능합니다."));
            return;
        }
        java.util.ArrayList<String> actions = new java.util.ArrayList<>();
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        for (VillageMercenarySystem.MercenaryClass kind : VillageMercenarySystem.MercenaryClass.values()) {
            actions.add("merc_class:" + kind.id());
            labels.add(kind.displayName() + " · " + deployment(kind).displayName()
                    + "|" + kind.description() + " · 고용비 " + VillageMercenarySystem.hireCost(kind));
        }
        send(player, "management", "용병 배치 지휘", VillageMercenarySystem.status(player.level().getServer())
                + "\n전투 전 병과별 거점만 지정합니다. 전투 중에는 각 병과 AI가 자동으로 우선 목표를 처리합니다.", actions, labels);
    }

    public static void openClass(ServerPlayer player, VillageMercenarySystem.MercenaryClass kind) {
        if (kind == null) { openCommand(player); return; }
        java.util.ArrayList<String> actions = new java.util.ArrayList<>();
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        actions.add("merc_hire:" + kind.id());
        labels.add("새 " + kind.displayName() + " 고용|주화 " + VillageMercenarySystem.hireCost(kind) + " · 정원 내에서 지속 성장");
        for (Deployment zone : Deployment.values()) {
            if (!allowed(kind, zone)) continue;
            actions.add("merc_deploy:" + kind.id() + ":" + zone.id());
            labels.add(zone.displayName() + " 배치|" + zone.description() + (deployment(kind) == zone ? " · 현재 선택" : ""));
        }
        actions.add("open_mercenary_command"); labels.add("용병 지휘 목록|다른 병과 관리");
        send(player, "management", kind.displayName(), kind.description() + "\n현재 배치: " + deployment(kind).displayName()
                + "\n전투 시작 후 세밀한 RTS 조작 없이 자동 전투하며, 배치 거점에서 지나치게 이탈하면 복귀합니다.", actions, labels);
    }

    public static String setDeployment(ServerPlayer player, VillageMercenarySystem.MercenaryClass kind, Deployment zone) {
        if (kind == null || zone == null) return "알 수 없는 용병 배치입니다.";
        if (VillageRaidSystem.isActive()) return "습격 중에는 용병 배치 거점을 바꿀 수 없습니다.";
        if (!allowed(kind, zone)) return kind.displayName() + "은(는) " + zone.displayName() + "에 배치할 수 없습니다.";
        VillageSiegePersistence.putInt("merc_zone_" + kind.id(), zone.ordinal());
        moveClass(player.level().getServer(), kind, zone, true);
        return kind.displayName() + " 배치를 " + zone.displayName() + "(으)로 지정했습니다.";
    }

    public static Deployment deployment(VillageMercenarySystem.MercenaryClass kind) {
        Deployment fallback = switch (kind) {
            case BASTION -> Deployment.GATE_FRONT;
            case STRIKER -> Deployment.GATE_FRONT;
            case RANGER -> Deployment.WALL;
            case MEDIC -> Deployment.INNER;
        };
        int ordinal = VillageSiegePersistence.getInt("merc_zone_" + kind.id(), fallback.ordinal());
        return Deployment.values()[Math.max(0, Math.min(Deployment.values().length - 1, ordinal))];
    }

    public static void tick(MinecraftServer server) {
        if (server == null || ++ticks < 20) return;
        ticks = 0;
        for (VillageMercenarySystem.MercenaryClass kind : VillageMercenarySystem.MercenaryClass.values()) {
            moveClass(server, kind, deployment(kind), false);
        }
    }

    private static void moveClass(MinecraftServer server, VillageMercenarySystem.MercenaryClass kind,
                                  Deployment zone, boolean force) {
        if (server == null) return;
        ServerLevel level = server.overworld();
        BlockPos center = VillageCouncilState.villageCenter().orElse(null);
        if (center == null) return;
        BlockPos rally = rallyPoint(center, zone, kind);
        AABB area = new AABB(center).inflate(VillageWorldSystem.BATTLEFIELD_RADIUS, 96,
                VillageWorldSystem.BATTLEFIELD_RADIUS);
        for (IronGolem golem : level.getEntitiesOfClass(IronGolem.class, area,
                mob -> classFromName(mob) == kind && mob.isAlive())) {
            double leash = switch (kind) {
                case BASTION -> 18.0;
                case STRIKER -> 34.0;
                case RANGER -> 22.0;
                case MEDIC -> 15.0;
            };
            if (force || !VillageRaidSystem.isActive() || golem.blockPosition().distSqr(rally) > leash * leash) {
                boolean accepted = golem.getNavigation().moveTo(rally.getX() + 0.5, rally.getY(), rally.getZ() + 0.5,
                        kind == VillageMercenarySystem.MercenaryClass.STRIKER ? 1.18 : 1.02);
                if (!accepted && zone == Deployment.WALL) {
                    BlockPos fallback = rallyPoint(center, Deployment.INNER, kind);
                    golem.getNavigation().moveTo(fallback.getX() + 0.5, fallback.getY(), fallback.getZ() + 0.5, 1.0);
                }
            }
            if (!VillageRaidSystem.isActive()) continue;
            if (kind == VillageMercenarySystem.MercenaryClass.BASTION) {
                Mob target = VillageRaidSystem.nearestActiveEnemy(level, rally, 25.0);
                if (target != null) golem.setTarget(target);
            } else if (kind == VillageMercenarySystem.MercenaryClass.STRIKER) {
                Mob target = VillageRaidSystem.nearestActiveEnemy(level, golem.blockPosition(), 42.0);
                if (target != null) golem.setTarget(target);
            } else if (kind == VillageMercenarySystem.MercenaryClass.RANGER
                    || kind == VillageMercenarySystem.MercenaryClass.MEDIC) {
                golem.setTarget(null);
            }
        }
    }

    private static BlockPos rallyPoint(BlockPos center, Deployment zone, VillageMercenarySystem.MercenaryClass kind) {
        return switch (zone) {
            case GATE_FRONT -> center.offset(kind == VillageMercenarySystem.MercenaryClass.STRIKER ? 9 : -9, 0, -58);
            case INNER -> center.offset(kind.ordinal() * 4 - 6, 0, -18);
            case WALL -> center.offset(kind == VillageMercenarySystem.MercenaryClass.RANGER ? 26 : -26, 10, -69);
        };
    }

    private static boolean allowed(VillageMercenarySystem.MercenaryClass kind, Deployment zone) {
        return switch (kind) {
            case BASTION -> zone != Deployment.WALL;
            case STRIKER -> zone != Deployment.WALL;
            case RANGER -> true;
            case MEDIC -> zone != Deployment.WALL;
        };
    }

    private static VillageMercenarySystem.MercenaryClass classFromName(IronGolem golem) {
        Component name = golem.getCustomName();
        if (name == null) return null;
        String plain = ChatFormatting.stripFormatting(name.getString());
        if (plain == null) return null;
        for (VillageMercenarySystem.MercenaryClass kind : VillageMercenarySystem.MercenaryClass.values()) {
            if (plain.startsWith(kind.displayName())) return kind;
        }
        return null;
    }

    private static void send(ServerPlayer player, String screenId, String title, String body,
                             List<String> actions, List<String> labels) {
        VillageNetwork.open(player, new VillageNetwork.OpenVillageUiPayload(
                screenId, title, body, String.join(SEP, actions), String.join(SEP, labels)));
    }

    public enum Deployment {
        GATE_FRONT("front", "성문 전방", "수호병·공격병이 주공을 먼저 저지하는 전선"),
        INNER("inner", "성 내부", "의무병과 예비대가 시설·플레이어를 지원하는 안전 거점"),
        WALL("wall", "성벽", "궁수 중심의 고지 원거리 지원 거점");
        private final String id, displayName, description;
        Deployment(String id, String displayName, String description) {
            this.id = id; this.displayName = displayName; this.description = description;
        }
        public String id() { return id; }
        public String displayName() { return displayName; }
        public String description() { return description; }
        public static Deployment fromId(String id) {
            if (id == null) return null;
            String value = id.toLowerCase(Locale.ROOT);
            for (Deployment zone : values()) if (zone.id.equals(value)) return zone;
            return null;
        }
    }
}
