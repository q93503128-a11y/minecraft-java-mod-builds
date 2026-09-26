package kr.moonseungjun.campfiresessions.client;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;

public record MusicTrack(
        String id,
        String title,
        String artist,
        String subtitle,
        MusicTheme theme,
        Supplier<SoundEvent> sound
) {}
