package kr.moonseungjun.survivalascension.elite;

/*
 * Tactical squad concepts are adapted from Warband (Renasca Studios / Divesh Gupta, MIT):
 * lightweight persistent squad membership, shared target focus, role separation, and temporary
 * routing after the leader falls. Survival Ascension uses its own formation rules, progression
 * thresholds, vanilla-item rewards and tick-budgeted player-centered coordinator.
 */

import kr.moonseungjun.survivalascension.SurvivalAscension;
import kr.moonseungjun.survivalascension.progress.SkillProgressData;
import kr.moonseungjun.survivalascension.progress.SkillType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class WarbandDirector {
    private static final String SQUAD_KEY = "survivalascension_warband_id";
    private static final String ROLE_KEY = "survivalascension_warband_role";
    private static final String ROUT_UNTIL_KEY = "survivalascension_warband_rout_until";
    private static final String ROLE_READY_KEY = "survivalascension_warband_role_ready";
    private static final String NO_WARBAND_KEY = "survivalascension_no_warband";
    private static final String PLAYER_NEXT_FORMATION_KEY = "survivalascension_next_warband_formation";

    private static final int BEHAVIOR_INTERVAL = 10;
    private static final int FORMATION_INTERVAL = 200;
    private static final int ROUT_TICKS = 160;
    private static final double FORMATION_RADIUS = 40.0D;
    private static final double COORDINATION_RADIUS = 56.0D;

    private static final Identifier LEADER_HEALTH_ID = id("warband_leader_health");
    private static final Identifier LEADER_ATTACK_ID = id("warband_leader_attack");
    private static final Identifier BRUISER_ATTACK_ID = id("warband_bruiser_attack");
    private static final Identifier BRUISER_KNOCKBACK_ID = id("warband_bruiser_knockback");
    private static final Identifier HUNTER_SPEED_ID = id("warband_hunter_speed");
    private static final Identifier SUPPORT_HEALTH_ID = id("warband_support_health");
    private static final Identifier SUPPORT_SPEED_ID = id("warband_support_speed");

    private static int behaviorTicker;
    private static int formationTicker;

    private WarbandDirector() {}

    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (event.getSpawnType().name().contains("SPAWNER")) {
            mob.getPersistentData().putBoolean(NO_WARBAND_KEY, true);
        }
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        boolean coordinate = ++behaviorTicker >= BEHAVIOR_INTERVAL;
        boolean form = ++formationTicker >= FORMATION_INTERVAL;
        if (!coordinate && !form) return;
        if (coordinate) behaviorTicker = 0;
        if (form) formationTicker = 0;

        Set<String> handledSquads = new HashSet<>();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.isSpectator() || !(player.level() instanceof ServerLevel level)) continue;
            if (form) tryFormWarband(player, level);
            if (coordinate) coordinateNearbyWarbands(player, level, handledSquads);
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob leader) || role(leader) != Role.LEADER) return;
        if (!(leader.level() instanceof ServerLevel level)) return;
        String squadId = squadId(leader);
        if (squadId.isEmpty()) return;

        long routUntil = level.getGameTime() + ROUT_TICKS;
        List<Mob> members = level.getEntitiesOfClass(Mob.class, leader.getBoundingBox().inflate(COORDINATION_RADIUS),
                mob -> mob.isAlive() && squadId.equals(squadId(mob)) && mob != leader);
        for (Mob member : members) {
            member.getPersistentData().putLong(ROUT_UNTIL_KEY, routUntil);
            member.setTarget(null);
        }

        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            int combat = SkillProgressData.get(player).level(player, SkillType.COMBAT);
            int shards = Math.max(1, Math.min(4, 1 + combat / 30));
            level.addFreshEntity(new ItemEntity(level, leader.getX(), leader.getY() + 0.5D, leader.getZ(), new ItemStack(Items.ECHO_SHARD, shards)));
            player.sendSystemMessage(Component.literal("§4[전단장 격파] §f적 분대가 붕괴합니다. §b메아리 조각 +" + shards));
        }
    }

    public static boolean isWarbandMember(Mob mob) { return !squadId(mob).isEmpty(); }

    private static void tryFormWarband(ServerPlayer player, ServerLevel level) {
        double power = averageSkillLevel(player);
        if (power < 30.0D) return;
        long now = level.getGameTime();
        CompoundTag playerData = player.getPersistentData();
        if (now < playerData.getLongOr(PLAYER_NEXT_FORMATION_KEY, 0L)) return;
        playerData.putLong(PLAYER_NEXT_FORMATION_KEY, now + 600L);

        double chance = Math.min(0.50D, 0.12D + Math.max(0.0D, power - 30.0D) * 0.006D);
        if (level.getRandom().nextDouble() >= chance) return;

        List<Mob> existing = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(COORDINATION_RADIUS),
                mob -> mob.isAlive() && mob instanceof Enemy && isWarbandMember(mob));
        if (!existing.isEmpty()) return;

        List<Mob> candidates = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(FORMATION_RADIUS),
                mob -> eligibleCandidate(mob));
        if (candidates.size() < 3) return;
        candidates.sort(Comparator.comparingDouble(player::distanceToSqr));

        int desired = Math.max(3, Math.min(6, 3 + (int) Math.floor((power - 30.0D) / 20.0D)));
        int size = Math.min(desired, candidates.size());
        String squadId = UUID.randomUUID().toString();
        Role[] pattern = { Role.LEADER, Role.BRUISER, Role.HUNTER, Role.SUPPORT, Role.BRUISER, Role.HUNTER };
        for (int i = 0; i < size; i++) assign(candidates.get(i), squadId, pattern[i]);

        player.sendSystemMessage(Component.literal("§4[적 전술 분대] §f" + size + "체가 협동 전투를 시작합니다."), true);
    }

    private static boolean eligibleCandidate(Mob mob) {
        if (!(mob instanceof Enemy) || !mob.isAlive() || mob.isBaby()) return false;
        if (mob instanceof EnderDragon || mob instanceof WitherBoss) return false;
        CompoundTag data = mob.getPersistentData();
        return !data.getBooleanOr(NO_WARBAND_KEY, false) && data.getStringOr(SQUAD_KEY, "").isEmpty();
    }

    private static void assign(Mob mob, String squadId, Role role) {
        CompoundTag data = mob.getPersistentData();
        data.putString(SQUAD_KEY, squadId);
        data.putString(ROLE_KEY, role.id);
        switch (role) {
            case LEADER -> {
                addPermanent(mob.getAttribute(Attributes.MAX_HEALTH), LEADER_HEALTH_ID, 0.25D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                addPermanent(mob.getAttribute(Attributes.ATTACK_DAMAGE), LEADER_ATTACK_ID, 0.10D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                mob.setHealth(mob.getMaxHealth());
                mob.setCustomName(Component.literal("§4[전단장] §f" + mob.getName().getString()));
                mob.setCustomNameVisible(true);
            }
            case BRUISER -> {
                addPermanent(mob.getAttribute(Attributes.ATTACK_DAMAGE), BRUISER_ATTACK_ID, 0.20D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                addPermanent(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), BRUISER_KNOCKBACK_ID, 0.15D, AttributeModifier.Operation.ADD_VALUE);
            }
            case HUNTER -> addPermanent(mob.getAttribute(Attributes.MOVEMENT_SPEED), HUNTER_SPEED_ID, 0.18D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case SUPPORT -> {
                addPermanent(mob.getAttribute(Attributes.MAX_HEALTH), SUPPORT_HEALTH_ID, 0.15D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                addPermanent(mob.getAttribute(Attributes.MOVEMENT_SPEED), SUPPORT_SPEED_ID, 0.05D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                mob.setHealth(mob.getMaxHealth());
            }
            case NONE -> { }
        }
    }

    private static void coordinateNearbyWarbands(ServerPlayer observer, ServerLevel level, Set<String> handledSquads) {
        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, observer.getBoundingBox().inflate(COORDINATION_RADIUS),
                mob -> mob.isAlive() && isWarbandMember(mob));
        Map<String, List<Mob>> groups = new HashMap<>();
        for (Mob mob : nearby) groups.computeIfAbsent(squadId(mob), key -> new ArrayList<>()).add(mob);

        for (Map.Entry<String, List<Mob>> entry : groups.entrySet()) {
            if (!handledSquads.add(entry.getKey())) continue;
            coordinateSquad(level, entry.getValue());
        }
    }

    private static void coordinateSquad(ServerLevel level, List<Mob> members) {
        if (members.isEmpty()) return;
        Mob anchor = members.getFirst();
        List<ServerPlayer> targets = level.getEntitiesOfClass(ServerPlayer.class, anchor.getBoundingBox().inflate(64.0D),
                player -> player.isAlive() && !player.isSpectator());
        if (targets.isEmpty()) return;
        targets.sort(Comparator.comparingDouble(anchor::distanceToSqr));
        ServerPlayer target = targets.getFirst();
        long now = level.getGameTime();

        for (Mob member : members) {
            CompoundTag data = member.getPersistentData();
            long routUntil = data.getLongOr(ROUT_UNTIL_KEY, 0L);
            if (now < routUntil) {
                retreat(member, target);
                continue;
            }
            member.setTarget(target);
            if (now < data.getLongOr(ROLE_READY_KEY, 0L)) continue;
            switch (role(member)) {
                case LEADER -> data.putLong(ROLE_READY_KEY, now + 40L);
                case BRUISER -> bruiserAction(member, target, data, now);
                case HUNTER -> hunterAction(member, target, data, now, level);
                case SUPPORT -> supportAction(member, members, data, now);
                case NONE -> { }
            }
        }
    }

    private static void retreat(Mob mob, ServerPlayer target) {
        Vec3 away = mob.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() <= 1.0E-5D) return;
        away = away.normalize();
        mob.setTarget(null);
        mob.setDeltaMovement(mob.getDeltaMovement().add(away.x * 0.22D, 0.06D, away.z * 0.22D));
        mob.hurtMarked = true;
    }

    private static void bruiserAction(Mob mob, ServerPlayer target, CompoundTag data, long now) {
        double distance = mob.distanceToSqr(target);
        if (distance < 9.0D || distance > 121.0D) return;
        Vec3 toward = target.position().subtract(mob.position()).multiply(1.0D, 0.0D, 1.0D);
        if (toward.lengthSqr() <= 1.0E-5D) return;
        toward = toward.normalize();
        mob.setDeltaMovement(toward.x * 0.65D, Math.max(0.10D, mob.getDeltaMovement().y), toward.z * 0.65D);
        mob.hurtMarked = true;
        data.putLong(ROLE_READY_KEY, now + 50L);
    }

    private static void hunterAction(Mob mob, ServerPlayer target, CompoundTag data, long now, ServerLevel level) {
        Vec3 away = mob.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() <= 1.0E-5D) return;
        away = away.normalize();
        double sign = level.getRandom().nextBoolean() ? 1.0D : -1.0D;
        Vec3 side = new Vec3(-away.z, 0.0D, away.x).scale(sign);
        double distance = mob.distanceToSqr(target);
        Vec3 impulse = side.scale(0.38D);
        if (distance < 25.0D) impulse = impulse.add(away.scale(0.30D));
        else if (distance > 144.0D) impulse = impulse.add(away.scale(-0.22D));
        mob.setDeltaMovement(mob.getDeltaMovement().add(impulse.x, 0.04D, impulse.z));
        mob.hurtMarked = true;
        data.putLong(ROLE_READY_KEY, now + 35L);
    }

    private static void supportAction(Mob mob, List<Mob> members, CompoundTag data, long now) {
        Mob wounded = null;
        double lowestRatio = 1.0D;
        for (Mob candidate : members) {
            if (!candidate.isAlive() || candidate.distanceToSqr(mob) > 64.0D) continue;
            double ratio = candidate.getHealth() / Math.max(1.0F, candidate.getMaxHealth());
            if (ratio < lowestRatio) {
                lowestRatio = ratio;
                wounded = candidate;
            }
        }
        if (wounded == null || lowestRatio >= 0.98D) return;
        wounded.heal(Math.max(2.0F, wounded.getMaxHealth() * 0.08F));
        data.putLong(ROLE_READY_KEY, now + 80L);
    }

    private static double averageSkillLevel(ServerPlayer player) {
        SkillProgressData data = SkillProgressData.get(player);
        int total = 0;
        for (SkillType skill : SkillType.values()) total += data.level(player, skill);
        return total / (double) SkillType.values().length;
    }

    private static String squadId(Mob mob) { return mob.getPersistentData().getStringOr(SQUAD_KEY, ""); }
    private static Role role(Mob mob) { return Role.fromId(mob.getPersistentData().getStringOr(ROLE_KEY, "")); }

    private static void addPermanent(AttributeInstance attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
        if (attribute == null || amount == 0.0D || attribute.hasModifier(id)) return;
        attribute.addPermanentModifier(new AttributeModifier(id, amount, operation));
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath(SurvivalAscension.MOD_ID, path); }

    private enum Role {
        NONE("", ""), LEADER("leader", "전단장"), BRUISER("bruiser", "돌격"), HUNTER("hunter", "추적"), SUPPORT("support", "지원");
        private final String id;
        private final String korean;
        Role(String id, String korean) { this.id = id; this.korean = korean; }
        private static Role fromId(String id) {
            for (Role role : values()) if (role.id.equals(id)) return role;
            return NONE;
        }
    }
}
