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

import calico.Calico;
import calico.controllers.CCanvasController;
import calico.controllers.CStrokeController;
import calico.inputhandlers.CCanvasInputHandler;
import calico.inputhandlers.CGroupInputHandler;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;

public class CGroupPointerModeInputHandler extends CalicoAbstractInputHandler {

	boolean hasStartedBge = false;
	long uuid;
	CGroupInputHandler parentHandler;
	
	
	public CGroupPointerModeInputHandler(long u, CGroupInputHandler par) {
		uuid = u;
		parentHandler = par;
	}
	
	public void actionPressed(InputEventInfo e) {

		if(e.isLeftButtonPressed())
		{
			int x = e.getX();
			int y = e.getY();
			long uuid = Calico.uuid();
			CStrokeController.setCurrentUUID(uuid);
			CStrokeController.start(uuid, CCanvasController.getCurrentUUID(), 0L);
			CStrokeController.append(uuid, x, y);
			hasStartedBge = true;
		}
		
	}
	
	public void actionDragged(InputEventInfo e) {

		int x = e.getX();
		int y = e.getY();

		
		if(e.isLeftButtonPressed() && hasStartedBge)
		{
			CStrokeController.append(CStrokeController.getCurrentUUID(), x, y);

		}
		
		else if(e.isLeftButtonPressed() && !hasStartedBge)
		{
			long uuid = Calico.uuid();
			CStrokeController.setCurrentUUID(uuid);
			CStrokeController.start(uuid, canvas_uid, 0L);
			CStrokeController.append(uuid, x, y);
			hasStartedBge = true;
		}
	}
	
	public void actionReleased(InputEventInfo e) {
		
		CalicoInputManager.unlockHandlerIfMatch(canvas_uid);

		
		int x = e.getX();
		int y = e.getY();
		
		if(!hasStartedBge && e.isLeftButton())
		{
			long uuid = Calico.uuid();
			CStrokeController.setCurrentUUID(uuid);
			CStrokeController.start(uuid, canvas_uid, 0L);

			CStrokeController.append(uuid, x, y);
			CStrokeController.finish(uuid);
		}
		else if(hasStartedBge && e.isLeftButton())
		{
			long bguid = CStrokeController.getCurrentUUID();
			CStrokeController.append(bguid, x, y);
			CStrokeController.finish(bguid);
			hasStartedBge = false;
		}
	}

}
