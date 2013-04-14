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
package calico.components.piemenu.canvas;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import calico.Calico;
import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.components.CArrow;
import calico.components.CCanvas;
import calico.components.arrow.AnchorPoint;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CArrowController;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.inputhandlers.InputEventInfo;

@Deprecated
public class ArrowButton extends PieMenuButton
{
	//Remove from BubbleMenu
	//public static int SHOWON = PieMenuButton.SHOWON_SCRAP_CREATE | PieMenuButton.SHOWON_SCRAP_MENU;	
	
	private long guuid = 0L;
	
	public ArrowButton(long uuid)
	{
		super("mode.arrow");
		guuid = uuid;
	}
	
	public ArrowButton() {
		super("mode.arrow");
	}

	public void onClick(InputEventInfo ev)
	{
		long canvasUUID = CCanvasController.getCurrentUUID();
		
		
		ArrowCreateMouseListener arrowCreateMouseListener = new ArrowCreateMouseListener(canvasUUID, guuid);
		CCanvasController.canvasdb.get(canvasUUID).addMouseListener(arrowCreateMouseListener);
		CCanvasController.canvasdb.get(canvasUUID).addMouseMotionListener(arrowCreateMouseListener);
		
		//pass click event on to this listener since it will miss it
		arrowCreateMouseListener.mousePressed(ev.getPoint());
		
		ev.stop();
//		ev.getMouseEvent().consume();
		BubbleMenu.isPerformingBubbleMenuAction = true;
		
		//CGroupController.drop(group_uuid);
	}
		
	private class ArrowCreateMouseListener implements MouseMotionListener, MouseListener
	{
		Point prevPoint;
		private CArrow tempArrow = null;
		long cuuid;
		
		public ArrowCreateMouseListener(long canvasUUID, long groupUUID)  {
			prevPoint = new Point();
			cuuid = canvasUUID;
			guuid = groupUUID;
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			Point scaledPoint = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getUnscaledPoint(e.getPoint());
			Point scaledStartPoint = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getUnscaledPoint(BubbleMenu.lastOpenedPosition);
			
			if(tempArrow==null)
			{
				// we need to make an arrow
				long auid = Calico.uuid();
				tempArrow = new CArrow(auid,cuuid, CalicoDataStore.PenColor, CArrow.TYPE_NORM_HEAD_B,
					new AnchorPoint(CArrow.TYPE_CANVAS, cuuid, scaledStartPoint),
					new AnchorPoint(CArrow.TYPE_CANVAS, cuuid, scaledPoint)
				);
				//CCanvasController.canvasdb.get(cuuid).getLayer().addChild(tempArrow);
				CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(cuuid).getLayer(), tempArrow);
			}
			else
			{
				tempArrow.setAnchorB(new AnchorPoint(CArrow.TYPE_CANVAS, cuuid, scaledPoint));
			}
			tempArrow.redraw(true);
			
			prevPoint.x = scaledPoint.x;
			prevPoint.y = scaledPoint.y;
			e.consume();
		}
		

		@Override
		public void mousePressed(MouseEvent e) {e.consume(); }
		@Override
		public void mouseMoved(MouseEvent e) { e.consume(); }		
		@Override
		public void mouseClicked(MouseEvent e) { e.consume(); }	
		@Override
		public void mouseEntered(MouseEvent e) { e.consume(); }	
		@Override
		public void mouseExited(MouseEvent e) { e.consume(); }
		
		
		public void mousePressed(Point p) {
			
			Point scaledPoint = CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getUnscaledPoint(p);
			prevPoint = scaledPoint;
			
//			prevPoint.x = e.getPoint().x;
//			prevPoint.y = e.getPoint().y;
//			mouseDownPoint = e.getPoint();
			
//			e.consume();
		}
		
		@Override
		public void mouseReleased(MouseEvent e) {

			CCanvasController.canvasdb.get(cuuid).removeMouseListener(this);
			CCanvasController.canvasdb.get(cuuid).removeMouseMotionListener(this);
			
			if(tempArrow!=null)
			{
				// Now we need to find out what groups its endpoints are on, as well linking its parents
				// Group at First point?
				long guuidA = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), this.tempArrow.getAnchorA().getPoint());
				long guuidB = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), this.tempArrow.getAnchorB().getPoint());
							
				if(guuidA!=0L)
				{
					tempArrow.setAnchorA(new AnchorPoint(CArrow.TYPE_GROUP, tempArrow.getAnchorA().getPoint(), guuidA));
				}
				
				if(guuidB!=0L)
				{
					tempArrow.setAnchorB(new AnchorPoint(CArrow.TYPE_GROUP, tempArrow.getAnchorB().getPoint(), guuidB));
				}
				
				
				CArrowController.start(tempArrow.getUUID(), tempArrow.getCanvasUUID(), tempArrow.getColor(), tempArrow.getArrowType(), 
					tempArrow.getAnchorA(), 
					tempArrow.getAnchorB()
				);
				
				//CCanvasController.canvasdb.get(cuuid).getLayer().removeChild(tempArrow);
				CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(cuuid).getLayer(), tempArrow);
				
				tempArrow = null;
			}
			
			e.consume();
//			PieMenu.isPerformingPieMenuAction = false;
			

		}
	}
}
