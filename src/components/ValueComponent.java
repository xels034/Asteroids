package components;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * informations required for any component that may store abriatary data
 * 
 */

public interface ValueComponent {

  public void putValue(String n, float f);
  public float getValue(String n);
}
