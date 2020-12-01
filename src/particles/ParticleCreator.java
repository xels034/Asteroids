package particles;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Can load particle effects files and convert them into particle settings
 * This settings can be used to spawn particles. The creator listenes to dedicated
 * particle creating messages. each settings object has an ID, which can be used to
 * abort an effect prematurely
 * 
 * for each setting instance currently active, a time difference since the last update of that
 * particular system occured is calculated. This is then multiplied by the density property of
 * the settings instance, to spawn an amount of particles as PhysicsEntries, which get then
 * registered by the PhysicsManager via Messaging
 * 
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

import logics.PhysicsEntry;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import components.ComponentContainer;

public class ParticleCreator {

  private LinkedList<ParticleSettings> systems;
  
  public ParticleCreator(){
    systems = new LinkedList<>();
  }
  
  public UUID addSystem(ParticleSettings ps){
    ParticleSettings ent = ps.copy();
    ent.touch();
    systems.add(ent);
    return (UUID)ent.settings.get("pUnqID");
  }
  
  public LinkedList<UUID> addAllSystems(LinkedList<ParticleSettings> psl){
    LinkedList<UUID> ids = new LinkedList<>();
    for(ParticleSettings ps: psl){
      ParticleSettings ent = ps.copy();
      ent.touch();
      systems.add(ent);
      ids.add((UUID)ent.settings.get("pUnqID"));
    }
    return ids;
  }
  
  public void abortSystems(LinkedList<UUID> ids){
    for(UUID id : ids){
      abortSystem(id);
    }
  }
  
  public void abortSystem(UUID id){
    ParticleSettings toDelete = null;
    for(ParticleSettings ps : systems){
      UUID psid = (UUID)ps.settings.get("pUnqID");
      if(psid == id) toDelete = ps;
    }
    systems.remove(toDelete);
  }
  
  public LinkedList<ComponentContainer> generate(long now){

    Random r = new Random();
    r.setSeed(now);
    
    LinkedList<ComponentContainer> ret = new LinkedList<>();
    LinkedList<ParticleSettings> empty = new LinkedList<>();
    for(ParticleSettings pe : systems){
      long gap = now - pe.lastTouch;
      pe.lastTouch = now;
      
      float amt = (gap/1000f)*(float)pe.settings.get("density");
      if(amt < 1){
        if (r.nextFloat() < amt) amt = 1;
        else amt = 0;
      }
      
      for(int i=0;i<amt;i++){
        
        //new center
        Vector2f c = new Vector2f((Vector2f)pe.settings.get("position"));
        float posRand = (float)pe.settings.get("pRand");
        Vector2f rF = new Vector2f(r.nextFloat(), r.nextFloat());
        rF.scale(posRand);
        Vector2f.add(c, rF, c);
        c.x-=posRand/2;
        c.y-=posRand/2;
        
        //lifetime
        
        float lifeTimeOrig = (float)pe.settings.get("lifeTime");
        float lifeRand = (float)pe.settings.get("ltRand");
        float lt = lifeTimeOrig;
        lt += (r.nextFloat()*lifeRand)*lifeTimeOrig;
        lt -= (lifeRand*lifeTimeOrig)/2;
        
        ComponentContainer cc = ((ComponentContainer)pe.settings.get("spawnObj")).copy();
        PhysicsEntry phyEnt = (PhysicsEntry)cc.getComponents("core").iterator().next();
        
        float scale = (float)pe.settings.get("scale");
        float scaleRndOrig = (float)pe.settings.get("sclRand");
        float scaleRnd = r.nextFloat()*scaleRndOrig;
        scaleRnd -= scaleRndOrig/2;
        
        phyEnt.vectorRotation = (boolean)pe.settings.get("vectorRotation");
        phyEnt.setScale(scale + scaleRnd*scale);
        phyEnt.setColorMod((Vector4f)pe.settings.get("colorMod"));
        
        //movement
        float angle = (float)pe.settings.get("angle");
        float anglRand = (float)pe.settings.get("aRand");
        float speedOrig = (float)pe.settings.get("speed");
        float spdRand = (float)pe.settings.get("sRand");
        angle += r.nextFloat()*anglRand;
        angle -= anglRand/2;
        float speed = speedOrig;
        speed += r.nextFloat()*speedOrig*spdRand;
        speed -= (speedOrig*spdRand)/2;
        Vector2f dir = PhysicsEntry.rotate(new Vector2f(1,0), angle);
        if(speed == 0) speed = Float.MIN_NORMAL*1000;
        dir.scale(speed);
        
        Vector2f momentum = new Vector2f((Vector2f)pe.settings.get("momentum"));
        momentum.scale((float)pe.settings.get("mass"));
        
        phyEnt.setPosition(c);
        phyEnt.addMovement(dir);
        phyEnt.addMovement(momentum);
        phyEnt.setRotation(angle);
        phyEnt.setV_Friction((float)pe.settings.get("vFriction"));
        phyEnt.lifeTime = lt;
        phyEnt.setMass((float)pe.settings.get("mass"));
        if((boolean)pe.settings.get("clds")) phyEnt.addTag("collisions");
        else phyEnt.removeTag("collisions");

        cc.registerComponent(phyEnt);
        cc.linkComponents();
        
        ret.add(cc);
      }
      
      if(now > (pe.timeStarted+(long)pe.settings.get("emitTime"))) {
        empty.add(pe);
      }
    }
    
    systems.removeAll(empty);
    return ret;
  }
  
  //each property stored in the text file has a type. This information, however, is only
  //used when reading, when getting any property, it is expected, that the caller knows
  //how to cast that property. It doesn't seem neccesary to supply this info, as each property
  //already needs all knowledge as how to use it, so the type should be already known to the user
  public static LinkedList<ParticleSettings> loadEffect(String fn, ComponentContainer cc){
    try (BufferedReader br = new BufferedReader(new FileReader("res/fx/"+fn+".pcl"));){
      if(fn.equals(""))throw new IOException();
      LinkedList<ParticleSettings> effect = new LinkedList<>();
      
      String line = br.readLine();

      while(line != null){
        ParticleSettings ps = new ParticleSettings();
        ps.settings.put("name", line.split(" ")[1].split("=")[1]);
        line = br.readLine();
        while(line != null){
          
          String type = line.split(" ")[0];
          String[] pair = line.split(" ")[1].split("=");
          
          if(type.equals("float")){
            ps.settings.put(pair[0], readFloat(pair[1]));
          }else if(type.equals("boolean")){
            ps.settings.put(pair[0], readBoolean(pair[1]));
          }else if(type.equals("long")){
            ps.settings.put(pair[0], readLong(pair[1]));
          }else if(type.equals("Vector2f")){
            String x = br.readLine().split("=")[1];
            String y = br.readLine().split("=")[1];
            ps.settings.put(pair[0], readVec2(x,y));
          }else if(type.equals("Vector4f")){
            String x = br.readLine().split("=")[1];
            String y = br.readLine().split("=")[1];
            String z = br.readLine().split("=")[1];
            String w = br.readLine().split("=")[1];
            ps.settings.put(pair[0], readVec4(x,y,z,w));
          }else{
            break;
          }
          
          line = br.readLine();
        }
        ps.settings.put("spawnObj", cc);
        effect.add(ps);
  
      }
      

      
      br.close();
      return effect;
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }
  
  private static float readFloat(String value){
    return Float.parseFloat(value);
  }
  
  private static Vector2f readVec2(String v1, String v2){
    return new Vector2f(readFloat(v1), readFloat(v2));
  }
  
  private static Vector4f readVec4(String v1, String v2, String v3, String v4){
    return new Vector4f(readFloat(v1),
              readFloat(v2),
              readFloat(v3),
              readFloat(v4));
  }
  
  private static boolean readBoolean(String v){
    return Boolean.parseBoolean(v);
  }
  
  private static long readLong(String v){
    return Long.parseLong(v);
  }
  
}
