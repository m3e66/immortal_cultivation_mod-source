#version 150
uniform vec4 ColorModulator;
uniform sampler2D Sampler0;
in vec4 tint;
in vec2 surface;
in float phase;
in float longitude;
flat in int family;
in vec3 viewNormal;
in vec3 viewPosition;
out vec4 fragColor;

float hash(vec3 p) {
    p = fract(p * .1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}
float noise(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + vec3(1,0,0)), f.x),
                   mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
               mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x),
                   mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y), f.z);
}
void main() {
    int element = family % 16;
    int surfaceFamily = family % 64;
    int art = family / 64 - 1;
    bool coating = surfaceFamily >= 16;
    float speed = element == 5 ? 1.7 : element == 1 || element == 6 ? .65 : element == 4 ? .55 : element == 7 ? 1.6 : 1.0;
    float clock = phase * .05 * speed;
    float t = surfaceFamily >= 32 ? longitude * .8 : surface.y;
    vec3 flow = vec3(cos(longitude) * 2.4, t * 7.0 - clock * 2.8, sin(longitude) * 2.4);
    if (art == 6) flow = vec3(cos(longitude + t * 3.0 + clock) * (3.3 - t * 1.8),
                              t * 6.0 + clock * 2.2, sin(longitude + t * 3.0 + clock) * (3.3 - t * 1.8));
    float facing = abs(dot(normalize(viewNormal), normalize(viewPosition)));
    if (art == 5) {
        // Opposing flame currents share one surface, including fists and weapon coatings.
        // Whole cycles close at the packed 75-second shader clock boundary.
        float cycle = clock * (6.28318530718 / 75.0);
        float curl = sin(t * 8.0 + longitude * 2.0 + cycle * 3.0) * .16;
        float violet = pow(.5 + .5 * cos(longitude - t * 4.8 + cycle * 9.0 + curl), 3.0);
        float jade = pow(.5 + .5 * cos(longitude + t * 4.8 - cycle * 9.0 + 3.14159265 - curl), 3.0);
        float wisps = noise(vec3(cos(longitude) * 2.2 + sin(cycle * 2.0),
                                 t * 6.0 + cos(cycle * 2.0), sin(longitude) * 2.2));
        float strength = max(violet, jade);
        float balance = jade / max(.001, jade + violet);
        vec3 violetColor = mix(vec3(.22, .075, .46), vec3(.69, .43, .98), violet);
        vec3 jadeColor = mix(vec3(.25, .58, .49), vec3(.77, 1.0, .87), jade);
        vec3 color = mix(violetColor, jadeColor, balance);
        float crest = pow(strength, 3.0) * (.45 + wisps * .55);
        color = mix(color, mix(vec3(.88, .76, 1.0), vec3(.91, 1.0, .94), balance), crest * .38);
        float alpha = .065 + strength * (.52 + wisps * .20);
        if (!coating) {
            float poles = smoothstep(0.0, .07, t) * (1.0 - smoothstep(.79 + wisps * .12, 1.0, t));
            float rim = pow(1.0 - facing, 2.0);
            alpha = (alpha * (.48 + facing * .52) + rim * .055) * poles;
        } else if (surfaceFamily >= 32) {
            float mask = texture(Sampler0, surface).a;
            if (mask < .1) discard;
            alpha *= mask;
        }
        fragColor = vec4(color, clamp(alpha * tint.a, 0.0, .72)) * ColorModulator;
        if (fragColor.a < .003) discard;
        return;
    }
    float broad = noise(flow + vec3(0, 0, clock * .16));
    float detail = noise(flow * 2.3 + broad * 1.5 - vec3(0, clock * 1.3, 0));
    float density = broad * .7 + detail * .3;
    vec3 core = min(vec3(1), tint.rgb * 1.25 + vec3(.055));
    vec3 glow = mix(core, vec3(1), .42);
    if (element == 2) glow = mix(core, vec3(.08, 1, .70), .78);
    if (element == 9) core *= .65;
    float alpha;
    vec3 color;
    if (coating) {
        color = mix(core, glow, density * .6);
        alpha = .30 + density * .40;
        if (surfaceFamily >= 32) {
            float mask = texture(Sampler0, surface).a;
            if (mask < .1) discard;
            alpha *= mask;
        }
    } else {
        float layer = surface.x;
        float edge = pow(smoothstep(0.0, .85, facing), .7);
        float base = smoothstep(0.0, .045, t);
        float crown = 1.0 - smoothstep(.90 + density * .04, 1.0, t);
        float tongues = smoothstep(.25, .72, density + (1.0 - t) * .10);
        if (layer > 1.5) {
            // Broad, low-opacity emission around the main volume replaces the hard ink outline.
            color = glow;
            float halo = smoothstep(0.0, .35, facing) * pow(1.0 - facing, .5);
            alpha = (layer > 2.5 ? .08 : .14) * halo * (0.45 + density) * crown * base;
        } else {
            color = mix(core, glow, smoothstep(.52, 1.0, t) * (.25 + density * .35));
            alpha = (layer > .5 ? .65 : .95) * edge * (.35 + .65 * tongues) * base * crown;
            alpha += .18 * pow(detail, 3.0) * edge * crown * base;
        }
    }
    if (element == 5) {
        float arc = pow(max(0.0, sin(t * 43.0 + longitude * 9.0 - clock * 18.0)), 28.0);
        color = mix(color, art == 14 ? vec3(.25, 1.0, .82) : vec3(.8, .9, 1), arc * .85);
        alpha += arc * .12;
    }
    if (art == 0) {
        alpha *= .58;
        color = mix(color, vec3(.66, .98, 1.0), .22);
    } else if (art == 1) {
        alpha *= .74 + .06 * sin(t * 8.0 - clock);
        color = mix(color, vec3(.78, 1.0, .90), .30);
    } else if (art >= 2 && art <= 4) {
        color *= vec3(.82, .61, .68);
        alpha = min(.97, alpha * (1.12 + float(art - 2) * .20));
    } else if (art == 6) {
        color *= .58;
        alpha *= 1.12;
    } else if (art == 7) {
        // Keep the original mint aura. Cutting flashes come only from confirmed hit events.
        color = mix(color, vec3(.847, 1.0, .941), density * .30);
        alpha *= .48;
    } else if (art == 9) {
        color = mix(vec3(.93, .16, .035), vec3(1.0, .76, .24), smoothstep(.42, .85, density));
    } else if (art == 10) {
        float ridge = pow(.5 + .5 * sin(longitude * 6.0 + t * 20.0), 14.0);
        color = mix(vec3(.29, .64, .83), vec3(.91, 1.0, 1.0), ridge * .75 + detail * .25);
        alpha *= .64 + ridge * .5;
    } else if (art == 11) {
        float band = smoothstep(.48, .78, .5 + .5 * cos(t * 22.0));
        color = mix(vec3(.67, .63, .51), vec3(1.0, .97, .84), band);
        alpha *= .45 + band * .5;
    } else if (art == 12) {
        color = mix(vec3(.10, .025, .19), vec3(.28, .67, .40), pow(detail, 5.0));
        alpha *= .75;
    } else if (art == 13) {
        color = mix(color, vec3(1.0, .98, .81) * (.35 + tint.r * .65), smoothstep(.4, .9, density) * .82);
        alpha *= .60 + tint.r * .45;
    } else if (art == 15) {
        float strata = smoothstep(.35, .75, .5 + .5 * sin(t * 25.0 - clock * .55 + broad));
        color = mix(vec3(.39, .23, .08), vec3(.95, .72, .28), strata);
        alpha *= .75 + strata * .25;
    } else if (art == 16) {
        float foam = pow(smoothstep(.35, .95, density), 3.0);
        color = mix(vec3(.035, .16, .33), vec3(.45, .94, 1.0), foam);
        alpha *= .65 + foam * .4;
    }
    fragColor = vec4(color, clamp(alpha * tint.a, 0.0, .88)) * ColorModulator;
    if (fragColor.a < .003) discard;
}
