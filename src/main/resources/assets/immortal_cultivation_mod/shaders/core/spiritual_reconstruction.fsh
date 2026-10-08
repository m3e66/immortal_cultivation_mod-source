#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewNormal;
in vec3 viewDirection;
in vec3 spiritualPosition;
in float materialInterference;
in float pressureStrength;
in float concealmentStrength;
flat in float signatureClass;

out vec4 fragColor;

float hash21(vec2 point) {
    point = fract(point * vec2(123.34, 456.21));
    point += dot(point, point + 45.32);
    return fract(point.x * point.y);
}

float layeredNoise(vec2 point) {
    float first = hash21(floor(point));
    float second = hash21(floor(point * 2.07 + 17.4));
    return first * 0.66 + second * 0.34;
}

void main() {
    vec4 source = texture(Sampler0, texCoord0);
    if (source.a < 0.08) {
        discard;
    }

    float time = GameTime * 1200.0;
    float beast = 1.0 - step(1.5, signatureClass);
    beast *= step(0.5, signatureClass);
    float cultivator = step(1.5, signatureClass)
        * (1.0 - step(2.5, signatureClass));
    float soul = step(2.5, signatureClass);

    float facing = abs(dot(normalize(viewNormal), normalize(viewDirection)));
    float fresnel = pow(1.0 - clamp(facing, 0.0, 1.0), 1.75);
    float innerRim = pow(1.0 - clamp(facing, 0.0, 1.0), 4.2);

    float flowSpeed = mix(0.10, 0.18, beast);
    float flowCoordinate = spiritualPosition.y * 8.5
        + sin(spiritualPosition.x * 5.2 + time * 0.035) * 1.15
        + cos(spiritualPosition.z * 4.6 - time * 0.028) * 0.82
        - time * flowSpeed;
    float qiFlow = pow(max(0.0, sin(flowCoordinate) * 0.5 + 0.5),
        mix(7.0, 11.0, cultivator));
    float crossingFlow = pow(max(0.0,
        sin(spiritualPosition.x * 7.0 - spiritualPosition.z * 6.0
            + spiritualPosition.y * 2.5 + time * 0.075) * 0.5 + 0.5),
        13.0);
    qiFlow = max(qiFlow, crossingFlow * (0.25 + cultivator * 0.65));

    vec2 noiseCell = gl_FragCoord.xy * 0.105
        + vec2(time * (0.055 + beast * 0.075), -time * 0.031);
    float noise = layeredNoise(noiseCell);
    float jaggedPulse = 0.5 + 0.5 * sin(
        time * 0.31 + spiritualPosition.x * 9.0
        - spiritualPosition.z * 7.0
    );

    float breakup = materialInterference * 0.48
        + concealmentStrength * 0.54;
    float fragmentThreshold = breakup
        * mix(0.46, 0.68, concealmentStrength);
    float fragmentNoise = layeredNoise(
        gl_FragCoord.xy * vec2(0.075, 0.17)
        + vec2(floor(time * 0.14), -floor(time * 0.09))
    );
    if (fragmentNoise < fragmentThreshold
        && fresnel < mix(0.72, 0.90, concealmentStrength)) {
        discard;
    }

    vec3 spiritualColor = vertexColor.rgb;
    spiritualColor = mix(spiritualColor,
        vec3(0.43, 0.025, 0.16), beast * 0.42);
    spiritualColor = mix(spiritualColor,
        vec3(0.88, 0.97, 1.0), cultivator * 0.18);
    spiritualColor = mix(spiritualColor,
        vec3(0.72, 0.52, 1.0), soul * 0.35);

    float bodyNoise = 0.82 + noise * 0.18;
    bodyNoise *= mix(1.0, 0.76 + jaggedPulse * 0.32, beast);
    float pressurePulse = pressureStrength
        * (0.5 + 0.5 * sin(time * 0.075
            + spiritualPosition.y * 3.0));
    vec3 finalColor = spiritualColor
        * (0.44 + fresnel * 1.45 + qiFlow * 0.74);
    finalColor += vec3(0.76, 0.93, 1.0)
        * innerRim * (0.38 + cultivator * 0.45);
    finalColor += spiritualColor * pressurePulse * 0.30;
    finalColor *= bodyNoise;

    float alphaShape = 0.10
        + fresnel * 0.66
        + innerRim * 0.22
        + qiFlow * (0.12 + cultivator * 0.10)
        + pressurePulse * 0.07;
    float alpha = vertexColor.a * source.a * alphaShape;
    alpha *= 1.0 - materialInterference * 0.36;
    alpha *= mix(1.0, 0.72 + jaggedPulse * 0.28, beast);

    fragColor = vec4(finalColor, clamp(alpha, 0.0, 0.92))
        * ColorModulator;
}
