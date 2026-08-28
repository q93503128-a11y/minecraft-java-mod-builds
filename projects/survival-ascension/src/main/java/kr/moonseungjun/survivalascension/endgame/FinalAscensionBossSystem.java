package kr.moonseungjun.survivalascension.endgame;

import kr.moonseungjun.survivalascension.equipment.AscensionAffixes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Unique final encounter controller. The Warden is only a vanilla visual/AI shell: health gates,
 * arena objectives, telegraphs, damage windows, completion and rewards are Survival-owned.
 */
public final class FinalAscensionBossSystem {
    private static final String BOSS_KEY = "survivalascension_final_boss";
    private static final String OWNER_KEY = "survivalascension_final_boss_owner";
    private static final String NO_WARBAND_KEY = "survivalascension_no_warband";

    private static final int TICK_INTERVAL = 2;
    private static final int OWNER_GRACE_TICKS = 120;
    private static final int ENCOUNTER_TIMEOUT_TICKS = 4200;
    private static final int TELEGRAPH_TICKS = 30;
    private static final int WARDEN_AGGRO_REFRESH_TICKS = 80;
    private static final double PLAYER_RADIUS = 72.0D;
    private static final double RECALL_RADIUS = 44.0D;

    private static final Map<UUID, Run> ACTIVE = new HashMap<>();
    private static boolean internalSpawn;
    private static int ticker;

    private FinalAscensionBossSystem() {}

    public static boolean tryStartFromClosure(ServerPlayer owner, BlockPos center) {
        if (!(owner.level() instanceof ServerLevel level)) return false;
        if (FinalAscensionData.get(level.getServer()).isComplete()) return false;
        if (ACTIVE.containsKey(owner.getUUID())) return false;

        Mob boss = spawnBoss(level, center, owner);
        if (boss == null) {
            owner.sendSystemMessage(Component.literal("§c[최후의 승천] §f최심부의 경계를 형성할 열린 공간이 부족합니다."));
            return false;
        }

        Run run = new Run(owner.getUUID(), level, center, boss.getUUID(), level.getGameTime() + ENCOUNTER_TIMEOUT_TICKS);
        ACTIVE.put(owner.getUUID(), run);
        run.bossBar.addPlayer(owner);
        run.bossBar.setVisible(true);
        owner.sendSystemMessage(Component.literal("§4[최종 관문] §f§d세계의 경계자§f가 모습을 드러냈습니다."));
        owner.sendSystemMessage(Component.literal("§7전조가 보이는 공격을 피하고, 체력 65%에서 생기는 실제 광핵을 파괴해 보호를 해제하세요."));
        scheduleAttack(run, boss, owner, AttackPattern.LINE);
        return true;
    }

    public static boolean isActive(ServerPlayer player) { return ACTIVE.containsKey(player.getUUID()); }
    public static boolean isInternalSpawn() { return internalSpawn; }

    public static boolean isFinalBoss(Entity entity) {
        return entity != null && entity.getPersistentData().getBooleanOr(BOSS_KEY, false);
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        removeStaleServerRuns(event.getServer());
        if (++ticker < TICK_INTERVAL) return;
        ticker = 0;
        if (ACTIVE.isEmpty()) return;

        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, Run> entry : new ArrayList<>(ACTIVE.entrySet())) {
            if (tickRun(event.getServer(), entry.getValue())) finished.add(entry.getKey());
        }
        for (UUID owner : finished) ACTIVE.remove(owner);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        if (ACTIVE.isEmpty()) return;
        List<UUID> stopped = new ArrayList<>();
        for (Map.Entry<UUID, Run> entry : new ArrayList<>(ACTIVE.entrySet())) {
            Run run = entry.getValue();
            if (run.level.getServer() != event.getServer()) continue;
            fail(run, null, "서버 종료");
            stopped.add(entry.getKey());
        }
        for (UUID owner : stopped) ACTIVE.remove(owner);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob boss) || !isFinalBoss(boss)) return;
        Run run = findRun(boss);
        if (run == null) return;

        if (run.phase == Phase.ANCHORS) {
            event.setAmount(0.0F);
            return;
        }

        float floorRatio = run.phase == Phase.OPENING ? 0.65F : run.phase == Phase.BREAKTHROUGH ? 0.30F : 0.0F;
        if (floorRatio <= 0.0F) return;
        float floor = boss.getMaxHealth() * floorRatio;
        if (boss.getHealth() <= floor) {
            event.setAmount(0.0F);
            return;
        }
        float projected = boss.getHealth() - event.getAmount();
        if (projected < floor) event.setAmount(Math.max(0.0F, boss.getHealth() - floor));
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob boss) || !isFinalBoss(boss)) return;
        Run run = findRun(boss);
        if (run == null || run.phase != Phase.FINAL) return;
        ServerPlayer owner = run.level.getServer().getPlayerList().getPlayer(run.owner);
        complete(run, owner);
        ACTIVE.remove(run.owner);
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob) || !isFinalBoss(mob)) return;
        String raw = mob.getPersistentData().getStringOr(OWNER_KEY, "");
        UUID owner;
        try {
            owner = UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            event.setCanceled(true);
            return;
        }
        Run run = ACTIVE.get(owner);
        if (run == null || run.level != level || !run.bossId.equals(mob.getUUID())) event.setCanceled(true);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Run run = ACTIVE.remove(event.getEntity().getUUID());
        if (run != null) fail(run, null, "도전자가 전장을 떠났습니다.");
    }

    private static boolean tickRun(MinecraftServer server, Run run) {
        ServerPlayer owner = server.getPlayerList().getPlayer(run.owner);
        Entity entity = run.level.getEntity(run.bossId);
        if (!(entity instanceof Mob boss) || !boss.isAlive()) {
            if (!FinalAscensionData.get(run.level.getServer()).isComplete()) fail(run, owner, "경계자의 상태를 확인할 수 없습니다.");
            return true;
        }

        boolean ownerValid = owner != null && owner.isAlive() && !owner.isSpectator()
                && owner.level() == run.level && distanceToCenterSqr(owner, run.center) <= PLAYER_RADIUS * PLAYER_RADIUS;
        if (ownerValid) run.ownerAbsentTicks = 0;
        else run.ownerAbsentTicks += TICK_INTERVAL;
        syncBossBarPlayers(server, run);
        if (run.ownerAbsentTicks >= OWNER_GRACE_TICKS) {
            fail(run, owner, "전장에서 이탈하거나 사망했습니다.");
            return true;
        }
        if (owner == null) return false;
        long now = run.level.getGameTime();
        if (now >= run.deadline) {
            fail(run, owner, "최종 관문의 제한시간을 초과했습니다.");
            return true;
        }

        maintainBossAggro(boss, owner, now);
        if (distanceToCenterSqr(boss, run.center) > RECALL_RADIUS * RECALL_RADIUS) {
            boss.getNavigation().moveTo(run.center.getX() + 0.5D, run.center.getY(), run.center.getZ() + 0.5D, 1.35D);
        } else if (boss.distanceToSqr(owner) > 20.0D * 20.0D) {
            boss.getNavigation().moveTo(owner, 1.30D);
        }

        float ratio = boss.getHealth() / Math.max(1.0F, boss.getMaxHealth());
        if (run.phase == Phase.OPENING && ratio <= 0.6501F) {
            if (!beginAnchors(run, owner, boss)) {
                fail(run, owner, "세계 고정점을 배치할 열린 공간이 부족합니다.");
                return true;
            }
        } else if (run.phase == Phase.BREAKTHROUGH && ratio <= 0.3001F) {
            beginFinal(run, owner, boss);
        }

        if (run.phase == Phase.ANCHORS) tickAnchors(run, owner, boss);
        else tickAttack(run, owner, boss, now);
        updateBossBar(run, boss);
        return false;
    }

    private static void maintainBossAggro(Mob boss, ServerPlayer owner, long now) {
        if (boss instanceof Warden warden) {
            if (warden.getTarget() != owner || now % WARDEN_AGGRO_REFRESH_TICKS < TICK_INTERVAL) {
                warden.increaseAngerAt(owner, 150, false);
                warden.setAttackTarget(owner);
            }
            return;
        }
        if (boss.getTarget() != owner) boss.setTarget(owner);
    }

    private static boolean beginAnchors(Run run, ServerPlayer owner, Mob boss) {
        clearPendingAttack(run);
        run.phase = Phase.ANCHORS;
        run.anchorTotal = 0;
        int[][] offsets = {{10, 0}, {-5, 9}, {-5, -9}};
        for (int[] offset : offsets) {
            BlockPos pos = findOpenSpawn(run.level, run.center.offset(offset[0], 0, offset[1]));
            if (pos == null || !placeMarker(run, pos, Blocks.CRYING_OBSIDIAN)) {
                clearMarkers(run);
                return false;
            }
            run.anchors.put(pos.immutable(), Blocks.CRYING_OBSIDIAN);
            run.anchorTotal++;
        }
        boss.setGlowingTag(true);
        owner.sendSystemMessage(Component.literal("§6[경계 전환] §f경계자가 피해를 차단합니다. 보라빛 세계 고정점 3개를 실제로 채굴하세요."));
        owner.sendSystemMessage(Component.literal("§7채굴 숙련의 광역 작업이 그대로 적용되며 웅크리면 정밀 채굴입니다."));
        return true;
    }

    private static void tickAnchors(Run run, ServerPlayer owner, Mob boss) {
        run.anchors.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            if (!run.level.hasChunkAt(pos)) return false;
            return !run.level.getBlockState(pos).is(entry.getValue());
        });
        if (!run.anchors.isEmpty()) {
            if (run.level.getGameTime() % 10L == 0L) {
                for (BlockPos pos : run.anchors.keySet()) burstAt(run.level, pos, ParticleTypes.REVERSE_PORTAL, 8);
            }
            return;
        }
        clearMarkers(run);
        run.phase = Phase.BREAKTHROUGH;
        boss.setGlowingTag(false);
        run.nextAttackTick = run.level.getGameTime() + 35L;
        owner.sendSystemMessage(Component.literal("§d[경계 붕괴] §f세계 고정점이 모두 파괴되어 경계자의 보호가 풀렸습니다."));
    }

    private static void beginFinal(Run run, ServerPlayer owner, Mob boss) {
        clearPendingAttack(run);
        run.phase = Phase.FINAL;
        run.nextAttackTick = run.level.getGameTime() + 20L;
        AttributeInstance speed = boss.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(Math.min(0.36D, speed.getBaseValue() * 1.12D));
        owner.sendSystemMessage(Component.literal("§4[최종 경계] §f경계자가 붕괴를 직접 끌어옵니다. 전조선·원환·지점 붕괴를 피하며 마지막 30%를 돌파하세요."));
    }

    private static void tickAttack(Run run, ServerPlayer owner, Mob boss, long now) {
        if (run.pendingAttack != AttackPattern.NONE) {
            renderTelegraph(run);
            if (now >= run.attackExecuteTick) {
                executeAttack(run, boss);
                clearPendingAttack(run);
                run.nextAttackTick = now + attackCooldown(run.phase);
            }
            return;
        }
        if (now < run.nextAttackTick) return;

        AttackPattern pattern;
        if (run.phase == Phase.OPENING) pattern = run.attackSerial++ % 2 == 0 ? AttackPattern.LINE : AttackPattern.RING;
        else if (run.phase == Phase.BREAKTHROUGH) pattern = run.attackSerial++ % 2 == 0 ? AttackPattern.MARKED : AttackPattern.LINE;
        else pattern = switch (run.attackSerial++ % 3) {
            case 0 -> AttackPattern.LINE;
            case 1 -> AttackPattern.RING;
            default -> AttackPattern.MARKED;
        };
        scheduleAttack(run, boss, owner, pattern);
    }

    private static void scheduleAttack(Run run, Mob boss, ServerPlayer owner, AttackPattern pattern) {
        run.pendingAttack = pattern;
        run.attackExecuteTick = run.level.getGameTime() + TELEGRAPH_TICKS;
        run.attackOrigin = boss.position();
        Vec3 toward = owner.position().subtract(boss.position()).multiply(1.0D, 0.0D, 1.0D);
        run.attackDirection = toward.lengthSqr() <= 1.0E-5D ? new Vec3(1.0D, 0.0D, 0.0D) : toward.normalize();
        run.markedPoint = owner.position();
        String message = switch (pattern) {
            case LINE -> "§c[경계 전조] §f직선 파쇄 — 붉게 그어진 선에서 벗어나세요.";
            case RING -> "§5[경계 전조] §f원환 붕괴 — 경계자에게 붙거나 바깥으로 빠지세요.";
            case MARKED -> "§6[경계 전조] §f지점 붕괴 — 지금 서 있는 자리에서 이동하세요.";
            default -> "";
        };
        if (!message.isEmpty()) owner.sendSystemMessage(Component.literal(message), true);
    }

    private static void renderTelegraph(Run run) {
        if (run.level.getGameTime() % 4L != 0L) return;
        switch (run.pendingAttack) {
            case LINE -> {
                for (int i = 1; i <= 16; i++) {
                    Vec3 p = run.attackOrigin.add(run.attackDirection.scale(i));
                    particle(run.level, ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y + 0.15D, p.z);
                }
            }
            case RING -> {
                for (int i = 0; i < 28; i++) {
                    double a = Math.PI * 2.0D * i / 28.0D;
                    particle(run.level, ParticleTypes.REVERSE_PORTAL,
                            run.attackOrigin.x + Math.cos(a) * 6.5D, run.attackOrigin.y + 0.15D,
                            run.attackOrigin.z + Math.sin(a) * 6.5D);
                }
            }
            case MARKED -> {
                for (int i = 0; i < 20; i++) {
                    double a = Math.PI * 2.0D * i / 20.0D;
                    particle(run.level, ParticleTypes.CRIT,
                            run.markedPoint.x + Math.cos(a) * 3.5D, run.markedPoint.y + 0.15D,
                            run.markedPoint.z + Math.sin(a) * 3.5D);
                }
            }
            default -> { }
        }
    }

    private static void executeAttack(Run run, Mob boss) {
        float damage = run.phase == Phase.FINAL ? 16.0F : run.phase == Phase.BREAKTHROUGH ? 13.0F : 10.0F;
        for (ServerPlayer player : run.level.getServer().getPlayerList().getPlayers()) {
            if (!validArenaPlayer(player, run)) continue;
            boolean hit = switch (run.pendingAttack) {
                case LINE -> insideLine(player.position(), run.attackOrigin, run.attackDirection, 16.0D, 2.2D);
                case RING -> {
                    double d = horizontalDistance(player.position(), run.attackOrigin);
                    yield d >= 4.25D && d <= 8.75D;
                }
                case MARKED -> horizontalDistance(player.position(), run.markedPoint) <= 3.75D;
                default -> false;
            };
            if (!hit) continue;
            player.hurtServer(run.level, boss.damageSources().mobAttack(boss), damage);
            Vec3 away = player.position().subtract(run.attackOrigin).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() > 1.0E-5D) {
                away = away.normalize();
                player.setDeltaMovement(player.getDeltaMovement().add(away.x * 0.55D, 0.18D, away.z * 0.55D));
                player.hurtMarked = true;
            }
        }
    }

    private static int attackCooldown(Phase phase) {
        return switch (phase) {
            case OPENING -> 70;
            case BREAKTHROUGH -> 55;
            case FINAL -> 42;
            default -> 80;
        };
    }

    private static boolean insideLine(Vec3 point, Vec3 origin, Vec3 direction, double length, double halfWidth) {
        Vec3 delta = point.subtract(origin).multiply(1.0D, 0.0D, 1.0D);
        double projection = delta.dot(direction);
        if (projection < 0.0D || projection > length) return false;
        Vec3 lateral = delta.subtract(direction.scale(projection));
        return lateral.lengthSqr() <= halfWidth * halfWidth;
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static void clearPendingAttack(Run run) {
        run.pendingAttack = AttackPattern.NONE;
        run.attackExecuteTick = 0L;
        run.attackOrigin = Vec3.ZERO;
        run.attackDirection = Vec3.ZERO;
        run.markedPoint = Vec3.ZERO;
    }

    private static Mob spawnBoss(ServerLevel level, BlockPos center, ServerPlayer owner) {
        Identifier id = Identifier.parse("minecraft:warden");
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (type == null) return null;

        for (BlockPos raw : List.of(center.offset(9, 0, 0), center.offset(-9, 0, 0), center.offset(0, 0, 9), center.offset(0, 0, -9))) {
            BlockPos pos = findOpenSpawn(level, raw);
            if (pos == null || !level.getBlockState(pos.above(2)).isAir()) continue;
            Entity entity;
            internalSpawn = true;
            try {
                entity = type.spawn(level, pos, EntitySpawnReason.TRIGGERED);
            } finally {
                internalSpawn = false;
            }
            if (!(entity instanceof Warden warden)) {
                if (entity != null) entity.discard();
                continue;
            }
            warden.setPersistenceRequired();
            warden.getPersistentData().putBoolean(BOSS_KEY, true);
            warden.getPersistentData().putString(OWNER_KEY, owner.getUUID().toString());
            warden.getPersistentData().putBoolean(NO_WARBAND_KEY, true);
            warden.setCustomName(Component.literal("§4세계의 경계자"));
            warden.setCustomNameVisible(true);
            AttributeInstance health = warden.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) health.setBaseValue(460.0D);
            AttributeInstance attack = warden.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) attack.setBaseValue(Math.min(24.0D, Math.max(18.0D, attack.getBaseValue())));
            AttributeInstance armor = warden.getAttribute(Attributes.ARMOR);
            if (armor != null) armor.setBaseValue(Math.max(10.0D, armor.getBaseValue()));
            AttributeInstance speed = warden.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(Math.min(0.32D, Math.max(0.26D, speed.getBaseValue())));
            warden.setHealth(warden.getMaxHealth());
            maintainBossAggro(warden, owner, level.getGameTime());
            return warden;
        }
        return null;
    }

    private static boolean placeMarker(Run run, BlockPos pos, Block block) {
        if (!run.level.hasChunkAt(pos)) return false;
        if (!run.level.getBlockState(pos).isAir() || !run.level.getFluidState(pos).isEmpty()) return false;
        if (!run.level.setBlockAndUpdate(pos, block.defaultBlockState())) return false;
        run.markers.put(pos.immutable(), block);
        return true;
    }

    private static void clearMarkers(Run run) {
        for (Map.Entry<BlockPos, Block> entry : new ArrayList<>(run.markers.entrySet())) {
            BlockPos pos = entry.getKey();
            if (run.level.hasChunkAt(pos) && run.level.getBlockState(pos).is(entry.getValue())) {
                run.level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
        run.markers.clear();
        run.anchors.clear();
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

    private static void updateBossBar(Run run, Mob boss) {
        String phase = switch (run.phase) {
            case OPENING -> "1단계 · 경계 절단";
            case ANCHORS -> "2단계 · 세계 고정점 " + (run.anchorTotal - run.anchors.size()) + "/" + run.anchorTotal;
            case BREAKTHROUGH -> "2단계 · 경계 돌파";
            case FINAL -> "3단계 · 최종 붕괴";
        };
        run.bossBar.setName(Component.literal("§4세계의 경계자 §7· §f" + phase));
        run.bossBar.setProgress(Mth.clamp(boss.getHealth() / Math.max(1.0F, boss.getMaxHealth()), 0.0F, 1.0F));
    }

    private static void complete(Run run, ServerPlayer owner) {
        clearMarkers(run);
        closeBossBar(run);
        if (owner == null) return;
        boolean first = FinalAscensionData.get(run.level.getServer()).complete(owner);
        if (!first) return;

        ItemStack token = new ItemStack(Items.NETHER_STAR);
        token.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("§d승천의 증표"));
        giveOrDrop(owner, token);
        ItemStack mythic = AscensionAffixes.createEliteDrop(run.level.getRandom(), 3);
        AscensionAffixes.awaken(mythic, run.level.getRandom());
        giveOrDrop(owner, mythic);
        owner.giveExperiencePoints(500);

        Component worldMessage = Component.literal("§d[최후의 승천 완료] §f세계의 경계가 열렸습니다. §7월드 정복 권한이 활성화됩니다.");
        for (ServerPlayer player : run.level.getServer().getPlayerList().getPlayers()) player.sendSystemMessage(worldMessage);
        owner.sendSystemMessage(Component.literal("§f개방: §dLv.100 최종 기동 권한 §7· §6Lv.100 최종 건축 권한"));
        owner.sendSystemMessage(Component.literal("§7각성 신화 III 장비 1개와 승천의 증표를 획득했습니다. 수치 티어를 새로 쌓는 대신 기존 숙련의 행동 범위를 확장합니다."));
    }

    private static void fail(Run run, ServerPlayer owner, String reason) {
        Entity boss = run.level.getEntity(run.bossId);
        if (boss != null) boss.discard();
        clearMarkers(run);
        closeBossBar(run);
        if (owner != null) owner.sendSystemMessage(Component.literal("§c[최종 관문 실패] §f" + reason + " §7최후의 승천 1막부터 다시 도전해야 합니다."));
    }

    private static void syncBossBarPlayers(MinecraftServer server, Run run) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean shouldSee = validArenaPlayer(player, run);
            boolean sees = run.bossBar.getPlayers().contains(player);
            if (shouldSee && !sees) run.bossBar.addPlayer(player);
            else if (!shouldSee && sees) run.bossBar.removePlayer(player);
        }
    }

    private static boolean validArenaPlayer(ServerPlayer player, Run run) {
        return player.level() == run.level && player.isAlive() && !player.isSpectator()
                && distanceToCenterSqr(player, run.center) <= PLAYER_RADIUS * PLAYER_RADIUS;
    }

    private static void closeBossBar(Run run) {
        run.bossBar.setVisible(false);
        for (ServerPlayer viewer : List.copyOf(run.bossBar.getPlayers())) run.bossBar.removePlayer(viewer);
    }

    private static Run findRun(Mob boss) {
        String raw = boss.getPersistentData().getStringOr(OWNER_KEY, "");
        try {
            Run run = ACTIVE.get(UUID.fromString(raw));
            return run != null && run.bossId.equals(boss.getUUID()) ? run : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static void removeStaleServerRuns(MinecraftServer server) {
        List<UUID> stale = new ArrayList<>();
        for (Map.Entry<UUID, Run> entry : ACTIVE.entrySet()) {
            if (entry.getValue().level.getServer() == server) continue;
            clearMarkers(entry.getValue());
            closeBossBar(entry.getValue());
            stale.add(entry.getKey());
        }
        for (UUID owner : stale) ACTIVE.remove(owner);
    }

    private static double distanceToCenterSqr(Entity entity, BlockPos center) {
        double dx = entity.getX() - (center.getX() + 0.5D);
        double dy = entity.getY() - (center.getY() + 0.5D);
        double dz = entity.getZ() - (center.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static void particle(ServerLevel level, ParticleOptions particle, double x, double y, double z) {
        level.sendParticles(particle, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static void burstAt(ServerLevel level, BlockPos pos, ParticleOptions particle, int count) {
        level.sendParticles(particle, pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D,
                count, 0.35D, 0.45D, 0.35D, 0.03D);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private enum Phase { OPENING, ANCHORS, BREAKTHROUGH, FINAL }
    private enum AttackPattern { NONE, LINE, RING, MARKED }

    private static final class Run {
        final UUID owner;
        final ServerLevel level;
        final BlockPos center;
        final UUID bossId;
        final long deadline;
        final ServerBossEvent bossBar;
        final Map<BlockPos, Block> markers = new LinkedHashMap<>();
        final Map<BlockPos, Block> anchors = new LinkedHashMap<>();
        Phase phase = Phase.OPENING;
        AttackPattern pendingAttack = AttackPattern.NONE;
        long nextAttackTick;
        long attackExecuteTick;
        int ownerAbsentTicks;
        int attackSerial;
        int anchorTotal;
        Vec3 attackOrigin = Vec3.ZERO;
        Vec3 attackDirection = Vec3.ZERO;
        Vec3 markedPoint = Vec3.ZERO;

        Run(UUID owner, ServerLevel level, BlockPos center, UUID bossId, long deadline) {
            this.owner = owner;
            this.level = level;
            this.center = center.immutable();
            this.bossId = bossId;
            this.deadline = deadline;
            this.nextAttackTick = level.getGameTime() + 30L;
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal("§4세계의 경계자"),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
            this.bossBar.setProgress(1.0F);
        }
    }
}
