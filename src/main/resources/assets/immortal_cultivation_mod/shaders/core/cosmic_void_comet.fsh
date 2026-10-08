#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 comet = texture(Sampler0, texCoord0);
    if (comet.a < 0.004) {
        discard;
    }

    float ticks = GameTime * 24000.0;
    float pulse = 0.94 + sin(ticks * 0.115) * 0.06;
    float coreMask = smoothstep(0.45, 0.98, comet.a);
    vec3 violet = vec3(0.58, 0.38, 1.0);
    vec3 cyanWhite = vec3(0.80, 0.96, 1.0);
    vec3 tint = mix(violet, cyanWhite, coreMask);
    vec3 color = comet.rgb * tint * (1.10 + coreMask * 0.65) * pulse;
    fragColor = vec4(color, comet.a) * vertexColor * ColorModulator;
}
