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

import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;

import calico.components.CGroup;
import calico.controllers.CArrowController;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CStrokeController;
import calico.networking.netstuff.ByteUtils;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

public abstract class CGroupDecorator extends CGroup {

	protected long decoratedGroupUUID;
	
	/**
	 * This should be called when it's being restored or loaded from the network.
	 * 
	 * @param guuid
	 * @param uuid
	 * @param puuid
	 */
	public CGroupDecorator(long guuid, long uuid, long cuuid, long puuid)
	{
		super(uuid, cuuid, puuid, true);
		this.decoratedGroupUUID = guuid;
		super.addChildGroup(decoratedGroupUUID, 0, 0);
		
		//all decorators are already finished
		this.finished = true;
		setNetworkCommand();
	}

	/**
	 * This constructor steals the parent, should be called when it's being
	 * initialized for the first time
	 * 
	 * @param guuid
	 * @param uuid
	 */
	public CGroupDecorator(long guuid, long uuid) {
		this(guuid, uuid, CGroupController.groups.get(guuid).getCanvasUUID(),
				CGroupController.groups.get(guuid).getParentUUID());

		if (CGroupController.exists(this.puid))
			CGroupController.groups.get(this.puid).addChildGroup(uuid, 0, 0);
		CGroupController.groups.get(guuid).setParentUUID(uuid);
		
//		int numChildren = 1;
//		
//		int basePacketSize = ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_LONG
//		+ ByteUtils.SIZE_OF_SHORT;
//		CalicoPacket packet = new CalicoPacket(basePacketSize
//				+ (1 * ByteUtils.SIZE_OF_LONG));
//		packet.putInt(NetworkCommand.GROUP_SET_CHILD_GROUPS);
//		packet.putLong(uuid);
//		packet.putCharInt(1);
//		packet.putLong(guuid);
//		ClientManager.send(packet);

	}

	protected CGroup getDecoratedGroup() {
		if (CGroupController.exists(decoratedGroupUUID))
			return CGroupController.groups.get(decoratedGroupUUID);
		else
			return null;
	}

	public void move(int x, int y) {
		getDecoratedGroup().move(x, y);
	}

	public GeneralPath getPathReference() {
		if (getDecoratedGroup() == null)
			return null;
		return getDecoratedGroup().getPathReference();
	}

	public Polygon getRawPolygon() {
		return getDecoratedGroup().getRawPolygon();
	}

	public double getArea() {
		return getDecoratedGroup().getArea();
	}

	public boolean hasChildGroup(long childuuid) {
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

	public void addChildStroke(long u) {		if (getDecoratedGroup() == null)
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
	public Point2D getMidPoint() {
		if (getDecoratedGroup() != null)
			return getDecoratedGroup().getMidPoint();
		else
			return new Point2D.Double(0, 0);
	}

	@Override
	public void setShapeToRoundedRectangle(Rectangle newBounds) {
		getDecoratedGroup().setShapeToRoundedRectangle(newBounds);
	}

	@Override
	public CalicoPacket[] getParentingUpdatePackets() {
		CalicoPacket[] packets = new CalicoPacket[4];

		// SET PARENT
		packets[0] = CalicoPacket.getPacket(NetworkCommand.GROUP_SET_PARENT,
				this.uuid, this.puid);

		int basePacketSize = ByteUtils.SIZE_OF_INT + ByteUtils.SIZE_OF_LONG
				+ ByteUtils.SIZE_OF_SHORT;

		// / Child
		long[] bgelist = super.getChildStrokes();

		packets[1] = new CalicoPacket(basePacketSize
				+ (bgelist.length * ByteUtils.SIZE_OF_LONG));
		packets[1].putInt(NetworkCommand.GROUP_SET_CHILD_STROKES);
		packets[1].putLong(this.uuid);
		packets[1].putCharInt(bgelist.length);
		if (bgelist.length > 0) {
			for (int i = 0; i < bgelist.length; i++) {
				packets[1].putLong(bgelist[i]);
			}
		}

		long[] grplist = super.getChildGroups();
		packets[2] = new CalicoPacket(basePacketSize
				+ (grplist.length * ByteUtils.SIZE_OF_LONG));
		packets[2].putInt(NetworkCommand.GROUP_SET_CHILD_GROUPS);
		packets[2].putLong(this.uuid);
		packets[2].putCharInt(grplist.length);
		if (grplist.length > 0) {
			for (int i = 0; i < grplist.length; i++) {
				packets[2].putLong(grplist[i]);
			}
		}
		// end child

		long[] arlist = super.getChildArrows();
		packets[3] = new CalicoPacket(basePacketSize
				+ (arlist.length * ByteUtils.SIZE_OF_LONG));
		packets[3].putInt(NetworkCommand.GROUP_SET_CHILD_ARROWS);
		packets[3].putLong(this.uuid);
		packets[3].putCharInt(arlist.length);
		if (arlist.length > 0) {
			for (int i = 0; i < arlist.length; i++) {
				packets[3].putLong(arlist[i]);
			}
		}

		ArrayList<CalicoPacket> totalPackets = new ArrayList<CalicoPacket>(
				Arrays.asList(packets));
		for (int i = 0; i < bgelist.length; i++)
			totalPackets.add(CalicoPacket.getPacket(
					NetworkCommand.STROKE_SET_PARENT, bgelist[i], this.uuid));
		for (int i = 0; i < grplist.length; i++)
			totalPackets.add(CalicoPacket.getPacket(
					NetworkCommand.GROUP_SET_PARENT, grplist[i], this.uuid));

		return totalPackets.toArray(new CalicoPacket[0]);
	}

	@Override
	public void delete() {
		CGroup decoratedGroup = getDecoratedGroup();
		if (decoratedGroup != null) {
			long[] child_strokes = decoratedGroup.getChildStrokes();
			long[] child_groups = decoratedGroup.getChildGroups();
			long[] child_arrows = decoratedGroup.getChildArrows();

			// Reparent any strokes
			if (child_strokes.length > 0) {
				for (int i = child_strokes.length-1; i >= 0; i--) {
					CStrokeController.no_notify_delete(child_strokes[i]);
				}
			}

			// Reparent any groups
			if (child_groups.length > 0) {
				for (int i = 0; i < child_groups.length; i++) {
					CGroupController.groups.get(child_groups[i]).delete();
				}
			}

			if (child_arrows.length > 0) {
				for (int i = child_groups.length-1; i >= 0; i--) {
					CArrowController.no_notify_delete(child_arrows[i]);
				}
			}

			decoratedGroup.clearChildGroups();
			CCanvasController.no_notify_remove_child_group(this.cuid,
					this.decoratedGroupUUID);
		}

		super.clearChildGroups();
		// remove this from parent
		if (this.puid != 0L) {
			CGroupController.no_notify_remove_child_group(this.puid, this.uuid);
		}

		// Remove from the canvas

		CCanvasController.no_notify_remove_child_group(this.cuid, this.uuid);
	}

	public long getDecoratedUUID() {
		return decoratedGroupUUID;
	}

	public CalicoPacket[] getUpdatePackets(boolean captureChildren) {
		CalicoPacket[] ret = {};
		try
		{
			ret = getDecoratorUpdatePackets(this.uuid, this.cuid, this.puid, this.getDecoratedUUID());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return ret;
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
	public void setPermanent(boolean temp) 
	{
		super.deleteChildGroup(decoratedGroupUUID);
		CGroupController.groups.get(decoratedGroupUUID).setParentUUID(super.getParentUUID());
		super.delete();
	}

}
