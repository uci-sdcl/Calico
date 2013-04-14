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
package calico.components.decorators;

import it.unimi.dsi.fastutil.longs.Long2ReferenceArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.SwingUtilities;

import calico.CalicoDraw;
import calico.components.CGroup;
import calico.controllers.CArrowController;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.inputhandlers.CalicoInputManager;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;

public abstract class CGroupDecorator extends CGroup {

	protected long decoratedGroupUUID;
	
	static {
		
	}
	
	/**
	 * This should be called when it's being restored or loaded from the network.
	 * @param guuid
	 * @param uuid
	 * @param puuid
	 */
	public CGroupDecorator(long guuid, long uuid, long cuuid, long puuid)
	{
		super(uuid, cuuid, puuid, true);
		this.decoratedGroupUUID = guuid;
		super.addChildGroup(decoratedGroupUUID, 0,0);
		
		//all decorators are already finished
		this.finished = true;
		setNetworkCommand();
		if (getDecoratedGroup() != null)
		{
			super.setPathTo(getDecoratedGroup().getPathReference());
			CGroupController.groupdb.get(guuid).setParentUUID(uuid);
		}
	}
	
	/**
	 * This constructor steals the parent, should be called when it's being initialized for the first time
	 * @param guuid
	 * @param uuid
	 */
	public CGroupDecorator(long guuid, long uuid)
	{
		this(guuid, uuid, CGroupController.groupdb.get(guuid).getCanvasUID(), CGroupController.groupdb.get(guuid).getParentUUID());
				
		if (CGroupController.exists(this.puid))
			CGroupController.groupdb.get(this.puid).addChildGroup(uuid, 0, 0);
		CGroupController.groupdb.get(guuid).setParentUUID(uuid);
	}
	
	/**
	 * Decorators will only ever have one child, and when that child is set, it means that the decoratedGroup was loaded
	 */
	public void setChildGroups(long[] gplist) 
	{
		super.setChildGroups(gplist);
		if (getDecoratedGroup() != null)
			super.setPathTo(getDecoratedGroup().getPathReference());
	}
	
	protected CGroup getDecoratedGroup()
	{
		if (CGroupController.exists(decoratedGroupUUID))
			return CGroupController.groupdb.get(decoratedGroupUUID);
		else
			return null;
	}

	public void move(int x, int y) {
		if (getDecoratedGroup() == null)
			return;
		
		getDecoratedGroup().move(x, y);
		super.setPathTo(getDecoratedGroup().getPathReference());
	}
	
	public void rotate(double radians, Point2D pivotPoint)
	{
		if (getDecoratedGroup() == null)
			return;
		
		super.rotate(radians, pivotPoint);
		super.setPathTo(getDecoratedGroup().getPathReference());
	}
	
	public void scale(double x, double y, Point2D pivotPoint)
	{
		if (getDecoratedGroup() == null)
			return;
		
		super.scale(x, y, pivotPoint);
		super.setPathTo(getDecoratedGroup().getPathReference());
	}
	
	public GeneralPath getPathReference()
	{
		if (getDecoratedGroup() == null)
			return null;
		
		return getDecoratedGroup().getPathReference();
	}
	
	public Polygon getRawPolygon()
	{
		if (getDecoratedGroup() == null)
			return null;
		
		return getDecoratedGroup().getRawPolygon();
	}
	
	public double getArea() 
	{
		if (getDecoratedGroup() == null)
			return 0l;
		
		return getDecoratedGroup().getArea();
	}
	
	public boolean hasChildGroup(long childuuid)
	{
		if (getDecoratedGroup() == null)
			return false;
		
		return getDecoratedGroup().hasChildGroup(childuuid) || childuuid == decoratedGroupUUID;
	}

	public void addChildArrow(long uid) {
		
		if (getDecoratedGroup() == null)
			return;
		
		getDecoratedGroup().addChildArrow(uid);
	}

	public void deleteChildArrow(long uid) {
		if (getDecoratedGroup() == null)
			return;
		
		getDecoratedGroup().deleteChildArrow(uid);
	}

	public long[] getChildArrows() {
		if (getDecoratedGroup() == null)
			return null;
		
		return this.childArrows.toLongArray();
	}
	
	public void addChildStroke(long u) {
		if (getDecoratedGroup() == null)
			return;
		
		getDecoratedGroup().addChildStroke(u);
	}

	@Override
	public void addChildGroup(long grpUUID, int x, int y) {
		if (getDecoratedGroup() == null)
			return;
		
		if (grpUUID == getDecoratedGroup().getUUID() || grpUUID == this.uuid)
			return;
		getDecoratedGroup().addChildGroup(grpUUID, x, y);		
	}

	public void deleteChildStroke(long u) {
		if (getDecoratedGroup() == null)
			return;
		
		getDecoratedGroup().deleteChildStroke(u);
	}

	public long[] getChildStrokes() {
		if (getDecoratedGroup() == null)
			return null;
		
		return getDecoratedGroup().getChildStrokes();
	}

	public void deleteChildGroup(long u) {
		if (getDecoratedGroup() == null)
			return;
		
		if (u == getDecoratedGroup().getUUID())
			return;
		getDecoratedGroup().deleteChildGroup(u);
	}

	public long[] getChildGroups() {
		if (getDecoratedGroup() == null)
			return null;
		
		return getDecoratedGroup().getChildGroups();
	}
	
	protected abstract void setNetworkCommand();
	
	@Override
	public void delete() {
		CGroup decoratedGroup = getDecoratedGroup();
		if (decoratedGroup != null)
		{
			long[] child_strokes = decoratedGroup.getChildStrokes();
			long[] child_groups = decoratedGroup.getChildGroups();
			long[] child_arrows = decoratedGroup.getChildArrows();
	
			// Reparent any strokes
			decoratedGroup.setChildStrokes(new long[] { });
			if (child_strokes.length > 0) {
				for (int i = child_strokes.length-1; i >= 0; i--) {
					CStrokeController.no_notify_delete(child_strokes[i]);
				}
			}
	
			// Reparent any groups
			decoratedGroup.setChildGroups(new long[] { });
			if (child_groups.length > 0) {
				for (int i = child_groups.length-1; i >= 0; i--) {
					if (!CGroupController.exists(child_groups[i])) { continue; };
					CGroupController.groupdb.get(child_groups[i]).delete();
					if(CCanvasController.canvas_has_child_group_node(CGroupController.groupdb.get(child_groups[i]).getCanvasUID(), child_groups[i]))
					{
						/*final long tempUUID = child_groups[i];
						SwingUtilities.invokeLater(
								new Runnable() { public void run() { 
										CGroupController.groupdb.get(tempUUID).removeFromParent();
								}});*/
						CalicoDraw.removeNodeFromParent(CGroupController.groupdb.get(child_groups[i]));
					}
					CGroupController.dq_add(child_groups[i]);
				}
			}
	
			if (child_arrows.length > 0) {
				for (int i = 0; i < child_arrows.length; i++) {
					CArrowController.no_notify_delete(child_arrows[i]);
				}
			}
	
			if(CCanvasController.canvas_has_child_group_node(CGroupController.groupdb.get(this.decoratedGroupUUID).getCanvasUID(), this.decoratedGroupUUID))
			{
				/*final long tempUUID = this.decoratedGroupUUID;
				SwingUtilities.invokeLater(
						new Runnable() { public void run() { 
								CGroupController.groupdb.get(tempUUID).removeFromParent();
						}});*/
				CalicoDraw.removeNodeFromParent(CGroupController.groupdb.get(this.decoratedGroupUUID));
			}
			CGroupController.dq_add(this.decoratedGroupUUID);
			decoratedGroup.clearChildGroups();
			CCanvasController.no_notify_delete_child_group(this.cuid, this.decoratedGroupUUID);
		}
		
		super.clearChildGroups();
		// remove this from parent
		if (this.puid != 0L) {
			CGroupController.no_notify_delete_child_group(this.puid, this.uuid);
		}

		// Remove from the canvas
		
		CCanvasController.no_notify_delete_child_group(this.cuid, this.uuid);
	}
	
	@Override
	public Image getFamilyPicture()
	{
		if (getDecoratedGroup() == null)
			return null;
		
		return getDecoratedGroup().getFamilyPicture();
	}
	
	@Override
	public Point2D getMidPoint()
	{
		if (getDecoratedGroup() == null)
			return null;
		
		return getDecoratedGroup().getMidPoint();
	}
	
	@Override
	public void setShapeToRoundedRectangle(Rectangle newBounds) {
		if (getDecoratedGroup() == null)
			return;
		
		getDecoratedGroup().setShapeToRoundedRectangle(newBounds);
	}
	
	@Override
	public PBounds getBounds()
	{
		if (getDecoratedGroup() == null)
			return null;
		
		return getDecoratedGroup().getBounds();
	}
	
	public CalicoPacket[] getUpdatePackets(boolean captureChildren) {
		return getDecoratorUpdatePackets(this.uuid, this.cuid, this.puid, this.getDecoratedUUID());
	}
	
	public CalicoPacket[] getUpdatePackets(long uuid, long cuid, long puid, int dx, int dy, boolean captureChildren) {
		return getDecoratorUpdatePackets(this.uuid, this.cuid, this.puid, this.getDecoratedUUID());
	}
	
	public CalicoPacket[] getDecoratorUpdatePackets(long uuid, long cuid, long puid, long decorated_uuid) {
		return new CalicoPacket[] { CalicoPacket.getPacket(this.networkLoadCommand, uuid, cuid, puid, decorated_uuid) };
	}

	public CalicoPacket[] getDecoratorUpdatePackets(long uuid, long cuid, long puid, long decorated_uuid, Long2ReferenceArrayMap<Long> subGroupMappings) {
		return new CalicoPacket[] { CalicoPacket.getPacket(this.networkLoadCommand, uuid, cuid, puid, decorated_uuid) };
	}
	
	@Override
	public CalicoPacket[] getParentingUpdatePackets() {
		CalicoPacket[] packets = new CalicoPacket[3];

		// SET PARENT
//		packets[0] = CalicoPacket.getPacket(NetworkCommand.GROUP_SET_PARENT,
//				this.uuid, this.puid);
		

		int basePacketSize = ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_LONG
				+ ByteUtils.SIZE_OF_SHORT;

		// / Child
		long[] bgelist = super.getChildStrokes();

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

		long[] grplist = super.getChildGroups();
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

		long[] arlist = super.getChildArrows();
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

		ArrayList<CalicoPacket> totalPackets = new ArrayList<CalicoPacket>(
				Arrays.asList(packets));
//		for (int i = 0; i < bgelist.length; i++)
//			totalPackets.add(CalicoPacket.getPacket(
//					NetworkCommand.STROKE_SET_PARENT, bgelist[i], this.uuid));
//		for (int i = 0; i < grplist.length; i++)
//			totalPackets.add(CalicoPacket.getPacket(
//					NetworkCommand.GROUP_SET_PARENT, grplist[i], this.uuid));

		return totalPackets.toArray(new CalicoPacket[0]);
	}
	
	public long getDecoratedUUID() {
		return decoratedGroupUUID;
	}

}
