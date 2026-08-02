package kr.moonseungjun.arcanecircle.world;

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

/** Adds persistent circle-ranked mage residents to vanilla villages and ranks vanilla spellcaster enemies. */
public final class ArcaneMageService {
    private static final String MAGE_TAG = "arcanecircle_mage";
    private static final String CIRCLE_PREFIX = "arcanecircle_circle_";
    private static final Set<String> HOSTILE_MAGE_TYPES = Set.of("witch", "evoker", "illusioner");
    private static final Map<UUID, Long> LAST_CAST = new HashMap<>();
    private static final Map<UUID, Integer> CIRCLES = new HashMap<>();
    private static final String NAME_PREFIX = "[마도사:";

    private ArcaneMageService() {}

    public static void tickNear(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || player.isSpectator()) return;
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(48.0),
                villager -> villager.isAlive() && !villager.isBaby() && level.isVillage(villager.blockPosition()));
        ensureVillageResidents(level, villagers);
        for (Villager villager : villagers) {
            if (isMage(villager)) castResidentSpell(level, villager);
        }

        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0),
                value -> value.isAlive() && HOSTILE_MAGE_TYPES.contains(typePath(value)))) {
            ensureHostileMage(level, mob);
            castHostileSpell(level, mob);
        }
        if ((level.getGameTime() & 255L) == 0L) {
            LAST_CAST.entrySet().removeIf(entry -> level.getGameTime() - entry.getValue() > 1200L);
        }
    }

    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getTarget() instanceof Villager villager) || !isMage(villager)) return;
        event.setCanceled(true);
        int circle = circle(villager);
        ArcaneQuestData quests = ArcaneQuestData.get(((ServerLevel) player.level()).getServer());
        ArcaneQuestData.QuestStatus status = quests.status(player);
        if (status.complete()) {
            quests.claim(player);
        } else if (!status.active()) {
            quests.assign(player, circle);
        } else {
            ArcaneNoticeService.push(player, Component.literal("§5[마도사 의뢰] §f" + status.description()
                    + " §d" + status.progress() + "/" + status.target() + " §7· 보상 " + status.reward() + " A"), 90);
        }
        player.sendSystemMessage(Component.literal("§5[" + circle + "써클 마도사] §f학부 조율·주문서·지팡이는 모두 아르카나로 거래합니다."));
        ArcaneNetwork.openPage(player, "academy");
    }

    public static boolean isMage(Entity entity) {
        return CIRCLES.containsKey(entity.getUUID()) || namedCircle(entity) > 0;
    }

    public static int circle(Entity entity) {
        Integer cached = CIRCLES.get(entity.getUUID());
        if (cached != null) return cached;
        int parsed = namedCircle(entity);
        if (parsed > 0) {
            CIRCLES.put(entity.getUUID(), parsed);
            return parsed;
        }
        return 1;
    }

    private static int namedCircle(Entity entity) {
        Component customName = entity.getCustomName();
        if (customName == null) return 0;
        String name = customName.getString();
        int start = name.indexOf(NAME_PREFIX);
        if (start < 0) return 0;
        int end = name.indexOf(']', start + NAME_PREFIX.length());
        if (end < 0) return 0;
        try {
            return Math.max(1, Math.min(9,
                    Integer.parseInt(name.substring(start + NAME_PREFIX.length(), end))));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static void ensureVillageResidents(ServerLevel level, List<Villager> villagers) {
        if (villagers.isEmpty()) return;
        int desired = Math.max(1, Math.min(3, (villagers.size() + 5) / 8));
        long existing = villagers.stream().filter(ArcaneMageService::isMage).count();
        if (existing >= desired) return;
        villagers.stream().filter(v -> !isMage(v))
                .sorted(Comparator.comparingInt(v -> Math.floorMod(v.getUUID().hashCode(), 10000)))
                .limit(desired - existing)
                .forEach(v -> promoteResident(level, v));
    }

    private static void promoteResident(ServerLevel level, Villager villager) {
        long day = Math.max(0L, level.getGameTime() / 24000L);
        int cap = Math.max(1, Math.min(6, 2 + (int) (day / 6L)));
        int circle = 1 + Math.floorMod(villager.getUUID().hashCode(), cap);
        mark(villager, circle);
        villager.setPersistenceRequired();
        villager.setCustomName(Component.literal("§d[마도사:" + circle + "] " + circle + "써클 마도사"));
        villager.setCustomNameVisible(false);
    }

    private static void ensureHostileMage(ServerLevel level, Mob mob) {
        if (isMage(mob)) return;
        int base = switch (typePath(mob)) {
            case "evoker" -> 4;
            case "illusioner" -> 5;
            default -> 2;
        };
        int dayBonus = Math.min(3, (int) Math.max(0L, level.getGameTime() / 24000L / 8L));
        int circle = Math.min(9, base + dayBonus + Math.floorMod(mob.getUUID().hashCode(), 2));
        mark(mob, circle);
        mob.setCustomName(Component.literal("§5[마도사:" + circle + "] " + circle + "써클 " + mob.getName().getString()));
        mob.setCustomNameVisible(false);
    }

    private static void mark(Entity entity, int circle) {
        CIRCLES.put(entity.getUUID(), Math.max(1, Math.min(9, circle)));
    }

    private static void castResidentSpell(ServerLevel level, Villager caster) {
        int circle = circle(caster);
        long now = level.getGameTime();
        if (!ready(caster, now, Math.max(26, 70 - circle * 5))) return;
        Mob target = level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(13.0),
                        value -> value.isAlive() && value instanceof Enemy)
                .stream().min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
        if (target == null) {
            if (caster.getHealth() < caster.getMaxHealth()) {
                caster.heal(Math.max(1.0F, circle * 0.75F));
                level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.NEUTRAL, 0.35F, 1.4F);
            }
            return;
        }
        float damage = 2.0F + circle * 1.4F;
        target.hurtServer(level, level.damageSources().magic(), damage);
        if (circle >= 2) target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30 + circle * 8, Math.min(3, circle / 3)));
        if (circle >= 4) target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
        if (circle >= 5) {
            Vec3 away = target.position().subtract(caster.position());
            Vec3 push = new Vec3(away.x, 0.0, away.z);
            if (push.lengthSqr() > 0.00001) {
                push = push.normalize().scale(0.35 + circle * 0.05);
                target.push(push.x, 0.12, push.z);
            }
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.NEUTRAL, 0.45F, 1.55F - circle * 0.04F);
    }

    private static void castHostileSpell(ServerLevel level, Mob caster) {
        if (!(caster.getTarget() instanceof ServerPlayer target) || !target.isAlive()) return;
        int circle = circle(caster);
        long now = level.getGameTime();
        if (!ready(caster, now, Math.max(24, 76 - circle * 5))) return;
        if (caster.distanceToSqr(target) > 28.0 * 28.0) return;
        target.hurtServer(level, level.damageSources().magic(), 2.5F + circle * 1.35F);
        switch (circle % 4) {
            case 0 -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 18 + circle * 2, 0));
            case 1 -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45 + circle * 5, Math.min(3, circle / 3)));
            case 2 -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50 + circle * 6, Math.min(2, circle / 4)));
            default -> target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 30 + circle * 10));
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.HOSTILE, 0.7F, 1.25F - circle * 0.035F);
    }

    private static boolean ready(Entity entity, long now, int interval) {
        long last = LAST_CAST.getOrDefault(entity.getUUID(), Long.MIN_VALUE / 4);
        int phase = Math.floorMod(entity.getUUID().hashCode(), Math.max(1, interval));
        if ((now + phase) % interval != 0L || now == last) return false;
        LAST_CAST.put(entity.getUUID(), now);
        return true;
    }

    private static String typePath(Entity entity) {
        var key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key == null ? "" : key.getPath();
    }
}
