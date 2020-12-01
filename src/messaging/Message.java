package messaging;

import java.util.Set;

import components.Component;
import components.ComponentContainer;

/**
 * 
 * @author David-Peter Desh, Dominik Lisowski
 *
 * Messages consist of at least a type. This type then dictates the class
 * of which the parameter (if present) is. Each Handler has to make sure
 * as to know how to cast the parameter Object depending on the message type.
 * Some message Type have dedicated parameter classes, some don't, if it isn't
 * necessary.
 *
 */

public class Message {
  public enum M_TYPE{
    CHANGE_STATE,
    RAW_INPT,
    CONTROL_CMD,
    ENTITY_MGR,
    ENTITY_UPD,
    PARTICLE_CRT,
    CONTROL_CHANGE
  }
  
  public enum STATE{
    GAME,
    MENU,
    SPLASH_SCREEN,
    QUIT_SCREEN,
    WIN,
    GAME_OVER,
    PAUSE,
    CLOSE_GAME
  }
  
  public enum CCMD{
    ACC,
    DEC,
    RIGHT,
    LEFT,
    FIRE,
    C_MOVE,
    C_BTN,
    MENU,
    CONT
  }
  
  public enum CC{
    NEW_ACCELERATION,
    NEW_DECELERATION,
    NEW_LEFT,
    NEW_RIGHT,
    NEW_SHOOT,
    MSAA_UP,
    MSAA_DOWN,
    GLOW_UP,
    GLOW_DOWN
  }
  
  public enum DIRECTION{
    RIGHT,
    LEFT,
    DOWN,
    UP
  }
  
  public enum ENT{
    ADD,
    REMOVE
  }
  
  public static class UPD_Param{
    public Set<Component> removed;
    public Set<Component> added;
    
    public UPD_Param(Set<Component> r, Set<Component> a){
      removed = r;
      added = a;
    }
  }
  
  public static class ENT_Param{
    public ENT mode;
    public ComponentContainer cc;
    
    public ENT_Param(ComponentContainer cc, ENT m){
      this.cc = cc;
      mode=m;
    }
  }
  
  public static class RW_IPT_Param{
    public int key;
    public boolean pressed;
    
    public RW_IPT_Param(int k, boolean p){
      key=k;
      pressed=p;
    }
  }
  
  public static class CCMD_Param{
    public CCMD cmd;
    public int receiver;
    public boolean pressed;
    
    public CCMD_Param(CCMD c, int r, boolean p){
      cmd=c;
      receiver=r;
      pressed=p;
    }
  }

  private M_TYPE msgType;
  private Object params;
  
  public Message(M_TYPE m){
    msgType=m;
  }
  
  public Message(M_TYPE m, Object o){
    msgType = m;
    params = o;
  }
  
  public M_TYPE getMsgType(){
    return msgType;
  }
  
  public Object getParam(){
    return params;
  }
  
  public void setParams(Object o){
    params = o;
  }
  
  public Message copy(){
    return new Message(msgType, params);
  }
}
