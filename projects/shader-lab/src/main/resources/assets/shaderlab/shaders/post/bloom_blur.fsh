#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BloomBlurConfig {
    vec2 Direction;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 stepSize = Direction / max(InSize, vec2(1.0));

    vec3 color = texture(InSampler, texCoord).rgb * 0.2270270270;
    color += texture(InSampler, texCoord + stepSize * 1.3846153846).rgb * 0.3162162162;
    color += texture(InSampler, texCoord - stepSize * 1.3846153846).rgb * 0.3162162162;
    color += texture(InSampler, texCoord + stepSize * 3.2307692308).rgb * 0.0702702703;
    color += texture(InSampler, texCoord - stepSize * 3.2307692308).rgb * 0.0702702703;

    fragColor = vec4(max(color, vec3(0.0)), 1.0);
}
