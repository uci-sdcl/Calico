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
package calico.components;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import calico.Calico;
import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.components.composable.Composable;
import calico.components.composable.ComposableElement;
import calico.components.composable.ComposableElementController;
import calico.components.composable.connectors.ArrowheadElement;
import calico.components.composable.connectors.HighlightElement;
import calico.controllers.CCanvasController;
import calico.controllers.CConnectorController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.Geometry;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.nodes.PComposite;

public class CConnector extends PComposite implements Composable{
	
	private static Logger logger = Logger.getLogger(CConnector.class.getName());
	
	private static final long serialVersionUID = 1L;
	
	private long uuid = 0L;
	private long canvasUID = 0L;
	
	private Color color = null;
	
	Stroke stroke;
	Color strokePaint;
	float thickness;
	
	final public static int TYPE_HEAD = 1;
	final public static int TYPE_TAIL = 2;
	private long anchorHeadUUID = 0l;
	private long anchorTailUUID = 0l;
	
	//The components that are drawn by Piccolo
	private PPath connectorLine = null;
	
	//The data model for the connector
	private Point pointHead = null;
	private Point pointTail = null;
	private Polygon rawPolygon = null;
	
	//Save the current anchor location and parent before moving
	private Point savedHeadPoint = null;
	private Point savedTailPoint = null;
	private long savedAnchorHeadUUID = 0l;
	private long savedAnchorTailUUID = 0l;
	//Orthogonal distance from the direct head to tail line
	private double[] orthogonalDistance;
	//Percent along the direct head to tail line (Percentage in decimal format; Can be negative)
	private double[] travelDistance;
	
	private boolean isHighlighted = false;
	
	// This will hold the bubble menu buttons (Class<?>)
	private static ObjectArrayList<Class<?>> bubbleMenuButtons = new ObjectArrayList<Class<?>>(); 
	
	public CConnector(long uuid, long cuid, Color color, float thickness, Polygon points)
	{
		this.uuid = uuid;
		this.canvasUID = cuid;
		this.color = color;
		this.thickness = thickness;
		
		pointHead = new Point(points.xpoints[points.npoints-1], points.ypoints[points.npoints-1]);
		pointTail = new Point(points.xpoints[0], points.ypoints[0]);
		this.anchorHeadUUID = CGroupController.get_smallest_containing_group_for_point(cuid, pointHead);
		this.anchorTailUUID = CGroupController.get_smallest_containing_group_for_point(cuid, pointTail);
		
		//pointHead and pointTail must already be assigned
		createDataModelFromPolygon(points);

		stroke = new BasicStroke( thickness );
		strokePaint = this.color;

		resetBounds();
		redraw();
	}
	
	
	public CConnector(long uuid, long cuid, Color color, float thickness, Polygon polygon, long anchorHead, long anchorTail)
	{
		this.uuid = uuid;
		this.canvasUID = cuid;
		this.color = color;
		this.thickness = thickness;
		this.anchorHeadUUID = anchorHead;
		this.anchorTailUUID = anchorTail;
		
		pointHead = new Point(polygon.xpoints[polygon.npoints-1], polygon.ypoints[polygon.npoints-1]);
		pointTail = new Point(polygon.xpoints[0], polygon.ypoints[0]);
		
		//pointHead and pointTail must already be assigned
		createDataModelFromPolygon(polygon);
		
		stroke = new BasicStroke( thickness );
		strokePaint = this.color;

		resetBounds();
		redraw();
	}
	
	public CConnector(long uuid, long cuid, Color color, float thickness, Point head, Point tail, double[] orthogonalDistance, double[] travelDistance,
			 long anchorHead, long anchorTail)
	{
		this.uuid = uuid;
		this.canvasUID = cuid;
		this.color = color;
		this.thickness = thickness;
		this.anchorHeadUUID = anchorHead;
		this.anchorTailUUID = anchorTail;
		
		pointHead = head;
		pointTail = tail;
		
		this.orthogonalDistance = orthogonalDistance;
		this.travelDistance = travelDistance;
		
		stroke = new BasicStroke( thickness );
		strokePaint = this.color;

		resetBounds();
		redraw();
	}
	
	/**
	 * Builds the connector data model from a polygon
	 * @param points
	 */
	public void createDataModelFromPolygon(Polygon points)
	{
		orthogonalDistance = new double[points.npoints];
		travelDistance = new double[points.npoints];

		for (int i = 0; i < points.npoints; i++)
		{
			//Set the distance from the point to the the direct line from the tail to the head
			orthogonalDistance[i] = Geometry.distance(pointTail.x, pointTail.y, pointHead.x, pointHead.y, points.xpoints[i], points.ypoints[i]);
			
			double[] vectorTail = {pointTail.getX(), pointTail.getY()};
			double[] vectorHead = {pointHead.getX(), pointHead.getY()};
			double[] vectorCurrent = {points.xpoints[i], points.ypoints[i]};
			
			//Get which side of the tail to head line the point is on so we can see if we need to flip the distance
			double side = Geometry.getSide(vectorTail, vectorHead, vectorCurrent);
			
			//Set the point's distance to negative depending on what side of the direct line it is on
			if (side < 0)
			{
				orthogonalDistance[i] = -orthogonalDistance[i];
			}
			
			
			
			double[] intersectingPoint = Geometry.computeIntersectingPoint(pointTail.x, pointTail.y, pointHead.x, pointHead.y, points.xpoints[i], points.ypoints[i]);
			
			//Calculate the lengths from the tail and head to the current point, and also the length from the tail to the head
			double lengthTailToPoint = Geometry.length(pointTail.getX(), pointTail.getY(), intersectingPoint[0], intersectingPoint[1]);
			double lengthTailToHead = Geometry.length(pointTail.getX(), pointTail.getY(), pointHead.getX(), pointHead.getY());
			double lengthPointToHead = Geometry.length(intersectingPoint[0], intersectingPoint[1], pointHead.getX(), pointHead.getY());
			
			//Store the percent along the tail to head line that the point is perpendicular to
			travelDistance[i] = lengthTailToPoint / lengthTailToHead;
			
			//Set percent to negative if it is behind the tail point
			if (lengthPointToHead > lengthTailToHead && lengthTailToPoint < lengthPointToHead)
			{
				travelDistance[i] = -travelDistance[i];
			}
			
			
		}
	}
	
	public long getCanvasUUID()
	{
		return this.canvasUID;
	}
	
	//Now that getRawPolygon gets called multiple times per redraw due to compositional notations
	//We want to calculate this only once and then return the cached polygon when someone calls getRawPolygon
	public void calcRawPolygon()
	{
		rawPolygon = new Polygon();
		double[] tail = {pointTail.getX(), pointTail.getY()};
		double[] head = {pointHead.getX(), pointHead.getY()};
		double dx = pointHead.getX() - pointTail.getX();
		double dy = pointHead.getY() - pointTail.getY();
		double idx = -dy;
		double idy = dx;
		double magnitude = Math.sqrt((Math.pow(idx, 2) + Math.pow(idy, 2)));
		for (int i = 0; i < travelDistance.length; i++)
		{
			double[] pointOnTailHead = Geometry.computePointOnLine(tail[0],tail[1], head[0], head[1], travelDistance[i]);
			double x = pointOnTailHead[0] + (orthogonalDistance[i] * (idx / magnitude));
			double y = pointOnTailHead[1] + (orthogonalDistance[i] * (idy / magnitude));
			rawPolygon.addPoint((int)x, (int)y);
		}
	}
	
	//Return the cached polygon
	public Polygon getRawPolygon()
	{
		return rawPolygon;
	}
	
	public Polygon getPolygon()
	{
		return calico.utils.Geometry.getPolyFromPath(connectorLine.getPathReference().getPathIterator(null));
	}
	
	public GeneralPath getPathReference()
	{
		return connectorLine.getPathReference();
	}
	
	public double[] getOrthogonalDistance()
	{
		return orthogonalDistance;
	}
	
	public double[] getTravelDistance()
	{
		return travelDistance;
	}
	
	public Point getHead()
	{
		return pointHead;
	}
	
	public Point getTail()
	{
		return pointTail;
	}
	
	public void delete()
	{		
		//Remove all elements
		ComposableElementController.no_notify_removeAllElements(this.uuid);
		
		// remove from canvas
		CCanvasController.no_notify_delete_child_connector(this.canvasUID, this.uuid);
		
		//Remove from groups
		CGroupController.no_notify_delete_child_connector(this.getAnchorUUID(TYPE_HEAD), uuid);
		CGroupController.no_notify_delete_child_connector(this.getAnchorUUID(TYPE_TAIL), uuid);
		
		if(CCanvasController.canvas_has_child_connector_node(this.canvasUID, uuid))
		{
			//This line is not thread safe so must invokeLater to prevent eraser artifacts.
			/*SwingUtilities.invokeLater(
					new Runnable() { public void run() { removeFromParent(); } }
			);*/
			CalicoDraw.removeNodeFromParent(this);
			//removeFromParent();
		}
	}
	
	public void linearize()
	{
		orthogonalDistance = new double[]{0.0, 0.0};
		travelDistance = new double[]{0.0, 1.0};
		
		redraw();
	}
	
	public void savePosition(int anchorType)
	{
		switch(anchorType)
		{
		case TYPE_HEAD: savedHeadPoint = (Point) pointHead.clone();
						savedAnchorHeadUUID = anchorHeadUUID;
			break;
		case TYPE_TAIL: savedTailPoint = (Point) pointTail.clone();
						savedAnchorTailUUID = anchorTailUUID;
			break;
		}
	}
	
	public void loadPosition(int anchorType)
	{
		switch(anchorType)
		{
		case TYPE_HEAD: 
			if (CGroupController.groupdb.get(savedAnchorHeadUUID).containsPoint(savedHeadPoint.x, savedHeadPoint.y))
			{
				setAnchorUUID(savedAnchorHeadUUID, anchorType);
				CGroupController.no_notify_add_connector(savedAnchorHeadUUID, this.uuid);
				setAnchorPoint(anchorType, savedHeadPoint);
			}		
			else
			{
				CConnectorController.no_notify_delete(this.uuid);
			}
			
			savedHeadPoint = null;
			savedAnchorHeadUUID = 0l;
			break;
		case TYPE_TAIL: 
			if (CGroupController.groupdb.get(savedAnchorTailUUID).containsPoint(savedTailPoint.x, savedTailPoint.y))
			{
				setAnchorUUID(savedAnchorTailUUID, anchorType);
				CGroupController.no_notify_add_connector(savedAnchorTailUUID, this.uuid);
				setAnchorPoint(anchorType, savedTailPoint);
			}
			else
			{
				CConnectorController.no_notify_delete(this.uuid);
			}
			
			savedTailPoint = null;
			savedAnchorTailUUID = 0l;
			break;
		}
	}
	
	public void setAnchorUUID(long uuid, int anchorType)
	{
		switch(anchorType)
		{
		case TYPE_HEAD: anchorHeadUUID = uuid;
			break;
		case TYPE_TAIL: anchorTailUUID = uuid;
			break;
		}
	}
	
	public long getAnchorUUID(int anchorType)
	{
		switch(anchorType)
		{
		case TYPE_HEAD: return anchorHeadUUID;

		case TYPE_TAIL: return anchorTailUUID;

		default: return 0l;
		}
	}
	
	public void setAnchorPoint(int anchorType, Point point)
	{
		switch(anchorType)
		{
		case TYPE_HEAD: pointHead = (Point) point.clone();
			break;

		case TYPE_TAIL: pointTail = (Point) point.clone();
			break;

		}
		
		redraw();
	}
	
	public Point getAnchorPoint(int anchorType)
	{
		switch(anchorType)
		{
		case TYPE_HEAD: return pointHead;

		case TYPE_TAIL: return pointTail;

		}
		return null;
	}
	
	public long getUUID()
	{
		return this.uuid;
	}
	
	public void setColor(Color color)
	{
		this.color = color;
		
		redraw();
	}
	
	public Color getColor()
	{
		return color;
	}
	
	public void setStroke(Stroke stroke)
	{
		this.stroke = stroke;
		
		redraw();
	}
	
	public Stroke getStroke()
	{
		return stroke;
	}
	
	public float getThickness()
	{
		return thickness;
	}
	
	@Override
	public PBounds getBounds()
	{
		Rectangle bounds = this.getFullBounds().getBounds();
		double buffer = 30;
		PBounds bufferBounds = new PBounds(bounds.getX() - buffer, bounds.getY() - buffer, bounds.getWidth() + buffer * 2, bounds.getHeight() + buffer * 2);
		return bufferBounds;
	}
	
	public void redraw()
	{
		calcRawPolygon();
		
		calcConnectorLine();
		
		//First cache the nodes such as arrowheads and cardinality objects
		//because the CalicoDraw methods (Event Dispatch Thread) should not be performing heavy calculations
		ArrayList<PNode> nodes = new ArrayList<PNode>();
		if (ComposableElementController.elementList.containsKey(this.uuid))
		{
			for (Map.Entry<Long, ComposableElement> entry : ComposableElementController.elementList.get(this.uuid).entrySet())
			{
				ComposableElement element = entry.getValue();
				if (element.isDrawable())
				{
					PNode node = element.getNode();
					if (node != null)
					{
						nodes.add(node);
					}
				}
			}
		}
				
		CalicoDraw.removeAllChildrenFromNode(this);

		//Now we actually add the child nodes to the connector PComposite
		for (int i = 0; i < nodes.size(); i++)
		{
			CalicoDraw.addChildToNode(this, nodes.get(i), 0);
		}

		CalicoDraw.addChildToNode(this, connectorLine, 0);
		//this.repaint();
		CalicoDraw.repaintNode(this);
	}
	
	protected void calcConnectorLine()
	{
		Polygon linePoints = getRawPolygon();

		connectorLine = new PPath();
		connectorLine.setStroke(stroke);
		connectorLine.setStrokePaint(this.color);
		
		applyAffineTransform(linePoints);
	}
	
	
	public void highlight_on() {
		isHighlighted = true;

		redraw();
	}
	
	public void highlight_off() {
		isHighlighted = false;

		redraw();
	}
	
	public boolean isHighlighted()
	{
		return isHighlighted;
	}
	
	public void moveAnchor(long guuid, int deltaX, int deltaY)
	{
		if (anchorHeadUUID == guuid)
		{
			pointHead.setLocation(pointHead.x + deltaX, pointHead.y + deltaY);
		}
		if (anchorTailUUID == guuid)
		{
			pointTail.setLocation(pointTail.x + deltaX, pointTail.y + deltaY);
		}
		redraw();
	}
	
	public void moveAnchor(int type, int deltaX, int deltaY)
	{
		if (type == TYPE_HEAD)
		{
			pointHead.setLocation(pointHead.x + deltaX, pointHead.y + deltaY);
		}
		else if (type == TYPE_TAIL)
		{
			pointTail.setLocation(pointTail.x + deltaX, pointTail.y + deltaY);
		}
		redraw();
	}

	
	
	protected void applyAffineTransform(Polygon points)
	{
		PAffineTransform piccoloTextTransform = getPTransform(points);
		GeneralPath p = (GeneralPath) getBezieredPoly(points).createTransformedShape(piccoloTextTransform);
		connectorLine.setPathTo(p);

	}
	
	public PAffineTransform getPTransform(Polygon points) {
		PAffineTransform piccoloTextTransform = new PAffineTransform();
		return piccoloTextTransform;
	}
	
	public GeneralPath getBezieredPoly(Polygon pts)
	{
		GeneralPath p = new GeneralPath();
		if (pts.npoints > 0)
		{
			p.moveTo(pts.xpoints[0], pts.ypoints[0]);
			if (pts.npoints >= 4)
			{
				int counter = 1;
				for (int i = 1; i+2 < pts.npoints; i += 3)
				{
					p.curveTo(pts.xpoints[i], pts.ypoints[i], 
							pts.xpoints[i+1], pts.ypoints[i+1], 
							pts.xpoints[i+2], pts.ypoints[i+2]);
					counter += 3;
				}
				while (counter < pts.npoints)
				{
					p.lineTo(pts.xpoints[counter], pts.ypoints[counter]);
					counter++;
				}
			}
			else
			{
				for (int i = 1; i < pts.npoints; i++)
				{
					p.lineTo(pts.xpoints[i], pts.ypoints[i]);
				}
			}
		}
		return p;
	}
	
	public CalicoPacket[] getUpdatePackets(long uuid, long cuid)
	{			
		int packetSize = ByteUtils.SIZE_OF_INT + (2 * ByteUtils.SIZE_OF_LONG) + ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_INT
				+ (ByteUtils.SIZE_OF_INT * 4) + ByteUtils.SIZE_OF_INT + (2 * this.orthogonalDistance.length * ByteUtils.SIZE_OF_LONG) + (2 * ByteUtils.SIZE_OF_LONG);
		
		CalicoPacket packet = new CalicoPacket(packetSize);

		packet.putInt(NetworkCommand.CONNECTOR_LOAD);
		packet.putLong(uuid);
		packet.putLong(cuid);
		packet.putColor(new Color(this.getColor().getRed(), this.getColor().getGreen(), this.getColor().getBlue()));
		packet.putFloat(this.thickness);
		
		packet.putInt(pointHead.x);
		packet.putInt(pointHead.y);
		packet.putInt(pointTail.x);
		packet.putInt(pointTail.y);
		
		packet.putInt(this.orthogonalDistance.length);
		for(int j=0;j<this.orthogonalDistance.length;j++)
		{
			packet.putDouble(this.orthogonalDistance[j]);
			packet.putDouble(this.travelDistance[j]);
		}
		
		packet.putLong(anchorHeadUUID);
		packet.putLong(anchorTailUUID);
		
		return new CalicoPacket[]{packet};

	}
	
	public CalicoPacket[] getUpdatePackets()
	{
		return getUpdatePackets(this.uuid, this.canvasUID);
	}
	
	public CalicoPacket[] getStrokePackets()
	{			
		calcRawPolygon();
		Polygon mousePoints = getRawPolygon();
		int packetSize = ByteUtils.SIZE_OF_INT + (3 * ByteUtils.SIZE_OF_LONG) + ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_SHORT + (2 * mousePoints.npoints * ByteUtils.SIZE_OF_SHORT);
		
		CalicoPacket packet = new CalicoPacket(packetSize);
		//UUID CUID PUID <COLOR> <NUMCOORDS> x1 y1
		packet.putInt(NetworkCommand.STROKE_LOAD);
		packet.putLong(Calico.uuid());
		packet.putLong(canvasUID);
		packet.putLong(0l);
		packet.putColor(new Color(this.getColor().getRed(), this.getColor().getGreen(), this.getColor().getBlue()));
		packet.putFloat(this.thickness);
		packet.putCharInt(mousePoints.npoints);
		for(int j=0;j<mousePoints.npoints;j++)
		{
			packet.putInt(mousePoints.xpoints[j]);
			packet.putInt(mousePoints.ypoints[j]);
		}
		packet.putDouble(0.0);
		packet.putDouble(1.0);
		packet.putDouble(1.0);
		
		return new CalicoPacket[]{packet};

	}
	
	public ObjectArrayList<Class<?>> getBubbleMenuButtons()
	{
		ObjectArrayList<Class<?>> bubbleMenuButtons = new ObjectArrayList<Class<?>>();
		bubbleMenuButtons.addAll(internal_getBubbleMenuButtons());
		//bubbleMenuButtons.addAll(CConnector.bubbleMenuButtons);
		return bubbleMenuButtons;
	}
	
	protected ObjectArrayList<Class<?>> internal_getBubbleMenuButtons()
	{
		ObjectArrayList<Class<?>> bubbleMenuButtons = new ObjectArrayList<Class<?>>(); 
		bubbleMenuButtons.add(calico.components.bubblemenu.connectors.ConnectorLinearizeButton.class);
		bubbleMenuButtons.add(calico.components.bubblemenu.connectors.ConnectorMakeStrokeButton.class);
		bubbleMenuButtons.add(calico.components.bubblemenu.connectors.ConnectorMoveHeadButton.class);
		bubbleMenuButtons.add(calico.components.bubblemenu.connectors.ConnectorMoveTailButton.class);
		return bubbleMenuButtons;
	}
	
	public int get_signature() {

		int sig = (int) (this.orthogonalDistance.length + pointHead.x + pointHead.y + anchorTailUUID);

//		System.out.println("Debug sig for group " + uuid + ": " + sig + ", 1) " + this.points.npoints + ", 2) " + isPermanent() + ", 3) " + this.points.xpoints[0] + ", 4) " + this.points.xpoints[0] + ", 5) " + this.points.ypoints[0] + ", 6) " + (int)(this.rotation*10) + ", 7) " + (int)(this.scaleX*10) + ", 8) " + (int)(this.scaleY*10));
		return sig;
	}
	
	public String get_signature_debug_output()
	{
		return "Debug sig for connector " + uuid + ": 1) " +this.orthogonalDistance.length + ", 2) " + pointHead.x + ", 3) " + pointHead.y + ", 4) " + anchorTailUUID;
	}	


	@Override
	public CalicoPacket[] getComposableElements() {
		if (!ComposableElementController.elementList.containsKey(uuid))
		{
			return new CalicoPacket[0];
		}
		
		CalicoPacket[] packets = new CalicoPacket[ComposableElementController.elementList.get(uuid).size()];
		int count = 0;
		for (Map.Entry<Long, ComposableElement> entry : ComposableElementController.elementList.get(uuid).entrySet())
		{
			packets[count] = entry.getValue().getPacket();
			count++;
		}
		
		return packets;
	}


	@Override
	public void resetToDefaultElements() {
		removeAllElements();
		
		ComposableElementController.addElement(new ArrowheadElement(Calico.uuid(), uuid, CConnector.TYPE_HEAD, CalicoOptions.arrow.stroke_size, Color.black, Color.red, ArrowheadElement.getDefaultArrow()));
		ComposableElementController.addElement(new ArrowheadElement(Calico.uuid(), uuid, CConnector.TYPE_TAIL, CalicoOptions.arrow.stroke_size, Color.black, Color.black, ArrowheadElement.getDefaultCircle()));
		ComposableElementController.addElement(new HighlightElement(Calico.uuid(), uuid, CalicoOptions.stroke.background_transparency, new BasicStroke(CalicoOptions.pen.stroke_size + 8), Color.blue));
	
		redraw();
	}


	@Override
	public void removeAllElements() {
		if (!ComposableElementController.elementList.containsKey(uuid))
			return;
		
		for (Map.Entry<Long, ComposableElement> entry : ComposableElementController.elementList.get(uuid).entrySet())
		{
			ComposableElementController.removeElement(entry.getValue().getElementUUID(), uuid);
		}
	}
	
	@Override
	public int getComposableType()
	{
		return Composable.TYPE_CONNECTOR;
	}
}
