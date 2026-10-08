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
    vec4 source = texture(Sampler0, texCoord);
    float alpha = source.a * vertexColor.a * ColorModulator.a;
    if (alpha < 0.01) discard;

    // Use source heat/brightness, not its orange hue, so blue and green remain saturated.
    float heat = dot(source.rgb, vec3(0.299, 0.587, 0.114));
    vec3 color = vertexColor.rgb * mix(0.35, 1.0, smoothstep(0.0, 0.75, heat));
    color = mix(color, vec3(1.0), smoothstep(0.72, 1.0, heat) * 0.75);
    fragColor = linear_fog(vec4(color * ColorModulator.rgb, alpha), vertexDistance, FogStart, FogEnd, FogColor);
}
