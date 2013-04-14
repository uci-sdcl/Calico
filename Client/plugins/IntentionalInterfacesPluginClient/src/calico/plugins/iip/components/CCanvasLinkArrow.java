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
package calico.plugins.iip.components;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import calico.CalicoDraw;
import calico.Geometry;
import calico.components.arrow.AbstractArrow;
import calico.plugins.iip.components.CCanvasLinkAnchor.ArrowEndpointType;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.IntentionCanvasController;
import calico.plugins.iip.util.IntentionalInterfacesGraphics;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * Represents a <code>CCanvasLink</code> in the Piccolo component hierarchy of the Intention View. May have a label in
 * the middle of the arrow stem.
 * 
 * @author Byron Hawkins
 */
public class CCanvasLinkArrow extends AbstractArrow<CCanvasLinkAnchor>
{
	public static final Color NORMAL_COLOR = Color.black;
	public static final Color HIGHLIGHTED_COLOR = new Color(0xFFFF30);
	public static final Color FLOATING_COLOR = new Color(0x888888);

	private final CCanvasLink link;

	public CCanvasLinkArrow(CCanvasLink link)
	{
		super(Color.black, TYPE_NORM_HEAD_B);

		this.link = link;

		setAnchorA(link.getAnchorA());
		setAnchorB(link.getAnchorB());

		setHighlighted(false);
	}

	public long getId()	{
		return link.getId();
	}

	public void setHighlighted(boolean b)
	{
		if (b)
		{
			setColor(HIGHLIGHTED_COLOR);
		}
		else
		{
			if ((link.getAnchorA().getArrowEndpointType() == ArrowEndpointType.FLOATING)
					|| (link.getAnchorB().getArrowEndpointType() == ArrowEndpointType.FLOATING))
			{
				setColor(FLOATING_COLOR);
			}
			else
			{
				setColor(NORMAL_COLOR);
			}
		}

		redraw();
	}

	@Override
	protected void addRenderingElements()
	{
		super.addRenderingElements();
		PText label = null;
		
		if (CIntentionCellController.getInstance().isRootCanvas(link.getAnchorA().getCanvasId()))
		{
			label = IntentionalInterfacesGraphics.createLabelOnSegment(link.getLabel(), link.getAnchorA().getPoint(), link.getAnchorB().getPoint());
			label.setTextPaint(getColor());
		}
		else
		{
			//get intention type for target cavnas
			CIntentionType type = IntentionCanvasController.getInstance().getIntentionType(CIntentionCellController.getInstance().getCellByCanvasId(link.getAnchorB().getCanvasId()).getIntentionTypeId());
			String name = link.getLabel();
			if (type != null)
			{
				name = type.getName();
				
			}
			label = IntentionalInterfacesGraphics.createLabelOnSegment(name, link.getAnchorA().getPoint(), link.getAnchorB().getPoint());
			if (type != null)
				label.setPaint(type.getColor());
			label.setTextPaint(getColor());
			
		}
		
		CalicoDraw.addChildToNode(this, label, 0);
//		addChild(0, label);
	}
}
