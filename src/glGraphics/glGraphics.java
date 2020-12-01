package glGraphics;

/**
 * 
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * glGraphics is the main hub for collecting draw calls. glGraphics then gets executed by the ApPWindow object
 * and performs the actual openGL commands to draw any geometry.
 * Draw calls are limited to 2 kinds of objects: Lines & Text
 * 
 * glGraphics manages a list of all rendered line-geometry. For rendering text, a third-party solution is used.
 * As the control of how the slick-unit TrueTypeFont makes it's draw calls is severely limited, only rudimentary
 * information is stored.
 * 
 * In the case of line-geometry, it is advised to use as many re-usable objects as possible. For this, pre-build
 * objects can be registered. They are stored in the glGraphics object and also on the graphics card. This data
 * can be re-used each draw call and thus reducing cpu-gpu communication overhead.
 * 
 * Non-registered objects may also be drawn, but this is discouraged. Draw calls are already built around the
 * idea of predefined objects as opposed to loose, unrelated lines. Draw calls expect at least a collection of
 * points to draw a connected line between them. The possibility of drawing "freely" is there, however, predefined
 * objects are the most efficient way of sending draw calls to the gpu. Even draw calls for unregistered and "loose"
 * lines are converted into one-time-only objects, as to send them whole to the gpu, discarding unregistered objects
 * after the frame is drawn completely. Object data ready to be sent to the gpu are stored in so called Constructs.
 * They essentially represent an entity system for renderable objects, together with TrueTyoeFontWrappers.
 * 
 * Draw calls reference a registered object, or force the creation of a one-frame-only object, that gets stored in a
 * LinkedList, determinating the order or draw calls. Eventually, the execute() method is called. It is expected, that
 * the FrameBufferManager is in the correct drawState the execute() is called. The whole drawOrders list is traversed
 * and drawn into the offscreen texture. After drawing, any one-frame-only objects get deleted from the list of known
 * objects, and the workOrder list is cleared.
 * 
 * Any object, that isn't drawn for 10 seconds gets also deleted from the gpu. However, it stays in the list of registered
 * constructs. THis entry has to be deleted manually by any draw caller. When a registered object gets deleted, the corresponding
 * VertexArrayObject on the gpu is also deleted. This prevents a memory leak on the cpu aswell as on the gpu, when the same
 * objects may get recreated in the course of multiple game sessions over a very
 * long period of time.
 * 
 * As the PhysicsManager, that runs on a different thread, may also need information stored in registered constructs, there are
 * synchronized waith methods, to unsure that any deleting may only occur if any object needing crucial information has released
 * the glGraphics object. However, this has to be done manually
 * 
 * glGraphics has also the ability to load constructs directly from files. This approach should be the default as to not clutter the code
 * with manual construct creation
 */


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import util.Ref;
import static org.lwjgl.opengl.GL11.*;

public class glGraphics {

  private class ConstructEntry{
    private UUID pointer;
    private Matrix4f model;
    private Vector4f colorMod;
    
    public ConstructEntry(UUID uuid, Matrix4f m, Vector4f cm){
      pointer = uuid;
      model = new Matrix4f(m);
      colorMod = new Vector4f(cm);
    }
  }
  
  private class TextEntry{
    private Vector2f position;
    private Vector4f color;
    private String text;
    
    public TextEntry(Vector2f p, Vector4f c, String t){
      position = new Vector2f(p);
      color = new Vector4f(c);
      text = t;
    }
  }

  private Matrix4f viewMat;
  
  //holds all registered constructs if not explicitly freed
  private HashMap<UUID, Construct> assets;
  //holds all assets that are currently on the gpu
  private ArrayList<UUID> gpuLoaded;
  private LinkedList<UUID> toRelease;
  //linked list of construct idx and separate matrix
  private LinkedList<ConstructEntry> workOrders;
  private LinkedList<TextEntry> textOrders;
  private TrueTypeFontWrapper font;
  private ShaderWrapper primaryShader;

  private boolean wait;
  
  public glGraphics(ShaderWrapper sw){
    wait = false;
    
    assets = new HashMap<>();
    gpuLoaded = new ArrayList<>();
    toRelease = new LinkedList<>();
    
    workOrders = new LinkedList<>();
    textOrders = new LinkedList<>();
    font = new TrueTypeFontWrapper("res/fonts/alienleague.ttf", 28f);
    primaryShader = sw;
    
    viewMat = new Matrix4f();
    viewMat.translate(new Vector2f(-1,1));
    viewMat.scale(new Vector3f(1f/(Ref.xRes/2), -1f/(Ref.yRes/2), 1));
    
    glDisable(GL_DEPTH_TEST);
    glEnable(GL_LINE_SMOOTH);
  }
  
  public UUID registerConstruct(Construct c){
    UUID u = UUID.randomUUID();
    assets.put(u, c);
    return u;
  }
  
  public UUID loadConstruct(String fn){
    Construct c = new Construct();
    BufferedReader br;
    try {
      br = new BufferedReader(new FileReader(fn));
      String line;
      String lc[];
      
      c.lineW = Float.parseFloat(br.readLine().split("=")[1]);
      
      boolean pairs = Boolean.parseBoolean(br.readLine().split("=")[1]);
      boolean vertexMode = Boolean.parseBoolean(br.readLine().split("=")[1]);
      
      if(vertexMode){
        LinkedList<Vertex> ll = new LinkedList<>();
        line = br.readLine();
        while(line != null){
          lc = line.split(" ");
          ll.add(new Vertex(new Vector2f(Float.parseFloat(lc[0]),
                           Float.parseFloat(lc[1])),
                    new Vector4f(Float.parseFloat(lc[2]),
                             Float.parseFloat(lc[3]),
                             Float.parseFloat(lc[4]),
                             Float.parseFloat(lc[5]))));
          
          line = br.readLine();
        }
        
        c.buildLines(ll, pairs);
        
      }else{
        lc = br.readLine().split(" ");
        Vector4f color = new Vector4f(Float.parseFloat(lc[0]),
                        Float.parseFloat(lc[1]),
                        Float.parseFloat(lc[2]),
                        Float.parseFloat(lc[3]));
        LinkedList<Vector2f> ll = new LinkedList<>();
        line = br.readLine();
        while(line != null){
          
          lc = line.split(" ");
          ll.add(new Vector2f(Float.parseFloat(lc[0]),
                    Float.parseFloat(lc[1])));
          
          line = br.readLine();
        }
        
        c.buildLines(ll, color, pairs);
      }
      
      
      br.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return registerConstruct(c);
  }
  
  public Construct getConstruct(UUID u){
    Construct c = assets.get(u);
    if(c == null) throw new OpenGLException("no such construct registered");
    return assets.get(u);
  }
  
  public void releaseConstruct(UUID u){
    toRelease.add(u);
  }
  
  private void gpuUpload(UUID u){
    assets.get(u).bake();
    gpuLoaded.add(u);
  }
  
  public void drawConstruct(UUID u){
    drawConstruct(u, new Vector4f(1,1,1,1));
  }
  
  public void drawConstruct(UUID idx, Vector4f colorMod){
    //derives the matrix from the loc/rot/scale attributes of the construct
    if(idx == null) throw new IllegalArgumentException("Error: idx is null!");
    if(!assets.containsKey(idx)) throw new IllegalArgumentException("No construct with idx="+idx+" registered");
    if(!gpuLoaded.contains(idx)) gpuUpload(idx);

    Matrix4f m = new Matrix4f();
    Construct c = assets.get(idx);
    
    m.translate(c.position);
    
    // - rotation, because through the y-axis mirroring, the rotational direction changed
    Matrix4f.mul(m, new Matrix4f().rotate(-c.rotation, new Vector3f(0,0,1)), m);
    Matrix4f.mul(m, new Matrix4f().scale(c.scale), m);
    
    workOrders.add(new ConstructEntry(idx, m, colorMod));
  }
  
  @Deprecated
  public void drawConstruct(UUID idx, Matrix4f m, Vector4f colorMod){
    if(idx == null) throw new IllegalArgumentException("Error: idx is null!");
    if(!assets.containsKey(idx)) throw new IllegalArgumentException("No construct with idx="+idx+" registered");
    if(!gpuLoaded.contains(idx)) gpuUpload(idx);

    workOrders.add(new ConstructEntry(idx, m, colorMod));
  }
  
  public void drawLines(Collection<Vector2f> cv, Vector4f color, boolean pairs){
    drawLines(cv, color, Ref.lineW, pairs);
  }
  
  public void drawLines(Collection<Vertex> cv, boolean pairs){
    drawLines(cv, Ref.lineW, pairs);
  }
  
  public void drawLines(Collection<Vector2f> cv, Vector4f color, float lw, boolean pairs){
    Construct c = new Construct();
    c.buildLines(cv, color, pairs);
    c.lineW = lw;
    c.dynamic = true;
    UUID k = UUID.randomUUID();
    assets.put(k, c);
    
    workOrders.add(new ConstructEntry(k, new Matrix4f(), new Vector4f(1,1,1,1)));
    
  }
  
  public void drawLines(Collection<Vertex> cv, float lw, boolean pairs){
    Construct c = new Construct();
    c.buildLines(cv, pairs);
    c.lineW = lw;
    c.dynamic = true;
    UUID k = UUID.randomUUID();
    assets.put(k, c);
    
    workOrders.add(new ConstructEntry(k, new Matrix4f(), new Vector4f(1,1,1,1)));

  }
  
  public void drawText(float x, float y, String t, Vector4f color){
    textOrders.add(new TextEntry(new Vector2f(x,y), color, t));
  }
  
  public void execute(){
    Matrix4f pvm = new Matrix4f();
    
    //check if lineWIdth or colorMod have to be set anew
    //skipping uniform storage greatly increases performance
    //doing this for modelMatrix seems unnecessary, as it is
    //expected to be different for every single workOrder
    float lw=0;
    Vector4f cm = new Vector4f(0,0,0,0);    
    long now = System.currentTimeMillis();

    for(ConstructEntry ce : workOrders){
      
      Matrix4f.mul(viewMat, ce.model, pvm);
      primaryShader.storeUniform("pvm", pvm);
      
      if(!cm.equals(ce.colorMod)){
        primaryShader.storeUniform("brightness", ce.colorMod);
        cm = ce.colorMod;
      }

      if(lw != assets.get(ce.pointer).lineW){
        lw = assets.get(ce.pointer).lineW;
        glLineWidth(lw);
      }
      
      assets.get(ce.pointer).draw(now);
    }
    
    for(TextEntry te : textOrders){
      font.drawText(te.position.x, te.position.y, te.text, te.color, primaryShader);
    }
    cleanUp(now);
  }
  
  private void cleanUp(long now){
    workOrders.clear();
    textOrders.clear();
    
    LinkedList<UUID> dynamics = new LinkedList<>();
    LinkedList<UUID> olds = new LinkedList<>();
    
    for(UUID u : assets.keySet()){
      if(assets.get(u).dynamic) dynamics.add(u);
    }
    for(UUID u : dynamics){
      assets.remove(u);
    }

    for(UUID u : gpuLoaded){
      long gap = now - assets.get(u).getLastRendered();
      if(gap > Ref.renderPause) olds.add(u);
    }

    for(UUID u: olds){
      assets.get(u).releaseVBO();
      gpuLoaded.remove(u);
    }
    
    for(UUID u : toRelease){
      assets.get(u).releaseVBO();
      gpuLoaded.remove(u);
      assets.remove(u);
    }
    toRelease.clear();
  }
  
  
  public int getTextWidth(String t){
    return font.getTextWidth(t);
  }
  
  public int getTextHeight(String t){
    return font.getTextHeight(t);
  }
  
  public void deconstructAll(){
    gpuLoaded.clear();
    for(Construct c : assets.values()){
      c.releaseVBO();
    }
    assets.clear();
  }
  
  public boolean isLocked(){
    return wait;
  }
  
  public synchronized void takeLock(){
    wait = true;
  }
  
  public synchronized void releaseLock(){
    wait = false;
    notifyAll();
  }
  
  public synchronized void waitForRelease(){
    if(wait)
      try {
        wait();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
  }
}
