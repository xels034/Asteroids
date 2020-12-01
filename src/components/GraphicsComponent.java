package components;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Information that is required of any component that wants to be drawn
 * 
 */

import java.util.UUID;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public interface GraphicsComponent extends Component{

  public UUID getConstructID();
  public Vector2f getPosition();
  public Vector3f getScale();
  public float getRotation();

  public Vector4f getColorMod();
}
