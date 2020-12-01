package logics;

/**
 * @author David-Peter Desch, Dominik Lisowski
 * 
 * Used to accelerate the collision handlign process. A grid seemed inapropriate, as the size of physical
 * objects mary vary quite a lot.
 * 
 * The Tree can be configured:
 * 
 * MaxDepth denotes the maximum deapth of a node. IF a node at maxDepth would have to be split in 4, it
 * instead remains with more objects per node that usual
 * 
 * maxItems denotes the maximum number of items in a node before it is split in 4. IF the node is at
 * maxDepth, this splitting does not happen. When a node is split, all its inhabitants are distributed
 * among the 4 new child-nodes.
 * 
 * Items, that occupy more than 1 node are stored higher up in the QuadTree, until the object fits in 1 node
 * When acquiring any collision candidates for a object from the tree, all items sharing the same node as the
 * object are returned, as well as all objects in any parent nodes.
 */

import java.awt.geom.Rectangle2D;
import java.util.LinkedList;

import components.CollisionComponent;

public class QuadTree {

  private int maxDepth;
  private int maxItems;
  private int level;
  private QuadTree[] nodes;
  private LinkedList<CollisionComponent> items;
  private Rectangle2D.Double bounds;
  
  public QuadTree(Rectangle2D.Double rect, int mD, int mI, int lvl){
    maxDepth = mD;
    maxItems = mI;
    level=lvl;
    
    items = new LinkedList<>();
    nodes = new QuadTree[4];
    bounds = rect;
  }
  
  public void clear(){
    items.clear();
    for(int i=0; i < nodes.length; i++){
      if(nodes[i] != null){
        nodes[i].clear();
        nodes[i] = null;
      }
    }
  }
  
  private void split(){
    double subWidth = bounds.getWidth()/2;
    double subHeight = bounds.getHeight()/2;
    double x = bounds.getX();
    double y = bounds.getY();
    
    nodes[0] = new QuadTree(new Rectangle2D.Double(x+subWidth, y, subWidth, subHeight), maxDepth, maxItems, level+1);
    nodes[1] = new QuadTree(new Rectangle2D.Double(x, y, subWidth, subHeight), maxDepth, maxItems, level+1);
    nodes[2] = new QuadTree(new Rectangle2D.Double(x, y+subHeight, subWidth, subHeight), maxDepth, maxItems, level+1);
    nodes[3] = new QuadTree(new Rectangle2D.Double(x+subWidth, y+subHeight, subWidth, subHeight), maxDepth, maxItems, level+1);
  }
  
  private int getIndex(Rectangle2D.Double pbB){
    double vLine = bounds.x+(bounds.width/2);
    double hLine = bounds.y+(bounds.height/2);
    
    if(pbB.x < bounds.x || pbB.x+pbB.width > bounds.x+bounds.height) return -1;
    if(pbB.y < bounds.y || pbB.y+pbB.height > bounds.y+bounds.height) return -1;
    
    boolean fitsTop = (pbB.y+pbB.height < hLine);
    boolean fitsBottom =(pbB.y > hLine);
    boolean fitsLeft = (pbB.x+pbB.width < vLine);
    boolean fitsRight = (pbB.x > vLine);
    
    if(fitsTop){
      if(fitsLeft)     return 1;
      else if(fitsRight)  return 0;
    }else if(fitsBottom){
      if(fitsLeft)     return 2;
      else if(fitsRight)  return 3;
    }
    return -1;
  }
  
  public void insert(CollisionComponent cp){
    if(nodes[0] != null){
      int idx = getIndex(cp.getSquareBounds());
      if(idx != -1){
        nodes[idx].insert(cp);
        return;
      }
    }
    
    items.add(cp);
    
    if(items.size() > maxItems && level < maxDepth){
      if(nodes[0] == null){
        split();
      }
      int i=0;
      while( i < items.size()){
        CollisionComponent cpToInsert = items.get(i);
        int idx = getIndex(cpToInsert.getSquareBounds());
        //insert and get rid of item
        //else ++ to skip this item
        if(idx != -1){
          nodes[idx].insert(cpToInsert);
          items.remove(i);
        }else{
          i++;
        }
      }
    }
  }
  
  public LinkedList<CollisionComponent> getCandidates(Rectangle2D.Double pbB){
    LinkedList<CollisionComponent> ret = new LinkedList<>();
    int idx = getIndex(pbB);
    
    if(nodes[0] != null){
      if(idx != -1)            ret.addAll(nodes[idx].getCandidates(pbB));
      else{
        for(int i=0;i<4;i++) ret.addAll(nodes[i].getCandidates(pbB));
      }
    }
    
    ret.addAll(items);
    return ret;
  }
  
  public LinkedList<CollisionComponent> getEscapers(){
    LinkedList<CollisionComponent> ret = new LinkedList<>();
    for(CollisionComponent cp: items){
      if(!bounds.contains(cp.getSquareBounds())){
        ret.add(cp);
      }
    }
    return ret;
  }
  
}
