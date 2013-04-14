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
package calico.inputhandlers.groups;

import java.awt.*;

import calico.*;
import calico.components.*;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.piemenu.*;
import calico.controllers.*;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.*;
import calico.modules.*;
import calico.networking.*;
import calico.networking.netstuff.*;

import java.awt.Color;
import java.awt.geom.*;
import java.util.*;

import org.apache.log4j.Logger;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolox.nodes.PLine;


public class CGroupScrapModeInputHandler extends CalicoAbstractInputHandler
{
	public static Logger logger = Logger.getLogger(CGroupScrapModeInputHandler.class.getName());

	private long uuid = 0L;

	private static final double DRAG_THRESHOLD = 10.0;

	private InputEventInfo lastEvent = null;

	private boolean hasStartedGroup = false;
	private boolean hasStartedBge = false;
	private boolean isRCMove = false;// is right click move?
	private boolean weDidSomething = false;
	public static boolean dragging = false;
	public static boolean startDrag = false;

	private boolean serverNotifiedOfMove = false;

	private InputEventInfo pressPoint = null;

	private Point lastMovePoint = new Point(0,0);



	private boolean hasNotPassedThreshold = true;


	private Polygon draggedCoords = null;

	private CGroupInputHandler parentHandler = null;

	public CGroupScrapModeInputHandler(long u, CGroupInputHandler par)
	{
		uuid = u;
		parentHandler = par;

		canvas_uid = CGroupController.groupdb.get(uuid).getCanvasUID();

//		this.setupModeIcon("mode.scrap");
	}

	public void actionPressed(InputEventInfo e)
	{

		CalicoInputManager.lockInputHandler(uuid);
		weDidSomething = false;


		hasStartedGroup = false;

		if(e.isRightButtonPressed())
		{
			hasNotPassedThreshold = true;
		}
		pressPoint = e;
		lastEvent = e;

//		CalicoInputManager.drawCursorImage(canvas_uid,
//				CalicoIconManager.getIconImage("mode.scrap"), e.getPoint());
//		this.showModeIcon(e.getPoint());
	}


	public void actionDragged(InputEventInfo e)
	{

		if (startDrag || dragging)
		{
			if (startDrag)
			{
				dragging = true;

				CGroupController.move_start(this.uuid);
				serverNotifiedOfMove = true;
				CalicoInputManager.lockInputHandler(this.uuid);
				Point delta = e.getDelta(pressPoint);
				lastMovePoint.translate(delta.x, delta.y);
				CGroupController.move(this.uuid, delta.x, delta.y);
				weDidSomething = true;
				startDrag = false;
			}
			else if(dragging && lastEvent!=null)
			{


				CalicoInputManager.lockInputHandler(this.uuid);
				Point delta = e.getDelta(lastEvent);
				lastMovePoint.translate(delta.x, delta.y);
				CGroupController.move(this.uuid, delta.x, delta.y);
				weDidSomething = true;



			}
			
			long smallestParent = CGroupController.groupdb.get(uuid).calculateParent(e.getPoint().x, e.getPoint().y);
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
		}

		lastEvent = e;

	}


	public void actionReleased(InputEventInfo e)
	{
//		this.hideModeIcon();

		CalicoInputManager.unlockHandlerIfMatch(this.uuid);
		CGroupController.no_notify_unbold(this.uuid);

		if(weDidSomething)
		{
			int x = e.getX();
			int y = e.getY();

			if(dragging)//e.isRightButton())
			{
				// Are we done drawing/moving?
				// DONT COMMENT THIS OUT! We need it to fix parents
				CGroupController.move_end(this.uuid, e.getPoint().x, e.getPoint().y);

				lastMovePoint = new Point(0,0);

				CGroupController.groupdb.get(this.uuid).resetRightClickMode();
			
			}
			else if(hasStartedGroup && e.isLeftButton())
			{
				CGroupController.append(CGroupController.getCurrentUUID(), x, y);


				//CGroupController.set_parent(CGroupController.getCurrentUUID(), this.uuid);


				CGroupController.finish(CGroupController.getCurrentUUID(), true);

				CGroupController.setLastCreatedGroupUUID(CGroupController.getCurrentUUID());

				CGroupController.show_group_piemenu(CGroupController.getCurrentUUID(), e.getPoint(), PieMenuButton.SHOWON_SCRAP_CREATE);
				/*
				PieMenu.displayPieMenu(e.getPoint(), 
						new GroupDeleteButton(CGroupController.getCurrentUUID()), 
						new GroupSetPermanentButton(CGroupController.getCurrentUUID())
						//new GroupChangeChildrenColorButton(CGroupController.getCurrentUUID()),
						//new GroupCopyButton(CGroupController.getCurrentUUID())
				);*/
				hasStartedGroup = false;
			}


			if(!CGroupController.groupdb.get(uuid).isPermanent())
			{
				CGroupController.drop(uuid);
			}


		}
		else if (!weDidSomething)
		{
			CGroupController.show_group_bubblemenu(uuid);
		}
		else if(!weDidSomething && e.isRightButton())
		{
			long stroke = CStrokeController.getPotentialScrap(e.getPoint());
			if (!CGroupController.groupdb.get(uuid).isPermanent())
				stroke = 0;

			if (stroke != 0l)
				CalicoAbstractInputHandler.clickMenu(stroke, 0l, e.getPoint());
			else
				CGroupController.show_group_piemenu(uuid, e.getGlobalPoint());
		}

		serverNotifiedOfMove = false;

		dragging = false;
	}

}
