package util;

import org.lwjgl.util.vector.Vector2f;

/**
 * 
 * @author David-Peter Desh, Dominik Lisowski
 *
 * Simple collection of application wide constants
 *
 */

public class Ref {
  
  public static final int LOG_LEVEL = -1;
  
  //how long, in ms, it takes for a Construct to be discarded if it isn't drawn
  public static final long renderPause = 10000;
  
  //map GL_ACTIVE_TEXTURE slots for up to 8 FrameBuffers
  public static final int[] fboTargetSlot = {10, 11, 12, 13, 14, 15, 16, 17};
  public static final int MAX_FBO = 8;
  
  public static final float lineW = 2;
  public static final float fadeTime=0.15f;
  
  //Display, FBOs, Shader
  public static final int xRes = 1280;
  public static final int yRes = 720;
  public static int msaa = 8;
  public static final int maxFPS = 60;
  
  //blur Shader
  public static int glowQuality  = 16;
  public static final float glowStrength = 4;
  public static final int   glowRadius   = 16;
  
  //fxaa Shader
  public static final float FXAA_SPAN_MAX   = 8;
  public static final float FXAA_REDUCE_MUL = 1f/16f;
  public static final float FXAA_REDUCE_MIN = 1f/32f;
  
  //physics stuff
  public static final long PHYS_T_STEP = 10;
  public static final int maxDepth = 64;
  public static final int maxItems = 5;
  
  //Button size
  public static final Vector2f buttonSize = new Vector2f(400f, 100f);
}
