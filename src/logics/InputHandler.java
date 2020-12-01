package logics;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Works each update through any keyboard inputs that happened during the last update.
 * If sends the raw_input key as a message, aswell as a translated game-command. It is
 * adviced to use raw input only, when game commands are unavailable or insufficient
 * 
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import messaging.Message;
import messaging.Message.CCMD;
import messaging.Message.M_TYPE;
import messaging.Messenger;

import org.lwjgl.input.Keyboard;

import util.SimpleLogger;

public class InputHandler{

  private static HashMap<Integer, Message.CCMD> inputMap;
  
  public InputHandler(){
    inputMap = new HashMap<>();
    loadDefaults();
  }
  
  private static void loadDefaults(){
    
    String up,left,down,right,shoot;
    up = left = down = right = shoot = null;
    try (FileReader fr = new FileReader("res/savings/keyBindings");
         BufferedReader br = new BufferedReader(fr);){

        up = br.readLine();
        left = br.readLine();
        down = br.readLine();
        right = br.readLine();
        shoot = br.readLine();
        br.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

      
    inputMap.put(Integer.parseInt(up), CCMD.ACC);
    inputMap.put(Keyboard.getKeyIndex("UP"), CCMD.ACC);
    
    inputMap.put(Integer.parseInt(down), CCMD.DEC);
    inputMap.put(Keyboard.getKeyIndex("DOWN"), CCMD.DEC);
    
    inputMap.put(Integer.parseInt(left), CCMD.LEFT);
    inputMap.put(Keyboard.getKeyIndex("LEFT"), CCMD.LEFT);
    
    inputMap.put(Integer.parseInt(right), CCMD.RIGHT);
    inputMap.put(Keyboard.getKeyIndex("RIGHT"), CCMD.RIGHT);
    
    inputMap.put(Integer.parseInt(shoot), CCMD.FIRE);
    inputMap.put(Keyboard.getKeyIndex("RETURN"), CCMD.CONT);
    inputMap.put(Keyboard.getKeyIndex("ESCAPE"), CCMD.MENU);
  }
  
  
  
  public void update(){
    //We decided against the use of mouse input, because it would only be sued in the very simple menu, where you're faster with the keyboard anyways
    while(Keyboard.next()){
      int ke = Keyboard.getEventKey();
      boolean st = Keyboard.getEventKeyState();
      SimpleLogger.log("Key "+ke+"("+Keyboard.getKeyName(ke) + ") was " + st + " pressed", 3, this.getClass(), "update");
      Messenger.send(new Message(M_TYPE.RAW_INPT, new Message.RW_IPT_Param(ke, st)));
      
      if(inputMap.containsKey(ke)){
        Messenger.send(new Message(M_TYPE.CONTROL_CMD, new Message.CCMD_Param(inputMap.get(ke), 0, st)));
      }  
    }
    
    
    /*while(Mouse.next()){
      if(Mouse.getEventButton() != -1){
        SimpleLogger.log("Button " + Mouse.getButtonName(Mouse.getEventButton()) + " was " + Mouse.getEventButtonState() + " pressed", 11, AppWindow.class, "run");  
      }
      else {
        SimpleLogger.log("Mouse Move Event: dx="+Mouse.getDX() + " dy="+Mouse.getDY() + "dz="+Mouse.getDWheel(), 11, AppWindow.class, "run");
      }
    }*/
  }
  
}
