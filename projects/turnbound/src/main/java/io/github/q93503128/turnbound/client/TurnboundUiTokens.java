package io.github.q93503128.turnbound.client;

/**
 * TURNBOUND visual design tokens.
 *
 * <p>Keep new screens on this small palette/spacing scale instead of inventing per-screen
 * colors and arbitrary gaps. Screen-specific semantic colors are fine when they describe
 * actual game data, but generic chrome should come from here.</p>
 */
final class TurnboundUiTokens {
    static final int BACKGROUND = 0xF00A0D12;
    static final int SURFACE = 0xEC151A22;
    static final int ELEVATED_SURFACE = 0xF20F141B;
    static final int INSET = 0xE810141B;

    static final int PRIMARY = 0xFF6DC6FF;
    static final int ACCENT = 0xFFFFC857;
    static final int SUCCESS = 0xFF62D39A;
    static final int WARNING = 0xFFFFC857;
    static final int DANGER = 0xFFFF6B6B;
    static final int DISABLED = 0xFF707987;

    static final int TEXT_PRIMARY = 0xFFF4F0E6;
    static final int TEXT_SECONDARY = 0xFFB7B2AA;
    static final int TEXT_MUTED = 0xFF7B8088;
    static final int BORDER = 0xFF8B694A;
    static final int MAP_ROAD = 0xFFD8C79D;

    static final int XS = 4;
    static final int S = 8;
    static final int M = 12;
    static final int L = 16;
    static final int XL = 24;

    private TurnboundUiTokens() {}
}
