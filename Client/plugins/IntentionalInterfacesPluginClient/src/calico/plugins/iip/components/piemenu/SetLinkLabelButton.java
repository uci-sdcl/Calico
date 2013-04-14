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
package calico.plugins.iip.components.piemenu;

import calico.components.piemenu.PieMenuButton;
import calico.plugins.iip.components.CCanvasLink;
import calico.plugins.iip.components.LinkLabelDialog;
import calico.plugins.iip.controllers.CCanvasLinkController;
import calico.plugins.iip.iconsets.CalicoIconManager;

/**
 * Simple button for the user to request changing the label on a link. This feature is obsolete.
 * 
 * @author Byron Hawkins
 */
public class SetLinkLabelButton extends PieMenuButton
{
	private CCanvasLink link = null;

	public SetLinkLabelButton()
	{
		super(calico.plugins.iip.iconsets.CalicoIconManager.getIconImage("intention.set-link-label"));
	}

	public void setContext(CCanvasLink link)
	{
		this.link = link;
	}

	@Override
	public void onClick()
	{
		if (link == null)
		{
			System.out.println("Warning: set link label button displayed without having been prepared with a link!");
			return;
		}

		LinkLabelDialog.Action action = LinkLabelDialog.getInstance().queryUserForLabel(link);
		switch (action)
		{
			case OK:
				System.out.println("Set label for link to " + LinkLabelDialog.getInstance().getText());
				CCanvasLinkController.getInstance().setLinkLabel(link.getId(), LinkLabelDialog.getInstance().getText());
				break;
			case CANCEL:
				System.out.println("Cancel setting the link label.");
				break;
		}
	}
}
