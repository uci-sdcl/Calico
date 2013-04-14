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
package calico.plugins.iip.components.menus;

import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;

import calico.CalicoDraw;
import calico.components.menus.CanvasGenericMenuBar;
import calico.components.menus.buttons.ExitButton;
import calico.components.menus.buttons.HistoryNavigationBackButton;
import calico.components.menus.buttons.HistoryNavigationForwardButton;
import calico.inputhandlers.InputEventInfo;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.components.menus.buttons.NewClusterButton;
import calico.plugins.iip.components.menus.buttons.NewClusterCanvasButton;
import calico.plugins.iip.components.menus.buttons.ZoomToExtent;

/**
 * Menu bar for the Intention View. Includes simple buttons and a zoom slider with +/- buttons on either end. Input
 * events intersecting with the slider are forwarded to it.
 * 
 * @author Byron Hawkins
 */
public class IntentionGraphMenuBar extends CanvasGenericMenuBar
{
	private final Rectangle zoomSliderBounds;
	private final IntentionGraphZoomSlider zoomSlider;

	private boolean draggingZoomKnob = false;

	public IntentionGraphMenuBar(int screenPosition)
	{
		super(screenPosition, IntentionGraph.getInstance().getBounds());

		addCap(CanvasGenericMenuBar.ALIGN_START);
		
//		addIcon(new ZoomToExtent());
//		addIcon(new NewClusterButton());
//		addIcon(new NewClusterCanvasButton());

		if (IntentionGraph.getInstance().getFocus() == IntentionGraph.Focus.CLUSTER)
		{
			addIcon(new ZoomOutButton());
			addSpacer();
			addIcon(new ZoomInButton());
			addSpacer();
			addIcon(new ZoomToClusterMenuButton());
			addSpacer();
			addIcon(new ZoomToCenterRingMenuButton());
			
			
//			CalicoDraw.addChildToNode(this, zoomSlider);
			//		addChild(zoomSlider);
//			CalicoDraw.setNodeBounds(zoomSlider, zoomSliderBounds);
			//		zoomSlider.setBounds(zoomSliderBounds);
//			CalicoDraw.repaintNode(zoomSlider);
			//		zoomSlider.repaint();
		}
		
		addSpacer();

		zoomSliderBounds = addIcon(IntentionGraphZoomSlider.SPAN);
		zoomSlider = new IntentionGraphZoomSlider();
		

		
		addTextEndAligned("  Exit  ", new Font("Verdana", Font.BOLD, 12), new ExitButton());
		addSpacer(ALIGN_END);
//		addIconRightAligned(new HistoryNavigationForwardButton());
//		addIconRightAligned(new HistoryNavigationBackButton());

	}

	public void initialize()
	{
		zoomSlider.refreshState();
	}

	public void processEvent(InputEventInfo event)
	{
		switch (event.getAction())
		{
			case InputEventInfo.ACTION_PRESSED:
				draggingZoomKnob = zoomSliderBounds.contains(event.getGlobalPoint());
				clickMenu(event, event.getGlobalPoint());
				break;
			case InputEventInfo.ACTION_RELEASED:
				draggingZoomKnob = false;
				clickMenu(event, event.getGlobalPoint());
				break;
			case InputEventInfo.ACTION_DRAGGED:
				if (draggingZoomKnob && zoomSliderBounds.contains(event.getGlobalPoint()))
				{
					zoomSlider.dragTo(event.getGlobalPoint());
				}
				break;
		}
	}

	// @Override
	public void clickMenu(InputEventInfo event, Point point)
	{
		if (zoomSliderBounds.contains(point))
		{
			if (event.getAction() == InputEventInfo.ACTION_PRESSED)
			{
				zoomSlider.click(point);
			}
		}
		else
		{
			super.clickMenu(event, point);
		}
	}
}
