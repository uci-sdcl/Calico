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
import java.awt.Polygon;
import java.awt.Rectangle;

import calico.Calico;
import calico.CalicoUtils;
import calico.components.CGroup;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CGroupDecoratorController;
import calico.controllers.CStrokeController;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.InputEventInfo;
import calico.networking.Networking;
import calico.networking.netstuff.*;

public class ListCreateButton extends PieMenuButton
{

	public static int SHOWON = PieMenuButton.SHOWON_SCRAP_CREATE | PieMenuButton.SHOWON_SCRAP_MENU;
	long cuid;
	private boolean isActive = false;
	
	
	public ListCreateButton(long cuid, long uuid)
	{
		super("lists.create");
		this.uuid = uuid;
		this.cuid = cuid;
	}
	public ListCreateButton(long uuid)
	{
		this(CCanvasController.getCurrentUUID(), uuid);
	}
	
	public void onPressed(InputEventInfo ev)
	{
		if (!CGroupController.exists(uuid) || isActive)
		{
			return;
		}
		
		isActive = true;
		
		super.onPressed(ev);
	}
	
	public void onReleased(InputEventInfo ev)
	{
		//super.onClick(ev);
		
		if (CGroupController.exists(this.uuid))
		{
			long listuuid = createList(this.uuid);
			CGroupController.show_group_bubblemenu(listuuid);
		}
		else if (CStrokeController.exists(this.uuid))
		{
			long new_uuid = Calico.uuid();
			CStrokeController.makeScrap(this.uuid, new_uuid);
			CGroupController.set_permanent(new_uuid, true);
			long listuuid = createList(new_uuid);
			CGroupController.show_group_bubblemenu(listuuid);
		}
		ev.stop();
		
		isActive = false;
	}
	public long createList(long groupToBeDecorated) {
		long newuuid = Calico.uuid();

//		CGroupController.shrink_to_contents(groupToBeDecorated);
		CGroupDecoratorController.list_create(groupToBeDecorated, newuuid);
		return newuuid;
	}
}
