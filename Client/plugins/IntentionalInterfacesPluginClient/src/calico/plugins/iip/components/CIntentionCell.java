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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;

import javax.swing.SwingUtilities;

import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.components.CCanvas;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CHistoryController;
import calico.controllers.CCanvasController.HistoryFrame;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.networking.netstuff.CalicoPacket;
import calico.plugins.iip.IntentionalInterfacesClientPlugin;
import calico.plugins.iip.IntentionalInterfacesNetworkCommands;
import calico.plugins.iip.components.graph.IntentionGraph;
import calico.plugins.iip.components.piemenu.iip.CreateLinkButton;
import calico.plugins.iip.components.piemenu.iip.DeleteCanvasButton;
import calico.plugins.iip.components.piemenu.iip.SetCanvasTitleButton;
import calico.plugins.iip.components.piemenu.iip.UnpinCanvas;
import calico.plugins.iip.components.piemenu.iip.ZoomToBranchButton;
import calico.plugins.iip.components.piemenu.iip.ZoomToCenterRingButton;
import calico.plugins.iip.components.piemenu.iip.ZoomToClusterButton;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.IntentionCanvasController;
import calico.plugins.iip.iconsets.CalicoIconManager;
import calico.plugins.iip.inputhandlers.CIntentionCellInputHandler;
import calico.plugins.iip.util.IntentionalInterfacesGraphics;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.nodes.PComposite;

/**
 * Represents a canvas thumbnail in the Intention View, both in the plugin's internal model and in the Piccolo component
 * hierarchy (with inner class <code>Shell</code>). The classname <code>CIntentionCell</code> is generally abbreviated
 * "CIC" throughout the documentation.
 * 
 * @author Byron Hawkins
 */
public class CIntentionCell implements CalicoEventListener
{
	public static final String DEFAULT_TITLE = "<default>";
	private static final double MINIMUM_SNAPSHOT_SCALE = 1.0;
	public static final Color COORDINATES_COLOR = Color.blue;
	private static final Insets THUMBNAIL_INSETS = new Insets(2, 2, 2, 2);
	public static final Dimension THUMBNAIL_SIZE = new Dimension(200, 130);
	public static final Font COORDINATES_FONT = new Font("Helvetica", Font.BOLD, THUMBNAIL_SIZE.width / 10);
	
	private static Image iconImage_create = CalicoIconManager.getIconImage("intention-graph.link-create");
	private static Image iconImage_delete = CalicoIconManager.getIconImage("intention-graph.link-delete");
	private static Image pinImage = CalicoIconManager.getIconImage(
			"intention-graph.cell-pin");
	private static Image pinSelectedImage= CalicoIconManager.getIconImage(
			"intention-graph.cell-pin-selected");
			
	
	public enum CellIconType {NONE, CREATE_LINK, DELETE_LINK};
	
	private CellIconType iconToShow = CellIconType.NONE;
	
	private static final int BUBBLE_MENU_TYPE_ID = BubbleMenu.registerType(new BubbleMenuComponentType());
	
	/**
	 * Simple button to delete a canvas and its associated CIC.
	 */
	private final DeleteCanvasButton deleteCanvasButton = new DeleteCanvasButton();
	/**
	 * Button for initiating the arrow creation phase, which is governed by <code>CIntentionArrowPhase</code>.
	 */
	private final CreateLinkButton linkButton = new CreateLinkButton();
	/**
	 * Simple button to zoom and pan the Intention View such that the cluster containing the selected CIC fits neatly in
	 * the Intention View.
	 */
	private final ZoomToClusterButton zoomToClusterButton = new ZoomToClusterButton();
	/**
	 * Opens a dialog to set the name of the canvas
	 */
	private final SetCanvasTitleButton setCanvasTitleButton = new SetCanvasTitleButton();
	/**
	 * Simple button to zoom into the center ring of the cluster
	 */
	private final ZoomToCenterRingButton zoomToCenterRingButton = new ZoomToCenterRingButton();
	/**
	 * Simple button to zoom into the center ring of the cluster
	 */
	private final ZoomToBranchButton zoomToBranchButton = new ZoomToBranchButton();
	/**
	 * Unpin the currently selected CIC
	 */
	private final UnpinCanvas unpinCanvas = new UnpinCanvas();

	private enum BorderColor
	{
		PLAIN(Color.black),
		HIGHLIGHTED(new Color(0xFFFF30));

		Color color;

		private BorderColor(Color color)
		{
			this.color = color;
		}
	}

	/**
	 * Identifies this cell.
	 */
	private long uuid;
	/**
	 * Identifies the canvas for which this cell renders thumbnails.
	 */
	private long canvas_uuid;
	/**
	 * Pixel position in the Intention View of the upper left corner of the canvas thumbnail.
	 */
	private Point2D location;
	/**
	 * Title of the canvas, which appears both on the CIC and on the canvas itself (represented there by
	 * <code>CanvasTitlePanel</code>). The title is maintained in this class because it is a feature of this plugin, but
	 * the title is still effectively associated with the canvas.
	 */
	private String title;
	/**
	 * Tag associated with the canvas of this CIC. The tag is maintained in this class because it is a feature of this
	 * plugin, but the tag is still effectively associated with the canvas.
	 */
	private long intentionTypeId = -1L;

	/**
	 * Rendering state flag indicating that the border of the CIC is currently drawn in the highlighted color.
	 */
	private boolean highlighted = false;
	/**
	 * State flag used in the construction process of a canvas. See <code>CIntentionCellFactory</code> for details.
	 */
	private boolean isNew = false;

	/**
	 * Represents the CIC in the Piccolo component hierarchy of the Intention View.
	 */
	private final Shell shell;
	
	private boolean isPinned = false;
	
	private PImage pin = new PImage(pinImage);

	/**
	 * Create a new CIC and add it to the Intention View.
	 */
	public CIntentionCell(long uuid, long canvas_uuid, Point2D location, String title)
	{
		this.uuid = uuid;
		this.canvas_uuid = canvas_uuid;
		this.location = location;
		this.title = title;

		shell = new Shell(location.getX(), location.getY());
		if (!IntentionGraph.getInstance().isClusterRoot(canvas_uuid))
			CalicoDraw.addChildToNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT), shell);
		
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_TAG, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_UNTAG, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_SET_TITLE, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_UPDATE_FINISHED, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_TOPOLOGY, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CLINK_CREATE, this, CalicoEventHandler.PASSIVE_LISTENER);
		
//		IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).addChild(shell);
	}
	
	public boolean isNew()
	{
		return isNew;
	}

	public void setNew(boolean isNew)
	{
		this.isNew = isNew;
	}

	/**
	 * Create and install the thumbnail image.
	 */
	public void initialize()
	{
		shell.updateContents();
	}

	/**
	 * Detach all the resources of the CIC from the Intention View.
	 */
	public void delete()
	{
		shell.delete();
	}

	private Color currentBorderColor()
	{
		if (false /*highlighted*/)
		{
			return BorderColor.HIGHLIGHTED.color;
		}
		else
		{
			return BorderColor.PLAIN.color;
		}
	}

	public long getId()
	{
		return uuid;
	}

	public long getCanvasId()
	{
		return canvas_uuid;
	}

	public String getTitle()
	{
		String ret = "";
		
		//Pre-append cluster prefix
		if (CIntentionCellController.getInstance().isRootCanvas(canvas_uuid))
		{
			long[] roots = calico.plugins.iip.components.graph.IntentionGraph.getInstance().getRootsOfAllClusters();
			int position = 0;
			while (position < roots.length)
				if (roots[position] == canvas_uuid)
					break;
				else
					position++;
			
			ret += (position+1) + ". ";

		}
		
		ret += getTitleWithoutPrefix();
		

		
		return ret;
	}

	public String getTitleWithoutPrefix() {
		String ret;
		
		if (title.equals(DEFAULT_TITLE))
		{
			if (CIntentionCellController.getInstance().isRootCanvas(canvas_uuid))
				ret = "Unnamed cluster";
			else if (CCanvasController.canvasdb.containsKey(canvas_uuid))
				ret = "Canvas " + CCanvasController.canvasdb.get(canvas_uuid).getIndex();
			else
				ret = "Canvas ";
		}
		else 
			ret = title;
		return ret;
	}
	


	/**
	 * Return true if the user has set a title on the canvas associated to this CIC, or false if the canvas uses the
	 * default title.
	 */
	public boolean hasUserTitle()
	{
		return !title.equals(DEFAULT_TITLE);
	}

	public void setTitle(String title)
	{
		this.title = title;
//		shell.titleBar.updateTitle();
	}

	public Long getIntentionTypeId()
	{
		return intentionTypeId;
	}

	public void setIntentionType(long intentionTypeId)
	{
		this.intentionTypeId = intentionTypeId;
	}

	/**
	 * Return true if the user has assigned a tag to the canvas associated to this CIC.
	 */
	public boolean hasIntentionType()
	{
		return intentionTypeId >= 0L;
	}

	/**
	 * Remove any tag that may have been assigned to the canvas associated with this CIC.
	 */
	public void clearIntentionType()
	{
		intentionTypeId = -1L;
	}

	/**
	 * Return true when <code>point</code> contacts the physical coordinate region occupied by the canvas thumbnail of
	 * this CIC in the Intention View. It is assumed that <code>point</code> specifies screen coordinates.
	 */
	public boolean contains(Point2D point)
	{
		if (IntentionGraph.getInstance().isClusterRoot(this.getCanvasId()))
			return false;
		PBounds bounds = shell.getGlobalBounds();
		return ((point.getX() > bounds.x) && (point.getY() > bounds.y) && ((point.getX() - bounds.x) < bounds.width) && (point.getY() - bounds.y) < bounds.height);
	}

	/**
	 * Get the pixel position of this CIC within the Intention View.
	 */
	public Point2D getLocation()
	{
		return shell.getBounds().getOrigin();
	}

	/**
	 * Get the bounds of this CIC in screen coordinates.
	 */
	public PBounds getGlobalBounds()
	{
		PBounds bounds = shell.getBounds();
		// bounds.setOrigin(0.0, 0.0);
		return new PBounds(shell.localToGlobal(bounds)); // getBounds();
	}
	
	public PBounds getBounds()
	{
		PBounds bounds = shell.getBounds();
		return new PBounds(bounds);
	}

	/**
	 * Get the center point of this CIC in Intention View coordinates.
	 */
	public Point2D getCenter()
	{
		return shell.thumbnailBounds.getCenter2D();
	}

	/**
	 * Set the location of this CIC in Intention View coordinates.
	 */
	public void setLocation(final double x, final double y)
	{
//		SwingUtilities.invokeLater(
		IntentionalInterfacesClientPlugin.addNewEventDispatcherEvent(
				new Runnable() { public void run() { 
					location.setLocation(x, y);
					shell.setX(x);
					shell.setY(y);
					shell.layoutChildren();
					if (CIntentionCellInputHandler.getInstance().getActiveCell() == getId())
						SwingUtilities.invokeLater(
								new Runnable() { public void run() { 
									if (getBounds() != null)
										BubbleMenu.moveIconPositions(getBounds());
								}});
						
//					shell.repaint();
				}});


//		CalicoDraw.repaint(shell);
//		shell.repaint();
	}

	public Dimension2D getSize()
	{
		return shell.thumbnailBounds.getSize();
	}

	/**
	 * Clone the bounds of this CIC, which occur in Intention View coordinates.
	 */
	public PBounds copyBounds()
	{
		return (PBounds) shell.thumbnailBounds.clone();
	}

	public void setHighlighted(boolean highlighted)
	{
		this.highlighted = highlighted;
		CalicoDraw.repaint(shell);
//		shell.repaint();
	}

	/**
	 * Return true if this CIC can be seen in the present visible area of the Intention View.
	 */
	public boolean isInGraphFootprint()
	{
		return IntentionGraph.getInstance().getLocalBounds(IntentionGraph.Layer.CONTENT).intersects(shell.getBounds());
	}

	/**
	 * Update the canvas thumbnail to reflect its most recent contents.
	 */
	public void contentsChanged()
	{
		shell.canvasSnapshot.contentsChanged();
	}

	/**
	 * Update the iconification state of this CIC, based on the current zoom ratio and whether iconification mode is
	 * enabled. This feature is obsolete.
	 */
	public void updateIconification()
	{
		shell.updateIconification();
		CalicoDraw.repaint(shell);
//		shell.repaint();
	}

	/**
	 * Update the list of users drawn in the upper right corner of this CIC.
	 */
	public void updateUserList()
	{
		shell.userList.updateUsers();
	}

	/**
	 * Return true if the present zoom ratio allows the thumbnail to be drawn. This is used for iconification, which is
	 * obsolete.
	 */
	private boolean scaleAllowsSnapshot()
	{
		return ((!IntentionGraph.getInstance().getIconifyMode()) || (IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale() >= MINIMUM_SNAPSHOT_SCALE));
	}

	private static final int BORDER_WIDTH = 1;

	/**
	 * Represents the CIC in the Piccolo component hierarchy of the Intention View. The size of a CIC is statically
	 * defined as <code>THUMBNAIL_SIZE</code>, so all scaling occurs globally to the Intention View.
	 * 
	 * @author Byron Hawkins
	 */
	private class Shell extends PComposite implements PropertyChangeListener
	{
		private final Color BACKGROUND_COLOR = new Color(0xFF, 0xFF, 0xFF, 0xCC);

		/**
		 * Renders the canvas number in the upper left corner.
		 */
		private final PImage canvasAddress;
		/**
		 * Renders the thumbnail image.
		 */
		private final CanvasSnapshot canvasSnapshot = new CanvasSnapshot();
		
		private final PImage cellIcon = new PImage(); 
		/**
		 * Renders the title of the canvas, above the CIC and left justified.
		 */
		private final TitleBar titleBar = new TitleBar();
		/**
		 * Renders the list of users currently viewing the canvas associated with this CIC.
		 */
		private final UserList userList = new UserList();

		/**
		 * State flag indicating that the thumbnail is currently being displayed. This flag supports iconification,
		 * which is obsolete.
		 */
		private boolean showingSnapshot = false;

		/**
		 * Bounds of the thumbnail rectangle, not including the title sitting above the CIC.
		 */
		private PBounds thumbnailBounds = new PBounds();

		/**
		 * Maintains the last zoom ratio, to avoid regenerating the thumbnail image when the Intention View display
		 * changes in any way that does not affect the rendering of this thumbnail (including miniscule zoom changes
		 * that have no net effect).
		 */
		private double lastScale = Double.MIN_VALUE;

		public Shell(double x, double y)
		{
			canvasAddress = new PImage(IntentionalInterfacesGraphics.superimposeCellAddress(
					CalicoIconManager.getIconImage("intention-graph.obscured-intention-cell"), canvas_uuid));
			CalicoDraw.addChildToNode(this, canvasAddress);
//			addChild(canvasAddress);

			CalicoDraw.addChildToNode(this, titleBar);
			CalicoDraw.addChildToNode(this, userList);
//			addChild(titleBar);
//			addChild(userList);

			thumbnailBounds.setRect(x, y, THUMBNAIL_SIZE.width - (CCanvas.ROUNDED_RECTANGLE_OVERFLOW + CCanvas.CELL_MARGIN), THUMBNAIL_SIZE.height
					- (CCanvas.ROUNDED_RECTANGLE_OVERFLOW + CCanvas.CELL_MARGIN));
			CalicoDraw.setNodeBounds(this, thumbnailBounds);
//			setBounds(thumbnailBounds);

			titleBar.setWidth(thumbnailBounds.getWidth());

			updateIconification();
			
			cellIcon.setBounds(new Rectangle(0,0,64,64));
			cellIcon.setVisible(false);
			CalicoDraw.addChildToNode(this, cellIcon);
			
			pin.setVisible(false);
			CalicoDraw.addChildToNode(this, pin);
//			this.addChild(cellIcon);

			IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).addPropertyChangeListener(PNode.PROPERTY_TRANSFORM, this);

			CalicoDraw.moveNodeToFront(userList);
//			userList.moveToFront();
//			CalicoDraw.repaint(this);
//			repaint();
		}
		
		private void setIconImage(final Image image)
		{
			SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
						cellIcon.setImage(image);
					}});
		}

		void delete()
		{
			IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).removeChild(this);
			IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).removePropertyChangeListener(PNode.PROPERTY_TRANSFORM, this);
		}

		void updateIconification()
		{
			lastScale = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale();
			if (showingSnapshot != scaleAllowsSnapshot())
			{
				if (showingSnapshot)
				{
					CalicoDraw.removeChildFromNode(this, canvasSnapshot.snapshot);
//					removeChild(canvasSnapshot.snapshot);
					CalicoDraw.addChildToNode(this, canvasAddress);
//					addChild(canvasAddress);
				}
				else
				{
					CalicoDraw.removeChildFromNode(this, canvasAddress);
//					removeChild(canvasAddress);
					CalicoDraw.addChildToNode(this, canvasSnapshot.snapshot);
//					addChild(canvasSnapshot.snapshot);
				}

				showingSnapshot = !showingSnapshot;
			}
		}

		void updateContents()
		{
			if (IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getScale() != lastScale)
			{
				updateIconification();
			}

			if (canvasSnapshot.isDirty)
			{
				canvasSnapshot.contentsChanged();
			}
		}

		@Override
		public void propertyChange(PropertyChangeEvent event)
		{
			updateContents();
		}

		@Override
		protected void paint(PPaintContext paintContext)
		{
			super.paint(paintContext);

			Graphics2D g = paintContext.getGraphics();
			Color c = g.getColor();
			Paint p = g.getPaint();

			g.setColor(BACKGROUND_COLOR);
			g.fill(getBounds());

			g.setColor(currentBorderColor());
			g.translate(thumbnailBounds.x, thumbnailBounds.y);

						
			if (highlighted)
				drawHighlight(g);
			drawHistoryHighlight(g);

			
			g.drawRoundRect(0, 0, ((int) thumbnailBounds.width) - 1, ((int) thumbnailBounds.height) - 1, 10, 10);
			IntentionalInterfacesGraphics.superimposeCellAddressInCorner(g, canvas_uuid, thumbnailBounds.width - (2 * BORDER_WIDTH), COORDINATES_FONT,
					COORDINATES_COLOR);
			
			g.translate(-thumbnailBounds.x, -thumbnailBounds.y);
			g.setPaint(p);
			g.setColor(c);
			
			
		}
		
		private void drawHighlight(Graphics2D g) {
			Stroke oldStroke = g.getStroke();
			Color oldColor = g.getColor();
			Paint oldPaint = g.getPaint();
			Stroke borderStroke;


			Color borderColor = Color.BLUE;
			Point2D center = new Point2D.Float((float)thumbnailBounds.getWidth()/2, 
					(float)thumbnailBounds.getHeight()/2);
			float radius = (float)thumbnailBounds.getWidth() + 25;
			float[] dist = {.2f, .8f};

			borderStroke =
					new BasicStroke(50f,
							BasicStroke.CAP_BUTT,
							BasicStroke.JOIN_MITER);
			Color highlightColor = new Color(borderColor.getRed(),borderColor.getGreen(),borderColor.getBlue(), 255);
			Color[] colors = {highlightColor, Color.WHITE};
			RadialGradientPaint gradient =
					new RadialGradientPaint(center, radius, dist, colors);
			g.setPaint(gradient);

			g.drawRoundRect(0, 0, ((int) thumbnailBounds.width) - 1, ((int) thumbnailBounds.height) - 1, 10, 10);
			g.setStroke(borderStroke);
			g.drawRoundRect(0, 0, ((int) thumbnailBounds.width) - 1, ((int) thumbnailBounds.height) - 1, 10, 10);

			g.setStroke(oldStroke);
			g.setPaint(oldPaint);
			g.setColor(oldColor);
		}

		private void drawHistoryHighlight(Graphics2D g) {
			Stroke oldStroke = g.getStroke();
			Color oldColor = g.getColor();
			Paint oldPaint = g.getPaint();
			Stroke borderStroke;
			long[] mostRecentFrames = new long[1];
			for (int i = 0; i < mostRecentFrames.length; i++)
			{
				CHistoryController.Frame f = CHistoryController.getInstance().getFrame(i);
				if (f != null && f instanceof HistoryFrame)
					mostRecentFrames[i] =  ((HistoryFrame)f).getCanvasId();
				else
					mostRecentFrames[i] = 0l;
			}
			
//			for (int i = 0; i < mostRecentFrames.length; i++)
//			if (getCanvasId() == mostRecentFrames[i])
//			{
//				g.setColor(new Color(0, 0, 0, (int)(255 * (((float)5 - i)/5))));
//				borderStroke =
//		        new BasicStroke(4.0f,
//		                        BasicStroke.CAP_BUTT,
//		                        BasicStroke.JOIN_MITER);
//
//			}
			
			
			Color borderColor = Color.BLUE;
			Point2D center = new Point2D.Float((float)thumbnailBounds.getWidth()/2, 
					(float)thumbnailBounds.getHeight()/2);
		     float radius = (float)thumbnailBounds.getWidth() + 25;
		     float[] dist = {.2f, .8f};

		     
			
			if (getCanvasId() == mostRecentFrames[0])
			{
				borderStroke =
		        new BasicStroke(50f,
		                        BasicStroke.CAP_BUTT,
		                        BasicStroke.JOIN_MITER);
			     Color highlightColor = new Color(borderColor.getRed(),borderColor.getGreen(),borderColor.getBlue(), 255);
			     Color[] colors = {highlightColor, Color.WHITE};
			     RadialGradientPaint gradient =
			         new RadialGradientPaint(center, radius, dist, colors);
				g.setPaint(gradient);
				
				g.drawRoundRect(0, 0, ((int) thumbnailBounds.width) - 1, ((int) thumbnailBounds.height) - 1, 10, 10);
				g.setStroke(borderStroke);
				g.drawRoundRect(0, 0, ((int) thumbnailBounds.width) - 1, ((int) thumbnailBounds.height) - 1, 10, 10);
			}
			
			for (int i = 1; i < mostRecentFrames.length; i++)
				if (getCanvasId() == mostRecentFrames[i])
				{
					borderStroke =
							new BasicStroke(50f,
									BasicStroke.CAP_BUTT,
									BasicStroke.JOIN_MITER);
					int opacityFactor = (int)(255 * 0.5f * ((float)mostRecentFrames.length - i)/mostRecentFrames.length);
					Color highlightColor = new Color(borderColor.getRed(),borderColor.getGreen(),borderColor.getBlue(), opacityFactor);
					Color[] colors = {highlightColor, Color.WHITE};
					RadialGradientPaint gradient =
							new RadialGradientPaint(center, radius, dist, colors);
					g.setPaint(gradient);
					g.setStroke(borderStroke);
					g.drawRoundRect(0, 0, ((int) thumbnailBounds.width) - 1, ((int) thumbnailBounds.height) - 1, 10, 10);
				}
			g.setStroke(oldStroke);
			g.setPaint(oldPaint);
			g.setColor(oldColor);
		}

		@Override
		protected void layoutChildren()
		{
			titleBar.setX(getX());
			titleBar.setY(getY() - titleBar.HEIGHT);

			userList.setX(getX() + 4);
			userList.setY(getY() + 2);

			thumbnailBounds.setOrigin(getX(), getY());

			if (showingSnapshot)
			{
				canvasSnapshot.snapshot.setBounds(thumbnailBounds.x + BORDER_WIDTH, thumbnailBounds.y + BORDER_WIDTH, thumbnailBounds.width
						- (2 * BORDER_WIDTH), thumbnailBounds.height - (2 * BORDER_WIDTH));
			}
			else
			{
				canvasAddress.setBounds(thumbnailBounds.x + BORDER_WIDTH, thumbnailBounds.y + BORDER_WIDTH, thumbnailBounds.width - (2 * BORDER_WIDTH),
						thumbnailBounds.height - (2 * BORDER_WIDTH));
			}
			
			cellIcon.setBounds(new Rectangle((int)thumbnailBounds.x + BORDER_WIDTH, (int)thumbnailBounds.y + BORDER_WIDTH,64,64));
			cellIcon.setPaintInvalid(false);
			
			pin.setBounds(thumbnailBounds.x + thumbnailBounds.width - pin.getBoundsReference().width, thumbnailBounds.y, pin.getBoundsReference().width, pin.getBoundsReference().height);
//			cellIcon.moveToFront();
		}
	}

	private class TitleBar extends PComposite
	{
		private final int HEIGHT = THUMBNAIL_SIZE.width / 7;
		private final int LEFT_INSET = 2;
		private final int TEXT_INSET = 1;

		private final int FADE_HEIGHT = HEIGHT;
		private final Color MASK_COLOR = new Color(0xFF, 0xFF, 0xFF, 0xDD);
		private final Color TRANSPARENT = new Color(0xFF, 0xFF, 0xFF, 0x00);
		private final GradientPaint TOP_FADE = new GradientPaint(0f, 0f, TRANSPARENT, 0f, FADE_HEIGHT / 2, MASK_COLOR);
		private final GradientPaint BOTTOM_FADE = new GradientPaint(0f, FADE_HEIGHT / 2, MASK_COLOR, 0f, FADE_HEIGHT, TRANSPARENT);

		private final PText title = new PText();

		public TitleBar()
		{
			// width is arbitrary, it will be immediately changed by the Shell
			setBounds(0, 0, 100, HEIGHT);
			title.setFont(new Font("Helvetica", Font.PLAIN, THUMBNAIL_SIZE.width / 10));

			CalicoDraw.addChildToNode(this, title);
//			addChild(title);
			updateTitle();
		}

		private void updateTitle()
		{
			int index = getSiblingIndex();
			
//			String tag = "";
			String titlePrefix = "";
//			if (!CIntentionCellController.getInstance().isRootCanvas(canvas_uuid))
				titlePrefix = getTitlePrefix() /*+ getSiblingIndex() + ". "*/;
			
//			if (getIntentionTypeId() != -1
//					&&  IntentionCanvasController.getInstance().intentionTypeExists(getIntentionTypeId()))
//				tag = " (" + IntentionCanvasController.getInstance().getIntentionType(getIntentionTypeId()).getName() + ")";
			
			title.setText(titlePrefix + getTitle() /* + tag*/);
//			CalicoDraw.repaint(this);
//			repaint();
		}



		@Override
		protected void layoutChildren()
		{
			PBounds bounds = getBounds();
			title.setBounds(bounds.x + LEFT_INSET, bounds.y + TEXT_INSET, bounds.width - LEFT_INSET, bounds.height - (2 * TEXT_INSET));
		}

		@Override
		protected void paint(PPaintContext paintContext)
		{
			Graphics2D g = paintContext.getGraphics();
			int yTitle = (int) (title.getY() - (((FADE_HEIGHT - HEIGHT) / 2)));
			g.translate(title.getX(), yTitle);

			g.setPaint(TOP_FADE);
			g.fillRect(-2, 0, ((int) title.getBounds().width) + 4, FADE_HEIGHT / 2);
			g.setPaint(BOTTOM_FADE);
			g.fillRect(-2, FADE_HEIGHT / 2, ((int) title.getBounds().width) + 4, FADE_HEIGHT);
			g.translate(-title.getX(), -yTitle);

			super.paint(paintContext);
		}
	}
	
	public int getSiblingIndex() {
		long parentUUID = CIntentionCellController.getInstance().getCIntentionCellParent(canvas_uuid);
		long[] siblings = CIntentionCellController.getInstance().getCIntentionCellChildren(parentUUID);
		
		int index = 1;
		for (int i = 0; i < siblings.length; i++)
		{
			if (siblings[i] == canvas_uuid)
				index = i+1;
		}
		return index;
	}
	
	public String getTitlePrefix() {
		
		String titlePrefix = "";
		

		
		//get parent cell
		int centerRingIndex = 0;
		int jumpsToRoot = 0;
		long parentCanvasId = CIntentionCellController.getInstance().getCIntentionCellParent(canvas_uuid);
		while (parentCanvasId > 0l)
		{
			long nextParent = CIntentionCellController.getInstance().getCIntentionCellParent(parentCanvasId);
			if (nextParent > 0l)
			{
				jumpsToRoot++;
				centerRingIndex = CIntentionCellController.getInstance().getCellByCanvasId(parentCanvasId).getSiblingIndex();
			}
			parentCanvasId = nextParent;
		}
		
//		long parentCanvasId = CIntentionCellController.getInstance().getCIntentionCellParent(canvas_uuid);
//		if (parentCanvasId > 0l)
//		{
//			CIntentionCell parentCell = CIntentionCellController.getInstance().getCellByCanvasId(parentCanvasId);
//			titlePrefix += parentCell.getTitlePrefix();
//		}
				
		if (CIntentionCellController.getInstance().isRootCanvas(CIntentionCellController.getInstance().getCIntentionCellParent(canvas_uuid)))
			titlePrefix = getSiblingIndex() + ". ";
		else if (!CIntentionCellController.getInstance().isRootCanvas(canvas_uuid))
			titlePrefix = centerRingIndex + "." + jumpsToRoot + " ";
//			titlePrefix += getSiblingIndex() + ". ";
//		else 
//		{
//			int clusterIndex = getClusterIndex();
//			if (clusterIndex != -1)
//				titlePrefix += "C" + clusterIndex + ".";
//			else
//				titlePrefix += "C#.";
//		}
		

		
		return titlePrefix;
	}
	
	public int getClusterIndex()
	{
		return IntentionGraph.getInstance().getClusterIndex(CIntentionCellController.getInstance().getClusterRootCanvasId(canvas_uuid)) + 1;
	}

	private class UserList extends PText
	{
		public UserList()
		{
			setText("Username"); // template, for sizing
			setFont(new Font("Helvetica", Font.BOLD, THUMBNAIL_SIZE.width / 10));
			setTextPaint(Color.BLUE);
			setBounds(this.getBounds().getBounds());
			setConstrainWidthToTextWidth(true);
			setConstrainHeightToTextHeight(true);

			setText("");
		}

		void updateUsers()
		{
			StringBuilder userListText = new StringBuilder();
			int[] clients = CCanvasController.canvasdb.get(canvas_uuid).getClients();
			for (int i = 0; i < clients.length; i++)
			{
				if (CalicoDataStore.clientInfo.containsKey(clients[i]) && !CalicoDataStore.clientInfo.get(clients[i]).equals(CalicoDataStore.Username))
				{
					userListText.append(CalicoDataStore.clientInfo.get(clients[i]) + "\n");
				}

			}

			if (!getText().equals(userListText.toString()))
			{
				setText(userListText.toString());
//				CalicoDraw.repaint(this);
//				repaint();
			}
		}

		@Override
		protected void paint(PPaintContext paintContext)
		{
			Graphics2D g = paintContext.getGraphics();
			g.setColor(Color.white);
			g.fill(getBounds());

			super.paint(paintContext);
		}
	}

	private class CanvasSnapshot
	{
		private final PImage snapshot = new PImage();

		private boolean isDirty = true;

		boolean isOnScreen()
		{
			return (isInGraphFootprint() && scaleAllowsSnapshot());
		}

		void contentsChanged()
		{
			if (isInGraphFootprint() && shell.showingSnapshot)
			{
				updateSnapshot();
			}
			else
			{
				isDirty = true;
			}
		}

		private void updateSnapshot()
		{
			long start = System.currentTimeMillis();
			snapshot.setImage(IntentionalInterfacesGraphics.createCanvasThumbnail(canvas_uuid, THUMBNAIL_INSETS));
			CalicoDraw.setNodeBounds(snapshot, shell.thumbnailBounds);
//			snapshot.setBounds(shell.thumbnailBounds);
			isDirty = false;

//			CalicoDraw.repaint(snapshot);
//			snapshot.repaint();
		}
	}

	@Override
	public void handleCalicoEvent(int event, CalicoPacket p) {
		
		if (event == IntentionalInterfacesNetworkCommands.CIC_TAG
				|| event == IntentionalInterfacesNetworkCommands.CIC_UNTAG
				|| event == IntentionalInterfacesNetworkCommands.CIC_SET_TITLE
				|| event == IntentionalInterfacesNetworkCommands.CIC_UPDATE_FINISHED
				|| event == IntentionalInterfacesNetworkCommands.CIC_TOPOLOGY
				|| event == IntentionalInterfacesNetworkCommands.CLINK_CREATE)
		{
			shell.titleBar.updateTitle();
//			removeIfRootCanvas();
		}
		if (event == IntentionalInterfacesNetworkCommands.CIC_TOPOLOGY
				|| event == IntentionalInterfacesNetworkCommands.CLINK_CREATE)
		{
			SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
						hideIfRootCanvas();
					}});
			
		}
		
		
	}

	public void hideIfRootCanvas() {
		//This line of code is added to prevent new cluster centers from appearing in the user's view
		if (
			//There is already a check to not add the shell, but it fails to detect that it's a cluster center so
				//it was added anyways. This next line checks if it's inside.
			IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getChildrenReference().contains(shell)
			//This next line checks that it's a cluster root. This line returns true when it shouldn't for new
				//CICs, not sure why.
				&& IntentionGraph.getInstance().isClusterRoot(canvas_uuid)
			//The last check that makes this work is to look for links. This if-clause will remove ALL new 
				//CICs because they're flagged as canvas centers (even when they're not). Another way to check
				//if the new CIC is a new cluster is to check for CCanvasLinks.
				&& CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(canvas_uuid).size() == 0)
			CalicoDraw.removeChildFromNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT), shell);
		else if (
				!IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT).getChildrenReference().contains(shell)
				&& !IntentionGraph.getInstance().isClusterRoot(canvas_uuid)
				&& CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(canvas_uuid).size() > 0)
			CalicoDraw.addChildToNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT), shell);
		
	}
	
	public void setCellIcon(CellIconType iconToShow)
	{
		switch (iconToShow)
		{
		case NONE:
			CalicoDraw.setVisible(shell.cellIcon,false);
			break;
		case CREATE_LINK:
			shell.setIconImage(CIntentionCell.iconImage_create);
			CalicoDraw.setVisible(shell.cellIcon,true);
			break;
		case DELETE_LINK:
			shell.setIconImage(CIntentionCell.iconImage_delete);
			CalicoDraw.setVisible(shell.cellIcon,true);
			break;
		}
			
		this.iconToShow = iconToShow;
	}
	
	public void moveToFront()
	{
		shell.moveToFront();
	}
	
	public void hide()
	{
		shell.setVisible(false);
	}
	
	public void show()
	{
		shell.setVisible(true);
	}
	
	public boolean getVisible()
	{
		return shell.getVisible();
	}
	
	public boolean getIsPinned()
	{
		return isPinned;
	}
	
	public void setIsPinned(boolean value)
	{
		if (isPinned == value)
			return;
		
		long[] children = CIntentionCellController.getInstance().getCIntentionCellChildren(getCanvasId());
		if (children.length > 0)
			 CIntentionCellController.getInstance().getCellByCanvasId(children[0]).setIsPinned(value);
		
		isPinned = value;
		pin.setVisible(isPinned);
		CalicoDraw.repaint(pin);
		//show/hide pin
	}
	
	public void setPinHighlighted(boolean value)
	{
		if (value)
			pin.setImage(pinSelectedImage);
		else
			pin.setImage(pinImage);
	}
	
	/**
	 * 
	 * @param p Point in global bounds
	 * @return
	 */
	public boolean pinImageContainsPoint(Point p)
	{
//		System.out.println("Rect: " + pin.getGlobalBounds().toString() + "\nPoint: " + p.toString() );
		return pin.getGlobalBounds().contains(p);
	}
	
	public void setDragging(boolean value)
	{
		if (value)
			shell.setTransparency(.7f);
		else
			shell.setTransparency(1.0f);
	}
	
	public void showBubbleMenu() {
		boolean isRootCanvas = CIntentionCellController.getInstance().isRootCanvas(getCanvasId());
		boolean isRootChildCanvas = CIntentionCellController.getInstance().isRootCanvas(
				CIntentionCellController.getInstance().getCIntentionCellParent(getCanvasId()));
		
		ArrayList<PieMenuButton> buttons = new ArrayList<PieMenuButton>();
		
		buttons.add(setCanvasTitleButton);
		
		if (CCanvasController.canvasdb.size() > 1
				&& isRootChildCanvas)
		{
			buttons.add(deleteCanvasButton);
			buttons.add(linkButton);
			buttons.add(zoomToCenterRingButton);
			if (CIntentionCellController.getInstance().getCIntentionCellChildren(getCanvasId()).length > 0)
				buttons.add(zoomToBranchButton);
		}
		else if (CCanvasController.canvasdb.size() > 1
				&& !isRootCanvas)
		{
			buttons.add(deleteCanvasButton);
			buttons.add(linkButton);
			buttons.add(zoomToCenterRingButton);
			buttons.add(zoomToBranchButton);
		}
		
		if (getIsPinned())
			buttons.add(unpinCanvas);
		
		BubbleMenu.displayBubbleMenu(getId(), true, BUBBLE_MENU_TYPE_ID, buttons.toArray(new PieMenuButton[buttons.size()]));
	}
	
	/**
	 * Integration point for a CIC with the bubble menu.
	 * 
	 * @author Byron Hawkins
	 */
	private static class BubbleMenuComponentType implements BubbleMenu.ComponentType
	{
		@Override
		public PBounds getBounds(long uuid)
		{
			PBounds local = CIntentionCellController.getInstance().getCellById(uuid).getGlobalBounds();
			if (CCanvasController.exists(CCanvasController.getCurrentUUID()))	
				return new PBounds(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getLayer().globalToLocal(local));
			else
				return local;
		}

		@Override
		public void highlight(boolean b, long uuid)
		{
			if (CIntentionCellController.getInstance().getCellById(uuid) != null)
				CIntentionCellController.getInstance().getCellById(uuid).setHighlighted(b);
		}

		@Override
		public int getButtonPosition(String buttonClassname)
		{
			if (buttonClassname.equals(DeleteCanvasButton.class.getName()))
			{
				return 1;
			}
			if (buttonClassname.equals(CreateLinkButton.class.getName()))
			{
				return 2;
			}
			if (buttonClassname.equals(ZoomToClusterButton.class.getName()))
			{
				return 3;
			}
			if (buttonClassname.equals(SetCanvasTitleButton.class.getName()))
			{
				return 3;
			}
			if (buttonClassname.equals(UnpinCanvas.class.getName()))
			{
				return 5;
			} 
			if (buttonClassname.equals(ZoomToCenterRingButton.class.getName()))
			{
				return 7;
			}
			if (buttonClassname.equals(ZoomToBranchButton.class.getName()))
			{
				return 8;
			}
			
			
			return 0;
		}
	}
}
