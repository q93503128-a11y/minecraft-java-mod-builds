package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.ArcaneDamage;
import kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;
import kr.moonseungjun.arcanecircle.network.ArcaneNetwork;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Assigns scarce circle ranks, independent social affiliations and everyday roles to mage entities.
 * Species never determines allegiance: villagers may be covenant or villain mages and witches may be licensed.
 */
public final class ArcaneMageService {
    // Retained for migration/JAR audits and old-world naming compatibility.
    private static final String MAGE_TAG = "arcanecircle_mage";
    private static final String CIRCLE_PREFIX = "arcanecircle_circle_";
    private static final String NAME_PREFIX = "[마도사:";
    private static final Set<String> SPELLCASTER_TYPES = Set.of("witch", "evoker", "illusioner");
    private static final int[] CIRCLE_WEIGHTS = {59_000, 25_000, 10_000, 4_000, 1_500, 400, 80, 20};

    private static final Map<UUID, Long> LAST_CAST = new HashMap<>();
    private static final Map<UUID, MageProfile> PROFILES = new HashMap<>();

    private ArcaneMageService() {}

    public static void tickNear(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || player.isSpectator()) return;

        List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(56.0),
                value -> value.isAlive() && !value.isBaby() && level.isVillage(value.blockPosition()));
        ensureVillageResidents(level, villagers);
        for (Villager villager : villagers) {
            if (isMage(villager)) castResidentSpell(level, villager);
        }

        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(56.0),
                value -> value.isAlive() && SPELLCASTER_TYPES.contains(typePath(value)))) {
            ensureNaturalMage(mob);
            castHostileSpell(level, mob);
        }

        if ((level.getGameTime() & 255L) == 0L) {
            LAST_CAST.entrySet().removeIf(entry -> level.getGameTime() - entry.getValue() > 2400L);
            PROFILES.entrySet().removeIf(entry -> level.getEntity(entry.getKey()) == null);
        }
    }

    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof Villager villager) || !isMage(villager)) return;
        event.setCanceled(true);

        MageProfile mage = profile(villager);
        ArcaneQuestData quests = ArcaneQuestData.get(((ServerLevel) player.level()).getServer());
        ArcaneQuestData.QuestStatus status = quests.status(player);
        if (status.complete()) quests.claim(player);
        else if (!status.active()) quests.assign(player, mage.circle());
        else {
            ArcaneNoticeService.push(player, Component.literal("§5[마도사 의뢰] §f" + status.description()
                    + " §d" + status.progress() + "/" + status.target() + " §7· 보상 " + status.reward() + " A"), 90);
        }

        player.sendSystemMessage(Component.literal(color(mage.affiliation()) + "[" + mage.circle() + "써클 "
                + mage.role().displayName() + " 마도사] §f소속 §7" + mage.affiliation().displayName()
                + " §f· 주문서와 지팡이는 아르카나로 거래합니다."));
        ArcaneNetwork.openPage(player, "academy");
    }

    public static boolean isMage(Entity entity) {
        return PROFILES.containsKey(entity.getUUID()) || parseProfile(entity) != null;
    }

    public static int circle(Entity entity) { return profile(entity).circle(); }
    public static MagicTradition affiliation(Entity entity) { return profile(entity).affiliation(); }
    public static MageSociety.Role role(Entity entity) { return profile(entity).role(); }

    public static MagicTradition affiliation(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return MagicTradition.UNBOUND;
        return ArcaneWorldData.get(level.getServer()).tradition(player);
    }

    public static boolean autoHostile(Entity left, Entity right) {
        if (!isMage(left) || !isMage(right)) return false;
        return MageSociety.hostile(affiliation(left), affiliation(right));
    }

    private static MageProfile profile(Entity entity) {
        MageProfile cached = PROFILES.get(entity.getUUID());
        if (cached != null) return cached;
        MageProfile parsed = parseProfile(entity);
        if (parsed != null) {
            PROFILES.put(entity.getUUID(), parsed);
            return parsed;
        }
        return new MageProfile(1, MagicTradition.UNBOUND, MageSociety.Role.WANDERER);
    }

    private static MageProfile parseProfile(Entity entity) {
        Component customName = entity.getCustomName();
        if (customName == null) return null;
        String name = customName.getString();
        int start = name.indexOf(NAME_PREFIX);
        if (start < 0) return null;
        int end = name.indexOf(']', start + NAME_PREFIX.length());
        if (end < 0) return null;
        String[] parts = name.substring(start + NAME_PREFIX.length(), end).split(":");
        try {
            int circle = clamp(Integer.parseInt(parts[0]), 1, 9);
            MagicTradition affiliation = parts.length >= 2
                    ? MagicTradition.parse(parts[1]) : MagicTradition.UNBOUND;
            MageSociety.Role role = parts.length >= 3
                    ? MageSociety.Role.parse(parts[2]) : MageSociety.Role.WANDERER;
            return new MageProfile(circle, affiliation, role);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void ensureVillageResidents(ServerLevel level, List<Villager> villagers) {
        if (villagers.size() < 4) return;
        int seed = villagers.stream().mapToInt(value -> value.getUUID().hashCode()).min().orElse(0);
        int roll = Math.floorMod(seed, 100);
        int desired = roll < 52 ? 0 : roll < 90 ? 1 : 2;
        desired = Math.min(desired, Math.max(0, villagers.size() / 7));
        long existing = villagers.stream().filter(ArcaneMageService::isMage).count();
        if (existing >= desired) return;
        villagers.stream().filter(value -> !isMage(value))
                .sorted(Comparator.comparingInt(value -> Math.floorMod(value.getUUID().hashCode(), 100_000)))
                .limit(desired - existing)
                .forEach(ArcaneMageService::promoteResident);
    }

    private static void promoteResident(Villager villager) {
        int circle = weightedCircle(villager.getUUID(), 4);
        MagicTradition affiliation = residentAffiliation(villager.getUUID());
        MageSociety.Role role = residentRole(villager.getUUID(), affiliation);
        mark(villager, new MageProfile(circle, affiliation, role));
        villager.setPersistenceRequired();
    }

    private static void ensureNaturalMage(Mob mob) {
        if (isMage(mob)) return;
        String type = typePath(mob);
        int cap = switch (type) {
            case "illusioner" -> 6;
            case "evoker" -> 5;
            default -> 4;
        };
        int minimum = switch (type) {
            case "illusioner" -> 3;
            case "evoker" -> 2;
            default -> 1;
        };
        int circle = Math.max(minimum, weightedCircle(mob.getUUID(), cap));
        MagicTradition affiliation = naturalAffiliation(mob.getUUID(), type);
        MageSociety.Role role = naturalRole(mob.getUUID(), type, affiliation);
        mark(mob, new MageProfile(circle, affiliation, role));
        mob.setPersistenceRequired();
    }

    private static void mark(Entity entity, MageProfile profile) {
        PROFILES.put(entity.getUUID(), profile);
        String visible = color(profile.affiliation()) + "[마도사:" + profile.circle() + ":"
                + profile.affiliation().name() + ":" + profile.role().name() + "] "
                + profile.circle() + "써클 " + profile.role().displayName() + " 마도사";
        entity.setCustomName(Component.literal(visible));
        entity.setCustomNameVisible(true);
    }

    private static void castResidentSpell(ServerLevel level, Villager caster) {
        MageProfile mage = profile(caster);
        long now = level.getGameTime();
        int interval = Math.max(34, 92 - mage.circle() * 7);
        if (!ready(caster, now, interval)) return;

        LivingEntity target = findResidentTarget(level, caster, mage);
        if (target == null) {
            if ((mage.role() == MageSociety.Role.HOUSEHOLD || mage.role() == MageSociety.Role.SCHOLAR)
                    && caster.getHealth() < caster.getMaxHealth()) {
                caster.heal(Math.max(1.0F, mage.circle() * 0.65F));
                level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.NEUTRAL, 0.35F, 1.45F);
            }
            return;
        }

        float roleScale = mage.role() == MageSociety.Role.WARDEN ? 1.18F
                : mage.role() == MageSociety.Role.VILLAIN ? 1.28F : 1.0F;
        float damage = (1.8F + mage.circle() * 1.25F) * roleScale;
        ArcaneDamage.hurt(level, caster, target, damage);
        if (target instanceof Mob mob) mob.setTarget(caster);
        if (mage.circle() >= 2) target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                30 + mage.circle() * 8, Math.min(3, mage.circle() / 3)));
        if (mage.circle() >= 4) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
        if (mage.circle() >= 5) pushAway(caster, target, 0.28 + mage.circle() * 0.045);
        level.playSound(null, caster.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.NEUTRAL, 0.45F, 1.55F - mage.circle() * 0.04F);
    }

    private static LivingEntity findResidentTarget(ServerLevel level, Villager caster, MageProfile mage) {
        LivingEntity hostileMage = level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(15.0),
                        value -> value.isAlive() && isMage(value)
                                && MageSociety.hostile(mage.affiliation(), affiliation(value)))
                .stream().min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
        if (hostileMage != null) return hostileMage;

        Mob enemy = level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(13.0),
                        value -> value.isAlive() && value instanceof Enemy && !isMage(value))
                .stream().min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
        if (enemy != null) return enemy;

        if (mage.role() != MageSociety.Role.VILLAIN && mage.affiliation() != MagicTradition.PRIMAL) return null;
        return level.getEntitiesOfClass(ServerPlayer.class, caster.getBoundingBox().inflate(14.0),
                        value -> value.isAlive() && !value.isSpectator()
                                && MageSociety.hostile(mage.affiliation(), affiliation(value)))
                .stream().min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
    }

    private static void castHostileSpell(ServerLevel level, Mob caster) {
        MageProfile mage = profile(caster);
        LivingEntity target = caster.getTarget();
        if (target == null || !target.isAlive()) return;
        MagicTradition targetAffiliation = target instanceof ServerPlayer player
                ? affiliation(player) : isMage(target) ? affiliation(target) : MagicTradition.UNBOUND;
        if (MageSociety.avoidsAutoTarget(mage.affiliation(), targetAffiliation)) {
            caster.setTarget(null);
            return;
        }
        if (isMage(target) && !MageSociety.hostile(mage.affiliation(), targetAffiliation)
                && mage.role() != MageSociety.Role.VILLAIN) return;
        if (caster.distanceToSqr(target) > 30.0 * 30.0) return;

        long now = level.getGameTime();
        if (!ready(caster, now, Math.max(30, 94 - mage.circle() * 7))) return;
        float roleScale = mage.role() == MageSociety.Role.VILLAIN ? 1.30F : 1.0F;
        ArcaneDamage.hurt(level, caster, target, (2.0F + mage.circle() * 1.28F) * roleScale);
        switch ((mage.circle() + mage.role().ordinal()) % 4) {
            case 0 -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 18 + mage.circle() * 2, 0));
            case 1 -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                    45 + mage.circle() * 5, Math.min(3, mage.circle() / 3)));
            case 2 -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    50 + mage.circle() * 6, Math.min(2, mage.circle() / 4)));
            default -> target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 30 + mage.circle() * 10));
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE, 0.7F, 1.25F - mage.circle() * 0.035F);
    }

    private static int weightedCircle(UUID uuid, int cap) {
        cap = clamp(cap, 1, 8);
        int total = 0;
        for (int index = 0; index < cap; index++) total += CIRCLE_WEIGHTS[index];
        int roll = Math.floorMod(uuid.hashCode() * 31 + uuid.toString().hashCode(), total);
        int cumulative = 0;
        for (int index = 0; index < cap; index++) {
            cumulative += CIRCLE_WEIGHTS[index];
            if (roll < cumulative) return index + 1;
        }
        return cap;
    }

    private static MagicTradition residentAffiliation(UUID uuid) {
        int roll = Math.floorMod(uuid.hashCode(), 100);
        if (roll < 58) return MagicTradition.ARCANE;
        if (roll < 78) return MagicTradition.DIVINE;
        if (roll < 96) return MagicTradition.OCCULT;
        return MagicTradition.PRIMAL;
    }

    private static MageSociety.Role residentRole(UUID uuid, MagicTradition affiliation) {
        int roll = Math.floorMod(uuid.toString().hashCode(), 100);
        if (affiliation == MagicTradition.PRIMAL) return roll < 72
                ? MageSociety.Role.VILLAIN : MageSociety.Role.WANDERER;
        if (roll < 32) return MageSociety.Role.HOUSEHOLD;
        if (roll < 58) return MageSociety.Role.LICENSED;
        if (roll < 78) return MageSociety.Role.SCHOLAR;
        if (roll < 94) return MageSociety.Role.WARDEN;
        return MageSociety.Role.WANDERER;
    }

    private static MagicTradition naturalAffiliation(UUID uuid, String type) {
        int roll = Math.floorMod(uuid.hashCode(), 100);
        if ("witch".equals(type)) {
            if (roll < 52) return MagicTradition.OCCULT;
            if (roll < 78) return MagicTradition.PRIMAL;
            if (roll < 92) return MagicTradition.UNBOUND;
            return roll < 97 ? MagicTradition.ARCANE : MagicTradition.DIVINE;
        }
        if (roll < 70) return MagicTradition.PRIMAL;
        if (roll < 88) return MagicTradition.OCCULT;
        if (roll < 95) return MagicTradition.UNBOUND;
        return roll < 98 ? MagicTradition.ARCANE : MagicTradition.DIVINE;
    }

    private static MageSociety.Role naturalRole(UUID uuid, String type, MagicTradition affiliation) {
        int roll = Math.floorMod(uuid.toString().hashCode(), 100);
        if (affiliation == MagicTradition.PRIMAL || "evoker".equals(type)) {
            return roll < 78 ? MageSociety.Role.VILLAIN : MageSociety.Role.WARDEN;
        }
        if ("witch".equals(type)) return roll < 45 ? MageSociety.Role.WANDERER
                : roll < 78 ? MageSociety.Role.SCHOLAR : MageSociety.Role.HOUSEHOLD;
        return roll < 55 ? MageSociety.Role.WARDEN : MageSociety.Role.SCHOLAR;
    }

    private static void pushAway(LivingEntity caster, LivingEntity target, double strength) {
        Vec3 delta = target.position().subtract(caster.position());
        Vec3 push = new Vec3(delta.x, 0.0, delta.z);
        if (push.lengthSqr() <= 0.00001) return;
        push = push.normalize().scale(strength);
        target.push(push.x, 0.12, push.z);
    }

    private static boolean ready(Entity entity, long now, int interval) {
        long last = LAST_CAST.getOrDefault(entity.getUUID(), Long.MIN_VALUE / 4);
        int phase = Math.floorMod(entity.getUUID().hashCode(), Math.max(1, interval));
        if ((now + phase) % interval != 0L || now == last) return false;
        LAST_CAST.put(entity.getUUID(), now);
        return true;
    }

    private static String color(MagicTradition affiliation) {
        return switch (affiliation) {
            case ARCANE -> "§9";
            case DIVINE -> "§f";
            case OCCULT -> "§5";
            case PRIMAL -> "§4";
            default -> "§7";
        };
    }

    private static String typePath(Entity entity) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key == null ? "" : key.getPath();
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record MageProfile(int circle, MagicTradition affiliation, MageSociety.Role role) {}
}
