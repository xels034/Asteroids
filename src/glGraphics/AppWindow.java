package glGraphics;

/**
 * 
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * AppWindow is the root of the whole application.
 * the main method sits here, and calls a new instance of AppWindow
 * 
 * AppWindow sets up (1) all the openGL stuff and (2) the whole game mechanic
 * rendering and updating every part of the game happens in AppWindows run() method
 * 
 * 
 * 
 * [1]Rendering:
 * Rendering happens in multiple passes. First, the scene is drawn into an offscreen texture.
 * Then follow any number of post-process passes, until the image is finally drawn onto the screen.
 * By using offscreen textures, the use of different brightnesses than 0-1 is possible. This represents
 * effectively a very rudimentary HDR-pipeline. Managing the selection of the correct render targets
 * is the duty of the FrameBufferManager
 * 
 * When the correct FrameBuffer is selected, actual draw calls (instead of just fullscreen post-process shaders)
 * are allowed. Those draw calls are collected by the public static glGraphics glx. It is set up before any java
 * game logic objects are created, and so it is assured, that all access to it happen after its creation.
 * 
 * After collecting all draw calls, its list gets executed, preparing the FrameBufferManager for any post-process passes
 * Each of these passes accepts a shader. It is expected, that this shader draws a screen-aligned quad and reads from the current
 * offscreen image. All that has to be done is to set any variables that the shader needs. Shaders are collected in a map,
 * as to easily give them names in the code.
 * 
 * 
 * 
 * [2]Updating:
 * Each frame the InputHandler (for listening for input), the FSM and the Messenger are updated, to process another batch of data
 * The FSM manages to update the correct state, e.g. Menu or Game. The ingame physics, however, are not updated by this main loop
 * (see PhysicsManager for additional info)
 */

import states.FSM;
import states.Game;
import states.GameOver;
import states.Menu;
import states.QuitScreen;
import states.SplashScreen;
import states.State;
import util.Ref;
import util.SimpleLogger;

import java.util.HashMap;

import logics.InputHandler;
import messaging.Handler;
import messaging.Message;
import messaging.Message.STATE;
import messaging.Messenger;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

public class AppWindow implements Handler{

  public static void main(String[] args) {

    AppWindow w = new AppWindow();
    if(w.ready) w.run();
    else{
      Menu.writeOptions(new String[]{"OFF", "OFF"});
      SimpleLogger.log("AppWindow Setup failed :( can't start application.", -2, AppWindow.class, "main");
    }
    
    Messenger.unsubscribe(w);
  }
  
  
  //Render Stuff
  private HashMap<String, ShaderWrapper> shaders;
  public static glGraphics glx;
  private FrameBufferManager fbm;
  private InputHandler ip;
  
  private boolean ready;
  private boolean closeRequest;
  
  private long stamp;
  private long frames;
  //##########
  
  //Game Logic Stuff
  private FSM mainFSM;
  //################
  
  
  public AppWindow(){
    ready = false;
    closeRequest = false;
    
    try{ setupGL(); }
    catch (LWJGLException e) {e.printStackTrace();}
    
    if(ready) setupJAVA();
  }
  
  private void setupGL() throws LWJGLException{
    setupDisplay();
    setupShader();
    fbm = new FrameBufferManager(3,shaders.get("fxaa"));
    glx = new glGraphics(shaders.get("line"));

    ready = true;
  }
  
  private void setupJAVA(){
    Messenger.subscribe(this, Message.M_TYPE.CHANGE_STATE);
    
    State menu,game,splashScreen, quitScreen, gameOver;
    
    ip = new InputHandler();

    mainFSM = new FSM(false);
    menu = new Menu(false);
    game = new Game(false);
    splashScreen = new SplashScreen(false);
    quitScreen = new QuitScreen(false);
    gameOver = new GameOver(false);
    
    mainFSM.setStartEnd(splashScreen, quitScreen);
    mainFSM.registerState(menu);
    mainFSM.registerState(game);
    mainFSM.registerState(gameOver);
    mainFSM.registerState(quitScreen);
    
    mainFSM.registerTransition(splashScreen, STATE.MENU, menu);
    mainFSM.registerTransition(menu, STATE.GAME, game);
    mainFSM.registerTransition(menu, STATE.QUIT_SCREEN, quitScreen);
    mainFSM.registerTransition(game, STATE.GAME_OVER, gameOver);
    mainFSM.registerTransition(gameOver, STATE.MENU, menu);
    mainFSM.registerTransition(quitScreen, STATE.MENU, menu);
    
    mainFSM.activate();

  }

  private void setupDisplay() throws LWJGLException{
    
    Display.setDisplayMode(new DisplayMode(Ref.xRes,Ref.yRes));
    Display.setResizable(false);
    
    Menu.getGraficOptions();
    int msaa = Math.min(GL_MAX_SAMPLES, Ref.msaa);

                        //bpp,a, z, stenc, multisample
    PixelFormat pf = new PixelFormat(8, 8, 8, 8, msaa);
    Display.create(pf, new ContextAttribs());
    
    glEnable(GL_MULTISAMPLE);
    glEnable(GL11.GL_TEXTURE_2D);
    glEnable(GL11.GL_BLEND);
    glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    glEnable(GL_LINE_SMOOTH);
  }
  
  private void setupShader() {
    shaders = new HashMap<>();
    
    String lineVsh    = "res/shader/line/line.vsh";
    String lineFsh    = "res/shader/line/line.fsh";
    String sa_quadVsh = "res/shader/shared/sa_quad.vsh";
    String comb       = "res/shader/glare/combine.fsh";
    String fxaaFsh    = "res/shader/fxaa/fxaa.fsh";
    String blankFsh   = "res/shader/fxaa/blank.fsh";
    String vb         = "res/shader/glare/vectorBlur.fsh";

    shaders.put("line" , new ShaderWrapper(lineVsh,    lineFsh));
    shaders.put("vectorBlur", new ShaderWrapper(sa_quadVsh, vb));
    shaders.put("combine", new ShaderWrapper(sa_quadVsh, comb));
    shaders.put("fxaa" , new ShaderWrapper(sa_quadVsh, fxaaFsh));
    shaders.put("blank", new ShaderWrapper(sa_quadVsh, blankFsh));
    
    Construct.shader = shaders.get("line");
    
    setupUniforms();
  }
  
  private void setupUniforms(){
    ShaderWrapper line  = shaders.get("line");
    ShaderWrapper comb = shaders.get("combine");
    ShaderWrapper fxaa  = shaders.get("fxaa");
    ShaderWrapper vb = shaders.get("vectorBlur");
    
    line.storeUniform("brightness", new Vector4f(1,1,1,1));

    fxaa.storeUniform("fboTex",         Ref.fboTargetSlot[1]);
    fxaa.storeUniform("texcoordOffset",  new Vector2f(1f/Ref.xRes, 1f/Ref.yRes));
    fxaa.storeUniform("FXAA_SPAN_MAX",   Ref.FXAA_SPAN_MAX);
    fxaa.storeUniform("FXAA_REDUCE_MUL", Ref.FXAA_REDUCE_MUL);
    fxaa.storeUniform("FXAA_REDUCE_MIN", Ref.FXAA_SPAN_MAX);
    
    comb.storeUniform("glare", 2);
    comb.storeUniform("original", 0);
    
    vb.storeUniform("fboTex",  Ref.fboTargetSlot[0]);
    vb.storeUniform("texDim", new Vector2f(Ref.xRes, Ref.yRes));
    vb.storeUniform("quality",   Ref.glowQuality);
    vb.storeUniform("strength",  Ref.glowStrength);
    vb.storeUniform("dir", new Vector2f(Ref.glowRadius, 0));
  }
  
  private void run(){
    while(!Display.isCloseRequested() && !closeRequest){
      update();
      render();
      Display.update();
      Display.sync(Ref.maxFPS);
    }
    
    cleanUp();
  }
  
  private void update(){
    updateFPSCounter();
    ip.update();
    mainFSM.update();
    Messenger.update();
  }
  
  private void updateFPSCounter(){
    if(System.currentTimeMillis() - stamp > 1000){
      Display.setTitle("Asteroids  ||  FPS: "+frames);
      frames = 0;
      stamp = System.currentTimeMillis();
    }
    frames++;
  }
  
  private void render(){
    //draw geometry to target 0
    fbm.setSourceRender(shaders.get("line"));
    //finalize draws all glx orders
    fbm.finalizeSource();
    if(Ref.glowQuality > 0){
      shaders.get("vectorBlur").storeUniform("quality", Ref.glowQuality);
      //get narrow blur on target2
      //H = 0 -> 1
      //V = 1 -> 2
      shaders.get("vectorBlur").storeUniform("dir", new Vector2f(Ref.glowRadius, 0));
      shaders.get("vectorBlur").storeUniform("strength", Ref.glowStrength*0.55f);
      fbm.doCustomPostPro(0, 1, shaders.get("vectorBlur"));
      shaders.get("vectorBlur").storeUniform("dir", new Vector2f(0, Ref.glowRadius));
      fbm.doCustomPostPro(1, 2, shaders.get("vectorBlur"));
  
      //apply sum on target 1
      //C = 0 + 2 -> 1
      shaders.get("combine").storeUniform("original", Ref.fboTargetSlot[0]);
      shaders.get("combine").storeUniform("glare", Ref.fboTargetSlot[2]);
      fbm.doCustomPostPro(2, 1, shaders.get("combine"));
      
      //get broad blur on target2
      //H = 1 -> 0
      //V = 0 -> 2
      shaders.get("vectorBlur").storeUniform("dir", new Vector2f(Ref.glowRadius*8, 0));
      shaders.get("vectorBlur").storeUniform("strength", Ref.glowStrength/2.7f);
      fbm.doCustomPostPro(1, 0, shaders.get("vectorBlur"));
      shaders.get("vectorBlur").storeUniform("dir", new Vector2f(0, Ref.glowRadius*8));
      fbm.doCustomPostPro(0, 2, shaders.get("vectorBlur"));
  
      //apply sum on target 0
      //C = 1 + 2 -> 0
      shaders.get("combine").storeUniform("original", Ref.fboTargetSlot[1]);
      shaders.get("combine").storeUniform("glare", Ref.fboTargetSlot[2]);
      fbm.doCustomPostPro(2, 0, shaders.get("combine"));
      
      //get horizontal lensFlare on target 1
      shaders.get("vectorBlur").storeUniform("dir", new Vector2f(Ref.glowRadius*8, 0));
      shaders.get("vectorBlur").storeUniform("strength", Ref.glowStrength/10f);
      fbm.doCustomPostPro(0, 1, shaders.get("vectorBlur"));
      
      //apply sum on target 2
      shaders.get("combine").storeUniform("original", Ref.fboTargetSlot[0]);
      shaders.get("combine").storeUniform("glare", Ref.fboTargetSlot[1]);
      fbm.doCustomPostPro(1, 2, shaders.get("combine"));
      fbm.doCustomPostPro(2, 1, shaders.get("blank"));
      //fxaa from last written target(2)
      fbm.finalizeImage(shaders.get("fxaa"));
    }else{
      fbm.finalizeImage(shaders.get("blank"));
    }

  }
  
  private void cleanUp(){
    mainFSM.deactivate();
    
    glx.waitForRelease();
    
    glx.deconstructAll();
    fbm.releaseAll();
    for(ShaderWrapper sw : shaders.values()){
      sw.releaseShader();
    }
    shaders.clear();
  }

  @Override
  public void handleMessage(Message m) {
    if((Message.STATE)m.getParam() == STATE.CLOSE_GAME) closeRequest = true;
  }
}