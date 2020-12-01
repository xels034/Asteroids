package glGraphics;

/**
 * 
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * The ScreenBufferWrapper is a special from of a FrameBufferWrapper, specifically created
 * to draw on the screen. Because the screen target can't be configured as a typical texture,
 * special handling is necessary.
 */

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

import org.lwjgl.opengl.GL11;

import util.SimpleLogger;

public class ScreenBufferWrapper extends FrameBufferWrapper {

  public ScreenBufferWrapper(int rs, int rsID){
    super();
    readSlot = rs;
    readTexID = rsID;
  }
  
  @Override
  public void bind(boolean clear){
    if(!ready) throw new IllegalStateException("Framebuffer creation was unsuccessfull! Can't bind");
    
    glActiveTexture(slotMap.get(readSlot));
    glBindTexture(GL11.GL_TEXTURE_2D, readTexID);
    glGenerateMipmap(GL11.GL_TEXTURE_2D);
    shader.storeUniform("fboTex", readSlot);
    
        glUseProgram(shader.getShaderID());
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    if(clear) glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    
  }
  
  @Override
  public void setPipeLine(int rs, int rsid, int ws, int wsid, ShaderWrapper sw){
    readSlot = rs;
    readTexID = rsid;
    shader = sw;
    
    ready = true;
  }
  
  @Override
  public int getWriteTextureID(){
    SimpleLogger.log("Warning: attempting to get texture ID of screenbuffer. Its always 0 and can't be read!", 0, ScreenBufferWrapper.class, "getWriteTextureID");
    return 0;
  }
  
  @Override
  public void releaseFBO(){
    
  }
}
