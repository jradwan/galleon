//////////////////////////////////////////////////////////////////////
//
// File: BFocusMgr.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

import com.tivo.hme.sdk.*;

/**
 * A helper class for figuring out how to move the focus in response to an arrow
 * key. BFocusMgr will try to move the focus in the direction specified. It will
 * find the view that is closest to the ray jutting from the center of the src
 * view in the direction of the arrow key.<p>
 *
 * In order to be considered a candidate for focus, a view must:
 * <ul>
 *   <li>be visible on screen
 *   <li>be focusable (see BView.setFocusable)
 * </ul>
 *
 * @author      Adam Doppelt
 */
public class BFocusMgr implements IHmeProtocol, IBananas
{
    //
    // the src bounds and center
    //
    
    private int sx1, sy1, sx2, sy2, scx, scy;

    //
    // the current best candidate for moving the focus
    //
    
    private BView bestView;
    private int bestX, bestY;

    //
    // the previous follow. We remember this so we can bounce directly back to
    // it if the user reverses direction. This is essential because the focus
    // movement algorithm won't necessarily bring the user back to the place
    // they expect.
    //

    private BView lastSrc;
    private int lastCode;

    /**
     * Find the new focus when an arrow key is pressed on src. This method is
     * synchronized because we store search state inside the focus mgr.
     * 
     * @param src the view which the arrow is pointing away from
     * @param code the arrow key
     * @return the view in that direction or null if none found
     */
    public synchronized BView followArrow(BView src, int code)
    {
        //
        // if this is the opposite of the previous follow, return the previous
        // src.
        //
        
        if (bestView == src && bestView.getID() != -1 && lastSrc.getID() != -1) {
            // some tricky code to figure out the "reverse" of the code
            // KEY_UP => KEY_DOWN, KEY_DOWN => KEY_UP, etc.
            int reverse = code + 1 - ((code % 2) * 2);
            if (reverse == lastCode) {
                return lastSrc;
            }
        }

        lastSrc = src;
        lastCode = code;
        
        //
        // move src to screen space
        //

        BPoint s = src.toScreen();
        BScreen screen = src.getScreen();

        sx1 = s.x + screen.getX();
        sy1 = s.y + screen.getY();
        sx2 = sx1 + src.getWidth();
        sy2 = sy1 + src.getHeight();
        scx = (sx1 + sx2) / 2;
        scy = (sy1 + sy2) / 2;        

        //
        // now walk the tree looking for candidates
        //

        bestView = null;
        bestX = bestY = 100000;
        
        walkTree(screen, 0, 0, code);
        return bestView;
    }

    void walkTree(BView view, int dx, int dy, int code)
    {
        //
        // 1. ignore invisible views
        //
        
        if (!view.getVisible() || view.getTransparency() == 1f) {
            return;
        }

        //
        // 2. is this view focusable and better than bestView?
        //

        if (view.isFocusable()) {
            checkForNewBest(view, dx, dy, code);
        }

        dx += view.getX() + view.getTranslationX();
        dy += view.getY() + view.getTranslationY();

        //
        // 3. check children - ignore clipped views
        //

        if (view.getChildCount() > 0) {
            int x1 = -view.getTranslationX();
            int y1 = -view.getTranslationY();
            int x2 = x1 + view.getWidth();
            int y2 = y1 + view.getHeight();
        
            for (int i = view.getChildCount(); i-- > 0;) {
                BView v = (BView)view.getChild(i);
                if (v.getX() < x1 || v.getY() < y1 || v.getX() >= x2 || v.getY() >= y2) {
                    continue;
                }
                walkTree(v, dx, dy, code);
            }
        }
    }

    void checkForNewBest(BView view, int dx, int dy, int code)
    {
        //
        // map view to screen space
        //

        int x1 = view.getX() + dx;
        int y1 = view.getY() + dy;
        int x2 = x1 + view.getWidth();
        int y2 = y1 + view.getHeight();
        int cx = (x1 + x2) / 2;
        int cy = (y1 + y2) / 2;        

        int distX = 0;
        int distY = 0;

        if (view.isFocusable()) {
            switch (code) {
              case KEY_UP:
              case KEY_DOWN: {
                  //
                  // eliminate views that are below/above the src
                  //
                  
                  if (code == KEY_UP) {
                      if (sy1 < y2) {
                          return;
                      }
                  } else if (sy2 > y1) {
                      return;
                  }

                  //
                  // calculate distX
                  // (if we overlap in the x, the distance is 0)
                  //
                  
                  if (!overlaps(sx1, sx2, x1, x2)) {
                      distX = Math.abs(scx - cx);
                      if (distX > bestX) {
                          return;
                      }
                  }

                  //
                  // calculate distY
                  //
                  // if there is a tie in distX, use distY to break it
                  // if there is a tie in distY, pick the leftmost node
                  //
                  
                  distY = Math.abs(scy - cy);
                  if (distX == bestX) {
                      if (distY > bestY) {
                          return;
                      }
                      if (distY < bestY) {
                          break;
                      }
                      if (scx < cx) {
                          return;
                      }
                  }
                  break;
              }

              case KEY_LEFT:
              case KEY_RIGHT: {
                  //
                  // eliminate views that are right/left of src
                  //
                  
                  if (code == KEY_LEFT) {
                      if (sx1 < x2) {
                          return;
                      }
                  } else if (sx2 > x1) {
                      return;
                  }

                  //
                  // calculate distY
                  // (if we overlap in the y, the distance is 0)
                  //
                  
                  if (!overlaps(sy1, sy2, y1, y2)) {
                      distY = Math.abs(scy - cy);
                      if (distY > bestY) {
                          return;
                      }
                  }

                  //
                  // calculate distX
                  //
                  // if there is a tie in distY, use distX to break it
                  // if there is a tie in distX, pick the topmost node
                  //

                  distX = Math.abs(scx - cx);
                  if (distY == bestY) {
                      if (distX > bestX) {
                          return;
                      }
                      if (distX < bestX) {
                          break;
                      }
                      if (scy < cy) {
                          return;
                      }
                  }
                  break;
              }
            }
        }

        //
        // if we get here, we have a winner
        //

        bestView = view;
        bestX = distX;
        bestY = distY;
    }

    /**
     * Returns true if the range a1-a2 overlaps b1-b2.
     */
    static boolean overlaps(int a1, int a2, int b1, int b2)
    {
        return (a1 < a2 && b1 < b2 && b2 > a1 && a2 > b1);
    }
}
