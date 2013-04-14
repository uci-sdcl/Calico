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
package calico.components.menus;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.Font;
import java.awt.Rectangle;

import javax.swing.JOptionPane;

import calico.Calico;
import calico.CalicoDataStore;
import calico.components.menus.buttons.EmailGridButton;
import calico.components.menus.buttons.ExitButton;
import calico.inputhandlers.InputEventInfo;

public class GridBottomMenuBar extends CanvasGenericMenuBar
{
	private static final long serialVersionUID = 1L;

	private long cuid = 0L;

	private static ObjectArrayList<Class<?>> externalButtons = new ObjectArrayList<Class<?>>();
	private static ObjectArrayList<Class<?>> externalButtons_rightAligned = new ObjectArrayList<Class<?>>();

	public GridBottomMenuBar(long c)
	{
		super(CanvasGenericMenuBar.POSITION_BOTTOM);
		Calico.logger.debug("loaded generic menu bar for GridBottomMenuBar");
		cuid = c;

		addCap(CanvasGenericMenuBar.ALIGN_START);

		// addIcon(new GridViewportChangeButton(GridViewportChangeButton.BUT_MINUS));
		// addIcon(new GridViewportChangeButton(GridViewportChangeButton.BUT_PLUS));

		// addSpacer();
		//
		// addIcon(new GridSessionMenuButton(1));
		// addIcon(new GridSessionMenuButton(0));

		// addSpacer();
		//
		// addText(
		// "view clients",
		// new Font("Verdana", Font.BOLD, 12),
		// new CanvasTextButton(cuid) {
		// public void actionMouseClicked(Rectangle boundingBox) {
		// CalicoDataStore.gridObject.drawClientList(boundingBox);
		// }
		// }
		// );

		// addSpacer();

		// addText(
		// "sessions",
		// new Font("Verdana", Font.BOLD, 12),
		// new CanvasTextButton(cuid) {
		// public void actionMouseClicked(Rectangle boundingBox) {
		// Calico.showSessionPopup();
		// }
		// }
		// );

		// addSpacer();

		addTextEndAligned("  Exit  ", new Font("Verdana", Font.BOLD, 12), new ExitButton(cuid));		
		addSpacer(ALIGN_END);
		addIconRightAligned(new EmailGridButton());

		try
		{
			for (Class<?> button : externalButtons)
			{
				addSpacer();
				addIcon((CanvasMenuButton) button.getConstructor(long.class).newInstance(cuid));
			}

			for (Class<?> button : externalButtons_rightAligned)
			{
				addSpacer(ALIGN_END);
				addIconRightAligned((CanvasMenuButton) button.getConstructor(long.class).newInstance(cuid));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		// addCap();

	}

	public static void addMenuButton(Class<?> button)
	{
		externalButtons.add(button);
	}

	public static void addMenuButtonRightAligned(Class<?> button)
	{
		externalButtons_rightAligned.add(button);
	}

}
