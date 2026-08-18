package kr.moonseungjun.villageguardians;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class VillageBossAspectSystem {
    private static final int LONG = 20 * 60 * 30;
    private static final Map<UUID, Aspect> ACTIVE = new HashMap<>();

    private VillageBossAspectSystem() {}

    public static void reset() { ACTIVE.clear(); }
    public static void forget(UUID id) { if (id != null) ACTIVE.remove(id); }

    public static Aspect preview(int day, int wave, int bossIndex) {
        Aspect[] values = Aspect.values();
        return values[Math.floorMod(day * 31 + wave * 17 + bossIndex * 13, values.length)];
    }

    public static void configure(ServerLevel level, Mob mob, int day, int wave, int bossIndex) {
        Aspect aspect = preview(day, wave, bossIndex);
        ACTIVE.put(mob.getUUID(), aspect);
        Component base = mob.getCustomName();
        mob.setCustomName(Component.literal("§4[" + aspect.displayName() + "] §f"
                + (base == null ? "우두머리" : base.getString())));
        switch (aspect) {
            case BERSERKER -> {
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, LONG, 2));
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, LONG, 1));
            }
            case BULWARK -> {
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG, 2));
                mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, LONG, 4));
            }
            case BLOODBOUND -> mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, LONG, 1));
            case STORMCALLER -> mob.addEffect(new MobEffectInstance(MobEffects.SPEED, LONG, 1));
            case WARLEADER -> mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG, 1));
            case WALLBREAKER -> mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, LONG, 1));
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                SoundSource.HOSTILE, 1.15f, 0.65f + aspect.ordinal() * 0.06f);
    }

    public static void tick(ServerLevel level, MinecraftServer server, Mob mob, int globalTicks) {
        Aspect aspect = ACTIVE.get(mob.getUUID());
        if (aspect == null || !mob.isAlive()) return;
        switch (aspect) {
            case BERSERKER -> {
                if (globalTicks % 70 != 0) return;
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 2));
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 3));
            }
            case BULWARK -> {
                if (globalTicks % 150 != 0) return;
                mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 4));
                for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, mob.position(), 8.0, 10, mob.getUUID())) {
                    ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 80, 0));
                }
            }
            case BLOODBOUND -> {
                if (globalTicks % 100 == 85) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                            mob.getX(), mob.getY() + 1.0, mob.getZ(), 18, 1.0, 0.7, 1.0, 0.03);
                    return;
                }
                if (globalTicks % 100 != 0) return;
                float healed = 0.0f;
                for (ServerPlayer player : nearbyPlayers(server, mob, 11.0)) {
                    player.hurtServer(level, level.damageSources().magic(), 3.5f + VillageCouncilState.currentDay() * 0.16f);
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 70, 0));
                    healed += 4.0f;
                }
                if (healed > 0.0f) mob.heal(Math.min(22.0f, healed));
            }
            case STORMCALLER -> {
                if (globalTicks % 80 == 65) {
                    ServerPlayer warning = nearbyPlayers(server, mob, 18.0).stream()
                            .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
                    if (warning != null) {
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                                warning.getX(), warning.getY() + 0.15, warning.getZ(),
                                18, 0.8, 0.08, 0.8, 0.03);
                    }
                    return;
                }
                if (globalTicks % 80 != 0) return;
                ServerPlayer target = nearbyPlayers(server, mob, 18.0).stream()
                        .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
                if (target == null) return;
                Vec3 strike = target.position().add(
                        (mob.getRandom().nextDouble() - 0.5) * 2.0, 0.0,
                        (mob.getRandom().nextDouble() - 0.5) * 2.0);
                var lightning = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
                if (lightning != null) {
                    lightning.setVisualOnly(true);
                    lightning.setPos(strike.x, strike.y, strike.z);
                    level.addFreshEntity(lightning);
                }
                target.hurtServer(level, level.damageSources().magic(),
                        4.5f + VillageCouncilState.currentDay() * 0.20f);
            }
            case WARLEADER -> {
                if (globalTicks % 120 != 0) return;
                for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, mob.position(), 13.0, 18, mob.getUUID())) {
                    ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 1));
                    ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0));
                }
            }
            case WALLBREAKER -> { }
        }
    }

    public static float structureMultiplier(Mob mob) {
        return ACTIVE.get(mob.getUUID()) == Aspect.WALLBREAKER ? 1.55f : 1.0f;
    }

    public static String previewText(int day, int wave, int bossIndex) {
        Aspect aspect = preview(day, wave, bossIndex);
        return aspect.displayName() + " · " + aspect.description();
    }

    private static java.util.List<ServerPlayer> nearbyPlayers(MinecraftServer server, Mob mob, double radius) {
        double squared = radius * radius;
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == mob.level() && player.isAlive()
                        && !player.isSpectator() && !VillageRespawnSystem.isDowned(player)
                        && player.distanceToSqr(mob) <= squared)
                .toList();
    }

    public enum Aspect {
        BERSERKER("광전", "짧은 주기로 공격력과 이동 속도가 폭증합니다."),
        BULWARK("철벽", "자신과 주변 병력에게 보호막과 저항을 반복 부여합니다."),
        BLOODBOUND("혈계", "주변 수호자의 생명력을 흡수해 스스로 회복합니다."),
        STORMCALLER("뇌광", "주변 수호자 위치에 마법 번개를 반복 호출합니다."),
        WARLEADER("군령", "주변 적 병력의 공격력과 이동 속도를 강화합니다."),
        WALLBREAKER("파성", "시설에 가하는 피해가 크게 증가합니다.");

        private final String displayName;
        private final String description;
        Aspect(String displayName, String description) {
            this.displayName = displayName; this.description = description;
        }
        public String displayName() { return displayName; }
        public String description() { return description; }

        public static Aspect fromName(String name) {
            if (name == null) return null;
            String normalized = name.toUpperCase(Locale.ROOT);
            for (Aspect aspect : values()) if (aspect.name().equals(normalized)) return aspect;
            return null;
        }
    }
}
