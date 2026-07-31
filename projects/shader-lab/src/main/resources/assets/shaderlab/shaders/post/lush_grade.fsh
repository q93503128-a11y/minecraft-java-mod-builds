#version 330

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

vec3 acesTonemap(vec3 color) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((color * (a * color + b)) / (color * (c * color + d) + e), 0.0, 1.0);
}

vec3 sampleChromaticScene(vec2 uv) {
    vec2 centered = uv * 2.0 - 1.0;
    float edge = clamp(dot(centered, centered), 0.0, 1.5);
    vec2 offset = centered * edge * 1.15 / max(SceneSize, vec2(1.0));

    float red = texture(SceneSampler, clamp(uv + offset, 0.0, 1.0)).r;
    float green = texture(SceneSampler, uv).g;
    float blue = texture(SceneSampler, clamp(uv - offset, 0.0, 1.0)).b;
    return vec3(red, green, blue);
}

void main() {
    vec4 source = texture(SceneSampler, texCoord);
    vec3 color = max(sampleChromaticScene(texCoord), vec3(0.0));
    vec3 bloom = max(texture(BloomSampler, texCoord).rgb, vec3(0.0));

    float bloomWarmth = clamp((bloom.r - bloom.b) * 2.0 + 0.5, 0.0, 1.0);
    vec3 coolGlow = bloom * vec3(0.78, 1.08, 1.28);
    vec3 warmGlow = bloom * vec3(1.24, 1.02, 0.76);
    vec3 pearlescentBloom = mix(coolGlow, warmGlow, bloomWarmth);

    color = color * 1.12 + pearlescentBloom * 1.18;

    float preToneLuma = luminance(color);
    float shadowMask = 1.0 - smoothstep(0.10, 0.48, preToneLuma);
    float highlightMask = smoothstep(0.45, 1.05, preToneLuma);

    color += shadowMask * vec3(-0.015, 0.010, 0.040);
    color += highlightMask * vec3(0.055, 0.025, -0.020);

    float gray = luminance(color);
    float chroma = max(max(color.r, color.g), color.b) - min(min(color.r, color.g), color.b);
    float vibrance = 1.16 + (1.0 - clamp(chroma, 0.0, 1.0)) * 0.10;
    color = mix(vec3(gray), color, vibrance);

    color = acesTonemap(color * 1.22);

    float depth = texture(SceneDepthSampler, texCoord).r;
    float distantMask = 1.0 - smoothstep(0.0005, 0.0200, depth);
    vec3 atmosphericColor = mix(vec3(0.43, 0.61, 0.82), vec3(0.68, 0.54, 0.82), texCoord.y);
    color = mix(color, atmosphericColor, distantMask * 0.055);

    vec2 centered = texCoord * 2.0 - 1.0;
    float radial = dot(centered, centered);
    float vignette = 1.0 - smoothstep(0.38, 1.42, radial) * 0.13;
    color *= vignette;

    float centerAura = 1.0 - smoothstep(0.05, 1.15, length(centered));
    color += pearlescentBloom * centerAura * 0.10;

    color = clamp(color, 0.0, 1.0);
    fragColor = vec4(color, source.a);
}
