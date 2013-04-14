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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;

import calico.Calico;
import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.piemenu.PieMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CGroupController;
import calico.controllers.CGroupDecoratorController;
import calico.controllers.CStrokeController;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.CalicoInputManager;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.util.PPaintContext;

@SuppressWarnings("serial")
public class CListDecorator extends CGroupDecorator {
	
	int iconWidth = 16, iconHeight = 16, iconWidthBuffer = 4;
	int iconXSpace = this.iconWidth + this.iconWidthBuffer*2;
	int widthBuffer = 5;
	
	private final boolean debugList = false;
	
	Image checkIcon, uncheckedIcon;
	
	static
	{
		CGroupDecoratorController.registerGroupDecoratorCommands("LIST");
	}

	public CListDecorator(long guuid, long uuid, long cuuid, long puuid) {
		super(guuid, uuid, cuuid, puuid);
		initializeCheckValues();
		if (debugList)
			System.out.println("Created list! guuid: " + guuid + ", uuid: " + uuid);
	}
	
	public CListDecorator(long guuid, long uuid)
	{
		super(guuid, uuid);
		initializeCheckValues();
		if (debugList)
			System.out.println("Created list! guuid: " + guuid + ", uuid: " + uuid);
	}

	@Override
	protected void paint(final PPaintContext paintContext) {
		if (getDecoratedGroup() == null)
			return;
		
		final Graphics2D g2 = paintContext.getGraphics();
		
		getDecoratedGroup().setPaint(Color.white);
		this.setPaint(Color.white);
		
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
			       0.3f));
		
		super.paint(paintContext);
		
		if (BubbleMenu.highlightedParentGroup == this.uuid)
		{
			if (CGroupController.exists(CalicoInputManager.group) && CalicoInputManager.group != this.uuid)
			{
				if (this.containsPoint(CalicoInputManager.mostRecentPoint.x, CalicoInputManager.mostRecentPoint.y))
				{
					g2.setColor(Color.blue);
					g2.draw(getNearestLine());
					this.repaintFrom(this.getBounds(), this);
					//CalicoDraw.repaintNode(this);
				}
			}
		}
		
		long[] childGroups = this.getChildGroups();
		
		Image checkImage;
		if (childGroups != null)
		{
			Rectangle[] iconBounds = getCheckIconBounds();
			for (int i = 0; i < childGroups.length; i++)
			{
				if (CGroupDecoratorController.groupCheckValues.containsKey(childGroups[i]))
				{
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
						       1.0f));
					checkImage = (CGroupDecoratorController.groupCheckValues.get(childGroups[i]).booleanValue())
						? checkIcon
						: uncheckedIcon;
					g2.drawImage(checkImage, iconBounds[i].x, iconBounds[i].y, iconBounds[i].width, iconBounds[i].height, null);
					
				}
			}
		}
		else
			System.out.println();
	}
	
	@Override
	protected void setNetworkCommand() {
		this.networkLoadCommand = NetworkCommand.LIST_LOAD;
	}
	
	@Override
	public void setChildGroups(long[] gplist) {
		super.setChildGroups(gplist);
		initializeCheckValues();
//		resetListElementPositions();
	}
	
	@Override
	public void addChildGroup(long grpUUID, int x, int y) {
//		System.out.println("Before add child:");
//		printBounds();		
		if (CGroupController.exists(grpUUID) && !CGroupController.groupdb.get(grpUUID).isPermanent())
			return;
		
		super.addChildGroup(grpUUID, x, y);
		if (!CGroupDecoratorController.groupCheckValues.containsKey(grpUUID))
			CGroupDecoratorController.groupCheckValues.put(grpUUID, new Boolean(false));
		
		resetListElementPositions(grpUUID, x, y);
		
		recomputeBounds();
		recomputeValues();
		
		Calico.logger.debug("Group added to list: " + grpUUID);
//		System.out.println("After add child:");
//		printBounds();
	}
	
	@Override
	public void deleteChildGroup(long grpUUID) {
		super.deleteChildGroup(grpUUID);
//		CCanvasController.canvasdb.get(this.cuid).getLayer().removeChild(CGroupDecoratorController.groupImages.get(grpUUID));
//		groupImages.remove(grpUUID);
		
		resetListElementPositions();
		
		recomputeBounds();
		recomputeValues();

		Calico.logger.debug("Group removed from list: " + grpUUID);
	}
	
	public void setCheck(long guuid, boolean value)
	{
		CGroupDecoratorController.groupCheckValues.put(guuid, new Boolean(value));
//		CCanvasController.canvasdb.get(this.cuid).getLayer().removeChild(this.groupImages.get(guuid));
		
//		if (CGroupDecoratorController.groupImages.containsKey(guuid))
//		{
//			Rectangle bounds = CGroupDecoratorController.groupImages.get(guuid).getBounds().getBounds();
////			this.groupImages.remove(guuid);
//			setIcon(guuid, bounds.x, bounds.y);
//		}
		//this.invalidatePaint();
		CalicoDraw.invalidatePaint(this);
		//this.repaint();
		CalicoDraw.repaint(this);
	}
	
	public boolean isChecked(long guuid)
	{
		if (!CGroupDecoratorController.groupCheckValues.containsKey(guuid)) { return false; }
		else
			return CGroupDecoratorController.groupCheckValues.get(guuid).booleanValue();
	}
	
	@Override
	public boolean containsShape(Shape shape)
	{
//		printBounds();
//		System.out.println("Shape: " + shape.getBounds2D().getCenterX() + ", " + shape.getBounds2D().getCenterY());
		return this.containsPoint((int)shape.getBounds2D().getCenterX(), (int)shape.getBounds2D().getCenterY());
	}
	
	public CalicoPacket[] getDecoratorUpdatePackets(long uuid, long cuid, long puid, long decorated_uuid) {
		CalicoPacket[] packets = new CalicoPacket[getChildGroups().length + 1];
		packets[0] = CalicoPacket.getPacket(NetworkCommand.LIST_LOAD, this.uuid, this.cuid, this.puid, getDecoratedGroup().getUUID());
		long[] keySet = getChildGroups();
		for (int i = 0; i < keySet.length; i++)
		{
			packets[i+1] = CalicoPacket.getPacket(NetworkCommand.LIST_CHECK_SET, this.uuid, this.cuid, this.puid, keySet[i], CGroupDecoratorController.groupCheckValues.get(keySet[i]).booleanValue());
		}
		return packets;
	}
	
	@Override
	public CalicoPacket[] getDecoratorUpdatePackets(long uuid, long cuid, long puid, long decorated_uuid, Long2ReferenceArrayMap<Long> subGroupMappings) {
		CalicoPacket[] superPackets = super.getDecoratorUpdatePackets(uuid, cuid, puid, decorated_uuid, subGroupMappings);
		long[] keySet = subGroupMappings.keySet().toLongArray();
		
		CalicoPacket[] packets = new CalicoPacket[keySet.length + superPackets.length];
		
		for (int i = 0; i < superPackets.length; i++)
			packets[i] = superPackets[i];
		
		
		for (int i = 0; i < keySet.length; i++)
		{
			boolean checkValue = false;
			long subGroupMapping = 0l;
			if (CGroupDecoratorController.groupCheckValues.get(keySet[i]) != null)
				checkValue = CGroupDecoratorController.groupCheckValues.get(keySet[i]).booleanValue();
			if (subGroupMappings.get(keySet[i]) != null)
				subGroupMapping = subGroupMappings.get(keySet[i]).longValue();
			
			packets[i+superPackets.length] = CalicoPacket.getPacket(NetworkCommand.LIST_CHECK_SET, uuid, cuid, puid, subGroupMapping, checkValue);
		}
		return packets;
	}
	
	@Override
	public void recomputeBounds()
	{
		if (getDecoratedGroup() != null && getDecoratedGroup().getPathReference() != null)
		{
			resetListElementPositions();
			
			Rectangle bounds = getDecoratedGroup().getBoundsOfContents();
			
			Rectangle newBounds = new Rectangle(bounds.x - widthBuffer - iconXSpace, bounds.y,
					bounds.width + widthBuffer*2 + iconXSpace, bounds.height);
			
			CGroupController.no_notify_make_rectangle(getDecoratedUUID(), newBounds.x, newBounds.y, newBounds.width, newBounds.height);
			
			
			//this.invalidatePaint();
			CalicoDraw.invalidatePaint(this);
			//this.repaint();
			CalicoDraw.repaint(this);
			//getDecoratedGroup().repaint();
			CalicoDraw.repaint(getDecoratedGroup());
		}
		super.recomputeBounds();
	}
	
	@Override
	public void recomputeValues()
	{
		if (getDecoratedGroup() == null)
			return;
		
		long[] childGroups = getDecoratedGroup().getChildGroups();

		for (int i = 0; i < childGroups.length; i++)
		{
			if (CGroupController.groupdb.get(childGroups[i]) instanceof CListDecorator)
			{
				CListDecorator innerList = ((CListDecorator)CGroupController.groupdb.get(childGroups[i]));
				if (innerList.getChildGroups().length > 0)
				{
					boolean containsUnchecked = false;
					long[] innerListGroups = innerList.getChildGroups();
					for (int j = 0; j < innerListGroups.length; j++)
						if (CGroupDecoratorController.groupCheckValues.containsKey(innerListGroups[j])
								&& CGroupDecoratorController.groupCheckValues.get(innerListGroups[j]).booleanValue() == false)
							containsUnchecked = true;
					
					
					if (containsUnchecked)
//						CGroupDecoratorController.groupCheckValues.put(childGroups[i], new Boolean(false));
						setCheck(childGroups[i], false);
					else
//						CGroupDecoratorController.groupCheckValues.put(childGroups[i], new Boolean(true));
						setCheck(childGroups[i], true);
				}
			}
		}
		super.recomputeValues();
	}
	
	@Override
	public void move(int x, int y)
	{
		super.move(x, y);
		ArrayList<PImage> groupIcons = new ArrayList<PImage>();
//		long[] childgroups = getChildGroups();
//		for (int i = 0; i < childgroups.length; i++)
//		{
//			groupIcons.add(CGroupDecoratorController.groupImages.get(childgroups[i]));
//		}
		
		
		Rectangle bounds;
		for (PImage icon : groupIcons)
		{
			if (icon == null || icon.getBounds() == null)
				continue;
			bounds = icon.getBounds().getBounds();
			bounds.translate(x, y);
			//icon.setBounds(bounds);
			CalicoDraw.setNodeBounds(icon, bounds);
		}
	}
	
	public void resetListElementPositions()
	{
		resetListElementPositions(false);
	}
	
	public void resetListElementPositions(boolean setLocationToFirstElement)
	{
		resetListElementPositions(setLocationToFirstElement, 0l, 0, 0);
	}
	
	public void resetListElementPositions(long guuid, int g_x, int g_y)
	{
		resetListElementPositions(false, guuid, g_x, g_y);
	}
	
	public void resetListElementPositions(boolean setLocationToFirstElement, long guuid, int g_x, int g_y) {
		
		int moveToX, moveToY, deltaX, deltaY, elementSpacing = 5;
		
		
		int yOffset = /*elementSpacing + */0;
		int widestWidth = 0;
		int x, y;
		
		if (setLocationToFirstElement && getChildGroups().length > 0)
		{
			long firstchild = getChildGroups()[0];
			x = CGroupController.groupdb.get(firstchild).getPathReference().getBounds().x - widthBuffer - iconXSpace; //  bounds.x; // + widthBuffer / 2;
			y = CGroupController.groupdb.get(firstchild).getPathReference().getBounds().y - yOffset; //bounds.y; // + elementSpacing / 2;
		}
		else 
		{
			if (getPathReference() == null)
				return;
			
			x = this.getPathReference().getBounds().x + CalicoOptions.group.padding;
			y = this.getPathReference().getBounds().y + CalicoOptions.group.padding;
		}
		
		Rectangle bounds; // = getDecoratedGroup().getPathReference().getBounds();
		
		long[] listElements = getChildGroups();
		
		listElements = insertGroupByYPosition(listElements, guuid, g_x, g_y);
		
		if (listElements.length < 1 || CGroupController.groupdb.get(listElements[0]) == null || CGroupController.groupdb.get(listElements[0]).getPathReference() == null)
			return;
		
		if (debugList)
		{
			Rectangle lb = getBounds().getBounds();
			System.out.printf("List (%d)/\n\t bounds: (%d,%d,%d,%d)", this.uuid, lb.x, lb.y, lb.width, lb.height);
			System.out.println("");
		}
		for (int i = 0; i < listElements.length; i++)
		{
			if (!CGroupController.exists(listElements[i]))
				continue;
			
			//destination
			moveToX = x + widthBuffer + iconXSpace;
			moveToY = y;
			
			//figure out offset
			bounds = CGroupController.groupdb.get(listElements[i]).getPathReference().getBounds();
			deltaX = moveToX - bounds.x;
			deltaY = moveToY - bounds.y;
			
			if (debugList)
			{
				System.out.printf("\t%d: %d, %d, %d, %d", i, moveToX, moveToY + yOffset, bounds.width, bounds.height);
				System.out.println("");
			}
			
			CGroupController.no_notify_move(listElements[i], deltaX, deltaY + yOffset);
			yOffset += bounds.height + elementSpacing;
			
			//check for the widest width
			if (bounds.width > widestWidth)
				widestWidth = bounds.width;
		}
		if (listElements.length == 0)
		{
			widestWidth = 100;
			yOffset = 50;
		}

	}
	
	/**
	 * This method assumes that the given array is already ordered by their Y position
	 * 
	 * @param listElements
	 * @param guuid
	 * @param gX
	 * @param gY
	 * @return
	 */
	private long[] insertGroupByYPosition(long[] listElements, long guuid,
			int gX, int gY) {
		
		if (!CGroupController.exists(guuid))
			return listElements;
		
		long[] retList = listElements.clone(); 
		
		for (int i = 0; i < retList.length; i++)
			if (retList[i] == guuid)
				retList = ArrayUtils.removeElement(retList, guuid);
				

		for (int i = 0; i < retList.length; i++)
		{
			if (CGroupController.groupdb.get(retList[i]).getMidPoint().getY() > gY)
			{
				retList = ArrayUtils.add(retList, i, guuid);
				break;
			}
		}
		
		if (retList.length > 0)
		{
			if (CGroupController.groupdb.get(retList[retList.length-1]).getMidPoint().getY() < gY)
				retList = ArrayUtils.add(retList, guuid);
		}
		
		return retList;
	}

	public Rectangle[] getCheckIconBounds()
	{
		int moveToY, elementSpacing = 5, widthBuffer = 5;
		int iconXSpace = this.iconWidth + this.iconWidthBuffer*2;
		
		int yOffset = elementSpacing + 0;
		int x, y;
		
		long[] listElements = getChildGroups();
		
		if (listElements == null || listElements.length == 0)
			return new Rectangle[] { };
		
		long firstchild = listElements[0];
		if (!CGroupController.exists(firstchild))
			return new Rectangle[] { };
		
		x = CGroupController.groupdb.get(firstchild).getPathReference().getBounds().x - widthBuffer - iconXSpace; //  bounds.x; // + widthBuffer / 2;
		y = CGroupController.groupdb.get(firstchild).getPathReference().getBounds().y - yOffset; //bounds.y; // + elementSpacing / 2;
		
		Rectangle bounds;
		
		
		Rectangle[] checkMarkBounds = new Rectangle[listElements.length];
		
		if (listElements.length < 1 || CGroupController.groupdb.get(listElements[0]) == null || CGroupController.groupdb.get(listElements[0]).getPathReference() == null)
			return null;
		
		for (int i = 0; i < listElements.length; i++)
		{
			if (!CGroupController.exists(listElements[i]))
				continue;
			
			//destination
			moveToY = y;
			
			//figure out offset
			bounds = CGroupController.groupdb.get(listElements[i]).getPathReference().getBounds();

			checkMarkBounds[i] = new Rectangle(x + iconWidthBuffer, moveToY + yOffset + bounds.height/2 - iconHeight/2, this.iconWidth, this.iconHeight);
			yOffset += bounds.height + elementSpacing;
		}
		
		return checkMarkBounds;
	}
	
//	private void removeAllCheckMarks() {
//		if (getDecoratedGroup() == null)
//			return;
//		
//		long[] listElements = getDecoratedGroup().getChildGroups();
//		
////		long[] keyset = CGroupDecoratorController.groupImages.keySet().toLongArray();
//		for (int i = 0; i < listElements.length; i++)
//		{
////			CCanvasController.canvasdb.get(cuid).getLayer().removeChild(CGroupDecoratorController.groupImages.get(listElements[i]));
//			CGroupDecoratorController.groupImages.remove(listElements[i]);
//		}
//		
////		LongSet keySet = groupImages.keySet();
////		for (Long key : keySet)
////		{
////			CCanvasController.canvasdb.get(cuid).getLayer().removeChild(groupImages.get(key));
////		}
//	}
	
	@Override
	public void delete()
	{
//		removeAllCheckMarks();
		super.delete();		
	}

//	private void setIcon(long l, int x, int y) {
//		
//		if (!CGroupController.exists(l))
//			return;
//		
//		Image iconImage = (CGroupDecoratorController.groupCheckValues.get(l).booleanValue())
//							? CalicoIconManager.getIconImage("lists.checked")
//							: CalicoIconManager.getIconImage("lists.unchecked");
//		
//		PImage checkIcon;
//		
//		if (CGroupDecoratorController.groupImages.containsKey(l))
//			checkIcon = CGroupDecoratorController.groupImages.get(l);
//		else
//			checkIcon = null;
//		
//		if (checkIcon == null)
//		{
//			checkIcon = new PImage();
//			CGroupDecoratorController.groupImages.put(l, checkIcon);
//		}
//
//		long canvasUID = CGroupController.groupdb.get(l).getCanvasUID();
////		if (!CCanvasController.canvasdb.get(canvasUID).getLayer().getChildrenReference().contains(checkIcon))
////			CCanvasController.canvasdb.get(canvasUID).getLayer().addChild(checkIcon);
//		
//		checkIcon.setImage(iconImage);
//		checkIcon.setBounds(x, y, this.iconWidth, this.iconHeight);
//		checkIcon.setPaintInvalid(true);
//	}

	private long[] orderByYAxis(long[] listItems)
	{
		if (getDecoratedGroup() == null)
		{
			return null;
		}
		int[] yValues = new int[listItems.length];
		
		//copy Y values to array to sort
		for (int i=0;i<listItems.length;i++)
		{
			if (CGroupController.exists(listItems[i]))
				yValues[i] = (int)CGroupController.groupdb.get(listItems[i]).getMidPoint().getY();
		}
		
		//sort the y values
		java.util.Arrays.sort(yValues);
		
		//match the y values back to their Groups and return the sorted array
		long[] sortedElementList = new long[listItems.length];
		for (int i=0;i<listItems.length;i++)
		{
			for (int j=0;j<listItems.length;j++)
			{
				if (CGroupController.exists(listItems[j]))
				{
					if ((int)CGroupController.groupdb.get(listItems[j]).getMidPoint().getY() == yValues[i])
					{
						sortedElementList[i] = listItems[j];
						//This line needed in case multiple groups have the same y value
						listItems[j] = -1;
						break;
					}
				}
			}
		}
		
		return sortedElementList;
	}
	
	public Line2D getNearestLine() {
//		Point2D midPoint = CGroupController.groupdb.get(CalicoInputManager.group).getMidPoint();
		Point2D referencePoint = new Point2D.Double(CalicoInputManager.mostRecentPoint.getX(), CalicoInputManager.mostRecentPoint.getY());
		long[] listElements = getChildGroups();
		
		int[] yPos = new int[listElements.length+1];
		int maxWidth = 0;
		Rectangle bds;
		
		for (int i = 0; i < listElements.length; i++)
		{
			if (CGroupController.groupdb.get(listElements[i]) == null)
				continue;
			bds = CGroupController.groupdb.get(listElements[i]).getPathReference().getBounds();
			yPos[i] = bds.y - 5/2;
			if (bds.width > maxWidth)
				maxWidth = bds.width;
		}
		if (listElements.length > 0)
		{
			bds = CGroupController.groupdb.get(listElements[listElements.length-1]).getPathReference().getBounds();
			yPos[yPos.length-1] = bds.y + bds.height + 5/2;
		}
		
		double smallestDistance = Integer.MAX_VALUE;
		int smallestIndex = 0;
		double distance = 0;
		for (int i = 0; i < yPos.length; i++)
		{
			if (smallestDistance > (distance = referencePoint.distance(new Point2D.Double(referencePoint.getX(), yPos[i]))))
			{
				smallestDistance = distance;
				smallestIndex = i;
			}
		}
		
		double x1 = getPathReference().getBounds2D().getX() + 10;
		double y = yPos[smallestIndex] - 1;
		int iconXSpace = this.iconWidth + this.iconWidthBuffer*2;
		
		return new Line2D.Double(x1, y, x1 + maxWidth - 4 + iconXSpace, y);
		
	}
	
	private void initializeCheckValues()
	{
		if (getDecoratedGroup() == null)
			return;
		
		checkIcon = CalicoIconManager.getIconImage("lists.checked");
		uncheckedIcon =  CalicoIconManager.getIconImage("lists.unchecked");
		
		long[] childGroups = getChildGroups().clone();
		for (int i = 0; i < childGroups.length; i++)
		{
			if (!CGroupDecoratorController.groupCheckValues.containsKey(childGroups[i]))
			{
				CGroupDecoratorController.groupCheckValues.put(childGroups[i], new Boolean(false));
			}
		}
	}
	
	@Override
	public void moveInFrontOfInternal()
	{
//		super.moveInFrontOf(node);
//		ReferenceCollection<PImage> groupIcons = CGroupDecoratorController.groupImages.values();
//		for (PImage icon : groupIcons)
//		{
//			icon.moveInFrontOf(this);
//		}
	}
	
	public long getGroupCheckMarkAtPoint(Point p)
	{
		Rectangle[] checkMarks = getCheckIconBounds();
		long[] children = getChildGroups();
		if (checkMarks == null || children == null)
			return 0l;
		
		for (int i = 0; i < checkMarks.length; i++)
			if (checkMarks[i].contains(p))
				return children[i];
		
//		LongSet keySet = CGroupDecoratorController.groupImages.keySet();
//		for (Long key : keySet)
//		{
//			if (CGroupDecoratorController.groupImages.get(key.longValue()).getBounds().contains(p))
//				return key;
//		}
		return 0l;
	}
	
	@Override
	public boolean canParentChild(long child, int x,  int y)
	{
		if (child == 0l || child == this.uuid)
			return false;
		
		if (CGroupController.exists(getParentUUID())
				&& CGroupController.groupdb.get(getParentUUID()) instanceof CGroupDecorator)
			return false;
		
		long potentialParent_new_parent = 0l;
		long child_parent = 0l;
		
		potentialParent_new_parent = getParentUUID();
		
		if (CStrokeController.strokes.containsKey(child))
		{
			return false;
		}
		else if (CGroupController.groupdb.containsKey(child))
		{
			if (!CGroupController.groupdb.get(child).isPermanent())
				return false;
			
			//must contain center of mass
			Point2D center = CGroupController.groupdb.get(child).getMidPoint();
			if (!this.containsPoint(x, y))
				return false;
			
			child_parent = CGroupController.groupdb.get(child).getParentUUID();
		}
		
		if (CGroupController.group_is_ancestor_of(child, this.uuid))
			return false;
		
		if (child_parent == 0l)
			return true;
		
		return potentialParent_new_parent == child_parent;
	}
	
	@Override
	public void rotate(double radians) 
	{
		//Do nothing
	}
	
	@Override
	public void rotate(double radians, Point2D pivotPoint) 
	{
		//Do nothing	
	}
	
	@Override
	public void scale(double scaleX, double scaleY)
	{
		//Do nothing
	}
	
	@Override
	public void scale(double scaleX, double scaleY, Point2D pivotPoint)
	{
		//Do nothing
	}

	@Override
	public long[] getChildGroups()
	{
		return orderByYAxis(super.getChildGroups());
	}
	
	@Override
	protected ObjectArrayList<Class<?>> internal_getPieMenuButtons()
	{
		ObjectArrayList<Class<?>> pieMenuButtons = new ObjectArrayList<Class<?>>(); 
		pieMenuButtons.add(calico.components.piemenu.groups.GroupDropButton.class);
//		pieMenuButtons.add(calico.components.piemenu.groups.GroupSetPermanentButton.class);
		pieMenuButtons.add(calico.components.piemenu.PieMenuButton.class);
//		pieMenuButtons.add(calico.components.piemenu.groups.GroupShrinkToContentsButton.class);
		pieMenuButtons.add(calico.components.piemenu.PieMenuButton.class);
//		pieMenuButtons.add(calico.components.piemenu.groups.ListCreateButton.class);
		pieMenuButtons.add(calico.components.piemenu.PieMenuButton.class);
//		pieMenuButtons.add(calico.components.piemenu.groups.GroupMoveButton.class);
		pieMenuButtons.add(calico.components.piemenu.groups.GroupCopyDragButton.class);
//		pieMenuButtons.add(calico.components.piemenu.groups.GroupRotateButton.class);
		pieMenuButtons.add(calico.components.piemenu.PieMenuButton.class);
		pieMenuButtons.add(calico.components.piemenu.canvas.ArrowButton.class);
		pieMenuButtons.add(calico.components.piemenu.groups.GroupDeleteButton.class);
//		pieMenuButtons.add(calico.components.piemenu.canvas.TextCreate.class);
		pieMenuButtons.add(calico.components.piemenu.PieMenuButton.class);
		return pieMenuButtons;
	}
	
	@Override
	protected ObjectArrayList<Class<?>> internal_getBubbleMenuButtons()
	{
		ObjectArrayList<Class<?>> pieMenuButtons = new ObjectArrayList<Class<?>>(); 
		pieMenuButtons.add(calico.components.piemenu.groups.GroupDropButton.class);
//		pieMenuButtons.add(calico.components.piemenu.groups.GroupMoveButton.class);
		pieMenuButtons.add(calico.components.piemenu.groups.GroupCopyDragButton.class);
		pieMenuButtons.add(calico.components.piemenu.canvas.ArrowButton.class);
		pieMenuButtons.add(calico.components.piemenu.groups.GroupDeleteButton.class);
		return pieMenuButtons;
	}
	
	
}
