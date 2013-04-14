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
import calico.components.bubblemenu.BubbleMenu;
import calico.components.decorators.CListDecorator;
import calico.components.CCanvas;
import calico.components.CGroup;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.inputhandlers.InputEventInfo;
import edu.umd.cs.piccolo.nodes.PImage;

public class GroupRotateButton extends PieMenuButton
{
	
	public static int SHOWON = PieMenuButton.SHOWON_SCRAP_CREATE | PieMenuButton.SHOWON_SCRAP_MENU;
	private boolean isActive = false;
	PImage ghost;
	Point2D.Double prevPoint, mouseDownPoint, mouseUpPoint;
	Point2D.Double centerPoint;
	long cuuid;
	boolean isListItem;
	
	public GroupRotateButton(long u)
	{
		super("group.rotate");
		draggable = true;
		this.uuid = u;
	}
	
	public void onPressed(InputEventInfo ev)
	{	
		if (!CGroupController.exists(uuid) || isActive)
		{
			return;
		}
		
		isActive = true;
		
		long canvasUUID = CGroupController.groupdb.get(uuid).getCanvasUID();
		ghost = new PImage();
		ghost.setImage(CGroupController.groupdb.get(uuid).getFamilyPicture());
		//ghost.setBounds(CGroupController.groupdb.get(uuid).getBounds().getBounds2D());
		CalicoDraw.setNodeBounds(ghost, CGroupController.groupdb.get(uuid).getBounds().getBounds2D());
//		ghost.scale(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().getScale());
		
		//CCanvasController.canvasdb.get(canvasUUID).getLayer().addChild(ghost);
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(canvasUUID).getLayer(), ghost);
		
		/*RotateMouseListener rotateDragListener = new RotateMouseListener(ghost, canvasUUID, guuid);
		CCanvasController.canvasdb.get(canvasUUID).addMouseListener(rotateDragListener);
		CCanvasController.canvasdb.get(canvasUUID).addMouseMotionListener(rotateDragListener);

		
		//pass click event on to this listener since it will miss it
		rotateDragListener.mousePressed(ev.getPoint());*/
		
		Point2D cp = CGroupController.groupdb.get(uuid).getMidPoint();
		centerPoint = new Point2D.Double(cp.getX(), cp.getY());
		prevPoint = new Point2D.Double();
		cuuid = canvasUUID;
		
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
		
		Point scaledPoint = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getUnscaledPoint(ev.getPoint());
		
		prevPoint.x = scaledPoint.getX();
		prevPoint.y = scaledPoint.getY();
		mouseDownPoint = new Point2D.Double(scaledPoint.getX(), scaledPoint.getY()); 
		
		if (!isListItem)
		{
			CGroupController.move_start(uuid);
		}
		
		ev.stop();
		BubbleMenu.isPerformingBubbleMenuAction = true;
		
		
		Calico.logger.debug("CLICKED GROUP ROTATE BUTTON");
	}
	
	public void onDragged(InputEventInfo ev)
	{
		Point scaledPoint = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getUnscaledPoint(ev.getPoint());
		
		Point2D.Double p = new Point2D.Double(scaledPoint.getX(), scaledPoint.getY());
		double angle = getAngle(prevPoint, p, centerPoint);
		ghost.rotateAboutPoint(angle, centerPoint);
		
		/*double oldScale = getScaleMP(prevPoint);
		double newScale = getScaleMP(p);
		double scale = newScale/oldScale;
		ghost.scaleAboutPoint(scale, centerPoint);*/

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
		
		//Turn off highlighter before rotate to make sure it does not leave artifacts. 
		CGroupController.groupdb.get(uuid).highlight_off();
		CGroupController.groupdb.get(uuid).highlight_repaint();
		
		double angle = getAngle(mouseDownPoint, mouseUpPoint, centerPoint);
		CGroupController.rotate(uuid, angle);
		if (!isListItem)
		{
			CGroupController.move_end(this.uuid, ev.getX(), ev.getY()); 
		}
		
		//Turn highlighter back on for rotated version
		CGroupController.groupdb.get(uuid).highlight_on();
		
		/*double scale = getScaleMP(mouseUpPoint);
		CGroupController.scale(guuid, scale, scale);*/

		ev.stop();
//			PieMenu.isPerformingPieMenuAction = false;
		
		if(!CGroupController.groupdb.get(uuid).isPermanent())
		{
			//CGroupController.drop(guuid);
		}
		
		isActive = false;
	}
	
	
	
	//gets angle between two points with respect to the third point
	double getAngle(Point2D point1, Point2D point2, Point2D midPoint)
	{
		Point2D adjustedPoint1 = new Point2D.Double(point1.getX() - midPoint.getX(), point1.getY() - midPoint.getY());
		double point1Angle = getAngle(adjustedPoint1, new Point(0,0));
		
		Point2D adjustedPoint2 = new Point2D.Double(point2.getX() - midPoint.getX(), point2.getY() - midPoint.getY());
		double point2Angle = getAngle(adjustedPoint2, new Point(0,0));
		
		double angle = point1Angle - point2Angle;
		
		return angle;			
	}
	
	//taken from: http://bytes.com/topic/c/answers/452165-finding-angle-between-two-points#post1728631
	double getAngle(Point2D point1, Point2D point2 )
	{
		double theta;
		if ( point2.getX() - point1.getX() == 0 )
			if ( point2.getY() > point1.getY() )
				theta = 0;
			else
				theta = Math.PI;
		else
		{
			theta = Math.atan( (point2.getY() - point1.getY()) / (point2.getX() - point1.getX()) );
			if ( point2.getX() > point1.getX() )
				theta = Math.PI / 2.0f - theta;
			else
				theta = Math.PI * 1.5f - theta;
		};
		return theta;
	}
	
}
