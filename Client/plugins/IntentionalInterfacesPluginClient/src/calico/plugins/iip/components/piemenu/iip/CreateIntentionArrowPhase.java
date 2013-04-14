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
package calico.plugins.iip.components.piemenu.iip;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;

import calico.CalicoDraw;
import calico.components.arrow.AbstractArrow;
import calico.components.arrow.AbstractArrowAnchorPoint;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.CCanvasLinkAnchor;
import calico.plugins.iip.components.CCanvasLinkArrow;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.IntentionCanvasController;
import calico.plugins.iip.controllers.IntentionGraphController;

/**
 * Input mode controller for creating and moving arrows. Most of the features are obsolete. At present the only
 * supported operation is to create a link between existing canvases.
 * 
 * This operation is initiated from the <code>CreateLinkButton</code>, and it terminates on input release. If the input
 * release occurs on a canvas other than the one selected, a link is created from the selected canvas to the input
 * release target; otherwise nothing happens.
 * 
 * The sequence of events for creating or moving an arrow is referred to here as an "arrow phase". The phase begins when
 * the user presses down on a button that invokes <code>startCreate()</code>. The next input release always terminates
 * the phase.
 * 
 * @author Byron Hawkins
 */
public class CreateIntentionArrowPhase implements MouseListener, MouseMotionListener
{
	public static CreateIntentionArrowPhase getInstance()
	{
		return INSTANCE;
	}

	static final CreateIntentionArrowPhase INSTANCE = new CreateIntentionArrowPhase();

	/**
	 * Consumers of the <code>CreateIntentionArrowPhase</code> use these modes to specify which end of a link to move.
	 * This feature is bsolete.
	 * 
	 * @author Byron Hawkins
	 */
	public enum MoveLinkEndpointMode
	{
		MOVE_ANCHOR_A,
		MOVE_ANCHOR_B;
	}

	/**
	 * Consumers of the <code>CreateIntentionArrowPhase</code> use these modes to request a particular kind of new link.
	 * Only <code>LINK_EXISTING</code> is presently supported.
	 * 
	 * @author Byron Hawkins
	 */
	public enum NewLinkMode
	{
		LINK_TO_COPY,
		LINK_TO_BLANK,
		LINK_EXISTING;
	}

	/**
	 * 
	 * 
	 * @author Byron Hawkins
	 */
	private enum Mode
	{
		MOVE_ANCHOR_A,
		MOVE_ANCHOR_B,
		LINK_TO_COPY,
		LINK_TO_BLANK,
		LINK_EXISTING;
	}

	/**
	 * State flag to track the current input activity
	 */
	private boolean dragInitiated;
	private Mode mode;
	/**
	 * The link to move, or null if creating a new link.
	 */
	private CCanvasLink link;
	/**
	 * The cell at which to attach the A end of the arrow, or null if it should be orphaned at the A end.
	 */
	private CIntentionCell fromCell;
	/**
	 * The cell at which to attach the B end of the arrow (the head), or null if it should be orphaned at the B end.
	 */
	private CIntentionCell toCell;
	/**
	 * The pixel position in Intention View coordinates (the same as CIntentionCell coordinates) of the point which the
	 * user clicked at the beginning of the operation.
	 */
	private Point2D anchorPoint;
	/**
	 * The pixel position in screen coordinates of the point which the user clicked at the beginning of the operation.
	 */
	private Point2D dragStartPoint;
	/**
	 * True if the consumer requested a copy operation, false if a link to a new canvas has been requested.
	 */
	private boolean copy;

	/**
	 * The arrow drawn while the operation is in progress. When the operation completes, this arrow is hidden and a
	 * model-backed arrow replaces it (unless the operation is aborted).
	 */
	private final TransitoryArrow arrow = new TransitoryArrow();

	/**
	 * State flag indicating that the mouse pointer is currently on the selected cell, which is either the
	 * <code>fromCell</code> or the <code>toCell</code>.
	 */
	private boolean onSelf;

	public CreateIntentionArrowPhase()
	{
		CalicoDraw.addChildToNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT), arrow);
//		IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).addChild(arrow);
		CalicoDraw.setVisible(arrow, false);
//		arrow.setVisible(false);
	}

	public void startMove(CCanvasLink link, MoveLinkEndpointMode moveMode, Point dragStartPoint)
	{
		throw new UnsupportedOperationException("Moving arrows is not presently supported.");
	}

	/**
	 * Entry point for creating new links. Buttons call this method.
	 */
	void startCreate(CIntentionCell fromCell, Point dragStartPoint, NewLinkMode mode)
	{
		this.mode = (mode == NewLinkMode.LINK_EXISTING) ? Mode.LINK_EXISTING : (mode == NewLinkMode.LINK_TO_BLANK) ? Mode.LINK_TO_BLANK : Mode.LINK_TO_COPY;
		this.link = null;
		this.fromCell = fromCell;
		this.toCell = null;
		this.anchorPoint = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT)
				.globalToLocal(new Point2D.Double(dragStartPoint.getX(), dragStartPoint.getY()));
		this.dragStartPoint = dragStartPoint;
		this.onSelf = false;
		this.dragInitiated = false;

		System.out.println("Start creating arrow from cell #" + fromCell.getCanvasId() + " at " + fromCell.getLocation() + " with anchor point " + anchorPoint);

		startPhase();
	}

	/**
	 * Begin the "arrow phase".
	 */
	private void startPhase()
	{
		IntentionGraph.getInstance().addMouseListener(this);
		IntentionGraph.getInstance().addMouseMotionListener(this);

		if (fromCell != null)
		{
			fromCell.setHighlighted(true);
		}
		if (toCell != null)
		{
			toCell.setHighlighted(true);
		}
	}

	/**
	 * Start the drag phase of the arrow phase.
	 */
	private void startDrag()
	{
		dragInitiated = true;

		moveTransitoryArrow(anchorPoint, true);

		arrow.setVisible(true);
	}

	/**
	 * Move the arrow which is temporarily drawn to represent the arrow being created or moved. This method may move
	 * either endpoint, even though the user is only dragging one of them. It wll be necessary to move the "fixed" end
	 * of the arrow at the beginning of the drag phase, for example, since the transitory arrow is a singleton and it
	 * will have been in some other position at the end of the last "arrow phase".
	 * 
	 * @param point
	 *            the new destination of the movable endpoint
	 * @param fixedSide
	 *            when true, the fixed endpoint of the arrow will be moved to <code>point</code>; otherwise (in the
	 *            typical usage) the endpoint being dragged will be moved to <code>point</code>.
	 */
	private void moveTransitoryArrow(Point2D point, boolean fixedSide)
	{
		if (fixedSide)
		{
			switch (mode)
			{
				case LINK_EXISTING:
				case LINK_TO_BLANK:
				case LINK_TO_COPY:
				case MOVE_ANCHOR_B:
					arrow.a.getPoint().setLocation(point);
					break;
				case MOVE_ANCHOR_A:
					arrow.b.getPoint().setLocation(point);
					break;
				default:
					throw new IllegalArgumentException("Unknown mode " + mode);
			}
		}
		else
		{
			switch (mode)
			{
				case LINK_EXISTING:
				case LINK_TO_BLANK:
				case LINK_TO_COPY:
				case MOVE_ANCHOR_B:
					arrow.b.getPoint().setLocation(point);
					break;
				case MOVE_ANCHOR_A:
					arrow.a.getPoint().setLocation(point);
					break;
				default:
					throw new IllegalArgumentException("Unknown mode " + mode);
			}
		}

		arrow.redraw(true);
	}

	/**
	 * End the "arrow phase", creating or moving the "real" arrow if the operation has completed in an applicable way.
	 * 
	 * @param terminationPoint
	 *            last known mouse point prior to phase termination
	 */
	private void terminatePhase(Point terminationPoint)
	{
		IntentionGraph.getInstance().removeMouseListener(this);
		IntentionGraph.getInstance().removeMouseMotionListener(this);

		if (toCell != null)
		{
			toCell.setHighlighted(false);
		}

		if (dragInitiated)
		{
			arrow.setVisible(false);

			if (onSelf)
			{
				System.out.println("Cancelling arrow creation because the arrow is pointing to the source cell");
				return;
			}
		}

		Point2D graphPosition = getGraphPosition(terminationPoint);
		switch (mode)
		{
			case LINK_EXISTING:
			case LINK_TO_BLANK:
			case LINK_TO_COPY:
				createLink(graphPosition);
				break;
			case MOVE_ANCHOR_A:
			case MOVE_ANCHOR_B:
				moveLink(graphPosition);
				break;
		}
	}

	/**
	 * Apply new endpoint coordinates <code>graphPosition</code> to the movable endpoint of the link being moved. This
	 * method is not used when an arrow is being created.
	 */
	private void moveLink(Point2D graphPosition)
	{
		CCanvasLinkAnchor anchor = (mode == Mode.MOVE_ANCHOR_A) ? link.getAnchorA() : link.getAnchorB();
		if (getTransitoryCell() == null)
		{
			CCanvasLinkController.getInstance().orphanLink(anchor, graphPosition.getX(), graphPosition.getY());
		}
		else
		{
			long canvasId = (mode == Mode.MOVE_ANCHOR_A) ? fromCell.getCanvasId() : toCell.getCanvasId();
			CCanvasLinkController.getInstance().moveLink(anchor, canvasId);
		}

		CCanvasLinkArrow arrow = IntentionGraphController.getInstance().getArrowByLinkId(link.getId());
//		CalicoDraw.setVisible(arrow, true);
		arrow.setVisible(true);
		arrow.setHighlighted(false);
	}

	/**
	 * Create a new link with its draggable end at coordinates <code>graphPosition</code>.
	 */
	private void createLink(Point2D graphPosition)
	{
		if (dragInitiated)
		{
			if (toCell == null)
			{
				if (mode != Mode.LINK_EXISTING)
				{
					CCanvasLinkController.getInstance().createLinkToEmptyCanvas(fromCell.getCanvasId(), graphPosition.getX(), graphPosition.getY(),
							(mode == Mode.LINK_TO_COPY));
				}
			}
			else
			{
				CCanvasLinkController.getInstance().createLink(fromCell.getCanvasId(), toCell.getCanvasId());
			}
		}
		else
		{
			long newCanvasId = CCanvasLinkController.getInstance().createLinkToEmptyCanvas(fromCell.getCanvasId());
			if (copy)
			{
				IntentionCanvasController.getInstance().copyCanvas(fromCell.getCanvasId(), newCanvasId);
			}
		}

		System.out.println("Create arrow from cell #" + fromCell.getCanvasId() + " at " + fromCell.getLocation() + ", with anchor point " + anchorPoint
				+ ", to " + ((toCell == null) ? "the canvas" : "cell #" + toCell.getCanvasId()) + " at " + graphPosition);
	}

	/**
	 * Translate a screen coordinate pair into an Intention View coordinate pair (sharing the same coordinate basis as
	 * the <code>CIntentionCell</code>s).
	 */
	private Point2D getGraphPosition(Point2D point)
	{
		return IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).globalToLocal(new Point2D.Double(point.getX(), point.getY()));
	}

	/**
	 * Get the CIC at which the arrow is anchored, if any. For arrows being moved, the anchor is the CIC attachment
	 * which is not being changed. For arrows being created, the anchor is the "from" end of the arrow, which is not
	 * being dragged around.
	 */
	private CIntentionCell getAnchorCell()
	{
		switch (mode)
		{
			case MOVE_ANCHOR_A:
				return toCell;
			case LINK_EXISTING:
			case LINK_TO_BLANK:
			case LINK_TO_COPY:
			case MOVE_ANCHOR_B:
				return fromCell;
			default:
				throw new IllegalArgumentException("Unknown mode " + mode);
		}
	}

	/**
	 * Get the CIC to which the arrow has been attached by dragging, if any.
	 */
	private CIntentionCell getTransitoryCell()
	{
		switch (mode)
		{
			case MOVE_ANCHOR_A:
				return fromCell;
			case LINK_EXISTING:
			case LINK_TO_BLANK:
			case LINK_TO_COPY:
			case MOVE_ANCHOR_B:
				return toCell;
			default:
				throw new IllegalArgumentException("Unknown mode " + mode);
		}
	}

	/**
	 * Assign either <code>fromCell</code> or <code>toCell</code> to be <code>cell</code>. If the tail of an arrow is
	 * being dragged, then <code>fromCell = cell</code>, otherwise if the arrowhead is being dragged, then
	 * <code>toCell = cell</code>.
	 */
	private void setTransitoryCell(CIntentionCell cell)
	{
		switch (mode)
		{
			case MOVE_ANCHOR_A:
				fromCell = cell;
				break;
			case LINK_EXISTING:
			case MOVE_ANCHOR_B:
				toCell = cell;
				break;
			case LINK_TO_BLANK:
			case LINK_TO_COPY:
				if (cell != null)
				{
					throw new IllegalArgumentException("Can't set the transitory cell for mode " + mode);
				}
				else
				{
					break;
				}
			default:
				throw new IllegalArgumentException("Unknown mode " + mode);
		}
	}

	/**
	 * Discern whether <code>cell</code> is an allowable endpoint CIC for the arrow being moved or created.
	 */
	private boolean canLinkTo(CIntentionCell cell)
	{
		if ((cell == null) || onSelf || (mode == Mode.LINK_TO_BLANK) || (mode == Mode.LINK_TO_COPY))
		{
			return false;
		}

		CIntentionCell anchorA = (mode == Mode.MOVE_ANCHOR_A) ? cell : getAnchorCell();
		CIntentionCell target = (mode == Mode.MOVE_ANCHOR_A) ? getAnchorCell() : cell;
		if (isAlreadyLinked(target, anchorA.getCanvasId()) || isParent(target, anchorA.getCanvasId()))
		{
			return false;
		}

		return true;
	}

	/**
	 * Discern whether a link already exists from <code>canvasIdOfAnchorA</code> to <code>target</code>.
	 */
	private boolean isAlreadyLinked(CIntentionCell target, long canvasIdOfAnchorA)
	{
		for (Long anchorId : CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(canvasIdOfAnchorA))
		{
			CCanvasLinkAnchor anchor = CCanvasLinkController.getInstance().getAnchor(anchorId);
			if (anchor.getLink().getAnchorA() == anchor)
			{
				if (anchor.getLink().getAnchorB().getCanvasId() == target.getCanvasId())
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Discern whether an arrow exists from <code>target</code> to <code>canvasIdOfAnchorA</code>. This is used to avoid
	 * creating cycles in the graph of arrows.
	 */
	private boolean isParent(CIntentionCell target, long canvasIdOfAnchorA)
	{
		CCanvasLinkAnchor incomingAnchor = null;
		for (Long anchorId : CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(canvasIdOfAnchorA))
		{
			CCanvasLinkAnchor anchor = CCanvasLinkController.getInstance().getAnchor(anchorId);
			if (anchor.getLink().getAnchorB() == anchor)
			{
				incomingAnchor = anchor;
				break;
			}
		}

		if (incomingAnchor == null)
		{
			return false;
		}

		if (CCanvasLinkController.getInstance().canvasBisDescendentOfCanvasA(incomingAnchor.getCanvasId(), target.getCanvasId()))
//				incomingAnchor.getOpposite().getCanvasId() == target.getCanvasId())
		{
			System.out.println("Cycle detected on canvas id " + target.getCanvasId());
			return true;
		}

		return isParent(target, incomingAnchor.getOpposite().getCanvasId());
	}

	@Override
	public void mouseDragged(MouseEvent event)
	{
		if (!dragInitiated)
		{
			startDrag();
		}

		Point2D graphPosition = getGraphPosition(event.getPoint());

		moveTransitoryArrow(graphPosition, false);

		long newCellId = CIntentionCellController.getInstance().getCellAt(event.getPoint());
		CIntentionCell newCell = CIntentionCellController.getInstance().getCellById(newCellId);

		// kind of risky to require `onSelf to be set before calling canLinkTo()
		onSelf = ((getAnchorCell() != null) && (newCellId == getAnchorCell().getId()));

		if (!canLinkTo(newCell))
		{
			newCell = null;
		}
		if ((getTransitoryCell() != null) && (newCell != getTransitoryCell()))
		{
			getTransitoryCell().setHighlighted(false);
		}
		setTransitoryCell(newCell);
		if (getTransitoryCell() != null)
		{
			getTransitoryCell().setHighlighted(true);
		}

		switch (mode)
		{
			case MOVE_ANCHOR_A:
			case MOVE_ANCHOR_B:
			case LINK_EXISTING:
				if (getTransitoryCell() == null)
				{
					arrow.setColor(CCanvasLinkArrow.FLOATING_COLOR);
					break;
				}
			case LINK_TO_BLANK:
			case LINK_TO_COPY:
				arrow.setColor(CCanvasLinkArrow.NORMAL_COLOR);
		}
	}

	@Override
	public void mouseMoved(MouseEvent event)
	{
	}

	@Override
	public void mouseClicked(MouseEvent event)
	{
	}

	@Override
	public void mouseEntered(MouseEvent event)
	{
	}

	@Override
	public void mouseExited(MouseEvent event)
	{
		terminatePhase(event.getPoint());
	}

	@Override
	public void mousePressed(MouseEvent event)
	{
	}

	@Override
	public void mouseReleased(MouseEvent event)
	{
		terminatePhase(event.getPoint());
	}

	/**
	 * The arrow which is drawn during the "arrow phase". There is only one instance, and it is hidden after the phase terminates. 
	 *
	 * @author Byron Hawkins
	 */
	private class TransitoryArrow extends AbstractArrow<TransitoryAnchor>
	{
		final TransitoryAnchor a;
		final TransitoryAnchor b;

		public TransitoryArrow()
		{
			super(Color.black, TYPE_NORM_HEAD_B);

			setAnchorA(a = new TransitoryAnchor());
			setAnchorB(b = new TransitoryAnchor());
		}
	}

	private class TransitoryAnchor extends AbstractArrowAnchorPoint
	{
	}
}
