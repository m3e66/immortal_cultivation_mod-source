#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec2 texCoord0;
in vec4 vertexColor;
in vec3 skyDirection;

out vec4 fragColor;

float starMask(vec3 color) {
    float peak = max(color.r, max(color.g, color.b));
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    return smoothstep(0.34, 0.78, peak)
            * smoothstep(0.16, 0.62, luminance);
}

void main() {
    const float inverseTau = 0.15915494309;
    const float inversePi = 0.31830988618;
    vec3 direction = normalize(skyDirection);
    vec2 panoramaUv = vec2(
            fract(0.5 + atan(direction.z, direction.x) * inverseTau
                    + texCoord0.x * inverseTau),
            0.5 - asin(clamp(direction.y, -1.0, 1.0)) * inversePi);
    vec4 panorama = texture(Sampler0, panoramaUv);

    float trailTurns = max(0.0, texCoord0.y) * inverseTau;
    float trailOpacity = smoothstep(0.0, 0.16 * inverseTau, trailTurns);
    vec3 starTrail = vec3(0.0);
    if (trailTurns > 0.00001) {
        const int trailSamples = 32;
        for (int sampleIndex = 1; sampleIndex <= trailSamples; sampleIndex++) {
            float progress = float(sampleIndex) / float(trailSamples);
            vec2 trailUv = vec2(
                    fract(panoramaUv.x - trailTurns * progress),
                    panoramaUv.y);
            vec3 sampleColor = texture(Sampler0, trailUv).rgb;
            float strength = starMask(sampleColor)
                    * (1.0 - progress) * 0.82 * trailOpacity;
            starTrail = max(starTrail, sampleColor * strength);
        }
    }
    panorama.rgb += starTrail;
    fragColor = panorama
            * vec4(1.0, 1.0, 1.0, vertexColor.a) * ColorModulator;
}
