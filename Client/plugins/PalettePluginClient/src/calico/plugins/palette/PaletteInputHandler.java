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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import calico.controllers.CCanvasController;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.plugins.palette.menuitems.*;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.nodes.PImage;

public class PaletteInputHandler extends CalicoAbstractInputHandler {
	
	
	PaletteBar paletteBar;
	long paletteUUID;
	long paletteItemUUID = 0;
	PImage ghost;
	int menuItemIndex = -1;
	
	
	public PaletteInputHandler(PaletteBar bar)
	{
		this.paletteBar = bar;
	}

	@Override
	public void actionDragged(InputEventInfo ev) {
		if (ghost != null)
		{
			ghost.setOffset(ev.getGlobalPoint().getX() - ghost.getBounds().width/2, ev.getGlobalPoint().getY() - ghost.getBounds().height/2);
			CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getCamera().repaint();
		}
		
	}

	@Override
	public void actionPressed(InputEventInfo ev) { 
		paletteItemUUID = 0;
		menuItemIndex = -1;
		
		for (int i = 0; i < paletteBar.getChildrenCount(); i++)
		{
			if (paletteBar.getChild(i) instanceof PaletteBarItem
					&& ((PaletteBarItem)paletteBar.getChild(i)).getGlobalBounds().contains(ev.getGlobalPoint()))
			{
				PaletteBarItem item = (PaletteBarItem)paletteBar.getChild(i);
				paletteItemUUID = item.getUUID();
				if (paletteItemUUID == 0)
					continue;
				
				BufferedImage img = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
				img.getGraphics().setColor(Color.black);
				img.getGraphics().fillRect(0, 0, 30, 30);
				ghost = new PImage();
				ghost.setImage(item.getImage());
				ghost.setBounds(item.getBounds());
				ghost.setOffset(ev.getGlobalPoint().getX() - ghost.getBounds().width/2, ev.getGlobalPoint().getY() - ghost.getBounds().height/2);
				ghost.setTransparency(1.0f);
				CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getCamera().addChild(ghost);
				ghost.setVisible(true);
				ghost.repaint();
				return;	
			}
			else if(paletteBar.getChild(i) instanceof PaletteBarMenuItem
					&& ((PaletteBarMenuItem)paletteBar.getChild(i)).getGlobalBounds().contains(ev.getGlobalPoint()))
			{
				
				menuItemIndex = i;
			}
		}
		
	}

	@Override
	public void actionReleased(InputEventInfo ev) {
		if (paletteItemUUID > 0)
		{
			PalettePlugin.pastePaletteItem(PalettePlugin.getActivePaletteUUID(), this.paletteItemUUID, CCanvasController.getCurrentUUID(), ev.getX(), ev.getY());
			paletteItemUUID = 0;
			CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getCamera().removeChild(ghost);
			ghost = null;
		}
		else if (menuItemIndex > -1)
		{
			((PaletteBarMenuItem)paletteBar.getChild(menuItemIndex)).onClick(ev);
			menuItemIndex = -1;
			
		}
		CalicoInputManager.unlockHandlerIfMatch(paletteBar.getUUID());

	}
	
}
