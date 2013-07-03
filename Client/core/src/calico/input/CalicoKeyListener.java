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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import calico.Calico;
import calico.CalicoDataStore;
import calico.CalicoOptions;
import calico.components.bubblemenu.BubbleMenu;
import calico.components.decorators.CListDecorator;
import calico.components.grid.CGrid;
import calico.components.menus.buttons.CanvasNavButton;
import calico.controllers.CCanvasController;
import calico.controllers.CGroupController;
import calico.controllers.CImageController;
import calico.networking.Networking;
import calico.networking.netstuff.CalicoPacket;
import calico.networking.netstuff.NetworkCommand;
import calico.perspectives.CanvasPerspective;
import calico.perspectives.GridPerspective;

public class CalicoKeyListener extends KeyAdapter {

    public void keyPressed(KeyEvent evt) {
    	
    	int buttonType = -2;
        // Check for key characters.
        if (evt.getKeyCode() == KeyEvent.VK_LEFT || evt.getKeyCode() == KeyEvent.VK_PAGE_UP) {
            buttonType = CanvasNavButton.TYPE_LEFT;
        }

        if (evt.getKeyCode() == KeyEvent.VK_RIGHT || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
        	buttonType = CanvasNavButton.TYPE_RIGHT;
        }
        
        if (evt.getKeyCode() == KeyEvent.VK_UP) {
        	buttonType = CanvasNavButton.TYPE_UP;
        }
        
        if (evt.getKeyCode() == KeyEvent.VK_DOWN) {
        	buttonType = CanvasNavButton.TYPE_DOWN;
        }
        
        if (buttonType > -1)
        	moveToCell(buttonType);
        
        if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
 
//        	if (!GridPerspective.getInstance().isActive())
//        		CGrid.loadGrid();
        }
        
        if (CanvasPerspective.getInstance().isActive() && evt.getKeyCode() == KeyEvent.VK_ENTER) {
        	createTextScrap();
        }
        
        if (CanvasPerspective.getInstance().isActive() && evt.isControlDown()
        		&& evt.getKeyCode() == KeyEvent.VK_V) {
        	System.out.println("attempting paste!");
        	attemptImagePaste();
        }
    }
    
    private void moveToCell(int button_type)
    {
    	/*
		// Grid Size
		int gridx = CalicoDataStore.GridCols-1;
		int gridy = CalicoDataStore.GridRows-1;
				
		// Canvas Coords
		long cuuid = CCanvasController.getLastActiveUUID();
		int xpos = CCanvasController.canvasdb.get(cuuid).getGridCol();
		int ypos = CCanvasController.canvasdb.get(cuuid).getGridRow();
    	
		switch(button_type)
		{
			case CanvasNavButton.TYPE_DOWN:
				if((ypos+1)<=gridy)
				{
					
					
					loadCanvas(xpos,ypos+1);
					
				}
				else
				{
					loadCanvas(xpos,0);
				}
				break;
				
			case CanvasNavButton.TYPE_UP:
				if((ypos-1)>=0)
				{
					loadCanvas(xpos,ypos-1);
				}
				else
				{
					loadCanvas(xpos,gridy);
				}
				break;
				
			case CanvasNavButton.TYPE_LEFT:
				if((xpos-1)>=0)
				{
					loadCanvas(xpos-1,ypos);
				}
				else
				{
					loadCanvas(gridx,ypos);
				}
				break;
				
			case CanvasNavButton.TYPE_RIGHT:
				if((xpos+1)<=gridx)
				{
					loadCanvas(xpos+1,ypos);
				}
				else
				{
					loadCanvas(0,ypos);
				}
				break;
		}
		*/
	
    }
    
	private void loadCanvas(int x, int y)
	{
		long cuid = CGrid.getCanvasAtPos(x, y);
		
		if(cuid==0L)
		{
			// Error
			return;
		}
		CCanvasController.unloadCanvasImages(CCanvasController.getCurrentUUID());
		CCanvasController.loadCanvas(cuid);
	}
	
	private void createTextScrap()
	{
		int xPos = CalicoDataStore.ScreenWidth/3, yPos = CalicoDataStore.ScreenHeight/3;
		boolean updateBubbleIcons = false;
		if (false
				&& BubbleMenu.activeUUID != 0l && CGroupController.groupdb.get(BubbleMenu.activeUUID) instanceof CListDecorator)
		{
			xPos = CGroupController.groupdb.get(BubbleMenu.activeUUID).getPathReference().getBounds().x + 50;
			yPos = CGroupController.groupdb.get(BubbleMenu.activeUUID).getPathReference().getBounds().y
					+ CGroupController.groupdb.get(BubbleMenu.activeUUID).getPathReference().getBounds().height - 10;
			updateBubbleIcons = true;
		}
		else
		{
			while (CGroupController.get_smallest_containing_group_for_point(CCanvasController.getCurrentUUID(), new Point(xPos, yPos)) != 0)
				yPos += CalicoOptions.group.padding * 4;
		}
		
		
//		String response = JOptionPane.showInputDialog(CCanvasController.canvasdb.get(CCanvasController.getCurrentUUID()).getComponent(),
//				  "Create Scrap with Text",
//				  "Please enter text",
//				  JOptionPane.QUESTION_MESSAGE);
		JTextArea textArea = new JTextArea("",20,10);
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
		if (response != null && response.length() > 0)
		{
			if (isImageURL(response))
			{
				new_uuid = Calico.uuid();
				Networking.send(CalicoPacket.getPacket(NetworkCommand.GROUP_IMAGE_DOWNLOAD, new_uuid, CCanvasController.getCurrentUUID(), response, 50, 50));
			}
			else
			{
				new_uuid = Calico.uuid();
				
				if (CGroupController.exists(BubbleMenu.activeUUID) 
						&& CGroupController.groupdb.get(BubbleMenu.activeUUID) instanceof CListDecorator)
				{
					CGroupController.create_text_scrap(new_uuid, CCanvasController.getCurrentUUID(), response, xPos, CalicoDataStore.ScreenHeight);
					CGroupController.set_parent(new_uuid, BubbleMenu.activeUUID);
				}
				else
				{
					CGroupController.create_text_scrap(new_uuid, CCanvasController.getCurrentUUID(), response, xPos, yPos);
				}
				if (updateBubbleIcons)
					BubbleMenu.moveIconPositions(CGroupController.groupdb.get(BubbleMenu.activeUUID).getBounds());
			}
		}
//		CGroupController.move_start(new_uuid);
//		CGroupController.move_end(new_uuid, xPos, yPos);
	}
	
	private boolean isImageURL(String text)
	{
		String regex = "((https?|ftp|gopher|telnet|file|notes|ms-help):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)+\\.(?:gif|jpg|jpeg|png|bmp|GIF|JPEG|JPG|PNG|BMP|Gif|Jpg|Jpeg|Png|Bmp)$";
		Pattern pattern = Pattern.compile(regex); 
		Matcher matcher = pattern.matcher(text); 
		return matcher.matches();
	}
	
	private void attemptImagePaste()
	{
		Image clipboardImage;
		String clipboardText;
		if ((clipboardImage = getImageFromClipboard()) != null)
		{
			try {
			    // retrieve image
			    BufferedImage bi = (BufferedImage) clipboardImage;
			    File outputfile = new File("saved.png");
			    ImageIO.write(bi, "png", outputfile);
		        Networking.send(CImageController.getImageTransferPacket(Calico.uuid(), CCanvasController.getCurrentUUID(), 
		        		50, 50, outputfile));
			} catch (IOException e) {
			    e.printStackTrace();
			}	
		}
		else if ((clipboardText = getTextFromClipboard()) != null
				&& clipboardText.length() > 0)
		{
			long new_uuid = Calico.uuid();
			CGroupController.create_text_scrap(new_uuid, CCanvasController.getCurrentUUID(), 
					clipboardText, CalicoDataStore.ScreenWidth / 3, CalicoDataStore.ScreenHeight / 3);
		}
		
	}
	
	/**
	 * Get an image off the system clipboard.
	 * @return Returns an Image if successful; otherwise returns null.
	 * 
	 * Taken from: http://alvinalexander.com/blog/post/jfc-swing/how-copy-paste-image-into-java-swing-application
	 */
	public Image getImageFromClipboard()
	{
	  Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
	  if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor))
	  {
	    try
	    {
	      return (Image) transferable.getTransferData(DataFlavor.imageFlavor);
	    }
	    catch (UnsupportedFlavorException e)
	    {
	      // handle this as desired
	      e.printStackTrace();
	    }
	    catch (IOException e)
	    {
	      // handle this as desired
	      e.printStackTrace();
	    }
	  }
	  return null;
	}
	
	public String getTextFromClipboard()
	{
	  Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
	  if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor))
	  {
	    try
	    {
	      return (String) transferable.getTransferData(DataFlavor.stringFlavor);
	    }
	    catch (UnsupportedFlavorException e)
	    {
	      // handle this as desired
	      e.printStackTrace();
	    }
	    catch (IOException e)
	    {
	      // handle this as desired
	      e.printStackTrace();
	    }
	  }
	  return null;
	}
	
}

