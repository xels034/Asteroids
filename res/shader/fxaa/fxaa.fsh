//==============================================================================
//                     NVIDIA FXAA III.8 by TIMOTHY LOTTES
//------------------------------------------------------------------------------                      
//COPYRIGHT (C) 2010, 2011 NVIDIA CORPORATION. ALL RIGHTS RESERVED.
//------------------------------------------------------------------------------                       
//TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, THIS SOFTWARE IS PROVIDED 
//*AS IS* AND NVIDIA AND ITS SUPPLIERS DISCLAIM ALL WARRANTIES, EITHER EXPRESS 
//OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, IMPLIED WARRANTIES OF 
//MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL NVIDIA 
//OR ITS SUPPLIERS BE LIABLE FOR ANY SPECIAL, INCIDENTAL, INDIRECT, OR 
//CONSEQUENTIAL DAMAGES WHATSOEVER (INCLUDING, WITHOUT LIMITATION, DAMAGES FOR 
//LOSS OF BUSINESS PROFITS, BUSINESS INTERRUPTION, LOSS OF BUSINESS INFORMATION, 
//OR ANY OTHER PECUNIARY LOSS) ARISING OUT OF THE USE OF OR INABILITY TO USE 
//THIS SOFTWARE, EVEN IF NVIDIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH 
//DAMAGES.
//------------------------------------------------------------------------------
//https://docs.google.com/file/d/0B2manFVVrzQAOGE3ZjgzMDUtYWZmNC00ZWVlLWFhMGEtZDdjMzMxMjM4M2Y4/edit?authkey=CPWYuLIJ&num=50&sort=name&ddrp=1&layout=list
//==============================================================================
//

#version 410
#extension GL_EXT_gpu_shader4 : enable

in vec2 pipeTexCord;

const float FXAA3_QUALITY_EDGE_THRESHOLD_MIN = 1f/32f;
const float FXAA3_QUALITY_EDGE_THRESHOLD     = 1f/16f;
const float FXAA3_QUALITY_SUBPIX_TRIM        = 1f/2f;
const float FXAA3_QUALITY_SUBPIX_TRIM_SCALE  = (1f/1f - FXAA3_QUALITY_SUBPIX_TRIM);
const float FXAA3_QUALITY_SUBPIX_CAP         = 3f/4f;
const float FXAA3_SEARCH_STEPS               = 6;
const float FXAA3_SEARCH_THRESHOLD           =(1.0/4.0);

uniform sampler2D fboTex;

out vec4 outColor;

void main(void) {
	ivec2 g_texDim = textureSize2D(fboTex, 0);
	vec2 rcpFrame = vec2(1f/g_texDim.x, 1f/g_texDim.y);
	vec3 lm       = vec3(0.299, 0.587, 0.114);
	vec3 rgbo     = texture(fboTex, pipeTexCord).rgb;

	float lumaN  = dot(texture(fboTex, pipeTexCord + vec2( 0, -1) * rcpFrame).rgb, lm);
	float lumaW  = dot(texture(fboTex, pipeTexCord + vec2(-1,  0) * rcpFrame).rgb, lm);
	float lumaE  = dot(texture(fboTex, pipeTexCord + vec2(+1,  0) * rcpFrame).rgb, lm);
	float lumaS  = dot(texture(fboTex, pipeTexCord + vec2( 0, +1) * rcpFrame).rgb, lm);
	float lumaNW = dot(texture(fboTex, pipeTexCord + vec2(-1, -1) * rcpFrame).rgb, lm);
	float lumaNE = dot(texture(fboTex, pipeTexCord + vec2(+1, -1) * rcpFrame).rgb, lm);
	float lumaSW = dot(texture(fboTex, pipeTexCord + vec2(-1, +1) * rcpFrame).rgb, lm);
	float lumaSE = dot(texture(fboTex, pipeTexCord + vec2(+1, +1) * rcpFrame).rgb, lm);
	vec4 rgbyM  = texture(fboTex, pipeTexCord);
	rgbyM.a     = dot(rgbyM.rgb, lm);
	float lumaM = rgbyM.a;

	float rangeMin = min(lumaM, min(min(lumaN, lumaW), min(lumaS, lumaE)));
	float rangeMax = max(lumaM, max(max(lumaN, lumaW), max(lumaS, lumaE)));
	float range = rangeMax - rangeMin;

	float MIX = 1;
	if(range < max(FXAA3_QUALITY_EDGE_THRESHOLD_MIN, rangeMax * FXAA3_QUALITY_EDGE_THRESHOLD)){
		MIX = 0;
	}
	float lumaL = (lumaN + lumaW + lumaE + lumaS)/4f;
	float rangeL = abs(lumaL - lumaM);
	float blendL = clamp((rangeL / range) - FXAA3_QUALITY_SUBPIX_TRIM, 0, 1) * FXAA3_QUALITY_SUBPIX_TRIM_SCALE;
	blendL = min(FXAA3_QUALITY_SUBPIX_CAP, blendL);

	float edgeVert = abs(lumaNW + (-2f * lumaN) + lumaNE) +
				2f * abs(lumaW  + (-2f * lumaM) + lumaE ) +
					 abs(lumaSW + (-2f * lumaS) + lumaSE);
	float edgeHorz = abs(lumaNW + (-2f * lumaW) + lumaSW) +
				2f * abs(lumaN  + (-2f * lumaM) + lumaS ) +
					 abs(lumaNE + (-2f * lumaE) + lumaSE);
	bool horzSpan = edgeHorz >= edgeVert;
	
	float lengthSign = horzSpan ? -rcpFrame.y : -rcpFrame.x;
	if(!horzSpan) lumaN = lumaW;
	if(!horzSpan) lumaS = lumaE;
	float gradientN = abs(lumaN - lumaM);
	float gradientS = abs(lumaS - lumaM);
	lumaN = (lumaN + lumaM) /2f;
	lumaS = (lumaS + lumaM) /2f;

	bool pairN = gradientN >= gradientS;
	if(!pairN) lumaN       = lumaS;
	if(!pairN) gradientN   = gradientS;
	if(!pairN) lengthSign *= -1f;
	vec2 posN;
	posN.x = pipeTexCord.x + (horzSpan ? 0f : lengthSign/2f);
	posN.y = pipeTexCord.y + (horzSpan ? lengthSign/2f : 0f);

	gradientN *= FXAA3_SEARCH_THRESHOLD;

	vec2 posP = posN;
	vec2 offNP = horzSpan ? 
		vec2(rcpFrame.x, 0) :
		vec2(0, rcpFrame.y);
	float lumaEndN;
	float lumaEndP;
	bool doneN = false;
	bool doneP = false;
	posN += offNP * (-1.5);
	posP += offNP * ( 1.5);
	for(int i=0; i<FXAA3_SEARCH_STEPS; i++){
		lumaEndN = dot(texture(fboTex, posN).rgb, lm);
		lumaEndP = dot(texture(fboTex, posP).rgb, lm);

		bool doneN2 = abs(lumaEndN - lumaN) >= gradientN;
		bool doneP2 = abs(lumaEndP - lumaN) >= gradientN;
		if(doneN2 && !doneN) posN += offNP;
		if(doneP2 && !doneP) posP -= offNP;
		if(doneN2 && doneP2) break;
		doneN = doneN2;
		doneP = doneP2;
		if(!doneN) posN -= offNP * 2;
		if(!doneP) posP += offNP * 2;
	}
	
	float dstN = horzSpan ? pipeTexCord.x - posN.x : pipeTexCord.y - posN.y;
	float dstP = horzSpan ? posP.x - pipeTexCord.x : posP.y - pipeTexCord.y;
	
	bool directionN = dstN < dstP;
	lumaEndN = directionN ? lumaEndN : lumaEndP;
	
	if(((lumaM - lumaN) < 0) == ((lumaEndN - lumaN) < 0))
		lengthSign = 0;
		
	float spanLength = (dstP + dstN);
	dstN = directionN ? dstN : dstP;
	float subPixelOffset = 0.5 + (dstN * (-1f/spanLength));
	subPixelOffset += blendL * (1f/8f);
	subPixelOffset *= lengthSign;
	vec3 rgbF = texture(fboTex, vec2(
		pipeTexCord.x + (horzSpan ? 0 : subPixelOffset),
		pipeTexCord.y + (horzSpan ? subPixelOffset : 0))).rgb;
	
	float lumaF = dot(rgbF, lm) + (1f/(65536.0*256.0));
	float lumaB = mix(lumaF, lumaL, blendL);
	float scale = min(4f, lumaB/lumaF);
	rgbF *= scale;
	
	//rgbo = vec3(1,0,0);
	//rgbF = vec3(0,1,0);
	
	//rgbF = mix(vec3(0,1,0), vec3(1,0,0), MIX);
	rgbF = mix(rgbo, rgbF, MIX);
	outColor = vec4(rgbF, 1);
	//if(pipeTexCord.x > 0.5) outColor = vec4(rgbo,1);
}
