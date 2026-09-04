package kr.moonseungjun.survivalascension.expedition;

import kr.moonseungjun.survivalascension.apex.ApexHuntSystem;
import kr.moonseungjun.survivalascension.compat.ContentPackCompatibility;
import kr.moonseungjun.survivalascension.compat.TargetedResonanceRecovery;
import kr.moonseungjun.survivalascension.endgame.AscensionTrialSystem;
import kr.moonseungjun.survivalascension.endgame.FinalAscensionSystem;
import kr.moonseungjun.survivalascension.production.OutpostData;
import kr.moonseungjun.survivalascension.production.OutpostService;
import kr.moonseungjun.survivalascension.production.ProductionData;
import kr.moonseungjun.survivalascension.progress.SkillProgressionService;
import kr.moonseungjun.survivalascension.world.WorldAscensionData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.function.BooleanSupplier;

public final class ExpeditionOperationSystem {
    public static final int START_RADIUS = 4;
    public static final int WORK_RADIUS = 48;
    public static final int RETURN_RADIUS = 8;
    public static final int SUPPLY_CHARGE_COST = 1;
    public static final int FORWARD_SHIFT_EXTRA = 48;

    private ExpeditionOperationSystem() {}

    public static boolean isActive(ServerPlayer player) { return ExpeditionOperationData.get(player).active(player) != null; }

    public static void startOrStatus(ServerPlayer player) { startOrStatus(player, () -> true); }

    public static void startOrStatus(ServerPlayer player, BooleanSupplier localSupplyCommit) {
        if (player.isCreative() || player.isSpectator()) { player.sendSystemMessage(Component.literal("§6[원정 작전] §f크리에이티브/관전자 상태에서는 원정 작전을 시작할 수 없습니다.")); return; }
        ExpeditionOperationData data = ExpeditionOperationData.get(player);
        if (data.active(player) != null) { sendStatus(player); return; }
        if (FinalAscensionSystem.isFinalSequenceActive(player)) { player.sendSystemMessage(Component.literal("§6[원정 작전] §f최후의 승천 진행 중에는 새 원정 작전을 시작할 수 없습니다.")); return; }
        if (FinalAscensionSystem.hasOtherMajorActivity(player)) { player.sendSystemMessage(Component.literal("§6[원정 작전] §f진행 중인 현장 사건·방어전·정점 사냥·승천 시련을 먼저 끝내세요.")); return; }

        OutpostData.OutpostEntry outpost = OutpostService.nearestActiveOutpost(player, START_RADIUS);
        if (outpost == null) { player.sendSystemMessage(Component.literal("§6[원정 작전] §f활성 전초기지 4블록 안에서 시작해야 합니다.")); return; }
        ServerLevel level = (ServerLevel) player.level();
        ExpeditionRegion region = regionAt(level, outpost.pos());
        if (region == null) { player.sendSystemMessage(Component.literal("§6[원정 작전] §f이 전초기지는 현재 원정권으로 분류되지 않는 위치에 있습니다.")); return; }
        ExpeditionData expedition = ExpeditionData.get(player);
        if (!expedition.isComplete(player, region)) { player.sendSystemMessage(Component.literal("§6[원정 작전] §f먼저 이 전초의 §e" + region.koreanName() + " 현장 지령§f을 완수해야 합니다.")); return; }
        ProductionData production = ProductionData.get(player);
        if (production.supplyCharges(player) < SUPPLY_CHARGE_COST) { player.sendSystemMessage(Component.literal("§6[원정 작전] §f출발 준비에는 §e현장 보급권 1개§f가 필요합니다.")); return; }

        ExpeditionOperation operation = ExpeditionOperation.forRegion(region);
        ExpeditionComplication complication = chooseComplication(player, level, data);
        long deadline = level.getGameTime() + operation.durationTicks();
        if (!data.start(player, operation, outpost.dimension(), outpost.pos(), deadline, complication)) { player.sendSystemMessage(Component.literal("§c[원정 작전] §f작전 상태가 바뀌어 출발하지 못했습니다.")); return; }
        if (!localSupplyCommit.getAsBoolean()) {
            data.fail(player);
            player.sendSystemMessage(Component.literal("§c[원정 작전] §f전초의 현지 실물 보급 재고가 바뀌어 출발하지 않았습니다. §7보급권은 소비되지 않았습니다."));
            return;
        }
        if (!production.consumeSupplyCharge(player)) {
            data.fail(player);
            player.sendSystemMessage(Component.literal("§c[원정 작전] §f보급권 상태가 바뀌어 출발을 취소했습니다. 보급권은 소비되지 않았습니다."));
            return;
        }
        player.sendSystemMessage(Component.literal("§6[원정 작전 출발] §f" + operation.koreanName() + " §7· 보급권1 소비"));
        player.sendSystemMessage(Component.literal("§c[작전 변수] §f" + complication.koreanName() + " §7· " + complication.description()));
        if (region == ExpeditionRegion.DEEP && ContentPackCompatibility.hasResonanceOperationRewards()) player.sendSystemMessage(Component.literal("§d[공명 회수 계약] §f심층 작전 귀환 성공 시 외부 공명 장비 1개를 확보합니다. §7귀환 순간 주손/보조손 장비 종류로 목표를 좁힐 수 있습니다. 현재: §d" + TargetedResonanceRecovery.describeFocus(player.getMainHandItem(), player.getOffhandItem())));
        player.sendSystemMessage(Component.literal("§7전초에서 최소 §e" + operation.rangeTarget() + "블록§7까지 전진한 뒤, 전초 반경 " + WORK_RADIUS + "블록 밖의 §f" + region.koreanName() + "§7에서 수행: §f" + operation.taskSummary()));
        player.sendSystemMessage(Component.literal("§7목표를 끝낸 뒤 같은 전초기지 반경 §e" + RETURN_RADIUS + "블록§7로 돌아와야 보상을 받습니다. §f제한 " + operation.durationTicks() / 1200 + "분"));
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        ExpeditionOperationData data = ExpeditionOperationData.get(player);
        ExpeditionOperationData.ActiveOperation active = data.active(player);
        if (active == null) return;
        if (player.isCreative() || player.isSpectator()) { fail(player, "게임 모드가 변경되어 작전이 종료되었습니다."); return; }
        if (!(player.level() instanceof ServerLevel level) || !active.dimension().equals(level.dimension().toString())) { fail(player, "작전 중 다른 차원으로 이탈했습니다."); return; }
        ExpeditionOperation operation = ExpeditionOperation.forRegion(active.region());
        long now = level.getGameTime();
        if (now >= active.deadline()) { fail(player, "작전 제한시간을 초과했습니다."); return; }
        active = recoverComplicationState(player, data, active, operation, level);
        if (active == null) return;
        if (active.complication() == ExpeditionComplication.HOT_EXTRACTION && active.complicationState() == 1 && active.extractionDeadline() > 0L && now >= active.extractionDeadline()) { fail(player, "긴급 철수 제한시간을 초과했습니다."); return; }
        double distanceSq = active.anchor().distSqr(player.blockPosition());
        if (!active.rangeReached() && distanceSq >= operation.rangeTarget() * operation.rangeTarget()) if (data.markRangeReached(player)) player.sendSystemMessage(Component.literal("§6[작전 전진선 돌파] §f" + operation.koreanName() + " §7· 이제 전초 " + WORK_RADIUS + "블록 밖의 " + active.region().koreanName() + "에서 현장 목표가 기록됩니다."));
        if (active.complication() == ExpeditionComplication.FORWARD_SHIFT && active.complicationState() > 0 && distanceSq >= (double) active.complicationState() * active.complicationState() && ExpeditionProgression.currentRegion(player) == active.region()) if (data.completeForwardShift(player)) { player.sendSystemMessage(Component.literal("§a[전선 재전개 완료] §f추가 전진선을 확보했습니다. 남은 현장 목표 기록이 재개됩니다.")); active = data.active(player); }
        if (data.objectivesComplete(player, operation) && distanceSq <= RETURN_RADIUS * RETURN_RADIUS && OutpostService.isRecoveryOperational(player, level, active.dimension(), active.anchor())) complete(player, operation);
    }

    public static void onLivingDeath(LivingDeathEvent event) { if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player && isActive(player)) fail(player, "작전 중 사망했습니다. 투입한 보급권은 반환되지 않습니다."); }
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) { if (event.getEntity() instanceof ServerPlayer player && isActive(player)) { player.sendSystemMessage(Component.literal("§6[원정 작전 재개] §f로그아웃 전 진행 중이던 작전이 유지되어 있습니다.")); sendStatus(player); } }

    public static void recordAction(ServerPlayer player, ExpeditionAction action, int amount) {
        if (amount <= 0 || player.isCreative() || player.isSpectator()) return;
        ExpeditionOperationData data = ExpeditionOperationData.get(player); ExpeditionOperationData.ActiveOperation active = data.active(player);
        if (active == null || !active.rangeReached()) return;
        if (!(player.level() instanceof ServerLevel level) || !active.dimension().equals(level.dimension().toString())) return;
        double distanceSq = active.anchor().distSqr(player.blockPosition());
        if (distanceSq < WORK_RADIUS * WORK_RADIUS || ExpeditionProgression.currentRegion(player) != active.region()) return;
        ExpeditionOperation operation = ExpeditionOperation.forRegion(active.region());
        if (active.complication() == ExpeditionComplication.DEEP_FRONT && distanceSq < operation.rangeTarget() * operation.rangeTarget()) return;
        if (active.complication() == ExpeditionComplication.FORWARD_SHIFT && active.complicationState() > 0) return;
        for (int i = 0; i < operation.tasks().size(); i++) {
            ExpeditionOperation.Task task = operation.tasks().get(i); if (task.action() != action) continue;
            ExpeditionOperationData.ProgressResult result = data.addProgress(player, i, amount, task.target()); if (result.newProgress() == result.oldProgress()) continue;
            boolean objectivesComplete = data.objectivesComplete(player, operation);
            if (result.taskCompletedNow()) {
                player.sendSystemMessage(Component.literal("§6[작전 목표 완료] §f" + task.action().koreanName() + " §a" + result.newProgress() + "/" + task.target()));
                ExpeditionOperationData.ActiveOperation refreshed = data.active(player);
                if (!objectivesComplete && refreshed != null && refreshed.complication() == ExpeditionComplication.FORWARD_SHIFT && refreshed.complicationState() == 0) { int targetRadius = operation.rangeTarget() + FORWARD_SHIFT_EXTRA; if (data.beginForwardShift(player, targetRadius)) player.sendSystemMessage(Component.literal("§c[전선 재전개] §f첫 현장 목표가 끝났습니다. 남은 목표는 원점 기준 §e" + targetRadius + "블록§f까지 추가 전진한 뒤 다시 기록됩니다.")); }
            } else { int oldQuarter=result.oldProgress()*4/task.target(),newQuarter=result.newProgress()*4/task.target(); if(newQuarter>oldQuarter) player.sendSystemMessage(Component.literal("§6[작전 진행] §f"+task.action().koreanName()+" §e"+result.newProgress()+"/"+task.target()),true); }
            if (objectivesComplete) {
                ExpeditionOperationData.ActiveOperation refreshed=data.active(player);
                if(refreshed!=null&&refreshed.complication()==ExpeditionComplication.HOT_EXTRACTION&&refreshed.complicationState()==0){int window=refreshed.complication().extractionWindowTicks(operation);if(data.armExtraction(player,level.getGameTime()+window))player.sendSystemMessage(Component.literal("§c[긴급 철수] §f현장 목표 완료. §e"+window/1200+"분 "+(window/20)%60+"초§f 안에 같은 전초8블록으로 귀환해야 합니다."));}
                player.sendSystemMessage(Component.literal("§a[작전 현장 목표 완료] §f같은 전초기지 반경 "+RETURN_RADIUS+"블록으로 복귀하면 작전이 완료됩니다."));
            }
            return;
        }
    }

    public static void sendStatus(ServerPlayer player) {
        ExpeditionOperationData data=ExpeditionOperationData.get(player); ExpeditionOperationData.ActiveOperation active=data.active(player);
        player.sendSystemMessage(Component.literal("§6[원정 작전] §f지역 최초 완수 §e"+data.uniqueCompleted(player)+"/9 §7· 총 귀환 성공 §f"+data.totalCompletions(player)+(data.masteryClaimed(player)?" §6· 9종 완주 보상 수령":"")));
        if(active==null){player.sendSystemMessage(Component.literal("§7활성 전초기지에서 보급권1로 시작 · 완수한 해당 원정권 필요 · 출발/현지작업/귀환 + 작전 변수1개"));return;}
        ExpeditionOperation operation=ExpeditionOperation.forRegion(active.region()); long now=player.level() instanceof ServerLevel level?level.getGameTime():0L; long seconds=Math.max(0L,(active.deadline()-now+19L)/20L);
        player.sendSystemMessage(Component.literal("§f"+operation.koreanName()+" §7· 전진선 "+(active.rangeReached()?"§a돌파":"§e"+operation.rangeTarget()+"블록 필요")+" §7· 남은 "+seconds/60+"분 "+seconds%60+"초"));
        player.sendSystemMessage(Component.literal("§c작전 변수 §f"+active.complication().koreanName()+" §7· "+active.complication().description()));
        if(active.region()==ExpeditionRegion.DEEP&&ContentPackCompatibility.hasResonanceOperationRewards())player.sendSystemMessage(Component.literal("  §d공명 회수 계약 §7· 귀환 시 현재 손 장비 종류 우선 · 현재 §d"+TargetedResonanceRecovery.describeFocus(player.getMainHandItem(),player.getOffhandItem())));
        if(active.complication()==ExpeditionComplication.FORWARD_SHIFT&&active.complicationState()>0)player.sendSystemMessage(Component.literal("  §c재전개 대기 §7· 원점에서 §e"+active.complicationState()+"블록§7까지 추가 전진 필요")); else if(active.complication()==ExpeditionComplication.FORWARD_SHIFT&&active.complicationState()<0)player.sendSystemMessage(Component.literal("  §a전선 재전개 완료")); else if(active.complication()==ExpeditionComplication.HOT_EXTRACTION&&active.complicationState()==1){long extractionSeconds=Math.max(0L,(active.extractionDeadline()-now+19L)/20L);player.sendSystemMessage(Component.literal("  §c긴급 철수 §e"+extractionSeconds/60+"분 "+extractionSeconds%60+"초 남음"));}
        player.sendSystemMessage(Component.literal("  §7- §f"+operation.tasks().get(0).action().koreanName()+" §e"+active.progressA()+"§7/§f"+operation.tasks().get(0).target()));
        player.sendSystemMessage(Component.literal("  §7- §f"+operation.tasks().get(1).action().koreanName()+" §e"+active.progressB()+"§7/§f"+operation.tasks().get(1).target()));
    }

    private static ExpeditionOperationData.ActiveOperation recoverComplicationState(ServerPlayer player, ExpeditionOperationData data, ExpeditionOperationData.ActiveOperation active, ExpeditionOperation operation, ServerLevel level) {
        if(active.complication()==ExpeditionComplication.FORWARD_SHIFT&&active.complicationState()==0){boolean aDone=active.progressA()>=operation.tasks().get(0).target(),bDone=active.progressB()>=operation.tasks().get(1).target();if(aDone^bDone){int targetRadius=operation.rangeTarget()+FORWARD_SHIFT_EXTRA;if(data.beginForwardShift(player,targetRadius)){player.sendSystemMessage(Component.literal("§c[전선 재전개 복구] §f저장된 첫 목표 완료 상태를 확인했습니다. 원점 기준 §e"+targetRadius+"블록§f까지 추가 전진해야 남은 목표가 재개됩니다."));active=data.active(player);}}}
        if(active!=null&&active.complication()==ExpeditionComplication.HOT_EXTRACTION&&active.complicationState()==0&&data.objectivesComplete(player,operation)){int window=active.complication().extractionWindowTicks(operation);if(data.armExtraction(player,level.getGameTime()+window)){player.sendSystemMessage(Component.literal("§c[긴급 철수 복구] §f저장된 현장 목표 완료 상태를 확인해 귀환 제한시간을 복구했습니다."));active=data.active(player);}}
        return active;
    }
    private static ExpeditionComplication chooseComplication(ServerPlayer player,ServerLevel level,ExpeditionOperationData data){long mix=level.getGameTime()^player.getUUID().getMostSignificantBits()^player.getUUID().getLeastSignificantBits()^((long)data.totalCompletions(player)<<32);return switch(Math.floorMod(Long.hashCode(mix),6)){case 0->ExpeditionComplication.DEEP_FRONT;case 1->ExpeditionComplication.FORWARD_SHIFT;case 2->ExpeditionComplication.HOT_EXTRACTION;case 3->ExpeditionComplication.PURSUIT;case 4->ExpeditionComplication.ANOMALY_SURGE;default->ExpeditionComplication.HIDDEN_AMBUSH;};}
    private static ExpeditionRegion regionAt(ServerLevel level,BlockPos pos){int worldStage=WorldAscensionData.get(level.getServer()).stage();for(ExpeditionRegion region:ExpeditionRegion.values())if(worldStage>=region.requiredWorldStage()&&region.matches(level.getBiome(pos)))return region;return null;}

    private static void complete(ServerPlayer player,ExpeditionOperation operation){ExpeditionOperationData data=ExpeditionOperationData.get(player);ExpeditionOperationData.CompletionResult result=data.complete(player,operation.region());SkillProgressionService.award(player,operation.region().rewardSkill(),operation.skillXpReward());player.giveExperiencePoints(operation.experienceReward());int stage=operation.region().requiredWorldStage();if(stage==0){giveOrDrop(player,new ItemStack(Items.EMERALD,8));giveOrDrop(player,new ItemStack(Items.AMETHYST_SHARD,8));}else if(stage==1){giveOrDrop(player,new ItemStack(Items.DIAMOND,2));giveOrDrop(player,new ItemStack(Items.AMETHYST_SHARD,16));giveOrDrop(player,new ItemStack(Items.ECHO_SHARD,2));}else{giveOrDrop(player,new ItemStack(Items.DIAMOND,4));giveOrDrop(player,new ItemStack(Items.ECHO_SHARD,4));giveOrDrop(player,new ItemStack(Items.DRAGON_BREATH,2));}
        if(operation.region()==ExpeditionRegion.DEEP&&player.level() instanceof ServerLevel level){TargetedResonanceRecovery.Recovery recovery=TargetedResonanceRecovery.select(level.getRandom(),player.getMainHandItem(),player.getOffhandItem());ItemStack resonanceReward=recovery.stack();if(!resonanceReward.isEmpty()){String itemName=resonanceReward.getHoverName().getString();giveOrDrop(player,resonanceReward);player.sendSystemMessage(Component.literal("§d[공명 회수] §f심층 작전에서 §d"+itemName+"§f을 확보했습니다. §7"+recovery.focusLabel()+(recovery.focused()?" 적용":"")+" · 원본 기능은 유지되며 승천 각인은 장비 메뉴에서 직접 선택합니다."));}}
        player.sendSystemMessage(Component.literal("§a[원정 작전 귀환] §f"+operation.koreanName()+" 완료 · "+operation.region().rewardSkill().koreanName()+" 숙련 XP +"+operation.skillXpReward()+" · 경험치 +"+operation.experienceReward()+" §7· 최초 "+result.uniqueCompleted()+"/9 · 총 "+result.totalCompletions()+"회"));
        if(result.masteryNow()){giveOrDrop(player,new ItemStack(Items.NETHERITE_SCRAP,2));giveOrDrop(player,new ItemStack(Items.ECHO_SHARD,16));giveOrDrop(player,new ItemStack(Items.AMETHYST_SHARD,64));giveOrDrop(player,new ItemStack(Items.DRAGON_BREATH,8));player.giveExperiencePoints(300);player.sendSystemMessage(Component.literal("§6[전초작전 9종 완주] §f모든 지역의 출발-현지작업-귀환 작전을 최초 1회 이상 완료했습니다."));player.sendSystemMessage(Component.literal("§7보상 · 네더라이트 파편2 · 메아리16 · 자수정64 · 드래곤의 숨결8 · 경험치300"));}}
    private static void fail(ServerPlayer player,String reason){if(ExpeditionOperationData.get(player).fail(player))player.sendSystemMessage(Component.literal("§c[원정 작전 실패] §f"+reason));}
    private static void giveOrDrop(ServerPlayer player,ItemStack stack){if(!player.getInventory().add(stack))player.drop(stack,false);}
}
