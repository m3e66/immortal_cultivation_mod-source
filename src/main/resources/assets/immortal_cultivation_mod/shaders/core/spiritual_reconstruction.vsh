#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float GameTime;

out vec4 vertexColor;
out vec2 texCoord0;
out vec3 viewNormal;
out vec3 viewDirection;
out vec3 spiritualPosition;
out float materialInterference;
out float pressureStrength;
out float concealmentStrength;
flat out float signatureClass;

void main() {
    float time = GameTime * 1200.0;
    materialInterference = clamp(float(UV2.x) / 255.0, 0.0, 1.0);
    pressureStrength = clamp(float(UV2.y) / 255.0, 0.0, 1.0);
    concealmentStrength = clamp(float(UV1.x) / 255.0, 0.0, 1.0);
    signatureClass = round(clamp(float(UV1.y) / 255.0, 0.0, 1.0) * 3.0);

    float pressureWave = sin(
        Position.y * 5.0 + Position.x * 2.7
        - Position.z * 2.1 + time * 0.11
    );
    vec3 displaced = Position
        + Normal * pressureStrength * pressureWave * 0.012;
    vec4 viewPosition = ModelViewMat * vec4(displaced, 1.0);
    gl_Position = ProjMat * viewPosition;

    vertexColor = Color;
    texCoord0 = UV0;
    viewNormal = normalize(mat3(ModelViewMat) * Normal);
    viewDirection = normalize(-viewPosition.xyz);
    spiritualPosition = displaced;
}
