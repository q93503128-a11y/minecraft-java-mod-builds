#version 330

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    vec3 color = max(texture(InSampler, texCoord).rgb, vec3(0.0));
    float brightness = max(max(color.r, color.g), color.b);
    float luma = luminance(color);

    const float threshold = 0.66;
    const float knee = 0.24;

    float soft = clamp((brightness - threshold + knee) / (2.0 * knee), 0.0, 1.0);
    soft = soft * soft * knee;
    float contribution = max(soft, brightness - threshold) / max(brightness, 0.0001);

    vec3 highlight = color * contribution;
    highlight *= mix(0.86, 1.12, smoothstep(0.62, 1.0, luma));

    fragColor = vec4(max(highlight, vec3(0.0)), 1.0);
}
