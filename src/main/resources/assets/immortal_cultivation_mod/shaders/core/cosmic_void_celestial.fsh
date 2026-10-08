#version 150

uniform vec4 ColorModulator;
uniform float GameTime;
uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

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
    return vec2(0.5 + atan(point.z, point.x) * inverseTau,
                0.5 - asin(clamp(point.y, -1.0, 1.0)) * inversePi);
}

void main() {
    vec2 point = texCoord0 * 2.0 - 1.0;
    float radius = length(point);
    const float bodyRadius = 0.94;
    if (radius > bodyRadius) {
        discard;
    }

    float antialias = max(fwidth(radius) * 1.6, 0.0012);
    float bodyMask = 1.0 - smoothstep(bodyRadius - antialias,
                                      bodyRadius + antialias, radius);
    float normalizedRadius = min(radius / bodyRadius, 1.0);
    float depth = sqrt(max(0.0, 1.0 - normalizedRadius * normalizedRadius));
    vec3 normal = normalize(vec3(point.x / bodyRadius,
                                 -point.y / bodyRadius, depth));

    float ticks = GameTime * 24000.0;
    float rotationSpeed = mix(0.000012, 0.000092, vertexColor.g);
    float rotation = ticks * rotationSpeed + vertexColor.r * 6.28318530718;
    vec3 surfacePoint = rotateY(normal, rotation);
    vec3 surface = texture(Sampler0, sphereUv(surfacePoint)).rgb;

    float emissive = vertexColor.a;
    vec3 lightDirection = normalize(vec3(0.88, 0.24, 0.41));
    float directLight = smoothstep(-0.24, 0.66, dot(normal, lightDirection));
    float light = mix(0.18 + directLight * 1.08, 1.14, emissive);
    vec3 color = surface * light;

    float fresnel = pow(1.0 - depth, 3.2) * vertexColor.b;
    vec3 rimColor = mix(surface, vec3(0.58, 0.74, 1.0), 0.48);
    color += rimColor * fresnel * (0.16 + emissive * 0.36);

    fragColor = vec4(color, bodyMask) * ColorModulator;
}
