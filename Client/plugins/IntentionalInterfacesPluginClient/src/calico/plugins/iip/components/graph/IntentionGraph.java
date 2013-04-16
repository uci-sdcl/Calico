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
package calico.plugins.iip.components.graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import calico.Calico;
import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.Geometry;
import calico.CalicoOptions.menu.menubar;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.menus.CanvasMenuBar;
import calico.controllers.CArrowController;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CHistoryController;
import calico.events.CalicoEventHandler;
import calico.events.CalicoEventListener;
import calico.input.CalicoMouseListener;
import calico.inputhandlers.CalicoAbstractInputHandler;
import calico.inputhandlers.CalicoInputManager;
import calico.inputhandlers.InputEventInfo;
import calico.networking.netstuff.CalicoPacket;
import calico.perspectives.CalicoPerspective;
import calico.plugins.iip.IntentionalInterfacesNetworkCommands;
import calico.plugins.iip.components.CCanvasLinkArrow;
import calico.plugins.iip.components.CIntentionCell;
import calico.plugins.iip.components.canvas.CanvasInputProximity;
import calico.plugins.iip.components.canvas.CanvasTitlePanel;
import calico.plugins.iip.components.graph.CIntentionTopology.Cluster;
import calico.plugins.iip.components.menus.IntentionGraphMenuBar;
import calico.plugins.iip.components.menus.buttons.NewClusterCanvasButton;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.controllers.CIntentionCellFactory;
import calico.plugins.iip.controllers.IntentionGraphController;
import calico.plugins.iip.inputhandlers.CIntentionCellInputHandler;
import calico.plugins.iip.inputhandlers.IntentionGraphInputHandler;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolox.nodes.PClip;

/**
 * Visual container for the Intention View. The <code>contentCanvas</code>
 * contains canvas thumbnails and arrows, and the <code>canvas</code> contains
 * tools and handlers and such things. The <code>menuBar</code> sits along the
 * bottom of the screen, and the <code>topology</code> renders the visible
 * elements of the layout topology. Methods should be self explanatory by name.
 * 
 * @author Byron Hawkins
 */
public class IntentionGraph 
		implements CalicoEventListener {
	/**
	 * Represents the 3 layers of the Intention View. The order and indexing is
	 * deliberate--changing it would probably break the entire view.
	 * 
	 * @author Byron Hawkins
	 */
	public enum Layer {
		TOPOLOGY(0), CONTENT(1), TOOLS(2);

		public final int id;

		private Layer(int id) {
			this.id = id;
		}
	}

	/**
	 * Represents the two types of focus that the Intention View can have.
	 * 
	 * @author nfmangano
	 * 
	 */
	public enum Focus {
		CLUSTER(0), WALL(1);

		public final int id;

		private Focus(int id) {
			this.id = id;
		}
	}

	public static IntentionGraph getInstance() {
		if (INSTANCE == null) {
			new IntentionGraph();
		}
		return INSTANCE;
	}

	/**
	 * Represents the cluster which has focus. Only used if
	 */
	private static long clusterFocus = 0l;

	private static Focus focus = Focus.WALL;

	static ArrayList<PClip> boundBorders = new ArrayList<PClip>();

	private static IntentionGraph INSTANCE;

	/**
	 * Piccolo layer containing the painted topology elements.
	 */
	private final PLayer topologyLayer = new PLayer();
	/**
	 * Piccolo layer containing the bubble and pie menus.
	 */
	private final PLayer toolLayer = new PLayer();

	/**
	 * Piccolo canvas containing the topology, tool and content layers. The
	 * content layer is constructed within <code>contentCanvas</code>, and then
	 * added to this <code>canvas</code>, to prevent zoom from getting tangled
	 * up with menus and other statically sized stuff.
	 */
	private final ContainedCanvas canvas = new ContainedCanvas();
	/**
	 * Piccolo canvas containing the <code>CIntentionCell</code>s. This canvas
	 * is never added to the Intention View directly; instead, its only layer is
	 * extracted and added to the <code>canvas</code> above.
	 */
	private final ContainedCanvas contentCanvas = new ContainedCanvas();
	/**
	 * Simple menu bar sitting at the bottom of the Intention View.
	 */
	private IntentionGraphMenuBar menuBar;

	/**
	 * Contains the Piccolo components which render the topology of the cluster
	 * layout.
	 */
	private CIntentionTopology topology;

	/**
	 * obsolete
	 */
	private boolean iconifyMode = false;

	private final long uuid;

	private PBounds zoomRegions;

	private NewClusterCanvasButton newCanvasButton;

	private double defaultScale;

	protected Rectangle2D defaultBounds;

	public final static long WALL = -1;

	private IntentionGraph() {
		INSTANCE = this;

		uuid = Calico.uuid();

		// IntentionGraph.exitButtonBounds = new
		// Rectangle(CalicoDataStore.ScreenWidth-32,5,24,24);
		canvas.setBackground(new Color(180, 187, 197));
		canvas.setPreferredSize(new Dimension(CalicoDataStore.ScreenWidth,
				CalicoDataStore.ScreenHeight));
		setBounds(0, 0, CalicoDataStore.ScreenWidth,
				CalicoDataStore.ScreenHeight);
		translate((CalicoDataStore.ScreenWidth / 2)
				- (CIntentionCell.THUMBNAIL_SIZE.width / 2),
				(CalicoDataStore.ScreenHeight / 2)
						- (CIntentionCell.THUMBNAIL_SIZE.height / 2));

		CalicoInputManager.addCustomInputHandler(uuid,
				new IntentionGraphInputHandler());

		canvas.addMouseListener(new CalicoMouseListener());
		canvas.addMouseMotionListener(new CalicoMouseListener());

		canvas.removeInputEventListener(canvas.getPanEventHandler());
		canvas.removeInputEventListener(canvas.getZoomEventHandler());

		repaint();

		PLayer contentLayer = contentCanvas.getLayer();
		toolLayer.setParent(contentLayer.getParent());
		topologyLayer.setParent(contentLayer.getParent());
		canvas.getCamera().addLayer(Layer.TOPOLOGY.id, topologyLayer);
		canvas.getCamera().addLayer(Layer.CONTENT.id, contentLayer);
		canvas.getCamera().addLayer(Layer.TOOLS.id, toolLayer);

		drawMenuBar();
		
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CLINK_CREATE, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CLINK_MOVE_ANCHOR, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.II_PERSPECTIVE_ACTIVATED, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_TAG, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_UNTAG, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_UPDATE_FINISHED, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_UNTAG, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_SET_TITLE, this, CalicoEventHandler.PASSIVE_LISTENER);
		CalicoEventHandler.getInstance().addListener(IntentionalInterfacesNetworkCommands.CIC_TOPOLOGY, this, CalicoEventHandler.PASSIVE_LISTENER);
	}

	public long getId() {
		return uuid;
	}

	public PLayer getLayer(Layer layer) {
		switch (layer) {
		case TOPOLOGY:
			return topologyLayer;
		case CONTENT:
			return contentCanvas.getLayer();
		case TOOLS:
			return toolLayer;
		default:
			throw new IllegalArgumentException("Unknown layer " + layer);
		}
	}

	public JComponent getComponent() {
		return canvas;
	}

	public Point getTranslation() {
		double x = getLayer(Layer.CONTENT).getTransform().getTranslateX();
		double y = getLayer(Layer.CONTENT).getTransform().getTranslateY();
		return new Point((int) x, (int) y);
	}
	
	public Point2D getCenterPoint() {
		return getLayer(Layer.CONTENT).getGlobalFullBounds().getCenter2D();
	}

	public void translate(double x, double y) {
		
		if (IntentionGraph.this.getLayer(Layer.TOPOLOGY).getCameraCount() > 0)
		{
			//guard left
			if (getGlobalCoordinatesForVisibleBounds().getX() - x < defaultBounds.getX())
				x = getGlobalCoordinatesForVisibleBounds().getX() - defaultBounds.getX();

			//guard right
			if (getGlobalCoordinatesForVisibleBounds().getX() + getGlobalCoordinatesForVisibleBounds().getWidth() + x > 
				defaultBounds.getX() + defaultBounds.getWidth()
				&& x < 0)
				x = 0;

			//guard top
			if (getGlobalCoordinatesForVisibleBounds().getY() - y < defaultBounds.getY())
				y = getGlobalCoordinatesForVisibleBounds().getY() - defaultBounds.getY();
//
//			//guard bottom
			if (getGlobalCoordinatesForVisibleBounds().getY() + getGlobalCoordinatesForVisibleBounds().getHeight() + y > 
				defaultBounds.getY() + defaultBounds.getHeight()
				&& y < 0)
				y = 0;
		}
		
		getLayer(Layer.CONTENT).translate(x, y);
		getLayer(Layer.TOPOLOGY).translate(x, y);
		
//		while (IntentionGraph.this.getLayer(Layer.TOPOLOGY).getCameraCount() > 0
//				&& getGlobalCoordinatesForVisibleBounds().getX() + getGlobalCoordinatesForVisibleBounds().getWidth() + x > 
//			defaultBounds.getX() + defaultBounds.getWidth())
//		{
//			getLayer(Layer.CONTENT).translate(-1, 0);
//			getLayer(Layer.TOPOLOGY).translate(-1, 0);
//		}

		if (BubbleMenu.isBubbleMenuActive()) {
			BubbleMenu.clearMenu();
		}
		refreshTopologyTitles();
	}

	public void translateGlobal(double x, double y) {
		final Point2D.Double translation = new Point2D.Double(x, y);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				getLayer(Layer.CONTENT).setGlobalTranslation(translation);
				getLayer(Layer.TOPOLOGY).setGlobalTranslation(translation);

				if (BubbleMenu.isBubbleMenuActive()) {
					BubbleMenu.clearMenu();
				}
			}
		});

	}

	public void setTopology(CIntentionTopology topology) {
		List<PNode> list = new ArrayList<PNode>();
		if (this.topology != null) {
			list = this.topology.getTitles();
			CalicoDraw.removeAllChildrenFromNode(topologyLayer);
			// topologyLayer.removeAllChildren();
		}

		this.topology = topology;

		for (CIntentionTopology.Cluster cluster : this.topology.getClusters()) {
			CalicoDraw.addChildToNode(topologyLayer, cluster);
			// topologyLayer.addChild(cluster);
		}
		
		for (PNode n : list)
			CalicoDraw.removeChildFromNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS), n);

//		repaint();
	}
	
	public void setScaleAboutPoint(double scale, Point2D point) {
//		if (getLayer(IntentionGraph.Layer.CONTENT).getScale() == Double.NaN) {
			getLayer(IntentionGraph.Layer.CONTENT).scaleAboutPoint(scale, point);
//		} else {
//			getLayer(IntentionGraph.Layer.CONTENT).setScale(scale);
//		}

//		if (getLayer(IntentionGraph.Layer.TOPOLOGY).getScale() == Double.NaN) {
			getLayer(IntentionGraph.Layer.TOPOLOGY).scaleAboutPoint(scale, point);
//		} else {
//			getLayer(IntentionGraph.Layer.TOPOLOGY).setScale(scale);
//		}

		if (BubbleMenu.isBubbleMenuActive()) {
			BubbleMenu.clearMenu();
		}
	}
	

	public void setScale(double scale) {
		if (scale < defaultScale)
			scale = defaultScale;
		
		if (getLayer(IntentionGraph.Layer.CONTENT).getScale() == Double.NaN) {
			getLayer(IntentionGraph.Layer.CONTENT).setGlobalScale(scale);
		} else {
			getLayer(IntentionGraph.Layer.CONTENT).setScale(scale);
		}

		if (getLayer(IntentionGraph.Layer.TOPOLOGY).getScale() == Double.NaN) {
			getLayer(IntentionGraph.Layer.TOPOLOGY).setGlobalScale(scale);
		} else {
			getLayer(IntentionGraph.Layer.TOPOLOGY).setScale(scale);
		}

		if (BubbleMenu.isBubbleMenuActive()) {
			BubbleMenu.clearMenu();
		}
//		if (scale > defaultScale)
//		{
//			if (topology != null)
//				
//				topology.hideTitles();
//		}
//		else
//		{
			refreshTopologyTitles();
//		}
	}

	public void setViewTransform(final AffineTransform transform) {
		getLayer(IntentionGraph.Layer.CONTENT).setTransform(transform);
		getLayer(IntentionGraph.Layer.TOPOLOGY).setTransform(transform);

//		if (BubbleMenu.isBubbleMenuActive()) {
//			BubbleMenu.clearMenu();
//		}
	}

	public void activateIconifyMode(boolean b) {
		iconifyMode = b;
	}

	public boolean getIconifyMode() {
		return iconifyMode;
	}

	public void updateZoom() {
//		refreshTopologyTitles();
		if (focus == Focus.CLUSTER && clusterFocus != 0l)
			zoomToCluster(clusterFocus);
		else
			fitContents();
	}
	
	public double getDefaultScale()
	{
		return defaultScale;
	}
	
	public void setFocusToCluster(long cluster) {
		setFocusToCluster(cluster, false);
	}
	
	public void setFocus_primitive(Focus focus, long clusterWithFocus)
	{
		this.focus = focus;
		this.clusterFocus = clusterWithFocus;
	}

	public void setFocusToCluster(long cluster, boolean flagPerspectiveChanged) {
		//remove all CICs not belonging to this cluster
//		if (CalicoPerspective.Active.getCurrentPerspective() instanceof IntentionalInterfacesPerspective &&
//				focus == Focus.CLUSTER && clusterFocus == cluster)
//			return;
		
		focus = Focus.CLUSTER;
		clusterFocus = cluster;
//		if (flagPerspectiveChanged == false)
			updateZoom();
		
		CIntentionCellController.getInstance().hideCellsOutsideOfCluster(cluster);
		CalicoPerspective.Active.getCurrentPerspective().activate();
		
		CalicoDraw.repaint(topology.getCluster(cluster));
//		CalicoDraw.invalidatePaint(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOPOLOGY));
		
		
		topology.getCluster(cluster).activateCluster();
//		if (flagPerspectiveChanged == false)
			setDefaultDimensions();
		drawMenuBar();
//		if (flagPerspectiveChanged)
		/**
		 * This second call is put to cause all perspective change listeners to be fired again.
		 */

//		updateButtons();
	}

	public void setFocusToWall() {
		setFocusToWall(false);
	}
	
	public void setFocusToWall(boolean flagPerspectiveChanged) {
//		if (focus == Focus.WALL)
//			return;
		
		CIntentionCellController.getInstance().showAllCells();
		
		if (topology.getCluster(this.getClusterInFocus()) != null)
			topology.getCluster(this.getClusterInFocus()).deactivateCluster();
		focus = Focus.WALL;
		updateZoom();
		drawMenuBar();
		
		if (flagPerspectiveChanged)
		{
			CalicoPerspective.Active.getCurrentPerspective().activate();
			CalicoDraw.repaint(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS));
		}
		
		setDefaultDimensions();
//		updateButtons();
	}
	
	private void updateButtons() {
		if (this.newCanvasButton != null)
			CalicoDraw.removeChildFromNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT), this.newCanvasButton);
		
		if (IntentionGraph.getInstance().getFocus() != IntentionGraph.Focus.WALL)
		{
			this.newCanvasButton = new NewClusterCanvasButton();
			int defaultDimensions = calico.CalicoOptions.menu.icon_size;
			PBounds regions = calico.plugins.iip.components.graph.IntentionGraph.getInstance().getClusterBounds(getClusterInFocus());
			this.newCanvasButton.setBounds(regions.x + 1, regions.y + 1, defaultDimensions, defaultDimensions);
			CalicoDraw.addChildToNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.CONTENT), this.newCanvasButton);
		}
	}
	
	public Focus getFocus()
	{
		return focus;
	}
	
	public long getClusterInFocus()
	{
		if (focus == Focus.CLUSTER && clusterFocus > 0l)
			return CIntentionCellController.getInstance().getClusterRootCanvasId(clusterFocus);
		
		return 0l;
	}

	private void fitContents() {
		for (PNode n : boundBorders) {
			CalicoDraw.removeChildFromNode(
					getLayer(IntentionGraph.Layer.CONTENT), n);
		}
		boundBorders.clear();

		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		int visibleCount = 0;
		PLayer layer = IntentionGraph.getInstance().getLayer(
				IntentionGraph.Layer.CONTENT);
		// for (PNode node : (Iterable<PNode>) layer.getChildrenReference())
		// {
		// if (node.getVisible())
		// {
		// visibleCount++;
		//
		// PBounds bounds = node.getBounds();
		// if (bounds.x < minX)
		// {
		// minX = bounds.x;
		// }
		// if (bounds.y < minY)
		// {
		// minY = bounds.y;
		// }
		// if ((bounds.x + bounds.width) > maxX)
		// {
		// maxX = bounds.x + bounds.width;
		// }
		// if ((bounds.y + bounds.height) > maxY)
		// {
		// maxY = bounds.y + bounds.height;
		// }
		// }
		// }

		PClip temp;
		layer = IntentionGraph.getInstance().getLayer(
				IntentionGraph.Layer.TOPOLOGY);
		for (PNode node : (Iterable<PNode>) layer.getChildrenReference()) {
			if (node.getVisible()) {
				visibleCount++;

				// PBounds bounds = new
				// PBounds(node.localToGlobal(node.getBounds()));
				PBounds bounds = node.getBounds();
				if (bounds.x < minX) {
					minX = bounds.x;
				}
				if (bounds.y < minY) {
					minY = bounds.y;
				}
				if ((bounds.x + bounds.width) > maxX) {
					maxX = bounds.x + bounds.width;
				}
				if ((bounds.y + bounds.height) > maxY) {
					maxY = bounds.y + bounds.height;
				}
//				temp = new PClip();
//				temp.setPathTo(node.getBounds());
//				temp.setStrokePaint(Color.red);
//				temp.setBounds(node.getBounds());
//				boundBorders.add(temp);
			}
		}
		for (PNode n : boundBorders) {
			CalicoDraw
					.addChildToNode(getLayer(IntentionGraph.Layer.CONTENT), n);
		}

		if (visibleCount < 2) {
			PLayer content = IntentionGraph.getInstance().getLayer(
					IntentionGraph.Layer.CONTENT);
			if (content.getChildrenCount() == 1)
				zoomToRegion(content.getChild(0).getBounds());
			// setViewTransform(new AffineTransform());
			// translate(minX, minY);
			// repaint();
		} else {
			zoomToRegion(new PBounds(minX, minY, (maxX - minX), (maxY - minY)));
		}
	}

	private void zoomToCell(long cellId) {
		setScale(1.0);

		CIntentionCell cell = CIntentionCellController.getInstance()
				.getCellById(cellId);
		Point2D center = cell.getCenter();
		Dimension canvasSize = contentCanvas.getBounds().getSize();
		translateGlobal((canvasSize.width / 2.0) - center.getX(),
				(canvasSize.height / 2.0) - center.getY());
	}

	private void zoomToCluster(long memberCanvasId) {
		long clusterRootCanvasId = CIntentionCellController.getInstance()
				.getClusterRootCanvasId(memberCanvasId);
		CIntentionTopology.Cluster cluster = topology
				.getCluster(clusterRootCanvasId);
		if (cluster == null)
			return;
		PBounds maxRingBounds = cluster.getVisualBoxBounds();// cluster.getMaxRingBounds();
		if (maxRingBounds == null) {
			return; // no zooming on atomic clusters
		}

		double margin = maxRingBounds.width * 0.03;
		maxRingBounds.x -= margin;
		maxRingBounds.y -= margin;
		maxRingBounds.width += (2 * margin);
		maxRingBounds.height += (2 * margin);
		zoomToRegion(maxRingBounds);
	}
	
	public void zoomToInnerRing()
	{
		// 1) get all canvases that are children of the root node
//		long activeCanvasId = CIntentionCellController.getInstance().getCellById(CIntentionCellInputHandler.getInstance().getActiveCell()).getCanvasId();
//		long rootCanvasId = CIntentionCellController.getInstance().getClusterRootCanvasId(activeCanvasId);
		long rootCanvasId = IntentionGraph.getInstance().getClusterInFocus();
		
		Long[] rootAnchors = new Long[1];
		//all anchors attached to the root will lead us to elements in the first ring
		rootAnchors = CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(rootCanvasId).toArray(rootAnchors);
		if (rootAnchors.length == 0)
			return;
		
		// 2) get their bounds
		Point2D.Double min = new Point2D.Double(Double.MAX_VALUE, Double.MAX_VALUE), max = new Point2D.Double(Double.MIN_VALUE,Double.MIN_VALUE);
		for (Long l : rootAnchors)
		{
			long canvasId = CCanvasLinkController.getInstance().getAnchor(l.longValue()).getLink().getAnchorB().getCanvasId();
			if (CIntentionCellController.getInstance().getCellByCanvasId(canvasId).getIsPinned())
				continue;
			PBounds bounds = CIntentionCellController.getInstance().getCellByCanvasId(canvasId).getBounds();
			updateBoundPoints(bounds, min, max);
		}

		// 3) zoom to those bounds
		if (getFocus() != Focus.CLUSTER)
			IntentionGraph.getInstance().setFocusToCluster(rootCanvasId);
		IntentionGraph.getInstance().zoomToRegion(new PBounds(min.x, min.y, max.x - min.x, max.y - min.y));
	}
	
	public void zoomToSingleChildBranch()
	{
		// 1) get all canvases that are in branch
		// simplest way is to traverse all the way to the leaf, then back up to the root
		long activeCanvasId = CIntentionCellController.getInstance().getCellById(CIntentionCellInputHandler.getInstance().getActiveCell()).getCanvasId();
		long rootCanvasId = CIntentionCellController.getInstance().getClusterRootCanvasId(activeCanvasId);
		
		//get leaf
		long[] children;
		long child = activeCanvasId;
		do 
		{
			children = CIntentionCellController.getInstance().getCIntentionCellChildren(child);
			if (children.length > 0)
				child = children[0];
		} while (children.length > 0);
		long leaf = child;
		
		// 2) go up the branch and get bounds of all nodes
		Point2D.Double min = new Point2D.Double(Double.MAX_VALUE, Double.MAX_VALUE), max = new Point2D.Double(Double.MIN_VALUE,Double.MIN_VALUE);
		
		long traversingCanvas = leaf;
		while (!CIntentionCellController.getInstance().isRootCanvas(traversingCanvas))
		{
			PBounds bounds = CIntentionCellController.getInstance().getCellByCanvasId(traversingCanvas).getBounds();
			updateBoundPoints(bounds, min, max);
			traversingCanvas = CIntentionCellController.getInstance().getCIntentionCellParent(traversingCanvas);
		}
		
		// 3) zoom to those bounds
		if (getFocus() != Focus.CLUSTER)
			IntentionGraph.getInstance().setFocusToCluster(rootCanvasId);
		IntentionGraph.getInstance().zoomToRegion(new PBounds(min.x, min.y, max.x - min.x, max.y - min.y));
	}
	
	/**
	 * Updates min and max to include boundaries of bounds
	 * @param bounds
	 * @param min This value will be changed
	 * @param max This value will be changed
	 */
	private void updateBoundPoints(PBounds bounds,
			java.awt.geom.Point2D.Double min, java.awt.geom.Point2D.Double max) {
		if (bounds.x < min.x)
			min.x = bounds.x;
		if (bounds.y < min.y)
			min.y = bounds.y;
		if (bounds.x + bounds.width > max.x)
			max.x = bounds.x + bounds.width;
		if (bounds.y + bounds.height > max.y)
			max.y = bounds.y + bounds.height;
	}
	
	public void initializeZoom(CalicoPacket p)
	{
		p.rewind();
		int comm = p.getInt();
		if (comm != IntentionalInterfacesNetworkCommands.WALL_BOUNDS)
			return;
		
		int x = p.getInt();
		int y = p.getInt();
		int width = p.getInt();
		int height = p.getInt();
		
		zoomToRegion(new PBounds(x,y,width,height));
	}

	private void zoomToRegion(final PBounds bounds) {
		this.zoomRegions = bounds;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				final double ZOOM_ADJUSTMENT = .9;

				final PBounds viewBounds = new PBounds(contentCanvas
						.getVisibleRect());
				final PAffineTransform newTransform = new PAffineTransform();

				double xRatio = viewBounds.getWidth() / bounds.getWidth();
				double yRatio = viewBounds.getHeight() / bounds.getHeight();
				final double s = Math.min(xRatio, yRatio) * ZOOM_ADJUSTMENT;
				newTransform.scale(s, s);

				newTransform.translate(bounds.getX() * -1, bounds.getY() * -1);
				double xMargin = 0;
				double yMargin = 0;

				/**
				 * The below formulas for xMargin and yMargin look like black
				 * magic, but make sense if you draw out the proportions as
				 * boxes.
				 */
				// if (xRatio > yRatio)
				xMargin = (viewBounds.getWidth() / (2 * s) - bounds.getWidth() / 2);
				// else
				yMargin = (viewBounds.getHeight() / (2 * s) - bounds
						.getHeight() / 2);
				newTransform.translate(xMargin, yMargin);
				setViewTransform(newTransform);
				
//				getLayer(Layer.TOPOLOGY).repaint();
//				getLayer(Layer.CONTENT).repaint();
				CalicoDraw.repaint(getLayer(Layer.TOPOLOGY));
				CalicoDraw.repaint(getLayer(Layer.CONTENT));
				refreshTopologyTitles();
				CalicoDraw.repaint(menuBar);
				SwingUtilities.invokeLater(
						new Runnable() { public void run() { 
							if (CIntentionCellController.getInstance().getCellById(
									CIntentionCellInputHandler.getInstance().getActiveCell()) != null)
								BubbleMenu.moveIconPositions(
										CIntentionCellController.getInstance().getCellById(
												CIntentionCellInputHandler.getInstance().getActiveCell()).getBounds());
						}});

			}


		});
	}
	
	private void setDefaultDimensions()
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					IntentionGraph.this.defaultScale = IntentionGraph.this.getLayer(Layer.CONTENT).getScale();
					Cluster c = topology.getCluster(getClusterInFocus());
					if (c != null)
					{
						IntentionGraph.this.defaultBounds = c.getVisualBoxBounds().getBounds2D(); 
					}
//					IntentionGraph.this.defaultBounds = getGlobalCoordinatesForVisibleBounds();
//					IntentionGraph.this.defaultBounds = IntentionGraph.this.getLayer(Layer.TOPOLOGY).getCamera(0).getViewBounds();
//					System.out.println("Default bounds are: " + IntentionGraph.this.defaultBounds.toString());
//					System.out.println("Topology box bounds are: " + IntentionGraph.getInstance().topology.getCluster(getClusterInFocus()).getBounds().toString());
				}});

	}

	public void initialize() {
		if (menuBar != null)
			menuBar.initialize();
	}
	
	public Rectangle2D getGlobalCoordinatesForVisibleBounds() {
		if (IntentionGraph.this.getLayer(Layer.TOPOLOGY).getCameraCount() > 0)
			return IntentionGraph.this.getLayer(Layer.TOPOLOGY).globalToLocal(
					IntentionGraph.this.getLayer(Layer.TOPOLOGY).getCamera(0).getGlobalBounds());
		else return new Rectangle2D.Double();
	}

	public void repaint() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				canvas.repaint();
				contentCanvas.repaint();
			}
		});
	}

	public Rectangle getBounds() {
		return canvas.getBounds();
	}

	public Rectangle getLocalBounds(Layer layer) {
		Rectangle globalBounds = canvas.getBounds();
		Point2D localPoint = getLayer(layer).globalToLocal(
				globalBounds.getLocation());
		Dimension2D localSize = getLayer(layer).globalToLocal(
				globalBounds.getSize());
		return new Rectangle((int) localPoint.getX(), (int) localPoint.getY(),
				(int) localSize.getWidth(), (int) localSize.getHeight());
	}

	public void setBounds(int x, int y, int w, int h) {
		// CalicoDraw.setNodeBounds(canvas, x, y, w, h);
		canvas.setBounds(x, y, w, h);
	}
	
	private void drawMenuBar() {
		if (menuBar != null) {
			canvas.getCamera().removeChild(menuBar);
		}

		menuBar = new IntentionGraphMenuBar(CanvasMenuBar.POSITION_BOTTOM);
		CalicoDraw.addChildToNode(canvas.getCamera(), menuBar);
		// canvas.getCamera().addChild(menuBar);

		contentCanvas
				.setBounds(0, 0, CalicoDataStore.ScreenWidth,
						(int) (CalicoDataStore.ScreenHeight - menuBar
								.getBounds().height));
	}

	public boolean processToolEvent(InputEventInfo event) {
		if (menuBar.isPointInside(event.getGlobalPoint())) {
			menuBar.processEvent(event);
			return true;
		}
		return false;
	}

	public void addMouseListener(MouseListener listener) {
		canvas.addMouseListener(listener);
	}

	public void addMouseMotionListener(MouseMotionListener listener) {
		canvas.addMouseMotionListener(listener);
	}

	public void removeMouseListener(MouseListener listener) {
		canvas.removeMouseListener(listener);
	}

	public void removeMouseMotionListener(MouseMotionListener listener) {
		canvas.removeMouseMotionListener(listener);
	}

	/**
	 * Wrap the Piccolo canvas to gain access to one protected method.
	 * 
	 * @author Byron Hawkins
	 */
	private class ContainedCanvas extends PCanvas {
		public ContainedCanvas() {
			super.removeInputSources();
		}
	}

	public long getClusterAt(Point2D p) {
		CIntentionTopology.Cluster cluster = topology.getClusterAt(p);
		if (cluster != null)
			return cluster.getRootCanvasId();

		return 0l;
	}
	
	public PBounds getClusterBounds(long rootCanvasId)
	{
		if (!CIntentionCellController.getInstance().isRootCanvas(rootCanvasId))
			return null;
		return topology.getCluster(rootCanvasId).getBounds();
	}
	
	public boolean clusterHasChildren(long clusterCanvasRootId)
	{
		if (!isClusterRoot(clusterCanvasRootId))
			return false;
		
		
		
		return topology.getCluster(clusterCanvasRootId).hasChildren();

	}
	
	/**
	 * Returns true if the ring contains {@link Point} p. Returns false if the point is not contained or the ring level doesn't exist.
	 * @param p The point passed (is not changed) in global coordinates.
	 * @param ringLevel The ring level. The first ring level is zero.
	 * @return
	 */
	public boolean ringContainsPoint(long clusterCanvasRootId, Point p, int ringLevel)
	{
		if (!isClusterRoot(clusterCanvasRootId))
			return false;
		
		return topology.getCluster(clusterCanvasRootId).ringContainsPoint(p, ringLevel);		
	}
	
	public long[] getRootsOfAllClusters()
	{
		Collection<Cluster> clusters = topology.getClusters();
		
		long[] ret = new long[clusters.size()];
		
		java.util.Iterator<Cluster> cit =  clusters.iterator();
		int counter = 0;
		
		while (cit.hasNext())
		{
			ret[counter] = cit.next().getRootCanvasId();
			counter++;
		}
		
		return ret;	
	}
	
	public boolean isClusterRoot(long id)
	{
		long[] clusterRoots = getRootsOfAllClusters();
		
		for (int i = 0; i < clusterRoots.length; i++)
			if (clusterRoots[i] == id)
				return true;
		
		return false;
	}
	
	public int getClusterIndex(long clusterId)
	{
		Collection<Cluster> clusters = topology.getClusters();
		java.util.Iterator<Cluster> cit =  clusters.iterator();
		int counter = 0;
		
		while (cit.hasNext())
		{
			if (cit.next().getRootCanvasId() == clusterId)
				return counter;
			counter++;
		}
		
		return -1;			
	}
	
	public void removeExtraCluster(long emptyClusterToIgnore) {
		long[] clusterRoots = IntentionGraph.getInstance().getRootsOfAllClusters();
		if (clusterRoots.length < 6)
			return;
		
		for (int i = 0; i < clusterRoots.length; i++)
		{
			if (clusterRoots[i] != emptyClusterToIgnore
					&& CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(clusterRoots[i]).size() == 0)
			{
				CIntentionCellController.getInstance().deleteCanvas(clusterRoots[i]);
				break;
			}
		}
	}
	
	/**
	 * Returns the number of children on the first ring of a cluster
	 * @param clusterCanvasRootId
	 * @return
	 */
	public int getNumBaseClusterChildren(long clusterCanvasRootId)
	{
		if (!isClusterRoot(clusterCanvasRootId))
			return 0;
		
		int numRootLinks = CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(clusterCanvasRootId).size();
		return numRootLinks;
	}
	
	public void deleteCanvasAndRemoveExtraClusters(long canvasId) {
		//If we are deleting the last child in a cluster, remove that cluster by deleting the root canvas node
		
		long rootCanvasId = CIntentionCellController.getInstance().getClusterRootCanvasId(canvasId);
		int numRootLinks = CCanvasLinkController.getInstance().getAnchorIdsByCanvasId(rootCanvasId).size();
		
		CIntentionCellController.getInstance().deleteCanvas(canvasId);
		
		
		if (numRootLinks == 1)
		{
			//we want to remove the extra cluster, but let this one remain
			IntentionGraph.getInstance().removeExtraCluster(rootCanvasId);
		}
	}
	
	public void createClusterIfNoEmptyClusterExists(long clusterRoot) {
		//Check if we need to create a new cluster
		boolean emptyClusterExists = false;
		long[] clusterRoots = IntentionGraph.getInstance().getRootsOfAllClusters();
		for (int i = 0; i < clusterRoots.length; i++)
		{
			if (!IntentionGraph.getInstance().clusterHasChildren(clusterRoots[i])
					&& clusterRoots[i] != clusterRoot)
				emptyClusterExists = true;
		}
		if (!emptyClusterExists)
			CIntentionCellFactory.getInstance().createNewCell();
	}
	
	private void refreshTopologyTitles()
	{
		if (topology == null)
			return;
		
		Collection<Cluster> clusters = topology.getClusters();
		
		java.util.Iterator<Cluster> cit =  clusters.iterator();
		
		while (cit.hasNext())
		{
			cit.next().updateTitleText();
		}
		topology.updateTitlesVisibility(CalicoPerspective.Active.getCurrentPerspective());
		CanvasTitlePanel.getInstance().refresh();
		CalicoDraw.repaint(topologyLayer);
	}
	
	/**
	 * @param p Point in global coordinates
	 * @return The canvasId of cluster whose title contains point p. Returns 0l if no cluster title found.
	 */
	public long getClusterWithTitleTextAtPoint(Point p)
	{
		Collection<Cluster> clusters = topology.getClusters();
		
		java.util.Iterator<Cluster> cit =  clusters.iterator();
		
		while (cit.hasNext())
		{
			Cluster c = cit.next();
			if (c.clusterTitleTextContainsPoint(p))
				return c.getRootCanvasId();
		}
		
		return 0l;
	}
	
	/**
	 * @param p Point in global coordinates
	 * @return The canvasId of cluster whose title contains point p. Returns 0l if no cluster title found.
	 */
	public long getClusterWithWallTextAtPoint(Point p)
	{
		Collection<Cluster> clusters = topology.getClusters();
		
		java.util.Iterator<Cluster> cit =  clusters.iterator();
		
		while (cit.hasNext())
		{
			Cluster c = cit.next();
			if (c.clusterWallTextContainsPoint(p))
				return c.getRootCanvasId();
		}
		
		return 0l;
	}
	
	@Override
	public void handleCalicoEvent(int event, CalicoPacket p) {
		
		if (event == IntentionalInterfacesNetworkCommands.CLINK_CREATE
				|| event == IntentionalInterfacesNetworkCommands.CLINK_MOVE_ANCHOR
				|| event == IntentionalInterfacesNetworkCommands.CIC_TAG
				|| event == IntentionalInterfacesNetworkCommands.CIC_UNTAG
				|| event == IntentionalInterfacesNetworkCommands.CIC_UPDATE_FINISHED
				|| event == IntentionalInterfacesNetworkCommands.CIC_UNTAG
				|| event == IntentionalInterfacesNetworkCommands.CIC_SET_TITLE
				|| event == IntentionalInterfacesNetworkCommands.CIC_TOPOLOGY)
		{
			refreshTopologyTitles();
			
			
			
			long[] arrowKeySet = IntentionGraphController.getInstance().getArrowLinkKeySet();
			for (int i = 0; i < arrowKeySet.length; i++)
			{
				if (IntentionGraphController.getInstance().getArrowByLinkId(arrowKeySet[i]) instanceof CCanvasLinkArrow)
					CalicoDraw.moveNodeToFront(IntentionGraphController.getInstance().getArrowByLinkId(arrowKeySet[i]));
			}
			
			
		}		
	}
	
	/**
	 * Only intended to be accessed by input handlers.
	 * @param clusterRootId
	 * @param handler
	 */
	public CIntentionTopology.Cluster getClusterInFocus(CalicoAbstractInputHandler handler)
	{
		if (handler == null || getFocus() != Focus.CLUSTER)
			return null;
		
		return topology.getCluster(clusterFocus);
	}
	
	public long createNewClusterCanvas() {
		long clusterRoot = IntentionGraph.getInstance().getClusterInFocus();

		long newCanvasId = CIntentionCellFactory.getInstance()
				.createNewCell(CCanvasController.getCurrentUUID(), CanvasInputProximity.forPosition(getBounds().getX())).getCanvasId();
		
		CCanvasLinkController.getInstance().createLink(clusterRoot /*CIntentionCellController.getInstance().getClusterRootCanvasId(currentCell)*/, newCanvasId);
		
		IntentionGraph.getInstance().createClusterIfNoEmptyClusterExists(clusterRoot);
		
		return newCanvasId;
	}

	public double getScale() {
		return IntentionGraph.this.getLayer(Layer.CONTENT).getScale();
	}
}
