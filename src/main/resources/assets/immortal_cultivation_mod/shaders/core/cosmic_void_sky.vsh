#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;
out vec4 vertexColor;
out vec3 skyDirection;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
    // Color RGB carries the original cube direction. UV carries the current
    // longitude rotation and fast-forward trail span as constant face values.
    skyDirection = Color.rgb * 2.0 - 1.0;
}
