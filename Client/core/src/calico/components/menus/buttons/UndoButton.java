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
package calico.components.menus.buttons;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

import calico.*;
import calico.components.*;
import calico.components.grid.*;
import calico.components.menus.CanvasMenuButton;
import calico.controllers.CCanvasController;
import calico.events.CalicoEventHandler;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.InputEventInfo;
import calico.modules.*;
import calico.networking.*;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolox.nodes.PLine;
import edu.umd.cs.piccolox.pswing.*;

import java.net.*;

import javax.swing.ProgressMonitor;

import edu.umd.cs.piccolo.event.*;



public class UndoButton extends CanvasMenuButton
{
	private static final long serialVersionUID = 1L;
	
	private long cuid = 0L;
	public static final String undoMessage = "Loading previous state from server, please wait... ";
	public static ProgressMonitor progressMonitor;
	
	public UndoButton(long c)
	{
		super();
		cuid = c;
		iconString = "undo";
		try
		{
			setImage(CalicoIconManager.getIconImage(iconString));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	public void actionMouseClicked(InputEventInfo event)
	{
		if (event.getAction() == InputEventInfo.ACTION_PRESSED)
		{
			super.onMouseDown();
		}
		else if (event.getAction() == InputEventInfo.ACTION_RELEASED && isPressed)
		{
			CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE_START, 0d, 1d, undoMessage));
			CalicoEventHandler.getInstance().fireEvent(NetworkCommand.STATUS_SENDING_LARGE_FILE, CalicoPacket.getPacket(NetworkCommand.STATUS_SENDING_LARGE_FILE, 0.1d, 1d, undoMessage));
			/*progressMonitor = new ProgressMonitor(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getComponent(),
					undoMessage,
	                "something", 0, 100);
			progressMonitor.setProgress(0);
			progressMonitor.setMillisToPopup(1);
			progressMonitor.setMillisToDecideToPopup(1);*/
			Networking.send(CalicoPacket.getPacket(NetworkCommand.CANVAS_UNDO, cuid));
			Calico.logger.debug("SENDING UNDO COMMAND");
			//StatusMessage.popup("Not yet implemented");
			super.onMouseUp();
		}
	}
	
}
