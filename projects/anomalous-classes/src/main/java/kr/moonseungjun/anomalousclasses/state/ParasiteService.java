package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import kr.moonseungjun.anomalousclasses.network.ClassNetwork;
import kr.moonseungjun.anomalousclasses.network.payload.ParasiteHostViewPayload;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * One original living host, one controlling player, one persistent server claim.
 * Client packets carry key state only; all host eligibility, BD and movement
 * are computed by the server. The 6 initial adapters use the real vanilla mob.
 */
public final class ParasiteService {
    private static final String MARK = "anomalousclasses_parasite_host";
    private static final ResourceLocation HOST_HP = id("parasite_host_health");
    private static final ResourceLocation HUMAN_HP = id("parasite_human_health");
    private static final int AIM_RANGE = 8;
    private static final int ACTION_INTERVAL = 20;
    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();
    private static final Map<UUID, Motion> MOTIONS = new HashMap<>();
    private static final Map<UUID, Map<UUID, Contribution>> CONTRIBUTIONS = new HashMap<>();
    private static final Map<UUID, Map<String, Deque<Long>>> RECENT_KILLS = new HashMap<>();
    private static MinecraftServer activeServer;

    private static final class Channel {
        UUID target;
        int ticks;
        int lostTicks;
        boolean pressed;
        boolean exit;
        boolean anchored;
    }
    private static final class Contribution {
        float damage;
        long lastTick;
    }
    private static final class Motion {
        int mask;
        float yaw;
        float pitch;
        long lastInput;
        Vec3 lastPosition;
        long lastActivityTick;
        boolean sprintWasDown;
        long dashUntil;
        long dashReadyAt;
    }
    private ParasiteService() {}

    public static boolean current(ServerPlayer player) {
        return player != null && player.getServer() != null
                && ClassProfileSavedData.get(player.getServer()).profile(player.getUUID())
                .currentClass().orElse(null) == ClassId.PARASITE_MORPHER;
    }

    public static void input(ServerPlayer player, int action) {
        if (player.getServer() == null || !player.isAlive()) return;
        if (action == 1) {
            Channel ch = CHANNELS.get(player.getUUID());
            if (ch != null) ch.pressed = false;
            return;
        }
        if (action != 0 || !current(player)) return;
        ParasiteSavedData data = ParasiteSavedData.get(player.getServer());
        if (data.player(player.getUUID()).host() == null
                && !data.mayAttempt(player.getUUID(), clock(player))) {
            message(player, "message.anomalousclasses.parasite.death_lock");
            return;
        }
        Channel previous = CHANNELS.get(player.getUUID());
        if (previous != null && previous.pressed) return;
        Mob target = aimed(player);
        Channel channel = new Channel();
        channel.pressed = true;
        if (target != null && eligible(player, target, true)) {
            if (data.player(player.getUUID()).host() == null
                    && !data.mayAttempt(player.getUUID(), clock(player))) {
                message(player, "message.anomalousclasses.parasite.death_lock");
                return;
            }
            channel.target = target.getUUID();
            channel.anchored = ParasiteAbilityService.takeAnchoring(player);
        } else if (data.player(player.getUUID()).host() != null && target == null) {
            // Unoccupied aim: hold R to exit, rather than an instantaneous dodge.
            channel.exit = true;
        } else {
            message(player, "message.anomalousclasses.parasite.no_target");
            return;
        }
        CHANNELS.put(player.getUUID(), channel);
    }

    public static void movement(ServerPlayer player, int mask, float yaw, float pitch) {
        if (!current(player) || !Float.isFinite(yaw) || !Float.isFinite(pitch)
                || pitch < -90.0F || pitch > 90.0F || mask < 0 || (mask & ~63) != 0) return;
        ParasiteSavedData.State state = ParasiteSavedData.get(player.getServer()).player(player.getUUID());
        if (state.host() == null) return;
        Mob host = loadedHost(player, state.host());
        if (host == null || player.getVehicle() != host) return;
        Motion m = MOTIONS.computeIfAbsent(player.getUUID(), unused -> new Motion());
        m.mask = mask;
        m.yaw = Mth.wrapDegrees(yaw);
        m.pitch = pitch;
        m.lastInput = clock(player);
    }

    /** Validated server snapshot of camera aim, independent of passenger eye height. */
    public static Vec3 viewDirection(ServerPlayer player) {
        Motion motion = MOTIONS.get(player.getUUID());
        if (motion != null && clock(player) - motion.lastInput <= 10L)
            return Vec3.directionFromRotation(motion.pitch, motion.yaw);
        return player.getLookAngle();
    }

    public static Mob activeHost(ServerPlayer player) {
        if (!current(player)) return null;
        UUID id = ParasiteSavedData.get(player.getServer()).player(player.getUUID()).host();
        Mob host = loadedHost(player, id);
        return host != null && host.isAlive() && ownsHost(player, host)
                && player.getVehicle() == host ? host : null;
    }

    public static boolean canPrepareIntrusion(ServerPlayer player) {
        if (!current(player) || !player.isAlive()) return false;
        Mob host = aimed(player);
        return host != null && eligible(player, host, false);
    }

    public static boolean prepareClassChange(ServerPlayer player, ClassId previous) {
        if (previous != ClassId.PARASITE_MORPHER) return true;
        end(player, false);
        CHANNELS.remove(player.getUUID());
        MOTIONS.remove(player.getUUID());
        ParasiteTraitService.reset(player);
        return true;
    }

    public static void onLogin(ServerPlayer player) {
        if (player.getServer() == null) return;
        if (!current(player)) {
            end(player, false);
        } else {
            ParasiteSavedData.State state = ParasiteSavedData.get(player.getServer()).player(player.getUUID());
            Mob host = loadedHost(player, state.host());
            if (host != null && ownsHost(player, host) && player.getVehicle() != host) {
                player.startRiding(host, true);
            }
            // The original host may be in an unloaded chunk during login.
            // Never abandon its saved claim merely because getEntity() returns null.
        }
        applyHumanStats(player);
        syncView(player);
    }

    public static void onLogout(Entity entity) {
        if (!(entity instanceof ServerPlayer player) || player.getServer() == null) return;
        CHANNELS.remove(player.getUUID());
        MOTIONS.remove(player.getUUID());
        CONTRIBUTIONS.remove(player.getUUID());
        RECENT_KILLS.remove(player.getUUID());
        ParasiteSpeciesService.logout(player.getUUID());
        Mob host = loadedHost(player, ParasiteSavedData.get(player.getServer()).player(player.getUUID()).host());
        if (host != null) ParasiteAbilityService.endHost(player, host);
        // Do not forcibly load a chunk. The original host/claim remain saved.
    }

    public static void onRespawn(ServerPlayer player) {
        if (player.getServer() == null) return;
        end(player, false);
        applyHumanStats(player);
        syncView(player);
    }

    /** Cross-dimensional possession cannot strand an owner or leave an active remote body. */
    public static void onChangedDimension(ServerPlayer player) {
        if (player.getServer() == null) return;
        end(player, false);
        CHANNELS.remove(player.getUUID());
        MOTIONS.remove(player.getUUID());
        syncView(player);
    }

    public static void syncView(ServerPlayer player) {
        if (player == null || player.getServer() == null) return;
        Mob host = activeHost(player);
        ClassNetwork.sendToPlayer(player, new ParasiteHostViewPayload(
                host == null ? -1 : host.getId(),
                host == null ? ParasiteHostViewPayload.NONE : host.getUUID()));
    }

    public static void applyHumanStats(ServerPlayer player) {
        if (player.getServer() == null) return;
        float ratio = player.getMaxHealth() <= 0 ? 1.0F : player.getHealth() / player.getMaxHealth();
        AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
        if (instance == null) return;
        instance.removeModifier(HUMAN_HP);
        if (current(player) && ParasiteSavedData.get(player.getServer()).player(player.getUUID()).host() == null) {
            PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
            // Base player health is 20. Do not erase purchased generic graph bonuses.
            instance.addOrUpdateTransientModifier(new AttributeModifier(HUMAN_HP,
                    ParasiteRules.humanMaximum(profile) - 20.0, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.isAlive()) player.setHealth(Math.max(.5F,
                Math.min(player.getMaxHealth(), player.getMaxHealth() * Math.max(0, Math.min(1, ratio)))));
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // First inspect damage addressed to a real inhabited Mob; a successful
        // inherited Enderman dodge cancels this hit before armor or health loss.
        if (event.getEntity() instanceof Mob occupied && occupied.getPersistentData().contains(MARK)
                && occupied.level().getServer() != null) {
            UUID controllingId = hostOwner(occupied);
            ServerPlayer controller = controllingId == null ? null
                    : occupied.level().getServer().getPlayerList().getPlayer(controllingId);
            if (controller != null && ownsHost(controller, occupied)
                    && ParasiteSpeciesService.dodge(controller, occupied, event.getSource())) {
                event.setCanceled(true);
                return;
            }
        }
        if (!(event.getEntity() instanceof ServerPlayer owner) || !current(owner)) return;
        Mob host = loadedHost(owner, ParasiteSavedData.get(owner.getServer()).player(owner.getUUID()).host());
        if (host == null || owner.getVehicle() != host || !host.isAlive()) return;
        // Redirect exactly once even when the host is temporarily invulnerable;
        // the passenger must not become a second damageable body.
        event.setCanceled(true);
        host.hurt(event.getSource(), event.getAmount());
    }

    public static void onDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide() || event.getEntity().level().getServer() == null) return;
        Entity source = event.getSource().getEntity();
        if (source instanceof ServerPlayer attacker && current(attacker)) {
            ParasiteSavedData.State st = ParasiteSavedData.get(attacker.getServer()).player(attacker.getUUID());
            Mob host = loadedHost(attacker, st.host());
            if (host != null && attacker.getVehicle() == host) {
                String species = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()).toString();
                double multiplier = ParasiteRules.damageMultiplier(st.bd(), st.mastery(species),
                        ClassProfileSavedData.get(attacker.getServer()).profile(attacker.getUUID()));
                if (ParasiteAbilityService.awakened(attacker, host)) multiplier *= 1.15;
                float base = event.getNewDamage();
                if (attacker.getMainHandItem().isEmpty()) {
                    // Bare hands use the host's actual natural attack when that species has one.
                    AttributeInstance natural = host.getAttribute(Attributes.ATTACK_DAMAGE);
                    double inherent = natural == null ? 1.5 : natural.getValue();
                    base = Math.max(base, (float) Math.max(1, inherent));
                }
                event.setNewDamage((float) Math.min(2000, base * multiplier));
                trackHit(attacker, event.getEntity(), event.getNewDamage());
                CombatTracker.markOwnedCombat(attacker);
            } else if (st.host() == null) {
                event.setNewDamage(event.getNewDamage() * .4F);
            }
        }
        if (event.getEntity() instanceof ServerPlayer victim && current(victim)
                && event.getNewDamage() > 0) {
            Channel ch = CHANNELS.get(victim.getUUID());
            if (ch != null && !ch.exit)
                ch.ticks = Math.max(0, ch.ticks - (ch.anchored ? 2 : 8));
        }
        if (event.getEntity() instanceof Mob host && host.getPersistentData().contains(MARK)) {
            UUID ownerId = hostOwner(host);
            if (ownerId != null) {
                ServerPlayer owner = host.level().getServer().getPlayerList().getPlayer(ownerId);
                if (owner != null && ownsHost(owner, host)) {
                    if (event.getSource().is(DamageTypes.FALL)) {
                        String species = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()).toString();
                        int slimeMastery = ParasiteSavedData.get(owner.getServer())
                                .player(ownerId).mastery("minecraft:slime");
                        event.setNewDamage(event.getNewDamage()
                                * ParasiteRules.fallDamageFactor(species, slimeMastery));
                    }
                    CombatTracker.markOwnedCombat(owner);
                    ParasiteAbilityService.interruptOnDamage(owner, host, event.getNewDamage());
                    if (ParasiteAbilityService.awakened(owner, host))
                        event.setNewDamage(Math.min(2000.0F, event.getNewDamage() * 1.15F));
                    Channel ch = CHANNELS.get(owner.getUUID());
                    if (ch != null && !ch.exit && event.getNewDamage() > 0)
                        ch.ticks = Math.max(0, ch.ticks - (ch.anchored ? 2 : 8));
                }
            }
        }
    }

    /** Capped, one-time survival milestones supplement movement and combat BD. */
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer owner)
                || owner.isCreative() || owner.isSpectator()) return;
        Mob host = activeHost(owner);
        if (host == null) return;
        var block = event.getState();
        String milestone = null;
        int amount = 0;
        if (block.is(Blocks.IRON_ORE) || block.is(Blocks.DEEPSLATE_IRON_ORE)) {
            milestone = "anomalousclasses:mine_iron";
            amount = 12;
        } else if (block.is(Blocks.DIAMOND_ORE) || block.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
            milestone = "anomalousclasses:mine_diamond";
            amount = 24;
        } else if (block.getBlock() instanceof CropBlock crop && crop.isMaxAge(block)) {
            milestone = "anomalousclasses:harvest_crop";
            amount = 12;
        } else if (block.is(Blocks.DEEPSLATE) && event.getPos().getY() <= -16) {
            milestone = "anomalousclasses:deep_mining";
            amount = 12;
        }
        if (milestone == null) return;
        ParasiteSavedData data = ParasiteSavedData.get(owner.getServer());
        if (data.firstMilestone(owner.getUUID(), milestone)) {
            String species = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()).toString();
            data.reward(owner.getUUID(), species, amount,
                    ClassProfileSavedData.get(owner.getServer()).profile(owner.getUUID()));
        }
    }

    public static void onDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) awardHostCombat(event);
        if (!(event.getEntity() instanceof Mob host) || host.level().isClientSide()
                || !host.getPersistentData().contains(MARK)) return;
        UUID ownerId = hostOwner(host);
        MinecraftServer server = host.level().getServer();
        if (ownerId == null || server == null) return;
        ParasiteSavedData data = ParasiteSavedData.get(server);
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        int lock = owner == null ? 1200 : ParasiteRules.lockoutTicks(
                ClassProfileSavedData.get(server).profile(ownerId));
        if (!data.hostDied(ownerId, host.getUUID(), host.level().getGameTime(), lock)) return;
        if (owner != null) ParasiteAbilityService.endHost(owner, host);
        host.getPersistentData().remove(MARK);
        if (owner != null) {
            owner.stopRiding();
            applyHumanStats(owner);
            if (owner.isAlive()) owner.setHealth(Math.max(1.0F, owner.getMaxHealth() * .75F));
            message(owner, "message.anomalousclasses.parasite.host_died");
            syncView(owner);
        }
        CHANNELS.remove(ownerId);
        MOTIONS.remove(ownerId);
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)
                || !mob.getPersistentData().contains(MARK)) return;
        MinecraftServer server = event.getLevel().getServer();
        UUID ownerId = hostOwner(mob);
        if (server == null || ownerId == null) { restore(mob); return; }
        ParasiteSavedData data = ParasiteSavedData.get(server);
        ParasiteSavedData.State state = data.player(ownerId);
        if (!mob.getUUID().equals(state.host()) || !ownerId.equals(data.ownerOf(mob.getUUID()))) {
            restore(mob);
            return;
        }
        mob.setNoAi(true);
    }

    public static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server != activeServer) {
            CHANNELS.clear();
            MOTIONS.clear();
            CONTRIBUTIONS.clear();
            RECENT_KILLS.clear();
            activeServer = server;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!current(player) || !player.isAlive()) continue;
            ParasiteSavedData data = ParasiteSavedData.get(server);
            ParasiteSavedData.State state = data.player(player.getUUID());
            Mob host = loadedHost(player, state.host());
            if (state.host() != null && (host == null || !ownsHost(player, host))) {
                // Preserve unloaded host identity. R can still perform the deliberate 30-tick
                // human-form exit when recovery is impossible; no chunk forcing.
                Channel ch = CHANNELS.get(player.getUUID());
                if (ch != null && ch.exit) tickChannel(player, ch);
                continue;
            }
            if (host != null) {
                if (!host.isAlive()) {
                    onDeadHost(player, host);
                    continue;
                }
                if (player.getVehicle() != host) {
                    // Sneak/dismount must not bypass the 30-tick deliberate exit.
                    if (!player.startRiding(host, true)) continue;
                    syncView(player);
                }
                moveHost(player, host, state);
                ParasiteSpeciesService.tick(player, host, state);
                ParasiteAbilityService.tick(player, host);
                if (server.getTickCount() % 100 == 0) applyHostStats(player, host);
            }
            Channel ch = CHANNELS.get(player.getUUID());
            if (ch != null) tickChannel(player, ch);
            if (server.getTickCount() % 40 == 0 && state.host() == null) applyHumanStats(player);
        }
    }

    private static void tickChannel(ServerPlayer player, Channel ch) {
        if (!ch.pressed) { CHANNELS.remove(player.getUUID()); return; }
        if (ch.exit) {
            if (CombatTracker.isLocked(player)) { CHANNELS.remove(player.getUUID()); return; }
            if (++ch.ticks >= 30) {
                end(player, false);
                applyHumanStats(player);
                CHANNELS.remove(player.getUUID());
            }
            return;
        }
        Mob target = aimed(player);
        if (target == null || !target.getUUID().equals(ch.target) || !eligible(player, target, false)) {
            ch.ticks = Math.max(0, ch.ticks - (ch.anchored ? 2 : 4));
            if (++ch.lostTicks > (ch.anchored ? 14 : 8)) {
                CHANNELS.remove(player.getUUID());
                return;
            }
            return;
        }
        ch.lostTicks = 0;
        ParasiteSavedData.State state = ParasiteSavedData.get(player.getServer()).player(player.getUUID());
        PlayerClassProfile profile = ClassProfileSavedData.get(player.getServer()).profile(player.getUUID());
        var spec = ParasiteRules.supported(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()));
        if (spec == null) { CHANNELS.remove(player.getUUID()); return; }
        int duration = ParasiteRules.infiltrationTicks(spec, state.bd(), profile);
        if (++ch.ticks >= duration) {
            if (possess(player, target)) message(player, "message.anomalousclasses.parasite.possessed");
            CHANNELS.remove(player.getUUID());
        } else if (ch.ticks % 8 == 0) {
            player.displayClientMessage(Component.translatable(
                    "message.anomalousclasses.parasite.progress",
                    Math.min(100, Math.round(100.0F * ch.ticks / duration))), true);
        }
    }

    private static boolean possess(ServerPlayer player, Mob target) {
        ParasiteSavedData data = ParasiteSavedData.get(player.getServer());
        ParasiteSavedData.State state = data.player(player.getUUID());
        if (!eligible(player, target, false)) return false;
        Mob previous = loadedHost(player, state.host());
        if (target == previous) return false;
        if (!player.startRiding(target, true)) return false;
        String dim = player.serverLevel().dimension().location().toString();
        if (!data.claim(player.getUUID(), target.getUUID(), dim)) {
            player.stopRiding();
            if (previous != null) player.startRiding(previous, true);
            return false;
        }
        if (previous != null) {
            ParasiteAbilityService.endHost(player, previous);
            restore(previous);
        }
        CompoundTag mark = new CompoundTag();
        mark.putUUID("owner", player.getUUID());
        mark.putBoolean("originalNoAi", target.isNoAi());
        target.getPersistentData().put(MARK, mark);
        target.getNavigation().stop();
        target.setNoAi(true);
        data.consumeEmergency(player.getUUID(), clock(player));
        applyHostStats(player, target);
        applyHumanStats(player);
        Motion motion = new Motion();
        motion.yaw = player.getYRot();
        motion.pitch = player.getXRot();
        motion.lastInput = clock(player);
        MOTIONS.put(player.getUUID(), motion);
        syncView(player);
        return true;
    }

    private static void moveHost(ServerPlayer owner, Mob host, ParasiteSavedData.State state) {
        Motion m = MOTIONS.computeIfAbsent(owner.getUUID(), unused -> new Motion());
        long tick = clock(owner);
        if (tick - m.lastInput > 10) m.mask = 0;
        double front = ((m.mask & 1) != 0 ? 1 : 0) - ((m.mask & 2) != 0 ? 1 : 0);
        double sideways = ((m.mask & 8) != 0 ? 1 : 0) - ((m.mask & 4) != 0 ? 1 : 0);
        double length = Math.hypot(front, sideways);
        boolean sprint = (m.mask & 32) != 0 && front > 0;
        if (host.getType() == net.minecraft.world.entity.EntityType.PIG
                && sprint && !m.sprintWasDown && tick >= m.dashReadyAt) {
            // Pig's own short burst is available immediately, without trait inheritance.
            m.dashUntil = tick + 12;
            m.dashReadyAt = tick + 100;
        }
        m.sprintWasDown = sprint;
        if (length > 0) {
            front /= length; sideways /= length;
            double rad = Math.toRadians(m.yaw);
            double dx = (-Math.sin(rad) * front + Math.cos(rad) * sideways);
            double dz = ( Math.cos(rad) * front + Math.sin(rad) * sideways);
            var spec = ParasiteRules.supported(BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()));
            double base = host.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double velocity = Math.min(.32, Math.max(.065, base * 1.45))
                    * spec.speedFactor()
                    * ParasiteRules.movementMultiplier(ClassProfileSavedData.get(owner.getServer())
                    .profile(owner.getUUID()))
                    * (ParasiteAbilityService.awakened(owner, host) ? 1.15 : 1.0);
            if (sprint) velocity *= 1.20;
            if (host.getType() == net.minecraft.world.entity.EntityType.PIG && tick < m.dashUntil)
                velocity *= 1.60;
            // Aquatic bodies and fully-inherited guardian adaptation swim with
            // their actual look pitch; no flight when out of water.
            boolean aquatic = host.isInWaterOrBubble()
                    && ("aquatic".equals(spec.adapter())
                    || ParasiteRules.hasGuardianAdaptation(false,
                    state.mastery("minecraft:guardian")));
            double swimY = aquatic ? Math.max(-.24, Math.min(.24,
                    -Math.sin(Math.toRadians(m.pitch)) * velocity)) : 0.0;
            host.move(MoverType.SELF, new Vec3(dx * Math.min(.50, velocity),
                    swimY, dz * Math.min(.50, velocity)));
            host.setYRot(m.yaw);
            host.setYHeadRot(m.yaw);
            host.setXRot(m.pitch);
        }
        // Jump works from standstill too, independent of horizontal motion.
        // Slime elasticity is innate, and mastery-100 inheritance has no timed cooldown.
        if (host.isInWaterOrBubble()
                && (ParasiteRules.hasGuardianAdaptation(
                host.getType() == net.minecraft.world.entity.EntityType.GUARDIAN,
                state.mastery("minecraft:guardian"))
                || "aquatic".equals(ParasiteRules.supported(
                BuiltInRegistries.ENTITY_TYPE.getKey(host.getType())).adapter()))
                && (m.mask & 16) != 0) {
            Vec3 velocity = host.getDeltaMovement();
            host.setDeltaMovement(velocity.x, Math.max(velocity.y, 0.18), velocity.z);
        }
        if ((m.mask & 16) != 0 && host.onGround()) {
            String species = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()).toString();
            double impulse = ParasiteRules.jumpImpulse(species, state);
            Vec3 velocity = host.getDeltaMovement();
            host.setDeltaMovement(velocity.x, Math.max(velocity.y, impulse), velocity.z);
        }
        // Wall climbing is environmental: jump-only works even without horizontal travel.
        if ((host.getType() == net.minecraft.world.entity.EntityType.SPIDER
                || host.getType() == net.minecraft.world.entity.EntityType.CAVE_SPIDER
                || state.mastery("minecraft:spider") >= 100
                || state.mastery("minecraft:cave_spider") >= 100)
                && host.horizontalCollision && ((m.mask & 16) != 0 || front > 0))
            host.setDeltaMovement(host.getDeltaMovement().x, .18, host.getDeltaMovement().z);
        if (tick % ACTION_INTERVAL == 0) {
            Vec3 pos = host.position();
            if (m.lastPosition != null && m.lastPosition.distanceToSqr(pos) > 2.25
                    && tick - m.lastActivityTick >= 20) {
                m.lastActivityTick = tick;
                PlayerClassProfile profile = ClassProfileSavedData.get(owner.getServer()).profile(owner.getUUID());
                String species = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()).toString();
                if (ParasiteSavedData.get(owner.getServer()).recordActiveTicks(owner.getUUID(), 20, profile))
                    ParasiteSavedData.get(owner.getServer()).reward(owner.getUUID(), species, 4, profile);
                String biome = owner.serverLevel().dimension().location() + "/" +
                        host.level().getBiome(host.blockPosition()).unwrapKey()
                        .map(key -> key.location().toString()).orElse("unknown");
                if (!biome.endsWith("/unknown") && ParasiteSavedData.get(owner.getServer())
                        .firstBiome(owner.getUUID(), toBiomeKey(biome))) {
                    ParasiteSavedData.get(owner.getServer()).reward(owner.getUUID(), species, 12, profile);
                }
            }
            m.lastPosition = pos;
        }
    }

    private static String toBiomeKey(String composite) {
        // Valid ResourceLocation shape; dimension is included to prevent repeat farming.
        return "anomalousclasses:" + composite.replace(':','_').replace('/','_');
    }

    private static boolean eligible(ServerPlayer player, Mob host, boolean announce) {
        if (host == null || !host.isAlive() || host.isRemoved() || player.level() != host.level()
                || host.isNoAi() || host.isPassenger() || host.isVehicle()
                || player.distanceToSqr(host) > AIM_RANGE * AIM_RANGE
                || player.isAlliedTo(host) || host instanceof net.minecraft.world.entity.npc.AbstractVillager
                || (host instanceof TamableAnimal animal && animal.isTame())
                || (host instanceof AbstractHorse horse && horse.isTamed())
                || host.getPersistentData().contains(MARK)
                || MentalDominatorSavedData.get(player.getServer()).controlled(host.getUUID()) != null
                || host instanceof kr.moonseungjun.anomalousclasses.entity.PuppetEntity)
            return false;
        ParasiteSavedData data = ParasiteSavedData.get(player.getServer());
        if (data.ownerOf(host.getUUID()) != null) return false;
        var spec = ParasiteRules.supported(BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()));
        if (spec == null) {
            if (announce) message(player, "message.anomalousclasses.parasite.unsupported");
            return false;
        }
        if (data.player(player.getUUID()).bd() < spec.requiredBd()) {
            if (announce) message(player, "message.anomalousclasses.parasite.insufficient_bd");
            return false;
        }
        if (data.player(player.getUUID()).host() == null
                && !data.mayAttempt(player.getUUID(), clock(player))) return false;
        return true;
    }

    private static Mob aimed(ServerPlayer player) {
        // The player can reach a new host through the senses of the currently
        // inhabited one, rather than raycasting above its body from the passenger.
        Mob source = activeHost(player);
        Vec3 eye = source == null ? player.getEyePosition() : source.getEyePosition();
        Vec3 look = viewDirection(player);
        Vec3 end = eye.add(look.scale(AIM_RANGE));
        AABB area = (source == null ? player.getBoundingBox() : source.getBoundingBox())
                .expandTowards(look.scale(AIM_RANGE)).inflate(1.0);
        Mob best = null;
        double bestDist = AIM_RANGE * AIM_RANGE;
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, area,
                candidate -> candidate.isAlive() && candidate.isPickable() && candidate != player.getVehicle())) {
            var hit = mob.getBoundingBox().inflate(.25).clip(eye, end);
            if (hit.isEmpty()) continue;
            double d = eye.distanceToSqr(hit.get());
            if (d < bestDist) { best = mob; bestDist = d; }
        }
        if (best == null) return null;
        BlockHitResult wall = player.level().clip(new ClipContext(eye,end,
                ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        return wall.getType() != HitResult.Type.MISS
                && eye.distanceToSqr(wall.getLocation()) < bestDist ? null : best;
    }

    private static boolean ownsHost(ServerPlayer player, Mob host) {
        if (player == null || host == null || !host.getPersistentData().contains(MARK)) return false;
        UUID owner = hostOwner(host);
        ParasiteSavedData data = ParasiteSavedData.get(player.getServer());
        return player.getUUID().equals(owner)
                && player.getUUID().equals(data.ownerOf(host.getUUID()))
                && host.getUUID().equals(data.player(owner).host());
    }

    private static UUID hostOwner(Mob host) {
        CompoundTag tag = host.getPersistentData().getCompound(MARK);
        return tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }
    private static Mob loadedHost(ServerPlayer owner, UUID id) {
        if (id == null || owner == null) return null;
        Entity entity = owner.serverLevel().getEntity(id);
        return entity instanceof Mob mob ? mob : null;
    }
    private static Mob loadedSavedHost(ServerPlayer owner, UUID id) {
        if (id == null || owner == null || owner.getServer() == null) return null;
        ParasiteSavedData.State state = ParasiteSavedData.get(owner.getServer()).player(owner.getUUID());
        ResourceLocation key = ResourceLocation.tryParse(state.dimension());
        if (key == null) return null;
        ServerLevel level = owner.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, key));
        Entity loaded = level == null ? null : level.getEntity(id); // no chunk forcing
        return loaded instanceof Mob mob ? mob : null;
    }

    private static void applyHostStats(ServerPlayer owner, Mob host) {
        if (!ownsHost(owner,host)) return;
        var data = ParasiteSavedData.get(owner.getServer()).player(owner.getUUID());
        PlayerClassProfile profile = ClassProfileSavedData.get(owner.getServer()).profile(owner.getUUID());
        String species = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()).toString();
        AttributeInstance hp = host.getAttribute(Attributes.MAX_HEALTH);
        if (hp == null) return;
        float ratio = host.getMaxHealth() <= 0 ? 1.0F : host.getHealth() / host.getMaxHealth();
        hp.removeModifier(HOST_HP);
        hp.addOrUpdateTransientModifier(new AttributeModifier(HOST_HP,
                ParasiteRules.healthMultiplier(data.bd(), data.mastery(species), profile) - 1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (host.isAlive()) host.setHealth(Math.max(.5F, host.getMaxHealth() * ratio));
    }
    private static void restore(Mob host) {
        if (host == null) return;
        CompoundTag mark = host.getPersistentData().getCompound(MARK);
        AttributeInstance hp = host.getAttribute(Attributes.MAX_HEALTH);
        float ratio = host.getMaxHealth() <= 0 ? 1.0F : host.getHealth() / host.getMaxHealth();
        if (hp != null) hp.removeModifier(HOST_HP);
        if (host.isAlive()) host.setHealth(Math.max(.5F, host.getMaxHealth() * ratio));
        if (host.getPersistentData().contains(MARK)) host.setNoAi(mark.getBoolean("originalNoAi"));
        host.getPersistentData().remove(MARK);
    }
    private static void end(ServerPlayer owner, boolean death) {
        if (owner == null || owner.getServer() == null) return;
        ParasiteSavedData data = ParasiteSavedData.get(owner.getServer());
        UUID hostId = data.player(owner.getUUID()).host();
        if (hostId == null) return;
        Mob host = loadedSavedHost(owner, hostId);
        if (owner.getVehicle() != null) owner.stopRiding();
        if (host != null) {
            ParasiteAbilityService.endHost(owner, host);
            restore(host);
        }
        data.release(owner.getUUID(), hostId);
        if (!death) applyHumanStats(owner);
        syncView(owner);
    }
    private static void onDeadHost(ServerPlayer owner, Mob host) {
        ParasiteSavedData data = ParasiteSavedData.get(owner.getServer());
        if (!data.hostDied(owner.getUUID(), host.getUUID(), clock(owner),
                ParasiteRules.lockoutTicks(ClassProfileSavedData.get(owner.getServer()).profile(owner.getUUID())))) return;
        owner.stopRiding();
        ParasiteAbilityService.endHost(owner, host);
        host.getPersistentData().remove(MARK);
        applyHumanStats(owner);
        if (owner.isAlive()) owner.setHealth(Math.max(1.0F, owner.getMaxHealth() * .75F));
        CHANNELS.remove(owner.getUUID());
        MOTIONS.remove(owner.getUUID());
        message(owner, "message.anomalousclasses.parasite.host_died");
        syncView(owner);
    }

    private static void trackHit(ServerPlayer owner, LivingEntity victim, float damage) {
        if (!(victim instanceof Monster) || !victim.isAlive()
                || owner.isAlliedTo(victim) || damage <= 0 || !Float.isFinite(damage)) return;
        Map<UUID, Contribution> hits = CONTRIBUTIONS.computeIfAbsent(
                owner.getUUID(), unused -> new HashMap<>());
        long now = clock(owner);
        if (hits.size() >= 64) hits.entrySet().removeIf(e -> now - e.getValue().lastTick > 200);
        if (hits.size() >= 64 && !hits.containsKey(victim.getUUID())) return;
        Contribution hit = hits.computeIfAbsent(victim.getUUID(), unused -> new Contribution());
        hit.damage = Math.min(victim.getMaxHealth(), hit.damage + damage);
        hit.lastTick = now;
    }

    private static void awardHostCombat(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Monster target)
                || !(event.getSource().getEntity() instanceof ServerPlayer owner)
                || !current(owner) || activeHost(owner) == null) return;
        Map<UUID, Contribution> hits = CONTRIBUTIONS.get(owner.getUUID());
        if (hits == null) return;
        Contribution hit = hits.remove(target.getUUID());
        if (hit == null || clock(owner) - hit.lastTick > 100
                || hit.damage < Math.min(8.0F, target.getMaxHealth() * .25F)) return;
        String enemy = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
        Map<String, Deque<Long>> speciesHistory = RECENT_KILLS.computeIfAbsent(
                owner.getUUID(), unused -> new HashMap<>());
        Deque<Long> recent = speciesHistory.computeIfAbsent(enemy, unused -> new ArrayDeque<>());
        long now = clock(owner);
        while (!recent.isEmpty() && now - recent.peekFirst() > 1200L) recent.removeFirst();
        int repeated = recent.size();
        recent.addLast(now);
        if (repeated >= 6) return; // Dense mob farms cannot scale BD without bound.
        int base = Math.max(3, Math.min(60, (int) Math.ceil(target.getMaxHealth() / 4.0)));
        int earned = repeated >= 3 ? Math.max(1, base / 3) : base;
        Mob host = activeHost(owner);
        String species = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()).toString();
        ParasiteSavedData.get(owner.getServer()).reward(owner.getUUID(), species, earned,
                ClassProfileSavedData.get(owner.getServer()).profile(owner.getUUID()));
    }
    private static long clock(ServerPlayer player) {
        return player.serverLevel().getGameTime();
    }
    private static ResourceLocation id(String suffix) {
        return ResourceLocation.fromNamespaceAndPath(AnomalousClasses.MOD_ID, suffix);
    }
    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
