package kr.moonseungjun.anomalousclasses.client;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import kr.moonseungjun.anomalousclasses.network.payload.MentalDominatorActionPayload;
import kr.moonseungjun.anomalousclasses.network.payload.MentalDominatorStatePayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestClassMenuPayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestClassFocusPayload;
import kr.moonseungjun.anomalousclasses.network.payload.RequestProfilePayload;
import kr.moonseungjun.anomalousclasses.state.ClassId;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-backed command list for the Mental Dominator's actual controlled entities. */
public final class MentalDominatorScreen extends Screen {
    private static final int ROW_HEIGHT = 25;
    private MentalDominatorStatePayload state;
    private MenuUi.Box outer, listBox, detailBox;
    private MenuUi.Button profileButton, focusButton, classButton, codexButton;
    private MenuUi.Button allFollowButton, allStayButton, followButton, stayButton, damageButton, releaseButton;
    private String selectedEntityId = "";
    private int scrollOffset;
    private boolean codexView;
    private boolean releaseConfirm;
    private boolean pending;

    public MentalDominatorScreen(MentalDominatorStatePayload state) {
        super(Component.translatable("screen.anomalousclasses.mental.title"));
        this.state = state;
    }

    @Override
    protected void init() {
        if (selectedEntityId.isBlank() && !state.mobs().isEmpty()) selectedEntityId = state.mobs().get(0).id();
        MentalDominatorScreenLayout layout = MentalDominatorScreenLayout.calculate(width, height);
        outer = layout.outer();
        listBox = layout.list();
        detailBox = layout.detail();
        int buttonHeight = layout.buttonHeight();
        profileButton = addRenderableWidget(new MenuUi.Button(layout.profile(),
                Component.translatable("screen.anomalousclasses.mental.profile"), button -> request(RequestProfilePayload.INSTANCE)));
        focusButton = addRenderableWidget(new MenuUi.Button(layout.focus(), Component.translatable("screen.anomalousclasses.mental.focus"),
                button -> request(RequestClassFocusPayload.INSTANCE)));
        classButton = addRenderableWidget(new MenuUi.Button(layout.changeClass(),
                Component.translatable("screen.anomalousclasses.mental.change_class"),
                button -> request(RequestClassMenuPayload.INSTANCE)));
        codexButton = addRenderableWidget(new MenuUi.Button(layout.codex(), Component.translatable("screen.anomalousclasses.mental.codex"),
                button -> { codexView = !codexView; scrollOffset = 0; refreshButtons(); }));

        allFollowButton = addRenderableWidget(new MenuUi.Button(layout.allFollow(),
                Component.translatable("screen.anomalousclasses.mental.all_follow"),
                button -> action(MentalDominatorActionPayload.ALL_FOLLOW, "")));
        allStayButton = addRenderableWidget(new MenuUi.Button(layout.allStay(), Component.translatable("screen.anomalousclasses.mental.all_stay"),
                button -> action(MentalDominatorActionPayload.ALL_STAY, "")));

        followButton = addRenderableWidget(new MenuUi.Button(layout.follow(), Component.translatable("screen.anomalousclasses.mental.follow"),
                button -> action(MentalDominatorActionPayload.FOLLOW, selectedEntityId)));
        stayButton = addRenderableWidget(new MenuUi.Button(layout.stay(), Component.translatable("screen.anomalousclasses.mental.stay"),
                button -> action(MentalDominatorActionPayload.STAY, selectedEntityId)));
        damageButton = addRenderableWidget(new MenuUi.Button(layout.damage(),
                Component.translatable("screen.anomalousclasses.mental.allow_damage"),
                button -> action(MentalDominatorActionPayload.TOGGLE_PLAYER_DAMAGE, selectedEntityId)));
        releaseButton = addRenderableWidget(new MenuUi.Button(layout.release(),
                Component.translatable("screen.anomalousclasses.mental.release"),
                button -> {
                    if (!releaseConfirm) { releaseConfirm = true; refreshButtons(); }
                    else action(MentalDominatorActionPayload.RELEASE, selectedEntityId);
                }));
        refreshButtons();
    }

    public void applyServerState(MentalDominatorStatePayload updated) {
        boolean keepReleaseConfirm = state != null && updated.resultCode() == 0
                && state.revision() == updated.revision() && releaseConfirm;
        state = updated;
        pending = false;
        if (!codexView && state.mobs().stream().noneMatch(mob -> mob.id().equals(selectedEntityId))) {
            selectedEntityId = state.mobs().isEmpty() ? "" : state.mobs().get(0).id();
        }
        releaseConfirm = keepReleaseConfirm;
        refreshButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x5C17111F);
        MenuUi.drawPanel(graphics, outer);
        MenuUi.drawTitle(graphics, font, title, outer, width, height);
        drawResourceSummary(graphics);
        drawList(graphics);
        drawDetails(graphics);
        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
        if (state.resultCode() == 1) drawNotice(graphics, "screen.anomalousclasses.mental.stale");
        else if (state.resultCode() == 2) drawNotice(graphics, "screen.anomalousclasses.mental.not_current");
        else if (state.resultCode() == 4) drawNotice(graphics, "screen.anomalousclasses.mental.not_owned");
        else if (state.resultCode() == 5) drawNotice(graphics, "screen.anomalousclasses.mental.release_pending");
    }

    private void drawResourceSummary(GuiGraphics graphics) {
        int y = outer.y() + 35;
        Component resource = Component.translatable("screen.anomalousclasses.mental.psyche",
                number(state.psyche()), number(state.maximumPsyche()));
        MenuUi.drawCentered(graphics, font, resource, width / 2, y, Math.max(1, outer.width() / 2), 0xFFF1E7D7);
        Component upkeep = Component.translatable("screen.anomalousclasses.mental.upkeep", number(state.upkeepPerSecond()));
        MenuUi.drawFitRight(graphics, font, upkeep, outer.right() - 20, y, Math.max(65, outer.width() / 4), 0xFFE6C788);
        Component slots = Component.translatable("screen.anomalousclasses.mental.slots",
                state.controlledCount(), state.maxControlled());
        MenuUi.drawFit(graphics, font, slots, outer.x() + 20, y, Math.max(70, outer.width() / 4), 0xFFD8D0C5);
    }

    private void drawList(GuiGraphics graphics) {
        MenuUi.drawPanel(graphics, listBox);
        int pad = Math.max(7, listBox.width() / 18);
        int contentX = listBox.x() + pad;
        int contentWidth = Math.max(1, listBox.width() - pad * 2);
        Component heading = Component.translatable(codexView
                ? "screen.anomalousclasses.mental.codex_title" : "screen.anomalousclasses.mental.list_title");
        MenuUi.drawFit(graphics, font, heading, contentX, listBox.y() + 8, contentWidth, 0xFFE6C788);
        int firstY = listBox.y() + 11 + font.lineHeight + 3;
        int bottom = listBox.bottom() - 9;
        int visible = Math.max(1, (bottom - firstY) / ROW_HEIGHT);
        List<String> labels = codexView ? state.codex() : state.mobs().stream().map(MentalDominatorStatePayload.MobView::id).toList();
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, labels.size() - visible)));
        graphics.enableScissor(listBox.x() + 2, firstY, listBox.right() - 2, bottom);
        for (int row = 0; row < visible; row++) {
            int index = scrollOffset + row;
            if (index >= labels.size()) break;
            int rowY = firstY + row * ROW_HEIGHT;
            if (codexView) {
                Component species = entityTypeComponent(labels.get(index));
                MenuUi.drawFit(graphics, font, species, contentX, rowY + 3, contentWidth, 0xFFD8D0C5);
                continue;
            }
            MentalDominatorStatePayload.MobView mob = findMob(labels.get(index));
            if (mob == null) continue;
            boolean selected = mob.id().equals(selectedEntityId);
            if (selected) graphics.fill(listBox.x() + 4, rowY, listBox.right() - 4, rowY + ROW_HEIGHT - 1, 0x7A59305F);
            Component line = displayName(mob);
            MenuUi.drawFit(graphics, font, line, contentX + 2, rowY + 2, contentWidth - 4, selected ? 0xFFFFE8C5 : 0xFFD8D0C5);
            Component status = Component.translatable(statusKey(mob.status()));
            int statusColor = mob.status() == 0 ? 0xFF9CC68A : 0xFFC2B4A0;
            MenuUi.drawFit(graphics, font, status, contentX + 2, rowY + 2 + font.lineHeight,
                    contentWidth - 4, statusColor);
        }
        graphics.disableScissor();
    }

    private void drawDetails(GuiGraphics graphics) {
        MenuUi.drawPanel(graphics, detailBox);
        if (codexView) {
            drawText(graphics, "screen.anomalousclasses.mental.codex_count", state.codex().size(),
                    detailBox.y() + 12, 0xFFD8D0C5);
            return;
        }
        MentalDominatorStatePayload.MobView mob = findMob(selectedEntityId);
        if (mob == null) {
            Component empty = Component.translatable("screen.anomalousclasses.mental.empty");
            MenuUi.drawCentered(graphics, font, empty, detailBox.x() + detailBox.width() / 2,
                    detailBox.y() + detailBox.height() / 2 - font.lineHeight / 2, detailBox.width() - 12, 0xFFD8D0C5);
            return;
        }
        int pad = Math.max(8, detailBox.width() / 18);
        int x = detailBox.x() + pad;
        int textWidth = Math.max(1, detailBox.width() - pad * 2);
        int y = detailBox.y() + 12;
        MenuUi.drawFit(graphics, font, displayName(mob), x, y, textWidth, 0xFFF1E7D7);
        y += font.lineHeight + 4;
        if (!mob.name().isBlank()) {
            MenuUi.drawFit(graphics, font, entityTypeComponent(mob.type()), x, y, textWidth, 0xFFC8B9D2);
            y += font.lineHeight + 5;
        }
        Component mode = Component.translatable(mob.command() == 0
                ? "screen.anomalousclasses.mental.mode_follow" : "screen.anomalousclasses.mental.mode_stay");
        MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.mental.mode", mode),
                x, y, textWidth, 0xFFD8D0C5);
        y += font.lineHeight + 3;
        if (mob.hasLiveHealth()) {
            MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.mental.health",
                    number(mob.health()), number(mob.maxHealth())), x, y, textWidth, 0xFFD8D0C5);
        } else {
            MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.mental.health_unavailable"),
                    x, y, textWidth, 0xFFB8AFA5);
        }
        y += font.lineHeight + 3;
        MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.mental.entity_upkeep",
                number(mob.upkeepPerSecond())), x, y, textWidth, 0xFFE6C788);
        y += font.lineHeight + 3;
        MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.mental.location",
                dimensionComponent(mob.dimension()), mob.x(), mob.y(), mob.z()), x, y, textWidth, 0xFFB8AFA5);
        y += font.lineHeight + 3;
        MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.mental.allow_damage_state",
                Component.translatable(mob.allowPlayerDamage() ? "options.on" : "options.off")),
                x, y, textWidth, 0xFFD8D0C5);
        if (releaseConfirm) {
            Component confirm = Component.translatable("screen.anomalousclasses.mental.release_confirm", displayName(mob));
            MenuUi.drawFit(graphics, font, confirm, x, damageButton.getY() - font.lineHeight - 3,
                    textWidth, 0xFFE6B18C);
        }
    }

    private void drawNotice(GuiGraphics graphics, String key) {
        Component message = Component.translatable(key);
        int maxWidth = Math.max(80, outer.width() / 3);
        MenuUi.drawCentered(graphics, font, message, width / 2, outer.bottom() - 18, maxWidth, 0xFFFFD69C);
    }

    private void drawText(GuiGraphics graphics, String key, int count, int y, int color) {
        MenuUi.drawFit(graphics, font, Component.translatable(key, count), detailBox.x() + 10, y,
                Math.max(1, detailBox.width() - 20), color);
    }

    private String statusKey(int status) {
        return switch (status) {
            case 0 -> "screen.anomalousclasses.mental.status_loaded";
            case 1 -> "screen.anomalousclasses.mental.status_unloaded";
            default -> "screen.anomalousclasses.mental.status_missing";
        };
    }

    private Component displayName(MentalDominatorStatePayload.MobView mob) {
        return mob.name().isBlank() ? entityTypeComponent(mob.type()) : Component.literal(mob.name());
    }

    private Component entityTypeComponent(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !"minecraft".equals(location.getNamespace())) {
            return Component.translatable("screen.anomalousclasses.mental.unknown_creature");
        }
        return Component.translatable("entity." + location.getNamespace() + "."
                + location.getPath().replace('/', '.'));
    }

    private Component dimensionComponent(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null || !"minecraft".equals(location.getNamespace())) {
            return Component.translatable("screen.anomalousclasses.mental.unknown_location");
        }
        return switch (location.getPath()) {
            case "overworld" -> Component.translatable("screen.anomalousclasses.mental.dimension_overworld");
            case "the_nether" -> Component.translatable("screen.anomalousclasses.mental.dimension_nether");
            case "the_end" -> Component.translatable("screen.anomalousclasses.mental.dimension_end");
            default -> Component.translatable("screen.anomalousclasses.mental.unknown_location");
        };
    }

    private MentalDominatorStatePayload.MobView findMob(String id) {
        if (id == null || id.isBlank()) return null;
        return state.mobs().stream().filter(mob -> mob.id().equals(id)).findFirst().orElse(null);
    }

    private void action(int action, String entityId) {
        boolean global = action == MentalDominatorActionPayload.ALL_FOLLOW || action == MentalDominatorActionPayload.ALL_STAY;
        if (pending || state == null || entityId == null || (!global && entityId.isBlank())) return;
        pending = true;
        releaseConfirm = false;
        refreshButtons();
        PacketDistributor.sendToServer(new MentalDominatorActionPayload(action, entityId, state.revision()));
    }

    private void request(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (pending) return;
        pending = true;
        refreshButtons();
        PacketDistributor.sendToServer(payload);
    }

    private void refreshButtons() {
        boolean hasSelection = !codexView && findMob(selectedEntityId) != null;
        if (profileButton != null) profileButton.active = !pending;
        if (focusButton != null) focusButton.active = !pending;
        if (classButton != null) classButton.active = !pending;
        if (codexButton != null) codexButton.setMessage(Component.translatable(codexView
                ? "screen.anomalousclasses.mental.return_list" : "screen.anomalousclasses.mental.codex"));
        if (codexButton != null) codexButton.active = !pending;
        if (allFollowButton != null) allFollowButton.active = !pending && state.controlledCount() > 0;
        if (allStayButton != null) allStayButton.active = !pending && state.controlledCount() > 0;
        if (followButton != null) followButton.active = !pending && hasSelection;
        if (stayButton != null) stayButton.active = !pending && hasSelection;
        if (damageButton != null) {
            MentalDominatorStatePayload.MobView mob = findMob(selectedEntityId);
            damageButton.active = !pending && hasSelection;
            damageButton.setMessage(Component.translatable("screen.anomalousclasses.mental.allow_damage",
                    Component.translatable(mob != null && mob.allowPlayerDamage() ? "options.off" : "options.on")));
        }
        if (releaseButton != null) {
            releaseButton.active = !pending && hasSelection;
            releaseButton.setMessage(Component.translatable(releaseConfirm
                    ? "screen.anomalousclasses.mental.release_confirm_button" : "screen.anomalousclasses.mental.release"));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !codexView && listBox != null && listBox.contains((int) mouseX, (int) mouseY)) {
            int pad = Math.max(7, listBox.width() / 18);
            int firstY = listBox.y() + 11 + font.lineHeight + 3;
            int bottom = listBox.bottom() - 9;
            int visible = Math.max(1, (bottom - firstY) / ROW_HEIGHT);
            int row = ((int) mouseY - firstY) / ROW_HEIGHT;
            if (row >= 0 && row < visible) {
                int index = scrollOffset + row;
                if (index >= 0 && index < state.mobs().size()) {
                    selectedEntityId = state.mobs().get(index).id();
                    releaseConfirm = false;
                    refreshButtons();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (listBox != null && listBox.contains((int) mouseX, (int) mouseY)) {
            int listSize = codexView ? state.codex().size() : state.mobs().size();
            int visible = Math.max(1, (listBox.height() - font.lineHeight - 28) / ROW_HEIGHT);
            scrollOffset = Math.max(0, Math.min(Math.max(0, listSize - visible),
                    scrollOffset - (int) Math.signum(verticalAmount) * 3));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private static String number(double value) {
        return new DecimalFormat("0.##", DecimalFormatSymbolsHolder.SYMBOLS).format(Math.max(0.0, value));
    }

    private static final class DecimalFormatSymbolsHolder {
        private static final java.text.DecimalFormatSymbols SYMBOLS = java.text.DecimalFormatSymbols.getInstance(Locale.ROOT);
    }

    @Override public void onClose() { PacketDistributor.sendToServer(new kr.moonseungjun.anomalousclasses.network.payload.MentalIntrusionPayload(
            kr.moonseungjun.anomalousclasses.network.payload.MentalIntrusionPayload.STOP)); super.onClose(); }
    @Override public boolean isPauseScreen() { return false; }
}
