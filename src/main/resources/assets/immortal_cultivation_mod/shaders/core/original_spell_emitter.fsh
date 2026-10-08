#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in float vertexDistance;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;

void main() {
    vec4 color = vertexColor * ColorModulator;
    // Spell styling occupies separate UV bands. The reference light ball keeps legacy materials.
    if (texCoord.x >= 11.5) {
        float crossSection = abs((floor(texCoord.y * 32.0) + 0.5) / 32.0 - 0.5) * 2.0;
        float core = 1.0 - smoothstep(0.12, 0.38, crossSection);
        color.rgb = mix(color.rgb, vec3(1.0, 0.99, 0.94), core * 0.78);
        color.a *= (0.26 + 0.74 * core) * (1.0 - smoothstep(0.65, 1.0, crossSection));
        if (color.a < 0.01) discard;
    } else if (texCoord.x >= 9.5) {
        // Solid square mote.
    } else if (texCoord.x >= 7.5) {
        vec2 pixel = abs(floor((texCoord - vec2(8.0, 0.0)) * 7.0) - vec2(3.0));
        // Stepped four-point sparkle: long narrow arms and a three-pixel center.
        if (!(pixel.x < 0.5 || pixel.y < 0.5 || max(pixel.x, pixel.y) < 1.5)) discard;
    } else if (texCoord.x >= 5.5) {
        vec2 pixel = (floor((texCoord - vec2(6.0, 0.0)) * 16.0) + 0.5) / 16.0;
        if (length(pixel - vec2(0.5)) > 0.47) discard;
    } else if (texCoord.x >= 3.5) {
        vec2 pixel = (floor((texCoord - vec2(4.0, 0.0)) * 48.0) + 0.5) / 48.0;
        float radius = length(pixel - vec2(0.5));
        color.a *= 1.0 - smoothstep(0.37, 0.5, radius);
        if (color.a < 0.01) discard;
    } else if (texCoord.x >= 1.5) {
        // The legacy built-in circle has a 0.3 UV radius and cubic alpha falloff.
        vec2 uv = texCoord - vec2(2.0, 0.0);
        float radial = clamp(1.0 - length(uv - vec2(0.5)) / 0.3, 0.0, 1.0);
        color.a *= radial * radial * (3.0 - 2.0 * radial);
        if (color.a < 0.01) discard;
    } else {
        color *= texture(Sampler0, texCoord);
        if (color.a < 0.1) discard;
    }
    if (color.a <= 0.0) discard;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
