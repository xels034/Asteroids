package states;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Main Class for handling the actual game.
 * Any constructs are loaded from files or created
 * procedually. All needed components are created as well.
 * Howver, this is done in the code as opposed to be loaded
 * from files. It would have been nice to allow file-driven
 * component creation, however, there wasn't enough time.
 * 
 * The Game then renders all GraphicsComponents received by the
 * Messaging system. Upgrades are also handled manually, because
 * of a lack of time sadly. At 12.000 points, the player ComponentContainer
 * gets modified to receive a new engine, and at 20.000 points to receive
 * new weapons.
 * 
 * When the player dies, highscores are written to a file. Sending the message
 * to change the state in the FSM is delayed 3.5 seconds, so that the player
 * has a chance to recognize, that he died.
 * 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.TreeSet;
import java.util.UUID;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import particles.ParticleCreator;
import particles.ParticleSettings;
import components.Component;
import components.ComponentContainer;
import components.GraphicsComponent;
import components.PhysicsComponent;
import components.StatusComponent;
import components.TriggerComponent;
import glGraphics.AppWindow;
import glGraphics.Construct;
import glGraphics.glGraphics;
import logics.AsteroidSpawner;
import logics.Engine;
import logics.LifeWatcher;
import logics.ParticleAnnounce;
import logics.PhysicsEntry;
import logics.PhysicsManager;
import logics.PlayerController;
import logics.ValueComponentImpl;
import logics.Weapon;
import messaging.Message;
import messaging.Message.CCMD;
import messaging.Message.CCMD_Param;
import messaging.Message.ENT;
import messaging.Message.ENT_Param;
import messaging.Message.M_TYPE;
import messaging.Message.UPD_Param;
import messaging.Messenger;
import util.Ref;

public class Game extends State {

  private glGraphics glx;
  private PhysicsManager phm;
  private HashMap<String, UUID> constructs;
  private HashMap<String, Component> components;
  private HashMap<String, ComponentContainer> ccPatterns;
  
  private LinkedList<ComponentContainer> entities;
  private LinkedList<StatusComponent> stati;
  
  private int score;
  private int upgradeStage;
  private boolean gameOver;
  
  private PhysicsComponent player;
  
  private long nextSpawn;
  private long increment;
  private float incrementScale;
  
  public Game(boolean activated) {
    super(activated);
  }
  
  private void createAssets(){
    constructs = new HashMap<>();
    glx = AppWindow.glx;
    String root = "res/models";
    File f = new File(root);
    for(String s : f.list()){
      constructs.put(s.split("\\.")[0], glx.loadConstruct(root+"/"+s));
    }
    
    createSphere(16, 50, 2, 0.4f, "asteroid_big");
    createSphere(16, 30, 2, 0.4f, "asteroid_medium");
    createSphere(16, 10, 2, 0.4f, "asteroid_small");
    createSphere(8, 5, 1, 1f, "debris");
  }
  
  private void createSphere(float seg, float rad, float lw, float noise, String name){
    LinkedList<Vector2f> ll = new LinkedList<>();
    Construct c = new Construct();
    UUID u;
    Random r = new Random();
    r.setSeed(System.currentTimeMillis());
    
    for(int i=0; i<seg; i++){
      
      float n = r.nextFloat()*noise;
      
      float x = (float)Math.sin((i/seg)*Math.PI*2)*rad*(1-n);
      float y = (float)Math.cos((i/seg)*Math.PI*2)*rad*(1-n);
      ll.add(new Vector2f(x,y));

    }
    c.buildLines(ll, new Vector4f(0.8f, 1f, 0.85f, 1), false);
    c.lineW = lw;
    u = glx.registerConstruct(c);
    constructs.put(name, u);
  }
  
  private void createComponents(){
    components = new HashMap<>();
    ccPatterns = new HashMap<>();

    createPE_Components();
    createParticleCC();
    createPA_Components();
    createAmmo();
    createUpgradeComponents();
    createMiscComponents();
    createMainCC();
    spawn();
  }
  
  private void createPE_Components(){
    PhysicsEntry pe;
    
    pe = new PhysicsEntry(constructs.get("spark"), -1, 1, Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0);
    pe.addTag("noTrigger");
    components.put("pe_spark", pe);
    
    pe= new PhysicsEntry(constructs.get("exhaust"), -1, 1, Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0);
    components.put("pe_exhaust", pe);
    
    pe = new PhysicsEntry(constructs.get("player_bBullet"), 0, 0.001f, 00.5f, Float.POSITIVE_INFINITY, 10);
    pe.setV_Friction(-50f);
    pe.vectorRotation = true;
    components.put("pe_bBullet", pe);
    
    pe = new PhysicsEntry(constructs.get("player_aBullet"), 0, 0.001f, 0.5f, Float.POSITIVE_INFINITY, 5);
    pe.setV_Friction(-50f);
    pe.vectorRotation = true;
    components.put("pe_aBullet", pe);
    
    pe = new PhysicsEntry(constructs.get("player_ship"),0, 1f, Double.POSITIVE_INFINITY, 30, 0);
    pe.setPosition(new Vector2f(640, 360));
    components.put("pe_player_ship", pe);
    
    pe = new PhysicsEntry(constructs.get("asteroid_big"), -1, 1000, Double.POSITIVE_INFINITY, 150, 10);
    components.put("pe_asteroid_big", pe);
    
    pe = new PhysicsEntry(constructs.get("asteroid_medium"), -1, 500, Double.POSITIVE_INFINITY, 75, 6);
    components.put("pe_asteroid_medium", pe);
    
    pe = new PhysicsEntry(constructs.get("asteroid_small"), -1, 250, Double.POSITIVE_INFINITY, 40, 4);
    components.put("pe_asteroid_small", pe);
    
    pe= new PhysicsEntry(constructs.get("debris"), -1, 1, Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, 0);
    pe.addTag("noTrigger");
    components.put("pe_debris", pe);
    

  }
  
  private void createParticleCC(){
    ComponentContainer cc;
    
    cc = new ComponentContainer();
    cc.registerComponent(components.get("pe_spark"));
    cc.linkComponents();
    ccPatterns.put("cc_spark", cc);
    
    cc = new ComponentContainer();
    cc.registerComponent(components.get("pe_exhaust"));
    cc.linkComponents();
    ccPatterns.put("cc_exhaust", cc);
    
    cc = new ComponentContainer();
    cc.registerComponent(components.get("pe_debris"));
    cc.linkComponents();
    ccPatterns.put("cc_debris", cc);
  }
  
  private void createPA_Components(){
    LinkedList<ParticleSettings> psll;
    ParticleAnnounce pA;
    byte flags;
    
    psll = ParticleCreator.loadEffect("bBulletSpawn", ccPatterns.get("cc_spark"));
    flags = ParticleAnnounce.birth;
    pA = new ParticleAnnounce(psll, new Vector2f(0,0), flags, ParticleAnnounce.impulseRot);
    components.put("pA_bBulletSpawn", pA);
    
    psll = ParticleCreator.loadEffect("aBulletSpawn", ccPatterns.get("cc_spark"));
    flags = ParticleAnnounce.birth;
    pA = new ParticleAnnounce(psll, new Vector2f(0,0), flags, ParticleAnnounce.impulseRot);
    components.put("pA_aBulletSpawn", pA);
    
    psll = ParticleCreator.loadEffect("bBulletCollide", ccPatterns.get("cc_spark"));
    flags = ParticleAnnounce.collision;
    pA =new ParticleAnnounce(psll, new Vector2f(0,0), flags, ParticleAnnounce.impulseRot);
    components.put("pA_bBulletCollide", pA);
    
    psll = ParticleCreator.loadEffect("aBulletCollide", ccPatterns.get("cc_spark"));
    flags = ParticleAnnounce.collision;
    pA =new ParticleAnnounce(psll, new Vector2f(0,0), flags, ParticleAnnounce.impulseRot);
    components.put("pA_aBulletCollide", pA);
    
    psll = ParticleCreator.loadEffect("playerCollide", ccPatterns.get("cc_spark"));
    flags = ParticleAnnounce.collision;
    pA =new ParticleAnnounce(psll, new Vector2f(0,0), flags, ParticleAnnounce.impulseRot);
    components.put("pA_playerCollide", pA);
    
    psll = ParticleCreator.loadEffect("playerDeath", ccPatterns.get("cc_debris"));
    flags = ParticleAnnounce.death;
    pA =new ParticleAnnounce(psll, new Vector2f(0,0), flags, ParticleAnnounce.impulseRot);
    components.put("pA_playerDeath", pA);
    
    pA = new ParticleAnnounce(ParticleCreator.loadEffect("exhaust", ccPatterns.get("cc_exhaust")),new Vector2f(-20,0), ParticleAnnounce.none, ParticleAnnounce.orientRot);
    pA.addTag("engineEffect");
    components.put("pA_exhaust_center", pA);
    
    pA = new ParticleAnnounce(ParticleCreator.loadEffect("exhaust", ccPatterns.get("cc_exhaust")),new Vector2f(-20,15), ParticleAnnounce.none, ParticleAnnounce.orientRot);
    pA.addTag("engineEffect");
    components.put("pA_exhaust_right", pA);
    
    pA = new ParticleAnnounce(ParticleCreator.loadEffect("exhaust", ccPatterns.get("cc_exhaust")),new Vector2f(-20,-15), ParticleAnnounce.none, ParticleAnnounce.orientRot);
    pA.addTag("engineEffect");
    components.put("pA_exhaust_left", pA);
    
    psll = ParticleCreator.loadEffect("asteroidDeath", ccPatterns.get("cc_debris"));
    flags = ParticleAnnounce.death;
    pA = new ParticleAnnounce(psll,new Vector2f(0,0), flags, ParticleAnnounce.impulseRot);
    components.put("pA_asteroidDeath", pA);
    
    psll = ParticleCreator.loadEffect("playerUpgrade", ccPatterns.get("cc_debris"));
    flags = ParticleAnnounce.none;
    pA = new ParticleAnnounce(psll,new Vector2f(0,0), flags, ParticleAnnounce.impulseRot);
    pA.addTag("upgradeTrigger");
    components.put("pA_playerUpgrade", pA);
  }
  
  private void createAmmo(){
    ComponentContainer cc;
    
    cc = new ComponentContainer();
    cc.registerComponent(components.get("pA_bBulletSpawn"));
    cc.registerComponent(components.get("pA_bBulletCollide"));
    cc.registerComponent(components.get("pe_bBullet"));
    cc.linkComponents();
    ccPatterns.put("cc_bBullet", cc);
    
    cc = new ComponentContainer();
    cc.registerComponent(components.get("pA_aBulletSpawn"));
    cc.registerComponent(components.get("pA_aBulletCollide"));
    cc.registerComponent(components.get("pe_aBullet"));
    cc.linkComponents();
    ccPatterns.put("cc_aBullet", cc);
  }
  
  private void createUpgradeComponents(){
    Engine e;
    Weapon w;
    
    e = new Engine(150f,100f,3f, 0.6f, 0.9f, constructs.get("player_bEngine"));
    components.put("basicEngine", e);
    
    e = new Engine(600f,200f,15f, 0.8f, 0.99f, constructs.get("player_aEngine"));
    components.put("advancedEngine", e);
    
    w = new Weapon(constructs.get("player_bGun"), new Vector2f(10,0), ccPatterns.get("cc_bBullet"), 3, 0.01f);
    components.put("basicWeapon", w);

    w = new Weapon(constructs.get("player_aGunL"),new Vector2f(0,-20), ccPatterns.get("cc_aBullet"), 10, 0.01f);
    components.put("advancedWeaponL", w);
    
    w = new Weapon(constructs.get("player_aGunR"),new Vector2f( 0,20), ccPatterns.get("cc_aBullet"), 10, 0.01f);
    components.put("advancedWeaponR", w);

  }
  
  private void createMiscComponents(){
    Component c;

    c = new PlayerController(0);
    c.addTag("player0");
    components.put("controller0", c);
    
    c = new LifeWatcher("Player Health", "core");
    components.put("playerHealth", c);
    
    c = new ValueComponentImpl();
    ((ValueComponentImpl)c).putValue("score", 100);
    components.put("value_asteroid_big", c);
    
    c = new ValueComponentImpl();
    ((ValueComponentImpl)c).putValue("score", 150);
    components.put("value_asteroid_medium", c);
    
    c = new ValueComponentImpl();
    ((ValueComponentImpl)c).putValue("score", 200);
    components.put("value_asteroid_small", c);
  }
  
  private void createMainCC(){
    ComponentContainer cc;
    
    cc= new ComponentContainer();
    cc.registerComponent(components.get("pA_playerDeath"));
    cc.registerComponent(components.get("pA_exhaust_center"));
    //cc.registerComponent(components.get("pA_exhaust_left"));
    //cc.registerComponent(components.get("pA_exhaust_right"));
    cc.registerComponent(components.get("pe_player_ship"));
    cc.registerComponent(components.get("basicEngine"));
    cc.registerComponent(components.get("basicWeapon"));
    //cc.registerComponent(components.get("advancedEngine"));
    //cc.registerComponent(components.get("advancedWeaponL"));
    //cc.registerComponent(components.get("advancedWeaponR"));
    cc.registerComponent(components.get("pA_playerCollide"));
    cc.registerComponent(components.get("controller0"));
    cc.registerComponent(components.get("playerHealth"));
    cc.registerComponent(components.get("pA_playerUpgrade"));
    cc.linkComponents();
    ccPatterns.put("cc_player_ship", cc);
    
    cc = new ComponentContainer();
    cc.registerComponent(components.get("pe_asteroid_small"));
    cc.registerComponent(components.get("pA_asteroidDeath").copy(false));
    cc.registerComponent(components.get("value_asteroid_small"));
    cc.linkComponents();
    ccPatterns.put("cc_asteroid_small", cc);
    
    AsteroidSpawner as;
    as = new AsteroidSpawner(ccPatterns.get("cc_asteroid_small"), 3, 50, 100);
    components.put("as_asteroid_small", as);
    
    cc = new ComponentContainer();
    cc.registerComponent(components.get("pe_asteroid_medium"));
    cc.registerComponent(components.get("pA_asteroidDeath").copy(false));
    cc.registerComponent(components.get("value_asteroid_medium"));
    cc.registerComponent(components.get("as_asteroid_small"));
    cc.linkComponents();
    ccPatterns.put("cc_asteroid_medium", cc);
    
    as = new AsteroidSpawner(ccPatterns.get("cc_asteroid_medium"), 3, 50, 100);
    components.put("as_asteroid_medium", as);
    
    cc = new ComponentContainer();
    cc.registerComponent(components.get("pe_asteroid_big"));
    cc.registerComponent(components.get("pA_asteroidDeath"));
    cc.registerComponent(components.get("value_asteroid_big"));
    cc.registerComponent(components.get("as_asteroid_medium"));
    cc.linkComponents();
    ccPatterns.put("cc_asteroid_big", cc);

  }
  
  private void spawn(){
    ComponentContainer p = ccPatterns.get("cc_player_ship").copy();
    player = (PhysicsComponent)p.getComponents("core").iterator().next();
    Messenger.send(new Message(M_TYPE.ENTITY_MGR, new ENT_Param(p,ENT.ADD)));
    
    score = 0;
    upgradeStage = 0;
    increment = 60000;
    incrementScale = 0.94f;
    nextSpawn = System.currentTimeMillis();
  }
  
  @Override
  public void activate(){
    gameOver = false;
    phm = new PhysicsManager();
    
    entities = new LinkedList<>();
    stati = new LinkedList<>();

    Messenger.subscribe(this, Message.M_TYPE.ENTITY_MGR);
    Messenger.subscribe(this, Message.M_TYPE.ENTITY_UPD);
    Messenger.subscribe(this, Message.M_TYPE.CONTROL_CMD);
    phm.start();
    createAssets();
    createComponents();
  }
  
  @Override
  public void deactivate(){
    phm.stop();
    
    glx.waitForRelease();
    for(UUID u : constructs.values()){
      glx.releaseConstruct(u);
    }
    for(ComponentContainer cc : entities){
      cc.strip();
    }
    
    entities.clear();
    Messenger.unsubscribe(this);
  }

  @Override
  public void update(){

    handleSpawning();
    
    LinkedList<Component> cl = new LinkedList<>();
    for(ComponentContainer cc : entities){
      cl.addAll(cc.getComponents("graphics"));
    }
    
    GraphicsComponent gc;
    Construct c;
    for(Component comp : cl){
      gc = (GraphicsComponent)comp;
      c = glx.getConstruct(gc.getConstructID());
      c.position = gc.getPosition();
      c.scale = new Vector3f(gc.getScale());
      c.rotation = gc.getRotation();

      glx.drawConstruct(gc.getConstructID(), gc.getColorMod());
    }
    
    //glx.drawText(0, 0, "TPS: "+phm.getTPS(), new Vector4f(0.6f, 0.1f, 1.1f, 1));
    
    
    
    Vector2f anchor = new Vector2f(20, Ref.yRes-40);
    Construct bar = glx.getConstruct(constructs.get("status_bar"));
    Construct frame = glx.getConstruct(constructs.get("status_frame"));
    
    for(StatusComponent sc : stati){
      glx.drawText(anchor.x, anchor.y-35, sc.getName(), new Vector4f(0.6f, 0.1f, 1.1f, 1f));
      
      frame.position = anchor;
      glx.drawConstruct(constructs.get("status_frame"));
      
      bar.scale = new Vector3f(sc.getStatus(), 1, 1);
      bar.position = new Vector2f(anchor.x+10, anchor.y);
      glx.drawConstruct(constructs.get("status_bar"));
      
      anchor.x += 180;
    }
    
    String scoreText = "Score: "+score;
    glx.drawText(Ref.xRes-glx.getTextWidth(scoreText)-20, anchor.y, scoreText, new Vector4f(0.6f, 0.1f, 1.1f, 1));
  }
  
  private void handleSpawning(){
    long now = System.currentTimeMillis();
    
    if(now > nextSpawn){
      
      //SimpleLogger.log("new asteroid should appear", 1, this.getClass(), "handleSpawning");
      
      ComponentContainer newAsteroid = ccPatterns.get("cc_asteroid_big").copy();
      PhysicsEntry newCore = (PhysicsEntry)newAsteroid.getComponents("core").iterator().next();

      float x;
      float y;
      boolean found=false;
      
      while(!found){
        x = (float)(Ref.xRes*Math.random());
        y = (float)(Ref.yRes*Math.random());
        
        Vector2f asteroidPosition = new Vector2f(x, y);
        
        Vector2f distanceToPlayer = new Vector2f();
        Vector2f.sub(asteroidPosition, player.getPosition(), distanceToPlayer);
        
        if(distanceToPlayer.lengthSquared()*1.5 > newCore.getRadius2()){
          found=true;
          newCore.setPosition(asteroidPosition);
        }
      }


      
      Vector2f impulse = PhysicsEntry.rotate(new Vector2f(10000, 0), (float)(Math.random()*Math.PI/2));
      newCore.addMovement(impulse);
      
      float rotation = (float)(Math.random()-0.5);
      
      newCore.rotate(rotation*500);
      
      Messenger.send(new Message(M_TYPE.ENTITY_MGR, new Message.ENT_Param(newAsteroid, ENT.ADD)));
      

      increment *= incrementScale;
      nextSpawn+= (increment);
    }
  }
  
  @SuppressWarnings("unlikely-arg-type")
  @Override
  public void handleMessage(Message m){
    if(m.getMsgType() == M_TYPE.ENTITY_MGR){
      ENT_Param ep = (ENT_Param)m.getParam();
      
      switch(ep.mode){
      case ADD:
        handleAddition(ep);
        break;
      case REMOVE:
        handleRemoval(ep);
        break;
      }
    }else if(m.getMsgType() == M_TYPE.ENTITY_UPD){
      UPD_Param up = (UPD_Param)m.getParam();
      for(Component c : up.removed){
        if(c.getTags().contains("status")) stati.remove(c);
      }
      for(Component c : up.added){
        if(c.getTags().contains("status")) stati.add((StatusComponent)c);
      }
    }else{
      CCMD_Param cp = (CCMD_Param)m.getParam();
      
      if(cp.cmd == CCMD.MENU && cp.pressed==false){
        updateScoreFile();
        Messenger.send(new Message(M_TYPE.CHANGE_STATE, Message.STATE.GAME_OVER));
      }
    }

  }
  
  private void handleAddition(ENT_Param ep){
    entities.add(ep.cc);
    for(Component c : ep.cc.getComponents("status")){
      stati.add((StatusComponent)c);
    }
  }
  
  @SuppressWarnings("unlikely-arg-type")
  private void handleRemoval(ENT_Param ep){

    entities.remove(ep.cc);
    for(Component c : ep.cc.getComponents("status")){
      stati.remove(c);
    }
    
    for(Component c : ep.cc.getComponents("value")){
      score += ((ValueComponentImpl)c).getValue("score");
      upgradeCheck();
    }
    
    if(!ep.cc.getComponents("player0").isEmpty() && !gameOver){
      prepareGameOver();
    }
    
    
    boolean canStrip = true;
    for(String s : PhysicsManager.getTags()){
      if(!ep.cc.getComponents(s).isEmpty()) canStrip = false;
    }

    if(canStrip) ep.cc.strip();
    
  }
  
  
  
  private void upgradeCheck(){
    if(upgradeStage == 0 && score >= 12000) upgrade1();
      
    if(upgradeStage == 1 && score >= 20000) upgrade2();
  }
  
  private void upgrade1(){
    HashSet<Component> old = new HashSet<>();
    HashSet<Component> neww = new HashSet<>();
    ComponentContainer cc = player.getComponentContainer();
    HashSet<String> tags = new HashSet<>();
    
    tags.add("motor");
    tags.add("engineEffect");
    
    for(Component m :  cc.getComponents(tags)){
      cc.removeComponent(m);
      old.add(m);
    }
    
    Component engine, eff1, eff2;
    
    engine = components.get("advancedEngine").copy(false);
    engine.addTag("motor");
    cc.registerComponent(engine);

    eff1 = components.get("pA_exhaust_left").copy(false);
    cc.registerComponent(eff1);
    
    eff2 = components.get("pA_exhaust_right").copy(false);
    cc.registerComponent(eff2);

    engine.link(cc);
    eff1.link(cc);
    eff2.link(cc);
    
    neww.add(engine);
    neww.add(eff1);
    neww.add(eff2);
    
    HashSet<Component> trgLst = cc.getComponents("upgradeTrigger");
    for(Component c : trgLst){
      ((TriggerComponent)c).trigger(System.currentTimeMillis());
    }

    Messenger.send(new Message(M_TYPE.ENTITY_UPD, new UPD_Param(old, neww)));
    
    upgradeStage = 1;
  }
  
  private void upgrade2(){
    HashSet<Component> old = new HashSet<>();
    HashSet<Component> neww = new HashSet<>();
    ComponentContainer cc = player.getComponentContainer();
    
    for(Component w : cc.getComponents("weapon")){
      cc.removeComponent(w);
      old.add(w);
    }
    
    Component c;
    
    c = components.get("advancedWeaponL");
    cc.registerComponent(c);
    c.link(cc);
    neww.add(c);
    
    c = components.get("advancedWeaponR");
    cc.registerComponent(c);
    c.link(cc);
    neww.add(c);
    
    Messenger.send(new Message(M_TYPE.ENTITY_UPD, new UPD_Param(old, neww)));
    
    HashSet<Component> trgLst = cc.getComponents("upgradeTrigger");
    for(Component comp : trgLst){
      ((TriggerComponent)comp).trigger(System.currentTimeMillis());
    }
    
    upgradeStage = 2;
  }
  
  private void prepareGameOver(){
    Thread t = new Thread(new Runnable(){
      @Override
      public void run() {
        long now = System.currentTimeMillis();
        
        updateScoreFile();
        
        long gap = System.currentTimeMillis() - now;
        if(gap > 0){
          try {
            Thread.sleep(3500);
          } catch (InterruptedException e) {
            //face it. no-one cares
          }
        }
        Messenger.send(new Message(M_TYPE.CHANGE_STATE, Message.STATE.GAME_OVER));  
      }
    });
    t.start();
  }
  
  private void updateScoreFile(){
    if(!gameOver){

      try (BufferedReader br = new BufferedReader(new FileReader("res/savings/highscore"));
           BufferedWriter wr = new BufferedWriter(new FileWriter("res/savings/highscore"));){

        TreeSet<Integer> scoreList = new TreeSet<>();
        
        for(int i=0;i<5;i++){
          scoreList.add(Integer.parseInt(br.readLine()));
        }
        br.close();
        scoreList.add(score);
        
        
        
        Iterator<Integer> s =  scoreList.descendingIterator();
        for(int i=0;i<5;i++){
          wr.write(s.next()+""); wr.newLine();
        }
        
        wr.write(score+""); wr.newLine();
        wr.close();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      gameOver = true;
    }
  }

}
