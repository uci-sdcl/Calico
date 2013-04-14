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
import calico.components.piemenu.PieMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.InputEventInfo;
import edu.umd.cs.piccolo.nodes.PImage;

public class GroupIncreaseSizeButton extends PieMenuButton
{
	
	long uuid;
	public static int SHOWON = 0; //PieMenuButton.SHOWON_SCRAP_MENU;
	
	public GroupIncreaseSizeButton(long u)
	{
		super("group.increase");
		uuid = u;
	}
	
	public void onClick(InputEventInfo ev)
	{
		long canvasUUID = CGroupController.groupdb.get(uuid).getCanvasUID();
		PImage ghost = new PImage();
		ghost.setImage(CGroupController.groupdb.get(uuid).getFamilyPicture());
		//ghost.setBounds(CGroupController.groupdb.get(uuid).getBounds().getBounds2D());
		CalicoDraw.setNodeBounds(ghost, CGroupController.groupdb.get(uuid).getBounds().getBounds2D());
		//CCanvasController.canvasdb.get(canvasUUID).getLayer().addChild(ghost);
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(canvasUUID).getLayer(), ghost);
		
		ResizeMouseListener resizeDragListener = new ResizeMouseListener(ghost, canvasUUID, uuid);
		CCanvasController.canvasdb.get(canvasUUID).addMouseListener(resizeDragListener);
		CCanvasController.canvasdb.get(canvasUUID).addMouseMotionListener(resizeDragListener);
		
		//pass click event on to this listener since it will miss it
		resizeDragListener.mousePressed(ev.getPoint());
		
		ev.stop();
		BubbleMenu.isPerformingBubbleMenuAction = true;
		
		
		Calico.logger.debug("CLICKED GROUP SCALE BUTTON");

	}
	
	private class ResizeMouseListener implements MouseMotionListener, MouseListener
	{
		PImage ghost;
		Point2D.Double prevPoint, mouseDownPoint;
		Point2D.Double scrapMidPoint;
		long cuuid, guuid;
		
		public ResizeMouseListener(PImage g, long canvasUUID, long groupUUID)  {
			ghost = g;
			prevPoint = new Point2D.Double();
			cuuid = canvasUUID;
			guuid = groupUUID;
			Point2D mp = CGroupController.groupdb.get(guuid).getMidPoint();
			scrapMidPoint = new Point2D.Double(mp.getX(), mp.getY());
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			double oldScale = getScaleMP(prevPoint);
			double newScale = getScaleMP(getPoint2D(e.getPoint()));
			double scale = newScale/oldScale;
			ghost.scaleAboutPoint(scale, scrapMidPoint);
			
			prevPoint.x = e.getPoint().getX();
			prevPoint.y = e.getPoint().getY();
			e.consume();
		}
		

		@Override
		public void mouseMoved(MouseEvent e) { e.consume(); }		
		@Override
		public void mouseClicked(MouseEvent e) { e.consume(); }	
		@Override
		public void mouseEntered(MouseEvent e) { e.consume(); }	
		@Override
		public void mouseExited(MouseEvent e) { e.consume(); }
		@Override
		public void mousePressed(MouseEvent e) { e.consume(); }
			
		public void mousePressed(Point p ) {
			prevPoint.x = p.getX();
			prevPoint.y = p.getY();
			mouseDownPoint = new Point2D.Double(p.getX(), p.getY()); 
			
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {
			CCanvasController.canvasdb.get(cuuid).removeMouseListener(this);
			CCanvasController.canvasdb.get(cuuid).removeMouseMotionListener(this);
			//CCanvasController.canvasdb.get(cuuid).getLayer().removeChild(ghost);
			CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(cuuid).getLayer(), ghost);
			
			double scale = getScaleMP(getPoint2D(e.getPoint()));
			double newScale = scale * CGroupController.groupdb.get(guuid).getScale();
			CGroupController.scale(guuid, newScale, newScale);
			
			e.consume();
//			PieMenu.isPerformingPieMenuAction = false;
		}
		
		private double getScaleMP(Point2D.Double p)
		{
			double originalDistance = Math.sqrt(Math.pow(mouseDownPoint.getY() - scrapMidPoint.getY(), 2) 
											+ Math.pow(mouseDownPoint.getX() - scrapMidPoint.getX(), 2));
			double newDistance = Math.sqrt(Math.pow(p.getY() - scrapMidPoint.getY(), 2) 
					+ Math.pow(p.getX() - scrapMidPoint.getX(), 2));
			
			return newDistance / originalDistance;
		}
		
		private Point2D.Double getPoint2D(Point p)
		{
			return new Point2D.Double(p.getX(), p.getY());
		}

	}
	
}
