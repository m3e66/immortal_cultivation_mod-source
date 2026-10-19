#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 texCoord;
out vec4 tint;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position + Normal * .001, 1.0);
    texCoord = UV0;
    tint = Color;
}
