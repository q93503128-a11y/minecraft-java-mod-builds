package kr.moonseungjun.villageguardians;

import com.mojang.math.Transformation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Shared non-particle skill presentation engine.
 *
 * Every effect is composed from vanilla-rendered item and block display entities.
 * No particle emitter, particle geometry, command-generated effect or permanent
 * world block is used. Gameplay remains in VillageRoleAbilitySystem; this class
 * owns only visible, bounded and automatically cleaned presentation actors.
 */
public final class VillageSkillEffectSystem {
    private static final double TAU = Math.PI * 2.0;
    private static final int GOLD = 0xFFD45A;
    private static final int STEEL = 0xA9D6FF;
    private static final int RED = 0xFF5A42;
    private static final int ICE = 0x75D7FF;
    private static final int ARCANE = 0xB76CFF;
    private static final int HOLY = 0xFFF1A6;
    private static final int GUARD = 0x5DA9FF;

    private static final List<Scene> SCENES = new ArrayList<>();
    private static final DisplayAccess DISPLAY = new DisplayAccess();

    private VillageSkillEffectSystem() {}

    public static void reset() {
        for (Scene scene : SCENES) scene.discard();
        SCENES.clear();
    }

    public static void startCast(
            ServerLevel level,
            ServerPlayer player,
            VillageRoleSkillSystem.ActiveSkill skill,
            int calculatedDuration,
            Vec3 direction) {
        if (!DISPLAY.available() || level == null || player == null || skill == null) return;
        Vec3 forward = horizontal(direction);
        switch (skill) {
            case VANGUARD_WHIRLWIND -> add(createWhirlwind(level, player,
                    Math.max(42, calculatedDuration / 2), forward));
            case VANGUARD_BREAKER -> add(createBuff(level, player, 56, forward));
            case VANGUARD_CRY -> add(createBladeCharge(level, player, 30, forward));
            case VANGUARD_STORM -> add(createSlamCharge(level, player, 44, forward));

            case RANGER_VOLLEY -> add(createRapidFire(level, player, 64, forward));
            case RANGER_PIERCE -> add(createTargetLock(level, player, 90, forward));
            case RANGER_RICOCHET -> add(createArrowRainField(level, player,
                    player.position().add(forward.scale(15.0)), 54, forward));
            case RANGER_FIRE_RAIN -> add(createEnergyCharge(level, player, 38, forward));

            case ARCANIST_FIRE_ORB -> add(createFireOrb(level, player, 72, forward));
            case ARCANIST_FROST_RING -> add(createFrostField(level, player,
                    player.position().add(forward.scale(10.0)), Math.max(120, calculatedDuration), forward));
            case ARCANIST_CHAIN -> add(createTornado(level, player,
                    player.position().add(forward.scale(2.0)), Math.max(100, calculatedDuration), forward));
            case ARCANIST_NOVA -> add(createLightningField(level, player,
                    player.position().add(forward.scale(13.0)), Math.max(80, calculatedDuration / 2), forward));

            case LUMINAR_VEIL -> add(createHealingField(level, player,
                    player.position(), Math.max(160, calculatedDuration * 2), forward));

            case WARDEN_TAUNT -> add(createShieldCharge(level, player, 32, forward));
            case WARDEN_BASH -> add(createTaunt(level, player, 48, forward));
            case WARDEN_FORMATION -> add(createFortress(level, player,
                    Math.max(100, calculatedDuration), forward));
            case WARDEN_FIELD -> add(createAegis(level, player,
                    Math.max(160, calculatedDuration * 2), forward));
            default -> {
                // Target-aware priest effects are started after their target list is resolved.
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (server == null || SCENES.isEmpty()) return;
        Iterator<Scene> iterator = SCENES.iterator();
        while (iterator.hasNext()) {
            Scene scene = iterator.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(scene.owner);
            long now = scene.level.getGameTime();
            if (owner == null || owner.level() != scene.level || now > scene.startedAt + scene.duration) {
                scene.discard();
                iterator.remove();
                continue;
            }
            update(scene, owner, Math.max(0, (int) (now - scene.startedAt)));
        }
    }

    public static void bladeWave(ServerLevel level, ServerPlayer player, Vec3 direction) {
        add(createBladeWave(level, player, 20, horizontal(direction)));
    }

    public static void slamImpact(ServerLevel level, ServerPlayer player) {
        add(createSlamImpact(level, player, 30, horizontal(player.getLookAngle())));
    }

    public static void energyArrow(ServerLevel level, ServerPlayer player, Vec3 direction) {
        add(createEnergyProjectile(level, player, 38, horizontal(direction)));
    }

    public static void arrowRainImpact(ServerLevel level, ServerPlayer player, Vec3 center) {
        add(createArrowRainImpact(level, player, center, 24, horizontal(player.getLookAngle())));
    }

    public static void shieldCharge(ServerLevel level, ServerPlayer player, Vec3 direction) {
        add(createShieldCharge(level, player, 26, horizontal(direction)));
    }

    public static void ricochet(
            ServerLevel level,
            ServerPlayer player,
            Mob primary,
            List<Mob> chained) {
        if (primary == null) return;
        List<Vec3> points = new ArrayList<>();
        points.add(player.getEyePosition());
        points.add(primary.getEyePosition());
        if (chained != null) {
            for (Mob target : chained) {
                if (target != null) points.add(target.getEyePosition());
            }
        }
        add(createPath(level, player, Mode.RICOCHET, points, 9 + points.size() * 3, STEEL));
    }

    public static void healLink(ServerLevel level, ServerPlayer caster, ServerPlayer target) {
        if (target == null) return;
        add(createPath(level, caster, Mode.HEAL_LINK,
                List.of(caster.getEyePosition(), target.getEyePosition()), 32, HOLY));
    }

    public static void cleanse(ServerLevel level, ServerPlayer caster, List<ServerPlayer> allies) {
        List<Vec3> points = positions(allies, caster.position());
        add(createPulse(level, caster, Mode.CLEANSE, points, 44, HOLY));
    }

    public static void miracle(ServerLevel level, ServerPlayer caster, List<ServerPlayer> allies) {
        List<Vec3> points = positions(allies, caster.position());
        add(createMiracle(level, caster, points, 72, horizontal(caster.getLookAngle())));
    }

    private static List<Vec3> positions(List<ServerPlayer> players, Vec3 fallback) {
        List<Vec3> result = new ArrayList<>();
        if (players != null) {
            for (ServerPlayer player : players) {
                if (player != null) result.add(player.position());
            }
        }
        if (result.isEmpty()) result.add(fallback);
        return result;
    }

    private static void add(Scene scene) {
        if (scene == null || scene.nodes.isEmpty()) return;
        SCENES.add(scene);
    }

    private static Scene createWhirlwind(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.WHIRLWIND, owner.position(), forward, duration, GOLD);
        for (int i = 0; i < 6; i++) item(scene, new ItemStack(Items.NETHERITE_SWORD), 1.32f, GOLD);
        for (int i = 0; i < 6; i++) block(scene, Blocks.GLASS.defaultBlockState(), new Vector3f(1.32f, 0.045f, 0.20f), GOLD);
        return scene;
    }

    private static Scene createBuff(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.BUFF, owner.position(), forward, duration, GOLD);
        item(scene, new ItemStack(Items.GOLDEN_SWORD), 1.85f, GOLD);
        for (int i = 0; i < 5; i++) item(scene,
                new ItemStack(i % 2 == 0 ? Items.SHIELD : Items.GOLDEN_SWORD), 0.92f, GOLD);
        return scene;
    }

    private static Scene createBladeCharge(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.BLADE_CHARGE, owner.position(), forward, duration, STEEL);
        for (int i = 0; i < 4; i++) item(scene, new ItemStack(Items.DIAMOND_SWORD), 1.18f, STEEL);
        return scene;
    }

    private static Scene createSlamCharge(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.SLAM_CHARGE, owner.position(), forward, duration, RED);
        item(scene, new ItemStack(Items.NETHERITE_SWORD), 2.05f, RED);
        for (int i = 0; i < 7; i++) block(scene, Blocks.BLACKSTONE.defaultBlockState(),
                new Vector3f(0.34f, 0.34f, 0.34f), RED);
        return scene;
    }

    private static Scene createRapidFire(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.RAPID_FIRE, owner.position(), forward, duration, STEEL);
        item(scene, new ItemStack(Items.BOW), 1.42f, STEEL);
        for (int i = 0; i < 3; i++) item(scene, new ItemStack(Items.SPECTRAL_ARROW), 1.15f, STEEL);
        return scene;
    }

    private static Scene createTargetLock(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.TARGET_LOCK, owner.position(), forward, duration, GOLD);
        for (int i = 0; i < 6; i++) item(scene, new ItemStack(Items.SPECTRAL_ARROW), 0.86f, GOLD);
        return scene;
    }

    private static Scene createArrowRainField(ServerLevel level, ServerPlayer owner, Vec3 center,
                                               int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.ARROW_RAIN, center, forward, duration, STEEL);
        for (int i = 0; i < 14; i++) item(scene, new ItemStack(Items.SPECTRAL_ARROW), 0.95f, STEEL);
        return scene;
    }

    private static Scene createArrowRainImpact(ServerLevel level, ServerPlayer owner, Vec3 center,
                                                int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.ARROW_RAIN_IMPACT, center, forward, duration, STEEL);
        for (int i = 0; i < 8; i++) item(scene, new ItemStack(Items.ARROW), 0.78f, STEEL);
        return scene;
    }

    private static Scene createEnergyCharge(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.ENERGY_CHARGE, owner.position(), forward, duration, GOLD);
        item(scene, new ItemStack(Items.BOW), 1.62f, GOLD);
        item(scene, new ItemStack(Items.SPECTRAL_ARROW), 2.55f, GOLD);
        item(scene, new ItemStack(Items.SPECTRAL_ARROW), 1.45f, GOLD);
        item(scene, new ItemStack(Items.SPECTRAL_ARROW), 1.45f, GOLD);
        return scene;
    }

    private static Scene createEnergyProjectile(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.ENERGY_PROJECTILE,
                owner.getEyePosition().add(forward.scale(0.8)), forward, duration, GOLD);
        item(scene, new ItemStack(Items.SPECTRAL_ARROW), 3.25f, GOLD);
        item(scene, new ItemStack(Items.SPECTRAL_ARROW), 1.75f, GOLD);
        item(scene, new ItemStack(Items.SPECTRAL_ARROW), 1.75f, GOLD);
        for (int i = 0; i < 4; i++) block(scene, Blocks.GLASS.defaultBlockState(),
                new Vector3f(0.22f, 0.22f, 1.9f), GOLD);
        return scene;
    }

    private static Scene createFireOrb(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.FIRE_ORB,
                owner.getEyePosition().add(forward.scale(0.8)), forward, duration, RED);
        item(scene, new ItemStack(Items.FIRE_CHARGE), 2.05f, RED);
        for (int i = 0; i < 4; i++) item(scene, new ItemStack(Items.BLAZE_ROD), 0.82f, GOLD);
        return scene;
    }

    private static Scene createFrostField(ServerLevel level, ServerPlayer owner, Vec3 center,
                                          int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.FROST_FIELD, center, forward, duration, ICE);
        for (int i = 0; i < 12; i++) block(scene, Blocks.PACKED_ICE.defaultBlockState(),
                new Vector3f(0.55f, 0.08f, 1.32f), ICE);
        for (int i = 0; i < 6; i++) item(scene, new ItemStack(Items.AMETHYST_SHARD), 0.88f, ICE);
        return scene;
    }

    private static Scene createTornado(ServerLevel level, ServerPlayer owner, Vec3 center,
                                       int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.TORNADO, center, forward, duration, ARCANE);
        for (int i = 0; i < 12; i++) item(scene,
                new ItemStack(i % 2 == 0 ? Items.FEATHER : Items.QUARTZ), 0.82f, ARCANE);
        for (int i = 0; i < 6; i++) block(scene, Blocks.GLASS.defaultBlockState(),
                new Vector3f(0.38f, 0.16f, 0.95f), ARCANE);
        return scene;
    }

    private static Scene createLightningField(ServerLevel level, ServerPlayer owner, Vec3 center,
                                               int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.LIGHTNING_FIELD, center, forward, duration, ARCANE);
        for (int i = 0; i < 8; i++) item(scene, new ItemStack(Items.BLAZE_ROD), 1.18f, ARCANE);
        for (int i = 0; i < 4; i++) block(scene, Blocks.REDSTONE_BLOCK.defaultBlockState(),
                new Vector3f(0.22f, 0.22f, 0.22f), ARCANE);
        return scene;
    }

    private static Scene createHealingField(ServerLevel level, ServerPlayer owner, Vec3 center,
                                             int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.HEAL_FIELD, center, forward, duration, HOLY);
        for (int i = 0; i < 12; i++) block(scene, Blocks.SEA_LANTERN.defaultBlockState(),
                new Vector3f(0.52f, 0.055f, 1.12f), HOLY);
        for (int i = 0; i < 6; i++) item(scene, new ItemStack(Items.AMETHYST_SHARD), 0.78f, HOLY);
        return scene;
    }

    private static Scene createShieldCharge(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.SHIELD_CHARGE, owner.position(), forward, duration, GUARD);
        for (int i = 0; i < 3; i++) item(scene, new ItemStack(Items.SHIELD), 1.82f, GUARD);
        return scene;
    }

    private static Scene createTaunt(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.TAUNT, owner.position(), forward, duration, GUARD);
        for (int i = 0; i < 8; i++) item(scene, new ItemStack(Items.SHIELD), 0.88f, GUARD);
        return scene;
    }

    private static Scene createFortress(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.FORTRESS, owner.position(), forward, duration, GUARD);
        for (int i = 0; i < 15; i++) block(scene,
                (i + i / 5) % 2 == 0 ? Blocks.GLASS.defaultBlockState() : Blocks.DARK_PRISMARINE.defaultBlockState(),
                new Vector3f(0.94f, 0.94f, 0.16f), GUARD);
        for (int i = 0; i < 3; i++) item(scene, new ItemStack(Items.SHIELD), 1.35f, GUARD);
        return scene;
    }

    private static Scene createAegis(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.AEGIS, owner.position(), forward, duration, GUARD);
        for (int i = 0; i < 12; i++) block(scene,
                i % 3 == 1 ? Blocks.DARK_PRISMARINE.defaultBlockState() : Blocks.GLASS.defaultBlockState(),
                new Vector3f(0.92f, 0.92f, 0.14f), GUARD);
        for (int i = 0; i < 2; i++) item(scene, new ItemStack(Items.SHIELD), 1.52f, GUARD);
        return scene;
    }

    private static Scene createBladeWave(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.BLADE_WAVE,
                owner.getEyePosition().add(forward.scale(0.8)), forward, duration, STEEL);
        item(scene, new ItemStack(Items.NETHERITE_SWORD), 1.72f, STEEL);
        item(scene, new ItemStack(Items.DIAMOND_SWORD), 1.38f, STEEL);
        for (int i = 0; i < 3; i++) block(scene, Blocks.GLASS.defaultBlockState(),
                new Vector3f(0.18f, 0.055f, 1.35f), STEEL);
        return scene;
    }

    private static Scene createSlamImpact(ServerLevel level, ServerPlayer owner, int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.SLAM_IMPACT, owner.position(), forward, duration, RED);
        for (int i = 0; i < 14; i++) block(scene,
                i % 2 == 0 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                new Vector3f(0.42f, 0.22f, 1.05f), i % 2 == 0 ? RED : ARCANE);
        return scene;
    }

    private static Scene createPath(ServerLevel level, ServerPlayer owner, Mode mode,
                                    List<Vec3> points, int duration, int color) {
        Scene scene = scene(level, owner, mode, points.get(0),
                points.size() > 1 ? horizontal(points.get(1).subtract(points.get(0))) : new Vec3(0, 0, 1),
                duration, color);
        scene.points.addAll(points);
        int count = mode == Mode.HEAL_LINK ? 5 : Math.min(7, Math.max(3, points.size() + 1));
        for (int i = 0; i < count; i++) item(scene,
                new ItemStack(mode == Mode.HEAL_LINK ? (i % 2 == 0 ? Items.GHAST_TEAR : Items.AMETHYST_SHARD)
                        : Items.SPECTRAL_ARROW), mode == Mode.HEAL_LINK ? 0.82f : 1.08f, color);
        return scene;
    }

    private static Scene createPulse(ServerLevel level, ServerPlayer owner, Mode mode,
                                     List<Vec3> points, int duration, int color) {
        Scene scene = scene(level, owner, mode, owner.position(), horizontal(owner.getLookAngle()), duration, color);
        scene.points.addAll(points);
        for (int i = 0; i < Math.max(10, points.size() * 3); i++) item(scene,
                new ItemStack(i % 3 == 0 ? Items.GOLD_INGOT : Items.AMETHYST_SHARD), 0.76f, color);
        return scene;
    }

    private static Scene createMiracle(ServerLevel level, ServerPlayer owner, List<Vec3> points,
                                       int duration, Vec3 forward) {
        Scene scene = scene(level, owner, Mode.MIRACLE, owner.position(), forward, duration, HOLY);
        scene.points.addAll(points);
        item(scene, new ItemStack(Items.TOTEM_OF_UNDYING), 2.35f, HOLY);
        for (int i = 0; i < 12; i++) block(scene,
                i % 2 == 0 ? Blocks.GOLD_BLOCK.defaultBlockState() : Blocks.SEA_LANTERN.defaultBlockState(),
                new Vector3f(0.36f, 0.72f, 0.14f), HOLY);
        for (int i = 0; i < Math.max(6, points.size() * 2); i++) item(scene,
                new ItemStack(Items.AMETHYST_SHARD), 0.72f, HOLY);
        return scene;
    }

    private static Scene scene(ServerLevel level, ServerPlayer owner, Mode mode,
                               Vec3 center, Vec3 direction, int duration, int color) {
        return new Scene(level, owner.getUUID(), mode, center, horizontal(direction),
                level.getGameTime(), Math.max(2, duration), color);
    }

    private static void item(Scene scene, ItemStack stack, float scale, int color) {
        Display.ItemDisplay display = DISPLAY.item(scene.level, stack, scene.center, scale, color);
        if (display != null) scene.nodes.add(new Node(display, scene.nodes.size(), new Vector3f(scale, scale, scale)));
    }

    private static void block(Scene scene, BlockState state, Vector3f scale, int color) {
        Display.BlockDisplay display = DISPLAY.block(scene.level, state, scene.center, scale, color);
        if (display != null) scene.nodes.add(new Node(display, scene.nodes.size(), new Vector3f(scale)));
    }

    private static void update(Scene scene, ServerPlayer owner, int age) {
        double progress = clamp(age / (double) Math.max(1, scene.duration), 0.0, 1.0);
        switch (scene.mode) {
            case WHIRLWIND -> updateWhirlwind(scene, owner, age, progress);
            case BUFF -> updateBuff(scene, owner, age, progress);
            case BLADE_CHARGE -> updateBladeCharge(scene, owner, age, progress);
            case SLAM_CHARGE -> updateSlamCharge(scene, owner, age, progress);
            case RAPID_FIRE -> updateRapid(scene, owner, age, progress);
            case TARGET_LOCK -> updateTargetLock(scene, owner, age, progress);
            case ARROW_RAIN, ARROW_RAIN_IMPACT -> updateArrowRain(scene, age, progress);
            case ENERGY_CHARGE -> updateEnergyCharge(scene, owner, age, progress);
            case ENERGY_PROJECTILE -> updateEnergyProjectile(scene, age, progress);
            case FIRE_ORB -> updateFireOrb(scene, age, progress);
            case FROST_FIELD -> updateFrost(scene, age, progress);
            case TORNADO -> updateTornado(scene, age, progress);
            case LIGHTNING_FIELD -> updateLightning(scene, age, progress);
            case HEAL_FIELD -> updateHealField(scene, age, progress);
            case SHIELD_CHARGE -> updateShieldCharge(scene, owner, age, progress);
            case TAUNT -> updateTaunt(scene, owner, age, progress);
            case FORTRESS -> updateWall(scene, owner, age, true);
            case AEGIS -> updateWall(scene, owner, age, false);
            case BLADE_WAVE -> updateBladeWave(scene, age, progress);
            case SLAM_IMPACT -> updateSlamImpact(scene, age, progress);
            case RICOCHET, HEAL_LINK -> updatePath(scene, age, progress);
            case CLEANSE -> updateCleanse(scene, age, progress);
            case MIRACLE -> updateMiracle(scene, owner, age, progress);
        }
    }

    private static void updateWhirlwind(Scene scene, ServerPlayer owner, int age, double progress) {
        Vec3 center = owner.position();
        for (Node node : scene.nodes) {
            int i = node.index % 6;
            double angle = age * 0.48 + i * TAU / 6.0;
            if (node.index < 6) {
                Vec3 pos = center.add(Math.cos(angle) * 2.25, 1.05 + Math.sin(angle * 2.0) * 0.10,
                        Math.sin(angle) * 2.25);
                pose(node, pos, 0.0f, (float) (-angle + Math.PI / 2.0), (float) (Math.PI / 2.0),
                        node.scale.x, node.scale.y, node.scale.z);
            } else {
                double ringAngle = angle + 0.26;
                Vec3 pos = center.add(Math.cos(ringAngle) * 1.82, 1.02,
                        Math.sin(ringAngle) * 1.82);
                float pulse = 1.0f + (float) Math.sin(age * 0.35 + i) * 0.12f;
                pose(node, pos, 0.0f, (float) -ringAngle, 0.0f,
                        node.scale.x * pulse, node.scale.y, node.scale.z);
            }
        }
    }

    private static void updateBuff(Scene scene, ServerPlayer owner, int age, double progress) {
        Vec3 center = owner.position();
        for (Node node : scene.nodes) {
            if (node.index == 0) {
                double y = 2.55 + Math.sin(age * 0.18) * 0.18;
                pose(node, center.add(0, y, 0), 0.0f, age * 0.08f,
                        (float) Math.PI, 1.85f, 1.85f, 1.85f);
            } else {
                double angle = age * 0.18 + (node.index - 1) * TAU / 5.0;
                double radius = 1.2 + progress * 1.2;
                pose(node, center.add(Math.cos(angle) * radius, 1.1 + Math.sin(angle * 2.0) * 0.20,
                                Math.sin(angle) * radius),
                        0, (float) -angle, node.index % 2 == 0 ? 0 : (float) Math.PI / 2,
                        node.scale.x, node.scale.y, node.scale.z);
            }
        }
    }

    private static void updateBladeCharge(Scene scene, ServerPlayer owner, int age, double progress) {
        Vec3 right = right(scene.direction);
        Vec3 center = owner.getEyePosition().add(scene.direction.scale(0.7));
        for (Node node : scene.nodes) {
            double lane = node.index - 1.5;
            double reach = 0.4 + progress * 1.8;
            Vec3 pos = center.add(right.scale(lane * 0.55)).add(scene.direction.scale(reach))
                    .add(0, Math.sin(age * 0.45 + node.index) * 0.20, 0);
            pose(node, pos, 0, yaw(scene.direction), (float) (Math.PI / 2 + lane * 0.16),
                    node.scale.x, node.scale.y, node.scale.z);
        }
    }

    private static void updateSlamCharge(Scene scene, ServerPlayer owner, int age, double progress) {
        Vec3 center = owner.position();
        for (Node node : scene.nodes) {
            if (node.index == 0) {
                pose(node, owner.getEyePosition().add(scene.direction.scale(0.35)).add(0, 1.15, 0),
                        (float) (-Math.PI / 4), yaw(scene.direction), (float) Math.PI,
                        2.05f, 2.05f, 2.05f);
            } else {
                double angle = age * 0.24 + (node.index - 1) * TAU / 7.0;
                double radius = 1.15 + progress * 1.65;
                double y = 0.18 + Math.sin(angle * 2.0) * 0.28 + progress * 0.65;
                pose(node, center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius),
                        age * 0.11f, (float) -angle, age * 0.07f,
                        node.scale.x, node.scale.y, node.scale.z);
            }
        }
    }

    private static void updateRapid(Scene scene, ServerPlayer owner, int age, double progress) {
        Vec3 center = owner.getEyePosition().add(scene.direction.scale(1.0));
        Vec3 right = right(scene.direction);
        for (Node node : scene.nodes) {
            if (node.index == 0) {
                pose(node, center.add(0, -0.12, 0), 0, yaw(scene.direction), 0,
                        1.42f, 1.42f, 1.42f);
            } else {
                double lane = node.index - 2.0;
                pose(node, center.add(right.scale(lane * 0.34)).add(scene.direction.scale(0.45))
                                .add(0, lane * 0.08, 0),
                        0, yaw(scene.direction), (float) Math.PI / 2,
                        1.15f, 1.15f, 1.15f);
            }
        }
    }

    private static void updateTargetLock(Scene scene, ServerPlayer owner, int age, double progress) {
        Vec3 center = owner.getEyePosition();
        for (Node node : scene.nodes) {
            double angle = age * 0.10 + node.index * TAU / scene.nodes.size();
            double radius = 0.75 + Math.sin(age * 0.12) * 0.08;
            Vec3 pos = center.add(Math.cos(angle) * radius, 0.35 + Math.sin(angle * 2.0) * 0.22,
                    Math.sin(angle) * radius).add(scene.direction.scale(0.35));
            pose(node, pos, 0, yaw(scene.direction), (float) Math.PI / 2,
                    node.scale.x, node.scale.y, node.scale.z);
        }
    }

    private static void updateArrowRain(Scene scene, int age, double progress) {
        int count = scene.nodes.size();
        for (Node node : scene.nodes) {
            double angle = node.index * TAU / count + (node.index % 3) * 0.22;
            double radius = 1.6 + (node.index % 5) * 1.18;
            double cycle = ((age + node.index * 3) % 24) / 24.0;
            double y = 9.5 - cycle * 9.0;
            Vec3 pos = scene.center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            pose(node, pos, (float) (-Math.PI / 2), (float) -angle, 0,
                    node.scale.x, node.scale.y, node.scale.z);
        }
    }

    private static void updateEnergyCharge(Scene scene, ServerPlayer owner, int age, double progress) {
        Vec3 center = owner.getEyePosition().add(scene.direction.scale(1.25));
        Vec3 right = right(scene.direction);
        for (Node node : scene.nodes) {
            if (node.index == 0) {
                pose(node, center.add(scene.direction.scale(-0.35)), 0, yaw(scene.direction), 0,
                        1.62f, 1.62f, 1.62f);
            } else {
                double lane = node.index == 1 ? 0.0 : (node.index == 2 ? -0.42 : 0.42);
                float scale = node.index == 1 ? (float) (1.35 + progress * 2.15) : (float) (0.85 + progress * 1.0);
                pose(node, center.add(right.scale(lane)).add(scene.direction.scale(progress * 0.65)),
                        0, yaw(scene.direction), (float) Math.PI / 2,
                        scale, scale, scale);
            }
        }
    }

    private static void updateEnergyProjectile(Scene scene, int age, double progress) {
        Vec3 right = right(scene.direction);
        Vec3 center = scene.center.add(scene.direction.scale(age * 1.85));
        for (Node node : scene.nodes) {
            if (node.index < 3) {
                double lane = node.index == 0 ? 0 : (node.index == 1 ? -0.55 : 0.55);
                float scale = node.index == 0 ? 3.25f : 1.75f;
                pose(node, center.add(right.scale(lane)), 0, yaw(scene.direction), (float) Math.PI / 2,
                        scale, scale, scale);
            } else {
                double angle = age * 0.36 + (node.index - 3) * TAU / 4.0;
                Vec3 pos = center.add(right.scale(Math.cos(angle) * 0.85)).add(0, Math.sin(angle) * 0.85, 0);
                pose(node, pos, 0, yaw(scene.direction), (float) angle,
                        node.scale.x, node.scale.y, node.scale.z);
            }
        }
    }

    private static void updateFireOrb(Scene scene, int age, double progress) {
        Vec3 center = scene.center.add(scene.direction.scale(age * 0.78));
        for (Node node : scene.nodes) {
            if (node.index == 0) {
                float pulse = 1.75f + (float) Math.sin(age * 0.45) * 0.25f;
                pose(node, center, age * 0.11f, age * 0.15f, age * 0.09f,
                        pulse, pulse, pulse);
            } else {
                double angle = age * 0.42 + (node.index - 1) * TAU / 4.0;
                Vec3 pos = center.add(Math.cos(angle) * 0.95, Math.sin(angle * 1.5) * 0.55,
                        Math.sin(angle) * 0.95);
                pose(node, pos, (float) angle, (float) -angle, (float) Math.PI / 2,
                        node.scale.x, node.scale.y, node.scale.z);
            }
        }
    }

    private static void updateFrost(Scene scene, int age, double progress) {
        for (Node node : scene.nodes) {
            if (node.index < 12) {
                double angle = node.index * TAU / 12.0 + age * 0.015;
                double radius = 5.8;
                Vec3 pos = scene.center.add(Math.cos(angle) * radius, 0.05, Math.sin(angle) * radius);
                float pulse = 0.92f + (float) Math.sin(age * 0.12 + node.index) * 0.10f;
                pose(node, pos, 0, (float) -angle, 0,
                        node.scale.x * pulse, node.scale.y, node.scale.z);
            } else {
                int i = node.index - 12;
                double angle = age * 0.08 + i * TAU / 6.0;
                Vec3 pos = scene.center.add(Math.cos(angle) * 3.4, 0.45 + Math.sin(age * 0.13 + i) * 0.25,
                        Math.sin(angle) * 3.4);
                pose(node, pos, age * 0.04f, (float) -angle, (float) Math.PI / 4,
                        node.scale.x, node.scale.y, node.scale.z);
            }
        }
    }

    private static void updateTornado(Scene scene, int age, double progress) {
        scene.center = scene.center.add(scene.direction.scale(0.10));
        for (Node node : scene.nodes) {
            int layer = node.index % 6;
            double angle = age * 0.35 + node.index * TAU / scene.nodes.size();
            double radius = 0.65 + layer * 0.20;
            double y = 0.25 + layer * 0.62;
            Vec3 pos = scene.center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            float scale = 0.65f + layer * 0.08f;
            pose(node, pos, age * 0.08f, (float) -angle, age * 0.12f,
                    node.scale.x * scale, node.scale.y * scale, node.scale.z * scale);
        }
    }

    private static void updateLightning(Scene scene, int age, double progress) {
        for (Node node : scene.nodes) {
            if (node.index < 8) {
                double angle = node.index * TAU / 8.0 + age * 0.04;
                double radius = 2.0 + (node.index % 4) * 1.45;
                double y = 5.5 - ((age + node.index * 4) % 20) * 0.22;
                pose(node, scene.center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius),
                        (float) (-Math.PI / 2), (float) -angle, 0,
                        node.scale.x, node.scale.y, node.scale.z);
            } else {
                int i = node.index - 8;
                double angle = age * 0.18 + i * TAU / 4.0;
                pose(node, scene.center.add(Math.cos(angle) * 2.2, 0.25, Math.sin(angle) * 2.2),
                        age * 0.1f, (float) -angle, age * 0.08f,
                        node.scale.x, node.scale.y, node.scale.z);
            }
        }
    }

    private static void updateHealField(Scene scene, int age, double progress) {
        for (Node node : scene.nodes) {
            if (node.index < 12) {
                double angle = node.index * TAU / 12.0 + age * 0.025;
                Vec3 pos = scene.center.add(Math.cos(angle) * 6.5, 0.08, Math.sin(angle) * 6.5);
                pose(node, pos, 0, (float) -angle, 0,
                        node.scale.x, node.scale.y, node.scale.z);
            } else {
                int i = node.index - 12;
                double angle = age * 0.10 + i * TAU / 6.0;
                double y = 0.55 + ((age + i * 7) % 35) / 35.0 * 2.3;
                pose(node, scene.center.add(Math.cos(angle) * 3.2, y, Math.sin(angle) * 3.2),
                        age * 0.05f, (float) -angle, age * 0.05f,
                        node.scale.x, node.scale.y, node.scale.z);
            }
        }
    }

    private static void updateShieldCharge(Scene scene, ServerPlayer owner, int age, double progress) {
        Vec3 forward = horizontal(owner.getLookAngle());
        Vec3 right = right(forward);
        Vec3 center = owner.position().add(forward.scale(1.35)).add(0, 1.05, 0);
        for (Node node : scene.nodes) {
            double lane = node.index - 1.0;
            pose(node, center.add(right.scale(lane * 0.72)), 0, yaw(forward), 0,
                    1.82f, 1.82f, 1.82f);
        }
    }

    private static void updateTaunt(Scene scene, ServerPlayer owner, int age, double progress) {
        Vec3 center = owner.position();
        for (Node node : scene.nodes) {
            double angle = node.index * TAU / scene.nodes.size() + age * 0.06;
            double radius = 0.8 + progress * 7.8;
            pose(node, center.add(Math.cos(angle) * radius, 0.9 + Math.sin(angle * 2) * 0.25,
                            Math.sin(angle) * radius),
                    0, (float) -angle, 0,
                    node.scale.x, node.scale.y, node.scale.z);
        }
    }

    private static void updateWall(Scene scene, ServerPlayer owner, int age, boolean fortress) {
        Vec3 forward = horizontal(owner.getLookAngle());
        Vec3 right = right(forward);
        double distance = fortress ? 1.75 : 2.25;
        Vec3 base = owner.position().add(forward.scale(distance));
        int blockCount = fortress ? 15 : 12;
        int width = fortress ? 5 : 4;
        for (Node node : scene.nodes) {
            if (node.index < blockCount) {
                int x = node.index % width;
                int y = node.index / width;
                double horizontal = x - (width - 1) / 2.0;
                Vec3 pos = base.add(right.scale(horizontal * 0.92)).add(0, 0.15 + y * 0.92, 0);
                float pulse = 0.94f + (float) Math.sin(age * 0.10 + node.index) * 0.035f;
                pose(node, pos, 0, yaw(forward), 0,
                        node.scale.x * pulse, node.scale.y * pulse, node.scale.z);
            } else {
                int i = node.index - blockCount;
                double horizontal = (i - (scene.nodes.size() - blockCount - 1) / 2.0) * 1.6;
                pose(node, base.add(right.scale(horizontal)).add(0, fortress ? 1.45 : 1.05, 0),
                        0, yaw(forward), 0,
                        node.scale.x, node.scale.y, node.scale.z);
            }
        }
    }

    private static void updateBladeWave(Scene scene, int age, double progress) {
        Vec3 right = right(scene.direction);
        Vec3 center = scene.center.add(scene.direction.scale(age * 1.38));
        for (Node node : scene.nodes) {
            if (node.index < 2) {
                double lane = node.index == 0 ? -0.28 : 0.28;
                pose(node, center.add(right.scale(lane)).add(0, node.index * 0.18, 0),
                        0, yaw(scene.direction), (float) (Math.PI / 2 + lane),
                        node.scale.x, node.scale.y, node.scale.z);
            } else {
                double angle = age * 0.42 + (node.index - 2) * TAU / 3.0;
                pose(node, center.add(right.scale(Math.cos(angle) * 0.55)).add(0, Math.sin(angle) * 0.45, 0),
                        0, yaw(scene.direction), (float) angle,
                        node.scale.x, node.scale.y, node.scale.z);
            }
        }
    }

    private static void updateSlamImpact(Scene scene, int age, double progress) {
        int count = scene.nodes.size();
        for (Node node : scene.nodes) {
            double angle = node.index * TAU / count + (node.index % 3) * 0.10;
            double radius = 0.6 + progress * (5.0 + node.index % 3);
            double y = Math.sin(progress * Math.PI) * (0.55 + (node.index % 4) * 0.14);
            pose(node, scene.center.add(Math.cos(angle) * radius, 0.08 + y, Math.sin(angle) * radius),
                    age * 0.12f, (float) -angle, age * 0.09f,
                    node.scale.x, node.scale.y, node.scale.z);
        }
    }

    private static void updatePath(Scene scene, int age, double progress) {
        if (scene.points.size() < 2) return;
        int segments = scene.points.size() - 1;
        for (Node node : scene.nodes) {
            double phase = clamp(progress * segments + node.index * 0.17, 0.0, segments - 0.001);
            int segment = Math.min(segments - 1, (int) phase);
            double local = phase - segment;
            Vec3 from = scene.points.get(segment);
            Vec3 to = scene.points.get(segment + 1);
            Vec3 pos = from.lerp(to, local).add(0, Math.sin((local + node.index * 0.2) * Math.PI) * 0.35, 0);
            Vec3 dir = horizontal(to.subtract(from));
            float roll = scene.mode == Mode.HEAL_LINK ? age * 0.12f : (float) Math.PI / 2;
            pose(node, pos, 0, yaw(dir), roll,
                    node.scale.x, node.scale.y, node.scale.z);
        }
    }

    private static void updateCleanse(Scene scene, int age, double progress) {
        int points = Math.max(1, scene.points.size());
        for (Node node : scene.nodes) {
            Vec3 center = scene.points.get(node.index % points);
            int ringIndex = node.index / points;
            double angle = age * 0.10 + ringIndex * TAU / Math.max(3, scene.nodes.size() / points);
            double radius = 0.25 + progress * 2.8;
            double y = 0.35 + progress * 1.8;
            pose(node, center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius),
                    age * 0.08f, (float) -angle, age * 0.05f,
                    node.scale.x, node.scale.y, node.scale.z);
        }
    }

    private static void updateMiracle(Scene scene, ServerPlayer owner, int age, double progress) {
        Vec3 center = owner.position();
        int blockStart = 1;
        int itemStart = 13;
        for (Node node : scene.nodes) {
            if (node.index == 0) {
                float scale = 1.55f + (float) Math.sin(progress * Math.PI) * 1.15f;
                pose(node, center.add(0, 3.0 + Math.sin(age * 0.12) * 0.18, 0),
                        0, age * 0.06f, 0, scale, scale, scale);
            } else if (node.index < itemStart) {
                int i = node.index - blockStart;
                double angle = i * TAU / 12.0 + age * 0.025;
                double radius = 2.0 + progress * 5.5;
                double y = 0.25 + Math.sin(progress * Math.PI) * 2.1;
                pose(node, center.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius),
                        0, (float) -angle, 0,
                        node.scale.x, node.scale.y, node.scale.z);
            } else {
                int points = Math.max(1, scene.points.size());
                Vec3 target = scene.points.get((node.index - itemStart) % points);
                double angle = age * 0.13 + node.index;
                pose(node, target.add(Math.cos(angle) * 0.85,
                                0.65 + ((age + node.index * 5) % 35) / 35.0 * 2.0,
                                Math.sin(angle) * 0.85),
                        age * 0.08f, (float) -angle, age * 0.07f,
                        node.scale.x, node.scale.y, node.scale.z);
            }
        }
    }

    private static void pose(Node node, Vec3 position, float pitch, float yaw, float roll,
                             float scaleX, float scaleY, float scaleZ) {
        if (node.entity == null || node.entity.isRemoved()) return;
        node.entity.setPos(position.x, position.y, position.z);
        DISPLAY.transform(node.entity, new Vector3f(scaleX, scaleY, scaleZ), pitch, yaw, roll);
    }

    private static Vec3 horizontal(Vec3 vector) {
        if (vector == null) return new Vec3(0, 0, 1);
        Vec3 horizontal = new Vec3(vector.x, 0.0, vector.z);
        return horizontal.lengthSqr() < 0.0001 ? new Vec3(0, 0, 1) : horizontal.normalize();
    }

    private static Vec3 right(Vec3 forward) {
        return new Vec3(-forward.z, 0.0, forward.x);
    }

    private static float yaw(Vec3 direction) {
        return (float) Math.atan2(-direction.x, direction.z);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Mode {
        WHIRLWIND, BUFF, BLADE_CHARGE, SLAM_CHARGE,
        RAPID_FIRE, TARGET_LOCK, ARROW_RAIN, ARROW_RAIN_IMPACT,
        ENERGY_CHARGE, ENERGY_PROJECTILE,
        FIRE_ORB, FROST_FIELD, TORNADO, LIGHTNING_FIELD,
        HEAL_FIELD, HEAL_LINK, CLEANSE, MIRACLE,
        SHIELD_CHARGE, TAUNT, FORTRESS, AEGIS,
        BLADE_WAVE, SLAM_IMPACT, RICOCHET
    }

    private static final class Scene {
        private final ServerLevel level;
        private final UUID owner;
        private final Mode mode;
        private Vec3 center;
        private final Vec3 direction;
        private final long startedAt;
        private final int duration;
        private final int color;
        private final List<Node> nodes = new ArrayList<>();
        private final List<Vec3> points = new ArrayList<>();

        private Scene(ServerLevel level, UUID owner, Mode mode, Vec3 center, Vec3 direction,
                      long startedAt, int duration, int color) {
            this.level = level;
            this.owner = owner;
            this.mode = mode;
            this.center = center;
            this.direction = direction;
            this.startedAt = startedAt;
            this.duration = duration;
            this.color = color;
        }

        private void discard() {
            for (Node node : nodes) {
                if (node.entity != null && !node.entity.isRemoved()) node.entity.discard();
            }
            nodes.clear();
        }
    }

    private record Node(Display entity, int index, Vector3f scale) {}

    /** Isolates the private vanilla display mutators in one audited adapter. */
    private static final class DisplayAccess {
        private final Method setTransformation;
        private final Method setTransformationDuration;
        private final Method setPosRotDuration;
        private final Method setViewRange;
        private final Method setGlowColor;
        private final Method setItemTransform;
        private final Method setBlockState;
        private final boolean available;
        private boolean errorReported;

        private DisplayAccess() {
            Method transformation = null;
            Method transformationDuration = null;
            Method posRotDuration = null;
            Method viewRange = null;
            Method glowColor = null;
            Method itemTransform = null;
            Method blockState = null;
            boolean ready = false;
            try {
                transformation = privateMethod(Display.class, "setTransformation", Transformation.class);
                transformationDuration = privateMethod(Display.class,
                        "setTransformationInterpolationDuration", int.class);
                posRotDuration = privateMethod(Display.class, "setPosRotInterpolationDuration", int.class);
                viewRange = privateMethod(Display.class, "setViewRange", float.class);
                glowColor = privateMethod(Display.class, "setGlowColorOverride", int.class);
                itemTransform = privateMethod(Display.ItemDisplay.class,
                        "setItemTransform", ItemDisplayContext.class);
                blockState = privateMethod(Display.BlockDisplay.class,
                        "setBlockState", BlockState.class);
                ready = true;
            } catch (ReflectiveOperationException exception) {
                VillageGuardians.LOGGER.error("Unable to initialize non-particle skill display adapter", exception);
            }
            setTransformation = transformation;
            setTransformationDuration = transformationDuration;
            setPosRotDuration = posRotDuration;
            setViewRange = viewRange;
            setGlowColor = glowColor;
            setItemTransform = itemTransform;
            setBlockState = blockState;
            available = ready;
        }

        private static Method privateMethod(Class<?> owner, String name, Class<?>... arguments)
                throws ReflectiveOperationException {
            Method method = owner.getDeclaredMethod(name, arguments);
            method.setAccessible(true);
            return method;
        }

        private boolean available() {
            return available;
        }

        private Display.ItemDisplay item(ServerLevel level, ItemStack stack, Vec3 position,
                                         float scale, int color) {
            if (!available) return null;
            Display.ItemDisplay display = EntityTypes.ITEM_DISPLAY.create(level, EntitySpawnReason.EVENT);
            if (display == null) return null;
            display.getSlot(0).set(stack.copy());
            prepare(display, position, color);
            invoke(setItemTransform, display, ItemDisplayContext.FIXED);
            transform(display, new Vector3f(scale, scale, scale), 0, 0, 0);
            return level.addFreshEntity(display) ? display : null;
        }

        private Display.BlockDisplay block(ServerLevel level, BlockState state, Vec3 position,
                                           Vector3f scale, int color) {
            if (!available) return null;
            Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.EVENT);
            if (display == null) return null;
            prepare(display, position, color);
            invoke(setBlockState, display, state);
            transform(display, scale, 0, 0, 0);
            return level.addFreshEntity(display) ? display : null;
        }

        private void prepare(Display display, Vec3 position, int color) {
            display.setPos(position.x, position.y, position.z);
            display.setNoGravity(true);
            display.setInvulnerable(true);
            display.setSilent(true);
            display.setGlowingTag(true);
            invoke(setTransformationDuration, display, 1);
            invoke(setPosRotDuration, display, 1);
            invoke(setViewRange, display, 3.0f);
            invoke(setGlowColor, display, color);
        }

        private void transform(Display display, Vector3f scale, float pitch, float yaw, float roll) {
            if (!available || display == null) return;
            Transformation transformation = new Transformation(
                    new Vector3f(),
                    new Quaternionf().rotationXYZ(pitch, yaw, roll),
                    new Vector3f(scale),
                    new Quaternionf());
            invoke(setTransformation, display, transformation);
        }

        private void invoke(Method method, Object target, Object argument) {
            if (method == null || target == null) return;
            try {
                method.invoke(target, argument);
            } catch (ReflectiveOperationException exception) {
                if (!errorReported) {
                    errorReported = true;
                    VillageGuardians.LOGGER.error("A skill display actor could not be updated", exception);
                }
            }
        }
    }
}
