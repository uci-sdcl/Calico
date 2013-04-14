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
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.SwingUtilities;

import calico.CalicoDraw;
import calico.events.CalicoEventListener;
import calico.inputhandlers.CalicoInputManager;
import calico.networking.netstuff.CalicoPacket;
import calico.perspectives.CalicoPerspective;
import calico.perspectives.CalicoPerspective.PerspectiveChangeListener;
import calico.plugins.iip.IntentionalInterfacesNetworkCommands;
import calico.plugins.iip.components.canvas.CanvasTitlePanel;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.iconsets.CalicoIconManager;
import calico.plugins.iip.perspectives.IntentionalInterfacesPerspective;

import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolox.nodes.PClip;
import edu.umd.cs.piccolox.nodes.PComposite;

/**
 * Represents the intention topology of the Intention View. It draws each <code>CIntentionRing</code> from the server's
 * intention layout, along with a bounding box around each cluster.
 * 
 * @author Byron Hawkins
 */
public class CIntentionTopology implements PerspectiveChangeListener
{
	private static final Color RING_COLOR = new Color(0x8b, 0x89, 0x89);
	private static final Color BOUNDING_BOX_COLOR = Color.black;//new Color(0x8b, 0x89, 0x89);
	
	private static PImage canvasCreate = 
			new PImage(CalicoIconManager.getIconImage(
					"intention.clusterview.canvas-create"));
	
	private Cluster activeCluster = null;

	/**
	 * Represents one cluster in the Piccolo component hierarchy of the IntentionView. It is constructed from the
	 * topology that was serialized on the server, taking one cluster out of the serialized topology data and inflating
	 * it into a set of <code>PPath</code>s for the rings and a last <code>PPath</code> for the bounding box.
	 * 
	 * @author Byron Hawkins
	 */
	public class Cluster extends PComposite
	{
		private final long rootCanvasId;
		private final List<PPath> rings = new ArrayList<PPath>();
		private final PClip box;
		private final PClip outerBox;
		private final PText wallTitle;
		private final PText clusterTitle;
				

//		buffer.append(rootCanvasId);
//		buffer.append("[");
//		buffer.append(center.x);
//		buffer.append(",");
//		buffer.append(center.y);
//		buffer.append(",");
//		buffer.append(boundingBox.x);
//		buffer.append(",");
//		buffer.append(boundingBox.y);
//		buffer.append(",");
//		buffer.append(boundingBox.width);
//		buffer.append(",");
//		buffer.append(boundingBox.height);
//		buffer.append(",");			
//		buffer.append(outerBox.x);
//		buffer.append(",");
//		buffer.append(outerBox.y);
//		buffer.append(",");
//		buffer.append(outerBox.width);
//		buffer.append(",");
//		buffer.append(outerBox.height);			
//		buffer.append(":");
		
		Cluster(String serialized)
		{		
			StringTokenizer tokens = new StringTokenizer(serialized, "[],:");
			rootCanvasId = Long.parseLong(tokens.nextToken());
			int x = Integer.parseInt(tokens.nextToken());
			int y = Integer.parseInt(tokens.nextToken());

			setX(x);
			setY(y);

			int xBox = Integer.parseInt(tokens.nextToken());
			int yBox = Integer.parseInt(tokens.nextToken());
			int wBox = Integer.parseInt(tokens.nextToken());
			int hBox = Integer.parseInt(tokens.nextToken());
			box = new PClip();
			box.setPathToRectangle(xBox, yBox, wBox, hBox);
			box.setStrokePaint(BOUNDING_BOX_COLOR);
			
			
			int xOuterBox = Integer.parseInt(tokens.nextToken());
			int yOuterBox = Integer.parseInt(tokens.nextToken());
			int wOuterBox = Integer.parseInt(tokens.nextToken());
			int hOuterBox = Integer.parseInt(tokens.nextToken());		
			
			outerBox = new PClip();
			outerBox.setPathToRectangle(xOuterBox, yOuterBox, wOuterBox, hOuterBox);
			outerBox.setStrokePaint(BOUNDING_BOX_COLOR);		
			outerBox.setPaint(Color.white);
			outerBox.setBounds(xOuterBox, yOuterBox, wOuterBox, hOuterBox);
			

			
			
			
//			addChild(box);
//			CalicoDraw.addChildToNode(this, box);
			CalicoDraw.addChildToNode(this, outerBox);
			CalicoDraw.setNodeBounds(this, xOuterBox, yOuterBox, wOuterBox, hOuterBox);


			while (tokens.hasMoreTokens())
			{
				int radius = Integer.parseInt(tokens.nextToken());
				PPath ring = PPath.createEllipse((float) (outerBox.getBounds().getCenterX() - radius), 
						(float) (outerBox.getBounds().getCenterY() - radius), radius * 2, radius * 2);
				ring.setStrokePaint(RING_COLOR);
				rings.add(ring);
			}

			for (int i = (rings.size() - 1); i >= 0; i--)
			{
				CalicoDraw.addChildToNode(outerBox, rings.get(i));
//				box.addChild(rings.get(i));
			}
			
			PBounds localBounds = new PBounds(outerBox.getBounds());
			Rectangle2D globalBounds = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOPOLOGY).localToGlobal(localBounds);
			
			wallTitle = new PText("Wall > ");
			wallTitle.setOffset(globalBounds.getX() + 20, globalBounds.getY() + 20);
			Font font = new Font ("Helvetica", Font.PLAIN , 20);
			wallTitle.setFont(font);
			wallTitle.recomputeLayout();
			
			clusterTitle = new PText("Unnamed cluster");
//			clusterTitle.setOffset(globalBounds.getX() + 20 + wallTitle.getGlobalBounds().getWidth(), globalBounds.getY() + 20);
			clusterTitle.setOffset(globalBounds.getX() + 20, globalBounds.getY() + 20);
			clusterTitle.setWidth(outerBox.getGlobalBounds().getWidth());
			clusterTitle.setConstrainWidthToTextWidth(false);
//			Font font = new Font ("Helvetica", Font.PLAIN , 30);
			clusterTitle.setFont(font);
			clusterTitle.recomputeLayout();
			clusterTitle.setVisible(false);
			
			CalicoDraw.addChildToNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS), clusterTitle);
//			CalicoDraw.addChildToNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS), wallTitle);
			


			
		}
		
		public boolean clusterTitleTextContainsPoint(Point p)
		{
			return clusterTitle.getGlobalFullBounds().contains(p);
		}
		
		public boolean clusterWallTextContainsPoint(Point p)
		{
			return wallTitle.getGlobalFullBounds().contains(p);
		}
		
		public void updateTitleText()
		{
//			SwingUtilities.invokeLater(
//					new Runnable() { public void run() { 
//						PBounds localBounds = new PBounds(outerBox.getBounds());
//						Rectangle2D globalBounds = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOPOLOGY).localToGlobal(localBounds);
//						wallTitle.setOffset(globalBounds.getX() + 20, globalBounds.getY() + 20);
//						clusterTitle.setOffset(globalBounds.getX() + 20 + wallTitle.getGlobalBounds().getWidth(), globalBounds.getY() + 20);
//						
//						wallTitle.setOffset(globalBounds.getX() + 20, globalBounds.getY() + 20);
//						clusterTitle.setWidth(outerBox.getGlobalBounds().getWidth() - wallTitle.getGlobalBounds().getWidth());
//						
//						wallTitle.recomputeLayout();
//						clusterTitle.recomputeLayout();
//					}});
			SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
//						Font font;
//						if (IntentionGraph.getInstance().getFocus() == IntentionGraph.Focus.CLUSTER)
//						{
//							font = new Font ("Helvetica", Font.PLAIN , (int)(40)); 
//						}
//						else
//						{
//							font = new Font ("Helvetica", Font.PLAIN , (int)(70));
//						}
//						wallTitle.setFont(font);
//						wallTitle.recomputeLayout();
						
						if (clusterTitle == null || CIntentionCellController.getInstance().getCellByCanvasId(rootCanvasId) == null)
							return;
						
						if (showTitle(CalicoPerspective.Active.getCurrentPerspective(), Cluster.this))
							clusterTitle.setVisible(true);
						else
							clusterTitle.setVisible(false);
						
						PBounds localBounds = new PBounds(outerBox.getBounds());
						Rectangle2D globalBounds = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOPOLOGY).localToGlobal(localBounds);
						globalBounds = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS).globalToLocal(globalBounds);
						wallTitle.setOffset(globalBounds.getX() + 5, globalBounds.getY() + 5);
						clusterTitle.setOffset(globalBounds.getX() + 5, globalBounds.getY() + 2);
						
//						wallTitle.setOffset(globalBounds.getX() + 5, globalBounds.getY() + 5);
						clusterTitle.setWidth(outerBox.getGlobalBounds().getWidth());
						
						wallTitle.recomputeLayout();
						clusterTitle.recomputeLayout();
						
//						clusterTitle.setFont(font);
						clusterTitle.setText(CIntentionCellController.getInstance().getCellByCanvasId(rootCanvasId).getTitleWithoutPrefix());
//						clusterTitle.setOffset(outerBox.getX() + 20 + wallTitle.getBounds().getWidth(), outerBox.getY() + 15);
//						clusterTitle.setWidth(outerBox.getBounds().getWidth() - wallTitle.getBounds().getWidth());
						clusterTitle.recomputeLayout();
						clusterTitle.setBounds(clusterTitle.getBounds());
						
						if (IntentionGraph.getInstance().getFocus() == IntentionGraph.Focus.CLUSTER
								&& IntentionGraph.getInstance().getClusterInFocus() == rootCanvasId)
							layoutCreateCanvas();
						
					}});

			
			
//			clusterTitle.repaint();
		}

		public PBounds getMaxRingBounds()
		{
			if (rings.isEmpty())
			{
				return null;
			}

			double span = rings.get(rings.size() - 1).getWidth();
			return new PBounds(getX() - (span / 2.0), getY() - (span / 2.0), span, span);
		}
		
		/**
		 * Returns true if the ring contains {@link Point} p. Returns false if the point is not contained or the ring level doesn't exist.
		 * @param p The point passed (is not changed) in global coordinates.
		 * @param ringLevel The ring level. The first ring level is zero.
		 * @return
		 */
		public boolean ringContainsPoint(Point p, int ringLevel)
		{
			if (rings.size() <= ringLevel)
				return false;
			
			Point2D local = outerBox.globalToLocal(new Point(p));
			
			return ((ArrayList<PPath>)rings).get(ringLevel).getPathReference().contains(local);
		}
		
		public PBounds getVisualBoxBounds()
		{
			
			return outerBox.getBounds();
		}
		
		/**
		 * Returns 
		 * @param p The point in global coordinates
		 * @return Returns true if the canvasCreate object contains Point p
		 */
		public boolean createCanvasIconContainsPoint(Point p)
		{
			return canvasCreate.getBoundsReference().contains(p);
		}
		
		public long getRootCanvasId()
		{
			return rootCanvasId;
		}
		
		public boolean hasChildren()
		{
			return rings.size() > 0;
		}
		
		public void activateCluster()
		{
			SwingUtilities.invokeLater(
					new Runnable() { public void run() { 
						if (activeCluster != null &&
								activeCluster.getRootCanvasId() == getRootCanvasId())
							return;
						
						if (activeCluster != null)
							activeCluster.deactivateCluster();
						
						layoutCreateCanvas();
						
						CanvasTitlePanel.getInstance().refresh();
						CalicoDraw.repaint(canvasCreate);
					}

					
					});

		}
		
		public void deactivateCluster()
		{
			canvasCreate.getParent().removeChild(canvasCreate);
		}
		
		private void layoutCreateCanvas() {
			PBounds localBounds = new PBounds(outerBox.getBounds());
			Rectangle2D globalBounds = IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOPOLOGY).localToGlobal(localBounds);
			
			canvasCreate.setBounds(globalBounds.getX() + globalBounds.getWidth() - canvasCreate.getWidth() - 20,
					globalBounds.getY()+10, canvasCreate.getWidth(), canvasCreate.getHeight());
			CalicoDraw.addChildToNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS), canvasCreate);
		}
	}
	
	

	private final Map<Long, Cluster> clusters = new HashMap<Long, Cluster>();

	public CIntentionTopology(String serialized)
	{
		StringTokenizer tokens = new StringTokenizer(serialized, "C");
		while (tokens.hasMoreTokens())
		{
			Cluster cluster = new Cluster(tokens.nextToken());
			clusters.put(cluster.rootCanvasId, cluster);
		}
		CalicoPerspective.addListener(this);
	}

	public void clear()
	{
		clusters.clear();
	}

	public Collection<Cluster> getClusters()
	{
		return clusters.values();
	}

	public Cluster getCluster(long rootCanvasId)
	{
		return clusters.get(rootCanvasId);
	}
	
	public Cluster getClusterAt(Point2D p)
	{
		for (Cluster c : clusters.values())
		{
			if (c.getBounds().contains(p))
				return c;
		}
		return null;
	}

	@Override
	public void perspectiveChanged(CalicoPerspective perspective) {
		updateTitlesVisibility(perspective);
	}

	public void updateTitlesVisibility(CalicoPerspective perspective) {
		if (perspective instanceof IntentionalInterfacesPerspective
				&& IntentionGraph.getInstance().getFocus() == IntentionGraph.Focus.CLUSTER)
		{
			long clusterInFocus = IntentionGraph.getInstance().getClusterInFocus();
			for (Cluster c : clusters.values())
			{
				
				if (c.rootCanvasId == clusterInFocus)
				{
//					c.wallTitle.is
					if (c.wallTitle.getVisible())
						CalicoDraw.setVisible(c.wallTitle, false);
					if (c.clusterTitle.getVisible())
						CalicoDraw.setVisible(c.clusterTitle, false);
				}
				else
				{
					if (!c.wallTitle.getVisible())
						CalicoDraw.setVisible(c.wallTitle, true);
					if (!c.clusterTitle.getVisible())
						CalicoDraw.setVisible(c.clusterTitle, true);					
				}
			}
			
		}
		else if (perspective instanceof IntentionalInterfacesPerspective)
		{
			for (Cluster c : clusters.values())
			{
				if (!c.wallTitle.getVisible())
					CalicoDraw.setVisible(c.wallTitle, true);
				if (!c.clusterTitle.getVisible())
					CalicoDraw.setVisible(c.clusterTitle, true);
			}
			
		}
	}
	
	public void hideTitles()
	{
		for (Cluster c : clusters.values())
		{
			if (!c.wallTitle.getVisible())
			{
				CalicoDraw.setVisible(c.wallTitle, false);
				CalicoDraw.repaint(c.wallTitle);
			}
			if (!c.clusterTitle.getVisible())
			{
				CalicoDraw.setVisible(c.clusterTitle, false);
				CalicoDraw.repaint(c.clusterTitle);
			}
		}
	}
	
	private boolean showTitle(CalicoPerspective perspective, Cluster c)
	{
		if (perspective instanceof IntentionalInterfacesPerspective
				&& IntentionGraph.getInstance().getFocus() == IntentionGraph.Focus.CLUSTER)
		{
			long clusterInFocus = IntentionGraph.getInstance().getClusterInFocus();

				
				if (c.rootCanvasId == clusterInFocus)
				{
					return false;
				}
				else
				{
					return true;					
				}
			
			
		}
		else if (perspective instanceof IntentionalInterfacesPerspective)
		{
			return true;
		}
		
		return false;
	}

	public List<PNode> getTitles() {
		ArrayList<PNode> ret = new ArrayList<PNode>();
		for (Cluster c : clusters.values())
		{
			ret.add(c.clusterTitle);
			ret.add(c.wallTitle);
//			CalicoDraw.removeChildFromNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS), c.clusterTitle);
//			CalicoDraw.removeChildFromNode(IntentionGraph.getInstance().getLayer(IntentionGraph.Layer.TOOLS), c.wallTitle);
		}
		return ret;
	}
	
//	public Rectangle getTopologyBounds()
//	{
//		double minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE,
//				maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
//		
//		for (Cluster c : clusters.values())
//		{
//			if (minX > c.outerBox.getX())
//				minX = c.outerBox.getX();
//			if (minY > c.outerBox.getY())
//				minY = c.outerBox.getY();
//			if (maxX < c.outerBox.getX() + c.outerBox.getWidth())
//				maxX = c.outerBox.getX() + c.outerBox.getWidth();
//			if (maxY < c.outerBox.getY() + c.outerBox.getHeight())
//				maxY = c.outerBox.getY() + c.outerBox.getHeight());
//		}
//		
//		return new Rectangle(minX, minY,
//				maxX - minX, maxY - minY);
//	}
	

	
}
