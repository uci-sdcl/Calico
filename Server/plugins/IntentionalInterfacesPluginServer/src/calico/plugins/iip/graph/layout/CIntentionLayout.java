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
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import calico.controllers.CCanvasController;
import calico.plugins.iip.CCanvasLink;
import calico.plugins.iip.IntentionalInterfaceState;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.graph.layout.CIntentionTopology.Cluster;
import edu.umd.cs.piccolo.util.PBounds;

public class CIntentionLayout
{
	private static final CIntentionLayout INSTANCE = new CIntentionLayout();

	public static CIntentionLayout getInstance()
	{
		return INSTANCE;
	}
	
	private static int calculateCellDiameter(Dimension cellSize)
	{
		return ((int) Math.sqrt((cellSize.height * cellSize.height) + (double) (cellSize.width * cellSize.width)));
	}

	public static Point centerCanvasAt(int x, int y)
	{
		return new Point(x - (CIntentionLayout.INTENTION_CELL_SIZE.width / 2), y - (CIntentionLayout.INTENTION_CELL_SIZE.height / 2));
	}

	public static final Dimension INTENTION_CELL_SIZE = new Dimension(200, 130);
	static final int INTENTION_CELL_DIAMETER = calculateCellDiameter(INTENTION_CELL_SIZE);

	private final CIntentionClusterGraph graph = new CIntentionClusterGraph();
	private final CIntentionTopology topology = new CIntentionTopology();

	public CIntentionTopology getTopology()
	{
		return topology;
	}

	public void populateState(IntentionalInterfaceState state)
	{
		state.setTopologyPacket(topology.createPacket());
		state.setClusterGraphPacket(graph.createPacket());
		
	}
	
	public void inflateStoredClusterGraph(String graphData)
	{
		graph.inflateStoredData(graphData);
	}

	public long getRootCanvasId(long canvasId)
	{
		while (true)
		{ // walk to the root of the cluster
			Long linkId = CCanvasLinkController.getInstance().getIncomingLink(canvasId);
			if (linkId == null)
			{
				break;
			}
			else
			{
				CCanvasLink link = CCanvasLinkController.getInstance().getLink(linkId);
				canvasId = link.getAnchorA().getCanvasId();
			}
		}
		return canvasId;
	}

	public void replaceCluster(long originalRootCanvasId, long newRootCanvasId)
	{
		CIntentionCluster cluster = new CIntentionCluster(newRootCanvasId);
		graph.replaceCluster(originalRootCanvasId, cluster);
	}

	public void insertCluster(long rootCanvasId)
	{
		CIntentionCluster cluster = new CIntentionCluster(rootCanvasId);
		graph.insertCluster(cluster);
	}

	public void insertCluster(long contextCanvasId, long rootCanvasId)
	{
		CIntentionCluster cluster = new CIntentionCluster(rootCanvasId);
		graph.insertCluster(getRootCanvasId(contextCanvasId), cluster);
	}

	public void removeClusterIfAny(long rootCanvasId)
	{
		graph.removeClusterIfAny(rootCanvasId);
	}

	public List<CIntentionClusterLayout> layoutGraph()
	{
		topology.clear();

		List<CIntentionClusterLayout> clusterLayouts = graph.layoutClusters();
		for (CIntentionClusterLayout clusterLayout : clusterLayouts)
		{
			topology.addCluster(clusterLayout);
		}

		graph.reset();
		
		return clusterLayouts;
	}

	static int getCanvasIndex(long canvasId)
	{
		if (canvasId < 0L)
		{
			return -1;
		}
		return CCanvasController.canvases.get(canvasId).getIndex();
	}
	
	public int getClusterCount()
	{
		return graph.getClusterCount();
	}
	
	public Rectangle getTopologyBounds()
	{
		return topology.getTopologyBounds();
	}
	
	public Rectangle getClusterBounds(long rootCanvasId)
	{
		for (Cluster c : topology.getClusters())
			if (c.getRootCanvasId() == rootCanvasId)
				return c.getOuterBoxBounds();
		
		return null;
	}
	
	public Point2D getArrowAnchorPosition(long canvas_uuid, long opposite_canvas_uuid)
	{
		calico.plugins.iip.CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(opposite_canvas_uuid);
		return getArrowAnchorPosition(canvas_uuid, cell.getCenter());
	}

	public Point2D getArrowAnchorPosition(long canvas_uuid, Point2D opposite)
	{
		return getArrowAnchorPosition(canvas_uuid, opposite.getX(), opposite.getY());
	}

	public Point2D getArrowAnchorPosition(long canvas_uuid, double xOpposite, double yOpposite)
	{
		calico.plugins.iip.CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid);
		return alignAnchorAtCellEdge(cell.copyBounds(), xOpposite, yOpposite);
	}
	
	/**
	 * Trig machinery to align arrow anchors with CIC edges.
	 */
	private Point2D alignAnchorAtCellEdge(Rectangle2D cellBounds, double xOpposite, double yOpposite)
	{
		double[] intersection = new double[2];
		for (CellEdge edge : CellEdge.values())
		{
			if (edge.findIntersection(cellBounds, xOpposite, yOpposite, intersection))
			{
				return new Point2D.Double(intersection[0], intersection[1]);
			}
		}

//		System.out.println("Failed to align an arrow to a CIntentionCell edge--can't find the arrow's intersection with the cell!");

		return new Point2D.Double(cellBounds.getCenterX(), cellBounds.getCenterY());
	}
	
	/**
	 * Utility for calculating the intersection of a line with the bounds of a CIC, used for placing arrow endpoints
	 * adjacent to cell edges.
	 * 
	 * @author Byron Hawkins
	 */
	private enum CellEdge
	{
		TOP
		{
			@Override
			boolean findIntersection(Rectangle2D cellBounds, double xOpposite, double yOpposite, double[] intersection)
			{
				int result = calico.utils.Geometry.findLineSegmentIntersection(cellBounds.getX(), cellBounds.getY(), cellBounds.getX() + cellBounds.getWidth(),
						cellBounds.getY(), cellBounds.getCenterX(), cellBounds.getCenterY(), xOpposite, yOpposite, intersection);
				return result == 1;
			}
		},
		RIGHT
		{
			@Override
			boolean findIntersection(Rectangle2D cellBounds, double xOpposite, double yOpposite, double[] intersection)
			{
				int result = calico.utils.Geometry.findLineSegmentIntersection(cellBounds.getX(), cellBounds.getY() + cellBounds.getHeight(),
						cellBounds.getX() + cellBounds.getWidth(), cellBounds.getY() + cellBounds.getHeight(), cellBounds.getCenterX(),
						cellBounds.getCenterY(), xOpposite, yOpposite, intersection);
				return result == 1;
			}
		},
		BOTTOM
		{
			@Override
			boolean findIntersection(Rectangle2D cellBounds, double xOpposite, double yOpposite, double[] intersection)
			{
				int result = calico.utils.Geometry.findLineSegmentIntersection(cellBounds.getX(), cellBounds.getY(), cellBounds.getX(),
						cellBounds.getY() + cellBounds.getHeight(), cellBounds.getCenterX(), cellBounds.getCenterY(), xOpposite, yOpposite, intersection);
				return result == 1;
			}
		},
		LEFT
		{
			@Override
			boolean findIntersection(Rectangle2D cellBounds, double xOpposite, double yOpposite, double[] intersection)
			{
				int result = calico.utils.Geometry.findLineSegmentIntersection(cellBounds.getX() + cellBounds.getWidth(), cellBounds.getY(),
						cellBounds.getX() + cellBounds.getWidth(), cellBounds.getY() + cellBounds.getHeight(), cellBounds.getCenterX(),
						cellBounds.getCenterY(), xOpposite, yOpposite, intersection);
				return result == 1;
			}
		};

		abstract boolean findIntersection(Rectangle2D cellBounds, double xOpposite, double yOpposite, double[] intersection);
	}
	
}
