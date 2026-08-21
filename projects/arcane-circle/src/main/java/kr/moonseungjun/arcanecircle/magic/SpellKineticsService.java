package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps authoritative combat synchronized with authored presentation. Every launch owns one
 * CastTargetSnapshot, so delayed gameplay never re-samples the player's later look direction.
 */
public final class SpellKineticsService {
    private static final Map<UUID, List<PendingCast>> PENDING = new HashMap<>();
    private static final int MAX_PENDING_PER_PLAYER = 32;

    private SpellKineticsService() {}

    public static boolean launch(ServerPlayer player, MagicPlayerData.CastPreparation cast,
                                 CombatGrowthService.Snapshot growthSnapshot) {
        SpellArchetype.Mode mode = SpellArchetype.mode(cast.spell().id());
        int circle = Math.max(1, Math.min(9, cast.spell().circle()));
        CastTargetSnapshot targetSnapshot = WorldMagicService.captureSnapshot(player, cast.spell(), cast.range());
        WorldMagicService.release(player, cast, targetSnapshot);

        if (ArcaneFieldService.handles(cast.spell().id()) && !EighthCircleSpellService.handles(cast.spell().id())) {
            boolean executed = targetSnapshot.executeLocked(player,
                    () -> ArcaneFieldService.executeSpecial(player, cast.spell().id(),
                            cast.range(), cast.power(), targetSnapshot));
            SpellCastingService.finishKineticCast(player, cast, growthSnapshot, executed);
            return executed;
        }

        if ("meteor_swarm".equals(cast.spell().id())) {
            MeteorBarragePattern.Strike first = MeteorBarragePattern.strike(targetSnapshot.barrageSeed(), 0);
            enqueue(player, new PendingCast(cast, growthSnapshot, targetSnapshot,
                    clock(player) + first.impactTick(), 0, MeteorBarragePattern.count(),
                    cast.power(), false, 0));
            ArcaneNoticeService.push(player, Component.literal(
                    "§6[운석 폭격] §f" + MeteorBarragePattern.count() + "발 연속 낙하"), 75);
            return true;
        }

        int presentationImpactDelay = SpellPresentationProfile.impactDelayTicks(cast.spell(),
                WorldMagicService.kineticDistance(player, cast.spell(), cast.range(), targetSnapshot));

        if (FirstCircleSpellService.handles(cast.spell().id())
                || SecondCircleSpellService.handles(cast.spell().id())
                || ThirdCircleSpellService.handles(cast.spell().id())
                || FourthCircleSpellService.handles(cast.spell().id())
                || FifthCircleSpellService.handles(cast.spell().id())
                || SixthCircleSpellService.handles(cast.spell().id())
                || SeventhCircleSpellService.handles(cast.spell().id())
                || EighthCircleSpellService.handles(cast.spell().id())
                || PlanarSpellService.handles(cast.spell().id()) || SimulacrumService.handles(cast.spell().id())
                || HighUtilitySpellService.handles(cast.spell().id()) || HighWardSpellService.handles(cast.spell().id())
                || HighControlSpellService.handles(cast.spell().id()) || SpellGameplayService.handles(cast.spell().id())) {
            if (presentationImpactDelay > 1) {
                enqueue(player, new PendingCast(cast, growthSnapshot, targetSnapshot,
                        clock(player) + presentationImpactDelay, 0, 1, cast.power(), false, 0));
                return true;
            }
            boolean executed = executeLocked(player, targetSnapshot, cast.spell().id(), cast.range(), cast.power());
            SpellCastingService.finishKineticCast(player, cast, growthSnapshot, executed);
            return executed;
        }

        if (mode == SpellArchetype.Mode.INSTANT) {
            if (presentationImpactDelay > 1) {
                enqueue(player, new PendingCast(cast, growthSnapshot, targetSnapshot,
                        clock(player) + presentationImpactDelay, 0, 1, cast.power(), false, 0));
                return true;
            }
            boolean executed = executeLocked(player, targetSnapshot, cast.spell().id(), cast.range(), cast.power());
            SpellCastingService.finishKineticCast(player, cast, growthSnapshot, executed);
            return executed;
        }

        if (mode == SpellArchetype.Mode.PROJECTILE) {
            if (presentationImpactDelay <= 1) {
                boolean executed = executeLocked(player, targetSnapshot, cast.spell().id(), cast.range(), cast.power());
                SpellCastingService.finishKineticCast(player, cast, growthSnapshot, executed);
                return executed;
            }
            enqueue(player, new PendingCast(cast, growthSnapshot, targetSnapshot,
                    clock(player) + presentationImpactDelay, 0, 1, cast.power(), false, 0));
            return true;
        }

        int totalPulses;
        int interval;
        double pulsePower;
        switch (mode) {
            case CHANNEL -> {
                totalPulses = Math.min(6, 3 + circle / 3);
                interval = Math.max(2, 4 - circle / 4);
                pulsePower = cast.power() * 1.08 / totalPulses;
                ArcaneNoticeService.push(player, Component.literal(
                        "§b[집중 방출] §f" + cast.spell().name() + " §7· " + totalPulses + "회 연속 타격"), 45);
            }
            case FIELD -> {
                totalPulses = Math.min(7, 4 + circle / 2);
                interval = Math.max(7, 12 - circle / 2);
                pulsePower = cast.power() * 0.96 / totalPulses;
                ArcaneNoticeService.push(player, Component.literal(
                        "§d[영역 전개] §f" + cast.spell().name() + " §7· " + totalPulses + "회 지속 맥동"), 55);
            }
            default -> throw new IllegalStateException("unhandled spell mode " + mode);
        }

        if (presentationImpactDelay > 1) {
            enqueue(player, new PendingCast(cast, growthSnapshot, targetSnapshot,
                    clock(player) + presentationImpactDelay, interval, totalPulses, pulsePower, false, 0));
            return true;
        }

        boolean first = executeLocked(player, targetSnapshot, cast.spell().id(), cast.range(), pulsePower);
        int remaining = totalPulses - 1;
        if (remaining <= 0) {
            SpellCastingService.finishKineticCast(player, cast, growthSnapshot, first);
            return first;
        }
        enqueue(player, new PendingCast(cast, growthSnapshot, targetSnapshot,
                clock(player) + interval, interval, remaining, pulsePower, first, 1));
        return true;
    }

    private static boolean executeLocked(ServerPlayer player, CastTargetSnapshot targetSnapshot,
                                         String spellId, double range, double power) {
        if (ArcaneFieldService.blocksCasting(player) || EighthCircleSpellService.blocksCasting(player)) return false;
        SpellDefinition spell = SpellCatalog.spell(spellId).orElse(null);
        if (FifthCircleSpellService.intercepts(player, targetSnapshot)) return false;
        if (spell != null && SixthCircleSpellService.intercepts(player, spell, targetSnapshot, range)) return false;
        if (spell != null && HighWardSpellService.intercepts(player, spell, targetSnapshot, range)) return false;

        boolean firstCircleOwned = FirstCircleSpellService.handles(spellId);
        boolean secondCircleOwned = !firstCircleOwned && SecondCircleSpellService.handles(spellId);
        boolean thirdCircleOwned = !firstCircleOwned && !secondCircleOwned && ThirdCircleSpellService.handles(spellId);
        boolean fourthCircleOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned
                && FourthCircleSpellService.handles(spellId);
        boolean fifthCircleOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned && !fourthCircleOwned
                && FifthCircleSpellService.handles(spellId);
        boolean sixthCircleOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned && !fourthCircleOwned
                && !fifthCircleOwned && SixthCircleSpellService.handles(spellId);
        boolean seventhCircleOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned && !fourthCircleOwned
                && !fifthCircleOwned && !sixthCircleOwned && SeventhCircleSpellService.handles(spellId);
        boolean eighthCircleOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned && !fourthCircleOwned
                && !fifthCircleOwned && !sixthCircleOwned && !seventhCircleOwned && EighthCircleSpellService.handles(spellId);
        boolean planarOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned && !fourthCircleOwned
                && !fifthCircleOwned && !sixthCircleOwned && !seventhCircleOwned && !eighthCircleOwned
                && PlanarSpellService.handles(spellId);
        boolean simulacrumOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned && !fourthCircleOwned
                && !fifthCircleOwned && !sixthCircleOwned && !seventhCircleOwned && !eighthCircleOwned && !planarOwned
                && SimulacrumService.handles(spellId);
        boolean utilityOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned && !fourthCircleOwned
                && !fifthCircleOwned && !sixthCircleOwned && !seventhCircleOwned && !eighthCircleOwned
                && !planarOwned && !simulacrumOwned && HighUtilitySpellService.handles(spellId);
        boolean wardOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned && !fourthCircleOwned
                && !fifthCircleOwned && !sixthCircleOwned && !seventhCircleOwned && !eighthCircleOwned
                && !planarOwned && !simulacrumOwned && !utilityOwned && HighWardSpellService.handles(spellId);
        boolean controlOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned && !fourthCircleOwned
                && !fifthCircleOwned && !sixthCircleOwned && !seventhCircleOwned && !eighthCircleOwned
                && !planarOwned && !simulacrumOwned && !utilityOwned && !wardOwned
                && HighControlSpellService.handles(spellId);
        boolean gameplayOwned = !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned && !fourthCircleOwned
                && !fifthCircleOwned && !sixthCircleOwned && !seventhCircleOwned && !eighthCircleOwned
                && !planarOwned && !simulacrumOwned && !utilityOwned && !wardOwned && !controlOwned
                && SpellGameplayService.handles(spellId);
        boolean executed = targetSnapshot.executeLocked(player, () -> firstCircleOwned
                ? FirstCircleSpellService.execute(player, spellId, range, power, targetSnapshot)
                : secondCircleOwned ? SecondCircleSpellService.execute(player, spellId, range, power, targetSnapshot)
                : thirdCircleOwned ? ThirdCircleSpellService.execute(player, spellId, range, power, targetSnapshot)
                : fourthCircleOwned ? FourthCircleSpellService.execute(player, spellId, range, power, targetSnapshot)
                : fifthCircleOwned ? FifthCircleSpellService.execute(player, spellId, range, power, targetSnapshot)
                : sixthCircleOwned ? SixthCircleSpellService.execute(player, spellId, range, power, targetSnapshot)
                : seventhCircleOwned ? SeventhCircleSpellService.execute(player, spellId, range, power, targetSnapshot)
                : eighthCircleOwned ? EighthCircleSpellService.execute(player, spellId, range, power, targetSnapshot)
                : planarOwned ? PlanarSpellService.execute(player, spellId)
                : simulacrumOwned ? SimulacrumService.execute(player, targetSnapshot)
                : utilityOwned ? HighUtilitySpellService.execute(player, spellId, range, power, targetSnapshot)
                : wardOwned ? HighWardSpellService.execute(player, spellId, range, power, targetSnapshot)
                : controlOwned ? HighControlSpellService.execute(player, spellId, range, power, targetSnapshot)
                : gameplayOwned ? SpellGameplayService.execute(player, spellId, range, power, targetSnapshot)
                : SpellCastingService.executeResolved(player, spellId, range, power));
        if (executed && !firstCircleOwned && !secondCircleOwned && !thirdCircleOwned && !fourthCircleOwned
                && !fifthCircleOwned && !sixthCircleOwned && !seventhCircleOwned && !eighthCircleOwned
                && !planarOwned && !simulacrumOwned && !utilityOwned && !wardOwned && !controlOwned && !gameplayOwned) {
            DestructiveMagicService.applyPhysicalAftermath(player, spellId, targetSnapshot, range, power);
        }
        return executed;
    }

    private static void enqueue(ServerPlayer player, PendingCast pending) {
        List<PendingCast> queue = PENDING.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>());
        while (queue.size() >= MAX_PENDING_PER_PLAYER) {
            PendingCast dropped = queue.removeFirst();
            WorldMagicService.cancelRelease(player, dropped.cast().spell().id());
            SpellCastingService.finishKineticCast(player, dropped.cast(), dropped.growthSnapshot(), dropped.anyExecuted());
        }
        queue.add(pending);
    }

    public static void tick(ServerPlayer player) {
        List<PendingCast> casts = PENDING.get(player.getUUID());
        if (casts == null || casts.isEmpty()) return;
        if (!player.isAlive() || player.isSpectator()
                || ArcaneFieldService.blocksCasting(player) || EighthCircleSpellService.blocksCasting(player)) {
            cancel(player);
            return;
        }
        long now = clock(player);
        Iterator<PendingCast> iterator = casts.iterator();
        while (iterator.hasNext()) {
            PendingCast pending = iterator.next();
            if (!pending.targetSnapshot().validFor(player)) {
                WorldMagicService.cancelRelease(player, pending.cast().spell().id());
                iterator.remove();
                continue;
            }
            if (now < pending.nextTick()) continue;
            boolean meteor = "meteor_swarm".equals(pending.cast().spell().id());
            boolean executed;
            if (meteor) {
                long seed = pending.targetSnapshot().barrageSeed();
                executed = pending.targetSnapshot().executeLocked(player,
                        () -> MeteorBarragePattern.withSeed(seed,
                                () -> HighCircleSpellEffects.meteorImpact(player,
                                        pending.targetSnapshot().target(), pending.cast().range(),
                                        pending.pulsePower(), pending.pulseIndex())));
            } else {
                executed = executeLocked(player, pending.targetSnapshot(), pending.cast().spell().id(),
                        pending.cast().range(), pending.pulsePower());
            }
            int remaining = pending.remainingPulses() - 1;
            boolean any = pending.anyExecuted() || executed;
            if (remaining <= 0) {
                iterator.remove();
                SpellCastingService.finishKineticCast(player, pending.cast(), pending.growthSnapshot(), any);
            } else if (meteor) {
                int nextIndex = pending.pulseIndex() + 1;
                long seed = pending.targetSnapshot().barrageSeed();
                int gap = Math.max(1, MeteorBarragePattern.strike(seed, nextIndex).impactTick()
                        - MeteorBarragePattern.strike(seed, pending.pulseIndex()).impactTick());
                pending.advanceMeteor(now + gap, remaining, any, nextIndex);
            } else {
                pending.advance(now + pending.interval(), remaining, any);
            }
        }
        if (casts.isEmpty()) PENDING.remove(player.getUUID());
    }

    public static void cancel(ServerPlayer player) {
        List<PendingCast> casts=PENDING.remove(player.getUUID());
        if(casts!=null&&!casts.isEmpty()){
            Set<String> spellIds=new HashSet<>();
            for(PendingCast pending:casts)spellIds.add(pending.cast().spell().id());
            for(String spellId:spellIds)WorldMagicService.cancelRelease(player,spellId);
        }
        WorldMagicService.stop(player);
    }

    public static void clear(UUID playerId) { PENDING.remove(playerId); }
    public static void clearAll() { PENDING.clear(); }
    private static long clock(ServerPlayer player) { return ((ServerLevel) player.level()).getServer().overworld().getGameTime(); }

    private static final class PendingCast {
        private final MagicPlayerData.CastPreparation cast;
        private final CombatGrowthService.Snapshot growthSnapshot;
        private final CastTargetSnapshot targetSnapshot;
        private long nextTick;
        private final int interval;
        private int remainingPulses;
        private final double pulsePower;
        private boolean anyExecuted;
        private int pulseIndex;

        private PendingCast(MagicPlayerData.CastPreparation cast, CombatGrowthService.Snapshot growthSnapshot,
                            CastTargetSnapshot targetSnapshot, long nextTick, int interval, int remainingPulses,
                            double pulsePower, boolean anyExecuted, int pulseIndex) {
            this.cast=cast; this.growthSnapshot=growthSnapshot; this.targetSnapshot=targetSnapshot;
            this.nextTick=nextTick; this.interval=interval; this.remainingPulses=remainingPulses;
            this.pulsePower=pulsePower; this.anyExecuted=anyExecuted; this.pulseIndex=pulseIndex;
        }
        MagicPlayerData.CastPreparation cast(){return cast;}
        CombatGrowthService.Snapshot growthSnapshot(){return growthSnapshot;}
        CastTargetSnapshot targetSnapshot(){return targetSnapshot;}
        long nextTick(){return nextTick;}
        int interval(){return interval;}
        int remainingPulses(){return remainingPulses;}
        double pulsePower(){return pulsePower;}
        boolean anyExecuted(){return anyExecuted;}
        int pulseIndex(){return pulseIndex;}
        void advance(long next,int remaining,boolean executed){nextTick=next;remainingPulses=remaining;anyExecuted=executed;pulseIndex++;}
        void advanceMeteor(long next,int remaining,boolean executed,int index){nextTick=next;remainingPulses=remaining;anyExecuted=executed;pulseIndex=index;}
    }
}
