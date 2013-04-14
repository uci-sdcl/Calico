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
package calico.components.composable;

import calico.networking.netstuff.CalicoPacket;
import edu.umd.cs.piccolo.PNode;

public abstract class ComposableElement {
	
	public static final int TYPE_ARROWHEAD = 0;
	public static final int TYPE_CARDINALITY = 1;
	public static final int TYPE_COLOR = 2;
	public static final int TYPE_HIGHLIGHT = 3;
	public static final int TYPE_LABEL = 4;
	public static final int TYPE_LINESTYLE = 5;
	
	/**
	 * The element UUID
	 */
	protected long uuid;
	
	/**
	 * The component UUID
	 */
	protected long cuuid;
	
	
	public ComposableElement(long uuid, long cuuid)
	{
		this.uuid = uuid;
		this.cuuid = cuuid;
	}
	
	public long getElementUUID()
	{
		return this.uuid;
	}
	
	public long getComponentUUID()
	{
		return this.cuuid;
	}
	
	public Composable getComposable()
	{
		return null;
	}
	
	/**
	 * Override this function to add behavior and elements that are only applied once
	 * IE. setting a color variable
	 */
	public void applyElement()
	{
		
	}
	
	/**
	 * Override this function to to change behavior and elements only when the element is being removed
	 * IE. setting a color back to the original color
	 */
	public void removeElement()
	{
		
	}
	
	/**
	 * If a PNode needs to be returned so it can be added to a component on each redraw/repaint, this should return true.
	 * @return
	 */
	public boolean isDrawable()
	{
		return false;
	}
	
	/**
	 * If isDrawable is true, this should return the PNode to add. All calculations for locations, rotations, etc should be done 
	 * within this method.
	 * @return
	 */
	public PNode getNode()
	{
		return null;
	}
	
	public CalicoPacket getPacket(long uuid, long cuuid)
	{
		return null;
	}
	
	/**
	 * Return the packet that stores the element
	 * @return
	 */
	public CalicoPacket getPacket()
	{
		return null;
	}
	
}
