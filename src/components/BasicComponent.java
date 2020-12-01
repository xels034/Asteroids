package components;

/**
 * 
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Basic implementation of common methods of any Component
 * 
 */

import java.util.TreeSet;

public abstract class BasicComponent implements Component{
  
  protected ComponentContainer parent;
  protected TreeSet<String> tags;
  
  public BasicComponent(){
    tags = new TreeSet<>();
  }
  
  @Override
  public ComponentContainer getComponentContainer(){
    return parent;
  }
  
  @Override
    public TreeSet<String> getTags(){
    TreeSet<String> ts = new TreeSet<>();
    ts.addAll(tags);
    return ts;
  }
  
  @Override
    public void addTag(String t){
    tags.add(t);
  }
  
  @Override
    public void removeTag(String t){
    tags.remove(t);
  }
  
  @Override
    public void link(ComponentContainer cc){
    parent = cc;
  }
  
  @Override
    public void announceUnregister(){
    
  }
  
  @Override
    public void unregisterContainer(){
    parent = null;
  }
}
