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
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        SkillTreeNavigation.tick(minecraft);
        if (minecraft.player == null || minecraft.level == null) return;
        boolean screenOpen = minecraft.screen != null;
        boolean intrusionDown = !screenOpen && ClassSelectionKeyMappings.MENTAL_INTRUSION.isDown();
        if (intrusionDown != intrusionWasDown) {
            PacketDistributor.sendToServer(new MentalIntrusionPayload(intrusionDown
                    ? MentalIntrusionPayload.START : MentalIntrusionPayload.STOP));
            PacketDistributor.sendToServer(new ParasiteInputPayload(intrusionDown
                    ? ParasiteInputPayload.START : ParasiteInputPayload.STOP));
            if (intrusionDown) PacketDistributor.sendToServer(PuppetTargetPayload.INSTANCE);
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
        if (minecraft.player.getVehicle() instanceof net.minecraft.world.entity.Mob
                && ++parasiteMovementTicks % 2 == 0) {
            int mask = (minecraft.options.keyUp.isDown() ? 1 : 0)
                    | (minecraft.options.keyDown.isDown() ? 2 : 0)
                    | (minecraft.options.keyLeft.isDown() ? 4 : 0)
                    | (minecraft.options.keyRight.isDown() ? 8 : 0)
                    | (minecraft.options.keyJump.isDown() ? 16 : 0)
                    | (minecraft.options.keySprint.isDown() ? 32 : 0);
            PacketDistributor.sendToServer(new ParasiteMovePayload(mask, minecraft.player.getYRot()));
        }
        // Direct keybinds request stable IDs; the server rejects all other classes.
        if (ClassSelectionKeyMappings.MENTAL_SHOCK.consumeClick()) {
            PacketDistributor.sendToServer(new MentalSpellPayload(0));
            PacketDistributor.sendToServer(new PuppetAbilityPayload(0));
            PacketDistributor.sendToServer(new ParasiteAbilityPayload(0));
        }
        if (ClassSelectionKeyMappings.MENTAL_SUGGESTION.consumeClick()) {
            PacketDistributor.sendToServer(new MentalSpellPayload(1));
            PacketDistributor.sendToServer(new PuppetAbilityPayload(1));
            PacketDistributor.sendToServer(new ParasiteAbilityPayload(1));
        }
        if (ClassSelectionKeyMappings.MENTAL_OVERLOAD.consumeClick()) {
            PacketDistributor.sendToServer(new MentalSpellPayload(2));
            PacketDistributor.sendToServer(new PuppetAbilityPayload(2));
            PacketDistributor.sendToServer(new ParasiteAbilityPayload(2));
        }
        if (ClassSelectionKeyMappings.MENTAL_CONTAINMENT.consumeClick()) {
            PacketDistributor.sendToServer(new MentalSpellPayload(3));
        }
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
