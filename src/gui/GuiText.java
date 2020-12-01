package gui;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * This is used to display a text without the button construct.
 * GuiText isn't interactable by default and should not be interactable.
 * It's only to show some text for intructions or informations.
 * 
 */

import glGraphics.AppWindow;
import glGraphics.glGraphics;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import messaging.Message;

public class GuiText extends GuiElement{

  glGraphics myGlx = AppWindow.glx;
  public String value;
  public GuiButton parent = null;
  public boolean active = false;
  
  public GuiText(Vector2f position, String text) {
    super();
    this.position = position;
    this.value = text;
  }

  @Override
  public void handleMessage(Message m) {
    
  }

  @Override
  public void render() {
    myGlx.drawText(position.x, position.y, value, new Vector4f(0.6f, 0.1f, 1.1f, 1f));
  }

  @Override
  public void update() {
    
  }
}