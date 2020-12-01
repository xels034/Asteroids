//Author: DOminik Lisowski
//
//working one-pass blur split up in horizontal and vertical components. This was an attempt
//at a correct two-pass blur. However, correct results require 2 textures to
//read from simultaneously. This is not supported the the FrameBufferManager, and as the one-pass
//works acceptable, this isnt used.

#version 150

in vec2 pipeTexCord;

uniform sampler2D fboTex;
uniform int quality;
uniform vec2 dir;
uniform vec2 texDim;
uniform float strength;

out vec4 outColor;

void main(void) {
    vec4 sum = vec4(0.0f, 0.0f, 0.0f, 0.0f);
    vec4 sample;
    float factor;
    vec2 tmpTexCord;
    int totalSamples = (quality*2 + 1);
    //(width / quality) tells, how often to shrink the texture size, log2 gives the lod
    float lod = log2(length(dir) / quality);
    float maxLOD = log2(min(texDim.x, texDim.y)) - 1;

    for(float i=-quality; i<=quality; i++){
        factor = (i / quality);
        tmpTexCord = pipeTexCord + (dir*factor)/texDim;
        //for some inexplicable reason, samples sometimes return negative values, resulting in a
        //darkened image, so the values below 0 are clamped
        sample = max(vec4(0,0,0,1), textureLod(fboTex, tmpTexCord, min(lod,maxLOD)));

        //force a positive value
        factor *= sign(factor);

        //linear falloff
        sample *= (1 - factor);
        sum += sample;
    }
    sum /= totalSamples;

    outColor = sum * strength;
    outColor = max(outColor, vec4(0,0,0,0));
    outColor.a = min(outColor.a, 1);
}
