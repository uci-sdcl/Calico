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
import calico.networking.*;
import calico.networking.netstuff.*;
import calico.admin.*;
import calico.clients.*;
import calico.components.decorators.CGroupDecorator;
import calico.components.decorators.CListDecorator;
import calico.controllers.CArrowController;
import calico.controllers.CCanvasController;
import calico.controllers.CConnectorController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.utils.CalicoUtils;
import calico.utils.Geometry;
import calico.uuid.*;

import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.json.me.*;

import edu.umd.cs.piccolo.util.PAffineTransform;

public class CGroup {
	// private static final long serialVersionUID = 42L;

	// The UUID for the group
	protected long uuid = 0L;

	// The parent of the group (another group, or 0==on canvas)
	protected long puid = 0L;

	// the uuid for the canvas that contains this group
	protected long cuid = 0L;

	protected Polygon points = new Polygon();

//	private GeneralPath smoothedPath;
	private GeneralPath pathReferenceShadow;	//this shadows the path reference attribute on the client side
	protected double scaleX = 1.0d, scaleY = 1.0d;
	protected double rotation = 0.0d;

	// See method applyAffineTransform() for explanation
	// ArrayList<AffineTransform> groupTransforms;

	protected double groupArea = 0.0;

	// These are all the child groups
	protected LongArraySet childGroups = new LongArraySet();

	// the child BGElements, this ARE NOT bgelements that are in child groups
	protected LongArraySet childStrokes = new LongArraySet();
	
	protected LongArraySet childConnectors = new LongArraySet();

	protected boolean isDeleted = false;

	// list of arrows
	protected LongArraySet childArrows = new LongArraySet();

	protected String text = "";
	private boolean textSet = false;

	protected boolean isPermanent = false;

	protected boolean finished = false;

	protected static Logger logger = Logger.getLogger(CGroup.class.getName());
	
	protected int networkLoadCommand = NetworkCommand.GROUP_LOAD;
	
	private static BasicStroke groupStroke = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	public CGroup(long uuid, long cuid, long puid, boolean isPerm) {
		this.uuid = uuid;
		this.cuid = cuid;
		this.puid = puid;
		this.isPermanent = isPerm;
	}

	public CGroup(long uuid, long cuid, long puid) {
		this(uuid, cuid, puid, false);
	}

	public CGroup(long uuid, long cuid) {
		this(uuid, cuid, 0L);
	}

	public byte[] getHashCode() {
		CalicoPacket pack = new CalicoPacket(18);
		pack.putInt(Arrays.hashCode(new long[] { this.uuid, this.cuid,
				this.puid }));
		pack.putInt(Arrays.hashCode(this.points.xpoints));
		pack.putInt(Arrays.hashCode(this.points.ypoints));
		pack.putInt(Arrays.hashCode(getChildStrokes()));
		pack.putInt(Arrays.hashCode(getChildGroups()));
		pack.putInt(Arrays.hashCode(getChildArrows()));
		pack.putInt(Arrays.hashCode(getChildConnectors()));

		return pack.getBuffer();
	}

	public boolean isPermanent() {
		return this.isPermanent;
	}

	public void setPermanent(boolean perm) {
		this.isPermanent = perm;
	}

	public void setText(String t) {
		this.text = t;
		this.textSet = true;
	}

	public void addPoint(int x, int y) {
		this.points.addPoint(x, y);
	}
	
	public void append(int[] x, int[] y)
	{
		for (int i = 0; i < x.length; i++)
			this.points.addPoint(x[i], y[i]);
	}

	/**
	 * Moves this group, as well as any children
	 * 
	 * @param x
	 *            x-delta
	 * @param y
	 *            y-delta
	 */
	public void move(int x, int y) {
		if (points == null)
			return;
		
		if (childArrows.size() > 0) {
			long[] auid = childArrows.toLongArray();
			for (int i = 0; i < auid.length; i++) {
				CArrowController.no_notify_move_group(auid[i], this.uuid, x, y);
			}
		}
		
		if (childConnectors.size() > 0) {
			long[] cuid = childConnectors.toLongArray();
			for (int i = 0; i < cuid.length; i++) {
				CConnectorController.no_notify_move_group_anchor(cuid[i], uuid, x, y);
			}
		}

		for (long s : childStrokes) {
			if (CStrokeController.strokes.containsKey(s)) {
				if (CStrokeController.strokes.get(s).getParentUUID() == this.uuid)
					CStrokeController.no_notify_move(s, x, y);
				else {
					logger.error("GROUP " + this.uuid
							+ " is trying to move kidnapped stroke " + s);
					childStrokes.rem(s);
				}
			}
		}

		for (long g : childGroups) {
			if (CGroupController.groups.containsKey(g))
				if (CGroupController.groups.get(g).getParentUUID() == this.uuid)
					CGroupController.no_notify_move(g, x, y);
				else {
					logger.error("GROUP " + this.uuid
							+ " is trying to move kidnapped group " + g);
					childGroups.rem(g);
				}
		}
		
		points.translate(x, y);
		applyAffineTransform();

	}//

	/**
	 * @return the Polygon object representation of the points making up this
	 *         group
	 */
//	public Polygon getPolygon() {
//		Polygon p = getPolyFromPath(pathReferenceShadow.getPathIterator(null));
//		System.out.println("CGroup.getPolygon called.\n" +
//				"Bounds: (" + p.getBounds().x + ", " + p.getBounds().y + ", " +
//							+ p.getBounds().width + ", " + p.getBounds().height + ")");
//		return p;
//	}
	
	public GeneralPath getPathReference() 
	{
		return pathReferenceShadow;
	}
	
	public Polygon getRawPolygon() {
		return points;
	}

	/**
	 * @return the Area of the group
	 */
	public double getArea() {
		return this.groupArea;
	}

	/**
	 * Sets the UUID of the parent container of this object
	 * 
	 * @param u
	 *            uuid of parent (0 to unset)
	 */
	public void setParentUUID(long u) {
		if (u == this.puid)
			return;
		
		if (puid == this.uuid)
			puid = 0l;
		
		logger.trace("Changing parent for " + uuid + ": " + this.puid + " -> " + u);
		this.puid = u;
		if (CGroupController.exists(u))
		{
			CGroup parent = CGroupController.groups.get(u);
			if (!parent.hasChildGroup(this.uuid))
			{
				Point2D midPoint = getMidPoint();
				parent.addChildGroup(this.uuid, (int)midPoint.getX(), (int)midPoint.getY());
			}
		}

	}

	public boolean hasChildGroup(long childuuid) {
		long[] children = getChildGroups();
		for (int i = 0; i < children.length; i++)
		{
			if (children[i] == childuuid)
				return true;
		}
		return false;
	}
	
	public long getParentUUID() {
		return this.puid;
	}

	public void addChildArrow(long uid) {
		if (!this.childArrows.contains(uid))
			this.childArrows.add(uid);
	}

	public void deleteChildArrow(long uid) {
		this.childArrows.remove(uid);
	}

	public long[] getChildArrows() {
		return this.childArrows.toLongArray();
	}
	
	public void addChildConnector(long uid) {
		if (!this.childConnectors.contains(uid))
			this.childConnectors.add(uid);
	}

	public void deleteChildConnector(long uid) {
		this.childConnectors.remove(uid);
	}

	public long[] getChildConnectors() {
		return this.childConnectors.toLongArray();
	}

	public void setChildStrokes(long[] bglist) {
		childStrokes.clear();
		for (int i = 0; i < bglist.length; i++)
			childStrokes.add(bglist[i]);
	}

	public void setChildGroups(long[] gplist) {
		childGroups.clear();
		for (int i = 0; i < gplist.length; i++)
		{
			childGroups.add(gplist[i]);
			if (CGroupController.exists(gplist[i]))
			{
				CGroup child = CGroupController.groups.get(gplist[i]);
				if (child.getParentUUID() != this.uuid)
					child.setParentUUID(this.uuid);
			}	
		}
	}
	
	public void setChildArrows(long[] arlist) {
		this.childArrows.clear();
		for (int i = 0; i < arlist.length; i++)
			childArrows.add(arlist[i]);
	}
	
	public void setChildConnectors(long[] ctlist, int x, int y) {
		this.childConnectors.clear();
		for (int i = 0; i < ctlist.length; i++)
			childConnectors.add(ctlist[i]);
	}
	
	public long getCanvasUUID() {
		return this.cuid;
	}

	public void setCanvasUUID(long cuid) {
		this.cuid = cuid;
	}

	public void addChildStroke(long u) {
		if (!this.childStrokes.contains(u))
			this.childStrokes.add(u);
	}

	public void addChildGroup(long grpUUID, int x, int y) {
		if (!this.childGroups.contains(grpUUID))
			this.childGroups.add(grpUUID);
		
		if (CGroupController.exists(grpUUID))
		{
			CGroup child = CGroupController.groups.get(grpUUID);
			if (child.getParentUUID() != this.uuid)
				child.setParentUUID(this.uuid);
		}		
	}

	public void deleteChildStroke(long u) {
		this.childStrokes.remove(u);
	}

	public long[] getChildStrokes() {
		return childStrokes.toLongArray();
	}

	public void deleteChildGroup(long u) {
		// logger.debug("REMOVING CHILD GROUP "+u+" to PARENT "+this.uuid);
		if (this.childGroups.contains(u))
			this.childGroups.remove(u);
	}

	public long[] getChildGroups() {
		// logger.debug("GETTING CHILD GROUPS OF PARENT "+this.uuid);
		return childGroups.toLongArray();
	}

	/**
	 * Have we received the GROUP_FINISH packet already?
	 * 
	 * @return
	 */
	public boolean isFinished() {
		return this.finished;
	}

	public void setChildrenColor(Color col) {
		int r = col.getRed();
		int g = col.getGreen();
		int b = col.getBlue();

		/*
		 * // Set the color for all BGElements for(int
		 * i=0;i<childBGElements.size();i++) { CStrokeController.strokes.get(
		 * childBGElements.getLong(i) ).setColor(r, g, b); }
		 * 
		 * // Call the set color for all group elements too. for(int
		 * i=0;i<childGroups.size();i++) { CGroupController.groups.get(
		 * childGroups.getLong(i) ).setChildrenColor(col); }
		 */

	}

	public Point getPosition() {
		return new Point(this.points.xpoints[0], this.points.ypoints[0]);
	}

	/**
	 * Finish the CGroup, this will then get all the children inside of it, and
	 * then send out a packet to notify all clients of the changes.
	 */
	public void finish() {
		if (isFinished()) {
			return;
		}
		this.finished = true;

		this.groupArea = Geometry.computePolygonArea(this.points);
		
//		smoothedPath = ;
		pathReferenceShadow = Geometry.getBezieredPoly(points);

		// Notify the clients of the perm status of this group.
		setPermanent(isPermanent());
		applyAffineTransform();
	}

	public CalicoPacket[] getParentingUpdatePackets() {
		CalicoPacket[] packets = new CalicoPacket[3];

		// SET PARENT
//		packets[0] = CalicoPacket.getPacket(NetworkCommand.GROUP_SET_PARENT,
//				this.uuid, this.puid);

		int basePacketSize = ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_LONG
				+ ByteUtils.SIZE_OF_SHORT;

		// / Child
		long[] bgelist = getChildStrokes();

		packets[0] = new CalicoPacket(basePacketSize
				+ (bgelist.length * ByteUtils.SIZE_OF_LONG));
		packets[0].putInt(NetworkCommand.GROUP_SET_CHILD_STROKES);
		packets[0].putLong(this.uuid);
		packets[0].putCharInt(bgelist.length);
		if (bgelist.length > 0) {
			for (int i = 0; i < bgelist.length; i++) {
				packets[0].putLong(bgelist[i]);
			}
		}

		long[] grplist = getChildGroups();
		packets[1] = new CalicoPacket(basePacketSize
				+ (grplist.length * ByteUtils.SIZE_OF_LONG));
		packets[1].putInt(NetworkCommand.GROUP_SET_CHILD_GROUPS);
		packets[1].putLong(this.uuid);
		packets[1].putCharInt(grplist.length);
		if (grplist.length > 0) {
			for (int i = 0; i < grplist.length; i++) {
				packets[1].putLong(grplist[i]);
			}
		}
		// end child

		long[] arlist = getChildArrows();
		packets[2] = new CalicoPacket(basePacketSize
				+ (arlist.length * ByteUtils.SIZE_OF_LONG));
		packets[2].putInt(NetworkCommand.GROUP_SET_CHILD_ARROWS);
		packets[2].putLong(this.uuid);
		packets[2].putCharInt(arlist.length);
		if (arlist.length > 0) {
			for (int i = 0; i < arlist.length; i++) {
				packets[2].putLong(arlist[i]);
			}
		}
		
		ArrayList<CalicoPacket> totalPackets = new ArrayList<CalicoPacket>(Arrays.asList(packets));
//		for (int i = 0; i < bgelist.length; i++)
//			totalPackets.add(CalicoPacket.getPacket(NetworkCommand.STROKE_SET_PARENT, bgelist[i], this.uuid));
//		for (int i = 0; i < grplist.length; i++)
//			totalPackets.add(CalicoPacket.getPacket(NetworkCommand.GROUP_SET_PARENT, grplist[i], this.uuid));

		return totalPackets.toArray(new CalicoPacket[0]);
	}

	/**
	 * This sends an update that contains the list of child elements of this
	 * object. This is used so that the client doesn't need to maintain anything
	 * 
	 * @return CalicoPacket
	 */
	public CalicoPacket notifyOfChildItems() {
		// This sends a GROUP_SET_CHILDREN packet out

		long[] grplist = this.childGroups.toLongArray();
		long[] bgelist = this.childStrokes.toLongArray();

		int packetSize = 8 + (grplist.length * 8) + (bgelist.length * 8); // 

		CalicoPacket pack = new CalicoPacket(NetworkCommand.GROUP_SET_CHILDREN,
				packetSize);
		pack.putLong(this.uuid);
		pack.putInt(bgelist.length);
		pack.putInt(grplist.length);

		if (bgelist.length > 0) {
			for (int i = 0; i < bgelist.length; i++) {
				pack.putLong(bgelist[i]);
			}
		}

		if (grplist.length > 0) {
			for (int i = 0; i < grplist.length; i++) {
				pack.putLong(grplist[i]);
			}
		}

		return pack;
	}

	public void drop() {
		// TODO: Finish

		// Unset the parents for all bgelements
		if (childStrokes.size() > 0) {
			long[] bgelist = childStrokes.toLongArray();
			for (int i = 0; i < bgelist.length; i++) {
				// Unset the parent
				CStrokeController.strokes.get(bgelist[i]).setParentUUID(0L);

				// Notify the clients
				ClientManager.send(CalicoPacket.getPacket(
						NetworkCommand.BGE_PARENT, bgelist[i], 0L));
			}
		}

		if (childArrows.size() > 0) {
			long[] arsrps = childArrows.toLongArray();
			for (int i = 0; i < arsrps.length; i++) {
				CArrowController.arrows.get(arsrps[i]).delete();
			}
		}

		// Unset all group parents
		if (childGroups.size() > 0) {
			long[] grplist = childGroups.toLongArray();
			for (int i = 0; i < grplist.length; i++) {
				// Unset the parent
				CGroupController.groups.get(grplist[i]).setParentUUID(0L);

				// Notify clients
				ClientManager.send(CalicoPacket.getPacket(
						NetworkCommand.GROUP_SET_PARENT, grplist[i], 0L));
			}
		}

		// Remove it from its parent
		if (getParentUUID() != 0L) {
			CGroupController.groups.get(getParentUUID()).deleteChildGroup(uuid);

			ClientManager.send(CGroupController.groups.get(getParentUUID())
					.notifyOfChildItems());
		}

		// Notify the clients that we deleted this group
		ClientManager.send(CalicoPacket.getPacket(NetworkCommand.GROUP_DROP,
				uuid));

		// Remove from canvas
		CCanvasController.canvases.get(cuid).deleteChildGroup(uuid);

		// Now remove from the backend
		CGroupController.groups.remove(uuid);

	}
	
	public boolean containsPoint(int x, int y) {
		if (getPathReference() != null)
			return getPathReference().contains(x, y);
		else
			return false;
	}

	public boolean containsShape(Shape shape)
	{
		if (shape == null)
			return false;
		Polygon polygon = Geometry.getPolyFromPath(shape.getPathIterator(null));
		GeneralPath containerGroup = getPathReference();
		if (containerGroup == null)
			return false;
		int totalNotContained = 0;
		for(int i=0;i<polygon.npoints;i++)
		{
			if (!containerGroup.contains(new Point(polygon.xpoints[i], polygon.ypoints[i])))
			{
				totalNotContained++;
			}
			if (totalNotContained > polygon.npoints*.1)
				return false;
		}
		return true;
	}
	
	public void delete() {
		// Just some cleanup.
		this.points = null;
		delete(true);
	}
	
	public void delete(boolean recurse) {

		if (recurse) {

			long[] child_strokes = getChildStrokes();
			long[] child_groups = getChildGroups();
			long[] child_arrows = getChildArrows();
			long[] child_connectors = getChildConnectors();

			// Reparent any strokes
			if (child_strokes.length > 0) {
				for (int i = 0; i < child_strokes.length; i++) {
					CStrokeController.no_notify_delete(child_strokes[i]);
				}
			}

			// Reparent any groups
			if (child_groups.length > 0) {
				for (int i = 0; i < child_groups.length; i++) {
					CGroupController.no_notify_delete(child_groups[i]);
				}
			}

			if (child_arrows.length > 0) {
				for (int i = 0; i < child_arrows.length; i++) {
					CArrowController.no_notify_delete(child_arrows[i]);
				}
			}
			
			if (child_connectors.length > 0) {
				for (int i = 0; i < child_connectors.length; i++) {
					CConnectorController.no_notify_delete(child_connectors[i]);
				}
			}
		}

		// remove from parent
		if (this.puid != 0L) {
			CGroupController.no_notify_remove_child_group(this.puid, this.uuid);
		}

		// Remove from the canvas
		CCanvasController.no_notify_remove_child_group(this.cuid, this.uuid);
	}
	
	public void recheckParentAfterMove() {
		Point2D mid = getMidPoint();
		recheckParentAfterMove((int)mid.getX(), (int)mid.getY());
	}
	
	public void recheckParentAfterMove(boolean b) {
		Point2D mid = getMidPoint();
		recheckParentAfterMove((int)mid.getX(), (int)mid.getY(), b);
	}

	public void recheckParentAfterMove(int x, int y) {
		recheckParentAfterMove(x, y, true);
	}

	public void recheckParentAfterMove(int x, int y, boolean sendPackets) {
		// This checks to make sure we havent moved the group In
		
		long oldParentUUID = getParentUUID();

		//decorators will remove children manually
		if (CGroupController.groups.get(oldParentUUID) instanceof CGroupDecorator)
			return;
		
		long smallestGUID = calculateParent(x, y);

		if (oldParentUUID == smallestGUID
			|| smallestGUID == 0l)
			return;
		
		logger.trace("GROUP " + this.uuid + " WILL PARENT TO " + smallestGUID);

		if (oldParentUUID != 0L) {
			CGroupController.no_notify_remove_child_group(oldParentUUID,
					this.uuid);
		}

		CGroupController.no_notify_remove_child_group(oldParentUUID, this.uuid);
		CGroupController.groups.get(smallestGUID).addChildGroup(this.uuid, x, y);
		
//		if (sendPackets) {
//			CGroupController.set_parent(this.uuid, smallestGUID);
//		} else {
			CGroupController.no_notify_set_parent(this.uuid, smallestGUID);
//		}

	}

	public long calculateParent(int x, int y) {
		long smallestGUID = 0L;
		double smallestGroupArea = Double.MAX_VALUE;

		// Now, we check all other groups.
		long[] grouparr = CCanvasController.canvases.get(cuid).getChildGroups();
		if (grouparr.length > 0) {
			for (int i = 0; i < grouparr.length; i++) {
				if (grouparr[i] != this.uuid
						&& CGroupController.groups.get(grouparr[i]).isPermanent()
						&& this.isPermanent()
						&& smallestGroupArea > CGroupController.groups.get(grouparr[i]).getArea()
						&& CGroupController.canParentChild(grouparr[i], this.uuid, x, y)) 
				{
					smallestGroupArea = CGroupController.groups.get(grouparr[i]).getArea();
					smallestGUID = grouparr[i];
					logger.trace("GROUP " + this.uuid + " CAN PARENT TO "
							+ grouparr[i]);
				}
			}
		}
		return smallestGUID;
	}
	
	/**
	 * @param potentialParent
	 * @param child
	 * @return
	 */
	public boolean canParentChild(long child, int x, int y)
	{
		//A child cannot be parented to nothing or itself
		if (child == 0l || child == this.uuid)
			return false;
		
		//The child cannot be already be parented to a decorator because
		//	this causes crazy things to happen with things like the list.
		if (CGroupController.exists(child)
				&& CGroupController.groups.get(child).getParentUUID() != 0l
				&& CGroupController.groups.get(CGroupController.getDecoratorParent(
						CGroupController.groups.get(child).getParentUUID()))
					instanceof CGroupDecorator)
			return false;
		
		//The parent must exist, and cannot be parented to a decorator
		if (CGroupController.exists(getParentUUID())
				&& CGroupController.groups.get(getParentUUID()) instanceof CGroupDecorator)
			return false;
				
		if (CStrokeController.strokes.containsKey(child))
		{
			 if (!CGroupController.group_contains_stroke(this.uuid, child))
				 return false;
		}
		else if (CGroupController.groups.containsKey(child))
		{
			CGroup childGroup = CGroupController.groups.get(child);
			//the area of the child must be smaller than the area of the parent
			 if (getArea() < childGroup.getArea())
				 return false;
			 //the parent must completely contain the child
			 if (!CGroupController.group_contains_group(this.uuid, childGroup.uuid))
				 return false;		 
			 //The child should not be a temp scrap
			 if (!childGroup.isPermanent())
				 return false;
		}

		//We don't want to parent something if we can parent its ancestor. I.e., if 
		//	we select a scrap, we don't want to select all of the content on top of 
		//	that scrap too
		if (CGroupController.group_can_parent_ancestor(this.uuid, child,x,y))
			return false;
		
		return true;
	}

	public CalicoPacket[] getUpdatePackets(long uuid, long cuid, long puid, int dx, int dy, boolean captureChildren) {

//		if (COptions.client.network.cluster_size >= this.points.npoints) {
			// WE CAN SEND A STROKE_LOAD SINGLE PACKET

			int packetSize = ByteUtils.SIZE_OF_INT
					+ (3 * ByteUtils.SIZE_OF_LONG) + ByteUtils.SIZE_OF_BYTE
					+ ByteUtils.SIZE_OF_SHORT
					+ (2 * this.points.npoints * ByteUtils.SIZE_OF_SHORT)
					+ CalicoPacket.getSizeOfString(this.text);

			CalicoPacket packet = new CalicoPacket(packetSize);
			// UUID CUID PUID <COLOR> <NUMCOORDS> x1 y1
			packet.putInt(networkLoadCommand);
			packet.putLong(uuid);
			packet.putLong(cuid);
			packet.putLong(puid);
			packet.putBoolean(this.isPermanent);
			packet.putCharInt(this.points.npoints);
			for (int j = 0; j < this.points.npoints; j++) {
				packet.putInt(this.points.xpoints[j] + dx);
				packet.putInt(this.points.ypoints[j] + dy);
			}
			packet.putBoolean(captureChildren);
			packet.putDouble(this.rotation);
			packet.putDouble(this.scaleX);
			packet.putDouble(this.scaleY);
			packet.putString(this.text);

			return new CalicoPacket[] {packet};
			
			// TODO: FIX THIS
//			if (!(getText().length() > 0))
//				return new CalicoPacket[] { packet };
//			else
//				return new CalicoPacket[] {
//						packet,
//						CalicoPacket.getPacket(NetworkCommand.GROUP_SET_TEXT,
//								uuid, this.text) };
//		} else {
//
//			Polygon pointtemp = CalicoUtils.copyPolygon(this.points);
//
//			int numPackets = 5;
//
//			for (int i = 0; i < pointtemp.npoints; i = i
//					+ COptions.client.network.cluster_size) {
//				numPackets++;
//			}
//
//			CalicoPacket[] packets = new CalicoPacket[numPackets];
//
//			packets[0] = CalicoPacket
//					.getPacket(NetworkCommand.GROUP_START, uuid,
//							cuid, puid, (this.isPermanent ? 1 : 0));
//			int packetIndex = 1;
//
//			for (int i = 0; i < pointtemp.npoints; i = i
//					+ COptions.client.network.cluster_size) {
//				int numingroup = (i + COptions.client.network.cluster_size) > pointtemp.npoints ? (pointtemp.npoints - i)
//						: COptions.client.network.cluster_size;
//
//				packets[packetIndex] = new CalicoPacket(
//						(2 * numingroup * ByteUtils.SIZE_OF_CHAR)
//								+ ByteUtils.SIZE_OF_INT
//								+ ByteUtils.SIZE_OF_CHAR
//								+ ByteUtils.SIZE_OF_LONG);
//				packets[packetIndex]
//						.putInt(NetworkCommand.GROUP_APPEND_CLUSTER);
//				packets[packetIndex].putLong(uuid);
//				packets[packetIndex].putCharInt(numingroup);
//
//				for (int j = 0; j < numingroup; j++) {
//					packets[packetIndex].putInt(pointtemp.xpoints[i + j] + dx);
//					packets[packetIndex].putInt(pointtemp.ypoints[i + j] + dy);
//				}
//				packetIndex++;
//			}
//
//			packets[packetIndex++] = CalicoPacket.getPacket(
//					NetworkCommand.GROUP_FINISH, uuid, captureChildren);
//			packets[packetIndex++] = CalicoPacket.getPacket(
//					NetworkCommand.GROUP_ROTATE, uuid, this.rotation);
//			packets[packetIndex++] = CalicoPacket.getPacket(
//					NetworkCommand.GROUP_SCALE, uuid, this.scaleX, this.scaleY);
//			packets[packetIndex] = CalicoPacket.getPacket(
//						NetworkCommand.GROUP_SET_TEXT, uuid, this.text);
//
//			return packets;
//		}
	}
	
	public CalicoPacket[] getUpdatePackets(boolean captureChildren)
	{
		return getUpdatePackets(this.uuid, this.cuid, this.puid, 0, 0, captureChildren);
	}

	// a negative integer, zero, or a positive integer as this object is less
	// than, equal to, or greater than the specified object.
	public int compareTo(CGroup grp) {
		if (getArea() < grp.getArea()) {
			return -1;
		} else if (getArea() > grp.getArea()) {
			return 1;
		} else {
			return 0;
		}
	}

	// NEW

	public void calculateParenting(boolean includeStrokes, int x, int y) {

		// Check the bounds for the other items on the canvas.

		long[] grouparr = CCanvasController.canvases.get(this.cuid)
				.getChildGroups();
		long[] bgearr = CCanvasController.canvases.get(this.cuid)
				.getChildStrokes();
		long[] ararr = CCanvasController.canvases.get(this.cuid)
				.getChildArrows();

		// Check to see if any groups are inside of this.
		if (grouparr.length > 0) {
			for (int i = 0; i < grouparr.length; i++) {
				if (CGroupController.canParentChild(this.uuid, grouparr[i], x, y)
						&& CGroupController.group_contains_group(this.uuid, grouparr[i])) 
				{
					// it is contained in the group, so set it's parent
					CGroupController.no_notify_set_parent(grouparr[i], this.uuid);
				}
			}
		}

		// Send the BGElement Parents
		if (includeStrokes && bgearr.length > 0) {
			for (int i = 0; i < bgearr.length; i++) {
				// We first check to make sure this element isnt parented to
				// something else
				// then we check to see if it contained in this group
				if (CGroupController.canParentChild(this.uuid, bgearr[i], x, y)
						&& CGroupController.group_contains_stroke(this.uuid, bgearr[i])) 
				{
					// it is contained in the group, so set it's parent
					// CStrokeController.no_notify_set_parent(bgearr[i],
					// this.uuid);
					// changed by Nick
					CStrokeController.no_notify_set_parent(bgearr[i], this.uuid);
				} 
				else if (/*CStrokeController.strokes.get(bgearr[i])
						.getParentUUID() != 0L
						&&*/ CGroupController.group_contains_stroke(this.uuid, bgearr[i])) 
				{
					// Check to see if the current parent group is larger than
					// this one.
					long pguid = CStrokeController.strokes.get(bgearr[i])
							.getParentUUID();

					if (CGroupController.group_contains_group(pguid, this.uuid)
							|| !CGroupController.group_contains_stroke(pguid, bgearr[i])) {
						// it is contained in the group, so set it's parent
						CStrokeController.no_notify_set_parent(bgearr[i],
								this.uuid);
					}

				}

			}// for bgearr
		}
		
		//Check for arrows (and hey, why not use this cool flag that was already here)
		if (includeStrokes && ararr.length > 0)
		{
			CArrow arr;
			for (int i = 0; i < ararr.length; i++) {
				arr = CArrowController.arrows.get(ararr[i]);
				if (arr.getAnchorA().getType() == CArrow.TYPE_CANVAS
					|| CGroupController.group_contains_group(arr.getAnchorA().getUUID(),this.uuid)
					)
				{
					if (this.containsPoint(arr.getAnchorA().getPoint().x, arr.getAnchorA().getPoint().y))
					{
						arr.setAnchorA(new AnchorPoint(CArrow.TYPE_GROUP, arr.getAnchorA().getPoint(), this.uuid));
						addChildArrow(ararr[i]);
					}
				}
				if (arr.getAnchorB().getType() == CArrow.TYPE_CANVAS
						|| CGroupController.group_contains_group(arr.getAnchorB().getUUID(),this.uuid)
						)
				{
					if (this.containsPoint(arr.getAnchorB().getPoint().x, arr.getAnchorB().getPoint().y))
					{
						arr.setAnchorB(new AnchorPoint(CArrow.TYPE_GROUP, arr.getAnchorB().getPoint(), this.uuid));
						addChildArrow(ararr[i]);
					}
				}				
			}
		}
	}

	public void render(Graphics2D g, boolean showChildren) {
		if (this instanceof CGroupImage)
			((CGroupImage)this).render_internal(g);
		else
			render_internal(g);

		if (showChildren) {
			if (this.childGroups.size() > 0) {
				long[] uuids = getChildGroups();
				for (int i = 0; i < uuids.length; i++) {
					CGroupController.groups.get(uuids[i]).render(g,
							showChildren);
				}
			}

			if (this.childStrokes.size() > 0) {
				long[] uuids = getChildStrokes();
				for (int i = 0; i < uuids.length; i++) {
					CStrokeController.strokes.get(uuids[i]).render(g);
				}
			}
		}

	}

	protected void render_internal(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke(CGroup.groupStroke);
		g.setPaint(new Color(0,0,0, 50));
//		g.drawPolygon(points.xpoints, points.ypoints, points.npoints);
		pathReferenceShadow.closePath();
		g.draw(pathReferenceShadow);
		Color drawColor;
		if (CGroupController.groups.get(getParentUUID()) instanceof CListDecorator)
			drawColor = new Color(0, 0, 0, 50);
		else
		{
			drawColor = new Color(0x62, 0xA5, 0xCC, 50);
			g.setPaint(drawColor);// new Color( Color.BLUE.getRed(),
			g.fill(pathReferenceShadow);
		}


		// Color.BLUE.getGreen(), Color.BLUE.getBlue(),
		// 100));

		

//		g.fillPolygon(points.xpoints, points.ypoints, points.npoints);
		
		if (textSet) {
			PAffineTransform piccoloTextTransform = getPTransform();
			AffineTransform old = g.getTransform();
			g.setTransform(piccoloTextTransform);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			g.setColor(Color.BLACK);
			g.setFont(COptions.group.font);
			
//			int yTextOffset = getTextBounds(text, g).height;
			
			String[] tokens = text.split("\n");
			int yTextOffset = 0;
			for (int i = 0; i < tokens.length; i++)
			{	
				String token = tokens[i];
				g.drawString(token,
						(float) (pathReferenceShadow.getBounds().getY() + COptions.group.text_padding + COptions.group.padding), 
						(float) (pathReferenceShadow.getBounds().getY() + COptions.group.text_padding + COptions.group.padding*2.25 + yTextOffset));
				yTextOffset += getTextBounds(token, g).height;
			}
			
//			g.drawString(this.text,
//					(float) (points.getBounds().getX() + COptions.group.text_padding + COptions.group.padding), 
//					(float) (pathReferenceShadow.getBounds().getY() + COptions.group.padding + (pathReferenceShadow.getBounds().getHeight())/2 + yTextOffset/4));
			g.setTransform(old);
			
		}
	}

	public void render(Graphics2D g) {
		render(g, false);
	}

	public void setPolygon(Polygon p) {
		points = p;
//		coordsOriginal = new Polygon(p.xpoints, p.ypoints, p.npoints);

		Polygon areaTemp = new Polygon();
		for (int i = 0; i < this.points.npoints; i++) {
			areaTemp.addPoint(this.points.xpoints[i], this.points.ypoints[i]);
		}

		// Area
		this.groupArea = Geometry.computePolygonArea(areaTemp);
	}

	private Rectangle getTextBounds(String t, Graphics2D g) {
//		Graphics2D g2d = (Graphics2D) new BufferedImage(16, 16,
//				BufferedImage.TYPE_INT_RGB).getGraphics();
		FontRenderContext frc = g.getFontRenderContext();
		Rectangle fontBounds = COptions.group.font.getStringBounds(t, frc)
				.getBounds();
		return fontBounds;
	}
	
	public void clearChildGroups() {
		this.childGroups.clear();
		this.childGroups = new LongArraySet();
	}

	public void clearChildStrokes() {
		this.childStrokes.clear();
		this.childStrokes = new LongArraySet();
	}

	public void clearChildArrows() {
		this.childArrows.clear();
		this.childArrows = new LongArraySet();
	}
	
	public void clearChildConnectors() {
		this.childConnectors.clear();
		this.childConnectors = new LongArraySet();
	}

	// TODO: Finish this
	public Properties toProperties() {
		Properties props = new Properties();

		props.setProperty("uuid", "" + this.uuid);
		props.setProperty("puid", "" + this.puid);
		props.setProperty("cuid", "" + this.cuid);
		props.setProperty("area", "" + this.groupArea);
		props.setProperty("perm", "" + this.isPermanent);

		int[] pointsprint = new int[2 * this.points.npoints];
		int pointind = 0;
		for (int i = 0; i < this.points.npoints; i++) {
			pointsprint[pointind++] = this.points.xpoints[i];
			pointsprint[pointind++] = this.points.ypoints[i];
		}

		props.setProperty("points", Arrays.toString(pointsprint));

		props.setProperty("child.groups", Arrays.toString(getChildGroups()));
		props.setProperty("child.strokes", Arrays.toString(getChildStrokes()));
		props.setProperty("child.arrows", Arrays.toString(getChildArrows()));
		props.setProperty("child.connectors", Arrays.toString(getChildConnectors()));

		return props;
	}

	public static CGroup getGroup(long uuid) {
		return CGroupController.groups.get(uuid);
	}

	public Rectangle getBoundsOfObjects(long[] children) {
		if (children.length == 0)
			return new Rectangle(0, 0, 0, 0);

		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
		Rectangle bounds;

		// iterate through child strokes
		for (int i = 0; i < children.length; i++) {
			if (CStrokeController.strokes.containsKey(children[i])) {
				if (CStrokeController.strokes.get(children[i]) == null
						|| CStrokeController.strokes.get(children[i]).getPathReference().getBounds().x == 0)
					continue;
				bounds = CStrokeController.strokes.get(children[i]).getPathReference().getBounds();
//				printBounds("Stroke " + children[i], bounds);
			} 
			else if (CGroupController.groups.containsKey(children[i]))
			{
				bounds = CGroupController.groups.get(children[i]).getPathReference().getBounds();
//				printBounds("Group " + children[i], bounds);
			}
			else
				continue;
			if (bounds.x < minX)
				minX = bounds.x;
			if (bounds.y < minY)
				minY = bounds.y;
			if (bounds.x + bounds.width > maxX)
				maxX = bounds.x + bounds.width;
			if (bounds.y + bounds.height > maxY)
				maxY = bounds.y + bounds.height;
		}

		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}

	public String getText() {
		return text;
	}

	public Rectangle getBoundsOfObjects(Rectangle[] objects) {
		if (objects.length == 0)
			return new Rectangle(0, 0, 0, 0);

		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

		Rectangle bounds;
		// iterate through child strokes
		for (int i = 0; i < objects.length; i++) {
			bounds = objects[i];
			if (bounds.x < minX)
				minX = bounds.x;
			if (bounds.y < minY)
				minY = bounds.y;
			if (bounds.x + bounds.width > maxX)
				maxX = bounds.x + bounds.width;
			if (bounds.y + bounds.height > maxY)
				maxY = bounds.y + bounds.height;
		}

		return new Rectangle(minX, minY, maxX - minX, maxY - minY);
	}

	public Rectangle getBoundsOfContents() {
		ArrayList<Rectangle> boundsOfContainedElements = new ArrayList<Rectangle>();

		long[] strokearr = this.getChildStrokes();
		long[] grouparr = this.getChildGroups();
		long[] childIndices = ArrayUtils.addAll(strokearr, grouparr);

		Rectangle childrenBounds = getBoundsOfObjects(childIndices);
		if (childrenBounds.width < 1 || childrenBounds.height < 1)
			childrenBounds = this.getPathReference().getBounds();
		boundsOfContainedElements.add(childrenBounds);

		Rectangle textDimensions = getTextBounds(this.text, (Graphics2D) new BufferedImage(16, 16,
					BufferedImage.TYPE_INT_RGB).getGraphics());
		Rectangle textBounds;
		textBounds = new Rectangle(childrenBounds.x, childrenBounds.y,
				textDimensions.width + COptions.group.text_padding*2, textDimensions.height
						+ COptions.group.text_padding*2);
		if (textBounds.width > 0 && textBounds.height > 0)
			boundsOfContainedElements.add(textBounds);

		return getBoundsOfObjects(boundsOfContainedElements
				.toArray(new Rectangle[] {}));
	}

	public void shrinkToContents() {
		setShapeToRoundedRectangle(getBoundsOfContents());
	}

	public void setShapeToRoundedRectangle(Rectangle newBounds) {
		setShapeToRoundedRectangle(newBounds, COptions.group.padding);
	}
	
	public void setShapeToRoundedRectangle(Rectangle newBounds, int padding) {
		if (newBounds.width == 0 && newBounds.height == 0)
			return;

		if (points != null)
			points.reset();
		points = Geometry.getRoundedPolygon(newBounds, padding);
		
		this.groupArea = Geometry.computePolygonArea(this.points);
//		smoothedPath = 
		pathReferenceShadow = Geometry.getBezieredPoly(points);
		
		scaleX = 1.0d;
		scaleY = 1.0d;
		rotation = 0.0d;
	}
	
	public void printBounds()
	{
		printBounds(new Long(uuid).toString(), getPathReference().getBounds());
	}
	
	private void printBounds(String id, Rectangle bounds)
	{
		System.out.println("\n\t\t" + id + ": (" + bounds.x + ", " + bounds.y + ", " + 
				bounds.width + ", " + bounds.height + ")");
	}

	private void applyAffineTransform() {
		PAffineTransform piccoloTextTransform = getPTransform();
		GeneralPath p = (GeneralPath) Geometry.getBezieredPoly(points).createTransformedShape(piccoloTextTransform);
		pathReferenceShadow = p;
		this.groupArea = Geometry.computePolygonArea(Geometry.getPolyFromPath(p.getPathIterator(null)));
	}

	public PAffineTransform getPTransform() {
		PAffineTransform piccoloTextTransform = new PAffineTransform();
		Point2D midPoint = Geometry.getMidPoint2D(points);
		piccoloTextTransform.rotate(rotation, midPoint.getX(), midPoint.getY());
		piccoloTextTransform.scaleAboutPoint(scaleX, midPoint.getX(), midPoint.getY());
		return piccoloTextTransform;
	}
	
	public Point2D getMidPoint()
	{
		return Geometry.getMidPoint2D(points);
	}

	public void rotate(double radians) {
		rotate(radians, Geometry.getMidPoint2D(points));
	}

	public void rotate(double radians, Point2D pivotPoint) {
		System.out.println("Performing rotate on (" + this.uuid + ") at point (" + pivotPoint.getX() + ", " + pivotPoint.getY() + " with scale  " + rotation);
		
		AffineTransform rotateAboutPivot = AffineTransform.getRotateInstance(radians, pivotPoint.getX(), pivotPoint.getY());
		
		//1) compute mid point translation
		Point2D oldMidPoint = Geometry.getMidPoint2D(points);
		Point2D newMidPoint = null;
		newMidPoint = rotateAboutPivot.transform(oldMidPoint, newMidPoint);
		int deltaX = (int)(newMidPoint.getX() - oldMidPoint.getX());
		int deltaY = (int)(newMidPoint.getY() - oldMidPoint.getY());
		points.translate(deltaX, deltaY);
//		smoothedPath.transform(AffineTransform.getTranslateInstance(deltaX, deltaY));
		
		//2) compute actual rotation change
		Rectangle2D bounds = points.getBounds2D();
		Point2D oldBoundsRightCorner = new Point2D.Double(bounds.getX() + bounds.getWidth(), bounds.getY());
		Point2D newBoundsRightCorner = null;
		newBoundsRightCorner = rotateAboutPivot.transform(oldBoundsRightCorner, newBoundsRightCorner);
		
		double angleOld = calico.utils.Geometry.angle(oldMidPoint, oldBoundsRightCorner);
		double angleNew = calico.utils.Geometry.angle(newMidPoint, newBoundsRightCorner);
		double actualRotation = angleNew - angleOld;
		primative_rotate(actualRotation + rotation);
		
		if (childArrows.size() > 0) {
			long[] auid = childArrows.toLongArray();
			Point2D ptSrc = new Point2D.Double();
			Point2D ptDst = new Point2D.Double();
			for (int i = 0; i < auid.length; i++) {
				ptSrc = new Point(CArrowController.arrows.get(auid[i]).getAnchor(uuid).getPoint());
				rotateAboutPivot.transform(ptSrc, ptDst);
				CArrowController.no_notify_move_group_anchor(auid[i], uuid, Math.round((float)ptDst.getX() - (float)ptSrc.getX()),
						Math.round((float)ptDst.getY() - (float)ptSrc.getY()));
			}
		}
		
		if (childConnectors.size() > 0) {
			long[] cuid = childConnectors.toLongArray();
			Point2D ptSrc = new Point2D.Double();
			Point2D ptDst = new Point2D.Double();
			for (int i = 0; i < cuid.length; i++) {
				if (CConnectorController.connectors.get(cuid[i]).getAnchorUUID(CConnector.TYPE_HEAD) == uuid)
				{
					ptSrc = new Point(CConnectorController.connectors.get(cuid[i]).getHead());
					rotateAboutPivot.transform(ptSrc, ptDst);
					CConnectorController.no_notify_move_group_anchor(cuid[i], CConnector.TYPE_HEAD, Math.round((float)ptDst.getX() - (float)ptSrc.getX()),
							Math.round((float)ptDst.getY() - (float)ptSrc.getY()));
				}
				if (CConnectorController.connectors.get(cuid[i]).getAnchorUUID(CConnector.TYPE_TAIL) == uuid)
				{
					ptSrc = new Point(CConnectorController.connectors.get(cuid[i]).getTail());
					rotateAboutPivot.transform(ptSrc, ptDst);
					CConnectorController.no_notify_move_group_anchor(cuid[i], CConnector.TYPE_TAIL, Math.round((float)ptDst.getX() - (float)ptSrc.getX()),
							Math.round((float)ptDst.getY() - (float)ptSrc.getY()));
				}
			}
		}
		
		for (long g : childGroups)
		{
			if (CGroupController.is_parented_to(g, this.uuid))
				CGroupController.groups.get(g).rotate(radians, pivotPoint);
			else {
				logger.error("GROUP " + this.uuid
						+ " is trying to translate kidnapped group " + g);
				CGroupController.no_notify_remove_child_group(this.uuid, g);
			}
		}
		
		for (long s : childStrokes)
		{
			if (CStrokeController.strokes.containsKey(s))
			{
				if (CStrokeController.is_parented_to(s, this.uuid))
					CStrokeController.strokes.get(s).rotate(radians, pivotPoint);
				else {
					logger.error("GROUP " + this.uuid
							+ " is trying to trasnlate kidnapped stroke " + s);
					CGroupController.no_notify_remove_child_stroke(this.uuid,s);
				}
			}
		}
		
		recomputeBounds();
	}

	public void primative_rotate(double actualRotation) {
		rotation = actualRotation;
		applyAffineTransform();
	}

	public void scale(double scaleX, double scaleY)
	{
		Rectangle2D bounds = points.getBounds2D();
		scale(scaleX,scaleY,new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()));
	}
	
	public void scale(double scaleX, double scaleY, Point2D pivotPoint) {
		System.out.println("Performing scale on (" + this.uuid + ") at point (" + pivotPoint.getX() + ", " + pivotPoint.getY() + " with scale " + scaleX);
		
		AffineTransform scaleAboutPivot1 = AffineTransform.getTranslateInstance(-1 * pivotPoint.getX(), -1 * pivotPoint.getY());
		AffineTransform scaleAboutPivot2 = AffineTransform.getScaleInstance(scaleX, scaleY);
		AffineTransform scaleAboutPivot3 = AffineTransform.getTranslateInstance(pivotPoint.getX(), pivotPoint.getY());
		
		//1) compute mid point translation
		Point2D oldMidPoint = Geometry.getMidPoint2D(points);
		Point2D newMidPoint = null;
		newMidPoint = scaleAboutPivot1.transform(oldMidPoint, newMidPoint);
		newMidPoint = scaleAboutPivot2.transform(newMidPoint, null);
		newMidPoint = scaleAboutPivot3.transform(newMidPoint, null);
		int deltaX = new Double(newMidPoint.getX() - oldMidPoint.getX()).intValue();
		int deltaY = new Double(newMidPoint.getY() - oldMidPoint.getY()).intValue();
		points.translate(deltaX, deltaY);
//		smoothedPath.transform(AffineTransform.getTranslateInstance(deltaX, deltaY));
		
		//2) assign actual scale
		primative_scale(this.scaleX * scaleX, this.scaleY * scaleY);
		
		if (childArrows.size() > 0) {
			long[] auid = childArrows.toLongArray();
			Point2D ptSrc = new Point2D.Double();
			Point2D ptDst = new Point2D.Double();
			for (int i = 0; i < auid.length; i++) {
				ptSrc = new Point(CArrowController.arrows.get(auid[i]).getAnchor(uuid).getPoint());
				ptDst = scaleAboutPivot1.transform(ptSrc, ptDst);
				ptDst = scaleAboutPivot2.transform(ptDst, null);
				ptDst = scaleAboutPivot3.transform(ptDst, null);
				CArrowController.no_notify_move_group_anchor(auid[i], uuid, Math.round((float)ptDst.getX() - (float)ptSrc.getX()),
						Math.round((float)ptDst.getY() - (float)ptSrc.getY()));
			}
		}
		
		if (childConnectors.size() > 0) {
			long[] cuid = childConnectors.toLongArray();
			Point2D ptSrc = new Point2D.Double();
			Point2D ptDst = new Point2D.Double();
			for (int i = 0; i < cuid.length; i++) {
				if (CConnectorController.connectors.get(cuid[i]).getAnchorUUID(CConnector.TYPE_HEAD) == uuid)
				{
					ptSrc = new Point(CConnectorController.connectors.get(cuid[i]).getHead());
					ptDst = scaleAboutPivot1.transform(ptSrc, ptDst);
					ptDst = scaleAboutPivot2.transform(ptDst, null);
					ptDst = scaleAboutPivot3.transform(ptDst, null);
					CConnectorController.no_notify_move_group_anchor(cuid[i], CConnector.TYPE_HEAD, Math.round((float)ptDst.getX() - (float)ptSrc.getX()),
							Math.round((float)ptDst.getY() - (float)ptSrc.getY()));
				}
				if (CConnectorController.connectors.get(cuid[i]).getAnchorUUID(CConnector.TYPE_TAIL) == uuid)
				{
					ptSrc = new Point(CConnectorController.connectors.get(cuid[i]).getTail());
					ptDst = scaleAboutPivot1.transform(ptSrc, ptDst);
					ptDst = scaleAboutPivot2.transform(ptDst, null);
					ptDst = scaleAboutPivot3.transform(ptDst, null);
					CConnectorController.no_notify_move_group_anchor(cuid[i], CConnector.TYPE_TAIL, Math.round((float)ptDst.getX() - (float)ptSrc.getX()),
							Math.round((float)ptDst.getY() - (float)ptSrc.getY()));
				}
			}
		}
		
		for (long g : childGroups)
		{
			if (CGroupController.is_parented_to(g, this.uuid))
				CGroupController.groups.get(g).scale(scaleX, scaleY, pivotPoint);
			else {
				logger.error("GROUP " + this.uuid
						+ " is trying to translate kidnapped group " + g);
				CGroupController.no_notify_remove_child_group(this.uuid, g);
			}
		}
		
		for (long s : childStrokes)
		{
			if (CStrokeController.strokes.containsKey(s))
			{
				if (CStrokeController.is_parented_to(s, this.uuid))
					CStrokeController.strokes.get(s).scale(scaleX, scaleY, pivotPoint);
				else {
					logger.error("GROUP " + this.uuid
							+ " is trying to trasnlate kidnapped stroke " + s);
					CGroupController.no_notify_remove_child_stroke(this.uuid,s);
				}
			}
		}
		
		recomputeBounds();
	}

	public void primative_scale(double scaleX, double scaleY) {
		this.scaleX = scaleX;
		this.scaleY = scaleY;
		applyAffineTransform();
	}
	
	public void unparentAllChildren()
	{
		for (int i = getChildGroups().length-1; i >= 0; i--)
			CGroupController.no_notify_set_parent(getChildGroups()[i], 0l);
		for (int i = getChildStrokes().length-1; i >= 0; i--)
			CStrokeController.no_notify_set_parent(getChildStrokes()[i], 0l);
		for (long childArrow : getChildArrows())
		{
			CArrow tempArrow = CArrowController.arrows.get(childArrow);
			if (tempArrow.getAnchorA().getUUID() == this.uuid)
				tempArrow.setAnchorA(new AnchorPoint(CArrow.TYPE_CANVAS, tempArrow.getAnchorA().getPoint(), this.uuid));
			if (tempArrow.getAnchorB().getUUID() == this.uuid)
				tempArrow.setAnchorB(new AnchorPoint(CArrow.TYPE_CANVAS, tempArrow.getAnchorB().getPoint(), this.uuid));
		}
		for (long childConnector : getChildConnectors())
		{
			CConnector tempConnector = CConnectorController.connectors.get(childConnector);
			if (tempConnector.getAnchorUUID(CConnector.TYPE_HEAD) == this.uuid)
				tempConnector.setAnchorUUID(0l, CConnector.TYPE_HEAD);
			if (tempConnector.getAnchorUUID(CConnector.TYPE_TAIL) == this.uuid)
				tempConnector.setAnchorUUID(0l, CConnector.TYPE_TAIL);
		}
		
		childGroups.clear();
		childStrokes.clear();
		childArrows.clear();
		childConnectors.clear();
	}

	public double getRotation() {
		return rotation;
	}

	public double getScale() {
		return scaleX;
	}
	
	public long getUUID() {
		return this.uuid;
	}
	
	public boolean canParent(Shape s, double area)
	{
		if (area < -1)
			area = Geometry.computePolygonArea(Geometry.getPolyFromPath(s.getPathIterator(null)));
		if (this.containsShape(s) && this.groupArea > area)
			return true;
		
		return false;
	}
	
	public boolean canParent(Shape s)
	{
		return canParent(s, -1);
	}
	
	public void recomputeBounds()
	{
		if (CGroupController.exists(puid))
			CGroupController.groups.get(puid).recomputeBounds();
	}
	
	public void recomputeValues()
	{
		if (CGroupController.exists(puid))
			CGroupController.groups.get(puid).recomputeValues();
	}

	public int get_signature() {
		int sig = this.points.npoints + this.points.xpoints[0]
		      + this.points.ypoints[0] + this.text.length() + (int)(this.rotation*10) + (int)(this.scaleX*10) + (int)(this.scaleY*10);
		if (isPermanent()) {
			sig++;
		}
//		System.out.println("Debug sig for group " + uuid + ": " + sig + ", 1) " + this.points.npoints + ", 2) " + isPermanent() + ", 3) " + this.points.xpoints[0] + ", 4) " + this.points.xpoints[0] + ", 5) " + this.points.ypoints[0] + ", 6) " + (int)(this.rotation*10) + ", 7) " + (int)(this.scaleX*10) + ", 8) " + (int)(this.scaleY*10));
		return sig;
	}
	
	public String get_signature_debug_output()
	{
		return "Debug sig for group " + uuid + ": 1) " + this.points.npoints + ", 2) " + isPermanent() + ", 3) " + this.text.length() + ", 4) " + this.points.xpoints[0] + ", 5) " + this.points.ypoints[0] + ", 6) " + (int)(this.rotation*10) + ", 7) " + (int)(this.scaleX*10) + ", 8) " + (int)(this.scaleY*10);
	}
	
	public long getTopmostParent()
	{
		long uuid = this.uuid;
		long parentUUID = this.puid;
		
		while (CGroupController.exists(parentUUID))
		{
			uuid = parentUUID;
			parentUUID = CGroupController.groups.get(uuid).getParentUUID();
		}
		
		return uuid;
	}

}
