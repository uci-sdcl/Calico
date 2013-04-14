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

import calico.*;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.modules.StatusMessage;
import calico.components.*;
import calico.components.arrow.CArrow;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CArrowController;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;

import java.awt.*;

@Deprecated
public class ArrowSetAnchorButton extends PieMenuButton
{
	private Point point = null;
	public ArrowSetAnchorButton(Point p)
	{
		super("arrow.create");
		point = p;
	}
	
	
	public void onClick(InputEventInfo ev)
	{
		if(CArrowController.getOutstandingAnchorPoint()==null)
		{
			CArrowController.setOutstandingAnchorPoint(point);
			
			//CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).menuBar.redrawArrowIndicator();
			
			if(CalicoOptions.arrow.show_creation_popup)
			{
				StatusMessage.popup("Please click to place the end point for the arrow.");
			}
		}
		else
		{
			// It is not null, so we want to finish it
			Point start = CArrowController.getOutstandingAnchorPoint();
			Point end = point;
			long startGroupUID = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), start);
			long endGroupUID = CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), end);
			if(endGroupUID==startGroupUID && endGroupUID!=0L)
			{
				return;
			}

			long curCanvasUID = CCanvasController.getCurrentUUID();
			int startType = CArrow.TYPE_GROUP;
			int endType = CArrow.TYPE_GROUP;
			
			if(startGroupUID==0L)
			{
				startGroupUID = curCanvasUID;
				startType = CArrow.TYPE_CANVAS;
			}
			if(endGroupUID==0L)
			{
				endGroupUID = curCanvasUID;
				endType = CArrow.TYPE_CANVAS;
			}

			// ARROW!
			/*CArrowController.start(Calico.uuid(), curCanvasUID, 
					startType, startGroupUID, start,
					endType, endGroupUID, end
				);
				*/
		
			CArrowController.setOutstandingAnchorPoint(null);
			
			//CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).menuBar.redrawArrowIndicator();
			
		}
		ev.stop();
		
	}
	

	
}
