package components;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * This Class is used to store a collection of components and do unsure inter-component communication. Each Component
 * can have different tags. Other components can search for components by those tags. different logic operations for
 * a set of tags can be used (AND, OR, XOR) to offer detailed searches. Tags can be dynamically added and removed any
 * time.
 * 
 * When components are registered in a component, they can't yet be sure that all other needed components are registered aswell.
 * The method linkComponents() sets references to and from the component and the componentContainer. The link() method should be
 * overridden by components to aquire additional information aboutn other components, as it is this stage, wher eit is assured, that
 * all needed components are registered.
 * 
 * Before unregistering, the announceUnregister() method is called, as to give components the time, to access other components while
 * it is guaranteed, that they are still there.
 * 
 */

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class ComponentContainer {

  public enum Logic {AND, OR, XOR}
  
  LinkedList<Component> components;

  public ComponentContainer(){
    components = new LinkedList<>();
  }
  
  public HashSet<Component> getComponents(String tag){
    HashSet<String> hs = new HashSet<>();
    hs.add(tag);
    return getComponents(hs, Logic.OR);
  }
  
  public HashSet<Component> getComponents(Set<String> tags){
    return getComponents(tags, Logic.OR);
  }
  
  public HashSet<Component> getComponents(Set<String> tags, Logic l){
    HashSet<Component> ll = new HashSet<>();

    for(Component co : components){
      int hitCounter = 0;
      for(String s : co.getTags()){
        if(tags.contains(s)) hitCounter++;
      }
      switch(l){
      case AND: if(hitCounter == tags.size()) ll.add(co); break;
      case OR:  if(hitCounter > 0)            ll.add(co); break;
      case XOR: if(hitCounter == 1)       ll.add(co); break;
      }

    }
    return ll;
  }
  
  public void registerComponent(Component c){
    components.add(c);
  }
  
  public void linkComponents(){
    for(Component c : components){
      c.link(this);
    }
  }
  
  public void strip(){
    for(Component c : components){
      c.announceUnregister();
    }
    
    for(Component c : components){
      c.unregisterContainer();
    }
    components.clear();
  }
  
  public void removeComponent(Component c){
    components.remove(c);
    c.announceUnregister();
    c.unregisterContainer();
  }
  
  public ComponentContainer copy(){
    ComponentContainer copy = new ComponentContainer();
    for(Component c : components){
      copy.registerComponent(c.copy(false));
    }
    copy.linkComponents();

    return copy;
  }
  
  public int getComponentCount(){
    return components.size();
  }
}
