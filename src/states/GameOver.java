package states;
/**
 * 
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * This state shows that the player lost and his achieved score.
 * Press any key to switch to the menu.
 * 
 */
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import glGraphics.AppWindow;
import glGraphics.glGraphics;
import messaging.Message;
import messaging.Messenger;

import org.lwjgl.util.vector.Vector4f;

import util.SimpleLogger;


public class GameOver extends State {

  glGraphics myGlx = AppWindow.glx;
  String score;
  
  public GameOver(boolean activated) {
    super(activated);
    
  }
  
  @Override
    public void update(){
    myGlx.drawText(580, 250, "Game Over", new Vector4f(0.6f, 0.1f, 1.1f, 1));
    myGlx.drawText(560, 310, "Press Any Key", new Vector4f(0.6f, 0.1f, 1.1f, 1));
    myGlx.drawText(540, 370, "Your Score: " + score, new Vector4f(0.6f, 0.1f, 1.1f, 1));
  }
  
  @Override
    public void handleMessage(Message m){
    messaging.Messenger.send(new Message(Message.M_TYPE.CHANGE_STATE, Message.STATE.MENU));
  }
  
  @Override
    public void activate(){
    score = getScore();
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
  
  /**
   * The sixth row in the data is the last score you achived.
   * @return
   */
  private String getScore(){
    String myScore = "";
    
    try (FileReader fr = new FileReader("res/savings/highScore"); BufferedReader br = new BufferedReader(fr);){

      br.readLine();
        br.readLine();
        br.readLine();
        br.readLine();
        br.readLine();
        myScore = br.readLine();
        
        br.close();
        fr.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return myScore;
  }

}
