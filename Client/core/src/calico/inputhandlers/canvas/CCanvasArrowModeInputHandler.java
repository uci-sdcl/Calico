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
package calico.inputhandlers.canvas;

import calico.*;

import calico.components.*;
import calico.components.arrow.AnchorPoint;
import calico.components.arrow.CArrow;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.decorators.CGroupDecorator;
import calico.components.menus.*;
import calico.components.piemenu.*;
import calico.controllers.CArrowController;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.CCanvasInputHandler;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.networking.*;
import calico.networking.netstuff.*;


import java.awt.geom.*;
import java.awt.*;

import java.util.*;

import javax.swing.SwingUtilities;

import org.apache.log4j.*;

import edu.umd.cs.piccolo.event.*;


// implements PenListener
public class CCanvasArrowModeInputHandler extends CalicoAbstractInputHandler
{
	public static Logger logger = Logger.getLogger(CCanvasArrowModeInputHandler.class.getName());

	public static final double CREATE_GROUP_MIN_DIST = 15.0;
	
	

	@SuppressWarnings("unused")
	private InputEventInfo lastEvent = null;
	
	
	@SuppressWarnings("unused")
	private CCanvasInputHandler parentHandler = null;
	
	private Point pressedPoint = null;
	private boolean hasBrokenDistanceThreshold = false;
	
	
	private CArrow tempArrow = null;
	private long tempGuuidA = 0l;
	private long tempGuuidB = 0l;

	public CCanvasArrowModeInputHandler(long cuid, CCanvasInputHandler parent)
	{
		canvas_uid = cuid;
		parentHandler = parent;
//		this.setupModeIcon("mode.arrow");
	}

	public void actionPressed(InputEventInfo e)
	{
		
		if(e.isLeftButtonPressed())
		{
			pressedPoint = e.getPoint();
			hasBrokenDistanceThreshold = false;
			
			if (BubbleMenu.isBubbleMenuActive())
			{
				long guuid = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), e.getPoint());
				if (guuid != BubbleMenu.activeUUID)
				{
					BubbleMenu.clearMenu();
				}
			}
		}
//		this.showModeIcon(e.getPoint());
		CalicoInputManager.drawCursorImage(canvas_uid,
				CalicoIconManager.getIconImage("mode.arrow"), e.getPoint());
		
		lastEvent = e;
	}

	
	public void actionDragged(InputEventInfo e)
	{
//		this.hideModeIcon(e.getPoint());

		CalicoInputManager.lockInputHandler(canvas_uid);

		if(e.isLeftButtonPressed())
		{
			
			if(hasBrokenDistanceThreshold)
			{
				// at this point, we dont care if they slip below the threshold.
				
				if(tempArrow==null)
				{
					// we need to make an arrow
					long auid = Calico.uuid();
					tempArrow = new CArrow(auid,canvas_uid, CalicoDataStore.PenColor, CArrow.TYPE_NORM_HEAD_B,
						new AnchorPoint(CArrow.TYPE_CANVAS, canvas_uid, pressedPoint),
						new AnchorPoint(CArrow.TYPE_CANVAS, canvas_uid, e.getPoint())
					);
					
					/*final CArrow finalArrow = tempArrow;
					SwingUtilities.invokeLater(
							new Runnable() { public void run() { 
								CCanvasController.canvasdb.get(canvas_uid).getLayer().addChild(finalArrow);
							}});*/
					CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(canvas_uid).getLayer(), tempArrow);
							
					//CCanvasController.canvasdb.get(canvas_uid).getLayer().addChild(tempArrow);
					
					//Highlight the groups that are associated with the points
					tempGuuidA = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), this.tempArrow.getAnchorA().getPoint());
					tempGuuidB = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), this.tempArrow.getAnchorB().getPoint());
					
					if (tempGuuidA != 0l && !(CGroupController.groupdb.get(tempGuuidA) instanceof CGroupDecorator))
					{
						CGroupController.groupdb.get(tempGuuidA).highlight_on();
						CGroupController.groupdb.get(tempGuuidA).highlight_repaint();
					}
					
					if (tempGuuidA != tempGuuidB && tempGuuidB != 0l && !(CGroupController.groupdb.get(tempGuuidB) instanceof CGroupDecorator))
					{
						CGroupController.groupdb.get(tempGuuidB).highlight_on();
						CGroupController.groupdb.get(tempGuuidB).highlight_repaint();
					}
				}
				else
				{
					tempArrow.setAnchorB(new AnchorPoint(CArrow.TYPE_CANVAS, canvas_uid, e.getPoint()));
					
					//Change the highlight of the group associated with point B
					long guuidB = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), this.tempArrow.getAnchorB().getPoint());

					if (guuidB != tempGuuidB)
					{
						if (tempGuuidB != 0l && tempGuuidB != tempGuuidA)
						{
							CGroupController.groupdb.get(tempGuuidB).highlight_off();
							CGroupController.groupdb.get(tempGuuidB).highlight_repaint();
						}
						if (guuidB != 0l && guuidB != tempGuuidA && !(CGroupController.groupdb.get(guuidB) instanceof CGroupDecorator))
						{
							CGroupController.groupdb.get(guuidB).highlight_on();
							CGroupController.groupdb.get(guuidB).highlight_repaint();
						}
						tempGuuidB = guuidB;
					}
				}
				tempArrow.redraw(true);
				
			}
			else
			{
				// have they broken the threshold
				if(pressedPoint.distance(e.getPoint()) >= CalicoOptions.arrow.create_dist_threshold)
				{
					hasBrokenDistanceThreshold = true;
					if (BubbleMenu.isBubbleMenuActive())
					{
						BubbleMenu.clearMenu();
					}
				}
			}
		}
		
		lastEvent = e;
		
	}
	
	public void actionScroll(InputEventInfo e)
	{
	}
	

	public void actionReleased(InputEventInfo e)
	{
//		this.hideModeIcon();
		CalicoInputManager.unlockHandlerIfMatch(canvas_uid);
		
		boolean isLeft = (e.getButton()==InputEventInfo.BUTTON_LEFT);
		
		if(isLeft && tempArrow!=null)
		{
			// Now we need to find out what groups its endpoints are on, as well linking its parents
			// Group at First point?
			long guuidA = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), this.tempArrow.getAnchorA().getPoint());
			long guuidB = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), this.tempArrow.getAnchorB().getPoint());
						
			if(guuidA!=0L && !(CGroupController.groupdb.get(guuidA) instanceof CGroupDecorator))
			{
				tempArrow.setAnchorA(new AnchorPoint(CArrow.TYPE_GROUP, tempArrow.getAnchorA().getPoint(), guuidA));
				CGroupController.groupdb.get(guuidA).highlight_off();
				CGroupController.groupdb.get(guuidA).highlight_repaint();
			}
			
			if(guuidB!=0L && !(CGroupController.groupdb.get(guuidB) instanceof CGroupDecorator))
			{
				tempArrow.setAnchorB(new AnchorPoint(CArrow.TYPE_GROUP, tempArrow.getAnchorB().getPoint(), guuidB));
				CGroupController.groupdb.get(guuidB).highlight_off();
				CGroupController.groupdb.get(guuidB).highlight_repaint();
			}
			
			
			CArrowController.start(tempArrow.getUUID(), tempArrow.getCanvasUUID(), tempArrow.getColor(), tempArrow.getArrowType(), 
				tempArrow.getAnchorA(), 
				tempArrow.getAnchorB()
			);
			
			
			//This line is not thread safe so must invokeLater to prevent exceptions.
			/*final CArrow finalArrow = tempArrow;
			SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
						CCanvasController.canvasdb.get(canvas_uid).getLayer().removeChild(finalArrow);
						tempArrow = null;
						} }
			);*/
			CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(canvas_uid).getLayer(), tempArrow);
			//CCanvasController.canvasdb.get(canvas_uid).getLayer().removeChild(tempArrow);
			tempArrow = null;
			
			tempGuuidB = 0;
		}
		
		if (!hasBrokenDistanceThreshold)
		{
			long guuid = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), e.getPoint());
			if (BubbleMenu.activeUUID != guuid && guuid != 0)
			{
				CGroupController.show_group_bubblemenu(guuid);
			}
		}
		
		lastEvent = e;
	}
}
