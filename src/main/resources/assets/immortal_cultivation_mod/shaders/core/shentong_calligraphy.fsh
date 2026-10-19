#version 150
uniform sampler2D Sampler0;
uniform vec4 GlyphRect;
uniform float Brush;
uniform float Fade;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
float noise(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
void main(){
    vec2 uv=(texCoord-GlyphRect.xy)/GlyphRect.zw;
    vec3 ink=texture(Sampler0,texCoord).rgb;
    // Ragged bristles race diagonally through the actual calligraphic stroke mask.
    float grain=noise(floor(uv*vec2(79.0,107.0)));
    float sweep=uv.y*.73+uv.x*.27+sin(uv.x*84.0)*.025+(grain-.5)*.055;
    float write=smoothstep(sweep-.028,sweep+.028,Brush*1.14-.035);
    float rim=max(0.0,ink.b-ink.g);
    float hot=exp(-abs(sweep-(Brush*1.14-.035))*65.0)*(1.0-step(.999,Brush));
    float alpha=max(ink.g*.98,max(rim*.88,ink.r*.52))*write*Fade*vertexColor.a;
    if(alpha<.004)discard;
    vec3 color=mix(vec3(.99,.72,.32),vec3(1.0,.95,.73),rim);
    color=mix(color,vec3(.035,.025,.018),ink.g);
    color=mix(color,vec3(1.0,.96,.77),hot*ink.b*.82);
    fragColor=vec4(color*vertexColor.rgb,alpha);
}
