//Author: Dominik Lisowski
//
//Passes the read offscreen texture 1:1 to the rendertarget. It is supposed to be used in
//conjunction with the finalizeImage() method of the FrameBufferManager, if special circumstances
//dictate that.

#version 150

in vec2 pipeTexCord;

uniform sampler2D fboTex;

out vec4 outColor;

void main(void){
    outColor = clamp(texture(fboTex, pipeTexCord), vec4(0), vec4(1));
}