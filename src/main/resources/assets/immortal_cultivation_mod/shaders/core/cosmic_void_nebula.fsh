#version 150

uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float hash21(vec2 point) {
    point = fract(point * vec2(123.34, 456.21));
    point += dot(point, point + 45.32);
    return fract(point.x * point.y);
}

float valueNoise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);
    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0, 1.0));
    return mix(mix(a, b, local.x), mix(c, d, local.x), local.y);
}

float fbm(vec2 point) {
    float value = 0.0;
    float amplitude = 0.56;
    for (int octave = 0; octave < 4; octave++) {
        value += valueNoise(point) * amplitude;
        point = mat2(1.63, -1.12, 1.12, 1.63) * point + 7.19;
        amplitude *= 0.48;
    }
    return value;
}

void main() {
    float ticks = GameTime * 24000.0;
    float seed = dot(vertexColor.rgb, vec3(17.3, 43.1, 71.7));
    vec2 drift = vec2(ticks * (0.000010 + seed * 0.0000015),
                      -ticks * (0.000006 + seed * 0.0000008));
    vec2 point = texCoord0 * vec2(3.25, 2.15) + drift + seed;
    float broad = fbm(point);
    float detail = fbm(point * 1.83 - drift * 0.31 + 12.7);
    float cloud = smoothstep(0.50, 0.83, broad * 0.74 + detail * 0.26);
    float centralBand = smoothstep(0.0, 0.22,
            0.5 - abs(texCoord0.y - 0.5 + sin(texCoord0.x * 5.0 + seed) * 0.07));
    vec2 edge = smoothstep(vec2(0.0), vec2(0.16), texCoord0)
              * (1.0 - smoothstep(vec2(0.84), vec2(1.0), texCoord0));
    float edgeFade = edge.x * edge.y;
    float alpha = vertexColor.a * cloud * centralBand * edgeFade;
    vec3 color = vertexColor.rgb * (0.54 + detail * 0.56);
    fragColor = vec4(color, alpha) * ColorModulator;
}
