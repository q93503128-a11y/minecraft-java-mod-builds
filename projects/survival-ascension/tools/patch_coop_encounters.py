from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java/kr/moonseungjun/survivalascension"


def read(rel):
    return (JAVA / rel).read_text(encoding="utf-8")


def write(rel, text):
    (JAVA / rel).write_text(text, encoding="utf-8")


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 anchor, got {count}")
    return text.replace(old, new, 1)


def replace_section(text, start, end, new, label):
    a = text.find(start)
    if a < 0:
        raise SystemExit(f"{label}: start anchor missing")
    b = text.find(end, a)
    if b < 0:
        raise SystemExit(f"{label}: end anchor missing")
    return text[:a] + new.rstrip() + "\n\n" + text[b:]


# -----------------------------------------------------------------------------
# Elite / mythic: true shared field mini-boss presentation and rewards.
# -----------------------------------------------------------------------------
rel = "elite/EliteMobSystem.java"
t = read(rel)
t = replace_once(t,
'''import net.minecraft.nbt.CompoundTag;\nimport net.minecraft.network.chat.Component;''',
'''import net.minecraft.core.particles.ParticleTypes;\nimport net.minecraft.nbt.CompoundTag;\nimport net.minecraft.network.chat.Component;''', "elite particle import")
t = replace_once(t,
'''import net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.ServerPlayer;''',
'''import net.minecraft.server.level.ServerBossEvent;\nimport net.minecraft.server.level.ServerLevel;\nimport net.minecraft.server.level.ServerPlayer;''', "elite boss import")
t = replace_once(t,
'''import net.minecraft.world.entity.Mob;\nimport net.minecraft.world.entity.ai.attributes.AttributeInstance;''',
'''import net.minecraft.world.BossEvent;\nimport net.minecraft.world.effect.MobEffectInstance;\nimport net.minecraft.world.effect.MobEffects;\nimport net.minecraft.world.entity.Mob;\nimport net.minecraft.world.entity.ai.attributes.AttributeInstance;''', "elite effects import")
t = replace_once(t,
'''import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;\nimport net.neoforged.neoforge.event.entity.living.LivingDamageEvent;\nimport net.neoforged.neoforge.event.entity.living.LivingDeathEvent;\n\nimport java.util.List;''',
'''import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;\nimport net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;\nimport net.neoforged.neoforge.event.entity.living.LivingDamageEvent;\nimport net.neoforged.neoforge.event.entity.living.LivingDeathEvent;\nimport net.neoforged.neoforge.event.tick.ServerTickEvent;\n\nimport java.util.ArrayList;\nimport java.util.HashMap;\nimport java.util.HashSet;\nimport java.util.List;\nimport java.util.Map;\nimport java.util.Set;\nimport java.util.UUID;''', "elite util imports")
t = replace_once(t,
'''    private static final String REACTION_READY_KEY = "survivalascension_elite_reaction_ready";''',
'''    private static final String REACTION_READY_KEY = "survivalascension_elite_reaction_ready";\n    private static final String MYTHIC_PHASE_KEY = "survivalascension_mythic_phase";\n    private static final double MYTHIC_ALERT_RADIUS = 192.0D;\n    private static final double MYTHIC_BOSSBAR_RADIUS = 128.0D;\n    private static final double MYTHIC_REWARD_RADIUS = 48.0D;''', "elite mythic constants")
t = replace_once(t,
'''    private static final Identifier TRAIT_KNOCKBACK_ID = id("elite_trait_knockback");''',
'''    private static final Identifier TRAIT_KNOCKBACK_ID = id("elite_trait_knockback");\n    private static final Identifier MYTHIC_COOP_HEALTH_ID = id("mythic_coop_health");\n    private static final Map<UUID, MythicRuntime> MYTHICS = new HashMap<>();\n    private static int mythicTicker;''', "elite runtime fields")

t = replace_section(t,
"    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {",
"    public static void onDamagePre(LivingDamageEvent.Pre event) {",
'''    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (FinalAscensionBossSystem.isInternalSpawn()) return;
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy) || mob instanceof EnderDragon || mob instanceof WitherBoss || mob.isBaby()) return;
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (isElite(mob)) return;
        if (event.getSpawnType().name().contains("SPAWNER")) return;

        List<ServerPlayer> nearby = level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(96.0D),
                player -> !player.isSpectator());
        if (nearby.isEmpty()) return;

        double power = nearby.stream().mapToDouble(EliteMobSystem::averageSkillLevel).average().orElse(0.0D);
        int worldStage = WorldAscensionData.get(level.getServer()).stage();
        RandomSource random = level.getRandom();
        double eliteChance = Math.min(0.28D, 0.025D + power * 0.00135D + worldStage * 0.04D);
        if (random.nextDouble() >= eliteChance) return;

        Rank rank = chooseRank(random, power, worldStage);
        Trait trait = Trait.values()[random.nextInt(Trait.values().length)];
        applyElite(mob, rank, trait, nearby.size());

        if (rank == Rank.MYTHIC_III) {
            MythicRuntime runtime = ensureMythicRuntime(mob);
            for (ServerPlayer viewer : playersNear(level, mob, MYTHIC_ALERT_RADIUS)) {
                runtime.contributors.add(viewer.getUUID());
                viewer.sendSystemMessage(Component.literal("§6§l[신화 III 출현] §r§f" + mob.getName().getString()
                        + " §7· §e" + directionLabel(viewer, mob) + " " + (int)Math.round(Math.sqrt(viewer.distanceToSqr(mob))) + "m"
                        + " §7· §fX " + mob.blockPosition().getX() + " Z " + mob.blockPosition().getZ()));
            }
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel && rank(mob) == Rank.MYTHIC_III) {
            mob.setPersistenceRequired();
            mob.setGlowingTag(true);
            ensureMythicRuntime(mob);
        }
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        if (++mythicTicker < 10) return;
        mythicTicker = 0;
        if (MYTHICS.isEmpty()) return;
        List<UUID> remove = new ArrayList<>();
        for (Map.Entry<UUID, MythicRuntime> entry : new ArrayList<>(MYTHICS.entrySet())) {
            MythicRuntime runtime = entry.getValue();
            if (runtime.level.getServer() != event.getServer()) continue;
            Entity entity = runtime.level.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive() || rank(mob) != Rank.MYTHIC_III) {
                closeMythicBar(runtime);
                remove.add(entry.getKey());
                continue;
            }
            mob.setGlowingTag(true);
            mob.setPersistenceRequired();
            syncMythicBossBar(runtime, mob);
            int phase = mob.getPersistentData().getIntOr(MYTHIC_PHASE_KEY, 0);
            if (phase >= 1) mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 30, phase >= 2 ? 1 : 0, true, false));
            if (phase >= 2) {
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 30, 1, true, false));
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 30, 0, true, false));
            }
            if (runtime.level.getGameTime() % 20L == 0L) {
                runtime.level.sendParticles(phase >= 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.TOTEM_OF_UNDYING,
                        mob.getX(), mob.getY() + mob.getBbHeight() * 0.6D, mob.getZ(), phase >= 2 ? 22 : 12,
                        0.55D, 0.8D, 0.55D, 0.02D);
            }
        }
        for (UUID id : remove) MYTHICS.remove(id);
    }''', "elite spawn/tick")

t = replace_section(t,
"    public static void onDamagePost(LivingDamageEvent.Post event) {",
"    public static void onLivingDeath(LivingDeathEvent event) {",
'''    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof Mob attacker
                && event.getEntity() instanceof ServerPlayer
                && isElite(attacker)
                && trait(attacker) == Trait.VAMPIRIC
                && event.getHealthDamage() > 0.0F
                && attacker.isAlive()) {
            float fraction = switch (rank(attacker)) {
                case ELITE_I -> 0.18F;
                case ASCENDED_II -> 0.28F;
                case MYTHIC_III -> 0.48F;
                default -> 0.0F;
            };
            float heal = Math.min(attacker.getMaxHealth() * 0.10F, event.getHealthDamage() * fraction);
            if (heal > 0.0F) attacker.heal(heal);
        }

        if (event.getEntity() instanceof Mob defender
                && defender.level() instanceof ServerLevel level
                && isElite(defender)
                && defender.isAlive()
                && event.getHealthDamage() > 0.0F) {
            ServerPlayer player = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer
                    ? sourcePlayer : AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level);
            if (player != null) {
                reactToPlayerHit(defender, player);
                if (rank(defender) == Rank.MYTHIC_III) {
                    ensureMythicRuntime(defender).contributors.add(player.getUUID());
                    updateMythicPhase(defender);
                }
            }
        }
    }''', "elite damage post")

t = replace_section(t,
"    public static void onLivingDeath(LivingDeathEvent event) {",
"    public static boolean isElite(net.minecraft.world.entity.LivingEntity entity) {",
'''    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !isElite(event.getEntity()) || !(event.getEntity().level() instanceof ServerLevel level)) return;
        ServerPlayer killer = event.getSource().getEntity() instanceof ServerPlayer sourcePlayer
                ? sourcePlayer : AscensionAffixes.rangedProjectileOwner(event.getSource().getDirectEntity(), level);
        Rank rank = rank(event.getEntity());

        if (rank == Rank.MYTHIC_III && event.getEntity() instanceof Mob mob) {
            MythicRuntime runtime = MYTHICS.remove(mob.getUUID());
            Set<UUID> recipients = new HashSet<>();
            if (runtime != null) recipients.addAll(runtime.contributors);
            if (killer != null) recipients.add(killer.getUUID());
            for (ServerPlayer player : playersNear(level, mob, MYTHIC_REWARD_RADIUS)) recipients.add(player.getUUID());
            if (runtime != null) closeMythicBar(runtime);
            dropRankReward(level, mob, rank);
            for (UUID id : recipients) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
                if (player == null || player.level() != level) continue;
                player.giveExperiencePoints(90);
                giveOrDrop(player, new ItemStack(Items.DIAMOND, 1));
                giveOrDrop(player, new ItemStack(Items.EMERALD, 2 + level.getRandom().nextInt(3)));
                giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, 1));
                player.sendSystemMessage(Component.literal("§6[신화 공동 격파] §f경험치 §e+90 §7· 다이아1 · 에메랄드2~4 · 메아리1"));
            }
            return;
        }

        if (killer == null) return;
        int vanillaXp = switch (rank) {
            case ELITE_I -> 8;
            case ASCENDED_II -> 24;
            case MYTHIC_III -> 60;
            default -> 0;
        };
        if (vanillaXp > 0) killer.giveExperiencePoints(vanillaXp);
        if (event.getEntity() instanceof Mob mob) dropRankReward(level, mob, rank);
    }''', "elite death")

t = replace_section(t,
"    private static Rank chooseRank(RandomSource random, double power, int worldStage) {",
"    private static void applyElite(Mob mob, Rank rank, Trait trait) {",
'''    private static Rank chooseRank(RandomSource random, double power, int worldStage) {
        double roll = random.nextDouble();
        double mythicChance = Math.min(0.08D, 0.004D + power * 0.00035D + worldStage * 0.012D);
        double ascendedChance = Math.min(0.52D, 0.11D + power * 0.0032D + worldStage * 0.05D);
        if (roll < mythicChance) return Rank.MYTHIC_III;
        if (roll < mythicChance + ascendedChance) return Rank.ASCENDED_II;
        return Rank.ELITE_I;
    }''', "elite choose rank")

t = replace_section(t,
"    private static void applyElite(Mob mob, Rank rank, Trait trait) {",
"    private static void addPermanent(AttributeInstance attribute, Identifier id, double amount, AttributeModifier.Operation operation) {",
'''    private static void applyElite(Mob mob, Rank rank, Trait trait, int nearbyPlayers) {
        CompoundTag data = mob.getPersistentData();
        data.putInt(RANK_KEY, rank.id);
        data.putString(TRAIT_KEY, trait.id);

        addPermanent(mob.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, rank.healthBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addPermanent(mob.getAttribute(Attributes.ARMOR), ARMOR_ID, rank.armorBonus, AttributeModifier.Operation.ADD_VALUE);
        addPermanent(mob.getAttribute(Attributes.MOVEMENT_SPEED), SPEED_ID, rank.speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addPermanent(mob.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, rank.attackBonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addPermanent(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_ID, rank.knockbackBonus, AttributeModifier.Operation.ADD_VALUE);
        if (rank == Rank.MYTHIC_III) {
            int extraPlayers = Math.min(4, Math.max(0, nearbyPlayers - 1));
            addPermanent(mob.getAttribute(Attributes.MAX_HEALTH), MYTHIC_COOP_HEALTH_ID, extraPlayers * 0.70D,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            mob.setPersistenceRequired();
            mob.setGlowingTag(true);
        }

        switch (trait) {
            case SWIFT -> addPermanent(mob.getAttribute(Attributes.MOVEMENT_SPEED), TRAIT_SPEED_ID,
                    switch (rank) { case ELITE_I -> 0.12D; case ASCENDED_II -> 0.18D; case MYTHIC_III -> 0.34D; default -> 0.0D; },
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case BULWARK -> {
                addPermanent(mob.getAttribute(Attributes.ARMOR), TRAIT_ARMOR_ID,
                        switch (rank) { case ELITE_I -> 2.0D; case ASCENDED_II -> 4.0D; case MYTHIC_III -> 12.0D; default -> 0.0D; },
                        AttributeModifier.Operation.ADD_VALUE);
                addPermanent(mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE), TRAIT_KNOCKBACK_ID,
                        switch (rank) { case ELITE_I -> 0.10D; case ASCENDED_II -> 0.20D; case MYTHIC_III -> 0.55D; default -> 0.0D; },
                        AttributeModifier.Operation.ADD_VALUE);
            }
            case BERSERKER -> addPermanent(mob.getAttribute(Attributes.ATTACK_DAMAGE), TRAIT_ATTACK_ID,
                    switch (rank) { case ELITE_I -> 0.08D; case ASCENDED_II -> 0.15D; case MYTHIC_III -> 0.45D; default -> 0.0D; },
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case VAMPIRIC -> { }
        }

        mob.setHealth(mob.getMaxHealth());
        if (mob.getCustomName() == null) {
            String prefix = switch (rank) {
                case ELITE_I -> "§b[정예 I] ";
                case ASCENDED_II -> "§d[승천 II] ";
                case MYTHIC_III -> "§6§l[신화 III] §r";
                default -> "";
            };
            mob.setCustomName(Component.literal(prefix + trait.koreanName + " " + mob.getName().getString()));
            mob.setCustomNameVisible(rank == Rank.MYTHIC_III);
        }
        if (rank == Rank.MYTHIC_III) ensureMythicRuntime(mob);
    }

    private static MythicRuntime ensureMythicRuntime(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) throw new IllegalStateException("mythic mob must be server-side");
        return MYTHICS.computeIfAbsent(mob.getUUID(), id -> new MythicRuntime(level, mob));
    }

    private static void updateMythicPhase(Mob mob) {
        int oldPhase = mob.getPersistentData().getIntOr(MYTHIC_PHASE_KEY, 0);
        float ratio = mob.getHealth() / Math.max(1.0F, mob.getMaxHealth());
        int newPhase = ratio <= 0.33F ? 2 : ratio <= 0.66F ? 1 : 0;
        if (newPhase <= oldPhase || !(mob.level() instanceof ServerLevel level)) return;
        mob.getPersistentData().putInt(MYTHIC_PHASE_KEY, newPhase);
        level.sendParticles(newPhase >= 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.TOTEM_OF_UNDYING,
                mob.getX(), mob.getY() + mob.getBbHeight() * 0.5D, mob.getZ(), newPhase >= 2 ? 70 : 45,
                1.2D, 1.0D, 1.2D, 0.08D);
        for (ServerPlayer player : playersNear(level, mob, MYTHIC_BOSSBAR_RADIUS)) {
            player.sendSystemMessage(Component.literal(newPhase >= 2
                    ? "§c§l[신화 폭주] §r§f최종 단계 돌입 · 공격/저항 강화"
                    : "§6[신화 격변] §f2단계 돌입 · 기동 강화"), true);
            if (newPhase >= 2 && player.distanceToSqr(mob) <= 36.0D) {
                Vec3 push = player.position().subtract(mob.position()).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 1.0E-5D) {
                    push = push.normalize();
                    player.setDeltaMovement(player.getDeltaMovement().add(push.x * 0.85D, 0.28D, push.z * 0.85D));
                    player.hurtMarked = true;
                }
            }
        }
    }

    private static List<ServerPlayer> playersNear(ServerLevel level, Entity entity, double radius) {
        double radiusSqr = radius * radius;
        List<ServerPlayer> players = new ArrayList<>();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level && player.isAlive() && !player.isSpectator() && player.distanceToSqr(entity) <= radiusSqr) players.add(player);
        }
        return players;
    }

    private static String directionLabel(ServerPlayer player, Entity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        if (Math.abs(dx) > Math.abs(dz) * 2.0D) return dx >= 0 ? "동쪽" : "서쪽";
        if (Math.abs(dz) > Math.abs(dx) * 2.0D) return dz >= 0 ? "남쪽" : "북쪽";
        if (dx >= 0 && dz >= 0) return "남동쪽";
        if (dx >= 0) return "북동쪽";
        if (dz >= 0) return "남서쪽";
        return "북서쪽";
    }

    private static void syncMythicBossBar(MythicRuntime runtime, Mob mob) {
        Set<ServerPlayer> shouldSee = new HashSet<>(playersNear(runtime.level, mob, MYTHIC_BOSSBAR_RADIUS));
        for (ServerPlayer player : shouldSee) {
            runtime.contributors.add(player.getUUID());
            if (!runtime.bossBar.getPlayers().contains(player)) runtime.bossBar.addPlayer(player);
        }
        for (ServerPlayer viewer : List.copyOf(runtime.bossBar.getPlayers())) if (!shouldSee.contains(viewer)) runtime.bossBar.removePlayer(viewer);
        int phase = mob.getPersistentData().getIntOr(MYTHIC_PHASE_KEY, 0) + 1;
        runtime.bossBar.setName(Component.literal("§6신화 III §7· §f" + trait(mob).koreanName + " §7· 단계 " + phase + "/3"));
        runtime.bossBar.setProgress(Math.max(0.0F, Math.min(1.0F, mob.getHealth() / Math.max(1.0F, mob.getMaxHealth()))));
        runtime.bossBar.setVisible(true);
    }

    private static void closeMythicBar(MythicRuntime runtime) {
        runtime.bossBar.setVisible(false);
        for (ServerPlayer viewer : List.copyOf(runtime.bossBar.getPlayers())) runtime.bossBar.removePlayer(viewer);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    private static final class MythicRuntime {
        final ServerLevel level;
        final ServerBossEvent bossBar;
        final Set<UUID> contributors = new HashSet<>();

        MythicRuntime(ServerLevel level, Mob mob) {
            this.level = level;
            this.bossBar = new ServerBossEvent(UUID.randomUUID(), Component.literal("신화 III"),
                    BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
            this.bossBar.setProgress(1.0F);
            this.bossBar.setVisible(true);
        }
    }''', "elite apply/helpers")
t = replace_once(t,
'''        MYTHIC_III(3, 1.70D, 9.0D, 0.16D, 0.60D, 0.35D);''',
'''        MYTHIC_III(3, 3.50D, 14.0D, 0.22D, 1.10D, 0.65D);''', "elite mythic stats")
write(rel, t)


# Register mythic runtime lifecycle.
rel = "SurvivalAscension.java"
t = read(rel)
t = replace_once(t,
'''        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onFinalizeSpawn);\n        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onDamagePre);''',
'''        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onFinalizeSpawn);\n        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onEntityJoin);\n        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onServerTick);\n        NeoForge.EVENT_BUS.addListener(EliteMobSystem::onDamagePre);''', "register mythic runtime")
write(rel, t)


# -----------------------------------------------------------------------------
# Field incidents: nearby players become one shared encounter.
# -----------------------------------------------------------------------------
rel = "expedition/ExpeditionIncidentSystem.java"
t = read(rel)
t = replace_once(t,
'''    private static final double EVENT_RADIUS = 48.0D;''',
'''    private static final double EVENT_RADIUS = 48.0D;\n    private static final double COOP_JOIN_RADIUS = 72.0D;''', "incident coop radius")

t = replace_section(t,
"    public static boolean isActive(ServerPlayer player) {",
"    public static void onPlayerTick(PlayerTickEvent.Post event) {",
'''    public static boolean isActive(ServerPlayer player) {
        UUID id = player.getUUID();
        if (PENDING.containsKey(id) || ACTIVE.containsKey(id)) return true;
        for (PendingIncident pending : PENDING.values()) if (pending.participants.contains(id)) return true;
        for (ActiveIncident active : ACTIVE.values()) if (active.participants.contains(id)) return true;
        return false;
    }''', "incident isActive")

t = replace_once(t,
'''        PendingIncident pending = PENDING.get(player.getUUID());\n        if (pending != null) {\n            tickPending(player, pending);\n            return;\n        }\n\n        if (player.tickCount % CHECK_INTERVAL_TICKS != 0''',
'''        PendingIncident pending = PENDING.get(player.getUUID());\n        if (pending != null) {\n            tickPending(player, pending);\n            return;\n        }\n        if (isActive(player)) return;\n\n        if (player.tickCount % CHECK_INTERVAL_TICKS != 0''', "incident participant trigger guard")

t = replace_section(t,
"    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {",
"    public static void onLivingDeath(LivingDeathEvent event) {",
'''    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID leaving = event.getEntity().getUUID();
        for (PendingIncident pending : PENDING.values()) {
            pending.participants.remove(leaving);
            if (event.getEntity() instanceof ServerPlayer sp) pending.bossBar.removePlayer(sp);
        }
        for (ActiveIncident active : ACTIVE.values()) {
            active.participants.remove(leaving);
            if (event.getEntity() instanceof ServerPlayer sp) active.bossBar.removePlayer(sp);
        }

        PendingIncident pending = PENDING.get(leaving);
        if (pending != null) {
            ServerPlayer replacement = replacementController(pending.level, pending.center, pending.region, pending.participants);
            if (replacement != null) {
                PENDING.remove(leaving);
                pending.owner = replacement.getUUID();
                PENDING.put(pending.owner, pending);
            } else {
                PENDING.remove(leaving);
                closeBossBar(pending.bossBar);
            }
        }

        ActiveIncident active = ACTIVE.get(leaving);
        if (active != null) {
            ServerPlayer replacement = replacementController(active.level, active.center, active.incident.region(), active.participants);
            if (replacement != null) {
                ACTIVE.remove(leaving);
                active.owner = replacement.getUUID();
                for (UUID id : active.mobIds) {
                    Entity entity = active.level.getEntity(id);
                    if (entity instanceof Mob mob) mob.getPersistentData().putString(INCIDENT_OWNER_KEY, active.owner.toString());
                }
                ACTIVE.put(active.owner, active);
            } else {
                ACTIVE.remove(leaving);
                cleanupMobs(active);
                closeBossBar(active.bossBar);
            }
        }
    }''', "incident logout transfer")

t = replace_section(t,
"    public static void onLivingDeath(LivingDeathEvent event) {",
"    public static void onEntityJoin(EntityJoinLevelEvent event) {",
'''    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) return;
        String ownerText = mob.getPersistentData().getStringOr(INCIDENT_OWNER_KEY, "");
        if (ownerText.isEmpty()) return;
        try {
            UUID owner = UUID.fromString(ownerText);
            ActiveIncident active = ACTIVE.get(owner);
            if (active != null && active.level == level) {
                active.mobIds.remove(mob.getUUID());
                if (event.getSource().getEntity() instanceof ServerPlayer killer && qualifiesParticipant(killer, active.level, active.center, active.incident.region(), EVENT_RADIUS)) {
                    active.participants.add(killer.getUUID());
                }
            }
        } catch (IllegalArgumentException ignored) {
        }
    }''', "incident death contribution")

t = replace_section(t,
"    public static void recordAction(ServerPlayer player, ExpeditionAction action, int amount) {",
"    private static void queueStart(ServerPlayer player, ServerLevel level, ExpeditionRegion region,",
'''    public static void recordAction(ServerPlayer player, ExpeditionAction action, int amount) {
        if (amount <= 0) return;
        ActiveIncident active = findActiveFor(player, action);
        if (active == null) return;
        active.participants.add(player.getUUID());
        if (!active.bossBar.getPlayers().contains(player)) active.bossBar.addPlayer(player);
        active.actionProgress = Math.min(active.actionTarget(), active.actionProgress + amount);
        updateBossBar(active);
        if (active.actionProgress >= active.actionTarget()) complete(active);
    }''', "incident shared action")

t = replace_section(t,
"    private static void queueStart(ServerPlayer player, ServerLevel level, ExpeditionRegion region,",
"    private static void tickPending(ServerPlayer player, PendingIncident pending) {",
'''    private static void queueStart(ServerPlayer player, ServerLevel level, ExpeditionRegion region,
                                   ExpeditionIncident incident, boolean rare) {
        if (FinalAscensionSystem.isFinalSequenceActive(player)) return;
        long now = level.getGameTime();
        BlockPos center = player.blockPosition().immutable();
        if (overlapsActiveIncident(level, center, player.getUUID())) {
            player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
            return;
        }

        PendingIncident pending = new PendingIncident(player.getUUID(), level, center, region, incident,
                now + PRE_ALERT_TICKS, rare);
        PENDING.put(player.getUUID(), pending);
        List<ServerPlayer> party = eligiblePlayers(level, center, region, COOP_JOIN_RADIUS);
        if (party.isEmpty()) party = List.of(player);
        for (ServerPlayer member : party) {
            pending.participants.add(member.getUUID());
            pending.bossBar.addPlayer(member);
        }
        pending.bossBar.setVisible(true);
        updatePendingBossBar(pending);
        renderBoundary(level, center, rare, true);
        int seconds = PRE_ALERT_TICKS / 20;
        Component message = Component.literal((rare ? "§d[희귀 공동 사건 예고] " : "§e[공동 사건 예고] ")
                + "§f" + region.koreanName() + " · §e" + incident.koreanName() + " §7· 참가 " + party.size() + "명"
                + " §7· X " + center.getX() + " Z " + center.getZ() + " §7· " + seconds + "초 후 개방");
        notify(pending.level, pending.participants, message, false);
    }''', "incident queue shared")

t = replace_section(t,
"    private static void tickPending(ServerPlayer player, PendingIncident pending) {",
"    private static void cancelPending(ServerPlayer player, PendingIncident pending, String reason) {",
'''    private static void tickPending(ServerPlayer player, PendingIncident pending) {
        if (PENDING.get(player.getUUID()) != pending) return;
        long now = pending.level.getGameTime();
        syncPendingParticipants(pending);
        List<ServerPlayer> inside = eligiblePlayers(pending.level, pending.center, pending.region, EVENT_RADIUS);
        if (inside.isEmpty()) {
            cancelPending(player, pending, "참가자가 사건 구역을 벗어나 개방이 취소되었습니다.");
            return;
        }

        if (now >= pending.openTick) {
            if (PENDING.remove(pending.owner) != pending) return;
            closeBossBar(pending.bossBar);
            start(player, pending.level, pending.region, pending.incident, pending.rare, pending.center);
            return;
        }

        if (now % 20L == 0L) {
            renderBoundary(pending.level, pending.center, pending.rare, true);
            updatePendingBossBar(pending);
        }
        if (now % PRE_ALERT_ACTIONBAR_INTERVAL == 0L) {
            long seconds = Math.max(1L, (pending.openTick - now + 19L) / 20L);
            notify(pending.level, pending.participants, Component.literal((pending.rare ? "§d[희귀 공동 사건] " : "§e[공동 사건] ")
                    + "§f" + pending.incident.koreanName() + " §7· §e" + seconds + "초 후 개방"), true);
        }
    }''', "incident pending shared")

t = replace_section(t,
"    private static void cancelPending(ServerPlayer player, PendingIncident pending, String reason) {",
"    private static void start(ServerPlayer player, ServerLevel level, ExpeditionRegion region,",
'''    private static void cancelPending(ServerPlayer player, PendingIncident pending, String reason) {
        if (PENDING.remove(pending.owner) != pending) return;
        closeBossBar(pending.bossBar);
        long now = pending.level.getGameTime();
        player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
        notify(pending.level, pending.participants, Component.literal("§7[공동 사건 예고 취소] §f" + reason), false);
    }''', "incident cancel shared")

t = replace_section(t,
"    private static void start(ServerPlayer player, ServerLevel level, ExpeditionRegion region,",
"    private static void tickActive(ServerPlayer player, ActiveIncident active) {",
'''    private static void start(ServerPlayer player, ServerLevel level, ExpeditionRegion region,
                              ExpeditionIncident incident, boolean rare, BlockPos center) {
        long now = level.getGameTime();
        List<ServerPlayer> party = eligiblePlayers(level, center, region, EVENT_RADIUS);
        if (party.isEmpty()) {
            player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
            return;
        }
        if (overlapsActiveIncident(level, center, player.getUUID())) {
            player.getPersistentData().putLong(READY_TICK_KEY, now + OVERLAP_RETRY_TICKS);
            player.sendSystemMessage(Component.literal("§7[공동 사건 보류] §f근처 사건 구역과 겹쳐 이번 개방을 건너뜁니다."));
            return;
        }
        player.getPersistentData().putLong(READY_TICK_KEY, now + START_COOLDOWN_TICKS);
        ActiveIncident active = new ActiveIncident(player.getUUID(), level, center, incident,
                now + incident.durationTicks() + (rare ? RARE_EXTRA_TIME_TICKS : 0), rare, party.size());
        for (ServerPlayer member : party) active.participants.add(member.getUUID());
        ServerPlayer target = party.get(0);

        if (incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            Set<UUID> spawned = spawnAmbush(target, active);
            int minimum = Math.max(3, active.spawnTarget() * 2 / 3);
            if (spawned.size() < minimum) {
                for (UUID id : spawned) {
                    Entity entity = level.getEntity(id);
                    if (entity != null) entity.discard();
                }
                player.getPersistentData().putLong(READY_TICK_KEY, now + 1200);
                notify(level, active.participants, Component.literal("§7[공동 사건 보류] §f습격대를 배치할 공간이 부족합니다."), false);
                return;
            }
            active.mobIds.addAll(spawned);
            Mob reinforcement = spawnRareReinforcement(target, active);
            if (reinforcement != null) {
                active.mobIds.add(reinforcement.getUUID());
                active.reinforcementCount = 1;
            }
            active.initialMobCount = active.mobIds.size();
        }

        ACTIVE.put(active.owner, active);
        for (ServerPlayer member : party) active.bossBar.addPlayer(member);
        active.bossBar.setVisible(true);
        updateBossBar(active);
        renderBoundary(active.level, active.center, active.rare, false);
        String prefix = rare ? "§d[희귀 공동 사건] " : "§6[공동 사건] ";
        String objective = incident.kind() == ExpeditionIncident.Kind.AMBUSH
                ? "습격대 " + active.initialMobCount + "체 격파"
                : incident.action().koreanName() + " " + active.actionTarget();
        notify(level, active.participants, Component.literal(prefix + "§f" + region.koreanName() + " · §e" + incident.koreanName()
                + " §7· 참가 " + active.participantCountSnapshot + "명 · 공동 목표: §f" + objective), false);
    }''', "incident start shared")

t = replace_section(t,
"    private static void tickActive(ServerPlayer player, ActiveIncident active) {",
"    private static Set<UUID> spawnAmbush(ServerPlayer player, ActiveIncident active) {",
'''    private static void tickActive(ServerPlayer player, ActiveIncident active) {
        if (ACTIVE.get(player.getUUID()) != active) return;
        long now = active.level.getGameTime();
        syncActiveParticipants(active);
        List<ServerPlayer> inside = eligiblePlayers(active.level, active.center, active.incident.region(), EVENT_RADIUS);
        if (inside.isEmpty()) active.outsideTicks += 5;
        else active.outsideTicks = 0;

        if (active.outsideTicks >= OUTSIDE_GRACE_TICKS) {
            fail(active, "모든 참가자가 사건 지역을 너무 오래 벗어났습니다.");
            return;
        }
        if (now >= active.deadline) {
            fail(active, "제한시간이 끝났습니다.");
            return;
        }

        if (now % 20L == 0L) {
            renderBoundary(active.level, active.center, active.rare, false);
            long seconds = Math.max(1L, (active.deadline - now + 19L) / 20L);
            if (active.outsideTicks > 0) {
                notify(active.level, active.participants, Component.literal("§c[공동 사건 경계] §f48블록 안으로 한 명이라도 복귀하세요."), true);
            } else if (seconds <= 10L || seconds == 30L) {
                notify(active.level, active.participants, Component.literal((seconds <= 10L ? "§c" : "§e") + "[공동 사건] §f"
                        + active.incident.koreanName() + " §7· 남은 시간 §e" + seconds + "초"), true);
            }
        }

        if (active.incident.kind() == ExpeditionIncident.Kind.AMBUSH) {
            Set<UUID> unresolved = new HashSet<>();
            ServerPlayer target = inside.isEmpty() ? null : inside.get(0);
            for (UUID id : active.mobIds) {
                Entity entity = active.level.getEntity(id);
                if (entity == null) {
                    unresolved.add(id);
                    continue;
                }
                if (!(entity instanceof Mob mob) || !mob.isAlive()) continue;
                unresolved.add(id);
                mob.setGlowingTag(true);
                if (target != null && (mob.getTarget() == null || !mob.getTarget().isAlive())) mob.setTarget(target);
                if (distanceToCenterSqr(mob, active.center) > EVENT_RADIUS * EVENT_RADIUS) {
                    mob.getNavigation().moveTo(active.center.getX() + 0.5D, active.center.getY(), active.center.getZ() + 0.5D, 1.25D);
                }
            }
            active.mobIds.clear();
            active.mobIds.addAll(unresolved);
            if (active.mobIds.isEmpty()) {
                complete(active);
                return;
            }
        }
        updateBossBar(active);
    }''', "incident active shared")

t = replace_section(t,
"    private static void complete(ServerPlayer player, ActiveIncident active) {",
"    private static void fail(ServerPlayer player, ActiveIncident active, String reason) {",
'''    private static void complete(ActiveIncident active) {
        if (ACTIVE.remove(active.owner) != active) return;
        cleanupMobs(active);
        closeBossBar(active.bossBar);
        for (UUID id : new HashSet<>(active.participants)) {
            ServerPlayer member = active.level.getServer().getPlayerList().getPlayer(id);
            if (member == null || member.level() != active.level) continue;
            ExpeditionData data = ExpeditionData.get(member);
            if (!data.claimIncidentReward(member, active.incident.region())) continue;
            int stage = active.incident.region().requiredWorldStage();
            int skillXp = (100 + stage * 50) * (active.rare ? 2 : 1);
            SkillProgressionService.award(member, active.incident.region().rewardSkill(), skillXp);
            if (stage == 0) {
                giveOrDrop(member, new ItemStack(Items.EMERALD, active.rare ? 10 : 4));
                giveOrDrop(member, new ItemStack(Items.AMETHYST_SHARD, active.rare ? 20 : 8));
            } else if (stage == 1) {
                giveOrDrop(member, new ItemStack(Items.DIAMOND, active.rare ? 5 : 2));
                giveOrDrop(member, new ItemStack(Items.ECHO_SHARD, active.rare ? 10 : 4));
            } else {
                giveOrDrop(member, new ItemStack(Items.DIAMOND, active.rare ? 8 : 4));
                giveOrDrop(member, new ItemStack(Items.ECHO_SHARD, active.rare ? 16 : 8));
            }
            member.sendSystemMessage(Component.literal((active.rare ? "§d[희귀 공동 사건 해결] " : "§a[공동 사건 해결] ")
                    + "§f" + active.incident.koreanName() + " §7· " + active.incident.region().rewardSkill().koreanName() + " 숙련 XP +" + skillXp));
            ExpeditionDirective.Task bonusTask = data.firstIncompleteTask(member, active.incident.region());
            if (bonusTask != null) {
                int bonus = Math.max(1, bonusTask.target() / (active.rare ? 3 : 5));
                ExpeditionProgression.grantIncidentBonus(member, active.incident.region(), bonusTask.action(), bonus);
            }
        }
    }''', "incident complete shared")

t = replace_section(t,
"    private static void fail(ServerPlayer player, ActiveIncident active, String reason) {",
"    private static void updatePendingBossBar(PendingIncident pending) {",
'''    private static void fail(ActiveIncident active, String reason) {
        if (ACTIVE.remove(active.owner) != active) return;
        cleanupMobs(active);
        closeBossBar(active.bossBar);
        notify(active.level, active.participants, Component.literal("§c[공동 사건 실패] §f" + active.incident.koreanName() + " · " + reason
                + " §7· 개인 원정 지령 진행도는 잃지 않습니다."), false);
    }''', "incident fail shared")

# Insert shared helper block before stale cleanup.
marker = "    private static void removeStaleServerIncidents(MinecraftServer server) {"
helpers = '''    private static ActiveIncident findActiveFor(ServerPlayer player, ExpeditionAction action) {
        for (ActiveIncident active : ACTIVE.values()) {
            if (active.level != player.level() || active.incident.kind() != ExpeditionIncident.Kind.ACTION_RUSH) continue;
            if (active.incident.action() != action) continue;
            if (qualifiesParticipant(player, active.level, active.center, active.incident.region(), EVENT_RADIUS)) return active;
        }
        return null;
    }

    private static boolean qualifiesParticipant(ServerPlayer player, ServerLevel level, BlockPos center,
                                                ExpeditionRegion region, double radius) {
        return player.level() == level && player.isAlive() && !player.isCreative() && !player.isSpectator()
                && !FinalAscensionSystem.isFinalSequenceActive(player)
                && ExpeditionProgression.currentRegion(player) == region
                && ExpeditionData.get(player).isDiscovered(player, region)
                && distanceToCenterSqr(player, center) <= radius * radius;
    }

    private static List<ServerPlayer> eligiblePlayers(ServerLevel level, BlockPos center, ExpeditionRegion region, double radius) {
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (qualifiesParticipant(player, level, center, region, radius)) result.add(player);
        }
        return result;
    }

    private static void syncPendingParticipants(PendingIncident pending) {
        Set<ServerPlayer> viewers = new HashSet<>(eligiblePlayers(pending.level, pending.center, pending.region, COOP_JOIN_RADIUS));
        for (ServerPlayer viewer : viewers) {
            if (distanceToCenterSqr(viewer, pending.center) <= EVENT_RADIUS * EVENT_RADIUS) pending.participants.add(viewer.getUUID());
            if (!pending.bossBar.getPlayers().contains(viewer)) pending.bossBar.addPlayer(viewer);
        }
        for (ServerPlayer viewer : List.copyOf(pending.bossBar.getPlayers())) if (!viewers.contains(viewer)) pending.bossBar.removePlayer(viewer);
    }

    private static void syncActiveParticipants(ActiveIncident active) {
        Set<ServerPlayer> viewers = new HashSet<>(eligiblePlayers(active.level, active.center, active.incident.region(), COOP_JOIN_RADIUS));
        for (ServerPlayer viewer : viewers) {
            if (distanceToCenterSqr(viewer, active.center) <= EVENT_RADIUS * EVENT_RADIUS) active.participants.add(viewer.getUUID());
            if (!active.bossBar.getPlayers().contains(viewer)) active.bossBar.addPlayer(viewer);
        }
        for (ServerPlayer viewer : List.copyOf(active.bossBar.getPlayers())) if (!viewers.contains(viewer)) active.bossBar.removePlayer(viewer);
    }

    private static ServerPlayer replacementController(ServerLevel level, BlockPos center, ExpeditionRegion region, Set<UUID> participants) {
        for (UUID id : participants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null && qualifiesParticipant(player, level, center, region, COOP_JOIN_RADIUS)) return player;
        }
        return null;
    }

    private static void notify(ServerLevel level, Set<UUID> participants, Component message, boolean actionbar) {
        for (UUID id : participants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(id);
            if (player != null && player.level() == level) player.sendSystemMessage(message, actionbar);
        }
    }

'''
t = replace_once(t, marker, helpers + marker, "incident helpers")
t = replace_once(t, "        final UUID owner;\n        final ServerLevel level;", "        UUID owner;\n        final ServerLevel level;", "pending mutable owner")
t = replace_once(t,
'''        final boolean rare;\n        final ServerBossEvent bossBar;''',
'''        final boolean rare;\n        final ServerBossEvent bossBar;\n        final Set<UUID> participants = new HashSet<>();''', "pending participants")
# Active owner: after pending replacement there is one remaining final UUID owner.
t = replace_once(t, "        final UUID owner;\n        final ServerLevel level;", "        UUID owner;\n        final ServerLevel level;", "active mutable owner")
t = replace_once(t,
'''        final ServerBossEvent bossBar;\n        final Set<UUID> mobIds = new HashSet<>();\n        int initialMobCount;''',
'''        final ServerBossEvent bossBar;\n        final Set<UUID> mobIds = new HashSet<>();\n        final Set<UUID> participants = new HashSet<>();\n        final int participantCountSnapshot;\n        int initialMobCount;''', "active participants")
t = replace_once(t,
'''        ActiveIncident(UUID owner, ServerLevel level, BlockPos center, ExpeditionIncident incident, long deadline, boolean rare) {\n            this.owner = owner;''',
'''        ActiveIncident(UUID owner, ServerLevel level, BlockPos center, ExpeditionIncident incident, long deadline, boolean rare, int participantCountSnapshot) {\n            this.owner = owner;''', "active constructor signature")
t = replace_once(t,
'''            this.rare = rare;\n            this.bossBar = new ServerBossEvent''',
'''            this.rare = rare;\n            this.participantCountSnapshot = Math.max(1, participantCountSnapshot);\n            this.bossBar = new ServerBossEvent''', "active party snapshot")
t = replace_section(t,
"        int actionTarget() {",
"        int spawnTarget() {",
'''        int actionTarget() {
            int base = incident.actionTarget();
            if (rare && base > 0) base = Math.max(base + 1, (base * 3 + 1) / 2);
            double coop = 1.0D + Math.min(4, participantCountSnapshot - 1) * 0.55D;
            return base <= 0 ? base : Math.max(base, (int)Math.ceil(base * coop));
        }''', "incident scaled action target")
t = replace_section(t,
"        int spawnTarget() {",
"    }\n}",
'''        int spawnTarget() {
            int base = incident.spawnCount();
            if (rare && base > 0) base = Math.max(base + 2, (base * 3 + 1) / 2);
            double coop = 1.0D + Math.min(4, participantCountSnapshot - 1) * 0.65D;
            return base <= 0 ? base : Math.max(base, (int)Math.ceil(base * coop));
        }
    }
}''', "incident scaled spawn target")
write(rel, t)


# -----------------------------------------------------------------------------
# Apex hunts: party survives owner departure, scales, and shares progression.
# -----------------------------------------------------------------------------
rel = "apex/ApexHuntSystem.java"
t = read(rel)
t = replace_once(t,
'''    private static final Identifier ATTACK_ID = id("apex_attack");''',
'''    private static final Identifier ATTACK_ID = id("apex_attack");\n    private static final Identifier COOP_HEALTH_ID = id("apex_coop_health");\n    private static final Identifier COOP_ATTACK_ID = id("apex_coop_attack");''', "apex coop attrs")
t = replace_section(t,
"    public static boolean isActive(ServerPlayer player) {",
"    public static void tryStart(ServerPlayer player) {",
'''    public static boolean isActive(ServerPlayer player) {
        UUID id = player.getUUID();
        if (ACTIVE.containsKey(id)) return true;
        for (Hunt hunt : ACTIVE.values()) if (hunt.participants.contains(id)) return true;
        return false;
    }''', "apex isActive")
t = replace_once(t,
'''        ACTIVE.put(player.getUUID(), hunt);\n        hunt.bossBar.addPlayer(player);\n        hunt.bossBar.setVisible(true);\n        player.sendSystemMessage(Component.literal("§4[정점 사냥] §f" + region.koreanName() + " · §e" + archetype.koreanName()\n                + "§f 출현." + (hunt.packEscortCount > 0 ? " §b· 이변 호위 1체 포함" : "")\n                + " §7보스의 행동 전조와 호위 조합을 읽고 90초 안에 격파하세요."));''',
'''        ACTIVE.put(player.getUUID(), hunt);\n        syncBossBarPlayers(server, hunt);\n        hunt.bossBar.setVisible(true);\n        notifyHunt(hunt, Component.literal("§4[공동 정점 사냥] §f" + region.koreanName() + " · §e" + archetype.koreanName()\n                + "§f 출현 §7· 참가 " + hunt.partySizeSnapshot + "명" + (hunt.packEscortCount > 0 ? " §b· 이변 호위 1체 포함" : "")\n                + " §7· 90초 안에 공동 격파하세요."), false);''', "apex shared start")
t = replace_section(t,
"    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {",
"    public static void onEntityJoin(EntityJoinLevelEvent event) {",
'''    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID leaving = event.getEntity().getUUID();
        if (event.getEntity() instanceof ServerPlayer sp) {
            for (Hunt hunt : ACTIVE.values()) hunt.bossBar.removePlayer(sp);
        }
        // The hunt is server-driven. It remains alive while another participant stays in the arena.
        for (Hunt hunt : ACTIVE.values()) {
            if (hunt.owner.equals(leaving)) hunt.ownerDisconnected = true;
        }
    }''', "apex logout persistence")
t = replace_once(t,
'''        Hunt hunt = new Hunt(owner.getUUID(), level, center, archetype, level.getGameTime() + HUNT_TIMEOUT_TICKS);\n\n        Mob boss = spawnOne''',
'''        Hunt hunt = new Hunt(owner.getUUID(), level, center, archetype, level.getGameTime() + HUNT_TIMEOUT_TICKS);\n        admitNearbyParticipants(hunt);\n        hunt.partySizeSnapshot = Math.max(1, hunt.participants.size());\n\n        Mob boss = spawnOne''', "apex seed party")
t = replace_once(t,
'''        applyBossStats(boss, archetype);''',
'''        applyBossStats(boss, archetype, hunt.partySizeSnapshot);''', "apex scaled boss call")

t = replace_section(t,
"    private static boolean tickHunt(MinecraftServer server, Hunt hunt) {",
"    private static void runPattern(Hunt hunt, Mob boss, ServerPlayer owner, long now) {",
'''    private static boolean tickHunt(MinecraftServer server, Hunt hunt) {
        long now = hunt.level.getGameTime();
        syncBossBarPlayers(server, hunt);
        ServerPlayer target = chooseHuntTarget(server, hunt);
        if (target != null) hunt.partyAbsentTicks = 0;
        else hunt.partyAbsentTicks += 5;

        if (hunt.partyAbsentTicks >= OWNER_GRACE_TICKS) {
            fail(hunt, "모든 참가자가 사냥권에서 이탈하거나 사망했습니다.");
            return true;
        }
        if (now >= hunt.deadline) {
            fail(hunt, "90초 제한시간을 초과했습니다.");
            return true;
        }

        Entity bossEntity = hunt.level.getEntity(hunt.bossId);
        if (bossEntity == null) {
            int seconds = Math.max(0, (int) ((hunt.deadline - now + 19L) / 20L));
            hunt.bossBar.setName(Component.literal("§4공동 정점 사냥 §7[" + hunt.archetype.koreanName() + "] §f대상 재확인 중 §7· " + seconds + "초"));
            return false;
        }
        if (!(bossEntity instanceof Mob boss) || !boss.isAlive()) {
            complete(hunt);
            return true;
        }
        pruneAndRecall(hunt, target, boss);
        if (target != null) runPattern(hunt, boss, target, now);

        int seconds = Math.max(0, (int) ((hunt.deadline - now + 19L) / 20L));
        hunt.bossBar.setName(Component.literal("§4공동 정점 사냥 §7[" + hunt.archetype.koreanName() + "] §f"
                + Math.max(0, (int) Math.ceil(boss.getHealth())) + "/" + (int) Math.ceil(boss.getMaxHealth())
                + " §7· 호위 " + Math.max(0, hunt.mobIds.size() - 1) + " · 참가 " + hunt.bossBar.getPlayers().size() + " · " + seconds + "초"));
        hunt.bossBar.setProgress(Mth.clamp(boss.getHealth() / Math.max(1.0F, boss.getMaxHealth()), 0.0F, 1.0F));
        return false;
    }''', "apex shared tick")
# Rename runPattern parameter and its body references until next method.
a = t.find("    private static void runPattern(Hunt hunt, Mob boss, ServerPlayer owner, long now) {")
b = t.find("    private static void addReinforcements(Hunt hunt, ServerPlayer owner, int requested) {", a)
if a < 0 or b < 0:
    raise SystemExit("apex runPattern anchors missing")
segment = t[a:b].replace("ServerPlayer owner", "ServerPlayer target").replace("owner", "target")
t = t[:a] + segment + t[b:]
# Reinforcement/prune method parameters can remain named owner; they receive target.

t = replace_section(t,
"    private static void complete(Hunt hunt, ServerPlayer owner) {",
"    private static void fail(Hunt hunt, ServerPlayer owner, String reason) {",
'''    private static void complete(Hunt hunt) {
        int stage = WorldAscensionData.get(hunt.level.getServer()).stage();
        for (UUID id : new HashSet<>(hunt.participants)) {
            ServerPlayer player = hunt.level.getServer().getPlayerList().getPlayer(id);
            if (player == null || player.level() != hunt.level) continue;
            boolean owner = id.equals(hunt.owner);
            boolean mythic = stage >= 2 && hunt.level.getRandom().nextDouble() < (owner ? 0.20D : 0.10D);
            giveOrDrop(player, AscensionAffixes.createEliteDrop(hunt.level.getRandom(), mythic ? 3 : 2));
            if (stage >= 2) {
                giveOrDrop(player, new ItemStack(Items.DIAMOND, owner ? 3 : 2));
                giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, owner ? 6 : 4));
                if (owner) giveOrDrop(player, new ItemStack(Items.NETHERITE_SCRAP, 1));
                player.giveExperiencePoints(owner ? 180 : 140);
            } else {
                giveOrDrop(player, new ItemStack(Items.DIAMOND, owner ? 2 : 1));
                giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, owner ? 4 : 3));
                player.giveExperiencePoints(owner ? 120 : 100);
            }

            ApexHuntData data = ApexHuntData.get(player);
            boolean first = data.recordVictory(player, hunt.archetype);
            player.sendSystemMessage(Component.literal("§a[공동 정점 격파] §f" + hunt.archetype.koreanName()
                    + " §7· 정점 도감 " + data.uniqueDefeated(player) + "/9 · 총 " + data.victories(player) + "승"
                    + (first ? " §e· 최초 격파 공유" : "") + (owner ? " §6· 추적자 보너스" : "")));
            if (data.claimMasteryReward(player)) {
                giveOrDrop(player, AscensionAffixes.createEliteDrop(hunt.level.getRandom(), 3));
                giveOrDrop(player, new ItemStack(Items.NETHERITE_SCRAP, 4));
                giveOrDrop(player, new ItemStack(Items.ECHO_SHARD, 32));
                giveOrDrop(player, new ItemStack(Items.DRAGON_BREATH, 16));
                player.giveExperiencePoints(500);
                player.sendSystemMessage(Component.literal("§6[정점 사냥 완주] §f9개 원정권 정점 최초 격파 완료"));
            }
        }
        cleanupMobs(hunt);
        closeBossBar(hunt);
    }''', "apex shared complete")
t = replace_section(t,
"    private static void fail(Hunt hunt, ServerPlayer owner, String reason) {",
"    private static Mob spawnOne(ServerLevel level, BlockPos center, boolean water, boolean tall,",
'''    private static void fail(Hunt hunt, String reason) {
        cleanupMobs(hunt);
        closeBossBar(hunt);
        notifyHunt(hunt, Component.literal("§c[공동 정점 사냥 실패] §f" + hunt.archetype.koreanName()
                + " · " + reason + " §7· 추적 재료는 반환되지 않습니다."), false);
    }''', "apex shared fail")
# onLivingDeath still calls old completion signature.
t = t.replace("        complete(hunt, owner);\n        ACTIVE.remove(ownerId);", "        complete(hunt);\n        ACTIVE.remove(ownerId);", 1)

t = replace_section(t,
"    private static void applyBossStats(Mob boss, ApexArchetype archetype) {",
"    private static void addPermanent(AttributeInstance attribute, Identifier id, double amount, AttributeModifier.Operation operation) {",
'''    private static void applyBossStats(Mob boss, ApexArchetype archetype, int partySize) {
        addPermanent(boss.getAttribute(Attributes.MAX_HEALTH), HEALTH_ID, archetype.healthBonus(), AttributeModifier.Operation.ADD_VALUE);
        addPermanent(boss.getAttribute(Attributes.ARMOR), ARMOR_ID, archetype.armorBonus(), AttributeModifier.Operation.ADD_VALUE);
        addPermanent(boss.getAttribute(Attributes.ATTACK_DAMAGE), ATTACK_ID, archetype.attackBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        int extras = Math.min(4, Math.max(0, partySize - 1));
        addPermanent(boss.getAttribute(Attributes.MAX_HEALTH), COOP_HEALTH_ID, extras * 0.65D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        addPermanent(boss.getAttribute(Attributes.ATTACK_DAMAGE), COOP_ATTACK_ID, extras * 0.12D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        boss.setHealth(boss.getMaxHealth());
        boss.setCustomName(Component.literal("§4[정점] §e" + archetype.koreanName()));
        boss.setCustomNameVisible(true);
    }''', "apex coop stats")

t = replace_section(t,
"    private static void syncBossBarPlayers(MinecraftServer server, Hunt hunt) {",
"    private static void removeStaleServerHunts(MinecraftServer server) {",
'''    private static void syncBossBarPlayers(MinecraftServer server, Hunt hunt) {
        Set<ServerPlayer> shouldSee = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == hunt.level && player.isAlive() && !player.isSpectator()
                    && distanceToCenterSqr(player, hunt.center) <= PLAYER_RADIUS * PLAYER_RADIUS
                    && ExpeditionProgression.currentRegion(player) == hunt.archetype.region()) {
                shouldSee.add(player);
                hunt.participants.add(player.getUUID());
                if (!hunt.bossBar.getPlayers().contains(player)) hunt.bossBar.addPlayer(player);
            }
        }
        for (ServerPlayer viewer : List.copyOf(hunt.bossBar.getPlayers())) if (!shouldSee.contains(viewer)) hunt.bossBar.removePlayer(viewer);
    }

    private static void admitNearbyParticipants(Hunt hunt) {
        for (ServerPlayer player : hunt.level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == hunt.level && player.isAlive() && !player.isSpectator()
                    && distanceToCenterSqr(player, hunt.center) <= PLAYER_RADIUS * PLAYER_RADIUS
                    && ExpeditionProgression.currentRegion(player) == hunt.archetype.region()) {
                hunt.participants.add(player.getUUID());
            }
        }
        hunt.participants.add(hunt.owner);
    }

    private static ServerPlayer chooseHuntTarget(MinecraftServer server, Hunt hunt) {
        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != hunt.level || !player.isAlive() || player.isSpectator()) continue;
            if (ExpeditionProgression.currentRegion(player) != hunt.archetype.region()) continue;
            double distance = distanceToCenterSqr(player, hunt.center);
            if (distance <= PLAYER_RADIUS * PLAYER_RADIUS && distance < bestDistance) {
                best = player;
                bestDistance = distance;
                hunt.participants.add(player.getUUID());
            }
        }
        return best;
    }

    private static void notifyHunt(Hunt hunt, Component message, boolean actionbar) {
        for (UUID id : hunt.participants) {
            ServerPlayer player = hunt.level.getServer().getPlayerList().getPlayer(id);
            if (player != null && player.level() == hunt.level) player.sendSystemMessage(message, actionbar);
        }
    }''', "apex coop helpers")
t = replace_once(t,
'''        final Set<UUID> mobIds = new HashSet<>();\n        UUID bossId;''',
'''        final Set<UUID> mobIds = new HashSet<>();\n        final Set<UUID> participants = new HashSet<>();\n        UUID bossId;''', "apex participants field")
t = replace_once(t,
'''        int ownerAbsentTicks;''',
'''        int partyAbsentTicks;\n        int partySizeSnapshot = 1;\n        boolean ownerDisconnected;''', "apex party fields")
write(rel, t)


# -----------------------------------------------------------------------------
# Ascension Trial: shared arena survival and meaningful teammate rewards.
# -----------------------------------------------------------------------------
rel = "endgame/AscensionTrialSystem.java"
t = read(rel)
t = replace_once(t,
'''        Trial trial = new Trial(player.getUUID(), level, center, now + SETUP_TICKS, doctrine);\n        ACTIVE.put(player.getUUID(), trial);\n        trial.bossBar.addPlayer(player);\n        trial.bossBar.setVisible(true);\n        player.sendSystemMessage(Component.literal("§5[승천 시련] §f전술 교리 §d" + doctrine.koreanName() + " §7· §f" + doctrine.description()));\n        player.sendSystemMessage(Component.literal("§5[승천 시련] §f4개 웨이브를 제한시간 안에 격파하세요. §7각 웨이브 중반에 교리별 증원이 도착합니다."));''',
'''        Trial trial = new Trial(player.getUUID(), level, center, now + SETUP_TICKS, doctrine);\n        ACTIVE.put(player.getUUID(), trial);\n        syncBossBarPlayers(server, trial);\n        trial.partySizeSnapshot = Math.max(1, trial.participants.size());\n        trial.bossBar.setVisible(true);\n        notifyTrial(trial, Component.literal("§5[공동 승천 시련] §f전술 교리 §d" + doctrine.koreanName() + " §7· 참가 " + trial.partySizeSnapshot + "명 · §f" + doctrine.description()), false);\n        notifyTrial(trial, Component.literal("§5[공동 승천 시련] §f4개 웨이브를 함께 제한시간 안에 격파하세요."), false);''', "trial shared start")
t = replace_section(t,
"    public static boolean isActive(ServerPlayer player) {",
"    public static void onServerTick(ServerTickEvent.Pre event) {",
'''    public static boolean isActive(ServerPlayer player) {
        UUID id = player.getUUID();
        if (ACTIVE.containsKey(id)) return true;
        for (Trial trial : ACTIVE.values()) if (trial.participants.contains(id)) return true;
        return false;
    }''', "trial isActive")
t = replace_section(t,
"    private static boolean tickTrial(MinecraftServer server, Trial trial) {",
"    private static boolean spawnWave(Trial trial) {",
'''    private static boolean tickTrial(MinecraftServer server, Trial trial) {
        long now = trial.level.getGameTime();
        syncBossBarPlayers(server, trial);
        ServerPlayer target = chooseTrialTarget(server, trial);
        if (target != null) trial.partyAbsentTicks = 0;
        else trial.partyAbsentTicks += 5;

        if (trial.partyAbsentTicks >= OWNER_GRACE_TICKS) {
            fail(trial, "모든 참가자가 전장에서 이탈하거나 사망했습니다.");
            return true;
        }

        pruneAndRecall(trial, target);
        if (!trial.mobIds.isEmpty()) {
            maybeReinforce(trial, target);
            if (now >= trial.waveDeadline) {
                fail(trial, "웨이브 제한시간을 초과했습니다.");
                return true;
            }
            int seconds = Math.max(0, (int) ((trial.waveDeadline - now + 19L) / 20L));
            trial.bossBar.setName(Component.literal("§5공동 승천 시련 §7[" + trial.doctrine.koreanName() + "] §f· " + trial.wave + "/" + TOTAL_WAVES + " §7· 적 " + trial.mobIds.size() + "체 · 참가 " + trial.bossBar.getPlayers().size() + " · " + seconds + "초"));
            float remaining = (trial.waveDeadline - now) / (float) WAVE_TIMEOUT_TICKS;
            trial.bossBar.setProgress(Mth.clamp(remaining, 0.0F, 1.0F));
            return false;
        }

        if (trial.wave > 0 && !trial.waveResolved) {
            trial.waveResolved = true;
            int xp = switch (trial.wave) { case 1 -> 25; case 2 -> 40; case 3 -> 60; default -> 0; };
            for (ServerPlayer member : trialPlayers(trial)) {
                if (xp > 0) member.giveExperiencePoints(xp);
                member.sendSystemMessage(Component.literal("§5[공동 승천 시련] §f" + trial.wave + " 웨이브 격파" + (xp > 0 ? " §7· 경험치 +" + xp : "")));
            }
            if (trial.wave >= TOTAL_WAVES) {
                complete(trial);
                return true;
            }
            trial.nextWaveTick = now + SETUP_TICKS;
        }

        if (now < trial.nextWaveTick) {
            int seconds = Math.max(0, (int) ((trial.nextWaveTick - now + 19L) / 20L));
            trial.bossBar.setName(Component.literal("§5공동 승천 시련 §7[" + trial.doctrine.koreanName() + "] §f· 다음 웨이브까지 §d" + seconds + "초"));
            trial.bossBar.setProgress(Mth.clamp((trial.nextWaveTick - now) / (float) SETUP_TICKS, 0.0F, 1.0F));
            return false;
        }

        if (!spawnWave(trial)) {
            fail(trial, "적을 배치할 공간이 부족합니다. 더 열린 지형에서 다시 시도하세요.");
            return true;
        }
        return false;
    }''', "trial shared tick")
t = replace_once(t,
'''        int target = WAVE_COUNTS[trial.wave - 1];''',
'''        int baseTarget = WAVE_COUNTS[trial.wave - 1];\n        int extras = Math.min(4, Math.max(0, trial.partySizeSnapshot - 1));\n        int target = Math.min(32, Math.max(baseTarget, (int)Math.ceil(baseTarget * (1.0D + extras * 0.55D))));''', "trial scaled waves")
t = replace_section(t,
"    private static void syncBossBarPlayers(MinecraftServer server, Trial trial) {",
"    private static void complete(Trial trial, ServerPlayer owner) {",
'''    private static void syncBossBarPlayers(MinecraftServer server, Trial trial) {
        Set<ServerPlayer> shouldSee = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == trial.level && player.isAlive() && !player.isSpectator()
                    && distanceToCenterSqr(player, trial.center) <= PLAYER_RADIUS * PLAYER_RADIUS) {
                shouldSee.add(player);
                trial.participants.add(player.getUUID());
                if (!trial.bossBar.getPlayers().contains(player)) trial.bossBar.addPlayer(player);
            }
        }
        for (ServerPlayer viewer : List.copyOf(trial.bossBar.getPlayers())) if (!shouldSee.contains(viewer)) trial.bossBar.removePlayer(viewer);
    }

    private static ServerPlayer chooseTrialTarget(MinecraftServer server, Trial trial) {
        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() != trial.level || !player.isAlive() || player.isSpectator()) continue;
            double distance = distanceToCenterSqr(player, trial.center);
            if (distance <= PLAYER_RADIUS * PLAYER_RADIUS && distance < bestDistance) {
                best = player;
                bestDistance = distance;
                trial.participants.add(player.getUUID());
            }
        }
        return best;
    }

    private static List<ServerPlayer> trialPlayers(Trial trial) {
        List<ServerPlayer> result = new ArrayList<>();
        for (UUID id : trial.participants) {
            ServerPlayer player = trial.level.getServer().getPlayerList().getPlayer(id);
            if (player != null && player.level() == trial.level) result.add(player);
        }
        return result;
    }

    private static void notifyTrial(Trial trial, Component message, boolean actionbar) {
        for (ServerPlayer player : trialPlayers(trial)) player.sendSystemMessage(message, actionbar);
    }''', "trial coop helpers")
t = replace_section(t,
"    private static void complete(Trial trial, ServerPlayer owner) {",
"    private static void fail(Trial trial, ServerPlayer owner, String reason) {",
'''    private static void complete(Trial trial) {
        for (ServerPlayer player : trialPlayers(trial)) {
            boolean owner = player.getUUID().equals(trial.owner);
            player.giveExperiencePoints(owner ? 200 : 160);
            giveOrDrop(player, AscensionAffixes.createEliteDrop(trial.level.getRandom(), 3));
            giveOrDrop(player, new ItemStack(Items.NETHERITE_SCRAP, owner ? 2 : 1));
            giveOrDrop(player, new ItemStack(Items.DIAMOND, owner ? 4 : 2));
            player.sendSystemMessage(Component.literal("§d[공동 승천 시련 완료] §f" + trial.doctrine.koreanName()
                    + " 교리 돌파 · 신화 III 장비 1개" + (owner ? " §6· 개방자 보너스" : " §7· 협동 보상")));
        }
        closeBossBar(trial);
    }''', "trial shared complete")
t = replace_section(t,
"    private static void fail(Trial trial, ServerPlayer owner, String reason) {",
"    private static void playerFailureMessage(ServerPlayer owner, Trial trial, String reason) {",
'''    private static void fail(Trial trial, String reason) {
        for (UUID id : trial.mobIds) {
            Entity entity = trial.level.getEntity(id);
            if (entity != null) entity.discard();
        }
        trial.mobIds.clear();
        notifyTrial(trial, Component.literal("§c[공동 승천 시련 실패] §f" + trial.doctrine.koreanName() + " 교리 · " + reason
                + " §7· 입장 재료는 반환되지 않습니다."), false);
        closeBossBar(trial);
    }''', "trial shared fail")
# Remove now-unused owner-specific helper.
t = replace_section(t,
"    private static void playerFailureMessage(ServerPlayer owner, Trial trial, String reason) {",
"    private static void removeStaleServerTrials(MinecraftServer server) {",
'''    ''', "trial remove owner fail helper")
t = replace_once(t,
'''        final ServerBossEvent bossBar;\n        final Set<UUID> mobIds = new HashSet<>();''',
'''        final ServerBossEvent bossBar;\n        final Set<UUID> mobIds = new HashSet<>();\n        final Set<UUID> participants = new HashSet<>();''', "trial participants")
t = replace_once(t,
'''        int ownerAbsentTicks;''',
'''        int partyAbsentTicks;\n        int partySizeSnapshot = 1;''', "trial party fields")
write(rel, t)

print("cooperative encounter patch applied")
