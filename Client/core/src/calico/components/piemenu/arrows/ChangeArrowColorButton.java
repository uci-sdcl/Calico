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
package calico.components.piemenu.arrows;

import java.awt.*;
import java.util.*;

import calico.Calico;
import calico.components.piemenu.*;
import calico.controllers.CCanvasController;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.InputEventInfo;

@Deprecated
public class ChangeArrowColorButton extends PieMenuButton
{
	private Timer menuTimer = null;
	private long uuid = 0L;
	
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
			PieMenu.displayPieMenu(point, 
					new SetArrowColorButton(uuid,Color.RED),
					new SetArrowColorButton(uuid,Color.BLUE),
					new SetArrowColorButton(uuid,Color.GREEN),
					new SetArrowColorButton(uuid,Color.ORANGE),
					new SetArrowColorButton(uuid,Color.PINK),
					new SetArrowColorButton(uuid,Color.YELLOW),
					new SetArrowColorButton(uuid,Color.BLACK)
			);
			CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).repaint();
			this.cancel();
		}
	
	}
	
	public ChangeArrowColorButton(long uid)
	{
		super("color.changecolor");
		uuid = uid;
	}
	
	public void onClick(InputEventInfo ev)
	{
		ev.stop();
		
		menuTimer = new Timer("MenuTimer",true);
		menuTimer.schedule(new MenuTimer(ev.getGlobalPoint(),uuid), 100);
				
	}
}
