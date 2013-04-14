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
package calico.plugins.iip.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import calico.Geometry;
import calico.components.CCanvas;
import calico.controllers.CCanvasController;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * Graphical utilities for the Intention View.
 *
 * @author Byron Hawkins
 */
public class IntentionalInterfacesGraphics
{
	private static final Color COORDINATES_COLOR = new Color(0x77777766);
	private static final int CORNER_INSET = 3;

	public static Image superimposeCellAddress(Image baseImage, long canvas_uuid)
	{
		CCanvas canvas = CCanvasController.canvasdb.get(canvas_uuid);
		String coordinates;
		if (canvas == null)
		{
			coordinates = "X";
		}
		else
		{
			coordinates = String.valueOf(canvas.getIndex());
		}
		Rectangle baseBounds = new Rectangle(baseImage.getWidth(null), baseImage.getHeight(null));
		BufferedImage compound = new BufferedImage(baseBounds.width, baseBounds.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) compound.getGraphics();

		Color c = g.getColor();
		Font f = g.getFont();

		g.drawImage(baseImage, 0, 0, null);
		g.setFont(new Font("Verdana", Font.BOLD, 32));
		g.setColor(COORDINATES_COLOR);

		Rectangle2D coordinatesBoundsMess = g.getFontMetrics().getStringBounds(coordinates, g);
		Rectangle coordinatesBounds = new Rectangle((int) coordinatesBoundsMess.getWidth(), (int) coordinatesBoundsMess.getHeight());
		int x = (baseBounds.width - coordinatesBounds.width) / 2;
		int y = (baseBounds.height - ((baseBounds.height - coordinatesBounds.height) / 2)) - g.getFontMetrics().getDescent();
		g.drawString(coordinates, x, y);

		g.setColor(c);
		g.setFont(f);

		return compound;
	}

	public static void superimposeCellAddressInCorner(Graphics2D g, long canvas_uuid, double width, Font font, Color color)
	{
		CCanvas canvas = CCanvasController.canvasdb.get(canvas_uuid);
		if (canvas == null)
		{
			// this canvas is being deleted, so stop trying to draw about it
			return;
		}
		
		String coordinates = String.valueOf(canvas.getIndex());

		Color c = g.getColor();
		Font f = g.getFont();

		g.setFont(font);
		g.setColor(color);

		Rectangle2D coordinatesBoundsMess = g.getFontMetrics().getStringBounds(coordinates, g);
		Rectangle coordinatesBounds = new Rectangle((int) coordinatesBoundsMess.getWidth(), (int) coordinatesBoundsMess.getHeight());
		int x = (int) (width - (coordinatesBounds.width + CORNER_INSET));
		int y = (coordinatesBounds.height + CORNER_INSET) - g.getFontMetrics().getDescent();
		g.drawString(coordinates, x, y);

		g.setColor(c);
		g.setFont(f);
	}

	private static final double POSITIVE_X_AXIS = 0.0;
	private static final double POSITIVE_Y_AXIS = -(Math.PI / 2.0);
	private static final double NEGATIVE_X_AXIS = -Math.PI;
	private static final double NEGATIVE_Y_AXIS = -(3.0 * (Math.PI / 2.0));
	private static final int VERTICAL = (int) ((Math.PI / 2.0) * 1000.0);
	private static final int OBLIQUE = (int) ((Math.PI / 4.0) * 1000.0);

	public static PText createLabelOnSegment(String text, Point2D segmentEndpoint1, Point2D segmentEndpoint2)
	{
		PText label = new PText(text);
		label.setFont(new Font("Verdana", Font.PLAIN, 20));
		PBounds bounds = label.computeFullBounds(null);
		double[] center = Geometry.computePointOnLine(segmentEndpoint1.getX(), segmentEndpoint1.getY(), segmentEndpoint2.getX(), segmentEndpoint2.getY(), 0.5);

		bounds.setRect(center[0] - (bounds.getWidth() / 2.0), center[1], bounds.getWidth(), bounds.getHeight());
		label.setBounds(bounds);

		double theta = calico.utils.Geometry.angle(segmentEndpoint1.getX(), segmentEndpoint1.getY(), segmentEndpoint2.getX(), segmentEndpoint2.getY(),
				segmentEndpoint1.getX() + 1.0, segmentEndpoint1.getY());
		if ((theta < POSITIVE_Y_AXIS) && (theta > NEGATIVE_Y_AXIS))
		{
			// flip the other way
			theta += NEGATIVE_X_AXIS;
		}
		label.rotateAboutPoint(theta, bounds.getX() + (bounds.getWidth() / 2.0), bounds.getY() + (bounds.getHeight() / 2.0));

		// at this point the label is directly on top of the segment and parallel to it, so now raise by its height to
		// avoid the collision. Offset must be reduced with increased verticality because of some weird arithmetic
		// artifact.
		int verticality = Math.abs((int) (theta * 1000.0) % (2 * VERTICAL));
		if (verticality > VERTICAL)
		{
			verticality = Math.abs(verticality - (2 * VERTICAL));
		}
		double verticalityComplement = 1.0 - (verticality / (double) VERTICAL);

		int obliqueness = Math.abs((int) (theta * 1000.0) % VERTICAL);
		if (obliqueness > OBLIQUE)
		{
			obliqueness = Math.abs(obliqueness - (2 * OBLIQUE));
		}
		double obliquenessPercent = (obliqueness / (double) OBLIQUE);

		double baselineOffset = -10.0;
		baselineOffset -= bounds.getHeight() * (0.5 + (0.5 * verticalityComplement));
		baselineOffset -= 2.0 * obliquenessPercent;
		label.translate(0.0, baselineOffset);

		return label;
	}

	public static Image createCanvasThumbnail(long canvasId, Insets insets)
	{
		return createCanvasThumbnail(canvasId, null, insets);
	}

	public static Image createCanvasThumbnail(long canvasId, Dimension size, Insets insets)
	{
		CCanvas canvas = CCanvasController.canvasdb.get(canvasId);
		
		if (canvas.getContentCamera().getBounds().isEmpty())
		{
			// there's no image to get yet
			return new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
		}
		
		//Nick: Using this class properly scales it.
		Image canvasSnapshot = CCanvasController.image(canvasId);//canvas.getContentCamera().toImage();

		if (size == null)
		{
			size = new Dimension(canvasSnapshot.getWidth(null), canvasSnapshot.getHeight(null));
		}

		BufferedImage thumbnail = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) thumbnail.getGraphics();
		g.drawImage(canvasSnapshot, insets.left, insets.top, size.width - (insets.left + insets.right), size.height - (insets.top + insets.bottom), null);

		return thumbnail;
	}
}
