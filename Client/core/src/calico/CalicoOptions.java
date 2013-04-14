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
package calico;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;

import calico.input.CInputMode;



/**
 * This class handles the parsing of the configuration file
 * All static variables are parsed from the config file
 */
public class CalicoOptions
{

	///////////////////////////////////////////////////////////////////////////
	/// CONFIG VARIABLE CLASSES
	///////////////////////////////////////////////////////////////////////////
	
	public static class images
	{
		public static String download_folder = getImageFolder();
		private static String getImageFolder()
		{
			String r = (new Long(System.currentTimeMillis() / 1000)).toString();
			
			String folder = System.getProperty("java.io.tmpdir");
			if (!folder.endsWith(File.separator))
				folder += File.separator;
			
			folder += "img-" + r + File.separator;
			
			return folder;
		}
	}
	
	public static class pen 
	{
		public static float stroke_size = 1.0f;
		public static Color default_color = Color.BLACK;
		
		public static int doubleClickTolerance = 10;
		public static int doubleClickTimeLimit = 800;
		
		public static long press_and_hold_menu_animation_duration = 333;
		public static long press_and_hold_menu_animation_tick_rate = 20;
		public static Color press_and_hold_menu_animation_color = Color.RED;
		public static double press_and_hold_menu_radius = 30;
		
		public static class strikethru
		{
			public static boolean enabled = false;
			public static boolean debug = false;
			public static int max_height = 20;
			public static int min_intersects=5;
		}
		
		public static class debug
		{
			public static boolean enabled = false;
			public static boolean show_inflection=true;
			public static boolean show_coordinates=false;
		}
		
		public static class eraser
		{
			public static double radius = 11d;
		}
	}
	
	public static class webstart
	{
		public static boolean isWebstart = false;
	}
	
	public static class group
	{
		public static int default_rightclick_mode = 2;
		public static Color background_color = new Color(0x62,0xA5,0xCC);//Color.BLUE;
		public static float background_transparency = 0.3f;
		public static Color stroke_color = Color.BLACK;
		public static float stroke_size = 1.5f;
		public static float stroke_size_bold = 2.0f;
		public static Color stroke_selected_color = Color.BLUE;
		public static Color temp_background_color = new Color(0x22,0x88,0x22);//Color.GREEN;
		public static int padding = 10;
		public static int text_padding = 0;
		public static Font font = new Font("Helvetica", Font.PLAIN, 14);
		
	}
	
	public static class scrap 
	{
		public static class drag
		{
			public static int threshold = 3;
		}
	}
	
	public static class canvas
	{
		public static boolean lowquality_on_interaction = false;
		public static Color background_color = Color.WHITE;
		public static class input
		{
			public static CInputMode default_mode = CInputMode.EXPERT;
		}
	}
	
	public static class arrow
	{
		public static float stroke_size = 1.3f;
		public static double create_dist_threshold = 5.0;
		public static boolean show_creation_popup = false;
		public static boolean enable_pen_create = false;

		public static double length = 25.0;
		public static double angle = 0.3;
		public static double inset = 0.8;
		
		public static int headsize = 20;// NOT USED
		public static int difference = 10;// NOT USED
		public static double factor = 0.5;// NOT USED
	}
	
	public static class stroke
	{
		public static final double min_create_scrap_length = 300;
		public static Color default_color = Color.BLACK;
		public static float transparency = 1.0f;
		public static float background_transparency = 0.3f;
		public static final double max_head_to_heal_distance = 20;
	}
	
	public static class messagebox
	{
		public static long fade_time_pause = 100L;
		public static int fade_time = 2500;
		public static int padding = 8;
		public static class color
		{
			public static Color notice = new Color(0x62,0xA5,0xCC);
			public static Color error = new Color(0xBC,0x2A,0x4D);
			public static Color success = new Color(0x22,0x88,0x22);
			public static Color text = Color.BLACK;		
		}
		public static class font
		{
			public static int size = 12;
			public static String name = "Monospaced";
		}
	}
	
	public static class menu
	{
		public static Color[] colorlist = {
			Color.BLACK, 
			new Color(255,0,0), //Color.RED, 
			new Color(0,128,0), //Color.GREEN, 
			Color.BLUE, 
			new Color(190,190,190), //Color.GRAY, 
			new Color(255,192,0), //Color.ORANGE, 
			new Color(225, 225, 28), //Color.YELLOW, 
			new Color(181,50,181)}; //Color.MAGENTA};
		public static String[] colorlist_icons = {"color.black", "color.red", "color.green", "color.blue", "color.silver", "color.orange", "color.yellow", "color.purple"};
		public static float[] pensize = {1.0f, 3.0f, 5.0f};
		public static String[] pensize_icons = {"size.small", "size.medium", "size.large"};
		public static int icon_size = 35;
		public static float icon_tooltip_dist_threshold = 5.0f;
		
		public static class hold
		{
			public static int duration = 2;
			public static int dist_threshold = 2;
		}
		public static class menubar
		{
			public static int padding = 4;
			//public static Color background_color = new Color(0xC8, 0xC8, 0xC8);
			public static Color background_color = new Color(83,83,83);
			public static float transparency_disabled = 0.5f;
			public static int iconBuffer = 3;
			public static final int defaultIconDimension = 24 + iconBuffer;
			public static int defaultSpriteSize = 22;
		}
		
		
	}
	
	public static class core
	{
		public static String version = "Calico 3 Client";
		public static String icontheme = "calico";
		public static class connection
		{
			public static String settings_file = "calico.settings";
		}
		public static int tickrate = 66;
		public static int hash_check_request_interval = 500;
		

		public static double max_hold_distance = 5.0;// this is the maximum movement we will consider as "holding still" - increase if you have Parkinsons

		public static int hold_time = 250;// this is the maximum movement we will consider as "holding still" - increase if you have Parkinsons
		
		public static String plugins = "";
	}
	
	public static class viewport
	{
		public static long viewportmovetime=200;
		public static Color viewport_background_color = Color.LIGHT_GRAY;//Color.BLUE;
		public static float viewport_background_transparency = 0.3f;
		
	}
	///////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * Values loaded from the config file
	 */
	private static Properties configFile = null;
	
	// Logger
	private static Logger logger = Logger.getLogger(CalicoOptions.class.getName());
	
	// NEW PROPERTIES
	//private static Properties config_options = new Properties();
	
	
	// Color hex matching
	//private static Pattern hexcolor3 = Pattern.compile("^#?([0-9A-F])([0-9A-F])([0-9A-F])$");
	private static Pattern hexcolor6 = Pattern.compile("^#?([0-9A-F]{2})([0-9A-F]{2})([0-9A-F]{2})$");
	//private static Pattern intcolor = Pattern.compile("^([0-9]{1,3}),([0-9]{1,3}),([0-9]{1,3})$");
	
	
	public static void setup()
	{
		if (webstart.isWebstart)
			return;
		
		// Check if they passed the config file via the command line, if they did, then load it
		String cfgFilePath = System.getProperty("cfgfile", "conf/calico.properties");
		
		try
		{
			configFile = loadPropertyFile(cfgFilePath, System.getProperties());
			logger.info("Loading config file: "+cfgFilePath);
			
			setupConfigClass();
			
		}
		catch(Exception e)
		{
			logger.error("Failed to load config file, using default settings.");
		}
		
	}//
	
	
	public static class network
	{
		public static long timeout = 15000L;
		public static int cluster_size = 8000;
	}

	private static Class<?> getConfigClass(Class<?>[] classes, String name)
	{
		for(int j=0;j<classes.length;j++)
		{
			if(classes[j].getSimpleName().equals(name))
			{
				return classes[j];
			}
		}
		return null;
	}
	public static Field getConfigField(String fieldpath) throws NoSuchFieldException
	{
		String[] pathParts = fieldpath.split("\\.");
		String fieldName = pathParts[pathParts.length-1];
		
		Class<?> rootClass = CalicoOptions.class;
		
		for(int i=0;i<pathParts.length-1;i++)
		{
			rootClass = getConfigClass(rootClass.getClasses(), pathParts[i]);
		}		
		return rootClass.getField(fieldName);
	}
	
	
	
	public static void setconfig_int(Field field, int value) 			throws IllegalAccessException {field.setInt(null, value);}
	public static void setconfig_float(Field field, float value) 		throws IllegalAccessException {field.setFloat(null, value);}
	public static void setconfig_long(Field field, long value) 			throws IllegalAccessException {field.setLong(null, value);}
	public static void setconfig_double(Field field, double value) 		throws IllegalAccessException {field.setDouble(null, value);}
	public static void setconfig_boolean(Field field, boolean value) 	throws IllegalAccessException {field.setBoolean(null, value);}
	public static void setconfig_Object(Field field, Object value) 		throws IllegalAccessException {field.set(null, value);}
	
	

	public static void setConfigVariable(String configVariable, String value)
	{
		try
		{
			Field field = getConfigField(configVariable);
		
			logger.debug("[CONFIG] Set '"+configVariable+"' = "+value);
			
			String fieldtype = field.getType().getCanonicalName();
		
			if(fieldtype.equals("long"))
			{
				setconfig_long(field, Long.decode(value).longValue());
			}
			else if(fieldtype.equals("int"))
			{
				setconfig_int(field, Integer.parseInt(value));
			}
			else if(fieldtype.equals("float"))
			{
				setconfig_float(field, Float.parseFloat(value));
			}
			else if(fieldtype.equals("boolean"))
			{
				setconfig_boolean(field, Boolean.parseBoolean(value));
			}
			else if(fieldtype.equals("double"))
			{
				setconfig_double(field, Double.parseDouble(value));
			}
			else if(fieldtype.equals("java.lang.String"))
			{
				setconfig_Object(field, value);
			}
			else if(fieldtype.equals("java.awt.Color"))
			{
				setconfig_Object(field, parseColorString(value));
			}
		}
		catch(NoSuchFieldException nsfe)
		{
			logger.error("No such field: "+configVariable);
		}
		catch(IllegalAccessException e)
		{
			// dunno
		}
		
	}
	
	/*
	 * 
	 *	InputStream is = ClassLoader.getSystemClassLoader().getResourceAsInputStream("java/lang/String.class")
		byte[] bytes = IOUtils.toByteArray(is);
	 * 
	 */
	
	
	
	/**
	 * This will take all the variables from the property file, and load it to the class objects
	 */
	private static void setupConfigClass()
	{
		Class<?>[] configClasses = CalicoOptions.class.getDeclaredClasses();
		
		for(int i=0;i<configClasses.length;i++)
		{
			processClassConfig(configClasses[i]);
		}
		
		
		// Specialty ones
		
		
	}
	
	private static void processClassConfig(Class<?> className)
	{
		//logger.debug("CONFIG ["+className.getCanonicalName()+"]");
		
		Field[] fields = className.getDeclaredFields();
		for(int i=0;i<fields.length;i++)
		{
			Field field = fields[i];
			
			String fieldname = className.getCanonicalName()+"."+field.getName();
			fieldname = fieldname.replaceFirst(CalicoOptions.class.getCanonicalName()+".", "");
			String fieldtype = field.getType().getCanonicalName();
			//logger.debug(fieldname+"|"+fieldtype);
			try
			{
				String value = configFile.getProperty(fieldname);
				
				if(value==null)
				{
					// Failed, so ignore it
				}
				else if(fieldtype.equals("long"))
				{
					field.setLong(null, Long.decode(value).longValue());
				}
				else if(fieldtype.equals("int"))
				{
					field.setInt(null, Integer.parseInt(value));
				}
				else if(fieldtype.equals("float"))
				{
					field.setFloat(null, Float.parseFloat(value));
				}
				else if(fieldtype.equals("boolean"))
				{
					field.setBoolean(null, Boolean.parseBoolean(value));
				}
				else if(fieldtype.equals("double"))
				{
					field.setDouble(null, Double.parseDouble(value));
				}
				else if(fieldtype.equals("java.lang.String"))
				{
					field.set(null, value);
				}
				else if(fieldtype.equals("java.awt.Color"))
				{
					field.set(null, parseColorString(value));
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			
		}///
		
		
		Class<?>[] configClasses = className.getDeclaredClasses();
		for(int i=0;i<configClasses.length;i++)
		{
			processClassConfig(configClasses[i]);
		}
	}///////
	

	
	public static Properties loadPropertyFile(String confFile, Properties def) throws FileNotFoundException, IOException
	{
		Properties prop = new Properties(def);
		prop.load(new FileInputStream(new File(confFile)));
		return prop;
	}
	public static Properties loadPropertyFile(String confFile) throws FileNotFoundException, IOException
	{
		return loadPropertyFile(confFile,new Properties());
	}
	
	
	
	public static void writePropertyFile(Properties prop, String file)
	{
		try
		{
			prop.store(new FileOutputStream(new File(file)),file);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			Calico.logger.warn("Unable to write property file "+file);
		}
	}
	
	private static Color[] parseColorArray(String vals)
	{
		String[] parts = vals.split(",");
		int len = parts.length;
		
		Color[] colors = new Color[len];
		
		for(int i=0;i<len;i++)
		{
			colors[i] = parseColorString(parts[i]);
		}
		
		return colors;
	}
	
	
	private static Color parseColorString(String color)
	{		
		color = color.toUpperCase().trim();
		// Look for a 6 character hex code
		Matcher hx6 = hexcolor6.matcher(color);
		if(hx6.find())
		{
			return new Color(
					Integer.parseInt(hx6.group(1),16),
					Integer.parseInt(hx6.group(2),16),
					Integer.parseInt(hx6.group(3),16)
					);
		}
				
		return null;
	}
	
	public static BufferedImage getColorImage(Color color)
	{
		return getColorImage(color,16);
	}
	public static BufferedImage getColorImage(Color color,int size)
	{
		/*
		BufferedImage bimg = new BufferedImage(size,size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)bimg.createGraphics();
		g.setComposite(AlphaComposite.Src);
		g.setColor(color);
		g.fillOval(0,0,size-1,size-1);
		*/
		
		BufferedImage bimg = new BufferedImage(size,size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)bimg.createGraphics();
		
		// Turn on antialias
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.setComposite(AlphaComposite.Src);
		
		// Border color
		g.setColor(Color.BLACK);
		g.fillOval(0,0, size-1, size-1);
		
		// Circle color
		g.setColor(color);
		g.fillOval(1,1,size-3,size-3);
		g.dispose();
		return bimg;
	}
	
	public static BufferedImage getColorImageRect(Color color,int size)
	{
		/*
		BufferedImage bimg = new BufferedImage(size,size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)bimg.createGraphics();
		g.setComposite(AlphaComposite.Src);
		g.setColor(color);
		g.fillOval(0,0,size-1,size-1);
		*/
		
		BufferedImage bimg = new BufferedImage(size,size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D)bimg.createGraphics();
		
		// Turn on antialias
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		
		g.setComposite(AlphaComposite.Src);
		
		// Border color
		g.setColor(Color.BLACK);
		g.fillRect(0,0, size-1, size-1);
		
		// Circle color
		g.setColor(color);
		g.fillRect(1,1,size-3,size-3);
		g.dispose();
		return bimg;
	}
	
	
	

	
	public static String getconfig_String(String key)
	{
		return System.getProperty(key, configFile.getProperty(key));
	}
	public static int getconfig_int(String key)
	{
		return Integer.parseInt(System.getProperty(key, configFile.getProperty(key)));
	}
	public static long getconfig_long(String key)
	{
		return Long.decode(System.getProperty(key, configFile.getProperty(key))).longValue();
	}
	public static float getconfig_float(String key)
	{
		return Float.parseFloat(System.getProperty(key, configFile.getProperty(key)));
	}
	public static double getconfig_double(String key)
	{
		return Double.parseDouble(System.getProperty(key, configFile.getProperty(key)));
	}
	public static boolean getconfig_boolean(String key)
	{
		return Boolean.parseBoolean(System.getProperty(key, configFile.getProperty(key)));
	}
	
	
	
	

}//End Options
