#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec2 texCoord;
in vec4 tint;
out vec4 fragColor;
void main() {
    float mask = texture(Sampler0, texCoord).a;
    if (mask < .1) discard;
    fragColor = vec4(tint.rgb, tint.a * mask) * ColorModulator;
}
