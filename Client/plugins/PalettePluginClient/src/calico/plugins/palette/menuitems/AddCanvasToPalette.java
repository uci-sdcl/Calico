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
package calico.plugins.palette.menuitems;

import java.awt.Color;
import java.awt.Rectangle;

import javax.swing.SwingUtilities;

import calico.Calico;
import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.components.CGroup;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.inputhandlers.InputEventInfo;
import calico.networking.Networking;
import calico.plugins.palette.*;
import calico.plugins.palette.iconsets.CalicoIconManager;

public class AddCanvasToPalette extends PaletteBarMenuItem {

	public AddCanvasToPalette()
	{
		super();
		this.setImage(CalicoIconManager.getIconImage("palette.add"));
	}
	
	@Override
	public void onClick(InputEventInfo ev) {
		//create scrap that takes up entire  canvas
		final long uuid = Calico.uuid();
		final long cuuid = CCanvasController.getCurrentUUID();
	

		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					Networking.ignoreConsistencyCheck = true;
					final CGroup group = new CGroup(uuid, cuuid);
					final Rectangle bounds = new Rectangle(0, 0, CalicoDataStore.serverScreenWidth, CalicoDataStore.serverScreenHeight);
					group.setShapeToRoundedRectangle(bounds, 0);
					CGroupController.no_notify_start(uuid, cuuid, 0l, true, group);
					group.setPaint(Color.white);
					CGroupController.setCurrentUUID(uuid);
					CGroupController.no_notify_finish(uuid, true, false, false);
					//add scrap to palette
					PalettePlugin.addGroupToPalette(PalettePlugin.getActivePaletteUUID(), uuid);
					//drop the scrap
					CGroupController.drop(uuid);
					Networking.ignoreConsistencyCheck = false;
					group.setVisible(false);
				}});
	}

}
