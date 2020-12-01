package components;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Methods that any Component needs, used for managing inside of ComponentContainers.
 * See ComponentConainer.java for additional details
 * 
 */

import java.util.TreeSet;

public interface Component{

  public ComponentContainer getComponentContainer();
  
  public TreeSet<String> getTags();
  public void addTag(String t);
  public void removeTag(String t);
  
  public void link(ComponentContainer cc);
  public void announceUnregister();
  public void unregisterContainer();
  public Component copy(boolean parentInsert);
}
