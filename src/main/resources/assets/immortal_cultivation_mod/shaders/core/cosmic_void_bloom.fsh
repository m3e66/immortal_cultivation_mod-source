#version 150

uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec2 offset = texCoord0 * 2.0 - 1.0;
    float radius = length(offset);
    if (radius >= 1.0) {
        discard;
    }

    const float bodyEdge = 0.43;
    float outside = max(radius - bodyEdge, 0.0);
    float edgeFade = 1.0 - smoothstep(0.86, 1.0, radius);
    float wideHalo = exp(-outside * 3.35) * edgeFade;
    float nearHalo = exp(-outside * 8.5) * edgeFade;
    float brightRim = exp(-pow((radius - bodyEdge) / 0.027, 2.0));

    float strength = vertexColor.a;
    float alpha = clamp((wideHalo * 0.17
            + nearHalo * 0.27
            + brightRim * 0.26) * strength, 0.0, 1.0);
    vec3 tint = vertexColor.rgb;
    vec3 hotColor = mix(tint, vec3(1.0), 0.62);
    vec3 color = tint * (wideHalo * 0.62 + nearHalo * 0.76)
            + hotColor * brightRim * 0.72;

    fragColor = vec4(color, alpha) * ColorModulator;
}
