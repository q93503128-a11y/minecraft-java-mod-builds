package kr.moonseungjun.anomalousclasses.client;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.network.payload.RequestClassManagementPayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestProfilePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestSkillTreePayload;
import kr.moonseungjun.anomalousclasses.network.payload.MentalIntrusionPayload;
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
