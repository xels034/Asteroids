package glGraphics;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

public class Vertex {
  public Vector2f position;
  public Vector4f color;
  
  public Vertex(Vector2f p){
    position = new Vector2f(p);
  }
  
  public Vertex(Vector2f p, Vector4f c){
    this(p);
    color = new Vector4f(c);
  }
}
