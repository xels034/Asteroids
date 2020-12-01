package components;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * information required by any component, that does something (whatever is defined in trigger()) when instructed
 * 
 */

public interface TriggerComponent extends Component{

  public void trigger(long now);
  public long getLastTriggered();
}
