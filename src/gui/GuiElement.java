package gui;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * A GuiElement has a linked list for other GuiElements.
 * You also can choose if a guiElement should be interactable,
 * has visible children.
 * 
 */

import java.util.LinkedList;

import messaging.Handler;
import messaging.Messenger;

import org.lwjgl.util.vector.Vector2f;

public abstract class GuiElement implements Handler {
  public abstract void render();
  public abstract void update();
  public Vector2f position = new Vector2f(0,0);
  public LinkedList<GuiElement> children = new LinkedList<>();
  public boolean interactable = false;
  public boolean visibleChildren = true;
  public boolean runVisi = false;
  
  public float startX, startY;
  public float endX, endY;
  
  public void clearR(){
    for(GuiElement uie : children){
      uie.clearR();
    }
    children.clear();
  }
  
  public void unsubscribeAll(){
    for(GuiElement uie : children){
      Messenger.unsubscribe(uie);
    }
    Messenger.unsubscribe(this);
  }
}
