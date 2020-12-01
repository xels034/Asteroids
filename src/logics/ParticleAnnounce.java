package logics;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * TriggerComponent, that sends a Particle Create message with the pre-defined particle settings and position
 * of the "core"-tagged component in its Container. It has also 3 pre-defined points at which this component
 * will be triggered by the PhysicsManager. Birth, collision and death
 * 
 */

import java.util.LinkedList;

import messaging.Message;
import messaging.Message.M_TYPE;
import messaging.Messenger;

import org.lwjgl.util.vector.Vector2f;

import particles.ParticleSettings;
import components.BasicComponent;
import components.Component;
import components.ComponentContainer;
import components.PhysicsComponent;
import components.TriggerComponent;

public class ParticleAnnounce extends BasicComponent implements TriggerComponent{

  public static final byte none      = 0; //0000.0000
  public static final byte birth      = 1; //0000.0001
  public static final byte death      = 2; //0000.0010
  public static final byte collision = 4; //0000.0100
  
  public static final int staticRot = 0;
  public static final int impulseRot = 1;
  public static final int orientRot = 2;
  
  private LinkedList<ParticleSettings> settings;
  private Vector2f position;
  private PhysicsComponent core;
  private byte flags;
  private int rotationMode;
  
  private long lastTrigger;
  
  public ParticleAnnounce(LinkedList<ParticleSettings> ps, Vector2f p, byte f, int rm){
    settings = ps;
    flags = f;
    //deliberate referencing for moving anchors
    position = new Vector2f(p);
    rotationMode = rm;
  }
  
  @Override
  public void link(ComponentContainer cc){
    super.link(cc);
    core = (PhysicsComponent)parent.getComponents("core").iterator().next();

    if((flags & birth) == birth)     tags.add("birthTrigger");
    if((flags & collision) == collision) tags.add("collisionTrigger");
    if((flags & death) == death)         tags.add("deathTrigger");
    
  }
  
  @Override
  public void trigger(long now){
    lastTrigger = now;
    for(ParticleSettings ps : settings){
      Messenger.send(new Message(M_TYPE.PARTICLE_CRT, getPositioned_PS_Copy(ps)));
    }
  }
  
  @Override
  public long getLastTriggered() {
    return lastTrigger;
  }
  
  private ParticleSettings getPositioned_PS_Copy(ParticleSettings ps){
    ParticleSettings copy = ps.copy();
    Vector2f pos = new Vector2f(position);
    
    pos = PhysicsEntry.rotate(pos, core.getRotation());
    
    Vector2f.add(pos, core.getPosition(), pos);
     
    float angle = (float)copy.settings.get("angle");
    if(rotationMode == impulseRot) angle += PhysicsEntry.getRotation(core.getV_Impulse());
    if(rotationMode == orientRot)  angle += core.getRotation();

    copy.settings.put("angle", angle);
    copy.settings.put("position", pos);
    copy.settings.put("momentum", core.getV_Impulse());
    return copy;
  }
  
  @Override
  public Component copy(boolean parentInsert) {
    ParticleAnnounce copy = new ParticleAnnounce(settings, position, flags, rotationMode);
    
    copy.tags.clear();
    copy.tags.addAll(tags);
    
    if(parentInsert){
      parent.registerComponent(copy);
      copy.link(parent);
    }
    
    return copy;
  }
}
