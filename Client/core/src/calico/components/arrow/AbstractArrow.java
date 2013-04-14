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
package calico.components.arrow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;

import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.Geometry;
import calico.controllers.CGroupController;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolox.nodes.PComposite;
import edu.umd.cs.piccolox.nodes.PLine;

public abstract class AbstractArrow<AnchorType extends AbstractArrowAnchorPoint> extends PComposite
{
	public static final int TYPE_NORM_HEAD_A = 1;
	public static final int TYPE_NORM_HEAD_B = 2;
	public static final int TYPE_NORM_HEAD_AB = 3;

	public static final int REGION_TAIL = 1 << 0;
	public static final int REGION_MIDDLE = 1 << 1;
	public static final int REGION_HEAD = 1 << 2;

	public static final int ANCHOR_A = 1 << 0;
	public static final int ANCHOR_B = 1 << 1;

	private static final long serialVersionUID = 1L;

	protected AnchorType anchorA = null;
	protected AnchorType anchorB = null;

	private PPath arrowHeadA = null;
	private PPath arrowHeadB = null;
	private PLine arrowLine = null;

	private int arrowType = TYPE_NORM_HEAD_B;

	// This is used as a larger polygon, used to check if the mouse is touching the arrow.
	@Deprecated
	private Polygon arrowContainment = new Polygon();

	private Polygon pointPath = new Polygon();

	private Color color = Color.BLACK;

	private Polygon poly_anchorA = new Polygon();
	private Polygon poly_anchorB = new Polygon();
	private Polygon poly_line = new Polygon();

	public AbstractArrow(Color color, int type)
	{
		this.color = color;
		this.arrowType = type;
	}
	
	public Color getColor()
	{
		return this.color;
	}

	public int getArrowType()
	{
		return arrowType;
	}

	public void setArrowType(int type)
	{
		arrowType = type;
	}

	public void setColor(Color color)
	{
		this.color = color;
	}

	public AnchorType getAnchorA()
	{
		return this.anchorA;
	}

	public AnchorType getAnchorB()
	{
		return this.anchorB;
	}

	public void setAnchorA(AnchorType anchor)
	{
		this.anchorA = anchor;
	}

	public void setAnchorB(AnchorType anchor)
	{
		this.anchorB = anchor;
	}

	public Polygon getPolygon()
	{
		return pointPath;
	}

	@Deprecated
	public boolean containsMousePoint(Point point)
	{
		return arrowContainment.contains(point);
	}

	public double getPointPercentage(Point point)
	{
		double dist = anchorA.getPoint().distance(anchorB.getPoint());
		double pointDist = anchorA.getPoint().distance(point);

		return (100.0 * (pointDist / dist));
	}

	public static int getPercentRegion(double percent)
	{
		if (percent < 20.0)
		{
			return CArrow.REGION_TAIL;
		}
		else if (percent > 80.0)
		{
			return CArrow.REGION_HEAD;
		}
		return CArrow.REGION_MIDDLE;
	}

	public Polygon getLinePolygon()
	{
		Polygon temp = new Polygon();
		temp.addPoint(anchorA.getPoint().x, anchorA.getPoint().y);
		temp.addPoint(anchorB.getPoint().x, anchorB.getPoint().y);
		temp.addPoint(anchorA.getPoint().x, anchorA.getPoint().y);
		return temp;
	}

	public void moveAnchor(int anchor, int x, int y)
	{
		if (anchor == CArrow.ANCHOR_A)
		{
			anchorA.translate(x, y);
		}
		else
		{
			anchorB.translate(x, y);
		}
		redraw();
	}

	public void redraw()
	{
		redraw(true);
	}

	public void redraw(boolean repaint)
	{
		//this.removeAllChildren();
		//CalicoDraw.removeAllChildrenFromNode(this);
		CalicoDraw.removeAllChildrenFromNode(this);
		addRenderingElements();
		
		if (arrowType == CArrow.TYPE_NORM_HEAD_AB || arrowType == CArrow.TYPE_NORM_HEAD_A)
		{
			CalicoDraw.addChildToNode(this, arrowHeadA, 0);
		}
		if (arrowType == CArrow.TYPE_NORM_HEAD_AB || arrowType == CArrow.TYPE_NORM_HEAD_B)
		{
			CalicoDraw.addChildToNode(this, arrowHeadB, 0);
		}
		CalicoDraw.addChildToNode(this, arrowLine, 0);
		//this.repaint();
//		CalicoDraw.repaint(this);
		if (repaint)
		{
			CalicoDraw.repaint(this);
			//this.setPaintInvalid(true);
			//CalicoDraw.setNodePaintInvalid(this, true);
		}
	}
	
	protected void addRenderingElements()
	{
		if (arrowType == CArrow.TYPE_NORM_HEAD_AB || arrowType == CArrow.TYPE_NORM_HEAD_A)
		{
			arrowHeadA = null;

			int[] apoints = Geometry.createArrow(anchorB.getPoint().x, anchorB.getPoint().y, anchorA.getPoint().x, anchorA.getPoint().y,
					CalicoOptions.arrow.length, CalicoOptions.arrow.angle, CalicoOptions.arrow.inset);

			arrowHeadA = new PPath();
			arrowHeadA.moveTo((float) apoints[0], (float) apoints[1]);
			for (int i = 2; i < apoints.length; i = i + 2)
			{
				arrowHeadA.lineTo((float) apoints[i], (float) apoints[i + 1]);
			}
			arrowHeadA.setStroke(new BasicStroke(CalicoOptions.arrow.stroke_size));
			arrowHeadA.setStrokePaint(this.color);
			arrowHeadA.setPaint(this.color);

			//this.addChild(0, arrowHeadA);
			//CalicoDraw.addChildToNode(this, arrowHeadA, 0);
		}
		if (arrowType == CArrow.TYPE_NORM_HEAD_AB || arrowType == CArrow.TYPE_NORM_HEAD_B)
		{
			int[] bpoints = Geometry.createArrow(anchorA.getPoint().x, anchorA.getPoint().y, anchorB.getPoint().x, anchorB.getPoint().y,
					CalicoOptions.arrow.length, CalicoOptions.arrow.angle, CalicoOptions.arrow.inset);

			arrowHeadB = new PPath();
			arrowHeadB.moveTo((float) bpoints[0], (float) bpoints[1]);
			for (int i = 2; i < bpoints.length; i = i + 2)
			{
				arrowHeadB.lineTo((float) bpoints[i], (float) bpoints[i + 1]);
			}
			arrowHeadB.setStroke(new BasicStroke(CalicoOptions.arrow.stroke_size));
			arrowHeadB.setStrokePaint(this.color);
			arrowHeadB.setPaint(this.color);
			//this.addChild(0, arrowHeadB);
			//CalicoDraw.addChildToNode(this, arrowHeadB, 0);
		}

		arrowLine = new PLine();
		arrowLine.addPoint(0, anchorA.getPoint().x, anchorA.getPoint().y);
		arrowLine.addPoint(1, anchorB.getPoint().x, anchorB.getPoint().y);
		arrowLine.setStroke(new BasicStroke(CalicoOptions.arrow.stroke_size));
		arrowLine.setStrokePaint(this.color);
		arrowLine.setPaint(this.color);

		//this.addChild(0, arrowLine);
		//CalicoDraw.addChildToNode(this, arrowLine, 0);
	}

	public int get_signature()
	{
		return anchorA.getPoint().x + anchorA.getPoint().y + anchorB.getPoint().x + anchorB.getPoint().y;
	}
}
