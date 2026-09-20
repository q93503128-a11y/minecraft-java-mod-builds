package kr.moonseungjun.villageguardians;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creates combat roles from vanilla entity models and equipment. The readable
 * silhouettes and fantasy role language deliberately reference the CC0 Tiny
 * Creatures / Kenney monster sets documented in THIRD_PARTY_NOTICES.
 */
public final class VillageEnemyArchetypeSystem {
    private static final int LONG_EFFECT_TICKS = 20 * 60 * 30;
    private static final Map<UUID, Float> SUPPORT_HEAL_BUDGET = new HashMap<>();

    private VillageEnemyArchetypeSystem() {}

    public record SpawnedEnemy(Mob mob, Archetype archetype, boolean boss) {}

    /** Runtime doctrine for the real flying roster. All roles keep the same Phantom silhouette,
     * while target ownership, cadence and threat priority are authored by VillageRaidSystem. */
    public enum AerialRole {
        RAIDER("하늘 약탈귀", "수호자 급강하"),
        BOMBARDIER("파성 망령", "내부 시설 폭격"),
        HARRIER("폭풍 사냥귀", "고속 수호자 추격");

        private final String displayName;
        private final String combatRole;
        AerialRole(String displayName, String combatRole) {
            this.displayName = displayName;
            this.combatRole = combatRole;
        }
        public String displayName() { return displayName; }
        public String combatRole() { return combatRole; }
    }

    public static SpawnedEnemy create(
            ServerLevel level,
            int day,
            int wave,
            int index,
            boolean boss,
            VillageWaveTrait trait) {
        boolean flying = willSpawnFlying(day, wave, index, boss, trait);
        Archetype archetype = boss ? bossForDay(day)
                : flying ? Archetype.MARKSMAN : select(day, wave, index, trait);
        Mob mob = flying ? EntityTypes.PHANTOM.create(level, EntitySpawnReason.EVENT) : createEntity(level, archetype);
        return mob == null ? null : new SpawnedEnemy(mob, archetype, boss);
    }

    public static Archetype previewArchetype(
            int day, int wave, int index, boolean boss, VillageWaveTrait trait) {
        return boss ? bossForDay(day) : select(day, wave, index, trait);
    }

    public static boolean isFlying(Mob mob) {
        return mob != null && mob.getType() == EntityTypes.PHANTOM;
    }

    /** Shared deterministic predicate used by both the real spawn path and daytime intelligence. */
    public static boolean willSpawnFlying(
            int day, int wave, int index, boolean boss, VillageWaveTrait trait) {
        return !boss && shouldSpawnFlying(day, wave, index, trait);
    }

    /** Deterministic flying doctrine; daytime intel and the real spawn loop call this exact selector. */
    public static AerialRole aerialRole(int day, int wave, int index, VillageWaveTrait trait) {
        if (day < 10) return AerialRole.RAIDER;
        int roll = Math.floorMod(day * 31 + wave * 17 + index * 13, 12);
        if (day >= 13 && (trait == VillageWaveTrait.HUNTERS ? roll >= 6 : roll >= 10)) {
            return AerialRole.HARRIER;
        }
        if (trait == VillageWaveTrait.STORMFRONT ? roll >= 5 : roll >= 8) {
            return AerialRole.BOMBARDIER;
        }
        return AerialRole.RAIDER;
    }

    private static boolean shouldSpawnFlying(int day, int wave, int index, VillageWaveTrait trait) {
        if (day < 7) return false;
        int cadence = trait == VillageWaveTrait.STORMFRONT ? 4 : day >= 13 ? 6 : 9;
        if (trait == VillageWaveTrait.HUNTERS) cadence = Math.min(cadence, 5);
        return Math.floorMod(index + wave * 2 + day, cadence) == 0;
    }

    public static String combatRole(Archetype archetype) {
        return switch (archetype) {
            case GRUNT -> "기본 근접 전열";
            case RUSHER -> "저체력 고기동 척후";
            case BULWARK -> "방패·중장갑 전열";
            case SAPPER -> "시설 집중 폭파";
            case MARKSMAN -> "후방 원거리 사격";
            case SHIELDBREAKER -> "성벽·방패 파쇄";
            case HEXER -> "약화·둔화 주술";
            case WAR_CHANTER -> "주변 적 공격·속도 강화";
            case NECROMANCER -> "회복·보호막 지원";
            case TOWER_HUNTER -> "포탑 교란 원거리";
            case SIEGE_BEAST -> "대형 공성·충격파";
            case IRON_WARLORD -> "중장갑 지휘 보스";
            case PLAGUE_ARCHON -> "독·회복·교란 보스";
            case DREAD_KNIGHT -> "암흑·흡혈 근접 보스";
        };
    }

    public static void configure(
            ServerLevel level,
            Mob mob,
            Archetype archetype,
            VillageWaveTrait trait,
            int day,
            int wave,
            boolean boss) {
        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        if (!isFlying(mob)) equip(mob, archetype);
        applyArchetypeAttributes(mob, archetype, day);
        applyArchetypeEffects(mob, archetype, day, wave);
        trait.applyLongEffects(mob);
        // Siege actors must remain readable, interceptable threats. Campaign/wave haste must not
        // turn structure-focused units or bosses into sprinting missiles.
        if (usesSiegePacing(archetype)) {
            mob.removeEffect(MobEffects.SPEED);
        }
        String visibleName = isFlying(mob)
                ? "§b웨이브 " + wave + " · 하늘 약탈귀 §8[성벽 우회 공중 급습]"
                : displayName(archetype, trait, day, wave, boss);
        mob.setCustomName(Component.literal(visibleName));
        mob.setCustomNameVisible(alwaysShowNameplate(archetype, boss, isFlying(mob)));
        if (isFlying(mob) || boss || archetype == Archetype.NECROMANCER || archetype == Archetype.TOWER_HUNTER) {
            mob.setGlowingTag(true);
        }
        mob.setHealth(mob.getMaxHealth());
        SUPPORT_HEAL_BUDGET.put(mob.getUUID(),
                Math.max(6.0f, mob.getMaxHealth() * (boss ? 0.12f : 0.20f)));
        spawnAura(level, mob, archetype, boss ? 28 : 10);
    }

    public static float structureDamageMultiplier(Archetype archetype) {
        return switch (archetype) {
            case SAPPER -> 1.72f;
            case SHIELDBREAKER -> 1.72f;
            case TOWER_HUNTER -> 1.28f;
            case SIEGE_BEAST -> 2.15f;
            case IRON_WARLORD -> 1.75f;
            case PLAGUE_ARCHON -> 1.32f;
            case DREAD_KNIGHT -> 1.58f;
            default -> 1.0f;
        };
    }

    public static boolean prefersTower(Archetype archetype) {
        return archetype == Archetype.TOWER_HUNTER || archetype == Archetype.PLAGUE_ARCHON;
    }

    public static boolean ignoresNearbyPlayersUntilInside(Archetype archetype) {
        return archetype == Archetype.SAPPER
                || archetype == Archetype.SHIELDBREAKER
                || archetype == Archetype.TOWER_HUNTER
                || archetype == Archetype.SIEGE_BEAST;
    }

    public static boolean isBoss(Archetype archetype) {
        return archetype.ordinal() >= Archetype.SIEGE_BEAST.ordinal();
    }

    public static boolean usesSiegePacing(Archetype archetype) {
        return archetype == Archetype.SAPPER
                || archetype == Archetype.SHIELDBREAKER
                || isBoss(archetype);
    }

    public static boolean isTacticalThreat(Archetype archetype) {
        if (archetype == null) return false;
        return switch (archetype) {
            case SAPPER, SHIELDBREAKER, HEXER, WAR_CHANTER, NECROMANCER, TOWER_HUNTER -> true;
            default -> isBoss(archetype);
        };
    }

    public static boolean alwaysShowNameplate(Archetype archetype, boolean boss, boolean flying) {
        return flying || boss || isTacticalThreat(archetype);
    }

    public static void tickAbility(
            ServerLevel level,
            MinecraftServer server,
            Mob mob,
            Archetype archetype,
            VillageWaveTrait trait,
            int globalTicks) {
        int cadence = trait == VillageWaveTrait.HEXED ? 180 : 220;
        switch (archetype) {
            case HEXER -> {
                if (!abilityReady(mob, globalTicks, cadence)) return;
                for (ServerPlayer player : nearbyPlayers(server, mob, 8.0)) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 45, 0));
                }
                spawnAura(level, mob, archetype, 18);
            }
            case WAR_CHANTER -> {
                if (!abilityReady(mob, globalTicks, 140)) return;
                for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, mob.position(), 9.0, 12, mob.getUUID())) {
                    ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 0));
                    Archetype allyType = VillageRaidSystem.archetypeOf(ally);
                    if (!usesSiegePacing(allyType)) {
                        ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0));
                    }
                }
                spawnAura(level, mob, archetype, 18);
            }
            case NECROMANCER -> {
                if (!abilityReady(mob, globalTicks, 240)) return;
                for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, mob.position(), 10.0, 5, mob.getUUID())) {
                    if (supportHeal(ally, 2.5f + VillageCampaignProgression.effectiveCombatDay(VillageCouncilState.currentDay()) * 0.05f) > 0.0f) {
                        ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, 0));
                    }
                }
                spawnAura(level, mob, archetype, 20);
            }
            case TOWER_HUNTER -> {
                if (!abilityReady(mob, globalTicks, 180)) return;
                int disabledId = VillagePlacedTurretSystem.disableNearestActiveTurret(mob.position(), 48.0, 20 * 7);
                if (disabledId < 0) return;
                spawnAura(level, mob, archetype, 20);
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal("§5[포탑 교란] §f탑 사냥꾼이 배치 포탑 #" + disabledId
                                + "의 사격 회로를 7초간 마비시켰습니다."), false);
            }
            case SIEGE_BEAST -> {
                if (!abilityReady(mob, globalTicks, 100)) return;
                damageAndDebuffPlayers(level, server, mob, 9.5, 4.0f, MobEffects.SLOWNESS);
                spawnAura(level, mob, archetype, 28);
            }
            case IRON_WARLORD -> {
                if (!abilityReady(mob, globalTicks, 120)) return;
                for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, mob.position(), 12.0, 18, mob.getUUID())) {
                    ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 140, 1));
                    ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 140, 0));
                }
                spawnAura(level, mob, archetype, 26);
            }
            case PLAGUE_ARCHON -> {
                if (!abilityReady(mob, globalTicks, 150)) return;
                damageAndDebuffPlayers(level, server, mob, 11.0, 3.5f, MobEffects.POISON);
                for (Mob ally : VillageRaidSystem.activeEnemiesNear(level, mob.position(), 11.0, 6, mob.getUUID())) {
                    supportHeal(ally, 3.0f);
                }
                spawnAura(level, mob, archetype, 28);
            }
            case DREAD_KNIGHT -> {
                if (!abilityReady(mob, globalTicks, 90)) return;
                float drained = 0.0f;
                for (ServerPlayer player : nearbyPlayers(server, mob, 10.0)) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                    player.hurtServer(level, level.damageSources().magic(), 4.5f);
                    drained += 3.0f;
                }
                if (drained > 0.0f) supportHeal(mob, Math.min(12.0f, drained));
                spawnAura(level, mob, archetype, 30);
            }
            default -> {
            }
        }
    }

    public static void onStructureHit(ServerLevel level, Mob mob, Archetype archetype) {
        if (archetype == Archetype.SAPPER) {
            level.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 0.8, mob.getZ(),
                    3, 0.4, 0.3, 0.4, 0.02);
        } else if (archetype == Archetype.SHIELDBREAKER || isBoss(archetype)) {
            level.sendParticles(ParticleTypes.CRIT, mob.getX(), mob.getY() + 0.8, mob.getZ(),
                    10, 0.35, 0.35, 0.35, 0.05);
        }
    }

    private static Archetype select(int day, int wave, int index, VillageWaveTrait trait) {
        int slot = Math.floorMod(index + wave * 3 + day, 24);
        if (trait == VillageWaveTrait.SWARM) {
            return slot % 5 == 0 ? Archetype.SAPPER : slot % 2 == 0 ? Archetype.RUSHER : Archetype.GRUNT;
        }
        if (trait == VillageWaveTrait.IRONCLAD) {
            if (slot % 8 == 0) return Archetype.MARKSMAN;
            return slot % 3 == 0 ? Archetype.SHIELDBREAKER : Archetype.BULWARK;
        }
        if (trait == VillageWaveTrait.SIEGE) {
            if (slot % 6 == 0) return Archetype.SAPPER;
            if (slot % 4 == 0) return Archetype.SHIELDBREAKER;
            return slot % 2 == 0 ? Archetype.BULWARK : Archetype.GRUNT;
        }
        if (trait == VillageWaveTrait.HUNTERS) {
            if (day >= 7 && slot % 8 == 0) return Archetype.TOWER_HUNTER;
            return slot % 3 == 0 ? Archetype.RUSHER : Archetype.MARKSMAN;
        }
        if (trait == VillageWaveTrait.HEXED) {
            if (day >= 9 && slot == 0) return Archetype.NECROMANCER;
            if (slot == 5 || slot == 17) return Archetype.WAR_CHANTER;
            if (slot == 11) return Archetype.HEXER;
            return lineMix(slot);
        }
        if (trait == VillageWaveTrait.FRENZY) {
            if (slot % 7 == 0) return Archetype.WAR_CHANTER;
            return slot % 3 == 0 ? Archetype.GRUNT : Archetype.RUSHER;
        }
        if (trait == VillageWaveTrait.REGENERATING) {
            if (day >= 9 && slot == 0) return Archetype.NECROMANCER;
            if (slot == 12) return Archetype.HEXER;
            return slot % 4 == 0 ? Archetype.RUSHER : slot % 2 == 0 ? Archetype.BULWARK : Archetype.GRUNT;
        }
        if (trait == VillageWaveTrait.PHALANX) {
            if (slot % 8 == 0) return Archetype.WAR_CHANTER;
            if (slot % 5 == 0) return Archetype.MARKSMAN;
            return slot % 3 == 0 ? Archetype.SHIELDBREAKER : Archetype.BULWARK;
        }
        if (trait == VillageWaveTrait.BLOOD_MOON) {
            if (slot % 8 == 0) return Archetype.WAR_CHANTER;
            if (slot % 6 == 0) return Archetype.SHIELDBREAKER;
            return slot % 3 == 0 ? Archetype.GRUNT : Archetype.RUSHER;
        }
        if (trait == VillageWaveTrait.STORMFRONT) {
            if (day >= 11 && slot == 0) return Archetype.TOWER_HUNTER;
            if (slot == 13) return Archetype.HEXER;
            return slot % 4 == 0 ? Archetype.RUSHER
                    : slot % 3 == 0 ? Archetype.BULWARK : Archetype.MARKSMAN;
        }
        if (trait == VillageWaveTrait.RIFTED) {
            if (slot == 0) return Archetype.NECROMANCER;
            if (slot == 12) return Archetype.HEXER;
            if (slot % 5 == 0) return Archetype.MARKSMAN;
            return slot % 3 == 0 ? Archetype.BULWARK
                    : slot % 2 == 0 ? Archetype.SHIELDBREAKER : Archetype.RUSHER;
        }

        if (day >= 11 && slot == 0) return Archetype.NECROMANCER;
        if (day >= 9 && slot == 3) return Archetype.TOWER_HUNTER;
        if (day >= 8 && slot == 6) return Archetype.WAR_CHANTER;
        if (day >= 6 && slot == 11) return Archetype.HEXER;
        if (day >= 5 && slot == 14) return Archetype.SHIELDBREAKER;
        if (day >= 4 && slot == 17) return Archetype.SAPPER;
        return lineMix(slot);
    }

    private static Archetype lineMix(int slot) {
        return switch (Math.floorMod(slot, 6)) {
            case 0 -> Archetype.RUSHER;
            case 1, 5 -> Archetype.GRUNT;
            case 2 -> Archetype.BULWARK;
            case 3 -> Archetype.MARKSMAN;
            default -> Archetype.GRUNT;
        };
    }

    private static Archetype bossForDay(int day) {
        int cycle = Math.floorMod(Math.max(3, day) - 3, 4);
        return switch (cycle) {
            case 0 -> Archetype.SIEGE_BEAST;
            case 1 -> Archetype.IRON_WARLORD;
            case 2 -> Archetype.PLAGUE_ARCHON;
            default -> Archetype.DREAD_KNIGHT;
        };
    }

    private static Mob createEntity(ServerLevel level, Archetype archetype) {
        return switch (archetype) {
            case GRUNT -> EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
            case RUSHER, SAPPER -> EntityTypes.ZOMBIE.create(level, EntitySpawnReason.EVENT);
            case BULWARK -> EntityTypes.HUSK.create(level, EntitySpawnReason.EVENT);
            case MARKSMAN -> EntityTypes.SKELETON.create(level, EntitySpawnReason.EVENT);
            case SHIELDBREAKER -> EntityTypes.VINDICATOR.create(level, EntitySpawnReason.EVENT);
            case HEXER -> EntityTypes.WITCH.create(level, EntitySpawnReason.EVENT);
            case WAR_CHANTER -> EntityTypes.PILLAGER.create(level, EntitySpawnReason.EVENT);
            case NECROMANCER -> EntityTypes.EVOKER.create(level, EntitySpawnReason.EVENT);
            case TOWER_HUNTER -> EntityTypes.STRAY.create(level, EntitySpawnReason.EVENT);
            case SIEGE_BEAST -> EntityTypes.RAVAGER.create(level, EntitySpawnReason.EVENT);
            case IRON_WARLORD -> EntityTypes.VINDICATOR.create(level, EntitySpawnReason.EVENT);
            case PLAGUE_ARCHON -> EntityTypes.EVOKER.create(level, EntitySpawnReason.EVENT);
            case DREAD_KNIGHT -> EntityTypes.WITHER_SKELETON.create(level, EntitySpawnReason.EVENT);
        };
    }

    private static void equip(Mob mob, Archetype archetype) {
        if (mob instanceof Zombie zombie) {
            // finalizeSpawn may randomly turn an ordinary GRUNT into a vanilla baby/chicken-jockey.
            // Raid silhouettes are authored, not random: only RUSHER/SAPPER are babies, and no raid
            // zombie is allowed to retain a vanilla mount after finalizeSpawn.
            zombie.stopRiding();
            zombie.setBaby(archetype == Archetype.RUSHER || archetype == Archetype.SAPPER);
        }
        switch (archetype) {
            case GRUNT -> mob.setItemSlot(EquipmentSlot.MAINHAND, Items.STONE_SWORD.getDefaultInstance());
            case RUSHER -> mob.setItemSlot(EquipmentSlot.MAINHAND, Items.WOODEN_SWORD.getDefaultInstance());
            case BULWARK -> {
                mob.setItemSlot(EquipmentSlot.HEAD, Items.IRON_HELMET.getDefaultInstance());
                mob.setItemSlot(EquipmentSlot.CHEST, Items.IRON_CHESTPLATE.getDefaultInstance());
                mob.setItemSlot(EquipmentSlot.OFFHAND, Items.SHIELD.getDefaultInstance());
            }
            case SAPPER -> {
                mob.setItemSlot(EquipmentSlot.HEAD, Items.TNT.getDefaultInstance());
                mob.setItemSlot(EquipmentSlot.MAINHAND, Items.FLINT_AND_STEEL.getDefaultInstance());
            }
            case MARKSMAN -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, Items.BOW.getDefaultInstance());
                mob.setItemSlot(EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET.getDefaultInstance());
            }
            case SHIELDBREAKER -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, Items.IRON_AXE.getDefaultInstance());
                mob.setItemSlot(EquipmentSlot.HEAD, Items.IRON_HELMET.getDefaultInstance());
            }
            case HEXER -> mob.setItemSlot(EquipmentSlot.MAINHAND, Items.AMETHYST_SHARD.getDefaultInstance());
            case WAR_CHANTER -> mob.setItemSlot(EquipmentSlot.MAINHAND, Items.GOAT_HORN.getDefaultInstance());
            case NECROMANCER -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, Items.BONE.getDefaultInstance());
                mob.setItemSlot(EquipmentSlot.HEAD, Items.WITHER_SKELETON_SKULL.getDefaultInstance());
            }
            case TOWER_HUNTER -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, Items.CROSSBOW.getDefaultInstance());
                mob.setItemSlot(EquipmentSlot.HEAD, Items.CHAINMAIL_HELMET.getDefaultInstance());
            }
            case IRON_WARLORD -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, Items.DIAMOND_AXE.getDefaultInstance());
                mob.setItemSlot(EquipmentSlot.HEAD, Items.NETHERITE_HELMET.getDefaultInstance());
                mob.setItemSlot(EquipmentSlot.CHEST, Items.NETHERITE_CHESTPLATE.getDefaultInstance());
            }
            case PLAGUE_ARCHON -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, Items.NETHER_STAR.getDefaultInstance());
                mob.setItemSlot(EquipmentSlot.HEAD, Items.WITHER_SKELETON_SKULL.getDefaultInstance());
            }
            case DREAD_KNIGHT -> {
                mob.setItemSlot(EquipmentSlot.MAINHAND, Items.NETHERITE_SWORD.getDefaultInstance());
                mob.setItemSlot(EquipmentSlot.HEAD, Items.NETHERITE_HELMET.getDefaultInstance());
                mob.setItemSlot(EquipmentSlot.CHEST, Items.NETHERITE_CHESTPLATE.getDefaultInstance());
            }
            default -> {
            }
        }
    }

    private static void applyArchetypeAttributes(Mob mob, Archetype archetype, int day) {
        if (isFlying(mob)) {
            var health = mob.getAttribute(Attributes.MAX_HEALTH);
            float pacedDay = VillageCampaignProgression.effectiveCombatDay(day);
            if (health != null) health.setBaseValue(Math.min(52.0, 18.0 + Math.max(0.0f, pacedDay - 7.0f) * 0.72));
            var attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) attack.setBaseValue(Math.min(9.0, 3.0 + Math.max(0.0f, pacedDay - 7.0f) * 0.11));
            return;
        }
        if (archetype == Archetype.RUSHER) {
            var health = mob.getAttribute(Attributes.MAX_HEALTH);
            float pacedDay = VillageCampaignProgression.effectiveCombatDay(day);
            if (health != null) health.setBaseValue(Math.min(30.0, 11.0 + Math.max(0.0f, pacedDay - 1.0f) * 0.34));
            var attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) attack.setBaseValue(Math.min(5.0, 1.5 + Math.max(0.0f, pacedDay - 1.0f) * 0.065));
            var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.19);
        } else if (archetype == Archetype.SAPPER) {
            // Sappers stay threatening through objective priority, not raw sprint speed.
            var health = mob.getAttribute(Attributes.MAX_HEALTH);
            float pacedDay = VillageCampaignProgression.effectiveCombatDay(day);
            if (health != null) health.setBaseValue(Math.min(24.0, 8.5 + Math.max(0.0f, pacedDay - 1.0f) * 0.24));
            var attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) attack.setBaseValue(Math.min(4.0, 1.25 + Math.max(0.0f, pacedDay - 1.0f) * 0.045));
            var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.15);
        } else if (archetype == Archetype.SHIELDBREAKER) {
            var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.19);
        } else if (archetype == Archetype.SIEGE_BEAST) {
            var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.16);
        } else if (archetype == Archetype.IRON_WARLORD) {
            var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.18);
        } else if (archetype == Archetype.PLAGUE_ARCHON) {
            var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.17);
        } else if (archetype == Archetype.DREAD_KNIGHT) {
            var speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(0.20);
        }
    }

    private static void applyArchetypeEffects(Mob mob, Archetype archetype, int day, int wave) {
        switch (archetype) {
            case RUSHER -> { }
            case BULWARK -> {
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG_EFFECT_TICKS, 1));
                mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, LONG_EFFECT_TICKS, 0));
            }
            case SAPPER -> { }
            case SHIELDBREAKER -> mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, LONG_EFFECT_TICKS, 1));
            case WAR_CHANTER -> mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG_EFFECT_TICKS, 0));
            case NECROMANCER -> mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG_EFFECT_TICKS, 0));
            case TOWER_HUNTER -> mob.addEffect(new MobEffectInstance(MobEffects.SPEED, LONG_EFFECT_TICKS, 1));
            case SIEGE_BEAST -> {
                mob.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, LONG_EFFECT_TICKS, 5));
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, LONG_EFFECT_TICKS, 2));
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG_EFFECT_TICKS, 1));
            }
            case IRON_WARLORD -> {
                mob.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, LONG_EFFECT_TICKS, 6));
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, LONG_EFFECT_TICKS, 3));
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG_EFFECT_TICKS, 2));
            }
            case PLAGUE_ARCHON -> {
                mob.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, LONG_EFFECT_TICKS, 5));
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG_EFFECT_TICKS, 2));
                mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 8, 1));
            }
            case DREAD_KNIGHT -> {
                mob.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, LONG_EFFECT_TICKS, 7));
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, LONG_EFFECT_TICKS, 3));
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, LONG_EFFECT_TICKS, 2));
            }
            default -> {
            }
        }
        if (day >= 12 && !isBoss(archetype) && (wave + day) % 3 == 0) {
            mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, LONG_EFFECT_TICKS, Math.min(3, day / 10)));
        }
    }

    private static String displayName(
            Archetype archetype,
            VillageWaveTrait trait,
            int day,
            int wave,
            boolean boss) {
        if (boss) return "§4" + archetype.displayName() + " §7· 제 " + day + "일";
        return archetype.color() + "웨이브 " + wave + " · " + archetype.displayName()
                + " §8[" + trait.displayName() + "]";
    }

    private static List<ServerPlayer> nearbyPlayers(MinecraftServer server, Mob mob, double radius) {
        double squared = radius * radius;
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> player.level() == mob.level()
                        && player.isAlive()
                        && !player.isSpectator()
                        && !VillageRespawnSystem.isDowned(player)
                        && player.distanceToSqr(mob) <= squared)
                .toList();
    }

    private static boolean abilityReady(Mob mob, int globalTicks, int cadence) {
        int safeCadence = Math.max(1, cadence);
        int phase = Math.floorMod(mob.getUUID().hashCode(), safeCadence);
        return Math.floorMod(globalTicks + phase, safeCadence) == 0;
    }

    public static void forget(UUID uuid) {
        if (uuid != null) SUPPORT_HEAL_BUDGET.remove(uuid);
    }

    public static void resetRaidState() {
        SUPPORT_HEAL_BUDGET.clear();
    }

    private static float supportHeal(Mob ally, float requested) {
        if (ally == null || !ally.isAlive() || requested <= 0.0f) return 0.0f;
        float missing = Math.max(0.0f, ally.getMaxHealth() - ally.getHealth());
        if (missing <= 0.0f) return 0.0f;
        float remaining = SUPPORT_HEAL_BUDGET.getOrDefault(
                ally.getUUID(), Math.max(6.0f, ally.getMaxHealth() * 0.16f));
        if (remaining <= 0.0f) return 0.0f;
        float applied = Math.min(requested, Math.min(missing, remaining));
        if (applied <= 0.0f) return 0.0f;
        ally.heal(applied);
        SUPPORT_HEAL_BUDGET.put(ally.getUUID(), Math.max(0.0f, remaining - applied));
        return applied;
    }

    private static void damageAndDebuffPlayers(
            ServerLevel level,
            MinecraftServer server,
            Mob mob,
            double radius,
            float damage,
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        for (ServerPlayer player : nearbyPlayers(server, mob, radius)) {
            player.addEffect(new MobEffectInstance(effect, 90, 1));
            float endlessBonus = Math.min(7.0f,
                    VillageCampaignProgression.effectiveCombatDay(VillageCouncilState.currentDay()) * 0.16f);
            player.hurtServer(level, level.damageSources().magic(), damage + endlessBonus);
        }
    }

    private static void spawnAura(ServerLevel level, Mob mob, Archetype archetype, int count) {
        if (isFlying(mob)) {
            level.sendParticles(ParticleTypes.END_ROD, mob.getX(), mob.getY(), mob.getZ(),
                    Math.max(6, count), 0.7, 0.35, 0.7, 0.035);
            return;
        }
        var particle = switch (archetype) {
            case SAPPER, SIEGE_BEAST -> ParticleTypes.SMOKE;
            case HEXER, NECROMANCER, PLAGUE_ARCHON -> ParticleTypes.WITCH;
            case WAR_CHANTER, IRON_WARLORD -> ParticleTypes.CRIT;
            case TOWER_HUNTER, DREAD_KNIGHT -> ParticleTypes.SOUL_FIRE_FLAME;
            case BULWARK, SHIELDBREAKER -> ParticleTypes.DAMAGE_INDICATOR;
            case MARKSMAN -> ParticleTypes.ENCHANT;
            default -> ParticleTypes.CLOUD;
        };
        level.sendParticles(particle, mob.getX(), mob.getY() + mob.getBbHeight() * 0.65, mob.getZ(),
                count, 0.45, 0.55, 0.45, 0.04);
    }

    public enum Archetype {
        GRUNT("오크 보병", "§c"),
        RUSHER("고블린 척후병", "§a"),
        BULWARK("버그베어 방패병", "§7"),
        SAPPER("코볼트 폭파병", "§6"),
        MARKSMAN("망령 장궁병", "§f"),
        SHIELDBREAKER("성벽 파쇄병", "§4"),
        HEXER("흑주술사", "§5"),
        WAR_CHANTER("전쟁 고수", "§e"),
        NECROMANCER("리치 사제", "§d"),
        TOWER_HUNTER("탑 사냥꾼", "§3"),
        SIEGE_BEAST("공성 거수", "§4"),
        IRON_WARLORD("철갑 전쟁군주", "§4"),
        PLAGUE_ARCHON("역병 대주술사", "§4"),
        DREAD_KNIGHT("검은 성문기사", "§4");

        private final String displayName;
        private final String color;

        Archetype(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String displayName() { return displayName; }
        public String color() { return color; }
    }
}
