#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    // The captured scene supplies RGB; the mesh supplies soft coverage independently of scene alpha.
    vec3 scene = texture(Sampler0, texCoord).rgb;
    fragColor = vec4(scene * vertexColor.rgb * ColorModulator.rgb,
            vertexColor.a * ColorModulator.a);
}
