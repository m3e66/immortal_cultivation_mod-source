#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec4 ColorModulator;
in vec2 texCoord;
in float bodyHeight;
in float opacity;
flat in float growth;
flat in float cracking;
out vec4 fragColor;
void main() {
    if (texture(Sampler0, texCoord).a < .1) discard;
    // Vanilla's real ice texture follows the posed model UVs; the original skin supplies its cutout mask.
    vec4 ice = texture(Sampler1, fract(texCoord * 4.0));
    float grain = dot(ice.rgb, vec3(.2126, .7152, .0722));
    // Leave the last band uncovered until the server actually confirms a full freeze.
    float reach = min(.965, growth * 1.02 - .04 + grain * .02);
    float coverage = growth >= 1.0 ? 1.0 : 1.0 - smoothstep(reach - .045, reach + .015, bodyHeight);
    float ridges = smoothstep(.72, .95, grain);
    float fracture = 1.0 - smoothstep(.015, .055 + cracking * .12,
        abs(sin(texCoord.x * 37.0 + texCoord.y * 29.0 + sin(texCoord.y * 67.0))));
    vec3 color = ice.rgb;
    color = mix(color, vec3(1.0), fracture * cracking);
    float alpha = (.12 + growth * .70 + ridges * .12) * coverage;
    alpha *= 1.0 - cracking * fracture * .9;
    alpha *= 1.0 - smoothstep(.78, 1.0, cracking);
    fragColor = vec4(color, alpha * opacity) * ColorModulator;
    if (fragColor.a < .004) discard;
}
