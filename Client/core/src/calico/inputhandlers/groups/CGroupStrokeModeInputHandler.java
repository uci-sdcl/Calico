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

import calico.*;
import calico.controllers.CStrokeController;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.CGroupInputHandler;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;

import org.apache.log4j.Logger;


public class CGroupStrokeModeInputHandler extends CalicoAbstractInputHandler
{
	public static Logger logger = Logger.getLogger(CGroupStrokeModeInputHandler.class.getName());
	
	private long uuid = 0L;

	private boolean hasBeenPressed = false;
	private boolean hasStartedBge = false;
	
	public CGroupStrokeModeInputHandler(long u, CGroupInputHandler par)
	{
		uuid = u;
		this.canvas_uid =  CGroupController.groupdb.get(uuid).getCanvasUID();
	}
	
	public void actionPressed(InputEventInfo e)
	{

		
		hasBeenPressed = true;
		if(e.isLeftButtonPressed())
		{
			int x = e.getX();
			int y = e.getY();
			long suuid = Calico.uuid();
			CStrokeController.setCurrentUUID(suuid);
			CStrokeController.start(suuid, CCanvasController.getCurrentUUID(), this.uuid);
			CStrokeController.append(suuid, x, y);
			CStrokeController.append(suuid, x, y);
			hasStartedBge = true;
		}

		CalicoInputManager.lockInputHandler(uuid);
		
		CalicoInputManager.drawCursorImage(canvas_uid,
				CalicoIconManager.getIconImage("mode.stroke"), e.getPoint());
	}


	public void actionDragged(InputEventInfo e)
	{

		int x = e.getX();
		int y = e.getY();

		
		if(e.isLeftButtonPressed() && hasStartedBge)
		{
			CStrokeController.append(CStrokeController.getCurrentUUID(), x, y);
			//e.setHandled(true);
		}
		else if(e.isLeftButtonPressed() && !hasStartedBge)
		{
			long suuid = Calico.uuid();
			CStrokeController.setCurrentUUID(uuid);
			CStrokeController.start(suuid, CCanvasController.getCurrentUUID(), this.uuid);
			CStrokeController.append(suuid, x, y);
			hasStartedBge = true;
		}
	}


	public void actionReleased(InputEventInfo e)
	{
		
		CalicoInputManager.unlockHandlerIfMatch(uuid);

		

		int x = e.getX();
		int y = e.getY();
		if(!hasBeenPressed && (e.getButton()==InputEventInfo.BUTTON_LEFT))
		{
			long suuid = Calico.uuid();
			CStrokeController.setCurrentUUID(suuid);
			CStrokeController.start(suuid, CCanvasController.getCurrentUUID(), this.uuid);

			CStrokeController.append(suuid, x, y);
			CStrokeController.finish(suuid);
		}
		else if(hasStartedBge && (e.getButton()==InputEventInfo.BUTTON_LEFT))
		{
			long bguid = CStrokeController.getCurrentUUID();
			CStrokeController.append(bguid, x, y);
			CStrokeController.finish(bguid);
			hasStartedBge = false;
		}
		hasBeenPressed = false;
		
	}

}
