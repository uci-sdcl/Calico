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

import calico.components.menus.CanvasMenuButton;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.controllers.IntentionCanvasController;
import calico.plugins.iip.iconsets.CalicoIconManager;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * Simple button to show the tag panel. This feature is obsolete.
 * 
 * @author Byron Hawkins
 */
public class ZoomInButton extends CanvasMenuButton
{
	private static final long serialVersionUID = 1L;



	/**
	 * Instantiated via reflection in CanvasStatusBar
	 */
	public ZoomInButton()
	{


		try
		{
			setImage(CalicoIconManager.getIconImage("intention-graph.zoom-in"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

//		CanvasPerspectiveController.getInstance().canvasIntentionToolBarCreated(toolbar);
	}

	public void actionMouseClicked()
	{
		final Point2D centerOriginal = new Point2D.Double(IntentionGraph.getInstance().getGlobalCoordinatesForVisibleBounds().getCenterX(),
				IntentionGraph.getInstance().getGlobalCoordinatesForVisibleBounds().getCenterY());
		double scale = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale();
		if (scale >= 9.0)
		{
			scale = 10.0;
		}
		else if (scale <= 0.9)
		{
			scale += 0.1;
		}
		else if (scale < 0.95)
		{
			scale = 1.5;
		}
		else
		{
			scale += 1.0;
		}
		
		IntentionGraph.getInstance().setScale(scale);
		SwingUtilities.invokeLater(
				new Runnable() { public void run() {
					final Point2D center =  new Point2D.Double(IntentionGraph.getInstance().getGlobalCoordinatesForVisibleBounds().getCenterX(),
							IntentionGraph.getInstance().getGlobalCoordinatesForVisibleBounds().getCenterY());

					IntentionGraph.getInstance().translate(center.getX() - centerOriginal.getX(), 
							center.getY() - centerOriginal.getY());
					IntentionGraph.getInstance().repaint(); 
				}});
	}
}
