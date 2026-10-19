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
    vec4 color = texture(Sampler0, texCoord) * vertexColor * ColorModulator;
    // Retain faint bloom edges that vanilla emissive shaders discard below 0.1.
    if (color.a <= 0.0) discard;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
