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
package calico.utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

public class ImageUtils
{
	private static final SaturationAdjuster SATURATION_ADJUSTER = new SaturationAdjuster();

	/**
	 * Produce a new image from the input by changing the saturation and brightness according to <code>adjustment</code>
	 * , which ranges from -1.0 to 1.0 as follows:
	 * 
	 * <pre>
	 * -1.0f grayscale
	 * 0f    original colors
	 * 1.0f  full saturation
	 * </pre>
	 * 
	 * The saturation will change by <code>adjustment</code>, while the brightness will change by
	 * <code>-adjustment</code>.
	 */
	public static BufferedImage adjustIntensity(BufferedImage image, float adjustment)
	{
		if (adjustment == 0.0)
		{
			return image;
		}

		return SATURATION_ADJUSTER.adjustIntensity(image, adjustment);
	}

	/*
	 * private static class SaturationAdjuster { private static final double RW = 0.3086; private static final double RG
	 * = 0.6084; private static final double RB = 0.0820;
	 * 
	 * BufferedImage adjustSaturation(Image image, double adjustment) { double adjustmentInverse = 1 - adjustment;
	 * double a = adjustmentInverse * RW + adjustment; double b = adjustmentInverse * RW; double c = adjustmentInverse *
	 * RW; double d = adjustmentInverse * RG; double e = adjustmentInverse * RG + adjustment; double f =
	 * adjustmentInverse * RG; double g = adjustmentInverse * RB; double h = adjustmentInverse * RB; double i =
	 * adjustmentInverse * RB + adjustment;
	 * 
	 * // output_red = a*red + d*green + g*blue; // output_green = b*red + e*green + h*blue; // output_blue = c*red +
	 * f*green + i*blue; // output_alpha = alpha;
	 * 
	 * } }
	 */

	private static class SaturationAdjuster
	{
		BufferedImage adjustIntensity(BufferedImage image, float adjustment)
		{
			HSBAdjustFilter filter = new HSBAdjustFilter(0, adjustment, -adjustment);
			BufferedImage adjustedImage = filter.createCompatibleDestImage(image);
			Graphics2D g = (Graphics2D) adjustedImage.getGraphics();
			g.drawImage(image, filter, 0, 0);
			g.dispose();
			return adjustedImage;
		}
	}

	/**
	 * Code taken from http://www.jhlabs.com/ip/filters/download.html.
	 */
	public static class HSBAdjustFilter extends PointFilter
	{
		public float hFactor, sFactor, bFactor;
		private float[] hsb = new float[3];

		public HSBAdjustFilter()
		{
			this(0, 0, 0);
		}

		public HSBAdjustFilter(float h, float s, float b)
		{
			hFactor = h;
			sFactor = s;
			bFactor = b;
			canFilterIndexColorModel = true;
		}

		public void setHFactor(float hFactor)
		{
			this.hFactor = hFactor;
		}

		public float getHFactor()
		{
			return hFactor;
		}

		public void setSFactor(float sFactor)
		{
			this.sFactor = sFactor;
		}

		public float getSFactor()
		{
			return sFactor;
		}

		public void setBFactor(float bFactor)
		{
			this.bFactor = bFactor;
		}

		public float getBFactor()
		{
			return bFactor;
		}

		public int filterRGB(int x, int y, int rgb)
		{
			int a = rgb & 0xff000000;
			int r = (rgb >> 16) & 0xff;
			int g = (rgb >> 8) & 0xff;
			int b = rgb & 0xff;
			Color.RGBtoHSB(r, g, b, hsb);
			hsb[0] += hFactor;
			while (hsb[0] < 0)
				hsb[0] += Math.PI * 2;
			hsb[1] += sFactor;
			if (hsb[1] < 0)
				hsb[1] = 0;
			else if (hsb[1] > 1.0)
				hsb[1] = 1.0f;
			hsb[2] += bFactor;
			if (hsb[2] < 0)
				hsb[2] = 0;
			else if (hsb[2] > 1.0)
				hsb[2] = 1.0f;
			rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
			return a | (rgb & 0xffffff);
		}

		public String toString()
		{
			return "Colors/Adjust HSB...";
		}
	}

	/**
	 * An abstract superclass for point filters. The interface is the same as the old RGBImageFilter.
	 * 
	 * Code taken from http://www.jhlabs.com/ip/filters/download.html.
	 */
	public static abstract class PointFilter extends AbstractBufferedImageOp
	{
		protected boolean canFilterIndexColorModel = false;

		public BufferedImage filter(BufferedImage src, BufferedImage dst)
		{
			int width = src.getWidth();
			int height = src.getHeight();
			int type = src.getType();
			WritableRaster srcRaster = src.getRaster();

			if (dst == null)
				dst = createCompatibleDestImage(src, null);
			WritableRaster dstRaster = dst.getRaster();

			setDimensions(width, height);

			int[] inPixels = new int[width];
			for (int y = 0; y < height; y++)
			{
				// We try to avoid calling getRGB on images as it causes them to become unmanaged, causing horrible
				// performance problems.
				if (type == BufferedImage.TYPE_INT_ARGB)
				{
					srcRaster.getDataElements(0, y, width, 1, inPixels);
					for (int x = 0; x < width; x++)
						inPixels[x] = filterRGB(x, y, inPixels[x]);
					dstRaster.setDataElements(0, y, width, 1, inPixels);
				}
				else
				{
					src.getRGB(0, y, width, 1, inPixels, 0, width);
					for (int x = 0; x < width; x++)
						inPixels[x] = filterRGB(x, y, inPixels[x]);
					dst.setRGB(0, y, width, 1, inPixels, 0, width);
				}
			}

			return dst;
		}

		public void setDimensions(int width, int height)
		{
		}

		public abstract int filterRGB(int x, int y, int rgb);
	}

	/**
	 * A convenience class which implements those methods of BufferedImageOp which are rarely changed.
	 * 
	 * Code taken from http://www.jhlabs.com/ip/filters/download.html.
	 */
	public static abstract class AbstractBufferedImageOp implements BufferedImageOp, Cloneable
	{
		@Override
		public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel dstCM)
		{
			if (dstCM == null)
				dstCM = src.getColorModel();
			return new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight()), dstCM.isAlphaPremultiplied(), null);
		}

		public BufferedImage createCompatibleDestImage(BufferedImage src)
		{
			return createCompatibleDestImage(src, null);
		}

		public Rectangle2D getBounds2D(BufferedImage src)
		{
			return new Rectangle(0, 0, src.getWidth(), src.getHeight());
		}

		public Point2D getPoint2D(Point2D srcPt, Point2D dstPt)
		{
			if (dstPt == null)
				dstPt = new Point2D.Double();
			dstPt.setLocation(srcPt.getX(), srcPt.getY());
			return dstPt;
		}

		public RenderingHints getRenderingHints()
		{
			return null;
		}

		/**
		 * A convenience method for getting ARGB pixels from an image. This tries to avoid the performance penalty of
		 * BufferedImage.getRGB unmanaging the image.
		 * 
		 * @param image
		 *            a BufferedImage object
		 * @param x
		 *            the left edge of the pixel block
		 * @param y
		 *            the right edge of the pixel block
		 * @param width
		 *            the width of the pixel arry
		 * @param height
		 *            the height of the pixel arry
		 * @param pixels
		 *            the array to hold the returned pixels. May be null.
		 * @return the pixels
		 * @see #setRGB
		 */
		public int[] getRGB(BufferedImage image, int x, int y, int width, int height, int[] pixels)
		{
			int type = image.getType();
			if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB)
				return (int[]) image.getRaster().getDataElements(x, y, width, height, pixels);
			return image.getRGB(x, y, width, height, pixels, 0, width);
		}

		/**
		 * A convenience method for setting ARGB pixels in an image. This tries to avoid the performance penalty of
		 * BufferedImage.setRGB unmanaging the image.
		 * 
		 * @param image
		 *            a BufferedImage object
		 * @param x
		 *            the left edge of the pixel block
		 * @param y
		 *            the right edge of the pixel block
		 * @param width
		 *            the width of the pixel arry
		 * @param height
		 *            the height of the pixel arry
		 * @param pixels
		 *            the array of pixels to set
		 * @see #getRGB
		 */
		public void setRGB(BufferedImage image, int x, int y, int width, int height, int[] pixels)
		{
			int type = image.getType();
			if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB)
				image.getRaster().setDataElements(x, y, width, height, pixels);
			else
				image.setRGB(x, y, width, height, pixels, 0, width);
		}
	}
}
