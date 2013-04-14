package calico.plugins.userlist.iconsets;

import calico.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;

import calico.modules.ErrorMessage;

public class CalicoIconManager
{
	private static Properties iconTheme = new Properties();
	
	private static String iconNotFound = "";
	private static String iconThemeName = "";
	
	public static int defaultIconSize = 16;
	
	public static Logger logger = Logger.getLogger(CalicoIconManager.class.getName());
	
	public static void setIconTheme(Class<?> clazz, String name)
	{
		iconThemeName = name;
		try
		{
			iconTheme.load( clazz.getResourceAsStream("/calico/iconsets/"+iconThemeName+"/userlisticontheme.properties")  );
			//iconTheme.list(System.out);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ErrorMessage.fatal("Unable to load icon theme "+iconThemeName+".");
		}
		
		// Load Up the default parts
		iconNotFound = iconTheme.getProperty("notfound","cross.png");
		
		// Show some blip about it
		logger.info("Loading Icon Theme: "+iconTheme.getProperty("iconset.name")+" by "+iconTheme.getProperty("author.name")+" ("+iconTheme.getProperty("author.email")+")");
		
	}
	
	public static String getIcon(String icon)
	{
		String iconPath = iconTheme.getProperty(icon,iconNotFound);
		if(iconPath.startsWith("@"))
		{
			return getIcon( iconPath.replace("@", "") );
		}
		
		URL url = CalicoDataStore.calicoObj.getClass().getResource("iconsets/"+iconThemeName+"/"+iconPath);
		return url.toString();
	}
	
	public static Image getIconImage(String iconPath)
	{
		try
		{
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			Image icon = toolkit.getImage(new URL(CalicoIconManager.getIcon(iconPath)));
			
			while(!toolkit.prepareImage(icon, -1, -1, null))
			{
				Thread.sleep(1L);
			}
			
			
			return icon;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static BufferedImage getImagePart(Image img, int x, int y, int w, int h)
	{
		BufferedImage bgBuf = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) bgBuf.getGraphics();
		//g.setBackground(new Color(83,83,83));
		g.drawImage(img, null, null);
		g.dispose();
		
		return bgBuf.getSubimage(x, y, w, h);
	}
	
}
