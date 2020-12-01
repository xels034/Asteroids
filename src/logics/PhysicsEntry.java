package logics;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Heart of most ingame ComponentContainers. This component combines many different interfaces. This is done,
 * because much of its data is needed by more than one interface, so all informations are collected in this
 * component. This is the "core"-tagged component, upon so many other components rely. It is important, that only
 * 1 such component exists, as behavior of other components is undefined when more than 1 "core"-component exists
 * 
 */

import glGraphics.AppWindow;
import glGraphics.Construct;

import java.awt.geom.Rectangle2D;
import java.util.UUID;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import util.Ref;
import components.BasicComponent;
import components.CollisionComponent;
import components.Component;
import components.GraphicsComponent;
import components.PhysicsComponent;
import components.UpdateComponent;

public class PhysicsEntry extends BasicComponent implements PhysicsComponent, GraphicsComponent, CollisionComponent, UpdateComponent{
  
  private int layer;
  private float hp;
  private float dmg;
  
  private UUID shape;
  private Vector4f colorMod;
  private Rectangle2D.Double bounds;
  private float radius;
  
  private Vector2f position;
  private float scale, scaleDelta; //difference of old and new scale, for easily adjusting the shape
  private float rotation;
  
  private float mass;
  private float vFriction;
  private float rFriction;
  
  private Vector2f vImpulse;
  private float rImpulse;
  
  public double lifeTime;
  private long birth;
  //pos change
  //scale change
  private boolean[] updateFlag;
  private long lastUpdate;
  private long lastChecked;
  
  public boolean vectorRotation;
  
  public PhysicsEntry(UUID u, int l, float m, double lt, float h, float d){
    super();
    birth = System.currentTimeMillis();
    lastUpdate = birth;
    
    colorMod = new Vector4f(1,1,1,1);
    
    layer = l;
    hp=h;
    dmg = d;
    
    updateFlag = new boolean[]{true,true,true};
    shape = u;
    calcBounds();
    
    position = new Vector2f(0,0);
    scale = 1f;
    scaleDelta = 1;
    rotation = 0;
    
    mass = m;
    vFriction = 0;
    rFriction = 0;
    
    vImpulse = new Vector2f(0,0);
    rImpulse = 0;
    
    vectorRotation = false;
    
    lifeTime = lt; //input in s, storage as ms
    
    tags.add("physics");
    tags.add("graphics");
    tags.add("collisions");
    tags.add("update");
    tags.add("core");
  }
  
  private void calcBounds(){
    Vector2f min = new Vector2f(0,0);
    Vector2f max = new Vector2f(0,0);
    
    Construct c = AppWindow.glx.getConstruct(shape);
    
    for(Vector2f v : c.getShape()){
      min.x = Math.min(min.x, v.x);
      min.y = Math.min(min.y,  v.y);
      
      max.x = Math.max(max.x,  v.x);
      max.y = Math.max(max.y, v.y);
    }
    
    float xDiff = max.x + min.x;
    float yDiff = max.y - min.y;
    
    radius = (float)Math.hypot(xDiff, yDiff);
    
    bounds = new Rectangle2D.Double(-radius, -radius, radius*2, radius*2);
  }
  
  @Override
    public void update(float dt){

    //dt = 1 friction = 0.1 -> impulse = 0.9
    //dt = 2 friction = 0.1 -> impulse = 0.9*0.9
    
    //rest = 1-friction
    //impulse = rest^dt
    
    float vd = (float)(Math.pow((1-vFriction), dt));
    
    
    vImpulse.x *= vd;
    vImpulse.y *= vd;

    Vector2f imp = new Vector2f(vImpulse.x*dt, vImpulse.y*dt);
    Vector2f.add(position, imp, position);
    
    if(vectorRotation) {
      rotation = getRotation(vImpulse);
    }
    else {
      float rd = (float)(Math.pow((1-rFriction), dt));
      rImpulse *= rd;
      rotation += rImpulse*dt;
    }
    
    lastUpdate = System.currentTimeMillis();
    
    updateFlag[0] = true;
    
  }
  
  @Override
    public void setPosition(Vector2f p){
    position = new Vector2f(p);
    updateFlag[0]=true;
  }
  
  @Override
    public void addMovement(Vector2f p){
    p.x /= mass;
    p.y /= mass;
    Vector2f.add(vImpulse, p, vImpulse);
  }
  
  public void setScale(float s){
    scaleDelta = s/scale;
    scale = s;
    updateFlag[1]=true;
  }
  
  public void scale(float s){
    scaleDelta = s;
    scale *= s;
    updateFlag[1]=true;
  }
  
  @Override
    public void setRotation(float r){
    rotation = r;
  }
  
  @Override
    public void rotate(float r){
    rImpulse += r/mass;
  }
  
  @Override
    public Vector2f getPosition(){
    float gap = (System.currentTimeMillis()-lastUpdate)/1000f;
    
    //extrapolation for display
    Vector2f actPos = new Vector2f(position.x + vImpulse.x*gap,
                     position.y + vImpulse.y*gap);
    return new Vector2f(actPos);
  }
  
  @Override
    public Vector3f getScale(){
    return new Vector3f(scale,scale,scale);
  }
  
  @Override
    public float getRotation(){
    float gap = (System.currentTimeMillis()-lastUpdate)/1000f;
    
    //extrapolation for display

    float actRot = rotation;
    if(!vectorRotation){
      actRot += rImpulse*gap;
    }

    return actRot;
  }
  
  private void updateBounds(){
    if(updateFlag[1]){
      radius *= scaleDelta;
      bounds.width *= scaleDelta;
      bounds.height*= scaleDelta;
      scaleDelta = 1;
      
      updateFlag[1]=false;
    }
    
    if(updateFlag[0]){
      bounds.x = -radius + position.x;
      bounds.y = -radius + position.y;
      updateFlag[0] = false;
    }
  }
  
  @Override
  public void check(long ts){
    lastChecked = ts;
  }
  
  @Override
  public long lastChecked(){
    return lastChecked;
  }
  
  @Override
  public float getRadius(){
    updateBounds();
    return radius;
  }
  
  @Override
  public float getRadius2(){
    updateBounds();
    return radius*radius;
  }
  
  @Override
  public Rectangle2D.Double getSquareBounds(){
    updateBounds();
    return bounds;
  }

  @Override
  public void setMass(float m) {
    mass = m;  
  }

  @Override
  public void setV_Friction(float f) {
    vFriction = f;
  }

  @Override
  public void setR_Friction(float f) {
    rFriction = f;
  }

  @Override
  public float getMass() {
    return mass;
  }

  @Override
  public float getV_Friction() {
    return vFriction;
  }

  @Override
  public float getR_Friction() {
    return rFriction;
  }
  
  @Override
  public Vector2f getV_Impulse(){
    return new Vector2f(vImpulse);
  }
  
  @Override
  public float getR_Impulse(){
    return rImpulse;
  }

  @Override
  public UUID getConstructID() {
    return UUID.fromString(shape.toString());
  }

  @Override
  public Component copy(boolean parentsInsert) {
    PhysicsEntry copy = new PhysicsEntry(shape, layer, mass, lifeTime, hp, dmg);
    
    copy.bounds = (Rectangle2D.Double)bounds.clone();
    copy.position = new Vector2f(position);
    copy.radius = radius;
    copy.rFriction = rFriction;
    copy.rImpulse = rImpulse;
    copy.rotation = rotation;
    copy.scale = scale;
    copy.scaleDelta = scaleDelta;
    copy.updateFlag = updateFlag;
    copy.vectorRotation = vectorRotation;
    copy.vFriction = vFriction;
    copy.vImpulse = new Vector2f(vImpulse);
    copy.colorMod = new Vector4f(colorMod);
    copy.tags.clear();
    copy.tags.addAll(tags);
    
    if(parentsInsert && parent != null){
      copy.lastChecked = lastChecked;
      copy.lastUpdate = lastUpdate;
      copy.birth = birth;
      parent.registerComponent(copy);
      super.link(parent);
    }
    
    return copy;
  }

  @Override
  public double getRemainingLifeTime(long now) {
    if(lifeTime == Double.POSITIVE_INFINITY) return lifeTime;
    else return lifeTime - (now-birth)/1000f;
  }
  

  public void setColorMod(Vector4f cm){
    colorMod = new Vector4f(cm);
  }

  @Override
  public Vector4f getColorMod() {
    long now = System.currentTimeMillis();
    
    float alpha = (float)(Math.max(0, Math.min(1, getRemainingLifeTime(now)/Ref.fadeTime)));
    
    return new Vector4f(colorMod.x,
              colorMod.y,
              colorMod.z,
              colorMod.w * alpha);
  }

  @Override
  public int getLayer() {
    return layer;
  }

  @Override
  public float getLife() {
    return hp;
  }

  @Override
  public float getDamage() {
    return dmg;
  }

  @Override
  public void applyDmg(float d) {
    if(hp != Float.POSITIVE_INFINITY) hp -= d;
  }
  
  public static Vector2f rotate(Vector2f v, float r){
    Vector2f result = new Vector2f();
    
    result.x = (float)(v.x*Math.cos(r)+v.y*Math.sin(r));
    result.y = -(float)(v.x*Math.sin(r)-v.y*Math.cos(r));
    
    return result;
  }
  
  public static float getRotation(Vector2f v){
    return (float)Math.atan2(-v.y, v.x);
  }

}
