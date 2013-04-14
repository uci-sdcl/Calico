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
package calico.components.menus;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

import calico.*;
import calico.components.*;
import calico.components.grid.*;
import calico.components.menus.buttons.*;
import calico.controllers.CCanvasController;
import calico.iconsets.CalicoIconManager;
import calico.input.CInputMode;
import calico.inputhandlers.*;
import calico.modules.*;
import calico.networking.*;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolox.nodes.*;
import edu.umd.cs.piccolox.pswing.*;

import java.net.*;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import edu.umd.cs.piccolo.event.*;



public class CanvasMenuBar extends CanvasGenericMenuBar
{
	private static final long serialVersionUID = 1L;
	
	private long cuid = 0L;
	
	private PImage lockButton;
	int setLock_button_array_index;
	Rectangle setLock_bounds;
	
	private PImage clients;
	
	private static ObjectArrayList<Class<?>> externalButtons = new ObjectArrayList<Class<?>>();
	private static ObjectArrayList<Class<?>> externalButtonsPreAppended = new ObjectArrayList<Class<?>>();
	private static ObjectArrayList<Class<?>> externalButtons_rightAligned = new ObjectArrayList<Class<?>>();
	
	public CanvasMenuBar(long c, int screenPos)
	{		
		super(screenPos, CCanvasController.canvasdb.get(c).getBounds());		
		
		cuid = c;
		
		Rectangle rect_default = new Rectangle(0,0,20,20);
		
		addCap(CanvasGenericMenuBar.ALIGN_START);
		
		try
		{
			for (Class<?> button : externalButtonsPreAppended)
			{
				if (button.getName().compareTo(SpacerButton.class.getName()) == 0)
					addSpacer();
				else
					addIcon((CanvasMenuButton) button.getConstructor(long.class).newInstance(cuid));
			}
			

			
			
			addIcon(new ClearButton(cuid));
			addSpacer();

			for(int i=0;i<CalicoOptions.menu.colorlist.length;i++)
			{
				addIcon(new MBColorButton(cuid, CalicoOptions.menu.colorlist[i], CalicoOptions.menu.colorlist_icons[i], rect_default));
			}
			addSpacer();
			

			addIcon(new MBModeChangeButton(cuid, CInputMode.POINTER));
			addSpacer();
			
			for(int i=0;i<CalicoOptions.menu.pensize.length;i++)
			{
				addIcon(new MBSizeButton(cuid, CalicoOptions.menu.pensize[i], CalicoOptions.menu.pensize_icons[i], rect_default));
			}
	
			addSpacer();
			
			// Mode buttons
			addIcon(new MBModeChangeButton(cuid, CInputMode.DELETE));
			addSpacer();
			addIcon(new UndoButton(cuid));
			addIcon(new RedoButton(cuid));
			
	
					
			addSpacer();
			addIcon(new TextCreateButton(cuid));
			addIcon(new ImageCreateButton(cuid));
//			addIcon(new CanvasViewScrapCreateButton(cuid));
		
			for (Class<?> button : externalButtons)
			{
				addSpacer();
				addIcon((CanvasMenuButton) button.getConstructor(long.class).newInstance(cuid));
			}
			
			for (Class<?> button : externalButtons_rightAligned)
			{
				addSpacer(ALIGN_END);
				addIconRightAligned((CanvasMenuButton) button.getConstructor(long.class).newInstance(cuid));
			}
			
			centerIconsVertically();

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		/*
		SwingUtilities.invokeLater(
				new Runnable() { public void run() {
					double lowest = Integer.MAX_VALUE;
					double highest = Integer.MIN_VALUE;
					for (int i = 0; i < button_array_index; i++)
					{
						PNode child = button_array[i];
						if (child.getBounds().y < lowest)
							lowest = child.getBounds().y;
						if (child.getBounds().y + child.getBounds().height > highest)
							highest = child.getBounds().y + child.getBounds().height;
					}
					
					final double delta = getBounds().y + getBounds().height / 2 - (lowest + (highest - lowest)/2);
					
					for (int i = 0; i < button_array_index; i++)
					{
						button_array[i].translate(0, delta);
						rect_array[i].translate(0, (new Double(delta)).intValue());
					}
				}});


		*/

		CalicoDraw.invalidatePaint(this);
	}
	
	private void setLock()
	{
		String text = (CCanvasController.canvasdb.get(cuid).getLockValue())?"DO NOT ERASE":"   CAN ERASE   ";
		
		
		
		setLock(text, new Font("Verdana", Font.BOLD, 12),
				new CanvasTextButton(cuid) {
			public void actionMouseClicked(InputEventInfo event, Rectangle boundingBox) {
				if (event.getAction() == InputEventInfo.ACTION_PRESSED)
				{
					isPressed = true;
				}
				else if (event.getAction() == InputEventInfo.ACTION_RELEASED && isPressed)
				{
					boolean lockValue = CCanvasController.canvasdb.get(cuid).getLockValue();
					long time = (new Date()).getTime();
					CCanvasController.lock_canvas(cuid, !lockValue, CalicoDataStore.Username, time);
	//				setLock();
					CCanvasController.canvasdb.get(cuid).drawMenuBars();
					
					isPressed = false;
				}
			}
		});
		
	}
	
	protected void setLock(String text, Font font, CanvasTextButton buttonHandler)
	{
		/*PText gct = new PText(text);
		gct.setConstrainWidthToTextWidth(true);
		gct.setConstrainHeightToTextHeight(true);
		gct.setFont(font);//new Font("Monospaced", Font.BOLD, 20));
		Rectangle rect_coordtxt = addIcon(gct.getBounds().getBounds());
		gct.setBounds(rect_coordtxt);
		addChild(0,gct);
		*/
		
		if (lockButton != null)
		{
			//removeChild(lockButton);
			CalicoDraw.removeChildFromNode(this, lockButton);
		}
		
		Image img = getTextImage(text,font);

		int imgSpan = 0;
		switch (this.position)
		{
			case POSITION_TOP:
			case POSITION_BOTTOM:
				imgSpan = img.getWidth(null);
				break;
			case POSITION_LEFT:
			case POSITION_RIGHT:
				imgSpan = img.getHeight(null);
				break;
		}		
		
		if (setLock_bounds == null)
			setLock_bounds = addIcon(imgSpan);
		
		lockButton = new PImage();
		
		lockButton.setImage(img);
		
		lockButton.setBounds(setLock_bounds);
		
		super.text_rect_array[setLock_button_array_index] = setLock_bounds;

		text_button_array[setLock_button_array_index] = buttonHandler;
		
		//addChild(0,lockButton);
		CalicoDraw.addChildToNode(this, lockButton, 0);
	}
	
	private void changeLock()
	{
		
	}
	
	
	
	
	public void redrawColorIcon()
	{
		
	}
	
	
	public void redrawArrowIndicator()
	{

	}

	public void redrawClients() {
		
		Rectangle bounds = clients.getBounds().getBounds();
		
		
		
		PImage newClients = new PImage();
		newClients.setImage(getTextImage(CCanvasController.canvasdb.get(cuid).getClients().length+" clients", 
				new Font("Verdana", Font.BOLD, 12)));
		newClients.setBounds(bounds);
		//addChild(0, newClients);
		CalicoDraw.addChildToNode(this, newClients, 0);
		//removeChild(clients);
		CalicoDraw.removeChildFromNode(this, clients);
		
		clients = newClients;
		
	}
	
	public static void addMenuButtonPreAppend(Class<?> button)
	{
		externalButtonsPreAppended.add(button);
	}
	
	public static void addMenuButton(Class<?> button)
	{
		externalButtons.add(button);
	}
	
	public static void addMenuButtonRightAligned(Class<?> button)
	{
		externalButtons_rightAligned.add(button);
	}
	
	public static void removeMenuButton(Class<?> button)
	{
		externalButtons.remove(button);
	}
	
		
}
