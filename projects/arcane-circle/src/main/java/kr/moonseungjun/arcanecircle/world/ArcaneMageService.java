package kr.moonseungjun.arcanecircle.world;

import kr.moonseungjun.arcanecircle.magic.ArcaneDamage;
import kr.moonseungjun.arcanecircle.magic.ArcaneFieldService;
import kr.moonseungjun.arcanecircle.magic.ArcaneNoticeService;
import kr.moonseungjun.arcanecircle.magic.RpgScaleService;
import kr.moonseungjun.arcanecircle.magic.SpellCatalog;
import kr.moonseungjun.arcanecircle.magic.SpellDefinition;
import kr.moonseungjun.arcanecircle.magic.WorldMagicService;
import kr.moonseungjun.arcanecircle.network.ArcaneNetwork;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** NPC mages retain aggression, visibly charge a circle, then release their spell. */
public final class ArcaneMageService {
    private static final String NAME_PREFIX = "[마도사:";
    private static final Set<String> SPELLCASTER_TYPES = Set.of("witch", "evoker", "illusioner");
    private static final int[] CIRCLE_WEIGHTS = {59_000, 25_000, 10_000, 4_000, 1_500, 400, 80, 20};
    private static final long RETALIATION_TICKS = 1_200L;

    private static final Map<UUID, Long> LAST_CAST = new HashMap<>();
    private static final Map<UUID, Long> LAST_PROCESSED = new HashMap<>();
    private static final Map<UUID, MageProfile> PROFILES = new HashMap<>();
    private static final Map<UUID, UUID> RETALIATION_TARGET = new HashMap<>();
    private static final Map<UUID, Long> AGGRO_UNTIL = new HashMap<>();
    private static final Map<UUID, NpcCast> CASTS = new HashMap<>();
    private static final Set<UUID> FORCED_CAST = new java.util.HashSet<>();

    private ArcaneMageService() {}

    public static void tickNear(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || player.isSpectator()) return;
        long now = level.getGameTime();
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(72.0), value -> value.isAlive() && !value.isBaby());
        if (now % 100L == Math.floorMod(player.getUUID().hashCode(), 100)) {
            ensureVillageResidents(level, villagers.stream()
                    .filter(value -> level.isVillage(value.blockPosition())).toList());
        }
        for (Villager villager : villagers) {
            if (!isMage(villager) || alreadyProcessed(villager, now)) continue;
            applyMageStats(villager);
            tickResident(level, villager, now);
            namedAura(level, villager);
        }
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(72.0),
                value -> value.isAlive() && !(value instanceof Villager))) {
            if (SPELLCASTER_TYPES.contains(typePath(mob))) ensureNaturalMage(mob);
            if (!isMage(mob) || alreadyProcessed(mob, now)) continue;
            applyMageStats(mob);
            tickHostile(level, mob, now);
            namedAura(level, mob);
        }
        if ((now & 255L) == 0L) cleanup(level, now);
    }

    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(event.getTarget() instanceof Villager villager) || !isMage(villager)) return;
        event.setCanceled(true);
        MageProfile mage = profile(villager);
        ArcaneQuestData.get(((ServerLevel) player.level()).getServer())
                .offer(player, mage.circle(), mage.affiliation());
        player.sendSystemMessage(Component.literal(color(mage.affiliation()) + "[" + mage.circle()
                + "써클 " + mage.role().displayName() + " 마도사] §f" + visibleName(villager)
                + " §7· " + mage.affiliation().displayName()
                + " · 의뢰 난이도와 보상은 고정 등급제로 결정됩니다."));
        ArcaneNetwork.openPage(player, "quests");
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Mob mage) || !isMage(mage)) return;
        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker) || attacker == mage || !attacker.isAlive()) return;
        mage.setTarget(attacker);
        UUID mageId = mage.getUUID();
        RETALIATION_TARGET.put(mageId, attacker.getUUID());
        long now = mage.level() instanceof ServerLevel level ? level.getGameTime() : mage.tickCount;
        AGGRO_UNTIL.put(mageId, now + RETALIATION_TICKS);
        FORCED_CAST.add(mageId);
        LAST_CAST.put(mageId, Long.MIN_VALUE / 4L);
        NpcCast interrupted = CASTS.get(mageId);
        if (interrupted != null && !interrupted.released()) {
            CASTS.remove(mageId);
            WorldMagicService.stop(mage);
        }
    }

    public static void registerNamedMage(Mob entity, int circle, MagicTradition affiliation,
                                         MageSociety.Role role, String name, String id) {
        mark(entity, new MageProfile(clamp(circle, 1, 9), affiliation, role), name);
        entity.addTag("arcanecircle_named_" + id);
        entity.setPersistenceRequired();
        entity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(switch (affiliation) {
            case ARCANE -> Items.AMETHYST_SHARD;
            case DIVINE -> Items.END_ROD;
            case OCCULT -> Items.ECHO_SHARD;
            case PRIMAL -> Items.BLAZE_ROD;
            default -> Items.STICK;
        }));
        entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, true, false));
        AttributeInstance scale = entity.getAttribute(Attributes.SCALE);
        if (scale != null) scale.setBaseValue(Math.min(1.34, 1.10 + circle * 0.025));
        applyMageStats(entity);
    }

    public static boolean isMage(Entity entity) {
        return PROFILES.containsKey(entity.getUUID()) || parseProfile(entity) != null;
    }
    public static int circle(Entity entity) { return profile(entity).circle(); }
    public static MagicTradition affiliation(Entity entity) { return profile(entity).affiliation(); }
    public static MageSociety.Role role(Entity entity) { return profile(entity).role(); }
    public static MagicTradition affiliation(ServerPlayer player) {
        return ArcaneWorldData.get(((ServerLevel) player.level()).getServer()).tradition(player);
    }
    public static boolean autoHostile(Entity left, Entity right) {
        return isMage(left) && isMage(right)
                && MageSociety.hostile(affiliation(left), affiliation(right));
    }

    private static void tickResident(ServerLevel level, Villager caster, long now) {
        MageProfile profile = profile(caster);
        LivingEntity target = findResidentTarget(level, caster, profile, now);
        if (tickCast(level, caster, target, profile, now, false)) return;
        if (target == null) {
            if (caster.getHealth() < caster.getMaxHealth()
                    && (profile.role() == MageSociety.Role.HOUSEHOLD
                    || profile.role() == MageSociety.Role.SCHOLAR)) {
                caster.heal((float) (1.0 + Math.pow(1.45, profile.circle() - 1)));
            }
            return;
        }
        caster.setTarget(target);
        int interval = Math.max(18, (int) Math.round((92 - profile.circle() * 7)
                * profile.affiliation().cooldownMultiplier()));
        if (ready(caster, now, interval)) startCast(level, caster, target, profile, now, false);
    }

    private static void tickHostile(ServerLevel level, Mob caster, long now) {
        MageProfile profile = profile(caster);
        LivingEntity target = rememberedTarget(level, caster, now);
        LivingEntity attacker = recentAttacker(caster);
        if (attacker != null) {
            target = attacker;
            rememberAggro(caster, attacker, now);
        }
        if (target == null) target = caster.getTarget();
        if (tickCast(level, caster, target, profile, now, true)) return;
        if (target == null || !target.isAlive() || caster.distanceToSqr(target) > 36.0 * 36.0) return;
        boolean retaliating = RETALIATION_TARGET.get(caster.getUUID()) != null;
        MagicTradition targetAffiliation = target instanceof ServerPlayer serverPlayer
                ? affiliation(serverPlayer) : isMage(target) ? affiliation(target) : MagicTradition.UNBOUND;
        if (!retaliating && MageSociety.avoidsAutoTarget(profile.affiliation(), targetAffiliation)) {
            caster.setTarget(null);
            return;
        }
        caster.setTarget(target);
        int interval = Math.max(16, (int) Math.round((94 - profile.circle() * 7)
                * profile.affiliation().cooldownMultiplier()));
        if (ready(caster, now, interval)) startCast(level, caster, target, profile, now, true);
    }

    private static void startCast(ServerLevel level, Mob caster, LivingEntity target,
                                  MageProfile profile, long now, boolean hostile) {
        if (target == null || !target.isAlive()) return;
        SpellDefinition visual = chooseCombatSpell(caster, profile);
        int required = Math.max(8, 8 + visual.circle() * 3);
        double range = Math.min(36.0, Math.max(8.0, Math.sqrt(caster.distanceToSqr(target)) + 2.0));
        double power = spellDamage(profile, hostile ? 1.08F : 1.0F)
                * (0.76 + visual.circle() * 0.055);
        CASTS.put(caster.getUUID(), new NpcCast(target.getUUID(), visual.id(), now,
                required, range, power, hostile, false, -1L));
        caster.setTarget(target);
        caster.getLookControl().setLookAt(target, 30.0F, 30.0F);
        WorldMagicService.charge(caster, target, visual, 0.0, range, power);
        level.playSound(null, caster.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                hostile ? SoundSource.HOSTILE : SoundSource.NEUTRAL, 0.55F,
                1.45F - profile.circle() * 0.035F);
    }

    private static boolean tickCast(ServerLevel level, Mob caster, LivingEntity fallbackTarget,
                                    MageProfile profile, long now, boolean hostile) {
        NpcCast cast = CASTS.get(caster.getUUID());
        if (cast == null) return false;
        if (ArcaneFieldService.blocksCasting(caster)) {
            CASTS.remove(caster.getUUID());
            if (cast.released()) WorldMagicService.cancelRelease(caster, cast.spellId());
            WorldMagicService.stop(caster);
            return false;
        }
        Entity rawTarget = level.getEntity(cast.targetId());
        LivingEntity target = rawTarget instanceof LivingEntity living ? living : fallbackTarget;
        if (target == null || !target.isAlive() || caster.distanceToSqr(target) > 48.0 * 48.0) {
            CASTS.remove(caster.getUUID());
            if (!cast.released()) WorldMagicService.stop(caster);
            return false;
        }
        caster.setTarget(target);
        caster.getLookControl().setLookAt(target, 35.0F, 35.0F);
        SpellDefinition spell = SpellCatalog.spell(cast.spellId()).orElseGet(() -> chooseCombatSpell(caster, profile));
        if (cast.released()) {
            if (now < cast.impactAt()) return true;
            finishNpcImpact(level, caster, target, spell, cast, profile, now);
            return true;
        }
        long elapsed = now - cast.startedAt();
        double progress = Math.min(1.0, elapsed / (double) Math.max(1, cast.requiredTicks()));
        WorldMagicService.charge(caster, target, spell, progress, cast.range(), cast.power());
        if (elapsed < cast.requiredTicks()) return true;
        WorldMagicService.release(caster, target, spell, cast.range(), cast.power());
        int impactDelay = NpcSpellResolver.impactDelay(caster, target, spell);
        if (impactDelay > 1) {
            CASTS.put(caster.getUUID(), new NpcCast(cast.targetId(), cast.spellId(), cast.startedAt(), cast.requiredTicks(), cast.range(), cast.power(), cast.hostile(), true, now + impactDelay));
            return true;
        }
        finishNpcImpact(level, caster, target, spell, cast, profile, now);
        return true;
    }

    private static void finishNpcImpact(ServerLevel level, Mob caster, LivingEntity target, SpellDefinition spell, NpcCast cast, MageProfile profile, long now) {
        CASTS.remove(caster.getUUID());
        LAST_CAST.put(caster.getUUID(), now);
        boolean executed = NpcSpellResolver.execute(level, caster, target, spell, cast.range(), cast.power());
        if (executed) { if (target instanceof Mob mob) mob.setTarget(caster); applyControl(caster, target, profile); }
        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, cast.hostile() ? SoundSource.HOSTILE : SoundSource.NEUTRAL, 0.78F, 1.25F - profile.circle() * 0.03F);
        if (RETALIATION_TARGET.containsKey(caster.getUUID())) AGGRO_UNTIL.put(caster.getUUID(), Math.max(AGGRO_UNTIL.getOrDefault(caster.getUUID(), 0L), now + RETALIATION_TICKS / 2L));
    }

    private static LivingEntity findResidentTarget(ServerLevel level, Villager caster,
                                                   MageProfile profile, long now) {
        LivingEntity remembered = rememberedTarget(level, caster, now);
        if (remembered != null && caster.distanceToSqr(remembered) <= 48.0 * 48.0) return remembered;
        LivingEntity assigned = caster.getTarget();
        if (assigned != null && assigned.isAlive() && caster.distanceToSqr(assigned) <= 48.0 * 48.0) {
            return assigned;
        }
        LivingEntity attacker = recentAttacker(caster);
        if (attacker != null && caster.distanceToSqr(attacker) <= 48.0 * 48.0) {
            rememberAggro(caster, attacker, now);
            return attacker;
        }
        LivingEntity hostileMage = level.getEntitiesOfClass(Mob.class,
                caster.getBoundingBox().inflate(18.0), value -> value.isAlive() && isMage(value)
                        && MageSociety.hostile(profile.affiliation(), affiliation(value))).stream()
                .min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
        if (hostileMage != null) return hostileMage;
        Mob enemy = level.getEntitiesOfClass(Mob.class, caster.getBoundingBox().inflate(15.0),
                value -> value.isAlive() && value instanceof Enemy && !isMage(value)).stream()
                .min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
        if (enemy != null) return enemy;
        if (profile.role() != MageSociety.Role.VILLAIN
                && profile.affiliation() != MagicTradition.PRIMAL) return null;
        return level.getEntitiesOfClass(ServerPlayer.class, caster.getBoundingBox().inflate(18.0),
                value -> value.isAlive() && !value.isSpectator()
                        && MageSociety.hostile(profile.affiliation(), affiliation(value))).stream()
                .min(Comparator.comparingDouble(caster::distanceToSqr)).orElse(null);
    }

    private static LivingEntity rememberedTarget(ServerLevel level, Mob caster, long now) {
        UUID id = caster.getUUID();
        if (AGGRO_UNTIL.getOrDefault(id, 0L) <= now) {
            RETALIATION_TARGET.remove(id);
            AGGRO_UNTIL.remove(id);
            return null;
        }
        UUID targetId = RETALIATION_TARGET.get(id);
        Entity target = targetId == null ? null : level.getEntity(targetId);
        if (target instanceof LivingEntity living && living.isAlive()) return living;
        RETALIATION_TARGET.remove(id);
        AGGRO_UNTIL.remove(id);
        return null;
    }

    private static void rememberAggro(Mob caster, LivingEntity target, long now) {
        caster.setTarget(target);
        RETALIATION_TARGET.put(caster.getUUID(), target.getUUID());
        AGGRO_UNTIL.put(caster.getUUID(), now + RETALIATION_TICKS);
    }

    /** High-circle mages favor high magic but deliberately retain mid/low-circle choices. */
    private static SpellDefinition chooseCombatSpell(Mob caster, MageProfile profile) {
        int circle=Math.max(1,Math.min(9,profile.circle()));
        List<SpellDefinition> all=SpellCatalog.spells().values().stream()
                .filter(spell->spell.circle()<=circle)
                .filter(spell->SpellCatalog.isDamaging(spell.id())).toList();
        if(all.isEmpty())return SpellCatalog.spell("magic_missile").orElseThrow();
        int roll=caster.getRandom().nextInt(100),minCircle,maxCircle;
        if(circle>=6&&roll<55){minCircle=Math.max(1,circle-1);maxCircle=circle;}
        else if(circle>=4&&roll<85){minCircle=Math.max(2,circle-4);maxCircle=Math.max(minCircle,circle-2);}
        else{minCircle=1;maxCircle=Math.max(1,circle/2);}
        List<SpellDefinition> band=all.stream()
                .filter(spell->spell.circle()>=minCircle&&spell.circle()<=maxCircle).toList();
        if(band.isEmpty())band=all;
        List<SpellDefinition> themed=band.stream()
                .filter(spell->preferredSchool(profile.affiliation(),spell.school())).toList();
        List<SpellDefinition> candidates=themed.isEmpty()?band:themed;
        return candidates.get(caster.getRandom().nextInt(candidates.size()));
    }

    private static boolean preferredSchool(MagicTradition affiliation, SpellDefinition.School school) {
        return switch(affiliation){
            case ARCANE -> school==SpellDefinition.School.ARCANE||school==SpellDefinition.School.SPACE||school==SpellDefinition.School.WARD;
            case DIVINE -> school==SpellDefinition.School.LIFE||school==SpellDefinition.School.WARD||school==SpellDefinition.School.ARCANE;
            case OCCULT -> school==SpellDefinition.School.SPACE||school==SpellDefinition.School.ARCANE||school==SpellDefinition.School.FROST;
            case PRIMAL -> school==SpellDefinition.School.FIRE||school==SpellDefinition.School.FROST||school==SpellDefinition.School.WIND;
            default -> true;
        };
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
        Component name = entity.getCustomName();
        if (name == null) return null;
        String text = name.getString();
        int start = text.indexOf(NAME_PREFIX);
        if (start < 0) return null;
        int end = text.indexOf(']', start + NAME_PREFIX.length());
        if (end < 0) return null;
        String[] parts = text.substring(start + NAME_PREFIX.length(), end).split(":");
        try {
            return new MageProfile(clamp(Integer.parseInt(parts[0]), 1, 9),
                    parts.length >= 2 ? MagicTradition.parse(parts[1]) : MagicTradition.UNBOUND,
                    parts.length >= 3 ? MageSociety.Role.parse(parts[2]) : MageSociety.Role.WANDERER);
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
                .limit(desired - existing).forEach(ArcaneMageService::promoteResident);
    }

    private static void promoteResident(Villager villager) {
        int circle = weightedCircle(villager.getUUID(), 5);
        MagicTradition affiliation = residentAffiliation(villager.getUUID());
        mark(villager, new MageProfile(circle, affiliation,
                residentRole(villager.getUUID(), affiliation)), null);
        villager.setPersistenceRequired();
        applyMageStats(villager);
    }

    private static void ensureNaturalMage(Mob mob) {
        if (isMage(mob)) return;
        String type = typePath(mob);
        int cap = "illusioner".equals(type) ? 7 : "evoker".equals(type) ? 6 : 5;
        int minimum = "illusioner".equals(type) ? 3 : "evoker".equals(type) ? 2 : 1;
        int circle = Math.max(minimum, weightedCircle(mob.getUUID(), cap));
        MagicTradition affiliation = naturalAffiliation(mob.getUUID(), type);
        mark(mob, new MageProfile(circle, affiliation,
                naturalRole(mob.getUUID(), type, affiliation)), null);
        mob.setPersistenceRequired();
        applyMageStats(mob);
    }

    private static void mark(Entity entity, MageProfile profile, String name) {
        PROFILES.put(entity.getUUID(), profile);
        String suffix = name == null || name.isBlank()
                ? profile.circle() + "써클 " + profile.role().displayName() + " 마도사" : name;
        entity.setCustomName(Component.literal(color(profile.affiliation()) + "[마도사:"
                + profile.circle() + ":" + profile.affiliation().name() + ":"
                + profile.role().name() + "] " + suffix));
        entity.setCustomNameVisible(name != null && !name.isBlank());
    }

    private static void applyMageStats(Mob mage) {
        MageProfile profile = profile(mage);
        RpgScaleService.ensureBaseline(mage);
        RpgScaleService.applyExtraHealth(mage, "mage_c" + profile.circle(),
                Math.pow(1.85, profile.circle() - 1));
        AttributeInstance attack = mage.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null && mage.addTag("arcanecircle_mage_attack")) {
            attack.setBaseValue(Math.min(500.0, Math.max(attack.getBaseValue(), 2.0)
                    * Math.pow(1.28, profile.circle() - 1)));
        }
        if (profile.circle() >= 5) {
            mage.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 50,
                    Math.min(3, (profile.circle() - 4) / 2), true, false));
        }
    }

    private static float spellDamage(MageProfile profile, float base) {
        double role = profile.role() == MageSociety.Role.VILLAIN ? 1.28
                : profile.role() == MageSociety.Role.WARDEN ? 1.12 : 1.0;
        return (float) Math.min(420.0, base * 2.1 * Math.pow(1.72, profile.circle() - 1)
                * role * profile.affiliation().powerMultiplier());
    }

    private static void applyControl(LivingEntity caster, LivingEntity target, MageProfile profile) {
        switch ((profile.circle() + profile.role().ordinal()) % 4) {
            case 0 -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION,
                    16 + profile.circle() * 2, 0));
            case 1 -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                    40 + profile.circle() * 6, Math.min(4, profile.circle() / 2)));
            case 2 -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    45 + profile.circle() * 7, Math.min(3, profile.circle() / 3)));
            default -> target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(),
                    30 + profile.circle() * 12));
        }
        if (profile.circle() >= 5) pushAway(caster, target, 0.22 + profile.circle() * 0.045);
    }

    private static int weightedCircle(UUID uuid, int cap) {
        cap = clamp(cap, 1, 8);
        int total = 0;
        for (int i = 0; i < cap; i++) total += CIRCLE_WEIGHTS[i];
        int roll = Math.floorMod(uuid.hashCode() * 31 + uuid.toString().hashCode(), total);
        int sum = 0;
        for (int i = 0; i < cap; i++) {
            sum += CIRCLE_WEIGHTS[i];
            if (roll < sum) return i + 1;
        }
        return cap;
    }

    private static MagicTradition residentAffiliation(UUID uuid) {
        int roll = Math.floorMod(uuid.hashCode(), 100);
        return roll < 58 ? MagicTradition.ARCANE : roll < 78 ? MagicTradition.DIVINE
                : roll < 96 ? MagicTradition.OCCULT : MagicTradition.PRIMAL;
    }

    private static MageSociety.Role residentRole(UUID uuid, MagicTradition affiliation) {
        int roll = Math.floorMod(uuid.toString().hashCode(), 100);
        if (affiliation == MagicTradition.PRIMAL) {
            return roll < 72 ? MageSociety.Role.VILLAIN : MageSociety.Role.WANDERER;
        }
        return roll < 32 ? MageSociety.Role.HOUSEHOLD : roll < 58 ? MageSociety.Role.LICENSED
                : roll < 78 ? MageSociety.Role.SCHOLAR : roll < 94 ? MageSociety.Role.WARDEN
                : MageSociety.Role.WANDERER;
    }

    private static MagicTradition naturalAffiliation(UUID uuid, String type) {
        int roll = Math.floorMod(uuid.hashCode(), 100);
        if ("witch".equals(type)) {
            return roll < 52 ? MagicTradition.OCCULT : roll < 78 ? MagicTradition.PRIMAL
                    : roll < 92 ? MagicTradition.UNBOUND : roll < 97 ? MagicTradition.ARCANE
                    : MagicTradition.DIVINE;
        }
        return roll < 70 ? MagicTradition.PRIMAL : roll < 88 ? MagicTradition.OCCULT
                : roll < 95 ? MagicTradition.UNBOUND : roll < 98 ? MagicTradition.ARCANE
                : MagicTradition.DIVINE;
    }

    private static MageSociety.Role naturalRole(UUID uuid, String type, MagicTradition affiliation) {
        int roll = Math.floorMod(uuid.toString().hashCode(), 100);
        if (affiliation == MagicTradition.PRIMAL || "evoker".equals(type)) {
            return roll < 78 ? MageSociety.Role.VILLAIN : MageSociety.Role.WARDEN;
        }
        if ("witch".equals(type)) {
            return roll < 45 ? MageSociety.Role.WANDERER : roll < 78 ? MageSociety.Role.SCHOLAR
                    : MageSociety.Role.HOUSEHOLD;
        }
        return roll < 55 ? MageSociety.Role.WARDEN : MageSociety.Role.SCHOLAR;
    }

    private static LivingEntity recentAttacker(LivingEntity caster) {
        LivingEntity attacker = caster.getLastHurtByMob();
        return attacker == null || !attacker.isAlive() || attacker == caster ? null : attacker;
    }

    private static void pushAway(LivingEntity caster, LivingEntity target, double strength) {
        Vec3 delta = target.position().subtract(caster.position());
        Vec3 planar = new Vec3(delta.x, 0.0, delta.z);
        if (planar.lengthSqr() < 0.00001) return;
        planar = planar.normalize().scale(strength);
        target.push(planar.x, 0.12, planar.z);
    }

    private static boolean ready(Entity entity, long now, int interval) {
        UUID id = entity.getUUID();
        if (FORCED_CAST.remove(id)) return true;
        long last = LAST_CAST.computeIfAbsent(id,
                ignored -> now - Math.floorMod(id.hashCode(), Math.max(1, interval)));
        return now - last >= interval;
    }

    private static boolean alreadyProcessed(Entity entity, long now) {
        Long previous = LAST_PROCESSED.put(entity.getUUID(), now);
        return previous != null && previous == now;
    }

    private static void cleanup(ServerLevel level, long now) {
        LAST_CAST.entrySet().removeIf(entry -> now - entry.getValue() > 4_800L);
        LAST_PROCESSED.entrySet().removeIf(entry -> now - entry.getValue() > 600L);
        PROFILES.entrySet().removeIf(entry -> level.getEntity(entry.getKey()) == null);
        RETALIATION_TARGET.entrySet().removeIf(entry -> level.getEntity(entry.getKey()) == null
                || level.getEntity(entry.getValue()) == null);
        AGGRO_UNTIL.entrySet().removeIf(entry -> entry.getValue() <= now
                || level.getEntity(entry.getKey()) == null);
        CASTS.entrySet().removeIf(entry -> level.getEntity(entry.getKey()) == null);
        FORCED_CAST.removeIf(id -> level.getEntity(id) == null);
    }

    private static void namedAura(ServerLevel level, Mob mage) {
        MageProfile profile = profile(mage);
        if (!mage.isCustomNameVisible() || profile.circle() < 7 || mage.tickCount % 5 != 0) return;
        ParticleOptions particle = switch (profile.affiliation()) {
            case ARCANE -> ParticleTypes.ENCHANT;
            case DIVINE -> ParticleTypes.END_ROD;
            case OCCULT -> ParticleTypes.WITCH;
            case PRIMAL -> ParticleTypes.FLAME;
            default -> ParticleTypes.CRIT;
        };
        double angle = (mage.tickCount * 0.17 + profile.circle()) % (Math.PI * 2.0);
        double radius = 0.72 + profile.circle() * 0.025;
        level.sendParticles(particle, mage.getX() + Math.cos(angle) * radius,
                mage.getY() + 0.35 + Math.sin(angle * 2.0) * 0.18,
                mage.getZ() + Math.sin(angle) * radius, 2, 0.02, 0.05, 0.02, 0.01);
    }

    private static String visibleName(Entity entity) {
        String value = entity.getName().getString();
        int split = value.indexOf("] ");
        return split >= 0 ? value.substring(split + 2) : value;
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
    private record NpcCast(UUID targetId, String spellId, long startedAt, int requiredTicks,
                           double range, double power, boolean hostile, boolean released, long impactAt) {}
}
