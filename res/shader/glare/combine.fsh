#version 150

in vec2 pipeTexCord;

uniform sampler2D glare;
uniform sampler2D original;

out vec4 outColor;

void main(void){
    outColor = texture(glare, pipeTexCord) + texture(original, pipeTexCord);
}