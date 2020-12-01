package states;

/**
 * 
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * This state asks you if you are sure to quit.
 * RETURN -> QUIT
 * ENTER -> MENU
 * 
 */

import glGraphics.AppWindow;
import glGraphics.glGraphics;
import messaging.Message;
import messaging.Messenger;
import messaging.Message.RW_IPT_Param;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector4f;

import util.SimpleLogger;


public class QuitScreen extends State {
  
  glGraphics myGlx = AppWindow.glx;

  public QuitScreen(boolean activated) {
    super(activated);
  }

  @Override
    public void update(){
    myGlx.drawText(560, 380, "Enter: Quit", new Vector4f(0.6f, 0.1f, 1.1f, 1));
    myGlx.drawText(580, 280, "ESC: Menu", new Vector4f(0.6f, 0.1f, 1.1f, 1));
  }
  
  @Override
    public void handleMessage(Message m){
    if(((RW_IPT_Param)m.getParam()).pressed == true){
      if(((RW_IPT_Param)m.getParam()).key == Keyboard.getKeyIndex("RETURN")) messaging.Messenger.send(new Message(Message.M_TYPE.CHANGE_STATE, Message.STATE.CLOSE_GAME));
      if(((RW_IPT_Param)m.getParam()).key == Keyboard.getKeyIndex("ESCAPE")) messaging.Messenger.send(new Message(Message.M_TYPE.CHANGE_STATE, Message.STATE.MENU));
    }
  }
  
  @Override
    public void activate(){
    this.activated = true;
    SimpleLogger.log("State activated", 1, this.getClass(), "activate");
    
    Messenger.subscribe(this, Message.M_TYPE.RAW_INPT);
  }
  
  @Override
    public void deactivate(){
    Messenger.unsubscribe(this);
    this.activated = false;
    SimpleLogger.log("State deactivated.", 1, this.getClass(), "deactivate");

    
  }
}
