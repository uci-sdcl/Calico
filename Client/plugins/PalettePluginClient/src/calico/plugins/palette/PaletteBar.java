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
package calico.plugins.palette;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;

import calico.CalicoDataStore;
import calico.components.menus.CanvasStatusBar;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.StickyItem;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.plugins.palette.menuitems.*;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolox.nodes.PComposite;

public class PaletteBar extends PComposite 
	implements CalicoEventListener, StickyItem {
	
	int xLoc = 50, yLoc = 50;
	int itemBuffer = 5;
	int defaultHeight = 30;
	long uuid;
	private boolean menuBarIconsVisible = false;
	
	private int maxChildrenPerRow = 15;
	
//	Palette palette;
	
	
	public PaletteBar(Palette p)
	{
//		this.palette = p;
		this.uuid = calico.Calico.uuid();
		
		setupPaletteItems();
//		CalicoEventHandler.getInstance().addListenerForType("PALETTE", this, CalicoEventHandler.PASSIVE_LISTENER);
		for (Integer event : PalettePlugin.getNetworkCommands(PaletteNetworkCommands.class))
			CalicoEventHandler.getInstance().addListener(event.intValue(), this, CalicoEventHandler.PASSIVE_LISTENER);
		
		CalicoInputManager.addCustomInputHandler(getUUID(), new PaletteInputHandler(this));
		CalicoInputManager.registerStickyItem(this);
	}

	private void setupPaletteItems() {

		this.removeAllChildren();
		ArrayList<PNode> newChildren = new ArrayList<PNode>();
		

		
		if (true)
		{
			
			newChildren.add(new NewPalette());
//			newChildren.add(new SavePalette());
//			newChildren.add(new OpenPalette());
			newChildren.add(new ClosePalette());
//			newChildren.add(new ImportImages());
//			newChildren.add(new AddCanvasToPalette());	
//			newChildren.add(new HideMenuBarIcons());
		}
		else
		{
//			newChildren.add(new ShowMenuBarIcons());
		}
		
		newChildren.add(new ShiftToLeftPalette());
		
		newChildren.add(new ShiftToRightPalette());
		
		
		newChildren.addAll(addPaletteItems(newChildren.size()));
		
		
		this.addChildren(newChildren);
		
		int height = itemBuffer + (defaultHeight + itemBuffer) * (int) Math.ceil(((double)newChildren.size()) / this.maxChildrenPerRow);
		
		this.yLoc = (int)(CalicoDataStore.ScreenHeight 
				- CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).statusBar.getBoundsReference().getHeight() 
				- 50
				- height);
		
		int numItemsToShowPerRow = /*(newChildren.size() < this.maxChildrenPerRow) ? newChildren.size() :*/ this.maxChildrenPerRow;
		int widthOfItems = itemBuffer + (PalettePlugin.PALETTE_ITEM_WIDTH + itemBuffer) * numItemsToShowPerRow;
		
		
		setBounds(xLoc, yLoc, widthOfItems, height);
		

		layoutChildren();
//		if (CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()) != null)
//		{
//			double y = CalicoDataStore.ScreenHeight - this.getBoundsReference().getHeight() - CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).statusBar.getBoundsReference().getHeight() - 50;
//			this.translate(0, -1 * getY() + y);
//			this.setBounds(this.getBoundsReference().getX(), y, this.getBoundsReference().getWidth(), this.getBoundsReference().getHeight());
//		}
		this.setPaint(Color.GRAY);
	}
	
	private ArrayList<PNode> addPaletteItems(int numMenuItems) {

		
		ArrayList<PNode> ret = new ArrayList<PNode>();
		ArrayList<CalicoPacket> paletteItems = PalettePlugin.getActivePalette().getPaletteItems();
		int itemsToShow = (int) Math.ceil((double)(paletteItems.size() + numMenuItems) / this.maxChildrenPerRow) * this.maxChildrenPerRow - numMenuItems;
		for (int i = 0; i < itemsToShow; i++)
		{
			PNode item;
			if (paletteItems.size() > i)
			{
				CalicoPacket packet = paletteItems.get(i);
				packet.rewind();
				int comm = packet.getInt();
				
				if (comm != PaletteNetworkCommands.PALETTE_PACKET)
				{
					item = new PaletteBarItem(0l, 0l, null);
					continue;
				}
				
				long uuid = packet.getLong();
				
				long paletteUUID = packet.getLong();
				
				Image paletteItemImage = packet.getBufferedImage();
				
				item = new PaletteBarItem(this.getUUID(), uuid, paletteItemImage);
			}
			else
			{
				item = new PaletteBarItem(0l, 0l, null);
			}
			ret.add(item);
		}
		return ret;
	}
	
	public void layoutChildren() {
		double xOffset = xLoc + itemBuffer;
		double yOffset = yLoc + itemBuffer;
			
		Iterator i = getChildrenIterator();
		int itemPerRowCounter = 0;
		
		while (i.hasNext()) {
			PNode each = (PNode) i.next();
			each.setOffset(xOffset - each.getX(), yOffset);
			xOffset += each.getFullBoundsReference().getWidth() + itemBuffer;
			
			itemPerRowCounter++;
			if (itemPerRowCounter >= this.maxChildrenPerRow)
			{
				itemPerRowCounter = 0;
				yOffset += defaultHeight + itemBuffer;
				xOffset = xLoc + itemBuffer;
			}
		}
	}
	
	public boolean visible()
	{
		long cuid = CCanvasController.getCurrentUUID();
		PCamera camera = CCanvasController.canvasdb.get(cuid).getCamera();
		
		int paletteIndex = -1;
		for (int i = 0; i < camera.getChildrenCount(); i++)
			if (camera.getChild(i) instanceof PaletteBar
				&& ((PaletteBar)camera.getChild(i)).getUUID() == this.getUUID())
				paletteIndex = i;
		
		return paletteIndex != -1 && super.getVisible();
	}
	
	public long getUUID()
	{
		return uuid;
	}

	@Override
	public void handleCalicoEvent(int event, CalicoPacket p) {
		if (event == PaletteNetworkCommands.PALETTE_PACKET)
		{
			p.rewind();
			p.getInt();
			long itemUUID = p.getLong();
			long paletteUUID = p.getLong();
			setupPaletteItems();
			repaint();
		}
		else if (event == PaletteNetworkCommands.PALETTE_SWITCH_VISIBLE_PALETTE)
		{
			setupPaletteItems();
			repaint();
		}
		else if (event == PaletteNetworkCommands.PALETTE_HIDE_MENU_BAR_ICONS)
		{
			hideMenuBarIcons();
		}
		else if (event == PaletteNetworkCommands.PALETTE_SHOW_MENU_BAR_ICONS)
		{
			showMenuBarIcons();
		}
		else if (event == PaletteNetworkCommands.PALETTE_PASTE_ITEM)
		{
			CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).repaint();
		}
		
//		if (PalettePlugin.getActivePalette().contains(itemUUID))
		
		
	}

	@Override
	public boolean containsPoint(Point p) {
		return getGlobalBounds().contains(p);
	}
	
	public void setVisible(boolean visible)
	{
		if (visible)
			CalicoInputManager.registerStickyItem(this);
		else
			CalicoInputManager.unregisterStickyItem(this);
		
		this.setupPaletteItems();
		
		super.setVisible(visible);
	}
	
	public void hideMenuBarIcons()
	{
		menuBarIconsVisible = false;
//		Image initialImage = this.toImage();
//		final BufferedImage bInitialImage = new BufferedImage(initialImage.getWidth(null), initialImage.getHeight(null), BufferedImage.TYPE_INT_ARGB);
//		bInitialImage.getGraphics().drawImage(initialImage, 0, 0, null);
//		
//		PActivity flash = new PActivity(500,70, System.currentTimeMillis()) {
//			long step = 0;
//      
//		    protected void activityStep(long time) {
//		            super.activityStep(time);
//		            float t = 1.0f - 1.0f * step/5;
//		            
//		            //perform drawing here
//		            
//		            step++;
//		            if (t <= 0)
//		            	terminate();
//		    }
//		    
//		    protected void activityFinished() {
//
//		    }
//		};
//		
//		if (getRoot() != null)
//			getRoot().addActivity(flash);
		setupPaletteItems();
		repaint();
		
		
	}
	
	public void showMenuBarIcons()
	{
		menuBarIconsVisible = true;
		setupPaletteItems();
		repaint();
	}
	
	public boolean MenuBarIconsVisible()
	{
		return menuBarIconsVisible;
	}
	

	

	
	

}
