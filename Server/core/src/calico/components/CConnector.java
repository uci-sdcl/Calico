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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.Map;
import org.apache.log4j.Logger;

import calico.components.composable.Composable;
import calico.components.composable.ComposableElement;
import calico.components.composable.ComposableElementController;
import calico.controllers.CCanvasController;
import calico.controllers.CConnectorController;
import calico.controllers.CGroupController;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PAffineTransform;
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
	}
	
	public long getCanvasUUID()
	{
		return this.canvasUID;
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
		CCanvasController.no_notify_remove_child_connector(this.canvasUID, this.uuid);
		
		//Remove from groups
		CGroupController.no_notify_remove_child_connector(this.getAnchorUUID(TYPE_HEAD), uuid);
		CGroupController.no_notify_remove_child_connector(this.getAnchorUUID(TYPE_TAIL), uuid);
	}
	
	public void linearize()
	{
		orthogonalDistance = new double[]{0.0, 0.0};
		travelDistance = new double[]{0.0, 1.0};
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
			if (CGroupController.groups.get(savedAnchorHeadUUID).containsPoint(savedHeadPoint.x, savedHeadPoint.y))
			{
				setAnchorUUID(savedAnchorHeadUUID, anchorType);
				CGroupController.no_notify_add_child_connector(savedAnchorHeadUUID, this.uuid);
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
			if (CGroupController.groups.get(savedAnchorTailUUID).containsPoint(savedTailPoint.x, savedTailPoint.y))
			{
				setAnchorUUID(savedAnchorTailUUID, anchorType);
				CGroupController.no_notify_add_child_connector(savedAnchorTailUUID, this.uuid);
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
	}
	
	public Color getColor()
	{
		return color;
	}
	
	public void setStroke(Stroke stroke)
	{
		this.stroke = stroke;
	}
	
	public Stroke getStroke()
	{
		return stroke;
	}
	
	public float getThickness()
	{
		return thickness;
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
	public int getComposableType()
	{
		return Composable.TYPE_CONNECTOR;
	}

	@Override
	public void resetToDefaultElements() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeAllElements() {
		// TODO Auto-generated method stub
		
	}
	
	//Now that getRawPolygon gets called multiple times per redraw due to compositional notations
	//We want to calculate this only once and then return the cached polygon when someone calls getRawPolygon
	public Polygon getRawPolygon()
	{
		Polygon rawPolygon = new Polygon();
		double[] tail = {pointTail.getX(), pointTail.getY()};
		double[] head = {pointHead.getX(), pointHead.getY()};
		double dx = pointHead.getX() - pointTail.getX();
		double dy = pointHead.getY() - pointTail.getY();
		double idx = -dy;
		double idy = dx;
		double magnitude = Math.sqrt((Math.pow(idx, 2) + Math.pow(idy, 2)));
		for (int i = 0; i < travelDistance.length; i++)
		{
			double[] pointOnTailHead = calico.utils.Geometry.computePointOnLine(tail[0],tail[1], head[0], head[1], travelDistance[i]);
			double x = pointOnTailHead[0] + (orthogonalDistance[i] * (idx / magnitude));
			double y = pointOnTailHead[1] + (orthogonalDistance[i] * (idy / magnitude));
			rawPolygon.addPoint((int)x, (int)y);
		}
		return rawPolygon;
	}
	
	public void render(Graphics2D g)
	{
		g.setStroke(new BasicStroke(thickness));
		g.setPaint(strokePaint);
		Polygon points = getRawPolygon();
		PAffineTransform piccoloTextTransform = getPTransform(points);
		GeneralPath p = (GeneralPath) getBezieredPoly(points).createTransformedShape(piccoloTextTransform);
		g.draw(p);
//		g.drawPolyline(this.points.xpoints, this.points.ypoints, this.points.npoints);
	}
	
}
