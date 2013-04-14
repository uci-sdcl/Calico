/*******************************************************************************
 * Copyright (c) 2013, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * 
 * None of the name of the Regents of the University of California, or the names of its
 * contributors may be used to endorse or promote products derived from this software without specific
 * prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package calico.components.piemenu;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.components.menus.ContextMenu;
import calico.controllers.CGroupController;
import calico.inputhandlers.InputEventInfo;
import calico.perspectives.CalicoPerspective;

public class PieMenu
{
	public static Logger logger = Logger.getLogger(PieMenu.class.getName());
	static double DEFAULT_DEGREE_INCREMENT = 360.0/9.0;
	public static double DEG2RAD = Math.PI/180.0;
	static double START_ANGLE = -180.0;
	public static int DEFAULT_MENU_RADIUS = 45;//35
	
	
	//private static PieMenuButton[] buttonList = null;
	
	static ObjectArrayList<PieMenuButton> buttonList = new ObjectArrayList<PieMenuButton>();
	
	private static PieMenuContainer pieContainer = null;
	
	//worst piece of programming right here --v 
	public static boolean isPerformingPieMenuAction = false;
	public static long highlightedGroup = 0l;
	public static Point lastOpenedPosition = null;
	
	private static final List<ContextMenu.Listener> listeners = new ArrayList<ContextMenu.Listener>();

	public static void addListener(ContextMenu.Listener listener)
	{
		listeners.add(listener);
	}
	
	public static void removeListener(ContextMenu.Listener listener)
	{
		listeners.remove(listener);
	}
	
	// TODO: The pie menu must maintain a list of the current menu objects on display.
	// When one is called, then we call that button's onClick method.
	
	/**
	 * Users can call this and specify a point, and the array of buttons to be displayed
	 * @param location
	 * @param buttons
	 */
	public static void displayPieMenu(Point location, PieMenuButton... buttons)
	{
		displayPieMenuArray(location, buttons);
	}
	public static void displayPieMenuArray(Point location, PieMenuButton[] buttons)
	{
		lastOpenedPosition = location;
		if(pieContainer!=null)
		{
			// Clear out the old one, then try this again
			clearMenu();
		}
		
		buttonList.addElements(0, buttons, 0, buttons.length);
		getIconPositions(location);
		
		//adjust the location of the pie menu if it's off the screen
		Rectangle2D bounds = PieMenuContainer.getComputedBounds();
		Rectangle2D screenBounds = new Rectangle2D.Double(0d,0d, CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight);
		if (!screenBounds.contains(bounds))
		{
			if (bounds.getX() < 0)
				location.translate((int)(bounds.getX() * -1), 0);
			if (bounds.getY() < 0)
				location.translate(0, (int)(bounds.getY() * -1));
			if (bounds.getX() + bounds.getWidth() > screenBounds.getWidth())
				location.translate( (int)(screenBounds.getWidth() - bounds.getX() - bounds.getWidth()), 0);
			if (bounds.getY() + bounds.getHeight() > screenBounds.getHeight())
				location.translate(0, (int)(screenBounds.getHeight() - bounds.getY() - bounds.getHeight()));
			
			getIconPositions(location);
		}
		
		drawPieMenu( );
		
		for (ContextMenu.Listener listener : listeners)
		{
			listener.menuDisplayed(ContextMenu.PIE_MENU);
		}
	}
	
	private static void drawPieMenu()//, PieMenuButton[] buttons)
	{
		pieContainer = new PieMenuContainer();
		pieContainer.setBounds(getBoundsOfButtons());
		
		CalicoPerspective.Active.drawPieMenu(pieContainer);
	}
	
	
	private static void getIconPositions(Point center)
	{
		int numOfPositions = buttonList.size();
		
		double degIncrement = Math.min(360.0 / (numOfPositions), DEFAULT_DEGREE_INCREMENT);
		
		// Now we get the radius
		int menuRadius = getMinimumRadius(DEFAULT_MENU_RADIUS,numOfPositions) + 1;
		
		double curDegree = START_ANGLE;
		
		for(int i=0;i<numOfPositions;i++)
		{
			Point pos = getButtonPoint(curDegree,degIncrement,menuRadius);
//			pos.translate(center.x-(CalicoOptions.menu.icon_size/2), center.y-(CalicoOptions.menu.icon_size/2));
			pos.translate(center.x, center.y);
						
			buttonList.get(i).setPosition(pos);
			
			curDegree = curDegree + degIncrement;
		}
		
	}//
	
	public static Rectangle2D getBoundsOfButtons()
	{
		double lowX = java.lang.Double.MAX_VALUE, lowY = java.lang.Double.MAX_VALUE, highX = java.lang.Double.MIN_VALUE, highY = java.lang.Double.MIN_VALUE;
		
		Rectangle bounds;
		for (PieMenuButton button : buttonList)
		{
			bounds = button.bounds;
			if (lowX > bounds.x)
				lowX = bounds.x;
			if (lowY > bounds.y)
				lowY = bounds.y;
			if (highX < bounds.x + bounds.width)
				highX = bounds.x + bounds.width;
			if (highY < bounds.y + bounds.height)
				highY = bounds.y + bounds.height;
		}
		
		return new Rectangle2D.Double(lowX, lowY, highX - lowX, highY - lowY);
	}
	
	private static Point getButtonPoint(double curDegree, double degIncrement, int menuRadius)
	{
		double a = (curDegree + degIncrement/2.0) * DEG2RAD;
		
		return new Point(
				((int) (0/2 + Math.cos(a)*menuRadius)) - ((int)Math.round(menuRadius/4)),
				((int) (0/2 + Math.sin(a)*menuRadius)) - ((int)Math.round(menuRadius/4))
		);
	}
	
	static int getMinimumRadius(int startRadius, int numButtons)
	{
		if(doIconsOverlap(startRadius, numButtons))
		{
			return getMinimumRadius(startRadius+1,numButtons);
		}
		else
		{
			return startRadius;
		}
	}
	
	private static boolean doIconsOverlap(int radius, int numButtons)
	{
		Rectangle[] buttonBounds = new Rectangle[numButtons];
		
		double degIncrement = Math.min(360.0 / (numButtons), DEFAULT_DEGREE_INCREMENT);
	
		double curDegree = START_ANGLE;
		
		for(int i=0;i<numButtons;i++)
		{
			Point pos = getButtonPoint(curDegree,degIncrement,radius);
			buttonBounds[i] = new Rectangle(pos.x,pos.y,CalicoOptions.menu.icon_size,CalicoOptions.menu.icon_size);
			
			curDegree = curDegree + degIncrement;
		}
		
		for(int i=0;i<numButtons;i++)
		{
			for(int j=0;j<numButtons;j++)
			{
				if(i!=j && buttonBounds[i].intersects(buttonBounds[j]))
				{
					return true;
				}
			}
		}
		
		
		return false;
	}
	
	private static Shape getIconSliceBounds(int radius, int numButtons, int buttonNumber)
	{
		double degIncrement = Math.min(360.0 / (numButtons), DEFAULT_DEGREE_INCREMENT);
		
		Arc2D.Double arc = new Arc2D.Double(pieContainer.getGlobalBounds(), PieMenu.START_ANGLE - degIncrement*(buttonNumber+1), degIncrement, Arc2D.PIE);
		
		return arc;
	}
	
	
	
	public static void clearMenu()
	{
		//pieContainer.removeAllChildren();
		//pieContainer.removeFromParent();
		CalicoDraw.removeAllChildrenFromNode(pieContainer);
		CalicoDraw.removeNodeFromParent(pieContainer);
		
		buttonList.clear();
		pieContainer = null;
		
		// given a PieMenuListener, the CGroupController could listen for the "clear" event and respond accordingly.
		if (highlightedGroup != 0l)
		{
			if (CGroupController.exists(highlightedGroup))
			{
				CGroupController.groupdb.get(highlightedGroup).highlight_off();
				CGroupController.groupdb.get(highlightedGroup).highlight_repaint();
			}
			
			highlightedGroup = 0l;
		}

		for (ContextMenu.Listener listener : listeners)
		{
			listener.menuCleared(ContextMenu.PIE_MENU);
		}
	}
	
	public static boolean checkIfCoordIsOnPieMenu(Point point)
	{
		if(pieContainer==null)
		{
			return false; 
		}		
		return pieContainer.getGlobalBounds().contains(point);
	}
	
	public static void clickPieMenuButton(Point point, InputEventInfo ev)
	{		
		if(buttonList.size()==0)
		{
			return;
		}
		
		int numOfPositions = buttonList.size();
		int menuRadius = getMinimumRadius(DEFAULT_MENU_RADIUS,numOfPositions) + 1;
		for(int i=0;i<buttonList.size();i++)
		{
			if (getIconSliceBounds(menuRadius, numOfPositions, i).contains(point))
//			if(getButton(i).checkWithinBounds(point))
			{
				CGroupController.restoreOriginalStroke = false;
				getButton(i).onClick(ev);
				clearMenu();
				//cancel stroke restore now that the user completed an action
				return;
			}
		}
		
		
		// This allows the menus on a HOLD (so they dont go away when you release your hold)
		if(ev.getAction()==InputEventInfo.ACTION_RELEASED)
		{
			logger.trace("PieMenu: Ignorning mouse event because it is a RELEASE event.");
			return;
		}
		
		// TODO: If we get to this point... should we just kill the menu?
		clearMenu();
		
		ev.stop();
//		ev.getMouseEvent().consume();
		
	}
	
	public static boolean isPieMenuActive()
	{
		return (pieContainer!=null);
	}
	
	static int getButtonCount()
	{
		return buttonList.size();
	}
	static PieMenuButton getButton(int i)
	{
		return buttonList.get(i);
	}
	
	public static boolean performingPieMenuAction()
	{
		return PieMenu.isPerformingPieMenuAction;
	}
	
	
}
