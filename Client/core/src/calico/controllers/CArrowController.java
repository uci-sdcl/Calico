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
package calico.controllers;

import java.awt.*;

import calico.*;
import calico.networking.*;
import calico.networking.netstuff.*;
import calico.components.*;
import calico.components.arrow.AnchorPoint;
import calico.components.arrow.CArrow;
import calico.inputhandlers.CalicoInputManager;
import calico.modules.*;


import java.awt.geom.*;
import java.util.*;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolox.nodes.*;

import it.unimi.dsi.fastutil.longs.*;

/**
 * This handles all start/append/finish requests for lines
 *
 * @author Mitch Dempsey
 */
public class CArrowController
{
	private static Logger logger = Logger.getLogger(CArrowController.class.getName());
	
	public static Long2ReferenceArrayMap<CArrow> arrows = new Long2ReferenceArrayMap<CArrow>();

	private static long currentArrowid = 0L;
	
	//public static boolean hasOutstandingAnchorPoint = false;
	public static Point outstandingAnchorPoint = null;
	
	// Does nothing right now
	public static void setup()
	{
		arrows.clear();
	}
	
	public static boolean exists(long uuid)
	{
		return arrows.containsKey(uuid);
	}

	public static Point getOutstandingAnchorPoint()
	{
		return outstandingAnchorPoint;
	}
	public static void setOutstandingAnchorPoint(Point p)
	{
		outstandingAnchorPoint = p;

//		CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).menuBar.redrawArrowIndicator();
	}
	
	
	
	
	public static void no_notify_start(final long uuid, final long cuid, Color color,int type, AnchorPoint anchorA, AnchorPoint anchorB)
	{
		if (exists(uuid))
			no_notify_delete(uuid);
		
		arrows.put(uuid, new CArrow(uuid, cuid, color, type, anchorA, anchorB));
		arrows.get(uuid).redraw();
		
		// Add to the canvas 
		
		CCanvasController.canvasdb.get(cuid).addChildArrow(uuid);
		
		// TODO: We need to notify the groups 
		if(anchorA.getType()==CArrow.TYPE_GROUP)
		{
			CGroupController.no_notify_add_arrow(anchorA.getUUID(), uuid);
		}
		if(anchorB.getType()==CArrow.TYPE_GROUP)
		{
			CGroupController.no_notify_add_arrow(anchorB.getUUID(), uuid);	
		}
		
		// Add the node to the painter
		//CCanvasController.canvasdb.get(cuid).getLayer().addChild(arrows.get(uuid));
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(cuid).getLayer(), arrows.get(uuid));
		

		// Add the input handler
		CalicoInputManager.addArrowInputHandler(uuid);
		
		//arrows.get(uuid).repaint();
		CalicoDraw.repaintNode(arrows.get(uuid));
		
	}
	
	public static void no_notify_delete(long uuid)
	{
		if(!exists(uuid)){return;}
		
		final CArrow arrow = arrows.get(uuid);
		
		long cuid = arrow.getCanvasUUID();
		
		CCanvasController.canvasdb.get(cuid).removeChildArrow(uuid);
		
		//This line is not thread safe so must invokeLater to prevent eraser artifacts.
		/*SwingUtilities.invokeLater(
				new Runnable() { public void run() { arrow.removeFromParent(); } }
		);	*/
		CalicoDraw.removeNodeFromParent(arrow);
		//arrow.removeFromParent();
				
		// Clear the anchor A
		if(arrows.get(uuid).getAnchorA().getType()==CArrow.TYPE_GROUP)
		{
			CGroupController.no_notify_delete_child_arrow(arrows.get(uuid).getAnchorA().getUUID(), uuid);
		}
		
		// Clear anchor B
		if(arrows.get(uuid).getAnchorB().getType()==CArrow.TYPE_GROUP)
		{
			CGroupController.no_notify_delete_child_arrow(arrows.get(uuid).getAnchorB().getUUID(), uuid);
		}
		
		arrows.remove(uuid);
				
		//CCanvasController.canvasdb.get(cuid).getLayer().setPaintInvalid(true);
		CalicoDraw.setNodePaintInvalid(CCanvasController.canvasdb.get(cuid).getLayer(), true);
	}
	
	public static void no_notify_set_color(long uuid, Color color)
	{
		
	}
	
	public static void no_notify_move_anchor(long uuid, int type, int x, int y)
	{
		
	}
	
	public static void no_notify_move_group_anchor(long uuid, long guuid, int x, int y)
	{
		if (arrows.get(uuid) != null)
			arrows.get(uuid).moveGroup(guuid, x, y);
	}
	
	
	//////////////////////////////////////// NOTIFIERS ////////////////
	
	
	public static void start(long uuid, long cuid, Color color, int type, AnchorPoint anchorA, AnchorPoint anchorB)
	{
		no_notify_start(uuid, cuid, color, type, anchorA, anchorB);
		
		Networking.send(NetworkCommand.ARROW_CREATE,
			uuid,
			cuid,
			type,
			color.getRGB(),
			anchorA.getType(), anchorA.getUUID(), anchorA.getPoint().x, anchorA.getPoint().y,
			anchorB.getType(), anchorB.getUUID(), anchorB.getPoint().x, anchorB.getPoint().y
		);
	}
	
	public static void set_color(long uuid, Color color)
	{
		
	}
	
	public static void delete(long uuid)
	{
		no_notify_delete(uuid);
		
		Networking.send(NetworkCommand.ARROW_DELETE, uuid);
		
	}
	
	
	// THIS IS DIFFERENT FROM SET_PARENT ON GROUP/STROKES - It only changes the parent of the anchor node IF THE ANCHOR IS SET TO THE CANVAS
	public static void no_notify_parented_to_group(long uuid, long parent_uuid)
	{
		// TODO: Finish
	}

	
	
	
	
	/*
	public static void no_notify_start(long uuid,long canvasuid,int type, int anchorAType, long anchorAUUID, Point anchorAPoint, int anchorBType, long anchorBUUID, Point anchorBPoint )
	{

		
		arrows.put(uuid, new CArrow(uuid,canvasuid,type));
		
		arrows.get(uuid).setAnchorA(anchorAType, anchorAUUID, anchorAPoint);
		arrows.get(uuid).setAnchorB(anchorBType, anchorBUUID, anchorBPoint);
		arrows.get(uuid).createPointPath();
	}


	public static void no_notify_delete(long uuid)
	{

		if(!arrows.containsKey(uuid))
		{
			logger.warn("ARROW delete on nonexistant "+uuid);
			return;
		}
		
		CArrow elm = arrows.get(uuid);
		elm.delete();
		arrows.remove(uuid);
	}
	
	
	public static void no_notify_color(long uuid, Color color)
	{
		if(!arrows.containsKey(uuid))
		{
			logger.warn("ARROW color on nonexistant "+uuid);
			return;
		}
		arrows.get(uuid).setColor(color);
	}
	
	
	public static void no_notify_color(long uuid, int r, int g, int b)
	{
		no_notify_color(uuid,new Color(r,g,b));
	}

	
	public static void no_notify_move(long uuid, long guid, int x, int y)
	{

		if(!arrows.containsKey(uuid))
		{
			logger.warn("ARROW move on nonexistant "+uuid);
			return;
		}
		
		arrows.get(uuid).moveGroup(guid, x, y);
	}
	
	
	public static void start(long uuid,long canvasuid,   int anchorAType, long anchorAUUID, Point anchorAPoint, int anchorBType, long anchorBUUID, Point anchorBPoint )
	{
		no_notify_start(uuid, canvasuid,  CArrow.TYPE_NORM_HEAD_B, anchorAType, anchorAUUID, anchorAPoint, anchorBType, anchorBUUID, anchorBPoint );
		//UUID CANVASUID ARROW_TYPE ANCHOR_A_TYPE ANCHOR_A_UUID ANCHOR_A_X ANCHOR_A_Y   ANCHOR_B_TYPE ANCHOR_B_UUID ANCHOR_B_X ANCHOR_B_Y
		Networking.send(NetworkCommand.ARROW_CREATE, uuid, canvasuid,  CArrow.TYPE_NORM_HEAD_B, 
				anchorAType, anchorAUUID, anchorAPoint.x, anchorAPoint.y, 
				anchorBType, anchorBUUID, anchorBPoint.x, anchorBPoint.y);
		
		color(uuid, CalicoDataStore.PenColor);
		
		//append(uuid,x,y);
	}


	public static void delete(long uuid)
	{
		no_notify_delete(uuid);
		//Networking.send(NetworkCommand.BGE_DELETE, uuid);
	}
	
	public static void color(long uuid, Color color)
	{
		no_notify_color(uuid,color);
		Networking.send(NetworkCommand.ARROW_SET_COLOR, uuid, color.getRed(), color.getGreen(), color.getBlue());
	}
	public static void color(long uuid, int r, int g, int b)
	{
		color(uuid,new Color(r,g,b));
	}*/
	
	
	

	public static long getCurrentUUID()
	{
		return currentArrowid;
	}

	public static void setCurrentUUID(long u)
	{
		currentArrowid = u;
	}

	public static boolean intersectsCircle(long l, Point center, double radius) {
		CArrow arrow = arrows.get(l);

		double circleDist = Line2D.ptSegDist(arrow.getAnchorA().getPoint().getX(), arrow.getAnchorA().getPoint().getY(), arrow.getAnchorB().getPoint().getX(), arrow.getAnchorB().getPoint().getY(), center.getX(), center.getY());
		if (circleDist < radius)
			return true;

		return false;
	}

	public static int get_signature(long l) {
		if (!exists(l))
			return 0;
		
		return arrows.get(l).get_signature();
	}

	public static void recheck_parent(long l) {
		if (!exists(l))
			return;
		CArrowController.arrows.get(l).calculateParent();
		
	}


}
