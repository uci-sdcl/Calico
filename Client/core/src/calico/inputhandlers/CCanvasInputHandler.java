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
package calico.inputhandlers;

import calico.*;

import calico.components.*;
import calico.components.menus.*;
import calico.components.piemenu.*;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.input.CInputMode;
import calico.inputhandlers.canvas.*;
import calico.networking.*;
import calico.networking.netstuff.NetworkCommand;


import java.awt.geom.*;
import java.awt.*;

import java.util.*;

import org.apache.log4j.Logger;

import edu.umd.cs.piccolo.event.*;


// implements PenListener
public class CCanvasInputHandler extends CalicoAbstractInputHandler
{
	public static Logger logger = Logger.getLogger(CCanvasInputHandler.class.getName());
	
	private CCanvasExpertModeInputHandler modehandler_expert = null;
	private CCanvasScrapModeInputHandler modehandler_scrap = null;
	private CCanvasArrowModeInputHandler modehandler_arrow = null;
	private CCanvasEraseModeInputHandler modehandler_delete = null;
	private CCanvasStrokeModeInputHandler modehandler_stroke = null;
	private CCanvasPointerModeInputHandler modehandler_pointer = null;
	

	public static boolean dropChangeViewportFocusEvents=false;
	//private InputEventInfo lastEvent = null;
	

	public CCanvasInputHandler(long cuid)
	{
		canvas_uid = cuid;
		modehandler_expert = new CCanvasExpertModeInputHandler(canvas_uid, this);
		modehandler_scrap = new CCanvasScrapModeInputHandler(canvas_uid, this);

		modehandler_arrow = new CCanvasArrowModeInputHandler(canvas_uid, this);
		modehandler_delete = new CCanvasEraseModeInputHandler(canvas_uid, this);
		modehandler_stroke = new CCanvasStrokeModeInputHandler(canvas_uid, this);
		modehandler_pointer = new CCanvasPointerModeInputHandler(canvas_uid, this);
		
	}

	

//	private void getMenuBarClick(Point point)
//	{
//		if(CCanvasController.canvasdb.get(canvas_uid).isPointOnMenuBar(point))
//		{
//			CCanvasController.canvasdb.get(canvas_uid).clickMenuBar(point);
//		}
//	}
	



	public void actionPressed(InputEventInfo e)
	{
		CalicoInputManager.lockInputHandler(this.canvas_uid);
		
		// remove the last temp group
		//logger.debug("CCanvasInputHandler.actionPressed()");
		
		CGroupController.checkToRemoveLastTempGroup();
		
		if(CCanvasController.canvasdb.get(canvas_uid).isPointOnMenuBar(e.getGlobalPoint()))
		{
			return;
		}
		
		if(CalicoOptions.canvas.lowquality_on_interaction)
		{
			CCanvasController.canvasdb.get(canvas_uid).setInteracting(true);
		}
		
		switch(CalicoDataStore.Mode)
		{
			case EXPERT:modehandler_expert.actionPressed(e);break;
			case ARROW:modehandler_arrow.actionPressed(e);break;
			case SCRAP:modehandler_scrap.actionPressed(e);break;
			case DELETE:modehandler_delete.actionPressed(e);break;
			case STROKE:modehandler_stroke.actionPressed(e);break;
			case POINTER:modehandler_pointer.actionPressed(e);break;
		}

	}//actionPressed

	public void actionDragged(InputEventInfo e)
	{
		// are we on the menu bar
		if(CCanvasController.canvasdb.get(canvas_uid).isPointOnMenuBar(e.getGlobalPoint()))
		{
			return;
		}
		
		
		switch(CalicoDataStore.Mode)
		{
			case EXPERT:modehandler_expert.actionDragged(e);break;
			case ARROW:modehandler_arrow.actionDragged(e);break;
			case SCRAP:modehandler_scrap.actionDragged(e);break;
			case DELETE:modehandler_delete.actionDragged(e);break;
			case STROKE:modehandler_stroke.actionDragged(e);break;
			case POINTER:modehandler_pointer.actionDragged(e);break;
		}
	}//actionDragged
	
	public void actionScroll(InputEventInfo e)
	{
		switch(CalicoDataStore.Mode)
		{
			case EXPERT:modehandler_expert.actionScroll(e);break;
			case ARROW:modehandler_arrow.actionScroll(e);break;
			case SCRAP:modehandler_scrap.actionScroll(e);break;
			case DELETE:modehandler_delete.actionScroll(e);break;
			case STROKE:modehandler_stroke.actionScroll(e);break;
			case POINTER:modehandler_pointer.actionScroll(e);break;
		}
	}//actionScroll
	

	public void actionReleased(InputEventInfo e)
	{
		CalicoInputManager.unlockHandlerIfMatch(this.canvas_uid);
		
		// do they want lower quality for rendering?
		if(CalicoOptions.canvas.lowquality_on_interaction)
		{
			CCanvasController.canvasdb.get(canvas_uid).setInteracting(false);
		}

		
		switch(CalicoDataStore.Mode)
		{
			case EXPERT:modehandler_expert.actionReleased(e);break;
			case ARROW:modehandler_arrow.actionReleased(e);break;
			case SCRAP:modehandler_scrap.actionReleased(e);break;
			case DELETE:modehandler_delete.actionReleased(e);break;
			case STROKE:modehandler_stroke.actionReleased(e);break;
			case POINTER:modehandler_pointer.actionReleased(e);break;
		}
	}//actionReleased
	
	
	public void routeToHandler_actionDragged(CInputMode modeFlag, InputEventInfo e)
	{
		switch(modeFlag)
		{
			case EXPERT:modehandler_expert.actionDragged(e);break;
			case ARROW:modehandler_arrow.actionDragged(e);break;
			case SCRAP:modehandler_scrap.actionDragged(e);break;
			case DELETE:modehandler_delete.actionDragged(e);break;
			case STROKE:modehandler_stroke.actionDragged(e);break;
			case POINTER:modehandler_pointer.actionDragged(e);break;
		}
	}
	
	public void routeToHandler_actionPressed(CInputMode modeFlag, InputEventInfo e)
	{
		switch(modeFlag)
		{
			case EXPERT:modehandler_expert.actionPressed(e);break;
			case ARROW:modehandler_arrow.actionPressed(e);break;
			case SCRAP:modehandler_scrap.actionPressed(e);break;
			case DELETE:modehandler_delete.actionPressed(e);break;
			case STROKE:modehandler_stroke.actionPressed(e);break;
			case POINTER:modehandler_pointer.actionPressed(e);break;
		}
	}
	
	public void routeToHandler_actionReleased(CInputMode modeFlag, InputEventInfo e)
	{
		switch(modeFlag)
		{
			case EXPERT:modehandler_expert.actionReleased(e);break;
			case ARROW:modehandler_arrow.actionReleased(e);break;
			case SCRAP:modehandler_scrap.actionReleased(e);break;
			case DELETE:modehandler_delete.actionReleased(e);break;
			case STROKE:modehandler_stroke.actionReleased(e);break;
			case POINTER:modehandler_pointer.actionReleased(e);break;
		}
	}
}

