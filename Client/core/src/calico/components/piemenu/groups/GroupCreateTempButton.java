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
import java.util.Timer;
import java.util.TimerTask;

import calico.Calico;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.inputhandlers.InputEventInfo;

public class GroupCreateTempButton extends PieMenuButton
{
	private Timer menuTimer = null;
	public static int SHOWON = 0;
	long guuid;
	
	public GroupCreateTempButton(long uuid)
	{
		super("group.temp");
		guuid = uuid;
	}
	
	public void onClick(InputEventInfo ev)
	{
		super.onClick(ev);
		if (CGroupController.exists(guuid))
		{
			CGroupController.set_permanent(guuid, true);
			ev.stop();
		}
		else if (CStrokeController.exists(guuid))
		{
			long new_uuid = Calico.uuid();
			CStrokeController.makeScrap(guuid, new_uuid);
			ev.stop();
			menuTimer = new Timer("MenuTimer",true);
			menuTimer.schedule(new MenuTimer(ev.getPoint(),new_uuid), 100);			
		}

		
		Calico.logger.debug("CLICKED GROUP PERM BUTTON");
	}
	
	private class MenuTimer extends TimerTask
	{
		private Point point = null;
		private long uuid = 0L;
		public MenuTimer(Point p, long uid)
		{
			point = p;
			uuid = uid;
		}
		
		public void run()
		{
			CGroupController.show_group_piemenu(uuid, point);
			CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).repaint();
			this.cancel();
		}
	
	}
}
