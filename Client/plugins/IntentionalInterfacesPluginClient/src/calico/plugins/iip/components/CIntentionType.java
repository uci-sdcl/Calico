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
package calico.plugins.iip.components;

import java.awt.Color;

/**
 * Represents a canvas tag in this plugin's internal model. Rendering associated with this tag is maintained by other
 * classes such as the <code>CanvasTagPanel</code>.
 * 
 * @author Byron Hawkins
 */
public class CIntentionType
{
	/**
	 * Static set of tag colors, identified by array index, which is known both to the client and server.
	 */
	public static final Color[] AVAILABLE_COLORS = new Color[] { new Color(0xFFFFFF), new Color(0xC4FF5E), new Color(0xFFF024), new Color(0x29FFE2), new Color(0x52DCFF),
			new Color(0xFFBDC1), new Color(0xFFE1C9), new Color(0xC2E4FF), new Color(0xEED9FF) };

	/**
	 * Identifies the tag.
	 */
	private final long uuid;
	/**
	 * Display name of the tag.
	 */
	private String name;
	/**
	 * Index of the tag's color. This index is the color reference value on both the client and server, and is only
	 * correlated with a visual color in this class.
	 */
	private int colorIndex;
	/**
	 * Description of tag.
	 */
	private String description;

	public CIntentionType(long uuid, String name, int colorIndex, String description)
	{
		this.uuid = uuid;
		this.name = name;
		this.colorIndex = colorIndex;
		this.description = description;
	}

	public long getId()
	{
		return uuid;
	}

	public String getName()
	{
		return name;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	public void setDescription(String description)
	{
		this.description = description;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * A tag'S color is known only by the color index in all other classes on the client and server.
	 */
	public int getColorIndex()
	{
		return colorIndex;
	}

	public Color getColor()
	{
		return AVAILABLE_COLORS[colorIndex];
	}

	/**
	 * A color is assigned by setting the index using this method.
	 */
	public void setColorIndex(int colorIndex)
	{
		this.colorIndex = colorIndex;
	}
}
