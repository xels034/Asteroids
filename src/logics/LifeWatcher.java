package logics;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * provides a means for the Game class to display the health of the "core"-tagged component of
 * its ComponentContainer
 * 
 */

import components.BasicComponent;
import components.CollisionComponent;
import components.Component;
import components.ComponentContainer;
import components.StatusComponent;

public class LifeWatcher extends BasicComponent implements StatusComponent{

  private float max;
  
  private String name;
  private String watchTag;
  private CollisionComponent cCore;
  
  public LifeWatcher(String n, String wt){
    name = n;
    watchTag = wt;
    tags.add("status");
  }
  
  @Override
  public void link(ComponentContainer cc){
    super.link(cc);
    
    cCore = (CollisionComponent)parent.getComponents(watchTag).iterator().next();
    max = cCore.getLife();
  }
  
  @Override
  public String getName() {
    return name;
  }

  @Override
  public float getStatus() {
    return cCore.getLife() / max;
  }

  @Override
  public Component copy(boolean parentInsert) {
    LifeWatcher copy = new LifeWatcher(name, watchTag);
    
    copy.tags.clear();
    copy.tags.addAll(tags);
    
    if(parentInsert){
      parent.registerComponent(copy);
      copy.link(parent);
    }
    
    return copy;
  }

}
