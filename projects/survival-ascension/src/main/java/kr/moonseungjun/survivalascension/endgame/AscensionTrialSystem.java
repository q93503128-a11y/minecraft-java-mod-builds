package kr.moonseungjun.survivalascension.endgame;

/*
 * Wave lifecycle and boss-bar encounter presentation are adapted from the MIT-licensed
 * Gateways to Eternity project (Copyright (c) 2020 Brennan Ward).
 * Survival Ascension uses its own activation economy, vanilla-mob compositions,
 * world/infrastructure gates, failure rules and rewards.
 */

import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureData;
import kr.moonseungjun.survivalascension.infrastructure.InfrastructureProject;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AscensionTrialSystem {
    public static final int ECHO_SHARD_COST = 32;
    public static final int AMETHYST_COST = 64;
    public static final int DRAGON_BREATH_COST = 8;

    private static final String READY_TICK_KEY = "survivalascension_ascension_trial_ready";
    private static final String TRIAL_OWNER_KEY = "survivalascension_ascension_trial_owner";
    private static final String TRIAL_WAVE_KEY = "survivalascension_ascension_trial_wave";

    private static final int TOTAL_WAVES = 4;
    private static final int SETUP_TICKS = 100;
    private static final int WAVE_TIMEOUT_TICKS = 1200;
    private static final int START_COOLDOWN_TICKS = 2400;
    private static final int OWNER_GRACE_TICKS = 200;
    private static final double PLAYER_RADIUS = 64.0D;
    private static final double EXCLUSION_RADIUS = 96.0D;
    private static final double RECALL_RADIUS = 48.0D;
    private static final int[] WAVE_COUNTS = {8, 10, 10, 12};

    private static final Map<UUID, Trial> ACTIVE = new HashMap<>();
    private static int ticker;

    private AscensionTrialSystem() {}

    public static void tryStart(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.isCreative() || player.isSpectator()) {
            player.sendSystemMessage(Component.literal("§5[승천 시련] §f크리에이티브/관전자 상태에서는 시작할 수 없습니다."));
            return;
        }
        MinecraftServer server = level.getServer();
        removeStaleServerTrials(server);
        if (WorldAscensionData.get(server).stage() < 2) {
            player.sendSystemMessage(Component.literal("§5[승천 시련] §f월드 승천 §d2단계§f가 필요합니다."));
            return;
        }
        if (!InfrastructureData.get(player).isComplete(InfrastructureProject.ASCENSION_NEXUS)) {
            player.sendSystemMessage(Component.literal("§5[승천 시련] §f먼저 §d승천 중추§f를 완공해야 합니다."));
            return;
        }
        if (ACTIVE.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§5[승천 시련] §f이미 진행 중인 시련이 있습니다."));
            return;
        }
        for (Trial active : ACTIVE.values()) {
            if (active.level == level && active.center.distSqr(player.blockPosition()) < EXCLUSION_RADIUS * EXCLUSION_RADIUS) {
                player.sendSystemMessage(Component.literal("§5[승천 시련] §f근처에서 다른 시련이 진행 중입니다. §7(96블록 간격 필요)"));
                return;
            }
        }

        long now = level.getGameTime();
        CompoundTag persistent = player.getPersistentData();
        long ready = persistent.getLongOr(READY_TICK_KEY, 0L);
        if (now < ready) {
            long seconds = Math.max(1L, (ready - now + 19L) / 20L);
            player.sendSystemMessage(Component.literal("§5[승천 시련] §f재개방까지 §d" + seconds + "초§f 남았습니다."));
            return;
        }

        BlockPos center = player.blockPosition();
        if (countOpenSpawnSlots(level, center) < 8) {
            player.sendSystemMessage(Component.literal("§5[승천 시련] §f주변 공간이 좁습니다. 반경 8~14블록의 열린 지형에서 시작하세요."));
            return;
        }
        if (!hasCost(player)) {
            player.sendSystemMessage(Component.literal("§5[승천 시련] §f입장 재료가 부족합니다. §b메아리 조각 32 §7· §d자수정 조각 64 §7· §5드래곤의 숨결 8"));
            return;
        }

        consume(player, Items.ECHO_SHARD, ECHO_SHARD_COST);
        consume(player, Items.AMETHYST_SHARD, AMETHYST_COST);
        consume(player, Items.DRAGON_BREATH, DRAGON_BREATH_COST);
        persistent.putLong(READY_TICK_KEY, now + START_COOLDOWN_TICKS);

        Trial trial = new Trial(player.getUUID(), level, center, now + SETUP_TICKS);
        ACTIVE.put(player.getUUID(), trial);
        trial.bossBar.addPlayer(player);
        trial.bossBar.setVisible(true);
        player.sendSystemMessage(Component.literal("§5[승천 시련] §f개방되었습니다. §d4개 웨이브§f를 제한시간 안에 격파하세요. §7이탈/사망 10초 지속 시 실패"));
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        removeStaleServerTrials(event.getServer());
        if (++ticker < 5) return;
        ticker = 0;
        if (ACTIVE.isEmpty()) return;

        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, Trial> entry : new ArrayList<>(ACTIVE.entrySet())) {
            if (tickTrial(event.getServer(), entry.getValue())) finished.add(entry.getKey());
        }
        for (UUID owner : finished) ACTIVE.remove(owner);
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
        String ownerText = mob.getPersistentData().getStringOr(TRIAL_OWNER_KEY, "");
        if (ownerText.isEmpty()) return;

        UUID ownerId;
        try {
            ownerId = UUID.fromString(ownerText);
        } catch (IllegalArgumentException ignored) {
            event.setCanceled(true);
            return;
        }

        Trial active = ACTIVE.get(ownerId);
        if (active == null || active.level != level || !active.mobIds.contains(mob.getUUID())) {
            event.setCanceled(true);
        }
    }

    private static boolean tickTrial(MinecraftServer server, Trial trial) {
        long now = trial.level.getGameTime();
        ServerPlayer owner = server.getPlayerList().getPlayer(trial.owner);
        boolean ownerValid = owner != null && owner.isAlive() && !owner.isSpectator()
                && owner.level() == trial.level && distanceToCenterSqr(owner, trial.center) <= PLAYER_RADIUS * PLAYER_RADIUS;
        if (ownerValid) trial.ownerAbsentTicks = 0;
        else trial.ownerAbsentTicks += 5;
        syncBossBarPlayers(server, trial);

        if (trial.ownerAbsentTicks >= OWNER_GRACE_TICKS) {
            fail(trial, owner, "소유자가 전장에서 이탈하거나 사망했습니다.");
            return true;
        }

        pruneAndRecall(trial, owner);
        if (!trial.mobIds.isEmpty()) {
            if (now >= trial.waveDeadline) {
                fail(trial, owner, "웨이브 제한시간을 초과했습니다.");
                return true;
            }
            int seconds = Math.max(0, (int) ((trial.waveDeadline - now + 19L) / 20L));
            trial.bossBar.setName(Component.literal("§5승천 시련 §f· " + trial.wave + "/" + TOTAL_WAVES + " 웨이브 §7· 적 " + trial.mobIds.size() + "체 · " + seconds + "초"));
            float remaining = (trial.waveDeadline - now) / (float) WAVE_TIMEOUT_TICKS;
            trial.bossBar.setProgress(Mth.clamp(remaining, 0.0F, 1.0F));
            return false;
        }

        if (trial.wave > 0 && !trial.waveResolved) {
            trial.waveResolved = true;
            if (owner != null) {
                int xp = switch (trial.wave) { case 1 -> 25; case 2 -> 40; case 3 -> 60; default -> 0; };
                if (xp > 0) owner.giveExperiencePoints(xp);
                owner.sendSystemMessage(Component.literal("§5[승천 시련] §f" + trial.wave + " 웨이브 격파" + (xp > 0 ? " §7· 경험치 +" + xp : "")));
            }
            if (trial.wave >= TOTAL_WAVES) {
                complete(trial, owner);
                return true;
            }
            trial.nextWaveTick = now + SETUP_TICKS;
        }

        if (now < trial.nextWaveTick) {
            int seconds = Math.max(0, (int) ((trial.nextWaveTick - now + 19L) / 20L));
            trial.bossBar.setName(Component.literal("§5승천 시련 §f· 다음 웨이브까지 §d" + seconds + "초"));
            trial.bossBar.setProgress(Mth.clamp((trial.nextWaveTick - now) / (float) SETUP_TICKS, 0.0F, 1.0F));
            return false;
        }

        if (!spawnWave(trial)) {
            fail(trial, owner, "적을 배치할 공간이 부족합니다. 더 열린 지형에서 다시 시도하세요.");
            return true;
        }
        return false;
    }

    private static boolean spawnWave(Trial trial) {
        trial.wave++;
        trial.waveResolved = false;
        int target = WAVE_COUNTS[trial.wave - 1];
        Set<UUID> spawned = new HashSet<>();
        for (int i = 0; i < target; i++) {
            Mob mob = spawnOne(trial.level, trial.center, waveTypeId(trial.wave, i), i, target);
            if (mob == null) continue;
            mob.setPersistenceRequired();
            mob.getPersistentData().putString(TRIAL_OWNER_KEY, trial.owner.toString());
            mob.getPersistentData().putInt(TRIAL_WAVE_KEY, trial.wave);
            spawned.add(mob.getUUID());
        }
        if (spawned.size() < Math.max(4, target * 2 / 3)) {
            for (UUID id : spawned) {
                Entity entity = trial.level.getEntity(id);
                if (entity != null) entity.discard();
            }
            trial.mobIds.clear();
            return false;
        }
        trial.mobIds.clear();
        trial.mobIds.addAll(spawned);
        trial.waveDeadline = trial.level.getGameTime() + WAVE_TIMEOUT_TICKS;
        trial.bossBar.setProgress(1.0F);
        return true;
    }

    private static Mob spawnOne(ServerLevel level, BlockPos center, String typeId, int index, int count) {
        Identifier identifier = Identifier.parse(typeId);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(identifier)) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(identifier);
        if (type == null) return null;
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = Math.PI * 2.0D * (index + attempt * 0.37D) / Math.max(1, count);
            int radius = 8 + level.getRandom().nextInt(7);
            int dx = (int) Math.round(Math.cos(angle) * radius);
            int dz = (int) Math.round(Math.sin(angle) * radius);
            BlockPos base = center.offset(dx, 0, dz);
            BlockPos pos = findOpenSpawn(level, base);
            if (pos == null) continue;
            Entity entity = type.spawn(level, pos, EntitySpawnReason.TRIGGERED);
            if (entity instanceof Mob mob) return mob;
            if (entity != null) entity.discard();
        }
        return null;
    }

    private static BlockPos findOpenSpawn(ServerLevel level, BlockPos base) {
        for (int dy = 4; dy >= -5; dy--) {
            BlockPos pos = base.offset(0, dy, 0);
            if (!level.hasChunkAt(pos)) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
            if (level.getBlockState(pos.below()).isAir()) continue;
            if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) continue;
            return pos;
        }
        return null;
    }

    private static int countOpenSpawnSlots(ServerLevel level, BlockPos center) {
        int open = 0;
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2.0D * i / 16.0D;
            BlockPos base = center.offset((int) Math.round(Math.cos(angle) * 10.0D), 0, (int) Math.round(Math.sin(angle) * 10.0D));
            if (findOpenSpawn(level, base) != null) open++;
        }
        return open;
    }

    private static String waveTypeId(int wave, int index) {
        return switch (wave) {
            case 1 -> index % 2 == 0 ? "minecraft:zombie" : "minecraft:skeleton";
            case 2 -> switch (index % 5) {
                case 0, 1 -> "minecraft:husk";
                case 2, 3 -> "minecraft:stray";
                default -> "minecraft:witch";
            };
            case 3 -> switch (index % 5) {
                case 0, 1 -> "minecraft:wither_skeleton";
                case 2 -> "minecraft:vindicator";
                case 3 -> "minecraft:pillager";
                default -> "minecraft:witch";
            };
            default -> switch (index) {
                case 0 -> "minecraft:ravager";
                case 1, 2, 3 -> "minecraft:vindicator";
                case 4, 5 -> "minecraft:wither_skeleton";
                case 6, 7 -> "minecraft:pillager";
                default -> "minecraft:witch";
            };
        };
    }

    private static void pruneAndRecall(Trial trial, ServerPlayer owner) {
        if (trial.mobIds.isEmpty()) return;
        Set<UUID> alive = new HashSet<>();
        for (UUID id : trial.mobIds) {
            Entity entity = trial.level.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
            alive.add(id);
            if (owner != null && mob.getTarget() == null) mob.setTarget(owner);
            if (distanceToCenterSqr(mob, trial.center) > RECALL_RADIUS * RECALL_RADIUS) {
                mob.getNavigation().moveTo(trial.center.getX() + 0.5D, trial.center.getY(), trial.center.getZ() + 0.5D, 1.25D);
            }
        }
        trial.mobIds.clear();
        trial.mobIds.addAll(alive);
    }

    private static void syncBossBarPlayers(MinecraftServer server, Trial trial) {
        Set<ServerPlayer> shouldSee = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == trial.level && player.isAlive() && !player.isSpectator()
                    && distanceToCenterSqr(player, trial.center) <= PLAYER_RADIUS * PLAYER_RADIUS) {
                shouldSee.add(player);
                if (!trial.bossBar.getPlayers().contains(player)) trial.bossBar.addPlayer(player);
            }
        }
        for (ServerPlayer viewer : List.copyOf(trial.bossBar.getPlayers())) {
            if (!shouldSee.contains(viewer)) trial.bossBar.removePlayer(viewer);
        }
    }

    private static void complete(Trial trial, ServerPlayer owner) {
        if (owner != null) {
            owner.giveExperiencePoints(200);
            giveOrDrop(owner, AscensionAffixes.createEliteDrop(trial.level.getRandom(), 3));
            giveOrDrop(owner, new ItemStack(Items.NETHERITE_SCRAP, 2));
            giveOrDrop(owner, new ItemStack(Items.DIAMOND, 4));
            owner.sendSystemMessage(Component.literal("§d[승천 시련 완료] §f신화 III 장비 1개 §7· 네더라이트 파편 2 · 다이아 4 · 경험치 +200"));
        }
        for (ServerPlayer player : trial.level.getServer().getPlayerList().getPlayers()) {
            if (player == owner || player.level() != trial.level || !player.isAlive() || player.isSpectator()) continue;
            if (distanceToCenterSqr(player, trial.center) <= 48.0D * 48.0D) {
                player.giveExperiencePoints(80);
                player.sendSystemMessage(Component.literal("§5[승천 시련] §f협동 완료 보상 경험치 §d+80"));
            }
        }
        closeBossBar(trial);
    }

    private static void fail(Trial trial, ServerPlayer owner, String reason) {
        for (UUID id : trial.mobIds) {
            Entity entity = trial.level.getEntity(id);
            if (entity != null) entity.discard();
        }
        trial.mobIds.clear();
        if (owner != null) owner.sendSystemMessage(Component.literal("§c[승천 시련 실패] §f" + reason + " §7· 입장 재료는 반환되지 않습니다."));
        closeBossBar(trial);
    }

    private static void removeStaleServerTrials(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, Trial> entry : ACTIVE.entrySet()) {
            Trial trial = entry.getValue();
            if (trial.level.getServer() != server) {
                closeBossBar(trial);
                stale.add(entry.getKey());
            }
        }
        for (UUID owner : stale) ACTIVE.remove(owner);
    }

    private static void closeBossBar(Trial trial) {
        trial.bossBar.setVisible(false);
        for (ServerPlayer viewer : List.copyOf(trial.bossBar.getPlayers())) trial.bossBar.removePlayer(viewer);
    }

    private static boolean hasCost(ServerPlayer player) {
        return count(player, Items.ECHO_SHARD) >= ECHO_SHARD_COST
                && count(player, Items.AMETHYST_SHARD) >= AMETHYST_COST
                && count(player, Items.DRAGON_BREATH) >= DRAGON_BREATH_COST;
    }

    private static int count(ServerPlayer player, Item item) {
        int found = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item)) found += stack.getCount();
        }
        return found;
    }

    private static void consume(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(item)) continue;
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        player.getInventory().setChanged();
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static double distanceToCenterSqr(Entity entity, BlockPos center) {
        double dx = entity.getX() - (center.getX() + 0.5D);
        double dy = entity.getY() - (center.getY() + 0.5D);
        double dz = entity.getZ() - (center.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static final class Trial {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final ServerBossEvent bossBar;
        final Set<UUID> mobIds = new HashSet<>();
        int wave;
        long waveDeadline;
        long nextWaveTick;
        int ownerAbsentTicks;
        boolean waveResolved = true;

        Trial(UUID owner, ServerLevel level, BlockPos center, long nextWaveTick) {
            this.owner = owner;
            this.level = level;
            this.center = center.immutable();
            this.nextWaveTick = nextWaveTick;
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal("§5승천 시련"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
            this.bossBar.setProgress(1.0F);
        }
    }
}
