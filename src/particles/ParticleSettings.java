package particles;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Stores all possible information in a HashMap. the class of each property is unknown however,
 * and it is expected, that the caller of a particular property knows how to interpret it. It is
 * highly discouraged to traverse through the whole Entry set. It is supposed that each property
 * needed gets called explicitly and that it is known how to cast the Object returned
 * 
 */

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

public class ParticleSettings {
  
  
  public long timeStarted;
  public long lastTouch;
  
  public HashMap<String, Object>  settings;
  
  
  public ParticleSettings(){
    
    settings = new HashMap<>();
    settings.put("pUnqID", UUID.randomUUID());
    
  }
  
  public void touch(){
    timeStarted = System.currentTimeMillis();
    lastTouch = timeStarted;
  }
  
  public ParticleSettings copy(){
    ParticleSettings copy = new ParticleSettings();
    
    for(Entry<String, Object> ent : settings.entrySet()){
      if(!copy.settings.containsKey(ent.getKey())){
        copy.settings.put(ent.getKey(), ent.getValue());
      }
    }
    
    return copy;
  }
  
}
