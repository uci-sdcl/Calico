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
package calico.input;

import java.awt.image.BufferedImage;

import calico.iconsets.CalicoIconManager;

public enum CInputMode
{
	// public static final int MODE_EXPERT = 1 << 0;
	// public static final int MODE_SCRAP = 1 << 1;
	// public static final int MODE_STROKE = 1 << 2;
	// public static final int MODE_ARROW = 1 << 3;
	// public static final int MODE_DELETE = 1 << 4;
	// public static final int MODE_POINTER = 1 << 5;

	EXPERT("mode.expert"),
	SCRAP("mode.scrap"),
	STROKE("mode.stroke"),
	ARROW("mode.arrow"),
	DELETE("mode.delete"),
	POINTER("mode.pointer");

	private final int id;
	private final String imageId;
	private BufferedImage image = null;

	private CInputMode(String imageId)
	{
		this.id = 1 << ordinal();
		this.imageId = imageId;
	}

	public BufferedImage getImage()
	{
		return image;
	}

	public int getId()
	{
		return id;
	}

	public String getImageId()
	{
		return imageId;
	}

	public static CInputMode forId(int id)
	{
		for (CInputMode mode : CInputMode.values())
		{
			if (mode.id == id)
			{
				return mode;
			}
		}
		return null;
	}

	public static void setup()
	{
		for (CInputMode mode : CInputMode.values())
		{
			mode.image = CalicoIconManager.getIconImage(mode.imageId);
		}
	}
}
