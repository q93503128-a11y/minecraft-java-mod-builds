package kr.moonseungjun.campfiresessions.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

public final class GuitarArmPoseParams {
    public static final EnumProxy<HumanoidModel.ArmPose> GUITAR_POSE = new EnumProxy<>(
            HumanoidModel.ArmPose.class,
            true,
            true,
            (IArmPoseTransformer) (model, state, guitarArm) -> applyGuitarPose(model, guitarArm)
    );

    private GuitarArmPoseParams() {}

    private static void applyGuitarPose(HumanoidModel<?> model, HumanoidArm guitarArm) {
        float bpm = Math.max(60.0F, CampfireMusicClient.playingBpm());
        double beat = CampfireMusicClient.elapsedSecondsExact() * bpm / 60.0;
        float strum = (float) Math.sin(beat * Math.PI * 2.0) * 0.075F;

        var strumArm = guitarArm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        var neckArm = guitarArm == HumanoidArm.RIGHT ? model.leftArm : model.rightArm;
        float side = guitarArm == HumanoidArm.RIGHT ? 1.0F : -1.0F;

        // Guitar body stays in front of the torso. The holding arm makes a small beat-driven strum,
        // while the opposite hand reaches across toward the neck.
        strumArm.xRot = -1.02F + strum;
        strumArm.yRot = -0.18F * side;
        strumArm.zRot = 0.20F * side;

        neckArm.xRot = -1.22F;
        neckArm.yRot = 0.58F * side;
        neckArm.zRot = -0.43F * side;
    }
}
