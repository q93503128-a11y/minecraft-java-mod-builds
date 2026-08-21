package kr.moonseungjun.survivalascension.progress;

import java.util.Objects;
import java.util.function.ToIntFunction;

public final class SkillClientBridge {
    private static volatile ToIntFunction<SkillType> clientLevel = skill -> 0;
    private SkillClientBridge() {}
    public static void install(ToIntFunction<SkillType> reader) { clientLevel = Objects.requireNonNull(reader); }
    public static int level(SkillType skill) { return clientLevel.applyAsInt(skill); }
}
