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
package calico.components.piemenu.canvas;

import java.awt.Dimension;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import calico.Calico;
import calico.CalicoDataStore;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.inputhandlers.InputEventInfo;
import calico.networking.Networking;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;

@Deprecated
public class TextCreate extends PieMenuButton {

	public static int SHOWON = PieMenuButton.SHOWON; //PieMenuButton.SHOWON_SCRAP_MENU;
	long uuid = 0L;
	
	public TextCreate()
	{
		super("canvas.text");		
	}
	
	public TextCreate(long u)
	{
		super("canvas.text");
		this.uuid = u;
	}
	
	public void onClick(InputEventInfo ev)
	{
		super.onClick(ev);
//		String response = JOptionPane.showInputDialog(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getComponent(),
//				  "Create Scrap with Text",
//				  "Please enter text",
//				  JOptionPane.QUESTION_MESSAGE);
		
		final JTextArea textArea = new JTextArea("Create Scrap with Text",20,10);
		textArea.setEditable(true);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		JScrollPane areaScrollPane = new JScrollPane(textArea);
		areaScrollPane.setVerticalScrollBarPolicy(
		                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		areaScrollPane.setPreferredSize(new Dimension(250, 250));
		Object[] inputText = new Object[]{areaScrollPane};
		
		String[] options = {"OK", "Cancel"};
		int responseInt = JOptionPane.showOptionDialog(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getComponent(),
					inputText,
				  "Please enter text",
				  JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
		
		String response = null;
		if (responseInt == JOptionPane.OK_OPTION)
			response = textArea.getText();
		
		long new_uuid = 0l;
		if (response != null)
		{
			if (isImageURL(response))
			{
				new_uuid = Calico.uuid();
				Networking.send(CalicoPacket.getPacket(NetworkCommand.GROUP_IMAGE_DOWNLOAD, new_uuid, CCanvasController.getCurrentUUID(), response, CalicoDataStore.ScreenWidth / 2, CalicoDataStore.ScreenHeight / 2));
			}
			else
			{
				new_uuid = Calico.uuid();
				CGroupController.create_text_scrap(new_uuid, CCanvasController.getCurrentUUID(), response, CalicoDataStore.ScreenWidth / 2, CalicoDataStore.ScreenHeight / 2);
			}
		}
//		if (this.uuid != 0l && new_uuid != 0l && CGroupController.groupdb.get(new_uuid).getParentUUID() == 0l)
//		{
//			CGroupController.move_start(new_uuid);
//			CGroupController.move_end(new_uuid, ev.getX(), ev.getY());
//		}
	}
	
	private boolean isImageURL(String text)
	{
		String regex = "((https?|ftp|gopher|telnet|file|notes|ms-help):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)+\\.(?:gif|jpg|jpeg|png|bmp|GIF|JPEG|JPG|PNG|BMP|Gif|Jpg|Jpeg|Png|Bmp)$";
		Pattern pattern = Pattern.compile(regex); 
		Matcher matcher = pattern.matcher(text); 
		return matcher.matches();
	}
}
