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

import java.awt.Point;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.SwingUtilities;

import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.iconsets.CalicoIconManager;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolox.nodes.PComposite;

/**
 * Represents the zoom slider in the Piccolo component hierarchy. The zoom ratio is maintained directly as a
 * <code>double</code>, so <code>1.0</code> is correlated with the center of the slider. The knob may be positioned by
 * dragging, or a click anywhere in the slider will move the knob to that position. When the zoom ratio changes, whether
 * by an action on this slider or some other action, this slider expects to be notified with a call to
 * <code>refreshState()</code>.
 * 
 * @author Byron Hawkins
 */
public class IntentionGraphZoomSlider extends PComposite implements PropertyChangeListener
{
	public static final int SPAN = CalicoOptions.menu.menubar.defaultIconDimension;

	private final PImage knob;
	private final PImage zoomOutButton;
	private final PImage slider;
	private final PImage zoomInButton;

	private double buttonSpan;
	private double knobInset;

	public IntentionGraphZoomSlider()
	{
		knob = new PImage(CalicoIconManager.getIconImage("intention-graph.zoom-knob"));
		zoomOutButton = new PImage(CalicoIconManager.getIconImage("intention-graph.zoom-out"));
		slider = new PImage(CalicoIconManager.getIconImage("intention-graph.zoom-slider"));
		zoomInButton = new PImage(CalicoIconManager.getIconImage("intention-graph.zoom-in"));

//		addChild(zoomOutButton);
//		addChild(slider);
//		addChild(zoomInButton);
//		addChild(knob);
		CalicoDraw.addChildToNode(this, zoomOutButton);
//		CalicoDraw.addChildToNode(this, slider);
		CalicoDraw.addChildToNode(this, zoomInButton);
		CalicoDraw.addChildToNode(this, knob);

		IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).addPropertyChangeListener(PNode.PROPERTY_TRANSFORM, this);
	}

	public void refreshState()
	{
		updateKnobPosition();
	}

	public void dragTo(Point point)
	{
		final Point2D centerOriginal = new Point2D.Double(IntentionGraph.getInstance().getGlobalCoordinatesForVisibleBounds().getCenterX(),
				IntentionGraph.getInstance().getGlobalCoordinatesForVisibleBounds().getCenterY());
		PBounds bounds = getBounds();
		double x = (point.x - bounds.x);
		if ((x > buttonSpan) && (x < (bounds.width - buttonSpan)))
		{
			double scale = convertSlidePointToScale(point);
			IntentionGraph.getInstance().setScale(scale);
			SwingUtilities.invokeLater(
					new Runnable() { public void run() {
						final Point2D center =  new Point2D.Double(IntentionGraph.getInstance().getGlobalCoordinatesForVisibleBounds().getCenterX(),
								IntentionGraph.getInstance().getGlobalCoordinatesForVisibleBounds().getCenterY());

						IntentionGraph.getInstance().translate(center.getX() - centerOriginal.getX(), 
								center.getY() - centerOriginal.getY());
						IntentionGraph.getInstance().repaint(); 
					}});

//			System.out.println("zoom to " + scale);
		}
	}

	public void click(Point point)
	{
		final Point2D centerOriginal = new Point2D.Double(IntentionGraph.getInstance().getGlobalCoordinatesForVisibleBounds().getCenterX(),
				IntentionGraph.getInstance().getGlobalCoordinatesForVisibleBounds().getCenterY());
		PBounds bounds = getBounds();
		double x = (point.x - bounds.x);

		double scale = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale();

		if (x < buttonSpan)
		{
			if (IntentionGraph.getInstance().getDefaultScale() == IntentionGraph.getInstance().getScale())
			{
				IntentionGraph.getInstance().setFocusToCluster(IntentionGraph.getInstance().getClusterInFocus(), false);
				return;
			}
			
			if (scale <= 0.2)
			{
				scale = 0.1;
			}
			else if (scale <= 1.0)
			{
				scale -= 0.1;
			}
			else if (scale < 1.5)
			{
				scale = 0.9;
			}
			else
			{
				scale -= 1.0;
			}
		}
		else if (x > (bounds.width - buttonSpan))
		{
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
		}
		else
		{
//			scale = convertSlidePointToScale(point);
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

	private double convertSlidePointToScale(Point point)
	{
		double x = (point.x - getBounds().x);
		double sliderWidth = slider.getBounds().width - (2 * knobInset);
		double sliderPosition = Math.min(sliderWidth, Math.max(0, x - (buttonSpan + knobInset)));
		double sliderCenter = sliderWidth / 2;
		double ratio;
		if (sliderPosition < sliderCenter)
		{
			ratio = Math.max(0.1, sliderPosition / sliderCenter);
		}
		else if (sliderPosition > sliderCenter)
		{
			ratio = 1.0 + (((sliderPosition - sliderCenter) / sliderCenter) * 9.0);
		}
		else
		{
			ratio = 1.0;
		}

		return ratio;
	}

	@Override
	protected void layoutChildren()
	{
		PBounds bounds = getBounds();

		buttonSpan = bounds.height;
		zoomOutButton.setBounds(bounds.x, bounds.y, buttonSpan, buttonSpan);
		zoomInButton.setBounds(bounds.x + bounds.width - buttonSpan, bounds.y, buttonSpan, buttonSpan);

		double sliderWidth = bounds.width - (2 * buttonSpan);
		slider.setBounds(bounds.x + buttonSpan, bounds.y, sliderWidth, bounds.height);

		knobInset = sliderWidth * 0.05;

		updateKnobPosition();
	}

	private void updateKnobPosition()
	{
		double knobHeight = getBounds().height / 2.0;
		double knobWidth = knob.getImage().getWidth(null) * (knobHeight / knob.getImage().getHeight(null));

		double scale = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale();

		// limit extremes
		if (scale < 0.0)
		{
			scale = 0.0;
		}
		else if (scale > 10.0)
		{
			scale = 10.0;
		}

		double ratio = scale;
		if (scale > 1.0)
		{
			ratio = scale / 10.0; // invert to [0.1 - 1.0]
			ratio = (ratio - 0.1) / 0.9; // normalize to [0.0 - 1.0]
		}

		double sliderHalfWidth = ((slider.getBounds().width - (2 * knobInset)) / 2.0);
		double knobHalfWidth = (knobWidth / 2.0);
		double xCenter = sliderHalfWidth - knobHalfWidth;
		double knobCenter;
		if (scale < 1.0)
		{
			double distanceFromCenter = sliderHalfWidth - (sliderHalfWidth * ratio);
			knobCenter = xCenter - distanceFromCenter;
		}
		else if (scale > 1.0)
		{
			double distanceFromCenter = sliderHalfWidth * ratio;
			knobCenter = xCenter + distanceFromCenter;
		}
		else
		{
			knobCenter = xCenter;
		}

		double yOffset = (getBounds().height - knobHeight) / 2.0;
		knob.setBounds(slider.getBounds().x + knobInset + knobCenter, getBounds().y + yOffset, knobWidth, knobHeight);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event)
	{
		updateKnobPosition();
	}
}
