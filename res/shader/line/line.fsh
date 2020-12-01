//Author: Dominik Lisowski
//
//Simple shader used for geometry (excluding text, as it uses the openGL fixed function pipeline)

#version 150

in vec4 pipeColor;

uniform vec4 brightness;

out vec4 outColor;

void main(void){
    outColor = pipeColor * brightness;
}