//Author: Dominik Lisowski
//
//one-pass blur filter with linear fallof.
//falloff can be further adjusted by an exponent (similar to a gamma value)
//
//The decision was against a two-pass blur. While the performance loss is very severe,
//it still provides acceptable results with clever use of different LoD levels. The
//one-pass filter was choosen, as a correctly working two-pass filter needs at least
//2 offscreen textures at a time to read from. It seemed like the lesser evil to
//use a one-pass blur instead of blowing up the FrameBuffer capabilities.

#version 150

in vec2 pipeTexCord;

uniform sampler2D fboTex;
uniform int width;
uniform int texW;
uniform int texH;
uniform float strength;
uniform float baseBoost;
uniform int quality;
uniform float fallOff;

out vec4 outColor;

void main(void) {
    vec4 sum = vec4(0.0f, 0.0f, 0.0f, 0.0f);
    vec4 sample;
    vec2 tmpTexCord;
    //ivec2 texelCord;
    //take (quality*2)+1 samples in each dimension
    int totalSamples = (quality*2 + 1);
        totalSamples *= totalSamples;
    //(width / quality) tells, how often we sould shrink the texture size, log2 gives us the lod
    float lod = log2(width / quality);
    float maxLOD = log2(min(texW,texH)) - 1;
    float factor;

    for(float i=-quality; i<=quality; i++){
        for(float j=-quality; j<=quality; j++){

            //transform pixel difference to relative difference
            tmpTexCord = vec2(pipeTexCord.x + ((i/quality)*width)/texW,
                              pipeTexCord.y + ((j/quality)*width)/texH);

                                                     //higher LOD = more blurry, but kepp the max in mind
            sample = max(vec4(0,0,0,1), textureLod(fboTex, tmpTexCord, min(lod*1.5,7)));

            //texelCord = ivec2(pipeTexCord.x*texW + i, pipeTexCord.y*texH +j);
            //sample = texelFetch(tex, texelCord, 0);

            factor  = (i / quality) * (j / quality);
            if(factor < 0) factor *= -1;

            sample *= (1 - factor);
            sum += sample;
        }
    }
    sum /= totalSamples;
    sample = texture(fboTex, pipeTexCord);

    vec4 tmpSum = sum * sum;
    
    sum = mix(sum, tmpSum, fallOff);
    
    outColor = sum * sum * strength + sample * baseBoost;
    outColor = max(outColor, vec4(0,0,0,0));
    outColor.a = min(outColor.a, 1);
}
