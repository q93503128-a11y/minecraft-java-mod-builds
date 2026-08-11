package kr.moonseungjun.villageguardians;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Set;

/** Keeps modal interfaces visually isolated from vanilla HUD, chat and title layers. */
@EventBusSubscriber(value = Dist.CLIENT, modid = VillageGuardians.MOD_ID)
public final class VillageUiHudSuppressor {
    private static final Set<Identifier> BLOCKED = Set.of(
            VanillaGuiLayers.CROSSHAIR,
            VanillaGuiLayers.HOTBAR,
            VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.ARMOR_LEVEL,
            VanillaGuiLayers.FOOD_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH,
            VanillaGuiLayers.AIR_LEVEL,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND,
            VanillaGuiLayers.EXPERIENCE_LEVEL,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR,
            VanillaGuiLayers.SELECTED_ITEM_NAME,
            VanillaGuiLayers.EFFECTS,
            VanillaGuiLayers.BOSS_OVERLAY,
            VanillaGuiLayers.SCOREBOARD_SIDEBAR,
            VanillaGuiLayers.OVERLAY_MESSAGE,
            VanillaGuiLayers.TITLE,
            VanillaGuiLayers.CHAT
    );

    private VillageUiHudSuppressor() {}

    @SubscribeEvent
    public static void beforeGuiLayer(RenderGuiLayerEvent.Pre event) {
        Screen screen = Minecraft.getInstance().gui.screen();
        if (isVillageModal(screen) && BLOCKED.contains(event.getName())) {
            event.setCanceled(true);
        }
    }

    static boolean isVillageModal(Screen screen) {
        return screen instanceof VillageCommandCenterScreen
                || screen instanceof VillageQuickChatSafeScreen
                || screen instanceof VillageFusionSafeScreen
                || screen instanceof VillageSkillTreeScreen
                || screen instanceof VillageRoleProgressScreen
                || screen instanceof VillageRelicScreen
                || screen instanceof VillageRelicChoiceScreen
                || screen instanceof VillageWaveIntelScreen
                || screen instanceof VillageGameOverScreen
                || screen instanceof VillageSkillTestScreen
                || screen instanceof VillageSkillTestPasswordScreen
                || screen instanceof VillageFacilityScreen
                || screen instanceof VillageResultScreen
                || screen instanceof VillageConfirmScreen
                || screen instanceof VillageUiScreen;
    }
}
