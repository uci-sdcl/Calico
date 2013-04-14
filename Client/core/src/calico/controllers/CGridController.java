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

import calico.Calico;
import calico.CalicoDataStore;
import calico.components.grid.CGrid;
import calico.components.menus.CanvasMenuBar;
import calico.components.menus.buttons.CanvasNavButton;
import calico.components.menus.buttons.CanvasNavButtonDown;
import calico.components.menus.buttons.CanvasNavButtonLeft;
import calico.components.menus.buttons.CanvasNavButtonRight;
import calico.components.menus.buttons.CanvasNavButtonUp;
import calico.components.menus.buttons.ReturnToGrid;
import calico.components.menus.buttons.SpacerButton;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.modules.MessageObject;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.perspectives.GridPerspective;

public class CGridController {
	
	private static CGridController instance;
	
	public static CGridController getInstance()
	{
		if (instance == null)
			instance = new CGridController();
		return instance;
	}
	
	private CGridController()
	{
		CalicoEventHandler.getInstance().addListener(NetworkCommand.CONSISTENCY_FINISH, listener, CalicoEventHandler.ACTION_PERFORMER_LISTENER);
		
		CanvasMenuBar.addMenuButtonPreAppend(ReturnToGrid.class);
		
		CanvasMenuBar.addMenuButtonPreAppend(SpacerButton.class);
		
		CanvasMenuBar.addMenuButtonPreAppend(CanvasNavButtonLeft.class);
		CanvasMenuBar.addMenuButtonPreAppend(CanvasNavButtonRight.class);
		CanvasMenuBar.addMenuButtonPreAppend(CanvasNavButtonUp.class);
		CanvasMenuBar.addMenuButtonPreAppend(CanvasNavButtonDown.class);
		
		CanvasMenuBar.addMenuButtonPreAppend(SpacerButton.class);
		
	}
	
	private static CalicoEventListener listener = new CalicoEventListener() {		

		@Override
		public void handleCalicoEvent(int event, CalicoPacket p) {
			if (event == NetworkCommand.CONSISTENCY_FINISH)
			{
				CONSISTENCY_FINISH(p);
			}
			
		}
	};
	
	private static void CONSISTENCY_FINISH(CalicoPacket p)
	{
		//long[] canvasuids = CCanvasController.getCanvasIDList();
		
		/*
		for(int can=0;can<canvasuids.length;can++)
		{
			CCanvasController.render(canvasuids[can]);
		}*/
		
		if( GridPerspective.getInstance().isActive() )
		{
			Calico cal = CalicoDataStore.calicoObj;
			
			cal.getContentPane().removeAll();
			cal.getContentPane().add( CGrid.getInstance().getComponent() );
			CGrid.getInstance().refreshCells();
	        cal.pack();
	        cal.setVisible(true);
			cal.repaint();
		}

		MessageObject.showNotice("The grid has loaded");
		Calico.isGridLoading = false;

	}
}
