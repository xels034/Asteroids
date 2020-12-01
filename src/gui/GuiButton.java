package gui;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * This Element is used to draw Buttons with text in it.
 * The text will be always in the middle of the button.
 * Every Button has a linked list, in this list are other buttons,
 * this is used to build the menu and navigate through it.
 * It is also possible to make a button not interactable,
 * so one can use it to show something, like highscores or the header.
 * 
 * The buttons are also animated. If you switch to a button, the glow will increase.
 * If you switch to an other button, the glow will decrease.
 * With animationDuration you can set the animation duration in milliseconds.
 * Every button can also send a message if you select it. But only if the message isn't null.
 * The button will react if the player presses the RETURN key and not if you release the key.
 * 
 */

import glGraphics.AppWindow;
import glGraphics.Construct;
import glGraphics.glGraphics;

import java.util.LinkedList;
import java.util.UUID;

import messaging.Message;
import messaging.Message.DIRECTION;
import messaging.Message.RW_IPT_Param;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import util.Ref;

public class GuiButton extends GuiElement{
  private Vector2f size;
  public String value;
  private static Construct c;
  private static UUID cID;
  glGraphics myGlx = AppWindow.glx;
  public boolean active = false;
  public GuiButton parent = null;
  protected Message message;
  public Vector2f textPosition;
  
  private long animationDuration = 400;
  private long start;
  private boolean run;
  
  public GuiButton(Vector2f position, Vector2f size, String value, boolean interact, Message m) {
    this.position = position;
    this.size = size;
    this.value = value;
    this.interactable = interact;
    this.message = m;
    
    if(c == null){
      makeButtonConstruct();
    }
  }
  
  private static void makeButtonConstruct(){
    float halfheight = Ref.buttonSize.y/2; 
    
    Vector2f v1 = new Vector2f();
    Vector2f v2 = new Vector2f();
    Vector2f v3 = new Vector2f();
    Vector2f v4 = new Vector2f();
    Vector2f v5 = new Vector2f();
    Vector2f v6 = new Vector2f();
    
    Vector2f.add(new Vector2f(0,0), new Vector2f(halfheight, 0), v1);
    Vector2f.add(new Vector2f(0,0), new Vector2f(Ref.buttonSize.x - halfheight, 0), v2);
    Vector2f.add(new Vector2f(0,0), new Vector2f(Ref.buttonSize.x, halfheight), v3);
    Vector2f.add(new Vector2f(0,0), new Vector2f(Ref.buttonSize.x - halfheight, Ref.buttonSize.y), v4);
    Vector2f.add(new Vector2f(0,0), new Vector2f(halfheight, Ref.buttonSize.y), v5);
    Vector2f.add(new Vector2f(0,0), new Vector2f(0, halfheight), v6);
    
    LinkedList<Vector2f> ll = new LinkedList<>();
    ll.add(v1);
    ll.add(v2);
    ll.add(v3);
    ll.add(v4);
    ll.add(v5);
    ll.add(v6);
    
    c = new Construct();
    c.buildLines(ll, new Vector4f(1,1,1,1), false);
  }

  @Override
  public void handleMessage(Message m) {
    if     (message != null &&
        m.getMsgType() == messaging.Message.M_TYPE.RAW_INPT &&
        ((RW_IPT_Param)m.getParam()).key == Keyboard.getKeyIndex("RETURN") &&
        ((RW_IPT_Param)m.getParam()).pressed == true){
      messaging.Messenger.send(message);
    }  
  }

  @Override
  public void render() {
    try{
      textPosition = new Vector2f();
      textPosition.x = position.x + Ref.buttonSize.x * size.x /2f - myGlx.getTextWidth(value)/2f;
      textPosition.y = position.y + Ref.buttonSize.y * size.y /2f - myGlx.getTextHeight(value)/2f;
      
      c.scale = new Vector3f(size.x, size.y, 1);
      c.position = new Vector2f(position.x, position.y);
      if(!run && System.currentTimeMillis() < (start+animationDuration)){
        long cur = System.currentTimeMillis()-start;
        float i= ((float)cur)/((float)animationDuration);
        myGlx.drawConstruct(cID, new Vector4f(1.6f - i, 1.1f - i, 2.1f - i, 1));
      }
      else if(run && System.currentTimeMillis() < (start+animationDuration)){
        long cur = System.currentTimeMillis()-start;
        float i= ((float)cur)/((float)animationDuration);
        myGlx.drawConstruct(cID, new Vector4f(0.6f + i, 0.1f + i, 1.1f + i, 1));
      }
      else if(active == false) {
        myGlx.drawConstruct(cID, new Vector4f(0.6f, 0.1f, 1.1f, 1));
      }
      else{
        myGlx.drawConstruct(cID, new Vector4f(1.6f, 1.1f, 2.1f, 1));
      }
      
      myGlx.drawText(textPosition.x, textPosition.y, value, new Vector4f(0.6f, 0.1f, 1.1f, 1));
    }catch (IllegalArgumentException x){
      cID = myGlx.registerConstruct(c);
    }
    
    if(visibleChildren == true) for(GuiElement ui : children) ui.render();
  }

  @Override
  public void update() {
    // TODO Auto-generated method stub
    
  }
  
  public void activate(){
    start();
    
    
    active = true;
  }
  
  public void deactivate(){
    start = System.currentTimeMillis();
    run = false;
    active = false;
  }
  
  private void start(){
    start = System.currentTimeMillis();
    run = true;
  }

  public void setVisibleChildren(boolean visibleChildren, DIRECTION direction, int pixel) {
    this.visibleChildren = visibleChildren;
  }
}
