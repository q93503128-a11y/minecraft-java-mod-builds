package dev.moonseungjun.testmod;

import com.mojang.math.Transformation;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class TestMod implements ModInitializer {
    public static final String MOD_ID = "moon_test";

    private static final int APPEAR_TICK = 10;
    private static final int DESCENT_TICK = 28;
    private static final int IMPACT_TICK = 120;
    private static final int END_TICK = 137;
    private static final int TRAIL_COUNT = 7;
    private static final double DAMAGE_RADIUS = 11.0;
    private static final double TARGET_RANGE = 96.0;

    private static final ResourceKey<Item> STARFALL_FOCUS_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "starfall_focus")
    );

    public static final Item STARFALL_FOCUS = registerItem(
            STARFALL_FOCUS_KEY,
            StarfallFocusItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
    );

    private static final Map<ResourceKey<Level>, List<ActiveCast>> ACTIVE_CASTS = new HashMap<>();

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> entries.accept(STARFALL_FOCUS));
        ServerTickEvents.END_WORLD_TICK.register(TestMod::tickWorld);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ensureFocus(handler.getPlayer()));
    }

    private static Item registerItem(ResourceKey<Item> key, Function<Item.Properties, Item> factory, Item.Properties properties) {
        Item item = factory.apply(properties.setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    private static void ensureFocus(ServerPlayer player) {
        ItemStack focus = new ItemStack(STARFALL_FOCUS);
        if (!player.getInventory().contains(focus)) {
            player.addItem(focus);
        }
    }

    private static void beginCast(ServerLevel level, ServerPlayer caster, Vec3 target) {
        Vec3 horizontal = target.subtract(caster.position()).multiply(1.0, 0.0, 1.0);
        if (horizontal.lengthSqr() < 0.01) {
            horizontal = caster.getViewVector(1.0F).multiply(1.0, 0.0, 1.0);
        }
        if (horizontal.lengthSqr() < 0.01) {
            horizontal = new Vec3(0.0, 0.0, 1.0);
        }

        Vec3 direction = horizontal.normalize();
        Vec3 start = target.add(direction.scale(96.0)).add(0.0, 126.0, 0.0);
        ActiveCast cast = new ActiveCast(caster.getUUID(), target, start);
        ACTIVE_CASTS.computeIfAbsent(level.dimension(), ignored -> new ArrayList<>()).add(cast);

        level.playSound(null, caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.55F);
        caster.displayClientMessage(Component.translatable("message.moon_test.starfall_cast"), true);
    }

    private static void tickWorld(ServerLevel level) {
        List<ActiveCast> casts = ACTIVE_CASTS.get(level.dimension());
        if (casts == null || casts.isEmpty()) {
            return;
        }

        Iterator<ActiveCast> iterator = casts.iterator();
        while (iterator.hasNext()) {
            ActiveCast cast = iterator.next();
            cast.age++;

            if (cast.age == APPEAR_TICK) {
                revealStar(level, cast);
            }
            if (cast.age >= APPEAR_TICK && cast.age < DESCENT_TICK) {
                holdStar(level, cast);
            }
            if (cast.age == DESCENT_TICK) {
                createTrail(level, cast);
                level.playSound(null, BlockPos.containing(cast.start), SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS, 2.2F, 1.7F);
            }
            if (cast.age >= DESCENT_TICK && cast.age < IMPACT_TICK) {
                moveMeteor(level, cast);
            }
            if (cast.age == IMPACT_TICK) {
                impact(level, cast);
            }
            if (cast.age > IMPACT_TICK && cast.age < END_TICK) {
                animateImpact(cast);
            }
            if (cast.age >= END_TICK) {
                cast.discardDisplays();
                iterator.remove();
            }
        }

        if (casts.isEmpty()) {
            ACTIVE_CASTS.remove(level.dimension());
        }
    }

    private static void revealStar(ServerLevel level, ActiveCast cast) {
        cast.core = spawnLight(level, cast.start, "✦", 0xF8FEFF, 2.6F, true);
        cast.core.setTransformationInterpolationDuration(3);
        level.playSound(null, BlockPos.containing(cast.start), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 2.5F, 1.95F);
    }

    private static void holdStar(ServerLevel level, ActiveCast cast) {
        if (cast.core == null) {
            return;
        }
        double pulse = 1.0 + Math.sin((cast.age - APPEAR_TICK) * 0.72) * 0.16;
        setScale(cast.core, (float) (2.6 * pulse), 3);
        if (cast.age == 18) {
            level.playSound(null, BlockPos.containing(cast.start), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 1.6F, 1.5F);
        }
    }

    private static void createTrail(ServerLevel level, ActiveCast cast) {
        if (!cast.trail.isEmpty()) {
            return;
        }
        for (int i = 0; i < TRAIL_COUNT; i++) {
            float scale = 1.45F - i * 0.14F;
            Display.TextDisplay afterimage = spawnLight(level, cast.start, "✦", 0xA8E9FF, scale, false);
            afterimage.setTransformationInterpolationDuration(4);
            cast.trail.add(afterimage);
        }
    }

    private static void moveMeteor(ServerLevel level, ActiveCast cast) {
        if (cast.core == null) {
            return;
        }

        double t = clamp01((cast.age - DESCENT_TICK) / (double) (IMPACT_TICK - DESCENT_TICK));
        double progress = descentEase(t);
        Vec3 impactPoint = cast.target.add(0.0, 0.75, 0.0);
        Vec3 corePos = cast.start.lerp(impactPoint, progress);
        cast.core.setPos(corePos.x, corePos.y, corePos.z);

        float coreScale = (float) (2.35 + 1.55 * Math.pow(t, 1.7));
        setScale(cast.core, coreScale, 3);

        for (int i = 0; i < cast.trail.size(); i++) {
            double delayedT = clamp01(t - (i + 1) * (0.026 + t * 0.010));
            double delayedProgress = descentEase(delayedT);
            Vec3 trailPos = cast.start.lerp(impactPoint, delayedProgress);
            Display.TextDisplay display = cast.trail.get(i);
            display.setPos(trailPos.x, trailPos.y, trailPos.z);
            setScale(display, (float) ((1.35 - i * 0.13) * (0.9 + t * 0.45)), 4);
        }

        if (cast.age == 64) {
            level.playSound(null, BlockPos.containing(corePos), SoundEvents.BEACON_AMBIENT,
                    SoundSource.PLAYERS, 1.1F, 1.65F);
        } else if (cast.age == 92) {
            level.playSound(null, BlockPos.containing(corePos), SoundEvents.TRIDENT_RETURN,
                    SoundSource.PLAYERS, 1.4F, 1.25F);
        } else if (cast.age == 108) {
            level.playSound(null, BlockPos.containing(cast.target), SoundEvents.WARDEN_SONIC_CHARGE,
                    SoundSource.PLAYERS, 1.4F, 1.55F);
        }
    }

    private static double descentEase(double t) {
        return Math.pow(t, 2.18);
    }

    private static void impact(ServerLevel level, ActiveCast cast) {
        Vec3 center = cast.target.add(0.0, 0.65, 0.0);
        if (cast.core != null) {
            cast.core.setPos(center.x, center.y, center.z);
            cast.core.setText(Component.literal("✦").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
            setScale(cast.core, 7.0F, 2);
        }
        for (Display.TextDisplay display : cast.trail) {
            display.discard();
        }
        cast.trail.clear();

        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2.0 * i / 16.0;
            Vec3 pos = center.add(Math.cos(angle) * 1.1, 0.05, Math.sin(angle) * 1.1);
            Display.TextDisplay point = spawnLight(level, pos, "•", 0xC8F3FF, 0.95F, false);
            point.setTransformationInterpolationDuration(2);
            cast.shockRing.add(point);
        }

        level.playSound(null, BlockPos.containing(center), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, 2.4F, 1.12F);
        level.playSound(null, BlockPos.containing(center), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 1.75F, 1.45F);
        level.playSound(null, BlockPos.containing(center), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST,
                SoundSource.PLAYERS, 1.55F, 1.2F);

        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(cast.casterId);
        AABB box = new AABB(center, center).inflate(DAMAGE_RADIUS);
        List<LivingEntity> victims = level.getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity.isAlive() && (caster == null || !entity.getUUID().equals(caster.getUUID()))
        );

        for (LivingEntity victim : victims) {
            double distance = victim.position().distanceTo(center);
            if (distance > DAMAGE_RADIUS) {
                continue;
            }
            double falloff = 1.0 - distance / DAMAGE_RADIUS;
            float damage = (float) (14.0 + 34.0 * falloff);
            victim.hurtServer(level, level.damageSources().magic(), damage);

            Vec3 horizontal = victim.position().subtract(center).multiply(1.0, 0.0, 1.0);
            if (horizontal.lengthSqr() < 0.001) {
                horizontal = new Vec3(0.01, 0.0, 0.0);
            }
            Vec3 push = horizontal.normalize().scale(0.7 + 1.35 * falloff);
            victim.push(push.x, 0.45 + 0.45 * falloff, push.z);
        }

        createCrater(level, BlockPos.containing(cast.target), caster);
    }

    private static void animateImpact(ActiveCast cast) {
        int age = cast.age - IMPACT_TICK;
        if (cast.core != null) {
            float scale = 7.0F + age * 0.48F;
            setScale(cast.core, scale, 2);
        }

        double radius = 1.1 + age * 0.92;
        for (int i = 0; i < cast.shockRing.size(); i++) {
            double angle = Math.PI * 2.0 * i / cast.shockRing.size();
            Vec3 pos = cast.target.add(
                    Math.cos(angle) * radius,
                    0.45 + age * 0.025,
                    Math.sin(angle) * radius
            );
            Display.TextDisplay point = cast.shockRing.get(i);
            point.setPos(pos.x, pos.y, pos.z);
            setScale(point, Math.max(0.25F, 1.05F - age * 0.045F), 2);
        }
    }

    private static Display.TextDisplay spawnLight(
            ServerLevel level,
            Vec3 pos,
            String glyph,
            int color,
            float scale,
            boolean glowing
    ) {
        Display.TextDisplay display = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
        display.setText(Component.literal(glyph).withStyle(style -> style.withColor(color).withBold(true)));
        display.setBackgroundColor(0);
        display.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        display.setViewRange(8.0F);
        display.setShadowRadius(0.0F);
        display.setPosRotInterpolationDuration(3);
        display.setGlowColorOverride(color);
        display.setGlowingTag(glowing);
        setScale(display, scale, 0);
        display.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(display);
        return display;
    }

    private static void setScale(Display.TextDisplay display, float scale, int interpolationTicks) {
        display.setTransformationInterpolationDuration(interpolationTicks);
        display.setTransformation(new Transformation(
                null,
                null,
                new Vector3f(scale, scale, scale),
                null
        ));
    }

    private static void createCrater(ServerLevel level, BlockPos center, ServerPlayer caster) {
        int radius = 5;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -3; y <= 1; y++) {
                    double shapedDistance = x * x + z * z + (y * 1.55) * (y * 1.55);
                    if (shapedDistance > radius * radius) {
                        continue;
                    }

                    BlockPos pos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || level.getBlockEntity(pos) != null) {
                        continue;
                    }
                    if (state.is(Blocks.BEDROCK)
                            || state.is(Blocks.END_PORTAL_FRAME)
                            || state.is(Blocks.REINFORCED_DEEPSLATE)
                            || state.is(Blocks.COMMAND_BLOCK)
                            || state.is(Blocks.CHAIN_COMMAND_BLOCK)
                            || state.is(Blocks.REPEATING_COMMAND_BLOCK)) {
                        continue;
                    }

                    float hardness = state.getDestroySpeed(level, pos);
                    if (hardness < 0.0F || hardness > 50.0F) {
                        continue;
                    }
                    level.destroyBlock(pos, false, caster);
                }
            }
        }
    }

    private static boolean isNight(ServerLevel level) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        return time >= 13000L && time <= 23000L;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static final class StarfallFocusItem extends Item {
        private StarfallFocusItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player user, InteractionHand hand) {
            if (level.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            if (!(level instanceof ServerLevel serverLevel) || !(user instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            if (serverLevel.dimension() != Level.OVERWORLD) {
                serverPlayer.displayClientMessage(Component.translatable("message.moon_test.overworld_only"), true);
                return InteractionResult.FAIL;
            }

            if (serverPlayer.isShiftKeyDown()) {
                serverLevel.setDayTime(18000L);
                serverPlayer.displayClientMessage(Component.translatable("message.moon_test.night_opened"), true);
                return InteractionResult.SUCCESS;
            }

            if (!isNight(serverLevel)) {
                serverPlayer.displayClientMessage(Component.translatable("message.moon_test.night_only"), true);
                return InteractionResult.FAIL;
            }

            Vec3 from = serverPlayer.getEyePosition();
            Vec3 to = from.add(serverPlayer.getViewVector(1.0F).scale(TARGET_RANGE));
            HitResult hit = serverLevel.clip(new ClipContext(
                    from,
                    to,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    serverPlayer
            ));
            if (hit.getType() != HitResult.Type.BLOCK) {
                serverPlayer.displayClientMessage(Component.translatable("message.moon_test.target_required"), true);
                return InteractionResult.FAIL;
            }

            Vec3 target = hit.getLocation();
            BlockPos skyCheck = BlockPos.containing(target).above();
            if (!serverLevel.canSeeSky(skyCheck)) {
                serverPlayer.displayClientMessage(Component.translatable("message.moon_test.sky_required"), true);
                return InteractionResult.FAIL;
            }

            beginCast(serverLevel, serverPlayer, target);
            return InteractionResult.SUCCESS;
        }
    }

    private static final class ActiveCast {
        private final UUID casterId;
        private final Vec3 target;
        private final Vec3 start;
        private final List<Display.TextDisplay> trail = new ArrayList<>();
        private final List<Display.TextDisplay> shockRing = new ArrayList<>();
        private Display.TextDisplay core;
        private int age;

        private ActiveCast(UUID casterId, Vec3 target, Vec3 start) {
            this.casterId = casterId;
            this.target = target;
            this.start = start;
        }

        private void discardDisplays() {
            if (core != null) {
                core.discard();
            }
            for (Display.TextDisplay display : trail) {
                display.discard();
            }
            for (Display.TextDisplay display : shockRing) {
                display.discard();
            }
            trail.clear();
            shockRing.clear();
        }
    }
}
