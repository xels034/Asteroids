package components;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Information required for any component, that manipulaters the speed/rotation
 * of any physics capable component
 * 
 */

public interface MotorComponent extends UpdateComponent{
  
  public void movement(int i);
  public void rotation(int i);
}
