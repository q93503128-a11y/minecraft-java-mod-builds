package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenance;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import dev.moonseungjun.augmentationsurvival.augment.combat.ExpansionTuning;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.mixin.ItemDisplayAugmentAccessor;
import dev.moonseungjun.augmentationsurvival.state.AugmentationAttachments;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import dev.moonseungjun.augmentationsurvival.state.ShieldThrowEscrowState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.Vec3;

/** Physical Shield Throw flight, ricochet, return, item escrow and replay authority. */
public final class ShieldThrowFlightService {
    private static final String SHIELD_THROW_ID = id("shield_throw");
    private static final String ABILITY_HASTE_ID = id("ability_haste");
    private static final String DOUBLE_CHARGE_ID = id("double_charge");
    private static final String REPLAY_ID = id("replay");
    private static final double FLIGHT_SPEED = 1.10D;
    private static final double HIT_RADIUS = 0.70D;
    private static final int MAX_FLIGHT_TICKS = 120;

    private static final Map<UUID, OwnerRuntime> RUNTIME = new HashMap<>();
    private static boolean initialized;

    private ShieldThrowFlightService() { }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
        ServerPlayerEvents.JOIN.register(player -> {
            RUNTIME.remove(player.getUUID());
            recoverEscrow(player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            RUNTIME.remove(newPlayer.getUUID());
            recoverEscrow(newPlayer);
        });
        ServerPlayerEvents.LEAVE.register(player -> recoverAll(player));
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) recoverAll(player);
            return true;
        });
        ServerTickEvents.END_SERVER_TICK.register(ShieldThrowFlightService::tick);
    }

    /** Replaces the former instant damage loop. */
    public static boolean tryUse(ServerPlayer player, double efficiency, boolean manualUse) {
        if (player.isDeadOrDying() || !(efficiency > 0.0D)) return false;
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        int level = boundedLevel(state, SHIELD_THROW_ID);
        if (level <= 0) return false;

        OwnerRuntime ownerRuntime = RUNTIME.computeIfAbsent(player.getUUID(), ignored -> new OwnerRuntime());
        if (manualUse && ownerRuntime.hasPhysicalFlight()) return false;

        ShieldContext context = resolveShieldContext(player, manualUse);
        if (context == null) return false;
        AdvancedActiveTuning.Snapshot tuning = AdvancedActiveTuning.current();
        List<UUID> targets = findTargets(
                player,
                tuning.shieldThrowRange(level),
                tuning.shieldThrowTargets(level)
        );
        if (targets.isEmpty()) return false;

        Reservation reservation = null;
        if (manualUse) {
            reservation = reserve(
                    player,
                    state,
                    tuning.shieldThrowCooldownTicks(level)
            );
            if (reservation == null) return false;
        }

        float damage = (float) Math.min(
                Float.MAX_VALUE,
                context.rawDamage * tuning.shieldThrowDamageFraction(level) * efficiency
        );
        if (!(damage > 0.0F) || !Float.isFinite(damage)) return false;

        Display.ItemDisplay display = spawnDisplay(player, context.shield);
        if (display == null) return false;

        if (manualUse) {
            player.setAttached(
                    AugmentationAttachments.SHIELD_THROW_ESCROW,
                    new ShieldThrowEscrowState(context.shield, context.hand == InteractionHand.OFF_HAND)
            );
            player.setItemInHand(context.hand, ItemStack.EMPTY);
            player.inventoryMenu.broadcastChanges();
        }

        ownerRuntime.flights.add(new Flight(
                display,
                manualUse,
                context.hand,
                List.copyOf(targets),
                damage
        ));

        if (manualUse) {
            commitReservation(player, reservation);
            if (boundedLevel(AugmentationPlayerStateService.state(player), REPLAY_ID) > 0) {
                ownerRuntime.replays.add(new ReplayRequest(
                        player.level().getGameTime() + ExpansionTuning.current().replayDelayTicks(),
                        ExpansionTuning.current().replayEfficiency()
                ));
            }
        }
        return true;
    }

    public static int activeFlightCount(ServerPlayer player) {
        OwnerRuntime runtime = RUNTIME.get(player.getUUID());
        return runtime == null ? 0 : runtime.flights.size();
    }

    private static Display.ItemDisplay spawnDisplay(ServerPlayer player, ItemStack shield) {
        ServerLevel level = player.level();
        Display.ItemDisplay display = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
        Vec3 start = player.getEyePosition().add(player.getLookAngle().normalize().scale(0.75D));
        display.setPos(start.x, start.y - 0.25D, start.z);
        display.setNoGravity(true);
        display.setXRot(90.0F);
        display.setYRot(player.getYRot());
        ItemDisplayAugmentAccessor accessor = (ItemDisplayAugmentAccessor) display;
        accessor.augmentationSurvival$setItemStack(shield.copy());
        accessor.augmentationSurvival$setItemTransform(ItemDisplayContext.FIXED);
        if (!level.addFreshEntity(display)) {
            display.discard();
            return null;
        }
        level.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW,
                SoundSource.PLAYERS,
                0.65F,
                1.35F
        );
        return display;
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, OwnerRuntime>> ownerIterator = RUNTIME.entrySet().iterator();
        while (ownerIterator.hasNext()) {
            Map.Entry<UUID, OwnerRuntime> entry = ownerIterator.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            OwnerRuntime runtime = entry.getValue();
            if (owner == null) {
                discardDisplays(runtime);
                ownerIterator.remove();
                continue;
            }

            long now = owner.level().getGameTime();
            Iterator<ReplayRequest> replayIterator = runtime.replays.iterator();
            while (replayIterator.hasNext()) {
                ReplayRequest replay = replayIterator.next();
                if (replay.dueTick > now) continue;
                replayIterator.remove();
                tryUse(owner, replay.efficiency, false);
            }

            Iterator<Flight> flightIterator = runtime.flights.iterator();
            while (flightIterator.hasNext()) {
                Flight flight = flightIterator.next();
                if (!tickFlight(owner, flight)) {
                    finishFlight(owner, flight);
                    flightIterator.remove();
                }
            }

            if (runtime.flights.isEmpty() && runtime.replays.isEmpty()) {
                ownerIterator.remove();
            }
        }
    }

    private static boolean tickFlight(ServerPlayer owner, Flight flight) {
        Display.ItemDisplay display = flight.display;
        if (display.isRemoved()) return false;
        if (display.level() != owner.level()) return false;
        flight.ageTicks++;
        display.setXRot(90.0F);
        display.setYRot(display.getYRot() + 42.0F);

        if (flight.ageTicks >= MAX_FLIGHT_TICKS) flight.returning = true;
        if (!flight.returning) {
            LivingEntity target = currentTarget(owner, flight);
            if (target == null) {
                flight.returning = true;
            } else {
                Vec3 goal = target.getBoundingBox().getCenter();
                if (advance(display, goal, HIT_RADIUS)) {
                    hit(owner, target, flight.damage);
                    flight.targetIndex++;
                    if (flight.targetIndex >= flight.targetIds.size()) flight.returning = true;
                }
            }
        }

        if (flight.returning) {
            Vec3 goal = owner.position().add(0.0D, owner.getBbHeight() * 0.62D, 0.0D);
            if (advance(display, goal, 0.55D)) return false;
        }
        return true;
    }

    private static boolean advance(Display.ItemDisplay display, Vec3 goal, double arrivalRadius) {
        Vec3 delta = goal.subtract(display.position());
        double distance = delta.length();
        if (distance <= FLIGHT_SPEED + arrivalRadius) {
            display.setPos(goal.x, goal.y, goal.z);
            return true;
        }
        Vec3 next = display.position().add(delta.scale(FLIGHT_SPEED / distance));
        display.setPos(next.x, next.y, next.z);
        return false;
    }

    private static LivingEntity currentTarget(ServerPlayer owner, Flight flight) {
        ServerLevel level = owner.level();
        while (flight.targetIndex < flight.targetIds.size()) {
            Entity entity = level.getEntity(flight.targetIds.get(flight.targetIndex));
            if (entity instanceof LivingEntity living && !living.isDeadOrDying()) return living;
            flight.targetIndex++;
        }
        return null;
    }

    private static void hit(ServerPlayer owner, LivingEntity target, float damage) {
        DamageSource source = owner.damageSources().playerAttack(owner);
        CombatProvenanceContext.runAs(
                CombatProvenance.ACTIVE_SPECIAL,
                () -> target.hurtServer(owner.level(), source, damage)
        );
        owner.level().playSound(
                null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.SHIELD_BLOCK,
                SoundSource.PLAYERS,
                0.75F,
                1.25F
        );
    }

    private static void finishFlight(ServerPlayer owner, Flight flight) {
        if (!flight.display.isRemoved()) flight.display.discard();
        if (flight.physical) recoverEscrow(owner);
    }

    private static List<UUID> findTargets(ServerPlayer player, double range, int limit) {
        double rangeSquared = range * range;
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range),
                candidate -> CompanionAugmentService.isHostileTarget(player, candidate)
                        && player.distanceToSqr(candidate) <= rangeSquared
                        && player.hasLineOfSight(candidate)
        );
        if (candidates.isEmpty()) return List.of();

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity primary = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Vec3 delta = candidate.getBoundingBox().getCenter().subtract(eye);
            double along = delta.dot(look);
            if (along <= 0.0D || along > range) continue;
            double perpendicularSq = Math.max(0.0D, delta.lengthSqr() - along * along);
            double aimRadius = Math.max(1.0D, candidate.getBbWidth());
            if (perpendicularSq > aimRadius * aimRadius) continue;
            double score = perpendicularSq + along * 1.0E-4D;
            if (score < best) {
                best = score;
                primary = candidate;
            }
        }
        if (primary == null) return List.of();

        List<LivingEntity> ordered = new ArrayList<>();
        ordered.add(primary);
        LivingEntity cursor = primary;
        while (ordered.size() < limit) {
            LivingEntity from = cursor;
            LivingEntity next = candidates.stream()
                    .filter(candidate -> !ordered.contains(candidate))
                    .min(Comparator.comparingDouble(from::distanceToSqr))
                    .orElse(null);
            if (next == null) break;
            ordered.add(next);
            cursor = next;
        }
        return ordered.stream().map(Entity::getUUID).toList();
    }

    private static ShieldContext resolveShieldContext(ServerPlayer player, boolean manualUse) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (off.is(Items.SHIELD)) {
            if (main.isEmpty()) return null;
            return new ShieldContext(
                    off.copy(),
                    InteractionHand.OFF_HAND,
                    player.getAttributeValue(Attributes.ATTACK_DAMAGE)
            );
        }
        if (main.is(Items.SHIELD)) {
            if (off.isEmpty()) return null;
            ItemAttributeModifiers modifiers = off.getOrDefault(
                    DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.EMPTY
            );
            return new ShieldContext(
                    main.copy(),
                    InteractionHand.MAIN_HAND,
                    modifiers.compute(Attributes.ATTACK_DAMAGE, 1.0D, EquipmentSlot.MAINHAND)
            );
        }
        if (!manualUse) {
            ShieldThrowEscrowState escrow = player.getAttachedOrSet(
                    AugmentationAttachments.SHIELD_THROW_ESCROW,
                    ShieldThrowEscrowState.empty()
            );
            if (escrow.active()) {
                ItemStack weapon = escrow.offhand() ? main : off;
                if (weapon.isEmpty()) return null;
                double rawDamage;
                if (escrow.offhand()) {
                    rawDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
                } else {
                    ItemAttributeModifiers modifiers = weapon.getOrDefault(
                            DataComponents.ATTRIBUTE_MODIFIERS,
                            ItemAttributeModifiers.EMPTY
                    );
                    rawDamage = modifiers.compute(Attributes.ATTACK_DAMAGE, 1.0D, EquipmentSlot.MAINHAND);
                }
                return new ShieldContext(
                        escrow.shield(),
                        escrow.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                        rawDamage
                );
            }
        }
        return null;
    }

    private static Reservation reserve(ServerPlayer player, AugmentationPlayerState state, int baseCooldownTicks) {
        long now = player.level().getGameTime();
        int cooldown = baseCooldownTicks;
        if (boundedLevel(state, ABILITY_HASTE_ID) > 0) {
            cooldown = Math.max(
                    1,
                    (int) Math.round(baseCooldownTicks
                            * (1.0D - ExpansionTuning.current().abilityHasteReduction()))
            );
        }
        if (boundedLevel(state, DOUBLE_CHARGE_ID) <= 0) {
            return state.cooldownReadyTick(SHIELD_THROW_ID) > now
                    ? null
                    : new Reservation(SHIELD_THROW_ID, now + cooldown);
        }
        String first = chargeKey(0);
        String second = chargeKey(1);
        long firstReady = state.cooldownReadyTick(first);
        long secondReady = state.cooldownReadyTick(second);
        String selected;
        if (firstReady <= now) selected = first;
        else if (secondReady <= now) selected = second;
        else return null;
        long queueTail = Math.max(now, Math.max(firstReady, secondReady));
        return new Reservation(selected, queueTail + cooldown);
    }

    private static void commitReservation(ServerPlayer player, Reservation reservation) {
        AugmentationPlayerState latest = AugmentationPlayerStateService.state(player);
        AugmentationPlayerStateService.replace(
                player,
                latest.withCooldownReadyTick(reservation.key, reservation.readyTick)
        );
    }

    private static void recoverAll(ServerPlayer player) {
        OwnerRuntime runtime = RUNTIME.remove(player.getUUID());
        if (runtime != null) discardDisplays(runtime);
        recoverEscrow(player);
    }

    private static void discardDisplays(OwnerRuntime runtime) {
        for (Flight flight : runtime.flights) {
            if (!flight.display.isRemoved()) flight.display.discard();
        }
        runtime.flights.clear();
        runtime.replays.clear();
    }

    private static void recoverEscrow(ServerPlayer player) {
        ShieldThrowEscrowState escrow = player.getAttachedOrSet(
                AugmentationAttachments.SHIELD_THROW_ESCROW,
                ShieldThrowEscrowState.empty()
        );
        if (!escrow.active()) return;

        ItemStack shield = escrow.shield().copy();
        InteractionHand hand = escrow.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, shield);
        } else if (!player.getInventory().add(shield)) {
            player.drop(shield, false);
        }
        player.setAttached(AugmentationAttachments.SHIELD_THROW_ESCROW, ShieldThrowEscrowState.empty());
        player.inventoryMenu.broadcastChanges();
    }

    private static int boundedLevel(AugmentationPlayerState state, String augmentId) {
        int level = state.level(augmentId);
        return level <= 0 ? 0 : Math.min(level, AugmentCatalog.require(augmentId).maxLevel());
    }

    private static String chargeKey(int index) {
        return SHIELD_THROW_ID + "#charge" + index;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    private record ShieldContext(ItemStack shield, InteractionHand hand, double rawDamage) { }
    private record Reservation(String key, long readyTick) { }
    private record ReplayRequest(long dueTick, double efficiency) { }

    private static final class OwnerRuntime {
        final List<Flight> flights = new ArrayList<>();
        final List<ReplayRequest> replays = new ArrayList<>();

        boolean hasPhysicalFlight() {
            for (Flight flight : flights) if (flight.physical) return true;
            return false;
        }
    }

    private static final class Flight {
        final Display.ItemDisplay display;
        final boolean physical;
        final InteractionHand returnHand;
        final List<UUID> targetIds;
        final float damage;
        int targetIndex;
        int ageTicks;
        boolean returning;

        Flight(
                Display.ItemDisplay display,
                boolean physical,
                InteractionHand returnHand,
                List<UUID> targetIds,
                float damage
        ) {
            this.display = display;
            this.physical = physical;
            this.returnHand = returnHand;
            this.targetIds = targetIds;
            this.damage = damage;
        }
    }
}
