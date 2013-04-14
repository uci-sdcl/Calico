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
package calico.plugins.iip.components.canvas;

import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

import calico.Calico;
import calico.CalicoDraw;
import calico.components.CCanvas;
import calico.components.grid.CGrid;
import calico.controllers.CCanvasController;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.inputhandlers.StickyItem;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.CCanvasLinkAnchor;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.CIntentionType;
import calico.plugins.iip.components.IntentionPanelLayout;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.IntentionCanvasController;
import calico.plugins.iip.iconsets.CalicoIconManager;
import calico.plugins.iip.util.IntentionalInterfacesGraphics;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.nodes.PComposite;

/**
 * A panel in the Canvas View which shows all the intention links that are attached to the current canvas. This feature
 * is obsolete.
 * 
 * @author Byron Hawkins
 */
public class CanvasLinkPanel implements StickyItem
{
	private static CanvasLinkPanel getInstance()
	{
		return INSTANCE;
	}

	private static CanvasLinkPanel INSTANCE = new CanvasLinkPanel();

	private static final LinkSorter LINK_SORTER = new LinkSorter();

	public static final double PREVIEW_X_SPACER = 10.0;

	public static final double PANEL_COMPONENT_INSET = 5.0;
	public static final double TABLE_UNIT_SPAN = 30.0;
	public static final double ROW_TEXT_INSET = 1.0;

	private static final Color PREVIEW_ROW_BACKGROUND = new Color(0xEAEAEA);

	private final PanelNode panel = new PanelNode();
	private final CanvasThumbnail thumbnail = new CanvasThumbnail();

	private final long uuid;
	private long canvas_uuid;
	// remove this
	private final CCanvasLink.LinkDirection direction = CCanvasLink.LinkDirection.INCOMING;

	private boolean visible;
	private IntentionPanelLayout layout;

	private final Image checkmarkImage;
	private final Image incomingLinkFrameImage;
	private final Image outgoingLinkFrameImage;
	private final Dimension tableCheckmarkInset = new Dimension();

	private boolean initialized = false;

	private CanvasLinkPanel()
	{
		uuid = Calico.uuid();
		canvas_uuid = 0L;

		CalicoInputManager.addCustomInputHandler(uuid, new InputHandler());

		checkmarkImage = CalicoIconManager.getIconImage("intention.checkmark");
		incomingLinkFrameImage = CalicoIconManager.getIconImage("intention.incoming-link-frame");
		outgoingLinkFrameImage = CalicoIconManager.getIconImage("intention.outgoing-link-frame");
		tableCheckmarkInset.width = (int) ((TABLE_UNIT_SPAN - checkmarkImage.getWidth(null)) / 2.0);
		tableCheckmarkInset.height = (int) ((TABLE_UNIT_SPAN - checkmarkImage.getHeight(null)) / 2.0);

		panel.setVisible(visible = false);
		panel.initialize();

		initialized = true;
	}

	@Override
	public long getUUID()
	{
		return uuid;
	}

	@Override
	public boolean containsPoint(Point p)
	{
		return panel.getBounds().contains(p);
	}

	public boolean isVisible()
	{
		return panel.getVisible();
	}

	public void setVisible(boolean b)
	{
		if (visible == b)
		{
			return;
		}

		visible = b;

		if (b)
		{
			refresh();
		}
		else
		{
			panel.setVisible(false);
		}

		if (b)
		{
			CalicoInputManager.registerStickyItem(this);
			CalicoDraw.repaint(panel);
//			panel.repaint();
		}
		else
		{
			CalicoInputManager.unregisterStickyItem(this);
		}
	}

	public void moveTo(long canvas_uuid)
	{
		this.canvas_uuid = canvas_uuid;

		if (panel.getParent() != null)
		{
			panel.getParent().removeChild(panel);
		}
		if (thumbnail.getParent() != null)
		{
			thumbnail.getParent().removeChild(thumbnail);
		}
		updateLinks();
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(canvas_uuid).getCamera(), panel);
//		CCanvasController.canvasdb.get(canvas_uuid).getCamera().addChild(panel);
		CalicoDraw.addChildToNode(CCanvasController.canvasdb.get(canvas_uuid).getCamera(), thumbnail);
//		CCanvasController.canvasdb.get(canvas_uuid).getCamera().addChild(thumbnail);
	}

	public void refresh()
	{
		if (!visible)
		{
			return;
		}

		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
				{
					refresh();
				}
			});
			return;
		}

		panel.refreshIntentionTypeSelections();
		panel.setVisible(true);
		updatePanelBounds();
	}

	public void updateLinks()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
				{
					updateLinks();
				}
			});
			return;
		}

		panel.updateLinks();
		updatePanelBounds();
	}

	public void updateIntentionTypes()
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			SwingUtilities.invokeLater(new Runnable() {
				public void run()
				{
					updateIntentionTypes();
				}
			});
			return;
		}

		panel.updateIntentionTypes();
		updatePanelBounds();
	}

	public void updatePanelBounds()
	{
		double width = panel.calculateWidth();
		double height = panel.calculateHeight();
		layout.updateBounds(panel, width, height);

		if (visible)
		{
			CalicoDraw.repaint(panel);
//			panel.repaint();
		}
	}

	public void setLayout(IntentionPanelLayout layout)
	{
		this.layout = layout;
	}

	private class IntentionTypeRow
	{
		private final CIntentionType type;
		private final PText label;

		private double y;

		IntentionTypeRow(CIntentionType type)
		{
			this.type = type;

			label = new PText(type.getName());
			label.setConstrainWidthToTextWidth(true);
			label.setConstrainHeightToTextHeight(true);
			label.setFont(label.getFont().deriveFont(20f));
		}

		void setPosition(double x, double y)
		{
			this.y = y;
			label.setX(x + PANEL_COMPONENT_INSET);
			label.setY(y + ROW_TEXT_INSET);
		}

		void installComponents()
		{
			CalicoDraw.addChildToNode(panel, label);
//			panel.addChild(label);
		}

		void removeAllComponents()
		{
			CalicoDraw.removeChildFromNode(panel, label);
//			panel.removeChild(label);
		}
	}

	private class LinkColumn
	{
		private final CCanvasLinkAnchor linkOppositeAnchor;
		private final PImage headerCell;

		private final Long2ReferenceArrayMap<PImage> checkmarksByIntentionTypeId = new Long2ReferenceArrayMap<PImage>();

		private int x;

		LinkColumn(CCanvasLinkAnchor linkOppositeAnchor)
		{
			this.linkOppositeAnchor = linkOppositeAnchor;

			Image linkFrameImage;
			if (linkOppositeAnchor.getCanvasId() == linkOppositeAnchor.getLink().getAnchorA().getCanvasId())
			{
				linkFrameImage = incomingLinkFrameImage;
			}
			else
			{
				linkFrameImage = outgoingLinkFrameImage;
			}
			headerCell = new PImage(IntentionalInterfacesGraphics.superimposeCellAddress(linkFrameImage, linkOppositeAnchor.getCanvasId()));
			headerCell.setBounds(0.0, 0.0, TABLE_UNIT_SPAN, TABLE_UNIT_SPAN);
		}

		void setPosition(double x, double y)
		{
			this.x = (int) x;

			headerCell.setX(x);
			headerCell.setY(y);

			for (IntentionTypeRow row : panel.typeRows)
			{
				PImage checkmark = checkmarksByIntentionTypeId.get(row.type.getId());
				if (checkmark != null)
				{
					checkmark.setX(x); // + tableCheckmarkInset.width);
					checkmark.setY(row.y); // + tableCheckmarkInset.height);
				}
			}
		}

		void installComponents()
		{
			CalicoDraw.addChildToNode(panel, headerCell);
//			panel.addChild(headerCell);
			updateCheckmarks();
		}

		void removeAllComponents()
		{
			CalicoDraw.addChildToNode(panel, headerCell);
//			panel.removeChild(headerCell);
			removeCheckmarks();
		}

		void removeCheckmarks()
		{
			for (PImage checkmark : checkmarksByIntentionTypeId.values())
			{
				panel.removeChild(checkmark);
			}
		}

		void updateIntentionTypes()
		{
			removeCheckmarks();
			checkmarksByIntentionTypeId.clear();
			updateCheckmarks();
		}

		void updateCheckmarks()
		{
			CIntentionCell cell = CIntentionCellController.getInstance().getCellByCanvasId(linkOppositeAnchor.getCanvasId());
			for (CIntentionType type : IntentionCanvasController.getInstance().getActiveIntentionTypes())
			{
				if (cell.getIntentionTypeId() == type.getId())
				{
					BufferedImage paintedCheckmark = new BufferedImage((int) TABLE_UNIT_SPAN, (int) TABLE_UNIT_SPAN, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = (Graphics2D) paintedCheckmark.getGraphics();
					g.setColor(type.getColor());
					g.fillRect(0, 0, (int) TABLE_UNIT_SPAN, (int) TABLE_UNIT_SPAN);
					g.drawImage(checkmarkImage, tableCheckmarkInset.width, tableCheckmarkInset.height, null);

					// kind of a hack, would not be necessary if my checkmark icon had a transparent background
					for (int i = 0; i < paintedCheckmark.getHeight(); i++)
					{
						for (int j = 0; j < paintedCheckmark.getWidth(); j++)
						{
							if (paintedCheckmark.getRGB(j, i) == Color.white.getRGB())
							{
								paintedCheckmark.setRGB(j, i, type.getColor().getRGB());
							}
						}
					}

					PImage checkmark = new PImage(paintedCheckmark);
					checkmarksByIntentionTypeId.put(type.getId(), checkmark);
					CalicoDraw.addChildToNode(panel, checkmark);
//					panel.addChild(checkmark);
				}
			}
		}
	}

	private class PanelNode extends PComposite
	{
		private final Color CLICK_HIGHLIGHT = new Color(0xFFFF30);
		private final Color CONTEXT_HIGHLIGHT = Color.red;

		private PPath clickHighlight = createHighlight(CLICK_HIGHLIGHT);
		private PPath contextHighlight = createHighlight(CONTEXT_HIGHLIGHT);

		private List<IntentionTypeRow> typeRows = new ArrayList<IntentionTypeRow>();
		private List<LinkColumn> linkColumns = new ArrayList<LinkColumn>();

		private int xColumnStart;

		private PPath border;

		public PanelNode()
		{
			CalicoDraw.addChildToNode(this, contextHighlight);
			CalicoDraw.addChildToNode(this, clickHighlight);
//			addChild(contextHighlight);
//			addChild(clickHighlight);
		}

		void initialize()
		{
			panel.setPaint(Color.white);
		}

		private PPath createHighlight(Color c)
		{
			PPath highlight = new PPath(new Rectangle2D.Double(0, 0, TABLE_UNIT_SPAN, TABLE_UNIT_SPAN));
			highlight.setStrokePaint(c);
			highlight.setStroke(new BasicStroke(1f));
			highlight.setVisible(false);
			return highlight;
		}

		private double getMaxIntentionHeaderWidth()
		{
			double maxWidth = 0.0;
			for (IntentionTypeRow row : typeRows)
			{
				if (row.label.getBounds().width > maxWidth)
				{
					maxWidth = row.label.getBounds().width;
				}
			}
			return maxWidth;
		}

		private double calculateWidth()
		{
			return getMaxIntentionHeaderWidth() + (TABLE_UNIT_SPAN * linkColumns.size()) + (2 * PANEL_COMPONENT_INSET);
		}

		private double calculateHeight()
		{
			return (2 + typeRows.size()) * TABLE_UNIT_SPAN;
		}

		void refreshIntentionTypeSelections()
		{
			if (canvas_uuid == 0L)
			{
				return;
			}

			for (LinkColumn column : linkColumns)
			{
				column.updateIntentionTypes();
			}
		}

		void updateIntentionTypes()
		{
			for (IntentionTypeRow row : typeRows)
			{
				row.removeAllComponents();
			}
			typeRows.clear();

			for (CIntentionType type : IntentionCanvasController.getInstance().getActiveIntentionTypes())
			{
				IntentionTypeRow row = new IntentionTypeRow(type);
				row.installComponents();
				typeRows.add(row);
			}

			refreshIntentionTypeSelections();

			if (border != null)
			{
				removeChild(border);
			}
			border = new PPath(new Rectangle2D.Double(0, 0, calculateWidth(), calculateHeight()));
			border.setStrokePaint(Color.black);
			border.setStroke(new BasicStroke(1f));
			CalicoDraw.addChildToNode(this, border);
//			addChild(border);
		}

		void updateLinks()
		{
			for (LinkColumn column : linkColumns)
			{
				column.removeAllComponents();
			}
			linkColumns.clear();

			List<CCanvasLinkAnchor> sortedAnchors = new ArrayList<CCanvasLinkAnchor>();
			for (long anchorId : CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(canvas_uuid))
			{
				sortedAnchors.add(CCanvasLinkController.getInstance().getAnchor(anchorId));
			}

			Collections.sort(sortedAnchors, LINK_SORTER);

			for (CCanvasLinkAnchor anchor : sortedAnchors)
			{
				LinkColumn column = new LinkColumn(anchor.getOpposite());
				linkColumns.add(column);
				column.installComponents();
			}
		}

		LinkColumn getClickedColumn(InputEventInfo event)
		{
			if (event.getX() < xColumnStart)
			{
				return null;
			}

			for (LinkColumn column : linkColumns)
			{
				if ((column.x + TABLE_UNIT_SPAN) > event.getX())
				{
					panel.highlightClickedColumn(column);
					return column;
				}
			}

			return null;
		}

		private void highlightClickedColumn(LinkColumn column)
		{
			showHighlight(clickHighlight, column);
			CalicoDraw.repaint(this);
//			repaint();
		}

		private void showHighlight(PPath highlight, LinkColumn column)
		{
			PBounds bounds = getBounds();
			highlight.setBounds(column.x, bounds.getY(), TABLE_UNIT_SPAN, TABLE_UNIT_SPAN);
			highlight.moveToFront();
			highlight.setVisible(true);
		}

		@Override
		protected void layoutChildren()
		{
			if (!initialized)
			{
				return;
			}

			PBounds bounds = panel.getBoundsReference();

			double yRow = bounds.y;
			for (IntentionTypeRow row : typeRows)
			{
				row.setPosition(bounds.x, yRow += TABLE_UNIT_SPAN);
			}

			double xColumn = bounds.x + getMaxIntentionHeaderWidth() + PANEL_COMPONENT_INSET;
			this.xColumnStart = (int) xColumn;
			for (LinkColumn column : linkColumns)
			{
				column.setPosition(xColumn, bounds.y);
				xColumn += TABLE_UNIT_SPAN;
			}

			if (CCanvasLinkController.getInstance().hasTraversedLink())
			{
				long traversedCanvasId = CCanvasLinkController.getInstance().getTraversedLinkSourceCanvas();
				for (LinkColumn column : linkColumns)
				{
					if (column.linkOppositeAnchor.getCanvasId() == traversedCanvasId)
					{
						showHighlight(contextHighlight, column);
						break;
					}
				}
			}
			else
			{
				contextHighlight.setVisible(false);
			}

			border.setBounds(bounds);
		}
	}

	private static final int BORDER_WIDTH = 1;

	private class CanvasThumbnail extends PComposite
	{
		private final PImage snapshot = new PImage();
		private long currentCanvasId;

		public CanvasThumbnail()
		{
			// GridRemoval: double gridCellWidth = CGrid.getInstance().getImgw() - (CGridCell.ROUNDED_RECTANGLE_OVERFLOW
			// + CGridCell.CELL_MARGIN);
			// GridRemoval: double gridCellHeight = CGrid.getInstance().getImgh() -
			// (CGridCell.ROUNDED_RECTANGLE_OVERFLOW + CGridCell.CELL_MARGIN);
			// GridRemoval: setBounds(0.0, 0.0, gridCellWidth * 3.0, gridCellHeight * 3.0);
			setPaint(Color.white);

			snapshot.setBounds(getBounds());
			CalicoDraw.addChildToNode(this, snapshot);
//			addChild(snapshot);

			hide();
		}

		void displayThumbnail(long canvasId)
		{
			currentCanvasId = canvasId;

			PBounds bounds = panel.getBounds();
			double xPanelRight = bounds.x + bounds.width;
			setX(xPanelRight + PREVIEW_X_SPACER);
			double yPanelBottom = bounds.y + bounds.height;
			setY(yPanelBottom - getBounds().height);

			CCanvas canvas = CCanvasController.canvasdb.get(canvasId);
			snapshot.setImage(canvas.getContentCamera().toImage());
			snapshot.setBounds(getBounds());

			CalicoDraw.moveNodeToFront(this);
//			moveToFront();
			CalicoDraw.setVisible(this, true);
//			setVisible(true);
			CalicoDraw.repaint(this);
//			repaint();
		}

		void hide()
		{
			CalicoDraw.setVisible(this, false);
//			setVisible(false);
		}

		@Override
		protected void paint(PPaintContext paintContext)
		{
			super.paint(paintContext);

			Graphics2D g = paintContext.getGraphics();
			Color c = g.getColor();
			PBounds bounds = getBounds();

			g.setColor(Color.black);
			g.translate(bounds.x, bounds.y);
			g.drawRoundRect(0, 0, ((int) bounds.width) - 1, ((int) bounds.height) - 1, 10, 10);
			IntentionalInterfacesGraphics.superimposeCellAddressInCorner(g, currentCanvasId, bounds.width - (2 * BORDER_WIDTH),
					CIntentionCell.COORDINATES_FONT, CIntentionCell.COORDINATES_COLOR);

			g.translate(-bounds.x, -bounds.y);
			g.setColor(c);
		}
	}

	private enum InputState
	{
		IDLE,
		PRESSED,
		THUMBNAIL
	}

	private class InputHandler extends CalicoAbstractInputHandler
	{
		private final Object stateLock = new Object();

		private InputState state = InputState.IDLE;
		private LinkColumn clickedColumn = null;
		private Point pressAnchor;

		private final long tapDuration = 500L;
		private final double dragThreshold = 10.0;

		private final PressAndHoldTimer pressAndHold = new PressAndHoldTimer();

		@Override
		public void actionReleased(InputEventInfo event)
		{
			synchronized (stateLock)
			{
				if (state == InputState.THUMBNAIL)
				{
					thumbnail.hide();
				}
				else if ((state == InputState.PRESSED) && (clickedColumn != null))
				{
					CCanvasLinkController.getInstance().traverseLinkToCanvas(clickedColumn.linkOppositeAnchor.getOpposite());
				}
				state = InputState.IDLE;
			}

			panel.clickHighlight.setVisible(false);
			clickedColumn = null;

			CalicoInputManager.unlockHandlerIfMatch(uuid);
		}

		@Override
		public void actionDragged(InputEventInfo event)
		{
			if (pressAnchor.distance(event.getGlobalPoint()) < dragThreshold)
			{
				// not a drag, completely ignore this event
				return;
			}

			synchronized (stateLock)
			{
				if (state == InputState.THUMBNAIL)
				{
					thumbnail.hide();
				}
				state = InputState.IDLE;
			}

			clickedColumn = null;
		}

		@Override
		public void actionPressed(InputEventInfo event)
		{
			synchronized (stateLock)
			{
				state = InputState.PRESSED;
				pressAnchor = event.getGlobalPoint();
			}

			LinkColumn clickTestColumn = panel.getClickedColumn(event);
			if (clickTestColumn != null)
			{
				clickedColumn = clickTestColumn;
			}

			if (clickedColumn != null)
			{
				pressAndHold.start(event.getGlobalPoint());
			}
		}

		private class PressAndHoldTimer extends Timer
		{
			private Point point;

			void start(Point point)
			{
				this.point = point;

				synchronized (stateLock)
				{
					schedule(new Task(), 500L);
				}
			}

			private class Task extends TimerTask
			{
				@Override
				public void run()
				{
					synchronized (stateLock)
					{
						if (state == InputState.PRESSED)
						{
							thumbnail.displayThumbnail(clickedColumn.linkOppositeAnchor.getCanvasId());

							state = InputState.THUMBNAIL;
						}
					}
				}
			}
		}
	}

	private static class LinkSorter implements Comparator<CCanvasLinkAnchor>
	{
		@Override
		public int compare(CCanvasLinkAnchor first, CCanvasLinkAnchor second)
		{
			boolean firstIsOutgoing = (first.getCanvasId() == first.getLink().getAnchorA().getCanvasId());
			boolean secondIsOutgoing = (second.getCanvasId() == second.getLink().getAnchorA().getCanvasId());
			if (firstIsOutgoing != secondIsOutgoing)
			{
				if (firstIsOutgoing)
				{
					return -1;
				}
				else
				{
					return 1;
				}
			}

			CCanvas firstTargetCanvas = CCanvasController.canvasdb.get(first.getOpposite().getCanvasId());
			CCanvas secondTargetCanvas = CCanvasController.canvasdb.get(second.getOpposite().getCanvasId());
			return firstTargetCanvas.getIndex() - secondTargetCanvas.getIndex();
		}
	}
}
