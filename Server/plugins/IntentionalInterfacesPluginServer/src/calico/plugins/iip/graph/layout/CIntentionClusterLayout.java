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
package calico.plugins.iip.graph.layout;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public class CIntentionClusterLayout
{
	public class CanvasPosition
	{
		public final long canvasId;
		public final Point location;

		CanvasPosition(long canvasId, Point location)
		{
			this.canvasId = canvasId;
			this.location = location;
		}

		void translateBy(int x, int y)
		{
			location.x += x;
			location.y += y;
		}
	}

	private final CIntentionCluster cluster;
	private final List<CanvasPosition> canvasPositions = new ArrayList<CanvasPosition>();

	// transitory value, per getBoundingBox()
	private boolean isCalculated = false;
	private final Point rootCanvasPosition = new Point();
	private final Dimension boundingBox = new Dimension();
	private final Dimension outerBox = new Dimension();

	CIntentionClusterLayout(CIntentionCluster cluster)
	{
		this.cluster = cluster;
	}

	void reset()
	{
		isCalculated = false;
		rootCanvasPosition.setLocation(0, 0);
		boundingBox.setSize(0, 0);
	}

	public CIntentionCluster getCluster()
	{
		return cluster;
	}

	public List<CanvasPosition> getCanvasPositions()
	{
		return canvasPositions;
	}

	void addCanvas(long canvasId, Point location)
	{
		canvasPositions.add(new CanvasPosition(canvasId, location));
	}

	Point getLayoutCenterWithinBounds(Dimension bounds)
	{
		if (!isCalculated)
			calculate();

		int xInset = (bounds.width - boundingBox.width) / 2;
		int yInset = (bounds.height - boundingBox.height) / 2;
		return new Point(rootCanvasPosition.x + xInset + (CIntentionLayout.INTENTION_CELL_SIZE.width / 2), rootCanvasPosition.y + yInset
				+ (CIntentionLayout.INTENTION_CELL_SIZE.height / 2));
	}

	public Dimension getBoundingBox()
	{
		if (!isCalculated)
			calculate();

		return boundingBox;
	}
	
	public Dimension getOuterBox()
	{
		return outerBox;
	}
	
	public void setOuterBox(Dimension outerBox)
	{
		if (outerBox != null)
			this.outerBox.setSize(outerBox);  
	}

	private void calculate()
	{
		boundingBox.width = (int) Math.ceil(cluster.getOccupiedSpan());// (xMax - xMin) /*+ 20*/;
		boundingBox.height = (int) Math.ceil(cluster.getOccupiedSpan()); //(yMax - yMin) /*+ 20*/;
		
		int xMin = Integer.MAX_VALUE;
		int xMax = -Integer.MAX_VALUE;
		int yMin = Integer.MAX_VALUE;
		int yMax = -Integer.MAX_VALUE;

		CanvasPosition rootCanvas = null;

		for (CanvasPosition position : canvasPositions)
		{
			xMin = Math.min(position.location.x, xMin);
			yMin = Math.min(position.location.y, yMin);
			xMax = Math.max(position.location.x + CIntentionLayout.INTENTION_CELL_SIZE.width, xMax);
			yMax = Math.max(position.location.y + CIntentionLayout.INTENTION_CELL_SIZE.height, yMax);

			if (position.canvasId == cluster.getRootCanvasId())
			{
				rootCanvas = position;
			}
		}

//		rootCanvasPosition.x = (rootCanvas.location.x - (xMin /* - 10*/)); // clumsy handling of the buffer spacing
//		rootCanvasPosition.y = (rootCanvas.location.y - (yMin /*- 10*/));
		rootCanvasPosition.x = (boundingBox.width/2 - CIntentionLayout.INTENTION_CELL_SIZE.width/2);
		rootCanvasPosition.y = (boundingBox.height/2 - CIntentionLayout.INTENTION_CELL_SIZE.height/2);

//		boundingBox.setSize(CIntentionClusterGraph.getClusterDimensions());
//		boundingBox.width = (int) Math.ceil(cluster.getOccupiedSpan());// (xMax - xMin) /*+ 20*/;
//		boundingBox.height = (int) Math.ceil(cluster.getOccupiedSpan()); //(yMax - yMin) /*+ 20*/;

		isCalculated = true;
	}
}
