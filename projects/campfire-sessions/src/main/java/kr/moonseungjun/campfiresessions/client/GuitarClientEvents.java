package kr.moonseungjun.campfiresessions.client;

import kr.moonseungjun.campfiresessions.CampfireSessions;
import kr.moonseungjun.campfiresessions.registry.ModItems;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT, modid = CampfireSessions.MOD_ID)
public final class GuitarClientEvents {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(CampfireSessions.MOD_ID, "music"));

    private static final KeyMapping PREVIOUS_TRACK = new KeyMapping(
            "key.campfiresessions.previous_track", GLFW.GLFW_KEY_B, CATEGORY);
    private static final KeyMapping NEXT_TRACK = new KeyMapping(
            "key.campfiresessions.next_track", GLFW.GLFW_KEY_N, CATEGORY);
    private static final KeyMapping REPEAT_TRACK = new KeyMapping(
            "key.campfiresessions.repeat_track", GLFW.GLFW_KEY_R, CATEGORY);

    private GuitarClientEvents() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(PREVIOUS_TRACK);
        event.register(NEXT_TRACK);
        event.register(REPEAT_TRACK);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getItemStack().is(ModItems.ACOUSTIC_GUITAR.get())) return;
        openScreen();
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(ModItems.ACOUSTIC_GUITAR.get())) return;
        openScreen();
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        CampfireMusicClient.clientTick();

        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) return;

        boolean holdingGuitar = player.getMainHandItem().is(ModItems.ACOUSTIC_GUITAR.get())
                || player.getOffhandItem().is(ModItems.ACOUSTIC_GUITAR.get());

        while (NEXT_TRACK.consumeClick()) {
            if (holdingGuitar && minecraft.gui.screen() == null) {
                CampfireMusicClient.next();
                showTrackMessage();
            }
        }
        while (PREVIOUS_TRACK.consumeClick()) {
            if (holdingGuitar && minecraft.gui.screen() == null) {
                CampfireMusicClient.previous();
                showTrackMessage();
            }
        }
        while (REPEAT_TRACK.consumeClick()) {
            if (holdingGuitar && minecraft.gui.screen() == null) {
                CampfireMusicClient.toggleRepeatOne();
                player.sendOverlayMessage(Component.literal(
                        CampfireMusicClient.isRepeatOne() ? "♪ Repeat one: ON" : "♪ Auto next: ON"));
            }
        }
    }

    private static void showTrackMessage() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        MusicTrack track = CampfireMusicClient.selectedTrack();
        minecraft.player.sendOverlayMessage(Component.literal(
                "♪ " + track.title() + " · " + track.bpm() + " BPM"));
    }

    private static void openScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(minecraft.gui.screen() instanceof MusicPlayerScreen)) {
            minecraft.gui.setScreen(new MusicPlayerScreen());
        }
    }
}
