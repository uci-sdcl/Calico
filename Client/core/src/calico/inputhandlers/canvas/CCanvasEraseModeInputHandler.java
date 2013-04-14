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
import calico.components.menus.*;
import calico.components.piemenu.*;
import calico.controllers.*;
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

import org.apache.log4j.*;

import edu.umd.cs.piccolo.event.*;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;


// implements PenListener
public class CCanvasEraseModeInputHandler extends CalicoAbstractInputHandler
{
	public static Logger logger = Logger.getLogger(CCanvasEraseModeInputHandler.class.getName());

	public static final double CREATE_GROUP_MIN_DIST = 15.0;
	

	private boolean hasStartedGroup = false;
	private boolean hasStartedBge = false;
	
	private boolean hasBeenPressed = false;
	private boolean hasBeenMoved = false;

	private InputEventInfo lastEvent = null;
	
//	private Polygon draggedCoords = new Polygon();

	private CCanvasInputHandler parentHandler = null;

	private static String iconName = "mode.delete";
	
	private PPath eraser;
	
	boolean erasedSomething = false;

	public CCanvasEraseModeInputHandler(long cuid, CCanvasInputHandler parent)
	{
		canvas_uid = cuid;
		parentHandler = parent;
		
//		this.setupModeIcon();
		
	}
	
	public void actionPressed(InputEventInfo e)
	{
//		draggedCoords = new Polygon();
//		draggedCoords.addPoint(e.getX(), e.getY());
		lastEvent = e;
		
		//draw eraser circle

		drawCircle(e);

		CalicoInputManager.drawCursorImage(CCanvasController.getCurrentUUID(), CalicoIconManager.getIconImage(iconName), e.getPoint());
		Networking.send(NetworkCommand.ERASE_START, canvas_uid);
		erasedSomething = false;
//		this.showModeIcon(e.getPoint());
	}



	public void actionDragged(InputEventInfo e)
	{
//		this.hideModeIcon(e.getPoint());
//		draggedCoords.addPoint(e.getX(), e.getY());

//		CalicoInputManager.lockInputHandler(canvas_uid);

		//int x = e.getX();
		//int y = e.getY();

		drawCircle(e);
		Ellipse2D.Double eraseRectangle = new Ellipse2D.Double(e.getPoint().x - CalicoOptions.pen.eraser.radius, e.getPoint().y - CalicoOptions.pen.eraser.radius, CalicoOptions.pen.eraser.radius*2, CalicoOptions.pen.eraser.radius*2);
		
		if( (e.isLeftButtonPressed() || e.isMiddleButtonPressed() ))
		{
			long[] bgelist = CCanvasController.canvasdb.get(canvas_uid).getChildStrokes();
			if(bgelist.length>0)
			{
				for(int i=0;i<bgelist.length;i++)
				{
					if(CStrokeController.exists(bgelist[i]) && CStrokeController.intersectsCircle(bgelist[i],e.getPoint(), CalicoOptions.pen.eraser.radius) )
					{
						System.out.println("Deleting Stroke. Stroke ID: " + bgelist[i] + ", parent: " + CStrokeController.strokes.get(bgelist[i]).getParentUUID() + ", i = " + i + ", line: (" + lastEvent.getPoint().getX() + "," + lastEvent.getPoint().getY() + ") (" + e.getPoint().getX() + "," + e.getPoint().getY());
						CStrokeController.delete(bgelist[i]);
						erasedSomething = true;
					}
				}
			}
			
			long[] arrowlist = CCanvasController.canvasdb.get(canvas_uid).getChildArrows();
			if(arrowlist.length>0)
			{
				for(int i=0;i<arrowlist.length;i++)
				{
					if(CArrowController.exists(arrowlist[i]) && CArrowController.intersectsCircle(arrowlist[i],e.getPoint(), CalicoOptions.pen.eraser.radius) )
					{
						logger.debug("DELETE ARROW "+arrowlist[i]);
						CArrowController.delete(arrowlist[i]);
						erasedSomething = true;
					}
				}
			}
			
			long[] connectorlist = CCanvasController.canvasdb.get(canvas_uid).getChildConnectors();
			if(connectorlist.length>0)
			{
				for(int i=0;i<connectorlist.length;i++)
				{
					if(CConnectorController.exists(connectorlist[i]) && CConnectorController.intersectsCircle(connectorlist[i],e.getPoint(), CalicoOptions.pen.eraser.radius) )
					{
						logger.debug("DELETE CONNECTOR "+connectorlist[i]);
						CConnectorController.delete(connectorlist[i]);
						erasedSomething = true;
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
		Networking.send(NetworkCommand.ERASE_END, canvas_uid, erasedSomething);
		
		removeCircle();
		
		lastEvent = e;
		erasedSomething = false;
	}
	
	private void drawCircle(InputEventInfo e) {
		if (eraser == null)
		{
		Ellipse2D.Double hitTarget = new Ellipse2D.Double(e.getPoint().x - CalicoOptions.pen.eraser.radius, e.getPoint().y - CalicoOptions.pen.eraser.radius, CalicoOptions.pen.eraser.radius*2, CalicoOptions.pen.eraser.radius*2);
		eraser = new PPath(hitTarget);
		eraser.setStrokePaint(Color.black);
		eraser.setStroke(new BasicStroke(1.0f));
		//CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().addChild(eraser);
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer(), eraser);
		//eraser.invalidatePaint();
		CalicoDraw.invalidatePaint(eraser);
		CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().repaintFrom(eraser.getBounds(), eraser);
		}
		else {
			Rectangle origBounds = eraser.getBounds().getBounds();
			Ellipse2D.Double hitTarget = new Ellipse2D.Double(e.getPoint().x - CalicoOptions.pen.eraser.radius, e.getPoint().y - CalicoOptions.pen.eraser.radius, CalicoOptions.pen.eraser.radius*2, CalicoOptions.pen.eraser.radius*2);
			eraser.setPathTo(hitTarget);
			//eraser.repaint();
			CalicoDraw.repaintNode(eraser);
			CCanvasController.canvasdb.get(canvas_uid).repaint(new PBounds(origBounds));
//			eraser.moveTo((float)(e.getPoint().x - CalicoOptions.pen.eraser.radius), (float)(e.getPoint().y - CalicoOptions.pen.eraser.radius));
		}
	}
	
	private void removeCircle()
	{
		PBounds bounds = eraser.getBounds();
		//CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().removeChild(eraser);
		CalicoDraw.removeChildFromNode(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer(), eraser);
		CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().repaintFrom(bounds, null);
		eraser = null;
	}
}
