#version 150

uniform vec4 ColorModulator;
uniform float SurfaceRotation;
uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

float hash31(vec3 point) {
    point = fract(point * 0.1031);
    point += dot(point, point.yzx + 33.33);
    return fract((point.x + point.y) * point.z);
}

float valueNoise3(vec3 point) {
    vec3 cell = floor(point);
    vec3 local = fract(point);
    local = local * local * (3.0 - 2.0 * local);

    float n000 = hash31(cell + vec3(0.0, 0.0, 0.0));
    float n100 = hash31(cell + vec3(1.0, 0.0, 0.0));
    float n010 = hash31(cell + vec3(0.0, 1.0, 0.0));
    float n110 = hash31(cell + vec3(1.0, 1.0, 0.0));
    float n001 = hash31(cell + vec3(0.0, 0.0, 1.0));
    float n101 = hash31(cell + vec3(1.0, 0.0, 1.0));
    float n011 = hash31(cell + vec3(0.0, 1.0, 1.0));
    float n111 = hash31(cell + vec3(1.0, 1.0, 1.0));

    float n00 = mix(n000, n100, local.x);
    float n10 = mix(n010, n110, local.x);
    float n01 = mix(n001, n101, local.x);
    float n11 = mix(n011, n111, local.x);

    return mix(
        mix(n00, n10, local.y),
        mix(n01, n11, local.y),
        local.z
    );
}

float fbm(vec3 point) {
    float value = 0.0;
    float weight = 0.55;

    for (int octave = 0; octave < 5; octave++) {
        value += valueNoise3(point) * weight;
        point = point * 2.03 + vec3(1.7, -2.4, 0.8);
        weight *= 0.48;
    }

    return value;
}

vec3 rotateY(vec3 point, float angle) {
    float cosine = cos(angle);
    float sine = sin(angle);

    return vec3(
        point.x * cosine + point.z * sine,
        point.y,
        -point.x * sine + point.z * cosine
    );
}

vec2 sphereUv(vec3 point) {
    const float inverseTau = 0.15915494309;
    const float inversePi = 0.31830988618;

    return vec2(
        0.5 + atan(point.z, point.x) * inverseTau,
        0.5 - asin(clamp(point.y, -1.0, 1.0)) * inversePi
    );
}

void main() {
    vec2 point = texCoord0 * 2.0 - 1.0;
    float radius = length(point);

    const float bodyRadius = 0.785;
    const float atmosphereRadius = 0.985;

    if (radius > atmosphereRadius) {
        discard;
    }

    float antialias = max(fwidth(radius) * 1.6, 0.0012);

    float bodyMask = 1.0 - smoothstep(
        bodyRadius - antialias,
        bodyRadius + antialias,
        radius
    );

    float normalizedRadius = min(radius / bodyRadius, 1.0);

    float depth = sqrt(
        max(0.0, 1.0 - normalizedRadius * normalizedRadius)
    );

    vec3 normal = normalize(vec3(
        point.x / bodyRadius,
        -point.y / bodyRadius,
        depth
    ));

    vec3 surfacePoint = rotateY(normal, SurfaceRotation);
    vec3 baseSurface = texture(Sampler0, sphereUv(surfacePoint)).rgb;

    vec3 lightDirection = normalize(vec3(0.98, 0.10, 0.055));

    float normalLight = dot(normal, lightDirection);
    float daylight = smoothstep(-0.06, 0.52, normalLight);

    vec3 bodyColor = baseSurface * (0.12 + daylight * 1.55);

    float fresnel = pow(1.0 - depth, 3.3);
    float litRim = smoothstep(-0.18, 0.72, normalLight);

    bodyColor += vec3(0.055, 0.20, 0.58)
        * fresnel
        * (0.13 + litRim * 1.42);

    vec3 edgeDirection = normalize(
        vec3(point.x, -point.y, 0.001)
    );

    float edgeLight = smoothstep(
        -0.24,
        0.86,
        dot(edgeDirection, lightDirection)
    );

    float outsideDistance = clamp(
        (radius - bodyRadius)
        / (atmosphereRadius - bodyRadius),
        0.0,
        1.0
    );

    float outsideBand =
        smoothstep(
            bodyRadius - antialias,
            bodyRadius + antialias,
            radius
        )
        * pow(1.0 - outsideDistance, 2.2);

    float atmosphereAlpha =
        outsideBand * (0.08 + edgeLight * 0.78);

    vec3 atmosphereColor = mix(
        vec3(0.025, 0.10, 0.36),
        vec3(0.42, 0.70, 1.0),
        pow(edgeLight, 1.7)
    );

    float alpha =
        max(bodyMask, atmosphereAlpha)
        * vertexColor.a;

    vec3 color = mix(
        atmosphereColor,
        bodyColor,
        bodyMask
    );

    fragColor =
        vec4(color * vertexColor.rgb, alpha)
        * ColorModulator;
}