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

import java.awt.*;
import java.awt.List;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

import calico.*;
import calico.CalicoOptions.menu.menubar;
import calico.components.*;
import calico.components.grid.*;
import calico.controllers.CCanvasController;
import calico.iconsets.CalicoIconManager;
import calico.inputhandlers.*;
import calico.modules.*;
import calico.networking.*;

import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolo.nodes.*;
import edu.umd.cs.piccolox.nodes.*;
import edu.umd.cs.piccolox.pswing.*;

import java.net.*;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import edu.umd.cs.piccolo.event.*;



public class CanvasGenericMenuBar extends PComposite
{
	public static Logger logger = Logger.getLogger(CanvasGenericMenuBar.class.getName());
	
	
	public static final int POSITION_TOP = 1;
	public static final int POSITION_BOTTOM = 2;
	public static final int POSITION_LEFT = 3;
	public static final int POSITION_RIGHT = 4;
	
	public static final int ALIGN_START = 10;
	public static final int ALIGN_END = 11;
	public static final int ALIGN_CENTER = 12;
	
	private static final long serialVersionUID = 1L;
	
	private Rectangle rect_overall = new Rectangle();
	protected Rectangle[] rect_array = new Rectangle[50];
	protected CanvasMenuButton[] button_array = new CanvasMenuButton[50];
	protected int button_array_index = 0;
	
	protected Rectangle[] text_rect_array = new Rectangle[50];
	protected CanvasTextButton[] text_button_array = new CanvasTextButton[50];
	protected int text_button_array_index = 0;
	
	//private long cuid = 0L;
	
	private int empty_space_start = 3;
	private int empty_space_end = 0;
	
	private int icon_padding = 4;
	
	protected int position = CanvasGenericMenuBar.POSITION_BOTTOM;
	
	
	private Rectangle screenBounds = null;

	
	
	public CanvasGenericMenuBar(int position, Rectangle screenBounds)
	{
		this.position = position;
		
		switch (this.position)
		{
			case POSITION_TOP:
			case POSITION_BOTTOM:
				empty_space_end += screenBounds.width;
				break;
			case POSITION_LEFT:
			case POSITION_RIGHT:
				empty_space_end += screenBounds.height;
				break;
		}
		
		this.screenBounds = new Rectangle(screenBounds);
		icon_padding = CalicoOptions.menu.menubar.padding;
		
		int menubarWidth = menubar.defaultIconDimension+menubar.iconBuffer*2;
		switch (this.position)
		{
			case CanvasGenericMenuBar.POSITION_TOP:
				this.rect_overall = new Rectangle(0, 0, this.screenBounds.width, menubarWidth);
				empty_space_end -= 3;
				break;
			case CanvasGenericMenuBar.POSITION_BOTTOM:
				this.rect_overall = new Rectangle(0, this.screenBounds.height-menubarWidth, this.screenBounds.width, menubarWidth);
				empty_space_end -= 3;
				break;
			case CanvasGenericMenuBar.POSITION_LEFT:
				this.rect_overall = new Rectangle(0, 0, menubarWidth, this.screenBounds.height);
				empty_space_end -= menubarWidth;
				break;
			case CanvasGenericMenuBar.POSITION_RIGHT:
				this.rect_overall = new Rectangle(this.screenBounds.width-menubarWidth, 0, menubarWidth, this.screenBounds.height);
				empty_space_end -= menubarWidth;
				break;
		}
		
		setBounds(this.rect_overall);
		//CalicoDraw.setNodeBounds(this, this.rect_overall);
		setPaint( CalicoOptions.menu.menubar.background_color );
	}
	public CanvasGenericMenuBar(int position)
	{		
		this(position, new Rectangle(0,0,CalicoDataStore.ScreenWidth, CalicoDataStore.ScreenHeight));		
	}
	public CanvasGenericMenuBar()
	{
		this(CanvasGenericMenuBar.POSITION_BOTTOM);
	}
	
	public void addSpacer()
	{
		addSpacer(ALIGN_START);
	}
	
	public void addSpacer(int align)
	{
		//xcoord_position = xcoord_position + 24;
		addCap(align);
		//xcoord_position = xcoord_position + 5;
		addCap(align);
	}
	
	public Rectangle addIcon(int span)
	{
		int startPosition = empty_space_start;
		
		Rectangle temp = addIcon(span, startPosition);
		
		int tempSpan = 0;
		switch (this.position)
		{
			case POSITION_TOP:
			case POSITION_BOTTOM:
				tempSpan = temp.width;
				break;
			case POSITION_LEFT:
			case POSITION_RIGHT:
				tempSpan = temp.height;
				break;
		}
		empty_space_start = empty_space_start + tempSpan;
		return temp;
	}
	
	public Rectangle addIconCenterAligned(int span)
	{
		int centerPosition = (int) (this.getWidth() / 2) - (span / 2);
		
		Rectangle temp = addIcon(span, centerPosition);
		
		return temp;
	}
	
	private Rectangle addIconEndAligned(int span)
	{
		int endPosition = empty_space_end - span;
		
		Rectangle temp = addIcon(span, endPosition);
		
		empty_space_end -= span;
		return temp;
	}
	
	private Rectangle addIcon(int span, int startPosition) {
		Rectangle temp = null;
		switch (this.position)
		{
			case CanvasGenericMenuBar.POSITION_BOTTOM:
				temp = new Rectangle(startPosition, this.screenBounds.height-(menubar.defaultIconDimension + menubar.iconBuffer), span, menubar.defaultIconDimension);
				break;
			case CanvasGenericMenuBar.POSITION_TOP:
    			temp = new Rectangle(startPosition, menubar.iconBuffer, span, menubar.defaultIconDimension);
    			break;
			case CanvasGenericMenuBar.POSITION_LEFT:
				temp = new Rectangle(menubar.iconBuffer, startPosition, menubar.defaultIconDimension, span);
				break;
			case CanvasGenericMenuBar.POSITION_RIGHT:
				temp = new Rectangle(this.screenBounds.width-(menubar.defaultIconDimension + menubar.iconBuffer), startPosition, menubar.defaultIconDimension, span);
				break;
		}
		return temp;
	}
	
	public void addIcon(CanvasMenuButton icon)
	{
		addIcon(icon, menubar.defaultIconDimension);
	}
	
	public void addIcon(CanvasMenuButton icon, int span)
	{
		rect_array[button_array_index] = addIcon(span);
		button_array[button_array_index] = icon;
		button_array[button_array_index].setBounds(rect_array[button_array_index]);
		
		
		//addChild(0,button_array[button_array_index]);
		CalicoDraw.addChildToNode(this, button_array[button_array_index], 0);
		button_array_index++;
	}
	
	protected void centerIconsVertically()
	{
		//get bounds, figure out delta for mid point, shift all figures
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					ListIterator<PNode> children = CanvasGenericMenuBar.this.getChildrenIterator();
					int maxHeight = Integer.MIN_VALUE;
					int minHeight = Integer.MAX_VALUE;
					while (children.hasNext())
					{
						PNode child = children.next();
						if (child.getBoundsReference().y < minHeight)
							minHeight = (int)child.getBoundsReference().y;
						if (child.getBoundsReference().y + child.getBounds().height > maxHeight)
							maxHeight = (int)(child.getBoundsReference().y + child.getBounds().height);
					}
					int mid = (minHeight + maxHeight) / 2;
					int deltaY = (int)CanvasGenericMenuBar.this.getBounds().getCenterY() - mid;
					
					for (int i = 0; i < rect_array.length; i++)
					{
						if (rect_array[i] != null)
						{
							rect_array[i].translate(0, deltaY);
							button_array[i].setBounds(rect_array[i]);
						}
					}
				}});

		
		
		
//		rect_array[button_array_index] = addIcon(span);
//		button_array[button_array_index] = icon;
//		button_array[button_array_index].setBounds(rect_array[button_array_index]);
		
		
		//addChild(0,button_array[button_array_index]);
//		CalicoDraw.addChildToNode(this, button_array[button_array_index], 0);
	}
	
	public void addIconRightAligned(CanvasMenuButton icon)
	{
		addIconRightAligned(icon, menubar.defaultIconDimension);
	}
	
	public void addIconRightAligned(CanvasMenuButton icon, int span)
	{
		rect_array[button_array_index] = addIconEndAligned(span);
		button_array[button_array_index] = icon;
		button_array[button_array_index].setBounds(rect_array[button_array_index]);
		
		
		//addChild(0,button_array[button_array_index]);
		CalicoDraw.addChildToNode(this, button_array[button_array_index], 0);
		button_array_index++;
	}
	
	
	public boolean isPointInside(Point point)
	{
		return rect_overall.contains(point);
	}
	
	
	public PImage addText(String text, Font font, CanvasTextButton buttonHandler)
	{
		Image img = getTextImage(text,font);
		
		int imageSpan = 0;
		switch (this.position)
		{
			case POSITION_TOP:
			case POSITION_BOTTOM:
				imageSpan = img.getWidth(null);
				break;
			case POSITION_LEFT:
			case POSITION_RIGHT:
				imageSpan = img.getHeight(null);
				break;
		}
		Rectangle temp = addIcon(imageSpan);
		
		PImage img2 = new PImage();
		
		img2.setImage(img);
		
		img2.setBounds(temp);

		text_rect_array[text_button_array_index] = temp;

		text_button_array[text_button_array_index] = buttonHandler;
		text_button_array_index++;
		
		//addChild(0,img2);
		CalicoDraw.addChildToNode(this, img2, 0);
		
		return img2;
	}
	
	public PImage addTextCenterAligned(String text, Font font, CanvasTextButton buttonHandler)
	{
		Image img = getTextImage(text,font);
		
		int imageSpan = 0;
		switch (this.position)
		{
			case POSITION_TOP:
			case POSITION_BOTTOM:
				imageSpan = img.getWidth(null);
				break;
			case POSITION_LEFT:
			case POSITION_RIGHT:
				imageSpan = img.getHeight(null);
				break;
		}
		Rectangle temp = addIconCenterAligned(imageSpan);
		
		PImage img2 = new PImage();
		
		img2.setImage(img);
		
		img2.setBounds(temp);

		text_rect_array[text_button_array_index] = temp;

		text_button_array[text_button_array_index] = buttonHandler;
		text_button_array_index++;
		
		//addChild(0,img2);
		CalicoDraw.addChildToNode(this, img2, 0);
		
		return img2;
	}
	
	public PImage addTextEndAligned(String text, Font font, CanvasTextButton buttonHandler)
	{
		Image img = getTextImage(text,font);
		
		int imageSpan = 0;
		switch (this.position)
		{
			case POSITION_TOP:
			case POSITION_BOTTOM:
				imageSpan = img.getWidth(null);
				break;
			case POSITION_LEFT:
			case POSITION_RIGHT:
				imageSpan = img.getHeight(null);
				break;
		}
		Rectangle temp = addIconEndAligned(imageSpan);
		
		PImage img2 = new PImage();
		
		img2.setImage(img);
		
		img2.setBounds(temp);

		text_rect_array[text_button_array_index] = temp;

		text_button_array[text_button_array_index] = buttonHandler;
		text_button_array_index++;
		
		//addChild(0,img2);
		CalicoDraw.addChildToNode(this, img2, 0);
		
		return img2;
	}
	
	public void addText(String text, Font font) {
		addText(text,font,null);
	}
	public void addText(String text)
	{
		addText(text, new Font("Monospaced", Font.BOLD, 14), null);
	}
	public void addText(String text, CanvasTextButton buttonHandler)
	{
		addText(text, new Font("Monospaced", Font.BOLD, 14), buttonHandler);
	}
	
	public void addCap(int align)
	{
		try
		{
			Rectangle temp = new Rectangle();
			
			switch (align)
			{
				case ALIGN_START: temp = addIcon(2);
								break;
				case ALIGN_CENTER: temp = addIconCenterAligned(2);
								break;
				case ALIGN_END: temp = addIconEndAligned(2);
								break;
			}
			
			PImage img = new PImage();
			//this is using a sprite and scaling it
			img.setImage(CalicoIconManager.getImagePart(CalicoIconManager.getIconImage("menu.button_bg"),
				menubar.defaultIconDimension+1,
				0,
				4,
				menubar.defaultIconDimension
			) );
			
			img.setBounds(temp);
			//addChild(0,img);
			CalicoDraw.addChildToNode(this, img, 0);
			//button_array_index++;
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//getImagePart
	}
	
	/**
	 * They clicked the menu, this should process which button they pushed
	 * @param point
	 */
	public void clickMenu(InputEventInfo event, Point point)
	{
		for(int i=0;i<button_array_index;i++)
		{
			if(rect_array[i].contains(point))
			{
				button_array[i].actionMouseClicked(event);
				//Backwards compatibility with plugins
				if (event.getAction() == InputEventInfo.ACTION_RELEASED)
					button_array[i].actionMouseClicked();
				return;
			}
		}
		if(text_button_array_index>0) {
			for(int i=0;i<text_button_array_index;i++)
			{
				if(text_rect_array[i].contains(point))
				{
					text_button_array[i].actionMouseClicked(event, text_rect_array[i]);
					//Backwards compatibility with plugins
					if (event.getAction() == InputEventInfo.ACTION_RELEASED)
						text_button_array[i].actionMouseClicked(text_rect_array[i]);
					return;
				}
			}
		}
	}
	
	protected Image getTextImage(String text, Font font)
	{
		Image background = CalicoIconManager.getIconImage("menu.button_bg");
		//button_bg_black.png = 88x66 px
		BufferedImage bgBuf = new BufferedImage(88,66, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bgBuf.getGraphics();
		g.setBackground(new Color(83,83,83));
		g.drawImage(background, null, null);
		
		//the 110 might be arbitrary
		BufferedImage textImage = new BufferedImage(110, menubar.defaultSpriteSize, BufferedImage.TYPE_INT_ARGB );
		
		Graphics2D g2 = (Graphics2D) textImage.getGraphics();
		g2.setBackground(new Color(83,83,83));
		//g.drawImage(this.background, null, null);

		g2.setFont(font);
		
		FontMetrics fontmetrics = g2.getFontMetrics(font);
		Rectangle2D strbounds = fontmetrics.getStringBounds(text, g2);
		
		int offset = (int) Math.ceil(Math.abs(strbounds.getY()));
		
	//	logger.debug("FONT BOUNDS: "+strbounds.toString()+" | "+fontmetrics.stringWidth(text));
		
		
		for(int i=0;i<6;i++)
		{
			g2.drawImage(bgBuf.getSubimage(0,0, menubar.defaultSpriteSize,menubar.defaultSpriteSize), i*menubar.defaultSpriteSize,0,null);//, null, null);
			//g2.translate(i*22, 0);
		}
		//g2.translate(0, 0);
		g2.setPaint(Color.BLACK);
		//g2.drawString(text, 0, offset);
		FontRenderContext frc = g2.getFontRenderContext();
		   TextLayout layout = new TextLayout(text, font, frc);
		   layout.draw(g2, (float)0, (float)offset);
		
		//g2.drawImage(img,  3, 3, null);
		
		textImage = (BufferedImage)textImage.getSubimage(0, 0, (int)strbounds.getWidth(), (int)strbounds.getHeight());
		
		switch (this.position)
		{
			case POSITION_TOP:
			case POSITION_BOTTOM:
				return textImage;
			case POSITION_LEFT:
			case POSITION_RIGHT:
				BufferedImage rotatedImage =  new BufferedImage((int)strbounds.getHeight(), (int)strbounds.getWidth(), BufferedImage.TYPE_INT_ARGB);
				g2 = (Graphics2D) rotatedImage.getGraphics();
				g2.rotate(Math.PI/2);
				g2.translate(0, -(int)strbounds.getHeight()); 
				g2.drawImage(textImage, 0, 0, null);
				return rotatedImage;
			default: 
				throw new IllegalStateException("Unknown menu bar position " + position);
		}
	}
}
