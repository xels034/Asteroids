package states;

/**
 * 
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * The finite state machine handles the different states.
 * 
 */

import java.util.HashMap;

import util.SimpleLogger;
import messaging.Message;
import messaging.Messenger;

public class FSM extends State {
  public FSM(boolean activated) {
    super(activated);
  }

  public HashMap<State, HashMap<Message.STATE, State>> states = new HashMap<>();
  private State currentState;

  /**
   * If a message with M_TYPE CHANGE_STATE is incoming, the FSM changes the state.
   */
  @Override
  public void handleMessage(Message m){
    if((states.get(currentState)).containsKey(m.getParam())){
      currentState.deactivate();
      currentState = states.get(currentState).get(m.getParam());
      currentState.activate();
    }
  }
  
  /**
   * Only the current States will be updated.
   */
  @Override
  public void update(){
    if(currentState != null)currentState.update();
  }
  
  /**
   * Deactivate the current state and unsubscribe him from the messenger.
   * So the garbage collector can do his work.
   */
  @Override
  public void deactivate(){
    super.deactivate();
    if(currentState != null)currentState.deactivate();
    Messenger.unsubscribe(this);
  }
  
  public void registerState(State newState){
    states.put(newState, new HashMap<Message.STATE, State>());
    SimpleLogger.log("new State added.", 1, this.getClass(), "registerState");
  }
  
  public void registerTransition(State source, Message.STATE menu, State target){
    if(states.containsKey(source)){
      states.get(source).put(menu, target);
      SimpleLogger.log("Registered Transition.", 1, this.getClass(), "registerTransition");
    }
  }
  
  public void unregisterState(State deleteState){
    if(states.containsKey(deleteState)){
      states.remove(deleteState);
      SimpleLogger.log("State deleted.", 1, this.getClass(), "unregisterState");
    }
  }
  
  public void unregisterTransition(State source, Message msg){
    if(states.containsKey(source) && states.get(source).containsKey(msg.getParam())) {
      states.get(source).remove(msg.getParam());
      SimpleLogger.log("Deleted Transition.", 1, this.getClass(), "unregisterTransition");
    }
  }
  
  /**
   * This method sets the start and end state. This two states will be registered
   * if you use this method.
   * @param start Startstate
   * @param end Endstate
   */
  public void setStartEnd(State start, State end){
    registerState(start);
    registerState(end);
    currentState = start;
    SimpleLogger.log("Start & End set.", 1, this.getClass(), "setStartEnd");
    currentState.activate();
    this.activate();
  }
  
  /**
   * Activates the state, so the state listening too his transitions.
   */
  @Override
  public void activate(){
    Messenger.subscribe(this, Message.M_TYPE.CHANGE_STATE);
  }
}