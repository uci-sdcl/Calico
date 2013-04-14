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
import calico.CalicoDraw;
import calico.components.CGroup;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.decorators.CListDecorator;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.inputhandlers.InputEventInfo;
import edu.umd.cs.piccolo.nodes.PImage;

public class GroupResizeButton extends PieMenuButton
{
	
	PImage ghost;
	Point2D.Double prevPoint, mouseDownPoint, mouseUpPoint;
	Point2D.Double centerPoint;
	long cuuid;
	boolean isListItem;
	public static int SHOWON = PieMenuButton.SHOWON_SCRAP_CREATE | PieMenuButton.SHOWON_SCRAP_MENU;
	private boolean isActive = false;
	
	public GroupResizeButton(long u)
	{
		super("group.resize");
		draggable = true;
		this.uuid = u;
	}
	
	public void onPressed(InputEventInfo ev)
	{	
		super.onPressed(ev);
		if (!CGroupController.exists(uuid) || isActive)
		{
			return;
		}
		
		isActive = true;
		
		cuuid = CGroupController.groupdb.get(uuid).getCanvasUID();
		ghost = new PImage();
		ghost.setImage(CGroupController.groupdb.get(uuid).getFamilyPicture());
		//ghost.setBounds(CGroupController.groupdb.get(uuid).getBounds().getBounds2D());
		CalicoDraw.setNodeBounds(ghost, CGroupController.groupdb.get(uuid).getBounds().getBounds2D());
//		ghost.scale(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().getScale());
		
		//CCanvasController.canvasdb.get(canvasUUID).getLayer().addChild(ghost);
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(cuuid).getLayer(), ghost);
		
		Point2D cp = CGroupController.groupdb.get(uuid).getMidPoint();
		centerPoint = new Point2D.Double(cp.getX(), cp.getY());
		prevPoint = new Point2D.Double();
		
		isListItem = false;
		CGroup cGroup = CGroupController.groupdb.get(uuid);
		while(cGroup.getParentUUID() != 0l)
		{
			cGroup = CGroupController.groupdb.get(cGroup.getParentUUID());
			if (cGroup instanceof CListDecorator)
			{
				isListItem = true;
				break;
			}
		}
		
		//BubbleMenu.setSelectedButton(GroupRotateButton.class.getName());
		Point scaledPoint = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getUnscaledPoint(ev.getPoint());
		
		prevPoint.x = scaledPoint.getX();
		prevPoint.y = scaledPoint.getY();
		mouseDownPoint = new Point2D.Double(scaledPoint.getX(), scaledPoint.getY()); 
		
		if (!isListItem)
		{
			CGroupController.move_start(uuid);
		}
		
		
		/*RotateMouseListener rotateDragListener = new RotateMouseListener(ghost, cuuid, guuid);
		CCanvasController.canvasdb.get(cuuid).addMouseListener(rotateDragListener);
		CCanvasController.canvasdb.get(cuuid).addMouseMotionListener(rotateDragListener);

		
		//pass click event on to this listener since it will miss it
		rotateDragListener.mousePressed(ev.getPoint());*/
		
		ev.stop();
		//BubbleMenu.isPerformingBubbleMenuAction = true;
		
		
		Calico.logger.debug("CLICKED GROUP ROTATE BUTTON");
	}
	
	public void onDragged(InputEventInfo ev)
	{
		Point scaledPoint = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getUnscaledPoint(ev.getPoint());
		
		Point2D.Double p = new Point2D.Double(scaledPoint.getX(), scaledPoint.getY());
		/*double angle = getAngle(prevPoint, p, centerPoint);
		ghost.rotateAboutPoint(angle, centerPoint);*/
		
		double oldScale = getScaleMP(prevPoint);
		double newScale = getScaleMP(p);
		double scale = newScale/oldScale;
		ghost.scaleAboutPoint(scale, centerPoint);

		//ghost.repaintFrom(ghost.getBounds(), ghost);
		CalicoDraw.repaintNode(ghost);

		BubbleMenu.moveIconPositions(ghost.getFullBounds());
		
		
		prevPoint.x = scaledPoint.getX();
		prevPoint.y = scaledPoint.getY();
		ev.stop();
	}
	
	public void onReleased(InputEventInfo ev)
	{
		Point scaledPoint = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getUnscaledPoint(ev.getPoint());
		
		mouseUpPoint = new Point2D.Double(scaledPoint.getX(), scaledPoint.getY());

		//CCanvasController.canvasdb.get(cuuid).getLayer().removeChild(ghost);
		CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(cuuid).getLayer(), ghost);
		
		/*double angle = getAngle(mouseDownPoint, mouseUpPoint, centerPoint);
		CGroupController.rotate(guuid, angle);*/
		
		//Turn off highlighter before resize to make sure it does not leave artifacts. 
		CGroupController.groupdb.get(uuid).highlight_off();
		CGroupController.groupdb.get(uuid).highlight_repaint();
		
		double scale = getScaleMP(mouseUpPoint);
		CGroupController.scale(uuid, scale, scale);
		if (!isListItem)
		{
			CGroupController.move_end(this.uuid, ev.getX(), ev.getY()); 
		}
		
		//Turn highlighter back on for resized version
		CGroupController.groupdb.get(uuid).highlight_on();
		
		ev.stop();
//			PieMenu.isPerformingPieMenuAction = false;
		
		if(!CGroupController.groupdb.get(uuid).isPermanent())
		{
			//CGroupController.drop(guuid);
		}
		
		isActive = false;
	}
	
	
	
	private double getScaleMP(Point2D.Double p)
	{
		double originalDistance = Math.sqrt(Math.pow(mouseDownPoint.getY() - centerPoint.getY(), 2) 
										+ Math.pow(mouseDownPoint.getX() - centerPoint.getX(), 2));
		double newDistance = Math.sqrt(Math.pow(p.getY() - centerPoint.getY(), 2) 
				+ Math.pow(p.getX() - centerPoint.getX(), 2));
		
		return newDistance / originalDistance;
	}
	
}
