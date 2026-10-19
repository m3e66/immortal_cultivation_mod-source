#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec4 tint;
out vec2 surface;
out float phase;
out float longitude;
flat out int family;
out vec3 viewNormal;
out vec3 viewPosition;
void main(){
    vec4 position=ModelViewMat*vec4(Position,1.0);
    gl_Position=ProjMat*position;
    tint=Color;surface=UV0;phase=float(UV1.x)*.05;family=UV1.y;
    longitude=float(UV2.x)*.001;
    viewNormal=mat3(ModelViewMat)*Normal;viewPosition=position.xyz;
}
