package states;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * If you make a new state, it will be not activated.
 * Only the FSM will activate a state. If the FSM activate a state, it
 * start the method activate. In this method you will make your objects
 * and initialize the state.
 * 
 */

import messaging.Handler;
import messaging.Message;


public class State implements Handler{
  
  protected boolean activated = false;
  
  public State(boolean activated) {
    this.activated = activated;
  }

  public void activate(){
    this.activated = true;
  }
  
  public void deactivate(){
    this.activated = false;
  }
  
  public void update(){
    
  }

  @Override
  public void handleMessage(Message m) {
    // TODO Auto-generated method stub
    
  }
}
