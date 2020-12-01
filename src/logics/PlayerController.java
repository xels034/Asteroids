package logics;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Listens to the game-commands from the input handler and activates any tagged compoents
 * in its container
 * 
 */

import java.util.HashSet;

import components.BasicComponent;
import components.Component;
import components.ComponentContainer;
import components.MotorComponent;
import components.SpawnComponent;
import messaging.Handler;
import messaging.Message;
import messaging.Message.CCMD_Param;
import messaging.Messenger;

public class PlayerController extends BasicComponent implements Handler{
  
  public int playerNumber;
  
  private int fire; //-1 up, 1 down
  private int move; //-1 dec, 1 acc
  private int rotate;//-1 right, 1 left
  
  public PlayerController(int i){
    super();
    
    playerNumber = i;
    fire = 0;
    move = 0;
    rotate = 0;
  }
  
  @Override
  public void link(ComponentContainer cc) {
    parent = cc;
    Messenger.subscribe(this, Message.M_TYPE.CONTROL_CMD);
  }
  
  @Override
  public void handleMessage(Message m) {
    CCMD_Param cp = (CCMD_Param)m.getParam();
    if(cp.receiver == playerNumber){
      handleComponents(cp);
    }
  }
  
  private void handleComponents(CCMD_Param cp){
    HashSet<Component> motors   = parent.getComponents("motor");
    HashSet<Component> armament = parent.getComponents("weapon");
    
    MotorComponent mc;
    SpawnComponent weapon;
    
    int mod = 1;
    if(!cp.pressed) mod = -1;
    
    switch(cp.cmd){
    case ACC:   move   +=  1*mod;break;
    case DEC:   move   += -1*mod;break;
    case LEFT:  rotate +=  1*mod;break;
    case RIGHT: rotate += -1*mod;break;
    case FIRE:  fire   +=  1*mod;break;
    default:   break;    
    }
    
    move = (int)Math.signum(move);
    rotate = (int)Math.signum(rotate);
    fire = (int)Math.signum(fire);
    
    for(Component c : motors){
      mc = (MotorComponent)c;
      mc.movement(move);
      mc.rotation(rotate);
    }
    
    for(Component c : armament){
      weapon = (SpawnComponent)c;
      if(fire == 1) weapon.switchOn();
      else      weapon.switchOff();
    }
  }
  
  @Override
  public void announceUnregister(){
    Messenger.unsubscribe(this);
  }

  @Override
  public Component copy(boolean insertParent) {
    PlayerController copy = new PlayerController(playerNumber);
    
    copy.fire = fire;
    copy.move = move;
    copy.rotate = rotate;
    copy.tags.clear();
    copy.tags.addAll(tags);
    
    if(insertParent && parent != null) {
      parent.registerComponent(copy);
      copy.link(parent);
    }
    
    return copy;
  }
  
}
