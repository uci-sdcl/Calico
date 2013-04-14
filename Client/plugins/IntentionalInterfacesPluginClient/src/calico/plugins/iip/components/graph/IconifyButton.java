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
package calico.plugins.iip.components.graph;

import calico.components.menus.CanvasMenuButton;
import calico.plugins.iip.controllers.CIntentionCellController;
import calico.plugins.iip.iconsets.CalicoIconManager;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * Simple toggle button which alternately enables or disables the icon form of the canvas thumbnails in the Intention
 * View. When the icon form is enabled and the thumbnails are small, they will appear as icons instead of attempting to
 * draw a miniscule thumbnail. This feature is obsolete.
 * 
 * @author Byron Hawkins
 */
public class IconifyButton extends CanvasMenuButton
{
	private static final long serialVersionUID = 1L;

	private long currentCanvasId;

	private boolean active = true;

	public IconifyButton()
	{
		this(0L);
	}

	/**
	 * Invoked via reflection in CanvasStatusBar
	 */
	public IconifyButton(long canvas_uuid)
	{
		try
		{
			this.currentCanvasId = canvas_uuid;

			updateImage();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void actionMouseClicked()
	{
		active = !active;
		updateImage();

		CIntentionCellController.getInstance().activateIconifyMode(active);
	}

	private void updateImage()
	{
		PBounds bounds = getBounds();

		if (active)
		{
			setImage(CalicoIconManager.getIconImage("intention.iconify-on-button"));
		}
		else
		{
			setImage(CalicoIconManager.getIconImage("intention.iconify-off-button"));
		}

		setBounds(bounds);
	}
}
