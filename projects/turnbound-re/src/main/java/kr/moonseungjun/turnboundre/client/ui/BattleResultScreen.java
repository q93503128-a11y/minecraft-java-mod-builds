package kr.moonseungjun.turnboundre.client.ui;

import kr.moonseungjun.turnboundre.client.BattleResultClientState;
import kr.moonseungjun.turnboundre.network.BattleResultNetworkPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Locale;

/** Compact non-pausing terminal presentation. World cleanup happens only after a server-accepted acknowledgement. */
public final class BattleResultScreen extends Screen {
    private static final Identifier TITLE_BOX = Identifier.withDefaultNamespace("advancements/title_box");
    private static final Identifier FRAME = Identifier.withDefaultNamespace("advancements/task_frame_obtained");
    private static final int TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int TEXT_SECONDARY = 0xFFBBBBBB;
    private static final int TEXT_WARNING = 0xFFFFCC66;
    private static final int TEXT_SUCCESS = 0xFFAAFFAA;

    private BattleResultNetworkPayloads.ResultView result;
    private Button continueButton;
    private boolean acknowledgementSent;
    private long seenGeneration = -1L;
    private String feedback = "";

    public BattleResultScreen() {
        super(Minecraft.getInstance(), Minecraft.getInstance().font,
                Component.translatable("screen.turnbound_re.battle_result"));
    }

    @Override
    protected void init() {
        syncState();
        if (!BattleResultLayout.supports(this.width, this.height)) return;
        BattleResultLayout.Layout layout = BattleResultLayout.calculate(this.width, this.height);
        int buttonWidth = Math.min(110, layout.footer().width());
        continueButton = Button.builder(Component.translatable("screen.turnbound_re.result.continue"), ignored -> acknowledge())
                .bounds(layout.footer().x() + (layout.footer().width() - buttonWidth) / 2,
                        layout.footer().y() + 2, buttonWidth, 20)
                .build();
        continueButton.active = result != null && !acknowledgementSent;
        this.addRenderableWidget(continueButton);
    }

    @Override
    public void tick() {
        super.tick();
        if (BattleResultClientState.generation() != seenGeneration) {
            syncState();
            if (result == null) {
                this.minecraft.gui.setScreen(null);
                return;
            }
            acknowledgementSent = false;
            if (continueButton != null) continueButton.active = true;
        }
    }

    @Override
    public void onClose() {
        acknowledge();
        // Intentionally do not call super: ESC is equivalent to Continue and waits for server cleanup acknowledgement.
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the Minecraft world visible behind the compact result presentation.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!BattleResultLayout.supports(this.width, this.height)) {
            graphics.text(this.font, Component.translatable("screen.turnbound_re.result.canvas_too_small"),
                    Math.max(8, this.width / 2 - 100), Math.max(8, this.height / 2 - 10), TEXT_WARNING, true);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }

        BattleResultLayout.Layout layout = BattleResultLayout.calculate(this.width, this.height);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TITLE_BOX,
                layout.header().x(), layout.header().y(), layout.header().width(), layout.header().height());
        if (result != null) {
            Component outcome = Component.translatable(result.victory()
                    ? "screen.turnbound_re.result.victory" : "screen.turnbound_re.result.defeat");
            graphics.text(this.font, outcome,
                    layout.header().x() + (layout.header().width() - this.font.width(outcome)) / 2,
                    layout.header().y() + 10,
                    result.victory() ? TEXT_SUCCESS : TEXT_WARNING,
                    true);
            renderRewards(graphics, layout.rewards());
        }

        if (acknowledgementSent) {
            feedback = Component.translatable("screen.turnbound_re.result.returning").getString();
        } else if (!BattleResultClientState.closeError().isBlank()) {
            feedback = Component.translatable("screen.turnbound_re.result.server_rejected").getString();
        }
        if (!feedback.isBlank()) {
            graphics.text(this.font, Component.literal(fit(feedback, layout.footer().width())),
                    layout.footer().x(), Math.max(layout.header().bottom(), layout.footer().y() - this.font.lineHeight - 2),
                    acknowledgementSent ? TEXT_SECONDARY : TEXT_WARNING, true);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderRewards(GuiGraphicsExtractor graphics, BattleResultLayout.Rect region) {
        int x = region.x() + 8;
        int y = region.y();
        graphics.text(this.font, Component.translatable("screen.turnbound_re.result.rewards"), x, y, TEXT_PRIMARY, true);
        y += 18;

        if (!result.hasRewards()) {
            graphics.text(this.font, Component.translatable("screen.turnbound_re.result.no_rewards"), x, y, TEXT_SECONDARY, true);
            return;
        }
        if (result.coinDelta() > 0) {
            y = rewardRow(graphics, region, y, Component.translatable(
                    "screen.turnbound_re.result.coin", result.coinDelta(), result.coinTotal()));
        }
        if (result.essenceDelta() > 0) {
            y = rewardRow(graphics, region, y, Component.translatable(
                    "screen.turnbound_re.result.essence", result.essenceDelta(), result.essenceTotal()));
        }
        for (BattleResultNetworkPayloads.ShardView shard : result.shards()) {
            rewardRow(graphics, region, y, Component.translatable(
                    "screen.turnbound_re.result.shard", displayName(shard.characterId()), shard.amount(), shard.total()));
            y += 26;
        }
    }

    private int rewardRow(GuiGraphicsExtractor graphics, BattleResultLayout.Rect region, int y, Component text) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME, region.x() + 8, y, 20, 20);
        graphics.text(this.font, Component.literal(fit(text.getString(), Math.max(1, region.width() - 42))),
                region.x() + 34, y + 6, TEXT_PRIMARY, true);
        return y + 26;
    }

    private void acknowledge() {
        if (acknowledgementSent || result == null) return;
        acknowledgementSent = true;
        feedback = Component.translatable("screen.turnbound_re.result.returning").getString();
        if (continueButton != null) continueButton.active = false;
        ClientPacketDistributor.sendToServer(BattleResultNetworkPayloads.AcknowledgeResultC2S.from(result));
    }

    private void syncState() {
        seenGeneration = BattleResultClientState.generation();
        result = BattleResultClientState.result().orElse(null);
        if (!BattleResultClientState.closeError().isBlank()) {
            feedback = Component.translatable("screen.turnbound_re.result.server_rejected").getString();
        } else if (!acknowledgementSent) {
            feedback = "";
        }
    }

    private static String displayName(String id) {
        if (id == null || id.isBlank()) return "?";
        int colon = id.indexOf(':');
        String raw = colon >= 0 ? id.substring(colon + 1) : id;
        String[] words = raw.replace('-', '_').split("_+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) out.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.isEmpty() ? "?" : out.toString();
    }

    private String fit(String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (this.font.width(text) <= maxWidth) return text;
        String suffix = "...";
        int suffixWidth = this.font.width(suffix);
        if (suffixWidth >= maxWidth) return "";
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end)) + suffixWidth > maxWidth) end--;
        return end <= 0 ? "" : text.substring(0, end) + suffix;
    }
}
