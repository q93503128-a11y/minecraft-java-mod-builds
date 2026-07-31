#version 330

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

vec3 filmicCurve(vec3 color) {
    const float a = 2.51;
    const float b = 0.03;
    const float c = 2.43;
    const float d = 0.59;
    const float e = 0.14;
    return clamp((color * (a * color + b)) / (color * (c * color + d) + e), 0.0, 1.0);
}

void main() {
    vec4 source = texture(InSampler, texCoord);
    vec3 color = max(source.rgb, vec3(0.0));

    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    vec3 shadows = vec3(0.985, 1.005, 1.025);
    vec3 highlights = vec3(1.025, 1.010, 0.985);
    color *= mix(shadows, highlights, smoothstep(0.18, 0.82, luminance));

    color = mix(vec3(luminance), color, 1.075);
    color = filmicCurve(color * 1.12);
    color = (color - 0.5) * 1.035 + 0.5;

    fragColor = vec4(clamp(color, 0.0, 1.0), source.a);
}
