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
package calico;

import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

import javax.swing.SwingUtilities;

import calico.components.CGroup;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.PRoot;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.util.PBounds;

public class CalicoDraw {

	public static void addChildToNode(final PNode parent, final PNode child)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					parent.addChild(child);
				}});
	}
	
	public static void addChildToNode(final PNode parent, final PNode child, final int index)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					parent.addChild(index, child);
				}});
	}
	
	public static void addChildrenToNode(final PNode node, final Collection nodes)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.addChildren(nodes);
				}});
	}
	
	public static void removeChildFromNode(final PNode parent, final PNode child)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					parent.removeChild(child);
				}});
	}
	
	public static void removeChildFromNode(final PNode parent, final int index)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					parent.removeChild(index);
				}});
	}
	
	public static void removeAllChildrenFromNode(final PNode node)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.removeAllChildren();
				}});
	}
	
	public static void removeNodeFromParent(final PNode node)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.removeFromParent();
				}});
	}
	
	public static void addActivityToNode(final PNode node, final PActivity activity)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.getRoot().addActivity(activity);
				}});
	}
	
	public static void setNodeTransparency(final PNode node, final float transparency)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.setTransparency(transparency);
				}});
	}
	
	public static void setNodeBounds(final PNode node, final double x, final double y, final double width, final double height)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.setBounds(x, y, width, height);
				}});
	}
	
	public static void setNodeBounds(final PNode node, final Rectangle2D newBounds)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.setBounds(newBounds);
				}});
	}
	
	public static void setNodeX(final PNode node, final double x)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.setX(x);
				}});
	}
	
	public static void setNodeY(final PNode node, final double y)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.setY(y);
				}});
	}
	
	public static void moveNodeInFrontOf(final PNode node, final PNode sibling)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.moveInFrontOf(sibling);
				}});
	}
	
	public static void moveGroupInFrontOf(final CGroup group, final PNode sibling)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					group.moveGroupInFrontOf(sibling);
				}});
	}
	
	public static void moveNodeInBackOf(final PNode node, final PNode sibling)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.moveInBackOf(sibling);
				}});
	}
	
	public static void moveNodeToFront(final PNode node)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.moveToFront();
				}});
	}
	
	public static void moveNodeToBack(final PNode node)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.moveToBack();
				}});
	}
	
	public static void repaint(final PNode node)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.repaint();
				}});
	}
	
	public static void repaintNode(final PNode node)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.repaintFrom(node.getBounds(), node);
				}});
	}
	
	public static void repaintNode(final PNode node, final PBounds bounds, final PNode childOrThis)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.repaintFrom(bounds, childOrThis);
				}});
	}
	
	public static void setNodePaintInvalid(final PNode node, final boolean paintInvalid)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.setPaintInvalid(paintInvalid);
				}});
	}
	
	public static void invalidatePaint(final PNode node)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.invalidatePaint();
				}});
	}
	
	public static void setVisible(final PNode node, final boolean visible)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					node.setVisible(visible);
				}});
	}
	
	public static void processEventFromCamera(final PRoot root, final InputEvent e, final int type, final PCamera camera)
	{
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					root.getDefaultInputManager().processEventFromCamera(e, type, camera);
				}});
	}
	
	
}
