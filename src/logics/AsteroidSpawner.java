package logics;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Custom-tailored component to spawn a new Asteroid after the ComponentContainer is deemed dead.
 * However, it can spawn any ComponentContainer
 * 
 * When the trigger() is called, 3 copied instances of the toSpawn are created and sent of in different,
 * slightly altered directions each time
 * 
 */

import messaging.Message;
import messaging.Message.ENT;
import messaging.Message.M_TYPE;
import messaging.Messenger;

import org.lwjgl.util.vector.Vector2f;

import components.BasicComponent;
import components.Component;
import components.ComponentContainer;
import components.TriggerComponent;

public class AsteroidSpawner extends BasicComponent implements TriggerComponent{

  private ComponentContainer toSpawn;
  private int count;
  private float radius;
  private float force;
  
  private long lastTriggered;

  
  public AsteroidSpawner(ComponentContainer cc, int c, float r, float f){
    toSpawn = cc;
    count = c;
    radius = r;
    force= f;
    
    tags.add("deathTrigger");
  }
  
  @Override
  public void trigger(long now) {
    
    lastTriggered = now;
    
    PhysicsEntry core = (PhysicsEntry)parent.getComponents("core").iterator().next();
    PhysicsEntry newCore;
    
    ComponentContainer newInstance;
    
    for(int i=0; i< count; i++){
      newInstance = toSpawn.copy();
      newCore = (PhysicsEntry)newInstance.getComponents("core").iterator().next();
      
      float dir = (float)i/(float)count*(float)Math.PI*2;
      dir += Math.random()*Math.PI/2;
      
      Vector2f posOffset = PhysicsEntry.rotate(new Vector2f(radius, 0), dir);
      Vector2f newPosition = new Vector2f();
      Vector2f.add(core.getPosition(), posOffset, newPosition);
      newCore.setPosition(newPosition);
      
      Vector2f impulse = new Vector2f(posOffset);
      impulse.normalise();
      impulse.scale(force*newCore.getMass());

      newCore.addMovement(core.getV_Impulse());
      newCore.addMovement(impulse);
      
      
      Messenger.send(new Message(M_TYPE.ENTITY_MGR, new Message.ENT_Param(newInstance, ENT.ADD)));
    }
  }

  @Override
  public long getLastTriggered() {
    return lastTriggered;
  }

  @Override
  public Component copy(boolean parentInsert) {
    AsteroidSpawner copy = new AsteroidSpawner(toSpawn, count, radius, force);
    copy.tags.clear();
    copy.tags.addAll(tags);
    
    if(parentInsert){
      parent.registerComponent(copy);
      copy.link(parent);
    }
    
    return copy;
  }

  
}
