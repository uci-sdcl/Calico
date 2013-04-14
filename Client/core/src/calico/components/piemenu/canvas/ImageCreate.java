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

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;

import calico.Calico;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.piemenu.PieMenuButton;
import calico.controllers.CCanvasController;
import calico.controllers.CImageController;
import calico.inputhandlers.InputEventInfo;
import calico.networking.Networking;

@Deprecated
public class ImageCreate extends PieMenuButton {

	public static int SHOWON = PieMenuButton.SHOWON; //PieMenuButton.SHOWON_SCRAP_MENU;
	long uuid = 0L;
	
	public ImageCreate()
	{
		super("canvas.image");		
	}
	
	public ImageCreate(long u)
	{
		super("canvas.image");
		this.uuid = u;
	}
	
	public void onClick(InputEventInfo ev)
	{
		super.onClick(ev);
		
		final JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new ImageFileFilter());
        int returnVal = fc.showOpenDialog(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getComponent());

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            Networking.send(CImageController.getImageTransferPacket(Calico.uuid(), CCanvasController.getCurrentUUID(), 
            		BubbleMenu.lastOpenedPosition.x, BubbleMenu.lastOpenedPosition.y, file));
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
	
	class ImageFileFilter extends javax.swing.filechooser.FileFilter 
	{
	    public boolean accept(File file) 
	    {
	    	if (file.isFile())
	    	{
    	        String filename = file.getName().toLowerCase();
    	        return filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".gif");
	    	}
	    	else
	    	{
	    		return true;
	    	}
	    }
	    
	    public String getDescription() 
	    {
	        return "*.png, *.jpg, *.gif";
	    }
	}
}
