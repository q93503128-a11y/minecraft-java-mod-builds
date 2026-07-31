#version 330

#moj_import <minecraft:globals.glsl>

uniform sampler2D SceneSampler;
uniform sampler2D BloomSampler;
uniform sampler2D SceneDepthSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 SceneSize;
    vec2 BloomSize;
    vec2 SceneDepthSize;
};

in vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

float hash21(vec2 point) {
    point = fract(point * vec2(123.34, 456.21));
    point += dot(point, point + 45.32);
    return fract(point.x * point.y);
}

float valueNoise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);

    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0, 1.0));

    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float fbm(vec2 point) {
    float total = 0.0;
    float amplitude = 0.55;

    for (int octave = 0; octave < 4; octave++) {
        total += valueNoise(point) * amplitude;
        point = point * 2.03 + vec2(17.13, 9.71);
        amplitude *= 0.48;
    }

    return total;
}

vec3 acesTonemap(vec3 color) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((color * (a * color + b)) / (color * (c * color + d) + e), 0.0, 1.0);
}

vec3 sharpenScene(vec2 uv) {
    vec2 texel = 1.0 / max(SceneSize, vec2(1.0));
    vec3 center = texture(SceneSampler, uv).rgb;
    vec3 crossBlur =
            texture(SceneSampler, clamp(uv + vec2(texel.x, 0.0), 0.0, 1.0)).rgb
          + texture(SceneSampler, clamp(uv - vec2(texel.x, 0.0), 0.0, 1.0)).rgb
          + texture(SceneSampler, clamp(uv + vec2(0.0, texel.y), 0.0, 1.0)).rgb
          + texture(SceneSampler, clamp(uv - vec2(0.0, texel.y), 0.0, 1.0)).rgb;
    crossBlur *= 0.25;

    return max(center + (center - crossBlur) * 0.24, vec3(0.0));
}

vec3 auroraCurtain(vec2 uv, float skyMask, float nightMask) {
    float time = GameTime * 1200.0;
    float upperSky = smoothstep(0.47, 0.64, uv.y) * (1.0 - smoothstep(0.985, 1.0, uv.y));

    float broadNoise = fbm(vec2(uv.x * 2.8 + time * 0.015, time * 0.006));
    float fineNoise = fbm(vec2(uv.x * 8.5 - time * 0.022, uv.y * 2.2 + time * 0.009));

    float centerA = 0.70
            + sin(uv.x * 7.0 + time * 0.055) * 0.055
            + (broadNoise - 0.5) * 0.12;
    float centerB = 0.79
            + sin(uv.x * 10.5 - time * 0.041 + 1.7) * 0.038
            + (fineNoise - 0.5) * 0.065;
    float centerC = 0.61
            + sin(uv.x * 5.2 + time * 0.032 + 3.1) * 0.030
            + (broadNoise - 0.5) * 0.050;

    float ribbonA = exp(-pow(abs(uv.y - centerA) * 15.5, 1.42));
    float ribbonB = exp(-pow(abs(uv.y - centerB) * 22.0, 1.34));
    float ribbonC = exp(-pow(abs(uv.y - centerC) * 27.0, 1.28));

    float verticalCurtain = pow(clamp(fbm(vec2(uv.x * 18.0 + time * 0.018, uv.y * 2.4)), 0.0, 1.0), 2.2);
    float folds = 0.52 + 0.48 * sin(uv.x * 44.0 + fineNoise * 9.0 + time * 0.075);
    folds = smoothstep(0.08, 1.0, folds);

    float intensity = (ribbonA * 0.95 + ribbonB * 0.62 + ribbonC * 0.38);
    intensity *= mix(0.48, 1.18, verticalCurtain) * mix(0.72, 1.18, folds);
    intensity *= upperSky * skyMask * nightMask;

    float hueFlow = 0.5 + 0.5 * sin(uv.x * 8.0 + time * 0.038 + broadNoise * 4.0);
    vec3 greenCyan = mix(vec3(0.10, 1.00, 0.65), vec3(0.10, 0.70, 1.12), hueFlow);
    vec3 violetPink = mix(vec3(0.42, 0.20, 1.10), vec3(1.00, 0.22, 0.78), 0.5 + 0.5 * sin(uv.x * 5.0 - time * 0.026));

    vec3 aurora = greenCyan * (ribbonA + ribbonC * 0.35);
    aurora += violetPink * ribbonB * 0.72;
    aurora *= mix(0.58, 1.25, verticalCurtain);
    aurora += vec3(0.30, 0.95, 1.10) * pow(intensity, 1.8) * 0.42;

    return aurora * intensity * 1.18;
}

vec3 dreamWater(vec3 sceneColor, vec2 uv, float depth, out float waterMask) {
    float time = GameTime * 1600.0;
    float blueLead = max(sceneColor.b, sceneColor.g * 0.92) - sceneColor.r;
    float cyanPresence = min(sceneColor.g, sceneColor.b);
    float aquaticHue = smoothstep(0.035, 0.22, blueLead)
            * smoothstep(0.10, 0.52, cyanPresence);

    float nonSky = smoothstep(0.0009, 0.0100, depth);
    float avoidUpperSky = 1.0 - smoothstep(0.84, 0.97, uv.y);
    float notNearBlack = smoothstep(0.035, 0.16, luminance(sceneColor));
    waterMask = clamp(aquaticHue * nonSky * avoidUpperSky * notNearBlack, 0.0, 1.0);

    vec2 aspectUv = vec2(uv.x * (SceneSize.x / max(SceneSize.y, 1.0)), uv.y);
    float waveA = sin(aspectUv.x * 34.0 + uv.y * 15.0 + time * 0.070);
    float waveB = sin(aspectUv.x * -23.0 + uv.y * 28.0 - time * 0.052);
    float waveC = sin(aspectUv.x * 11.0 + uv.y * -42.0 + time * 0.035);
    float wave = waveA * 0.45 + waveB * 0.34 + waveC * 0.21;

    float movingNoise = fbm(vec2(aspectUv.x * 7.0 + time * 0.012, uv.y * 9.0 - time * 0.008));
    float crest = smoothstep(0.48, 1.0, wave * 0.5 + movingNoise * 0.85);
    float glint = pow(max(0.0, 0.5 + 0.5 * sin(aspectUv.x * 53.0 + movingNoise * 12.0 + time * 0.11)), 9.0);

    float pearlShift = 0.5 + 0.5 * sin(aspectUv.x * 5.5 + uv.y * 7.0 + time * 0.018);
    vec3 lagoon = vec3(0.025, 0.42, 0.62);
    vec3 moonViolet = vec3(0.30, 0.17, 0.68);
    vec3 pearlColor = mix(vec3(0.08, 0.90, 1.00), vec3(0.70, 0.35, 1.00), pearlShift);

    vec3 recolored = mix(sceneColor, mix(lagoon, moonViolet, pearlShift * 0.28), 0.34);
    recolored += pearlColor * crest * 0.20;
    recolored += vec3(0.78, 0.96, 1.00) * glint * 0.18;
    recolored += vec3(0.04, 0.16, 0.22) * (movingNoise - 0.5) * 0.24;

    return max(recolored, vec3(0.0));
}

void main() {
    vec4 source = texture(SceneSampler, texCoord);
    vec3 color = sharpenScene(texCoord);
    vec3 bloom = max(texture(BloomSampler, texCoord).rgb, vec3(0.0));

    float depth = texture(SceneDepthSampler, texCoord).r;
    float skyMask = 1.0 - smoothstep(0.00045, 0.0060, depth);
    float sourceLuma = luminance(source.rgb);
    float nightMask = 1.0 - smoothstep(0.13, 0.43, sourceLuma);

    float bloomWarmth = clamp((bloom.r - bloom.b) * 1.55 + 0.5, 0.0, 1.0);
    vec3 coolGlow = bloom * vec3(0.72, 1.02, 1.22);
    vec3 warmGlow = bloom * vec3(1.17, 0.98, 0.73);
    vec3 pearlescentBloom = mix(coolGlow, warmGlow, bloomWarmth);

    color = color * 1.035 + pearlescentBloom * 0.66;

    float waterMask;
    vec3 waterColor = dreamWater(color, texCoord, depth, waterMask);
    color = mix(color, waterColor, waterMask * 0.78);

    vec3 aurora = auroraCurtain(texCoord, skyMask, nightMask);
    color += aurora;

    float preToneLuma = luminance(color);
    float shadowMask = 1.0 - smoothstep(0.09, 0.40, preToneLuma);
    float highlightMask = smoothstep(0.52, 1.15, preToneLuma);
    color += shadowMask * vec3(-0.005, 0.009, 0.026);
    color += highlightMask * vec3(0.028, 0.012, -0.009);

    float gray = luminance(color);
    float chroma = max(max(color.r, color.g), color.b) - min(min(color.r, color.g), color.b);
    float vibrance = 1.08 + (1.0 - clamp(chroma, 0.0, 1.0)) * 0.055;
    color = mix(vec3(gray), color, vibrance);

    color = acesTonemap(color * 1.08);

    float distantMask = 1.0 - smoothstep(0.0005, 0.0200, depth);
    vec3 atmosphericColor = mix(vec3(0.32, 0.52, 0.74), vec3(0.57, 0.42, 0.76), texCoord.y);
    color = mix(color, atmosphericColor, distantMask * (1.0 - nightMask) * 0.022);

    vec2 centered = texCoord * 2.0 - 1.0;
    float radial = dot(centered, centered);
    float vignette = 1.0 - smoothstep(0.52, 1.46, radial) * 0.055;
    color *= vignette;

    color += aurora * 0.10;
    color = clamp(color, 0.0, 1.0);
    fragColor = vec4(color, source.a);
}
