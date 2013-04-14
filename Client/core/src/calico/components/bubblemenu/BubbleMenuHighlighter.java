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
package calico.components.bubblemenu;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import calico.CalicoDraw;
import calico.CalicoOptions;

import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.nodes.PComposite;

public class BubbleMenuHighlighter extends PComposite {
	public static int halo_buffer = 12;
	public static int halo_size = CalicoOptions.menu.icon_size + halo_buffer;
	
	public BubbleMenuHighlighter()
	{
		//setBounds(0,0,halo_size,halo_size);
		//Initialize it offscreen in case it gets drawn;
		CalicoDraw.setNodeBounds(this, -100,-100,halo_size,halo_size);
	}
	
	
	protected void paint(PPaintContext paintContext)
	{
		Graphics2D graphics = (Graphics2D)paintContext.getGraphics();
		graphics.setStroke(new BasicStroke(1.0f));
		
		if (BubbleMenu.selectedButtonIndex != -1 && BubbleMenu.getButton(BubbleMenu.selectedButtonIndex).haloEnabled
				&& BubbleMenu.buttonPosition[BubbleMenu.selectedButtonIndex] != 0)
		{
			//Rectangle2D buttonBounds = BubbleMenu.buttonList.get(BubbleMenu.selectedButtonIndex).getBounds();
			
			Ellipse2D.Double halo = new Ellipse2D.Double(getBounds().getX(), getBounds().getY(), getBounds().getWidth(), getBounds().getHeight());
			
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
			
			graphics.setColor(new Color(255,196,121));
			graphics.fill(halo);
			//graphics.setPaint(new Color(243,179,97));
			graphics.setPaint(Color.gray);
			graphics.draw(halo);
		}
			
	}
}
