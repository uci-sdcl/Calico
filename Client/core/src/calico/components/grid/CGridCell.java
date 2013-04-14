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
package calico.components.grid;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import calico.CalicoDataStore;
import calico.CalicoDraw;
import calico.CalicoOptions;
import calico.components.CCanvas;
import calico.controllers.CCanvasController;
import calico.iconsets.CalicoIconManager;
import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.nodes.PLine;

public class CGridCell extends PImage//PComposite
{
	private static final long serialVersionUID = 1L;

	public static final int ROUNDED_RECTANGLE_OVERFLOW = 4;
	public static final int CELL_MARGIN = 6;

	private int cellId = 0;
	private int cellWidth = 0;
	private int cellHeight = 0;

	private long canvasUID = 0L;

	private Rectangle tBounds = new Rectangle();
	
	private int last_signature = 0;
	private PImage lock;
	private PText presenceText;
	private PNode presenceTextBackground;
	private PImage canvasLockedIcon;

	public CGridCell(long cuid, int pos, int xOffset, int yOffset, int w, int h)
	{
		canvasUID = cuid;
		cellId = pos;
		cellWidth = w;
		cellHeight = h;

		int rc = CGrid.getCanvasRow(canvasUID);
		int cc = CGrid.getCanvasColumn(canvasUID);
//		int rc = CCanvasController.canvasdb.get(canvasUID).getGridRow();
//		int cc = CCanvasController.canvasdb.get(canvasUID).getGridCol();


		//setBounds( (cc*cellWidth)+2, 50+( (rc*cellHeight)+2 ),  cellWidth-5,  cellHeight-5);
		Rectangle bounds = new Rectangle(xOffset + (cc*cellWidth), yOffset+( (rc*cellHeight) ),  cellWidth-CELL_MARGIN,  cellHeight-CELL_MARGIN);
		//setBounds( bounds );
		CalicoDraw.setNodeBounds(this, bounds);
		//setPaint( CalicoOptions.getColor("grid.item_background") );

		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					tBounds = getBounds().getBounds();
			
			
					/*PImage img = new PImage(CCanvasController.image(canvasUID));
					img.setBounds( tBounds.getX()+2,tBounds.getY()+2,tBounds.getWidth()-4,tBounds.getHeight()-4);
			
					addChild(0, img );
			
			
					addChild(drawBorderLine(tBounds.getX(),tBounds.getY(), tBounds.getX()+tBounds.getWidth(),tBounds.getY()));//top
					addChild(drawBorderLine(tBounds.getX()+tBounds.getWidth(),tBounds.getY(), tBounds.getX()+tBounds.getWidth(),tBounds.getY()+tBounds.getHeight()));//right
					addChild(drawBorderLine(tBounds.getX(),tBounds.getY(), tBounds.getX(),tBounds.getY()+tBounds.getHeight()));//left
					addChild(drawBorderLine(tBounds.getX(),tBounds.getY()+tBounds.getHeight(), tBounds.getX()+tBounds.getWidth(),tBounds.getY()+tBounds.getHeight()));//bottom
			
					*/
					render();
			
//					CCanvasController.canvasdb.get(canvasUID).setGridCoordRect( tBounds );
				}});

		


	}
	
	//adds to the other constructor the option to indicate the row and column for the Cell
	public CGridCell(long cuid, int pos, int xOffset, int yOffset, int w, int h,int rc, int cc)
	{
		canvasUID = cuid;
		cellId = pos;
		cellWidth = w;
		cellHeight = h;

		//setBounds( (cc*cellWidth)+2, 50+( (rc*cellHeight)+2 ),  cellWidth-5,  cellHeight-5);
		//setBounds(xOffset + (cc*cellWidth), yOffset + ( (rc*cellHeight) ),  cellWidth-2,  cellHeight-2);
		CalicoDraw.setNodeBounds(this, xOffset + (cc*cellWidth), yOffset + ( (rc*cellHeight) ),  cellWidth-2,  cellHeight-2);
		//setPaint( CalicoOptions.getColor("grid.item_background") );

		
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					tBounds = getBounds().getBounds();
			
			
					/*PImage img = new PImage(CCanvasController.image(canvasUID));
					img.setBounds( tBounds.getX()+2,tBounds.getY()+2,tBounds.getWidth()-4,tBounds.getHeight()-4);
			
					addChild(0, img );
			
			
					addChild(drawBorderLine(tBounds.getX(),tBounds.getY(), tBounds.getX()+tBounds.getWidth(),tBounds.getY()));//top
					addChild(drawBorderLine(tBounds.getX()+tBounds.getWidth(),tBounds.getY(), tBounds.getX()+tBounds.getWidth(),tBounds.getY()+tBounds.getHeight()));//right
					addChild(drawBorderLine(tBounds.getX(),tBounds.getY(), tBounds.getX(),tBounds.getY()+tBounds.getHeight()));//left
					addChild(drawBorderLine(tBounds.getX(),tBounds.getY()+tBounds.getHeight(), tBounds.getX()+tBounds.getWidth(),tBounds.getY()+tBounds.getHeight()));//bottom
			
					*/
					render();
			
//					CCanvasController.canvasdb.get(canvasUID).setGridCoordRect( tBounds );
				}});

	}
	
	public long getCanvasUID(){
		return canvasUID;
	}
	
	private PLine drawBorderLine(double x,double y, double x2, double y2)
	{
		PLine pline = new PLine();
		pline.addPoint(0, x, y);
		pline.addPoint(1, x2, y2);
		pline.setStroke(new BasicStroke( 1.0f ));
		pline.setStrokePaint( CGrid.item_border );
		return pline;
	}

	public void renderIfChanged()
	{
		int sig = CCanvasController.get_signature(canvasUID);
		if(sig!=this.last_signature)
		{			
			refreshImage();
		}
	}

	public void refreshImage() {
		SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					render();
				}});
	}
	

	public void render()
	{
		Image img = CCanvasController.image(canvasUID);
//		Image img = getCanvasImage(canvasUID);
		this.last_signature = CCanvasController.get_signature(canvasUID);

		Rectangle rect = new Rectangle(0, 0, tBounds.width, tBounds.height);
		RoundRectangle2D rRect = new RoundRectangle2D.Double(2, 2, tBounds.width-ROUNDED_RECTANGLE_OVERFLOW, tBounds.height-ROUNDED_RECTANGLE_OVERFLOW, 8, 8);
		Area border = new Area(rect);
		border.subtract(new Area(rRect));

		BufferedImage bimg = new BufferedImage(tBounds.width, tBounds.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)bimg.createGraphics();
		g.setComposite(AlphaComposite.Src);
		g.drawImage(img, 2, 2, tBounds.width, tBounds.height,null);
		g.setColor(Color.green);
		g.fill(border);
		g.setColor(new Color(100,100,100));
		g.draw(rRect);
		
//		if (CCanvasController.canvasdb.get(canvasUID).getLockValue())
//		{
//			g.setStroke(new BasicStroke(4));
//		}
		
		g.setColor(Color.black);
		g.drawRoundRect(	2,//new Double(getBounds().x).intValue() + 2,
							2,//new Double(getBounds().y).intValue() + 2,
							tBounds.width - ROUNDED_RECTANGLE_OVERFLOW,
							tBounds.height - ROUNDED_RECTANGLE_OVERFLOW,
							10,
							10);
		g.dispose();
		
		for(int i = 0; i < bimg.getHeight(); i++)
		{
			for(int j = 0; j < bimg.getWidth(); j++)
			{
				if(bimg.getRGB(j, i) == Color.green.getRGB())
				{
					//bimg.setRGB(j, i, 0x8F1C1C);
					bimg.setRGB(j, i, 0xFFFFFF);
				}
			}
		}
		
		setImage( bimg );
		//setImage( img );

		setBounds( tBounds.getX(),tBounds.getY(),tBounds.getWidth(),tBounds.getHeight());
		//CalicoDraw.setNodeBounds(this, tBounds.getX(),tBounds.getY(),tBounds.getWidth(),tBounds.getHeight());

		repaint();
		//CalicoDraw.repaint(this);
		CGrid.getInstance().repaint();
		
		updatePresenceText();
		updateCanvasLockIcon();
	}/////
	
	private Image getCanvasImage(long cuid)
	{
		CCanvas canvas = CCanvasController.canvasdb.get(cuid);
		PCamera canvasCam =canvas.getCamera();
					
		Image img = canvasCam.toImage();	
		
		return img;
	}
	
	public void updatePresenceText()
	{
		
		if (presenceText != null && presenceText.getParent() == this)
		{
			this.removeChild(presenceText);
			this.removeChild(presenceTextBackground);
			//CalicoDraw.removeChildFromNode(this, presenceText);
			//CalicoDraw.removeChildFromNode(this, presenceTextBackground);
		}
		
		StringBuilder str = new StringBuilder();
		int[] clients = CCanvasController.canvasdb.get(this.canvasUID).getClients();
		for(int i=0;i<clients.length;i++) {
			if(CalicoDataStore.clientInfo.containsKey(clients[i]) 
					&& !CalicoDataStore.clientInfo.get(clients[i]).equals(CalicoDataStore.Username)) {
				str.append(CalicoDataStore.clientInfo.get(clients[i]) + "\n");
			} else {
//				str.append("Unknown ("+clients[i]+")" + "\n");
			}
			
		}
//		this.presenceText.setText(str.toString());
//		this.presenceText.repaint();
		
		/*final PText tempPresenceText = new PText(str.toString());
		tempPresenceText.setFont(new Font("Helvetica", Font.BOLD, 12));
		tempPresenceText.setTextPaint(Color.BLUE);
		tempPresenceText.setBounds(this.getBounds().getBounds());
		tempPresenceText.translate(7, 7);*/
		presenceText = new PText(str.toString());
		presenceText.setFont(new Font("Helvetica", Font.BOLD, 12));
		presenceText.setTextPaint(Color.BLUE);
		presenceText.setBounds(this.getBounds().getBounds());
		presenceText.translate(7, 7);
		
		this.addChild(presenceText);
		//CalicoDraw.addChildToNode(this, tempPresenceText);
		presenceText.moveInFrontOf(this);
		//CalicoDraw.moveNodeInFrontOf(tempPresenceText, this);
		
		/*final PNode tempPresenceTextBackground = new PNode()
		{
			protected void paint(final PPaintContext paintContext) {
				final Graphics2D g2 = paintContext.getGraphics();
				Rectangle bounds = this.getBounds().getBounds();
				g2.setColor(Color.white);
				g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		};*/
		presenceTextBackground = new PNode()
		{
			protected void paint(final PPaintContext paintContext) {
				final Graphics2D g2 = paintContext.getGraphics();
				Rectangle bounds = this.getBounds().getBounds();
				g2.setColor(Color.white);
				g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
			}
		};
		
		this.addChild(presenceTextBackground);
		//CalicoDraw.addChildToNode(this, tempPresenceTextBackground);
		presenceTextBackground.moveInBackOf(presenceText);
		//CalicoDraw.moveNodeInBackOf(tempPresenceTextBackground, tempPresenceText);
		this.repaint();
		//CalicoDraw.repaint(this);
		
		/*SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					presenceText = tempPresenceText;
					presenceTextBackground = tempPresenceTextBackground;
				}});*/
	}
	
	public void updateCanvasLockIcon()
	{
		if (canvasLockedIcon != null && canvasLockedIcon.getParent() == this)
		{
			this.removeChild(canvasLockedIcon);
			//CalicoDraw.removeChildFromNode(this, canvasLockedIcon);
			//CalicoDraw.repaint(this);
		}
		if (!CCanvasController.canvasdb.get(canvasUID).getLockValue())
			return;
		//(new Exception()).printStackTrace();
		Image img = CalicoIconManager.getIconImage("grid.donoterase");
//		this.presenceText.setText(str.toString());
//		this.presenceText.repaint();
		
		canvasLockedIcon = new PImage(img);
		Rectangle cellBounds = this.getBounds().getBounds();
		Rectangle bounds = new Rectangle(cellBounds.x + cellBounds.width / 10 * 9 - 5, cellBounds.y + cellBounds.height / 10 * 9 - 10, 16, 16);
		//final CGridCell tempGridCell = this;
		
		//final Rectangle cellBounds;
		//final Rectangle bounds;
		/*SwingUtilities.invokeLater(
				new Runnable() { public void run() { 
					Rectangle cellBounds = tempGridCell.getBounds().getBounds();
					Rectangle bounds = new Rectangle(cellBounds.x + cellBounds.width / 10 * 9 - 5, cellBounds.y + cellBounds.height / 10 * 9 - 10, 16, 16);
					canvasLockedIcon.setBounds(bounds);
					System.out.println("locked" + " " + tempGridCell.getCanvasUID() + " " + bounds.toString());
				}});*/
		
		canvasLockedIcon.setBounds(bounds);
		//CalicoDraw.setNodeBounds(canvasLockedIcon, bounds);
		this.addChild(canvasLockedIcon);
		//CalicoDraw.addChildToNode(this, canvasLockedIcon);
		canvasLockedIcon.moveInFrontOf(this);
		//CalicoDraw.moveNodeInFrontOf(canvasLockedIcon, this);
		this.repaint();
		//CalicoDraw.repaint(this);
	}
	
//	public void updateCanvasLockIcon()
//	{
//		
//		if (canvasLockedIcon != null && canvasLockedIcon.getParent() == this)
//		{
//			this.removeChild(canvasLockedIcon);
//		}
//		
//		if (!CCanvasController.canvasdb.get(canvasUID).getLockValue())
//			return;
//		
//		presenceText = new PText("Do not erase");
//		presenceText.setFont(new Font("Helvetica", Font.BOLD, 12));
//		presenceText.setTextPaint(Color.BLUE);
//		Rectangle cellBounds = this.getBounds().getBounds();
//		Rectangle textBounds = Geometry.getTextBounds(presenceText.getText(), presenceText.getFont());
//		Rectangle bounds = new Rectangle(cellBounds.x + cellBounds.width - textBounds.width - 8, cellBounds.y + cellBounds.height - textBounds.height, textBounds.width, textBounds.height);
//		presenceText.setBounds(bounds);
//		this.addChild(presenceText);
//		presenceText.moveInFrontOf(this);
//		this.repaint();
//	}


/*

Rectangle rect = new Rectangle(0, 0, img.getWidth(null), img.getHeight(null));
RoundRectangle2D rRect = new RoundRectangle2D.Double(2, 2, img.getWidth(null) - 4, img.getHeight(null) - 4, 8, 8);
Area border = new Area(rect);
border.subtract(new Area(rRect));

BufferedImage bimg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
Graphics2D g = (Graphics2D)bimg.createGraphics();
g.setComposite(AlphaComposite.Src);
g.drawImage(img, 0, 0, null);
g.setColor(Color.green);
g.fill(border);
g.setColor(new Color(100,100,100));
g.draw(rRect);
g.setColor(Color.black);
g.drawRoundRect(	new Double(getBounds().x).intValue() + 2,
					new Double(getBounds().y).intValue() + 2,
					new Double(getBounds().width).intValue() - 4,
					new Double(getBounds().height).intValue() - 4,
					10,
					10);
g.dispose();
for(int i = 0; i < bimg.getHeight(); i++)
{
	for(int j = 0; j < bimg.getWidth(); j++)
	{
		if(bimg.getRGB(j, i) == Color.green.getRGB())
		{
			bimg.setRGB(j, i, 0x8F1C1C);
		}
	}
}

*/



}//CGridCell
