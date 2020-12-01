package components;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * It is expected, that any colliding object offers the following informations and methods, as to properly handle
 * any collisions
 */

import java.awt.geom.Rectangle2D;

import org.lwjgl.util.vector.Vector2f;

public interface CollisionComponent extends Component{
  
  public int getLayer();
  public float getLife();
  public float getDamage();
  
  public void applyDmg(float d);
  
  public Vector2f getPosition();
  public float getRadius();
  public float getRadius2();
  public Rectangle2D.Double getSquareBounds();
  
  public void check(long ts);
  public long lastChecked();
  
}
