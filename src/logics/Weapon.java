package logics;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Is accessed by another component to spawn a new copy of the
 * stored ComponentContainer (its projectiles) The update method
 * implements the firing behaviour, which mimics that of an
 * automatic weapon in this case. It spawns those objects in the
 * direction of the "core"-tagged component in its container and
 * applies a customizable random direction to the shot, to simulate
 * a finite accuracy
 * 
 */

import java.util.Random;
import java.util.UUID;

import messaging.Message;
import messaging.Messenger;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import messaging.Message.ENT;
import messaging.Message.ENT_Param;
import components.BasicComponent;
import components.Component;
import components.ComponentContainer;
import components.GraphicsComponent;
import components.PhysicsComponent;
import components.SpawnComponent;
import components.UpdateComponent;

public class Weapon extends BasicComponent implements GraphicsComponent, SpawnComponent, UpdateComponent{

  private UUID constructID;
  private PhysicsComponent core;

  private ComponentContainer projectilePattern;
  private Vector2f hardPoint;
  private Random r;
  
  private boolean fireing;
  private float rate;
  private float cd;
  private float spread;
  
  private float heat;
  
  public Weapon(UUID u, Vector2f hp, ComponentContainer pp, float r, float sp){
    super();
    
    constructID = u;
    hardPoint = new Vector2f(hp);
    projectilePattern = pp.copy();
    rate=r;
    spread = sp;
    this.r = new Random();
    
    heat=0;
    cd=0;
    fireing = false;
    
    tags.add("graphics");
    tags.add("spawns");
    tags.add("weapon");
    tags.add("update");
  }
  
  @Override
  public void link(ComponentContainer cc) {
    super.link(cc);
    core = (PhysicsComponent) cc.getComponents("core").iterator().next();
  }
  
  @Override
  public void update(float dt) {
    cd-=dt;
    if(fireing && cd <= 0){
      fire();
      cd = 1/rate;
      if(heat < 2)heat+=0.2;
    }else{
      if(heat > 0) heat-=0.02;
    }
  }
  
  protected void fire(){

    ComponentContainer bullet = projectilePattern.copy();
    PhysicsComponent bCore = (PhysicsComponent)bullet.getComponents("core").iterator().next();

    
    
    float rotShift = (float)(r.nextDouble()*Math.PI*2*spread);
    
    Vector2f spawn = PhysicsEntry.rotate(hardPoint, core.getRotation());
    
    Vector2f.add(core.getPosition(), spawn, spawn);
    
    bCore.setPosition(spawn);
    bCore.addMovement(PhysicsEntry.rotate(new Vector2f(0.5f, 0), core.getRotation()+rotShift-spread*(float)Math.PI));
    Vector2f imp = new Vector2f(core.getV_Impulse());

    imp.scale(1-core.getV_Friction());
    imp.scale(bCore.getMass());
    bCore.addMovement(imp);
    
    Messenger.send(new Message(Message.M_TYPE.ENTITY_MGR, new ENT_Param(bullet, ENT.ADD)));
    
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
  public void switchOn() {
    fireing = true;
  }

  @Override
  public void switchOff() {
    fireing = false;
    
  }

  @Override
  public Component copy(boolean parentInsert) {
    Weapon copy = new Weapon(constructID, hardPoint, projectilePattern, rate, spread);
    
    copy.tags.clear();
    copy.tags.addAll(tags);
    
    if(parentInsert && parent != null) {
      parent.registerComponent(copy);
      copy.link(parent);
    }
    
    return copy;
  }

  @Override
  public Vector4f getColorMod() {
    return new Vector4f(1+heat*0.5f,1+heat*0.5f,1+heat*0.5f,1);
  }

  @Override
  public double getRemainingLifeTime(long now) {
    return Double.POSITIVE_INFINITY;
  }


}
