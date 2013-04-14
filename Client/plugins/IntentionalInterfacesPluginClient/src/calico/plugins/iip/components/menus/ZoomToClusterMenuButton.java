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

import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import calico.components.bubblemenu.BubbleMenu;
import calico.components.menus.CanvasMenuButton;
import calico.perspectives.CalicoPerspective;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.IntentionCanvasController;
import calico.plugins.iip.iconsets.CalicoIconManager;
import calico.plugins.iip.perspectives.IntentionalInterfacesPerspective;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * Simple button to show the tag panel. This feature is obsolete.
 * 
 * @author Byron Hawkins
 */
public class ZoomToClusterMenuButton extends CanvasMenuButton
{
	private static final long serialVersionUID = 1L;



	/**
	 * Instantiated via reflection in CanvasStatusBar
	 */
	public ZoomToClusterMenuButton()
	{


		try
		{
			setImage(CalicoIconManager.getIconImage("intention-graph.zoom-out-ring-menu"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

//		CanvasPerspectiveController.getInstance().canvasIntentionToolBarCreated(toolbar);
	}

	public void actionMouseClicked()
	{
		super.actionMouseClicked();
		
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					IntentionGraph.getInstance().setFocusToCluster(IntentionGraph.getInstance().getClusterInFocus(), false);
					if (BubbleMenu.isBubbleMenuActive())
						BubbleMenu.clearMenu();
				}});

		
		
	}
}
