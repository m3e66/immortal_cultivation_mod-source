#version 150

uniform sampler2D DiffuseSampler;
uniform float Intensity;

in vec2 texCoord;

out vec4 fragColor;

vec2 safeUv(vec2 uv) {
    return clamp(uv, vec2(0.001), vec2(0.999));
}

void main() {
    float strength = clamp(Intensity, 0.0, 1.0);
    vec2 fromCenter = texCoord - vec2(0.5);
    float radius = length(fromCenter);
    float edgeMask = smoothstep(0.075, 0.72, radius);
    vec2 zoomStep = fromCenter * (0.010 + strength * 0.027) * strength;

    vec4 source = texture(DiffuseSampler, texCoord);
    vec3 zoomBlur = source.rgb * 0.24;
    zoomBlur += texture(DiffuseSampler, safeUv(texCoord - zoomStep * 0.45)).rgb * 0.18;
    zoomBlur += texture(DiffuseSampler, safeUv(texCoord - zoomStep * 0.95)).rgb * 0.16;
    zoomBlur += texture(DiffuseSampler, safeUv(texCoord - zoomStep * 1.55)).rgb * 0.14;
    zoomBlur += texture(DiffuseSampler, safeUv(texCoord - zoomStep * 2.20)).rgb * 0.11;
    zoomBlur += texture(DiffuseSampler, safeUv(texCoord - zoomStep * 2.95)).rgb * 0.09;
    zoomBlur += texture(DiffuseSampler, safeUv(texCoord - zoomStep * 3.75)).rgb * 0.08;

    float blurAmount = edgeMask * strength * 0.86;
    vec3 color = mix(source.rgb, zoomBlur, blurAmount);

    vec2 radialDirection = radius > 0.0001
            ? fromCenter / radius
            : vec2(0.0);
    vec2 chromaOffset = radialDirection
            * edgeMask
            * strength
            * (0.0018 + radius * 0.0065);
    float redChannel = texture(
            DiffuseSampler,
            safeUv(texCoord + chromaOffset)).r;
    float blueChannel = texture(
            DiffuseSampler,
            safeUv(texCoord - chromaOffset)).b;
    color.r = mix(color.r, redChannel, edgeMask * strength * 0.82);
    color.b = mix(color.b, blueChannel, edgeMask * strength * 0.82);

    float hotEdge = smoothstep(0.38, 0.92, radius) * strength;
    color *= 1.0 + hotEdge * 0.055;
    fragColor = vec4(color, source.a);
}
