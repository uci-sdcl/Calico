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
package calico.components.arrow;

import java.awt.Point;


/**
 * Used to denote the anchor points in {@link CArrow}
 * 
 * @author mdempsey
 * 
 */
public class AnchorPoint extends AbstractArrowAnchorPoint implements Cloneable
{
	private int type = CArrow.TYPE_CANVAS;
	private long uuid = 0L;

	public AnchorPoint(int type, Point point, long uuid)
	{
		super(point);

		this.type = type;
		this.uuid = uuid;
	}

	public AnchorPoint(Point point, long uuid)
	{
		this(CArrow.TYPE_CANVAS, point, uuid);
	}

	public AnchorPoint(int type, long uuid, Point point)
	{
		this(type, point, uuid);
	}

	public int getType()
	{
		return this.type;
	}

	public long getUUID()
	{
		return this.uuid;
	}

	public void setType(int type)
	{
		this.type = type;
	}

	public void setUUID(long uuid)
	{
		this.uuid = uuid;
	}

	// For cloning dolly
	public AnchorPoint clone()
	{
		return new AnchorPoint(type, new Point(getPoint().x, getPoint().y), uuid);
	}
}
