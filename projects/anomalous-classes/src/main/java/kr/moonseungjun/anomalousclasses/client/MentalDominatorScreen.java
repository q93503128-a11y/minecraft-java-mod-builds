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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-backed command list for the Mental Dominator's actual controlled entities. */
public final class MentalDominatorScreen extends Screen {
    private static final int ROW_HEIGHT = 34;
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
        boolean returnToProfile = ClassManagementNavigation.shouldReturnToProfile();
        MentalDominatorScreenLayout layout = MentalDominatorScreenLayout.calculate(width, height, returnToProfile);
        outer = layout.outer();
        listBox = layout.list();
        detailBox = layout.detail();
        int buttonHeight = layout.buttonHeight();
        if (returnToProfile) {
            profileButton = addRenderableWidget(new MenuUi.Button(layout.profile(),
                    Component.translatable("screen.anomalousclasses.mental.back_to_profile"), button -> {
                        ClassManagementNavigation.clearReturnToProfile();
                        request(RequestProfilePayload.INSTANCE);
                    }));
        }
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
        if (width < 360) {
            int titleRoom = Math.max(1, codexButton.getX() - (focusButton.getX() + focusButton.getWidth()) - 8);
            MenuUi.drawCentered(graphics, font, title, width / 2, outer.y() + 8, titleRoom, 0xFFF1E7D7);
        } else {
            MenuUi.drawTitle(graphics, font, title, outer, width, height);
        }
        drawResourceSummary(graphics);
        drawList(graphics);
        drawDetails(graphics);
        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
        if (pending) drawNotice(graphics, "screen.anomalousclasses.mental.pending");
        else if (state.resultCode() == 1) drawNotice(graphics, "screen.anomalousclasses.mental.stale");
        else if (state.resultCode() == 2) drawNotice(graphics, "screen.anomalousclasses.mental.not_current");
        else if (state.resultCode() == 4) drawNotice(graphics, "screen.anomalousclasses.mental.not_owned");
        else if (state.resultCode() == 5) drawNotice(graphics, "screen.anomalousclasses.mental.release_pending");
    }

    private void drawResourceSummary(GuiGraphics graphics) {
        int left = outer.x() + 16;
        int contentWidth = Math.max(1, outer.width() - 32);
        int gap = Math.min(7, Math.max(3, contentWidth / 100));
        int column = Math.max(1, (contentWidth - gap * 2) / 3);
        int y = outer.y() + 30;
        int tileHeight = 24;
        Component[] values = {
                Component.translatable("screen.anomalousclasses.mental.slots",
                        state.controlledCount(), state.maxControlled()),
                Component.translatable("screen.anomalousclasses.mental.psyche",
                        number(state.psyche()), number(state.maximumPsyche())),
                Component.translatable("screen.anomalousclasses.mental.upkeep", number(state.upkeepPerSecond()))
        };
        int[] colors = {0xFFD8D0C5, 0xFFF1E7D7, 0xFFE6C788};
        for (int index = 0; index < values.length; index++) {
            int x = left + index * (column + gap);
            int tileWidth = index == values.length - 1 ? left + contentWidth - x : column;
            MenuUi.Box tile = new MenuUi.Box(x, y, Math.max(1, tileWidth), tileHeight);
            MenuUi.drawInsetSurface(graphics, tile);
            graphics.fill(tile.x() + 5, tile.y() + 2, tile.right() - 5, tile.y() + 3,
                    index == 1 ? MenuUi.accent(ClassId.MENTAL_DOMINATOR) : 0x907C6544);
            MenuUi.drawCentered(graphics, font, values[index], tile.x() + tile.width() / 2,
                    tile.y() + 6, Math.max(1, tile.width() - 10), colors[index]);
            if (index == 1) {
                MenuUi.drawRankProgress(graphics, tile.x() + 7, tile.bottom() - 4,
                        Math.max(1, tile.width() - 14), 2,
                        state.maximumPsyche() <= 0.0 ? 0.0 : state.psyche() / state.maximumPsyche());
            }
        }
    }

    private void drawList(GuiGraphics graphics) {
        MenuUi.drawInsetSurface(graphics, listBox);
        graphics.fill(listBox.x() + 7, listBox.y() + 4, listBox.right() - 7, listBox.y() + 6,
                MenuUi.accent(ClassId.MENTAL_DOMINATOR));
        int pad = Math.max(7, listBox.width() / 18);
        int contentX = listBox.x() + pad;
        int contentWidth = Math.max(1, listBox.width() - pad * 2);
        Component heading = Component.translatable(codexView
                ? "screen.anomalousclasses.mental.codex_title" : "screen.anomalousclasses.mental.list_title");
        int count = codexView ? state.codex().size() : state.mobs().size();
        int countWidth = Math.min(24, Math.max(16, contentWidth / 5));
        MenuUi.drawFit(graphics, font, heading, contentX, listBox.y() + 8,
                Math.max(1, contentWidth - countWidth - 5), 0xFFE6C788);
        MenuUi.drawFitRight(graphics, font, Component.literal(Integer.toString(count)),
                listBox.right() - pad, listBox.y() + 8, countWidth, 0xFFC8B9D2);
        int firstY = listBox.y() + 11 + font.lineHeight + 3;
        int bottom = listBox.bottom() - 9;
        int visible = visibleRows(listBox);
        List<String> labels = codexView ? state.codex() : state.mobs().stream().map(MentalDominatorStatePayload.MobView::id).toList();
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, labels.size() - visible)));
        graphics.enableScissor(listBox.x() + 2, firstY, listBox.right() - 2, bottom);
        for (int row = 0; row < visible; row++) {
            int index = scrollOffset + row;
            if (index >= labels.size()) break;
            int rowY = firstY + row * ROW_HEIGHT;
            if (codexView) {
                Component species = entityTypeComponent(labels.get(index));
                drawCreatureIcon(graphics, labels.get(index), contentX, rowY + 4, selectedEntityId.equals(labels.get(index)));
                MenuUi.drawFit(graphics, font, species, contentX + 24, rowY + 7,
                        Math.max(1, contentWidth - 24), 0xFFD8D0C5);
                continue;
            }
            MentalDominatorStatePayload.MobView mob = findMob(labels.get(index));
            if (mob == null) continue;
            boolean selected = mob.id().equals(selectedEntityId);
            if (selected) {
                graphics.fill(listBox.x() + 4, rowY, listBox.right() - 4, rowY + ROW_HEIGHT - 1, 0x7459305F);
                graphics.fill(listBox.x() + 4, rowY + 2, listBox.x() + 6, rowY + ROW_HEIGHT - 3,
                        MenuUi.accent(ClassId.MENTAL_DOMINATOR));
            }
            drawCreatureIcon(graphics, mob.type(), contentX, rowY + 5, selected);
            Component line = displayName(mob);
            int labelX = contentX + 24;
            int labelWidth = Math.max(1, contentWidth - 24);
            MenuUi.drawFit(graphics, font, line, labelX, rowY + 3, labelWidth, selected ? 0xFFFFE8C5 : 0xFFD8D0C5);
            Component status = Component.translatable(statusKey(mob.status()));
            int statusColor = mob.status() == 0 ? 0xFF9CC68A : 0xFFC2B4A0;
            MenuUi.drawFit(graphics, font, status, labelX, rowY + 4 + font.lineHeight, labelWidth, statusColor);
            if (mob.hasLiveHealth()) {
                MenuUi.drawRankProgress(graphics, labelX, rowY + ROW_HEIGHT - 4, labelWidth, 2,
                        mob.maxHealth() <= 0.0F ? 0.0 : mob.health() / mob.maxHealth());
            }
        }
        graphics.disableScissor();
    }

    private void drawDetails(GuiGraphics graphics) {
        MenuUi.drawInsetSurface(graphics, detailBox);
        graphics.fill(detailBox.x() + 7, detailBox.y() + 4, detailBox.right() - 7, detailBox.y() + 6,
                MenuUi.accent(ClassId.MENTAL_DOMINATOR));
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
        int y = detailBox.y() + 10;
        drawCreatureIcon(graphics, mob.type(), x, y, true);
        int nameX = x + 25;
        int nameWidth = Math.max(1, textWidth - 25);
        MenuUi.drawFit(graphics, font, displayName(mob), nameX, y + 1, nameWidth, 0xFFF1E7D7);
        int metaY = y + 2 + font.lineHeight;
        Component typeName = entityTypeComponent(mob.type());
        Component statusName = Component.translatable(statusKey(mob.status()));
        int statusWidth = Math.min(nameWidth / 2, font.width(statusName));
        MenuUi.drawFit(graphics, font, typeName, nameX, metaY,
                Math.max(1, nameWidth - statusWidth - 6), 0xFFC8B9D2);
        MenuUi.drawFitRight(graphics, font, statusName, nameX + nameWidth, metaY,
                statusWidth, mob.status() == 0 ? 0xFF9CC68A : 0xFFC2B4A0);
        y += Math.max(25, font.lineHeight * 2 + 6);
        graphics.fill(x, y, detailBox.right() - pad, y + 1, 0x806D4D7D);
        y += 6;
        Component mode = Component.translatable(mob.command() == 0
                ? "screen.anomalousclasses.mental.mode_follow" : "screen.anomalousclasses.mental.mode_stay");
        if (mob.hasLiveHealth()) {
            MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.mental.health",
                    number(mob.health()), number(mob.maxHealth())), x, y, textWidth, 0xFFD8D0C5);
            y += font.lineHeight + 1;
            MenuUi.drawRankProgress(graphics, x, y, textWidth, 3,
                    mob.maxHealth() <= 0.0F ? 0.0 : mob.health() / mob.maxHealth());
            y += 7;
        } else {
            MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.mental.health_unavailable"),
                    x, y, textWidth, 0xFFB8AFA5);
            y += font.lineHeight + 3;
        }
        MenuUi.drawFit(graphics, font, Component.translatable("screen.anomalousclasses.mental.mode", mode),
                x, y, textWidth, 0xFFD8D0C5);
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
        int maxWidth = Math.max(80, outer.width() - 28);
        MenuUi.drawCentered(graphics, font, message, width / 2, outer.y() + 46, maxWidth, 0xFFFFD69C);
    }

    private void drawText(GuiGraphics graphics, String key, int count, int y, int color) {
        MenuUi.drawFit(graphics, font, Component.translatable(key, count), detailBox.x() + 10, y,
                Math.max(1, detailBox.width() - 20), color);
    }

    private int visibleRows(MenuUi.Box box) {
        int firstY = box.y() + 11 + font.lineHeight + 3;
        int bottom = box.bottom() - 9;
        return Math.max(1, (bottom - firstY) / ROW_HEIGHT);
    }

    private void drawCreatureIcon(GuiGraphics graphics, String type, int x, int y, boolean selected) {
        ItemStack icon = creatureIcon(type);
        if (icon.isEmpty()) return;
        int size = 20;
        graphics.fill(x, y, x + size, y + size, selected ? 0xA8402C52 : 0x8E241D2A);
        graphics.fill(x, y, x + size, y + 1, selected ? 0xD6C69AE9 : 0x806E5A70);
        graphics.fill(x, y, x + 1, y + size, selected ? 0xD6C69AE9 : 0x806E5A70);
        graphics.renderItem(icon, x + 2, y + 2);
    }

    private static ItemStack creatureIcon(String type) {
        ResourceLocation entity = ResourceLocation.tryParse(type);
        if (entity == null || !"minecraft".equals(entity.getNamespace())) return ItemStack.EMPTY;
        Item icon = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                entity.getNamespace(), entity.getPath() + "_spawn_egg"));
        return icon == Items.AIR ? ItemStack.EMPTY : new ItemStack(icon);
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
            int firstY = listBox.y() + 11 + font.lineHeight + 3;
            int visible = visibleRows(listBox);
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
            int visible = visibleRows(listBox);
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
