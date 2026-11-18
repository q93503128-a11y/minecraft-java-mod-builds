package kr.moonseungjun.anomalousclasses.client;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.network.payload.RequestClassManagementPayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestProfilePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestSkillTreePayload;
import kr.moonseungjun.anomalousclasses.network.payload.MentalIntrusionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.MentalSpellPayload;
import kr.moonseungjun.anomalousclasses.network.payload.PuppetTargetPayload;
import kr.moonseungjun.anomalousclasses.network.payload.PuppetAbilityPayload;
import kr.moonseungjun.anomalousclasses.network.payload.ParasiteInputPayload;
import kr.moonseungjun.anomalousclasses.network.payload.ParasiteMovePayload;
import kr.moonseungjun.anomalousclasses.network.payload.ParasiteAbilityPayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestMentalDominatorPayload;
import kr.moonseungjun.anomalousclasses.network.payload.BarrierInputPayload;
import kr.moonseungjun.anomalousclasses.network.payload.PactAbilityPayload;
import kr.moonseungjun.anomalousclasses.network.payload.ParasiteHostAttackPayload;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = AnomalousClasses.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    private static boolean intrusionWasDown;
    private static int mentalRefreshTicks;
    private static int parasiteMovementTicks;
    private ClientGameEvents() {}

    @SubscribeEvent
    public static void onAttackInput(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.isAttack() || minecraft.screen != null || !ParasiteViewClient.isControlling()) return;
        // Keep ordinary survival mining functional. The camera's apparent
        // entity hit can disagree with the passenger's vanilla reach check;
        // send any non-block attack intent and let the server raycast the host.
        if (minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK) return;
        event.setCanceled(true);
        PacketDistributor.sendToServer(ParasiteHostAttackPayload.INSTANCE);
    }

    @SubscribeEvent
    public static void onRenderLocalPlayer(RenderPlayerEvent.Pre event) {
        // The passenger's logical body remains real for inventory/XP, but it
        // must not visually hover above the host from its owner's camera.
        if (event.getEntity() == Minecraft.getInstance().player
                && ParasiteViewClient.isControlling()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ParasiteViewClient.tick(minecraft);
        SkillTreeNavigation.tick(minecraft);
        if (minecraft.player == null || minecraft.level == null) {
            ParasiteTraitClientControls.reset();
            return;
        }
        boolean screenOpen = minecraft.screen != null;
        boolean intrusionDown = !screenOpen && ClassSelectionKeyMappings.MENTAL_INTRUSION.isDown();
        if (intrusionDown != intrusionWasDown) {
            PacketDistributor.sendToServer(new MentalIntrusionPayload(intrusionDown
                    ? MentalIntrusionPayload.START : MentalIntrusionPayload.STOP));
            PacketDistributor.sendToServer(new ParasiteInputPayload(intrusionDown
                    ? ParasiteInputPayload.START : ParasiteInputPayload.STOP));
            if (intrusionDown) {
                PacketDistributor.sendToServer(PuppetTargetPayload.INSTANCE);
                if (ParasiteTraitClientControls.isBarrier())
                    PacketDistributor.sendToServer(new BarrierInputPayload(BarrierInputPayload.PREVIEW_OR_PLACE));
                if (ParasiteTraitClientControls.isPact())
                    PacketDistributor.sendToServer(new PactAbilityPayload(6));
            }
            intrusionWasDown = intrusionDown;
        }
        if (screenOpen) {
            if (minecraft.screen instanceof MentalDominatorScreen) {
                if (++mentalRefreshTicks >= 20) {
                    mentalRefreshTicks = 0;
                    PacketDistributor.sendToServer(RequestMentalDominatorPayload.INSTANCE);
                }
            } else {
                mentalRefreshTicks = 0;
            }
            return;
        }
        mentalRefreshTicks = 0;
        if (ParasiteViewClient.isControlling() && ++parasiteMovementTicks % 2 == 0) {
            int mask = (minecraft.options.keyUp.isDown() ? 1 : 0)
                    | (minecraft.options.keyDown.isDown() ? 2 : 0)
                    | (minecraft.options.keyLeft.isDown() ? 4 : 0)
                    | (minecraft.options.keyRight.isDown() ? 8 : 0)
                    | (minecraft.options.keyJump.isDown() ? 16 : 0)
                    | (minecraft.options.keySprint.isDown() ? 32 : 0);
            PacketDistributor.sendToServer(new ParasiteMovePayload(mask,
                    minecraft.player.getYRot(), minecraft.player.getXRot()));
        }
        // Direct keybinds request stable IDs; the server rejects all other classes.
        if (ClassSelectionKeyMappings.MENTAL_SHOCK.consumeClick()) {
            PacketDistributor.sendToServer(new MentalSpellPayload(0));
            PacketDistributor.sendToServer(new PuppetAbilityPayload(0));
            PacketDistributor.sendToServer(new ParasiteAbilityPayload(0));
            if (ParasiteTraitClientControls.isBarrier())
                PacketDistributor.sendToServer(new BarrierInputPayload(BarrierInputPayload.CYCLE_KIND));
            if (ParasiteTraitClientControls.isPact())
                PacketDistributor.sendToServer(new PactAbilityPayload(0));
        }
        if (ClassSelectionKeyMappings.MENTAL_SUGGESTION.consumeClick()) {
            PacketDistributor.sendToServer(new MentalSpellPayload(1));
            PacketDistributor.sendToServer(new PuppetAbilityPayload(1));
            PacketDistributor.sendToServer(new ParasiteAbilityPayload(1));
            if (ParasiteTraitClientControls.isBarrier())
                PacketDistributor.sendToServer(new BarrierInputPayload(BarrierInputPayload.CYCLE_SIZE));
            if (ParasiteTraitClientControls.isPact())
                PacketDistributor.sendToServer(new PactAbilityPayload(1));
        }
        if (ClassSelectionKeyMappings.MENTAL_OVERLOAD.consumeClick()) {
            PacketDistributor.sendToServer(new MentalSpellPayload(2));
            PacketDistributor.sendToServer(new PuppetAbilityPayload(2));
            PacketDistributor.sendToServer(new ParasiteAbilityPayload(2));
            if (ParasiteTraitClientControls.isBarrier())
                PacketDistributor.sendToServer(new BarrierInputPayload(BarrierInputPayload.BOLT));
            if (ParasiteTraitClientControls.isPact())
                PacketDistributor.sendToServer(new PactAbilityPayload(2));
        }
        if (ClassSelectionKeyMappings.MENTAL_CONTAINMENT.consumeClick()) {
            if (ParasiteTraitClientControls.isParasite())
                ParasiteTraitClientControls.toggle();
            else if (ParasiteTraitClientControls.isBarrier())
                PacketDistributor.sendToServer(new BarrierInputPayload(BarrierInputPayload.FLASH));
            else if (ParasiteTraitClientControls.isPact())
                PacketDistributor.sendToServer(new PactAbilityPayload(3));
            else
                PacketDistributor.sendToServer(new MentalSpellPayload(3));
        }
        if (ClassSelectionKeyMappings.BARRIER_AMPLIFY.consumeClick()) {
            if (ParasiteTraitClientControls.isBarrier())
                PacketDistributor.sendToServer(new BarrierInputPayload(BarrierInputPayload.AMPLIFY));
            if (ParasiteTraitClientControls.isPact())
                PacketDistributor.sendToServer(new PactAbilityPayload(4));
        }
        if (ClassSelectionKeyMappings.BARRIER_REPEAT.consumeClick()) {
            if (ParasiteTraitClientControls.isBarrier())
                PacketDistributor.sendToServer(new BarrierInputPayload(BarrierInputPayload.REPEAT));
            if (ParasiteTraitClientControls.isPact())
                PacketDistributor.sendToServer(new PactAbilityPayload(5));
        }
        if (ClassSelectionKeyMappings.BARRIER_CANCEL.consumeClick()
                && ParasiteTraitClientControls.isBarrier())
            PacketDistributor.sendToServer(new BarrierInputPayload(BarrierInputPayload.CANCEL_NEAREST));
        if (ClassSelectionKeyMappings.OPEN_PROFILE.consumeClick()) {
            PacketDistributor.sendToServer(RequestProfilePayload.INSTANCE);
        } else if (ClassSelectionKeyMappings.OPEN_SKILL_TREE.consumeClick()) {
            SkillTreeNavigation.begin(SkillTreeNavigation.ReturnTarget.WORLD);
            PacketDistributor.sendToServer(RequestSkillTreePayload.INSTANCE);
        } else if (ClassSelectionKeyMappings.OPEN_CLASS_MANAGEMENT.consumeClick()) {
            ClassManagementNavigation.enterFromWorld();
            PacketDistributor.sendToServer(RequestClassManagementPayload.INSTANCE);
        }
    }
}
