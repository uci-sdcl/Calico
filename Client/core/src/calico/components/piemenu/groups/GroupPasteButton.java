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
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import calico.Calico;
import calico.Geometry;
import calico.components.CGroup;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.inputhandlers.InputEventInfo;
import calico.networking.Networking;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

public class GroupPasteButton extends PieMenuButton
{
	private long uuid = 0L;
	public GroupPasteButton(long uuid)
	{
		super("scrap.paste");
		this.uuid = uuid;
	}
	
	public void onClick(InputEventInfo ev)
	{
		ev.stop();
		
		if (CGroupController.exists(this.uuid))
		{
		
			CGroup group = CGroupController.groupdb.get(this.uuid);
			Rectangle rect = group.getCoordList().getBounds();
			Point2D center = calico.utils.Geometry.getMidPoint2D(group.getCoordList());
			int shift_x = ev.getPoint().x - (int)center.getX();
			int shift_y = ev.getPoint().y - (int)center.getY();
		
				
			Networking.send(CalicoPacket.getPacket(NetworkCommand.GROUP_COPY_TO_CANVAS, 
				this.uuid,
				CCanvasController.getCurrentUUID(),
				0L,
				shift_x,
				shift_y,
				ev.getPoint().x,
				ev.getPoint().y
			));
		}
	}
}
