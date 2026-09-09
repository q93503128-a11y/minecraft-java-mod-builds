package kr.moonseungjun.riftfrontier.combat.presentation.mesh;

import java.util.Arrays;

/** Samples glTF animation channels into joint skin matrices without owning combat timing. */
public final class JointPoseSampler {
    private JointPoseSampler() {
    }

    public static Affine3x4[] sampleSkinMatrices(JointRig rig, AnimationClip clip, float sampleSeconds) {
        if (rig == null || clip == null || !Float.isFinite(sampleSeconds) || sampleSeconds < 0.0f) {
            throw new IllegalArgumentException("rig, clip and non-negative finite sample time are required");
        }
        RestTrs[] pose = new RestTrs[rig.jointCount()];
        for (int joint = 0; joint < pose.length; joint++) {
            pose[joint] = RestTrs.fromAffine(rig.restLocalTransform(joint));
        }

        float time = Math.min(sampleSeconds, clip.durationSeconds());
        boolean[][] driven = new boolean[rig.jointCount()][AnimationClip.Path.values().length];
        for (AnimationClip.Channel channel : clip.channels()) {
            if (channel.joint() >= rig.jointCount()) {
                throw new IllegalArgumentException("animation channel references joint outside rig");
            }
            int pathIndex = channel.path().ordinal();
            if (driven[channel.joint()][pathIndex]) {
                throw new IllegalArgumentException("clip contains duplicate channel for one joint/path");
            }
            driven[channel.joint()][pathIndex] = true;
            float[] value = sample(channel, time);
            RestTrs current = pose[channel.joint()];
            pose[channel.joint()] = switch (channel.path()) {
                case TRANSLATION -> current.withTranslation(value[0], value[1], value[2]);
                case SCALE -> current.withScale(value[0], value[1], value[2]);
                case ROTATION -> current.withRotation(value[0], value[1], value[2], value[3]);
            };
        }

        Affine3x4[] globals = new Affine3x4[rig.jointCount()];
        Affine3x4[] skins = new Affine3x4[rig.jointCount()];
        boolean[] visiting = new boolean[rig.jointCount()];
        boolean[] resolved = new boolean[rig.jointCount()];
        for (int joint = 0; joint < rig.jointCount(); joint++) {
            resolveGlobal(joint, rig, pose, globals, visiting, resolved);
            skins[joint] = multiply(globals[joint], rig.inverseBindMatrix(joint));
        }
        return skins;
    }

    private static void resolveGlobal(int joint, JointRig rig, RestTrs[] pose, Affine3x4[] globals,
                                      boolean[] visiting, boolean[] resolved) {
        if (resolved[joint]) {
            return;
        }
        if (visiting[joint]) {
            throw new IllegalArgumentException("joint hierarchy cycle while sampling pose");
        }
        visiting[joint] = true;
        Affine3x4 local = pose[joint].toAffine();
        int parent = rig.parentJoint(joint);
        if (parent >= 0) {
            resolveGlobal(parent, rig, pose, globals, visiting, resolved);
            globals[joint] = multiply(globals[parent], local);
        } else {
            globals[joint] = local;
        }
        visiting[joint] = false;
        resolved[joint] = true;
    }

    static float[] sample(AnimationClip.Channel channel, float time) {
        int keys = channel.keyCount();
        int components = channel.path().components();
        if (keys == 1 || time <= channel.keyTime(0)) {
            return keyValue(channel, 0);
        }
        if (time >= channel.keyTime(keys - 1)) {
            return keyValue(channel, keys - 1);
        }
        int left = 0;
        while (left + 1 < keys && channel.keyTime(left + 1) <= time) {
            left++;
        }
        int right = left + 1;
        float t0 = channel.keyTime(left);
        float t1 = channel.keyTime(right);
        float alpha = (time - t0) / (t1 - t0);
        if (channel.interpolation() == AnimationClip.Interpolation.STEP) {
            return keyValue(channel, left);
        }
        if (channel.interpolation() == AnimationClip.Interpolation.CUBICSPLINE) {
            return cubic(channel, left, right, alpha, t1 - t0);
        }
        float[] a = keyValue(channel, left);
        float[] b = keyValue(channel, right);
        if (channel.path() == AnimationClip.Path.ROTATION) {
            return slerp(a, b, alpha);
        }
        float[] out = new float[components];
        for (int i = 0; i < components; i++) {
            out[i] = a[i] + (b[i] - a[i]) * alpha;
        }
        return out;
    }

    private static float[] cubic(AnimationClip.Channel channel, int left, int right, float t, float dt) {
        int c = channel.path().components();
        float t2 = t * t;
        float t3 = t2 * t;
        float h00 = 2 * t3 - 3 * t2 + 1;
        float h10 = t3 - 2 * t2 + t;
        float h01 = -2 * t3 + 3 * t2;
        float h11 = t3 - t2;
        float[] out = new float[c];
        int leftBase = left * c * 3;
        int rightBase = right * c * 3;
        for (int i = 0; i < c; i++) {
            float p0 = channel.value(leftBase + c + i);
            float m0 = channel.value(leftBase + 2 * c + i) * dt;
            float p1 = channel.value(rightBase + c + i);
            float m1 = channel.value(rightBase + i) * dt;
            out[i] = h00 * p0 + h10 * m0 + h01 * p1 + h11 * m1;
        }
        return channel.path() == AnimationClip.Path.ROTATION ? normalizeQuaternion(out) : out;
    }

    private static float[] keyValue(AnimationClip.Channel channel, int key) {
        int c = channel.path().components();
        int base = channel.interpolation() == AnimationClip.Interpolation.CUBICSPLINE
                ? key * c * 3 + c : key * c;
        float[] value = new float[c];
        for (int i = 0; i < c; i++) {
            value[i] = channel.value(base + i);
        }
        return channel.path() == AnimationClip.Path.ROTATION ? normalizeQuaternion(value) : value;
    }

    private static float[] slerp(float[] qa, float[] qb, float t) {
        float[] a = normalizeQuaternion(qa);
        float[] b = normalizeQuaternion(qb);
        float dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
        if (dot < 0.0f) {
            dot = -dot;
            for (int i = 0; i < 4; i++) {
                b[i] = -b[i];
            }
        }
        if (dot > 0.9995f) {
            float[] out = new float[4];
            for (int i = 0; i < 4; i++) {
                out[i] = a[i] + (b[i] - a[i]) * t;
            }
            return normalizeQuaternion(out);
        }
        double theta0 = Math.acos(Math.max(-1.0f, Math.min(1.0f, dot)));
        double sinTheta0 = Math.sin(theta0);
        double theta = theta0 * t;
        float s0 = (float) (Math.sin(theta0 - theta) / sinTheta0);
        float s1 = (float) (Math.sin(theta) / sinTheta0);
        return normalizeQuaternion(new float[]{
                s0 * a[0] + s1 * b[0], s0 * a[1] + s1 * b[1],
                s0 * a[2] + s1 * b[2], s0 * a[3] + s1 * b[3]
        });
    }

    private static float[] normalizeQuaternion(float[] q) {
        double lengthSquared = 0.0;
        for (float value : q) {
            lengthSquared += value * value;
        }
        if (!(lengthSquared > 1.0e-16) || !Double.isFinite(lengthSquared)) {
            throw new IllegalArgumentException("animation rotation quaternion is degenerate");
        }
        float inv = (float) (1.0 / Math.sqrt(lengthSquared));
        float[] out = q.clone();
        for (int i = 0; i < out.length; i++) {
            out[i] *= inv;
        }
        return out;
    }

    static Affine3x4 multiply(Affine3x4 a, Affine3x4 b) {
        return new Affine3x4(
                a.m00()*b.m00()+a.m01()*b.m10()+a.m02()*b.m20(),
                a.m00()*b.m01()+a.m01()*b.m11()+a.m02()*b.m21(),
                a.m00()*b.m02()+a.m01()*b.m12()+a.m02()*b.m22(),
                a.m00()*b.m03()+a.m01()*b.m13()+a.m02()*b.m23()+a.m03(),
                a.m10()*b.m00()+a.m11()*b.m10()+a.m12()*b.m20(),
                a.m10()*b.m01()+a.m11()*b.m11()+a.m12()*b.m21(),
                a.m10()*b.m02()+a.m11()*b.m12()+a.m12()*b.m22(),
                a.m10()*b.m03()+a.m11()*b.m13()+a.m12()*b.m23()+a.m13(),
                a.m20()*b.m00()+a.m21()*b.m10()+a.m22()*b.m20(),
                a.m20()*b.m01()+a.m21()*b.m11()+a.m22()*b.m21(),
                a.m20()*b.m02()+a.m21()*b.m12()+a.m22()*b.m22(),
                a.m20()*b.m03()+a.m21()*b.m13()+a.m22()*b.m23()+a.m23()
        );
    }

    private record RestTrs(float tx, float ty, float tz, float qx, float qy, float qz, float qw,
                           float sx, float sy, float sz) {
        static RestTrs fromAffine(Affine3x4 m) {
            float sx = length(m.m00(), m.m10(), m.m20());
            float sy = length(m.m01(), m.m11(), m.m21());
            float sz = length(m.m02(), m.m12(), m.m22());
            if (sx < 1.0e-8f || sy < 1.0e-8f || sz < 1.0e-8f) {
                throw new IllegalArgumentException("rest transform contains degenerate scale");
            }
            float r00=m.m00()/sx, r01=m.m01()/sy, r02=m.m02()/sz;
            float r10=m.m10()/sx, r11=m.m11()/sy, r12=m.m12()/sz;
            float r20=m.m20()/sx, r21=m.m21()/sy, r22=m.m22()/sz;
            float det = r00*(r11*r22-r12*r21)-r01*(r10*r22-r12*r20)+r02*(r10*r21-r11*r20);
            if (Math.abs(det - 1.0f) > 1.0e-3f) {
                throw new IllegalArgumentException("rest transform contains shear or reflection unsupported by TRS animation");
            }
            float[] q = quaternionFromRotation(r00,r01,r02,r10,r11,r12,r20,r21,r22);
            return new RestTrs(m.m03(),m.m13(),m.m23(),q[0],q[1],q[2],q[3],sx,sy,sz);
        }

        RestTrs withTranslation(float x,float y,float z){ return new RestTrs(x,y,z,qx,qy,qz,qw,sx,sy,sz); }
        RestTrs withScale(float x,float y,float z){ return new RestTrs(tx,ty,tz,qx,qy,qz,qw,x,y,z); }
        RestTrs withRotation(float x,float y,float z,float w){
            float[] q=normalizeQuaternion(new float[]{x,y,z,w});
            return new RestTrs(tx,ty,tz,q[0],q[1],q[2],q[3],sx,sy,sz);
        }

        Affine3x4 toAffine() {
            float x=qx,y=qy,z=qz,w=qw;
            float r00=1-2*(y*y+z*z), r01=2*(x*y-z*w), r02=2*(x*z+y*w);
            float r10=2*(x*y+z*w), r11=1-2*(x*x+z*z), r12=2*(y*z-x*w);
            float r20=2*(x*z-y*w), r21=2*(y*z+x*w), r22=1-2*(x*x+y*y);
            return new Affine3x4(r00*sx,r01*sy,r02*sz,tx,r10*sx,r11*sy,r12*sz,ty,r20*sx,r21*sy,r22*sz,tz);
        }

        private static float length(float x,float y,float z){ return (float)Math.sqrt(x*x+y*y+z*z); }

        private static float[] quaternionFromRotation(float m00,float m01,float m02,float m10,float m11,float m12,float m20,float m21,float m22) {
            float x,y,z,w;
            float trace=m00+m11+m22;
            if(trace>0){ float s=(float)Math.sqrt(trace+1.0f)*2; w=0.25f*s; x=(m21-m12)/s; y=(m02-m20)/s; z=(m10-m01)/s; }
            else if(m00>m11&&m00>m22){ float s=(float)Math.sqrt(1+m00-m11-m22)*2; w=(m21-m12)/s; x=0.25f*s; y=(m01+m10)/s; z=(m02+m20)/s; }
            else if(m11>m22){ float s=(float)Math.sqrt(1+m11-m00-m22)*2; w=(m02-m20)/s; x=(m01+m10)/s; y=0.25f*s; z=(m12+m21)/s; }
            else { float s=(float)Math.sqrt(1+m22-m00-m11)*2; w=(m10-m01)/s; x=(m02+m20)/s; y=(m12+m21)/s; z=0.25f*s; }
            return normalizeQuaternion(new float[]{x,y,z,w});
        }
    }
}
