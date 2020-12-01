package logics;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Wraps a HashMap if String and Float for storing an retrieval of values among components
 * 
 */

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import components.BasicComponent;
import components.Component;
import components.ValueComponent;

public class ValueComponentImpl extends BasicComponent implements ValueComponent{

  private HashMap<String, Float> values;
  
  public ValueComponentImpl(){
    values = new HashMap<>();
    tags.add("value");
  }
  
  public ValueComponentImpl(String extraTag){
    this();
    tags.add(extraTag);
  }
  
  public ValueComponentImpl(LinkedList<String> extraTags){
    this();
    tags.addAll(extraTags);
  }
  
  @Override
  public void putValue(String n, float f){
    values.put(n, f);
  }
  
  @Override
  public float getValue(String n) {
    return values.get(n);
  }

  @Override
  public Component copy(boolean parentInsert) {
    ValueComponentImpl copy = new ValueComponentImpl();
    for(Entry<String, Float> e : values.entrySet()){
      copy.values.put(e.getKey(), e.getValue());
    }
    
    copy.tags.clear();
    copy.tags.addAll(tags);
    
    if(parentInsert){
      parent.registerComponent(copy);
      copy.link(parent);
    }
    
    return copy;
  }

}
