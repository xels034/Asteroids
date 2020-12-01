//Author: Dominik Lisowski
//
//Vertex Shader used for all post-process shaders. It is linked in conjunction with any
//fragment shader meant for post-proccessing and is used to draw a screen-aligned quad

#version 150

in vec2 position;
in vec2 inTexCord;

out vec2 pipeTexCord;

void main(void) {
    pipeTexCord = inTexCord;
    gl_Position =  vec4(position, 0.0f, 1.0f);
}