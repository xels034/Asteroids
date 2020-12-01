//Author: DOminik Lisowski
//
//working one-pass blur split up in horizontal and vertical components. This was an attempt
//at a correct two-pass blur. However, correct results require 2 textures to
//read from simultaneously. This is not supported the the FrameBufferManager, and as the one-pass
//works acceptable, this isnt used.

#version 150

in vec2 pipeTexCord;

uniform sampler2D fboTex;
uniform int width;
uniform int texW;
uniform int texH;
uniform float strength;
uniform int quality;

out vec4 outColor;

void main(void) {
    vec4 sum = vec4(0.0f, 0.0f, 0.0f, 0.0f);
    vec4 sample;
    vec2 tmpTexCord;
    //ivec2 texelCord;
    //take (quality*2)+1 samples in each dimension
    int totalSamples = (quality*2 + 1);
    //(width / quality) tells, how often we sould shrink the texture size, log2 gives us the lod
    float lod = log2(width / quality);
    float maxLOD = log2(min(texW,texH)) - 1;
    float factor;


        for(float j=-quality; j<=quality; j++){

            //transform pixel difference to relative difference
            tmpTexCord = vec2(pipeTexCord.x,
                              pipeTexCord.y + ((j/quality)*width)/texH);
                                                     //higher LOD = more blurry, but kepp the max in mind
            sample = max(vec4(0,0,0,1), textureLod(fboTex, tmpTexCord, min(lod,maxLOD)));

            factor  = (j / quality);
            if(factor < 0) factor = -factor;

            sample *= (1 - factor);
            sum += sample;
        }

    sum /= totalSamples;
    
    outColor = sum * strength;
    outColor = max(outColor, vec4(0,0,0,0));
    outColor.a = min(outColor.a, 1);
}
