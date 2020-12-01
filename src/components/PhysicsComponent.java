package components;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Information required for any physics capable component
 * 
 */

import org.lwjgl.util.vector.Vector2f;

public interface PhysicsComponent extends UpdateComponent{
  
  public void setPosition(Vector2f p);
  public void addMovement(Vector2f p);
  
  public void setRotation(float r);
  public void rotate(float r);
  
  public float getRotation();
  public Vector2f getPosition();
  
  public void setMass(float m);
  public void setV_Friction(float f);
  public void setR_Friction(float f);
  
  public float getMass();
  public float getV_Friction();
  public float getR_Friction();
  public Vector2f getV_Impulse();
  public float getR_Impulse();
}
