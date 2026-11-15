package kr.moonseungjun.anomalousclasses.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
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
    private static MinecraftServer activeServer;

    private static final class Channel {
        UUID target;
        int ticks;
        int lostTicks;
        boolean pressed;
        boolean exit;
    }
    private static final class Motion {
        int mask;
        float yaw;
        long lastInput;
        Vec3 lastPosition;
        long lastActivityTick;
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
        } else if (data.player(player.getUUID()).host() != null && target == null) {
            // Unoccupied aim: hold R to exit, rather than an instantaneous dodge.
            channel.exit = true;
        } else {
            message(player, "message.anomalousclasses.parasite.no_target");
            return;
        }
        CHANNELS.put(player.getUUID(), channel);
    }

    public static void movement(ServerPlayer player, int mask, float yaw) {
        if (!current(player) || !Float.isFinite(yaw) || mask < 0 || (mask & ~31) != 0) return;
        ParasiteSavedData.State state = ParasiteSavedData.get(player.getServer()).player(player.getUUID());
        if (state.host() == null) return;
        Mob host = loadedHost(player, state.host());
        if (host == null || player.getVehicle() != host) return;
        Motion m = MOTIONS.computeIfAbsent(player.getUUID(), unused -> new Motion());
        m.mask = mask;
        m.yaw = yaw;
        m.lastInput = clock(player);
    }

    public static boolean prepareClassChange(ServerPlayer player, ClassId previous) {
        if (previous != ClassId.PARASITE_MORPHER) return true;
        end(player, false);
        CHANNELS.remove(player.getUUID());
        MOTIONS.remove(player.getUUID());
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
            } else if (state.host() != null && host == null) {
                // Host has no loaded entity in the player's dimension: do not soft-lock on relog.
                ParasiteSavedData.get(player.getServer()).release(player.getUUID(), state.host());
            }
        }
        applyHumanStats(player);
    }

    public static void onLogout(Entity entity) {
        if (!(entity instanceof ServerPlayer player) || player.getServer() == null) return;
        CHANNELS.remove(player.getUUID());
        MOTIONS.remove(player.getUUID());
        // Do not forcibly load a chunk. The original host/claim remain saved.
    }

    public static void onRespawn(ServerPlayer player) {
        if (player.getServer() == null) return;
        end(player, false);
        applyHumanStats(player);
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
            instance.addOrReplaceTransientModifier(new AttributeModifier(HUMAN_HP,
                    ParasiteRules.humanMaximum(profile) - 20.0, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.isAlive()) player.setHealth(Math.max(.5F,
                Math.min(player.getMaxHealth(), player.getMaxHealth() * Math.max(0, Math.min(1, ratio)))));
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner) || !current(owner)) return;
        Mob host = loadedHost(owner, ParasiteSavedData.get(owner.getServer()).player(owner.getUUID()).host());
        if (host == null || owner.getVehicle() != host || !host.isAlive()) return;
        // Only one body takes the incoming attack; the passenger cannot absorb a second copy.
        if (host.hurt(event.getSource(), event.getAmount())) event.setCanceled(true);
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
                event.setNewDamage((float) Math.min(2000, event.getNewDamage() * multiplier));
                CombatTracker.markOwnedCombat(attacker);
            } else if (st.host() == null) {
                event.setNewDamage(event.getNewDamage() * .4F);
            }
        }
        if (event.getEntity() instanceof Mob host && host.getPersistentData().contains(MARK)) {
            UUID ownerId = hostOwner(host);
            if (ownerId != null) {
                ServerPlayer owner = host.level().getServer().getPlayerList().getPlayer(ownerId);
                if (owner != null && ownsHost(owner, host)) CombatTracker.markOwnedCombat(owner);
            }
        }
    }

    public static void onDeath(LivingDeathEvent event) {
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
        host.getPersistentData().remove(MARK);
        if (owner != null) {
            owner.stopRiding();
            applyHumanStats(owner);
            if (owner.isAlive()) owner.setHealth(Math.max(1.0F, owner.getMaxHealth() * .75F));
            message(owner, "message.anomalousclasses.parasite.host_died");
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
            activeServer = server;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!current(player) || !player.isAlive()) continue;
            ParasiteSavedData data = ParasiteSavedData.get(server);
            ParasiteSavedData.State state = data.player(player.getUUID());
            Mob host = loadedHost(player, state.host());
            if (state.host() != null && (host == null || !ownsHost(player, host))) {
                // If the chunk is unloaded, preserve the claim and wait. No teleport or chunk forcing.
                continue;
            }
            if (host != null) {
                if (!host.isAlive()) {
                    onDeadHost(player, host);
                    continue;
                }
                if (player.getVehicle() != host) {
                    // Do not allow dismount to bypass the combat escape cast.
                    if (CombatTracker.isLocked(player)) player.startRiding(host, true);
                    else { end(player, false); applyHumanStats(player); continue; }
                }
                moveHost(player, host, state);
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
            if (++ch.lostTicks > 8) { CHANNELS.remove(player.getUUID()); return; }
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
        if (previous != null) restore(previous);
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
        MOTIONS.put(player.getUUID(), motion);
        return true;
    }

    private static void moveHost(ServerPlayer owner, Mob host, ParasiteSavedData.State state) {
        Motion m = MOTIONS.computeIfAbsent(owner.getUUID(), unused -> new Motion());
        long tick = clock(owner);
        if (tick - m.lastInput > 10) m.mask = 0;
        double front = ((m.mask & 1) != 0 ? 1 : 0) - ((m.mask & 2) != 0 ? 1 : 0);
        double sideways = ((m.mask & 8) != 0 ? 1 : 0) - ((m.mask & 4) != 0 ? 1 : 0);
        double length = Math.hypot(front, sideways);
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
                    .profile(owner.getUUID()));
            host.move(MoverType.SELF, new Vec3(dx * velocity, 0, dz * velocity));
            host.setYRot(m.yaw);
            host.setYHeadRot(m.yaw);
            if ((m.mask & 16) != 0 && host.onGround())
                host.setDeltaMovement(host.getDeltaMovement().add(0, .42, 0));
            // Spider's own wall mobility is available without another unlock or cooldown.
            if (host.getType() == net.minecraft.world.entity.EntityType.SPIDER
                    && host.horizontalCollision && ((m.mask & 16) != 0 || front > 0))
                host.setDeltaMovement(host.getDeltaMovement().x, .18, host.getDeltaMovement().z);
        }
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
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getLookAngle().scale(AIM_RANGE));
        AABB area = player.getBoundingBox().expandTowards(player.getLookAngle().scale(AIM_RANGE)).inflate(1.0);
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
    private static void applyHostStats(ServerPlayer owner, Mob host) {
        if (!ownsHost(owner,host)) return;
        var data = ParasiteSavedData.get(owner.getServer()).player(owner.getUUID());
        PlayerClassProfile profile = ClassProfileSavedData.get(owner.getServer()).profile(owner.getUUID());
        String species = BuiltInRegistries.ENTITY_TYPE.getKey(host.getType()).toString();
        AttributeInstance hp = host.getAttribute(Attributes.MAX_HEALTH);
        if (hp == null) return;
        float ratio = host.getMaxHealth() <= 0 ? 1.0F : host.getHealth() / host.getMaxHealth();
        hp.removeModifier(HOST_HP);
        hp.addOrReplaceTransientModifier(new AttributeModifier(HOST_HP,
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
        Mob host = loadedHost(owner, hostId);
        if (owner.getVehicle() != null) owner.stopRiding();
        if (host != null) restore(host);
        data.release(owner.getUUID(), hostId);
        if (!death) applyHumanStats(owner);
    }
    private static void onDeadHost(ServerPlayer owner, Mob host) {
        ParasiteSavedData data = ParasiteSavedData.get(owner.getServer());
        if (!data.hostDied(owner.getUUID(), host.getUUID(), clock(owner),
                ParasiteRules.lockoutTicks(ClassProfileSavedData.get(owner.getServer()).profile(owner.getUUID())))) return;
        owner.stopRiding();
        host.getPersistentData().remove(MARK);
        applyHumanStats(owner);
        if (owner.isAlive()) owner.setHealth(Math.max(1.0F, owner.getMaxHealth() * .75F));
        CHANNELS.remove(owner.getUUID());
        MOTIONS.remove(owner.getUUID());
        message(owner, "message.anomalousclasses.parasite.host_died");
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
