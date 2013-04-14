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

import calico.COptions;
import calico.clients.Client;
import calico.clients.ClientManager;
import calico.components.CCanvas;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.uuid.UUIDAllocator;

public class CGridController {
	
	private static CGridController instance;
	private static boolean isActive = false;
	
	public final static CalicoEventListener listener = new CalicoEventListener() {		
		@Override
		public void handleCalicoEvent(int event, CalicoPacket p, Client client) {
			if (event == NetworkCommand.CANVAS_LIST)
				GRID_SIZE(p, client);
			
		}
	};
	
	public static CGridController getInstance()
	{
		if (instance == null)
			instance = new CGridController();
		return instance;
	}
	
	private CGridController()
	{
		CalicoEventHandler.getInstance().addListener(NetworkCommand.CANVAS_LIST, listener, CalicoEventHandler.ACTION_PERFORMER_LISTENER);
	}

	private static void initializeGridCanvases()
	{
		for(int i=0;i<COptions.GridRows;i++)
		{	  	
			for(int y=0;y<COptions.GridCols;y++)
			{
				// Make the canvas
				CCanvas can = new CCanvas(UUIDAllocator.getUUID());
				// Add to the main list
				CCanvasController.canvases.put(can.getUUID(), can);
			
			}
		}
	}
	
	public static void initialize()
	{
		isActive = true;
		initializeGridCanvases();
	}
	
	public static int getCanvasRow(long cuid)
	{
		//Formula: Index - Row * NumColumns
		return CCanvasController.canvases.get(cuid).getIndex() - getCanvasColumn(cuid) * COptions.GridCols;
	}
	
	public static int getCanvasColumn(long cuid)
	{
		return (int) Math.floor(CCanvasController.canvases.get(cuid).getIndex() / COptions.GridCols);
	}
	
	public static String getCanvasCoord(long cuid)
	{
		int x = getCanvasRow(cuid);
		int y = getCanvasColumn(cuid);
		return (Character.valueOf( (char) (x+65)) ).toString()+""+y;
	}
	

	
	public static void GRID_SIZE(CalicoPacket notused,Client client)
	{
		if (!isActive)
			return;
		
		CalicoPacket p = new CalicoPacket();
		p.putInt(NetworkCommand.GRID_SIZE);
		p.putInt(COptions.GridRows);
		p.putInt(COptions.GridCols);
		ClientManager.send(client,p);
		
		// Load up the sessions?
		CSessionController.sendSessionList();
		
	}
	
}
