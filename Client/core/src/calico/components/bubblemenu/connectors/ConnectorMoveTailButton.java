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
package calico.components.bubblemenu.connectors;

import java.awt.Point;

import calico.Calico;
import calico.CalicoDataStore;
import calico.components.CConnector;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.decorators.CGroupDecorator;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CConnectorController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.inputhandlers.InputEventInfo;

public class ConnectorMoveTailButton extends PieMenuButton
{
	private boolean isActive = false;
	
	private Point prevPoint, mouseDownPoint;
	private CConnector tempConnector;
	private long tempGuuid;
	
	public ConnectorMoveTailButton(long uuid)
	{
		super("connector.point");
		draggable = true;
		this.uuid = uuid;
	}
	
	public void onPressed(InputEventInfo ev)
	{
		if (!CConnectorController.exists(uuid) || isActive)
		{
			return;
		}
		
		prevPoint = new Point();
		
		prevPoint.x = 0;
		prevPoint.y = 0;
		mouseDownPoint = null;
		tempConnector = CConnectorController.connectors.get(uuid);
		tempGuuid = 0l;
		
		ev.stop();
		BubbleMenu.isPerformingBubbleMenuAction = true;
		
		
		isActive = true;
		super.onPressed(ev);
	}
	
	public void onDragged(InputEventInfo ev)
	{
		if (mouseDownPoint == null)
		{
			prevPoint.x = ev.getPoint().x;
			prevPoint.y = ev.getPoint().y;
			mouseDownPoint = ev.getPoint();
			CConnectorController.move_group_anchor_start(uuid, CConnector.TYPE_TAIL);
		}

		CConnectorController.move_group_anchor(uuid, CConnector.TYPE_TAIL, (int)(ev.getPoint().x - prevPoint.x), ev.getPoint().y - prevPoint.y);
		
		//Change the highlight of the group associated with point B
		long guuid = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), this.tempConnector.getTail());

		if (guuid != tempGuuid)
		{
			if (tempGuuid != 0l)
			{
				CGroupController.groupdb.get(tempGuuid).highlight_off();
				CGroupController.groupdb.get(tempGuuid).highlight_repaint();
			}
			if (guuid != 0l && !(CGroupController.groupdb.get(guuid) instanceof CGroupDecorator))
			{
				CGroupController.groupdb.get(guuid).highlight_on();
				CGroupController.groupdb.get(guuid).highlight_repaint();
			}
			tempGuuid = guuid;
		}
		
		prevPoint.x = ev.getPoint().x;
		prevPoint.y = ev.getPoint().y;
		ev.stop();
	}
	
	public void onReleased(InputEventInfo ev)
	{
		if (tempGuuid != 0l)
		{
			CGroupController.groupdb.get(tempGuuid).highlight_off();
			CGroupController.groupdb.get(tempGuuid).highlight_repaint();
		}
		
		CConnectorController.move_group_anchor_end(uuid, CConnector.TYPE_TAIL);
		
		ev.stop();
		
		Calico.logger.debug("CLICKED MOVE TAIL BUTTON");
		
		isActive = false;
	}
	
	@Override
	public Point getPreferredPosition()
	{
		return CConnectorController.connectors.get(uuid).getTail();
	}
	
}
