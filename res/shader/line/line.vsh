#version 150

//Author: Dominik Lisowski
//
//Simple shader used for geometry (excluding text, as it uses the openGL fixed function pipeline)

in vec2 position;
in vec4 color;

uniform mat4 pvm;

out vec4 pipeColor;

void main(void) {
    pipeColor = color;
    gl_Position =  pvm * vec4(position, 0.0, 1.0);
}