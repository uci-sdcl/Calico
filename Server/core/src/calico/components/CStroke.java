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

import calico.*;
import calico.networking.netstuff.*;
import calico.controllers.*;
import calico.utils.*;

import java.awt.*;
import java.util.Arrays;
import java.util.Properties;

import java.awt.geom.*;

import org.apache.log4j.Logger;

import edu.umd.cs.piccolo.util.PAffineTransform;


// Eventual todo list:
// TODO: Add a function that sends an update packet, for consistency (Has the uuid, parent, color, position, and coords, all rolled into one)




public class CStroke
{
	private static Logger logger = Logger.getLogger(CStroke.class.getName());
	
	private long parent 	= 0L;
	private long uuid 		= 0L;
	private long canvasuid 	= 0L;

	private Polygon points = new Polygon();
	private GeneralPath smoothedPath;
	private GeneralPath pathReferenceShadow;	//this shadows the path reference attribute on the client side
	protected double scaleX = 1.0d, scaleY = 1.0d;
	protected double rotation = 0.0d;
	
	private int red = 0;
	private int green = 0;
	private int blue = 0;
	
	private Color color = Color.BLACK;
	private float thickness;
	
	private boolean finished = false;
	
	//See method applyAffineTransform() for explanation
//	private Polygon pointsOriginal;
	
	//See method applyAffineTransform() for explanation
//	ArrayList<AffineTransform> strokeTransforms; 	

	
	////////////// BACKUP STATE
	
	
	public CStroke(long uuid, long cuid)
	{
		this(uuid,cuid,0L, COptions.stroke.default_color, COptions.stroke.default_thickness);
	}
	
	public CStroke(long uuid, long cuid, long puid)
	{
		this(uuid, cuid, puid, COptions.stroke.default_color, COptions.stroke.default_thickness);
	}
	public CStroke(long uuid, long cuid, long puid, Color color, float thickness)
	{
		this.canvasuid = cuid;
		this.uuid = uuid;
		this.parent = puid;
		
		this.color = color;
		
		this.red = color.getRed();
		this.blue = color.getBlue();
		this.green = color.getGreen();
		
		this.thickness = thickness;
		
//		strokeTransforms = new ArrayList<AffineTransform>();
	}
	
	
	
	public byte[] getHashCode()
	{
		CalicoPacket pack = new CalicoPacket(16);
		pack.putInt(Arrays.hashCode(new long[]{this.uuid, this.canvasuid, this.parent}));
		pack.putInt(Arrays.hashCode(this.points.xpoints) );
		pack.putInt(Arrays.hashCode(this.points.ypoints) );
		pack.putInt(color.getRGB());
		pack.putFloat(thickness);
		return pack.getBuffer();
	}
	
	
	
	public long getCanvasUUID()
	{
		return this.canvasuid;
	}
	
	public GeneralPath getPathReference()
	{
		return pathReferenceShadow;
	}
	
	/**
	 * Has this been finished (Should we discard anymore BGE_FInished packets)
	 * @return
	 */
	public boolean isFinished()
	{
		return this.finished;
	}
	
	public void forceSetFinished(boolean fin)
	{
		this.finished = fin;
	}
	
	
	public void setParentUUID(long newParentUUID)
	{
		logger.trace("Changing parent for " + uuid + ": " + this.parent + " -> " + newParentUUID);
		this.parent = newParentUUID;
	}

	public Polygon getPolygon() {
		return Geometry.getPolyFromPath(pathReferenceShadow.getPathIterator(null));
	}
	
	public Polygon getRawPolygon() {
		return points;
	}
	
	public boolean isContainedInPath(GeneralPath path)
	{
		Polygon p = Geometry.getPolyFromPath(getPathReference().getPathIterator(null));
		for(int i=0;i<p.npoints;i++)
		{
			Point point = new Point(p.xpoints[i], p.ypoints[i]);
			if(!path.contains(point))
			{
				return false;
			}
		}
		
		return true;
	}
	
	public void move(int x, int y)
	{
		points.translate(x, y);
		applyAffineTransform();
	}

	public void delete()
	{
		this.points.reset();
	}
	

	public long getUUID()
	{
		return this.uuid;
	}
	
	
	public long getParentUUID()
	{
		return this.parent;
	}
	
	public Color getColor()
	{
		return new Color(this.red, this.green, this.blue);
	}
	
	public float getThickness()
	{
		return thickness;
	}
	
	public CalicoPacket[] getUpdatePackets(long uuid, long cuid, long puid, int dx, int dy)
	{
		if(!isFinished())
		{
			return null;
		}
		
//		if(COptions.client.network.cluster_size>=this.points.npoints)
//		{
			// WE CAN SEND A STROKE_LOAD SINGLE PACKET
			
			int packetSize = ByteUtils.SIZE_OF_INT + (3 * ByteUtils.SIZE_OF_LONG) + ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_SHORT + (2 * this.points.npoints * ByteUtils.SIZE_OF_SHORT);
			
			CalicoPacket packet = new CalicoPacket(packetSize);
			//UUID CUID PUID <COLOR> <NUMCOORDS> x1 y1
			packet.putInt(NetworkCommand.STROKE_LOAD);
			packet.putLong(uuid);
			packet.putLong(cuid);
			packet.putLong(puid);
			packet.putColor(new Color(this.red, this.green, this.blue));
			packet.putFloat(this.thickness);
			packet.putCharInt(this.points.npoints);
			for(int j=0;j<this.points.npoints;j++)
			{
				packet.putInt(this.points.xpoints[j] + dx);
				packet.putInt(this.points.ypoints[j] + dy);
			}
			packet.putDouble(this.rotation);
			packet.putDouble(this.scaleX);
			packet.putDouble(this.scaleY);
			
			return new CalicoPacket[]{packet};
//		}
//		else
//		{
//		
//			// how many packets are we going to need?
//			int numPackets = 4;// START + END
//			for(int i=0;i<this.points.npoints;i=i+COptions.client.network.cluster_size)
//			{
//				numPackets++;// APPENDs
//			}
//			
//			CalicoPacket[] packets = new CalicoPacket[numPackets];
//			
//			packets[0] = CalicoPacket.getPacket(NetworkCommand.STROKE_START, 
//				uuid, 
//				cuid, 
//				puid, 
//				this.red, this.green, this.blue,
//				thickness
//			);
//			
//			int packetIndex = 1;
//			
//			for(int i=0;i<this.points.npoints;i=i+COptions.client.network.cluster_size)
//			{
//				// determine how many coordinates will be in the next cluster
//				int numingroup = (i+COptions.client.network.cluster_size) > this.points.npoints ? (this.points.npoints-i) : COptions.client.network.cluster_size;
//				
//				packets[packetIndex] = new CalicoPacket(2 * numingroup * ByteUtils.SIZE_OF_SHORT);
//				packets[packetIndex].putInt(NetworkCommand.STROKE_APPEND);
//				packets[packetIndex].putLong(uuid);
//				packets[packetIndex].putCharInt(numingroup);
//				
//				for(int j=0;j<numingroup;j++)
//				{
//					packets[packetIndex].putInt(this.points.xpoints[i+j] + dx);
//					packets[packetIndex].putInt(this.points.ypoints[i+j] + dy);
//				}
//				
//				packetIndex++;
//			}
//			packets[packetIndex++] = CalicoPacket.getPacket(NetworkCommand.STROKE_FINISH, uuid);
//			packets[packetIndex++] = CalicoPacket.getPacket(
//					NetworkCommand.STROKE_ROTATE, uuid, this.rotation);
//			packets[packetIndex] = CalicoPacket.getPacket(
//					NetworkCommand.STROKE_SCALE, uuid, this.scaleX, this.scaleY);
//			
//			return packets;
//		}
	}
	
	public CalicoPacket[] getUpdatePackets()
	{
		return getUpdatePackets(this.uuid, this.canvasuid, this.parent, 0, 0);
	}
	

	/**
	 * Sets the color of the stroke
	 * @param r Red 0-255
	 * @param g Green 0-255
	 * @param b Blue 0-255
	 */
	public void setColor(int red, int green, int blue)
	{
		this.red = red;
		this.green = green;
		this.blue = blue;
	}
	
	public void setColor(Color col)
	{
		setColor(col.getRed(), col.getGreen(), col.getBlue());
	}
	
	public void setThickenss(float t)
	{
		this.thickness = t;
	}

	/**
	 * Adds a coordinate to the stroke's line
	 * @param x
	 * @param y
	 */
	public void append(int x, int y)
	{
		this.points.addPoint(x,y);
		
		if(points.npoints==1)
		{
			smoothedPath = new GeneralPath();
			smoothedPath.moveTo(x, y);
			pathReferenceShadow = new GeneralPath();
			pathReferenceShadow.moveTo(x, y);
		}
		else
		{
			smoothedPath.lineTo(x, y);
			pathReferenceShadow.lineTo(x,y);
		}
	}
	
	public void batch_append(int[] x, int[] y)
	{
		for(int i=0;i<x.length;i++)
		{
			points.addPoint(x[i], y[i]);
		}
	}

	/**
	 * Finish drawing the stroke
	 */
	public void finish()
	{
		// Have we already finished?
		if(isFinished())
		{
			return;
		}
		
		// Mark this as being finished.
		this.finished = true;
		
		smoothedPath = getBezieredPoly(points);
		pathReferenceShadow = smoothedPath;
		applyAffineTransform();
//		pointsOriginal = new Polygon(points.xpoints, points.ypoints, points.npoints);
	}//
	
	/**
	 * This recalculates the parents of the element, and then returns the packet that should be sent
	 * @return
	 */
	public CalicoPacket calculateParent()
	{
		
		double smallestArea = Double.MAX_VALUE;
		long smallestGroupUUID = 0L;
		
		long[] groupList = CCanvasController.canvases.get(this.canvasuid).getChildGroups();
		
		if(groupList.length>0)
		{
			for(int i=0;i<groupList.length;i++)
			{
				if (!CGroupController.exists(groupList[i]))
					continue;
				
				if(smallestArea > CGroupController.groups.get(groupList[i]).getArea() 
					&& CGroupController.groups.get(groupList[i]).isPermanent()
//					&& isContainedInPath(CGroupController.groups.get(groupList[i]).getPathReference())
					&& CGroupController.groups.get(groupList[i]).canParentChild(this.uuid, (int)Geometry.getMidPoint2D(points).getX(), (int)Geometry.getMidPoint2D(points).getY())
					)
				{
					smallestArea = CGroupController.groups.get(groupList[i]).getArea();
					smallestGroupUUID = groupList[i];
//					CalicoServer.logger.debug("Checking area for GRP_"+groupList[i]+" FITS!");
				}//
				else
				{
//					CalicoServer.logger.debug("Checking area for GRP_"+groupList[i]+" NO FITS!");
				}
			}
		}
		
		// Only run this if we actually found a parent
		if(smallestGroupUUID!=0L)
		{
			// We have a parent!
			CStrokeController.no_notify_set_parent(this.uuid, smallestGroupUUID);
		}
		
		
		
		CalicoPacket packet = new CalicoPacket(ByteUtils.SIZE_OF_INT + (2 * ByteUtils.SIZE_OF_LONG));
		packet.putInt(NetworkCommand.STROKE_SET_PARENT);
		packet.putLong(this.uuid);
		packet.putLong(this.parent);
		return packet;
	}
	
	
	public void render(Graphics2D g)
	{
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(new BasicStroke(thickness,BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setPaint(getColor());
		g.draw(pathReferenceShadow);
//		g.drawPolyline(this.points.xpoints, this.points.ypoints, this.points.npoints);
	}

	
	
	public Properties toProperties()
	{
		Properties props = new Properties();

		props.setProperty("uuid", ""+this.uuid);
		props.setProperty("color", ""+((new Color(this.red, this.green, this.blue)).getRGB()) );
		props.setProperty("puid", ""+this.parent);
		props.setProperty("cuid", ""+this.canvasuid);
		
		int[] pointsprint = new int[2*this.points.npoints];
		int pointind = 0;
		for(int i=0;i<this.points.npoints;i++)
		{
			pointsprint[pointind++] = this.points.xpoints[i];
			pointsprint[pointind++] = this.points.ypoints[i];
		}
		
		props.setProperty("points", Arrays.toString(pointsprint) );
		
		return props;
	}

	public void rotate(double radians)
	{
		rotate(radians, Geometry.getMidPoint2D(points));
	}
	
	public void rotate(double radians, Point2D pivotPoint)
	{
		AffineTransform rotateAboutPivot = AffineTransform.getRotateInstance(radians, pivotPoint.getX(), pivotPoint.getY());
		
		//1) compute mid point translation
		Point2D oldMidPoint = Geometry.getMidPoint2D(points);
		Point2D newMidPoint = null;
		newMidPoint = rotateAboutPivot.transform(oldMidPoint, newMidPoint);
		int deltaX = new java.lang.Double(newMidPoint.getX() - oldMidPoint.getX()).intValue();
		int deltaY = new java.lang.Double(newMidPoint.getY() - oldMidPoint.getY()).intValue();
		points.translate(deltaX, deltaY);
		smoothedPath.transform(AffineTransform.getTranslateInstance(deltaX, deltaY));
		
		//2) compute actual rotation change
		Rectangle2D bounds = points.getBounds2D();
		Point2D oldBoundsRightCorner = new Point2D.Double(bounds.getX() + bounds.getWidth(), bounds.getY());
		Point2D newBoundsRightCorner = null;
		newBoundsRightCorner = rotateAboutPivot.transform(oldBoundsRightCorner, newBoundsRightCorner);
		
		double angleOld = calico.utils.Geometry.angle(oldMidPoint, oldBoundsRightCorner);
		double angleNew = calico.utils.Geometry.angle(newMidPoint, newBoundsRightCorner);
		double actualRotation = angleNew - angleOld;
		primative_rotate(actualRotation + rotation);
	}

	public void primative_rotate(double actualRotation) {
		rotation = actualRotation;
		applyAffineTransform();
	}
	
	public void scale(double scaleX, double scaleY)
	{
		scale(scaleX, scaleY, Geometry.getMidPoint2D(points));
	}
	
	public void scale(double scaleX, double scaleY, Point2D pivotPoint) {
		
		AffineTransform scaleAboutPivot1 = AffineTransform.getTranslateInstance(-1 * pivotPoint.getX(), -1 * pivotPoint.getY());
		AffineTransform scaleAboutPivot2 = AffineTransform.getScaleInstance(scaleX, scaleY);
		AffineTransform scaleAboutPivot3 = AffineTransform.getTranslateInstance(pivotPoint.getX(), pivotPoint.getY());
		
		//1) compute mid point translation
		Point2D oldMidPoint = Geometry.getMidPoint2D(points);
		Point2D newMidPoint = null;
		newMidPoint = scaleAboutPivot1.transform(oldMidPoint, newMidPoint);
		newMidPoint = scaleAboutPivot2.transform(newMidPoint, null);
		newMidPoint = scaleAboutPivot3.transform(newMidPoint, null);
		int deltaX = new java.lang.Double(newMidPoint.getX() - oldMidPoint.getX()).intValue();
		int deltaY = new java.lang.Double(newMidPoint.getY() - oldMidPoint.getY()).intValue();
		points.translate(deltaX, deltaY);
		smoothedPath.transform(AffineTransform.getTranslateInstance(deltaX, deltaY));
		
		//2) assign actual scale
		primative_scale(this.scaleX * scaleX, this.scaleY * scaleY);
	}

	public void primative_scale(double scaleX, double scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		applyAffineTransform();
	}
	
	/**
	 * See CGroup.applyAffineTransform for explanation
	 * 
	 * @param maintainCenter
	 */
	protected void applyAffineTransform()
	{
		PAffineTransform piccoloTextTransform = getPTransform();
		Point2D midPoint = Geometry.getMidPoint2D(points);
		piccoloTextTransform.rotate(rotation, midPoint.getX(), midPoint.getY());
		piccoloTextTransform.scaleAboutPoint(scaleX, midPoint.getX(), midPoint.getY());
		GeneralPath p = (GeneralPath) getBezieredPoly(points).createTransformedShape(piccoloTextTransform);
		pathReferenceShadow = p;		
	}
	
	public PAffineTransform getPTransform() {
		PAffineTransform piccoloTextTransform = new PAffineTransform();
		Point2D midPoint = Geometry.getMidPoint2D(points);
		piccoloTextTransform.rotate(rotation, midPoint.getX(), midPoint.getY());
		piccoloTextTransform.scaleAboutPoint(scaleX, midPoint.getX(), midPoint.getY());
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

	public double getRotation() {
		return rotation;
	}

	public double getScale() {
		return scaleX;
	}
	
	public int get_signature()
	{
		if (!finished)
			return 0;
		int sig = this.points.npoints + this.color.getRGB() + this.points.xpoints[0] + this.points.ypoints[0] + (int)(this.rotation*10) + (int)(this.scaleX*10) + (int)(this.scaleY*10);
//		System.out.println("Debug sig for stroke " + uuid + ": " + sig + ", 1) " + this.points.npoints + ", 2) " + this.color.getRGB() + ", 3) " + this.points.xpoints[0] + ", 4) " + this.points.xpoints[0] + ", 5) " + this.points.ypoints[0] + ", 6) " + (int)(this.rotation*10) + ", 7) " + (int)(this.scaleX*10) + ", 8) " + (int)(this.scaleY*10));
		return sig;
	}
	
	public String get_signature_debug_output()
	{
		return "Debug sig for stroke " + uuid + ": 1) " + this.points.npoints + ", 2) " + this.color.getRGB() + ", 3) " + 0 + ", 4) " + this.points.xpoints[0] + ", 5) " + this.points.ypoints[0] + ", 6) " + (int)(this.rotation*10) + ", 7) " + (int)(this.scaleX*10) + ", 8) " + (int)(this.scaleY*10);
	}
	
}

