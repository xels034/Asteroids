package glGraphics;

/**
 * 
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Constructs store all necessary data to make a successfull and complete draw call of a particular shape.
 * Ideally they are registered for re-use on the cpu & gpu, reducing overhead. However, they are also used
 * for one-frame-only objects, getting deleted at the end of the frame's draw calls.
 * 
 * Mainly, Constructs possess a set of lines, describing their shape (lines don't have to be connected)
 * Each Line consists of 2 Vertices. Each vertex has a position and a color. As a working HDR environment
 * is present, colors may exceed the typical 0-1 range. Each Construct also has information about position,
 * rotation and scale, separately stored. This data isn't in the form of a matrix, because in the process
 * of translating the default openGL coordinate system into a more familiar screen-pixel coordinate system
 * (origin top-left, 1 unit = 1 pixel) the rotational information gets mirrored. This can be counteracted
 * by inverting the rotation alone, however, it would be impossible in a precalculated matrix. Each Construct
 * finally has a ColorMod. This color gets multiplied by the color of each vertex. With this, each instance
 * of the same Construct can have different colors, as opposed to storing one Construct for each color variation.
 */

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import util.Ref;
import util.SimpleLogger;

public class Construct {
  //actual set of drawn lines
  private LinkedList<Line> lineSet;
  
  //gets drawn in a GL_DYNAMIC_DRAW VAO and isn't registered in the glx
  public boolean dynamic;
  
  //used to determine, if an object can be distarted, because it hasn't been drawn for an amount of time (see Ref.java)
  private long lastRendered;
  
  public Vector2f position;
  public Vector3f scale;
  public float rotation;
  public float lineW;
  
  //needed references of openGL calls, -1 if not yet generated
  private int vaPointer;
  private int vbPointer;
  private int ebPointer;
  private int ebLength;
  
  //same as above, but used for all unregistered Constructs
  private static int vaGeneric = -1;
  private static int vbGeneric = -1;
  private static int ebGeneric = -1;
  private static int ebG_Length= -1;
  
  //glsl in variable positions and shaderProgram idx
  private static int shPosAttrib = -1;
  private static int shColAttrib = -1;
  
  public static ShaderWrapper shader;
  
  public Construct(Collection<Line> cl){
    this();
    setLines(cl);
  }
  
  public Construct(){
    lastRendered = System.currentTimeMillis();
    dynamic = false;
    lineSet  =new LinkedList<>();
    vaPointer = -1;
    vbPointer = -1;
    ebPointer = -1;
    ebLength = -1;
    
    if(shPosAttrib == -1){
      if(shader == null) throw new IllegalStateException("global standardShader hasn't been set yet");
      //construct draw calls only function under the standard line shader (not default ogl shader!)
      shPosAttrib = glGetAttribLocation(shader.getShaderID(), "position");
      shColAttrib = glGetAttribLocation(shader.getShaderID(), "color");
    }
    
    position = new Vector2f(0,0);
    scale = new Vector3f(1,1,1);
    rotation = 0;
    lineW = Ref.lineW;
  }
  
  private void allocateDynamicVAO(){
    //This VAO is dynamically filled with all objects that aren't registered
    vaGeneric = glGenVertexArrays();
    glBindVertexArray(vaGeneric);
    
    vbGeneric = glGenBuffers();
    glBindBuffer(GL_ARRAY_BUFFER, vbGeneric);
    
    ebGeneric = glGenBuffers();
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebGeneric);
    
    glVertexAttribPointer(shPosAttrib, 2, GL_FLOAT, false, 6*(Float.SIZE/8), 0L);
    glEnableVertexAttribArray(shPosAttrib);
    glVertexAttribPointer(shColAttrib, 4, GL_FLOAT, false, 6*(Float.SIZE/8), 2*(Float.SIZE/8));
    glEnableVertexAttribArray(shColAttrib);
  }
  
  public void addLine(Line l){
    lineSet.add(l);
  }
  
  public void setLines(Collection<Line> cl){
    lineSet.addAll(cl);
  }
  
  public void buildLines(Collection<Vector2f> cv, Vector4f color, boolean pairs){
    //exactly the same as with vertices, but a single color is given to each vertex
    LinkedList<Vertex> ll = new LinkedList<>();
    for(Vector2f v : cv){
      ll.add(new Vertex(v, color));
    }
    buildLines(ll, pairs);
  }
  
  public void buildLines(Collection<Vertex> cv, boolean pairs){
    //each vertex can have its own color. if not paired, a loop is formed
    if(pairs){
      if(cv.size()%2 == 1)throw new IllegalArgumentException("Uneven number of vertices, can't make pairs!");
      Iterator<Vertex> iter = cv.iterator();
      Vertex s,e;
      while(iter.hasNext()){
        //takes 2 vertices and puts then in a line
        s = iter.next();
        e = iter.next();
        lineSet.add(new Line(s,e));
      }
    }else{
      if(cv.size() < 2)throw new IllegalArgumentException("Need at least 2 Vertices! ("+cv.size()+" given)");
      Iterator<Vertex> iter = cv.iterator();
      Vertex s,e;
      s=e=null;
      while(iter.hasNext()){
        //one vertex is start and endpoint of 2 lines
        s=e;
        e=iter.next();
        if(s != null & e != null){
          lineSet.add(new Line(s,e));
        }
      }
      //close the loop
      lineSet.add(new Line(e,cv.iterator().next()));
    }
  }
  

  
  public void draw(long ts){
    //glLineWidth(0.3f);
    if(dynamic){
      bakeDynamic();
      glUseProgram(shader.getShaderID());
      glBindVertexArray(vaGeneric);
      glDrawElements(GL_LINES, ebG_Length, GL_UNSIGNED_INT, 0);
    }else{
      if(vaPointer == -1) throw new IllegalStateException("No valid VertexArrayObject assigned.");
      glUseProgram(shader.getShaderID());
      glBindVertexArray(vaPointer);
      glDrawElements(GL_LINES, ebLength, GL_UNSIGNED_INT, 0);
    }
    lastRendered = ts;
  }
  
  public void bake(){
    bakeFixed();
  }
  
  private void bakeFixed(){
    if(lineSet.isEmpty()) throw new IllegalStateException("Construct does not contain any lines at all.");
    glUseProgram(shader.getShaderID());
    
    vaPointer = glGenVertexArrays();
    glBindVertexArray(vaPointer);
    
    vbPointer = glGenBuffers();
    glBindBuffer(GL_ARRAY_BUFFER, vbPointer);
    
    //fill the vertexBuffer and the elementBuffer
    float[] verts = bakeVertexBuffer();
    
    FloatBuffer buff = (ByteBuffer.allocateDirect(verts.length*(Float.SIZE/8)).order(ByteOrder.nativeOrder())).asFloatBuffer();
    buff.put(verts);
    buff.flip();
    glBufferData(GL_ARRAY_BUFFER, buff, GL_STATIC_DRAW);
    
    ebPointer = glGenBuffers();
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebPointer);
    
    int[] elems = bakeElementBuffer();
    ebLength = elems.length;
    
    IntBuffer buffi = (ByteBuffer.allocateDirect(elems.length*(Float.SIZE/8)).order(ByteOrder.nativeOrder())).asIntBuffer();
    buffi.put(elems);
    buffi.flip();
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffi, GL_STATIC_DRAW);
    
    //define attribPositions in each vertexArray
    glVertexAttribPointer(shPosAttrib, 2, GL_FLOAT, false, 6*(Float.SIZE/8), 0L);
    glEnableVertexAttribArray(shPosAttrib);
    glVertexAttribPointer(shColAttrib, 4, GL_FLOAT, false, 6*(Float.SIZE/8), 2*(Float.SIZE/8));
    glEnableVertexAttribArray(shColAttrib);
    
    int err = glGetError();
    if(err != 0) SimpleLogger.log(GLU.gluErrorString(err) + "("+err+")", -1, Construct.class, "bakeFixed");
  }
  
  private void bakeDynamic(){
    if(lineSet.isEmpty()) throw new IllegalStateException("Construct does not contain any lines at all.");
    if(vaGeneric == -1) allocateDynamicVAO();
    
    glBindVertexArray(vaGeneric);
    glUseProgram(shader.getShaderID());
    glBindBuffer(GL_ARRAY_BUFFER, vbGeneric);
    
    float[] verts = bakeVertexBuffer();
    
    FloatBuffer buff = (ByteBuffer.allocateDirect(verts.length*(Float.SIZE/8)).order(ByteOrder.nativeOrder())).asFloatBuffer();
    buff.put(verts);
    buff.flip();
    glBufferData(GL_ARRAY_BUFFER, buff, GL_DYNAMIC_DRAW);
    
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebGeneric);
    
    int[] elems = bakeElementBuffer();
    ebG_Length = elems.length;
    
    IntBuffer buffi = (ByteBuffer.allocateDirect(elems.length*(Float.SIZE/8)).order(ByteOrder.nativeOrder())).asIntBuffer();
    buffi.put(elems);
    buffi.flip();
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffi, GL_DYNAMIC_DRAW);
  }
  
  private float[] bakeVertexBuffer(){
    //per Line: 2 Vertices á 2 pos float & 4 col float
    float[] array = new float[lineSet.size()*2*(2+4)];
    int idx=0;
    for(Line l : lineSet){
      array[idx+0] = l.start.position.x;
      array[idx+1] = l.start.position.y;
      array[idx+2] = l.start.color.x;
      array[idx+3] = l.start.color.y;
      array[idx+4] = l.start.color.z;
      array[idx+5] = l.start.color.w;
      
      idx += (2+4);
      
      array[idx+0] = l.end.position.x;
      array[idx+1] = l.end.position.y;
      array[idx+2] = l.end.color.x;
      array[idx+3] = l.end.color.y;
      array[idx+4] = l.end.color.z;
      array[idx+5] = l.end.color.w;
      
      idx += (2+4);
    }
    
    return array;
  }
  
  private int[] bakeElementBuffer(){
    //one idx per vertex
    int[] array = new int[lineSet.size()*2];
    for(int i=0;i<array.length;i++){
      array[i]=i;
    }
    return array;
  }
  
  public long getLastRendered(){
    return lastRendered;
  }
  
  public void releaseVBO(){
    if(!dynamic){
      SimpleLogger.log("deleting construct", 10, Construct.class, "releaseVBO");
      glDeleteBuffers(vbPointer);
      glDeleteBuffers(ebPointer);
      glDeleteVertexArrays(vaPointer);
    }
  }
  
  public LinkedList<Vector2f> getShape(){
    LinkedList<Vector2f> shape = new LinkedList<>();
    
    for(Line l : lineSet){
      shape.add(l.start.position);
      shape.add(l.end.position);
    }
    
    return shape;
  }
}
