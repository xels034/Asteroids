package logics;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Updates all physically simulated objets in the game. This manager runs in a seperate frame, so that framerate and
 * update rate are independant, typically allowing higher framerates while physics updates take longer
 * 
 * Whenever the addition/removal/update of ComponentContainers is messaged, their components get placed into different
 * lists, corresponding to certain tags.
 * 
 * "update"-tagged component get their update method called and get checked if their
 * lifetime is over.
 * 
 * "physics"-tagged components get their position checked. If they run out of the screen, their coordinates are reverted back into
 * the screen ( % operator)
 * 
 * "collisions"-tagged components are then checked if they are colliding. Any resulting damage is applied, and any "collisionTrigger"-tagged
 * components are fired. If they are also a PhysicsEntry, collision forces are applied. As asteroids have greatly varying sizes, and as particles
 * may be able to collide with other objects, a QuadTree is used to speed up the collision-checking. Only circle-shaped collision shapes are supported
 * right now.
 * 
 * Messaging is done by a ConcurrentLinkedQueue object, as the multi-threaded approach makes it neccesary, that removal/addition of objects are done only
 * by the Worker thread to ensure they are done at the right point in the updateCycle.
 * 
 */

import glGraphics.AppWindow;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.util.vector.Vector2f;

import particles.ParticleCreator;
import particles.ParticleSettings;
import components.CollisionComponent;
import components.Component;
import components.ComponentContainer;
import components.ComponentContainer.Logic;
import components.PhysicsComponent;
import components.TriggerComponent;
import components.UpdateComponent;
import messaging.Handler;
import messaging.Message;
import messaging.Message.ENT;
import messaging.Message.ENT_Param;
import messaging.Message.M_TYPE;
import messaging.Message.UPD_Param;
import messaging.Messenger;
import util.Ref;
import util.SimpleLogger;

public class PhysicsManager implements Handler{

  private class Worker extends Thread{
    @Override
    public void run(){
      SimpleLogger.log("PhM Worker start cycle", 1, this.getClass(), "run");
      while(doCycle){
        updateCycle();
      }

      physics.clear();
      toAdd.clear();
      toRemove.clear();
      qt.clear();

      SimpleLogger.log("PhM Worker out of cycle", 1, this.getClass(), "run");
    }
  }
  
  private static HashSet<String> addTags;
  
  private QuadTree qt;
  private ParticleCreator pcr;
  private Worker mThread;
  
  private ConcurrentLinkedQueue<Message> messages;
  
  private List<Component> toAdd;
  private List<Component> toRemove;
  private List<ComponentContainer> toStrip;
  
  private List<UpdateComponent> updates;
  private List<PhysicsComponent> physics;
  private List<CollisionComponent> collider;
  
  private boolean doCycle;
  private long lastUpdate;
  
  private float updateSum;
  private int updateCount;
  private int tps;
  
  private long now;
  
  public PhysicsManager(){
    qt = new QuadTree(new Rectangle2D.Double(0, 0, 1280, 720), Ref.maxDepth, Ref.maxItems, 0);
    pcr = new ParticleCreator();
    
    physics = new ArrayList<>();
    toAdd = new ArrayList<>();
    toRemove = new ArrayList<>();
    updates = new ArrayList<>();
    collider = new ArrayList<>();
    toStrip = new ArrayList<>();
    messages = new ConcurrentLinkedQueue<>();
    
    updateCount=0;
    updateSum = 0;
    
    if(addTags == null){
      addTags = new HashSet<>();
      addTags.add("physics");
      addTags.add("update");
      addTags.add("collisions");
    }
  }
  
  @SuppressWarnings("unchecked")
  public static Set<String> getTags(){
    return (Set<String>)addTags.clone();
  }
  
  public void start(){
    Messenger.subscribe(this, Message.M_TYPE.ENTITY_MGR);
    Messenger.subscribe(this, Message.M_TYPE.PARTICLE_CRT);
    Messenger.subscribe(this, Message.M_TYPE.ENTITY_UPD);
    
    doCycle = true;
    mThread = new Worker();
    mThread.start();
  }
  
  public void stop(){
    doCycle=false;
    Messenger.unsubscribe(this);
  }
  
  public void updateCycle(){
    AppWindow.glx.takeLock();
    
    handleMessageThreaded();
    float dt = sleepToUpdate();
    updateLists();
    updatePhysics(dt);
    updateParticles();
    updateTPS(dt);
    
    AppWindow.glx.releaseLock();
  }
  
  private float sleepToUpdate(){
    now = System.currentTimeMillis();
    
    long sleepTime = Ref.PHYS_T_STEP - (now-lastUpdate);
    if(sleepTime > 0){
      try {
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      now = System.currentTimeMillis();
      
    }
    
    float dt = (now-lastUpdate);
    lastUpdate = now;
    
    //keep dt as seconds for easer handling
    dt /= 1000f;
    return dt;
  }
  
  private void updateLists(){
    physics.removeAll(toRemove);
    updates.removeAll(toRemove);
    collider.removeAll(toRemove);
    
    for(ComponentContainer cc : toStrip){
      cc.strip();
    }
    
    
    
    for(Component c : toAdd){
      if(c.getTags().contains("update"))     updates.add((UpdateComponent)c);
      if(c.getTags().contains("physics"))    physics.add((PhysicsComponent)c);
      if(c.getTags().contains("collisions")) collider.add((CollisionComponent)c);
    }
    
    toRemove.clear();
    toStrip.clear();
    toAdd.clear();
  }
  
  private void updatePhysics(float dt){
    Vector2f pos;
    for(UpdateComponent uc : updates){
      uc.update(dt);
      if(uc.getRemainingLifeTime(now) <= 0) {
        handleDeadComponent(uc);
      }
    }
    
    for(PhysicsComponent e : physics){
      //ensuring objects appear on the other side of the screen
      pos = e.getPosition();
      e.setPosition(new Vector2f((pos.x+Ref.xRes) % Ref.xRes,
                     (pos.y+Ref.yRes) % Ref.yRes));
    }
    updateCollisions(now);
  }
  
  private void updateCollisions(@SuppressWarnings("hiding") long now){
    //rebuild quadTree
    qt.clear();
    for(CollisionComponent c : collider){
      qt.insert(c);
    }
    
    LinkedList<CollisionComponent> ll;
    Vector2f distance = new Vector2f(0,0);
    
    for(CollisionComponent candidate : collider){
      ll = qt.getCandidates(candidate.getSquareBounds());
      
      for(CollisionComponent test : ll){
        //test only if different layers, or some is layer-1, as this collides with everything
        if(candidate != test &&
           test.lastChecked() != now &&
           (candidate.getLayer() != test.getLayer() || candidate.getLayer() == -1 || test.getLayer() == -1)){
          
          //how far apart are the candidates
          Vector2f.sub(test.getPosition(), candidate.getPosition(), distance);
          float d2 = distance.x*distance.x + distance.y*distance.y;
          
          if(candidate.getRadius2()+test.getRadius2() > d2){
            //they are colliding, now:
            resolveCollision(candidate, test);
          }
        }
      }
      //candidate has been checked against all possible objects, it is checked for this frame, skipping all other checks against it
      candidate.check(now);
    }
  }
  
  private void resolveCollision(CollisionComponent cpA, CollisionComponent cpB){
    LinkedList<Component> ctList = new LinkedList<>();
    
    //do dmg
    cpA.applyDmg(cpB.getDamage());
    cpB.applyDmg(cpA.getDamage());
    
    //certain components may not want to invoke the trigger of the object colliding with
    //(e.g. particle shouldn't create a new particle cascade)
    if(cpA.getComponentContainer().getComponents("noTrigger").isEmpty() &&
       cpB.getComponentContainer().getComponents("noTrigger").isEmpty()){
      
      //fire any collisionTrigger components
      ctList.clear();
      ctList.addAll(cpA.getComponentContainer().getComponents("collisionTrigger"));
      ctList.addAll(cpB.getComponentContainer().getComponents("collisionTrigger"));
      TriggerComponent tc;
      
      for(Component co : ctList){
        //((TriggerComponent)tc).trigger(now);;
        tc = (TriggerComponent)co;
        if(!co.getTags().contains("noTrigger")) tc.trigger(now);
      }
    }
    //see if anythings dead yet
    if(cpA.getLife() <= 0) handleDeadComponent(cpA);
    if(cpB.getLife() <= 0) handleDeadComponent(cpB);

    //if something survived, apply forces
    if(cpA.getLife() > 0 || cpB.getLife() > 0){
      //both susceptible to forces?
        if(cpA instanceof PhysicsEntry && cpB instanceof PhysicsEntry){
        applyCollisionForce((PhysicsEntry)cpA, (PhysicsEntry)cpB);
        }else{
          //TODO collisionHandler objects, handling more than just physObj<=>physObj
        }
    }
  }
  
  private void handleDeadComponent(Component c){
    LinkedList<Component> ctList = new LinkedList<>();
    ctList.addAll(c.getComponentContainer().getComponents("deathTrigger"));
    TriggerComponent tc;
    for(Component lc : ctList){
      tc = (TriggerComponent)lc;
      if(tc.getLastTriggered() != now) tc.trigger(now);
    }

    //could wait on message, but that might keep the obj alive 1 frame longer,
    //which can have issues (e.g. particle systems fire 2 times)
    
    if(!toRemove.contains(c)){
      toRemove.add(c);
      Messenger.send(new Message(M_TYPE.ENTITY_MGR, new ENT_Param(c.getComponentContainer(), ENT.REMOVE)));
    }

  }
  
  private void applyCollisionForce(PhysicsEntry peA, PhysicsEntry peB){
    Vector2f distance = new Vector2f();
    Vector2f relVel = new Vector2f();
    
    Vector2f.sub(peB.getPosition(), peA.getPosition(), distance);
    Vector2f normal = new Vector2f(distance);
    if(normal.lengthSquared() > 0) normal.normalise();
    
    Vector2f.sub(peB.getV_Impulse(), peA.getV_Impulse(), relVel);
    float velAlongNormal = Vector2f.dot(relVel, normal);
    
    //is it > 0, they are already moving apart
    if(velAlongNormal < 0){
      //some impulse scalar. it lets small objects bounce off more than big ones. 1.5 is 1 + 0.5 bouncyness
      float j = -1.5f * velAlongNormal;
      j /= (1f/peA.getMass()) + (1f/peB.getMass());
      
      //will only push along normal
      Vector2f impulse = new Vector2f(normal);
      impulse.scale(j);
      Vector2f impulse2 = new Vector2f(-impulse.x, -impulse.y);
      
      peB.addMovement(impulse);
      peA.addMovement(impulse2);
    }
  }
  
  private void updateTPS(float dt){
    updateCount++;
    if(dt>0)updateSum+=dt;

    if(updateSum > 1){
      tps = updateCount;
      updateCount = 0;
      updateSum = 0;
    }
  }
  
  private void updateParticles(){
    LinkedList<ComponentContainer> ccll = pcr.generate(now);
    for(ComponentContainer cc : ccll){
      Messenger.send(new Message(M_TYPE.ENTITY_MGR, new ENT_Param(cc, ENT.ADD)));
    }
  }
  
  @Override
  public void handleMessage(Message m) {
    messages.add(m);
  }
  
  private void handleMessageThreaded(){
    LinkedList<Message> mCopy = new LinkedList<>();
    mCopy.addAll(messages);

    for(Message m : mCopy){
      switch(m.getMsgType()){
      case ENTITY_MGR:
        handleENT(m);
        break;
      case ENTITY_UPD:
        handleUPD(m);
        break;
      case PARTICLE_CRT:
        handlePCL(m);
        break;
      default: break;
      }
    }
    messages.removeAll(mCopy);

  }
  
  private void handleENT(Message m){
    ENT_Param ep = (ENT_Param)m.getParam();
    
    HashSet<Component> hs = ep.cc.getComponents(addTags, Logic.OR);

    switch(ep.mode){
    case ADD:
      HashSet<Component> bt = ep.cc.getComponents("birthTrigger");
      for(Component c : bt){
        ((TriggerComponent)c).trigger(now);
      }
      toAdd.addAll(hs);
      break;
    case REMOVE:
      toRemove.addAll(hs);
      toStrip.add(ep.cc);
      break;
    }

  }
  

  private void handlePCL(Message m){
    pcr.addSystem((ParticleSettings)m.getParam());
  }
  
  private void handleUPD(Message m){
    UPD_Param up = (UPD_Param)m.getParam();
    toRemove.addAll(up.removed);
    toAdd.addAll(up.added);
  }
  
  public int getTPS(){
    return tps;
  }
  
}
