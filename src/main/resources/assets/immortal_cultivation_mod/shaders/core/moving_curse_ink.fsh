#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
in vec4 vertexColor;
in vec2 runeUV;
in vec2 skinUV;
out vec4 fragColor;
void main() {
    float alpha = texture(Sampler0, runeUV).a * texture(Sampler1, skinUV).a * vertexColor.a * ColorModulator.a;
    if (alpha < 0.1) discard;
    // Ink stays black during damage flashes and under different world lighting.
    fragColor = vec4(0.0, 0.0, 0.0, alpha);
}
