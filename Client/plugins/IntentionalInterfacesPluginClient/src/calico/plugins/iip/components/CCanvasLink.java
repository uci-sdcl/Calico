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

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import calico.plugins.iip.components.graph.IntentionGraph;

/**
 * Represents a link in this plugin's internal model of the intention graph. The Piccolo component representative is the
 * <code>CCanvasLinkArrow</code>.
 * 
 * @author Byron Hawkins
 */
public class CCanvasLink
{
	/**
	 * Simple enum which specifies how a link relates to a particular <code>CIntentionCell</code>.
	 * 
	 * @author Byron Hawkins
	 */
	public enum LinkDirection
	{
		INCOMING,
		OUTGOING;
	}

	/**
	 * Specifies how far away from an arrow an input press event may occur and still be considered a press on the arrow.
	 */
	private static final double HIT_PROXIMITY = 10.0;

	private long uuid;

	/**
	 * The "from" end of the arrow.
	 */
	private CCanvasLinkAnchor anchorA;
	/**
	 * The "to" end of the arrow.
	 */
	private CCanvasLinkAnchor anchorB;

	/**
	 * The label text of the arrow, or null if there is no label.
	 */
	private String label;

	// these instances are used for calculations
	private final Line2D hitTestLink = new Line2D.Double();
	private final Point2D hitTestPoint = new Point2D.Double();

	public CCanvasLink(long uuid, CCanvasLinkAnchor anchorA, CCanvasLinkAnchor anchorB, String label)
	{
		this.uuid = uuid;
		this.anchorA = anchorA;
		this.anchorB = anchorB;

		setLabel(label);

		anchorA.setLink(this);
		anchorB.setLink(this);
	}

	public long getId()
	{
		return uuid;
	}

	public CCanvasLinkAnchor getAnchorA()
	{
		return anchorA;
	}

	public CCanvasLinkAnchor getAnchorB()
	{
		return anchorB;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String userLabel)
	{
		this.label = userLabel;
	}

	/**
	 * Discern whether an input event at <code>point</code> can be considered effective for this arrow.
	 */
	public boolean contains(Point2D point)
	{
		if (IntentionGraph.getInstance().isClusterRoot(getAnchorA().getCanvasId()))
			return false;
		hitTestPoint.setLocation(point);
		IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).globalToLocal(hitTestPoint);
		hitTestLink.setLine(anchorA.getPoint(), anchorB.getPoint());
		double proximity = hitTestLink.ptSegDist(hitTestPoint);
		return proximity < HIT_PROXIMITY;
	}
}
