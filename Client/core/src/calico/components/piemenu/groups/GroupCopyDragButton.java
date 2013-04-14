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
package calico.components.piemenu.groups;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;

import calico.Calico;
import calico.components.CCanvas;
import calico.components.CGroup;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.bubblemenu.BubbleMenuButton;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.inputhandlers.InputEventInfo;
import calico.networking.Networking;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import edu.umd.cs.piccolo.nodes.PImage;

public class GroupCopyDragButton extends PieMenuButton
{
	public static int SHOWON = PieMenuButton.SHOWON_SCRAP_CREATE | PieMenuButton.SHOWON_SCRAP_MENU;
	private long new_guuid = 0L;
	private boolean isActive = false;
	Point prevPoint, mouseDownPoint;
	long cuuid, oguuid;
	private Point mouseDownPoint2; 
	
	public GroupCopyDragButton(long uuid)
	{
		super("group.copy");
		draggable = true;
		this.uuid = uuid;
	}
	
	public void onPressed(InputEventInfo ev)
	{
		if (!CGroupController.exists(uuid) || isActive)
		{
			return;
		}
		
		isActive = true;

		long old_guuid = uuid;
		//This updates the current guuid
		BubbleMenu.updateGroupUUID(CGroupController.copy_to_canvas(old_guuid));

		CGroupController.groupdb.get(old_guuid).highlight_off();
		CGroupController.groupdb.get(old_guuid).highlight_repaint();
		if (!CGroupController.groupdb.get(old_guuid).isPermanent())
		{
			CGroupController.drop(old_guuid);
			CGroupController.setCurrentUUID(uuid);
			CGroupController.setLastCreatedGroupUUID(uuid);
		}
		
		
		
			
		CGroupController.groupdb.get(uuid).highlight_on();
		CGroupController.groupdb.get(uuid).highlight_repaint();
		
		
		long canvasUUID = CGroupController.groupdb.get(uuid).getCanvasUID();
		
		/*TranslateMouseListener resizeDragListener = new TranslateMouseListener(canvasUUID, guuid, new_guuid);
		CCanvasController.canvasdb.get(canvasUUID).addMouseListener(resizeDragListener);
		CCanvasController.canvasdb.get(canvasUUID).addMouseMotionListener(resizeDragListener);
		
		//pass click event on to this listener since it will miss it
		resizeDragListener.mousePressed(ev.getPoint());*/
		
		prevPoint = new Point(0, 0);
		mouseDownPoint = null;
		mouseDownPoint2 = ev.getPoint();
		cuuid = canvasUUID;

		super.onPressed(ev);
		ev.stop();
		
		System.out.println("CLICKED GROUP COPY BUTTON");
	}
	
	public void onDragged(InputEventInfo ev)
	{
		if (mouseDownPoint == null)
		{
			prevPoint.x = ev.getPoint().x;
			prevPoint.y = ev.getPoint().y;
			mouseDownPoint = ev.getPoint();
			CGroupController.move_start(uuid);
		}
		
		CGroupController.move(uuid, (int)(ev.getPoint().x - prevPoint.x), ev.getPoint().y - prevPoint.y);
		
		long smallestParent = CGroupController.groupdb.get(uuid).calculateParent(ev.getPoint().x, ev.getPoint().y);
		if (smallestParent != BubbleMenu.highlightedParentGroup)
		{
			if (BubbleMenu.highlightedParentGroup != 0l)
			{
				CGroupController.groupdb.get(BubbleMenu.highlightedParentGroup).highlight_off();
				CGroupController.groupdb.get(BubbleMenu.highlightedParentGroup).highlight_repaint();
			}
			if (smallestParent != 0l)
			{
				CGroupController.groupdb.get(smallestParent).highlight_on();
				CGroupController.groupdb.get(smallestParent).highlight_repaint();
			}
			BubbleMenu.highlightedParentGroup = smallestParent;
		}
		
		/*if ((smallestParent = CGroupController.groupdb.get(guuid).calculateParent(e.getPoint().x, e.getPoint().y)) != 0l)
		{
			CGroupController.groupdb.get(smallestParent).highlight_on();
		}*/
		
		prevPoint.x = ev.getPoint().x;
		prevPoint.y = ev.getPoint().y;
		ev.stop();
	}
	
	public void onReleased(InputEventInfo ev)
	{
		if (BubbleMenu.highlightedParentGroup != 0l)
		{
			CGroupController.groupdb.get(BubbleMenu.highlightedParentGroup).highlight_off();
			CGroupController.groupdb.get(BubbleMenu.highlightedParentGroup).highlight_repaint();
			BubbleMenu.highlightedParentGroup = 0l;
		}
		int deltaX = 0;
		int deltaY = 0;
		if (Math.abs(ev.getX() - mouseDownPoint2.getX()) < 5
				&& Math.abs(ev.getY() - mouseDownPoint2.getY()) < 5)
		{
			deltaX = 20;
			deltaY = 20;
			CGroupController.move(uuid, deltaX, deltaY);
		}
		
		//This threw a null pointer exception for some reason...
		if (mouseDownPoint != null)
			CGroupController.move_end(this.uuid, ev.getX() + deltaX, ev.getY() + deltaY); 
		
		//Update the menu location in case it was dropped into a list
		//BubbleMenu.moveIconPositions(CGroupController.groupdb.get(guuid).getBounds());
		super.onReleased(ev);
		ev.stop();
		isActive = false;
	}
		
}
