#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 texCoord;
out float bodyHeight;
out float opacity;
flat out float growth;
flat out float cracking;
void main() {
    growth = clamp(float(UV1.x) / 1000.0, 0.0, 1.0);
    cracking = clamp(float(UV1.y) / 1000.0, 0.0, 1.0);
    // Reuse the exact posed surface. Per-face normal extrusion opens cracks at shared corners.
    // The render material supplies a depth bias to avoid fighting the original skin.
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = UV0;
    opacity = Color.a;
    bodyHeight = (Position.y - float(UV2.x) / 128.0) / max(.05, float(UV2.y) / 256.0);
}
