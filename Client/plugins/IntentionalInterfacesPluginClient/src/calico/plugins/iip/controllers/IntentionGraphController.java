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
package calico.plugins.iip.controllers;

import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;

import java.awt.Color;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.SwingUtilities;

import calico.Calico;
import calico.CalicoDraw;
import calico.Geometry;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.menus.GridBottomMenuBar;
import calico.controllers.CCanvasController;
import calico.networking.Networking;
import calico.networking.PacketHandler;
import calico.networking.netstuff.CalicoPacket;
import calico.plugins.iip.IntentionalInterfacesNetworkCommands;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.CCanvasLinkAnchor;
import calico.plugins.iip.components.CCanvasLinkAnchor.ArrowEndpointType;
import calico.plugins.iip.components.CCanvasLinkArrow;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.canvas.ShowIntentionGraphButton;
import calico.plugins.iip.components.graph.IntentionGraph;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * Coordinates visual components of the Intention View with this plugin's internal model and the server.
 * 
 * @author Byron Hawkins
 */
public class IntentionGraphController
{
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
			boolean findIntersection(PBounds cellBounds, double xOpposite, double yOpposite, double[] intersection)
			{
				int result = Geometry.findLineSegmentIntersection(cellBounds.getX(), cellBounds.getY(), cellBounds.getX() + cellBounds.getWidth(),
						cellBounds.getY(), cellBounds.getCenterX(), cellBounds.getCenterY(), xOpposite, yOpposite, intersection);
				return result == 1;
			}
		},
		RIGHT
		{
			@Override
			boolean findIntersection(PBounds cellBounds, double xOpposite, double yOpposite, double[] intersection)
			{
				int result = Geometry.findLineSegmentIntersection(cellBounds.getX(), cellBounds.getY() + cellBounds.getHeight(),
						cellBounds.getX() + cellBounds.getWidth(), cellBounds.getY() + cellBounds.getHeight(), cellBounds.getCenterX(),
						cellBounds.getCenterY(), xOpposite, yOpposite, intersection);
				return result == 1;
			}
		},
		BOTTOM
		{
			@Override
			boolean findIntersection(PBounds cellBounds, double xOpposite, double yOpposite, double[] intersection)
			{
				int result = Geometry.findLineSegmentIntersection(cellBounds.getX(), cellBounds.getY(), cellBounds.getX(),
						cellBounds.getY() + cellBounds.getHeight(), cellBounds.getCenterX(), cellBounds.getCenterY(), xOpposite, yOpposite, intersection);
				return result == 1;
			}
		},
		LEFT
		{
			@Override
			boolean findIntersection(PBounds cellBounds, double xOpposite, double yOpposite, double[] intersection)
			{
				int result = Geometry.findLineSegmentIntersection(cellBounds.getX() + cellBounds.getWidth(), cellBounds.getY(),
						cellBounds.getX() + cellBounds.getWidth(), cellBounds.getY() + cellBounds.getHeight(), cellBounds.getCenterX(),
						cellBounds.getCenterY(), xOpposite, yOpposite, intersection);
				return result == 1;
			}
		};

		abstract boolean findIntersection(PBounds cellBounds, double xOpposite, double yOpposite, double[] intersection);
	}

	public static IntentionGraphController getInstance()
	{
		return INSTANCE;
	}

	public static void initialize()
	{
		INSTANCE = new IntentionGraphController();

		GridBottomMenuBar.addMenuButtonRightAligned(ShowIntentionGraphButton.class);
	}

	private static IntentionGraphController INSTANCE;

	/**
	 * Map of all arrows in the Intention View, indexed by <code>CCanvasLink</code> id.
	 */
	private final Long2ReferenceArrayMap<CCanvasLinkArrow> arrowsByLinkId = new Long2ReferenceArrayMap<CCanvasLinkArrow>();

	/**
	 * Notify this controller that a link has been created, so that it can create and install the corresponding
	 * <code>CCanvsaLinkArrow</code>.
	 */
	public void addLink(CCanvasLink link)
	{
		CCanvasLinkArrow arrow = new CCanvasLinkArrow(link);

		CIntentionCell destination = CIntentionCellController.getInstance().getCellByCanvasId(link.getAnchorB().getCanvasId());
		if (destination != null)
		{
			Color color = IntentionCanvasController.getInstance().getIntentionTypeColor(destination.getIntentionTypeId());
			arrow.setColor(Color.black /*color*/);
		}

		arrowsByLinkId.put(link.getId(), arrow);
//		CalicoDraw.addChildToNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT), arrow);
		
		if (!IntentionGraph.getInstance().isClusterRoot(arrow.getAnchorA().getCanvasId()))
			IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).addChild(arrow);
//		CalicoDraw.moveNodeToBack(arrow);
		arrow.moveToBack();
		arrow.redraw();
	}

	public CCanvasLinkArrow getArrowByLinkId(long uuid)
	{
		return arrowsByLinkId.get(uuid);
	}
	
	public long[] getArrowLinkKeySet()
	{
		return arrowsByLinkId.keySet().toLongArray();
	}

	/**
	 * Move the pixel coordinates of <code>linkSourceCanvasId</code> such that it aligns with a link endpoint at
	 * <code>xLinkEndpoint, yLinkEndpoint</code>.
	 */
	public Point2D alignCellEdgeAtLinkEndpoint(long linkSourceCanvasId, double xLinkEndpoint, double yLinkEndpoint)
	{
		CIntentionCell sourceCell = CIntentionCellController.getInstance().getCellByCanvasId(linkSourceCanvasId);
		Point2D sourceCenter = sourceCell.getCenter();

		Dimension2D cellSize = sourceCell.getSize();
		Point2D.Double cellOrigin = new Point2D.Double();
		double xDelta = (xLinkEndpoint - sourceCenter.getX());
		if (Math.abs(xDelta) < 0.01)
		{
			if (yLinkEndpoint < sourceCenter.getY())
			{
				cellOrigin.setLocation(xLinkEndpoint - (cellSize.getWidth() / 2.0), yLinkEndpoint - sourceCell.getSize().getHeight());
			}
			else
			{
				cellOrigin.setLocation(xLinkEndpoint - (cellSize.getWidth() / 2.0), yLinkEndpoint);
			}
		}
		else
		{
			double cellDiagonalSlope = (cellSize.getHeight() / cellSize.getWidth());
			double linkSlope = (yLinkEndpoint - sourceCenter.getY()) / xDelta;

			if (Math.abs(linkSlope) < cellDiagonalSlope)
			{ // attaching to right or left edge
				double xSpanToCenter = (cellSize.getWidth() / 2.0);
				double ySpanToCenter = (xSpanToCenter * linkSlope);
				if (xLinkEndpoint > sourceCenter.getX())
				{ // left
					cellOrigin.setLocation(xLinkEndpoint, yLinkEndpoint + ySpanToCenter - (cellSize.getHeight() / 2.0));
				}
				else
				{ // right
					cellOrigin.setLocation(xLinkEndpoint - cellSize.getWidth(), yLinkEndpoint - (ySpanToCenter + (cellSize.getHeight() / 2.0)));
				}
			}
			else
			{ // top/bottom
				double ySpanToCenter = (cellSize.getHeight() / 2.0);
				double xSpanToCenter = (ySpanToCenter / linkSlope);
				if (yLinkEndpoint > sourceCenter.getY())
				{ // top
					cellOrigin.setLocation(xLinkEndpoint + xSpanToCenter - (cellSize.getWidth() / 2.0), yLinkEndpoint);
				}
				else
				{ // bottom
					cellOrigin.setLocation(xLinkEndpoint - (xSpanToCenter + (cellSize.getWidth() / 2.0)), yLinkEndpoint - cellSize.getHeight());
				}
			}
		}

		return cellOrigin;
	}

	private void updateAnchorPosition(CCanvasLinkAnchor anchor)
	{
		if (anchor.getArrowEndpointType() == ArrowEndpointType.INTENTION_CELL)
		{
			CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(anchor.getCanvasId());

			Point2D position = alignAnchorAtCellEdge(cell.getLocation().getX(), cell.getLocation().getY(), cell.getSize(), getOppositePosition(anchor));
			anchor.getPoint().setLocation(position);
		}
	}

	private Point2D getOppositePosition(CCanvasLinkAnchor anchor)
	{
		CCanvasLinkAnchor opposite = anchor.getOpposite();
		switch (opposite.getArrowEndpointType())
		{
			case INTENTION_CELL:
				return CIntentionCellController.getInstance().getCellByCanvasId(opposite.getCanvasId()).getCenter();
			case FLOATING:
				return anchor.getPoint();
			default:
				throw new IllegalArgumentException("Unknown anchor type " + anchor.getArrowEndpointType());
		}
	}

	/**
	 * Remove the visual components associated with <code>link</code>.
	 */
	public void removeLink(CCanvasLink link)
	{
		if (arrowsByLinkId == null || link == null)
			return;
		CCanvasLinkArrow arrow = arrowsByLinkId.remove(link.getId());
		CalicoDraw.removeChildFromNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT), arrow);
	}

	/**
	 * Notify this controller that the contents of <code>canvas_uuid</code> have changed, so that the thumbnail image
	 * can be updated.
	 */
	public void contentChanged(long canvas_uuid)
	{
		CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid);
		if (cell == null)
		{
			return;
		}

		cell.contentsChanged();
//		IntentionGraph.getInstance().repaint();
	}

	public void initializeDisplay()
	{
		IntentionGraph.getInstance().initialize();
	}

	/**
	 * Notify this controller that <code>link</code> or any of its display-related components has changed in some way
	 * that affects the display of the arrow, so that the display components can be updated accordingly.
	 */
	public void updateLinkArrow(final CCanvasLink link)
	{
		final CCanvasLinkArrow arrow = arrowsByLinkId.get(link.getId());
		if (arrow == null)
		{
			System.out.println("Warning: arrow is null in IntentionGraphController.updateLinkArrow");
			return;
		}
			
		CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(link.getAnchorB().getCanvasId());
		if ((cell != null) && cell.isNew())
		{
			CalicoDraw.setVisible(arrow, false);
//			arrow.setVisible(false);
			return;
		}

		if (!arrow.getVisible())
		{
			CalicoDraw.setVisible(arrow, true);
//			arrow.setVisible(true);
		}

		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					alignAnchors(link);
					arrow.redraw(false);
				}});

	}

	/**
	 * Notify this controller that CIC <code>cellId</code> has moved to pixel position <code>x, y</code> in the
	 * Intention View coordinate system, so that the attached arrow anchors can be moved accordingly. Anchor positions
	 * will be adjusted to keep them aligned to the edge of the CIC.
	 */
	public void localUpdateAttachedArrows(long cellId, double x, double y)
	{
		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(cellId);
		long canvasId = cell.getCanvasId();
		List<Long> anchorIds = CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(canvasId);
		for (long anchorId : anchorIds)
		{
			CCanvasLinkAnchor anchor = CCanvasLinkController.getInstance().getAnchor(anchorId);
			Point2D edgePosition = alignAnchorAtCellEdge(x, y, cell.getSize(), getOppositePosition(anchor));
			anchor.getPoint().setLocation(edgePosition);
			updateLinkArrow(anchor.getLink());
		}
	}

	/**
	 * Notify this controller that the CIC <code>cellId</code> has moved to pixel position <code>x, y</code> in the
	 * Intention View's coordinate space. The arrow anchor positions in this plugin's internal model will be updated
	 * accordingly, and the anchor position changes will be sent to the server (via <code>CCanvasLinkController</code>).
	 * Related visual components will also be adjusted, such as the position of the bubble menu.
	 */
	public void cellMoved(long cellId, double x, double y)
	{
		CIntentionCell cell = CIntentionCellController.getInstance().getCellById(cellId);
		long canvasId = cell.getCanvasId();
		List<Long> anchorIds = CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(canvasId);
		for (long anchorId : anchorIds)
		{
			CCanvasLinkAnchor anchor = CCanvasLinkController.getInstance().getAnchor(anchorId);
			Point2D edgePosition = alignAnchorAtCellEdge(x, y, cell.getSize(), getOppositePosition(anchor));

			CCanvasLinkController.getInstance().moveLinkAnchor(anchor, edgePosition);
		}

		if (BubbleMenu.isBubbleMenuActive() && (BubbleMenu.activeUUID == cellId))
		{
			BubbleMenu.moveIconPositions(cell.getBounds());
		}

		if (cell.isNew())
		{
			cell.setNew(false);
			localUpdateAttachedArrows(cell.getId(), cell.getLocation().getX(), cell.getLocation().getY());
		}
	}

	public Point2D getArrowAnchorPosition(long canvas_uuid, long opposite_canvas_uuid)
	{
		CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(opposite_canvas_uuid);
		return getArrowAnchorPosition(canvas_uuid, cell.getCenter());
	}

	public Point2D getArrowAnchorPosition(long canvas_uuid, Point2D opposite)
	{
		return getArrowAnchorPosition(canvas_uuid, opposite.getX(), opposite.getY());
	}

	public Point2D getArrowAnchorPosition(long canvas_uuid, double xOpposite, double yOpposite)
	{
		CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(canvas_uuid);
		return alignAnchorAtCellEdge(cell.copyBounds(), xOpposite, yOpposite);
	}

	/**
	 * Trig machinery to align arrow anchors with CIC edges.
	 */
	private void alignAnchors(CCanvasLink link)
	{
		CIntentionCell fromCell = CIntentionCellController.getInstance().getCellByCanvasId(link.getAnchorA().getCanvasId());
		CIntentionCell toCell = CIntentionCellController.getInstance().getCellByCanvasId(link.getAnchorB().getCanvasId());
		Point2D aPosition;
		Point2D bPosition;

		if (fromCell == null)
		{
			aPosition = link.getAnchorA().getPoint();
		}
		else
		{
			if (toCell == null)
			{
				aPosition = alignAnchorAtCellEdge(fromCell.copyBounds(), link.getAnchorB().getPoint());
			}
			else
			{
				aPosition = alignAnchorAtCellEdge(fromCell.copyBounds(), toCell.getCenter());
			}
		}

		if (toCell == null)
		{
			bPosition = link.getAnchorB().getPoint();
		}
		else
		{
			if (fromCell == null)
			{
				bPosition = alignAnchorAtCellEdge(toCell.copyBounds(), link.getAnchorA().getPoint());
			}
			else
			{
				bPosition = alignAnchorAtCellEdge(toCell.copyBounds(), fromCell.getCenter());
			}
		}

		link.getAnchorA().getPoint().setLocation(aPosition);
		link.getAnchorB().getPoint().setLocation(bPosition);
	}

	/**
	 * Trig machinery to align arrow anchors with CIC edges.
	 */
	private Point2D alignAnchorAtCellEdge(double xCell, double yCell, Dimension2D cellSize, Point2D opposite)
	{
		return alignAnchorAtCellEdge(new PBounds(xCell, yCell, cellSize.getWidth(), cellSize.getHeight()), opposite.getX(), opposite.getY());
	}

	/**
	 * Trig machinery to align arrow anchors with CIC edges.
	 */
	private Point2D alignAnchorAtCellEdge(PBounds cellBounds, Point2D opposite)
	{
		return alignAnchorAtCellEdge(cellBounds, opposite.getX(), opposite.getY());
	}

	/**
	 * Trig machinery to align arrow anchors with CIC edges.
	 */
	private Point2D alignAnchorAtCellEdge(PBounds cellBounds, double xOpposite, double yOpposite)
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
}
