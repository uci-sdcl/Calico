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
package calico.iconsets;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import calico.CalicoDataStore;
import calico.modules.ErrorMessage;

public class CalicoIconManager
{
	private static Properties iconTheme = new Properties();

	private static String iconNotFound = "";
	private static String iconThemeName = "";

	public static int defaultIconSize = 16;

	public static Logger logger = Logger.getLogger(CalicoIconManager.class.getName());

	private static final Map<String, BufferedImage> ICON_CACHE = new HashMap<String, BufferedImage>();

	public static void setIconTheme(String name)
	{
		iconThemeName = name;
		try
		{
			iconTheme.load(CalicoDataStore.calicoObj.getClass().getResourceAsStream("iconsets/" + iconThemeName + "/icontheme.properties"));
			// iconTheme.list(System.out);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			ErrorMessage.fatal("Unable to load icon theme " + iconThemeName + ".");
		}

		// Load Up the default parts
		iconNotFound = iconTheme.getProperty("notfound", "cross.png");

		// Show some blip about it
		logger.info("Loading Icon Theme: " + iconTheme.getProperty("iconset.name") + " by " + iconTheme.getProperty("author.name") + " ("
				+ iconTheme.getProperty("author.email") + ")");

	}

	public static String getIcon(String icon)
	{
		String iconPath = iconTheme.getProperty(icon, iconNotFound);
		if (iconPath.startsWith("@"))
		{
			return getIcon(iconPath.replace("@", ""));
		}

		URL url = CalicoDataStore.calicoObj.getClass().getResource("iconsets/" + iconThemeName + "/" + iconPath);
		return url.toString();
	}

	public static BufferedImage getIconImage(String iconPath)
	{
		BufferedImage image = ICON_CACHE.get(iconPath);
		if (image == null)
		{
			try
			{
				image = ImageIO.read(new URL(CalicoIconManager.getIcon(iconPath)));
				ICON_CACHE.put(iconPath, image);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}
		}
		return image;
	}

	public static BufferedImage getImagePart(Image img, int x, int y, int w, int h)
	{
		BufferedImage bgBuf = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bgBuf.getGraphics();
		// g.setBackground(new Color(83,83,83));
		g.drawImage(img, null, null);
		g.dispose();

		return bgBuf.getSubimage(x, y, w, h);
	}

}
