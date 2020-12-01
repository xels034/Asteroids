package states;

/**
 * 
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Because the buttons have a linked list within other buttons, and know witch button is the parent
 * of his list, one can easily navigate.
 * The Menu has buttons to play the game, change the key bindings or graphic options, 
 * show you the credits or close the game.
 * With RETURN you can select/activate a button.
 * ESC is a shortcut to get to the quitScreen.
 * 
 */

import gui.GuiButton;
import gui.GuiElement;
import gui.GuiText;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import messaging.Message;
import messaging.Message.CC;
import messaging.Message.DIRECTION;
import messaging.Message.RW_IPT_Param;
import messaging.Messenger;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import util.Ref;
import util.SimpleLogger;


public class Menu extends State {

  private GuiButton   header, play, keyBindings, options, credits, quit, hs1, hs2, hs3, hs4, hs5, w, a, s, d, shoot;
  private GuiButton  activeButton, headerButton, msaaUp, msaaDown, glowUp, glowDown;
  private GuiText   creditsText1, creditsText2, creditsText3, creditsText4, msaaText, glowText;
  private boolean listening = false;
  private String[] keys = {"","","","",""};
  private int row = 0;
  private LinkedList<String> msaaOptions;
  private LinkedList<String> glowOptions;
  private String[] currentOptions;
  
  public Menu(boolean activated) {
    super(activated);
  }
  
  @Override
  public void update(){
    header.render();
  }
  
  public void setAllVisibleChildrenFalse(GuiElement gui){
    for(GuiElement uie : gui.children){
      uie.visibleChildren = false;
    }
  }
  
  @Override
  public void handleMessage(Message m){
    //System.out.println((RW_IPT_Param)m.getParam());
    switch(m.getMsgType()){
    case CONTROL_CHANGE:
      
      switch((CC)m.getParam()){
      case NEW_ACCELERATION:
        activeButton.value = "Press any key";
        row = 0;
        break;
      case NEW_DECELERATION:
        activeButton.value = "Press any key";
        row = 2;
        break;
      case NEW_LEFT:
        activeButton.value = "Press any key";
        row = 1;
        break;
      case NEW_RIGHT:
        activeButton.value = "Press any key";
        row = 3;
        break;
      case NEW_SHOOT:
        activeButton.value = "Press any key";
        row = 4;
        break;
      case MSAA_UP:
        currentOptions = getGraficOptions();
        for(int i = 0; i < msaaOptions.size()-1; i++){
          if (msaaOptions.get(i).equals(currentOptions[0])){
            currentOptions[0] = msaaOptions.get(i+1);
            msaaText.value = "MSAA: " + currentOptions[0];
            break;
          }
        }
        writeOptions(currentOptions);
        break;
      case MSAA_DOWN:
        currentOptions = getGraficOptions();
        for(int i = msaaOptions.size() - 1; i > 0; i--){
          if (msaaOptions.get(i).equals(currentOptions[0])){
            currentOptions[0] = msaaOptions.get(i-1);
            msaaText.value = "MSAA: " + currentOptions[0];
            break;
          }
        }
        writeOptions(currentOptions);
        break;
      case GLOW_UP:
        currentOptions = getGraficOptions();
        for(int i = 0; i < glowOptions.size()-1; i++){
          if (glowOptions.get(i).equals(currentOptions[1])){
            currentOptions[1] = glowOptions.get(i+1);
            glowText.value = "Glow: " + currentOptions[1];
            break;
          }
        }
        writeOptions(currentOptions);
        break;
      case GLOW_DOWN:
        currentOptions = getGraficOptions();
        for(int i = glowOptions.size() - 1; i > 0; i--){
          if (glowOptions.get(i).equals(currentOptions[1])){
            currentOptions[1] = glowOptions.get(i-1);
            glowText.value = "Glow: " + currentOptions[1];
            break;
          }
        }
        writeOptions(currentOptions);
        break;
      default:
        break;
      }
      break;
    case RAW_INPT:
      handleRAWinput(m);
      break;
    default:
      break;
    }
  }
  
  @Override
  public void activate(){
    getKeys();
    String[] hs = getHighscore();
    currentOptions = getGraficOptions();
    
    msaaOptions = new LinkedList<>();
    glowOptions = new LinkedList<>();
    msaaOptions.add("OFF");
    msaaOptions.add("4X");
    msaaOptions.add("16X");
    glowOptions.add("OFF");
    glowOptions.add("LOW");
    glowOptions.add("HIGH");
    
    Message mPlay = new Message(Message.M_TYPE.CHANGE_STATE, Message.STATE.GAME);
    Message mQuit = new Message(Message.M_TYPE.CHANGE_STATE, Message.STATE.QUIT_SCREEN);
    Message mW = new Message(Message.M_TYPE.CONTROL_CHANGE, Message.CC.NEW_ACCELERATION);
    Message mA = new Message(Message.M_TYPE.CONTROL_CHANGE, Message.CC.NEW_LEFT);
    Message mS = new Message(Message.M_TYPE.CONTROL_CHANGE, Message.CC.NEW_DECELERATION);
    Message mD = new Message(Message.M_TYPE.CONTROL_CHANGE, Message.CC.NEW_RIGHT);
    Message mShoot = new Message(Message.M_TYPE.CONTROL_CHANGE, Message.CC.NEW_SHOOT);
    Message mMSAAup = new Message(Message.M_TYPE.CONTROL_CHANGE, Message.CC.MSAA_UP);
    Message mMSAAdown = new Message(Message.M_TYPE.CONTROL_CHANGE, Message.CC.MSAA_DOWN);
    Message mGLOWup = new Message(Message.M_TYPE.CONTROL_CHANGE, Message.CC.GLOW_UP);
    Message mGLOWdown = new Message(Message.M_TYPE.CONTROL_CHANGE, Message.CC.GLOW_DOWN);
    
    header = new GuiButton(new Vector2f(440,20), new Vector2f(1f, 1f), "Asteroids", false, null);
    play = new GuiButton(new Vector2f(20,120), new Vector2f(1f, 1f), "Play", true, mPlay);
    keyBindings = new GuiButton(new Vector2f(20,235), new Vector2f(1f, 1f), "Key Bindings", true, null);
    options = new GuiButton(new Vector2f(20,350), new Vector2f(1f, 1f), "Options", true, null);
    credits = new GuiButton(new Vector2f(20,465), new Vector2f(1f, 1f), "Credits", true, null);
    quit = new GuiButton(new Vector2f(20,580), new Vector2f(1f, 1f), "Quit", true, mQuit);
    
    creditsText1 = new GuiText(new Vector2f(650, 240), "Created By: Dominik Lisowski, David-Peter Desch");
    creditsText2 = new GuiText(new Vector2f(650, 340), "realized with Java and lwjgl");
    creditsText3 = new GuiText(new Vector2f(650, 440), "thanks to DI Roman Divotkey");
    creditsText4 = new GuiText(new Vector2f(650, 540), "Copyright 2014");
    
    hs1 = new GuiButton(new Vector2f(860, 120), new Vector2f(1f, 1f), "1. " + hs[0], false, null);
    hs2 = new GuiButton(new Vector2f(860, 235), new Vector2f(1f, 1f), "2. " + hs[1], false, null);
    hs3 = new GuiButton(new Vector2f(860, 350), new Vector2f(1f, 1f), "3. " + hs[2], false, null);
    hs4 = new GuiButton(new Vector2f(860, 465), new Vector2f(1f, 1f), "4. " + hs[3], false, null);
    hs5 = new GuiButton(new Vector2f(860, 580), new Vector2f(1f, 1f), "5. " + hs[4], false, null);
    
    w = new GuiButton(new Vector2f(860f, 120f), new Vector2f(1f, 1f), "Accelerate: " + Keyboard.getKeyName(Integer.parseInt(keys[0])), true, mW);
    a = new GuiButton(new Vector2f(860f, 235f), new Vector2f(1f, 1f), "Left: " + Keyboard.getKeyName(Integer.parseInt(keys[1])), true, mA);
    s = new GuiButton(new Vector2f(860f, 465f), new Vector2f(1f, 1f), "Decelerate: " + Keyboard.getKeyName(Integer.parseInt(keys[2])), true, mS);
    d = new GuiButton(new Vector2f(860f, 350f), new Vector2f(1f, 1f), "Right: " + Keyboard.getKeyName(Integer.parseInt(keys[3])), true, mD);
    shoot = new GuiButton(new Vector2f(860f, 580f), new Vector2f(1f, 1f), "Shoot: " + Keyboard.getKeyName(Integer.parseInt(keys[4])), true, mShoot);
    
    msaaUp = new GuiButton(new Vector2f(860, 120), new Vector2f(1f,1f), "MSAA Up", true, mMSAAup);
    msaaText = new GuiText(new Vector2f(1000, 230), "MSAA: " + currentOptions[0]);
    msaaDown = new GuiButton(new Vector2f(860, 270), new Vector2f(1f,1f), "MSAA Down", true, mMSAAdown);
    glowUp = new GuiButton(new Vector2f(860, 430), new Vector2f(1f,1f), "Glow Up", true, mGLOWup);
    glowText = new GuiText(new Vector2f(1000, 540), "Glow: " + currentOptions[1]);
    glowDown = new GuiButton(new Vector2f(860, 580), new Vector2f(1f,1f), "Glow Down", true, mGLOWdown);
    
    
    header.children.add(play);
    play.parent = header;
    header.children.add(keyBindings);
    keyBindings.parent = header;
    header.children.add(options);
    options.parent = header;
    header.children.add(credits);
    credits.parent = header;
    header.children.add(quit);
    quit.parent = header;
    
    keyBindings.children.add(w);
    w.parent = keyBindings;
    keyBindings.children.add(a);
    a.parent = keyBindings;
    keyBindings.children.add(d);
    d.parent = keyBindings;
    keyBindings.children.add(s);
    s.parent = keyBindings;
    keyBindings.children.add(shoot);
    shoot.parent = keyBindings;
    
    credits.children.add(creditsText1);
    creditsText1.parent = credits;
    credits.children.add(creditsText2);
    creditsText2.parent = credits;
    credits.children.add(creditsText3);
    creditsText3.parent = credits;
    credits.children.add(creditsText4);
    creditsText4.parent = credits;
    
    
    setAllVisibleChildrenFalse(header);
    play.active = true;
    play.setVisibleChildren(true, DIRECTION.RIGHT, 450);
    activeButton = play;
    headerButton = header;
    
    play.children.add(hs1);
    hs1.parent = play;
    play.children.add(hs2);
    hs2.parent = play;
    play.children.add(hs3);
    hs3.parent = play;
    play.children.add(hs4);
    hs4.parent = play;
    play.children.add(hs5);
    hs5.parent = play;
    
    options.children.add(msaaUp);
    msaaUp.parent = options;
    options.children.add(msaaText);
    msaaText.parent = options;
    options.children.add(msaaDown);
    msaaDown.parent = options;
    options.children.add(glowUp);
    glowUp.parent = options;
    options.children.add(glowText);
    glowText.parent = options;
    options.children.add(glowDown);
    glowDown.parent = options;
    
    this.activated = true;
    SimpleLogger.log("State activated", 1, this.getClass(), "activate");
    
    Messenger.subscribe(this, Message.M_TYPE.RAW_INPT);
    Messenger.subscribe(this, Message.M_TYPE.CONTROL_CHANGE);
  }
  
  @Override
  public void deactivate(){
    this.activated = false;
    SimpleLogger.log("State deactivated.", 1, this.getClass(), "deactivate");
    
    header.unsubscribeAll();
    
    for(GuiElement uie : header.children){
      Messenger.unsubscribe(uie);
    }
    
    Messenger.unsubscribe(header);
    Messenger.unsubscribe(this);
  }
  
  public void setNewActiveButton(GuiElement ui){
    activeButton.deactivate();
    //activeButton.visibleChildren = false;
    activeButton.setVisibleChildren(false, DIRECTION.RIGHT, 450);
    activeButton = (GuiButton) ui;
    if(headerButton.children.isEmpty() == false) headerButton.visibleChildren = true;
    activeButton.activate();
    //activeButton.visibleChildren = true;
    activeButton.setVisibleChildren(true, DIRECTION.RIGHT, 450);
    headerButton = activeButton.parent;
  }
  
  public void changeKeyBinding(int newKey){
    int maxRow = 4;
    
    try(BufferedWriter writer = new BufferedWriter(new FileWriter("res/savings/keyBindings"))){
      for(int i = 0; i <= maxRow; i++){
        if(i == row) writer.write(newKey+"");
        else writer.write(keys[i]);
        writer.newLine();
      }
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    getKeys();
    
    w.value = "Accelerate: " + Keyboard.getKeyName(Integer.parseInt(keys[0]));
    a.value = "Left: " + Keyboard.getKeyName(Integer.parseInt(keys[1]));
    s.value = "Decelerate: " + Keyboard.getKeyName(Integer.parseInt(keys[2]));
    d.value = "Right: " + Keyboard.getKeyName(Integer.parseInt(keys[3]));
    shoot.value = "Shoot: " + Keyboard.getKeyName(Integer.parseInt(keys[4]));
  }
  
  public void getKeys(){
    try (FileReader fr = new FileReader("res/savings/keyBindings"); BufferedReader br = new BufferedReader(fr);){

      keys[0] = br.readLine();
        keys[1] = br.readLine();
        keys[2] = br.readLine();
        keys[3] = br.readLine();
        keys[4] = br.readLine();
        
        br.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  private void handleRAWinput(Message m){
    if  (listening &&
        ((RW_IPT_Param)m.getParam()).pressed == true &&
        ((RW_IPT_Param)m.getParam()).key != Keyboard.getKeyIndex("UP")&&
        ((RW_IPT_Param)m.getParam()).key != Keyboard.getKeyIndex("DOWN")&&
        ((RW_IPT_Param)m.getParam()).key != Keyboard.getKeyIndex("LEFT")&&
        ((RW_IPT_Param)m.getParam()).key != Keyboard.getKeyIndex("RIGHT")&&
        ((RW_IPT_Param)m.getParam()).key != Keyboard.getKeyIndex("ESCAPE")&&
        ((RW_IPT_Param)m.getParam()).key != Keyboard.getKeyIndex("RETURN")) {
          getKeys();
          int newKey = ((RW_IPT_Param)m.getParam()).key;
          listening = false;
          changeKeyBinding(newKey);
    }
    else if (!listening){
      int current = headerButton.children.indexOf(activeButton);
      if(((RW_IPT_Param)m.getParam()).pressed == true){
        if(    ((RW_IPT_Param)m.getParam()).key == Integer.parseInt(keys[0]) ||
            ((RW_IPT_Param)m.getParam()).key == Keyboard.getKeyIndex("UP")){
          while(current >= 0){
            if(current <= 0) current = headerButton.children.size();
            current--;
            if(headerButton.children.get(current).interactable) break;
          }
          setNewActiveButton(headerButton.children.get(current));
          
        }else if(  ((RW_IPT_Param)m.getParam()).key == Integer.parseInt(keys[1]) ||
              ((RW_IPT_Param)m.getParam()).key == Keyboard.getKeyIndex("LEFT")){
              if(activeButton.parent.parent != null) {
                header = activeButton.parent.parent;
                setNewActiveButton(activeButton.parent);
              }
          
        }else if(  ((RW_IPT_Param)m.getParam()).key == Integer.parseInt(keys[2]) ||
              ((RW_IPT_Param)m.getParam()).key == Keyboard.getKeyIndex("DOWN")){
          while(current <= headerButton.children.size()){
            current++;
            if(current >= headerButton.children.size()) current = 0;
            if(headerButton.children.get(current).interactable) break;
          }
          setNewActiveButton(headerButton.children.get(current));
          
        }else if(  ((RW_IPT_Param)m.getParam()).key == Integer.parseInt(keys[3]) ||
              ((RW_IPT_Param)m.getParam()).key == Keyboard.getKeyIndex("RIGHT")){
              if(activeButton.children.isEmpty() == false){
                for(GuiElement ui: activeButton.children){
                  if(ui.interactable){
                    headerButton = activeButton;
                    setNewActiveButton(ui);
                    break;
                  }
                }  
              }
          
        }else if(  ((RW_IPT_Param)m.getParam()).key == Keyboard.getKeyIndex("RETURN")){
          if(w.active || a.active || s.active || d.active || shoot.active){
            listening = true;
          }
        }else if(  ((RW_IPT_Param)m.getParam()).key == Keyboard.getKeyIndex("ESCAPE")){
          Messenger.send(new Message(Message.M_TYPE.CHANGE_STATE, Message.STATE.QUIT_SCREEN));
        }

        if(m.getMsgType() == Message.M_TYPE.CHANGE_STATE){
          listening = true;
          
        }
      }
      if(((RW_IPT_Param)m.getParam()).pressed == true){
        activeButton.handleMessage(m);
      }
      
    }
  }
  
  public String[] getHighscore(){
    String[] highscore = new String[5];
    try (FileReader fr = new FileReader("res/savings/highScore"); BufferedReader br = new BufferedReader(fr);){
      
      highscore[0] = br.readLine();
        highscore[1] = br.readLine();
        highscore[2] = br.readLine();
        highscore[3] = br.readLine();
        highscore[4] = br.readLine();
        
        br.close();
        fr.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    return highscore;
  }
  

  public static String[] getGraficOptions(){
    String[] grafikOptions = new String[2];
    try (FileReader fr = new FileReader("res/savings/graficOptions"); BufferedReader br = new BufferedReader(fr);){

      grafikOptions[0] = br.readLine();
        grafikOptions[1] = br.readLine();
        
        br.close();
        fr.close();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    updateRef(grafikOptions);
    return grafikOptions;
  }
  
  public static void writeOptions(String[] string){
    try(BufferedWriter writer = new BufferedWriter(new FileWriter("res/savings/graficOptions"));){
      updateRef(string);
      
      int maxLines = 2;
      
      for(int i=0;i<maxLines;i++){
        writer.write(string[i]);
        writer.newLine();
      }
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  private static void updateRef(String[] s){
    if(s[0].equals("OFF")) Ref.msaa = 0;
    if(s[0].equals("4X")) Ref.msaa = 4;
    if(s[0].equals("16X")) Ref.msaa = 16;
    
    if(s[1].equals("OFF")) Ref.glowQuality = 0;
    if(s[1].equals("LOW")) Ref.glowQuality = 4;
    if(s[1].equals("HIGH")) Ref.glowQuality = 16;
  }
}
