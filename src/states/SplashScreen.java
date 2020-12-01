package states;

import glGraphics.AppWindow;
import glGraphics.glGraphics;
import gui.GuiButton;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import messaging.Message;
import messaging.Messenger;
import util.SimpleLogger;


/**
 * 
 * @author David-Peter Desch, Dominik Lisowski
 *
 * First State (startState) is the SplashScreen. This is the first screen you will see if you start the game.
 * Press any key to switch to the menu.
 *
 */

public class SplashScreen extends State{
  
  private GuiButton header;
  glGraphics myGlx = AppWindow.glx;

  public SplashScreen(boolean activated) {
    super(activated);
  }
  
  @Override
    public void update(){
    header.render();
    myGlx.drawText(560, 400, "Press Any Key", new Vector4f(0.6f, 0.1f, 1.1f, 1));
  }
  
  @Override
    public void handleMessage(Message m){
    messaging.Messenger.send(new Message(Message.M_TYPE.CHANGE_STATE, Message.STATE.MENU));
  }
  
  @Override
    public void activate(){
    
    header = new GuiButton(new Vector2f(440,200), new Vector2f(1f, 1f), "Asteroids", false, null);
    
    
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
