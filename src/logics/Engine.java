package logics;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Manipulates a PhysicsEntry component with the "core" tag
 * Has also a graphical representation. Its color is modified
 * depending on how long the engine is already firing in the
 * forward direction
 * 
 */

import java.util.LinkedList;
import java.util.UUID;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import components.BasicComponent;
import components.Component;
import components.ComponentContainer;
import components.GraphicsComponent;
import components.MotorComponent;
import components.PhysicsComponent;
import components.TriggerComponent;
import components.UpdateComponent;

public class Engine extends BasicComponent implements MotorComponent, GraphicsComponent, UpdateComponent{
  
  private float accStrength;
  private float decStrength;
  private float rotStrength;
  
  private float vFriction;
  private float rFriction;
  
  private int moveState;
  private int rotState;
  
  private float heat;
  
  private UUID constructID;
  private PhysicsComponent core;
  private LinkedList<TriggerComponent> engineEffects;
  
  public Engine(float acc, float dec, float rot, float vf, float rf, UUID conID){
    super();
    
    engineEffects = new LinkedList<>();
    
    accStrength = acc;
    decStrength = dec;
    rotStrength = rot;
    constructID = conID;
    
    vFriction=vf;
    rFriction=rf;
    
    moveState = 0;
    rotState = 0;
    
    heat = 0;
    
    tags.add("motor");
    tags.add("graphics");
    tags.add("update");
  }
  
  @Override
  public void link(ComponentContainer cc) {
    super.link(cc);

    core = (PhysicsComponent)parent.getComponents("core").iterator().next();
    for(Component c : cc.getComponents("engineEffect")){
      engineEffects.add((TriggerComponent)c);
    }
    
    core.setV_Friction(vFriction);
    core.setR_Friction(rFriction);
  }
  
  @Override
  public void update(float dt){  
    if(core != null){
      if(moveState != 0){
        float str;
        if(moveState == 1) {
          str =  accStrength;
          
          
          for(TriggerComponent tc : engineEffects){
            tc.trigger(System.currentTimeMillis());
          }
        }
        else {
          str = -decStrength;
        }
        
        str *= dt;
        
        core.addMovement(PhysicsEntry.rotate(new Vector2f(str, 0), core.getRotation()));
        
        if(heat < 0.5) heat+= 0.3f;
      }
      if(heat > 0) heat -= 0.15f;
      else heat = 0;
      
      
      if(rotState != 0){
        core.rotate(rotStrength*rotState*dt);
      }
    }
  }

  @Override
  public UUID getConstructID() {
    return constructID;
  }

  @Override
  public Vector2f getPosition() {
    return core.getPosition();
  }

  @Override
  public Vector3f getScale() {
    GraphicsComponent gc = (GraphicsComponent)core;
    return gc.getScale();
  }

  @Override
  public float getRotation() {
    return core.getRotation();
  }

  @Override
  public void movement(int i) {
    moveState = (int)Math.signum(i);
  }

  @Override
  public void rotation(int i) {
    rotState = (int)Math.signum(i);
  }
  
  @Override
  public void announceUnregister(){
    core = null;
  }

  @Override
  public Component copy(boolean parentInsert) {
    Engine copy = new Engine(accStrength, decStrength, rotStrength, vFriction, rFriction, constructID);
    
    copy.tags.clear();
    copy.tags.addAll(tags);
    
    copy.moveState = moveState;
    copy.rotState = rotState;
    
    if(parentInsert && parent != null) {
      parent.registerComponent(copy);
      copy.link(parent);
    }
    
    return copy;
  }

  @Override
  public Vector4f getColorMod() {
    return new Vector4f(1+heat*2,1+heat*1f,1+heat*1f, 1);
  }

  @Override
  public double getRemainingLifeTime(long now) {
    return Double.POSITIVE_INFINITY;
  }

}
