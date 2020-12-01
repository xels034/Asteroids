package components;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * information required for any component, that wants to receive frame-by-frame updates f rom the physics engine
 * 
 */

public interface UpdateComponent extends Component{

  public void update(float dt);
  public double getRemainingLifeTime(long now);
}
