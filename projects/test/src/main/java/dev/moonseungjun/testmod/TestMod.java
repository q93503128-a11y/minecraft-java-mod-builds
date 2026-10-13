package dev.moonseungjun.testmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TestMod implements ModInitializer {
    public static final String MOD_ID = "moon_test";

    private static final int COOLDOWN_TICKS = 20 * 12;
    private static final int IMPACT_TICK = 58;
    private static final int END_TICK = 72;
    private static final double DAMAGE_RADIUS = 9.0;

    public static final Item STARFALL_FOCUS = Registry.register(
            Registries.ITEM,
            Identifier.of(MOD_ID, "starfall_focus"),
            new StarfallFocusItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC))
    );

    private static final Map<RegistryKey<World>, List<ActiveCast>> ACTIVE_CASTS = new HashMap<>();

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(STARFALL_FOCUS));
        ServerTickEvents.END_WORLD_TICK.register(TestMod::tickWorld);
    }

    private static void beginCast(ServerWorld world, ServerPlayerEntity caster, Vec3d target) {
        Vec3d horizontal = target.subtract(caster.getPos()).multiply(1.0, 0.0, 1.0);
        if (horizontal.lengthSquared() < 0.01) {
            horizontal = caster.getRotationVec(1.0F).multiply(1.0, 0.0, 1.0);
        }

        Vec3d direction = horizontal.normalize();
        Vec3d star = target.add(direction.multiply(18.0)).add(0.0, 46.0, 0.0);

        ACTIVE_CASTS.computeIfAbsent(world.getRegistryKey(), ignored -> new ArrayList<>())
                .add(new ActiveCast(caster.getUuid(), target, star));

        world.playSound(null, BlockPos.ofFloored(target), SoundEvents.BLOCK_BEACON_ACTIVATE,
                SoundCategory.PLAYERS, 1.1F, 1.35F);
        caster.sendMessage(Text.translatable("message.moon_test.starfall_cast"), true);
    }

    private static void tickWorld(ServerWorld world) {
        List<ActiveCast> casts = ACTIVE_CASTS.get(world.getRegistryKey());
        if (casts == null || casts.isEmpty()) {
            return;
        }

        Iterator<ActiveCast> iterator = casts.iterator();
        while (iterator.hasNext()) {
            ActiveCast cast = iterator.next();
            cast.age++;

            if (cast.age <= 18) {
                renderStarPulse(world, cast);
            } else if (cast.age < IMPACT_TICK) {
                renderApproach(world, cast);
            } else if (!cast.impacted) {
                cast.impacted = true;
                impact(world, cast);
            } else {
                renderAftershock(world, cast);
            }

            if (cast.age >= END_TICK) {
                iterator.remove();
            }
        }

        if (casts.isEmpty()) {
            ACTIVE_CASTS.remove(world.getRegistryKey());
        }
    }

    private static void renderStarPulse(ServerWorld world, ActiveCast cast) {
        double pulse = 0.35 + 0.22 * Math.sin(cast.age * 0.9);
        Vec3d center = cast.star;

        forcedParticles(world, ParticleTypes.GLOW, center, 8, pulse, pulse, pulse, 0.01);
        forcedParticles(world, ParticleTypes.END_ROD, center, 3, 0.08, 0.08, 0.08, 0.0);

        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2.0 * i / 8.0;
            Vec3d arm = center.add(Math.cos(angle) * pulse * 1.8, Math.sin(angle) * pulse * 1.8, 0.0);
            forcedParticles(world, ParticleTypes.END_ROD, arm, 1, 0.0, 0.0, 0.0, 0.0);
        }

        if (cast.age == 6 || cast.age == 13 || cast.age == 18) {
            forcedParticles(world, ParticleTypes.FLASH, center, 1, 0.0, 0.0, 0.0, 0.0);
            world.playSound(null, BlockPos.ofFloored(center), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                    SoundCategory.PLAYERS, 1.7F, 1.45F + cast.age * 0.01F);
        }
    }

    private static void renderApproach(ServerWorld world, ActiveCast cast) {
        double t = (cast.age - 18.0) / (IMPACT_TICK - 18.0);
        double accelerated = t * t;
        Vec3d impactPoint = cast.target.add(0.0, 0.8, 0.0);
        Vec3d core = cast.star.lerp(impactPoint, accelerated);

        int coreCount = 8 + (int) (t * 22.0);
        double spread = 0.08 + t * 0.22;
        forcedParticles(world, ParticleTypes.GLOW, core, coreCount, spread, spread, spread, 0.02);
        forcedParticles(world, ParticleTypes.END_ROD, core, 6 + (int) (t * 8.0), spread * 0.7, spread * 0.7, spread * 0.7, 0.01);
        forcedParticles(world, ParticleTypes.ELECTRIC_SPARK, core, 3 + (int) (t * 5.0), spread, spread, spread, 0.015);

        for (int i = 1; i <= 9; i++) {
            double delayed = Math.max(0.0, t - i * (0.010 + 0.008 * t));
            double trailProgress = delayed * delayed;
            Vec3d trail = cast.star.lerp(impactPoint, trailProgress);
            forcedParticles(world, i % 2 == 0 ? ParticleTypes.FIREWORK : ParticleTypes.END_ROD,
                    trail, 2, 0.06 + t * 0.08, 0.06 + t * 0.08, 0.06 + t * 0.08, 0.0);
        }

        if (cast.age % 8 == 0) {
            float pitch = 0.75F + (float) t;
            world.playSound(null, BlockPos.ofFloored(cast.target), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                    SoundCategory.PLAYERS, 1.2F, pitch);
        }

        if (cast.age >= 48 && cast.age % 3 == 0) {
            forcedParticles(world, ParticleTypes.FLASH, core, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static void impact(ServerWorld world, ActiveCast cast) {
        Vec3d center = cast.target.add(0.0, 0.55, 0.0);

        forcedParticles(world, ParticleTypes.FLASH, center, 5, 0.4, 0.4, 0.4, 0.0);
        forcedParticles(world, ParticleTypes.GLOW, center, 95, 2.2, 1.0, 2.2, 0.16);
        forcedParticles(world, ParticleTypes.END_ROD, center, 70, 2.6, 1.2, 2.6, 0.22);
        forcedParticles(world, ParticleTypes.ELECTRIC_SPARK, center, 55, 2.2, 0.8, 2.2, 0.18);

        world.playSound(null, BlockPos.ofFloored(center), SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                SoundCategory.PLAYERS, 2.0F, 1.28F);
        world.playSound(null, BlockPos.ofFloored(center), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                SoundCategory.PLAYERS, 1.35F, 1.45F);
        world.playSound(null, BlockPos.ofFloored(center), SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,
                SoundCategory.PLAYERS, 2.0F, 0.9F);

        ServerPlayerEntity caster = world.getServer().getPlayerManager().getPlayer(cast.casterId);
        Box box = new Box(center, center).expand(DAMAGE_RADIUS);
        List<LivingEntity> victims = world.getEntitiesByClass(LivingEntity.class, box,
                entity -> entity.isAlive() && (caster == null || !entity.getUuid().equals(caster.getUuid())));

        for (LivingEntity victim : victims) {
            double distance = victim.getPos().distanceTo(center);
            if (distance > DAMAGE_RADIUS) {
                continue;
            }

            float damage = (float) (12.0 + 28.0 * (1.0 - distance / DAMAGE_RADIUS));
            victim.damage(world.getDamageSources().magic(), damage);

            Vec3d push = victim.getPos().subtract(center).multiply(1.0, 0.0, 1.0);
            if (push.lengthSquared() < 0.01) {
                push = new Vec3d(0.01, 0.0, 0.0);
            }

            double strength = 0.65 + 1.05 * (1.0 - Math.min(1.0, distance / DAMAGE_RADIUS));
            Vec3d velocity = push.normalize().multiply(strength).add(0.0, 0.42 + strength * 0.18, 0.0);
            victim.setVelocity(victim.getVelocity().add(velocity));
        }

        createCrater(world, BlockPos.ofFloored(cast.target), caster);
    }

    private static void renderAftershock(ServerWorld world, ActiveCast cast) {
        int after = cast.age - IMPACT_TICK;
        if (after < 0 || after > 11) {
            return;
        }

        double radius = 1.2 + after * 0.78;
        int points = 44;
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            Vec3d point = cast.target.add(
                    Math.cos(angle) * radius,
                    0.25 + Math.sin(after * 0.6) * 0.08,
                    Math.sin(angle) * radius
            );
            forcedParticles(world, i % 3 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD,
                    point, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private static void createCrater(ServerWorld world, BlockPos center, ServerPlayerEntity caster) {
        int radius = 4;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -2; y <= 1; y++) {
                    double distanceSquared = x * x + z * z + (y * 1.45) * (y * 1.45);
                    if (distanceSquared > radius * radius) {
                        continue;
                    }

                    BlockPos pos = center.add(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    if (state.isAir() || world.getBlockEntity(pos) != null) {
                        continue;
                    }
                    if (state.isOf(Blocks.BEDROCK)
                            || state.isOf(Blocks.END_PORTAL_FRAME)
                            || state.isOf(Blocks.REINFORCED_DEEPSLATE)
                            || state.isOf(Blocks.COMMAND_BLOCK)
                            || state.isOf(Blocks.CHAIN_COMMAND_BLOCK)
                            || state.isOf(Blocks.REPEATING_COMMAND_BLOCK)) {
                        continue;
                    }

                    float hardness = state.getHardness(world, pos);
                    if (hardness < 0.0F || hardness > 50.0F) {
                        continue;
                    }

                    world.breakBlock(pos, false, caster);
                }
            }
        }
    }

    private static void forcedParticles(ServerWorld world, ParticleEffect particle, Vec3d position,
                                        int count, double dx, double dy, double dz, double speed) {
        for (ServerPlayerEntity viewer : world.getPlayers()) {
            if (viewer.squaredDistanceTo(position) <= 512.0 * 512.0) {
                world.spawnParticles(viewer, particle, true,
                        position.x, position.y, position.z,
                        count, dx, dy, dz, speed);
            }
        }
    }

    private static boolean isNight(ServerWorld world) {
        long time = Math.floorMod(world.getTimeOfDay(), 24000L);
        return time >= 13000L && time <= 23000L;
    }

    private static final class StarfallFocusItem extends Item {
        private StarfallFocusItem(Settings settings) {
            super(settings);
        }

        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            ItemStack stack = user.getStackInHand(hand);
            if (world.isClient()) {
                return TypedActionResult.success(stack, true);
            }
            if (!(world instanceof ServerWorld serverWorld) || !(user instanceof ServerPlayerEntity serverPlayer)) {
                return TypedActionResult.pass(stack);
            }
            if (serverWorld.getRegistryKey() != World.OVERWORLD) {
                serverPlayer.sendMessage(Text.translatable("message.moon_test.overworld_only"), true);
                return TypedActionResult.fail(stack);
            }
            if (!isNight(serverWorld)) {
                serverPlayer.sendMessage(Text.translatable("message.moon_test.night_only"), true);
                return TypedActionResult.fail(stack);
            }
            if (serverPlayer.getItemCooldownManager().isCoolingDown(this)) {
                return TypedActionResult.fail(stack);
            }

            HitResult hit = serverPlayer.raycast(64.0, 1.0F, false);
            if (hit.getType() != HitResult.Type.BLOCK) {
                serverPlayer.sendMessage(Text.translatable("message.moon_test.target_required"), true);
                return TypedActionResult.fail(stack);
            }

            Vec3d target = hit.getPos();
            BlockPos skyCheck = BlockPos.ofFloored(target).up();
            if (!serverWorld.isSkyVisible(skyCheck)) {
                serverPlayer.sendMessage(Text.translatable("message.moon_test.sky_required"), true);
                return TypedActionResult.fail(stack);
            }

            beginCast(serverWorld, serverPlayer, target);
            serverPlayer.getItemCooldownManager().set(this, COOLDOWN_TICKS);
            return TypedActionResult.success(stack, false);
        }
    }

    private static final class ActiveCast {
        private final UUID casterId;
        private final Vec3d target;
        private final Vec3d star;
        private int age;
        private boolean impacted;

        private ActiveCast(UUID casterId, Vec3d target, Vec3d star) {
            this.casterId = casterId;
            this.target = target;
            this.star = star;
        }
    }
}
